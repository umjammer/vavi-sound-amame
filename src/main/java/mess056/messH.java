/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056;

import arcadeflex056.fucPtr;
import static consoleflex056.funcPtr.*;
import mame056.driverH;
import mame056.driverH.MachineDriver;
import static mame056.driverH.ROT0;
import mame056.commonH.RomModule;
import static mame056.commonH.rommodule_macro;

import mame056.inptportH;
import static mame056.inptportH.IPT_COIN1;
import static mame056.inptportH.IPT_COIN2;
import static mame056.inptportH.IPT_COIN3;
import static mame056.inptportH.IPT_COIN4;
import static mame056.inptportH.IPT_TILT;
import static mame056.inptportH.input_macro;

public class messH
{
	
/*TODO*/////	#ifndef MESS_H
/*TODO*/////	#define MESS_H
/*TODO*/////	
/*TODO*/////	
/*TODO*/////	#define ARRAY_LENGTH(x) (sizeof(x)/sizeof(x[0]))
/*TODO*/////	
/*TODO*/////	/* MESS_DEBUG is a debug switch (for developers only) for
/*TODO*/////	   debug code, which should not be found in distributions, like testdrivers,...
/*TODO*/////	   contrary to MAME_DEBUG, NDEBUG it should not be found in the makefiles of distributions
/*TODO*/////	   use it in your private root makefile */
/*TODO*/////	//#define MESS_DEBUG
/*TODO*/////	
/*TODO*/////	#if defined(__STDC_VERSION__) && (__STDC_VERSION__ >= 199901L)
/*TODO*/////		#elif (! defined(__bool_true_false_are_defined)) && (! defined(__cplusplus))
/*TODO*/////		#ifndef bool
/*TODO*/////			#define bool int
/*TODO*/////		#endif
/*TODO*/////		#ifndef true
/*TODO*/////			#define true 1
/*TODO*/////		#endif
/*TODO*/////		#ifndef false
/*TODO*/////			#define false 0
/*TODO*/////		#endif
/*TODO*/////	#endif
/*TODO*/////	
/*TODO*/////	#ifdef __cplusplus
/*TODO*/////	extern "C" {
/*TODO*/////	#endif
/*TODO*/////	
/*TODO*/////	#ifndef TRUE
/*TODO*/////	#define TRUE 1
/*TODO*/////	#endif
/*TODO*/////	
/*TODO*/////	#ifndef FALSE
/*TODO*/////	#define FALSE 0
/*TODO*/////	#endif
/*TODO*/////	
/*TODO*/////	/* Endian macros */
/*TODO*/////	#define FLIPENDIAN_INT16(x)	((((x) >> 8) | ((x) << 8)) & 0xffff)
/*TODO*/////	#define FLIPENDIAN_INT32(x)	((((x) << 24) | (((UINT32) (x)) >> 24) | \
/*TODO*/////	                       (( (x) & 0x0000ff00) << 8) | (( (x) & 0x00ff0000) >> 8)))
/*TODO*/////	
/*TODO*/////	#ifdef LSB_FIRST
/*TODO*/////	#define BIG_ENDIANIZE_INT16(x)		(FLIPENDIAN_INT16(x))
/*TODO*/////	#define BIG_ENDIANIZE_INT32(x)		(FLIPENDIAN_INT32(x))
/*TODO*/////	#define LITTLE_ENDIANIZE_INT16(x)	(x)
/*TODO*/////	#define LITTLE_ENDIANIZE_INT32(x)	(x)
/*TODO*/////	#else
/*TODO*/////	#define BIG_ENDIANIZE_INT16(x)		(x)
/*TODO*/////	#define BIG_ENDIANIZE_INT32(x)		(x)
/*TODO*/////	#define LITTLE_ENDIANIZE_INT16(x)	(FLIPENDIAN_INT16(x))
/*TODO*/////	#define LITTLE_ENDIANIZE_INT32(x)	(FLIPENDIAN_INT32(x))
/*TODO*/////	#endif /* LSB_FIRST */
/*TODO*/////	
/*TODO*/////	/* Win32 defines this for vararg functions */
/*TODO*/////	#ifndef DECL_SPEC
/*TODO*/////	#define DECL_SPEC
/*TODO*/////	#endif
/*TODO*/////	
/*TODO*/////	int DECL_SPEC mess_printf(char *fmt, ...);
/*TODO*/////	
/*TODO*/////	extern extern int displayimageinfo(struct mame_bitmap *bitmap, int selected);
/*TODO*/////	extern int filemanager(struct mame_bitmap *bitmap, int selected);
/*TODO*/////	extern int tapecontrol(struct mame_bitmap *bitmap, int selected);
/*TODO*/////	
/*TODO*/////	/* driver.h - begin */
    public static int IPT_SELECT1 = IPT_COIN1;
    public static int IPT_SELECT2 = IPT_COIN2;
    public static int IPT_SELECT3 = IPT_COIN3;
    public static int IPT_SELECT4 = IPT_COIN4;
    public static int IPT_KEYBOARD = IPT_TILT;
/*TODO*/////	/* driver.h - end */
/*TODO*/////	
/*TODO*/////	
/*TODO*/////	/* The wrapper for osd_fopen() */
/*TODO*/////	void *image_fopen(int type, int id, int filetype, int read_or_write);
/*TODO*/////	
    /* IODevice Initialisation return values.  Use these to determine if */
    /* the emulation can continue if IODevice initialisation fails */
    public static int INIT_PASS = 0;
    public static int INIT_FAIL = 1;
    public static int IMAGE_VERIFY_PASS = 0;
    public static int IMAGE_VERIFY_FAIL = 1;
    
