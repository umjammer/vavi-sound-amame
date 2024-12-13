package arcadeflex056;

import static common.libc.cstring.*;
import static common.libc.cstdio.*;
import static mame056.driverH.*;
import static mame056.version.*;
import static mame056.common.*;

import static mame056.driver.*;

import static mess056.mess.*;

public class fronthlp {
		
		public static int silentident,knownstatus;
		
		public static final int KNOWN_START  = 0;
	    public static final int KNOWN_ALL    = 1;
	    public static final int KNOWN_NONE   = 2;
	    public static final int KNOWN_SOME   = 3;
	/*TODO*///	
	/*TODO*///	extern unsigned int crc32 (unsigned int crc, const unsigned char *buf, unsigned int len);
	/*TODO*///	
	/*TODO*///	
	/*TODO*///	void get_rom_sample_path (int argc, char **argv, int game_index, char *override_default_rompath);
	/*TODO*///	
		public static GameDriver gamedrv;
	/*TODO*///	
	/*TODO*///	/* compare string[8] using standard(?) DOS wildchars ('?' & '*')      */
	/*TODO*///	/* for this to work correctly, the shells internal wildcard expansion */
	/*TODO*///	/* mechanism has to be disabled. Look into msdos.c */
	/*TODO*///	
	/*TODO*///	int strwildcmp(const char *sp1, const char *sp2)
	/*TODO*///	{
	/*TODO*///		char s1[9], s2[9];
	/*TODO*///		int i, l1, l2;
	/*TODO*///		char *p;
	/*TODO*///	
	/*TODO*///		strncpy(s1, sp1, 8); s1[8] = 0; if (s1[0] == 0) strcpy(s1, "*");
	/*TODO*///	
	/*TODO*///		strncpy(s2, sp2, 8); s2[8] = 0; if (s2[0] == 0) strcpy(s2, "*");
	/*TODO*///	
	/*TODO*///		p = strchr(s1, '*');
	/*TODO*///		if (p)
	/*TODO*///		{
	/*TODO*///			for (i = p - s1; i < 8; i++) s1[i] = '?';
	/*TODO*///			s1[8] = 0;
	/*TODO*///		}
	/*TODO*///	
	/*TODO*///		p = strchr(s2, '*');
	/*TODO*///		if (p)
	/*TODO*///		{
	/*TODO*///			for (i = p - s2; i < 8; i++) s2[i] = '?';
	/*TODO*///			s2[8] = 0;
	/*TODO*///		}
	/*TODO*///	
	/*TODO*///		l1 = strlen(s1);
	/*TODO*///		if (l1 < 8)
	/*TODO*///		{
	/*TODO*///			for (i = l1 + 1; i < 8; i++) s1[i] = ' ';
	/*TODO*///			s1[8] = 0;
	/*TODO*///		}
	/*TODO*///	
	/*TODO*///		l2 = strlen(s2);
	/*TODO*///		if (l2 < 8)
	/*TODO*///		{
	/*TODO*///			for (i = l2 + 1; i < 8; i++) s2[i] = ' ';
	/*TODO*///			s2[8] = 0;
	/*TODO*///		}
	/*TODO*///	
	/*TODO*///		for (i = 0; i < 8; i++)
	/*TODO*///		{
	/*TODO*///			if (s1[i] == '?' && s2[i] != '?') s1[i] = s2[i];
	/*TODO*///			if (s2[i] == '?' && s1[i] != '?') s2[i] = s1[i];
	/*TODO*///		}
	/*TODO*///	
	/*TODO*///		return stricmp(s1, s2);
	/*TODO*///	}
	/*TODO*///	
	/*TODO*///	
	/*TODO*///	/* Identifies a rom from from this checksum */
	/*TODO*///	void identify_rom(const char* name, int checksum, int length)
	/*TODO*///	{
	/*TODO*///	/* Nicola output format */
	/*TODO*///	#if 1
	/*TODO*///		int found = 0;
	/*TODO*///	
	/*TODO*///		/* remove directory name */
	/*TODO*///		int i;
	/*TODO*///		for (i = strlen(name)-1;i >= 0;i--)
	/*TODO*///		{
	/*TODO*///			if (name[i] == '/' || name[i] == '\\')
	/*TODO*///			{
	/*TODO*///				i++;
	/*TODO*///				break;
	/*TODO*///			}
	/*TODO*///		}
	/*TODO*///		if (silentident == 0)
	/*TODO*///			printf("%-12s ",&name[i]);
	/*TODO*///	
	/*TODO*///		for (i = 0; drivers[i]; i++)
	/*TODO*///		{
	/*TODO*///			const struct RomModule *region, *rom;
	/*TODO*///	
	/*TODO*///			for (region = rom_first_region(drivers[i]); region; region = rom_next_region(region))
	/*TODO*///				for (rom = rom_first_file(region); rom; rom = rom_next_file(rom))
	/*TODO*///				{
	/*TODO*///					if (checksum == ROM_GETCRC(rom))
	/*TODO*///					{
	/*TODO*///						if (silentident == 0)
	/*TODO*///						{
	/*TODO*///							if (found != 0)
	/*TODO*///								printf("             ");
	/*TODO*///							printf("= %-12s  %s\n",ROM_GETNAME(rom),drivers[i]->description);
	/*TODO*///						}
	/*TODO*///						found++;
	/*TODO*///					}
	/*TODO*///					if (BADCRC(checksum) == ROM_GETCRC(rom))
	/*TODO*///					{
	/*TODO*///						if (silentident == 0)
	/*TODO*///						{
	/*TODO*///							if (found != 0)
	/*TODO*///								printf("             ");
	/*TODO*///							printf("= (BAD) %-12s  %s\n",ROM_GETNAME(rom),drivers[i]->description);
	/*TODO*///						}
	/*TODO*///						found++;
	/*TODO*///					}
	/*TODO*///				}
	/*TODO*///		}
	/*TODO*///		if (found == 0)
	/*TODO*///		{
	/*TODO*///			unsigned size = length;
	/*TODO*///			while (size && (size & 1) == 0) size >>= 1;
	/*TODO*///			if (size & ~1)
	/*TODO*///			{
	/*TODO*///				if (silentident == 0)
	/*TODO*///					printf("NOT A ROM\n");
	/*TODO*///			}
	/*TODO*///			else
	/*TODO*///			{
	/*TODO*///				if (silentident == 0)
	/*TODO*///					printf("NO MATCH\n");
	/*TODO*///				if (knownstatus == KNOWN_START)
	/*TODO*///					knownstatus = KNOWN_NONE;
	/*TODO*///				else if (knownstatus == KNOWN_ALL)
	/*TODO*///					knownstatus = KNOWN_SOME;
	/*TODO*///			}
	/*TODO*///		}
	/*TODO*///		else
	/*TODO*///		{
	/*TODO*///			if (knownstatus == KNOWN_START)
	/*TODO*///				knownstatus = KNOWN_ALL;
	/*TODO*///			else if (knownstatus == KNOWN_NONE)
	/*TODO*///				knownstatus = KNOWN_SOME;
	/*TODO*///		}
	/*TODO*///	#else
	/*TODO*///	/* New output format */
	/*TODO*///		int i;
	/*TODO*///		printf("%s\n",name);
	/*TODO*///	
	/*TODO*///		for (i = 0; drivers[i]; i++)
	/*TODO*///		{
	/*TODO*///			const struct RomModule *region, *rom;
	/*TODO*///	
	/*TODO*///			for (region = rom_first_region(drivers[i]; region; region = rom_next_region(region))
	/*TODO*///				for (rom = rom_first_file(region); rom; rom = rom_next_file(rom))
	/*TODO*///					if (checksum == ROM_GETCRC(romp))
	/*TODO*///					{
	/*TODO*///						printf("\t%s/%s %s, %s, %s\n",drivers[i]->name,ROM_GETNAME(rom),
	/*TODO*///							drivers[i]->description,
	/*TODO*///							drivers[i]->manufacturer,
	/*TODO*///							drivers[i]->year);
	/*TODO*///					}
	/*TODO*///		}
	/*TODO*///	#endif
	/*TODO*///	}
	/*TODO*///	
	/*TODO*///	/* Identifies a file from from this checksum */
	/*TODO*///	void identify_file(const char* name)
	/*TODO*///	{
	/*TODO*///		FILE *f;
	/*TODO*///		int length;
	/*TODO*///		char* data;
	/*TODO*///	
	/*TODO*///		f = fopen(name,"rb");
	/*TODO*///		if (f == 0) {
	/*TODO*///			return;
	/*TODO*///		}
	/*TODO*///	
	/*TODO*///		/* determine length of file */
	/*TODO*///		if (fseek (f, 0L, SEEK_END)!=0)	{
	/*TODO*///			fclose(f);
	/*TODO*///			return;
	/*TODO*///		}
	/*TODO*///	
	/*TODO*///		length = ftell(f);
	/*TODO*///		if (length == -1L) {
	/*TODO*///			fclose(f);
	/*TODO*///			return;
	/*TODO*///		}
	/*TODO*///	
	/*TODO*///		/* empty file */
	/*TODO*///		if (length == 0) {
	/*TODO*///			fclose(f);
	/*TODO*///			return;
	/*TODO*///		}
	/*TODO*///	
	/*TODO*///		/* allocate space for entire file */
	/*TODO*///		data = (char*)malloc(length);
	/*TODO*///		if (data == 0) {
	/*TODO*///			fclose(f);
	/*TODO*///			return;
	/*TODO*///		}
	/*TODO*///	
	/*TODO*///		if (fseek (f, 0L, SEEK_SET)!=0) {
	/*TODO*///			free(data);
	/*TODO*///			fclose(f);
	/*TODO*///			return;
	/*TODO*///		}
	/*TODO*///	
	/*TODO*///		if (fread(data, 1, length, f) != length) {
	/*TODO*///			free(data);
	/*TODO*///			fclose(f);
	/*TODO*///			return;
	/*TODO*///		}
	/*TODO*///	
	/*TODO*///		fclose(f);
	/*TODO*///	
	/*TODO*///		identify_rom(name, crc32(0L,(const unsigned char*)data,length),length);
	/*TODO*///	
	/*TODO*///		free(data);
	/*TODO*///	}
	/*TODO*///	
	/*TODO*///	void identify_zip(const char* zipname)
	/*TODO*///	{
	/*TODO*///		struct zipent* ent;
	/*TODO*///	
	/*TODO*///		ZIP* zip = openzip( zipname );
	/*TODO*///		if (zip == 0)
	/*TODO*///			return;
	/*TODO*///	
	/*TODO*///		while ((ent = readzip(zip))) {
	/*TODO*///			/* Skip empty file and directory */
	/*TODO*///			if (ent->uncompressed_size!=0) {
	/*TODO*///				char* buf = (char*)malloc(strlen(zipname)+1+strlen(ent->name)+1);
	/*TODO*///				sprintf(buf,"%s/%s",zipname,ent->name);
	/*TODO*///				identify_rom(buf,ent->crc32,ent->uncompressed_size);
	/*TODO*///				free(buf);
	/*TODO*///			}
	/*TODO*///		}
	/*TODO*///	
	/*TODO*///		closezip(zip);
	/*TODO*///	}
	/*TODO*///	
	/*TODO*///	void romident(const char* name, int enter_dirs);
	/*TODO*///	
	/*TODO*///	void identify_dir(const char* dirname)
	/*TODO*///	{
	/*TODO*///		DIR *dir;
	/*TODO*///		struct dirent *ent;
	/*TODO*///	
	/*TODO*///		dir = opendir(dirname);
	/*TODO*///		if (dir == 0) {
	/*TODO*///			return;
	/*TODO*///		}
	/*TODO*///	
	/*TODO*///		ent = readdir(dir);
	/*TODO*///		while (ent) {
	/*TODO*///			/* Skip special files */
	/*TODO*///			if (ent->d_name[0]!='.') {
	/*TODO*///				char* buf = (char*)malloc(strlen(dirname)+1+strlen(ent->d_name)+1);
	/*TODO*///				sprintf(buf,"%s/%s",dirname,ent->d_name);
	/*TODO*///				romident(buf,0);
	/*TODO*///				free(buf);
	/*TODO*///			}
	/*TODO*///	
	/*TODO*///			ent = readdir(dir);
	/*TODO*///		}
	/*TODO*///		closedir(dir);
	/*TODO*///	}
	/*TODO*///	
	/*TODO*///	void romident(const char* name,int enter_dirs) {
	/*TODO*///		struct stat s;
	/*TODO*///	
	/*TODO*///		if (stat(name,&s) != 0)	{
	/*TODO*///			printf("%s: %s\n",name,strerror(errno));
	/*TODO*///			return;
	/*TODO*///		}
	/*TODO*///	
	/*TODO*///		if (S_ISDIR(s.st_mode)) {
	/*TODO*///			if (enter_dirs)
	/*TODO*///				identify_dir(name);
	/*TODO*///		} else {
	/*TODO*///			unsigned l = strlen(name);
	/*TODO*///			if (l>=4 && stricmp(name+l-4,".zip")==0)
	/*TODO*///				identify_zip(name);
	/*TODO*///			else
	/*TODO*///				identify_file(name);
	/*TODO*///			return;
	/*TODO*///		}
	/*TODO*///	}
	/*TODO*///	
	/*TODO*///	
	/*TODO*///	#ifndef MESS
	/*TODO*///	enum { LIST_LIST = 1, LIST_LISTINFO, LIST_LISTFULL, LIST_LISTSAMDIR, LIST_LISTROMS, LIST_LISTSAMPLES,
	/*TODO*///			LIST_LMR, LIST_LISTDETAILS, LIST_GAMELIST,
	/*TODO*///			LIST_LISTGAMES, LIST_LISTCLONES,
	/*TODO*///			LIST_WRONGORIENTATION, LIST_WRONGFPS, LIST_LISTCRC, LIST_LISTDUPCRC, LIST_WRONGMERGE,
	/*TODO*///			LIST_LISTROMSIZE, LIST_LISTCPU, LIST_SOURCEFILE };
	/*TODO*///	#else
	/*TODO*///	enum { LIST_LIST = 1, LIST_LISTINFO, LIST_LISTFULL, LIST_LISTSAMDIR, LIST_LISTROMS, LIST_LISTSAMPLES,
	/*TODO*///			LIST_LMR, LIST_LISTDETAILS, LIST_GAMELIST,
	/*TODO*///			LIST_LISTGAMES, LIST_LISTCLONES,
	/*TODO*///			LIST_WRONGORIENTATION, LIST_WRONGFPS, LIST_LISTCRC, LIST_LISTDUPCRC, LIST_WRONGMERGE,
	/*TODO*///			LIST_LISTROMSIZE, LIST_LISTCPU, LIST_SOURCEFILE, LIST_MESSINFO };
	/*TODO*///	#endif
	/*TODO*///	
		public static final int LIST_LIST 				= 1;
		public static final int LIST_LISTINFO			= 2;
		public static final int LIST_LISTFULL			= 3;
		public static final int LIST_LISTSAMDIR			= 4;
		public static final int LIST_LISTROMS			= 5;
		public static final int LIST_LISTSAMPLES		= 6;
		public static final int LIST_LMR				= 7;
		public static final int LIST_LISTDETAILS		= 8;
		public static final int LIST_GAMELIST			= 9;
		public static final int LIST_LISTGAMES			= 10;
		public static final int LIST_LISTCLONES			= 11;
		public static final int LIST_WRONGORIENTATION	= 12;
		public static final int LIST_WRONGFPS			= 13;
		public static final int LIST_LISTCRC			= 14;
		public static final int LIST_LISTDUPCRC			= 15;
		public static final int LIST_WRONGMERGE			= 16;
		public static final int LIST_LISTROMSIZE		= 17;
		public static final int LIST_LISTCPU			= 18;
		public static final int LIST_SOURCEFILE			= 19;
		public static final int LIST_MESSINFO			= 20;
	
