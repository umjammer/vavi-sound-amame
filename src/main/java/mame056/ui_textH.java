/**
 * ported to 0.56
 * ported to 0.37b7
 */
package mame056;

import static common.ptr.*;

public class ui_textH {

    /* Important: this must match the default_text list in ui_text.c! */
    public static final int UI_first_entry = -1;

    public static final int UI_mame = 0;

    /* copyright stuff */
    public static final int UI_copyright1 = 1;
    public static final int UI_copyright2 = 2;
    public static final int UI_copyright3 = 3;

    /* misc menu stuff */
    public static final int UI_returntomain = 4;
    public static final int UI_returntoprior = 5;
    public static final int UI_anykey = 6;
    public static final int UI_on = 7;
    public static final int UI_off = 8;
    public static final int UI_NA = 9;
    public static final int UI_OK = 10;
    public static final int UI_INVALID = 11;
    public static final int UI_none = 12;
    public static final int UI_cpu = 13;
    public static final int UI_address = 14;
    public static final int UI_value = 15;
    public static final int UI_sound = 16;
    public static final int UI_sound_lc = 17;
    /* lower-case version */
    public static final int UI_stereo = 18;
    public static final int UI_vectorgame = 19;
    public static final int UI_screenres = 20;
    public static final int UI_text = 21;
    public static final int UI_volume = 22;
    public static final int UI_relative = 23;
    public static final int UI_allchannels = 24;
    public static final int UI_brightness = 25;
    public static final int UI_gamma = 26;
    public static final int UI_vectorflicker = 27;
    public static final int UI_vectorintensity = 28;
    public static final int UI_overclock = 29;
    public static final int UI_allcpus = 30;
    public static final int UI_historymissing = 31;

    /* special characters */
    public static final int UI_leftarrow = 32;
    public static final int UI_rightarrow = 33;
    public static final int UI_uparrow = 34;
    public static final int UI_downarrow = 35;
    public static final int UI_lefthilight = 36;
    public static final int UI_righthilight = 37;

    /* warnings */
    public static final int UI_knownproblems = 38;
    public static final int UI_imperfectcolors = 39;
    public static final int UI_wrongcolors = 40;
    public static final int UI_imperfectgraphics = 41;
    public static final int UI_imperfectsound = 42;
    public static final int UI_nosound = 43;
    public static final int UI_nococktail = 44;
    public static final int UI_brokengame = 45;
    public static final int UI_brokenprotection = 46;
    public static final int UI_workingclones = 47;
    public static final int UI_typeok = 48;

    //#ifdef MESS
    public static final int UI_comp1 = 49;
    public static final int UI_comp2 = 50;
    //#endif
        
    /* main menu */
    public static final int UI_inputgeneral = 51;
    public static final int UI_dipswitches = 52;
    public static final int UI_analogcontrols = 53;
    public static final int UI_calibrate = 54;
    public static final int UI_bookkeeping = 55;
    public static final int UI_inputspecific = 56;
    public static final int UI_gameinfo = 57;
    public static final int UI_history = 58;
    public static final int UI_resetgame = 59;
    public static final int UI_returntogame = 60;
    
    //#ifdef MESS
    public static final int UI_imageinfo = 61;
    public static final int UI_filemanager = 62;
    public static final int UI_tapecontrol = 63;
    //#endif
        
    public static final int UI_cheat = 64;
    public static final int UI_memorycard = 65;

    /* input stuff */
    public static final int UI_keyjoyspeed = 66;
    public static final int UI_reverse = 67;
    public static final int UI_sensitivity = 68;

    /* stats */
    public static final int UI_tickets = 69;
    public static final int UI_coin = 70;
    public static final int UI_locked = 71;

    /* memory card */
    public static final int UI_loadcard = 72;
    public static final int UI_ejectcard = 73;
    public static final int UI_createcard = 74;
    public static final int UI_resetcard = 75;
    public static final int UI_loadfailed = 76;
    public static final int UI_loadok = 77;
    public static final int UI_cardejected = 78;
    public static final int UI_cardcreated = 79;
    public static final int UI_cardcreatedfailed = 80;
    public static final int UI_cardcreatedfailed2 = 81;
    public static final int UI_carderror = 82;

    /* cheat stuff */
    public static final int UI_enablecheat = 83;
    public static final int UI_addeditcheat = 84;
    public static final int UI_startcheat = 85;
    public static final int UI_continuesearch = 86;
    public static final int UI_viewresults = 87;
    public static final int UI_restoreresults = 88;
    public static final int UI_memorywatch = 89;
    public static final int UI_generalhelp = 90;
    public static final int UI_options = 91;
    public static final int UI_reloaddatabase = 92;
    public static final int UI_watchpoint = 93;
    public static final int UI_disabled = 94;
    public static final int UI_cheats = 95;
    public static final int UI_watchpoints = 96;
    public static final int UI_moreinfo = 97;
    public static final int UI_moreinfoheader = 98;
    public static final int UI_cheatname = 99;
    public static final int UI_cheatdescription = 100;
    public static final int UI_cheatactivationkey = 101;
    public static final int UI_code = 102;
    public static final int UI_max = 103;
    public static final int UI_set = 104;
    public static final int UI_conflict_found = 105;
    public static final int UI_no_help_available = 106;

    /* watchpoint stuff */
    public static final int UI_watchlength = 107;
    public static final int UI_watchdisplaytype = 108;
    public static final int UI_watchlabeltype = 109;
    public static final int UI_watchlabel = 110;
    public static final int UI_watchx = 111;
    public static final int UI_watchy = 112;
    public static final int UI_watch = 113;

    public static final int UI_hex = 114;
    public static final int UI_decimal = 115;
    public static final int UI_binary = 116;

    /* search stuff */
    public static final int UI_search_lives = 117;
    public static final int UI_search_timers = 118;
    public static final int UI_search_energy = 119;
    public static final int UI_search_status = 120;
    public static final int UI_search_slow = 121;
    public static final int UI_search_speed = 122;
    public static final int UI_search_speed_fast = 123;
    public static final int UI_search_speed_medium = 124;
    public static final int UI_search_speed_slow = 125;
    public static final int UI_search_speed_veryslow = 126;
    public static final int UI_search_speed_allmemory = 127;
    public static final int UI_search_select_memory_areas = 128;
    public static final int UI_search_matches_found = 129;
    public static final int UI_search_noinit = 130;
    public static final int UI_search_nosave = 131;
    public static final int UI_search_done = 132;
    public static final int UI_search_OK = 133;
    public static final int UI_search_select_value = 134;
    public static final int UI_search_all_values_saved = 135;
    public static final int UI_search_one_match_found_added = 136;

    public static final int UI_last_entry = 137;

    public static class lang_struct {

        public int version;
        public int multibyte;/* UNUSED: 1 if this is a multibyte font/language */
        public UBytePtr fontdata;/* pointer to the raw font data to be decoded */
        public char fontglyphs;/* total number of glyps in the external font - 1 */
        public String langname;
        public String fontname;
        public String author;
    }
}
