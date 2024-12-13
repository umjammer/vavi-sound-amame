/***************************************************************************
	commodore c64 home computer

    peter.trauner@jk.uni-linz.ac.at
    documentation
     www.funet.fi
***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.includes;

import static mame056.inptport.*;
import static mame056.inptportH.*;
import static mame056.inputH.*;
import static mess056.includes.sid6581H.*;

public class c64H
{
	
	public static void C64_DIPS() {
	     PORT_START();
	     PORT_BIT( 0x800, IP_ACTIVE_HIGH, IPT_BUTTON1);
	     PORT_BIT( 0x400, IP_ACTIVE_HIGH, IPT_BUTTON2);
	     PORT_BIT( 0x8000, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT | IPF_8WAY );
	     PORT_BIT( 0x4000, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_8WAY );
             PORT_BIT( 0x2000, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP | IPF_8WAY );
            PORT_BIT( 0x1000, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN | IPF_8WAY );
            PORT_BIT ( 0x300, 0x0,	 IPT_UNUSED );
            PORT_BITX( 8, IP_ACTIVE_HIGH, IPT_BUTTON1|IPF_PLAYER2, 
                       "P2 Button", KEYCODE_INSERT,JOYCODE_2_BUTTON1 );
            PORT_BITX( 4, IP_ACTIVE_HIGH, IPT_BUTTON2|IPF_PLAYER2, 
                       "P2 Button 2", KEYCODE_PGUP,JOYCODE_2_BUTTON2 );
            PORT_BITX( 0x80, IP_ACTIVE_HIGH, 
                       IPT_JOYSTICK_LEFT|IPF_PLAYER2 | IPF_8WAY,
                       "P2 Left",KEYCODE_DEL,JOYCODE_2_LEFT );
            PORT_BITX( 0x40, IP_ACTIVE_HIGH, 
                       IPT_JOYSTICK_RIGHT|IPF_PLAYER2 | IPF_8WAY,
                       "P2 Right",KEYCODE_PGDN,JOYCODE_2_RIGHT );
            PORT_BITX( 0x20, IP_ACTIVE_HIGH, 
                       IPT_JOYSTICK_UP|IPF_PLAYER2 | IPF_8WAY,
                       "P2 Up", KEYCODE_HOME, JOYCODE_2_UP);
            PORT_BITX( 0x10, IP_ACTIVE_HIGH, 
                       IPT_JOYSTICK_DOWN|IPF_PLAYER2 | IPF_8WAY,
                       "P2 Down", KEYCODE_END, JOYCODE_2_DOWN);
            PORT_BIT ( 0x3, 0x0,	 IPT_UNUSED );
            PORT_START();
            PORT_BITX( 0x100, IP_ACTIVE_HIGH, IPT_BUTTON1, 
                       "Paddle 1 Button", KEYCODE_LCONTROL, JOYCODE_1_BUTTON1);
            PORT_ANALOGX(0xff,128,IPT_PADDLE|IPF_REVERSE,
                         30,20,0,255,KEYCODE_LEFT,KEYCODE_RIGHT,
                         JOYCODE_1_LEFT,JOYCODE_1_RIGHT);
            PORT_START();
            PORT_BITX( 0x100, IP_ACTIVE_HIGH, IPT_BUTTON2, 
                       "Paddle 2 Button", KEYCODE_LALT, JOYCODE_1_BUTTON2);
            PORT_ANALOGX(0xff,128,IPT_PADDLE|IPF_PLAYER2|IPF_REVERSE,
                         30,20,0,255,KEYCODE_DOWN,KEYCODE_UP,
                         JOYCODE_1_UP,JOYCODE_1_DOWN);
            PORT_START();
            PORT_BITX( 0x100, IP_ACTIVE_HIGH, IPT_BUTTON3, 
                       "Paddle 3 Button", KEYCODE_INSERT,JOYCODE_2_BUTTON1 );
            PORT_ANALOGX(0xff,128,IPT_PADDLE|IPF_PLAYER3|IPF_REVERSE,
                         30,20,0,255,KEYCODE_HOME,KEYCODE_PGUP,JOYCODE_NONE,JOYCODE_NONE);
	     PORT_START();
		PORT_BITX( 0x100, IP_ACTIVE_HIGH, IPT_BUTTON4, 
			   "Paddle 4 Button", KEYCODE_DEL, JOYCODE_2_BUTTON2);
		PORT_ANALOGX(0xff,128,IPT_PADDLE|IPF_PLAYER4|IPF_REVERSE,
			     30,20,0,255,KEYCODE_END,KEYCODE_PGDN,JOYCODE_NONE,JOYCODE_NONE);
		PORT_START();
		PORT_BITX( 0x8000, IP_ACTIVE_HIGH, IPT_BUTTON1, 
			"Mouse Button Left", KEYCODE_LCONTROL, JOYCODE_1_BUTTON1 );
		PORT_BITX( 0x4000, IP_ACTIVE_HIGH, IPT_BUTTON1, 
			"Mouse Button Right", KEYCODE_LALT, JOYCODE_1_BUTTON2);
		PORT_ANALOGX( 0x7e, 0x00, IPT_TRACKBALL_X | IPF_PLAYER1, 100, 0, 0, 0, KEYCODE_NONE, KEYCODE_NONE, JOYCODE_NONE, JOYCODE_NONE );
			/*PORT_BITX( 0x8000, IP_ACTIVE_HIGH, IPT_BUTTON2,*/ 
		  /*"Lightpen Signal", KEYCODE_LCONTROL, 0);*/
	     /*PORT_ANALOGX(0x1ff,0,IPT_PADDLE|IPF_PLAYER1,*/
		   /*30,2,0,320-1,KEYCODE_LEFT,KEYCODE_RIGHT,*/
			 /*JOYCODE_1_LEFT,JOYCODE_1_RIGHT);*/
	     PORT_START();
		PORT_ANALOGX( 0x7e, 0x00, IPT_TRACKBALL_Y | IPF_PLAYER1 | IPF_REVERSE, 100, 0, 0, 0, KEYCODE_NONE, KEYCODE_NONE, JOYCODE_NONE, JOYCODE_NONE );
	     /*PORT_ANALOGX(0xff,0,IPT_PADDLE|IPF_PLAYER2,*/
			  /*30,2,0,200-1,KEYCODE_UP,KEYCODE_DOWN,*/
			  /*JOYCODE_1_UP,JOYCODE_1_DOWN);*/
		PORT_START();
		PORT_DIPNAME ( 0xe000, 0x2000, "Gameport A");
		PORT_DIPSETTING(  0, "None" );
		PORT_DIPSETTING(	0x2000, "Joystick 1" );
		PORT_DIPSETTING(	0x4000, "Paddles 1, 2" );
		PORT_DIPSETTING(	0x6000, "Mouse Joystick Emulation/2 Button Joystick" );
		PORT_DIPSETTING(	0x8000, "Mouse" );
		/*PORT_DIPSETTING(	0xa000, "Lightpen" );*/
		/*PORT_DIPNAME ( 0x1000, 0x1000, "Lightpen Draw Pointer");*/
		PORT_DIPSETTING(  0, DEF_STR( "Off") );
		PORT_DIPSETTING(  0x1000, DEF_STR( "On") );
		PORT_DIPNAME ( 0xe00, 0x200, "Gameport B");
		PORT_DIPSETTING(  0, "None" );
		PORT_DIPSETTING(	0x0200, "Joystick 2" );
		PORT_DIPSETTING(	0x0400, "Paddles 3, 4" );
		PORT_DIPSETTING(	0x0600, "Mouse Joystick Emulation/2 Button Joystick" );
		PORT_DIPSETTING(	0x0800, "Mouse" );
		PORT_BITX( 0x100, IP_ACTIVE_HIGH, IPT_DIPSWITCH_NAME|IPF_TOGGLE,
			     "Swap Gameport 1 and 2", KEYCODE_NUMLOCK, IP_JOY_NONE);
		PORT_DIPSETTING(  0, DEF_STR( "No") );
		PORT_DIPSETTING(	0x100, DEF_STR( "Yes") );
	}
	
	/*TODO*///#if 0
	/*TODO*///    PORT_BITX( 0x100, IP_ACTIVE_HIGH, IPF_TOGGLE,
	/*TODO*///		   "Swap Gameport 1 and 2", KEYCODE_NUMLOCK, IP_JOY_NONE);#endif
	
	public static int LIGHTPEN		(){ return ((input_port_7_word_r.handler(0)&0xe000)==0xa000)?1:0; }
	public static int MOUSE1		(){ return ((input_port_7_word_r.handler(0)&0xe000)==0x8000)?1:0; }
	public static int JOYSTICK1_2BUTTON	(){ return ((input_port_7_word_r.handler(0)&0xe000)==0x6000)?1:0; }
	public static int PADDLES12		(){ return ((input_port_7_word_r.handler(0)&0xe000)==0x4000)?1:0; }
	public static int JOYSTICK1		(){ return ((input_port_7_word_r.handler(0)&0xe000)==0x2000)?1:0; }
	public static int LIGHTPEN_POINTER	(){ return (LIGHTPEN()!=0&&(input_port_7_word_r.handler(0)&0x1000)!=0)?1:0; }
	public static int MOUSE2		(){ return ((input_port_7_word_r.handler(0)&0xe00)==0x800)?1:0; }
	public static int JOYSTICK2_2BUTTON     (){ return ((input_port_7_word_r.handler(0)&0xe00)==0x600)?1:0; }
	public static int PADDLES34		(){ return ((input_port_7_word_r.handler(0)&0xe00)==0x400)?1:0; }
	public static int JOYSTICK2		(){ return ((input_port_7_word_r.handler(0)&0xe00)==0x200)?1:0; }
	public static int JOYSTICK_SWAP		(){ return ((input_port_7_word_r.handler(0)&0x100)!=0)?1:0; }

	public static int PADDLE1_BUTTON	(){ return ((input_port_1_word_r.handler(0)&0x100)); }
	public static int PADDLE1_VALUE         (){ return (input_port_1_word_r.handler(0)&0xff); }
	public static int PADDLE2_BUTTON	(){ return (input_port_2_word_r.handler(0)&0x100); }
	public static int PADDLE2_VALUE         (){ return (input_port_2_word_r.handler(0)&0xff); }
	public static int PADDLE3_BUTTON	(){ return ((input_port_3_word_r.handler(0)&0x100)); }
	public static int PADDLE3_VALUE         (){ return (input_port_3_word_r.handler(0)&0xff); }
	public static int PADDLE4_BUTTON	(){ return ((input_port_4_word_r.handler(0)&0x100)); }
	public static int PADDLE4_VALUE         (){ return (input_port_4_word_r.handler(0)&0xff); }

	public static int MOUSE1_BUTTON1        (){ return (MOUSE1()!=0&&(readinputport(5)&0x8000)!=0)?1:0; }
	public static int MOUSE1_BUTTON2        (){ return (MOUSE1()!=0&&(readinputport(5)&0x4000)!=0)?1:0; }
	public static int MOUSE1_X              (){ return ((readinputport(5)&0x3ff)); }
	public static int MOUSE1_Y              (){ return (readinputport(6)); }

	public static int MOUSE2_BUTTON1        (){ return (MOUSE1()!=0&&(readinputport(5)&0x8000)!=0)?1:0; }
	public static int MOUSE2_BUTTON2        (){ return (MOUSE1()!=0&&(readinputport(5)&0x4000)!=0)?1:0; }
	public static int MOUSE2_X              (){ return ((readinputport(5)&0x3ff)); }
	public static int MOUSE2_Y              (){ return (readinputport(6)); }

	public static int LIGHTPEN_BUTTON       (){ return (LIGHTPEN()!=0&&(readinputport(5)&0x8000)!=0)?1:0; }
	public static int LIGHTPEN_X_VALUE      (){ return ((readinputport(5)&0x3ff)&~1)	/* effectiv resolution */; }
	public static int LIGHTPEN_Y_VALUE      (){ return (readinputport(6)&~1)	/* effectiv resolution */; }

	public static int JOYSTICK_1_LEFT	(){ return ((input_port_0_word_r.handler(0)&0x8000)); }
	public static int JOYSTICK_1_RIGHT	(){ return ((input_port_0_word_r.handler(0)&0x4000)); }
	public static int JOYSTICK_1_UP		(){ return ((input_port_0_word_r.handler(0)&0x2000)); }
	public static int JOYSTICK_1_DOWN	(){ return ((input_port_0_word_r.handler(0)&0x1000)); }
	public static int JOYSTICK_1_BUTTON     (){ return ((input_port_0_word_r.handler(0)&0x800)); }
	public static int JOYSTICK_1_BUTTON2    (){ return ((input_port_0_word_r.handler(0)&0x400)); }
	public static int JOYSTICK_2_LEFT	(){ return ((input_port_0_word_r.handler(0)&0x80)); }
	public static int JOYSTICK_2_RIGHT	(){ return ((input_port_0_word_r.handler(0)&0x40)); }
	public static int JOYSTICK_2_UP		(){ return ((input_port_0_word_r.handler(0)&0x20)); }
	public static int JOYSTICK_2_DOWN	(){ return ((input_port_0_word_r.handler(0)&0x10)); }
	public static int JOYSTICK_2_BUTTON     (){ return ((input_port_0_word_r.handler(0)&8)); }
	public static int JOYSTICK_2_BUTTON2    (){ return ((input_port_0_word_r.handler(0)&4)); }

	public static int QUICKLOAD		(){ return (input_port_8_word_r.handler(0)&0x8000); }
	public static int DATASSETTE            (){ return (input_port_8_word_r.handler(0)&0x4000); }
	public static int DATASSETTE_TONE       (){ return (input_port_8_word_r.handler(0)&0x2000); }

	public static int DATASSETTE_PLAY	(){ return (input_port_8_word_r.handler(0)&0x1000); }
	public static int DATASSETTE_RECORD	(){ return (input_port_8_word_r.handler(0)&0x800); }
	public static int DATASSETTE_STOP	(){ return (input_port_8_word_r.handler(0)&0x400); }

	public static int SID8580		(){ return ((input_port_8_r.handler(0)&0x80)!=0 ? MOS8580 : MOS6581); }

	public static int AUTO_MODULE           (){ return ((input_port_8_r.handler(0)&0x1c)==0)?1:0; }
	public static int ULTIMAX_MODULE        (){ return ((input_port_8_r.handler(0)&0x1c)==4)?1:0; }
	public static int C64_MODULE            (){ return ((input_port_8_r.handler(0)&0x1c)==8)?1:0; }
	public static int SUPERGAMES_MODULE     (){ return ((input_port_8_r.handler(0)&0x1c)==0x10)?1:0; }
	public static int ROBOCOP2_MODULE       (){ return ((input_port_8_r.handler(0)&0x1c)==0x14)?1:0; }
	public static int C128_MODULE           (){ return ((input_port_8_r.handler(0)&0x1c)==0x18)?1:0; }

	public static int SERIAL8ON             (){ return (input_port_8_r.handler(0)&2); }
	public static int SERIAL9ON             (){ return (input_port_8_r.handler(0)&1); }

	public static int KEY_ARROW_LEFT        (){ return (input_port_9_word_r.handler(0)&0x8000); }
	public static int KEY_1                 (){ return (input_port_9_word_r.handler(0)&0x4000); }
	public static int KEY_2                 (){ return (input_port_9_word_r.handler(0)&0x2000); }
	public static int KEY_3                 (){ return (input_port_9_word_r.handler(0)&0x1000); }
	public static int KEY_4                 (){ return (input_port_9_word_r.handler(0)&0x800); }
	public static int KEY_5                 (){ return (input_port_9_word_r.handler(0)&0x400); }
	public static int KEY_6                 (){ return (input_port_9_word_r.handler(0)&0x200); }
	public static int KEY_7                 (){ return (input_port_9_word_r.handler(0)&0x100); }
	public static int KEY_8                 (){ return (input_port_9_word_r.handler(0)&0x80); }
	public static int KEY_9                 (){ return (input_port_9_word_r.handler(0)&0x40); }
	public static int KEY_0                 (){ return (input_port_9_word_r.handler(0)&0x20); }
	public static int KEY_PLUS              (){ return (input_port_9_word_r.handler(0)&0x10); }
	public static int KEY_MINUS             (){ return (input_port_9_word_r.handler(0)&8); }
	public static int KEY_POUND             (){ return (input_port_9_word_r.handler(0)&4); }
	public static int KEY_HOME              (){ return (input_port_9_word_r.handler(0)&2); }
	public static int KEY_DEL               (){ return (input_port_9_word_r.handler(0)&1); }

	public static int KEY_CTRL              (){ return (input_port_10_word_r.handler(0)&0x8000); }
	public static int KEY_Q                 (){ return (input_port_10_word_r.handler(0)&0x4000); }
	public static int KEY_W                 (){ return (input_port_10_word_r.handler(0)&0x2000); }
	public static int KEY_E                 (){ return (input_port_10_word_r.handler(0)&0x1000); }
	public static int KEY_R                 (){ return (input_port_10_word_r.handler(0)&0x800); }
	public static int KEY_T                 (){ return (input_port_10_word_r.handler(0)&0x400); }
	public static int KEY_Y                 (){ return (input_port_10_word_r.handler(0)&0x200); }
	public static int KEY_U                 (){ return (input_port_10_word_r.handler(0)&0x100); }
	public static int KEY_I                 (){ return (input_port_10_word_r.handler(0)&0x80); }
	public static int KEY_O                 (){ return (input_port_10_word_r.handler(0)&0x40); }
	public static int KEY_P                 (){ return (input_port_10_word_r.handler(0)&0x20); }
	public static int KEY_ATSIGN            (){ return (input_port_10_word_r.handler(0)&0x10); }
	public static int KEY_ASTERIX           (){ return (input_port_10_word_r.handler(0)&8); }
	public static int KEY_ARROW_UP          (){ return (input_port_10_word_r.handler(0)&4); }
	public static int KEY_RESTORE           (){ return (input_port_10_word_r.handler(0)&2); }
	public static int KEY_STOP              (){ return (input_port_10_word_r.handler(0)&1); }

	public static int KEY_SHIFTLOCK         (){ return (input_port_11_word_r.handler(0)&0x8000); }
	public static int KEY_A                 (){ return (input_port_11_word_r.handler(0)&0x4000); }
	public static int KEY_S                 (){ return (input_port_11_word_r.handler(0)&0x2000); }
	public static int KEY_D                 (){ return (input_port_11_word_r.handler(0)&0x1000); }
	public static int KEY_F                 (){ return (input_port_11_word_r.handler(0)&0x800); }
	public static int KEY_G                 (){ return (input_port_11_word_r.handler(0)&0x400); }
	public static int KEY_H                 (){ return (input_port_11_word_r.handler(0)&0x200); }
	public static int KEY_J                 (){ return (input_port_11_word_r.handler(0)&0x100); }
	public static int KEY_K                 (){ return (input_port_11_word_r.handler(0)&0x80); }
	public static int KEY_L                 (){ return (input_port_11_word_r.handler(0)&0x40); }
	public static int KEY_SEMICOLON         (){ return (input_port_11_word_r.handler(0)&0x20); }
	public static int KEY_COLON             (){ return (input_port_11_word_r.handler(0)&0x10); }
	public static int KEY_EQUALS            (){ return (input_port_11_word_r.handler(0)&8); }
	public static int KEY_RETURN            (){ return (input_port_11_word_r.handler(0)&4); }
	public static int KEY_CBM               (){ return (input_port_11_word_r.handler(0)&2); }
	public static int KEY_LEFT_SHIFT        (){ return ((input_port_11_word_r.handler(0)&1)!=0||KEY_SHIFTLOCK()!=0)?1:0; }

	public static int KEY_Z                 (){ return (input_port_12_word_r.handler(0)&0x8000); }
	public static int KEY_X                 (){ return (input_port_12_word_r.handler(0)&0x4000); }
	public static int KEY_C                 (){ return (input_port_12_word_r.handler(0)&0x2000); }
	public static int KEY_V                 (){ return (input_port_12_word_r.handler(0)&0x1000); }
	public static int KEY_B                 (){ return (input_port_12_word_r.handler(0)&0x800); }
	public static int KEY_N                 (){ return (input_port_12_word_r.handler(0)&0x400); }
	public static int KEY_M                 (){ return (input_port_12_word_r.handler(0)&0x200); }
	public static int KEY_COMMA             (){ return (input_port_12_word_r.handler(0)&0x100); }
	public static int KEY_POINT             (){ return (input_port_12_word_r.handler(0)&0x80); }
	public static int KEY_SLASH             (){ return (input_port_12_word_r.handler(0)&0x40); }
	public static int KEY_RIGHT_SHIFT       (){ return ((input_port_12_word_r.handler(0)&0x20)!=0
				 ||KEY_CURSOR_UP()!=0||KEY_CURSOR_LEFT()!=0)?1:0; }
	public static int KEY_CURSOR_DOWN       (){ return ((input_port_12_word_r.handler(0)&0x10)!=0||KEY_CURSOR_UP()!=0)?1:0; }
	public static int KEY_CURSOR_RIGHT      (){ return ((input_port_12_word_r.handler(0)&8)!=0||KEY_CURSOR_LEFT()!=0)?1:0; }
	public static int KEY_SPACE             (){ return (input_port_12_word_r.handler(0)&4); }
	public static int KEY_F1                (){ return (input_port_12_word_r.handler(0)&2); }
	public static int KEY_F3                (){ return (input_port_12_word_r.handler(0)&1); }

	public static int KEY_F5                (){ return (input_port_13_word_r.handler(0)&0x8000); }
	public static int KEY_F7                (){ return (input_port_13_word_r.handler(0)&0x4000); }
	public static int KEY_CURSOR_UP         (){ return (input_port_13_word_r.handler(0)&0x2000); }
	public static int KEY_CURSOR_LEFT       (){ return (input_port_13_word_r.handler(0)&0x1000); }

	
}
