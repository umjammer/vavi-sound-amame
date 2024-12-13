/***************************************************************************

  cbm vc20/c64
  cbm c16 series (other physical representation)
  tape/cassette/datassette emulation

***************************************************************************/
/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.machine;

import static arcadeflex056.fileio.*;
import static arcadeflex056.fucPtr.*;
import static arcadeflex056.osdepend.logerror;
import static common.libc.cstdio.*;
import static common.libc.cstring.*;
import static common.ptr.*;
import static consoleflex056.funcPtr.*;
import static mame056.commonH.*;
import static mame056.mame.*;
import static mame056.osdependH.*;
import static mame056.sound.dac.*;
import static mame056.sound.dacH.*;
import static mame056.timer.*;
import static mame056.timerH.*;
import static mess056.deviceH.*;
import static mess056.mess.*;
import static mess056.messH.*;
import static mess056.includes.vc20tapeH.*;

import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import static mame056.usrintrf.ui_text;

public class vc20tape
{
	
	
	public static int VERBOSE_DBG = 0;
	
	public static DACinterface vc20tape_sound_interface = new DACinterface
        (
		1,
		new int[]{25}
	);
	
	public static int TONE_ON_VALUE = 0xff;
        
        public static final int TAPE_WAV = 1;
	public static final int TAPE_PRG = 2;
	public static final int TAPE_ZIP = 3;
	
	/* write line high active, */
	/* read line low active!? */
	
	public static class _tape
	{
		public int on, noise;
		public int play, record;
	
		public int data;
		public int motor;
		public WriteHandlerPtr read_callback;
	
		public int type;						   /* 0 nothing */
	};
        
        public static _tape tape = new _tape();
	
	/* these are the values for wav files */
	public static class _wav
	{
		public int state;
		public timer_entry timer;
		public int image_type;
                public int image_id;
		public int pos;
		public GameSample sample;
	};
        
        public static _wav wav = new _wav();
        
        public static long  VC20_SHORT		= (long)(176e-6);
	public static long  VC20_MIDDLE		= (long)(256e-6);
	public static long  VC20_LONG		= (long)(336e-6);
	public static long  C16_SHORT           = (long)(246e-6);		   /* messured */
	public static long  C16_MIDDLE          = (long)(483e-6);
	public static long  C16_LONG            = (long)(965e-6);
	public static long  PCM_SHORT(){	return (prg.c16 != 0 ?C16_SHORT:VC20_SHORT); }
	public static long  PCM_MIDDLE(){	return (prg.c16 != 0 ?C16_MIDDLE:VC20_MIDDLE); }
	public static long  PCM_LONG(){         return (prg.c16 != 0 ?C16_LONG:VC20_LONG); }
	
	/* these are the values for prg files */
	public static class _prg
	{
		public int state;
	
		/* these values are shared with the zip driver */
		public timer_entry timer;
		public int image_type;
                public int image_id;
		public int pos;
	
		public int c16;
		public UBytePtr prg;
		public int length;
		public int stateblock, stateheader, statebyte, statebit;
		public int prgdata;
		public String name;					   /*name for cbm */
		public int chksum;
		public double lasttime;
	};
        
        public static _prg prg = new _prg();
	
	/* these are values for zip files */
	public static class _zip
	{
		public int image_type;
                public int image_id;
		public int state;
		public ZipFile zip;
		public ZipEntry zipentry;
	};
        
        public static _zip zip = new _zip();
	
