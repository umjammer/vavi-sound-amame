/***************************************************************************

	machine/nec765.c

	Functions to emulate a NEC765/Intel 8272 compatible floppy disk controller

	Code by Kevin Thacker.

	TODO:

    - overrun condition
	- Scan Commands
	- crc error in id field and crc error in data field errors
	- disc not present, and no sectors on track for data, deleted data, write, write deleted,
		read a track etc
        - end of cylinder condition - almost working, needs fixing  with
                PCW and PC drivers
	- resolve "ready" state stuff (ready state when reset for PC, ready state change while processing command AND
	while idle)
***************************************************************************/
/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.machine;

import static arcadeflex056.fucPtr.*;
import static arcadeflex056.osdepend.logerror;
import static common.ptr.*;
import static java.lang.Math.abs;
import static mame056.timer.*;
import static mame056.timerH.*;
import static mess056.includes.flopdrvH.*;
import static mess056.includes.nec765H.*;
import static mess056.machine.flopdrv.*;

public class nec765
{
	
	//typedef enum
	//{
	public static final int NEC765_COMMAND_PHASE_FIRST_BYTE = 0;
	public static final int NEC765_COMMAND_PHASE_BYTES = 1;
	public static final int NEC765_RESULT_PHASE = 2;
	public static final int NEC765_EXECUTION_PHASE_READ = 3;
	public static final int NEC765_EXECUTION_PHASE_WRITE = 4;
	//} NEC765_PHASE;
	
	/* uncomment the following line for verbose information */
	//#define VERBOSE
	
	/* uncomment this to not allow end of cylinder "error" */
	public static boolean NO_END_OF_CYLINDER = true;
	
	//#ifdef VERBOSE
	/* uncomment the following line for super-verbose information i.e. data
	transfer bytes */
	//#define SUPER_VERBOSE
	//#endif
	
	
	
	/* state of nec765 Interrupt (INT) output */
	public static int NEC765_INT	= 0x02;
	/* data rate for floppy discs (MFM data) */
	public static int NEC765_DATA_RATE	= 32;
	/* state of nec765 terminal count input*/
	public static int NEC765_TC	= 0x04;
	
	public static int NEC765_DMA_MODE = 0x08;
	
	public static int NEC765_SEEK_OPERATION_IS_RECALIBRATE = 0x01;
	
	public static int NEC765_SEEK_ACTIVE = 0x010;
	/* state of nec765 DMA DRQ output */
	public static int NEC765_DMA_DRQ = 0x020;
	/* state of nec765 FDD READY input */
	public static int NEC765_FDD_READY = 0x040;
	
	public static int NEC765_RESET = 0x080;
	
	public static class NEC765
	{
		public int	sector_counter;
		/* version of fdc to emulate */
		public int version;
		/* main status register */
		public int    FDC_main;
		/* data register */
		public int	nec765_data_reg;
	
		public int c,h,r,n;
	
		int sector_id;
	
		int data_type;
	
		public char[] format_data = new char[4];
	
		public int    nec765_phase;
		public int[]    nec765_command_bytes = new int[16];
		public int[]    nec765_result_bytes = new int[16];
		public int    nec765_transfer_bytes_remaining;
		public int    nec765_transfer_bytes_count;
		public int[]    nec765_status = new int[4];
		/* present cylinder number per drive */
		public int[]    pcn = new int[4];
		
		/* drive being accessed. drive outputs from fdc */
		public int    drive;
		/* side being accessed: side output from fdc */
		public int	side;
	
		
		/* step rate time in us */
		public int	srt_in_ms;
	
		public int	ncn;
	
	//	unsigned int    nec765_id_index;
		public char[] execution_phase_data;
		public int	nec765_flags;
	
	//	unsigned char specify[2];
	//	unsigned char perpendicular_mode[1];
	
		public int command;
	
		public timer_entry seek_timer;
		public timer_entry timer;
		public int timer_type;
	};
        
	static NEC765 fdc = new NEC765();
	static char[] nec765_data_buffer = new char[32*1024];
	
	
	public static nec765_interface nec765_iface;
	
	
	static int nec765_cmd_size[] = {
		1,1,9,3,2,9,9,2,1,9,2,1,9,6,1,3,
		1,9,1,1,1,1,9,1,1,9,1,1,1,9,1,1
	};
	
	static void nec765_setup_drive_and_side()
	{
		// drive index nec765 sees
		fdc.drive = fdc.nec765_command_bytes[1] & 0x03;
		// side index nec765 sees
		fdc.side = (fdc.nec765_command_bytes[1]>>2) & 0x01;
	}
	
	
	/* setup status register 0 based on data in status register 1 and 2 */
	static void nec765_setup_st0()
	{
		/* clear completition status bits, drive bits and side bits */
		fdc.nec765_status[0] &= ~((1<<7) | (1<<6) | (1<<2) | (1<<1) | (1<<0));
		/* fill in drive */
		fdc.nec765_status[0] |= fdc.drive | (fdc.side<<2);
	
		/* fill in completion status bits based on bits in st0, st1, st2 */
		/* no error bits set */
		if ((fdc.nec765_status[1] | fdc.nec765_status[2])==0)
		{
			return;
		}
	
		fdc.nec765_status[0] |= 0x040;
	}
	
	
	static int nec765_n_to_bytes(int n)
	{
		/* 0. 128 bytes, 1.256 bytes, 2.512 bytes etc */
		/* data_size = ((1<<(N+7)) */
		return 1<<(n+7);
	}
	
	static void nec765_set_data_request()
	{
		fdc.FDC_main |= 0x080;
	}
	
	static void nec765_clear_data_request()
	{
		fdc.FDC_main &= ~0x080;
	}
	
	static void nec765_seek_complete()
	{
			/* tested on Amstrad CPC */
	
			/* if a seek is done without drive connected: */
			/*  abnormal termination of command,
				seek complete, 
				not ready
			*/
	
			/* if a seek is done with drive connected, but disc missing: */
			/* abnormal termination of command,
				seek complete,
				not ready */
	
			/* if a seek is done with drive connected and disc in drive */
			/* seek complete */
	
	
			/* On the PC however, it appears that recalibrates and seeks can be performed without
			a disc in the drive. */
	
			/* Therefore, the above output is dependant on the state of the drive */
	
			/* In the Amstrad CPC, the drive select is provided by the NEC765. A single port is also
			assigned for setting the drive motor state. The motor state controls the motor of the selected
			drive */
	
			/* On the PC the drive can be selected with the DIGITAL OUTPUT REGISTER, and the motor of each
			of the 4 possible drives is also settable using the same register */
		
			/* Assumption for PC: (NOT TESTED - NEEDS VERIFICATION) */
	
			/* If a seek is done without drive connected: */
			/* abnormal termination of command,
				seek complete,
				fault
				*/
	
			/* if a seek is done with drive connected, but disc missing: */
			/* seek complete */
			
			/* if a seek is done with drive connected and disc in drive: */
			/* seek complete */
	
		/* On Amstrad CPC:
			If drive not connected, or drive connected but disc not in drive, not ready! 
			If drive connected and drive motor on, ready!
		   On PC:
		    Drive is always ready!
	
		In 37c78 docs, the ready bits of the nec765 are marked as unused.
		This indicates it is always ready!!!!!
		*/
	
		fdc.pcn[fdc.drive] = fdc.ncn;
	
		/* drive ready? */
		if (floppy_drive_get_flag_state(fdc.drive, FLOPPY_DRIVE_READY) != 0)
		{
			/* yes */
	
			/* recalibrate? */
			if ((fdc.nec765_flags & NEC765_SEEK_OPERATION_IS_RECALIBRATE) != 0)
			{
				/* yes */
	
				/* at track 0? */
				if (floppy_drive_get_flag_state(fdc.drive, FLOPPY_DRIVE_HEAD_AT_TRACK_0) != 0)
				{
					/* yes. Seek complete */
					fdc.nec765_status[0] = 0x020;
				}
				else
				{
					/* no, track 0 failed after 77 steps */
					fdc.nec765_status[0] = 0x040 | 0x020 | 0x010;
				}
			}
			else
			{
				/* no, seek */
	
				/* seek complete */
				fdc.nec765_status[0] = 0x020;
			}
		}
		else
		{
			/* abnormal termination, not ready */
			fdc.nec765_status[0] = 0x040 | 0x020 | 0x08;		
		}
	
		/* set drive and side */
		fdc.nec765_status[0] |= fdc.drive | (fdc.side<<2);
	
		nec765_set_int(1);
	
		fdc.nec765_flags &= ~NEC765_SEEK_ACTIVE;
	}
	
