/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package amame.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.Icon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;

import amame.emu.Machine;
import amame.emu.MediaSlot;
import amame.emu.Panel;
import amame.frontend.Frontend;

import static java.lang.System.getLogger;


/**
 * The front panel in swing: amame's web gui and frontend page in one.
 * <p>
 * On top the machine to play, the device hot swap of amame, and the system exclusive import and
 * export. Below that the disks and cartridges of the machine, its display and its controls with
 * their lights, and the virtual keyboard at the bottom. The buttons and sliders do their work on
 * the emulation thread.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-21 nsano initial version <br>
 */
public final class PanelView extends JPanel {

    private static final Logger logger = getLogger(PanelView.class.getName());

    private final Frontend frontend;

    private final DisplayView displayView = new DisplayView();
    private final JPanel controlsView = new JPanel();
    private final JPanel mediaView = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
    private final JComboBox<String> machines = new JComboBox<>(Frontend.NAMES.toArray(String[]::new));
    private final JLabel sysexLength = new JLabel("sysex 0 bytes");
    private final JLabel title = new JLabel();
    private final Timer refresh;
    private boolean switching;

    public PanelView(Frontend frontend) {
        super(new BorderLayout(4, 4));
        this.frontend = frontend;
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.add(new JLabel("machine"));
        toolbar.add(machines);
        machines.addActionListener(e -> {
            if (!switching) switchMachine((String) machines.getSelectedItem());
        });
        JButton reset = new JButton("Reset");
        reset.addActionListener(e -> {
            Machine m = frontend.getMachine();
            if (m != null) m.post(m::reset);
        });
        toolbar.add(reset);
        JButton load = new JButton("Load .syx");
        load.addActionListener(e -> loadSysex());
        toolbar.add(load);
        JButton save = new JButton("Save .syx");
        save.addActionListener(e -> saveSysex());
        toolbar.add(save);
        JButton clear = new JButton("Clear");
        clear.addActionListener(e -> frontend.clearSysex());
        toolbar.add(clear);
        toolbar.add(sysexLength);
        add(toolbar, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(4, 4));
        JPanel displayBox = new JPanel(new BorderLayout());
        displayBox.add(title, BorderLayout.NORTH);
        displayBox.add(displayView, BorderLayout.CENTER);
        displayBox.add(mediaView, BorderLayout.SOUTH);
        center.add(displayBox, BorderLayout.NORTH);
        controlsView.setLayout(new BoxLayout(controlsView, BoxLayout.Y_AXIS));
        center.add(new JScrollPane(controlsView), BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        add(new KeyboardView((note, velocity) -> frontend.key(note, velocity, 0)), BorderLayout.SOUTH);

        frontend.addMachineListener(m -> SwingUtilities.invokeLater(() -> showMachine(m)));
        frontend.addSysexListener(length -> SwingUtilities.invokeLater(() -> sysexLength.setText("sysex " + length + " bytes")));

        refresh = new Timer(1000 / 30, e -> {
            displayView.repaint();
            controlsView.repaint();
            mediaView.repaint();
        });
        refresh.start();

        if (frontend.getMachine() != null) showMachine(frontend.getMachine());
    }

    /** stops the display refresh, when the window goes away */
    public void dispose() {
        refresh.stop();
    }

    private void switchMachine(String name) {
        Machine current = frontend.getMachine();
        if (current != null && current.getName().equals(name)) return;
        machines.setEnabled(false);
        // the roms load on a thread of their own, the gui and the audio go on meanwhile
        Thread.ofVirtual().start(() -> {
            try {
                frontend.setMachine(name);
            } catch (IOException | RuntimeException e) {
                logger.log(Level.WARNING, "cannot start " + name, e);
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                        "cannot start " + name + ":\n" + e.getMessage(), "amame", JOptionPane.ERROR_MESSAGE));
            } finally {
                SwingUtilities.invokeLater(() -> {
                    machines.setEnabled(true);
                    Machine m = frontend.getMachine();
                    if (m != null) selectQuietly(m.getName());
                });
            }
        });
    }

    private void selectQuietly(String name) {
        switching = true;
        machines.setSelectedItem(name);
        switching = false;
    }

    private void showMachine(Machine m) {
        selectQuietly(m.getName());
        title.setText(m.getDescription());
        Panel panel = m.getPanel();
        displayView.setDisplay(panel.getDisplay());

        controlsView.removeAll();
        Map<String, JPanel> groups = new LinkedHashMap<>();
        for (Panel.Control control : panel.getControls()) {
            JPanel group = groups.computeIfAbsent(control.getGroup(), g -> {
                JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
                if (!g.isEmpty()) p.setBorder(BorderFactory.createTitledBorder(g));
                return p;
            });
            group.add(widget(m, control));
        }
        for (JPanel group : groups.values()) controlsView.add(group);
        controlsView.revalidate();
        controlsView.repaint();

        mediaView.removeAll();
        for (Panel.Light light : panel.getLights()) {
            mediaView.add(new JLabel(light.getName(), new Led(light::isLit), JLabel.LEFT));
        }
        for (MediaSlot slot : m.getMediaSlots()) mediaView.add(mediaWidget(m, slot));
        mediaView.revalidate();
        Component root = SwingUtilities.getRoot(this);
        if (root instanceof java.awt.Window w) w.pack();
    }

    private Component widget(Machine m, Panel.Control control) {
        return switch (control.getType()) {
            case BUTTON -> {
                JButton button = new JButton(control.getName());
                button.setMargin(new java.awt.Insets(2, 4, 2, 4));
                if (control.hasLight()) button.setIcon(new Led(control::isLit));
                boolean[] down = {false};
                button.getModel().addChangeListener(e -> {
                    boolean pressed = button.getModel().isPressed();
                    if (pressed != down[0]) {
                        down[0] = pressed;
                        m.post(() -> control.set(pressed ? 1f : 0f));
                    }
                });
                yield button;
            }
            case SLIDER -> {
                JPanel p = new JPanel(new GridLayout(2, 1));
                p.add(new JLabel(control.getName()));
                JSlider slider = new JSlider(0, 1023, 1023);
                slider.addChangeListener(e -> {
                    float value = slider.getValue() / 1023f;
                    m.post(() -> control.set(value));
                });
                p.add(slider);
                yield p;
            }
        };
    }

    /** a light, red when lit */
    private record Led(BooleanSupplier lit) implements Icon {

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(lit.getAsBoolean() ? new Color(0xff3030) : new Color(0x502020));
            g2.fillOval(x, y, getIconWidth(), getIconHeight());
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return 8;
        }

        @Override
        public int getIconHeight() {
            return 8;
        }
    }

    /** a floppy drive or a cartridge slot: what is in it, and putting a file in or taking it out */
    private Component mediaWidget(Machine m, MediaSlot slot) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        p.setBorder(BorderFactory.createTitledBorder(slot.getName()));
        JLabel file = new JLabel();
        Runnable update = () -> {
            Path path = slot.getPath();
            file.setText(path != null ? path.getFileName().toString() : "(empty)");
        };
        update.run();
        p.add(file);
        String[] extensions = slot.getExtensions().toArray(String[]::new);
        JButton load = new JButton("Load");
        load.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter(slot.getName().toLowerCase(), extensions));
            if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
            Path path = chooser.getSelectedFile().toPath();
            media(m, slot, () -> slot.load(path), update);
        });
        p.add(load);
        JButton create = new JButton("New");
        create.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new java.io.File("amame." + extensions[0]));
            if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            Path path = chooser.getSelectedFile().toPath();
            if (java.nio.file.Files.exists(path) && JOptionPane.showConfirmDialog(this, path.getFileName() + " exists, replace it?",
                    "amame", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
            media(m, slot, () -> slot.create(path), update);
        });
        p.add(create);
        JButton eject = new JButton("Eject");
        eject.addActionListener(e -> media(m, slot, slot::unload, update));
        p.add(eject);
        return p;
    }

    /** something done to a medium */
    @FunctionalInterface
    private interface MediaAction {
        void run() throws IOException;
    }

    /** does it on the emulation thread, and shows what is in the slot or what went wrong after */
    private void media(Machine m, MediaSlot slot, MediaAction action, Runnable update) {
        m.post(() -> {
            try {
                action.run();
            } catch (IOException | RuntimeException e) {
                logger.log(Level.WARNING, slot.getName(), e);
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, e.getMessage(), "amame", JOptionPane.ERROR_MESSAGE));
            }
            SwingUtilities.invokeLater(update);
        });
    }

    private void loadSysex() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("system exclusive", "syx"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path path = chooser.getSelectedFile().toPath();
        try {
            frontend.loadSysex(path);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "amame", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveSysex() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("amame.syx"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            frontend.saveSysex(chooser.getSelectedFile().toPath());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "amame", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** the views of the groups, for tests */
    List<Component> getGroups() {
        return List.of(controlsView.getComponents());
    }
}