	/* from sound/samples.c no changes (static declared) */
	/* readsamples not useable (loads files only from sample or game directory) */
	/* and doesn't search the rompath */
	/*TODO*///#ifdef LSB_FIRST
	/*TODO*///#define intelLong(x) (x)
	/*TODO*///#else
	public static long intelLong(long x){ 
            //return (((x << 24) | (((long) x) >> 24) | (( x & 0x0000ff00) << 8) | (( x & 0x00ff0000) >> 8)));
            return x;
        }
	/*TODO*///#endif
	static GameSample vc20_read_wav_sample (Object f)
	{
		long offset = 0;
		char[] length = new char[4];
                char[] rate = new char[4]; 
                char[] filesize = new char[4];
                int temp32;
		char[] bits = new char[2];
                char[] temp16 = new char[2];
		char[] buf = new char[32];
		GameSample result;
	
		/* read the core header and make sure it's a WAVE file */
		offset += osd_fread (f, buf, 4);
		if (offset < 4)
			return null;
		if (memcmp (buf, "RIFF".toCharArray(), 4) != 0)
			return null;
	
		/* get the total size */
		offset += osd_fread (f, filesize, 4);
		if (offset < 8)
			return null;
		filesize[0] = (char) intelLong (filesize[0]);
	
		/* read the RIFF file type and make sure it's a WAVE file */
		offset += osd_fread (f, buf, 4);
		if (offset < 12)
			return null;
		if (memcmp (buf, "WAVE".toCharArray(), 4) != 0)
			return null;
	
		/* seek until we find a format tag */
		while (true)
		{
			offset += osd_fread (f, buf, 4);
			offset += osd_fread (f, length, 4);
			length[0] = (char) intelLong (length[0]);
			if (memcmp (buf, "fmt ".toCharArray(), 4) == 0)
				break;
	
			/* seek to the next block */
			osd_fseek (f, length[0], SEEK_CUR);
			offset += length[0];
			if (offset >= filesize[0])
				return null;
		}
	
		/* read the format -- make sure it is PCM */
		offset += osd_fread_lsbfirst (f, temp16, 2);
		if (temp16[0] != 1)
			return null;
	
		/* number of channels -- only mono is supported */
		offset += osd_fread_lsbfirst (f, temp16, 2);
		if (temp16[0] != 1)
			return null;
	
		/* sample rate */
		offset += osd_fread (f, rate, 4);
		rate[0] = (char) intelLong (rate[0]);
	
		/* bytes/second and block alignment are ignored */
		offset += osd_fread (f, buf, 6);
	
		/* bits/sample */
		offset += osd_fread_lsbfirst (f, bits, 2);
		if (bits[0] != 8 && bits[0] != 16)
			return null;
	
		/* seek past any extra data */
		osd_fseek (f, length[0] - 16, SEEK_CUR);
		offset += length[0] - 16;
	
		/* seek until we find a data tag */
		while (true)
		{
			offset += osd_fread (f, buf, 4);
			offset += osd_fread (f, length, 4);
			length[0] = (char) intelLong (length[0]);
			if (memcmp (buf, "data".toCharArray(), 4) == 0)
				break;
	
			/* seek to the next block */
			osd_fseek (f, length[0], SEEK_CUR);
			offset += length[0];
			if (offset >= filesize[0])
				return null;
		}
	
		/* allocate the game sample */
		result = new GameSample(length[0]);
	
		if (result == null)
			return null;
	
		/* fill in the sample data */
		result.length = length[0];
		result.smpfreq = rate[0];
		result.resolution = bits[0];
	
		/* read the data in */
		if (bits[0] == 8)
		{
			osd_fread (f, result.data, length[0]);
	
			/* convert 8-bit data to signed samples */
			for (temp32 = 0; temp32 < length[0]; temp32++)
				result.data[temp32] -= 0x80;
		}
		else
		{
			/* 16-bit data is fine as-is */
			osd_fread_lsbfirst (f, result.data, length[0]);
		}
	
		return result;
	}
	
	static void vc20_wav_state ()
	{
		switch (wav.state)
		{
		case 0:
			break;						   /* not inited */
		case 1:						   /* off */
			if (tape.on != 0)
			{
				wav.state = 2;
				break;
			}
			break;
		case 2:						   /* on */
			if (tape.on == 0)
			{
				wav.state = 1;
				tape.play = 0;
				tape.record = 0;
				DAC_data_w(0, 0);
				break;
			}
			if (tape.motor!=0 && tape.play!=0)
			{
				wav.state = 3;
				wav.timer = timer_pulse(1.0 / wav.sample.smpfreq, 0, vc20_wav_timer);
				break;
			}
			if (tape.motor!=0 && tape.record!=0)
			{
				wav.state = 4;
				break;
			}
			break;
		case 3:						   /* reading */
			if (tape.on==0)
			{
				wav.state = 1;
				tape.play = 0;
				tape.record = 0;
				DAC_data_w(0, 0);
				if (wav.timer != null)
					timer_remove (wav.timer);
				break;
			}
			if (tape.motor==0 || tape.play==0)
			{
				wav.state = 2;
				if (wav.timer != null)
					timer_remove (wav.timer);
				DAC_data_w (0, 0);
				break;
			}
			break;
		case 4:						   /* saving */
			if (tape.on==0)
			{
				wav.state = 1;
				tape.play = 0;
				tape.record = 0;
				DAC_data_w (0, 0);
				break;
			}
			if (tape.motor==0 || tape.record==0)
			{
				wav.state = 2;
				DAC_data_w (0, 0);
				break;
			}
			break;
		}
	}
	
	static void vc20_wav_open (int image_type, int image_id)
	{
		Object fp;
	
		fp = osd_fopen (Machine.gamedrv.name, device_filename(image_type,image_id), OSD_FILETYPE_IMAGE_R, 0);
		if (fp == null)
		{
			logerror("tape %s file not found\n", device_filename(image_type,image_id));
			return;
		}
		if ((wav.sample = vc20_read_wav_sample (fp)) == null)
		{
			logerror("tape %s could not be loaded\n", device_filename(image_type,image_id));
			osd_fclose (fp);
			return;
		}
		logerror("tape %s loaded\n", device_filename(image_type,image_id));
		osd_fclose (fp);
	
		wav.image_type = image_type;
	    wav.image_id = image_id;
		tape.type = TAPE_WAV;
		wav.pos = 0;
		tape.on = 1;
		wav.state = 2;
	}
	
	static void vc20_wav_write (int data)
	{
		if (tape.noise != 0)
			DAC_data_w (0, data);
	}
	
