/**
 * ported to v0.37b7
 *
 */
package mess056.sound;

import static common.ptr.*;
import static mame056.sndintrfH.*;
import static mess056.sound.speakerH.*;
import static mame056.sound.streams.*;
import static common.libc.cstdio.*;
import static mame056.sndintrf.*;
import static mame056.mame.Machine;

public class speaker extends snd_interface {

    static short default_levels[] = {0, 32767};

    public speaker() {
        sound_num = SOUND_SPEAKER;
        name = "Speaker";
    }

    @Override
    public int chips_num(MachineSound msound) {
        return ((Speaker_interface) msound.sound_interface).num;
    }

    @Override
    public int chips_clock(MachineSound msound) {
        return 0;
    }

    public static class struct_speaker {

        int channel;
        short[] levels;
        int num_levels;
        int level;
        int mixing_level;
    };

    static Speaker_interface intf;
    static struct_speaker[] speaker = new struct_speaker[MAX_SPEAKER];

    public static void speaker_sh_init(int which, int speaker_num_levels, short[] speaker_levels) {
        struct_speaker sp = speaker[which];
        sp.levels = speaker_levels;
        sp.num_levels = speaker_num_levels;
    }
    public static StreamInitPtr speaker_sound_update = new StreamInitPtr() {
        public void handler(int param, ShortPtr buffer, int length) {
            struct_speaker sp = speaker[param];
            int volume = sp.levels[sp.level] * sp.mixing_level / 100;

            while (length-- > 0) {
                buffer.writeinc((short) (volume));
            }
        }
    };

    @Override
    public int start(MachineSound msound) {
        int i;

        intf = (Speaker_interface) msound.sound_interface;

        for (i = 0; i < intf.num; i++) {
            String buf = "";
            speaker[i] = new struct_speaker();
            speaker[i].mixing_level = intf.mixing_level[i];
            if (intf.num > 1) {
                buf = sprintf("Speaker #%d", i + 1);
            } else {
                buf += "Speaker";
            }
            speaker[i].channel = stream_init(buf, speaker[i].mixing_level, Machine.sample_rate, 0, speaker_sound_update);
            if (speaker[i].channel == -1) {
                return 1;
            }
            speaker[i].num_levels = 2;
            speaker[i].levels = default_levels;
            speaker[i].level = 0;
        }
        return 0;
    }

    @Override
    public void stop() {
        /* nothing */
    }

    @Override
    public void update() {
        int i;
        for (i = 0; i < intf.num; i++) {
            stream_update(speaker[i].channel, 0);
        }
    }

    @Override
    public void reset() {
        //nothing
    }

    public static void speaker_level_w(int which, int new_level) {
        struct_speaker sp = speaker[which];

        if (new_level < 0) {
            new_level = 0;
        } else if (new_level >= sp.num_levels) {
            new_level = sp.num_levels - 1;
        }

        if (new_level == sp.level) {
            return;
        }

        /* force streams.c to update sound until this point in time now */
        stream_update(sp.channel, 0);

        sp.level = new_level;
    }

}
