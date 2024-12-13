/*========================================================================= */
/* This source implements the ADSR volume envelope of the SID-chip. */
/* Two different envelope shapes are implemented, an exponential */
/* approximation and the linear shape, which can easily be determined */
/* by reading the registers of the third SID operator. */
/* */
/* Accurate volume envelope times as of November 1994 are used */
/* courtesy of George W. Taylor <aa601@cfn.cs.dal.ca>, <yurik@io.org> */
/* They are slightly modified. */
/* */
/* To use the rounded envelope times from the C64 Programmers Reference */
/* Book define SID_REFTIMES at the Makefile level. */
/* */
/* To perform realtime calculations with floating point precision define */
/* SID_FPUENVE at the Makefile level. On high-end FPUs (not Pentium !), */
/* this can result in speed improvement. Default is integer fixpoint. */
/* */
/* Global Makefile definables: */
/* */
/*   DIRECT_FIXPOINT - use a union to access integer fixpoint operands */
/*                     in memory. This makes an assumption about the */
/*                     hardware and software architecture and therefore */
/*                     is considered a hack ! */
/* */
/* Local (or Makefile) definables: */
/* */
/*   SID_REFTIMES - use rounded envelope times */
/*   SID_FPUENVE  - use floating point precision for calculations */
/*                  (will override the global DIRECT_FIXPOINT setting !) */
/* */
/*========================================================================= */

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
package mess056.sndhrdw;

import static java.lang.Math.exp;
import static mess056.sndhrdw.side6581H.*;
import static mess056.sndhrdw.sidenvelH.*;
import static mess056.sndhrdw.sidvoiceH.*;

/**
 *
 * @author chusogar
 */
public class sidenvel {

/*TODO*///	#define VERBOSE_DBG 0
	
	
	
	public static byte masterVolumeLevels[] =
	{
	  (byte)0,  (byte)17,  (byte)34,  (byte)51,  (byte)68,  (byte)85, (byte)102, (byte)119,
	  (byte)136, (byte)153, (byte)170, (byte)187, (byte)204, (byte)221, (byte)238, (byte)255
	};
	
	static int[] masterAmplModTable = new int[16*256];

	static float attackTimes[] =
	{
	  /* milliseconds */
/*TODO*///	#if defined(SID_REFTIMES)
/*TODO*///	  2,8,16,24,38,56,68,80,
/*TODO*///	  100,250,500,800,1000,3000,5000,8000
/*TODO*///	#else
	  2.2528606f, 8.0099577f, 15.7696042f, 23.7795619f, 37.2963655f, 55.0684591f,
	  66.8330845f, 78.3473987f,
	  98.1219818f, 244.554021f, 489.108042f, 782.472742f, 977.715461f, 2933.64701f,
	  4889.07793f, 7822.72493f
/*TODO*///	#endif
	};
	
	static float decayReleaseTimes[] =
	{
	  /* milliseconds */
/*TODO*///	#if defined(SID_REFTIMES)
/*TODO*///	  8,24,48,72,114,168,204,240,
/*TODO*///	  300,750,1500,2400,3000,9000,15000,24000
/*TODO*///	#else
	  8.91777693f, 24.594051f, 48.4185907f, 73.0116639f, 114.512475f, 169.078356f,
	  205.199432f, 240.551975f,
	  301.266125f, 750.858245f, 1501.71551f, 2402.43682f, 3001.89298f, 9007.21405f,
	  15010.998f, 24018.2111f
/*TODO*///	#endif
	};
	
/*TODO*///	#ifdef SID_FPUENVE
/*TODO*///	  static float attackRates[16];
/*TODO*///	  static float decayReleaseRates[16];
/*TODO*///	#elif defined(DIRECT_FIXPOINT)
/*TODO*///	  static udword attackRates[16];
/*TODO*///	  static udword decayReleaseRates[16];
/*TODO*///	#else
	  static int[] attackRates = new int[16];
          static int[] attackRatesP = new int[16];
	  static int[] decayReleaseRates = new int[16];
          static int[] decayReleaseRatesP = new int[16];
/*TODO*///	#endif

