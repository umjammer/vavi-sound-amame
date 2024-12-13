package mess056;
/*
This file is a set of function calls and defs required for MESS.
*/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 

import static mame056.version.build_version;
import static mess056.deviceH.*;
import static mess056.device.devices;
import static mess056.messH.*;
import static mess056.machine.flopdrv.*;
import static mess056.includes.flopdrvH.*;

import static mame056.driverH.*;

import static arcadeflex056.osdepend.logerror;
import static arcadeflex056.fileio.*;
import static common.libc.cstring.*;
import static common.libc.cstdio.*;
import static common.ptr.*;
import mame056.commonH.mame_bitmap;
import static mame056.cpuexec.machine_reset;
import static mame056.inptportH.*;
import static mame056.input.*;
import static mame056.inputH.*;
import static mame056.mame.*;
import static mame056.osdependH.OSD_FILETYPE_NVRAM;
import static mame056.usrintrf.*;

public class mess
{
	
/*TODO*///	extern struct GameOptions options;
/*TODO*///	extern const struct Devices devices[];
/*TODO*///	
/*TODO*///	/* CRC database file for this driver, supplied by the OS specific code */
/*TODO*///	extern const char *crcfile;
/*TODO*///	extern const char *pcrcfile;
/*TODO*///	
	
	/* Globals */
	static int mess_keep_going;
	static String renamed_image;
	
	public static class image_info {
		String name = null;
		int crc;
		int length;
		String longname;
		String manufacturer;
		String year;
		String playable;
		String extrainfo;
	};
	
	public static int MAX_INSTANCES = 5;
	public static image_info[][] images = new image_info[IO_COUNT][MAX_INSTANCES];
        public static int[] count = new int[IO_COUNT];
        
        static {
          for (int i=0 ; i<IO_COUNT ; i++)
              for (int j=0 ; j<MAX_INSTANCES ; j++)
                  images[i][j]=new image_info();
        };
	

