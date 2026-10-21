/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package amame.drivers;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import amame.cpu.M68000Cpu;
import amame.devices.EsqPanel;
import amame.devices.FloppyDisk;
import amame.devices.MC68681;
import amame.devices.MidiPort;
import amame.devices.VfxCartridge;
import amame.devices.WD1772;
import amame.emu.EmuTimer;
import amame.emu.Machine;
import amame.emu.MediaSlot;
import amame.emu.Panel;
import amame.emu.RomSet;
import amame.emu.SoundStream;
import amame.sound.ES5505;
import amame.sound.ES5510;


/**
 * Ensoniq ES5505 + ES5510 based synthesizers, the VFX and the VFX-SD, after mame's
 * ensoniq/esq5505.cpp (R. Belmont, Parduz, Christian Brunschen, Phil Bennett) and the
 * button and slider hooks amame added to it.
 * <p>
 * A 68000, the OTIS sample player, the ESP effects processor fed by the pump, a 68681 duart
 * with the midi on channel A and the front panel on channel B, the cartridge slot and the
 * VFX-SD's floppy drive.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-21 nsano initial version <br>
 */
public final class Esq5505 extends Machine {

    /** the models this plays */
    public enum Model {
        VFX("vfx", "Ensoniq VFX", 0x20000, false,
                new String[] {"vfx210b-high.bin", "vfx210b-low.bin"}, new int[] {0x59853be8, 0xc51b19cd},
                new String[] {"u14.bin", "u15.bin", "u16.bin"}, new int[] {0x85592299, 0xc0055975, 0xc3ddaf95}),
        VFXSD("vfxsd", "Ensoniq VFX-SD", 0x40000, true,
                new String[] {"vfxsd_200_upper.bin", "vfxsd_200_lower.bin"}, new int[] {0x9a40efa2, 0x7bd31aea},
                new String[] {"u57.bin", "u58.bin", "u60.bin"}, new int[] {0x85592299, 0xc0055975, 0xc3ddaf95});

        final String name;
        final String description;
        final int romSize;
        final boolean sd;
        final String[] osroms;
        final int[] osromCrcs;
        final String[] waveroms;
        final int[] waveromCrcs;

        Model(String name, String description, int romSize, boolean sd, String[] osroms, int[] osromCrcs, String[] waveroms, int[] waveromCrcs) {
            this.name = name;
            this.description = description;
            this.romSize = romSize;
            this.sd = sd;
            this.osroms = osroms;
            this.osromCrcs = osromCrcs;
            this.waveroms = waveroms;
            this.waveromCrcs = waveromCrcs;
        }

        public static Model of(String name) {
            for (Model m : values()) if (m.name.equals(name)) return m;
            throw new IllegalArgumentException(name);
        }
    }

    /** the panel buttons, code and name, from the comment in mame's driver */
    private static final Object[][] BUTTONS = {
            {54, "Presets", "bank"}, {52, "Cart", "bank"}, {53, "Sounds", "bank"}, {51, "Seq", "bank"},
            {55, "0", "number"}, {56, "1", "number"}, {57, "2", "number"}, {46, "3", "number"}, {47, "4", "number"},
            {48, "5", "number"}, {49, "6", "number"}, {35, "7", "number"}, {34, "8", "number"}, {25, "9", "number"},
            {30, "Tracks 1-6", "number"}, {31, "Tracks 7-12", "number"},
            {58, "Soft Top Left", "soft"}, {42, "Soft Top Center", "soft"}, {43, "Soft Top Right", "soft"},
            {50, "Soft Bottom Left", "soft"}, {44, "Soft Bottom Center", "soft"}, {45, "Soft Bottom Right", "soft"},
            {8, "Compare", "edit"}, {3, "Write", "edit"}, {63, "Data -", "edit"}, {62, "Data +", "edit"},
            {2, "Program Control", "program"}, {7, "Effect", "program"}, {17, "Output", "program"},
            {4, "Wave", "program"}, {6, "Mixer/Shaper", "program"}, {9, "Copy Effects", "program"},
            {5, "Select Voice", "program"}, {10, "LFO", "program"}, {11, "Pitch", "program"}, {13, "Pitch Mod", "program"},
            {15, "Filter", "program"}, {12, "Env1", "program"}, {14, "Env2", "program"}, {16, "Env3", "program"},
            {36, "Volume", "performance"}, {37, "Pan", "performance"}, {38, "Timbre", "performance"},
            {39, "Key Zone", "performance"}, {40, "Transpose", "performance"}, {41, "Release", "performance"},
            {20, "Master", "system"}, {21, "Storage", "system"}, {24, "MIDI", "system"},
            {22, "Stop/Cont", "sequencer"}, {23, "Play", "sequencer"}, {19, "Record", "sequencer"},
    };