          static int attackTabLen = 255;
          static int releaseTabLen;
          static int[] releasePos = new int[256];
	
	
	public static void enveEmuInit( int updateFreq, boolean measuredValues )
	{
		int i, j, k;
	
		releaseTabLen = releaseTab.length;
		for ( i = 0; i < 256; i++ )
		{
			j = 0;
			while (( j < releaseTabLen ) && (releaseTab[j] > i) )
			{
				j++;
			}
			if ( j < releaseTabLen )
			{
				releasePos[i] = j;
			}
			else
			{
				releasePos[i] = releaseTabLen -1;
			}
		}
	
		k = 0;
		for ( i = 0; i < 16; i++ )
		{
			for ( j = 0; j < 256; j++ )
			{
				int tmpVol = j;
				if (measuredValues)
				{
					tmpVol = (int) ((293.0*(1-exp(j/-130.0)))+4.0);
					if (j == 0)
						tmpVol = 0;
					if (tmpVol > 255)
						tmpVol = 255;
				}
				/* Want the modulated volume value in the high byte. */
				masterAmplModTable[k++] = ((tmpVol * masterVolumeLevels[i]) / 255) << 8;
			}
		}
	
		for ( i = 0; i < 16; i++ )
		{
/*TODO*///	#ifdef SID_FPUENVE
/*TODO*///			double scaledenvelen = floor(( attackTimes[i] * updateFreq ) / 1000UL );
/*TODO*///			if (scaledenvelen == 0)
/*TODO*///				scaledenvelen = 1;
/*TODO*///			attackRates[i] = attackTabLen / scaledenvelen;
/*TODO*///	
/*TODO*///			scaledenvelen = floor(( decayReleaseTimes[i] * updateFreq ) / 1000UL );
/*TODO*///			if (scaledenvelen == 0)
/*TODO*///				scaledenvelen = 1;
/*TODO*///			decayReleaseRates[i] = releaseTabLen / scaledenvelen;
/*TODO*///	#elif defined(DIRECT_FIXPOINT)
/*TODO*///			udword scaledenvelen = (udword)floor(( attackTimes[i] * updateFreq ) / 1000UL );
/*TODO*///			if (scaledenvelen == 0)
/*TODO*///				scaledenvelen = 1;
/*TODO*///			attackRates[i] = (attackTabLen << 16) / scaledenvelen;
/*TODO*///	
/*TODO*///			scaledenvelen = (udword)floor(( decayReleaseTimes[i] * updateFreq ) / 1000UL );
/*TODO*///			if (scaledenvelen == 0)
/*TODO*///				scaledenvelen = 1;
/*TODO*///			decayReleaseRates[i] = (releaseTabLen << 16) / scaledenvelen;
/*TODO*///	#else
			//int scaledenvelen = (int)(/*floor*/(( attackTimes[i] * updateFreq ) / 1000UL ));
                    int scaledenvelen = (int)(/*floor*/(( attackTimes[i] * updateFreq ) / 1000 ));
	
			if (scaledenvelen == 0)
				scaledenvelen = 1;
			attackRates[i] = attackTabLen / scaledenvelen;
			//attackRatesP[i] = (( attackTabLen % scaledenvelen ) * 65536UL ) / scaledenvelen;
                        attackRatesP[i] = (( attackTabLen % scaledenvelen ) * 65536 ) / scaledenvelen;
	
			//scaledenvelen = (udword)(/*floor*/(( decayReleaseTimes[i] * updateFreq ) / 1000UL ));
                        scaledenvelen = (int) (/*floor*/(( decayReleaseTimes[i] * updateFreq ) / 1000 ));
			if (scaledenvelen == 0)
				scaledenvelen = 1;
			decayReleaseRates[i] = releaseTabLen / scaledenvelen;
			//decayReleaseRatesP[i] = (( releaseTabLen % scaledenvelen ) * 65536UL ) / scaledenvelen;
                        decayReleaseRatesP[i] = (( releaseTabLen % scaledenvelen ) * 65536 ) / scaledenvelen;
/*TODO*///	#endif
	  }
	}
	
	/* Reset op. */
	