		public static String dupe(String src)
		{
			/*TODO*///	if (src != 0)
			/*TODO*///	{
			/*TODO*///		char *dst = malloc(strlen(src) + 1);
			/*TODO*///		if (dst != 0)
			/*TODO*///			strcpy(dst,src);
			/*TODO*///		return dst;
			/*TODO*///	}
			/*TODO*///	return NULL;
		    return src;
		}


/*TODO*///	static char* stripspace(const char *src)
/*TODO*///	{
/*TODO*///		static char buff[512];
/*TODO*///		if( src )
/*TODO*///		{
/*TODO*///			char *dst;
/*TODO*///			while( *src && isspace(*src) )
/*TODO*///				src++;
/*TODO*///			strcpy(buff, src);
/*TODO*///			dst = buff + strlen(buff);
/*TODO*///			while( dst >= buff && isspace(*--dst) )
/*TODO*///				*dst = '\0';
/*TODO*///			return buff;
/*TODO*///		}
/*TODO*///		return NULL;
/*TODO*///	}
/*TODO*///
/*TODO*///	int DECL_SPEC mess_printf(char *fmt, ...)	
	public static int mess_printf(String fmt, Object... arguments)
	{
		System.out.printf(fmt, arguments);
/*TODO*///		va_list arg;
		int length = 0;
/*TODO*///	
/*TODO*///		va_start(arg,fmt);
/*TODO*///	
/*TODO*///		if (options.mess_printf_output)
/*TODO*///			length = options.mess_printf_output(fmt, arg);
/*TODO*///		else if (!options.gui_host)
/*TODO*///			length = vprintf(fmt, arg);
/*TODO*///	
/*TODO*///		va_end(arg);
/*TODO*///	
		return length;
	}
	
/*TODO*///	static int read_crc_config (const char *file, struct image_info *img, const char* sysname)
/*TODO*///	{
/*TODO*///		int retval;
/*TODO*///		void *config = config_open (file);
/*TODO*///	
/*TODO*///		retval = 1;
/*TODO*///		if( config )
/*TODO*///		{
/*TODO*///			char line[1024];
/*TODO*///			char crc[9+1];
/*TODO*///	
/*TODO*///			sprintf(crc, "%08x", img->crc);
/*TODO*///			config_load_string(config,sysname,0,crc,line,sizeof(line));
/*TODO*///			if( line[0] )
/*TODO*///			{
/*TODO*///				logerror("found CRC %s= %s\n", crc, line);
/*TODO*///				img->longname = dupe(stripspace(strtok(line, "|")));
/*TODO*///				img->manufacturer = dupe(stripspace(strtok(NULL, "|")));
/*TODO*///				img->year = dupe(stripspace(strtok(NULL, "|")));
/*TODO*///				img->playable = dupe(stripspace(strtok(NULL, "|")));
/*TODO*///				img->extrainfo = dupe(stripspace(strtok(NULL, "|")));
/*TODO*///				retval = 0;
/*TODO*///			}
/*TODO*///			config_close(config);
/*TODO*///		}
/*TODO*///		return retval;
/*TODO*///	}
/*TODO*///	
/*TODO*///	
	public static Object image_fopen(int type, int id, int filetype, int read_or_write)
    {
        //System.out.println("image_fopen "+filetype);
        image_info img = images[type][id];
	    String sysname;
	    Object file;
	    int extnum;
	
	    if (type >= IO_COUNT) {
	        logerror("image_fopen: type out of range (%d)\n", type);
	        return null;
	    }
	
	    if (id >= count[type]) {
	        logerror("image_fopen: id out of range (%d)\n", id);
	        return null;
	    }
	
	    if (img.name == null) {
	        return null;
	    }
	
	    /* try the supported extensions */
	    extnum = 0;
	    for (;;) {
	        String ext;
	        String p;
	
	        sysname = Machine.gamedrv.name;
	        logerror("image_fopen: trying %s for system %s\n", img.name, sysname);
	
	        //System.out.println("image_fopen: trying %s for system %s\n");
	        //System.out.println(img.name);
	        //System.out.println(sysname);
	
	        file = osd_fopen(sysname, img.name, filetype, read_or_write);
	        /* file found, break out */
	        //System.out.println("file==null " + file);
	        if (file != null) {
	            break;
	        }
	        /*TODO*///		if( Machine->gamedrv->clone_of &&
	/*TODO*///			Machine->gamedrv->clone_of != &driver_0 )
	/*TODO*///		{
	/*TODO*///			sysname = Machine->gamedrv->clone_of->name;
	/*TODO*///			logerror("image_fopen: now trying %s for system %s\n", img->name, sysname);
	/*TODO*///			file = osd_fopen(sysname, img->name, filetype, read_or_write);
	/*TODO*///		}
	/*TODO*///		if( file )
	/*TODO*///			break;
	/*TODO*///
	        ext = device_file_extension(type, extnum);
	        //System.out.println(ext);
	        extnum++;
	
	        /* no (more) extensions, break out */
	        if (ext == null) {
	            break;
	        }
	        p = strrchr(img.name, '.');
	        //System.out.println("p: " + p);
	        //System.out.println("img.name: " + img.name);
	        //System.out.println("ext: " + ext);
	
	        /* does the current name already have an extension? */
	        if (p == null) {
	            throw new UnsupportedOperationException("unimplemented");
	            /*TODO*///			++p; /* skip the dot */
	/*TODO*///			/* new extension won't fit? */
	/*TODO*///			if( strlen(p) < strlen(ext) )
	/*TODO*///			{
	/*TODO*///				img->name = realloc(img->name, l - strlen(p) + strlen(ext) + 1);
	/*TODO*///				if( !img->name )
	/*TODO*///				{
	/*TODO*///					logerror("image_fopen: realloc failed.. damn it!\n");
	/*TODO*///					return NULL;
	/*TODO*///				}
	/*TODO*///			}
	/*TODO*///			strcpy(p, ext);
	        } else {
	            img.name += sprintf(".%s", ext);
	        }
	    }
	    /*TODO*///
	/*TODO*///	if( file )
	/*TODO*///	{
	/*TODO*///		void *config;
	/*TODO*///		const struct IODevice *pc_dev = Machine->gamedrv->dev;
	/*TODO*///
	/*TODO*///		logerror("image_fopen: found image %s for system %s\n", img->name, sysname);
	/*TODO*///		img->length = osd_fsize(file);
	/*TODO*////* Cowering, partial crcs for NES/A7800/others */
	/*TODO*///		img->crc = 0;
	/*TODO*///		while( pc_dev && pc_dev->count && !img->crc)
	/*TODO*///		{
	/*TODO*///			logerror("partialcrc() -> %08lx\n",pc_dev->partialcrc);
	/*TODO*///			if( type == pc_dev->type && pc_dev->partialcrc )
	/*TODO*///			{
	/*TODO*///				unsigned char *pc_buf = (unsigned char *)malloc(img->length);
	/*TODO*///				if( pc_buf )
	/*TODO*///				{
	/*TODO*///					osd_fseek(file,0,SEEK_SET);
	/*TODO*///					osd_fread(file,pc_buf,img->length);
	/*TODO*///					osd_fseek(file,0,SEEK_SET);
	/*TODO*///					logerror("Calling partialcrc()\n");
	/*TODO*///					img->crc = (*pc_dev->partialcrc)(pc_buf,img->length);
	/*TODO*///					free(pc_buf);
	/*TODO*///				}
	/*TODO*///				else
	/*TODO*///				{
	/*TODO*///					logerror("failed to malloc(%d)\n", img->length);
	/*TODO*///				}
	/*TODO*///			}
	/*TODO*///			pc_dev++;
	/*TODO*///		}
	/*TODO*///
	/*TODO*///		if (!img->crc) img->crc = osd_fcrc(file);
	/*TODO*///		if( img->crc == 0 && img->length < 0x100000 )
	/*TODO*///		{
	/*TODO*///			logerror("image_fopen: calling osd_fchecksum() for %d bytes\n", img->length);
	/*TODO*///			osd_fchecksum(sysname, img->name, &img->length, &img->crc);
	/*TODO*///			logerror("image_fopen: CRC is %08x\n", img->crc);
	/*TODO*///		}
	/*TODO*///		free_image_info(img);
	/*TODO*///
	/*TODO*///		if (read_crc_config (crcfile, img, sysname) && Machine->gamedrv->clone_of->name)
	/*TODO*///			read_crc_config (pcrcfile, img, Machine->gamedrv->clone_of->name);
	/*TODO*///
	/*TODO*///		config = config_open(crcfile);
	/*TODO*///	}
	/*TODO*///
	    return file;
	}
	
/*TODO*///	void *image_fopen(int type, int id, int filetype, int read_or_write)
/*TODO*///	{
/*TODO*///		struct image_info *img = &images[type][id];
/*TODO*///		const char *sysname;
/*TODO*///		void *file;
/*TODO*///		int extnum;
/*TODO*///		int original_len;
/*TODO*///	
/*TODO*///		if( type >= IO_COUNT )
/*TODO*///		{
/*TODO*///			logerror("image_fopen: type out of range (%d)\n", type);
/*TODO*///			return NULL;
/*TODO*///		}
/*TODO*///	
/*TODO*///		if( id >= count[type] )
/*TODO*///		{
/*TODO*///			logerror("image_fopen: id out of range (%d)\n", id);
/*TODO*///			return NULL;
/*TODO*///		}
/*TODO*///	
/*TODO*///		if( img->name == NULL )
/*TODO*///			return NULL;
/*TODO*///	
/*TODO*///		/* try the supported extensions */
/*TODO*///		extnum = 0;
/*TODO*///	
/*TODO*///		/* remember original file name */
/*TODO*///		original_len = strlen(img->name);
/*TODO*///	
/*TODO*///		{
/*TODO*///			extern struct GameDriver driver_0;
/*TODO*///	
/*TODO*///			sysname = Machine->gamedrv->name;
/*TODO*///			logerror("image_fopen: trying %s for system %s\n", img->name, sysname);
/*TODO*///			file = osd_fopen(sysname, img->name, filetype, read_or_write);
/*TODO*///			/* file found, break out */
/*TODO*///			if (file == 0)
/*TODO*///			{
/*TODO*///				if( Machine->gamedrv->clone_of &&
/*TODO*///					Machine->gamedrv->clone_of != &driver_0 )
/*TODO*///				{	/* R Nabet : Shouldn't this be moved to osd code ??? Mac osd code does such a retry
/*TODO*///					whenever it makes sense, and I think this is the correct way. */
/*TODO*///					sysname = Machine->gamedrv->clone_of->name;
/*TODO*///					logerror("image_fopen: now trying %s for system %s\n", img->name, sysname);
/*TODO*///					file = osd_fopen(sysname, img->name, filetype, read_or_write);
/*TODO*///				}
/*TODO*///			}
/*TODO*///		}
/*TODO*///	
/*TODO*///		if (file)
/*TODO*///		{
/*TODO*///			void *config;
/*TODO*///			const struct IODevice *pc_dev = Machine->gamedrv->dev;
/*TODO*///	
/*TODO*///			/* did osd_fopen() rename the image? (yes, I know this is a hack) */
/*TODO*///			if (renamed_image)
/*TODO*///			{
/*TODO*///				free(img->name);
/*TODO*///				img->name = renamed_image;
/*TODO*///				renamed_image = NULL;
/*TODO*///			}
/*TODO*///	
/*TODO*///			logerror("image_fopen: found image %s for system %s\n", img->name, sysname);
/*TODO*///			img->length = osd_fsize(file);
/*TODO*///	/* Cowering, partial crcs for NES/A7800/others */
/*TODO*///			img->crc = 0;
/*TODO*///			while( pc_dev && pc_dev->count && !img->crc)
/*TODO*///			{
/*TODO*///				logerror("partialcrc() -> %08lx\n",pc_dev->partialcrc);
/*TODO*///				if( type == pc_dev->type && pc_dev->partialcrc )
/*TODO*///				{
/*TODO*///					unsigned char *pc_buf = (unsigned char *)malloc(img->length);
/*TODO*///					if( pc_buf )
/*TODO*///					{
/*TODO*///						osd_fseek(file,0,SEEK_SET);
/*TODO*///						osd_fread(file,pc_buf,img->length);
/*TODO*///						osd_fseek(file,0,SEEK_SET);
/*TODO*///						logerror("Calling partialcrc()\n");
/*TODO*///						img->crc = (*pc_dev->partialcrc)(pc_buf,img->length);
/*TODO*///						free(pc_buf);
/*TODO*///					}
/*TODO*///					else
/*TODO*///					{
/*TODO*///						logerror("failed to malloc(%d)\n", img->length);
/*TODO*///					}
/*TODO*///				}
/*TODO*///				pc_dev++;
/*TODO*///			}
/*TODO*///	
/*TODO*///			if (!img->crc) img->crc = osd_fcrc(file);
/*TODO*///			if( img->crc == 0 && img->length < 0x100000 )
/*TODO*///			{
/*TODO*///				logerror("image_fopen: calling osd_fchecksum() for %d bytes\n", img->length);
/*TODO*///				osd_fchecksum(sysname, img->name, &img->length, &img->crc);
/*TODO*///				logerror("image_fopen: CRC is %08x\n", img->crc);
/*TODO*///			}
/*TODO*///	
/*TODO*///			if (read_crc_config (crcfile, img, sysname) && Machine->gamedrv->clone_of->name)
/*TODO*///				read_crc_config (pcrcfile, img, Machine->gamedrv->clone_of->name);
/*TODO*///	
/*TODO*///			config = config_open(crcfile);
/*TODO*///		}
/*TODO*///	
/*TODO*///		return file;
/*TODO*///	}