    /** the buttons with lights and the lights, from mame's layouts, 16 is the floppy's */
    private static final Map<Integer, Integer> LIGHTS = Map.ofEntries(
            Map.entry(52, 15), Map.entry(53, 13), Map.entry(54, 7),
            Map.entry(55, 14), Map.entry(56, 6), Map.entry(57, 4), Map.entry(46, 12), Map.entry(47, 3),
            Map.entry(48, 11), Map.entry(49, 2), Map.entry(35, 10), Map.entry(34, 1), Map.entry(25, 9),
            Map.entry(8, 5), Map.entry(30, 0), Map.entry(31, 8));

    private final Model model;

    private byte[] osrom;
    private final byte[] osram = new byte[0x10000];
    private byte[] seqram;
    private final byte[] waverom = new byte[0x200000];

    private M68000Cpu maincpu;
    private MC68681 duart;
    private ES5505 otis;
    private ES5510 esp;
    private EsqPanel panel;
    private MidiPort mdin;
    private SoundStream pump;
    private WD1772 fdc;
    private VfxCartridge cart;
    private Path cartPath;
    private Path diskPath;

    private int duartIo;
    private boolean otisIrq;
    private boolean duartIrq;
    private boolean espHalted = true;
    /** the VFX-SD's drive, selecting it with a new disk or none raises a disk change on irq 1 */
    private boolean floppyIsActive;
    private boolean floppyIsLoaded;
    private boolean floppyDskchg = true;
    private boolean docirq;
    private EmuTimer motorOnTimer;
    /** what the motor on timer is to do */
    private boolean floppyMotorOn;
    private EmuTimer dskchgResetTimer;
    private final int[] analogValues = new int[8];

    private final List<Panel.Control> controls;
    private final List<Panel.Light> lights;
    private final List<MediaSlot> mediaSlots;
    private final Panel front;

    public Esq5505(String name) {
        this(Model.of(name));
    }

    public Esq5505(Model model) {
        this.model = model;
        List<Panel.Control> list = new ArrayList<>();
        for (Object[] button : BUTTONS) {
            int code = (Integer) button[0];
            if (!model.sd && "sequencer".equals(button[2])) continue;
            Integer light = LIGHTS.get(code);
            list.add(new Panel.SimpleControl((String) button[1], Panel.Control.Type.BUTTON, (String) button[2],
                    v -> panel.setButton(code, v > 0.5f), light == null ? null : () -> panel != null && panel.isLightOn(light)));
        }
        // the analog values are 10 bits, left-justified within 16 bits
        list.add(new Panel.SimpleControl("Data Entry", Panel.Control.Type.SLIDER, "slider",
                v -> panel.setAnalogValue(3, Math.round(v * 0x3ff) << 6)));
        list.add(new Panel.SimpleControl("Volume", Panel.Control.Type.SLIDER, "slider",
                v -> panel.setAnalogValue(5, Math.round(v * 0x3ff) << 6)));
        controls = Collections.unmodifiableList(list);
        lights = model.sd ? List.of(new Panel.SimpleLight("Disk", () -> panel != null && panel.isFloppyActive())) : List.of();
        List<MediaSlot> slots = new ArrayList<>();
        slots.add(new CartridgeSlot());
        if (model.sd) slots.add(new FloppySlot());
        mediaSlots = Collections.unmodifiableList(slots);
        front = new Panel() {
            @Override
            public Display getDisplay() {
                return panel;
            }

            @Override
            public List<Control> getControls() {
                return controls;
            }

            @Override
            public List<Light> getLights() {
                return lights;
            }

            @Override
            public boolean key(int note, int velocity) {
                // 61 keys from C2, PORT_GM_NOTE(36 + i)
                int key = note - 36;
                if (key < 0 || key > 60) return false;
                if (velocity > 0) panel.keyDown(key, velocity); else panel.keyUp(key);
                return true;
            }
        };
    }