	public static void enveEmuResetOperator(sidOperator pVoice)
	{
            //System.out.println("enveEmuResetOperator NOT IMPLEMENTED!!!!");
		/* mute, end of R-phase */
		pVoice.ADSRctrl = ENVE_MUTE;
	//	pVoice.gateOnCtrl = (pVoice.gateOffCtrl = false);
	
/*TODO*///	#ifdef SID_FPUENVE
/*TODO*///		pVoice.fenveStep = (pVoice.fenveStepAdd = 0);
/*TODO*///		pVoice.enveStep = 0;
/*TODO*///	#elif defined(DIRECT_FIXPOINT)
/*TODO*///		pVoice.enveStep.l = (pVoice.enveStepAdd.l = 0);
/*TODO*///	#else
		pVoice.enveStep = (pVoice.enveStepPnt = 0);
		pVoice.enveStepAdd = (pVoice.enveStepAddPnt = 0);
/*TODO*///	#endif
		pVoice.enveSusVol = 0;
		pVoice.enveVol = 0;
		pVoice.enveShortAttackCount = 0;
	}

/*TODO*///	INLINE uword enveEmuStartAttack(sidOperator*);
/*TODO*///	INLINE uword enveEmuStartDecay(sidOperator*);
/*TODO*///	INLINE uword enveEmuStartRelease(sidOperator*);
/*TODO*///	INLINE uword enveEmuAlterAttack(sidOperator*);
/*TODO*///	INLINE uword enveEmuAlterDecay(sidOperator*);
/*TODO*///	INLINE uword enveEmuAlterSustain(sidOperator*);
/*TODO*///	INLINE uword enveEmuAlterSustainDecay(sidOperator*);
/*TODO*///	INLINE uword enveEmuAlterRelease(sidOperator*);
/*TODO*///	INLINE uword enveEmuAttack(sidOperator*);
/*TODO*///	INLINE uword enveEmuDecay(sidOperator*);
/*TODO*///	INLINE uword enveEmuSustain(sidOperator*);
/*TODO*///	INLINE uword enveEmuSustainDecay(sidOperator*);
/*TODO*///	INLINE uword enveEmuRelease(sidOperator*);
/*TODO*///	INLINE uword enveEmuMute(sidOperator*);
/*TODO*///	
/*TODO*///	INLINE uword enveEmuStartShortAttack(sidOperator*);
/*TODO*///	INLINE uword enveEmuAlterShortAttack(sidOperator*);
/*TODO*///	INLINE uword enveEmuShortAttack(sidOperator*);
	
	
	/* Real-time functions. */
	/* Order is important because of inline optimizations. */
	/* */
	/* ADSRctrl is (index*2) to enveModeTable[], because of KEY-bit. */
	
        public static ptr2sidUwordFunc enveEmuEnveAdvance = new ptr2sidUwordFunc() {
            public int handler(sidOperator pVoice) {
/*TODO*///	#ifdef SID_FPUENVE
/*TODO*///		pVoice.fenveStep += pVoice.fenveStepAdd;
/*TODO*///	#elif defined(DIRECT_FIXPOINT)
/*TODO*///		pVoice.enveStep.l += pVoice.enveStepAdd.l;
/*TODO*///	#else
		pVoice.enveStepPnt += pVoice.enveStepAddPnt;
		pVoice.enveStep += pVoice.enveStepAdd + (( pVoice.enveStepPnt > 65535 )?1:0);
		pVoice.enveStepPnt &= 0xFFFF;
/*TODO*///	#endif

                return 0xff;
            }
        };
/*TODO*///	
/*TODO*///	/* */
/*TODO*///	/* Mute/Idle. */
/*TODO*///	/* */
/*TODO*///	
/*TODO*///	/* Only used in the beginning. */
        public static ptr2sidUwordFunc enveEmuMute = new ptr2sidUwordFunc() {
            public int handler(sidOperator pVoice) {
		return 0;
            }
        };
	
	/* */
	/* Release */
	/* */
	