	/* common init for all IO_FLOPPY devices */
	public static void	floppy_device_common_init(int id)
	{
		logerror("floppy device common init: id: %02x\n",id);
		/* disk inserted */
		floppy_drive_set_flag_state(id, FLOPPY_DRIVE_DISK_INSERTED, 1);
		/* drive connected */
		floppy_drive_set_flag_state(id, FLOPPY_DRIVE_CONNECTED, 1);
	}

	/* common exit for all IO_FLOPPY devices */
	public static void floppy_device_common_exit(int id)
	{
		logerror("floppy device common exit: id: %02x\n",id);
		/* disk removed */
		floppy_drive_set_flag_state(id, FLOPPY_DRIVE_DISK_INSERTED, 0);
		/* drive disconnected */
		floppy_drive_set_flag_state(id, FLOPPY_DRIVE_CONNECTED, 0);
	}
	
	
/*TODO*///	/*
/*TODO*///	 * Does the system support cassette (for tapecontrol)
/*TODO*///	 * TRUE, FALSE return
/*TODO*///	 */
/*TODO*///	int system_supports_cassette_device (void)
/*TODO*///	{
/*TODO*///		const struct IODevice *dev = Machine->gamedrv->dev;
/*TODO*///	
/*TODO*///		/* Cycle through all devices for this system */
/*TODO*///		while(dev->type != IO_END)
/*TODO*///		{
/*TODO*///			if (dev->type == IO_CASSETTE)
/*TODO*///				return TRUE;
/*TODO*///			dev++;
/*TODO*///		}
/*TODO*///	
/*TODO*///		return FALSE;
/*TODO*///	}

	/*
	 * Return a name for the device type (to be used for UI functions)
	 */
	public static String device_typename(int type)
	{
		if (type < IO_COUNT)
			return devices[type].name;
		return "UNKNOWN";
	}

/*TODO*///	const char *device_brieftypename(int type)
/*TODO*///	{
/*TODO*///		if (type < IO_COUNT)
/*TODO*///			return devices[type].shortname;
/*TODO*///		return "UNKNOWN";
/*TODO*///	}
	
