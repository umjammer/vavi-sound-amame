/*********************************************************/
/*    Konami PCM controller                              */
/*********************************************************/

/*

	Changelog, Mish, August 1999:
		Removed interface support for different memory regions per channel.
		Removed interface support for differing channel volume.

		Added bankswitching.
		Added support for multiple chips.

		(Nb:  Should different memory regions per channel be needed
		the bankswitching function can set this up).

NS990821
support for the K007232_VOL() macro.
added external port callback, and functions to set the volume of the channels

*/


/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mame056.sound;

import static common.ptr.*;
import static common.subArrays.*;
import static arcadeflex056.fucPtr.*;
import static common.libc.cstdio.*;
import static mame056.common.memory_region;
import static mame056.mame.Machine;
import static mame056.sndintrf.*;
import static mame056.sndintrfH.*;
import static mame056.sound.streams.*;
import static mame056.sound.k007232H.*;

public class k007232 extends snd_interface
{
	
	
        public static int  KDAC_A_PCM_MAX    = (2);		/* Channels per chip */

    private static void memset(int i, int i0, int buffer_len) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

        @Override
        public int chips_num(MachineSound msound) {
            return ((K007232_interface) msound.sound_interface).num_chips;
        }

        @Override
        public int chips_clock(MachineSound msound) {
            return 0;//NO functionality expected
        }

        @Override
        public int start(MachineSound msound) {
            return K007232_sh_start(msound);
        }

        @Override
        public void stop() {
            //NO functionality expected
        }

        @Override
        public void update() {
            //NO functionality expected
        }

        @Override
        public void reset() {
            //NO functionality expected
        }
	
	
	public static class KDAC_A_PCM
	{
		public char[][] vol = new char[KDAC_A_PCM_MAX][2];	/* volume for the left and right channel */
		public int[]  addr  = new int[KDAC_A_PCM_MAX];
		public int[]  start = new int[KDAC_A_PCM_MAX];
		public int[]  step  = new int[KDAC_A_PCM_MAX];
		public int[] play   = new int[KDAC_A_PCM_MAX];
		public int[] loop   = new int[KDAC_A_PCM_MAX];
	
		public char[] wreg  = new char[0x10];	/* write data */
		public UBytePtr[] pcmbuf = new UBytePtr[2];	/* Channel A & B pointers */
	
	};
	
	static KDAC_A_PCM[]    kpcm = new KDAC_A_PCM[MAX_K007232];
	
	static int[] pcm_chan = new int[MAX_K007232];
	
	static K007232_interface intf;
	
