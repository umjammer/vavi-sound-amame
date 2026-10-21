/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package amame.devices;

import java.util.function.IntConsumer;

import amame.emu.EmuTimer;
import amame.emu.Machine;


/**
 * Motorola MC68681 DUART, after mame's mc68681.cpp.
 * <p>
 * Like {@link I8251} the channels move whole characters at the pace of their baud rate instead
 * of shifting bits, the registers, the status bits, the fifo and the interrupts are those of
 * mame. The counter/timer is there in full, the VFX ticks its clock with it.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-21 nsano initial version <br>
 */
public final class MC68681 {

    private static final int INT_INPUT_PORT_CHANGE = 0x80;
    private static final int INT_DELTA_BREAK_B = 0x40;
    private static final int INT_RXRDY_FFULLB = 0x20;
    private static final int INT_TXRDYB = 0x10;
    private static final int INT_COUNTER_READY = 0x08;
    private static final int INT_DELTA_BREAK_A = 0x04;
    private static final int INT_RXRDY_FFULLA = 0x02;
    private static final int INT_TXRDYA = 0x01;

    private static final int STATUS_RECEIVED_BREAK = 0x80;
    private static final int STATUS_FRAMING_ERROR = 0x40;
    private static final int STATUS_PARITY_ERROR = 0x20;
    private static final int STATUS_OVERRUN_ERROR = 0x10;
    private static final int STATUS_TRANSMITTER_EMPTY = 0x08;
    private static final int STATUS_TRANSMITTER_READY = 0x04;
    private static final int STATUS_FIFO_FULL = 0x02;
    private static final int STATUS_RECEIVER_READY = 0x01;

    private static final int MODE_RX_INT_SELECT_BIT = 0x40;
    private static final int MODE_BLOCK_ERROR = 0x20;

    private static final int RX_FIFO_SIZE = 3;

    private static final int[] BAUD_RATE_ACR_0 = {50, 110, 134, 200, 300, 600, 1200, 1050, 2400, 4800, 7200, 9600, 38400, 0, 0, 0};
    private static final int[] BAUD_RATE_ACR_1 = {75, 110, 134, 150, 300, 600, 1200, 2000, 2400, 4800, 1800, 9600, 19200, 0, 0, 0};

    private final double clock;
    private int ip3clk, ip4clk, ip5clk, ip6clk;

    private IntConsumer irqCallback = state -> {};
    private IntConsumer outport = data -> {};
    private final IntConsumer[] tx = {data -> {}, data -> {}};

    private int acr;
    private int imr;
    private int isr;
    private int opcr;
    private int opr;
    private int ctr;
    private int ipcr;
    private int ipLastState;
    private int halfPeriod;
    private boolean irqState;
    private int ivr = 0x0f;
    private boolean readVector;

    private final EmuTimer duartTimer;
    private final Channel[] channels = new Channel[2];

    /** one serial channel */
    private final class Channel {

        final int ch;
        int cr;
        int csr;
        int mr1;
        int mr2;
        int mrPtr;
        int sr;
        int rxBaudRate;
        int txBaudRate;
        boolean rxEnabled;
        final int[] rxFifo = new int[RX_FIFO_SIZE + 1];
        int rxFifoReadPtr;
        int rxFifoWritePtr;
        int rxFifoNum;
        int txData;
        boolean txDataInBuffer;
        boolean txBreak;
        boolean txEnabled;
        int dataBits = 8;
        boolean parity;
        int stopBits2 = 2;

        /** the character being shifted out or -1 */
        int txShift = -1;
        final EmuTimer txReadyTimer;
        final EmuTimer txDoneTimer;

        Channel(Machine machine, int ch) {
            this.ch = ch;
            txReadyTimer = machine.timer(this::startBitDone);
            txDoneTimer = machine.timer(this::traComplete);
        }

        void reset() {
            writeCR(0x10); // reset MR
            writeCR(0x20); // reset Rx
            writeCR(0x30); // reset Tx
            writeCR(0x40); // reset errors
            dataBits = 8;
            parity = false;
            stopBits2 = 2;
            txBaudRate = rxBaudRate = 0;
            csr = 0;
            txBreak = false;
        }

        double bitTime() {
            int baud = txBaudRate;
            if (baud <= 0 && (csr & 0x0f) == 0x0d) {
                // the timer drives the 16x clock, a rising edge every two counts
                double rate = getCtRate();
                int count = Math.max(ctr, 1);
                if (rate > 0) return 16 * 2.0 * count / rate;
            }
            if (baud <= 0) baud = 31250;
            return 1.0 / baud;
        }

