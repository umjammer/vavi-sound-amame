/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package amame.frontend;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

import amame.drivers.Esq5505;
import amame.drivers.Fb01;
import amame.emu.Machine;
import amame.emu.RomSet;

import static java.lang.System.getLogger;


/**
 * The bridge between a host, a plug-in or the standalone application, and the emulated machine,
 * after amame's frontend and machine manager.
 * <p>
 * It holds the machine that is playing, swaps it for another one on request, the driver hot
 * swapping of amame, and remembers the battery backed memory of each one. It is the virtual
 * audio cable and the virtual midi cable of amame's osd too: {@link #render} pulls the audio,
 * {@link #midiIn} puts midi in, and the system exclusive messages the machine sends are
 * collected, to be saved as a {@code .syx} file, as amame's frontend does.
 * <p>
 * {@link #render} is called from the audio thread, everything else may be called from any.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-21 nsano initial version <br>
 */
public final class Frontend {

    private static final Logger logger = getLogger(Frontend.class.getName());

    /** the machines, by their mame names, amame's drivlist */
    public static final Map<String, Supplier<Machine>> MACHINES = Map.of(
            "fb01", Fb01::new,
            "vfx", () -> new Esq5505(Esq5505.Model.VFX),
            "vfxsd", () -> new Esq5505(Esq5505.Model.VFXSD));

    /** the machines in the order to offer them */
    public static final List<String> NAMES = List.of("fb01", "vfx", "vfxsd");

    /** the machine playing, swapped on the audio thread */
    private volatile Machine machine;

    /** the battery backed memories, by machine name */
    private final Map<String, byte[]> nvrams = new java.util.concurrent.ConcurrentHashMap<>();

    /** where the memories are kept between runs, null to keep them only in memory */
    private Path nvramDirectory;

    private final List<Consumer<Machine>> machineListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<Integer>> sysexListeners = new CopyOnWriteArrayList<>();
    private Consumer<byte[]> midiOutListener = message -> {};

    // sysex capture
    private boolean capturingSysex;
    private final ByteArrayOutputStream sysex = new ByteArrayOutputStream();

    /** the midi out of the machine, put back together into messages */
    private final ByteArrayOutputStream midiMessage = new ByteArrayOutputStream();
    private int midiExpected;

    /** keeps the battery backed memories in files in the directory, as mame's nvram directory */
    public void setNvramDirectory(Path nvramDirectory) {
        this.nvramDirectory = nvramDirectory;
    }

    /** called on a machine change, with the new machine */
    public void addMachineListener(Consumer<Machine> listener) {
        machineListeners.add(listener);
    }

    /** called with the length of the captured system exclusive data when it grows */
    public void addSysexListener(Consumer<Integer> listener) {
        sysexListeners.add(listener);
    }

    /** whole midi messages the machine sends */
    public void setMidiOutListener(Consumer<byte[]> listener) {
        this.midiOutListener = listener;
    }

    public Machine getMachine() {
        return machine;
    }

    /** whether the roms of a machine can be found */
    public static boolean isAvailable(String name) {
        Machine m = MACHINES.get(name).get();
        return new RomSet(m.getName()).exists();
    }

    /**
     * builds and starts a machine and swaps it in, the one playing until then is stopped and
     * its memory kept. this takes a while, the roms are loaded here, so it is better not called
     * on the audio thread.
     */
    public synchronized void setMachine(String name) throws IOException {
        Supplier<Machine> factory = MACHINES.get(name);
        if (factory == null) throw new IllegalArgumentException("no such machine: " + name);
        Machine old = machine;
        if (old != null) saveNvram(old);

        Machine m = factory.get();
        m.setMidiOut(this::onMidi);
        m.start();
        byte[] nvram = loadNvram(name);
        if (nvram != null) {
            m.setNvram(nvram);
            m.reset();
        }
        resetMidi();
        machine = m;
        for (Consumer<Machine> listener : machineListeners) listener.accept(m);
    }

    /** keeps the memory of the machine playing, at the end */
    public synchronized void close() {
        Machine m = machine;
        if (m != null) saveNvram(m);
    }

    /** the memory of every machine that played, for a plug-in's state */
    public Map<String, byte[]> getNvrams() {
        Machine m = machine;
        if (m != null && m.getNvram() != null) nvrams.put(m.getName(), m.getNvram());
        return Map.copyOf(nvrams);
    }

    /** restores the memories, the machine playing takes its own one after a reset */
    public void setNvrams(Map<String, byte[]> data) {
        nvrams.putAll(data);
        Machine m = machine;
        if (m != null && data.containsKey(m.getName())) {
            byte[] nvram = data.get(m.getName());
            m.post(() -> {
                m.setNvram(nvram);
                m.reset();
            });
        }
    }

    private void saveNvram(Machine m) {
        byte[] nvram = m.getNvram();
        if (nvram == null) return;
        nvrams.put(m.getName(), nvram);
        if (nvramDirectory == null) return;
        try {
            Files.createDirectories(nvramDirectory);
            Files.write(nvramDirectory.resolve(m.getName() + ".nv"), nvram);
        } catch (IOException e) {
            logger.log(Level.WARNING, "nvram of " + m.getName() + " not saved", e);
        }
    }

