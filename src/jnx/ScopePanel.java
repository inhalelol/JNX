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
 * ScopePanel.java
 *
 * Created on Mar 5, 2011, 8:53:32 AM
 */
package jnx;

import java.awt.*;
import java.awt.image.*;
import javax.swing.*;
import javax.swing.border.*;
import java.io.*;
import javax.imageio.*;

/**
 *
 * @author lutusp
 */
final public class ScopePanel extends JPanel {

    JNX parent = null;
    int scope_size;
    int scope_sync_max;
    InternalScopePanel scope_pane;
    double vscalea = -1, vscaleb = 1;
    double hscale = .1; // seconds
    double hcal = 1;
    String h_unit = "Sec";
    double gain_scale = .5;
    int traces = 4;
    int scope_pointer = 0;
    // -1 = presync, 0 = awaiting sync, 1 = synced
    int sync_phase = 1;
    int sync_count = 0;
    double old_sync = 0;
    double[] inputa = null, inputb = null, outputa = null, outputb = null;
    boolean spectrum_trace = false;
    boolean dual_trace = true;
    TextFieldController horizontal_control, vertical_control;
    ToggleButtonController sync_with_signal;
    Color zero_line_color;

    /** Creates new form ScopePanel */
    public ScopePanel(JNX p, String title, boolean spec) {
        parent = p;
        initComponents();
        spectrum_trace(spec);
        zero_line_color = new Color(.2f, .5f, .8f);
        TitledBorder tb = (TitledBorder) getBorder();
        tb.setTitle(title);
        scope_pane = new InternalScopePanel();
        add(scope_pane);
        horizontal_control = new TextFieldController(hscale_textfield, "100", 1000);
        vertical_control = new TextFieldController(vscale_textfield, "100", 1000);
        sync_with_signal = new ToggleButtonController(this.sync_checkbox, false);
    }

    public void spectrum_trace(boolean v) {
        spectrum_trace = v;
        if (spectrum_trace) {
            this.sync_checkbox.setEnabled(!spectrum_trace);
            gain_scale = 2;
            vscalea = 0;
            h_unit = "Hz";
        }
    }

    public void reset_scope_size() {
        scope_size = (int) (hscale * parent.receiver.sample_rate);
        hcal = (double) scope_size / parent.receiver.sample_rate;
        reset_scope_inner();
    }

    public void reset_scope_params(int size) {
        scope_size = size;
        reset_scope_inner();
    }

    private void reset_scope_inner() {
        scope_pointer = 0;
        inputa = new double[scope_size];
        inputb = new double[scope_size];
        outputa = new double[scope_size];
        outputb = new double[scope_size];
        scope_sync_max = scope_size * 3 / 4;
    }

    public void set_hcal(double v) {
        hcal = v;
    }

    public void write_value(double a) {
        write_value2(a, 0, false);
    }

    public void write_value(double a, double b) {
        write_value2(a, b, true);
    }

    public void write_value2(double a, double b, boolean dual) {
        dual_trace = dual;
        if (inputa != null) {
            if (sync_phase < 1) {
                switch (sync_phase) {
                    case -1:
                        old_sync = a;
                        sync_phase++;
                        break;
                    case 0:
                        if (a > 0 && old_sync < 0 || sync_count > scope_sync_max) {
                            sync_phase++;
                        }
                        break;
                }
                old_sync = a;
                sync_count++;
            }
            if (sync_phase == 1) {
                inputa[scope_pointer] = a;
                inputb[scope_pointer] = b;
                scope_pointer++;
                if (scope_pointer >= scope_size) {
                    scope_pointer = 0;
                    System.arraycopy(inputa, 0, outputa, 0, scope_size);
                    System.arraycopy(inputb, 0, outputb, 0, scope_size);
                    repaint();
                    sync_count = 0;
                    if (sync_with_signal.get_value()) {
                        sync_phase = -1;
                    } else {
                        sync_phase = 1;
                    }
                    check_controls();
                }
            }
        }
    }

    private void check_controls() {
        if (!spectrum_trace) {
            double v = this.horizontal_control.get_pct_dvalue() * 0.1;
            v = (v < .01) ? .01 : v;
            int ss = (int) (v * parent.receiver.sample_rate);
            if (ss != scope_size) {
                hscale = v;
                reset_scope_size();
            }
        }
    }

    public void write_array(double[] data) {
        dual_trace = false;
        System.arraycopy(data, 0, outputa, 0, scope_size);
        repaint();
    }

    private double ntrp(double x, double xa, double xb, double ya, double yb) {
        return (x - xa) * (yb - ya) / (xb - xa) + ya;
    }

    class InternalScopePanel extends JPanel {

        public InternalScopePanel() {
            super();
            addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {

                @Override
                public void mouseMoved(java.awt.event.MouseEvent evt) {
                    track_mouse(evt);
                }
            });
            addMouseListener(new java.awt.event.MouseAdapter() {

                @Override
                public void mouseClicked(java.awt.event.MouseEvent evt) {
                    handle_mouse_click(evt);
                }
            });

