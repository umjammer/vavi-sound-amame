/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056;

import static arcadeflex056.settings.current_platform_configuration;
import static arcadeflex056.settings.installationDir;
import static common.libc.cstring.*;
import java.io.File;
import java.util.StringTokenizer;
import static mame056.commonH.*;
import static mame056.inptportH.*;
import static mame056.input.*;
import static mame056.inputH.*;
import static mame056.mame.*;
import static mame056.usrintrf.*;
import static mame056.usrintrfH.*;
import static mess056.deviceH.*;
import static mess056.mess.*;
import static mess056.messH.*;
import static mess056.utils.*;

public class filemngr
{
	static String szCurrentDirectory = null;
        
	static int count_chars_entered;
	static String enter_string;
	static int enter_string_size;
	static int enter_filename_mode;
	
	public static String entered_filename;
	
	public static void start_enter_string(String string_buffer, int max_string_size, int filename_mode)
	{
		enter_string = string_buffer;
		count_chars_entered = strlen(string_buffer);
		enter_string_size = max_string_size;
		enter_filename_mode = filename_mode;
	}
	
	
	/* code, lower case (w/o shift), upper case (with shift), control */
	static char code_to_char_table[] =
	{
		KEYCODE_0, '0', ')', 0,
		KEYCODE_1, '1', '!', 0,
		KEYCODE_2, '2', '"', 0,
		KEYCODE_3, '3', '#', 0,
		KEYCODE_4, '4', '$', 0,
		KEYCODE_5, '5', '%', 0,
		KEYCODE_6, '6', '^', 0,
		KEYCODE_7, '7', '&', 0,
		KEYCODE_8, '8', '*', 0,
		KEYCODE_9, '9', '(', 0,
		KEYCODE_A, 'a', 'A', 1,
		KEYCODE_B, 'b', 'B', 2,
		KEYCODE_C, 'c', 'C', 3,
		KEYCODE_D, 'd', 'D', 4,
		KEYCODE_E, 'e', 'E', 5,
		KEYCODE_F, 'f', 'F', 6,
		KEYCODE_G, 'g', 'G', 7,
		KEYCODE_H, 'h', 'H', 8,
		KEYCODE_I, 'i', 'I', 9,
		KEYCODE_J, 'j', 'J', 10,
		KEYCODE_K, 'k', 'K', 11,
		KEYCODE_L, 'l', 'L', 12,
		KEYCODE_M, 'm', 'M', 13,
		KEYCODE_N, 'n', 'N', 14,
		KEYCODE_O, 'o', 'O', 15,
		KEYCODE_P, 'p', 'P', 16,
		KEYCODE_Q, 'q', 'Q', 17,
		KEYCODE_R, 'r', 'R', 18,
		KEYCODE_S, 's', 'S', 19,
		KEYCODE_T, 't', 'T', 20,
		KEYCODE_U, 'u', 'U', 21,
		KEYCODE_V, 'v', 'V', 22,
		KEYCODE_W, 'w', 'W', 23,
		KEYCODE_X, 'x', 'X', 24,
		KEYCODE_Y, 'y', 'Y', 25,
		KEYCODE_Z, 'z', 'Z', 26,
		KEYCODE_OPENBRACE, '[', '{', 27,
		KEYCODE_BACKSLASH, '\\', '|', 28,
		KEYCODE_CLOSEBRACE, ']', '}', 29,
		KEYCODE_TILDE, '^', '~', 30,
		KEYCODE_BACKSPACE, 127, 127, 31,
		KEYCODE_COLON, ':', ';', 0,
		KEYCODE_EQUALS, '=', '+', 0,
		KEYCODE_MINUS, '-', '_', 0,
		KEYCODE_STOP, '.', '<', 0,
		KEYCODE_COMMA, ',', '>', 0,
		KEYCODE_SLASH, '/', '?', 0,
		KEYCODE_ENTER, 13, 13, 13,
		KEYCODE_ESC, 27, 27, 27
	};
	
