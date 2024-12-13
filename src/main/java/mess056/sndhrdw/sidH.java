/*
  approximation of the sid6581 chip
  this part is for one chip,
*/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.sndhrdw;

import static arcadeflex056.fucPtr.*;
import static mess056.sndhrdw.sidvoiceH.*;

public class sidH
{
    
        public static class _filter {
            public boolean Enabled;
            public int Type, CurType;
            public float Dy, ResDy;
            public int Value;
        };
	
	/* private area */
	public static class _SID6581 {
	    public int on;
	
	    public int mixer_channel; // mame stream/ mixer channel
	
	    public ReadHandlerPtr ad_read;
	    public int type;
	    public int clock;
	
	    public int PCMfreq; // samplerate of the current systems soundcard/DAC
	    public int PCMsid, PCMsidNoise;
	
/*TODO*///	#if 0
/*TODO*///		/* following depends on type */
/*TODO*///		ptr2sidVoidFunc ModeNormalTable[16];
/*TODO*///		ptr2sidVoidFunc ModeRingTable[16];
/*TODO*///		// for speed reason it could be better to make them global!
/*TODO*///		UINT8* waveform30;
/*TODO*///		UINT8* waveform50;
/*TODO*///		UINT8* waveform60;
/*TODO*///		UINT8* waveform70;
/*TODO*///	#endif
		public int[] reg = new int[0x20];
	
	//	bool sidKeysOn[0x20], sidKeysOff[0x20];
	
		public int masterVolume;
		public int masterVolumeAmplIndex;
	
                public _filter filter = new _filter();
	
		public sidOperator optr1, optr2, optr3;
                public int optr3_outputmask;
	};
	
	public static _SID6581 SID6581 = new _SID6581();
}
