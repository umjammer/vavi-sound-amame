/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.machine;

import static arcadeflex056.fileio.*;
import static arcadeflex056.osdepend.logerror;
import static common.ptr.*;
import static consoleflex056.funcPtr.*;
import static mame056.osdependH.*;
import static mess056.deviceH.*;
import static mess056.includes.cbmdriveH.*;
import static mess056.includes.cbmserbH.*;
import static mess056.machine.cbmdrive.*;
import static mess056.mess.*;
import static common.libc.cstdio.*;
import static mame056.mame.Machine;
import static mame056.usrintrf.ui_text;
import static mess056.machine.c64.c64_state;
import static mess056.vidhrdw.vic6567.vic2;

public class cbmserb
{
	
	public static int VERBOSE_DBG = 0;				   /* general debug messages */
	
	public static CBM_Drive[] cbm_drive = new CBM_Drive[2];
        
        static {
            for (int _i=0 ; _i<2 ; _i++)
                cbm_drive[_i] = new CBM_Drive();
        }
	
	public static CBM_Serial cbm_serial = new CBM_Serial();
	
	/* must be called before other functions */
	public static void cbm_drive_open ()
	{
            System.out.println("cbm_drive_open");
		int i;
	
		cbm_drive_open_helper ();
	
		cbm_serial.count = 0;
		for (i = 0; i < cbm_serial.atn.length; i++)
	
		{
			cbm_serial.atn[i] =
				cbm_serial.data[i] =
				cbm_serial.clock[i] = 1;
		}
	}
	
	public static void cbm_drive_close ()
	{
            System.out.println("cbm_drive_close");
		int i;
	
		cbm_serial.count = 0;
		for (i = 0; i < cbm_drive.length; i++)
		{
			cbm_drive[i]._interface = 0;
	
			if (cbm_drive[i].drive == D64_IMAGE)
			{
				if (cbm_drive[i].d.d64.image != null)
					cbm_drive[i].d.d64.image = null;
			}
			cbm_drive[i].drive = 0;
		}
	}
	
	public static void cbm_drive_config (CBM_Drive drive, int _interface, int serialnr)
	{
            System.out.println("cbm_drive_config");
		int i;
	
		if (_interface==SERIAL)
			drive.i.serial.device=serialnr;
	
		if (_interface==IEEE)
			drive.i.ieee.device=serialnr;
	
		if (drive._interface == _interface)
			return;
	
		if (drive._interface == SERIAL)
		{
			for (i = 0; (i < cbm_serial.count) && (cbm_serial.drives[i] != drive); i++) ;
			for (; i + 1 < cbm_serial.count; i++)
				cbm_serial.drives[i] = cbm_serial.drives[i + 1];
			cbm_serial.count--;
		}
	
		drive._interface = _interface;
	
		if (drive._interface == IEC)
		{
			drive.i.iec.handshakein =
				drive.i.iec.handshakeout = 0;
			drive.i.iec.status = 0;
			drive.i.iec.dataout = drive.i.iec.datain = 0xff;
			drive.i.iec.state = 0;
		}
		else if (drive._interface == SERIAL)
		{
			cbm_serial.drives[cbm_serial.count++] = drive;
			vc1541_reset_write(drive, 0);
		}
	}
	
	public static void cbm_drive_0_config (int _interface, int serialnr)
	{
            System.out.println("cbm_drive_0_config");
		cbm_drive_config (cbm_drive[0], _interface, serialnr);
	}
	public static void cbm_drive_1_config (int _interface, int serialnr)
	{
            System.out.println("cbm_drive_1_config");
		cbm_drive_config (cbm_drive[1], _interface, serialnr);
	}
	
	/* load *.prg files directy from filesystem (rom directory) */
	public static int cbm_drive_attach_fs (int id)
	{
            System.out.println("cbm_drive_attach_fs");
		CBM_Drive drive = cbm_drive[id];
	
		if (drive.drive == D64_IMAGE)
		{
			return 1;					   /* as long as floppy system is called before driver init */
			//if (drive.d.d64.image != null)
			//	drive.d.d64.image = null;
		}
		//memset (&(drive.d.fs), 0, sizeof (drive.d.fs));
                drive.d.fs = new _fs(); 
		drive.drive = FILESYSTEM;
		return 0;
	}
	
