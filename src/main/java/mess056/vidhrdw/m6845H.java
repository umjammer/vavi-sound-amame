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

import static arcadeflex056.fucPtr.*;

public class m6845H {
    
    public static class crtc6845_interface
    {
            public WriteHandlerPtr out_MA_func = null;
            public WriteHandlerPtr out_RA_func = null;
            public WriteHandlerPtr out_HS_func = null;
            public WriteHandlerPtr out_VS_func = null;
            public WriteHandlerPtr out_DE_func = null;
            public WriteHandlerPtr out_CR_func = null;
            public WriteHandlerPtr out_CRE_func = null;

        public crtc6845_interface(WriteHandlerPtr out_MA_func, WriteHandlerPtr out_RA_func, WriteHandlerPtr out_HS_func, WriteHandlerPtr out_VS_func, WriteHandlerPtr out_DE_func, WriteHandlerPtr out_CR_func, WriteHandlerPtr out_CRE_func) {
            this.out_MA_func = out_MA_func;
            this.out_RA_func = out_RA_func;
            this.out_HS_func = out_HS_func;
            this.out_VS_func = out_VS_func;
            this.out_DE_func = out_DE_func;
            this.out_CR_func = out_CR_func;
            this.out_CRE_func = out_CRE_func;
        }
    };

    public static class crtc6845_state
    {
            /* Register Select */
            public int address_register;
            /* register data */
            public int[] registers = new int[32];
            /* vertical and horizontal sync widths */
            public int vertical_sync_width, horizontal_sync_width;

            public int screen_start_address;         /* = R12<<8 + R13 */
            public int cursor_address;				  /* = R14<<8 + R15 */
            public int light_pen_address;			  /* = R16<<8 + R17 */

            public int scan_lines_increment;

            public int Horizontal_Counter;
            public int Horizontal_Counter_Reset;

            public int Scan_Line_Counter;
            public int Scan_Line_Counter_Reset;

            public int Character_Row_Counter;
            public int Character_Row_Counter_Reset;

            public int Horizontal_Sync_Width_Counter;
            public int Vertical_Sync_Width_Counter;

            public int HSYNC;
            public int VSYNC;

            public int Vertical_Total_Adjust_Active;
            public int Vertical_Total_Adjust_Counter;

            public int Memory_Address;
            public int Memory_Address_of_next_Character_Row;
            public int Memory_Address_of_this_Character_Row;

            public int Horizontal_Display_Enabled;
            public int Vertical_Display_Enabled;
            public int Display_Enabled;
            public int Display_Delayed_Enabled;

            public int Cursor_Delayed_Status;

            public int Cursor_Flash_Count;

            public int Delay_Flags;
            public int Cursor_Start_Delay;
            public int Display_Enabled_Delay;
            public int Display_Disable_Delay;

            public int	Vertical_Adjust_Done;
            //	int cycles_to_vsync_start;
            //	int cycles_to_vsync_end;
    };

}