	/*
	 * For now I use a lookup table for valid filename characters.
	 * Maybe change this for different platforms?
	 * Put it to osd_cpu? Make it an osd_... function?
	 */
	static char valid_filename_char[] =
	{
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 	/* 00-0f */
		0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 	/* 10-1f */
		1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 1, 1, 0, 	/*	!"#$%&'()*+,-./ */
		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 	/* 0123456789:;<=>? */
		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 	/* @ABCDEFGHIJKLMNO */
		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 	/* PQRSTUVWXYZ[\]^_ */
		0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 	/* `abcdefghijklmno */
		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 0, 	/* pqrstuvwxyz{|}~	*/
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 	/* 80-8f */
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 	/* 90-9f */
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 	/* a0-af */
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 	/* b0-bf */
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 	/* c0-cf */
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 	/* d0-df */
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 	/* e0-ef */
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0		/* f0-ff */
	};
	
	public static int code_to_ascii(int code)
	{
		int i;
                int _longo = code_to_char_table.length / 4;
	
		for (i = 0; i < _longo; i++)
	
		{
			if (code_to_char_table[i * 4] == code)
			{
				if (keyboard_pressed(KEYCODE_LCONTROL)!=0 ||
					keyboard_pressed(KEYCODE_RCONTROL)!=0 )
					return code_to_char_table[i * 4 + 3];
				if (keyboard_pressed(KEYCODE_LSHIFT)!=0 ||
					keyboard_pressed(KEYCODE_RSHIFT)!=0 )
					return code_to_char_table[i * 4 + 2];
				return code_to_char_table[i * 4 + 1];
			}
		}
	
		return -1;
	}
	
	public static String update_entered_string()
	{
                //System.out.println("update_entered_string");
		int code;
		int ascii_char;
                
                char[] _enter_string;
	
		/* get key */
		code = keyboard_read_async();
	
		/* key was pressed? */
		if (code == CODE_NONE)
			return null;
	
		ascii_char = code_to_ascii(code);
	
		switch (ascii_char)
		{
			/* char could not be converted to ascii */
		case -1:
			return null;
	
		case 13:	/* Return */
			return enter_string;
	
		case 25:	/* Ctrl-Y (clear line) */
			count_chars_entered = 0;
			//enter_string[count_chars_entered] = '\0';
                        _enter_string = enter_string.toCharArray();
                        _enter_string[count_chars_entered] = '\0';
                        enter_string = String.valueOf(_enter_string);
			break;
	
		case 27:	/* Escape */
			return null;
	
			/* delete */
		case 127:
			count_chars_entered--;
			if (count_chars_entered < 0)
				count_chars_entered = 0;
			//enter_string[count_chars_entered] = '\0';
                        _enter_string = enter_string.toCharArray();
                        _enter_string[count_chars_entered] = '\0';
                        enter_string = String.valueOf(_enter_string);
			break;
	
			/* got a char - add to string */
		default:
			if (count_chars_entered < enter_string_size)
			{
				if ((enter_filename_mode!=0 && valid_filename_char[ascii_char] != 0) ||
					enter_filename_mode==0)
				{
					/* store char */
					//enter_string[count_chars_entered] = ascii_char;
                                        _enter_string = enter_string.toCharArray();
                                        _enter_string[count_chars_entered] = (char) ascii_char;
                                        enter_string = String.valueOf(_enter_string);
					/* update count of chars entered */
					count_chars_entered++;
					/* add null to end of string */
					//enter_string[count_chars_entered] = '\0';
                                        _enter_string = enter_string.toCharArray();
                                        _enter_string[count_chars_entered] = '\0';
                                        enter_string = String.valueOf(_enter_string);
				}
			}
			break;
		}
	
		return null;
	}
	
	
	static String current_filespecification = "*.*";
/*TODO*///	const char fs_directory[] = "[DIR]";
/*TODO*///	const char fs_device[] = "[DRIVE]";
/*TODO*///	const char fs_file[] = "[FILE]";
/*TODO*///	/*const char fs_archive[] = "[ARCHIVE]"; */
	
