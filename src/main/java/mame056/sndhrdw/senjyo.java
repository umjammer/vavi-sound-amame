/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mame056.sndhrdw;

import static arcadeflex056.fucPtr.*;
import static common.ptr.*;
import static mame056.cpuexec.*;
import static mame056.cpuexecH.*;
import static mame056.machine.z80fmly.*;
import static mame056.machine.z80fmlyH.*;
import static mame056.mame.*;
import static mame056.sndintrf.*;
import static mame056.sndintrfH.*;
import static mame037b11.sound.mixer.mixer_play_sample;
import static mame037b11.sound.mixer.mixer_set_sample_frequency;
import static mame056.sound.mixer.*;
import static mame056.sound.mixerH.*;

public class senjyo
{
	
	
	/* z80 pio */
	static Interrupt_retiPtr pio_interrupt = new Interrupt_retiPtr() {
            public void handler(int state) {
                cpu_cause_interrupt (1, Z80_VECTOR(0,state) );
            }
        };
	
	static z80pio_interface pio_intf = new z80pio_interface
	(
		1,
		new Interrupt_retiPtr[]{pio_interrupt},
		new Interrupt_retiPtr[]{null},
		new Interrupt_retiPtr[]{null}
	);
	
	/* z80 ctc */
	static ReadHandlerPtr ctc_interrupt = new ReadHandlerPtr() {
            public int handler(int state) {
                cpu_cause_interrupt (1, Z80_VECTOR(1,state) );
                
                return 0;
            }
        };
	
	static z80ctc_interface ctc_intf = new z80ctc_interface
	(
		1,                   /* 1 chip */
		new int[]{ 0 },               /* clock (filled in from the CPU 0 clock */
		new int[]{ NOTIMER_2 },       /* timer disables */
		new ReadHandlerPtr[]{ ctc_interrupt },   /* interrupt handler */
		new WriteHandlerPtr[]{ z80ctc_0_trg1_w }, /* ZC/TO0 callback */
		new WriteHandlerPtr[]{ null },               /* ZC/TO1 callback */
		new WriteHandlerPtr[]{ null }                /* ZC/TO2 callback */
	);
	
	
	/* single tone generator */
	public static int SINGLE_LENGTH = 10000;
	public static int SINGLE_DIVIDER = 8;
	
	static BytePtr _single = new BytePtr();
	static int single_rate = 1000;
	static int single_volume = 0;
	static int channel;
	
	
	public static WriteHandlerPtr senjyo_volume_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		single_volume = data & 0x0f;
		mixer_set_volume(channel,single_volume * 100 / 15);
	} };
	
	public static ShStartPtr senjyo_sh_start = new ShStartPtr() {
            public int handler(MachineSound msound) {
                int i;	
	
		channel = mixer_allocate_channel(15);
		mixer_set_name(channel,"Tone");
	
		/* z80 ctc init */
		ctc_intf.baseclock[0] = Machine.drv.cpu[1].cpu_clock;
		z80ctc_init (ctc_intf);
	
		/* z80 pio init */
		z80pio_init (pio_intf);
	
		if ((_single = new BytePtr(SINGLE_LENGTH)) == null)
		{
			_single = null;
			return 1;
		}
		for (i = 0;i < SINGLE_LENGTH;i++)		/* freq = ctc2 zco / 8 */
			_single.write(i, ((i/SINGLE_DIVIDER)&0x01)*127);
	
		/* CTC2 single tone generator */
		mixer_set_volume(channel,0);
		mixer_play_sample(channel,_single,SINGLE_LENGTH,single_rate,1);
	
		return 0;
            }
        };
	
	
	public static ShStopPtr senjyo_sh_stop = new ShStopPtr() {
            public void handler() {
                _single = null;
            }
        };
	
	
	public static ShUpdatePtr senjyo_sh_update = new ShUpdatePtr() {
            public void handler() {
                double period;
	
		if (Machine.sample_rate == 0) return;
	
	
		/* ctc2 timer single tone generator frequency */
		period = z80ctc_getperiod (0, 2);
		if( period != 0 ) single_rate = (int)(1.0 / period );
		else single_rate = 0;
	
		mixer_set_sample_frequency(channel,single_rate);
            }
        };

}