        double characterTime() {
            return bitTime() * (1 + dataBits + (parity ? 1 : 0) + stopBits2 / 2.0);
        }

        /** TxRDY is not set until the end of start bit time */
        void startBitDone() {
            if (!txDataInBuffer && txEnabled) {
                sr |= STATUS_TRANSMITTER_READY;
                updateInterrupts();
            }
        }

        void traComplete() {
            // don't actually send in loopback mode
            if ((mr2 & 0xc0) != 0x80) {
                tx[ch].accept(txShift);
            } else {
                receiveCharacter(txShift);
            }
            txShift = -1;
            if (txDataInBuffer) {
                txDataInBuffer = false;
                startTx(txData);
            } else {
                sr |= STATUS_TRANSMITTER_EMPTY;
                if (txEnabled) sr |= STATUS_TRANSMITTER_READY;
                updateInterrupts();
            }
        }

        void startTx(int data) {
            txShift = data;
            txReadyTimer.adjust(bitTime());
            txDoneTimer.adjust(characterTime());
        }

        /** a character arrived on rxd */
        void receiveCharacter(int data) {
            if (rxEnabled || (mr2 & 0xc0) == 0x80) {
                rxFifoPush(data & ((1 << dataBits) - 1), 0);
            }
        }

        void rxFifoPush(int data, int errors) {
            if (rxFifoNum == RX_FIFO_SIZE + 1) {
                sr |= STATUS_OVERRUN_ERROR;
                // In case of overrun the FIFO tail entry is overwritten
                if (rxFifoWritePtr != 0)
                    rxFifoWritePtr--;
                else
                    rxFifoWritePtr = RX_FIFO_SIZE;
            }

            rxFifo[rxFifoWritePtr++] = data | (errors << 8);
            if (rxFifoWritePtr > RX_FIFO_SIZE)
                rxFifoWritePtr = 0;

            if (rxFifoNum <= RX_FIFO_SIZE) {
                rxFifoNum++;
            }

            if (rxFifoNum == 1) {
                sr |= STATUS_RECEIVER_READY;
                if ((mr1 & MODE_BLOCK_ERROR) == 0)
                    sr &= ~(STATUS_RECEIVED_BREAK | STATUS_FRAMING_ERROR | STATUS_PARITY_ERROR);
                sr |= errors;
            }
            if (rxFifoNum == RX_FIFO_SIZE)
                sr |= STATUS_FIFO_FULL;
            updateInterrupts();
        }

        void updateInterrupts() {
            switch (mr2 & 0xc0) {
                case 0x40, 0xc0 -> sr &= ~STATUS_TRANSMITTER_READY; // echo, remote loopback
                default -> {}
            }

            if ((sr & STATUS_TRANSMITTER_READY) != 0) {
                setIsrBits(ch == 0 ? INT_TXRDYA : INT_TXRDYB);
            } else {
                clearIsrBits(ch == 0 ? INT_TXRDYA : INT_TXRDYB);
            }
            int rxBit = ch == 0 ? INT_RXRDY_FFULLA : INT_RXRDY_FFULLB;
            boolean rx = (mr1 & MODE_RX_INT_SELECT_BIT) != 0 ? (sr & STATUS_FIFO_FULL) != 0 : (sr & STATUS_RECEIVER_READY) != 0;
            if (rx) setIsrBits(rxBit); else clearIsrBits(rxBit);
        }

        int readRxFifo() {
            if (rxFifoNum == 0) {
                updateInterrupts();
                return 0;
            }

            int rv = rxFifo[rxFifoReadPtr++];
            if (rxFifoReadPtr > RX_FIFO_SIZE) {
                rxFifoReadPtr = 0;
            }

            rxFifoNum--;
            if (rxFifoNum == RX_FIFO_SIZE - 1) {
                sr &= ~STATUS_FIFO_FULL;
            }

            if ((mr1 & MODE_BLOCK_ERROR) == 0)
                sr &= ~(STATUS_RECEIVED_BREAK | STATUS_FRAMING_ERROR | STATUS_PARITY_ERROR);
            if (rxFifoNum == 0)
                sr &= ~STATUS_RECEIVER_READY;
            else
                sr |= rxFifo[rxFifoReadPtr] >> 8;
            updateInterrupts();

            return rv & 0xff;
        }

        int readChanReg(int reg) {
            return switch (reg) {
                case 0 -> {
                    if (mrPtr == 0) {
                        mrPtr = 1;
                        yield mr1;
                    }
                    yield mr2;
                }
                case 1 -> sr;
                case 3 -> readRxFifo();
                default -> 0xff; // CSR, reading this is prohibited
            };
        }

