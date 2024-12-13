/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.machine;

import static arcadeflex056.fileio.*;
import static arcadeflex056.fucPtr.*;
import static arcadeflex056.osdepend.logerror;
import static arcadeflex056.video.osd_skip_this_frame;
import static common.libc.cstdio.*;
import static common.libc.cstring.*;
import static common.ptr.*;
import static consoleflex056.funcPtr.*;
import static mame056.common.*;
import static mame056.commonH.*;
import static mame056.cpu.m6502.m6502H.*;
import static mame056.cpuexecH.*;
import static mame056.drawgfx.*;
import static mame056.inptport.*;
import static mame056.mame.*;
import static mame056.memory.*;
import static mame056.memoryH.*;
import static mame056.osdependH.*;
import static mame056.vidhrdw.generic.*;
import static mess056.deviceH.*;
import static mess056.includes.nesH.*;
import static mess056.machine.nes_mmc.*;
import static mess056.mess.*;
import static mess056.messH.*;
import static mess056.systems.nes.battery_ram;
import static mess056.systems.nes.nes_charlayout;
import static mess056.vidhrdw.nes.*;

public class nes
{
    
    public static final int M6502_INT_NONE = 0;
/*TODO*///	
/*TODO*///	/* Uncomment this to dump reams of ppu state info to the errorlog */
/*TODO*///	//#define LOG_PPU
/*TODO*///	
/*TODO*///	/* Uncomment this to dump info about the inputs to the errorlog */
/*TODO*///	//#define LOG_JOY
/*TODO*///	
/*TODO*///	/* Uncomment this to generate prg chunk files when the cart is loaded */
/*TODO*///	//#define SPLIT_PRG
/*TODO*///	
    public static final int BATTERY_SIZE = 0x2000;
    static String battery_name = "";
    public static char[] battery_data = new char[BATTERY_SIZE];
    
/*TODO*///	void nes_vh_renderscanline (int scanline);

    public static ppu_struct _ppu = new ppu_struct();
    public static nes_struct _nes = new nes_struct();
    public static fds_struct nes_fds;

    static int ppu_scanlines_per_frame;

    public static UBytePtr[] ppu_page = new UBytePtr[4];

    public static int current_scanline;
    static char[] use_vram = new char[512];

    /* PPU Variables */
    public static int PPU_Control0;		// $2000
    public static int PPU_Control1;		// $2001
    public static int PPU_Status;			// $2002
    public static int PPU_Sprite_Addr;	// $2003

    public static int PPU_X_fine;

    public static int u16_PPU_address;		// $2006
    public static int PPU_address_latch;
    public static int u16_PPU_refresh_data;
    public static int u16_PPU_refresh_latch;

    public static int PPU_tile_page;
    public static int PPU_sprite_page;
    public static int PPU_background_color;
    public static int PPU_add;

    public static int PPU_data_latch;
    public static int PPU_toggle;

    static int[]/*UINT32*/ in_0 = new int[3];
    static int[]/*UINT32*/ in_1 = new int[3];
    static int/*UINT32*/ in_0_shift;
    static int/*UINT32*/ in_1_shift;

    public static InitDriverPtr init_nes_core = new InitDriverPtr() {
        public void handler() {
            //throw new UnsupportedOperationException("Not supported yet.");
            /* We set these here in case they weren't set in the cart loader */
            _nes.rom = memory_region(REGION_CPU1);
            _nes.vrom = memory_region(REGION_GFX1);
            _nes.vram = memory_region(REGION_GFX2);
            _nes.wram = memory_region(REGION_USER1);

            battery_ram = _nes.wram;

            /* Set up the memory handlers for the mapper */
            switch (_nes.mapper)
            {
                    case 20:
                            _nes.slow_banking = 0;
                            install_mem_read_handler(0, 0x4030, 0x403f, fds_r);
                            install_mem_read_handler(0, 0x6000, 0xdfff, MRA_RAM);
                            install_mem_read_handler(0, 0xe000, 0xffff, MRA_ROM);

                            install_mem_write_handler(0, 0x4020, 0x402f, fds_w);
                            install_mem_write_handler(0, 0x6000, 0xdfff, MWA_RAM);
                            install_mem_write_handler(0, 0xe000, 0xffff, MWA_ROM);
                            break;
                    case 40:
                            _nes.slow_banking = 1;
                            /* Game runs code in between banks, so we do things different */
                            install_mem_read_handler(0, 0x6000, 0x7fff, MRA_RAM);
                            install_mem_read_handler(0, 0x8000, 0xffff, MRA_ROM);

                            install_mem_write_handler(0, 0x6000, 0x7fff, nes_mid_mapper_w);
                            install_mem_write_handler(0, 0x8000, 0xffff, nes_mapper_w);
                            break;
                    default:
                            _nes.slow_banking = 0;
                            install_mem_read_handler(0, 0x6000, 0x7fff, MRA_BANK5);
                            install_mem_read_handler(0, 0x8000, 0x9fff, MRA_BANK1);
                            install_mem_read_handler(0, 0xa000, 0xbfff, MRA_BANK2);
                            install_mem_read_handler(0, 0xc000, 0xdfff, MRA_BANK3);
                            install_mem_read_handler(0, 0xe000, 0xffff, MRA_BANK4);
                            memory_set_bankhandler_r(1, 0, MRA_BANK1);
                            memory_set_bankhandler_r(2, 0, MRA_BANK2);
                            memory_set_bankhandler_r(3, 0, MRA_BANK3);
                            memory_set_bankhandler_r(4, 0, MRA_BANK4);
                            memory_set_bankhandler_r(5, 0, MRA_BANK5);

                            install_mem_write_handler(0, 0x6000, 0x7fff, nes_mid_mapper_w);
                            install_mem_write_handler(0, 0x8000, 0xffff, nes_mapper_w);
            				break;
		}
	
		/* Set up the mapper callbacks */
		{
			int i = 0;
	
			while (mmc_list[i].iNesMapper != -1)
			{
				if (mmc_list[i].iNesMapper == _nes.mapper)
				{
					mmc_write_low = mmc_list[i].mmc_write_low;
					mmc_read_low = mmc_list[i].mmc_read_low;
					mmc_write_mid = mmc_list[i].mmc_write_mid;
					mmc_write = mmc_list[i].mmc_write;
					ppu_latch = mmc_list[i].ppu_latch;
					mmc_irq = mmc_list[i].mmc_irq;
					break;
				}
				i ++;
			}
			if (mmc_list[i].iNesMapper == -1)
			{
				printf ("Mapper %d is not yet supported, defaulting to no mapper.\n",_nes.mapper);
				mmc_write_low = mmc_write_mid = mmc_write = null;
				mmc_read_low = null;
				ppu_latch = null;
				mmc_irq = null;
			}
		}
	
            /* Load a battery file, but only if there's no trainer since they share */
 /* overlapping memory. */
            if (_nes.trainer != 0) {
                return;
            }

            /* We need this because battery ram is loaded before the */
 /* memory subsystem is set up. When this routine is called */
 /* everything is ready, so we can just copy over the data */
 /* we loaded before. */
            memcpy(battery_ram, battery_data, BATTERY_SIZE);
        }
    };

