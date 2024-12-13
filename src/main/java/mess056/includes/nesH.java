/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
package mess056.includes;

import static common.ptr.*;

public class nesH {
    /* Uncomment this to see all 4 ppu vram pages at once */
    //#define BIG_SCREEN

    /* Uncomment this to do away with any form of mirroring */
    //#define NO_MIRRORING

    public static int NEW_SPRITE_HIT;

    /* Uncomment this to have proper emulation of the color intensity */
    /* bits, at the expense of speed (and wonked sprites). */
    /* NPW 31-Aug-2001 - Uncommented because this is the only way to get it to work at this point */
    public static int COLOR_INTENSITY;

    public static int BOTTOM_VISIBLE_SCANLINE   = 239;		/* The bottommost visible scanline */
    public static int NMI_SCANLINE              = 244;		/* 244 times Bayou Billy perfectly */
    public static int NTSC_SCANLINES_PER_FRAME  = 262;
    public static int PAL_SCANLINES_PER_FRAME   = 305;		/* verify - times Elite perfectly */

    //enum {
    public static int PPU_c0_inc = 0x04;
    public static int PPU_c0_spr_select = 0x08;
    public static int PPU_c0_chr_select = 0x10;
    public static int PPU_c0_sprite_size = 0x20;
    public static int PPU_c0_NMI = 0x80;

    public static int PPU_c1_background_L8 = 0x02;
    public static int PPU_c1_sprites_L8 = 0x04;
    public static int PPU_c1_background = 0x08;
    public static int PPU_c1_sprites = 0x10;

    public static int PPU_status_8sprites = 0x20;
    public static int PPU_status_sprite0_hit = 0x40;
    public static int PPU_status_vblank = 0x80;
    //};

    public static class ppu_struct {
            public int control_0;		/* $2000 */
            public int control_1;		/* $2001 */
            public int status;			/* $2002 */
            public int sprite_address;	/* $2003 */

            public int refresh_data;	/* $2005 */
            public int refresh_latch;
            public int x_fine;

            public int address; 		/* $2006 */
            public int address_latch;

            public int data_latch;		/* $2007 - read */

            public int current_scanline;
            public int[] page = new int[4];
            public int scanlines_per_frame;
    };

    public static class nes_struct {
            /* load-time cart variables which remain constant */
            public int trainer;
            public int battery;
            public char[] prg_chunks=new char[1];
            public char[] chr_chunks=new char[1];

            /* system variables which don't change at run-time */
            public int mapper;
            public int four_screen_vram;
            public int hard_mirroring;
            public int slow_banking;

            public UBytePtr rom = new UBytePtr();
            public UBytePtr vrom = new UBytePtr();
            public UBytePtr vram = new UBytePtr();
            public UBytePtr wram = new UBytePtr();

            /* Variables which can change */
            public int mid_ram_enable;
    };

    //extern struct nes_struct nes;

    public static class fds_struct {
            public UBytePtr data = new UBytePtr();
            public int  sides;

            /* Variables which can change */
            public int  motor_on;
            public int  door_closed;
            public int  current_side;
            public int  head_position;
            public int  status0;
            public int  read_mode;
            public int  write_reg;
    };

   
}
