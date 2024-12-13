/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.machine;

import static common.libc.cstdio.*;
import static common.libc.cstring.*;
import static common.ptr.*;
import static arcadeflex056.fileio.*;
import static arcadeflex056.osdepend.logerror;
import static consoleflex056.funcPtr.*;
import static mame056.mame.*;
import static mame056.osdependH.*;
import static mess056.deviceH.*;
import static mess056.includes.cbmH.*;
import static mess056.machine.c64.quickload;
import static mess056.mess.*;
import static mess056.messH.*;

public class cbm
{
/*TODO*///	/* safer replacement str[0]=0; */
/*TODO*///	int DECL_SPEC cbm_snprintf (char *str, size_t size, const char *format,...)
/*TODO*///	{
/*TODO*///		va_list list;
/*TODO*///	
/*TODO*///		va_start (list, format);
/*TODO*///	
/*TODO*///		return vsprintf (str, format, list);
/*TODO*///	}
/*TODO*///	
/*TODO*///	void *cbm_memset16 (void *dest, int value, size_t size)
/*TODO*///	{
/*TODO*///		register int i;
/*TODO*///	
/*TODO*///		for (i = 0; i < size; i++)
/*TODO*///			((short *) dest)[i] = value;
/*TODO*///		return dest;
/*TODO*///	}
	
	public static class _quick
	{
		public int specified;
		public int addr;
		public UBytePtr data=new UBytePtr();
		public int length;
	};
        
        public static _quick quick = new _quick();
        
        public static String readC64Filename(final char[] buffer, final int offset, final int length) {
            final StringBuffer filename = new StringBuffer();
            
            int _longo = offset + length;

            for (int i = offset; i < _longo; ++i) {
                if ((byte)(buffer[i]&0xff) == (byte) 0xa0) {
                    filename.append(' ');
                } else {
                    filename.append((char) buffer[i]);
                }
            }

            return filename.toString();
        }

        
        public static io_initPtr cbm_quick_init = new io_initPtr() {
            public int handler(int id) {
                Object fp;
		int read;
		String cp;
	
		//memset (&quick, 0, sizeof (quick));
                quick = new _quick();
	
		if (device_filename(IO_QUICKLOAD, id) == null)
			return INIT_PASS;
	
		quick.specified = 1;
	
		fp = image_fopen (IO_QUICKLOAD, id, OSD_FILETYPE_IMAGE_R, 0);
		if (fp == null)
			return INIT_FAIL;
	
		quick.length = osd_fsize (fp);
	
		if ((cp = strrchr (device_filename(IO_QUICKLOAD, id), '.')) != null)
		{
                    System.out.println("CP="+cp);
			if (device_filename(IO_QUICKLOAD, id).toLowerCase().endsWith(".prg") )
			{
                            System.out.println("Dentro!");
                                char[] _b = new char[2];
				osd_fread_lsbfirst (fp, _b, 2);
                                //quick.addr = (_b[1]&0xff)<<8|_b[0];
                                //quick.addr = (_b[ 0] & 0xff) + (_b[ 1] & 0xff) * 256;;
                                quick.addr = 0x801;
                                System.out.println("Address="+quick.addr); // other option: 0x801 and load"*",8,1
				quick.length -= 2;
			}
			else if (device_filename(IO_QUICKLOAD, id).toLowerCase().endsWith(".p00"))
			{
				char[] buffer=new char[7];
	
				osd_fread (fp, buffer, buffer.length);
				if (!strncmp (buffer, "C64File", buffer.length))
				{
					osd_fseek (fp, 26, SEEK_SET);
					byte[] _b = new byte[2];
                                        osd_fread_lsbfirst (fp, _b, 2);
                                        //quick.addr = (_b[1]&0xff)<<8|_b[0];
                                        quick.addr = 0x801;
					quick.length -= 28;
				}
			}
                        else if (device_filename(IO_QUICKLOAD, id).toLowerCase().endsWith(".t64"))
			{
				
                                        osd_fseek (fp, 0x60, SEEK_SET);
					//byte[] _b = new byte[2];
                                        //osd_fread_lsbfirst (fp, _b, 2);
                                        quick.addr = 0x801;
					quick.length -= 0x60;
                                
				
			}
		}
		if (quick.addr == 0)
		{
			osd_fclose (fp);
			return INIT_FAIL;
		}
		if ((quick.data = new UBytePtr(quick.length)) == null)
		{
			osd_fclose (fp);
			return INIT_FAIL;
		}
                
		read = osd_fread (fp, quick.data, quick.length);
		osd_fclose (fp);
                System.out.println("Retorno "+((read != quick.length)?1:0));
                quickload=1;
		return (read != quick.length)?1:0;
            }
        };
	