    public static InitDriverPtr init_nes = new InitDriverPtr() {
        public void handler() {
            ppu_scanlines_per_frame = NTSC_SCANLINES_PER_FRAME;
            init_nes_core.handler();
        }
    };

    public static InitDriverPtr init_nespal = new InitDriverPtr() {
        public void handler() {
            ppu_scanlines_per_frame = PAL_SCANLINES_PER_FRAME;
            init_nes_core.handler();
        }
    };

    public static InitMachinePtr nes_init_machine = new InitMachinePtr() {
        public void handler() {
            current_scanline = 0;

            ppu_reset(_ppu);

            /* Some carts have extra RAM and require it on at startup, e.g. Metroid */
            _nes.mid_ram_enable = 1;

            /* Reset the mapper variables. Will also mark the char-gen ram as dirty */
            mapper_reset(_nes.mapper);

            /* Reset the serial input ports */
            in_0_shift = 0;
            in_1_shift = 0;
        }
    };
    public static StopMachinePtr nes_stop_machine = new StopMachinePtr() {
        public void handler() {

            /* Write out the battery file if necessary */
            if (_nes.battery != 0) {
                throw new UnsupportedOperationException("Not supported yet.");
                /*TODO*///			void *f;
/*TODO*///	
/*TODO*///			f = osd_fopen(battery_name,0,OSD_FILETYPE_NVRAM,1);
/*TODO*///			if (f != 0)
/*TODO*///			{
/*TODO*///				osd_fwrite(f,battery_ram,BATTERY_SIZE);
/*TODO*///				osd_fclose (f);
/*TODO*///			}
            }
        }
    };

    public static void ppu_reset(ppu_struct _ppu) {
        /* Reset PPU variables */
        PPU_Control0 = PPU_Control1 = PPU_Status = 0;
        PPU_address_latch = 0;
        PPU_data_latch = 0;
        u16_PPU_address = PPU_Sprite_Addr = 0;
        PPU_tile_page = PPU_sprite_page = PPU_background_color = 0;

        PPU_add = 1;
        PPU_background_color = 0;
        PPU_toggle = 0;

        /* Reset mirroring */
        if (1 != 0) {
            ppu_page[0] = new UBytePtr(videoram, 0x2000);
            ppu_page[1] = new UBytePtr(videoram, 0x2400);
            ppu_page[2] = new UBytePtr(videoram, 0x2800);
            ppu_page[3] = new UBytePtr(videoram, 0x2c00);
        } else {
            throw new UnsupportedOperationException("Not supported yet.");
            /*TODO*///                        switch (nes.hard_mirroring)
/*TODO*///		{
/*TODO*///			case 0: ppu_mirror_h(); break;
/*TODO*///			case 1: ppu_mirror_v(); break;
/*TODO*///		}
        }
    }

    public static ReadHandlerPtr nes_IN0_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            int dip;
            int retVal;

            /* Some games expect bit 6 to be set because the last entry on the data bus shows up */
 /* in the unused upper 3 bits, so typically a read from $4016 leaves 0x40 there. */
            retVal = 0x40;

            retVal |= ((in_0[0] >> in_0_shift) & 0x01);

            /* Check the fake dip to see what's connected */
            dip = readinputport(2);

            switch (dip & 0x0f) {
                case 0x01: /* zapper */ {
                    char pix;
                    retVal |= 0x08;
                    /* no sprite hit */

 /* If button 1 is pressed, indicate the light gun trigger is pressed */
                    retVal |= ((in_0[0] & 0x01) << 4);

                    /* Look at the screen and see if the cursor is over a bright pixel */
                    pix = Machine.scrbitmap.line[in_0[2]].read(in_0[1]);
                    if ((pix == Machine.pens[0x20]) || (pix == Machine.pens[0x30])
                            || (pix == Machine.pens[0x33]) || (pix == Machine.pens[0x34])) {
                        retVal &= ~0x08;
                        /* sprite hit */
                    }
                }
                break;
                case 0x02:
                    /* multitap */
 /* Handle data line 1's serial output */
                    //			retVal |= ((in_0[1] >> in_0_shift) & 0x01) << 1;
                    break;
            }

