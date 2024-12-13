/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mame056.sound;

import static arcadeflex056.fucPtr.*;
import static mame056.sound.ay8910H.*;

public class _2610intfH
{
/*TODO*///	#ifdef BUILD_YM2610
/*TODO*///	  void YM2610UpdateRequest(int chip);
/*TODO*///	#endif
/*TODO*///	
    public static final int   MAX_2610 = 2;
/*TODO*///	
/*TODO*///	#ifndef VOL_YM3012
/*TODO*///	/* #define YM3014_VOL(Vol,Pan) VOL_YM3012((Vol)/2,Pan,(Vol)/2,Pan) */
/*TODO*///	#define YM3012_VOL(LVol,LPan,RVol,RPan) (MIXER(LVol,LPan)|(MIXER(RVol,RPan) << 16))
/*TODO*///	#endif
/*TODO*///	
    public static class YM2610interface extends AY8910interface
    {
            int num;	/* total number of 8910 in the machine */
            int baseclock;
            int[] volumeSSG=new int[MAX_8910]; /* for SSG sound */
            ReadHandlerPtr[] portAread = new ReadHandlerPtr[MAX_8910];
            ReadHandlerPtr[] portBread = new ReadHandlerPtr[MAX_8910];
            WriteHandlerPtr[] portAwrite = new WriteHandlerPtr[MAX_8910];
            WriteHandlerPtr[] portBwrite = new WriteHandlerPtr[MAX_8910];
            WriteYmHandlerPtr[] handler = new WriteYmHandlerPtr[MAX_8910];	/* IRQ handler for the YM2610 */
            int[] pcmromb = new int[MAX_2610];		/* Delta-T rom region */
            int[] pcmroma = new int[MAX_2610];		/* ADPCM   rom region */
            int[] volumeFM = new int[MAX_2610];		/* use YM3012_VOL macro */
            
        public YM2610interface(int num, int baseclock, int[] mixing_level, ReadHandlerPtr[] pAr, ReadHandlerPtr[] pBr, WriteHandlerPtr[] pAw, WriteHandlerPtr[] pBw, WriteYmHandlerPtr[] ym_handler) {
            super(num, baseclock, mixing_level, pAr, pBr, pAw, pBw, ym_handler);
        }

        public YM2610interface(int num, int baseclock, int[] mixing_level, ReadHandlerPtr[] pAr, ReadHandlerPtr[] pBr, WriteHandlerPtr[] pAw, WriteHandlerPtr[] pBw) {
            super(num, baseclock, mixing_level, pAr, pBr, pAw, pBw);
        }

        public YM2610interface(int num, int baseclock, int[] volumeSSG, ReadHandlerPtr[] portAread, ReadHandlerPtr[] portBread, WriteHandlerPtr[] portAwrite, WriteHandlerPtr[] portBwrite, WriteYmHandlerPtr[] handler, int[] pcmromb, int[] pcmroma, int[] volumeFM) {
            //super(num, baseclock, volumeSSG, portAread, portBread, portAwrite, portBwrite, handler, pcmromb, pcmroma, volumeFM);
            super(num, baseclock, volumeSSG, portAread, portBread, portAwrite, portBwrite, handler);
            this.num = num;
            this.baseclock = baseclock;
            this.volumeSSG = volumeSSG;
            this.portAread = portAread;
            this.portBread = portBread;
            this.portAwrite = portAwrite;
            this.portBwrite = portBwrite;
            this.handler = handler;
            this.pcmromb = pcmromb;
            this.pcmroma = pcmroma;
            this.volumeFM = volumeFM;
        }
    };
	
/*TODO*///	/************************************************/
/*TODO*///	/* Sound Hardware Start							*/
/*TODO*///	/************************************************/
/*TODO*///	int YM2610_sh_start(const struct MachineSound *msound);
/*TODO*///	int YM2610B_sh_start(const struct MachineSound *msound);
/*TODO*///	
/*TODO*///	/************************************************/
/*TODO*///	/* Sound Hardware Stop							*/
/*TODO*///	/************************************************/
/*TODO*///	
/*TODO*///	
/*TODO*///	/************************************************/
/*TODO*///	/* Chip 0 functions								*/
/*TODO*///	/************************************************/
/*TODO*///	READ16_HANDLER( YM2610_status_port_0_A_lsb_r );
/*TODO*///	READ16_HANDLER( YM2610_status_port_0_B_lsb_r );
/*TODO*///	READ16_HANDLER( YM2610_read_port_0_lsb_r );
/*TODO*///	WRITE16_HANDLER( YM2610_control_port_0_A_lsb_w );
/*TODO*///	WRITE16_HANDLER( YM2610_control_port_0_B_lsb_w );
/*TODO*///	WRITE16_HANDLER( YM2610_data_port_0_A_lsb_w );
/*TODO*///	WRITE16_HANDLER( YM2610_data_port_0_B_lsb_w );
/*TODO*///	
/*TODO*///	/************************************************/
/*TODO*///	/* Chip 1 functions								*/
/*TODO*///	/************************************************/
/*TODO*///	READ16_HANDLER( YM2610_status_port_1_A_lsb_r );
/*TODO*///	READ16_HANDLER( YM2610_status_port_1_B_lsb_r );
/*TODO*///	READ16_HANDLER( YM2610_read_port_1_lsb_r );
/*TODO*///	WRITE16_HANDLER( YM2610_control_port_1_A_lsb_w );
/*TODO*///	WRITE16_HANDLER( YM2610_control_port_1_B_lsb_w );
/*TODO*///	WRITE16_HANDLER( YM2610_data_port_1_A_lsb_w );
/*TODO*///	WRITE16_HANDLER( YM2610_data_port_1_B_lsb_w );
/*TODO*///	
/*TODO*///	#endif
/*TODO*///	/**************** end of file ****************/
}
