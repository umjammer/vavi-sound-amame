/***************************************************************************

  MOS sound interface device sid6581

***************************************************************************/

/* uses Michael Schwendt's sidplay (copyright message in 6581_.cpp)
   problematic and much work to integrate, so better to redo bugfixes
   in the converted code

   now only 1 sid chip allowed!
   needs rework
*/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.sndhrdw;

import static arcadeflex056.fucPtr.*;
import static common.libc.cstdio.*;
import static common.ptr.*;
import static common.subArrays.*;
import static mame056.mame.options;
import static mame056.sndintrfH.*;
import static mame056.sound.streams.*;
import static mess056.includes.sid6581H.*;
import static mess056.sndhrdw.sid.*;
import static mess056.sndhrdw.sidH.*;
import static mess056.sndhrdw.sidvoice.*;

public class sid6581
{
	
	public static int VERBOSE_DBG = 1;
	
	
	public static _SID6581[] _sid6581 = new _SID6581[MAX_SID6581];
        
        static {
            for (int _i = 0 ; _i<MAX_SID6581 ; _i++)
                _sid6581[_i] = new _SID6581();
        }
	
	public static void sid6581_set_type (int number, int type)
	{
	    _sid6581[number].type=type;
	    sidInitWaveformTables(type);
	}
	
	public static void sid6581_reset(int number)
	{
		boolean b = sidEmuReset(_sid6581[number]);
	}
	
        public static ReadHandlerPtr sid6581_0_port_r = new ReadHandlerPtr() {
            public int handler(int offset) {
                return sid6581_port_r(0, offset);
            }
        };

	public static ReadHandlerPtr sid6581_1_port_r = new ReadHandlerPtr() {
            public int handler(int offset) {
		return sid6581_port_r(1, offset);
            }
        };
	
        public static WriteHandlerPtr sid6581_0_port_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                sid6581_port_w(0, offset, data);
            }
        };

        public static WriteHandlerPtr sid6581_1_port_w  = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
		sid6581_port_w(1, offset, data);
            }
        };
	
	public static void sid6581_update()
	{
		int i;
		for (i=0; i<MAX_SID6581; i++) {
			if (_sid6581[i].on != 0)
				stream_update(_sid6581[i].mixer_channel,0);
		}
	}
	
	public static StreamInitPtr sid6581_sh_update = new StreamInitPtr() {
            public void handler(int param, ShortPtr buffer, int length) {
                sidEmuFillBuffer(_sid6581[param], buffer, length);
            }
        };
	
	public static ShStartPtr sid6581_custom_start = new ShStartPtr() {
            public int handler(MachineSound driver) {
		SID6581_interface iface=(SID6581_interface) driver.sound_interface;
		int i;
	
		for (i=0; i< iface.count; i++) {
			String name="";
			if (iface.count!=1) name=sprintf("SID%d",i);
			else name=sprintf("SID");
			_sid6581[i].mixer_channel = stream_init (name, iface.chips[i].default_mixer_level, options.samplerate, i, sid6581_sh_update);
	
			_sid6581[i].PCMfreq = options.samplerate;	
			_sid6581[i].type=iface.chips[i].type;
			_sid6581[i].clock=iface.chips[i].clock;
			_sid6581[i].ad_read=iface.chips[i].ad_read;
			_sid6581[i].on=1;
			sid6581_init(_sid6581[i]);
		}
	
		return 0;
            }
        };

	public static ShStopPtr sid6581_custom_stop = new ShStopPtr() {
            public void handler() {
            
            }
        };
        
	public static ShUpdatePtr sid6581_custom_update = new ShUpdatePtr() {
            public void handler() {
            
            }
        };
	
}