            in_0_shift++;
            return retVal;
        }
    };

    public static ReadHandlerPtr nes_IN1_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            int dip;
            int retVal;

            /* Some games expect bit 6 to be set because the last entry on the data bus shows up */
 /* in the unused upper 3 bits, so typically a read from $4017 leaves 0x40 there. */
            retVal = 0x40;

            /* Handle data line 0's serial output */
            retVal |= ((in_1[0] >> in_1_shift) & 0x01);

            /* Check the fake dip to see what's connected */
            dip = readinputport(2);

            switch (dip & 0xf0) {
                case 0x10: /* zapper */ {
                    char pix;
                    retVal |= 0x08;
                    /* no sprite hit */

 /* If button 1 is pressed, indicate the light gun trigger is pressed */
                    retVal |= ((in_1[0] & 0x01) << 4);

                    /* Look at the screen and see if the cursor is over a bright pixel */
                    pix = Machine.scrbitmap.line[in_1[2]].read(in_1[1]);
                    if ((pix == Machine.pens[0x20]) || (pix == Machine.pens[0x30])
                            || (pix == Machine.pens[0x33]) || (pix == Machine.pens[0x34])) {
                        retVal &= ~0x08;
                        /* sprite hit */
                    }
                }
                break;
                case 0x20:
                    /* multitap */
 /* Handle data line 1's serial output */
                    //			retVal |= ((in_1[1] >> in_1_shift) & 0x01) << 1;
                    break;
                case 0x30:
                    /* arkanoid dial */
 /* Handle data line 2's serial output */
                    retVal |= ((in_1[2] >> in_1_shift) & 0x01) << 3;

                    /* Handle data line 3's serial output - bits are reversed */
                    //			retVal |= ((in_1[3] >> in_1_shift) & 0x01) << 4;
                    retVal |= ((in_1[3] << in_1_shift) & 0x80) >> 3;
                    break;
            }

            in_1_shift++;
            return retVal;
        }
    };

    public static WriteHandlerPtr nes_IN0_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            int dip;

            if ((data & 0x01) != 0) {
                return;
            }

            /* Toggling bit 0 high then low resets both controllers */
            in_0_shift = 0;
            in_1_shift = 0;

            in_0[0] = readinputport(0);

            /* Check the fake dip to see what's connected */
            dip = readinputport(2);

            switch (dip & 0x0f) {
                case 0x01:
                    /* zapper */
                    in_0[1] = readinputport(3);
                    /* x-axis */
                    in_0[2] = readinputport(4);
                    /* y-axis */
                    break;

                case 0x02:
                    /* multitap */
                    in_0[0] |= (readinputport(8) << 8);
                    in_0[0] |= (0x08 << 16);
                    /* OR in the 4-player adapter id, channel 0 */

 /* Optional: copy the data onto the second channel */
                    //			in_0[1] = in_0[0];
                    //			in_0[1] |= (0x04 << 16); /* OR in the 4-player adapter id, channel 1 */
                    break;
            }

            in_1[0] = readinputport(1);

            switch (dip & 0xf0) {
                case 0x10:
                    /* zapper */
                    if ((dip & 0x01) != 0) {
                        /* zapper is also on port 1, use 2nd player analog inputs */
                        in_1[1] = readinputport(5);
                        /* x-axis */
                        in_1[2] = readinputport(6);
                        /* y-axis */
                    } else {
                        in_1[1] = readinputport(3);
                        /* x-axis */
                        in_1[2] = readinputport(4);
                        /* y-axis */
                    }
                    break;

                case 0x20:
                    /* multitap */
                    in_1[0] |= (readinputport(9) << 8);
                    in_1[0] |= (0x04 << 16);
                    /* OR in the 4-player adapter id, channel 0 */

 /* Optional: copy the data onto the second channel */
                    //			in_1[1] = in_1[0];
                    //			in_1[1] |= (0x08 << 16); /* OR in the 4-player adapter id, channel 1 */
                    break;

                case 0x30:
                    /* arkanoid dial */
                    in_1[3] = (((readinputport(10) & 0xFF) + 0x52) ^ 0xff) & 0xFF;
                    //			in_1[3] = readinputport (10) ^ 0xff;
                    //			in_1[3] = 0x02;

                    /* Copy the joypad data onto the third channel */
                    in_1[2] = in_1[0] /*& 0x01*/;
                    break;
            }
        }
    };

    public static WriteHandlerPtr nes_IN1_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {

        }
    };

    static int vblank_started = 0;
    public static InterruptPtr nes_interrupt = new InterruptPtr() {
        public int handler() {
            int ret;

            ret = M6502_IRQ_LINE;

            /* See if a mapper generated an irq */
	    if (mmc_irq != null) ret = mmc_irq.handler(current_scanline);
            if (current_scanline <= BOTTOM_VISIBLE_SCANLINE) {
                /* If background or sprites are enabled, copy the ppu address latch */
                if ((PPU_Control1 & 0x18) != 0) {
                    /* Copy only the scroll x-coarse and the x-overflow bit */
                    u16_PPU_refresh_data = (u16_PPU_refresh_data & ~0x041f) & 0xFFFF;
                    u16_PPU_refresh_data = (u16_PPU_refresh_data | (u16_PPU_refresh_latch & 0x041f)) & 0xFFFF;
                }
                /* If we're not rendering this frame, fake the sprite hit */
                if (osd_skip_this_frame() != 0) {
                    if ((current_scanline == spriteram.read(0) + 7) && (PPU_Control1 & 0x10) != 0) {
                        PPU_Status |= PPU_status_sprite0_hit;
                    }
                }

                /* Render this scanline if appropriate */
                if ((PPU_Control1 & 0x18) != 0 /*&& !osd_skip_this_frame()*/) {
                    nes_vh_renderscanline(current_scanline);
                }
            } /* Has the vblank started? */ else if (current_scanline == BOTTOM_VISIBLE_SCANLINE + 1) {
                logerror("** Vblank started\n");

                /* Note: never reset the toggle to the scroll/address latches on vblank */
 /* VBlank in progress, set flag */
                PPU_Status |= PPU_status_vblank;
            } else if (current_scanline == NMI_SCANLINE) {
                /* Check if NMIs are enabled on vblank */
                if ((PPU_Control0 & PPU_c0_NMI) != 0) {
                    ret = M6502_INT_NMI;
                }
            }

            /* Increment the scanline pointer & check to see if it's rolled */
            if (++current_scanline == ppu_scanlines_per_frame) {
                /* vblank is over, start at top of screen again */
                current_scanline = 0;
                vblank_started = 0;

                /* Clear the vblank & sprite hit flag */
                PPU_Status &= ~(PPU_status_vblank | PPU_status_sprite0_hit);

                /* If background or sprites are enabled, copy the ppu address latch */
                if ((PPU_Control1 & 0x18) != 0) {
                    u16_PPU_refresh_data = u16_PPU_refresh_latch & 0xFFFF;
                }

                //if ((PPU_refresh_data & 0x400) != 0) Debugger ();
                logerror("** New frame\n");

                /* TODO: verify - this code assumes games with chr chunks won't generate chars on the fly */
 /* Pinbot seems to use both VROM and VRAM */
                if ((_nes.chr_chunks[0] == 0) || (_nes.mapper == 119)) {
                    int i;

                    /* Decode any dirty characters */
                    for (i = 0; i < 0x200; i++) {
                        if (dirtychar[i] != 0) {
                            decodechar(Machine.gfx[1], i, _nes.vram, Machine.drv.gfxdecodeinfo[1].gfxlayout);
                            dirtychar[i] = 0;
                            use_vram[i] = 1;
                        }
                    }
                }
            }

            if ((ret != M6502_IRQ_LINE)) {
                logerror("--- scanline %d", current_scanline);
                if (ret == M6502_INT_IRQ) {
                    logerror(" IRQ\n");
                } else {
                    logerror(" NMI\n");
                }
            }

            return ret;
        }
    };

    public static ReadHandlerPtr nes_ppu_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            int retVal = 0;
            /*
	    |  $2002  | PPU Status Register (R)                                  |
	    |         |   %vhs-----                                              |
	    |         |               v = VBlank Occurance                       |
	    |         |                      1 = In VBlank                       |
	    |         |               h = Sprite #0 Occurance                    |
	    |         |                      1 = VBlank has hit Sprite #0        |
	    |         |               s = Scanline Sprite Count                  |
	    |         |                      0 = 8 or less sprites on the        |
	    |         |                          current scanline                |
	    |         |                      1 = More than 8 sprites on the      |
	    |         |                          current scanline                |
             */
            switch (offset & 0x07) {
                case 0:
                case 1:
                case 3:
                case 5:
                case 6:
                    retVal = 0x00;
                    break;

                case 2:
                    retVal = PPU_Status & 0xFF;

                    /* This is necessary: see W&W1, Gi Joe Atlantis */
                    PPU_toggle = 0;

                    /* Note that we don't clear the vblank flag - this is correct. */
 /* Many games would break if we did: Dragon Warrior 3, GI Joe Atlantis */
 /* are two. */
                    break;

                case 4:
                    retVal = spriteram.read(PPU_Sprite_Addr);
                    break;

                case 7:
                    retVal = PPU_data_latch & 0xFF;

                    if (ppu_latch != null) ppu_latch.handler(u16_PPU_address & 0x3fff);
                    if ((u16_PPU_address >= 0x2000) && (u16_PPU_address <= 0x3fef)) {
                        PPU_data_latch = ppu_page[(u16_PPU_address & 0xc00) >> 10].read(u16_PPU_address & 0x3ff);
                    } else {
                        PPU_data_latch = videoram.read(u16_PPU_address & 0x3fff);
                    }

                    /* TODO: this is a bit of a hack, needed to get Argus, ASO, etc to work */
 /* but, B-Wings, submath (j) seem to use this location differently... */
                    if (_nes.chr_chunks[0] != 0 && (u16_PPU_address & 0x3fff) < 0x2000) {
                        int vrom_loc;

                        vrom_loc = (nes_vram[(u16_PPU_address & 0x1fff) >> 10] * 16) + (u16_PPU_address & 0x3ff);
                        PPU_data_latch = _nes.vrom.read(vrom_loc);
                    }
                    u16_PPU_address = (u16_PPU_address + PPU_add) & 0xFFFF;
                    break;
            }

            return retVal & 0xFF;
        }
    };

    public static WriteHandlerPtr nes_ppu_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            switch (offset & 0x07) {
                /*
	    |  $2000  | PPU Control Register #1 (W)                              |
	    |         |   %vMsbpiNN                                              |
	    |         |               v = Execute NMI on VBlank                  |
	    |         |                      1 = Enabled                         |
	    |         |               M = PPU Selection (unused)                 |
	    |         |                      0 = Master                          |
	    |         |                      1 = Slave                           |
	    |         |               s = Sprite Size                            |
	    |         |                      0 = 8x8                             |
	    |         |                      1 = 8x16                            |
	    |         |               b = Background Pattern Table Address       |
	    |         |                      0 = $0000 (VRAM)                    |
	    |         |                      1 = $1000 (VRAM)                    |
	    |         |               p = Sprite Pattern Table Address           |
	    |         |                      0 = $0000 (VRAM)                    |
	    |         |                      1 = $1000 (VRAM)                    |
	    |         |               i = PPU Address Increment                  |
	    |         |                      0 = Increment by 1                  |
	    |         |                      1 = Increment by 32                 |
	    |         |              NN = Name Table Address                     |
	    |         |                     00 = $2000 (VRAM)                    |
	    |         |                     01 = $2400 (VRAM)                    |
	    |         |                     10 = $2800 (VRAM)                    |
	    |         |                     11 = $2C00 (VRAM)                    |
	    |         |                                                          |
	    |         | NOTE: Bit #6 (M) has no use, as there is only one (1)    |
	    |         |       PPU installed in all forms of the NES and Famicom. |
                 */
                case 0:
                    /* PPU Control 0 */
                    PPU_Control0 = data;

                    u16_PPU_refresh_latch = (u16_PPU_refresh_latch & ~0x0c00) & 0xFFFF;
                    u16_PPU_refresh_latch = (u16_PPU_refresh_latch | (data & 0x03) << 10) & 0xFFFF;

                    /* The char ram bank points either 0x0000 or 0x1000 (page 0 or page 4) */
                    PPU_tile_page = (PPU_Control0 & PPU_c0_chr_select) >> 2;
                    PPU_sprite_page = (PPU_Control0 & PPU_c0_spr_select) >> 1;

                    if ((PPU_Control0 & PPU_c0_inc) != 0) {
                        PPU_add = 32;
                    } else {
                        PPU_add = 1;
                    }
                    break;
                /*
	    |  $2001  | PPU Control Register #2 (W)                              |
	    |         |   %fffpcsit                                              |
	    |         |             fff = Full Background Colour                 |
	    |         |                    000 = Black                           |
	    |         |                    001 = Red                             |
	    |         |                    010 = Blue                            |
	    |         |                    100 = Green                           |
	    |         |               p = Sprite Visibility                      |
	    |         |                      1 = Display                         |
	    |         |               c = Background Visibility                  |
	    |         |                      1 = Display                         |
	    |         |               s = Sprite Clipping                        |
	    |         |                      0 = Sprites not displayed in left   |
	    |         |                          8-pixel column                  |
	    |         |                      1 = No clipping                     |
	    |         |               i = Background Clipping                    |
	    |         |                      0 = Background not displayed in     |
	    |         |                          left 8-pixel column             |
	    |         |                      1 = No clipping                     |
	    |         |               t = Display Type                           |
	    |         |                      0 = Colour display                  |
	    |         |                      1 = Mono-type (B&W) display         |
                 */
                case 1:
                    /* PPU Control 1 */
 /* If color intensity has changed, change all the pens */
                    if ((data & 0xe0) != (PPU_Control1 & 0xe0)) {
                    }
                    PPU_Control1 = data;
                    break;
                case 2:
                    /* PPU Status */
                    break;
                case 3:
                    /* PPU Sprite Memory Address */
                    PPU_Sprite_Addr = data;
                    break;
                case 4:
                    /* PPU Sprite Data */
                    spriteram.write(PPU_Sprite_Addr, data);
                    PPU_Sprite_Addr++;
                    PPU_Sprite_Addr &= 0xff;
                    break;

                case 5:
                    if (PPU_toggle != 0) /* (second write) */ {
                        u16_PPU_refresh_latch = (u16_PPU_refresh_latch & ~0x03e0) & 0xFFFF;
                        u16_PPU_refresh_latch = (u16_PPU_refresh_latch | (data & 0xf8) << 2) & 0xFFFF;

                        u16_PPU_refresh_latch = (u16_PPU_refresh_latch & ~0x7000) & 0xFFFF;
                        u16_PPU_refresh_latch = (u16_PPU_refresh_latch | (data & 0x07) << 12) & 0xFFFF;
                    } /* (first write) */ else {
                        u16_PPU_refresh_latch = (u16_PPU_refresh_latch & ~0x1f) & 0xFFFF;
                        u16_PPU_refresh_latch = (u16_PPU_refresh_latch | (data & 0xf8) >> 3) & 0xFFFF;

                        PPU_X_fine = data & 0x07;
                    }
                    PPU_toggle = PPU_toggle!=0?0:1;
                    break;

                case 6:
                    /* PPU Address Register */
 /* PPU Memory Adress */
                    if (PPU_toggle != 0) {
                        u16_PPU_address = ((PPU_address_latch << 8) | data) & 0xFFFF;

                        u16_PPU_refresh_latch = (u16_PPU_refresh_latch & ~0x00ff) & 0xFFFF;
                        u16_PPU_refresh_latch = (u16_PPU_refresh_latch | data) & 0xFFFF;
                        u16_PPU_refresh_data = u16_PPU_refresh_latch;
                    } else {
                        PPU_address_latch = data & 0xFF;

                        if (data != 0x3f) /* TODO: remove this hack! */ {
                            u16_PPU_refresh_latch = (u16_PPU_refresh_latch & ~0xff00) & 0xFFFF;
                            u16_PPU_refresh_latch = (u16_PPU_refresh_latch | (data & 0x3f) << 8) & 0xFFFF;
                        }
                    }
                    PPU_toggle = PPU_toggle!=0?0:1;
                    break;

                case 7:
                    /* PPU I/O Register */

                    if ((current_scanline <= BOTTOM_VISIBLE_SCANLINE) /*&& (PPU_Control1 & 0x18)*/) {
                        //				logerror("*** PPU write during hblank (%d) ",  current_scanline);
                    }
                    Write_PPU(data);
                    break;
            }
        }
    };

    	
	public static void ppu_mirror_h ()
	{
		if (_nes.four_screen_vram != 0) return;
	
/*TODO*///	#ifdef LOG_PPU
/*TODO*///		logerror ("mirror: horizontal\n");
/*TODO*///	#endif
/*TODO*///	
/*TODO*///	#ifdef NO_MIRRORING
/*TODO*///		return;
/*TODO*///	#endif
	
		ppu_page[0] = new UBytePtr(videoram, 0x2000);
		ppu_page[1] = new UBytePtr(videoram, 0x2000);
		ppu_page[2] = new UBytePtr(videoram, 0x2400);
		ppu_page[3] = new UBytePtr(videoram, 0x2400);
	}
	
	public static void ppu_mirror_v ()
	{
		if (_nes.four_screen_vram != 0) return;
	
/*TODO*///	#ifdef LOG_PPU
/*TODO*///		logerror ("mirror: vertical\n");
/*TODO*///	#endif
/*TODO*///	
/*TODO*///	#ifdef NO_MIRRORING
/*TODO*///		return;
/*TODO*///	#endif
	
		ppu_page[0] = new UBytePtr(videoram, 0x2000);
		ppu_page[1] = new UBytePtr(videoram, 0x2400);
		ppu_page[2] = new UBytePtr(videoram, 0x2000);
		ppu_page[3] = new UBytePtr(videoram, 0x2400);
	}
	
	public static void ppu_mirror_low ()
	{
		if (_nes.four_screen_vram != 0) return;
	
/*TODO*///	#ifdef LOG_PPU
/*TODO*///		logerror ("mirror: $2000\n");
/*TODO*///	#endif
/*TODO*///	
/*TODO*///	#ifdef NO_MIRRORING
/*TODO*///		return;
/*TODO*///	#endif
/*TODO*///	
		ppu_page[0] = new UBytePtr(videoram, 0x2000);
		ppu_page[1] = new UBytePtr(videoram, 0x2000);
		ppu_page[2] = new UBytePtr(videoram, 0x2000);
		ppu_page[3] = new UBytePtr(videoram, 0x2000);
	}
	
	public static void ppu_mirror_high ()
	{
		if (_nes.four_screen_vram != 0) return;
	
/*TODO*///	#ifdef LOG_PPU
/*TODO*///		logerror ("mirror: $2400\n");
/*TODO*///	#endif
	
/*TODO*///	#ifdef NO_MIRRORING
/*TODO*///		return;
/*TODO*///	#endif
	
		ppu_page[0] = new UBytePtr(videoram, 0x2400);
		ppu_page[1] = new UBytePtr(videoram, 0x2400);
		ppu_page[2] = new UBytePtr(videoram, 0x2400);
		ppu_page[3] = new UBytePtr(videoram, 0x2400);
	}
	
	public static void ppu_mirror_custom (int page, int address)
	{
		if (_nes.four_screen_vram != 0) return;
	
		address = (address << 10) | 0x2000;
	
/*TODO*///	#ifdef LOG_PPU
/*TODO*///		logerror ("mirror custom, page: %d, address: %04x\n", page, address);
/*TODO*///	#endif
	
/*TODO*///	#ifdef NO_MIRRORING
/*TODO*///		return;
/*TODO*///	#endif
	
		ppu_page[page] = new UBytePtr(videoram, address);
	}
	
	public static void ppu_mirror_custom_vrom (int page, int address)
	{
		if (_nes.four_screen_vram != 0) return;
	
/*TODO*///	#ifdef LOG_PPU
/*TODO*///		logerror ("mirror custom vrom, page: %d, address: %04x\n", page, address);
/*TODO*///	#endif
/*TODO*///	
/*TODO*///	#ifdef NO_MIRRORING
/*TODO*///		return;
/*TODO*///	#endif
	
		ppu_page[page] = new UBytePtr(_nes.vrom, address);
	}
	
    static void Write_PPU(int data) {
        int tempAddr = u16_PPU_address & 0x3fff;

        if (ppu_latch != null) ppu_latch.handler(tempAddr);
        if (tempAddr < 0x2000) {
            /* This ROM writes to the character gen portion of VRAM */
            dirtychar[tempAddr >> 4] = 1;
            _nes.vram.write(tempAddr, data);
            videoram.write(tempAddr, data);

            if (_nes.chr_chunks[0] != 0) {
                logerror("****** PPU write to vram with CHR_ROM - %04x:%02x!\n", tempAddr, data);
            }
            u16_PPU_address = (u16_PPU_address + PPU_add) & 0xFFFF;
            return;
        }

        /* The only valid background colors are writes to 0x3f00 and 0x3f10 */
 /* and even then, they are mirrors of each other. */
 /* As usual, some games attempt to write values > the number of colors so we must mask the data. */
        if (tempAddr >= 0x3f00) {
            videoram.write(tempAddr, data);
            data &= 0x3f;

            if ((tempAddr & 0x03) != 0) {
                Machine.gfx[0].colortable.write(tempAddr & 0x1f, Machine.pens[data]);
                colortable_mono[tempAddr & 0x1f] = Machine.pens[data & 0xf0];
            }

            if ((tempAddr & 0x0f) == 0) {
                int i;

                PPU_background_color = data;
                for (i = 0; i < 0x20; i += 0x04) {
                    Machine.gfx[0].colortable.write(i, Machine.pens[data]);
                    colortable_mono[i] = Machine.pens[data & 0xf0];
                }
            }
            u16_PPU_address = (u16_PPU_address + PPU_add) & 0xFFFF;
            return;
        } /* everything else */ else {
            /* Writes to $3000-$3eff are mirrors of $2000-$2eff, used by e.g. Trojan */
            int page = (u16_PPU_address & 0x0c00) >> 10;
            int address = u16_PPU_address & 0x3ff;

            ppu_page[page].write(address, data);
        }

        //end:
        u16_PPU_address = (u16_PPU_address + PPU_add) & 0xFFFF;
    }

    public static io_initPtr nes_load_rom = new io_initPtr() {
        public int handler(int id) {
            /*TODO*///		const char *mapinfo;
            int mapint1 = 0, mapint2 = 0, mapint3 = 0, mapint4 = 0, goodcrcinfo = 0;
            Object romfile;
            char[] magic = new char[4];
            char[] skank = new char[8];
            int local_options = 0;
            char m[] = new char[1];
            int i;

            if ((device_filename(IO_CARTSLOT, id) == null) && (id == 0)) {
                //		printf("NES requires cartridge!\n");
                return INIT_FAILED;
            } else {
                battery_name = device_filename(IO_CARTSLOT, id);

                /* Strip off file extension if it exists */
                if (battery_name.lastIndexOf('.') != -1) {
                    battery_name = battery_name.substring(0, battery_name.lastIndexOf('.'));

                }
                logerror("battery name (minus extension): %s\n", battery_name);
            }

            if ((romfile = image_fopen(IO_CARTSLOT, id, OSD_FILETYPE_IMAGE_R, 0)) == null) {
                logerror("image_fopen failed in nes_load_rom.\n");
                return 1;
            }

            /* Verify the file is in iNES format */
            osd_fread(romfile, magic, 4);

            if ((magic[0] != 'N')
                    || (magic[1] != 'E')
                    || (magic[2] != 'S')) {
                logerror("BAD section hit during LOAD ROM.\n");
                osd_fclose(romfile);
                return 1;
            }
            /*TODO*///		mapinfo = device_extrainfo(IO_CARTSLOT,id);
/*TODO*///		if (mapinfo != 0)
/*TODO*///		{
/*TODO*///			if (4 == sscanf(mapinfo,"%d %d %d %d",&mapint1,&mapint2,&mapint3,&mapint4))
/*TODO*///			{
/*TODO*///				nes.mapper = mapint1;
/*TODO*///				local_options = mapint2;
/*TODO*///				nes.prg_chunks = mapint3;
/*TODO*///				nes.chr_chunks = mapint4;
/*TODO*///				logerror("NES.CRC info: %d %d %d %d\n",mapint1,mapint2,mapint3,mapint4);
/*TODO*///				goodcrcinfo = 1;
/*TODO*///			} else 
/*TODO*///			{
/*TODO*///				logerror("NES: [%s], Invalid mapinfo found\n",mapinfo);
/*TODO*///			}
/*TODO*///		} else
/*TODO*///		{
/*TODO*///			logerror("NES: No extrainfo found\n");
/*TODO*///		}
            if (goodcrcinfo == 0) {
                osd_fread(romfile, _nes.prg_chunks, 1);
                osd_fread(romfile, _nes.chr_chunks, 1);
                /* Read the first ROM option byte (offset 6) */
                osd_fread(romfile, m, 1);

                /* Interpret the iNES header flags */
                _nes.mapper = (char) ((m[0] & 0xf0) >> 4);
                local_options = m[0] & 0x0f;

                /* Read the second ROM option byte (offset 7) */
                osd_fread(romfile, m, 1);

                /* Check for skanky headers */
                osd_fread(romfile, skank, 8);

                /* If the header has junk in the unused bytes, assume the extra mapper byte is also invalid */
 /* We only check the first 4 unused bytes for now */
                for (i = 0; i < 4; i++) {
                    //logerror("%02x ", skank[i]);
                    if (skank[i] != 0x00) {
                        logerror("(skank: %d)", i);
                        //				m = 0;
                    }
                }
                logerror("\n");

                _nes.mapper = (char) (_nes.mapper | (m[0] & 0xf0));
            }

            _nes.hard_mirroring = local_options & 0x01;
            _nes.battery = local_options & 0x02;
            _nes.trainer = local_options & 0x04;
            _nes.four_screen_vram = local_options & 0x08;

            if (_nes.battery != 0) {
                logerror("-- Battery found\n");
            }
            if (_nes.trainer != 0) {
                logerror("-- Trainer found\n");
            }
            if (_nes.four_screen_vram != 0) {
                logerror("-- 4-screen VRAM\n");
            }

            /* Free the regions that were allocated by the ROM loader */
            free_memory_region(REGION_CPU1);
            free_memory_region(REGION_GFX1);

            /* Allocate them again with the proper size */
            if (new_memory_region(REGION_CPU1, 0x10000 + (_nes.prg_chunks[0] + 1) * 0x4000, 0) != 0
                    || new_memory_region(REGION_GFX1, (_nes.chr_chunks[0] + 1) * 0x2000, 0) != 0) {
                printf("Memory allocation failed reading roms!\n");
                logerror("BAD section hit during LOAD ROM.\n");
                osd_fclose(romfile);
                return 1;
            }

            _nes.rom = memory_region(REGION_CPU1);
            _nes.vrom = memory_region(REGION_GFX1);
            _nes.vram = memory_region(REGION_GFX2);
            _nes.wram = memory_region(REGION_USER1);

            /* Position past the header */
            osd_fseek(romfile, 16, SEEK_SET);

            /* Load the 0x200 byte trainer at 0x7000 if it exists */
            if (_nes.trainer != 0) {
                /*TODO*///			osd_fread (romfile, &_nes.wram[0x1000], 0x200);
                throw new UnsupportedOperationException("Not supported yet.");

            }

            /* Read in the program chunks */
            if (_nes.prg_chunks[0] == 1) {
                osd_fread(romfile, _nes.rom, 0x14000, 0x4000);
                /* Mirror this bank into $8000 */
                memcpy(_nes.rom, 0x10000, _nes.rom, 0x14000, 0x4000);
            } else {
                osd_fread(romfile, _nes.rom, 0x10000, 0x4000 * _nes.prg_chunks[0]);
            }

            logerror("**\n");
            logerror("Mapper: %d\n", _nes.mapper);
            //logerror("PRG chunks: %02x, size: %06x\n", _nes.prg_chunks[0], 0x4000 * _nes.prg_chunks[0]);

            /* Read in any chr chunks */
            if (_nes.chr_chunks[0] > 0) {
                osd_fread(romfile, _nes.vrom, 0x2000 * _nes.chr_chunks[0]);

                /* Mark each char as not existing in VRAM */
                for (i = 0; i < 512; i++) {
                    use_vram[i] = 0;
                }
                /* Calculate the total number of characters to decode */
                nes_charlayout.total = _nes.chr_chunks[0] * 512;
                if (_nes.mapper == 2) {
                    printf("Warning: VROM has been found in VRAM-based mapper. Either the mapper is set wrong or the ROM image is incorrect.\n");
                }
            } else {
                /* Mark each char as existing in VRAM */
                for (i = 0; i < 512; i++) {
                    use_vram[i] = 1;
                }
                nes_charlayout.total = 512;
            }

            //logerror("CHR chunks: %02x, size: %06x\n", _nes.chr_chunks, 0x4000 * _nes.chr_chunks[0]);
            logerror("**\n");

            /* Attempt to load a battery file for this ROM. If successful, we */
 /* must wait until later to move it to the system memory. */
            if (_nes.battery != 0) {
                throw new UnsupportedOperationException("Not supported yet.");
                /*TODO*///			void *f;
/*TODO*///	
/*TODO*///			f = osd_fopen (battery_name, 0, OSD_FILETYPE_NVRAM, 0);
/*TODO*///			if (f != 0)
/*TODO*///			{
/*TODO*///				osd_fread (f, battery_data, BATTERY_SIZE);
/*TODO*///				osd_fclose (f);
/*TODO*///			}
/*TODO*///			else
/*TODO*///				memset (battery_data, 0, BATTERY_SIZE);
            }

            osd_fclose(romfile);
            return 0;

        }
    };

    public static io_partialcrcPtr nes_partialcrc = new io_partialcrcPtr() {
        public int/*UINT32*/ handler(UBytePtr buf,/*unsigned*/ int size) {
            throw new UnsupportedOperationException("Not supported yet.");
            /*TODO*///	UINT32 crc;
/*TODO*///	if (size < 17) return 0;
/*TODO*///	crc = (UINT32) crc32(0L,&buf[16],size-16);
/*TODO*///	logerror("NES Partial CRC: %08lx %d\n",crc,size);
/*TODO*///	return crc;
        }
    };
    public static io_idPtr nes_id_rom = new io_idPtr() {
        public int handler(int id) {
            Object romfile;
            char[] magic = new char[4];
            int retval;

            if ((romfile = image_fopen(IO_CARTSLOT, id, OSD_FILETYPE_IMAGE_R, 0)) == null) {
                return 0;
            }

            retval = 1;
            /* Verify the file is in iNES format */
            osd_fread(romfile, magic, 4);
            if ((magic[0] != 'N')
                    || (magic[1] != 'E')
                    || (magic[2] != 'S')) {
                retval = 0;
            }

            osd_fclose(romfile);
            return retval;
        }
    };
    /*TODO*///	