        public static ptr2sidUwordFunc enveEmuRelease = new ptr2sidUwordFunc() {
            public int handler(sidOperator pVoice) {
/*TODO*///	#ifdef SID_FPUENVE
/*TODO*///		pVoice.enveStep = (uword)pVoice.fenveStep;
/*TODO*///	#endif
/*TODO*///	#if defined(DIRECT_FIXPOINT) && !defined(SID_FPUENVE)
/*TODO*///		if ( pVoice.enveStep.w[HI] >= releaseTabLen )
/*TODO*///	#else
		if ( pVoice.enveStep >= releaseTabLen )
/*TODO*///	#endif
		{
			pVoice.enveVol = releaseTab[releaseTabLen -1];
			return masterAmplModTable[ pVoice.sid.masterVolumeAmplIndex + pVoice.enveVol ];
		}
		else
		{
/*TODO*///	#if defined(DIRECT_FIXPOINT) && !defined(SID_FPUENVE)
/*TODO*///			pVoice.enveVol = releaseTab[pVoice.enveStep.w[HI]];
/*TODO*///	#else
			pVoice.enveVol = releaseTab[pVoice.enveStep];
/*TODO*///	#endif
			enveEmuEnveAdvance.handler(pVoice);
			return masterAmplModTable[ pVoice.sid.masterVolumeAmplIndex + pVoice.enveVol ];
		}
            }
        };
            
	
        public static ptr2sidUwordFunc enveEmuAlterRelease = new ptr2sidUwordFunc() {
            public int handler(sidOperator pVoice) {
		int release = pVoice.SIDSR & 0x0F;
/*TODO*///	#ifdef SID_FPUENVE
/*TODO*///		pVoice.fenveStepAdd = decayReleaseRates[release];
/*TODO*///	#elif defined(DIRECT_FIXPOINT)
/*TODO*///		pVoice.enveStepAdd.l = decayReleaseRates[release];
/*TODO*///	#else
		pVoice.enveStepAdd = decayReleaseRates[release];
		pVoice.enveStepAddPnt = decayReleaseRatesP[release];
/*TODO*///	#endif
		pVoice.ADSRproc = enveEmuRelease;
		return enveEmuRelease.handler(pVoice);
            }
        };
	
        public static ptr2sidUwordFunc enveEmuStartRelease = new ptr2sidUwordFunc() {
            public int handler(sidOperator pVoice) {
		pVoice.ADSRctrl = ENVE_RELEASE;
/*TODO*///	#ifdef SID_FPUENVE
/*TODO*///		pVoice.fenveStep = releasePos[pVoice.enveVol];
/*TODO*///	#elif defined(DIRECT_FIXPOINT)
/*TODO*///		pVoice.enveStep.w[HI] = releasePos[pVoice.enveVol];
/*TODO*///		pVoice.enveStep.w[LO] = 0;
/*TODO*///	#else
		pVoice.enveStep = releasePos[(pVoice.enveVol)&0xff];
		pVoice.enveStepPnt = 0;
/*TODO*///	#endif
		return enveEmuAlterRelease.handler(pVoice);
            }
        };
	
	/* */
	/* Sustain */
	/* */
	
        public static ptr2sidUwordFunc enveEmuSustain = new ptr2sidUwordFunc() {
            public int handler(sidOperator pVoice) {
		return masterAmplModTable[(pVoice.sid.masterVolumeAmplIndex+pVoice.enveVol)&0xfff];
            }
        };
	
