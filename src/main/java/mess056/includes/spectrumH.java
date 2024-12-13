package mess056.includes;

public class spectrumH {
    /*TODO*///extern int  spectrum_snap_load(int id);
    /*TODO*///extern void spectrum_snap_exit(int id);

    /*TODO*///extern 
    public static enum TIMEX_CART_TYPE
    {
            TIMEX_CART_NONE,
            TIMEX_CART_DOCK,
            TIMEX_CART_EXROM,
            TIMEX_CART_HOME
    };

    /*TODO*///extern TIMEX_CART_TYPE timex_cart_type;
    /*TODO*///extern UINT8 timex_cart_chunks;
    /*TODO*///extern UINT8 * timex_cart_data;
    /*TODO*///extern int  spectrum_cart_load(int id);
    /*TODO*///extern void spectrum_cart_exit(int id);

    /*TODO*///extern int  timex_cart_load(int id);
    /*TODO*///extern void timex_cart_exit(int id);
    /*TODO*///extern 
    /*TODO*///extern extern 
    /*TODO*///extern int  spec_quick_init (int id);
    /*TODO*///extern void spec_quick_exit (int id);
    /*TODO*///extern int  spec_quick_open (int id, int mode, void *arg);

    /*-----------------27/02/00 10:49-------------------
     code for WAV reading writing
    --------------------------------------------------*/
    /*TODO*///extern int spectrum_cassette_init(int);
    /*TODO*///extern void spectrum_cassette_exit(int);

    /*TODO*///extern int spectrum_128_port_7ffd_data;
    /*TODO*///extern int spectrum_plus3_port_1ffd_data;
    /*TODO*///extern int ts2068_port_ff_data;
    /*TODO*///extern int ts2068_port_f4_data;
    /*TODO*///extern int PreviousFE;
    /*TODO*///extern UBytePtr spectrum_128_screen_location;
    /*TODO*///extern UBytePtr ts2068_ram;


    /*TODO*///extern extern 

    /*TODO*///extern extern extern void spectrum_vh_screenrefresh(struct mame_bitmap *bitmap, int full_refresh);
    /*TODO*///extern 
    /*TODO*///extern extern extern void spectrum_128_vh_screenrefresh(struct mame_bitmap *bitmap, int full_refresh);

    /*TODO*///extern extern void ts2068_vh_screenrefresh(struct mame_bitmap *bitmap, int full_refresh);

    /*TODO*///extern void tc2048_vh_screenrefresh(struct mame_bitmap *bitmap, int full_refresh);

    /*TODO*///extern WRITE_HANDLER ( spectrum_characterram_w );
    /*TODO*///extern READ_HANDLER  ( spectrum_characterram_r );
    /*TODO*///extern WRITE_HANDLER ( spectrum_colorram_w );
    /*TODO*///extern READ_HANDLER  ( spectrum_colorram_r );

    /* Spectrum screen size in pixels */
    public static final int SPEC_UNSEEN_LINES = 16;   /* Non-visible scanlines before first border
                                       line. Some of these may be vertical retrace. */
    public static final int SPEC_TOP_BORDER = 48;   /* Number of border lines before actual screen */
    public static final int SPEC_DISPLAY_YSIZE = 192;  /* Vertical screen resolution */
    public static final int SPEC_BOTTOM_BORDER = 56;   /* Number of border lines at bottom of screen */
    public static final int SPEC_SCREEN_HEIGHT = (SPEC_TOP_BORDER + SPEC_DISPLAY_YSIZE + SPEC_BOTTOM_BORDER);

    public static final int SPEC_LEFT_BORDER = 48;   /* Number of left hand border pixels */
    public static final int SPEC_DISPLAY_XSIZE = 256;  /* Horizontal screen resolution */
    public static final int SPEC_RIGHT_BORDER = 48;   /* Number of right hand border pixels */
    public static final int SPEC_SCREEN_WIDTH = (SPEC_LEFT_BORDER + SPEC_DISPLAY_XSIZE + SPEC_RIGHT_BORDER);

    public static final int SPEC_LEFT_BORDER_CYCLES = 24;   /* Cycles to display left hand border */
    public static final int SPEC_DISPLAY_XSIZE_CYCLES = 128;  /* Horizontal screen resolution */
    public static final int SPEC_RIGHT_BORDER_CYCLES = 24;   /* Cycles to display right hand border */
    public static final int SPEC_RETRACE_CYCLES = 48;   /* Cycles taken for horizonal retrace */
    public static final int SPEC_CYCLES_PER_LINE = 224;  /* Number of cycles to display a single line */

    /* 128K machines take an extra 4 cycles per scan line - add this to retrace */
    public static final int SPEC128_UNSEEN_LINES = 15;
    public static final int SPEC128_RETRACE_CYCLES = 52;
    public static final int SPEC128_CYCLES_PER_LINE = 228;

    /* Border sizes for TS2068. These are guesses based on the number of cycles
       available per frame. */
    public static final int TS2068_TOP_BORDER = 32;
    public static final int TS2068_BOTTOM_BORDER = 32;
    public static final int TS2068_SCREEN_HEIGHT = (TS2068_TOP_BORDER + SPEC_DISPLAY_YSIZE + TS2068_BOTTOM_BORDER);

    /* Double the border sizes to maintain ratio of screen to border */
    public static final int TS2068_LEFT_BORDER = 96;   /* Number of left hand border pixels */
    public static final int TS2068_DISPLAY_XSIZE = 512;  /* Horizontal screen resolution */
    public static final int TS2068_RIGHT_BORDER = 96;   /* Number of right hand border pixels */
    public static final int TS2068_SCREEN_WIDTH = (TS2068_LEFT_BORDER + TS2068_DISPLAY_XSIZE + TS2068_RIGHT_BORDER);
}