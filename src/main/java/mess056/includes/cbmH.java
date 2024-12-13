/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.includes;

import static common.ptr.*;
import static mess056.deviceH.*;
import static mess056.machine.cbm.*;
import static mess056.messH.*;

public class cbmH
{
	
	/* must be defined until some driver init problems are solved */
	public static int NEW_GAMEDRIVER;
	
	/* global header file for
	 * vc20
	 * c16
	 * c64
	 * c128
	 * c65*/
	
	/*TODO*///#if 0
/*TODO*///	//#else
/*TODO*///	/* quick (and unsafe as sprintf) snprintf */
/*TODO*///	#define snprintf cbm_snprintf
/*TODO*///	int DECL_SPEC cbm_snprintf (char *str, size_t size, const char *format,...);
/*TODO*///	#endif
/*TODO*///	
/*TODO*///	#define memset16 cbm_memset16
/*TODO*///	void *cbm_memset16 (void *dest, int value, size_t size);
/*TODO*///	
/*TODO*///	/**************************************************************************
/*TODO*///	 * Logging
/*TODO*///	 * call the XXX_LOG with XXX_LOG("info",(errorlog,"%fmtn",args));
/*TODO*///	 * where "info" can also be 0 to append .."%fmt",args to a line.
/*TODO*///	 **************************************************************************/
/*TODO*///	#define LOG(LEVEL,N,M,A)  
/*TODO*///	        { 
/*TODO*///		  if(LEVEL>=N) { 
/*TODO*///		    if( M ) 
/*TODO*///	              logerror("%11.6f: %-24s",timer_get_time(), (char*)M );
/*TODO*///		    logerror A; 
/*TODO*///		  } 
/*TODO*///	        }
/*TODO*///	
/*TODO*///	/* debugging level here for all on or off */
/*TODO*///	#if 1
/*TODO*///	# ifdef VERBOSE_DBG
/*TODO*///	#  undef VERBOSE_DBG
/*TODO*///	# endif
/*TODO*///	# if 1
/*TODO*///	#  define VERBOSE_DBG 0
/*TODO*///	# else
/*TODO*///	#  define VERBOSE_DBG 1
/*TODO*///	# endif
/*TODO*///	#else
/*TODO*///	# define PET_TEST_CODE
/*TODO*///	#endif
/*TODO*///	
/*TODO*///	#if VERBOSE_DBG
/*TODO*///	#define DBG_LOG(n,m,a) LOG(VERBOSE_DBG,n,m,a)
/*TODO*///	#else
/*TODO*///	#define DBG_LOG(n,m,a)
/*TODO*///	#endif
/*TODO*///	
/*TODO*///	#if 0
/*TODO*///	#ifndef __cplusplus
/*TODO*///	typedef int bool;
/*TODO*///	#endif
/*TODO*///	
/*TODO*///	#ifndef true
/*TODO*///	#define true 1
/*TODO*///	#endif
/*TODO*///	#ifndef false
/*TODO*///	#define false 0
/*TODO*///	#endif
/*TODO*///	#endif
/*TODO*///	
/*TODO*///	void cbm_quick_exit (int id);
/*TODO*///	int cbm_quick_init (int id);
/*TODO*///	/* pet with basic 1 */
/*TODO*///	int cbm_pet1_quick_open (int id, int mode, void *arg);
/*TODO*///	int cbm_pet_quick_open (int id, int mode, void *arg);
/*TODO*///	int cbm_quick_open (int id, int mode, void *arg);
/*TODO*///	int cbmb_quick_open (int id, int mode, void *arg);
/*TODO*///	int cbm500_quick_open (int id, int mode, void *arg);
/*TODO*///	int cbm_c65_quick_open (int id, int mode, void *arg);
/*TODO*///	
	/*TODO*///#define IODEVICE_CBM_PET1_QUICK 
/*TODO*///	{
/*TODO*///	   IO_QUICKLOAD,	   /* type */
/*TODO*///	   1,				   /* count */
/*TODO*///	   "p000prg0",       /*file extensions */
/*TODO*///	   IO_RESET_CPU,	   /* reset if file changed */
/*TODO*///	   null,               /* id */
/*TODO*///	   cbm_quick_init,     /* init */
/*TODO*///	   cbm_quick_exit,     /* exit */
/*TODO*///	   null,               /* info */
/*TODO*///	   cbm_pet1_quick_open,     /* open */
/*TODO*///	   null,               /* close */
/*TODO*///	   null,               /* status */
/*TODO*///	   null,               /* seek */
/*TODO*///	   null,               /* input */
/*TODO*///	   null,               /* output */
/*TODO*///	   null,               /* input_chunk */
/*TODO*///	   null                /* output_chunk */
/*TODO*///	}
/*TODO*///	
/*TODO*///	#define IODEVICE_CBM_PET_QUICK 
/*TODO*///	{
/*TODO*///	   IO_QUICKLOAD,          /* type */
/*TODO*///	   1,                                      /* count */
/*TODO*///	   "p000prg0",            /*file extensions */
/*TODO*///	   IO_RESET_CPU,	   /* reset if file changed */
/*TODO*///	   null,               /* id */
/*TODO*///	   cbm_quick_init,     /* init */
/*TODO*///	   cbm_quick_exit,     /* exit */
/*TODO*///	   null,               /* info */
/*TODO*///	   cbm_pet_quick_open,     /* open */
/*TODO*///	   null,               /* close */
/*TODO*///	   null,               /* status */
/*TODO*///	   null,               /* seek */
/*TODO*///	   null,               /* input */
/*TODO*///	   null,               /* output */
/*TODO*///	   null,               /* input_chunk */
/*TODO*///	   null                /* output_chunk */
/*TODO*///	}
/*TODO*///	
        public static IODevice IODEVICE_CBM_QUICK = 
            new IODevice(
            IO_QUICKLOAD,          /* type */
            1,                                      /* count */
            "t64\0p00\0prg\0vsf\0",            /*file extensions */
            IO_RESET_NONE,	   /* reset if file changed */
            null,               /* id */
            cbm_quick_init,     /* init */
            cbm_quick_exit,     /* exit */
            null,               /* info */
            cbm_quick_open,     /* open */
            null,               /* close */
            null,               /* status */
            null,               /* seek */
            null,               /* input */
            null,               /* output */
            null,               /* input_chunk */
            null,                /* output_chunk */
            null,
            null
        );
	
/*TODO*///	public static IODevice IODEVICE_CBMB_QUICK = 
/*TODO*///            new IODevice(
/*TODO*///	   IO_QUICKLOAD,          /* type */
/*TODO*///	   1,                                      /* count */
/*TODO*///	   "p00\0prg\0",            /*file extensions */
/*TODO*///	   IO_RESET_CPU,	   /* reset if file changed */
/*TODO*///	   null,               /* id */
/*TODO*///	   cbm_quick_init,     /* init */
/*TODO*///	   cbm_quick_exit,     /* exit */
/*TODO*///	   null,               /* info */
/*TODO*///	   cbmb_quick_open,     /* open */
/*TODO*///	   null,               /* close */
/*TODO*///	   null,               /* status */
/*TODO*///	   null,               /* seek */
/*TODO*///	   null,               /* input */
/*TODO*///	   null,               /* output */
/*TODO*///	   null,               /* input_chunk */
/*TODO*///	   null ,               /* output_chunk */
/*TODO*///           null,
/*TODO*///           null
/*TODO*///	);
	
/*TODO*///	public static IODevice IODEVICE_CBM500_QUICK = 
/*TODO*///            new IODevice(
/*TODO*///	   IO_QUICKLOAD,          /* type */
/*TODO*///	   1,                                      /* count */
/*TODO*///	   "p00\0prg\0",            /*file extensions */
/*TODO*///	   IO_RESET_CPU,	   /* reset if file changed */
/*TODO*///	   null,               /* id */
/*TODO*///	   cbm_quick_init,     /* init */
/*TODO*///	   cbm_quick_exit,     /* exit */
/*TODO*///	   null,               /* info */
/*TODO*///	   cbm500_quick_open,     /* open */
/*TODO*///	   null,               /* close */
/*TODO*///	   null,               /* status */
/*TODO*///	   null,               /* seek */
/*TODO*///	   null,               /* input */
/*TODO*///	   null,               /* output */
/*TODO*///	   null,               /* input_chunk */
/*TODO*///	   null,                /* output_chunk */
/*TODO*///           null,
/*TODO*///           null
/*TODO*///	);
/*TODO*///	
/*TODO*///	public static IODevice IODEVICE_CBM_C65_QUICK = 
/*TODO*///            new IODevice(
/*TODO*///	   IO_QUICKLOAD,          /* type */
/*TODO*///	   1,                                      /* count */
/*TODO*///	   "p00\0prg\0",            /*file extensions */
/*TODO*///	   IO_RESET_CPU,	   /* reset if file changed */
/*TODO*///	   null,               /* id */
/*TODO*///	   cbm_quick_init,     /* init */
/*TODO*///	   cbm_quick_exit,     /* exit */
/*TODO*///	   null,               /* info */
/*TODO*///	   cbm_c65_quick_open,     /* open */
/*TODO*///	   null,               /* close */
/*TODO*///	   null,               /* status */
/*TODO*///	   null,               /* seek */
/*TODO*///	   null,               /* input */
/*TODO*///	   null,               /* output */
/*TODO*///	   null,               /* input_chunk */
/*TODO*///	   null,                /* output_chunk */
/*TODO*///           null,
/*TODO*///           null
/*TODO*///        );
	
