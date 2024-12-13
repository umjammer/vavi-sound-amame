/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.machine;

import static arcadeflex036.libc_old.*;
import static mess056.includes.cbmdriveH.*;
import static mess056.machine.cbmserb.*;
import static mame056.timer.*;
import static arcadeflex056.osdepend.logerror;
import static arcadeflex056.fileio.*;
import static common.libc.cstdio.*;
import static common.libc.cstring.*;
import static common.ptr.*;
import static mame056.mame.Machine;
import static mame056.osdependH.*;
import static mame056.usrintrf.ui_text;
import static mess056.machine.c64.c64_state;
import static mess056.vidhrdw.vic6567.vic2;

public class cbmdrive
{
/*
	#define VERBOSE_DBG 1				   /* general debug messages */
	
	
	/* tracks 1 to 35
	 * sectors number from 0
	 * each sector holds 256 data bytes
	 * directory and Bitmap Allocation Memory in track 18
	 * sector 0:
	 * 0: track# of directory begin (this linkage of sector often used)
	 * 1: sector# of directory begin
	 *
	 * BAM entries (one per track)
	 * offset 0: # of free sectors
	 * offset 1: sector 0 (lsb) free to sector 7
	 * offset 2: sector 8 to 15
	 * offset 3: sector 16 to whatever the number to sectors in track is
	 *
	 * directory sector:
	 * 0,1: track sector of next directory sector
	 * 2, 34, 66, ... : 8 directory entries
	 *
	 * directory entry:
	 * 0: file type
	 * (0x = scratched/splat, 8x = alive, Cx = locked
	 * where x: 0=DEL, 1=SEQ, 2=PRG, 3=USR, 4=REL)
	 * 1,2: track and sector of file
	 * 3..18: file name padded with a0
	 * 19,20: REL files side sector
	 * 21: REL files record length
	 * 28,29: number of blocks in file
	 * ended with illegal track and sector numbers
	 */
	
	public static int D64_MAX_TRACKS = 35;
	
	static int d64_sectors_per_track[] =
	{
		21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21,
		19, 19, 19, 19, 19, 19, 19,
		18, 18, 18, 18, 18, 18,
		17, 17, 17, 17, 17
	};
	
	static int[] d64_offset = new int[D64_MAX_TRACKS];		   /* offset of begin of track in d64 file */
	
	/* must be called before other functions */
	public static void cbm_drive_open_helper ()
	{
		int i;
	
		d64_offset[0] = 0;
		for (i = 1; i < 35; i++)
			d64_offset[i] = d64_offset[i - 1] + d64_sectors_per_track[i - 1] * 256;
	}
	
	/* calculates offset to beginning of d64 file for sector beginning */
	public static int d64_tracksector2offset (int track, int sector)
	{
		return d64_offset[track - 1] + sector * 256;
	}
	
	public static int cbm_compareNames (String left, UBytePtr right)
	{
		int i;
	
		for (i = 0; i < 16; i++)
		{
			if ((left.charAt(i) == '*') || (right.read(i) == '*'))
				return 1;
			if (left.charAt(i) == right.read(i))
				continue;
			if ((left.charAt(i) == 0xa0) && (right.read(i) == 0))
				return 1;
			if ((right.read(i) == 0xa0) && (left.charAt(i) == 0))
				return 1;
			return 0;
		}
		return 1;
	}
	
	/* searches program with given name in directory
	 * delivers -1 if not found
	 * or pos in image of directory node */
	public static int d64_find (CBM_Drive drive, String name)
	{
                //System.out.println("d64_find");
		int pos, track, sector, i;
	
		pos = d64_tracksector2offset (18, 0);
		track = drive.d.d64.image.read(pos);
		sector = drive.d.d64.image.read(pos + 1);
	
		while ((track >= 1) && (track <= 35))
		{
			pos = d64_tracksector2offset (track, sector);
			for (i = 2; i < 256; i += 32)
			{
				if ((drive.d.d64.image.read(pos + i) & 0x80) != 0)
				{
					if (stricmp (name, "*") == 0)
						return pos + i;
					if (cbm_compareNames (name, new UBytePtr(drive.d.d64.image, pos + i + 3)) != 0)
						return pos + i;
				}
			}
			track = drive.d.d64.image.read(pos);
			sector = drive.d.d64.image.read(pos + 1);
		}
		return -1;
	}
	
	/* reads file into buffer */
	public static void d64_readprg (CBM_Drive c1551, int pos)
	{
                //System.out.println("d64_readprg");
		int i;
	
		for (i = 0; i < 16; i++){
                        char[] _c = new char[1];
                        _c[0] = c1551.d.d64.image.read(pos + i + 3);
                        String _s=new String(_c).toUpperCase();
			c1551.d.d64.filename[i] = _s.charAt(0);
                }
	
		c1551.d.d64.filename[i] = 0;
	
		pos = d64_tracksector2offset (c1551.d.d64.image.read(pos + 1), c1551.d.d64.image.read(pos + 2));
	
		i = pos;
		c1551.size = 0;
		while (c1551.d.d64.image.read(i) != 0)
		{
                    c64_state();
			c1551.size += 254;
			i = d64_tracksector2offset (c1551.d.d64.image.read(i), c1551.d.d64.image.read(i + 1));
		}
		c1551.size += c1551.d.d64.image.read(i + 1);
	
/*TODO*///		DBG_LOG (3, "d64 readprg", ("size %d\n", c1551.size));
	
		//c1551.buffer = new UBytePtr(c1551.buffer, c1551.size);
                c1551.buffer = new UBytePtr(c1551.size);
/*TODO*///		if (c1551.buffer==null) {
/*TODO*///			logerror("out of memory %s %d\n",
/*TODO*///					__FILE__, __LINE__);
/*TODO*///			osd_exit();
/*TODO*///			exit(1);
/*TODO*///		}
	
		c1551.size--;
	
/*TODO*///		DBG_LOG (3, "d64 readprg", ("track: %d sector: %d\n",
/*TODO*///									c1551.d.d64.image[pos + 1],
/*TODO*///									c1551.d.d64.image[pos + 2]));
	
		for (i = 0; i < c1551.size; i += 254)
		{
                    c64_state();
			if (i + 254 < c1551.size)
			{							   /* not last sector */
				memcpy (new UBytePtr(c1551.buffer, i), new UBytePtr(c1551.d.d64.image, pos + 2), 254);
				pos = d64_tracksector2offset (c1551.d.d64.image.read(pos + 0),
										  c1551.d.d64.image.read(pos + 1));
/*TODO*///				DBG_LOG (3, "d64 readprg", ("track: %d sector: %d\n",
/*TODO*///											c1551.d.d64.image[pos],
/*TODO*///											c1551.d.d64.image[pos + 1]));
			}
			else
			{
				memcpy (new UBytePtr(c1551.buffer, i), new UBytePtr(c1551.d.d64.image, pos + 2), c1551.size - i);
			}
		}

	}
	
