/***************************************************************************

	Konami 051649 - SCC1 sound as used in Haunted Castle, City Bomber

	This file is pieced together by Bryan McPhail from a combination of
	Namco Sound, Amuse by Cab, Haunted Castle schematics and whoever first
	figured out SCC!

	The 051649 is a 5 channel sound generator, each channel gets it's
	waveform from RAM (32 bytes per waveform, 8 bit signed data).

	This sound chip is the same as the sound chip in some Konami
	megaROM cartridges for the MSX. It is actually well researched
	and documented:

		http://www.msxnet.org/tech/scc.html

	Thanks to Sean Young (sean@msxnet.org) for some bugfixes.

	K052539 is equivalent to this chip except channel 5 does not share
	waveforms with channel 4.

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */

package mame056.sound;

import static arcadeflex056.fileio.*;
import static arcadeflex056.fucPtr.*;
import static common.ptr.*;
import static consoleflex056.funcPtr.*;
import static mame056.sndintrf.*;
import static mame056.sndintrfH.*;
import static mame056.usrintrf.ui_text;
import static mame056.sound.streams.*;
import static mame056.timerH.*;
import static mame056.timer.*;
import static mess056.messH.*;
import static arcadeflex056.osdepend.logerror;
import static mame056.mame.Machine;
import static mame056.sound.k051649H.*;
import static common.libc.cstring.*;
import static common.libc.cstdio.*;
import common.subArrays.IntArray;
import static common.util.*;
import static mame056.common.*;
import static mame056.mame.*;


public class k051649  extends snd_interface {
    
    public k051649() {
        this.sound_num = SOUND_K051649;
        this.name = "K051649";
    }

    @Override
    public int chips_num(MachineSound msound) {
        return 1;
    }

    @Override
    public int chips_clock(MachineSound msound) {
        return ((k051649_interface) msound.sound_interface).master_clock;
    }

    @Override
    public int start(MachineSound msound) {
        return K051649_sh_start(msound);
    }

    @Override
    public void stop() {
        K051649_sh_stop();
    }

    @Override
    public void update() {
        //NO FUNCTIONAL CODE IS NECCESARY
    }

    @Override
    public void reset() {
        //NO FUNCTIONAL CODE IS NECCESARY
    }
    
    public static final int FREQBASEBITS	= 16;
	
    /* this structure defines the parameters for a channel */
    public static class k051649_sound_channel
    {
            public long counter;
            public int frequency;
            public int volume;
            public int key;
            public char[] waveform=new char[32];		/* 19991207.CAB */
    };

    static k051649_sound_channel[] channel_list = new k051649_sound_channel[5];
    
    static {
        for (int _i=0 ; _i<5 ; _i++){
           k051649_sound_channel _obj = new k051649_sound_channel();
           for ( int _j=0 ; _j<32 ; _j++){
              _obj.waveform[_j] = '0';
           }
           channel_list[_i] = _obj;
        }
    }
	
	/* global sound parameters */
	static int stream,mclock,rate;
	
	/* mixer tables and internal buffers */
	static IntArray mixer_table;
	static IntArray mixer_lookup;
	static ShortPtr mixer_buffer;
	