	static timer_callback vc20_wav_timer = new timer_callback() {
            public void handler(int data) {
                if (wav.sample.resolution == 8)
		{
			tape.data = wav.sample.data[wav.pos] > 0x0 ? 1 : 0;
			wav.pos++;
			if (wav.pos >= wav.sample.length)
			{
				wav.pos = 0;
				tape.play = 0;
			}
		}
		else
		{
			tape.data = ((wav.sample.data))[wav.pos] > 0x0 ? 1 : 0;
			wav.pos++;
			if (wav.pos * 2 >= wav.sample.length)
			{
				wav.pos = 0;
				tape.play = 0;
			}
		}
		if (tape.noise != 0)
			DAC_data_w(0, tape.data!=0 ? TONE_ON_VALUE : 0);
		if (tape.read_callback != null)
			tape.read_callback.handler(0, tape.data);
		/*    vc20_wav_state(); // removing timer in timer puls itself hangs */
            }
        };
	
	static void vc20_prg_state ()
	{
		switch (prg.state)
		{
		case 0:
			break;						   /* not inited */
		case 1:						   /* off */
			if (tape.on != 0)
			{
				prg.state = 2;
				break;
			}
			break;
		case 2:						   /* on */
			if (tape.on == 0)
			{
				prg.state = 1;
				tape.play = 0;
				tape.record = 0;
				DAC_data_w (0, 0);
				break;
			}
			if (tape.motor!=0 && tape.play!=0)
			{
				prg.state = 3;
				prg.timer = timer_set (0.0, 0, vc20_prg_timer);
				break;
			}
			if (tape.motor!=0 && tape.record!=0)
			{
				prg.state = 4;
				break;
			}
			break;
		case 3:						   /* reading */
			if (tape.on==0)
			{
				prg.state = 1;
				tape.play = 0;
				tape.record = 0;
				DAC_data_w (0, 0);
				if (prg.timer != null)
					timer_remove (prg.timer);
				break;
			}
			if (tape.motor==0 || tape.play==0)
			{
				prg.state = 2;
				if (prg.timer != null)
					timer_remove (prg.timer);
				DAC_data_w (0, 0);
				break;
			}
			break;
		case 4:						   /* saving */
			if (tape.on==0)
			{
				prg.state = 1;
				tape.play = 0;
				tape.record = 0;
				DAC_data_w (0, 0);
				break;
			}
			if (tape.motor==0 || tape.record==0)
			{
				prg.state = 2;
				DAC_data_w (0, 0);
				break;
			}
			break;
		}
	}
	
	static void vc20_prg_open (int image_type, int image_id)
	{
		String name;
                Object fp;
		int i;
	
		fp = osd_fopen (Machine.gamedrv.name, device_filename(image_type,image_id), OSD_FILETYPE_IMAGE_R, 0);
		if (fp == null)
		{
			logerror("tape %s file not found\n", device_filename(image_type,image_id));
			return;
		}
		prg.length = osd_fsize (fp);
		if ((prg.prg = new UBytePtr(prg.length)) == null)
		{
			logerror("tape %s could not be loaded\n", device_filename(image_type,image_id));
			osd_fclose (fp);
			return;
		}
		osd_fread (fp, prg.prg, prg.length);
		logerror("tape %s loaded\n", device_filename(image_type,image_id));
		osd_fclose (fp);
	
		name = device_filename(image_type,image_id);
                /*TODO*///for (i = 0; name[i] != 0; i++)
                /*TODO*///            prg.name[i] = toupper (name[i]);
                /*TODO*///    for (; i < 16; i++)
                /*TODO*///            prg.name[i] = ' ';
                
                prg.name = name.toUpperCase();
	
		prg.image_type = image_type;
                prg.image_id = image_id;
		prg.stateblock = 0;
		prg.stateheader = 0;
		prg.statebyte = 0;
		prg.statebit = 0;
		tape.type = TAPE_PRG;
		tape.on = 1;
		prg.state = 2;
		prg.pos = 0;
	}
	