	/* reads sector into buffer */
	public static void d64_read_sector (CBM_Drive c1551, int track, int sector)
	{
		System.out.println("d64_read_sector TO BE IMPLEMENTED!");
                int pos;
/*TODO*///	
/*TODO*///		snprintf (c1551.d.d64.filename, sizeof (c1551.d.d64.filename),
/*TODO*///				  "track %d sector %d", track, sector);
/*TODO*///	
/*TODO*///		pos = d64_tracksector2offset (track, sector);
/*TODO*///	
/*TODO*///		c1551.buffer = (UINT8*)realloc (c1551.buffer,256);
/*TODO*///		if (!c1551.buffer) {
/*TODO*///			logerror("out of memory %s %d\n",__FILE__, __LINE__);
/*TODO*///			osd_exit();
/*TODO*///			exit(1);
/*TODO*///		}
/*TODO*///	
/*TODO*///		logerror("d64 read track %d sector %d\n", track, sector);
/*TODO*///	
/*TODO*///		memcpy (c1551.buffer, c1551.d.d64.image + pos, 256);
/*TODO*///		c1551.size = 256;
/*TODO*///		c1551.pos = 0;
	}
	
	/* reads directory into buffer */
	public static void d64_read_directory (CBM_Drive c1551)
	{
                System.out.println("d64_read_directory");
		int pos, track, sector, i, j, blocksfree, addr = 0x0101/*0x1001*/;
                
                //c1551.buffer = new UBytePtr (c1551.buffer, 8 * 18 * 25); // REALLOC
                //if (c1551.buffer==null)
                    c1551.buffer = new UBytePtr(8 * 18 * 25);
                
                /*char[] _tempMEM = c1551.buffer.memory;
	        c1551.buffer = new UBytePtr(8 * 18 * 25);
                
                int _longo = (_tempMEM.length<=c1551.buffer.memory.length)?_tempMEM.length : c1551.buffer.memory.length;
                
                for (int _i=0 ; _i<_longo; _i++)
                    c1551.buffer.memory[_i] = _tempMEM[_i];
	        */
                
                if (c1551.buffer==null) {
/*TODO*///                        logerror("out of memory %s %d\n",
/*TODO*///                                        __FILE__, __LINE__);
/*TODO*///                        osd_exit();
/*TODO*///                        exit(1);
                }

                c1551.size = 0;

                pos = d64_tracksector2offset (18, 0);
                track = c1551.d.d64.image.read(pos);
                sector = c1551.d.d64.image.read(pos + 1);

                blocksfree = 0;
                for (j = 1, i = 4; j <= 35; j++, i += 4)
                {
                        blocksfree += c1551.d.d64.image.read(pos + i);
                }
                c1551.buffer.write(c1551.size++, addr & 0xff);
                c1551.buffer.write(c1551.size++, addr >> 8);
                addr += 29;
                c1551.buffer.write(c1551.size++, addr & 0xff);
                c1551.buffer.write(c1551.size++, addr >> 8);
                c1551.buffer.write(c1551.size++,  0);
                c1551.buffer.write(c1551.size++, 0);
                c1551.buffer.write(c1551.size++, '\"');
                for (j = 0; j < 16; j++)
                        c1551.buffer.write(c1551.size++, c1551.d.d64.image.read(pos + 0x90 + j));
        /*memcpy(c1551.buffer+c1551.size,c1551.image+pos+0x90, 16);c1551.size+=16; */
                c1551.buffer.write(c1551.size++, '\"');
                c1551.buffer.write(c1551.size++, ' ');
                c1551.buffer.write(c1551.size++, c1551.d.d64.image.read(pos + 162));
                c1551.buffer.write(c1551.size++, c1551.d.d64.image.read(pos + 163));
                c1551.buffer.write(c1551.size++, ' ');
                c1551.buffer.write(c1551.size++, c1551.d.d64.image.read(pos + 165));
                c1551.buffer.write(c1551.size++, c1551.d.d64.image.read(pos + 166));
                c1551.buffer.write(c1551.size++, 0);

                while ((track >= 1) && (track <= 35))
                {
                        pos = d64_tracksector2offset (track, sector);
                        for (i = 2; i < 256; i += 32)
                        {
                                if ((c1551.d.d64.image.read(pos + i) & 0x80) != 0)
                                {
                                        int len, blocks = c1551.d.d64.image.read(pos + i + 28)
                                        + 256 * c1551.d.d64.image.read(pos + i + 29);
                                        String dummy="          ";

                                        dummy = sprintf ("%d", blocks);
                                        len = strlen (dummy);
                                        addr += 29 - len;
                                        c1551.buffer.write(c1551.size++, addr & 0xff);
                                        c1551.buffer.write(c1551.size++, addr >> 8);
                                        c1551.buffer.write(c1551.size++, c1551.d.d64.image.read(pos + i + 28));
                                        c1551.buffer.write(c1551.size++, c1551.d.d64.image.read(pos + i + 29));
                                        for (j = 4; j > len; j--)
                                                c1551.buffer.write(c1551.size++, ' ');
                                        c1551.buffer.write(c1551.size++, '\"');
                                        for (j = 0; j < 16; j++)
                                                c1551.buffer.write(c1551.size++, c1551.d.d64.image.read(pos + i + 3 + j));
                                        c1551.buffer.write(c1551.size++, '\"');
                                        c1551.buffer.write(c1551.size++, ' ');
                                        switch (c1551.d.d64.image.read(pos + i) & 0x3f)
                                        {
                                        case 0:
                                                c1551.buffer.write(c1551.size++, 'D');
                                                c1551.buffer.write(c1551.size++, 'E');
                                                c1551.buffer.write(c1551.size++, 'L');
                                                break;
                                        case 1:
                                                c1551.buffer.write(c1551.size++, 'S');
                                                c1551.buffer.write(c1551.size++, 'E');
                                                c1551.buffer.write(c1551.size++, 'Q');
                                                break;
                                        case 2:
                                                c1551.buffer.write(c1551.size++, 'P');
                                                c1551.buffer.write(c1551.size++, 'R');
                                                c1551.buffer.write(c1551.size++, 'G');
                                                break;
                                        case 3:
                                                c1551.buffer.write(c1551.size++, 'U');
                                                c1551.buffer.write(c1551.size++, 'S');
                                                c1551.buffer.write(c1551.size++, 'R');
                                                break;
                                        case 4:
                                                c1551.buffer.write(c1551.size++, 'R');
                                                c1551.buffer.write(c1551.size++, 'E');
                                                c1551.buffer.write(c1551.size++, 'L');
                                                break;
                                        }
                                        c1551.buffer.write(c1551.size++, 0);
                                }
                        }
                        track = c1551.d.d64.image.read(pos);
                        sector = c1551.d.d64.image.read(pos + 1);
                }
                addr += 14;
                c1551.buffer.write(c1551.size++, addr & 0xff);
                c1551.buffer.write(c1551.size++, addr >> 8);
                c1551.buffer.write(c1551.size++, blocksfree & 0xff);
                c1551.buffer.write(c1551.size++, blocksfree >> 8);
                memcpy (new UBytePtr(c1551.buffer, c1551.size), "BLOCKS FREE".toCharArray(), 11);
                c1551.size += 11;
                c1551.buffer.write(c1551.size++, 0);

                strcpy (c1551.d.d64.filename, "$");
                
                // fill until end buffer
                /*int _endBuf = (8 * 18 * 25);
                for (int _k=c1551.size ; _k<_endBuf ; _k++)
                    c1551.buffer.write(_k, 0);*/
	}
	
