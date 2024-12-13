/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.sndhrdw;

import static mess056.includes.sid6581H.MOS8580;
import static mess056.sndhrdw.sidenvel.*;
import static mess056.sndhrdw.sidenvelH.*;
import static mess056.sndhrdw.sidvoiceH.*;
import static mess056.sndhrdw.sidw6581H.*;
import static mess056.sndhrdw.sidw6581_30H.waveform30_6581;
import static mess056.sndhrdw.sidw6581_50H.waveform50_6581;
import static mess056.sndhrdw.sidw6581_60H.waveform60_6581;
import static mess056.sndhrdw.sidw6581_70H.waveform70_6581;

import static mess056.sndhrdw.sidw8580H.*;
import static mess056.sndhrdw.sidw8580H_30.waveform30_8580;
import static mess056.sndhrdw.sidw8580H_50.waveform50_8580;
import static mess056.sndhrdw.sidw8580H_60.waveform60_8580;
import static mess056.sndhrdw.sidw8580H_70.waveform70_8580;

/**
 *
 * @author chusogar
 */
public class sidvoice {
	
	static byte[] triangleTable = new byte[4096];
        static byte[] sawtoothTable = new byte[4096];
        static byte[] squareTable = new byte[2*4096];
	static int[] waveform30=new int[2*4096];
        static int[] waveform50=new int[2*4096];
        static int[] waveform60=new int[2*4096];
        static int[] waveform70=new int[2*4096];
/*TODO*///	#if defined(LARGE_NOISE_TABLE)
/*TODO*///	  static ubyte noiseTableMSB[1<<8];
/*TODO*///	  static ubyte noiseTableLSB[1L<<16];
/*TODO*///	#else
        public static byte[] noiseTableMSB = new byte[1<<8];
        public static byte[] noiseTableMID = new byte[1<<8];
	public static byte[] noiseTableLSB = new byte[1<<8];
/*TODO*///	#endif

        public static byte[] ampMod1x8;

        public static int noiseSeed = 0x7ffff8;

	public static void sidInitMixerEngine()
	{
		int uk;
		int si, sj;
	
		/* 8-bit volume modulation tables. */
		float filterAmpl = 1.0f;
	
		filterAmpl = 0.7f;
	
		ampMod1x8=new byte[256*256];
/*TODO*///		if (ampMod1x8 == null) {
/*TODO*///			printf("out of memory\n");exit(1);
/*TODO*///		}
	
		uk = 0;
		for ( si = 0; si < 256; si++ )
		{
			for ( sj = -128; sj < 128; sj++, uk++ )
			{
				ampMod1x8[uk] = (byte) (((si*sj)/255)*filterAmpl);
			}
		}
	
	}
	
	public static void waveAdvance(sidOperator pVoice)
	{
/*TODO*///	#if defined(DIRECT_FIXPOINT)
/*TODO*///		pVoice.waveStep.l += pVoice.waveStepAdd.l;
/*TODO*///		pVoice.waveStep.w[HI] &= 4095;
/*TODO*///	#else
		pVoice.waveStepPnt += pVoice.waveStepAddPnt;
		pVoice.waveStep += pVoice.waveStepAdd;
		if (pVoice.waveStepPnt > 65535 ) pVoice.waveStep++;
		pVoice.waveStepPnt &= 0xFFFF;
		pVoice.waveStep &= 4095;
/*TODO*///	#endif
	}

	public static void noiseAdvance(sidOperator pVoice)
	{
		pVoice.noiseStep += pVoice.noiseStepAdd;
		if (pVoice.noiseStep >= (1L<<20))
		{
			pVoice.noiseStep -= (1L<<20);
/*TODO*///	#if defined(DIRECT_FIXPOINT)
/*TODO*///			pVoice.noiseReg.l = (pVoice.noiseReg.l << 1) |
/*TODO*///				(((pVoice.noiseReg.l >> 22) ^ (pVoice.noiseReg.l >> 17)) & 1);
/*TODO*///	#else
			pVoice.noiseReg = (pVoice.noiseReg << 1) |
				(((pVoice.noiseReg >> 22) ^ (pVoice.noiseReg >> 17)) & 1);
/*TODO*///	#endif
/*TODO*///	#if defined(DIRECT_FIXPOINT) && defined(LARGE_NOISE_TABLE)
/*TODO*///			pVoice.noiseOutput = (noiseTableLSB[pVoice.noiseReg.w[LO]]
/*TODO*///								   |noiseTableMSB[pVoice.noiseReg.w[HI]&0xff]);
/*TODO*///	#elif defined(DIRECT_FIXPOINT)
/*TODO*///			pVoice.noiseOutput = (noiseTableLSB[pVoice.noiseReg.b[LOLO]]
/*TODO*///								   |noiseTableMID[pVoice.noiseReg.b[LOHI]]
/*TODO*///								   |noiseTableMSB[pVoice.noiseReg.b[HILO]]);
/*TODO*///	#else
			pVoice.noiseOutput = (byte) (noiseTableLSB[pVoice.noiseReg&0xff]
								   |noiseTableMID[pVoice.noiseReg>>8&0xff]
								   |noiseTableMSB[pVoice.noiseReg>>16&0xff]);
/*TODO*///	#endif
		}
	}
	
