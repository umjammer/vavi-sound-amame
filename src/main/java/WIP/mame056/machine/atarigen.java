/*##########################################################################

	atarigen.c

	General functions for Atari raster games.

##########################################################################*/


/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.machine;

import static arcadeflex056.osdepend.logerror;
import static WIP.mame056.machine.atarigenH.*;
import static common.libc.cstdio.*;
import static common.libc.cstring.*;
import static arcadeflex056.fileio.*;
import static arcadeflex056.fucPtr.*;
import static common.ptr.*;
import static arcadeflex056.fucPtr.*;
import static mame056.common.*;
import static mame056.commonH.*;
import static mame056.cpuintrfH.*;
import static mame056.cpuintrf.*;
import static mame056.cpuexec.*;
import static mame056.cpuexecH.*;
import static mame056.mame.*;
import static mame056.memory.*;
import static mame056.memoryH.*;
import static mame056.sound.mixer.*;
import static mame056.sound.mixerH.*;
import static mame056.timerH.*;
import static mame056.timer.*;

public class atarigen
{
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	/*##########################################################################
/*TODO*///		CONSTANTS
/*TODO*///	##########################################################################*/
/*TODO*///	
/*TODO*///	#define SOUND_INTERLEAVE_RATE		TIME_IN_USEC(50)
/*TODO*///	#define SOUND_INTERLEAVE_REPEAT		20
	
	
	
	/*##########################################################################
		GLOBAL VARIABLES
	##########################################################################*/
	
	public static int 				atarigen_scanline_int_state;
	public static int 				atarigen_sound_int_state;
	public static int 				atarigen_video_int_state;

	public static UBytePtr	atarigen_eeprom_default = new UBytePtr();
        public static UBytePtr	atarigen_eeprom = new UBytePtr();
        public static int[]	atarigen_eeprom_size = new int[1];

	public static int 				atarigen_cpu_to_sound_ready;
	public static int 				atarigen_sound_to_cpu_ready;

/*TODO*///	data16_t *			atarivc_data;
/*TODO*///	data16_t *			atarivc_eof_data;
/*TODO*///	struct atarivc_state_desc atarivc_state;
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	/*##########################################################################
/*TODO*///		STATIC VARIABLES
/*TODO*///	##########################################################################*/
/*TODO*///	
	static atarigen_int_callback update_int_callback;
	static timer_entry		scanline_interrupt_timer;

	static int 		eeprom_unlocked;
	
	static int 		atarigen_slapstic_num;
/*TODO*///	static data16_t *	atarigen_slapstic;
	static int			atarigen_slapstic_bank;
/*TODO*///	static void *		atarigen_slapstic_bank0;

	static int 		sound_cpu_num;
	static int 		atarigen_cpu_to_sound;
	static int 		atarigen_sound_to_cpu;
	static int 		timed_int;
	static int 		ym2151_int;
	
        static UBytePtr		speed_a, speed_b;
	static int 		speed_pc;

	static atarigen_scanline_callback scanline_callback;
	static int 			scanlines_per_callback;
	static double 		scanline_callback_period;
	static int 			last_scanline;

/*TODO*///	static int 			actual_vc_latch0;
/*TODO*///	static int 			actual_vc_latch1;
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	/*##########################################################################
/*TODO*///		STATIC FUNCTION DECLARATIONS
/*TODO*///	##########################################################################*/
/*TODO*///	
/*TODO*///	static void scanline_interrupt_callback(int param);
/*TODO*///	
/*TODO*///	static void decompress_eeprom_word(const data16_t *data);
/*TODO*///	static void decompress_eeprom_byte(const data16_t *data);
/*TODO*///	
/*TODO*///	static static void sound_comm_timer(int reps_left);
/*TODO*///	static void delayed_sound_reset(int param);
/*TODO*///	static void delayed_sound_w(int param);
/*TODO*///	static void delayed_6502_sound_w(int param);
/*TODO*///	
/*TODO*///	static void atarigen_set_vol(int volume, const char *string);
/*TODO*///	
/*TODO*///	static void vblank_timer(int param);
/*TODO*///	static void scanline_timer(int scanline);
/*TODO*///	
/*TODO*///	static void atarivc_common_w(offs_t offset, data16_t newword);
/*TODO*///	
/*TODO*///	static void unhalt_cpu(int param);
	
	
	
	/*##########################################################################
		INTERRUPT HANDLING
	##########################################################################*/
	
	/*---------------------------------------------------------------
		atarigen_interrupt_reset: Initializes the state of all
		the interrupt sources.
	---------------------------------------------------------------*/
	
	public static void atarigen_interrupt_reset(atarigen_int_callback update_int)
	{
		/* set the callback */
		update_int_callback = update_int;
	
		/* reset the interrupt states */
		atarigen_video_int_state = atarigen_sound_int_state = atarigen_scanline_int_state = 0;
		scanline_interrupt_timer = null;
	}
	
	
	/*---------------------------------------------------------------
		atarigen_update_interrupts: Forces the interrupt callback
		to be called with the current VBLANK and sound interrupt
		states.
	---------------------------------------------------------------*/
	
	public static void atarigen_update_interrupts()
	{
		update_int_callback.handler();
	}
	
	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		atarigen_scanline_int_set: Sets the scanline when the next
/*TODO*///		scanline interrupt should be generated.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	void atarigen_scanline_int_set(int scanline)
/*TODO*///	{
/*TODO*///		if (scanline_interrupt_timer)
/*TODO*///			timer_remove(scanline_interrupt_timer);
/*TODO*///		scanline_interrupt_timer = timer_set(cpu_getscanlinetime(scanline), 0, scanline_interrupt_callback);
/*TODO*///	}
	
	
	/*---------------------------------------------------------------
		atarigen_scanline_int_gen: Standard interrupt routine
		which sets the scanline interrupt state.
	---------------------------------------------------------------*/
	
	public static int atarigen_scanline_int_gen()
	{
		atarigen_scanline_int_state = 1;
		update_int_callback.handler();
		return ignore_interrupt.handler();
	}
	
	
	/*---------------------------------------------------------------
		atarigen_scanline_int_ack_w: Resets the state of the
		scanline interrupt.
	---------------------------------------------------------------*/
	
