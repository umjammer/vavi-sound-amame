/* This is a basic disc image format.
Each driver which uses this must use basicdsk_set_geometry, so that
the data will be accessed correctly */


/* THIS DISK IMAGE CODE USED TO BE PART OF THE WD179X EMULATION, EXTRACTED INTO THIS FILE */
/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
package mess056.machine;

import static arcadeflex056.fileio.*;
import static arcadeflex056.osdepend.logerror;
import static common.libc.cstdio.*;
import static common.libc.cstring.*;
import static common.ptr.*;
import static consoleflex056.funcPtr.*;

import static mame056.osdependH.*;
import static mess056.device.*;
import static mess056.deviceH.*;
import static mess056.includes.flopdrvH.*;
import static mess056.includes.basicdskH.*;
import static mess056.machine.flopdrv.*;
import static mess056.mess.*;
import static mess056.messH.*;

/**
 *
 * @author chusogar
 */
public class basicdsk {
    
    public static int basicdsk_MAX_DRIVES = 4;
    
    public static int VERBOSE = 1;
    
    static _basicdsk[]     basicdsk_drives = new _basicdsk[basicdsk_MAX_DRIVES];
    
    static {
        for (int i = 0 ; i<basicdsk_MAX_DRIVES ; i++)
            basicdsk_drives[i] = new _basicdsk();
    }

    public static floppy_interface basicdsk_floppy_interface=new floppy_interface() {
        @Override
        public void seek_callback(int drive, int physical_track) {
            System.out.println("seek_callback");
            basicdsk_seek_callback(drive, physical_track);
        }

        @Override
        public int get_sectors_per_track(int drive, int physical_side) {
            System.out.println("get_sectors_per_track");
            return basicdsk_get_sectors_per_track(drive, physical_side);
        }

        @Override
        public void get_id_callback(int drive, chrn_id id_chrn, int id_index, int physical_side) {
            System.out.println("get_id_callback");
            basicdsk_get_id_callback(drive, id_chrn, id_index, physical_side);
        }

        @Override
        public void read_sector_data_into_buffer(int drive, int side, int data_id, char[] buf, int length) {
            System.out.println("read_sector_data_into_buffer");
            basicdsk_read_sector_data_into_buffer(drive, side, data_id, buf, length);
        }

        @Override
        public void write_sector_data_from_buffer(int drive, int side, int data_id, char[] buf, int length, int ddam) {
            System.out.println("write_sector_data_from_buffer");
            basicdsk_write_sector_data_from_buffer(drive, side, data_id, buf, length, ddam);
        }

        @Override
        public void read_track_data_info_buffer(int drive, int side, char[] ptr, int length) {
            System.out.println("read_track_data_info_buffer");
            // nothing to do
        }

        @Override
        public void format_sector(int drive, int side, int sector_index, int c, int h, int r, int n, int filler) {
            System.out.println("format_sector");
            // nothing to do
        }
    };
    /*{
            basicdsk_seek_callback,
            basicdsk_get_sectors_per_track,
            basicdsk_get_id_callback,
            basicdsk_read_sector_data_into_buffer,
            basicdsk_write_sector_data_from_buffer,
            null,
            null
    };*/

    /* attempt to insert a disk into the drive specified with id */
    public static int basicdsk_floppy_init(int id)
    {
        System.out.println("basicdsk_floppy_init "+id);
            String name = device_filename(IO_FLOPPY, id);

            if (id < basicdsk_MAX_DRIVES)
            {
                
                    //_basicdsk w = basicdsk_drives[id];
                    
                    /* do we have an image name ? */
                    //if (name==null || !name[0])
                    if (name==null)
                    {
                            return INIT_PASS;
                    }
                    basicdsk_drives[id].mode = 1;
                    basicdsk_drives[id].image_file = image_fopen(IO_FLOPPY, id, OSD_FILETYPE_IMAGE_R, OSD_FOPEN_READ);
                    
                    if( basicdsk_drives[id].image_file == null )
                    {
                            basicdsk_drives[id].mode = 0;
                            basicdsk_drives[id].image_file = image_fopen(IO_FLOPPY, id, OSD_FILETYPE_IMAGE_R, OSD_FOPEN_READ);
                            if( basicdsk_drives[id].image_file == null )
                            {
                                    basicdsk_drives[id].mode = 1;
                                    basicdsk_drives[id].image_file = image_fopen(IO_FLOPPY, id, OSD_FILETYPE_IMAGE_R, OSD_FOPEN_RW_CREATE);
                            }
                    }

                    /* this will be setup in the set_geometry function */
                    basicdsk_drives[id].ddam_map = null;

                    /* the following line is unsafe, but floppy_drives_init assumes we start on track 0,
                    so we need to reflect this */
                    basicdsk_drives[id].track = 0;
                    //basicdsk_drives[id] = w;
                                        
                    floppy_drive_set_disk_image_interface(id,basicdsk_floppy_interface);
                                        
                    return  INIT_PASS;
            }
            System.out.println("basicdsk_floppy_init exit");

            return INIT_FAIL;
    }