	public static io_initPtr cbm_quick_init2 = new io_initPtr() {
            public int handler(int id) {
                System.out.println("cbm_quick_init");
                Object fp;
		int read;
		String cp="";
	
		//quick = new _quick();
	
		if (device_filename(IO_QUICKLOAD, id) == null)
			return INIT_PASS;
	
		quick.specified = 1;
	
		fp = image_fopen (IO_QUICKLOAD, id, OSD_FILETYPE_IMAGE_R, 0);
                System.out.println(fp);
		if (fp == null)
			return INIT_FAIL;
	
		quick.length = osd_fsize (fp);
                System.out.println("Size: "+quick.length);
	
		if ((cp = strrchr (device_filename(IO_QUICKLOAD, id), '.')) != null)
		{
                        System.out.println("Check! "+cp);
			if (device_filename(IO_QUICKLOAD, id).toLowerCase().endsWith(".prg"))
			{
                            System.out.println("PRG!");
                                char[] _b = new char[2];
				osd_fread_lsbfirst (fp, _b, 2);
                                quick.addr = (_b[1]&0xff)<<8|_b[0];
                                System.out.println("ADDRESSL: "+quick.addr);
                                System.out.println("ADDRESS1: "+((((int)_b[0])<<8)+(int)_b[1]));
                                System.out.println("ADDRESS2: "+((((int)_b[1])<<8)+(int)_b[0]));
                                //quick.addr = ((int)_c[0]&0xff);
				quick.length -= 2;
			}
			else if (device_filename(IO_QUICKLOAD, id).toLowerCase().endsWith(".p00"))
			{
                            System.out.println("P00!");
				char[] buffer=new char[7];
                                char[] _b = new char[2];
	
				osd_fread (fp, buffer, buffer.length);
				if (strncmp (buffer, "C64File", buffer.length) == false)
				{
					osd_fseek (fp, 26, SEEK_SET);
					osd_fread_lsbfirst (fp, _b, 2);
                                        quick.addr = (_b[1]&0xff)<<8|_b[0];
                                        System.out.println("ADDRESSL: "+quick.addr);
					quick.length -= 28;
				}
			}
		}
		if (quick.addr == 0)
		{
			osd_fclose (fp);
                        System.out.println("LOAD FAILS!");
			return INIT_FAIL;
		}
		if ((quick.data = new UBytePtr(quick.length)) == null)
		{
			osd_fclose (fp);
                        System.out.println("LOAD FAILS 2!");
			return INIT_FAIL;
		}
                UBytePtr b = quick.data;
		read = osd_fread (fp, b, quick.length);
                quick.data = new UBytePtr(b);
		osd_fclose (fp);
                //System.out.println("LOAD RESULT: "+(read != quick.length ? 1 : 0));
		//return read != quick.length ? 1 : 0;
                quickload = 1;
                return INIT_PASS;
            }
        };

