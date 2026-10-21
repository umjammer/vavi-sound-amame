/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package amame.plugin;

import java.awt.Component;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import amame.emu.Machine;
import amame.frontend.Frontend;
import amame.gui.PanelView;
import com.sun.jna.Pointer;
import org.urish.jnavst.VstConst;
import org.urish.jnavst.VstEffect;

import static java.lang.System.getLogger;


/**
 * amame as a VST 2.4 instrument, after amame's plugin.cpp.
 * <p>
 * No inputs, a stereo output, midi in. The midi of a block goes into the machine at the frame
 * it is for, sysex too. The state the host saves with a project is the chunk: the machine playing
 * and the battery backed memory of every machine that played, which is where the synthesizers
 * keep their voices. The editor is the swing panel, in a window of its own: a java plug-in has
 * no way into a native window, a java host puts {@link #getEditorComponent()} where it likes.
 * <p>
 * The machine to start with is the system property {@code amame.machine}, fb01 when not set.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-21 nsano initial version <br>
 */
public class AmamePlugin extends VstEffect {

    private static final Logger logger = getLogger(AmamePlugin.class.getName());

    /** 'AMAM', the chunk starts with it */
    private static final int CHUNK_MAGIC = 0x414d414d;

    private final Frontend frontend = new Frontend();

    private PanelView view;
    private JFrame window;

    public AmamePlugin() {
        setNumInputs(0);
        setNumOutputs(2);
        setUniqueID("amam");
        isSynth(true);
        programsAreChunks(true);
        hasEditor(true);
        frontend.setMidiOutListener(message -> sendMidi(message, 0));
    }

    public Frontend getFrontend() {
        return frontend;
    }

    @Override
    protected String getEffectName() {
        return "AMAME";
    }

    @Override
    protected String getVendorString() {
        return "vavi";
    }

    @Override
    protected String getProductString() {
        return "AMAME arcoflex056";
    }

    @Override
    protected String getProgramName() {
        Machine m = frontend.getMachine();
        return m != null ? m.getName() : "";
    }

    @Override
    protected int canDo(String what) {
        return switch (what) {
            case "receiveVstEvents", "receiveVstMidiEvent", "sendVstMidiEvent", "sendVstEvents" -> VstConst.VST_CanDoYes;
            default -> VstConst.VST_CanDoNo;
        };
    }

    @Override
    protected void open() {
        if (frontend.getMachine() != null) return;
        try {
            frontend.setMachine(System.getProperty("amame.machine", "fb01"));
        } catch (IOException | RuntimeException e) {
            logger.log(Level.ERROR, "the machine did not start", e);
        }
    }

    @Override
    protected void close() {
        editClose();
        frontend.close();
    }

    // audio and midi io

    private final List<byte[]> blockMessages = new ArrayList<>();
    private final List<Integer> blockFrames = new ArrayList<>();

    @Override
    protected void processEvents(List<MidiEvent> events) {
        for (MidiEvent event : events) {
            blockMessages.add(event.data());
            blockFrames.add(event.deltaFrames());
        }
    }

    @Override
    protected void processReplacing(float[][] inputs, float[][] outputs, int sampleFrames) {
        List<Machine.TimedEvent> events = null;
        if (!blockMessages.isEmpty()) {
            events = frontend.timed(blockMessages, blockFrames);
            events.sort(java.util.Comparator.comparingInt(Machine.TimedEvent::frame));
            blockMessages.clear();
            blockFrames.clear();
        }
        frontend.render(outputs, sampleFrames, getSampleRate(), events);
    }

    // the state

    @Override
    protected byte[] getChunk(boolean program) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeInt(CHUNK_MAGIC);
            Machine m = frontend.getMachine();
            dos.writeUTF(m != null ? m.getName() : "");
            Map<String, byte[]> nvrams = frontend.getNvrams();
            dos.writeInt(nvrams.size());
            for (Map.Entry<String, byte[]> e : nvrams.entrySet()) {
                dos.writeUTF(e.getKey());
                dos.writeInt(e.getValue().length);
                dos.write(e.getValue());
            }
            dos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected void setChunk(byte[] data, boolean program) {
        try {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
            if (dis.readInt() != CHUNK_MAGIC) {
                logger.log(Level.WARNING, "not an amame chunk");
                return;
            }
            String name = dis.readUTF();
            int n = dis.readInt();
            Map<String, byte[]> nvrams = new HashMap<>();
            for (int i = 0; i < n; i++) {
                String key = dis.readUTF();
                byte[] value = new byte[dis.readInt()];
                dis.readFully(value);
                nvrams.put(key, value);
            }
            frontend.setNvrams(nvrams);
            Machine m = frontend.getMachine();
            if (!name.isEmpty() && (m == null || !m.getName().equals(name))) {
                frontend.setMachine(name);
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "the chunk is broken", e);
        }
    }

    // the editor

    private PanelView view() {
        if (view == null) view = new PanelView(frontend);
        return view;
    }

    @Override
    public Component getEditorComponent() {
        return view();
    }

    @Override
    protected int[] getEditorSize() {
        java.awt.Dimension d = view().getPreferredSize();
        return new int[] {d.width, d.height};
    }

    @Override
    protected boolean editOpen(Pointer window) {
        SwingUtilities.invokeLater(() -> {
            if (this.window == null) {
                this.window = new JFrame("AMAME");
                this.window.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
                this.window.setContentPane(view());
                this.window.pack();
            }
            this.window.setVisible(true);
        });
        return true;
    }

    @Override
    protected void editClose() {
        SwingUtilities.invokeLater(() -> {
            if (window != null) {
                window.dispose();
                window = null;
            }
        });
    }
}
