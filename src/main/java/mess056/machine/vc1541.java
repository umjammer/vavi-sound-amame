/***************************************************************************

       commodore vc1541 floppy disk drive

***************************************************************************/

/*
 vc1540
  higher serial bus timing than the vc1541, to fast for c64
 vc1541
  (also build in in sx64)
  used with commodore vc20, c64, c16, c128
  floppy disk drive 5 1/4 inch, single sided, double density,
   40 tracks (tone head to big to use 80 tracks?)
  gcr encoding

  computer M6502, 2x VIA6522, 2 KByte RAM, 16 KByte ROM
  1 Commodore serial bus (2 connectors)

 vc1541 ieee488 hardware modification
  additional ieee488 connection, modified rom

 vc1541 II
  ?

 dolphin drives
  vc1541 clone?
  additional 8 KByte RAM at 0x8000 (complete track buffer ?)
  24 KByte rom

 c1551
  used with commodore c16
  m6510t? processor (4 MHz???),
  (m6510t internal 4mhz clock?, 8 port pins?)
  VIA6522 ?, CIA6526 ?,
  2 KByte RAM, 16 KByte ROM
  connector commodore C16 expansion cartridge
  parallel protocoll

 1750
  single sided 1751
 1751
  (also build in in c128d series)
  used with c128 (vic20,c16,c64 possible?)
  double sided
  can read some other disk formats (mfm encoded?)
  modified commodore serial bus (with quick serial transmission)
  (m6510?, cia6526?)
0000-00FF      Zero page work area, job queue, variables
0100-01FF      GCR overflow area and stack (1571 mode BAM side 1)
0200-02FF      Command buffer, parser, tables, variables
0300-07FF      5 data buffers, 0-4 one of which is used for BAM
1800-1BFF      65C22A, serial, controller ports
1C00-1FFF      65C22A, controller ports
8000-FFE5      32K byte ROM, DOS and controller routines
FFE6-FFFF      JMP table, user command vectors

 1581
  3 1/2 inch, double sided, double density, 80 tracks
  used with c128 (vic20,c16,c64 possible?)
  only mfm encoding?
0000-00FF       Zero page work area, job queue, variables
0100-01FF       Stack, variables, vectors
0200-02FF       Command buffer, tables, variables
0300-09FF       Data buffers (0-6)
0A00-0AFF       BAM for tracks 0-39
0B00-0BFF       BAM for tracks 40-79
0C00-1FFF       Track cache buffer
4000-5FFF       8520A CIA
6000-7FFF       WD177X FDC
8000-FEFF       32K byte ROM, DOS and controller routines
FF00-FFFF       Jump table, vectors

 2031/4031
 ieee488 interface
 1541 with ieee488 bus instead of serial bus
  $1800 via6522 used for ieee488 interface?
  maybe like in pet series?
   port a data in/out?
   port b ce port, ddr 31

 2040/3040
 ieee488 interface
 2 drives

 2041
 ieee488 interface

 4040
 ieee488 interface
 2 drives

 sfd1001
 ieee488 interface
 5 1/4 inch high density
 2 heads

 */

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.machine;

public class vc1541
{
	
/*TODO*///	
/*TODO*///	#define VERBOSE_DBG 1
/*TODO*///	
/*TODO*///	
/*TODO*///	/*
/*TODO*///	 * only for testing at the moment
/*TODO*///	 */
/*TODO*///	
/*TODO*///	#define WRITEPROTECTED 0

	/* 0 summarized, 1 computer, 2 vc1541 */
	public static class _serial
	{
		public int[] atn=new int[3], data=new int[3], clock=new int[3];
	}
	
