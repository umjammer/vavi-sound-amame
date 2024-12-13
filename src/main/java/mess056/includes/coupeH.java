/***************************************************************************

 SAM Coupe Driver - Written By Lee Hammerton

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
package mess056.includes;


public class coupeH {
    
    public static final int DSK1_PORT	= 224; 	/* Disk Drive 1 Port 8 ports - decode port number as (bit 0-1 address lines, bit 2 side) */
    public static final int DSK2_PORT	= 240; 	/* Disk Drive 2 Port 8 ports */

    public static final int LPEN_PORT	= 248; 	/* X location of raster (Not supported yet) */
    public static final int CLUT_PORT	= 248; 	/* Base port for CLUT (Write Only) */
    public static final int LINE_PORT	= 249; 	/* Line interrupt port (Write Only) */
    public static final int STAT_PORT	= 249; 	/* Keyboard status hi (Read Only) */
    public static final int LMPR_PORT	= 250; 	/* Low bank page register */
    public static final int HMPR_PORT	= 251; 	/* Hi bank page register */
    public static final int VMPR_PORT	= 252; 	/* Screen page register */
    public static final int KEYB_PORT	= 254; 	/* Keyboard status low (Read Only) */
    public static final int BORD_PORT	= 254; 	/* Border Port (Write Only) */
    public static final int SSND_DATA	= 255; 	/* Sound data port */
    public static final int HPEN_PORT	= 504; 	/* Y location of raster (currently == curvideo line + 10 vblank lines!) */
    public static final int SSND_ADDR	= 511; 	/* Sound address port */

    public static final int LMPR_RAM0	= 0x20;	/* If bit set ram is paged into bank 0, else its rom0 */
    public static final int LMPR_ROM1	= 0x40;	/* If bit set rom1 is paged into bank 3, else its ram */
    
}