	static void vc20_prg_write (int data)
	{
	/*TODO*///#if 0
	/*TODO*///	/* this was used to decode cbms tape format, but could */
/*TODO*///		/* be converted to a real program writer */
/*TODO*///		/* c16: be sure the cpu clock is about 1.8 MHz (when screen is off) */
/*TODO*///		static int count = 0;
/*TODO*///		static int old = 0;
/*TODO*///		static double time = 0;
/*TODO*///		static int bytecount = 0, byte;
/*TODO*///	
/*TODO*///		if (old != data)
/*TODO*///		{
/*TODO*///			double neu = timer_get_time ();
/*TODO*///			int diff = (neu - time) * 1000000;
/*TODO*///	
/*TODO*///			count++;
/*TODO*///			logerror("%f %d %s %d\n", (PCM_LONG + PCM_MIDDLE) / 2,
/*TODO*///						 bytecount, old ? "high" : "low",
/*TODO*///						 diff);
/*TODO*///			if (old)
/*TODO*///			{
/*TODO*///				if (count > 0 /*27000 */ )
/*TODO*///				{
/*TODO*///					switch (bytecount)
/*TODO*///					{
/*TODO*///					case 0:
/*TODO*///						if (diff > (PCM_LONG + PCM_MIDDLE) * 1e6 / 2)
/*TODO*///						{
/*TODO*///							bytecount++;
/*TODO*///							byte = 0;
/*TODO*///						}
/*TODO*///						break;
/*TODO*///					case 1:
/*TODO*///					case 3:
/*TODO*///					case 5:
/*TODO*///					case 7:
/*TODO*///					case 9:
/*TODO*///					case 11:
/*TODO*///					case 13:
/*TODO*///					case 15:
/*TODO*///					case 17:
/*TODO*///						bytecount++;
/*TODO*///						break;
/*TODO*///					case 2:
/*TODO*///						if (diff > (PCM_MIDDLE + PCM_SHORT) * 1e6 / 2)
/*TODO*///							byte |= 1;
/*TODO*///						bytecount++;
/*TODO*///						break;
/*TODO*///					case 4:
/*TODO*///						if (diff > (PCM_MIDDLE + PCM_SHORT) * 1e6 / 2)
/*TODO*///							byte |= 2;
/*TODO*///						bytecount++;
/*TODO*///						break;
/*TODO*///					case 6:
/*TODO*///						if (diff > (PCM_MIDDLE + PCM_SHORT) * 1e6 / 2)
/*TODO*///							byte |= 4;
/*TODO*///						bytecount++;
/*TODO*///						break;
/*TODO*///					case 8:
/*TODO*///						if (diff > (PCM_MIDDLE + PCM_SHORT) * 1e6 / 2)
/*TODO*///							byte |= 8;
/*TODO*///						bytecount++;
/*TODO*///						break;
/*TODO*///					case 10:
/*TODO*///						if (diff > (PCM_MIDDLE + PCM_SHORT) * 1e6 / 2)
/*TODO*///							byte |= 0x10;
/*TODO*///						bytecount++;
/*TODO*///						break;
/*TODO*///					case 12:
/*TODO*///						if (diff > (PCM_MIDDLE + PCM_SHORT) * 1e6 / 2)
/*TODO*///							byte |= 0x20;
/*TODO*///						bytecount++;
/*TODO*///						break;
/*TODO*///					case 14:
/*TODO*///						if (diff > (PCM_MIDDLE + PCM_SHORT) * 1e6 / 2)
/*TODO*///							byte |= 0x40;
/*TODO*///						bytecount++;
/*TODO*///						break;
/*TODO*///					case 16:
/*TODO*///						if (diff > (PCM_MIDDLE + PCM_SHORT) * 1e6 / 2)
/*TODO*///							byte |= 0x80;
/*TODO*///						logerror("byte %.2x\n", byte);
/*TODO*///						bytecount = 0;
/*TODO*///						break;
/*TODO*///					}
/*TODO*///				}
/*TODO*///			}
/*TODO*///			old = data;
/*TODO*///			time = timer_get_time ();
/*TODO*///		}
/*TODO*///	#endif
		if (tape.noise != 0)
			DAC_data_w (0, data!=0 ? TONE_ON_VALUE : 0);
	}
	
	static void vc20_tape_bit (int bit)
	{
		switch (prg.statebit)
		{
		case 0:
			if (bit != 0)
			{
				timer_reset (prg.timer, prg.lasttime = PCM_MIDDLE());
				prg.statebit = 2;
			}
			else
			{
				prg.statebit++;
				timer_reset (prg.timer, prg.lasttime = PCM_SHORT());
			}
			break;
		case 1:
			timer_reset (prg.timer, prg.lasttime = PCM_MIDDLE());
			prg.statebit = 0;
			break;
		case 2:
			timer_reset (prg.timer, prg.lasttime = PCM_SHORT());
			prg.statebit = 0;
			break;
		}
	}
        
        static int bit = 0, parity = 0;
	
	static void vc20_tape_byte ()
	{
		
		/* convert one byte to vc20 tape data
		 * puls wide modulation
		 * 3 type of pulses (quadratic on/off pulse)
		 * K (short) 176 microseconds
		 * M 256
		 * L 336
		 * LM bit0 bit1 bit2 bit3 bit4 bit5 bit6 bit7 oddparity
		 * 0 coded as KM, 1 as MK
		 * gives 8.96 milliseconds for 1 byte
		 */
		switch (prg.statebyte)
		{
		case 0:
			timer_reset (prg.timer, prg.lasttime = PCM_LONG());
			prg.statebyte++;
			break;
		case 1:
			timer_reset (prg.timer, prg.lasttime = PCM_MIDDLE());
			prg.statebyte++;
			bit = 1;
			parity = 0;
			break;
		case 2:
		case 3:
		case 4:
		case 5:
		case 6:
		case 7:
		case 8:
		case 9:
			vc20_tape_bit (prg.prgdata & bit);
			if ((prg.prgdata & bit) != 0)
				parity = parity!=0?0:1;
			bit <<= 1;
			prg.statebyte++;
			break;
		case 10:
			vc20_tape_bit (parity!=0?0:1);
			prg.chksum ^= prg.prgdata;
			prg.statebyte = 0;
			prg.pos--;
			break;
		}
	}
	
