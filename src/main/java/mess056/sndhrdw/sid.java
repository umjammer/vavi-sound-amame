/*
  copyright peter trauner 2000
  
  based on michael schwend's sid play

  Noise generation algorithm is used courtesy of Asger Alstrup Nielsen.
  His original publication can be found on the SID home page.

  Noise table optimization proposed by Phillip Wooller. The output of
  each table does not differ.

  MOS-8580 R5 combined waveforms recorded by Dennis "Deadman" Lindroos.
*/


/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.sndhrdw;

import static common.ptr.*;
import static common.subArrays.*;
import static java.lang.Math.exp;
import static java.lang.Math.log;
import static mame056.mame.options;
import static mame056.sound.streams.*;
import static mess056.sndhrdw.sid6581.*;
import static mess056.sndhrdw.sidH.*;
import static mess056.sndhrdw.sidenvel.*;
import static mess056.sndhrdw.sidvoice.*;
import static mess056.sndhrdw.sidvoiceH.*;

public class sid
{
	
/*TODO*///	#define VERBOSE_DBG 0

        public static float[] filterTable = new float[0x800];
        public static float[] bandPassParam = new float[0x800];
/*TODO*///	#define lowPassParam filterTable
	public static float[] filterResTable = new float[16];

	public static int maxLogicalVoices = 4;

	static int mix16monoMiddleIndex = 256*maxLogicalVoices/2;
	static int[] mix16mono = new int[256*maxLogicalVoices];

	static int zero16bit=0;  /* either signed or unsigned */
/*TODO*///	udword splitBufferLen;

	public static void MixerInit(boolean threeVoiceAmplify)
	{
		long si;
		int ui;
		long ampDiv = maxLogicalVoices;
	
		if (threeVoiceAmplify)
		{
			ampDiv = (maxLogicalVoices-1);
		}
	
		/* Mixing formulas are optimized by sample input value. */
	
		si = (-128*maxLogicalVoices) * 256;
		for (ui = 0; ui < mix16mono.length; ui++ )
		{
			mix16mono[ui] = (int) ((si/ampDiv) + zero16bit);
			si+=256;
		}
	
	}
	
	
	public static void syncEm(_SID6581 This)
	{
		boolean sync1 = (This.optr1.modulator.cycleLenCount <= 0);
		boolean sync2 = (This.optr2.modulator.cycleLenCount <= 0);
		boolean sync3 = (This.optr3.modulator.cycleLenCount <= 0);
	
		This.optr1.cycleLenCount--;
		This.optr2.cycleLenCount--;
		This.optr3.cycleLenCount--;
	
		if (This.optr1.sync && sync1)
		{
			This.optr1.cycleLenCount = 0;
			This.optr1.outProc = sidWaveCalcNormal;
/*TODO*///	#if defined(DIRECT_FIXPOINT)
/*TODO*///			optr1.waveStep.l = 0;
/*TODO*///	#else
			This.optr1.waveStep = (This.optr1.waveStepPnt = 0);
/*TODO*///	#endif
		}
		if (This.optr2.sync && sync2)
		{
			This.optr2.cycleLenCount = 0;
			This.optr2.outProc = sidWaveCalcNormal;
/*TODO*///	#if defined(DIRECT_FIXPOINT)
/*TODO*///			This.optr2.waveStep.l = 0;
/*TODO*///	#else
			This.optr2.waveStep = (This.optr2.waveStepPnt = 0);
/*TODO*///	#endif
		}
		if (This.optr3.sync && sync3)
		{
			This.optr3.cycleLenCount = 0;
			This.optr3.outProc = sidWaveCalcNormal;
/*TODO*///	#if defined(DIRECT_FIXPOINT)
/*TODO*///			optr3.waveStep.l = 0;
/*TODO*///	#else
			This.optr3.waveStep = (This.optr3.waveStepPnt = 0);
/*TODO*///	#endif
		}
	}
	
	
	public static void sidEmuFillBuffer(_SID6581 This, ShortPtr buffer, int bufferLen )
	{
	//void* fill16bitMono( SID6581 *This, void* buffer, udword numberOfSamples )
	    ShortPtr buffer16bit = new ShortPtr(buffer);
	    for ( ; bufferLen > 0; bufferLen-- )
	    {
		buffer16bit.write( mix16mono[(mix16monoMiddleIndex
						      +(This.optr1.outProc).handler(This.optr1)
						      +(This.optr2.outProc).handler(This.optr2)
						      +(This.optr3.outProc.handler(This.optr3)&This.optr3_outputmask)
	/* hack for digi sounds
	   does n't seam to come from a tone operator
	   ghostbusters and goldrunner everything except volume zeroed */
						      +(This.masterVolume<<2)
	//						  +(*sampleEmuRout)()
		    )&0x3ff]);
                buffer16bit.offset++;
		syncEm(This);
	    }
	}
	
