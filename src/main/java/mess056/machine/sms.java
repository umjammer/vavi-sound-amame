/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.machine;

import static arcadeflex056.fileio.*;
import static arcadeflex056.fucPtr.*;
import static arcadeflex056.osdepend.logerror;
import static common.ptr.*;
import static common.libc.cstdio.*;
import static common.libc.cstring.*;
import static consoleflex056.funcPtr.*;
import static mame056.common.*;
import static mame056.commonH.*;
import static mame056.inptport.*;
import static mame056.inptportH.*;
import static mame056.memory.*;
import static mame056.memoryH.*;
import static mame056.osdependH.*;
import static mess056.device.*;
import static mess056.deviceH.*;
import static mess056.mess.*;
import static mess056.messH.*;

public class sms
{
	
	public static int sms_page_count;
	public static int sms_fm_detect;
	public static int sms_version;
	
	public static WriteHandlerPtr sms_fm_detect_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                sms_fm_detect = (data & 1);
            }
        };
	
	public static ReadHandlerPtr sms_fm_detect_r = new ReadHandlerPtr() {
            public int handler(int offset) {
                return ( (readinputport(3) & 1)!=0 ? sms_fm_detect : 0x00 );
            }
        };
	
	public static WriteHandlerPtr sms_version_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                sms_version = (data & 0xA0);
            }
        };
	
	public static ReadHandlerPtr sms_version_r = new ReadHandlerPtr() {
            public int handler(int offset) {
                int temp;

                /* Move bits 7,5 of port 3F into bits 7, 6 */
                temp = (sms_version & 0x80) | (sms_version & 0x20) << 1;

                /* Inverse version detect value for Japanese machines */
                if((readinputport(3) & 2) != 0) temp ^= 0xC0;

                /* Merge version data with input port #2 data */
                temp = (temp & 0xC0) | (readinputport(1) & 0x3F);

                return (temp);
            }
        };
	
	public static WriteHandlerPtr sms_mapper_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                UBytePtr RAM = new UBytePtr(memory_region(REGION_CPU1));

                offset &= 3;
                data %= sms_page_count;

                RAM.write(0xDFFC + offset, data);
                RAM.write(0xFFFC + offset, data);

                switch(offset)
                {
                    case 0: /* Control */
                        break;

                    case 1: /* Select 16k ROM bank for 0000-3FFF */
                        memcpy(new UBytePtr(RAM, 0x0000), new UBytePtr(RAM, 0x10000 + (data * 0x4000)), 0x3C00);
                        break;

                    case 2: /* Select 16k ROM bank for 4000-7FFF */
                        memcpy(new UBytePtr(RAM, 0x4000), new UBytePtr(RAM, 0x10000 + (data * 0x4000)), 0x4000);
                        break;

                    case 3: /* Select 16k ROM bank for 8000-BFFF */
                        memcpy(new UBytePtr(RAM, 0x8000), new UBytePtr(RAM, 0x10000 + (data * 0x4000)), 0x4000);
                        break;
                }
            }
        };
	
	public static WriteHandlerPtr sms_cartram_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
            }
        };
	
	public static WriteHandlerPtr sms_ram_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                UBytePtr RAM = new UBytePtr(memory_region(REGION_CPU1));
                RAM.write(0xC000 + (offset & 0x1FFF), data);
                RAM.write(0xE000 + (offset & 0x1FFF), data);
            }
        };
	
	public static WriteHandlerPtr gg_sio_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                logerror("*** write %02X to SIO register #%d\n", data, offset);

                switch(offset & 7)
                {
                    case 0x00: /* Parallel Data */
                        break;

                    case 0x01: /* Data Direction/ NMI Enable */
                        break;

                    case 0x02: /* Serial Output */
                        break;

                    case 0x03: /* Serial Input */
                        break;

                    case 0x04: /* Serial Control / Status */
                        break;
                }
            }
        };
	
	public static ReadHandlerPtr gg_sio_r = new ReadHandlerPtr() {
            public int handler(int offset) {
                logerror("*** read SIO register #%d\n", offset);

                switch(offset & 7)
                {
                    case 0x00: /* Parallel Data */
                        break;

                    case 0x01: /* Data Direction/ NMI Enable */
                        break;

                    case 0x02: /* Serial Output */
                        break;

                    case 0x03: /* Serial Input */
                        break;

                    case 0x04: /* Serial Control / Status */
                        break;
                }

                return (0x00);
            }
        };
	
	public static WriteHandlerPtr gg_psg_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                /* D7 = Noise Left */
                /* D6 = Tone3 Left */
                /* D5 = Tone2 Left */
                /* D4 = Tone1 Left */

                /* D3 = Noise Right */
                /* D2 = Tone3 Right */
                /* D1 = Tone2 Right */
                /* D0 = Tone1 Right */
            }
        };
	
	
	/****************************************************************************/
	static int sms_verify_cart (UBytePtr magic)
	{
		//const char *sysname;
		int retval;
	
		//sysname = Machine->gamedrv->name;
		//printf ("Driver is %s\n",sysname);
	
	
		retval = IMAGE_VERIFY_FAIL;
	
		/* Verify the file is a valid image - check $7ff0 for "TMR SEGA" */
		if (strncmp(new UBytePtr(magic, 0x7ff0).memory,"TMR SEGA",8))
		{
			/* Technically, it should be this, but remove for now until verified:
			if (!strcmp(sysname, "gamegear"))
			{
				if ((unsigned char)magic[0x7ffd] < 0x50)
					retval = IMAGE_VERIFY_PASS;
			}
			if (!strcmp(sysname, "sms"))
			{
				if ((unsigned char)magic[0x7ffd] >= 0x50)
					retval = IMAGE_VERIFY_PASS;
			}
			*/
			retval = IMAGE_VERIFY_PASS;
		}
	
		/* Check at $81f0 also */
		if (retval == 0)
		{
			if (strncmp(new UBytePtr(magic, 0x81f0).memory,"TMR SEGA",8))
			{
				/* Technically, it should be this, but remove for now until verified:
				if (!strcmp(sysname, "gamegear"))
				{
					if ((unsigned char)magic[0x81fd] < 0x50)
						retval = IMAGE_VERIFY_PASS;
				}
				if (!strcmp(sysname, "sms"))
				{
					if ((unsigned char)magic[0x81fd] >= 0x50)
						retval = IMAGE_VERIFY_PASS;
				}
				*/
				retval = IMAGE_VERIFY_PASS;
			}
		}
	
		return retval;
	}
	
	public static io_initPtr sms_init_cart = new io_initPtr() {
            public int handler(int id) {
                int size, ret;
	    Object handle;
	    UBytePtr RAM;
	
	    /* Ensure filename was specified */
	    if(device_filename(IO_CARTSLOT,id) == null)
	    {
	        printf("Cartridge Name Required!\n");
	        return (INIT_FAIL);
		}
	
	    /* Ensure filename was specified */
	    handle = image_fopen(IO_CARTSLOT, id, OSD_FILETYPE_IMAGE_R, 0);
	    if(handle == null)
	    {
			printf("Cartridge Name Required!\n");
	        return (INIT_FAIL);
		}
	
	    /* Get file size */
	    size = osd_fsize(handle);
	
	    /* Check for 512-byte header */
	    if(((size / 512) & 1) != 0)
	    {
	        osd_fseek(handle, 512, SEEK_SET);
	        size -= 512;
	    }
	
	    /* Allocate memory */
	    ret = new_memory_region(REGION_CPU1, size,0);
	
	    /* Oops.. couldn't do it */
	    if(ret != 0)
	    {
	        printf("Error allocating %d bytes.\n", size);
	        return INIT_FAIL;
	    }
	
	    /* Get base of CPU1 memory region */
	    RAM = new UBytePtr(memory_region(REGION_CPU1));
	
	    /* Load ROM banks */
	    size = osd_fread(handle, new UBytePtr(RAM, 0x10000), size);
	
	    /* Close file */
	    osd_fclose(handle);
	
		/* check the image */
		if (sms_verify_cart(new UBytePtr(RAM, 0x10000))==IMAGE_VERIFY_FAIL)
		{
			logerror("Invalid Image\n");
			return INIT_FAIL;
		}
	
	    /* Get 16K page count */
	    sms_page_count = (size / 0x4000);
	
	    /* Load up first 32K of image */
	    memcpy(new UBytePtr(RAM, 0x0000), new UBytePtr(RAM, 0x10000), 0x4000);
	    memcpy(new UBytePtr(RAM, 0x4000), new UBytePtr(RAM, 0x14000), 0x4000);
	    memcpy(new UBytePtr(RAM, 0x8000), new UBytePtr(RAM, 0x10000), 0x4000);
	
	    return (INIT_PASS);
            }
        };
	
	
	public static InitMachinePtr sms_init_machine = new InitMachinePtr() {
            public void handler() {
                sms_fm_detect = 0;
            }
        };
	
	
}
