/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.includes;

import static mess056.deviceH.*;
import static mess056.messH.*;
import static mess056.machine.cbmserb.*;

public class cbmserbH
{
    public static IODevice IODEVICE_CBM_DRIVE =
        new IODevice(
	
	   IO_FLOPPY,          /* type */
	   2,				   /* count */
	   "d64\0",            /* G64 later *//*file extensions */
	   IO_RESET_NONE,	   /* reset if file changed */
	   null,               /* id */
	   cbm_drive_attach_image,        /* init */
	   null,			   /* exit */
	   null,               /* info */
	   null,               /* open */
	   null,               /* close */
	   null,               /* status */
	   null,               /* seek */
	   null,			   /* tell */
	   null,               /* input */
	   null,               /* output */
	   null,               /* input_chunk */
	   null                /* output_chunk */
        );
	
	public static final int IEC = 1;
	public static final int SERIAL = 2;
	public static final int IEEE = 3;
	
}