	/* 01 prg id
	 * lo hi load address
	 * lo hi end address
	 * 192-5-1 bytes: filename (filled with 0x20)
	 * xor chksum */
        static int i = 0;
        
	static void vc20_tape_prgheader ()
	{
		
		switch (prg.stateheader)
		{
		case 0:
			prg.chksum = 0;
			prg.prgdata = 1;
			prg.stateheader++;
			vc20_tape_byte ();
			break;
		case 1:
			prg.prgdata = prg.prg.read(0);
			prg.stateheader++;
			vc20_tape_byte ();
			break;
		case 2:
			prg.prgdata = prg.prg.read(1);
			prg.stateheader++;
			vc20_tape_byte ();
			break;
		case 3:
			prg.prgdata = (prg.prg.read(0) + prg.length - 2) & 0xff;
			prg.stateheader++;
			vc20_tape_byte ();
			break;
		case 4:
			prg.prgdata = ((prg.prg.read(0) + (prg.prg.read(1) << 8)) + prg.length - 2) >> 8;
			prg.stateheader++;
			i = 0;
			vc20_tape_byte ();
			break;
		case 5:
			if ((i != 16) && (prg.name.charAt(i) != 0))
			{
				prg.prgdata = prg.name.charAt(i);
				i++;
				vc20_tape_byte ();
				break;
			}
			prg.prgdata = 0x20;
			prg.stateheader++;
			vc20_tape_byte ();
			break;
		case 6:
			if (i != 192 - 5 - 1)
			{
				vc20_tape_byte ();
				i++;
				break;
			}
			prg.prgdata = prg.chksum;
			vc20_tape_byte ();
			prg.stateheader = 0;
			break;
		}
	}
	
	static void vc20_tape_program ()
	{
		switch (prg.stateblock)
		{
		case 0:
			prg.pos = (9 + 192 + 1) * 2 + (9 + prg.length - 2 + 1) * 2;
			i = 0;
			prg.stateblock++;
			timer_reset (prg.timer, prg.lasttime = PCM_SHORT());
			break;
		case 1:
			i++;
			if (i < 12000 /*27136 */ )
			{							   /* this time is not so important */
				timer_reset (prg.timer, prg.lasttime = PCM_SHORT());
				break;
			}
			/* writing countdown $89 ... $80 */
			prg.stateblock++;
			prg.prgdata = 0x89;
			vc20_tape_byte ();
			break;
		case 2:
			if (prg.prgdata != 0x81)
			{
				prg.prgdata--;
				vc20_tape_byte ();
				break;
			}
			prg.stateblock++;
			vc20_tape_prgheader ();
			break;
		case 3:
			timer_reset (prg.timer, prg.lasttime = PCM_LONG());
			prg.stateblock++;
			i = 0;
			break;
		case 4:
			if (i < 80)
			{
				i++;
				timer_reset (prg.timer, prg.lasttime = PCM_SHORT());
				break;
			}
			/* writing countdown $09 ... $00 */
			prg.prgdata = 9;
			prg.stateblock++;
			vc20_tape_byte ();
			break;
		case 5:
			if (prg.prgdata != 1)
			{
				prg.prgdata--;
				vc20_tape_byte ();
				break;
			}
			prg.stateblock++;
			vc20_tape_prgheader ();
			break;
		case 6:
			timer_reset (prg.timer, prg.lasttime = PCM_LONG());
			prg.stateblock++;
			i = 0;
			break;
		case 7:
			if (i < 80)
			{
				i++;
				timer_reset (prg.timer, prg.lasttime = PCM_SHORT());
				break;
			}
			i = 0;
			prg.stateblock++;
			timer_reset (prg.timer, prg.lasttime = PCM_SHORT());
			break;
		case 8:
			if (i < 3000 /*5376 */ )
			{
				i++;
				timer_reset (prg.timer, prg.lasttime = PCM_SHORT());
				break;
			}
			prg.prgdata = 0x89;
			prg.stateblock++;
			vc20_tape_byte ();
			break;
		case 9:
			if (prg.prgdata != 0x81)
			{
				prg.prgdata--;
				vc20_tape_byte ();
				break;
			}
			i = 2;
			prg.chksum = 0;
			prg.prgdata = prg.prg.read(i);
			i++;
			vc20_tape_byte ();
			prg.stateblock++;
			break;
		case 10:
			if (i < prg.length)
			{
				prg.prgdata = prg.prg.read(i);
				i++;
				vc20_tape_byte ();
				break;
			}
			prg.prgdata = prg.chksum;
			vc20_tape_byte ();
			prg.stateblock++;
			break;
		case 11:
			timer_reset (prg.timer, prg.lasttime = PCM_LONG());
			prg.stateblock++;
			i = 0;
			break;
		case 12:
			if (i < 80)
			{
				i++;
				timer_reset (prg.timer, prg.lasttime = PCM_SHORT());
				break;
			}
			/* writing countdown $09 ... $00 */
			prg.prgdata = 9;
			prg.stateblock++;
			vc20_tape_byte ();
			break;
		case 13:
			if (prg.prgdata != 1)
			{
				prg.prgdata--;
				vc20_tape_byte ();
				break;
			}
			prg.chksum = 0;
			i = 2;
			prg.prgdata = prg.prg.read(i);
			i++;
			vc20_tape_byte ();
			prg.stateblock++;
			break;
		case 14:
			if (i < prg.length)
			{
				prg.prgdata = prg.prg.read(i);
				i++;
				vc20_tape_byte ();
				break;
			}
			prg.prgdata = prg.chksum;
			vc20_tape_byte ();
			prg.stateblock++;
			break;
		case 15:
			timer_reset (prg.timer, prg.lasttime = PCM_LONG());
			prg.stateblock++;
			i = 0;
			break;
		case 16:
			if (i < 80)
			{
				i++;
				timer_reset (prg.timer, prg.lasttime = PCM_SHORT());
				break;
			}
			prg.stateblock = 0;
			break;
		}
	
	}
	
