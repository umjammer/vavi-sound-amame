/***************************************************************************

	          The book of Revelations according to LollypopMan

                                  aka
                      Machine functions for the a2600
                  The Blaggers Guide to Emu Programming

              Thanks to Cowering for the research efforts ;)

                         TODO: Better Comments ;)

***************************************************************************/


/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.machine;

import static arcadeflex056.fileio.*;
import static arcadeflex056.fucPtr.*;
import static common.ptr.*;
import static common.libc.cstdio.*;
import static common.libc.cstring.*;
import static arcadeflex056.osdepend.*;
import static arcadeflex056.video.osd_skip_this_frame;
import consoleflex056.funcPtr;
import consoleflex056.funcPtr.StopMachinePtr;
import consoleflex056.funcPtr.io_idPtr;
import consoleflex056.funcPtr.io_initPtr;
import static mame056.common.*;
import static mame056.commonH.*;
import static mame056.cpuexec.*;
import static mame056.cpuexecH.*;
import static mame056.cpuintrf.*;
import static mame056.cpuintrfH.*;
import static mame056.inptport.*;
import static mame056.inptportH.*;
import static mame056.drawgfx.*;
import static mame056.drawgfxH.*;
import static mame056.mame.*;
import static mame056.osdependH.*;
import static mess056.machine.tiaH.*;
import static mame056.timer.*;
import static mame056.timerH.*;
import static mess056.device.*;
import static mess056.deviceH.*;
import static mess056.machine.riot.*;
import static mess056.machine.riotH.*;
import static mess056.mess.*;
import static mess056.messH.*;

public class a2600
{
	
    /* for detailed logging */
    public static int TIA_VERBOSE = 0;
    public static int RIOT_VERBOSE = 0;

    /* TIA *Write* Addresses (6 bit) */

    public static final int   VSYNC	= 0x00;	/* Vertical Sync Set/Clear              */
    public static final int	VBLANK	= 0x01;    /* Vertical Blank Set/Clear	            */
    public static final int	WSYNC	= 0x02;    /* Wait for Horizontal Blank            */
    public static final int	RSYNC	= 0x03;    /* Reset Horizontal Sync Counter        */
    public static final int	NUSIZ0	= 0x04;    /* Number-Size player/missle 0	        */
    public static final int	NUSIZ1	= 0x05;    /* Number-Size player/missle 1		    */


    public static final int	COLUP0	= 0x06;    /* Color-Luminance Player 0			    */
    public static final int	COLUP1	= 0x07;    /* Color-Luminance Player 1				*/
    public static final int	COLUPF	= 0x08;    /* Color-Luminance Playfield			*/
    public static final int	COLUBK	= 0x09;    /* Color-Luminance BackGround			*/
    /*
    COLUP0, COLUP1, COLUPF, COLUBK:
    These addresses write data into the player, playfield
    and background color-luminance registers:

    COLOR           D7  D6  D5  D4 | D3  D2  D1  LUM
    grey/gold       0   0   0   0  | 0   0   0   black
                    0   0   0   1  | 0   0   1   dark grey
    orange/brt org  0   0   1   0  | 0   1   0
                    0   0   1   1  | 0   1   1   grey
    pink/purple     0   1   0   0  | 1   0   0
                    0   1   0   1  | 1   0   1
    purp/blue/blue  0   1   1   0  | 1   1   0   light grey
                    0   1   1   1  | 1   1   1   white
    blue/lt blue    1   0   0   0
                    1   0   0   1
    torq/grn blue   1   0   1   0
                    1   0   1   1
    grn/yel grn     1   1   0   0
                    1   1   0   1
    org.grn/lt org  1   1   1   0
                    1   1   1   1
    */

    public static final int	CTRLPF	= 0x0A;    /* Control Playfield, Ball, Collisions	*/
    public static final int	REFP0	= 0x0B;    /* Reflection Player 0					*/
    public static final int	REFP1	= 0x0C;    /* Reflection Player 1					*/
    public static final int     PF0	= 0x0D;    /* Playfield Register Byte 0			*/
    public static final int	PF1	= 0x0E;    /* Playfield Register Byte 1			*/
    public static final int	PF2	= 0x0F;    /* Playfield Register Byte 2			*/
    public static final int	RESP0	= 0x10;    /* Reset Player 0						*/
    public static final int	RESP1	= 0x11;    /* Reset Player 1						*/
    public static final int	RESM0	= 0x12;    /* Reset Missle 0						*/
    public static final int	RESM1	= 0x13;    /* Reset Missle 1						*/
    public static final int	RESBL	= 0x14;    /* Reset Ball							*/