    @Override
    public String getName() {
        return model.name;
    }

    @Override
    public String getDescription() {
        return model.description;
    }

    @Override
    public void start() throws IOException {
        RomSet roms = new RomSet(model.name);
        osrom = new byte[model.romSize];
        // ROM_LOAD16_BYTE, the high byte of each word from the first, the low byte from the second
        RomSet.interleave(osrom, 0, 2, roms.load(model.osroms[0], 0x10000, model.osromCrcs[0]));
        RomSet.interleave(osrom, 1, 2, roms.load(model.osroms[1], 0x10000, model.osromCrcs[1]));
        // the waverom is read as little endian words: the rom bytes are the high bytes, at the odd addresses
        RomSet.interleave(waverom, 1, 2, roms.load(model.waveroms[0], 0x80000, model.waveromCrcs[0]));
        RomSet.interleave(waverom, 0x100001, 2, roms.load(model.waveroms[1], 0x80000, model.waveromCrcs[1]));
        // init_denib: the 12 bit samples get their low nibbles from the nibble rom
        byte[] nibbles = roms.load(model.waveroms[2], 0x80000, model.waveromCrcs[2]);
        for (int i = 0; i < 0x80000; i++) {
            waverom[i * 2] = (byte) ((nibbles[i] & 0x0f) << 4);
            waverom[0x100000 + i * 2] = (byte) (nibbles[i] & 0xf0);
        }
        if (model.sd) seqram = new byte[0x50000];

        maincpu = addCpu(new M68000Cpu(10_000_000, new M68000Cpu.Bus() {
            @Override
            public int read8(int address) {
                int word = read16(address & ~1);
                return (address & 1) == 0 ? word >> 8 : word & 0xff;
            }

            @Override
            public int read16(int address) {
                return read(address, false, false);
            }

            @Override
            public int readProgram16(int address, boolean supervisor) {
                return read(address, true, supervisor);
            }

            @Override
            public void write8(int address, int data) {
                write(address, (address & 1) == 0 ? data << 8 : data, (address & 1) == 0 ? 0xff00 : 0x00ff);
            }

            @Override
            public void write16(int address, int data) {
                write(address, data, 0xffff);
            }
        }));
        maincpu.setIrqAcknowledge(level -> level == 3 ? duart.getIrqVector() : 24 + level);

        duart = new MC68681(this, 4_000_000);
        duart.setClocks(500000, 500000, 1000000, 1000000);
        duart.setIrqCallback(state -> { duartIrq = state != 0; updateIrq(); });
        duart.setTx(0, b -> midiOut.write(b));
        duart.setTx(1, b -> panel.rx(b));
        duart.setOutport(this::duartOutput);
        mdin = new MidiPort(this, b -> duart.rx(0, b));

        panel = new EsqPanel(this);
        panel.setWriteTx(b -> duart.rx(1, b));
        panel.setWriteAnalog((offset, value) -> analogValues[offset & 7] = value);
        try {
            // the vfd is drawn as text, the font only tells it is the real thing
            new RomSet("esq2x40_vfx").load("esqvfd_font_vfx.bin", 192, 0x58dc335b);
        } catch (IOException e) {
            // not needed
        }

        esp = new ES5510();
        if (model.sd) {
            fdc = new WD1772(this);
            motorOnTimer = timer(this::floppyMotorOn);
            dskchgResetTimer = timer(this::floppyDskchgReset);
            // machine_start: the disk change according to whether there is a floppy in the drive
            floppyDskchg = fdc.getDisk() == null;
        }

        otis = new ES5505(10_000_000, 4);
        otis.setRegion(0, waverom);
        if (model.sd) otis.setRegion(1, new byte[0x200000]);
        otis.setIrqCallback(state -> {
            if (otisIrq != (state != 0)) {
                otisIrq = state != 0;
                if (fdc != null) updateDocirq(); else { docirq = otisIrq; updateIrq(); }
            }
        });
        otis.setReadPort(() -> analogValues[duartIo & 7]);
        otis.setUpdate(this::updateOtis);
        // the pump runs at the rate of the otis, the clock of mame's pump follows it
        otisRate = 10_000_000 / (16.0 * 21);
        otisPosition = (long) Math.floor(time() * otisRate);
        pump = stream(4, otisRate, this::pumpUpdate);
        pump.route(0, 0, 1.0f).route(1, 1, 1.0f);
        otis.setSampleRateChanged(rate -> {
            updateOtis();
            otisRate = rate;
            otisPosition = (long) Math.floor(time() * rate);
            pump.setRate(rate);
        });

        reset();
    }