	public static WriteHandlerPtr atarigen_scanline_int_ack_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		atarigen_scanline_int_state = 0;
		update_int_callback.handler();
	}};
	
/*TODO*///	WRITE32_HANDLER( atarigen_scanline_int_ack32_w )
/*TODO*///	{
/*TODO*///		atarigen_scanline_int_state = 0;
/*TODO*///		(*update_int_callback)();
/*TODO*///	}
	
	
	/*---------------------------------------------------------------
		atarigen_sound_int_gen: Standard interrupt routine which
		sets the sound interrupt state.
	---------------------------------------------------------------*/
	
	public static int atarigen_sound_int_gen()
	{
		atarigen_sound_int_state = 1;
		update_int_callback.handler();
		return ignore_interrupt.handler();
	}
	
	
	/*---------------------------------------------------------------
		atarigen_sound_int_ack_w: Resets the state of the sound
		interrupt.
	---------------------------------------------------------------*/
	
	public static WriteHandlerPtr16 atarigen_sound_int_ack_w = new WriteHandlerPtr16() {public void handler(int offset, int data, int d)
	{
		atarigen_sound_int_state = 0;
		update_int_callback.handler();
	}};
	
/*TODO*///	WRITE32_HANDLER( atarigen_sound_int_ack32_w )
/*TODO*///	{
/*TODO*///		atarigen_sound_int_state = 0;
/*TODO*///		(*update_int_callback)();
/*TODO*///	}
	
	
	/*---------------------------------------------------------------
		atarigen_video_int_gen: Standard interrupt routine which
		sets the video interrupt state.
	---------------------------------------------------------------*/
	
	public static int atarigen_video_int_gen()
	{
		atarigen_video_int_state = 1;
		update_int_callback.handler();
		return ignore_interrupt.handler();
	}
	
	
	/*---------------------------------------------------------------
		atarigen_video_int_ack_w: Resets the state of the video
		interrupt.
	---------------------------------------------------------------*/
	// 16bits
	public static WriteHandlerPtr atarigen_video_int_ack_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		atarigen_video_int_state = 0;
		update_int_callback.handler();
	}};
	
/*TODO*///	WRITE32_HANDLER( atarigen_video_int_ack32_w )
/*TODO*///	{
/*TODO*///		atarigen_video_int_state = 0;
/*TODO*///		(*update_int_callback)();
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		scanline_interrupt_callback: Signals an interrupt.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	static void scanline_interrupt_callback(int param)
/*TODO*///	{
/*TODO*///		/* generate the interrupt */
/*TODO*///		atarigen_scanline_int_gen();
/*TODO*///	
/*TODO*///		/* set a new timer to go off at the same scan line next frame */
/*TODO*///		scanline_interrupt_timer = timer_set(TIME_IN_HZ(Machine->drv->frames_per_second), 0, scanline_interrupt_callback);
/*TODO*///	}
	
	
	
	/*##########################################################################
		EEPROM HANDLING
	##########################################################################*/
	
	/*---------------------------------------------------------------
		atarigen_eeprom_reset: Makes sure that the unlocked state
		is cleared when we reset.
	---------------------------------------------------------------*/
	
	public static void atarigen_eeprom_reset()
	{
		eeprom_unlocked = 0;
	}
	
	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		atarigen_eeprom_enable_w: Any write to this handler will
/*TODO*///		allow one byte to be written to the EEPROM data area the
/*TODO*///		next time.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	WRITE16_HANDLER( atarigen_eeprom_enable_w )
/*TODO*///	{
/*TODO*///		eeprom_unlocked = 1;
/*TODO*///	}
/*TODO*///	
/*TODO*///	WRITE32_HANDLER( atarigen_eeprom_enable32_w )
/*TODO*///	{
/*TODO*///		eeprom_unlocked = 1;
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		atarigen_eeprom_w: Writes a "word" to the EEPROM, which is
/*TODO*///		almost always accessed via the low byte of the word only.
/*TODO*///		If the EEPROM hasn't been unlocked, the write attempt is
/*TODO*///		ignored.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	WRITE16_HANDLER( atarigen_eeprom_w )
/*TODO*///	{
/*TODO*///		if (eeprom_unlocked == 0)
/*TODO*///			return;
/*TODO*///	
/*TODO*///		COMBINE_DATA(&atarigen_eeprom[offset]);
/*TODO*///		eeprom_unlocked = 0;
/*TODO*///	}
/*TODO*///	
/*TODO*///	WRITE32_HANDLER( atarigen_eeprom32_w )
/*TODO*///	{
/*TODO*///		if (eeprom_unlocked == 0)
/*TODO*///			return;
/*TODO*///	
/*TODO*///		COMBINE_DATA(&((data32_t *)atarigen_eeprom)[offset]);
/*TODO*///		eeprom_unlocked = 0;
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		atarigen_eeprom_r: Reads a "word" from the EEPROM, which is
/*TODO*///		almost always accessed via the low byte of the word only.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	READ16_HANDLER( atarigen_eeprom_r )
/*TODO*///	{
/*TODO*///		return atarigen_eeprom[offset] | 0xff00;
/*TODO*///	}
/*TODO*///	
/*TODO*///	READ16_HANDLER( atarigen_eeprom_upper_r )
/*TODO*///	{
/*TODO*///		return atarigen_eeprom[offset] | 0x00ff;
/*TODO*///	}
/*TODO*///	
/*TODO*///	READ32_HANDLER( atarigen_eeprom_upper32_r )
/*TODO*///	{
/*TODO*///		return (atarigen_eeprom[offset * 2] << 16) | atarigen_eeprom[offset * 2 + 1] | 0x00ff00ff;
/*TODO*///	}
	
	
	/*---------------------------------------------------------------
		atarigen_nvram_handler: Loads the EEPROM data.
	---------------------------------------------------------------*/
	
	public static nvramPtr atarigen_nvram_handler = new nvramPtr() {
            public void handler(Object file, int read_or_write) {
                if (read_or_write != 0)
			osd_fwrite(file, atarigen_eeprom, atarigen_eeprom_size[0]);
		else if (file != null)
			osd_fread(file, atarigen_eeprom, atarigen_eeprom_size[0]);
		else
		{
			/* all 0xff's work for most games */
			memset(atarigen_eeprom, 0xff, atarigen_eeprom_size[0]);
	
			/* anything else must be decompressed */
			if (atarigen_eeprom_default != null)
			{
				if (atarigen_eeprom_default.read(0) == 0)
					decompress_eeprom_byte(new UBytePtr(atarigen_eeprom_default, 1));
				else
					decompress_eeprom_word(new UBytePtr(atarigen_eeprom_default, 1));
			}
		}
            }
        };
	
	
	/*---------------------------------------------------------------
		decompress_eeprom_word: Used for decompressing EEPROM data
		that has every other byte invalid.
	---------------------------------------------------------------*/
	
	public static void decompress_eeprom_word(UBytePtr data)
	{
		xShortPtr dest = new xShortPtr(atarigen_eeprom);
		int value;
	
		while ((value = data.readinc()) != 0)
		{
			int count = (value >> 8);
			value = (value << 8) | (value & 0xff);
	
			while (count-- != 0){
				dest.write((char) value);
                                dest.inc();
                        }
		}
	}
	
	
	/*---------------------------------------------------------------
		decompress_eeprom_byte: Used for decompressing EEPROM data
		that is byte-packed.
	---------------------------------------------------------------*/
	
	public static void decompress_eeprom_byte(UBytePtr data)
	{
		UBytePtr dest = new UBytePtr(atarigen_eeprom);
		int value;
	
		while ((value = data.readinc()) != 0)
		{
			int count = (value >> 8);
			value = (value << 8) | (value & 0xff);
	
			while (count-- != 0)
				dest.writeinc( value );
		}
	}
	
	
	
