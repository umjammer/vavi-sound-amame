/******************************************************************************
 PeT mess@utanet.at 2000,2001
******************************************************************************/
/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.sndhrdw;

import static arcadeflex056.fucPtr.*;
import static common.libc.cstdio.*;
import static common.ptr.*;
import static mame056.commonH.*;
import static mame056.mame.options;
import static mame056.sndintrf.*;
import static mame056.sndintrfH.*;
import static mame056.sound.mixerH.*;
import static mame056.sound.streams.*;
import static mess056.machine.lynx.*;

public class lynx
{
	
	
	/* accordingly to atari's reference manual
	   there were no stereo lynx produced (the manual knows only production until mid 1991)
	   the howard/developement board might have stereo support
	   the revised lynx 2 hardware might have stereo support at least at the stereo jacks
	
	   some games support stereo
	*/
	
	
	/*
	AUDIO_A	EQU $FD20
	AUDIO_B	EQU $FD28
	AUDIO_C	EQU $FD30
	AUDIO_D	EQU $FD38
	
	VOLUME_CNTRL	EQU 0
	FEEDBACK_ENABLE EQU 1	; enables 11/10/5..0
	OUTPUT_VALUE	EQU 2
	SHIFTER_L	EQU 3
	AUD_BAKUP	EQU 4
	AUD_CNTRL1	EQU 5
	AUD_COUNT	EQU 6
	AUD_CNTRL2	EQU 7
	
	; AUD_CNTRL1
	FEEDBACK_7	EQU %10000000
	AUD_RESETDONE	EQU %01000000
	AUD_INTEGRATE	EQU %00100000
	AUD_RELOAD	EQU %00010000
	AUD_CNTEN	EQU %00001000
	AUD_LINK	EQU %00000111	
	; link timers (0.2.4 / 1.3.5.7.Aud0.Aud1.Aud2.Aud3.1
	AUD_64us	EQU %00000110
	AUD_32us	EQU %00000101
	AUD_16us	EQU %00000100
	AUD_8us	EQU %00000011
	AUD_4us	EQU %00000010
	AUD_2us	EQU %00000001
	AUD_1us	EQU %00000000
	
	; AUD_CNTRL2 (read only)
	; B7..B4	; shifter bits 11..8
	; B3	; who knows
	; B2	; last clock state (0.1 causes count)
	; B1	; borrow in (1 causes count)
	; B0	; borrow out (count EQU 0 and borrow in)
	
	ATTEN_A	EQU $FD40
	ATTEN_B	EQU $FD41
	ATTEN_C	EQU $FD42
	ATTEN_D	EQU $FD43
	; B7..B4 attenuation left ear (0 silent ..15/16 volume)
	; B3..B0       "     right ear
	
	MPAN	EQU $FD44
	; B7..B4 left ear
	; B3..B0 right ear
	; B7/B3 EQU Audio D
	; a 1 enables attenuation for channel and side
	
	
	MSTEREO	EQU $FD50	; a 1 disables audio connection
	AUD_D_LEFT	EQU %10000000
	AUD_C_LEFT	EQU %01000000
	AUD_B_LEFT	EQU %00100000
	AUD_A_LEFT	EQU %00010000
	AUD_D_RIGHT	EQU %00001000
	AUD_C_RIGHT	EQU %00000100
	AUD_B_RIGHT	EQU %00000010
	AUD_A_RIGHT	EQU %00000001
	
	 */
	static int mixer_channel;
	static int usec_per_sample;
	static int[] shift_mask;
	static int[] shift_xor;
        
        public static class _n {
            public int volume;
            public int feedback;
            public int output;
            public int shifter;
            public int bakup;
            public int control1;
            public int counter;
            public int control2;
        };
        
        public static class _reg {
            public int[] data = new int[8];
            public _n n = new _n();
        };
	
	public static class LYNX_AUDIO {
	    public int nr;
	    public _reg reg = new _reg();
	    public int attenuation;
	    public int mask;
	    public int shifter;
	    public int ticks;
	    public int count;