	static String[] fs_item;
	static String[] fs_subitem;
	static char[] fs_flags;
        static int[] fs_types;
	static int[] fs_order;
	static int fs_chunk;
	static int fs_total;
	
/*TODO*///	enum {
        public static final int		FILESELECT_NONE = 0;
        public static final int		FILESELECT_QUIT = 1;
        public static final int         FILESELECT_FILESPEC = 2;
        public static final int		FILESELECT_DEVICE = 3;
        public static final int		FILESELECT_DIRECTORY = 4;
        public static final int		FILESELECT_FILE = 5;
/*TODO*///	} FILESELECT_ENTRY_TYPE;
/*TODO*///	
/*TODO*///	
/*TODO*///	char *fs_dupe(const char *src, int len)
/*TODO*///	{
/*TODO*///		char *dst;
/*TODO*///		int display_length;
/*TODO*///		int display_width;
/*TODO*///	
/*TODO*///		display_width = (Machine->uiwidth / Machine->uifontwidth);
/*TODO*///		display_length = len;
/*TODO*///	
/*TODO*///		if (display_length>display_width)
/*TODO*///			display_length = display_width;
/*TODO*///	
/*TODO*///		/* malloc space for string + NULL char + extra char.*/
/*TODO*///		dst = malloc(len+2);
/*TODO*///		if (dst)
/*TODO*///		{
/*TODO*///			strcpy(dst, src);
/*TODO*///			/* copy old char to end of string */
/*TODO*///			dst[len+1]=dst[display_length];
/*TODO*///			/* put in NULL to cut string. */
/*TODO*///			dst[len+1]='\0';
/*TODO*///		}
/*TODO*///		return dst;
/*TODO*///	
/*TODO*///	}
/*TODO*///	
/*TODO*///	
	public static void fs_free()
	{
		if (fs_chunk > 0)
		{
			int i;
			/* free duplicated strings of file and directory names */
			for (i = 0; i < fs_total; i++)
			{
				if (fs_types[i] == FILESELECT_FILE ||
					fs_types[i] == FILESELECT_DIRECTORY)
					fs_item[i] = null;
			}
			fs_item = null;
			fs_subitem = null;
			fs_flags = null;
			fs_types = null;
			fs_order = null;
			fs_chunk = 0;
			fs_total = 0;
		}
	}
	
/*TODO*///	int fs_alloc(void)
/*TODO*///	{
/*TODO*///		if (fs_total < fs_chunk)
/*TODO*///		{
/*TODO*///			fs_order[fs_total] = fs_total;
/*TODO*///			return (fs_total += 1) - 1;
/*TODO*///		}
/*TODO*///		if (fs_chunk)
/*TODO*///		{
/*TODO*///			fs_chunk += 256;
/*TODO*///			logerror("fs_alloc() next chunk (total %d)\n", fs_chunk);
/*TODO*///			fs_item = realloc(fs_item, fs_chunk * sizeof(char **));
/*TODO*///			fs_subitem = realloc(fs_subitem, fs_chunk * sizeof(char **));
/*TODO*///			fs_flags = realloc(fs_flags, fs_chunk * sizeof(char *));
/*TODO*///			fs_types = realloc(fs_types, fs_chunk * sizeof(int));
/*TODO*///			fs_order = realloc(fs_order, fs_chunk * sizeof(int));
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			fs_chunk = 512;
/*TODO*///			logerror("fs_alloc() first chunk %d\n", fs_chunk);
/*TODO*///			fs_item = malloc(fs_chunk * sizeof(char **));
/*TODO*///			fs_subitem = malloc(fs_chunk * sizeof(char **));
/*TODO*///			fs_flags = malloc(fs_chunk * sizeof(char *));
/*TODO*///			fs_types = malloc(fs_chunk * sizeof(int));
/*TODO*///			fs_order = malloc(fs_chunk * sizeof(int));
/*TODO*///		}
/*TODO*///	
/*TODO*///		/* what do we do if reallocation fails? raise(SIGABRT) seems a way outa here */
/*TODO*///		if (!fs_item || !fs_subitem || !fs_flags || !fs_types || !fs_order)
/*TODO*///		{
/*TODO*///			logerror("failed to allocate fileselect buffers!\n");
/*TODO*///			raise(SIGABRT);
/*TODO*///		}
/*TODO*///	
/*TODO*///		fs_order[fs_total] = fs_total;
/*TODO*///		return (fs_total += 1) - 1;
/*TODO*///	}
/*TODO*///	
/*TODO*///	static int DECL_SPEC fs_compare(const void *p1, const void *p2)
/*TODO*///	{
/*TODO*///		const int i1 = *(int *)p1;
/*TODO*///		const int i2 = *(int *)p2;
/*TODO*///	
/*TODO*///		if (fs_types[i1] != fs_types[i2])
/*TODO*///			return fs_types[i1] - fs_types[i2];
/*TODO*///		return strcmp(fs_item[i1], fs_item[i2]);
/*TODO*///	
/*TODO*///	}
/*TODO*///	
/*TODO*///	#define MAX_ENTRIES_IN_MENU (SEL_MASK-1)
/*TODO*///	
/*TODO*///	int fs_init_done = 0;
/*TODO*///	
	public static void fs_generate_filelist()
    	{
/*TODO*///		void *dir;
/*TODO*///		int qsort_start, count, i, n;
/*TODO*///		const char **tmp_menu_item;
/*TODO*///		const char **tmp_menu_subitem;
/*TODO*///		char *tmp_flags;
/*TODO*///		int *tmp_types;
/*TODO*///	
/*TODO*///		/* should be moved inside mess.c ??? */
/*TODO*///		if (fs_init_done==0)
/*TODO*///		{
/*TODO*///			/* this will not work if roms is not a sub-dir of mess, and
/*TODO*///			   will also not work if we are not in the mess dir */
/*TODO*///			/* go to initial roms directory */
/*TODO*///			osd_change_directory("software");
/*TODO*///			osd_change_directory(Machine->gamedrv->name);
/*TODO*///			fs_init_done = 1;
/*TODO*///		}
/*TODO*///	
/*TODO*///		/* just to be safe */
/*TODO*///		fs_free();
/*TODO*///	
/*TODO*///		/* quit back to main menu option at top */
/*TODO*///		n = fs_alloc();
/*TODO*///		fs_item[n] = "Quit Fileselector";
/*TODO*///		fs_subitem[n] = 0;
/*TODO*///		fs_types[n] = FILESELECT_QUIT;
/*TODO*///		fs_flags[n] = 0;
/*TODO*///	
/*TODO*///		/* insert blank line */
/*TODO*///		n = fs_alloc();
/*TODO*///		fs_item[n] = "-";
/*TODO*///		fs_subitem[n] = 0;
/*TODO*///		fs_types[n] = FILESELECT_NONE;
/*TODO*///		fs_flags[n] = 0;
/*TODO*///	
/*TODO*///		/* current directory */
/*TODO*///		n = fs_alloc();
/*TODO*///		fs_item[n] = osd_get_cwd();
/*TODO*///		fs_subitem[n] = 0;
/*TODO*///		fs_types[n] = FILESELECT_NONE;
/*TODO*///		fs_flags[n] = 0;
/*TODO*///	
/*TODO*///		/* blank line */
/*TODO*///		n = fs_alloc();
/*TODO*///		fs_item[n] = "-";
/*TODO*///		fs_subitem[n] = 0;
/*TODO*///		fs_types[n] = FILESELECT_NONE;
/*TODO*///		fs_flags[n] = 0;
/*TODO*///	
/*TODO*///	
/*TODO*///		/* file specification */
/*TODO*///		n = fs_alloc();
/*TODO*///		fs_item[n] = "File Specification";
/*TODO*///		fs_subitem[n] = current_filespecification;
/*TODO*///		fs_types[n] = FILESELECT_FILESPEC;
/*TODO*///		fs_flags[n] = 0;
/*TODO*///	
/*TODO*///		/* insert blank line */
/*TODO*///		n = fs_alloc();
/*TODO*///		fs_item[n] = "-";
/*TODO*///		fs_subitem[n] = 0;
/*TODO*///		fs_types[n] = FILESELECT_NONE;
/*TODO*///		fs_flags[n] = 0;
/*TODO*///	
/*TODO*///		qsort_start = fs_total;
/*TODO*///	
/*TODO*///		/* devices first */
/*TODO*///		count = osd_num_devices();
/*TODO*///		if (count > 0)
/*TODO*///		{
/*TODO*///			logerror("fs_generate_filelist: %d devices\n", count);
/*TODO*///			for (i = 0; i < count; i++)
/*TODO*///			{
/*TODO*///				if (fs_total >= MAX_ENTRIES_IN_MENU)
/*TODO*///					break;
/*TODO*///				n = fs_alloc();
/*TODO*///				fs_item[n] = osd_get_device_name(i);
/*TODO*///				fs_subitem[n] = fs_device;
/*TODO*///				fs_types[n] = FILESELECT_DEVICE;
/*TODO*///				fs_flags[n] = 0;
/*TODO*///			}
/*TODO*///		}
/*TODO*///	
/*TODO*///		/* directory entries */
/*TODO*///		dir = osd_dir_open(".", current_filespecification);
/*TODO*///		if (dir)
/*TODO*///		{
/*TODO*///			int len, filetype;
/*TODO*///			char filename[260];
/*TODO*///			len = osd_dir_get_entry(dir, filename, sizeof(filename), &filetype);
/*TODO*///			while (len > 0)
/*TODO*///			{
/*TODO*///				if (fs_total >= MAX_ENTRIES_IN_MENU)
/*TODO*///					break;
/*TODO*///				n = fs_alloc();
/*TODO*///				fs_item[n] = fs_dupe(filename,len);
/*TODO*///				if (filetype)
/*TODO*///				{
/*TODO*///					fs_types[n] = FILESELECT_DIRECTORY;
/*TODO*///					fs_subitem[n] = fs_directory;
/*TODO*///				}
/*TODO*///				else
/*TODO*///				{
/*TODO*///					fs_types[n] = FILESELECT_FILE;
/*TODO*///					fs_subitem[n] = fs_file;
/*TODO*///				}
/*TODO*///				fs_flags[n] = 0;
/*TODO*///				len = osd_dir_get_entry(dir, filename, sizeof(filename), &filetype);
/*TODO*///			}
/*TODO*///			osd_dir_close(dir);
/*TODO*///		}
/*TODO*///	
/*TODO*///		n = fs_alloc();
/*TODO*///		fs_item[n] = 0; 		 /* terminate array */
/*TODO*///		fs_subitem[n] = 0;
/*TODO*///		fs_types[n] = FILESELECT_NONE;
/*TODO*///		fs_flags[n] = 0;
/*TODO*///	
/*TODO*///		logerror("fs_generate_filelist: sorting %d entries\n", n - qsort_start);
/*TODO*///		qsort(&fs_order[qsort_start], n - qsort_start, sizeof(int), fs_compare);
/*TODO*///	
/*TODO*///		tmp_menu_item = malloc(n * sizeof(char *));
/*TODO*///		tmp_menu_subitem = malloc(n * sizeof(char *));
/*TODO*///		tmp_flags = malloc(n * sizeof(char));
/*TODO*///		tmp_types = malloc(n * sizeof(int));
/*TODO*///	
/*TODO*///		/* no space to sort? have to leave now... */
/*TODO*///		if (!tmp_menu_item || !tmp_menu_subitem || !tmp_flags || !tmp_types )
/*TODO*///			return;
/*TODO*///	
/*TODO*///		/* copy items in original order */
/*TODO*///		memcpy(tmp_menu_item, fs_item, n * sizeof(char *));
/*TODO*///		memcpy(tmp_menu_subitem, fs_subitem, n * sizeof(char *));
/*TODO*///		memcpy(tmp_flags, fs_flags, n * sizeof(char));
/*TODO*///		memcpy(tmp_types, fs_types, n * sizeof(int));
/*TODO*///	
/*TODO*///		for (i = qsort_start; i < n; i++)
/*TODO*///		{
/*TODO*///			int j = fs_order[i];
/*TODO*///			fs_item[i] = tmp_menu_item[j];
/*TODO*///			fs_subitem[i] = tmp_menu_subitem[j];
/*TODO*///			fs_flags[i] = tmp_flags[j];
/*TODO*///			fs_types[i] = tmp_types[j];
/*TODO*///		}
/*TODO*///	
/*TODO*///		free(tmp_menu_item);
/*TODO*///		free(tmp_menu_subitem);
/*TODO*///		free(tmp_flags);
/*TODO*///		free(tmp_types);
	}
	