	public static int BASE_SHIFT    = (12);
	
	
	
/*TODO*///	#if 0
/*TODO*///	static int kdac_note[] = {
/*TODO*///	  261.63/8, 277.18/8,
/*TODO*///	  293.67/8, 311.13/8,
/*TODO*///	  329.63/8,
/*TODO*///	  349.23/8, 369.99/8,
/*TODO*///	  392.00/8, 415.31/8,
/*TODO*///	  440.00/8, 466.16/8,
/*TODO*///	  493.88/8,
/*TODO*///	
/*TODO*///	  523.25/8,
/*TODO*///	};
/*TODO*///	
/*TODO*///	static float kdaca_fn[][2] = {
/*TODO*///	  /* B */
/*TODO*///	  { 0x03f, 493.88/8 },		/* ?? */
/*TODO*///	  { 0x11f, 493.88/4 },		/* ?? */
/*TODO*///	  { 0x18f, 493.88/2 },		/* ?? */
/*TODO*///	  { 0x1c7, 493.88   },
/*TODO*///	  { 0x1e3, 493.88*2 },
/*TODO*///	  { 0x1f1, 493.88*4 },		/* ?? */
/*TODO*///	  { 0x1f8, 493.88*8 },		/* ?? */
/*TODO*///	  /* A+ */
/*TODO*///	  { 0x020, 466.16/8 },		/* ?? */
/*TODO*///	  { 0x110, 466.16/4 },		/* ?? */
/*TODO*///	  { 0x188, 466.16/2 },
/*TODO*///	  { 0x1c4, 466.16   },
/*TODO*///	  { 0x1e2, 466.16*2 },
/*TODO*///	  { 0x1f1, 466.16*4 },		/* ?? */
/*TODO*///	  { 0x1f8, 466.16*8 },		/* ?? */
/*TODO*///	  /* A */
/*TODO*///	  { 0x000, 440.00/8 },		/* ?? */
/*TODO*///	  { 0x100, 440.00/4 },		/* ?? */
/*TODO*///	  { 0x180, 440.00/2 },
/*TODO*///	  { 0x1c0, 440.00   },
/*TODO*///	  { 0x1e0, 440.00*2 },
/*TODO*///	  { 0x1f0, 440.00*4 },		/* ?? */
/*TODO*///	  { 0x1f8, 440.00*8 },		/* ?? */
/*TODO*///	  { 0x1fc, 440.00*16},		/* ?? */
/*TODO*///	  { 0x1fe, 440.00*32},		/* ?? */
/*TODO*///	  { 0x1ff, 440.00*64},		/* ?? */
/*TODO*///	  /* G+ */
/*TODO*///	  { 0x0f2, 415.31/4 },
/*TODO*///	  { 0x179, 415.31/2 },
/*TODO*///	  { 0x1bc, 415.31   },
/*TODO*///	  { 0x1de, 415.31*2 },
/*TODO*///	  { 0x1ef, 415.31*4 },		/* ?? */
/*TODO*///	  { 0x1f7, 415.31*8 },		/* ?? */
/*TODO*///	  /* G */
/*TODO*///	  { 0x0e2, 392.00/4 },
/*TODO*///	  { 0x171, 392.00/2 },
/*TODO*///	  { 0x1b8, 392.00   },
/*TODO*///	  { 0x1dc, 392.00*2 },
/*TODO*///	  { 0x1ee, 392.00*4 },		/* ?? */
/*TODO*///	  { 0x1f7, 392.00*8 },		/* ?? */
/*TODO*///	  /* F+ */
/*TODO*///	  { 0x0d0, 369.99/4 },		/* ?? */
/*TODO*///	  { 0x168, 369.99/2 },
/*TODO*///	  { 0x1b4, 369.99   },
/*TODO*///	  { 0x1da, 369.99*2 },
/*TODO*///	  { 0x1ed, 369.99*4 },		/* ?? */
/*TODO*///	  { 0x1f6, 369.99*8 },		/* ?? */
/*TODO*///	  /* F */
/*TODO*///	  { 0x0bf, 349.23/4 },		/* ?? */
/*TODO*///	  { 0x15f, 349.23/2 },
/*TODO*///	  { 0x1af, 349.23   },
/*TODO*///	  { 0x1d7, 349.23*2 },
/*TODO*///	  { 0x1eb, 349.23*4 },		/* ?? */
/*TODO*///	  { 0x1f5, 349.23*8 },		/* ?? */
/*TODO*///	  /* E */
/*TODO*///	  { 0x0ac, 329.63/4 },
/*TODO*///	  { 0x155, 329.63/2 },		/* ?? */
/*TODO*///	  { 0x1ab, 329.63   },
/*TODO*///	  { 0x1d5, 329.63*2 },
/*TODO*///	  { 0x1ea, 329.63*4 },		/* ?? */
/*TODO*///	  { 0x1f4, 329.63*8 },		/* ?? */
/*TODO*///	  /* D+ */
/*TODO*///	  { 0x098, 311.13/4 },		/* ?? */
/*TODO*///	  { 0x14c, 311.13/2 },
/*TODO*///	  { 0x1a6, 311.13   },
/*TODO*///	  { 0x1d3, 311.13*2 },
/*TODO*///	  { 0x1e9, 311.13*4 },		/* ?? */
/*TODO*///	  { 0x1f4, 311.13*8 },		/* ?? */
/*TODO*///	  /* D */
/*TODO*///	  { 0x080, 293.67/4 },		/* ?? */
/*TODO*///	  { 0x140, 293.67/2 },		/* ?? */
/*TODO*///	  { 0x1a0, 293.67   },
/*TODO*///	  { 0x1d0, 293.67*2 },
/*TODO*///	  { 0x1e8, 293.67*4 },		/* ?? */
/*TODO*///	  { 0x1f4, 293.67*8 },		/* ?? */
/*TODO*///	  { 0x1fa, 293.67*16},		/* ?? */
/*TODO*///	  { 0x1fd, 293.67*32},		/* ?? */
/*TODO*///	  /* C+ */
/*TODO*///	  { 0x06d, 277.18/4 },		/* ?? */
/*TODO*///	  { 0x135, 277.18/2 },		/* ?? */
/*TODO*///	  { 0x19b, 277.18   },
/*TODO*///	  { 0x1cd, 277.18*2 },
/*TODO*///	  { 0x1e6, 277.18*4 },		/* ?? */
/*TODO*///	  { 0x1f2, 277.18*8 },		/* ?? */
/*TODO*///	  /* C */
/*TODO*///	  { 0x054, 261.63/4 },
/*TODO*///	  { 0x12a, 261.63/2 },
/*TODO*///	  { 0x195, 261.63   },
/*TODO*///	  { 0x1ca, 261.63*2 },
/*TODO*///	  { 0x1e5, 261.63*4 },
/*TODO*///	  { 0x1f2, 261.63*8 },		/* ?? */
/*TODO*///	
/*TODO*///	  { -1, -1 },
/*TODO*///	};
/*TODO*///	#endif
	
