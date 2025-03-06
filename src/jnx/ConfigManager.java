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
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;
import java.util.concurrent.*;
import java.io.*;
import java.awt.*;

/**
 *
 * @author lutusp
 */
final public class ConfigManager {

    String line_sep;
    String init_path;
    Component parent;
    Pattern pat;
    Matcher mat;
    ConcurrentSkipListMap<String, ControlInterface> map;

    public ConfigManager(Component p, String path) {
        parent = p;
        init_path = path;
        pat = Pattern.compile("\\s*(.+?)\\s*=\\s*(.+?)\\s*");
        line_sep = System.getProperty("line.separator");
        create_control_map();
        read_config_file();
    }

    // locate all parent fields that
    // implement ControlInterface
    private void create_control_map() {
        map = new ConcurrentSkipListMap<String, ControlInterface>();
        String name;
        Iterator<Field> fi = Arrays.asList(parent.getClass().getDeclaredFields()).iterator();
        while (fi.hasNext()) {
            Field f = fi.next();
            name = f.getName();
            try {
                Object obj = f.get(parent);
                String t = (obj.getClass().getGenericInterfaces()[0]).toString();
                if (t.equals("interface jnx.ControlInterface")) {
                    map.put(name, (ControlInterface) obj);
                }
            } catch (Exception e) {
                //System.out.println(e + " = " + name);
            }
        }
    }

    private void read_config_file() {
        try {
            File f = new File(init_path);
            if (f.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(f));
                String line;
                while ((line = br.readLine()) != null) {
                    mat = pat.matcher(line);
                    if (mat.matches()) {
                        map.get(mat.group(1)).set_value(mat.group(2));
                    }
                }
                br.close();
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public void write_config_file() {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(init_path));
            Iterator<String> is = map.keySet().iterator();
            while (is.hasNext()) {
                String key = is.next();
                String val = map.get(key).toString();
                bw.write(key + " = " + val + line_sep);
            }
            bw.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