	public static void noiseAdvanceHp(sidOperator pVoice)
	{
		int tmp = pVoice.noiseStepAdd;
		while (tmp >= (1L<<20))
		{
			tmp -= (1L<<20);
/*TODO*///	#if defined(DIRECT_FIXPOINT)
/*TODO*///			pVoice.noiseReg.l = (pVoice.noiseReg.l << 1) |
/*TODO*///				(((pVoice.noiseReg.l >> 22) ^ (pVoice.noiseReg.l >> 17)) & 1);
/*TODO*///	#else
			pVoice.noiseReg = (pVoice.noiseReg << 1) |
				(((pVoice.noiseReg >> 22) ^ (pVoice.noiseReg >> 17)) & 1);
/*TODO*///	#endif
		}
		pVoice.noiseStep += tmp;
		if (pVoice.noiseStep >= (1L<<20))
		{
			pVoice.noiseStep -= (1L<<20);
/*TODO*///	#if defined(DIRECT_FIXPOINT)
/*TODO*///			pVoice.noiseReg.l = (pVoice.noiseReg.l << 1) |
/*TODO*///				(((pVoice.noiseReg.l >> 22) ^ (pVoice.noiseReg.l >> 17)) & 1);
/*TODO*///	#else
			pVoice.noiseReg = (pVoice.noiseReg << 1) |
				(((pVoice.noiseReg >> 22) ^ (pVoice.noiseReg >> 17)) & 1);
/*TODO*///	#endif
		}
/*TODO*///	#if defined(DIRECT_FIXPOINT) && defined(LARGE_NOISE_TABLE)
/*TODO*///		pVoice.noiseOutput = (noiseTableLSB[pVoice.noiseReg.w[LO]]
/*TODO*///							   |noiseTableMSB[pVoice.noiseReg.w[HI]&0xff]);
/*TODO*///	#elif defined(DIRECT_FIXPOINT)
/*TODO*///		pVoice.noiseOutput = (noiseTableLSB[pVoice.noiseReg.b[LOLO]]
/*TODO*///							   |noiseTableMID[pVoice.noiseReg.b[LOHI]]
/*TODO*///							   |noiseTableMSB[pVoice.noiseReg.b[HILO]]);
/*TODO*///	#else
		pVoice.noiseOutput = (byte) (noiseTableLSB[pVoice.noiseReg&0xff]
							   |noiseTableMID[pVoice.noiseReg>>8&0xff]
							   |noiseTableMSB[pVoice.noiseReg>>16&0xff]);
/*TODO*///	#endif
	}
	
	
/*TODO*///	#if defined(DIRECT_FIXPOINT)
/*TODO*///	  #define triangle triangleTable[pVoice.waveStep.w[HI]]
/*TODO*///	  #define sawtooth sawtoothTable[pVoice.waveStep.w[HI]]
/*TODO*///        public static int square(sidOperator pVoice) { return squareTable[pVoice.waveStep.w[HI] + pVoice.pulseIndex]; }
/*TODO*///	  #define triSaw waveform30[pVoice.waveStep.w[HI]]
/*TODO*///	  #define triSquare waveform50[pVoice.waveStep.w[HI] + pVoice.SIDpulseWidth]
/*TODO*///	  #define sawSquare waveform60[pVoice.waveStep.w[HI] + pVoice.SIDpulseWidth]
/*TODO*///	  #define triSawSquare waveform70[pVoice.waveStep.w[HI] + pVoice.SIDpulseWidth]
/*TODO*///	#else
        public static int triangle(sidOperator pVoice) { return  triangleTable[pVoice.waveStep]; }
        public static int sawtooth(sidOperator pVoice) { return  sawtoothTable[pVoice.waveStep]; }
        public static int square(sidOperator pVoice) { return squareTable[pVoice.waveStep + pVoice.pulseIndex]; }
        public static int triSaw(sidOperator pVoice) { return waveform30[pVoice.waveStep]; }
        public static int triSquare(sidOperator pVoice) { return  waveform50[pVoice.waveStep + pVoice.SIDpulseWidth]; }
        public static int sawSquare(sidOperator pVoice) { return  waveform60[pVoice.waveStep + pVoice.SIDpulseWidth]; }
        public static int triSawSquare(sidOperator pVoice) { return  waveform70[pVoice.waveStep + pVoice.SIDpulseWidth]; }
/*TODO*///	#endif

        public static ptr2sidUwordFunc sidMode00 = new ptr2sidUwordFunc() {
            public int handler(sidOperator pVoice) {
		pVoice.output = (pVoice.filtIO-0x80);
		waveAdvance(pVoice);
                
                return 0xff;
            }
        };
	
/*TODO*///	#if 0
/*TODO*///	/* not used */
/*TODO*///	static void sidModeReal00(sidOperator* pVoice)  {
/*TODO*///		pVoice.output = 0;
/*TODO*///		waveAdvance(pVoice);
/*TODO*///	}
/*TODO*///	#endif
/*TODO*///	
        public static ptr2sidUwordFunc sidMode10 = new ptr2sidUwordFunc() {
            public int handler(sidOperator pVoice) {
                pVoice.output = triangle(pVoice);
                waveAdvance(pVoice);
                
                return 0xff;
              }
        };
	