	static String[] typename_id_dev = new String[40];
	static int which_dev = 0;
                
        /* Return a name for a device of type 'type' with id 'id' */
	public static String device_typename_id(int type, int id)
	{
		if (type < IO_COUNT)
		{
                    which_dev = ++which_dev % 40;
                    /* for the average user counting starts at #1 ;-) */
                    typename_id_dev[which_dev] = sprintf("%s #%d", devices[type].name, id + 1);
                    //System.out.println("Returning "+typename_id_dev[which_dev]);
                    return typename_id_dev[which_dev];
		}
		return "UNKNOWN";
	}

		/*
		 * Return the number of filenames for a device of type 'type'.
		 */
		public static int device_count(int type)
		{
			if (type >= IO_COUNT)
				return 0;
			return count[type];
		}

	
		/*
	     * Return the 'id'th filename for a device of type 'type',
	     * NULL if not enough image names of that type are available.
	     */
	    public static String device_filename(int type, int id)
	    {
                //System.out.println("device_filename: "+type+", "+id+", "+images[type][id].name);
	        if (type >= IO_COUNT)
	            return null;
	        if (id < count[type])
	            return images[type][id].name;
	        return null;
	    }
	    
	    /*
         * Return the 'num'th file extension for a device of type 'type',
         * NULL if no file extensions of that type are available.
         */
        public static String device_file_extension(int type, int extnum)
        {
            IODevice[] dev = Machine.gamedrv.dev;
            int cont = 0;
            String ext=null;
            if (type >= IO_COUNT)
                return null;
            while( dev[cont].count != 0)
            {
                if( type == dev[cont].type )
                {
                    ext = dev[cont].file_extensions;
                    /*TODO*/////while( (ext && ext && extnum--) > 0 )
                    /*TODO*/////    ext = ext + (ext.length()) + 1;
                    /*TODO*/////if( ext && !ext )
                    /*TODO*/////    ext = null;
                    return ext;
                }
                /*TODO*/////dev++;
                cont++;
            }
            return null;
        }
	
/*TODO*///	/*
/*TODO*///	 * Return the 'id'th crc for a device of type 'type',
/*TODO*///	 * NULL if not enough image names of that type are available.
/*TODO*///	 */
/*TODO*///	unsigned int device_crc(int type, int id)
/*TODO*///	{
/*TODO*///		if (type >= IO_COUNT)
/*TODO*///			return 0;
/*TODO*///		if (id < count[type])
/*TODO*///			return images[type][id].crc;
/*TODO*///		return 0;
/*TODO*///	}
/*TODO*///	
/*TODO*///	/*
/*TODO*///	 * Set the 'id'th crc for a device of type 'type',
/*TODO*///	 * this is to be used if only a 'partial crc' shall be used.
/*TODO*///	 */
/*TODO*///	void device_set_crc(int type, int id, UINT32 new_crc)
/*TODO*///	{
/*TODO*///		if (type >= IO_COUNT)
/*TODO*///		{
/*TODO*///			logerror("device_set_crc: type out of bounds (%d)\n", type);
/*TODO*///			return;
/*TODO*///		}
/*TODO*///		if (id < count[type])
/*TODO*///		{
/*TODO*///			images[type][id].crc = new_crc;
/*TODO*///			logerror("device_set_crc: new_crc %08x\n", new_crc);
/*TODO*///		}
/*TODO*///		else
/*TODO*///			logerror("device_set_crc: id out of bounds (%d)\n", id);
/*TODO*///	}
/*TODO*///	
/*TODO*///	/*
/*TODO*///	 * Return the 'id'th length for a device of type 'type',
/*TODO*///	 * NULL if not enough image names of that type are available.
/*TODO*///	 */
/*TODO*///	unsigned int device_length(int type, int id)
/*TODO*///	{
/*TODO*///		if (type >= IO_COUNT)
/*TODO*///			return 0;
/*TODO*///		if (id < count[type])
/*TODO*///			return images[type][id].length;
/*TODO*///		return 0;
/*TODO*///	}
	
	/*
	 * Return the 'id'th long name for a device of type 'type',
	 * NULL if not enough image names of that type are available.
	 */
	public static String device_longname(int type, int id)
	{
		if (type >= IO_COUNT)
			return null;
		if (id < count[type])
			return images[type][id].longname;
		return null;
	}
	
	/*
	 * Return the 'id'th manufacturer name for a device of type 'type',
	 * NULL if not enough image names of that type are available.
	 */
	public static String device_manufacturer(int type, int id)
	{
		if (type >= IO_COUNT)
			return null;
		if (id < count[type])
			return images[type][id].manufacturer;
		return null;
	}
	
	/*
	 * Return the 'id'th release year for a device of type 'type',
	 * NULL if not enough image names of that type are available.
	 */
	public static String device_year(int type, int id)
	{
		if (type >= IO_COUNT)
			return null;
		if (id < count[type])
			return images[type][id].year;
		return null;
	}
	
	/*
	 * Return the 'id'th playable info for a device of type 'type',
	 * NULL if not enough image names of that type are available.
	 */
	public static String device_playable(int type, int id)
	{
		if (type >= IO_COUNT)
			return null;
		if (id < count[type])
			return images[type][id].playable;
		return null;
	}
	
	
	/*
	 * Return the 'id'th extrainfo info for a device of type 'type',
	 * NULL if not enough image names of that type are available.
	 */
	public static String device_extrainfo(int type, int id)
	{
		if (type >= IO_COUNT)
			return null;
		if (id < count[type])
			return images[type][id].extrainfo;
		return null;
	}
	
