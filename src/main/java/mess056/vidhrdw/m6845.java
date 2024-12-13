
/************************************************************************
	crct6845

	MESS Driver By:

 	Gordon Jefferyes
 	mess_bbc@gjeffery.dircon.co.uk

 ************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
package mess056.vidhrdw;

import static mess056.vidhrdw.m6845H.*;
import static common.libc.cstring.*;

public class m6845 {
	
	public static int True  = 1;
	public static int False = 0;
	
	public static int Cursor_Start_Delay_Flag       = 1;
	public static int Cursor_On_Flag                = 2;
	public static int Display_Enabled_Delay_Flag    = 4;
	public static int Display_Disable_Delay_Flag    = 8;
        
        public static crtc6845_state crtc = new crtc6845_state();
	
	/* total number of chr -1 */
	public static int R0_horizontal_total(){
            return crtc.registers[0];
        }
        public static void R0_horizontal_total(int i){
            crtc.registers[0] = i;
        }
	/* total number of displayed chr */
	public static int R1_horizontal_displayed(){
            return crtc.registers[1];
        }
        public static void R1_horizontal_displayed(int i){
            crtc.registers[1]=i;
        }
	/* position of horizontal Sync pulse */
	public static int R2_horizontal_sync_position(){
            return crtc.registers[2];
        }
        public static void R2_horizontal_sync_position(int i){
            crtc.registers[2]=i;
        }
	/* HSYNC & VSYNC width */
	public static int R3_sync_width(){
            return crtc.registers[3];
        }
        public static void R3_sync_width(int i){
            crtc.registers[3]=i;
        }
	/* total number of character rows -1 */
	public static int R4_vertical_total(){
            return crtc.registers[4];
        }
        public static void R4_vertical_total(int i){
            crtc.registers[4]=i;
        }
	/* *** Not implemented yet ***
	R5 Vertical total adjust
	This 5 bit write only register is programmed with the fraction
	for use in conjunction with register R4. It is programmed with
	a number of scan lines. If can be varied
	slightly in conjunction with R4 to move the whole display area
	up or down a little on the screen.
	BBC Emulator: It is usually set to 0 except
	when using mode 3,6 and 7 in which it is set to 2
	*/
	public static int R5_vertical_total_adjust(){
            return crtc.registers[5];
        }
        public static void R5_vertical_total_adjust(int i){
            crtc.registers[5]=i;
        }
	/* total number of displayed chr rows */
	public static int R6_vertical_displayed(){
            return crtc.registers[6];
        }
        public static void R6_vertical_displayed(int i){
            crtc.registers[6]=i;
        }
	/* position of vertical sync pulse */
	public static int R7_vertical_sync_position(){
            return crtc.registers[7];
        }
        public static void R7_vertical_sync_position(int i){
            crtc.registers[7]=i;
        }
	/* *** Part not implemented ***
	R8 interlace settings
	Interlace mode (bits 0,1)
	Bit 1	Bit 0	Description
	0		0		Normal (non-interlaced) sync mode
	1		0		Normal (non-interlaced) sync mode
	0		1		Interlace sync mode
	1		1		Interlace sync and video
	*/
	public static int R8_interlace_display_enabled(){
            return crtc.registers[8];
        }
        public static void R8_interlace_display_enabled(int i){
            crtc.registers[8]=i;
        }
	/* scan lines per character -1 */
	public static int R9_scan_lines_per_character(){
            return crtc.registers[9];
        }
        public static void R9_scan_lines_per_character(int i){
            crtc.registers[9]=i;
        }
	/* *** Part not implemented yet ***
	R10 The cursor start register
	Bit 6 	Bit 5
	0		0		Solid cursor
	0		1		No cursor (This no cursor setting is working)
	1		0		slow flashing cursor
	1		1		fast flashing cursor
	*/
	public static int R10_cursor_start(){
            return crtc.registers[10];
        }
        public static void R10_cursor_start(int i){
            crtc.registers[10]=i;
        }
	/* cursor end row */
	public static int R11_cursor_end(){
            return crtc.registers[11];
        }
        public static void R11_cursor_end(int i){
            crtc.registers[11]=i;
        }
	/* screen start high */
	public static int R12_screen_start_address_H(){
            return crtc.registers[12];
        }
        public static void R12_screen_start_address_H(int i){
            crtc.registers[12]=i;
        }
	/* screen start low */
	public static int R13_screen_start_address_L(){
            return crtc.registers[13];
        }
        public static void R13_screen_start_address_L(int i){
            crtc.registers[13]=i;
        }
	/* Cursor address high */
	public static int R14_cursor_address_H(){
            return crtc.registers[14];
        }
        public static void R14_cursor_address_H(int i){
            crtc.registers[14]=i;
        }
	/* Cursor address low */
	public static int R15_cursor_address_L(){
            return crtc.registers[15];
        }
        public static void R15_cursor_address_L(int i){
            crtc.registers[15]=i;
        }
	/* *** Not implemented yet *** */
	public static int R16_light_pen_address_H(){
            return crtc.registers[16];
        }
        public static void R16_light_pen_address_H(int i){
            crtc.registers[16]=i;
        }
	/* *** Not implemented yet *** */
	public static int R17_light_pen_address_L(){
            return crtc.registers[17];
        }
        public static void R17_light_pen_address_L(int i){
            crtc.registers[17]=i;
        }
	
	/*TODO*///#if 0
	/*TODO*////* VSYNC functions */
	/*TODO*///
	/*TODO*////* timer to set vsync */
	/*TODO*///static void *crtc6845_vsync_set_timer = NULL;
	/*TODO*////* timer to reset vsync */
	/*TODO*///static void *crtc6845_vsync_clear_timer = NULL;
	/*TODO*///
	/*TODO*///static void crtc6845_vsync_set_timer_callback(int);
	/*TODO*///static void crtc6845_vsync_clear_timer_callback(int);
	/*TODO*///static void crtc6845_remove_vsync_set_timer(void);
	/*TODO*///static void crtc6845_remove_vsync_clear_timer(void);
	/*TODO*///static void crtc6845_set_new_vsync_set_time(int);
	/*TODO*///static void crtc6845_set_new_vsync_clear_time(int);
	/*TODO*///static void crtc6845_recalc_cycles_to_vsync_start(void);
	/*TODO*///static void crtc6845_recalc_cycles_to_vsync_end(void);
	/*TODO*///
	/*TODO*///#endif
	
	// local copy of the 6845 external procedure calls
	static crtc6845_interface crct6845_calls = new crtc6845_interface(
		null,// Memory Address register
		null,// Row Address register
		null,// Horizontal status
		null,// Vertical status
		null,// Display Enabled status
		null,// Cursor status
	
	
		null// Cursor status allways called
		// As the BBC video emulation does not redraw all off the screen every time
		// This function outputs:
		// B1 set to 1 when at cursor position, set to 0 when past cursor
		// B0 set to 1 when the cursor is on and 0 when the cursor is off
		// this function is call at the cursor location even if the cursor is off
		// this means that the cursor will get clear from the screen
	
	);
	
	/* set up the local copy of the 6845 external procedure calls */
	public static void crtc6845_config(crtc6845_interface intf)
	{
		crct6845_calls.out_MA_func=intf.out_MA_func;
		crct6845_calls.out_RA_func=intf.out_RA_func;
		crct6845_calls.out_HS_func=intf.out_HS_func;
		crct6845_calls.out_VS_func=intf.out_VS_func;
		crct6845_calls.out_DE_func=intf.out_DE_func;
		crct6845_calls.out_CR_func=intf.out_CR_func;
		crct6845_calls.out_CRE_func=intf.out_CRE_func;
	}
	
	public static void crtc6845_start()
	{
	/*TODO*///#if 0
	/*TODO*///	crtc6845_vsync_set_timer = NULL;
	/*TODO*///	crtc6845_vsync_clear_timer = NULL;
	/*TODO*///#endif
	}
	
	public static void crtc6845_stop()
	{
	/*TODO*///#if 0
	/*TODO*///	crtc6845_remove_vsync_set_timer();
	/*TODO*///	crtc6845_remove_vsync_clear_timer();
	/*TODO*///#endif
	}
	
	
	/* 6845 registers */
	
	/* functions to set the 6845 registers */
	public static void crtc6845_address_w(int offset, int data)
	{
		crtc.address_register=data & 0x1f;
	}
	
	public static void crtc6845_get_state(int offset, crtc6845_state state)
	{
		//memcpy(state, &crtc, sizeof(crtc6845_state));
                state.address_register = crtc.address_register;
                state.registers = crtc.registers;
                state.vertical_sync_width = crtc.vertical_sync_width;
                state.horizontal_sync_width = crtc.horizontal_sync_width;

                state.screen_start_address = crtc.screen_start_address;
                state.cursor_address = crtc.cursor_address;
                state.light_pen_address = crtc.light_pen_address;

                state.scan_lines_increment = crtc.scan_lines_increment;

                state.Horizontal_Counter = crtc.Horizontal_Counter;
                state.Horizontal_Counter_Reset = crtc.Horizontal_Counter_Reset;

                state.Scan_Line_Counter = crtc.Scan_Line_Counter;
                state.Scan_Line_Counter_Reset = crtc.Scan_Line_Counter_Reset;

                state.Character_Row_Counter = crtc.Character_Row_Counter;
                state.Character_Row_Counter_Reset = crtc.Character_Row_Counter_Reset;

                state.Horizontal_Sync_Width_Counter = crtc.Horizontal_Sync_Width_Counter;
                state.Vertical_Sync_Width_Counter = crtc.Vertical_Sync_Width_Counter;

                state.HSYNC = crtc.HSYNC;
                state.VSYNC = crtc.VSYNC;

                state.Vertical_Total_Adjust_Active = crtc.Vertical_Total_Adjust_Active;
                state.Vertical_Total_Adjust_Counter = crtc.Vertical_Total_Adjust_Counter;

                state.Memory_Address = crtc.Memory_Address;
                state.Memory_Address_of_next_Character_Row = crtc.Memory_Address_of_next_Character_Row;
                state.Memory_Address_of_this_Character_Row = crtc.Memory_Address_of_this_Character_Row;

                state.Horizontal_Display_Enabled = crtc.Horizontal_Display_Enabled;
                state.Vertical_Display_Enabled = crtc.Vertical_Display_Enabled;
                state.Display_Enabled = crtc.Display_Enabled;
                state.Display_Delayed_Enabled = crtc.Display_Delayed_Enabled;

                state.Cursor_Delayed_Status = crtc.Cursor_Delayed_Status;

                state.Cursor_Flash_Count = crtc.Cursor_Flash_Count;

                state.Delay_Flags = crtc.Delay_Flags;
                state.Cursor_Start_Delay = crtc.Cursor_Start_Delay;
                state.Display_Enabled_Delay = crtc.Display_Enabled_Delay;
                state.Display_Disable_Delay = crtc.Display_Disable_Delay;

                state.Vertical_Adjust_Done = crtc.Vertical_Adjust_Done;
                
	}
	
	public static void crtc6845_set_state(int offset, crtc6845_state state)
	{
		//memcpy(&crtc, state, sizeof(crtc6845_state));
                crtc.address_register = state.address_register;
                crtc.registers = state.registers;
                crtc.vertical_sync_width = state.vertical_sync_width;
                crtc.horizontal_sync_width = state.horizontal_sync_width;

                crtc.screen_start_address = state.screen_start_address;
                crtc.cursor_address = state.cursor_address;
                crtc.light_pen_address = state.light_pen_address;

                crtc.scan_lines_increment = state.scan_lines_increment;

                crtc.Horizontal_Counter = state.Horizontal_Counter;
                crtc.Horizontal_Counter_Reset = state.Horizontal_Counter_Reset;

                crtc.Scan_Line_Counter = state.Scan_Line_Counter;
                crtc.Scan_Line_Counter_Reset = state.Scan_Line_Counter_Reset;

                crtc.Character_Row_Counter = state.Character_Row_Counter;
                crtc.Character_Row_Counter_Reset = state.Character_Row_Counter_Reset;

                crtc.Horizontal_Sync_Width_Counter = state.Horizontal_Sync_Width_Counter;
                crtc.Vertical_Sync_Width_Counter = state.Vertical_Sync_Width_Counter;

                crtc.HSYNC = state.HSYNC;
                crtc.VSYNC = state.VSYNC;

                crtc.Vertical_Total_Adjust_Active = state.Vertical_Total_Adjust_Active;
                crtc.Vertical_Total_Adjust_Counter = state.Vertical_Total_Adjust_Counter;

                crtc.Memory_Address = state.Memory_Address;
                crtc.Memory_Address_of_next_Character_Row = state.Memory_Address_of_next_Character_Row;
                crtc.Memory_Address_of_this_Character_Row = state.Memory_Address_of_this_Character_Row;

                crtc.Horizontal_Display_Enabled = state.Horizontal_Display_Enabled;
                crtc.Vertical_Display_Enabled = state.Vertical_Display_Enabled;
                crtc.Display_Enabled = state.Display_Enabled;
                crtc.Display_Delayed_Enabled = state.Display_Delayed_Enabled;

                crtc.Cursor_Delayed_Status = state.Cursor_Delayed_Status;

                crtc.Cursor_Flash_Count = state.Cursor_Flash_Count;

                crtc.Delay_Flags = state.Delay_Flags;
                crtc.Cursor_Start_Delay = state.Cursor_Start_Delay;
                crtc.Display_Enabled_Delay = state.Display_Enabled_Delay;
                crtc.Display_Disable_Delay = state.Display_Disable_Delay;

                crtc.Vertical_Adjust_Done = state.Vertical_Adjust_Done;
	}
	
	
	public static void crtc6845_register_w(int offset, int data)
	{
		switch (crtc.address_register)
		{
			case 0:
	                        R0_horizontal_total(data & 0x0ff);
	//                        crtc6845_recalc_cycles_to_vsync_end();
	  //                      crtc6845_recalc_cycles_to_vsync_start();
				break;
			case 1:
	                        R1_horizontal_displayed(data & 0x0ff);
				break;
			case 2:
	                        R2_horizontal_sync_position(data & 0x0ff);
				break;
			case 3:
	                {
	                        /* if 0 is programmed, vertical sync width is 16 */
	                        crtc.vertical_sync_width = (data>>4) & 0x0f;
	
	                        if (crtc.vertical_sync_width == 0)
	                           crtc.vertical_sync_width = 16;
	
	                        R3_sync_width(data);
	
	                        crtc.horizontal_sync_width = data & 0x0f;
	
	    //                                            crtc6845_recalc_cycles_to_vsync_end();
					}
	                break;
	
	        case 4:
	                        R4_vertical_total(data&0x7f);
	          //              crtc6845_recalc_cycles_to_vsync_start();
				break;
			case 5:
				R5_vertical_total_adjust(data&0x1f);
	
	
	        //                crtc6845_recalc_cycles_to_vsync_start();
	
				break;
			case 6:
				R6_vertical_displayed(data&0x7f);
				break;
			case 7:
				R7_vertical_sync_position(data&0x7f);
	      //                  crtc6845_recalc_cycles_to_vsync_start();
				break;
			case 8:
				R8_interlace_display_enabled(data&0xf3);
				crtc.scan_lines_increment=((R8_interlace_display_enabled()&0x03)==3)?2:1;
				break;
			case 9:
				R9_scan_lines_per_character(data&0x1f);
	            //            crtc6845_recalc_cycles_to_vsync_start();
				break;
			case 10:
				R10_cursor_start(data&0x7f);
				break;
			case 11:
				R11_cursor_end(data&0x1f);
				break;
			case 12:
				R12_screen_start_address_H(data&0x3f);
				crtc.screen_start_address=(R12_screen_start_address_H()<<8)+R13_screen_start_address_L();
				break;
			case 13:
				R13_screen_start_address_L(data);
				crtc.screen_start_address=(R12_screen_start_address_H()<<8)+R13_screen_start_address_L();
				break;
			case 14:
				R14_cursor_address_H(data&0x3f);
				crtc.cursor_address=(R14_cursor_address_H()<<8)+R15_cursor_address_L();
				break;
			case 15:
				R15_cursor_address_L(data);
				crtc.cursor_address=(R14_cursor_address_H()<<8)+R15_cursor_address_L();
				break;
			case 16:
				/* light pen H  (read only) */
				break;
			case 17:
				/* light pen L  (read only) */
				break;
			default:
				break;
		}
	}
	
	
	public static int crtc6845_register_r(int offset)
	{
		int retval=0;
	
		switch (crtc.address_register)
		{
			case 14:
				retval=R14_cursor_address_H();
				break;
			case 15:
				retval=R15_cursor_address_L();
				break;
			case 16:
				retval=R16_light_pen_address_H();
				break;
			case 17:
				retval=R17_light_pen_address_L();
				break;
			default:
				break;
		}
		return retval;
	}
	
	
	public static void crtc6845_reset(int which)
	{
		memset(crtc.registers, 0, 32);
		crtc.address_register = 0;
		crtc.scan_lines_increment = 1;
		crtc.Horizontal_Counter = 0;
		crtc.Horizontal_Counter_Reset = True;
		crtc.Scan_Line_Counter = 0;
		crtc.Scan_Line_Counter_Reset = True;
		crtc.Character_Row_Counter = 0;
		crtc.Character_Row_Counter_Reset = True;
		crtc.Horizontal_Sync_Width_Counter=0;
		crtc.Vertical_Sync_Width_Counter=0;
		crtc.HSYNC=False;
		crtc.VSYNC=False;
		crtc.vertical_sync_width = 0;
		crtc.horizontal_sync_width = 0;
		crtc.Memory_Address=0;
		crtc.Memory_Address_of_next_Character_Row=0;
		crtc.Memory_Address_of_this_Character_Row=0;
		crtc.Horizontal_Display_Enabled=False;
		crtc.Vertical_Display_Enabled=False;
		crtc.Display_Enabled=False;
		crtc.Display_Delayed_Enabled=False;
		crtc.Cursor_Delayed_Status=False;
		crtc.Cursor_Flash_Count=0;
		crtc.Delay_Flags=0;
		crtc.Cursor_Start_Delay=0;
		crtc.Display_Enabled_Delay=0;
		crtc.Display_Disable_Delay=0;
		crtc.cursor_address =0 ;
		crtc.Vertical_Total_Adjust_Active = False;
		crtc.Vertical_Total_Adjust_Counter = 0;
		crtc.Vertical_Adjust_Done = False;
	}
	
	/* called when the internal horizontal display enabled or the
	vertical display enabled changed to set up the real
	display enabled output (which may be delayed 0,1 or 2 characters */
	public static void check_display_enabled()
	{
		int Next_Display_Enabled;
	
	
		Next_Display_Enabled=crtc.Horizontal_Display_Enabled&crtc.Vertical_Display_Enabled;
		if ((Next_Display_Enabled != 0) && (crtc.Display_Enabled==0))
		{
			crtc.Display_Enabled_Delay=(R8_interlace_display_enabled()>>4)&0x03;
			if (crtc.Display_Enabled_Delay<3)
			{
				crtc.Delay_Flags=crtc.Delay_Flags | Display_Enabled_Delay_Flag;
			}
		}
		if ((Next_Display_Enabled==0) && (crtc.Display_Enabled!=0))
		{
			crtc.Display_Disable_Delay=(R8_interlace_display_enabled()>>4)&0x03;
			crtc.Delay_Flags=crtc.Delay_Flags | Display_Disable_Delay_Flag;
		}
		crtc.Display_Enabled=Next_Display_Enabled;
	}
	
	public static void crtc6845_restart_frame()
	{
						/* no restart frame */
						/* End of All Vertical Character rows */
						crtc.Scan_Line_Counter = 0;
						crtc.Character_Row_Counter=0;
						crtc.Vertical_Display_Enabled=True;
						check_display_enabled();
	
										/* KT - As it stands it emulates the UM6845R well */
						crtc.Memory_Address=(crtc.Memory_Address_of_this_Character_Row=crtc.screen_start_address);
										/* HD6845S/MC6845 */
						crtc.Memory_Address_of_next_Character_Row = crtc.Memory_Address;
	}
	
	public static void crtc6845_frameclock()
	{
		crtc.Cursor_Flash_Count=(crtc.Cursor_Flash_Count+1)%50;
	}
	
	/* clock the 6845 */
	public static void crtc6845_clock()
	{
		/* KT - I think the compiler might generate shit code when using "%" operator! */
		/*crtc.Memory_Address=(crtc.Memory_Address+1)%0x4000;*/
		crtc.Memory_Address=(crtc.Memory_Address+1)&0x03fff;
	
		/*crtc.Horizontal_Counter=(crtc.Horizontal_Counter+1)%256;*/
		crtc.Horizontal_Counter=(crtc.Horizontal_Counter+1)&0x0ff;
	
		if (crtc.Horizontal_Counter_Reset != 0)
		{
			/* End of a Horizontal scan line */
			crtc.Horizontal_Counter=0;
			crtc.Horizontal_Counter_Reset=False;
			crtc.Horizontal_Display_Enabled=True;
			check_display_enabled();
	
			crtc.Memory_Address=crtc.Memory_Address_of_this_Character_Row;
	
			/* Vertical clock pulse (R0 CO out) */
			/*crtc.Scan_Line_Counter=(crtc.Scan_Line_Counter+crtc.scan_lines_increment)%32;*/
			crtc.Scan_Line_Counter=(crtc.Scan_Line_Counter+crtc.scan_lines_increment)&0x01f;
	
		if (crtc.Vertical_Total_Adjust_Active != 0)
			{
				/* update counter */
				crtc.Vertical_Total_Adjust_Counter = (crtc.Vertical_Total_Adjust_Counter+1) & 0x01f;
			}
	
	
	                /* Vertical Sync Clock Pulse (In Vertical control) */
	                if (crtc.VSYNC != 0)
	                {
	                        crtc.Vertical_Sync_Width_Counter=(crtc.Vertical_Sync_Width_Counter+1);	// & 0x0f;
	                }
	
			if (crtc.Scan_Line_Counter_Reset != 0)
			{
				/* End of a Vertical Character row */
				crtc.Scan_Line_Counter=0;
				crtc.Scan_Line_Counter_Reset=False;
				crtc.Memory_Address=(crtc.Memory_Address_of_this_Character_Row=crtc.Memory_Address_of_next_Character_Row);
	
				/* Character row clock pulse (R9 CO out) */
	/*			crtc.Character_Row_Counter=(crtc.Character_Row_Counter+1)%128;*/
				crtc.Character_Row_Counter=(crtc.Character_Row_Counter+1)&0x07f;
				if (crtc.Character_Row_Counter_Reset != 0)
				{
					crtc.Character_Row_Counter_Reset=False;
	
					/* if vertical adjust is set, the first time it will do the vertical, adjust, the
					next time, it will not do it and complete the frame */
	
					/* vertical adjust set? */
					if (R5_vertical_total_adjust()!=0)
					{
						/* it's active */
						//crtc.Vertical_Adjust_Done = TRUE;
	
						crtc.Vertical_Total_Adjust_Active = True;
						crtc.Vertical_Total_Adjust_Counter = 0;
					}
					else
					{
						crtc6845_restart_frame();
					}
	
		        }
	
				/* Check for end of All Vertical Character rows */
				if (crtc.Character_Row_Counter==R4_vertical_total())
				{
					if ((crtc.Vertical_Total_Adjust_Active)==0)
					{
						crtc.Character_Row_Counter_Reset=True;
					}
				}
	
				/* Check for end of Displayed Vertical Character rows */
				if (crtc.Character_Row_Counter==R6_vertical_displayed())
				{
					crtc.Vertical_Display_Enabled=False;
					check_display_enabled();
				}
	
	
				/* Check for start of Vertical Sync Pulse */
				if (crtc.Character_Row_Counter==R7_vertical_sync_position())
				{
					crtc.VSYNC=True;
					if (crct6845_calls.out_VS_func != null) 
                                            crct6845_calls.out_VS_func.handler(0,crtc.VSYNC); /* call VS update */
				}
	
	
			}
	
	                /* KT - Moved here because VSYNC length is in scanlines */
	                if (crtc.VSYNC != 0)
	                {
	                        /* Check for end of Vertical Sync Pulse */
	                        if (crtc.Vertical_Sync_Width_Counter==crtc.vertical_sync_width)
	                        {
	                                crtc.Vertical_Sync_Width_Counter=0;
	                                crtc.VSYNC=False;
	                                if (crct6845_calls.out_VS_func != null)
                                            crct6845_calls.out_VS_func.handler(0,crtc.VSYNC); /* call VS update */
	                        }
	                }
	
	
			/* vertical total adjust active? */
			if (crtc.Vertical_Total_Adjust_Active != 0)
			{
				/* equals r5? */
				if (crtc.Vertical_Total_Adjust_Counter==R5_vertical_total_adjust())
				{
					/* not active, clear counter and restart frame */
					crtc.Vertical_Total_Adjust_Active = False;
					crtc.Vertical_Total_Adjust_Counter = 0;
		//			/* cause a scan-line counter reset, and a character row counter reset.
		//			i.e. restart frame */
		//			crtc.Scan_Line_Counter_Reset = TRUE;
		//			crtc.Character_Row_Counter_Reset = TRUE;
	
					// KT this caused problems when R7 == 0 and R5 was set!
					crtc6845_restart_frame();
	
					/* Check for start of Vertical Sync Pulse */
					if (crtc.Character_Row_Counter==R7_vertical_sync_position())
					{
						crtc.VSYNC=True;
						if (crct6845_calls.out_VS_func != null)
                                                    crct6845_calls.out_VS_func.handler(0,crtc.VSYNC); /* call VS update */
					}
				}
			}
	
	
			/* Check for end of Vertical Character Row */
			if (crtc.Scan_Line_Counter==R9_scan_lines_per_character())
			{
				crtc.Scan_Line_Counter_Reset=True;
			}
			if (crct6845_calls.out_RA_func != null)
                            crct6845_calls.out_RA_func.handler(0,crtc.Scan_Line_Counter); /* call RA update */
		}
		/* end of vertical clock pulse */
	
		/* Check for end of Horizontal Scan line */
		if (crtc.Horizontal_Counter==R0_horizontal_total())
		{
			crtc.Horizontal_Counter_Reset=True;
		}
	
		/* Check for end of Display Horizontal Scan line */
		if (crtc.Horizontal_Counter==R1_horizontal_displayed())
		{
			crtc.Memory_Address_of_next_Character_Row=crtc.Memory_Address;
			crtc.Horizontal_Display_Enabled=False;
			check_display_enabled();
		}
	
		/* Horizontal Sync Clock Pulse (Clk) */
		if (crtc.HSYNC != 0)
		{
			crtc.Horizontal_Sync_Width_Counter=(crtc.Horizontal_Sync_Width_Counter+1) & 0x0f;
		}
	
		/* Check for start of Horizontal Sync Pulse */
		if (crtc.Horizontal_Counter==R2_horizontal_sync_position())
		{
	                /* KT - If horizontal sync width is 0, on UM6845R/HD6845S
	                no hsync is generated */
	                if (crtc.horizontal_sync_width!=0)
	                {
	                        crtc.HSYNC=True;
	                        if (crct6845_calls.out_HS_func != null)
                                    crct6845_calls.out_HS_func.handler(0,crtc.HSYNC); /* call HS update */
	                }
	        }
	
	        if (crtc.HSYNC != 0)
	        {
	                /* Check for end of Horizontal Sync Pulse */
	                if (crtc.Horizontal_Sync_Width_Counter==crtc.horizontal_sync_width)
	                {
	
	                        crtc.Horizontal_Sync_Width_Counter=0;
	                        crtc.HSYNC=False;
	                        if (crct6845_calls.out_HS_func != null)
                                    crct6845_calls.out_HS_func.handler(0,crtc.HSYNC); /* call HS update */
	                }
	        }
		if (crct6845_calls.out_MA_func != null)
                    crct6845_calls.out_MA_func.handler(0,crtc.Memory_Address);	/* call MA update */
	
	
	
		/* *** cursor checks still to be done *** */
		if (crtc.Memory_Address==crtc.cursor_address)
		{
			if ((crtc.Scan_Line_Counter>=(R10_cursor_start()&0x1f)) && (crtc.Scan_Line_Counter<=R11_cursor_end()) && (crtc.Display_Enabled != 0))
			{
				crtc.Cursor_Start_Delay=(R8_interlace_display_enabled()>>6)&0x03;
				if (crtc.Cursor_Start_Delay<3) crtc.Delay_Flags=crtc.Delay_Flags | Cursor_Start_Delay_Flag;
			}
		}
	
	
	    /* all the cursor and delay flags are stored in one byte so that we can very quickly (for speed) check if anything
	       needs doing with them, if any are on then we need to do more longer test to find which ones */
		if (crtc.Delay_Flags != 0)
		{
	        /* if the cursor is on, then turn it off on the next clock */
			if ((crtc.Delay_Flags & Cursor_On_Flag) != 0)
			{
				crtc.Delay_Flags=crtc.Delay_Flags^Cursor_On_Flag;
				crtc.Cursor_Delayed_Status=False;
				if (crct6845_calls.out_CR_func != null)
                                    crct6845_calls.out_CR_func.handler(0,crtc.Cursor_Delayed_Status); /* call CR update */
				if (crct6845_calls.out_CRE_func != null)
                                    crct6845_calls.out_CRE_func.handler(0,0); /* call CRE update */
			}
	
			/* cursor enabled delay */
			if ((crtc.Delay_Flags & Cursor_Start_Delay_Flag) != 0)
			{
				crtc.Cursor_Start_Delay-=1;
				if (crtc.Cursor_Start_Delay<0)
				{
					if ((R10_cursor_start()&0x60)!=0x20)
					{
						crtc.Delay_Flags=(crtc.Delay_Flags^Cursor_Start_Delay_Flag)|Cursor_On_Flag;
						crtc.Cursor_Delayed_Status=True;
						if ((crct6845_calls.out_CR_func != null)&&(crtc.Cursor_Flash_Count>25))
                                                    crct6845_calls.out_CR_func.handler(0,crtc.Cursor_Delayed_Status); /* call CR update */
						if (crct6845_calls.out_CRE_func != null)
                                                    crct6845_calls.out_CRE_func.handler(0,2|((crtc.Cursor_Flash_Count>25?1:0)&1)); /* call CR update */
					}
				}
			}
	
	    	/* display enabled delay */
			if ((crtc.Delay_Flags & Display_Enabled_Delay_Flag) != 0)
			{
				crtc.Display_Enabled_Delay-=1;
				if (crtc.Display_Enabled_Delay<0)
				{
					crtc.Delay_Flags=crtc.Delay_Flags^Display_Enabled_Delay_Flag;
					crtc.Display_Delayed_Enabled=True;
					if (crct6845_calls.out_DE_func != null)
                                            crct6845_calls.out_DE_func.handler(0,crtc.Display_Delayed_Enabled); /* call DE update */
				}
			}
	
			/* display disable delay */
			if ((crtc.Delay_Flags & Display_Disable_Delay_Flag) != 0)
			{
				crtc.Display_Disable_Delay-=1;
				if (crtc.Display_Disable_Delay<0)
				{
					crtc.Delay_Flags=crtc.Delay_Flags^Display_Disable_Delay_Flag;
					crtc.Display_Delayed_Enabled=False;
					if (crct6845_calls.out_DE_func != null)
                                            crct6845_calls.out_DE_func.handler(0,crtc.Display_Delayed_Enabled); /* call DE update */
				}
			}
		}
	
	}
	
	/* functions to read the 6845 outputs */
	
	public static int crtc6845_memory_address_r(int offset)  { return crtc.Memory_Address; }    /* MA = Memory Address output */
	public static int crtc6845_row_address_r(int offset)     { return crtc.Scan_Line_Counter; } /* RA = Row Address output */
	public static int crtc6845_horizontal_sync_r(int offset) { return crtc.HSYNC; }             /* HS = Horizontal Sync */
	public static int crtc6845_vertical_sync_r(int offset)   { return crtc.VSYNC; }             /* VS = Vertical Sync */
	public static int crtc6845_display_enabled_r(int offset) { return crtc.Display_Delayed_Enabled; }   /* DE = Display Enabled */
	public static int crtc6845_cursor_enabled_r(int offset)  { return crtc.Cursor_Delayed_Status; }             /* CR = Cursor Enabled */
	
	/*TODO*///#if 0
	
	/* KT:
	
	  The following bit of code uses timers to set/reset the vsync output of the 6845.
	  If a function has been setup in the interface it will be executed with the new vsync
	  value.
	
	  */
	
	/* calculate the number of cycles to the next vsync */
	/* ignores Reg 5! - to be completed */
	/*TODO*///int     crtc6845_cycles_to_vsync(void)
	/*TODO*///{
	/*TODO*///	/* passed vertical sync position */
	/*TODO*///	int cycles_per_frame;
	/*TODO*///
	/*TODO*///	int cycles_into_frame;
	/*TODO*///	int scans_per_character = R9_scan_lines_per_character+1;
	/*TODO*///	int chars_per_line = R0_horizontal_total+1;
	/*TODO*///    int cycles_to_vsync_start;
	/*TODO*///
	/*TODO*///	/* calculate current position into frame as char cycles */
	/*TODO*///	/* scans into frames */
	/*TODO*///	cycles_into_frame  = crtc.Character_Row_Counter*scans_per_character;
	/*TODO*///	cycles_into_frame += crtc.Scan_Line_Counter;
	/*TODO*///	/* scans into frames as char cycles */
	/*TODO*///	cycles_into_frame *= chars_per_line;
	/*TODO*///	/* total cycles into frame */
	/*TODO*///	cycles_into_frame += crtc.Horizontal_Counter;
	/*TODO*///
	/*TODO*///	/* cycles to vsync start as char cycles */
	/*TODO*///	cycles_to_vsync_start = R7_vertical_sync_position*scans_per_character;
	/*TODO*///	cycles_to_vsync_start *= chars_per_line;
	/*TODO*///
	/*TODO*///
	/*TODO*///    if (cycles_into_frame<cycles_to_vsync_start)
	/*TODO*///    {
	/*TODO*///		/* not gone past vertical sync yet! */
	/*TODO*///		return cycles_to_vsync_start - cycles_into_frame;
	/*TODO*///	}
	/*TODO*///
	/*TODO*///	cycles_per_frame = (R4_vertical_total+1)*scans_per_character*chars_per_line;
	/*TODO*///
	/*TODO*///	return (cycles_per_frame - cycles_into_frame) + cycles_to_vsync_start;
	/*TODO*///}
	
	/*TODO*////* calculate the number of CRTC cycles for VSYNC */
	/*TODO*///int     crtc6845_cycles_to_vsync_end(void)
	/*TODO*///{
	/*TODO*///        /* if we are in vsync */
	/*TODO*///        if (crtc.VSYNC)
	/*TODO*///        {
	/*TODO*///           return (R0_horizontal_total+1)*(crtc.vertical_sync_width-crtc.Vertical_Sync_Width_Counter);
	/*TODO*///        }
	/*TODO*///
	/*TODO*///        return crtc6845_cycles_to_vsync() + ((R0_horizontal_total+1)*crtc.vertical_sync_width);
	/*TODO*///}
	/*TODO*///
	/*TODO*///
	/*TODO*////* number of crtc cycles for whole frame */
	/*TODO*///static int crtc6845_cycles_per_frame(void)
	/*TODO*///{
	/*TODO*///	int scan_lines_per_character = R9_scan_lines_per_character+1;
	/*TODO*///	int chars_per_line = R0_horizontal_total+1;
	/*TODO*///
	/*TODO*///	return /* time for all rows */
	/*TODO*///			((R4_vertical_total+1)*chars_per_line*scan_lines_per_character)
	/*TODO*///			/* add time for scanlines in vertical adjust */
	/*TODO*///			+(R5_vertical_total_adjust*chars_per_line);
	/*TODO*///}
	/*TODO*///
	/*TODO*////* number of crtc cycles for vsync */
	/*TODO*///static int crtc6845_vsync_length_in_cycles(void)
	/*TODO*///{
	/*TODO*///	int length;
	/*TODO*///
	/*TODO*///	/* if length is programmed as 0, actual length for hd6845s is 16 */
	/*TODO*///	length = crtc.vertical_sync_width;
	/*TODO*///
	/*TODO*///	if (length==0)
	/*TODO*///	{
	/*TODO*///		length = 16;
	/*TODO*///	}
	/*TODO*///
	/*TODO*///	/* cycles for vsync */
	/*TODO*///	return (R0_horizontal_total+1)*length;
	/*TODO*///}
	/*TODO*///
	/*TODO*///
	/*TODO*////* remove "vsync set" timer */
	/*TODO*///static void crtc6845_remove_vsync_set_timer(void)
	/*TODO*///{
	/*TODO*///	if (crtc6845_vsync_set_timer!=NULL)
	/*TODO*///	{
	/*TODO*///		timer_remove(crtc6845_vsync_set_timer);
	/*TODO*///		crtc6845_vsync_set_timer = NULL;
	/*TODO*///	}
	/*TODO*///}
	/*TODO*///
	/*TODO*////* remove "vsync clear" timer */
	/*TODO*///static void crtc6845_remove_vsync_clear_timer(void)
	/*TODO*///{
	/*TODO*///	if (crtc6845_vsync_clear_timer!=NULL)
	/*TODO*///	{
	/*TODO*///		timer_remove(crtc6845_vsync_clear_timer);
	/*TODO*///		crtc6845_vsync_clear_timer = NULL;
	/*TODO*///	}
	/*TODO*///}
	/*TODO*///
	/*TODO*////* setup new time for "vsync set" */
	/*TODO*///static void crtc6845_set_new_vsync_set_time(int cycles)
	/*TODO*///{
	/*TODO*///	int crtc_cycles_to_vsync_start;
	/*TODO*///
	/*TODO*///	/* get cycles to vsync start, or if vsync cannot be reached
	/*TODO*///	cycles will be -1 */
	/*TODO*///	crtc_cycles_to_vsync_start = cycles;
	/*TODO*///
	/*TODO*///	crtc6845_remove_vsync_set_timer();
	/*TODO*///
	/*TODO*///        if (crtc_cycles_to_vsync_start!=-1)
	/*TODO*///	{
	/*TODO*///                crtc6845_vsync_set_timer = timer_set(TIME_IN_USEC(crtc_cycles_to_vsync_start), 0, crtc6845_vsync_set_timer_callback);
	/*TODO*///	}
	/*TODO*///}
	/*TODO*///
	/*TODO*////* setup new time for "vsync clear" */
	/*TODO*///static void crtc6845_set_new_vsync_clear_time(int cycles)
	/*TODO*///{
	/*TODO*///	int crtc_cycles_to_vsync_end;
	/*TODO*///
	/*TODO*///	crtc6845_remove_vsync_clear_timer();
	/*TODO*///
	/*TODO*///	/* get number of cycles to end of vsync */
	/*TODO*///	crtc_cycles_to_vsync_end = cycles;
	/*TODO*///
	/*TODO*///	if (crtc_cycles_to_vsync_end!=-1)
	/*TODO*///	{
	/*TODO*///                crtc6845_vsync_clear_timer = timer_set(TIME_IN_USEC(crtc_cycles_to_vsync_end), 0, crtc6845_vsync_clear_timer_callback);
	/*TODO*///	}
	/*TODO*///}
	/*TODO*///
	/*TODO*////* for these two below, might be better to record the current cpu time, and use that
	/*TODO*///to recalculate where the start/end of the vsync will next occur! */
	/*TODO*///
	/*TODO*///static void crtc6845_vsync_clear_timer_callback(int dummy)
	/*TODO*///{
	/*TODO*///	/* clear vsync */
	/*TODO*///	crtc.VSYNC = 0;
	/*TODO*///
	/*TODO*///	/* call function to let emulation "know" */
	/*TODO*///	if (crct6845_calls.out_VS_func) (crct6845_calls.out_VS_func)(0,crtc.VSYNC); /* call VS update */
	/*TODO*///
	/*TODO*///
	/*TODO*///	/* if we got to here the vsync has just ended */
	/*TODO*///	/* the next vsync will occur in cycles per frame - vsync length in cycles */
	/*TODO*///	/* this will work as long as the vsync length has not been reprogrammed while the vsync was active! */
	/*TODO*///	/* setup time for vsync set timer */
	/*TODO*///	crtc6845_set_new_vsync_set_time(crtc6845_cycles_per_frame()-crtc6845_vsync_length_in_cycles());
	/*TODO*///
	/*TODO*///	/* prevent timer from being free'd and don't let it trigger again */
	/*TODO*///	timer_reset(crtc6845_vsync_clear_timer, TIME_NEVER);
	/*TODO*///}
	/*TODO*///
	/*TODO*////* called when vsync is set */
	/*TODO*///static void crtc6845_vsync_set_timer_callback(int dummy)
	/*TODO*///{
	/*TODO*///	/* set vsync */
	/*TODO*///	crtc.VSYNC = 1;
	/*TODO*///
	/*TODO*///	/* call function to let emulation "know" */
	/*TODO*///	if (crct6845_calls.out_VS_func) (crct6845_calls.out_VS_func)(0,crtc.VSYNC); /* call VS update */
	/*TODO*///
	/*TODO*///
	/*TODO*///	/* if we got to here the vsync has just been set, and has just started */
	/*TODO*///	/* the next timer will be in vsync length cycles unless it is reprogrammed as the VSYNC
	/*TODO*///	is active, in this case, the new vsync end will be re-calculated */
	/*TODO*///
	/*TODO*///	/* setup time for vsync clear timer */
	/*TODO*///    crtc6845_set_new_vsync_clear_time(crtc6845_vsync_length_in_cycles());
	/*TODO*///
	/*TODO*///	/* prevent timer from being free'd and don't let it trigger again */
	/*TODO*///	timer_reset(crtc6845_vsync_set_timer, TIME_NEVER);
	/*TODO*///}
	/*TODO*///static void crtc6845_recalc_cycles_to_vsync_end(void)
	/*TODO*///{
	/*TODO*///	int cycles_to_vsync_end = crtc6845_cycles_to_vsync_end();
	/*TODO*///
	/*TODO*///	/* if we're in vsync, the end is important, otherwise we are waiting for the next vsync
	/*TODO*///	to start. The start is not affected by the length of the vsync */
	/*TODO*///	if (crtc.VSYNC)
	/*TODO*///	{
	/*TODO*///		crtc6845_set_new_vsync_clear_time(cycles_to_vsync_end);
	/*TODO*///	}
	/*TODO*///}
	/*TODO*///
	/*TODO*///static void crtc6845_recalc_cycles_to_vsync_start(void)
	/*TODO*///{
	/*TODO*///	int cycles_to_vsync_start = crtc6845_cycles_to_vsync();
	/*TODO*///
	/*TODO*///	/* if we're not in vsync, the end is important, otherwise we are waiting for the end
	/*TODO*///	of the vsync */
	/*TODO*///	if (!crtc.VSYNC)
	/*TODO*///	{
	/*TODO*///		crtc6845_set_new_vsync_set_time(cycles_to_vsync_start);
	/*TODO*///	}
	/*TODO*///}
	/*TODO*///
	/*TODO*////* ignores r5 ! to be completed */
	/*TODO*///void crtc6845_recalc(int offset,int num_cycles)
	/*TODO*///{
	/*TODO*///	int cycles;
	/*TODO*///	int num_scan_lines;
	/*TODO*///	int scans_into_frame;
	/*TODO*///	int scan_lines_per_char = (R9_scan_lines_per_character+1);
	/*TODO*///	int scan_lines_per_frame = (R4_vertical_total+1)*scan_lines_per_char;
	/*TODO*///	cycles = num_cycles + crtc.Horizontal_Counter;
	/*TODO*///
	/*TODO*///	/* calculate number of scan-lines */
	/*TODO*///	num_scan_lines = cycles/(R0_horizontal_total+1);
	/*TODO*///	num_scan_lines+=crtc.Scan_Line_Counter;
	/*TODO*///	/* set new horizontal counter */
	/*TODO*///	crtc.Horizontal_Counter = cycles % (R0_horizontal_total+1);
	/*TODO*///
	/*TODO*///	scans_into_frame = crtc.Character_Row_Counter*scan_lines_per_char;
	/*TODO*///
	/*TODO*///	while ((scans_into_frame+num_scan_lines)>=scan_lines_per_frame)
	/*TODO*///	{
	/*TODO*///		int scans_to_end_of_frame;
	/*TODO*///
	/*TODO*///		scans_to_end_of_frame = scan_lines_per_frame - scans_into_frame;
	/*TODO*///		num_scan_lines -= scans_to_end_of_frame;
	/*TODO*///		scans_into_frame = 0;
	/*TODO*///		/* subtract R5 lines here! */
	/*TODO*///
	/*TODO*///	}
	/*TODO*///
	/*TODO*///	/* update row position */
	/*TODO*///	crtc.Character_Row_Counter = num_scan_lines/scan_lines_per_char;
	/*TODO*///    /* remainder is the scan line counter */
	/*TODO*///    crtc.Scan_Line_Counter = num_scan_lines % scan_lines_per_char;
	/*TODO*///}
	/*TODO*///
	/*TODO*///#endif    
}
