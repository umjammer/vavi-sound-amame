/******************************************************************************
 PeT mess@utanet.at 2000,2001
******************************************************************************/
/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.machine;

import static arcadeflex056.fucPtr.*;
import static arcadeflex056.osdepend.logerror;
import static common.ptr.*;
import static mame056.common.*;
import static mame056.commonH.*;
import static mame056.cpu.m6502.m6502H.M65SC02_IRQ_LINE;
import static mame056.cpuintrf.*;
import static mame056.cpuintrfH.*;
import static mame056.cpuexec.*;
import static mame056.timer.*;
import static mame056.timerH.*;
import static mame056.inptport.*;
import static mame056.mame.*;
import static mame056.memory.*;
import static mame056.memoryH.*;
import static mess056.includes.lynxH.*;
import static mess056.systems.lynx.lynx_draw_lines;
import static mess056.systems.lynx.lynx_rotate;
import static mess056.systems.lynx.lynx_palette;

public class lynx
{
	
	public static int lynx_granularity=1;
	public static int lynx_line;
        
        public static class _hl {
            public int l;
            public int h;
        }
        
        public static class _s 
        {
         
            // eng used by the blitter engine
            // scb written by the blitter scb blocks to engine expander
            //     used by the engine
            //     might be used directly by software!
            public _hl eng1=new _hl(), eng2=new _hl(),
                h_offset=new _hl(), v_offset=new _hl(), vidbas=new _hl(), colbas=new _hl(),
                eng3=new _hl(), eng4=new _hl(), 
                scb1=new _hl(), scb2=new _hl(), scb3=new _hl(), scb4=new _hl(), scb5=new _hl(), scb6=new _hl(),
                eng5=new _hl(), eng6=new _hl(), eng7=new _hl(), eng8=new _hl(),
                colloff=new _hl(),
                eng9=new _hl(),
                hsizoff=new _hl(), vsizoff=new _hl(),
                eng10=new _hl(), eng11=new _hl();

            public int[] res = new int[0x20];
            public int[] used= new int[2];
            public int D,C,B,A,P,N;
            public int[] res1 = new int[8];
            public int H,G,F,E;
            public int[] res2 = new int[8];
            public int M,L,K,J;
            public int[] res3 = new int[0x21];
            public int SPRG0;
            public int SPRSYS;
        }
        
        public static class _u
        {
            public UBytePtr data = new UBytePtr(0x100);
            public _s s = new _s();
        }
	
	public static class SUZY {
	    public _u u = new _u();
	    public boolean accumulate_overflow = false;
	    public int high;
	    public int low;
	};
	
	static SUZY suzy=new SUZY();
	
	public static class _blitter {
	    public UBytePtr mem = new UBytePtr();
	    // global
	    public int screen;
	    public int colbuf;
	    public int colpos; // byte where value of collision is written
	    public int xoff, yoff;
	    // in command
	    public int mode;
	    public int cmd;
	    public int spritenr;
	    public int x,y;
	    public int width, height; // uint16 important for blue lightning
	    public int stretch, tilt; // uint16 important
	    public int[] color = new int[16]; // or stored
	    public WriteHandlerPtr line_function;
	    public int bitmap;
	
	    public boolean everon = false;
	    public int memory_accesses;
	    public double time;
	};
        
        public static _blitter blitter = new _blitter();
	
	public static int GET_WORD(UBytePtr mem, int index){
            return (mem.read(index)| mem.read(index+1)<<8);
        }
	
	/*
	mode from blitter command 
	#define SHADOW         (0x07)
	#define XORSHADOW      (0x06)
	#define NONCOLLIDABLE  (0x05)
	#define NORMAL         (0x04)
	#define BOUNDARY       (0x03)
	#define BOUNDARYSHADOW (0x02)
	#define BKGRNDNOCOL    (0x01)
	#define BKGRND         (0x00)
	*/
	
