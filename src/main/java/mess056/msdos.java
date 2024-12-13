/** *
 * Ported to mess 0.37b6
 */
package mess056;


import mame056.driverH.GameDriver;
import static mess056.deviceH.*;
import static mess056.mess.device_typename;
import static mess056.messH.*;
import static mess056.system.drivers;
import static arcadeflex056.osdepend.*;
import static mame056.mame.options;
import mame056.mameH.ImageFile;
import static mame056.mameH.MAX_IMAGES;
import static common.libc.cstdio.*;
import static common.libc.cstring.*;

public class msdos {

    /*TODO*///extern struct GameOptions options;
/*TODO*///
/*TODO*////* fronthlp functions */
/*TODO*///extern int strwildcmp(const char *sp1, const char *sp2);
/*TODO*///

/*
 * Detect the type of image given in 'arg':
 * 1st: user specified type (after -rom, -floppy ect.)
 * 2nd: match extensions specified by the driver
 * default: add image to the list of names for IO_CARTSLOT
 */
public static int detect_image_type(int game_index, int type, String arg)
{
	GameDriver drv = drivers[game_index];
	String ext;

	if (options.image_count >= MAX_IMAGES)
	{
		printf("Too many image names specified!\n");
		return 1;
	}

	if (type!=0)
	{
		logerror("User specified %s for %s\n", device_typename(type), arg);
		/* the user specified a device type */
		options.image_files[options.image_count].type = type;
		options.image_files[options.image_count].name = arg;
		options.image_count++;
		return 0;
	}
        
        throw new UnsupportedOperationException("unimplemented");
/*TODO*///	/* Look up the filename extension in the drivers device list */
/*TODO*///	ext = strrchr(arg, '.');
/*TODO*///	if (ext)
/*TODO*///	{
/*TODO*///		const struct IODevice *dev = drv->dev;
/*TODO*///
/*TODO*///		ext++;
/*TODO*///		while (dev->type != IO_END)
/*TODO*///		{
/*TODO*///			const char *dst = dev->file_extensions;
/*TODO*///
/*TODO*///			/* scan supported extensions for this device */
/*TODO*///			while (dst && *dst)
/*TODO*///			{
/*TODO*///				if (stricmp(dst, ext) == 0)
/*TODO*///				{
/*TODO*///					logerror("Extension match %s [%s] for %s\n", device_typename(dev->type), dst, arg);
/*TODO*///					options.image_files[options.image_count].type = dev->type;
/*TODO*///					options.image_files[options.image_count].name = strdup(arg);
/*TODO*///					options.image_count++;
/*TODO*///					return 0;
/*TODO*///				}
/*TODO*///				/* skip '\0' once in the list of extensions */
/*TODO*///				dst += strlen(dst) + 1;
/*TODO*///			}
/*TODO*///			dev++;
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	type = IO_CARTSLOT;
/*TODO*///	logerror("Default %s for %s\n", device_typename(type), arg);
/*TODO*///	/* Every unrecognized image type is added here */
/*TODO*///	options.image_files[options.image_count].type = type;
/*TODO*///	options.image_files[options.image_count].name = strdup(arg);
/*TODO*///	options.image_count++;
/*TODO*///	return 0;
}



    /* Small check to see if system supports device */
    public static int system_supports_device(int game_index, int type) {
        int dev_ptr = 0;
        IODevice[] dev = drivers[game_index].dev;

        while (dev[dev_ptr].type != IO_END) {
            if (dev[dev_ptr].type == type) {
                return 1;
            }
            dev_ptr++;
        }
        return 0;
    }