	static int d64_open (int id)
	{
		Object in;
		int size;
	
		cbm_drive[id].d.d64 = new _d64(); 
	
		cbm_drive[id].d.d64.image_type = IO_FLOPPY;
		cbm_drive[id].d.d64.image_id = id;
		if ((in = image_fopen (IO_FLOPPY, id, OSD_FILETYPE_IMAGE_R, 0))==null)
		{
			logerror(" image %s not found\n", device_filename(IO_FLOPPY,id));
			return 1;
		}
		size = osd_fsize (in);
		if ((cbm_drive[id].d.d64.image = new UBytePtr(size))==null)
		{
			osd_fclose (in);
			return 1;
		}
		if (size != osd_fread (in, cbm_drive[id].d.d64.image, size))
		{
			cbm_drive[id].d.d64.image = null;
			osd_fclose (in);
			return 1;
		}
		osd_fclose (in);
	
		logerror("floppy image %s loaded\n",
					 device_filename(IO_FLOPPY,id));
	
		cbm_drive[id].drive = D64_IMAGE;
		return 0;
	}
	
	/* open an d64 image */
	public static io_initPtr cbm_drive_attach_image = new io_initPtr() {
            public int handler(int id) {
                /*TODO*///#if 1
		if (device_filename(IO_FLOPPY,id)==null)
			return cbm_drive_attach_fs (id);
	/*TODO*///#else
	/*TODO*///    CBM_Drive *drive = cbm_drive + id;
	/*TODO*///	if (drive.drive == FILESYSTEM) {
	/*TODO*///
	/*TODO*///	}
	/*TODO*///#endif
		return d64_open (id);
            }
        };
	
	
	/*TODO*///static void c1551_write_data (CBM_Drive c1551, int data)
	/*TODO*///{
	/*TODO*///	c1551.i.iec.datain = data;
	/*TODO*///	c1551_state (c1551);
	/*TODO*///}
	
	/*TODO*///static int c1551_read_data (CBM_Drive c1551)
	/*TODO*///{
	/*TODO*///	c1551_state (c1551);
	/*TODO*///	return c1551.i.iec.dataout;
	/*TODO*///}
	
	/*TODO*///static void c1551_write_handshake (CBM_Drive c1551, int data)
	/*TODO*///{
	/*TODO*///	c1551.i.iec.handshakein = (data&0x40)!=0?1:0;
	/*TODO*///	c1551_state (c1551);
	/*TODO*///}
	
	/*TODO*///static int c1551_read_handshake (CBM_Drive c1551)
	/*TODO*///{
	/*TODO*///	c1551_state (c1551);
	/*TODO*///	return c1551.i.iec.handshakeout!=0?0x80:0;
	/*TODO*///}
	
	/*TODO*///public static int c1551_read_status (CBM_Drive c1551)
	/*TODO*///{
	/*TODO*///	c1551_state (c1551);
	/*TODO*///	return c1551.i.iec.status;
	/*TODO*///}
	
	/*TODO*///public static void c1551_0_write_data (int data)
	/*TODO*///{
	/*TODO*///	c1551_write_data (cbm_drive[0], data);
	/*TODO*///}
	/*TODO*///public static int c1551_0_read_data ()
	/*TODO*///{
	/*TODO*///	return c1551_read_data (cbm_drive[0]);
	/*TODO*///}
	/*TODO*///public static void c1551_0_write_handshake (int data)
	/*TODO*///{
	/*TODO*///	c1551_write_handshake (cbm_drive[0], data);
	/*TODO*///}
	/*TODO*///public static int c1551_0_read_handshake ()
	/*TODO*///{
	/*TODO*///	return c1551_read_handshake (cbm_drive[0]);
	/*TODO*///}
	/*TODO*///public static int c1551_0_read_status ()
	/*TODO*///{
	/*TODO*///	return c1551_read_status (cbm_drive[0]);
	/*TODO*///}
	
	/*TODO*///public static void c1551_1_write_data (int data)
	/*TODO*///{
	/*TODO*///	c1551_write_data (cbm_drive[1], data);
	/*TODO*///}
	/*TODO*///public static int c1551_1_read_data ()
	/*TODO*///{
	/*TODO*///	return c1551_read_data (cbm_drive[1]);
	/*TODO*///}
	/*TODO*///public static void c1551_1_write_handshake (int data)
	/*TODO*///{
	/*TODO*///	c1551_write_handshake (cbm_drive[1], data);
	/*TODO*///}
	/*TODO*///public static int c1551_1_read_handshake ()
	/*TODO*///{
	/*TODO*///	return c1551_read_handshake (cbm_drive[1]);
	/*TODO*///}
	/*TODO*///public static int c1551_1_read_status ()
	/*TODO*///{
	/*TODO*///	return c1551_read_status (cbm_drive[1]);
	/*TODO*///}
	
