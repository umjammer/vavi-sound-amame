/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.includes;

import static mame056.inptport.*;

public class c16H
{
	
	public static int JOYSTICK1_PORT(){ return (input_port_7_r.handler(0)&0x80); }
	public static int JOYSTICK2_PORT(){ return (input_port_7_r.handler(0)&0x40); }
	
	public static int JOYSTICK_2_LEFT(){ return ((input_port_1_r.handler(0)&0x80)); }
	public static int JOYSTICK_2_RIGHT(){ return ((input_port_1_r.handler(0)&0x40)); }
	public static int JOYSTICK_2_UP(){ return ((input_port_1_r.handler(0)&0x20)); }
	public static int JOYSTICK_2_DOWN(){ return ((input_port_1_r.handler(0)&0x10)); }
	public static int JOYSTICK_2_BUTTON(){ return ((input_port_1_r.handler(0)&8)); }
	
	public static int JOYSTICK_1_LEFT(){ return ((input_port_0_r.handler(0)&0x80)); }
	public static int JOYSTICK_1_RIGHT(){ return ((input_port_0_r.handler(0)&0x40)); }
	public static int JOYSTICK_1_UP(){ return ((input_port_0_r.handler(0)&0x20)); }
	public static int JOYSTICK_1_DOWN(){ return ((input_port_0_r.handler(0)&0x10)); }
	public static int JOYSTICK_1_BUTTON(){ return ((input_port_0_r.handler(0)&8)); }
	
	public static int KEY_ESC(){ return (input_port_2_word_r.handler(0)&0x8000); }
	public static int KEY_1(){ return (input_port_2_word_r.handler(0)&0x4000); }
	public static int KEY_2(){ return (input_port_2_word_r.handler(0)&0x2000); }
	public static int KEY_3(){ return (input_port_2_word_r.handler(0)&0x1000); }
	public static int KEY_4(){ return (input_port_2_word_r.handler(0)&0x800); }
	public static int KEY_5(){ return (input_port_2_word_r.handler(0)&0x400); }
	public static int KEY_6(){ return (input_port_2_word_r.handler(0)&0x200); }
	public static int KEY_7(){ return (input_port_2_word_r.handler(0)&0x100); }
	public static int KEY_8(){ return (input_port_2_word_r.handler(0)&0x80); }
	public static int KEY_9(){ return (input_port_2_word_r.handler(0)&0x40); }
	public static int KEY_0(){ return (input_port_2_word_r.handler(0)&0x20); }
	public static int KEY_LEFT(){ return (input_port_2_word_r.handler(0)&0x10); }
	public static int KEY_RIGHT(){ return (input_port_2_word_r.handler(0)&8); }
	public static int KEY_UP(){ return (input_port_2_word_r.handler(0)&4); }
	public static int KEY_DOWN(){ return (input_port_2_word_r.handler(0)&2); }
	public static int KEY_DEL(){ return (input_port_2_word_r.handler(0)&1); }
	
	public static int KEY_CTRL(){ return (input_port_3_word_r.handler(0)&0x8000); }
	public static int KEY_Q(){ return (input_port_3_word_r.handler(0)&0x4000); }
	public static int KEY_W(){ return (input_port_3_word_r.handler(0)&0x2000); }
	public static int KEY_E(){ return (input_port_3_word_r.handler(0)&0x1000); }
	public static int KEY_R(){ return (input_port_3_word_r.handler(0)&0x800); }
	public static int KEY_T(){ return (input_port_3_word_r.handler(0)&0x400); }
	public static int KEY_Y(){ return (input_port_3_word_r.handler(0)&0x200); }
	public static int KEY_U(){ return (input_port_3_word_r.handler(0)&0x100); }
	public static int KEY_I(){ return (input_port_3_word_r.handler(0)&0x80); }
	public static int KEY_O(){ return (input_port_3_word_r.handler(0)&0x40); }
	public static int KEY_P(){ return (input_port_3_word_r.handler(0)&0x20); }
	public static int KEY_ATSIGN(){ return (input_port_3_word_r.handler(0)&0x10); }
	public static int KEY_PLUS(){ return (input_port_3_word_r.handler(0)&8); }
	public static int KEY_MINUS(){ return (input_port_3_word_r.handler(0)&4); }
	public static int KEY_HOME(){ return (input_port_3_word_r.handler(0)&2); }
	public static int KEY_STOP(){ return (input_port_3_word_r.handler(0)&1); }
	