	static timer_callback nec765_seek_timer_callback = new timer_callback() {
            public void handler(int param) {
                /* seek complete */
			nec765_seek_complete();
	
			if (fdc.seek_timer != null)
			{
				timer_reset(fdc.seek_timer, TIME_NEVER);
			}
            }
        };
	
	static timer_callback nec765_timer_callback = new timer_callback() {
            public void handler(int param) {
                /* type 0 = data transfer mode in execution phase */
		if (fdc.timer_type==0)
		{
			/* set data request */
			nec765_set_data_request();
	
			fdc.timer_type = 4;
			
			if ((fdc.nec765_flags & NEC765_DMA_MODE) == 0)
			{
				if (fdc.timer != null)
				{
					// for pcw
					timer_reset(fdc.timer, TIME_IN_USEC(27));
				}
			}
			else
			{
				nec765_timer_callback.handler(fdc.timer_type);
			}
		}
		else
		if (fdc.timer_type==2)
		{
			/* result phase begin */
	
			/* generate a int for specific commands */
			switch (fdc.command)
			{
				/* read a track */
				case 2:
				/* write data */
				case 5:
				/* read data */
				case 6:
				/* write deleted data */
				case 9:
				/* read id */
				case 10:
				/* read deleted data */
				case 12:
				/* format at track */
				case 13:
				/* scan equal */
				case 17:
				/* scan low or equal */
				case 19:
				/* scan high or equal */
				case 29:
				{
					nec765_set_int(1);
				}
				break;
	
				default:
					break;
			}
	
			nec765_set_data_request();
	
			if (fdc.timer != null)
			{
				timer_reset(fdc.timer, TIME_NEVER);
			}
		}
		else
		if (fdc.timer_type == 4)
		{
			/* if in dma mode, a int is not generated per byte. If not in  DMA mode
			a int is generated per byte */
			if ((fdc.nec765_flags & NEC765_DMA_MODE) != 0)
			{
				nec765_set_dma_drq(1);
			}
			else
			{
				if ((fdc.FDC_main & (1<<7)) != 0)
				{
					/* set int to indicate data is ready */
					nec765_set_int(1);
				}
			}
	
			if (fdc.timer != null)
			{
				timer_reset(fdc.timer, TIME_NEVER);
			}
		}
            }
        };
	
	
	/* after (32-27) the DRQ is set, then 27 us later, the int is set.
	I don't know if this is correct, but it is required for the PCW driver.
	In this driver, the first NMI calls the handler function, furthur NMI's are
	effectively disabled by reading the data before the NMI int can be set.
	*/
	
	/* setup data request */
	static void nec765_setup_timed_data_request(int bytes)
	{
		/* setup timer to trigger in NEC765_DATA_RATE us */
		fdc.timer_type = 0;
		if (fdc.timer != null)
		{
			/* disable the timer */
			timer_remove(fdc.timer);	//timer_enable(fdc.timer, 0);
			fdc.timer = null;
		}
	
		if ((fdc.nec765_flags & NEC765_DMA_MODE) == 0)
		{
			fdc.timer = timer_set(TIME_IN_USEC(32-27)	/*NEC765_DATA_RATE)*bytes*/, 0, nec765_timer_callback);
		}
		else
		{
                    System.out.println("nec765_timer_callback");
			nec765_timer_callback.handler(fdc.timer_type);
		}
	}
	
	/* setup result data request */
	static void nec765_setup_timed_result_data_request()
	{
		fdc.timer_type = 2;
		if (fdc.timer != null)
		{
			/* disable the timer */
			timer_remove(fdc.timer);
			fdc.timer = null;
		}
		if ((fdc.nec765_flags & NEC765_DMA_MODE) == 0)
		{
			fdc.timer = timer_set(TIME_IN_USEC(NEC765_DATA_RATE)*2, 0, nec765_timer_callback);
		}
		else
		{
			nec765_timer_callback.handler(fdc.timer_type);
		}
	}
	
	
	/* sets up a timer to issue a seek complete in signed_tracks time */
	static void nec765_setup_timed_int(int signed_tracks)
	{
		if (fdc.seek_timer != null)
		{
			/* disable the timer */
			timer_remove(fdc.seek_timer);	
			fdc.seek_timer = null;
		}
	
		/* setup timer to signal after seek time is complete */
		fdc.seek_timer = timer_pulse(TIME_IN_MSEC(fdc.srt_in_ms*abs(signed_tracks)), 0, nec765_seek_timer_callback);
	}
	
	static void nec765_seek_setup(int is_recalibrate)
	{
		int signed_tracks;
		
		fdc.nec765_flags |= NEC765_SEEK_ACTIVE;
	
		if (is_recalibrate != 0)
		{
			/* head cannot be specified with recalibrate */
			fdc.nec765_command_bytes[1] &=~0x04;
		}
	
		nec765_setup_drive_and_side();
	
		fdc.FDC_main |= (1<<fdc.drive);
	
		/* recalibrate command? */
		if (is_recalibrate != 0)
		{
			fdc.nec765_flags |= NEC765_SEEK_OPERATION_IS_RECALIBRATE;
	
			fdc.ncn = 0;
	
			/* if drive is already at track 0, or drive is not ready */
			if (
				floppy_drive_get_flag_state(fdc.drive, FLOPPY_DRIVE_HEAD_AT_TRACK_0)!=0 || 
				(floppy_drive_get_flag_state(fdc.drive, FLOPPY_DRIVE_READY) == 0)
				)
			{
				/* seek completed */
				nec765_seek_complete();
			}
			else
			{
				/* is drive present? */
				if (floppy_drive_get_flag_state(fdc.drive, FLOPPY_DRIVE_CONNECTED)!=0)
				{
					/* yes - calculate real number of tracks to seek */
	
					int current_track;
		
					/* get current track */
					current_track = floppy_drive_get_current_track(fdc.drive);
	
					/* get number of tracks to seek */
					signed_tracks = -current_track;
				}
				else
				{
					/* no, seek 77 tracks and then stop */
					/* true for NEC765A, but not for other variants */
					signed_tracks = -77;
				}
	
				if (signed_tracks!=0)
				{
					/* perform seek - if drive isn't present it will not do anything */
					floppy_drive_seek(fdc.drive, signed_tracks);
				
					nec765_setup_timed_int(signed_tracks);
				}
				else
				{
					nec765_seek_complete();
				}
			}
		}
		else
		{
	
			fdc.nec765_flags &= ~NEC765_SEEK_OPERATION_IS_RECALIBRATE;
	
			fdc.ncn = fdc.nec765_command_bytes[2]&0xFF;
	
			/* get signed tracks */
			signed_tracks = fdc.ncn - fdc.pcn[fdc.drive];
	
			/* if no tracks to seek, or drive is not ready, seek is complete */
			if ((signed_tracks==0) || (floppy_drive_get_flag_state(fdc.drive, FLOPPY_DRIVE_READY)==0))
			{
				nec765_seek_complete();
			}
			else
			{
				/* perform seek - if drive isn't present it will not do anything */
				floppy_drive_seek(fdc.drive, signed_tracks);
	
				/* seek complete - issue an interrupt */
				nec765_setup_timed_int(signed_tracks);
			}
		}
	
	    nec765_idle();
	
	}
	
	
	
	static void nec765_setup_execution_phase_read(char[] ptr, int size)
	{
		fdc.FDC_main |= 0x040;                     /* FDC.CPU */
	
		fdc.nec765_transfer_bytes_count = 0;
		fdc.nec765_transfer_bytes_remaining = size;
		fdc.execution_phase_data = ptr;
		fdc.nec765_phase = NEC765_EXECUTION_PHASE_READ;
	
		nec765_setup_timed_data_request(1);
	}
	
	static void nec765_setup_execution_phase_write(char[] ptr, int size)
	{
		fdc.FDC_main &= ~0x040;                     /* FDC.CPU */
	
		fdc.nec765_transfer_bytes_count = 0;
		fdc.nec765_transfer_bytes_remaining = size;
		fdc.execution_phase_data = ptr;
		fdc.nec765_phase = NEC765_EXECUTION_PHASE_WRITE;
	
		/* setup a data request with first byte */
		nec765_setup_timed_data_request(1);
	}
	
	
	static void nec765_setup_result_phase(int byte_count)
	{
		fdc.FDC_main |= 0x040;                     /* FDC.CPU */
		fdc.FDC_main &= ~0x020;                    /* not execution phase */
	
		fdc.nec765_transfer_bytes_count = 0;
		fdc.nec765_transfer_bytes_remaining = byte_count;
		fdc.nec765_phase = NEC765_RESULT_PHASE;
	
		nec765_setup_timed_result_data_request();
	}
	