	static void vc1541_reset_write (CBM_Drive vc1541, int level)
	{
		if (level == 0)
		{
			vc1541.i.serial.data =
				vc1541.i.serial.clock =
				vc1541.i.serial.atn = 1;
			vc1541.i.serial.state = 0;
		}
                //cbm_serial.drives[vc1541.number] = vc1541;
	}
	
	static int vc1541_atn_read (CBM_Drive vc1541)
	{
		vc1541_state (vc1541);
		return vc1541.i.serial.atn;
	}
	
	static int vc1541_data_read (CBM_Drive vc1541)
	{
		vc1541_state (vc1541);
		return vc1541.i.serial.data;
	}
	
	static int vc1541_clock_read (CBM_Drive vc1541)
	{
		vc1541_state (vc1541);
		return vc1541.i.serial.clock;
	}
	
	static void vc1541_data_write (CBM_Drive vc1541, int level)
	{
		vc1541_state (vc1541);
	}
	
	static void vc1541_clock_write (CBM_Drive vc1541, int level)
	{
		vc1541_state (vc1541);
	}
	
	static void vc1541_atn_write (CBM_Drive vc1541, int level)
	{
		vc1541_state (vc1541);
	}
	
	
	/* bus handling */
	public static void cbm_serial_reset_write (int level)
	{
            //System.out.println("cbm_serial_reset_write");
		int i;
	
		for (i = 0; i < cbm_serial.count; i++)
			vc1541_reset_write (cbm_serial.drives[i], level);
		/* init bus signals */
	}
	
	public static int cbm_serial_request_read ()
	{
            //System.out.println("cbm_serial_request_read");
		/* in c16 not connected */
		return 1;
	}
	
	public static void cbm_serial_request_write (int level)
	{
            System.out.println("cbm_serial_request_write");
	}
	
	public static int cbm_serial_atn_read ()
	{
            //System.out.println("cbm_serial_atn_read");
		int i;
	
		cbm_serial.atn[0] = cbm_serial.atn[1];
		for (i = 0; i < cbm_serial.count; i++)
			cbm_serial.atn[0] &= cbm_serial.atn[i + 2] =
				vc1541_atn_read (cbm_serial.drives[i]);
		return cbm_serial.atn[0];
	}
	
	public static int cbm_serial_data_read ()
	{
            //System.out.println("cbm_serial_data_read");
		int i;
	
		cbm_serial.data[0] = cbm_serial.data[1];
		for (i = 0; i < cbm_serial.count; i++)
			cbm_serial.data[0] &= cbm_serial.data[i + 2] =
				vc1541_data_read (cbm_serial.drives[i]);
                //System.out.println("END cbm_serial_data_read");
                c64_state();
		return cbm_serial.data[0];
	}
	
	public static int cbm_serial_clock_read ()
	{
            //System.out.println("cbm_serial_clock_read");
		int i;
	
		cbm_serial.clock[0] = cbm_serial.clock[1];
		for (i = 0; i < cbm_serial.count; i++)
			cbm_serial.clock[0] &= cbm_serial.clock[i + 2] =
				vc1541_clock_read (cbm_serial.drives[i]);
		return cbm_serial.clock[0];
	}
	
	public static void cbm_serial_data_write (int level)
	{
            //System.out.println("cbm_serial_data_write");
		int i;
	
		cbm_serial.data[0] =
			cbm_serial.data[1] = level;
		/* update line */
		for (i = 0; i < cbm_serial.count; i++)
			cbm_serial.data[0] &= cbm_serial.data[i + 2];
		/* inform drives */
		for (i = 0; i < cbm_serial.count; i++)
			vc1541_data_write (cbm_serial.drives[i], cbm_serial.data[0]);
	}
	
	public static void cbm_serial_clock_write (int level)
	{
            //System.out.println("cbm_serial_clock_write");
		int i;
	
		cbm_serial.clock[0] =
			cbm_serial.clock[1] = level;
		/* update line */
		for (i = 0; i < cbm_serial.count; i++)
			cbm_serial.clock[0] &= cbm_serial.clock[i + 2];
		/* inform drives */
		for (i = 0; i < cbm_serial.count; i++)
			vc1541_clock_write (cbm_serial.drives[i], cbm_serial.clock[0]);
	}
	
