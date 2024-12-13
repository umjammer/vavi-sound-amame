/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056;

import static mame056.osdependH.OSD_FILETYPE_IMAGE_R;
import mess056.cassetteH.cassette_args;
import static mess056.device.device_open;
import static mess056.device.device_status;
import static mess056.deviceH.IO_CASSETTE;
import static mess056.mess.image_fopen;
import static mess056.messH.INIT_FAIL;
import static mess056.messH.INIT_PASS;
import static mess056.messH.OSD_FOPEN_READ;
import static mess056.messH.OSD_FOPEN_RW_CREATE;
import mess056.sound.waveH.wave_args;

public class cassette
{
	
	public static int cassette_init(int id, cassette_args args)
	{
                System.out.println("cassette_init");
		Object file;
		wave_args wa;
	
		/* Try to open existing file */
                System.out.println("trying to load");
		file = image_fopen(IO_CASSETTE, id, OSD_FILETYPE_IMAGE_R, 0);
		if (file != null) {
                    System.out.println("no null");
			//memset(wa, 0, (wa));
                        wa = new wave_args(file);
	
			/*TODO*///if ((args.calc_chunk_info) != null) {
			/*TODO*///	args.calc_chunk_info(file, wa.chunk_size, wa.chunk_samples);
			/*TODO*///}
			/*TODO*///else {
				wa.chunk_size = args.chunk_size;
				wa.chunk_samples = args.chunk_samples;
			/*TODO*///}
	
			wa.file = file;
			wa.smpfreq = args.input_smpfreq;
			/*TODO*///wa.fill_wave = args.fill_wave;
			wa.header_samples = args.header_samples;
			wa.trailer_samples = args.trailer_samples;;
			wa.display = 1;
	
			if ((device_open(IO_CASSETTE, id, 0, wa)) != 0)
				return INIT_FAIL;
	
	        device_status(IO_CASSETTE, id, args.initial_status);
			return INIT_PASS;
		}
                System.out.println("file=null");
		/* No file?  Can I create it? */
		file = image_fopen(IO_CASSETTE, id, OSD_FILETYPE_IMAGE_R, OSD_FOPEN_RW_CREATE);
		if (file != null) {
			//memset(wa, 0, sizeof(wa));
                        wa = new wave_args(file);
	
			wa.file = file;
			wa.display = 1;
			wa.smpfreq = args.create_smpfreq
				;
			if ((device_open(IO_CASSETTE, id, 1, wa)) != 0)
				return INIT_FAIL;
	
	        device_status(IO_CASSETTE, id, args.initial_status);
			return INIT_PASS;
	    }
		return INIT_PASS;
	}
	
}
