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
 * Intel 8251 USART (NEC uPD71051), asynchronous mode, after mame's i8251.cpp.
 * <p>
 * mame clocks this bit by bit from a 500kHz clock device. Here characters are moved whole,
 * timed by the baud rate the mode byte and the clock make, which keeps the status bits and the
 * rxrdy, txrdy lines the program sees as they are without a timer event per clock.
 * Synchronous mode is not there, nothing here uses it.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-21 nsano initial version <br>
 */
public final class I8251 {

    private static final int STATUS_FRAMING_ERROR = 0x20;
    private static final int STATUS_OVERRUN_ERROR = 0x10;
    private static final int STATUS_PARITY_ERROR = 0x08;
    private static final int STATUS_TX_EMPTY = 0x04;
    private static final int STATUS_RX_READY = 0x02;
    private static final int STATUS_TX_READY = 0x01;

    private static final int NEXT_COMMAND = 0;
    private static final int NEXT_MODE = 1;

    /** the txc/rxc clock */
    private final double clock;

    private IntConsumer rxrdyHandler = state -> {};
    private IntConsumer txrdyHandler = state -> {};
    private IntConsumer txemptyHandler = state -> {};
    /** characters going out, the txd pin a byte at a time */
    private IntConsumer txdHandler = data -> {};

    private int flags;
    private int status;
    private int command;
    private int mode;
    private int brFactor;
    private int dataBits;
    private int stopBits2;
    private boolean parity;

    private int rxData;
    private int txData;
    private boolean cts = true;
    /** the tx enable when the holding register was written, a character written goes out even if tx is disabled after */
    private boolean delayedTxEn;
    private boolean dsr = true;

    /** the shift register, the character in it or -1 */
    private int txShift = -1;

    private final EmuTimer txTimer;
    private int lastRxrdy = -1, lastTxrdy = -1, lastTxempty = -1;

    /**
     * @param clock the frequency on txc and rxc
     */
    public I8251(Machine machine, double clock) {
        this.clock = clock;
        this.txTimer = machine.timer(this::txDone);
    }

    public void setRxrdyHandler(IntConsumer handler) {
        rxrdyHandler = handler;
    }

    public void setTxrdyHandler(IntConsumer handler) {
        txrdyHandler = handler;
    }

    public void setTxemptyHandler(IntConsumer handler) {
        txemptyHandler = handler;
    }

    public void setTxdHandler(IntConsumer handler) {
        txdHandler = handler;
    }

    public void reset() {
        txTimer.enable(false);
        txShift = -1;
        flags = NEXT_MODE;
        status = STATUS_TX_EMPTY | STATUS_TX_READY;
        mode = 0;
        command = 0;
        rxData = 0;
        txData = 0;
        delayedTxEn = false;
        brFactor = 1;
        dataBits = 8;
        stopBits2 = 2;
        parity = false;
        update();
    }

    /** seconds a character takes on the wire */
    private double characterTime() {
        // start + data + parity + stop, the stop bits in halves
        double bits = 1 + dataBits + (parity ? 1 : 0) + stopBits2 / 2.0;
        return bits * brFactor / clock;
    }

    private boolean isTxEnabled() {
        return (command & 0x01) != 0 && !cts;
    }

    private int rxrdy() {
        if ((command & 0x04) == 0) return 0;
        return (status & STATUS_RX_READY) != 0 ? 1 : 0;
    }

    private int txrdy() {
        return isTxEnabled() && (status & STATUS_TX_READY) != 0 ? 1 : 0;
    }

    private void update() {
        int rxrdy = rxrdy();
        if (rxrdy != lastRxrdy) {
            lastRxrdy = rxrdy;
            rxrdyHandler.accept(rxrdy);
        }
        int txrdy = txrdy();
        if (txrdy != lastTxrdy) {
            lastTxrdy = txrdy;
            txrdyHandler.accept(txrdy);
        }
        int txempty = (status & STATUS_TX_EMPTY) != 0 ? 1 : 0;
        if (txempty != lastTxempty) {
            lastTxempty = txempty;
            txemptyHandler.accept(txempty);
        }
    }

    /** moves the holding register to the shift register when it can */
    private void checkForTxStart() {
        checkForTxStart(isTxEnabled());
    }

    private void checkForTxStart(boolean enabled) {
        if (txShift < 0 && (status & STATUS_TX_READY) == 0 && enabled) {
            txShift = txData;
            status |= STATUS_TX_READY;
            status &= ~STATUS_TX_EMPTY;
            txTimer.adjust(characterTime());
        }
    }

    private void txDone() {
        txdHandler.accept(txShift);
        txShift = -1;
        status |= STATUS_TX_EMPTY;
        checkForTxStart(isTxEnabled() || delayedTxEn);
        update();
    }

    /** a character arrived on rxd */
    public void receiveCharacter(int data) {
        if ((command & 0x04) == 0) return;
        rxData = data & ((1 << dataBits) - 1);
        if ((status & STATUS_RX_READY) != 0) {
            status |= STATUS_OVERRUN_ERROR;
        }
        status |= STATUS_RX_READY;
        update();
    }

    public void writeCts(boolean state) {
        cts = state;
        checkForTxStart();
        update();
    }

    public void writeDsr(boolean state) {
        dsr = !state;
    }

    private void commandW(int data) {
        command = data;
        if ((data & 0x10) != 0) {
            status &= ~(STATUS_PARITY_ERROR | STATUS_OVERRUN_ERROR | STATUS_FRAMING_ERROR);
        }
        if ((data & 0x40) != 0) {
            flags = NEXT_MODE;
        }
        checkForTxStart();
        update();
    }

    private void modeW(int data) {
        mode = data;
        dataBits = ((data >> 2) & 0x03) + 5;
        parity = (data & 0x10) != 0;
        stopBits2 = switch (data & 0xc0) {
            case 0x80 -> 3;
            case 0xc0 -> 4;
            default -> 2;
        };
        brFactor = switch (data & 0x03) {
            case 2 -> 16;
            case 3 -> 64;
            default -> 1;
        };
        flags = NEXT_COMMAND;
    }

    public int statusR() {
        return (dsr ? 0x80 : 0) | status;
    }

    public void controlW(int data) {
        if (flags == NEXT_MODE) {
            modeW(data);
        } else {
            commandW(data);
        }
    }

    public int dataR() {
        status &= ~STATUS_RX_READY;
        update();
        return rxData;
    }

    public void dataW(int data) {
        txData = data;
        status &= ~STATUS_TX_READY;
        // Store state of tx enable when writing to DB buffer
        delayedTxEn = isTxEnabled();
        checkForTxStart();
        update();
    }

    public int read(int offset) {
        return (offset & 1) != 0 ? statusR() : dataR();
    }

    public void write(int offset, int data) {
        if ((offset & 1) != 0) {
            controlW(data);
        } else {
            dataW(data);
        }
    }
}
