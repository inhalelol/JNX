/***************************************************************************
 *   Copyright (C) 2011 by Paul Lutus                                      *
 *   lutusp@arachnoid.com                                                  *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU General Public License     *
 *   along with this program; if not, write to the                         *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 ***************************************************************************/

/*
 * JNX.java
 *
 * Created on Mar 5, 2011, 8:11:20 AM
 */
package jnx;

import java.util.*;
import java.util.Timer;
import java.util.TimerTask;
import java.io.*;
//import java.text.*;
import java.net.*;
import javax.swing.*;
import javax.sound.sampled.*;
import java.awt.datatransfer.*;
import java.awt.Toolkit;

/**
 *
 * @author lutusp
 */
final public class JNX extends javax.swing.JFrame implements ClipboardOwner {

    final String app_version = "1.4";
    boolean debug = false;
    java.util.List<String> data_rates = Arrays.asList(
            "12000", "16000", "24000", "32000", "48000", "96000");
    java.util.List<String> spectrum_choices = Arrays.asList(
            "No filter", "Space filter", "Mark filter");
    final String app_path, app_name, program_name, user_dir, data_path, init_path, file_sep;
    int timer_period_ms = 250;
    Timer periodic_timer = null;
    int scope_size = 500;
    HelpPane help_pane;
    ConfigManager config_mgr;
    NavtexReceiver receiver;
    ScopePanel time_scope_pane, spectrum_scope_pane;
    SpectrumDisplayManager spectrum_manager;
    TabPanelController selected_tab;
    ToggleButtonController navtex_filter, scroll_to_bottom, log_data, inverse_logic;
    ToggleButtonController configuration_visible, time_scope_sync, spectrum_scope_sync;
    ToggleButtonController strict;
    ComboBoxController audio_source, audio_dest, sample_rate;
    ComboBoxTextController spectrum_selection;
    TextFieldController center_frequency, baud_rate;
    TextFieldController time_scope_hcontrol, time_scope_vcontrol, spectrum_scope_hcontrol, spectrum_scope_vcontrol, monitor_volume;
    FrameController appsize;
    MessageFilter accepted_navtex_messages;
    BufferedWriter log_buffer = null;
    File log_file = null;
    String log_path = null;
    boolean scope_visible = false;