	public static void nec765_idle()
	{
		fdc.FDC_main &= ~0x040;                     /* CPU.FDC */
		fdc.FDC_main &= ~0x020;                    /* not execution phase */
		fdc.FDC_main &= ~0x010;                     /* not busy */
		fdc.nec765_phase = NEC765_COMMAND_PHASE_FIRST_BYTE;
	
		nec765_set_data_request();
	}
	
	/* set int output */
	static void nec765_set_int(int state)
	{
		fdc.nec765_flags &= ~NEC765_INT;
	
		if (state != 0)
		{
			fdc.nec765_flags |= NEC765_INT;
		}
	
		if (nec765_iface.interrupt != null)
			nec765_iface.interrupt.handler((fdc.nec765_flags & NEC765_INT));
	}
	
	/* set dma request output */
	public static void nec765_set_dma_drq(int state)
	{
		fdc.nec765_flags &= ~NEC765_DMA_DRQ;
	
		if (state != 0)
		{
			fdc.nec765_flags |= NEC765_DMA_DRQ;
		}
	
		if (nec765_iface.dma_drq != null)
			nec765_iface.dma_drq.handler((fdc.nec765_flags & NEC765_DMA_DRQ), (fdc.FDC_main & (1<<6)));
	}
	
	/* Drive ready */
	
	/* 
	
	A drive will report ready if:
	- drive is selected
	- disc is in the drive
	- disk is rotating at a constant speed (normally 300rpm)
	
	On more modern PCs, a ready signal is not provided by the drive.
	This signal is not used in the PC design and was eliminated to save costs 
	If you look at the datasheets for the modern NEC765 variants, you will see the Ready
	signal is not mentioned.
	
	On the original NEC765A, ready signal is required, and some commands will fail if the drive
	is not ready.
	
	
	
	
	*/
	
	
	
	
	/* done when ready state of drive changes */
	/* this ignores if command is active, in which case command should terminate immediatly
	with error */
	public static i_ready_state_change_callback nec765_set_ready_change_callback = new i_ready_state_change_callback() {
            public void handler(int drive, int state) {
                logerror("nec765: ready state change\n");
	
		/* drive that changed state */
		fdc.nec765_status[0] = 0x0c0 | drive;
	
		/* not ready */
		if (state==0)
			fdc.nec765_status[0] |= 8;
	
		/* trigger an int */
		nec765_set_int(1);
            }
        };
	
	public static void nec765_init(nec765_interface iface, int version)
	{
		fdc.version = version;
		fdc.timer = timer_set(TIME_NEVER, 0, nec765_timer_callback);
		fdc.seek_timer = null;
		//memset(&nec765_iface, 0, sizeof(nec765_interface));
	
		if (iface != null)
		{
			//memcpy(&nec765_iface, iface, sizeof(nec765_interface));
                    nec765_iface = iface;
		}
	
		fdc.nec765_flags &= NEC765_FDD_READY;
	
		nec765_reset(0);
	
		floppy_drive_set_ready_state_change_callback(0, nec765_set_ready_change_callback);
		floppy_drive_set_ready_state_change_callback(1, nec765_set_ready_change_callback);
		floppy_drive_set_ready_state_change_callback(2, nec765_set_ready_change_callback);
		floppy_drive_set_ready_state_change_callback(3, nec765_set_ready_change_callback);
	}
	
	public static void nec765_stop()
	{
		if (fdc.timer!=null)
		{
			timer_remove(fdc.timer);
			fdc.timer = null;
		}
	
		if (fdc.seek_timer!=null)
		{
			timer_remove(fdc.seek_timer);
			fdc.seek_timer = null;
		}
	}
	
	
	/* terminal count input */
	static void nec765_set_tc_state(int state)
	{
		int old_state;
	
		old_state = fdc.nec765_flags;
	
		/* clear drq */
		nec765_set_dma_drq(0);
	
		fdc.nec765_flags &= ~NEC765_TC;
		if (state != 0)
		{
			fdc.nec765_flags |= NEC765_TC;
		}
	
		/* changed state? */
		if (((fdc.nec765_flags^old_state) & NEC765_TC)!=0)
		{
			/* now set? */
			if ((fdc.nec765_flags & NEC765_TC)!=0)
			{
				/* yes */
				if (fdc.timer != null)
				{
					if (fdc.timer_type==0)
					{
						timer_remove(fdc.timer);
						fdc.timer = null;
					}
				}
	
                                if( NO_END_OF_CYLINDER ){
                                                        nec765_continue_command();
                                } else {
                                                        nec765_update_state();
                                }
			}
		}
	}
	
	public static ReadHandlerPtr nec765_status_r = new ReadHandlerPtr() {
            public int handler(int offset) {
                /*TODO*///#ifdef SUPER_VERBOSE
                /*TODO*///	logerror("nec765 status r: %02x\n",fdc.FDC_main);
                /*TODO*///#endif
		return fdc.FDC_main&0xFF;
            }
        };
	
	/* control mark handling code */
	
	/* if SK==1, and we are executing a read data command, and a deleted data mark is found,
	skip it.
	if SK==1, and we are executing a read deleted data command, and a data mark is found,
	skip it. */
	
	static int nec765_read_skip_sector()
	{
		/* skip set? */
		if ((fdc.nec765_command_bytes[0] & (1<<5))!=0)
		{
			/* read data? */
			if ((fdc.command&0xFF) == 0x06)
			{
				/* did we just find a sector with deleted data mark? */
				if (fdc.data_type == NEC765_DAM_DELETED_DATA)
				{
					/* skip it */
					return 1; //return TRUE;
				}
			}
			/* deleted data? */
			else 
			if ((fdc.command&0xFF) == 0x0c)
			{
				/* did we just find a sector with data mark ? */
				if (fdc.data_type == NEC765_DAM_DATA)
				{
					/* skip it */
					return 1; //return TRUE;
				}
			}
		}
	
		/* do not skip */
		return 0; //return FALSE;
	}
	
	/* this is much closer to how the nec765 actually gets sectors */
	/* used by read data, read deleted data, write data, write deleted data */
	/* What the nec765 does:
	
	  - get next sector id from disc
	  - if sector id matches id specified in command, it will
		search for next data block and read data from it.
	
	  - if the index is seen twice while it is searching for a sector, then the sector cannot be found
	*/
	
	static void nec765_get_next_id(chrn_id id)
	{
		/* get next id from disc */
            //System.out.println("Antes: "+id.data_id);
		floppy_drive_get_next_id(fdc.drive, fdc.side,id);
	
		fdc.sector_id = id.data_id&0xFF;
                
                //System.out.println("Despues: "+id.data_id);
	
		/* set correct data type */
		fdc.data_type = NEC765_DAM_DATA;
		if ((id.flags & ID_FLAG_DELETED_DATA) != 0)
		{
			fdc.data_type = NEC765_DAM_DELETED_DATA;
		}
	}
	
