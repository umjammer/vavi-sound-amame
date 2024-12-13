/***************************************************************************

  2610intf.c

  The YM2610 emulator supports up to 2 chips.
  Each chip has the following connections:
  - Status Read / Control Write A
  - Port Read / Data Write A
  - Control Write B
  - Data Write B

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mame056.sound;

import static arcadeflex056.fucPtr.*;
import static common.ptr.*;
import static common.libc.cstdio.*;
import static arcadeflex056.osdepend.logerror;
import static mame056.mame.Machine;
import static mame056.sndintrf.*;
import static mame056.sndintrfH.*;
import static mame056.timer.*;
import static mame056.timerH.*;
import static mame056.sound.mixer.mixer_set_volume;
import static mame056.sound.streams.*;
import static mame056.sound.ay8910.*;
import static mame037b11.sound.fm.*;
import static mame037b11.sound.fmH.*;
import static mame056.common.*;
import static mame056.sound._2610intfH.*;

public class _2610intf  extends snd_interface
{

    @Override
    public int chips_num(MachineSound msound) {
        return ((YM2610interface) msound.sound_interface).num;
    }

    @Override
    public int chips_clock(MachineSound msound) {
        return ((YM2610interface) msound.sound_interface).baseclock;
    }

    @Override
    public int start(MachineSound msound) {
        return YM2610_sh_start(msound);
    }

    @Override
    public void stop() {
        YM2610_sh_stop();
    }

    @Override
    public void update() {
        //NO functionality expected
    }

    @Override
    public void reset() {
        YM2610_sh_reset();
    }

/*TODO*///	#if BUILD_YM2610
/*TODO*///	
/*TODO*///	/* use FM.C with stream system */

    static int[] stream = new int[MAX_2610];

    /* Global Interface holder */
    static YM2610interface intf;

    static timer_entry[][] Timer = new timer_entry[MAX_2610][2];

    /*------------------------- TM2610 -------------------------------*/
    /* IRQ Handler */
    static FM_IRQHANDLER_Ptr IRQHandler = new FM_IRQHANDLER_Ptr() {
        @Override
        public void handler(int n, int irq) {
            if(intf.handler[n] != null) intf.handler[n].handler(irq);
        }
    };
  	
    /* Timer overflow callback from timer.c */
    static timer_callback timer_callback_2610 = new timer_callback() {
        public void handler(int param) {
            int n=param&0x7f;
            int c=param>>7;

    //	logerror("2610 TimerOver %d\n",c);
            Timer[n][c] = null;
            YM2610TimerOver(n,c);
        }
    };

    /* TimerHandler from fm.c */
    static FM_TIMERHANDLER_Ptr TimerHandler = new FM_TIMERHANDLER_Ptr() {
        public void handler(int n, int c, double count, double stepTime) {
            if( count == 0 )
            {	/* Reset FM Timer */
                    if( Timer[n][c] != null )
                    {
    //			logerror("2610 TimerReset %d\n",c);
                            timer_remove (Timer[n][c]);
                            Timer[n][c] = null;
                    }
            }
            else
            {	/* Start FM Timer */
                    double timeSec = (double)count * stepTime;

                    if( Timer[n][c] == null )
                    {
                            Timer[n][c] = timer_set (timeSec , (c<<7)|n, timer_callback_2610 );
                    }
            }
        }
    };


    static void FMTimerInit()
    {
            int i;

            for( i = 0 ; i < MAX_2610 ; i++ )
                    Timer[i][0] = Timer[i][1] = null;
    }
	