        public static int UI_SHIFT_PRESSED(){
            return (keyboard_pressed(KEYCODE_LSHIFT)!=0 || keyboard_pressed(KEYCODE_RSHIFT)!=0) ? 1 : 0;
        }
        
        public static int UI_CONTROL_PRESSED(){
            return (keyboard_pressed(KEYCODE_LCONTROL)!=0 || keyboard_pressed(KEYCODE_RCONTROL)!=0) ? 1 : 0;
        }
        
        /* and mask to get bits */
        public static int SEL_BITS_MASK() {
            return (~SEL_MASK);
        }

	public static int fileselect(mame_bitmap bitmap, int selected)
	{
		int sel, total, arrowize;
		int visible;
	
		sel = selected - 1;
	
		/* generate menu? */
		if (fs_total == 0)
			fs_generate_filelist();
	
		total = fs_total - 1;
	
		if (total > 0)
		{
			/* make sure it is in range - might go out of range if
			 * we were stepping up and down directories */
			if ((sel & SEL_MASK) >= total)
				sel = (sel & SEL_BITS_MASK()) | (total - 1);
	
			arrowize = 0;
			if (sel < total)
			{
				switch (fs_types[sel])
				{
					/* arrow pointing inwards (arrowize = 1) */
					case FILESELECT_QUIT:
					case FILESELECT_FILE:
						break;
	
					case FILESELECT_FILESPEC:
					case FILESELECT_DIRECTORY:
					case FILESELECT_DEVICE:
						/* arrow pointing to right -
						 * indicating more available if
						 * selected, or editable */
						arrowize = 2;
						break;
				}
			}
	
			if ((sel & (1 << SEL_BITS)) != 0)	/* are we waiting for a new key? */
			{
				String name;
	
				/* change menu item to show this filename */
				fs_subitem[sel & SEL_MASK] = current_filespecification;
	
				/* display the menu */
				ui_displaymenu(bitmap, fs_item, fs_subitem, fs_flags, sel & SEL_MASK, 3);
	
				/* update string with any keys that are pressed */
				name = update_entered_string();
	
				/* finished entering filename? */
				if (name != null)
				{
					/* yes */
					sel &= SEL_MASK;
	
					/* if no name entered - go back to default all selection */
					if (strlen(name) == 0)
						current_filespecification = "*";
					else
						current_filespecification = name;
					fs_free();
				}
	
				schedule_full_refresh();
				return sel + 1;
			}
	
	
			ui_displaymenu(bitmap, fs_item, fs_subitem, fs_flags, sel, arrowize);
	
			/* borrowed from usrintrf.c */
			visible = Machine.uiheight / (3 * Machine.uifontheight /2) -1;
	
			if (input_ui_pressed_repeat(IPT_UI_DOWN, 8) != 0)
			{
				if (UI_CONTROL_PRESSED() != 0)
				{
					sel = total - 1;
				}
				else
				if (UI_SHIFT_PRESSED() != 0)
				{
					sel = (sel + visible) % total;
				}
				else
				{
					sel = (sel + 1) % total;
				}
			}
	
			if (input_ui_pressed_repeat(IPT_UI_UP, 8) != 0)
			{
				if (UI_CONTROL_PRESSED() != 0)
				{
					sel = 0;
				}
				if (UI_SHIFT_PRESSED() != 0)
				{
					sel = (sel + total - visible) % total;
				}
				else
				{
					sel = (sel + total - 1) % total;
				}
			}
	
			if (input_ui_pressed(IPT_UI_SELECT) != 0)
			{
				if (sel < SEL_MASK)
				{
					switch (fs_types[sel])
					{
					case FILESELECT_QUIT:
						sel = -1;
						break;
	
					case FILESELECT_FILESPEC:
						start_enter_string(current_filespecification, 32, 0);
						sel |= 1 << SEL_BITS; /* we'll ask for a key */
						schedule_full_refresh();
						break;
	
					case FILESELECT_FILE:
						/* copy filename */
						strncpyz(entered_filename, osd_get_cwd(), entered_filename.length());
						strncatz(entered_filename, fs_item[sel], entered_filename.length());
	
						fs_free();
						sel = -3;
						break;
	
					case FILESELECT_DIRECTORY:
						/*	fs_chdir(fs_item[sel]); */
						osd_change_directory(fs_item[sel]);
						fs_free();
	
						schedule_full_refresh();
						break;
	
					case FILESELECT_DEVICE:
						/*	 fs_chdir("/"); */
						osd_change_device(fs_item[sel]);
						fs_free();
						schedule_full_refresh();
						break;
	
					default:
						break;
					}
				}
			}
	
			if (input_ui_pressed(IPT_UI_CANCEL) != 0)
				sel = -1;
	
			if (input_ui_pressed(IPT_UI_CONFIGURE) != 0)
				sel = -2;
		}
		else
		{
			sel = -1;
		}
	
		if (sel == -1 || sel == -3)
			fs_free();
	
		if (sel == -1 || sel == -2 || sel == -3)
			schedule_full_refresh();
	
		return sel + 1;
	}
        