    private final float[][] otisOut = new float[8][1];

    /**
     * the samples of the otis not yet through the pump. the otis renders up to the present
     * whenever the cpu touches it, its stream in mame does, but the pump and so the esp only
     * run when the output is rendered: in between the esp's registers hold what the cpu
     * wrote, which the VFX-SD reads back to check the esp.
     */
    private float[][] otisFifo = new float[8][4096];
    private int otisFifoRead;
    private int otisFifoLength;
    private double otisRate;
    private long otisPosition;

    private void updateOtis() {
        long target = (long) Math.floor(time() * otisRate);
        int samples = (int) (target - otisPosition);
        if (samples <= 0) return;
        if (otisFifoRead > 0) {
            for (int c = 0; c < 8; c++) System.arraycopy(otisFifo[c], otisFifoRead, otisFifo[c], 0, otisFifoLength - otisFifoRead);
            otisFifoLength -= otisFifoRead;
            otisFifoRead = 0;
        }
        if (otisFifoLength + samples > otisFifo[0].length) {
            int size = Math.max(otisFifoLength + samples, otisFifo[0].length * 2);
            for (int c = 0; c < 8; c++) otisFifo[c] = java.util.Arrays.copyOf(otisFifo[c], size);
        }
        otis.generate(otisFifo, otisFifoLength, samples);
        otisFifoLength += samples;
        otisPosition = target;
    }

    /** the next sample of the otis, from the fifo or rendered now */
    private void nextOtisSample() {
        if (otisFifoRead == otisFifoLength) {
            otisFifoRead = otisFifoLength = 0;
            otis.generate(otisOut, 0, 1);
            otisPosition++;
            return;
        }
        for (int c = 0; c < 8; c++) {
            otisOut[c][0] = otisFifo[c][otisFifoRead];
        }
        otisFifoRead++;
    }

    /** esq_5505_5510_pump_device::sound_stream_update */
    private void pumpUpdate(float[][] outputs, int offset, int samples) {
        for (int s = 0; s < samples; s++) {
            nextOtisSample();
            // Push the 'Aux' output samples directly into the output stream
            outputs[2][offset + s] = otisOut[0][0];
            outputs[3][offset + s] = otisOut[1][0];
            // Push the 'FX1', 'FX2' and 'DRY' samples into the ESP
            for (int i = 0; i < 6; i++) {
                esp.serW(i, (short) (int) (otisOut[2 + i][0] * 32768.0f));
            }
            if (!espHalted) {
                esp.runOnce();
            }
            // Read the processed result from the ESP.
            outputs[0][offset + s] = esp.serR(6) / 32768.0f;
            outputs[1][offset + s] = esp.serR(7) / 32768.0f;
        }
    }

    @Override
    public void reset() {
        super.reset();
        // the duart drives its output pins at reset, which select the analog value and the drive
        floppyIsActive = false;
        duartIo = 0;
        duart.reset();
        panel.reset();
        esp.reset();
        if (fdc != null) {
            fdc.reset();
            // machine_reset: check our image devices for load status
            floppyLoaded(fdc.getDisk() != null);
        }
        mdin.clear();
        cartridgeLoaded(cart != null);
        analogValues[0] = 0x7fc0; // pitch mod: start in the center
        analogValues[1] = 0x0000; // patch select: nothing pressed.
        analogValues[2] = 0xffc0; // mod wheel: at the bottom, no modulation
        analogValues[3] = 0xccc0; // data entry: somewhere in the middle
        analogValues[4] = 0xffc0; // control voltage / pedal: full on.
        analogValues[5] = 0xffc0; // Volume control: full on.
        analogValues[6] = 0x7fc0; // Battery voltage: something reasonable.
        analogValues[7] = 0x5540; // vRef to check battery.
    }

    private void updateIrq() {
        maincpu.setIrqLevel(duartIrq ? 3 : docirq ? 1 : 0);
    }