/*TODO*///	/* update request from fm.c */
/*TODO*///	void YM2610UpdateRequest(int chip)
/*TODO*///	{
/*TODO*///		stream_update(stream[chip],100);
/*TODO*///	}
	
	public static int YM2610_sh_start(MachineSound msound)
	{
		int i,j;
		int rate = Machine.sample_rate;
		String[] buf = new String[YM2610_NUMBUF];
		String[] name= new String[YM2610_NUMBUF];
		int mixed_vol;
                int[] vol = new int[YM2610_NUMBUF];
		UBytePtr[] pcmbufa=new UBytePtr[YM2610_NUMBUF], pcmbufb=new UBytePtr[YM2610_NUMBUF];
		int[]  pcmsizea=new int[YM2610_NUMBUF],pcmsizeb=new int[YM2610_NUMBUF];
	
		intf = (YM2610interface) msound.sound_interface;
		if( intf.num > MAX_2610 ) return 1;
	
		if (AY8910_sh_start(msound) != 0) return 1;
	
		/* Timer Handler set */
		FMTimerInit();
	
		/* stream system initialize */
		for (i = 0;i < intf.num;i++)
		{
			/* stream setup */
			mixed_vol = intf.volumeFM[i];
			/* stream setup */
			for (j = 0 ; j < YM2610_NUMBUF ; j++)
			{
				name[j]=buf[j];
				vol[j] = mixed_vol & 0xffff;
				mixed_vol>>=16;
/*TODO*///				sprintf(buf[j],"%s #%d Ch%d",sound_name(msound),i,j+1);
			}
			stream[i] = stream_init_multi(YM2610_NUMBUF,name,vol,rate,i,YM2610UpdateOne);
			/* setup adpcm buffers */
			pcmbufa[i]  = new UBytePtr(memory_region(intf.pcmroma[i]));
			pcmsizea[i] = memory_region_length(intf.pcmroma[i]);
			pcmbufb[i]  = new UBytePtr(memory_region(intf.pcmromb[i]));
			pcmsizeb[i] = memory_region_length(intf.pcmromb[i]);
		}
	
		/**** initialize YM2610 ****/
		if (YM2610Init(intf.num,intf.baseclock,rate,
			           pcmbufa,pcmsizea,pcmbufb,pcmsizeb,
			           TimerHandler,IRQHandler) == 0)
			return 0;
	
		/* error */
		return 1;
	}
	
/*TODO*///	#if BUILD_YM2610B
/*TODO*///	int YM2610B_sh_start(const struct MachineSound *msound)
/*TODO*///	{
/*TODO*///		int i,j;
/*TODO*///		int rate = Machine.sample_rate;
/*TODO*///		char buf[YM2610_NUMBUF][40];
/*TODO*///		const char *name[YM2610_NUMBUF];
/*TODO*///		int mixed_vol,vol[YM2610_NUMBUF];
/*TODO*///		void *pcmbufa[YM2610_NUMBUF],*pcmbufb[YM2610_NUMBUF];
/*TODO*///		int  pcmsizea[YM2610_NUMBUF],pcmsizeb[YM2610_NUMBUF];
/*TODO*///	
/*TODO*///		intf = msound.sound_interface;
/*TODO*///		if( intf.num > MAX_2610 ) return 1;
/*TODO*///	
/*TODO*///		if (AY8910_sh_start_ym(msound)) return 1;
/*TODO*///	
/*TODO*///		/* Timer Handler set */
/*TODO*///		FMTimerInit();
/*TODO*///	
/*TODO*///		/* stream system initialize */
/*TODO*///		for (i = 0;i < intf.num;i++)
/*TODO*///		{
/*TODO*///			/* stream setup */
/*TODO*///			mixed_vol = intf.volumeFM[i];
/*TODO*///			/* stream setup */
/*TODO*///			for (j = 0 ; j < YM2610_NUMBUF ; j++)
/*TODO*///			{
/*TODO*///				name[j]=buf[j];
/*TODO*///				vol[j] = mixed_vol & 0xffff;
/*TODO*///				mixed_vol>>=16;
/*TODO*///				sprintf(buf[j],"%s #%d Ch%d",sound_name(msound),i,j+1);
/*TODO*///			}
/*TODO*///			stream[i] = stream_init_multi(YM2610_NUMBUF,name,vol,rate,i,YM2610BUpdateOne);
/*TODO*///			/* setup adpcm buffers */
/*TODO*///			pcmbufa[i]  = (void *)(memory_region(intf.pcmroma[i]));
/*TODO*///			pcmsizea[i] = memory_region_length(intf.pcmroma[i]);
/*TODO*///			pcmbufb[i]  = (void *)(memory_region(intf.pcmromb[i]));
/*TODO*///			pcmsizeb[i] = memory_region_length(intf.pcmromb[i]);
/*TODO*///		}
/*TODO*///	
/*TODO*///		/**** initialize YM2610 ****/
/*TODO*///		if (YM2610Init(intf.num,intf.baseclock,rate,
/*TODO*///			           pcmbufa,pcmsizea,pcmbufb,pcmsizeb,
/*TODO*///			           TimerHandler,IRQHandler) == 0)
/*TODO*///			return 0;
/*TODO*///	
/*TODO*///		/* error */
/*TODO*///		return 1;
/*TODO*///	}
/*TODO*///	#endif
	
	/************************************************/
	/* Sound Hardware Stop							*/
	/************************************************/
	public static void YM2610_sh_stop()
	{
		YM2610Shutdown();
		AY8910_sh_stop_ym();
	}
	
	/* reset */
	static void YM2610_sh_reset()
	{
		int i;
	
		for (i = 0;i < intf.num;i++)
			YM2610ResetChip(i);
	}
	
	/************************************************/
	/* Status Read for YM2610 - Chip 0				*/
	/************************************************/
	public static ReadHandlerPtr YM2610_status_port_0_A_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
	//logerror("PC %04x: 2610 S0A=%02X\n",cpu_get_pc(),YM2610Read(0,0));
		return YM2610Read(0,0);
	} };
	