/*TODO*///	/*##########################################################################
/*TODO*///		SLAPSTIC HANDLING
/*TODO*///	##########################################################################*/
/*TODO*///	
/*TODO*///	INLINE void update_bank(int bank)
/*TODO*///	{
/*TODO*///		/* if the bank has changed, copy the memory; Pit Fighter needs this */
/*TODO*///		if (bank != atarigen_slapstic_bank)
/*TODO*///		{
/*TODO*///			/* bank 0 comes from the copy we made earlier */
/*TODO*///			if (bank == 0)
/*TODO*///				memcpy(atarigen_slapstic, atarigen_slapstic_bank0, 0x2000);
/*TODO*///			else
/*TODO*///				memcpy(atarigen_slapstic, &atarigen_slapstic[bank * 0x1000], 0x2000);
/*TODO*///	
/*TODO*///			/* remember the current bank */
/*TODO*///			atarigen_slapstic_bank = bank;
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		atarigen_slapstic_init: Installs memory handlers for the
/*TODO*///		slapstic and sets the chip number.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	void atarigen_slapstic_init(int cpunum, int base, int chipnum)
/*TODO*///	{
/*TODO*///		atarigen_slapstic_num = chipnum;
/*TODO*///		atarigen_slapstic = NULL;
/*TODO*///	
/*TODO*///		/* if we have a chip, install it */
/*TODO*///		if (chipnum)
/*TODO*///		{
/*TODO*///			/* initialize the slapstic */
/*TODO*///			slapstic_init(chipnum);
/*TODO*///	
/*TODO*///			/* install the memory handlers */
/*TODO*///			atarigen_slapstic = install_mem_read16_handler(cpunum, base, base + 0x7fff, atarigen_slapstic_r);
/*TODO*///			atarigen_slapstic = install_mem_write16_handler(cpunum, base, base + 0x7fff, atarigen_slapstic_w);
/*TODO*///	
/*TODO*///			/* allocate memory for a copy of bank 0 */
/*TODO*///			atarigen_slapstic_bank0 = auto_malloc(0x2000);
/*TODO*///			if (atarigen_slapstic_bank0)
/*TODO*///				memcpy(atarigen_slapstic_bank0, atarigen_slapstic, 0x2000);
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		atarigen_slapstic_reset: Makes the selected slapstic number
/*TODO*///		active and resets its state.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	void atarigen_slapstic_reset(void)
/*TODO*///	{
/*TODO*///		if (atarigen_slapstic_num)
/*TODO*///		{
/*TODO*///			slapstic_reset();
/*TODO*///			update_bank(slapstic_bank());
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		atarigen_slapstic_w: Assuming that the slapstic sits in
/*TODO*///		ROM memory space, we just simply tweak the slapstic at this
/*TODO*///		address and do nothing more.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	WRITE16_HANDLER( atarigen_slapstic_w )
/*TODO*///	{
/*TODO*///		update_bank(slapstic_tweak(offset));
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		atarigen_slapstic_r: Tweaks the slapstic at the appropriate
/*TODO*///		address and then reads a word from the underlying memory.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	READ16_HANDLER( atarigen_slapstic_r )
/*TODO*///	{
/*TODO*///		/* fetch the result from the current bank first */
/*TODO*///		int result = atarigen_slapstic[offset & 0xfff];
/*TODO*///	
/*TODO*///		/* then determine the new one */
/*TODO*///		update_bank(slapstic_tweak(offset));
/*TODO*///		return result;
/*TODO*///	}
	
	
	
	/*##########################################################################
		SOUND I/O
	##########################################################################*/
	
	/*---------------------------------------------------------------
		atarigen_sound_io_reset: Resets the state of the sound I/O.
	---------------------------------------------------------------*/
	
	public static void atarigen_sound_io_reset(int cpu_num)
	{
		/* remember which CPU is the sound CPU */
		sound_cpu_num = cpu_num;
	
		/* reset the internal interrupts states */
		timed_int = ym2151_int = 0;
	
		/* reset the sound I/O states */
		atarigen_cpu_to_sound = atarigen_sound_to_cpu = 0;
		atarigen_cpu_to_sound_ready = atarigen_sound_to_cpu_ready = 0;
	}
	
	
	/*---------------------------------------------------------------
		atarigen_6502_irq_gen: Generates an IRQ signal to the 6502
		sound processor.
	---------------------------------------------------------------*/
	
	public static InterruptPtr atarigen_6502_irq_gen = new InterruptPtr() { public int handler() 
	{
		timed_int = 1;
		update_6502_irq();
		return ignore_interrupt.handler();
	} };
	
	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		atarigen_6502_irq_ack_r: Resets the IRQ signal to the 6502
