/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.includes;

public class amstradH
{
	
	/* On the Amstrad, any part of the 64k memory can be access by the video
	hardware (GA and CRTC - the CRTC specifies the memory address to access,
	and the GA fetches 2 bytes of data for each 1us cycle.
	
	The Z80 must also access the same ram.
	
	To maintain the screen display, the Z80 is halted on each memory access.
	
	The result is that timing for opcodes, appears to fall into a nice pattern,
	where the time for each opcode can be measured in NOP cycles. NOP cycles is
	the name I give to the time taken for one NOP command to execute.
	
	This happens to be 1us.
	
	From measurement, there are 64 NOPs per line, with 312 lines per screen.
	This gives a total of 19968 NOPs per frame. */
	
	/* number of us cycles per frame (measured) */
	public static int AMSTRAD_US_PER_FRAME	= 19968;
	public static int AMSTRAD_FPS           = 50;
	
	
	
	/* These are the measured visible screen dimensions in CRTC characters.
	50 CRTC chars in X, 35 CRTC chars in Y (8 lines per char assumed) */
	public static int AMSTRAD_SCREEN_WIDTH          = (50*16);
	public static int AMSTRAD_SCREEN_HEIGHT         = (35*8);
	public static int AMSTRAD_MONITOR_SCREEN_WIDTH	= (64*16);
	public static int AMSTRAD_MONITOR_SCREEN_HEIGHT	= (39*8);
	
	/*TODO*///#if AMSTRAD_VIDEO_USE_EVENT_LIST || 1
	/*TODO*////* codes for eventlist */
	/*TODO*///enum
	/*TODO*///{
	/*TODO*///	/* change pen colour with gate array */
        public static final int EVENT_LIST_CODE_GA_COLOUR = 0;
        /* change mode with gate array */
        public static final int EVENT_LIST_CODE_GA_MODE = 1;
        /* change CRTC register data */
        public static final int EVENT_LIST_CODE_CRTC_WRITE = 2;
        /* change CRTC register selection */
        public static final int EVENT_LIST_CODE_CRTC_INDEX_WRITE = 3;
	/*TODO*///};
	/*TODO*///#endif
	
}