	public static int KEY_SHIFTLOCK(){ return (input_port_4_word_r.handler(0)&0x8000); }
	public static int KEY_A(){ return (input_port_4_word_r.handler(0)&0x4000); }
	public static int KEY_S(){ return (input_port_4_word_r.handler(0)&0x2000); }
	public static int KEY_D(){ return (input_port_4_word_r.handler(0)&0x1000); }
	public static int KEY_F(){ return (input_port_4_word_r.handler(0)&0x800); }
	public static int KEY_G(){ return (input_port_4_word_r.handler(0)&0x400); }
	public static int KEY_H(){ return (input_port_4_word_r.handler(0)&0x200); }
	public static int KEY_J(){ return (input_port_4_word_r.handler(0)&0x100); }
	public static int KEY_K(){ return (input_port_4_word_r.handler(0)&0x80); }
	public static int KEY_L(){ return (input_port_4_word_r.handler(0)&0x40); }
	public static int KEY_SEMICOLON(){ return (input_port_4_word_r.handler(0)&0x20); }
	public static int KEY_COLON(){ return (input_port_4_word_r.handler(0)&0x10); }
	public static int KEY_ASTERIX(){ return (input_port_4_word_r.handler(0)&8); }
	public static int KEY_RETURN(){ return (input_port_4_word_r.handler(0)&4); }
	public static int KEY_CBM(){ return (input_port_4_word_r.handler(0)&2); }
	public static int KEY_LEFT_SHIFT(){ return (input_port_4_word_r.handler(0)&1); }
	
	
	public static int KEY_Z(){ return (input_port_5_word_r.handler(0)&0x8000); }
	public static int KEY_X(){ return (input_port_5_word_r.handler(0)&0x4000); }
	public static int KEY_C(){ return (input_port_5_word_r.handler(0)&0x2000); }
	public static int KEY_V(){ return (input_port_5_word_r.handler(0)&0x1000); }
	public static int KEY_B(){ return (input_port_5_word_r.handler(0)&0x800); }
	public static int KEY_N(){ return (input_port_5_word_r.handler(0)&0x400); }
	public static int KEY_M(){ return (input_port_5_word_r.handler(0)&0x200); }
	public static int KEY_COMMA(){ return (input_port_5_word_r.handler(0)&0x100); }
	public static int KEY_POINT(){ return (input_port_5_word_r.handler(0)&0x80); }
	public static int KEY_SLASH(){ return (input_port_5_word_r.handler(0)&0x40); }
	public static int KEY_RIGHT_SHIFT(){ return (input_port_5_word_r.handler(0)&0x20); }
	public static int KEY_POUND(){ return (input_port_5_word_r.handler(0)&0x10); }
	public static int KEY_EQUALS(){ return (input_port_5_word_r.handler(0)&8); }
	public static int KEY_SPACE(){ return (input_port_5_word_r.handler(0)&4); }
	public static int KEY_F1(){ return (input_port_5_word_r.handler(0)&2); }
	public static int KEY_F2(){ return (input_port_5_word_r.handler(0)&1); }
	
	public static int KEY_F3(){ return (input_port_6_word_r.handler(0)&0x8000); }
	public static int KEY_HELP(){ return (input_port_6_word_r.handler(0)&0x4000); }
	
	public static int JOYSTICK_SWAP(){ return (input_port_6_word_r.handler(0)&0x2000); }
	
	public static int DATASSETTE_PLAY(){ return (input_port_6_word_r.handler(0)&4); }
	public static int DATASSETTE_RECORD(){ return (input_port_6_word_r.handler(0)&2); }
	public static int DATASSETTE_STOP(){ return (input_port_6_word_r.handler(0)&1); }
	
