/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package amame.cpu.z80;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import static java.lang.System.getLogger;


public class Tables {

    private static final Logger logger = getLogger(Tables.class.getName());

//#region Table

    public static void initialiseTables() {
        initTableInc();
        initTableDec();
        initTableParity();
        initTableALU();
        InitTableRotShift();
        InitTableHalfBorrow();
        InitTableHalfCarry();
        initTableNeg();
        initTableDaa();
        initTableCc();
    }

    static int[] tableInc;

    private static void initTableInc() {
        tableInc = new int[256];
        for (int i = 0; i < 256; ++i) {
            tableInc[i] = flagByte(false, false, i == 0x80, undocumentedX(i), (i & 0xF) == 0x0, undocumentedY(i), i == 0, i > 127);
        }
    }

    static int[] tableDec;

    private static void initTableDec() {
        tableDec = new int[256];
        for (int i = 0; i < 256; ++i) {
            tableDec[i] = flagByte(false, true, i == 0x7F, undocumentedX(i), (i & 0xF) == 0xF, undocumentedY(i), i == 0, i > 127);
        }
    }

    static boolean[] tableParity;

    private static void initTableParity() {
        tableParity = new boolean[256];
        for (int i = 0; i < 256; ++i) {
            int bits = 0;
            for (int j = 0; j < 8; ++j) {
                bits += (i >>> j) & 1;
            }
            tableParity[i] = (bits & 1) == 0;
        }
    }

    static int[][][][] tableALU;

    private static void initTableALU() {
        tableALU = new int[8][256][256][2]; // Class, OP1, OP2, Carry

        for (int i = 0; i < 8; ++i) {
            for (int op1 = 0; op1 < 256; ++op1) {
                for (int op2 = 0; op2 < 256; ++op2) {
                    for (int c = 0; c < 2; ++c) {

                        int ac = (i == 1 || i == 3) ? c : 0;

                        boolean S = false;
                        boolean Z = false;
                        boolean C = false;
                        boolean H = false;
                        boolean N = false;
                        boolean P = false;

                        int result_b = 0;
                        int result_si = 0;
                        int result_ui = 0;

                        // Fetch result
                        switch (i) {
                            case 0:
                            case 1:
                                result_si = (byte) op1 + (byte) op2 + ac;
                                result_ui = op1 + op2 + ac;
                                break;
                            case 2:
                            case 3:
                            case 7:
                                result_si = (byte) op1 - (byte) op2 - ac;
                                result_ui = op1 - op2 - ac;
                                break;
                            case 4:
                                result_si = op1 & op2;
                                break;
                            case 5:
                                result_si = op1 ^ op2;
                                break;
                            case 6:
                                result_si = op1 | op2;
                                break;
                        }

                        result_b = result_si & 0xff;

                        // Parity/Carry

                        switch (i) {
                            case 0:
                            case 1:
                            case 2:
                            case 3:
                            case 7:
                                P = result_si < -128 || result_si > 127;
                                C = result_ui < 0 || result_ui > 255;
                                break;
                            case 4:
                            case 5:
                            case 6:
                                P = tableParity[result_b];
                                C = false;
                                break;
                        }

                        // Subtraction
                        N = i == 2 || i == 3 || i == 7;

                        // Half carry
                        switch (i) {
                            case 0:
                            case 1:
                                H = ((op1 & 0xF) + (op2 & 0xF) + (ac & 0xF)) > 0xF;
                                break;
                            case 2:
                            case 3:
                            case 7:
                                H = ((op1 & 0xF) - (op2 & 0xF) - (ac & 0xF)) < 0x0;
                                break;
                            case 4:
                                H = true;
                                break;
                            case 5:
                            case 6:
                                H = false;
                                break;
                        }

                        // Undocumented
                        int UndocumentedFlags = result_b & 0x28;
                        if (i == 7) UndocumentedFlags = op2 & 0x28;

                        S = result_b > 127;
                        Z = result_b == 0;

                        if (i == 7) result_b = op1 & 0xff;

                        tableALU[i][op1][op2][c] =
                                result_b * 256 +
                                        ((C ? 0x01 : 0) + (N ? 0x02 : 0) + (P ? 0x04 : 0) + (H ? 0x10 : 0) + (Z ? 0x40 : 0) + (S ? 0x80 : 0)) +
                                        UndocumentedFlags;
if (tableALU[i][op1][op2][c] < 0) logger.log(Level.ERROR, "@@@@: tableALU: " + tableALU[i][op1][op2][c]);
                    }
                }
            }
        }
    }