	static int nec765_get_matching_sector()
	{
		/* number of times we have seen index hole */
		int index_count = 0;
	
		/* get sector id's */
		do
	    {
			chrn_id id = new chrn_id();
	
			nec765_get_next_id(id);
	
			/* tested on Amstrad CPC - All bytes must match, otherwise
			a NO DATA error is reported */
			if (id.R == fdc.nec765_command_bytes[4])
			{
				if (id.C == fdc.nec765_command_bytes[2])
				{
					if (id.H == fdc.nec765_command_bytes[3])
					{
						if (id.N == fdc.nec765_command_bytes[5])
						{
							/* end of cylinder is set if:
							1. sector data is read completely (i.e. no other errors occur like
							no data.
							2. sector being read is same specified by EOT
							3. terminal count is not received */
							if (fdc.nec765_command_bytes[4]==fdc.nec765_command_bytes[6])
							{
								/* set end of cylinder */
								fdc.nec765_status[1] |= NEC765_ST1_END_OF_CYLINDER;
							}
	
							return 1; //return TRUE;
						}
					}
				}
				else
				{
					/* the specified sector ID was found, however, the C value specified
					in the read/write command did not match the C value read from the disc */
	
					/* no data - checked on Amstrad CPC */
					fdc.nec765_status[1] |= NEC765_ST1_NO_DATA;
					/* bad C value */
					fdc.nec765_status[2] |= NEC765_ST2_WRONG_CYLINDER;
	
					if ((id.C&0xFF) == 0x0ff)
					{
						/* the C value is 0x0ff which indicates a bad track in the IBM soft-sectored
						format */
						fdc.nec765_status[2] |= NEC765_ST2_BAD_CYLINDER;
					}
	
					return 0; //return FALSE;
				}
			}
	
			 /* index set? */
			if (floppy_drive_get_flag_state(fdc.drive, FLOPPY_DRIVE_INDEX) != 0)
			{
				index_count++;
			}
	   
		}
		while (index_count!=2);
	
		/* no data - specified sector ID was not found */
                fdc.nec765_status[1] |= NEC765_ST1_NO_DATA;
	  
		return 0;
	}
	
	static void nec765_read_complete()
	{
	
                /* causes problems!!! - need to fix */
                if( NO_END_OF_CYLINDER ){
                        /* set end of cylinder */
                        fdc.nec765_status[1] &= ~NEC765_ST1_END_OF_CYLINDER;
                } else {
                        /* completed read command */

                        /* end of cylinder is set when:
                         - a whole sector has been read
                         - terminal count input is not set
                         - AND the the sector specified by EOT was read
                         */

                        /* if end of cylinder is set, and we did receive a terminal count, then clear it */
                        if ((fdc.nec765_flags & NEC765_TC)!=0)
                        {
                                /* set end of cylinder */
                                fdc.nec765_status[1] &= ~NEC765_ST1_END_OF_CYLINDER;
                        }
                }
	
		nec765_setup_st0();
	
		fdc.nec765_result_bytes[0] = fdc.nec765_status[0]&0xFF;
		fdc.nec765_result_bytes[1] = fdc.nec765_status[1]&0xFF;
		fdc.nec765_result_bytes[2] = fdc.nec765_status[2]&0xFF;
		fdc.nec765_result_bytes[3] = fdc.nec765_command_bytes[2]&0xFF; /* C */
		fdc.nec765_result_bytes[4] = fdc.nec765_command_bytes[3]&0xFF; /* H */
		fdc.nec765_result_bytes[5] = fdc.nec765_command_bytes[4]&0xFF; /* R */
		fdc.nec765_result_bytes[6] = fdc.nec765_command_bytes[5]&0xFF; /* N */
	
		nec765_setup_result_phase(7);
	}
	
	static void nec765_read_data()
	{
	
		if ((floppy_drive_get_flag_state(fdc.drive, FLOPPY_DRIVE_READY))==0)
		{
			fdc.nec765_status[0] = 0x0c0 | (1<<4) | fdc.drive | (fdc.side<<2);
			fdc.nec765_status[1] = 0x00;
			fdc.nec765_status[2] = 0x00;
	
			fdc.nec765_result_bytes[0] = fdc.nec765_status[0]&0xffff;
			fdc.nec765_result_bytes[1] = fdc.nec765_status[1]&0xffff;
			fdc.nec765_result_bytes[2] = fdc.nec765_status[2]&0xffff;
			fdc.nec765_result_bytes[3] = fdc.nec765_command_bytes[2]&0xffff; /* C */
			fdc.nec765_result_bytes[4] = fdc.nec765_command_bytes[3]&0xffff; /* H */
			fdc.nec765_result_bytes[5] = fdc.nec765_command_bytes[4]&0xffff; /* R */
			fdc.nec765_result_bytes[6] = fdc.nec765_command_bytes[5]&0xffff; /* N */
			nec765_setup_result_phase(7);
			return;
		}
/*TODO*///	#ifdef VERBOSE
/*TODO*///		logerror("sector c: %02x h: %02x r: %02x n: %02x\n",fdc.nec765_command_bytes[2], fdc.nec765_command_bytes[3],fdc.nec765_command_bytes[4], fdc.nec765_command_bytes[5]);
/*TODO*///	#endif
		/* find a sector to read data from */
		{
			int found_sector_to_read;
	
			found_sector_to_read = 0;
			/* check for finished reading sectors */
			do
			{
				/* get matching sector */
				if (nec765_get_matching_sector() != 0)
				{
	
					/* skip it? */
					if (nec765_read_skip_sector() != 0)
					{
						/* yes */
	
						/* check that we haven't finished reading all sectors */
						if (nec765_sector_count_complete()!=0)
						{
							/* read complete */
							nec765_read_complete();
							return;
						}
	
						/* read not finished */
	
						/* increment sector count */
						nec765_increment_sector();
					}
					else
					{
						/* found a sector to read */
						found_sector_to_read = 1;
					}
				}
				else
				{
					/* error in finding sector */
					nec765_read_complete();
					return;
				}
			}
			while (found_sector_to_read==0);
		}	
			
		{
			int data_size;
	
			data_size = nec765_n_to_bytes(fdc.nec765_command_bytes[5]);
	
			floppy_drive_read_sector_data(fdc.drive, fdc.side, fdc.sector_id,nec765_data_buffer,data_size);
	
	        nec765_setup_execution_phase_read(nec765_data_buffer, data_size);
		}
	}
	
	
	static void nec765_format_track()
	{
		/* write protected? */
		if (floppy_drive_get_flag_state(fdc.drive,FLOPPY_DRIVE_DISK_WRITE_PROTECTED) != 0)
		{
			fdc.nec765_status[1] |= NEC765_ST1_NOT_WRITEABLE;
	
			nec765_setup_st0();
			/* TODO: Check result is correct */
				fdc.nec765_result_bytes[0] = fdc.nec765_status[0];
                                fdc.nec765_result_bytes[1] = fdc.nec765_status[1];
                                fdc.nec765_result_bytes[2] = fdc.nec765_status[2];
				fdc.nec765_result_bytes[3] = fdc.format_data[0];
				fdc.nec765_result_bytes[4] = fdc.format_data[1];
				fdc.nec765_result_bytes[5] = fdc.format_data[2];
				fdc.nec765_result_bytes[6] = fdc.format_data[3];
				nec765_setup_result_phase(7);
	
			return;
		}
	
	    nec765_setup_execution_phase_write(fdc.format_data, 4);
	}
	
	static void nec765_read_a_track()
	{
            System.out.println("nec765_read_a_track");
		int data_size;
	
		/* SKIP not allowed with this command! */
	
		/* get next id */
		chrn_id id = new chrn_id();
	
		nec765_get_next_id(id);
	
	        /* TO BE CONFIRMED! */
	        /* check id from disc */
	        if (id.C==fdc.nec765_command_bytes[2])
	        {
	            if (id.H==fdc.nec765_command_bytes[3])
	            {
	                if (id.R==fdc.nec765_command_bytes[4])
	                {
	                    if (id.N==fdc.nec765_command_bytes[5])
	                    {
	                        /* if ID found, then no data is not set */
	                        /* otherwise no data will remain set */
	                        fdc.nec765_status[1] &=~NEC765_ST1_NO_DATA;
	                    }
	                }
	            }
	        }
	
	
	        data_size = nec765_n_to_bytes(id.N);
		
		floppy_drive_read_sector_data(fdc.drive, fdc.side, fdc.sector_id,nec765_data_buffer,data_size);
	
		nec765_setup_execution_phase_read(nec765_data_buffer, data_size);
	}
	
	static int nec765_just_read_last_sector_on_track()
	{
		if (floppy_drive_get_flag_state(fdc.drive, FLOPPY_DRIVE_INDEX) != 0)
			return 1;
	
		return 0;
	
	
	}
	