/*TODO*///	READ16_HANDLER( YM2610_status_port_0_A_lsb_r )
/*TODO*///	{
/*TODO*///	//logerror("PC %04x: 2610 S0A=%02X\n",cpu_get_pc(),YM2610Read(0,0));
/*TODO*///		return YM2610Read(0,0);
/*TODO*///	}
	
	public static ReadHandlerPtr YM2610_status_port_0_B_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
	//logerror("PC %04x: 2610 S0B=%02X\n",cpu_get_pc(),YM2610Read(0,2));
		return YM2610Read(0,2);
	} };
	
/*TODO*///	READ16_HANDLER( YM2610_status_port_0_B_lsb_r )
/*TODO*///	{
/*TODO*///	//logerror("PC %04x: 2610 S0B=%02X\n",cpu_get_pc(),YM2610Read(0,2));
/*TODO*///		return YM2610Read(0,2);
/*TODO*///	}
/*TODO*///	
/*TODO*///	/************************************************/
/*TODO*///	/* Status Read for YM2610 - Chip 1				*/
/*TODO*///	/************************************************/
/*TODO*///	public static ReadHandlerPtr YM2610_status_port_1_A_r  = new ReadHandlerPtr() { public int handler(int offset) {
/*TODO*///		return YM2610Read(1,0);
/*TODO*///	} };
/*TODO*///	
/*TODO*///	READ16_HANDLER( YM2610_status_port_1_A_lsb_r ) {
/*TODO*///		return YM2610Read(1,0);
/*TODO*///	}
/*TODO*///	
/*TODO*///	public static ReadHandlerPtr YM2610_status_port_1_B_r  = new ReadHandlerPtr() { public int handler(int offset) {
/*TODO*///		return YM2610Read(1,2);
/*TODO*///	} };
/*TODO*///	
/*TODO*///	READ16_HANDLER( YM2610_status_port_1_B_lsb_r ) {
/*TODO*///		return YM2610Read(1,2);
/*TODO*///	}
	
	/************************************************/
	/* Port Read for YM2610 - Chip 0				*/
	/************************************************/
	public static ReadHandlerPtr YM2610_read_port_0_r  = new ReadHandlerPtr() { public int handler(int offset){
		return YM2610Read(0,1);
	} };
	
/*TODO*///	READ16_HANDLER( YM2610_read_port_0_lsb_r ){
/*TODO*///		return YM2610Read(0,1);
/*TODO*///	}
/*TODO*///	
/*TODO*///	/************************************************/
/*TODO*///	/* Port Read for YM2610 - Chip 1				*/
/*TODO*///	/************************************************/
/*TODO*///	public static ReadHandlerPtr YM2610_read_port_1_r  = new ReadHandlerPtr() { public int handler(int offset){
/*TODO*///		return YM2610Read(1,1);
/*TODO*///	} };
/*TODO*///	
/*TODO*///	READ16_HANDLER( YM2610_read_port_1_lsb_r ){
/*TODO*///		return YM2610Read(1,1);
/*TODO*///	}
	
	/************************************************/
	/* Control Write for YM2610 - Chip 0			*/
	/* Consists of 2 addresses						*/
	/************************************************/
	public static WriteHandlerPtr YM2610_control_port_0_A_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	//logerror("PC %04x: 2610 Reg A %02X",cpu_get_pc(),data);
		YM2610Write(0,0,data);
	} };
