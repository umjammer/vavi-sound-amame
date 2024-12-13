
/***************************************************************************

	v9938 / v9958 emulation

***************************************************************************/

/*
 todo:

 - sprite collision 
 - vdp engine -- make run at correct speed
 - vr/hr/fh flags: double-check all of that
 - make vdp engine work in exp. ram
*/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.vidhrdw;

import static arcadeflex056.fucPtr.*;
import static arcadeflex056.osdepend.logerror;
import static arcadeflex056.video.osd_set_visible_area;
import static arcadeflex056.video.osd_skip_this_frame;
import static common.libc.cstring.*;
import static common.ptr.*;
import static mame056.commonH.*;
import static mame056.mame.*;
import static mame056.cpuexec.*;
import static mess056.vidhrdw.tms9928a.INTCallbackPtr;
import static mess056.vidhrdw.v9938H.*;
import static mess056.vidhrdw.v9938mod.*;

public class v9938
{
	
	public static class _V9938 {
		/* general */
		public int model;
		public int offset_x, offset_y, visible_y, mode;
		/* palette */
		public int	pal_write_first, cmd_write_first;
		public int pal_write, cmd_write;
		public int[] palReg=new int[32], statReg=new int[10], contReg=new int[48];
                public int read_ahead;
		/* memory */
		public int address_latch;
		public UBytePtr vram = new UBytePtr(), vram_exp = new UBytePtr();
		public int vram_size;
	    /* interrupt */
                public int INT;
                public INTCallbackPtr INTCallback;
		public int scanline;
                /* blinking */
                public int blink, blink_count;
                /* sprites */
                public int sprite_limit;
		/* size */
		public int size=0, size_old, size_auto, size_now;
	};
	
	
	
	static char[] pal_ind16=new char[16], pal_ind256=new char[256], pal_indYJK;
	
	public static int V9938_MODE_TEXT1	= (0);
	public static int V9938_MODE_MULTI	= (1);
	public static int V9938_MODE_GRAPHIC1	= (2);
	public static int V9938_MODE_GRAPHIC2	= (3);
	public static int V9938_MODE_GRAPHIC3	= (4);
	public static int V9938_MODE_GRAPHIC4	= (5);
	public static int V9938_MODE_GRAPHIC5	= (6);
	public static int V9938_MODE_GRAPHIC6	= (7);
	public static int V9938_MODE_GRAPHIC7	= (8);
	public static int V9938_MODE_TEXT2	= (9);
	public static int V9938_MODE_UNKNOWN	= (10);
	
	
	static String v9938_modes[] = {
		"TEXT 1", "MULTICOLOR", "GRAPHIC 1", "GRAPHIC 2", "GRAPHIC 3",
		"GRAPHIC 4", "GRAPHIC 5", "GRAPHIC 6", "GRAPHIC 7", "TEXT 2",
		"UNKNOWN" };
	
	
	/***************************************************************************
	
		Palette functions
	
	***************************************************************************/
	
	/*
	About the colour burst registers:
	
	The color burst registers will only have effect on the composite video outputfrom
	the V9938. but the output is only NTSC (Never The Same Color ,so the
	effects are already present) . this system is not used in europe
	the european machines use a separate PAL  (Phase Alternating Line) encoder
	or no encoder at all , only RGB output.
	
	Erik de Boer.
	
	--
	Right now they're not emulated. For completeness sake they should -- with
	a dip-switch to turn them off. I really don't know how they work though. :(
	*/
	
	/*
	 In screen 8, the colors are encoded as:
	
	 7  6  5  4  3  2  1  0
	+--+--+--+--+--+--+--+--+
	|g2|g1|g0|r2|r1|r0|b2|b1|
	+--+--+--+--+--+--+--+--+
	
	b0 is set if b2 and b1 are set (remember, color bus is 3 bits)
	
	*/
	