    /* IODevice Initialisation return values.  Use these to determine if */
/* the emulation can continue if IODevice initialisation fails */
    public static int INIT_OK = 0;
    public static int INIT_FAILED = 1;
    public static int INIT_UNKNOWN = 2;
    

/* IODevice ID return values.  Use these to determine if */
/* the emulation can continue if image cannot be positively IDed */
    public static int ID_FAILED = 0;
    public static int ID_OK = 1;
    public static int ID_UNKNOWN = 2;
	
/*TODO*/////	/* possible values for osd_fopen() last argument
/*TODO*/////	 * OSD_FOPEN_READ
/*TODO*/////	 *	open existing file in read only mode.
/*TODO*/////	 *	ZIP images can be opened only in this mode, unless
/*TODO*/////	 *	we add support for writing into ZIP files.
/*TODO*/////	 * OSD_FOPEN_WRITE
/*TODO*/////	 *	open new file in write only mode (truncate existing file).
/*TODO*/////	 *	used for output images (eg. a cassette being written).
/*TODO*/////	 * OSD_FOPEN_RW
/*TODO*/////	 *	open existing(!) file in read/write mode.
/*TODO*/////	 *	used for floppy/harddisk images. if it fails, a driver
/*TODO*/////	 *	might try to open the image with OSD_FOPEN_READ and set
/*TODO*/////	 *	an internal 'write protect' flag for the FDC/HDC emulation.
/*TODO*/////	 * OSD_FOPEN_RW_CREATE
/*TODO*/////	 *	open existing file or create new file in read/write mode.
/*TODO*/////	 *	used for floppy/harddisk images. if a file doesn't exist,
/*TODO*/////	 *	it shall be created. Used to 'format' new floppy or harddisk
/*TODO*/////	 *	images from within the emulation. A driver might use this
/*TODO*/////	 *	if both, OSD_FOPEN_RW and OSD_FOPEN_READ modes, failed.
/*TODO*/////	 */
	public static final int OSD_FOPEN_READ = 0, OSD_FOPEN_WRITE = 1, OSD_FOPEN_RW = 2, OSD_FOPEN_RW_CREATE = 3;
/*TODO*/////	
/*TODO*/////	
/*TODO*/////	#ifdef MAX_KEYS
/*TODO*/////	 #undef MAX_KEYS
        public static int MAX_KEYS	= 128; /* for MESS but already done in INPUT.C*/
/*TODO*/////	#endif
/*TODO*/////	
/*TODO*/////	
/*TODO*/////	enum {
        public static int IO_RESET_NONE =   0;	/* changing the device file doesn't reset anything 								*/
        public static int IO_RESET_CPU  =   1;	/* only reset the CPU 															*/
        public static int IO_RESET_ALL  =   2;	/* restart the driver including audio/video 									*/
/*TODO*/////	};

    
	/******************************************************************************
         * This is a start at the proposed peripheral structure.
	 * It will be filled with live starting with the next release (I hope).
	 * For now it gets us rid of the several lines MESS specific code
	 * in the GameDriver struct and replaces it by only one pointer.
	 *	type				type of device (from above enum)
	 *	count				maximum number of instances
	 *	file_extensions 	supported file extensions
	 *	_private			to be used by the peripheral driver code
	 *	id					identify file
	 *	init				initialize device
	 *	exit				shutdown device
	 *	info				get info for device instance
	 *	open				open device (with specific args)
	 *	close				close device
	 *	status				(set a device status and) get the previous status
	 *	seek				seek to file position
	 *	tell				tell current file position
	 *	input				input character or code
	 *	output				output character or code
	 *	input_chunk 		input chunk of data (eg. sector or track)
	 *	output_chunk		output chunk of data (eg. sector or track)
	 ******************************************************************************/
	public static class IODevice {
            public int type;
            public int count;
            public String file_extensions;
            public int reset_depth;
            //public String dummy;
            public io_idPtr id;
            public io_initPtr init;
            public io_exitPtr exit;
            public io_infoPtr info;
            public io_openPtr open;
            public io_closePtr close;
            public io_statusPtr status;
            public io_seekPtr seek;
            public io_tellPtr tell;
            public io_inputPtr input;
            public io_outputPtr output;
            public io_input_chunkPtr input_chunk;
            public io_output_chunkPtr output_chunk;
            public io_partialcrcPtr partialcrc;
            
