/**************************************************************************************
* Gameboy sound emulation (c) Anthony Kruize (trandor@labyrinth.net.au)
*
* Anyways, sound on the gameboy consists of 4 separate 'channels'
*   Sound1 = Quadrangular waves with SWEEP and ENVELOPE functions  (NR10,11,12,13,14)
*   Sound2 = Quadrangular waves with ENVELOPE functions (NR21,22,23,24)
*   Sound3 = Wave patterns from WaveRAM (NR30,31,32,33,34)
*   Sound4 = White noise with an envelope (NR41,42,43,44)
*
* Each sound channel has 2 modes, namely ON and OFF...  whoa
*
* These tend to be the two most important equations in
* converting between Hertz and GB frequency registers:
* (Sounds will have a 2.4% higher frequency on Super GB.)
*       gb = 2048 - (131072 / Hz)
*       Hz = 131072 / (2048 - gb)
*
* Changes:
*
*	10/2/2002		AK - Preliminary sound code.
*	13/2/2002		AK - Added a hack for mode 4, other fixes.
*	23/2/2002		AK - Use lookup tables, added sweep to mode 1. Re-wrote the square
*						 wave generation.
*	13/3/2002		AK - Added mode 3, better lookup tables, other adjustments.
*	15/3/2002		AK - Mode 4 can now change frequencies.
*	31/3/2002		AK - Accidently forgot to handle counter/consecutive for mode 1.
*	 3/4/2002		AK - Mode 1 sweep can still occur if shift is 0.  Don't let frequency
*						 go past the maximum allowed value. Fixed Mode 3 length table.
*						 Slight adjustment to Mode 4's period table generation.
*	 5/4/2002		AK - Mode 4 is done correctly, using a polynomial counter instead
*						 of being a total hack.
*	 6/4/2002		AK - Slight tweak to mode 3's frequency calculation.
*	13/4/2002		AK - Reset envelope value when sound is initialized.
*	21/4/2002		AK - Backed out the mode 3 frequency calculation change.
*						 Merged init functions into gameboy_sound_w().
*
***************************************************************************************/

/*
 * ported to v0.56.1
 * using automatic conversion tool v0.01
 */ 
package mess056.sndhrdw;

import static arcadeflex056.fucPtr.*;
import static arcadeflex056.osdepend.logerror;
import static common.libc.cstdlib.rand;
import static common.ptr.*;
import static mame056.mame.Machine;
import static mame056.sndintrfH.*;
import static mame056.sound.mixerH.*;
import static mame056.sound.streams.*;
import static mess056.machine.gb.gb_ram;

public class gb
{
    public static final int NR10 = 0xFF10;
    public static final int NR11 = 0xFF11;
    public static final int NR12 = 0xFF12;
    public static final int NR13 = 0xFF13;
    public static final int NR14 = 0xFF14;
    public static final int NR21 = 0xFF16;
    public static final int NR22 = 0xFF17;
    public static final int NR23 = 0xFF18;
    public static final int NR24 = 0xFF19;
    public static final int NR30 = 0xFF1A;
    public static final int NR31 = 0xFF1B;
    public static final int NR32 = 0xFF1C;
    public static final int NR33 = 0xFF1D;
    public static final int NR34 = 0xFF1E;
    public static final int NR41 = 0xFF20;
    public static final int NR42 = 0xFF21;
    public static final int NR43 = 0xFF22;
    public static final int NR44 = 0xFF23;
    public static final int NR50 = 0xFF24;
    public static final int NR51 = 0xFF25;
    public static final int NR52 = 0xFF26;

    public static final int LEFT = 1;
    public static final int RIGHT = 2;
    public static final int MAX_FREQUENCIES = 2048;

    static int channel = 1;
    static int rate;

    /* Represents wave duties of 12.5%, 25%, 50% and 75% */
    static float wave_duty_table[] = { 8.0f, 4.0f, 2.0f, 1.33f };

    static int[] env_length_table = new int[8];
    static int[] swp_time_table = new int[8];
    static int[] period_table = new int[MAX_FREQUENCIES];
    static int[] period_mode3_table = new int[MAX_FREQUENCIES];
    static int[][] period_mode4_table = new int[8][16];
    static int[] length_table = new int[64];
    static int[] length_mode3_table = new int[256];

