/**
 * ported to v0.37b7
 *
 */
package mess056.sound;

public class speakerH {

    public static final int MAX_SPEAKER = 2;

    public static class Speaker_interface {

        public Speaker_interface(int num, int[] mixing_level, int[] num_level, short[] levels) {
            this.num = num;
            this.mixing_level = mixing_level;
            this.num_level = num_level;
            this.levels = levels;
        }

        public Speaker_interface(int num, int[] mixing_level) {
            this.num = num;
            this.mixing_level = mixing_level;
        }

        public int num;
        public int[] mixing_level;//[MAX_SPEAKER];	/* mixing level in percent */
        public int[] num_level;//[MAX_SPEAKER]; 	/* optional: number of levels (if not two) */
        public short[] levels;//[MAX_SPEAKER]; 	/* optional: pointer to level lookup table */
    }
}