	public static int QUICKLOAD(){ return (input_port_6_word_r.handler(0)&8); }
	
	public static int KEY_SHIFT(){ return (KEY_LEFT_SHIFT()!=0||KEY_RIGHT_SHIFT()!=0||KEY_SHIFTLOCK()!=0)? 1:0; }
	
	public static int DATASSETTE(){ return (input_port_7_r.handler(0)&0x20); }
	public static int DATASSETTE_TONE(){ return (input_port_7_r.handler(0)&0x10); }
	
	public static int NO_REAL_FLOPPY(){ return ((input_port_8_r.handler(0)&0xc0)==0)?1:0; }
	public static int REAL_C1551(){ return ((input_port_8_r.handler(0)&0xc0)==0x40)?1:0; }
	public static int REAL_VC1541(){ return ((input_port_8_r.handler(0)&0xc0)==0x80)?1:0; }
	
	public static int IEC8ON(){ return ((input_port_8_r.handler(0)&0x38)==8)?1:0; }
	public static int IEC9ON(){ return ((input_port_8_r.handler(0)&7)==1)?1:0; }
	
	public static int SERIAL8ON(){ return ((input_port_8_r.handler(0)&0x38)==0x18)?1:0; }
	public static int SERIAL9ON(){ return ((input_port_8_r.handler(0)&7)==3)?1:0; }
	
	public static int SIDCARD(){ return ((input_port_9_r.handler(0)&0x80)); }
	// a lot of c64 software has been converted to c16
	// these oftenly still produce the commands for the sid chip at 0xd400
	// with following hack you can hear these sounds
	public static int SIDCARD_HACK(){ return ((input_port_9_r.handler(0)&0x40)); }
	
	public static int C16_PAL(){ return ((input_port_9_r.handler(0)&0x10)==0)?1:0; }
	
	public static int TYPE_C16(){ return ((input_port_9_r.handler(0)&0xc)==0)?1:0; }
	public static int TYPE_PLUS4(){ return ((input_port_9_r.handler(0)&0xc)==4)?1:0; }
	public static int TYPE_364(){ return ((input_port_9_r.handler(0)&0xc)==8)?1:0; }
	
	public static int DIPMEMORY(){ return (input_port_9_r.handler(0)&3); }
	public static final int MEMORY16K = 0;
	public static final int MEMORY32K = 2;
	public static final int MEMORY64K = 3;
	
        /*
	extern UINT8 *c16_memory;
	
	extern WRITE_HANDLER(c16_m7501_port_w);
	extern READ_HANDLER(c16_m7501_port_r);
	
	extern WRITE_HANDLER(c16_6551_port_w);
	extern READ_HANDLER(c16_6551_port_r);
	
	extern READ_HANDLER(c16_fd1x_r);
	extern WRITE_HANDLER(plus4_6529_port_w);
	extern READ_HANDLER(plus4_6529_port_r);
	
	extern WRITE_HANDLER(c16_6529_port_w);
	extern READ_HANDLER(c16_6529_port_r);
	
	extern WRITE_HANDLER(c364_speech_w);
	extern READ_HANDLER(c364_speech_r);
	
	#if 0
	extern WRITE_HANDLER(c16_iec9_port_w);
	extern READ_HANDLER(c16_iec9_port_r);
	
	extern WRITE_HANDLER(c16_iec8_port_w);
	extern READ_HANDLER(c16_iec8_port_r);
	
	#endif
	
	extern WRITE_HANDLER(c16_select_roms);
	extern WRITE_HANDLER(c16_switch_to_rom);
	extern WRITE_HANDLER(c16_switch_to_ram);
	extern WRITE_HANDLER(plus4_switch_to_ram);
	
	extern int c16_read_keyboard (int databus);
	extern void c16_interrupt (int);
	
	extern void c16_driver_init (void);
	extern void c16_driver_shutdown (void);
	extern void c16_init_machine (void);
	extern void c16_shutdown_machine (void);
	extern int c16_frame_interrupt (void);
	
	extern int c16_rom_init (int id);
	extern int c16_rom_load (int id);
	
	#endif
        */
}
