/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.formats;

import static common.ptr.*;
import static common.libc.cstring.*;

public class fmsx_cas
{
	
        public static int CAS_PERIOD            = (16);
        public static int CAS_HEADER_PERIODS    = (4000);
        public static int CAS_EMPTY_PERIODS     = (1000);
        public static int ALLOCATE_BLOCK        = (1024*8);
        public static char CasHeader[] = { 0x1F,0xA6,0xDE,0xBA,0xCC,0x13,0x7D,0x74 };

/*TODO*///	public static int fmsx_cas_to_wav_size (UBytePtr casdata, int caslen)
/*TODO*///	{
/*TODO*///		int 	pos, size=0;
/*TODO*///	
/*TODO*///		if (caslen < 8) return -1;
/*TODO*///		if (memcmp (casdata, CasHeader, sizeof (CasHeader) ) ) return -1;
/*TODO*///	
/*TODO*///		pos = size = 0;
/*TODO*///		 
/*TODO*///		while (pos < caslen)
/*TODO*///			{
/*TODO*///			if ( (pos + 8) < caslen)
/*TODO*///				if (!memcmp (casdata + pos, CasHeader, 8) ) 
/*TODO*///					{
/*TODO*///					size += (CAS_EMPTY_PERIODS + CAS_HEADER_PERIODS) * CAS_PERIOD;
/*TODO*///					pos += 8;
/*TODO*///					continue;
/*TODO*///					}
/*TODO*///	
/*TODO*///			size += CAS_PERIOD * 12;
/*TODO*///			pos++;
/*TODO*///			}
/*TODO*///	
/*TODO*///		return size;
/*TODO*///		}
	
        public static int fmsx_cas_to_wav (UBytePtr casdata, int caslen, UBytePtr wavdata, int wavlen)
	{
		int cas_pos, samples_size, bit, state = 1, samples_pos, size, n, i, p;
		char[] samples;
                UBytePtr nsamples = null;
                
		if (caslen < 8) return 1;
		if (memcmp (casdata, CasHeader, CasHeader.length ) != 0 ) return 1;
	
		cas_pos = 8;
		samples_size = ALLOCATE_BLOCK * 2;
		samples = new char[caslen * 2 * 1024];
		if (samples == null)
			return 2;
	
		samples_pos = 0;
	
	    while (cas_pos < caslen)
			{
			/* check memory for entire header (silence + header itself) */
			size = (CAS_EMPTY_PERIODS + CAS_HEADER_PERIODS) * CAS_PERIOD;
                        
                        System.out.println("size="+size);
                        System.out.println("samples_size="+samples_size);
                        System.out.println("samples_pos="+samples_pos);
                        
			if ( (samples_pos + size) >= samples_size)
				{
				samples_size += size;
				nsamples = new UBytePtr(samples, samples_size * 2);
				if (nsamples == null)
					{
					samples = null;
					return 2;
					}
				else samples = nsamples.memory;
				}
	
			/* write CAS_EMPTY_PERIODS of silence */
			memset (new UBytePtr(samples, samples_pos), 0, CAS_EMPTY_PERIODS * CAS_PERIOD * 2);
			samples_pos += CAS_EMPTY_PERIODS * CAS_PERIOD;
	
			/* write CAS_HEADER_PERIODS of header (high frequency) */
			for (i=0;i<CAS_HEADER_PERIODS*4;i++)
				{
				for (n=0;n<CAS_PERIOD / 4;n++)
					samples[samples_pos + n] = (char) (state!=0 ? 32767 : -32767);
	
				samples_pos += CAS_PERIOD / 4 ;
				state = state!=0?0:1;
			}
	
			while (cas_pos < caslen)
			{
				/* check if we've hit a new header (or end of block) */
				if ( (cas_pos + 8) < caslen)
				{
                                        if (memcmp (new UBytePtr(casdata, cas_pos), CasHeader, 8) == 0)
					{
						cas_pos += 8;
						break; /* falls back to loop above; plays header again */
					}
				}
	
				/* check if we've got enough memory for the next byte */
				size = CAS_PERIOD * 11;
				if ( (samples_pos + size) >= samples_size)
					{
					samples_size += ALLOCATE_BLOCK;
					nsamples = new UBytePtr(samples, samples_size * 2);
					if (nsamples == null)
						{
						samples = null;
						return 2;
						}
					else samples = nsamples.memory;
					}
	
				for (i=0;i<=11;i++)
					{
					if (i == 0) bit = 0;
					else if (i < 9) bit = (casdata.read(cas_pos) & (1 << (i - 1) ) );
					else bit = 1;
                                        
                                        //System.out.println("Bit="+bit);
	
					/* write this one bit */
					for (n=0;n<(bit!=0 ? 4 : 2);n++)
					{
						size = (bit!=0 ? CAS_PERIOD / 4 : CAS_PERIOD / 2);
						for (p=0;p<size;p++)
						{
							samples[samples_pos + p] = (char) (state!=0 ? 32767 : -32767);
						}
						state = state!=0?0:1;
						samples_pos += size;
					}
				}
                                cas_pos++;
                            }
			}
	
		wavdata = new UBytePtr(samples);
		wavlen = samples_pos;
                
                wavdata_loaded = new UBytePtr(samples);
		wavlen_loaded = samples_pos;
	
		return 0;
		}
	
        public static UBytePtr wavdata_loaded;
        public static int wavlen_loaded;
}
