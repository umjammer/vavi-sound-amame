/*** t11: Portable DEC T-11 emulator ******************************************

	Copyright (C) Aaron Giles 1998

    Opcode table plus function prototypes

*****************************************************************************/

/*

modes:
  rg = register
  rgd = register deferred
  in = increment
  ind = increment deferred
  de = decrement
  ded = decrement deferred
  ix = index
  ixd = index deferred
  
*/
package mame056.cpu.t11;

/**
 *
 * @author chusogar
 */
public class t11table {
/*TODO*///static void (*opcode_table[65536 >> 3])(void) =
/*TODO*///{
/*TODO*///	/* 0x0000 */
/*TODO*///	op_0000,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	jmp_rgd,	jmp_in,		jmp_ind,	jmp_de,		jmp_ded,	jmp_ix,		jmp_ixd,
/*TODO*///	rts,		illegal,	illegal,	illegal,	ccc,		ccc,		scc,		scc,
/*TODO*///	swab_rg,	swab_rgd,	swab_in,	swab_ind,	swab_de,	swab_ded,	swab_ix,	swab_ixd,
/*TODO*///	/* 0x0100 */
/*TODO*///	br,			br,			br,			br,			br,			br,			br,			br,
/*TODO*///	br,			br,			br,			br,			br,			br,			br,			br,
/*TODO*///	br,			br,			br,			br,			br,			br,			br,			br,
/*TODO*///	br,			br,			br,			br,			br,			br,			br,			br,	
/*TODO*///	/* 0x0200 */
/*TODO*///	bne,		bne,		bne,		bne,		bne,		bne,		bne,		bne,
/*TODO*///	bne,		bne,		bne,		bne,		bne,		bne,		bne,		bne,
/*TODO*///	bne,		bne,		bne,		bne,		bne,		bne,		bne,		bne,
/*TODO*///	bne,		bne,		bne,		bne,		bne,		bne,		bne,		bne,	
/*TODO*///	/* 0x0300 */
/*TODO*///	beq,		beq,		beq,		beq,		beq,		beq,		beq,		beq,
/*TODO*///	beq,		beq,		beq,		beq,		beq,		beq,		beq,		beq,
/*TODO*///	beq,		beq,		beq,		beq,		beq,		beq,		beq,		beq,
/*TODO*///	beq,		beq,		beq,		beq,		beq,		beq,		beq,		beq,	
/*TODO*///	/* 0x0400 */
/*TODO*///	bge,		bge,		bge,		bge,		bge,		bge,		bge,		bge,
/*TODO*///	bge,		bge,		bge,		bge,		bge,		bge,		bge,		bge,
/*TODO*///	bge,		bge,		bge,		bge,		bge,		bge,		bge,		bge,
/*TODO*///	bge,		bge,		bge,		bge,		bge,		bge,		bge,		bge,	
/*TODO*///	/* 0x0500 */
/*TODO*///	blt,		blt,		blt,		blt,		blt,		blt,		blt,		blt,
/*TODO*///	blt,		blt,		blt,		blt,		blt,		blt,		blt,		blt,
/*TODO*///	blt,		blt,		blt,		blt,		blt,		blt,		blt,		blt,
/*TODO*///	blt,		blt,		blt,		blt,		blt,		blt,		blt,		blt,	
/*TODO*///	/* 0x0600 */
/*TODO*///	bgt,		bgt,		bgt,		bgt,		bgt,		bgt,		bgt,		bgt,
/*TODO*///	bgt,		bgt,		bgt,		bgt,		bgt,		bgt,		bgt,		bgt,
/*TODO*///	bgt,		bgt,		bgt,		bgt,		bgt,		bgt,		bgt,		bgt,
/*TODO*///	bgt,		bgt,		bgt,		bgt,		bgt,		bgt,		bgt,		bgt,	
/*TODO*///	/* 0x0700 */
/*TODO*///	ble,		ble,		ble,		ble,		ble,		ble,		ble,		ble,
/*TODO*///	ble,		ble,		ble,		ble,		ble,		ble,		ble,		ble,
/*TODO*///	ble,		ble,		ble,		ble,		ble,		ble,		ble,		ble,
/*TODO*///	ble,		ble,		ble,		ble,		ble,		ble,		ble,		ble,	
/*TODO*///	/* 0x0800 */
/*TODO*///	illegal,	jsr_rgd,	jsr_in,		jsr_ind,	jsr_de,		jsr_ded,	jsr_ix,		jsr_ixd,
/*TODO*///	illegal,	jsr_rgd,	jsr_in,		jsr_ind,	jsr_de,		jsr_ded,	jsr_ix,		jsr_ixd,
/*TODO*///	illegal,	jsr_rgd,	jsr_in,		jsr_ind,	jsr_de,		jsr_ded,	jsr_ix,		jsr_ixd,
/*TODO*///	illegal,	jsr_rgd,	jsr_in,		jsr_ind,	jsr_de,		jsr_ded,	jsr_ix,		jsr_ixd,
/*TODO*///	/* 0x0900 */
/*TODO*///	illegal,	jsr_rgd,	jsr_in,		jsr_ind,	jsr_de,		jsr_ded,	jsr_ix,		jsr_ixd,
/*TODO*///	illegal,	jsr_rgd,	jsr_in,		jsr_ind,	jsr_de,		jsr_ded,	jsr_ix,		jsr_ixd,
/*TODO*///	illegal,	jsr_rgd,	jsr_in,		jsr_ind,	jsr_de,		jsr_ded,	jsr_ix,		jsr_ixd,
/*TODO*///	illegal,	jsr_rgd,	jsr_in,		jsr_ind,	jsr_de,		jsr_ded,	jsr_ix,		jsr_ixd,
/*TODO*///	/* 0x0a00 */
/*TODO*///	clr_rg,		clr_rgd,	clr_in,		clr_ind,	clr_de,		clr_ded,	clr_ix,		clr_ixd,
/*TODO*///	com_rg,		com_rgd,	com_in,		com_ind,	com_de,		com_ded,	com_ix,		com_ixd,
/*TODO*///	inc_rg,		inc_rgd,	inc_in,		inc_ind,	inc_de,		inc_ded,	inc_ix,		inc_ixd,
/*TODO*///	dec_rg,		dec_rgd,	dec_in,		dec_ind,	dec_de,		dec_ded,	dec_ix,		dec_ixd,
/*TODO*///	/* 0x0b00 */
/*TODO*///	neg_rg,		neg_rgd,	neg_in,		neg_ind,	neg_de,		neg_ded,	neg_ix,		neg_ixd,
/*TODO*///	adc_rg,		adc_rgd,	adc_in,		adc_ind,	adc_de,		adc_ded,	adc_ix,		adc_ixd,
/*TODO*///	sbc_rg,		sbc_rgd,	sbc_in,		sbc_ind,	sbc_de,		sbc_ded,	sbc_ix,		sbc_ixd,
/*TODO*///	tst_rg,		tst_rgd,	tst_in,		tst_ind,	tst_de,		tst_ded,	tst_ix,		tst_ixd,
/*TODO*///	/* 0x0c00 */
/*TODO*///	ror_rg,		ror_rgd,	ror_in,		ror_ind,	ror_de,		ror_ded,	ror_ix,		ror_ixd,
/*TODO*///	rol_rg,		rol_rgd,	rol_in,		rol_ind,	rol_de,		rol_ded,	rol_ix,		rol_ixd,
/*TODO*///	asr_rg,		asr_rgd,	asr_in,		asr_ind,	asr_de,		asr_ded,	asr_ix,		asr_ixd,
/*TODO*///	asl_rg,		asl_rgd,	asl_in,		asl_ind,	asl_de,		asl_ded,	asl_ix,		asl_ixd,
/*TODO*///	/* 0x0d00 */
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	sxt_rg,		sxt_rgd,	sxt_in,		sxt_ind,	sxt_de,		sxt_ded,	sxt_ix,		sxt_ixd,
/*TODO*///	/* 0x0e00 */
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	/* 0x0f00 */
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///
/*TODO*///	/* 0x1000 */
/*TODO*///	mov_rg_rg,	mov_rg_rgd,	mov_rg_in,	mov_rg_ind,	mov_rg_de,	mov_rg_ded,	mov_rg_ix,	mov_rg_ixd,
/*TODO*///	mov_rg_rg,	mov_rg_rgd,	mov_rg_in,	mov_rg_ind,	mov_rg_de,	mov_rg_ded,	mov_rg_ix,	mov_rg_ixd,
/*TODO*///	mov_rg_rg,	mov_rg_rgd,	mov_rg_in,	mov_rg_ind,	mov_rg_de,	mov_rg_ded,	mov_rg_ix,	mov_rg_ixd,
/*TODO*///	mov_rg_rg,	mov_rg_rgd,	mov_rg_in,	mov_rg_ind,	mov_rg_de,	mov_rg_ded,	mov_rg_ix,	mov_rg_ixd,
/*TODO*///	/* 0x1100 */
/*TODO*///	mov_rg_rg,	mov_rg_rgd,	mov_rg_in,	mov_rg_ind,	mov_rg_de,	mov_rg_ded,	mov_rg_ix,	mov_rg_ixd,
/*TODO*///	mov_rg_rg,	mov_rg_rgd,	mov_rg_in,	mov_rg_ind,	mov_rg_de,	mov_rg_ded,	mov_rg_ix,	mov_rg_ixd,
/*TODO*///	mov_rg_rg,	mov_rg_rgd,	mov_rg_in,	mov_rg_ind,	mov_rg_de,	mov_rg_ded,	mov_rg_ix,	mov_rg_ixd,
/*TODO*///	mov_rg_rg,	mov_rg_rgd,	mov_rg_in,	mov_rg_ind,	mov_rg_de,	mov_rg_ded,	mov_rg_ix,	mov_rg_ixd,
/*TODO*///	/* 0x1200 */
/*TODO*///	mov_rgd_rg,	mov_rgd_rgd,mov_rgd_in,	mov_rgd_ind,mov_rgd_de,	mov_rgd_ded,mov_rgd_ix,	mov_rgd_ixd,
/*TODO*///	mov_rgd_rg,	mov_rgd_rgd,mov_rgd_in,	mov_rgd_ind,mov_rgd_de,	mov_rgd_ded,mov_rgd_ix,	mov_rgd_ixd,
/*TODO*///	mov_rgd_rg,	mov_rgd_rgd,mov_rgd_in,	mov_rgd_ind,mov_rgd_de,	mov_rgd_ded,mov_rgd_ix,	mov_rgd_ixd,
/*TODO*///	mov_rgd_rg,	mov_rgd_rgd,mov_rgd_in,	mov_rgd_ind,mov_rgd_de,	mov_rgd_ded,mov_rgd_ix,	mov_rgd_ixd,
/*TODO*///	/* 0x1300 */
/*TODO*///	mov_rgd_rg,	mov_rgd_rgd,mov_rgd_in,	mov_rgd_ind,mov_rgd_de,	mov_rgd_ded,mov_rgd_ix,	mov_rgd_ixd,
/*TODO*///	mov_rgd_rg,	mov_rgd_rgd,mov_rgd_in,	mov_rgd_ind,mov_rgd_de,	mov_rgd_ded,mov_rgd_ix,	mov_rgd_ixd,
/*TODO*///	mov_rgd_rg,	mov_rgd_rgd,mov_rgd_in,	mov_rgd_ind,mov_rgd_de,	mov_rgd_ded,mov_rgd_ix,	mov_rgd_ixd,
/*TODO*///	mov_rgd_rg,	mov_rgd_rgd,mov_rgd_in,	mov_rgd_ind,mov_rgd_de,	mov_rgd_ded,mov_rgd_ix,	mov_rgd_ixd,
/*TODO*///	/* 0x1400 */
/*TODO*///	mov_in_rg,	mov_in_rgd,	mov_in_in,	mov_in_ind,	mov_in_de,	mov_in_ded,	mov_in_ix,	mov_in_ixd,
/*TODO*///	mov_in_rg,	mov_in_rgd,	mov_in_in,	mov_in_ind,	mov_in_de,	mov_in_ded,	mov_in_ix,	mov_in_ixd,
/*TODO*///	mov_in_rg,	mov_in_rgd,	mov_in_in,	mov_in_ind,	mov_in_de,	mov_in_ded,	mov_in_ix,	mov_in_ixd,
/*TODO*///	mov_in_rg,	mov_in_rgd,	mov_in_in,	mov_in_ind,	mov_in_de,	mov_in_ded,	mov_in_ix,	mov_in_ixd,
/*TODO*///	/* 0x1500 */
/*TODO*///	mov_in_rg,	mov_in_rgd,	mov_in_in,	mov_in_ind,	mov_in_de,	mov_in_ded,	mov_in_ix,	mov_in_ixd,
/*TODO*///	mov_in_rg,	mov_in_rgd,	mov_in_in,	mov_in_ind,	mov_in_de,	mov_in_ded,	mov_in_ix,	mov_in_ixd,
/*TODO*///	mov_in_rg,	mov_in_rgd,	mov_in_in,	mov_in_ind,	mov_in_de,	mov_in_ded,	mov_in_ix,	mov_in_ixd,
/*TODO*///	mov_in_rg,	mov_in_rgd,	mov_in_in,	mov_in_ind,	mov_in_de,	mov_in_ded,	mov_in_ix,	mov_in_ixd,
/*TODO*///	/* 0x1600 */
/*TODO*///	mov_ind_rg,	mov_ind_rgd,mov_ind_in,	mov_ind_ind,mov_ind_de,	mov_ind_ded,mov_ind_ix,	mov_ind_ixd,
/*TODO*///	mov_ind_rg,	mov_ind_rgd,mov_ind_in,	mov_ind_ind,mov_ind_de,	mov_ind_ded,mov_ind_ix,	mov_ind_ixd,
/*TODO*///	mov_ind_rg,	mov_ind_rgd,mov_ind_in,	mov_ind_ind,mov_ind_de,	mov_ind_ded,mov_ind_ix,	mov_ind_ixd,
/*TODO*///	mov_ind_rg,	mov_ind_rgd,mov_ind_in,	mov_ind_ind,mov_ind_de,	mov_ind_ded,mov_ind_ix,	mov_ind_ixd,
/*TODO*///	/* 0x1700 */
/*TODO*///	mov_ind_rg,	mov_ind_rgd,mov_ind_in,	mov_ind_ind,mov_ind_de,	mov_ind_ded,mov_ind_ix,	mov_ind_ixd,
/*TODO*///	mov_ind_rg,	mov_ind_rgd,mov_ind_in,	mov_ind_ind,mov_ind_de,	mov_ind_ded,mov_ind_ix,	mov_ind_ixd,
/*TODO*///	mov_ind_rg,	mov_ind_rgd,mov_ind_in,	mov_ind_ind,mov_ind_de,	mov_ind_ded,mov_ind_ix,	mov_ind_ixd,
/*TODO*///	mov_ind_rg,	mov_ind_rgd,mov_ind_in,	mov_ind_ind,mov_ind_de,	mov_ind_ded,mov_ind_ix,	mov_ind_ixd,
/*TODO*///	/* 0x1800 */
/*TODO*///	mov_de_rg,	mov_de_rgd,	mov_de_in,	mov_de_ind,	mov_de_de,	mov_de_ded,	mov_de_ix,	mov_de_ixd,
/*TODO*///	mov_de_rg,	mov_de_rgd,	mov_de_in,	mov_de_ind,	mov_de_de,	mov_de_ded,	mov_de_ix,	mov_de_ixd,
/*TODO*///	mov_de_rg,	mov_de_rgd,	mov_de_in,	mov_de_ind,	mov_de_de,	mov_de_ded,	mov_de_ix,	mov_de_ixd,
/*TODO*///	mov_de_rg,	mov_de_rgd,	mov_de_in,	mov_de_ind,	mov_de_de,	mov_de_ded,	mov_de_ix,	mov_de_ixd,
/*TODO*///	/* 0x1900 */
/*TODO*///	mov_de_rg,	mov_de_rgd,	mov_de_in,	mov_de_ind,	mov_de_de,	mov_de_ded,	mov_de_ix,	mov_de_ixd,
/*TODO*///	mov_de_rg,	mov_de_rgd,	mov_de_in,	mov_de_ind,	mov_de_de,	mov_de_ded,	mov_de_ix,	mov_de_ixd,
/*TODO*///	mov_de_rg,	mov_de_rgd,	mov_de_in,	mov_de_ind,	mov_de_de,	mov_de_ded,	mov_de_ix,	mov_de_ixd,
/*TODO*///	mov_de_rg,	mov_de_rgd,	mov_de_in,	mov_de_ind,	mov_de_de,	mov_de_ded,	mov_de_ix,	mov_de_ixd,
/*TODO*///	/* 0x1a00 */
/*TODO*///	mov_ded_rg,	mov_ded_rgd,mov_ded_in,	mov_ded_ind,mov_ded_de,	mov_ded_ded,mov_ded_ix,	mov_ded_ixd,
/*TODO*///	mov_ded_rg,	mov_ded_rgd,mov_ded_in,	mov_ded_ind,mov_ded_de,	mov_ded_ded,mov_ded_ix,	mov_ded_ixd,
/*TODO*///	mov_ded_rg,	mov_ded_rgd,mov_ded_in,	mov_ded_ind,mov_ded_de,	mov_ded_ded,mov_ded_ix,	mov_ded_ixd,
/*TODO*///	mov_ded_rg,	mov_ded_rgd,mov_ded_in,	mov_ded_ind,mov_ded_de,	mov_ded_ded,mov_ded_ix,	mov_ded_ixd,
/*TODO*///	/* 0x1b00 */
/*TODO*///	mov_ded_rg,	mov_ded_rgd,mov_ded_in,	mov_ded_ind,mov_ded_de,	mov_ded_ded,mov_ded_ix,	mov_ded_ixd,
/*TODO*///	mov_ded_rg,	mov_ded_rgd,mov_ded_in,	mov_ded_ind,mov_ded_de,	mov_ded_ded,mov_ded_ix,	mov_ded_ixd,
/*TODO*///	mov_ded_rg,	mov_ded_rgd,mov_ded_in,	mov_ded_ind,mov_ded_de,	mov_ded_ded,mov_ded_ix,	mov_ded_ixd,
/*TODO*///	mov_ded_rg,	mov_ded_rgd,mov_ded_in,	mov_ded_ind,mov_ded_de,	mov_ded_ded,mov_ded_ix,	mov_ded_ixd,
/*TODO*///	/* 0x1c00 */
/*TODO*///	mov_ix_rg,	mov_ix_rgd,	mov_ix_in,	mov_ix_ind,	mov_ix_de,	mov_ix_ded,	mov_ix_ix,	mov_ix_ixd,
/*TODO*///	mov_ix_rg,	mov_ix_rgd,	mov_ix_in,	mov_ix_ind,	mov_ix_de,	mov_ix_ded,	mov_ix_ix,	mov_ix_ixd,
/*TODO*///	mov_ix_rg,	mov_ix_rgd,	mov_ix_in,	mov_ix_ind,	mov_ix_de,	mov_ix_ded,	mov_ix_ix,	mov_ix_ixd,
/*TODO*///	mov_ix_rg,	mov_ix_rgd,	mov_ix_in,	mov_ix_ind,	mov_ix_de,	mov_ix_ded,	mov_ix_ix,	mov_ix_ixd,
/*TODO*///	/* 0x1d00 */
/*TODO*///	mov_ix_rg,	mov_ix_rgd,	mov_ix_in,	mov_ix_ind,	mov_ix_de,	mov_ix_ded,	mov_ix_ix,	mov_ix_ixd,
/*TODO*///	mov_ix_rg,	mov_ix_rgd,	mov_ix_in,	mov_ix_ind,	mov_ix_de,	mov_ix_ded,	mov_ix_ix,	mov_ix_ixd,
/*TODO*///	mov_ix_rg,	mov_ix_rgd,	mov_ix_in,	mov_ix_ind,	mov_ix_de,	mov_ix_ded,	mov_ix_ix,	mov_ix_ixd,
/*TODO*///	mov_ix_rg,	mov_ix_rgd,	mov_ix_in,	mov_ix_ind,	mov_ix_de,	mov_ix_ded,	mov_ix_ix,	mov_ix_ixd,
/*TODO*///	/* 0x1e00 */
/*TODO*///	mov_ixd_rg,	mov_ixd_rgd,mov_ixd_in,	mov_ixd_ind,mov_ixd_de,	mov_ixd_ded,mov_ixd_ix,	mov_ixd_ixd,
/*TODO*///	mov_ixd_rg,	mov_ixd_rgd,mov_ixd_in,	mov_ixd_ind,mov_ixd_de,	mov_ixd_ded,mov_ixd_ix,	mov_ixd_ixd,
/*TODO*///	mov_ixd_rg,	mov_ixd_rgd,mov_ixd_in,	mov_ixd_ind,mov_ixd_de,	mov_ixd_ded,mov_ixd_ix,	mov_ixd_ixd,
/*TODO*///	mov_ixd_rg,	mov_ixd_rgd,mov_ixd_in,	mov_ixd_ind,mov_ixd_de,	mov_ixd_ded,mov_ixd_ix,	mov_ixd_ixd,
/*TODO*///	/* 0x1f00 */
/*TODO*///	mov_ixd_rg,	mov_ixd_rgd,mov_ixd_in,	mov_ixd_ind,mov_ixd_de,	mov_ixd_ded,mov_ixd_ix,	mov_ixd_ixd,
/*TODO*///	mov_ixd_rg,	mov_ixd_rgd,mov_ixd_in,	mov_ixd_ind,mov_ixd_de,	mov_ixd_ded,mov_ixd_ix,	mov_ixd_ixd,
/*TODO*///	mov_ixd_rg,	mov_ixd_rgd,mov_ixd_in,	mov_ixd_ind,mov_ixd_de,	mov_ixd_ded,mov_ixd_ix,	mov_ixd_ixd,
/*TODO*///	mov_ixd_rg,	mov_ixd_rgd,mov_ixd_in,	mov_ixd_ind,mov_ixd_de,	mov_ixd_ded,mov_ixd_ix,	mov_ixd_ixd,
/*TODO*///
/*TODO*///	/* 0x2000 */
/*TODO*///	cmp_rg_rg,	cmp_rg_rgd,	cmp_rg_in,	cmp_rg_ind,	cmp_rg_de,	cmp_rg_ded,	cmp_rg_ix,	cmp_rg_ixd,
/*TODO*///	cmp_rg_rg,	cmp_rg_rgd,	cmp_rg_in,	cmp_rg_ind,	cmp_rg_de,	cmp_rg_ded,	cmp_rg_ix,	cmp_rg_ixd,
/*TODO*///	cmp_rg_rg,	cmp_rg_rgd,	cmp_rg_in,	cmp_rg_ind,	cmp_rg_de,	cmp_rg_ded,	cmp_rg_ix,	cmp_rg_ixd,
/*TODO*///	cmp_rg_rg,	cmp_rg_rgd,	cmp_rg_in,	cmp_rg_ind,	cmp_rg_de,	cmp_rg_ded,	cmp_rg_ix,	cmp_rg_ixd,
/*TODO*///	/* 0x2100 */
/*TODO*///	cmp_rg_rg,	cmp_rg_rgd,	cmp_rg_in,	cmp_rg_ind,	cmp_rg_de,	cmp_rg_ded,	cmp_rg_ix,	cmp_rg_ixd,
/*TODO*///	cmp_rg_rg,	cmp_rg_rgd,	cmp_rg_in,	cmp_rg_ind,	cmp_rg_de,	cmp_rg_ded,	cmp_rg_ix,	cmp_rg_ixd,
/*TODO*///	cmp_rg_rg,	cmp_rg_rgd,	cmp_rg_in,	cmp_rg_ind,	cmp_rg_de,	cmp_rg_ded,	cmp_rg_ix,	cmp_rg_ixd,
/*TODO*///	cmp_rg_rg,	cmp_rg_rgd,	cmp_rg_in,	cmp_rg_ind,	cmp_rg_de,	cmp_rg_ded,	cmp_rg_ix,	cmp_rg_ixd,
/*TODO*///	/* 0x2200 */
/*TODO*///	cmp_rgd_rg,	cmp_rgd_rgd,cmp_rgd_in,	cmp_rgd_ind,cmp_rgd_de,	cmp_rgd_ded,cmp_rgd_ix,	cmp_rgd_ixd,
/*TODO*///	cmp_rgd_rg,	cmp_rgd_rgd,cmp_rgd_in,	cmp_rgd_ind,cmp_rgd_de,	cmp_rgd_ded,cmp_rgd_ix,	cmp_rgd_ixd,
/*TODO*///	cmp_rgd_rg,	cmp_rgd_rgd,cmp_rgd_in,	cmp_rgd_ind,cmp_rgd_de,	cmp_rgd_ded,cmp_rgd_ix,	cmp_rgd_ixd,
/*TODO*///	cmp_rgd_rg,	cmp_rgd_rgd,cmp_rgd_in,	cmp_rgd_ind,cmp_rgd_de,	cmp_rgd_ded,cmp_rgd_ix,	cmp_rgd_ixd,
/*TODO*///	/* 0x2300 */
/*TODO*///	cmp_rgd_rg,	cmp_rgd_rgd,cmp_rgd_in,	cmp_rgd_ind,cmp_rgd_de,	cmp_rgd_ded,cmp_rgd_ix,	cmp_rgd_ixd,
/*TODO*///	cmp_rgd_rg,	cmp_rgd_rgd,cmp_rgd_in,	cmp_rgd_ind,cmp_rgd_de,	cmp_rgd_ded,cmp_rgd_ix,	cmp_rgd_ixd,
/*TODO*///	cmp_rgd_rg,	cmp_rgd_rgd,cmp_rgd_in,	cmp_rgd_ind,cmp_rgd_de,	cmp_rgd_ded,cmp_rgd_ix,	cmp_rgd_ixd,
/*TODO*///	cmp_rgd_rg,	cmp_rgd_rgd,cmp_rgd_in,	cmp_rgd_ind,cmp_rgd_de,	cmp_rgd_ded,cmp_rgd_ix,	cmp_rgd_ixd,
/*TODO*///	/* 0x2400 */
/*TODO*///	cmp_in_rg,	cmp_in_rgd,	cmp_in_in,	cmp_in_ind,	cmp_in_de,	cmp_in_ded,	cmp_in_ix,	cmp_in_ixd,
/*TODO*///	cmp_in_rg,	cmp_in_rgd,	cmp_in_in,	cmp_in_ind,	cmp_in_de,	cmp_in_ded,	cmp_in_ix,	cmp_in_ixd,
/*TODO*///	cmp_in_rg,	cmp_in_rgd,	cmp_in_in,	cmp_in_ind,	cmp_in_de,	cmp_in_ded,	cmp_in_ix,	cmp_in_ixd,
/*TODO*///	cmp_in_rg,	cmp_in_rgd,	cmp_in_in,	cmp_in_ind,	cmp_in_de,	cmp_in_ded,	cmp_in_ix,	cmp_in_ixd,
/*TODO*///	/* 0x2500 */
/*TODO*///	cmp_in_rg,	cmp_in_rgd,	cmp_in_in,	cmp_in_ind,	cmp_in_de,	cmp_in_ded,	cmp_in_ix,	cmp_in_ixd,
/*TODO*///	cmp_in_rg,	cmp_in_rgd,	cmp_in_in,	cmp_in_ind,	cmp_in_de,	cmp_in_ded,	cmp_in_ix,	cmp_in_ixd,
/*TODO*///	cmp_in_rg,	cmp_in_rgd,	cmp_in_in,	cmp_in_ind,	cmp_in_de,	cmp_in_ded,	cmp_in_ix,	cmp_in_ixd,
/*TODO*///	cmp_in_rg,	cmp_in_rgd,	cmp_in_in,	cmp_in_ind,	cmp_in_de,	cmp_in_ded,	cmp_in_ix,	cmp_in_ixd,
/*TODO*///	/* 0x2600 */
/*TODO*///	cmp_ind_rg,	cmp_ind_rgd,cmp_ind_in,	cmp_ind_ind,cmp_ind_de,	cmp_ind_ded,cmp_ind_ix,	cmp_ind_ixd,
/*TODO*///	cmp_ind_rg,	cmp_ind_rgd,cmp_ind_in,	cmp_ind_ind,cmp_ind_de,	cmp_ind_ded,cmp_ind_ix,	cmp_ind_ixd,
/*TODO*///	cmp_ind_rg,	cmp_ind_rgd,cmp_ind_in,	cmp_ind_ind,cmp_ind_de,	cmp_ind_ded,cmp_ind_ix,	cmp_ind_ixd,
/*TODO*///	cmp_ind_rg,	cmp_ind_rgd,cmp_ind_in,	cmp_ind_ind,cmp_ind_de,	cmp_ind_ded,cmp_ind_ix,	cmp_ind_ixd,
/*TODO*///	/* 0x2700 */
/*TODO*///	cmp_ind_rg,	cmp_ind_rgd,cmp_ind_in,	cmp_ind_ind,cmp_ind_de,	cmp_ind_ded,cmp_ind_ix,	cmp_ind_ixd,
/*TODO*///	cmp_ind_rg,	cmp_ind_rgd,cmp_ind_in,	cmp_ind_ind,cmp_ind_de,	cmp_ind_ded,cmp_ind_ix,	cmp_ind_ixd,
/*TODO*///	cmp_ind_rg,	cmp_ind_rgd,cmp_ind_in,	cmp_ind_ind,cmp_ind_de,	cmp_ind_ded,cmp_ind_ix,	cmp_ind_ixd,
/*TODO*///	cmp_ind_rg,	cmp_ind_rgd,cmp_ind_in,	cmp_ind_ind,cmp_ind_de,	cmp_ind_ded,cmp_ind_ix,	cmp_ind_ixd,
/*TODO*///	/* 0x2800 */
/*TODO*///	cmp_de_rg,	cmp_de_rgd,	cmp_de_in,	cmp_de_ind,	cmp_de_de,	cmp_de_ded,	cmp_de_ix,	cmp_de_ixd,
/*TODO*///	cmp_de_rg,	cmp_de_rgd,	cmp_de_in,	cmp_de_ind,	cmp_de_de,	cmp_de_ded,	cmp_de_ix,	cmp_de_ixd,
/*TODO*///	cmp_de_rg,	cmp_de_rgd,	cmp_de_in,	cmp_de_ind,	cmp_de_de,	cmp_de_ded,	cmp_de_ix,	cmp_de_ixd,
/*TODO*///	cmp_de_rg,	cmp_de_rgd,	cmp_de_in,	cmp_de_ind,	cmp_de_de,	cmp_de_ded,	cmp_de_ix,	cmp_de_ixd,
/*TODO*///	/* 0x2900 */
/*TODO*///	cmp_de_rg,	cmp_de_rgd,	cmp_de_in,	cmp_de_ind,	cmp_de_de,	cmp_de_ded,	cmp_de_ix,	cmp_de_ixd,
/*TODO*///	cmp_de_rg,	cmp_de_rgd,	cmp_de_in,	cmp_de_ind,	cmp_de_de,	cmp_de_ded,	cmp_de_ix,	cmp_de_ixd,
/*TODO*///	cmp_de_rg,	cmp_de_rgd,	cmp_de_in,	cmp_de_ind,	cmp_de_de,	cmp_de_ded,	cmp_de_ix,	cmp_de_ixd,
/*TODO*///	cmp_de_rg,	cmp_de_rgd,	cmp_de_in,	cmp_de_ind,	cmp_de_de,	cmp_de_ded,	cmp_de_ix,	cmp_de_ixd,
/*TODO*///	/* 0x2a00 */
/*TODO*///	cmp_ded_rg,	cmp_ded_rgd,cmp_ded_in,	cmp_ded_ind,cmp_ded_de,	cmp_ded_ded,cmp_ded_ix,	cmp_ded_ixd,
/*TODO*///	cmp_ded_rg,	cmp_ded_rgd,cmp_ded_in,	cmp_ded_ind,cmp_ded_de,	cmp_ded_ded,cmp_ded_ix,	cmp_ded_ixd,
/*TODO*///	cmp_ded_rg,	cmp_ded_rgd,cmp_ded_in,	cmp_ded_ind,cmp_ded_de,	cmp_ded_ded,cmp_ded_ix,	cmp_ded_ixd,
/*TODO*///	cmp_ded_rg,	cmp_ded_rgd,cmp_ded_in,	cmp_ded_ind,cmp_ded_de,	cmp_ded_ded,cmp_ded_ix,	cmp_ded_ixd,
/*TODO*///	/* 0x2b00 */
/*TODO*///	cmp_ded_rg,	cmp_ded_rgd,cmp_ded_in,	cmp_ded_ind,cmp_ded_de,	cmp_ded_ded,cmp_ded_ix,	cmp_ded_ixd,
/*TODO*///	cmp_ded_rg,	cmp_ded_rgd,cmp_ded_in,	cmp_ded_ind,cmp_ded_de,	cmp_ded_ded,cmp_ded_ix,	cmp_ded_ixd,
/*TODO*///	cmp_ded_rg,	cmp_ded_rgd,cmp_ded_in,	cmp_ded_ind,cmp_ded_de,	cmp_ded_ded,cmp_ded_ix,	cmp_ded_ixd,
/*TODO*///	cmp_ded_rg,	cmp_ded_rgd,cmp_ded_in,	cmp_ded_ind,cmp_ded_de,	cmp_ded_ded,cmp_ded_ix,	cmp_ded_ixd,
/*TODO*///	/* 0x2c00 */
/*TODO*///	cmp_ix_rg,	cmp_ix_rgd,	cmp_ix_in,	cmp_ix_ind,	cmp_ix_de,	cmp_ix_ded,	cmp_ix_ix,	cmp_ix_ixd,
/*TODO*///	cmp_ix_rg,	cmp_ix_rgd,	cmp_ix_in,	cmp_ix_ind,	cmp_ix_de,	cmp_ix_ded,	cmp_ix_ix,	cmp_ix_ixd,
/*TODO*///	cmp_ix_rg,	cmp_ix_rgd,	cmp_ix_in,	cmp_ix_ind,	cmp_ix_de,	cmp_ix_ded,	cmp_ix_ix,	cmp_ix_ixd,
/*TODO*///	cmp_ix_rg,	cmp_ix_rgd,	cmp_ix_in,	cmp_ix_ind,	cmp_ix_de,	cmp_ix_ded,	cmp_ix_ix,	cmp_ix_ixd,
/*TODO*///	/* 0x2d00 */
/*TODO*///	cmp_ix_rg,	cmp_ix_rgd,	cmp_ix_in,	cmp_ix_ind,	cmp_ix_de,	cmp_ix_ded,	cmp_ix_ix,	cmp_ix_ixd,
/*TODO*///	cmp_ix_rg,	cmp_ix_rgd,	cmp_ix_in,	cmp_ix_ind,	cmp_ix_de,	cmp_ix_ded,	cmp_ix_ix,	cmp_ix_ixd,
/*TODO*///	cmp_ix_rg,	cmp_ix_rgd,	cmp_ix_in,	cmp_ix_ind,	cmp_ix_de,	cmp_ix_ded,	cmp_ix_ix,	cmp_ix_ixd,
/*TODO*///	cmp_ix_rg,	cmp_ix_rgd,	cmp_ix_in,	cmp_ix_ind,	cmp_ix_de,	cmp_ix_ded,	cmp_ix_ix,	cmp_ix_ixd,
/*TODO*///	/* 0x2e00 */
/*TODO*///	cmp_ixd_rg,	cmp_ixd_rgd,cmp_ixd_in,	cmp_ixd_ind,cmp_ixd_de,	cmp_ixd_ded,cmp_ixd_ix,	cmp_ixd_ixd,
/*TODO*///	cmp_ixd_rg,	cmp_ixd_rgd,cmp_ixd_in,	cmp_ixd_ind,cmp_ixd_de,	cmp_ixd_ded,cmp_ixd_ix,	cmp_ixd_ixd,
/*TODO*///	cmp_ixd_rg,	cmp_ixd_rgd,cmp_ixd_in,	cmp_ixd_ind,cmp_ixd_de,	cmp_ixd_ded,cmp_ixd_ix,	cmp_ixd_ixd,
/*TODO*///	cmp_ixd_rg,	cmp_ixd_rgd,cmp_ixd_in,	cmp_ixd_ind,cmp_ixd_de,	cmp_ixd_ded,cmp_ixd_ix,	cmp_ixd_ixd,
/*TODO*///	/* 0x2f00 */
/*TODO*///	cmp_ixd_rg,	cmp_ixd_rgd,cmp_ixd_in,	cmp_ixd_ind,cmp_ixd_de,	cmp_ixd_ded,cmp_ixd_ix,	cmp_ixd_ixd,
/*TODO*///	cmp_ixd_rg,	cmp_ixd_rgd,cmp_ixd_in,	cmp_ixd_ind,cmp_ixd_de,	cmp_ixd_ded,cmp_ixd_ix,	cmp_ixd_ixd,
/*TODO*///	cmp_ixd_rg,	cmp_ixd_rgd,cmp_ixd_in,	cmp_ixd_ind,cmp_ixd_de,	cmp_ixd_ded,cmp_ixd_ix,	cmp_ixd_ixd,
/*TODO*///	cmp_ixd_rg,	cmp_ixd_rgd,cmp_ixd_in,	cmp_ixd_ind,cmp_ixd_de,	cmp_ixd_ded,cmp_ixd_ix,	cmp_ixd_ixd,
/*TODO*///
/*TODO*///	/* 0x3000 */
/*TODO*///	bit_rg_rg,	bit_rg_rgd,	bit_rg_in,	bit_rg_ind,	bit_rg_de,	bit_rg_ded,	bit_rg_ix,	bit_rg_ixd,
/*TODO*///	bit_rg_rg,	bit_rg_rgd,	bit_rg_in,	bit_rg_ind,	bit_rg_de,	bit_rg_ded,	bit_rg_ix,	bit_rg_ixd,
/*TODO*///	bit_rg_rg,	bit_rg_rgd,	bit_rg_in,	bit_rg_ind,	bit_rg_de,	bit_rg_ded,	bit_rg_ix,	bit_rg_ixd,
/*TODO*///	bit_rg_rg,	bit_rg_rgd,	bit_rg_in,	bit_rg_ind,	bit_rg_de,	bit_rg_ded,	bit_rg_ix,	bit_rg_ixd,
/*TODO*///	/* 0x3100 */
/*TODO*///	bit_rg_rg,	bit_rg_rgd,	bit_rg_in,	bit_rg_ind,	bit_rg_de,	bit_rg_ded,	bit_rg_ix,	bit_rg_ixd,
/*TODO*///	bit_rg_rg,	bit_rg_rgd,	bit_rg_in,	bit_rg_ind,	bit_rg_de,	bit_rg_ded,	bit_rg_ix,	bit_rg_ixd,
/*TODO*///	bit_rg_rg,	bit_rg_rgd,	bit_rg_in,	bit_rg_ind,	bit_rg_de,	bit_rg_ded,	bit_rg_ix,	bit_rg_ixd,
/*TODO*///	bit_rg_rg,	bit_rg_rgd,	bit_rg_in,	bit_rg_ind,	bit_rg_de,	bit_rg_ded,	bit_rg_ix,	bit_rg_ixd,
/*TODO*///	/* 0x3200 */
/*TODO*///	bit_rgd_rg,	bit_rgd_rgd,bit_rgd_in,	bit_rgd_ind,bit_rgd_de,	bit_rgd_ded,bit_rgd_ix,	bit_rgd_ixd,
/*TODO*///	bit_rgd_rg,	bit_rgd_rgd,bit_rgd_in,	bit_rgd_ind,bit_rgd_de,	bit_rgd_ded,bit_rgd_ix,	bit_rgd_ixd,
/*TODO*///	bit_rgd_rg,	bit_rgd_rgd,bit_rgd_in,	bit_rgd_ind,bit_rgd_de,	bit_rgd_ded,bit_rgd_ix,	bit_rgd_ixd,
/*TODO*///	bit_rgd_rg,	bit_rgd_rgd,bit_rgd_in,	bit_rgd_ind,bit_rgd_de,	bit_rgd_ded,bit_rgd_ix,	bit_rgd_ixd,
/*TODO*///	/* 0x3300 */
/*TODO*///	bit_rgd_rg,	bit_rgd_rgd,bit_rgd_in,	bit_rgd_ind,bit_rgd_de,	bit_rgd_ded,bit_rgd_ix,	bit_rgd_ixd,
/*TODO*///	bit_rgd_rg,	bit_rgd_rgd,bit_rgd_in,	bit_rgd_ind,bit_rgd_de,	bit_rgd_ded,bit_rgd_ix,	bit_rgd_ixd,
/*TODO*///	bit_rgd_rg,	bit_rgd_rgd,bit_rgd_in,	bit_rgd_ind,bit_rgd_de,	bit_rgd_ded,bit_rgd_ix,	bit_rgd_ixd,
/*TODO*///	bit_rgd_rg,	bit_rgd_rgd,bit_rgd_in,	bit_rgd_ind,bit_rgd_de,	bit_rgd_ded,bit_rgd_ix,	bit_rgd_ixd,
/*TODO*///	/* 0x3400 */
/*TODO*///	bit_in_rg,	bit_in_rgd,	bit_in_in,	bit_in_ind,	bit_in_de,	bit_in_ded,	bit_in_ix,	bit_in_ixd,
/*TODO*///	bit_in_rg,	bit_in_rgd,	bit_in_in,	bit_in_ind,	bit_in_de,	bit_in_ded,	bit_in_ix,	bit_in_ixd,
/*TODO*///	bit_in_rg,	bit_in_rgd,	bit_in_in,	bit_in_ind,	bit_in_de,	bit_in_ded,	bit_in_ix,	bit_in_ixd,
/*TODO*///	bit_in_rg,	bit_in_rgd,	bit_in_in,	bit_in_ind,	bit_in_de,	bit_in_ded,	bit_in_ix,	bit_in_ixd,
/*TODO*///	/* 0x3500 */
/*TODO*///	bit_in_rg,	bit_in_rgd,	bit_in_in,	bit_in_ind,	bit_in_de,	bit_in_ded,	bit_in_ix,	bit_in_ixd,
/*TODO*///	bit_in_rg,	bit_in_rgd,	bit_in_in,	bit_in_ind,	bit_in_de,	bit_in_ded,	bit_in_ix,	bit_in_ixd,
/*TODO*///	bit_in_rg,	bit_in_rgd,	bit_in_in,	bit_in_ind,	bit_in_de,	bit_in_ded,	bit_in_ix,	bit_in_ixd,
/*TODO*///	bit_in_rg,	bit_in_rgd,	bit_in_in,	bit_in_ind,	bit_in_de,	bit_in_ded,	bit_in_ix,	bit_in_ixd,
/*TODO*///	/* 0x3600 */
/*TODO*///	bit_ind_rg,	bit_ind_rgd,bit_ind_in,	bit_ind_ind,bit_ind_de,	bit_ind_ded,bit_ind_ix,	bit_ind_ixd,
/*TODO*///	bit_ind_rg,	bit_ind_rgd,bit_ind_in,	bit_ind_ind,bit_ind_de,	bit_ind_ded,bit_ind_ix,	bit_ind_ixd,
/*TODO*///	bit_ind_rg,	bit_ind_rgd,bit_ind_in,	bit_ind_ind,bit_ind_de,	bit_ind_ded,bit_ind_ix,	bit_ind_ixd,
/*TODO*///	bit_ind_rg,	bit_ind_rgd,bit_ind_in,	bit_ind_ind,bit_ind_de,	bit_ind_ded,bit_ind_ix,	bit_ind_ixd,
/*TODO*///	/* 0x3700 */
/*TODO*///	bit_ind_rg,	bit_ind_rgd,bit_ind_in,	bit_ind_ind,bit_ind_de,	bit_ind_ded,bit_ind_ix,	bit_ind_ixd,
/*TODO*///	bit_ind_rg,	bit_ind_rgd,bit_ind_in,	bit_ind_ind,bit_ind_de,	bit_ind_ded,bit_ind_ix,	bit_ind_ixd,
/*TODO*///	bit_ind_rg,	bit_ind_rgd,bit_ind_in,	bit_ind_ind,bit_ind_de,	bit_ind_ded,bit_ind_ix,	bit_ind_ixd,
/*TODO*///	bit_ind_rg,	bit_ind_rgd,bit_ind_in,	bit_ind_ind,bit_ind_de,	bit_ind_ded,bit_ind_ix,	bit_ind_ixd,
/*TODO*///	/* 0x3800 */
/*TODO*///	bit_de_rg,	bit_de_rgd,	bit_de_in,	bit_de_ind,	bit_de_de,	bit_de_ded,	bit_de_ix,	bit_de_ixd,
/*TODO*///	bit_de_rg,	bit_de_rgd,	bit_de_in,	bit_de_ind,	bit_de_de,	bit_de_ded,	bit_de_ix,	bit_de_ixd,
/*TODO*///	bit_de_rg,	bit_de_rgd,	bit_de_in,	bit_de_ind,	bit_de_de,	bit_de_ded,	bit_de_ix,	bit_de_ixd,
/*TODO*///	bit_de_rg,	bit_de_rgd,	bit_de_in,	bit_de_ind,	bit_de_de,	bit_de_ded,	bit_de_ix,	bit_de_ixd,
/*TODO*///	/* 0x3900 */
/*TODO*///	bit_de_rg,	bit_de_rgd,	bit_de_in,	bit_de_ind,	bit_de_de,	bit_de_ded,	bit_de_ix,	bit_de_ixd,
/*TODO*///	bit_de_rg,	bit_de_rgd,	bit_de_in,	bit_de_ind,	bit_de_de,	bit_de_ded,	bit_de_ix,	bit_de_ixd,
/*TODO*///	bit_de_rg,	bit_de_rgd,	bit_de_in,	bit_de_ind,	bit_de_de,	bit_de_ded,	bit_de_ix,	bit_de_ixd,
/*TODO*///	bit_de_rg,	bit_de_rgd,	bit_de_in,	bit_de_ind,	bit_de_de,	bit_de_ded,	bit_de_ix,	bit_de_ixd,
/*TODO*///	/* 0x3a00 */
/*TODO*///	bit_ded_rg,	bit_ded_rgd,bit_ded_in,	bit_ded_ind,bit_ded_de,	bit_ded_ded,bit_ded_ix,	bit_ded_ixd,
/*TODO*///	bit_ded_rg,	bit_ded_rgd,bit_ded_in,	bit_ded_ind,bit_ded_de,	bit_ded_ded,bit_ded_ix,	bit_ded_ixd,
/*TODO*///	bit_ded_rg,	bit_ded_rgd,bit_ded_in,	bit_ded_ind,bit_ded_de,	bit_ded_ded,bit_ded_ix,	bit_ded_ixd,
/*TODO*///	bit_ded_rg,	bit_ded_rgd,bit_ded_in,	bit_ded_ind,bit_ded_de,	bit_ded_ded,bit_ded_ix,	bit_ded_ixd,
/*TODO*///	/* 0x3b00 */
/*TODO*///	bit_ded_rg,	bit_ded_rgd,bit_ded_in,	bit_ded_ind,bit_ded_de,	bit_ded_ded,bit_ded_ix,	bit_ded_ixd,
/*TODO*///	bit_ded_rg,	bit_ded_rgd,bit_ded_in,	bit_ded_ind,bit_ded_de,	bit_ded_ded,bit_ded_ix,	bit_ded_ixd,
/*TODO*///	bit_ded_rg,	bit_ded_rgd,bit_ded_in,	bit_ded_ind,bit_ded_de,	bit_ded_ded,bit_ded_ix,	bit_ded_ixd,
/*TODO*///	bit_ded_rg,	bit_ded_rgd,bit_ded_in,	bit_ded_ind,bit_ded_de,	bit_ded_ded,bit_ded_ix,	bit_ded_ixd,
/*TODO*///	/* 0x3c00 */
/*TODO*///	bit_ix_rg,	bit_ix_rgd,	bit_ix_in,	bit_ix_ind,	bit_ix_de,	bit_ix_ded,	bit_ix_ix,	bit_ix_ixd,
/*TODO*///	bit_ix_rg,	bit_ix_rgd,	bit_ix_in,	bit_ix_ind,	bit_ix_de,	bit_ix_ded,	bit_ix_ix,	bit_ix_ixd,
/*TODO*///	bit_ix_rg,	bit_ix_rgd,	bit_ix_in,	bit_ix_ind,	bit_ix_de,	bit_ix_ded,	bit_ix_ix,	bit_ix_ixd,
/*TODO*///	bit_ix_rg,	bit_ix_rgd,	bit_ix_in,	bit_ix_ind,	bit_ix_de,	bit_ix_ded,	bit_ix_ix,	bit_ix_ixd,
/*TODO*///	/* 0x3d00 */
/*TODO*///	bit_ix_rg,	bit_ix_rgd,	bit_ix_in,	bit_ix_ind,	bit_ix_de,	bit_ix_ded,	bit_ix_ix,	bit_ix_ixd,
/*TODO*///	bit_ix_rg,	bit_ix_rgd,	bit_ix_in,	bit_ix_ind,	bit_ix_de,	bit_ix_ded,	bit_ix_ix,	bit_ix_ixd,
/*TODO*///	bit_ix_rg,	bit_ix_rgd,	bit_ix_in,	bit_ix_ind,	bit_ix_de,	bit_ix_ded,	bit_ix_ix,	bit_ix_ixd,
/*TODO*///	bit_ix_rg,	bit_ix_rgd,	bit_ix_in,	bit_ix_ind,	bit_ix_de,	bit_ix_ded,	bit_ix_ix,	bit_ix_ixd,
/*TODO*///	/* 0x3e00 */
/*TODO*///	bit_ixd_rg,	bit_ixd_rgd,bit_ixd_in,	bit_ixd_ind,bit_ixd_de,	bit_ixd_ded,bit_ixd_ix,	bit_ixd_ixd,
/*TODO*///	bit_ixd_rg,	bit_ixd_rgd,bit_ixd_in,	bit_ixd_ind,bit_ixd_de,	bit_ixd_ded,bit_ixd_ix,	bit_ixd_ixd,
/*TODO*///	bit_ixd_rg,	bit_ixd_rgd,bit_ixd_in,	bit_ixd_ind,bit_ixd_de,	bit_ixd_ded,bit_ixd_ix,	bit_ixd_ixd,
/*TODO*///	bit_ixd_rg,	bit_ixd_rgd,bit_ixd_in,	bit_ixd_ind,bit_ixd_de,	bit_ixd_ded,bit_ixd_ix,	bit_ixd_ixd,
/*TODO*///	/* 0x3f00 */
/*TODO*///	bit_ixd_rg,	bit_ixd_rgd,bit_ixd_in,	bit_ixd_ind,bit_ixd_de,	bit_ixd_ded,bit_ixd_ix,	bit_ixd_ixd,
/*TODO*///	bit_ixd_rg,	bit_ixd_rgd,bit_ixd_in,	bit_ixd_ind,bit_ixd_de,	bit_ixd_ded,bit_ixd_ix,	bit_ixd_ixd,
/*TODO*///	bit_ixd_rg,	bit_ixd_rgd,bit_ixd_in,	bit_ixd_ind,bit_ixd_de,	bit_ixd_ded,bit_ixd_ix,	bit_ixd_ixd,
/*TODO*///	bit_ixd_rg,	bit_ixd_rgd,bit_ixd_in,	bit_ixd_ind,bit_ixd_de,	bit_ixd_ded,bit_ixd_ix,	bit_ixd_ixd,
/*TODO*///
/*TODO*///	/* 0x4000 */
/*TODO*///	bic_rg_rg,	bic_rg_rgd,	bic_rg_in,	bic_rg_ind,	bic_rg_de,	bic_rg_ded,	bic_rg_ix,	bic_rg_ixd,
/*TODO*///	bic_rg_rg,	bic_rg_rgd,	bic_rg_in,	bic_rg_ind,	bic_rg_de,	bic_rg_ded,	bic_rg_ix,	bic_rg_ixd,
/*TODO*///	bic_rg_rg,	bic_rg_rgd,	bic_rg_in,	bic_rg_ind,	bic_rg_de,	bic_rg_ded,	bic_rg_ix,	bic_rg_ixd,
/*TODO*///	bic_rg_rg,	bic_rg_rgd,	bic_rg_in,	bic_rg_ind,	bic_rg_de,	bic_rg_ded,	bic_rg_ix,	bic_rg_ixd,
/*TODO*///	/* 0x4100 */
/*TODO*///	bic_rg_rg,	bic_rg_rgd,	bic_rg_in,	bic_rg_ind,	bic_rg_de,	bic_rg_ded,	bic_rg_ix,	bic_rg_ixd,
/*TODO*///	bic_rg_rg,	bic_rg_rgd,	bic_rg_in,	bic_rg_ind,	bic_rg_de,	bic_rg_ded,	bic_rg_ix,	bic_rg_ixd,
/*TODO*///	bic_rg_rg,	bic_rg_rgd,	bic_rg_in,	bic_rg_ind,	bic_rg_de,	bic_rg_ded,	bic_rg_ix,	bic_rg_ixd,
/*TODO*///	bic_rg_rg,	bic_rg_rgd,	bic_rg_in,	bic_rg_ind,	bic_rg_de,	bic_rg_ded,	bic_rg_ix,	bic_rg_ixd,
/*TODO*///	/* 0x4200 */
/*TODO*///	bic_rgd_rg,	bic_rgd_rgd,bic_rgd_in,	bic_rgd_ind,bic_rgd_de,	bic_rgd_ded,bic_rgd_ix,	bic_rgd_ixd,
/*TODO*///	bic_rgd_rg,	bic_rgd_rgd,bic_rgd_in,	bic_rgd_ind,bic_rgd_de,	bic_rgd_ded,bic_rgd_ix,	bic_rgd_ixd,
/*TODO*///	bic_rgd_rg,	bic_rgd_rgd,bic_rgd_in,	bic_rgd_ind,bic_rgd_de,	bic_rgd_ded,bic_rgd_ix,	bic_rgd_ixd,
/*TODO*///	bic_rgd_rg,	bic_rgd_rgd,bic_rgd_in,	bic_rgd_ind,bic_rgd_de,	bic_rgd_ded,bic_rgd_ix,	bic_rgd_ixd,
/*TODO*///	/* 0x4300 */
/*TODO*///	bic_rgd_rg,	bic_rgd_rgd,bic_rgd_in,	bic_rgd_ind,bic_rgd_de,	bic_rgd_ded,bic_rgd_ix,	bic_rgd_ixd,
/*TODO*///	bic_rgd_rg,	bic_rgd_rgd,bic_rgd_in,	bic_rgd_ind,bic_rgd_de,	bic_rgd_ded,bic_rgd_ix,	bic_rgd_ixd,
/*TODO*///	bic_rgd_rg,	bic_rgd_rgd,bic_rgd_in,	bic_rgd_ind,bic_rgd_de,	bic_rgd_ded,bic_rgd_ix,	bic_rgd_ixd,
/*TODO*///	bic_rgd_rg,	bic_rgd_rgd,bic_rgd_in,	bic_rgd_ind,bic_rgd_de,	bic_rgd_ded,bic_rgd_ix,	bic_rgd_ixd,
/*TODO*///	/* 0x4400 */
/*TODO*///	bic_in_rg,	bic_in_rgd,	bic_in_in,	bic_in_ind,	bic_in_de,	bic_in_ded,	bic_in_ix,	bic_in_ixd,
/*TODO*///	bic_in_rg,	bic_in_rgd,	bic_in_in,	bic_in_ind,	bic_in_de,	bic_in_ded,	bic_in_ix,	bic_in_ixd,
/*TODO*///	bic_in_rg,	bic_in_rgd,	bic_in_in,	bic_in_ind,	bic_in_de,	bic_in_ded,	bic_in_ix,	bic_in_ixd,
/*TODO*///	bic_in_rg,	bic_in_rgd,	bic_in_in,	bic_in_ind,	bic_in_de,	bic_in_ded,	bic_in_ix,	bic_in_ixd,
/*TODO*///	/* 0x4500 */
/*TODO*///	bic_in_rg,	bic_in_rgd,	bic_in_in,	bic_in_ind,	bic_in_de,	bic_in_ded,	bic_in_ix,	bic_in_ixd,
/*TODO*///	bic_in_rg,	bic_in_rgd,	bic_in_in,	bic_in_ind,	bic_in_de,	bic_in_ded,	bic_in_ix,	bic_in_ixd,
/*TODO*///	bic_in_rg,	bic_in_rgd,	bic_in_in,	bic_in_ind,	bic_in_de,	bic_in_ded,	bic_in_ix,	bic_in_ixd,
/*TODO*///	bic_in_rg,	bic_in_rgd,	bic_in_in,	bic_in_ind,	bic_in_de,	bic_in_ded,	bic_in_ix,	bic_in_ixd,
/*TODO*///	/* 0x4600 */
/*TODO*///	bic_ind_rg,	bic_ind_rgd,bic_ind_in,	bic_ind_ind,bic_ind_de,	bic_ind_ded,bic_ind_ix,	bic_ind_ixd,
/*TODO*///	bic_ind_rg,	bic_ind_rgd,bic_ind_in,	bic_ind_ind,bic_ind_de,	bic_ind_ded,bic_ind_ix,	bic_ind_ixd,
/*TODO*///	bic_ind_rg,	bic_ind_rgd,bic_ind_in,	bic_ind_ind,bic_ind_de,	bic_ind_ded,bic_ind_ix,	bic_ind_ixd,
/*TODO*///	bic_ind_rg,	bic_ind_rgd,bic_ind_in,	bic_ind_ind,bic_ind_de,	bic_ind_ded,bic_ind_ix,	bic_ind_ixd,
/*TODO*///	/* 0x4700 */
/*TODO*///	bic_ind_rg,	bic_ind_rgd,bic_ind_in,	bic_ind_ind,bic_ind_de,	bic_ind_ded,bic_ind_ix,	bic_ind_ixd,
/*TODO*///	bic_ind_rg,	bic_ind_rgd,bic_ind_in,	bic_ind_ind,bic_ind_de,	bic_ind_ded,bic_ind_ix,	bic_ind_ixd,
/*TODO*///	bic_ind_rg,	bic_ind_rgd,bic_ind_in,	bic_ind_ind,bic_ind_de,	bic_ind_ded,bic_ind_ix,	bic_ind_ixd,
/*TODO*///	bic_ind_rg,	bic_ind_rgd,bic_ind_in,	bic_ind_ind,bic_ind_de,	bic_ind_ded,bic_ind_ix,	bic_ind_ixd,
/*TODO*///	/* 0x4800 */
/*TODO*///	bic_de_rg,	bic_de_rgd,	bic_de_in,	bic_de_ind,	bic_de_de,	bic_de_ded,	bic_de_ix,	bic_de_ixd,
/*TODO*///	bic_de_rg,	bic_de_rgd,	bic_de_in,	bic_de_ind,	bic_de_de,	bic_de_ded,	bic_de_ix,	bic_de_ixd,
/*TODO*///	bic_de_rg,	bic_de_rgd,	bic_de_in,	bic_de_ind,	bic_de_de,	bic_de_ded,	bic_de_ix,	bic_de_ixd,
/*TODO*///	bic_de_rg,	bic_de_rgd,	bic_de_in,	bic_de_ind,	bic_de_de,	bic_de_ded,	bic_de_ix,	bic_de_ixd,
/*TODO*///	/* 0x4900 */
/*TODO*///	bic_de_rg,	bic_de_rgd,	bic_de_in,	bic_de_ind,	bic_de_de,	bic_de_ded,	bic_de_ix,	bic_de_ixd,
/*TODO*///	bic_de_rg,	bic_de_rgd,	bic_de_in,	bic_de_ind,	bic_de_de,	bic_de_ded,	bic_de_ix,	bic_de_ixd,
/*TODO*///	bic_de_rg,	bic_de_rgd,	bic_de_in,	bic_de_ind,	bic_de_de,	bic_de_ded,	bic_de_ix,	bic_de_ixd,
/*TODO*///	bic_de_rg,	bic_de_rgd,	bic_de_in,	bic_de_ind,	bic_de_de,	bic_de_ded,	bic_de_ix,	bic_de_ixd,
/*TODO*///	/* 0x4a00 */
/*TODO*///	bic_ded_rg,	bic_ded_rgd,bic_ded_in,	bic_ded_ind,bic_ded_de,	bic_ded_ded,bic_ded_ix,	bic_ded_ixd,
/*TODO*///	bic_ded_rg,	bic_ded_rgd,bic_ded_in,	bic_ded_ind,bic_ded_de,	bic_ded_ded,bic_ded_ix,	bic_ded_ixd,
/*TODO*///	bic_ded_rg,	bic_ded_rgd,bic_ded_in,	bic_ded_ind,bic_ded_de,	bic_ded_ded,bic_ded_ix,	bic_ded_ixd,
/*TODO*///	bic_ded_rg,	bic_ded_rgd,bic_ded_in,	bic_ded_ind,bic_ded_de,	bic_ded_ded,bic_ded_ix,	bic_ded_ixd,
/*TODO*///	/* 0x4b00 */
/*TODO*///	bic_ded_rg,	bic_ded_rgd,bic_ded_in,	bic_ded_ind,bic_ded_de,	bic_ded_ded,bic_ded_ix,	bic_ded_ixd,
/*TODO*///	bic_ded_rg,	bic_ded_rgd,bic_ded_in,	bic_ded_ind,bic_ded_de,	bic_ded_ded,bic_ded_ix,	bic_ded_ixd,
/*TODO*///	bic_ded_rg,	bic_ded_rgd,bic_ded_in,	bic_ded_ind,bic_ded_de,	bic_ded_ded,bic_ded_ix,	bic_ded_ixd,
/*TODO*///	bic_ded_rg,	bic_ded_rgd,bic_ded_in,	bic_ded_ind,bic_ded_de,	bic_ded_ded,bic_ded_ix,	bic_ded_ixd,
/*TODO*///	/* 0x4c00 */
/*TODO*///	bic_ix_rg,	bic_ix_rgd,	bic_ix_in,	bic_ix_ind,	bic_ix_de,	bic_ix_ded,	bic_ix_ix,	bic_ix_ixd,
/*TODO*///	bic_ix_rg,	bic_ix_rgd,	bic_ix_in,	bic_ix_ind,	bic_ix_de,	bic_ix_ded,	bic_ix_ix,	bic_ix_ixd,
/*TODO*///	bic_ix_rg,	bic_ix_rgd,	bic_ix_in,	bic_ix_ind,	bic_ix_de,	bic_ix_ded,	bic_ix_ix,	bic_ix_ixd,
/*TODO*///	bic_ix_rg,	bic_ix_rgd,	bic_ix_in,	bic_ix_ind,	bic_ix_de,	bic_ix_ded,	bic_ix_ix,	bic_ix_ixd,
/*TODO*///	/* 0x4d00 */
/*TODO*///	bic_ix_rg,	bic_ix_rgd,	bic_ix_in,	bic_ix_ind,	bic_ix_de,	bic_ix_ded,	bic_ix_ix,	bic_ix_ixd,
/*TODO*///	bic_ix_rg,	bic_ix_rgd,	bic_ix_in,	bic_ix_ind,	bic_ix_de,	bic_ix_ded,	bic_ix_ix,	bic_ix_ixd,
/*TODO*///	bic_ix_rg,	bic_ix_rgd,	bic_ix_in,	bic_ix_ind,	bic_ix_de,	bic_ix_ded,	bic_ix_ix,	bic_ix_ixd,
/*TODO*///	bic_ix_rg,	bic_ix_rgd,	bic_ix_in,	bic_ix_ind,	bic_ix_de,	bic_ix_ded,	bic_ix_ix,	bic_ix_ixd,
/*TODO*///	/* 0x4e00 */
/*TODO*///	bic_ixd_rg,	bic_ixd_rgd,bic_ixd_in,	bic_ixd_ind,bic_ixd_de,	bic_ixd_ded,bic_ixd_ix,	bic_ixd_ixd,
/*TODO*///	bic_ixd_rg,	bic_ixd_rgd,bic_ixd_in,	bic_ixd_ind,bic_ixd_de,	bic_ixd_ded,bic_ixd_ix,	bic_ixd_ixd,
/*TODO*///	bic_ixd_rg,	bic_ixd_rgd,bic_ixd_in,	bic_ixd_ind,bic_ixd_de,	bic_ixd_ded,bic_ixd_ix,	bic_ixd_ixd,
/*TODO*///	bic_ixd_rg,	bic_ixd_rgd,bic_ixd_in,	bic_ixd_ind,bic_ixd_de,	bic_ixd_ded,bic_ixd_ix,	bic_ixd_ixd,
/*TODO*///	/* 0x4f00 */
/*TODO*///	bic_ixd_rg,	bic_ixd_rgd,bic_ixd_in,	bic_ixd_ind,bic_ixd_de,	bic_ixd_ded,bic_ixd_ix,	bic_ixd_ixd,
/*TODO*///	bic_ixd_rg,	bic_ixd_rgd,bic_ixd_in,	bic_ixd_ind,bic_ixd_de,	bic_ixd_ded,bic_ixd_ix,	bic_ixd_ixd,
/*TODO*///	bic_ixd_rg,	bic_ixd_rgd,bic_ixd_in,	bic_ixd_ind,bic_ixd_de,	bic_ixd_ded,bic_ixd_ix,	bic_ixd_ixd,
/*TODO*///	bic_ixd_rg,	bic_ixd_rgd,bic_ixd_in,	bic_ixd_ind,bic_ixd_de,	bic_ixd_ded,bic_ixd_ix,	bic_ixd_ixd,
/*TODO*///
/*TODO*///	/* 0x5000 */
/*TODO*///	bis_rg_rg,	bis_rg_rgd,	bis_rg_in,	bis_rg_ind,	bis_rg_de,	bis_rg_ded,	bis_rg_ix,	bis_rg_ixd,
/*TODO*///	bis_rg_rg,	bis_rg_rgd,	bis_rg_in,	bis_rg_ind,	bis_rg_de,	bis_rg_ded,	bis_rg_ix,	bis_rg_ixd,
/*TODO*///	bis_rg_rg,	bis_rg_rgd,	bis_rg_in,	bis_rg_ind,	bis_rg_de,	bis_rg_ded,	bis_rg_ix,	bis_rg_ixd,
/*TODO*///	bis_rg_rg,	bis_rg_rgd,	bis_rg_in,	bis_rg_ind,	bis_rg_de,	bis_rg_ded,	bis_rg_ix,	bis_rg_ixd,
/*TODO*///	/* 0x5100 */
/*TODO*///	bis_rg_rg,	bis_rg_rgd,	bis_rg_in,	bis_rg_ind,	bis_rg_de,	bis_rg_ded,	bis_rg_ix,	bis_rg_ixd,
/*TODO*///	bis_rg_rg,	bis_rg_rgd,	bis_rg_in,	bis_rg_ind,	bis_rg_de,	bis_rg_ded,	bis_rg_ix,	bis_rg_ixd,
/*TODO*///	bis_rg_rg,	bis_rg_rgd,	bis_rg_in,	bis_rg_ind,	bis_rg_de,	bis_rg_ded,	bis_rg_ix,	bis_rg_ixd,
/*TODO*///	bis_rg_rg,	bis_rg_rgd,	bis_rg_in,	bis_rg_ind,	bis_rg_de,	bis_rg_ded,	bis_rg_ix,	bis_rg_ixd,
/*TODO*///	/* 0x5200 */
/*TODO*///	bis_rgd_rg,	bis_rgd_rgd,bis_rgd_in,	bis_rgd_ind,bis_rgd_de,	bis_rgd_ded,bis_rgd_ix,	bis_rgd_ixd,
/*TODO*///	bis_rgd_rg,	bis_rgd_rgd,bis_rgd_in,	bis_rgd_ind,bis_rgd_de,	bis_rgd_ded,bis_rgd_ix,	bis_rgd_ixd,
/*TODO*///	bis_rgd_rg,	bis_rgd_rgd,bis_rgd_in,	bis_rgd_ind,bis_rgd_de,	bis_rgd_ded,bis_rgd_ix,	bis_rgd_ixd,
/*TODO*///	bis_rgd_rg,	bis_rgd_rgd,bis_rgd_in,	bis_rgd_ind,bis_rgd_de,	bis_rgd_ded,bis_rgd_ix,	bis_rgd_ixd,
/*TODO*///	/* 0x5300 */
/*TODO*///	bis_rgd_rg,	bis_rgd_rgd,bis_rgd_in,	bis_rgd_ind,bis_rgd_de,	bis_rgd_ded,bis_rgd_ix,	bis_rgd_ixd,
/*TODO*///	bis_rgd_rg,	bis_rgd_rgd,bis_rgd_in,	bis_rgd_ind,bis_rgd_de,	bis_rgd_ded,bis_rgd_ix,	bis_rgd_ixd,
/*TODO*///	bis_rgd_rg,	bis_rgd_rgd,bis_rgd_in,	bis_rgd_ind,bis_rgd_de,	bis_rgd_ded,bis_rgd_ix,	bis_rgd_ixd,
/*TODO*///	bis_rgd_rg,	bis_rgd_rgd,bis_rgd_in,	bis_rgd_ind,bis_rgd_de,	bis_rgd_ded,bis_rgd_ix,	bis_rgd_ixd,
/*TODO*///	/* 0x5400 */
/*TODO*///	bis_in_rg,	bis_in_rgd,	bis_in_in,	bis_in_ind,	bis_in_de,	bis_in_ded,	bis_in_ix,	bis_in_ixd,
/*TODO*///	bis_in_rg,	bis_in_rgd,	bis_in_in,	bis_in_ind,	bis_in_de,	bis_in_ded,	bis_in_ix,	bis_in_ixd,
/*TODO*///	bis_in_rg,	bis_in_rgd,	bis_in_in,	bis_in_ind,	bis_in_de,	bis_in_ded,	bis_in_ix,	bis_in_ixd,
/*TODO*///	bis_in_rg,	bis_in_rgd,	bis_in_in,	bis_in_ind,	bis_in_de,	bis_in_ded,	bis_in_ix,	bis_in_ixd,
/*TODO*///	/* 0x5500 */
/*TODO*///	bis_in_rg,	bis_in_rgd,	bis_in_in,	bis_in_ind,	bis_in_de,	bis_in_ded,	bis_in_ix,	bis_in_ixd,
/*TODO*///	bis_in_rg,	bis_in_rgd,	bis_in_in,	bis_in_ind,	bis_in_de,	bis_in_ded,	bis_in_ix,	bis_in_ixd,
/*TODO*///	bis_in_rg,	bis_in_rgd,	bis_in_in,	bis_in_ind,	bis_in_de,	bis_in_ded,	bis_in_ix,	bis_in_ixd,
/*TODO*///	bis_in_rg,	bis_in_rgd,	bis_in_in,	bis_in_ind,	bis_in_de,	bis_in_ded,	bis_in_ix,	bis_in_ixd,
/*TODO*///	/* 0x5600 */
/*TODO*///	bis_ind_rg,	bis_ind_rgd,bis_ind_in,	bis_ind_ind,bis_ind_de,	bis_ind_ded,bis_ind_ix,	bis_ind_ixd,
/*TODO*///	bis_ind_rg,	bis_ind_rgd,bis_ind_in,	bis_ind_ind,bis_ind_de,	bis_ind_ded,bis_ind_ix,	bis_ind_ixd,
/*TODO*///	bis_ind_rg,	bis_ind_rgd,bis_ind_in,	bis_ind_ind,bis_ind_de,	bis_ind_ded,bis_ind_ix,	bis_ind_ixd,
/*TODO*///	bis_ind_rg,	bis_ind_rgd,bis_ind_in,	bis_ind_ind,bis_ind_de,	bis_ind_ded,bis_ind_ix,	bis_ind_ixd,
/*TODO*///	/* 0x5700 */
/*TODO*///	bis_ind_rg,	bis_ind_rgd,bis_ind_in,	bis_ind_ind,bis_ind_de,	bis_ind_ded,bis_ind_ix,	bis_ind_ixd,
/*TODO*///	bis_ind_rg,	bis_ind_rgd,bis_ind_in,	bis_ind_ind,bis_ind_de,	bis_ind_ded,bis_ind_ix,	bis_ind_ixd,
/*TODO*///	bis_ind_rg,	bis_ind_rgd,bis_ind_in,	bis_ind_ind,bis_ind_de,	bis_ind_ded,bis_ind_ix,	bis_ind_ixd,
/*TODO*///	bis_ind_rg,	bis_ind_rgd,bis_ind_in,	bis_ind_ind,bis_ind_de,	bis_ind_ded,bis_ind_ix,	bis_ind_ixd,
/*TODO*///	/* 0x5800 */
/*TODO*///	bis_de_rg,	bis_de_rgd,	bis_de_in,	bis_de_ind,	bis_de_de,	bis_de_ded,	bis_de_ix,	bis_de_ixd,
/*TODO*///	bis_de_rg,	bis_de_rgd,	bis_de_in,	bis_de_ind,	bis_de_de,	bis_de_ded,	bis_de_ix,	bis_de_ixd,
/*TODO*///	bis_de_rg,	bis_de_rgd,	bis_de_in,	bis_de_ind,	bis_de_de,	bis_de_ded,	bis_de_ix,	bis_de_ixd,
/*TODO*///	bis_de_rg,	bis_de_rgd,	bis_de_in,	bis_de_ind,	bis_de_de,	bis_de_ded,	bis_de_ix,	bis_de_ixd,
/*TODO*///	/* 0x5900 */
/*TODO*///	bis_de_rg,	bis_de_rgd,	bis_de_in,	bis_de_ind,	bis_de_de,	bis_de_ded,	bis_de_ix,	bis_de_ixd,
/*TODO*///	bis_de_rg,	bis_de_rgd,	bis_de_in,	bis_de_ind,	bis_de_de,	bis_de_ded,	bis_de_ix,	bis_de_ixd,
/*TODO*///	bis_de_rg,	bis_de_rgd,	bis_de_in,	bis_de_ind,	bis_de_de,	bis_de_ded,	bis_de_ix,	bis_de_ixd,
/*TODO*///	bis_de_rg,	bis_de_rgd,	bis_de_in,	bis_de_ind,	bis_de_de,	bis_de_ded,	bis_de_ix,	bis_de_ixd,
/*TODO*///	/* 0x5a00 */
/*TODO*///	bis_ded_rg,	bis_ded_rgd,bis_ded_in,	bis_ded_ind,bis_ded_de,	bis_ded_ded,bis_ded_ix,	bis_ded_ixd,
/*TODO*///	bis_ded_rg,	bis_ded_rgd,bis_ded_in,	bis_ded_ind,bis_ded_de,	bis_ded_ded,bis_ded_ix,	bis_ded_ixd,
/*TODO*///	bis_ded_rg,	bis_ded_rgd,bis_ded_in,	bis_ded_ind,bis_ded_de,	bis_ded_ded,bis_ded_ix,	bis_ded_ixd,
/*TODO*///	bis_ded_rg,	bis_ded_rgd,bis_ded_in,	bis_ded_ind,bis_ded_de,	bis_ded_ded,bis_ded_ix,	bis_ded_ixd,
/*TODO*///	/* 0x5b00 */
/*TODO*///	bis_ded_rg,	bis_ded_rgd,bis_ded_in,	bis_ded_ind,bis_ded_de,	bis_ded_ded,bis_ded_ix,	bis_ded_ixd,
/*TODO*///	bis_ded_rg,	bis_ded_rgd,bis_ded_in,	bis_ded_ind,bis_ded_de,	bis_ded_ded,bis_ded_ix,	bis_ded_ixd,
/*TODO*///	bis_ded_rg,	bis_ded_rgd,bis_ded_in,	bis_ded_ind,bis_ded_de,	bis_ded_ded,bis_ded_ix,	bis_ded_ixd,
/*TODO*///	bis_ded_rg,	bis_ded_rgd,bis_ded_in,	bis_ded_ind,bis_ded_de,	bis_ded_ded,bis_ded_ix,	bis_ded_ixd,
/*TODO*///	/* 0x5c00 */
/*TODO*///	bis_ix_rg,	bis_ix_rgd,	bis_ix_in,	bis_ix_ind,	bis_ix_de,	bis_ix_ded,	bis_ix_ix,	bis_ix_ixd,
/*TODO*///	bis_ix_rg,	bis_ix_rgd,	bis_ix_in,	bis_ix_ind,	bis_ix_de,	bis_ix_ded,	bis_ix_ix,	bis_ix_ixd,
/*TODO*///	bis_ix_rg,	bis_ix_rgd,	bis_ix_in,	bis_ix_ind,	bis_ix_de,	bis_ix_ded,	bis_ix_ix,	bis_ix_ixd,
/*TODO*///	bis_ix_rg,	bis_ix_rgd,	bis_ix_in,	bis_ix_ind,	bis_ix_de,	bis_ix_ded,	bis_ix_ix,	bis_ix_ixd,
/*TODO*///	/* 0x5d00 */
/*TODO*///	bis_ix_rg,	bis_ix_rgd,	bis_ix_in,	bis_ix_ind,	bis_ix_de,	bis_ix_ded,	bis_ix_ix,	bis_ix_ixd,
/*TODO*///	bis_ix_rg,	bis_ix_rgd,	bis_ix_in,	bis_ix_ind,	bis_ix_de,	bis_ix_ded,	bis_ix_ix,	bis_ix_ixd,
/*TODO*///	bis_ix_rg,	bis_ix_rgd,	bis_ix_in,	bis_ix_ind,	bis_ix_de,	bis_ix_ded,	bis_ix_ix,	bis_ix_ixd,
/*TODO*///	bis_ix_rg,	bis_ix_rgd,	bis_ix_in,	bis_ix_ind,	bis_ix_de,	bis_ix_ded,	bis_ix_ix,	bis_ix_ixd,
/*TODO*///	/* 0x5e00 */
/*TODO*///	bis_ixd_rg,	bis_ixd_rgd,bis_ixd_in,	bis_ixd_ind,bis_ixd_de,	bis_ixd_ded,bis_ixd_ix,	bis_ixd_ixd,
/*TODO*///	bis_ixd_rg,	bis_ixd_rgd,bis_ixd_in,	bis_ixd_ind,bis_ixd_de,	bis_ixd_ded,bis_ixd_ix,	bis_ixd_ixd,
/*TODO*///	bis_ixd_rg,	bis_ixd_rgd,bis_ixd_in,	bis_ixd_ind,bis_ixd_de,	bis_ixd_ded,bis_ixd_ix,	bis_ixd_ixd,
/*TODO*///	bis_ixd_rg,	bis_ixd_rgd,bis_ixd_in,	bis_ixd_ind,bis_ixd_de,	bis_ixd_ded,bis_ixd_ix,	bis_ixd_ixd,
/*TODO*///	/* 0x5f00 */
/*TODO*///	bis_ixd_rg,	bis_ixd_rgd,bis_ixd_in,	bis_ixd_ind,bis_ixd_de,	bis_ixd_ded,bis_ixd_ix,	bis_ixd_ixd,
/*TODO*///	bis_ixd_rg,	bis_ixd_rgd,bis_ixd_in,	bis_ixd_ind,bis_ixd_de,	bis_ixd_ded,bis_ixd_ix,	bis_ixd_ixd,
/*TODO*///	bis_ixd_rg,	bis_ixd_rgd,bis_ixd_in,	bis_ixd_ind,bis_ixd_de,	bis_ixd_ded,bis_ixd_ix,	bis_ixd_ixd,
/*TODO*///	bis_ixd_rg,	bis_ixd_rgd,bis_ixd_in,	bis_ixd_ind,bis_ixd_de,	bis_ixd_ded,bis_ixd_ix,	bis_ixd_ixd,
/*TODO*///
/*TODO*///	/* 0x6000 */
/*TODO*///	add_rg_rg,	add_rg_rgd,	add_rg_in,	add_rg_ind,	add_rg_de,	add_rg_ded,	add_rg_ix,	add_rg_ixd,
/*TODO*///	add_rg_rg,	add_rg_rgd,	add_rg_in,	add_rg_ind,	add_rg_de,	add_rg_ded,	add_rg_ix,	add_rg_ixd,
/*TODO*///	add_rg_rg,	add_rg_rgd,	add_rg_in,	add_rg_ind,	add_rg_de,	add_rg_ded,	add_rg_ix,	add_rg_ixd,
/*TODO*///	add_rg_rg,	add_rg_rgd,	add_rg_in,	add_rg_ind,	add_rg_de,	add_rg_ded,	add_rg_ix,	add_rg_ixd,
/*TODO*///	/* 0x6100 */
/*TODO*///	add_rg_rg,	add_rg_rgd,	add_rg_in,	add_rg_ind,	add_rg_de,	add_rg_ded,	add_rg_ix,	add_rg_ixd,
/*TODO*///	add_rg_rg,	add_rg_rgd,	add_rg_in,	add_rg_ind,	add_rg_de,	add_rg_ded,	add_rg_ix,	add_rg_ixd,
/*TODO*///	add_rg_rg,	add_rg_rgd,	add_rg_in,	add_rg_ind,	add_rg_de,	add_rg_ded,	add_rg_ix,	add_rg_ixd,
/*TODO*///	add_rg_rg,	add_rg_rgd,	add_rg_in,	add_rg_ind,	add_rg_de,	add_rg_ded,	add_rg_ix,	add_rg_ixd,
/*TODO*///	/* 0x6200 */
/*TODO*///	add_rgd_rg,	add_rgd_rgd,add_rgd_in,	add_rgd_ind,add_rgd_de,	add_rgd_ded,add_rgd_ix,	add_rgd_ixd,
/*TODO*///	add_rgd_rg,	add_rgd_rgd,add_rgd_in,	add_rgd_ind,add_rgd_de,	add_rgd_ded,add_rgd_ix,	add_rgd_ixd,
/*TODO*///	add_rgd_rg,	add_rgd_rgd,add_rgd_in,	add_rgd_ind,add_rgd_de,	add_rgd_ded,add_rgd_ix,	add_rgd_ixd,
/*TODO*///	add_rgd_rg,	add_rgd_rgd,add_rgd_in,	add_rgd_ind,add_rgd_de,	add_rgd_ded,add_rgd_ix,	add_rgd_ixd,
/*TODO*///	/* 0x6300 */
/*TODO*///	add_rgd_rg,	add_rgd_rgd,add_rgd_in,	add_rgd_ind,add_rgd_de,	add_rgd_ded,add_rgd_ix,	add_rgd_ixd,
/*TODO*///	add_rgd_rg,	add_rgd_rgd,add_rgd_in,	add_rgd_ind,add_rgd_de,	add_rgd_ded,add_rgd_ix,	add_rgd_ixd,
/*TODO*///	add_rgd_rg,	add_rgd_rgd,add_rgd_in,	add_rgd_ind,add_rgd_de,	add_rgd_ded,add_rgd_ix,	add_rgd_ixd,
/*TODO*///	add_rgd_rg,	add_rgd_rgd,add_rgd_in,	add_rgd_ind,add_rgd_de,	add_rgd_ded,add_rgd_ix,	add_rgd_ixd,
/*TODO*///	/* 0x6400 */
/*TODO*///	add_in_rg,	add_in_rgd,	add_in_in,	add_in_ind,	add_in_de,	add_in_ded,	add_in_ix,	add_in_ixd,
/*TODO*///	add_in_rg,	add_in_rgd,	add_in_in,	add_in_ind,	add_in_de,	add_in_ded,	add_in_ix,	add_in_ixd,
/*TODO*///	add_in_rg,	add_in_rgd,	add_in_in,	add_in_ind,	add_in_de,	add_in_ded,	add_in_ix,	add_in_ixd,
/*TODO*///	add_in_rg,	add_in_rgd,	add_in_in,	add_in_ind,	add_in_de,	add_in_ded,	add_in_ix,	add_in_ixd,
/*TODO*///	/* 0x6500 */
/*TODO*///	add_in_rg,	add_in_rgd,	add_in_in,	add_in_ind,	add_in_de,	add_in_ded,	add_in_ix,	add_in_ixd,
/*TODO*///	add_in_rg,	add_in_rgd,	add_in_in,	add_in_ind,	add_in_de,	add_in_ded,	add_in_ix,	add_in_ixd,
/*TODO*///	add_in_rg,	add_in_rgd,	add_in_in,	add_in_ind,	add_in_de,	add_in_ded,	add_in_ix,	add_in_ixd,
/*TODO*///	add_in_rg,	add_in_rgd,	add_in_in,	add_in_ind,	add_in_de,	add_in_ded,	add_in_ix,	add_in_ixd,
/*TODO*///	/* 0x6600 */
/*TODO*///	add_ind_rg,	add_ind_rgd,add_ind_in,	add_ind_ind,add_ind_de,	add_ind_ded,add_ind_ix,	add_ind_ixd,
/*TODO*///	add_ind_rg,	add_ind_rgd,add_ind_in,	add_ind_ind,add_ind_de,	add_ind_ded,add_ind_ix,	add_ind_ixd,
/*TODO*///	add_ind_rg,	add_ind_rgd,add_ind_in,	add_ind_ind,add_ind_de,	add_ind_ded,add_ind_ix,	add_ind_ixd,
/*TODO*///	add_ind_rg,	add_ind_rgd,add_ind_in,	add_ind_ind,add_ind_de,	add_ind_ded,add_ind_ix,	add_ind_ixd,
/*TODO*///	/* 0x6700 */
/*TODO*///	add_ind_rg,	add_ind_rgd,add_ind_in,	add_ind_ind,add_ind_de,	add_ind_ded,add_ind_ix,	add_ind_ixd,
/*TODO*///	add_ind_rg,	add_ind_rgd,add_ind_in,	add_ind_ind,add_ind_de,	add_ind_ded,add_ind_ix,	add_ind_ixd,
/*TODO*///	add_ind_rg,	add_ind_rgd,add_ind_in,	add_ind_ind,add_ind_de,	add_ind_ded,add_ind_ix,	add_ind_ixd,
/*TODO*///	add_ind_rg,	add_ind_rgd,add_ind_in,	add_ind_ind,add_ind_de,	add_ind_ded,add_ind_ix,	add_ind_ixd,
/*TODO*///	/* 0x6800 */
/*TODO*///	add_de_rg,	add_de_rgd,	add_de_in,	add_de_ind,	add_de_de,	add_de_ded,	add_de_ix,	add_de_ixd,
/*TODO*///	add_de_rg,	add_de_rgd,	add_de_in,	add_de_ind,	add_de_de,	add_de_ded,	add_de_ix,	add_de_ixd,
/*TODO*///	add_de_rg,	add_de_rgd,	add_de_in,	add_de_ind,	add_de_de,	add_de_ded,	add_de_ix,	add_de_ixd,
/*TODO*///	add_de_rg,	add_de_rgd,	add_de_in,	add_de_ind,	add_de_de,	add_de_ded,	add_de_ix,	add_de_ixd,
/*TODO*///	/* 0x6900 */
/*TODO*///	add_de_rg,	add_de_rgd,	add_de_in,	add_de_ind,	add_de_de,	add_de_ded,	add_de_ix,	add_de_ixd,
/*TODO*///	add_de_rg,	add_de_rgd,	add_de_in,	add_de_ind,	add_de_de,	add_de_ded,	add_de_ix,	add_de_ixd,
/*TODO*///	add_de_rg,	add_de_rgd,	add_de_in,	add_de_ind,	add_de_de,	add_de_ded,	add_de_ix,	add_de_ixd,
/*TODO*///	add_de_rg,	add_de_rgd,	add_de_in,	add_de_ind,	add_de_de,	add_de_ded,	add_de_ix,	add_de_ixd,
/*TODO*///	/* 0x6a00 */
/*TODO*///	add_ded_rg,	add_ded_rgd,add_ded_in,	add_ded_ind,add_ded_de,	add_ded_ded,add_ded_ix,	add_ded_ixd,
/*TODO*///	add_ded_rg,	add_ded_rgd,add_ded_in,	add_ded_ind,add_ded_de,	add_ded_ded,add_ded_ix,	add_ded_ixd,
/*TODO*///	add_ded_rg,	add_ded_rgd,add_ded_in,	add_ded_ind,add_ded_de,	add_ded_ded,add_ded_ix,	add_ded_ixd,
/*TODO*///	add_ded_rg,	add_ded_rgd,add_ded_in,	add_ded_ind,add_ded_de,	add_ded_ded,add_ded_ix,	add_ded_ixd,
/*TODO*///	/* 0x6b00 */
/*TODO*///	add_ded_rg,	add_ded_rgd,add_ded_in,	add_ded_ind,add_ded_de,	add_ded_ded,add_ded_ix,	add_ded_ixd,
/*TODO*///	add_ded_rg,	add_ded_rgd,add_ded_in,	add_ded_ind,add_ded_de,	add_ded_ded,add_ded_ix,	add_ded_ixd,
/*TODO*///	add_ded_rg,	add_ded_rgd,add_ded_in,	add_ded_ind,add_ded_de,	add_ded_ded,add_ded_ix,	add_ded_ixd,
/*TODO*///	add_ded_rg,	add_ded_rgd,add_ded_in,	add_ded_ind,add_ded_de,	add_ded_ded,add_ded_ix,	add_ded_ixd,
/*TODO*///	/* 0x6c00 */
/*TODO*///	add_ix_rg,	add_ix_rgd,	add_ix_in,	add_ix_ind,	add_ix_de,	add_ix_ded,	add_ix_ix,	add_ix_ixd,
/*TODO*///	add_ix_rg,	add_ix_rgd,	add_ix_in,	add_ix_ind,	add_ix_de,	add_ix_ded,	add_ix_ix,	add_ix_ixd,
/*TODO*///	add_ix_rg,	add_ix_rgd,	add_ix_in,	add_ix_ind,	add_ix_de,	add_ix_ded,	add_ix_ix,	add_ix_ixd,
/*TODO*///	add_ix_rg,	add_ix_rgd,	add_ix_in,	add_ix_ind,	add_ix_de,	add_ix_ded,	add_ix_ix,	add_ix_ixd,
/*TODO*///	/* 0x6d00 */
/*TODO*///	add_ix_rg,	add_ix_rgd,	add_ix_in,	add_ix_ind,	add_ix_de,	add_ix_ded,	add_ix_ix,	add_ix_ixd,
/*TODO*///	add_ix_rg,	add_ix_rgd,	add_ix_in,	add_ix_ind,	add_ix_de,	add_ix_ded,	add_ix_ix,	add_ix_ixd,
/*TODO*///	add_ix_rg,	add_ix_rgd,	add_ix_in,	add_ix_ind,	add_ix_de,	add_ix_ded,	add_ix_ix,	add_ix_ixd,
/*TODO*///	add_ix_rg,	add_ix_rgd,	add_ix_in,	add_ix_ind,	add_ix_de,	add_ix_ded,	add_ix_ix,	add_ix_ixd,
/*TODO*///	/* 0x6e00 */
/*TODO*///	add_ixd_rg,	add_ixd_rgd,add_ixd_in,	add_ixd_ind,add_ixd_de,	add_ixd_ded,add_ixd_ix,	add_ixd_ixd,
/*TODO*///	add_ixd_rg,	add_ixd_rgd,add_ixd_in,	add_ixd_ind,add_ixd_de,	add_ixd_ded,add_ixd_ix,	add_ixd_ixd,
/*TODO*///	add_ixd_rg,	add_ixd_rgd,add_ixd_in,	add_ixd_ind,add_ixd_de,	add_ixd_ded,add_ixd_ix,	add_ixd_ixd,
/*TODO*///	add_ixd_rg,	add_ixd_rgd,add_ixd_in,	add_ixd_ind,add_ixd_de,	add_ixd_ded,add_ixd_ix,	add_ixd_ixd,
/*TODO*///	/* 0x6f00 */
/*TODO*///	add_ixd_rg,	add_ixd_rgd,add_ixd_in,	add_ixd_ind,add_ixd_de,	add_ixd_ded,add_ixd_ix,	add_ixd_ixd,
/*TODO*///	add_ixd_rg,	add_ixd_rgd,add_ixd_in,	add_ixd_ind,add_ixd_de,	add_ixd_ded,add_ixd_ix,	add_ixd_ixd,
/*TODO*///	add_ixd_rg,	add_ixd_rgd,add_ixd_in,	add_ixd_ind,add_ixd_de,	add_ixd_ded,add_ixd_ix,	add_ixd_ixd,
/*TODO*///	add_ixd_rg,	add_ixd_rgd,add_ixd_in,	add_ixd_ind,add_ixd_de,	add_ixd_ded,add_ixd_ix,	add_ixd_ixd,
/*TODO*///
/*TODO*///	/* 0x7000 */
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	/* 0x7100 */
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	/* 0x7200 */
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	/* 0x7300 */
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	/* 0x7400 */
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	/* 0x7500 */
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	/* 0x7600 */
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	/* 0x7700 */
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	/* 0x7800 */
/*TODO*///	xor_rg,		xor_rgd,	xor_in,		xor_ind,	xor_de,		xor_ded,	xor_ix,		xor_ixd,
/*TODO*///	xor_rg,		xor_rgd,	xor_in,		xor_ind,	xor_de,		xor_ded,	xor_ix,		xor_ixd,
/*TODO*///	xor_rg,		xor_rgd,	xor_in,		xor_ind,	xor_de,		xor_ded,	xor_ix,		xor_ixd,
/*TODO*///	xor_rg,		xor_rgd,	xor_in,		xor_ind,	xor_de,		xor_ded,	xor_ix,		xor_ixd,
/*TODO*///	/* 0x7900 */
/*TODO*///	xor_rg,		xor_rgd,	xor_in,		xor_ind,	xor_de,		xor_ded,	xor_ix,		xor_ixd,
/*TODO*///	xor_rg,		xor_rgd,	xor_in,		xor_ind,	xor_de,		xor_ded,	xor_ix,		xor_ixd,
/*TODO*///	xor_rg,		xor_rgd,	xor_in,		xor_ind,	xor_de,		xor_ded,	xor_ix,		xor_ixd,
/*TODO*///	xor_rg,		xor_rgd,	xor_in,		xor_ind,	xor_de,		xor_ded,	xor_ix,		xor_ixd,
/*TODO*///	/* 0x7a00 */
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	/* 0x7b00 */
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	/* 0x7c00 */
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	/* 0x7d00 */
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	/* 0x7e00 */
/*TODO*///	sob,		sob,		sob,		sob,		sob,		sob,		sob,		sob,
/*TODO*///	sob,		sob,		sob,		sob,		sob,		sob,		sob,		sob,
/*TODO*///	sob,		sob,		sob,		sob,		sob,		sob,		sob,		sob,
/*TODO*///	sob,		sob,		sob,		sob,		sob,		sob,		sob,		sob,	
/*TODO*///	/* 0x7f00 */
/*TODO*///	sob,		sob,		sob,		sob,		sob,		sob,		sob,		sob,
/*TODO*///	sob,		sob,		sob,		sob,		sob,		sob,		sob,		sob,
/*TODO*///	sob,		sob,		sob,		sob,		sob,		sob,		sob,		sob,
/*TODO*///	sob,		sob,		sob,		sob,		sob,		sob,		sob,		sob,	
/*TODO*///
/*TODO*///	/* 0x8000 */
/*TODO*///	bpl,		bpl,		bpl,		bpl,		bpl,		bpl,		bpl,		bpl,
/*TODO*///	bpl,		bpl,		bpl,		bpl,		bpl,		bpl,		bpl,		bpl,
/*TODO*///	bpl,		bpl,		bpl,		bpl,		bpl,		bpl,		bpl,		bpl,
/*TODO*///	bpl,		bpl,		bpl,		bpl,		bpl,		bpl,		bpl,		bpl,	
/*TODO*///	/* 0x8100 */
/*TODO*///	bmi,		bmi,		bmi,		bmi,		bmi,		bmi,		bmi,		bmi,
/*TODO*///	bmi,		bmi,		bmi,		bmi,		bmi,		bmi,		bmi,		bmi,
/*TODO*///	bmi,		bmi,		bmi,		bmi,		bmi,		bmi,		bmi,		bmi,
/*TODO*///	bmi,		bmi,		bmi,		bmi,		bmi,		bmi,		bmi,		bmi,	
/*TODO*///	/* 0x8200 */
/*TODO*///	bhi,		bhi,		bhi,		bhi,		bhi,		bhi,		bhi,		bhi,
/*TODO*///	bhi,		bhi,		bhi,		bhi,		bhi,		bhi,		bhi,		bhi,
/*TODO*///	bhi,		bhi,		bhi,		bhi,		bhi,		bhi,		bhi,		bhi,
/*TODO*///	bhi,		bhi,		bhi,		bhi,		bhi,		bhi,		bhi,		bhi,	
/*TODO*///	/* 0x8300 */
/*TODO*///	blos,		blos,		blos,		blos,		blos,		blos,		blos,		blos,
/*TODO*///	blos,		blos,		blos,		blos,		blos,		blos,		blos,		blos,
/*TODO*///	blos,		blos,		blos,		blos,		blos,		blos,		blos,		blos,
/*TODO*///	blos,		blos,		blos,		blos,		blos,		blos,		blos,		blos,	
/*TODO*///	/* 0x8400 */
/*TODO*///	bvc,		bvc,		bvc,		bvc,		bvc,		bvc,		bvc,		bvc,
/*TODO*///	bvc,		bvc,		bvc,		bvc,		bvc,		bvc,		bvc,		bvc,
/*TODO*///	bvc,		bvc,		bvc,		bvc,		bvc,		bvc,		bvc,		bvc,
/*TODO*///	bvc,		bvc,		bvc,		bvc,		bvc,		bvc,		bvc,		bvc,	
/*TODO*///	/* 0x8500 */
/*TODO*///	bvs,		bvs,		bvs,		bvs,		bvs,		bvs,		bvs,		bvs,
/*TODO*///	bvs,		bvs,		bvs,		bvs,		bvs,		bvs,		bvs,		bvs,
/*TODO*///	bvs,		bvs,		bvs,		bvs,		bvs,		bvs,		bvs,		bvs,
/*TODO*///	bvs,		bvs,		bvs,		bvs,		bvs,		bvs,		bvs,		bvs,	
/*TODO*///	/* 0x8600 */
/*TODO*///	bcc,		bcc,		bcc,		bcc,		bcc,		bcc,		bcc,		bcc,
/*TODO*///	bcc,		bcc,		bcc,		bcc,		bcc,		bcc,		bcc,		bcc,
/*TODO*///	bcc,		bcc,		bcc,		bcc,		bcc,		bcc,		bcc,		bcc,
/*TODO*///	bcc,		bcc,		bcc,		bcc,		bcc,		bcc,		bcc,		bcc,	
/*TODO*///	/* 0x8700 */
/*TODO*///	bcs,		bcs,		bcs,		bcs,		bcs,		bcs,		bcs,		bcs,
/*TODO*///	bcs,		bcs,		bcs,		bcs,		bcs,		bcs,		bcs,		bcs,
/*TODO*///	bcs,		bcs,		bcs,		bcs,		bcs,		bcs,		bcs,		bcs,
/*TODO*///	bcs,		bcs,		bcs,		bcs,		bcs,		bcs,		bcs,		bcs,	
/*TODO*///	/* 0x8800 */
/*TODO*///	emt,		emt,		emt,		emt,		emt,		emt,		emt,		emt,
/*TODO*///	emt,		emt,		emt,		emt,		emt,		emt,		emt,		emt,
/*TODO*///	emt,		emt,		emt,		emt,		emt,		emt,		emt,		emt,
/*TODO*///	emt,		emt,		emt,		emt,		emt,		emt,		emt,		emt,	
/*TODO*///	/* 0x8900 */
/*TODO*///	trap,		trap,		trap,		trap,		trap,		trap,		trap,		trap,
/*TODO*///	trap,		trap,		trap,		trap,		trap,		trap,		trap,		trap,
/*TODO*///	trap,		trap,		trap,		trap,		trap,		trap,		trap,		trap,
/*TODO*///	trap,		trap,		trap,		trap,		trap,		trap,		trap,		trap,	
/*TODO*///	/* 0x8a00 */
/*TODO*///	clrb_rg,	clrb_rgd,	clrb_in,	clrb_ind,	clrb_de,	clrb_ded,	clrb_ix,	clrb_ixd,
/*TODO*///	comb_rg,	comb_rgd,	comb_in,	comb_ind,	comb_de,	comb_ded,	comb_ix,	comb_ixd,
/*TODO*///	incb_rg,	incb_rgd,	incb_in,	incb_ind,	incb_de,	incb_ded,	incb_ix,	incb_ixd,
/*TODO*///	decb_rg,	decb_rgd,	decb_in,	decb_ind,	decb_de,	decb_ded,	decb_ix,	decb_ixd,
/*TODO*///	/* 0x8b00 */
/*TODO*///	negb_rg,	negb_rgd,	negb_in,	negb_ind,	negb_de,	negb_ded,	negb_ix,	negb_ixd,
/*TODO*///	adcb_rg,	adcb_rgd,	adcb_in,	adcb_ind,	adcb_de,	adcb_ded,	adcb_ix,	adcb_ixd,
/*TODO*///	sbcb_rg,	sbcb_rgd,	sbcb_in,	sbcb_ind,	sbcb_de,	sbcb_ded,	sbcb_ix,	sbcb_ixd,
/*TODO*///	tstb_rg,	tstb_rgd,	tstb_in,	tstb_ind,	tstb_de,	tstb_ded,	tstb_ix,	tstb_ixd,
/*TODO*///	/* 0x8c00 */
/*TODO*///	rorb_rg,	rorb_rgd,	rorb_in,	rorb_ind,	rorb_de,	rorb_ded,	rorb_ix,	rorb_ixd,
/*TODO*///	rolb_rg,	rolb_rgd,	rolb_in,	rolb_ind,	rolb_de,	rolb_ded,	rolb_ix,	rolb_ixd,
/*TODO*///	asrb_rg,	asrb_rgd,	asrb_in,	asrb_ind,	asrb_de,	asrb_ded,	asrb_ix,	asrb_ixd,
/*TODO*///	aslb_rg,	aslb_rgd,	aslb_in,	aslb_ind,	aslb_de,	aslb_ded,	aslb_ix,	aslb_ixd,
/*TODO*///	/* 0x8d00 */
/*TODO*///	mtps_rg,	mtps_rgd,	mtps_in,	mtps_ind,	mtps_de,	mtps_ded,	mtps_ix,	mtps_ixd,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	mfps_rg,	mfps_rgd,	mfps_in,	mfps_ind,	mfps_de,	mfps_ded,	mfps_ix,	mfps_ixd,
/*TODO*///	/* 0x8e00 */
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	/* 0x8f00 */
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///
/*TODO*///	/* 0x9000 */
/*TODO*///	movb_rg_rg,	movb_rg_rgd,movb_rg_in,	movb_rg_ind,movb_rg_de,	movb_rg_ded,movb_rg_ix,	movb_rg_ixd,
/*TODO*///	movb_rg_rg,	movb_rg_rgd,movb_rg_in,	movb_rg_ind,movb_rg_de,	movb_rg_ded,movb_rg_ix,	movb_rg_ixd,
/*TODO*///	movb_rg_rg,	movb_rg_rgd,movb_rg_in,	movb_rg_ind,movb_rg_de,	movb_rg_ded,movb_rg_ix,	movb_rg_ixd,
/*TODO*///	movb_rg_rg,	movb_rg_rgd,movb_rg_in,	movb_rg_ind,movb_rg_de,	movb_rg_ded,movb_rg_ix,	movb_rg_ixd,
/*TODO*///	/* 0x9100 */
/*TODO*///	movb_rg_rg,	movb_rg_rgd,movb_rg_in,	movb_rg_ind,movb_rg_de,	movb_rg_ded,movb_rg_ix,	movb_rg_ixd,
/*TODO*///	movb_rg_rg,	movb_rg_rgd,movb_rg_in,	movb_rg_ind,movb_rg_de,	movb_rg_ded,movb_rg_ix,	movb_rg_ixd,
/*TODO*///	movb_rg_rg,	movb_rg_rgd,movb_rg_in,	movb_rg_ind,movb_rg_de,	movb_rg_ded,movb_rg_ix,	movb_rg_ixd,
/*TODO*///	movb_rg_rg,	movb_rg_rgd,movb_rg_in,	movb_rg_ind,movb_rg_de,	movb_rg_ded,movb_rg_ix,	movb_rg_ixd,
/*TODO*///	/* 0x9200 */
/*TODO*///	movb_rgd_rg,movb_rgd_rgd,movb_rgd_in,movb_rgd_ind,movb_rgd_de,movb_rgd_ded,movb_rgd_ix,movb_rgd_ixd,
/*TODO*///	movb_rgd_rg,movb_rgd_rgd,movb_rgd_in,movb_rgd_ind,movb_rgd_de,movb_rgd_ded,movb_rgd_ix,movb_rgd_ixd,
/*TODO*///	movb_rgd_rg,movb_rgd_rgd,movb_rgd_in,movb_rgd_ind,movb_rgd_de,movb_rgd_ded,movb_rgd_ix,movb_rgd_ixd,
/*TODO*///	movb_rgd_rg,movb_rgd_rgd,movb_rgd_in,movb_rgd_ind,movb_rgd_de,movb_rgd_ded,movb_rgd_ix,movb_rgd_ixd,
/*TODO*///	/* 0x9300 */
/*TODO*///	movb_rgd_rg,movb_rgd_rgd,movb_rgd_in,movb_rgd_ind,movb_rgd_de,movb_rgd_ded,movb_rgd_ix,movb_rgd_ixd,
/*TODO*///	movb_rgd_rg,movb_rgd_rgd,movb_rgd_in,movb_rgd_ind,movb_rgd_de,movb_rgd_ded,movb_rgd_ix,movb_rgd_ixd,
/*TODO*///	movb_rgd_rg,movb_rgd_rgd,movb_rgd_in,movb_rgd_ind,movb_rgd_de,movb_rgd_ded,movb_rgd_ix,movb_rgd_ixd,
/*TODO*///	movb_rgd_rg,movb_rgd_rgd,movb_rgd_in,movb_rgd_ind,movb_rgd_de,movb_rgd_ded,movb_rgd_ix,movb_rgd_ixd,
/*TODO*///	/* 0x9400 */
/*TODO*///	movb_in_rg,	movb_in_rgd,movb_in_in,	movb_in_ind,movb_in_de,	movb_in_ded,movb_in_ix,	movb_in_ixd,
/*TODO*///	movb_in_rg,	movb_in_rgd,movb_in_in,	movb_in_ind,movb_in_de,	movb_in_ded,movb_in_ix,	movb_in_ixd,
/*TODO*///	movb_in_rg,	movb_in_rgd,movb_in_in,	movb_in_ind,movb_in_de,	movb_in_ded,movb_in_ix,	movb_in_ixd,
/*TODO*///	movb_in_rg,	movb_in_rgd,movb_in_in,	movb_in_ind,movb_in_de,	movb_in_ded,movb_in_ix,	movb_in_ixd,
/*TODO*///	/* 0x9500 */
/*TODO*///	movb_in_rg,	movb_in_rgd,movb_in_in,	movb_in_ind,movb_in_de,	movb_in_ded,movb_in_ix,	movb_in_ixd,
/*TODO*///	movb_in_rg,	movb_in_rgd,movb_in_in,	movb_in_ind,movb_in_de,	movb_in_ded,movb_in_ix,	movb_in_ixd,
/*TODO*///	movb_in_rg,	movb_in_rgd,movb_in_in,	movb_in_ind,movb_in_de,	movb_in_ded,movb_in_ix,	movb_in_ixd,
/*TODO*///	movb_in_rg,	movb_in_rgd,movb_in_in,	movb_in_ind,movb_in_de,	movb_in_ded,movb_in_ix,	movb_in_ixd,
/*TODO*///	/* 0x9600 */
/*TODO*///	movb_ind_rg,movb_ind_rgd,movb_ind_in,movb_ind_ind,movb_ind_de,movb_ind_ded,movb_ind_ix,movb_ind_ixd,
/*TODO*///	movb_ind_rg,movb_ind_rgd,movb_ind_in,movb_ind_ind,movb_ind_de,movb_ind_ded,movb_ind_ix,movb_ind_ixd,
/*TODO*///	movb_ind_rg,movb_ind_rgd,movb_ind_in,movb_ind_ind,movb_ind_de,movb_ind_ded,movb_ind_ix,movb_ind_ixd,
/*TODO*///	movb_ind_rg,movb_ind_rgd,movb_ind_in,movb_ind_ind,movb_ind_de,movb_ind_ded,movb_ind_ix,movb_ind_ixd,
/*TODO*///	/* 0x9700 */
/*TODO*///	movb_ind_rg,movb_ind_rgd,movb_ind_in,movb_ind_ind,movb_ind_de,movb_ind_ded,movb_ind_ix,movb_ind_ixd,
/*TODO*///	movb_ind_rg,movb_ind_rgd,movb_ind_in,movb_ind_ind,movb_ind_de,movb_ind_ded,movb_ind_ix,movb_ind_ixd,
/*TODO*///	movb_ind_rg,movb_ind_rgd,movb_ind_in,movb_ind_ind,movb_ind_de,movb_ind_ded,movb_ind_ix,movb_ind_ixd,
/*TODO*///	movb_ind_rg,movb_ind_rgd,movb_ind_in,movb_ind_ind,movb_ind_de,movb_ind_ded,movb_ind_ix,movb_ind_ixd,
/*TODO*///	/* 0x9800 */
/*TODO*///	movb_de_rg,	movb_de_rgd,movb_de_in,	movb_de_ind,movb_de_de,	movb_de_ded,movb_de_ix,	movb_de_ixd,
/*TODO*///	movb_de_rg,	movb_de_rgd,movb_de_in,	movb_de_ind,movb_de_de,	movb_de_ded,movb_de_ix,	movb_de_ixd,
/*TODO*///	movb_de_rg,	movb_de_rgd,movb_de_in,	movb_de_ind,movb_de_de,	movb_de_ded,movb_de_ix,	movb_de_ixd,
/*TODO*///	movb_de_rg,	movb_de_rgd,movb_de_in,	movb_de_ind,movb_de_de,	movb_de_ded,movb_de_ix,	movb_de_ixd,
/*TODO*///	/* 0x9900 */
/*TODO*///	movb_de_rg,	movb_de_rgd,movb_de_in,	movb_de_ind,movb_de_de,	movb_de_ded,movb_de_ix,	movb_de_ixd,
/*TODO*///	movb_de_rg,	movb_de_rgd,movb_de_in,	movb_de_ind,movb_de_de,	movb_de_ded,movb_de_ix,	movb_de_ixd,
/*TODO*///	movb_de_rg,	movb_de_rgd,movb_de_in,	movb_de_ind,movb_de_de,	movb_de_ded,movb_de_ix,	movb_de_ixd,
/*TODO*///	movb_de_rg,	movb_de_rgd,movb_de_in,	movb_de_ind,movb_de_de,	movb_de_ded,movb_de_ix,	movb_de_ixd,
/*TODO*///	/* 0x9a00 */
/*TODO*///	movb_ded_rg,movb_ded_rgd,movb_ded_in,movb_ded_ind,movb_ded_de,movb_ded_ded,movb_ded_ix,movb_ded_ixd,
/*TODO*///	movb_ded_rg,movb_ded_rgd,movb_ded_in,movb_ded_ind,movb_ded_de,movb_ded_ded,movb_ded_ix,movb_ded_ixd,
/*TODO*///	movb_ded_rg,movb_ded_rgd,movb_ded_in,movb_ded_ind,movb_ded_de,movb_ded_ded,movb_ded_ix,movb_ded_ixd,
/*TODO*///	movb_ded_rg,movb_ded_rgd,movb_ded_in,movb_ded_ind,movb_ded_de,movb_ded_ded,movb_ded_ix,movb_ded_ixd,
/*TODO*///	/* 0x9b00 */
/*TODO*///	movb_ded_rg,movb_ded_rgd,movb_ded_in,movb_ded_ind,movb_ded_de,movb_ded_ded,movb_ded_ix,movb_ded_ixd,
/*TODO*///	movb_ded_rg,movb_ded_rgd,movb_ded_in,movb_ded_ind,movb_ded_de,movb_ded_ded,movb_ded_ix,movb_ded_ixd,
/*TODO*///	movb_ded_rg,movb_ded_rgd,movb_ded_in,movb_ded_ind,movb_ded_de,movb_ded_ded,movb_ded_ix,movb_ded_ixd,
/*TODO*///	movb_ded_rg,movb_ded_rgd,movb_ded_in,movb_ded_ind,movb_ded_de,movb_ded_ded,movb_ded_ix,movb_ded_ixd,
/*TODO*///	/* 0x9c00 */
/*TODO*///	movb_ix_rg,	movb_ix_rgd,movb_ix_in,	movb_ix_ind,movb_ix_de,	movb_ix_ded,movb_ix_ix,	movb_ix_ixd,
/*TODO*///	movb_ix_rg,	movb_ix_rgd,movb_ix_in,	movb_ix_ind,movb_ix_de,	movb_ix_ded,movb_ix_ix,	movb_ix_ixd,
/*TODO*///	movb_ix_rg,	movb_ix_rgd,movb_ix_in,	movb_ix_ind,movb_ix_de,	movb_ix_ded,movb_ix_ix,	movb_ix_ixd,
/*TODO*///	movb_ix_rg,	movb_ix_rgd,movb_ix_in,	movb_ix_ind,movb_ix_de,	movb_ix_ded,movb_ix_ix,	movb_ix_ixd,
/*TODO*///	/* 0x9d00 */
/*TODO*///	movb_ix_rg,	movb_ix_rgd,movb_ix_in,	movb_ix_ind,movb_ix_de,	movb_ix_ded,movb_ix_ix,	movb_ix_ixd,
/*TODO*///	movb_ix_rg,	movb_ix_rgd,movb_ix_in,	movb_ix_ind,movb_ix_de,	movb_ix_ded,movb_ix_ix,	movb_ix_ixd,
/*TODO*///	movb_ix_rg,	movb_ix_rgd,movb_ix_in,	movb_ix_ind,movb_ix_de,	movb_ix_ded,movb_ix_ix,	movb_ix_ixd,
/*TODO*///	movb_ix_rg,	movb_ix_rgd,movb_ix_in,	movb_ix_ind,movb_ix_de,	movb_ix_ded,movb_ix_ix,	movb_ix_ixd,
/*TODO*///	/* 0x9e00 */
/*TODO*///	movb_ixd_rg,movb_ixd_rgd,movb_ixd_in,movb_ixd_ind,movb_ixd_de,movb_ixd_ded,movb_ixd_ix,movb_ixd_ixd,
/*TODO*///	movb_ixd_rg,movb_ixd_rgd,movb_ixd_in,movb_ixd_ind,movb_ixd_de,movb_ixd_ded,movb_ixd_ix,movb_ixd_ixd,
/*TODO*///	movb_ixd_rg,movb_ixd_rgd,movb_ixd_in,movb_ixd_ind,movb_ixd_de,movb_ixd_ded,movb_ixd_ix,movb_ixd_ixd,
/*TODO*///	movb_ixd_rg,movb_ixd_rgd,movb_ixd_in,movb_ixd_ind,movb_ixd_de,movb_ixd_ded,movb_ixd_ix,movb_ixd_ixd,
/*TODO*///	/* 0x9f00 */
/*TODO*///	movb_ixd_rg,movb_ixd_rgd,movb_ixd_in,movb_ixd_ind,movb_ixd_de,movb_ixd_ded,movb_ixd_ix,movb_ixd_ixd,
/*TODO*///	movb_ixd_rg,movb_ixd_rgd,movb_ixd_in,movb_ixd_ind,movb_ixd_de,movb_ixd_ded,movb_ixd_ix,movb_ixd_ixd,
/*TODO*///	movb_ixd_rg,movb_ixd_rgd,movb_ixd_in,movb_ixd_ind,movb_ixd_de,movb_ixd_ded,movb_ixd_ix,movb_ixd_ixd,
/*TODO*///	movb_ixd_rg,movb_ixd_rgd,movb_ixd_in,movb_ixd_ind,movb_ixd_de,movb_ixd_ded,movb_ixd_ix,movb_ixd_ixd,
/*TODO*///
/*TODO*///	/* 0xa000 */
/*TODO*///	cmpb_rg_rg,	cmpb_rg_rgd,cmpb_rg_in,	cmpb_rg_ind,cmpb_rg_de,	cmpb_rg_ded,cmpb_rg_ix,	cmpb_rg_ixd,
/*TODO*///	cmpb_rg_rg,	cmpb_rg_rgd,cmpb_rg_in,	cmpb_rg_ind,cmpb_rg_de,	cmpb_rg_ded,cmpb_rg_ix,	cmpb_rg_ixd,
/*TODO*///	cmpb_rg_rg,	cmpb_rg_rgd,cmpb_rg_in,	cmpb_rg_ind,cmpb_rg_de,	cmpb_rg_ded,cmpb_rg_ix,	cmpb_rg_ixd,
/*TODO*///	cmpb_rg_rg,	cmpb_rg_rgd,cmpb_rg_in,	cmpb_rg_ind,cmpb_rg_de,	cmpb_rg_ded,cmpb_rg_ix,	cmpb_rg_ixd,
/*TODO*///	/* 0xa100 */
/*TODO*///	cmpb_rg_rg,	cmpb_rg_rgd,cmpb_rg_in,	cmpb_rg_ind,cmpb_rg_de,	cmpb_rg_ded,cmpb_rg_ix,	cmpb_rg_ixd,
/*TODO*///	cmpb_rg_rg,	cmpb_rg_rgd,cmpb_rg_in,	cmpb_rg_ind,cmpb_rg_de,	cmpb_rg_ded,cmpb_rg_ix,	cmpb_rg_ixd,
/*TODO*///	cmpb_rg_rg,	cmpb_rg_rgd,cmpb_rg_in,	cmpb_rg_ind,cmpb_rg_de,	cmpb_rg_ded,cmpb_rg_ix,	cmpb_rg_ixd,
/*TODO*///	cmpb_rg_rg,	cmpb_rg_rgd,cmpb_rg_in,	cmpb_rg_ind,cmpb_rg_de,	cmpb_rg_ded,cmpb_rg_ix,	cmpb_rg_ixd,
/*TODO*///	/* 0xa200 */
/*TODO*///	cmpb_rgd_rg,cmpb_rgd_rgd,cmpb_rgd_in,cmpb_rgd_ind,cmpb_rgd_de,cmpb_rgd_ded,cmpb_rgd_ix,cmpb_rgd_ixd,
/*TODO*///	cmpb_rgd_rg,cmpb_rgd_rgd,cmpb_rgd_in,cmpb_rgd_ind,cmpb_rgd_de,cmpb_rgd_ded,cmpb_rgd_ix,cmpb_rgd_ixd,
/*TODO*///	cmpb_rgd_rg,cmpb_rgd_rgd,cmpb_rgd_in,cmpb_rgd_ind,cmpb_rgd_de,cmpb_rgd_ded,cmpb_rgd_ix,cmpb_rgd_ixd,
/*TODO*///	cmpb_rgd_rg,cmpb_rgd_rgd,cmpb_rgd_in,cmpb_rgd_ind,cmpb_rgd_de,cmpb_rgd_ded,cmpb_rgd_ix,cmpb_rgd_ixd,
/*TODO*///	/* 0xa300 */
/*TODO*///	cmpb_rgd_rg,cmpb_rgd_rgd,cmpb_rgd_in,cmpb_rgd_ind,cmpb_rgd_de,cmpb_rgd_ded,cmpb_rgd_ix,cmpb_rgd_ixd,
/*TODO*///	cmpb_rgd_rg,cmpb_rgd_rgd,cmpb_rgd_in,cmpb_rgd_ind,cmpb_rgd_de,cmpb_rgd_ded,cmpb_rgd_ix,cmpb_rgd_ixd,
/*TODO*///	cmpb_rgd_rg,cmpb_rgd_rgd,cmpb_rgd_in,cmpb_rgd_ind,cmpb_rgd_de,cmpb_rgd_ded,cmpb_rgd_ix,cmpb_rgd_ixd,
/*TODO*///	cmpb_rgd_rg,cmpb_rgd_rgd,cmpb_rgd_in,cmpb_rgd_ind,cmpb_rgd_de,cmpb_rgd_ded,cmpb_rgd_ix,cmpb_rgd_ixd,
/*TODO*///	/* 0xa400 */
/*TODO*///	cmpb_in_rg,	cmpb_in_rgd,cmpb_in_in,	cmpb_in_ind,cmpb_in_de,	cmpb_in_ded,cmpb_in_ix,	cmpb_in_ixd,
/*TODO*///	cmpb_in_rg,	cmpb_in_rgd,cmpb_in_in,	cmpb_in_ind,cmpb_in_de,	cmpb_in_ded,cmpb_in_ix,	cmpb_in_ixd,
/*TODO*///	cmpb_in_rg,	cmpb_in_rgd,cmpb_in_in,	cmpb_in_ind,cmpb_in_de,	cmpb_in_ded,cmpb_in_ix,	cmpb_in_ixd,
/*TODO*///	cmpb_in_rg,	cmpb_in_rgd,cmpb_in_in,	cmpb_in_ind,cmpb_in_de,	cmpb_in_ded,cmpb_in_ix,	cmpb_in_ixd,
/*TODO*///	/* 0xa500 */
/*TODO*///	cmpb_in_rg,	cmpb_in_rgd,cmpb_in_in,	cmpb_in_ind,cmpb_in_de,	cmpb_in_ded,cmpb_in_ix,	cmpb_in_ixd,
/*TODO*///	cmpb_in_rg,	cmpb_in_rgd,cmpb_in_in,	cmpb_in_ind,cmpb_in_de,	cmpb_in_ded,cmpb_in_ix,	cmpb_in_ixd,
/*TODO*///	cmpb_in_rg,	cmpb_in_rgd,cmpb_in_in,	cmpb_in_ind,cmpb_in_de,	cmpb_in_ded,cmpb_in_ix,	cmpb_in_ixd,
/*TODO*///	cmpb_in_rg,	cmpb_in_rgd,cmpb_in_in,	cmpb_in_ind,cmpb_in_de,	cmpb_in_ded,cmpb_in_ix,	cmpb_in_ixd,
/*TODO*///	/* 0xa600 */
/*TODO*///	cmpb_ind_rg,cmpb_ind_rgd,cmpb_ind_in,cmpb_ind_ind,cmpb_ind_de,cmpb_ind_ded,cmpb_ind_ix,cmpb_ind_ixd,
/*TODO*///	cmpb_ind_rg,cmpb_ind_rgd,cmpb_ind_in,cmpb_ind_ind,cmpb_ind_de,cmpb_ind_ded,cmpb_ind_ix,cmpb_ind_ixd,
/*TODO*///	cmpb_ind_rg,cmpb_ind_rgd,cmpb_ind_in,cmpb_ind_ind,cmpb_ind_de,cmpb_ind_ded,cmpb_ind_ix,cmpb_ind_ixd,
/*TODO*///	cmpb_ind_rg,cmpb_ind_rgd,cmpb_ind_in,cmpb_ind_ind,cmpb_ind_de,cmpb_ind_ded,cmpb_ind_ix,cmpb_ind_ixd,
/*TODO*///	/* 0xa700 */
/*TODO*///	cmpb_ind_rg,cmpb_ind_rgd,cmpb_ind_in,cmpb_ind_ind,cmpb_ind_de,cmpb_ind_ded,cmpb_ind_ix,cmpb_ind_ixd,
/*TODO*///	cmpb_ind_rg,cmpb_ind_rgd,cmpb_ind_in,cmpb_ind_ind,cmpb_ind_de,cmpb_ind_ded,cmpb_ind_ix,cmpb_ind_ixd,
/*TODO*///	cmpb_ind_rg,cmpb_ind_rgd,cmpb_ind_in,cmpb_ind_ind,cmpb_ind_de,cmpb_ind_ded,cmpb_ind_ix,cmpb_ind_ixd,
/*TODO*///	cmpb_ind_rg,cmpb_ind_rgd,cmpb_ind_in,cmpb_ind_ind,cmpb_ind_de,cmpb_ind_ded,cmpb_ind_ix,cmpb_ind_ixd,
/*TODO*///	/* 0xa800 */
/*TODO*///	cmpb_de_rg,	cmpb_de_rgd,cmpb_de_in,	cmpb_de_ind,cmpb_de_de,	cmpb_de_ded,cmpb_de_ix,	cmpb_de_ixd,
/*TODO*///	cmpb_de_rg,	cmpb_de_rgd,cmpb_de_in,	cmpb_de_ind,cmpb_de_de,	cmpb_de_ded,cmpb_de_ix,	cmpb_de_ixd,
/*TODO*///	cmpb_de_rg,	cmpb_de_rgd,cmpb_de_in,	cmpb_de_ind,cmpb_de_de,	cmpb_de_ded,cmpb_de_ix,	cmpb_de_ixd,
/*TODO*///	cmpb_de_rg,	cmpb_de_rgd,cmpb_de_in,	cmpb_de_ind,cmpb_de_de,	cmpb_de_ded,cmpb_de_ix,	cmpb_de_ixd,
/*TODO*///	/* 0xa900 */
/*TODO*///	cmpb_de_rg,	cmpb_de_rgd,cmpb_de_in,	cmpb_de_ind,cmpb_de_de,	cmpb_de_ded,cmpb_de_ix,	cmpb_de_ixd,
/*TODO*///	cmpb_de_rg,	cmpb_de_rgd,cmpb_de_in,	cmpb_de_ind,cmpb_de_de,	cmpb_de_ded,cmpb_de_ix,	cmpb_de_ixd,
/*TODO*///	cmpb_de_rg,	cmpb_de_rgd,cmpb_de_in,	cmpb_de_ind,cmpb_de_de,	cmpb_de_ded,cmpb_de_ix,	cmpb_de_ixd,
/*TODO*///	cmpb_de_rg,	cmpb_de_rgd,cmpb_de_in,	cmpb_de_ind,cmpb_de_de,	cmpb_de_ded,cmpb_de_ix,	cmpb_de_ixd,
/*TODO*///	/* 0xaa00 */
/*TODO*///	cmpb_ded_rg,cmpb_ded_rgd,cmpb_ded_in,cmpb_ded_ind,cmpb_ded_de,cmpb_ded_ded,cmpb_ded_ix,cmpb_ded_ixd,
/*TODO*///	cmpb_ded_rg,cmpb_ded_rgd,cmpb_ded_in,cmpb_ded_ind,cmpb_ded_de,cmpb_ded_ded,cmpb_ded_ix,cmpb_ded_ixd,
/*TODO*///	cmpb_ded_rg,cmpb_ded_rgd,cmpb_ded_in,cmpb_ded_ind,cmpb_ded_de,cmpb_ded_ded,cmpb_ded_ix,cmpb_ded_ixd,
/*TODO*///	cmpb_ded_rg,cmpb_ded_rgd,cmpb_ded_in,cmpb_ded_ind,cmpb_ded_de,cmpb_ded_ded,cmpb_ded_ix,cmpb_ded_ixd,
/*TODO*///	/* 0xab00 */
/*TODO*///	cmpb_ded_rg,cmpb_ded_rgd,cmpb_ded_in,cmpb_ded_ind,cmpb_ded_de,cmpb_ded_ded,cmpb_ded_ix,cmpb_ded_ixd,
/*TODO*///	cmpb_ded_rg,cmpb_ded_rgd,cmpb_ded_in,cmpb_ded_ind,cmpb_ded_de,cmpb_ded_ded,cmpb_ded_ix,cmpb_ded_ixd,
/*TODO*///	cmpb_ded_rg,cmpb_ded_rgd,cmpb_ded_in,cmpb_ded_ind,cmpb_ded_de,cmpb_ded_ded,cmpb_ded_ix,cmpb_ded_ixd,
/*TODO*///	cmpb_ded_rg,cmpb_ded_rgd,cmpb_ded_in,cmpb_ded_ind,cmpb_ded_de,cmpb_ded_ded,cmpb_ded_ix,cmpb_ded_ixd,
/*TODO*///	/* 0xac00 */
/*TODO*///	cmpb_ix_rg,	cmpb_ix_rgd,cmpb_ix_in,	cmpb_ix_ind,cmpb_ix_de,	cmpb_ix_ded,cmpb_ix_ix,	cmpb_ix_ixd,
/*TODO*///	cmpb_ix_rg,	cmpb_ix_rgd,cmpb_ix_in,	cmpb_ix_ind,cmpb_ix_de,	cmpb_ix_ded,cmpb_ix_ix,	cmpb_ix_ixd,
/*TODO*///	cmpb_ix_rg,	cmpb_ix_rgd,cmpb_ix_in,	cmpb_ix_ind,cmpb_ix_de,	cmpb_ix_ded,cmpb_ix_ix,	cmpb_ix_ixd,
/*TODO*///	cmpb_ix_rg,	cmpb_ix_rgd,cmpb_ix_in,	cmpb_ix_ind,cmpb_ix_de,	cmpb_ix_ded,cmpb_ix_ix,	cmpb_ix_ixd,
/*TODO*///	/* 0xad00 */
/*TODO*///	cmpb_ix_rg,	cmpb_ix_rgd,cmpb_ix_in,	cmpb_ix_ind,cmpb_ix_de,	cmpb_ix_ded,cmpb_ix_ix,	cmpb_ix_ixd,
/*TODO*///	cmpb_ix_rg,	cmpb_ix_rgd,cmpb_ix_in,	cmpb_ix_ind,cmpb_ix_de,	cmpb_ix_ded,cmpb_ix_ix,	cmpb_ix_ixd,
/*TODO*///	cmpb_ix_rg,	cmpb_ix_rgd,cmpb_ix_in,	cmpb_ix_ind,cmpb_ix_de,	cmpb_ix_ded,cmpb_ix_ix,	cmpb_ix_ixd,
/*TODO*///	cmpb_ix_rg,	cmpb_ix_rgd,cmpb_ix_in,	cmpb_ix_ind,cmpb_ix_de,	cmpb_ix_ded,cmpb_ix_ix,	cmpb_ix_ixd,
/*TODO*///	/* 0xae00 */
/*TODO*///	cmpb_ixd_rg,cmpb_ixd_rgd,cmpb_ixd_in,cmpb_ixd_ind,cmpb_ixd_de,cmpb_ixd_ded,cmpb_ixd_ix,cmpb_ixd_ixd,
/*TODO*///	cmpb_ixd_rg,cmpb_ixd_rgd,cmpb_ixd_in,cmpb_ixd_ind,cmpb_ixd_de,cmpb_ixd_ded,cmpb_ixd_ix,cmpb_ixd_ixd,
/*TODO*///	cmpb_ixd_rg,cmpb_ixd_rgd,cmpb_ixd_in,cmpb_ixd_ind,cmpb_ixd_de,cmpb_ixd_ded,cmpb_ixd_ix,cmpb_ixd_ixd,
/*TODO*///	cmpb_ixd_rg,cmpb_ixd_rgd,cmpb_ixd_in,cmpb_ixd_ind,cmpb_ixd_de,cmpb_ixd_ded,cmpb_ixd_ix,cmpb_ixd_ixd,
/*TODO*///	/* 0xaf00 */
/*TODO*///	cmpb_ixd_rg,cmpb_ixd_rgd,cmpb_ixd_in,cmpb_ixd_ind,cmpb_ixd_de,cmpb_ixd_ded,cmpb_ixd_ix,cmpb_ixd_ixd,
/*TODO*///	cmpb_ixd_rg,cmpb_ixd_rgd,cmpb_ixd_in,cmpb_ixd_ind,cmpb_ixd_de,cmpb_ixd_ded,cmpb_ixd_ix,cmpb_ixd_ixd,
/*TODO*///	cmpb_ixd_rg,cmpb_ixd_rgd,cmpb_ixd_in,cmpb_ixd_ind,cmpb_ixd_de,cmpb_ixd_ded,cmpb_ixd_ix,cmpb_ixd_ixd,
/*TODO*///	cmpb_ixd_rg,cmpb_ixd_rgd,cmpb_ixd_in,cmpb_ixd_ind,cmpb_ixd_de,cmpb_ixd_ded,cmpb_ixd_ix,cmpb_ixd_ixd,
/*TODO*///
/*TODO*///	/* 0xb000 */
/*TODO*///	bitb_rg_rg,	bitb_rg_rgd,bitb_rg_in,	bitb_rg_ind,bitb_rg_de,	bitb_rg_ded,bitb_rg_ix,	bitb_rg_ixd,
/*TODO*///	bitb_rg_rg,	bitb_rg_rgd,bitb_rg_in,	bitb_rg_ind,bitb_rg_de,	bitb_rg_ded,bitb_rg_ix,	bitb_rg_ixd,
/*TODO*///	bitb_rg_rg,	bitb_rg_rgd,bitb_rg_in,	bitb_rg_ind,bitb_rg_de,	bitb_rg_ded,bitb_rg_ix,	bitb_rg_ixd,
/*TODO*///	bitb_rg_rg,	bitb_rg_rgd,bitb_rg_in,	bitb_rg_ind,bitb_rg_de,	bitb_rg_ded,bitb_rg_ix,	bitb_rg_ixd,
/*TODO*///	/* 0xb100 */
/*TODO*///	bitb_rg_rg,	bitb_rg_rgd,bitb_rg_in,	bitb_rg_ind,bitb_rg_de,	bitb_rg_ded,bitb_rg_ix,	bitb_rg_ixd,
/*TODO*///	bitb_rg_rg,	bitb_rg_rgd,bitb_rg_in,	bitb_rg_ind,bitb_rg_de,	bitb_rg_ded,bitb_rg_ix,	bitb_rg_ixd,
/*TODO*///	bitb_rg_rg,	bitb_rg_rgd,bitb_rg_in,	bitb_rg_ind,bitb_rg_de,	bitb_rg_ded,bitb_rg_ix,	bitb_rg_ixd,
/*TODO*///	bitb_rg_rg,	bitb_rg_rgd,bitb_rg_in,	bitb_rg_ind,bitb_rg_de,	bitb_rg_ded,bitb_rg_ix,	bitb_rg_ixd,
/*TODO*///	/* 0xb200 */
/*TODO*///	bitb_rgd_rg,bitb_rgd_rgd,bitb_rgd_in,bitb_rgd_ind,bitb_rgd_de,bitb_rgd_ded,bitb_rgd_ix,bitb_rgd_ixd,
/*TODO*///	bitb_rgd_rg,bitb_rgd_rgd,bitb_rgd_in,bitb_rgd_ind,bitb_rgd_de,bitb_rgd_ded,bitb_rgd_ix,bitb_rgd_ixd,
/*TODO*///	bitb_rgd_rg,bitb_rgd_rgd,bitb_rgd_in,bitb_rgd_ind,bitb_rgd_de,bitb_rgd_ded,bitb_rgd_ix,bitb_rgd_ixd,
/*TODO*///	bitb_rgd_rg,bitb_rgd_rgd,bitb_rgd_in,bitb_rgd_ind,bitb_rgd_de,bitb_rgd_ded,bitb_rgd_ix,bitb_rgd_ixd,
/*TODO*///	/* 0xb300 */
/*TODO*///	bitb_rgd_rg,bitb_rgd_rgd,bitb_rgd_in,bitb_rgd_ind,bitb_rgd_de,bitb_rgd_ded,bitb_rgd_ix,bitb_rgd_ixd,
/*TODO*///	bitb_rgd_rg,bitb_rgd_rgd,bitb_rgd_in,bitb_rgd_ind,bitb_rgd_de,bitb_rgd_ded,bitb_rgd_ix,bitb_rgd_ixd,
/*TODO*///	bitb_rgd_rg,bitb_rgd_rgd,bitb_rgd_in,bitb_rgd_ind,bitb_rgd_de,bitb_rgd_ded,bitb_rgd_ix,bitb_rgd_ixd,
/*TODO*///	bitb_rgd_rg,bitb_rgd_rgd,bitb_rgd_in,bitb_rgd_ind,bitb_rgd_de,bitb_rgd_ded,bitb_rgd_ix,bitb_rgd_ixd,
/*TODO*///	/* 0xb400 */
/*TODO*///	bitb_in_rg,	bitb_in_rgd,bitb_in_in,	bitb_in_ind,bitb_in_de,	bitb_in_ded,bitb_in_ix,	bitb_in_ixd,
/*TODO*///	bitb_in_rg,	bitb_in_rgd,bitb_in_in,	bitb_in_ind,bitb_in_de,	bitb_in_ded,bitb_in_ix,	bitb_in_ixd,
/*TODO*///	bitb_in_rg,	bitb_in_rgd,bitb_in_in,	bitb_in_ind,bitb_in_de,	bitb_in_ded,bitb_in_ix,	bitb_in_ixd,
/*TODO*///	bitb_in_rg,	bitb_in_rgd,bitb_in_in,	bitb_in_ind,bitb_in_de,	bitb_in_ded,bitb_in_ix,	bitb_in_ixd,
/*TODO*///	/* 0xb500 */
/*TODO*///	bitb_in_rg,	bitb_in_rgd,bitb_in_in,	bitb_in_ind,bitb_in_de,	bitb_in_ded,bitb_in_ix,	bitb_in_ixd,
/*TODO*///	bitb_in_rg,	bitb_in_rgd,bitb_in_in,	bitb_in_ind,bitb_in_de,	bitb_in_ded,bitb_in_ix,	bitb_in_ixd,
/*TODO*///	bitb_in_rg,	bitb_in_rgd,bitb_in_in,	bitb_in_ind,bitb_in_de,	bitb_in_ded,bitb_in_ix,	bitb_in_ixd,
/*TODO*///	bitb_in_rg,	bitb_in_rgd,bitb_in_in,	bitb_in_ind,bitb_in_de,	bitb_in_ded,bitb_in_ix,	bitb_in_ixd,
/*TODO*///	/* 0xb600 */
/*TODO*///	bitb_ind_rg,bitb_ind_rgd,bitb_ind_in,bitb_ind_ind,bitb_ind_de,bitb_ind_ded,bitb_ind_ix,bitb_ind_ixd,
/*TODO*///	bitb_ind_rg,bitb_ind_rgd,bitb_ind_in,bitb_ind_ind,bitb_ind_de,bitb_ind_ded,bitb_ind_ix,bitb_ind_ixd,
/*TODO*///	bitb_ind_rg,bitb_ind_rgd,bitb_ind_in,bitb_ind_ind,bitb_ind_de,bitb_ind_ded,bitb_ind_ix,bitb_ind_ixd,
/*TODO*///	bitb_ind_rg,bitb_ind_rgd,bitb_ind_in,bitb_ind_ind,bitb_ind_de,bitb_ind_ded,bitb_ind_ix,bitb_ind_ixd,
/*TODO*///	/* 0xb700 */
/*TODO*///	bitb_ind_rg,bitb_ind_rgd,bitb_ind_in,bitb_ind_ind,bitb_ind_de,bitb_ind_ded,bitb_ind_ix,bitb_ind_ixd,
/*TODO*///	bitb_ind_rg,bitb_ind_rgd,bitb_ind_in,bitb_ind_ind,bitb_ind_de,bitb_ind_ded,bitb_ind_ix,bitb_ind_ixd,
/*TODO*///	bitb_ind_rg,bitb_ind_rgd,bitb_ind_in,bitb_ind_ind,bitb_ind_de,bitb_ind_ded,bitb_ind_ix,bitb_ind_ixd,
/*TODO*///	bitb_ind_rg,bitb_ind_rgd,bitb_ind_in,bitb_ind_ind,bitb_ind_de,bitb_ind_ded,bitb_ind_ix,bitb_ind_ixd,
/*TODO*///	/* 0xb800 */
/*TODO*///	bitb_de_rg,	bitb_de_rgd,bitb_de_in,	bitb_de_ind,bitb_de_de,	bitb_de_ded,bitb_de_ix,	bitb_de_ixd,
/*TODO*///	bitb_de_rg,	bitb_de_rgd,bitb_de_in,	bitb_de_ind,bitb_de_de,	bitb_de_ded,bitb_de_ix,	bitb_de_ixd,
/*TODO*///	bitb_de_rg,	bitb_de_rgd,bitb_de_in,	bitb_de_ind,bitb_de_de,	bitb_de_ded,bitb_de_ix,	bitb_de_ixd,
/*TODO*///	bitb_de_rg,	bitb_de_rgd,bitb_de_in,	bitb_de_ind,bitb_de_de,	bitb_de_ded,bitb_de_ix,	bitb_de_ixd,
/*TODO*///	/* 0xb900 */
/*TODO*///	bitb_de_rg,	bitb_de_rgd,bitb_de_in,	bitb_de_ind,bitb_de_de,	bitb_de_ded,bitb_de_ix,	bitb_de_ixd,
/*TODO*///	bitb_de_rg,	bitb_de_rgd,bitb_de_in,	bitb_de_ind,bitb_de_de,	bitb_de_ded,bitb_de_ix,	bitb_de_ixd,
/*TODO*///	bitb_de_rg,	bitb_de_rgd,bitb_de_in,	bitb_de_ind,bitb_de_de,	bitb_de_ded,bitb_de_ix,	bitb_de_ixd,
/*TODO*///	bitb_de_rg,	bitb_de_rgd,bitb_de_in,	bitb_de_ind,bitb_de_de,	bitb_de_ded,bitb_de_ix,	bitb_de_ixd,
/*TODO*///	/* 0xba00 */
/*TODO*///	bitb_ded_rg,bitb_ded_rgd,bitb_ded_in,bitb_ded_ind,bitb_ded_de,bitb_ded_ded,bitb_ded_ix,bitb_ded_ixd,
/*TODO*///	bitb_ded_rg,bitb_ded_rgd,bitb_ded_in,bitb_ded_ind,bitb_ded_de,bitb_ded_ded,bitb_ded_ix,bitb_ded_ixd,
/*TODO*///	bitb_ded_rg,bitb_ded_rgd,bitb_ded_in,bitb_ded_ind,bitb_ded_de,bitb_ded_ded,bitb_ded_ix,bitb_ded_ixd,
/*TODO*///	bitb_ded_rg,bitb_ded_rgd,bitb_ded_in,bitb_ded_ind,bitb_ded_de,bitb_ded_ded,bitb_ded_ix,bitb_ded_ixd,
/*TODO*///	/* 0xbb00 */
/*TODO*///	bitb_ded_rg,bitb_ded_rgd,bitb_ded_in,bitb_ded_ind,bitb_ded_de,bitb_ded_ded,bitb_ded_ix,bitb_ded_ixd,
/*TODO*///	bitb_ded_rg,bitb_ded_rgd,bitb_ded_in,bitb_ded_ind,bitb_ded_de,bitb_ded_ded,bitb_ded_ix,bitb_ded_ixd,
/*TODO*///	bitb_ded_rg,bitb_ded_rgd,bitb_ded_in,bitb_ded_ind,bitb_ded_de,bitb_ded_ded,bitb_ded_ix,bitb_ded_ixd,
/*TODO*///	bitb_ded_rg,bitb_ded_rgd,bitb_ded_in,bitb_ded_ind,bitb_ded_de,bitb_ded_ded,bitb_ded_ix,bitb_ded_ixd,
/*TODO*///	/* 0xbc00 */
/*TODO*///	bitb_ix_rg,	bitb_ix_rgd,bitb_ix_in,	bitb_ix_ind,bitb_ix_de,	bitb_ix_ded,bitb_ix_ix,	bitb_ix_ixd,
/*TODO*///	bitb_ix_rg,	bitb_ix_rgd,bitb_ix_in,	bitb_ix_ind,bitb_ix_de,	bitb_ix_ded,bitb_ix_ix,	bitb_ix_ixd,
/*TODO*///	bitb_ix_rg,	bitb_ix_rgd,bitb_ix_in,	bitb_ix_ind,bitb_ix_de,	bitb_ix_ded,bitb_ix_ix,	bitb_ix_ixd,
/*TODO*///	bitb_ix_rg,	bitb_ix_rgd,bitb_ix_in,	bitb_ix_ind,bitb_ix_de,	bitb_ix_ded,bitb_ix_ix,	bitb_ix_ixd,
/*TODO*///	/* 0xbd00 */
/*TODO*///	bitb_ix_rg,	bitb_ix_rgd,bitb_ix_in,	bitb_ix_ind,bitb_ix_de,	bitb_ix_ded,bitb_ix_ix,	bitb_ix_ixd,
/*TODO*///	bitb_ix_rg,	bitb_ix_rgd,bitb_ix_in,	bitb_ix_ind,bitb_ix_de,	bitb_ix_ded,bitb_ix_ix,	bitb_ix_ixd,
/*TODO*///	bitb_ix_rg,	bitb_ix_rgd,bitb_ix_in,	bitb_ix_ind,bitb_ix_de,	bitb_ix_ded,bitb_ix_ix,	bitb_ix_ixd,
/*TODO*///	bitb_ix_rg,	bitb_ix_rgd,bitb_ix_in,	bitb_ix_ind,bitb_ix_de,	bitb_ix_ded,bitb_ix_ix,	bitb_ix_ixd,
/*TODO*///	/* 0xbe00 */
/*TODO*///	bitb_ixd_rg,bitb_ixd_rgd,bitb_ixd_in,bitb_ixd_ind,bitb_ixd_de,bitb_ixd_ded,bitb_ixd_ix,bitb_ixd_ixd,
/*TODO*///	bitb_ixd_rg,bitb_ixd_rgd,bitb_ixd_in,bitb_ixd_ind,bitb_ixd_de,bitb_ixd_ded,bitb_ixd_ix,bitb_ixd_ixd,
/*TODO*///	bitb_ixd_rg,bitb_ixd_rgd,bitb_ixd_in,bitb_ixd_ind,bitb_ixd_de,bitb_ixd_ded,bitb_ixd_ix,bitb_ixd_ixd,
/*TODO*///	bitb_ixd_rg,bitb_ixd_rgd,bitb_ixd_in,bitb_ixd_ind,bitb_ixd_de,bitb_ixd_ded,bitb_ixd_ix,bitb_ixd_ixd,
/*TODO*///	/* 0xbf00 */
/*TODO*///	bitb_ixd_rg,bitb_ixd_rgd,bitb_ixd_in,bitb_ixd_ind,bitb_ixd_de,bitb_ixd_ded,bitb_ixd_ix,bitb_ixd_ixd,
/*TODO*///	bitb_ixd_rg,bitb_ixd_rgd,bitb_ixd_in,bitb_ixd_ind,bitb_ixd_de,bitb_ixd_ded,bitb_ixd_ix,bitb_ixd_ixd,
/*TODO*///	bitb_ixd_rg,bitb_ixd_rgd,bitb_ixd_in,bitb_ixd_ind,bitb_ixd_de,bitb_ixd_ded,bitb_ixd_ix,bitb_ixd_ixd,
/*TODO*///	bitb_ixd_rg,bitb_ixd_rgd,bitb_ixd_in,bitb_ixd_ind,bitb_ixd_de,bitb_ixd_ded,bitb_ixd_ix,bitb_ixd_ixd,
/*TODO*///
/*TODO*///	/* 0xc000 */
/*TODO*///	bicb_rg_rg,	bicb_rg_rgd,bicb_rg_in,	bicb_rg_ind,bicb_rg_de,	bicb_rg_ded,bicb_rg_ix,	bicb_rg_ixd,
/*TODO*///	bicb_rg_rg,	bicb_rg_rgd,bicb_rg_in,	bicb_rg_ind,bicb_rg_de,	bicb_rg_ded,bicb_rg_ix,	bicb_rg_ixd,
/*TODO*///	bicb_rg_rg,	bicb_rg_rgd,bicb_rg_in,	bicb_rg_ind,bicb_rg_de,	bicb_rg_ded,bicb_rg_ix,	bicb_rg_ixd,
/*TODO*///	bicb_rg_rg,	bicb_rg_rgd,bicb_rg_in,	bicb_rg_ind,bicb_rg_de,	bicb_rg_ded,bicb_rg_ix,	bicb_rg_ixd,
/*TODO*///	/* 0xc100 */
/*TODO*///	bicb_rg_rg,	bicb_rg_rgd,bicb_rg_in,	bicb_rg_ind,bicb_rg_de,	bicb_rg_ded,bicb_rg_ix,	bicb_rg_ixd,
/*TODO*///	bicb_rg_rg,	bicb_rg_rgd,bicb_rg_in,	bicb_rg_ind,bicb_rg_de,	bicb_rg_ded,bicb_rg_ix,	bicb_rg_ixd,
/*TODO*///	bicb_rg_rg,	bicb_rg_rgd,bicb_rg_in,	bicb_rg_ind,bicb_rg_de,	bicb_rg_ded,bicb_rg_ix,	bicb_rg_ixd,
/*TODO*///	bicb_rg_rg,	bicb_rg_rgd,bicb_rg_in,	bicb_rg_ind,bicb_rg_de,	bicb_rg_ded,bicb_rg_ix,	bicb_rg_ixd,
/*TODO*///	/* 0xc200 */
/*TODO*///	bicb_rgd_rg,bicb_rgd_rgd,bicb_rgd_in,bicb_rgd_ind,bicb_rgd_de,bicb_rgd_ded,bicb_rgd_ix,bicb_rgd_ixd,
/*TODO*///	bicb_rgd_rg,bicb_rgd_rgd,bicb_rgd_in,bicb_rgd_ind,bicb_rgd_de,bicb_rgd_ded,bicb_rgd_ix,bicb_rgd_ixd,
/*TODO*///	bicb_rgd_rg,bicb_rgd_rgd,bicb_rgd_in,bicb_rgd_ind,bicb_rgd_de,bicb_rgd_ded,bicb_rgd_ix,bicb_rgd_ixd,
/*TODO*///	bicb_rgd_rg,bicb_rgd_rgd,bicb_rgd_in,bicb_rgd_ind,bicb_rgd_de,bicb_rgd_ded,bicb_rgd_ix,bicb_rgd_ixd,
/*TODO*///	/* 0xc300 */
/*TODO*///	bicb_rgd_rg,bicb_rgd_rgd,bicb_rgd_in,bicb_rgd_ind,bicb_rgd_de,bicb_rgd_ded,bicb_rgd_ix,bicb_rgd_ixd,
/*TODO*///	bicb_rgd_rg,bicb_rgd_rgd,bicb_rgd_in,bicb_rgd_ind,bicb_rgd_de,bicb_rgd_ded,bicb_rgd_ix,bicb_rgd_ixd,
/*TODO*///	bicb_rgd_rg,bicb_rgd_rgd,bicb_rgd_in,bicb_rgd_ind,bicb_rgd_de,bicb_rgd_ded,bicb_rgd_ix,bicb_rgd_ixd,
/*TODO*///	bicb_rgd_rg,bicb_rgd_rgd,bicb_rgd_in,bicb_rgd_ind,bicb_rgd_de,bicb_rgd_ded,bicb_rgd_ix,bicb_rgd_ixd,
/*TODO*///	/* 0xc400 */
/*TODO*///	bicb_in_rg,	bicb_in_rgd,bicb_in_in,	bicb_in_ind,bicb_in_de,	bicb_in_ded,bicb_in_ix,	bicb_in_ixd,
/*TODO*///	bicb_in_rg,	bicb_in_rgd,bicb_in_in,	bicb_in_ind,bicb_in_de,	bicb_in_ded,bicb_in_ix,	bicb_in_ixd,
/*TODO*///	bicb_in_rg,	bicb_in_rgd,bicb_in_in,	bicb_in_ind,bicb_in_de,	bicb_in_ded,bicb_in_ix,	bicb_in_ixd,
/*TODO*///	bicb_in_rg,	bicb_in_rgd,bicb_in_in,	bicb_in_ind,bicb_in_de,	bicb_in_ded,bicb_in_ix,	bicb_in_ixd,
/*TODO*///	/* 0xc500 */
/*TODO*///	bicb_in_rg,	bicb_in_rgd,bicb_in_in,	bicb_in_ind,bicb_in_de,	bicb_in_ded,bicb_in_ix,	bicb_in_ixd,
/*TODO*///	bicb_in_rg,	bicb_in_rgd,bicb_in_in,	bicb_in_ind,bicb_in_de,	bicb_in_ded,bicb_in_ix,	bicb_in_ixd,
/*TODO*///	bicb_in_rg,	bicb_in_rgd,bicb_in_in,	bicb_in_ind,bicb_in_de,	bicb_in_ded,bicb_in_ix,	bicb_in_ixd,
/*TODO*///	bicb_in_rg,	bicb_in_rgd,bicb_in_in,	bicb_in_ind,bicb_in_de,	bicb_in_ded,bicb_in_ix,	bicb_in_ixd,
/*TODO*///	/* 0xc600 */
/*TODO*///	bicb_ind_rg,bicb_ind_rgd,bicb_ind_in,bicb_ind_ind,bicb_ind_de,bicb_ind_ded,bicb_ind_ix,bicb_ind_ixd,
/*TODO*///	bicb_ind_rg,bicb_ind_rgd,bicb_ind_in,bicb_ind_ind,bicb_ind_de,bicb_ind_ded,bicb_ind_ix,bicb_ind_ixd,
/*TODO*///	bicb_ind_rg,bicb_ind_rgd,bicb_ind_in,bicb_ind_ind,bicb_ind_de,bicb_ind_ded,bicb_ind_ix,bicb_ind_ixd,
/*TODO*///	bicb_ind_rg,bicb_ind_rgd,bicb_ind_in,bicb_ind_ind,bicb_ind_de,bicb_ind_ded,bicb_ind_ix,bicb_ind_ixd,
/*TODO*///	/* 0xc700 */
/*TODO*///	bicb_ind_rg,bicb_ind_rgd,bicb_ind_in,bicb_ind_ind,bicb_ind_de,bicb_ind_ded,bicb_ind_ix,bicb_ind_ixd,
/*TODO*///	bicb_ind_rg,bicb_ind_rgd,bicb_ind_in,bicb_ind_ind,bicb_ind_de,bicb_ind_ded,bicb_ind_ix,bicb_ind_ixd,
/*TODO*///	bicb_ind_rg,bicb_ind_rgd,bicb_ind_in,bicb_ind_ind,bicb_ind_de,bicb_ind_ded,bicb_ind_ix,bicb_ind_ixd,
/*TODO*///	bicb_ind_rg,bicb_ind_rgd,bicb_ind_in,bicb_ind_ind,bicb_ind_de,bicb_ind_ded,bicb_ind_ix,bicb_ind_ixd,
/*TODO*///	/* 0xc800 */
/*TODO*///	bicb_de_rg,	bicb_de_rgd,bicb_de_in,	bicb_de_ind,bicb_de_de,	bicb_de_ded,bicb_de_ix,	bicb_de_ixd,
/*TODO*///	bicb_de_rg,	bicb_de_rgd,bicb_de_in,	bicb_de_ind,bicb_de_de,	bicb_de_ded,bicb_de_ix,	bicb_de_ixd,
/*TODO*///	bicb_de_rg,	bicb_de_rgd,bicb_de_in,	bicb_de_ind,bicb_de_de,	bicb_de_ded,bicb_de_ix,	bicb_de_ixd,
/*TODO*///	bicb_de_rg,	bicb_de_rgd,bicb_de_in,	bicb_de_ind,bicb_de_de,	bicb_de_ded,bicb_de_ix,	bicb_de_ixd,
/*TODO*///	/* 0xc900 */
/*TODO*///	bicb_de_rg,	bicb_de_rgd,bicb_de_in,	bicb_de_ind,bicb_de_de,	bicb_de_ded,bicb_de_ix,	bicb_de_ixd,
/*TODO*///	bicb_de_rg,	bicb_de_rgd,bicb_de_in,	bicb_de_ind,bicb_de_de,	bicb_de_ded,bicb_de_ix,	bicb_de_ixd,
/*TODO*///	bicb_de_rg,	bicb_de_rgd,bicb_de_in,	bicb_de_ind,bicb_de_de,	bicb_de_ded,bicb_de_ix,	bicb_de_ixd,
/*TODO*///	bicb_de_rg,	bicb_de_rgd,bicb_de_in,	bicb_de_ind,bicb_de_de,	bicb_de_ded,bicb_de_ix,	bicb_de_ixd,
/*TODO*///	/* 0xca00 */
/*TODO*///	bicb_ded_rg,bicb_ded_rgd,bicb_ded_in,bicb_ded_ind,bicb_ded_de,bicb_ded_ded,bicb_ded_ix,bicb_ded_ixd,
/*TODO*///	bicb_ded_rg,bicb_ded_rgd,bicb_ded_in,bicb_ded_ind,bicb_ded_de,bicb_ded_ded,bicb_ded_ix,bicb_ded_ixd,
/*TODO*///	bicb_ded_rg,bicb_ded_rgd,bicb_ded_in,bicb_ded_ind,bicb_ded_de,bicb_ded_ded,bicb_ded_ix,bicb_ded_ixd,
/*TODO*///	bicb_ded_rg,bicb_ded_rgd,bicb_ded_in,bicb_ded_ind,bicb_ded_de,bicb_ded_ded,bicb_ded_ix,bicb_ded_ixd,
/*TODO*///	/* 0xcb00 */
/*TODO*///	bicb_ded_rg,bicb_ded_rgd,bicb_ded_in,bicb_ded_ind,bicb_ded_de,bicb_ded_ded,bicb_ded_ix,bicb_ded_ixd,
/*TODO*///	bicb_ded_rg,bicb_ded_rgd,bicb_ded_in,bicb_ded_ind,bicb_ded_de,bicb_ded_ded,bicb_ded_ix,bicb_ded_ixd,
/*TODO*///	bicb_ded_rg,bicb_ded_rgd,bicb_ded_in,bicb_ded_ind,bicb_ded_de,bicb_ded_ded,bicb_ded_ix,bicb_ded_ixd,
/*TODO*///	bicb_ded_rg,bicb_ded_rgd,bicb_ded_in,bicb_ded_ind,bicb_ded_de,bicb_ded_ded,bicb_ded_ix,bicb_ded_ixd,
/*TODO*///	/* 0xcc00 */
/*TODO*///	bicb_ix_rg,	bicb_ix_rgd,bicb_ix_in,	bicb_ix_ind,bicb_ix_de,	bicb_ix_ded,bicb_ix_ix,	bicb_ix_ixd,
/*TODO*///	bicb_ix_rg,	bicb_ix_rgd,bicb_ix_in,	bicb_ix_ind,bicb_ix_de,	bicb_ix_ded,bicb_ix_ix,	bicb_ix_ixd,
/*TODO*///	bicb_ix_rg,	bicb_ix_rgd,bicb_ix_in,	bicb_ix_ind,bicb_ix_de,	bicb_ix_ded,bicb_ix_ix,	bicb_ix_ixd,
/*TODO*///	bicb_ix_rg,	bicb_ix_rgd,bicb_ix_in,	bicb_ix_ind,bicb_ix_de,	bicb_ix_ded,bicb_ix_ix,	bicb_ix_ixd,
/*TODO*///	/* 0xcd00 */
/*TODO*///	bicb_ix_rg,	bicb_ix_rgd,bicb_ix_in,	bicb_ix_ind,bicb_ix_de,	bicb_ix_ded,bicb_ix_ix,	bicb_ix_ixd,
/*TODO*///	bicb_ix_rg,	bicb_ix_rgd,bicb_ix_in,	bicb_ix_ind,bicb_ix_de,	bicb_ix_ded,bicb_ix_ix,	bicb_ix_ixd,
/*TODO*///	bicb_ix_rg,	bicb_ix_rgd,bicb_ix_in,	bicb_ix_ind,bicb_ix_de,	bicb_ix_ded,bicb_ix_ix,	bicb_ix_ixd,
/*TODO*///	bicb_ix_rg,	bicb_ix_rgd,bicb_ix_in,	bicb_ix_ind,bicb_ix_de,	bicb_ix_ded,bicb_ix_ix,	bicb_ix_ixd,
/*TODO*///	/* 0xce00 */
/*TODO*///	bicb_ixd_rg,bicb_ixd_rgd,bicb_ixd_in,bicb_ixd_ind,bicb_ixd_de,bicb_ixd_ded,bicb_ixd_ix,bicb_ixd_ixd,
/*TODO*///	bicb_ixd_rg,bicb_ixd_rgd,bicb_ixd_in,bicb_ixd_ind,bicb_ixd_de,bicb_ixd_ded,bicb_ixd_ix,bicb_ixd_ixd,
/*TODO*///	bicb_ixd_rg,bicb_ixd_rgd,bicb_ixd_in,bicb_ixd_ind,bicb_ixd_de,bicb_ixd_ded,bicb_ixd_ix,bicb_ixd_ixd,
/*TODO*///	bicb_ixd_rg,bicb_ixd_rgd,bicb_ixd_in,bicb_ixd_ind,bicb_ixd_de,bicb_ixd_ded,bicb_ixd_ix,bicb_ixd_ixd,
/*TODO*///	/* 0xcf00 */
/*TODO*///	bicb_ixd_rg,bicb_ixd_rgd,bicb_ixd_in,bicb_ixd_ind,bicb_ixd_de,bicb_ixd_ded,bicb_ixd_ix,bicb_ixd_ixd,
/*TODO*///	bicb_ixd_rg,bicb_ixd_rgd,bicb_ixd_in,bicb_ixd_ind,bicb_ixd_de,bicb_ixd_ded,bicb_ixd_ix,bicb_ixd_ixd,
/*TODO*///	bicb_ixd_rg,bicb_ixd_rgd,bicb_ixd_in,bicb_ixd_ind,bicb_ixd_de,bicb_ixd_ded,bicb_ixd_ix,bicb_ixd_ixd,
/*TODO*///	bicb_ixd_rg,bicb_ixd_rgd,bicb_ixd_in,bicb_ixd_ind,bicb_ixd_de,bicb_ixd_ded,bicb_ixd_ix,bicb_ixd_ixd,
/*TODO*///
/*TODO*///	/* 0xd000 */
/*TODO*///	bisb_rg_rg,	bisb_rg_rgd,bisb_rg_in,	bisb_rg_ind,bisb_rg_de,	bisb_rg_ded,bisb_rg_ix,	bisb_rg_ixd,
/*TODO*///	bisb_rg_rg,	bisb_rg_rgd,bisb_rg_in,	bisb_rg_ind,bisb_rg_de,	bisb_rg_ded,bisb_rg_ix,	bisb_rg_ixd,
/*TODO*///	bisb_rg_rg,	bisb_rg_rgd,bisb_rg_in,	bisb_rg_ind,bisb_rg_de,	bisb_rg_ded,bisb_rg_ix,	bisb_rg_ixd,
/*TODO*///	bisb_rg_rg,	bisb_rg_rgd,bisb_rg_in,	bisb_rg_ind,bisb_rg_de,	bisb_rg_ded,bisb_rg_ix,	bisb_rg_ixd,
/*TODO*///	/* 0xd100 */
/*TODO*///	bisb_rg_rg,	bisb_rg_rgd,bisb_rg_in,	bisb_rg_ind,bisb_rg_de,	bisb_rg_ded,bisb_rg_ix,	bisb_rg_ixd,
/*TODO*///	bisb_rg_rg,	bisb_rg_rgd,bisb_rg_in,	bisb_rg_ind,bisb_rg_de,	bisb_rg_ded,bisb_rg_ix,	bisb_rg_ixd,
/*TODO*///	bisb_rg_rg,	bisb_rg_rgd,bisb_rg_in,	bisb_rg_ind,bisb_rg_de,	bisb_rg_ded,bisb_rg_ix,	bisb_rg_ixd,
/*TODO*///	bisb_rg_rg,	bisb_rg_rgd,bisb_rg_in,	bisb_rg_ind,bisb_rg_de,	bisb_rg_ded,bisb_rg_ix,	bisb_rg_ixd,
/*TODO*///	/* 0xd200 */
/*TODO*///	bisb_rgd_rg,bisb_rgd_rgd,bisb_rgd_in,bisb_rgd_ind,bisb_rgd_de,bisb_rgd_ded,bisb_rgd_ix,bisb_rgd_ixd,
/*TODO*///	bisb_rgd_rg,bisb_rgd_rgd,bisb_rgd_in,bisb_rgd_ind,bisb_rgd_de,bisb_rgd_ded,bisb_rgd_ix,bisb_rgd_ixd,
/*TODO*///	bisb_rgd_rg,bisb_rgd_rgd,bisb_rgd_in,bisb_rgd_ind,bisb_rgd_de,bisb_rgd_ded,bisb_rgd_ix,bisb_rgd_ixd,
/*TODO*///	bisb_rgd_rg,bisb_rgd_rgd,bisb_rgd_in,bisb_rgd_ind,bisb_rgd_de,bisb_rgd_ded,bisb_rgd_ix,bisb_rgd_ixd,
/*TODO*///	/* 0xd300 */
/*TODO*///	bisb_rgd_rg,bisb_rgd_rgd,bisb_rgd_in,bisb_rgd_ind,bisb_rgd_de,bisb_rgd_ded,bisb_rgd_ix,bisb_rgd_ixd,
/*TODO*///	bisb_rgd_rg,bisb_rgd_rgd,bisb_rgd_in,bisb_rgd_ind,bisb_rgd_de,bisb_rgd_ded,bisb_rgd_ix,bisb_rgd_ixd,
/*TODO*///	bisb_rgd_rg,bisb_rgd_rgd,bisb_rgd_in,bisb_rgd_ind,bisb_rgd_de,bisb_rgd_ded,bisb_rgd_ix,bisb_rgd_ixd,
/*TODO*///	bisb_rgd_rg,bisb_rgd_rgd,bisb_rgd_in,bisb_rgd_ind,bisb_rgd_de,bisb_rgd_ded,bisb_rgd_ix,bisb_rgd_ixd,
/*TODO*///	/* 0xd400 */
/*TODO*///	bisb_in_rg,	bisb_in_rgd,bisb_in_in,	bisb_in_ind,bisb_in_de,	bisb_in_ded,bisb_in_ix,	bisb_in_ixd,
/*TODO*///	bisb_in_rg,	bisb_in_rgd,bisb_in_in,	bisb_in_ind,bisb_in_de,	bisb_in_ded,bisb_in_ix,	bisb_in_ixd,
/*TODO*///	bisb_in_rg,	bisb_in_rgd,bisb_in_in,	bisb_in_ind,bisb_in_de,	bisb_in_ded,bisb_in_ix,	bisb_in_ixd,
/*TODO*///	bisb_in_rg,	bisb_in_rgd,bisb_in_in,	bisb_in_ind,bisb_in_de,	bisb_in_ded,bisb_in_ix,	bisb_in_ixd,
/*TODO*///	/* 0xd500 */
/*TODO*///	bisb_in_rg,	bisb_in_rgd,bisb_in_in,	bisb_in_ind,bisb_in_de,	bisb_in_ded,bisb_in_ix,	bisb_in_ixd,
/*TODO*///	bisb_in_rg,	bisb_in_rgd,bisb_in_in,	bisb_in_ind,bisb_in_de,	bisb_in_ded,bisb_in_ix,	bisb_in_ixd,
/*TODO*///	bisb_in_rg,	bisb_in_rgd,bisb_in_in,	bisb_in_ind,bisb_in_de,	bisb_in_ded,bisb_in_ix,	bisb_in_ixd,
/*TODO*///	bisb_in_rg,	bisb_in_rgd,bisb_in_in,	bisb_in_ind,bisb_in_de,	bisb_in_ded,bisb_in_ix,	bisb_in_ixd,
/*TODO*///	/* 0xd600 */
/*TODO*///	bisb_ind_rg,bisb_ind_rgd,bisb_ind_in,bisb_ind_ind,bisb_ind_de,bisb_ind_ded,bisb_ind_ix,bisb_ind_ixd,
/*TODO*///	bisb_ind_rg,bisb_ind_rgd,bisb_ind_in,bisb_ind_ind,bisb_ind_de,bisb_ind_ded,bisb_ind_ix,bisb_ind_ixd,
/*TODO*///	bisb_ind_rg,bisb_ind_rgd,bisb_ind_in,bisb_ind_ind,bisb_ind_de,bisb_ind_ded,bisb_ind_ix,bisb_ind_ixd,
/*TODO*///	bisb_ind_rg,bisb_ind_rgd,bisb_ind_in,bisb_ind_ind,bisb_ind_de,bisb_ind_ded,bisb_ind_ix,bisb_ind_ixd,
/*TODO*///	/* 0xd700 */
/*TODO*///	bisb_ind_rg,bisb_ind_rgd,bisb_ind_in,bisb_ind_ind,bisb_ind_de,bisb_ind_ded,bisb_ind_ix,bisb_ind_ixd,
/*TODO*///	bisb_ind_rg,bisb_ind_rgd,bisb_ind_in,bisb_ind_ind,bisb_ind_de,bisb_ind_ded,bisb_ind_ix,bisb_ind_ixd,
/*TODO*///	bisb_ind_rg,bisb_ind_rgd,bisb_ind_in,bisb_ind_ind,bisb_ind_de,bisb_ind_ded,bisb_ind_ix,bisb_ind_ixd,
/*TODO*///	bisb_ind_rg,bisb_ind_rgd,bisb_ind_in,bisb_ind_ind,bisb_ind_de,bisb_ind_ded,bisb_ind_ix,bisb_ind_ixd,
/*TODO*///	/* 0xd800 */
/*TODO*///	bisb_de_rg,	bisb_de_rgd,bisb_de_in,	bisb_de_ind,bisb_de_de,	bisb_de_ded,bisb_de_ix,	bisb_de_ixd,
/*TODO*///	bisb_de_rg,	bisb_de_rgd,bisb_de_in,	bisb_de_ind,bisb_de_de,	bisb_de_ded,bisb_de_ix,	bisb_de_ixd,
/*TODO*///	bisb_de_rg,	bisb_de_rgd,bisb_de_in,	bisb_de_ind,bisb_de_de,	bisb_de_ded,bisb_de_ix,	bisb_de_ixd,
/*TODO*///	bisb_de_rg,	bisb_de_rgd,bisb_de_in,	bisb_de_ind,bisb_de_de,	bisb_de_ded,bisb_de_ix,	bisb_de_ixd,
/*TODO*///	/* 0xd900 */
/*TODO*///	bisb_de_rg,	bisb_de_rgd,bisb_de_in,	bisb_de_ind,bisb_de_de,	bisb_de_ded,bisb_de_ix,	bisb_de_ixd,
/*TODO*///	bisb_de_rg,	bisb_de_rgd,bisb_de_in,	bisb_de_ind,bisb_de_de,	bisb_de_ded,bisb_de_ix,	bisb_de_ixd,
/*TODO*///	bisb_de_rg,	bisb_de_rgd,bisb_de_in,	bisb_de_ind,bisb_de_de,	bisb_de_ded,bisb_de_ix,	bisb_de_ixd,
/*TODO*///	bisb_de_rg,	bisb_de_rgd,bisb_de_in,	bisb_de_ind,bisb_de_de,	bisb_de_ded,bisb_de_ix,	bisb_de_ixd,
/*TODO*///	/* 0xda00 */
/*TODO*///	bisb_ded_rg,bisb_ded_rgd,bisb_ded_in,bisb_ded_ind,bisb_ded_de,bisb_ded_ded,bisb_ded_ix,bisb_ded_ixd,
/*TODO*///	bisb_ded_rg,bisb_ded_rgd,bisb_ded_in,bisb_ded_ind,bisb_ded_de,bisb_ded_ded,bisb_ded_ix,bisb_ded_ixd,
/*TODO*///	bisb_ded_rg,bisb_ded_rgd,bisb_ded_in,bisb_ded_ind,bisb_ded_de,bisb_ded_ded,bisb_ded_ix,bisb_ded_ixd,
/*TODO*///	bisb_ded_rg,bisb_ded_rgd,bisb_ded_in,bisb_ded_ind,bisb_ded_de,bisb_ded_ded,bisb_ded_ix,bisb_ded_ixd,
/*TODO*///	/* 0xdb00 */
/*TODO*///	bisb_ded_rg,bisb_ded_rgd,bisb_ded_in,bisb_ded_ind,bisb_ded_de,bisb_ded_ded,bisb_ded_ix,bisb_ded_ixd,
/*TODO*///	bisb_ded_rg,bisb_ded_rgd,bisb_ded_in,bisb_ded_ind,bisb_ded_de,bisb_ded_ded,bisb_ded_ix,bisb_ded_ixd,
/*TODO*///	bisb_ded_rg,bisb_ded_rgd,bisb_ded_in,bisb_ded_ind,bisb_ded_de,bisb_ded_ded,bisb_ded_ix,bisb_ded_ixd,
/*TODO*///	bisb_ded_rg,bisb_ded_rgd,bisb_ded_in,bisb_ded_ind,bisb_ded_de,bisb_ded_ded,bisb_ded_ix,bisb_ded_ixd,
/*TODO*///	/* 0xdc00 */
/*TODO*///	bisb_ix_rg,	bisb_ix_rgd,bisb_ix_in,	bisb_ix_ind,bisb_ix_de,	bisb_ix_ded,bisb_ix_ix,	bisb_ix_ixd,
/*TODO*///	bisb_ix_rg,	bisb_ix_rgd,bisb_ix_in,	bisb_ix_ind,bisb_ix_de,	bisb_ix_ded,bisb_ix_ix,	bisb_ix_ixd,
/*TODO*///	bisb_ix_rg,	bisb_ix_rgd,bisb_ix_in,	bisb_ix_ind,bisb_ix_de,	bisb_ix_ded,bisb_ix_ix,	bisb_ix_ixd,
/*TODO*///	bisb_ix_rg,	bisb_ix_rgd,bisb_ix_in,	bisb_ix_ind,bisb_ix_de,	bisb_ix_ded,bisb_ix_ix,	bisb_ix_ixd,
/*TODO*///	/* 0xdd00 */
/*TODO*///	bisb_ix_rg,	bisb_ix_rgd,bisb_ix_in,	bisb_ix_ind,bisb_ix_de,	bisb_ix_ded,bisb_ix_ix,	bisb_ix_ixd,
/*TODO*///	bisb_ix_rg,	bisb_ix_rgd,bisb_ix_in,	bisb_ix_ind,bisb_ix_de,	bisb_ix_ded,bisb_ix_ix,	bisb_ix_ixd,
/*TODO*///	bisb_ix_rg,	bisb_ix_rgd,bisb_ix_in,	bisb_ix_ind,bisb_ix_de,	bisb_ix_ded,bisb_ix_ix,	bisb_ix_ixd,
/*TODO*///	bisb_ix_rg,	bisb_ix_rgd,bisb_ix_in,	bisb_ix_ind,bisb_ix_de,	bisb_ix_ded,bisb_ix_ix,	bisb_ix_ixd,
/*TODO*///	/* 0xde00 */
/*TODO*///	bisb_ixd_rg,bisb_ixd_rgd,bisb_ixd_in,bisb_ixd_ind,bisb_ixd_de,bisb_ixd_ded,bisb_ixd_ix,bisb_ixd_ixd,
/*TODO*///	bisb_ixd_rg,bisb_ixd_rgd,bisb_ixd_in,bisb_ixd_ind,bisb_ixd_de,bisb_ixd_ded,bisb_ixd_ix,bisb_ixd_ixd,
/*TODO*///	bisb_ixd_rg,bisb_ixd_rgd,bisb_ixd_in,bisb_ixd_ind,bisb_ixd_de,bisb_ixd_ded,bisb_ixd_ix,bisb_ixd_ixd,
/*TODO*///	bisb_ixd_rg,bisb_ixd_rgd,bisb_ixd_in,bisb_ixd_ind,bisb_ixd_de,bisb_ixd_ded,bisb_ixd_ix,bisb_ixd_ixd,
/*TODO*///	/* 0xdf00 */
/*TODO*///	bisb_ixd_rg,bisb_ixd_rgd,bisb_ixd_in,bisb_ixd_ind,bisb_ixd_de,bisb_ixd_ded,bisb_ixd_ix,bisb_ixd_ixd,
/*TODO*///	bisb_ixd_rg,bisb_ixd_rgd,bisb_ixd_in,bisb_ixd_ind,bisb_ixd_de,bisb_ixd_ded,bisb_ixd_ix,bisb_ixd_ixd,
/*TODO*///	bisb_ixd_rg,bisb_ixd_rgd,bisb_ixd_in,bisb_ixd_ind,bisb_ixd_de,bisb_ixd_ded,bisb_ixd_ix,bisb_ixd_ixd,
/*TODO*///	bisb_ixd_rg,bisb_ixd_rgd,bisb_ixd_in,bisb_ixd_ind,bisb_ixd_de,bisb_ixd_ded,bisb_ixd_ix,bisb_ixd_ixd,
/*TODO*///
/*TODO*///	/* 0xe000 */
/*TODO*///	sub_rg_rg,	sub_rg_rgd,	sub_rg_in,	sub_rg_ind,	sub_rg_de,	sub_rg_ded,	sub_rg_ix,	sub_rg_ixd,
/*TODO*///	sub_rg_rg,	sub_rg_rgd,	sub_rg_in,	sub_rg_ind,	sub_rg_de,	sub_rg_ded,	sub_rg_ix,	sub_rg_ixd,
/*TODO*///	sub_rg_rg,	sub_rg_rgd,	sub_rg_in,	sub_rg_ind,	sub_rg_de,	sub_rg_ded,	sub_rg_ix,	sub_rg_ixd,
/*TODO*///	sub_rg_rg,	sub_rg_rgd,	sub_rg_in,	sub_rg_ind,	sub_rg_de,	sub_rg_ded,	sub_rg_ix,	sub_rg_ixd,
/*TODO*///	/* 0xe100 */
/*TODO*///	sub_rg_rg,	sub_rg_rgd,	sub_rg_in,	sub_rg_ind,	sub_rg_de,	sub_rg_ded,	sub_rg_ix,	sub_rg_ixd,
/*TODO*///	sub_rg_rg,	sub_rg_rgd,	sub_rg_in,	sub_rg_ind,	sub_rg_de,	sub_rg_ded,	sub_rg_ix,	sub_rg_ixd,
/*TODO*///	sub_rg_rg,	sub_rg_rgd,	sub_rg_in,	sub_rg_ind,	sub_rg_de,	sub_rg_ded,	sub_rg_ix,	sub_rg_ixd,
/*TODO*///	sub_rg_rg,	sub_rg_rgd,	sub_rg_in,	sub_rg_ind,	sub_rg_de,	sub_rg_ded,	sub_rg_ix,	sub_rg_ixd,
/*TODO*///	/* 0xe200 */
/*TODO*///	sub_rgd_rg,	sub_rgd_rgd,sub_rgd_in,	sub_rgd_ind,sub_rgd_de,	sub_rgd_ded,sub_rgd_ix,	sub_rgd_ixd,
/*TODO*///	sub_rgd_rg,	sub_rgd_rgd,sub_rgd_in,	sub_rgd_ind,sub_rgd_de,	sub_rgd_ded,sub_rgd_ix,	sub_rgd_ixd,
/*TODO*///	sub_rgd_rg,	sub_rgd_rgd,sub_rgd_in,	sub_rgd_ind,sub_rgd_de,	sub_rgd_ded,sub_rgd_ix,	sub_rgd_ixd,
/*TODO*///	sub_rgd_rg,	sub_rgd_rgd,sub_rgd_in,	sub_rgd_ind,sub_rgd_de,	sub_rgd_ded,sub_rgd_ix,	sub_rgd_ixd,
/*TODO*///	/* 0xe300 */
/*TODO*///	sub_rgd_rg,	sub_rgd_rgd,sub_rgd_in,	sub_rgd_ind,sub_rgd_de,	sub_rgd_ded,sub_rgd_ix,	sub_rgd_ixd,
/*TODO*///	sub_rgd_rg,	sub_rgd_rgd,sub_rgd_in,	sub_rgd_ind,sub_rgd_de,	sub_rgd_ded,sub_rgd_ix,	sub_rgd_ixd,
/*TODO*///	sub_rgd_rg,	sub_rgd_rgd,sub_rgd_in,	sub_rgd_ind,sub_rgd_de,	sub_rgd_ded,sub_rgd_ix,	sub_rgd_ixd,
/*TODO*///	sub_rgd_rg,	sub_rgd_rgd,sub_rgd_in,	sub_rgd_ind,sub_rgd_de,	sub_rgd_ded,sub_rgd_ix,	sub_rgd_ixd,
/*TODO*///	/* 0xe400 */
/*TODO*///	sub_in_rg,	sub_in_rgd,	sub_in_in,	sub_in_ind,	sub_in_de,	sub_in_ded,	sub_in_ix,	sub_in_ixd,
/*TODO*///	sub_in_rg,	sub_in_rgd,	sub_in_in,	sub_in_ind,	sub_in_de,	sub_in_ded,	sub_in_ix,	sub_in_ixd,
/*TODO*///	sub_in_rg,	sub_in_rgd,	sub_in_in,	sub_in_ind,	sub_in_de,	sub_in_ded,	sub_in_ix,	sub_in_ixd,
/*TODO*///	sub_in_rg,	sub_in_rgd,	sub_in_in,	sub_in_ind,	sub_in_de,	sub_in_ded,	sub_in_ix,	sub_in_ixd,
/*TODO*///	/* 0xe500 */
/*TODO*///	sub_in_rg,	sub_in_rgd,	sub_in_in,	sub_in_ind,	sub_in_de,	sub_in_ded,	sub_in_ix,	sub_in_ixd,
/*TODO*///	sub_in_rg,	sub_in_rgd,	sub_in_in,	sub_in_ind,	sub_in_de,	sub_in_ded,	sub_in_ix,	sub_in_ixd,
/*TODO*///	sub_in_rg,	sub_in_rgd,	sub_in_in,	sub_in_ind,	sub_in_de,	sub_in_ded,	sub_in_ix,	sub_in_ixd,
/*TODO*///	sub_in_rg,	sub_in_rgd,	sub_in_in,	sub_in_ind,	sub_in_de,	sub_in_ded,	sub_in_ix,	sub_in_ixd,
/*TODO*///	/* 0xe600 */
/*TODO*///	sub_ind_rg,	sub_ind_rgd,sub_ind_in,	sub_ind_ind,sub_ind_de,	sub_ind_ded,sub_ind_ix,	sub_ind_ixd,
/*TODO*///	sub_ind_rg,	sub_ind_rgd,sub_ind_in,	sub_ind_ind,sub_ind_de,	sub_ind_ded,sub_ind_ix,	sub_ind_ixd,
/*TODO*///	sub_ind_rg,	sub_ind_rgd,sub_ind_in,	sub_ind_ind,sub_ind_de,	sub_ind_ded,sub_ind_ix,	sub_ind_ixd,
/*TODO*///	sub_ind_rg,	sub_ind_rgd,sub_ind_in,	sub_ind_ind,sub_ind_de,	sub_ind_ded,sub_ind_ix,	sub_ind_ixd,
/*TODO*///	/* 0xe700 */
/*TODO*///	sub_ind_rg,	sub_ind_rgd,sub_ind_in,	sub_ind_ind,sub_ind_de,	sub_ind_ded,sub_ind_ix,	sub_ind_ixd,
/*TODO*///	sub_ind_rg,	sub_ind_rgd,sub_ind_in,	sub_ind_ind,sub_ind_de,	sub_ind_ded,sub_ind_ix,	sub_ind_ixd,
/*TODO*///	sub_ind_rg,	sub_ind_rgd,sub_ind_in,	sub_ind_ind,sub_ind_de,	sub_ind_ded,sub_ind_ix,	sub_ind_ixd,
/*TODO*///	sub_ind_rg,	sub_ind_rgd,sub_ind_in,	sub_ind_ind,sub_ind_de,	sub_ind_ded,sub_ind_ix,	sub_ind_ixd,
/*TODO*///	/* 0xe800 */
/*TODO*///	sub_de_rg,	sub_de_rgd,	sub_de_in,	sub_de_ind,	sub_de_de,	sub_de_ded,	sub_de_ix,	sub_de_ixd,
/*TODO*///	sub_de_rg,	sub_de_rgd,	sub_de_in,	sub_de_ind,	sub_de_de,	sub_de_ded,	sub_de_ix,	sub_de_ixd,
/*TODO*///	sub_de_rg,	sub_de_rgd,	sub_de_in,	sub_de_ind,	sub_de_de,	sub_de_ded,	sub_de_ix,	sub_de_ixd,
/*TODO*///	sub_de_rg,	sub_de_rgd,	sub_de_in,	sub_de_ind,	sub_de_de,	sub_de_ded,	sub_de_ix,	sub_de_ixd,
/*TODO*///	/* 0xe900 */
/*TODO*///	sub_de_rg,	sub_de_rgd,	sub_de_in,	sub_de_ind,	sub_de_de,	sub_de_ded,	sub_de_ix,	sub_de_ixd,
/*TODO*///	sub_de_rg,	sub_de_rgd,	sub_de_in,	sub_de_ind,	sub_de_de,	sub_de_ded,	sub_de_ix,	sub_de_ixd,
/*TODO*///	sub_de_rg,	sub_de_rgd,	sub_de_in,	sub_de_ind,	sub_de_de,	sub_de_ded,	sub_de_ix,	sub_de_ixd,
/*TODO*///	sub_de_rg,	sub_de_rgd,	sub_de_in,	sub_de_ind,	sub_de_de,	sub_de_ded,	sub_de_ix,	sub_de_ixd,
/*TODO*///	/* 0xea00 */
/*TODO*///	sub_ded_rg,	sub_ded_rgd,sub_ded_in,	sub_ded_ind,sub_ded_de,	sub_ded_ded,sub_ded_ix,	sub_ded_ixd,
/*TODO*///	sub_ded_rg,	sub_ded_rgd,sub_ded_in,	sub_ded_ind,sub_ded_de,	sub_ded_ded,sub_ded_ix,	sub_ded_ixd,
/*TODO*///	sub_ded_rg,	sub_ded_rgd,sub_ded_in,	sub_ded_ind,sub_ded_de,	sub_ded_ded,sub_ded_ix,	sub_ded_ixd,
/*TODO*///	sub_ded_rg,	sub_ded_rgd,sub_ded_in,	sub_ded_ind,sub_ded_de,	sub_ded_ded,sub_ded_ix,	sub_ded_ixd,
/*TODO*///	/* 0xeb00 */
/*TODO*///	sub_ded_rg,	sub_ded_rgd,sub_ded_in,	sub_ded_ind,sub_ded_de,	sub_ded_ded,sub_ded_ix,	sub_ded_ixd,
/*TODO*///	sub_ded_rg,	sub_ded_rgd,sub_ded_in,	sub_ded_ind,sub_ded_de,	sub_ded_ded,sub_ded_ix,	sub_ded_ixd,
/*TODO*///	sub_ded_rg,	sub_ded_rgd,sub_ded_in,	sub_ded_ind,sub_ded_de,	sub_ded_ded,sub_ded_ix,	sub_ded_ixd,
/*TODO*///	sub_ded_rg,	sub_ded_rgd,sub_ded_in,	sub_ded_ind,sub_ded_de,	sub_ded_ded,sub_ded_ix,	sub_ded_ixd,
/*TODO*///	/* 0xec00 */
/*TODO*///	sub_ix_rg,	sub_ix_rgd,	sub_ix_in,	sub_ix_ind,	sub_ix_de,	sub_ix_ded,	sub_ix_ix,	sub_ix_ixd,
/*TODO*///	sub_ix_rg,	sub_ix_rgd,	sub_ix_in,	sub_ix_ind,	sub_ix_de,	sub_ix_ded,	sub_ix_ix,	sub_ix_ixd,
/*TODO*///	sub_ix_rg,	sub_ix_rgd,	sub_ix_in,	sub_ix_ind,	sub_ix_de,	sub_ix_ded,	sub_ix_ix,	sub_ix_ixd,
/*TODO*///	sub_ix_rg,	sub_ix_rgd,	sub_ix_in,	sub_ix_ind,	sub_ix_de,	sub_ix_ded,	sub_ix_ix,	sub_ix_ixd,
/*TODO*///	/* 0xed00 */
/*TODO*///	sub_ix_rg,	sub_ix_rgd,	sub_ix_in,	sub_ix_ind,	sub_ix_de,	sub_ix_ded,	sub_ix_ix,	sub_ix_ixd,
/*TODO*///	sub_ix_rg,	sub_ix_rgd,	sub_ix_in,	sub_ix_ind,	sub_ix_de,	sub_ix_ded,	sub_ix_ix,	sub_ix_ixd,
/*TODO*///	sub_ix_rg,	sub_ix_rgd,	sub_ix_in,	sub_ix_ind,	sub_ix_de,	sub_ix_ded,	sub_ix_ix,	sub_ix_ixd,
/*TODO*///	sub_ix_rg,	sub_ix_rgd,	sub_ix_in,	sub_ix_ind,	sub_ix_de,	sub_ix_ded,	sub_ix_ix,	sub_ix_ixd,
/*TODO*///	/* 0xee00 */
/*TODO*///	sub_ixd_rg,	sub_ixd_rgd,sub_ixd_in,	sub_ixd_ind,sub_ixd_de,	sub_ixd_ded,sub_ixd_ix,	sub_ixd_ixd,
/*TODO*///	sub_ixd_rg,	sub_ixd_rgd,sub_ixd_in,	sub_ixd_ind,sub_ixd_de,	sub_ixd_ded,sub_ixd_ix,	sub_ixd_ixd,
/*TODO*///	sub_ixd_rg,	sub_ixd_rgd,sub_ixd_in,	sub_ixd_ind,sub_ixd_de,	sub_ixd_ded,sub_ixd_ix,	sub_ixd_ixd,
/*TODO*///	sub_ixd_rg,	sub_ixd_rgd,sub_ixd_in,	sub_ixd_ind,sub_ixd_de,	sub_ixd_ded,sub_ixd_ix,	sub_ixd_ixd,
/*TODO*///	/* 0xef00 */
/*TODO*///	sub_ixd_rg,	sub_ixd_rgd,sub_ixd_in,	sub_ixd_ind,sub_ixd_de,	sub_ixd_ded,sub_ixd_ix,	sub_ixd_ixd,
/*TODO*///	sub_ixd_rg,	sub_ixd_rgd,sub_ixd_in,	sub_ixd_ind,sub_ixd_de,	sub_ixd_ded,sub_ixd_ix,	sub_ixd_ixd,
/*TODO*///	sub_ixd_rg,	sub_ixd_rgd,sub_ixd_in,	sub_ixd_ind,sub_ixd_de,	sub_ixd_ded,sub_ixd_ix,	sub_ixd_ixd,
/*TODO*///	sub_ixd_rg,	sub_ixd_rgd,sub_ixd_in,	sub_ixd_ind,sub_ixd_de,	sub_ixd_ded,sub_ixd_ix,	sub_ixd_ixd,
/*TODO*///
/*TODO*///	/* 0xf000 */
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	/* 0xf100 */
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	/* 0xf200 */
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	/* 0xf300 */
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	/* 0xf400 */
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	/* 0xf500 */
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	/* 0xf600 */
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	/* 0xf700 */
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	/* 0xf800 */
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	/* 0xf900 */
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	/* 0xfa00 */
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	/* 0xfb00 */
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	/* 0xfc00 */
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	/* 0xfd00 */
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	/* 0xfe00 */
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	/* 0xff00 */
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,
/*TODO*///	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal,	illegal
/*TODO*///};
    
}