	/* build a table to divide by the number of voices */
	public static int make_mixer_table(int voices)
	{
		int count = voices * 256;
		int i;
		int gain = 8;
	
		/* allocate memory */
		mixer_table = new IntArray(512 * voices);
		if (mixer_table == null)
			return 1;
	
		/* find the middle of the table */
		mixer_lookup = new IntArray(mixer_table, (256 * voices));
	
		/* fill in the table - 16 bit case */
		for (i = 0; i < count; i++)
		{
			int val = i * gain * 16 / voices;
			if (val > 32767) val = 32767;
			mixer_lookup.write( i, val );
			mixer_lookup.write(-i, -val);
		}
	
		return 0;
	}
	
	
	/* generate sound to the mix buffer */
	static StreamInitPtr K051649_update = new StreamInitPtr() {            
            public void handler(int ch, ShortPtr buffer, int length) {
                k051649_sound_channel[] voice=channel_list;
                
		ShortPtr mix;
		int i,v,f,j,k;
	
		/* zap the contents of the mixer buffer */
		mixer_buffer = new ShortPtr( length * 2 );
	
		for (j=0; j<5; j++) {
			v=voice[j].volume;
			f=voice[j].frequency;
			k=voice[j].key;
			if (v!=0 && f!=0 && k!=0)
			{
				char[] w = voice[j].waveform;			/* 19991207.CAB */
				int c=(int) voice[j].counter;
	
				mix = new ShortPtr(mixer_buffer);
	
				/* add our contribution */
				for (i = 0; i < length; i++)
				{
					int offs;
	
					/* Amuse source:  Cab suggests this method gives greater resolution */
					/* Sean Young 20010417: the formula is really: f = clock/(16*(f+1))*/
					c+=(long)((((float)mclock / (float)((f+1) * 16))*(float)(1<<FREQBASEBITS)) / (float)(rate / 32));
					offs = (c >> 16) & 0x1f;
					mix.writeinc((short) (mix.read()+ (w[offs] * v)>>3));
				}
	
				/* update the counter for this voice */
				voice[j].counter = c;
			}
		}
	
		/* mix it down */
		mix = new ShortPtr(mixer_buffer);
		for (i = 0; i < length/2; i++)
			buffer.writeinc((short) mixer_lookup.read(mix.readinc()));
            }
        };
	

	public static int K051649_sh_start(MachineSound msound)
	{
		String snd_name = "K051649";
		k051649_sound_channel[] voice=channel_list;
		k051649_interface intf = (k051649_interface) msound.sound_interface;
		int i;
	
		/* get stream channels */
		stream = stream_init(snd_name, intf.volume, Machine.sample_rate, 0, K051649_update);
		mclock = intf.master_clock;
		rate = Machine.sample_rate;
	
		/* allocate a buffer to mix into - 1 second's worth should be more than enough */
		if ((mixer_buffer = new ShortPtr(2 * Machine.sample_rate)) == null)
			return 1;
	
		/* build the mixer table */
		if (make_mixer_table(5) != 0)
		{
			mixer_buffer = null;
			return 1;
		}
	
		/* reset all the voices */
		for (i=0; i>5; i++) {
			voice[i].frequency = 0;
			voice[i].volume = 0;
			voice[i].counter = 0;
		}
	
		return 0;
	}
	
	public static void K051649_sh_stop()
	{
		mixer_table = null;
		mixer_buffer = null;
	}
	
	/********************************************************************************/
	
	public static WriteHandlerPtr K051649_waveform_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		stream_update(stream,0);
		channel_list[offset>>5].waveform[offset&0x1f]=(char) data;
		/* SY 20001114: Channel 5 shares the waveform with channel 4 */
	    if (offset >= 0x60)
			channel_list[4].waveform[offset&0x1f]=(char) data;
	} };
	
	/* SY 20001114: Channel 5 doesn't share the waveform with channel 4 on this chip */
	public static WriteHandlerPtr K052539_waveform_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		stream_update(stream,0);
		channel_list[offset>>5].waveform[offset&0x1f]=(char) data;
	} };
	
	public static WriteHandlerPtr K051649_volume_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		stream_update(stream,0);
		channel_list[offset&0x7].volume=data&0xf;
	} };
        
        static int[] f = new int[10];
	
	public static WriteHandlerPtr K051649_frequency_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		
		f[offset]=data;
	
		stream_update(stream,0);
		channel_list[offset>>1].frequency=(f[offset&0xe] + (f[offset|1]<<8))&0xfff;
	} };
	
	public static WriteHandlerPtr K051649_keyonoff_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		channel_list[0].key=data&1;
		channel_list[1].key=data&2;
		channel_list[2].key=data&4;
		channel_list[3].key=data&8;
		channel_list[4].key=data&16;
	} };
}