/*TODO*///		sound processor. Both reads and writes can be used.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	public static ReadHandlerPtr atarigen_6502_irq_ack_r  = new ReadHandlerPtr() { public int handler(int offset)
/*TODO*///	{
/*TODO*///		timed_int = 0;
/*TODO*///		update_6502_irq();
/*TODO*///		return ignore_interrupt();
/*TODO*///	} };
	
	public static WriteHandlerPtr atarigen_6502_irq_ack_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		timed_int = 0;
		update_6502_irq();
	} };
	
	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		atarigen_ym2151_irq_gen: Sets the state of the YM2151's
/*TODO*///		IRQ line.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	void atarigen_ym2151_irq_gen(int irq)
/*TODO*///	{
/*TODO*///		ym2151_int = irq;
/*TODO*///		update_6502_irq();
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		atarigen_sound_reset_w: Write handler which resets the
/*TODO*///		sound CPU in response.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	WRITE16_HANDLER( atarigen_sound_reset_w )
/*TODO*///	{
/*TODO*///		timer_set(TIME_NOW, 0, delayed_sound_reset);
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		atarigen_sound_reset: Resets the state of the sound CPU
/*TODO*///		manually.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	void atarigen_sound_reset(void)
/*TODO*///	{
/*TODO*///		timer_set(TIME_NOW, 1, delayed_sound_reset);
/*TODO*///	}
	
	
	/*---------------------------------------------------------------
		atarigen_sound_w: Handles communication from the main CPU
		to the sound CPU. Two versions are provided, one with the
		data byte in the low 8 bits, and one with the data byte in
		the upper 8 bits.
	---------------------------------------------------------------*/
	// 16bits
	public static WriteHandlerPtr atarigen_sound_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
/*TODO*///		if (ACCESSING_LSB)
/*TODO*///			timer_set(TIME_NOW, data & 0xff, delayed_sound_w);
	}};
	
/*TODO*///	WRITE16_HANDLER( atarigen_sound_upper_w )
/*TODO*///	{
/*TODO*///		if (ACCESSING_MSB)
/*TODO*///			timer_set(TIME_NOW, (data >> 8) & 0xff, delayed_sound_w);
/*TODO*///	}
/*TODO*///	
/*TODO*///	WRITE32_HANDLER( atarigen_sound_upper32_w )
/*TODO*///	{
/*TODO*///		if (ACCESSING_MSB32)
/*TODO*///			timer_set(TIME_NOW, (data >> 24) & 0xff, delayed_sound_w);
/*TODO*///	}
	
	
	/*---------------------------------------------------------------
		atarigen_sound_r: Handles reading data communicated from the
		sound CPU to the main CPU. Two versions are provided, one
		with the data byte in the low 8 bits, and one with the data
		byte in the upper 8 bits.
	---------------------------------------------------------------*/
	
	public static ReadHandlerPtr atarigen_sound_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		atarigen_sound_to_cpu_ready = 0;
		atarigen_sound_int_ack_w.handler(0, 0, 0);
		return atarigen_sound_to_cpu | 0xff00;
	}};
	
/*TODO*///	READ16_HANDLER( atarigen_sound_upper_r )
/*TODO*///	{
/*TODO*///		atarigen_sound_to_cpu_ready = 0;
/*TODO*///		atarigen_sound_int_ack_w(0, 0, 0);
/*TODO*///		return (atarigen_sound_to_cpu << 8) | 0x00ff;
/*TODO*///	}
/*TODO*///	
/*TODO*///	READ32_HANDLER( atarigen_sound_upper32_r )
/*TODO*///	{
/*TODO*///		atarigen_sound_to_cpu_ready = 0;
/*TODO*///		atarigen_sound_int_ack32_w(0, 0, 0);
/*TODO*///		return (atarigen_sound_to_cpu << 24) | 0x00ffffff;
/*TODO*///	}
	
	
	/*---------------------------------------------------------------
		atarigen_6502_sound_w: Handles communication from the sound
		CPU to the main CPU.
	---------------------------------------------------------------*/
	
	public static WriteHandlerPtr atarigen_6502_sound_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		timer_set(TIME_NOW, data, delayed_6502_sound_w);
	} };
	
	
	/*---------------------------------------------------------------
		atarigen_6502_sound_r: Handles reading data communicated
		from the main CPU to the sound CPU.
	---------------------------------------------------------------*/
	
	public static ReadHandlerPtr atarigen_6502_sound_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		atarigen_cpu_to_sound_ready = 0;
		cpu_set_nmi_line(sound_cpu_num, CLEAR_LINE);
		return atarigen_cpu_to_sound;
	} };
	
	
	/*---------------------------------------------------------------
		update_6502_irq: Called whenever the IRQ state changes. An
		interrupt is generated if either atarigen_6502_irq_gen()
		was called, or if the YM2151 generated an interrupt via
		the atarigen_ym2151_irq_gen() callback.
	---------------------------------------------------------------*/
	
	public static void update_6502_irq()
	{
		if (timed_int!=0 || ym2151_int!=0)
			cpu_set_irq_line(sound_cpu_num, M6502_INT_IRQ, ASSERT_LINE);
		else
			cpu_set_irq_line(sound_cpu_num, M6502_INT_IRQ, CLEAR_LINE);
	}
	
	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		sound_comm_timer: Set whenever a command is written from