        /*****************************************************************************
         *  --Distribute images to their respective Devices--
         *  Copy the Images specified at the CLI from options.image_files[] to the
         *  array of filenames we keep here, depending on the Device type identifier
         *  of each image.  Multiple instances of the same device are allowed
         *  RETURNS 0 on success, 1 if failed
         ****************************************************************************/
        public static int distribute_images()
        {
            int i,j;
            
            System.out.println("Distributing Images to Devices...");
	
            logerror("Distributing Images to Devices...\n");
		/* Set names to NULL */
		for (i=0;i<IO_COUNT;i++)
			for (j=0;j<MAX_INSTANCES;j++)
				images[i][j].name = null;
	
		for( i = 0; i < options.image_count; i++ )
		{
			int type = options.image_files[i].type;
	
			if (type < IO_COUNT)
			{
				/* Add a filename to the arrays of names */
				if( options.image_files[i].name != null )
				{
					images[type][count[type]].name = dupe(options.image_files[i].name);
					if( images[type][count[type]].name == null )
					{
						mess_printf(" ERROR - dupe() failed\n");
						return 1;
					}
				}
				count[type]++;
			}
			else
			{
				mess_printf(" Invalid Device type %d for %s\n", type, options.image_files[i].name);
				return 1;
			}
		}
	
		/* everything was fine */
		return 0;
    }


	/* Small check to see if system supports device */
	public static int supported_device(IODevice[] dev, int type)
	{
		int dev_ptr = 0;
		while(dev[dev_ptr].type!=IO_END)
		{
			if(dev[dev_ptr].type==type)
				return 1;	/* Return OK */
			dev_ptr++;
		}
		return 0;
	}

	/*****************************************************************************
	 *  --Initialise Devices--
	 *  Call the init() functions for all devices of a driver
	 *  ith all user specified image names.
	 ****************************************************************************/
	public static int init_devices(GameDriver game) {
        //  throw new UnsupportedOperationException("unimplemented");
        GameDriver gamedrv = game;
        IODevice[] dev = gamedrv.dev;
        int dev_ptr = 0;
        int i, id;
        
        logerror("Initialising Devices...\n");
        System.out.println("---------------------- COUNT: "+options.image_count);
	
        /* Check that the driver supports all devices requested (options struct)*/
        for( i = 0; i < options.image_count; i++ )
        {
                if (supported_device(dev, options.image_files[i].type)==0)
                {
                        mess_printf(" ERROR: Device [%s] is not supported by this system\n",device_typename(options.image_files[i].type));
                        return 1;
                }
        }

        /* Ok! All devices are supported.  Now distribute them to the appropriate device..... */
        //if (distribute_images() == 1)
        //        return 1;
        
        
        /* Initialise all floppy drives here if the device is Setting can be overriden by the drivers and UI */
    	floppy_drives_init();

        /* initialize all devices */
        while (dev[dev_ptr].count != 0) {

            /* try and check for valid image and compute 'partial' CRC
		   for imageinfo if possible */
            if (dev[dev_ptr].id != null) {
                for (id = 0; id < dev[dev_ptr].count; id++) {
                    int result;

                    /* initialize */
                    logerror("%s id (%s)\n", device_typename_id(dev[dev_ptr].type, id), device_filename(dev[dev_ptr].type, id) != null ? device_filename(dev[dev_ptr].type, id) : "NULL");
                    result = (dev[dev_ptr].id).handler(id);
                    logerror("%s id returns %d\n", device_typename_id(dev[dev_ptr].type, id), result);

                    if (result != ID_OK && device_filename(dev[dev_ptr].type, id) != null) {
                        printf("%s id failed (%s)\n", device_typename_id(dev[dev_ptr].type, id), device_filename(dev[dev_ptr].type, id));
                        /* HJB: I think we can't abort if a device->id function fails _yet_, because
 * we first would have to clean up every driver to use the correct return values.
 * device->init will fail if a file really can't be loaded.
                         */
 /*					return 1; */
                    }
                    
                    
                }
            } else {
                logerror("%s does not support id!\n", device_typename(dev[dev_ptr].type));
            }

            /* if this device supports initialize (it should!) */
            if (dev[dev_ptr].init != null) {
                /* all instances */
                for (id = 0; id < dev[dev_ptr].count; id++) {
                    int result;
                    
                    /* init succeeded */
    				/* if floppy, perform common init */
    				if ((dev[dev_ptr].type == IO_FLOPPY) & (device_filename(dev[dev_ptr].type, id) != null))
    				{
    					floppy_device_common_init(id);
    				}

                    /* initialize */
                    logerror("%s init (%s)\n", device_typename_id(dev[dev_ptr].type, id), device_filename(dev[dev_ptr].type, id) != null ? device_filename(dev[dev_ptr].type, id) : "NULL");
                    result = (dev[dev_ptr].init).handler(id);
                    logerror("%s init returns %d\n", device_typename_id(dev[dev_ptr].type, id), result);

                    if (result != INIT_OK && device_filename(dev[dev_ptr].type, id) != null) {
                        printf("%s init failed (%s)\n", device_typename_id(dev[dev_ptr].type, id), device_filename(dev[dev_ptr].type, id));
                        return 1;
                    }
                }
            } else {
                logerror("%s does not support init!\n", device_typename(dev[dev_ptr].type));
            }
            dev_ptr++;
        }
        return 0;
    }
    