	public static VhConvertColorPromPtr v9938_init_palette = new VhConvertColorPromPtr() {
            public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
                int	i,red;
                int _palette = 0;
	
		if (Machine.scrbitmap.depth == 8)
			{
			/* create 256 colour palette -- this is actually the graphic 7
			   palette, with duplicate entries so the core fill shrink it to
			   256 colours */
			for (i=0;i<512;i++)
				{
				palette[_palette++] = (char)(((i >> 6) & 7) * 36); /* red */
				palette[_palette++] = (char)(((i >> 3) & 7) * 36); /* green */
				red = (i & 6); if (red == 6) red++;
				palette[_palette++] = (char)(red * 36); /* blue */
				}
			}
		else
			{
			/* create the full 512 colour palette */
			for (i=0;i<512;i++)
				{
				palette[_palette++] = (char)(((i >> 6) & 7) * 36); /* red */
				palette[_palette++] = (char)(((i >> 3) & 7) * 36); /* green */
				palette[_palette++] = (char)((i & 7) * 36); /* blue */
				}
			}
            }
        };
		
	/*
	
	The v9958 can display up to 19286 colours. For this we need a larger palette.
	
	The colours are encoded in 17 bits; however there are just 19268 different colours.
	Here we calculate the palette and a 2^17 reference table to the palette,
	which is: pal_indYJK. It's 256K in size, but I can't think of a faster way
	to emulate this. Also it keeps the palette a reasonable size. :)
	
	*/
	
	public static VhConvertColorPromPtr v9958_init_palette = new VhConvertColorPromPtr() {
            public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
		int r,g,b,y,j,k,i,k0,j0,n;
		char[] pal;
	
		pal = new UBytePtr(palette, 512*3).memory;
		v9938_init_palette.handler(palette, colortable, color_prom);
	
		/* set up YJK table */
		if (pal_indYJK == null)
			{
			pal_indYJK = new char[0x20000];
			if (pal_indYJK == null)
				{
				logerror ("Fatal: cannot malloc () in v9958_init_palette (), cannot exit\n");
				return;
				}
			}
	
		logerror ("Building YJK table for V9958 screens, may take a while ... \n");
		i = 0;
		for (y=0;y<32;y++) for (k=0;k<64;k++) for (j=0;j<64;j++)
			{
			/* calculate the color */
			if (k >= 32) k0 = (k - 64); else k0 = k;
			if (j >= 32) j0 = (j - 64); else j0 = j;
			r = y + j0;
                        b = (y * 5 - 2 * j0 - k0) / 4;
                        g = y + k0;
			if (r < 0) r = 0; else if (r > 31) r = 31;
			if (g < 0) g = 0; else if (g > 31) g = 31;
			if (b < 0) b = 0; else if (b > 31) b = 31;
	
			if (Machine.scrbitmap.depth == 8)
				{
				/* we don't have the space for more entries, so map it to the
				   256 colours we already have */
				r /= 4; g /= 4; b /= 4;
				pal_indYJK[y | j << 5 | k << (5 + 6)] = (char)((r << 6) | (g << 3) | b);
				}
			else
				{
				r = (255 * r) / 31;
				b = (255 * g) / 31;
				g = (255 * r) / 31;
				/* have we seen this one before */
				n = 0;
				while (n < i)
					{
					if (pal[n*3+0] == r && pal[n*3+1] == g && pal[n*3+2] == b)
						{
						pal_indYJK[y | j << 5 | k << (5 + 6)] = (char)(n + 512);
						break;
						}
					n++;
					}
	
				if (i == n)
					{
					/* so we haven't; add it */
					pal[i*3+0] = (char) r;
					pal[i*3+1] = (char) g;
					pal[i*3+2] = (char) b;
					pal_indYJK[y | j << 5 | k << (5 + 6)] = (char) (i + 512);
					i++;
					}
				}
			}
	
		if (i != 19268)
			logerror ("Table creation failed - %d colours out of 19286 created\n", i);
		}
        };
	
	/*
	
	 so lookups for screen 12 will look like:
	
	 int ind;
	
	 ind = (*data & 7) << 11 | (*(data + 1) & 7) << 14 |
		   (*(data + 2) & 7) << 5 | (*(data + 3) & 7) << 8;
	
	 pixel0 = pal_indYJK[ind | (*data >> 3) & 31];
	 pixel1 = pal_indYJK[ind | (*(data + 1) >> 3) & 31];
	 pixel2 = pal_indYJK[ind | (*(data + 2) >> 3) & 31];
	 pixel3 = pal_indYJK[ind | (*(data + 3) >> 3) & 31];
	
	and for screen 11:
	
	pixel0 = (*data) & 8 ? pal_ind16[(*data) >> 4] : pal_indYJK[ind | (*data >> 3) & 30];
	pixel1 = *(data+1) & 8 ? pal_ind16[*(data+1) >> 4] : pal_indYJK[ind | *(data+1) >> 3) & 30];
	pixel2 = *(data+2) & 8 ? pal_ind16[*(data+2) >> 4] : pal_indYJK[ind | *(data+2) >> 3) & 30];
	pixel3 = *(data+3) & 8 ? pal_ind16[*(data+3) >> 4] : pal_indYJK[ind | *(data+3) >> 3) & 30];
	
	*/
	
        public static _V9938 _vdp = new _V9938();
        
	public static WriteHandlerPtr v9938_palette_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                int indexp;
	
		if (_vdp.pal_write_first != 0)
			{
			/* store in register */
			indexp = _vdp.contReg[0x10] & 15;
			_vdp.palReg[indexp*2] = _vdp.pal_write & 0x77;
			_vdp.palReg[indexp*2+1] = data & 0x07;
			/* update palette */
			pal_ind16[indexp] = (char) ((((int)_vdp.pal_write << 2) & 0x01c0)  |
                                (((int)data << 3) & 0x0038)  |
                                ((int)_vdp.pal_write & 0x0007));
	
			_vdp.contReg[0x10] = (_vdp.contReg[0x10] + 1) & 15;
			_vdp.pal_write_first = 0;
			}
		else
			{
			_vdp.pal_write = data;
			_vdp.pal_write_first = 1;
			}
            }
            
        };
		
                /* taken from V9938 Technical Data book, page 148. it's in G-R-B format */
		static char pal16[] = {
			0, 0, 0, /* 0: black/transparent */
			0, 0, 0, /* 1: black */
			6, 1, 1, /* 2: medium green */
			7, 3, 3, /* 3: light green */
			1, 1, 7, /* 4: dark blue */
			3, 2, 7, /* 5: light blue */
			1, 5, 1, /* 6: dark red */
			6, 2, 7, /* 7: cyan */
			1, 7, 1, /* 8: medium red */
			3, 7, 3, /* 9: light red */
			6, 6, 1, /* 10: dark yellow */
			6, 6, 4, /* 11: light yellow */
			4, 1, 1, /* 12: dark green */
			2, 6, 5, /* 13: magenta */
			5, 5, 5, /* 14: gray */
			7, 7, 7  /* 15: white */
	        };
                
	static void v9938_reset_palette ()
		{
		
		int i, red, ind;
	
		for (i=0;i<16;i++)
			{
			/* set the palette registers */
			_vdp.palReg[i*2+0] = pal16[i*3+1] << 4 | pal16[i*3+2];
			_vdp.palReg[i*2+1] = pal16[i*3];
			/* set the reference table */
			pal_ind16[i] = (char) (pal16[i*3+1] << 6 | pal16[i*3] << 3 | pal16[i*3+2]);
			}
	
		/* set internal palette GRAPHIC 7 */
		for (i=0;i<256;i++)
			{
			ind = (i << 4) & 0x01c0;
			ind |= (i >> 2) & 0x0038;
			red = (i << 1) & 6; if (red == 6) red++;
			ind |= red;
	
			pal_ind256[i] = (char) ind;
			}
		}
	
	/***************************************************************************
	
		Memory functions
	
	***************************************************************************/
	
	static void v9938_vram_write (int offset, int data)
		{
		int newoffset;
	
		if ( (_vdp.mode == V9938_MODE_GRAPHIC6) || (_vdp.mode == V9938_MODE_GRAPHIC7) )
			{
	        newoffset = ((offset & 1) << 16) | (offset >> 1);
	   		if (newoffset < _vdp.vram_size)
	        	_vdp.vram.write(newoffset, data);
			}
		else
			{
			if (offset < _vdp.vram_size)
				_vdp.vram.write(offset, data);
	        }
		}
	
	static int v9938_vram_read (int offset)
		{
		if ( (_vdp.mode == V9938_MODE_GRAPHIC6) || (_vdp.mode == V9938_MODE_GRAPHIC7) )
			return _vdp.vram.read(((offset & 1) << 16) | (offset >> 1));
		else
			return _vdp.vram.read(offset);
		}
	
	public static WriteHandlerPtr v9938_vram_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                int address;
	
		/*v9938_update_command ();*/
	
		_vdp.cmd_write_first = 0;
	
	    address = ((int)_vdp.contReg[14] << 14) | _vdp.address_latch;
	
	    if ((_vdp.contReg[47] & 0x20)!=0)
	        {
	        if (_vdp.vram_exp!=null && address < 0x10000)
	            _vdp.vram_exp.write(address, data);
	        }
	    else
	        {
			v9938_vram_write (address, data);
	        }
	
		_vdp.address_latch = (_vdp.address_latch + 1) & 0x3fff;
		if (_vdp.address_latch==0 && (_vdp.contReg[0] & 0x0c)!=0 ) /* correct ??? */
			{
			_vdp.contReg[14] = (_vdp.contReg[14] + 1) & 7;
			}
            }
        };
		
	public static ReadHandlerPtr v9938_vram_r = new ReadHandlerPtr() {
            public int handler(int offset) {
                int ret;
		int address;
	
		address = ((int)_vdp.contReg[14] << 14) | _vdp.address_latch;
	
		_vdp.cmd_write_first = 0;
	
		ret = _vdp.read_ahead;
	
		if ((_vdp.contReg[47] & 0x20) != 0)
			{
			/* correct? */
			if (_vdp.vram_exp!=null && address < 0x10000)
				_vdp.read_ahead = _vdp.vram_exp.read(address);
			else
				_vdp.read_ahead = 0xff;
			}
		else
			{
			_vdp.read_ahead = v9938_vram_read (address);
			}
	
		_vdp.address_latch = (_vdp.address_latch + 1) & 0x3fff;
		if (_vdp.address_latch==0 && (_vdp.contReg[0] & 0x0c)!=0 ) /* correct ??? */
			{
			_vdp.contReg[14] = (_vdp.contReg[14] + 1) & 7;
			}
	
		return ret;
            }
        };
		
	public static WriteHandlerPtr v9938_command_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
		if (_vdp.cmd_write_first != 0)
			{
			if ((data & 0x80) != 0)
				v9938_register_write(data & 0x3f, _vdp.cmd_write);
			else
				{
				_vdp.address_latch =
					((data << 8) | _vdp.cmd_write) & 0x3fff;
				if ( (data & 0x40) == 0 ) v9938_vram_r.handler(0); /* read ahead! */
				}
	
			_vdp.cmd_write_first = 0;
			}
		else
			{
			_vdp.cmd_write = data;
			_vdp.cmd_write_first = 1;
			}
		}
        };
	
	/***************************************************************************
	
		Init/stop/reset/Interrupt functions
	
	***************************************************************************/
	
	public static int v9938_init (int model, int vram_size, INTCallbackPtr callback )
	{
		_vdp = new _V9938();
	
		_vdp.model = model;
		_vdp.vram_size = vram_size;
		_vdp.INTCallback = callback;
		_vdp.size_old = -1;
	
		/* allocate VRAM */
		_vdp.vram = new UBytePtr (0x20000);
		if (_vdp.vram==null) return 1;
		memset (_vdp.vram, 0, 0x20000);
		if (_vdp.vram_size < 0x20000)
			{
			/* set unavailable RAM to 0xff */
			memset ( new UBytePtr(_vdp.vram, _vdp.vram_size), 0xff, (0x20000 - _vdp.vram_size) );
			}
		/* do we have expanded memory? */
		if (_vdp.vram_size > 0x20000)
			{
			_vdp.vram_exp = new UBytePtr (0x10000);
			if (_vdp.vram_exp==null)
				{
				_vdp.vram = null;
				return 1;
				}
			memset (_vdp.vram_exp, 0, 0x10000);
			}
		else
			_vdp.vram_exp = null;
	
		return 0;
		}
	
	public static void v9938_reset ()
		{
                    System.out.println("v9938_reset");
		int i;
	
		/* offset reset */
		_vdp.offset_x = 7;
		_vdp.offset_y = 7 + 10;
		_vdp.visible_y = 192;
		/* register reset */
		v9938_reset_palette (); /* palette registers */
		for (i=0;i<10;i++) _vdp.statReg[i] = 0;
		_vdp.statReg[2] = 0x0c;
		if (_vdp.model == MODEL_V9958) _vdp.statReg[1] |= 4;
		for (i=0;i<48;i++) _vdp.contReg[i] = 0;
		_vdp.cmd_write_first = _vdp.pal_write_first = 0;
		_vdp.INT = 0;
		_vdp.read_ahead = 0; _vdp.address_latch = 0; /* ??? */
		_vdp.scanline = 0;
                //v9938_set_mode ();
                //v9938_check_int ();
		}
	
	public static VhStopPtr v9938_exit = new VhStopPtr() {
            @Override
            public void handler() {
                _vdp.vram = null;
		if (_vdp.vram_exp != null) _vdp.vram_exp=null;
		if (pal_indYJK != null)
			{ pal_indYJK = null; }
            }
        };

	static void v9938_check_int ()
		{
		int n;
	
		n = ( (_vdp.contReg[1] & 0x20)!=0 && (_vdp.statReg[0] & 0x80)!=0 ) ||
			( (_vdp.statReg[1] & 0x01)!=0 && (_vdp.contReg[0] & 0x10)!=0 )?1:0;
	
		if (n != _vdp.INT)
			{
			_vdp.INT = n;
			logerror ("V9938: IRQ line %s\n", n!=0 ? "up" : "down");
			}
	
		/* 
	    ** Somehow the IRQ request is going down without cpu_irq_line () being
	    ** called; because of this Mr. Ghost, Xevious and SD Snatcher don't
	    ** run. As a patch it's called every scanline 
	    */
		_vdp.INTCallback.handler(n);
		}
	
	public static void v9938_set_sprite_limit (int i)
		{
		_vdp.sprite_limit = i;
		}
	
	public static void v9938_set_resolution (int i)
		{
		if (i == RENDER_AUTO)
			{
			_vdp.size_auto = 1;
			}
		else
			{
			_vdp.size = i;
			_vdp.size_auto = 0;
			}	
		}
	
	/***************************************************************************
	
		Register functions
	
	***************************************************************************/
	
	public static WriteHandlerPtr v9938_register_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                int reg;
	
		reg = _vdp.contReg[17] & 0x3f;
		if (reg != 17) v9938_register_write (reg, data); /* true ? */
		if ( (_vdp.contReg[17] & 0x80) == 0 )
			_vdp.contReg[17] = (_vdp.contReg[17] + 1) & 0x3f;
            }
        };
		
        static int reg_mask[] = {
			0x7e, 0x7b, 0x7f, 0xff, 0x3f, 0xff, 0x3f, 0xff,
			0xfb, 0xbf, 0x07, 0x03, 0xff, 0xff, 0x07, 0x0f,
			0x0f, 0xbf, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
			0x00, 0x7f, 0x3f, 0x07 };
        
	public static void v9938_register_write (int reg, int data)
		{
		//System.out.println("v9938_register_write: "+reg);
	
		if (reg <= 27)
			{
			data &= reg_mask[reg];
			if (_vdp.contReg[reg] == data) return;
			}
	
		if (reg > 46)
			{
			logerror ("V9938: Attempted to write to non-existant R#%d\n", reg);
			return;
			}
	
		/*v9938_update_command (); */
	
		switch (reg)
			{
			/* registers that affect interrupt and display mode */
			case 0:
			case 1:
				_vdp.contReg[reg] = data;
				v9938_set_mode ();
				v9938_check_int ();
				logerror ("V9938: mode = %s\n", v9938_modes[_vdp.mode]);
				break;
			case 18:
			case 9:
				_vdp.contReg[reg] = data;
				/* recalc offset */
				_vdp.offset_x = ( (~_vdp.contReg[18] - 8) & 0x0f);
				_vdp.offset_y = (~(_vdp.contReg[18]>>4) - 8) & 0x0f;
				if ((_vdp.contReg[9] & 0x80) != 0)
					_vdp.visible_y = 212;
				else
					{
					_vdp.visible_y = 192;
					_vdp.offset_y += 10;
					}
	
				break;
			case 15:
				_vdp.pal_write_first = 0;
				break;
			/* color burst registers aren't emulated */
			case 20:
			case 21:
			case 22:
				logerror ("V9938: Write %02xh to R#%d; color burst not emulated\n",
					data, reg);
				break;
			case 25:
			case 26:
			case 27:
				if (_vdp.model != MODEL_V9958)
					{
					logerror ("V9938: Attempting to write %02xh to V9958 R#%d\n");
					data = 0;
					}
				break;
			case 44:
				v9938_cpu_to_vdp (data);
				break;
			case 46:
				v9938_command_unit_w(data);
				break;
			}
	
		if (reg != 15)
			logerror ("V9938: Write %02x to R#%d\n", data, reg);
	
		_vdp.contReg[reg] = data;
		}
	
	public static ReadHandlerPtr v9938_status_r = new ReadHandlerPtr() {
            public int handler(int offset) {
                int reg, n;
		int ret;
	
		_vdp.cmd_write_first = 0;
	
		reg = _vdp.contReg[15] & 0x0f;
		if (reg > 9)
			return 0xff;
	
		switch (reg)
			{
			case 0:
				ret = _vdp.statReg[0];
				_vdp.statReg[0] &= 0x1f;
				break;
			case 1:
				ret = _vdp.statReg[1];
				_vdp.statReg[1] &= 0xfe;
				break;
			case 2:
				/*v9938_update_command ();*/
				n = cycles_currently_ran ();
				if ( (n < 28) || (n > 199) ) _vdp.statReg[2] |= 0x20;
				else _vdp.statReg[2] &= ~0x20;
				ret = _vdp.statReg[2];
				break;
			case 7:
				ret = _vdp.statReg[7];
				_vdp.statReg[7] = v9938_vdp_to_cpu () ;
                                _vdp.contReg[44] = v9938_vdp_to_cpu () ;
				break;
			default:
				ret = _vdp.statReg[reg];
				break;
			}
	
		logerror ("V9938: Read %02x from S#%d\n", ret, reg);
		v9938_check_int ();
	
		return ret;
            }
        };
		
	/***************************************************************************
	
		Refresh / render function
	
	***************************************************************************/
	
        public static int V9938_SECOND_FIELD(){ return ( (((_vdp.contReg[9] & 0x04)!=0 && (_vdp.statReg[2] & 2)==0) || _vdp.blink!=0))?0:1; }
	
        public static int V9938_WIDTH	= (512 + 32);
