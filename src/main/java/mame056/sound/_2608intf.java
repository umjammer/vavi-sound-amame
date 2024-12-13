/* don't support sampling rythm sound yet */
//#define YM2608_USE_SAMPLES
/***************************************************************************

  2608intf.c

  The YM2608 emulator supports up to 2 chips.
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
import static mame056.commonH.*;
import static mame056.sound._2608intfH.*;

public class _2608intf   extends snd_interface
{

    @Override
    public int chips_num(MachineSound msound) {
        return ((YM2608interface) msound.sound_interface).num;
    }

    @Override
    public int chips_clock(MachineSound msound) {
        return ((YM2608interface) msound.sound_interface).baseclock;
    }

    @Override
    public int start(MachineSound msound) {
        return YM2608_sh_start(msound);
    }

    @Override
    public void stop() {
        YM2608_sh_stop();
    }

    @Override
    public void update() {
        //NO functionality expected
    }

    @Override
    public void reset() {
        YM2608_sh_reset();
    }
/*TODO*///	
/*TODO*///	#ifdef BUILD_YM2608
/*TODO*///	
/*TODO*///	/* use FM.C with stream system */

	static int[] stream = new int[MAX_2608];

	static UBytePtr rhythm_buf;
	
	/* Global Interface holder */
	static YM2608interface intf;
	
	static timer_entry[][] Timer = new timer_entry[MAX_2608][2];

/*TODO*///	#ifdef YM2608_USE_SAMPLES
	static String ym2608_pDrumNames[] =
	{
		"2608_BD.wav",
		"2608_SD.wav",
		"2608_TOP.wav",
		"2608_HH.wav",
		"2608_TOM.wav",
		"2608_RIM.wav",
		null
	};
/*TODO*///	#endif
	
	/*------------------------- TM2608 -------------------------------*/
	/* IRQ Handler */
	static FM_IRQHANDLER_Ptr IRQHandler = new FM_IRQHANDLER_Ptr() {
            @Override
            public void handler(int n, int irq) {
                    if(intf.handler[n] != null) intf.handler[n].handler(irq);
            }
        };
	
	/* Timer overflow callback from timer.c */
	static timer_callback timer_callback_2608 = new timer_callback() {
            public void handler(int param) {
                    int n=param&0x7f;
                    int c=param>>7;

            //	logerror("2608 TimerOver %d\n",c);
                    Timer[n][c] = null;
                    YM2608TimerOver(n,c);
            }
        };
	
	/* TimerHandler from fm.c */
	static FM_TIMERHANDLER_Ptr TimerHandler = new FM_TIMERHANDLER_Ptr() {
            public void handler(int n, int c, double count, double stepTime) {
                    if( count == 0 )
                    {	/* Reset FM Timer */
                            if( Timer[n][c] != null )
                            {
            //			logerror("2608 TimerReset %d\n",c);
                                    timer_remove (Timer[n][c]);
                                    Timer[n][c] = null;
                            }
                    }
                    else
                    {	/* Start FM Timer */
                            double timeSec = (double)count * stepTime;

                            if( Timer[n][c] == null )
                            {
                                    Timer[n][c] = timer_set (timeSec , (c<<7)|n, timer_callback_2608 );
                            }
                    }
            }
        };
	
	static void FMTimerInit()
	{
		int i;
	
		for( i = 0 ; i < MAX_2608 ; i++ )
			Timer[i][0] = Timer[i][1] = null;
	}
	
