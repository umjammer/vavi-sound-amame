/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package amame.drivers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import amame.cpu.Z80Cpu;
import amame.devices.HD44780;
import amame.devices.I8251;
import amame.devices.MidiPort;
import amame.emu.Machine;
import amame.emu.Panel;
import amame.emu.RomSet;
import amame.sound.YM2151;


/**
 * Yamaha FB-01, after mame's yamaha/fb01.cpp (Wilbert Pol).
 * <p>
 * A Z80, a YM2164, an uPD71051 uart on the midi and a 2x8 lcd, the 16KB of battery backed ram
 * holds the voices and the configurations.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-21 nsano initial version <br>
 */
public final class Fb01 extends Machine {

    private static final String[] BUTTONS = {
            "System Set Up", "Inst Select", "Inst Assign", "Inst Function",
            "Voice Function", "Voice Select", "-1/No", "+1/Yes"
    };

    private final byte[] rom = new byte[0x8000];
    private final byte[] nvram = new byte[0x4000];

    private Z80Cpu maincpu;
    private YM2151 ym2164;
    private I8251 upd71051;
    private HD44780 hd44780;
    private MidiPort mdin;

    /** active low */
    private volatile int panel = 0xff;

    private int ym2164Irq;
    private int upd71051Txrdy;
    private int upd71051Rxrdy;

    private final Panel front = new Panel() {
        @Override
        public Display getDisplay() {
            return hd44780;
        }

        @Override
        public List<Control> getControls() {
            return controls;
        }
    };

    private final List<Panel.Control> controls;

    {
        List<Panel.Control> list = new ArrayList<>();
        for (int i = 0; i < BUTTONS.length; i++) {
            int bit = 1 << i;
            list.add(new Panel.SimpleControl(BUTTONS[i], Panel.Control.Type.BUTTON, "", v -> {
                if (v > 0.5f) panel &= ~bit; else panel |= bit;
            }));
        }
        controls = Collections.unmodifiableList(list);
    }

    @Override
    public String getName() {
        return "fb01";
    }

    @Override
    public String getDescription() {
        return "Yamaha FB-01 FM Sound Generator";
    }

    @Override
    public void start() throws IOException {
        RomSet roms = new RomSet("fb01");
        byte[] data = roms.load("nec__-011_xb712c0__8709ex700__d27c256c-15.ic11", 0x8000, 0x7357e9a4);
        System.arraycopy(data, 0, rom, 0, rom.length);

        maincpu = addCpu(new Z80Cpu(12_000_000 / 2.0, new Z80Cpu.Bus() {
            @Override
            public int read(int address) {
                if (address < 0x8000) return rom[address] & 0xff;
                return nvram[address & 0x3fff] & 0xff;
            }

            @Override
            public void write(int address, int data) {
                if (address >= 0x8000) nvram[address & 0x3fff] = (byte) data;
            }

            @Override
            public int in(int port) {
                return switch (port & 0xff) {
                    case 0x01 -> ym2164.ym2151_status_port_0_r() & 0xff;
                    case 0x10, 0x11 -> upd71051.read(port);
                    case 0x20 -> panel;
                    case 0x30, 0x31 -> hd44780.read(port);
                    default -> 0xff;
                };
            }

            @Override
            public void out(int port, int data) {
                switch (port & 0xff) {
                    case 0x00 -> ym2164.ym2151_register_port_0_w(data);
                    case 0x01 -> ym2164.ym2151_data_port_0_w(data);
                    case 0x10, 0x11 -> upd71051.write(port, data);
                    case 0x30, 0x31 -> hd44780.write(port, data);
                    default -> {}
                }
            }
        }));

        // TODO: clock not measured, datasheet typical clock used
        hd44780 = new HD44780(this, 270_000, 2, 8, true);
        try {
            hd44780.setCgrom(new RomSet("hd44780").load("hd44780_a00.bin", 0x1000, 0xe459877c));
        } catch (IOException e) {
            // the display falls back to text
        }

        upd71051 = new I8251(this, 4_000_000 / 8.0);
        upd71051.setRxrdyHandler(state -> { upd71051Rxrdy = state; updateInt(); });
        upd71051.setTxrdyHandler(state -> { upd71051Txrdy = state; updateInt(); });
        upd71051.setTxdHandler(b -> midiOut.write(b));
        mdin = new MidiPort(this, upd71051::receiveCharacter);

        ym2164 = new YM2151();
        ym2164.ym2151_init(this, 4_000_000);
        ym2164.setIrqHandler(state -> { ym2164Irq = state; updateInt(); });
        ym2164.stream.route(0, 0, 1.0f).route(1, 1, 1.0f);

        reset();
    }

    @Override
    public void reset() {
        super.reset();
        hd44780.reset();
        upd71051.reset();
        ym2164.ym2151_reset_chip();
        mdin.clear();
        // machine_reset
        upd71051.writeCts(false);
    }

    private void updateInt() {
        maincpu.setIrq(ym2164Irq != 0 || upd71051Txrdy != 0 || upd71051Rxrdy != 0);
    }

    @Override
    public Panel getPanel() {
        return front;
    }

    @Override
    public void midiIn(byte[] message) {
        mdin.write(message);
    }

    @Override
    public byte[] getNvram() {
        return nvram.clone();
    }

    @Override
    public void setNvram(byte[] data) {
        System.arraycopy(data, 0, nvram, 0, Math.min(data.length, nvram.length));
    }
}