        public static ptr2sidUwordFunc enveEmuSustainDecay = new ptr2sidUwordFunc() {
            public int handler(sidOperator pVoice) {
/*TODO*///	#ifdef SID_FPUENVE
/*TODO*///		pVoice.enveStep = (uword)pVoice.fenveStep;
/*TODO*///	#endif
/*TODO*///	#if defined(DIRECT_FIXPOINT) && !defined(SID_FPUENVE)
/*TODO*///		if ( pVoice.enveStep.w[HI] >= releaseTabLen )
/*TODO*///	#else
		if ( pVoice.enveStep >= releaseTabLen )
/*TODO*///	#endif
		{
			pVoice.enveVol = releaseTab[releaseTabLen-1];
			return enveEmuAlterSustain.handler(pVoice);
		}
		else
		{
/*TODO*///	#if defined(DIRECT_FIXPOINT) && !defined(SID_FPUENVE)
/*TODO*///			pVoice.enveVol = releaseTab[pVoice.enveStep.w[HI]];
/*TODO*///	#else
			pVoice.enveVol = releaseTab[pVoice.enveStep];
/*TODO*///	#endif
			/* Will be controlled from sidEmuSet2(). */
			if ( pVoice.enveVol <= pVoice.enveSusVol )
			{
				pVoice.enveVol = pVoice.enveSusVol;
				return enveEmuAlterSustain.handler(pVoice);
			}
			else
			{
				enveEmuEnveAdvance.handler(pVoice);
				return masterAmplModTable[ pVoice.sid.masterVolumeAmplIndex + pVoice.enveVol ];
			}
		}
            }
        };
	
/*TODO*///	/* This is the same as enveEmuStartSustainDecay(). */
        public static ptr2sidUwordFunc enveEmuAlterSustainDecay = new ptr2sidUwordFunc() {
            public int handler(sidOperator pVoice) {
		int decay = pVoice.SIDAD & 0x0F ;
/*TODO*///	#ifdef SID_FPUENVE
/*TODO*///		pVoice.fenveStepAdd = decayReleaseRates[decay];
/*TODO*///	#elif defined(DIRECT_FIXPOINT)
/*TODO*///		pVoice.enveStepAdd.l = decayReleaseRates[decay];
/*TODO*///	#else
		pVoice.enveStepAdd = decayReleaseRates[decay];
		pVoice.enveStepAddPnt = decayReleaseRatesP[decay];
/*TODO*///	#endif
		pVoice.ADSRproc = enveEmuSustainDecay;
		return enveEmuSustain.handler(pVoice);
            }
        };
	
	/* This is the same as enveEmuStartSustain(). */
        public static ptr2sidUwordFunc enveEmuAlterSustain = new ptr2sidUwordFunc() {
            public int handler(sidOperator pVoice) {
		if ( pVoice.enveVol > pVoice.enveSusVol )
		{
			pVoice.ADSRctrl = ENVE_SUSTAINDECAY;
			pVoice.ADSRproc = enveEmuSustainDecay;
			return enveEmuAlterSustainDecay.handler(pVoice);
		}
		else
		{
			pVoice.ADSRctrl = ENVE_SUSTAIN;
			pVoice.ADSRproc = enveEmuSustain;
			return enveEmuSustain.handler(pVoice);
		}
            }
        };
	
/*TODO*///	/* */
/*TODO*///	/* Decay */
/*TODO*///	/* */
/*TODO*///	
        public static ptr2sidUwordFunc enveEmuDecay = new ptr2sidUwordFunc() {
            public int handler(sidOperator pVoice) {
/*TODO*///	#ifdef SID_FPUENVE
/*TODO*///		pVoice.enveStep = (uword)pVoice.fenveStep;
/*TODO*///	#endif
/*TODO*///	#if defined(DIRECT_FIXPOINT) && !defined(SID_FPUENVE)
/*TODO*///		if ( pVoice.enveStep.w[HI] >= releaseTabLen )
/*TODO*///	#else
		if ( pVoice.enveStep >= releaseTabLen )
/*TODO*///	#endif
		{
			pVoice.enveVol = pVoice.enveSusVol;
			return enveEmuAlterSustain.handler(pVoice);  /* start sustain */
		}
		else
		{
/*TODO*///	#if defined(DIRECT_FIXPOINT) && !defined(SID_FPUENVE)
/*TODO*///			pVoice.enveVol = releaseTab[pVoice.enveStep.w[HI]];
/*TODO*///	#else
			pVoice.enveVol = releaseTab[pVoice.enveStep];
/*TODO*///	#endif
			/* Will be controlled from sidEmuSet2(). */
			if ( pVoice.enveVol <= pVoice.enveSusVol )
			{
				pVoice.enveVol = pVoice.enveSusVol;
				return enveEmuAlterSustain.handler(pVoice);  /* start sustain */
			}
			else
			{
				enveEmuEnveAdvance.handler(pVoice);
				return masterAmplModTable[ pVoice.sid.masterVolumeAmplIndex + pVoice.enveVol ];
			}
		}
            }
        };
	
        public static ptr2sidUwordFunc enveEmuAlterDecay = new ptr2sidUwordFunc() {
            public int handler(sidOperator pVoice) {
		int decay = pVoice.SIDAD & 0x0F ;
/*TODO*///	#ifdef SID_FPUENVE
/*TODO*///		pVoice.fenveStepAdd = decayReleaseRates[decay];
/*TODO*///	#elif defined(DIRECT_FIXPOINT)
/*TODO*///		pVoice.enveStepAdd.l = decayReleaseRates[decay];
/*TODO*///	#else
		pVoice.enveStepAdd = decayReleaseRates[decay];
		pVoice.enveStepAddPnt = decayReleaseRatesP[decay];
/*TODO*///	#endif
		pVoice.ADSRproc = enveEmuDecay;
		return enveEmuDecay.handler(pVoice);
            }
        };
	
