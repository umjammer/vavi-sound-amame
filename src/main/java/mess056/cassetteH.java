/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056;

public class cassetteH
{
	
	public static class cassette_args
	{
		/* Args that always apply whenever a cassette is there */
		public int initial_status;
	
		/* Args to use when the wave file is already there */
		/*TODO*///int (*fill_wave)(INT16 *buffer, int length, UINT8 *bytes);
		/*TODO*///void (*calc_chunk_info)(void *file, int *chunk_size, int *chunk_samples);
		public int input_smpfreq;
                public int header_samples;
                public int trailer_samples;
                public int chunk_size;		/* used if calc_chunk_info is NULL */
		public int chunk_samples;	/* used if calc_chunk_info is NULL */
	
		/* Args to use when the wave file is being created */
		public int create_smpfreq;
	};
	
	/*TODO*///int cassette_init(int id, const struct cassette_args *args);
	
}