    /* remove a disk from the drive specified by id */
    public static io_exitPtr basicdsk_floppy_exit = new io_exitPtr() {
        public int handler(int id) {
            _basicdsk pDisk;

            /* sanity check */
            if ((id<0) || (id>=basicdsk_MAX_DRIVES))
                    return INIT_FAIL;

            pDisk = basicdsk_drives[id];

            /* if file was opened, close it */
            if (pDisk.image_file!=null)
            {
                    osd_fclose(pDisk.image_file);
                    pDisk.image_file = null;
            }

            /* free ddam map */
            if (pDisk.ddam_map!=null)
            {
                    pDisk.ddam_map = null;
            }
            
            return INIT_PASS;
        }
    };
    
    /* set data mark/deleted data mark for the sector specified. If ddam!=0, the sector will
    have a deleted data mark, if ddam==0, the sector will have a data mark */
    public static void basicdsk_set_ddam(int id, int physical_track, int physical_side, int sector_id, int ddam)
    {
        System.out.println("basicdsk_set_ddam exit");
            long ddam_bit_offset, ddam_bit_index, ddam_byte_offset;
            _basicdsk pDisk;

            /* sanity check */
            if (id>=basicdsk_MAX_DRIVES)
                    return;

            pDisk = basicdsk_drives[id];

            if (pDisk.ddam_map == null)
                    return;

            logerror("basicdsk_set_ddam: #%d T:$%02x H:%d S:$%02x = %d\n",id, physical_track, physical_side, sector_id,ddam);

        /* calculate bit-offset into map */
            ddam_bit_offset = (((physical_track * pDisk.heads) + physical_side)*pDisk.sec_per_track) +
                                            sector_id - pDisk.first_sector_id;

            /* if offset exceeds the number of bits that are stored in the ddam map return 0 */
            if (ddam_bit_offset>=(pDisk.ddam_map_size<<3))
                    return;

            /* calculate byte offset */
            ddam_byte_offset = ddam_bit_offset>>3;
            /* calc bit index within byte */
            ddam_bit_index = ddam_bit_offset & 0x07;

            /* clear bit */
            pDisk.ddam_map.write((int) ddam_byte_offset, pDisk.ddam_map.read((int) ddam_byte_offset) & ~(1<<ddam_bit_index));

            /* deleted dam? */
            if (ddam != 0)
            {
                    /* set deleted dam */
                    pDisk.ddam_map.write((int) ddam_byte_offset, pDisk.ddam_map.read((int) ddam_byte_offset) | (1<<ddam_bit_index));
            }
    }

    /* get dam state for specified sector */
    public static int basicdsk_get_ddam(int id, int physical_track, int physical_side, int sector_id)
    {
        System.out.println("basicdsk_get_ddam exit");
            long ddam_bit_offset, ddam_bit_index, ddam_byte_offset;
            _basicdsk pDisk;

            /* sanity check */
            if (id>=basicdsk_MAX_DRIVES)
                    return 0;

            pDisk = basicdsk_drives[id];

            if (pDisk.ddam_map==null)
                    return 0;

            /* calculate bit-offset into map */
            ddam_bit_offset = (((physical_track * pDisk.heads) + physical_side)*pDisk.sec_per_track) +
                                            sector_id - pDisk.first_sector_id;

            /* if offset exceeds the number of bits that are stored in the ddam map return 0 */
            if (ddam_bit_offset>=(pDisk.ddam_map_size<<3))
                    return 0;

            /* calculate byte offset */
            ddam_byte_offset = ddam_bit_offset>>3;
            /* calc bit index within byte */
            ddam_bit_index = ddam_bit_offset & 0x07;

            /* clear bit */
            return ((pDisk.ddam_map.read((int) ddam_byte_offset) & (1<<ddam_bit_index))!=0 ? 1 : 0);
    }