        public static ptr2sidUwordFunc enveEmuStartDecay = new ptr2sidUwordFunc() {
            public int handler(sidOperator pVoice) {
		pVoice.ADSRctrl = ENVE_DECAY;
/*TODO*///	#ifdef SID_FPUENVE
/*TODO*///		pVoice.fenveStep = 0;
/*TODO*///	#elif defined(DIRECT_FIXPOINT)
/*TODO*///		pVoice.enveStep.l = 0;
/*TODO*///	#else
		pVoice.enveStep = (pVoice.enveStepPnt = 0);
/*TODO*///	#endif
		return enveEmuAlterDecay.handler(pVoice);
            }
        };
	
	/* */
	/* Attack */
	/* */
	
        public static ptr2sidUwordFunc enveEmuAttack = new ptr2sidUwordFunc() {
            public int handler(sidOperator pVoice) {
/*TODO*///	#ifdef SID_FPUENVE
/*TODO*///		pVoice.enveStep = (uword)pVoice.fenveStep;
/*TODO*///	#endif
/*TODO*///	#if defined(DIRECT_FIXPOINT) && !defined(SID_FPUENVE)
/*TODO*///		if ( pVoice.enveStep.w[HI] > attackTabLen )
/*TODO*///	#else
		if ( pVoice.enveStep >= attackTabLen )
/*TODO*///	#endif
			return enveEmuStartDecay.handler(pVoice);
		else
		{
/*TODO*///	#if defined(DIRECT_FIXPOINT) && !defined(SID_FPUENVE)
/*TODO*///			pVoice.enveVol = pVoice.enveStep.w[HI];
/*TODO*///	#else
			pVoice.enveVol = pVoice.enveStep;
/*TODO*///	#endif
			enveEmuEnveAdvance.handler(pVoice);
			return masterAmplModTable[ pVoice.sid.masterVolumeAmplIndex + pVoice.enveVol ];
		}
            }
        };
	
        public static ptr2sidUwordFunc enveEmuAlterAttack = new ptr2sidUwordFunc() {
            public int handler(sidOperator pVoice) {
		int attack = pVoice.SIDAD >> 4;
/*TODO*///	#ifdef SID_FPUENVE
/*TODO*///		pVoice.fenveStepAdd = attackRates[attack];
/*TODO*///	#elif defined(DIRECT_FIXPOINT)
/*TODO*///		pVoice.enveStepAdd.l = attackRates[attack];
/*TODO*///	#else
		pVoice.enveStepAdd = attackRates[attack&0xf];
		pVoice.enveStepAddPnt = attackRatesP[attack&0xf];
/*TODO*///	#endif
		pVoice.ADSRproc = enveEmuAttack;
		return enveEmuAttack.handler(pVoice);
            }
        };