    public static final int	AUDC0	= 0x15;    /* Audio Control 0						*/
    public static final int	AUDC1	= 0x16;    /* Audio Control 1						*/
    public static final int	AUDF0	= 0x17;    /* Audio Frequency 0					*/
    public static final int	AUDF1	= 0x18;    /* Audio Frequency 1					*/
    public static final int	AUDV0	= 0x19;    /* Audio Volume 0						*/
    public static final int	AUDV1	= 0x1A;    /* Audio Volume 1						*/

    /* The next 5 registers are flash registers */
    public static final int	GRP0	= 0x1B;    /* Graphics Register Player 0			*/
    public static final int	GRP1	= 0x1C;    /* Graphics Register Player 0			*/
    public static final int	ENAM0	= 0x1D;    /* Graphics Enable Missle 0				*/
    public static final int	ENAM1	= 0x1E;    /* Graphics Enable Missle 1				*/
    public static final int	ENABL	= 0x1F;    /* Graphics Enable Ball					*/


    public static final int     HMP0	= 0x20;    /* Horizontal Motion Player 0			*/
    public static final int	HMP1	= 0x21;    /* Horizontal Motion Player 0			*/
    public static final int	HMM0	= 0x22;    /* Horizontal Motion Missle 0			*/
    public static final int	HMM1	= 0x23;    /* Horizontal Motion Missle 1			*/
    public static final int	HMBL	= 0x24;    /* Horizontal Motion Ball				*/
    public static final int	VDELP0	= 0x25;    /* Vertical Delay Player 0				*/
    public static final int	VDELP1	= 0x26;    /* Vertical Delay Player 1				*/
    public static final int	VDELBL	= 0x27;    /* Vertical Delay Ball					*/
    public static final int	RESMP0	= 0x28;    /* Reset Missle 0 to Player 0			*/
    public static final int	RESMP1	= 0x29;    /* Reset Missle 1 to Player 1			*/
    public static final int	HMOVE	= 0x2A;    /* Apply Horizontal Motion				*/
    public static final int	HMCLR	= 0x2B;    /* Clear Horizontal Move Registers		*/
    public static final int	CXCLR	= 0x2C;    /* Clear Collision Latches				*/

    /* TIA *Read* Addresses */
                            /*          bit 6  bit 7				*/
    public static final int	CXM0P	= 0x00;	/* Read Collision M0-P1  M0-P0			*/
    public static final int	CXM1P	= 0x01;    /*                M1-P0  M1-P1			*/
    public static final int	CXP0FB	= 0x02;	/*                P0-PF  P0-BL			*/
    public static final int	CXP1FB	= 0x03;    /*                P1-PF  P1-BL			*/
    public static final int	CXM0FB	= 0x04;    /*                M0-PF  M0-BL			*/
    public static final int	CXM1FB	= 0x05;    /*                M1-PF  M1-BL			*/
    public static final int	CXBLPF	= 0x06;    /*                BL-PF  -----			*/
    public static final int	CXPPMM	= 0x07;    /*                P0-P1  M0-M1			*/
    public static final int	INPT0	= 0x08;    /* Read Pot Port 0						*/
    public static final int	INPT1	= 0x09;    /* Read Pot Port 1						*/
    public static final int	INPT2	= 0x0A;    /* Read Pot Port 2						*/
    public static final int	INPT3	= 0x0B;    /* Read Pot Port 3						*/
    public static final int	INPT4	= 0x0C;    /* Read Input (Trigger) 0				*/
    public static final int	INPT5	= 0x0D;    /* Read Input (Trigger) 1				*/

    /* keep a record of the colors here */
    public static class color_registers {
            public int P0; /* player 0   */
            public int M0;	/* missile 0  */
            public int P1;	/* player 1   */
            public int M1;	/* missile 1  */
            public int PF;	/* playfield  */
            public int BL;	/* ball       */
            public int BK;	/* background */
    };
    