	/* --------------------------------------------------------------------- Init */
	
	
	/* Reset. */
	
	public static boolean sidEmuReset(_SID6581 This)
	{
                if (This.optr1==null)
                    This.optr1=new sidOperator();
                if (This.optr2==null)
                    This.optr2=new sidOperator();
                if (This.optr3==null)
                    This.optr3=new sidOperator();
                
		sidClearOperator( This.optr1 );
		enveEmuResetOperator( This.optr1 );
		sidClearOperator( This.optr2 );
		enveEmuResetOperator( This.optr2 );
		sidClearOperator( This.optr3 );
		enveEmuResetOperator( This.optr3 );
		This.optr3_outputmask = ~0;  /* on */
	
	//	sampleEmuReset();
	
		This.filter.Type = (This.filter.CurType = 0);
		This.filter.Value = 0;
		This.filter.Dy = (This.filter.ResDy = 0);
	
		sidEmuSet( This.optr1 );
		sidEmuSet( This.optr2 );
		sidEmuSet( This.optr3 );
	
		sidEmuSet2( This.optr1 );
		sidEmuSet2( This.optr2 );
		sidEmuSet2( This.optr3 );
	
	
		return true;
	}
	
	
	public static void filterTableInit()
	{
		int uk;
		/* Parameter calculation has not been moved to a separate function */
		/* by purpose. */
		float filterRefFreq = 44100.0f;
	
	/*	extern filterfloat filterTable[0x800]; */
		float yMax = 1.0f;
		float yMin = 0.01f;
		float yAdd;
		float yTmp, rk, rk2;
	
		float resDyMax;
		float resDyMin;
		float resDy;
	
		uk = 0;
		for ( rk = 0; rk < 0x800; rk++ )
		{
			filterTable[uk] = (float) ((((exp(rk/0x800*log(400.0))/60.0)+0.05)
                                *filterRefFreq) / options.samplerate);
			if ( filterTable[uk] < yMin )
				filterTable[uk] = yMin;
			if ( filterTable[uk] > yMax )
				filterTable[uk] = yMax;
			uk++;
		}
	
		/*extern filterfloat bandPassParam[0x800]; */
		yMax = 0.22f;
		yMin = 0.05f;  /* less for some R1/R4 chips */
		yAdd = (yMax-yMin)/2048.0f;
		yTmp = yMin;
		uk = 0;
		/* Some C++ compilers still have non-local scope! */
		for ( rk2 = 0; rk2 < 0x800; rk2++ )
		{
			bandPassParam[uk] = (yTmp*filterRefFreq) / options.samplerate;
			yTmp += yAdd;
			uk++;
		}
	
		/*extern filterfloat filterResTable[16]; */
		resDyMax = 1.0f;
		resDyMin = 2.0f;
		resDy = resDyMin;
		for ( uk = 0; uk < 16; uk++ )
		{
			filterResTable[uk] = resDy;
			resDy -= (( resDyMin - resDyMax ) / 15 );
		}
		filterResTable[0] = resDyMin;
		filterResTable[15] = resDyMax;
	}
	
	public static void sid6581_init (_SID6581 This)
	{
                This.optr1 = new sidOperator();
                This.optr2 = new sidOperator();
                This.optr3 = new sidOperator();
                
		This.optr1.sid=This;
		This.optr2.sid=This;
		This.optr3.sid=This;
	
		This.optr1.modulator = This.optr3;
		This.optr3.carrier = This.optr1;
		This.optr1.filtVoiceMask = 1;
	
		This.optr2.modulator = This.optr1;
		This.optr1.carrier = This.optr2;
		This.optr2.filtVoiceMask = 2;
	
		This.optr3.modulator = This.optr2;
		This.optr2.carrier = This.optr3;
		This.optr3.filtVoiceMask = 4;
	
	
	
		This.PCMsid = (int) (This.PCMfreq * (16777216.0 / This.clock));
		This.PCMsidNoise = (int) ((This.clock*256.0)/This.PCMfreq);
	
		This.filter.Enabled = true;
	
		sidInitMixerEngine();
		filterTableInit();
	
		sidInitWaveformTables(This.type);
	
		enveEmuInit(This.PCMfreq, true);
	
		MixerInit(false);
	
		sidEmuReset(This);
	}
	