	/*
	 * Call the exit() functions for all devices of a
	 * driver for all images.
	 */
	public static void exit_devices()
	{
/*TODO*///		const struct IODevice *dev = Machine->gamedrv->dev;
/*TODO*///		int id;
/*TODO*///		int type;
/*TODO*///	
/*TODO*///		/* shutdown all devices */
/*TODO*///		while( dev->count )
/*TODO*///		{
/*TODO*///			/* all instances */
/*TODO*///			if( dev->exit)
/*TODO*///			{
/*TODO*///				/* shutdown */
/*TODO*///				for( id = 0; id < device_count(dev->type); id++ )
/*TODO*///					(*dev->exit)(id);
/*TODO*///	
/*TODO*///			}
/*TODO*///			else
/*TODO*///			{
/*TODO*///				logerror("%s does not support exit!\n", device_typename(dev->type));
/*TODO*///			}
/*TODO*///	
/*TODO*///			/* The following is always executed for a IO_FLOPPY exit */
/*TODO*///			/* KT: if a image is removed:
/*TODO*///				1. Disconnect drive
/*TODO*///				2. Remove disk from drive */
/*TODO*///			/* This is done here, so if a device doesn't support exit, the status
/*TODO*///			will still be correct */
/*TODO*///			if (dev->type == IO_FLOPPY)
/*TODO*///			{
/*TODO*///				for (id = 0; id< device_count(dev->type); id++)
/*TODO*///				{
/*TODO*///					floppy_device_common_exit(id);
/*TODO*///				}
/*TODO*///			}
/*TODO*///	
/*TODO*///			dev++;
/*TODO*///		}
/*TODO*///	
/*TODO*///		for( type = 0; type < IO_COUNT; type++ )
/*TODO*///		{
/*TODO*///			if( images[type] )
/*TODO*///			{
/*TODO*///				for( id = 0; id < device_count(dev->type); id++ )
/*TODO*///				{
/*TODO*///					if( images[type][id].name )
/*TODO*///						free(images[type][id].name);
/*TODO*///					if( images[type][id].longname )
/*TODO*///						free(images[type][id].longname);
/*TODO*///					if( images[type][id].manufacturer )
/*TODO*///						free(images[type][id].manufacturer);
/*TODO*///					if( images[type][id].year )
/*TODO*///						free(images[type][id].year);
/*TODO*///					if( images[type][id].playable )
/*TODO*///						free(images[type][id].playable);
/*TODO*///					if( images[type][id].extrainfo )
/*TODO*///						free(images[type][id].extrainfo);
/*TODO*///				}
/*TODO*///			}
/*TODO*///			count[type] = 0;
/*TODO*///		}
/*TODO*///	
/*TODO*///		/* KT: clean up */
/*TODO*///		floppy_drives_exit();
/*TODO*///	
/*TODO*///	#ifdef MAME_DEBUG
/*TODO*///		for( type = 0; type < IO_COUNT; type++ )
/*TODO*///		{
/*TODO*///			if (count[type])
/*TODO*///				mess_printf("OOPS!!!  Appears not all images free!\n");
/*TODO*///	
/*TODO*///		}
/*TODO*///	#endif
	
	}
	