	static void nec765_write_complete()
	{
	
            /* causes problems!!! - need to fix */
            if( NO_END_OF_CYLINDER ){
                    /* set end of cylinder */
                    fdc.nec765_status[1] &= ~NEC765_ST1_END_OF_CYLINDER;
            } else {
                    /* completed read command */

                    /* end of cylinder is set when:
                     - a whole sector has been read
                     - terminal count input is not set
                     - AND the the sector specified by EOT was read
                     */

                    /* if end of cylinder is set, and we did receive a terminal count, then clear it */
                    if ((fdc.nec765_flags & NEC765_TC)!=0)
                    {
                            /* set end of cylinder */
                            fdc.nec765_status[1] &= ~NEC765_ST1_END_OF_CYLINDER;
                    }
            }
	
		nec765_setup_st0();
	
	    fdc.nec765_result_bytes[0] = fdc.nec765_status[0];
	    fdc.nec765_result_bytes[1] = fdc.nec765_status[1];
	    fdc.nec765_result_bytes[2] = fdc.nec765_status[2];
	    fdc.nec765_result_bytes[3] = fdc.nec765_command_bytes[2]; /* C */
	    fdc.nec765_result_bytes[4] = fdc.nec765_command_bytes[3]; /* H */
	    fdc.nec765_result_bytes[5] = fdc.nec765_command_bytes[4]; /* R */
	    fdc.nec765_result_bytes[6] = fdc.nec765_command_bytes[5]; /* N */
	
	    nec765_setup_result_phase(7);
	}
	
	
	static void nec765_write_data()
	{
		if ((floppy_drive_get_flag_state(fdc.drive, FLOPPY_DRIVE_READY)) == 0)
		{
			fdc.nec765_status[0] = 0x0c0 | (1<<4) | fdc.drive | (fdc.side<<2);
                        fdc.nec765_status[1] = 0x00;
                        fdc.nec765_status[2] = 0x00;

                        fdc.nec765_result_bytes[0] = fdc.nec765_status[0];
                        fdc.nec765_result_bytes[1] = fdc.nec765_status[1];
                        fdc.nec765_result_bytes[2] = fdc.nec765_status[2];
                        fdc.nec765_result_bytes[3] = fdc.nec765_command_bytes[2]; /* C */
                        fdc.nec765_result_bytes[4] = fdc.nec765_command_bytes[3]; /* H */
                        fdc.nec765_result_bytes[5] = fdc.nec765_command_bytes[4]; /* R */
                        fdc.nec765_result_bytes[6] = fdc.nec765_command_bytes[5]; /* N */
			nec765_setup_result_phase(7);
			return;
		}
	
		/* write protected? */
		if (floppy_drive_get_flag_state(fdc.drive,FLOPPY_DRIVE_DISK_WRITE_PROTECTED) != 0)
		{
			fdc.nec765_status[1] |= NEC765_ST1_NOT_WRITEABLE;
	
			nec765_write_complete();
			return;
		}
	
		if (nec765_get_matching_sector() != 0)
		{
			int data_size;
	
			data_size = nec765_n_to_bytes(fdc.nec765_command_bytes[5]);
	
	        nec765_setup_execution_phase_write(nec765_data_buffer, data_size);
		}
	    else
	    {
	        nec765_setup_result_phase(7);
	    }
	}
	
	
	/* return true if we have read all sectors, false if not */
	static int nec765_sector_count_complete()
	{
	/* this is not correct?? */
	/*TODO*///#if 1
		/* if terminal count has been set - yes */
		if ((fdc.nec765_flags & NEC765_TC) != 0)
		{
			/* completed */
			return 1;
		}
	
	
		
		/* multi-track? */
		if ((fdc.nec765_command_bytes[0] & 0x080) != 0)
		{
			/* it appears that in multi-track mode,
			the EOT parameter of the command is ignored!? -
			or is it ignored the first time and not the next, so that
			if it is started on side 0, it will end at EOT on side 1,
			but if started on side 1 it will end at end of track????
			
			PC driver requires this to end at last sector on side 1, and
			ignore EOT parameter.
			
			To be checked!!!!
			*/
	
			/* if just read last sector and on side 1 - finish */
			if ((nec765_just_read_last_sector_on_track() != 0) &&
				(fdc.side==1))
			{
				return 1;
			}
	
			/* if not on second side then we haven't finished yet */
			if (fdc.side!=1)
			{
				/* haven't finished yet */
				return 0;
			}
		}
		else
		{
			/* sector id == EOT? */
			if ((fdc.nec765_command_bytes[4]==fdc.nec765_command_bytes[6]))
			{
	
				/* completed */
				return 1;
			}
		}
	/*TODO*///#else
	/*TODO*///
	/*TODO*///	/* if terminal count has been set - yes */
	/*TODO*///	if (fdc.nec765_flags & NEC765_TC)
	/*TODO*///	{
	/*TODO*///		/* completed */
	/*TODO*///		return 1;
	/*TODO*///	}
	/*TODO*///	
	/*TODO*///	/* Multi-Track operation:
	/*TODO*///
	/*TODO*///	Verified on Amstrad CPC.
	/*TODO*///
	/*TODO*///		disc format used:
	/*TODO*///			9 sectors per track
	/*TODO*///			2 sides
	/*TODO*///			Sector IDs: &01, &02, &03, &04, &05, &06, &07, &08, &09
	/*TODO*///
	/*TODO*///		Command specified: 
	/*TODO*///			SIDE = 0,
	/*TODO*///			C = 0,H = 0,R = 1, N = 2, EOT = 1
	/*TODO*///		Sectors read:
	/*TODO*///			Sector 1 side 0
	/*TODO*///			Sector 1 side 1
	/*TODO*///
	/*TODO*///		Command specified: 
	/*TODO*///			SIDE = 0,
	/*TODO*///			C = 0,H = 0,R = 1, N = 2, EOT = 3
	/*TODO*///		Sectors read:
	/*TODO*///			Sector 1 side 0
	/*TODO*///			Sector 2 side 0
	/*TODO*///			Sector 3 side 0
	/*TODO*///			Sector 1 side 1
	/*TODO*///			Sector 2 side 1
	/*TODO*///			Sector 3 side 1
	/*TODO*///
	/*TODO*///			
	/*TODO*///		Command specified:
	/*TODO*///			SIDE = 0,
	/*TODO*///			C = 0, H = 0, R = 7, N = 2, EOT = 3
	/*TODO*///		Sectors read:
	/*TODO*///			Sector 7 side 0
	/*TODO*///			Sector 8 side 0
	/*TODO*///			Sector 9 side 0
	/*TODO*///			Sector 10 not found. Error "No Data"
	/*TODO*///
	/*TODO*///		Command specified:
	/*TODO*///			SIDE = 1,
	/*TODO*///			C = 0, H = 1, R = 1, N = 2, EOT = 1
	/*TODO*///		Sectors read:
	/*TODO*///			Sector 1 side 1
	/*TODO*///
	/*TODO*///		Command specified:
	/*TODO*///			SIDE = 1,
	/*TODO*///			C = 0, H = 1, R = 1, N = 2, EOT = 2
	/*TODO*///		Sectors read:
	/*TODO*///			Sector 1 side 1
	/*TODO*///			Sector 1 side 2
	/*TODO*///
	/*TODO*///  */
	/*TODO*///
	/*TODO*///	/* sector id == EOT? */
	/*TODO*///	if ((fdc.nec765_command_bytes[4]==fdc.nec765_command_bytes[6]))
	/*TODO*///	{
	/*TODO*///		/* multi-track? */
	/*TODO*///		if (fdc.nec765_command_bytes[0] & 0x080)
	/*TODO*///		{
	/*TODO*///			/* if we have reached EOT (fdc.nec765_command_bytes[6]) 
	/*TODO*///			on side 1, then read is complete */
	/*TODO*///			if (fdc.side==1)
	/*TODO*///				return 1;
	/*TODO*///
	/*TODO*///			return 0;
	/*TODO*///
	/*TODO*///		}
	/*TODO*///
	/*TODO*///		/* completed */
	/*TODO*///		return 1;
	/*TODO*///	}
	/*TODO*///#endif
		/* not complete */
		return 0;
	}
	
	static void nec765_increment_sector()
	{
		/* multi-track? */
		if ((fdc.nec765_command_bytes[0] & 0x080) != 0)
		{
			/* reached EOT? */
			/* if (fdc.nec765_command_bytes[4]==fdc.nec765_command_bytes[6])*/
			if (nec765_just_read_last_sector_on_track() != 0)
			{
				/* yes */
	
				/* reached EOT */
				/* change side to 1 */
				fdc.side = 1;
				/* reset sector id to 1 */
				fdc.nec765_command_bytes[4] = 1;
				/* set head to 1 for get next sector test */
				fdc.nec765_command_bytes[3] = 1;
			}
			else
			{
				/* increment */
				fdc.nec765_command_bytes[4]++;
			}
	
		}
		else
		{
			fdc.nec765_command_bytes[4]++;
		}
	}
	