    /* dir_sector is a relative offset from the start of the disc,
    dir_length is a relative offset from the start of the disc */
    public static void basicdsk_set_geometry(int drive, int tracks, int heads, int sec_per_track, int sector_length, int first_sector_id, int offset_track_zero)
    {
        System.out.println("basicdsk_set_geometry");
            //_basicdsk pDisk;
            long N;
            long ShiftCount;


            if (drive >= basicdsk_MAX_DRIVES)
            {
                    logerror("basicdsk drive #%d not supported!\n", drive);
                    return;
            }

            //pDisk = basicdsk_drives[drive];


            if (VERBOSE != 0){
                    logerror("basicdsk geometry for drive #%d is %d tracks, %d heads, %d sec/track, %d bytes per sector, first sector id: %d, file offset to track 0: %d\n",
                            drive, tracks, heads, sec_per_track, sector_length, first_sector_id, offset_track_zero);
            }

            basicdsk_drives[drive].tracks = tracks;
            basicdsk_drives[drive].heads = heads;
            basicdsk_drives[drive].first_sector_id = first_sector_id;
            basicdsk_drives[drive].sec_per_track = sec_per_track;
            basicdsk_drives[drive].sector_length = sector_length;
            basicdsk_drives[drive].offset = offset_track_zero;

            floppy_drive_set_geometry_absolute( drive, tracks, heads );

            basicdsk_drives[drive].image_size = basicdsk_drives[drive].tracks * basicdsk_drives[drive].heads * basicdsk_drives[drive].sec_per_track * basicdsk_drives[drive].sector_length;

            /* if a ddam map was already set up clear it */
            if (basicdsk_drives[drive].ddam_map!=null)
            {
                    basicdsk_drives[drive].ddam_map = null;
            }
            /* setup a new ddam map */
            basicdsk_drives[drive].ddam_map_size = ((basicdsk_drives[drive].tracks * basicdsk_drives[drive].heads * basicdsk_drives[drive].sec_per_track)+7)>>3;
            basicdsk_drives[drive].ddam_map = new UBytePtr((int)basicdsk_drives[drive].ddam_map_size);

            if (basicdsk_drives[drive].ddam_map!=null)
            {
                    memset(basicdsk_drives[drive].ddam_map, 0, (int)basicdsk_drives[drive].ddam_map_size);
            }


            /* from sector length calculate N value for sector id's */
            /* N = 0 for 128, N = 1 for 256, N = 2 for 512 ... */
            N = (basicdsk_drives[drive].sector_length);
            ShiftCount = 0;

            if (N!=0)
            {
                    while ((N & 0x080000000)==0)
                    {
                            N = N<<1;
                            ShiftCount++;
                    }

                    /* get left-shift required to shift 1 to this
                    power of 2 */

                    basicdsk_drives[drive].N = (int) ((31 - ShiftCount)-7);
            }
            else
            {
                    basicdsk_drives[drive].N = 0;
            }
            
            //basicdsk_drives[drive] = pDisk;
    }


    /* seek to track/head/sector relative position in image file */
    public static int basicdsk_seek(_basicdsk w, int t, int h, int s)
    {
        System.out.println("basicdsk_seek");
        int offset;
            /* allow two additional tracks */
        if (t >= w.tracks + 2)
            {
                    logerror("basicdsk track %d >= %d\n", t, w.tracks + 2);
                    return 0;
            }

        if (h >= w.heads)
        {
                    logerror("basicdsk head %d >= %d\n", h, w.heads);
                    return 0;
            }

        if (s >= (w.first_sector_id + w.sec_per_track))
            {
                    logerror("basicdsk sector %d\n", w.sec_per_track+w.first_sector_id);
                    return 0;
            }

            offset = 0;
            offset += t;
            offset *= w.heads;
            offset += h;
            offset *= w.sec_per_track;
            offset += (s-w.first_sector_id);
            offset *= w.sector_length;
            offset += w.offset;


            if (VERBOSE != 0) {
                logerror("basicdsk seek track:%d head:%d sector:%d. offset #0x%08lX\n",
                         t, h, s, offset);
            }

            if (offset > w.image_size)
            {
                    logerror("basicdsk seek offset %ld >= %ld\n", offset, w.image_size);
                    return 0;
            }

            if (osd_fseek(w.image_file, offset, SEEK_SET) < 0)
            {
                    logerror("basicdsk seek failed\n");
                    return 0;
            }

            return 1;
    }




