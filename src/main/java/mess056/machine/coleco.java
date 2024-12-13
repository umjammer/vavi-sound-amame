/**
 * Ported to mess 0.56
 */
package mess056.machine;

import static common.ptr.*;
import static mess056.mess.device_filename;
import static mess056.mess.image_fopen;
import static mess056.deviceH.IO_CARTSLOT;
import static consoleflex056.funcPtr.*;
import static mame056.inptport.*;

import static common.libc.cstring.strlen;
import static mame056.commonH.REGION_CPU1;
import static mame056.osdependH.OSD_FILETYPE_IMAGE_R;
import static mame056.osdependH.OSD_FILETYPE_ROM;
import static mess056.messH.IMAGE_VERIFY_FAIL;
import static mess056.messH.IMAGE_VERIFY_PASS;
import static mess056.messH.INIT_FAIL;
import static mess056.messH.INIT_PASS;
import static mess_spec.common.*;
import static arcadeflex056.fileio.*;
import static arcadeflex056.fucPtr.*;
import static arcadeflex056.osdepend.logerror;

public class coleco {

    static int JoyMode = 0;

    static int coleco_verify_cart(UBytePtr cartdata) {
        int retval = IMAGE_VERIFY_FAIL;

        /* Verify the file is in Colecovision format */
        if ((cartdata.read(0) == 0xAA) && (cartdata.read(1) == 0x55)) {
            retval = IMAGE_VERIFY_PASS;
        }
        if ((cartdata.read(0) == 0x55) && (cartdata.read(1) == 0xAA)) {
            retval = IMAGE_VERIFY_PASS;
        }

        return retval;
    }
    public static io_initPtr coleco_init_cart = new io_initPtr() {
        public int handler(int id) {
            Object cartfile = null;
            UBytePtr cartdata = new UBytePtr();
            int init_result = INIT_FAIL;

            /* A cartridge isn't strictly mandatory for the coleco */
            if (device_filename(IO_CARTSLOT, id) == null || strlen(device_filename(IO_CARTSLOT, id)) == 0) {
                logerror("Coleco - warning: no cartridge specified!\n");
                return INIT_PASS;
            }
            /* Load the specified Cartridge File */
            if ((cartfile = image_fopen(IO_CARTSLOT, id, OSD_FILETYPE_IMAGE_R, 0)) == null) {
                logerror("Coleco - Unable to locate cartridge: %s\n", device_filename(IO_CARTSLOT, id));
                return INIT_FAIL;
            }
            
            System.out.println(cartfile);

            /* All seems OK */
            cartdata = new UBytePtr(memory_region(REGION_CPU1), 0x8000);
            osd_fread(cartfile, cartdata, 0x8000);

            /* Verify the cartridge image */
            if (coleco_verify_cart(cartdata) == IMAGE_VERIFY_FAIL) {
                logerror("Coleco - Image verify FAIL\n");
                init_result = INIT_FAIL;
            } else {
                logerror("Coleco - Image verify PASS\n");
                init_result = INIT_PASS;
            }
            osd_fclose(cartfile);
            return init_result;
        }
    };
    public static ReadHandlerPtr coleco_paddle_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            /* Player 1 */
            if ((offset & 0x02) == 0) {
                /* Keypad and fire 1 */
                if (JoyMode == 0) {
                    int inport0, inport1, data;

                    inport0 = input_port_0_r.handler(0);
                    inport1 = input_port_1_r.handler(0);

                    if ((inport0 & 0x01) == 0) /* 0 */ {
                        data = 0x0A;
                    } else if ((inport0 & 0x02) == 0) /* 1 */ {
                        data = 0x0D;
                    } else if ((inport0 & 0x04) == 0) /* 2 */ {
                        data = 0x07;
                    } else if ((inport0 & 0x08) == 0) /* 3 */ {
                        data = 0x0C;
                    } else if ((inport0 & 0x10) == 0) /* 4 */ {
                        data = 0x02;
                    } else if ((inport0 & 0x20) == 0) /* 5 */ {
                        data = 0x03;
                    } else if ((inport0 & 0x40) == 0) /* 6 */ {
                        data = 0x0E;
                    } else if ((inport0 & 0x80) == 0) /* 7 */ {
                        data = 0x05;
                    } else if ((inport1 & 0x01) == 0) /* 8 */ {
                        data = 0x01;
                    } else if ((inport1 & 0x02) == 0) /* 9 */ {
                        data = 0x0B;
                    } else if ((inport1 & 0x04) == 0) /* # */ {
                        data = 0x06;
                    } else if ((inport1 & 0x08) == 0) /* . */ {
                        data = 0x09;
                    } else {
                        data = 0x0F;
                    }

                    return (inport1 & 0x70) | (data);

                } /* Joystick and fire 2*/ else {
                    return input_port_2_r.handler(0);
                }
            } /* Player 2 */ else {
                /* Keypad and fire 1 */
                if (JoyMode == 0) {
                    int inport3, inport4, data;

                    inport3 = input_port_3_r.handler(0);
                    inport4 = input_port_4_r.handler(0);

                    if ((inport3 & 0x01) == 0) /* 0 */ {
                        data = 0x0A;
                    } else if ((inport3 & 0x02) == 0) /* 1 */ {
                        data = 0x0D;
                    } else if ((inport3 & 0x04) == 0) /* 2 */ {
                        data = 0x07;
                    } else if ((inport3 & 0x08) == 0) /* 3 */ {
                        data = 0x0C;
                    } else if ((inport3 & 0x10) == 0) /* 4 */ {
                        data = 0x02;
                    } else if ((inport3 & 0x20) == 0) /* 5 */ {
                        data = 0x03;
                    } else if ((inport3 & 0x40) == 0) /* 6 */ {
                        data = 0x0E;
                    } else if ((inport3 & 0x80) == 0) /* 7 */ {
                        data = 0x05;
                    } else if ((inport4 & 0x01) == 0) /* 8 */ {
                        data = 0x01;
                    } else if ((inport4 & 0x02) == 0) /* 9 */ {
                        data = 0x0B;
                    } else if ((inport4 & 0x04) == 0) /* # */ {
                        data = 0x06;
                    } else if ((inport4 & 0x08) == 0) /* . */ {
                        data = 0x09;
                    } else {
                        data = 0x0F;
                    }

                    return (inport4 & 0x70) | (data);

                } /* Joystick and fire 2*/ else {
                    return input_port_5_r.handler(0);
                }
            }

        }
    };
    public static WriteHandlerPtr coleco_paddle_toggle_off = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            JoyMode = 0;
        }
    };
    public static WriteHandlerPtr coleco_paddle_toggle_on = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            JoyMode = 1;
        }
    };
}