	/*
	 * Change the associated image filename for a device.
	 * Returns 0 if successful.
	 */
	public static int device_filename_change(int type, int id, String name)
	{
                //System.out.println("device_filename_change");
                //System.out.println("Type: "+type);
                //System.out.println("Id: "+id);
                //System.out.println("Name: "+name);
                
		IODevice[] dev = Machine.gamedrv.dev;
                int _dev = 0;
		//image_info img = images[type][id];
	
		if( type >= IO_COUNT )
			return 1;
	
		while( dev[_dev].count !=0 && dev[_dev].type != type )
			_dev++;
                
                //System.out.println("Dev: "+_dev);
                //System.out.println("Dev Count: "+dev[_dev].count);
	
		if( id >= dev[_dev].count )
			return 1;
	
		if( dev[_dev].exit != null )
			dev[_dev].exit.handler(id);
	
		/* if floppy, perform common exit */
		if (dev[_dev].type == IO_FLOPPY)
		{
			floppy_device_common_exit(id);
		}
                
                //System.out.println("Dev Init: "+dev[_dev].init);
	
		if( dev[_dev].init != null )
		{
			int result;
			/*
			 * set the new filename and reset all addition info, it will
			 * be inserted by osd_fopen() and the crc handling
			 */
			images[type][id].name = null;
			images[type][id].length = 0;
			images[type][id].crc = 0;
			if( name != null )
			{
				images[type][id].name = dupe(name);
                                //System.out.println("NAME: "+images[type][id].name);
				/* Check the name */
				if( images[type][id].name == null )
					return 1;
				/* check the count - if it was 0, add new! */
				if (device_count(type)==0)
					count[type]++;
			}
                        
                        //System.out.println("Reset Depth: "+dev[_dev].reset_depth);
	
			if( dev[_dev].reset_depth == IO_RESET_CPU )
				machine_reset();
			else
			if( dev[_dev].reset_depth == IO_RESET_ALL )
			{
				mess_keep_going = 1;
	
			}
	
			result = dev[_dev].init.handler(id);
			if( result != 0)
				return 1;
	
			/* init succeeded */
			/* if floppy, perform common init */
			if (dev[_dev].type == IO_FLOPPY)
			{
				floppy_device_common_init(id);
			}
	
		}
		return 0;
	}
	
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	#ifdef MAME_DEBUG
/*TODO*///	int messvaliditychecks(void)
/*TODO*///	{
/*TODO*///		int i;
/*TODO*///		int error = 0;
/*TODO*///	
/*TODO*///		/* Check the device struct array */
/*TODO*///		i=0;
/*TODO*///		while (devices[i].id != IO_COUNT)
/*TODO*///		{
/*TODO*///			if (devices[i].id != i)
/*TODO*///			{
/*TODO*///				mess_printf("MESS Validity Error - Device struct array order mismatch\n");
/*TODO*///				error = 1;
/*TODO*///			}
/*TODO*///			i++;
/*TODO*///		}
/*TODO*///		if (i < IO_COUNT)
/*TODO*///		{
/*TODO*///			mess_printf("MESS Validity Error - Device struct entry missing\n");
/*TODO*///			error = 1;
/*TODO*///		}
/*TODO*///		return error;
/*TODO*///	}
/*TODO*///	#endif
/*TODO*///	
/*TODO*///
        public static int displayimageinfo(mame_bitmap bitmap, int selected)
        {
                        //char buf[2048], *dst = buf;
        String dst = "";
        int type, id, sel = selected - 1;

        dst += sprintf("%s\n\n", Machine.gamedrv.description);

        for (type = 0; type < IO_COUNT; type++) {
            for (id = 0; id < device_count(type); id++) {
                String name = device_filename(type, id);
                if (name != null) {
                    String info;
                    dst += sprintf("%s: %s\n", device_typename_id(type, id), device_filename(type, id));
                    info = device_longname(type, id);
                    if (info != null) {
                        dst += sprintf("%s\n", info);
                    }
                    info = device_manufacturer(type, id);
                    if (info != null) {
                        dst += sprintf("%s", info);
                        info = /*stripspace*/ (device_year(type, id));
                        if ((info != null) && (info.length() != 0)) {
                            dst += sprintf(", %s", info);
                        }
                        dst += sprintf("\n");
                    }
                    info = device_playable(type, id);
                    if (info != null) {
                        dst += sprintf("%s\n", info);
                    }
// why is extrainfo printed? only MSX and NES use it that i know of ... Cowering
				info = device_extrainfo(type,id);
				if( info != null )
					dst += sprintf(dst,"%s\n", info);
                } else {
                    dst += sprintf("%s: ---\n", device_typename_id(type, id));
                }
            }
        }

        if (sel == -1) {
            /* startup info, print MAME version and ask for any key */

            dst += "\n\tPress any key to Begin";
            ui_drawbox(bitmap, 0, 0, Machine.uiwidth, Machine.uiheight);
            ui_displaymessagewindow(bitmap, dst);

            sel = 0;
            if (code_read_async() != CODE_NONE
                    || code_read_async() != CODE_NONE) {
                sel = -1;
            }
        } else {
            /* menu system, use the normal menu keys */
            dst += "\n\t\u001A Return to Main Menu \u001B";

            ui_displaymessagewindow(bitmap, dst);

            if ((input_ui_pressed(IPT_UI_SELECT)) != 0) {
                sel = -1;
            }

            if ((input_ui_pressed(IPT_UI_CANCEL)) != 0) {
                sel = -1;
            }

            if ((input_ui_pressed(IPT_UI_CONFIGURE)) != 0) {
                sel = -2;
            }
        }

        if (sel == -1 || sel == -2) {
            /* tell updatescreen() to clean after us */
            schedule_full_refresh();
        }

        return sel + 1;
    }
/*TODO*///	public static int displayimageinfo(struct mame_bitmap *bitmap, int selected)
/*TODO*///	{
/*TODO*///		char buf[2048], *dst = buf;
/*TODO*///		int type, id, sel = selected - 1;
/*TODO*///	
/*TODO*///		dst += sprintf(dst,"%s\n\n",Machine->gamedrv->description);
/*TODO*///	
/*TODO*///		for (type = 0; type < IO_COUNT; type++)
/*TODO*///		{
/*TODO*///			for( id = 0; id < device_count(type); id++ )
/*TODO*///			{
/*TODO*///				const char *name = device_filename(type,id);
/*TODO*///				if( name )
/*TODO*///				{
/*TODO*///					const char *info;
/*TODO*///					char *filename;
/*TODO*///	
/*TODO*///					filename = (char *) device_filename(type, id);
/*TODO*///	
/*TODO*///					dst += sprintf(dst,"%s: %s\n", device_typename_id(type,id), osd_basename(filename));
/*TODO*///					info = device_longname(type,id);
/*TODO*///					if( info )
/*TODO*///						dst += sprintf(dst,"%s\n", info);
/*TODO*///					info = device_manufacturer(type,id);
/*TODO*///					if( info )
/*TODO*///					{
/*TODO*///						dst += sprintf(dst,"%s", info);
/*TODO*///						info = stripspace(device_year(type,id));
/*TODO*///						if( info && strlen(info))
/*TODO*///							dst += sprintf(dst,", %s", info);
/*TODO*///						dst += sprintf(dst,"\n");
/*TODO*///					}
/*TODO*///					info = device_playable(type,id);
/*TODO*///					if( info )
/*TODO*///						dst += sprintf(dst,"%s\n", info);
/*TODO*///	// why is extrainfo printed? only MSX and NES use it that i know of ... Cowering
/*TODO*///	//				info = device_extrainfo(type,id);
/*TODO*///	//				if( info )
/*TODO*///	//					dst += sprintf(dst,"%s\n", info);
/*TODO*///				}
/*TODO*///				else
/*TODO*///				{
/*TODO*///					dst += sprintf(dst,"%s: ---\n", device_typename_id(type,id));
/*TODO*///				}
/*TODO*///			}
/*TODO*///		}
/*TODO*///	
/*TODO*///		if (sel == -1)
/*TODO*///		{
/*TODO*///			/* startup info, print MAME version and ask for any key */
/*TODO*///	
/*TODO*///			strcat(buf,"\n\tPress any key to Begin");
/*TODO*///			ui_drawbox(bitmap,0,0,Machine->uiwidth,Machine->uiheight);
/*TODO*///			ui_displaymessagewindow(bitmap, buf);
/*TODO*///	
/*TODO*///			sel = 0;
/*TODO*///			if (code_read_async() != KEYCODE_NONE ||
/*TODO*///				code_read_async() != JOYCODE_NONE)
/*TODO*///				sel = -1;
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			/* menu system, use the normal menu keys */
/*TODO*///			strcat(buf,"\n\t\x1a Return to Main Menu \x1b");
/*TODO*///	
/*TODO*///			ui_displaymessagewindow(bitmap,buf);
/*TODO*///	
/*TODO*///			if (input_ui_pressed(IPT_UI_SELECT))
/*TODO*///				sel = -1;
/*TODO*///	
/*TODO*///			if (input_ui_pressed(IPT_UI_CANCEL))
/*TODO*///				sel = -1;
/*TODO*///	
/*TODO*///			if (input_ui_pressed(IPT_UI_CONFIGURE))
/*TODO*///				sel = -2;
/*TODO*///		}
/*TODO*///	
/*TODO*///		if (sel == -1 || sel == -2)
/*TODO*///		{
/*TODO*///			/* tell updatescreen() to clean after us */
/*TODO*///			schedule_full_refresh();
/*TODO*///		}
/*TODO*///	
/*TODO*///		return sel + 1;
/*TODO*///	}
/*TODO*///	
	