    static boolean[][] tableHalfBorrow;

    private static void InitTableHalfBorrow() {
        tableHalfBorrow = new boolean[256][256];
        for (int i = 0; i < 256; i++) {
            for (int j = 0; j < 256; j++) {
                tableHalfBorrow[i][j] = ((i & 0xF) - (j & 0xF)) < 0;
            }
        }
    }

    static boolean[][] tableHalfCarry;

    private static void InitTableHalfCarry() {
        tableHalfCarry = new boolean[256][256];
        for (int i = 0; i < 256; i++) {
            for (int j = 0; j < 256; j++) {
                tableHalfCarry[i][j] = ((i & 0xF) + (j & 0xF)) > 0xF;
            }
        }
    }

    static int[][][] tableRotShift;

    private static void InitTableRotShift() {
        tableRotShift = new int[2][8][65536]; // All, operation, AF
        for (int all = 0; all < 2; all++) {
            for (int y = 0; y < 8; ++y) {
                for (int af = 0; af < 65536; af++) {
                    int old = (af >> 8) & 0xff;
                    boolean OldCarry = (af & 0x01) != 0;

                    int newAf = (af & ~0x13) & 0xffff; // Clear HALF-CARRY, SUBTRACT and CARRY flags

                    int new_ = old;
                    if ((y & 1) == 0) {
                        if ((old & 0x80) != 0) ++newAf;

                        new_ = (new_ << 1) & 0xff;

                        if ((y & 0x04) == 0) {
                            if (((y & 0x02) == 0) ? ((newAf & 0x01) != 0) : OldCarry) new_ |= 0x01;
                        } else {
                            if ((y & 0x02) != 0) new_ |= 0x01;
                        }

                    } else {

                        if ((old & 0x01) != 0) ++newAf;

                        new_ = (new_ >>> 1) & 0xff;

                        if ((y & 0x04) == 0) {
                            if (((y & 0x02) == 0) ? ((newAf & 0x01) != 0) : OldCarry) new_ |= 0x80;
                        } else {
                            if ((y & 0x02) == 0) new_ |= (old & 0x80);
                        }
                    }

                    newAf &= 0xFF;
                    newAf |= (new_ * 256) & 0xffff;

                    if (all == 1) {
                        newAf &= ~0xC4 & 0xffff; // Clear S, Z, P
                        if ((new_ & 0xff) > 127) newAf |= 0x80;
                        if (new_ == 0) newAf |= 0x40;
                        if (tableParity[new_ & 0xff]) newAf |= 0x04;
                    }

                    tableRotShift[all][y][af] = ((newAf & ~0x28) | ((newAf >> 8) & 0x28)) & 0xffff;
if (tableRotShift[all][y][af] < 0) logger.log(Level.ERROR, "@@@@: tableRotShift: " + tableRotShift[all][y][af]);
                }
            }
        }
    }

    static int[] tableNeg;

    private static void initTableNeg() {
        tableNeg = new int[65536];
        for (int af = 0; af < 65536; af++) {
            int raf = 0;
            int b = (af >>> 8);
            int a = -b;
            raf |= a * 256;
            raf |= flagByte(b != 0x00, true, b == 0x80, undocumentedX(a), tableHalfCarry[a & 0xff][b & 0xff],
                    undocumentedY(a), a == 0, (a & 0xff) > 127);
            tableNeg[af] = raf & 0xffff;
if (tableNeg[af] < 0) logger.log(Level.ERROR, "@@@@: tableNeg" + tableNeg[af]);
        }
    }

    static int[] TableDaa;

    private static void initTableDaa() {
        TableDaa = new int[65536];
        for (int af = 0; af < 65536; ++af) {
            int a = af >>> 8;
            int tmp = a;

            if (isN(af)) {
                if (isH(af) || ((a & 0x0F) > 0x09)) tmp -= 0x06;
                if (isC(af) || (a & 0xff) > 0x99) tmp -= 0x60;
            } else {
                if (isH(af) || ((a & 0x0F) > 0x09)) tmp += 0x06;
                if (isC(af) || (a & 0xff) > 0x99) tmp += 0x60;
            }

            TableDaa[af] = ((tmp * 256) + flagByte(isC(af) || (a & 0xff) > 0x99, isN(af), tableParity[tmp & 0xff], undocumentedX(tmp), ((a ^ tmp) & 0x10) != 0, undocumentedY(tmp), tmp == 0, (tmp & 0xff) > 127)) & 0xffff;
if (TableDaa[af] < 0) logger.log(Level.ERROR, "@@@@: TableDaa: " + TableDaa[af]);
        }
    }