/*TODO*///	/* update request from fm.c */
/*TODO*///	void YM2608UpdateRequest(int chip)
/*TODO*///	{
/*TODO*///		stream_update(stream[chip],100);
/*TODO*///	}
	
	public static int YM2608_sh_start(MachineSound msound)
	{
		int i,j;
		int rate = Machine.sample_rate;
		String[] buf=new String[YM2608_NUMBUF];
		String[] name = new String[YM2608_NUMBUF];
		int mixed_vol;
                int[] vol = new int[YM2608_NUMBUF];
		UBytePtr[] pcmbufa = new UBytePtr[YM2608_NUMBUF];
		int[]  pcmsizea = new int[YM2608_NUMBUF];
		int[] rhythm_pos = new int[6+1];
		GameSamples	psSamples;
		int total_size,r_offset,s_size;
	
		intf = (YM2608interface) msound.sound_interface;
		if( intf.num > MAX_2608 ) return 1;
	
		if (AY8910_sh_start_ym(msound) != 0) return 1;
	
		/* Timer Handler set */
		FMTimerInit();
	
		/* stream system initialize */
		for (i = 0;i < intf.num;i++)
		{
			/* stream setup */
			mixed_vol = intf.volumeFM[i];
			/* stream setup */
			for (j = 0 ; j < YM2608_NUMBUF ; j++)
			{
				name[j]=buf[j];
				vol[j] = mixed_vol & 0xffff;
				mixed_vol>>=16;
/*TODO*///				sprintf(buf[j],"%s #%d Ch%d",sound_name(msound),i,j+1);
			}
			stream[i] = stream_init_multi(YM2608_NUMBUF,name,vol,rate,i,YM2608UpdateOne);
			/* setup adpcm buffers */
			pcmbufa[i]  = new UBytePtr(memory_region(intf.pcmrom[i]));
			pcmsizea[i] = memory_region_length(intf.pcmrom[i]);
		}
	
		/* rythm rom build */
		rhythm_buf = null;
/*TODO*///	#ifdef YM2608_USE_SAMPLES
/*TODO*///		psSamples = readsamples(ym2608_pDrumNames,"ym2608");
/*TODO*///	#else
		psSamples = null;
/*TODO*///	#endif
		if( psSamples != null )
		{
			/* calcrate total data size */
			total_size = 0;
			for( i=0;i<6;i++)
			{
				s_size = psSamples.sample[i].length;
				total_size += s_size!=0 ? s_size : 1;
			}
			/* aloocate rythm data */
			rhythm_buf = new UBytePtr(total_size);
			if( rhythm_buf==null ) return 0;
	
			r_offset = 0;
			/* merge sampling data */
			for(i=0;i<6;i++)
			{
				/* set start point */
				rhythm_pos[i] = r_offset*2;
				/* copy sample data */
				s_size = psSamples.sample[i].length;
				if((s_size!=0) && psSamples.sample[i].data!=null)
				{
					if( psSamples.sample[i].resolution==16 )
					{
						ShortPtr s_ptr = new ShortPtr(psSamples.sample[i].data);
						for(j=0;j<s_size;j++) rhythm_buf.write(r_offset++, s_ptr.readinc());
					}else{
						ShortPtr s_ptr = new ShortPtr(psSamples.sample[i].data);
						for(j=0;j<s_size;j++) rhythm_buf.write(r_offset++, s_ptr.readinc()*0x0101);
					}
				}else rhythm_buf.write(r_offset++, 0);
				/* set end point */
				rhythm_pos[i+1] = r_offset*2;
			}
			freesamples( psSamples );
		}else
		{
			/* aloocate rythm data */
			rhythm_buf = new UBytePtr(6);
			if( rhythm_buf==null ) return 0;
			for(i=0;i<6;i++)
			{
				/* set start point */
				rhythm_pos[i] = i*2;
				rhythm_buf.write(i, 0);
				/* set end point */
				rhythm_pos[i+1] = (i+1)*2;
			}
		}
	
		/**** initialize YM2608 ****/
		if (YM2608Init(intf.num,intf.baseclock,rate,
			           pcmbufa,pcmsizea,rhythm_buf,rhythm_pos,
			           TimerHandler,IRQHandler) == 0)
			return 0;
	
		/* error */
		return 1;
	}
	
	/************************************************/
	/* Sound Hardware Stop							*/
	/************************************************/
	public static void YM2608_sh_stop()
	{
		YM2608Shutdown();
		if( rhythm_buf != null ) rhythm_buf=null;
		rhythm_buf = null;
		AY8910_sh_stop_ym();
	}
	/* reset */
	public static void YM2608_sh_reset()
	{
		int i;
	
		for (i = 0;i < intf.num;i++)
			YM2608ResetChip(i);
	}
	
	/************************************************/
	/* Status Read for YM2608 - Chip 0				*/
	/************************************************/
	public static ReadHandlerPtr YM2608_status_port_0_A_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
	//logerror("PC %04x: 2608 S0A=%02X\n",cpu_get_pc(),YM2608Read(0,0));
		return YM2608Read(0,0);
	} };
	
	public static ReadHandlerPtr YM2608_status_port_0_B_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
	//logerror("PC %04x: 2608 S0B=%02X\n",cpu_get_pc(),YM2608Read(0,2));
		return YM2608Read(0,2);
	} };
	