	public static void showmessdisclaimer()
	{
		mess_printf(
			"MESS is an emulator: it reproduces, more or less faithfully, the behaviour of\n"+
			"several computer and console systems. But hardware is useless without software\n"+
			"so a file dump of the BIOS, cartridges, discs, and cassettes which run on that\n"+
			"hardware is required. Such files, like any other commercial software, are\n"+
			"copyrighted material and it is therefore illegal to use them if you don't own\n"+
			"the original media from which the files are derived. Needless to say, these\n"+
			"files are not distributed together with MESS. Distribution of MESS together\n"+
			"with these files is a violation of copyright law and should be promptly\n"+
			"reported to the authors so that appropriate legal action can be taken.\n\n");
	}
	
	public static void showmessinfo()
	{
		mess_printf(
			"M.E.S.S. v%s\n"+
			"Multiple Emulation Super System - Copyright (C) 1997-2001 by the MESS Team\n"+
			"M.E.S.S. is based on the ever excellent M.A.M.E. Source code\n"+
			"Copyright (C) 1997-2001 by Nicola Salmoria and the MAME Team\n\n",
			build_version);
		showmessdisclaimer();
		mess_printf(
			"Usage:  MESS <system> <device> <software> <options>\n\n"+
			"        MESS -list        for a brief list of supported systems\n"+
			"        MESS -listdevices for a full list of supported devices\n"+
			"        MESS -showusage   to see usage instructions\n"+
			"See mess.txt for help, readme.txt for options.\n");
	
	}
        
    /* load battery backed nvram from a driver subdir. in the nvram dir. */
    public static int battery_load( String filename, UBytePtr buffer, int length )
    {
            Object f;
            int bytes_read = 0;
            int result = 0;

            /* some sanity checking */
            if( buffer != null && length > 0 )
            {
                    f = osd_fopen(Machine.gamedrv.name, filename, OSD_FILETYPE_NVRAM, 0);
                    if (f != null)
                    {
                            bytes_read = osd_fread(f, buffer, length);
                            osd_fclose(f);
                            result = 1;
                    }

                    /* fill remaining bytes (if necessary) */
                    memset(new UBytePtr(buffer, bytes_read), '\0', length - bytes_read);
            }
            return result;
    }

    /* save battery backed nvram to a driver subdir. in the nvram dir. */
    public static int battery_save( String filename, UBytePtr buffer, int length )
    {
            Object f;

            /* some sanity checking */
            if( buffer != null && length > 0 )
            {
                    f = osd_fopen(Machine.gamedrv.name, filename, OSD_FILETYPE_NVRAM, 1);
                    if (f != null)
                    {
                            osd_fwrite(f, buffer, length);
                            osd_fclose(f);
                            return 1;
                    }
            }
            return 0;
    }
	/*
    * Copy the image names from options.image_files[] to
    * the array of filenames we keep here, depending on the
    * type identifier of each image.
     */
    public static int get_filenames() {
         IODevice[] dev = Machine.gamedrv.dev;
        int dev_ptr = 0;
        int i;

        for (i = 0; i < options.image_count; i++) {
            int type = options.image_files[i].type;

            if (type < IO_COUNT) {
                /*TODO*///			/* Add a filename to the arrays of names */
/*TODO*///			if( images[type] )
/*TODO*///				images[type] = realloc(images[type],(count[type]+1)*sizeof(struct image_info));
/*TODO*///			else
/*TODO*///				images[type] = malloc(sizeof(struct image_info));
/*TODO*///			if( !images[type] )
/*TODO*///				return 1;
/*TODO*///			memset(&images[type][count[type]], 0, sizeof(struct image_info));
/*TODO*///			if( options.image_files[i].name )
/*TODO*///			{
/*TODO*///				images[type][count[type]].name = dupe(options.image_files[i].name);
/*TODO*///				if( !images[type][count[type]].name )
/*TODO*///					return 1;
/*TODO*///			}
/*TODO*///			logerror("%s #%d: %s\n", typename[type], count[type]+1, images[type][count[type]].name);
/*TODO*///			count[type]++;
//TODO below code needs to be tested
                images[type][count[type]] = new image_info();
                if (options.image_files[i].name != null) {
                    images[type][count[type]].name = options.image_files[i].name;
                    if (images[type][count[type]].name == null) {
                        return 1;
                    }
                }
                //logerror("%s #%d: %s\n", typename[type], count[type] + 1, images[type][count[type]].name);
                count[type]++;
            } else {
                logerror("Invalid IO_ type %d for %s\n", type, options.image_files[i].name);
                return 1;
            }
        }

        /* Does the driver have any IODevices defined? */
        if (dev != null) {
            while (dev[dev_ptr].count != 0) {
                int type = dev[dev_ptr].type;
                /*TODO*///                while (count[type] < dev[dev_ptr].count) {
/*TODO*///                    throw new UnsupportedOperationException("unimplemented");
                /*TODO*///				/* Add an empty slot name the arrays of names */
/*TODO*///				if( images[type] )
/*TODO*///					images[type] = realloc(images[type],(count[type]+1)*sizeof(struct image_info));
/*TODO*///				else
/*TODO*///					images[type] = malloc(sizeof(struct image_info));
/*TODO*///				if( !images[type] )
/*TODO*///					return 1;
/*TODO*///				memset(&images[type][count[type]], 0, sizeof(struct image_info));
/*TODO*///				count[type]++;
/*TODO*///                }
                dev_ptr++;
            }
        }

        /* everything was fine */
        return 0;
    }	
}