    /*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////* Function to handle aliases in the MESS.CFG file */
/*TODO*///char *get_alias(const char *driver_name, char *alias)
/*TODO*///{
/*TODO*///        char *alias_copy;
/*TODO*///		// the string will be worked on, so duplicate it.
/*TODO*///        alias_copy =strdup(get_config_string((char*)driver_name,alias,NULL));
/*TODO*///        return alias_copy;
/*TODO*///
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////*
/*TODO*/// * Parse the alias command line
/*TODO*/// */
/*TODO*///void parse_alias(char *src, int *argc, char **argv)
/*TODO*///{
/*TODO*///    int argnum = 0;
/*TODO*///	char *arg;
/*TODO*///
/*TODO*///	while (*src)
/*TODO*///	{
/*TODO*///        while( *src && isspace(*src) )
/*TODO*///            src++;
/*TODO*///
/*TODO*///        arg = src;
/*TODO*///
/*TODO*///        if(*src =='"')
/*TODO*///		{
/*TODO*///			if(*src++==NULL)
/*TODO*///				break;
/*TODO*///			else
/*TODO*///				arg=src;
/*TODO*///
/*TODO*///			while(*src)
/*TODO*///            {
/*TODO*///				if (*src=='"')
/*TODO*///					*src='\0';
/*TODO*///				if (*src++ == NULL)
/*TODO*///					break;
/*TODO*///			}
/*TODO*///		}
/*TODO*///
/*TODO*///		else
/*TODO*///		{
/*TODO*///			while(*src)
/*TODO*///            {
/*TODO*///				if (*src==' ')
/*TODO*///					*src='\0';
/*TODO*///				if (*src++ == NULL)
/*TODO*///					break;
/*TODO*///			}
/*TODO*///		}
/*TODO*///		argv[argnum++]=arg;
/*TODO*///	}
/*TODO*///	*argc=argnum;
/*TODO*///
/*TODO*///}
/*TODO*///
    public static int requested_device_type(String tchar) {

        logerror("Requested device is %s\n", tchar);

        if (stricmp(tchar, "-cartridge") == 0 || stricmp(tchar, "-cart") == 0) {
            return (IO_CARTSLOT);
        } else if (stricmp(tchar, "-floppydisk") == 0 || stricmp(tchar, "-flop") == 0) {
            return (IO_FLOPPY);
        } else if (stricmp(tchar, "-harddisk") == 0 || stricmp(tchar, "-hard") == 0) {
            return (IO_HARDDISK);
        } else if (stricmp(tchar, "-cassette") == 0 || stricmp(tchar, "-cass") == 0) {
            return (IO_CASSETTE);
        } else if (stricmp(tchar, "-printer") == 0 || stricmp(tchar, "-prin") == 0) {
            return (IO_PRINTER);
        } else if (stricmp(tchar, "-serial") == 0 || stricmp(tchar, "-serl") == 0) {
            return (IO_SERIAL);
        } else if (stricmp(tchar, "-snapshot") == 0 || stricmp(tchar, "-dump") == 0) {
            return (IO_SNAPSHOT);
        } else if (stricmp(tchar, "-quickload") == 0 || stricmp(tchar, "-quik") == 0) {
            return (IO_QUICKLOAD);
        //} else if (stricmp(tchar, "-alias") == 0) {
        //    return (IO_ALIAS);
        } /* all other switches set type to -1 */ else {
            logerror("Requested Device not supported!!\n");
            return -1;
        }
    }

