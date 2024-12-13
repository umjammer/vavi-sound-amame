/***************************************************************************
 supervision sound hardware

 PeT mess@utanet.at
***************************************************************************/
/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.sndhrdw;

import static arcadeflex056.fucPtr.*;
import static arcadeflex056.osdepend.logerror;
import static common.ptr.*;
import static mame056.mame.*;
import static mame056.sndintrfH.*;
import static mame056.sound.mixerH.*;
import static mame056.sound.streams.*;
import static mame056.timer.*;
import static mess056.includes.svisionH.*;


public class svision {
	
	
	static int mixer_channel;
	
	public static SVISION_CHANNEL[] svision_channel = new SVISION_CHANNEL[2];
        
        static {
            svision_channel[0]=new SVISION_CHANNEL();
            svision_channel[1]=new SVISION_CHANNEL();
        }
	
	public static void svision_soundport_w (SVISION_CHANNEL channel, int offset, int data)
	{
	    stream_update(mixer_channel,0);
	    logerror("%.6f channel 1 write %d %02x\n", timer_get_time(),offset&3, data);
	    channel.reg[offset]=data;
	    switch (offset) {
	    case 0:
	    case 1:
		if (channel.reg[0] != 0) {
		    if (channel==svision_channel[0]) 
			channel.size=(int)((options.samplerate*channel.reg[0]<<6)/4e6);
                    else if (channel==svision_channel[1]) 
			channel.size=(int)((options.samplerate*channel.reg[0]<<6)/4e6);
		    else
			channel.size=(int)((options.samplerate*channel.reg[0]<<6)/4e6);
		} else channel.size=0;
		channel.pos=0;
	    }
	    
	}
	
	/************************************/
	/* Sound handler update             */
	/************************************/
	public static StreamInitMultiPtr svision_update = new StreamInitMultiPtr() {
            public void handler(int param, ShortPtr[] buffer, int length) {
                ShortPtr left=buffer[0], right=buffer[1];
                int i, j;
                SVISION_CHANNEL channel;

                for (i = 0; i < length; i++, left.inc(), right.inc())
                {
                    left.write(0);
                    right.write(0);
                    for (channel=svision_channel[0], j=0; j<svision_channel.length; j++/*, channel++*/) {
                        if (channel.pos<=channel.size/2) {
                            if ((channel.reg[2]&0x40)!=0) {
                                left.write(left.read()+(channel.reg[2]&0xf)<<8);
                            }
                            if ((channel.reg[2]&0x20)!=0) {
                                right.write(right.read()+(channel.reg[2]&0xf)<<8);
                            }
                        }
                        if ((channel.reg[2]&0x60)!=0) {
                            if (++channel.pos>=channel.size) channel.pos=0;
                        }
                    }
                }
            }
        };

	/************************************/
	/* Sound handler start              */
	/************************************/
        static int vol[]={ MIXER(50, MIXER_PAN_LEFT), MIXER(50, MIXER_PAN_RIGHT) };
	static String names[]= { "supervision", "supervision" };
            
	public static ShStartPtr svision_custom_start = new ShStartPtr() {
            public int handler(MachineSound msound) {
                if (options.samplerate == 0) return 0;
	
                mixer_channel = stream_init_multi(2, names, vol, options.samplerate, 0, svision_update);

                return 0;
            }
        };
	
	
	/************************************/
	/* Sound handler stop               */
	/************************************/
	public static ShStopPtr svision_custom_stop = new ShStopPtr() {
            public void handler() {
            
            }
        };
	
	public static ShUpdatePtr svision_custom_update = new ShUpdatePtr() {
            public void handler() {
                
            }
        };
	
}