	public static void lynx_plot_pixel(int mode, int x, int y, int color)
	{
	    int back;
	    UBytePtr screen;
	    UBytePtr colbuf;
	    
	    blitter.everon=true;
	    screen=new UBytePtr(blitter.mem, blitter.screen+y*80+x/2);
	    colbuf=new UBytePtr(blitter.mem, blitter.colbuf+y*80+x/2);
	    switch (mode) {
	    case 0x00: // background (shadow bug!)
		if ((x&1)==0) {
		    screen.write((screen.read()&0x0f)|(color<<4));
		} else {
		    screen.write((screen.read()&0xf0)|color);
		}
		blitter.memory_accesses++;
		break;
	    case 0x10:
		if ((x&1)==0) {
		    if (color!=0xe) {
			colbuf.write((colbuf.read()&~0xf0)|(blitter.spritenr<<4));
			blitter.memory_accesses++;
		    }
		    screen.write((screen.read()&0x0f)|(color<<4));
		} else {
		    if (color!=0xe) {
			colbuf.write((colbuf.read()&~0xf)|(blitter.spritenr));
			blitter.memory_accesses++;
		    }
		    screen.write((screen.read()&0xf0)|color);
		}
		blitter.memory_accesses++;
		break;
	    case 0x01: // background, no colliding
	    case 0x11:
		if ((x&1)==0) {
		    screen.write((screen.read()&0x0f)|(color<<4));
		} else {
		    screen.write((screen.read()&0xf0)|color);
		}
		blitter.memory_accesses++;
		break;
	    case 0x03: // boundary: pen P? transparent, but collides
	    case 0x02: // boundary, shadow
		if ((color==0)||(color==0xf)) break;
		if ((x&1)==0) {
		    screen.write((screen.read()&0x0f)|(color<<4));
		} else {
		    screen.write((screen.read()&0xf0)|color);
		}
		blitter.memory_accesses++;
		break;
	    case 0x13:
		if (color==0) break;
		if ((x&1)==0) {
		    back=colbuf.read();
		    if (back>>4>blitter.mem.read(blitter.colpos))
			blitter.mem.write(blitter.colpos, back>>4);
		    colbuf.write((back&~0xf0)|(blitter.spritenr<<4));
		    if (color!=0xf) {
			screen.write((screen.read()&0x0f)|(color<<4));
			blitter.memory_accesses++;
		    }
		} else {
		    back=colbuf.read();
		    if ((back&0xf)>blitter.mem.read(blitter.colpos))
			blitter.mem.write(blitter.colpos, back&0xf);
		    colbuf.write((back&~0xf)|(blitter.spritenr));
		    if (color!=0xf) {
			screen.write((screen.read()&0xf0)|color);
			blitter.memory_accesses++;
		    }
		}
		blitter.memory_accesses+=2;
		break;
	    case 0x12:
		if (color==0) break;
		if ((x&1)==0) {
		    if (color!=0xe) {
			back=colbuf.read();
			if (back>>4>blitter.mem.read(blitter.colpos))
			    blitter.mem.write(blitter.colpos, back>>4);
			colbuf.write((back&~0xf0)|(blitter.spritenr<<4));
		    }
		    if (color!=0xf) {
			screen.write((screen.read()&0x0f)|(color<<4));
			blitter.memory_accesses++;
		    }
		} else {
		    if (color!=0xe) {
			back=colbuf.read();
			if ((back&0xf)>blitter.mem.read(blitter.colpos))
			    blitter.mem.write(blitter.colpos, back&0xf);
			colbuf.write((back&~0xf)|(blitter.spritenr));
		    }
		    if (color!=0xf) {
			screen.write((screen.read()&0xf0)|color);
			blitter.memory_accesses++;
		    }
		}
		blitter.memory_accesses+=2;
		break;
	    case 0x04: // pen 0 transparent, 
	    case 0x07: // shadow: pen e doesn't collide
	    case 0x05: // non collidable sprite
	    case 0x15:
		if (color==0) break;
		if ((x&1)==0) {
		    screen.write((screen.read()&0x0f)|(color<<4));
		} else {
		    screen.write((screen.read()&0xf0)|color);
		}
		blitter.memory_accesses++;
		break;
	    case 0x14:
		if (color==0) break;
		if ((x&1)==0) {
		    back=colbuf.read();
		    colbuf.write((back&~0xf0)|(blitter.spritenr<<4));
		    if (back>>4>blitter.mem.read(blitter.colpos))
			blitter.mem.write(blitter.colpos, back>>4);
		    screen.write((screen.read()&0x0f)|(color<<4));
		} else {
		    back=colbuf.read();
		    colbuf.write((back&~0xf)|(blitter.spritenr));
		    if ((back&0xf)>blitter.mem.read(blitter.colpos))
			blitter.mem.write(blitter.colpos, back&0xf);
		    screen.write((screen.read()&0xf0)|color);
		}
		blitter.memory_accesses+=3;
		break;
	    case 0x17:
		if (color==0) break;
		if ((x&1)==0) {
		    if (color!=0xe) {
			back=colbuf.read();
			if (back>>4>blitter.mem.read(blitter.colpos))
			    blitter.mem.write(blitter.colpos, back>>4);
			colbuf.write((back&~0xf0)|(blitter.spritenr<<4));
			blitter.memory_accesses+=2;
		    }
		    screen.write((screen.read()&0x0f)|(color<<4));
		} else {
		    if (color!=0xe) {
			back=colbuf.read();
			if ((back&0xf)>blitter.mem.read(blitter.colpos))
			    blitter.mem.write(blitter.colpos, back&0xf);
			colbuf.write((back&~0xf)|(blitter.spritenr));
			blitter.memory_accesses+=2;
		    }
		    screen.write((screen.read()&0xf0)|color);
		}
		blitter.memory_accesses++;
		break;
	    case 0x06: // xor sprite (shadow bug!)
		if ((x&1)==0) {
		    screen.write((screen.read()&0x0f)^(color<<4));
		} else {
		    screen.write((screen.read()&0xf0)^color);
		}
		blitter.memory_accesses++;
		break;
	    case 0x16:
		if ((x&1)==0) {
		    if (color!=0xe) {
			back=colbuf.read();
			if (back>>4>blitter.mem.read(blitter.colpos))
			    blitter.mem.write(blitter.colpos, back>>4);
			colbuf.write((back&~0xf0)|(blitter.spritenr<<4));
			blitter.memory_accesses+=2;
		    }
		    screen.write((screen.read()&0x0f)^(color<<4));
		} else {
		    if (color!=0xe) {
			back=colbuf.read();
			if ((back&0xf)>blitter.mem.read(blitter.colpos))
			    blitter.mem.write(blitter.colpos, back&0xf);
			colbuf.write((back&~0xf)|(blitter.spritenr));
			blitter.memory_accesses+=2;
		    }
		    screen.write((screen.read()&0xf0)^color);
		}
		blitter.memory_accesses++;
		break;
	    }
	}
	
	public static int bits=0;
	public static int mask;
        
        static WriteHandlerPtr lynx_blit_2color_line = new WriteHandlerPtr() {
            public void handler(int y, int xdir) {
                bits=1; 
		mask=0x1;
                
                INCLUDE_LYNX_LINE_FUNCTION(y, xdir);
            }
        };

        static WriteHandlerPtr lynx_blit_4color_line = new WriteHandlerPtr() {
            public void handler(int y, int xdir) {
		bits=2; 
		mask=0x3;
                
                INCLUDE_LYNX_LINE_FUNCTION(y, xdir);
            }
        };
	static WriteHandlerPtr lynx_blit_8color_line = new WriteHandlerPtr() {
            public void handler(int y, int xdir) {
		bits=3; 
		mask=0x7;
                
                INCLUDE_LYNX_LINE_FUNCTION(y, xdir);
            }
        };
	static WriteHandlerPtr lynx_blit_16color_line = new WriteHandlerPtr() {
            public void handler(int y, int xdir) {
		bits=4; 
		mask=0xf;
                
                INCLUDE_LYNX_LINE_FUNCTION(y, xdir);
            }
        };
	
	/*
	2 color rle: ??
	 0, 4 bit repeat count-1, 1 bit color
	 1, 4 bit count of values-1, 1 bit color, ....
	*/
	