	public static ptr2sidUwordFunc sidMode20 = new ptr2sidUwordFunc() {
            public int handler(sidOperator pVoice) {
                pVoice.output = sawtooth(pVoice);
                waveAdvance(pVoice);
                
                return 0xff;
              }
        };
	
        public static ptr2sidUwordFunc sidMode30 = new ptr2sidUwordFunc() {
            public int handler(sidOperator pVoice) {
                pVoice.output = triSaw(pVoice);
                waveAdvance(pVoice);
                
                return 0xff;
            }
        };
	  
	
        public static ptr2sidUwordFunc sidMode40 = new ptr2sidUwordFunc() {
            public int handler(sidOperator pVoice) {
                pVoice.output = square(pVoice);
                waveAdvance(pVoice);
                
                return 0xff;
            }
        };
        
	public static ptr2sidUwordFunc sidMode50 = new ptr2sidUwordFunc() {
            public int handler(sidOperator pVoice) {
                pVoice.output = triSquare(pVoice);
                waveAdvance(pVoice);
                
                return 0xff;
            }
        };
	
	public static ptr2sidUwordFunc sidMode60 = new ptr2sidUwordFunc() {
            public int handler(sidOperator pVoice) {
                pVoice.output = sawSquare(pVoice);
                waveAdvance(pVoice);
                
                return 0xff;
            }
        };
	
	public static ptr2sidUwordFunc sidMode70 = new ptr2sidUwordFunc() {
            public int handler(sidOperator pVoice) {
                pVoice.output = triSawSquare(pVoice);
                waveAdvance(pVoice);
              
                return 0xff;
            }
        };
	
        public static ptr2sidUwordFunc sidMode80 = new ptr2sidUwordFunc() {
            public int handler(sidOperator pVoice) {
                pVoice.output = pVoice.noiseOutput;
                waveAdvance(pVoice);
                noiseAdvance(pVoice);
                
                return 0xff;
              }
        };
	
	public static ptr2sidUwordFunc sidMode80hp = new ptr2sidUwordFunc() {
            public int handler(sidOperator pVoice) {
                pVoice.output = pVoice.noiseOutput;
                waveAdvance(pVoice);
                noiseAdvanceHp(pVoice);
                
                return 0xff;
            }
        };

	
        public static ptr2sidUwordFunc sidModeLock = new ptr2sidUwordFunc() {
            public int handler(sidOperator pVoice) {
		pVoice.noiseIsLocked = true;
		pVoice.output = (pVoice.filtIO-0x80);
		waveAdvance(pVoice);
                
                return 0xff;
            }
        };
	
	/* */
	/* */
	/* */
	
        public static ptr2sidUwordFunc sidMode14 = new ptr2sidUwordFunc() {
            public int handler(sidOperator pVoice) {
/*TODO*///	#if defined(DIRECT_FIXPOINT)
/*TODO*///	  if ( pVoice.modulator.waveStep.w[HI] < 2048 )
/*TODO*///	#else
                if ( pVoice.modulator.waveStep < 2048 )
/*TODO*///	#endif
                        pVoice.output = triangle(pVoice);
                else
                      pVoice.output = 0xFF ^ triangle(pVoice);
                waveAdvance(pVoice);
                
                return 0xff;
            }
        };
	
        public static ptr2sidUwordFunc sidMode34 = new ptr2sidUwordFunc() {
            public int handler(sidOperator pVoice) {
/*TODO*///	#if defined(DIRECT_FIXPOINT)
/*TODO*///	  if ( pVoice.modulator.waveStep.w[HI] < 2048 )
/*TODO*///	#else
                if ( pVoice.modulator.waveStep < 2048 )
/*TODO*///	#endif
                        pVoice.output = triSaw(pVoice);
                  else
                        pVoice.output = 0xFF ^ triSaw(pVoice);
                  waveAdvance(pVoice);
                  
                  return 0xff;
            }
        };
	
        public static ptr2sidUwordFunc sidMode54 = new ptr2sidUwordFunc() {
            public int handler(sidOperator pVoice) {
/*TODO*///	#if defined(DIRECT_FIXPOINT)
/*TODO*///	  if ( pVoice.modulator.waveStep.w[HI] < 2048 )
/*TODO*///	#else
                if ( pVoice.modulator.waveStep < 2048 )
/*TODO*///	#endif
                        pVoice.output = triSquare(pVoice);
                  else
                    pVoice.output = 0xFF ^ triSquare(pVoice);
                  waveAdvance(pVoice);
                  
                  return 0xff;
                }
        };
	
        public static ptr2sidUwordFunc sidMode74 = new ptr2sidUwordFunc() {
            public int handler(sidOperator pVoice) {
/*TODO*///	#if defined(DIRECT_FIXPOINT)
/*TODO*///	  if ( pVoice.modulator.waveStep.w[HI] < 2048 )
/*TODO*///	#else
                if ( pVoice.modulator.waveStep < 2048 )
/*TODO*///	#endif
                        pVoice.output = triSawSquare(pVoice);
                  else
                    pVoice.output = 0xFF ^ triSawSquare(pVoice);
                  waveAdvance(pVoice);
                  
                  return 0xff;
            }
        };
	
	/* */
	/* */
	/* */
	
