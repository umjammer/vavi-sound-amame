/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.includes;

import static arcadeflex056.fucPtr.*;

public class nec765H
{
	
	
	public static int NEC765_DAM_DELETED_DATA = 0x0f8;
	public static int NEC765_DAM_DATA = 0x0fb;
	
	public static class nec765_interface
	{
		/* interrupt issued */
		public ReadHandlerPtr	interrupt;
	
		/* dma data request */
		public WriteHandlerPtr dma_drq;

                public nec765_interface(ReadHandlerPtr interrupt, WriteHandlerPtr dma_drq) {
                    this.interrupt = interrupt;
                    this.dma_drq = dma_drq;
                }
	};
	
	
	/* supported versions */
	//typedef enum
	//{
	public static final int NEC765A=0;
        public static final int NEC765B=1;
	public static final int SMC37C78=2;
	//} NEC765_VERSION;
	
	
	/*********************/
	/* STATUS REGISTER 1 */
	
	/* this is set if a TC signal was not received after the sector data was read */
	public static int NEC765_ST1_END_OF_CYLINDER = (1<<7);
	/* this is set if the sector ID being searched for is not found */
	public static int NEC765_ST1_NO_DATA = (1<<2);
	/* set if disc is write protected and a write/format operation was performed */
	public static int NEC765_ST1_NOT_WRITEABLE = (1<<1);
	
	/*********************/
	/* STATUS REGISTER 2 */
	
	/* C parameter specified did not match C value read from disc */
	public static int NEC765_ST2_WRONG_CYLINDER = (1<<4);
	/* C parameter specified did not match C value read from disc, and C read from disc was 0x0ff */
	public static int NEC765_ST2_BAD_CYLINDER = (1<<1);
	/* this is set if the FDC encounters a Deleted Data Mark when executing a read data
	command, or FDC encounters a Data Mark when executing a read deleted data command */
	public static int NEC765_ST2_CONTROL_MARK = (1<<6);
}