	static float[] fncode = new float[0x200];
	/*************************************************************/
	static void KDAC_A_make_fncode(){
	  int i;
/*TODO*///	#if 0
/*TODO*///	  int i, j, k;
/*TODO*///	  float fn;
/*TODO*///	  for( i = 0; i < 0x200; i++ )  fncode[i] = 0;
/*TODO*///	
/*TODO*///	  i = 0;
/*TODO*///	  while( (int)kdaca_fn[i][0] != -1 ){
/*TODO*///	    fncode[(int)kdaca_fn[i][0]] = kdaca_fn[i][1];
/*TODO*///	    i++;
/*TODO*///	  }
/*TODO*///	
/*TODO*///	  i = j = 0;
/*TODO*///	  while( i < 0x200 ){
/*TODO*///	    if( fncode[i] != 0 ){
/*TODO*///	      if( i != j ){
/*TODO*///		fn = (fncode[i] - fncode[j]) / (i - j);
/*TODO*///		for( k = 1; k < (i-j); k++ )
/*TODO*///		  fncode[k+j] = fncode[j] + fn*k;
/*TODO*///		j = i;
/*TODO*///	      }
/*TODO*///	    }
/*TODO*///	    i++;
/*TODO*///	  }
/*TODO*///	 #if 0
/*TODO*///	 	for( i = 0; i < 0x200; i++ )
/*TODO*///	  logerror("fncode[%04x] = %.2f\n", i, fncode[i] );
/*TODO*///	 #endif
/*TODO*///	
/*TODO*///	#else
	  for( i = 0; i < 0x200; i++ ){
	    fncode[i] = (0x200 * 55) / (0x200 - i);
	//    logerror("2 : fncode[%04x] = %.2f\n", i, fncode[i] );
	  }
	
/*TODO*///	#endif
	}
	
	
	/************************************************/
	/*    Konami PCM update                         */
	/************************************************/
	