		public static final int VERIFY_ROMS		= 0x00000001;
	    public static final int VERIFY_SAMPLES	= 0x00000002;
	    public static final int VERIFY_VERBOSE	= 0x00000004;
	    public static final int VERIFY_TERSE	= 0x00000008;
	
	/*TODO*///	void CLIB_DECL terse_printf(char *fmt,...)
	/*TODO*///	{
	/*TODO*///		/* no-op */
	/*TODO*///	}
	/*TODO*///	
	/*TODO*///	
	/*TODO*///	int CLIB_DECL compare_names(const void *elem1, const void *elem2)
	/*TODO*///	{
	/*TODO*///		struct GameDriver *drv1 = *(struct GameDriver **)elem1;
	/*TODO*///		struct GameDriver *drv2 = *(struct GameDriver **)elem2;
	/*TODO*///		return strcmp(drv1->description, drv2->description);
	/*TODO*///	}
	/*TODO*///	
	/*TODO*///	
	/*TODO*///	int CLIB_DECL compare_driver_names(const void *elem1, const void *elem2)
	/*TODO*///	{
	/*TODO*///		struct GameDriver *drv1 = *(struct GameDriver **)elem1;
	/*TODO*///		struct GameDriver *drv2 = *(struct GameDriver **)elem2;
	/*TODO*///		return strcmp(drv1->name, drv2->name);
	/*TODO*///	}

	
	    public static int frontend_help(int argc, String[] argv)
	    {
			int i, j;
		    int list = 0;
			int listclones = 1;
			int verify = 0;
			int ident = 0;
			int help = 1;    /* by default is TRUE */
			String gamename="";//char gamename[9];
			int sortby = 0;
		
			/* covert '/' in '-' */
			//if (argv[1].length() > 0) {
				for (i = 1; i < argc; i++) {
					if (argv[i].charAt(0) != '/') {
						argv[i].replaceFirst("/", "-");
					}
				}
			//}
	/*TODO*///		/* by default display the help unless */
	/*TODO*///		/* a game or an utility are specified */
	/*TODO*///	
	/*TODO*///		strcpy(gamename, "");
	
		for (i = 1;i < argc;i++)
		{
			/* find the FIRST "gamename" field (without '-') */
            if ((strlen(gamename) == 0) && (argv[i].charAt(0) != '-'))
            {
                    /* check if a filename was entered as the game name */
                    /* and remove any remaining portion of file extension */
/*TODO*/ //                     for (j = 0;j < 8 && argv[i][j] && argv[i][j] != '.';j++)
/*TODO*/ //				gamename[j] = argv[i][j];
/*TODO*/ //                     gamename[j] = 0;
/*TEMPHACK*/                 gamename=argv[1];//TO_BE_REMOVED (temp solution but works ok)
               
            }
		}
	
		for (i = 1; i < argc; i++)
		{
			/* check for front-end utilities */
			if (stricmp(argv[i],"-list")==0) list = LIST_LIST;
	/*TODO*///	 		if (!stricmp(argv[i],"-listinfo")) list = LIST_LISTINFO;
	/*TODO*///			if (!stricmp(argv[i],"-listfull")) list = LIST_LISTFULL;
	/*TODO*///	        if (!stricmp(argv[i],"-listdetails")) list = LIST_LISTDETAILS; /* A detailed MAMELIST.TXT type roms lister */
	/*TODO*///	        if (!stricmp(argv[i],"-gamelist")) list = LIST_GAMELIST; /* GAMELIST.TXT */
	/*TODO*///			if (!stricmp(argv[i],"-listgames")) list = LIST_LISTGAMES;
	/*TODO*///			if (!stricmp(argv[i],"-listclones")) list = LIST_LISTCLONES;
	/*TODO*///			if (!stricmp(argv[i],"-listsamdir")) list = LIST_LISTSAMDIR;
	/*TODO*///			if (!stricmp(argv[i],"-listcrc")) list = LIST_LISTCRC;
	/*TODO*///			if (!stricmp(argv[i],"-listdupcrc")) list = LIST_LISTDUPCRC;
	/*TODO*///			if (!stricmp(argv[i],"-listwrongmerge")) list = LIST_WRONGMERGE;
	/*TODO*///			if (!stricmp(argv[i],"-listromsize")) list = LIST_LISTROMSIZE;
	/*TODO*///			if (!stricmp(argv[i],"-listcpu")) list = LIST_LISTCPU;
	/*TODO*///	
	/*TODO*///	#ifdef MAME_DEBUG /* do not put this into a public release! */
	/*TODO*///			if (!stricmp(argv[i],"-lmr")) list = LIST_LMR;
	/*TODO*///	#endif
	/*TODO*///			if (!stricmp(argv[i],"-wrongorientation")) list = LIST_WRONGORIENTATION;
	/*TODO*///			if (!stricmp(argv[i],"-wrongfps")) list = LIST_WRONGFPS;
	/*TODO*///			if (!stricmp(argv[i],"-noclones")) listclones = 0;
	/*TODO*///			if (!stricmp(argv[i],"-sortname")) sortby = 1;
	/*TODO*///			if (!stricmp(argv[i],"-sortdriver")) sortby = 2;
	/*TODO*///			#ifdef MESS
	/*TODO*///					if (!stricmp(argv[i],"-listdevices"))  list = LIST_MESSINFO;
	/*TODO*///					if (!stricmp(argv[i],"-listtext")) list = LIST_MESSINFO;
	/*TODO*///					if (!stricmp(argv[i],"-createdir")) list = LIST_MESSINFO;
	/*TODO*///			#endif
	
	
			/* these options REQUIRES gamename field to work */
			if (strlen(gamename) > 0)
			{
				if (stricmp(argv[i],"-listroms")==0) list = LIST_LISTROMS;
				if (stricmp(argv[i],"-listsamples")==0) list = LIST_LISTSAMPLES;
				if (stricmp(argv[i],"-verifyroms")==0) verify = VERIFY_ROMS;
				if (stricmp(argv[i],"-verifysets")==0) verify = VERIFY_ROMS|VERIFY_VERBOSE|VERIFY_TERSE;
				if (stricmp(argv[i],"-vset")==0) verify = VERIFY_ROMS|VERIFY_VERBOSE;
				if (stricmp(argv[i],"-verifysamples")==0) verify = VERIFY_SAMPLES|VERIFY_VERBOSE;
				if (stricmp(argv[i],"-vsam")==0) verify = VERIFY_SAMPLES|VERIFY_VERBOSE;
				if (stricmp(argv[i],"-romident")==0) ident = 1;
				if (stricmp(argv[i],"-isknown")==0) ident = 2;
				if (stricmp(argv[i],"-sourcefile")==0) list = LIST_SOURCEFILE;
			}
		}
	
		if ((strlen(gamename)> 0) || (list!=0) || (verify!=0)) help = 0;
	
		for (i = 1;i < argc;i++)
		{
			/* ...however, I WANT the help! */
			if (stricmp(argv[i],"-?")==0 || stricmp(argv[i],"-h")==0 || stricmp(argv[i],"-help")==0)
				help = 1;
		}
	
		if (help != 0)  /* brief help - useful to get current version info */
		{
			if (!settings.MESS){
			printf("M.A.M.E. v%s - Multiple Arcade Machine Emulator\n"+
					"Copyright (C) 1997-2001 by Nicola Salmoria and the MAME Team\n\n",build_version);
			showdisclaimer();
			printf("Usage:  MAME gamename [options]\n\n"+
					"        MAME -list      for a brief list of supported games\n"+
					"        MAME -listfull  for a full list of supported games\n\n"+
					"See readme.txt for a complete list of options.\n");
			}else{
				showmessinfo();
			}
			return 0;
		}
	
	/*TODO*///		/* sort the list if requested */
	/*TODO*///		if (sortby)
	/*TODO*///		{
	/*TODO*///			int count = 0;
	/*TODO*///	
	/*TODO*///			/* first count the drivers */
	/*TODO*///			while (drivers[count]) count++;
	/*TODO*///	
	/*TODO*///			/* qsort as appropriate */
	/*TODO*///			if (sortby == 1)
	/*TODO*///				qsort(drivers, count, sizeof(drivers[0]), compare_names);
	/*TODO*///			else if (sortby == 2)
	/*TODO*///				qsort(drivers, count, sizeof(drivers[0]), compare_driver_names);
	/*TODO*///		}
	
		switch (list)  /* front-end utilities ;) */
		{
	/*TODO*///	
	/*TODO*///	        #ifdef MESS
	/*TODO*///			case LIST_MESSINFO: /* all mess specific calls here */
	/*TODO*///			{
	/*TODO*///				for (i=1;i<argc;i++)
	/*TODO*///				{
	/*TODO*///					/* list all mess info options here */
	/*TODO*///					if (
	/*TODO*///						!stricmp(argv[i],"-listdevices") |
	/*TODO*///						!stricmp(argv[i],"-listtext")    |
	/*TODO*///						!stricmp(argv[i],"-createdir")
	/*TODO*///					   )
	/*TODO*///				 	{
	/*TODO*///						/* send the gamename and arg to mess.c */
	/*TODO*///						list_mess_info(gamename, argv[i], listclones);
	/*TODO*///					}
	/*TODO*///				}
	/*TODO*///				return 0;
	/*TODO*///				break;
	/*TODO*///			}
	/*TODO*///			#endif
		
			case LIST_LIST: /* simple games list */
				if(!settings.MESS)
					printf("\nMAME currently supports the following games:\n\n");
				else
					printf("\nMESS currently supports the following systems:\n\n");
				
				i = 0; j = 0;
				while (drivers[i]!=null)
				{
	                            if ((listclones!=0 || drivers[i].clone_of == null
							|| ((drivers[i].clone_of.flags & NOT_A_DRIVER)!=0)
	/*TODO*/				 ) /*&& !strwildcmp(gamename, drivers_old[i].name)*/)
					{
						printf("%-8s",drivers[i].name);
						j++;
						if ((j % 8)==0) printf("\n");
						else printf("  ");
					}
					i++;
				}
				if ((j % 8)!=0) printf("\n");
				printf("\n");
				if (j != i) printf("Total ROM sets displayed: %4d - ", j);
				if(!settings.MESS)
					printf("Total ROM sets supported: %4d\n", i);
				else
					printf("Total Systems supported: %4d\n", i);
				
	            return 0;
				//break;
	
	/*TODO*///			case LIST_LISTFULL: /* games list with descriptions */
	/*TODO*///				printf("Name:     Description:\n");
	/*TODO*///				for (i = 0; drivers[i]; i++)
	/*TODO*///					if ((listclones || drivers[i]->clone_of == 0
	/*TODO*///							|| (drivers[i]->clone_of->flags & NOT_A_DRIVER)
	/*TODO*///							) && !strwildcmp(gamename, drivers[i]->name))
	/*TODO*///					{
	/*TODO*///						char name[200];
	/*TODO*///	
	/*TODO*///						printf("%-10s",drivers[i]->name);
	/*TODO*///	
	/*TODO*///						strcpy(name,drivers[i]->description);
	/*TODO*///	
	/*TODO*///						/* Move leading "The" to the end */
	/*TODO*///						if (strstr(name," (")) *strstr(name," (") = 0;
	/*TODO*///						if (strncmp(name,"The ",4) == 0)
	/*TODO*///						{
	/*TODO*///							printf("\"%s",name+4);
	/*TODO*///							printf(", The");
	/*TODO*///						}
	/*TODO*///						else
	/*TODO*///							printf("\"%s",name);
	/*TODO*///	
	/*TODO*///						/* print the additional description only if we are listing clones */
	/*TODO*///						if (listclones)
	/*TODO*///						{
	/*TODO*///							if (strchr(drivers[i]->description,'('))
	/*TODO*///								printf(" %s",strchr(drivers[i]->description,'('));
	/*TODO*///						}
	/*TODO*///						printf("\"\n");
	/*TODO*///					}
	/*TODO*///				return 0;
	/*TODO*///				break;
	/*TODO*///	
	/*TODO*///			case LIST_LISTSAMDIR: /* games list with samples directories */
	/*TODO*///				printf("Name:     Samples dir:\n");
	/*TODO*///				for (i = 0; drivers[i]; i++)
	/*TODO*///					if ((listclones || drivers[i]->clone_of == 0
	/*TODO*///							|| (drivers[i]->clone_of->flags & NOT_A_DRIVER)
	/*TODO*///							) && !strwildcmp(gamename, drivers[i]->name))
	/*TODO*///					{
	/*TODO*///	#if (HAS_SAMPLES || HAS_VLM5030)
	/*TODO*///						for( j = 0; drivers[i]->drv->sound[j].sound_type && j < MAX_SOUND; j++ )
	/*TODO*///						{
	/*TODO*///							const char **samplenames = NULL;
	/*TODO*///	#if (HAS_SAMPLES)
	/*TODO*///							if( drivers[i]->drv->sound[j].sound_type == SOUND_SAMPLES )
	/*TODO*///								samplenames = ((struct Samplesinterface *)drivers[i]->drv->sound[j].sound_interface)->samplenames;
	/*TODO*///	#endif
	/*TODO*///	#if (HAS_VLM5030)
	/*TODO*///							if( drivers[i]->drv->sound[j].sound_type == SOUND_VLM5030 )
	/*TODO*///								samplenames = ((struct VLM5030interface *)drivers[i]->drv->sound[j].sound_interface)->samplenames;
	/*TODO*///	#endif
	/*TODO*///							if (samplenames != 0 && samplenames[0] != 0)
	/*TODO*///							{
	/*TODO*///								printf("%-10s",drivers[i]->name);
	/*TODO*///								if (samplenames[0][0] == '*')
	/*TODO*///									printf("%s\n",samplenames[0]+1);
	/*TODO*///								else
	/*TODO*///									printf("%s\n",drivers[i]->name);
	/*TODO*///							}
	/*TODO*///						}
	/*TODO*///	#endif
	/*TODO*///					}
	/*TODO*///				return 0;
	/*TODO*///				break;
	/*TODO*///	
	/*TODO*///			case LIST_LISTROMS: /* game roms list or */
	/*TODO*///			case LIST_LISTSAMPLES: /* game samples list */
	/*TODO*///				j = 0;
	/*TODO*///				while (drivers[j] && (stricmp(gamename,drivers[j]->name) != 0))
	/*TODO*///					j++;
	/*TODO*///				if (drivers[j] == 0)
	/*TODO*///				{
	/*TODO*///					printf("Game \"%s\" not supported!\n",gamename);
	/*TODO*///					return 1;
	/*TODO*///				}
	/*TODO*///				gamedrv = drivers[j];
	/*TODO*///				if (list == LIST_LISTROMS)
	/*TODO*///					printromlist(gamedrv->rom,gamename);
	/*TODO*///				else
	/*TODO*///				{
	/*TODO*///	#if (HAS_SAMPLES || HAS_VLM5030)
	/*TODO*///					int k;
	/*TODO*///					for( k = 0; gamedrv->drv->sound[k].sound_type && k < MAX_SOUND; k++ )
	/*TODO*///					{
	/*TODO*///						const char **samplenames = NULL;
	/*TODO*///	#if (HAS_SAMPLES)
	/*TODO*///						if( gamedrv->drv->sound[k].sound_type == SOUND_SAMPLES )
	/*TODO*///								samplenames = ((struct Samplesinterface *)gamedrv->drv->sound[k].sound_interface)->samplenames;
	/*TODO*///	#endif
	/*TODO*///	#if (HAS_VLM5030)
	/*TODO*///						if( gamedrv->drv->sound[k].sound_type == SOUND_VLM5030 )
	/*TODO*///								samplenames = ((struct VLM5030interface *)gamedrv->drv->sound[k].sound_interface)->samplenames;
	/*TODO*///	#endif
	/*TODO*///						if (samplenames != 0 && samplenames[0] != 0)
	/*TODO*///						{
	/*TODO*///							i = 0;
	/*TODO*///							while (samplenames[i] != 0)
	/*TODO*///							{
	/*TODO*///								printf("%s\n",samplenames[i]);
	/*TODO*///								i++;
	/*TODO*///							}
	/*TODO*///						}
	/*TODO*///	                }
	/*TODO*///	#endif
	/*TODO*///				}
	/*TODO*///				return 0;
	/*TODO*///				break;
	/*TODO*///	
	/*TODO*///			case LIST_LMR:
	/*TODO*///				{
	/*TODO*///					int total;
	/*TODO*///	
	/*TODO*///					total = 0;
	/*TODO*///					for (i = 0; drivers[i]; i++)
	/*TODO*///							total++;
	/*TODO*///					for (i = 0; drivers[i]; i++)
	/*TODO*///					{
	/*TODO*///						static int first_missing = 1;
	/*TODO*///						get_rom_sample_path (argc, argv, i, NULL);
	/*TODO*///						if (RomsetMissing (i))
	/*TODO*///						{
	/*TODO*///							if (first_missing)
	/*TODO*///							{
	/*TODO*///								first_missing = 0;
	/*TODO*///								printf ("game      clone of  description\n");
	/*TODO*///								printf ("--------  --------  -----------\n");
	/*TODO*///							}
	/*TODO*///							printf ("%-10s%-10s%s\n",
	/*TODO*///									drivers[i]->name,
	/*TODO*///									(drivers[i]->clone_of) ? drivers[i]->clone_of->name : "",
	/*TODO*///									drivers[i]->description);
	/*TODO*///						}
	/*TODO*///						fprintf(stderr,"%d%%\r",100 * (i+1) / total);
	/*TODO*///					}
	/*TODO*///				}
	/*TODO*///				return 0;
	/*TODO*///				break;
	/*TODO*///	
	/*TODO*///			case LIST_LISTDETAILS: /* A detailed MAMELIST.TXT type roms lister */
	/*TODO*///	
	/*TODO*///				/* First, we shall print the header */
	/*TODO*///	
	/*TODO*///				printf(" romname driver     ");
	/*TODO*///				for(j=0;j<MAX_CPU;j++) printf("cpu %d    ",j+1);
	/*TODO*///				for(j=0;j<MAX_SOUND;j++) printf("sound %d     ",j+1);
	/*TODO*///				printf("name\n");
	/*TODO*///				printf("-------- ---------- ");
	/*TODO*///				for(j=0;j<MAX_CPU;j++) printf("-------- ");
	/*TODO*///				for(j=0;j<MAX_SOUND;j++) printf("----------- ");
	/*TODO*///				printf("--------------------------\n");
	/*TODO*///	
	/*TODO*///				/* Let's cycle through the drivers */
	/*TODO*///	
	/*TODO*///				for (i = 0; drivers[i]; i++)
	/*TODO*///					if ((listclones || drivers[i]->clone_of == 0
	/*TODO*///							|| (drivers[i]->clone_of->flags & NOT_A_DRIVER)
	/*TODO*///							) && !strwildcmp(gamename, drivers[i]->name))
	/*TODO*///					{
	/*TODO*///						/* Dummy structs to fetch the information from */
	/*TODO*///	
	/*TODO*///						const struct MachineDriver *x_driver = drivers[i]->drv;
	/*TODO*///						const struct MachineCPU *x_cpu = x_driver->cpu;
	/*TODO*///						const struct MachineSound *x_sound = x_driver->sound;
	/*TODO*///	
	/*TODO*///						/* First, the rom name */
	/*TODO*///	
	/*TODO*///						printf("%-8s ",drivers[i]->name);
	/*TODO*///	
	/*TODO*///						#ifndef MESS
	/*TODO*///						/* source file (skip the leading "src/drivers/" */
	/*TODO*///						printf("%-10s ",&drivers[i]->source_file[12]);
	/*TODO*///						#else
	/*TODO*///						/* source file (skip the leading "src/mess/systems/" */
	/*TODO*///						printf("%-10s ",&drivers[i]->source_file[17]);
	/*TODO*///						#endif
	/*TODO*///	
	/*TODO*///						/* Then, cpus */
	/*TODO*///	
	/*TODO*///						for(j=0;j<MAX_CPU;j++)
	/*TODO*///						{
	/*TODO*///							if (x_cpu[j].cpu_type & CPU_AUDIO_CPU)
	/*TODO*///								printf("[%-6s] ",cputype_name(x_cpu[j].cpu_type));
	/*TODO*///							else
	/*TODO*///								printf("%-8s ",cputype_name(x_cpu[j].cpu_type));
	/*TODO*///						}
	/*TODO*///	
	/*TODO*///						/* Then, sound chips */
	/*TODO*///	
	/*TODO*///						for(j=0;j<MAX_SOUND;j++)
	/*TODO*///						{
	/*TODO*///							if (sound_num(&x_sound[j]))
	/*TODO*///							{
	/*TODO*///								printf("%dx",sound_num(&x_sound[j]));
	/*TODO*///								printf("%-9s ",sound_name(&x_sound[j]));
	/*TODO*///							}
	/*TODO*///							else
	/*TODO*///								printf("%-11s ",sound_name(&x_sound[j]));
	/*TODO*///						}
	/*TODO*///	
	/*TODO*///						/* Lastly, the name of the game and a \newline */
	/*TODO*///	
	/*TODO*///						printf("%s\n",drivers[i]->description);
	/*TODO*///					}
	/*TODO*///				return 0;
	/*TODO*///				break;
	/*TODO*///	
	/*TODO*///			case LIST_GAMELIST: /* GAMELIST.TXT */
	/*TODO*///				printf("This is the complete list of games supported by MAME %s.\n",build_version);
	/*TODO*///				if (listclones == 0)
	/*TODO*///					printf("Variants of the same game are not included, you can use the -listclones command\n"
	/*TODO*///						"to get a list of the alternate versions of a given game.\n");
	/*TODO*///				printf("\n"
	/*TODO*///					"This list is generated automatically and is not 100%% accurate (particularly in\n"
	/*TODO*///					"the Screen Flip column). Please let us know of any errors so we can correct\n"
	/*TODO*///					"them.\n"
	/*TODO*///					"\n"
	/*TODO*///					"Here are the meanings of the columns:\n"
	/*TODO*///					"\n"
	/*TODO*///					"Working\n"
	/*TODO*///					"=======\n"
	/*TODO*///					"  NO: Emulation is still in progress; the game does not work correctly. This\n"
	/*TODO*///					"  means anything from major problems to a black screen.\n"
	/*TODO*///					"\n"
	/*TODO*///					"Correct Colors\n"
	/*TODO*///					"==============\n"
	/*TODO*///					"    YES: Colors should be identical to the original.\n"
	/*TODO*///					"  CLOSE: Colors are nearly correct.\n"
	/*TODO*///					"     NO: Colors are completely wrong. \n"
	/*TODO*///					"  \n"
	/*TODO*///					"  Note: In some cases, the color PROMs for some games are not yet available.\n"
	/*TODO*///					"  This causes a NO GOOD DUMP KNOWN message on startup (and, of course, the game\n"
	/*TODO*///					"  has wrong colors). The game will still say YES in this column, however,\n"
	/*TODO*///					"  because the code to handle the color PROMs has been added to the driver. When\n"
	/*TODO*///					"  the PROMs are available, the colors will be correct.\n"
	/*TODO*///					"\n"
	/*TODO*///					"Sound\n"
	/*TODO*///					"=====\n"
	/*TODO*///					"  PARTIAL: Sound support is incomplete or not entirely accurate. \n"
	/*TODO*///					"\n"
	/*TODO*///					"  Note: Some original games contain analog sound circuitry, which is difficult\n"
	/*TODO*///					"  to emulate. Therefore, these emulated sounds may be significantly different.\n"
	/*TODO*///					"\n"
	/*TODO*///					"Screen Flip\n"
	/*TODO*///					"===========\n"
	/*TODO*///					"  Many games were offered in cocktail-table models, allowing two players to sit\n"
	/*TODO*///					"  across from each other; the game's image flips 180 degrees for each player's\n"
	/*TODO*///					"  turn. Some games also have a \"Flip Screen\" DIP switch setting to turn the\n"
	/*TODO*///					"  picture (particularly useful with vertical games).\n"
	/*TODO*///					"  In many cases, this feature has not yet been emulated.\n"
	/*TODO*///					"\n"
	/*TODO*///					"Internal Name\n"
	/*TODO*///					"=============\n"
	/*TODO*///					"  This is the unique name that must be used when running the game from a\n"
	/*TODO*///					"  command line.\n"
	/*TODO*///					"\n"
	/*TODO*///					"  Note: Each game's ROM set must be placed in the ROM path, either in a .zip\n"
	/*TODO*///					"  file or in a subdirectory with the game's Internal Name. The former is\n"
	/*TODO*///					"  suggested, because the files will be identified by their CRC instead of\n"
	/*TODO*///					"  requiring specific names.\n\n");
	/*TODO*///				printf("+----------------------------------+-------+-------+-------+-------+----------+\n");
	/*TODO*///				printf("|                                  |       |Correct|       |Screen | Internal |\n");
	/*TODO*///				printf("| Game Name                        |Working|Colors | Sound | Flip  |   Name   |\n");
	/*TODO*///				printf("+----------------------------------+-------+-------+-------+-------+----------+\n");
	/*TODO*///	
	/*TODO*///				for (i = 0; drivers[i]; i++)
	/*TODO*///					if ((listclones || drivers[i]->clone_of == 0
	/*TODO*///							|| (drivers[i]->clone_of->flags & NOT_A_DRIVER)
	/*TODO*///							) && !strwildcmp(gamename, drivers[i]->name))
	/*TODO*///					{
	/*TODO*///						char name[200],name_ref[200];
	/*TODO*///	
	/*TODO*///						strcpy(name,drivers[i]->description);
	/*TODO*///	
	/*TODO*///						/* Move leading "The" to the end */
	/*TODO*///						if (strstr(name," (")) *strstr(name," (") = 0;
	/*TODO*///						if (strncmp(name,"The ",4) == 0)
	/*TODO*///						{
	/*TODO*///							sprintf(name_ref,"%s, The ",name+4);
	/*TODO*///						}
	/*TODO*///						else
	/*TODO*///							sprintf(name_ref,"%s ",name);
	/*TODO*///	
	/*TODO*///						/* print the additional description only if we are listing clones */
	/*TODO*///						if (listclones)
	/*TODO*///						{
	/*TODO*///							if (strchr(drivers[i]->description,'('))
	/*TODO*///								strcat(name_ref,strchr(drivers[i]->description,'('));
	/*TODO*///						}
	/*TODO*///	
	/*TODO*///						printf("| %-33.33s",name_ref);
	/*TODO*///	
	/*TODO*///						if (drivers[i]->flags & (GAME_NOT_WORKING | GAME_UNEMULATED_PROTECTION))
	/*TODO*///						{
	/*TODO*///							const struct GameDriver *maindrv;
	/*TODO*///							int foundworking;
	/*TODO*///	
	/*TODO*///							if (drivers[i]->clone_of && !(drivers[i]->clone_of->flags & NOT_A_DRIVER))
	/*TODO*///								maindrv = drivers[i]->clone_of;
	/*TODO*///							else maindrv = drivers[i];
	/*TODO*///	
	/*TODO*///							foundworking = 0;
	/*TODO*///							j = 0;
	/*TODO*///							while (drivers[j])
	/*TODO*///							{
	/*TODO*///								if (drivers[j] == maindrv || drivers[j]->clone_of == maindrv)
	/*TODO*///								{
	/*TODO*///									if ((drivers[j]->flags & (GAME_NOT_WORKING | GAME_UNEMULATED_PROTECTION)) == 0)
	/*TODO*///									{
	/*TODO*///										foundworking = 1;
	/*TODO*///										break;
	/*TODO*///									}
	/*TODO*///								}
	/*TODO*///								j++;
	/*TODO*///							}
	/*TODO*///	
	/*TODO*///							if (foundworking)
	/*TODO*///								printf("| No(1) ");
	/*TODO*///							else
	/*TODO*///								printf("|   No  ");
	/*TODO*///						}
	/*TODO*///						else
	/*TODO*///							printf("|  Yes  ");
	/*TODO*///	
	/*TODO*///						if (drivers[i]->flags & GAME_WRONG_COLORS)
	/*TODO*///							printf("|   No  ");
	/*TODO*///						else if (drivers[i]->flags & GAME_IMPERFECT_COLORS)
	/*TODO*///							printf("| Close ");
	/*TODO*///						else
	/*TODO*///							printf("|  Yes  ");
	/*TODO*///	
	/*TODO*///						{
	/*TODO*///							const char **samplenames = NULL;
	/*TODO*///	#if (HAS_SAMPLES || HAS_VLM5030)
	/*TODO*///							for (j = 0;drivers[i]->drv->sound[j].sound_type && j < MAX_SOUND; j++)
	/*TODO*///							{
	/*TODO*///	#if (HAS_SAMPLES)
	/*TODO*///								if (drivers[i]->drv->sound[j].sound_type == SOUND_SAMPLES)
	/*TODO*///								{
	/*TODO*///									samplenames = ((struct Samplesinterface *)drivers[i]->drv->sound[j].sound_interface)->samplenames;
	/*TODO*///									break;
	/*TODO*///								}
	/*TODO*///	#endif
	/*TODO*///	#if (HAS_VLM5030)
	/*TODO*///								if (drivers[i]->drv->sound[j].sound_type == SOUND_VLM5030)
	/*TODO*///								{
	/*TODO*///									samplenames = ((struct VLM5030interface *)drivers[i]->drv->sound[j].sound_interface)->samplenames;
	/*TODO*///									break;
	/*TODO*///								}
	/*TODO*///	#endif
	/*TODO*///							}
	/*TODO*///	#endif
	/*TODO*///							if (drivers[i]->flags & GAME_NO_SOUND)
	/*TODO*///								printf("|   No  ");
	/*TODO*///							else if (drivers[i]->flags & GAME_IMPERFECT_SOUND)
	/*TODO*///							{
	/*TODO*///								if (samplenames)
	/*TODO*///									printf("|Part(2)");
	/*TODO*///								else
	/*TODO*///									printf("|Partial");
	/*TODO*///							}
	/*TODO*///							else
	/*TODO*///							{
	/*TODO*///								if (samplenames)
	/*TODO*///									printf("| Yes(2)");
	/*TODO*///								else
	/*TODO*///									printf("|  Yes  ");
	/*TODO*///							}
	/*TODO*///						}
	/*TODO*///	
	/*TODO*///						if (drivers[i]->flags & GAME_NO_COCKTAIL)
	/*TODO*///							printf("|   No  ");
	/*TODO*///						else
	/*TODO*///							printf("|  Yes  ");
	/*TODO*///	
	/*TODO*///						printf("| %-8s |\n",drivers[i]->name);
	/*TODO*///					}
	/*TODO*///	
	/*TODO*///				printf("+----------------------------------+-------+-------+-------+-------+----------+\n\n");
	/*TODO*///				printf("(1) There are variants of the game (usually bootlegs) that work correctly\n");
	/*TODO*///	#if (HAS_SAMPLES)
	/*TODO*///				printf("(2) Needs samples provided separately\n");
	/*TODO*///	#endif
	/*TODO*///				return 0;
	/*TODO*///				break;
	/*TODO*///	
	/*TODO*///			case LIST_LISTGAMES: /* list games, production year, manufacturer */
	/*TODO*///				for (i = 0; drivers[i]; i++)
	/*TODO*///					if ((listclones || drivers[i]->clone_of == 0
	/*TODO*///							|| (drivers[i]->clone_of->flags & NOT_A_DRIVER)
	/*TODO*///							) && !strwildcmp(gamename, drivers[i]->description))
	/*TODO*///					{
	/*TODO*///						char name[200];
	/*TODO*///	
	/*TODO*///						printf("%-5s%-36s ",drivers[i]->year,drivers[i]->manufacturer);
	/*TODO*///	
	/*TODO*///						strcpy(name,drivers[i]->description);
	/*TODO*///	
	/*TODO*///						/* Move leading "The" to the end */
	/*TODO*///						if (strstr(name," (")) *strstr(name," (") = 0;
	/*TODO*///						if (strncmp(name,"The ",4) == 0)
	/*TODO*///						{
	/*TODO*///							printf("%s",name+4);
	/*TODO*///							printf(", The");
	/*TODO*///						}
	/*TODO*///						else
	/*TODO*///							printf("%s",name);
	/*TODO*///	
	/*TODO*///						/* print the additional description only if we are listing clones */
	/*TODO*///						if (listclones)
	/*TODO*///						{
	/*TODO*///							if (strchr(drivers[i]->description,'('))
	/*TODO*///								printf(" %s",strchr(drivers[i]->description,'('));
	/*TODO*///						}
	/*TODO*///						printf("\n");
	/*TODO*///					}
	/*TODO*///				return 0;
	/*TODO*///				break;
	/*TODO*///	
	/*TODO*///			case LIST_LISTCLONES: /* list clones */
	/*TODO*///				printf("Name:    Clone of:\n");
	/*TODO*///				for (i = 0; drivers[i]; i++)
	/*TODO*///					if (drivers[i]->clone_of && !(drivers[i]->clone_of->flags & NOT_A_DRIVER) &&
	/*TODO*///							(!strwildcmp(gamename,drivers[i]->name)
	/*TODO*///									|| !strwildcmp(gamename,drivers[i]->clone_of->name)))
	/*TODO*///						printf("%-8s %-8s\n",drivers[i]->name,drivers[i]->clone_of->name);
	/*TODO*///				return 0;
	/*TODO*///				break;
	/*TODO*///	
	/*TODO*///			case LIST_WRONGORIENTATION: /* list drivers which incorrectly use the orientation and visible area fields */
	/*TODO*///				for (i = 0; drivers[i]; i++)
	/*TODO*///					if ((drivers[i]->drv->video_attributes & VIDEO_TYPE_VECTOR) == 0 &&
	/*TODO*///							(drivers[i]->clone_of == 0
	/*TODO*///									|| (drivers[i]->clone_of->flags & NOT_A_DRIVER)) &&
	/*TODO*///							drivers[i]->drv->default_visible_area.max_x - drivers[i]->drv->default_visible_area.min_x + 1 <=
	/*TODO*///							drivers[i]->drv->default_visible_area.max_y - drivers[i]->drv->default_visible_area.min_y + 1)
	/*TODO*///					{
	/*TODO*///						if (strcmp(drivers[i]->name,"crater") &&
	/*TODO*///							strcmp(drivers[i]->name,"mpatrol") &&
	/*TODO*///							strcmp(drivers[i]->name,"troangel") &&
	/*TODO*///							strcmp(drivers[i]->name,"travrusa") &&
	/*TODO*///							strcmp(drivers[i]->name,"kungfum") &&
	/*TODO*///							strcmp(drivers[i]->name,"battroad") &&
	/*TODO*///							strcmp(drivers[i]->name,"vigilant") &&
	/*TODO*///							strcmp(drivers[i]->name,"sonson") &&
	/*TODO*///							strcmp(drivers[i]->name,"brkthru") &&
	/*TODO*///							strcmp(drivers[i]->name,"darwin") &&
	/*TODO*///							strcmp(drivers[i]->name,"exprraid") &&
	/*TODO*///							strcmp(drivers[i]->name,"sidetrac") &&
	/*TODO*///							strcmp(drivers[i]->name,"targ") &&
	/*TODO*///							strcmp(drivers[i]->name,"spectar") &&
	/*TODO*///							strcmp(drivers[i]->name,"venture") &&
	/*TODO*///							strcmp(drivers[i]->name,"mtrap") &&
	/*TODO*///							strcmp(drivers[i]->name,"pepper2") &&
	/*TODO*///							strcmp(drivers[i]->name,"hardhat") &&
	/*TODO*///							strcmp(drivers[i]->name,"fax") &&
	/*TODO*///							strcmp(drivers[i]->name,"circus") &&
	/*TODO*///							strcmp(drivers[i]->name,"robotbwl") &&
	/*TODO*///							strcmp(drivers[i]->name,"crash") &&
	/*TODO*///							strcmp(drivers[i]->name,"ripcord") &&
	/*TODO*///							strcmp(drivers[i]->name,"starfire") &&
	/*TODO*///							strcmp(drivers[i]->name,"fireone") &&
	/*TODO*///							strcmp(drivers[i]->name,"renegade") &&
	/*TODO*///							strcmp(drivers[i]->name,"battlane") &&
	/*TODO*///							strcmp(drivers[i]->name,"megatack") &&
	/*TODO*///							strcmp(drivers[i]->name,"killcom") &&
	/*TODO*///							strcmp(drivers[i]->name,"challeng") &&
	/*TODO*///							strcmp(drivers[i]->name,"kaos") &&
	/*TODO*///							strcmp(drivers[i]->name,"formatz") &&
	/*TODO*///							strcmp(drivers[i]->name,"bankp") &&
	/*TODO*///							strcmp(drivers[i]->name,"liberatr") &&
	/*TODO*///							strcmp(drivers[i]->name,"toki") &&
	/*TODO*///							strcmp(drivers[i]->name,"stactics") &&
	/*TODO*///							strcmp(drivers[i]->name,"sprint1") &&
	/*TODO*///							strcmp(drivers[i]->name,"sprint2") &&
	/*TODO*///							strcmp(drivers[i]->name,"nitedrvr") &&
	/*TODO*///							strcmp(drivers[i]->name,"punchout") &&
	/*TODO*///							strcmp(drivers[i]->name,"spnchout") &&
	/*TODO*///							strcmp(drivers[i]->name,"armwrest") &&
	/*TODO*///							strcmp(drivers[i]->name,"route16") &&
	/*TODO*///							strcmp(drivers[i]->name,"stratvox") &&
	/*TODO*///							strcmp(drivers[i]->name,"irobot") &&
	/*TODO*///							strcmp(drivers[i]->name,"leprechn") &&
	/*TODO*///							strcmp(drivers[i]->name,"starcrus") &&
	/*TODO*///							strcmp(drivers[i]->name,"astrof") &&
	/*TODO*///							strcmp(drivers[i]->name,"tomahawk") &&
	/*TODO*///							1)
	/*TODO*///							printf("%s %dx%d\n",drivers[i]->name,
	/*TODO*///									drivers[i]->drv->default_visible_area.max_x - drivers[i]->drv->default_visible_area.min_x + 1,
	/*TODO*///									drivers[i]->drv->default_visible_area.max_y - drivers[i]->drv->default_visible_area.min_y + 1);
	/*TODO*///					}
	/*TODO*///				return 0;
	/*TODO*///				break;
	/*TODO*///	
	/*TODO*///			case LIST_WRONGFPS: /* list drivers with too high frame rate */
	/*TODO*///				for (i = 0; drivers[i]; i++)
	/*TODO*///					if ((drivers[i]->drv->video_attributes & VIDEO_TYPE_VECTOR) == 0 &&
	/*TODO*///							(drivers[i]->clone_of == 0
	/*TODO*///									|| (drivers[i]->clone_of->flags & NOT_A_DRIVER)) &&
	/*TODO*///							drivers[i]->drv->frames_per_second > 57 &&
	/*TODO*///							drivers[i]->drv->default_visible_area.max_y - drivers[i]->drv->default_visible_area.min_y + 1 > 244 &&
	/*TODO*///							drivers[i]->drv->default_visible_area.max_y - drivers[i]->drv->default_visible_area.min_y + 1 <= 256)
	/*TODO*///					{
	/*TODO*///						printf("%s %dx%d %fHz\n",drivers[i]->name,
	/*TODO*///								drivers[i]->drv->default_visible_area.max_x - drivers[i]->drv->default_visible_area.min_x + 1,
	/*TODO*///								drivers[i]->drv->default_visible_area.max_y - drivers[i]->drv->default_visible_area.min_y + 1,
	/*TODO*///								drivers[i]->drv->frames_per_second);
	/*TODO*///					}
	/*TODO*///				return 0;
	/*TODO*///				break;
	/*TODO*///	
	/*TODO*///			case LIST_SOURCEFILE:
	/*TODO*///				for (i = 0; drivers[i]; i++)
	/*TODO*///					if (!strwildcmp(gamename,drivers[i]->name))
	/*TODO*///						printf("%-8s %s\n",drivers[i]->name,drivers[i]->source_file);
	/*TODO*///				return 0;
	/*TODO*///				break;
	/*TODO*///	
	/*TODO*///			case LIST_LISTCRC: /* list all crc-32 */
	/*TODO*///				for (i = 0; drivers[i]; i++)
	/*TODO*///				{
	/*TODO*///					const struct RomModule *region, *rom;
	/*TODO*///	
	/*TODO*///					for (region = rom_first_region(drivers[i]); region; region = rom_next_region(region))
	/*TODO*///						for (rom = rom_first_file(region); rom; rom = rom_next_file(rom))
	/*TODO*///							printf("%08x %-12s %s\n",ROM_GETCRC(rom),ROM_GETNAME(rom),drivers[i]->description);
	/*TODO*///				}
	/*TODO*///				return 0;
	/*TODO*///				break;
	/*TODO*///	
	/*TODO*///			case LIST_LISTDUPCRC: /* list duplicate crc-32 (with different ROM name) */
	/*TODO*///				for (i = 0; drivers[i]; i++)
	/*TODO*///				{
	/*TODO*///					const struct RomModule *region, *rom;
	/*TODO*///	
	/*TODO*///					for (region = rom_first_region(drivers[i]); region; region = rom_next_region(region))
	/*TODO*///						for (rom = rom_first_file(region); rom; rom = rom_next_file(rom))
	/*TODO*///							if (ROM_GETCRC(rom))
	/*TODO*///								for (j = i + 1; drivers[j]; j++)
	/*TODO*///								{
	/*TODO*///									const struct RomModule *region1, *rom1;
	/*TODO*///	
	/*TODO*///									for (region1 = rom_first_region(drivers[j]); region1; region1 = rom_next_region(region1))
	/*TODO*///										for (rom1 = rom_first_file(region1); rom1; rom1 = rom_next_file(rom1))
	/*TODO*///											if (strcmp(ROM_GETNAME(rom), ROM_GETNAME(rom1)) && ROM_GETCRC(rom) == ROM_GETCRC(rom1))
	/*TODO*///											{
	/*TODO*///												printf("%08x %-12s %-8s <-> %-12s %-8s\n",ROM_GETCRC(rom),
	/*TODO*///														ROM_GETNAME(rom),drivers[i]->name,
	/*TODO*///														ROM_GETNAME(rom1),drivers[j]->name);
	/*TODO*///											}
	/*TODO*///								}
	/*TODO*///				}
	/*TODO*///				return 0;
	/*TODO*///				break;
	/*TODO*///	
	/*TODO*///	
	/*TODO*///			case LIST_WRONGMERGE:	/* list duplicate crc-32 with different ROM name */
	/*TODO*///									/* and different crc-32 with duplicate ROM name */
	/*TODO*///									/* in clone sets */
	/*TODO*///				for (i = 0; drivers[i]; i++)
	/*TODO*///				{
	/*TODO*///					const struct RomModule *region, *rom;
	/*TODO*///	
	/*TODO*///					for (region = rom_first_region(drivers[i]); region; region = rom_next_region(region))
	/*TODO*///					{
	/*TODO*///						for (rom = rom_first_file(region); rom; rom = rom_next_file(rom))
	/*TODO*///						{
	/*TODO*///							if (ROM_GETCRC(rom))
	/*TODO*///							{
	/*TODO*///								for (j = 0; drivers[j]; j++)
	/*TODO*///								{
	/*TODO*///									if (j != i &&
	/*TODO*///										drivers[j]->clone_of &&
	/*TODO*///										(drivers[j]->clone_of->flags & NOT_A_DRIVER) == 0 &&
	/*TODO*///										(drivers[j]->clone_of == drivers[i] ||
	/*TODO*///										(i < j && drivers[j]->clone_of == drivers[i]->clone_of)))
	/*TODO*///									{
	/*TODO*///										const struct RomModule *region1, *rom1;
	/*TODO*///										int match = 0;
	/*TODO*///	
	/*TODO*///										for (region1 = rom_first_region(drivers[j]); region1; region1 = rom_next_region(region1))
	/*TODO*///										{
	/*TODO*///											for (rom1 = rom_first_file(region1); rom1; rom1 = rom_next_file(rom1))
	/*TODO*///											{
	/*TODO*///												if (!strcmp(ROM_GETNAME(rom), ROM_GETNAME(rom1)))
	/*TODO*///												{
	/*TODO*///													if (ROM_GETCRC(rom1) &&
	/*TODO*///															ROM_GETCRC(rom) != ROM_GETCRC(rom1) &&
	/*TODO*///															ROM_GETCRC(rom) != BADCRC(ROM_GETCRC(rom1)))
	/*TODO*///													{
	/*TODO*///														printf("%-12s %08x %-8s <-> %08x %-8s\n",ROM_GETNAME(rom),
	/*TODO*///																ROM_GETCRC(rom),drivers[i]->name,
	/*TODO*///																ROM_GETCRC(rom1),drivers[j]->name);
	/*TODO*///													}
	/*TODO*///													else
	/*TODO*///														match = 1;
	/*TODO*///												}
	/*TODO*///											}
	/*TODO*///										}
	/*TODO*///	
	/*TODO*///										if (match == 0)
	/*TODO*///										{
	/*TODO*///											for (region1 = rom_first_region(drivers[j]); region1; region1 = rom_next_region(region1))
	/*TODO*///											{
	/*TODO*///												for (rom1 = rom_first_file(region1); rom1; rom1 = rom_next_file(rom1))
	/*TODO*///												{
	/*TODO*///													if (strcmp(ROM_GETNAME(rom), ROM_GETNAME(rom1)) && ROM_GETCRC(rom) == ROM_GETCRC(rom1))
	/*TODO*///													{
	/*TODO*///														printf("%08x %-12s %-8s <-> %-12s %-8s\n",ROM_GETCRC(rom),
	/*TODO*///																ROM_GETNAME(rom),drivers[i]->name,
	/*TODO*///																ROM_GETNAME(rom1),drivers[j]->name);
	/*TODO*///													}
	/*TODO*///												}
	/*TODO*///											}
	/*TODO*///										}
	/*TODO*///									}
	/*TODO*///								}
	/*TODO*///							}
	/*TODO*///						}
	/*TODO*///					}
	/*TODO*///				}
	/*TODO*///				return 0;
	/*TODO*///				break;
	/*TODO*///	
	/*TODO*///			case LIST_LISTROMSIZE: /* I used this for statistical analysis */
	/*TODO*///				for (i = 0; drivers[i]; i++)
	/*TODO*///					if (drivers[i]->clone_of == 0 || (drivers[i]->clone_of->flags & NOT_A_DRIVER))
	/*TODO*///					{
	/*TODO*///						const struct RomModule *region, *rom, *chunk;
	/*TODO*///	
	/*TODO*///						j = 0;
	/*TODO*///						for (region = rom_first_region(drivers[i]); region; region = rom_next_region(region))
	/*TODO*///							for (rom = rom_first_file(region); rom; rom = rom_next_file(rom))
	/*TODO*///								for (chunk = rom_first_chunk(rom); chunk; chunk = rom_next_chunk(chunk))
	/*TODO*///									j += ROM_GETLENGTH(chunk);
	/*TODO*///	
	/*TODO*///						printf("%-8s\t%-5s\t%u\n",drivers[i]->name,drivers[i]->year,j);
	/*TODO*///					}
	/*TODO*///				return 0;
	/*TODO*///				break;
	/*TODO*///	
	/*TODO*///			case LIST_LISTCPU: /* I used this for statistical analysis */
	/*TODO*///				{
	/*TODO*///					int year;
	/*TODO*///	
	/*TODO*///					for (j = 1;j < CPU_COUNT;j++)
	/*TODO*///						printf("\t%s",cputype_name(j));
	/*TODO*///					printf("\n");
	/*TODO*///	
	/*TODO*///					for (year = 1980;year <= 1995;year++)
	/*TODO*///					{
	/*TODO*///						int count[CPU_COUNT];
	/*TODO*///	
	/*TODO*///						for (j = 0;j < CPU_COUNT;j++)
	/*TODO*///							count[j] = 0;
	/*TODO*///	
	/*TODO*///						i = 0;
	/*TODO*///						while (drivers[i])
	/*TODO*///						{
	/*TODO*///							if (drivers[i]->clone_of == 0 || (drivers[i]->clone_of->flags & NOT_A_DRIVER))
	/*TODO*///							{
	/*TODO*///								const struct MachineDriver *x_driver = drivers[i]->drv;
	/*TODO*///								const struct MachineCPU *x_cpu = x_driver->cpu;
	/*TODO*///	
	/*TODO*///								if (atoi(drivers[i]->year) == year)
	/*TODO*///								{
	/*TODO*///	//								for (j = 0;j < MAX_CPU;j++)
	/*TODO*///	j = 0;	// count only the main cpu
	/*TODO*///										count[x_cpu[j].cpu_type & ~CPU_FLAGS_MASK]++;
	/*TODO*///								}
	/*TODO*///							}
	/*TODO*///	
	/*TODO*///							i++;
	/*TODO*///						}
	/*TODO*///	
	/*TODO*///						printf("%d",year);
	/*TODO*///						for (j = 1;j < CPU_COUNT;j++)
	/*TODO*///							printf("\t%d",count[j]);
	/*TODO*///						printf("\n");
	/*TODO*///					}
	/*TODO*///				}
	/*TODO*///	
	/*TODO*///				return 0;
	/*TODO*///				break;
	/*TODO*///	
	/*TODO*///			case LIST_LISTINFO: /* list all info */
	/*TODO*///				print_mame_info( stdout, drivers );
	/*TODO*///				return 0;
		}
	
		if (verify != 0)  /* "verify" utilities */
		{
			int err = 0;
			int correct = 0;
			int incorrect = 0;
			int res = 0;
			int total = 0;
			int checked = 0;
			int notfound = 0;
	
	
	/*TODO*///			for (i = 0; drivers[i]; i++)
	/*TODO*///			{
	/*TODO*///				if (!strwildcmp(gamename, drivers[i]->name))
	/*TODO*///					total++;
	/*TODO*///			}
	/*TODO*///	
	/*TODO*///			for (i = 0; drivers[i]; i++)
	/*TODO*///			{
	/*TODO*///				if (strwildcmp(gamename, drivers[i]->name))
	/*TODO*///					continue;
	/*TODO*///	
	/*TODO*///				/* set rom and sample path correctly */
	/*TODO*///				get_rom_sample_path (argc, argv, i, NULL);
	/*TODO*///	
	/*TODO*///				if (verify & VERIFY_ROMS)
	/*TODO*///				{
	/*TODO*///					res = VerifyRomSet (i,(verify & VERIFY_TERSE) ? terse_printf : (verify_printf_proc)printf);
	/*TODO*///	
	/*TODO*///					if (res == CLONE_NOTFOUND || res == NOTFOUND)
	/*TODO*///					{
	/*TODO*///						notfound++;
	/*TODO*///						goto nextloop;
	/*TODO*///					}
	/*TODO*///	
	/*TODO*///					if (res == INCORRECT || res == BEST_AVAILABLE || (verify & VERIFY_VERBOSE))
	/*TODO*///					{
	/*TODO*///						printf ("romset %s ", drivers[i]->name);
	/*TODO*///						if (drivers[i]->clone_of && !(drivers[i]->clone_of->flags & NOT_A_DRIVER))
	/*TODO*///							printf ("[%s] ", drivers[i]->clone_of->name);
	/*TODO*///					}
	/*TODO*///				}
	/*TODO*///				if (verify & VERIFY_SAMPLES)
	/*TODO*///				{
	/*TODO*///					const char **samplenames = NULL;
	/*TODO*///	#if (HAS_SAMPLES || HAS_VLM5030)
	/*TODO*///	 				for( j = 0; drivers[i]->drv->sound[j].sound_type && j < MAX_SOUND; j++ )
	/*TODO*///					{
	/*TODO*///	#if (HAS_SAMPLES)
	/*TODO*///	 					if( drivers[i]->drv->sound[j].sound_type == SOUND_SAMPLES )
	/*TODO*///	 						samplenames = ((struct Samplesinterface *)drivers[i]->drv->sound[j].sound_interface)->samplenames;
	/*TODO*///	#endif
	/*TODO*///	#if (HAS_VLM5030)
	/*TODO*///						if( drivers[i]->drv->sound[j].sound_type == SOUND_VLM5030 )
	/*TODO*///							samplenames = ((struct VLM5030interface *)drivers[i]->drv->sound[j].sound_interface)->samplenames;
	/*TODO*///	#endif
	/*TODO*///					}
	/*TODO*///	#endif
	/*TODO*///					/* ignore games that need no samples */
	/*TODO*///					if (samplenames == 0 || samplenames[0] == 0)
	/*TODO*///						goto nextloop;
	/*TODO*///	
	/*TODO*///					res = VerifySampleSet (i,(verify_printf_proc)printf);
	/*TODO*///					if (res == NOTFOUND)
	/*TODO*///					{
	/*TODO*///						notfound++;
	/*TODO*///						goto nextloop;
	/*TODO*///					}
	/*TODO*///					printf ("sampleset %s ", drivers[i]->name);
	/*TODO*///				}
	/*TODO*///	
	/*TODO*///				if (res == NOTFOUND)
	/*TODO*///				{
	/*TODO*///					printf ("oops, should never come along here\n");
	/*TODO*///				}
	/*TODO*///				else if (res == INCORRECT)
	/*TODO*///				{
	/*TODO*///					printf ("is bad\n");
	/*TODO*///					incorrect++;
	/*TODO*///				}
	/*TODO*///				else if (res == CORRECT)
	/*TODO*///				{
	/*TODO*///					if (verify & VERIFY_VERBOSE)
	/*TODO*///						printf ("is good\n");
	/*TODO*///					correct++;
	/*TODO*///				}
	/*TODO*///				else if (res == BEST_AVAILABLE)
	/*TODO*///				{
	/*TODO*///					printf ("is best available\n");
	/*TODO*///					correct++;
	/*TODO*///				}
	/*TODO*///				if (res)
	/*TODO*///					err = res;
	/*TODO*///	
	/*TODO*///	nextloop:
	/*TODO*///				checked++;
	/*TODO*///				fprintf(stderr,"%d%%\r",100 * checked / total);
	/*TODO*///			}
	/*TODO*///	
	/*TODO*///			if (correct+incorrect == 0)
	/*TODO*///			{
	/*TODO*///				printf ("%s ", (verify & VERIFY_ROMS) ? "romset" : "sampleset" );
	/*TODO*///				if (notfound > 0)
	/*TODO*///					printf("\"%8s\" not found!\n",gamename);
	/*TODO*///				else
	/*TODO*///					printf("\"%8s\" not supported!\n",gamename);
	/*TODO*///				return 1;
	/*TODO*///			}
	/*TODO*///			else
	/*TODO*///			{
	/*TODO*///				printf("%d %s found, %d were OK.\n", correct+incorrect,
	/*TODO*///						(verify & VERIFY_ROMS)? "romsets" : "samplesets", correct);
	/*TODO*///				if (incorrect > 0)
	/*TODO*///					return 2;
	/*TODO*///				else
	/*TODO*///					return 0;
	/*TODO*///			}
			}
			if (ident != 0)
			{
				if (ident == 2) silentident = 1;
				else silentident = 0;
		
				for (i = 1;i < argc;i++)
				{
					/* find the FIRST "name" field (without '-') */
					if (argv[i].charAt(0) != '-')
					{
						knownstatus = KNOWN_START;
/*TODO*///				romident(argv[i],1);
						if (ident == 2)
						{
							switch (knownstatus)
							{
								case KNOWN_START: printf("ERROR     %s\n",argv[i]); break;
								case KNOWN_ALL:   printf("KNOWN     %s\n",argv[i]); break;
								case KNOWN_NONE:  printf("UNKNOWN   %s\n",argv[i]); break;
								case KNOWN_SOME:  printf("PARTKNOWN %s\n",argv[i]); break;
							}
						}
						break;
					}
				}
				return 0;
			}
		
			/* use a special return value if no frontend function used */
	    	
			return 1234;
		}
}