	/*
	4 color rle:
	 0, 4 bit repeat count-1, 2 bit color
	 1, 4 bit count of values-1, 2 bit color, ....
	*/
	
	/*
	8 color rle:
	 0, 4 bit repeat count-1, 3 bit color
	 1, 4 bit count of values-1, 3 bit color, ....
	*/
		
	/*
	16 color rle:
	 0, 4 bit repeat count-1, 4 bit color
	 1, 4 bit count of values-1, 4 bit color, ....
	*/
	
	static WriteHandlerPtr lynx_blit_2color_rle_line = new WriteHandlerPtr() {
            public void handler(int y, int xdir) {
		bits=1; 
		mask=0x1;
                
                INCLUDE_LYNX_LINE_RLE_FUNCTION(y, xdir);
            }
        };
	static WriteHandlerPtr lynx_blit_4color_rle_line = new WriteHandlerPtr() {
            public void handler(int y, int xdir) {
		bits=2; 
		mask=0x3;
                
                INCLUDE_LYNX_LINE_RLE_FUNCTION(y, xdir);
            }
        };
	static WriteHandlerPtr lynx_blit_8color_rle_line = new WriteHandlerPtr() {
            public void handler(int y, int xdir) {
		bits=3; 
		mask=0x7;
                
                INCLUDE_LYNX_LINE_RLE_FUNCTION(y, xdir);
            }
        };
	static WriteHandlerPtr lynx_blit_16color_rle_line = new WriteHandlerPtr() {
            public void handler(int y, int xdir) {
		int bits=4; 
		int mask=0xf;
                
                INCLUDE_LYNX_LINE_RLE_FUNCTION(y, xdir);
            }
        };
	
	static void lynx_blit_lines()
	{
		int i=0, hi=0, y=0;
		int ydir=0, xdir=0;
		int flip=blitter.mem.read(blitter.cmd+1)&3;
	
		blitter.everon=false;
	
		// flipping sprdemo3
		// fat bobby 0x10
		// mirror the sprite in gameplay?
		xdir=1;
		if ((blitter.mem.read(blitter.cmd)&0x20)!=0) { xdir=-1;blitter.x--;/*?*/ }
		ydir=1;
		if ((blitter.mem.read(blitter.cmd)&0x10)!=0) { ydir=-1;blitter.y--;/*?*/ }
		switch (blitter.mem.read(blitter.cmd+1)&3) {
		case 0: 
			flip =0;
			break;
		case 1: // blockout
			xdir*=-1;
			flip=1;
			break;
		case 2: // fat bobby horicontal
			ydir*=-1;
			flip=1;
			break;
		case 3:
			xdir*=-1;
			ydir*=-1;
			flip=3;
			break;
		}
                
                boolean _end = false;
	
                y=blitter.y; hi=0;
                
                for (int _x=0 ; _x<256 ; _x++) {
                    /*TODO*///for ( y=blitter.y, hi=0;(blitter.memory_accesses++,i=blitter.mem.blitter.bitmap]); blitter.bitmap+=i ) {
                         blitter.memory_accesses++; i=blitter.mem.read(blitter.bitmap); blitter.bitmap+=i;
                                
			if (i==1) {
			    // centered sprites sprdemo3, fat bobby, blockout
			    hi=0;
			    switch (flip&3) {
			    case 0:
			    case 2:
				ydir*=-1;
				blitter.y+=ydir;
				break;
			    case 1:
			    case 3:
				xdir*=-1;
				blitter.x+=xdir;
				break;
			    }
			    y=blitter.y;
			    flip++;
			    continue;
                            //_end = true;
			}
			for (;(hi<blitter.height); hi+=0x100, y+=ydir) {
			    if ( y>=0 && y<102 )
				blitter.line_function.handler(y,xdir);
			    blitter.width+=blitter.stretch;
			    blitter.x+=blitter.tilt;
			}
			hi-=blitter.height;
                    }
                
		switch (blitter.mode) {
		case 0x12: case 0x13: case 0x14: case 0x16: case 0x17:
		    if ((suzy.u.s.SPRG0&0x20)!=0 && blitter.everon) {
			blitter.mem.write(blitter.colpos, blitter.mem.read(blitter.colpos)|0x80);
		    }
		}
	}
	
	public static timer_callback lynx_blitter_timer = new timer_callback() {
            public void handler(int i) {
                suzy.u.s.SPRSYS&=~1; //blitter finished
            }
        };
	
	
	/*
	  control 0
	   bit 7,6: 00 2 color
	            01 4 color
	            11 8 colors?
	            11 16 color
	   bit 5,4: 00 right down
	            01 right up
	            10 left down
	            11 left up
	
	#define SHADOW         (0x07)
	#define XORSHADOW      (0x06)
	#define NONCOLLIDABLE  (0x05)
	#define NORMAL         (0x04)
	#define BOUNDARY       (0x03)
	#define BOUNDARYSHADOW (0x02)
	#define BKGRNDNOCOL    (0x01)
	#define BKGRND         (0x00)
	
	  control 1
	   bit 7: 0 bitmap rle encoded
	          1 not encoded
	   bit 3: 0 color info with command
	          1 no color info with command
	
	#define RELHVST        (0x30)
	#define RELHVS         (0x20)
	#define RELHV          (0x10)
	
	#define SKIPSPRITE     (0x04)
	
	#define DUP            (0x02)
	#define DDOWN          (0x00)
	#define DLEFT          (0x01)
	#define DRIGHT         (0x00)
	
	
	  coll
	#define DONTCOLLIDE    (0x20)
	
	  word next
	  word data
	  word x
	  word y
	  word width
	  word height
	
	  pixel c0 90 20 0000 datapointer x y 0100 0100 color (8 colorbytes)
	  4 bit direct?
	  datapointer 2 10 0
	  98 (0 colorbytes)
	
	  box c0 90 20 0000 datapointer x y width height color
	  datapointer 2 10 0
	
	  c1 98 00 4 bit direct without color bytes (raycast)
	
	  40 10 20 4 bit rle (sprdemo2)
	
	  line c1 b0 20 0000 datapointer x y 0100 0100 stretch tilt:x/y color (8 color bytes)
	  or
	  line c0 b0 20 0000 datapointer x y 0100 0100 stretch tilt:x/y color
	  datapointer 2 11 0
	
	  text ?(04) 90 20 0000 datapointer x y width height color
	  datapointer 2 10 0
	
	  stretch: hsize adder
	  tilt: hpos adder
	
	*/
        