/*TODO*///	#define V9938_BPP	(8)
/*TODO*///	#undef	V9938_BPP
/*TODO*///	#define V9938_BPP	(16)
/*TODO*///	#undef 	V9938_WIDTH
/*TODO*///        public static int  V9938_WIDTH	= (256 + 16);
/*TODO*///	#undef	V9938_BPP
/*TODO*///	#define V9938_BPP	(8)
/*TODO*///	#undef	V9938_BPP
/*TODO*///	#undef 	V9938_WIDTH
	
	public static ModeSprites_HandlersPtr v9938_sprite_mode1 = new ModeSprites_HandlersPtr() {
            public void handler(int line, UShortPtr col) {

		UBytePtr attrtbl, patterntbl, patternptr;
		int x, y, p, height, c, p2, i, n, pattern;
                
	
		memset (col, 0, 256);
	
		/* are sprites disabled? */
		if ((_vdp.contReg[8] & 0x02) != 0) return;
	
		attrtbl = new UBytePtr(_vdp.vram, (_vdp.contReg[5] << 7) + (_vdp.contReg[11] << 15));
		patterntbl = new UBytePtr(_vdp.vram, (_vdp.contReg[6] << 11));
	
		/* 16x16 or 8x8 sprites */
		height = (_vdp.contReg[1] & 2)!=0 ? 16 : 8;
		/* magnified sprites (zoomed) */
		if ((_vdp.contReg[1] & 1)!=0) height *= 2;
	
		p2 = p = 0;
		while (true)
			{
			y = attrtbl.read(0);
			if (y == 208) break;
			y = (y - _vdp.contReg[23]) & 255;
			if (y > 208)
				y = -(~y&255);
			else
				y++;
	
			/* if sprite in range, has to be drawn */
			if ( (line >= y) && (line  < (y + height) ) )
				{
				if (p2 == 4)
					{
					/* max maximum sprites per line! */
					if ( (_vdp.statReg[0] & 0x40) == 0)
						_vdp.statReg[0] = (_vdp.statReg[0] & 0xa0) | 0x40 | p;
	
					if (_vdp.sprite_limit != 0) break;
					}
				/* get x */
				x = attrtbl.read(1) + 16;
				//if ((attrtbl.read(3) & 0x80)!=0) x -= 32;
	
				/* get pattern */
				pattern = attrtbl.read(2);
				if ((_vdp.contReg[1] & 2) != 0)
					pattern &= 0xfc;
				n = line - y;
				patternptr = new UBytePtr(patterntbl, pattern * 8 +
                                        n);
					//((_vdp.contReg[1] & 1)!=0 ? n/2  : n));
				pattern = patternptr.read(0) << 8 | patternptr.read(16);
	
				/* get colour */
				c = attrtbl.read(3) & 0x0f;
	
				/* draw left part */
				n = 0;
				while (true)
					{
					if (n == 0) pattern = patternptr.read(0);
					else if ( (n == 1) && (_vdp.contReg[1] & 2)!=0 ) pattern = patternptr.read(16);
					else break;
	
					n++;
	
					for (i=0;i<8;i++)
						{
						if ((pattern & 0x80) != 0)
							{
							if ( (x >= 0) && (x < 256) )
								{
								if ((col.read(x) & 0x40) != 0)
									{
									/* we have a collision! */
									if (p2 < 4)
										_vdp.statReg[0] |= 0x20;
									}
								if ( (col.read(x) & 0x80) == 0)
									{
									if (c!=0 || (_vdp.contReg[8] & 0x20)!=0 )
										col.write(x, (char) (col.read(x) | 0xc0 | c));
									else
										col.write(x, (char) (col.read(x) | 0x40));
									}
	
								/* if zoomed, draw another pixel */
								if ((_vdp.contReg[1] & 1) != 0)
									{
									if ((col.read(x+1) & 0x40) != 0)
                                                                        {
                                                                                /* we have a collision! */
										if (p2 < 4)
											_vdp.statReg[0] |= 0x20;
                                                                        }
                                                                        if ( (col.read(x+1) & 0x80) == 0)
                                                                        {
                                                                                if (c!=0 || (_vdp.contReg[8] & 0x20)!=0 )
                                                                                                                col.write(x+1, (char) (col.read(x+1) | 0xc0 | c));
                                                                                                        else
                                                                                                                col.write(x+1, (char) (col.read(x+1) | 0x80));
                                                                        }
								}
							}
						}
						if ((_vdp.contReg[1] & 1)!=0) x += 2; else x++;
						pattern <<= 1;
						}
					}
	
				p2++;
				}
	
                                if (p >= 31) break;
                                p++;
                                attrtbl.inc( 4 );
			}
	
		if ( (_vdp.statReg[0] & 0x40) == 0)
			_vdp.statReg[0] = (_vdp.statReg[0] & 0xa0) | p;
                
            }
        };
        
	public static ModeSprites_HandlersPtr v9938_sprite_mode2 = new ModeSprites_HandlersPtr() {
            @Override
            public void handler(int line, UShortPtr col) {
                //System.out.println("v9938_sprite_mode2");
                boolean goto_skip_first_cc_set = false;
		int attrtbl, patterntbl, patternptr, colourtbl;
		int x, i, y, p, height, c, p2, n, pattern, colourmask, first_cc_seen;
	
		memset (col, 0, 256);
	
		/* are sprites disabled? */
		if ((_vdp.contReg[8] & 0x02) != 0) return;
	
		attrtbl = ( (_vdp.contReg[5] & 0xfc) << 7) + (_vdp.contReg[11] << 15);
		colourtbl =  ( (_vdp.contReg[5] & 0xf8) << 7) + (_vdp.contReg[11] << 15);
		patterntbl = (_vdp.contReg[6] << 11);
		colourmask = ( (_vdp.contReg[5] & 3) << 3) | 0x7; /* check this! */
	
		/* 16x16 or 8x8 sprites */
		height = (_vdp.contReg[1] & 2)!=0 ? 16 : 8;
		/* magnified sprites (zoomed) */
		if ((_vdp.contReg[1] & 1) != 0) 
                    height *= 2;
	
		p2 = p = first_cc_seen = 0;
		while (true)
			{
			y = v9938_vram_read (attrtbl);
			if (y == 216) break;
			y = (y - _vdp.contReg[23]) & 255;
			if (y > 216)
				y = -(~y&255);
			else
				y++;
	
			/* if sprite in range, has to be drawn */
			if ( (line >= y) && (line  < (y + height) ) )
				{
				if (p2 == 8)
					{
					/* max maximum sprites per line! */
					if ( (_vdp.statReg[0] & 0x40) == 0 )
						_vdp.statReg[0] = (_vdp.statReg[0] & 0xa0) | 0x40 | p;
	
					if (_vdp.sprite_limit != 0) break;
					}
	
				n = line - y; 
                                if ((_vdp.contReg[1] & 1)!=0) 
                                    n /= 2;
				/* get colour */
				c = v9938_vram_read (colourtbl + (((p&colourmask)*16) + n));
	
				/* don't draw all sprite with CC set before any sprites 
	               with CC = 0 are seen on this line */
				if ((c & 0x40) != 0)
				{
					if (first_cc_seen == 0){
                                            System.out.println("skip_first_cc_set");
/*TODO*///						skip_first_cc_set();
                                                goto_skip_first_cc_set = true;
                                                break;
					}
                                } else {
					first_cc_seen = 1;
                                }
	if (!goto_skip_first_cc_set){
				/* get pattern */
				pattern = v9938_vram_read (attrtbl + 2);
				if ((_vdp.contReg[1] & 2) != 0)
					pattern &= 0xfc;
				patternptr = patterntbl + pattern * 8 + n;
				pattern = (v9938_vram_read (patternptr) << 8) |
					v9938_vram_read (patternptr + 16);
	
				/* get x */
				x = v9938_vram_read (attrtbl + 1) + 16;
				if ((c & 0x80)!=0) x -= 32;
	
				n = (_vdp.contReg[1] & 2)!=0 ? 16 : 8;
				while (n-- != 0)
					{
					for (i=0;i<=(_vdp.contReg[1] & 1);i++)
						{
						if ( (x >= 0) && (x < 256) )
							{
							if ( (pattern & 0x8000)!=0 && (col.read(x) & 0x10)==0 )
								{
								if ( (c & 15)!=0 || (_vdp.contReg[8] & 0x20)!=0 ) 
									{
									if ( (c & 0x40)==0 )
										{
										if ((col.read(x) & 0x20)!=0) col.write(x, (char) (col.read(x) | 0x10));
										else 
											col.write(x, (char) (col.read(x) | 0x20 | (c & 15)));
										}
									else
										col.write(x, (char) (col.read(x)| c & 15));
	
									col.write(x, (char) (col.read(x) | 0x80));
									}
								}
							else
								{
								if ( (c & 0x40)==0 && (col.read(x) & 0x20)!=0 )
									col.write(x, (char) (col.read(x) | 0x10));
								}
	
							if ( (c & 0x60)==0 && (pattern & 0x8000)!=0 )
								{
								if ((col.read(x) & 0x40) != 0)
									{
									/* sprite collision! */
									if (p2 < 8)
										_vdp.statReg[0] |= 0x20;
									}
								else
									col.write(x, (char) (col.read(x) | 0x40));
								}
	
							x++;
							}
						}
	
					pattern <<= 1;
					}
        }
/*TODO*///	skip_first_cc_set:
                                goto_skip_first_cc_set = false;
				p2++;
				}
	
			if (p >= 31) break;
			p++;
			attrtbl += 4;
			}
	
		if ( (_vdp.statReg[0] & 0x40) == 0 )
			_vdp.statReg[0] = (_vdp.statReg[0] & 0xa0) | p;                
            }
        };
        
	public static class V9938_MODE {
		public int m;
		public ModeVisible_8_HandlersPtr visible_8 = null;
		public ModeVisible_16_HandlersPtr visible_16 = null;
		public ModeVisible_8_HandlersPtr visible_8s = null;
		public ModeVisible_16_HandlersPtr visible_16s = null;
		public ModeBorder_8_HandlersPtr border_8 = null;
		public ModeBorder_16_HandlersPtr border_16 = null;
		public ModeBorder_8_HandlersPtr border_8s = null;
		public ModeBorder_16_HandlersPtr border_16s = null;
		public ModeSprites_HandlersPtr sprites = null;
		public ModeDraw_Sprites_HandlersPtr draw_sprite_8 = null;
		public ModeDraw_Sprites_HandlersPtr draw_sprite_16 = null;
		public ModeDraw_Sprites_HandlersPtr draw_sprite_8s = null;
		public ModeDraw_Sprites_HandlersPtr draw_sprite_16s = null;
                
                public V9938_MODE(int m, 
                        ModeVisible_8_HandlersPtr visible_8, 
                        ModeVisible_16_HandlersPtr visible_16,
                        ModeVisible_8_HandlersPtr visible_8s,
                        ModeVisible_16_HandlersPtr visible_16s,
                        ModeBorder_8_HandlersPtr border_8,
                        ModeBorder_16_HandlersPtr border_16,
                        ModeBorder_8_HandlersPtr border_8s,
                        ModeBorder_16_HandlersPtr border_16s,
                        ModeSprites_HandlersPtr sprites,
                        ModeDraw_Sprites_HandlersPtr draw_sprite_8,
                        ModeDraw_Sprites_HandlersPtr draw_sprite_16,
                        ModeDraw_Sprites_HandlersPtr draw_sprite_8s,
                        ModeDraw_Sprites_HandlersPtr draw_sprite_16s)
                {
                    this.m = m;
                    this.visible_8 = visible_8;
                    this.visible_16 = visible_16;
                    this.visible_8s = visible_8s;
                    this.visible_16s = visible_16s;
                    this.border_8 = border_8;
                    this.border_16 = border_16;
                    this.border_8s = border_8s;
                    this.border_16s = border_16s;
                    this.sprites = sprites;
                    this.draw_sprite_8 = draw_sprite_8;
                    this.draw_sprite_16 = draw_sprite_16;
                    this.draw_sprite_8s = draw_sprite_8s;
                    this.draw_sprite_16s = draw_sprite_16s;
                }
	};
        
        static V9938_MODE modeMerged = new V9938_MODE(0xffff, 
                v9938_mode_text1_8, v9938_mode_graphic23_16,
			v9938_mode_graphic23_8s, v9938_mode_graphic23_16s,
			v9938_default_border_8, v9938_default_border_16,
			v9938_default_border_8s, v9938_default_border_16s,
			v9938_sprite_mode1, 
			v9938_default_draw_sprite_8, v9938_default_draw_sprite_16,
			v9938_default_draw_sprite_8s, v9938_default_draw_sprite_16s);
	
	static V9938_MODE modes[] = {
                //modeMerged,
		new V9938_MODE( 0x02,
			v9938_mode_text1_8, v9938_mode_text1_16,
			v9938_mode_text1_8s, v9938_mode_text1_16s,
			v9938_default_border_8, v9938_default_border_16,
			v9938_default_border_8s, v9938_default_border_16s,
			null, 
			null, null,
			null, null ),
		new V9938_MODE( 0x01,
			v9938_mode_multi_8, v9938_mode_multi_16,
			v9938_mode_multi_8s, v9938_mode_multi_16s,
			v9938_default_border_8, v9938_default_border_16,
			v9938_default_border_8s, v9938_default_border_16s,
			v9938_sprite_mode1, 
			v9938_default_draw_sprite_8, v9938_default_draw_sprite_16,
			v9938_default_draw_sprite_8s, v9938_default_draw_sprite_16s ),
		new V9938_MODE( 0x00,
			v9938_mode_graphic1_8, v9938_mode_graphic1_16,
			v9938_mode_graphic1_8s, v9938_mode_graphic1_16s,
			v9938_default_border_8, v9938_default_border_16,
			v9938_default_border_8s, v9938_default_border_16s,
			v9938_sprite_mode1, 
			v9938_default_draw_sprite_8, v9938_default_draw_sprite_16,
			v9938_default_draw_sprite_8s, v9938_default_draw_sprite_16s ),
		new V9938_MODE( 0x04,
			v9938_mode_graphic23_8, v9938_mode_graphic23_16,
			v9938_mode_graphic23_8s, v9938_mode_graphic23_16s,
			v9938_default_border_8, v9938_default_border_16,
			v9938_default_border_8s, v9938_default_border_16s,
			v9938_sprite_mode1, 
			v9938_default_draw_sprite_8, v9938_default_draw_sprite_16,
			v9938_default_draw_sprite_8s, v9938_default_draw_sprite_16s ),
		new V9938_MODE( 0x08,
			v9938_mode_graphic23_8, v9938_mode_graphic23_16,
			v9938_mode_graphic23_8s, v9938_mode_graphic23_16s,
			v9938_default_border_8, v9938_default_border_16,
			v9938_default_border_8s, v9938_default_border_16s,
			v9938_sprite_mode2, 
			v9938_default_draw_sprite_8, v9938_default_draw_sprite_16,
			v9938_default_draw_sprite_8s, v9938_default_draw_sprite_16s ),
		new V9938_MODE( 0x0c,
			v9938_mode_graphic4_8, v9938_mode_graphic4_16,
			v9938_mode_graphic4_8s, v9938_mode_graphic4_16s,
			v9938_default_border_8, v9938_default_border_16,
			v9938_default_border_8s, v9938_default_border_16s,
			v9938_sprite_mode2, 
			v9938_default_draw_sprite_8, v9938_default_draw_sprite_16,
			v9938_default_draw_sprite_8s, v9938_default_draw_sprite_16s ),
		new V9938_MODE( 0x10,
                        v9938_mode_graphic5_8, v9938_mode_graphic5_16,
			v9938_mode_graphic5_8s, v9938_mode_graphic5_16s,
			v9938_graphic5_border_8, v9938_graphic5_border_16,
			v9938_graphic5_border_8s, v9938_graphic5_border_16s,
			v9938_sprite_mode2, 
			v9938_graphic5_draw_sprite_8, v9938_graphic5_draw_sprite_16,
			v9938_graphic5_draw_sprite_8s, v9938_graphic5_draw_sprite_16s ),
		new V9938_MODE( 0x14,
			v9938_mode_graphic6_8, v9938_mode_graphic6_16,
			v9938_mode_graphic6_8s, v9938_mode_graphic6_16s,
			v9938_default_border_8, v9938_default_border_16,
			v9938_default_border_8s, v9938_default_border_16s,
			v9938_sprite_mode2, 
			v9938_default_draw_sprite_8, v9938_default_draw_sprite_16,
			v9938_default_draw_sprite_8s, v9938_default_draw_sprite_16s ),
		new V9938_MODE( 0x1c,
			v9938_mode_graphic7_8, v9938_mode_graphic7_16,
			v9938_mode_graphic7_8s, v9938_mode_graphic7_16s,
			v9938_graphic7_border_8, v9938_graphic7_border_16,
			v9938_graphic7_border_8s, v9938_graphic7_border_16s,
			v9938_sprite_mode2, 
			v9938_graphic7_draw_sprite_8, v9938_graphic7_draw_sprite_16,
			v9938_graphic7_draw_sprite_8s, v9938_graphic7_draw_sprite_16s ),
		new V9938_MODE( 0x0a,
			v9938_mode_text2_8, v9938_mode_text2_16,
			v9938_mode_text2_8s, v9938_mode_text2_16s,
			v9938_default_border_8, v9938_default_border_16,
			v9938_default_border_8s, v9938_default_border_16s,
			null, 
			null, null,
			null, null ),
		new V9938_MODE( 0xff,
			v9938_mode_unknown_8, v9938_mode_unknown_16,
			v9938_mode_unknown_8s, v9938_mode_unknown_16s,
			v9938_default_border_8, v9938_default_border_16,
			v9938_default_border_8s, v9938_default_border_16s,
			null, 
			null, null,
			null, null )
	};
	
	static void v9938_set_mode ()
	{
		int n,i;
	
		n = (((_vdp.contReg[0] & 0x0e) << 1) | ((_vdp.contReg[1] & 0x18) >> 3));
                
                //System.out.println("SET MODE: "+n);
                
                //n = 0x02;
                
                //System.out.println("Long: "+modes.length);
                
		
                for (i=0;i<modes.length;i++)
                {
                    if ( (modes[i].m == n) || (modes[i].m == 0xff) ) break;
                }

		_vdp.mode = i;
                
                
                
	}
	
	static void v9938_refresh_8 (mame_bitmap bmp, int line)
		{
                    //System.out.println("v9938_refresh_8");
		int i, double_lines;
		int[] col=new int[256];
                UBytePtr ln=null, ln2 = null;
	
		double_lines = 0;
	
		if (_vdp.size == RENDER_HIGH)
			{
			if ((_vdp.contReg[9] & 0x08) != 0)
				{
				_vdp.size_now = RENDER_HIGH;
				ln = bmp.line[line*2+((_vdp.statReg[2]>>1)&1)];
				}
			else
				{
				ln = bmp.line[line*2];
				ln2 = bmp.line[line*2+1];
				double_lines = 1;
				}
			}
		else
			ln = bmp.line[line];
	
		if ( (_vdp.contReg[1] & 0x40)==0 || (_vdp.statReg[2] & 0x40)!=0 )
			{
			if (_vdp.size == RENDER_HIGH)
				modes[_vdp.mode].border_8.handler(ln);
			else
				modes[_vdp.mode].border_8s.handler(ln);
			}
		else
			{
			i = (line - _vdp.offset_y) & 255;
			if (_vdp.size == RENDER_HIGH)
				{
				modes[_vdp.mode].visible_8.handler(ln, i);
				if (modes[_vdp.mode].sprites != null)
					{
                                            System.out.println("Need to be implemented A");
/*TODO*///					modes[_vdp.mode].sprites.handler(i, col);
/*TODO*///					modes[_vdp.mode].draw_sprite_8.handler(ln, col);
					}
				}
			else
				{
                                    System.out.println("Need to be implemented B");
				/*TODO*///modes[_vdp.mode].visible_8s (ln, i);
				/*TODO*///if (modes[_vdp.mode].sprites)
				/*TODO*///	{
				/*TODO*///	modes[_vdp.mode].sprites (i, col);
				/*TODO*///	modes[_vdp.mode].draw_sprite_8s (ln, col);
				/*TODO*///	}
				}
			}
	
		if (double_lines != 0)
			memcpy (ln2, ln, (512 + 32) * 2);
		}
	
	static void v9938_refresh_16 (mame_bitmap bmp, int line)
		{
                    //System.out.println("v9938_refresh_16");
		int i, double_lines;
		UShortPtr col=new UShortPtr(256);
		UShortPtr ln, ln2 = null;
	
		double_lines = 0;
	
		if (_vdp.size == RENDER_HIGH)
			{
			if ((_vdp.contReg[9] & 0x08)!=0)
				{
				_vdp.size_now = RENDER_HIGH;
			ln = new UShortPtr(bmp.line[line*2+((_vdp.statReg[2]>>1)&1)]);
				}
			else
				{
				ln = new UShortPtr(bmp.line[line*2]);
				ln2 = new UShortPtr(bmp.line[line*2+1]);
				double_lines = 1;
				}
                        
			}
		else
			ln = new UShortPtr(bmp.line[line*2]);
	
		if ( (_vdp.contReg[1] & 0x40)==0 || (_vdp.statReg[2] & 0x40)!=0 )
			{
			if (_vdp.size == RENDER_HIGH)
				modes[_vdp.mode].border_16.handler(ln);
			else
				modes[_vdp.mode].border_16s.handler(ln);
			}
		else
			{
			i = (line - _vdp.offset_y) & 255;
			if (_vdp.size == RENDER_HIGH)
				{
				modes[_vdp.mode].visible_16.handler(ln, i);
				if (modes[_vdp.mode].sprites != null)
					{
					modes[_vdp.mode].sprites.handler(i, col);
					modes[_vdp.mode].draw_sprite_16.handler(ln, col);
					}
				}
			else
				{
				modes[_vdp.mode].visible_16s.handler(ln, i);
				if (modes[_vdp.mode].sprites != null)
					{
					modes[_vdp.mode].sprites.handler(i, col);
					modes[_vdp.mode].draw_sprite_16s.handler(ln, col);
					}
				}
			}
                
		if (double_lines != 0)
			memcpy (ln2, ln, (512 + 32) * 2);
                
		}
	
	static void v9938_refresh_line (mame_bitmap bmp, int line)
		{
		int ind16, ind256;
	
		ind16 = pal_ind16[0];
		ind256 = pal_ind256[0];
	
		if ( (_vdp.contReg[8] & 0x20)==0 && (_vdp.mode != V9938_MODE_GRAPHIC5) )
			{
			pal_ind16[0] = pal_ind16[(_vdp.contReg[7] & 0x0f)];
			pal_ind256[0] = pal_ind256[_vdp.contReg[7]];
			}
	
		if (Machine.scrbitmap.depth == 8){
			v9938_refresh_8 (bmp, line);
                } else {
			v9938_refresh_16 (bmp, line);
                }
	
		if ( (_vdp.contReg[8] & 0x20)==0 && (_vdp.mode != V9938_MODE_GRAPHIC5) )
			{
			pal_ind16[0] = (char) ind16;
			pal_ind256[0] = (char) ind256;
			}
		}
	
	public static VhUpdatePtr v9938_refresh = new VhUpdatePtr() {
            public void handler(mame_bitmap bitmap, int full_refresh) {
                /* already been rendered, since we're using scanline stuff */
            }
        };
		
	/*
	
	From: awulms@inter.nl.net (Alex Wulms)
	*** About the HR/VR topic: this is how it works according to me:
	
	*** HR:
	HR is very straightforward:
	-HR=1 during 'display time'
	-HR=0 during 'horizontal border, horizontal retrace'
	I have put 'display time' and 'horizontal border, horizontal retrace' between
	quotes because HR does not only flip between 0 and 1 during the display of
	the 192/212 display lines, but also during the vertical border and during the
	vertical retrace.
	
	*** VR:
	VR is a little bit tricky
	-VR always gets set to 0 when the VDP starts with display line 0
	-VR gets set to 1 when the VDP reaches display line (192 if LN=0) or (212 if
	LN=1)
	-The VDP displays contents of VRAM as long as VR=0
	
	As a consequence of this behaviour, it is possible to program the famous
	overscan trick, where VRAM contents is shown in the borders:
	Generate an interrupt at line 230 (or so) and on this interrupt: set LN=1
	Generate an interrupt at line 200 (or so) and on this interrupt: set LN=0
	Repeat the above two steps
	
	*** The top/bottom border contents during overscan:
	On screen 0:
	1) The VDP keeps increasing the name table address pointer during bottom
	border, vertical retrace and top border
	2) The VDP resets the name table address pointer when the first display line
	is reached
	
	On the other screens:
	1) The VDP keeps increasing the name table address pointer during the bottom
	border
	2) The VDP resets the name table address pointer such that the top border
	contents connects up with the first display line. E.g., when the top border
	is 26 lines high, the VDP will take:
	'logical'      vram line
	TOPB000  256-26
	...
	TOPB025  256-01
	DISPL000 000
	...
	DISPL211 211
	BOTB000  212
	...
	BOTB024  236
	
	
	
	*** About the horizontal interrupt
	
	All relevant definitions on a row:
	-FH: Bit 0 of status register 1
	-IE1: Bit 4 of mode register 0
	-IL: Line number in mode register 19
	-DL: The line that the VDP is going to display (corrected for vertical scroll)
	-IRQ: Interrupt request line of VDP to Z80
	
	At the *start* of every new line (display, bottom border, part of vertical
	display), the VDP does:
	-FH = (FH && IE1) || (IL==DL)
	
	After reading of status register 1 by the CPU, the VDP does:
	-FH = 0
	
	Furthermore, the following is true all the time:
	-IRQ = FH && IE1
	
	The resulting behaviour:
	When IE1=0:
	-FH will be set as soon as display of line IL starts
	-FH will be reset as soon as status register 1 is read
	-FH will be reset as soon as the next display line is reached
	
	When IE=1:
	-FH and IRQ will be set as soon as display line IL is reached
	-FH and IRQ will be reset as soon as status register 1 is read
	
	Another subtile result:
	If, while FH and IRQ are set, IE1 gets reset, the next happens:
	-IRQ is reset immediately (since IRQ is always FH && IE1)
	-FH will be reset as soon as display of the next line starts (unless the next
	line is line IL)
	
	
	*** About the vertical interrupt:
	Another relevant definition:
	-FV: Bit 7 of status register 0
	-IE0: Bit 5 of mode register 1
	
	I only know for sure the behaviour when IE0=1:
	-FV and IRQ will be set as soon as VR changes from 0 to 1
	-FV and IRQ will be reset as soon as status register 0 is read
	
	A consequence is that NO vertical interrupts will be generated during the
	overscan trick, described in the VR section above.
	
	I do not know the behaviour of FV when IE0=0. That is the part that I still
	have to test.
	*/
	
	public static void v9938_interrupt_start_vblank ()
		{
/*TODO*///	#if 0
/*TODO*///		if (keyboard_pressed (KEYCODE_D) )
/*TODO*///			{
/*TODO*///			FILE *fp;
/*TODO*///			int i;
/*TODO*///	
/*TODO*///			fp = fopen ("vram.dmp", "wb");
/*TODO*///			if (fp)
/*TODO*///				{
/*TODO*///				fwrite (_vdp.vram, 0x10000, 1, fp);
/*TODO*///				fclose (fp);
/*TODO*///				usrintf_showmessage ("saved");
/*TODO*///				}
/*TODO*///	
/*TODO*///			for (i=0;i<24;i++) printf ("R#%d = %02x\n", i, _vdp.contReg[i]);
/*TODO*///			}
/*TODO*///	#endif
	
		/* at every frame, vdp switches fields */
		_vdp.statReg[2] = (_vdp.statReg[2] & 0xfd) | (~_vdp.statReg[2] & 2);
	
		/* color blinking */
		if ((_vdp.contReg[13] & 0xf0)==0)
			_vdp.blink = 0;
		else if ((_vdp.contReg[13] & 0x0f) == 0)
			_vdp.blink = 1;
		else
			{
			/* both on and off counter are non-zero: timed blinking */
			if (_vdp.blink_count != 0)
				_vdp.blink_count--;
			if (_vdp.blink_count == 0)
				{
				_vdp.blink = _vdp.blink!=0?0:1;
				if (_vdp.blink != 0)
					_vdp.blink_count = (_vdp.contReg[13] >> 4) * 10;
				else
					_vdp.blink_count = (_vdp.contReg[13] & 0x0f) * 10;
				}
			}
	
		/* check screen rendering size */
		if (_vdp.size_auto!=0 && (_vdp.size_now >= 0) && (_vdp.size != _vdp.size_now) )
			_vdp.size = _vdp.size_now;
	
		if (_vdp.size != _vdp.size_old)
			{
			if (_vdp.size == RENDER_HIGH)
				osd_set_visible_area (0, 512 + 32 - 1, 0, 424 + 32 - 1);
			else
				osd_set_visible_area (0, 256 + 16 - 1, 0, 212 + 16 - 1);
	
			_vdp.size_old = _vdp.size;
			}
	
		_vdp.size_now = -1;
		}
	
	public static int v9938_interrupt ()
		{
                    //System.out.println("v9938_interrupt");
                    
		UShortPtr col=new UShortPtr(256*2);
		int scanline, max, pal, scanline_start;

                v9938_update_command ();

                pal = _vdp.contReg[9] & 2;
                if (pal != 0) scanline_start = 53; else scanline_start = 22;

                /* set flags */
                if (_vdp.scanline == (_vdp.offset_y + scanline_start) )
                        {
                        _vdp.statReg[2] &= ~0x40;
                        }
                else if (_vdp.scanline == (_vdp.offset_y + _vdp.visible_y + scanline_start) )
                        {
                        _vdp.statReg[2] |= 0x40;
                        _vdp.statReg[0] |= 0x80;
                        }

                max = (pal!=0) ? 255 : (_vdp.contReg[9] & 0x80)!=0 ? 234 : 244;
                scanline = (_vdp.scanline - scanline_start - _vdp.offset_y);
                if ( (scanline >= 0) && (scanline <= max) &&
                   ( ( (scanline + _vdp.contReg[23]) & 255) == _vdp.contReg[19]) )
                        {
                        _vdp.statReg[1] |= 1;
                        logerror ("V9938: scanline interrupt (%d)\n", scanline);
                        }
                else
                        if ( (_vdp.contReg[0] & 0x10) == 0 ) _vdp.statReg[1] &= 0xfe;

                v9938_check_int ();

                /* check for start of vblank */
                if ((pal!=0 && (_vdp.scanline == 310)) ||
                        (pal==0 && (_vdp.scanline == 259)))
                        v9938_interrupt_start_vblank ();

                /* render the current line */
                if ((_vdp.scanline >= scanline_start) && (_vdp.scanline < (212 + 28 + scanline_start)))
                        {
                        scanline = (_vdp.scanline - scanline_start) & 255;

                        if (osd_skip_this_frame () != 0 )
                                {
                                if ( (_vdp.statReg[2] & 0x40)==0 && (modes[_vdp.mode].sprites)!=null )
                                        modes[_vdp.mode].sprites.handler((scanline - _vdp.offset_y) & 255, col);
                                }
                        else
                                {
                                v9938_refresh_line (Machine.scrbitmap, scanline);
                                }
                        }

                max = (_vdp.contReg[9] & 2)!=0 ? 313 : 262;
                if (++_vdp.scanline == max)
                        _vdp.scanline = 0;

                return _vdp.INT;
                    
		}