	public static io_exitPtr cbm_quick_exit = new io_exitPtr() {
            public int handler(int id) {
                if (quick.data != null)
			quick.data = null;
                
                return 1;
            }
        };
	
	
	public static io_openPtr cbm_quick_open = new io_openPtr() {
            public int handler(int id, int mode, Object args) {
                System.out.println("cbm_quick_open");
                
                int addr;
		UBytePtr memory = new UBytePtr((UBytePtr)args);
	
		if (quick.data == null)
			return 1;
		addr = quick.addr + quick.length;
	
		memcpy (new UBytePtr(memory, quick.addr), new UBytePtr(quick.data), quick.length);
		memory.write(0x31, addr & 0xff);
                memory.write(0x2f, addr & 0xff);
                memory.write(0x2d, addr & 0xff);
		memory.write(0x32, addr >> 8);
                memory.write(0x30, addr >> 8);
                memory.write(0x2e, addr >> 8);
		logerror("quick loading %s at %.4x size:%.4x\n",
		device_filename(IO_QUICKLOAD,id), quick.addr, quick.length);
	
		return 0;
            }
        };

/*TODO*///	int cbm_pet_quick_open (int id, int mode, void *arg)
/*TODO*///	{
/*TODO*///		int addr;
/*TODO*///		UINT8 *memory = (UINT8*)arg;
/*TODO*///	
/*TODO*///		if (quick.data == null)
/*TODO*///			return 1;
/*TODO*///		addr = quick.addr + quick.length;
/*TODO*///	
/*TODO*///		memcpy (memory + quick.addr, quick.data, quick.length);
/*TODO*///		memory[0x2e] = memory[0x2c] = memory[0x2a] = addr & 0xff;
/*TODO*///		memory[0x2f] = memory[0x2d] = memory[0x2b] = addr >> 8;
/*TODO*///		logerror("quick loading %s at %.4x size:%.4x\n",
/*TODO*///					 device_filename(IO_QUICKLOAD,id), quick.addr, quick.length);
/*TODO*///	
/*TODO*///		return 0;
/*TODO*///	}
/*TODO*///	
/*TODO*///	int cbm_pet1_quick_open (int id, int mode, void *arg)
/*TODO*///	{
/*TODO*///		int addr;
/*TODO*///		UINT8 *memory = (UINT8*)arg;
/*TODO*///	
/*TODO*///		if (quick.data == null)
/*TODO*///			return 1;
/*TODO*///		addr = quick.addr + quick.length;
/*TODO*///	
/*TODO*///		memcpy (memory + quick.addr, quick.data, quick.length);
/*TODO*///		memory[0x80] = memory[0x7e] = memory[0x7c] = addr & 0xff;
/*TODO*///		memory[0x81] = memory[0x7f] = memory[0x7d] = addr >> 8;
/*TODO*///		logerror("quick loading %s at %.4x size:%.4x\n",
/*TODO*///					 device_filename(IO_QUICKLOAD,id), quick.addr, quick.length);
/*TODO*///	
/*TODO*///		return 0;
/*TODO*///	}
/*TODO*///	
/*TODO*///	int cbmb_quick_open (int id, int mode, void *arg)
/*TODO*///	{
/*TODO*///		int addr;
/*TODO*///		UINT8 *memory = (UINT8*)arg;
/*TODO*///	
/*TODO*///		if (quick.data == null)
/*TODO*///			return 1;
/*TODO*///		addr = quick.addr + quick.length;
/*TODO*///	
/*TODO*///		memcpy (memory + quick.addr+0x10000, quick.data, quick.length);
/*TODO*///		memory[0xf0046] = addr & 0xff;
/*TODO*///		memory[0xf0047] = addr >> 8;
/*TODO*///		logerror("quick loading %s at %.4x size:%.4x\n",
/*TODO*///					 device_filename(IO_QUICKLOAD,id), quick.addr, quick.length);
/*TODO*///	
/*TODO*///		return 0;
/*TODO*///	}
/*TODO*///	
/*TODO*///	int cbm500_quick_open (int id, int mode, void *arg)
/*TODO*///	{
/*TODO*///		int addr;
/*TODO*///		UINT8 *memory = (UINT8*)arg;
/*TODO*///	
/*TODO*///		if (quick.data == null)
/*TODO*///			return 1;
/*TODO*///		addr = quick.addr + quick.length;
/*TODO*///	
/*TODO*///		memcpy (memory + quick.addr, quick.data, quick.length);
/*TODO*///		memory[0xf0046] = addr & 0xff;
/*TODO*///		memory[0xf0047] = addr >> 8;
/*TODO*///		logerror("quick loading %s at %.4x size:%.4x\n",
/*TODO*///					 device_filename(IO_QUICKLOAD,id), quick.addr, quick.length);
/*TODO*///	
/*TODO*///		return 0;
/*TODO*///	}
/*TODO*///	
/*TODO*///	int cbm_c65_quick_open (int id, int mode, void *arg)
/*TODO*///	{
/*TODO*///		int addr;
/*TODO*///		UINT8 *memory = (UINT8*)arg;
/*TODO*///	
/*TODO*///		if (quick.data == null)
/*TODO*///			return 1;
/*TODO*///		addr = quick.addr + quick.length;
/*TODO*///	
/*TODO*///		memcpy (memory + quick.addr, quick.data, quick.length);
/*TODO*///		memory[0x82] = addr & 0xff;
/*TODO*///		memory[0x83] = addr >> 8;
/*TODO*///	
/*TODO*///		logerror("quick loading %s at %.4x size:%.4x\n",
/*TODO*///					 device_filename(IO_QUICKLOAD,id), quick.addr, quick.length);
/*TODO*///	
/*TODO*///		return 0;
/*TODO*///	}
	