    static int[] cc_op, cc_cb, cc_ed, cc_xy, cc_xycb, cc_ex;

    private static void initTableCc() {
        cc_op = new int[] {
                4, 10, 7, 6, 4, 4, 7, 4, 4, 11, 7, 6, 4, 4, 7, 4,
                8, 10, 7, 6, 4, 4, 7, 4, 12, 11, 7, 6, 4, 4, 7, 4,
                7, 10, 16, 6, 4, 4, 7, 4, 7, 11, 16, 6, 4, 4, 7, 4,
                7, 10, 13, 6, 11, 11, 10, 4, 7, 11, 13, 6, 4, 4, 7, 4,
                4, 4, 4, 4, 4, 4, 7, 4, 4, 4, 4, 4, 4, 4, 7, 4,
                4, 4, 4, 4, 4, 4, 7, 4, 4, 4, 4, 4, 4, 4, 7, 4,
                4, 4, 4, 4, 4, 4, 7, 4, 4, 4, 4, 4, 4, 4, 7, 4,
                7, 7, 7, 7, 7, 7, 4, 7, 4, 4, 4, 4, 4, 4, 7, 4,
                4, 4, 4, 4, 4, 4, 7, 4, 4, 4, 4, 4, 4, 4, 7, 4,
                4, 4, 4, 4, 4, 4, 7, 4, 4, 4, 4, 4, 4, 4, 7, 4,
                4, 4, 4, 4, 4, 4, 7, 4, 4, 4, 4, 4, 4, 4, 7, 4,
                4, 4, 4, 4, 4, 4, 7, 4, 4, 4, 4, 4, 4, 4, 7, 4,
                5, 10, 10, 10, 10, 11, 7, 11, 5, 10, 10, 0, 10, 17, 7, 11,
                5, 10, 10, 11, 10, 11, 7, 11, 5, 4, 10, 11, 10, 0, 7, 11,
                5, 10, 10, 19, 10, 11, 7, 11, 5, 4, 10, 4, 10, 0, 7, 11,
                5, 10, 10, 4, 10, 11, 7, 11, 5, 6, 10, 4, 10, 0, 7, 11
        };
        cc_cb = new int[] {
                8, 8, 8, 8, 8, 8, 15, 8, 8, 8, 8, 8, 8, 8, 15, 8,
                8, 8, 8, 8, 8, 8, 15, 8, 8, 8, 8, 8, 8, 8, 15, 8,
                8, 8, 8, 8, 8, 8, 15, 8, 8, 8, 8, 8, 8, 8, 15, 8,
                8, 8, 8, 8, 8, 8, 15, 8, 8, 8, 8, 8, 8, 8, 15, 8,
                8, 8, 8, 8, 8, 8, 12, 8, 8, 8, 8, 8, 8, 8, 12, 8,
                8, 8, 8, 8, 8, 8, 12, 8, 8, 8, 8, 8, 8, 8, 12, 8,
                8, 8, 8, 8, 8, 8, 12, 8, 8, 8, 8, 8, 8, 8, 12, 8,
                8, 8, 8, 8, 8, 8, 12, 8, 8, 8, 8, 8, 8, 8, 12, 8,
                8, 8, 8, 8, 8, 8, 15, 8, 8, 8, 8, 8, 8, 8, 15, 8,
                8, 8, 8, 8, 8, 8, 15, 8, 8, 8, 8, 8, 8, 8, 15, 8,
                8, 8, 8, 8, 8, 8, 15, 8, 8, 8, 8, 8, 8, 8, 15, 8,
                8, 8, 8, 8, 8, 8, 15, 8, 8, 8, 8, 8, 8, 8, 15, 8,
                8, 8, 8, 8, 8, 8, 15, 8, 8, 8, 8, 8, 8, 8, 15, 8,
                8, 8, 8, 8, 8, 8, 15, 8, 8, 8, 8, 8, 8, 8, 15, 8,
                8, 8, 8, 8, 8, 8, 15, 8, 8, 8, 8, 8, 8, 8, 15, 8,
                8, 8, 8, 8, 8, 8, 15, 8, 8, 8, 8, 8, 8, 8, 15, 8
        };
        cc_ed = new int[] {
                8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
                8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
                8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
                8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
                12, 12, 15, 20, 8, 14, 8, 9, 12, 12, 15, 20, 8, 14, 8, 9,
                12, 12, 15, 20, 8, 14, 8, 9, 12, 12, 15, 20, 8, 14, 8, 9,
                12, 12, 15, 20, 8, 14, 8, 18, 12, 12, 15, 20, 8, 14, 8, 18,
                12, 12, 15, 20, 8, 14, 8, 8, 12, 12, 15, 20, 8, 14, 8, 8,
                8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
                8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
                16, 16, 16, 16, 8, 8, 8, 8, 16, 16, 16, 16, 8, 8, 8, 8,
                16, 16, 16, 16, 8, 8, 8, 8, 16, 16, 16, 16, 8, 8, 8, 8,
                8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
                8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
                8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
                8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8
        };
        cc_xy = new int[] {
                4, 4, 4, 4, 4, 4, 4, 4, 4, 15, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 15, 4, 4, 4, 4, 4, 4,
                4, 14, 20, 10, 9, 9, 9, 4, 4, 15, 20, 10, 9, 9, 9, 4,
                4, 4, 4, 4, 23, 23, 19, 4, 4, 15, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 9, 9, 19, 4, 4, 4, 4, 4, 9, 9, 19, 4,
                4, 4, 4, 4, 9, 9, 19, 4, 4, 4, 4, 4, 9, 9, 19, 4,
                9, 9, 9, 9, 9, 9, 19, 9, 9, 9, 9, 9, 9, 9, 19, 9,
                19, 19, 19, 19, 19, 19, 4, 19, 4, 4, 4, 4, 9, 9, 19, 4,
                4, 4, 4, 4, 9, 9, 19, 4, 4, 4, 4, 4, 9, 9, 19, 4,
                4, 4, 4, 4, 9, 9, 19, 4, 4, 4, 4, 4, 9, 9, 19, 4,
                4, 4, 4, 4, 9, 9, 19, 4, 4, 4, 4, 4, 9, 9, 19, 4,
                4, 4, 4, 4, 9, 9, 19, 4, 4, 4, 4, 4, 9, 9, 19, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 0, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 14, 4, 23, 4, 15, 4, 4, 4, 8, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 10, 4, 4, 4, 4, 4, 4
        };
        cc_xycb = new int[] {
                23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
                23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
                23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
                23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
                20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20,
                20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20,
                20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20,
                20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20,
                23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
                23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
                23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
                23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
                23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
                23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
                23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
                23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23
        };
        cc_ex = new int[] {
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                5, 0, 0, 0, 0, 0, 0, 0, 5, 0, 0, 0, 0, 0, 0, 0,
                5, 0, 0, 0, 0, 0, 0, 0, 5, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                5, 5, 5, 5, 0, 0, 0, 0, 5, 5, 5, 5, 0, 0, 0, 0,
                6, 0, 0, 0, 7, 0, 0, 2, 6, 0, 0, 0, 7, 0, 0, 2,
                6, 0, 0, 0, 7, 0, 0, 2, 6, 0, 0, 0, 7, 0, 0, 2,
                6, 0, 0, 0, 7, 0, 0, 2, 6, 0, 0, 0, 7, 0, 0, 2,
                6, 0, 0, 0, 7, 0, 0, 2, 6, 0, 0, 0, 7, 0, 0, 2
        };
    }

