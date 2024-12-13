/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.vidhrdw;

import static common.ptr.*;
import static common.subArrays.*;
import static mame056.mame.Machine;
import static mess056.vidhrdw.v9938H.*;
import static mess056.vidhrdw.v9938.V9938_WIDTH;
import static mess056.vidhrdw.v9938._vdp;
import static mess056.vidhrdw.v9938.pal_ind16;
import static mess056.vidhrdw.v9938.pal_ind256;
import static mess056.vidhrdw.v9938.V9938_SECOND_FIELD;

public class v9938mod {
    
    public static abstract interface ModeVisible_8_HandlersPtr {
        public abstract void handler(UBytePtr col, int line);
    }
    
    public static abstract interface ModeVisible_16_HandlersPtr {
        public abstract void handler(UShortPtr col, int line);
    }
    
    public static abstract interface ModeBorder_8_HandlersPtr {
        public abstract void handler(UBytePtr col);
    }
    
    public static abstract interface ModeBorder_16_HandlersPtr {
        public abstract void handler(UShortPtr col);
    }
    
    public static abstract interface ModeSprites_HandlersPtr {
        public abstract void handler(int line, UShortPtr col);
    }
    
    public static abstract interface ModeDraw_Sprites_HandlersPtr {
        public abstract void handler(UShortPtr ln, UShortPtr col);
    }
    
    public static ModeVisible_8_HandlersPtr v9938_mode_text1_8 = new ModeVisible_8_HandlersPtr() {
        public void handler(UBytePtr ln, int line) {
            int pattern, x, xx, name, xxx;	
            int fg, bg, pen;	
            UBytePtr nametbl, patterntbl;	

            patterntbl = new UBytePtr(_vdp.vram, (_vdp.contReg[4] << 11));
            nametbl = new UBytePtr(_vdp.vram, (_vdp.contReg[2] << 10));

        fg = Machine.pens[pal_ind16[_vdp.contReg[7] >> 4]];	
        bg = Machine.pens[pal_ind16[_vdp.contReg[7] & 15]];	

            name = (line/8)*40;	

            pen = Machine.pens[pal_ind16[(_vdp.contReg[7]&0x0f)]];	

            xxx = _vdp.offset_x + 8;	
    if (V9938_WIDTH > 512){
            xxx *= 2;
    }
            while (xxx-- !=0) {ln.writeinc( pen );}

            for (x=0;x<40;x++)	
                    {	
                    pattern = patterntbl.read((nametbl.read(name) * 8) + 	
                            ((line + _vdp.contReg[23]) & 7));	
                    for (xx=0;xx<6;xx++)	
                            {	
                            ln.writeinc( (pattern & 0x80)!=0 ? fg : bg );
    if (V9938_WIDTH > 512){
                            ln.writeinc( (pattern & 0x80)!=0 ? fg : bg );
    }
                            pattern <<= 1;	
                            }	
                    /* width height 212, characters start repeating at the bottom */ 
                    name = (name + 1) & 0x3ff;	
                    }	

            xxx = (16 - _vdp.offset_x) + 8;	
    if (V9938_WIDTH > 512){
            xxx *= 2;
    }
            while (xxx-- !=0) ln.writeinc( pen );
            if (_vdp.size_now != RENDER_HIGH) _vdp.size_now = RENDER_LOW;
        }
    };
    
    public static ModeVisible_16_HandlersPtr v9938_mode_text1_16 = new ModeVisible_16_HandlersPtr() {
        public void handler(UShortPtr ln, int line) {
            int pattern, x, xx, name, xxx;	
            int fg, bg, pen;	
            UBytePtr nametbl, patterntbl;	

            patterntbl = new UBytePtr(_vdp.vram, (_vdp.contReg[4] << 11) );
            nametbl = new UBytePtr(_vdp.vram, (_vdp.contReg[2] << 10) );
	
            fg = Machine.pens[pal_ind16[_vdp.contReg[7] >> 4]];
            bg = Machine.pens[pal_ind16[_vdp.contReg[7] & 15]];
	
            name = (line/8)*40;
	
            pen = Machine.pens[pal_ind16[(_vdp.contReg[7]&0x0f)]];	
	
            xxx = _vdp.offset_x + 8;	
            if (V9938_WIDTH > 512){
                    xxx *= 2;
            }
            while (xxx-- != 0){
                ln.write((char) pen);
                ln.inc();
            }	

            for (x=0;x<40;x++)	
            {	
                    pattern = patterntbl.read((nametbl.read(name) * 8) + 	
                            ((line + _vdp.contReg[23]) & 7));	
                    for (xx=0;xx<6;xx++)	
                    {	
                            ln.write((char) ((pattern & 0x80)!=0 ? fg : bg));
                            ln.inc();
                            if (V9938_WIDTH > 512){
                                ln.write((char) ((pattern & 0x80)!=0 ? fg : bg));
                                ln.inc();
                            }
                            pattern <<= 1;	
                    }	
                    /* width height 212, characters start repeating at the bottom */ 
                    name = (name + 1) & 0x3ff;	
            }	

            xxx = (16 - _vdp.offset_x) + 8;	
            if (V9938_WIDTH > 512){
                    xxx *= 2;
            }
            while (xxx-- != 0){
                ln.write((char) pen);
                ln.inc();
            }
            
            if (_vdp.size_now != RENDER_HIGH) _vdp.size_now = RENDER_LOW;
        }
    };
    