/*TODO*///		the main CPU to the sound CPU, in order to temporarily bump
/*TODO*///		up the interleave rate. This helps ensure that communications
/*TODO*///		between the two CPUs works properly.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	static void sound_comm_timer(int reps_left)
/*TODO*///	{
/*TODO*///		if (--reps_left)
/*TODO*///			timer_set(SOUND_INTERLEAVE_RATE, reps_left, sound_comm_timer);
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		delayed_sound_reset: Synchronizes the sound reset command
/*TODO*///		between the two CPUs.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	static void delayed_sound_reset(int param)
/*TODO*///	{
/*TODO*///		/* unhalt and reset the sound CPU */
/*TODO*///		if (param == 0)
/*TODO*///		{
/*TODO*///			cpu_set_halt_line(sound_cpu_num, CLEAR_LINE);
/*TODO*///			cpu_set_reset_line(sound_cpu_num, PULSE_LINE);
/*TODO*///		}
/*TODO*///	
/*TODO*///		/* reset the sound write state */
/*TODO*///		atarigen_sound_to_cpu_ready = 0;
/*TODO*///		atarigen_sound_int_ack_w(0, 0, 0);
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		delayed_sound_w: Synchronizes a data write from the main
/*TODO*///		CPU to the sound CPU.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	static void delayed_sound_w(int param)
/*TODO*///	{
/*TODO*///		/* warn if we missed something */
/*TODO*///		if (atarigen_cpu_to_sound_ready)
/*TODO*///			logerror("Missed command from 68010\n");
/*TODO*///	
/*TODO*///		/* set up the states and signal an NMI to the sound CPU */
/*TODO*///		atarigen_cpu_to_sound = param;
/*TODO*///		atarigen_cpu_to_sound_ready = 1;
/*TODO*///		cpu_set_nmi_line(sound_cpu_num, ASSERT_LINE);
/*TODO*///	
/*TODO*///		/* allocate a high frequency timer until a response is generated */
/*TODO*///		/* the main CPU is *very* sensistive to the timing of the response */
/*TODO*///		timer_set(SOUND_INTERLEAVE_RATE, SOUND_INTERLEAVE_REPEAT, sound_comm_timer);
/*TODO*///	}
	
	
	/*---------------------------------------------------------------
		delayed_6502_sound_w: Synchronizes a data write from the
		sound CPU to the main CPU.
	---------------------------------------------------------------*/
	
	static timer_callback delayed_6502_sound_w = new timer_callback() {
            public void handler(int param) {
                /* warn if we missed something */
		if (atarigen_sound_to_cpu_ready != 0)
			logerror("Missed result from 6502\n");
	
		/* set up the states and signal the sound interrupt to the main CPU */
		atarigen_sound_to_cpu = param;
		atarigen_sound_to_cpu_ready = 1;
		atarigen_sound_int_gen();
            }
        };
	
	
	
	/*##########################################################################
		SOUND HELPERS
	##########################################################################*/
	
	/*---------------------------------------------------------------
		atarigen_init_6502_speedup: Installs a special read handler
		to catch the main spin loop in the 6502 sound code. The
		addresses accessed seem to be the same across a large
		number of games, though the PC shifts.
	---------------------------------------------------------------*/
	
	public static void atarigen_init_6502_speedup(int cpunum, int compare_pc1, int compare_pc2)
	{
		UBytePtr memory = new UBytePtr(memory_region(REGION_CPU1+cpunum));
		int address_low, address_high;
	
		/* determine the pointer to the first speed check location */
		address_low = memory.read(compare_pc1 + 1) | (memory.read(compare_pc1 + 2) << 8);
		address_high = memory.read(compare_pc1 + 4) | (memory.read(compare_pc1 + 5) << 8);
		if (address_low != address_high - 1)
			logerror("Error: address %04X does not point to a speedup location!", compare_pc1);
		speed_a = new UBytePtr(memory, address_low);
	
		/* determine the pointer to the second speed check location */
		address_low = memory.read(compare_pc2 + 1) | (memory.read(compare_pc2 + 2) << 8);
		address_high = memory.read(compare_pc2 + 4) | (memory.read(compare_pc2 + 5) << 8);
		if (address_low != address_high - 1)
			logerror("Error: address %04X does not point to a speedup location!", compare_pc2);
		speed_b = new UBytePtr(memory, address_low);
	
		/* install a handler on the second address */
		speed_pc = compare_pc2;
		install_mem_read_handler(cpunum, address_low, address_low, m6502_speedup_r);
	}
	
	
	/*---------------------------------------------------------------
		atarigen_set_vol: Scans for a particular sound chip and
		changes the volume on all channels associated with it.
	---------------------------------------------------------------*/
	
	public static void atarigen_set_vol(int volume, String string)
	{
		int ch;
	
		for (ch = 0; ch < MIXER_MAX_CHANNELS; ch++)
		{
			String name = mixer_get_name(ch);
			if (name!=null && (name.equals(string)))
				mixer_set_volume(ch, volume);
		}
	}
	
	
	/*---------------------------------------------------------------
		atarigen_set_XXXXX_vol: Sets the volume for a given type
		of chip.
	---------------------------------------------------------------*/
	
	public static void atarigen_set_ym2151_vol(int volume)
	{
		atarigen_set_vol(volume, "2151");
	}
	
/*TODO*///	void atarigen_set_ym2413_vol(int volume)
/*TODO*///	{
/*TODO*///		atarigen_set_vol(volume, "2413");
/*TODO*///	}
	
	public static void atarigen_set_pokey_vol(int volume)
	{
		atarigen_set_vol(volume, "POKEY");
	}
	
	public static void atarigen_set_tms5220_vol(int volume)
	{
		atarigen_set_vol(volume, "5220");
	}
	