	public static void waveCalcCycleLen(sidOperator pVoice)
	{
/*TODO*///	#if defined(DIRECT_FIXPOINT)
/*TODO*///		pVoice.cycleAddLen.w[HI] = 0;
/*TODO*///		pVoice.cycleAddLen.l += pVoice.cycleLen.l;
/*TODO*///		pVoice.cycleLenCount = pVoice.cycleAddLen.w[HI];
/*TODO*///	#else
		pVoice.cycleAddLenPnt += pVoice.cycleLenPnt;
		pVoice.cycleLenCount = pVoice.cycleLen;
		if ( pVoice.cycleAddLenPnt > 65535 ) pVoice.cycleLenCount++;
		pVoice.cycleAddLenPnt &= 0xFFFF;
/*TODO*///	#endif
		/* If we keep the value cycleLen between 1 <= x <= 65535, */
		/* the following check is not required. */
	/*	if ( pVoice.cycleLenCount == 0 ) */
	/*	{ */
	/*#if defined(DIRECT_FIXPOINT) */
	/*		pVoice.waveStep.l = 0; */
	/*#else */
	/*		pVoice.waveStep = (pVoice.waveStepPnt = 0); */
	/*#endif */
	/*		pVoice.cycleLenCount = 0; */
	/*	} */
	/*	else */
		{
/*TODO*///	#if defined(DIRECT_FIXPOINT)
/*TODO*///			register uword diff = pVoice.cycleLenCount - pVoice.cycleLen.w[HI];
/*TODO*///	#else
			int diff = pVoice.cycleLenCount - pVoice.cycleLen;
/*TODO*///	#endif
			if ( pVoice.wavePre[diff].len != pVoice.cycleLenCount )
			{
				pVoice.wavePre[diff].len = pVoice.cycleLenCount;
/*TODO*///	#if defined(DIRECT_FIXPOINT)
/*TODO*///				pVoice.wavePre[diff].stp = (pVoice.waveStepAdd.l = (4096UL*65536UL) / pVoice.cycleLenCount);
/*TODO*///	#else
				//pVoice.wavePre[diff].stp = (pVoice.waveStepAdd = 4096UL / pVoice.cycleLenCount);
                                pVoice.wavePre[diff].stp = (pVoice.waveStepAdd = 4096 / pVoice.cycleLenCount);
				//pVoice.wavePre[diff].pnt = (pVoice.waveStepAddPnt = ((4096UL % pVoice.cycleLenCount) * 65536UL) / pVoice.cycleLenCount);
                                pVoice.wavePre[diff].pnt = (pVoice.waveStepAddPnt = ((4096 % pVoice.cycleLenCount) * 65536) / pVoice.cycleLenCount);
/*TODO*///	#endif
			}
			else
			{
/*TODO*///	#if defined(DIRECT_FIXPOINT)
/*TODO*///				pVoice.waveStepAdd.l = pVoice.wavePre[diff].stp;
/*TODO*///	#else
				pVoice.waveStepAdd = pVoice.wavePre[diff].stp;
				pVoice.waveStepAddPnt = pVoice.wavePre[diff].pnt;
/*TODO*///	#endif
			}
		}  /* see above (opening bracket) */
	}
	
	public static void waveCalcFilter(sidOperator pVoice)
	{
		if ( pVoice.filtEnabled )
		{
			if ( pVoice.sid.filter.Type != 0 )
			{
				if ( pVoice.sid.filter.Type == 0x20 )
				{
					float tmp;
					pVoice.filtLow += ( pVoice.filtRef * pVoice.sid.filter.Dy );
					tmp = (float)pVoice.filtIO - pVoice.filtLow;
					tmp -= pVoice.filtRef * pVoice.sid.filter.ResDy;
					pVoice.filtRef += ( tmp * (pVoice.sid.filter.Dy) );
					pVoice.filtIO = (byte)(pVoice.filtRef-pVoice.filtLow/4);
				}
				else if (pVoice.sid.filter.Type == 0x40)
				{
					float tmp, tmp2;
					pVoice.filtLow += ( pVoice.filtRef * pVoice.sid.filter.Dy * 0.1 );
					tmp = (float)pVoice.filtIO - pVoice.filtLow;
					tmp -= pVoice.filtRef * pVoice.sid.filter.ResDy;
					pVoice.filtRef += ( tmp * (pVoice.sid.filter.Dy) );
					tmp2 = pVoice.filtRef - pVoice.filtIO/8;
					if (tmp2 < -128)
						tmp2 = -128;
					if (tmp2 > 127)
						tmp2 = 127;
					pVoice.filtIO = (byte)tmp2;
				}
				else
				{
					float sample, sample2;
					int tmp;
					pVoice.filtLow += ( pVoice.filtRef * pVoice.sid.filter.Dy );
					sample = pVoice.filtIO;
					sample2 = sample - pVoice.filtLow;
					tmp = (int)sample2;
					sample2 -= pVoice.filtRef * pVoice.sid.filter.ResDy;
					pVoice.filtRef += ( sample2 * pVoice.sid.filter.Dy );
	
					if ( pVoice.sid.filter.Type == 0x10 )
					{
						pVoice.filtIO = (byte)pVoice.filtLow;
					}
					else if ( pVoice.sid.filter.Type == 0x30 )
					{
						pVoice.filtIO = (byte)pVoice.filtLow;
					}
					else if ( pVoice.sid.filter.Type == 0x50 )
					{
						pVoice.filtIO = (byte)(sample - (tmp >> 1));
					}
					else if ( pVoice.sid.filter.Type == 0x60 )
					{
						pVoice.filtIO = (byte)tmp;
					}
					else if ( pVoice.sid.filter.Type == 0x70 )
					{
						pVoice.filtIO = (byte)(sample - (tmp >> 1));
					}
				}
			}
			else /* pVoice.sid.filter.Type == 0x00 */
			{
				pVoice.filtIO = 0;
			}
		}
	}
	