        static int lynx_colors[]={2,4,8,16};
        
        static WriteHandlerPtr[] blit_line = {
            lynx_blit_2color_line,
            lynx_blit_4color_line,
            lynx_blit_8color_line,
            lynx_blit_16color_line
        };

        static WriteHandlerPtr[] blit_rle_line = {
            lynx_blit_2color_rle_line,
            lynx_blit_4color_rle_line,
            lynx_blit_8color_rle_line,
            lynx_blit_16color_rle_line
        };
	
	static void lynx_blitter()
	{
	    
	    int i; int o;int colors;
	    
	    blitter.memory_accesses=0;
	    blitter.mem=new UBytePtr(memory_region(REGION_CPU1));
	    blitter.colbuf=GET_WORD(suzy.u.data, 0xa);
	    blitter.screen=GET_WORD(suzy.u.data, 8);
	    blitter.xoff=GET_WORD(suzy.u.data,4);
	    blitter.yoff=GET_WORD(suzy.u.data,6);
	    // hsizeoff GET_WORD(suzy.u.data, 0x28)
	    // vsizeoff GET_WORD(suzy.u.data, 0x2a)
	
	    // these might be never set by the blitter hardware
	    blitter.width=0x100;
	    blitter.height=0x100;
	    blitter.stretch=0;
	    blitter.tilt=0;
	    
	    blitter.memory_accesses+=2;
	    for (blitter.cmd=GET_WORD(suzy.u.data, 0x10); blitter.cmd!=0; ) {
		
		blitter.memory_accesses+=1;
		if ((blitter.mem.read(blitter.cmd+1)&4)==0) {
		
		    blitter.colpos=GET_WORD(suzy.u.data, 0x24)+blitter.cmd;
		
		    blitter.bitmap=GET_WORD(blitter.mem,blitter.cmd+5);
		    blitter.x=GET_WORD(blitter.mem, blitter.cmd+7)-blitter.xoff;
		    blitter.y=GET_WORD(blitter.mem, blitter.cmd+9)-blitter.yoff;
		    blitter.memory_accesses+=6;
		    
		    blitter.mode=blitter.mem.read(blitter.cmd)&07;
		    if ((blitter.mem.read(blitter.cmd+1)&0x80) != 0) {
			blitter.line_function=blit_line[blitter.mem.read(blitter.cmd)>>6];
		    } else {
			blitter.line_function=blit_rle_line[blitter.mem.read(blitter.cmd)>>6];
		    }
		    
		    if ((blitter.mem.read(blitter.cmd+2)&0x20)==0 && ( suzy.u.s.SPRSYS&0x20)==0 ) {
			switch (blitter.mode) {
			case 0: case 2: case 3: case 4: case 6: case 7:
			    blitter.mode|=0x10;
			    blitter.mem.write(blitter.colpos, 0);
			    blitter.spritenr=blitter.mem.read(blitter.cmd+2)&0xf;
			}
		    }
		    
		    o=0xb;
		    if ((blitter.mem.read(blitter.cmd+1)&0x30) != 0) {
			blitter.width=GET_WORD(blitter.mem, blitter.cmd+11);
			blitter.height=GET_WORD(blitter.mem, blitter.cmd+13);
			blitter.memory_accesses+=4;
			o+=4;
		    }
		    
		    if ((blitter.mem.read(blitter.cmd+1)&0x20) != 0) {
			blitter.stretch=GET_WORD(blitter.mem, blitter.cmd+o);
			blitter.memory_accesses+=2;
			o+=2;
			if ((blitter.mem.read(blitter.cmd+1)&0x10) != 0) {
			    blitter.tilt=GET_WORD(blitter.mem, blitter.cmd+o);
			    blitter.memory_accesses+=2;
			    o+=2;
			}
		    }
		    colors=lynx_colors[blitter.mem.read(blitter.cmd)>>6];
		    
		    if ((blitter.mem.read(blitter.cmd+1)&8) == 0) {
			for (i=0; i<colors/2; i++) {
			    blitter.color[i*2]=blitter.mem.read(blitter.cmd+o+i)>>4;
			    blitter.color[i*2+1]=blitter.mem.read(blitter.cmd+o+i)&0xf;
			    blitter.memory_accesses++;
			}
		    }
		    
	/*TODO*///#if 0
	/*TODO*///	    if (debug_pos<ARRAY_LENGTH(debug_strings)) {
	/*TODO*///		snprintf(debug_strings[debug_pos],sizeof(debug_strings[0]),
	/*TODO*///			 "%.2x%.2x%.2x %.4x %.4x %.4x %.4x",
	/*TODO*///			 blitter.mem[blitter.cmd],
	/*TODO*///			 blitter.mem[blitter.cmd+1],
	/*TODO*///			 blitter.mem[blitter.cmd+2],
	/*TODO*///			 blitter.x, blitter.y,
	/*TODO*///			 blitter.width, blitter.height
	/*TODO*///		    );
	/*TODO*///		debug_pos++;
	/*TODO*///	    }
	/*TODO*///#endif
		    
	/*TODO*///#if 0
	/*TODO*///	    logerror("%04x %.2x %.2x %.2x x:%.4x y:%.4x",
	/*TODO*///		     blitter.cmd,
	/*TODO*///		     blitter.mem[blitter.cmd],blitter.mem[blitter.cmd+1],blitter.mem[blitter.cmd+2],
	/*TODO*///		     blitter.x,blitter.y);
	/*TODO*///	    if (blitter.mem[blitter.cmd+1]&0x30) {
	/*TODO*///		logerror(" w:%.4x h:%.4x", blitter.width,blitter.height);
	/*TODO*///	    }
	/*TODO*///	    if (blitter.mem[blitter.cmd+1]&0x20) {
	/*TODO*///		logerror(" s:%.4x t:%.4x", blitter.stretch, blitter.tilt);
	/*TODO*///	    }
	/*TODO*///	    if (!(blitter.mem[blitter.cmd+1]&0x8)) {
	/*TODO*///		logerror(" c:");
	/*TODO*///		for (i=0; i<colors/2; i++) {
	/*TODO*///		    logerror("%.2x", blitter.mem[blitter.cmd+o+i]);
	/*TODO*///		}
	/*TODO*///	    }
	/*TODO*///	    logerror(" %.4x\n", blitter.bitmap);
	/*TODO*///#endif
		    lynx_blit_lines();
		}
		blitter.cmd=GET_WORD(blitter.mem, blitter.cmd+3);
		blitter.memory_accesses+=2;
		if ((blitter.cmd&0xff00)==0) break;
	    }
	    timer_set(TIME_IN_CYCLES(blitter.memory_accesses*20,0), 0, lynx_blitter_timer);
	}
	