    private static int flagByte(boolean C, boolean N, boolean P, boolean X, boolean H, boolean Y, boolean Z, boolean S) {
        return (C ? 0x01 : 0) +
                (N ? 0x02 : 0) +
                (P ? 0x04 : 0) +
                (X ? 0x08 : 0) +
                (H ? 0x10 : 0) +
                (Y ? 0x20 : 0) +
                (Z ? 0x40 : 0) +
                (S ? 0x80 : 0);
    }

    public static boolean undocumentedX(int value) {
        return (value & 0x08) != 0;
    }

    public static boolean undocumentedY(int value) {
        return (value & 0x20) != 0;
    }

    public static boolean isC(int value) {
        return (value & 0x01) != 0;
    }

    public static boolean isN(int value) {
        return (value & 0x02) != 0;
    }

    public static boolean isP(int value) {
        return (value & 0x04) != 0;
    }

    public static boolean isX(int value) {
        return (value & 0x08) != 0;
    }

    public static boolean isH(int value) {
        return (value & 0x10) != 0;
    }

    public static boolean isY(int value) {
        return (value & 0x20) != 0;
    }

    public static boolean isZ(int value) {
        return (value & 0x40) != 0;
    }

    public static boolean isS(int value) {
        return (value & 0x80) != 0;
    }

//#endregion
}
