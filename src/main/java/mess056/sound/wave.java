/**
 * ported to v0.37b7
 *
 */
package mess056.sound;

import static arcadeflex056.fileio.*;
import common.ptr.ShortPtr;
import static consoleflex056.funcPtr.*;
import mame056.sndintrf.snd_interface;
import mame056.sndintrfH.MachineSound;
import static mame056.sndintrfH.SOUND_WAVE;
import static mame056.usrintrf.ui_text;
import static mame056.sound.streams.*;
import static mame056.timerH.*;
import static mame056.timer.*;
import static mess056.messH.INIT_OK;
//import static old.arcadeflex.libc_old.*;
import static arcadeflex056.osdepend.logerror;
import static mame056.mame.Machine;
import static mess056.sound.waveH.*;
import static common.libc.cstring.*;
import static common.libc.cstdio.*;
import static common.util.*;
import static mame056.common.*;
import static mame056.mame.*;

public class wave extends snd_interface {

    public wave() {
        sound_num = SOUND_WAVE;
        name = "Cassette";
    }

    @Override
    public int chips_num(MachineSound msound) {
        return ((Wave_interface) msound.sound_interface).num;
    }

    @Override
    public int chips_clock(MachineSound msound) {
        return 0;
    }

    /* Our private wave file structure */
    public static class struct_wave_file {

        int channel;/* channel for playback */
        Object file;/* osd file handle */
 /*TODO*///	int mode;				/* write mode? */
/*TODO*///	int (*fill_wave)(INT16 *,int,UINT8*);
        Object timer;/* timer (TIME_NEVER) for reading sample values */
        short play_sample;/* current sample value for playback */
 /*TODO*///	INT16 record_sample;	/* current sample value for playback */
        int display;/* display tape status on screen */
        int offset;/* offset set by device_seek function */
        int play_pos;/* sample position for playback */
        int record_pos;/* sample position for recording */
        int counter;/* sample fraction counter for playback */
        int smpfreq;/* sample frequency from the WAV header */
        int resolution;/* sample resolution in bits/sample (8 or 16) */
        int samples;/* number of samples (length * resolution / 8) */
        int length;/* length in bytes */
        byte[] data;/* sample data */
        int status;/* other status (mute, motor inhibit) */
    };

    static Wave_interface intf;
    static struct_wave_file[] wave = new struct_wave_file[MAX_WAVE];// = {{-1,},{-1,}};

    /*TODO*///#ifdef LSB_FIRST
/*TODO*///#define intelLong(x) (x)
/*TODO*///#else
/*TODO*///#define intelLong(x) (((x << 24) | (((unsigned long) x) >> 24) | \
/*TODO*///                       (( x & 0x0000ff00) << 8) | (( x & 0x00ff0000) >> 8)))
/*TODO*///#endif
    public static final int WAVE_OK = 0;
    public static final int WAVE_ERR = 1;
    public static final int WAVE_FMT = 2;

    static {
        for (int i = 0; i < MAX_WAVE; i++) {
            //struct wave_file *w = &wave[i];
            wave[i] = new struct_wave_file();
            wave[i].channel = -1; //channel inits to -1
        }
    }