	/* control mark handling code */
	
	/* if SK==0, and we are executing a read data command, and a deleted data sector is found,
	the data is not skipped. The data is read, but the control mark is set and the read is stopped */
	/* if SK==0, and we are executing a read deleted data command, and a data sector is found,
	the data is not skipped. The data is read, but the control mark is set and the read is stopped */
	static int nec765_read_data_stop()
	{
		/* skip not set? */
		if ((fdc.nec765_command_bytes[0] & (1<<5))==0)
		{
			/* read data? */
			if (fdc.command == 0x06)
			{
				/* did we just read a sector with deleted data? */
				if (fdc.data_type == NEC765_DAM_DELETED_DATA)
				{
					/* set control mark */
					fdc.nec765_status[2] |= NEC765_ST2_CONTROL_MARK;
	
					/* quit */
					return 1; //return TRUE;
				}
			}
			/* deleted data? */
			else 
			if (fdc.command == 0x0c)
			{
				/* did we just read a sector with data? */
				if (fdc.data_type == NEC765_DAM_DATA)
				{
					/* set control mark */
					fdc.nec765_status[2] |= NEC765_ST2_CONTROL_MARK;
	
					/* quit */
					return 1; //return TRUE;
				}
			}
		}
	
		/* continue */
		return 0; //return FALSE;
	}
	
	static void nec765_continue_command()
	{
		if ((fdc.nec765_phase == NEC765_EXECUTION_PHASE_READ) ||
			(fdc.nec765_phase == NEC765_EXECUTION_PHASE_WRITE))
		{
			switch (fdc.command)
	        {
				/* read a track */
				case 0x02:
				{
					fdc.sector_counter++;
	
					/* sector counter == EOT */
					if (fdc.sector_counter==fdc.nec765_command_bytes[6])
					{
						/* TODO: Add correct info here */
	
						fdc.nec765_status[1] |= NEC765_ST1_END_OF_CYLINDER;
	
						nec765_setup_st0();
	
						fdc.nec765_result_bytes[0] = fdc.nec765_status[0];
						fdc.nec765_result_bytes[1] = fdc.nec765_status[1];
						fdc.nec765_result_bytes[2] = fdc.nec765_status[2];
						fdc.nec765_result_bytes[3] = fdc.nec765_command_bytes[2]; /* C */
						fdc.nec765_result_bytes[4] = fdc.nec765_command_bytes[3]; /* H */
						fdc.nec765_result_bytes[5] = fdc.nec765_command_bytes[4]; /* R */
						fdc.nec765_result_bytes[6] = fdc.nec765_command_bytes[5]; /* N */
	
						nec765_setup_result_phase(7);
					}
					else
					{
						nec765_read_a_track();
					}
				}
				break;
	
				/* format track */
				case 0x0d:
				{
					floppy_drive_format_sector(fdc.drive, fdc.side, fdc.sector_counter,
						fdc.format_data[0], fdc.format_data[1],
						fdc.format_data[2], fdc.format_data[3],
						fdc.nec765_command_bytes[5]);
	
					fdc.sector_counter++;
	
					/* sector_counter = SC */
					if (fdc.sector_counter == fdc.nec765_command_bytes[3])
					{
						/* TODO: Check result is correct */
						fdc.nec765_result_bytes[0] = fdc.nec765_status[0];
						fdc.nec765_result_bytes[1] = fdc.nec765_status[1];
						fdc.nec765_result_bytes[2] = fdc.nec765_status[2];
						fdc.nec765_result_bytes[3] = fdc.format_data[0];
						fdc.nec765_result_bytes[4] = fdc.format_data[1];
						fdc.nec765_result_bytes[5] = fdc.format_data[2];
						fdc.nec765_result_bytes[6] = fdc.format_data[3];
						nec765_setup_result_phase(7);
					}
					else
					{
						nec765_format_track();
					}
				}
				break;
	
				/* write data, write deleted data */
				case 0x09:
				case 0x05:
				{
					/* sector id == EOT */
					int ddam;
	
					ddam = 0;
					if (fdc.command == 0x09)
					{
						ddam = 1;
					}
	
					/* write data to disc */
					floppy_drive_write_sector_data(fdc.drive, fdc.side, fdc.sector_id,nec765_data_buffer,nec765_n_to_bytes(fdc.nec765_command_bytes[5]),ddam);
	
					if (nec765_sector_count_complete() != 0)
					{
						nec765_increment_sector();
						nec765_write_complete();
					}
					else
					{
						nec765_increment_sector();
						nec765_write_data();
					}
				}
				break;
	
				/* read data, read deleted data */
				case 0x0c:
				case 0x06:
				{
	
					/* read all sectors? */
	
					/* sector id == EOT */
					if (nec765_sector_count_complete()!=0 || nec765_read_data_stop()!=0)
				    {
						nec765_read_complete();
					}
					else
					{	
						nec765_increment_sector();
						nec765_read_data();
					}
					}
					break;
	
				default:
					break;
			}
		}
	}
	
	
	static int nec765_get_command_byte_count()
	{
		fdc.command = fdc.nec765_command_bytes[0] & 0x01f;
	
		if (fdc.version==NEC765A)
		{
			 return nec765_cmd_size[fdc.command];
	    }
		else
		{
			if (fdc.version==SMC37C78)
			{
				switch (fdc.command)
				{
					/* version */
					case 0x010:
						return 1;
				
					/* verify */
					case 0x016:
						return 9;
	
					/* configure */
					case 0x013:
						return 3;
	
					/* dumpreg */
					case 0x0e:
						return 1;
				
					/* perpendicular mode */
					case 0x012:
						return 1;
	
					/* lock */
					case 0x014:
						return 1;
				
					/* seek/relative seek are together! */
	
					default:
						return nec765_cmd_size[fdc.command];
				}
			}
		}
	
		return nec765_cmd_size[fdc.command];
	}
	
	
	
	
	
