/***************************************************************************

  MOS ted 7360 (and sound interface)
  PeT mess@utanet.at

  main part in vidhrdw
***************************************************************************/
/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.sndhrdw;

import static arcadeflex056.fucPtr.*;
import static common.ptr.*;
import static mame056.mame.*;
import static mame056.sndintrf.*;
import static mame056.sndintrfH.*;
import static mame056.sound.streams.*;
import static mess056.includes.ted7360H.*;
import static mess056.vidhrdw.ted7360.*;

public class ted7360
{
	
	public static int VERBOSE_DBG = 1;
	
	/* noise channel: look into vic6560.c */
	public static int NOISE_BUFFER_SIZE_SEC = 5;
	
	public static int TONE_ON(){ return ((ted7360[0x11]&0x80))!=0?0:1; }		/* or tone update!? */
	public static int TONE1_ON(){ return ((ted7360[0x11]&0x10)); }
	public static int TONE1_VALUE(){ return (ted7360[0xe]|((ted7360[0x12]&3)<<8)); }
	public static int TONE2_ON(){ return ((ted7360[0x11]&0x20)); }
	public static int TONE2_VALUE(){ return (ted7360[0xf]|((ted7360[0x10]&3)<<8)); }
	public static int VOLUME(){ return (ted7360[0x11]&0x0f); }
	public static int NOISE_ON(){ return (ted7360[0x11]&0x40); }
	
	/*
	 * pal 111860.781
	 * ntsc 111840.45
	 */
	public static int TONE_FREQUENCY(int reg){ return ((TED7360_CLOCK()>>3)/(1024-reg)); }
	public static int TONE_FREQUENCY_MIN(){ return (TONE_FREQUENCY(0)); }
	public static int NOISE_FREQUENCY(){ return (TED7360_CLOCK()/8/(1024-TONE2_VALUE())); }
	public static int NOISE_FREQUENCY_MAX(){ return (TED7360_CLOCK()/8); }
	
	static int channel, noisesize,	/* number of samples */
		tone1pos = 0, tone2pos = 0,		   /* pos of tone */
		tone1samples = 1, tone2samples = 1,   /* count of samples to give out per tone */
		noisepos = 0, noisesamples = 1;
	
	static UBytePtr noise;
	
	public static void ted7360_soundport_w (int offset, int data)
	{
		stream_update(channel,0);
		/*    int old=ted7360[offset]; */
		switch (offset)
		{
		case 0xe:
		case 0x12:
			if (offset == 0x12)
				ted7360[offset] = (ted7360[offset] & ~3) | (data & 3);
			else
				ted7360[offset] = data;
			tone1pos = 0;
			tone1samples = options.samplerate / TONE_FREQUENCY (TONE1_VALUE());
			/*TODO*///DBG_LOG (1, "ted7360", ("tone1 %d %d sample:%d\n",
			/*TODO*///			TONE1_VALUE, TONE_FREQUENCY(TONE1_VALUE), tone1samples));
	
			break;
		case 0xf:
		case 0x10:
			ted7360[offset] = data;
			tone2pos = 0;
			tone2samples = options.samplerate / TONE_FREQUENCY (TONE2_VALUE());
			/*TODO*///DBG_LOG (1, "ted7360", ("tone2 %d %d sample:%d\n",
			/*TODO*///			TONE2_VALUE, TONE_FREQUENCY(TONE2_VALUE), tone2samples));
	
			noisesamples = (int) ((double) NOISE_FREQUENCY_MAX() * options.samplerate
								  * NOISE_BUFFER_SIZE_SEC / NOISE_FREQUENCY());
			/*TODO*///DBG_LOG (1, "ted7360", ("noise %d sample:%d\n",
			/*TODO*///			NOISE_FREQUENCY, noisesamples));
			if (NOISE_ON()==0 || ((double) noisepos / noisesamples >= 1.0))
			{
				noisepos = 0;
			}
			break;
		case 0x11:
			ted7360[offset] = data;
			/*TODO*///DBG_LOG(1, "ted7360", ("%s volume %d, %s %s %s\n",
			/*TODO*///		       TONE_ON?"on":"off",
			/*TODO*///		       VOLUME, TONE1_ON?"tone1":"", TONE2_ON?"tone2":"",
			/*TODO*///		       NOISE_ON?"noise":""));
			if (TONE_ON()==0||TONE1_ON()==0) tone1pos=0;
			if (TONE_ON()==0||TONE2_ON()==0) tone2pos=0;
			if (TONE_ON()==0||NOISE_ON()==0) noisepos=0;
			break;
		}
	}
	