    /**
     * ***************************************************************************
     * helper functions
     * ***************************************************************************
     */
    static int wave_read(int id) {
        struct_wave_file w = wave[id];
        long /*unsigned*/ offset = 0;
        long /*UINT32*/ filesize, temp32;
        int /*UINT16*/ channels, bits, temp16;
        char[] /*UINT8*/ buf = new char[32];

        if (w.file == null) {
            return WAVE_ERR;
        }
        /* read the core header and make sure it's a WAVE file */
        offset += osd_fread(w.file, buf, 4);
        if (offset < 4) {
            logerror("WAVE read error at offs %d\n", offset);
            return WAVE_ERR;
        }
        if (memcmp(buf, 0, "RIFF", 4) != 0) {
            logerror("WAVE header not 'RIFF'\n");
            return WAVE_FMT;
        }
        /* get the total size */
        offset += osd_fread(w.file, buf, 4);
        if (offset < 8) {
            logerror("WAVE read error at offs %d\n", offset);
            return WAVE_ERR;
        }
        filesize = charArrayToLong(buf);
        logerror("WAVE filesize %u bytes\n", filesize);

        /* read the RIFF file type and make sure it's a WAVE file */
        offset += osd_fread(w.file, buf, 4);
        if (offset < 12) {
            logerror("WAVE read error at offs %d\n", offset);
            return WAVE_ERR;
        }
        if (memcmp(buf, 0, "WAVE", 4) != 0) {
            logerror("WAVE RIFF type not 'WAVE'\n");
            return WAVE_FMT;
        }
        /* seek until we find a format tag */
        while (true) {
            offset += osd_fread(w.file, buf, 4);
            char[] tmp = new char[buf.length];//temp creation
            System.arraycopy(buf, 0, tmp, 0, buf.length);//temp creation
            offset += osd_fread(w.file, buf, 4);//offset += osd_fread(f, &length, 4);
            w.length = (int) charArrayToLong(buf);
            if (memcmp(tmp, 0, "fmt ", 4) == 0) {
                break;
            }

            /* seek to the next block */
            osd_fseek(w.file, (int) w.length, SEEK_CUR);
            offset += w.length;
            if (offset >= filesize) {
                logerror("WAVE no 'fmt ' tag found\n");
                return WAVE_ERR;
            }
        }
        /* read the format -- make sure it is PCM */
        offset += osd_fread_lsbfirst(w.file, buf, 2);
        temp16 = charArrayToInt(buf);
        if (temp16 != 1) {
            logerror("WAVE format %d not supported (not = 1 PCM)\n", temp16);
            return WAVE_ERR;
        }
        logerror("WAVE format %d (PCM)\n", temp16);

        /* number of channels -- only mono is supported */
        offset += osd_fread_lsbfirst(w.file, buf, 2);
        channels = charArrayToInt(buf);
        if (channels != 1 && channels != 2) {
            logerror("WAVE channels %d not supported (only 1 mono or 2 stereo)\n", channels);
            return WAVE_ERR;
        }
        logerror("WAVE channels %d\n", channels);

        /* sample rate */
        offset += osd_fread(w.file, buf, 4);
        w.smpfreq = (int) charArrayToLong(buf);

        logerror("WAVE sample rate %d Hz\n", w.smpfreq);

        /* bytes/second and block alignment are ignored */
        offset += osd_fread(w.file, buf, 6);

        /* bits/sample */
        offset += osd_fread_lsbfirst(w.file, buf, 2);
        bits = charArrayToInt(buf);
        if (bits != 8 && bits != 16) {
            logerror("WAVE %d bits/sample not supported (only 8/16)\n", bits);
            return WAVE_ERR;
        }
        logerror("WAVE bits/sample %d\n", bits);
        w.resolution = bits;

        /* seek past any extra data */
        osd_fseek(w.file, (int) w.length - 16, SEEK_CUR);
        offset += w.length - 16;

        /* seek until we find a data tag */
        while (true) {
            offset += osd_fread(w.file, buf, 4);
            char[] tmp = new char[buf.length];//temp creation
            System.arraycopy(buf, 0, tmp, 0, buf.length);//temp creation
            offset += osd_fread(w.file, buf, 4);//offset += osd_fread(f, &length, 4);
            w.length = (int) charArrayToLong(buf);
            if (memcmp(tmp, 0, "data", 4) == 0) {
                break;
            }

            /* seek to the next block */
            osd_fseek(w.file, (int) w.length, SEEK_CUR);
            offset += w.length;
            if (offset >= filesize) {
                logerror("WAVE not 'data' tag found\n");
                return WAVE_ERR;
            }
        }
        /* allocate the game sample */
        w.data = new byte[w.length];
        /* read the data in */
        if (w.resolution == 8) {
            if (osd_fread(w.file, w.data, w.length) != w.length) {
                logerror("WAVE failed read %d data bytes\n", w.length);
                w.data = null;
                return WAVE_ERR;
            }
            if (channels == 2) {
                throw new UnsupportedOperationException("Unsupported");
                /*TODO*///			UINT8 *src = w->data;
/*TODO*///			INT8 *dst = w->data;
/*TODO*///			logerror("WAVE mixing 8-bit unsigned stereo to 8-bit signed mono\n");
/*TODO*///            /* convert stereo 8-bit data to mono signed samples */
/*TODO*///			for( temp32 = 0; temp32 < w->length/2; temp32++ )
/*TODO*///			{
/*TODO*///				*dst = ((src[0] + src[1]) / 2) ^ 0x80;
/*TODO*///				dst += 1;
/*TODO*///				src += 2;
/*TODO*///			}
/*TODO*///			w->length /= 2;
/*TODO*///            w->data = realloc(w->data, w->length);
/*TODO*///			if( w->data == NULL )
/*TODO*///			{
/*TODO*///				logerror("WAVE failed to malloc %d bytes\n", w->length);
/*TODO*///				return WAVE_ERR;
/*TODO*///			}
            } else {
                /*TODO*///			UINT8 *src = w->data;
/*TODO*///			INT8 *dst = w->data;
                logerror("WAVE converting 8-bit unsigned to 8-bit signed\n");
                /* convert 8-bit data to signed samples */
                for (temp32 = 0; temp32 < w.length; temp32++) {
                    w.data[(int) temp32] ^= 0x80;//TODO RECHECK!
                }
                /*TODO*///			for( temp32 = 0; temp32 < w->length; temp32++ )
/*TODO*///				*dst++ = *src++ ^ 0x80;
            }
        } else {
            throw new UnsupportedOperationException("Unsupported");
            /*TODO*///		/* 16-bit data is fine as-is */
/*TODO*///		if( osd_fread_lsbfirst(w->file, w->data, w->length) != w->length )
/*TODO*///		{
/*TODO*///			logerror("WAVE failed read %d data bytes\n", w->length);
/*TODO*///			free(w->data);
/*TODO*///			return WAVE_ERR;
/*TODO*///        }
/*TODO*///        if( channels == 2 )
/*TODO*///        {
/*TODO*///			INT16 *src = w->data;
/*TODO*///			INT16 *dst = w->data;
/*TODO*///            logerror("WAVE mixing 16-bit stereo to 16-bit mono\n");
/*TODO*///            /* convert stereo 16-bit data to mono */
/*TODO*///			for( temp32 = 0; temp32 < w->length/2; temp32++ )
/*TODO*///			{
/*TODO*///				*dst = ((INT32)src[0] + (INT32)src[1]) / 2;
/*TODO*///				dst += 1;
/*TODO*///				src += 2;
/*TODO*///			}
/*TODO*///			w->length /= 2;
/*TODO*///			w->data = realloc(w->data, w->length);
/*TODO*///			if( w->data == NULL )
/*TODO*///			{
/*TODO*///				logerror("WAVE failed to malloc %d bytes\n", w->length);
/*TODO*///				return WAVE_ERR;
/*TODO*///            }
/*TODO*///        }
/*TODO*///		else
/*TODO*///		{
/*TODO*///			logerror("WAVE using 16-bit signed samples as is\n");
/*TODO*///        }
        }
        w.samples = w.length * 8 / w.resolution;
        logerror("WAVE %d samples - %d:%02d\n", w.samples, (w.samples / w.smpfreq) / 60, (w.samples / w.smpfreq) % 60);

        return WAVE_OK;
    }