	static timer_callback vc20_prg_timer = new timer_callback() {
            public void handler(int i) {
                if (tape.data==0)
		{								   /* send the same low phase */
			if (tape.noise!=0)
				DAC_data_w (0, 0);
			tape.data = 1;
			timer_reset (prg.timer, prg.lasttime);
		}
		else
		{
			if (tape.noise!=0)
				DAC_data_w (0, TONE_ON_VALUE);
			tape.data = 0;
			if (prg.statebit!=0)
			{
				vc20_tape_bit (0);
			}
			else if (prg.statebyte!=0)
			{							   /* send the rest of the byte */
				vc20_tape_byte ();
			}
			else if (prg.stateheader!=0)
			{
				vc20_tape_prgheader ();
			}
			else
			{
				vc20_tape_program ();
				if (prg.stateblock==0)
				{
					prg.timer = null;
					tape.play = 0;
				}
			}
		}
		if (tape.read_callback != null)
			tape.read_callback.handler(0, tape.data);
		vc20_prg_state ();
            }
        };
	
	static void vc20_zip_state ()
	{
		switch (zip.state)
		{
		case 0:
			break;						   /* not inited */
		case 1:						   /* off */
			if (tape.on != 0)
			{
				zip.state = 2;
				break;
			}
			break;
		case 2:						   /* on */
			if (tape.on==0)
			{
				zip.state = 1;
				tape.play = 0;
				tape.record = 0;
				DAC_data_w (0, 0);
				break;
			}
			if (tape.motor!=0 && tape.play!=0)
			{
				zip.state = 3;
				prg.timer = timer_set (0.0, 0, vc20_zip_timer);
				break;
			}
			if (tape.motor!=0 && tape.record!=0)
			{
				zip.state = 4;
				break;
			}
			break;
		case 3:						   /* reading */
			if (tape.on==0)
			{
				zip.state = 1;
				tape.play = 0;
				tape.record = 0;
				DAC_data_w (0, 0);
				if (prg.timer != null)
					timer_remove (prg.timer);
				break;
			}
			if (tape.motor==0 || tape.play==0)
			{
				zip.state = 2;
				if (prg.timer != null)
					timer_remove (prg.timer);
				DAC_data_w (0, 0);
				break;
			}
			break;
		case 4:						   /* saving */
			if (tape.on==0)
			{
				zip.state = 1;
				tape.play = 0;
				tape.record = 0;
				DAC_data_w (0, 0);
				timer_remove (prg.timer);
				break;
			}
			if (tape.motor==0 || tape.record==0)
			{
				zip.state = 2;
				timer_remove (prg.timer);
				DAC_data_w (0, 0);
				break;
			}
			break;
		}
	}
	
	static void vc20_zip_readfile ()
	{
            System.out.println("vc20_zip_readfile UNIMPLEMENTED!!!!");
	/*TODO*///	int i;
	/*TODO*///	String cp;
	/*TODO*///
	/*TODO*///	for (i = 0; i < 2; i++)
	/*TODO*///	{
	/*TODO*///		zip.zipentry = readzip (zip.zip);
	/*TODO*///		if (zip.zipentry == null)
	/*TODO*///		{
	/*TODO*///			i++;
	/*TODO*///			rewindzip (zip.zip);
	/*TODO*///			continue;
	/*TODO*///		}
	/*TODO*///		if ((cp = strrchr (zip.zipentry.name, '.')) == null)
	/*TODO*///			continue;
	/*TODO*///		if (stricmp (cp, ".prg") == 0)
	/*TODO*///			break;
	/*TODO*///	}
	/*TODO*///
	/*TODO*///	if (i == 2)
	/*TODO*///	{
	/*TODO*///		zip.state = 0;
	/*TODO*///		return;
	/*TODO*///	}
	/*TODO*///	for (i = 0; zip.zipentry.name[i] != 0; i++)
	/*TODO*///		prg.name[i] = toupper (zip.zipentry.name[i]);
	/*TODO*///	for (; i < 16; i++)
	/*TODO*///		prg.name[i] = ' ';
	/*TODO*///
	/*TODO*///	prg.length = zip.zipentry.uncompressed_size;
	/*TODO*///	if ((prg.prg = (UINT8 *) malloc (prg.length)) == null)
	/*TODO*///	{
	/*TODO*///		logerror("out of memory\n");
	/*TODO*///		zip.state = 0;
	/*TODO*///	}
	/*TODO*///	readuncompresszip (zip.zip, zip.zipentry, (char *) prg.prg);
	}
	