        void writeChanReg(int reg, int data) {
            switch (reg) {
                case 0 -> writeMR(data);
                case 1 -> {
                    csr = data;
                    txBaudRate = calcBaud(ch, data & 0xf);
                    rxBaudRate = calcBaud(ch, (data >> 4) & 0xf);
                }
                case 2 -> writeCR(data);
                case 3 -> writeTX(data);
                default -> {}
            }
        }

        void writeMR(int data) {
            if (mrPtr == 0) {
                mr1 = data;
                mrPtr = 1;
            } else {
                mr2 = data;
            }
            dataBits = (mr1 & 3) + 5;
            parity = ((mr1 >> 3) & 3) != 2;
            stopBits2 = switch ((mr2 >> 2) & 3) {
                case 2 -> 3;
                case 3 -> 4;
                default -> 2;
            };
            updateInterrupts();
        }

        void writeCR(int data) {
            switch ((data >> 4) & 0x07) {
                case 1 -> mrPtr = 0; // Reset MR pointer
                case 2 -> { // Reset channel receiver (disable receiver and flush fifo)
                    rxEnabled = false;
                    sr &= ~STATUS_RECEIVER_READY;
                    sr &= ~(STATUS_RECEIVED_BREAK | STATUS_FRAMING_ERROR | STATUS_PARITY_ERROR);
                    sr &= ~STATUS_OVERRUN_ERROR;
                    rxFifoReadPtr = 0;
                    rxFifoWritePtr = 0;
                    rxFifoNum = 0;
                }
                case 3 -> { // Reset channel transmitter
                    sr &= ~(STATUS_TRANSMITTER_READY | STATUS_TRANSMITTER_EMPTY);
                    clearIsrBits(ch == 0 ? INT_TXRDYA : INT_TXRDYB);
                    txReadyTimer.enable(false);
                    txDoneTimer.enable(false);
                    txShift = -1;
                    txDataInBuffer = false;
                    txEnabled = false;
                }
                case 4 -> sr &= ~(STATUS_RECEIVED_BREAK | STATUS_FRAMING_ERROR | STATUS_PARITY_ERROR | STATUS_OVERRUN_ERROR);
                case 5 -> clearIsrBits(ch == 0 ? INT_DELTA_BREAK_A : INT_DELTA_BREAK_B);
                case 6 -> { // Start Tx break
                    if (!txBreak) {
                        txBreak = true;
                        if ((mr2 & 0xc0) == 0x80) {
                            setIsrBits(ch == 0 ? INT_DELTA_BREAK_A : INT_DELTA_BREAK_B);
                            rxFifoPush(0, STATUS_RECEIVED_BREAK);
                        }
                    }
                }
                case 7 -> { // Stop tx break
                    if (txBreak) {
                        txBreak = false;
                        if ((mr2 & 0xc0) == 0x80) {
                            setIsrBits(ch == 0 ? INT_DELTA_BREAK_A : INT_DELTA_BREAK_B);
                        }
                    }
                }
                default -> {}
            }

            if ((data & 0x01) != 0) {
                rxEnabled = true;
            }
            if ((data & 0x02) != 0) {
                rxEnabled = false;
                sr &= ~STATUS_RECEIVER_READY;
            }

            if ((cr & 0x04) == 0 && (data & 0x04) != 0) {
                sr |= STATUS_TRANSMITTER_READY | STATUS_TRANSMITTER_EMPTY;
                txDataInBuffer = false;
                txEnabled = true;
                setIsrBits(ch == 0 ? INT_TXRDYA : INT_TXRDYB);
            }
            if ((data & 0x08) != 0) {
                sr &= ~(STATUS_TRANSMITTER_READY | STATUS_TRANSMITTER_EMPTY);
                txDataInBuffer = false;
                txEnabled = false;
                clearIsrBits(ch == 0 ? INT_TXRDYA : INT_TXRDYB);
            }

            updateInterrupts();
            cr = data;
        }

        void writeTX(int data) {
            if ((sr & STATUS_TRANSMITTER_READY) == 0) {
                // transmitter not ready, data discarded
                return;
            }

            sr &= ~(STATUS_TRANSMITTER_READY | STATUS_TRANSMITTER_EMPTY);
            if (txShift >= 0) {
                txData = data;
                txDataInBuffer = true;
            } else {
                startTx(data);
            }

            updateInterrupts();
        }
    }

    public MC68681(Machine machine, double clock) {
        this.clock = clock;
        this.duartTimer = machine.timer(this::duartTimerCallback);
        channels[0] = new Channel(machine, 0);
        channels[1] = new Channel(machine, 1);
    }