/*TODO*///	
/*TODO*///	/***************************************************************************
/*TODO*///	
/*TODO*///		Command unit
/*TODO*///	
/*TODO*///	***************************************************************************/
/*TODO*///	
/*TODO*///	public static int VDP(){ return _vdp.contReg;}
/*TODO*///	public static int VDPStatus(int pos){return  _vdp.statReg[pos];}
/*TODO*///	#define VRAM _vdp.vram
	public static int ScrMode(){return  _vdp.mode;}
	
	/*************************************************************/
	/** Completely rewritten by Alex Wulms:                     **/
	/**  - VDP Command execution 'in parallel' with CPU         **/
	/**  - Corrected behaviour of VDP commands                  **/
	/**  - Made it easier to implement correct S7/8 mapping     **/
	/**    by concentrating VRAM access in one single place     **/
	/**  - Made use of the 'in parallel' VDP command exec       **/
	/**    and correct timing. You must call the function       **/
	/**    LoopVDP() from LoopZ80 in MSX.c. You must call it    **/
	/**    exactly 256 times per screen refresh.                **/
	/** Started on       : 11-11-1999                           **/
	/** Beta release 1 on:  9-12-1999                           **/
	/** Beta release 2 on: 20-01-2000                           **/
	/**  - Corrected behaviour of VRM <. Z80 transfer          **/
	/**  - Improved performance of the code                     **/
	/** Public release 1.0: 20-04-2000                          **/
	/*************************************************************/
	
        public static UBytePtr VDP_VRMP5(int X, int Y){ return new UBytePtr(_vdp.vram, ((Y&1023)<<7) + ((X&255)>>1)); }
        public static UBytePtr VDP_VRMP6(int X, int Y){ return new UBytePtr(_vdp.vram, ((Y&1023)<<7) + ((X&511)>>2)); }