        public static ptr2sidUwordFunc enveEmuStartAttack = new ptr2sidUwordFunc() {
            public int handler(sidOperator pVoice) {
		pVoice.ADSRctrl = ENVE_ATTACK;
/*TODO*///	#ifdef SID_FPUENVE
/*TODO*///		pVoice.fenveStep = (float)pVoice.enveVol;
/*TODO*///	#elif defined(DIRECT_FIXPOINT)
/*TODO*///		pVoice.enveStep.w[HI] = pVoice.enveVol;
/*TODO*///		pVoice.enveStep.w[LO] = 0;
/*TODO*///	#else
		pVoice.enveStep = pVoice.enveVol;
		pVoice.enveStepPnt = 0;
/*TODO*///	#endif
		return enveEmuAlterAttack.handler(pVoice);
            }
        };

/*TODO*///	/* */
/*TODO*///	/* Experimental. */
/*TODO*///	/* */
/*TODO*///	
/*TODO*///	/*/*
        public static ptr2sidUwordFunc enveEmuShortAttack = new ptr2sidUwordFunc() {
            public int handler(sidOperator pVoice) {
/*TODO*///	#ifdef SID_FPUENVE
/*TODO*///		pVoice.enveStep = (uword)pVoice.fenveStep;
/*TODO*///	#endif
/*TODO*///	#if defined(DIRECT_FIXPOINT) && !defined(SID_FPUENVE)
/*TODO*///		if ((pVoice.enveStep.w[HI] > attackTabLen) ||
/*TODO*///			(pVoice.enveShortAttackCount == 0))
/*TODO*///	#else
		if ((pVoice.enveStep >= attackTabLen) ||
			(pVoice.enveShortAttackCount == 0))
/*TODO*///	#endif
	/*		return enveEmuStartRelease(pVoice); */
			return enveEmuStartDecay.handler(pVoice);
		else
		{
/*TODO*///	#if defined(DIRECT_FIXPOINT) && !defined(SID_FPUENVE)
/*TODO*///			pVoice.enveVol = pVoice.enveStep.w[HI];
/*TODO*///	#else
			pVoice.enveVol = pVoice.enveStep;
/*TODO*///	#endif
		    pVoice.enveShortAttackCount--;
	/*		cout << hex << pVoice.enveShortAttackCount << " / " << pVoice.enveVol << endl; */
			enveEmuEnveAdvance.handler(pVoice);
			return masterAmplModTable[ pVoice.sid.masterVolumeAmplIndex + pVoice.enveVol ];
		}
            }
        };
	
        public static ptr2sidUwordFunc enveEmuAlterShortAttack = new ptr2sidUwordFunc() {
            public int handler(sidOperator pVoice) {
		int attack = pVoice.SIDAD >> 4;
/*TODO*///	#ifdef SID_FPUENVE
/*TODO*///		pVoice.fenveStepAdd = attackRates[attack];
/*TODO*///	#elif defined(DIRECT_FIXPOINT)
/*TODO*///		pVoice.enveStepAdd.l = attackRates[attack];
/*TODO*///	#else
		pVoice.enveStepAdd = attackRates[attack];
		pVoice.enveStepAddPnt = attackRatesP[attack];
/*TODO*///	#endif
		pVoice.ADSRproc = enveEmuShortAttack;
		return enveEmuShortAttack.handler(pVoice);
            }
        };
	
        public static ptr2sidUwordFunc enveEmuStartShortAttack = new ptr2sidUwordFunc() {
            public int handler(sidOperator pVoice) {
		pVoice.ADSRctrl = ENVE_SHORTATTACK;
/*TODO*///	#ifdef SID_FPUENVE
/*TODO*///		pVoice.fenveStep = (float)pVoice.enveVol;
/*TODO*///	#elif defined(DIRECT_FIXPOINT)
/*TODO*///		pVoice.enveStep.w[HI] = pVoice.enveVol;
/*TODO*///		pVoice.enveStep.w[LO] = 0;
/*TODO*///	#else
		pVoice.enveStep = pVoice.enveVol;
		pVoice.enveStepPnt = 0;
/*TODO*///	#endif
		pVoice.enveShortAttackCount = 65535;  /* unused */
		return enveEmuAlterShortAttack.handler(pVoice);
            }    
        };
        
        public static ptr2sidUwordFunc enveModeTable[] =
	{
		/* 0 */
		enveEmuStartAttack, enveEmuStartRelease,
		enveEmuAttack, enveEmuDecay, enveEmuSustain, enveEmuRelease,
		enveEmuSustainDecay, enveEmuMute,
		/* 16 */
		enveEmuStartShortAttack,
		enveEmuMute, enveEmuMute, enveEmuMute,
		enveEmuMute, enveEmuMute, enveEmuMute, enveEmuMute,
	    /* 32		 */
		enveEmuStartAttack, enveEmuStartRelease,
		enveEmuAlterAttack, enveEmuAlterDecay, enveEmuAlterSustain, enveEmuAlterRelease,
		enveEmuAlterSustainDecay, enveEmuMute,
	    /* 48		 */
		enveEmuStartShortAttack,
		enveEmuMute, enveEmuMute, enveEmuMute,
		enveEmuMute, enveEmuMute, enveEmuMute, enveEmuMute
	};

}