        public static int osd_select_file(int sel, String filename)
	{
                //javax.swing.JFileChooser jf = new JFileChooser();
                current_platform_configuration.get_filemngr_class().setCurrentDirectory(new File(osd_get_cwd()));
                //jf.setCurrentDirectory(new File(osd_get_cwd() + "/" + Machine.gamedrv.name));
                //jf.setCurrentDirectory(new File(osd_get_cwd()));
                
                // extensions
                String _supFilesStr = "Supported Files (";
                String[] _arrExtensions = getExtensionsByPosition(sel);
                
                if (_arrExtensions != null){
                    int _top = _arrExtensions.length -1;

                    for (int _i=0 ; _i<_arrExtensions.length ; _i++){
                        _supFilesStr += "*."+_arrExtensions[_i];
                        if (_i != _top)
                            _supFilesStr += ", ";
                    }

                    _supFilesStr += ")";
                }
                
                //FileFilter filter = new FileNameExtensionFilter(_supFilesStr, _arrExtensions);
                current_platform_configuration.get_filemngr_class().setFileFilter(_supFilesStr, _arrExtensions);
                //jf.addChoosableFileFilter(filter);
                
                int option = current_platform_configuration.get_filemngr_class().showOpenDialog(null);
                


                System.out.println("**********************************************************************");
                
                if (option == 0) { // selected file
					Object selected_file = current_platform_configuration.get_filemngr_class().getSelectedFile();

                    if (selected_file != null){
                        filename = ((File)selected_file).getName().toString();
                        entered_filename = filename;
                        szCurrentDirectory = current_platform_configuration.get_filemngr_class().getCurrentDirectory().getAbsolutePath();
                        System.out.println("Loading "+szCurrentDirectory+"/"+filename);
                        
                        return 1;
                    }
                } else { // not selected
                    
                }
		//if (option != jMESYSFileLoader.APPROVE_OPTION)
                    return 0;  // User canceled or clicked the dialog's close box.
	}
        