    public static int load_image(int argc, String[] argv, int j, int game_index) {
        String driver = drivers[game_index].name;
        for (int im = 0; im < MAX_IMAGES; im++) { //init image files (consoleflex stuff)
            options.image_files[im] = new ImageFile();
        }
        int i, k;
        int res = 0;
        int type = IO_END;

        /*
	 * Take all following commandline arguments without "-" as
	 * image names or as an alias name, which is replaced by a list
	 * of images.
         */
        for (i = j + 1; i < argc; i++) {
            /* skip options and their additional arguments */
 /* this should really look up the structure values for easy maintenance */
            if (argv[i].startsWith("-"))//(argv[i][0] == '-')
            {

                type = requested_device_type(argv[i]);

                //if (type != IO_ALIAS) {

                    if (type > IO_END && system_supports_device(game_index, type) == 0) {
                        logerror("Specified Device (%s) not supported by this system\n", argv[i]);
                        type = -1;
                        /* strip device if systems doesnt support it */
                    }
                //}

            } else if (type != -1) /* only enter when valid option, otherwise get next */ {
               // if (type == IO_ALIAS) {
               //     throw new UnsupportedOperationException("unimplemented");
                    /*TODO*///				/* check if this is an alias for a set of images */
/*TODO*///				char *alias = get_alias(driver, argv[i]);
/*TODO*///
/*TODO*///				if (alias && strlen(alias))
/*TODO*///				{
/*TODO*///					int alias_argc;
/*TODO*///					char *alias_argv[32];  /* more than 32 arguments per alias?? */
/*TODO*///
/*TODO*///    				logerror("Using alias %s (%s) for driver %s\n", argv[i], alias, driver);
/*TODO*///					parse_alias(alias, &alias_argc, alias_argv);
/*TODO*///					type = IO_END;
/*TODO*///
/*TODO*///					for(k=0;k<alias_argc;k++)
/*TODO*///					{
/*TODO*///						if (alias_argv[k][0] == '-')
/*TODO*///						{
/*TODO*///							type = requested_device_type(alias_argv[k]);
/*TODO*///							if (type>IO_END && !system_supports_device(game_index, type))
/*TODO*///							{
/*TODO*///								logerror("Specified Device (%s) not supported by this system\n", argv[i]);
/*TODO*///								type = -1; /* strip device if systems doesnt support it */
/*TODO*///							}
/*TODO*///						}
/*TODO*///						else if (type!=-1 ) /* only when valid option */
/*TODO*///						{
/*TODO*///							if(type!=IO_END)
/*TODO*///							{
/*TODO*///                                res = detect_image_type(game_index, type, alias_argv[k]);
/*TODO*///                                type = IO_END; /* image detected, reset type */
/*TODO*///							}
/*TODO*///						}
/*TODO*///
/*TODO*///					}
/*TODO*///
/*TODO*///
/*TODO*///				}
/*TODO*///
             //   } /* use normal command line argument! */ else 
             if (type != IO_END) {
                    logerror("Loading image - No alias used\n");
                    res = detect_image_type(game_index, type, argv[i]);
                    type = IO_END;
                    /* image detected, reset type */
                }
            }
            /* If we had an error bail out now */
            if (res != 0) {
                return res;
            }

        }
        return res;
    }
    /*TODO*///
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////* This function contains all the -list calls from fronthlp.c for MESS */
/*TODO*////* Currently Supported: */
/*TODO*////*   -listdevices       */
/*TODO*////*   -listtext       	*/
/*TODO*///
/*TODO*///void list_mess_info(char *gamename, char *arg, int listclones)
/*TODO*///{
/*TODO*///
/*TODO*///	int i, j;
/*TODO*///
/*TODO*///	/* -listdevices */
/*TODO*///	if (!stricmp(arg, "-listdevices"))
/*TODO*///	{
/*TODO*///
/*TODO*///		i = 0;
/*TODO*///		j = 0;
/*TODO*///
/*TODO*///
/*TODO*///		printf(" SYSTEM      DEVICE NAME (brief)   IMAGE FILE EXTENSIONS SUPPORTED    \n");
/*TODO*///		printf("----------  --------------------  ------------------------------------\n");
/*TODO*///
/*TODO*///		while (drivers[i])
/*TODO*///		{
/*TODO*///			const struct IODevice *dev = drivers[i]->dev;
/*TODO*///
/*TODO*///			if (!strwildcmp(gamename, drivers[i]->name))
/*TODO*///			{
/*TODO*///				int devcount = 1;
/*TODO*///
/*TODO*///				printf("%-13s", drivers[i]->name);
/*TODO*///
/*TODO*///				/* if IODevice not used, print UNKNOWN */
/*TODO*///				if (dev->type == IO_END)
/*TODO*///					printf("%-12s\n", "UNKNOWN");
/*TODO*///
/*TODO*///				/* else cycle through Devices */
/*TODO*///				while (dev->type != IO_END)
/*TODO*///				{
/*TODO*///					const char *src = dev->file_extensions;
/*TODO*///
/*TODO*///					if (devcount == 1)
/*TODO*///						printf("%-12s(%s)   ", device_typename(dev->type), briefdevice_typename(dev->type));
/*TODO*///					else
/*TODO*///						printf("%-13s%-12s(%s)   ", "    ", device_typename(dev->type), briefdevice_typename(dev->type));
/*TODO*///
/*TODO*///					devcount++;
/*TODO*///
/*TODO*///					while (src && *src)
/*TODO*///					{
/*TODO*///
/*TODO*///						printf(".%-5s", src);
/*TODO*///						src += strlen(src) + 1;
/*TODO*///					}
/*TODO*///					dev++;			   /* next IODevice struct */
/*TODO*///					printf("\n");
/*TODO*///				}
/*TODO*///
/*TODO*///
/*TODO*///			}
/*TODO*///			i++;
/*TODO*///
/*TODO*///		}
/*TODO*///
/*TODO*///	}
/*TODO*///
/*TODO*///	/* -listtext */
/*TODO*///	else if (!stricmp(arg, "-listtext"))
/*TODO*///	{
/*TODO*///		printf("                   ==========================================\n" );
/*TODO*///		printf("                    M.E.S.S.  -  Multi-Emulator Super System\n"  );
/*TODO*///		printf("                             Copyright (C) 1998-2000\n");
/*TODO*///		printf("                                by the MESS team\n"    );
/*TODO*///		printf("                    Official Page at: http://mess.emuverse.com\n");
/*TODO*///		printf("                   ==========================================\n\n" );
/*TODO*///
/*TODO*///		printf("This document is generated for MESS %s\n\n",build_version);
/*TODO*///
/*TODO*///		printf("Please note that many people helped with this project, either directly or by\n"
/*TODO*///		       "releasing source code which was used to write the drivers. We are not trying to\n"
/*TODO*///		       "appropriate merit which isn't ours. See the acknowledgemnts section for a list\n"
/*TODO*///			   "of contributors, however please note that the list is largely incomplete. See\n"
/*TODO*///			   "also the CREDITS section in the emulator to see the people who contributed to a\n"
/*TODO*///			   "specific driver. Again, that list might be incomplete. We apologize in advance\n"
/*TODO*///			   "for any omission.\n\n"
/*TODO*///
/*TODO*///			   "All trademarks cited in this document are property of their respective owners.\n"
/*TODO*///
/*TODO*///			   "Especially, the MESS team would like to thank Nicola Salmoria and the MAME team\n"
/*TODO*///			   "for letting us play with their code and, in fact, incorporating MESS specific\n"
/*TODO*///			   "code into MAME.  Without it, MESS would be substantially less than what it is\n"
/*TODO*///			   "right now! ;-)\n\n"
/*TODO*///
/*TODO*///			   "Usage and Distribution Licence:\n"
/*TODO*///			   "===============================\n"
/*TODO*///			   "- MESS usage and distribution follows that of MAME.  Please read the MAME\n"
/*TODO*///			   "  readme.txt file distributed with MESS for further information.\n\n"
/*TODO*///
/*TODO*///			   "How to Contact The MESS Team\n"
/*TODO*///			   "============================\n"
/*TODO*///			   "Visit the web page at http://mess.emuverse.com to see a list of contributers\n"
/*TODO*///			   "If you have comments, suggestions or bug reports about an existing driver, check\n"
/*TODO*///			   "the page contacts section to find who has worked on it, and send comments to that \n"
/*TODO*///			   "person. If you are not sure who to contact, write to Ben (ben@mame.net) - who is the \n"
/*TODO*///			   "current coordinator of the MESS project [DOS]. \n\n"
/*TODO*///
/*TODO*///			   "PLEASE DON'T SEND BINARY ATTACHMENTS WITHOUT ASKING FIRST! \n"
/*TODO*///
/*TODO*///			   "THESE ARE NOT SUPPORT ADDRESSES. Support questions sent to these addresses\n"
/*TODO*///			   "*will* be ignored. Please understand that this is a *free* project, mostly\n"
/*TODO*///			   "targeted at experienced users. We don't have the resources to provide end user\n"
/*TODO*///			   "support. Basically, if you can't get the emulator to work, you are on your own.\n"
/*TODO*///			   "First of all, read this doc carefully. If you still can't find an answer to\n"
/*TODO*///			   "your question, try checking the beginner's sections that many emulation pages\n"
/*TODO*///			   "have, or ask on the appropriate Usenet newsgroups (e.g. comp.emulators.misc)\n"
/*TODO*///			   "or on the many emulation message boards.  The official MESS message board is at:\n"
/*TODO*///			   "   http://mess.emuverse.com\n\n");
/*TODO*///
/*TODO*///
/*TODO*///		printf("Also, please DO NOT SEND REQUESTS FOR NEW SYSTEMS TO ADD, unless you have some original\n");
/*TODO*///		printf("info on the hardware or, even better, have the technical expertise needed to\n");
/*TODO*///		printf("help us. Please don't send us information widely available on the Internet -\n");
/*TODO*///		printf("we are perfectly capable of finding it ourselves, thank you.\n\n\n");
/*TODO*///
/*TODO*///
/*TODO*///		printf("Complete Emulated System List\n");
/*TODO*///		printf("=============================\n");
/*TODO*///		printf("Here is the list of systems supported by MESS %s\n",build_version);
/*TODO*///		if (!listclones)
/*TODO*///			printf("Variants of the same system are not included, you can use the -listclones command\n"
/*TODO*///				"to get a list of the alternate versions of a given system.\n");
/*TODO*///		printf("\n"
/*TODO*///			   "The meanings of the columns are as follows:\n"
/*TODO*///			   "Working - \"No\" means that the emulation has shortcomings that cause the system\n"
/*TODO*///			   "  not to work correctly. This can be anywhere from just showing a black screen\n"
/*TODO*///			   "  to not being playable with major problems.\n"
/*TODO*///			   "Correct Colors - \"Yes\" means that colors should be identical to the original,\n"
/*TODO*///			   "  \"Close\" that they are very similar but wrong in places, \"No\" that they are\n"
/*TODO*///			   "  completely wrong. \n"
/*TODO*///			   "Sound - \"Partial\" means that sound support is either incomplete or not entirely\n"
/*TODO*///			   "  accurate. \n"
/*TODO*///			   "Internal Name - This is the unique name that should be specified on the command\n"
/*TODO*///			   "  line to run the system. ROMs must be placed in the ROM path, either in a .zip\n"
/*TODO*///			   "  file or in a subdirectory of the same name. The former is suggested, because\n"
/*TODO*///			   "  the files will be identified by their CRC instead of requiring specific\n"
/*TODO*///			   "  names.  NOTE! that as well as required ROM files to emulate the system, you may\n"
/*TODO*///			   "  also attach IMAGES of files created for system specific devices (some examples of \n"
/*TODO*///			   "  devices are cartridges, floppydisks, harddisks, etc).  See below for a complete list\n"
/*TODO*///			   "  of a systems supported devices and common file formats used for that device\n\n");
/*TODO*///
/*TODO*///		printf("System Information can be obtained from the SysInfo.dat file (online in the MESS UI\n"
/*TODO*///			   "from the Machine history) or sysinfo.htm.  To generate sysinfo.htm, execute \n"
/*TODO*///			   "dat2html.exe.\n\n\n");
/*TODO*///
/*TODO*///		printf("+-----------------------------------------+-------+-------+-------+----------+\n");
/*TODO*///		printf("|                                         |       |Correct|       | Internal |\n");
/*TODO*///		printf("| System Name                             |Working|Colors | Sound |   Name   |\n");
/*TODO*///		printf("+-----------------------------------------+-------+-------+-------+----------+\n");
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*///			/* Generate the System List */
/*TODO*///
/*TODO*///			 i = 0;
/*TODO*///			while (drivers[i])
/*TODO*///			{
/*TODO*///
/*TODO*///				if ((listclones || drivers[i]->clone_of == 0
/*TODO*///						|| (drivers[i]->clone_of->flags & NOT_A_DRIVER)
/*TODO*///						) && !strwildcmp(gamename, drivers[i]->name))
/*TODO*///				{
/*TODO*///					char name[200],name_ref[200];
/*TODO*///
/*TODO*///					strcpy(name,drivers[i]->description);
/*TODO*///
/*TODO*///					/* Move leading "The" to the end */
/*TODO*///					if (strstr(name," (")) *strstr(name," (") = 0;
/*TODO*///					if (strncmp(name,"The ",4) == 0)
/*TODO*///					{
/*TODO*///						sprintf(name_ref,"%s, The ",name+4);
/*TODO*///					}
/*TODO*///					else
/*TODO*///						sprintf(name_ref,"%s ",name);
/*TODO*///
/*TODO*///					/* print the additional description only if we are listing clones */
/*TODO*///					if (listclones)
/*TODO*///					{
/*TODO*///						if (strchr(drivers[i]->description,'('))
/*TODO*///							strcat(name_ref,strchr(drivers[i]->description,'('));
/*TODO*///					}
/*TODO*///
/*TODO*///					//printf("| %-33.33s",name_ref);
/*TODO*///					printf("| %-40.40s",name_ref);
/*TODO*///
/*TODO*///					if (drivers[i]->flags & GAME_NOT_WORKING)
/*TODO*///					{
/*TODO*///						const struct GameDriver *maindrv;
/*TODO*///						int foundworking;
/*TODO*///
/*TODO*///						if (drivers[i]->clone_of && !(drivers[i]->clone_of->flags & NOT_A_DRIVER))
/*TODO*///							maindrv = drivers[i]->clone_of;
/*TODO*///						else maindrv = drivers[i];
/*TODO*///
/*TODO*///						foundworking = 0;
/*TODO*///						j = 0;
/*TODO*///						while (drivers[j])
/*TODO*///						{
/*TODO*///							if (drivers[j] == maindrv || drivers[j]->clone_of == maindrv)
/*TODO*///							{
/*TODO*///								if ((drivers[j]->flags & GAME_NOT_WORKING) == 0)
/*TODO*///								{
/*TODO*///									foundworking = 1;
/*TODO*///									break;
/*TODO*///								}
/*TODO*///							}
/*TODO*///							j++;
/*TODO*///						}
/*TODO*///
/*TODO*///						if (foundworking)
/*TODO*///							printf("| No(1) ");
/*TODO*///						else
/*TODO*///							printf("|   No  ");
/*TODO*///					}
/*TODO*///					else
/*TODO*///						printf("|  Yes  ");
/*TODO*///
/*TODO*///					if (drivers[i]->flags & GAME_WRONG_COLORS)
/*TODO*///						printf("|   No  ");
/*TODO*///					else if (drivers[i]->flags & GAME_IMPERFECT_COLORS)
/*TODO*///						printf("| Close ");
/*TODO*///					else
/*TODO*///						printf("|  Yes  ");
/*TODO*///
/*TODO*///					{
/*TODO*///						const char **samplenames = 0;
/*TODO*///						for (j = 0;drivers[i]->drv->sound[j].sound_type && j < MAX_SOUND; j++)
/*TODO*///						{
/*TODO*///							if (drivers[i]->drv->sound[j].sound_type == SOUND_SAMPLES)
/*TODO*///							{
/*TODO*///								samplenames = ((struct Samplesinterface *)drivers[i]->drv->sound[j].sound_interface)->samplenames;
/*TODO*///								break;
/*TODO*///							}
/*TODO*///						}
/*TODO*///						if (drivers[i]->flags & GAME_NO_SOUND)
/*TODO*///							printf("|   No  ");
/*TODO*///						else if (drivers[i]->flags & GAME_IMPERFECT_SOUND)
/*TODO*///						{
/*TODO*///							if (samplenames)
/*TODO*///								printf("|Part(2)");
/*TODO*///							else
/*TODO*///								printf("|Partial");
/*TODO*///						}
/*TODO*///						else
/*TODO*///						{
/*TODO*///							if (samplenames)
/*TODO*///								printf("| Yes(2)");
/*TODO*///							else
/*TODO*///								printf("|  Yes  ");
/*TODO*///						}
/*TODO*///					}
/*TODO*///
/*TODO*///					printf("| %-8s |\n",drivers[i]->name);
/*TODO*///				}
/*TODO*///				i++;
/*TODO*///			}
/*TODO*///
/*TODO*///			printf("+-----------------------------------------+-------+-------+-------+----------+\n");
/*TODO*///			printf("(1) There are variants of the system that work correctly\n");
/*TODO*///			printf("(2) Needs samples provided separately\n\n\n\n\n");
/*TODO*///
/*TODO*///
/*TODO*///		printf("QUICK MESS USAGE GUIDE!\n"
/*TODO*///		       "=======================\n"
/*TODO*///		       "In order to use MESS, you must at least specify at the command line\n\n"
/*TODO*///               "      MESS <system>\n\n"
/*TODO*///			   "This will emulate the system requested.  Note that most systems require the BIOS for\n"
/*TODO*///			   "emulation.  These system BIOS files are copyright and ARE NOT supplied with MESS.\n\n"
/*TODO*///			   "To use files created for the system emulated (SOFTWARE), MESS works by attaching these\n"
/*TODO*///			   "files created for the particular device of that system, for example, a cartridge,\n"
/*TODO*///               "floppydisk, harddisk, cassette, software etc.  Therefore, in order to attach software to the\n"
/*TODO*///			   "system, you must specify at the command line:\n\n"
/*TODO*///               "      MESS <system> <device> <software_name>\n\n"
/*TODO*///			   "To manually manipulate the emulation options, you must specify:\n\n"
/*TODO*///               "      MESS <system> <device> <software_name> <options>\n\n");
/*TODO*///		printf("*For a complete list of systems emulated,  use: MESS -listfull\n"
/*TODO*///			   "*For system files (BIOS) required by each system, use: MESS <system> -listroms\n"
/*TODO*///			   "*See below for valid device names and usage.\n"
/*TODO*///			   "*See the MAME readme.txt and below for a detailed list of options.\n\n"
/*TODO*///			   "Make sure you have BIOS and SOFTWARE in a subdirectory specified in mess.cfg\n\n\n");
/*TODO*///		printf("Examples:\n\n"
/*TODO*///			   "    MESS nes -cart zelda.nes\n"
/*TODO*///			   "        will attach zelda.nes to the cartridge device and run MESS in\n"
/*TODO*///			   "        the following way:\n"
/*TODO*///			   "        <system>        = nes             (Nintendo Entertainment System)\n"
/*TODO*///			   "        <device>        = CARTRIDGE\n"
/*TODO*///			   "        <software_name> = zelda.nes       (Zelda cartridge)\n"
/*TODO*///			   "        <options>       = none specified, so default options (see mess.cfg)\n\n"
/*TODO*///			   "    MESS coleco -cart dkong -soundcard 0\n"
/*TODO*///			   "        will run MESS in the following way:\n\n"
/*TODO*///			   "        <system>        = coleco          (ColecoVision)\n"
/*TODO*///			   "        <device>        = CARTRIDGE\n"
/*TODO*///			   "        <software_name> = dkong.rom       (Donkey Kong cartridge)\n"
/*TODO*///			   "        <options>       = default options without sound (see mess.cfg)\n\n"
/*TODO*///			   "    MESS trs80 -flop boot.dsk -flop arcade1.dsk\n"
/*TODO*///			   "        will run MESS in the following way:\n"
/*TODO*///			   "        <system>         = trs80           (TRs-80 model 1)\n"
/*TODO*///			   "        <device1>        = FLOPPYDISK\n"
/*TODO*///			   "        <software_name1> = boot.dsk        (The Trs80 boot floppy diskl)\n"
/*TODO*///			   "        <device2>        = FLOPPYDISK\n"
/*TODO*///			   "        <software_name2> = arcade1.dsk     (floppy Disk which contains games)\n"
/*TODO*///			   "        <options>        = default options (all listed in mess.cfg)\n\n"
/*TODO*///			   "    MESS cgenie -flop games1\n"
/*TODO*///			   "        will run the system Colour Genie with one disk loaded,\n"
/*TODO*///			   "        automatically appending the file extension .dsk.\n\n\n"
/*TODO*///			   "If you dont want to type out device/image combinations, MESS supports \n"
/*TODO*///			   "ALIASed command lines from MESS.cfg.  An example entry is:\n\n"
/*TODO*///			   "    [ti99_4a]\n"
/*TODO*///			   "    parsec = -cart parsecg.bin -cart parsecc.bin \n\n"
/*TODO*///			   "So to load these files, you would simply then type:\n\n"
/*TODO*///			   "    MESS ti99_4a -alias parsec\n\n"
/*TODO*///			   "and both cartridges will be attached to the TI99_4a.\n\n\n");
/*TODO*///
/*TODO*///
/*TODO*///		printf("To automatically create the individual system directories in the \n"
/*TODO*///		       "SOFTWARE folder, use:\n"
/*TODO*///		       "MESS -createdir\n\n\n\n\n\n");
/*TODO*///
/*TODO*///		printf("DEVICE support list\n");
/*TODO*///		printf("===================\n");
/*TODO*///		printf("As mentioned, in order to fully utilise MESS, you will need to attach software files\n"
/*TODO*///			   "to the system devices.  The following list specifies all the devices and software \n"
/*TODO*///			   "file extensions currently supported by MESS.  Remember to use the DEVICE name \n"
/*TODO*///			   "(or the brief name) to attach software.  This list can easily be generated by \n"
/*TODO*///			   "specifying:\n\n"
/*TODO*///			   "    MESS -listdevices\n\n");
/*TODO*///		printf("Also note that MESS has a preliminary built-in File Manager for attaching images to\n"
/*TODO*///			   "system devices.  Use the UI (TAB key) to access.\n\n\n\n");
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*///	}
/*TODO*///
/*TODO*///    else if (!stricmp(arg, "-createdir"))
/*TODO*///	{
/*TODO*///	/***************************************************/
/*TODO*///	/* To create the SOFTWARE directories */
/*TODO*///		char* sys_rom_path  = "SOFTWARE";
/*TODO*///		char buf[128];
/*TODO*///		int d=0;
/*TODO*///
/*TODO*///			/* ensure the roms directory exists! */
/*TODO*///			sprintf(buf,"%s %s","md",sys_rom_path);
/*TODO*///			printf("%s\n",buf);
/*TODO*///			system(buf);
/*TODO*///
/*TODO*///			/* create directory for all currently supported drivers */
/*TODO*///			while(drivers[d])
/*TODO*///			{
/*TODO*///				/* create the systems directory */
/*TODO*///				sprintf(buf,"%s %s\\%s","md",sys_rom_path,drivers[d]->name);
/*TODO*///				printf("%s\n",buf);
/*TODO*///				system(buf);
/*TODO*///				d++;
/*TODO*///			}
/*TODO*///	}
/*TODO*///
/*TODO*///
/*TODO*///}    
}