    /*TODO*///#if 0
    /*TODO*///                        w.status = seek(w, w.track, w.head, w.sector);
    /*TODO*///                        if (w.status == 0)
    /*TODO*///                                read_sector(w);
/*TODO*///
/*TODO*///
    /*TODO*///        /* if a track was just formatted */
    /*TODO*///        if (w.dam_cnt)
    /*TODO*///        {
    /*TODO*///                int i;
    /*TODO*///                for (i = 0; i < w.dam_cnt; i++)
    /*TODO*///                {
    /*TODO*///                        if (w.track == w.dam_list[i][0] &&
    /*TODO*///                                w.head == w.dam_list[i][1] &&
    /*TODO*///                                w.sector == w.dam_list[i][2])
    /*TODO*///                        {
    /*TODO*///#if VERBOSE
    /*TODO*///                                logerror("basicdsk reading formatted sector %d, track %d, head %d\n", w.sector, w.track, w.head);
    /*TODO*///#endif
    /*TODO*///                                w.data_offset = w.dam_data[i];
    /*TODO*///                                return;
    /*TODO*///                        }
    /*TODO*///                }
    /*TODO*///                /* sector not found, now the track buffer is invalid */
    /*TODO*///                w.dam_cnt = 0;
    /*TODO*///        }
/*TODO*///
    /*TODO*///    /* if this is the real thing */
    /*TODO*///    if (w.image_file == REAL_FDD)
    /*TODO*///    {
    /*TODO*///        int tries = 3;
    /*TODO*///                do {
    /*TODO*///                        w.status = osd_fdc_get_sector(w.track, w.head, w.head, w.sector, w.buffer);
    /*TODO*///                        tries--;
    /*TODO*///                } while (tries && (w.status & (STA_2_REC_N_FND | STA_2_CRC_ERR | STA_2_LOST_DAT)));
    /*TODO*///                /* no error bits set ? */
    /*TODO*///                if ((w.status & (STA_2_REC_N_FND | STA_2_CRC_ERR | STA_2_LOST_DAT)) == 0)
    /*TODO*///                {
    /*TODO*///                        /* start transferring data to the emulation now */
    /*TODO*///                        w.status_drq = STA_2_DRQ;
    /*TODO*///                        if (w.callback)
    /*TODO*///                                (*w.callback) (basicdsk_DRQ_SET);
    /*TODO*///                        w.status |= STA_2_DRQ | STA_2_BUSY;
    /*TODO*///        }
    /*TODO*///        return;
    /*TODO*///    }
    /*TODO*///        else
    /*TODO*///        if (osd_fread(w.image_file, w.buffer, w.sector_length) != w.sector_length)
    /*TODO*///        {
    /*TODO*///                w.status = STA_2_LOST_DAT;
    /*TODO*///                return;
    /*TODO*///        }
/*TODO*///
/*TODO*///
    /*TODO*///#endif



    public static void basicdsk_step_callback(_basicdsk w, int drive, int direction)
    {
        System.out.println("basicdsk_step_callback");
                            w.track += direction;
    }

    /*TODO*///#if 0
    /*TODO*////* write a sector */
    /*TODO*///static void basicdsk_write_sector(basicdsk *w)
    /*TODO*///{
/*TODO*///
    /*TODO*///        if (w.image_file == REAL_FDD)
    /*TODO*///        {
    /*TODO*///                osd_fdc_put_sector(w.track, w.head, w.head, w.sector, w.buffer, w.write_cmd & FDC_DELETED_AM);
    /*TODO*///                return;
    /*TODO*///        }
/*TODO*///
    /*TODO*///        seek(w, w.track, w.head, w.sector);
    /*TODO*///        osd_fwrite(w.image_file, w.buffer, w.data_offset)
    /*TODO*///}
/*TODO*///
/*TODO*///
    /*TODO*////* write an entire track by extracting the sectors */
    /*TODO*///static void basicdsk_write_track(basicdsk *w)
    /*TODO*///{
    /*TODO*///        if (floppy_drive_get_flag_state(drv,FLOPPY_DRIVE_DISK_WRITE_PROTECTED))
    /*TODO*///    {
    /*TODO*///                w.status |= STA_1_WRITE_PRO;
    /*TODO*///                return;
    /*TODO*///        }
    /*TODO*///#endif

