/***********************************************************************

	DECO Cassette System machine

 ***********************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.machine;

import static arcadeflex056.fucPtr.*;
import static common.ptr.*;
import static mame056.common.*;
import static mame056.commonH.*;
import static mame056.cpu.i8x41.i8x41H.*;
import static mame056.cpuexec.*;
import static mame056.cpuexecH.*;
import static mame056.cpuintrf.*;
import static mame056.cpuintrfH.*;
import static mame056.inptport.*;
import static mame056.memory.*;
import static mame056.memoryH.*;
import static mame056.sndintrf.*;
import static mame056.timer.*;
import static mame056.timerH.*;

import static mame056.usrintrf.usrintf_showmessage;
import static WIP.mame056.drivers.decocass.decocass_w;
import static arcadeflex056.osdepend.logerror;
import common.subArrays.IntArray;

public class decocass
{
	
	/* tape direction, speed and timing (used also in vidhrdw/decocass.c) */
	public static int tape_dir;
	public static int tape_speed;
	public static double tape_time0;
	public static timer_entry tape_timer;
	
	public static int firsttime = 1;
	public static int tape_present;
	public static int tape_blocks;
	public static int tape_length;
	public static int tape_bot_eot;
	public static int crc16_lsb;
	public static int crc16_msb;
	
	/* pre-calculated crc16 of the tape blocks */
	static int[] tape_crc16_lsb = new int[256];
	static int[] tape_crc16_msb = new int[256];
	
	static ReadHandlerPtr decocass_dongle_r;
	static WriteHandlerPtr decocass_dongle_w;
	
	static int decocass_reset;
	static int i8041_p1;
	static int i8041_p2;
	
	/* dongle type #1: jumpers C and D assignments */
	public static int MAKE_MAP(int m0, int m1, int m2, int m3, int m4, int m5, int m6, int m7){
		return ((m0)<<0)|((m1)<<4)|((m2)<<8)|((m3)<<12)|	
		((m4)<<16)|((m5)<<20)|((m6)<<24)|((m7)<<28);
        };
	public static int MAP0(int m){ return ((m>>0)&15); }
	public static int MAP1(int m){ return ((m>>4)&15); }
	public static int MAP2(int m){ return ((m>>8)&15); }
	public static int MAP3(int m){ return ((m>>12)&15); }
	public static int MAP4(int m){ return ((m>>16)&15); }
	public static int MAP5(int m){ return ((m>>20)&15); }
	public static int MAP6(int m){ return ((m>>24)&15); }
	public static int MAP7(int m){ return ((m>>28)&15); }
	
        static int type1_inmap;
	static int type1_outmap;
	
	/* dongle type #2: status of the latches */
	static int type2_d2_latch;	/* latched 8041-STATUS D2 value */
	static int type2_xx_latch;	/* latched value (D7-4 == 0xc0) ? 1 : 0 */
	static int type2_promaddr;	/* latched PROM address A0-A7 */
	
	/* dongle type #3: status and patches */
	static int type3_ctrs;			/* 12 bit counter stage */
	static int type3_d0_latch;		/* latched 8041-D0 value */
	static int type3_pal_19;		/* latched 1 for PAL input pin-19 */
	static int type3_swap;
	
	static int TYPE3_SWAP_01 = 0;
	static int TYPE3_SWAP_12 = 1;
	static int TYPE3_SWAP_13 = 2;
	static int TYPE3_SWAP_24 = 3;
	static int TYPE3_SWAP_25 = 4;
	static int TYPE3_SWAP_34_0 = 5;
	static int TYPE3_SWAP_34_7 = 6;
	static int TYPE3_SWAP_56 = 7;
	static int TYPE3_SWAP_67 = 8;
		
	/* dongle type #4: status */
	static int type4_ctrs;			/* latched PROM address (E5x0 LSB, E5x1 MSB) */
	static int type4_latch; 		/* latched enable PROM (1100xxxx written to E5x1) */
	
	/* dongle type #5: status */
	static int type5_latch; 		/* latched enable PROM (1100xxxx written to E5x1) */
	
	/* four inputs from the quadrature decoder (H1, V1, H2, V2) */
	static int[] decocass_quadrature_decoder = new int[4];
	
	/* sound latches, ACK status bits and NMI timer */
	static int decocass_sound_ack;
	static timer_entry decocass_sound_timer;
	
	public static WriteHandlerPtr decocass_coin_counter_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	} };
	
	public static WriteHandlerPtr decocass_sound_command_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		logerror("CPU #%d sound command -> $%02x\n", cpu_getactivecpu(), data);
		soundlatch_w.handler(0,data);
		decocass_sound_ack |= 0x80;
		/* remove snd cpu data ack bit. i don't see it in the schems, but... */
		decocass_sound_ack &= ~0x40;
		cpu_set_irq_line(1, M6502_INT_IRQ, ASSERT_LINE);
	} };
	
	public static ReadHandlerPtr decocass_sound_data_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int data = soundlatch2_r.handler(0);
		logerror("CPU #%d sound data    <- $%02x\n", cpu_getactivecpu(), data);
		return data;
	} };
	
	public static ReadHandlerPtr decocass_sound_ack_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int data = decocass_sound_ack;	/* D6+D7 */
		logerror("CPU #%d sound ack     <- $%02x\n", cpu_getactivecpu(), data);
		return data;
	} };
	
	public static WriteHandlerPtr decocass_sound_data_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		logerror("CPU #%d sound data    -> $%02x\n", cpu_getactivecpu(), data);
		soundlatch2_w.handler(0, data);
		decocass_sound_ack |= 0x40;
	} };
	
	public static ReadHandlerPtr decocass_sound_command_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int data = soundlatch_r.handler(0);
		logerror("CPU #%d sound command <- $%02x\n", cpu_getactivecpu(), data);
		cpu_set_irq_line(1, M6502_INT_IRQ, CLEAR_LINE);
		decocass_sound_ack &= ~0x80;
		return data;
	} };
	
	static timer_callback decocass_sound_nmi_pulse = new timer_callback() {
            public void handler(int param) {
                cpu_set_nmi_line(1, PULSE_LINE);
            }
        };
	
	public static WriteHandlerPtr decocass_sound_nmi_enable_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		logerror("CPU #%d sound NMI enb -> $%02x\n", cpu_getactivecpu(), data);
		if (null == decocass_sound_timer)
			decocass_sound_timer = timer_pulse(TIME_IN_HZ(256 * 57 / 8 / 2), 0, decocass_sound_nmi_pulse);
	} };
	
	public static ReadHandlerPtr decocass_sound_nmi_enable_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int data = 0xff;
		logerror("CPU #%d sound NMI enb <- $%02x\n", cpu_getactivecpu(), data);
		if (null == decocass_sound_timer)
			decocass_sound_timer = timer_pulse(TIME_IN_HZ(256 * 57 / 8 / 2), 0, decocass_sound_nmi_pulse);
		return data;
	} };
	
	public static ReadHandlerPtr decocass_sound_data_ack_reset_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int data = 0xff;
		logerror("CPU #%d sound ack rst <- $%02x\n", cpu_getactivecpu(), data);
		decocass_sound_ack &= ~0x40;
		return data;
	} };
	
	public static WriteHandlerPtr decocass_sound_data_ack_reset_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		logerror("CPU #%d sound ack rst -> $%02x\n", cpu_getactivecpu(), data);
		decocass_sound_ack &= ~0x40;
	} };
	
	public static WriteHandlerPtr decocass_nmi_reset_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		cpu_set_nmi_line( 0, CLEAR_LINE );
	} };
	
	public static WriteHandlerPtr decocass_quadrature_decoder_reset_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* just latch the analog controls here */
		decocass_quadrature_decoder[0] = input_port_3_r.handler(0);
		decocass_quadrature_decoder[1] = input_port_4_r.handler(0);
		decocass_quadrature_decoder[2] = input_port_5_r.handler(0);
		decocass_quadrature_decoder[3] = input_port_6_r.handler(0);
	} };
	
	public static WriteHandlerPtr decocass_adc_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	} };
	
	/*
	 * E6x0    inputs
	 * E6x1    inputs
	 * E6x2    coin inp
	 * E6x3    quadrature decoder read
	 * E6x4    ""
	 * E6x5    ""
	 * E6x6    ""
	 * E6x7    a/d converter read
	 */
	public static ReadHandlerPtr decocass_input_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int data = 0xff;
		switch (offset & 7)
		{
		case 0: case 1: case 2:
			data = readinputport(offset & 7);
			break;
		case 3: case 4: case 5: case 6:
			data = decocass_quadrature_decoder[(offset & 7) - 3];
			break;
		default:
			break;
		}
	
		return data;
	} };
	
	/*
	 * D0 - REQ/ data request	  (8041 pin 34 port 1.7)
	 * D1 - FNO/ function number  (8041 pin 21 port 2.0)
	 * D2 - EOT/ end-of-tape	  (8041 pin 22 port 2.1)
	 * D3 - ERR/ error condition  (8041 pin 23 port 2.2)
	 * D4 - BOT-EOT from tape
	 * D5 -
	 * D6 -
	 * D7 - cassette present
	 */
	/* Note on a tapes leader-BOT-data-EOT-trailer format:
	 * A cassette has a transparent piece of tape on both ends,
	 * leader and trailer. And data tapes also have BOT and EOT
	 * holes, shortly before the the leader and trailer.
	 * The holes and clear tape are detected using a photo-resitor.
	 * When rewinding, the BOT/EOT signal will show a short
	 * pulse and if rewind continues a constant high signal later.
	 * The specs say the holes are "> 2ms" in length.
	 */
	
	public static final int TAPE_CLOCKRATE	= 4800;	/* clock pulses per second */
	
	/* duration of the clear LEADER (and trailer) of the tape */
	public static int TAPE_LEADER           = TAPE_CLOCKRATE;		/* 1s */
	/* duration of the GAP between leader and BOT/EOT */
	public static int TAPE_GAP		= TAPE_CLOCKRATE*3/2;	/* 1.5s */
	/* duration of BOT/EOT holes */
	public static int TAPE_HOLE		= TAPE_CLOCKRATE/400;	/* 0.0025s */
	
	/* byte offset of the tape chunks (8 clocks per byte = 16 samples) */
	/* 300 ms GAP between BOT and first data block (doesn't work.. thus /2) */
	public static int TAPE_PRE_GAP          = 34;
	public static int TAPE_LEADIN           = (TAPE_PRE_GAP + 1);
	public static int TAPE_HEADER           = (TAPE_LEADIN + 1);
	public static int TAPE_BLOCK		= (TAPE_HEADER + 256);
	public static int TAPE_CRC16_MSB	= (TAPE_BLOCK + 1);
	public static int TAPE_CRC16_LSB	= (TAPE_CRC16_MSB + 1);
	public static int TAPE_TRAILER          = (TAPE_CRC16_LSB + 1);
	public static int TAPE_LEADOUT          = (TAPE_TRAILER + 1);
	public static int TAPE_LONGCLOCK	= (TAPE_LEADOUT + 1);
	public static int TAPE_POST_GAP         = (TAPE_LONGCLOCK + 34);
	
	/* size of a tape chunk (block) including gaps */
	public static int TAPE_CHUNK		= TAPE_POST_GAP;
	
	public static int E5XX_MASK             = 0x02;	/* use 0x0e for old style board */
	
	public static int BIT0(int x){ return ((x)&1); }
	public static int BIT1(int x){ return (((x)>>1)&1); }
	public static int BIT2(int x){ return (((x)>>2)&1); }
	public static int BIT3(int x){ return (((x)>>3)&1); }
	public static int BIT4(int x){ return (((x)>>4)&1); }
	public static int BIT5(int x){ return (((x)>>5)&1); }
	public static int BIT6(int x){ return (((x)>>6)&1); }
	public static int BIT7(int x){ return (((x)>>7)&1); }
	
	public static WriteHandlerPtr decocass_reset_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		logerror("%9.7f 6502-PC: %04x decocass_reset_w(%02x): $%02x\n", timer_get_time(), cpu_getpreviouspc(), offset, data);
		decocass_reset = data;
	
		/* CPU #1 active hight reset */
		cpu_set_reset_line( 1, data & 0x01 );
	
		/* on reset also remove the sound timer */
		if ((data & 1)!=0 && decocass_sound_timer!=null)
		{
			timer_remove(decocass_sound_timer);
			decocass_sound_timer = null;
		}
	
		/* 8041 active low reset */
		cpu_set_reset_line( 2, (data & 0x08) ^ 0x08 );
	} };
	
	static String dirnm(int speed)
	{
		if (speed <  -1) return "fast rewind";
		if (speed == -1) return "rewind";
		if (speed ==  0) return "stop";
		if (speed ==  1) return "forward";
		return "fast forward";
	}
	
	static void tape_crc16(int data)
	{
		int c0, c1;
		int old_lsb = crc16_lsb;
		int old_msb = crc16_msb;
		int feedback;
	
		feedback = ((data >> 7) ^ crc16_msb) & 1;
	
		/* rotate 16 bits */
		c0 = crc16_lsb & 1;
		c1 = crc16_msb & 1;
		crc16_msb = (crc16_msb >> 1) | (c0 << 7);
		crc16_lsb = (crc16_lsb >> 1) | (c1 << 7);
	
		/* feedback into bit 7 */
		if (feedback != 0)
			crc16_lsb |= 0x80;
		else
			crc16_lsb &= ~0x80;
	
		/* feedback to bit 6 into bit 5 */
		if ((((old_lsb >> 6) ^ feedback) & 1) != 0 )
			crc16_lsb |= 0x20;
		else
			crc16_lsb &= ~0x20;
	
		/* feedback to bit 1 into bit 0 */
		if ((((old_msb >> 1) ^ feedback) & 1) != 0)
			crc16_msb |= 0x01;
		else
			crc16_msb &= ~0x01;
	}
        
        static int last_byte;
	
	static void tape_update()
	{
		
		double tape_time = tape_time0;
		int offset, rclk, rdata, tape_bit, tape_byte, tape_block;
	
		if (tape_timer != null)
			tape_time += tape_dir * timer_timeelapsed(tape_timer);
	
		if (tape_time < 0.0)
			tape_time = 0.0;
		else if (tape_time > 999.9)
			tape_time = 999.9;
	
		offset = (int)(tape_time * TAPE_CLOCKRATE + 0.499995);
	
		/* reset RCLK and RDATA inputs */
		rclk = 0;
		rdata = 0;
	
		if (offset < TAPE_LEADER)
		{
			if (offset < 0)
				offset = 0;
			/* LEADER area */
			if (0 == tape_bot_eot)
			{
				tape_bot_eot = 1;
				/*TODO*///set_led_status(1, 1);
				logerror("tape %5.4fs: %s found LEADER\n", tape_time, dirnm(tape_dir));
			}
		}
		else
		if (offset < TAPE_LEADER + TAPE_GAP)
		{
			/* GAP between LEADER and BOT hole */
			if (1 == tape_bot_eot)
			{
				tape_bot_eot = 0;
				//set_led_status(1, 0);
				logerror("tape %5.4fs: %s between BOT + LEADER\n", tape_time, dirnm(tape_dir));
			}
		}
		else
		if (offset < TAPE_LEADER + TAPE_GAP + TAPE_HOLE)
		{
			/* during BOT hole */
			if (0 == tape_bot_eot)
			{
				tape_bot_eot = 1;
				//set_led_status(1, 1);
				logerror("tape %5.4fs: %s found BOT\n", tape_time, dirnm(tape_dir));
			}
		}
		else
		if (offset < tape_length - TAPE_LEADER - TAPE_GAP - TAPE_HOLE)
		{
			offset -= TAPE_LEADER + TAPE_GAP + TAPE_HOLE;
	
			/* data area */
			if (1 == tape_bot_eot)
			{
				tape_bot_eot = 0;
				//set_led_status(1, 0);
				logerror("tape %5.4fs: %s data area\n", tape_time, dirnm(tape_dir));
			}
			rclk = (offset ^ 1) & 1;
			tape_bit = (offset / 2) % 8;
			tape_byte = (offset / 16) % TAPE_CHUNK;
			tape_block = offset / 16 / TAPE_CHUNK;
	
			if (tape_byte < TAPE_PRE_GAP)
			{
				rclk = 0;
				rdata = 0;
			}
			else
			if (tape_byte < TAPE_LEADIN)
			{
				rdata = (0x00 >> tape_bit) & 1;
				if (tape_byte != last_byte)
				{
					logerror("tape %5.4fs: LEADIN $00\n", tape_time);
					//set_led_status(2, 1);
				}
			}
			else
			if (tape_byte < TAPE_HEADER)
			{
				rdata = (0xaa >> tape_bit) & 1;
				if (tape_byte != last_byte){
					logerror("tape %5.4fs: HEADER $aa\n", tape_time);
                                }
			}
			else
			if (tape_byte < TAPE_BLOCK)
			{
				UShortPtr ptr = new UShortPtr(memory_region(REGION_USER2), tape_block * 256 + tape_byte - TAPE_HEADER);
				rdata = (ptr.read() >> tape_bit) & 1;
				if (tape_byte != last_byte){
					logerror("tape %5.4fs: DATA(%02x) $%02x\n", tape_time, tape_byte - TAPE_HEADER, ptr.read());
                                }
			}
			else
			if (tape_byte < TAPE_CRC16_MSB)
			{
				rdata = (tape_crc16_msb[tape_block] >> tape_bit) & 1;
				if (tape_byte != last_byte){
					logerror("tape %5.4fs: CRC16 MSB $%02x\n", tape_time, tape_crc16_msb[tape_block]);
                                }
			}
			else
			if (tape_byte < TAPE_CRC16_LSB)
			{
				rdata = (tape_crc16_lsb[tape_block] >> tape_bit) & 1;
				if (tape_byte != last_byte){
					logerror("tape %5.4fs: CRC16 LSB $%02x\n", tape_time, tape_crc16_lsb[tape_block]);
                                }
			}
			else
			if (tape_byte < TAPE_TRAILER)
			{
				rdata = (0xaa >> tape_bit) & 1;
				if (tape_byte != last_byte){
					logerror("tape %5.4fs: TRAILER $aa\n", tape_time);
                                }
			}
			else
			if (tape_byte < TAPE_LEADOUT)
			{
				rdata = (0x00 >> tape_bit) & 1;
				if (tape_byte != last_byte){
					logerror("tape %5.4fs: LEADOUT $00\n", tape_time);
                                }
			}
			else
			if (tape_byte < TAPE_LONGCLOCK)
			{
				if (tape_byte != last_byte)
				{
					logerror("tape %5.4fs: LONG CLOCK\n", tape_time);
					//set_led_status(2, 0);
				}
				rclk = 1;
				rdata = 0;
			}
			last_byte = tape_byte;
		}
		else
		if (offset < tape_length - TAPE_LEADER - TAPE_GAP)
		{
			/* during EOT hole */
			if (0 == tape_bot_eot)
			{
				tape_bot_eot = 1;
				//set_led_status(1, 1);
				logerror("tape %5.4fs: %s found EOT\n", tape_time, dirnm(tape_dir));
			}
		}
		else
		if (offset < tape_length - TAPE_LEADER)
		{
			/* GAP between EOT and trailer */
			if (1 == tape_bot_eot)
			{
				tape_bot_eot = 0;
				//set_led_status(1, 0);
				logerror("tape %5.4fs: %s EOT and TRAILER\n", tape_time, dirnm(tape_dir));
			}
		}
		else
		{
			/* TRAILER area */
			if (0 == tape_bot_eot)
			{
				tape_bot_eot = 1;
				//set_led_status(1, 1);
				logerror("tape %5.4fs: %s found TRAILER\n", tape_time, dirnm(tape_dir));
			}
			offset = tape_length - 1;
		}
	
		i8041_p2 = (i8041_p2 & ~0xe0) | (tape_bot_eot << 5) | (rclk << 6) | (rdata << 7);
	}
	
	/*TODO*///#ifdef MAME_DEBUG
	/*TODO*///static void decocass_fno(offs_t offset, int data)
	/*TODO*///{
	/*TODO*///		/* 8041ENA/ and is this a FNO write (function number)? */
	/*TODO*///		if (0 == (i8041_p2 & 0x01))
	/*TODO*///		{
	/*TODO*///			switch (data)
	/*TODO*///			{
	/*TODO*///			case 0x25: logerror("8041 FNO 25: write_block\n"); break;
	/*TODO*///			case 0x26: logerror("8041 FNO 26: rewind_block\n"); break;
	/*TODO*///			case 0x27: logerror("8041 FNO 27: read_block_a\n"); break;
	/*TODO*///			case 0x28: logerror("8041 FNO 28: read_block_b\n"); break;
	/*TODO*///			case 0x29: logerror("8041 FNO 29: tape_rewind_fast\n"); break;
	/*TODO*///			case 0x2a: logerror("8041 FNO 2a: tape_forward\n"); break;
	/*TODO*///			case 0x2b: logerror("8041 FNO 2b: tape_rewind\n"); break;
	/*TODO*///			case 0x2c: logerror("8041 FNO 2c: force_abort\n"); break;
	/*TODO*///			case 0x2d: logerror("8041 FNO 2d: tape_erase\n"); break;
	/*TODO*///			case 0x2e: logerror("8041 FNO 2e: search_tape_mark\n"); break;
	/*TODO*///			case 0x2f: logerror("8041 FNO 2f: search_eot\n"); break;
	/*TODO*///			case 0x30: logerror("8041 FNO 30: advance_block\n"); break;
	/*TODO*///			case 0x31: logerror("8041 FNO 31: write_tape_mark\n"); break;
	/*TODO*///			case 0x32: logerror("8041 FNO 32: reset_error\n"); break;
	/*TODO*///			case 0x33: logerror("8041 FNO 33: flag_status_report\n"); break;
	/*TODO*///			case 0x34: logerror("8041 FNO 34: report_status_to_main\n"); break;
	/*TODO*///			default:   logerror("8041 FNO %02x: invalid\n", data);
	/*TODO*///			}
	/*TODO*///		}
	/*TODO*///}
	/*TODO*///#endif
	
	/***************************************************************************
	 *
	 *	TYPE1 DONGLE (DE-0061)
	 *	- Test Tape
	 *	- Lock 'n Chase
	 *	- Treasure Island
	 *	- Super Astro Fighter
	 *	- Lucky Poker
	 *	- Terranian
	 *	- Explorer
	 *	- Pro Golf
	 *
	 ***************************************************************************/
	static int latch1;
        
	public static ReadHandlerPtr decocass_type1_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		
		int data;
	
		if (1 == (offset & 1))
		{
			if (0 == (offset & E5XX_MASK))
				data = cpunum_get_reg(2, I8X41_STAT);
			else
				data = 0xff;
	
			data =
				(BIT0(data) << 0) |
				(BIT1(data) << 1) |
				(1			<< 2) |
				(1			<< 3) |
				(1			<< 4) |
				(1			<< 5) |
				(1			<< 6) |
				(0			<< 7);
			logerror("%9.7f 6502-PC: %04x decocass_type1_r(%02x): $%02x <- (%s %s)\n",
				timer_get_time(), cpu_getpreviouspc(), offset, data,
				(data & 1)!=0 ? "OBF" : "-",
				(data & 2)!=0 ? "IBF" : "-");
		}
		else
		{
			int promaddr;
			int save;
			UBytePtr prom = new UBytePtr(memory_region(REGION_USER1));
	
			if (firsttime!=0)
			{
				logerror("prom data:\n");
				for (promaddr = 0; promaddr < 32; promaddr++)
				{
					if (promaddr % 8 == 0)
						logerror("%04x:", promaddr);
					logerror(" %02x%s", prom.read(promaddr), (promaddr % 8) == 7 ? "\n" : "");
				}
				firsttime = 0;
				latch1 = 0; 	 /* reset latch (??) */
			}
	
			if (0 == (offset & E5XX_MASK))
				data = cpunum_get_reg(2, I8X41_DATA);
			else
				data = 0xff;
	
			save = data;	/* save the unmodifed data for the latch */
	
			promaddr =
				(((data >> MAP0(type1_inmap)) & 1) << 0) |
				(((data >> MAP1(type1_inmap)) & 1) << 1) |
				(((data >> MAP4(type1_inmap)) & 1) << 2) |
				(((data >> MAP5(type1_inmap)) & 1) << 3) |
				(((data >> MAP7(type1_inmap)) & 1) << 4);
	
			data =
				(((prom.read(promaddr) >> 0) & 1)			   << MAP0(type1_outmap)) |
				(((prom.read(promaddr) >> 1) & 1)			   << MAP1(type1_outmap)) |
				((1 - ((latch1 >> MAP2(type1_inmap)) & 1)) << MAP2(type1_outmap)) |
				(((data >> MAP3(type1_inmap)) & 1)		   << MAP3(type1_outmap)) |
				(((prom.read(promaddr) >> 2) & 1)			   << MAP4(type1_outmap)) |
				(((prom.read(promaddr) >> 3) & 1)			   << MAP5(type1_outmap)) |
				(((latch1 >> MAP6(type1_inmap)) & 1)	   << MAP6(type1_outmap)) |
				(((prom.read(promaddr) >> 4) & 1)			   << MAP7(type1_outmap));
	
			logerror("%9.7f 6502-PC: %04x decocass_type1_r(%02x): $%02x <- (%s $%02x mangled with PROM[$%02x])\n", timer_get_time(), cpu_getpreviouspc(), offset, data, 0 == (offset & E5XX_MASK) ? "8041-DATA" : "open bus", save, promaddr);
	
			latch1 = save;		/* latch the data for the next A0 == 0 read */
		}
		return data;
	} };
	
	/*
	 * special handler for the test tape, because we cannot
	 * look inside the dongle :-/
	 * There seem to be lines 1, 3 and 6 straight through.
	 * The rest could be translated with the standard Type1 dongle
	 * PROM, but I don't know. For now we have found this lookup
	 * table by applying data to the dongle and logging the outputs.
	 */
	
	public static ReadHandlerPtr decocass_type1_map1_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int map[] = {
			0x01,0x34,0x03,0x36,0xa4,0x15,0xa6,0x17,
			0x09,0x3c,0x0b,0x3e,0xac,0x1d,0xae,0x1f,
			0x90,0x14,0x92,0x16,0x85,0x00,0x87,0x02,
			0x98,0x1c,0x9a,0x1e,0x8d,0x08,0x8f,0x0a,
			0x31,0x30,0x33,0x32,0xa1,0x11,0xa3,0x13,
			0x39,0x38,0x3b,0x3a,0xa9,0x19,0xab,0x1b,
			0x84,0xb5,0x86,0xb7,0x81,0xb4,0x83,0xb6,
			0x8c,0xbd,0x8e,0xbf,0x89,0xbc,0x8b,0xbe,
			0x41,0x74,0x43,0x76,0xe4,0x55,0xe6,0x57,
			0x49,0x7c,0x4b,0x7e,0xec,0x5d,0xee,0x5f,
			0xd0,0x54,0xd2,0x56,0xc5,0x40,0xc7,0x42,
			0xd8,0x5c,0xda,0x5e,0xcd,0x48,0xcf,0x4a,
			0x71,0x70,0x73,0x72,0xe1,0x51,0xe3,0x53,
			0x79,0x78,0x7b,0x7a,0xe9,0x59,0xeb,0x5b,
			0xc4,0xf5,0xc6,0xf7,0xc1,0xf4,0xc3,0xf6,
			0xcc,0xfd,0xce,0xff,0xc9,0xfc,0xcb,0xfe,
			0x25,0xa0,0x27,0xa2,0x95,0x10,0x97,0x12,
			0x2d,0xa8,0x2f,0xaa,0x9d,0x18,0x9f,0x1a,
			0x80,0xb1,0x82,0xb3,0x24,0xb0,0x26,0xb2,
			0x88,0xb9,0x8a,0xbb,0x2c,0xb8,0x2e,0xba,
			0x21,0x94,0x23,0x96,0x05,0x04,0x07,0x06,
			0x29,0x9c,0x2b,0x9e,0x0d,0x0c,0x0f,0x0e,
			0x35,0xa5,0x37,0xa7,0x20,0x91,0x22,0x93,
			0x3d,0xad,0x3f,0xaf,0x28,0x99,0x2a,0x9b,
			0x65,0xe0,0x67,0xe2,0xd5,0x50,0xd7,0x52,
			0x6d,0xe8,0x6f,0xea,0xdd,0x58,0xdf,0x5a,
			0xc0,0xf1,0xc2,0xf3,0x64,0xf0,0x66,0xf2,
			0xc8,0xf9,0xca,0xfb,0x6c,0xf8,0x6e,0xfa,
			0x61,0xd4,0x63,0xd6,0x45,0x44,0x47,0x46,
			0x69,0xdc,0x6b,0xde,0x4d,0x4c,0x4f,0x4e,
			0x75,0xe5,0x77,0xe7,0x60,0xd1,0x62,0xd3,
			0x7d,0xed,0x7f,0xef,0x68,0xd9,0x6a,0xdb
		};
		int save, data;
	
		if (1 == (offset & 1))
		{
			if (0 == (offset & E5XX_MASK))
				data = cpunum_get_reg(2, I8X41_STAT);
			else
				data = 0xff;
	
			data =
				(BIT0(data) << 0) |
				(BIT1(data) << 1) |
				(1			<< 2) |
				(1			<< 3) |
				(1			<< 4) |
				(1			<< 5) |
				(1			<< 6) |
				(0			<< 7);
			logerror("%9.7f 6502-PC: %04x decocass_type1_r(%02x): $%02x <- (%s %s)\n",
				timer_get_time(), cpu_getpreviouspc(), offset, data,
				(data & 1)!=0 ? "OBF" : "-",
				(data & 2)!=0 ? "IBF" : "-");
		}
		else
		{
			if (0 == (offset & E5XX_MASK))
				save = cpunum_get_reg(2, I8X41_DATA);
			else
				save = 0xff;
	
			data = map[save];
	
			logerror("%9.7f 6502-PC: %04x decocass_type1_r(%02x): $%02x '%c' <- map[%02x] (%s)\n", timer_get_time(), cpu_getpreviouspc(), offset, data, (data >= 32) ? data : '.', save, 0 == (offset & E5XX_MASK) ? "8041-DATA" : "open bus");
		}
		return data;
	} };
	
        static int latch2;
        
	public static ReadHandlerPtr decocass_type1_map2_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int map[] = {
	/* 00 */0x06,0x1f,0x8f,0x0c,0x02,0x1b,0x8b,0x08,
			0x1e,0x1d,0x8e,0x16,0x1a,0x19,0x8a,0x12,
			0x95,0x17,0x94,0x05,0x91,0x13,0x90,0x01,
			0x87,0x04,0x86,0x9f,0x83,0x00,0x82,0x9b,
	/* 20 */0x26,0x3f,0xaf,0x2c,0x22,0x3b,0xab,0x28,
			0x3e,0x3d,0xae,0x36,0x3a,0x39,0xaa,0x32,
			0xb5,0x37,0xb4,0x25,0xb1,0x33,0xb0,0x21,
			0xa7,0x24,0xa6,0xbf,0xa3,0x20,0xa2,0xbb,
	/* 40 */0x46,0x5f,0xcf,0x4c,0x42,0x5b,0xcb,0x48,
			0x5e,0x5d,0xce,0x56,0x5a,0x59,0xca,0x52,
			0xd5,0x57,0xd4,0x45,0xd1,0x53,0xd0,0x41,
			0xc7,0x44,0xc6,0xdf,0xc3,0x40,0xc2,0xdb,
	/* 60 */0x66,0x7f,0xef,0x6c,0x62,0x7b,0xeb,0x68,
			0x7e,0x7d,0xee,0x76,0x7a,0x79,0xea,0x72,
			0xf5,0x77,0xf4,0x65,0xf1,0x73,0xf0,0x61,
			0xe7,0x64,0xe6,0xff,0xe3,0x60,0xe2,0xfb,
	/* 80 */0x1c,0x8d,0x8c,0x15,0x18,0x89,0x88,0x11,
			0x0e,0x97,0x14,0x07,0x0a,0x93,0x10,0x03,
			0x85,0x9e,0x0f,0x9d,0x81,0x9a,0x0b,0x99,
			0x84,0x9c,0x0d,0x96,0x80,0x98,0x09,0x92,
	/* a0 */0x3c,0xad,0xac,0x35,0x38,0xa9,0xa8,0x31,
			0x2e,0xb7,0x34,0x27,0x2a,0xb3,0x30,0x23,
			0xa5,0xbe,0x2f,0xbd,0xa1,0xba,0x2b,0xb9,
			0xa4,0xbc,0x2d,0xb6,0xa0,0xb8,0x29,0xb2,
	/* c0 */0x5c,0xcd,0xcc,0x55,0x58,0xc9,0xc8,0x51,
			0x4e,0xd7,0x54,0x47,0x4a,0xd3,0x50,0x43,
			0xc5,0xde,0x4f,0xdd,0xc1,0xda,0x4b,0xd9,
			0xc4,0xdc,0x4d,0xd6,0xc0,0xd8,0x49,0xd2,
	/* e0 */0x7c,0xed,0xec,0x75,0x78,0xe9,0xe8,0x71,
			0x6e,0xf7,0x74,0x67,0x6a,0xf3,0x70,0x63,
			0xe5,0xfe,0x6f,0xfd,0xe1,0xfa,0x6b,0xf9,
			0xe4,0xfc,0x6d,0xf6,0xe0,0xf8,0x69,0xf2
		};
		
		int save, addr, data;
	
		/* read from tape:
		 *	7d 43 5d 4f 04 ae e3 59 57 cb d6 55 4d 15
		 * should become:
		 *	?? 48 44 52 42 30 31 44 45 43 4f 53 59 53
		 * lookup entries with above values:
		 *	?? 47 59 4f 44 ae a7 59 53 cf d2 55 4d 55
		 * difference:
		 *	   04 04 00 40 00 44 00 04 04 04 00 00 40
		 */
	
		if (1 == (offset & 1))
		{
			if (0 == (offset & E5XX_MASK))
				data = cpunum_get_reg(2, I8X41_STAT);
			else
				data = 0xff;
	
			data =
				(BIT0(data) << 0) |
				(BIT1(data) << 1) |
				(1			<< 2) |
				(1			<< 3) |
				(1			<< 4) |
				(1			<< 5) |
				(1			<< 6) |
				(0			<< 7);
			logerror("%9.7f 6502-PC: %04x decocass_type1_r(%02x): $%02x <- (%s %s)\n",
				timer_get_time(), cpu_getpreviouspc(), offset, data,
				(data & 1)!=0 ? "OBF" : "-",
				(data & 2)!=0 ? "IBF" : "-");
		}
		else
		{
			if (0 == (offset & E5XX_MASK))
				save = cpunum_get_reg(2, I8X41_DATA);
			else
				save = 0xff;
	
			addr = (save & ~0x44) | (latch2 & 0x44);
			data = map[addr];
	
			logerror("%9.7f 6502-PC: %04x decocass_type1_r(%02x): $%02x '%c' <- map[%02x = %02x^((%02x^%02x)&%02x)] (%s)\n", timer_get_time(), cpu_getpreviouspc(), offset, data, (data >= 32) ? data : '.', addr, save, latch2, save, 0x44, 0 == (offset & E5XX_MASK) ? "8041-DATA" : "open bus");
			latch2 = save;
		}
		return data;
	} };
        
        static int latch3;
	
	public static ReadHandlerPtr decocass_type1_map3_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int map[] = {
	/* 00 */0x03,0x36,0x01,0x34,0xa6,0x17,0xa4,0x15,
			0x0b,0x3e,0x09,0x3c,0xae,0x1f,0xac,0x1d,
			0x92,0x16,0x90,0x14,0x87,0x02,0x85,0x00,
			0x9a,0x1e,0x98,0x1c,0x8f,0x0a,0x8d,0x08,
	/* 20 */0x33,0x32,0x31,0x30,0xa3,0x13,0xa1,0x11,
			0x3b,0x3a,0x39,0x38,0xab,0x1b,0xa9,0x19,
			0x86,0xb7,0x84,0xb5,0x83,0xb6,0x81,0xb4,
			0x8e,0xbf,0x8c,0xbd,0x8b,0xbe,0x89,0xbc,
	/* 40 */0x43,0x76,0x41,0x74,0xe6,0x57,0xe4,0x55,
			0x4b,0x7e,0x49,0x7c,0xee,0x5f,0xec,0x5d,
			0xd2,0x56,0xd0,0x54,0xc7,0x42,0xc5,0x40,
			0xda,0x5e,0xd8,0x5c,0xcf,0x4a,0xcd,0x48,
	/* 60 */0x73,0x72,0x71,0x70,0xe3,0x53,0xe1,0x51,
			0x7b,0x7a,0x79,0x78,0xeb,0x5b,0xe9,0x59,
			0xc6,0xf7,0xc4,0xf5,0xc3,0xf6,0xc1,0xf4,
			0xce,0xff,0xcc,0xfd,0xcb,0xfe,0xc9,0xfc,
	/* 80 */0x27,0xa2,0x25,0xa0,0x97,0x12,0x95,0x10,
			0x2f,0xaa,0x2d,0xa8,0x9f,0x1a,0x9d,0x18,
			0x82,0xb3,0x80,0xb1,0x26,0xb2,0x24,0xb0,
			0x8a,0xbb,0x88,0xb9,0x2e,0xba,0x2c,0xb8,
	/* a0 */0x23,0x96,0x21,0x94,0x07,0x06,0x05,0x04,
			0x2b,0x9e,0x29,0x9c,0x0f,0x0e,0x0d,0x0c,
			0x37,0xa7,0x35,0xa5,0x22,0x93,0x20,0x91,
			0x3f,0xaf,0x3d,0xad,0x2a,0x9b,0x28,0x99,
	/* c0 */0x67,0xe2,0x65,0xe0,0xd7,0x52,0xd5,0x50,
			0x6f,0xea,0x6d,0xe8,0xdf,0x5a,0xdd,0x58,
			0xc2,0xf3,0xc0,0xf1,0x66,0xf2,0x64,0xf0,
			0xca,0xfb,0xc8,0xf9,0x6e,0xfa,0x6c,0xf8,
	/* e0 */0x63,0xd6,0x61,0xd4,0x47,0x46,0x45,0x44,
			0x6b,0xde,0x69,0xdc,0x4f,0x4e,0x4d,0x4c,
			0x77,0xe7,0x75,0xe5,0x62,0xd3,0x60,0xd1,
			0x7f,0xef,0x7d,0xed,0x6a,0xdb,0x68,0xd9
		};
		
		int save, addr, data;
	
		/* read from tape:
		 *	f6 5f e5 c5 17 23 62 40 67 51 c5 ee 85 23
		 * should become:
		 *	20 48 44 52 42 30 31 41 53 54 52 4f 50 32
		 * lookup entries with above values:
		 *	b6 5f e7 c5 55 23 22 42 65 53 c5 ec c7 21
		 * difference:
		 *	40 00 02 00 40 00 40 02 02 02 00 02 42 02
		 */
	
		if (1 == (offset & 1))
		{
			if (0 == (offset & E5XX_MASK))
				data = cpunum_get_reg(2, I8X41_STAT);
			else
				data = 0xff;
	
			data =
				(BIT0(data) << 0) |
				(BIT1(data) << 1) |
				(1			<< 2) |
				(1			<< 3) |
				(1			<< 4) |
				(1			<< 5) |
				(1			<< 6) |
				(0			<< 7);
			logerror("%9.7f 6502-PC: %04x decocass_type1_r(%02x): $%02x <- (%s %s)\n",
				timer_get_time(), cpu_getpreviouspc(), offset, data,
				(data & 1)!=0 ? "OBF" : "-",
				(data & 2)!=0 ? "IBF" : "-");
		}
		else
		{
	
			if (0 == (offset & E5XX_MASK))
				save = cpunum_get_reg(2, I8X41_DATA);
			else
				save = 0xff;
	
			addr = (save & ~0x42) | (latch3 & 0x42);
			data = map[addr];
	
			logerror("%9.7f 6502-PC: %04x decocass_type1_r(%02x): $%02x '%c' <- map[%02x = %02x^((%02x^%02x)&%02x)] (%s)\n", timer_get_time(), cpu_getpreviouspc(), offset, data, data >= 0x20 ? data : '.', addr, save, latch3, save, 0x42, 0 == (offset & E5XX_MASK) ? "8041-DATA" : "open bus");
			latch3 = save;
		}
		return data;
	} };
	
	/***************************************************************************
	 *
	 *	TYPE2 DONGLE (CS82-007)
	 *	- Mission X
	 *	- Disco No 1
	 *	- Pro Tennis
	 *	- Tornado
	 *
	 ***************************************************************************/
	public static ReadHandlerPtr decocass_type2_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int data;
	
		if (1 == type2_xx_latch)
		{
			if (1 == (offset & 1))
			{
				UBytePtr prom = new UBytePtr(memory_region(REGION_USER1));
				data = prom.read(256 * type2_d2_latch + type2_promaddr);
				logerror("%9.7f 6502-PC: %04x decocass_type2_r(%02x): $%02x <- prom[%03x]\n", timer_get_time(), cpu_getpreviouspc(), offset, data, 256 * type2_d2_latch + type2_promaddr);
			}
			else
			{
				data = 0xff;	/* floating input? */
			}
		}
		else
		{
			if (0 == (offset & E5XX_MASK))
				data = cpunum_get_reg(2, (offset & 1)!=0 ? I8X41_STAT : I8X41_DATA);
			else
				data = offset & 0xff;
	
			logerror("%9.7f 6502-PC: %04x decocass_type2_r(%02x): $%02x <- 8041-%s\n", timer_get_time(), cpu_getpreviouspc(), offset, data, (offset & 1)!=0 ? "STATUS" : "DATA");
		}
		return data;
	} };
	
	public static WriteHandlerPtr decocass_type2_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (1 == type2_xx_latch)
		{
			if (1 == (offset & 1))
			{
				logerror("%9.7f 6502-PC: %04x decocass_e5xx_w(%02x): $%02x -> set PROM+D2 latch $%02x", timer_get_time(), cpu_getpreviouspc(), offset, data);
			}
			else
			{
				type2_promaddr = data;
				logerror("%9.7f 6502-PC: %04x decocass_e5xx_w(%02x): $%02x -> set PROM addr $%02x\n", timer_get_time(), cpu_getpreviouspc(), offset, data, type2_promaddr);
				return;
			}
		}
		else
		{
			logerror("%9.7f 6502-PC: %04x decocass_e5xx_w(%02x): $%02x -> %s ", timer_get_time(), cpu_getpreviouspc(), offset, data, (offset & 1)!=0 ? "8041-CMND" : "8041 DATA");
		}
		if (1 == (offset & 1))
		{
			if (0xc0 == (data & 0xf0))
			{
				type2_xx_latch = 1;
				type2_d2_latch = (data & 0x04)!=0 ? 1 : 0;
				logerror("PROM:%s D2:%d", type2_xx_latch!=0 ? "on" : "off", type2_d2_latch);
			}
		}
		cpunum_set_reg(2, (offset & 1)!=0 ? I8X41_CMND : I8X41_DATA, data);
	
	/*TODO*///#ifdef MAME_DEBUG
	/*TODO*///	decocass_fno(offset, data);
	/*TODO*///#endif
	} };
	
	/***************************************************************************
	 *
	 *	TYPE3 DONGLE
	 *	- Bump 'n Jump
	 *	- Burnin' Rubber
	 *	- Burger Time
	 *	- Graplop
	 *	- Cluster Buster
	 *	- LaPaPa
	 *	- Fighting Ice Hockey
	 *	- Pro Bowling
	 *	- Night Star
	 *	- Pro Soccer
	 *	- Peter Pepper's Ice Cream Factory
	 *
	 ***************************************************************************/
	public static ReadHandlerPtr decocass_type3_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int data, save;
	
		if (1 == (offset & 1))
		{
			if (1 == type3_pal_19)
			{
				UBytePtr prom = new UBytePtr(memory_region(REGION_USER1));
				data = prom.read(type3_ctrs);
				logerror("%9.7f 6502-PC: %04x decocass_type3_r(%02x): $%02x <- prom[$%03x]\n", timer_get_time(), cpu_getpreviouspc(), offset, data, type3_ctrs);
				if (++type3_ctrs == 4096)
					type3_ctrs = 0;
			}
			else
			{
				if (0 == (offset & E5XX_MASK))
				{
					data = cpunum_get_reg(2, I8X41_STAT);
					logerror("%9.7f 6502-PC: %04x decocass_type3_r(%02x): $%02x <- 8041 STATUS\n", timer_get_time(), cpu_getpreviouspc(), offset, data);
				}
				else
				{
					data = 0xff;	/* open data bus? */
					logerror("%9.7f 6502-PC: %04x decocass_type3_r(%02x): $%02x <- open bus\n", timer_get_time(), cpu_getpreviouspc(), offset, data);
				}
			}
		}
		else
		{
			if (1 == type3_pal_19)
			{
				save = data = 0xff;    /* open data bus? */
				logerror("%9.7f 6502-PC: %04x decocass_type3_r(%02x): $%02x <- open bus", timer_get_time(), cpu_getpreviouspc(), offset, data);
			}
			else
			{
				if (0 == (offset & E5XX_MASK))
				{
					save = cpunum_get_reg(2, I8X41_DATA);
					if (type3_swap == TYPE3_SWAP_01)
					{
						data =
							(BIT1(save) << 0) |
							(type3_d0_latch << 1) |
							(BIT2(save) << 2) |
							(BIT3(save) << 3) |
							(BIT4(save) << 4) |
							(BIT5(save) << 5) |
							(BIT6(save) << 6) |
							(BIT7(save) << 7);
						type3_d0_latch = save & 1;
					}
					else
					if (type3_swap == TYPE3_SWAP_12)
					{
						data =
							(type3_d0_latch << 0) |
							(BIT2(save) << 1) |
							(BIT1(save) << 2) |
							(BIT3(save) << 3) |
							(BIT4(save) << 4) |
							(BIT5(save) << 5) |
							(BIT6(save) << 6) |
							(BIT7(save) << 7);
						type3_d0_latch = save & 1;
					}
					else
					if (type3_swap == TYPE3_SWAP_13)
					{
						data =
							(type3_d0_latch << 0) |
							(BIT3(save) << 1) |
							(BIT2(save) << 2) |
							(BIT1(save) << 3) |
							(BIT4(save) << 4) |
							(BIT5(save) << 5) |
							(BIT6(save) << 6) |
							(BIT7(save) << 7);
						type3_d0_latch = save & 1;
					}
					else
					if (type3_swap == TYPE3_SWAP_24)
					{
						data =
							(type3_d0_latch << 0) |
							(BIT1(save) << 1) |
							(BIT4(save) << 2) |
							(BIT3(save) << 3) |
							(BIT2(save) << 4) |
							(BIT5(save) << 5) |
							(BIT6(save) << 6) |
							(BIT7(save) << 7);
						type3_d0_latch = save & 1;
					}
					else
					if (type3_swap == TYPE3_SWAP_25)
					{
						data =
							(type3_d0_latch << 0) |
							(BIT1(save) << 1) |
							(BIT5(save) << 2) |
							(BIT3(save) << 3) |
							(BIT4(save) << 4) |
							(BIT2(save) << 5) |
							(BIT6(save) << 6) |
							(BIT7(save) << 7);
						type3_d0_latch = save & 1;
					}
					else
					if (type3_swap == TYPE3_SWAP_34_0)
					{
						data =
							(type3_d0_latch << 0) |
							(BIT1(save) << 1) |
							(BIT2(save) << 2) |
							(BIT3(save) << 4) |
							(BIT4(save) << 3) |
							(BIT5(save) << 5) |
							(BIT6(save) << 6) |
							(BIT7(save) << 7);
						type3_d0_latch = save & 1;
					}
					else
					if (type3_swap == TYPE3_SWAP_34_7)
					{
						data =
							(BIT7(save) << 0) |
							(BIT1(save) << 1) |
							(BIT2(save) << 2) |
							(BIT4(save) << 3) |
							(BIT3(save) << 4) |
							(BIT5(save) << 5) |
							(BIT6(save) << 6) |
							(type3_d0_latch << 7);
						type3_d0_latch = save & 1;
					}
					else
					if (type3_swap == TYPE3_SWAP_56)
					{
						data =
							type3_d0_latch |
							(BIT1(save) << 1) |
							(BIT2(save) << 2) |
							(BIT3(save) << 3) |
							(BIT4(save) << 4) |
							(BIT6(save) << 5) |
							(BIT5(save) << 6) |
							(BIT7(save) << 7);
						type3_d0_latch = save & 1;
					}
					else
					if (type3_swap == TYPE3_SWAP_67)
					{
						data =
							type3_d0_latch |
							(BIT1(save) << 1) |
							(BIT2(save) << 2) |
							(BIT3(save) << 3) |
							(BIT4(save) << 4) |
							(BIT5(save) << 5) |
							(BIT7(save) << 6) |
							(BIT6(save) << 7);
						type3_d0_latch = save & 1;
					}
					else
					{
						data =
							type3_d0_latch |
							(BIT1(save) << 1) |
							(BIT2(save) << 2) |
							(BIT3(save) << 3) |
							(BIT4(save) << 4) |
							(BIT5(save) << 5) |
							(BIT6(save) << 6) |
							(BIT7(save) << 7);
						type3_d0_latch = save & 1;
					}
					logerror("%9.7f 6502-PC: %04x decocass_type3_r(%02x): $%02x '%c' <- 8041-DATA\n", timer_get_time(), cpu_getpreviouspc(), offset, data, (data >= 32) ? data : '.');
				}
				else
				{
					save = 0xff;	/* open data bus? */
					data =
						type3_d0_latch |
						(BIT1(save) << 1) |
						(BIT2(save) << 2) |
						(BIT3(save) << 3) |
						(BIT4(save) << 4) |
						(BIT5(save) << 5) |
						(BIT6(save) << 7) |
						(BIT7(save) << 6);
					logerror("%9.7f 6502-PC: %04x decocass_type3_r(%02x): $%02x '%c' <- open bus (D0 replaced with latch)\n", timer_get_time(), cpu_getpreviouspc(), offset, data, (data >= 32) ? data : '.');
					type3_d0_latch = save & 1;
				}
			}
		}
	
		return data;
	} };
	
	public static WriteHandlerPtr decocass_type3_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (1 == (offset & 1))
		{
			if (1 == type3_pal_19)
			{
				type3_ctrs = data << 4;
				logerror("%9.7f 6502-PC: %04x decocass_e5xx_w(%02x): $%02x -> %s\n", timer_get_time(), cpu_getpreviouspc(), offset, data, "LDCTRS");
				return;
			}
			else
			if (0xc0 == (data & 0xf0))
				type3_pal_19 = 1;
		}
		else
		{
			if (1 == type3_pal_19)
			{
				/* write nowhere?? */
				logerror("%9.7f 6502-PC: %04x decocass_e5xx_w(%02x): $%02x -> %s\n", timer_get_time(), cpu_getpreviouspc(), offset, data, "nowhere?");
				return;
			}
		}
		logerror("%9.7f 6502-PC: %04x decocass_e5xx_w(%02x): $%02x -> %s\n", timer_get_time(), cpu_getpreviouspc(), offset, data, (offset & 1)!=0 ? "8041-CMND" : "8041-DATA");
		cpunum_set_reg(2, (offset & 1)!=0 ? I8X41_CMND : I8X41_DATA, data);
	} };
	
	/***************************************************************************
	 *
	 *	TYPE4 DONGLE
	 *	- Scrum Try
	 *	Contains a 32K (EP)ROM that can be read from any byte
	 *	boundary sequentially. The EPROM is enable after writing
	 *	1100xxxx to E5x1 once. Then an address is written LSB
	 *	to E5x0 MSB to E5x1 and every read from E5x1 returns the
	 *	next byte of the contents.
	 *
	 ***************************************************************************/
	
	public static ReadHandlerPtr decocass_type4_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int data;
	
		if (1 == (offset & 1))
		{
			if (0 == (offset & E5XX_MASK))
			{
				data = cpunum_get_reg(2, I8X41_STAT);
				logerror("%9.7f 6502-PC: %04x decocass_type4_r(%02x): $%02x <- 8041 STATUS\n", timer_get_time(), cpu_getpreviouspc(), offset, data);
			}
			else
			{
				data = 0xff;	/* open data bus? */
				logerror("%9.7f 6502-PC: %04x decocass_type4_r(%02x): $%02x <- open bus\n", timer_get_time(), cpu_getpreviouspc(), offset, data);
			}
		}
		else
		{
			if (type4_latch != 0)
			{
				UBytePtr prom = new UBytePtr(memory_region(REGION_USER1));
	
				data = prom.read(type4_ctrs);
				logerror("%9.7f 6502-PC: %04x decocass_type5_r(%02x): $%02x '%c' <- PROM[%04x]\n", timer_get_time(), cpu_getpreviouspc(), offset, data, (data >= 32) ? data : '.', type4_ctrs);
				type4_ctrs = (type4_ctrs+1) & 0x7fff;
			}
			else
			{
				if (0 == (offset & E5XX_MASK))
				{
					data = cpunum_get_reg(2, I8X41_DATA);
					logerror("%9.7f 6502-PC: %04x decocass_type4_r(%02x): $%02x '%c' <- open bus (D0 replaced with latch)\n", timer_get_time(), cpu_getpreviouspc(), offset, data, (data >= 32) ? data : '.');
				}
				else
				{
					data = 0xff;	/* open data bus? */
					logerror("%9.7f 6502-PC: %04x decocass_type4_r(%02x): $%02x <- open bus\n", timer_get_time(), cpu_getpreviouspc(), offset, data);
				}
			}
		}
	
		return data;
	} };
	
	public static WriteHandlerPtr decocass_type4_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (1 == (offset & 1))
		{
			if (1 == type4_latch)
			{
				type4_ctrs = (type4_ctrs & 0x00ff) | ((data & 0x7f) << 8);
				logerror("%9.7f 6502-PC: %04x decocass_e5xx_w(%02x): $%02x -> CTRS MSB (%04x)\n", timer_get_time(), cpu_getpreviouspc(), offset, data, type4_ctrs);
				return;
			}
			else
			if (0xc0 == (data & 0xf0))
			{
				type4_latch = 1;
			}
		}
		else
		{
			if (type4_latch != 0)
			{
				type4_ctrs = (type4_ctrs & 0xff00) | data;
				logerror("%9.7f 6502-PC: %04x decocass_e5xx_w(%02x): $%02x -> CTRS LSB (%04x)\n", timer_get_time(), cpu_getpreviouspc(), offset, data, type4_ctrs);
				return;
			}
		}
		logerror("%9.7f 6502-PC: %04x decocass_e5xx_w(%02x): $%02x -> %s\n", timer_get_time(), cpu_getpreviouspc(), offset, data, (offset & 1)!=0 ? "8041-CMND" : "8041-DATA");
		cpunum_set_reg(2, (offset & 1)!=0 ? I8X41_CMND : I8X41_DATA, data);
	} };
	
	/***************************************************************************
	 *
	 *	TYPE5 DONGLE
	 *	- Boulder Dash
	 *	Actually a NOP dongle returning 0x55 after triggering a latch
	 *	by writing 1100xxxx to E5x1
	 *
	 ***************************************************************************/
	
	public static ReadHandlerPtr decocass_type5_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int data;
	
		if (1 == (offset & 1))
		{
			if (0 == (offset & E5XX_MASK))
			{
				data = cpunum_get_reg(2, I8X41_STAT);
				logerror("%9.7f 6502-PC: %04x decocass_type5_r(%02x): $%02x <- 8041 STATUS\n", timer_get_time(), cpu_getpreviouspc(), offset, data);
			}
			else
			{
				data = 0xff;	/* open data bus? */
				logerror("%9.7f 6502-PC: %04x decocass_type5_r(%02x): $%02x <- open bus\n", timer_get_time(), cpu_getpreviouspc(), offset, data);
			}
		}
		else
		{
			if (type5_latch != 0)
			{
				data = 0x55;	/* Only a fixed value? It looks like this is all we need to do */
				logerror("%9.7f 6502-PC: %04x decocass_type5_r(%02x): $%02x '%c' <- fixed value???\n", timer_get_time(), cpu_getpreviouspc(), offset, data, (data >= 32) ? data : '.');
			}
			else
			{
				if (0 == (offset & E5XX_MASK))
				{
					data = cpunum_get_reg(2, I8X41_DATA);
					logerror("%9.7f 6502-PC: %04x decocass_type5_r(%02x): $%02x '%c' <- open bus (D0 replaced with latch)\n", timer_get_time(), cpu_getpreviouspc(), offset, data, (data >= 32) ? data : '.');
				}
				else
				{
					data = 0xff;	/* open data bus? */
					logerror("%9.7f 6502-PC: %04x decocass_type5_r(%02x): $%02x <- open bus\n", timer_get_time(), cpu_getpreviouspc(), offset, data);
				}
			}
		}
	
		return data;
	} };
	
	public static WriteHandlerPtr decocass_type5_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (1 == (offset & 1))
		{
			if (1 == type5_latch)
			{
				logerror("%9.7f 6502-PC: %04x decocass_e5xx_w(%02x): $%02x -> %s\n", timer_get_time(), cpu_getpreviouspc(), offset, data, "latch #2??");
				return;
			}
			else
			if (0xc0 == (data & 0xf0))
				type5_latch = 1;
		}
		else
		{
			if (type5_latch != 0)
			{
				/* write nowhere?? */
				logerror("%9.7f 6502-PC: %04x decocass_e5xx_w(%02x): $%02x -> %s\n", timer_get_time(), cpu_getpreviouspc(), offset, data, "nowhere?");
				return;
			}
		}
		logerror("%9.7f 6502-PC: %04x decocass_e5xx_w(%02x): $%02x -> %s\n", timer_get_time(), cpu_getpreviouspc(), offset, data, (offset & 1)!=0 ? "8041-CMND" : "8041-DATA");
		cpunum_set_reg(2, (offset & 1)!=0 ? I8X41_CMND : I8X41_DATA, data);
	} };
	
	/***************************************************************************
	 *
	 *	Main dongle and 8041 interface
	 *
	 ***************************************************************************/
	
	public static ReadHandlerPtr decocass_e5xx_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int data;
	
		/* E5x2-E5x3 and mirrors */
		if (2 == (offset & E5XX_MASK))
		{
			data =
				(BIT7(i8041_p1) 	  << 0) |	/* D0 = P17 - REQ/ */
				(BIT0(i8041_p2) 	  << 1) |	/* D1 = P20 - FNO/ */
				(BIT1(i8041_p2) 	  << 2) |	/* D2 = P21 - EOT/ */
				(BIT2(i8041_p2) 	  << 3) |	/* D3 = P22 - ERR/ */
				((tape_bot_eot) 	  << 4) |	/* D4 = BOT/EOT (direct from drive) */
				(1					  << 5) |	/* D5 floating input */
				(1					  << 6) |	/* D6 floating input */
				((1 - tape_present)   << 7);	/* D7 = cassette present */
	
			logerror("%9.7f 6502-PC: %04x decocass_e5xx_r(%02x): $%02x <- STATUS (%s%s%s%s%s%s%s%s)\n",
				timer_get_time(),
				cpu_getpreviouspc(),
				offset, data,
				(data & 0x01)!=0 ? "" : "REQ/",
				(data & 0x02)!=0 ? "" : " FNO/",
				(data & 0x04)!=0 ? "" : " EOT/",
				(data & 0x08)!=0 ? "" : " ERR/",
				(data & 0x10)!=0 ? " [BOT-EOT]" : "",
				(data & 0x20)!=0 ? " [BIT5?]" : "",
				(data & 0x40)!=0 ? " [BIT6?]" : "",
				(data & 0x80)!=0 ? "" : " [CASS-PRESENT/]");
		}
		else
		{
			if (decocass_dongle_r != null)
				data = decocass_dongle_r.handler(offset);
			else
				data = 0xff;
		}
		return data;
	} };
	
	public static WriteHandlerPtr decocass_e5xx_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (decocass_dongle_w != null)
		{
			decocass_dongle_w.handler(offset, data);
			return;
		}
	
		if (0 == (offset & E5XX_MASK))
		{
			logerror("%9.7f 6502-PC: %04x decocass_e5xx_w(%02x): $%02x -> %s\n", timer_get_time(), cpu_getpreviouspc(), offset, data, (offset & 1)!=0 ? "8041-CMND" : "8041-DATA");
			cpunum_set_reg(2, (offset & 1)!=0 ? I8X41_CMND : I8X41_DATA, data);
	//#ifdef MAME_DEBUG
	//		decocass_fno(offset, data);
	//#endif
		}
		else
		{
			logerror("%9.7f 6502-PC: %04x decocass_e5xx_w(%02x): $%02x -> dongle\n", timer_get_time(), cpu_getpreviouspc(), offset, data);
		}
	} };
	
	/***************************************************************************
	 *
	 *	init machine functions (select dongle and determine tape image size)
	 *
	 ***************************************************************************/
	static void decocass_state_save_postload()
	{
		int A;
		UBytePtr mem = new UBytePtr(memory_region(REGION_CPU1));
		int diff = memory_region_length(REGION_CPU1) / 2;
	
		memory_set_opcode_base(0, new UBytePtr(mem, diff));
	
		for (A = 0;A < 0x10000; A++)
			decocass_w.handler(A, mem.read(A));
		/* restart the timer if the tape was playing */
		if (0 != tape_dir)
			tape_timer = timer_set(TIME_NEVER, 0, null);
	}
	
	public static void decocass_init_common()
	{
		UBytePtr image = new UBytePtr(memory_region(REGION_USER2));
		int i, offs;
	
		tape_dir = 0;
		tape_speed = 0;
		tape_timer = null;
	
		firsttime = 1;
		tape_present = 1;
		tape_blocks = 0;
		for (i = memory_region_length(REGION_USER2) / 256 - 1; tape_blocks==0 && i > 0; i--)
			for (offs = 256 * i; tape_blocks==0 && offs < 256 * i + 256; offs++)
				if (image.read(offs) != 0)
					tape_blocks = i+1;
		for (i = 0; i < tape_blocks; i++)
		{
			crc16_lsb = 0;
			crc16_msb = 0;
			for (offs = 256 * i; offs < 256 * i + 256; offs++)
			{
				tape_crc16(image.read(offs) << 7);
				tape_crc16(image.read(offs) << 6);
				tape_crc16(image.read(offs) << 5);
				tape_crc16(image.read(offs) << 4);
				tape_crc16(image.read(offs) << 3);
				tape_crc16(image.read(offs) << 2);
				tape_crc16(image.read(offs) << 1);
				tape_crc16(image.read(offs) << 0);
			}
			tape_crc16_lsb[i] = crc16_lsb;
			tape_crc16_msb[i] = crc16_msb;
		}
	
		tape_length = tape_blocks * TAPE_CHUNK * 8 * 2 + 2 * (TAPE_LEADER + TAPE_GAP + TAPE_HOLE);
		tape_time0 = (double)(TAPE_LEADER + TAPE_GAP - TAPE_HOLE) / TAPE_CLOCKRATE;
		logerror("tape: %d blocks\n", tape_blocks);
		tape_bot_eot = 0;
	
		decocass_dongle_r = null;
		decocass_dongle_w = null;
	
		decocass_reset = 0;
		i8041_p1 = 0xff;
		i8041_p2 = 0xff;
	
		type1_inmap = MAKE_MAP(0,1,2,3,4,5,6,7);
		type1_outmap = MAKE_MAP(0,1,2,3,4,5,6,7);
	
		type2_d2_latch = 0;
		type2_xx_latch = 0;
		type2_promaddr = 0;
	
		type3_ctrs = 0;
		type3_d0_latch = 0;
		type3_pal_19 = 0;
		type3_swap = 0;
	
		//memset(decocass_quadrature_decoder, 0, sizeof(decocass_quadrature_decoder));
                decocass_quadrature_decoder = new int[4];
		decocass_sound_ack = 0;
		decocass_sound_timer = null;
	
		/* state saving code */
		/*TODO*///state_save_register_func_postload(decocass_state_save_postload);
		/*TODO*///state_save_register_int 	("decocass", 0, "tape_dir", &tape_dir);
		/*TODO*///state_save_register_int 	("decocass", 0, "tape_speed", &tape_speed);
		/*TODO*///state_save_register_double	("decocass", 0, "tape_time0", &tape_time0, 1);
		/*TODO*///state_save_register_int 	("decocass", 0, "firsttime", &firsttime);
		/*TODO*///state_save_register_int 	("decocass", 0, "tape_present", &tape_present);
		/*TODO*///state_save_register_int 	("decocass", 0, "tape_blocks", &tape_blocks);
		/*TODO*///state_save_register_int 	("decocass", 0, "tape_length", &tape_length);
		/*TODO*///state_save_register_int 	("decocass", 0, "tape_bot_eot", &tape_bot_eot);
		/*TODO*///state_save_register_UINT8	("decocass", 0, "crc16_lsb", &crc16_lsb, 1);
		/*TODO*///state_save_register_UINT8	("decocass", 0, "crc16_msb", &crc16_msb, 1);
		/*TODO*///state_save_register_UINT8	("decocass", 0, "tape_crc16_lsb", tape_crc16_lsb, 256);
		/*TODO*///state_save_register_UINT8	("decocass", 0, "tape_crc16_msb", tape_crc16_msb, 256);
		/*TODO*///state_save_register_UINT8	("decocass", 0, "decocass_reset", &decocass_reset, 1);
		/*TODO*///state_save_register_UINT8	("decocass", 0, "i8041_p1", &i8041_p1, 1);
		/*TODO*///state_save_register_UINT8	("decocass", 0, "i8041_p2", &i8041_p2, 1);
		/*TODO*///state_save_register_UINT32	("decocass", 0, "type1_inmap", &type1_inmap, 1);
		/*TODO*///state_save_register_UINT32	("decocass", 0, "type1_outmap", &type1_outmap, 1);
		/*TODO*///state_save_register_int 	("decocass", 0, "type2_d2_latch", &type2_d2_latch);
		/*TODO*///state_save_register_int 	("decocass", 0, "type2_xx_latch", &type2_xx_latch);
		/*TODO*///state_save_register_int 	("decocass", 0, "type2_promaddr", &type2_promaddr);
		/*TODO*///state_save_register_int 	("decocass", 0, "type3_ctrs", &type3_ctrs);
		/*TODO*///state_save_register_int 	("decocass", 0, "type3_d0_latch", &type3_d0_latch);
		/*TODO*///state_save_register_int 	("decocass", 0, "type3_pal_19", &type3_pal_19);
		/*TODO*///state_save_register_int 	("decocass", 0, "type3_swap", &type3_swap);
		/*TODO*///state_save_register_int 	("decocass", 0, "type4_ctrs", &type4_ctrs);
		/*TODO*///state_save_register_int 	("decocass", 0, "type4_latch", &type4_latch);
		/*TODO*///state_save_register_int 	("decocass", 0, "type5_latch", &type5_latch);
		/*TODO*///state_save_register_UINT8	("decocass", 0, "decocass_sound_ack", &decocass_sound_ack, 1);
	}
	
	public static InitMachinePtr decocass_init_machine = new InitMachinePtr() { public void handler() 
	{
		decocass_init_common();
	} };
	
	public static InitMachinePtr ctsttape_init_machine = new InitMachinePtr() { public void handler() 
	{
		decocass_init_common();
		logerror("dongle type #1 (DE-0061)\n");
		decocass_dongle_r = decocass_type1_map1_r;
	} };
	
	public static InitMachinePtr clocknch_init_machine = new InitMachinePtr() { public void handler() 
	{
		decocass_init_common();
		logerror("dongle type #1 (DE-0061 flip 2-3)\n");
		decocass_dongle_r = decocass_type1_r;
		type1_inmap = MAKE_MAP(0,1,3,2,4,5,6,7);
		type1_outmap = MAKE_MAP(0,1,3,2,4,5,6,7);
	} };
	
	public static InitMachinePtr ctisland_init_machine = new InitMachinePtr() { public void handler() 
	{
		decocass_init_common();
		logerror("dongle type #1 (DE-0061 flip 0-2)\n");
		decocass_dongle_r = decocass_type1_r;
		type1_inmap = MAKE_MAP(2,1,0,3,4,5,6,7);
		type1_outmap = MAKE_MAP(2,1,0,3,4,5,6,7);
	} };
	
	public static InitMachinePtr csuperas_init_machine = new InitMachinePtr() { public void handler() 
	{
		decocass_init_common();
		logerror("dongle type #1 (DE-0061 flip 4-5)\n");
		decocass_dongle_r = decocass_type1_r;
		type1_inmap = MAKE_MAP(0,1,2,3,5,4,6,7);
		type1_outmap = MAKE_MAP(0,1,2,3,5,4,6,7);
	} };
	
	public static InitMachinePtr castfant_init_machine = new InitMachinePtr() { public void handler() 
	{
		decocass_init_common();
		logerror("dongle type #1 (DE-0061 flip 1-2)\n");
		decocass_dongle_r = decocass_type1_map3_r;
	} };
	
	public static InitMachinePtr cluckypo_init_machine = new InitMachinePtr() { public void handler() 
	{
		decocass_init_common();
		logerror("dongle type #1 (DE-0061 flip 1-3)\n");
		decocass_dongle_r = decocass_type1_r;
		type1_inmap = MAKE_MAP(0,3,2,1,4,5,6,7);
		type1_outmap = MAKE_MAP(0,3,2,1,4,5,6,7);
	} };
	
	public static InitMachinePtr cterrani_init_machine = new InitMachinePtr() { public void handler() 
	{
		decocass_init_common();
		logerror("dongle type #1 (DE-0061 straight)\n");
		decocass_dongle_r = decocass_type1_r;
		type1_inmap = MAKE_MAP(0,1,2,3,4,5,6,7);
		type1_outmap = MAKE_MAP(0,1,2,3,4,5,6,7);
	} };
	
	public static InitMachinePtr cexplore_init_machine = new InitMachinePtr() { public void handler() 
	{
		decocass_init_common();
		logerror("dongle type #1 (DE-0061)\n");
		decocass_dongle_r = decocass_type1_map2_r;
	} };
	
	public static InitMachinePtr cprogolf_init_machine = new InitMachinePtr() { public void handler() 
	{
		decocass_init_common();
		logerror("dongle type #1 (DE-0061 flip 0-1)\n");
		decocass_dongle_r = decocass_type1_r;
		type1_inmap = MAKE_MAP(1,0,2,3,4,5,6,7);
		type1_outmap = MAKE_MAP(1,0,2,3,4,5,6,7);
	} };
	
	public static InitMachinePtr cmissnx_init_machine = new InitMachinePtr() { public void handler() 
	{
		decocass_init_common();
		logerror("dongle type #2 (CS82-007)\n");
		decocass_dongle_r = decocass_type2_r;
		decocass_dongle_w = decocass_type2_w;
	} };
	
	public static InitMachinePtr cdiscon1_init_machine = new InitMachinePtr() { public void handler() 
	{
		decocass_init_common();
		logerror("dongle type #2 (CS82-007)\n");
		decocass_dongle_r = decocass_type2_r;
		decocass_dongle_w = decocass_type2_w;
	} };
	
	public static InitMachinePtr cptennis_init_machine = new InitMachinePtr() { public void handler() 
	{
		decocass_init_common();
		logerror("dongle type #2 (CS82-007)\n");
		decocass_dongle_r = decocass_type2_r;
		decocass_dongle_w = decocass_type2_w;
	} };
	
	public static InitMachinePtr ctornado_init_machine = new InitMachinePtr() { public void handler() 
	{
		decocass_init_common();
		logerror("dongle type #2 (CS82-007)\n");
		decocass_dongle_r = decocass_type2_r;
		decocass_dongle_w = decocass_type2_w;
	} };
	
	public static InitMachinePtr cbnj_init_machine = new InitMachinePtr() { public void handler() 
	{
		decocass_init_common();
		logerror("dongle type #3 (PAL)\n");
		decocass_dongle_r = decocass_type3_r;
		decocass_dongle_w = decocass_type3_w;
		type3_swap = TYPE3_SWAP_67;
	} };
	
	public static InitMachinePtr cburnrub_init_machine = new InitMachinePtr() { public void handler() 
	{
		decocass_init_common();
		logerror("dongle type #3 (PAL)\n");
		decocass_dongle_r = decocass_type3_r;
		decocass_dongle_w = decocass_type3_w;
		type3_swap = TYPE3_SWAP_67;
	} };
	
	public static InitMachinePtr cbtime_init_machine = new InitMachinePtr() { public void handler() 
	{
		decocass_init_common();
		logerror("dongle type #3 (PAL)\n");
		decocass_dongle_r = decocass_type3_r;
		decocass_dongle_w = decocass_type3_w;
		type3_swap = TYPE3_SWAP_12;
	} };
	
	public static InitMachinePtr cgraplop_init_machine = new InitMachinePtr() { public void handler() 
	{
		decocass_init_common();
		logerror("dongle type #3 (PAL)\n");
		decocass_dongle_r = decocass_type3_r;
		decocass_dongle_w = decocass_type3_w;
		type3_swap = TYPE3_SWAP_56;
	} };
	
	public static InitMachinePtr clapapa_init_machine = new InitMachinePtr() { public void handler() 
	{
		decocass_init_common();
		logerror("dongle type #3 (PAL)\n");
		decocass_dongle_r = decocass_type3_r;
		decocass_dongle_w = decocass_type3_w;
		type3_swap = TYPE3_SWAP_34_7;
	} };
	
	public static InitMachinePtr cfghtice_init_machine = new InitMachinePtr() { public void handler() 
	{
		decocass_init_common();
		logerror("dongle type #3 (PAL)\n");
		decocass_dongle_r = decocass_type3_r;
		decocass_dongle_w = decocass_type3_w;
		type3_swap = TYPE3_SWAP_25;
	} };
	
	public static InitMachinePtr cprobowl_init_machine = new InitMachinePtr() { public void handler() 
	{
		decocass_init_common();
		logerror("dongle type #3 (PAL)\n");
		decocass_dongle_r = decocass_type3_r;
		decocass_dongle_w = decocass_type3_w;
		type3_swap = TYPE3_SWAP_34_0;
	} };
	
	public static InitMachinePtr cnightst_init_machine = new InitMachinePtr() { public void handler() 
	{
		decocass_init_common();
		logerror("dongle type #3 (PAL)\n");
		decocass_dongle_r = decocass_type3_r;
		decocass_dongle_w = decocass_type3_w;
		type3_swap = TYPE3_SWAP_13;
	} };
	
	public static InitMachinePtr cprosocc_init_machine = new InitMachinePtr() { public void handler() 
	{
		decocass_init_common();
		logerror("dongle type #3 (PAL)\n");
		decocass_dongle_r = decocass_type3_r;
		decocass_dongle_w = decocass_type3_w;
		type3_swap = TYPE3_SWAP_24;
	} };
	
	public static InitMachinePtr cppicf_init_machine = new InitMachinePtr() { public void handler() 
	{
		decocass_init_common();
		logerror("dongle type #3 (PAL)\n");
		decocass_dongle_r = decocass_type3_r;
		decocass_dongle_w = decocass_type3_w;
		type3_swap = TYPE3_SWAP_01;
	} };
	
	public static InitMachinePtr cscrtry_init_machine = new InitMachinePtr() { public void handler() 
	{
		decocass_init_common();
		logerror("dongle type #4 (32K ROM)\n");
		decocass_dongle_r = decocass_type4_r;
		decocass_dongle_w = decocass_type4_w;
	} };
	
	public static InitMachinePtr cbdash_init_machine = new InitMachinePtr() { public void handler() 
	{
		decocass_init_common();
		logerror("dongle type #5 (NOP)\n");
		decocass_dongle_r = decocass_type5_r;
		decocass_dongle_w = decocass_type5_w;
	} };
	
	/***************************************************************************
	 *
	 *	8041 port handlers
	 *
	 ***************************************************************************/
	
	static void tape_stop()
	{
		if (tape_timer != null)
		{
			/* remember time */
			tape_time0 += tape_dir * timer_timeelapsed(tape_timer);
			timer_remove(tape_timer);
			tape_timer = null;
		}
	}
	
	static int i8041_p1_old;
        
	public static WriteHandlerPtr i8041_p1_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		
	
		if (data != i8041_p1_old)
		{
			logerror("%9.7f 8041-PC: %03x i8041_p1_w: $%02x (%s%s%s%s%s%s%s%s)\n",
				timer_get_time(),
				cpu_getpreviouspc(),
				data,
				(data & 0x01)!=0 ? "" : "DATA-WRT",
				(data & 0x02)!=0 ? "" : " DATA-CLK",
				(data & 0x04)!=0 ? "" : " FAST",
				(data & 0x08)!=0 ? "" : " BIT3",
				(data & 0x10)!=0 ? "" : " REW",
				(data & 0x20)!=0 ? "" : " FWD",
				(data & 0x40)!=0 ? "" : " WREN",
				(data & 0x80)!=0 ? "" : " REQ");
			i8041_p1_old = data;
		}
	
		/* change in REW signal ? */
		if (((data ^ i8041_p1) & 0x10) != 0)
		{
			tape_stop();
			if (0 == (data & 0x10))
			{
				logerror("tape %5.4fs: rewind\n", tape_time0);
				tape_dir = -1;
				tape_timer = timer_set(TIME_NEVER, 0, null);
				/*TODO*///set_led_status(0, 1);
			}
			else
			{
				tape_dir = 0;
				tape_speed = 0;
				logerror("tape %5.4fs: stopped\n", tape_time0);
	/*TODO*///#if TAPE_UI_DISPLAY
	/*TODO*///			usrintf_showmessage("   [%05.1fs]   ", tape_time0);
	/*TODO*///#endif
	/*TODO*///			set_led_status(0, 0);
			}
		}
	
		/* change in FWD signal ? */
		if (((data ^ i8041_p1) & 0x20) != 0)
		{
			tape_stop();
			if (0 == (data & 0x20))
			{
				logerror("tape %5.4fs: forward\n", tape_time0);
				tape_dir = +1;
				tape_timer = timer_set(TIME_NEVER, 0, null);
				/*TODO*///set_led_status(0, 1);
			}
			else
			{
				tape_dir = 0;
				tape_speed = 0;
				logerror("tape %5.4fs: stopped\n", tape_time0);
	/*TODO*///#if TAPE_UI_DISPLAY
	/*TODO*///			usrintf_showmessage("   [%05.1fs]   ", tape_time0);
	/*TODO*///#endif
	/*TODO*///			set_led_status(0, 0);
			}
		}
	
		/* change in FAST signal ? */
		if (tape_timer!=null && ((data ^ i8041_p1) & 0x04)!=0)
		{
			tape_stop();
			tape_speed = (0 == (data & 0x04)) ? 1 : 0;
	
			if (tape_dir < 0)
			{
				logerror("tape: fast rewind %s\n", (0 == (data & 0x04)) ? "on" : "off");
				tape_dir = (tape_speed != 0) ? -7 : -1;
				tape_timer = timer_set(TIME_NEVER, 0, null);
			}
			else
			if (tape_dir > 0)
			{
				logerror("tape: fast forward %s\n", (0 == (data & 0x04)) ? "on" : "off");
				tape_dir = (tape_speed != 0) ? +7 : +1;
				tape_timer = timer_set(TIME_NEVER, 0, null);
			}
		}
	
		i8041_p1 = data;
	} };
        
        public static ReadHandlerPtr i8041_p1_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int data = i8041_p1;
		
	
		if (data != i8041_p1_old)
		{
			logerror("%9.7f 8041-PC: %03x i8041_p1_r: $%02x (%s%s%s%s%s%s%s%s)\n",
				timer_get_time(),
				cpu_getpreviouspc(),
				data,
				(data & 0x01)!=0 ? "" : "DATA-WRT",
				(data & 0x02)!=0 ? "" : " DATA-CLK",
				(data & 0x04)!=0 ? "" : " FAST",
				(data & 0x08)!=0 ? "" : " BIT3",
				(data & 0x10)!=0 ? "" : " REW",
				(data & 0x20)!=0 ? "" : " FWD",
				(data & 0x40)!=0 ? "" : " WREN",
				(data & 0x80)!=0 ? "" : " REQ");
			i8041_p1_old = data;
		}
		return data;
	} };
        
        static int i8041_p2_old;
	
	public static WriteHandlerPtr i8041_p2_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		
		if (data != i8041_p2_old)
		{
			logerror("%9.7f 8041-PC: %03x i8041_p2_w: $%02x (%s%s%s%s%s%s%s%s)\n",
				timer_get_time(),
				cpu_getpreviouspc(),
				data,
				(data & 0x01)!=0 ? "" : "FNO/",
				(data & 0x02)!=0 ? "" : " EOT/",
				(data & 0x04)!=0 ? "" : " ERR/",
				(data & 0x08)!=0 ? "" : " OUT3?/",
				(data & 0x10)!=0 ? " [IN4]" : "",
				(data & 0x20)!=0 ? " [BOT-EOT]" : "",
				(data & 0x40)!=0 ? " [RCLK]" : "",
				(data & 0x80)!=0 ? " [RDATA]" : "");
			i8041_p2_old = data;
		}
		i8041_p2 = data;
	} };
	
	public static ReadHandlerPtr i8041_p2_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int data;
		
		tape_update();
	
		data = i8041_p2;
	
		if (data != i8041_p2_old)
		{
			logerror("%9.7f 8041-PC: %03x i8041_p2_r: $%02x (%s%s%s%s%s%s%s%s)\n",
				timer_get_time(),
				cpu_getpreviouspc(),
				data,
				(data & 0x01)!=0 ? "" : "FNO/",
				(data & 0x02)!=0 ? "" : " EOT/",
				(data & 0x04)!=0 ? "" : " ERR/",
				(data & 0x08)!=0 ? "" : " OUT3?/",
				(data & 0x10)!=0 ? " [IN4]" : "",
				(data & 0x20)!=0 ? " [BOT-EOT]" : "",
				(data & 0x40)!=0 ? " [RCLK]" : "",
				(data & 0x80)!=0 ? " [RDATA]" : "");
			i8041_p2_old = data;
		}
		return data;
	} };
	
	
}