            private LYNX_AUDIO(int _i) {
                super();

                this.nr = _i;
            }
            
	};
        
	static LYNX_AUDIO lynx_audio[]= { 
		new LYNX_AUDIO( 0 ),
		new LYNX_AUDIO( 1 ),
		new LYNX_AUDIO( 2 ),
		new LYNX_AUDIO( 3 ) 
	};
	
	static void lynx_audio_reset_channel(LYNX_AUDIO This)
	{
	    //memset(This.reg.data, 0, (char*)(This+1)-(char*)(This.reg.data));
            This.reg.data = new int[8];
	}
	
	public static void lynx_audio_count_down(int nr)
	{
	    LYNX_AUDIO This=lynx_audio[nr];
	    if ((This.reg.n.control1&8)!=0 && (This.reg.n.control1&7)!=7) return;
	    if (nr==0) stream_update(mixer_channel,0);
	    This.count--;
	}
	
	public static void lynx_audio_debug(mame_bitmap bitmap)
	{
	    String str = "";
	    str = sprintf(str,"%.2x %.2x %.2x %.2x %.2x %.2x %.2x %.2x",
		    lynx_audio[0].reg.data[0],
		    lynx_audio[0].reg.data[1],
		    lynx_audio[0].reg.data[2],
		    lynx_audio[0].reg.data[3],
		    lynx_audio[0].reg.data[4],
		    lynx_audio[0].reg.data[5],
		    lynx_audio[0].reg.data[6],
		    lynx_audio[0].reg.data[7]);
	
	//    ui_text(bitmap, str, 0,0);
	}
	
	static void lynx_audio_shift(LYNX_AUDIO channel)
	{
	    channel.shifter=((channel.shifter<<1)&0x3ff)
		|shift_xor[channel.shifter&channel.mask];
	    
	    if ((channel.reg.n.control1&0x20) != 0) {
		if ((channel.shifter&1) != 0) {
		    channel.reg.n.output+=channel.reg.n.volume;
		} else {
		    channel.reg.n.output-=channel.reg.n.volume;
		}
	    }
	    switch (channel.nr) {
	    case 0: lynx_audio_count_down(1); break;
	    case 1: lynx_audio_count_down(2); break;
	    case 2: lynx_audio_count_down(3); break;
	    case 3: lynx_timer_count_down(1); break;
	    }
	}
	
	static void lynx_audio_execute(LYNX_AUDIO channel)
	{
	    if ((channel.reg.n.control1&8) != 0) { // count_enable
		channel.ticks+=usec_per_sample;
		if ((channel.reg.n.control1&7)==7) { // timer input
		    if (channel.count<0) {
			channel.count+=channel.reg.n.counter;
			lynx_audio_shift(channel);
		    }
		} else {
		    int t=1<<(channel.reg.n.control1&7);
		    for (;;) {
			for (;(channel.ticks>=t)&&channel.count>=0; channel.ticks-=t)
			    channel.count--;
			if (channel.ticks<t) break;
			if (channel.count<0) {
			    channel.count=channel.reg.n.counter;
			    lynx_audio_shift(channel);
			}
		    }
		}
		if ((channel.reg.n.control1&0x20) == 0) {
		    channel.reg.n.output=(channel.shifter&1)!=0?0-channel.reg.n.volume:channel.reg.n.volume;
		}
	    } else {
		channel.ticks=0;
		channel.count=0;
	    }
	}
	
	static int attenuation_enable;
	static int master_enable;
	