	public static void cbm_serial_atn_write (int level)
	{
            //System.out.println("cbm_serial_atn_write");
		int i;
	
		cbm_serial.atn[0] =
			cbm_serial.atn[1] = level;
		/* update line */
		for (i = 0; i < cbm_serial.count; i++)
			cbm_serial.atn[0] &= cbm_serial.atn[i + 2];
		/* inform drives */
		for (i = 0; i < cbm_serial.count; i++)
			vc1541_atn_write (cbm_serial.drives[i], cbm_serial.atn[0]);
	}
	
	/* delivers status for displaying */
	static void cbm_drive_status (CBM_Drive c1551, String text, int size)
	{
            //System.out.println("cbm_drive_status");
            //System.out.println("cbm_drive_status");
	/*TODO*///	text[0] = 0;
	/*TODO*///#if VERBOSE_DBG
	/*TODO*///	if ((c1551.interface == SERIAL) /*&&(c1551.i.serial.device==8) */ )
	/*TODO*///	{
	/*TODO*///		snprintf (text, size, "%d state:%d %d %d %s %s %s",
	/*TODO*///				  c1551.state, c1551.i.serial.state, c1551.pos, c1551.size,
	/*TODO*///				  cbm_serial.atn[0] ? "ATN" : "atn",
	/*TODO*///				  cbm_serial.clock[0] ? "CLOCK" : "clock",
	/*TODO*///				  cbm_serial.data[0] ? "DATA" : "data");
	/*TODO*///		return;
	/*TODO*///	}
	/*TODO*///	if ((c1551.interface == IEC) /*&&(c1551.i.serial.device==8) */ )
	/*TODO*///	{
	/*TODO*///		snprintf (text, size, "%d state:%d %d %d",
	/*TODO*///				  c1551.state, c1551.i.iec.state, c1551.pos, c1551.size);
	/*TODO*///		return;
	/*TODO*///	}
	/*TODO*///#endif
	/*TODO*///	if (c1551.drive == FILESYSTEM)
	/*TODO*///	{
	/*TODO*///		switch (c1551.state)
	/*TODO*///		{
	/*TODO*///		case OPEN:
	/*TODO*///			printf (text, size, "Romdir File %s open", c1551.d.fs.filename);
	/*TODO*///			break;
	/*TODO*///		case READING:
	/*TODO*///			snprintf (text, size, "Romdir File %s loading %d",
	/*TODO*///					  c1551.d.fs.filename, c1551.size - c1551.pos - 1);
	/*TODO*///			break;
	/*TODO*///		case WRITING:
	/*TODO*///			snprintf (text, size, "Romdir File %s saving %d",
	/*TODO*///					  c1551.d.fs.filename, c1551.pos);
	/*TODO*///			break;
	/*TODO*///		}
	/*TODO*///	}
	/*TODO*///	else 
            if (c1551.drive == D64_IMAGE)
            {
                    switch (c1551.state)
                    {
                    case OPEN:
                        //System.out.println("OPEN");
                            text = sprintf ("Image File %s open",
                                              new String(c1551.d.d64.filename));
                            
                            break;
                    case READING:
                        //System.out.println("READING");
                            //text = sprintf (text, size, "Image File %s loading %d",
                            //                  c1551.d.d64.filename,
                            //                  c1551.size - c1551.pos - 1);
                            text = sprintf ("Image File %s loading %d",
                                              new String(c1551.d.d64.filename),
                                              c1551.size - c1551.pos - 1);
                            break;
                    case WRITING:
                        //System.out.println("WRITING");
                            text = sprintf ("Image File %s saving %d",
                                              new String(c1551.d.d64.filename), c1551.pos);
                            break;
                    }
            }
            //int x = Machine.uixmin + 1 * Machine.uifontwidth * 16 + 1;
            int y = Machine.uiymin + Machine.uiheight - 9;
            ui_text(Machine.scrbitmap, text, 0, y);
	}
	
	public static void cbm_drive_0_status (String text, int size)
	{
            //System.out.println("cbm_drive_0_status");
		cbm_drive_status (cbm_drive[0], text, size);
	}
	
	public static void cbm_drive_1_status (String text, int size)
	{
            //System.out.println("cbm_drive_1_status");
		cbm_drive_status (cbm_drive[1], text, size);
	}
}