	public static void lynx_divide()
	{
		int left=suzy.u.s.H|(suzy.u.s.G<<8)|(suzy.u.s.F<<16)|(suzy.u.s.E<<24);
		int right=suzy.u.s.P|(suzy.u.s.N<<8);
		int res, mod;
		suzy.accumulate_overflow=false;
		if (right==0) {
		    suzy.accumulate_overflow=true;
		    res=0xffffffff;
		    mod=0; //?
		} else {
		    res=left/right;
		    mod=left%right;
		}
	//	logerror("coprocessor %8x / %8x = %4x\n", left, right, res);
		suzy.u.s.D=res&0xff;
		suzy.u.s.C=res>>8;
		suzy.u.s.B=res>>16;
		suzy.u.s.A=res>>24;
		suzy.u.s.M=mod&0xff;
		suzy.u.s.L=mod>>8;
		suzy.u.s.K=mod>>16;
		suzy.u.s.J=mod>>24;
	}
	
	public static void lynx_multiply()
	{
		int left, right;
		int res, accu;
		left=suzy.u.s.B|(suzy.u.s.A<<8);
		right=suzy.u.s.D|(suzy.u.s.C<<8);
		if ((suzy.u.s.SPRSYS&0x80) != 0) {
		    // to do
		    res=left*right;
		} else res=left*right;
	//	logerror("coprocessor %4x * %4x = %4x\n", left, right, res);
		suzy.u.s.H=res&0xff;
		suzy.u.s.G=res>>8;
		suzy.u.s.F=res>>16;
		suzy.u.s.E=res>>24;
		if ((suzy.u.s.SPRSYS&0x40) !=0) {
		    accu=suzy.u.s.M|suzy.u.s.L<<8|suzy.u.s.K<<16|suzy.u.s.J<<24;
		    accu+=res;
		    if (accu<res) suzy.accumulate_overflow=true;
		    suzy.u.s.M=accu;
		    suzy.u.s.L=accu>>8;
		    suzy.u.s.K=accu>>16;
		    suzy.u.s.J=accu>>24;
		}
	}
	
	public static ReadHandlerPtr suzy_read = new ReadHandlerPtr() {
            public int handler(int offset) {
                int data=0, input;
		switch (offset) {
		case 0x88:
			data=1; // must not be 0 for correct power up
			break;
		case 0x92:
			if (blitter.time!=0.0) {
			    if ( TIME_TO_CYCLES(0,timer_get_time()-blitter.time) > blitter.memory_accesses*20) {
				suzy.u.data.write(offset, suzy.u.data.read(offset)&~1); //blitter finished
				blitter.time=0;
			    }
			}
			data=suzy.u.data.read(offset);
			data&=~0x80; // math finished
			data&=~0x40;
			if (suzy.accumulate_overflow) data|=0x40;
			break;
		case 0xb0:
			input=readinputport(0);
			switch (lynx_rotate) {
			case 1:
				data=input;
				input&=0xf;
				if ((data&PAD_UP)       != 0) input|=PAD_LEFT;
				if ((data&PAD_LEFT)     != 0) input|=PAD_DOWN;
				if ((data&PAD_DOWN)     != 0) input|=PAD_RIGHT;
				if ((data&PAD_RIGHT)    != 0) input|=PAD_UP;
				break;
			case 2:
				data=input;
				input&=0xf;
				if ((data&PAD_UP)       != 0) input|=PAD_RIGHT;
				if ((data&PAD_RIGHT)    != 0) input|=PAD_DOWN;
				if ((data&PAD_DOWN)     != 0) input|=PAD_LEFT;
				if ((data&PAD_LEFT)     != 0) input|=PAD_UP;
				break;
			}
			if ((suzy.u.s.SPRSYS&8) != 0) {
				data=input&0xf;
				if ((input&PAD_UP)      != 0) data|=PAD_DOWN;
				if ((input&PAD_DOWN)    != 0) data|=PAD_UP;
				if ((input&PAD_LEFT)    != 0) data|=PAD_RIGHT;
				if ((input&PAD_RIGHT)   != 0) data|=PAD_LEFT;
			} else {
				data=input;
			}		
			break;
		case 0xb1: data=readinputport(1);break;
		case 0xb2:
			data=(new UBytePtr(memory_region(REGION_USER1), (suzy.high*lynx_granularity)+suzy.low)).read();
			suzy.low=(suzy.low+1)&(lynx_granularity-1);
			break;
		default:
			data=suzy.u.data.read(offset);
		}
	//	logerror("suzy read %.2x %.2x\n",offset,data);
		return data;
            }
        };
	