    public static color_registers colreg = new color_registers();


    /* keep a record of the playfield registers here */
    public static class playfield_registers {
            public int B0; /* 8 bits, only left 4 bits used */
            public int B1; /* 8 bits  */
            public int B2;	/* 8 bits  */
    };
    
    public static playfield_registers pfreg = new playfield_registers();

    static int[] scanline_registers = new int[80]; /* array to hold info on who gets displayed */

    static int msize0;
    static int msize1;

    /* bitmap */
    public static mame_bitmap stella_bitmap;

    /* local */
    public static UBytePtr a2600_cartridge_rom;

    public static ReadHandlerPtr a2600_riot_a_r = new ReadHandlerPtr() {
        public int handler(int chip) {
            /* joystick !? */
        return readinputport(0);
        }
    };
    
    public static ReadHandlerPtr a2600_riot_b_r = new ReadHandlerPtr() {
        public int handler(int chip) {
            /* console switches !? */
            return readinputport(0);
        }
    };

    public static WriteHandlerPtr a2600_riot_a_w = new WriteHandlerPtr() {
        public void handler(int chip, int data) {
            /* anything? */
        }
    };
  
    public static WriteHandlerPtr a2600_riot_b_w = new WriteHandlerPtr() {
        public void handler(int chip, int data) {
            /* anything? */
        }
    };


    static RIOTinterface a2600_riot = new RIOTinterface
    (
            1,						/* number of chips */
            new int[]{ 1190000 },			/* baseclock of chip */
            new ReadHandlerPtr[]{ a2600_riot_a_r }, 	/* port a input */
            new ReadHandlerPtr[]{ a2600_riot_b_r }, 	/* port b input */
            new WriteHandlerPtr[]{ a2600_riot_a_w }, 	/* port a output */
            new WriteHandlerPtr[]{ a2600_riot_b_w }, 	/* port b output */
            new ReadHandlerPtr[]{ null }				/* interrupt callback */
    );
    
    /***************************************************************************

      TIA Reads.

    ***************************************************************************/