/*TODO*///	//#define VDP_VRMP7(X, Y) (VRAM + ((Y&511)<<8) + ((X&511)>>1))
        public static UBytePtr VDP_VRMP7(int X, int Y){ return new UBytePtr(_vdp.vram, ((X&2)<<15) + ((Y&511)<<7) + ((X&511)>>2)); }
/*TODO*///	//#define VDP_VRMP8(X, Y) (VRAM + ((Y&511)<<8) + (X&255))
        public static UBytePtr VDP_VRMP8(int X, int Y){ return new UBytePtr(_vdp.vram, ((X&1)<<16) + ((Y&511)<<7) + ((X>>1)&127)); }
	
        public static UBytePtr VDP_VRMP(int M, int X, int Y){ return VDPVRMP(M, X, Y); }
        public static int VDP_POINT(int M, int X, int Y){ return VDPpoint(M, X, Y); }
        public static void VDP_PSET(int M, int X, int Y, int C, int O){ VDPpset(M, X, Y, C, O); }
	
        public static final int CM_ABRT  = 0x0;
        public static final int CM_POINT = 0x4;
        public static final int CM_PSET  = 0x5;
        public static final int CM_SRCH  = 0x6;
        public static final int CM_LINE  = 0x7;
        public static final int CM_LMMV  = 0x8;
        public static final int CM_LMMM  = 0x9;
        public static final int CM_LMCM  = 0xA;
        public static final int CM_LMMC  = 0xB;
        public static final int CM_HMMV  = 0xC;
        public static final int CM_HMMM  = 0xD;
        public static final int CM_YMMM  = 0xE;
        public static final int CM_HMMC  = 0xF;

/*TODO*///	/*************************************************************/
/*TODO*///	/* Many VDP commands are executed in some kind of loop but   */
/*TODO*///	/* essentially, there are only a few basic loop structures   */
/*TODO*///	/* that are re-used. We define the loop structures that are  */
/*TODO*///	/* re-used here so that they have to be entered only once    */
/*TODO*///	/*************************************************************/
/*TODO*///	#define pre_loop \
/*TODO*///	    while ((cnt-=delta) > 0) {
/*TODO*///	
/*TODO*///	
/*TODO*///	/* Loop over DX, DY */
/*TODO*///	#define post__x_y(MX) \
/*TODO*///	    if (!--ANX || ((ADX+=TX)&MX)) { \
/*TODO*///	      if (!(--NY&1023) || (DY+=TY)==-1) \
/*TODO*///	        break; \
/*TODO*///	      else { \
/*TODO*///	        ADX=DX; \
/*TODO*///	        ANX=NX; \
/*TODO*///	      } \
/*TODO*///	    } \
/*TODO*///	  }
/*TODO*///	
/*TODO*///	/* Loop over DX, SY, DY */
/*TODO*///	#define post__xyy(MX) \
/*TODO*///	    if ((ADX+=TX)&MX) { \
/*TODO*///	      if (!(--NY&1023) || (SY+=TY)==-1 || (DY+=TY)==-1) \
/*TODO*///	        break; \
/*TODO*///	      else \
/*TODO*///	        ADX=DX; \
/*TODO*///	    } \
/*TODO*///	  }
/*TODO*///	
/*TODO*///	/* Loop over SX, DX, SY, DY */
/*TODO*///	#define post_xxyy(MX) \
/*TODO*///	    if (!--ANX || ((ASX+=TX)&MX) || ((ADX+=TX)&MX)) { \
/*TODO*///	      if (!(--NY&1023) || (SY+=TY)==-1 || (DY+=TY)==-1) \
/*TODO*///	        break; \
/*TODO*///	      else { \
/*TODO*///	        ASX=SX; \
/*TODO*///	        ADX=DX; \
/*TODO*///	        ANX=NX; \
/*TODO*///	      } \
/*TODO*///	    } \
/*TODO*///	  }
	
	/*************************************************************/
	/** Structures and stuff                                    **/
	/*************************************************************/
	public static class _MMC {
	  public int SX,SY;
	  public int DX,DY;
	  public int TX,TY;
	  public int NX,NY;
	  public int MX;
	  public int ASX,ADX,ANX;
	  public int CL;
	  public int LO;
	  public int CM;
	};
        
        public static _MMC MMC = new _MMC();
	

	
	/*************************************************************/
	/** Variables visible only in this module                   **/
	/*************************************************************/
	static int Mask[] = { 0x0F,0x03,0x0F,0xFF };
	static int  PPB[]  = { 2,4,2,1 };
	static int  PPL[]  = { 256,512,512,256 };
	public static int  VdpOpsCnt=1;
        public static _vdpEngine VdpEngine = null;
	