	public static void sid6581_port_w (int This, int offset, int data)
	{
/*TODO*///	    DBG_LOG (1, "sid6581 write", ("offset %.2x value %.2x\n", offset, data));
	    offset &= 0x1f;
	
	    switch (offset)
	    {
	    case 0x19: case 0x1a: case 0x1b: case 0x1c:
	    case 0x1d:
	    case 0x1e:
	    case 0x1f:
		break;
	    case 0x15: case 0x16: case 0x17: 
	    case 0x18:
		stream_update(_sid6581[This].mixer_channel,0);
		_sid6581[This].reg[offset] = data;
		_sid6581[This].masterVolume = ( _sid6581[This].reg[0x18] & 15 );
		_sid6581[This].masterVolumeAmplIndex = _sid6581[This].masterVolume << 8;	    
	
		if ((_sid6581[This].reg[0x18]&0x80)!=0
                        && ((_sid6581[This].reg[0x17]&_sid6581[This].optr3.filtVoiceMask)==0)
                        ){
		    _sid6581[This].optr3_outputmask = 0;     /* off */
		} else {
		    _sid6581[This].optr3_outputmask = ~0;  /* on */
		}
		_sid6581[This].filter.Type = _sid6581[This].reg[0x18] & 0x70;
		if (_sid6581[This].filter.Type != _sid6581[This].filter.CurType)
		{
		    _sid6581[This].filter.CurType = _sid6581[This].filter.Type;
		    _sid6581[This].optr1.filtLow = (_sid6581[This].optr1.filtRef = 0);
		    _sid6581[This].optr2.filtLow = (_sid6581[This].optr2.filtRef = 0);
		    _sid6581[This].optr3.filtLow = (_sid6581[This].optr3.filtRef = 0);
		}
		if ( _sid6581[This].filter.Enabled )
		{
		    _sid6581[This].filter.Value = 0x7ff & ( (_sid6581[This].reg[0x15]&7) | ( _sid6581[This].reg[0x16] << 3 ));
		    if (_sid6581[This].filter.Type == 0x20){
/*TODO*///			sid6581[This].filter.Dy = bandPassParam[sid6581[This].filter.Value];
                    } else {
/*TODO*///			sid6581[This].filter.Dy = lowPassParam[sid6581[This].filter.Value];
                    }
/*TODO*///		    sid6581[This].filter.ResDy = filterResTable[sid6581[This].reg[0x17] >> 4] - sid6581[This].filter.Dy;
		    if ( _sid6581[This].filter.ResDy < 1.0 )
			_sid6581[This].filter.ResDy = (float) 1.0;
		}
	
		sidEmuSet( _sid6581[This].optr1 );
		sidEmuSet( _sid6581[This].optr3 );	
		sidEmuSet( _sid6581[This].optr2 );
	
		// relies on sidEmuSet also for other channels!
		sidEmuSet2( _sid6581[This].optr1 );
		sidEmuSet2( _sid6581[This].optr2 );
		sidEmuSet2( _sid6581[This].optr3 );
	
		break;
	    default:
		stream_update(_sid6581[This].mixer_channel,0);
		_sid6581[This].reg[offset] = data;
		
		if (offset<7) {
		    _sid6581[This].optr1.reg[offset] = data;
		} else if (offset<14) {
		    _sid6581[This].optr2.reg[offset-7] = data;
		} else if (offset<21) {
		    _sid6581[This].optr3.reg[offset-14] = data;
		}
	
		sidEmuSet( _sid6581[This].optr1 );
		sidEmuSet( _sid6581[This].optr3 );	
		sidEmuSet( _sid6581[This].optr2 );
	
		// relies on sidEmuSet also for other channels!
		sidEmuSet2( _sid6581[This].optr1 );
		sidEmuSet2( _sid6581[This].optr2 );
		sidEmuSet2( _sid6581[This].optr3 );
	    }
	}
	
	public static int sid6581_port_r (int This, int offset)
	{
	    int data = 0;
	/* SIDPLAY reads last written at a sid address value */
	    offset &= 0x1f;
	    switch (offset)
	    {
	    case 0x1d:
	    case 0x1e:
	    case 0x1f:
		data=0xff;
		break;
	    case 0x19:						   /* paddle 1 */
		if (_sid6581[This].ad_read != null)
		    data=_sid6581[This].ad_read.handler(0);
		else
		    data=0;
		break;
	    case 0x1a:						   /* paddle 2 */
		if (_sid6581[This].ad_read != null)
		    data=_sid6581[This].ad_read.handler(1);
		else
		    data=0;
		break;
	    case 0x1b:
		stream_update(_sid6581[This].mixer_channel,0);
		data = _sid6581[This].optr3.output;
		break;
	    case 0x1c:
		stream_update(_sid6581[This].mixer_channel,0);
		data = _sid6581[This].optr3.enveVol;
		break;
	    default:
		data=_sid6581[This].reg[offset];
	    }
/*TODO*///	    DBG_LOG (1, "sid6581 read", ("offset %.2x value %.2x\n", offset, data));
	    return data;
	}
	
}
