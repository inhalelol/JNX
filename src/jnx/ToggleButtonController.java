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

/**
 *
 * @author lutusp
 */
final public class ToggleButtonController implements ControlInterface {

    JToggleButton box;
    boolean value;

    public ToggleButtonController(JToggleButton b, boolean v) {
        box = b;
        box.setSelected(v);
        value = v;
        box.addItemListener(new java.awt.event.ItemListener() {

            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                item_state_changed();
            }
        });
    }

    private void item_state_changed() {
        value = box.isSelected();
    }

    public boolean get_value() {
        return value;
    }

    public void set_value(String s) {
        value = s.equals("true");
        box.setSelected(value);
    }

    public void set_value(boolean v) {
        value = v;
        box.setSelected(value);
    }

    @Override
    public String toString() {
        return Boolean.toString(value);
    }
}
