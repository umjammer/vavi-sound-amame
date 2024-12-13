/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
package mess056.machine;

import static arcadeflex056.fucPtr.*;

public class nes_mmcH {

    public static class _mmc
    {
            public int iNesMapper; /* iNES Mapper # */
            public String desc;     /* Mapper description */
            public WriteHandlerPtr mmc_write_low; /* $4100-$5fff write routine */
            public ReadHandlerPtr mmc_read_low; /* $4100-$5fff read routine */
            public WriteHandlerPtr mmc_write_mid; /* $6000-$7fff write routine */
            public WriteHandlerPtr mmc_write; /* $8000-$ffff write routine */
            public ReadHandlerPtr ppu_latch;
            public ReadHandlerPtr mmc_irq;
            
            public _mmc(int iNesMapper, String desc, WriteHandlerPtr mmc_write_low, ReadHandlerPtr mmc_read_low, WriteHandlerPtr mmc_write_mid, WriteHandlerPtr mmc_write, ReadHandlerPtr ppu_latch, ReadHandlerPtr mmc_irq){
                this.iNesMapper = iNesMapper;
                this.desc = desc;
                this.mmc_write_low = mmc_write_low;
                this.mmc_read_low = mmc_read_low;
                this.mmc_write_mid = mmc_write_mid;
                this.mmc_write = mmc_write;
                this.ppu_latch = ppu_latch;
                this.mmc_irq = mmc_irq;
            }
    };
    
    public static _mmc mmc;

    public static int MMC5_VRAM;

}