	public static int lynx_audio_read(int offset)
	{
	    int data=0;
	    stream_update(mixer_channel,0);
	    switch (offset) {
	    case 0x20: case 0x21: case 0x22: case 0x24: case 0x25:
	    case 0x28: case 0x29: case 0x2a: case 0x2c: case 0x2d: 
	    case 0x30: case 0x31: case 0x32: case 0x34: case 0x35: 
	    case 0x38: case 0x39: case 0x3a: case 0x3c: case 0x3d: 
		data=lynx_audio[(offset>>3)&3].reg.data[offset&7];
		break;
	    case 0x23: case 0x2b: case 0x33: case 0x3b: 
		data=lynx_audio[(offset>>3)&3].shifter&0xff;
		break;
	    case 0x26:case 0x2e:case 0x36:case 0x3e:
		data=lynx_audio[(offset>>3)&3].count;
		break;
	    case 0x27: case 0x2f: case 0x37: case 0x3f:
		data=(lynx_audio[(offset>>3)&3].shifter>>4)&0xf0;
		data|=lynx_audio[(offset>>3)&3].reg.data[offset&7]&0x0f;
		break;
	    case 0x40: case 0x41: case 0x42: case 0x43: 
		data=lynx_audio[offset&3].attenuation;
		break;
	    case 0x44: 
		data=attenuation_enable;
		break;
	    case 0x50:
		data=master_enable;
		break;
	    }
	    return data;
	}
	
	public static void lynx_audio_write(int offset, int data)
	{
	//	logerror("%.6f audio write %.2x %.2x\n", timer_get_time(), offset, data);
	    LYNX_AUDIO channel=lynx_audio[((offset>>3)&3)];
	    stream_update(mixer_channel,0);
	    switch (offset) {
	    case 0x20: case 0x22: case 0x24: case 0x26:
	    case 0x28: case 0x2a: case 0x2c: case 0x2e:
	    case 0x30: case 0x32: case 0x34: case 0x36:
	    case 0x38: case 0x3a: case 0x3c: case 0x3e:
		lynx_audio[(offset>>3)&3].reg.data[offset&7]=data;
		break;
	    case 0x23: case 0x2b: case 0x33: case 0x3b: 
		lynx_audio[(offset>>3)&3].reg.data[offset&7]=data;
		lynx_audio[(offset>>3)&3].shifter&=~0xff;
		lynx_audio[(offset>>3)&3].shifter|=data;
		break;
	    case 0x27: case 0x2f: case 0x37: case 0x3f:
		lynx_audio[(offset>>3)&3].reg.data[offset&7]=data;
		lynx_audio[(offset>>3)&3].shifter&=~0xf00;
		lynx_audio[(offset>>3)&3].shifter|=(data&0xf0)<<4;
		break;
	    case 0x21: case 0x25:
	    case 0x29: case 0x2d:
	    case 0x31: case 0x35:
	    case 0x39: case 0x3d:
		channel.reg.data[offset&7]=data;
		channel.mask=channel.reg.n.feedback;
		channel.mask|=(channel.reg.data[5]&0x80)<<1;
		break;
	    case 0x40: case 0x41: case 0x42: case 0x43: // lynx2 only, howard extension board
		lynx_audio[offset&3].attenuation=data;
		break;
	    case 0x44: 
		attenuation_enable=data; //lynx2 only, howard extension board
		break;
	    case 0x50:
		master_enable=data;//lynx2 only, howard write only
		break;
	    }
	}
	
	/************************************/
	/* Sound handler update             */
	/************************************/
	public static StreamInitPtr lynx_update = new StreamInitPtr() {
            public void handler(int param, ShortPtr buffer, int length) {
                int i, j;
                LYNX_AUDIO channel;
                int _channel = 0;
                int v;
                //System.out.println(length);
                //System.out.println(lynx_audio.length);

                for (i = 0; i < length; i++, buffer.inc())
                {
                    buffer.write( 0 );
                    j=0;
                    _channel = 0;
                    for (channel=lynx_audio[_channel]; j<lynx_audio.length; ) {
                        lynx_audio_execute(channel);
                        v=channel.reg.n.output;
                        buffer.write(buffer.read()+v*15);
                        j++; _channel++;
                    }
                }
            }
        };
	