    public static class SOUND1
    {
            public int on;
            public int channel;
            public int length;
            public int pos;
            public int period;
            public int frequency;
            public int count;
            public int signal;
            public int mode;
            public int duty;
            public int env_value;
            public int env_direction;
            public int env_length;
            public int env_count;
            public int swp_shift;
            public int swp_direction;
            public int swp_time;
            public int swp_count;
    };

    public static class SOUND2
    {
            public int on;
            public int channel;
            public int length;
            public int pos;
            public int period;
            public int count;
            public int signal;
            public int mode;
            public int duty;
            public int env_value;
            public int env_direction;
            public int env_length;
            public int env_count;
    };

    public static class SOUND3
    {
            public int on;
            public int channel;
            public int length;
            public int pos;
            public int period;
            public int count;
            public int offset;
            public int duty;
            public int mode;
            public int level;
    };

    public static class SOUND4
    {
            public int on;
            public int channel;
            public int length;
            public int period;
            public int pos;
            public int count;
            public int signal;
            public int mode;
            public int env_value;
            public int env_direction;
            public int env_length;
            public int env_count;
            public int ply_step;
            public int ply_value;
    };

    public static class SOUNDC
    {
            public int on;
            public int vol_left;
            public int vol_right;
            public int mode1_left;
            public int mode1_right;
            public int mode2_left;
            public int mode2_right;
            public int mode3_left;
            public int mode3_right;
            public int mode4_left;
            public int mode4_right;
    };

    static SOUND1 snd_1 = new SOUND1();
    static SOUND2 snd_2 = new SOUND2();
    static SOUND3 snd_3 = new SOUND3();
    static SOUND4 snd_4 = new SOUND4();
    static SOUNDC snd_control = new SOUNDC();

/*TODO*///    void gameboy_update(int param, INT16 **buffer, int length);

    public static WriteHandlerPtr gameboy_sound_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            /* change in registers so update first */
            stream_update(channel, 0);

            /* Only register NR52 is accessible if the sound controller is disabled */
            if( snd_control.on==0 && offset != NR52 )
            {
                    return;
            }

            /* Store the value */
            gb_ram.write(offset, data);