        static int previous_sel;
        
        static int[] ids = new int[40];

	public static int filemanager(mame_bitmap bitmap, int selected)
	{
		String name;
		String[] menu_item = new String[40];
		String[] menu_subitem = new String[40];
		int[] types = new int[40];
		
		char[] flag = new char[40];
	
		int sel, total, arrowize, type, id;
	
		IODevice[] dev = Machine.gamedrv.dev;
                int _dev = 0;
	
		sel = selected - 1;
	
	
		total = 0;
	
		/* Cycle through all devices for this system */
		while(dev[_dev].type != IO_END)
		{
			type = dev[_dev].type;
			for (id = 0; id < dev[_dev].count; id++)
			{
				name = device_typename_id(type, id);
	
				menu_item[total] = (name != null) ? name : "---";
	
				name = device_filename(type, id);
	
				menu_subitem[total] = (name != null) ? name : "---";
	
				flag[total] = 0;
				types[total] = type;
				ids[total] = id;
	
				total++;
	
			}
			_dev++;
		}
	
	
		/* if the fileselect() mode is active */
		if ((sel & (2 << SEL_BITS)) != 0)
		{
			sel = fileselect(bitmap, selected & ~(2 << SEL_BITS));
			if (sel != 0 && sel != -1 && sel!=-2)
				return sel | (2 << SEL_BITS);
	
			if (sel==-2)
			{
				/* selected a file */
	
				/* finish entering name */
				previous_sel = previous_sel & SEL_MASK;
	
				/* attempt a filename change */
				device_filename_change(types[previous_sel], ids[previous_sel], entered_filename);
			}
	
			sel = previous_sel;
	
			/* change menu item to show this filename */
			menu_subitem[sel & SEL_MASK] = entered_filename;
	
		}
	
		menu_item[total] = "Return to Main Menu";
		menu_subitem[total] = null;
		flag[total] = 0;
		total++;
		menu_item[total] = null;			   /* terminate array */
		menu_subitem[total] = null;
		flag[total] = 0;
	
		arrowize = 0;
		if (sel < total - 1)
			arrowize = 2;
	
		if ((sel & (1 << SEL_BITS)) != 0)	/* are we waiting for a new key? */
		{
			/* change menu item to show this filename */
			menu_subitem[sel & SEL_MASK] = entered_filename;
	
			/* display the menu */
			ui_displaymenu(bitmap, menu_item, menu_subitem, flag, sel & SEL_MASK, 3);
	
			/* update string with any keys that are pressed */
			name = update_entered_string();
	
			/* finished entering filename? */
			if (name != null)
			{
				/* yes */
				sel &= SEL_MASK;
				if (name.length() == 0)
					device_filename_change(types[sel], ids[sel], null);
				else
					device_filename_change(types[sel], ids[sel], name);
			}
	
			schedule_full_refresh();
			return sel + 1;
		}
	
		ui_displaymenu(bitmap, menu_item, menu_subitem, flag, sel, arrowize);
	
		if (input_ui_pressed_repeat(IPT_UI_DOWN, 8) != 0)
			sel = (sel + 1) % total;
	
		if (input_ui_pressed_repeat(IPT_UI_UP, 8) != 0)
			sel = (sel + total - 1) % total;
	
		if (input_ui_pressed(IPT_UI_SELECT) != 0)
		{
			int os_sel;
	
			/* Return to main menu? */
			if (sel == total-1)
			{
				sel = -1;
				os_sel = -1;
			}
			/* no, let the osd code have a crack at changing files */
			else os_sel = osd_select_file (sel, entered_filename);
                        
                        //System.out.println("File: "+entered_filename);
	
			if (os_sel != 0)
			{
				if (os_sel == 1)
				{
					/* attempt a filename change */
					device_filename_change(types[sel], ids[sel], entered_filename);
				}
			}
			/* osd code won't handle it, lets use our clunky interface */
			else if (UI_SHIFT_PRESSED() == 0)
			{
				/* save selection and switch to fileselect() */
				previous_sel = sel;
				sel = (2 << SEL_BITS);
				fs_total = 0;
			}
			else
			{
				{
					if (strcmp(menu_subitem[sel], "---") == 0){
						//entered_filename[0] = '\0';
                                                char[] _entered_filename = entered_filename.toCharArray();
                                                _entered_filename[0] = '\0';
                                                entered_filename = String.valueOf(_entered_filename);
                                        } else {
						entered_filename = menu_subitem[sel];
                                        }
					start_enter_string(entered_filename, entered_filename.length() - 1, 1);
	
					sel |= 1 << SEL_BITS;	/* we'll ask for a key */
	
					/* tell updatescreen() to clean after us (in case the window changes size) */
					schedule_full_refresh();
				}
			}
		}
	
		if (input_ui_pressed(IPT_UI_CANCEL) != 0)
			sel = -1;
	
		if (input_ui_pressed(IPT_UI_CONFIGURE) != 0)
			sel = -2;
	
		if (sel == -1 || sel == -2)
			schedule_full_refresh();
	
		return sel + 1;
	}

