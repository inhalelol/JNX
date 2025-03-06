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
import java.util.*;
import java.awt.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

/**
 *
 * @author lutusp
 */
final public class ComboBoxTextController implements ControlInterface {

    JComboBox box;
    String value;
    java.util.List<String> tooltips = null;
    String prompt = null;

    public ComboBoxTextController(JComboBox b, java.util.List<String> data, String sel, java.util.List<String> tips, String prompt) {
        setup(b, data, sel, tips, prompt);
    }

    public ComboBoxTextController(JComboBox b, java.util.List<String> data, String sel) {
        setup(b, data, sel, null, null);
    }

    private void setup(JComboBox b, java.util.List<String> data, String sel, java.util.List<String> tips, String prompt) {
        box = b;
        tooltips = new java.util.ArrayList<String>();
        this.prompt = prompt;
        populate_combobox(data, sel, tips);
        box.setRenderer(new ToolTipComboBoxRenderer());
        box.addMouseWheelListener(new java.awt.event.MouseWheelListener() {

            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                mouse_wheel_event(evt);
            }
        });
        box.addItemListener(new java.awt.event.ItemListener() {

            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                item_state_changed(evt);
            }
        });
        set_box_tooltip();
    }

    public void populate_combobox(java.util.List<String> data, String sel, java.util.List<String> tips) {
        if (tips != null) {
            for (int i = tips.size() - 1; i >= 0; i--) {
                tooltips.add(tips.get(i));
            }
        }
        box.removeAllItems();
        for (int i = data.size() - 1; i >= 0; i--) {
            box.addItem(data.get(i));
        }
        set_value(sel);
    }

    private void mouse_wheel_event(java.awt.event.MouseWheelEvent evt) {
        if (box.isEnabled()) {
            double n = (evt.getWheelRotation() > 0) ? 1 : -1;
            // modifier key multipliers
            n = (evt.isShiftDown()) ? n * 10 : n;
            n = (evt.isControlDown()) ? n * 10 : n;
            n = (evt.isAltDown()) ? n * 10 : n;
            int v = box.getSelectedIndex();
            v += n;
            v = check_range(v);
            box.setSelectedIndex(v);
            set_box_tooltip();
        }
    }

    private int check_range(int v) {
        int top = box.getItemCount();
        v = Math.min(top - 1, v);
        v = Math.max(0, v);
        return v;
    }

    private void item_state_changed(java.awt.event.ItemEvent evt) {
        set_box_tooltip();
        try {
            String s = (String) box.getSelectedItem();
            value = s;
            //System.out.println("new value: " + value);
        } catch (Exception e) {
            //System.out.println(e);
        }
    }

    private void set_box_tooltip() {
        int top = box.getItemCount();
        int i = box.getSelectedIndex();
        if (i >= 0 && i < top && tooltips != null && i < tooltips.size()) {
            String tip = tooltips.get(i);
            if (prompt != null) {
                tip = prompt + ": " + tip;
            }
            box.setToolTipText(tip);
            // now trick the system into updating the toooltip
            PointerInfo a = MouseInfo.getPointerInfo();
            Point b = a.getLocation();
            try {
                Robot r = new Robot();
                r.mouseMove(b.x, b.y - 1);
                r.mouseMove(b.x, b.y);
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    public String get_value() {
        return value;
    }

    public int get_index() {
        return box.getSelectedIndex();
    }


    public void set_value(String s) {
        try {
            box.setSelectedItem(s);
            // is this item in the list?
            String r = (String) box.getSelectedItem();
            if (!r.equals(s)) {
                box.setSelectedIndex(0);
                s = (String) box.getSelectedItem();
            }
            value = s;
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    @Override
    public String toString() {
        return value;
    }

    class ToolTipComboBoxRenderer extends BasicComboBoxRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value,
                int index, boolean is_selected, boolean has_focus) {
            if (is_selected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
                if (index >= 0 && tooltips != null && index < tooltips.size()) {
                    list.setToolTipText(tooltips.get(index));
                }
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            setFont(list.getFont());
            setText((value == null) ? "" : value.toString());
            return this;
        }
    };
}
