
package mame056.sound;

import static arcadeflex056.fucPtr.*;

public class k007232H {
    /*********************************************************/
    /*    Konami PCM controller                              */
    /*********************************************************/
    public static final int MAX_K007232		= 3;


    public static class K007232_interface
    {
            public int num_chips;			/* Number of chips */
            public int[] bank = new int[MAX_K007232];	/* memory regions */
            public int[] volume= new int[MAX_K007232];/* volume */
            public WriteYmHandlerPtr[] portwritehandler = new WriteYmHandlerPtr[MAX_K007232];
            
            public K007232_interface(int num_chips, int[] bank, int[] volume, WriteYmHandlerPtr[] portwritehandler){
                this.num_chips = num_chips;
                this.bank = bank;
                this.volume = volume;
                this.portwritehandler = portwritehandler;
            }
    };

    public static int K007232_VOL(int LVol, int LPan, int RVol, int RPan){
        return ((LVol)|((LPan)<<8)|((RVol)<<16)|((RPan)<<24));
    }

    /*
      The 007232 has two channels and produces two outputs. The volume control
      is external, however to make it easier to use we handle that inside the
      emulation. You can control volume and panning: for each of the two channels
      you can set the volume of the two outputs. If panning is not required,
      then volumeB will be 0 for channel 0, and volumeA will be 0 for channel 1.
      Volume is in the range 0-255.
    */
    
    
}