            switch( offset )
            {
            /*MODE 1 */
            case NR10: /* Sweep (R/W) */
                    snd_1.swp_shift = data & 0x7;
                    snd_1.swp_direction = (data & 0x8) >> 3;
                    snd_1.swp_direction |= snd_1.swp_direction - 1;
                    snd_1.swp_time = swp_time_table[ (data & 0x70) >> 4 ];
                    break;
            case NR11: /* Sound length/Wave pattern duty (R/W) */
                    snd_1.duty = (data & 0xC0) >> 6;
                    snd_1.length = length_table[data & 0x3F];
                    break;
            case NR12: /* Envelope (R/W) */
                    snd_1.env_value = data >> 4;
                    snd_1.env_direction = (data & 0x8) >> 3;
                    snd_1.env_direction |= snd_1.env_direction - 1;
                    snd_1.env_length = env_length_table[data & 0x7];
                    break;
            case NR13: /* Frequency lo (R/W) */
                    snd_1.frequency = ((gb_ram.read(NR14)&0x7)<<8) | gb_ram.read(NR13);
                    snd_1.period = period_table[snd_1.frequency];
                    break;
            case NR14: /* Frequency hi / Initialize (R/W) */
                    snd_1.mode = (data & 0x40) >> 6;
                    snd_1.frequency = ((gb_ram.read(NR14)&0x7)<<8) | gb_ram.read(NR13);
                    snd_1.period = period_table[snd_1.frequency];
                    if(( data & 0x80 ) != 0)
                    {
                            if( snd_1.on == 0 )
                                    snd_1.pos = 0;
                            snd_1.on = 1;
                            snd_1.count = 0;
                            snd_1.env_value = gb_ram.read(NR12) >> 4;
                            snd_1.env_count = 0;
                            snd_1.swp_count = 0;
                            snd_1.signal = 0x1;
                            gb_ram.write(NR52, gb_ram.read(NR52) | 0x1);
                    }
                    break;

            /*MODE 2 */
            case NR21: /* Sound length/Wave pattern duty (R/W) */
                    snd_2.duty = (data & 0xC0) >> 6;
                    snd_2.length = length_table[data & 0x3F];
                    break;
            case NR22: /* Envelope (R/W) */
                    snd_2.env_value = data >> 4;
                    snd_2.env_direction = (data & 0x8 ) >> 3;
                    snd_2.env_direction |= snd_2.env_direction - 1;
                    snd_2.env_length = env_length_table[data & 0x7];
                    break;
            case NR23: /* Frequency lo (R/W) */
                    snd_2.period = period_table[((gb_ram.read(NR24)&0x7)<<8) | gb_ram.read(NR23)];
                    break;
            case NR24: /* Frequency hi / Initialize (R/W) */
                    snd_2.mode = (data & 0x40) >> 6;
                    snd_2.period = period_table[((gb_ram.read(NR24)&0x7)<<8) | gb_ram.read(NR23)];
                    if(( data & 0x80 ) != 0)
                    {
                            if( snd_2.on == 0 )
                                    snd_2.pos = 0;
                            snd_2.on = 1;
                            snd_2.count = 0;
                            snd_2.env_value = gb_ram.read(NR22) >> 4;
                            snd_2.env_count = 0;
                            snd_2.signal = 0x1;
                            gb_ram.write(NR52, gb_ram.read(NR52) | 0x2);
                    }
                    break;

            /*MODE 3 */
            case NR30: /* Sound On/Off (R/W) */
                    snd_3.on = (data & 0x80) >> 7;
                    break;
            case NR31: /* Sound Length (R/W) */
                    snd_3.length = length_mode3_table[data];
                    break;
            case NR32: /* Select Output Level */
                    snd_3.level = (data & 0x60) >> 5;
                    break;
            case NR33: /* Frequency lo (W) */
                    snd_3.period = period_mode3_table[((gb_ram.read(NR34)&0x7)<<8) + gb_ram.read(NR33)];
                    break;
            case NR34: /* Frequency hi / Initialize (W) */
                    snd_3.mode = (data & 0x40) >> 6;
                    snd_3.period = period_mode3_table[((gb_ram.read(NR34)&0x7)<<8) + gb_ram.read(NR33)];
                    if(( data & 0x80 ) != 0)
                    {
                            if( snd_3.on == 0 )
                            {
                                    snd_3.pos = 0;
                                    snd_3.offset = 0;
                                    snd_3.duty = 0;
                            }
                            snd_3.on = 1;
                            snd_3.count = 0;
                            gb_ram.write(NR52, gb_ram.read(NR52) | 0x4 );
                    }
                    break;

            /*MODE 4 */
            case NR41: /* Sound Length (R/W) */
                    snd_4.length = length_table[data & 0x3F];
                    break;
            case NR42: /* Envelope (R/W) */
                    snd_4.env_value = data >> 4;
                    snd_4.env_direction = (data & 0x8 ) >> 3;
                    snd_4.env_direction |= snd_4.env_direction - 1;
                    snd_4.env_length = env_length_table[data & 0x7];
                    break;
            case NR43: /* Polynomial Counter/Frequency */
                    snd_4.period = period_mode4_table[data & 0x7][(data & 0xF0) >> 4];
                    snd_4.ply_step = (data & 0x8) >> 3;
                    break;
            case NR44: /* Counter/Consecutive / Initialize (R/W)  */
                    snd_4.mode = (data & 0x40) >> 6;
                    if(( data & 0x80 ) != 0)
                    {
                            if( snd_4.on == 0)
                                    snd_4.pos = 0;
                            snd_4.on = 1;
                            snd_4.count = 0;
                            snd_4.env_value = gb_ram.read(NR42) >> 4;
                            snd_4.env_count = 0;
                            snd_4.signal = rand();
                            snd_4.ply_value = 0x7fff;
                            gb_ram.write(NR52, gb_ram.read(NR52) | 0x8);
                    }
                    break;

            /* CONTROL */
            case NR50: /* Channel Control / On/Off / Volume (R/W)  */
                    snd_control.vol_left = data & 0x7;
                    snd_control.vol_right = (data & 0x70) >> 4;
                    break;
            case NR51: /* Selection of Sound Output Terminal */
                    snd_control.mode1_right = data & 0x1;
                    snd_control.mode1_left = (data & 0x10) >> 4;
                    snd_control.mode2_right = (data & 0x2) >> 1;
                    snd_control.mode2_left = (data & 0x20) >> 5;
                    snd_control.mode3_right = (data & 0x4) >> 2;
                    snd_control.mode3_left = (data & 0x40) >> 6;
                    snd_control.mode4_right = (data & 0x8) >> 3;
                    snd_control.mode4_left = (data & 0x80) >> 7;
                    break;
            case NR52: /* Sound On/Off (R/W) */
                    /* Only bit 7 is writable, writing to bits 0-3 does NOT enable or
                       disable sound.  They are read-only */
                    snd_control.on = (data & 0x80) >> 7;
                    if( snd_control.on == 0 )
                    {
                            snd_1.on = 0;
                            snd_2.on = 0;
                            snd_3.on = 0;
                            snd_4.on = 0;
                            gb_ram.write(offset, 0);
                    }
                    break;
            }
        }
    };

    public static StreamInitMultiPtr gameboy_update = new StreamInitMultiPtr() {
        public void handler(int param, ShortPtr[] buffer, int length) {

            int sample, left, right, mode4_mask;

            while( length-- > 0 )
            {
                    left = right = 0;

                    /* Mode 1 - Wave with Envelope and Sweep */
                    if( snd_1.on != 0 )
                    {
                            sample = snd_1.signal & snd_1.env_value;
                            snd_1.pos++;
                            if( snd_1.pos == ((int)(snd_1.period / wave_duty_table[snd_1.duty])) >> 16)
                            {
                                    snd_1.signal = -snd_1.signal;
                            }
                            else if( snd_1.pos > (snd_1.period >> 16) )
                            {
                                    snd_1.pos = 0;
                                    snd_1.signal = -snd_1.signal;
                            }

                            if( snd_1.length!=0 && snd_1.mode!=0 )
                            {
                                    snd_1.count++;
                                    if( snd_1.count >= snd_1.length )
                                    {
                                            snd_1.on = 0;
                                            gb_ram.write(NR52, gb_ram.read(NR52) & 0xFE);
                                    }
                            }

                            if( snd_1.env_length != 0 )
                            {
                                    snd_1.env_count++;
                                    if( snd_1.env_count >= snd_1.env_length )
                                    {
                                            snd_1.env_count = 0;
                                            snd_1.env_value += snd_1.env_direction;
                                            if( snd_1.env_value < 0 )
                                                    snd_1.env_value = 0;
                                            if( snd_1.env_value > 15 )
                                                    snd_1.env_value = 15;
                                    }
                            }

                            if( snd_1.swp_time != 0 )
                            {
                                    snd_1.swp_count++;
                                    if( snd_1.swp_count >= snd_1.swp_time )
                                    {
                                            snd_1.swp_count = 0;
                                            if( snd_1.swp_direction > 0 )
                                            {
                                                    snd_1.frequency -= snd_1.frequency / (1 << snd_1.swp_shift );
                                                    if( snd_1.frequency <= 0 )
                                                    {
                                                            snd_1.on = 0;
                                                            gb_ram.write(NR52, gb_ram.read(NR52) & 0xFE);
                                                    }
                                            }
                                            else
                                            {
                                                    snd_1.frequency += snd_1.frequency / (1 << snd_1.swp_shift );
                                                    if( snd_1.frequency >= MAX_FREQUENCIES )
                                                    {
                                                            snd_1.frequency = MAX_FREQUENCIES - 1;
                                                    }
                                            }

                                            snd_1.period = period_table[snd_1.frequency];
                                    }
                            }

                            if( snd_control.mode1_left != 0 )
                                    left += sample;
                            if( snd_control.mode1_right != 0 )
                                    right += sample;
                    }

                    /* Mode 2 - Wave with Envelope */
                    if( snd_2.on != 0 )
                    {
                            sample = snd_2.signal & snd_2.env_value;
                            snd_2.pos++;
                            if( snd_2.pos == ((int)(snd_2.period / wave_duty_table[snd_2.duty])) >> 16)
                            {
                                    snd_2.signal = -snd_2.signal;
                            }
                            else if( snd_2.pos > (snd_2.period >> 16) )
                            {
                                    snd_2.pos = 0;
                                    snd_2.signal = -snd_2.signal;
                            }

                            if( snd_2.length!=0 && snd_2.mode!=0 )
                            {
                                    snd_2.count++;
                                    if( snd_2.count >= snd_2.length )
                                    {
                                            snd_2.on = 0;
                                            gb_ram.write(NR52, gb_ram.read(NR52) & 0xFD);
                                    }
                            }

                            if( snd_2.env_length != 0 )
                            {
                                    snd_2.env_count++;
                                    if( snd_2.env_count >= snd_2.env_length )
                                    {
                                            snd_2.env_count = 0;
                                            snd_2.env_value += snd_2.env_direction;
                                            if( snd_2.env_value < 0 )
                                                    snd_2.env_value = 0;
                                            if( snd_2.env_value > 15 )
                                                    snd_2.env_value = 15;
                                    }
                            }

                            if( snd_control.mode2_left != 0 )
                                    left += sample;
                            if( snd_control.mode2_right != 0 )
                                    right += sample;
                    }

                    /* Mode 3 - Wave patterns from WaveRAM */
                    if( snd_3.on != 0 )
                    {
                            /* NOTE: This is close, but not quite right.
                               The problem is that the calculation for the period
                               is too course, resulting in wrong notes occasionally.
                               The most common side effect is the same note played twice */
                            sample = gb_ram.read(0xFF30 + snd_3.offset);
                            if( snd_3.duty == 0)
                            {
                                    sample >>= 4;
                            }

                            sample &= 0xF;
                            sample -= 8;

                            if( snd_3.level != 0 )
                                    sample >>= (snd_3.level - 1);
                            else
                                    sample = 0;

                            snd_3.pos++;
                            if( snd_3.pos > (((snd_3.period ) >> 21)) ) /* 21 = .. >> 16) / 32 */
    /*			if( (snd_3.pos<<16) >= (UINT32)(((snd_3.period / 31) + (1<<16))) ) */
                            {
                                    snd_3.pos = 0;
                                    snd_3.duty = snd_3.duty!=0?0:1;
                                    if( snd_3.duty == 0 )
                                    {
                                            snd_3.offset++;
                                            if( snd_3.offset > 0xF )
                                                    snd_3.offset = 0;
                                    }
                            }

                            if( snd_3.length!=0 && snd_3.mode!=0 )
                            {
                                    snd_3.count++;
                                    if( snd_3.count >= snd_3.length )
                                    {
                                            snd_3.on = 0;
                                            gb_ram.write(NR52, gb_ram.read(NR52) & 0xFB);
                                    }
                            }

                            if( snd_control.mode3_left != 0 )
                                    left += sample;
                            if( snd_control.mode3_right != 0 )
                                    right += sample;
                    }

                    /* Mode 4 - Noise with Envelope */
                    if( snd_4.on != 0 )
                    {
                            /* Similar problem to Mode 3, we seem to miss some notes */
                            sample = snd_4.signal & snd_4.env_value;
                            snd_4.pos++;
                            if( snd_4.pos == (snd_4.period >> 17) )
                            {
                                    /* Using a Polynomial Counter (aka Linear Feedback Shift Register)
                                       Mode 4 has a 7 bit and 15 bit counter so we need to shift the
                                       bits around accordingly */
                                    mode4_mask = (((snd_4.ply_value & 0x2) >> 1) ^ (snd_4.ply_value & 0x1)) << (snd_4.ply_step!=0 ? 6 : 14);
                                    snd_4.ply_value >>= 1;
                                    snd_4.ply_value |= mode4_mask;
                                    snd_4.ply_value &= (snd_4.ply_step!=0 ? 0x7f : 0x7fff);
                                    snd_4.signal = snd_4.ply_value;
                            }
                            else if( snd_4.pos > (snd_4.period >> 16) )
                            {
                                    snd_4.pos = 0;
                                    mode4_mask = (((snd_4.ply_value & 0x2) >> 1) ^ (snd_4.ply_value & 0x1)) << (snd_4.ply_step!=0 ? 6 : 14);
                                    snd_4.ply_value >>= 1;
                                    snd_4.ply_value |= mode4_mask;
                                    snd_4.ply_value &= (snd_4.ply_step!=0 ? 0x7f : 0x7fff);
                                    snd_4.signal = snd_4.ply_value;
                            }

                            if( snd_4.length!=0 && snd_4.mode!=0 )
                            {
                                    snd_4.count++;
                                    if( snd_4.count >= snd_4.length )
                                    {
                                            snd_4.on = 0;
                                            gb_ram.write(NR52, gb_ram.read(NR52) & 0xF7);
                                    }
                            }

                            if( snd_4.env_length != 0 )
                            {
                                    snd_4.env_count++;
                                    if( snd_4.env_count >= snd_4.env_length )
                                    {
                                            snd_4.env_count = 0;
                                            snd_4.env_value += snd_4.env_direction;
                                            if( snd_4.env_value < 0 )
                                                    snd_4.env_value = 0;
                                            if( snd_4.env_value > 15 )
                                                    snd_4.env_value = 15;
                                    }
                            }

                            if( snd_control.mode4_left != 0 )
                                    left += sample;
                            if( snd_control.mode4_right != 0 )
                                    right += sample;
                    }

                    /* Adjust for master volume */
                    left *= snd_control.vol_left;
                    right *= snd_control.vol_right;

                    /* pump up the volume */
                    left <<= 6;
                    right <<= 6;

                    /* Update the buffers */
                    buffer[0].writeinc((short) left);
                    buffer[1].writeinc((short) right);
            }

            gb_ram.write(NR52, (gb_ram.read(NR52)&0xf0) | snd_1.on | (snd_2.on << 1) | (snd_3.on << 2) | (snd_4.on << 3));
        }
    };

    public static ShStartPtr gameboy_sh_start = new ShStartPtr() {
        public int handler(MachineSound msound) {
        
            int I,J;
            String names[] = { "Gameboy left", "Gameboy right" };
            int volume[] = { MIXER( 50, MIXER_PAN_LEFT ), MIXER( 50, MIXER_PAN_RIGHT ) };

            snd_1 = new SOUND1();
            snd_2 = new SOUND2();
            snd_3 = new SOUND3();
            snd_4 = new SOUND4();

            channel = stream_init_multi(2, names, volume, Machine.sample_rate, 0, gameboy_update);

            rate = Machine.sample_rate;

            /* Calculate the envelope and sweep tables */
            for( I = 0; I < 8; I++ )
            {
                    env_length_table[I] = (I * ((1 << 16) / 64) * rate) >> 16;
                    swp_time_table[I] = (((I << 16) / 128) * rate) >> 15;
            }

            /* Calculate the period tables */
            for( I = 0; I < MAX_FREQUENCIES; I++ )
            {
                    period_table[I] = ((1 << 16) / (131072 / (2048 - I))) * rate;
                    period_mode3_table[I] = ((1 << 16) / (65536 / (2048 - I))) * rate;
            }
            /* Calculate the period table for mode 4 */
            for( I = 0; I < 8; I++ )
            {
                    for( J = 0; J < 16; J++ )
                    {
                            /* I is the dividing ratio of frequencies
                               J is the shift clock frequency */
                            period_mode4_table[I][J] = (int) (((1 << 16) / (524288 / ((I == 0)?0.5:I) / (1 << (J + 1)))) * rate);
                    }
            }

            /* Calculate the length table */
            for( I = 0; I < 64; I++ )
            {
                    length_table[I] = ((64 - I) * ((1 << 16)/256) * rate) >> 16;
            }
            /* Calculate the length table for mode 3 */
            for( I = 0; I < 256; I++ )
            {
                    length_mode3_table[I] = ((256 - I) * ((1 << 16)/256) * rate) >> 16;
            }

            return 0;
        }      
    };
}
