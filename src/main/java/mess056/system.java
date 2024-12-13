/******************************************************************************

  driver.c

  The list of all available drivers. Drivers have to be included here to be
  recognized by the executable.

  To save some typing, we use a hack here. This file is recursively #included
  twice, with different definitions of the DRIVER() macro. The first one
  declares external references to the drivers; the second one builds an array
  storing all the drivers.

******************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056;

import mame056.driverH.GameDriver;

import static mess056.systems.coleco.*;
import static mess056.systems.spectrum.*;
import static mess056.systems.zx.*;
import static mess056.systems.a2600.*;
import static mess056.systems.amstrad.*;
import static mess056.systems.coupe.*;
import static mess056.systems.sms.*;
import static mess056.systems.lynx.*;
import static mess056.systems.msx.*;
import static mess056.systems.gb.*;
import static mess056.systems.c16.*;
import static mess056.systems.nes.*;
import static mess056.systems.c64.*;
import static mess056.systems.svision.*;

public class system {
	/*TODO*///	
	/*TODO*///	
	/*TODO*///	#ifndef DRIVER_RECURSIVE
	/*TODO*///	
	/*TODO*///	/* The "root" driver, defined so we can have &driver_##NAME in macros. */
	/*TODO*///	struct GameDriver driver_0 =
	/*TODO*///	{
	/*TODO*///	  __FILE__,
	/*TODO*///	  0,
	/*TODO*///	  "root",
	/*TODO*///	  0,
	/*TODO*///	  0,
	/*TODO*///	  0,
	/*TODO*///	  0,
	/*TODO*///	  0,
	/*TODO*///	  0,
	/*TODO*///	  0,
	/*TODO*///	  0,
	/*TODO*///	  NOT_A_DRIVER,
	/*TODO*///	};
	/*TODO*///	
	/*TODO*///	#endif
	/*TODO*///	
	/*TODO*///	#ifdef TINY_COMPILE
	/*TODO*///	extern const struct GameDriver TINY_NAME;
	/*TODO*///	
	/*TODO*///	const struct GameDriver * drivers[] =
	/*TODO*///	{
	/*TODO*///	  TINY_POINTER,
	/*TODO*///	  0             /* end of array */
	/*TODO*///	};
	/*TODO*///	
	/*TODO*///	#else
	/*TODO*///	
	/*TODO*///	#ifndef DRIVER_RECURSIVE
	/*TODO*///	
	/*TODO*///	#define DRIVER_RECURSIVE
	/*TODO*///	
	/*TODO*///	/* step 1: declare all external references */
	/*TODO*///	#define DRIVER(NAME) extern const struct GameDriver driver_##NAME;
	/*TODO*///	#define TESTDRIVER(NAME) extern const struct GameDriver driver_##NAME;
	/*TODO*///	
	/*TODO*///	/* step 2: define the drivers[] array */
	/*TODO*///	#undef DRIVER
	/*TODO*///	#undef TESTDRIVER
	/*TODO*///	#define DRIVER(NAME) &driver_##NAME,
	/*TODO*///	#ifdef MESS_DEBUG
	/*TODO*///	#define TESTDRIVER(NAME) &driver_##NAME,
	/*TODO*///	#else
	/*TODO*///	#define TESTDRIVER(NAME)
	/*TODO*///	#endif
	/*TODO*///	const struct GameDriver *drivers[] =
	/*TODO*///	{
	/*TODO*///	  0             /* end of array */
	/*TODO*///	};
	/*TODO*///	
	/*TODO*///	#else /* DRIVER_RECURSIVE */
	/*TODO*///	
	/*TODO*///	#ifndef NEOMAME
	
	public static GameDriver drivers[] = {
	
	/*TODO*///	/****************CONSOLES****************************************************/
	/*TODO*///	
	/*TODO*///	/* for pong style games look into docs/pong.txt */
	/*TODO*///	
	/*TODO*///		/* ATARI */
            driver_a2600, 	/* Atari 2600						*/
	/*TODO*///		DRIVER( a5200 ) 	/* Atari 5200						*/
	/*TODO*///		DRIVER( a7800 ) 	/* Atari 7800						*/
            driver_lynx,	/* Atari Lynx Handheld					*/
            driver_lynxa,	/* Atari Lynx Handheld alternate rom save		*/
            driver_lynx2,	/* Atari Lynx II Handheld redesigned, no additions      */
	/*TODO*///	//	DRIVER( jaguar )	/* Atari Jaguar 					*/
	/*TODO*///	
        /* NINTENDO */
        driver_nes,		/* Nintendo Entertainment System			*/
	/*TODO*///		DRIVER( nespal )	/* Nintendo Entertainment System			*/
	driver_famicom,
	driver_gameboy,         /* Nintendo GameBoy Handheld				*/
        driver_gbcolor,         /* Nintendo GameBoy Color Handheld				*/
        driver_supergb,         /* Nintendo Super GameBoy Handheld				*/
	/*TODO*///		DRIVER (snes)		/* Nintendo Super Nintendo				*/
	/*TODO*///	//      DRIVER (vboy)		/* Nintendo Virtual Boy 				*/
	/*TODO*///	
	/*TODO*///		/* SEGA */
	driver_gamegear,	/* Sega Game Gear Handheld				*/
	driver_sms,		/* Sega Sega Master System				*/
	/*TODO*///		DRIVER( genesis )	/* Sega Genesis/MegaDrive				*/
	/*TODO*///	    DRIVER( saturn )	/* Sega Saturn							*/
	/*TODO*///	
	/*TODO*///		/* BALLY */
	/*TODO*///		DRIVER( astrocde )	/* Bally Astrocade					*/
	/*TODO*///	
	/*TODO*///		/* RCA */
	/*TODO*///	TESTDRIVER( vip )		/* Cosmac VIP						*/
	/*TODO*///		DRIVER( studio2 )	/* Studio II						*/
	/*TODO*///		/* hanimex mpt-02 */
	/*TODO*///	//      DRIVER( cstudio2 )      /* Colour Studio II				        */
	/*TODO*///	
	/*TODO*///		/* FAIRCHILD */
	/*TODO*///		DRIVER( channelf )      /* Fairchild Channel F VES - 1976                       */
	/*TODO*///		/* checkers cartridge, additional processor in it */
	/*TODO*///		/* saba videoplay */
	/*TODO*///		/* itt telematch */
	/*TODO*///		/* nordmende teleplay */
	/*TODO*///		/* channelf system 2, redesigned, no additions */
	/*TODO*///		/* saba videoplay 2 */
	/*TODO*///	
			/* COLECO */
	driver_coleco,	/* ColecoVision (Original BIOS )				  */
	driver_colecoa,	/* ColecoVision (Thick Characters)				  */
	/*TODO*///		/* Please dont include these next 2 in a distribution, they are Hacks */
	/*TODO*///	//      DRIVER( colecofb )	   ColecoVision (Fast BIOS load)				  */
	/*TODO*///	//      DRIVER( coleconb )	   ColecoVision (No BIOS load)					  */
	/*TODO*///	
	/*TODO*///		/* NEC */
	/*TODO*///		DRIVER( pce )		/* PC/Engine - Turbo Graphics-16  NEC 1989-1993   */
	/*TODO*///	
	/*TODO*///		DRIVER( arcadia ) /* Emerson Arcadia 2001                           */
	/*TODO*///		/* schmid tvg 2000 */
	/*TODO*///		/* eduscho tele fever */
	/*TODO*///		/* hanimex fever 1 (hmg 2650) */
	/*TODO*///		DRIVER( vcg )		/* Palladium Video-Computer-Game */
	/*TODO*///					/* different cartridge connector, more keys */
	/*TODO*///	
	/*TODO*///		/* GCE */
	/*TODO*///		DRIVER( vectrex )	/* General Consumer Electric Vectrex - 1982-1984  */
	/*TODO*///					/* (aka Milton-Bradley Vectrex) 				  */
	/*TODO*///		DRIVER( raaspec )	/* RA+A Spectrum - Modified Vectrex 			  */
	/*TODO*///	
	/*TODO*///		/* MATTEL */
	/*TODO*///		DRIVER( intv )      /* Mattel Intellivision - 1979 AKA INTV           */
	/*TODO*///		DRIVER( intvsrs )   /* Intellivision (Sears License) - 19??           */
	/*TODO*///	
	/*TODO*///		/* ENTEX */
	/*TODO*///		DRIVER( advision )	/* Adventurevision								  */
	/*TODO*///	
	/*TODO*///		/* CAPCOM */
	/*TODO*///		DRIVER( sfzch ) 	/* CPS Changer (Street Fighter ZERO)			  */
	/*TODO*///	
	/*TODO*///		/* Magnavox */
	/*TODO*///	//      DRIVER( odyssey )	/* Magnavox Odyssey - analogue (1972)			  */
	/*TODO*///	TESTDRIVER( odyssey2 )	/* Magnavox Odyssey 2 - 1978-1983				  */
	/*TODO*///	
	/*TODO*///		/* Watara */
	driver_svision,	/* Super Vision	Handheld						  */
	/*TODO*///	
	/*TODO*///		/* 1992 epoch barcode battler handheld*/
	/*TODO*///	
	/*TODO*///		/* tiger game.com handheld*/
	/*TODO*///	
	/*TODO*///	TESTDRIVER( vc4000 )		/* interton vc4000 */
	/*TODO*///		/* grundig super play computer 4000 */
	/*TODO*///	
	/*TODO*///		/* bandai wonderswan handheld*/
	/*TODO*///	
	/*TODO*///		/* 1979 mb microvision handheld*/
	/*TODO*///	
	/*TODO*///	/****************COMPUTERS****************************************************/
	/*TODO*///	    /* ACORN */
	/*TODO*///	    DRIVER( atom )      /* 1979 Acorn Atom                                */
	/*TODO*///	    DRIVER( atomeb )    /* 1979 Acorn Atom                                */
	/*TODO*///	    DRIVER( bbca )      /* 1981 BBC Micro Model A                         */
	/*TODO*///	    DRIVER( bbcb )      /* 1981 BBC Micro Model B                         */
	/*TODO*///	    DRIVER( bbcb1770 )  /* 1981 BBC Micro Model B with WD 1770 disc       */
	/*TODO*///	    DRIVER( bbcbp )     /* 198? BBC Micro Model B+ 64K                    */
	/*TODO*///	    DRIVER( bbcbp128 )  /* 198? BBC Micro Model B+ 128K                   */
	/*TODO*///	TESTDRIVER( bbcb6502 )  /* 198? BBC B WD1770 with a 6502 second processor */
	/*TODO*///	/*	DRIVER( electron )*//* 198? Acorn Electron							  */
	/*TODO*///	
	/*TODO*///	TESTDRIVER( a310 )      /* 1988 Acorn Archimedes 310                      */
	/*TODO*///	
	/*TODO*///		DRIVER( z88 )		/*												  */
	/*TODO*///	
	driver_cpc464,	/* Amstrad (Schneider in Germany) 1984			  */
	driver_cpc664,	/* Amstrad (Schneider in Germany) 1985			  */
	driver_cpc6128,	/* Amstrad (Schneider in Germany) 1985			  */
	driver_cpc464p, /* Amstrad CPC464  Plus - 1987					  */
	driver_cpc6128p,/* Amstrad CPC6128 Plus - 1987					  */
	driver_kccomp,	/* VEB KC compact								  */
	/*TODO*///	
	/*TODO*///		DRIVER( pcw8256 )	/* 198? PCW8256 								  */
	/*TODO*///		DRIVER( pcw8512 )	/* 198? PCW8512 								  */
	/*TODO*///		DRIVER( pcw9256 )	/* 198? PCW9256 								  */
	/*TODO*///		DRIVER( pcw9512 )	/* 198? PCW9512 (+) 							  */
	/*TODO*///		DRIVER( pcw10 ) 	/* 198? PCW10									  */
	/*TODO*///	
	/*TODO*///		DRIVER( pcw16 )     /* 1995 PCW16                                     */
	/*TODO*///	
	/*TODO*///		/* pc20 clone of sinclair pc200 */
	/*TODO*///		/* pc1512 ibm xt compatible */
	/*TODO*///		/* pc1640/pc6400 ibm xt compatible */
	/*TODO*///	
	/*TODO*///		DRIVER( nc100 ) 	/* 19?? NC100									  */
	/*TODO*///		DRIVER( nc100a ) 	/* 19?? NC100									  */
	/*TODO*///		DRIVER( nc200 )     /* 19?? NC200									  */
	/*TODO*///	
	/*TODO*///		/* APPLE */
	/*TODO*///	/*
	/*TODO*///	 * CPU Model			 Month				 Year
	/*TODO*///	 * -------------		 -----				 ----
	/*TODO*///	 *
	/*TODO*///	 * Apple I				 July				 1976
	/*TODO*///	 * Apple II 			 April				 1977
	/*TODO*///	 * Apple II Plus		 June				 1979
	/*TODO*///	 * Apple III			 May				 1980
	/*TODO*///	 * Apple IIe			 January			 1983
	/*TODO*///	 * Apple III Plus		 December			 1983
	/*TODO*///	 * Apple IIe Enhanced	 March				 1985
	/*TODO*///	 * Apple IIc			 April				 1984
	/*TODO*///	 * Apple IIc ROM 0		 ?					 1985
	/*TODO*///	 * Apple IIc ROM 3		 September			 1986
	/*TODO*///	 * Apple IIgs			 September			 1986
	/*TODO*///	 * Apple IIe Platinum	 January			 1987
	/*TODO*///	 * Apple IIgs ROM 01	 September			 1987
	/*TODO*///	 * Apple IIc ROM 4		 ?					 198?
	/*TODO*///	 * Apple IIc Plus		 September			 1988
	/*TODO*///	 * Apple IIgs ROM 3 	 August 			 1989
	/*TODO*///	 */
	/*TODO*///		DRIVER( apple1 )	/* 1976 Apple 1 								  */
	/*TODO*///		DRIVER( apple2c )	/* 1984 Apple //c								  */
	/*TODO*///		DRIVER( apple2c0 )	/* 1986 Apple //c (3.5 ROM) 					  */
	/*TODO*///		DRIVER( apple2cp )	/* 1988 Apple //c+								  */
	/*TODO*///		DRIVER( apple2e )	/* 1983 Apple //e								  */
	/*TODO*///		DRIVER( apple2ee )	/* 1985 Apple //e Enhanced						  */
	/*TODO*///		DRIVER( apple2ep )	/* 1987 Apple //e Platinum						  */
	/*TODO*///	/*
	/*TODO*///	 * Lisa 				 January			 1983
	/*TODO*///	 * Lisa 2 				 January			 1984
	/*TODO*///	 * Macintosh XL 		 January			 1985
	/*TODO*///	 */
	/*TODO*///		DRIVER( lisa2 ) 	/* 1984 Apple Lisa 2							  */
	/*TODO*///		DRIVER( lisa210 ) 	/* 1984 Apple Lisa 2/10							  */
	/*TODO*///		DRIVER( macxl ) 	/* 1984 Apple Macintosh XL						  */
	/*TODO*///	/*
	/*TODO*///	 * Macintosh 			 January			 1984
	/*TODO*///	 * Macintosh 512k		 July?				 1984
	/*TODO*///	 * Macintosh 512ke		 ?					 1986
	/*TODO*///	 * Macintosh Plus 		 ?					 1986
	/*TODO*///	 * Macintosh SE			 ?					 1987
	/*TODO*///	 * Macintosh II 		 ?					 1987
	/*TODO*///	 */
	/*TODO*///	/*	DRIVER( mac512k )*/	/* 1984 Apple Macintosh 512k					  */
	/*TODO*///		DRIVER( mac512ke )  /* 1986 Apple Macintosh 512ke                     */
	/*TODO*///		DRIVER( macplus )	/* 1986 Apple Macintosh Plus					  */
	/*TODO*///	/*	DRIVER( mac2 )*/	/* 1987 Apple Macintosh II						  */
	/*TODO*///	
	/*TODO*///		/* ATARI */
	/*TODO*///	/*
	/*TODO*///	400/800 10kB OS roms
	/*TODO*///	A    NTSC  (?)         (?)         (?)
	/*TODO*///	A    PAL   (?)         0x72b3fed4  CO15199, CO15299, CO12399B
	/*TODO*///	B    NTSC  (?)         0x0e86d61d  CO12499B, CO14599B, 12399B
	/*TODO*///	B    PAL   (?)         (?)         (?)
	/*TODO*///	
	/*TODO*///	XL/XE 16kB OS roms
	/*TODO*///	10   1200XL  10/26/1982  0xc5c11546  CO60616A, CO60617A
	/*TODO*///	11   1200XL  12/23/1982  (?)         CO60616B, CO60617B
	/*TODO*///	1    600XL   03/11/1983  0x643bcc98  CO62024
	/*TODO*///	2    XL/XE   05/10/1983  0x1f9cd270  CO61598B
	/*TODO*///	3    800XE   03/01/1985  0x29f133f7  C300717
	/*TODO*///	4    XEGS    05/07/1987  0x1eaf4002  C101687
	/*TODO*///	*/
	/*TODO*///	
	/*TODO*///		DRIVER( a400 )		/* 1979 Atari 400								  */
	/*TODO*///		DRIVER( a400pal )	/* 1979 Atari 400 PAL							  */
	/*TODO*///		DRIVER( a800 )		/* 1979 Atari 800								  */
	/*TODO*///		DRIVER( a800pal )	/* 1979 Atari 800 PAL							  */
	/*TODO*///		DRIVER( a800xl )	/* 1983 Atari 800 XL							  */
	/*TODO*///	
	/*TODO*///	//!!TESTDRIVER( atarist )	/* Atari ST 								  */
	/*TODO*///	
	/*TODO*///		/* COMMODORE */
	/*TODO*///		DRIVER( kim1 )		/* Commodore (MOS) KIM-1 1975					  */
	/*TODO*///	TESTDRIVER( sym1 )		/* Synertek SYM1								  */
	/*TODO*///	TESTDRIVER( aim65 )		/* Rockwell AIM65								  */
	/*TODO*///	
	/*TODO*///		DRIVER( pet )		/* PET2001/CBM20xx Series (Basic 1) 			  */
	/*TODO*///		DRIVER( cbm30 ) 	/* Commodore 30xx (Basic 2) 					  */
	/*TODO*///		DRIVER( cbm30b )	/* Commodore 30xx (Basic 2) (business keyboard)   */
	/*TODO*///		DRIVER( cbm40 ) 	/* Commodore 40xx FAT (CRTC) 60Hz				  */
	/*TODO*///		DRIVER( cbm40pal )	/* Commodore 40xx FAT (CRTC) 50Hz				  */
	/*TODO*///		DRIVER( cbm40b )	/* Commodore 40xx THIN (business keyboard)		  */
	/*TODO*///		DRIVER( cbm80 ) 	/* Commodore 80xx 60Hz							  */
	/*TODO*///		DRIVER( cbm80pal )	/* Commodore 80xx 50Hz							  */
	/*TODO*///		DRIVER( cbm80ger )	/* Commodore 80xx German (50Hz) 				  */
	/*TODO*///		DRIVER( cbm80swe )	/* Commodore 80xx Swedish (50Hz)				  */
	/*TODO*///		DRIVER( superpet )	/* Commodore SP9000/MMF9000 (50Hz)				  */
	/*TODO*///	TESTDRIVER( mmf9000 )	/* Commodore MMF9000 Swedish					  */
	/*TODO*///	
	/*TODO*///		DRIVER( vic20 ) 	/* Commodore Vic-20 NTSC						  */
	/*TODO*///		DRIVER( vic1001 )	/* Commodore VIC-1001 (VIC20 Japan)				  */
	/*TODO*///		DRIVER( vc20 )		/* Commodore Vic-20 PAL 						  */
	/*TODO*///		DRIVER( vic20swe )	/* Commodore Vic-20 Sweden						  */
	/*TODO*///	TESTDRIVER( vic20v ) 	/* Commodore Vic-20 NTSC, VC1540				  */
	/*TODO*///	TESTDRIVER( vc20v ) 	/* Commodore Vic-20 PAL, VC1541					  */
	/*TODO*///		DRIVER( vic20i )	/* Commodore Vic-20 IEEE488 Interface			  */
	/*TODO*///	
	driver_max,		/* Max (Japan)/Ultimax (US)/VC10 (German)		  */
        driver_c64,		/* Commodore 64 - NTSC							  */
	/*TODO*///	/*	DRIVER( j64 )*/		/* Commodore 64 - NTSC (Japan)					  */
	driver_c64pal,	/* Commodore 64 - PAL							  */
	/*TODO*///		DRIVER( vic64s )	/* Commodore VIC64S (Swedish)					  */
	driver_cbm4064,	/* Commodore CBM4064							  */
	/*TODO*///	TESTDRIVER( sx64 )		/* Commodore SX 64 - PAL						  */
	/*TODO*///	TESTDRIVER( vip64 )		/* Commodore VIP64 (SX64, PAL, Swedish)			  */
	/*TODO*///	TESTDRIVER( dx64 )		/* Commodore DX 64 - PROTOTPYE, PAL						  */
	driver_c64gs, 	/* Commodore 64 Games System					  */
	/*TODO*///	
	/*TODO*///		DRIVER( cbm500 )	/* Commodore 500/P128-40						  */
	/*TODO*///		DRIVER( cbm610 )	/* Commodore 610/B128LP 						  */
	/*TODO*///		DRIVER( cbm620 )	/* Commodore 620/B256LP 						  */
	/*TODO*///		DRIVER( cbm620hu )	/* Commodore 620/B256LP Hungarian				  */
	/*TODO*///	/*	DRIVER( cbm630 )*/	/* Commodore 630								  */
	/*TODO*///		DRIVER( cbm710 )	/* Commodore 710/B128HP 						  */
	/*TODO*///		DRIVER( cbm720 )	/* Commodore 720/B256HP 						  */
	/*TODO*///		DRIVER( cbm720se )	/* Commodore 720/B256HP Swedish/Finnish			  */
	/*TODO*///	/*	DRIVER( cbm730 )*/	/* Commodore 730								  */
	/*TODO*///	
	driver_c16,		/* Commodore 16 								  */
	/*TODO*///		DRIVER( c16hun )	/* Commodore 16 Novotrade (Hungarian Character Set)	  */
	/*TODO*///		DRIVER( c16c )		/* Commodore 16  c1551							  */
	/*TODO*///	TESTDRIVER( c16v )		/* Commodore 16  vc1541 						  */
	/*TODO*///		DRIVER( plus4 ) 	/* Commodore +4  c1551							  */
	/*TODO*///		DRIVER( plus4c )	/* Commodore +4  vc1541 						  */
	/*TODO*///	TESTDRIVER( plus4v )	/* Commodore +4 								  */
	/*TODO*///		DRIVER( c364 )		/* Commodore 364 - Prototype					  */
	/*TODO*///	
	/*TODO*///		DRIVER( c128 )		/* Commodore 128 - NTSC 						  */
	/*TODO*///		DRIVER( c128ger )	/* Commodore 128 - PAL (german) 				  */
	/*TODO*///		DRIVER( c128fra )	/* Commodore 128 - PAL (french) 				  */
	/*TODO*///		DRIVER( c128ita )	/* Commodore 128 - PAL (italian)				  */
	/*TODO*///		DRIVER( c128swe )	/* Commodore 128 - PAL (swedish)				  */
	/*TODO*///	TESTDRIVER( c128nor )	/* Commodore 128 - PAL (norwegian)				  */
	/*TODO*///	TESTDRIVER( c128d )		/* Commodore 128D - NTSC 						  */
	/*TODO*///	TESTDRIVER( c128dita )	/* Commodore 128D - PAL (italian) cost reduced set	  */
	/*TODO*///	
	/*TODO*///	/*	DRIVER( lcd )*/		/* Commodore LCD Prototype (m65c102 based)		  */
	/*TODO*///	
	/*TODO*///	/*	DRIVER( cbm900 )*/	/* Commodore 900 Prototype (z8000)				  */
	/*TODO*///	
	/*TODO*///	TESTDRIVER( amiga ) 	/* Commodore Amiga								  */
	/*TODO*///	TESTDRIVER( cdtv )
	/*TODO*///	
	/*TODO*///		DRIVER( c65 )		/* C65 / C64DX (Prototype, NTSC, 911001)		  */
	/*TODO*///		DRIVER( c65e )		/* C65 / C64DX (Prototype, NTSC, 910828)		  */
	/*TODO*///		DRIVER( c65d )		/* C65 / C64DX (Prototype, NTSC, 910626)		  */
	/*TODO*///		DRIVER( c65c )		/* C65 / C64DX (Prototype, NTSC, 910523)		  */
	/*TODO*///		DRIVER( c65ger )	/* C65 / C64DX (Prototype, German PAL, 910429)	  */
	/*TODO*///		DRIVER( c65a )		/* C65 / C64DX (Prototype, NTSC, 910111)		  */
	/*TODO*///	
	/*TODO*///		/* IBM PC & Clones */
	/*TODO*///		DRIVER( ibmpc )		/* 1982	IBM PC									  */
	/*TODO*///		DRIVER( ibmpca )	/* 1982 IBM PC									  */
	/*TODO*///		DRIVER( pcmda ) 	/* 1987 PC with MDA (MGA aka Hercules)			  */
	/*TODO*///		DRIVER( pc )		/* 1987 PC with CGA								  */
	/*TODO*///	TESTDRIVER( bondwell )	/* 1985	Bondwell (CGA)                         	  */
	/*TODO*///		DRIVER( europc )	/* 1988	Schneider Euro PC (CGA or Hercules)		  */
	/*TODO*///	
	/*TODO*///		/* pc junior */
	/*TODO*///	TESTDRIVER( ibmpcjr )	/*      IBM PC Jr								  */
	/*TODO*///		DRIVER( t1000hx )	/* 1987 Tandy 1000HX (similiar to PCJr) 		  */
	/*TODO*///	
	/*TODO*///		/* xt */
	/*TODO*///		DRIVER( ibmxt )		/* 1986	IBM XT									  */
	/*TODO*///	/*	DRIVER( ibm8530 )*/	/* 1987 IBM PS2 Model 30 (MCGA)						*/
	/*TODO*///		DRIVER( pc200 )     /* 1988 Sinclair PC200                            */
	/*TODO*///		DRIVER( pc20 )      /* 1988 Amstrad PC20                              */
	/*TODO*///		DRIVER( pc1512 )	/* 1986 Amstrad PC1512 (CGA compatible)			  */
	/*TODO*///		DRIVER( pc1640 )	/* 1987 Amstrad PC1640 (EGA compatible)			  */
	/*TODO*///	
	/*TODO*///		DRIVER( xtvga ) 	/* 198? PC-XT (VGA, MF2 Keyboard)				  */
	/*TODO*///	
	/*TODO*///		/* at */
	/*TODO*///	TESTDRIVER( ibmat )		/* 1985	IBM AT									  */
	/*TODO*///	TESTDRIVER( i8530286 )	/* 1988 IBM PS2 Model 30 286 (VGA)					*/
	/*TODO*///	//TESTDRIVER( t2500xl )	/* 19?? Tandy 2500XL (VGA)					*/
	/*TODO*///		DRIVER( at )		/* 1987 AMI Bios and Diagnostics				  */
	/*TODO*///	TESTDRIVER( atvga ) 	/*												  */
	/*TODO*///	TESTDRIVER( neat )		/* 1989	New Enhanced AT chipset, AMI BIOS		  */
	/*TODO*///	
	/*TODO*///	/*	DRIVER( ibm8535 )*/	/* 1991 IBM PS2 Model 35 (80386sx)					*/
	/*TODO*///	/*	DRIVER( at386)*/	/*												  */
	/*TODO*///	
	/*TODO*///		/* microchannel */
	/*TODO*///	/*	DRIVER( ibm8550 )*/	/* 1987 IBM PS2 Model 50 (80286)					*/
	/*TODO*///	
	/*TODO*///	/*	DRIVER( ibm8580 )*/	/* 1987 IBM PS2 Model 80 (80386)					*/
	/*TODO*///	
	/*TODO*///		/* SINCLAIR */
	/*TODO*///	
	driver_zx80,		/* Sinclair ZX-80								  */
	driver_zx81,		/* Sinclair ZX-81								  */
	/*TODO*///		DRIVER( ts1000 )	/* Timex Sinclair 1000							  */
	driver_aszmic,          /* ASZMIC ZX-81 ROM swap						  */
	/*TODO*///		DRIVER( pc8300 )	/* Your Computer - PC8300						  */
	/*TODO*///		DRIVER( pow3000 )	/* Creon Enterprises - Power 3000				  */
	/*TODO*///	
	driver_spectrum,	/* 1982 ZX Spectrum 							  */
	/*TODO*///		DRIVER( specpls4 )	/* 2000 ZX Spectrum +4							  */
	/*TODO*///		DRIVER( specbusy )	/* 1994 ZX Spectrum (BusySoft Upgrade v1.18)			  */
	/*TODO*///		DRIVER( specpsch )	/* 19?? ZX Spectrum (Maly's Psycho Upgrade)			  */
	/*TODO*///		DRIVER( specgrot )	/* ???? ZX Spectrum (De Groot's Upgrade)          */
	/*TODO*///		DRIVER( specimc )	/* 1985 ZX Spectrum (Collier's Upgrade)           */
	/*TODO*///		DRIVER( speclec )	/* 1987 ZX Spectrum (LEC Upgrade)				  */
	/*TODO*///		DRIVER( inves ) 	/* 1986 Inves Spectrum 48K+ 					  */
	/*TODO*///		DRIVER( tk90x ) 	/* 1985 TK90x Color Computer					  */
	/*TODO*///		DRIVER( tk95 )		/* 1986 TK95 Color Computer 					  */
	/*TODO*///		DRIVER( tc2048 )	/* 198? TC2048									  */
	/*TODO*///		DRIVER( ts2068 )	/* 1983 TS2068									  */
	/*TODO*///		DRIVER( uk2086 )	/* 1986 UK2086									  */
	/*TODO*///	
	driver_spec128,          /* 1986 ZX Spectrum 128"                          */
	/*TODO*///		DRIVER( spec128s )	/* 1985 ZX Spectrum 128 (Spain) 				  */
	driver_specpls2,	/* 1986 ZX Spectrum +2							  */
	/*TODO*///		DRIVER( specpl2a )	/* 1987 ZX Spectrum +2a 						  */
	driver_specpls3,	/* 1987 ZX Spectrum +3							  */
	/*TODO*///	
	/*TODO*///		DRIVER( specp2fr )	/* 1986 ZX Spectrum +2 (France) 				  */
	/*TODO*///		DRIVER( specp2sp )	/* 1986 ZX Spectrum +2 (Spain)					  */
	/*TODO*///		DRIVER( specp3sp )	/* 1987 ZX Spectrum +3 (Spain)					  */
	/*TODO*///		DRIVER( specpl3e )	/* 2000 ZX Spectrum +3e 						  */
	/*TODO*///	
        driver_pentagon,
	/*TODO*///		/* sinclair pc200 professional series ibmxt compatible*/
	/*TODO*///	
	/*TODO*///		/* SHARP */
	/*TODO*///	//TESTDRIVER( pc1500 )	/* 1982 Pocket Computer 1500						*/
	/*TODO*///	//TESTDRIVER( trs80pc2 )	/* 1982 Tandy TRS80 PC 2							*/
	/*TODO*///	//TESTDRIVER( pc1500a )	/* 1984 Pocket Computer 1500A						*/
	/*TODO*///	/*	DRIVER( pc1600 )*/	/* 1986 Pocket Computer 1600						*/
	/*TODO*///		DRIVER( pc1251 )	/* Pocket Computer 1251 						  */
	/*TODO*///	TESTDRIVER( trs80pc3 )	/* Tandy TRS80 PC-3									*/
	/*TODO*///	
	/*TODO*///		DRIVER( pc1401 )	/* Pocket Computer 1401 						  */
	/*TODO*///		DRIVER( pc1402 )	/* Pocket Computer 1402 						  */
	/*TODO*///	
	/*TODO*///		DRIVER( pc1350 )	/* Pocket Computer 1350 						  */
	/*TODO*///	
	/*TODO*///		DRIVER( pc1403 )	/* Pocket Computer 1403 						  */
	/*TODO*///		DRIVER( pc1403h )	/* Pocket Computer 1403H 						  */
	/*TODO*///	
	/*TODO*///		DRIVER( mz700 ) 	/* 1982 Sharp MZ700 							  */
	/*TODO*///		DRIVER( mz700j )	/* 1982 Sharp MZ700 Japan						  */
	/*TODO*///	TESTDRIVER( mz800  )	/* 1982 Sharp MZ800 							  */
	/*TODO*///	
	/*TODO*///	/*	DRIVER( x68000 )*/	/* X68000										  */
	/*TODO*///	
	/*TODO*///		/* TEXAS INSTRUMENTS */
	/*TODO*///	TESTDRIVER( ti990_4 )	/* 197? TI 990/4								  */
	/*TODO*///	TESTDRIVER( ti99_224 )	/* 1983 TI 99/2 								  */
	/*TODO*///	TESTDRIVER( ti99_232 )	/* 1983 TI 99/2 								  */
	/*TODO*///		DRIVER( ti99_4 )	/* 1978 TI 99/4 								  */
	/*TODO*///		DRIVER( ti99_4e )	/* 1980 TI 99/4E								  */
	/*TODO*///		DRIVER( ti99_4a )	/* 1981 TI 99/4A								  */
	/*TODO*///		DRIVER( ti99_4ae )	/* 1981 TI 99/4AE								  */
	/*TODO*///	
	/*TODO*///	    DRIVER( avigo )     /*                                                */
	/*TODO*///	
	/*TODO*///	/* Texas Instruments Calculators */
	/*TODO*///	
	/*TODO*///	/* TI-73 (Z80 6 MHz) */
	/*TODO*///	/*	DRIVER( ti73 )*/	/*TI 73 rom ver. 1.3004 */
	/*TODO*///	/*	DRIVER( ti73a )*/	/*TI 73 rom ver. 1.3007 */
	/*TODO*///	
	/*TODO*///	/* TI-80 (custom 980 kHz) */
	/*TODO*///	/*	DRIVER( ti80 )*/	/*TI 80 */
	/*TODO*///	
	/*TODO*///	/* TI-81 (Z80 2 MHz) */
	/*TODO*///		DRIVER( ti81 )		/*TI 81 rom ver. 1.8 */
	/*TODO*///	/*	DRIVER( ti81v11 )*/	/*TI 81 rom ver. 1.1 */
	/*TODO*///	/*	DRIVER( ti81v20 )*/	/*TI 81 rom ver. 2.0 */
	/*TODO*///	
	/*TODO*///	/* TI-82 (Z80 6 MHz) */
	/*TODO*///	/*	DRIVER( ti82 )*/	/*TI 82 rom ver. 3.0 */
	/*TODO*///	/*	DRIVER( ti82v4 )*/	/*TI 82 rom ver. 4.0 */
	/*TODO*///	/*	DRIVER( ti82v7 )*/	/*TI 82 rom ver. 7.0 */
	/*TODO*///	/*	DRIVER( ti82v8 )*/	/*TI 82 rom ver. 8.0 */
	/*TODO*///	/*	DRIVER( ti82v10 )*/	/*TI 82 rom ver. 10.0 */
	/*TODO*///	/*	DRIVER( ti82v12 )*/	/*TI 82 rom ver. 12.0 */
	/*TODO*///	/*	DRIVER( ti82v15 )*/	/*TI 82 rom ver. 15.0 */
	/*TODO*///	/*	DRIVER( ti82v16 )*/	/*TI 82 rom ver. 16.0 */
	/*TODO*///	/*	DRIVER( ti82v17 )*/	/*TI 82 rom ver. 17.0 */
	/*TODO*///	/*	DRIVER( ti82v18 )*/	/*TI 82 rom ver. 18.0 */
	/*TODO*///	/*	DRIVER( ti82v19 )*/	/*TI 82 rom ver. 19.0 */
	/*TODO*///	/*	DRIVER( ti82v19a )*/	/*TI 82 rom ver. 19.006 */
	/*TODO*///	
	/*TODO*///	/* TI-83 (Z80 6 MHz) */
	/*TODO*///	/*	DRIVER( ti83 )*/	/*TI 83 rom ver. 1.0200 */
	/*TODO*///	/*	DRIVER( ti83v02 )*/	/*TI 83 rom ver. 1.0300 */
	/*TODO*///	/*	DRIVER( ti83v03 )*/	/*TI 83 rom ver. 1.0400 */
	/*TODO*///	/*	DRIVER( ti83v04 )*/	/*TI 83 rom ver. 1.0600 */
	/*TODO*///	/*	DRIVER( ti83v06 )*/	/*TI 83 rom ver. 1.0700 */
	/*TODO*///	/*	DRIVER( ti83v07 )*/	/*TI 83 rom ver. 1.0800 */
	/*TODO*///	/*	DRIVER( ti83v08 )*/	/*TI 83 rom ver. 1.10 */
	/*TODO*///	
	/*TODO*///	/* TI-83 Plus (Z80 8 MHz) */
	/*TODO*///	/*	DRIVER( ti83p )*/	/*TI 83 rom ver. 1.03 */
	/*TODO*///	/*	DRIVER( ti83pv06 )*/	/*TI 83 rom ver. 1.06 */
	/*TODO*///	/*	DRIVER( ti83pv08 )*/	/*TI 83 rom ver. 1.08 */
	/*TODO*///	/*	DRIVER( ti83pv10 )*/	/*TI 83 rom ver. 1.10 */
	/*TODO*///	/*	DRIVER( ti83pv12 )*/	/*TI 83 rom ver. 1.12 */
	/*TODO*///	
	/*TODO*///	/* TI-85 (Z80 6MHz) */
	/*TODO*///	/*	DRIVER( ti85v10 )*/	/*TI 85 rom ver. 1.0 */
	/*TODO*///	/*	DRIVER( ti85v20 )*/	/*TI 85 rom ver. 2.0 */
	/*TODO*///		DRIVER( ti85 )  	/*TI 85 rom ver. 3.0a */
	/*TODO*///		DRIVER( ti85v40 )	/*TI 85 rom ver. 4.0 */
	/*TODO*///		DRIVER( ti85v50 )	/*TI 85 rom ver. 5.0 */
	/*TODO*///		DRIVER( ti85v60 )	/*TI 85 rom ver. 6.0 */
	/*TODO*///	/*	DRIVER( ti85v70 )*/	/*TI 85 rom ver. 7.0 */
	/*TODO*///		DRIVER( ti85v80 )	/*TI 85 rom ver. 8.0 */
	/*TODO*///		DRIVER( ti85v90 )	/*TI 85 rom ver. 9.0 */
	/*TODO*///		DRIVER( ti85v100 )	/*TI 85 rom ver. 10.0 */
	/*TODO*///	
	/*TODO*///	/* TI-86 (Z80 6 MHz) */
	/*TODO*///		DRIVER( ti86 )		/*TI 86 rom ver. 1.2 */
	/*TODO*///		DRIVER( ti86v13 )	/*TI 86 rom ver. 1.3 */
	/*TODO*///		DRIVER( ti86v14 )	/*TI 86 rom ver. 1.4 */
	/*TODO*///		TESTDRIVER( ti86v15 )	/*TI 86 rom ver. 1.5 */
	/*TODO*///		DRIVER( ti86v16 )	/*TI 86 rom ver. 1.6 */
	/*TODO*///		DRIVER( ti86grom )	/*TI 86 homebrew rom by Daniel Foesch */
	/*TODO*///	
	/*TODO*///	/* TI-89 (M68000) */
	/*TODO*///	/* TI-92 (M68000 10 MHz) */
	/*TODO*///	/* TI-92 Plus (M68000) */
	/*TODO*///	
	/*TODO*///		/* NEC */
	/*TODO*///		/* TK80 series i8080 based */
	/*TODO*///		/* PC-100 series i8086 based */
	/*TODO*///		/* PC-2001 series micro PD7907 based */
	/*TODO*///		/* PC-6001 series Z80 based */
	/*TODO*///		/* PC-8001 series Z80 based */
	/*TODO*///		/* PC-8201 series micro 80C85 based */
	/*TODO*///	
	/*TODO*///		/* PC-8801 series Z80 based */
	/*TODO*///		/* DRIVER( pc8801 ) */	/* PC-8801 */
	/*TODO*///		/* DRIVER( pc88mk2 ) */	/* PC-8801mkII */
	/*TODO*///		DRIVER( pc88srl )	/* PC-8801mkIISR(Low resolution display, VSYNC 15KHz) */
	/*TODO*///		DRIVER( pc88srh )	/* PC-8801mkIISR(High resolution display, VSYNC 24KHz) */
	/*TODO*///		/* DRIVER( pc8801tr ) */	/* PC-8801mkIITR */
	/*TODO*///		/* DRIVER( pc8801fr ) */	/* PC-8801mkIIFR */
	/*TODO*///		/* DRIVER( pc8801mr ) */	/* PC-8801mkIIMR */
	/*TODO*///		/* DRIVER( pc8801fh ) */	/* PC-8801FH */
	/*TODO*///		/* DRIVER( pc8801mh ) */	/* PC-8801MH */
	/*TODO*///		/* DRIVER( pc8801fa ) */	/* PC-8801FA */
	/*TODO*///		/* DRIVER( pc8801ma ) */	/* PC-8801MA */
	/*TODO*///		/* DRIVER( pc8801fe ) */	/* PC-8801FE */
	/*TODO*///		/* DRIVER( pc8801ma2 ) */	/* PC-8801MA2 */
	/*TODO*///		/* DRIVER( pc8801fe2 ) */	/* PC-8801FE2 */
	/*TODO*///		/* DRIVER( pc8801mc ) */	/* PC-8801MC */
	/*TODO*///		/* DRIVER( pc98do88 ) */	/* PC-98DO(88mode) */
	/*TODO*///		/* DRIVER( pc98dop8 ) */	/* PC-98DO+(88mode) */
	/*TODO*///	
	/*TODO*///		/* PC-88VA series micro PD9002(special version of V30 with Z80 compatible mode) based */
	/*TODO*///		/* DRIVER( pc88va ) */	/* PC-88VA */
	/*TODO*///		/* DRIVER( pc88va2 ) */	/* PC-88VA2 */
	/*TODO*///		/* DRIVER( pc88va3 ) */	/* PC-88VA3 */
	/*TODO*///	
	/*TODO*///		/* PC-9801 series i8086 based (V30, i386, ..) */
	/*TODO*///	
	/*TODO*///		/* CANTAB */
	/*TODO*///		DRIVER( jupiter )	/* Jupiter Ace									  */
	/*TODO*///	
	/*TODO*///		/* SORD */
	/*TODO*///		DRIVER( sordm5 )
	/*TODO*///	
	/*TODO*///		/* APF Electronics Inc. */
	/*TODO*///		DRIVER( apfm1000 )
	/*TODO*///		DRIVER( apfimag )
	/*TODO*///	
	/*TODO*///		/* Tatung */
	/*TODO*///		DRIVER( einstein )
	/*TODO*///	
	/*TODO*///		/* INTELLIGENT SOFTWARE */
	/*TODO*///		DRIVER( ep128 ) 	/* Enterprise 128 k 							  */
	/*TODO*///		DRIVER( ep128a )	/* Enterprise 128 k 							  */
	/*TODO*///	
	/*TODO*///		/* NON LINEAR SYSTEMS */
	/*TODO*///		DRIVER( kaypro )	/* Kaypro 2X									  */
	/*TODO*///	
	/*TODO*///		/* VEB MIKROELEKTRONIK */
	/*TODO*///		/* KC compact is partial CPC compatible */
	/*TODO*///	//	DRIVER( kc85_4 )	/* VEB KC 85/4									  */
	/*TODO*///	//    DRIVER( kc85_3 )    /* VEB KC 85/3                                    */
	/*TODO*///	TESTDRIVER( kc85_4d )   /* VEB KC 85/4 with disk interface                */
	/*TODO*///	    /* pc1715 z80/u880 based */
	/*TODO*///		/* pc1715w z80/u880 based */
	/*TODO*///		/* a5105 z80/u880 based */
	/*TODO*///		/* a5120 z80/u880 based */
	/*TODO*///		/* a7100 i8086 based */
	/*TODO*///	
	/*TODO*///		/* MICROBEE SYSTEMS */
	/*TODO*///		DRIVER( mbee )		/* Microbee 									  */
	/*TODO*///		DRIVER( mbeepc )	/* Microbee (Personal Communicator)				  */
	/*TODO*///		DRIVER( mbee56k )	/* Microbee 56K (CP/M)							  */
	/*TODO*///	
	/*TODO*///		/* TANDY RADIO SHACK */
	/*TODO*///		DRIVER( trs80 )	    /* TRS-80 Model I	- Radio Shack Level I BASIC   */
	/*TODO*///		DRIVER( trs80l2 ) 	/* TRS-80 Model I	- Radio Shack Level II BASIC  */
	/*TODO*///		DRIVER( trs80l2a )	/* TRS-80 Model I	- R/S L2 BASIC				  */
	/*TODO*///		DRIVER( sys80 ) 	/* EACA System 80								  */
	/*TODO*///		DRIVER( lnw80 ) 	/* LNW Research LNW-80							  */
	/*TODO*///	/*	DRIVER( trs80m2 )*/	/* TRS-80 Model II -							  */
	/*TODO*///	TESTDRIVER( trs80m3 )	/* TRS-80 Model III - Radio Shack/Tandy 		  */
	/*TODO*///	
	/*TODO*///		DRIVER( coco )		/* Color Computer								  */
	/*TODO*///		DRIVER( cocoe )		/* Color Computer (Extended BASIC 1.0)			  */
	/*TODO*///		DRIVER( coco2 ) 	/* Color Computer 2 							  */
	/*TODO*///		DRIVER( coco2b ) 	/* Color Computer 2B (uses M6847T1 video chip)    */
	/*TODO*///		DRIVER( coco3 ) 	/* Color Computer 3 (NTSC)						  */
	/*TODO*///		DRIVER( coco3p ) 	/* Color Computer 3 (PAL)						  */
	/*TODO*///		DRIVER( coco3h )	/* Hacked Color Computer 3 (6309)				  */
	/*TODO*///		DRIVER( dragon32 )	/* Dragon32 									  */
	/*TODO*///		DRIVER( cp400 ) 	/* Prologica CP400								  */
	/*TODO*///		DRIVER( mc10 )		/* MC-10										  */
	/*TODO*///	
	/*TODO*///		/* dragon 32 coco compatible */
	/*TODO*///	/*	DRIVER( dragon64 */	/* Dragon 64									  */
	/*TODO*///	
	/*TODO*///		/* EACA */
	/*TODO*///		DRIVER( cgenie )	/* Colour Genie EG2000							  */
	/*TODO*///		/* system 80 trs80 compatible */
	/*TODO*///	
	/*TODO*///		/* VIDEO TECHNOLOGY */
	/*TODO*///		DRIVER( laser110 )	/* 1983 Laser 110								  */
	/*TODO*///		DRIVER( laser200 )	/* 1983 Laser 200								  */
	/*TODO*///		DRIVER( laser210 )	/* 1983 Laser 210 (indentical to Laser 200 ?)	  */
	/*TODO*///		DRIVER( laser310 )	/* 1983 Laser 310 (210 with diff. keyboard and RAM) */
	/*TODO*///		DRIVER( vz200 ) 	/* 1983 Dick Smith Electronics / Sanyo VZ200	  */
	/*TODO*///		DRIVER( vz300 ) 	/* 1983 Dick Smith Electronics / Sanyo VZ300	  */
	/*TODO*///		DRIVER( fellow )	/* 1983 Salora Fellow (Finland) 				  */
	/*TODO*///		DRIVER( tx8000 )	/* 1983 Texet TX-8000 (U.K.)					  */
	/*TODO*///		DRIVER( laser350 )	/* 1984? Laser 350								  */
	/*TODO*///		DRIVER( laser500 )	/* 1984? Laser 500								  */
	/*TODO*///		DRIVER( laser700 )	/* 1984? Laser 700								  */
	/*TODO*///	
	/*TODO*///		/* Creativision console */
	/*TODO*///	
	/*TODO*///		/* TANGERINE */
	/*TODO*///		DRIVER( microtan )	/* 1979 Microtan 65 							  */
	/*TODO*///	
	/*TODO*///		DRIVER( oric1 ) 	/* 1983 Oric 1									  */
	/*TODO*///		DRIVER( orica ) 	/* 1984 Oric Atmos								  */
	/*TODO*///		DRIVER( prav8d )    /* 1985 Pravetz 8D                                  */
	/*TODO*///		DRIVER( prav8dd )   /* 1989 Pravetz 8D (Disk ROM)                       */
	/*TODO*///		DRIVER( prav8dda )  /* 1989 Pravetz 8D (Disk ROM, alternate)            */
	/*TODO*///		DRIVER( telstrat )	/* ??? Oric Telestrat/Stratos						*/
	/*TODO*///	
	/*TODO*///		/* PHILIPS */
	/*TODO*///		DRIVER( p2000t )	/* 1980 P2000T									  */
	/*TODO*///		DRIVER( p2000m )	/* 1980 P2000M									  */
	/*TODO*///		/* philips g7000 odyssey2 compatible */
	/*TODO*///	
	/*TODO*///		/* COMPUKIT */
	/*TODO*///		DRIVER( uk101 ) 	/* 1979 UK101									  */
	/*TODO*///	
	/*TODO*///		/* OHIO SCIENTIFIC */
	/*TODO*///		DRIVER( superbrd )	/* 1979 Superboard II							  */
	/*TODO*///	
	/*TODO*///		/* ASCII & MICROSOFT */
	driver_msx,		/* 1983 MSX 									  */
	/*TODO*///		DRIVER( msxj )		/* 1983 MSX Jap 								  */
	/*TODO*///		DRIVER( msxkr ) 	/* 1983 MSX Korean								  */
	/*TODO*///		DRIVER( msxuk ) 	/* 1983 MSX UK									  */
	/*TODO*///		DRIVER( hotbit11 )	/* 198? ???									      */
	/*TODO*///		DRIVER( hotbit12 )	/* 198? ???									      */
	/*TODO*///		DRIVER( expert10 )	/* 198? ???									      */
	/*TODO*///		DRIVER( expert11 )	/* 198? ???									      */
	driver_msx2, 		/* 1985 MSX2									  */
	driver_msx2a,		/* 1985 MSX2									  */
	/*TODO*///		DRIVER( msx2j ) 	/* 1983 MSX2 Jap								  */
	/*TODO*///	
	/*TODO*///		/* NASCOM MICROCOMPUTERS */
	/*TODO*///		DRIVER( nascom1 )	/* 1978 Nascom 1								  */
	/*TODO*///		DRIVER( nascom1a )  /**/
	/*TODO*///		DRIVER( nascom1b )  /**/
	/*TODO*///		DRIVER( nascom2 )	/* 1979 Nascom 2								  */
	/*TODO*///		DRIVER( nascom2a )	/* 1979 Nascom 2								  */
	/*TODO*///	
	/*TODO*///	
			/* MILES GORDON TECHNOLOGY */
	driver_coupe, 	/* 1989 Sam Coupe 256K RAM						  */
	driver_coupe512,	/* 1989 Sam Coupe 512K RAM						  */
	/*TODO*///	
	/*TODO*///		/* MOTOROLA */
	/*TODO*///	TESTDRIVER( mekd2 )     /* 1977 Motorola Evaluation Kit                   */
	/*TODO*///	
	/*TODO*///		/* DEC */
	/*TODO*///		DRIVER( pdp1 )      /* 1962 DEC PDP1 for SPACEWAR! - 1962             */
	/*TODO*///	
	/*TODO*///		/* MEMOTECH */
	/*TODO*///		DRIVER( mtx512 )    /* 1983 Memotech MTX512                           */
	/*TODO*///	
	/*TODO*///		/* MATTEL */
	/*TODO*///		DRIVER( intvkbd )	/* 1981 - Mattel Intellivision Keyboard Component */
	/*TODO*///							/* (Test marketed, later recalled )				  */
	/*TODO*///		DRIVER( aquarius )	/* 1983 Aquarius								  */
	/*TODO*///	
	/*TODO*///		/*EXIDY INC */
	/*TODO*///		DRIVER( exidy )  /* Sorcerer                                       */
	/*TODO*///	
	/*TODO*///		/* GALAKSIJA */
	/*TODO*///		DRIVER( galaxy )
	/*TODO*///	
	/*TODO*///		/* Team Concepts */
	/*TODO*///		/* CPU not known, else should be easy, look into systems/comquest.c */
	/*TODO*///	TESTDRIVER( comquest )	/* Comquest Plus German							*/
	/*TODO*///	
	/*TODO*///		/* Hewlett Packard */
	/*TODO*///	TESTDRIVER( hp48s ) 	/* HP48 S/SX										*/
	/*TODO*///	TESTDRIVER( hp48g ) 	/* HP48 G/GX										*/
	/*TODO*///	
	/*TODO*///		/* SpectraVideo */
	/*TODO*///		DRIVER( svi318 ) 	/* SVI-318										  */
	/*TODO*///		DRIVER( svi328 ) 	/* SVI-328										  */
	/*TODO*///		DRIVER( svi328a ) 	/* SVI-328	(BASIC 1.11)						  */
	/*TODO*///	
	/*TODO*///		/* Booth (this is the builder, not a company) */
	/*TODO*///		DRIVER( apexc )		/* 1951(?) APEXC : All-Purpose Electronic X-ray Computer */
	/*TODO*///	
	/*TODO*///	/****************Games*******************************************************/
	/*TODO*///	
	/*TODO*///		/* The Ideal Game Corp. */
	/*TODO*///		/* distributed by ARXON in Germany/Austria */
	/*TODO*///		/* PIC1655A (NMOS, not CMOS 16C55) dumping problems */
	/*TODO*///	/*	DRIVER( maniac )*/	/* Maniac										  */
	/*TODO*///	
	/*TODO*///		/* Computer Electronic */
	/*TODO*///		DRIVER( mk1 )		/* Chess Champion MK I							  */
	/*TODO*///		/* Quelle International */
	/*TODO*///		DRIVER( mk2 )		/* Chess Champion MK II							  */
	/*TODO*///		/* NOVAG Industries Ltd. */
	/*TODO*///	TESTDRIVER( ssystem3 )	/* Chess Champion Super System III / MK III		  */
	/*TODO*///	
	/*TODO*///		/* tchibo */
	/*TODO*///		/* single chip with ram, rom, io without label, how to dump? */
	/*TODO*///	/*	DRIVER( partner3)*/	/* Chess Partner 3 - Kasparov					  */
	/*TODO*///	
	/*TODO*///	#endif /* NEOMAME */
	/*TODO*///	
	/*TODO*///	#endif /* DRIVER_RECURSIVE */
	/*TODO*///	
	/*TODO*///	#endif /* TINY_COMPILE */
	/*TODO*///
			null
	};
}