	public static StreamInitMultiPtr lynx2_update = new StreamInitMultiPtr() {
            public void handler(int param, ShortPtr[] buffer, int length) {
                ShortPtr left=new ShortPtr(buffer[0]), right=new ShortPtr(buffer[1]);
                int i, j;
                LYNX_AUDIO channel;
                int _channel = 0;
                int v;

                for (i = 0; i < length; i++, left.inc(), right.inc())
                {
                    left.write( 0 );
                    right.write( 0 );
                    j=0;
/*TODO*///                    for (channel=lynx_audio[_channel];  j<lynx_audio.length; j++) {
/*TODO*///                        lynx_audio_execute(channel);
/*TODO*///                        v=channel.reg.n.output;
/*TODO*///                        if ((master_enable&(0x10<<j)) == 0) {
/*TODO*///                            if ((attenuation_enable&(0x10<<j)) != 0) {
/*TODO*///                                left.write(left.read()+v*(channel.attenuation>>4));
/*TODO*///                            } else {
/*TODO*///                                left.write(left.read()+v*15);
/*TODO*///                            }
/*TODO*///                        }
/*TODO*///                        if ((master_enable&(1<<j)) == 0) {
/*TODO*///                            if ((attenuation_enable&(1<<j)) != 0) {
/*TODO*///                                right.write(right.read()+v*(channel.attenuation&0xf));
/*TODO*///                            } else {
/*TODO*///                                right.write(right.read()+v*15);
/*TODO*///                            }
/*TODO*///                        }
/*TODO*///                        _channel++;
/*TODO*///                    }
                }
            }
        };
        
	public static void lynx_audio_init()
	{
	    int i;
	    shift_mask=new int[512];
	    assert(shift_mask!=null);
	
	    shift_xor=new int[4096];
	    assert(shift_xor!=null);
	
	    for (i=0; i<512; i++) {
		shift_mask[i]=0;
		if ((i&1)       != 0) shift_mask[i]|=1;
		if ((i&2)       != 0) shift_mask[i]|=2;
		if ((i&4)       != 0) shift_mask[i]|=4;
		if ((i&8)       != 0) shift_mask[i]|=8;
		if ((i&0x10)    != 0) shift_mask[i]|=0x10;
		if ((i&0x20)    != 0) shift_mask[i]|=0x20;
		if ((i&0x40)    != 0) shift_mask[i]|=0x400;
		if ((i&0x80)    != 0) shift_mask[i]|=0x800;
		if ((i&0x100)   != 0) shift_mask[i]|=0x80;
	    }
	    for (i=0; i<4096; i++) {
		int j;
		shift_xor[i]=1;
		for (j=4096/2; j>0; j>>=1) {
		    if ((i&j) != 0) shift_xor[i]^=1;
		}
	    }
	}
	
	public static void lynx_audio_reset()
	{
	    int i;
	    for (i=0; i<lynx_audio.length; i++) {
		lynx_audio_reset_channel(lynx_audio[i]);
	    }
	}
	
	/************************************/
	/* Sound handler start              */
	/************************************/
	public static ShStartPtr lynx_custom_start = new ShStartPtr() {
            public int handler(MachineSound msound) {
                if (options.samplerate==0) return 0;
	
                mixer_channel = stream_init("lynx", MIXER(50, MIXER_PAN_CENTER), 
                                            options.samplerate, 0, lynx_update);

                usec_per_sample=1000000/options.samplerate;

                lynx_audio_init();
                return 0;
            }
        };
	
	public static ShStartPtr lynx2_custom_start = new ShStartPtr() {
            public int handler(MachineSound msound) {
                int vol[]={ MIXER(50, MIXER_PAN_LEFT), MIXER(50, MIXER_PAN_RIGHT) };
                String names[]= { "lynx", "lynx" };

                if (options.samplerate==0) return 0;

                mixer_channel = stream_init_multi(2, names, vol, options.samplerate, 0, lynx2_update);

                usec_per_sample=1000000/options.samplerate;

                lynx_audio_init();
                return 0;
            }
	};
	
	/************************************/
	/* Sound handler stop               */
	/************************************/
	public static ShStopPtr lynx_custom_stop = new ShStopPtr() {
            public void handler() {
                shift_xor = null;
                shift_mask = null;
            }
        };
	
	public static ShUpdatePtr lynx_custom_update = new ShUpdatePtr() {
            public void handler() {
                // nothing to do
            }
        };
	
}
