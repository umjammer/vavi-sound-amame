/***************************************************************************

  gb.c

  Machine file to handle emulation of the Nintendo GameBoy.

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.machine;

import static arcadeflex036.libc_old.strcpy;
import static arcadeflex036.libc_old.strncpy;
import static arcadeflex056.fileio.*;
import static arcadeflex056.fucPtr.*;
import static arcadeflex056.osdepend.logerror;
import static common.ptr.*;
import static mame056.common.*;
import static mame056.commonH.*;
import static mame056.memory.*;
import static mame056.memoryH.*;
import static mame056.cpuexec.*;
import static mame056.cpuexecH.*;
import static mame056.cpuintrf.*;
import static mame056.cpuintrfH.*;
import static mame056.inptport.*;
import static mame056.inptportH.*;
import static common.libc.cstdio.*;
import static common.libc.cstring.*;
import static consoleflex056.funcPtr.*;
import static mame056.mame.*;
import static mame056.osdependH.*;
import static mame056.timer.*;
import static mess056.deviceH.*;

import static mess056.machine.gbH.*;
import static mess056.mess.*;
import static mess056.messH.*;
import static mess056.sndhrdw.gb.*;
import static mess056.vidhrdw.gb.*;

public class gb
{
	
    static int MBCType;				   /* MBC type: 0 for none                        */
    static int CartType;				   /* Cart Type (battery, ram, timer etc)         */
    public static UBytePtr[] ROMMap = new UBytePtr[512];			   /* Addresses of ROM banks                      */
    static int ROMBank;				   /* Number of ROM bank currently used           */
    static int ROMMask;				   /* Mask for the ROM bank number                */
    static int ROMBanks;				   /* Total number of ROM banks                   */
    static UBytePtr[] RAMMap = new UBytePtr[256];			   /* Addresses of RAM banks                      */
    static int RAMBank;				   /* Number of RAM bank currently used           */
    static int RAMMask;				   /* Mask for the RAM bank number                */
    static int RAMBanks;				   /* Total number of RAM banks                   */
    static int SIOCount;				   /* Serial I/O counter                          */
    static int MBC1Mode;				   /* MBC1 ROM/RAM mode                           */
    static int[] MBC3RTCMap = new int[5];			   /* MBC3 Real-Time-Clock banks                  */
    static int MBC3RTCBank;			   /* Number of RTC bank for MBC3                 */
    static UBytePtr[] GBC_RAMMap = new UBytePtr[8];		   /* (GBC) Addresses of internal RAM banks       */
    static int GBC_RAMBank;			   /* (GBC) Number of RAM bank currently used     */
    public static UBytePtr[] GBC_VRAMMap = new UBytePtr[2];				   /* (GBC) Addressses of video RAM banks         */
    static int GBC_VRAMBank;					   /* (GBC) Number of video RAM bank currently used */
    static char[] sgb_atf_data = new char[4050];	   /* (SGB) Attribute files                       */
    public static UBytePtr gb_ram;

    public static int Verbose = 0x00;
    public static int CheckCRC = 1;
    public static int LineDelay = 0;
    public static int IFreq = 60;

    public static InitMachinePtr machine_init_gb = new InitMachinePtr() {
        public void handler() {
            System.out.println("machine_init_gb");
            int i;

            /* Initialize the memory banks */
            MBC1Mode = 0;
            MBC3RTCBank = 0;
            ROMBank = 1;
            RAMBank = 0;
            cpu_setbank (1, ROMMap[ROMBank]!= null ? ROMMap[ROMBank] : new UBytePtr(gb_ram, 0x4000));
            cpu_setbank (2, RAMMap[RAMBank]!=null ? RAMMap[RAMBank] : new UBytePtr(gb_ram, 0xA000));

            /* Initialise the registers */
            LCDSTAT( 0x00 );
            CURLINE( 0x00 );
            CMPLINE( 0x00 );
            IFLAGS( 0x00 );
            ISWITCH( 0x00 );
            SIODATA( 0x00 );
            SIOCONT( 0x7E );
            SCROLLX( 0x00 );
            SCROLLY( 0x00 );
            WNDPOSX( 0x00 );
            WNDPOSY( 0x00 );
            gb_w_io.handler(0x05, 0x00 );	/* TIMECNT */
            gb_w_io.handler( 0x06, 0x00 );	/* TIMEMOD */
            gb_w_io.handler( 0x07, 0x00 );	/* TIMEFRQ */
            gb_w_io.handler( 0x40, 0x91 );	/* LCDCONT */
            gb_w_io.handler( 0x47, 0xFC );	/* BGRDPAL */
            gb_w_io.handler( 0x48, 0xFC );	/* SPR0PAL */
            gb_w_io.handler( 0x49, 0xFC );	/* SPR1PAL */

            /* Initialise the Sound Registers */
            gameboy_sound_w.handler(0xFF26,0xF1); /*Gameboy, F0 for SGB*/ /* set this first */
            gameboy_sound_w.handler(0xFF10,0x80);
            gameboy_sound_w.handler(0xFF11,0xBF);
            gameboy_sound_w.handler(0xFF12,0xF3);
            gameboy_sound_w.handler(0xFF14,0x3F); /* NOTE: Should be 0xBF but it causes a tone at startup */
            gameboy_sound_w.handler(0xFF16,0x3F);
            gameboy_sound_w.handler(0xFF17,0x00);
            gameboy_sound_w.handler(0xFF19,0xBF);
            gameboy_sound_w.handler(0xFF1A,0x7F);
            gameboy_sound_w.handler(0xFF1B,0xFF);
            gameboy_sound_w.handler(0xFF1C,0x9F);
            gameboy_sound_w.handler(0xFF1E,0xBF);
            gameboy_sound_w.handler(0xFF20,0xFF);
            gameboy_sound_w.handler(0xFF21,0x00);
            gameboy_sound_w.handler(0xFF22,0x00);
            gameboy_sound_w.handler(0xFF23,0xBF);
            gameboy_sound_w.handler(0xFF24,0x77);
            gameboy_sound_w.handler(0xFF25,0xF3);

            /* Initialize palette arrays */
            for( i = 0; i < 4; i++ ){
                    gb_bpal[i] = (char) i;
                    gb_spal0[i] = (char) i;
                    gb_spal1[i] = (char)i;
            }

            /* set the scanline refresh function */
            refresh_scanline = gb_refresh_scanline;
        }
    };

    public static InitMachinePtr machine_init_sgb = new InitMachinePtr() {
        public void handler() {
        
            /* Call GameBoy init */
            machine_init_gb.handler();

            if( new_memory_region( REGION_GFX1, 0x2000, 0 ) != 0)
            {
                    logerror("Memory allocation failed for SGB border tiles!\n");
            }
            sgb_tile_data = new UBytePtr(memory_region( REGION_GFX1 ));
            memset( sgb_tile_data, 0, 0x2000 );

            /* Initialize the Sound Registers */
            gameboy_sound_w.handler(0xFF26,0xF0); /* F0 for SGB */

            sgb_window_mask = 0;
//            memset( sgb_pal_map, 0, 20*18 );
            for (int _i = 0 ; _i<20 ; _i++)
                for (int _j = 0 ; _j<18 ; _j++)
                    sgb_pal_map[_i][_j] = 0;
                    
            memset( sgb_atf_data, 0, 4050 );

            /* HACKS for Donkey Kong Land 2 + 3.
               For some reason that I haven't figured out, they store the tile
               data differently.  Hacks will go once I figure it out */
            sgb_hack = 0;
/*TODO*///            if( strncmp((const char*)(gb_ram + 0x134), "DONKEYKONGLAND 2", 16 ) == 0 ||
/*TODO*///                    strncmp( (const char*)(gb_ram + 0x134), "DONKEYKONGLAND 3", 16 ) == 0 )
/*TODO*///            {
/*TODO*///                    sgb_hack = 1;
/*TODO*///            }

            /* set the scanline refresh function */
            refresh_scanline = sgb_refresh_scanline;
        }
    };

    public static InitMachinePtr machine_init_gbc = new InitMachinePtr() {
        public void handler() {
        
            int I;

            /* Call GameBoy init */
            machine_init_gb.handler();

            /* Allocate memory for internal ram */
            for( I = 0; I < 8; I++ )
            {
                    if ((GBC_RAMMap[I] = new UBytePtr(0x1000)) != null)
                            memset (GBC_RAMMap[I], 0, 0x1000);
                    else
                    {
                            logerror("Error allocating memory\n");
                    }
            }
            GBC_RAMBank = 0;
            cpu_setbank (3, GBC_RAMMap[GBC_RAMBank]);

            /* Allocate memory for video ram */
            for( I = 0; I < 2; I++ )
            {
                    if ((GBC_VRAMMap[I] = new UBytePtr(0x2000)) != null)
                    {
                            memset (GBC_VRAMMap[I], 0, 0x2000);
                    }
                    else
                    {
                            printf("Error allocating video memory\n");
                    }
            }
            GBC_VRAMBank = 0;
            cpu_setbank (4, GBC_VRAMMap[GBC_VRAMBank]);

            gb_chrgen = GBC_VRAMMap[0];
            gbc_chrgen = GBC_VRAMMap[1];
            gb_bgdtab = new UBytePtr(GBC_VRAMMap[0], 0x1C00);
            gb_wndtab = new UBytePtr(GBC_VRAMMap[0], 0x1C00);
            gbc_bgdtab =  new UBytePtr(GBC_VRAMMap[1], 0x1C00);
            gbc_wndtab = new UBytePtr(GBC_VRAMMap[1], 0x1C00);

            /* Initialise registers */
            gb_w_io.handler(0x6C, 0xFE );
            gb_w_io.handler( 0x72, 0x00 );
            gb_w_io.handler( 0x73, 0x00 );
            gb_w_io.handler( 0x74, 0x8F );
            gb_w_io.handler( 0x75, 0x00 );
            gb_w_io.handler( 0x76, 0x00 );
            gb_w_io.handler( 0x77, 0x00 );

            /* Are we in colour or mono mode? */
            if( gb_ram.read(0x143) == 0x80 || gb_ram.read(0x143) == 0xC0 )
                    gbc_mode = GBC_MODE_GBC;
            else
                    gbc_mode = GBC_MODE_MONO;

            /* HDMA disabled */
            gbc_hdma_enabled = 0;

            /* set the scanline refresh function */
            refresh_scanline = gbc_refresh_scanline;
        }
    };

    public static StopMachinePtr gb_shutdown_machine = new StopMachinePtr() {
        public void handler() {
        
            int I;
            UBytePtr battery_ram, ptr;

            /* Don't save if there was no battery */
            if( (CartType & BATTERY) == 0)
                    return;

            /* NOTE: The reason we save the carts RAM this way instead of using MAME's
               built in macros is because they force the filename to be the name of
               the machine.  We need to have a separate name for each game. */
            battery_ram = new UBytePtr( RAMBanks * 0x2000 );
            if( battery_ram != null )
            {
                    ptr = battery_ram;
                    for( I = 0; I < RAMBanks; I++ )
                    {
                            memcpy( ptr, RAMMap[I], 0x2000 );
                            ptr.inc( 0x2000 );
                    }
/*TODO*///                    battery_save( device_filename(IO_CARTSLOT, 0), battery_ram, RAMBanks * 0x2000 );

                    battery_ram = null;
            }

            /* We should release memory here, but this function is called upon reset
               and we don't reload the rom, so we're going to have to leak for now. */
    /*	for( I = 0; I < RAMBanks; I++ )
            {
                    free( RAMMap[I] );
            }
            for( I = 0; I < ROMBanks; I++ )
            {
                    free( ROMMap[I] );
            }*/
        }
    };

    public static WriteHandlerPtr gb_rom_bank_select = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
        
            /* No need to bank switch if there is no controller */
            if( MBCType == 0 )
                    return;

            data &= ROMMask;

            /* Selecting bank 0 == selecting bank 1 except with an MBC5 */
            if( data==0 && MBCType != MBC5 )
                    data = 1;

            if( ROMMask != 0 )
            {
                    switch( MBCType )
                    {
                    case MBC1:
                            ROMBank = data & 0x1F; /* Only uses lower 5 bits */
                            break;
                    case MBC2:
                            /* The least significant bit of the upper address byte must be 1 */
                            if(( offset & 0x0100 ) != 0)
                                    ROMBank = data;
                            break;
                    case MBC3:
                    case HUC1:	/* NOTE: Probably wrong */
                    case HUC3:	/* NOTE: Probably wrong */
                            ROMBank = data;
                            break;
                    case MBC5:
                            /* MBC5 has a 9 bit bank select */
                            if( offset < 0x1000 )
                            {
                                    ROMBank = (ROMBank & 0x100 ) | data;
                            }
                            else
                            {
                                    ROMBank = (ROMBank & 0xFF ) | ((data & 0x1) << 8) & 0xffff;
                            }
                            break;
                    }

                    /* Switch banks */
                    cpu_setbank (1, ROMMap[ROMBank]!=null ? ROMMap[ROMBank] : new UBytePtr(gb_ram, 0x4000));
                    if ((Verbose & 0x08) != 0)
                            printf ("ROM: Bank %d selected\n", ROMBank);
            }
        }
    };

    public static WriteHandlerPtr gb_ram_bank_select = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
        
            /* No need to bank switch if no controller or MBC2 */
            if( MBCType==0 || MBCType == MBC2 )
                    return;

            data &= RAMMask;
            if( RAMMask != 0 )
            {
                    switch( MBCType )
                    {
                    case MBC1:
                            data &= 0x3; /* Only uses the lower 2 bits */
                            if( MBC1Mode != 0 )
                            {
                                    /* Select the upper bits of the ROMMask */
                                    ROMBank |= data << 5;
                                    cpu_setbank (1, ROMMap[ROMBank]!=null ? ROMMap[ROMBank] : new UBytePtr(gb_ram, 0x4000));
                                    return;
                            }
                            else
                            {
                                    RAMBank = data;
                            }
                            break;
                    case MBC3:
                            if(( data & 0x8 ) != 0)	/* RTC banks */
                            {
                                    MBC3RTCBank = (data & 0xf) - 8;
                                    cpu_setbank (2, new UBytePtr(MBC3RTCMap[MBC3RTCBank]));
                                    return;
                            }
                            else	/* RAM banks */
                            {
                                    RAMBank = data & 0x3;
                            }
                            break;
                    case MBC5:
                            if(( CartType & RUMBLE ) != 0)
                            {
                                    logerror( "Rumble motor: %s\n", (data & 0x8)!=0 ? "On" : "Off" );
                                    data &= 0x7;
                            }
                            RAMBank = data;
                            break;
                    }

                    /* Switch banks */
                    cpu_setbank (2, RAMMap[RAMBank]!=null ? RAMMap[RAMBank] : new UBytePtr(gb_ram, 0xA000));
                    if ((Verbose & 0x08) != 0)
                            printf ("RAM: Bank %d selected\n", RAMBank);
            }
        }
    };

    public static WriteHandlerPtr gb_mem_mode_select = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
        
            switch( MBCType )
            {
                    case MBC1:
                            MBC1Mode = data & 0x1;
                            break;
                    case MBC3:
                            if(( CartType & TIMER ) != 0)
                            {
                                    /* FIXME: RTC Latch goes here */
                            }
                            break;
            }
        }
    };
    
    static int timer_shifts[]={10, 4, 6, 8};

    public static WriteHandlerPtr gb_w_io = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
        
            offset += 0xFF00;

            switch (offset)
            {
            case 0xFF00:						/* JOYP - Joypad */
                    JOYPAD( 0xCF | data );
                    if ((data & 0x20)==0)
                            JOYPAD( JOYPAD() & (readinputport (0) >> 4) | 0xF0 );
                    if ((data & 0x10)==0)
                            JOYPAD( JOYPAD() & readinputport (0) | 0xF0 );
                    return;
            case 0xFF01:						/* SB - Serial transfer data */
                    break;
            case 0xFF02:						/* SC - SIO control */
                    if ((data & 0x81) == 0x81)		/* internal clock && enable */
                    {
                            SIODATA( 0xFF );
                            SIOCount = 8;
                    }
                    else							/* external clock || disable */
                            SIOCount = 0;
                    break;
            case 0xFF04:						/* DIV - Divider register */
                    gb_divcount = 0;
                    return;
            case 0xFF05:						/* TIMA - Timer counter */
                    gb_timer_count = data << gb_timer_shift;
                    break;
            case 0xFF07:						/* TAC - Timer control */
                    gb_timer_shift = timer_shifts[data & 0x03];
                    data |= 0xF8;
                    break;
            case 0xFF0F:						/* IF - Interrupt flag */
                    data &= 0x1F;
                    break;
            case 0xFF40:						/* LCDC - LCD Control */
                    gb_chrgen = new UBytePtr(gb_ram, ((data & 0x10)!=0 ? 0x8000 : 0x8800));
                    gb_tile_no_mod = (data & 0x10)!=0 ? 0x00 : 0x80;
                    gb_bgdtab = new UBytePtr(gb_ram, ((data & 0x08)!=0 ? 0x9C00 : 0x9800));
                    gb_wndtab = new UBytePtr(gb_ram, ((data & 0x40)!=0 ? 0x9C00 : 0x9800));
                    break;
            case 0xFF41:						/* STAT - LCD Status */
                    data = (data & 0xF8) | (LCDSTAT() & 0x07);
                    break;
            case 0xFF44:						/* LY - LCD Y-coordinate */
                    data = 0;
                    break;
            case 0xFF46:						/* DMA - DMA Transfer and Start Address */
                    {
                            UBytePtr P = new UBytePtr(gb_ram, 0xFE00);
                            offset = (data << 8) & 0xffff;
                            for (data = 0; data < 0xA0; data++)
                                    P.writeinc( cpu_readmem16 (offset++) );
                    }
                    return;
            case 0xFF47:						/* BGP - Background Palette */
                    gb_bpal[0]=(char) (data & 0x3);
                    gb_bpal[1]=(char) ((data & 0xC) >> 2);
                    gb_bpal[2]=(char) ((data & 0x30) >> 4);
                    gb_bpal[3]=(char) ((data & 0xC0) >> 6);
                    break;
            case 0xFF48:						/* OBP0 - Object Palette 0 */
                    gb_spal0[0]=data & 0x3;
                    gb_spal0[1]=(data & 0xC) >> 2;
                    gb_spal0[2]=(data & 0x30) >> 4;
                    gb_spal0[3]=(data & 0xC0) >> 6;
                    break;
            case 0xFF49:						/* OBP1 - Object Palette 1 */
                    gb_spal1[0]=data & 0x3;
                    gb_spal1[1]=(data & 0xC) >> 2;
                    gb_spal1[2]=(data & 0x30) >> 4;
                    gb_spal1[3]=(data & 0xC0) >> 6;
                    break;
            default:
                    /* Sound Registers */
                    if ((offset >= 0xFF10) && (offset <= 0xFF26))
                    {
                            gameboy_sound_w.handler(offset, data);
                            return;
                    }
            }
            gb_ram.write(offset, data);
        }
    };

    static String sgbcmds[]= 
    {
            "PAL01   ",
            "PAL23   ",
            "PAL03   ",
            "PAL12   ",
            "ATTR_BLK",
            "ATTR_LIN",
            "ATTR_DIV",
            "ATTR_CHR",
            "SOUND   ",
            "SOU_TRN ",
            "PAL_SET ",
            "PAL_TRN ",
            "ATRC_EN ",
            "TEST_EN ",
            "ICON_EN ",
            "DATA_SND",
            "DATA_TRN",
            "MLT_REG ",
            "JUMP    ",
            "CHR_TRN ",
            "PCT_TRN ",
            "ATTR_TRN",
            "ATTR_SET",
            "MASK_EN ",
            "OBJ_TRN ",
            "????????",
    };
    
    static int sgb_bitcount = 0, sgb_bytecount = 0, sgb_start = 0, sgb_rest = 0;
    static int sgb_controller_no = 0, sgb_controller_mode = 0;
    static int sgb_packets = -1;
    static int[] sgb_data = new int[112];
    static int sgb_atf;

    public static WriteHandlerPtr sgb_w_io  = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
        
            offset += 0xFF00;

            switch( offset )
            {
                    case 0xFF00:
                            switch (data & 0x30)
                            {
                            case 0x00:				   /* start condition */
                                    if (sgb_start != 0)
                                            logerror("SGB: Start condition before end of transfer ??");
                                    sgb_bitcount = 0;
                                    sgb_start = 1;
                                    sgb_rest = 0;
                                    JOYPAD( 0x0F & ((readinputport (0) >> 4) | readinputport (0) | 0xF0) );
                                    break;
                            case 0x10:				   /* data true */
                                    if (sgb_rest != 0)
                                    {
                                            /* We should test for this case , but the code below won't
                                               work with the current setup */
                                            /* if (sgb_bytecount == 16)
                                            {
                                                    logerror("SGB: end of block is not zero!");
                                                    sgb_start = 0;
                                            }*/
                                            sgb_data[sgb_bytecount] >>= 1;
                                            sgb_data[sgb_bytecount] |= 0x80;
                                            sgb_bitcount++;
                                            if (sgb_bitcount == 8)
                                            {
                                                    sgb_bitcount = 0;
                                                    sgb_bytecount++;
                                            }
                                            sgb_rest = 0;
                                    }
                                    JOYPAD( 0x1F & ((readinputport (0) >> 4) | 0xF0) );
                                    break;
                            case 0x20:				   /* data false */
                                    if (sgb_rest != 0)
                                    {
                                            if( sgb_bytecount == 16 && sgb_packets == -1 )
                                            {
                                                    if( Verbose != 0 )
                                                    {
                                                            logerror("SGB: %s (%02X) pkts: %d data: %02X %02X %02X %02X %02X %02X %02X %02X %02X %02X %02X %02X %02X %02X %02X\n",
                                                                            sgbcmds[sgb_data[0] >> 3],sgb_data[0] >> 3, sgb_data[0] & 0x07, sgb_data[1], sgb_data[2], sgb_data[3],
                                                                            sgb_data[4], sgb_data[5], sgb_data[6], sgb_data[7],
                                                                            sgb_data[8], sgb_data[9], sgb_data[10], sgb_data[11],
                                                                            sgb_data[12], sgb_data[13], sgb_data[14], sgb_data[15]);
                                                    }
                                                    sgb_packets = sgb_data[0] & 0x07;
                                                    sgb_start = 0;
                                            }
                                            if (sgb_bytecount == (sgb_packets << 4) )
                                            {
                                                    switch( sgb_data[0] >> 3 )
                                                    {
                                                            case 0x00:	/* PAL01 */
                                                                    Machine.remapped_colortable.write(0*4 + 0, sgb_data[1] | (sgb_data[2] << 8));
                                                                    Machine.remapped_colortable.write(0*4 + 1, sgb_data[3] | (sgb_data[4] << 8));
                                                                    Machine.remapped_colortable.write(0*4 + 2,  sgb_data[5] | (sgb_data[6] << 8));
                                                                    Machine.remapped_colortable.write(0*4 + 3,  sgb_data[7] | (sgb_data[8] << 8));
                                                                    Machine.remapped_colortable.write(1*4 + 0,  sgb_data[1] | (sgb_data[2] << 8));
                                                                    Machine.remapped_colortable.write(1*4 + 1,  sgb_data[9] | (sgb_data[10] << 8));
                                                                    Machine.remapped_colortable.write(1*4 + 2,  sgb_data[11] | (sgb_data[12] << 8));
                                                                    Machine.remapped_colortable.write(1*4 + 3,  sgb_data[13] | (sgb_data[14] << 8));
                                                                    break;
                                                            case 0x01:	/* PAL23 */
                                                                    Machine.remapped_colortable.write(2*4 + 0,  sgb_data[1] | (sgb_data[2] << 8));
                                                                    Machine.remapped_colortable.write(2*4 + 1,  sgb_data[3] | (sgb_data[4] << 8));
                                                                    Machine.remapped_colortable.write(2*4 + 2,  sgb_data[5] | (sgb_data[6] << 8));
                                                                    Machine.remapped_colortable.write(2*4 + 3,  sgb_data[7] | (sgb_data[8] << 8));
                                                                    Machine.remapped_colortable.write(3*4 + 0,  sgb_data[1] | (sgb_data[2] << 8));
                                                                    Machine.remapped_colortable.write(3*4 + 1,  sgb_data[9] | (sgb_data[10] << 8));
                                                                    Machine.remapped_colortable.write(3*4 + 2,  sgb_data[11] | (sgb_data[12] << 8));
                                                                    Machine.remapped_colortable.write(3*4 + 3,  sgb_data[13] | (sgb_data[14] << 8));
                                                                    break;
                                                            case 0x02:	/* PAL03 */
                                                                    Machine.remapped_colortable.write(0*4 + 0,  sgb_data[1] | (sgb_data[2] << 8));
                                                                    Machine.remapped_colortable.write(0*4 + 1,  sgb_data[3] | (sgb_data[4] << 8));
                                                                    Machine.remapped_colortable.write(0*4 + 2,  sgb_data[5] | (sgb_data[6] << 8));
                                                                    Machine.remapped_colortable.write(0*4 + 3,  sgb_data[7] | (sgb_data[8] << 8));
                                                                    Machine.remapped_colortable.write(3*4 + 0,  sgb_data[1] | (sgb_data[2] << 8));
                                                                    Machine.remapped_colortable.write(3*4 + 1,  sgb_data[9] | (sgb_data[10] << 8));
                                                                    Machine.remapped_colortable.write(3*4 + 2,  sgb_data[11] | (sgb_data[12] << 8));
                                                                    Machine.remapped_colortable.write(3*4 + 3,  sgb_data[13] | (sgb_data[14] << 8));
                                                                    break;
                                                            case 0x03:	/* PAL12 */
                                                                    Machine.remapped_colortable.write(1*4 + 0,  sgb_data[1] | (sgb_data[2] << 8));
                                                                    Machine.remapped_colortable.write(1*4 + 1,  sgb_data[3] | (sgb_data[4] << 8));
                                                                    Machine.remapped_colortable.write(1*4 + 2,  sgb_data[5] | (sgb_data[6] << 8));
                                                                    Machine.remapped_colortable.write(1*4 + 3,  sgb_data[7] | (sgb_data[8] << 8));
                                                                    Machine.remapped_colortable.write(2*4 + 0,  sgb_data[1] | (sgb_data[2] << 8));
                                                                    Machine.remapped_colortable.write(2*4 + 1,  sgb_data[9] | (sgb_data[10] << 8));
                                                                    Machine.remapped_colortable.write(2*4 + 2,  sgb_data[11] | (sgb_data[12] << 8));
                                                                    Machine.remapped_colortable.write(2*4 + 3,  sgb_data[13] | (sgb_data[14] << 8));
                                                                    break;
                                                            case 0x04:	/* ATTR_BLK */
                                                                    {
                                                                            int I, J, K, o;
                                                                            if( Verbose != 0 )
                                                                                    logerror( "\tBLOCK: %d datasets\n", sgb_data[1] );
                                                                            for( K = 0; K < sgb_data[1]; K++ )
                                                                            {
                                                                                    o = K * 6;
                                                                                    if(( sgb_data[o + 2] & 0x1 ) != 0)
                                                                                    {
                                                                                            for( I = sgb_data[ o + 4]; I <= sgb_data[o + 6]; I++ )
                                                                                            {
                                                                                                    for( J = sgb_data[o + 5]; J <= sgb_data[o + 7]; J++ )
                                                                                                    {
                                                                                                            sgb_pal_map[I][J] = sgb_data[o + 3] & 0x3;
                                                                                                    }
                                                                                            }
                                                                                    }
                                                                                    if( Verbose != 0 )
                                                                                            logerror( "\tBlock: %d,%d to %d,%d\n", sgb_data[o + 4], sgb_data[o + 5], sgb_data[o + 6], sgb_data[o + 7] );
                                                                            }
                                                                    }
                                                                    break;
                                                            case 0x05:	/* ATTR_LIN */
                                                                    {
                                                                            int J, K;
                                                                            if( Verbose != 0 )
                                                                                    logerror( "\tLINE: %d datasets\n", sgb_data[1] );
                                                                            if( sgb_data[1] > 15 ) sgb_data[1] = 15;
                                                                            for( K = 0; K < sgb_data[1]; K++ )
                                                                            {
                                                                                    if(( sgb_data[K + 1] & 0x80 ) != 0)
                                                                                    {
                                                                                            for( J = 0; J < 20; J++ )
                                                                                            {
                                                                                                    sgb_pal_map[J][sgb_data[K + 1] & 0x1f] = (sgb_data[K + 1] & 0x60) >> 5;
                                                                                            }
                                                                                    }
                                                                                    else
                                                                                    {
                                                                                            for( J = 0; J < 18; J++ )
                                                                                            {
                                                                                                    sgb_pal_map[sgb_data[K + 1] & 0x1f][J] = (sgb_data[K + 1] & 0x60) >> 5;
                                                                                            }
                                                                                    }
                                                                            }
                                                                    }
                                                                    break;
                                                            case 0x06:	/* ATTR_DIV */
                                                                    {
                                                                            int I, J;
                                                                            if( Verbose != 0 )
                                                                                    logerror( "\tDIV: %s - line %d pal1: %d pal2: %d pal3: %d\n", (sgb_data[1] & 0x40)!=0?"Vertical":"Horizontal", sgb_data[2], (sgb_data[1] & 0xc) >> 2, (sgb_data[1] & 0x30) >> 4, sgb_data[1] & 0x3 );
                                                                            if(( sgb_data[1] & 0x40 ) != 0) /* Vertical */
                                                                            {
                                                                                    for( I = 0; I < sgb_data[2]; I++ )
                                                                                    {
                                                                                            for( J = 0; J < 20; J++ )
                                                                                            {
                                                                                                    sgb_pal_map[J][I] = (sgb_data[1] & 0xC) >> 2;
                                                                                            }
                                                                                    }
                                                                                    for( J = 0; J < 20; J++ )
                                                                                    {
                                                                                            sgb_pal_map[J][sgb_data[2]] = (sgb_data[1] & 0x30) >> 4;
                                                                                    }
                                                                                    for( I = sgb_data[2] + 1; I < 18; I++ )
                                                                                    {
                                                                                            for( J = 0; J < 20; J++ )
                                                                                            {
                                                                                                    sgb_pal_map[J][I] = sgb_data[1] & 0x3;
                                                                                            }
                                                                                    }
                                                                            }
                                                                            else /* Horizontal */
                                                                            {
                                                                                    for( I = 0; I < sgb_data[2]; I++ )
                                                                                    {
                                                                                            for( J = 0; J < 18; J++ )
                                                                                            {
                                                                                                    sgb_pal_map[I][J] = (sgb_data[1] & 0xC) >> 2;
                                                                                            }
                                                                                    }
                                                                                    for( J = 0; J < 18; J++ )
                                                                                    {
                                                                                            sgb_pal_map[sgb_data[2]][J] = (sgb_data[1] & 0x30) >> 4;
                                                                                    }
                                                                                    for( I = sgb_data[2] + 1; I < 20; I++ )
                                                                                    {
                                                                                            for( J = 0; J < 18; J++ )
                                                                                            {
                                                                                                    sgb_pal_map[I][J] = sgb_data[1] & 0x3;
                                                                                            }
                                                                                    }
                                                                            }
                                                                    }
                                                                    break;
                                                            case 0x07:	/* ATTR_CHR */
                                                                    {
                                                                            int I, sets;
                                                                            int x, y;

                                                                            if( Verbose != 0 )
                                                                            {
                                                                                    logerror( "\tCHAR: %d datasets  ", sgb_data[3] | (sgb_data[4] << 8) );
                                                                                    logerror( "x: %d   y: %d\n", sgb_data[1], sgb_data[2] );
                                                                            }
                                                                            sets = (sgb_data[3] | (sgb_data[4] << 8) );
                                                                            if( sets > 360 )
                                                                                    sets = 360;
                                                                            sets >>= 2;
                                                                            sets += 6;
                                                                            x = sgb_data[1];
                                                                            y = sgb_data[2];
                                                                            if( sgb_data[5] != 0 ) /* Vertical */
                                                                            {
                                                                                    for( I = 6; I < sets; I++ )
                                                                                    {
                                                                                            sgb_pal_map[x][y++] = (sgb_data[I] & 0xC0) >> 6;
                                                                                            if( y > 17 )
                                                                                            {
                                                                                                    y = 0;
                                                                                                    x++;
                                                                                                    if( x > 19 )
                                                                                                            x = 0;
                                                                                            }

                                                                                            sgb_pal_map[x][y++] = (sgb_data[I] & 0x30) >> 4;
                                                                                            if( y > 17 )
                                                                                            {
                                                                                                    y = 0;
                                                                                                    x++;
                                                                                                    if( x > 19 )
                                                                                                            x = 0;
                                                                                            }

                                                                                            sgb_pal_map[x][y++] = (sgb_data[I] & 0xC) >> 2;
                                                                                            if( y > 17 )
                                                                                            {
                                                                                                    y = 0;
                                                                                                    x++;
                                                                                                    if( x > 19 )
                                                                                                            x = 0;
                                                                                            }

                                                                                            sgb_pal_map[x][y++] = sgb_data[I] & 0x3;
                                                                                            if( y > 17 )
                                                                                            {
                                                                                                    y = 0;
                                                                                                    x++;
                                                                                                    if( x > 19 )
                                                                                                            x = 0;
                                                                                            }
                                                                                    }
                                                                            }
                                                                            else /* horizontal */
                                                                            {
                                                                                    for( I = 6; I < sets; I++ )
                                                                                    {
                                                                                            sgb_pal_map[x++][y] = (sgb_data[I] & 0xC0) >> 6;
                                                                                            if( x > 19 )
                                                                                            {
                                                                                                    x = 0;
                                                                                                    y++;
                                                                                                    if( y > 17 )
                                                                                                            y = 0;
                                                                                            }

                                                                                            sgb_pal_map[x++][y] = (sgb_data[I] & 0x30) >> 4;
                                                                                            if( x > 19 )
                                                                                            {
                                                                                                    x = 0;
                                                                                                    y++;
                                                                                                    if( y > 17 )
                                                                                                            y = 0;
                                                                                            }

                                                                                            sgb_pal_map[x++][y] = (sgb_data[I] & 0xC) >> 2;
                                                                                            if( x > 19 )
                                                                                            {
                                                                                                    x = 0;
                                                                                                    y++;
                                                                                                    if( y > 17 )
                                                                                                            y = 0;
                                                                                            }

                                                                                            sgb_pal_map[x++][y] = sgb_data[I] & 0x3;
                                                                                            if( x > 19 )
                                                                                            {
                                                                                                    x = 0;
                                                                                                    y++;
                                                                                                    if( y > 17 )
                                                                                                            y = 0;
                                                                                            }
                                                                                    }
                                                                            }
                                                                    }
                                                                    break;
                                                            case 0x08:	/* SOUND */
                                                                    /* This command enables internal sound effects */
                                                                    /* Not Implemented */
                                                                    if( Verbose != 0 )
                                                                            logerror( "\tSOUND: Effect A: %x  Effect B: %x\n", sgb_data[1], sgb_data[2] );
                                                                    break;
                                                            case 0x09:	/* SOU_TRN */
                                                                    /* This command sends data to the SNES sound processor.
                                                                       We'll need to emulate that for this to be used */
                                                                    /* Not Implemented */
                                                                    break;
                                                            case 0x0A:	/* PAL_SET */
                                                                    {
                                                                            int index_, J, I;

                                                                            /* Palette 0 */
                                                                            index_ = (sgb_data[1] | (sgb_data[2] << 8)) * 4;
                                                                            Machine.remapped_colortable.write(0,  sgb_pal_data[index_]);
                                                                            Machine.remapped_colortable.write(1,  sgb_pal_data[index_ + 1]);
                                                                            Machine.remapped_colortable.write(2,  sgb_pal_data[index_ + 2]);
                                                                            Machine.remapped_colortable.write(3,  sgb_pal_data[index_ + 3]);
                                                                            /* Palette 1 */
                                                                            index_ = (sgb_data[3] | (sgb_data[4] << 8)) * 4;
                                                                            Machine.remapped_colortable.write(4,  sgb_pal_data[index_]);
                                                                            Machine.remapped_colortable.write(5,  sgb_pal_data[index_ + 1]);
                                                                            Machine.remapped_colortable.write(6,  sgb_pal_data[index_ + 2]);
                                                                            Machine.remapped_colortable.write(7,  sgb_pal_data[index_ + 3]);
                                                                            /* Palette 2 */
                                                                            index_ = (sgb_data[5] | (sgb_data[6] << 8)) * 4;
                                                                            Machine.remapped_colortable.write(8,  sgb_pal_data[index_]);
                                                                            Machine.remapped_colortable.write(9,  sgb_pal_data[index_ + 1]);
                                                                            Machine.remapped_colortable.write(10,  sgb_pal_data[index_ + 2]);
                                                                            Machine.remapped_colortable.write(11,  sgb_pal_data[index_ + 3]);
                                                                            /* Palette 3 */
                                                                            index_ = (sgb_data[7] | (sgb_data[8] << 8)) * 4;
                                                                            Machine.remapped_colortable.write(12,  sgb_pal_data[index_]);
                                                                            Machine.remapped_colortable.write(13,  sgb_pal_data[index_ + 1]);
                                                                            Machine.remapped_colortable.write(14,  sgb_pal_data[index_ + 2]);
                                                                            Machine.remapped_colortable.write(15,  sgb_pal_data[index_ + 3]);
                                                                            /* Attribute File */
                                                                            if(( sgb_data[9] & 0x40 ) != 0)
                                                                                    sgb_window_mask = 0;
                                                                            sgb_atf = (sgb_data[9] & 0x3f) * (18 * 5);
                                                                            if(( sgb_data[9] & 0x80 ) != 0)
                                                                            {
                                                                                    for( J = 0; J < 18; J++ )
                                                                                    {
                                                                                            for( I = 0; I < 5; I++ )
                                                                                            {
                                                                                                sgb_pal_map[I * 4][J] = (sgb_atf_data[(J * 5) + sgb_atf + I] & 0xC0) >> 6;
												sgb_pal_map[(I * 4) + 1][J] = (sgb_atf_data[(J * 5) + sgb_atf + I] & 0x30) >> 4;
												sgb_pal_map[(I * 4) + 2][J] = (sgb_atf_data[(J * 5) + sgb_atf + I] & 0xC) >> 2;
												sgb_pal_map[(I * 4) + 3][J] = (sgb_atf_data[(J * 5) + sgb_atf + I] & 0x3);
                                                                                            }
                                                                                    }
                                                                            }
                                                                    }
                                                                    break;
                                                            case 0x0B:	/* PAL_TRN */
                                                                    {
                                                                            int I, col;

                                                                            for( I = 0; I < 2048; I++ )
                                                                            {
                                                                                    col = cpu_readmem16 (0x8800 + (I*2));
                                                                                    col |= (cpu_readmem16 (0x8800 + (I*2) + 1)) << 8;
                                                                                    sgb_pal_data[I] = col;
                                                                            }
                                                                    }
                                                                    break;
                                                            case 0x0C:	/* ATRC_EN */
                                                                    /* Not Implemented */
                                                                    break;
                                                            case 0x0D:	/* TEST_EN */
                                                                    /* Not Implemented */
                                                                    break;
                                                            case 0x0E:	/* ICON_EN */
                                                                    /* Not Implemented */
                                                                    break;
                                                            case 0x0F:	/* DATA_SND */
                                                                    /* Not Implemented */
                                                                    break;
                                                            case 0x10:	/* DATA_TRN */
                                                                    /* Not Implemented */
                                                                    break;
                                                            case 0x11:	/* MLT_REQ - Multi controller request */
                                                                    if (sgb_data[1] == 0x00)
                                                                            sgb_controller_mode = 0;
                                                                    else if (sgb_data[1] == 0x01)
                                                                            sgb_controller_mode = 2;
                                                                    break;
                                                            case 0x12:	/* JUMP */
                                                                    /* Not Implemented */
                                                                    break;
                                                            case 0x13:	/* CHR_TRN */
                                                                    if(( sgb_data[1] & 0x1 ) != 0)
                                                                            memcpy( new UBytePtr(sgb_tile_data, 4096), new UBytePtr(gb_ram, 0x8800), 4096 );
                                                                    else
                                                                            memcpy( new UBytePtr(sgb_tile_data), new UBytePtr(gb_ram, 0x8800), 4096 );
                                                                    break;
                                                            case 0x14:	/* PCT_TRN */
                                                                    {
                                                                            int I;
                                                                            int col;
                                                                            if( sgb_hack != 0 )
                                                                            {
                                                                                    memcpy( new UBytePtr(sgb_tile_map), new UBytePtr(gb_ram, 0x9000), 2048 );
                                                                                    for( I = 0; I < 64; I++ )
                                                                                    {
                                                                                            col = cpu_readmem16 (0x8800 + (I*2));
                                                                                            col |= (cpu_readmem16 (0x8800 + (I*2) + 1)) << 8;
                                                                                            Machine.remapped_colortable.write(SGB_BORDER_PAL_OFFSET + I,  col);
                                                                                    }
                                                                            }
                                                                            else /* Do things normally */
                                                                            {
                                                                                    memcpy( new UBytePtr(sgb_tile_map), new UBytePtr(gb_ram, 0x8800), 2048 );
                                                                                    for( I = 0; I < 64; I++ )
                                                                                    {
                                                                                            col = cpu_readmem16 (0x9000 + (I*2));
                                                                                            col |= (cpu_readmem16 (0x9000 + (I*2) + 1)) << 8;
                                                                                            Machine.remapped_colortable.write(SGB_BORDER_PAL_OFFSET + I,  col);
                                                                                    }
                                                                            }
                                                                    }
                                                                    break;
                                                            case 0x15:	/* ATTR_TRN */
                                                                    memcpy( new UBytePtr(sgb_atf_data), new UBytePtr(gb_ram, 0x8800), 4050 );
                                                                    break;
                                                            case 0x16:	/* ATTR_SET */
                                                                    {
                                                                            int J, I;

                                                                            /* Attribute File */
                                                                            if(( sgb_data[1] & 0x40 ) != 0)
                                                                                    sgb_window_mask = 0;
                                                                            sgb_atf = (sgb_data[1] & 0x3f) * (18 * 5);
                                                                            for( J = 0; J < 18; J++ )
                                                                            {
                                                                                    for( I = 0; I < 5; I++ )
                                                                                    {
                                                                                            sgb_pal_map[I * 4][J]=(sgb_atf_data[(J * 5) + sgb_atf + I] & 0xC0) >> 6;
                                                                                            sgb_pal_map[(I * 4) + 1][J]=(sgb_atf_data[(J * 5) + sgb_atf + I] & 0x30) >> 4;
                                                                                            sgb_pal_map[(I * 4) + 2][J]=(sgb_atf_data[(J * 5) + sgb_atf + I] & 0xC) >> 2;
                                                                                            sgb_pal_map[(I * 4) + 3][J]=sgb_atf_data[(J * 5) + sgb_atf + I] & 0x3;
                                                                                    }
                                                                            }
                                                                    }
                                                                    break;
                                                            case 0x17:	/* MASK_EN */
                                                                    sgb_window_mask = sgb_data[1];
                                                                    break;
                                                            case 0x18:	/* OBJ_TRN */
                                                                    /* Not Implemnted */
                                                                    break;
                                                            case 0x19:	/* ? */
                                                                    /* Called by: dkl,dkl2,dkl3,zeldadx
                                                                       But I don't know what it is for. */
                                                                    /* Not Implemented */
                                                                    break;
                                                            default:
                                                                    if( Verbose != 0 )
                                                                            logerror( "\tSGB: Unknown Command!\n" );
                                                                    break;
                                                    }

                                                    sgb_start = 0;
                                                    sgb_bytecount = 0;
                                                    sgb_packets = -1;
                                            }
                                            if( sgb_start != 0 )
                                            {
                                                    sgb_data[sgb_bytecount] >>= 1;
                                                    sgb_bitcount++;
                                                    if (sgb_bitcount == 8)
                                                    {
                                                            sgb_bitcount = 0;
                                                            sgb_bytecount++;
                                                    }
                                            }
                                            sgb_rest = 0;
                                    }
                                    JOYPAD( 0x2F & (readinputport (0) | 0xF0) );
                                    break;
                            case 0x30:				   /* rest condition */
                                    if (sgb_start != 0)
                                            sgb_rest = 1;
                                    if (sgb_controller_mode != 0)
                                    {
                                            sgb_controller_no++;
                                            if (sgb_controller_no == sgb_controller_mode)
                                                    sgb_controller_no = 0;
                                            JOYPAD( 0x3F - sgb_controller_no );
                                    }
                                    else
                                            JOYPAD( 0x3F );
                                    break;
                            }
                            //return;
                    default:
                            /* we didn't handle the write, so pass it to the GB handler */
                            gb_w_io.handler(offset - 0xFF00, data );
                            //return;
            }

            gb_ram.write(offset, data);
        }
    };
    
    static int gbc_to_gb_pal[]={32767, 21140, 10570, 0};
    static int BP = 0, OP = 0;

    public static WriteHandlerPtr gbc_w_io = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
        
            

            offset += 0xFF00;

            switch( offset )
            {
                    case 0xFF40:
                            gb_chrgen = new UBytePtr(GBC_VRAMMap[0], ((data & 0x10)!=0 ? 0x0000 : 0x0800));
                            gbc_chrgen = new UBytePtr(GBC_VRAMMap[1], ((data & 0x10)!=0 ? 0x0000 : 0x0800));
                            gb_tile_no_mod = (data & 0x10)!=0 ? 0x00 : 0x80;
                            gb_bgdtab = new UBytePtr(GBC_VRAMMap[0], ((data & 0x08)!=0 ? 0x1C00 : 0x1800));
                            gbc_bgdtab = new UBytePtr(GBC_VRAMMap[1], ((data & 0x08)!=0 ? 0x1C00 : 0x1800));
                            gb_wndtab = new UBytePtr(GBC_VRAMMap[0], ((data & 0x40)!=0 ? 0x1C00 : 0x1800));
                            gbc_wndtab = new UBytePtr(GBC_VRAMMap[1], ((data & 0x40)!=0 ? 0x1C00 : 0x1800));
                            break;
                    case 0xFF47:	/* BGP - GB background palette */
                            if( gbc_mode == GBC_MODE_MONO ) /* Some GBC games are lazy and still call this */
                            {
                                    Machine.remapped_colortable.write(0,  gbc_to_gb_pal[data & 0x03]);
                                    Machine.remapped_colortable.write(1,  gbc_to_gb_pal[(data & 0x0C) >> 2]);
                                    Machine.remapped_colortable.write(2,  gbc_to_gb_pal[(data & 0x30) >> 4]);
                                    Machine.remapped_colortable.write(3,  gbc_to_gb_pal[(data & 0xC0) >> 6]);
                            }
                            break;
                    case 0xFF48:	/* OBP0 - GB Object 0 palette */
                            if( gbc_mode == GBC_MODE_MONO ) /* Some GBC games are lazy and still call this */
                            {
                                    Machine.remapped_colortable.write(4,  gbc_to_gb_pal[(data & 0x03)]);
                                    Machine.remapped_colortable.write(5,  gbc_to_gb_pal[(data & 0x0C) >> 2]);
                                    Machine.remapped_colortable.write(6,  gbc_to_gb_pal[(data & 0x30) >> 4]);
                                    Machine.remapped_colortable.write(7,  gbc_to_gb_pal[(data & 0xC0) >> 6]);
                            }
                            break;
                    case 0xFF49:	/* OBP1 - GB Object 1 palette */
                            if( gbc_mode == GBC_MODE_MONO ) /* Some GBC games are lazy and still call this */
                            {
                                    Machine.remapped_colortable.write(8,  gbc_to_gb_pal[(data & 0x03)]);
                                    Machine.remapped_colortable.write(9,  gbc_to_gb_pal[(data & 0x0C) >> 2]);
                                    Machine.remapped_colortable.write(10,  gbc_to_gb_pal[(data & 0x30) >> 4]);
                                    Machine.remapped_colortable.write(11,  gbc_to_gb_pal[(data & 0xC0) >> 6]);
                            }
                            break;
                    case 0xFF4D:	/* KEY1 - Prepare speed switch */
                            if(( data & 0x1 ) != 0)
                            {
                                    data = (gb_ram.read(offset) & 0x80)!=0?0x00:0x80;
    //				timer_set_overclock( 0, (data & 0x80)?2.0:1.0 );
                                    logerror( "Switched to %s mode.\n", (data & 0x80)!=0 ? "FAST":"NORMAL" );
                            }
                            break;
                    case 0xFF4F:	/* VBK - VRAM bank select */
                            GBC_VRAMBank = data & 0x1;
                            cpu_setbank (4, GBC_VRAMMap[GBC_VRAMBank]);
                            data |= 0xFE;
                            break;
                    case 0xFF51:	/* HDMA1 - HBL General DMA - Source High */
                            break;
                    case 0xFF52:	/* HDMA2 - HBL General DMA - Source Low */
                            data &= 0xF0;
                            break;
                    case 0xFF53:	/* HDMA3 - HBL General DMA - Destination High */
                            data &= 0x1F;
                            break;
                    case 0xFF54:	/* HDMA4 - HBL General DMA - Destination Low */
                            data &= 0xF0;
                            break;
                    case 0xFF55:	/* HDMA5 - HBL General DMA - Mode, Length */
                            if( (data & 0x80) == 0 )
                            {
                                    /* General DMA */
                                    gbc_hdma( ((data & 0x7F) + 1) * 0x10 );
                                    data = 0xff;
                            }
                            else
                            {
                                    /* H-Blank DMA */
                                    gbc_hdma_enabled = 1;
                                    data &= 0x7f;
                            }
                            break;
                    case 0xFF56:	/* RP - Infrared port */
                            break;
                    case 0xFF68:	/* BCPS - Background palette specification */
                            break;
                    case 0xFF69:	/* BCPD - background palette data */
                            if(( GBCBCPS() & 0x1 ) != 0)
                            {
    //				BP = Machine.remapped_colortable[(GBCBCPS & 0x3e) >> 1];
    //				Machine.remapped_colortable[(GBCBCPS & 0x3e) >> 1,  data + (BP & 0xFF00);
    //				Machine.remapped_colortable[((GBCBCPS & 0x38) >> 1) + ((GBCBCPS & 0x6) >> 1),  ((UINT16)(data & 0x7f) << 8) | BP;
                                    Machine.remapped_colortable.write((GBCBCPS() & 0x3e) >> 1,  ((data & 0x7f) << 8) | BP);
                            }
                            else
                            {
    //				BP = Machine.remapped_colortable[(GBCBCPS & 0x3e) >> 1];
    //				Machine.remapped_colortable[(GBCBCPS & 0x3e) >> 1,  ((data << 8) + (BP & 0xFF));
                                    BP = data;
                            }

                            if(( GBCBCPS() & 0x80 ) != 0)
                            {
                                    GBCBCPS( GBCBCPS() + 1 );
                                    GBCBCPS( GBCBCPS() & 0xBF );
                            }
                            break;
                    case 0xFF6A:	/* OCPS - Object palette specification */
                            break;
                    case 0xFF6B:	/* OCPD - Object palette data */
                            if(( GBCOCPS() & 0x1 ) != 0)
                            {
                                    Machine.remapped_colortable.write(GBC_PAL_OBJ_OFFSET + ((GBCOCPS() & 0x3e) >> 1),  ((data & 0x7f) << 8) | OP);
                            }
                            else
                                    OP = data;

                            if(( GBCOCPS() & 0x80 ) != 0)
                            {
                                    GBCOCPS( GBCOCPS() + 1 );
                                    GBCOCPS( GBCOCPS() & 0xBF );
                            }
                            break;
                    case 0xFF70:	/* SVBK - RAM bank select */
                            GBC_RAMBank = data & 0x7;
                            cpu_setbank (3, GBC_RAMMap[GBC_RAMBank]);
                            break;
                    /* Undocumented registers */
                    case 0xFF6C:
                    case 0xFF72:
                    case 0xFF73:
                    case 0xFF74:
                    case 0xFF75:
                    case 0xFF76:
                    case 0xFF77:
                            logerror( "Write to undoco'ed register: %X = %X\n", offset, data );
                            return;
                    default:
                            /* we didn't handle the write, so pass it to the GB handler */
                            gb_w_io.handler(offset - 0xFF00, data );
                            return;
            }

            gb_ram.write(offset,  data);
        }
    };

    /* Interrupt Enable register */
    public static WriteHandlerPtr gb_w_ie = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
        
            gb_ram.write(0xFFFF,  data & 0x1F);
        }
    };

    /* IO read */
    public static ReadHandlerPtr gb_r_io = new ReadHandlerPtr() {
        public int handler(int offset) {
        
            offset += 0xFF00;

            switch(offset)
            {
                    case 0xFF04:
                            return ((gb_divcount >> 8) & 0xFF);
                    case 0xFF05:
                            return (gb_timer_count >> gb_timer_shift);
                    case 0xFF00:
                    case 0xFF01:
                    case 0xFF02:
                    case 0xFF03:
                    case 0xFF06:
                    case 0xFF07:
                    case 0xFF0F:
                    case 0xFF10:
                    case 0xFF11:
                    case 0xFF12:
                    case 0xFF13:
                    case 0xFF14:
                    case 0xFF16:
                    case 0xFF17:
                    case 0xFF18:
                    case 0xFF19:
                    case 0xFF1A:
                    case 0xFF1B:
                    case 0xFF1C:
                    case 0xFF1D:
                    case 0xFF1E:
                    case 0xFF20:
                    case 0xFF21:
                    case 0xFF22:
                    case 0xFF23:
                    case 0xFF24:
                    case 0xFF25:
                    case 0xFF26:
                    case 0xFF40:
                    case 0xFF41:
                    case 0xFF42:
                    case 0xFF43:
                    case 0xFF44:
                    case 0xFF45:
                    case 0xFF47:
                    case 0xFF48:
                    case 0xFF49:
                    case 0xFF4A:
                    case 0xFF4B:
                    /* GBC specific */
                    case 0xFF4D:
                    case 0xFF4F:
                    case 0xFF51:
                    case 0xFF52:
                    case 0xFF53:
                    case 0xFF54:
                    case 0xFF55:
                    case 0xFF56:
                    case 0xFF68:
                    case 0xFF69:
                    case 0xFF6A:
                    case 0xFF6B:
                    case 0xFF70:
                            return gb_ram.read(offset);
                    default:
                            /* It seems unsupported registers return 0xFF */
                            return 0xFF;
            }
        }
    };
    
    static String CartTypes[]= 
            {
                    "ROM ONLY",
                    "ROM+MBC1",
                    "ROM+MBC1+RAM",
                    "ROM+MBC1+RAM+BATTERY",
            "UNKNOWN",
                    "ROM+MBC2",
                    "ROM+MBC2+BATTERY",
            "UNKNOWN",
                    "ROM+RAM",
                    "ROM+RAM+BATTERY",
            "UNKNOWN",
                    "ROM+MMM01",
                    "ROM+MMM01+SRAM",
                    "ROM+MMM01+SRAM+BATTERY",
            "UNKNOWN",
                    "ROM+MBC3+TIMER+BATTERY",
                    "ROM+MBC3+TIMER+RAM+BATTERY",
                    "ROM+MBC3",
                    "ROM+MBC3+RAM",
                    "ROM+MBC3+RAM+BATTERY",
            "UNKNOWN",
            "UNKNOWN",
            "UNKNOWN",
            "UNKNOWN",
            "UNKNOWN",
                    "ROM+MBC5",
                    "ROM+MBC5+RAM",
                    "ROM+MBC5+RAM+BATTERY",
                    "ROM+MBC5+RUMBLE",
                    "ROM+MBC5+RUMBLE+SRAM",
                    "ROM+MBC5+RUMBLE+SRAM+BATTERY",
                    "Pocket Camera",
                    "Bandai TAMA5",
                    /* Need heaps of unknowns here */
                    "Hudson HuC-3",
                    "Hudson HuC-1"
            };

    public static class _Companies {
        public int Code;
        public String Name;
        
        public _Companies(int Code, String Name){
            this.Code = Code;
            this.Name = Name;
        }
    }
    
    public static _Companies Companies[]= 
    {
            new _Companies(0x3301, "Nintendo"),
            new _Companies(0x7901, "Accolade"),
            new _Companies(0xA400, "Konami"),
            new _Companies(0x6701, "Ocean"),
            new _Companies(0x5601, "LJN"),
            new _Companies(0x9900, "ARC?"),
            new _Companies(0x0101, "Nintendo"),
            new _Companies(0x0801, "Capcom"),
            new _Companies(0x0100, "Nintendo"),
            new _Companies(0xBB01, "SunSoft"),
            new _Companies(0xA401, "Konami"),
            new _Companies(0xAF01, "Namcot?"),
            new _Companies(0x4901, "Irem"),
            new _Companies(0x9C01, "Imagineer"),
            new _Companies(0xA600, "Kawada?"),
            new _Companies(0xB101, "Nexoft"),
            new _Companies(0x5101, "Acclaim"),
            new _Companies(0x6001, "Titus"),
            new _Companies(0xB601, "HAL"),
            new _Companies(0x3300, "Nintendo"),
            new _Companies(0x0B00, "Coconuts?"),
            new _Companies(0x5401, "Gametek"),
            new _Companies(0x7F01, "Kemco?"),
            new _Companies(0xC001, "Taito"),
            new _Companies(0xEB01, "Atlus"),
            new _Companies(0xE800, "Asmik?"),
            new _Companies(0xDA00, "Tomy?"),
            new _Companies(0xB100, "ASCII?"),
            new _Companies(0xEB00, "Atlus"),
            new _Companies(0xC000, "Taito"),
            new _Companies(0x9C00, "Imagineer"),
            new _Companies(0xC201, "Kemco?"),
            new _Companies(0xD101, "Sofel?"),
            new _Companies(0x6101, "Virgin"),
            new _Companies(0xBB00, "SunSoft"),
            new _Companies(0xCE01, "FCI?"),
            new _Companies(0xB400, "Enix?"),
            new _Companies(0xBD01, "Imagesoft"),
            new _Companies(0x0A01, "Jaleco?"),
            new _Companies(0xDF00, "Altron?"),
            new _Companies(0xA700, "Takara?"),
            new _Companies(0xEE00, "IGS?"),
            new _Companies(0x8300, "Lozc?"),
            new _Companies(0x5001, "Absolute?"),
            new _Companies(0xDD00, "NCS?"),
            new _Companies(0xE500, "Epoch?"),
            new _Companies(0xCB00, "VAP?"),
            new _Companies(0x8C00, "Vic Tokai"),
            new _Companies(0xC200, "Kemco?"),
            new _Companies(0xBF00, "Sammy?"),
            new _Companies(0x1800, "Hudson Soft"),
            new _Companies(0xCA01, "Palcom/Ultra"),
            new _Companies(0xCA00, "Palcom/Ultra"),
            new _Companies(0xC500, "Data East?"),
            new _Companies(0xA900, "Technos Japan?"),
            new _Companies(0xD900, "Banpresto?"),
            new _Companies(0x7201, "Broderbund?"),
            new _Companies(0x7A01, "Triffix Entertainment?"),
            new _Companies(0xE100, "Towachiki?"),
            new _Companies(0x9300, "Tsuburava?"),
            new _Companies(0xC600, "Tonkin House?"),
            new _Companies(0xCE00, "Pony Canyon"),
            new _Companies(0x7001, "Infogrames?"),
            new _Companies(0x8B01, "Bullet-Proof Software?"),
            new _Companies(0x5501, "Park Place?"),
            new _Companies(0xEA00, "King Records?"),
            new _Companies(0x5D01, "Tradewest?"),
            new _Companies(0x6F01, "ElectroBrain?"),
            new _Companies(0xAA01, "Broderbund?"),
            new _Companies(0xC301, "SquareSoft"),
            new _Companies(0x5201, "Activision?"),
            new _Companies(0x5A01, "Bitmap Brothers/Mindscape"),
            new _Companies(0x5301, "American Sammy"),
            new _Companies(0x4701, "Spectrum Holobyte"),
            new _Companies(0x1801, "Hudson Soft"),
            new _Companies(0x0000, null)
    };
    
    public static io_initPtr gb_load_rom = new io_initPtr() {
        public int handler(int id) {

            /*** Following are some known manufacturer codes *************************/
            int Checksum, I, J;
/*TODO*///            char *P, S[50];
            Object F;
            int rambanks[] = {0, 1, 1, 4, 16};

            for (I = 0; I < 256; I++)
                    RAMMap[I] = ROMMap[I] = null;

            if(device_filename(IO_CARTSLOT,id)==null)
            {
                    printf("Cartridge name required!\n");
                    return INIT_FAIL;
            }

            if( new_memory_region(REGION_CPU1, 0x10000,0) != 0 )
            {
                    logerror("Memory allocation failed reading roms!\n");
                    return INIT_FAIL;
            }

            gb_ram = memory_region(REGION_CPU1);
            memset (gb_ram, 0, 0x10000);

            /* FIXME should check first if a file is given, should give a more clear error */
            if ((F = image_fopen (IO_CARTSLOT, id, OSD_FILETYPE_IMAGE_R, OSD_FOPEN_READ))==null)
            {
                    logerror("image_fopen failed in gb_load_rom.\n");
                    return INIT_FAIL;
            }

    /* some tricks since we don't have a lseek, the filesize can't
       be determined easily. So we just keep reading into the same buffer untill
       the reads fails and then check if we have 512 bytes too much, so its a file
       with header or not */

        for (J = 0x4000; J == 0x4000;)
                    J = osd_fread (F, gb_ram, 0x4000);

            osd_fclose (F);

            /* FIXME: should check first if a file is given, should give a more clear error */
            if ((F = image_fopen (IO_CARTSLOT, id, OSD_FILETYPE_IMAGE_R, OSD_FOPEN_READ))==null)
            {
                    logerror("image_fopen failed in gb_load_rom.\n");
                    return INIT_FAIL;
            }

            if (J == 512)
            {
                    logerror("ROM-header found skipping\n");
                    osd_fread (F, gb_ram, 512);
            }

            if (osd_fread (F, gb_ram, 0x4000) != 0x4000)
            {
                    logerror("Error while reading from file: %s\n", device_filename(IO_CARTSLOT,id));
                    osd_fclose (F);
                    return INIT_FAIL;
            }

            ROMMap[0] = new UBytePtr(gb_ram);
            ROMBanks = 2 << gb_ram.read(0x0148);
            RAMBanks = rambanks[gb_ram.read(0x0149) & 3];
            Checksum = ( gb_ram.read(0x014E) << 8)&0xffff + gb_ram.read(0x014F);
            /* Fill in our cart details */
            switch( gb_ram.read(0x0147) )
            {
                    case 0x00:
                            MBCType = NONE;
                            CartType = 0;
                            break;
                    case 0x01:
                            MBCType = MBC1;
                            CartType = 0;
                            break;
                    case 0x02:
                            MBCType = MBC1;
                            CartType = RAM;
                            break;
                    case 0x03:
                            MBCType = MBC1;
                            CartType = RAM | BATTERY;
                            break;
                    case 0x05:
                            MBCType = MBC2;
                            CartType = 0;
                            break;
                    case 0x06:
                            MBCType = MBC2;
                            CartType = BATTERY;
                            break;
                    case 0x08:
                            MBCType = NONE;
                            CartType = RAM;
                    case 0x09:
                            MBCType = NONE;
                            CartType = RAM | BATTERY;
                            break;
                    case 0x0F:
                            MBCType = MBC3;
                            CartType = TIMER | BATTERY;
                            break;
                    case 0x10:
                            MBCType = MBC3;
                            CartType = TIMER | RAM | BATTERY;
                            break;
                    case 0x11:
                            MBCType = MBC3;
                            CartType = 0;
                            break;
                    case 0x12:
                            MBCType = MBC3;
                            CartType = RAM;
                            break;
                    case 0x13:
                            MBCType = MBC3;
                            CartType = RAM | BATTERY;
                            break;
                    case 0x19:
                            MBCType = MBC5;
                            CartType = 0;
                            break;
                    case 0x1A:
                            MBCType = MBC5;
                            CartType = RAM;
                            break;
                    case 0x1B:
                            MBCType = MBC5;
                            CartType = RAM | BATTERY;
                            break;
                    case 0x1C:
                            MBCType = MBC5;
                            CartType = RUMBLE;
                            break;
                    case 0x1D:
                            MBCType = MBC5;
                            CartType = RUMBLE | SRAM;
                            break;
                    case 0x1E:
                            MBCType = MBC5;
                            CartType = RUMBLE | SRAM | BATTERY;
                            break;
                    case 0xFE:
                            MBCType = HUC3;
                            CartType = 0;
                            break;
                    case 0xFF:
                            MBCType = HUC1;
                            CartType = 0;
                            break;
                    default:
                            MBCType = NONE;
                            CartType = UNKNOWN;
            }

            if (( CartType & UNKNOWN ) != 0)
            {
                    logerror("Error loading cartridge: Unknown ROM type\n");
                    osd_fclose (F);
                    return INIT_FAIL;
            }

            if (Verbose != 0)
            {
/*TODO*///                    strncpy (S, (char *)&gb_ram[0x0134], 16);
/*TODO*///                    S[16,  '\0';
/*TODO*///                    logerror("OK\n  Name: %s\n", S);
/*TODO*///                    logerror("  Type: %s [%Xh]\n", CartTypes[gb_ram[0x0147]], gb_ram[0x0147] );
/*TODO*///                    logerror("  GameBoy : %s\n", (gb_ram[0x0143, = 0xc0) ? "No" : "Yes" );
/*TODO*///                    logerror("  Super GB: %s [%Xh]\n", (gb_ram[0x0146, = 0x03) ? "Yes" : "No", gb_ram[0x0146] );
/*TODO*///                    logerror("  Color GB: %s [%Xh]\n", (gb_ram[0x0143, = 0x80 || gb_ram[0x0143, = 0xc0) ? "Yes" : "No", gb_ram[0x0143] );
/*TODO*///                    logerror("  ROM Size: %d 16kB Banks [%X]\n", ROMBanks, gb_ram[0x0148]);
/*TODO*///                    J = (gb_ram[0x0149] & 0x03) * 2;
/*TODO*///                    J = J ? (1 << (J - 1)) : 0;
/*TODO*///                    logerror("  RAM Size: %d kB [%X]\n", J, gb_ram[0x0149]);
/*TODO*///                    logerror("  License code %X%Xh\n", gb_ram[0x0145], gb_ram[0x0144] );
/*TODO*///                    J = ((UINT16) gb_ram[0x014B] << 8) + gb_ram[0x014A];
/*TODO*///                    for (I = 0, P = null; !P && Companies[I].Name; I++)
/*TODO*///                            if (J == Companies[I].Code)
/*TODO*///                                    P = Companies[I].Name;
/*TODO*///                    logerror("  Manufacturer ID: %Xh", J);
/*TODO*///                    logerror(" [%s]\n", P ? P : "?");
/*TODO*///
/*TODO*///                    logerror("  Version Number: %Xh\n", gb_ram[0x014C]);
/*TODO*///                    logerror("  Complement Check: %Xh\n", gb_ram[0x014D]);
/*TODO*///                    logerror("  Checksum: %Xh\n", Checksum);
/*TODO*///                    J = ((UINT16) gb_ram[0x0103] << 8) + gb_ram[0x0102];
/*TODO*///                    logerror("  Start Address: %Xh\n", J);
            }

            Checksum += gb_ram.read(0x014E) + gb_ram.read(0x014F);
            for (I = 0; I < 0x4000; I++)
                    Checksum -= gb_ram.read(I);

            if (Verbose != 0)
                    logerror("Loading %dx16kB ROM banks:.", ROMBanks);
            for (I = 1; I < ROMBanks; I++)
            {
                    if ((ROMMap[I] =  new UBytePtr(0x4000)) != null)
                    {
                            if (osd_fread (F, ROMMap[I], 0x4000) == 0x4000)
                            {
                                    for (J = 0; J < 0x4000; J++)
                                            Checksum -= ROMMap[I].read(J);
/*TODO*///                                    if (Verbose!=0)
/*TODO*///                                            putchar ('.');
                            }
                            else
                            {
                                    logerror("Error while reading from file: %s\n", device_filename(IO_CARTSLOT,id));
                                    break;
                            }
                    }
                    else
                    {
                            logerror("Error allocating memory\n");
                            break;
                    }
            }

            osd_fclose (F);
            if (I < ROMBanks)
                    return INIT_FAIL;

/*TODO*///            if (CheckCRC!=0 && (Checksum & 0xFFFF)!=0)
/*TODO*///            {
/*TODO*///                    logerror("Error loading cartridge: Checksum is wrong");
/*TODO*///                    return INIT_FAIL;
/*TODO*///            }

            /* MBC2 has 512 * 4bits (8kb) internal RAM */
            if( MBCType == MBC2 )
                    RAMBanks = 1;

            if (RAMBanks!=0 && MBCType!=0)
            {
                    for (I = 0; I < RAMBanks; I++)
                    {
                            if ((RAMMap[I] = new UBytePtr(0x2000)) != null)
                                    memset (RAMMap[I], 0, 0x2000);
                            else
                            {
                                    logerror("Error alocating memory\n");
                                    return INIT_FAIL;
                            }
                    }
            }

            /* Load the saved RAM if this cart has a battery */
            if(( CartType & BATTERY ) != 0 )
            {
                    UBytePtr battery_ram, ptr;
                    battery_ram = new UBytePtr( RAMBanks * 0x2000 );
                    if( battery_ram != null )
                    {
                            battery_load( device_filename(IO_CARTSLOT,id), battery_ram, RAMBanks * 0x2000 );
                            ptr = battery_ram;
                            for( I = 0; I < RAMBanks; I++ )
                            {
                                    memcpy( RAMMap[I], ptr, 0x2000 );
                                    ptr.inc( 0x2000 );
                            }

                            battery_ram = null;
                    }
            }

            /* Build rom bank Mask */
            if (ROMBanks < 3)
                    ROMMask = 0;
            else
            {
                    for (I = 1; I < ROMBanks; I <<= 1) ;
                    ROMMask = I - 1;
            }
            /* Build ram bank Mask */
            if (RAMMap[0] == null)
                    RAMMask = 0;
            else
            {
                    for (I = 1; I < RAMBanks; I <<= 1) ;
                    RAMMask = I - 1;
            }

            return INIT_PASS;
        }
    };
    
    static int count = 0;

    public static InterruptPtr gb_scanline_interrupt = new InterruptPtr() {
        public int handler() {
        
            /* This is a little dodgy, but it works... mostly */
            count = (count + 1) % 3;
            if ( count != 0 )
                    return 0;

            /* First let's draw the current scanline */
            if (CURLINE() < 144)
                    refresh_scanline.handler();

            /* The rest only makes sense if the display is enabled */
            if ((LCDCONT() & 0x80) != 0)
            {
                    if (CURLINE() == CMPLINE())
                    {
                            LCDSTAT( LCDSTAT() | 0x04 );
                            /* Generate lcd interrupt if requested */
                            if(( LCDSTAT() & 0x40 ) != 0)
                                    cpu_set_irq_line(0, LCD_INT, HOLD_LINE);
                    }
                    else
                            LCDSTAT( LCDSTAT() & 0xFB );

                    if (CURLINE() < 144)
                    {
                            /* Set Mode 2 lcdstate */
                            LCDSTAT( (LCDSTAT() & 0xFC) | 0x02 );
                            /* Generate lcd interrupt if requested */
                            if ((LCDSTAT() & 0x20) != 0)
                                    cpu_set_irq_line(0, LCD_INT, HOLD_LINE);

                            /* First  lcdstate change after aprox 19 uS */
                            timer_set (19.0 / 1000000.0, 0, gb_scanline_interrupt_set_mode3);
                            /* Second lcdstate change after aprox 60 uS */
                            timer_set (60.0 / 1000000.0, 0, gb_scanline_interrupt_set_mode0);
                    }
                    else
                    {
                            /* Generate VBlank interrupt */
                            if (CURLINE() == 144)
                            {
                                    /* Cause VBlank interrupt */
                                    cpu_set_irq_line(0, VBL_INT, HOLD_LINE);
                                    /* Set VBlank lcdstate */
                                    LCDSTAT( (LCDSTAT() & 0xFC) | 0x01 );
                                    /* Generate lcd interrupt if requested */
                                    if(( LCDSTAT() & 0x10 ) != 0 )
                                            cpu_set_irq_line(0, LCD_INT, HOLD_LINE);
                            }
                    }

                    CURLINE( (CURLINE() + 1) % 154 );
            }

            /* Generate serial IO interrupt */
            if (SIOCount != 0)
            {
                    SIODATA( (SIODATA() << 1) | 0x01 );
                    if (--SIOCount == 0)
                    {
                            SIOCONT( SIOCONT() & 0x7F );
                            cpu_set_irq_line(0, SIO_INT, HOLD_LINE);
                    }
            }
            
            return 1;
        }
    };

    public static timer_callback gb_scanline_interrupt_set_mode0 = new timer_callback() {
        public void handler(int param) {
            
            /* Set Mode 0 lcdstate */
            LCDSTAT( LCDSTAT() & 0xFC );
            /* Generate lcd interrupt if requested */
            if(( LCDSTAT() & 0x08 ) != 0 )
                    cpu_set_irq_line(0, LCD_INT, HOLD_LINE);

            /* Check for HBLANK DMA */
            if( gbc_hdma_enabled!=0 && (CURLINE() < 144) )
                    gbc_hdma(0x10);
        }
    };

    public static timer_callback gb_scanline_interrupt_set_mode3 = new timer_callback() {
        public void handler(int param) {
            /* Set Mode 3 lcdstate */
            LCDSTAT( (LCDSTAT() & 0xFC) | 0x03 );
        }
    };

    public static void gbc_hdma(int length)
    {
            int src, dst;

            src = (HDMA1() << 8)&0xffff | (HDMA2() & 0xF0);
            dst = ((HDMA3() & 0x1F) << 8)&0xffff | (HDMA4() & 0xF0);
            dst |= 0x8000;
            if( Verbose != 0 )
                    printf( "\tsrc=%X  dst=%X  length=%X\n", src, dst, length );
            while( length > 0 )
            {
                    cpu_writemem16( dst++, cpu_readmem16( src++ ) );
                    length--;
            }
            HDMA1( src >> 8 );
            HDMA2( src & 0xF0 );
            HDMA3( 0x1f & (dst >> 8) );
            HDMA4( dst & 0xF0 );
            HDMA5( HDMA5()-1);
            if( (HDMA5() & 0x7f) == 0 )
            {
                    HDMA5( 0xff );
                    gbc_hdma_enabled = 0;
            }
    }
    
}