    public static ModeVisible_8_HandlersPtr v9938_mode_text1_8s = new ModeVisible_8_HandlersPtr() {
        @Override
        public void handler(UBytePtr col, int line) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
            
    public static ModeVisible_16_HandlersPtr v9938_mode_text1_16s = new ModeVisible_16_HandlersPtr() {
        @Override
        public void handler(UShortPtr col, int line) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
            
    public static ModeBorder_8_HandlersPtr v9938_default_border_8 = new ModeBorder_8_HandlersPtr() {
        @Override
        public void handler(UBytePtr ln) {
            int pen;
	int	i;

    pen = Machine.pens[pal_ind16[(_vdp.contReg[7]&0x0f)]];
	i = V9938_WIDTH;
	while (i-- != 0){
            ln.writeinc( pen );
        }

	if (_vdp.size_now != RENDER_HIGH) _vdp.size_now = RENDER_LOW;
        }
    };
    
    public static ModeBorder_16_HandlersPtr v9938_default_border_16 = new ModeBorder_16_HandlersPtr() {
        @Override
        public void handler(UShortPtr ln) {
           int pen;
            int	i;

            pen = Machine.pens[pal_ind16[(_vdp.contReg[7]&0x0f)]];
            i = V9938_WIDTH;
            while (i-- != 0) {
                ln.write((char) pen);
                ln.inc();
            }

            if (_vdp.size_now != RENDER_HIGH) _vdp.size_now = RENDER_LOW;
        }
    };
    
    public static ModeBorder_8_HandlersPtr v9938_default_border_8s = new ModeBorder_8_HandlersPtr() {
        @Override
        public void handler(UBytePtr col) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
    
    public static ModeBorder_16_HandlersPtr v9938_default_border_16s = new ModeBorder_16_HandlersPtr() {
        @Override
        public void handler(UShortPtr ln) {
            int pen;
	int	i;

    pen = Machine.pens[pal_ind16[(_vdp.contReg[7]&0x0f)]];
	i = V9938_WIDTH;
	while (i-- != 0){
            ln.write((char) pen);
            ln.inc();
        }

	if (_vdp.size_now != RENDER_HIGH) _vdp.size_now = RENDER_LOW;
        }
    };
    
    public static ModeVisible_8_HandlersPtr v9938_mode_multi_8 = new ModeVisible_8_HandlersPtr() {
        @Override
        public void handler(UBytePtr col, int line) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
    
    public static ModeVisible_16_HandlersPtr v9938_mode_multi_16 = new ModeVisible_16_HandlersPtr() {
        @Override
        public void handler(UShortPtr ln, int line) {
            UBytePtr nametbl, patterntbl;
            int colour;
	int name, line2, x, xx;	
	int pen, pen_bg;
	
	nametbl = new UBytePtr(_vdp.vram, (_vdp.contReg[2] << 10));
	patterntbl = new UBytePtr(_vdp.vram, (_vdp.contReg[4] << 11));

	line2 = (line - _vdp.contReg[23]) & 255;
	name = (line2/8)*32;

	pen_bg = Machine.pens[pal_ind16[(_vdp.contReg[7]&0x0f)]];
if (V9938_WIDTH < 512){
	xx = _vdp.offset_x;
} else {
	xx = _vdp.offset_x * 2;
}
	while (xx-- !=0) {
            ln.write((char) pen_bg);
            ln.inc();
        }

	for (x=0;x<32;x++)	
		{	
		colour = patterntbl.read((nametbl.read(name) * 8) + ((line2/4)&7));
		pen = Machine.pens[pal_ind16[colour>>4]];
		/* eight pixels */
		ln.write((char) pen);ln.inc();
		ln.write((char) pen);ln.inc();
		ln.write((char) pen);ln.inc();
		ln.write((char) pen);ln.inc();
if (V9938_WIDTH > 512){
		ln.write((char) pen);ln.inc();
		ln.write((char) pen);ln.inc();
		ln.write((char) pen);ln.inc();
		ln.write((char) pen);ln.inc();
}
		pen = Machine.pens[pal_ind16[colour&15]];	
		/* eight pixels */	
		ln.write((char) pen);ln.inc();
		ln.write((char) pen);ln.inc();
		ln.write((char) pen);ln.inc();
		ln.write((char) pen);ln.inc();
if (V9938_WIDTH > 512){
		ln.write((char) pen);ln.inc();
		ln.write((char) pen);ln.inc();
		ln.write((char) pen);ln.inc();
		ln.write((char) pen);ln.inc();
}
		name++;	
		}
	
	xx = 16 - _vdp.offset_x;	
if (V9938_WIDTH > 512){
	xx *= 2;
}
	while (xx-- !=0){
            ln.write((char) pen_bg);
            ln.inc();
        }
	if (_vdp.size_now != RENDER_HIGH) _vdp.size_now = RENDER_LOW;
        }
    };
    
    public static ModeVisible_8_HandlersPtr v9938_mode_multi_8s = new ModeVisible_8_HandlersPtr() {
        @Override
        public void handler(UBytePtr col, int line) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
    
    public static ModeVisible_16_HandlersPtr v9938_mode_multi_16s = new ModeVisible_16_HandlersPtr() {
        @Override
        public void handler(UShortPtr col, int line) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
    
    public static ModeDraw_Sprites_HandlersPtr v9938_default_draw_sprite_8 = new ModeDraw_Sprites_HandlersPtr() {
        @Override
        public void handler(UShortPtr ln, UShortPtr col) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
    
    public static ModeDraw_Sprites_HandlersPtr v9938_default_draw_sprite_8s = new ModeDraw_Sprites_HandlersPtr() {
        @Override
        public void handler(UShortPtr ln, UShortPtr col) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
    
    public static ModeDraw_Sprites_HandlersPtr v9938_default_draw_sprite_16 = new ModeDraw_Sprites_HandlersPtr() {
        @Override
        public void handler(UShortPtr ln, UShortPtr col) {
            int i;
if (V9938_WIDTH > 512){
	ln.inc( _vdp.offset_x * 2 );
} else {
	ln.inc( _vdp.offset_x);
}

	for (i=0;i<256;i++)
		{
		if ((col.read(i) & 0x80) != 0)
			{
			ln.write((char) Machine.pens[pal_ind16[col.read(i)&0x0f]]);
                        ln.inc();

//if (V9938_WIDTH > 512){
			ln.write((char) Machine.pens[pal_ind16[col.read(i)&0x0f]]);
                        ln.inc();
//}
			}
		else
//if (V9938_WIDTH > 512){
			ln.inc( 2 );
/*} else {
			ln.inc();
}*/
		}
        }
    };
    
    public static ModeDraw_Sprites_HandlersPtr v9938_default_draw_sprite_16s = new ModeDraw_Sprites_HandlersPtr() {
        @Override
        public void handler(UShortPtr ln, UShortPtr col) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
    
    public static ModeVisible_8_HandlersPtr v9938_mode_graphic1_8 = new ModeVisible_8_HandlersPtr() {
        @Override
        public void handler(UBytePtr col, int line) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
    
    public static ModeVisible_16_HandlersPtr v9938_mode_graphic1_16 = new ModeVisible_16_HandlersPtr() {
        @Override
        public void handler(UShortPtr ln, int line) {
            int fg, bg, pen;
	UBytePtr nametbl, patterntbl, colourtbl;
	int pattern, x, xx, line2, name, charcode, colour, xxx;

	nametbl = new UBytePtr(_vdp.vram, (_vdp.contReg[2] << 10));
	colourtbl = new UBytePtr(_vdp.vram, (_vdp.contReg[3] << 6) + (_vdp.contReg[10] << 14));
	patterntbl = new UBytePtr(_vdp.vram, (_vdp.contReg[4] << 11));

	line2 = (line - _vdp.contReg[23]) & 255;

	name = (line2/8)*32;

	pen = Machine.pens[pal_ind16[(_vdp.contReg[7]&0x0f)]];
if (V9938_WIDTH < 512){
	xxx = _vdp.offset_x;
} else {
	xxx = _vdp.offset_x * 2;
}
	while (xxx-- != 0){
            ln.write((char) pen);
            ln.inc();
        }

	for (x=0;x<32;x++)
		{
		charcode = nametbl.read(name);
		colour = colourtbl.read(charcode/8);
		fg = Machine.pens[pal_ind16[colour>>4]];
		bg = Machine.pens[pal_ind16[colour&15]];
		pattern = patterntbl.read(charcode * 8 + (line2 & 7));

		for (xx=0;xx<8;xx++)
			{
			ln.write((char) ((pattern & 0x80)!=0 ? fg : bg));
                        ln.inc();
                        if (V9938_WIDTH > 512){
                            ln.write((char) ((pattern & 0x80)!=0 ? fg : bg));
                            ln.inc();
                        }
			pattern <<= 1;
			}
		name++;
		}

	xx = 16 - _vdp.offset_x;	
        if (V9938_WIDTH > 512){
                xx *= 2;
        }
	while (xx-- != 0){
            ln.write((char) pen);
            ln.inc();
        }
	if (_vdp.size_now != RENDER_HIGH) _vdp.size_now = RENDER_LOW;
        }
    };
    
    public static ModeVisible_8_HandlersPtr v9938_mode_graphic1_8s = new ModeVisible_8_HandlersPtr() {
        @Override
        public void handler(UBytePtr col, int line) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
    
    public static ModeVisible_16_HandlersPtr v9938_mode_graphic1_16s = new ModeVisible_16_HandlersPtr() {
        @Override
        public void handler(UShortPtr col, int line) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
    
    public static ModeVisible_8_HandlersPtr v9938_mode_graphic23_8 = new ModeVisible_8_HandlersPtr() {
        @Override
        public void handler(UBytePtr col, int line) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
    
    public static ModeVisible_16_HandlersPtr v9938_mode_graphic23_16 = new ModeVisible_16_HandlersPtr() {
        @Override
        public void handler(UShortPtr ln, int line) {
            int fg, bg, pen;
	UBytePtr nametbl, patterntbl, colourtbl;
	int pattern, x, xx, line2, name, charcode, 
		colour, colourmask, patternmask, xxx;

	colourmask = (_vdp.contReg[3] & 0x7f) * 8 | 7;
	patternmask = (_vdp.contReg[4] & 0x03) * 256 | (colourmask & 255);

	nametbl = new UBytePtr(_vdp.vram, (_vdp.contReg[2] << 10));
 	colourtbl = new UBytePtr(_vdp.vram, ((_vdp.contReg[3] & 0x80) << 6) + (_vdp.contReg[10] << 14));
	patterntbl = new UBytePtr(_vdp.vram, ((_vdp.contReg[4] & 0x3c) << 11));

	line2 = (line + _vdp.contReg[23]) & 255;
	name = (line2/8)*32;

	pen = Machine.pens[pal_ind16[(_vdp.contReg[7]&0x0f)]];
if (V9938_WIDTH < 512){
	xxx = _vdp.offset_x;
} else {
	xxx = _vdp.offset_x * 2;
}
	while (xxx-- != 0){
            ln.write((char) pen);
            ln.inc();
        }	
	
	for (x=0;x<32;x++)
		{
		charcode = nametbl.read(name) + (line2&0xc0)*4;
		colour = colourtbl.read((charcode&colourmask)*8+(line2&7));
		pattern = patterntbl.read((charcode&patternmask)*8+(line2&7));
        fg = Machine.pens[pal_ind16[colour>>4]];
        bg = Machine.pens[pal_ind16[colour&15]];
		for (xx=0;xx<8;xx++)
			{	
			ln.write((char) ((pattern & 0x80)!=0 ? fg : bg));
                        ln.inc();
if (V9938_WIDTH > 512){
			ln.write((char) ((pattern & 0x80)!=0 ? fg : bg));
                        ln.inc();
}
            pattern <<= 1;
			}
		name++;
		}
	
	xx = 16 - _vdp.offset_x;
if (V9938_WIDTH > 512){
	xx *= 2;
}
	while (xx-- != 0){
            ln.write((char) pen);
            ln.inc();
        }
	if (_vdp.size_now != RENDER_HIGH) _vdp.size_now = RENDER_LOW;
        }
    };
    
    public static ModeVisible_8_HandlersPtr v9938_mode_graphic23_8s = new ModeVisible_8_HandlersPtr() {
        @Override
        public void handler(UBytePtr col, int line) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
    
    public static ModeVisible_16_HandlersPtr v9938_mode_graphic23_16s = new ModeVisible_16_HandlersPtr() {
        @Override
        public void handler(UShortPtr col, int line) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
    
    public static ModeVisible_8_HandlersPtr v9938_mode_graphic4_8 = new ModeVisible_8_HandlersPtr() {
        @Override
        public void handler(UBytePtr col, int line) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
    
    public static ModeVisible_16_HandlersPtr v9938_mode_graphic4_16 = new ModeVisible_16_HandlersPtr() {
        @Override
        public void handler(UShortPtr ln, int line) {
            UBytePtr nametbl;
            int colour;
	int line2, linemask, x, xx;
	int pen, pen_bg;

	linemask = ((_vdp.contReg[2] & 0x1f) << 3) | 7;

	line2 = ((line + _vdp.contReg[23]) & linemask) & 255;

	nametbl = new UBytePtr(_vdp.vram, ((_vdp.contReg[2] & 0x40) << 10) + line2 * 128);
	if ( (_vdp.contReg[2] & 0x20)!=0 && (V9938_SECOND_FIELD()!=0) )
		nametbl.inc(0x8000);

	pen_bg = Machine.pens[pal_ind16[(_vdp.contReg[7]&0x0f)]];
if (V9938_WIDTH < 512){
	xx = _vdp.offset_x;
}else{
	xx = _vdp.offset_x * 2;
}
	while (xx-- !=0){
            ln.write((char) pen_bg);
            ln.inc();
        }

	for (x=0;x<128;x++)
		{
		colour = nametbl.readinc();
        pen = Machine.pens[pal_ind16[colour>>4]];
		ln.write((char) pen); ln.inc();
if (V9938_WIDTH > 512){
		ln.write((char) pen);
                ln.inc();
}
        pen = Machine.pens[pal_ind16[colour&15]];
		ln.write((char) pen); ln.inc();
if (V9938_WIDTH > 512){
		ln.write((char) pen);
                ln.inc();
}
		}
	
	xx = 16 - _vdp.offset_x;	
if (V9938_WIDTH > 512){
	xx *= 2;
}
	while (xx-- !=0){
            ln.write((char) pen_bg);
            ln.inc();
        }
	if (_vdp.size_now != RENDER_HIGH) _vdp.size_now = RENDER_LOW;
        }
    };
    
    public static ModeVisible_8_HandlersPtr v9938_mode_graphic4_8s = new ModeVisible_8_HandlersPtr() {
        @Override
        public void handler(UBytePtr col, int line) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
    
    public static ModeVisible_16_HandlersPtr v9938_mode_graphic4_16s = new ModeVisible_16_HandlersPtr() {
        @Override
        public void handler(UShortPtr col, int line) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
    
    public static ModeVisible_8_HandlersPtr v9938_mode_graphic5_8 = new ModeVisible_8_HandlersPtr() {
        @Override
        public void handler(UBytePtr col, int line) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
    
    public static ModeVisible_16_HandlersPtr v9938_mode_graphic5_16 = new ModeVisible_16_HandlersPtr() {
        @Override
        public void handler(UShortPtr ln, int line) {
            //System.out.println("v9938_mode_graphic5_16");
            
            UBytePtr nametbl;
            int colour;
            int line2, linemask, x, xx;
            int[] pen_bg0 = new int[4];
            int[] pen_bg1 = new int[4];

            if (V9938_WIDTH > 512){
                    pen_bg1=new int[4];
            }

            linemask = ((_vdp.contReg[2] & 0x1f) << 3) | 7;

            line2 = ((line + _vdp.contReg[23]) & linemask) & 255;

            nametbl = new UBytePtr(_vdp.vram, ((_vdp.contReg[2] & 0x40) << 10) + line2 * 128);
            if ( (_vdp.contReg[2] & 0x20)!=0 && (V9938_SECOND_FIELD())!=0 )
                    nametbl.inc( 0x8000 );

    if (V9938_WIDTH > 512){
            pen_bg1[0] = Machine.pens[pal_ind16[(_vdp.contReg[7]&0x03)]];
            pen_bg0[0] = Machine.pens[pal_ind16[((_vdp.contReg[7]>>2)&0x03)]];

            xx = _vdp.offset_x;
            while (xx-- != 0) { 
                ln.write((char) pen_bg0[0]); 
                ln.inc(); 
                ln.write((char) pen_bg1[0]); 
                ln.inc();
            }

            x = (_vdp.contReg[8] & 0x20)!=0 ? 0 : 1;

        for (;x<4;x++)
                    {
                    pen_bg0[x] = Machine.pens[pal_ind16[x]];
                    pen_bg1[x] = Machine.pens[pal_ind16[x]];
                    }

            for (x=0;x<128;x++)
            {
                    colour = nametbl.readinc();

                    ln.write((char) pen_bg0[colour>>6]); ln.inc();
                    ln.write((char) pen_bg1[(colour>>4)&3]); ln.inc();
                    ln.write((char) pen_bg0[(colour>>2)&3]); ln.inc();
                    ln.write((char) pen_bg1[(colour&3)]); ln.inc();
            }

            pen_bg1[0] = Machine.pens[pal_ind16[(_vdp.contReg[7]&0x03)]];
            pen_bg0[0] = Machine.pens[pal_ind16[((_vdp.contReg[7]>>2)&0x03)]];
            xx = 16 - _vdp.offset_x;	
            while (xx-- != 0) { 
                ln.write((char) pen_bg0[0]); 
                ln.inc(); 
                ln.write((char) pen_bg1[0]); 
                ln.inc(); 
            }
    } else {
            pen_bg0[0] = Machine.pens[pal_ind16[((_vdp.contReg[7]>>2)&0x03)]];

            x = (_vdp.contReg[8] & 0x20)!=0 ? 0 : 1;

        for (;x<4;x++)
                    pen_bg0[x] = Machine.pens[pal_ind16[x]];

            xx = _vdp.offset_x;
            while (xx-- != 0) {
                ln.write((char) pen_bg0[0]); ln.inc();
            } 

            for (x=0;x<128;x++)
            {
                    colour = nametbl.readinc();
                    ln.write((char) pen_bg0[colour>>6]); ln.inc();
                    ln.write((char) pen_bg0[(colour>>2)&3]); ln.inc();
            }

            pen_bg0[0] = Machine.pens[pal_ind16[((_vdp.contReg[7]>>2)&0x03)]];
            xx = 16 - _vdp.offset_x;	
            while (xx-- != 0){ 
                ln.write((char) pen_bg0[0]);
                ln.inc();
            } 
    }
            _vdp.size_now = RENDER_HIGH;
        }
    };
    
    public static ModeVisible_8_HandlersPtr v9938_mode_graphic5_8s = new ModeVisible_8_HandlersPtr() {
        @Override
        public void handler(UBytePtr col, int line) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
    
    public static ModeVisible_16_HandlersPtr v9938_mode_graphic5_16s = new ModeVisible_16_HandlersPtr() {
        @Override
        public void handler(UShortPtr ln, int line) {
            //System.out.println("v9938_mode_graphic5_16");
            UBytePtr nametbl;
            int colour;
            int line2, linemask, x, xx;
            int[] pen_bg0 = new int[4];
            int[] pen_bg1 = new int[4];

            if (V9938_WIDTH > 512){
                    pen_bg1=new int[4];
            }

            linemask = ((_vdp.contReg[2] & 0x1f) << 3) | 7;

            line2 = ((line + _vdp.contReg[23]) & linemask) & 255;

            nametbl = new UBytePtr(_vdp.vram, ((_vdp.contReg[2] & 0x40) << 10) + line2 * 128);
            if ( (_vdp.contReg[2] & 0x20)!=0 && (V9938_SECOND_FIELD())!=0 )
                    nametbl.inc( 0x8000 );

    if (V9938_WIDTH > 512){
            pen_bg1[0] = Machine.pens[pal_ind16[(_vdp.contReg[7]&0x03)]];
            pen_bg0[0] = Machine.pens[pal_ind16[((_vdp.contReg[7]>>2)&0x03)]];

            xx = _vdp.offset_x;
            while (xx-- != 0) { 
                ln.write((char) pen_bg0[0]); ln.inc(); ln.write((char) pen_bg1[0]); ln.inc();
            }

            x = (_vdp.contReg[8] & 0x20)!=0 ? 0 : 1;

        for (;x<4;x++)
                    {
                    pen_bg0[x] = Machine.pens[pal_ind16[x]];
                    pen_bg1[x] = Machine.pens[pal_ind16[x]];
                    }

            for (x=0;x<128;x++)
            {
                    colour = nametbl.readinc();

                    ln.write((char) pen_bg0[colour>>6]); ln.inc();
                    ln.write((char) pen_bg1[(colour>>4)&3]); ln.inc();
                    ln.write((char) pen_bg0[(colour>>2)&3]); ln.inc();
                    ln.write((char) pen_bg1[(colour&3)]); ln.inc();
            }

            pen_bg1[0] = Machine.pens[pal_ind16[(_vdp.contReg[7]&0x03)]];
            pen_bg0[0] = Machine.pens[pal_ind16[((_vdp.contReg[7]>>2)&0x03)]];
            xx = 16 - _vdp.offset_x;	
            while (xx-- != 0) { ln.write((char) pen_bg0[0]); ln.inc(); ln.write((char) pen_bg1[0]); ln.inc(); }
    } else {
            pen_bg0[0] = Machine.pens[pal_ind16[((_vdp.contReg[7]>>2)&0x03)]];

            x = (_vdp.contReg[8] & 0x20)!=0 ? 0 : 1;

        for (;x<4;x++)
                    pen_bg0[x] = Machine.pens[pal_ind16[x]];

            xx = _vdp.offset_x;
            while (xx-- != 0) {
                ln.write((char) pen_bg0[0]); ln.inc();
            } 

            for (x=0;x<128;x++)
            {
                    colour = nametbl.readinc();
                    ln.write((char) pen_bg0[colour>>6]); ln.inc();
                    ln.write((char) pen_bg0[(colour>>2)&3]); ln.inc();
            }

            pen_bg0[0] = Machine.pens[pal_ind16[((_vdp.contReg[7]>>2)&0x03)]];
            xx = 16 - _vdp.offset_x;	
            while (xx-- != 0){ 
                ln.write((char) pen_bg0[0]);
                ln.inc();
            } 
    }
            _vdp.size_now = RENDER_HIGH;
        }
    };
    
    public static ModeBorder_8_HandlersPtr v9938_graphic5_border_8 = new ModeBorder_8_HandlersPtr() {
        @Override
        public void handler(UBytePtr ln) {
            int i;
	int pen0;
if (V9938_WIDTH > 512){
	int pen1;

	pen1 = Machine.pens[pal_ind16[(_vdp.contReg[7]&0x03)]];
	pen0 = Machine.pens[pal_ind16[((_vdp.contReg[7]>>2)&0x03)]];
	i = (V9938_WIDTH) / 2;
	while (i-- != 0) { 
            ln.writeinc( pen0 ); ln.writeinc( pen1 );
        }
} else {
	pen0 = Machine.pens[pal_ind16[((_vdp.contReg[7]>>2)&0x03)]];
	i = V9938_WIDTH;
	while (i-- != 0) ln.writeinc( pen0 );
}
	_vdp.size_now = RENDER_HIGH;
        }
    };
    
    public static ModeBorder_16_HandlersPtr v9938_graphic5_border_16 = new ModeBorder_16_HandlersPtr() {
        @Override
        public void handler(UShortPtr ln) {
            //System.out.println("v9938_graphic5_border_16");
            int i;
	int pen0;
        /*if (V9938_WIDTH > 512){
                int pen1;

                pen1 = Machine.pens[pal_ind16[(_vdp.contReg[7]&0x03)]];
                pen0 = Machine.pens[pal_ind16[((_vdp.contReg[7]>>2)&0x03)]];
                i = (V9938_WIDTH) / 2;
                while (i-- != 0) { 
                    ln.write((char) pen0);
                    ln.inc();
                    ln.write((char) pen1);        
                    ln.inc();
                }
        } else {*/
                pen0 = Machine.pens[pal_ind16[((_vdp.contReg[7]>>2)&0x03)]];
                i = V9938_WIDTH;
                while (i-- != 0){
                    ln.write((char) pen0);
                    ln.inc();
                }
        //}
	_vdp.size_now = RENDER_HIGH;
        }
    };
    
    public static ModeBorder_8_HandlersPtr v9938_graphic5_border_8s = new ModeBorder_8_HandlersPtr() {
        @Override
        public void handler(UBytePtr col) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
    
    public static ModeBorder_16_HandlersPtr v9938_graphic5_border_16s = new ModeBorder_16_HandlersPtr() {
        @Override
        public void handler(UShortPtr ln) {
            //System.out.println("v9938_graphic5_border_16");
            int i;
	int pen0;
        if (V9938_WIDTH > 512){
                int pen1;

                pen1 = Machine.pens[pal_ind16[(_vdp.contReg[7]&0x03)]];
                pen0 = Machine.pens[pal_ind16[((_vdp.contReg[7]>>2)&0x03)]];
                i = (V9938_WIDTH) / 2;
                while (i-- != 0) { 
                    ln.write((char) pen0);
                    ln.inc();
                    ln.write((char) pen1);        
                    ln.inc();
                }
        } else {
                pen0 = Machine.pens[pal_ind16[((_vdp.contReg[7]>>2)&0x03)]];
                i = V9938_WIDTH;
                while (i-- != 0){
                    ln.write((char) pen0);
                    ln.inc();
                }
        }
	_vdp.size_now = RENDER_HIGH;
        }
    };
   
    public static ModeDraw_Sprites_HandlersPtr v9938_graphic5_draw_sprite_8 = new ModeDraw_Sprites_HandlersPtr() {
        @Override
        public void handler(UShortPtr ln, UShortPtr col) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
    
    public static ModeDraw_Sprites_HandlersPtr v9938_graphic5_draw_sprite_16 = new ModeDraw_Sprites_HandlersPtr() {
        @Override
        public void handler(UShortPtr ln, UShortPtr col) {
            //System.out.println("v9938_graphic5_draw_sprite_16");
            int i;
if (V9938_WIDTH > 512){
	ln.inc( _vdp.offset_x * 2 );
} else {
	ln.inc( _vdp.offset_x );
}

	for (i=0;i<256;i++)
		{
		if ((col.read(i) & 0x80) != 0)
		{
			ln.write((char) Machine.pens[pal_ind16[(col.read(i)>>2)&0x03]]);
                        ln.inc();
                        if (V9938_WIDTH > 512){
                            ln.write((char) Machine.pens[pal_ind16[col.read(i)&0x03]]);
                            ln.inc();
                        }
		}
		else
if (V9938_WIDTH > 512){
			ln.inc( 2 );
} else {
			ln.inc();
}
		}
        }
    };
    
    public static ModeDraw_Sprites_HandlersPtr v9938_graphic5_draw_sprite_8s = new ModeDraw_Sprites_HandlersPtr() {
        @Override
        public void handler(UShortPtr ln, UShortPtr col) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
    
    public static ModeDraw_Sprites_HandlersPtr v9938_graphic5_draw_sprite_16s = new ModeDraw_Sprites_HandlersPtr() {
        @Override
        public void handler(UShortPtr ln, UShortPtr col) {
            //System.out.println("v9938_graphic5_draw_sprite_16");
            int i;
if (V9938_WIDTH > 512){
	ln.inc( _vdp.offset_x * 2 );
} else {
	ln.inc( _vdp.offset_x );
}

	for (i=0;i<256;i++)
		{
		if ((col.read(i) & 0x80) != 0)
		{
			ln.write((char) Machine.pens[pal_ind16[(col.read(i)>>2)&0x03]]);
                        ln.inc();
                        if (V9938_WIDTH > 512){
                            ln.write((char) Machine.pens[pal_ind16[col.read(i)&0x03]]);
                            ln.inc();
                        }
		}
		else
if (V9938_WIDTH > 512){
			ln.inc( 2 );
} else {
			ln.inc();
}
		}
        }
    };
    
    public static ModeVisible_8_HandlersPtr v9938_mode_graphic6_8 = new ModeVisible_8_HandlersPtr() {
        @Override
        public void handler(UBytePtr col, int line) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
    
    public static ModeVisible_16_HandlersPtr v9938_mode_graphic6_16 = new ModeVisible_16_HandlersPtr() {
        @Override
        public void handler(UShortPtr ln, int line) {
            int colour;
    int line2, linemask, x, xx, nametbl;
    int pen_bg, fg0;
//if (V9938_WIDTH > 512){
	int fg1;
//}
	
    linemask = ((_vdp.contReg[2] & 0x1f) << 3) | 7;

	line2 = ((line + _vdp.contReg[23]) & linemask) & 255;

    nametbl = line2 << 8 ;
   	if ( (_vdp.contReg[2] & 0x20)!=0 && (V9938_SECOND_FIELD()!=0) )
        nametbl += 0x10000;	
	
	pen_bg = Machine.pens[pal_ind16[(_vdp.contReg[7]&0x0f)]];
if (V9938_WIDTH < 512){
	xx = _vdp.offset_x;
} else {
	xx = _vdp.offset_x * 2;
}
	while (xx-- != 0){
            ln.write((char) pen_bg);
            ln.inc();
        }
	
	if ((_vdp.contReg[2] & 0x40) != 0)
		{
		for (x=0;x<32;x++)
			{
			nametbl++;
			colour = _vdp.vram.read(((nametbl&1) << 16) | (nametbl>>1));	
        	fg0 = Machine.pens[pal_ind16[colour>>4]];
if (V9938_WIDTH < 512){
			ln.write((char) fg0);ln.inc(); ln.write((char) fg0);ln.inc(); 
			ln.write((char) fg0);ln.inc(); ln.write((char) fg0);ln.inc();
			ln.write((char) fg0);ln.inc(); ln.write((char) fg0);ln.inc(); 
			ln.write((char) fg0);ln.inc(); ln.write((char) fg0);ln.inc();
} else {
        	fg1 = Machine.pens[pal_ind16[colour&15]];
			ln.write((char) fg0);ln.inc(); ln.write((char) fg1);ln.inc(); ln.write((char) fg0);ln.inc(); ln.write((char) fg1);ln.inc(); 
			ln.write((char) fg0);ln.inc(); ln.write((char) fg1);ln.inc(); ln.write((char) fg0);ln.inc(); ln.write((char) fg1);ln.inc();  
			ln.write((char) fg0);ln.inc(); ln.write((char) fg1);ln.inc(); ln.write((char) fg0);ln.inc(); ln.write((char) fg1);ln.inc(); 
			ln.write((char) fg0);ln.inc(); ln.write((char) fg1);ln.inc(); ln.write((char) fg0);ln.inc(); ln.write((char) fg1);ln.inc(); 
}
			nametbl += 7;
			}
		}
	else
		{
		for (x=0;x<256;x++)	
			{	
			colour = _vdp.vram.read(((nametbl&1) << 16) | (nametbl>>1));	
        	ln.write((char) Machine.pens[pal_ind16[colour>>4]]); ln.inc();
if (V9938_WIDTH > 512){
        	ln.write((char) Machine.pens[pal_ind16[colour&15]]);
                ln.inc();
}
			nametbl++;
        	}
		}
	
	xx = 16 - _vdp.offset_x;
if (V9938_WIDTH > 512){
	xx *= 2; 
}
	while (xx-- !=0){
            ln.write((char) pen_bg);
            ln.inc();
        }
	_vdp.size_now = RENDER_HIGH;
        }
    };
    
    public static ModeVisible_8_HandlersPtr v9938_mode_graphic6_8s = new ModeVisible_8_HandlersPtr() {
        @Override
        public void handler(UBytePtr col, int line) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
    
    public static ModeVisible_16_HandlersPtr v9938_mode_graphic6_16s = new ModeVisible_16_HandlersPtr() {
        @Override
        public void handler(UShortPtr col, int line) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
    
    public static ModeVisible_8_HandlersPtr v9938_mode_graphic7_8 = new ModeVisible_8_HandlersPtr() {
        @Override
        public void handler(UBytePtr col, int line) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
    
    public static ModeVisible_16_HandlersPtr v9938_mode_graphic7_16 = new ModeVisible_16_HandlersPtr() {
        @Override
        public void handler(UShortPtr ln, int line) {
            int colour;
    int line2, linemask, x, xx, nametbl;
    int pen, pen_bg;

   	linemask = ((_vdp.contReg[2] & 0x1f) << 3) | 7;

	line2 = ((line + _vdp.contReg[23]) & linemask) & 255;

	nametbl = line2 << 8;
   	if ( (_vdp.contReg[2] & 0x20)!=0 && (V9938_SECOND_FIELD()!=0) )
		nametbl += 0x10000;	
	
	pen_bg = Machine.pens[pal_ind256[_vdp.contReg[7]]];	
if (V9938_WIDTH < 512){
	xx = _vdp.offset_x;
} else {
	xx = _vdp.offset_x * 2;
}
	while (xx-- !=0){
            ln.write((char) pen_bg);
            ln.inc();
        }

	if ((_vdp.contReg[2] & 0x40) != 0)
		{
		for (x=0;x<32;x++)
			{
			nametbl++;
			colour = _vdp.vram.read(((nametbl&1) << 16) | (nametbl>>1));	
			pen = Machine.pens[pal_ind256[colour]];
			ln.write((char) pen);ln.inc(); ln.write((char) pen);ln.inc();
			ln.write((char) pen);ln.inc(); ln.write((char) pen);ln.inc();
			ln.write((char) pen);ln.inc(); ln.write((char) pen);ln.inc();
			ln.write((char) pen);ln.inc(); ln.write((char) pen);ln.inc();
if (V9938_WIDTH > 512){
			ln.write((char) pen);ln.inc(); ln.write((char) pen);ln.inc();
			ln.write((char) pen);ln.inc(); ln.write((char) pen);ln.inc();
			ln.write((char) pen);ln.inc(); ln.write((char) pen);ln.inc();
			ln.write((char) pen);ln.inc(); ln.write((char) pen);ln.inc();
}
			nametbl++;
			}
		}
	else
		{
  		for (x=0;x<256;x++)	
        	{	
			colour = _vdp.vram.read(((nametbl&1) << 16) | (nametbl>>1));	
			pen = Machine.pens[pal_ind256[colour]];
			ln.write((char) pen);ln.inc();
if (V9938_WIDTH > 512){
		 	ln.write((char) pen);ln.inc();
}
			nametbl++;	
       		}
		}

	xx = 16 - _vdp.offset_x;	
if (V9938_WIDTH > 512){
	xx *= 2; 
}
	while (xx-- != 0){
            ln.write((char) pen_bg);
            ln.inc();
        }
	if (_vdp.size_now != RENDER_HIGH) _vdp.size_now = RENDER_LOW;
        }
    };
    
    public static ModeVisible_8_HandlersPtr v9938_mode_graphic7_8s = new ModeVisible_8_HandlersPtr() {
        @Override
        public void handler(UBytePtr col, int line) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
    
    public static ModeVisible_16_HandlersPtr v9938_mode_graphic7_16s = new ModeVisible_16_HandlersPtr() {
        @Override
        public void handler(UShortPtr col, int line) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
    
    public static ModeBorder_8_HandlersPtr v9938_graphic7_border_8 = new ModeBorder_8_HandlersPtr() {
        @Override
        public void handler(UBytePtr col) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
    
    public static ModeBorder_16_HandlersPtr v9938_graphic7_border_16 = new ModeBorder_16_HandlersPtr() {
        @Override
        public void handler(UShortPtr ln) {
            int pen;
	int i;

	pen = Machine.pens[pal_ind256[_vdp.contReg[7]]];
	i = V9938_WIDTH;
	while (i-- != 0){
            ln.write((char) pen);
            ln.inc();
        }

	if (_vdp.size_now != RENDER_HIGH) _vdp.size_now = RENDER_LOW;
        }
    };
    
    public static ModeBorder_8_HandlersPtr v9938_graphic7_border_8s = new ModeBorder_8_HandlersPtr() {
        @Override
        public void handler(UBytePtr col) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
    
    public static ModeBorder_16_HandlersPtr v9938_graphic7_border_16s = new ModeBorder_16_HandlersPtr() {
        @Override
        public void handler(UShortPtr col) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
    
    public static ModeDraw_Sprites_HandlersPtr v9938_graphic7_draw_sprite_8 = new ModeDraw_Sprites_HandlersPtr() {
        @Override
        public void handler(UShortPtr ln, UShortPtr col) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
    
    static int g7_ind16[] = {
		0, 2, 192, 194, 48, 50, 240, 242,
		482, 7, 448, 455, 56, 63, 504, 511  };
    
    public static ModeDraw_Sprites_HandlersPtr v9938_graphic7_draw_sprite_16 = new ModeDraw_Sprites_HandlersPtr() {
        @Override
        public void handler(UShortPtr ln, UShortPtr col) {
            int i;

if (V9938_WIDTH > 512){
	ln.write((char) (ln.read(0)+ _vdp.offset_x * 2));
} else {
	ln.write((char) (ln.read(0)+ _vdp.offset_x));
}

	for (i=0;i<256;i++)
		{
		if ((col.read(i) & 0x80) != 0)
			{
			ln.write((char) Machine.pens[g7_ind16[col.read(i)&0x0f]]);
                        ln.inc();
if (V9938_WIDTH > 512){
			ln.write((char) Machine.pens[g7_ind16[col.read(i)&0x0f]]);
                        ln.inc();
}
			}
		else
if (V9938_WIDTH > 512){
			ln.inc( 2 );
} else {
			ln.inc();
}
		}
        }
    };
    
    public static ModeDraw_Sprites_HandlersPtr v9938_graphic7_draw_sprite_8s = new ModeDraw_Sprites_HandlersPtr() {
        @Override
        public void handler(UShortPtr ln, UShortPtr col) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
    
    public static ModeDraw_Sprites_HandlersPtr v9938_graphic7_draw_sprite_16s = new ModeDraw_Sprites_HandlersPtr() {
        @Override
        public void handler(UShortPtr ln, UShortPtr col) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
    
    public static ModeVisible_8_HandlersPtr v9938_mode_text2_8 = new ModeVisible_8_HandlersPtr() {
        @Override
        public void handler(UBytePtr col, int line) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
    
    public static ModeVisible_16_HandlersPtr v9938_mode_text2_16 = new ModeVisible_16_HandlersPtr() {
        @Override
        public void handler(UShortPtr ln, int line) {
            int pattern, x, charcode, name, xxx, patternmask, colourmask;	
	int fg, bg, fg0, bg0, pen;	
	UBytePtr nametbl, patterntbl, colourtbl;	
	
	patterntbl = new UBytePtr(_vdp.vram, (_vdp.contReg[4] << 11));	
	colourtbl = new UBytePtr(_vdp.vram, ((_vdp.contReg[3] & 0xf8) << 6) + (_vdp.contReg[10] << 14));	
	colourmask = ((_vdp.contReg[3] & 7) << 5) | 0x1f; /* verify! */	
	nametbl = new UBytePtr(_vdp.vram, ((_vdp.contReg[2] & 0xfc) << 10));	
	patternmask = ((_vdp.contReg[2] & 3) << 10) | 0x3ff; /* seems correct */	
	
    fg = Machine.pens[pal_ind16[_vdp.contReg[7] >> 4]];	
    bg = Machine.pens[pal_ind16[_vdp.contReg[7] & 15]];	
    fg0 = Machine.pens[pal_ind16[_vdp.contReg[12] >> 4]];	
    bg0 = Machine.pens[pal_ind16[_vdp.contReg[12] & 15]];
	
	name = (line/8)*80;	
	
	xxx = _vdp.offset_x + 8;	
	pen = Machine.pens[pal_ind16[(_vdp.contReg[7]&0x0f)]];	
if (V9938_WIDTH > 512){
	xxx *= 2;
}
	while (xxx-- != 0){
            ln.write((char) pen);
            ln.inc();
        }	
	
	for (x=0;x<80;x++)	
		{	
		charcode = nametbl.read(name&patternmask);	
		if (_vdp.blink != 0)	
			{	
			pattern = colourtbl.read((name/8)&colourmask);	
			if ((pattern & (0x80 >> (name & 7) ) )	!= 0)
				{	
				pattern = patterntbl.read((charcode * 8) + 	
					((line + _vdp.contReg[23]) & 7));

if (V9938_WIDTH > 512){
				ln.write((char) ((pattern & 0x80)!=0 ? fg0 : bg0)); ln.inc();
				ln.write((char) ((pattern & 0x40)!=0 ? fg0 : bg0)); ln.inc();
				ln.write((char) ((pattern & 0x20)!=0 ? fg0 : bg0)); ln.inc();
				ln.write((char) ((pattern & 0x10)!=0 ? fg0 : bg0)); ln.inc();
				ln.write((char) ((pattern & 0x08)!=0 ? fg0 : bg0)); ln.inc();
				ln.write((char) ((pattern & 0x04)!=0 ? fg0 : bg0)); ln.inc();
} else {
				ln.write((char) ((pattern & 0x80)!=0 ? fg0 : bg0)); ln.inc();
				ln.write((char) ((pattern & 0x20)!=0 ? fg0 : bg0)); ln.inc();
				ln.write((char) ((pattern & 0x08)!=0 ? fg0 : bg0)); ln.inc();
}
		
				name++;	
				continue;	
				}	
			}	
	
		pattern = patterntbl.read((charcode * 8) + 	
			((line + _vdp.contReg[23]) & 7));

if (V9938_WIDTH > 512){
		ln.write((char) ((pattern & 0x80)!=0 ? fg : bg)); ln.inc();
		ln.write((char) ((pattern & 0x40)!=0 ? fg : bg)); ln.inc();
		ln.write((char) ((pattern & 0x20)!=0 ? fg : bg)); ln.inc();
		ln.write((char) ((pattern & 0x10)!=0 ? fg : bg)); ln.inc();
		ln.write((char) ((pattern & 0x08)!=0 ? fg : bg)); ln.inc();
		ln.write((char) ((pattern & 0x04)!=0 ? fg : bg)); ln.inc();
} else {
		ln.write((char) ((pattern & 0x80)!=0 ? fg : bg)); ln.inc();
		ln.write((char) ((pattern & 0x20)!=0 ? fg : bg)); ln.inc();
		ln.write((char) ((pattern & 0x08)!=0 ? fg : bg)); ln.inc();
}

		name++;
		}

	xxx = 16  - _vdp.offset_x + 8;	
if (V9938_WIDTH > 512){
	xxx *= 2;
}
	while (xxx-- != 0){
            ln.write( (char) pen );
            ln.inc();
        }
	_vdp.size_now = RENDER_HIGH;
        }
    };
    
    public static ModeVisible_8_HandlersPtr v9938_mode_text2_8s = new ModeVisible_8_HandlersPtr() {
        @Override
        public void handler(UBytePtr col, int line) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
    
    public static ModeVisible_16_HandlersPtr v9938_mode_text2_16s = new ModeVisible_16_HandlersPtr() {
        @Override
        public void handler(UShortPtr col, int line) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
    
    public static ModeVisible_8_HandlersPtr v9938_mode_unknown_8 = new ModeVisible_8_HandlersPtr() {
        @Override
        public void handler(UBytePtr col, int line) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
    
    public static ModeVisible_16_HandlersPtr v9938_mode_unknown_16 = new ModeVisible_16_HandlersPtr() {
        @Override
        public void handler(UShortPtr ln, int line) {
            int fg, bg;
            int x;
	
            fg = Machine.pens[pal_ind16[_vdp.contReg[7] >> 4]];
            bg = Machine.pens[pal_ind16[_vdp.contReg[7] & 15]];	
	
            if (V9938_WIDTH < 512){
                    x = _vdp.offset_x;
                    while (x-- != 0){
                        ln.write((char) bg);
                        ln.inc();
                    }	

                    x = 256;
                    while (x-- != 0){
                        ln.write((char) fg);
                        ln.inc();
                    }	

                    x = 16 - _vdp.offset_x;
                    while (x-- != 0){
                        ln.write((char) bg);
                        ln.inc();
                    }
            } else {
                x = _vdp.offset_x * 2;
                while (x-- != 0){
                    ln.write((char) bg);
                    ln.inc();
                }	

                x = 512;
                while (x-- != 0){
                    ln.write((char) fg);
                    ln.inc();
                }	

                x = (16 - _vdp.offset_x) * 2;
                while (x-- != 0){
                    ln.write((char) bg);
                    ln.inc();
                }
            }
            
            if (_vdp.size_now != RENDER_HIGH) _vdp.size_now = RENDER_LOW;
        }
    };
    
    public static ModeVisible_8_HandlersPtr v9938_mode_unknown_8s = new ModeVisible_8_HandlersPtr() {
        @Override
        public void handler(UBytePtr col, int line) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
    
    public static ModeVisible_16_HandlersPtr v9938_mode_unknown_16s = new ModeVisible_16_HandlersPtr() {
        @Override
        public void handler(UShortPtr col, int line) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
    
}