    /*TODO*///
/*TODO*///static int wave_write(int id)
/*TODO*///{
/*TODO*///	struct wave_file *w = &wave[id];
/*TODO*///	UINT32 filesize, offset = 0, temp32;
/*TODO*///	UINT16 temp16;
/*TODO*///
/*TODO*///	if( !w->file )
/*TODO*///        return WAVE_ERR;
/*TODO*///
/*TODO*///	while( w->play_pos < w->samples )
/*TODO*///    {
/*TODO*///		*((INT16 *)w->data + w->play_pos) = 0;
/*TODO*///		w->play_pos++;
/*TODO*///	}
/*TODO*///
/*TODO*///    filesize =
/*TODO*///		4 + 	/* 'RIFF' */
/*TODO*///		4 + 	/* size of entire file */
/*TODO*///		8 + 	/* 'WAVEfmt ' */
/*TODO*///		20 +	/* WAVE tag  (including size -- 0x10 in dword) */
/*TODO*///		4 + 	/* 'data' */
/*TODO*///		4 + 	/* size of data */
/*TODO*///		w->length;
/*TODO*///
/*TODO*///    /* write the core header for a WAVE file */
/*TODO*///	offset += osd_fwrite(w->file, "RIFF", 4);
/*TODO*///    if( offset < 4 )
/*TODO*///    {
/*TODO*///		logerror("WAVE write error at offs %d\n", offset);
/*TODO*///		return WAVE_ERR;
/*TODO*///    }
/*TODO*///
/*TODO*///	temp32 = intelLong(filesize) - 8;
/*TODO*///	offset += osd_fwrite(w->file, &temp32, 4);
/*TODO*///
/*TODO*///	/* read the RIFF file type and make sure it's a WAVE file */
/*TODO*///	offset += osd_fwrite(w->file, "WAVE", 4);
/*TODO*///	if( offset < 12 )
/*TODO*///	{
/*TODO*///		logerror("WAVE write error at offs %d\n", offset);
/*TODO*///		return WAVE_ERR;
/*TODO*///	}
/*TODO*///
/*TODO*///	/* write a format tag */
/*TODO*///	offset += osd_fwrite(w->file, "fmt ", 4);
/*TODO*///    if( offset < 12 )
/*TODO*///	{
/*TODO*///		logerror("WAVE write error at offs %d\n", offset);
/*TODO*///		return WAVE_ERR;
/*TODO*///    }
/*TODO*///    /* size of the following 'fmt ' fields */
/*TODO*///    offset += osd_fwrite(w->file, "\x10\x00\x00\x00", 4);
/*TODO*///	if( offset < 16 )
/*TODO*///	{
/*TODO*///		logerror("WAVE write error at offs %d\n", offset);
/*TODO*///		return WAVE_ERR;
/*TODO*///    }
/*TODO*///
/*TODO*///	/* format: PCM */
/*TODO*///	temp16 = 1;
/*TODO*///	offset += osd_fwrite_lsbfirst(w->file, &temp16, 2);
/*TODO*///	if( offset < 18 )
/*TODO*///	{
/*TODO*///		logerror("WAVE write error at offs %d\n", offset);
/*TODO*///		return WAVE_ERR;
/*TODO*///    }
/*TODO*///
/*TODO*///	/* channels: 1 (mono) */
/*TODO*///	temp16 = 1;
/*TODO*///    offset += osd_fwrite_lsbfirst(w->file, &temp16, 2);
/*TODO*///	if( offset < 20 )
/*TODO*///	{
/*TODO*///		logerror("WAVE write error at offs %d\n", offset);
/*TODO*///		return WAVE_ERR;
/*TODO*///    }
/*TODO*///
/*TODO*///	/* sample rate */
/*TODO*///	temp32 = intelLong(w->smpfreq);
/*TODO*///	offset += osd_fwrite(w->file, &temp32, 4);
/*TODO*///	if( offset < 24 )
/*TODO*///	{
/*TODO*///		logerror("WAVE write error at offs %d\n", offset);
/*TODO*///		return WAVE_ERR;
/*TODO*///    }
/*TODO*///
/*TODO*///	/* byte rate */
/*TODO*///	temp32 = intelLong(w->smpfreq * w->resolution / 8);
/*TODO*///	offset += osd_fwrite(w->file, &temp32, 4);
/*TODO*///	if( offset < 28 )
/*TODO*///	{
/*TODO*///		logerror("WAVE write error at offs %d\n", offset);
/*TODO*///		return WAVE_ERR;
/*TODO*///    }
/*TODO*///
/*TODO*///	/* block align (size of one `sample') */
/*TODO*///	temp16 = w->resolution / 8;
/*TODO*///	offset += osd_fwrite_lsbfirst(w->file, &temp16, 2);
/*TODO*///	if( offset < 30 )
/*TODO*///	{
/*TODO*///		logerror("WAVE write error at offs %d\n", offset);
/*TODO*///		return WAVE_ERR;
/*TODO*///    }
/*TODO*///
/*TODO*///	/* block align */
/*TODO*///	temp16 = w->resolution;
/*TODO*///	offset += osd_fwrite_lsbfirst(w->file, &temp16, 2);
/*TODO*///	if( offset < 32 )
/*TODO*///	{
/*TODO*///		logerror("WAVE write error at offs %d\n", offset);
/*TODO*///		return WAVE_ERR;
/*TODO*///    }
/*TODO*///
/*TODO*///	/* 'data' tag */
/*TODO*///	offset += osd_fwrite(w->file, "data", 4);
/*TODO*///	if( offset < 36 )
/*TODO*///	{
/*TODO*///		logerror("WAVE write error at offs %d\n", offset);
/*TODO*///		return WAVE_ERR;
/*TODO*///    }
/*TODO*///
/*TODO*///	/* data size */
/*TODO*///	temp32 = intelLong(w->length);
/*TODO*///	offset += osd_fwrite(w->file, &temp32, 4);
/*TODO*///	if( offset < 40 )
/*TODO*///	{
/*TODO*///		logerror("WAVE write error at offs %d\n", offset);
/*TODO*///		return WAVE_ERR;
/*TODO*///    }
/*TODO*///
/*TODO*///	if( osd_fwrite_lsbfirst(w->file, w->data, w->length) != w->length )
/*TODO*///	{
/*TODO*///		logerror("WAVE write error at offs %d\n", offset);
/*TODO*///		return WAVE_ERR;
/*TODO*///    }
/*TODO*///
/*TODO*///	return WAVE_OK;
/*TODO*///}
/*TODO*///
    static int tape_pos = 0;

