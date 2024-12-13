/**
 * ported to v0.37b7
 *
 */
package mess056.sound;

import static common.ptr.*;
import static consoleflex056.funcPtr.*;
import static mess056.deviceH.*;
import static mess056.messH.*;
import static mess056.sound.wave.*;

public class waveH {

    public static final int MAX_WAVE = 4;

    /**
     * ***************************************************************************
     * CassetteWave interface
     * ***************************************************************************
     */
    public static class Wave_interface {

        public Wave_interface(int num, int[] mixing_level) {
            this.num = num;
            this.mixing_level = mixing_level;
        }

        public int num;
        public int[] mixing_level;//[MAX_WAVE];
    };

    /*****************************************************************************
    * functions for the IODevice entry IO_CASSETTE. Example for the macro
    * IO_CASSETTE_WAVE(1,"wav\0cas\0",mycas_id,mycas_init,mycas_exit)
    *****************************************************************************/
    public static IODevice IO_CASSETTE_WAVE(int count, String fileext,io_idPtr id,io_initPtr init, io_exitPtr exit)
    {	
        return new IODevice(
            IO_CASSETTE,		/* type */
            count,				/* count */
            fileext,			/* file extensions */
            IO_RESET_NONE,		/* reset depth */
            null,//id, 				/* id */
            init,				/* init */
            exit,				/* exit */
            wave_info,			/* info */
            wave_open,			/* open */
            null,//wave_close, 		/* close */
            wave_status,		/* status */
            wave_seek,			/* seek */
            wave_tell,			/* tell */
            wave_input, 		/* input */
            null,//wave_output,		/* output */
            wave_input_chunk,	/* input_chunk */
            wave_output_chunk	/* output_chunk */
        );
    }

/*TODO*////*****************************************************************************
/*TODO*/// * Use this structure for the "void *args" argument of device_open()
/*TODO*/// * file
/*TODO*/// *	  file handle returned by osd_fopen() (mandatory)
/*TODO*/// * display
/*TODO*/// *	  display cassette icon, playing time and total time on screen
/*TODO*/// * fill_wave
/*TODO*/// *	  callback to fill in samples (optional)
/*TODO*/// * smpfreq
/*TODO*/// *	  sample frequency when the wave is generated (optional)
/*TODO*/// *	  used for fill_wave() and for writing (creating) wave files
/*TODO*/// * header_samples
/*TODO*/// *	  number of samples for a cassette header (optional)
/*TODO*/// * trailer_samples
/*TODO*/// *	  number of samples for a cassette trailer (optional)
/*TODO*/// * chunk_size
/*TODO*/// *	  number of bytes to convert at once (optional)
/*TODO*/// * chunk_samples
/*TODO*/// *	  number of samples produced for a data chunk (optional)
/*TODO*/// *****************************************************************************/
    public static abstract interface WaveFillerPtr {

        public abstract int handler(UBytePtr buffer, int length, UBytePtr bytes);
    }
    
    public static class wave_args {

        public wave_args(Object file) {
            this.file = file;
        }

        public Object file;
        public int display;
        public WaveFillerPtr fill_wave;
        public int smpfreq;
        public int header_samples;
        public int trailer_samples;
        public int chunk_size;
        public int chunk_samples;
    };
    
/*****************************************************************************
 * Your (optional) fill_wave callback will be called with "UINT8 *bytes" set
 * to one of these values if you should fill in the (optional) header or
 * trailer samples into the buffer.
 * Otherwise 'bytes' is a pointer to the chunk of data
 *****************************************************************************/
    public static final int CODE_HEADER = -1;
    public static final int CODE_TRAILER = -2;

    public static final int WAVE_STATUS_MOTOR_ENABLE = 1;
    public static final int WAVE_STATUS_MUTED = 2;
    public static final int WAVE_STATUS_MOTOR_INHIBIT = 4;

}