/*TODO*///	int nes_load_disk (int id)
/*TODO*///	{
/*TODO*///	 	FILE *diskfile;
/*TODO*///		unsigned char magic[4];
/*TODO*///	
/*TODO*///		if (!device_filename(IO_FLOPPY,id)) return INIT_FAILED;
/*TODO*///	
/*TODO*///		if (!(diskfile = image_fopen (IO_FLOPPY, id, OSD_FILETYPE_IMAGE_R, 0)))
/*TODO*///		{
/*TODO*///			logerror("image_fopen failed in nes_load_disk for [%s].\n",device_filename(IO_FLOPPY,id));
/*TODO*///				return 1;
/*TODO*///		}
/*TODO*///	
/*TODO*///		/* See if it has a fucking redundant header on it */
/*TODO*///		osd_fread (diskfile, magic, 4);
/*TODO*///		if ((magic[0] == 'F') &&
/*TODO*///			(magic[1] == 'D') &&
/*TODO*///			(magic[2] == 'S'))
/*TODO*///		{
/*TODO*///			/* Skip past the fucking redundant header */
/*TODO*///			osd_fseek (diskfile, 0x10, SEEK_SET);
/*TODO*///		}
/*TODO*///		else
/*TODO*///			/* otherwise, point to the start of the image */
/*TODO*///			osd_fseek (diskfile, 0, SEEK_SET);
/*TODO*///	
/*TODO*///		/* clear some of the cart variables we don't use */
/*TODO*///		nes.trainer = 0;
/*TODO*///		nes.battery = 0;
/*TODO*///		nes.prg_chunks = nes.chr_chunks = 0;
/*TODO*///	
/*TODO*///		nes.mapper = 20;
/*TODO*///		nes.four_screen_vram = 0;
/*TODO*///		nes.hard_mirroring = 0;
/*TODO*///	
/*TODO*///		nes_fds.sides = 0;
/*TODO*///		nes_fds.data = null;
/*TODO*///	
/*TODO*///		/* read in all the sides */
/*TODO*///		while (!osd_feof (diskfile))
/*TODO*///		{
/*TODO*///			nes_fds.sides ++;
/*TODO*///			nes_fds.data = realloc (nes_fds.data, nes_fds.sides * 65500);
/*TODO*///			osd_fread (diskfile, nes_fds.data + ((nes_fds.sides-1) * 65500), 65500);
/*TODO*///		}
/*TODO*///	
/*TODO*///		/* adjust for eof */
/*TODO*///		nes_fds.sides --;
/*TODO*///		nes_fds.data = realloc (nes_fds.data, nes_fds.sides * 65500);
/*TODO*///	
/*TODO*///		logerror ("Number of sides: %d", nes_fds.sides);
/*TODO*///	
/*TODO*///		osd_fclose (diskfile);
/*TODO*///		return 0;
/*TODO*///	
/*TODO*///	//bad:
/*TODO*///		logerror("BAD section hit during disk load.\n");
/*TODO*///		if (diskfile != 0) osd_fclose (diskfile);
/*TODO*///		return 1;
/*TODO*///	}
/*TODO*///	
/*TODO*///	void nes_exit_disk (int id)
/*TODO*///	{
/*TODO*///		/* TODO: should write out changes here as well */
/*TODO*///		free (nes_fds.data);
/*TODO*///		nes_fds.data = null;
/*TODO*///	}
/*TODO*///		
}