/*TODO*///	                      /*  SprOn SprOn SprOf SprOf */
/*TODO*///	                      /*  ScrOf ScrOn ScrOf ScrOn */
/*TODO*///	static int srch_timing[8]={ 818, 1025,  818,  830,   /* ntsc */
/*TODO*///	                            696,  854,  696,  684 }; /* pal  */
	static int line_timing[]={ 1063, 1259, 1063, 1161,
	                            904,  1026, 904,  953 };
	static int hmmv_timing[]={ 439,  549,  439,  531,
	                            366,  439,  366,  427 };
	static int lmmv_timing[]={ 873,  1135, 873, 1056,
	                            732,  909,  732,  854 };
	static int ymmm_timing[]={ 586,  952,  586,  610,
	                            488,  720,  488,  500 };
	static int hmmm_timing[]={ 818,  1111, 818,  854,
	                            684,  879,  684,  708 };
	static int lmmm_timing[]={ 1160, 1599, 1160, 1172,
	                            964,  1257, 964,  977 };
	
	
	/** VDPVRMP() **********************************************/
	/** Calculate addr of a pixel in vram                       **/
	/*************************************************************/
	public static UBytePtr VDPVRMP(int M,int X,int Y)
	{
	  switch(M)
	  {
	    case 0: return VDP_VRMP5(X,Y);
	    case 1: return VDP_VRMP6(X,Y);
	    case 2: return VDP_VRMP7(X,Y);
	    case 3: return VDP_VRMP8(X,Y);
	  }
	
	  return(_vdp.vram);
	}
	
	/** VDPpoint5() ***********************************************/
	/** Get a pixel on screen 5                                 **/
	/*************************************************************/
	public static int VDPpoint5(int SX, int SY)
	{
	  return ((VDP_VRMP5(SX, SY)).read() >>
	          (((~SX)&1)<<2)
	         )&15;
	}
	
	/** VDPpoint6() ***********************************************/
	/** Get a pixel on screen 6                                 **/
	/*************************************************************/
	public static int VDPpoint6(int SX, int SY)
	{
	  return ((VDP_VRMP6(SX, SY)).read() >>
	          (((~SX)&3)<<1)
	         )&3;
	}
	
	/** VDPpoint7() ***********************************************/
	/** Get a pixel on screen 7                                 **/
	/*************************************************************/
	public static int VDPpoint7(int SX, int SY)
	{
	  return ((VDP_VRMP7(SX, SY)).read() >>
	          (((~SX)&1)<<2)
	         )&15;
	}
	
	/** VDPpoint8() ***********************************************/
	/** Get a pixel on screen 8                                 **/
	/*************************************************************/
	public static int VDPpoint8(int SX, int SY)
	{
	  return (VDP_VRMP8(SX, SY)).read();
	}
	
	/** VDPpoint() ************************************************/
	/** Get a pixel on a screen                                 **/
	/*************************************************************/
	public static int VDPpoint(int SM, int SX, int SY)
	{
	  switch(SM)
	  {
	    case 0: return VDPpoint5(SX,SY);
	    case 1: return VDPpoint6(SX,SY);
	    case 2: return VDPpoint7(SX,SY);
	    case 3: return VDPpoint8(SX,SY);
	  }
	
	  return(0);
	}
	
	/** VDPpsetlowlevel() ****************************************/
	/** Low level function to set a pixel on a screen           **/
	/** Make it inline to make it fast                          **/
	/*************************************************************/
	public static void VDPpsetlowlevel(UBytePtr P, int CL, int M, int OP)
	{
          //System.out.println("VDPpsetlowlevel="+OP);
	  switch (OP)
	  {
	    case 0: P.write((P.read() & M) | CL); break;
	    case 1: P.write( P.read() & (CL | M)); break;
	    case 2: P.write( P.read()| CL ); break;
	    case 3: P.write( P.read()^ CL); break;
	    case 4: P.write( (P.read() & M) | ~(CL | M) ); break;
	    case 8: if (CL != 0) P.write( (P.read() & M) | CL ); break;
	    case 9: if (CL != 0) P.write( P.read() & (CL | M) ); break;
	    case 10:if (CL != 0) P.write( P.read() | CL ); break;
	    case 11:if (CL != 0) P.write( P.read() ^ CL ); break;
	    case 12:if (CL != 0) P.write( (P.read() & M) | ~(CL|M)); break;
	  }
	}
	
	/** VDPpset5() ***********************************************/
	/** Set a pixel on screen 5                                 **/
	/*************************************************************/
	public static void VDPpset5(int DX, int DY, int CL, int OP)
	{
	  int SH = ((~DX)&1)<<2;
	
	  VDPpsetlowlevel(VDP_VRMP5(DX, DY),
	                  CL << SH, ~(15<<SH), OP);
	}
	
	/** VDPpset6() ***********************************************/
	/** Set a pixel on screen 6                                 **/
	/*************************************************************/
	public static void VDPpset6(int DX, int DY, int CL, int OP)
	{
            //System.out.println("VDPpset6");
	  int SH = ((~DX)&3)<<1;
          
          UBytePtr P = new UBytePtr(_vdp.vram, ((DY&1023)<<7) + ((DX&511)>>2));
	
	  //VDPpsetlowlevel(P,CL << SH, ~(3<<SH), OP);
          P.write((P.read() & ~(3<<SH)) | CL<<SH);
	}
	
	/** VDPpset7() ***********************************************/
	/** Set a pixel on screen 7                                 **/
	/*************************************************************/
	public static void VDPpset7(int DX, int DY, int CL, int OP)
	{
	  int SH = ((~DX)&1)<<2;
	
	  VDPpsetlowlevel(VDP_VRMP7(DX, DY),
	                  CL << SH, ~(15<<SH), OP);
	}
	
	/** VDPpset8() ***********************************************/
	/** Set a pixel on screen 8                                 **/
	/*************************************************************/
	public static void VDPpset8(int DX, int DY, int CL, int OP)
	{
	  VDPpsetlowlevel(VDP_VRMP8(DX, DY),
	                  CL, 0, OP);
	}
	
	/** VDPpset() ************************************************/
	/** Set a pixel on a screen                                 **/
	/*************************************************************/
	public static void VDPpset(int SM, int DX, int DY, int CL, int OP)
	{
	  switch (SM) {
	    case 0: VDPpset5(DX, DY, CL, OP); break;
	    case 1: VDPpset6(DX, DY, CL, OP); break;
	    case 2: VDPpset7(DX, DY, CL, OP); break;
	    case 3: VDPpset8(DX, DY, CL, OP); break;
	  }
	}
        
        public static abstract interface _vdpEngine {
            public abstract void handler();
        }
	
	/** GetVdpTimingValue() **************************************/
	/** Get timing value for a certain VDP command              **/
	/*************************************************************/
	static int GetVdpTimingValue(int[] timing_values)
	{
	  return(timing_values[((_vdp.contReg[1]>>6)&1)|(_vdp.contReg[8]&2)|((_vdp.contReg[9]<<1)&4)]);
	}
	
	/** SrchEgine()** ********************************************/
	/** Search a dot                                            **/
	/*************************************************************/
	public static _vdpEngine SrchEngine = new _vdpEngine() {
            @Override
            public void handler() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                /*TODO*///	  register int SX=MMC.SX;
/*TODO*///	  register int SY=MMC.SY;
/*TODO*///	  register int TX=MMC.TX;
/*TODO*///	  register int ANX=MMC.ANX;
/*TODO*///	  register UINT8 CL=MMC.CL;
/*TODO*///	  register int cnt;
/*TODO*///	  register int delta;
/*TODO*///	
/*TODO*///	  delta = GetVdpTimingValue(srch_timing);
/*TODO*///	  cnt = VdpOpsCnt;
/*TODO*///	
/*TODO*///	#define pre_srch \
/*TODO*///	    pre_loop \
/*TODO*///	      if ((
/*TODO*///	#define post_srch(MX) \
/*TODO*///	           ==CL) ^ANX) { \
/*TODO*///	      _vdp.statReg[2]|=0x10; /* Border detected */ \
/*TODO*///	      break; \
/*TODO*///	    } \
/*TODO*///	    if ((SX+=TX) & MX) { \
/*TODO*///	      _vdp.statReg[2]&=0xEF; /* Border not detected */ \
/*TODO*///	      break; \
/*TODO*///	    } \
/*TODO*///	  }
/*TODO*///	
/*TODO*///	  switch (ScrMode) {
/*TODO*///	    case 5: pre_srch VDPpoint5(SX, SY) post_srch(256)
/*TODO*///	            break;
/*TODO*///	    case 6: pre_srch VDPpoint6(SX, SY) post_srch(512)
/*TODO*///	            break;
/*TODO*///	    case 7: pre_srch VDPpoint7(SX, SY) post_srch(512)
/*TODO*///	            break;
/*TODO*///	    case 8: pre_srch VDPpoint8(SX, SY) post_srch(256)
/*TODO*///	            break;
/*TODO*///	  }
/*TODO*///	
/*TODO*///	  if ((VdpOpsCnt=cnt)>0) {
/*TODO*///	    /* Command execution done */
/*TODO*///	    _vdp.statReg[2]&=0xFE;
/*TODO*///	    VdpEngine=0;
/*TODO*///	    /* Update SX in VDP registers */
/*TODO*///	    _vdp.statReg[8]=SX&0xFF;
/*TODO*///	    _vdp.statReg[9]=(SX>>8)|0xFE;
/*TODO*///	  }
/*TODO*///	  else {
/*TODO*///	    MMC.SX=SX;
/*TODO*///	  }
            }
        };

	
	/** LineEgine()** ********************************************/
	/** Draw a line                                             **/
	/*************************************************************/
	public static _vdpEngine LineEngine = new _vdpEngine() {
            @Override
            public void handler() {
                //System.out.println("LineEngine mode="+ScrMode());
                //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                int DX=MMC.DX;
                int DY=MMC.DY;
                int TX=MMC.TX;
                int TY=MMC.TY;
                int NX=MMC.NX;
                int NY=MMC.NY;
                int ASX=MMC.ASX;
                int ADX=MMC.ADX;
                int CL=MMC.CL;
                int LO=MMC.LO;
                int cnt;
                int delta;

                delta = GetVdpTimingValue(line_timing);
                cnt = VdpOpsCnt;
                //System.out.println("cntA="+cnt);
                //System.out.println("deltaA="+delta);
	
                if ((_vdp.contReg[45]&0x01)==0){
                  /* X-Axis is major direction */
                  //System.out.println("X-Axis");
                  switch (ScrMode()) {
                    case 5: 
                        while ((cnt-=delta) > 0) { 
                            VDPpset5(DX, DY, CL, LO);
                            DX+=TX; 
                            if ((ASX-=NY)<0) { 
                              ASX+=NX; 
                              DY+=TY; 
                            } 
                            ASX&=1023; /* Mask to 10 bits range */ 
                            if (ADX++==NX || (DX&256)!=0)
                              break; 
                        }
                        break;
	    
                    case 6: 
                        while ((cnt-=delta) > 0) { 

                            //System.out.println("cntB="+cnt);
                            //System.out.println("deltaB="+delta);
                            VDPpset6(DX, DY, CL, LO); 

                            DX+=TX;
                            if ((ASX-=NY)<0) {
                              ASX+=NX;
                              DY+=TY;
                            }
                            ASX&=1023; /* Mask to 10 bits range */
                            if (ADX++==NX || (DX&512)!=0)
                              break;
                          }
                        break;
                    case 7: 
                        while ((cnt-=delta) > 0) { 
                            VDPpset7(DX, DY, CL, LO);
                            DX+=TX; 
                            if ((ASX-=NY)<0) { 
                              ASX+=NX; 
                              DY+=TY; 
                            } 
                            ASX&=1023; /* Mask to 10 bits range */ 
                            if (ADX++==NX || (DX&512)!=0) {}
                              break;
                        }
                        break;
                    case 8: 
                        while ((cnt-=delta) > 0) { 
                            VDPpset8(DX, DY, CL, LO);
                            DX+=TX; 
                            if ((ASX-=NY)<0) { 
                              ASX+=NX; 
                              DY+=TY; 
                            } 
                            ASX&=1023; /* Mask to 10 bits range */ 
                            if (ADX++==NX || (DX&256)!=0)
                              break;
                        }
                        break;
                  }
                } else {
                  /* Y-Axis is major direction */
                  switch (ScrMode()) {
                    case 5: 
                        while ((cnt-=delta) > 0) { 
                            VDPpset5(DX, DY, CL, LO);
                            DY+=TY; 
                            if ((ASX-=NY)<0) { 
                              ASX+=NX; 
                              DX+=TX; 
                            } 
                            ASX&=1023; /* Mask to 10 bits range */ 
                            if (ADX++==NX || (DX&256)!=0)
                                break;
                        }
                        break;
                    case 6: 
                        while ((cnt-=delta) > 0) { 
                            VDPpset6(DX, DY, CL, LO);
                            DY+=TY; 
                            if ((ASX-=NY)<0) { 
                              ASX+=NX; 
                              DX+=TX; 
                            } 
                            ASX&=1023; /* Mask to 10 bits range */ 
                            if (ADX++==NX || (DX&512)!=0) {}
                            break;
                        }
                        break;
                    case 7: while ((cnt-=delta) > 0) { 
                        VDPpset7(DX, DY, CL, LO);
                        DY+=TY; 
                        if ((ASX-=NY)<0) { 
                          ASX+=NX; 
                          DX+=TX; 
                        } 
                        ASX&=1023; /* Mask to 10 bits range */ 
                        if (ADX++==NX || (DX&512)!=0)
                        break;
                    }
                    break;
                    
                    case 8: 
                        while ((cnt-=delta) > 0) { 
                            VDPpset8(DX, DY, CL, LO);
                            DY+=TY; 
                            if ((ASX-=NY)<0) { 
                              ASX+=NX; 
                              DX+=TX; 
                            } 
                            ASX&=1023; /* Mask to 10 bits range */ 
                            if (ADX++==NX || (DX&256)!=0)
                                break;
                        }
                        break;
                  
                  }
                }
                
                //System.out.println("cnt="+cnt);
                //System.out.println("VdpOpsCnt="+VdpOpsCnt);

                if ((VdpOpsCnt=cnt)>0) {
                  /* Command execution done */
                  _vdp.statReg[2]&=0xFE;
                  VdpEngine=null;
                  _vdp.contReg[38]=MMC.DY & 0xFF;
                  _vdp.contReg[39]=(MMC.DY>>8) & 0x03;
                }
                else {
                  MMC.DX=DX;
                  MMC.DY=DY;
                  MMC.ASX=ASX;
                  MMC.ADX=ADX;
                }
            }
        };

	
	/** LmmvEngine() *********************************************/
	/** VDP . Vram                                             **/
	/*************************************************************/
	public static _vdpEngine LmmvEngine = new _vdpEngine() {
            @Override
            public void handler() {
                //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                int DX=MMC.DX;
                int DY=MMC.DY;
                int TX=MMC.TX;
                int TY=MMC.TY;
                int NX=MMC.NX;
                int NY=MMC.NY;
                int ADX=MMC.ADX;
                int ANX=MMC.ANX;
                int CL=MMC.CL;
                int LO=MMC.LO;
                int cnt;
                int delta;

                delta = GetVdpTimingValue(lmmv_timing);
                cnt = VdpOpsCnt;

                switch (ScrMode()) {
                  case 5: 
                      while ((cnt-=delta) > 0) { 
                          VDPpset5(ADX, DY, CL, LO); 
                          if (--ANX==0 || ((ADX+=TX)&256)!=0) {
                            if ((--NY&1023)==0 || (DY+=TY)==-1)
                              break;
                            else {
                              ADX=DX;
                              ANX=NX;
                            }
                          }
                    }
                          break;
                  case 6: 
                      while ((cnt-=delta) > 0) { 
                          VDPpset6(ADX, DY, CL, LO); 
                          if (--ANX==0 || ((ADX+=TX)&512)!=0) {
                            if ((--NY&1023)==0 || (DY+=TY)==-1)
                              break;
                            else {
                              ADX=DX;
                              ANX=NX;
                            }
                          }
                    }
                          break;
                  case 7: while ((cnt-=delta) > 0) { 
                      VDPpset7(ADX, DY, CL, LO); 
                      if (--ANX==0 || ((ADX+=TX)&512)!=0) {
                            if ((--NY&1023)==0 || (DY+=TY)==-1)
                              break;
                            else {
                              ADX=DX;
                              ANX=NX;
                            }
                          }
                    }
                          break;
                  case 8: 
                      while ((cnt-=delta) > 0) { 
                          VDPpset8(ADX, DY, CL, LO); 
                          if (--ANX==0 || ((ADX+=TX)&256)!=0) {
                            if ((--NY&1023)==0 || (DY+=TY)==-1)
                              break;
                            else {
                              ADX=DX;
                              ANX=NX;
                            }
                          }
                    }
                          break;
                }

                if ((VdpOpsCnt=cnt)>0) {
                  /* Command execution done */
                  _vdp.statReg[2]&=0xFE;
                  VdpEngine=null;
                  if (NY == 0)
                    DY+=TY;
                  _vdp.contReg[38]=DY & 0xFF;
                  _vdp.contReg[39]=(DY>>8) & 0x03;
                  _vdp.contReg[42]=NY & 0xFF;
                  _vdp.contReg[43]=(NY>>8) & 0x03;
                }
                else {
                  MMC.DY=DY;
                  MMC.NY=NY;
                  MMC.ANX=ANX;
                  MMC.ADX=ADX;
                }
            }
        };

	
	/** LmmmEngine() *********************************************/
	/** Vram . Vram                                            **/
	/*************************************************************/
	public static _vdpEngine LmmmEngine = new _vdpEngine() {
            @Override
            public void handler() {
                //System.out.println("LmmmEngine needs to be implemented");
                //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                int SX=MMC.SX;
                int SY=MMC.SY;
                int DX=MMC.DX;
                int DY=MMC.DY;
                int TX=MMC.TX;
                int TY=MMC.TY;
                int NX=MMC.NX;
                int NY=MMC.NY;
                int ASX=MMC.ASX;
                int ADX=MMC.ADX;
                int ANX=MMC.ANX;
                int LO=MMC.LO;
                int cnt;
                int delta;

                delta = GetVdpTimingValue(lmmm_timing);
                cnt = VdpOpsCnt;

                switch (ScrMode()) {
                    case 5: 
                        while ((cnt-=delta) > 0) { 
                            VDPpset5(ADX, DY, VDPpoint5(ASX, SY), LO);
                            if (--ANX==0 || ((ASX+=TX)&256)!=0 || ((ADX+=TX)&256)!=0) {
                                if ((--NY&1023)==0 || (SY+=TY)==-1 || (DY+=TY)==-1)
                                  break;
                                else {
                                  ASX=SX;
                                  ADX=DX;
                                  ANX=NX;
                                }
                              }
                        }
                        break;
                    case 6: 
                        while ((cnt-=delta) > 0) { 
                            VDPpset6(ADX, DY, VDPpoint6(ASX, SY), LO);
                            if (--ANX==0 || ((ASX+=TX)&512)!=0 || ((ADX+=TX)&512)!=0) {
                                if ((--NY&1023)==0 || (SY+=TY)==-1 || (DY+=TY)==-1)
                                  break;
                                else {
                                  ASX=SX;
                                  ADX=DX;
                                  ANX=NX;
                                }
                              }
                        }
                        break;
                    case 7: 
                        while ((cnt-=delta) > 0) { 
                            VDPpset7(ADX, DY, VDPpoint7(ASX, SY), LO);
                            if (--ANX==0 || ((ASX+=TX)&512)!=0 || ((ADX+=TX)&512)!=0) {
                                if ((--NY&1023)==0 || (SY+=TY)==-1 || (DY+=TY)==-1)
                                  break;
                                else {
                                  ASX=SX;
                                  ADX=DX;
                                  ANX=NX;
                                }
                              }
                        }
                        break;
                    case 8: 
                        while ((cnt-=delta) > 0) { 
                            VDPpset8(ADX, DY, VDPpoint8(ASX, SY), LO);
                            if (--ANX==0 || ((ASX+=TX)&256)!=0 || ((ADX+=TX)&256)!=0) {
                                if ((--NY&1023)==0 || (SY+=TY)==-1 || (DY+=TY)==-1)
                                  break;
                                else {
                                  ASX=SX;
                                  ADX=DX;
                                  ANX=NX;
                                }
                              }
                        }
                        break;
                }

                if ((VdpOpsCnt=cnt)>0) {
                  /* Command execution done */
                  _vdp.statReg[2]&=0xFE;
                  VdpEngine=null;
                  if (NY == 0) {
                    SY+=TY;
                    DY+=TY;
                  }
                  else
                    if (SY==-1)
                      DY+=TY;
                  _vdp.contReg[42]=NY & 0xFF;
                  _vdp.contReg[43]=(NY>>8) & 0x03;
                  _vdp.contReg[34]=SY & 0xFF;
                  _vdp.contReg[35]=(SY>>8) & 0x03;
                  _vdp.contReg[38]=DY & 0xFF;
                  _vdp.contReg[39]=(DY>>8) & 0x03;
                }
                else {
                  MMC.SY=SY;
                  MMC.DY=DY;
                  MMC.NY=NY;
                  MMC.ANX=ANX;
                  MMC.ASX=ASX;
                  MMC.ADX=ADX;
                }
            }
        };


