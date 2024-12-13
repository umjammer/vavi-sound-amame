/***************************************************************************

       commodore vc1541 floppy disk drive

***************************************************************************/
/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.includes;

public class vc1541H
{
	
/*TODO*///	#ifdef PET_TEST_CODE
/*TODO*///	/* test with preliminary VC1541 emulation */
/*TODO*///	#define VC1541
/*TODO*///	/*#define CPU_SYNC */
/*TODO*///	#endif
/*TODO*///	
/*TODO*///	extern const struct Memory_ReadAddress vc1541_readmem[];
/*TODO*///	extern const struct Memory_WriteAddress vc1541_writemem[];
/*TODO*///	
/*TODO*///	extern const struct Memory_ReadAddress dolphin_readmem[];
/*TODO*///	extern const struct Memory_WriteAddress dolphin_writemem[];
/*TODO*///	
/*TODO*///	extern const struct Memory_ReadAddress c1551_readmem[];
/*TODO*///	extern const struct Memory_WriteAddress c1551_writemem[];
/*TODO*///	
/*TODO*///	typedef struct {
/*TODO*///		int cpunr;
/*TODO*///		int devicenr;
/*TODO*///	} VC1541_CONFIG;
/*TODO*///	
/*TODO*///	int vc1541_init(int id);
/*TODO*///	void vc1541_exit(int id);
/*TODO*///	
/*TODO*///	int vc1541_config(int id, int mode, VC1541_CONFIG*config);
/*TODO*///	void vc1541_reset(void);
/*TODO*///	void vc1541_drive_status(char *text, int size);
/*TODO*///	
/*TODO*///	typedef struct {
/*TODO*///		int cpunr;
/*TODO*///	} C1551_CONFIG;
/*TODO*///	
/*TODO*///	int c1551_config(int id, int mode, C1551_CONFIG*config);
/*TODO*///	#define c1551_reset vc1541_reset
/*TODO*///	
/*TODO*///	#define IODEVICE_VC1541 \
/*TODO*///	{\
/*TODO*///	   IO_FLOPPY,          /* type */\
/*TODO*///	   1,                                      /* count */\
/*TODO*///	   "d64\0",            /* G64 later *//*file extensions */\
/*TODO*///	   IO_RESET_CPU,       /* reset if file changed */\
/*TODO*///	   NULL,               /* id */\
/*TODO*///	   vc1541_init,        /* init */\
/*TODO*///	   vc1541_exit,        /* exit */\
/*TODO*///	   NULL,               /* info */\
/*TODO*///	   (int(*)(int,int,void*))vc1541_config,      /* open */\
/*TODO*///	   NULL,               /* close */\
/*TODO*///	   NULL,               /* status */\
/*TODO*///	   NULL,               /* seek */\
/*TODO*///	   NULL,               /* input */\
/*TODO*///	   NULL,               /* output */\
/*TODO*///	   NULL,               /* input_chunk */\
/*TODO*///	   NULL                /* output_chunk */\
/*TODO*///	}
/*TODO*///	
/*TODO*///	#define IODEVICE_C2031 IODEVICE_VC1541
/*TODO*///	
/*TODO*///	#define IODEVICE_C1551 \
/*TODO*///	{\
/*TODO*///	   IO_FLOPPY,          /* type */\
/*TODO*///	   1,                                      /* count */\
/*TODO*///	   "d64\0",            /* G64 later *//*file extensions */\
/*TODO*///	   IO_RESET_CPU,       /* reset if file changed */\
/*TODO*///	   NULL,               /* id */\
/*TODO*///	   vc1541_init,        /* init */\
/*TODO*///	   vc1541_exit,        /* exit */\
/*TODO*///	   NULL,               /* info */\
/*TODO*///	   (int(*)(int,int,void*))c1551_config,      /* open */\
/*TODO*///	   NULL,               /* close */\
/*TODO*///	   NULL,               /* status */\
/*TODO*///	   NULL,               /* seek */\
/*TODO*///	   NULL,               /* input */\
/*TODO*///	   NULL,               /* output */\
/*TODO*///	   NULL,               /* input_chunk */\
/*TODO*///	   NULL                /* output_chunk */\
/*TODO*///	}
/*TODO*///	
/*TODO*///	#define IODEVICE_C1571 \
/*TODO*///	{\
/*TODO*///	   IO_FLOPPY,          /* type */\
/*TODO*///	   1,                                      /* count */\
/*TODO*///	   "d64\0",            /* G64 later *//*file extensions */\
/*TODO*///	   IO_RESET_CPU,       /* reset if file changed */\
/*TODO*///	   NULL,               /* id */\
/*TODO*///	   vc1541_init,        /* init */\
/*TODO*///	   vc1541_exit,        /* exit */\
/*TODO*///	   NULL,               /* info */\
/*TODO*///	   (int(*)(int,int,void*))vc1541_config,      /* open */\
/*TODO*///	   NULL,               /* close */\
/*TODO*///	   NULL,               /* status */\
/*TODO*///	   NULL,               /* seek */\
/*TODO*///	   NULL,               /* input */\
/*TODO*///	   NULL,               /* output */\
/*TODO*///	   NULL,               /* input_chunk */\
/*TODO*///	   NULL                /* output_chunk */\
/*TODO*///	}
/*TODO*///	
/*TODO*///	#define VC1540_CPU \
/*TODO*///	          {\
/*TODO*///				CPU_M6502,\
/*TODO*///				1000000,\
/*TODO*///				vc1541_readmem,vc1541_writemem,\
/*TODO*///				0,0,\
/*TODO*///				0,0,\
/*TODO*///	       	  }
/*TODO*///	
/*TODO*///	#define VC1541_CPU VC1540_CPU
/*TODO*///	#define C2031_CPU VC1540_CPU
/*TODO*///	
/*TODO*///	#define DOLPHIN_CPU \
/*TODO*///	          {\
/*TODO*///				CPU_M6502,\
/*TODO*///				1000000,\
/*TODO*///				dolphin_readmem,dolphin_writemem,\
/*TODO*///				0,0,\
/*TODO*///				0,0,\
/*TODO*///			  }
/*TODO*///	
/*TODO*///	#define C1551_CPU \
/*TODO*///	          {\
/*TODO*///				CPU_M6510T,\
/*TODO*///				2000000,/* ??? reading seems to need more than 1 mhz */\
/*TODO*///				c1551_readmem,c1551_writemem,\
/*TODO*///				0,0,\
/*TODO*///				0,0,\
/*TODO*///	       	  }
/*TODO*///	
/*TODO*///	/* will follow later */
/*TODO*///	#define C1571_CPU VC1541_CPU
/*TODO*///	
/*TODO*///	#define VC1540_ROM(cpu) \
/*TODO*///		ROM_REGION(0x10000,cpu,0);\
/*TODO*///		ROM_LOAD("325302.01",  0xc000, 0x2000, 0x29ae9752);\
/*TODO*///		ROM_LOAD("325303.01",  0xe000, 0x2000, 0x10b39158);
/*TODO*///	#define C2031_ROM(cpu) \
/*TODO*///			ROM_REGION(0x10000,cpu,0);\
/*TODO*///			ROM_LOAD("dos2031",  0xc000, 0x4000, 0x21b80fdf);
/*TODO*///	#if 1
/*TODO*///	#define VC1541_ROM(cpu) \
/*TODO*///		ROM_REGION(0x10000,cpu,0);\
/*TODO*///		ROM_LOAD("325302.01",  0xc000, 0x2000, 0x29ae9752);\
/*TODO*///		ROM_LOAD("901229.05",  0xe000, 0x2000, 0x361c9f37);#else
/*TODO*///	/* for this I have the documented rom listing in german */
/*TODO*///	#define VC1541_ROM(cpu) \
/*TODO*///		ROM_REGION(0x10000,cpu,0);\
/*TODO*///		ROM_LOAD("325302.01",  0xc000, 0x2000, 0x29ae9752);\
/*TODO*///		ROM_LOAD("901229.03",  0xe000, 0x2000, 0x9126e74a);#endif
/*TODO*///	
/*TODO*///	#define DOLPHIN_ROM(cpu) \
/*TODO*///		ROM_REGION(0x10000,cpu,0);\
/*TODO*///		ROM_LOAD("c1541.rom",  0xa000, 0x6000, 0xbd8e42b2);
/*TODO*///	#define C1551_ROM(cpu) \
/*TODO*///		ROM_REGION(0x10000,cpu,0);\
/*TODO*///		ROM_LOAD("318008.01",  0xc000, 0x4000, 0x6d16d024);
/*TODO*///	#define C1570_ROM(cpu) \
/*TODO*///		ROM_REGION(0x10000,cpu,0);\
/*TODO*///		ROM_LOAD("315090.01",  0x8000, 0x8000, 0x5a0c7937);
/*TODO*///	#define C1571_ROM(cpu) \
/*TODO*///		ROM_REGION(0x10000,cpu,0);\
/*TODO*///		ROM_LOAD("310654.03",  0x8000, 0x8000, 0x3889b8b8);
/*TODO*///	#if 0
/*TODO*///		ROM_LOAD("dos2040",  0x?000, 0x2000, 0xd04c1fbb);
/*TODO*///		ROM_LOAD("dos3040",  0x?000, 0x3000, 0xf4967a7f);
/*TODO*///		ROM_LOAD("dos4040",  0x?000, 0x3000, 0x40e0ebaa);
/*TODO*///		ROM_LOAD("dos1001",  0xc000, 0x4000, 0x87e6a94e);
/*TODO*///		/* vc1541 drive hardware */
/*TODO*///		ROM_LOAD("dos2031",  0xc000, 0x4000, 0x21b80fdf);
/*TODO*///		ROM_LOAD("1540-c000.325302-01.bin",  0xc000, 0x2000, 0x29ae9752);	ROM_LOAD("1540-e000.325303-01.bin",  0xe000, 0x2000, 0x10b39158);
/*TODO*///		ROM_LOAD("1541-e000.901229-01.bin",  0xe000, 0x2000, 0x9a48d3f0);	ROM_LOAD("1541-e000.901229-02.bin",  0xe000, 0x2000, 0xb29bab75);	ROM_LOAD("1541-e000.901229-03.bin",  0xe000, 0x2000, 0x9126e74a);	ROM_LOAD("1541-e000.901229-05.bin",  0xe000, 0x2000, 0x361c9f37);
/*TODO*///		ROM_LOAD("1541-II.251968-03.bin",  0xe000, 0x2000, 0x899fa3c5);
/*TODO*///		ROM_LOAD("1541C.251968-01.bin",  0xc000, 0x4000, 0x1b3ca08d);	ROM_LOAD("1541C.251968-02.bin",  0xc000, 0x4000, 0x2d862d20);
/*TODO*///		ROM_LOAD("dos1541.c0",  0xc000, 0x2000, 0x5b84bcef);	ROM_LOAD("dos1541.e0",  0xe000, 0x2000, 0x2d8c1fde);	 /* merged gives 0x899fa3c5 */
/*TODO*///	
/*TODO*///		 /* 0x29ae9752 and 0x361c9f37 merged */
/*TODO*///		ROM_LOAD("vc1541",  0xc000, 0x4000, 0x57224cde);
/*TODO*///		 /* 0x29ae9752 and 0xb29bab75 merged */
/*TODO*///		ROM_LOAD("vc1541",  0xc000, 0x4000, 0xd3a5789c);
/*TODO*///		/* dolphin vc1541 */
/*TODO*///		ROM_LOAD("c1541.rom",  0xa000, 0x6000, 0xbd8e42b2);
/*TODO*///		ROM_LOAD("1551.318008-01.bin",  0xc000, 0x4000, 0x6d16d024);
/*TODO*///		/* bug fixes introduced bugs for 1541 mode
/*TODO*///		 jiffydos to have fixed 1571 and working 1541 mode */
/*TODO*///		ROM_LOAD("1570-rom.315090-01.bin",  0x8000, 0x8000, 0x5a0c7937);	ROM_LOAD("1571-rom.310654-03.bin",  0x8000, 0x8000, 0x3889b8b8);	ROM_LOAD("1571-rom.310654-05.bin",  0x8000, 0x8000, 0x5755bae3);	ROM_LOAD("1571cr-rom.318047-01.bin",  0x8000, 0x8000, 0xf24efcc4);
/*TODO*///		ROM_LOAD("1581-rom.318045-01.bin",  0x8000, 0x8000, 0x113af078);	ROM_LOAD("1581-rom.318045-02.bin",  0x8000, 0x8000, 0xa9011b84);	ROM_LOAD("1581-rom.beta.bin",  0x8000, 0x8000, 0xecc223cd);	/* modified drive 0x2000-0x3ffe ram, 0x3fff 6529 */
/*TODO*///		ROM_LOAD("1581rom5.bin",  0x8000, 0x8000, 0xe08801d7);
/*TODO*///		ROM_LOAD("",  0xc000, 0x4000, 0x);#endif
/*TODO*///	
/*TODO*///	/* serial bus vc20/c64/c16/vc1541 and some printer */
/*TODO*///	
/*TODO*///	#ifdef VC1541
/*TODO*///	#define cbm_serial_reset_write(level)   vc1541_serial_reset_write(0,level)
/*TODO*///	#define cbm_serial_atn_read()           vc1541_serial_atn_read(0)
/*TODO*///	#define cbm_serial_atn_write(level)     vc1541_serial_atn_write(0,level)
/*TODO*///	#define cbm_serial_data_read()          vc1541_serial_data_read(0)
/*TODO*///	#define cbm_serial_data_write(level)    vc1541_serial_data_write(0,level)
/*TODO*///	#define cbm_serial_clock_read()         vc1541_serial_clock_read(0)
/*TODO*///	#define cbm_serial_clock_write(level)   vc1541_serial_clock_write(0,level)
/*TODO*///	#define cbm_serial_request_read()       vc1541_serial_request_read(0)
/*TODO*///	#define cbm_serial_request_write(level) vc1541_serial_request_write(0,level)
/*TODO*///	#endif
/*TODO*///	
/*TODO*///	void vc1541_serial_reset_write(int which,int level);
/*TODO*///	int vc1541_serial_atn_read(int which);
/*TODO*///	void vc1541_serial_atn_write(int which,int level);
/*TODO*///	int vc1541_serial_data_read(int which);
/*TODO*///	void vc1541_serial_data_write(int which,int level);
/*TODO*///	int vc1541_serial_clock_read(int which);
/*TODO*///	void vc1541_serial_clock_write(int which,int level);
/*TODO*///	int vc1541_serial_request_read(int which);
/*TODO*///	void vc1541_serial_request_write(int which,int level);
/*TODO*///	
/*TODO*///	void c1551x_0_write_data (int data);
/*TODO*///	int c1551x_0_read_data (void);
/*TODO*///	void c1551x_0_write_handshake (int data);
/*TODO*///	int c1551x_0_read_handshake (void);
/*TODO*///	int c1551x_0_read_status (void);
}
