/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
package mame056.sound;

public class saa1099H {
    
    /**********************************************
	Philips SAA1099 Sound driver
    **********************************************/

    public static final int MAX_SAA1099 = 2;

    /* interface */
    public static class SAA1099_interface
    {
            public int numchips;						/* number of chips */
            public int[][] volume = new int[MAX_SAA1099][2];			/* playback volume */
            
            public SAA1099_interface(int numchips, int[][] volume){
                this.numchips = numchips;
                this.volume = volume;
            }
    };
    
}
