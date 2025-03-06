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
package jnx;

import javax.swing.*;
import java.awt.*;

/**
 *
 * @author lutusp
 */
final public class TextFieldController implements ControlInterface {

    JTextField field;
    private String value;
    private double dvalue = 0;
    private double maxvalue;
    private double pct_dvalue;
    private double n;

    public TextFieldController(JTextField f, String s, double mv) {
        field = f;
        field.setText(s);
        maxvalue = mv;
        input_changed();
        String tt = field.getToolTipText();
        if(tt == null || tt.length() == 0) {
            tt ="";
        }
        else {
             tt += "<br/>";
        }
        tt = "<html>" + tt + "(Change by typing or spin mouse wheel<br/>Ctrl,Shift,Alt increase rate of change)</html>";
        field.setToolTipText(tt);
        field.addKeyListener(new java.awt.event.KeyAdapter() {

            @Override
            public void keyReleased(java.awt.event.KeyEvent evt) {
                input_changed();
            }
        });
        field.addMouseWheelListener(new java.awt.event.MouseWheelListener() {

            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                mouse_wheel_event(evt);
            }
        });
    }

    private void mouse_wheel_event(java.awt.event.MouseWheelEvent evt) {
        if (field.isEnabled()) {
            double n = (evt.getWheelRotation() > 0) ? -1 : 1;
            process_modifier(evt, n);
        }
    }

    private void process_modifier(java.awt.event.InputEvent evt, double n) {
        n = (evt.isShiftDown()) ? n * 10 : n;
        n = (evt.isControlDown()) ? n * 10 : n;
        n = (evt.isAltDown()) ? n * 10 : n;
        dvalue += n;
        dvalue = (dvalue < 0.0) ? 0.0 : dvalue;
        process_entry();
        input_changed();
        process_entry();
    }

    private void process_entry() {
        value = String.format("%.1f", dvalue);
        field.setText(value);
    }

    private void input_changed() {
        field.setForeground(Color.black);
        try {
            value = field.getText();
            dvalue = Double.parseDouble(value);
            set_error(dvalue);
            dvalue = (dvalue < 0.0) ? 0.0 : dvalue;
            dvalue = (dvalue > maxvalue)?maxvalue:dvalue;
            // scaled value for percentages
            pct_dvalue = dvalue * .01;
            //process_entry();
        } catch (Exception e) {
            field.setForeground(Color.red);
        }
    }

    private void set_error(double v) {
        boolean error = (v < 0 || v > maxvalue);
        field.setForeground(error?Color.red:Color.black);
    }

    public String get_value() {
        return String.format("%.1f", dvalue);
    }

    public double get_dvalue() {
        return dvalue;
    }

    public double get_pct_dvalue() {
        return pct_dvalue;
    }

    public void set_value(String s) {
        field.setText(s);
        input_changed();
    }

    public void set_value(double v) {
        set_value("" + v);
    }

    @Override
    public String toString() {
        return value;
    }
}