	public static ptr2sidUwordFunc waveCalcMute = new ptr2sidUwordFunc() {
            public int handler(sidOperator pVoice) {
                pVoice.ADSRproc.handler(pVoice);  /* just process envelope */
		return pVoice.filtIO;//&pVoice.outputMask;
            }
        };
	
        public static ptr2sidUwordFunc sidWaveCalcNormal = new ptr2sidUwordFunc() {
            public int handler(sidOperator pVoice) {
		if ( pVoice.cycleLenCount <= 0 )
		{
			waveCalcCycleLen(pVoice);
			if (( pVoice.SIDctrl & 0x40 ) == 0x40 )
			{
				pVoice.pulseIndex = pVoice.newPulseIndex;
				if ( pVoice.pulseIndex > 2048 )
				{
/*TODO*///	#if defined(DIRECT_FIXPOINT)
/*TODO*///					pVoice.waveStep.w[HI] = 0;
/*TODO*///	#else
					pVoice.waveStep = 0;
/*TODO*///	#endif
				}
			}
		}
		pVoice.waveProc.handler(pVoice);
                
		pVoice.filtIO = ampMod1x8[(pVoice.ADSRproc.handler(pVoice)|pVoice.output)&0xffff];
	//	pVoice.filtIO = pVoice.sid.masterVolume; // test for digi sound
		waveCalcFilter(pVoice);
		return pVoice.filtIO;//&pVoice.outputMask;
            }
        };
	
	
	public static ptr2sidUwordFunc waveCalcRangeCheck = new ptr2sidUwordFunc() {
            public int handler(sidOperator pVoice) {
/*TODO*///	#if defined(DIRECT_FIXPOINT)
/*TODO*///		pVoice.waveStepOld = pVoice.waveStep.w[HI];
/*TODO*///		(*pVoice.waveProc)(pVoice);
/*TODO*///		if (pVoice.waveStep.w[HI] < pVoice.waveStepOld)
/*TODO*///	#else
		pVoice.waveStepOld = pVoice.waveStep;
		pVoice.waveProc.handler(pVoice);
		if (pVoice.waveStep < pVoice.waveStepOld)
/*TODO*///	#endif
		{
			/* Next step switch back to normal calculation. */
			pVoice.cycleLenCount = 0;
			pVoice.outProc = sidWaveCalcNormal;
/*TODO*///	#if defined(DIRECT_FIXPOINT)
/*TODO*///					pVoice.waveStep.w[HI] = 4095;
/*TODO*///	#else
					pVoice.waveStep = 4095;
/*TODO*///	#endif
		}
		pVoice.filtIO = ampMod1x8[(pVoice.ADSRproc.handler(pVoice)|pVoice.output)&0xffff];
		waveCalcFilter(pVoice);
		return pVoice.filtIO;//&pVoice.outputMask;
            }
        };

	/* -------------------------------------------------- Operator frame set-up 1 */
	