	static void nec765_update_state()
	{
	    switch (fdc.nec765_phase)
	    {
	         case NEC765_RESULT_PHASE:
	         {
	             /* set data reg */
				 fdc.nec765_data_reg = fdc.nec765_result_bytes[fdc.nec765_transfer_bytes_count];
	
				 if (fdc.nec765_transfer_bytes_count==0)
				 {
					/* clear int for specific commands */
					switch (fdc.command)
					{
						/* read a track */
						case 2:
						/* write data */
						case 5:
						/* read data */
						case 6:
						/* write deleted data */
						case 9:
						/* read id */
						case 10:
						/* read deleted data */
						case 12:
						/* format at track */
						case 13:
						/* scan equal */
						case 17:
						/* scan low or equal */
						case 19:
						/* scan high or equal */
						case 29:
						{
							nec765_set_int(0);
						}
						break;
	
						default:
							break;
					}
				 }
	
	/*TODO*///#ifdef VERBOSE
	/*TODO*///             logerror("NEC765: RESULT: %02x\n", fdc.nec765_data_reg);
	/*TODO*///#endif
	
	             fdc.nec765_transfer_bytes_count++;
	             fdc.nec765_transfer_bytes_remaining--;
	
	            if (fdc.nec765_transfer_bytes_remaining==0)
	            {
					nec765_idle();
	            }
				else
				{
					nec765_set_data_request();
				}
			 }
			 break;
	
	         case NEC765_EXECUTION_PHASE_READ:
	         {
				 /* setup data register */
	             fdc.nec765_data_reg = fdc.execution_phase_data[fdc.nec765_transfer_bytes_count];
	             fdc.nec765_transfer_bytes_count++;
	             fdc.nec765_transfer_bytes_remaining--;
	
	//#ifdef SUPER_VERBOSE
				logerror("EXECUTION PHASE READ: %02x\n", fdc.nec765_data_reg);
	//#endif
	
	            if ((fdc.nec765_transfer_bytes_remaining==0) || (fdc.nec765_flags & NEC765_TC)!=0)
	            {
	                nec765_continue_command();
	            }
				else
				{
					// trigger int
					nec765_setup_timed_data_request(1);
				}
			 }
			 break;
	
		    case NEC765_COMMAND_PHASE_FIRST_BYTE:
	        {
	                fdc.FDC_main |= 0x10;                      /* set BUSY */
	/*TODO*///#ifdef VERBOSE
	/*TODO*///                logerror("NEC765: COMMAND: %02x\n",fdc.nec765_data_reg);
	/*TODO*///#endif
					/* seek in progress? */
					if ((fdc.nec765_flags & NEC765_SEEK_ACTIVE) != 0)
					{
						/* any command results in a invalid - I think that seek, recalibrate and
						sense interrupt status may work*/
						fdc.nec765_data_reg = 0;
					}
	
					fdc.nec765_command_bytes[0] = fdc.nec765_data_reg;
	
					fdc.nec765_transfer_bytes_remaining = nec765_get_command_byte_count();
				
					fdc.nec765_transfer_bytes_count = 1;
	                fdc.nec765_transfer_bytes_remaining--;
	
	                if (fdc.nec765_transfer_bytes_remaining==0)
	                {
	                        nec765_setup_command();
	                }
	                else
	                {
							/* request more data */
							nec765_set_data_request();
	                        fdc.nec765_phase = NEC765_COMMAND_PHASE_BYTES;
	                }
	        }
	        break;
	
	                case NEC765_COMMAND_PHASE_BYTES:
	                {
	/*TODO*///#ifdef VERBOSE
	/*TODO*///                        logerror("NEC765: COMMAND: %02x\n",fdc.nec765_data_reg);
	/*TODO*///#endif
	                        fdc.nec765_command_bytes[fdc.nec765_transfer_bytes_count] = fdc.nec765_data_reg;
	                        fdc.nec765_transfer_bytes_count++;
	                        fdc.nec765_transfer_bytes_remaining--;
	
	                        if (fdc.nec765_transfer_bytes_remaining==0)
	                        {
	                                nec765_setup_command();
	                        }
							else
							{
								/* request more data */
								nec765_set_data_request();
							}
	
	                }
	                break;
	
	            case NEC765_EXECUTION_PHASE_WRITE:
	            {
	                fdc.execution_phase_data[fdc.nec765_transfer_bytes_count]=(char) fdc.nec765_data_reg;
	                fdc.nec765_transfer_bytes_count++;
	                fdc.nec765_transfer_bytes_remaining--;
	
	                 if ((fdc.nec765_transfer_bytes_remaining==0) || (fdc.nec765_flags & NEC765_TC)!=0)
	                {
	
	                        nec765_continue_command();
	                }
					else
					{
						nec765_setup_timed_data_request(1);
					}
	            }
			    break;
	
		}
	}
	
	
	public static ReadHandlerPtr nec765_data_r = new ReadHandlerPtr() {
            public int handler(int offset) {
                //	int data;
	
		/* get data we will return */
	//	data = fdc.nec765_data_reg;
	
	
		if ((fdc.FDC_main & 0x0c0)==0x0c0)
		{
			if (
				(fdc.nec765_phase == NEC765_EXECUTION_PHASE_READ) ||
				(fdc.nec765_phase == NEC765_EXECUTION_PHASE_WRITE))
			{
	
				/* reading the data byte clears the interrupt */
				nec765_set_int(0);
			}
	
			/* reset data request */
			nec765_clear_data_request();
	
			/* update state */
			nec765_update_state();
		}
	
	/*TODO*///#ifdef SUPER_VERBOSE
	/*TODO*///	logerror("DATA R: %02x\n", fdc.nec765_data_reg);
	/*TODO*///#endif
	
		return fdc.nec765_data_reg&0xFF;
            }
        };
	
	public static WriteHandlerPtr nec765_data_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                /*TODO*///#ifdef SUPER_VERBOSE
                /*TODO*///        logerror("DATA W: %02x\n", data);
                /*TODO*///#endif

		/* write data to data reg */
		fdc.nec765_data_reg = data;
	
