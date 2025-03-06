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
import java.util.regex.*;

/**
 *
 * @author lutusp
 */
final public class FrameController implements ControlInterface {

    JFrame frame;
    Pattern pat;

    public FrameController(JFrame f) {
        frame = f;
        pat = Pattern.compile("\\s*(\\d+?)\\s*,\\s*(\\d+?)\\s*");
    }

    public void set_value(String s) {
        try {
            Matcher mat = pat.matcher(s);
            if (mat.matches()) {
                int w = Integer.parseInt(mat.group(1));
                int h = Integer.parseInt(mat.group(2));
                frame.setSize(w, h);
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    @Override
    public String toString() {
        int w = frame.getWidth();
        int h = frame.getHeight();
        return String.format("%d,%d", w, h);
    }
}