	static StreamInitMultiPtr KDAC_A_update = new StreamInitMultiPtr() {
            public void handler(int chip, ShortPtr[] buffer, int buffer_len) {
		int i;
	
	
		buffer[0] = new ShortPtr(buffer_len);
		buffer[1] = new ShortPtr(buffer_len);
	
		for( i = 0; i < KDAC_A_PCM_MAX; i++ )
		{
			if (kpcm[chip].play[i] != 0)
			{
				int volA,volB,j,out;
				int addr, old_addr;
	
				/**** PCM setup ****/
				addr = kpcm[chip].start[i] + ((kpcm[chip].addr[i]>>BASE_SHIFT)&0x000fffff);
				volA = 2 * kpcm[chip].vol[i][0];
				volB = 2 * kpcm[chip].vol[i][1];
				for( j = 0; j < buffer_len; j++ )
				{
					old_addr = addr;
					addr = kpcm[chip].start[i] + ((kpcm[chip].addr[i]>>BASE_SHIFT)&0x000fffff);
					while (old_addr <= addr)
					{
						if ((kpcm[chip].pcmbuf[i].read(old_addr) & 0x80) != 0)
						{
							/* end of sample */
	
							if (kpcm[chip].loop[i] != 0)
							{
								/* loop to the beginning */
								addr = kpcm[chip].start[i];
								kpcm[chip].addr[i] = 0;
							}
							else
							{
								/* stop sample */
								kpcm[chip].play[i] = 0;
							}
							break;
						}
	
						old_addr++;
					}
	
					if (kpcm[chip].play[i] == 0)
						break;
	
					kpcm[chip].addr[i] += kpcm[chip].step[i];
	
					out = (kpcm[chip].pcmbuf[i].read(addr) & 0x7f) - 0x40;
	
					buffer[0].memory[j*2 +1] += out * volA;
					buffer[1].memory[j*2 +1] += out * volB;
				}
			}
		}
            }
        };
	
	
	/************************************************/
	/*    Konami PCM start                          */
	/************************************************/
	public static int K007232_sh_start(MachineSound msound)
	{
		int i,j;
	
		intf = (K007232_interface) msound.sound_interface;
	
		/* Set up the chips */
		for (j=0; j<intf.num_chips; j++)
		{
			String[] buf= new String[2];
			String[] name=new String[2];
                        
                        for (int _i=0 ; _i<2 ; _i++){
                            buf[_i]="";
                            name[_i]="";
                        }
                        
			int[] vol=new int[2];
                        
                        kpcm[j] = new KDAC_A_PCM();
	
			kpcm[j].pcmbuf[0] = new UBytePtr(memory_region(intf.bank[j]));
			kpcm[j].pcmbuf[1] = new UBytePtr(memory_region(intf.bank[j]));
	
			for( i = 0; i < KDAC_A_PCM_MAX; i++ )
			{
				kpcm[j].start[i] = 0;
				kpcm[j].step[i] = 0;
				kpcm[j].play[i] = 0;
				kpcm[j].loop[i] = 0;
			}
			kpcm[j].vol[0][0] = 255;	/* channel A output to output A */
			kpcm[j].vol[0][1] = 0;
			kpcm[j].vol[1][0] = 0;
			kpcm[j].vol[1][1] = 255;	/* channel B output to output B */
	
			for( i = 0; i < 0x10; i++ )  kpcm[j].wreg[i] = 0;
	
			for (i = 0;i < 2;i++)
			{
				sprintf(buf[i],"K007232 #%d",j);//sprintf(buf[i],"007232 #%d Ch #%c",j,'A'+i);
                                name[i] = buf[i];
			}
	
			vol[0]=intf.volume[j] & 0xffff;
			vol[1]=intf.volume[j] >> 16;
	
			pcm_chan[j] = stream_init_multi(2,name,vol,Machine.sample_rate,
					j,KDAC_A_update);
		}
	
		KDAC_A_make_fncode();
	
		return 0;
	}
	