	public static void sidEmuSet(sidOperator pVoice)
	{
            //System.out.println("sidEmuSet NOT IMPLEMENTED!!!!");
	    byte enveTemp, newWave, oldWave;
	    byte ADtemp;
	    byte SRtemp;
	    byte tmpSusVol;
	    
	    pVoice.SIDfreq = pVoice.reg[0]|(pVoice.reg[1]<<8);
	    
	    pVoice.SIDpulseWidth = (pVoice.reg[2]|(pVoice.reg[3]<<8)) & 0x0FFF;
	    pVoice.newPulseIndex = 4096 - pVoice.SIDpulseWidth;
/*TODO*///	#if defined(DIRECT_FIXPOINT)
/*TODO*///	    if ( ((pVoice.waveStep.w[HI] + pVoice.pulseIndex) >= 0x1000)
/*TODO*///		 && ((pVoice.waveStep.w[HI] + pVoice.newPulseIndex) >= 0x1000) )
/*TODO*///	    {
/*TODO*///		pVoice.pulseIndex = pVoice.newPulseIndex;
/*TODO*///	    }
/*TODO*///	    else if ( ((pVoice.waveStep.w[HI] + pVoice.pulseIndex) < 0x1000)
/*TODO*///		      && ((pVoice.waveStep.w[HI] + pVoice.newPulseIndex) < 0x1000) )
/*TODO*///	    {
/*TODO*///		pVoice.pulseIndex = pVoice.newPulseIndex;
/*TODO*///	    }
/*TODO*///	#else
	    if ( ((pVoice.waveStep + pVoice.pulseIndex) >= 0x1000)
		 && ((pVoice.waveStep + pVoice.newPulseIndex) >= 0x1000) )
	    {
		pVoice.pulseIndex = pVoice.newPulseIndex;
	    }
	    else if ( ((pVoice.waveStep + pVoice.pulseIndex) < 0x1000)
		      && ((pVoice.waveStep + pVoice.newPulseIndex) < 0x1000) )
	    {
		pVoice.pulseIndex = pVoice.newPulseIndex;
	    }
/*TODO*///	#endif
	
	
	    oldWave = pVoice.SIDctrl;
	    enveTemp = pVoice.ADSRctrl;
	    pVoice.SIDctrl = (newWave = (byte) (pVoice.reg[4]|(pVoice.reg[5]<<8)));
	    
	    if (( newWave & 1 ) ==0 )
	    {
		if (( oldWave & 1 ) !=0 )
		    enveTemp = ENVE_STARTRELEASE;
	/*		else if ( pVoice.gateOnCtrl ) */
	/*		{ */
	/*			enveTemp = ENVE_STARTSHORTATTACK; */
	/*		} */
	    }
	    else if ( /*pVoice.gateOffCtrl || */((oldWave&1)==0) )
	    {
		enveTemp = ENVE_STARTATTACK;
	    }
	    
	    if ((( oldWave ^ newWave ) & 0xF0 ) != 0 )
	    {
		pVoice.cycleLenCount = 0;
	    }
	    
	    ADtemp = (byte) (pVoice.reg[5]&0xff);
	    SRtemp = (byte) (pVoice.reg[6]&0xff);
	    if ( pVoice.SIDAD != ADtemp )
	    {
		enveTemp |= ENVE_ALTER;
	    }
	    else if ( pVoice.SIDSR != SRtemp )
	    {
		enveTemp |= ENVE_ALTER;
	    }
	    pVoice.SIDAD = ADtemp;
	    pVoice.SIDSR = SRtemp;
	    tmpSusVol = masterVolumeLevels[(SRtemp >> 4)&0xf];
	    if (pVoice.ADSRctrl != ENVE_SUSTAIN)  /* !!! */
	    {
		pVoice.enveSusVol = tmpSusVol;
	    }
	    else
	    {
		if ( pVoice.enveSusVol > pVoice.enveVol )
		    pVoice.enveSusVol = 0;
		else
		    pVoice.enveSusVol = tmpSusVol;
	    }
	    
	    pVoice.ADSRproc = enveModeTable[enveTemp>>1];  /* shifting out the KEY-bit */
	    pVoice.ADSRctrl = (byte) (enveTemp & (255-ENVE_ALTER-1));
	    
	    pVoice.filtEnabled = pVoice.sid.filter.Enabled &&
	        ((pVoice.sid.reg[0x17]&pVoice.filtVoiceMask)!=0);
	}
	
	/* -------------------------------------------------- Operator frame set-up 2 */
	
	/* MOS-8580, MOS-6581 (no 70) */
	public static ptr2sidUwordFunc sidModeNormalTable[] =
	{
	  sidMode00, sidMode10, sidMode20, sidMode30, sidMode40, sidMode50, sidMode60, sidMode70,
	  sidMode80, sidModeLock, sidModeLock, sidModeLock, sidModeLock, sidModeLock, sidModeLock, sidModeLock
	};
	
	/* MOS-8580, MOS-6581 (no 74) */
	static ptr2sidUwordFunc sidModeRingTable[] =
	{
	  sidMode00, sidMode14, sidMode00, sidMode34, sidMode00, sidMode54, sidMode00, sidMode74,
	  sidModeLock, sidModeLock, sidModeLock, sidModeLock, sidModeLock, sidModeLock, sidModeLock, sidModeLock
	};
	
	public static void sidClearOperator( sidOperator pVoice )
	{
            //System.out.println("sidClearOperator NOT IMPLEMENTED!!!!");
		pVoice.SIDfreq = 0;
		pVoice.SIDctrl = 0;
		pVoice.SIDAD = 0;
		pVoice.SIDSR = 0;
	
		pVoice.sync = false;
	
		pVoice.pulseIndex = (pVoice.newPulseIndex = (pVoice.SIDpulseWidth = 0));
		pVoice.curSIDfreq = (pVoice.curNoiseFreq = 0);
	
		pVoice.output = (pVoice.noiseOutput = 0);
		pVoice.filtIO = 0;
	
		pVoice.filtEnabled = false;
		pVoice.filtLow = (pVoice.filtRef = 0);
	
		pVoice.cycleLenCount = 0;
/*TODO*///	#if defined(DIRECT_FIXPOINT)
/*TODO*///		pVoice.cycleLen.l = (pVoice.cycleAddLen.l = 0);
/*TODO*///	#else
		pVoice.cycleLen = (pVoice.cycleLenPnt = 0);
		pVoice.cycleAddLenPnt = 0;
/*TODO*///	#endif
	
		pVoice.outProc = waveCalcMute;
	
/*TODO*///	#if defined(DIRECT_FIXPOINT)
/*TODO*///		pVoice.waveStepAdd.l = (pVoice.waveStep.l = 0);
/*TODO*///		pVoice.wavePre[0].len = (pVoice.wavePre[0].stp = 0);
/*TODO*///		pVoice.wavePre[1].len = (pVoice.wavePre[1].stp = 0);
/*TODO*///	#else
		pVoice.waveStepAdd = (pVoice.waveStepAddPnt = 0);
		pVoice.waveStep = (pVoice.waveStepPnt = 0);
		pVoice.wavePre[0].len = 0;
		pVoice.wavePre[0].stp = (pVoice.wavePre[0].pnt = 0);
		pVoice.wavePre[1].len = 0;
		pVoice.wavePre[1].stp = (pVoice.wavePre[1].pnt = 0);
/*TODO*///	#endif
		pVoice.waveStepOld = 0;
	
/*TODO*///	#if defined(DIRECT_FIXPOINT)
/*TODO*///		pVoice.noiseReg.l = noiseSeed;
/*TODO*///	#else
		pVoice.noiseReg = noiseSeed;
/*TODO*///	#endif
		pVoice.noiseStepAdd = (pVoice.noiseStep = 0);
		pVoice.noiseIsLocked = false;
	}