	public static int c1551_d64_command (CBM_Drive c1551, String name)
	{
                //System.out.println("c1551_d64_command "+name);
		int pos;
	
		/* name eventuell mit 0xa0 auffuellen */
	
		if (name.trim().equals("$"))
		{
			d64_read_directory (c1551);
		}
		else
		{
			if ((pos = d64_find (c1551, name)) == -1)
			{
				return 1;
			}
			d64_readprg (c1551, pos);
		}
		return 0;
	}
	
	public static int c1551_fs_command (CBM_Drive c1551, String name)
	{
            //System.out.println("c1551_fs_command TO BE IMPLEMENTED!!!!");
		Object fp;
		int type=0;
		int read;
		int i;
		String n="";
	
		n = name;
		fp = osd_fopen (Machine.gamedrv.name, n, OSD_FILETYPE_IMAGE_R, 0);
	
		if (fp == null)
		{
			n = n.toLowerCase();
			fp = osd_fopen (Machine.gamedrv.name, n, OSD_FILETYPE_IMAGE_R, 0);
		}
		if (fp == null)
		{
			n = name;
			n += ".prg";
	
			fp = osd_fopen (Machine.gamedrv.name, n, OSD_FILETYPE_IMAGE_R, 0);
		}
		if (fp == null)
		{
			n = n.toLowerCase();
			fp = osd_fopen (Machine.gamedrv.name, n, OSD_FILETYPE_IMAGE_R, 0);
		}
		if (fp == null)
		{
			type=1;
			n = name;
			n += ".p00";
			fp = osd_fopen (Machine.gamedrv.name, n, OSD_FILETYPE_IMAGE_R, 0);
		}
		if (fp == null)
		{
			n = n.toLowerCase();
			fp = osd_fopen (Machine.gamedrv.name, n, OSD_FILETYPE_IMAGE_R, 0);
		}
		if (fp != null)
		{
			if (type==1)
			{
				c1551.size = osd_fsize (fp);
				//c1551.buffer = (UINT8*)realloc (c1551.buffer, c1551.size);
                                c1551.buffer = new UBytePtr (c1551.buffer, c1551.size);
				if (c1551.buffer==null) {
/*TODO*///					logerror("out of memory %s %d\n",__FILE__, __LINE__);
/*TODO*///					osd_exit();
/*TODO*///					exit(1);
				}
	
				read = osd_fread (fp, c1551.buffer, 26);
				strncpy (c1551.d.fs.filename, new String((new UBytePtr(c1551.buffer, 8)).memory), 16);
				c1551.size -= 26;
				read = osd_fread (fp, c1551.buffer, c1551.size);
			}
			else
			{
				c1551.size = osd_fsize (fp);
				//c1551.buffer = (UINT8*)realloc (c1551.buffer,c1551.size);
                                c1551.buffer = new UBytePtr(c1551.buffer,c1551.size);
				if (c1551.buffer==null) {
/*TODO*///					logerror("out of memory %s %d\n",__FILE__, __LINE__);
/*TODO*///					osd_exit();
/*TODO*///					exit(1);
				}
	
				read = osd_fread (fp, c1551.buffer, c1551.size);
				osd_fclose (fp);
				logerror("loading file %s\n", name);
				c1551.d.fs.filename = name;
			}
		}
		else
		{
			logerror("file %s not found\n", name);
			return 1;
		}
		
                return 0;
	}
	