	/************************************/
	/* Sound handler update             */
	/************************************/
	public static StreamInitPtr ted7360_update = new StreamInitPtr() {
            public void handler(int param, ShortPtr buffer, int length) {
                int i, v;
	    
	    for (i = 0; i < length; i++)
	    {
		v = 0;
		if (TONE1_ON() != 0)
		{
		    if (tone1pos<=tone1samples/2) {
			v += 0x2ff; // depends on the volume between sound and noise
		    }
		    tone1pos++;
		    if (tone1pos>tone1samples) tone1pos=0;
		}
		if (TONE2_ON()!=0 || NOISE_ON()!=0 )
		{
		    if (TONE2_ON() != 0)
		    {						   /*higher priority ?! */
			if (tone2pos<=tone2samples/2) {
			    v += 0x2ff;
			}
			tone2pos++;
			if (tone2pos>tone2samples) tone2pos=0;
		    }
		    else
		    {
			v += noise.read((int) ((double) noisepos * noisesize / noisesamples));
			noisepos++;
			if ((double) noisepos / noisesamples >= 1.0)
			{
			    noisepos = 0;
			}
		    }
		}
		
		if (TONE_ON() != 0)
		{
		    int a=VOLUME();
		    if (a>8) a=8;
		    v = v * a;
		    buffer.write(i, v);
		}
		else
		    buffer.write(i, 0);
	    }
            }
        };
	
	
	/************************************/
	/* Sound handler start              */
	/************************************/
	public static ShStartPtr ted7360_custom_start = new ShStartPtr() {
            public int handler(MachineSound msound) {
                int i;
	
		if (options.samplerate==0) return 0;
	
		channel = stream_init ("ted7360", 50, options.samplerate, 0, ted7360_update);
	
		/* buffer for fastest played sample for 5 second
		 * so we have enough data for min 5 second */
		noisesize = NOISE_FREQUENCY_MAX() * NOISE_BUFFER_SIZE_SEC;
		noise = new UBytePtr(noisesize);
		if (noise == null)
		{
			return 1;
		}
	
		{
			int noiseshift = 0x7ffff8;
			int data;
	
			for (i = 0; i < noisesize; i++)
			{
				data = 0;
				if ((noiseshift & 0x400000) != 0)
					data |= 0x80;
				if ((noiseshift & 0x100000) != 0)
					data |= 0x40;
				if ((noiseshift & 0x010000) != 0)
					data |= 0x20;
				if ((noiseshift & 0x002000) != 0)
					data |= 0x10;
				if ((noiseshift & 0x000800) != 0)
					data |= 0x08;
				if ((noiseshift & 0x000080) != 0)
					data |= 0x04;
				if ((noiseshift & 0x000010) != 0)
					data |= 0x02;
				if ((noiseshift & 0x000004) != 0)
					data |= 0x01;
				noise.write(i, data);
				if (((noiseshift & 0x400000) == 0) != ((noiseshift & 0x002000) == 0))
					noiseshift = (noiseshift << 1) | 1;
				else
					noiseshift <<= 1;
			}
		}
		return 0;
            }
        };
	
	/************************************/
	/* Sound handler stop               */
	/************************************/
	public static ShStopPtr ted7360_custom_stop = new ShStopPtr() {
            public void handler() {
                noise = null;
            }
        };
	
	public static ShUpdatePtr ted7360_custom_update = new ShUpdatePtr() {
            public void handler() {
                // nothing to do
            }
        };
	
}