    static void wave_display(int id) {
        struct_wave_file w = wave[id];

        if (Math.abs(w.play_pos - tape_pos) > w.smpfreq / 4) {
            String buf = "";
            int x, y, n, t0, t1;

            x = Machine.uixmin + id * Machine.uifontwidth * 16 + 1;
            y = Machine.uiymin + Machine.uiheight - 9;
            n = (w.play_pos * 4 / w.smpfreq) & 3;
            t0 = w.play_pos / w.smpfreq;
            t1 = w.samples / w.smpfreq;
            buf = sprintf("%c%c %2d:%02d [%2d:%02d]", n * 2 + 2, n * 2 + 3, t0 / 60, t0 % 60, t1 / 60, t1 % 60);
            ui_text(Machine.scrbitmap, buf, x, y);
            tape_pos = w.play_pos;
        }
    }

    public static StreamInitPtr wave_sound_update = new StreamInitPtr() {
        public void handler(int id, ShortPtr buffer, int length) {
            struct_wave_file w = wave[id];
            int pos = w.play_pos;
            int count = w.counter;
            short sample = w.play_sample;

            if (w.timer == null || (w.status & WAVE_STATUS_MUTED) != 0) {
                while (length-- > 0) {
                    buffer.writeinc(sample);
                }
                return;
            }
            // System.out.println("Unimplemented sound update!");
            while (length-- != 0) {
                count -= w.smpfreq;
                while (count <= 0) {
                    count += Machine.sample_rate;
                    if (w.resolution == 16) {
                        throw new UnsupportedOperationException("unimplemented");
                        /*TODO*///				sample = *((INT16 *)w->data + pos);
                    } else {
                        sample = (short) (w.data[pos] * 256);//sample = *((INT8 *)w->data + pos)*256;
                    }
                    if (++pos >= w.samples) {
                        pos = w.samples - 1;
                        if (pos < 0) {
                            pos = 0;
                        }
                    }
                }
                buffer.writeinc(sample);
            }
            w.counter = count;
            w.play_pos = pos;
            w.play_sample = sample;

            if (w.display != 0) {
                wave_display(id);
            }
        }
    };

    /**
     * ***************************************************************************
     * WaveSound interface
     * ***************************************************************************
     */
    @Override
    public int start(MachineSound msound) {
        int i;

        intf = (Wave_interface) msound.sound_interface;
        for (i = 0; i < intf.num; i++) {
            //struct wave_file *w = &wave[i];
            String buf = "";

            if (intf.num > 1) {
                buf = sprintf("Cassette #%d", i + 1);
            } else {
                buf += "Cassette";
            }

            wave[i].channel = stream_init(buf, intf.mixing_level[i], Machine.sample_rate, i, wave_sound_update);

            if (wave[i].channel == -1) {
                return 1;
            }
        }

        return 0;
    }

    @Override
    public void stop() {
        int i;

        for (i = 0; i < intf.num; i++) {
            wave[i].channel = -1;
        }
    }