    /** the clocks on ip3 to ip6 */
    public void setClocks(int clk3, int clk4, int clk5, int clk6) {
        ip3clk = clk3;
        ip4clk = clk4;
        ip5clk = clk5;
        ip6clk = clk6;
    }

    public void setIrqCallback(IntConsumer callback) {
        irqCallback = callback;
    }

    /** the output port, inverted the way the pins are */
    public void setOutport(IntConsumer outport) {
        this.outport = outport;
    }

    /** characters sent on a channel */
    public void setTx(int ch, IntConsumer tx) {
        this.tx[ch] = tx;
    }

    /** a character received on a channel */
    public void rx(int ch, int data) {
        channels[ch].receiveCharacter(data);
    }

    public void reset() {
        acr = 0;
        imr = 0;
        isr = 0;
        opcr = 0;
        opr = 0;
        ctr = 0;
        ipcr = 0;
        duartTimer.enable(false);
        irqState = false;
        irqCallback.accept(0);
        outport.accept(opr ^ 0xff);
        ivr = 0x0f;
        readVector = false;
        channels[0].reset();
        channels[1].reset();
    }

    private boolean irqPending() {
        return (isr & imr) != 0;
    }

    private void updateInterrupts() {
        if ((isr & imr) != 0) {
            if (!irqState) {
                irqState = true;
                irqCallback.accept(1);
            }
        } else {
            if (irqState) {
                irqState = false;
                irqCallback.accept(0);
            }
        }
        if ((opcr & 0xf0) != 0) {
            if ((opcr & 0x10) != 0) opr = (isr & 0x02) != 0 ? opr | 0x10 : opr & ~0x10;
            if ((opcr & 0x20) != 0) opr = (isr & 0x20) != 0 ? opr | 0x20 : opr & ~0x20;
            if ((opcr & 0x40) != 0) opr = (isr & 0x01) != 0 ? opr | 0x40 : opr & ~0x40;
            if ((opcr & 0x80) != 0) opr = (isr & 0x10) != 0 ? opr | 0x80 : opr & ~0x80;
            outport.accept(opr ^ 0xff);
        }
        if (!irqPending())
            readVector = false; // clear IACK too
    }

    /** the vector of the interrupt acknowledge cycle */
    public int getIrqVector() {
        readVector = true;
        return ivr;
    }

    private double getCtRate() {
        if ((acr & 0x40) != 0) {
            // Timer mode
            return switch ((acr >> 4) & 3) {
                case 3 -> clock / 16; // X1/CLK / 16
                default -> clock; // IP2, IP2 / 16 are not handled, X1/CLK
            };
        } else {
            // Counter mode
            return switch ((acr >> 4) & 3) {
                case 1 -> channels[0].txBaudRate; // TxCA
                case 2 -> channels[1].txBaudRate; // TxCB
                case 3 -> clock / 16; // X1/CLK / 16
                default -> clock; // IP2 is not handled
            };
        }
    }

    private int getCtCount() {
        double rate = getCtRate();
        double remaining = duartTimer.remaining();
        if (Double.isInfinite(remaining)) return 0;
        return (int) (remaining * rate) & 0xffff;
    }

    private void startCt(int count) {
        double rate = getCtRate();
        if (rate <= 0) {
            duartTimer.enable(false);
            return;
        }
        duartTimer.adjust(count / rate);
    }

    private void duartTimerCallback() {
        if ((acr & 0x40) != 0) {
            // Timer mode
            halfPeriod ^= 1;

            // timer output to bit 3?
            if ((opcr & 0xc) == 0x4) {
                opr ^= 0x8;
                outport.accept(opr ^ 0xff);
            }

            if (halfPeriod == 0)
                setIsrBits(INT_COUNTER_READY);

            int count = Math.max(ctr & 0xffff, 1);
            startCt(count);
        } else {
            // assert OP3 counter ready
            if ((opcr & 0xc) == 0x4)
                opr |= 0x8;

            // Counter mode
            setIsrBits(INT_COUNTER_READY);
            startCt(0xffff);
        }
    }

