/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package amame;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import amame.frontend.Frontend;
import amame.gui.PanelView;

import static java.lang.System.getLogger;


/**
 * amame standalone: the synthesizer with its panel in a window, the audio on the default line
 * and the midi of every midi input of the system.
 * <p>
 * {@code java -Damame.rompath=... amame.Amame [fb01|vfx|vfxsd]}. The battery backed memories
 * are kept in {@code ~/.amame/nvram}.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-21 nsano initial version <br>
 */
public final class Amame {

    private static final Logger logger = getLogger(Amame.class.getName());

    private static final float SAMPLE_RATE = 48000;
    private static final int FRAMES = 256;

    private final Frontend frontend = new Frontend();
    private final List<MidiDevice> midiDevices = new ArrayList<>();
    private volatile boolean running = true;

    public static void main(String[] args) throws Exception {
        new Amame().run(args.length > 0 ? args[0] : "fb01");
    }

    private void run(String name) throws Exception {
        frontend.setNvramDirectory(Path.of(System.getProperty("user.home"), ".amame", "nvram"));
        frontend.setMachine(name);
        openMidiInputs();

        Thread audio = new Thread(this::audioLoop, "amame-audio");
        audio.setPriority(Thread.MAX_PRIORITY);
        audio.start();

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("amame");
            PanelView view = new PanelView(frontend);
            frame.setContentPane(view);
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent e) {
                    view.dispose();
                    shutdown();
                }
            });
            frame.pack();
            frame.setVisible(true);
        });
    }

    private void audioLoop() {
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 2, true, false);
        try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
            line.open(format, FRAMES * 4 * 8);
            line.start();
            float[][] out = new float[2][FRAMES];
            byte[] pcm = new byte[FRAMES * 4];
            while (running) {
                frontend.render(out, FRAMES, SAMPLE_RATE, null);
                for (int i = 0; i < FRAMES; i++) {
                    for (int ch = 0; ch < 2; ch++) {
                        int v = (int) Math.max(-32768, Math.min(32767, out[ch][i] * 32767));
                        pcm[i * 4 + ch * 2] = (byte) v;
                        pcm[i * 4 + ch * 2 + 1] = (byte) (v >> 8);
                    }
                }
                line.write(pcm, 0, pcm.length);
            }
            line.drain();
        } catch (LineUnavailableException e) {
            logger.log(Level.ERROR, "no audio line", e);
        }
    }

    /** every midi input of the system goes into the machine */
    private void openMidiInputs() {
        for (MidiDevice.Info info : MidiSystem.getMidiDeviceInfo()) {
            try {
                MidiDevice device = MidiSystem.getMidiDevice(info);
                if (device.getMaxTransmitters() == 0 || device instanceof javax.sound.midi.Sequencer
                        || device instanceof javax.sound.midi.Synthesizer) continue;
                device.open();
                device.getTransmitter().setReceiver(new Receiver() {
                    @Override
                    public void send(MidiMessage message, long timeStamp) {
                        if (message instanceof ShortMessage sm && sm.getCommand() == 0xf0 && sm.getChannel() >= 8) {
                            return; // real time messages, clock and active sensing are of no use
                        }
                        byte[] data = message instanceof SysexMessage ? message.getMessage() : trim(message);
                        frontend.midiIn(data);
                    }

                    @Override
                    public void close() {
                    }
                });
                midiDevices.add(device);
                logger.log(Level.INFO, "midi in: " + info.getName());
            } catch (MidiUnavailableException e) {
                logger.log(Level.DEBUG, "midi device " + info.getName() + " not opened: " + e);
            }
        }
    }

    private static byte[] trim(MidiMessage message) {
        byte[] data = message.getMessage();
        int length = Math.min(message.getLength(), data.length);
        byte[] result = new byte[length];
        System.arraycopy(data, 0, result, 0, length);
        return result;
    }

    private void shutdown() {
        running = false;
        for (MidiDevice device : midiDevices) device.close();
        frontend.close();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.exit(0);
    }
}