	public static void sidEmuSet2(sidOperator pVoice)
	{
            //System.out.println("sidEmuSet2 NOT IMPLEMENTED!!!!");
	    pVoice.outProc = sidWaveCalcNormal;
	    pVoice.sync = false;
	    
	    if ( (pVoice.SIDfreq < 16) || ((pVoice.SIDctrl & 8) != 0) )
	//    if ( /*(pVoice.SIDfreq < 16) || */((pVoice.SIDctrl & 8) != 0) )
	    {
		pVoice.outProc = waveCalcMute;
		if (pVoice.SIDfreq == 0)
		{
/*TODO*///	#if defined(DIRECT_FIXPOINT)
/*TODO*///		    pVoice.cycleLen.l = (pVoice.cycleAddLen.l = 0);
/*TODO*///		    pVoice.waveStep.l = 0;
/*TODO*///	#else
		    pVoice.cycleLen = (pVoice.cycleLenPnt = 0);
		    pVoice.cycleAddLenPnt = 0;
		    pVoice.waveStep = 0;
		    pVoice.waveStepPnt = 0;
/*TODO*///	#endif
		    pVoice.curSIDfreq = (pVoice.curNoiseFreq = 0);
		    pVoice.noiseStepAdd = 0;
		    pVoice.cycleLenCount = 0;
		}
		if ((pVoice.SIDctrl & 8) != 0)
		{
		    if (pVoice.noiseIsLocked)
		    {
			pVoice.noiseIsLocked = false;
/*TODO*///	#if defined(DIRECT_FIXPOINT)
/*TODO*///			pVoice.noiseReg.l = noiseSeed;
/*TODO*///	#else
			pVoice.noiseReg = noiseSeed;
/*TODO*///	#endif
		    }
		}
	    }
	    else
	    {
		if ( pVoice.curSIDfreq != pVoice.SIDfreq )
		{
		    pVoice.curSIDfreq = pVoice.SIDfreq;
		    /* We keep the value cycleLen between 1 <= x <= 65535. */
		    /* This makes a range-check in waveCalcCycleLen() unrequired. */
/*TODO*///	#if defined(DIRECT_FIXPOINT)
/*TODO*///		    pVoice.cycleLen.l = ((pVoice.sid.PCMsid << 12) / pVoice.SIDfreq) << 4;
/*TODO*///		    if (pVoice.cycleLenCount > 0)
/*TODO*///		    {
/*TODO*///			waveCalcCycleLen(pVoice);
/*TODO*///			pVoice.outProc = &waveCalcRangeCheck;
/*TODO*///		    }
/*TODO*///	#else
		    pVoice.cycleLen = pVoice.sid.PCMsid / pVoice.SIDfreq;
		    //pVoice.cycleLenPnt = (( pVoice.sid.PCMsid % pVoice.SIDfreq ) * 65536UL ) / pVoice.SIDfreq;
                    pVoice.cycleLenPnt = (( pVoice.sid.PCMsid % pVoice.SIDfreq ) * 65536 ) / pVoice.SIDfreq;
		    if (pVoice.cycleLenCount > 0)
		    {
			waveCalcCycleLen(pVoice);
			pVoice.outProc = waveCalcRangeCheck;
		    }
/*TODO*///	#endif
		}
		
		if ((( pVoice.SIDctrl & 0x80 ) == 0x80 ) && ( pVoice.curNoiseFreq != pVoice.SIDfreq ))
		{
		    pVoice.curNoiseFreq = pVoice.SIDfreq;
		    pVoice.noiseStepAdd = (pVoice.sid.PCMsidNoise * pVoice.SIDfreq) >> 8;
		    if (pVoice.noiseStepAdd >= (1L<<21))
			sidModeNormalTable[8] = sidMode80hp;
		    else
			sidModeNormalTable[8] = sidMode80;
		}
		
		if (( pVoice.SIDctrl & 2 ) != 0 )
		{
                    
		    if ( pVoice.modulator!=null && (( pVoice.modulator.SIDfreq == 0 ) || (( pVoice.modulator.SIDctrl & 8 ) != 0 )) )
		    {
			;
		    }
		    else if ((pVoice.modulator!=null)&&(pVoice.carrier!=null)&& (( pVoice.carrier.SIDctrl & 2 ) != 0 ) &&
			      ( pVoice.modulator.SIDfreq >= ( pVoice.SIDfreq << 1 )) )
		    {
			;
		    }
		    else
		    {
			pVoice.sync = true;
		    }
		}
		
		if ((pVoice.modulator!=null)&&(( pVoice.SIDctrl & 0x14 ) == 0x14 ) && ( pVoice.modulator.SIDfreq != 0 ))
		    pVoice.waveProc = sidModeRingTable[(pVoice.SIDctrl >> 4)&0xf];
		else
		    pVoice.waveProc = sidModeNormalTable[(pVoice.SIDctrl >> 4)&0xf];
	    }
	}
	