/*TODO*///	/************************************************/
/*TODO*///	/* Status Read for YM2608 - Chip 1				*/
/*TODO*///	/************************************************/
/*TODO*///	public static ReadHandlerPtr YM2608_status_port_1_A_r  = new ReadHandlerPtr() { public int handler(int offset) {
/*TODO*///		return YM2608Read(1,0);
/*TODO*///	} };
/*TODO*///	
/*TODO*///	public static ReadHandlerPtr YM2608_status_port_1_B_r  = new ReadHandlerPtr() { public int handler(int offset) {
/*TODO*///		return YM2608Read(1,2);
/*TODO*///	} };
/*TODO*///	
/*TODO*///	/************************************************/
/*TODO*///	/* Port Read for YM2608 - Chip 0				*/
/*TODO*///	/************************************************/
/*TODO*///	public static ReadHandlerPtr YM2608_read_port_0_r  = new ReadHandlerPtr() { public int handler(int offset){
/*TODO*///		return YM2608Read(0,1);
/*TODO*///	} };
/*TODO*///	
/*TODO*///	/************************************************/
/*TODO*///	/* Port Read for YM2608 - Chip 1				*/
/*TODO*///	/************************************************/
/*TODO*///	public static ReadHandlerPtr YM2608_read_port_1_r  = new ReadHandlerPtr() { public int handler(int offset){
/*TODO*///		return YM2608Read(1,1);
/*TODO*///	} };
	
	/************************************************/
	/* Control Write for YM2608 - Chip 0			*/
	/* Consists of 2 addresses						*/
	/************************************************/
	public static WriteHandlerPtr YM2608_control_port_0_A_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		YM2608Write(0,0,data);
	} };
	
	public static WriteHandlerPtr YM2608_control_port_0_B_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		YM2608Write(0,2,data);
	} };
	
/*TODO*///	/************************************************/
/*TODO*///	/* Control Write for YM2608 - Chip 1			*/
/*TODO*///	/* Consists of 2 addresses						*/
/*TODO*///	/************************************************/
/*TODO*///	public static WriteHandlerPtr YM2608_control_port_1_A_w = new WriteHandlerPtr() {public void handler(int offset, int data){
/*TODO*///		YM2608Write(1,0,data);
/*TODO*///	} };
/*TODO*///	
/*TODO*///	public static WriteHandlerPtr YM2608_control_port_1_B_w = new WriteHandlerPtr() {public void handler(int offset, int data){
/*TODO*///		YM2608Write(1,2,data);
/*TODO*///	} };
	
	/************************************************/
	/* Data Write for YM2608 - Chip 0				*/
	/* Consists of 2 addresses						*/
	/************************************************/
	public static WriteHandlerPtr YM2608_data_port_0_A_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		YM2608Write(0,1,data);
	} };
	
	public static WriteHandlerPtr YM2608_data_port_0_B_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		YM2608Write(0,3,data);
	} };
	
/*TODO*///	/************************************************/
/*TODO*///	/* Data Write for YM2608 - Chip 1				*/
/*TODO*///	/* Consists of 2 addresses						*/
/*TODO*///	/************************************************/
/*TODO*///	public static WriteHandlerPtr YM2608_data_port_1_A_w = new WriteHandlerPtr() {public void handler(int offset, int data){
/*TODO*///		YM2608Write(1,1,data);
/*TODO*///	} };
/*TODO*///	public static WriteHandlerPtr YM2608_data_port_1_B_w = new WriteHandlerPtr() {public void handler(int offset, int data){
/*TODO*///		YM2608Write(1,3,data);
/*TODO*///	} };
/*TODO*///	
/*TODO*///	/**************** end of file ****************/
/*TODO*///	
/*TODO*///	#endif
}
