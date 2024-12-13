/***************************************************************************

  WD179X.c

  Functions to emulate a WD179x floppy disc controller

  KT - Removed disk image code and replaced it with floppy drive functions.
	   Any disc image is now useable with this code.
	 - fixed write protect


 TODO:
	 - Multiple record read/write
	 - What happens if a track is read that doesn't have any id's on it?
	   (e.g. unformatted disc)
***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.machine;

import static arcadeflex056.fucPtr.*;
import static mame056.timer.*;
import static mame056.timerH.*;
import static mess056.includes.wd179xH.*;
import static arcadeflex056.osdepend.logerror;
import static common.ptr.*;
import static mess056.machine.flopdrv.*;
import static mess056.includes.flopdrvH.*;

public class wd179x
{
	
	public static int VERBOSE = 1;
	public static int VERBOSE_DATA = 0;		/* This turns on and off the recording of each byte during read and write */
	
	/* structure describing a double density track */
	public static int TRKSIZE_DD = 6144;
        
	/*TODO*///#if 0
	/*TODO*///static UINT8 track_DD[][2] = {
	/*TODO*///	{16, 0x4e}, 	/* 16 * 4E (track lead in)				 */
	/*TODO*///	{ 8, 0x00}, 	/*	8 * 00 (pre DAM)					 */
	/*TODO*///	{ 3, 0xf5}, 	/*	3 * F5 (clear CRC)					 */
	/*TODO*///
	/*TODO*///	{ 1, 0xfe}, 	/* *** sector *** FE (DAM)				 */
	/*TODO*///	{ 1, 0x80}, 	/*	4 bytes track,head,sector,seclen	 */
	/*TODO*///	{ 1, 0xf7}, 	/*	1 * F7 (CRC)						 */
	/*TODO*///	{22, 0x4e}, 	/* 22 * 4E (sector lead in) 			 */
	/*TODO*///	{12, 0x00}, 	/* 12 * 00 (pre AM) 					 */
	/*TODO*///	{ 3, 0xf5}, 	/*	3 * F5 (clear CRC)					 */
	/*TODO*///	{ 1, 0xfb}, 	/*	1 * FB (AM) 						 */
	/*TODO*///	{ 1, 0x81}, 	/*	x bytes sector data 				 */
	/*TODO*///	{ 1, 0xf7}, 	/*	1 * F7 (CRC)						 */
	/*TODO*///	{16, 0x4e}, 	/* 16 * 4E (sector lead out)			 */
	/*TODO*///	{ 8, 0x00}, 	/*	8 * 00 (post sector)				 */
	/*TODO*///	{ 0, 0x00}, 	/* end of data							 */
	/*TODO*///};
	/*TODO*///#endif
        
	/* structure describing a single density track */
	public static int TRKSIZE_SD = 3172;
	
        /*TODO*///#if 0
	/*TODO*///static UINT8 track_SD[][2] = {
	/*TODO*///	{16, 0xff}, 	/* 16 * FF (track lead in)				 */
	/*TODO*///	{ 8, 0x00}, 	/*	8 * 00 (pre DAM)					 */
	/*TODO*///	{ 1, 0xfc}, 	/*	1 * FC (clear CRC)					 */
	/*TODO*///
	/*TODO*///	{11, 0xff}, 	/* *** sector *** 11 * FF				 */
	/*TODO*///	{ 6, 0x00}, 	/*	6 * 00 (pre DAM)					 */
	/*TODO*///	{ 1, 0xfe}, 	/*	1 * FE (DAM)						 */
	/*TODO*///	{ 1, 0x80}, 	/*	4 bytes track,head,sector,seclen	 */
	/*TODO*///	{ 1, 0xf7}, 	/*	1 * F7 (CRC)						 */
	/*TODO*///	{10, 0xff}, 	/* 10 * FF (sector lead in) 			 */
	/*TODO*///	{ 4, 0x00}, 	/*	4 * 00 (pre AM) 					 */
	/*TODO*///	{ 1, 0xfb}, 	/*	1 * FB (AM) 						 */
	/*TODO*///	{ 1, 0x81}, 	/*	x bytes sector data 				 */
	/*TODO*///	{ 1, 0xf7}, 	/*	1 * F7 (CRC)						 */
	/*TODO*///	{ 0, 0x00}, 	/* end of data							 */
	/*TODO*///};
	/*TODO*///#endif
	
	static timer_entry busy_timer = null;
	
	/* one wd controlling multiple drives */
	static WD179X wd=null;
	/* this is the drive currently selected */
	static int drv = 0;
	/* this is the head currently selected */
	static int hd = 0;
	
	/* use this to determine which drive is controlled by WD */
	public static void wd179x_set_drive(int drive)
	{
            if (VERBOSE != 0){
		if( drive != drv )
		logerror("wd179x_set_drive: $%02x\n", drive);
            }
	
		drv = drive;
	}
	
	public static void wd179x_set_side(int head)
	{
            if (VERBOSE != 0){
		if( head != hd )
		logerror("wd179x_set_side: $%02x\n", head);
            }
	
		hd = head;
	}
	
	public static void wd179x_set_density(DENSITY density)
	{
		WD179X w = wd;
	
            if (VERBOSE != 0){
		if( w.density != density )
			logerror("wd179x_set_density: $%02x\n", density);
            }
	
		w.density = density;
	}
	
	
	static timer_callback wd179x_busy_callback = new timer_callback() {
            public void handler(int i) {
                WD179X w = new WD179X();
	
		wd179x_set_irq(w);			
		timer_reset(busy_timer, TIME_NEVER);
            }
        };
	
	static void wd179x_set_busy(WD179X w, double milliseconds)
	{
		if (busy_timer != null)
		{
			timer_remove(busy_timer);
		}
		w.status |= STA_1_BUSY;
		//busy_timer = timer_set(TIME_IN_MSEC(milliseconds), (int)w, wd179x_busy_callback);
                busy_timer = timer_set(TIME_IN_MSEC(milliseconds), drv, wd179x_busy_callback);
	}
	
	
	
	/* BUSY COUNT DOESN'T WORK PROPERLY! */
	
	static void wd179x_restore(WD179X w)
	{
			int step_counter = 255;
			
	/*TODO*///#if 0
	/*TODO*///		w.status |= STA_1_BUSY;
	/*TODO*///#endif
	
			/* setup step direction */
			w.direction = -1;
	
			w.command_type = TYPE_I;
	
			/* reset busy count */
			w.busy_count = 0;
	
			/* keep stepping until track 0 is received or 255 steps have been done */
			while (!(floppy_drive_get_flag_state(drv, FLOPPY_DRIVE_HEAD_AT_TRACK_0)!=0) && (step_counter!=0))
			{
				/* update time to simulate seek time busy signal */
				w.busy_count++;
				floppy_drive_seek(drv, w.direction);
				step_counter--;
			}
	
			/* update track reg */
			w.track_reg = 0;
	/*TODO*///#if 0
	/*TODO*///		/* simulate seek time busy signal */
	/*TODO*///		w.busy_count = 0;	//w.busy_count * ((w.data & FDC_STEP_RATE) + 1);
	/*TODO*///	
	/*TODO*///		/* when command completes set irq */
	/*TODO*///		wd179x_set_irq(w);
	/*TODO*///#endif
			wd179x_set_busy(w,0.1);
	
	}
	
	public static void wd179x_reset()
	{
            System.out.println("wd179x_reset");
		wd179x_restore(wd);
	}
	
	
	public static void wd179x_init(int type, timer_callback callback)
	{
            System.out.println("wd179x_init");
		//memset(&wd, 0, sizeof(WD179X));
                wd = new WD179X();
		wd.status = STA_1_TRACK0;
		wd.type = type;
		wd.callback = callback;
	//	wd.status_ipl = STA_1_IPL;
		wd.density = DENSITY.DEN_MFM_LO;
		busy_timer = null;
	
		wd179x_reset();
	
	/*TODO*///#if 0
	/*TODO*///
	/*TODO*///	for (i = 0; i < MAX_DRIVES; i++)
	/*TODO*///	{
	/*TODO*///		wd[i] = malloc(sizeof(WD179X));
	/*TODO*///		if (!wd[i])
	/*TODO*///		{
	/*TODO*///			while (--i >= 0)
	/*TODO*///			{
	/*TODO*///				free(wd[i]);
	/*TODO*///				wd[i] = 0;
	/*TODO*///			}
	/*TODO*///			return;
	/*TODO*///		}
	/*TODO*///		memset(wd[i], 0, sizeof(WD179X));
	/*TODO*///		wd[i].unit = 0;
	/*TODO*///		wd[i].tracks = 40;
	/*TODO*///		wd[i].heads = 1;
	/*TODO*///		wd[i].density = DEN_MFM_LO;
	/*TODO*///		wd[i].offset = 0;
	/*TODO*///		wd[i].first_sector_id = 0;
	/*TODO*///		wd[i].sec_per_track = 18;
	/*TODO*///		wd[i].sector_length = 256;
	/*TODO*///		wd[i].head = 0;
	/*TODO*///		wd[i].track = 0;
	/*TODO*///		wd[i].track_reg = 0;
	/*TODO*///		wd[i].direction = 1;
	/*TODO*///		wd[i].sector = 0;
	/*TODO*///		wd[i].data = 0;
	/*TODO*///		wd[i].status = (active) ? STA_1_TRACK0 : 0;
	/*TODO*///		wd[i].status_drq = 0;
	/*TODO*///		wd[i].status_ipl = 0;
	/*TODO*///		wd[i].busy_count = 0;
	/*TODO*///		wd[i].data_offset = 0;
	/*TODO*///		wd[i].data_count = 0;
	/*TODO*///		wd[i].image_name = 0;
	/*TODO*///		wd[i].image_size = 0;
	/*TODO*///		wd[i].dir_sector = 0;
	/*TODO*///		wd[i].dir_length = 0;
	/*TODO*///		wd[i].secmap = 0;
	/*TODO*///		wd[i].timer = null;
	/*TODO*///		wd[i].timer_rs = null;
	/*TODO*///		wd[i].timer_ws = null;
	/*TODO*///	}
	/*TODO*///#endif
	}
	
	static void write_track(WD179X w)
	{
	
	
	
	}
	
	/* read an entire track */
	static void read_track(WD179X w)
	{
	/*TODO*///#if 0
	/*TODO*///	UINT8 *psrc;		/* pointer to track format structure */
	/*TODO*///	UINT8 *pdst;		/* pointer to track buffer */
	/*TODO*///	int cnt;			/* number of bytes to fill in */
	/*TODO*///	UINT16 crc; 		/* id or data CRC */
	/*TODO*///	UINT8 d;			/* data */
	/*TODO*///	UINT8 t = w.track; /* track of DAM */
	/*TODO*///	UINT8 h = w.head;	/* head of DAM */
	/*TODO*///	UINT8 s = w.sector_dam;		/* sector of DAM */
	/*TODO*///	UINT16 l = w.sector_length;	/* sector length of DAM */
	/*TODO*///	int i;
	/*TODO*///
	/*TODO*///	for (i = 0; i < w.sec_per_track; i++)
	/*TODO*///	{
	/*TODO*///		w.dam_list[i][0] = t;
	/*TODO*///		w.dam_list[i][1] = h;
	/*TODO*///		w.dam_list[i][2] = i;
	/*TODO*///		w.dam_list[i][3] = l >> 8;
	/*TODO*///	}
	/*TODO*///
	/*TODO*///	pdst = w.buffer;
	/*TODO*///
	/*TODO*///	if (w.density)
	/*TODO*///	{
	/*TODO*///		psrc = track_DD[0];    /* double density track format */
	/*TODO*///		cnt = TRKSIZE_DD;
	/*TODO*///	}
	/*TODO*///	else
	/*TODO*///	{
	/*TODO*///		psrc = track_SD[0];    /* single density track format */
	/*TODO*///		cnt = TRKSIZE_SD;
	/*TODO*///	}
	/*TODO*///
	/*TODO*///	while (cnt > 0)
	/*TODO*///	{
	/*TODO*///		if (psrc[0] == 0)	   /* no more track format info ? */
	/*TODO*///		{
	/*TODO*///			if (w.dam_cnt < w.sec_per_track) /* but more DAM info ? */
	/*TODO*///			{
	/*TODO*///				if (w.density)/* DD track ? */
	/*TODO*///					psrc = track_DD[3];
	/*TODO*///				else
	/*TODO*///					psrc = track_SD[3];
	/*TODO*///			}
	/*TODO*///		}
	/*TODO*///
	/*TODO*///		if (psrc[0] != 0)	   /* more track format info ? */
	/*TODO*///		{
	/*TODO*///			cnt -= psrc[0];    /* subtract size */
	/*TODO*///			d = psrc[1];
	/*TODO*///
	/*TODO*///			if (d == 0xf5)	   /* clear CRC ? */
	/*TODO*///			{
	/*TODO*///				crc = 0xffff;
	/*TODO*///				d = 0xa1;	   /* store A1 */
	/*TODO*///			}
	/*TODO*///
	/*TODO*///			for (i = 0; i < *psrc; i++)
	/*TODO*///				*pdst++ = d;   /* fill data */
	/*TODO*///
	/*TODO*///			if (d == 0xf7)	   /* store CRC ? */
	/*TODO*///			{
	/*TODO*///				pdst--; 	   /* go back one byte */
	/*TODO*///				*pdst++ = crc & 255;	/* put CRC low */
	/*TODO*///				*pdst++ = crc / 256;	/* put CRC high */
	/*TODO*///				cnt -= 1;	   /* count one more byte */
	/*TODO*///			}
	/*TODO*///			else if (d == 0xfe)/* address mark ? */
	/*TODO*///			{
	/*TODO*///				crc = 0xffff;	/* reset CRC */
	/*TODO*///			}
	/*TODO*///			else if (d == 0x80)/* sector ID ? */
	/*TODO*///			{
	/*TODO*///				pdst--; 	   /* go back one byte */
	/*TODO*///				t = *pdst++ = w.dam_list[w.dam_cnt][0]; /* track number */
	/*TODO*///				h = *pdst++ = w.dam_list[w.dam_cnt][1]; /* head number */
	/*TODO*///				s = *pdst++ = w.dam_list[w.dam_cnt][2]; /* sector number */
	/*TODO*///				l = *pdst++ = w.dam_list[w.dam_cnt][3]; /* sector length code */
	/*TODO*///				w.dam_cnt++;
	/*TODO*///				calc_crc(&crc, t);	/* build CRC */
	/*TODO*///				calc_crc(&crc, h);	/* build CRC */
	/*TODO*///				calc_crc(&crc, s);	/* build CRC */
	/*TODO*///				calc_crc(&crc, l);	/* build CRC */
	/*TODO*///				l = (l == 0) ? 128 : l << 8;
	/*TODO*///			}
	/*TODO*///			else if (d == 0xfb)// data address mark ?
	/*TODO*///			{
	/*TODO*///				crc = 0xffff;	// reset CRC
	/*TODO*///			}
	/*TODO*///			else if (d == 0x81)// sector DATA ?
	/*TODO*///			{
	/*TODO*///				pdst--; 	   /* go back one byte */
	/*TODO*///				if (seek(w, t, h, s) == 0)
	/*TODO*///				{
	/*TODO*///					if (osd_fread(w.image_file, pdst, l) != l)
	/*TODO*///					{
	/*TODO*///						w.status = STA_2_CRC_ERR;
	/*TODO*///						return;
	/*TODO*///					}
	/*TODO*///				}
	/*TODO*///				else
	/*TODO*///				{
	/*TODO*///					w.status = STA_2_REC_N_FND;
	/*TODO*///					return;
	/*TODO*///				}
	/*TODO*///				for (i = 0; i < l; i++) // build CRC of all data
	/*TODO*///					calc_crc(&crc, *pdst++);
	/*TODO*///				cnt -= l;
	/*TODO*///			}
	/*TODO*///			psrc += 2;
	/*TODO*///		}
	/*TODO*///		else
	/*TODO*///		{
	/*TODO*///			*pdst++ = 0xff;    /* fill track */
	/*TODO*///			cnt--;			   /* until end */
	/*TODO*///		}
	/*TODO*///	}
	/*TODO*///#endif
	
		w.data_count = ((w.density)!=null) ? TRKSIZE_DD : TRKSIZE_SD;
	
		floppy_drive_read_track_data_info_buffer(drv, hd, w.buffer, (w.data_count) );
		
		w.data_offset = 0;
	
		wd179x_set_data_request();
		w.status |= STA_2_BUSY;
		w.busy_count = 0;
	}
	
	/* currently a empty function - to be completed! */
	public static void wd179x_exit()
	{
		WD179X w = wd;
                
                if (w != null) {
	
                    if (busy_timer!=null)
                    {
                            timer_remove(busy_timer);
                            busy_timer = null;
                    }
	
                    if (w.timer!=null)
                    {
                            timer_remove(w.timer);
                            w.timer = null;
                    }

                    if (w.timer_rs!=null)
                    {
                            timer_remove(w.timer_rs);
                            w.timer = null;
                    }

                    if (w.timer_ws!=null)
                    {
                            timer_remove(w.timer_ws);
                            w.timer_ws = null;
                    }
                }
	}
	
	/*TODO*///#if 0
	/*TODO*///void wd179x_stop_drive(void)
	/*TODO*///{
	/*TODO*///	WD179X *w = &wd;
	/*TODO*///
	/*TODO*///	w.busy_count = 0;
	/*TODO*///	w.status = 0;
	/*TODO*///	w.status_drq = 0;
	/*TODO*///	if (w.callback)
	/*TODO*///		(*w.callback) (WD179X_DRQ_CLR);
	/*TODO*///	w.status_ipl = 0;
	/*TODO*///}
	/*TODO*///#endif
	
	/* calculate CRC for data address marks or sector data */
	static void calc_crc(int crc, int value)
	{
		int l, h;
	
		l = value ^ (crc >> 8);
		crc = (crc & 0xff) | (l << 8);
		l >>= 4;
		l ^= (crc >> 8);
		crc <<= 8;
		crc = (crc & 0xff00) | l;
		l = (l << 4) | (l >> 4);
		h = l;
		l = (l << 2) | (l >> 6);
		l &= 0x1f;
		crc = crc ^ (l << 8);
		l = h & 0xf0;
		crc = crc ^ (l << 8);
		l = (h << 1) | (h >> 7);
		l &= 0xe0;
		crc = crc ^ l;
	}
	
	/* read the next data address mark */
	static void wd179x_read_id(WD179X w)
	{
            System.out.println("wd179x_read_id");
		chrn_id id = new chrn_id();
	
		w.status &= ~(STA_2_CRC_ERR | STA_2_REC_N_FND);
	
		/* get next id from disc */
		if (floppy_drive_get_next_id(drv, hd, id) != 0)
		{
			int crc = 0xffff;
	
			w.data_offset = 0;
			w.data_count = 6;
	
			/* for MFM */
			/* crc includes 3x0x0a1, and 1x0x0fe (id mark) */
			calc_crc(crc,0x0a1);
			calc_crc(crc,0x0a1);
			calc_crc(crc,0x0a1);
			calc_crc(crc,0x0fe);
	
			w.buffer[0] = (char) id.C;
			w.buffer[1] = (char) id.H;
			w.buffer[2] = (char) id.R;
			w.buffer[3] = (char) id.N;
			calc_crc(crc, w.buffer[0]);
			calc_crc(crc, w.buffer[1]);
			calc_crc(crc, w.buffer[2]);
			calc_crc(crc, w.buffer[3]);
			/* crc is stored hi-byte followed by lo-byte */
			w.buffer[4] = (char) (crc>>8);
			w.buffer[5] = (char) (crc & 255);
			
	
			w.sector = id.C;
			w.status |= STA_2_BUSY;
			w.busy_count = 50;
	
			wd179x_set_data_request();
			logerror("read id succeeded.\n");
		}
		else
		{
			/* record not found */
			w.status |= STA_2_REC_N_FND;
			//w.sector = w.track_reg;
			logerror("read id failed\n");
	
			wd179x_complete_command(w, 1);
		}
	}
	
	
	static int wd179x_find_sector(WD179X w)
	{
            System.out.println("wd179x_find_sector");
		int revolution_count;
		chrn_id id=new chrn_id();
	
		revolution_count = 0;
	
		w.status &= ~STA_2_REC_N_FND;
	
		while (revolution_count!=4)
		{
			if (floppy_drive_get_next_id(drv, hd, id) != 0)
			{
				/* compare track */
				if (id.C == w.track_reg)
				{
					/* compare id */
					if (id.R == w.sector)
					{
						w.sector_length = 1<<(id.N+7);
						w.sector_data_id = id.data_id;
						/* get ddam status */
						w.ddam = id.flags & ID_FLAG_DELETED_DATA;
						/* got record type here */
                                                if (VERBOSE != 0){
                                                        logerror("sector found! C:$%02x H:$%02x R:$%02x N:$%02x%s\n", id.C, id.H, id.R, id.N, w.ddam!=0 ? " DDAM" : "");
                                                }
						return 1;
					}
				}
			}
	
			 /* index set? */
			if (floppy_drive_get_flag_state(drv, FLOPPY_DRIVE_INDEX) != 0)
			{
				/* update revolution count */
				revolution_count++;
			}
		}
	
		/* record not found */
		w.status |= STA_2_REC_N_FND;
	
                if (VERBOSE != 0){
                        logerror("track %d sector %d not found!\n", w.track_reg, w.sector);
                }
		wd179x_complete_command(w, 1);
	
		return 0;
	}
	
	/* read a sector */
	static void wd179x_read_sector(WD179X w)
	{
            System.out.println("wd179x_read_sector");
		w.data_offset = 0;
	
		if (wd179x_find_sector(w) != 0)
		{
			w.data_count = w.sector_length;
	
			/* read data */
			floppy_drive_read_sector_data(drv, hd, w.sector_data_id, w.buffer, w.sector_length);
	
			wd179x_timed_data_request();
	
			w.status |= STA_2_BUSY;
			w.busy_count = 0;
		}
	}
	
	static void wd179x_set_irq(WD179X w)
	{
		w.status &= ~STA_2_BUSY;
		/* generate an IRQ */
		if (w.callback != null)
			w.callback.handler(WD179X_IRQ_SET);
	}
	
	static timer_callback wd179x_complete_command_callback = new timer_callback() {
            public void handler(int i) {
                WD179X w = wd;
	
		wd179x_set_irq(w);
	
		/* stop it, but don't allow it to be free'd */
		timer_reset(w.timer, TIME_NEVER); 
            }
        };
	
	/* called on error, or when command is actually completed */
	/* KT - I have used a timer for systems that use interrupt driven transfers.
	A interrupt occurs after the last byte has been read. If it occurs at the time
	when the last byte has been read it causes problems - same byte read again
	or bytes missed */
	/* TJL - I have add a parameter to allow the emulation to specify the delay
	*/
	static void wd179x_complete_command(WD179X w, int delay)
	{
		int usecs;
	
		w.data_count = 0;
	
		/* clear busy bit */
		w.status &= ~STA_2_BUSY;
	
		usecs = floppy_drive_get_datarate_in_us(w.density);
		usecs *= delay;
	
		/* remove old timer if it exists */
		if (w.timer!=null)
		{
			timer_remove(w.timer);
			w.timer = null;
		}
	
		/* set new timer */
		w.timer = timer_set(TIME_IN_USEC(usecs), 0, wd179x_complete_command_callback);
	}
	
	static void wd179x_write_sector(WD179X w)
	{
		/* at this point, the disc is write enabled, and data
		 * has been transfered into our buffer - now write it to
		 * the disc image or to the real disc
		 */
	
		/* find sector */
		if (wd179x_find_sector(w) != 0)
		{
			w.data_count = w.sector_length;
	
			/* write data */
			floppy_drive_write_sector_data(drv, hd, w.sector_data_id, w.buffer, w.sector_length,w.write_cmd & 0x01);
		}
	}
	
	
	/* verify the seek operation by looking for a id that has a matching track value */
	static void wd179x_verify_seek(WD179X w)
	{
		int revolution_count;
		chrn_id id=new chrn_id();
	
		revolution_count = 0;
	
		logerror("doing seek verify\n");
	
		w.status &= ~STA_1_SEEK_ERR;
	
		/* must be found within 5 revolutions otherwise error */
		while (revolution_count!=5)
		{
			if (floppy_drive_get_next_id(drv, hd, id) != 0)
			{
				/* compare track */
				if (id.C == w.track_reg)
				{
					logerror("seek verify succeeded!\n");
					return;
				}
			}
	
			 /* index set? */
			if (floppy_drive_get_flag_state(drv, FLOPPY_DRIVE_INDEX) != 0)
			{
				/* update revolution count */
				revolution_count++;
			}
		}
	
		w.status |= STA_1_SEEK_ERR;
	
		logerror("failed seek verify!");
	}
	
	
	/* clear a data request */
	static void wd179x_clear_data_request()
	{
		WD179X w = wd;
	
	//	w.status_drq = 0;
		if (w.callback != null)
			w.callback.handler(WD179X_DRQ_CLR);
		w.status &= ~STA_2_DRQ;
	}
	
	/* set data request */
	static void wd179x_set_data_request()
	{
		WD179X w = wd;
	
		if ((w.status & STA_2_DRQ) != 0)
		{
			w.status |= STA_2_LOST_DAT;
	//		return;
		}
	
		/* set drq */
	//	w.status_drq = STA_2_DRQ;
		if (w.callback != null)
			w.callback.handler(WD179X_DRQ_SET);
		w.status |= STA_2_DRQ;
	}
	
	/* callback for data transfers */
	static timer_callback wd179x_data_timer_callback = new timer_callback() {
            public void handler(int code) {
                WD179X w = wd;
	
		/* ok, trigger data request now */
		wd179x_set_data_request();
	
		/* stop it, but don't allow it to be free'd */
		timer_reset(w.timer, TIME_NEVER); 
            }
        };
	
	/* callback to initiate read sector */	
	static timer_callback wd179x_read_sector_callback = new timer_callback() {
            public void handler(int code) {
		WD179X w = wd;
	
		/* ok, start that read! */
	
                if (VERBOSE != 0){
                        logerror("wd179x: Read Sector callback.\n");
                }
	
		if (floppy_drive_get_flag_state(drv, FLOPPY_DRIVE_READY) == 0)
			wd179x_complete_command(w, 1);
		else
			wd179x_read_sector(w);
	
		/* stop it, but don't allow it to be free'd */
		if (w.timer_rs !=null)
			timer_reset(w.timer_rs, TIME_NEVER); 
            }
        };
	
	/* callback to initiate write sector */
	
	static timer_callback wd179x_write_sector_callback = new timer_callback() {
            public void handler(int code) {
		WD179X w = wd;
	
		/* ok, start that write! */
	
                if (VERBOSE != 0){
                        logerror("wd179x: Write Sector callback.\n");
                }
	
		if (floppy_drive_get_flag_state(drv, FLOPPY_DRIVE_READY) == 0)
			wd179x_complete_command(w, 1);
		else
		{
	
			/* drive write protected? */
			if (floppy_drive_get_flag_state(drv,FLOPPY_DRIVE_DISK_WRITE_PROTECTED) != 0)
			{
				w.status |= STA_2_WRITE_PRO;
	
				wd179x_complete_command(w, 1);
			}
			else
			{
				/* attempt to find it first before getting data from cpu */
				if (wd179x_find_sector(w) != 0)
				{
					/* request data */
					w.data_offset = 0;
					w.data_count = w.sector_length;
	
					wd179x_set_data_request();
	
					w.status |= STA_2_BUSY;
					w.busy_count = 0;
				}
			}
		}
	
		/* stop it, but don't allow it to be free'd */
		if (w.timer_ws !=null)
			timer_reset(w.timer_ws, TIME_NEVER); 
            }
        };
	
	/* setup a timed data request - data request will be triggered in a few usecs time */
	static void wd179x_timed_data_request()
	{
		int usecs;
		WD179X w = wd;
	
		usecs = floppy_drive_get_datarate_in_us(w.density);
	
		/* remove old timer if it exists */
		if (w.timer!=null)
		{
			timer_remove(w.timer);
			w.timer = null;
		}
	
		/* set new timer */
		w.timer = timer_set(TIME_IN_USEC(usecs), 0, wd179x_data_timer_callback);
	}
	
	/* setup a timed read sector - read sector will be triggered in a few usecs time */
	static void wd179x_timed_read_sector_request()
	{
		int usecs;
		WD179X w = wd;
	
		usecs = 20; /* How long should we wait? How about 20 micro seconds? */
	
		/* remove old timer if it exists */
		if (w.timer_rs !=null)
		{
			timer_remove(w.timer_rs);
			w.timer_rs = null;
		}
	
		/* set new timer */
		w.timer_rs = timer_set(TIME_IN_USEC(usecs), 0, wd179x_read_sector_callback);
	}
	
	/* setup a timed write sector - write sector will be triggered in a few usecs time */
	static void wd179x_timed_write_sector_request()
	{
		int usecs;
		WD179X w = wd;
	
		usecs = 20; /* How long should we wait? How about 20 micro seconds? */
	
		/* remove old timer if it exists */
		if (w.timer_ws !=null)
		{
			timer_remove(w.timer_ws);
			w.timer_ws = null;
		}
	
		/* set new timer */
		w.timer_ws = timer_set(TIME_IN_USEC(usecs), 0, wd179x_write_sector_callback);
	}
	
	
	/* read the FDC status register. This clears IRQ line too */
	public static ReadHandlerPtr wd179x_status_r = new ReadHandlerPtr() {
            public int handler(int offset) {
                WD179X w = wd;
		int result = w.status;
	
		if (w.callback != null)
			w.callback.handler(WD179X_IRQ_CLR);
	//	if (w.busy_count)
	//	{
	//		if (!--w.busy_count)
	//			w.status &= ~STA_1_BUSY;
	//	}
	
		/* type 1 command or force int command? */
		if ((w.command_type==TYPE_I) || (w.command_type==TYPE_IV))
		{
	
			/* if disc present toggle index pulse */
			if (floppy_drive_get_flag_state(drv, FLOPPY_DRIVE_DISK_INSERTED) != 0)
			{
				/* eventually toggle index pulse bit */
				w.status ^= STA_1_IPL;
			}
	
			/* set track 0 state */
			result &=~STA_1_TRACK0;
			if (floppy_drive_get_flag_state(drv, FLOPPY_DRIVE_HEAD_AT_TRACK_0) != 0)
				result |= STA_1_TRACK0;
	
		//	floppy_drive_set_ready_state(drv, 1,1);
			w.status &= ~STA_1_NOT_READY;
			
			if (w.type == WD_TYPE_179X)
			{
				if (floppy_drive_get_flag_state(drv, FLOPPY_DRIVE_READY) == 0)
					w.status |= STA_1_NOT_READY;
			}
			else
			{
				if (floppy_drive_get_flag_state(drv, FLOPPY_DRIVE_READY) != 0)
					w.status |= STA_1_NOT_READY;
			}
		}
		
		/* eventually set data request bit */
	//	w.status |= w.status_drq;
	
                if (VERBOSE != 0){
                        if (w.data_count < 4)
                                logerror("wd179x_status_r: $%02X (data_count %d)\n", result, w.data_count);
                }
	
	
		return result;
            }
        };
	
	/* read the FDC track register */
	public static ReadHandlerPtr wd179x_track_r = new ReadHandlerPtr() {
            public int handler(int offset) {
                WD179X w = wd;
	
                if (VERBOSE != 0){
                        logerror("wd179x_track_r: $%02X\n", w.track_reg);
                }
		return w.track_reg;
            }
        };
	{
		
	}
	
	/* read the FDC sector register */
	public static ReadHandlerPtr wd179x_sector_r = new ReadHandlerPtr() {
            public int handler(int offset) {
		WD179X w = wd;
	
                if (VERBOSE != 0){
                        logerror("wd179x_sector_r: $%02X\n", w.sector);
                }
		return w.sector;
            }
        };
	
	/* read the FDC data register */
	public static ReadHandlerPtr wd179x_data_r = new ReadHandlerPtr() {
            public int handler(int offset) {
		WD179X w = wd;
	
		if (w.data_count >= 1)
		{
			/* clear data request */
			wd179x_clear_data_request();
	
			/* yes */
			w.data = w.buffer[w.data_offset++];
	
                        if (VERBOSE_DATA != 0){
                                        logerror("wd179x_data_r: $%02X (data_count %d)\n", w.data, w.data_count);
                        }
			/* any bytes remaining? */
			if (--w.data_count < 1)
			{
				/* no */
				w.data_offset = 0;
	
				/* clear ddam type */
				w.status &=~STA_2_REC_TYPE;
				/* read a sector with ddam set? */
				if (w.command_type == TYPE_II && w.ddam != 0)
				{
					/* set it */
					w.status |= STA_2_REC_TYPE;
				}
	
				/* not incremented after each sector - only incremented in multi-sector
				operation. If this remained as it was oric software would not run! */
			//	w.sector++;
				/* Delay the INTRQ 3 byte times becuase we need to read two CRC bytes and
				   compare them with a calculated CRC */
				wd179x_complete_command(w, 3);
			}
			else
			{
				/* issue a timed data request */
				wd179x_timed_data_request();		
			}
		}
		else
		{
			logerror("wd179x_data_r: (no new data) $%02X (data_count %d)\n", w.data, w.data_count);
		}
		return w.data;
            }
        };
	
	/* write the FDC command register */
	public static WriteHandlerPtr wd179x_command_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                WD179X w = wd;
	
		floppy_drive_set_motor_state(drv, 1);
		floppy_drive_set_ready_state(drv, 1,0);
		/* also cleared by writing command */
		if (w.callback != null)
				w.callback.handler(WD179X_IRQ_CLR);
	
		/* clear write protected. On read sector, read track and read dam, write protected bit is clear */
		w.status &= ~((1<<6) | (1<<5) | (1<<4));
	
		if ((data & ~FDC_MASK_TYPE_IV) == FDC_FORCE_INT)
		{
                        if (VERBOSE != 0){
                                        logerror("wd179x_command_w $%02X FORCE_INT (data_count %d)\n", data, w.data_count);
                        }
			w.data_count = 0;
			w.data_offset = 0;
			w.status &= ~(STA_2_BUSY);
			
			wd179x_clear_data_request();
	
			if ((data & 0x0f) != 0)
			{
	
	
	
			}
	
	
	//		w.status_ipl = STA_1_IPL;
	/*		w.status_ipl = 0; */
			
			w.busy_count = 0;
			w.command_type = TYPE_IV;
	        return;
		}
	
		if ((data & 0x80) != 0)
		{
			/*w.status_ipl = 0;*/
	
			if ((data & ~FDC_MASK_TYPE_II) == FDC_READ_SEC)
			{
                                if (VERBOSE != 0){
                                                        logerror("wd179x_command_w $%02X READ_SEC\n", data);
                                }
				w.read_cmd = data;
				w.command = data & ~FDC_MASK_TYPE_II;
				w.command_type = TYPE_II;
				w.status &= ~STA_2_LOST_DAT;
				wd179x_clear_data_request();
	
				wd179x_timed_read_sector_request();
	
				return;
			}
	
			if ((data & ~FDC_MASK_TYPE_II) == FDC_WRITE_SEC)
			{
                                if (VERBOSE != 0){
                                                        logerror("wd179x_command_w $%02X WRITE_SEC\n", data);
                                }
				w.write_cmd = data;
				w.command = data & ~FDC_MASK_TYPE_II;
				w.command_type = TYPE_II;
				w.status &= ~STA_2_LOST_DAT;
				wd179x_clear_data_request();
	
				wd179x_timed_write_sector_request();
	
				return;
			}
	
			if ((data & ~FDC_MASK_TYPE_III) == FDC_READ_TRK)
			{
                                if (VERBOSE != 0){
                                                        logerror("wd179x_command_w $%02X READ_TRK\n", data);
                                }
				w.command = data & ~FDC_MASK_TYPE_III;
				w.command_type = TYPE_III;
				w.status &= ~STA_2_LOST_DAT;
				wd179x_clear_data_request();
	//#if 1
	//			w.status = seek(w, w.track, w.head, w.sector);
				if (w.status == 0)
					read_track(w);
	//#endif
				return;
			}
	
			if ((data & ~FDC_MASK_TYPE_III) == FDC_WRITE_TRK)
			{
                                if (VERBOSE != 0){
                                                        logerror("wd179x_command_w $%02X WRITE_TRK\n", data);
                                }
				w.command_type = TYPE_III;
				w.status &= ~STA_2_LOST_DAT;
				wd179x_clear_data_request();
	
				if (floppy_drive_get_flag_state(drv, FLOPPY_DRIVE_READY) == 0)
	            {
					wd179x_complete_command(w, 1);
	            }
	            else
	            {
	    
	                /* drive write protected? */
	                if (floppy_drive_get_flag_state(drv,FLOPPY_DRIVE_DISK_WRITE_PROTECTED) != 0)
	                {
	                    /* yes */
	                    w.status |= STA_2_WRITE_PRO;
	                    /* quit command */
	                    wd179x_complete_command(w, 1);
	                }
	                else
	                {
	                    w.command = data & ~FDC_MASK_TYPE_III;
	                    w.data_offset = 0;
	                    w.data_count = (w.density != null) ? TRKSIZE_DD : TRKSIZE_SD;
	                    wd179x_set_data_request();
	
	                    w.status |= STA_2_BUSY;
	                    w.busy_count = 0;
	                }
	            }
	            return;
			}
	
			if ((data & ~FDC_MASK_TYPE_III) == FDC_READ_DAM)
			{
                                if (VERBOSE != 0){
                                                        logerror("wd179x_command_w $%02X READ_DAM\n", data);
                                }
				w.command_type = TYPE_III;
				w.status &= ~STA_2_LOST_DAT;
	  			wd179x_clear_data_request();
	
				if (floppy_drive_get_flag_state(drv, FLOPPY_DRIVE_READY) == 0)
	            {
					wd179x_complete_command(w, 1);
	            }
	            else
	            {
	                wd179x_read_id(w);
	            }
				return;
			}
	
                        if (VERBOSE != 0){
                                        logerror("wd179x_command_w $%02X unknown\n", data);
                        }
			return;
		}
	
		w.status |= STA_1_BUSY;
		
		/* clear CRC error */
		w.status &=~STA_1_CRC_ERR;
	
		if ((data & ~FDC_MASK_TYPE_I) == FDC_RESTORE)
		{
	
                        if (VERBOSE != 0){
                                        logerror("wd179x_command_w $%02X RESTORE\n", data);
                        }
			wd179x_restore(w);
		}
	
		if ((data & ~FDC_MASK_TYPE_I) == FDC_SEEK)
		{
			int newtrack;
	
			logerror("old track: $%02x new track: $%02x\n", w.track_reg, w.data);
			w.command_type = TYPE_I;
	
			/* setup step direction */
			if (w.track_reg < w.data)
			{
				if (VERBOSE != 0){
        				logerror("direction: +1\n");
                                }
				w.direction = 1;
			}
			else
			if (w.track_reg > w.data)
                        {
                                if (VERBOSE != 0){
                                    logerror("direction: -1\n");
                                }
				w.direction = -1;
			}
	
			newtrack = w.data;
                        if (VERBOSE != 0){
                                        logerror("wd179x_command_w $%02X SEEK (data_reg is $%02X)\n", data, newtrack);
                        }
	
			/* reset busy count */
			w.busy_count = 0;
	
			/* keep stepping until reached track programmed */
			while (w.track_reg != newtrack)
			{
				/* update time to simulate seek time busy signal */
				w.busy_count++;
	
				/* update track reg */
				w.track_reg += w.direction;
	
				floppy_drive_seek(drv, w.direction);
			}
	
			/* simulate seek time busy signal */
			w.busy_count = 0;	//w.busy_count * ((data & FDC_STEP_RATE) + 1);
	/*TODO*///#if 0
	/*TODO*///		wd179x_set_irq(w);
	/*TODO*///#endif
			wd179x_set_busy(w,0.1);
	
		}
	
		if ((data & ~(FDC_STEP_UPDATE | FDC_MASK_TYPE_I)) == FDC_STEP)
		{
                        if (VERBOSE != 0){
                                        logerror("wd179x_command_w $%02X STEP dir %+d\n", data, w.direction);
                        }
			w.command_type = TYPE_I;
	        /* if it is a real floppy, issue a step command */
			/* simulate seek time busy signal */
			w.busy_count = 0;	//((data & FDC_STEP_RATE) + 1);
	
			floppy_drive_seek(drv, w.direction);
	
			if ((data & FDC_STEP_UPDATE) != 0)
				w.track_reg += w.direction;
	
	/*TODO*///#if 0
	/*TODO*///		wd179x_set_irq(w);
	/*TODO*///#endif
			wd179x_set_busy(w,0.1);
	
	
		}
	
		if ((data & ~(FDC_STEP_UPDATE | FDC_MASK_TYPE_I)) == FDC_STEP_IN)
		{
                        if (VERBOSE != 0){
                                        logerror("wd179x_command_w $%02X STEP_IN\n", data);
                        }
			w.command_type = TYPE_I;
                        w.direction = +1;
			/* simulate seek time busy signal */
			w.busy_count = 0;	//((data & FDC_STEP_RATE) + 1);
	
			floppy_drive_seek(drv, w.direction);
	
			if ((data & FDC_STEP_UPDATE) != 0)
				w.track_reg += w.direction;
	/*TODO*///#if 0
	/*TODO*///		wd179x_set_irq(w);
	/*TODO*///#endif
			wd179x_set_busy(w,0.1);
	
		}
	
		if ((data & ~(FDC_STEP_UPDATE | FDC_MASK_TYPE_I)) == FDC_STEP_OUT)
		{
                        if (VERBOSE != 0){
                                        logerror("wd179x_command_w $%02X STEP_OUT\n", data);
                        }
			w.command_type = TYPE_I;
                        w.direction = -1;
			/* simulate seek time busy signal */
			w.busy_count = 0;	//((data & FDC_STEP_RATE) + 1);
	
			/* for now only allows a single drive to be selected */
			floppy_drive_seek(drv, w.direction);
	
			if ((data & FDC_STEP_UPDATE) != 0)
				w.track_reg += w.direction;
	
	/*TODO*///#if 0
	/*TODO*///		wd179x_set_irq(w);
	/*TODO*///#endif
			wd179x_set_busy(w,0.1);
		}
	
	//	if (w.busy_count==0)
	//		w.status &= ~STA_1_BUSY;
	
	//	/* toggle index pulse at read */
	//	w.status_ipl = STA_1_IPL;
	
		/* 0 is enable spin up sequence, 1 is disable spin up sequence */
		if ((data & FDC_STEP_HDLOAD)==0)
			w.status |= STA_1_HD_LOADED;
	
		if ((data & FDC_STEP_VERIFY) != 0)
		{
			/* verify seek */
			wd179x_verify_seek(w);
		}
            }
        };
	
	/* write the FDC track register */
	public static WriteHandlerPtr wd179x_track_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                WD179X w = wd;
		w.track_reg = data;
	
                if (VERBOSE != 0){
                        logerror("wd179x_track_w $%02X\n", data);
                }
            }
        };
	
	
	/* write the FDC sector register */
	public static WriteHandlerPtr wd179x_sector_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
		WD179X w = wd;
		w.sector = data;
                if (VERBOSE != 0){
                        logerror("wd179x_sector_w $%02X\n", data);
                }
            }
        };
	
	/* write the FDC data register */
	public static WriteHandlerPtr wd179x_data_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
		WD179X w = wd;
	
		if (w.data_count > 0)
		{
			/* clear data request */
			wd179x_clear_data_request();
	
			/* put byte into buffer */
                        if (VERBOSE_DATA != 0){
                                        logerror("WD179X buffered data: $%02X at offset %d.\n", data, w.data_offset);
                        }
		
			w.buffer[w.data_offset++] = (char) data;
			
			if (--w.data_count < 1)
			{
				w.data_offset = 0;
	
				if (w.command == FDC_WRITE_TRK)
					write_track(w);
				else
					wd179x_write_sector(w);
	
				wd179x_complete_command(w, 3);
			}
			else
			{
				/* yes... setup a timed data request */
				wd179x_timed_data_request();
			}
	
		}
                else if (VERBOSE != 0){
		
			logerror("wd179x_data_w $%02X\n", data);
		}
	
		w.data = data;
            }
        };
	
}