	public static int cbm_c64_game;
	public static int cbm_c64_exrom;
        
	public static _CBM_ROM[] cbm_rom = new _CBM_ROM[0x20];
        
        static {
            for (int _i=0 ; _i<0x20 ; _i++)
                cbm_rom[_i] = new _CBM_ROM();
        }

	public static io_exitPtr cbm_rom_exit = new io_exitPtr() {
            public int handler(int id) {
                int i;
                if (id!=0) return 0;
                for (i=0;(i<0x20)&&(cbm_rom[i]!=null);i++) {
                    cbm_rom[i].chip=null;cbm_rom[i].size=0;
                }
                
                return 0;
            }
        };

	public static IODevice cbm_rom_find_device()
	{
		int i;
		for (i=0; (Machine.gamedrv.dev[i].count != 0)
				 &&(Machine.gamedrv.dev[i].type!=IO_CARTSLOT);
			 i++) ;
		return Machine.gamedrv.dev[i].count!=0?Machine.gamedrv.dev[i]:null;
	}
	
        public static io_initPtr cbm_rom_init = new io_initPtr() {
            public int handler(int id) {
                System.out.println("cbm_rom_init");
		Object fp;
		int i=0;
		int size, j, read;
		String cp="";
		int adr = 0;
		IODevice dev;
	
		if (id==0) {
		    cbm_c64_game=-1;
		    cbm_c64_exrom=-1;
		}
	
		if (device_filename(IO_CARTSLOT,id) == null)
			return INIT_PASS;
	
/*TODO*///		for (i=0;(i<0x20)&&(cbm_rom[i]!=null);i++)
/*TODO*///			;
/*TODO*///		if (i>=0x20) return INIT_FAIL;
	
		dev=cbm_rom_find_device();
	
		fp = image_fopen (IO_CARTSLOT, id, OSD_FILETYPE_IMAGE_R, 0);
		if (fp == null)
		{
			logerror("%s file not found\n", device_filename(IO_CARTSLOT,id));
			return INIT_FAIL;
		}
	
		size = osd_fsize (fp);
	
		if ((cp = strrchr (device_filename(IO_CARTSLOT,id), '.')) != null)
		{
                    System.out.println("Dentro");
			if (device_filename(IO_CARTSLOT, id).toLowerCase().endsWith(".prg"))
			{
				char[] _in=new char[2];
	
				osd_fread_lsbfirst (fp, _in, 2);
				logerror("rom prg %.4x\n", _in);
				size -= 2;
				logerror("loading rom %s at %.4x size:%.4x\n",
							 device_filename(IO_CARTSLOT,id), _in, size);
				if ((cbm_rom[i].chip=new UBytePtr(size)) == null) {
					osd_fclose(fp);
					return INIT_FAIL;
				}
				//cbm_rom[i].addr=in[0];
                                cbm_rom[i].addr=((((int)_in[1]&0xff)<<8)+(int)_in[0]);
				cbm_rom[i].size=size;
				read = osd_fread (fp, cbm_rom[i].chip, size);
				osd_fclose (fp);
				if (read != size)
					return INIT_FAIL;
			}
			else if (stricmp (cp, ".crt") == 0)
			{
				char[] in=new char[1];
				osd_fseek (fp, 0x18, SEEK_SET);
				osd_fread( fp, new char[]{(char)cbm_c64_exrom}, 1);
				osd_fread( fp, new char[]{(char)cbm_c64_game}, 1);
				osd_fseek (fp, 64, SEEK_SET);
				j = 64;
				logerror("loading rom %s size:%.4x\n",
							 device_filename(IO_CARTSLOT,id), size);
				while (j < size)
				{
					char[] segsize = new char[1];
					UBytePtr buffer=new UBytePtr(10);
                                        char[] number=new char[1];
	
					osd_fread (fp, buffer, 6);
					osd_fread(fp, segsize, 2);
					osd_fread (fp, new UBytePtr(buffer, 6), 3);
					osd_fread (fp, number, 1);
					osd_fread(fp, new char[]{(char)adr}, 2);
					osd_fread(fp, in, 2);
					logerror("%.4s %.2x %.2x %.4x %.2x %.2x %.2x %.2x %.4x:%.4x\n",
								 buffer, buffer.read(4), buffer.read(5), segsize,
								 buffer.read(6), buffer.read(7), buffer.read(8), number,
								 adr, in);
					logerror("loading chip at %.4x size:%.4x\n", adr, in);
	
	
					if ((cbm_rom[i].chip=new UBytePtr(size)) == null) {
						osd_fclose(fp);
						return INIT_FAIL;
					}
					cbm_rom[i].addr=adr;
					cbm_rom[i].size=in[0];
					read = osd_fread (fp, new UBytePtr(cbm_rom[i].chip), in[0]);
					i++;
					if (read != in[0])
					{
						osd_fclose (fp);
						return INIT_FAIL;
					}
					j += 16 + in[0];
				}
				osd_fclose (fp);
			}
			else
			{
				if (stricmp (cp, ".lo") == 0)
					adr = CBM_ROM_ADDR_LO;
				else if (stricmp (cp, ".hi") == 0)
					adr = CBM_ROM_ADDR_HI;
				else if (stricmp (cp, ".10") == 0)
					adr = 0x1000;
				else if (stricmp (cp, ".20") == 0)
					adr = 0x2000;
				else if (stricmp (cp, ".30") == 0)
					adr = 0x3000;
				else if (stricmp (cp, ".40") == 0)
					adr = 0x4000;
				else if (stricmp (cp, ".50") == 0)
					adr = 0x5000;
				else if (stricmp (cp, ".60") == 0)
					adr = 0x6000;
				else if (stricmp (cp, ".70") == 0)
					adr = 0x7000;
				else if (stricmp (cp, ".80") == 0)
					adr = 0x8000;
				else if (stricmp (cp, ".90") == 0)
					adr = 0x9000;
				else if (stricmp (cp, ".a0") == 0)
					adr = 0xa000;
				else if (stricmp (cp, ".b0") == 0)
					adr = 0xb000;
				else if (stricmp (cp, ".e0") == 0)
					adr = 0xe000;
				else if (stricmp (cp, ".f0") == 0)
					adr = 0xf000;
				else adr = CBM_ROM_ADDR_UNKNOWN;
				//logerror("loading %s rom at %.4x size:%.4x\n", device_filename(IO_CARTSLOT,id), adr, size);
				if ((cbm_rom[i].chip=new UBytePtr(size)) == null) {
					osd_fclose(fp);
					return INIT_FAIL;
				}
				cbm_rom[i].addr=adr;
				cbm_rom[i].size=size;
				read = osd_fread (fp, cbm_rom[i].chip, size);
	
				osd_fclose (fp);
				if (read != size)
					return INIT_FAIL;
			}
		}
                System.out.println("SALIMOS!!!!");
		return INIT_OK;
	}
            };
	
}