        // OSD METHODS (windows/dirio class)
        public static String osd_get_cwd() {
            //if (szCurrentDirectory == null)
                szCurrentDirectory = installationDir+"software/" + Machine.gamedrv.name;
            
            return szCurrentDirectory;
        }

        public static void osd_change_directory(String string) {
            System.out.println("osd_change_directory needs to be implemented!!!!");
        }
        
        public static void osd_change_device(String device)
	{
		char[] szBuffer = new char[3];
		szBuffer[0] = device.charAt(0);
		szBuffer[1] = ':';
		szBuffer[2] = '\0';
		osd_change_directory( String.valueOf(szBuffer) );
	}

        private static String[] getExtensionsByPosition(int sel) {
            IODevice[] dev = Machine.gamedrv.dev;
            int _dev = 0;
            
            String _strOut = "";
            String[] _extList=null;
            
            int _countSel = 0;
                
            while(dev[_dev].type != IO_END)
            {
                    int type = dev[_dev].type;
                    for (int id = 0; id < dev[_dev].count; id++)
                    {
                        if (_countSel == sel){
                            
                            _strOut = dev[_dev].file_extensions;
                            
                            StringTokenizer tok = new StringTokenizer(_strOut, "\0");
                            int _numExt = tok.countTokens();
                            _extList = new String[_numExt];
                            
                            for (int _i=0 ; _i<_numExt ; _i++){
                                
                                _extList[_i] = tok.nextToken();
                                
                            }
                        }
                         
                        _countSel++;

                    }
                    _dev++;
            }
            
            return _extList;
        }
	
}