/*TODO*///	
/*TODO*///	WRITE16_HANDLER( YM2610_control_port_0_A_lsb_w )
/*TODO*///	{
/*TODO*///	//logerror("PC %04x: 2610 Reg A %02X",cpu_get_pc(),data);
/*TODO*///		if (ACCESSING_LSB)
/*TODO*///		{
/*TODO*///			YM2610Write(0,0,data);
/*TODO*///		}
/*TODO*///	}
	
	public static WriteHandlerPtr YM2610_control_port_0_B_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	//logerror("PC %04x: 2610 Reg B %02X",cpu_get_pc(),data);
		YM2610Write(0,2,data);
	} };
	
/*TODO*///	WRITE16_HANDLER( YM2610_control_port_0_B_lsb_w )
/*TODO*///	{
/*TODO*///	//logerror("PC %04x: 2610 Reg B %02X",cpu_get_pc(),data);
/*TODO*///		if (ACCESSING_LSB)
/*TODO*///		{
/*TODO*///			YM2610Write(0,2,data);
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	/************************************************/
/*TODO*///	/* Control Write for YM2610 - Chip 1			*/
/*TODO*///	/* Consists of 2 addresses						*/
/*TODO*///	/************************************************/
/*TODO*///	public static WriteHandlerPtr YM2610_control_port_1_A_w = new WriteHandlerPtr() {public void handler(int offset, int data){
/*TODO*///		YM2610Write(1,0,data);
/*TODO*///	} };
/*TODO*///	
/*TODO*///	WRITE16_HANDLER( YM2610_control_port_1_A_lsb_w ){
/*TODO*///		if (ACCESSING_LSB)
/*TODO*///		{
/*TODO*///			YM2610Write(1,0,data);
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	public static WriteHandlerPtr YM2610_control_port_1_B_w = new WriteHandlerPtr() {public void handler(int offset, int data){
/*TODO*///		YM2610Write(1,2,data);
/*TODO*///	} };
/*TODO*///	
/*TODO*///	WRITE16_HANDLER( YM2610_control_port_1_B_lsb_w ){
/*TODO*///		if (ACCESSING_LSB)
/*TODO*///		{
/*TODO*///			YM2610Write(1,2,data);
/*TODO*///		}
/*TODO*///	}
	
	/************************************************/
	/* Data Write for YM2610 - Chip 0				*/
	/* Consists of 2 addresses						*/
	/************************************************/
	public static WriteHandlerPtr YM2610_data_port_0_A_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	//logerror(" =%02X\n",data);
		YM2610Write(0,1,data);
	} };
	
/*TODO*///	WRITE16_HANDLER( YM2610_data_port_0_A_lsb_w )
/*TODO*///	{
/*TODO*///	//logerror(" =%02X\n",data);
/*TODO*///		if (ACCESSING_LSB)
/*TODO*///		{
/*TODO*///			YM2610Write(0,1,data);
/*TODO*///		}
/*TODO*///	}
	
	public static WriteHandlerPtr YM2610_data_port_0_B_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	//logerror(" =%02X\n",data);
		YM2610Write(0,3,data);
	} };
	
/*TODO*///	WRITE16_HANDLER( YM2610_data_port_0_B_lsb_w )
/*TODO*///	{
/*TODO*///	//logerror(" =%02X\n",data);
/*TODO*///		if (ACCESSING_LSB)
/*TODO*///		{
/*TODO*///			YM2610Write(0,3,data);
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	/************************************************/
/*TODO*///	/* Data Write for YM2610 - Chip 1				*/
/*TODO*///	/* Consists of 2 addresses						*/
/*TODO*///	/************************************************/
/*TODO*///	public static WriteHandlerPtr YM2610_data_port_1_A_w = new WriteHandlerPtr() {public void handler(int offset, int data){
/*TODO*///		YM2610Write(1,1,data);
/*TODO*///	} };
/*TODO*///	
/*TODO*///	WRITE16_HANDLER( YM2610_data_port_1_A_lsb_w ){
/*TODO*///		if (ACCESSING_LSB)
/*TODO*///		{
/*TODO*///			YM2610Write(1,1,data);
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	public static WriteHandlerPtr YM2610_data_port_1_B_w = new WriteHandlerPtr() {public void handler(int offset, int data){
/*TODO*///		YM2610Write(1,3,data);
/*TODO*///	} };
/*TODO*///	
/*TODO*///	WRITE16_HANDLER( YM2610_data_port_1_B_lsb_w ){
/*TODO*///		if (ACCESSING_LSB)
/*TODO*///		{
/*TODO*///			YM2610Write(1,3,data);
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	/**************** end of file ****************/
/*TODO*///	
/*TODO*///	#endif
}