    /** Creates new form JNX */
    public JNX(String args[]) {
        initComponents();
        if (args.length > 0 && args[0].equals("-d")) {
            debug = true;
        }
        app_name = getClass().getSimpleName();
        URL url = getClass().getResource(app_name + ".class");
        String temp = url.getPath().replaceFirst("(.*?)!.*", "$1");
        temp = temp.replaceFirst("file:", "");
        app_path = new File(temp).getPath();
        user_dir = System.getProperty("user.home");
        file_sep = System.getProperty("file.separator");
        data_path = user_dir + file_sep + "." + app_name;
        File f = new File(data_path);
        if (!f.exists()) {
            f.mkdirs();
        }
        init_path = data_path + file_sep + app_name + ".ini";
        log_path = data_path + file_sep + "data.log";
        program_name = app_name + " " + app_version;
        setTitle(program_name);
        setIconImage(new ImageIcon(getClass().getResource("images/" + app_name + "_icon.png")).getImage());
        help_pane = new HelpPane(this);
        tabbed_pane.add(help_pane, "Help");
        receiver = new NavtexReceiver(this);
        time_scope_pane = new ScopePanel(this, "Time Domain", false);
        spectrum_scope_pane = new ScopePanel(this, "Frequency/Spectrum Domain", true);
        java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
        gbc.fill = java.awt.GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        scope_container.add(time_scope_pane, gbc);
        scope_container.add(spectrum_scope_pane, gbc);
        time_scope_hcontrol = time_scope_pane.horizontal_control;
        time_scope_vcontrol = time_scope_pane.vertical_control;
        spectrum_scope_hcontrol = spectrum_scope_pane.horizontal_control;
        spectrum_scope_vcontrol = spectrum_scope_pane.vertical_control;
        time_scope_sync = time_scope_pane.sync_with_signal;
        spectrum_scope_sync = spectrum_scope_pane.sync_with_signal;
        spectrum_manager = new SpectrumDisplayManager(this, spectrum_scope_pane);
        setup_control_values();
        receiver.control(true);
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                inner_close();
            }
        });
        periodic_timer = new java.util.Timer();
        periodic_timer.scheduleAtFixedRate(new PeriodicEvents(), 500, timer_period_ms);
    }

    class PeriodicEvents extends TimerTask {

        public void run() {
            update_indicators();
            update_control_values();
            update_control();
            restart_control();
            update_baud_error();
            receiver.periodic_actions();
            if (debug) {
                if (true) { // tally errors
                    double errors = 0;
                    try {
                        if (receiver.succeed_tally > 0) {
                            errors = 100.0 * receiver.fail_tally / (receiver.succeed_tally + receiver.fail_tally);
                        }
                    } catch (Exception e) {
                    }
                    String s = String.format("Success %d, failure %d, errors %.2f%%", receiver.succeed_tally, receiver.fail_tally, errors);
                    p(s);
                }
            }
            //repaint();
        }
    }

    private void setup_control_values() {
        appsize = new FrameController(this);
        selected_tab = new TabPanelController(tabbed_pane, 0);
        data_textarea.getCaret().setVisible(true);
        strict = new ToggleButtonController(strict_checkbox, false);
        navtex_filter = new ToggleButtonController(navtex_filter_checkbox, false);
        scroll_to_bottom = new ToggleButtonController(scroll_to_bottom_checkbox, true);
        spectrum_selection = new ComboBoxTextController(spectrum_combobox, spectrum_choices, "No filter");
        sample_rate = new ComboBoxController(data_rate_combobox, data_rates, "" + receiver.default_sample_rate);
        java.util.List<String> sdata = make_mixer_description_list(receiver.target_mixer_list, null);
        List<String> data = make_numeric_list(1, receiver.source_mixer_list.size() + 1, 1);
        audio_source = new ComboBoxController(audio_source_combobox, data, "1", sdata, "Select audio input");
        data = make_numeric_list(1, receiver.target_mixer_list.size() + 1, 1);
        audio_dest = new ComboBoxController(audio_dest_combobox, data, "1", sdata, "Select audio output");
        data = make_numeric_list(0, 501, 1);
        monitor_volume = new TextFieldController(monitor_volume_textfield, "0", 500);
        inverse_logic = new ToggleButtonController(inverted_checkbox, false);
        log_data = new ToggleButtonController(logging_checkbox, true);
        configuration_visible = new ToggleButtonController(configuration_checkbox, true);
        logging_checkbox.setToolTipText("Save received data to " + log_path);
        center_frequency = new TextFieldController(center_frequency_textfield, "" + receiver.default_sideband_frequency, 4000);
        baud_rate = new TextFieldController(baud_rate_textfield, "" + receiver.default_baud_rate, 1000);
        accepted_navtex_messages = new MessageFilter();
        config_mgr = new ConfigManager(this, init_path);
    }

    private void update_control_values() {
        receiver.inverse = this.inverse_logic.get_value();
        configuration_panel.setVisible(this.configuration_visible.get_value());
        control_logging(log_data.get_value());
        scope_visible = tabbed_pane.getSelectedComponent() == scope_container;
    }

    private void set_control_defaults() {
        if (ask_user(this, "Okay to reset all values to defaults?", "Reset Control Defaults")) {
            audio_source.set_value("1");
            sample_rate.set_value("" + receiver.default_sample_rate);
            center_frequency.set_value(receiver.default_sideband_frequency);
            baud_rate.set_value(receiver.default_baud_rate);
            inverse_logic.set_value(false);
            strict.set_value(false);
        }
    }

    private void restart_control() {
        boolean restart = false;
        restart |= receiver.audio_source != audio_source.get_value();
        restart |= receiver.audio_dest != audio_dest.get_value();
        restart |= receiver.sample_rate != sample_rate.get_value();
        restart |= receiver.baud_rate != baud_rate.get_dvalue();
        if (restart) {
            receiver.restart();
        }
    }

    public void update_control() {
        boolean update = false;
        update |= receiver.center_frequency_f != center_frequency.get_dvalue();
        if (update) {
            receiver.update_filters();
        }
    }

    private void update_baud_error() {
        String s = ((receiver.state == NavtexReceiver.State.NOSIGNAL) || Math.abs(receiver.baud_error) < 8) ? "OK" : (receiver.baud_error > 0) ? "ERR ->" : "<- ERR";
        baud_error_label.setText(s);
    }

    private void control_logging(boolean enable) {
        try {
            if (enable) {
                if (log_buffer == null || log_file == null || !log_file.exists()) {
                    log_file = new File(log_path);
                    FileWriter log_stream = new FileWriter(log_file, true);
                    log_buffer = new BufferedWriter(log_stream);
                }
            } else {
                if (log_buffer != null) {
                    log_buffer.close();
                    log_buffer = null;
                }
            }
        } catch (Exception e) {
            trace_errors("", e);
        }
    }

    public void log_data(String s) {
        if (log_buffer != null && log_file != null) {
            if (!log_file.exists()) {
                control_logging(true); // reopen deleted log file
            }
            try {
                log_buffer.write(s);
                log_buffer.flush();
                //p(s);
            } catch (Exception e) {
                trace_errors("", e);
            }
        }
    }

    public void trace_errors(String s, Exception e) {
        System.out.println(s + ": " + e);
        e.printStackTrace();
    }

    private List<String> make_numeric_list(int a, int b, int step) {
        List<String> data = new ArrayList<String>();
        for (int i = a; i != b; i += step) {
            data.add(Integer.toString(i));
        }
        return data;
    }

    private List<String> make_mixer_description_list(List<Mixer.Info> data, String extra) {
        List<String> out = new ArrayList<String>();
        if (extra != null) {
            out.add(extra);
        }
        for (Mixer.Info item : data) {
            out.add(item.getDescription());
        }

        return out;
    }

    public void append_to_data_page(String s) {
        log_data(s);
        data_textarea.append(s);
        if (scroll_to_bottom_checkbox.isSelected()) {
            data_textarea.setCaretPosition(data_textarea.getDocument().getLength());
        }
    }

    private void update_indicators() {
        if (receiver != null) {
            machine_state_label.setText(receiver.state.toString());
            update_volume_display();
        }
    }

    private void update_volume_display() {
        int max = 32;
        int v = (int) Math.sqrt(receiver.audio_average) / 4;
        String s = " Volume: ";
        for (int i = 0; i < v && i < max; i++) {
            s += "|";
        }
        if (v >= max) {
            s += "+";
        }
        volume_label.setText(s);
    }

    private void launch_message_filter_dialog() {
        MessageFilterDialog dialog = new MessageFilterDialog(this, true);
        dialog.setVisible(true);
    }

    private void clipboard_copy() {
        String s = data_textarea.getText();
        StringSelection stringSelection = new StringSelection(s);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, this);

    }

    // mandatory clipboard functions
    public void lostOwnership(Clipboard aClipboard, Transferable aContents) {
        //do nothing
    }

    private void clear_text_display() {
        if (ask_user(this, "Erase this display's contents?", "Clear display")) {
            data_textarea.setText("");
            receiver.succeed_tally = 0;
            receiver.fail_tally = 0;
        }
    }

    public static boolean ask_user(JFrame src, String query, String title, Object[] options) {
        if (options != null) {
            return (JOptionPane.showOptionDialog(src, query, title,
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]) == 0);
        } else {
            return (JOptionPane.showConfirmDialog(src, query, title, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION);
        }

    }

    public static boolean ask_user(JFrame src, String query, String title) {
        return ask_user(src, query, title, null);
    }

    public static void tell_user(JFrame src, String message, String title) {
        JOptionPane.showMessageDialog(src, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    public void beep() {
        Toolkit.getDefaultToolkit().beep();
    }

    private void close() {
        if (receiver.state != receiver.state.READ_DATA || ask_user(this, "Okay to quit JNX?", "Close JNX")) {
            inner_close();
            System.exit(0);
        }
    }

    // forced to close by system
    private void inner_close() {
        receiver.end_thread();
        control_logging(false);
        config_mgr.write_config_file();
    }

    public <T> void p(T s) {
        System.out.println(s);
        System.out.flush();
    }

    public <T> void debug_p(T s) {
        if (debug) {
            p(s);
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        tabbed_pane = new javax.swing.JTabbedPane();
        text_display_panel = new javax.swing.JPanel();
        data_scrollpane = new javax.swing.JScrollPane();
        data_textarea = new javax.swing.JTextArea();
        jPanel3 = new javax.swing.JPanel();
        scroll_to_bottom_checkbox = new javax.swing.JCheckBox();
        logging_checkbox = new javax.swing.JCheckBox();
        clipboard_button = new javax.swing.JButton();
        clear_button = new javax.swing.JButton();
        scope_container = new javax.swing.JPanel();
        bottom_panel_a = new javax.swing.JPanel();
        machine_state_label = new javax.swing.JLabel();
        volume_label = new javax.swing.JLabel();
        configuration_checkbox = new javax.swing.JCheckBox();
        quit_button = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        monitor_volume_textfield = new javax.swing.JTextField();
        configuration_panel = new javax.swing.JPanel();
        bottom_panel_b = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        data_rate_combobox = new javax.swing.JComboBox();
        jLabel4 = new javax.swing.JLabel();
        audio_source_combobox = new javax.swing.JComboBox();
        jLabel5 = new javax.swing.JLabel();
        audio_dest_combobox = new javax.swing.JComboBox();
        jLabel9 = new javax.swing.JLabel();
        spectrum_combobox = new javax.swing.JComboBox();
        defaults_button = new javax.swing.JButton();
        bottom_panel_c = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        center_frequency_textfield = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        baud_rate_textfield = new javax.swing.JTextField();
        baud_error_label = new javax.swing.JLabel();
        navtex_filter_checkbox = new javax.swing.JCheckBox();
        message_filter_button = new javax.swing.JButton();
        inverted_checkbox = new javax.swing.JCheckBox();
        strict_checkbox = new javax.swing.JCheckBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(600, 400));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        getContentPane().setLayout(new java.awt.GridBagLayout());

        tabbed_pane.setTabPlacement(javax.swing.JTabbedPane.BOTTOM);

        text_display_panel.setLayout(new java.awt.BorderLayout());

        data_scrollpane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        data_textarea.setEditable(false);
        data_textarea.setFont(new java.awt.Font("Monospaced", 0, 12));
        data_textarea.setLineWrap(true);
        data_textarea.setWrapStyleWord(true);
        data_textarea.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        data_textarea.setDisabledTextColor(new java.awt.Color(51, 51, 51));
        data_textarea.setEnabled(false);
        data_textarea.setFocusable(false);
        data_textarea.setMargin(new java.awt.Insets(4, 4, 4, 4));
        data_scrollpane.setViewportView(data_textarea);

        text_display_panel.add(data_scrollpane, java.awt.BorderLayout.CENTER);

        scroll_to_bottom_checkbox.setSelected(true);
        scroll_to_bottom_checkbox.setText("Scroll to bottom");
        scroll_to_bottom_checkbox.setToolTipText("Scroll to bottom as data is received");
        jPanel3.add(scroll_to_bottom_checkbox);

        logging_checkbox.setText("Log Data");
        logging_checkbox.setToolTipText("Automatically save data");
        jPanel3.add(logging_checkbox);

        clipboard_button.setText("Copy to Clipboard");
        clipboard_button.setToolTipText("Copy received data to clipboard");
        clipboard_button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                clipboard_buttonMouseClicked(evt);
            }
        });
        jPanel3.add(clipboard_button);

        clear_button.setText("Clear");
        clear_button.setToolTipText("Erase this window's content");
        clear_button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                clear_buttonMouseClicked(evt);
            }
        });
        jPanel3.add(clear_button);

        text_display_panel.add(jPanel3, java.awt.BorderLayout.PAGE_END);

        tabbed_pane.addTab("Data Display", text_display_panel);

        scope_container.setLayout(new java.awt.GridBagLayout());
        tabbed_pane.addTab("Time/Frequency Display", scope_container);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(tabbed_pane, gridBagConstraints);

        bottom_panel_a.setMaximumSize(new java.awt.Dimension(200, 25));
        bottom_panel_a.setPreferredSize(new java.awt.Dimension(200, 25));
        bottom_panel_a.setLayout(new java.awt.GridBagLayout());

        machine_state_label.setBackground(new java.awt.Color(51, 51, 51));
        machine_state_label.setForeground(new java.awt.Color(0, 204, 0));
        machine_state_label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        machine_state_label.setText("Machine State");
        machine_state_label.setToolTipText("Present receiver operational state");
        machine_state_label.setAlignmentX(0.1F);
        machine_state_label.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 204, 0)));
        machine_state_label.setMaximumSize(new java.awt.Dimension(150, 18));
        machine_state_label.setMinimumSize(new java.awt.Dimension(150, 18));
        machine_state_label.setOpaque(true);
        machine_state_label.setPreferredSize(new java.awt.Dimension(150, 18));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        bottom_panel_a.add(machine_state_label, gridBagConstraints);

        volume_label.setBackground(new java.awt.Color(51, 51, 51));
        volume_label.setForeground(new java.awt.Color(255, 255, 0));
        volume_label.setText("Volume");
        volume_label.setToolTipText("Relative volume level");
        volume_label.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 255, 0)));
        volume_label.setMaximumSize(new java.awt.Dimension(200, 18));
        volume_label.setMinimumSize(new java.awt.Dimension(200, 18));
        volume_label.setOpaque(true);
        volume_label.setPreferredSize(new java.awt.Dimension(200, 18));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        bottom_panel_a.add(volume_label, gridBagConstraints);

        configuration_checkbox.setText("Config");
        configuration_checkbox.setToolTipText("Show or hide configuration panel\n");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        bottom_panel_a.add(configuration_checkbox, gridBagConstraints);

        quit_button.setText("Quit");
        quit_button.setToolTipText("Exit JNX");
        quit_button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                quit_buttonMouseClicked(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.1;
        bottom_panel_a.add(quit_button, gridBagConstraints);

        jLabel2.setText("| Volume:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        bottom_panel_a.add(jLabel2, gridBagConstraints);

        monitor_volume_textfield.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        monitor_volume_textfield.setText("0.0");
        monitor_volume_textfield.setToolTipText("Adjust monitor volume, 0 = off");
        monitor_volume_textfield.setMaximumSize(new java.awt.Dimension(50, 19));
        monitor_volume_textfield.setMinimumSize(new java.awt.Dimension(50, 19));
        monitor_volume_textfield.setPreferredSize(new java.awt.Dimension(50, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        bottom_panel_a.add(monitor_volume_textfield, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        getContentPane().add(bottom_panel_a, gridBagConstraints);

        configuration_panel.setLayout(new java.awt.GridBagLayout());

        bottom_panel_b.setToolTipText("");
        bottom_panel_b.setLayout(new java.awt.GridBagLayout());

        jLabel1.setText("Rate:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        bottom_panel_b.add(jLabel1, gridBagConstraints);

        data_rate_combobox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        data_rate_combobox.setToolTipText("Select data sampling rate (samples/second)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
        bottom_panel_b.add(data_rate_combobox, gridBagConstraints);

        jLabel4.setText("Input:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        bottom_panel_b.add(jLabel4, gridBagConstraints);

        audio_source_combobox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
        bottom_panel_b.add(audio_source_combobox, gridBagConstraints);

        jLabel5.setText("Output:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        bottom_panel_b.add(jLabel5, gridBagConstraints);

        audio_dest_combobox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
        bottom_panel_b.add(audio_dest_combobox, gridBagConstraints);

        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel9.setText("Spectrum:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        bottom_panel_b.add(jLabel9, gridBagConstraints);

        spectrum_combobox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        spectrum_combobox.setToolTipText("Choose spectrum source");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 7;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
        bottom_panel_b.add(spectrum_combobox, gridBagConstraints);

        defaults_button.setText("Defaults");
        defaults_button.setToolTipText("Set all default values");
        defaults_button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                defaults_buttonMouseClicked(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
        bottom_panel_b.add(defaults_button, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(1, 1, 1, 1);
        configuration_panel.add(bottom_panel_b, gridBagConstraints);

        bottom_panel_c.setLayout(new java.awt.GridBagLayout());

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel3.setText("Freq. Hz:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        bottom_panel_c.add(jLabel3, gridBagConstraints);

        center_frequency_textfield.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        center_frequency_textfield.setText("0.0000");
        center_frequency_textfield.setToolTipText("Receiver sideband frequency Hz");
        center_frequency_textfield.setMaximumSize(new java.awt.Dimension(80, 19));
        center_frequency_textfield.setMinimumSize(new java.awt.Dimension(80, 19));
        center_frequency_textfield.setPreferredSize(new java.awt.Dimension(80, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
        bottom_panel_c.add(center_frequency_textfield, gridBagConstraints);

        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel7.setText("Baud:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        bottom_panel_c.add(jLabel7, gridBagConstraints);

        baud_rate_textfield.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        baud_rate_textfield.setText("0.000");
        baud_rate_textfield.setToolTipText("Receiver baud rate Hz");
        baud_rate_textfield.setMaximumSize(new java.awt.Dimension(80, 19));
        baud_rate_textfield.setMinimumSize(new java.awt.Dimension(80, 19));
        baud_rate_textfield.setPreferredSize(new java.awt.Dimension(80, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
        bottom_panel_c.add(baud_rate_textfield, gridBagConstraints);

        baud_error_label.setBackground(new java.awt.Color(51, 51, 51));
        baud_error_label.setForeground(new java.awt.Color(153, 204, 255));
        baud_error_label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        baud_error_label.setText("OK");
        baud_error_label.setToolTipText("Baud rate tracker status");
        baud_error_label.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 204, 255)));
        baud_error_label.setMaximumSize(new java.awt.Dimension(80, 15));
        baud_error_label.setMinimumSize(new java.awt.Dimension(80, 15));
        baud_error_label.setOpaque(true);
        baud_error_label.setPreferredSize(new java.awt.Dimension(80, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
        bottom_panel_c.add(baud_error_label, gridBagConstraints);

        navtex_filter_checkbox.setText("Navtex Filter");
        navtex_filter_checkbox.setToolTipText("Enable/disable Navtex-format message filtering");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
        bottom_panel_c.add(navtex_filter_checkbox, gridBagConstraints);

        message_filter_button.setText("Select");
        message_filter_button.setToolTipText("Choose which Navtex messages to display");
        message_filter_button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                message_filter_buttonMouseClicked(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
        bottom_panel_c.add(message_filter_button, gridBagConstraints);

        inverted_checkbox.setText("Inverted");
        inverted_checkbox.setToolTipText("Reverse mark and space signals");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
        bottom_panel_c.add(inverted_checkbox, gridBagConstraints);

        strict_checkbox.setText("Strict");
        strict_checkbox.setToolTipText("Reject unconfirmed characters");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
        bottom_panel_c.add(strict_checkbox, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(1, 1, 1, 1);
        configuration_panel.add(bottom_panel_c, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        getContentPane().add(configuration_panel, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void quit_buttonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_quit_buttonMouseClicked
        close();
    }//GEN-LAST:event_quit_buttonMouseClicked

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        close();
    }//GEN-LAST:event_formWindowClosing

    private void clipboard_buttonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_clipboard_buttonMouseClicked
        // TODO add your handling code here:
        clipboard_copy();
    }//GEN-LAST:event_clipboard_buttonMouseClicked

    private void clear_buttonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_clear_buttonMouseClicked
        // TODO add your handling code here:
        clear_text_display();
    }//GEN-LAST:event_clear_buttonMouseClicked

    private void defaults_buttonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_defaults_buttonMouseClicked
        // TODO add your handling code here:
        set_control_defaults();
    }//GEN-LAST:event_defaults_buttonMouseClicked

    private void message_filter_buttonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_message_filter_buttonMouseClicked
        // TODO add your handling code here:
        launch_message_filter_dialog();
    }//GEN-LAST:event_message_filter_buttonMouseClicked

    /**
     * @param args the command line arguments
     */
    public static void main(final String args[]) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            java.awt.EventQueue.invokeLater(new Runnable() {

                public void run() {
                    new JNX(args).setVisible(true);
                }
            });
        } catch (Exception e) {
            System.out.println("JNX main: " + e);
            e.printStackTrace();
        }
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox audio_dest_combobox;
    private javax.swing.JComboBox audio_source_combobox;
    private javax.swing.JLabel baud_error_label;
    private javax.swing.JTextField baud_rate_textfield;
    private javax.swing.JPanel bottom_panel_a;
    private javax.swing.JPanel bottom_panel_b;
    private javax.swing.JPanel bottom_panel_c;
    private javax.swing.JTextField center_frequency_textfield;
    private javax.swing.JButton clear_button;
    private javax.swing.JButton clipboard_button;
    private javax.swing.JCheckBox configuration_checkbox;
    private javax.swing.JPanel configuration_panel;
    private javax.swing.JComboBox data_rate_combobox;
    private javax.swing.JScrollPane data_scrollpane;
    protected javax.swing.JTextArea data_textarea;
    private javax.swing.JButton defaults_button;
    private javax.swing.JCheckBox inverted_checkbox;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JCheckBox logging_checkbox;
    private javax.swing.JLabel machine_state_label;
    protected javax.swing.JButton message_filter_button;
    private javax.swing.JTextField monitor_volume_textfield;
    protected javax.swing.JCheckBox navtex_filter_checkbox;
    private javax.swing.JButton quit_button;
    private javax.swing.JPanel scope_container;
    private javax.swing.JCheckBox scroll_to_bottom_checkbox;
    private javax.swing.JComboBox spectrum_combobox;
    private javax.swing.JCheckBox strict_checkbox;
    private javax.swing.JTabbedPane tabbed_pane;
    private javax.swing.JPanel text_display_panel;
    private javax.swing.JLabel volume_label;
    // End of variables declaration//GEN-END:variables
}