	static void vc20_zip_open (int image_type, int image_id)
	{
            System.out.println("vc20_zip_open UNIMPLEMENTED!!!!");
	/*TODO*///	if (!(zip.zip = openzip (device_filename(image_type,image_id))))
	/*TODO*///	{
	/*TODO*///		logerror("tape %s not found\n", device_filename(image_type,image_id));
	/*TODO*///		return;
	/*TODO*///	}
	/*TODO*///
	/*TODO*///	logerror("tape %s linked\n", device_filename(image_type,image_id));
	/*TODO*///
	/*TODO*///	tape.type = TAPE_ZIP;
	/*TODO*///	tape.on = 1;
	/*TODO*///	zip.image_type = image_type;
        /*TODO*///        zip.image_id = image_id;
	/*TODO*///	zip.state = 2;
	/*TODO*///	prg.stateblock = 0;
	/*TODO*///	prg.stateheader = 0;
	/*TODO*///	prg.statebyte = 0;
	/*TODO*///	prg.statebit = 0;
	/*TODO*///	prg.pos = 0;
	/*TODO*///	vc20_zip_readfile ();
	}
	
	static timer_callback vc20_zip_timer = new timer_callback() {
            public void handler(int data) {
                if (tape.data==0)
		{								   /* send the same low phase */
			if (tape.noise!=0)
				DAC_data_w (0, 0);
			tape.data = 1;
			timer_reset (prg.timer, prg.lasttime);
		}
		else
		{
			if (tape.noise!=0)
				DAC_data_w (0, TONE_ON_VALUE);
			tape.data = 0;
			if (prg.statebit!=0)
			{
				vc20_tape_bit (0);
			}
			else if (prg.statebyte!=0)
			{							   /* send the rest of the byte */
				vc20_tape_byte ();
			}
			else if (prg.stateheader!=0)
			{
				vc20_tape_prgheader ();
			}
			else
			{
				vc20_tape_program ();
				if (prg.stateblock==0)
				{
					/* loading next file of zip */
					timer_reset (prg.timer, 0.0);
					prg.prg = null;
					vc20_zip_readfile ();
				}
			}
		}
		if (tape.read_callback != null)
			tape.read_callback.handler(0, tape.data);
		vc20_prg_state ();
            }
        };
	
	public static void vc20_tape_open (WriteHandlerPtr read_callback)
	{
		tape.read_callback = read_callback;
	/*TODO*///#ifndef NEW_GAMEDRIVER
	/*TODO*///	tape.type = 0;
	/*TODO*///	tape.on = 0;
	/*TODO*///	tape.noise = 0;
	/*TODO*///	tape.play = 0;
	/*TODO*///	tape.record = 0;
	/*TODO*///	tape.motor = 0;
	/*TODO*///	tape.data = 0;
	/*TODO*///#endif
		prg.c16 = 0;
	}
	
	public static void c16_tape_open ()
	{
		vc20_tape_open (null);
		prg.c16 = 1;
	}
	
	public static io_initPtr vc20_tape_attach_image = new io_initPtr() {
            public int handler(int id) {
                String cp;
	
		tape.type = 0;
		tape.on = 0;
		tape.noise = 0;
		tape.play = 0;
		tape.record = 0;
		tape.motor = 0;
		tape.data = 0;
	
		if (device_filename(IO_CASSETTE,id) == null)
			return INIT_PASS;
	
		if ((cp = strrchr (device_filename(IO_CASSETTE,id), '.')) == null)
			return INIT_FAIL;
		if (device_filename(IO_CASSETTE, id).toLowerCase().endsWith(".wav"))
		{
			vc20_wav_open (IO_CASSETTE,id);
		}
		else if (stricmp (cp, ".prg") == 0)
		{
			vc20_prg_open (IO_CASSETTE,id);
		}
		else if (stricmp(cp, ".zip") == 0)
		{
			vc20_zip_open (IO_CASSETTE,id);
		}
		else
			return INIT_FAIL;
		return INIT_PASS;
            }
        };
	