/*TODO*///	/** LmcmEngine() *********************************************/
/*TODO*///	/** Vram . CPU                                             **/
/*TODO*///	/*************************************************************/
	public static _vdpEngine LmcmEngine = new _vdpEngine() {
            @Override
            public void handler() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                /*TODO*///	  if ((_vdp.statReg[2]&0x80)!=0x80) {
/*TODO*///	
/*TODO*///	    _vdp.statReg[7]=VDP[44]=VDP_POINT(ScrMode-5, MMC.ASX, MMC.SY);
/*TODO*///	    VdpOpsCnt-=GetVdpTimingValue(lmmv_timing);
/*TODO*///	    _vdp.statReg[2]|=0x80;
/*TODO*///	
/*TODO*///	    if (!--MMC.ANX || ((MMC.ASX+=MMC.TX)&MMC.MX)) {
/*TODO*///	      if (!(--MMC.NY & 1023) || (MMC.SY+=MMC.TY)==-1) {
/*TODO*///	        _vdp.statReg[2]&=0xFE;
/*TODO*///	        VdpEngine=0;
/*TODO*///	        if (!MMC.NY)
/*TODO*///	          MMC.DY+=MMC.TY;
/*TODO*///	        VDP[42]=MMC.NY & 0xFF;
/*TODO*///	        VDP[43]=(MMC.NY>>8) & 0x03;
/*TODO*///	        VDP[34]=MMC.SY & 0xFF;
/*TODO*///	        VDP[35]=(MMC.SY>>8) & 0x03;
/*TODO*///	      }
/*TODO*///	      else {
/*TODO*///	        MMC.ASX=MMC.SX;
/*TODO*///	        MMC.ANX=MMC.NX;
/*TODO*///	      }
/*TODO*///	    }
/*TODO*///	  }
            }
        };

        
	
	/** LmmcEngine() *********************************************/
	/** CPU . Vram                                             **/
	/*************************************************************/
	public static _vdpEngine LmmcEngine = new _vdpEngine() {
            @Override
            public void handler() {
                if ((_vdp.statReg[2]&0x80)!=0x80) {
                    int SM=ScrMode()-5;

                    _vdp.statReg[7]=_vdp.contReg[44]&=Mask[SM];
                    VDP_PSET(SM, MMC.ADX, MMC.DY, _vdp.contReg[44], MMC.LO);
                    VdpOpsCnt-=GetVdpTimingValue(lmmv_timing);
                    _vdp.statReg[2]|=0x80;

                    if (--MMC.ANX==0 || ((MMC.ADX+=MMC.TX)&MMC.MX)!=0) {
                      if ((--MMC.NY&1023)==0 || (MMC.DY+=MMC.TY)==-1) {
                        _vdp.statReg[2]&=0xFE;
                        VdpEngine=null;
                        if (MMC.NY==0)
                          MMC.DY+=MMC.TY;
                        _vdp.contReg[42]=MMC.NY & 0xFF;
                        _vdp.contReg[43]=(MMC.NY>>8) & 0x03;
                        _vdp.contReg[38]=MMC.DY & 0xFF;
                        _vdp.contReg[39]=(MMC.DY>>8) & 0x03;
                      }
                      else {
                        MMC.ADX=MMC.DX;
                        MMC.ANX=MMC.NX;
                      }
                    }
                  }
            }
        };

        
	/** HmmvEngine() *********************************************/
	/** VDP -. Vram                                            **/
	/*************************************************************/
	public static _vdpEngine HmmvEngine = new _vdpEngine() {
            @Override
            public void handler() {
                //System.out.println("HmmvEngine mode="+ScrMode());
                //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                int DX=MMC.DX;
                int DY=MMC.DY;
                int TX=MMC.TX;
                int TY=MMC.TY;
                int NX=MMC.NX;
                int NY=MMC.NY;
                int ADX=MMC.ADX;
                int ANX=MMC.ANX;
                int CL=MMC.CL;
                int cnt;
                int delta;

                delta = GetVdpTimingValue(hmmv_timing);
                cnt = VdpOpsCnt;
	
                switch (ScrMode()) {
                    case 5: 
                        while ((cnt-=delta) > 0) { 
                            VDP_VRMP5(ADX, DY).write( CL );
                        
                            if (--ANX==0 || ((ADX+=TX)&256)!=0) {
                                if ((--NY&1023)==0 || (DY+=TY)==-1)
                                  break;
                                else {
                                  ADX=DX;
                                  ANX=NX;
                                }
                              }
                        }
                        
	            break;
	    case 6: while ((cnt-=delta) > 0) { 
                VDP_VRMP6(ADX, DY).write( CL );
                        
                if (--ANX==0 || ((ADX+=TX)&512)!=0) {
                    if ((--NY&1023)==0 || (DY+=TY)==-1)
                      break;
                    else {
                      ADX=DX;
                      ANX=NX;
                    }
                  }
                }
	            break;
	    case 7: while ((cnt-=delta) > 0) { 
                VDP_VRMP7(ADX, DY).write( CL );
                
                        
                if (--ANX==0 || ((ADX+=TX)&512)!=0) {
                    if ((--NY&1023)==0 || (DY+=TY)==-1)
                      break;
                    else {
                      ADX=DX;
                      ANX=NX;
                    }
                  }
                }
	        
                break;
	            
	    case 8: while ((cnt-=delta) > 0) { 
                VDP_VRMP8(ADX, DY).write( CL );
                
                        
                if (--ANX==0 || ((ADX+=TX)&256)!=0) {
                    if ((--NY&1023)==0 || (DY+=TY)==-1)
                      break;
                    else {
                      ADX=DX;
                      ANX=NX;
                    }
                  }
                }
	        break;
                }
	
                if ((VdpOpsCnt=cnt)>0) {
                  /* Command execution done */
                  _vdp.statReg[2]&=0xFE;
                  VdpEngine=null;
                  if (NY == 0)
                    DY+=TY;
                  _vdp.contReg[42]=NY & 0xFF;
                  _vdp.contReg[43]=(NY>>8) & 0x03;
                  _vdp.contReg[38]=DY & 0xFF;
                  _vdp.contReg[39]=(DY>>8) & 0x03;
                }
                else {
                  MMC.DY=DY;
                  MMC.NY=NY;
                  MMC.ANX=ANX;
                  MMC.ADX=ADX;
                }
            }
        };

        
/*TODO*///	/** HmmmEngine() *********************************************/
/*TODO*///	/** Vram . Vram                                            **/
/*TODO*///	/*************************************************************/
	public static _vdpEngine HmmmEngine = new _vdpEngine() {
            @Override
            public void handler() {
                //System.out.println("HmmmEngine mode="+ScrMode());
                //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                int SX=MMC.SX;
                int SY=MMC.SY;
                int DX=MMC.DX;
                int DY=MMC.DY;
                int TX=MMC.TX;
                int TY=MMC.TY;
                int NX=MMC.NX;
                int NY=MMC.NY;
                int ASX=MMC.ASX;
                int ADX=MMC.ADX;
                int ANX=MMC.ANX;
                int cnt;
                int delta;

                delta = GetVdpTimingValue(hmmm_timing);
                cnt = VdpOpsCnt;

                switch (ScrMode()) {
                    case 5: 
                            while((cnt-=delta) > 0) { 
                                VDP_VRMP5(ADX, DY).write( VDP_VRMP5(ASX, SY).read() );
                            
                                if (--ANX==0 || ((ASX+=TX)&256)!=0 || ((ADX+=TX)&256)!=0) {
                                    if ((--NY&1023)==0 || (SY+=TY)==-1 || (DY+=TY)==-1)
                                      break;
                                    else {
                                      ASX=SX;
                                      ADX=DX;
                                      ANX=NX;
                                    }
                                  }
                            }

                            break;
        /*TODO*///	    case 6: pre_loop *VDP_VRMP6(ADX, DY) = *VDP_VRMP6(ASX, SY); post_xxyy(512)
        /*TODO*///	            break;
        /*TODO*///	    case 7: pre_loop *VDP_VRMP7(ADX, DY) = *VDP_VRMP7(ASX, SY); post_xxyy(512)
        /*TODO*///	            break;
        /*TODO*///	    case 8: pre_loop *VDP_VRMP8(ADX, DY) = *VDP_VRMP8(ASX, SY); post_xxyy(256)
        /*TODO*///	            break;
                  }

                  if ((VdpOpsCnt=cnt)>0) {
                    /* Command execution done */
                    _vdp.statReg[2]&=0xFE;
                    VdpEngine=null;
                    if (NY == 0) {
                      SY+=TY;
                      DY+=TY;
                    }
                    else
                      if (SY==-1)
                        DY+=TY;
                    _vdp.contReg[42]=NY & 0xFF;
                    _vdp.contReg[43]=(NY>>8) & 0x03;
                    _vdp.contReg[34]=SY & 0xFF;
                    _vdp.contReg[35]=(SY>>8) & 0x03;
                    _vdp.contReg[38]=DY & 0xFF;
                    _vdp.contReg[39]=(DY>>8) & 0x03;
                  }
                  else {
                    MMC.SY=SY;
                    MMC.DY=DY;
                    MMC.NY=NY;
                    MMC.ANX=ANX;
                    MMC.ASX=ASX;
                    MMC.ADX=ADX;
                  }
            }
        };

        