    @Override
    public void update() {

        int i;

        for (i = 0; i < intf.num; i++) {
            if (wave[i].channel != -1) {
                stream_update(wave[i].channel, 0);
            }
        }
    }

    @Override
    public void reset() {
        //no action required
    }

    /**
     * ***************************************************************************
     * IODevice interface functions
     * ***************************************************************************
     */

    /*
     * return info about a wave device
     */
    public static io_infoPtr wave_info = new io_infoPtr() {
        public void handler(int id, int whatinfo) {
            //return NULL;
        }
    };
    /*TODO*////*
/*TODO*/// * You can use this default handler if you don't want
/*TODO*/// * to support your own file types with the fill_wave()
/*TODO*/// * extension
/*TODO*/// */
/*TODO*///int wave_init(int id, const char *name)
/*TODO*///{
/*TODO*///	void *file;
/*TODO*///	if( !name || strlen(name) == 0 )
/*TODO*///		return INIT_OK;
/*TODO*///	file = osd_fopen(Machine->gamedrv->name, name, OSD_FILETYPE_IMAGE_RW, OSD_FOPEN_READ);
/*TODO*///	if( file )
/*TODO*///	{
/*TODO*///		struct wave_args wa = {0,};
/*TODO*///		wa.file = file;
/*TODO*///		wa.display = 1;
/*TODO*///		if( device_open(IO_CASSETTE,id,0,&wa) )
/*TODO*///			return INIT_FAILED;
/*TODO*///		return INIT_OK;
/*TODO*///    }
/*TODO*///	return INIT_FAILED;
/*TODO*///}
/*TODO*///
/*TODO*///void wave_exit(int id)
/*TODO*///{
/*TODO*///	wave_close(id);
/*TODO*///}
/*TODO*///
    public static io_statusPtr wave_status = new io_statusPtr() {

        public int handler(int id, int newstatus) {
            /* wave status has the following bitfields:
	 *
	 *  Bit 2:  Inhibit Motor (1=inhibit 0=noinhibit)
	 *	Bit 1:	Mute (1=mute 0=nomute)
	 *	Bit 0:	Motor (1=on 0=off)
	 *
	 *  Bit 0 is usually set by the tape control, and bit 2 is usually set by
	 *  the driver
	 *
	 *	Also, you can pass -1 to have it simply return the status
             */
            struct_wave_file w = wave[id];

            if (w.file == null) {
                return 0;
            }

            if (newstatus != -1) {
                w.status = newstatus;

                if ((newstatus & WAVE_STATUS_MOTOR_INHIBIT) != 0) {
                    newstatus = 0;
                } else {
                    newstatus &= WAVE_STATUS_MOTOR_ENABLE;
                }

                if (newstatus != 0 && w.timer == null) {
                    w.timer = timer_set(TIME_NEVER, 0, null);
                } else if (newstatus == 0 && w.timer != null) {
                    if (w.timer != null) {
                        w.offset += (timer_timeelapsed(w.timer) * w.smpfreq + 0.5);
                    }
                    timer_remove(w.timer);
                    w.timer = null;
                    schedule_full_refresh();
                }
            }
            return (w.timer != null ? WAVE_STATUS_MOTOR_ENABLE : 0)
                    | ((w.status & WAVE_STATUS_MOTOR_INHIBIT) != 0 ? w.status : w.status & ~WAVE_STATUS_MOTOR_ENABLE);
        }
    };
    public static io_openPtr wave_open = new io_openPtr() {

        public int handler(int id, int mode, Object args) {
            System.out.println("wave_open");
            struct_wave_file w = wave[id];
            wave_args wa = (wave_args) args;
            int result;

            /* wave already opened? */
            if (w.file != null) {
                wave_close.handler(id);
            }
            w.file = wa.file;
            /*TODO*///	w->mode = mode;
/*TODO*///	w->fill_wave = wa->fill_wave;
/*TODO*///	w->smpfreq = wa->smpfreq;
            w.display = wa.display;
            /*TODO*///
/*TODO*///	if( w->mode )
/*TODO*///	{
/*TODO*///        w->resolution = 16;
/*TODO*///		w->samples = w->smpfreq;
/*TODO*///		w->length = w->samples * w->resolution / 8;
/*TODO*///		w->data = malloc(w->length);
/*TODO*///		if( !w->data )
/*TODO*///		{
/*TODO*///			logerror("WAVE malloc(%d) failed\n", w->length);
/*TODO*///			memset(w, 0, sizeof(struct wave_file));
/*TODO*///			return WAVE_ERR;
/*TODO*///		}
/*TODO*///		return INIT_OK;
/*TODO*///    }
/*TODO*///	else
/*TODO*///	{
            result = wave_read(id);
            if (result == WAVE_OK) {
                /* return sample frequency in the user supplied structure */
                wa.smpfreq = w.smpfreq;
                w.offset = 0;
                return INIT_OK;
            }
            /*TODO*///
/*TODO*///		if( result == WAVE_FMT )
/*TODO*///		{
/*TODO*///			UINT8 *data;
/*TODO*///			int bytes, pos, length;
/*TODO*///
/*TODO*///			/* User supplied fill_wave function? */
/*TODO*///			if( w->fill_wave == NULL )
/*TODO*///			{
/*TODO*///				logerror("WAVE no fill_wave callback, failing now\n");
/*TODO*///				return WAVE_ERR;
/*TODO*///			}
/*TODO*///
/*TODO*///			logerror("WAVE creating wave using fill_wave() callback\n");
/*TODO*///
/*TODO*///			/* sanity check: default chunk size is one byte */
/*TODO*///			if( wa->chunk_size == 0 )
/*TODO*///			{
/*TODO*///				wa->chunk_size = 1;
/*TODO*///				logerror("WAVE chunk_size defaults to %d\n", wa->chunk_size);
/*TODO*///			}
/*TODO*///			if( wa->smpfreq == 0 )
/*TODO*///			{
/*TODO*///				wa->smpfreq = 11025;
/*TODO*///				logerror("WAVE smpfreq defaults to %d\n", w->smpfreq);
/*TODO*///			}
/*TODO*///
/*TODO*///			/* allocate a buffer for the binary data */
/*TODO*///			data = malloc(wa->chunk_size);
/*TODO*///			if( !data )
/*TODO*///			{
/*TODO*///				free(w->data);
/*TODO*///				/* zap the wave structure */
/*TODO*///				memset(&wave[id], 0, sizeof(struct wave_file));
/*TODO*///				return WAVE_ERR;
/*TODO*///			}
/*TODO*///
/*TODO*///			/* determine number of samples */
/*TODO*///			length =
/*TODO*///				wa->header_samples +
/*TODO*///				((osd_fsize(w->file) + wa->chunk_size - 1) / wa->chunk_size) * wa->chunk_samples +
/*TODO*///				wa->trailer_samples;
/*TODO*///
/*TODO*///			w->smpfreq = wa->smpfreq;
/*TODO*///			w->resolution = 16;
/*TODO*///			w->samples = length;
/*TODO*///			w->length = length * 2;   /* 16 bits per sample */
/*TODO*///
/*TODO*///			w->data = malloc(w->length);
/*TODO*///			if( !w->data )
/*TODO*///			{
/*TODO*///				logerror("WAVE failed to malloc %d bytes\n", w->length);
/*TODO*///				/* zap the wave structure */
/*TODO*///				memset(&wave[id], 0, sizeof(struct wave_file));
/*TODO*///				return WAVE_ERR;
/*TODO*///			}
/*TODO*///			logerror("WAVE creating max %d:%02d samples (%d) at %d Hz\n", (w->samples/w->smpfreq)/60, (w->samples/w->smpfreq)%60, w->samples, w->smpfreq);
/*TODO*///
/*TODO*///			pos = 0;
/*TODO*///			/* if there has to be a header */
/*TODO*///			if( wa->header_samples > 0 )
/*TODO*///			{
/*TODO*///				length = (*w->fill_wave)((INT16 *)w->data + pos, w->samples - pos, CODE_HEADER);
/*TODO*///				if( length < 0 )
/*TODO*///				{
/*TODO*///					logerror("WAVE conversion aborted at header\n");
/*TODO*///					free(w->data);
/*TODO*///					/* zap the wave structure */
/*TODO*///					memset(&wave[id], 0, sizeof(struct wave_file));
/*TODO*///					return WAVE_ERR;
/*TODO*///				}
/*TODO*///				logerror("WAVE header %d samples\n", length);
/*TODO*///				pos += length;
/*TODO*///			}
/*TODO*///
/*TODO*///			/* convert the file data to samples */
/*TODO*///			bytes = 0;
/*TODO*///			osd_fseek(w->file, 0, SEEK_SET);
/*TODO*///			while( pos < w->samples )
/*TODO*///			{
/*TODO*///				length = osd_fread(w->file, data, wa->chunk_size);
/*TODO*///				if( length == 0 )
/*TODO*///					break;
/*TODO*///				bytes += length;
/*TODO*///				length = (*w->fill_wave)((INT16 *)w->data + pos, w->samples - pos, data);
/*TODO*///				if( length < 0 )
/*TODO*///				{
/*TODO*///					logerror("WAVE conversion aborted at %d bytes (%d samples)\n", bytes, pos);
/*TODO*///					free(w->data);
/*TODO*///					/* zap the wave structure */
/*TODO*///					memset(&wave[id], 0, sizeof(struct wave_file));
/*TODO*///					return WAVE_ERR;
/*TODO*///				}
/*TODO*///				pos += length;
/*TODO*///			}
/*TODO*///			logerror("WAVE converted %d data bytes to %d samples\n", bytes, pos);
/*TODO*///
/*TODO*///			/* if there has to be a trailer */
/*TODO*///			if( wa->trailer_samples )
/*TODO*///			{
/*TODO*///				if( pos < w->samples )
/*TODO*///				{
/*TODO*///					length = (*w->fill_wave)((INT16 *)w->data + pos, w->samples - pos, CODE_TRAILER);
/*TODO*///					if( length < 0 )
/*TODO*///					{
/*TODO*///						logerror("WAVE conversion aborted at trailer\n");
/*TODO*///						free(w->data);
/*TODO*///						/* zap the wave structure */
/*TODO*///						memset(&wave[id], 0, sizeof(struct wave_file));
/*TODO*///						return WAVE_ERR;
/*TODO*///					}
/*TODO*///					logerror("WAVE trailer %d samples\n", length);
/*TODO*///					pos += length;
/*TODO*///				}
/*TODO*///			}
/*TODO*///
/*TODO*///			if( pos < w->samples )
/*TODO*///			{
/*TODO*///				/* what did the fill_wave() calls really fill into the buffer? */
/*TODO*///				w->samples = pos;
/*TODO*///				w->length = pos * 2;   /* 16 bits per sample */
/*TODO*///				w->data = realloc(w->data, w->length);
/*TODO*///				/* failure in the last step? how sad... */
/*TODO*///				if( !w->data )
/*TODO*///				{
/*TODO*///					logerror("WAVE realloc(%d) failed\n", w->length);
/*TODO*///					/* zap the wave structure */
/*TODO*///					memset(&wave[id], 0, sizeof(struct wave_file));
/*TODO*///					return WAVE_ERR;
/*TODO*///				}
/*TODO*///			}
/*TODO*///			logerror("WAVE %d samples - %d:%02d\n", w->samples, (w->samples/w->smpfreq)/60, (w->samples/w->smpfreq)%60);
/*TODO*///			/* hooray! :-) */
/*TODO*///			return INIT_OK;
/*TODO*///		}
/*TODO*///	}
            return WAVE_ERR;
        }
    };
    public static io_closePtr wave_close = new io_closePtr() {

        public void handler(int id) {
            System.out.println("Unimplemented wave_close function!");//TODO REMOVE
/*TODO*///	struct wave_file *w = &wave[id];
/*TODO*///
/*TODO*///    if( !w->file )
/*TODO*///		return;
/*TODO*///
/*TODO*///    if( w->timer )
/*TODO*///	{
/*TODO*///		if( w->channel != -1 )
/*TODO*///			stream_update(w->channel, 0);
/*TODO*///		w->samples = w->play_pos;
/*TODO*///		w->length = w->samples * w->resolution / 8;
/*TODO*///		timer_remove(w->timer);
/*TODO*///		w->timer = NULL;
/*TODO*///	}
/*TODO*///
/*TODO*///    if( w->mode )
/*TODO*///	{
/*TODO*///		wave_output(id,0);
/*TODO*///		wave_write(id);
/*TODO*///		w->mode = 0;
/*TODO*///	}
/*TODO*///
/*TODO*///    if( w->data )
/*TODO*///		free(w->data);
/*TODO*///    w->data = NULL;
/*TODO*///
/*TODO*///	if (w->file) {
/*TODO*///		osd_fclose(w->file);
/*TODO*///		w->file = NULL;
/*TODO*///	}
/*TODO*///	w->offset = 0;
/*TODO*///	w->play_pos = 0;
/*TODO*///	w->record_pos = 0;
/*TODO*///	w->counter = 0;
/*TODO*///	w->smpfreq = 0;
/*TODO*///	w->resolution = 0;
/*TODO*///	w->samples = 0;
/*TODO*///	w->length = 0;
        }
    };
    public static io_seekPtr wave_seek = new io_seekPtr() {
        public int handler(int id, int offset, int whence) {
            struct_wave_file w = wave[id];
            int/*UINT32*/ pos = 0;

            if (w.file == null) {
                return pos;
            }

            switch (whence) {
                case SEEK_SET:
                    w.offset = offset;
                    break;
                case SEEK_END:
                    w.offset = w.samples - 1;
                    break;
                case SEEK_CUR:
                    if (w.timer != null) {
                        pos = (int) (w.offset + (timer_timeelapsed(w.timer) * w.smpfreq + 0.5));
                    }
                    w.offset = pos + offset;
                    if (w.offset < 0) {
                        w.offset = 0;
                    }
                    if (w.offset >= w.length) {
                        w.offset = w.length - 1;
                    }
            }
            w.play_pos = w.record_pos = w.offset;

            if (w.timer != null) {
                timer_remove(w.timer);
                w.timer = timer_set(TIME_NEVER, 0, null);
            }

            return w.offset;
        }
    };
    public static io_tellPtr wave_tell = new io_tellPtr() {
        public int handler(int id) {
            struct_wave_file w = wave[id];
            int/*UINT32*/ pos = 0;
            if (w.timer != null) {
                pos = (int) (w.offset + (timer_timeelapsed(w.timer) * w.smpfreq + 0.5));
            }
            if (pos >= w.samples) {
                pos = w.samples - 1;
            }
            return pos;
        }
    };
    public static io_inputPtr wave_input = new io_inputPtr() {
        public int handler(int id) {
            struct_wave_file w = wave[id];
            int/*UINT32*/ pos = 0;
            int level = 0;

            if (w.file == null) {
                return level;
            }

            if (w.channel != -1) {
                stream_update(w.channel, 0);
            }

            if (w.timer != null) {
                pos = (int) (w.offset + (timer_timeelapsed(w.timer) * w.smpfreq + 0.5));
                if (pos >= w.samples) {
                    pos = w.samples - 1;
                }
                if (pos >= 0) {
                    if (w.resolution == 16) {
                        throw new UnsupportedOperationException("Unimplemented");
                        /*TODO*///				level = *((INT16 *)w->data + pos);
                    } else {
                        level = 256 * w.data[pos];//level = 256 * *((INT8 *)w->data + pos);
                    }
                }
            }
            if (w.display != 0) {
                wave_display(id);
            }
            return level;
        }
    };
    public static io_outputPtr wave_output = new io_outputPtr() {
        public void handler(int id, int data) {
            System.out.println("Unimplemented wave_output function");//TODO Remove
/*TODO*///	struct wave_file *w = &wave[id];
/*TODO*///	UINT32 pos = 0;
/*TODO*///
/*TODO*///	if( !w->file )
/*TODO*///		return;
/*TODO*///
/*TODO*///    if( !w->mode )
/*TODO*///		return;
/*TODO*///
/*TODO*///	if( data == w->record_sample )
/*TODO*///		return;
/*TODO*///
/*TODO*///	if( w->channel != -1 )
/*TODO*///		stream_update(w->channel, 0);
/*TODO*///
/*TODO*///    if( w->timer )
/*TODO*///    {
/*TODO*///		pos = w->offset + (timer_timeelapsed(w->timer) * w->smpfreq + 0.5);
/*TODO*///		if( pos >= w->samples )
/*TODO*///        {
/*TODO*///			/* add at least one second of data */
/*TODO*///			if( pos - w->samples < w->smpfreq )
/*TODO*///				w->samples += w->smpfreq;
/*TODO*///			else
/*TODO*///				w->samples = pos;	/* more than one second */
/*TODO*///            w->length = w->samples * w->resolution / 8;
/*TODO*///            w->data = realloc(w->data, w->length);
/*TODO*///            if( !w->data )
/*TODO*///            {
/*TODO*///                logerror("WAVE realloc(%d) failed\n", w->length);
/*TODO*///                memset(w, 0, sizeof(struct wave_file));
/*TODO*///                return;
/*TODO*///            }
/*TODO*///        }
/*TODO*///		while( w->record_pos < pos )
/*TODO*///        {
/*TODO*///			if( w->resolution == 16 )
/*TODO*///				*((INT16 *)w->data + w->record_pos) = w->record_sample;
/*TODO*///			else
/*TODO*///				*((INT8 *)w->data + w->record_pos) = w->record_sample / 256;
/*TODO*///			w->record_pos++;
/*TODO*///        }
/*TODO*///    }
/*TODO*///
/*TODO*///    if( w->display )
/*TODO*///        wave_display(id);
/*TODO*///
/*TODO*///    w->record_sample = data;
        }
    };
    public static io_input_chunkPtr wave_input_chunk = new io_input_chunkPtr() {
        public int handler(int id, Object dst, int chunks) {
            System.out.println("Unimplemented wave_input_chunk function");//TODO Remove
            return 0;//todo remove!!
/*TODO*///	struct wave_file *w = &wave[id];
/*TODO*///	UINT32 pos = 0;
/*TODO*///
/*TODO*///	if( !w->file )
/*TODO*///		return 0;
/*TODO*///
/*TODO*///    if( w->timer )
/*TODO*///	{
/*TODO*///		pos = w->offset + (timer_timeelapsed(w->timer) * w->smpfreq + 0.5);
/*TODO*///		if( pos >= w->samples )
/*TODO*///			pos = w->samples - 1;
/*TODO*///	}
/*TODO*///
/*TODO*///    if( pos + count >= w->samples )
/*TODO*///		count = w->samples - pos - 1;
/*TODO*///
/*TODO*///    if( count > 0 )
/*TODO*///	{
/*TODO*///		if( w->resolution == 16 )
/*TODO*///			memcpy(dst, (INT16 *)w->data + pos, count * sizeof(INT16));
/*TODO*///		else
/*TODO*///			memcpy(dst, (INT8 *)w->data + pos, count * sizeof(INT8));
/*TODO*///	}
/*TODO*///
/*TODO*///    return count;
        }
    };
    public static io_output_chunkPtr wave_output_chunk = new io_output_chunkPtr() {
        public void handler(int id, Object dst, int chunks) {
            System.out.println("Unimplemented wave_output_chunk function");//TODO Remove
            /*TODO*///	struct wave_file *w = &wave[id];
/*TODO*///	UINT32 pos = 0;
/*TODO*///
/*TODO*///	if( !w->file )
/*TODO*///		return 0;
/*TODO*///
/*TODO*///    if( w->timer )
/*TODO*///	{
/*TODO*///		pos = w->offset + (timer_timeelapsed(w->timer) * w->smpfreq + 0.5);
/*TODO*///		if( pos >= w->length )
/*TODO*///			pos = w->length - 1;
/*TODO*///	}
/*TODO*///
/*TODO*///    if( pos + count >= w->length )
/*TODO*///	{
/*TODO*///		/* add space for new data */
/*TODO*///		w->samples += count - pos;
/*TODO*///		w->length = w->samples * w->resolution / 8;
/*TODO*///		w->data = realloc(w->data, w->length);
/*TODO*///		if( !w->data )
/*TODO*///		{
/*TODO*///			logerror("WAVE realloc(%d) failed\n", w->length);
/*TODO*///			memset(w, 0, sizeof(struct wave_file));
/*TODO*///			return 0;
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///    if( count > 0 )
/*TODO*///	{
/*TODO*///		if( w->resolution == 16 )
/*TODO*///			memcpy((INT16 *)w->data + pos, src, count * sizeof(INT16));
/*TODO*///		else
/*TODO*///			memcpy((INT8 *)w->data + pos, src, count * sizeof(INT8));
/*TODO*///	}
/*TODO*///
/*TODO*///    return count;
        }
    };
}