	/* use to functions to parse, load the rom images into memory
	   and then use the cbm_rom var */
/*TODO*///	int cbm_rom_init(int id);
/*TODO*///	void cbm_rom_exit(int id);
        
        public static int CBM_ROM_ADDR_UNKNOWN  = 0;
	public static int CBM_ROM_ADDR_LO       = -1;
	public static int CBM_ROM_ADDR_HI       = -2;
	
	public static class _CBM_ROM {
		public int addr, size;
		public UBytePtr chip=new UBytePtr(1024 * 128);
	};
        
        public static _CBM_ROM CBM_ROM = new _CBM_ROM();
	
	
/*TODO*///	extern INT8 cbm_c64_game;
/*TODO*///	extern INT8 cbm_c64_exrom;
/*TODO*///	extern CBM_ROM cbm_rom[0x20];
	
	public static IODevice IODEVICE_CBM_ROM(String extensions)
	{
            return (new IODevice(
                    IO_CARTSLOT,        /* type */
                    2,                  /* in reality 1 *//* count */
                    extensions,            /*file extensions */
                    IO_RESET_ALL,	   /* reset if file changed */
                    null, 
                    cbm_rom_init,       /* init */
                    cbm_rom_exit,       /* exit */
                    null,               /* info */
                    null,               /* open */
                    null,               /* close */
                    null,               /* status */
                    null,               /* seek */
                    null,               /* input */
                    null,               /* output */
                    null,               /* input_chunk */
                    null,                /* output_chunk */
                    null,
                    null
            ));
        }
        
	
	/* prg file format
	 * sfx file format
	 * sda file format
	 * 0 lsb 16bit address
	 * 2 chip data */
	
	/* p00 file format (p00 .. p63, s00 .. s63, ..)
	 * 0x0000 C64File
	 * 0x0007 0
	 * 0x0008 Name in commodore encoding?
	 * 0x0018 0 0
	 * 0x001a lsb 16bit address
	 * 0x001c data */
	
	
}
