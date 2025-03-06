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
final public class TabPanelController implements ControlInterface {

    JTabbedPane pane;

    public TabPanelController(JTabbedPane f,int def) {
        pane = f;
        pane.setSelectedIndex(def);
    }

    public void set_value(String s) {
        try {
            int tab = Integer.parseInt(s);
            pane.setSelectedIndex(tab);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    @Override
    public String toString() {
        return String.format("%d", pane.getSelectedIndex());
    }
}
