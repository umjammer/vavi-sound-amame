package consoleflex056;

import common.ptr.UBytePtr;

/**
 *
 * @author shadow
 */
public class funcPtr {

    public static abstract interface StopMachinePtr {

        public abstract void handler();
    }

    //IODevice interfaces
    public static abstract interface io_idPtr {

        public abstract int handler(int id);
    }

    public static abstract interface io_initPtr {

        public abstract int handler(int id);
    }

    public static abstract interface io_exitPtr {

        public abstract int handler(int id);
    }

    public static abstract interface io_infoPtr {

        public abstract void handler(int id, int whatinfo);
    }

    public static abstract interface io_openPtr {

        public abstract int handler(int id, int mode, Object args);
    }

    public static abstract interface io_closePtr {

        public abstract void handler(int id);
    }

    public static abstract interface io_statusPtr {

        public abstract int handler(int id, int newststatus);
    }

    public static abstract interface io_seekPtr {

        public abstract int handler(int id, int offset, int whence);
    }

    public static abstract interface io_tellPtr {

        public abstract int handler(int id);
    }

    public static abstract interface io_inputPtr {

        public abstract int handler(int id);
    }

    public static abstract interface io_outputPtr {

        public abstract void handler(int id, int data);
    }

    public static abstract interface io_input_chunkPtr {

        public abstract int handler(int id, Object dst, int chunks);
    }

    public static abstract interface io_output_chunkPtr {

        public abstract void handler(int id, Object dst, int chunks);
    }

    public static abstract interface io_partialcrcPtr {

        public abstract int/*UINT32*/ handler(UBytePtr buf,/*unsigned*/ int size);
    }
}