	public static io_exitPtr vc20_tape_detach_image = new io_exitPtr() {
            public int handler(int id) {
                System.out.println("vc20_tape_detach_image");
                //vc20_tape_close();
                
                return INIT_PASS;
            }
        };
	
	public static void vc20_tape_close ()
	{
		switch (tape.type)
		{
		case TAPE_WAV:
			wav.sample = null;
			break;
		case TAPE_PRG:
			prg.prg = null;
			break;
		case TAPE_ZIP:
			prg.prg = null;
			/*TODO*///closezip (zip.zip);
			break;
		}
		/* HJB reset so vc20_tape_close() can be called multiple times!? */
	    tape.type = 0;
	}
	
	static void vc20_state ()
	{
            //System.out.println("vc20_state "+tape.type);
		switch (tape.type)
		{
		case TAPE_WAV:
                    //System.out.println("Tipo WAV");
			vc20_wav_state ();
			break;
		case TAPE_PRG:
			vc20_prg_state ();
			break;
		case TAPE_ZIP:
			vc20_zip_state ();
			break;
		}
	}
	
	static int vc20_tape_switch ()
	{
		int data = 1;
	
		switch (tape.type)
		{
		case TAPE_WAV:
			data = ((wav.state > 1) && (tape.play!=0 || tape.record!=0))?0:1;
			break;
		case TAPE_PRG:
			data = ((prg.state > 1) && (tape.play!=0 || tape.record!=0))?0:1;
			break;
		case TAPE_ZIP:
			data = ((zip.state > 1) && (tape.play!=0 || tape.record!=0))?0:1;
			break;
		}
		return data;
                //return 0;
	}
	
	public static int vc20_tape_read ()
	{
		switch (tape.type)
		{
		case TAPE_WAV:
			if (wav.state == 3)
				return tape.data;
			break;
		case TAPE_PRG:
			if (prg.state == 3)
				return tape.data;
			break;
		case TAPE_ZIP:
			if (zip.state == 3)
				return tape.data;
			break;
		}
		return 0;
	}
	
	/* here for decoding tape formats */
	public static void vc20_tape_write (int data)
	{
		switch (tape.type)
		{
		case TAPE_WAV:
			if (wav.state == 4)
				vc20_wav_write (data);
			break;
		case TAPE_PRG:
			if (prg.state == 4)
				vc20_prg_write (data);
			break;
		case TAPE_ZIP:
			if (zip.state == 4)
				vc20_prg_write (data);
			break;
		}
	}
	
	public static void vc20_tape_config (int on, int noise)
	{
		switch (tape.type)
		{
		case TAPE_WAV:
			tape.on = ((wav.state != 0) && on!=0) ? 1:0;
			break;
		case TAPE_PRG:
			tape.on = (prg.state != 0) && on!=0 ? 1:0;
			break;
		case TAPE_ZIP:
			tape.on = (zip.state != 0) && on!=0 ? 1:0;
			break;
		}
		tape.noise = tape.on!=0 && noise!=0 ? 1:0;
		vc20_state ();
	}
	
	static void vc20_tape_buttons (int play, int record, int stop)
	{
            //System.out.println("vc20_tape_buttons PLAY="+play);
		if (stop != 0)
		{
			tape.play = 0; tape.record = 0;
		}
		else if (play!=0 && tape.record==0)
		{
			tape.play = tape.on;
		}
		else if (record!=0 && tape.play==0)
		{
			tape.record = tape.on;
		}
		vc20_state ();
	}
	
	static void vc20_tape_motor (int data)
	{
            //System.out.println("vc20_tape_motor");
		tape.motor = data!=0?0:1;
		vc20_state ();
	}
	
	public static void vc20_tape_status (String text, int size)
	{
		text = "";
		switch (tape.type)
		{
		case TAPE_WAV:
			switch (wav.state)
			{
			case 4:
				text = sprintf ("Tape saving");
				break;
			case 3:
				text = sprintf ("Tape (%s) loading %d/%dsec",
						  device_filename(wav.image_type, wav.image_id),
						  wav.pos / wav.sample.smpfreq,
						  wav.sample.length / wav.sample.smpfreq);
				break;
			}
			break;
		case TAPE_PRG:
			switch (prg.state)
			{
			case 4:
				text = sprintf ("Tape saving");
				break;
			case 3:
				text = sprintf ("Tape (%s) loading %d",
					device_filename(prg.image_type, prg.image_id), prg.pos);
				break;
			}
			break;
		case TAPE_ZIP:
			switch (zip.state)
			{
			case 4:
				text = sprintf ("Tape saving");
				break;
			case 3:
				System.out.println("device_filename(zip.image_type... NOT IMPLEMENTED!!!!");
                                /*TODO*///printf (text, size, "Tape (%s) File %s loading %d",
				/*TODO*///	device_filename(zip.image_type,zip.image_id), zip.zipentry.name, prg.pos);
				break;
			}
			break;
		}
                //System.out.println(text);
                int y = Machine.uiymin + Machine.uiheight - 9;
                ui_text(Machine.scrbitmap, text, 0, y);
	}
}