    public static ReadHandlerPtr a2600_TIA_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            switch (offset) {
                case CXM0P:
                case CXM1P:
                case CXP0FB:
                case CXP1FB:
                case CXM0FB:
                case CXM1FB:
                case CXBLPF:
                case CXPPMM:
/*TODO*///                                            if (errorlog && TIA_VERBOSE)
/*TODO*///                                                    fprintf(errorlog,"TIA_r - COLLISION range\n");
                            break;

                    case INPT0:	  /* offset 0x08 */
                        if ((input_port_1_r.handler(0) & 0x02)!=0)
                            return 0x80;
                        else
                            return 0x00;
                    case INPT1:	  /* offset 0x09 */
                        if ((input_port_1_r.handler(0) & 0x08)!=0)
                            return 0x80;
                        else
                            return 0x00;
                    case INPT2:	  /* offset 0x0A */
                        if ((input_port_1_r.handler(0) & 0x01)!=0)
                            return 0x80;
                        else
                            return 0x00;
                    case INPT3:	  /* offset 0x0B */
                        if ((input_port_1_r.handler(0) & 0x04)!=0)
                            return 0x80;
                        else
                            return 0x00;
                    case INPT4:	  /* offset 0x0C */
                        if ((input_port_1_r.handler(0) & 0x08)!=0 || (input_port_1_r.handler(0) & 0x02)!=0)
                            return 0x00;
                        else
                            return 0x80;
                    case INPT5:	  /* offset 0x0D */
                        if ((input_port_1_r.handler(0) & 0x01)!=0 || (input_port_1_r.handler(0) & 0x04)!=0)
                            return 0x00;
                        else
                            return 0x80;
                    default:
 /*TODO*///                       if (errorlog) fprintf(errorlog,"TIA_r undefined read %x\n",offset);

                }
        return 0xFF;
        }
    };


    /***************************************************************************

      TIA Writes.

    ***************************************************************************/

    public static WriteHandlerPtr a2600_TIA_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            UBytePtr ROM = memory_region(REGION_CPU1);


            switch (offset)
            {

                    case VSYNC:
                            if ( (data & 0x00) == 0)
                            {

/*TODO*///                                    if (errorlog && TIA_VERBOSE)
/*TODO*///                                    {
/*TODO*///                        fprintf(errorlog,"TIA_w - VSYNC Stop\n");
/*TODO*///                                    }

                            }
                            else if ((data & 0x02) != 0)
                            {

 /*TODO*///                                   if (errorlog && TIA_VERBOSE)
 /*TODO*///                                           fprintf(errorlog,"TIA_w - VSYNC Start\n");

                            }
                            else /* not allowed */
                            {
 /*TODO*///                                   if (errorlog)
 /*TODO*///                                           fprintf(errorlog,"TIA_w - VSYNC Write Error! offset $%02x & data $%02x\n", offset, data);
                            }
                break;


                    case VBLANK:     	/* offset 0x01, bits 7,6 and 1 used */
                            if ( (data & 0x00) ==0)
                            {
 /*TODO*///                                   if (errorlog && TIA_VERBOSE)
 /*TODO*///                                           fprintf(errorlog,"TIA_w - VBLANK Stop\n");
                            }
                            else if ((data & 0x02)!=0)
                            {
 /*TODO*///                                   if (errorlog && TIA_VERBOSE)
 /*TODO*///                                           fprintf(errorlog,"TIA_w - VBLANK Start\n");
                            }
                            else
                            {
/*TODO*///                                    if (errorlog)
          /*TODO*///                                  fprintf(errorlog,"TIA_w - VBLANK Write Error! offset $%02x & data $%02x\n", offset, data);
                            }
                        break;


                    case WSYNC:     	/* offset 0x02 */
                            if ( (data & 0x00) == 0 )
                            {
/*TODO*///                                    if (errorlog && TIA_VERBOSE)
/*TODO*///                                            fprintf(errorlog,"TIA_w - WSYNC \n");
                                    //cpu_spinuntil_int (); /* wait til end of scanline */
                            }
                            else
                            {
/*TODO*///                                    if (errorlog && TIA_VERBOSE)
/*TODO*///                                            fprintf(errorlog,"TIA_w - WSYNC Write Error! offset $%02x & data $%02x\n", offset, data);
                            }
                break;



                    case RSYNC:     	/* offset 0x03 */
                            if ( (data & 0x00) == 0)
                            {
/*TODO*///                                    if (errorlog && TIA_VERBOSE)
/*TODO*///                                            fprintf(errorlog,"TIA_w - RSYNC \n");
                            }
                            else
                            {
/*TODO*///                                    if (errorlog && TIA_VERBOSE)
/*TODO*///                                            fprintf(errorlog,"TIA_w - RSYNC Write Error! offset $%02x & data $%02x\n", offset, data);
                            }
                break;



                    case NUSIZ0:     	/* offset 0x04 */
                            msize0 = 2^(data>>4);
/*TODO*///                            if (errorlog)
/*TODO*///                                    fprintf(errorlog,"TIA_w - NUSIZ0, Missile Size = %d clocks at horzpos %d\n",msize0, cpu_gethorzbeampos());
                            /* must implement player size checking! */

                break;


                    case NUSIZ1:     	/* offset 0x05 */
                            msize1 = 2^(data>>4);
/*TODO*///                            if (errorlog)
/*TODO*///                                    fprintf(errorlog,"TIA_w - NUSIZ1, Missile Size = %d clocks at horzpos %d\n",msize1, cpu_gethorzbeampos());
                            /* must implement player size checking! */

                break;


                    case COLUP0:     	/* offset 0x06 */

                            colreg.P0 = data>>4;
                            colreg.M0 = colreg.P0; /* missile same color */
/*TODO*///                            if (errorlog && TIA_VERBOSE)
/*TODO*///                                    fprintf(errorlog,"TIA_w - COLUP0 Write color is $%02x\n", colreg.P0);
                break;

                    case COLUP1:     	/* offset 0x07 */

                            colreg.P1 = data>>4;
                            colreg.M1 = colreg.P1; /* missile same color */
/*TODO*///                            if (errorlog && TIA_VERBOSE)
/*TODO*///                                    fprintf(errorlog,"TIA_w - COLUP1 Write color is $%02x\n", colreg.P1);
                break;

                    case COLUPF:     	/* offset 0x08 */

                            colreg.PF = data>>4;
                            colreg.BL = data>>4;  /* ball is same as playfield */
/*TODO*///                            if (errorlog && TIA_VERBOSE)
/*TODO*///                                    fprintf(errorlog,"TIA_w - COLUPF Write color is $%02x\n", colreg.PF);
                break;

                    case COLUBK:     	/* offset 0x09 */

                            colreg.BK = data>>4;
/*TODO*///                            if (errorlog && TIA_VERBOSE)
/*TODO*///                                    fprintf(errorlog,"TIA_w - COLUBK Write color is $%02x\n", colreg.BK);
                break;


                    case CTRLPF:     	/* offset 0x0A */


/*TODO*///                                    if (errorlog && TIA_VERBOSE)
/*TODO*///                                            fprintf(errorlog,"TIA_w - CTRLPF Write offset $%02x & data $%02x\n", offset, data);

                break;



                    case REFP0:
                            if ( (data & 0x00) == 0 )
                            {
/*TODO*///                    if (errorlog && TIA_VERBOSE)
/*TODO*///                                            fprintf(errorlog,"TIA_w - REFP0 No reflect \n");
                            }
                            else if (( data & 0x08) != 0)
                            {
/*TODO*///                                    if (errorlog && TIA_VERBOSE)
/*TODO*///                                            fprintf(errorlog,"TIA_w - REFP0 Reflect \n");
                            }
                            else
                            {
/*TODO*///                    if (errorlog && TIA_VERBOSE)
/*TODO*///                                            fprintf(errorlog,"TIA_w - Write Error, REFP0 offset $%02x & data $%02x\n", offset, data);
                            }
                break;


                    case REFP1:
                            if ( (data & 0x00) == 0 )
                            {
/*TODO*///                    if (errorlog && TIA_VERBOSE)
/*TODO*///                                            fprintf(errorlog,"TIA_w - REFP1 No reflect \n");
                            }
                            else if (( data & 0x08) != 0)
                            {
/*TODO*///                                    if (errorlog && TIA_VERBOSE)
/*TODO*///                                            fprintf(errorlog,"TIA_w - REFP1 Reflect \n");
                            }
                            else
                            {
/*TODO*///                    if (errorlog && TIA_VERBOSE)
/*TODO*///                                            fprintf(errorlog,"TIA_w - Write Error, REFP1 offset $%02x & data $%02x\n", offset, data);
                            }
                break;






                    case PF0:	    /* 0x0D Playfield Register Byte 0 */
                            pfreg.B0 = data;
/*TODO*///                            if (errorlog)
/*TODO*///                                    fprintf(errorlog,"TIA_w - PF0 register is $%02x \n", pfreg.B0);
                            break;

            case PF1:		/* 0x0E Playfield Register Byte 1 */
                            pfreg.B1 = data;
/*TODO*///                            if (errorlog)
/*TODO*///                                    fprintf(errorlog,"TIA_w - PF1 register is $%02x \n", pfreg.B1);
                            break;

                    case PF2: 		/* 0x0F Playfield Register Byte 2 */
                            pfreg.B2 = data;
/*TODO*///                            if (errorlog)
/*TODO*///                                    fprintf(errorlog,"TIA_w - PF2 register is $%02x \n", pfreg.B2);
                            break;


    /* These next 5 Registers are Strobe registers            */
    /* They will need to update the screen as soon as written */

                    case RESP0: 	/* 0x10 Reset Player 0 */
                            break;

                    case RESP1: 	/* 0x11 Reset Player 1 */
                            break;

                    case RESM0:		/* 0x12 Reset Missle 0 */
                            break;

                    case RESM1: 	/* 0x13 Reset Missle 1 */
                            break;

                    case RESBL: 	/* 0x14 Reset Ball */
                            break;







                    case AUDC0: /* audio control */
                    case AUDC1: /* audio control */
                    case AUDF0: /* audio frequency */
                    case AUDF1: /* audio frequency */
                    case AUDV0: /* audio volume 0 */
                    case AUDV1: /* audio volume 1 */

/*TODO*///                            tia_w(offset,data);
                            //ROM[offset] = data;
                            break;


                    case GRP0:		/* 0x1B Graphics Register Player 0 */
                            break;

                    case GRP1:		/* 0x1C Graphics Register Player 0 */
                            break;

                    case ENAM0: 	/* 0x1D Graphics Enable Missle 0 */
                            break;

                    case ENAM1:		/* 0x1E Graphics Enable Missle 1 */
                            break;

                    case ENABL: 	/* 0x1F Graphics Enable Ball */
                            break;

                    case HMP0:		/* 0x20	Horizontal Motion Player 0 */
                            break;

                    case HMP1: 		/* 0x21 Horizontal Motion Player 0 */
                            break;

                    case HMM0:		/* 0x22 Horizontal Motion Missle 0 */
                            break;

                    case HMM1: 		/* 0x23 Horizontal Motion Missle 1 */
                            break;

                    case HMBL: 		/* 0x24 Horizontal Motion Ball */
                            break;

                    case VDELP0:	/* 0x25 Vertical Delay Player 0 */
                            break;

                    case VDELP1: 	/* 0x26 Vertical Delay Player 1 */
                            break;

                    case VDELBL: 	/* 0x27 Vertical Delay Ball	*/
                            break;

                    case RESMP0:	/* 0x28 Reset Missle 0 to Player 0 */
                            break;

                    case RESMP1:	/* 0x29 Reset Missle 1 to Player 1 */
                            break;

                    case HMOVE: 	/* 0x2A Apply Horizontal Motion	*/
                            break;

                    case HMCLR: 	/* 0x2B Clear Horizontal Move Registers */
                            break;

                    case CXCLR:		/* 0x2C Clear Collision Latches	*/
                            break;




                    default:
/*TODO*///                            if (errorlog)
/*TODO*///                                    fprintf(errorlog,"TIA_w - UNKNOWN - offset %02x & data %02x\n", offset, data);
                    /* all others */
                    ROM.write(offset, data);
            }
        }
    };
    
    public static InitMachinePtr a2600_init_machine = new InitMachinePtr() {
        public void handler() {
            /* start RIOT interface */
            riot_init(a2600_riot);
        }
    };

    public static StopMachinePtr a2600_stop_machine = new StopMachinePtr() {
        public void handler() {
        
        }
    };

    public static io_idPtr a2600_id_rom = new io_idPtr() {
        public int handler(int id) {
            return 0;		/* no id possible */
        }
    };


    public static int Bankswitch_Method = 0;
    
    public static io_initPtr a2600_load_rom = new io_initPtr() {
        public int handler(int id) {
            Object cartfile;
		UBytePtr ROM = new UBytePtr(memory_region(REGION_CPU1));
	
		if (device_filename(IO_CARTSLOT, id) == null)
		{
			printf("a2600 Requires Cartridge!\n");
			return INIT_FAIL;
		}
	
		/* A cartridge isn't strictly mandatory, but it's recommended */
		cartfile = null;
		if ((cartfile = image_fopen(IO_CARTSLOT, id, OSD_FILETYPE_IMAGE_R, 0)) == null)
		{
			return 1;
		}
	
		a2600_cartridge_rom = new UBytePtr(ROM, 0x10000);	/* Load the cart outside the cpuspace for b/s purposes */
	
		if (cartfile != null)
		{
			/*TODO*///int crc;
			int cart_size;
	
			cart_size = osd_fsize(cartfile);
			osd_fread(cartfile, a2600_cartridge_rom, cart_size);		/* testing everything now :) */
			osd_fclose(cartfile);
			/* copy to mirrorred memory regions */
			/*TODO*///crc = crc32(0L,new UBytePtr(ROM, 0x10000), cart_size);
			Bankswitch_Method = 0;
	
			switch(cart_size)
			{
				case 0x10000:
							break;
				case 0x08000:
							break;
				case 0x04000:
							break;
				case 0x03000:
							break;
				case 0x02000:
				/*TODO*///			switch(crc)
				/*TODO*///			{
	/*TODO*///
				/*TODO*///				case 0x91b8f1b2:
				/*TODO*///							Bankswitch_Method = 0xFE;
				/*TODO*///							logerror("Decathlon detected and loaded\n");
				/*TODO*///							break;
				/*TODO*///				case 0xfd8c81e5:
				/*TODO*///							Bankswitch_Method = 0xE0;
				/*TODO*///							logerror("Tooth Protectors detected and loaded\n");
				/*TODO*///							break;
				/*TODO*///				case 0x0886a55d:
				/*TODO*///							Bankswitch_Method = 0xE0;
				/*TODO*///							logerror("SW: Death Star Battle detected and loaded\n");
				/*TODO*///							break;
				/*TODO*///				case 0x0d78e8a9:
				/*TODO*///							Bankswitch_Method = 0xE0;
				/*TODO*///							logerror("Gyruss detected and loaded\n");
				/*TODO*///							break;
				/*TODO*///				case 0x34d3ffc8:
				/*TODO*///							Bankswitch_Method = 0xE0;
				/*TODO*///							logerror("James Bond 007 detected and loaded\n");
				/*TODO*///							break;
				/*TODO*///				case 0xde97103d:
				/*TODO*///							Bankswitch_Method = 0xE0;
				/*TODO*///							logerror("Super Cobra detected and loaded\n");
				/*TODO*///							break;
				/*TODO*///				case 0xec959bf2:
				/*TODO*///							Bankswitch_Method = 0xE0;
				/*TODO*///							logerror("Tutankham detected and loaded\n");
				/*TODO*///							break;
				/*TODO*///				case 0x7d287f20:
				/*TODO*///							Bankswitch_Method = 0xE0;
				/*TODO*///							logerror("Popeye detected and loaded\n");
				/*TODO*///							break;
				/*TODO*///				case 0x65c31ca4:
				/*TODO*///							Bankswitch_Method = 0xE0;
				/*TODO*///							logerror("SW: Arcade Game detected and loaded\n");
				/*TODO*///							break;
				/*TODO*///				case 0xa87be8fd:
				/*TODO*///							Bankswitch_Method = 0xE0;
				/*TODO*///							logerror("Q*Bert's Qubes detected and loaded\n");
				/*TODO*///							break;
				/*TODO*///				case 0x3ba0d9bf:
				/*TODO*///							Bankswitch_Method = 0xE0;
				/*TODO*///							logerror("Frogger ][: Threeedeep detected and loaded\n");
				/*TODO*///							break;
				/*TODO*///				case 0xe680a1c9:
				/*TODO*///							Bankswitch_Method = 0xE0;
				/*TODO*///							logerror("Montezuma's Revenge detected and loaded\n");
				/*TODO*///							break;
				/*TODO*///				case 0x044735b9:
				/*TODO*///							Bankswitch_Method = 0xE0;
				/*TODO*///							logerror("Mr. Do's Castle detected and loaded\n");
				/*TODO*///							break;
				/*TODO*///				case 0xc820bd75:
				/*TODO*///							Bankswitch_Method = 0x3F;
				/*TODO*///							logerror("River Patrol detected and loaded\n");
				/*TODO*///							break;
				/*TODO*///				case 0xdd183a4f:
				/*TODO*///							Bankswitch_Method = 0x3F;
				/*TODO*///							logerror("Springer detected and loaded\n");
				/*TODO*///							break;
				/*TODO*///				case 0xdb376663:
				/*TODO*///							Bankswitch_Method = 0x3F;
				/*TODO*///							logerror("Polaris detected and loaded\n");
				/*TODO*///							break;
				/*TODO*///				case 0xbd08d915:
				/*TODO*///							Bankswitch_Method = 0x3F;
				/*TODO*///							logerror("Miner 2049'er detected and loaded\n");
				/*TODO*///							break;
				/*TODO*///				case 0xbfa477cd:
				/*TODO*///							Bankswitch_Method = 0x3F;
				/*TODO*///							logerror("Miner 2049'er Volume ][ detected and loaded\n");
				/*TODO*///							break;
				/*TODO*///				case 0x34b80a97:
				/*TODO*///							Bankswitch_Method = 0x3F;
				/*TODO*///							logerror("Espial detected and loaded\n");
				/*TODO*///							break;
				/*TODO*///				default:
											Bankswitch_Method = 0xf8;
				/*TODO*///							break;
				/*TODO*///			}
	
							switch(Bankswitch_Method)
							{
								case 0xf8:
											memcpy(new UBytePtr(ROM, 0x1000), new UBytePtr(ROM, 0x11000), 0x1000);
											memcpy(new UBytePtr(ROM, 0xf000), new UBytePtr(ROM, 0x11000), 0x1000);
											break;
								case 0xe0:
											memcpy(new UBytePtr(ROM, 0x1000), new UBytePtr(ROM, 0x10000), 0x1000);
											memcpy(new UBytePtr(ROM, 0x1400), new UBytePtr(ROM, 0x10400), 0x1000);
											memcpy(new UBytePtr(ROM, 0x1800), new UBytePtr(ROM, 0x10800), 0x1000);
											memcpy(new UBytePtr(ROM, 0x1c00), new UBytePtr(ROM, 0x11c00), 0x1000);
	
											memcpy(new UBytePtr(ROM, 0xf000), new UBytePtr(ROM, 0x10000), 0x1000);
											memcpy(new UBytePtr(ROM, 0xf400), new UBytePtr(ROM, 0x10400), 0x1000);
											memcpy(new UBytePtr(ROM, 0xf800), new UBytePtr(ROM, 0x10800), 0x1000);
											memcpy(new UBytePtr(ROM, 0xfc00), new UBytePtr(ROM, 0x11c00), 0x1000);
											break;
							}
							break;
				case 0x01000:
							memcpy(new UBytePtr(ROM, 0x1000), new UBytePtr(ROM, 0x10000), 0x1000);
							memcpy(new UBytePtr(ROM, 0xf000), new UBytePtr(ROM, 0x10000), 0x1000);
							break;
				case 0x00800:
							memcpy(new UBytePtr(ROM, 0x1000), new UBytePtr(ROM, 0x10000), 0x0800);
							memcpy(new UBytePtr(ROM, 0x1800), new UBytePtr(ROM, 0x10000), 0x0800);
							memcpy(new UBytePtr(ROM, 0xf000), new UBytePtr(ROM, 0x10000), 0x0800);
							memcpy(new UBytePtr(ROM, 0xf800), new UBytePtr(ROM, 0x10000), 0x0800);
							break;
			}
			/*TODO*///logerror("cartridge crc = %08x\n", crc);
		}
		else
		{
			return 1;
		}
	
		return 0;

        }
    };


    /* Video functions for the a2600         */
    /* Since all software drivern, have here */


    /***************************************************************************

      Start the video hardware emulation.

    ***************************************************************************/
    public static VhStartPtr a2600_vh_start = new VhStartPtr() {
        public int handler() {
            if ((stella_bitmap = bitmap_alloc(Machine.drv.screen_width,Machine.drv.screen_height)) == null)
                    return 1;
            return 0;
        }
    };
  
    public static VhStopPtr a2600_vh_stop = new VhStopPtr() {
        public void handler() {
            stella_bitmap = null;
        }
    };

    /* when called, update the bitmap. */
    public static InterruptPtr a2600_scanline_interrupt = new InterruptPtr() {
        public int handler() {
            int regpos, pixpos;
            int xs = Machine.visible_area.min_x;//68;
            int ys = Machine.visible_area.max_y;//228;
            int currentline =  cpu_getscanline();
            int backcolor;

            /* plot the playfield and background for now               */
            /* each register value is 4 color clocks                   */
            /* to pick the color, need to bit check the playfield regs */

            /* set color to background */
            backcolor=colreg.BK;

            /* check PF register 0 (left 4 bits only) */

            //pfreg.P0




            /* now we have color, plot for 4 color cycles */
            for (regpos=xs;regpos<ys;regpos=regpos+=4)
            {
                    for (pixpos = regpos;pixpos<regpos+4;pixpos++)
                            plot_pixel.handler(stella_bitmap, pixpos, currentline, Machine.pens[backcolor]);
            }





            return 0;
        }
    };


    /***************************************************************************

      Refresh the video screen

    ***************************************************************************/
    /* This routine is called at the start of vblank to refresh the screen */
    public static VhUpdatePtr a2600_vh_screenrefresh = new VhUpdatePtr() {
        public void handler(mame_bitmap bitmap, int full_refresh) {
            /*TODO*///            if (errorlog)
            /*TODO*///                    fprintf(errorlog,"SCREEN UPDATE CALLED\n");

            copybitmap(bitmap,stella_bitmap,0,0,0,0,Machine.visible_area,TRANSPARENCY_NONE,0);
        }
    };
    
	
}