    /** update_docirq_to_maincpu */
    private void updateDocirq() {
        boolean floppyDskchgIrq = floppyIsActive && floppyDskchg;
        if (floppyDskchgIrq && floppyIsLoaded) {
            // The drives that Ensoniq use only _pulse_ DSKCHG for a brief time, when a disk is in the drive.
            dskchgResetTimer.adjust(500e-9);
        }
        boolean updatedIrq = otisIrq || floppyDskchgIrq;
        if (updatedIrq != docirq) {
            docirq = updatedIrq;
            updateIrq();
        }
    }

    private void cartridgeLoaded(boolean loaded) {
        // On VFX and later, DUART input bit 1 is 0 for cartridge present.
        duart.ip(1, !loaded);
    }

    /** floppy_motor_on, the motor follows the drive select a little later */
    private void floppyMotorOn() {
        fdc.setDriveMotor(floppyMotorOn);
        panel.setFloppyActive(floppyMotorOn);
    }

    private void floppyDskchgReset() {
        floppyDskchg = !floppyIsLoaded;
        updateDocirq();
    }

    private void updateFloppyInputs() {
        // update the "Disk Ready" input
        duart.ip(0, floppyIsActive && floppyIsLoaded);
        // Also update the DOC IRQ in case there's a pending disk change to handle.
        updateDocirq();
    }

    private void floppyLoaded(boolean loaded) {
        floppyIsLoaded = loaded;
        if (!loaded) {
            // Only set floppyDskchg; it will be reset a short time after
            // the disk has been enabled while floppyDskchg is true.
            floppyDskchg = true;
        }
        updateFloppyInputs();
    }

    private void duartOutput(int data) {
        duartIo = data;
        // bits 0/1/2 = analog sel, bit 4 = DSEL (VFX-SD), bit 6 = ESPHALT
        espHalted = (data & 0x40) != 0;
        if (fdc != null) {
            fdc.setSide((data & 0x08) != 0 ? 0 : 1); // bit 3, inverted -> floppy
            floppyIsActive = (data & 0x10) != 0; // bit 4 is used to activate the floppy
            // a delayed motor on, since the keyboard constantly pulses this after a file has been read,
            // and a slightly delayed motor off, since the keyboard sometimes seems to pulse this.
            floppyMotorOn = floppyIsActive;
            motorOnTimer.adjust(floppyIsActive ? 100e-6 : 50e-6);
            updateFloppyInputs();
        }
    }

    private int read(int address, boolean program, boolean supervisor) {
        address &= 0xff_fffe;
        int lowerSize = model.sd ? 0x10000 : 0x8000;
        if (address < lowerSize) {
            // supervisor program space reads the rom
            if (program && supervisor) return word(osrom, address & 0xffff);
            return word(osram, address & 0xffff);
        }
        if (address >= 0x200000 && address < 0x200020) return otis.read((address - 0x200000) >> 1);
        if (address >= 0x260000 && address < 0x260200) return 0xff00 | esp.hostR((address - 0x260000) >> 1);
        if (address >= 0x280000 && address < 0x280020) return 0xff00 | duart.read((address - 0x280000) >> 1);
        if (fdc != null && address >= 0x2c0000 && address < 0x2c0008) return 0xff00 | fdc.read((address - 0x2c0000) >> 1);
        if (address >= 0x2e0000 && address < 0x300000) return 0xff00 | (cart != null ? cart.read((address - 0x2e0000) >> 1) : 0);
        if (seqram != null && address >= 0x330000 && address < 0x380000) return word(seqram, address - 0x330000);
        if (address >= 0xc00000 && address < 0xc00000 + model.romSize) return word(osrom, address - 0xc00000);
        if (address >= 0xff0000) return word(osram, address & 0xffff);
        return 0;
    }

