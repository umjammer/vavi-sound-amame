
/* DISK IMAGE FORMAT WHICH USED TO BE PART OF WD179X - NOW SEPERATED */

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
package mess056.includes;

import static common.ptr.*;

/**
 *
 * @author chusogar
 */
public class basicdskH {
    
    public static class SECMAP {
        public int	 track;
        public int	 sector;
        public int	 status;
    };
	
    public static class _basicdsk
    {
        public String image_name=""; 		/* file name for disc image */
        public Object image_file;			/* file handle for disc image */
        public int 	mode;				/* open mode == 0 read only, != 0 read/write */
        public long image_size;                     /* size of image file */

        public SECMAP	secmap = new SECMAP();

        public int	unit;				/* unit number if image_file == REAL_FDD */

        public int	tracks; 			/* maximum # of tracks */
        public int	heads;				/* maximum # of heads */

        public int	offset; 			/* track 0 offset */
        public int	first_sector_id;		/* id of first sector */
        public int	sec_per_track;			/* sectors per track */

        public int	head;				/* current head # */
        public int	track;				/* current track # */

        public int      N;
        public int	sector_length;			/* sector length (byte) */

        /* a bit for each sector in the image. If the bit is set, this sector
        has a deleted data address mark. If the bit is not set, this sector
        has a data address mark */
        public UBytePtr	ddam_map = new UBytePtr();
        public long ddam_map_size;
    };
	    
}