	public static WriteHandlerPtr suzy_write = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                suzy.u.data.write(offset, data);
		switch(offset) {
		case 0x52: case 0x54:
		case 0x60: case 0x62:
		case 0x6e:
		    suzy.u.data.write(offset+1, 0);
		    break;
		case 0x6c:
		    suzy.u.data.write(offset+1, 0);
		    suzy.accumulate_overflow=false;
		    break;
		case 0x55: lynx_multiply();break;
		case 0x63: lynx_divide();break;
		case 0x91:
		    if ((data&1) != 0) {
			blitter.time=timer_get_time();
			lynx_blitter();
		    }
	//	    logerror("suzy write %.2x %.2x\n",offset,data);
		    break;
	//	default:
	//	    logerror("suzy write %.2x %.2x\n",offset,data);
		}
            }
        };
	
	/*
	 0xfd0a r sync signal?
	 0xfd81 r interrupt source bit 2 vertical refresh
	 0xfd80 w interrupt quit
	 0xfd87 w bit 1 !clr bit 0 blocknumber clk
	 0xfd8b w bit 1 blocknumber hi B
	 0xfd94 w 0
	 0xfd95 w 4
	 0xfda0-f rw farben 0..15
	 0xfdb0-f rw bit0..3 farben 0..15
	*/
	public static MIKEY mikey=new MIKEY();
	
	
	/*
	HCOUNTER        EQU TIMER0
	VCOUNTER        EQU TIMER2
	SERIALRATE      EQU TIMER4
	
	TIM_BAKUP       EQU 0   ; backup-value (count+1)
	TIM_CNTRL1      EQU 1   ; timer-control register
	TIM_CNT EQU 2   ; current counter
	TIM_CNTRL2      EQU 3   ; dynamic control
	
	; TIM_CNTRL1
	TIM_IRQ EQU %10000000   ; enable interrupt (not TIMER4 !)
	TIM_RESETDONE   EQU %01000000   ; reset timer done
	TIM_MAGMODE     EQU %00100000   ; nonsense in Lynx !!
	TIM_RELOAD      EQU %00010000   ; enable reload
	TIM_COUNT       EQU %00001000   ; enable counter
	TIM_LINK        EQU %00000111   
	; link timers (0.2.4 / 1.3.5.7.Aud0.Aud1.Aud2.Aud3.1
	TIM_64us        EQU %00000110
	TIM_32us        EQU %00000101
	TIM_16us        EQU %00000100
	TIM_8us EQU %00000011
	TIM_4us EQU %00000010
	TIM_2us EQU %00000001
	TIM_1us EQU %00000000
	
	;TIM_CNTRL2 (read-only)
	; B7..B4 unused
	TIM_DONE        EQU %00001000   ; set if timer's done; reset with TIM_RESETDONE
	TIM_LAST        EQU %00000100   ; last clock (??)
	TIM_BORROWIN    EQU %00000010
	TIM_BORROWOUT   EQU %00000001
	*/
        public static class _s2 {
            public int bakup;
            public int cntrl1;
            public int cnt;
            public int cntrl2;		
        }
        
        public static class _u2 {
           public int[] data = new int[4];
           public _s2 s = new _s2();
        }
        
	public static class LYNX_TIMER{
	    public int nr;
	    public _u2 u = new _u2();
	    public boolean shot = false;
	    public int counter;
	    public timer_entry timer=null;
	    public double settime;

            private LYNX_TIMER(int nr) {
                this.nr = nr;
            }
	};
        
	static LYNX_TIMER lynx_timer[] = {
	    new LYNX_TIMER( 0 ),
	    new LYNX_TIMER( 1 ),
	    new LYNX_TIMER( 2 ),
	    new LYNX_TIMER( 3 ),
	    new LYNX_TIMER( 4 ),
	    new LYNX_TIMER( 5 ),
	    new LYNX_TIMER( 6 ),
	    new LYNX_TIMER( 7 )
	};
	
	static void lynx_timer_reset(LYNX_TIMER This)
	{
	    if (This.timer != null) timer_remove(This.timer);
	    /*TODO*///memset((char*)This+sizeof(This.nr), 0, sizeof(*This)-sizeof(This.nr));
	    This.settime=0.0;
	}
	
	static void lynx_timer_signal_irq(LYNX_TIMER This)
	{
	    if ( (This.u.s.cntrl1&0x80)!=0 && (This.nr!=4) ) { // irq flag handling later
		mikey.data[0x81]|=1<<This.nr;
		cpu_set_irq_line(0, M65SC02_IRQ_LINE, ASSERT_LINE);
	    }
	    switch (This.nr) {
	    case 0: lynx_timer_count_down(2); lynx_line++; break;
	    case 2: 
		lynx_timer_count_down(4); 
		lynx_draw_lines(-1);
		lynx_line=0;
		break;
	    case 1: lynx_timer_count_down(3); break;
	    case 3: lynx_timer_count_down(5); break;
	    case 5: lynx_timer_count_down(7); break;
	    case 7: 
                /*TODO*///lynx_audio_count_down(0); 
                break;
	    }
	}
	
	public static void lynx_timer_count_down(int nr)
	{
	    LYNX_TIMER This=lynx_timer[nr];
	    if ((This.u.s.cntrl1&0xf)==0xf) {
		if (This.counter>0) {
		    This.counter--;
		    return;
		} else if (This.counter==0) {
		    This.shot=true;
		    lynx_timer_signal_irq(This);
		    if ((This.u.s.cntrl1&0x10) != 0) {
			This.counter=This.u.s.bakup;
		    } else {
			This.counter--;
		    }
		    return;
		}
	    }
	}
	
	static timer_callback lynx_timer_shot = new timer_callback() {
            public void handler(int nr) {
                LYNX_TIMER This=lynx_timer[nr];
	    This.shot=true;
	    lynx_timer_signal_irq(This);
	    if ((This.u.s.cntrl1&0x10)==0) This.timer=null;
            }
        };
	
	static double times[]= { 1e-6, 2e-6, 4e-6, 8e-6, 16e-6, 32e-6, 64e-6 };
	
	static int lynx_timer_read(LYNX_TIMER This, int offset)
	{
	    int data=0;
	    switch (offset) {
	    case 2:
		if ((This.u.s.cntrl1&7)==7) {
		    data=This.counter;
		} else {
		    if (This.timer != null) {
			data=(int) (This.u.s.bakup-timer_timeleft(This.timer)/times[This.u.s.cntrl1&7]);
		    }
		}
		break;
	    case 3:
		data=This.u.data[offset];
		data&=~8;
		if (This.shot) data|=8;
		break;
	    default:
		data=This.u.data[offset];
	    }
	    /*TODO*///logerror("timer %d read %x %.2x\n",This-lynx_timer,offset,data);
	    return data;
	}
	
	static void lynx_timer_write(LYNX_TIMER This, int offset, int data)
	{
	    int t;
	    /*TODO*///logerror("timer %d write %x %.2x\n",This-lynx_timer,offset,data);
	    This.u.data[offset]=data;
	
	    if ((offset==1) && (data&0x40)!=0) This.shot=false;
	    
	    switch (offset) {
	    case 0:
	//	This.counter=This.u.s.bakup+1;
	//	break;
	    case 2:
	//	This.counter=data;
	//	break;
	    case 1:
		if (This.timer!=null) { timer_remove(This.timer); This.timer=null; }
		if ((This.u.s.cntrl1&0x8) != 0) {
		    if ((This.u.s.cntrl1&7)!=7) {
			t=This.u.s.bakup+1;
			if ((This.u.s.cntrl1&0x10) != 0) {
			    This.timer=timer_pulse(t*times[This.u.s.cntrl1&7],
						    This.nr, lynx_timer_shot);
			} else {
			    This.timer=timer_set(t*times[This.u.s.cntrl1&7],
						  This.nr, lynx_timer_shot);
			}
		    }
		}
		break;
	    }
	}
	
	public static class _uart {
	    public int serctl;
	    public int data_received, data_to_send, buffer;
	
	    public boolean received = false;
	    public boolean  sending = false;;
	    public boolean  buffer_loaded = false;;
	};
        
        public static _uart uart = new _uart();
	
	public static void lynx_uart_reset()
	{
	    uart = new _uart();
	}
	
	static timer_callback lynx_uart_timer = new timer_callback() {
            public void handler(int param) {
                if (uart.buffer_loaded) {
                    uart.data_to_send=uart.buffer;
                    uart.buffer_loaded=false;
                    timer_set(1.0e-6*11, 0, lynx_uart_timer);	
                } else {
                    uart.sending=false;
                }
            //    mikey.data[0x80]|=0x10;
                if ((uart.serctl&0x80) != 0) {
                    mikey.data[0x81]|=0x10;
                    cpu_set_irq_line(0, M65SC02_IRQ_LINE, ASSERT_LINE);
                }
            }
        };
		
	public static ReadHandlerPtr lynx_uart_r = new ReadHandlerPtr() {
            public int handler(int offset) {
                int data=0;
                switch (offset) {
                case 0x8c:
                    if (!uart.buffer_loaded) data|=0x80;
                    if (uart.received) data|=0x40;
                    if (!uart.sending) data|=0x20;
                    break;
                case 0x8d:
                    data=uart.data_received;
                    break;
                }
                logerror("uart read %.2x %.2x\n",offset,data);
                return data;
            }
        };
	
	public static WriteHandlerPtr lynx_uart_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                logerror("uart write %.2x %.2x\n",offset,data);
                switch (offset) {
                    case 0x8c:
                        uart.serctl=data;
                        break;
                    case 0x8d:
                        if (uart.sending) {
                            uart.buffer=data;
                            uart.buffer_loaded=true;
                        } else {
                            uart.sending=true;
                            uart.data_to_send=data;
                            timer_set(1.0e-6*11, 0, lynx_uart_timer);
                        }
                        break;
                    }
                }
        };
	
	public static ReadHandlerPtr mikey_read = new ReadHandlerPtr() {
            public int handler(int offset) {
                int data=0;
                switch (offset) {
                case 0: case 1: case 2: case 3:
                case 4: case 5: case 6: case 7:
                case 8: case 9: case 0xa: case 0xb:
                case 0xc: case 0xd: case 0xe: case 0xf:
                case 0x10: case 0x11: case 0x12: case 0x13:
                case 0x14: case 0x15: case 0x16: case 0x17:
                case 0x18: case 0x19: case 0x1a: case 0x1b:
                case 0x1c: case 0x1d: case 0x1e: case 0x1f:
                    data=lynx_timer_read(lynx_timer[(offset/4)], offset&3);
                    return data;
                case 0x20: case 0x21: case 0x22: case 0x23: case 0x24: case 0x25: case 0x26: case 0x27:
                case 0x28: case 0x29: case 0x2a: case 0x2b: case 0x2c: case 0x2d: case 0x2e: case 0x2f:
                case 0x30: case 0x31: case 0x32: case 0x33: case 0x34: case 0x35: case 0x36: case 0x37:
                case 0x38: case 0x39: case 0x3a: case 0x3b: case 0x3c: case 0x3d: case 0x3e: case 0x3f:
                case 0x40: case 0x41: case 0x42: case 0x43: case 0x44: case 0x50:
                    /*TODO*///data=lynx_audio_read(offset);
                    return data;
                case 0x81:
                    data=mikey.data[offset];
                    logerror("mikey read %.2x %.2x\n",offset,data);
                    break;
                case 0x8b:
                    data=mikey.data[offset];
                    data|=4; // no comlynx adapter
                    break;
                case 0x8c: case 0x8d:
                    data=lynx_uart_r.handler(offset);
                    break;
                default:
                    data=mikey.data[offset];
                    logerror("mikey read %.2x %.2x\n",offset,data);
                }
                return data;
            }
        };
	
	public static WriteHandlerPtr mikey_write = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                switch (offset) {
                case 0: case 1: case 2: case 3:
                case 4: case 5: case 6: case 7:
                case 8: case 9: case 0xa: case 0xb:
                case 0xc: case 0xd: case 0xe: case 0xf:
                case 0x10: case 0x11: case 0x12: case 0x13:
                case 0x14: case 0x15: case 0x16: case 0x17:
                case 0x18: case 0x19: case 0x1a: case 0x1b:
                case 0x1c: case 0x1d: case 0x1e: case 0x1f:
                    lynx_timer_write(lynx_timer[(offset/4)], offset&3, data);
                    return;
                case 0x20: case 0x21: case 0x22: case 0x23: case 0x24: case 0x25: case 0x26: case 0x27:
                case 0x28: case 0x29: case 0x2a: case 0x2b: case 0x2c: case 0x2d: case 0x2e: case 0x2f:
                case 0x30: case 0x31: case 0x32: case 0x33: case 0x34: case 0x35: case 0x36: case 0x37:
                case 0x38: case 0x39: case 0x3a: case 0x3b: case 0x3c: case 0x3d: case 0x3e: case 0x3f:
                case 0x40: case 0x41: case 0x42: case 0x43: case 0x44: case 0x50:
                    /*TODO*///lynx_audio_write(offset, data);
                    return;
                case 0x80:
                    mikey.data[0x81]&=~data; // clear interrupt source
                    logerror("mikey write %.2x %.2x\n",offset,data);
                    if (mikey.data[0x81]==0) {
                            cpu_set_irq_line(0, M65SC02_IRQ_LINE, CLEAR_LINE);	    
                    }
                    break;
                case 0x87:
                    mikey.data[offset]=data; //?
                    if ((data&2)!=0) {
                        if ((data&1)!=0) {
                            suzy.high<<=1;
                            if ((mikey.data[0x8b]&2)!=0) suzy.high|=1;
                            suzy.low=0;
                        }
                    } else {
                        suzy.high=0;
                        suzy.low=0;
                    }
                    break;
                case 0x8c: case 0x8d:
                    lynx_uart_w.handler(offset, data);
                    break;
                case 0xa0: case 0xa1: case 0xa2: case 0xa3: case 0xa4: case 0xa5: case 0xa6: case 0xa7:
                case 0xa8: case 0xa9: case 0xaa: case 0xab: case 0xac: case 0xad: case 0xae: case 0xaf:
                case 0xb0: case 0xb1: case 0xb2: case 0xb3: case 0xb4: case 0xb5: case 0xb6: case 0xb7:
                case 0xb8: case 0xb9: case 0xba: case 0xbb: case 0xbc: case 0xbd: case 0xbe: case 0xbf:
                    mikey.data[offset]=data;
                    lynx_draw_lines(lynx_line);
            /*TODO*///#if 0
            /*TODO*///        palette_change_color(offset&0xf,
            /*TODO*///                             (mikey.data[0xb0+(offset&0xf)]&0xf)<<4,
            /*TODO*///                             (mikey.data[0xa0+(offset&0xf)]&0xf)<<4,
            /*TODO*///                             mikey.data[0xb0+(offset&0xf)]&0xf0 );
            /*TODO*///#else
                    lynx_palette[offset&0xf]=Machine.pens[((mikey.data[0xb0+(offset&0xf)]&0xf))
                        |((mikey.data[0xa0+(offset&0xf)]&0xf)<<4)
                        |((mikey.data[0xb0+(offset&0xf)]&0xf0)<<4)];
            /*TODO*///#endif
                    break;
                case 0x8b:case 0x90:case 0x91:
                    mikey.data[offset]=data;
                    break;
                default:
                    mikey.data[offset]=data;
                    logerror("mikey write %.2x %.2x\n",offset,data);
                }
            }
        };
	
	public static WriteHandlerPtr lynx_memory_config = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	    // bit 7: hispeed, uses page mode accesses (4 instead of 5 cycles )
	    // when these are safe in the cpu
	    new UBytePtr(memory_region(REGION_CPU1), 0xfff9).write(data);
	    if ((data&1) != 0) {
		memory_set_bankhandler_r(1, 0, MRA_RAM);
		memory_set_bankhandler_w(1, 0, MWA_RAM);
	    } else {
		memory_set_bankhandler_r(1, 0, suzy_read);
		memory_set_bankhandler_w(1, 0, suzy_write);
	    }
	    if ((data&2)!=0) {
		memory_set_bankhandler_r(2, 0, MRA_RAM);
		memory_set_bankhandler_w(2, 0, MWA_RAM);
	    } else {
		memory_set_bankhandler_r(2, 0, mikey_read);
		memory_set_bankhandler_w(2, 0, mikey_write);
	    }
	    if ((data&4)!=0) {
		memory_set_bankhandler_r(3, 0, MRA_RAM);
	    } else {
		cpu_setbank(3, new UBytePtr(memory_region(REGION_CPU1), 0x10000));
		memory_set_bankhandler_r(3, 0, MRA_BANK3);
	    }
	    if ((data&8)!=0) {
		memory_set_bankhandler_r(4, 0, MRA_RAM);
	    } else {
		memory_set_bankhandler_r(4, 0, MRA_BANK4);
		cpu_setbank(4, new UBytePtr(memory_region(REGION_CPU1), 0x101fa));
	    }
	} };
	
	public static InitMachinePtr lynx_machine_init = new InitMachinePtr() {
            public void handler() {
                int i;
	    lynx_memory_config.handler(0,0);
	    
	    cpu_set_irq_line(0, M65SC02_IRQ_LINE, CLEAR_LINE);	    
	    
	    //memset(&suzy, 0, sizeof(suzy));
            suzy = new SUZY();
	    //memset(&mikey, 0, sizeof(mikey));
            mikey = new MIKEY();
	    
	    mikey.data[0x80]=0;
	    mikey.data[0x81]=0;
	
	    lynx_uart_reset();
	
	    for (i=0; i<lynx_timer.length; i++) {
		lynx_timer_reset(lynx_timer[i]);
	    }
	    /*TODO*///lynx_audio_reset();
	
	    // hack to allow current object loading to work
	//#if 1
	    lynx_timer_write(lynx_timer[0], 0, 160);
	    lynx_timer_write(lynx_timer[0], 1, 0x10|0x8|0);
	    lynx_timer_write(lynx_timer[0+2], 0, 102);
	    lynx_timer_write(lynx_timer[0+2], 1, 0x10|0x8|7);
	//#endif
            }
        };
	
}