		if ((fdc.FDC_main & 0x0c0)==0x080)
		{
			if (
				(fdc.nec765_phase == NEC765_EXECUTION_PHASE_READ) ||
				(fdc.nec765_phase == NEC765_EXECUTION_PHASE_WRITE))
			{
	
				/* reading the data byte clears the interrupt */
				nec765_set_int(0);
			}
	
			/* reset data request */
			nec765_clear_data_request();
	
			/* update state */
			nec765_update_state();
		}
            }
        };
	
	static void nec765_setup_invalid()
	{
		fdc.command = 0;
		fdc.nec765_result_bytes[0] = 0x080;
		nec765_setup_result_phase(1);
	}
	
	static void nec765_setup_command()
	{
		/* if not in dma mode set execution phase bit */
		if ((fdc.nec765_flags & NEC765_DMA_MODE) == 0)
		{
	        fdc.FDC_main |= 0x020;              /* execution phase */
		}
	
		switch (fdc.nec765_command_bytes[0] & 0x01f)
		{
			case 0x03:      /* specify */
			{
				/* setup step rate */
				fdc.srt_in_ms = 16-((fdc.nec765_command_bytes[1]>>4) & 0x0f);
	
				fdc.nec765_flags &= ~NEC765_DMA_MODE;
	
				if ((fdc.nec765_command_bytes[2] & 0x01)==0)
				{
					fdc.nec765_flags |= NEC765_DMA_MODE;
				}
	
				nec765_idle();
			}
			break;
	
			case 0x04:  /* sense drive status */
			{
				nec765_setup_drive_and_side();
	
				fdc.nec765_status[3] = fdc.drive | (fdc.side<<2);
	
				if (floppy_drive_get_flag_state(fdc.drive,FLOPPY_DRIVE_DISK_WRITE_PROTECTED) != 0)
				{
					fdc.nec765_status[3] |= 0x040;
				}
	
				if (floppy_drive_get_flag_state(fdc.drive, FLOPPY_DRIVE_READY) != 0)
				{
					fdc.nec765_status[3] |= 0x020;
				}
	
				if (floppy_drive_get_flag_state(fdc.drive, FLOPPY_DRIVE_HEAD_AT_TRACK_0) != 0)
				{
					fdc.nec765_status[3] |= 0x010;
				}
	
				fdc.nec765_status[3] |= 0x08;
	                               
					/* two side and fault not set but should be? */
	
				fdc.nec765_result_bytes[0] = fdc.nec765_status[3];
	
				nec765_setup_result_phase(1);
			}
			break;
	
			case 0x07:          /* recalibrate */
				nec765_seek_setup(1);
				break;
			case 0x0f:          /* seek */
				nec765_seek_setup(0);
				break;
	
			case 0x0a:      /* read id */
			{
				chrn_id id = new chrn_id();
	
				nec765_setup_drive_and_side();
	
				fdc.nec765_status[0] = fdc.drive | (fdc.side<<2);
				fdc.nec765_status[1] = 0;
				fdc.nec765_status[2] = 0;
	
				/* drive ready? */
				if (floppy_drive_get_flag_state(fdc.drive, FLOPPY_DRIVE_READY) != 0)
				{
					/* is disk inserted? */
					if (floppy_drive_get_flag_state(fdc.drive, FLOPPY_DRIVE_DISK_INSERTED) != 0)
					{
						int index_count = 0;
                                                
                                                //System.out.println("READY!!!!");
	
						/* floppy drive is ready and disc is inserted */
	
						/* this is the id that appears when a disc is not formatted */
						/* to be checked on Amstrad */
						id.C = 0;
						id.H = 0;
						id.R = 0x01;
						id.N = 0x02;
	
						/* repeat for two index counts before quitting */
						do
						{
							/* get next id from disc */
							if (floppy_drive_get_next_id(fdc.drive, fdc.side,id) != 0)
							{
								/* got an id - quit */
                                                            //System.out.println("GOT an ID!!!!");
                                                            //System.out.println(id.data_id);
								break;
							}
	
							if (floppy_drive_get_flag_state(fdc.drive, FLOPPY_DRIVE_INDEX) != 0)
							{
								/* update index count */
								index_count++;
							}
						}
						while (index_count!=2);
							
						/* at this point, we have seen a id or two index pulses have occured! */
						fdc.nec765_result_bytes[0] = fdc.nec765_status[0];
						fdc.nec765_result_bytes[1] = fdc.nec765_status[1];
						fdc.nec765_result_bytes[2] = fdc.nec765_status[2];
						fdc.nec765_result_bytes[3] = id.C; /* C */
						fdc.nec765_result_bytes[4] = id.H; /* H */
						fdc.nec765_result_bytes[5] = id.R; /* R */
						fdc.nec765_result_bytes[6] = id.N; /* N */
	
						nec765_setup_result_phase(7);
					}
					else
					{
						/* floppy drive is ready, but no disc is inserted */
						/* this occurs on the PC */
						/* in this case, the command never quits! */
						/* there are no index pulses to stop the command! */
					}
				}
				else
				{
					/* what are id values when drive not ready? */
	
					/* not ready, abnormal termination */
					fdc.nec765_status[0] |= (1<<3) | (1<<6);
					fdc.nec765_result_bytes[0] = fdc.nec765_status[0];
					fdc.nec765_result_bytes[1] = fdc.nec765_status[1];
					fdc.nec765_result_bytes[2] = fdc.nec765_status[2];
					fdc.nec765_result_bytes[3] = 0; /* C */
					fdc.nec765_result_bytes[4] = 0; /* H */
					fdc.nec765_result_bytes[5] = 0; /* R */
					fdc.nec765_result_bytes[6] = 0; /* N */
				}
			}
			break;
	
	
			case 0x08: /* sense interrupt status */
			/* interrupt pending? */
			if ((fdc.nec765_flags & NEC765_INT) != 0)
			{
				/* yes. Clear int */
				nec765_set_int(0);
	
				/* clear drive seek bits */
				fdc.FDC_main &= ~(1 | 2 | 4 | 8);
	
				/* return status */
				fdc.nec765_result_bytes[0] = fdc.nec765_status[0];
				/* return pcn */
				fdc.nec765_result_bytes[1] = fdc.pcn[fdc.drive];
	
				/* return result */
				nec765_setup_result_phase(2);
			}
			else
			{
				/* no int */
				nec765_setup_invalid();
			}
			break;
	
			case 0x06:  /* read data */
			{
	
				nec765_setup_drive_and_side();
	
				fdc.nec765_status[0] = fdc.drive | (fdc.side<<2);
				fdc.nec765_status[1] = 0;
				fdc.nec765_status[2] = 0;
	
	
				nec765_read_data();
			}
			break;
	
			/* read deleted data */
			case 0x0c:
			{
	
				nec765_setup_drive_and_side();
	
	            fdc.nec765_status[0] = fdc.drive | (fdc.side<<2);
				fdc.nec765_status[1] = 0;
				fdc.nec765_status[2] = 0;
	
	
				/* .. for now */
				nec765_read_data();
			}
			break;
	
			/* write deleted data */
			case 0x09:
			{
				nec765_setup_drive_and_side();
	
				fdc.nec765_status[0] = fdc.drive | (fdc.side<<2);
				fdc.nec765_status[1] = 0;
				fdc.nec765_status[2] = 0;
	
				/* ... for now */
				nec765_write_data();
			}
			break;
	
			/* read a track */
			case 0x02:
			{
				chrn_id id = new chrn_id();
	
				nec765_setup_drive_and_side();
	
				fdc.nec765_status[0] = fdc.drive | (fdc.side<<2);
				fdc.nec765_status[1] = 0;
				fdc.nec765_status[2] = 0;
	
				fdc.nec765_status[0] |= NEC765_ST1_NO_DATA;
	
				/* wait for index */
				do
				{
					/* get next id from disc */
					floppy_drive_get_next_id(fdc.drive, fdc.side,id);
				}
				while ((floppy_drive_get_flag_state(fdc.drive, FLOPPY_DRIVE_INDEX))==0);
	
	
				fdc.sector_counter = 0;
	
				nec765_read_a_track();
				}
				break;
	
			case 0x05:  /* write data */
			{
				nec765_setup_drive_and_side();
	
				fdc.nec765_status[0] = fdc.drive | (fdc.side<<2);
				fdc.nec765_status[1] = 0;
				fdc.nec765_status[2] = 0;
	
				nec765_write_data();
			}
			break;
	
			/* format a track */
			case 0x0d:
			{
					nec765_setup_drive_and_side();
	
					fdc.nec765_status[0] = fdc.drive | (fdc.side<<2);
					fdc.nec765_status[1] = 0;
					fdc.nec765_status[2] = 0;
	
					fdc.sector_counter = 0;
	
					nec765_format_track();
			}
			break;
	
			/* invalid */
	        default:
			{	
				switch (fdc.version)
				{
					case NEC765A:
					{
						nec765_setup_invalid();
					}
					break;
	
					case NEC765B:
					{
						/* from nec765b data sheet */
						if ((fdc.nec765_command_bytes[0] & 0x01f)==0x010)
						{
							/* version */
							fdc.nec765_status[0] = 0x090;
							fdc.nec765_result_bytes[0] = fdc.nec765_status[0];
							nec765_setup_result_phase(1);
						}
					}			
					break;
	
					case SMC37C78:
					{
						/* TO BE COMPLETED!!! !*/
						switch (fdc.nec765_command_bytes[0] & 0x01f)
						{
							/* version */
							case 0x010:
							{
								fdc.nec765_status[0] = 0x090;
								fdc.nec765_result_bytes[0] = fdc.nec765_status[0];
								nec765_setup_result_phase(1);
							}
							break;
	
							/* configure */
							case 0x013:
							{
							
							}
							break;
	
							/* dump reg */
							case 0x0e:
							{
								fdc.nec765_result_bytes[0] = fdc.pcn[0];
								fdc.nec765_result_bytes[1] = fdc.pcn[1];
								fdc.nec765_result_bytes[2] = fdc.pcn[2];
								fdc.nec765_result_bytes[3] = fdc.pcn[3];
								
								nec765_setup_result_phase(10);
	
							}
							break;
	
	
							/* perpendicular mode */
							case 0x012:
							{
								nec765_idle();
							}
							break;
	
							/* lock */
							case 0x014:
							{
								nec765_setup_result_phase(1);
							}
							break;
	
				
						}
					}
	
	
	
				}
	        }
	        break;
			}
	}
	
	
	/* dma acknowledge write */
	public static WriteHandlerPtr nec765_dack_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                /* clear request */
		nec765_set_dma_drq(0);
		/* write data */
		nec765_data_w.handler(offset, data);
            }
        };
	
	public static ReadHandlerPtr nec765_dack_r = new ReadHandlerPtr() {
            public int handler(int offset) {
                /* clear data request */
		nec765_set_dma_drq(0);
		/* read data */
		return nec765_data_r.handler(offset);	
            }
        };
	
	public static void nec765_reset(int offset)
	{
		/* nec765 in idle state - ready to accept commands */
		nec765_idle();
	
		/* set int low */
		nec765_set_int(0);
		/* set dma drq output */
		nec765_set_dma_drq(0);
	
		/* tandy 100hx assumes that after NEC is reset, it is in DMA mode */
		fdc.nec765_flags |= NEC765_DMA_MODE;
	
		/* if ready input is set during reset generate an int */
		if ((fdc.nec765_flags & NEC765_FDD_READY) != 0)
		{
			int i;
			int a_drive_is_ready;
	
			fdc.nec765_status[0] = 0x080 | 0x040;
		
			/* for the purpose of pc-xt. If any of the drives have a disk inserted,
			do not set not-ready - need to check with pc_fdc_hw.c whether all drives
			are checked or only the drive selected with the drive select bits?? */
	
			a_drive_is_ready = 0;
			for (i=0; i<4; i++)
			{
				if (floppy_drive_get_flag_state(i, FLOPPY_DRIVE_DISK_INSERTED) != 0)
				{
					a_drive_is_ready = 1;
					break;
				}
	
			}
	
			if (a_drive_is_ready == 0)
			{
				fdc.nec765_status[0] |= 0x08;
			}
	
			nec765_set_int(1);	
		}
	}
	
	static void nec765_set_reset_state(int state)
	{
		int flags;
	
		/* get previous reset state */
		flags = fdc.nec765_flags;
	
		/* set new reset state */
		/* clear reset */
		fdc.nec765_flags &= ~NEC765_RESET;
	
		/* reset */
		if (state != 0)
		{
			fdc.nec765_flags |= NEC765_RESET;
	
			nec765_set_int(0);
		}
	
		/* reset changed state? */
		if (((flags^fdc.nec765_flags) & NEC765_RESET)!=0)
		{
			/* yes */
	
			/* no longer reset */
			if ((fdc.nec765_flags & NEC765_RESET)==0)
			{
				/* reset nec */
				nec765_reset(0);
			}
		}
	}
	
	
	static void nec765_set_ready_state(int state)
	{
		/* clear ready state */
		fdc.nec765_flags &= ~NEC765_FDD_READY;
	
		if (state != 0)
		{
			fdc.nec765_flags |= NEC765_FDD_READY;
		}
	}
}
