/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.includes;

import static mess056.deviceH.*;
import static mess056.machine.vc20tape.vc20_tape_attach_image;
import static mess056.machine.vc20tape.vc20_tape_detach_image;
import static mess056.messH.*;
import static mess056.sound.wave.*;

public class vc20tapeH {
    public static IODevice IODEVICE_VC20TAPE = 
      new IODevice(      
        IO_CASSETTE,        /* type */
        1,                  /* count */
        "wav\0",    /* TAP, LNX and T64(maybe) later file extensions */
        IO_RESET_NONE,      /* reset if file changed */
        null,               /* id */
        vc20_tape_attach_image,	/* init */
        vc20_tape_detach_image,	/* exit */
        wave_info,			/* info */						
                        wave_open,			/* open */						
                        null, //wave_close, 		/* close */ 					
                        wave_status,		/* status */					
                        wave_seek,			/* seek */						
                        wave_tell,			/* tell */						
                        wave_input, 		/* input */ 					
                        null,//wave_output,		/* output */					
                        wave_input_chunk,	/* input_chunk */				
                        wave_output_chunk
      );

}