        public static _serial serial = new _serial();

/*TODO*///	/* G64 or D64 image
/*TODO*///	 implementation as writeback system
/*TODO*///	 */
/*TODO*///	typedef enum { TypeVC1541, TypeC1551, Type2031 } CBM_Drive_Emu_type;
/*TODO*///	
/*TODO*///	typedef struct
/*TODO*///	{
/*TODO*///		int cpunumber;
/*TODO*///		CBM_Drive_Emu_type type;
/*TODO*///		union {
/*TODO*///			struct {
/*TODO*///				int deviceid;
/*TODO*///				int serial_atn, serial_clock, serial_data;
/*TODO*///				int acka, data;
/*TODO*///			} serial;
/*TODO*///			struct {
/*TODO*///				int deviceid;
/*TODO*///			} ieee488;
/*TODO*///			struct {
/*TODO*///				UINT8 cpu_ddr, cpu_port;
/*TODO*///				void *timer;
/*TODO*///			} c1551;
/*TODO*///		} drive;
/*TODO*///	
/*TODO*///		int via0irq, via1irq;
/*TODO*///	
/*TODO*///		int led, motor, frequency;
/*TODO*///	
/*TODO*///		double track;
/*TODO*///		int clock;
/*TODO*///	
/*TODO*///		void *timer;
/*TODO*///	
/*TODO*///		struct {
/*TODO*///			UINT8 data[(1+2+2+1+256/4+4)*5];
/*TODO*///			int sync;
/*TODO*///			int ready;
/*TODO*///			int ffcount;
/*TODO*///		} head;
/*TODO*///	
/*TODO*///		struct {
/*TODO*///			int pos; /* position  in sector */
/*TODO*///			int sector;
/*TODO*///			int image_type;
/*TODO*///			int image_id;
/*TODO*///			UINT8 *data;
/*TODO*///		} d64;
/*TODO*///	} CBM_Drive_Emu;
/*TODO*///	
/*TODO*///	CBM_Drive_Emu vc1541_static= { 0 }, *vc1541 = &vc1541_static;
/*TODO*///	
/*TODO*///	/* four different frequencies for the 4 different zones on the disk */
/*TODO*///	static double vc1541_times[4]= {
/*TODO*///		13/16e6, 14/16e6, 15/16e6, 16/16e6
/*TODO*///	};
/*TODO*///	
/*TODO*///	/*
/*TODO*///	 * gcr encoding 4 bits to 5 bits
/*TODO*///	 * 1 change, 0 none
/*TODO*///	 *
/*TODO*///	 * physical encoding of the data on a track
/*TODO*///	 * sector header
/*TODO*///	 *  sync (5x 0xff not gcr encoded)
/*TODO*///	 *  sync mark (0x08)
/*TODO*///	 *  checksum (chksum xor sector xor track xor id1 xor id2 gives 0)
/*TODO*///	 *  sector#
/*TODO*///	 *  track#
/*TODO*///	 *  id2 (disk id to prevent writing to disk after disk change)
/*TODO*///	 *  id1
/*TODO*///	 *  0x0f
/*TODO*///	 *  0x0f
/*TODO*///	 * cap normally 10 (min 5) byte?
/*TODO*///	 * sector data
/*TODO*///	 *  sync (5x 0xff not gcr encoded)
/*TODO*///	 *  sync mark (0x07)
/*TODO*///	 *  256 bytes
/*TODO*///	 *  checksum (256 bytes xored)
/*TODO*///	 * cap
/*TODO*///	 *
/*TODO*///	 * max 42 tracks, stepper resolution 84 tracks
/*TODO*///	 */
/*TODO*///	static int bin_2_gcr[] =
/*TODO*///	{
/*TODO*///		0xa, 0xb, 0x12, 0x13, 0xe, 0xf, 0x16, 0x17,
/*TODO*///		9, 0x19, 0x1a, 0x1b, 0xd, 0x1d, 0x1e, 0x15
/*TODO*///	};
/*TODO*///	
/*TODO*///	#if 0
/*TODO*///	static int gcr_2_bin[] = {
/*TODO*///		-1, -1, -1, -1,
/*TODO*///		-1, -1, -1, -1,
/*TODO*///		-1, 8, 0, 1,
/*TODO*///		-1, 0xc, 4, 5,
/*TODO*///		-1, -1, 2, 3,
/*TODO*///		-1, 0xf, 6, 7,
/*TODO*///		-1, 9, 0xa, 0xb,
/*TODO*///		-1, 0xd, 0xe, -1
/*TODO*///	};
/*TODO*///	#endif
/*TODO*///	
/*TODO*///	static void gcr_double_2_gcr(UINT8 a, UINT8 b, UINT8 c, UINT8 d, UINT8 *dest)
/*TODO*///	{
/*TODO*///		UINT8 gcr[8];
/*TODO*///		gcr[0]=bin_2_gcr[a>>4];
/*TODO*///		gcr[1]=bin_2_gcr[a&0xf];
/*TODO*///		gcr[2]=bin_2_gcr[b>>4];
/*TODO*///		gcr[3]=bin_2_gcr[b&0xf];
/*TODO*///		gcr[4]=bin_2_gcr[c>>4];
/*TODO*///		gcr[5]=bin_2_gcr[c&0xf];
/*TODO*///		gcr[6]=bin_2_gcr[d>>4];
/*TODO*///		gcr[7]=bin_2_gcr[d&0xf];
/*TODO*///		dest[0]=(gcr[0]<<3)|(gcr[1]>>2);
/*TODO*///		dest[1]=(gcr[1]<<6)|(gcr[2]<<1)|(gcr[3]>>4);
/*TODO*///		dest[2]=(gcr[3]<<4)|(gcr[4]>>1);
/*TODO*///		dest[3]=(gcr[4]<<7)|(gcr[5]<<2)|(gcr[6]>>3);
/*TODO*///		dest[4]=(gcr[6]<<5)|gcr[7];
/*TODO*///	}
/*TODO*///	
/*TODO*///	static struct {
/*TODO*///		int count;
/*TODO*///		int data[4];
/*TODO*///	} gcr_helper;
/*TODO*///	
/*TODO*///	static void vc1541_sector_start(void)
/*TODO*///	{
/*TODO*///		gcr_helper.count=0;
/*TODO*///	}
/*TODO*///	
/*TODO*///	static void vc1541_sector_data(UINT8 data, int *pos)
/*TODO*///	{
/*TODO*///		gcr_helper.data[gcr_helper.count++]=data;
/*TODO*///		if (gcr_helper.count==4) {
/*TODO*///			gcr_double_2_gcr(gcr_helper.data[0], gcr_helper.data[1],
/*TODO*///							 gcr_helper.data[2], gcr_helper.data[3],
/*TODO*///							 vc1541->head.data+*pos);
/*TODO*///			*pos=*pos+5;
/*TODO*///			gcr_helper.count=0;
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	static void vc1541_sector_end(int *pos)
/*TODO*///	{
/*TODO*///		assert(gcr_helper.count==0);
/*TODO*///	}
/*TODO*///	
/*TODO*///	static void vc1541_sector_to_gcr(int track, int sector)
/*TODO*///	{
/*TODO*///		int i=0, j, offset, chksum=0;
/*TODO*///	
/*TODO*///		if (vc1541->d64.data==NULL) return;
/*TODO*///		vc1541->head.data[i++]=0xff;
/*TODO*///		vc1541->head.data[i++]=0xff;
/*TODO*///		vc1541->head.data[i++]=0xff;
/*TODO*///		vc1541->head.data[i++]=0xff;
/*TODO*///		vc1541->head.data[i++]=0xff;
/*TODO*///		vc1541_sector_start();
/*TODO*///	
/*TODO*///		vc1541_sector_data(8, &i);
/*TODO*///		chksum= sector^track
/*TODO*///			^vc1541->d64.data[D64_TRACK_ID1]^vc1541->d64.data[D64_TRACK_ID2];
/*TODO*///		vc1541_sector_data(chksum, &i);
/*TODO*///		vc1541_sector_data(sector, &i);
/*TODO*///		vc1541_sector_data(track, &i);
/*TODO*///		vc1541_sector_data(vc1541->d64.data[D64_TRACK_ID1], &i);
/*TODO*///		vc1541_sector_data(vc1541->d64.data[D64_TRACK_ID2], &i);
/*TODO*///		vc1541_sector_data(0xf, &i);
/*TODO*///		vc1541_sector_data(0xf, &i);
/*TODO*///		vc1541_sector_end(&i);
/*TODO*///	
/*TODO*///		/* 5 - 10 gcr bytes cap */
/*TODO*///		gcr_double_2_gcr(0, 0, 0, 0, vc1541->head.data+i);i+=5;
/*TODO*///		gcr_double_2_gcr(0, 0, 0, 0, vc1541->head.data+i);i+=5;
/*TODO*///		vc1541->head.data[i++]=0xff;
/*TODO*///		vc1541->head.data[i++]=0xff;
/*TODO*///		vc1541->head.data[i++]=0xff;
/*TODO*///		vc1541->head.data[i++]=0xff;
/*TODO*///		vc1541->head.data[i++]=0xff;
/*TODO*///		vc1541_sector_data(0x7, &i);
/*TODO*///	
/*TODO*///		chksum=0;
/*TODO*///		for (offset=d64_tracksector2offset(track,sector), j=0; j<256; j++) {
/*TODO*///			chksum^=vc1541->d64.data[offset];
/*TODO*///			vc1541_sector_data(vc1541->d64.data[offset++], &i);
/*TODO*///		}
/*TODO*///		vc1541_sector_data(chksum, &i);
/*TODO*///		vc1541_sector_data(0, &i); /* padding up */
/*TODO*///		vc1541_sector_data(0, &i);
/*TODO*///		vc1541_sector_end(&i);
/*TODO*///		gcr_double_2_gcr(0, 0, 0, 0, vc1541->head.data+i);i+=5;
/*TODO*///		gcr_double_2_gcr(0, 0, 0, 0, vc1541->head.data+i);i+=5;
/*TODO*///	}
/*TODO*///	
/*TODO*///	MEMORY_READ_START( vc1541_readmem )
/*TODO*///		{0x0000, 0x07ff, MRA_RAM},
/*TODO*///		{0x1800, 0x180f, via_2_r},		   /* 0 and 1 used in vc20 */
/*TODO*///		{0x1810, 0x189f, MRA_NOP}, /* for debugger */
/*TODO*///		{0x1c00, 0x1c0f, via_3_r},
/*TODO*///		{0x1c10, 0x1c9f, MRA_NOP}, /* for debugger */
/*TODO*///		{0xc000, 0xffff, MRA_ROM},
/*TODO*///	MEMORY_END
/*TODO*///	
/*TODO*///	MEMORY_WRITE_START( vc1541_writemem )
/*TODO*///		{0x0000, 0x07ff, MWA_RAM},
/*TODO*///		{0x1800, 0x180f, via_2_w},
/*TODO*///		{0x1c00, 0x1c0f, via_3_w},
/*TODO*///		{0xc000, 0xffff, MWA_ROM},
/*TODO*///	MEMORY_END
/*TODO*///	
/*TODO*///	MEMORY_READ_START( dolphin_readmem )
/*TODO*///		{0x0000, 0x07ff, MRA_RAM},
/*TODO*///		{0x1800, 0x180f, via_2_r},		   /* 0 and 1 used in vc20 */
/*TODO*///		{0x1c00, 0x1c0f, via_3_r},
/*TODO*///		{0x8000, 0x9fff, MRA_RAM},
/*TODO*///		{0xa000, 0xffff, MRA_ROM},
/*TODO*///	MEMORY_END
/*TODO*///	
/*TODO*///	MEMORY_WRITE_START( dolphin_writemem )
/*TODO*///		{0x0000, 0x07ff, MWA_RAM},
/*TODO*///		{0x1800, 0x180f, via_2_w},
/*TODO*///		{0x1c00, 0x1c0f, via_3_w},
/*TODO*///		{0x8000, 0x9fff, MWA_RAM},
/*TODO*///		{0xa000, 0xffff, MWA_ROM},
/*TODO*///	MEMORY_END
/*TODO*///	
/*TODO*///	#if 0
/*TODO*///	INPUT_PORTS_START (vc1541)
/*TODO*///	PORT_START
/*TODO*///	PORT_DIPNAME (0x60, 0x00, "Device #", IP_KEY_NONE);
/*TODO*///	PORT_DIPSETTING (0x00, "8");
/*TODO*///	PORT_DIPSETTING (0x20, "9");
/*TODO*///	PORT_DIPSETTING (0x40, "10");
/*TODO*///	PORT_DIPSETTING (0x60, "11");
/*TODO*///	INPUT_PORTS_END(); }}; 
/*TODO*///	#endif
/*TODO*///	
/*TODO*///	static void vc1541_timer(int param)
/*TODO*///	{
/*TODO*///		if (vc1541->clock==0) {
/*TODO*///			vc1541->clock=1;
/*TODO*///			vc1541->head.ready=0;
/*TODO*///			vc1541->head.sync=0;
/*TODO*///			if (vc1541->type==TypeVC1541) {
/*TODO*///				cpu_set_irq_line(vc1541->cpunumber, M6502_SET_OVERFLOW, 1);
/*TODO*///				via_3_ca1_w(0,1);
/*TODO*///			}
/*TODO*///			return;
/*TODO*///		}
/*TODO*///		if (++(vc1541->d64.pos)>=sizeof(vc1541->head.data)) {
/*TODO*///			if (++(vc1541->d64.sector)>=
/*TODO*///				d64_sectors_per_track[(int)vc1541->track-1]) {
/*TODO*///				vc1541->d64.sector=0;
/*TODO*///			}
/*TODO*///			vc1541_sector_to_gcr((int)vc1541->track,vc1541->d64.sector);
/*TODO*///			vc1541->d64.pos=0;
/*TODO*///		}
/*TODO*///		vc1541->head.ready=1;
/*TODO*///		if (vc1541->head.data[vc1541->d64.pos]==0xff) {
/*TODO*///			vc1541->head.ffcount++;
/*TODO*///			if (vc1541->head.ffcount==5) {
/*TODO*///				vc1541->head.sync=1;
/*TODO*///			}
/*TODO*///		} else {
/*TODO*///			vc1541->head.ffcount=0;
/*TODO*///			vc1541->head.sync=0;
/*TODO*///		}
/*TODO*///		if (vc1541->type==TypeVC1541) {
/*TODO*///			cpu_set_irq_line(vc1541->cpunumber, M6502_SET_OVERFLOW, 0);
/*TODO*///			via_3_ca1_w(0,0);
/*TODO*///		}
/*TODO*///		vc1541->clock=0;
/*TODO*///	}
/*TODO*///	
/*TODO*///	/*
/*TODO*///	 * via 6522 at 0x1800
/*TODO*///	 * port b
/*TODO*///	 * 0 inverted serial data in
/*TODO*///	 * 1 inverted serial data out
/*TODO*///	 * 2 inverted serial clock in
/*TODO*///	 * 3 inverted serial clock out
/*TODO*///	 * 4 inverted serial atn out
/*TODO*///	 * 5 input device id 1
/*TODO*///	 * 6 input device id 2
/*TODO*///	 * id 2+id 1/0+0 devicenumber 8/0+1 9/1+0 10/1+1 11
/*TODO*///	 * 7 inverted serial atn in
/*TODO*///	 * also ca1 (books says cb2)
/*TODO*///	 * irq to m6502 irq connected (or with second via irq)
/*TODO*///	 */
/*TODO*///	static void vc1541_via0_irq (int level)
/*TODO*///	{
/*TODO*///		vc1541->via0irq = level;
/*TODO*///		DBG_LOG(2, "vc1541 via0 irq",("level %d %d\n",vc1541->via0irq,vc1541->via1irq));
/*TODO*///		cpu_set_irq_line (vc1541->cpunumber,
/*TODO*///						  M6502_IRQ_LINE, vc1541->via1irq || vc1541->via0irq);
/*TODO*///	}
/*TODO*///	
/*TODO*///	public static ReadHandlerPtr vc1541_via0_read_portb  = new ReadHandlerPtr() { public int handler(int offset)
/*TODO*///	{
/*TODO*///		static int old=-1;
/*TODO*///		int value = 0x7a;
/*TODO*///	
/*TODO*///		if (!vc1541->drive.serial.serial_data || !serial.data[0])
/*TODO*///			value |= 1;
/*TODO*///		if (!vc1541->drive.serial.serial_clock || !serial.clock[0])
/*TODO*///			value |= 4;
/*TODO*///		if (!serial.atn[0]) value |= 0x80;
/*TODO*///	
/*TODO*///		switch (vc1541->drive.serial.deviceid)
/*TODO*///		{
/*TODO*///		case 8:
/*TODO*///			value &= ~0x60;
/*TODO*///			break;
/*TODO*///		case 9:
/*TODO*///			value &= ~0x40;
/*TODO*///			break;
/*TODO*///		case 10:
/*TODO*///			value &= ~0x20;
/*TODO*///			break;
/*TODO*///		case 11:
/*TODO*///			break;
/*TODO*///		}
/*TODO*///		if (value!=old) {
/*TODO*///	
/*TODO*///			DBG_LOG(2, "vc1541 serial read",("%s %s %s\n",
/*TODO*///											 serial.atn[0]?"ATN":"atn",
/*TODO*///											 serial.clock[0]?"CLOCK":"clock",
/*TODO*///											 serial.data[0]?"DATA":"data"));
/*TODO*///	
/*TODO*///			DBG_LOG(2, "vc1541 serial read",("%s %s %s\n",
/*TODO*///											 value&0x80?"ATN":"atn",
/*TODO*///											 value&4?"CLOCK":"clock",
/*TODO*///											 value&1?"DATA":"data"));
/*TODO*///			old=value;
/*TODO*///		}
/*TODO*///	
/*TODO*///		return value;
/*TODO*///	} };
/*TODO*///	
/*TODO*///	static void vc1541_acka(void)
/*TODO*///	{
/*TODO*///		int value=vc1541->drive.serial.data;
/*TODO*///		if (vc1541->drive.serial.acka!=serial.atn[0]) {
/*TODO*///			value=0;
/*TODO*///		}
/*TODO*///		if (value!= vc1541->drive.serial.serial_data)
/*TODO*///		{
/*TODO*///			vc1541_serial_data_write (1, vc1541->drive.serial.serial_data = value );
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	public static WriteHandlerPtr vc1541_via0_write_portb = new WriteHandlerPtr() {public void handler(int offset, int data)
/*TODO*///	{
/*TODO*///		DBG_LOG(2, "vc1541 serial write",("%s %s %s\n",
/*TODO*///										 data&0x10?"ATN":"atn",
/*TODO*///										 data&8?"CLOCK":"clock",
/*TODO*///										 data&2?"DATA":"data"));
/*TODO*///	
/*TODO*///		vc1541->drive.serial.data=data&2?0:1;
/*TODO*///		vc1541->drive.serial.acka=(data&0x10)?1:0;
/*TODO*///	#if 0
/*TODO*///		vc1541_acka();
/*TODO*///	#else
/*TODO*///		if ((!(data & 2)) != vc1541->drive.serial.serial_data)
/*TODO*///		{
/*TODO*///			vc1541_serial_data_write (1, vc1541->drive.serial.serial_data = !(data & 2));
/*TODO*///		}
/*TODO*///	#endif
/*TODO*///		if ((!(data & 8)) != vc1541->drive.serial.serial_clock)
/*TODO*///		{
/*TODO*///			vc1541_serial_clock_write (1, vc1541->drive.serial.serial_clock = !(data & 8));
/*TODO*///		}
/*TODO*///		vc1541_serial_atn_write (1, vc1541->drive.serial.serial_atn = 1);
/*TODO*///	} };
/*TODO*///	
/*TODO*///	/*
/*TODO*///	 * via 6522 at 0x1c00
/*TODO*///	 * port a
/*TODO*///	    byte in gcr format from or to floppy
/*TODO*///	
/*TODO*///	 * port b
/*TODO*///	 * 0 output steppermotor
/*TODO*///	 * 1 output steppermotor
/*TODO*///	     10: 00->01->10->11->00 move head to higher tracks
/*TODO*///	 * 2 output motor (rotation) (300 revolutions per minute)
/*TODO*///	 * 3 output led
/*TODO*///	 * 4 input disk not write protected
/*TODO*///	 * 5 timer adjustment
/*TODO*///	 * 6 timer adjustment
/*TODO*///	 * 4 different speed zones (track dependend)
/*TODO*///	    frequency select?
/*TODO*///	    3 slowest
/*TODO*///	    0 highest
/*TODO*///	 * 7 input sync signal when reading from disk (more then 9 1 bits)
/*TODO*///	
/*TODO*///	 * ca1 byte ready input (also m6502 set overflow input)
/*TODO*///	
/*TODO*///	 * ca2 set overflow enable for 6502
/*TODO*///	 * ca3 read/write
/*TODO*///	 *
/*TODO*///	 * irq to m6502 irq connected
/*TODO*///	 */
/*TODO*///	static void vc1541_via1_irq (int level)
/*TODO*///	{
/*TODO*///		vc1541->via1irq = level;
/*TODO*///		DBG_LOG(2, "vc1541 via1 irq",("level %d %d\n",vc1541->via0irq,vc1541->via1irq));
/*TODO*///		cpu_set_irq_line (vc1541->cpunumber,
/*TODO*///						  M6502_IRQ_LINE, vc1541->via1irq || vc1541->via0irq);
/*TODO*///	}
/*TODO*///	
/*TODO*///	public static ReadHandlerPtr vc1541_via1_read_porta  = new ReadHandlerPtr() { public int handler(int offset)
/*TODO*///	{
/*TODO*///		int data=vc1541->head.data[vc1541->d64.pos];
/*TODO*///		DBG_LOG(2, "vc1541 drive",("port a read %.2x\n", data));
/*TODO*///		return data;
/*TODO*///	} };
/*TODO*///	
/*TODO*///	public static WriteHandlerPtr vc1541_via1_write_porta = new WriteHandlerPtr() {public void handler(int offset, int data)
/*TODO*///	{
/*TODO*///		DBG_LOG(1, "vc1541 drive",("port a write %.2x\n", data));
/*TODO*///	} };
/*TODO*///	
/*TODO*///	public static ReadHandlerPtr vc1541_via1_read_portb  = new ReadHandlerPtr() { public int handler(int offset)
/*TODO*///	{
/*TODO*///		UINT8 value = 0xff;
/*TODO*///	
/*TODO*///	#if 0
/*TODO*///		if (WRITEPROTECTED)
/*TODO*///			value &= ~0x10;
/*TODO*///	#endif
/*TODO*///		if (vc1541->head.sync) {
/*TODO*///			value&=~0x80;
/*TODO*///		}
/*TODO*///	
/*TODO*///		return value;
/*TODO*///	} };
/*TODO*///	
/*TODO*///	public static WriteHandlerPtr vc1541_via1_write_portb = new WriteHandlerPtr() {public void handler(int offset, int data)
/*TODO*///	{
/*TODO*///		static int old=0;
/*TODO*///		if (data!=old) {
/*TODO*///			DBG_LOG(1, "vc1541 drive",("%.2x\n", data));
/*TODO*///			if ((old&3)!=(data&3)) {
/*TODO*///				switch (old&3) {
/*TODO*///				case 0:
/*TODO*///					if ((data&3)==1) vc1541->track+=0.5;
/*TODO*///					else if ((data&3)==3) vc1541->track-=0.5;
/*TODO*///					break;
/*TODO*///				case 1:
/*TODO*///					if ((data&3)==2) vc1541->track+=0.5;
/*TODO*///					else if ((data&3)==0) vc1541->track-=0.5;
/*TODO*///					break;
/*TODO*///				case 2:
/*TODO*///					if ((data&3)==3) vc1541->track+=0.5;
/*TODO*///					else if ((data&3)==1) vc1541->track-=0.5;
/*TODO*///					break;
/*TODO*///				case 3:
/*TODO*///					if ((data&3)==0) vc1541->track+=0.5;
/*TODO*///					else if ((data&3)==2) vc1541->track-=0.5;
/*TODO*///					break;
/*TODO*///				}
/*TODO*///				if (vc1541->track<1) vc1541->track=1.0;
/*TODO*///				if (vc1541->track>35) vc1541->track=35;
/*TODO*///			}
/*TODO*///			if ( (vc1541->motor!=(data&4))||(vc1541->frequency!=(data&0x60)) ) {
/*TODO*///				double tme;
/*TODO*///				vc1541->motor = data & 4;
/*TODO*///				vc1541->frequency = data & 0x60;
/*TODO*///				tme=vc1541_times[vc1541->frequency>>5]*8*2;
/*TODO*///				if (vc1541->motor) {
/*TODO*///					if (vc1541->timer!=NULL) {
/*TODO*///						timer_reset(vc1541->timer, tme);
/*TODO*///					} else {
/*TODO*///						vc1541->timer=timer_pulse(tme, 0, vc1541_timer);
/*TODO*///					}
/*TODO*///				} else {
/*TODO*///					if (vc1541->timer!=NULL)
/*TODO*///						timer_remove(vc1541->timer);
/*TODO*///					vc1541->timer=NULL;
/*TODO*///				}
/*TODO*///			}
/*TODO*///			old=data;
/*TODO*///		}
/*TODO*///		vc1541->led = data & 8;
/*TODO*///	} };
/*TODO*///	
/*TODO*///	static struct via6522_interface via2 =
/*TODO*///	{
/*TODO*///		0,								   /*vc1541_via0_read_porta, */
/*TODO*///		vc1541_via0_read_portb,
/*TODO*///		0,								   /*via2_read_ca1, */
/*TODO*///		0,								   /*via2_read_cb1, */
/*TODO*///		0,								   /*via2_read_ca2, */
/*TODO*///		0,								   /*via2_read_cb2, */
/*TODO*///		0,								   /*via2_write_porta, */
/*TODO*///		vc1541_via0_write_portb,
/*TODO*///		0,								   /*via2_write_ca2, */
/*TODO*///		0,								   /*via2_write_cb2, */
/*TODO*///		vc1541_via0_irq
/*TODO*///	}, via3 =
/*TODO*///	{
/*TODO*///		vc1541_via1_read_porta,
/*TODO*///		vc1541_via1_read_portb,
/*TODO*///		0,								   /*via3_read_ca1, */
/*TODO*///		0,								   /*via3_read_cb1, */
/*TODO*///		0,								   /*via3_read_ca2, */
/*TODO*///		0,								   /*via3_read_cb2, */
/*TODO*///		vc1541_via1_write_porta,
/*TODO*///		vc1541_via1_write_portb,
/*TODO*///		0,								   /*via3_write_ca2, */
/*TODO*///		0,								   /*via3_write_cb2, */
/*TODO*///		vc1541_via1_irq
/*TODO*///	};
/*TODO*///	
/*TODO*///	int vc1541_init (int id)
/*TODO*///	{
/*TODO*///		FILE *in;
/*TODO*///		int size;
/*TODO*///	
/*TODO*///		/*memset (&(drive->d64), 0, sizeof (drive->d64)); */
/*TODO*///		in = (FILE*)image_fopen (IO_FLOPPY, id, OSD_FILETYPE_IMAGE, 0);
/*TODO*///		if (in == 0)
/*TODO*///			return INIT_FAIL;
/*TODO*///	
/*TODO*///		size = osd_fsize (in);
/*TODO*///		if (!(vc1541->d64.data = (UINT8*)malloc (size)))
/*TODO*///		{
/*TODO*///			osd_fclose (in);
/*TODO*///			return INIT_FAIL;
/*TODO*///		}
/*TODO*///		if (size != osd_fread (in, vc1541->d64.data, size))
/*TODO*///		{
/*TODO*///			free (vc1541->d64.data);
/*TODO*///			osd_fclose (in);
/*TODO*///			return INIT_FAIL;
/*TODO*///		}
/*TODO*///		osd_fclose (in);
/*TODO*///	
/*TODO*///		logerror("floppy image %s loaded\n", device_filename(IO_FLOPPY, id));
/*TODO*///	
/*TODO*///		/*vc1541->drive = ; */
/*TODO*///		vc1541->d64.image_type = IO_FLOPPY;
/*TODO*///		vc1541->d64.image_id = id;
/*TODO*///		return INIT_PASS;
/*TODO*///	}
/*TODO*///	
/*TODO*///	void vc1541_exit(int id)
/*TODO*///	{
/*TODO*///		/* writeback of image data */
/*TODO*///		free(vc1541->d64.data);vc1541->d64.data=NULL;
/*TODO*///	}
/*TODO*///	
/*TODO*///	int vc1541_config (int id, int mode, VC1541_CONFIG *config)
/*TODO*///	{
/*TODO*///		via_config (2, &via2);
/*TODO*///		via_config (3, &via3);
/*TODO*///		vc1541->type=TypeVC1541;
/*TODO*///		vc1541->cpunumber = config->cpunr;
/*TODO*///		vc1541->drive.serial.deviceid = config->devicenr;
/*TODO*///		return 0;
/*TODO*///	}
/*TODO*///	
        public static void vc1541_reset ()
	{
		int i;
	
/*TODO*///		if (vc1541.type==TypeVC1541) {
/*TODO*///			for (i = 0; i < serial.atn.length; i++)
/*TODO*///			{
/*TODO*///				serial.atn[i] = serial.data[i] = serial.clock[i] = 1;
/*TODO*///			}
/*TODO*///		}
/*TODO*///		vc1541.track=1.0;
/*TODO*///		if ((vc1541.type==TypeVC1541)||(vc1541.type==Type2031)) {
/*TODO*///			via_reset ();
/*TODO*///		}
/*TODO*///		if ((vc1541.type==TypeC1551)) {
/*TODO*///			tpi6525_0_reset();
/*TODO*///		}
	}
/*TODO*///	
/*TODO*///	/* delivers status for displaying */
/*TODO*///	extern void vc1541_drive_status (char *text, int size)
/*TODO*///	{
/*TODO*///	#if 1||VERBOSE_DBG
/*TODO*///		if (vc1541->type==TypeVC1541) {
/*TODO*///			snprintf (text, size, "%s %4.1f %s %.2x %s %s %s",
/*TODO*///					  vc1541->led ? "LED" : "led",
/*TODO*///					  vc1541->track,
/*TODO*///					  vc1541->motor ? "MOTOR" : "motor",
/*TODO*///					  vc1541->frequency,
/*TODO*///					  serial.atn[0]?"ATN":"atn",
/*TODO*///					  serial.clock[0]?"CLOCK":"clock",
/*TODO*///					  serial.data[0]?"DATA":"data");
/*TODO*///		} else if (vc1541->type==TypeC1551) {
/*TODO*///			snprintf (text, size, "%s %4.1f %s %.2x",
/*TODO*///					  vc1541->led ? "LED" : "led",
/*TODO*///					  vc1541->track,
/*TODO*///					  vc1541->motor ? "MOTOR" : "motor",
/*TODO*///					  vc1541->frequency);
/*TODO*///		}
/*TODO*///	#else
/*TODO*///		text[0] = 0;
/*TODO*///	#endif
/*TODO*///		return;
/*TODO*///	}
/*TODO*///	
/*TODO*///	void vc1541_serial_reset_write (int which, int level)
/*TODO*///	{
/*TODO*///	}
/*TODO*///	
/*TODO*///	int vc1541_serial_atn_read (int which)
/*TODO*///	{
/*TODO*///	#ifdef CPU_SYNC
/*TODO*///			if (cpu_getactivecpu()==0) cpu_sync();
/*TODO*///	#endif
/*TODO*///		return serial.atn[0];
/*TODO*///	}
/*TODO*///	
/*TODO*///	void vc1541_serial_atn_write (int which, int level)
/*TODO*///	{
/*TODO*///	#if 0
/*TODO*///		int value;
/*TODO*///	#endif
/*TODO*///		if (serial.atn[1 + which] != level)
/*TODO*///		{
/*TODO*///	#ifdef CPU_SYNC
/*TODO*///			if (cpu_getactivecpu()==0) cpu_sync();
/*TODO*///	#endif
/*TODO*///			serial.atn[1 + which] = level;
/*TODO*///			if (serial.atn[0] != level)
/*TODO*///			{
/*TODO*///				serial.atn[0] = serial.atn[1] && serial.atn[2];
/*TODO*///				if (serial.atn[0] == level)
/*TODO*///				{
/*TODO*///					DBG_LOG(1, "vc1541",("%d:%.4x atn %s\n",
/*TODO*///										 cpu_getactivecpu (),
/*TODO*///										 cpu_get_pc(),
/*TODO*///										 serial.atn[0]?"ATN":"atn"));
/*TODO*///					via_set_input_ca1 (2, !level);
/*TODO*///	#if 0
/*TODO*///					value=vc1541->drive.serial.data;
/*TODO*///					if (vc1541->drive.serial.acka!=!level) value=0;
/*TODO*///					if (value!=serial.data[2]) {
/*TODO*///						serial.data[2]=value;
/*TODO*///						if (serial.data[0]!=value) {
/*TODO*///							serial.data[0]=serial.data[1] && serial.data[2];
/*TODO*///						}
/*TODO*///					}
/*TODO*///	#endif
/*TODO*///				}
/*TODO*///			}
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	int vc1541_serial_data_read (int which)
/*TODO*///	{
/*TODO*///	#ifdef CPU_SYNC
/*TODO*///			if (cpu_getactivecpu()==0) cpu_sync();
/*TODO*///	#endif
/*TODO*///		return serial.data[0];
/*TODO*///	}
/*TODO*///	
/*TODO*///	void vc1541_serial_data_write (int which, int level)
/*TODO*///	{
/*TODO*///		if (serial.data[1 + which] != level)
/*TODO*///		{
/*TODO*///	#ifdef CPU_SYNC
/*TODO*///			if (cpu_getactivecpu()==0) cpu_sync();
/*TODO*///	#endif
/*TODO*///			serial.data[1 + which] = level;
/*TODO*///			if (serial.data[0] != level)
/*TODO*///			{
/*TODO*///				serial.data[0] = serial.data[1] && serial.data[2];
/*TODO*///				if (serial.data[0] == level)
/*TODO*///				{
/*TODO*///					DBG_LOG(1, "vc1541",("%d:%.4x data %s\n",
/*TODO*///										 cpu_getactivecpu (),
/*TODO*///										 cpu_get_pc(),
/*TODO*///										 serial.data[0]?"DATA":"data"));
/*TODO*///				}
/*TODO*///			}
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	int vc1541_serial_clock_read (int which)
/*TODO*///	{
/*TODO*///	#ifdef CPU_SYNC
/*TODO*///			if (cpu_getactivecpu()==0) cpu_sync();
/*TODO*///	#endif
/*TODO*///		return serial.clock[0];
/*TODO*///	}
/*TODO*///	
/*TODO*///	void vc1541_serial_clock_write (int which, int level)
/*TODO*///	{
/*TODO*///		if (serial.clock[1 + which] != level)
/*TODO*///		{
/*TODO*///	#ifdef CPU_SYNC
/*TODO*///			if (cpu_getactivecpu()==0) cpu_sync();
/*TODO*///	#endif
/*TODO*///			serial.clock[1 + which] = level;
/*TODO*///			if (serial.clock[0] != level)
/*TODO*///			{
/*TODO*///				serial.clock[0] = serial.clock[1] && serial.clock[2];
/*TODO*///				if (serial.clock[0] == level)
/*TODO*///				{
/*TODO*///					DBG_LOG(1, "vc1541",("%d:%.4x clock %s\n",
/*TODO*///										 cpu_getactivecpu (),
/*TODO*///										 cpu_get_pc(),
/*TODO*///										 serial.clock[0]?"CLOCK":"clock"));
/*TODO*///				}
/*TODO*///			}
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	int vc1541_serial_request_read (int which)
/*TODO*///	{
/*TODO*///		return 1;
/*TODO*///	}
/*TODO*///	
/*TODO*///	void vc1541_serial_request_write (int which, int level)
/*TODO*///	{
/*TODO*///	}
/*TODO*///	
/*TODO*///	/*
/*TODO*///	  c1551 irq line
/*TODO*///	  only timing related??? (60 hz?), delivered from c16?
/*TODO*///	 */
/*TODO*///	static void c1551_timer(int param)
/*TODO*///	{
/*TODO*///		cpu_set_irq_line(vc1541->cpunumber, M6502_IRQ_LINE, PULSE_LINE);
/*TODO*///	}
/*TODO*///	
/*TODO*///	/*
/*TODO*///	  ddr 0x6f (p0 .. p5? in pinout)
/*TODO*///	  0,1: output stepper motor
/*TODO*///	  2 output motor (rotation) (300 revolutions per minute)
/*TODO*///	  3 output led ?
/*TODO*///	  4 input disk not write protected ?
/*TODO*///	  5,6: output frequency select
/*TODO*///	  7: input byte ready
/*TODO*///	 */
/*TODO*///	static WRITE_HANDLER ( c1551_port_w )
/*TODO*///	{
/*TODO*///		static int old=0;
/*TODO*///		if (offset) {
/*TODO*///			DBG_LOG(1, "c1551 port",("write %.2x\n",data));
/*TODO*///			vc1541->drive.c1551.cpu_port=data;
/*TODO*///	
/*TODO*///			if (data!=old) {
/*TODO*///				DBG_LOG(1, "vc1541 drive",("%.2x\n", data));
/*TODO*///				if ((old&3)!=(data&3)) {
/*TODO*///					switch (old&3) {
/*TODO*///					case 0:
/*TODO*///						if ((data&3)==1) vc1541->track+=0.5;
/*TODO*///						else if ((data&3)==3) vc1541->track-=0.5;
/*TODO*///						break;
/*TODO*///					case 1:
/*TODO*///						if ((data&3)==2) vc1541->track+=0.5;
/*TODO*///						else if ((data&3)==0) vc1541->track-=0.5;
/*TODO*///						break;
/*TODO*///					case 2:
/*TODO*///						if ((data&3)==3) vc1541->track+=0.5;
/*TODO*///						else if ((data&3)==1) vc1541->track-=0.5;
/*TODO*///						break;
/*TODO*///					case 3:
/*TODO*///						if ((data&3)==0) vc1541->track+=0.5;
/*TODO*///						else if ((data&3)==2) vc1541->track-=0.5;
/*TODO*///						break;
/*TODO*///					}
/*TODO*///					if (vc1541->track<1) vc1541->track=1.0;
/*TODO*///					if (vc1541->track>35) vc1541->track=35;
/*TODO*///				}
/*TODO*///				if ( (vc1541->motor!=(data&4))||(vc1541->frequency!=(data&0x60)) ) {
/*TODO*///					double tme;
/*TODO*///					vc1541->motor = data & 4;
/*TODO*///					vc1541->frequency = data & 0x60;
/*TODO*///					tme=vc1541_times[vc1541->frequency>>5]*8*2;
/*TODO*///					if (vc1541->motor) {
/*TODO*///						if (vc1541->timer!=NULL) {
/*TODO*///							timer_reset(vc1541->timer, tme);
/*TODO*///						} else {
/*TODO*///							vc1541->timer=timer_pulse(tme, 0, vc1541_timer);
/*TODO*///						}
/*TODO*///					} else {
/*TODO*///						if (vc1541->timer!=NULL)
/*TODO*///							timer_remove(vc1541->timer);
/*TODO*///						vc1541->timer=NULL;
/*TODO*///					}
/*TODO*///				}
/*TODO*///				old=data;
/*TODO*///			}
/*TODO*///			vc1541->led = data & 8;
/*TODO*///		} else {
/*TODO*///			vc1541->drive.c1551.cpu_ddr=data;
/*TODO*///			DBG_LOG(1, "c1551 ddr",("write %.2x\n",data));
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	static READ_HANDLER ( c1551_port_r )
/*TODO*///	{
/*TODO*///		int data;
/*TODO*///	
/*TODO*///		if (offset) {
/*TODO*///			data=0x7f;
/*TODO*///	#if 0
/*TODO*///			if (WRITEPROTECTED)
/*TODO*///				data &= ~0x10;
/*TODO*///	#endif
/*TODO*///			if (vc1541->head.ready) {
/*TODO*///				data|=0x80;
/*TODO*///				vc1541->head.ready=0;
/*TODO*///			}
/*TODO*///			data&=~vc1541->drive.c1551.cpu_ddr;
/*TODO*///			data|=vc1541->drive.c1551.cpu_ddr&vc1541->drive.c1551.cpu_port;
/*TODO*///			DBG_LOG(3, "c1551 port",("read %.2x\n", data));
/*TODO*///		} else {
/*TODO*///			data=vc1541->drive.c1551.cpu_ddr;
/*TODO*///			DBG_LOG(3, "c1551 ddr",("read %.2x\n", data));
/*TODO*///		}
/*TODO*///		return data;
/*TODO*///	}
/*TODO*///	
/*TODO*///	/*
/*TODO*///	   tia6523
/*TODO*///	   port a
/*TODO*///	    c16 communication in/out
/*TODO*///	   port b
/*TODO*///	    drive data in/out
/*TODO*///	   port c ddr (0x1f)
/*TODO*///	    0 output status out
/*TODO*///		1 output status out
/*TODO*///		2 output
/*TODO*///		3 output handshake out
/*TODO*///		4 output
/*TODO*///		5 input drive number 9
/*TODO*///		6 input sync 0 active
/*TODO*///		7 input handshake in
/*TODO*///	 */
/*TODO*///	static int c1551_port_c_r(void)
/*TODO*///	{
/*TODO*///		int data=0xff;
/*TODO*///		data&=~0x20;
/*TODO*///		if (vc1541->head.sync) data&=~0x40;
/*TODO*///		return data;
/*TODO*///	}
/*TODO*///	
/*TODO*///	static int c1551_port_b_r (void)
/*TODO*///	{
/*TODO*///		int data=vc1541->head.data[vc1541->d64.pos];
/*TODO*///		DBG_LOG(2, "c1551 drive",("port a read %.2x\n", data));
/*TODO*///		return data;
/*TODO*///	}
/*TODO*///	
/*TODO*///	int c1551_config (int id, int mode, C1551_CONFIG *config)
/*TODO*///	{
/*TODO*///		vc1541->cpunumber = config->cpunr;
/*TODO*///		vc1541->type=TypeC1551;
/*TODO*///		tpi6525[0].c.read=c1551_port_c_r;
/*TODO*///		tpi6525[0].b.read=c1551_port_b_r;
/*TODO*///		if (vc1541->drive.c1551.timer==NULL) {
/*TODO*///			/* time should be small enough to allow quitting of the irq
/*TODO*///			   line before the next interrupt is triggered */
/*TODO*///			vc1541->drive.c1551.timer=timer_pulse(1.0/60, 0, c1551_timer);
/*TODO*///		}
/*TODO*///		return 0;
/*TODO*///	}
/*TODO*///	
/*TODO*///	MEMORY_READ_START( c1551_readmem )
/*TODO*///	    {0x0000, 0x0001, c1551_port_r},
/*TODO*///		{0x0002, 0x07ff, MRA_RAM},
/*TODO*///	    {0x4000, 0x4007, tpi6525_0_port_r},
/*TODO*///		{0xc000, 0xffff, MRA_ROM},
/*TODO*///	MEMORY_END
/*TODO*///	
/*TODO*///	MEMORY_WRITE_START( c1551_writemem )
/*TODO*///	    {0x0000, 0x0001, c1551_port_w},
/*TODO*///		{0x0002, 0x07ff, MWA_RAM},
/*TODO*///	    {0x4000, 0x4007, tpi6525_0_port_w},
/*TODO*///		{0xc000, 0xffff, MWA_ROM},
/*TODO*///	MEMORY_END
/*TODO*///	
/*TODO*///	void c1551x_write_data (TPI6525 *This, int data)
/*TODO*///	{
/*TODO*///		DBG_LOG(1, "c1551 cpu", ("%d write data %.2x\n",
/*TODO*///							 cpu_getactivecpu (), data));
/*TODO*///	#ifdef CPU_SYNC
/*TODO*///		cpu_sync();
/*TODO*///	#endif
/*TODO*///		tpi6525_0_port_a_w(0,data);
/*TODO*///	}
/*TODO*///	
/*TODO*///	int c1551x_read_data (TPI6525 *This)
/*TODO*///	{
/*TODO*///		int data=0xff;
/*TODO*///	#ifdef CPU_SYNC
/*TODO*///		cpu_sync ();
/*TODO*///	#endif
/*TODO*///		data=tpi6525_0_port_a_r(0);
/*TODO*///		DBG_LOG(2, "c1551 cpu",("%d read data %.2x\n",
/*TODO*///							 cpu_getactivecpu (), data));
/*TODO*///		return data;
/*TODO*///	}
/*TODO*///	
/*TODO*///	void c1551x_write_handshake (TPI6525 *This, int data)
/*TODO*///	{
/*TODO*///		DBG_LOG(1, "c1551 cpu",("%d write handshake %.2x\n",
/*TODO*///							 cpu_getactivecpu (), data));
/*TODO*///	#ifdef CPU_SYNC
/*TODO*///		cpu_sync();
/*TODO*///	#endif
/*TODO*///		tpi6525_0_port_c_w(0,data&0x40?0xff:0x7f);
/*TODO*///	}
/*TODO*///	
/*TODO*///	int c1551x_read_handshake (TPI6525 *This)
/*TODO*///	{
/*TODO*///		int data=0xff;
/*TODO*///	#ifdef CPU_SYNC
/*TODO*///		cpu_sync();
/*TODO*///	#endif
/*TODO*///		data=tpi6525_0_port_c_r(0)&8?0x80:0;
/*TODO*///		DBG_LOG(2, "c1551 cpu",("%d read handshake %.2x\n",
/*TODO*///							 cpu_getactivecpu (), data));
/*TODO*///		return data;
/*TODO*///	}
/*TODO*///	
/*TODO*///	int c1551x_read_status (TPI6525 *This)
/*TODO*///	{
/*TODO*///		int data=0xff;
/*TODO*///	#ifdef CPU_SYNC
/*TODO*///		cpu_sync();
/*TODO*///	#endif
/*TODO*///		data=tpi6525_0_port_c_r(0)&3;
/*TODO*///		DBG_LOG(1, "c1551 cpu",("%d read status %.2x\n",
/*TODO*///							 cpu_getactivecpu (), data));
/*TODO*///		return data;
/*TODO*///	}
/*TODO*///	
/*TODO*///	void c1551x_0_write_data (int data)
/*TODO*///	{
/*TODO*///		c1551x_write_data(tpi6525, data);
/*TODO*///	}
/*TODO*///	
/*TODO*///	int c1551x_0_read_data (void)
/*TODO*///	{
/*TODO*///		return c1551x_read_data(tpi6525);
/*TODO*///	}
/*TODO*///	
/*TODO*///	void c1551x_0_write_handshake (int data)
/*TODO*///	{
/*TODO*///		c1551x_write_handshake(tpi6525, data);
/*TODO*///	}
/*TODO*///	
/*TODO*///	int c1551x_0_read_handshake (void)
/*TODO*///	{
/*TODO*///		return c1551x_read_handshake(tpi6525);
/*TODO*///	}
/*TODO*///	
/*TODO*///	int c1551x_0_read_status (void)
/*TODO*///	{
/*TODO*///		return c1551x_read_status(tpi6525);
/*TODO*///	}
}