    private byte[] loadNvram(String name) {
        byte[] nvram = nvrams.get(name);
        if (nvram != null || nvramDirectory == null) return nvram;
        Path path = nvramDirectory.resolve(name + ".nv");
        try {
            return Files.isRegularFile(path) ? Files.readAllBytes(path) : null;
        } catch (IOException e) {
            logger.log(Level.WARNING, "nvram of " + name + " not loaded", e);
            return null;
        }
    }

    /**
     * renders a block, silence while there is no machine
     *
     * @param events the midi of the block, may be null
     */
    public void render(float[][] outputs, int frames, double sampleRate, List<Machine.TimedEvent> events) {
        Machine m = machine;
        if (m == null) {
            for (float[] out : outputs) java.util.Arrays.fill(out, 0, frames, 0f);
            return;
        }
        m.render(outputs, frames, sampleRate, events);
    }

    /** the midi of a block, at its frames, made into what {@link #render} takes */
    public List<Machine.TimedEvent> timed(List<byte[]> messages, List<Integer> frames) {
        Machine m = machine;
        List<Machine.TimedEvent> events = new ArrayList<>(messages.size());
        if (m == null) return events;
        for (int i = 0; i < messages.size(); i++) {
            byte[] message = messages.get(i);
            events.add(new Machine.TimedEvent(frames.get(i), () -> m.midiIn(message)));
        }
        return events;
    }

    /** midi from anywhere, it goes in at the start of the next block */
    public void midiIn(byte[] message) {
        Machine m = machine;
        if (m == null) return;
        byte[] copy = message.clone();
        m.post(() -> m.midiIn(copy));
    }

    /** a key of the virtual keyboard: the machine's own keyboard when it has one, midi otherwise */
    public void key(int note, int velocity, int channel) {
        Machine m = machine;
        if (m == null) return;
        m.post(() -> {
            if (!m.getPanel().key(note, velocity)) {
                m.midiIn(velocity > 0
                        ? new byte[] {(byte) (0x90 | channel), (byte) note, (byte) velocity}
                        : new byte[] {(byte) (0x80 | channel), (byte) note, 0});
            }
        });
    }

    // midi out

    /** a byte from the uart of the machine, on the emulation thread */
    private void onMidi(int data) {
        captureSysex(data);
        assembleMidi(data);
    }

    private void resetMidi() {
        midiMessage.reset();
        midiExpected = 0;
        capturingSysex = false;
    }

    /** amame's frontend keeps every sysex the machine sends, to be saved */
    private void captureSysex(int data) {
        if (!capturingSysex) {
            if (data != 0xf0) return;
            capturingSysex = true;
        }
        int length;
        synchronized (sysex) {
            sysex.write(data);
            length = sysex.size();
        }
        if (data == 0xf7) capturingSysex = false;
        for (Consumer<Integer> listener : sysexListeners) listener.accept(length);
    }

    private void assembleMidi(int data) {
        if (data >= 0xf8) {
            // real time, in between anything
            midiOutListener.accept(new byte[] {(byte) data});
            return;
        }
        if ((data & 0x80) != 0) {
            midiMessage.reset();
            midiMessage.write(data);
            midiExpected = switch (data & 0xf0) {
                case 0xc0, 0xd0 -> 2;
                case 0xf0 -> switch (data) {
                    case 0xf0 -> -1; // until f7
                    case 0xf1, 0xf3 -> 2;
                    case 0xf2 -> 3;
                    default -> 1;
                };
                default -> 3;
            };
            if (data == 0xf7 || midiExpected == 1) {
                midiOutListener.accept(midiMessage.toByteArray());
                midiMessage.reset();
                midiExpected = 0;
            }
            return;
        }
        if (midiMessage.size() == 0) return; // running status out is not followed
        midiMessage.write(data);
        if (midiExpected > 0 && midiMessage.size() == midiExpected) {
            midiOutListener.accept(midiMessage.toByteArray());
            midiMessage.reset();
            midiExpected = 0;
        }
    }

    // sysex import and export

    /** what the machine sent since the last clear */
    public byte[] getSysex() {
        synchronized (sysex) {
            return sysex.toByteArray();
        }
    }

    public void clearSysex() {
        synchronized (sysex) {
            sysex.reset();
        }
        for (Consumer<Integer> listener : sysexListeners) listener.accept(0);
    }

    public void saveSysex(Path path) throws IOException {
        Files.write(path, getSysex());
    }

    /** sends a {@code .syx} file to the machine, one message at a time */
    public void loadSysex(Path path) throws IOException {
        for (byte[] message : splitSysex(Files.readAllBytes(path))) {
            midiIn(message);
        }
    }

    /** the f0 ... f7 messages of a dump */
    public static List<byte[]> splitSysex(byte[] data) {
        List<byte[]> messages = new ArrayList<>();
        int start = -1;
        for (int i = 0; i < data.length; i++) {
            int b = data[i] & 0xff;
            if (b == 0xf0) start = i;
            else if (b == 0xf7 && start >= 0) {
                byte[] message = new byte[i - start + 1];
                System.arraycopy(data, start, message, 0, message.length);
                messages.add(message);
                start = -1;
            }
        }
        return messages;
    }
}