    /*TODO*///#if 0
    /*TODO*///UINT8 *f;
    /*TODO*///int cnt;
    /*TODO*///        w.dam_cnt = 0;
    /*TODO*///    if (w.image_file != REAL_FDD && w.mode == 0)
    /*TODO*///    {
    /*TODO*///#if VERBOSE
    /*TODO*///                logerror("basicdsk write_track write protected image\n");
    /*TODO*///#endif
    /*TODO*///        w.status = STA_2_WRITE_PRO;
    /*TODO*///        return;
    /*TODO*///    }
/*TODO*///
    /*TODO*///        memset(w.dam_list, 0xff, sizeof(w.dam_list));
    /*TODO*///        memset(w.dam_data, 0x00, sizeof(w.dam_data));
/*TODO*///
    /*TODO*///        f = w.buffer;
    /*TODO*///#if VERBOSE
    /*TODO*///        logerror("basicdsk write_track %s_LOW\n", (w.density) ? "MFM" : "FM" );
    /*TODO*///#endif
    /*TODO*///    cnt = (w.density) ? TRKSIZE_DD : TRKSIZE_SD;
/*TODO*///
    /*TODO*///        do
    /*TODO*///        {
    /*TODO*///                while ((--cnt > 0) && (*f != 0xfe))	/* start of DAM ?? */
    /*TODO*///                        f++;
/*TODO*///
    /*TODO*///                if (cnt > 4)
    /*TODO*///                {
    /*TODO*///                int seclen;
    /*TODO*///                        cnt -= 5;
    /*TODO*///                        f++;			   /* skip FE */
    /*TODO*///                        w.dam_list[w.dam_cnt][0] = *f++;	  /* copy track number */
    /*TODO*///                        w.dam_list[w.dam_cnt][1] = *f++;	  /* copy head number */
    /*TODO*///                        w.dam_list[w.dam_cnt][2] = *f++;	  /* copy sector number */
    /*TODO*///                        w.dam_list[w.dam_cnt][3] = *f++;	  /* copy sector length */
    /*TODO*///                        /* sector length in bytes */
    /*TODO*///                        seclen = 128 << w.dam_list[w.dam_cnt][3];
    /*TODO*///#if VERBOSE
    /*TODO*///                        logerror("basicdsk write_track FE @%5d T:%02X H:%02X S:%02X L:%02X\n",
    /*TODO*///                                        (int)(f - w.buffer),
    /*TODO*///                                        w.dam_list[w.dam_cnt][0],w.dam_list[w.dam_cnt][1],
    /*TODO*///                                        w.dam_list[w.dam_cnt][2],w.dam_list[w.dam_cnt][3]);
    /*TODO*///#endif
    /*TODO*///                        /* search start of DATA */
    /*TODO*///                        while ((--cnt > 0) && (*f != 0xf9) && (*f != 0xfa) && (*f != 0xfb))
    /*TODO*///                                f++;
    /*TODO*///                        if (cnt > seclen)
    /*TODO*///                        {
    /*TODO*///                                cnt--;
    /*TODO*///                                /* skip data address mark */
    /*TODO*///                f++;
    /*TODO*///                /* set pointer to DATA to later write the sectors contents */
    /*TODO*///                                w.dam_data[w.dam_cnt] = (int)(f - w.buffer);
    /*TODO*///                                w.dam_cnt++;
    /*TODO*///#if VERBOSE
    /*TODO*///                                logerror("basicdsk write_track %02X @%5d data: %02X %02X %02X %02X ... %02X %02X %02X %02X\n",
    /*TODO*///                                                f[-1],
    /*TODO*///                                                (int)(f - w.buffer),
    /*TODO*////*TODO*///                                                f[0], f[1], f[2], f[3],
    /*TODO*///                                                f[seclen-4], f[seclen-3], f[seclen-2], f[seclen-1]);
    /*TODO*///#endif
    /*TODO*///                                f += seclen;
    /*TODO*///                                cnt -= seclen;
    /*TODO*///                        }
    /*TODO*///        }
    /*TODO*///        } while (cnt > 0);
/*TODO*///
    /*TODO*///        if (w.image_file == REAL_FDD)
    /*TODO*///        {
    /*TODO*///                w.status = osd_fdc_format(w.track, w.head, w.dam_cnt, w.dam_list[0]);
/*TODO*///
    /*TODO*///        if ((w.status & 0xfc) == 0)
    /*TODO*///                {
    /*TODO*///                        /* now put all sectors contained in the format buffer */
    /*TODO*///                        for (cnt = 0; cnt < w.dam_cnt; cnt++)
    /*TODO*///                        {
    /*TODO*///                                w.status = osd_fdc_put_sector(w.track, w.head, cnt, w.buffer[dam_data[cnt]], 0);
    /*TODO*///                                /* bail out if an error occured */
    /*TODO*///                                if (w.status & 0xfc)
    /*TODO*///                                        break;
    /*TODO*///                        }
    /*TODO*///        }
    /*TODO*///    }
    /*TODO*///        else
    /*TODO*///        {
    /*TODO*///                /* now put all sectors contained in the format buffer */
    /*TODO*///                for (cnt = 0; cnt < w.dam_cnt; cnt++)
    /*TODO*///                {
    /*TODO*///                        w.status = seek(w, w.track, w.head, w.dam_list[cnt][2]);
    /*TODO*///                        if (w.status == 0)
    /*TODO*///                        {
    /*TODO*///                                if (osd_fwrite(w.image_file, &w.buffer[w.dam_data[cnt]], w.sector_length) != w.sector_length)
    /*TODO*///                                {
    /*TODO*///                                        w.status = STA_2_LOST_DAT;
    /*TODO*///                                        return;
    /*TODO*///                                }
    /*TODO*///                        }
    /*TODO*///                }
    /*TODO*///        }
    /*TODO*///}
    /*TODO*///#endif
    /*TODO*///#if 0
    /*TODO*///                        if (w.image_file != REAL_FDD)
    /*TODO*///                        {
    /*TODO*///                                /* read normal or deleted data address mark ? */
    /*TODO*///                                w.status |= deleted_dam(w);
    /*TODO*///                        }
    /*TODO*///#endif
/*TODO*///
/*TODO*///
    /*TODO*///#if 0
/*TODO*///
    /*TODO*///        if ((data | 1) == 0xff)	   /* change single/double density ? */
    /*TODO*///        {
    /*TODO*///                /* only supports FM/LO and MFM/LO */
    /*TODO*///                w.density = (data & 1) ? DEN_MFM_LO : DEN_FM_LO;
    /*TODO*///#if 0
    /*TODO*///                if (w.image_file == REAL_FDD)
    /*TODO*///                        osd_fdc_density(w.unit, w.density, w.tracks, w.sec_per_track, w.sec_per_track, 1);
    /*TODO*///#endif
    /*TODO*///                return;
    /*TODO*///        }
    /*TODO*///#endif