	/************************************************/
	/*    Konami PCM write register                 */
	/************************************************/
	static void K007232_WriteReg( int r, int v, int chip )
	{
		int  data;
	
		if (Machine.sample_rate == 0) return;
	
		stream_update(pcm_chan[chip],0);
	
		kpcm[chip].wreg[r] = (char) v;			/* stock write data */
	
		if (r == 0x05)
		{
			if (kpcm[chip].start[0] < 0x20000)
			{
				kpcm[chip].play[0] = 1;
				kpcm[chip].addr[0] = 0;
			}
		}
		else if (r == 0x0b)
		{
			if (kpcm[chip].start[1] < 0x20000)
			{
				kpcm[chip].play[1] = 1;
				kpcm[chip].addr[1] = 0;
			}
		}
		else if (r == 0x0d)
		{
			/* select if sample plays once or looped */
			kpcm[chip].loop[0] = v & 0x01;
			kpcm[chip].loop[1] = v & 0x02;
			return;
		}
		else if (r == 0x0c)
		{
			/* external port, usually volume control */
			if (intf.portwritehandler[chip] != null) (intf.portwritehandler[chip]).handler(v);
			return;
		}
		else
		{
			int  reg_port;
	
			reg_port = 0;
			if (r >= 0x06)
			{
				reg_port = 1;
				r -= 0x06;
			}
	
			switch (r)
			{
				case 0x00:
				case 0x01:
					/**** address step ****/
					data = ((((kpcm[chip].wreg[reg_port*0x06 + 0x01])<<8)&0x0100) | ((kpcm[chip].wreg[reg_port*0x06 + 0x00])&0x00ff));
/*TODO*///					#if 0
/*TODO*///					if( !reg_port && r == 1 )
/*TODO*///					logerror("%04x\n" ,data );
/*TODO*///					#endif
	
					kpcm[chip].step[reg_port] =
						(int) (( (7850.0 / (float)Machine.sample_rate) ) *
                                                ( fncode[data] / (440.00/2) ) *
                                                ( 3580000 / 4000000 ) *
                                                (1<<BASE_SHIFT));
					break;
	
				case 0x02:
				case 0x03:
				case 0x04:
					/**** start address ****/
					kpcm[chip].start[reg_port] =
						(((kpcm[chip].wreg[reg_port*0x06 + 0x04]<<16)&0x00010000) |
						((kpcm[chip].wreg[reg_port*0x06 + 0x03]<< 8)&0x0000ff00) |
						((kpcm[chip].wreg[reg_port*0x06 + 0x02]    )&0x000000ff));
				break;
			}
		}
	}
	
	/************************************************/
	/*    Konami PCM read register                  */
	/************************************************/
	static int K007232_ReadReg( int r, int chip )
	{
		if (r == 0x05)
		{
			if (kpcm[chip].start[0] < 0x20000)
			{
				kpcm[chip].play[0] = 1;
				kpcm[chip].addr[0] = 0;
			}
		}
		else if (r == 0x0b)
		{
			if (kpcm[chip].start[1] < 0x20000)
			{
				kpcm[chip].play[1] = 1;
				kpcm[chip].addr[1] = 0;
			}
		}
		return 0;
	}
	
	/*****************************************************************************/
	
	public static WriteHandlerPtr K007232_write_port_0_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		K007232_WriteReg(offset,data,0);
	} };
	
	public static ReadHandlerPtr K007232_read_port_0_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return K007232_ReadReg(offset,0);
	} };
	
	public static WriteHandlerPtr K007232_write_port_1_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		K007232_WriteReg(offset,data,1);
	} };
	
	public static ReadHandlerPtr K007232_read_port_1_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return K007232_ReadReg(offset,1);
	} };
	
	public static WriteHandlerPtr K007232_write_port_2_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		K007232_WriteReg(offset,data,2);
	} };
	
	public static ReadHandlerPtr K007232_read_port_2_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return K007232_ReadReg(offset,2);
	} };
	
	public static void K007232_bankswitch(int chip, UBytePtr ptr_A, UBytePtr ptr_B)
	{
		kpcm[chip].pcmbuf[0] = ptr_A;
		kpcm[chip].pcmbuf[1] = ptr_B;
	}
	
	public static void K007232_set_volume(int chip,int channel,int volumeA,int volumeB)
	{
		kpcm[chip].vol[channel][0] = (char) volumeA;
		kpcm[chip].vol[channel][1] = (char) volumeB;
	}
	
	/*****************************************************************************/
}
