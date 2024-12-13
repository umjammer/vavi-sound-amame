/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
package mess056.includes;

import static mame056.timer.*;
import mess056.includes.flopdrvH.DENSITY;

/**
 *
 * @author chusogar
 */
public class wd179xH {
    
    public static final int WD179X_IRQ_CLR	= 0;
    public static final int WD179X_IRQ_SET	= 1;
    /* R Nabet : added events for the DRQ pin... */
    public static final int WD179X_DRQ_CLR	= 2;
    public static final int WD179X_DRQ_SET	= 3;


    public static int TYPE_I		= 1;
    public static int TYPE_II 		= 2;
    public static int TYPE_III		= 3;
    public static int TYPE_IV 		= 4;

    public static int FDC_STEP_RATE   = 0x03;    /* Type I additional flags */
    public static int FDC_STEP_VERIFY = 0x04;	/* verify track number */
    public static int FDC_STEP_HDLOAD = 0x08;	/* load head */
    public static int FDC_STEP_UPDATE = 0x10;	/* update track register */

    public static int FDC_RESTORE 	= 0x00;	/* Type I commands */
    public static int FDC_SEEK		= 0x10;
    public static int FDC_STEP		= 0x20;
    public static int FDC_STEP_IN 	= 0x40;
    public static int FDC_STEP_OUT	= 0x60;

    public static int FDC_MASK_TYPE_I 	= (FDC_STEP_HDLOAD|FDC_STEP_VERIFY|FDC_STEP_RATE);

    /* Type I commands status */
    public static int STA_1_BUSY	= 0x01;	/* controller is busy */
    public static int STA_1_IPL		= 0x02;	/* index pulse */
    public static int STA_1_TRACK0	= 0x04;	/* track 0 detected */
    public static int STA_1_CRC_ERR	= 0x08;	/* CRC error */
    public static int STA_1_SEEK_ERR	= 0x10;	/* seek error */
    public static int STA_1_HD_LOADED   = 0x20;	/* head loaded */
    public static int STA_1_WRITE_PRO   = 0x40;	/* floppy is write protected */
    public static int STA_1_NOT_READY   = 0x80;	/* controller not ready */

    /* Type II and III additional flags */
    public static int FDC_DELETED_AM	= 0x01;	/* read/write deleted address mark */
    public static int FDC_SIDE_CMP_T	= 0x02;	/* side compare track data */
    public static int FDC_15MS_DELAY	= 0x04;	/* delay 15ms before command */
    public static int FDC_SIDE_CMP_S	= 0x08;	/* side compare sector data */
    public static int FDC_MULTI_REC	= 0x10;	/* only for type II commands */

    /* Type II commands */
    public static int FDC_READ_SEC	= 0x80;	/* read sector */
    public static int FDC_WRITE_SEC	= 0xA0;	/* write sector */

    public static int FDC_MASK_TYPE_II	= (FDC_MULTI_REC|FDC_SIDE_CMP_S|FDC_15MS_DELAY|FDC_SIDE_CMP_T|FDC_DELETED_AM);

    /* Type II commands status */
    public static int STA_2_BUSY	= 0x01;
    public static int STA_2_DRQ		= 0x02;
    public static int STA_2_LOST_DAT	= 0x04;
    public static int STA_2_CRC_ERR	= 0x08;
    public static int STA_2_REC_N_FND   = 0x10;
    public static int STA_2_REC_TYPE	= 0x20;
    public static int STA_2_WRITE_PRO   = 0x40;
    public static int STA_2_NOT_READY   = 0x80;

    public static int FDC_MASK_TYPE_III	= (FDC_SIDE_CMP_S|FDC_15MS_DELAY|FDC_SIDE_CMP_T|FDC_DELETED_AM);

    /* Type III commands */
    public static int FDC_READ_DAM	= 0xc0;	/* read data address mark */
    public static int FDC_READ_TRK	= 0xe0;	/* read track */
    public static int FDC_WRITE_TRK	= 0xf0;	/* write track (format) */

    /* Type IV additional flags */
    public static int FDC_IM0 		= 0x01;	/* interrupt mode 0 */
    public static int FDC_IM1 		= 0x02;	/* interrupt mode 1 */
    public static int FDC_IM2 		= 0x04;	/* interrupt mode 2 */
    public static int FDC_IM3 		= 0x08;	/* interrupt mode 3 */

    public static int FDC_MASK_TYPE_IV	= (FDC_IM3|FDC_IM2|FDC_IM1|FDC_IM0);

    /* Type IV commands */
    public static int FDC_FORCE_INT	= 0xd0;	/* force interrupt */

    public static class WD179X {
            public timer_callback callback = null;              /* callback for IRQ status */
            public DENSITY   density=null;				/* FM/MFM, single / double density */
            public int  type;
            public int	track_reg;				/* value of track register */
            public int	data;					/* value of data register */
            public int	command;				/* last command written */
            public int	command_type;                           /* last command type */
            public int	sector; 				/* current sector # */

            public int	read_cmd;				/* last read command issued */
            public int	write_cmd;				/* last write command issued */
            public int	direction;				/* last step direction */

            public int	status; 				/* status register */
            public int	status_drq;                             /* status register data request bit */
            public int	status_ipl;                             /* status register toggle index pulse bit */
            public int	busy_count;                             /* how long to keep busy bit set */

            public char[]buffer=new char[6144];                 /* I/O buffer (holds up to a whole track) */
            public int	data_offset;                            /* offset into I/O buffer */
            public int	data_count;                             /* transfer count from/into I/O buffer */

            public int[]fmt_sector_data=new int[256];           /* pointer to data after formatting a track */

            public int[][]dam_list=new int[256][4];		/* list of data address marks while formatting */
            public int[]dam_data=new int[256];			/* offset to data inside buffer while formatting */
            public int 	dam_cnt;				/* valid number of entries in the dam_list */
            public int	sector_length;                          /* sector length (byte) */

            public int	ddam;					/* ddam of sector found - used when reading */
            public int	sector_data_id;
            public timer_entry	timer, timer_rs, timer_ws;
            public int		data_direction;
    };
    
    public static int WD_TYPE_177X	= 0;
    public static int WD_TYPE_179X	= 1;
    
}
