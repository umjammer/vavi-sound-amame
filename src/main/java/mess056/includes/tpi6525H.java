/***************************************************************************
    mos tri port interface 6525
	mos triple interface adapter 6523

    peter.trauner@jk.uni-linz.ac.at

	used in commodore b series
	used in commodore c1551 floppy disk drive
***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
package mess056.includes;

public class tpi6525H {
    /* tia6523 is a tpi6525 without control register!? */

    /*
     * tia6523
     *
     * only some lines of port b and c are in the pinout !
     *
     * connector to floppy c1551 (delivered with c1551 as c16 expansion)
     * port a for data read/write
     * port b
     * 0 status 0
     * 1 status 1
     * port c
     * 6 dav output edge data on port a available
     * 7 ack input edge ready for next datum
     */
    
    public static abstract interface ReadTPI6525Ptr {

        public abstract int handler();
    }
    
    public static abstract interface WriteTPI6525Ptr {

        public abstract void handler(int data);
    }
    
    public static class _blockA {
        public ReadTPI6525Ptr read = null;
        public WriteTPI6525Ptr output = null;
        public int port, ddr, in;
    }
    
    public static class _blockB {
        public WriteTPI6525Ptr output = null;
        public int level;
    }

    /* fill in the callback functions */
    public static class TPI6525 {
            public int number;
            public _blockA a=new _blockA(),b=new _blockA(),c=new _blockA();

            public _blockB ca=new _blockB(), cb=new _blockB(), interrupt=new _blockB();

            public int cr;
            public int air;

            public int[] irq_level = new int[5];
            
            public TPI6525(int _num){
                this.number = _num;
            }
    };    
}