	public static void sidInitWaveformTables(int type)
	{
            //System.out.println("sidInitWaveformTables NOT IMPLEMENTED!!!!");
		int i,j;
		int k;
	
		k = 0;
		for ( i = 0; i < 256; i++ )
			for ( j = 0; j < 8; j++ )
				triangleTable[k++] = (byte) i;
		for ( i = 255; i >= 0; i-- )
			for ( j = 0; j < 8; j++ )
				triangleTable[k++] = (byte) i;
	
		k = 0;
		for ( i = 0; i < 256; i++ )
			for ( j = 0; j < 16; j++ )
				sawtoothTable[k++] = (byte) i;
	
		k = 0;
		for ( i = 0; i < 4096; i++ )
		    squareTable[k++] = (byte) 255; //0; my estimation; especial for digi sound
		for ( i = 0; i < 4096; i++ )
		    squareTable[k++] = 0; //255;
	
		if ( type==MOS8580 )
		{
                    
                    for (int _i=0 ; _i<4096 ; _i++)
                        waveform30[_i] = waveform30_8580[_i];
	        
                    for (int _i=0 ; _i<(waveform50_8580.length) ; _i++)
                        waveform50[_i] = waveform50_8580[_i];
	        
                    for (int _i=0 ; _i<(waveform60_8580.length) ; _i++)
                        waveform60[_i] = waveform60_8580[_i];
	        
                    for (int _i=0 ; _i<(waveform70_8580.length) ; _i++)
                        waveform70[_i] = waveform70_8580[_i];
                    
		}
		else
		{
	        
                    for (int _i=0 ; _i<4096 ; _i++)
                        waveform30[_i] = waveform30_6581[_i];
	        
                    for (int _i=0 ; _i<(waveform50_6581.length) ; _i++)
                        waveform50[_i] = waveform50_6581[_i];
	        
                    for (int _i=0 ; _i<(waveform60_6581.length) ; _i++)
                        waveform60[_i] = waveform60_6581[_i];
	        
                    for (int _i=0 ; _i<(waveform70_6581.length) ; _i++)
                        waveform70[_i] = waveform70_6581[_i];  /* really audible? */
		}
                
		for ( i = 4096; i < 8192; i++ )
		{
			waveform50[i] = 0;
			waveform60[i] = 0;
			waveform70[i] = 0;
		}
	
		if ( type==MOS8580 )
		{
			sidModeNormalTable[3] = sidMode30;
			sidModeNormalTable[6] = sidMode60;
			sidModeNormalTable[7] = sidMode70;
			sidModeRingTable[7] = sidMode74;
		}
		else
		{
			sidModeNormalTable[3] = sidMode30;
			sidModeNormalTable[6] = sidMode60;
			sidModeNormalTable[7] = sidMode00;  /* really audible? */
			sidModeRingTable[7] = sidMode00;    /* */
		}
	
		{
/*TODO*///	#if defined(LARGE_NOISE_TABLE)
/*TODO*///		udword ni;
/*TODO*///		for (ni = 0; ni < sizeof(noiseTableLSB); ni++)
/*TODO*///		{
/*TODO*///			noiseTableLSB[ni] = (ubyte)
/*TODO*///				(((ni >> (13-4)) & 0x10) |
/*TODO*///				 ((ni >> (11-3)) & 0x08) |
/*TODO*///				 ((ni >> (7-2)) & 0x04) |
/*TODO*///				 ((ni >> (4-1)) & 0x02) |
/*TODO*///				 ((ni >> (2-0)) & 0x01));
/*TODO*///		}
/*TODO*///		for (ni = 0; ni < sizeof(noiseTableMSB); ni++)
/*TODO*///		{
/*TODO*///			noiseTableMSB[ni] = (ubyte)
/*TODO*///				(((ni << (7-(22-16))) & 0x80) |
/*TODO*///				 ((ni << (6-(20-16))) & 0x40) |
/*TODO*///				 ((ni << (5-(16-16))) & 0x20));
/*TODO*///		}
/*TODO*///	#else
		int ni;
		for (ni = 0; ni < noiseTableLSB.length; ni++)
		{
			noiseTableLSB[ni] = (byte)
				(((ni >> (7-2)) & 0x04) |
				 ((ni >> (4-1)) & 0x02) |
				 ((ni >> (2-0)) & 0x01));
		}
		for (ni = 0; ni < noiseTableMID.length; ni++)
		{
			noiseTableMID[ni] = (byte)
				(((ni >> (13-8-4)) & 0x10) |
				 ((ni << (3-(11-8))) & 0x08));
		}
		for (ni = 0; ni < noiseTableMSB.length; ni++)
		{
			noiseTableMSB[ni] = (byte)
				(((ni << (7-(22-16))) & 0x80) |
				 ((ni << (6-(20-16))) & 0x40) |
				 ((ni << (5-(16-16))) & 0x20));
		}
/*TODO*///	#endif
		}
	}
    
}
