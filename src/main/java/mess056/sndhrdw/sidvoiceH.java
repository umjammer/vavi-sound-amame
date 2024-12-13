/*
  approximation of the sid6581 chip
  this part is for 1 (of the 3) voices of a chip
*/
/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.sndhrdw;

import static arcadeflex056.fucPtr.*;
import static mess056.sndhrdw.sidH.*;

/**
 *
 * @author chusogar
 */
public class sidvoiceH {
    
    public static abstract interface ptr2sidUwordFunc {

        public abstract int handler(sidOperator pVoice);
    }
    
    /*public static abstract interface ptr2sidVoidFunc {

        public abstract void handler(sidOperator pVoice);
    }*/
            
	
    public static class sw_storage
    {
	public int len;

        public int stp;

        public int pnt;

    };

/*TODO*///	struct _SID6581;
/*TODO*///	
    
        public _SID6581 sid = new _SID6581();
        
        public static class _sidOperator
        {
		public int[] reg = new int[7];
		public int SIDfreq=0;
                public int SIDpulseWidth;
                public byte SIDctrl=0;
                public byte SIDAD, SIDSR;
	
                public boolean sync;

                public int pulseIndex, newPulseIndex;
                public int curSIDfreq;
                public int curNoiseFreq;

                public int output;//, outputMask;
	
		public char filtVoiceMask;
		public boolean filtEnabled;
		public float filtLow, filtRef;
		public byte filtIO;

		public int cycleLenCount;
/*TODO*///	#if defined(DIRECT_FIXPOINT)
/*TODO*///		cpuLword cycleLen, cycleAddLen;
/*TODO*///	#else
                public int cycleAddLenPnt;
                public int cycleLen, cycleLenPnt;
/*TODO*///	#endif

                public ptr2sidUwordFunc outProc;
                public ptr2sidUwordFunc waveProc;

/*TODO*///	#if defined(DIRECT_FIXPOINT)
/*TODO*///		cpuLword waveStep, waveStepAdd;
/*TODO*///	#else
                public int waveStep, waveStepAdd;
                public int waveStepPnt, waveStepAddPnt;
/*TODO*///	#endif
                public int waveStepOld;
                public sw_storage[] wavePre=new sw_storage[2];
	
/*TODO*///	#if defined(DIRECT_FIXPOINT) && defined(LARGE_NOISE_TABLE)
/*TODO*///		cpuLword noiseReg;
/*TODO*///	#elif defined(DIRECT_FIXPOINT)
                public int noiseReg;
/*TODO*///	#else
/*TODO*///		udword noiseReg;
/*TODO*///	#endif
                public int noiseStep, noiseStepAdd;
		public byte noiseOutput;
                public boolean noiseIsLocked;

                public byte ADSRctrl;
/*TODO*///	//	bool gateOnCtrl, gateOffCtrl;
                public ptr2sidUwordFunc ADSRproc;

/*TODO*///	#ifdef SID_FPUENVE
/*TODO*///		filterfloat fenveStep, fenveStepAdd;
/*TODO*///		udword enveStep;
/*TODO*///	#elif defined(DIRECT_FIXPOINT)
/*TODO*///		cpuLword enveStep, enveStepAdd;
/*TODO*///	#else
		public int enveStep, enveStepAdd;
                public int enveStepPnt, enveStepAddPnt;
/*TODO*///	#endif
		public int enveVol, enveSusVol;
                public int enveShortAttackCount;
                
                public _sidOperator(){
                    for (int _i=0 ; _i<2 ; _i++)
                        wavePre[_i] = new sw_storage();
                }
                    
        }
                
        public static class sidOperator
	{
		public _SID6581 sid = new _SID6581();
		public int[] reg = new int[7];
		public int SIDfreq=0;
                public int SIDpulseWidth;
                public byte SIDctrl=0;
                public byte SIDAD, SIDSR;
	
                public sidOperator carrier;
		public sidOperator modulator;
		public boolean sync;

                public int pulseIndex, newPulseIndex;
                public int curSIDfreq;
                public int curNoiseFreq;

                public int output;//, outputMask;
	
		public char filtVoiceMask;
		public boolean filtEnabled;
		public float filtLow, filtRef;
		public byte filtIO;

		public int cycleLenCount;
/*TODO*///	#if defined(DIRECT_FIXPOINT)
/*TODO*///		cpuLword cycleLen, cycleAddLen;
/*TODO*///	#else
                public int cycleAddLenPnt;
                public int cycleLen, cycleLenPnt;
/*TODO*///	#endif

                public ptr2sidUwordFunc outProc;
                public ptr2sidUwordFunc waveProc;

/*TODO*///	#if defined(DIRECT_FIXPOINT)
/*TODO*///		cpuLword waveStep, waveStepAdd;
/*TODO*///	#else
                public int waveStep, waveStepAdd;
                public int waveStepPnt, waveStepAddPnt;
/*TODO*///	#endif
                public int waveStepOld;
                public sw_storage[] wavePre=new sw_storage[2];
	
/*TODO*///	#if defined(DIRECT_FIXPOINT) && defined(LARGE_NOISE_TABLE)
/*TODO*///		cpuLword noiseReg;
/*TODO*///	#elif defined(DIRECT_FIXPOINT)
                public int noiseReg;
/*TODO*///	#else
/*TODO*///		udword noiseReg;
/*TODO*///	#endif
                public int noiseStep, noiseStepAdd;
		public byte noiseOutput;
                public boolean noiseIsLocked;

                public byte ADSRctrl;
/*TODO*///	//	bool gateOnCtrl, gateOffCtrl;
                public ptr2sidUwordFunc ADSRproc;

/*TODO*///	#ifdef SID_FPUENVE
/*TODO*///		filterfloat fenveStep, fenveStepAdd;
/*TODO*///		udword enveStep;
/*TODO*///	#elif defined(DIRECT_FIXPOINT)
/*TODO*///		cpuLword enveStep, enveStepAdd;
/*TODO*///	#else
		public int enveStep, enveStepAdd;
                public int enveStepPnt, enveStepAddPnt;
/*TODO*///	#endif
		public int enveVol, enveSusVol;
                public int enveShortAttackCount;
                
                public sidOperator(){
                    for (int _i=0 ; _i<2 ; _i++)
                        wavePre[_i] = new sw_storage();
                    
                }
	};

/*TODO*///	typedef sbyte (*ptr2sidFunc)(sidOperator *);
/*TODO*///	typedef uword (*ptr2sidUwordFunc)(sidOperator *);
/*TODO*///	typedef void (*ptr2sidVoidFunc)(sidOperator *);
/*TODO*///	
/*TODO*///	void sidClearOperator( sidOperator* pVoice );
/*TODO*///	
/*TODO*///	void sidEmuSet(sidOperator* pVoice);
/*TODO*///	void sidEmuSet2(sidOperator* pVoice);
/*TODO*///	sbyte sidWaveCalcNormal(sidOperator* pVoice);
/*TODO*///	
/*TODO*///	void sidInitWaveformTables(SIDTYPE type);
/*TODO*///	void sidInitMixerEngine(void);
/*TODO*///	
/*TODO*///	#if 0
/*TODO*///	extern ptr2sidVoidFunc sid6581ModeNormalTable[16];
/*TODO*///	extern ptr2sidVoidFunc sid6581ModeRingTable[16];
/*TODO*///	extern ptr2sidVoidFunc sid8580ModeNormalTable[16];
/*TODO*///	extern ptr2sidVoidFunc sid8580ModeRingTable[16];
/*TODO*///	#endif
/*TODO*///	
/*TODO*///	#endif
    
}