/*TODO*///	void atarigen_set_oki6295_vol(int volume)
/*TODO*///	{
/*TODO*///		atarigen_set_vol(volume, "6295");
/*TODO*///	}
	
	
	/*---------------------------------------------------------------
		m6502_speedup_r: Handles speeding up the 6502.
	---------------------------------------------------------------*/
	
	public static ReadHandlerPtr m6502_speedup_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int result = speed_b.read(0);
	
		if (cpu_getpreviouspc() == speed_pc && speed_a.read(0) == speed_a.read(1) && result == speed_b.read(1))
			cpu_spinuntil_int();
	
		return result;
	} };
	
	
	
	/*##########################################################################
		SCANLINE TIMING
	##########################################################################*/
	
	/*---------------------------------------------------------------
		atarigen_scanline_timer_reset: Sets up the scanline timer.
	---------------------------------------------------------------*/
	
	public static void atarigen_scanline_timer_reset(atarigen_scanline_callback update_graphics, int frequency)
	{
		/* set the scanline callback */
		scanline_callback = update_graphics;
		scanline_callback_period = (double)frequency * cpu_getscanlineperiod();
		scanlines_per_callback = frequency;
	
		/* compute the last scanline */
		last_scanline = (int)(TIME_IN_HZ(Machine.drv.frames_per_second) / cpu_getscanlineperiod());
	
		/* set a timer to go off on the next VBLANK */
		timer_set(cpu_getscanlinetime(Machine.drv.screen_height), 0, vblank_timer);
	}
	
	
	/*---------------------------------------------------------------
		vblank_timer: Called once every VBLANK to prime the scanline
		timers.
	---------------------------------------------------------------*/
	
	static timer_callback vblank_timer = new timer_callback() {
            public void handler(int param) {
                /* set a timer to go off at scanline 0 */
		timer_set(TIME_IN_USEC(Machine.drv.vblank_duration), 0, scanline_timer);
	
		/* set a timer to go off on the next VBLANK */
		timer_set(cpu_getscanlinetime(Machine.drv.screen_height), 1, vblank_timer);
            }
        };
	
	
	/*---------------------------------------------------------------
		scanline_timer: Called once every n scanlines to generate
		the periodic callback to the main system.
	---------------------------------------------------------------*/
	
	static timer_callback scanline_timer = new timer_callback() {
            public void handler(int scanline) {
		/* callback */
		if (scanline_callback != null)
		{
			scanline_callback.handler(scanline);
	
			/* generate another? */
			scanline += scanlines_per_callback;
			if (scanline < last_scanline && scanlines_per_callback!=0)
				timer_set(scanline_callback_period, scanline, scanline_timer);
		}
            }
        };
	
	
	