    private void write(int address, int data, int mask) {
        address &= 0xff_fffe;
        int lowerSize = model.sd ? 0x10000 : 0x8000;
        if (address < lowerSize) {
            combine(osram, address & 0xffff, data, mask);
        } else if (address >= 0x200000 && address < 0x200020) {
            otis.write((address - 0x200000) >> 1, data, mask);
        } else if (address >= 0x260000 && address < 0x260200) {
            if ((mask & 0xff) != 0) esp.hostW((address - 0x260000) >> 1, data & 0xff);
        } else if (address >= 0x280000 && address < 0x280020) {
            if ((mask & 0xff) != 0) duart.write((address - 0x280000) >> 1, data & 0xff);
        } else if (fdc != null && address >= 0x2c0000 && address < 0x2c0008) {
            if ((mask & 0xff) != 0) fdc.write((address - 0x2c0000) >> 1, data & 0xff);
        } else if (address >= 0x2e0000 && address < 0x300000) {
            if ((mask & 0xff) != 0 && cart != null) cart.write((address - 0x2e0000) >> 1, data & 0xff);
        } else if (seqram != null && address >= 0x330000 && address < 0x380000) {
            combine(seqram, address - 0x330000, data, mask);
        } else if (address >= 0xff0000) {
            combine(osram, address & 0xffff, data, mask);
        }
    }

    private static int word(byte[] mem, int address) {
        return ((mem[address] & 0xff) << 8) | (mem[address + 1] & 0xff);
    }

    private static void combine(byte[] mem, int address, int data, int mask) {
        if ((mask & 0xff00) != 0) mem[address] = (byte) (data >> 8);
        if ((mask & 0x00ff) != 0) mem[address + 1] = (byte) data;
    }

    @Override
    public Panel getPanel() {
        return front;
    }

    @Override
    public List<MediaSlot> getMediaSlots() {
        return mediaSlots;
    }

    /** the expansion cartridge */
    private final class CartridgeSlot implements MediaSlot {

        @Override
        public String getName() {
            return "Cartridge";
        }

        @Override
        public List<String> getExtensions() {
            return List.of("eeprom", "sc32", "rom", "cart");
        }

        @Override
        public Path getPath() {
            return cart != null ? cartPath : null;
        }

        @Override
        public void load(Path path) throws IOException {
            VfxCartridge c = VfxCartridge.load(path);
            unload();
            insert(c, path);
        }

        @Override
        public void create(Path path) throws IOException {
            VfxCartridge c = VfxCartridge.blank();
            c.save(path);
            unload();
            insert(c, path);
        }

        private void insert(VfxCartridge c, Path path) {
            cart = c;
            cartPath = path;
            if (duart != null) cartridgeLoaded(true);
        }

        @Override
        public void unload() throws IOException {
            if (cart == null) return;
            VfxCartridge c = cart;
            cart = null;
            if (duart != null) cartridgeLoaded(false);
            if (c.isWriteable()) c.save(cartPath);
            cartPath = null;
        }
    }

    /** the VFX-SD's floppy drive */
    private final class FloppySlot implements MediaSlot {

        @Override
        public String getName() {
            return "Floppy";
        }

        @Override
        public List<String> getExtensions() {
            return List.of("img");
        }

        @Override
        public Path getPath() {
            return fdc != null && fdc.getDisk() != null ? diskPath : null;
        }

        @Override
        public void load(Path path) throws IOException {
            insert(FloppyDisk.load(path), path);
        }

        @Override
        public void create(Path path) throws IOException {
            FloppyDisk disk = new FloppyDisk();
            disk.save(path);
            insert(disk, path);
        }

        private void insert(FloppyDisk disk, Path path) throws IOException {
            unload();
            fdc.setDisk(disk);
            diskPath = path;
            floppyLoaded(true);
        }

        @Override
        public void unload() throws IOException {
            FloppyDisk disk = fdc.getDisk();
            if (disk == null) return;
            fdc.setDisk(null);
            floppyLoaded(false);
            if (disk.isModified() && !disk.isWriteProtected()) disk.save(diskPath);
            diskPath = null;
        }
    }

    @Override
    public void midiIn(byte[] message) {
        mdin.write(message);
    }

    @Override
    public byte[] getNvram() {
        if (seqram == null) return osram.clone();
        byte[] data = new byte[osram.length + seqram.length];
        System.arraycopy(osram, 0, data, 0, osram.length);
        System.arraycopy(seqram, 0, data, osram.length, seqram.length);
        return data;
    }

    @Override
    public void setNvram(byte[] data) {
        System.arraycopy(data, 0, osram, 0, Math.min(data.length, osram.length));
        if (seqram != null && data.length > osram.length) {
            System.arraycopy(data, osram.length, seqram, 0, Math.min(data.length - osram.length, seqram.length));
        }
    }
}