            addMouseListener(new java.awt.event.MouseAdapter() {

                @Override
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    mouse_exited(evt);
                }
            });
        }

        

        // capture images for documentation
        private void handle_mouse_click(java.awt.event.MouseEvent evt) {
            if (parent.debug && evt.getButton() == 3) {
                try {
                    int w = 250, h = 125;
                    BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g = bi.createGraphics();
                    paint_scope(w, h, g);
                    int image_num = 0;
                    File f = null;
                    do {
                        f = new File(parent.data_path + "/" + "image" + image_num + ".png");
                        image_num++;
                    } while (f.exists());
                    ImageIO.write(bi, "PNG", f);
                    image_num++;
                } catch (Exception e) {
                    System.out.println(e);
                    e.printStackTrace();
                }
            }
        }

        private void track_mouse(java.awt.event.MouseEvent evt) {
            double x = (double) evt.getX() * hcal / getWidth();
            double y = 1 - ((double) evt.getY() / getHeight());
            y = (y * (vscaleb - vscalea) + vscalea) / (vertical_control.get_pct_dvalue() * gain_scale);
            String s = String.format("Mouse cursor: x = %.2f %s, y = %.2f", x, h_unit, y);
            mouse_pos_label.setText(s);
        }

        private void mouse_exited(java.awt.event.MouseEvent evt) {
            mouse_pos_label.setText("         ");
        }

        @Override
        public void paintComponent(Graphics g) {
            Rectangle r = g.getClipBounds();
            int width = r.width;
            int height = r.height;
            paint_scope(width, height, g);
        }

        private void paint_scope(int width, int height, Graphics gg) {
            double gain = vertical_control.get_pct_dvalue() * gain_scale;
            if (parent != null && outputa != null) {
                Graphics2D g = (Graphics2D) gg;
                // don't enable anitaliasing .. it places a huge
                // burden on the system
                //g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                //        RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(Color.black);
                g.fillRect(0, 0, width, height);
                draw_grid(width, height, g);
                if (dual_trace) {
                    draw_trace(outputa, gain, width, height - height / 2, g);
                    draw_trace(outputb, gain, width, height + height / 2, g);
                } else {
                    draw_trace(outputa, gain, width, height, g);
                }
            }
        }

        private void draw_trace(double[] data, double gain, int horiz, int vert, Graphics2D g) {
            int px, py, ox = 0, oy = 0;
            int len = data.length;
            py = (int) ntrp(0, vscalea, vscaleb, vert, 0);
            g.setColor(zero_line_color);
            g.drawLine(0, py, horiz, py);
            g.setColor(Color.green);
            for (int i = 0; i < len; i++) {
                double y = data[i] * gain;
                px = (int) ntrp(i, 0, len, 0, horiz);
                py = (int) ntrp(y, vscalea, vscaleb, vert, 0);
                if (i != 0) {
                    g.drawLine(ox, oy, px, py);
                }
                ox = px;
                oy = py;
            }
        }

        private void draw_grid(int w, int h, Graphics2D g) {
            int s = 8;
            double v = 0;
            g.setColor(new Color(.5f, .5f, .3f));
            Stroke old_stroke = g.getStroke();
            g.setStroke(new BasicStroke(
                    1f,
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND,
                    1f,
                    new float[]{2f},
                    0f));
            for (int y = 0; y <= s; y++) {
                v = (double) y / s;
                int pv = (int) (v * h);
                g.drawLine(0, pv, w, pv);
                pv = (int) (v * w);
                g.drawLine(pv, 0, pv, h);
            }
            g.setStroke(old_stroke);

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

        control_panel = new javax.swing.JPanel();
        mouse_pos_label = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        hscale_textfield = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        vscale_textfield = new javax.swing.JTextField();
        sync_checkbox = new javax.swing.JCheckBox();

        setBorder(javax.swing.BorderFactory.createTitledBorder("Title"));
        setLayout(new java.awt.BorderLayout());

        control_panel.setLayout(new java.awt.GridBagLayout());

        mouse_pos_label.setFont(new java.awt.Font("Monospaced", 1, 12)); // NOI18N
        mouse_pos_label.setText("         ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
        control_panel.add(mouse_pos_label, gridBagConstraints);

        jLabel1.setText("H Scale:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
        control_panel.add(jLabel1, gridBagConstraints);

        hscale_textfield.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        hscale_textfield.setText("100.0");
        hscale_textfield.setMaximumSize(new java.awt.Dimension(80, 19));
        hscale_textfield.setMinimumSize(new java.awt.Dimension(80, 19));
        hscale_textfield.setPreferredSize(new java.awt.Dimension(80, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
        control_panel.add(hscale_textfield, gridBagConstraints);

        jLabel2.setText("V Scale %:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
        control_panel.add(jLabel2, gridBagConstraints);

        vscale_textfield.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        vscale_textfield.setText("100.0");
        vscale_textfield.setMaximumSize(new java.awt.Dimension(80, 19));
        vscale_textfield.setMinimumSize(new java.awt.Dimension(80, 19));
        vscale_textfield.setPreferredSize(new java.awt.Dimension(80, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
        control_panel.add(vscale_textfield, gridBagConstraints);

        sync_checkbox.setSelected(true);
        sync_checkbox.setText("Sync");
        sync_checkbox.setToolTipText("Try to synchronize with data");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
        control_panel.add(sync_checkbox, gridBagConstraints);

        add(control_panel, java.awt.BorderLayout.SOUTH);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel control_panel;
    private javax.swing.JTextField hscale_textfield;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel mouse_pos_label;
    private javax.swing.JCheckBox sync_checkbox;
    private javax.swing.JTextField vscale_textfield;
    // End of variables declaration//GEN-END:variables
}