            public IODevice(int type, int count, String file_extensions, int reset_depth, io_idPtr id, io_initPtr init, io_exitPtr exit, io_infoPtr info, io_openPtr open, io_closePtr close, io_statusPtr status, io_seekPtr seek, io_tellPtr tell, io_inputPtr input, io_outputPtr output, io_input_chunkPtr input_chunk, io_output_chunkPtr output_chunk, io_partialcrcPtr partialcrc) {
                this.type = type;
                this.count = count;
                this.file_extensions = file_extensions;
                this.reset_depth = reset_depth;
                this.id = id;
                //this.dummy = id;
                this.init = init;
                this.exit = exit;
                this.info = info;
                this.open = open;
                this.close = close;
                this.status = status;
                this.seek = seek;
                this.tell = tell;
                this.input = input;
                this.output = output;
                this.input_chunk = input_chunk;
                this.output_chunk = output_chunk;
                this.partialcrc = partialcrc;
            }

            public IODevice(int type, int count, String file_extensions, int reset_depth, io_idPtr id, io_initPtr init, io_exitPtr exit, io_infoPtr info, io_openPtr open, io_closePtr close, io_statusPtr status, io_seekPtr seek, io_tellPtr tell, io_inputPtr input, io_outputPtr output, io_input_chunkPtr input_chunk, io_output_chunkPtr output_chunk) {
                this(type, count, file_extensions, reset_depth, id, init, exit, info, open, close, status, seek, tell, input, output, input_chunk, output_chunk, null);

            }

