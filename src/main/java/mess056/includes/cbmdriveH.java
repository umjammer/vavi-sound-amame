/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
package mess056.includes;

import static common.ptr.*;
import static mess056.machine.cbmdrive.d64_tracksector2offset;

public class cbmdriveH {
    
    public static final int OPEN = 1;
    public static final int READING = 2;
    public static final int WRITING = 3;
    
    public static int D64_IMAGE = 1;
    public static int FILESYSTEM = 2;
    public static int D64_MAX_TRACKS = 35;
    public static int D64_TRACK_ID1(){ return (d64_tracksector2offset(18,0)+162); }
    public static int D64_TRACK_ID2(){ return (d64_tracksector2offset(18,0)+163); }
    
    public static class _iec
    {
            public int handshakein, handshakeout;
            public int datain, dataout;
            public int status;
            public int state;
    }
     
    public static class _serial
    {
            public int device;
            public int data, clock, atn;
            public int state; 
            public char value;
            public int forme;				   /* i am selected */
            public int last;				   /* last byte to be sent */
            public int broadcast;			   /* sent to all */
            public double time;
    }
    
    public static class _ieee 
    {
            public int device;
            public int state;
            public int data;
    }
    
    public static class _i
    {
        public _iec iec = new _iec();
        public _serial serial = new _serial();
        public _ieee ieee = new _ieee();
    }
    
    public static class _fs {
        
        /* for visualization */
        public String filename;
            
    }
    
    public static class _d64 {
    
        public UBytePtr image = null;	   /*d64 image */
        /*    int track, sector; */
        /*    int sectorbuffer[256]; */

        /* for visualization */
        public int image_type;
        public int image_id;
        public char[] filename = new char[25];

    }
            
    public static class _d {
        public _fs fs = new _fs();
        public _d64 d64 = new _d64();
    }
    
    /* data for one drive */
    public static class CBM_Drive
    {
            public int number;
            public int _interface;
            public char[] cmdbuffer = new char[32];
            public int cmdpos;
    
            public int state;						   /*0 nothing */
            public UBytePtr buffer;
            public int size;
            public int pos;
            
            public _i i = new _i();
            
            public int drive;
            
            public _d d = new _d();
            
    };

    

    public static class CBM_Serial
    {
            public int count;
            public CBM_Drive[] drives = new CBM_Drive[4];
            /* whole + computer + drives */
            public int[] /*reset, request[6], */ data=new int[6], clock=new int[6], atn=new int[6];
            
            public CBM_Serial(){
                
                for (int _i=0 ; _i<2 ; _i++){
                    drives[_i] = new CBM_Drive();
                    drives[_i].number = _i;
                }
                
            }
    };

}
