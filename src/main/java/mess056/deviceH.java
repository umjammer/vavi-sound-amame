/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056;

public class deviceH
{

    /*TODO*/////enum
    /*TODO*/////{	
    /* List of all supported devices.  Refer to the device by these names only							*/
    public static int         IO_END = 0;	/*  0 - Dummy type to end IODevice enumerations 									*/
    public static int         IO_CARTSLOT=1;	/*  1 - Cartidge Port, as found on most console and on some computers 				*/
    public static int         IO_FLOPPY=2;	/*  2 - Floppy Disk unit 															*/
    public static int         IO_HARDDISK=3;	/*  3 - Hard Disk unit 																*/
    public static int         IO_CYLINDER=4;	/*  4 - Magnetically-Coated Cylinder 												*/
    public static int         IO_CASSETTE=5;	/*  5 - Cassette Recorder (common on early home computers) 							*/
    public static int         IO_PUNCHCARD=6;	/*  6 - Card Puncher/Reader 															*/
    public static int         IO_PUNCHTAPE=7;	/*  7 - Tape Puncher/Reader (reels instead of punchcards) 							*/
    public static int         IO_PRINTER=8;	/*  8 - Printer device 																*/
    public static int         IO_SERIAL=9;	/*  9 - some serial port 															*/
    public static int         IO_PARALLEL=10;      /* 10 - Generic Parallel Port														*/
    public static int         IO_SNAPSHOT=11;	/* 11 - Complete 'snapshot' of the state of the computer 							*/
    public static int         IO_QUICKLOAD=12;	/* 12 - Allow to load program/data into memory, without matching any actual device	*/
    public static int         IO_COUNT=13;		/* 13 - Total Number of IO_devices for searching										*/
    /*TODO*/////};

    public static class Devices {
        public int  id;
        public String name;
        public String shortname;

        public Devices(int id, String name, String shortname){
            this.id = id;
            this.name = name;
            this.shortname = shortname;
        }
    };

    /* Call this from the CLI to add a DEVICE (with its arg) to the options struct */
    /*TODO*/////int register_device (const int type, const char *arg);

    /* Device handlers */
    /*TODO*/////extern int device_open(int type, int id, int mode, void *args);
    /*TODO*/////extern void device_close(int type, int id);
    /*TODO*/////extern int device_seek(int type, int id, int offset, int whence);
    /*TODO*/////extern int device_tell(int type, int id);
    /*TODO*/////extern int device_status(int type, int id, int newstatus);
    /*TODO*/////extern int device_input(int type, int id);
    /*TODO*/////extern void device_output(int type, int id, int data);
    /*TODO*/////extern int device_input_chunk(int type, int id, void *dst, int chunks);
    /*TODO*/////extern void device_output_chunk(int type, int id, void *src, int chunks);


    /*TODO*/////#endif
}