            public IODevice(int type) {
                this(type, 0, "", 0, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
            }
	};


/*TODO*/////	/* these are called from mame.c*/
/*TODO*/////	#ifdef MAME_DEBUG
/*TODO*/////	#endif
/*TODO*/////	extern extern int init_devices(const void *game);
/*TODO*/////	extern extern 
/*TODO*/////	/* access mess.c internal fields for a device type (instance id) */
/*TODO*/////	extern int          device_count(int type);
/*TODO*/////	extern const char  *device_typename(int type);
/*TODO*/////	extern const char  *device_brieftypename(int type);
/*TODO*/////	extern const char  *device_typename_id(int type, int id);
/*TODO*/////	extern const char  *device_filename(int type, int id);
/*TODO*/////	extern unsigned int device_length(int type, int id);
/*TODO*/////	extern unsigned int device_crc(int type, int id);
/*TODO*/////	extern void         device_set_crc(int type, int id, UINT32 new_crc);
/*TODO*/////	extern const char  *device_longname(int type, int id);
/*TODO*/////	extern const char  *device_manufacturer(int type, int id);
/*TODO*/////	extern const char  *device_year(int type, int id);
/*TODO*/////	extern const char  *device_playable(int type, int id);
/*TODO*/////	extern const char  *device_extrainfo(int type, int id);
/*TODO*/////	extern const char  *device_file_extension(int type, int extnum);
/*TODO*/////	extern int          device_filename_change(int type, int id, const char *name);
/*TODO*/////	
/*TODO*/////	/* access functions from the struct IODevice arrays of a driver */
/*TODO*/////	
/*TODO*/////	extern const void *device_info(int type, int id);
/*TODO*/////	
/*TODO*/////	/* This is the dummy GameDriver with flag NOT_A_DRIVER set
/*TODO*/////	   It allows us to use an empty PARENT field in the macros. */
/*TODO*/////	
/*TODO*/////	 /* Flag is used to bail out in mame.c/run_game() and cpuintrf.c/run_cpu()
/*TODO*/////	 * but keep the program going. It will be set eg. if the filename for a
/*TODO*/////	 * device which has IO_RESET_ALL flag set is changed
/*TODO*/////	 */
/*TODO*/////	extern int mess_keep_going;
/*TODO*/////	
/*TODO*/////	/******************************************************************************
/*TODO*/////	 * MESS' version of the GAME() and GAMEX() macros of MAME
/*TODO*/////	 * CONS and CONSX are for consoles
/*TODO*/////	 * COMP and COMPX are for computers
/*TODO*/////	 ******************************************************************************/
/*TODO*/////	#define CONS(YEAR,NAME,PARENT,MACHINE,INPUT,INIT,COMPANY,FULLNAME)	\
/*TODO*/////	extern const struct GameDriver driver_##PARENT; \
/*TODO*/////	extern const struct GameDriver driver_##NAME;   \
/*TODO*/////	const struct GameDriver driver_##NAME = 	\
/*TODO*/////	{											\
/*TODO*/////		__FILE__,								\
/*TODO*/////		&driver_##PARENT,						\
/*TODO*/////		#NAME,									\
/*TODO*/////		FULLNAME,								\
/*TODO*/////		#YEAR,									\
/*TODO*/////		COMPANY,								\
/*TODO*/////		&machine_driver_##MACHINE,				\
/*TODO*/////		input_ports_##INPUT,					\
/*TODO*/////		init_##INIT,							\
/*TODO*/////		rom_##NAME,								\
/*TODO*/////		io_##NAME, 								\
/*TODO*/////		ROT0									\
/*TODO*/////	};

        
/*TODO*/////	#define CONSX(YEAR,NAME,PARENT,MACHINE,INPUT,INIT,COMPANY,FULLNAME,FLAGS)	\
/*TODO*/////	extern const struct GameDriver driver_##PARENT;   \
/*TODO*/////	extern const struct GameDriver driver_##NAME;   \
/*TODO*/////	const struct GameDriver driver_##NAME = 	\
/*TODO*/////	{											\
/*TODO*/////		__FILE__,								\
/*TODO*/////		&driver_##PARENT,						\
/*TODO*/////		#NAME,									\
/*TODO*/////		FULLNAME,								\
/*TODO*/////		#YEAR,									\
/*TODO*/////		COMPANY,								\
/*TODO*/////		&machine_driver_##MACHINE,				\
/*TODO*/////		input_ports_##INPUT,					\
/*TODO*/////		init_##INIT,							\
/*TODO*/////		rom_##NAME,								\
/*TODO*/////		io_##NAME, 								\
/*TODO*/////		ROT0|(FLAGS)							\
/*TODO*/////	};
/*TODO*/////	
/*TODO*/////	#define COMP(YEAR,NAME,PARENT,MACHINE,INPUT,INIT,COMPANY,FULLNAME)	\
/*TODO*/////	extern const struct GameDriver driver_##PARENT;   \
/*TODO*/////	extern const struct GameDriver driver_##NAME;   \
/*TODO*/////	const struct GameDriver driver_##NAME = 	\
/*TODO*/////	{											\
/*TODO*/////		__FILE__,								\
/*TODO*/////		&driver_##PARENT,						\
/*TODO*/////		#NAME,									\
/*TODO*/////		FULLNAME,								\
/*TODO*/////		#YEAR,									\
/*TODO*/////		COMPANY,								\
/*TODO*/////		&machine_driver_##MACHINE,				\
/*TODO*/////		input_ports_##INPUT,					\
/*TODO*/////		init_##INIT,							\
/*TODO*/////		rom_##NAME,								\
/*TODO*/////		io_##NAME, 								\
/*TODO*/////		ROT0|GAME_COMPUTER 						\
/*TODO*/////	};
/*TODO*/////	
/*TODO*/////	#define COMPX(YEAR,NAME,PARENT,MACHINE,INPUT,INIT,COMPANY,FULLNAME,FLAGS)	\
/*TODO*/////	extern const struct GameDriver driver_##PARENT;   \
/*TODO*/////	extern const struct GameDriver driver_##NAME;   \
/*TODO*/////	const struct GameDriver driver_##NAME = 	\
/*TODO*/////	{											\
/*TODO*/////		__FILE__,								\
/*TODO*/////		&driver_##PARENT,						\
/*TODO*/////		#NAME,									\
/*TODO*/////		FULLNAME,								\
/*TODO*/////		#YEAR,									\
/*TODO*/////		COMPANY,								\
/*TODO*/////		&machine_driver_##MACHINE,				\
/*TODO*/////		input_ports_##INPUT,					\
/*TODO*/////		init_##INIT,							\
/*TODO*/////		rom_##NAME,								\
/*TODO*/////		io_##NAME, 								\
/*TODO*/////		ROT0|GAME_COMPUTER|(FLAGS)	 			\
/*TODO*/////	};
/*TODO*/////	
/*TODO*/////	
/*TODO*/////	#ifdef __cplusplus
/*TODO*/////	}
/*TODO*/////	#endif
/*TODO*/////	
/*TODO*/////	#endif
}