    public int read(int offset) {
        offset &= 0xf;
        if (offset == 0x0c) return ivr;

        int r = 0xff;
        switch (offset) {
            case 0x00, 0x01, 0x03 -> r = channels[0].readChanReg(offset & 3); // MR1A/MR2A, SRA, RHRA
            case 0x04 -> { // IPCR
                r = ipcr;
                // reading this clears all the input change bits
                ipcr &= 0x0f;
                clearIsrBits(INT_INPUT_PORT_CHANGE);
            }
            case 0x05 -> r = isr; // ISR
            case 0x06 -> r = getCtCount() >> 8; // CUR
            case 0x07 -> r = getCtCount() & 0xff; // CLR
            case 0x08, 0x09, 0x0b -> r = channels[1].readChanReg(offset & 3); // MR1B/MR2B, SRB, RHRB
            case 0x0a -> r = 0x61; // 1X/16X Test
            case 0x0d -> { // IP
                r = ipLastState | 0x80; // bit 7 is always set
                // bit 6 is /IACK (note the active-low)
                if (readVector)
                    r &= ~0x40;
                else
                    r |= 0x40;
            }
            case 0x0e -> { // Start counter command
                if ((acr & 0x40) != 0) {
                    // Reset the timer
                    halfPeriod = 0;
                }
                int count = Math.max(ctr & 0xffff, 1);
                startCt(count);
            }
            case 0x0f -> { // Stop counter command
                // Stop the counter only
                if ((acr & 0x40) == 0) {
                    duartTimer.enable(false);
                    // clear OP3 counter ready
                    if ((opcr & 0xc) == 0x4)
                        opr &= ~0x8;
                }
                clearIsrBits(INT_COUNTER_READY);
            }
            default -> {}
        }
        return r & 0xff;
    }

    public void write(int offset, int data) {
        offset &= 0x0f;
        data &= 0xff;
        switch (offset) {
            case 0x00, 0x01, 0x02, 0x03 -> channels[0].writeChanReg(offset & 3, data); // MRA, CSRA, CRA, THRA
            case 0x04 -> { // ACR
                int oldAcr = acr;
                acr = data;
                // bits 6-4: Counter/Timer Mode And Clock Source Select
                // bits 3-0: IP3-0 Change-Of-State Interrupt Enable
                if (((oldAcr ^ data) & 0x40) != 0) {
                    if ((data & 0x40) != 0) {
                        // Entering timer mode
                        int count = Math.max(ctr & 0xffff, 1);
                        halfPeriod = 0;
                        startCt(count);
                    } else {
                        // Leaving timer mode
                        duartTimer.enable(false);
                    }
                }

                // check for pending input port delta interrupts
                if ((((ipcr >> 4) & data) & 0x0f) != 0)
                    setIsrBits(INT_INPUT_PORT_CHANGE);

                for (Channel channel : channels) {
                    channel.writeChanReg(1, channel.csr);
                    channel.updateInterrupts();
                }
            }
            case 0x05 -> { // IMR
                imr = data;
                updateInterrupts();
            }
            case 0x06 -> ctr = (ctr & 0x00ff) | (data << 8); // CTUR
            case 0x07 -> ctr = (ctr & 0xff00) | data; // CTLR
            case 0x08, 0x09, 0x0a, 0x0b -> channels[1].writeChanReg(offset & 3, data); // MRB, CSRB, CRB, THRB
            case 0x0c -> ivr = data;
            case 0x0d -> opcr = data; // OPCR
            case 0x0e -> { // Set Output Port Bits
                opr |= data;
                outport.accept(opr ^ 0xff);
            }
            case 0x0f -> { // Reset Output Port Bits
                opr &= ~data;
                outport.accept(opr ^ 0xff);
            }
            default -> {}
        }
    }

    /** an input pin, 0 to 3 report their changes */
    public void ip(int bit, boolean state) {
        int mask = 1 << bit;
        int newIP = (ipLastState & ~mask) | (state ? mask : 0);
        if (bit < 4 && newIP != ipLastState) {
            ipcr &= ~0x0f;
            ipcr |= newIP & 0x0f;
            ipcr |= 0x10 << bit;
            if ((acr & mask) != 0)
                setIsrBits(INT_INPUT_PORT_CHANGE);
        }
        ipLastState = newIP;
    }

    private int calcBaud(int ch, int data) {
        int baudRate;
        if ((acr & 0x80) == 0) {
            baudRate = BAUD_RATE_ACR_0[data & 0x0f];
            if ((data & 0xf) == 0xe) {
                baudRate = (ch == 0 ? ip3clk : ip5clk) / 16;
            } else if ((data & 0xf) == 0xf) {
                baudRate = ch == 0 ? ip3clk : ip5clk;
            }
        } else {
            baudRate = BAUD_RATE_ACR_1[data & 0x0f];
        }
        return baudRate;
    }

    private void clearIsrBits(int mask) {
        if ((isr & mask) != 0) {
            isr &= ~mask;
            updateInterrupts();
        }
    }

    private void setIsrBits(int mask) {
        if ((isr & mask) == 0) {
            isr |= mask;
            updateInterrupts();
        }
    }
}