/*TODO*///	/*##########################################################################
/*TODO*///		VIDEO CONTROLLER
/*TODO*///	##########################################################################*/
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		atarivc_eof_update: Callback that slurps up data and feeds
/*TODO*///		it into the video controller registers every refresh.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	static void atarivc_eof_update(int param)
/*TODO*///	{
/*TODO*///		atarivc_update(atarivc_eof_data);
/*TODO*///		timer_set(cpu_getscanlinetime(0), 0, atarivc_eof_update);
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		atarivc_reset: Initializes the video controller.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	void atarivc_reset(data16_t *eof_data)
/*TODO*///	{
/*TODO*///		/* this allows us to manually reset eof_data to NULL if it's not used */
/*TODO*///		atarivc_eof_data = eof_data;
/*TODO*///	
/*TODO*///		/* clear the RAM we use */
/*TODO*///		memset(atarivc_data, 0, 0x40);
/*TODO*///		memset(&atarivc_state, 0, sizeof(atarivc_state));
/*TODO*///	
/*TODO*///		/* reset the latches */
/*TODO*///		atarivc_state.latch1 = atarivc_state.latch2 = -1;
/*TODO*///		actual_vc_latch0 = actual_vc_latch1 = -1;
/*TODO*///	
/*TODO*///		/* start a timer to go off a little before scanline 0 */
/*TODO*///		if (atarivc_eof_data)
/*TODO*///			timer_set(cpu_getscanlinetime(0), 0, atarivc_eof_update);
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		atarivc_update: Copies the data from the specified location
/*TODO*///		once/frame into the video controller registers.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	void atarivc_update(const data16_t *data)
/*TODO*///	{
/*TODO*///		int i;
/*TODO*///	
/*TODO*///		/* echo all the commands to the video controller */
/*TODO*///		for (i = 0; i < 0x1c; i++)
/*TODO*///			if (data[i])
/*TODO*///				atarivc_common_w(i, data[i]);
/*TODO*///	
/*TODO*///		/* update the scroll positions */
/*TODO*///		atarimo_set_xscroll(0, atarivc_state.mo_xscroll, 0);
/*TODO*///		ataripf_set_xscroll(0, atarivc_state.pf0_xscroll, 0);
/*TODO*///		ataripf_set_xscroll(1, atarivc_state.pf1_xscroll, 0);
/*TODO*///		atarimo_set_yscroll(0, atarivc_state.mo_yscroll, 0);
/*TODO*///		ataripf_set_yscroll(0, atarivc_state.pf0_yscroll, 0);
/*TODO*///		ataripf_set_yscroll(1, atarivc_state.pf1_yscroll, 0);
/*TODO*///	
/*TODO*///		/* use this for debugging the video controller values */
/*TODO*///	#if 0
/*TODO*///		if (keyboard_pressed(KEYCODE_8))
/*TODO*///		{
/*TODO*///			static FILE *out;
/*TODO*///			if (out == 0) out = fopen("scroll.log", "w");
/*TODO*///			if (out)
/*TODO*///			{
/*TODO*///				for (i = 0; i < 64; i++)
/*TODO*///					fprintf(out, "%04X ", data[i]);
/*TODO*///				fprintf(out, "\n");
/*TODO*///			}
/*TODO*///		}
/*TODO*///	#endif
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		atarivc_w: Handles an I/O write to the video controller.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	WRITE16_HANDLER( atarivc_w )
/*TODO*///	{
/*TODO*///		int oldword = atarivc_data[offset];
/*TODO*///		int newword = oldword;
/*TODO*///	
/*TODO*///		COMBINE_DATA(&newword);
/*TODO*///		atarivc_common_w(offset, newword);
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		atarivc_common_w: Does the bulk of the word for an I/O
/*TODO*///		write.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	static void atarivc_common_w(offs_t offset, data16_t newword)
/*TODO*///	{
/*TODO*///		int oldword = atarivc_data[offset];
/*TODO*///		atarivc_data[offset] = newword;
/*TODO*///	
/*TODO*///		/* switch off the offset */
/*TODO*///		switch (offset)
/*TODO*///		{
/*TODO*///			/*
/*TODO*///				additional registers:
/*TODO*///	
/*TODO*///					01 = vertical start (for centering)
/*TODO*///					04 = horizontal start (for centering)
/*TODO*///			*/
/*TODO*///	
/*TODO*///			/* set the scanline interrupt here */
/*TODO*///			case 0x03:
/*TODO*///				if (oldword != newword)
/*TODO*///					atarigen_scanline_int_set(newword & 0x1ff);
/*TODO*///				break;
/*TODO*///	
/*TODO*///			/* latch enable */
/*TODO*///			case 0x0a:
/*TODO*///	
/*TODO*///				/* reset the latches when disabled */
/*TODO*///				ataripf_set_latch_lo((newword & 0x0080) ? actual_vc_latch0 : -1);
/*TODO*///				ataripf_set_latch_hi((newword & 0x0080) ? actual_vc_latch1 : -1);
/*TODO*///	
/*TODO*///				/* check for rowscroll enable */
/*TODO*///				atarivc_state.rowscroll_enable = (newword & 0x2000) >> 13;
/*TODO*///	
/*TODO*///				/* check for palette banking */
/*TODO*///				atarivc_state.palette_bank = ((newword & 0x0400) >> 10) ^ 1;
/*TODO*///				break;
/*TODO*///	
/*TODO*///			/* indexed parameters */
/*TODO*///			case 0x10: case 0x11: case 0x12: case 0x13:
/*TODO*///			case 0x14: case 0x15: case 0x16: case 0x17:
/*TODO*///			case 0x18: case 0x19: case 0x1a: case 0x1b:
/*TODO*///				switch (newword & 15)
/*TODO*///				{
/*TODO*///					case 9:
/*TODO*///						atarivc_state.mo_xscroll = (newword >> 7) & 0x1ff;
/*TODO*///						break;
/*TODO*///	
/*TODO*///					case 10:
/*TODO*///						atarivc_state.pf1_xscroll_raw = (newword >> 7) & 0x1ff;
/*TODO*///						atarivc_update_pf_xscrolls();
/*TODO*///						break;
/*TODO*///	
/*TODO*///					case 11:
/*TODO*///						atarivc_state.pf0_xscroll_raw = (newword >> 7) & 0x1ff;
/*TODO*///						atarivc_update_pf_xscrolls();
/*TODO*///						break;
/*TODO*///	
/*TODO*///					case 13:
/*TODO*///						atarivc_state.mo_yscroll = (newword >> 7) & 0x1ff;
/*TODO*///						break;
/*TODO*///	
/*TODO*///					case 14:
/*TODO*///						atarivc_state.pf1_yscroll = (newword >> 7) & 0x1ff;
/*TODO*///						break;
/*TODO*///	
/*TODO*///					case 15:
/*TODO*///						atarivc_state.pf0_yscroll = (newword >> 7) & 0x1ff;
/*TODO*///						break;
/*TODO*///				}
/*TODO*///				break;
/*TODO*///	
/*TODO*///			/* latch 1 value */
/*TODO*///			case 0x1c:
/*TODO*///				actual_vc_latch0 = -1;
/*TODO*///				actual_vc_latch1 = newword;
/*TODO*///				ataripf_set_latch_lo((atarivc_data[0x0a] & 0x80) ? actual_vc_latch0 : -1);
/*TODO*///				ataripf_set_latch_hi((atarivc_data[0x0a] & 0x80) ? actual_vc_latch1 : -1);
/*TODO*///				break;
/*TODO*///	
/*TODO*///			/* latch 2 value */
/*TODO*///			case 0x1d:
/*TODO*///				actual_vc_latch0 = newword;
/*TODO*///				actual_vc_latch1 = -1;
/*TODO*///				ataripf_set_latch_lo((atarivc_data[0x0a] & 0x80) ? actual_vc_latch0 : -1);
/*TODO*///				ataripf_set_latch_hi((atarivc_data[0x0a] & 0x80) ? actual_vc_latch1 : -1);
/*TODO*///				break;
/*TODO*///	
/*TODO*///			/* scanline IRQ ack here */
/*TODO*///			case 0x1e:
/*TODO*///				atarigen_scanline_int_ack_w(0, 0, 0);
/*TODO*///				break;
/*TODO*///	
/*TODO*///			/* log anything else */
/*TODO*///			case 0x00:
/*TODO*///			default:
/*TODO*///				if (oldword != newword)
/*TODO*///					logerror("vc_w(%02X, %04X) ** [prev=%04X]\n", offset, newword, oldword);
/*TODO*///				break;
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		atarivc_r: Handles an I/O read from the video controller.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	READ16_HANDLER( atarivc_r )
/*TODO*///	{
/*TODO*///		logerror("vc_r(%02X)\n", offset);
/*TODO*///	
/*TODO*///		/* a read from offset 0 returns the current scanline */
/*TODO*///		/* also sets bit 0x4000 if we're in VBLANK */
/*TODO*///		if (offset == 0)
/*TODO*///		{
/*TODO*///			int result = cpu_getscanline();
/*TODO*///	
/*TODO*///			if (result > 255)
/*TODO*///				result = 255;
/*TODO*///			if (result > Machine->visible_area.max_y)
/*TODO*///				result |= 0x4000;
/*TODO*///	
/*TODO*///			return result;
/*TODO*///		}
/*TODO*///		else
/*TODO*///			return atarivc_data[offset];
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	/*##########################################################################
/*TODO*///		VIDEO HELPERS
/*TODO*///	##########################################################################*/
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		atarigen_get_hblank: Returns a guesstimate about the current
/*TODO*///		HBLANK state, based on the assumption that HBLANK represents
/*TODO*///		10% of the scanline period.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	int atarigen_get_hblank(void)
/*TODO*///	{
/*TODO*///		return (cpu_gethorzbeampos() > (Machine->drv->screen_width * 9 / 10));
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		atarigen_halt_until_hblank_0_w: Halts CPU 0 until the
/*TODO*///		next HBLANK.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	WRITE16_HANDLER( atarigen_halt_until_hblank_0_w )
/*TODO*///	{
/*TODO*///		/* halt the CPU until the next HBLANK */
/*TODO*///		int hpos = cpu_gethorzbeampos();
/*TODO*///		int hblank = Machine->drv->screen_width * 9 / 10;
/*TODO*///		double fraction;
/*TODO*///	
/*TODO*///		/* if we're in hblank, set up for the next one */
/*TODO*///		if (hpos >= hblank)
/*TODO*///			hblank += Machine->drv->screen_width;
/*TODO*///	
/*TODO*///		/* halt and set a timer to wake up */
/*TODO*///		fraction = (double)(hblank - hpos) / (double)Machine->drv->screen_width;
/*TODO*///		timer_set(cpu_getscanlineperiod() * fraction, 0, unhalt_cpu);
/*TODO*///		cpu_set_halt_line(0, ASSERT_LINE);
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		atarigen_666_paletteram_w: 6-6-6 RGB palette RAM handler.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	WRITE16_HANDLER( atarigen_666_paletteram_w )
/*TODO*///	{
/*TODO*///		int newword, r, g, b;
/*TODO*///	
/*TODO*///		COMBINE_DATA(&paletteram16[offset]);
/*TODO*///		newword = paletteram16[offset];
/*TODO*///	
/*TODO*///		r = ((newword >> 9) & 0x3e) | ((newword >> 15) & 1);
/*TODO*///		g = ((newword >> 4) & 0x3e) | ((newword >> 15) & 1);
/*TODO*///		b = ((newword << 1) & 0x3e) | ((newword >> 15) & 1);
/*TODO*///	
/*TODO*///		r = (r << 2) | (r >> 4);
/*TODO*///		g = (g << 2) | (g >> 4);
/*TODO*///		b = (b << 2) | (b >> 4);
/*TODO*///	
/*TODO*///		palette_set_color(offset, r, g, b);
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		atarigen_expanded_666_paletteram_w: 6-6-6 RGB expanded
/*TODO*///		palette RAM handler.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	WRITE16_HANDLER( atarigen_expanded_666_paletteram_w )
/*TODO*///	{
/*TODO*///		COMBINE_DATA(&paletteram16[offset]);
/*TODO*///	
/*TODO*///		if (ACCESSING_MSB)
/*TODO*///		{
/*TODO*///			int palentry = offset / 2;
/*TODO*///			int newword = (paletteram16[palentry * 2] & 0xff00) | (paletteram16[palentry * 2 + 1] >> 8);
/*TODO*///	
/*TODO*///			int r, g, b;
/*TODO*///	
/*TODO*///			r = ((newword >> 9) & 0x3e) | ((newword >> 15) & 1);
/*TODO*///			g = ((newword >> 4) & 0x3e) | ((newword >> 15) & 1);
/*TODO*///			b = ((newword << 1) & 0x3e) | ((newword >> 15) & 1);
/*TODO*///	
/*TODO*///			r = (r << 2) | (r >> 4);
/*TODO*///			g = (g << 2) | (g >> 4);
/*TODO*///			b = (b << 2) | (b >> 4);
/*TODO*///	
/*TODO*///			palette_set_color(palentry & 0x1ff, r, g, b);
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		atarigen_666_paletteram32_w: 6-6-6 RGB palette RAM handler.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	WRITE32_HANDLER( atarigen_666_paletteram32_w )
/*TODO*///	{
/*TODO*///		int newword, r, g, b;
/*TODO*///	
/*TODO*///		COMBINE_DATA(&paletteram32[offset]);
/*TODO*///	
/*TODO*///		if (ACCESSING_MSW32)
/*TODO*///		{
/*TODO*///			newword = paletteram32[offset] >> 16;
/*TODO*///	
/*TODO*///			r = ((newword >> 9) & 0x3e) | ((newword >> 15) & 1);
/*TODO*///			g = ((newword >> 4) & 0x3e) | ((newword >> 15) & 1);
/*TODO*///			b = ((newword << 1) & 0x3e) | ((newword >> 15) & 1);
/*TODO*///	
/*TODO*///			r = (r << 2) | (r >> 4);
/*TODO*///			g = (g << 2) | (g >> 4);
/*TODO*///			b = (b << 2) | (b >> 4);
/*TODO*///	
/*TODO*///			palette_set_color(offset * 2, r, g, b);
/*TODO*///		}
/*TODO*///	
/*TODO*///		if (ACCESSING_LSW32)
/*TODO*///		{
/*TODO*///			newword = paletteram32[offset] & 0xffff;
/*TODO*///	
/*TODO*///			r = ((newword >> 9) & 0x3e) | ((newword >> 15) & 1);
/*TODO*///			g = ((newword >> 4) & 0x3e) | ((newword >> 15) & 1);
/*TODO*///			b = ((newword << 1) & 0x3e) | ((newword >> 15) & 1);
/*TODO*///	
/*TODO*///			r = (r << 2) | (r >> 4);
/*TODO*///			g = (g << 2) | (g >> 4);
/*TODO*///			b = (b << 2) | (b >> 4);
/*TODO*///	
/*TODO*///			palette_set_color(offset * 2 + 1, r, g, b);
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		unhalt_cpu: Timer callback to release the CPU from a halted state.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	static void unhalt_cpu(int param)
/*TODO*///	{
/*TODO*///		cpu_set_halt_line(param, CLEAR_LINE);
/*TODO*///	}
	
	
	
	/*##########################################################################
		MISC HELPERS
	##########################################################################*/
	
	/*---------------------------------------------------------------
		atarigen_invert_region: Inverts the bits in a region.
	---------------------------------------------------------------*/
	
	public static void atarigen_invert_region(int region)
	{
		int length = memory_region_length(region);
		UBytePtr base = new UBytePtr(memory_region(region));
	
		while (length-- != 0){
			base.writeinc(base.read() ^ 0xff );
                }
	}
	
	
/*TODO*///	void atarigen_swap_mem(void *ptr1, void *ptr2, int bytes)
/*TODO*///	{
/*TODO*///		UINT8 *p1 = ptr1;
/*TODO*///		UINT8 *p2 = ptr2;
/*TODO*///		while (bytes--)
/*TODO*///		{
/*TODO*///			int temp = *p1;
/*TODO*///			*p1++ = *p2;
/*TODO*///			*p2++ = temp;
/*TODO*///		}
/*TODO*///	}
}