    public static void basicdsk_get_id_callback(int drive, chrn_id id, int id_index, int side)
    {
        System.out.println("basicdsk_get_id_callback");
            _basicdsk w = basicdsk_drives[drive];

            /* construct a id value */
            id.C = w.track;
            id.H = side;
            id.R = w.first_sector_id + id_index;
            id.N = w.N;
            id.data_id = w.first_sector_id + id_index;
            id.flags = 0;

            /* get dam */
            if (basicdsk_get_ddam(drive, w.track, side, id.R) != 0)
            {
                    id.flags |= ID_FLAG_DELETED_DATA;
            }

    }

    public static int  basicdsk_get_sectors_per_track(int drive, int side)
    {
        System.out.println("basicdsk_get_sectors_per_track");
            _basicdsk w = basicdsk_drives[drive];

            /* attempting to access an invalid side or track? */
            if ((side>=w.heads) || (w.track>=w.tracks))
            {
                    /* no sectors */
                    return 0;
            }
            /* return number of sectors per track */
            return w.sec_per_track;
    }

    public static void basicdsk_seek_callback(int drive, int physical_track)
    {
        System.out.println("basicdsk_seek_callback");
            _basicdsk w = basicdsk_drives[drive];

            w.track = physical_track;
    }

    public static void basicdsk_write_sector_data_from_buffer(int drive, int side, int index1, char[] ptr, int length, int ddam)
    {
        System.out.println("basicdsk_write_sector_data_from_buffer");
            _basicdsk w = basicdsk_drives[drive];

            if (basicdsk_seek(w, w.track, side, index1)!=0 && w.mode!=0)
            {
                    osd_fwrite(w.image_file, ptr, length);
            }

            basicdsk_set_ddam(drive, w.track, side, index1, ddam);
    }

    public static void basicdsk_read_sector_data_into_buffer(int drive, int side, int index1, char[] ptr, int length)
    {
        System.out.println("basicdsk_read_sector_data_into_buffer exit");
            _basicdsk w = basicdsk_drives[drive];

            if (basicdsk_seek(w, w.track, side, index1) != 0)
            {
                    osd_fread(w.image_file, ptr, length);
            }
    }
    
}