/*TODO*///	/** YmmmEngine() *********************************************/
/*TODO*///	/** Vram . Vram                                            **/
/*TODO*///	/*************************************************************/
	public static _vdpEngine YmmmEngine = new _vdpEngine() {
            @Override
            public void handler() {
                //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                int SY=MMC.SY;
                int DX=MMC.DX;
                int DY=MMC.DY;
                int TX=MMC.TX;
                int TY=MMC.TY;
                int NY=MMC.NY;
                int ADX=MMC.ADX;
                int cnt;
                int delta;

                delta = GetVdpTimingValue(ymmm_timing);
                cnt = VdpOpsCnt;
	
	  switch (ScrMode()) {
	    case 5: 
                while ((cnt-=delta) > 0) { VDP_VRMP5(ADX, DY).write( VDP_VRMP5(ADX, SY).read()); 
                if (((ADX+=TX)&256)!=0) {
                    if ((--NY&1023)==0 || (SY+=TY)==-1 || (DY+=TY)==-1)
                      break;
                    else
                      ADX=DX;
                }
            }
	            break;
	    case 6: while ((cnt-=delta) > 0) { VDP_VRMP6(ADX, DY).write(VDP_VRMP6(ADX, SY).read()); 
                if (((ADX+=TX)&512)!=0) {
                    if ((--NY&1023)==0 || (SY+=TY)==-1 || (DY+=TY)==-1)
                      break;
                    else
                      ADX=DX;
                }
            }
	            break;
	    case 7: while ((cnt-=delta) > 0) { 
                VDP_VRMP7(ADX, DY).write(VDP_VRMP7(ADX, SY).read());
             
                if (((ADX+=TX)&512)!=0) {
                    if ((--NY&1023)==0 || (SY+=TY)==-1 || (DY+=TY)==-1)
                      break;
                    else
                      ADX=DX;
                }
            }
	            break;
	    case 8: while ((cnt-=delta) > 0) { 
                VDP_VRMP8(ADX, DY).write(VDP_VRMP8(ADX, SY).read());
            
                if (((ADX+=TX)&256)!=0) {
                    if ((--NY&1023)==0 || (SY+=TY)==-1 || (DY+=TY)==-1)
                      break;
                    else
                      ADX=DX;
                }
            }
	            break;
	  }
	
	  if ((VdpOpsCnt=cnt)>0) {
	    /* Command execution done */
	    _vdp.statReg[2]&=0xFE;
	    VdpEngine=null;
	    if (NY == 0) {
	      SY+=TY;
	      DY+=TY;
	    }
	    else
	      if (SY==-1)
	        DY+=TY;
	    _vdp.contReg[42]=NY & 0xFF;
	    _vdp.contReg[43]=(NY>>8) & 0x03;
	    _vdp.contReg[34]=SY & 0xFF;
	    _vdp.contReg[35]=(SY>>8) & 0x03;
	    _vdp.contReg[38]=DY & 0xFF;
	    _vdp.contReg[39]=(DY>>8) & 0x03;
	  }
	  else {
	    MMC.SY=SY;
	    MMC.DY=DY;
	    MMC.NY=NY;
	    MMC.ADX=ADX;
	  }
            }
        };

        
/*TODO*///	/** HmmcEngine() *********************************************/
/*TODO*///	/** CPU . Vram                                             **/
/*TODO*///	/*************************************************************/
	public static _vdpEngine HmmcEngine = new _vdpEngine() {
            @Override
            public void handler() {
                //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                	  if ((_vdp.statReg[2]&0x80)!=0x80) {
	
	    VDP_VRMP(ScrMode()-5, MMC.ADX, MMC.DY).write(_vdp.contReg[44]);
	    VdpOpsCnt-=GetVdpTimingValue(hmmv_timing);
	    _vdp.statReg[2]|=0x80;
	
	    if (--MMC.ANX==0 || ((MMC.ADX+=MMC.TX)&MMC.MX)!=0) {
	      if ((--MMC.NY&1023)==0 || (MMC.DY+=MMC.TY)==-1) {
	        _vdp.statReg[2]&=0xFE;
	        VdpEngine=null;
	        if (MMC.NY==0)
	          MMC.DY+=MMC.TY;
	        _vdp.contReg[42]=MMC.NY & 0xFF;
	        _vdp.contReg[43]=(MMC.NY>>8) & 0x03;
	        _vdp.contReg[38]=MMC.DY & 0xFF;
	        _vdp.contReg[39]=(MMC.DY>>8) & 0x03;
	      }
	      else {
	        MMC.ADX=MMC.DX;
	        MMC.ANX=MMC.NX;
	      }
	    }
	  }
            }
        };

        
        
	
	/** VDPWrite() ***********************************************/
	/** Use this function to transfer pixel(s) from CPU to VDP. **/
	/*************************************************************/
	static void v9938_cpu_to_vdp (int V)
	{
	  _vdp.statReg[2]&=0x7F;
	  _vdp.statReg[7]=V;
          _vdp.contReg[44]=V;
	  if(VdpEngine!=null &&(VdpOpsCnt>0)) VdpEngine.handler();
	}
	
	/** VDPRead() ************************************************/
	/** Use this function to transfer pixel(s) from VDP to CPU. **/
	/*************************************************************/
	public static int v9938_vdp_to_cpu ()
	{
	  _vdp.statReg[2]&=0x7F;
	  if(VdpEngine != null&&(VdpOpsCnt>0)) VdpEngine.handler();
	  return(_vdp.contReg[44]);
	}
        
        static String Ops[] =
	  {
	    "SET ","AND ","OR  ","XOR ","NOT ","NOP ","NOP ","NOP ",
	    "TSET","TAND","TOR ","TXOR","TNOT","NOP ","NOP ","NOP "
	  };
	  static String Commands[] =
	  {
	    " ABRT"," ????"," ????"," ????","POINT"," PSET"," SRCH"," LINE",
	    " LMMV"," LMMM"," LMCM"," LMMC"," HMMV"," HMMM"," YMMM"," HMMC"
	  };
	
	/** ReportVdpCommand() ***************************************/
	/** Report VDP Command to be executed                       **/
	/*************************************************************/
	static void ReportVdpCommand(int Op)
	{
	  
	  int CL, CM, LO;
	  int SX,SY, DX,DY, NX,NY;
	
	  /* Fetch arguments */
	  CL = _vdp.contReg[44];
	  SX = (_vdp.contReg[32]+((int)_vdp.contReg[33]<<8)) & 511;
	  SY = (_vdp.contReg[34]+((int)_vdp.contReg[35]<<8)) & 1023;
	  DX = (_vdp.contReg[36]+((int)_vdp.contReg[37]<<8)) & 511;
	  DY = (_vdp.contReg[38]+((int)_vdp.contReg[39]<<8)) & 1023;
	  NX = (_vdp.contReg[40]+((int)_vdp.contReg[41]<<8)) & 1023;
	  NY = (_vdp.contReg[42]+((int)_vdp.contReg[43]<<8)) & 1023;
	  CM = Op>>4;
	  LO = Op&0x0F;
	
	  logerror ("V9938: Opcode %02Xh %s-%s (%d,%d).(%d,%d),%d [%d,%d]%s\n",
	         Op, Commands[CM], Ops[LO],
	         SX,SY, DX,DY, CL, (_vdp.contReg[45]&0x04)!=0? -NX:NX,
	         (_vdp.contReg[45]&0x08)!=0? -NY:NY,
	         (_vdp.contReg[45]&0x70)!=0? " on ExtVRAM":""
	        );
	}
	
	/** VDPDraw() ************************************************/
	/** Perform a given V9938 operation Op.                     **/
	/*************************************************************/
	static int v9938_command_unit_w (int Op)
	{
            //System.out.println("v9938_command_unit_w");
	  int SM;
	
	  /* V9938 ops only work in SCREENs 5-8 */
	  if (_vdp.mode<5)
	    return(0);
	
	  SM = _vdp.mode-5;         /* Screen mode index 0..3  */
          //System.out.println("SM="+SM);
	
	  MMC.CM = Op>>4;
	  if ((MMC.CM & 0x0C) != 0x0C && MMC.CM != 0)
	    /* Dot operation: use only relevant bits of color */
	    _vdp.statReg[7]=(_vdp.contReg[44]&=Mask[SM]);
	
	/*  if(Verbose&0x02) */
	    ReportVdpCommand(Op);
            
            //System.out.println("Op="+(Op>>4));
	
	  switch(Op>>4) {
	    case CM_ABRT:
	      _vdp.statReg[2]&=0xFE;
	      VdpEngine=null;
	      return 1;
	    case CM_POINT:
	      _vdp.statReg[2]&=0xFE;
	      VdpEngine=null;
	      _vdp.statReg[7]=
                           VDP_POINT(SM, _vdp.contReg[32]+((int)_vdp.contReg[33]<<8),
	                                 _vdp.contReg[34]+((int)_vdp.contReg[35]<<8));
              _vdp.contReg[44]=
	                   VDP_POINT(SM, _vdp.contReg[32]+((int)_vdp.contReg[33]<<8),
	                                 _vdp.contReg[34]+((int)_vdp.contReg[35]<<8));
	      return 1;
	    case CM_PSET:
	      _vdp.statReg[2]&=0xFE;
	      VdpEngine=null;
	      VDP_PSET(SM,
	               _vdp.contReg[36]+((int)_vdp.contReg[37]<<8),
	               _vdp.contReg[38]+((int)_vdp.contReg[39]<<8),
	               _vdp.contReg[44],
	               Op&0x0F);
	      return 1;
	    case CM_SRCH:
	      VdpEngine=SrchEngine;
	      break;
	    case CM_LINE:
	      VdpEngine=LineEngine;
	      break;
	    case CM_LMMV:
	      VdpEngine=LmmvEngine;
	      break;
	    case CM_LMMM:
	      VdpEngine=LmmmEngine;
	      break;
	    case CM_LMCM:
	      VdpEngine=LmcmEngine;
	      break;
	    case CM_LMMC:
	      VdpEngine=LmmcEngine;
	      break;
	    case CM_HMMV:
	      VdpEngine=HmmvEngine;
	      break;
	    case CM_HMMM:
	      VdpEngine=HmmmEngine;
	      break;
	    case CM_YMMM:
	      VdpEngine=YmmmEngine;
	      break;
	    case CM_HMMC:
	      VdpEngine=HmmcEngine;
	      break;
	    default:
	      logerror("V9938: Unrecognized opcode %02Xh\n",Op);
	        return(0);
	  }
	
	  /* Fetch unconditional arguments */
	  MMC.SX = (_vdp.contReg[32]+((int)_vdp.contReg[33]<<8)) & 511;
	  MMC.SY = (_vdp.contReg[34]+((int)_vdp.contReg[35]<<8)) & 1023;
	  MMC.DX = (_vdp.contReg[36]+((int)_vdp.contReg[37]<<8)) & 511;
	  MMC.DY = (_vdp.contReg[38]+((int)_vdp.contReg[39]<<8)) & 1023;
	  MMC.NY = (_vdp.contReg[42]+((int)_vdp.contReg[43]<<8)) & 1023;
	  MMC.TY = (_vdp.contReg[45]&0x08) != 0? -1:1;
	  MMC.MX = PPL[SM];
	  MMC.CL = _vdp.contReg[44];
	  MMC.LO = Op&0x0F;
	
	  /* Argument depends on UINT8 or dot operation */
	  if ((MMC.CM & 0x0C) == 0x0C) {
	    MMC.TX = (_vdp.contReg[45]&0x04) != 0 ? -PPB[SM]:PPB[SM];
	    MMC.NX = ((_vdp.contReg[40]+((int)_vdp.contReg[41]<<8)) & 1023)/PPB[SM];
	  }
	  else {
	    MMC.TX = (_vdp.contReg[45]&0x04) != 0 ? -1:1;
	    MMC.NX = (_vdp.contReg[40]+((int)_vdp.contReg[41]<<8)) & 1023;
	  }
	
	  /* X loop variables are treated specially for LINE command */
	  if (MMC.CM == CM_LINE) {
	    MMC.ASX=((MMC.NX-1)>>1);
	    MMC.ADX=0;
	  }
	  else {
	    MMC.ASX = MMC.SX;
	    MMC.ADX = MMC.DX;
	  }
	
	  /* NX loop variable is treated specially for SRCH command */
	  if (MMC.CM == CM_SRCH)
	    MMC.ANX=(_vdp.contReg[45]&0x02)!=0 ? 1 : 0; /* Do we look for "==" or "!="? */
	  else
	    MMC.ANX = MMC.NX;
	
	  /* Command execution started */
	  _vdp.statReg[2]|=0x01;
	
	  /* Start execution if we still have time slices */
          //System.out.println("VdpOpsCnt="+VdpOpsCnt);
	  if(VdpEngine!=null && (VdpOpsCnt>0)) VdpEngine.handler();
          //System.out.println("End vdpEngine");
	
	  /* Operation successfull initiated */
	  return(1);
	}
	
	/** LoopVDP() ************************************************/
	/** Run X steps of active VDP command                       **/
	/*************************************************************/
	public static void v9938_update_command ()
	{
            //System.out.println("v9938_update_command NOT IMPLEMENTED!!!!");
	  if(VdpOpsCnt<=0)
	  {
	    VdpOpsCnt+=13662;
	    if(VdpEngine != null&&(VdpOpsCnt>0)) VdpEngine.handler();
	  }
	  else
	  {
	    VdpOpsCnt=13662;
	    if(VdpEngine != null) VdpEngine.handler();
	  }
	}
	
}
