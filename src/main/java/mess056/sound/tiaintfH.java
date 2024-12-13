/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
package mess056.sound;

/**
 *
 * @author chusogar
 */
public class tiaintfH {
    public static int TIA_DEFAULT_GAIN = 16;

    public static class TIAinterface {
        public int clock;
        public int volume;
        public int gain;
        public int baseclock;
        
        public TIAinterface(int clock, int volume, int gain){
            this.clock = clock;
            this.volume = volume;
            this.gain = gain;
        }
    };

}