	/**
	  7.1 Serial bus
	
	   CBM Serial Bus Control Codes
	
		20	Talk
		3F	Untalk
		40	Listen
		5F	Unlisten
		60	Open Channel
		70	-
		80	-
		90	-
		A0	-
		B0	-
		C0	-
		D0	-
		E0	Close
		F0	Open
	
	
	
		 How the C1541 is called by the C64:
	
			read (drive 8)
			/28 /f0 filename /3f
			/48 /60 read data /5f
			/28 /e0 /3f
	
			write (drive 8)
			/28 /f0 filename /3f
			/28 /60 send data /3f
			/28 /e0 /3f
	
		 I used '/' to denote bytes sent under Attention (ATN low).
	
		28 == LISTEN command + device number 8
		f0 == secondary addres for OPEN file on channel 0
	
	  Note that there's no acknowledge bit, but timeout/EOI handshake for each
	  byte. Check the C64 Kernel for exact description...
	
	 computer master
	
	 c16 called
	 dload
	  20 f0 30 3a name 3f
	  40 60 listening 5f
	  20 e0 3f
	
	 load
	  20 f0 name 3f
	 */
	public static void cbm_command (CBM_Drive drive)
	{
            //System.out.println("cbm_command");
        
		char[] name=new char[20];
                char type = 'P';
                int mode = 0;
		int channel=0, head=0, track=0, sector=0;
		int j, i, rc;
	
		if ((drive.cmdpos == 4)
			&& ((drive.cmdbuffer[0] & 0xf0) == 0x20)
			&& ((drive.cmdbuffer[1] & 0xf0) == 0xf0)
			&& (drive.cmdbuffer[2] == '#')
			&& (drive.cmdbuffer[3] == 0x3f))
		{
			logerror("floppy direct access channel %d opened\n",
						 drive.cmdbuffer[1] & 0xf);
		}
		else if ((drive.cmdpos >= 4)
				 && ((drive.cmdbuffer[0] & 0xf0) == 0x20)
				 && ((drive.cmdbuffer[1] & 0xf0) == 0xf0)
				 && (drive.cmdbuffer[drive.cmdpos - 1] == 0x3f))
		{
			if (drive.cmdbuffer[3] == ':')
				j = 4;
			else
				j = 2;
	
			for (i = 0; (j < name.length) && (drive.cmdbuffer[j] != 0x3f)
				 && (drive.cmdbuffer[j] != ',');
				 i++, j++)
				name[i] = drive.cmdbuffer[j];
			name[i] = 0;
	
			if (drive.cmdbuffer[j] == ',')
			{
				j++;
				if (j < drive.cmdpos)
				{
					type = drive.cmdbuffer[j];
					j++;
					if ((j < drive.cmdpos) && (drive.cmdbuffer[j] == 'j'))
					{
						j++;
						if (j < drive.cmdpos)
							mode = drive.cmdbuffer[j];
					}
				}
			}
			rc = 1;
			if (drive.drive == D64_IMAGE)
			{
                            //System.out.println("cbm_command B");
				if ((type == 'P') || (type == 'S'))
					rc = c1551_d64_command (drive, new String(name));
			}
			else if (drive.drive == FILESYSTEM)
			{
				if (type == 'P')
					rc = c1551_fs_command (drive, new String(name));
			}
			if (rc == 0)
			{
				drive.state = OPEN;
				drive.pos = 0;
			}
			/*TODO*///DBG_LOG (1, "cbm_open", ("%s %s type:%c %c\n", name,
			/*TODO*///						 rc ? "failed" : "success", type, mode ? mode : ' '));
		}
		else if ((drive.cmdpos == 1) && (drive.cmdbuffer[0] == 0x5f))
		{
			drive.state = OPEN;
		}
		else if ((drive.cmdpos == 3) && ((drive.cmdbuffer[0] & 0xf0) == 0x20)
				 && ((drive.cmdbuffer[1] & 0xf0) == 0xe0)
				 && (drive.cmdbuffer[2] == 0x3f))
		{
	/*    if (drive.buffer) free(drive.buffer);drive.buffer=0; */
			drive.state = 0;
		}
		else if ((drive.cmdpos == 2) && ((drive.cmdbuffer[0] & 0xf0) == 0x40)
				 && ((drive.cmdbuffer[1] & 0xf0) == 0x60))
		{
			if (drive.state == OPEN)
			{
				drive.state = READING;
			}
		}
		else if ((drive.cmdpos == 2) && ((drive.cmdbuffer[0] & 0xf0) == 0x20)
				 && ((drive.cmdbuffer[1] & 0xf0) == 0x60))
		{
			drive.state = WRITING;
		}
		else if ((drive.cmdpos == 1) && (drive.cmdbuffer[0] == 0x3f))
		{
			drive.state = OPEN;
		}
		else if ((drive.drive == D64_IMAGE)
				 /*TODO*///&& (4 == sscanf (drive.cmdbuffer, "U1: %d %d %d %d\x0d",
				 /*TODO*///				  channel, head, track, sector))
                                                                  )
		{
                    //System.out.println("cbm_command C");
			d64_read_sector (drive, track, sector);
			drive.state = OPEN;
		}
		else
		{
	
			logerror("unknown floppycommand(size:%d):", drive.cmdpos);
			for (i = 0; i < drive.cmdpos; i++)
				logerror("%.2x", drive.cmdbuffer[i]);
			logerror(" ");
			for (i = 0; i < drive.cmdpos; i++)
				logerror("%c", drive.cmdbuffer[i]);
			logerror("\n");
	
			drive.state = 0;
		}
		drive.cmdpos = 0;
                //System.out.println("END cbm_command");
	}
/*TODO*///	
/*TODO*///	 /*
/*TODO*///	  * 0x55 begin of command
/*TODO*///	  *
/*TODO*///	  * frame
/*TODO*///	  * selector
/*TODO*///	  * 0x81 device id
/*TODO*///	  * 0x82 command
/*TODO*///	  * 0x83 data
/*TODO*///	  * 0x84 read byte
/*TODO*///	  * handshake low
/*TODO*///	  *
/*TODO*///	  * byte (like in serial bus!)
/*TODO*///	  *
/*TODO*///	  * floppy drive delivers
/*TODO*///	  * status 3 for file not found
/*TODO*///	  * or filedata ended with status 3
/*TODO*///	  */
/*TODO*///	void c1551_state (CBM_Drive * c1551)
/*TODO*///	{
/*TODO*///	#if VERBOSE_DBG
/*TODO*///		static int oldstate;
/*TODO*///	
/*TODO*///		oldstate = c1551.i.iec.state;
/*TODO*///	#endif
/*TODO*///	
/*TODO*///		switch (c1551.i.iec.state)
/*TODO*///		{
/*TODO*///		case -1:						   /* currently neccessary for correct init */
/*TODO*///			if (c1551.i.iec.handshakein)
/*TODO*///			{
/*TODO*///				c1551.i.iec.state++;
/*TODO*///			}
/*TODO*///			break;
/*TODO*///		case 0:
/*TODO*///			if (c1551.i.iec.datain == 0x55)
/*TODO*///			{
/*TODO*///				c1551.i.iec.status = 0;
/*TODO*///				c1551.i.iec.state++;
/*TODO*///			}
/*TODO*///			break;
/*TODO*///		case 1:
/*TODO*///			if (c1551.i.iec.datain != 0x55)
/*TODO*///			{
/*TODO*///				c1551.i.iec.state = 10;
/*TODO*///			}
/*TODO*///			break;
/*TODO*///		case 10:
/*TODO*///			if (c1551.i.iec.datain != 0)
/*TODO*///			{
/*TODO*///				c1551.i.iec.handshakeout = 0;
/*TODO*///				if (c1551.i.iec.datain == 0x84)
/*TODO*///				{
/*TODO*///					c1551.i.iec.state = 20;
/*TODO*///					if (c1551.pos + 1 == c1551.size)
/*TODO*///						c1551.i.iec.status = 3;
/*TODO*///				}
/*TODO*///				else if (c1551.i.iec.datain == 0x83)
/*TODO*///				{
/*TODO*///					c1551.i.iec.state = 40;
/*TODO*///				}
/*TODO*///				else
/*TODO*///				{
/*TODO*///					c1551.i.iec.status = 0;
/*TODO*///					c1551.i.iec.state++;
/*TODO*///				}
/*TODO*///			}
/*TODO*///			break;
/*TODO*///		case 11:
/*TODO*///			if (!c1551.i.iec.handshakein)
/*TODO*///			{
/*TODO*///				c1551.i.iec.state++;
/*TODO*///				DBG_LOG(1,"c1551",("taken data %.2x\n",c1551.i.iec.datain));
/*TODO*///				if (c1551.cmdpos < sizeof (c1551.cmdbuffer))
/*TODO*///					c1551.cmdbuffer[c1551.cmdpos++] = c1551.i.iec.datain;
/*TODO*///				if ((c1551.i.iec.datain == 0x3f) || (c1551.i.iec.datain == 0x5f))
/*TODO*///				{
/*TODO*///					cbm_command (c1551);
/*TODO*///					c1551.i.iec.state = 30;
/*TODO*///				}
/*TODO*///				else if (((c1551.i.iec.datain & 0xf0) == 0x60))
/*TODO*///				{
/*TODO*///					cbm_command (c1551);
/*TODO*///					if (c1551.state == READING)
/*TODO*///					{
/*TODO*///					}
/*TODO*///					else if (c1551.state == WRITING)
/*TODO*///					{
/*TODO*///					}
/*TODO*///					else
/*TODO*///						c1551.i.iec.status = 3;
/*TODO*///				}
/*TODO*///				c1551.i.iec.handshakeout = 1;
/*TODO*///			}
/*TODO*///			break;
/*TODO*///		case 12:
/*TODO*///			if (c1551.i.iec.datain == 0)
/*TODO*///			{
/*TODO*///				c1551.i.iec.state++;
/*TODO*///			}
/*TODO*///			break;
/*TODO*///		case 13:
/*TODO*///			if (c1551.i.iec.handshakein)
/*TODO*///			{
/*TODO*///				c1551.i.iec.state = 10;
/*TODO*///			}
/*TODO*///			break;
/*TODO*///	
/*TODO*///		case 20:						   /* reading data */
/*TODO*///			if (!c1551.i.iec.handshakein)
/*TODO*///			{
/*TODO*///				c1551.i.iec.handshakeout = 1;
/*TODO*///				if (c1551.state == READING)
/*TODO*///					c1551.i.iec.dataout = c1551.buffer[c1551.pos++];
/*TODO*///				c1551.i.iec.state++;
/*TODO*///			}
/*TODO*///			break;
/*TODO*///		case 21:						   /* reading data */
/*TODO*///			if (c1551.i.iec.handshakein)
/*TODO*///			{
/*TODO*///				c1551.i.iec.handshakeout = 0;
/*TODO*///				c1551.i.iec.state++;
/*TODO*///			}
/*TODO*///			break;
/*TODO*///		case 22:
/*TODO*///			if (c1551.i.iec.datain == 0)
/*TODO*///			{
/*TODO*///				c1551.i.iec.state++;
/*TODO*///			}
/*TODO*///			break;
/*TODO*///		case 23:
/*TODO*///			if (!c1551.i.iec.handshakein)
/*TODO*///			{
/*TODO*///				c1551.i.iec.handshakeout = 1;
/*TODO*///				if (c1551.state == READING)
/*TODO*///					c1551.i.iec.state = 10;
/*TODO*///				else
/*TODO*///					c1551.i.iec.state = 0;
/*TODO*///			}
/*TODO*///			break;
/*TODO*///	
/*TODO*///		case 30:						   /* end of command */
/*TODO*///			if (c1551.i.iec.datain == 0)
/*TODO*///			{
/*TODO*///				c1551.i.iec.state++;
/*TODO*///			}
/*TODO*///			break;
/*TODO*///		case 31:
/*TODO*///			if (c1551.i.iec.handshakein)
/*TODO*///			{
/*TODO*///				c1551.i.iec.state = 0;
/*TODO*///			}
/*TODO*///			break;
/*TODO*///	
/*TODO*///		case 40:						   /* simple write */
/*TODO*///			if (!c1551.i.iec.handshakein)
/*TODO*///			{
/*TODO*///				c1551.i.iec.state++;
/*TODO*///				if ((c1551.state == 0) || (c1551.state == OPEN))
/*TODO*///				{
/*TODO*///					DBG_LOG (1, "c1551", ("taken data %.2x\n",
/*TODO*///										  c1551.i.iec.datain));
/*TODO*///					if (c1551.cmdpos < sizeof (c1551.cmdbuffer))
/*TODO*///						c1551.cmdbuffer[c1551.cmdpos++] = c1551.i.iec.datain;
/*TODO*///				}
/*TODO*///				else if (c1551.state == WRITING)
/*TODO*///				{
/*TODO*///					DBG_LOG (1, "c1551", ("written data %.2x\n", c1551.i.iec.datain));
/*TODO*///				}
/*TODO*///				c1551.i.iec.handshakeout = 1;
/*TODO*///			}
/*TODO*///			break;
/*TODO*///		case 41:
/*TODO*///			if (c1551.i.iec.datain == 0)
/*TODO*///			{
/*TODO*///				c1551.i.iec.state++;
/*TODO*///			}
/*TODO*///			break;
/*TODO*///		case 42:
/*TODO*///			if (c1551.i.iec.handshakein)
/*TODO*///			{
/*TODO*///				c1551.i.iec.state = 10;
/*TODO*///			}
/*TODO*///			break;
/*TODO*///		}
/*TODO*///	#if VERBOSE_DBG
/*TODO*///		if (oldstate != c1551.i.iec.state)
/*TODO*///			logerror ("state %d.%d %d\n", oldstate, c1551.i.iec.state, c1551.state);
/*TODO*///	#endif
/*TODO*///	}
/*TODO*///	
	public static void vc1541_state (CBM_Drive vc1541)
	{
/*TODO*///	#if VERBOSE_DBG
/*TODO*///		int oldstate = vc1541.i.serial.state;
/*TODO*///	
/*TODO*///	#endif
            
            //System.out.println(vc1541.number);
	
		switch (vc1541.i.serial.state)
		{
		case 0:
			if (cbm_serial.atn[0]==0 && cbm_serial.clock[0]==0)
			{
				vc1541.i.serial.data = 0;
				vc1541.i.serial.state = 2;
				break;
			}
			if (cbm_serial.clock[0]==0 && vc1541.i.serial.forme!=0)
			{
				vc1541.i.serial.data = 0;
				vc1541.i.serial.state++;
				break;
			}
			break;
		case 1:
			if (cbm_serial.atn[0]==0)
			{
				vc1541.i.serial.state++;
				break;
			}
			if (cbm_serial.clock[0]!=0)
			{
				vc1541.i.serial.broadcast = 0;
				vc1541.i.serial.data = 1;
				vc1541.i.serial.state = 100;
				vc1541.i.serial.last = 0;
				vc1541.i.serial.value = 0;
				vc1541.i.serial.time = timer_get_time ();
				break;
			}
			break;
		case 2:
			if (cbm_serial.clock[0]!=0)
			{
				vc1541.i.serial.broadcast = 1;
				vc1541.i.serial.data = 1;
				vc1541.i.serial.state = 100;
				vc1541.i.serial.last = 0;
				vc1541.i.serial.value = 0;
				vc1541.i.serial.time = timer_get_time ();
				break;
			}
			break;
			/* bits to byte fitting */
		case 100:
			if (cbm_serial.clock[0]==0)
			{
				vc1541.i.serial.state++;
				break;
			}
			break;
		case 102:
		case 104:
		case 106:
		case 108:
		case 110:
		case 112:
		case 114:
			if (cbm_serial.clock[0]==0)
				vc1541.i.serial.state++;
			break;
		case 101:
			if (cbm_serial.clock[0]!=0)
			{
				vc1541.i.serial.value |= cbm_serial.data[0]!=0 ? 1 : 0;
				vc1541.i.serial.state++;
			}
			break;
		case 103:
			if (cbm_serial.clock[0]!=0)
			{
				vc1541.i.serial.value |= cbm_serial.data[0]!=0 ? 2 : 0;
				vc1541.i.serial.state++;
			}
			break;
		case 105:
			if (cbm_serial.clock[0]!=0)
			{
				vc1541.i.serial.value |= cbm_serial.data[0]!=0 ? 4 : 0;
				vc1541.i.serial.state++;
			}
			break;
		case 107:
			if (cbm_serial.clock[0]!=0)
			{
				vc1541.i.serial.value |= cbm_serial.data[0]!=0 ? 8 : 0;
				vc1541.i.serial.state++;
			}
			break;
		case 109:
			if (cbm_serial.clock[0]!=0)
			{
				vc1541.i.serial.value |= cbm_serial.data[0]!=0 ? 0x10 : 0;
				vc1541.i.serial.state++;
			}
			break;
		case 111:
			if (cbm_serial.clock[0]!=0)
			{
				vc1541.i.serial.value |= cbm_serial.data[0]!=0 ? 0x20 : 0;
				vc1541.i.serial.state++;
			}
			break;
		case 113:
			if (cbm_serial.clock[0]!=0)
			{
				vc1541.i.serial.value |= cbm_serial.data[0]!=0 ? 0x40 : 0;
				vc1541.i.serial.state++;
			}
			break;
		case 115:
			if (cbm_serial.clock[0]!=0)
			{
				vc1541.i.serial.value |= cbm_serial.data[0]!=0 ? 0x80 : 0;
				if (vc1541.i.serial.broadcast!=0
					&& (((vc1541.i.serial.value & 0xf0) == 0x20)
						|| ((vc1541.i.serial.value & 0xf0) == 0x40)))
				{
					vc1541.i.serial.forme = ((vc1541.i.serial.value & 0xf)
						== vc1541.i.serial.device)?1:0;
					if (vc1541.i.serial.forme==0)
					{
						vc1541.i.serial.state = 160;
						break;
					}
				}
				if (vc1541.i.serial.forme != 0)
				{
					if (vc1541.cmdpos < vc1541.cmdbuffer.length)
						vc1541.cmdbuffer[vc1541.cmdpos++] = (char) vc1541.i.serial.value;
					/*TODO*///DBG_LOG (1, "serial read", ("%s %s %.2x\n",
					/*TODO*///			vc1541.i.serial.broadcast ? "broad" : "",
					/*TODO*///			vc1541.i.serial.last ? "last" : "",
					/*TODO*///							vc1541.i.serial.value));
				}
				vc1541.i.serial.state++;
			}
			break;
		case 116:
			if (cbm_serial.clock[0]==0)
			{
				if (vc1541.i.serial.last != 0)
					vc1541.i.serial.state = 130;
				else
					vc1541.i.serial.state++;
				if (vc1541.i.serial.broadcast!=0 &&
					((vc1541.i.serial.value == 0x3f) || (vc1541.i.serial.value == 0x5f)
					 || ((vc1541.i.serial.value & 0xf0) == 0x60)))
				{
					cbm_command (vc1541);
				}
				vc1541.i.serial.time = timer_get_time ();
				vc1541.i.serial.data = 0;
				break;
			}
			break;
		case 117:
			if (vc1541.i.serial.forme!=0 && ((vc1541.i.serial.value & 0xf0) == 0x60)
				&& vc1541.i.serial.broadcast!=0 && cbm_serial.atn[0]!=0)
			{
				if (vc1541.state == READING)
				{
					vc1541.i.serial.state = 200;
					break;
				}
				else if (vc1541.state != WRITING)
				{
					vc1541.i.serial.state = 150;
					break;
				}
			}
			if (((vc1541.i.serial.value == 0x3f)
				 || (vc1541.i.serial.value == 0x5f))
				&& vc1541.i.serial.broadcast!=0 && cbm_serial.atn[0]!=0)
			{
				vc1541.i.serial.data = 1;
				vc1541.i.serial.state = 140;
				break;
			}
			if (cbm_serial.clock[0] != 0)
			{
				vc1541.i.serial.time = timer_get_time ();
				vc1541.i.serial.broadcast = cbm_serial.atn[0]!=0?0:1;
				vc1541.i.serial.data = 1;
				vc1541.i.serial.value = 0;
				vc1541.i.serial.state++;
				break;
			}
			break;
			/* if computer lowers clk not in 200micros (last byte following)
			 * negativ pulse on data by listener */
		case 118:
			if (cbm_serial.clock[0] == 0)
			{
				vc1541.i.serial.value = 0;
				vc1541.i.serial.state = 101;
				vc1541.i.serial.data = 1;
				break;
			}
			if (timer_get_time () - vc1541.i.serial.time >= 200e-6)
			{
				vc1541.i.serial.data = 0;
				vc1541.i.serial.last = 1;
				vc1541.i.serial.state++;
				vc1541.i.serial.time = timer_get_time ();
				break;
			}
			break;
		case 119:
			if (timer_get_time () - vc1541.i.serial.time >= 60e-6)
			{
				vc1541.i.serial.value = 0;
				vc1541.i.serial.data = 1;
				vc1541.i.serial.state = 100;
				break;
			}
			break;
	
		case 130:						   /* last byte of talk */
			if (timer_get_time () - vc1541.i.serial.time >= 60e-6)
			{
				vc1541.i.serial.data = 1;
				vc1541.i.serial.state = 0;
				break;
			}
			break;
	
		case 131:
			if (cbm_serial.clock[0] != 0)
			{
				vc1541.i.serial.state = 0;
			}
			break;
	
		case 140:						   /* end of talk */
			if (cbm_serial.clock[0] != 0)
			{
				vc1541.i.serial.state = 0;
			}
			break;
	
		case 150:						   /* file not found */
			if (cbm_serial.atn[0] != 0)
			{
				vc1541.i.serial.state++;
				vc1541.i.serial.time = timer_get_time ();
				break;
			}
			break;
		case 151:
			if (timer_get_time () - vc1541.i.serial.time > 1000e-6)
			{
				vc1541.i.serial.state++;
				vc1541.i.serial.clock = 0;
				vc1541.i.serial.time = timer_get_time ();
				break;
			}
			break;
		case 152:
			if (timer_get_time () - vc1541.i.serial.time > 50e-6)
			{
				vc1541.i.serial.state++;
				vc1541.i.serial.clock = 1;
				break;
			}
			break;
		case 153:
			if (cbm_serial.clock[0] != 0)
			{
				vc1541.i.serial.state = 0;
			}
			break;
	
		case 160:						   /* not for me */
			if (cbm_serial.atn[0] != 0)
			{
				vc1541.i.serial.state = 0;
				vc1541.i.serial.time = timer_get_time ();
				break;
			}
			break;
	
		case 200:
			if (cbm_serial.clock[0] != 0)
			{
				vc1541.i.serial.state++;
				vc1541.i.serial.clock = 0;
				vc1541.i.serial.time = timer_get_time ();
				break;
			}
			break;
		case 201:
			if (timer_get_time () - vc1541.i.serial.time > 80e-6)
			{
				vc1541.i.serial.clock = 1;
				vc1541.i.serial.data = 1;
				vc1541.i.serial.state = 300;
				break;
			}
			break;
	
		case 300:
			if (cbm_serial.atn[0]==0)
			{
				vc1541.i.serial.state = 330;
				vc1541.i.serial.data = 1;
				vc1541.i.serial.clock = 1;
				break;
			}
			if (cbm_serial.data[0] != 0)
			{
                            
				vc1541.i.serial.value = vc1541.buffer.read(vc1541.pos);
                                //System.out.println((char)vc1541.i.serial.value);
				vc1541.i.serial.clock = 0;
				vc1541.i.serial.data = (vc1541.i.serial.value & 1) != 0 ? 1 : 0;
				vc1541.i.serial.state++;
				vc1541.i.serial.time = timer_get_time ();
				break;
			}
			break;
		case 301:
			if (cbm_serial.atn[0]==0)
			{
				vc1541.i.serial.state = 330;
				vc1541.i.serial.data = 1;
				vc1541.i.serial.clock = 1;
				break;
			}
			if (timer_get_time () - vc1541.i.serial.time > 40e-6)
			{
				vc1541.i.serial.clock = 1;
				vc1541.i.serial.state++;
				vc1541.i.serial.time = timer_get_time ();
				break;
			}
			break;
		case 303:
		case 305:
		case 307:
		case 309:
		case 311:
		case 313:
		case 315:
			if (cbm_serial.atn[0]==0)
			{
				vc1541.i.serial.state = 330;
				vc1541.i.serial.data = 1;
				vc1541.i.serial.clock = 1;
				break;
			}
			if (timer_get_time () - vc1541.i.serial.time > 20e-6)
			{
				vc1541.i.serial.clock = 1;
				vc1541.i.serial.state++;
				vc1541.i.serial.time = timer_get_time ();
				break;
			}
			break;
		case 302:
			if (cbm_serial.atn[0]==0)
			{
				vc1541.i.serial.state = 330;
				vc1541.i.serial.data = 1;
				vc1541.i.serial.clock = 1;
				break;
			}
			if (timer_get_time () - vc1541.i.serial.time > 20e-6)
			{
				vc1541.i.serial.data = (vc1541.i.serial.value & 2) != 0 ? 1 : 0;
				vc1541.i.serial.clock = 0;
				vc1541.i.serial.state++;
				vc1541.i.serial.time = timer_get_time ();
				break;
			}
			break;
		case 304:
			if (cbm_serial.atn[0]==0)
			{
				vc1541.i.serial.state = 330;
				vc1541.i.serial.data = 1;
				vc1541.i.serial.clock = 1;
				break;
			}
			if (timer_get_time () - vc1541.i.serial.time > 20e-6)
			{
				vc1541.i.serial.data = (vc1541.i.serial.value & 4) != 0 ? 1 : 0;
				vc1541.i.serial.clock = 0;
				vc1541.i.serial.state++;
				vc1541.i.serial.time = timer_get_time ();
				break;
			}
			break;
		case 306:
			if (cbm_serial.atn[0]==0)
			{
				vc1541.i.serial.state = 330;
				vc1541.i.serial.data = 1;
				vc1541.i.serial.clock = 1;
				break;
			}
			if (timer_get_time () - vc1541.i.serial.time > 20e-6)
			{
				vc1541.i.serial.data = (vc1541.i.serial.value & 8) != 0 ? 1 : 0;
				vc1541.i.serial.clock = 0;
				vc1541.i.serial.state++;
				vc1541.i.serial.time = timer_get_time ();
				break;
			}
			break;
		case 308:
			if (cbm_serial.atn[0]==0)
			{
				vc1541.i.serial.state = 330;
				vc1541.i.serial.data = 1;
				vc1541.i.serial.clock = 1;
				break;
			}
			if (timer_get_time () - vc1541.i.serial.time > 20e-6)
			{
				vc1541.i.serial.data = (vc1541.i.serial.value & 0x10) != 0 ? 1 : 0;
				vc1541.i.serial.clock = 0;
				vc1541.i.serial.state++;
				vc1541.i.serial.time = timer_get_time ();
				break;
			}
			break;
		case 310:
			if (cbm_serial.atn[0]==0)
			{
				vc1541.i.serial.state = 330;
				vc1541.i.serial.data = 1;
				vc1541.i.serial.clock = 1;
				break;
			}
			if (timer_get_time () - vc1541.i.serial.time > 20e-6)
			{
				vc1541.i.serial.data = (vc1541.i.serial.value & 0x20) != 0 ? 1 : 0;
				vc1541.i.serial.clock = 0;
				vc1541.i.serial.state++;
				vc1541.i.serial.time = timer_get_time ();
				break;
			}
			break;
		case 312:
			if (cbm_serial.atn[0]==0)
			{
				vc1541.i.serial.state = 330;
				vc1541.i.serial.data = 1;
				vc1541.i.serial.clock = 1;
				break;
			}
			if (timer_get_time () - vc1541.i.serial.time > 20e-6)
			{
				vc1541.i.serial.data = (vc1541.i.serial.value & 0x40) != 0 ? 1 : 0;
				vc1541.i.serial.clock = 0;
				vc1541.i.serial.state++;
				vc1541.i.serial.time = timer_get_time ();
				break;
			}
			break;
		case 314:
			if (cbm_serial.atn[0]==0)
			{
				vc1541.i.serial.state = 330;
				vc1541.i.serial.data = 1;
				vc1541.i.serial.clock = 1;
				break;
			}
			if (timer_get_time () - vc1541.i.serial.time > 20e-6)
			{
				vc1541.i.serial.data = (vc1541.i.serial.value & 0x80) != 0 ? 1 : 0;
				vc1541.i.serial.clock = 0;
				vc1541.i.serial.state++;
				vc1541.i.serial.time = timer_get_time ();
				break;
			}
			break;
		case 316:
			if (cbm_serial.atn[0]==0)
			{
				vc1541.i.serial.state = 330;
				vc1541.i.serial.data = 1;
				vc1541.i.serial.clock = 1;
				break;
			}
			if (timer_get_time () - vc1541.i.serial.time > 20e-6)
			{
				/*TODO*///DBG_LOG (1, "vc1541", ("%.2x written\n", vc1541.i.serial.value));
				vc1541.i.serial.data = 1;
				vc1541.i.serial.clock = 0;
				vc1541.i.serial.state++;
				vc1541.i.serial.time = timer_get_time ();
				break;
			}
			break;
		case 317:
			if (cbm_serial.atn[0]==0)
			{
				vc1541.i.serial.state = 330;
				vc1541.i.serial.data = 1;
				vc1541.i.serial.clock = 1;
				break;
			}
			if (cbm_serial.data[0]==0)
			{
				vc1541.i.serial.state++;
				vc1541.i.serial.time = timer_get_time ();
				break;
			}
			break;
		case 318:
			if (cbm_serial.atn[0]==0)
			{
				vc1541.i.serial.state = 330;
				vc1541.i.serial.data = 1;
				vc1541.i.serial.clock = 1;
				break;
			}
			if (vc1541.pos + 1 == vc1541.size)
			{
				vc1541.i.serial.clock = 1;
				vc1541.i.serial.state = 0;
				break;
			}
			if (vc1541.pos + 2 == vc1541.size)
			{
				vc1541.pos++;
				vc1541.i.serial.state = 320;
				break;
			}
			if (timer_get_time () - vc1541.i.serial.time > 100e-6)
			{
				vc1541.pos++;
				vc1541.i.serial.clock = 1;
				vc1541.i.serial.state = 300;
				break;
			}
			break;
		case 320:
			if (cbm_serial.atn[0]==0)
			{
				vc1541.i.serial.state = 330;
				vc1541.i.serial.data = 1;
				vc1541.i.serial.clock = 1;
				break;
			}
			if (timer_get_time () - vc1541.i.serial.time > 100e-6)
			{
				vc1541.i.serial.clock = 1;
				vc1541.i.serial.state++;
				break;
			}
			break;
		case 321:
			if (cbm_serial.atn[0]==0)
			{
				vc1541.i.serial.state = 330;
				vc1541.i.serial.data = 1;
				vc1541.i.serial.clock = 1;
				break;
			}
			if (cbm_serial.data[0] != 0)
			{
				vc1541.i.serial.state++;
			}
			break;
		case 322:
			if (cbm_serial.atn[0]==0)
			{
				vc1541.i.serial.state = 330;
				vc1541.i.serial.data = 1;
				vc1541.i.serial.clock = 1;
				break;
			}
			if (cbm_serial.data[0]==0)
			{
				vc1541.i.serial.state++;
			}
			break;
		case 323:
			if (cbm_serial.atn[0]==0)
			{
				vc1541.i.serial.state = 330;
				vc1541.i.serial.data = 1;
				vc1541.i.serial.clock = 1;
				break;
			}
			if (cbm_serial.data[0] != 0)
			{
				vc1541.i.serial.state = 300;
			}
			break;
		case 330:						   /* computer breaks receiving */
			vc1541.i.serial.state = 0;
			break;
		}
/*TODO*///	#if VERBOSE_DBG
/*TODO*///		if (oldstate != vc1541.i.serial.state)
/*TODO*///			logerror ("%d state %d.%d %d %s %s %s\n",
/*TODO*///					 vc1541.i.serial.device,
/*TODO*///					 oldstate,
/*TODO*///					 vc1541.i.serial.state, vc1541.state,
/*TODO*///					 cbm_serial.atn[0] ? "ATN" : "atn",
/*TODO*///					 cbm_serial.clock[0] ? "CLOCK" : "clock",
/*TODO*///					 cbm_serial.data[0] ? "DATA" : "data");
/*TODO*///	#endif
                
                // HACK
                //cbm_serial.drives[vc1541.number] = vc1541;
	}
/*TODO*///	
/*TODO*///	/* difference between vic20 and pet (first series)
/*TODO*///	   pet lowers atn and wants a reaction on ndac */
/*TODO*///	
/*TODO*///	void c2031_state(CBM_Drive *drive)
/*TODO*///	{
/*TODO*///	#if VERBOSE_DBG
/*TODO*///		int oldstate = drive.i.ieee.state;
/*TODO*///	#endif
/*TODO*///		int data;
/*TODO*///	
/*TODO*///		switch (drive.i.ieee.state)
/*TODO*///		{
/*TODO*///		case 0:
/*TODO*///			if (!cbm_ieee_dav_r()) {
/*TODO*///				drive.i.ieee.state=10;
/*TODO*///			} else if (!cbm_ieee_atn_r()) {
/*TODO*///				drive.i.ieee.state=11;
/*TODO*///				cbm_ieee_ndac_w(1,0);
/*TODO*///				logerror("arsch\n");
/*TODO*///			}
/*TODO*///			break;
/*TODO*///		case 1:
/*TODO*///			break;
/*TODO*///		case 10:
/*TODO*///			if (cbm_ieee_dav_r()) {
/*TODO*///				drive.i.ieee.state++;
/*TODO*///				cbm_ieee_nrfd_w(1,1);
/*TODO*///				cbm_ieee_ndac_w(1,0);
/*TODO*///			}
/*TODO*///			break;
/*TODO*///		case 11:
/*TODO*///			if (!cbm_ieee_dav_r()) {
/*TODO*///				cbm_ieee_nrfd_w(1,0);
/*TODO*///				data=cbm_ieee_data_r()^0xff;
/*TODO*///				cbm_ieee_ndac_w(1,1);
/*TODO*///				logerror("byte received %.2x\n",data);
/*TODO*///				if (!cbm_ieee_atn_r()&&((data&0x0f)==drive.i.ieee.device) ) {
/*TODO*///					if ((data&0xf0)==0x40)
/*TODO*///						drive.i.ieee.state=30;
/*TODO*///					else
/*TODO*///						drive.i.ieee.state=20;
/*TODO*///					if (drive.cmdpos < sizeof (drive.cmdbuffer))
/*TODO*///						drive.cmdbuffer[drive.cmdpos++] = data;
/*TODO*///				} else if ((data&0xf)==0xf) {
/*TODO*///					drive.i.ieee.state--;
/*TODO*///				} else {
/*TODO*///					drive.i.ieee.state++;
/*TODO*///				}
/*TODO*///			}
/*TODO*///			break;
/*TODO*///			/* wait until atn is released */
/*TODO*///		case 12:
/*TODO*///			if (cbm_ieee_atn_r()) {
/*TODO*///				drive.i.ieee.state++;
/*TODO*///				cbm_ieee_nrfd_w(1,0);
/*TODO*///			}
/*TODO*///			break;
/*TODO*///		case 13:
/*TODO*///			if (!cbm_ieee_atn_r()) {
/*TODO*///				drive.i.ieee.state=10;
/*TODO*///	/*			cbm_ieee_nrfd_w(1,0); */
/*TODO*///			}
/*TODO*///			break;
/*TODO*///	
/*TODO*///			/* receiving rest of command */
/*TODO*///		case 20:
/*TODO*///			if (cbm_ieee_dav_r()) {
/*TODO*///				drive.i.ieee.state++;
/*TODO*///				cbm_ieee_nrfd_w(1,1);
/*TODO*///				cbm_ieee_ndac_w(1,0);
/*TODO*///			}
/*TODO*///			break;
/*TODO*///		case 21:
/*TODO*///			if (!cbm_ieee_dav_r()) {
/*TODO*///				cbm_ieee_nrfd_w(1,0);
/*TODO*///				data=cbm_ieee_data_r()^0xff;
/*TODO*///				logerror("byte received %.2x\n",data);
/*TODO*///				if (drive.cmdpos < sizeof (drive.cmdbuffer))
/*TODO*///					drive.cmdbuffer[drive.cmdpos++] = data;
/*TODO*///				if (!cbm_ieee_atn_r()&&((data&0xf)==0xf)) {
/*TODO*///					cbm_command(drive);
/*TODO*///					drive.i.ieee.state=10;
/*TODO*///				} else
/*TODO*///					drive.i.ieee.state=20;
/*TODO*///				cbm_ieee_ndac_w(1,1);
/*TODO*///			}
/*TODO*///			break;
/*TODO*///	
/*TODO*///			/* read command */
/*TODO*///		case 30:
/*TODO*///			if (cbm_ieee_dav_r()) {
/*TODO*///				drive.i.ieee.state++;
/*TODO*///				cbm_ieee_nrfd_w(1,1);
/*TODO*///				cbm_ieee_ndac_w(1,0);
/*TODO*///			}
/*TODO*///			break;
/*TODO*///		case 31:
/*TODO*///			if (!cbm_ieee_dav_r()) {
/*TODO*///				cbm_ieee_nrfd_w(1,0);
/*TODO*///				data=cbm_ieee_data_r()^0xff;
/*TODO*///				logerror("byte received %.2x\n",data);
/*TODO*///				if (drive.cmdpos < sizeof (drive.cmdbuffer))
/*TODO*///					drive.cmdbuffer[drive.cmdpos++] = data;
/*TODO*///				cbm_command(drive);
/*TODO*///				if (drive.state==READING)
/*TODO*///					drive.i.ieee.state++;
/*TODO*///				else
/*TODO*///					drive.i.ieee.state=10;
/*TODO*///				cbm_ieee_ndac_w(1,1);
/*TODO*///			}
/*TODO*///			break;
/*TODO*///		case 32:
/*TODO*///			if (cbm_ieee_dav_r()) {
/*TODO*///				cbm_ieee_nrfd_w(1,1);
/*TODO*///				drive.i.ieee.state=40;
/*TODO*///			}
/*TODO*///			break;
/*TODO*///		case 40:
/*TODO*///			if (!cbm_ieee_ndac_r()) {
/*TODO*///				cbm_ieee_data_w(1,drive.buffer[drive.pos++]^0xff);
/*TODO*///				if (drive.pos>=drive.size)
/*TODO*///					cbm_ieee_eoi_w(1,0);
/*TODO*///				cbm_ieee_dav_w(1,0);
/*TODO*///				drive.i.ieee.state++;
/*TODO*///			}
/*TODO*///			break;
/*TODO*///		case 41:
/*TODO*///			if (!cbm_ieee_nrfd_r()) {
/*TODO*///				drive.i.ieee.state++;
/*TODO*///			}
/*TODO*///			break;
/*TODO*///		case 42:
/*TODO*///			if (cbm_ieee_ndac_r()) {
/*TODO*///				if (cbm_ieee_eoi_r())
/*TODO*///					drive.i.ieee.state=40;
/*TODO*///				else {
/*TODO*///					cbm_ieee_data_w(1,0xff);
/*TODO*///					cbm_ieee_ndac_w(1,0);
/*TODO*///					cbm_ieee_nrfd_w(1,0);
/*TODO*///					cbm_ieee_eoi_w(1,1);
/*TODO*///					drive.i.ieee.state=10;
/*TODO*///				}
/*TODO*///				cbm_ieee_dav_w(1,1);
/*TODO*///			}
/*TODO*///			break;
/*TODO*///		}
/*TODO*///	
/*TODO*///	#if VERBOSE_DBG
/*TODO*///		if (oldstate != drive.i.ieee.state)
/*TODO*///			logerror("%d state %d.%d %d\n",
/*TODO*///					 drive.i.ieee.device,
/*TODO*///					 oldstate,
/*TODO*///					 drive.i.ieee.state, drive.state
/*TODO*///					 );
/*TODO*///	#endif
/*TODO*///	}
}
