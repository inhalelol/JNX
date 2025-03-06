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
 * MyHelpPane.java
 *
 * Created on Feb 18, 2009, 2:12:32 PM
 */
package jnx;

import javax.swing.*;
import java.net.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.text.html.HTMLDocument.*;
import java.util.*;
import java.io.*;

/**
 *
 * @author lutusp
 */
final public class HelpPane extends javax.swing.JPanel {

    JNX parent;
    Stack<Integer> undoStack;
    Stack<Integer> redoStack;
    Document doc;
    String oldSearch = "";
    int oldPos = 0;
    Object oldHighlight = null;
    Highlighter highlighter;
    Highlighter.HighlightPainter highlightPainter;

    /** Creates new form MyHelpPane */
    public HelpPane(JNX p) {
        parent = p;
        initComponents();
        doc = helpTextPane.getDocument();
        undoStack = new Stack<Integer>();
        redoStack = new Stack<Integer>();
        highlighter = helpTextPane.getHighlighter();
        highlightPainter = new DefaultHighlighter.DefaultHighlightPainter(new Color(200, 255, 200));
        setupHelp();
        setButtons();
        setFocus();
    }

    void setFocus() {
        SwingUtilities.invokeLater(
                new Runnable() {

                    public void run() {
                        findTextField.requestFocus();
                    }
                });
    }

    // help resource related
    void setupHelp() {
        String fn = "help/JNXHelp.html";
        try {
            InputStream is = parent.getClass().getResourceAsStream(fn);
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line + "\n");
            }
            is.close();
            String s = sb.toString();
            s = s.replaceAll("\\(application path\\)", parent.app_path.replaceAll("\\\\", "&#92;"));
            s = s.replaceAll("\\(data path\\)", parent.data_path.replaceAll("\\\\", "&#92;"));
            s = s.replaceAll("\\(configuration path\\)", parent.init_path.replaceAll("\\\\", "&#92;"));
            s = s.replaceAll("\\(log path\\)", parent.log_path.replaceAll("\\\\", "&#92;"));
            //s = s.replaceAll("\\(browse path\\)", parent.browseContentPath.replaceAll("\\\\", "&#92;"));
            s = s.replaceAll("\\(user home directory\\)", parent.user_dir.replaceAll("\\\\", "&#92;"));
            s = s.replaceAll("\\(version\\)", parent.app_version);
            helpTextPane.setText(s);
            helpTextPane.select(0, 0);
            URL url = parent.getClass().getResource(fn).toURI().toURL();
            ((HTMLDocument) helpTextPane.getDocument()).setBase(url);
        } catch (Exception e) {
            System.out.println(e);
        }
    }
    // manageHyperlinks tries to find and launch a browser

    void manageHyperlinks(HyperlinkEvent evt) {
        URL url = evt.getURL();
        if (evt.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            String surl = evt.getURL().toString();
            if (surl.matches("http://.*")) {
                try {
                    java.awt.Desktop.getDesktop().browse(java.net.URI.create(surl));
                } catch (Exception e) {
                    parent.p(e);
                }
            } else { // possibly a bookmark
                if (surl.matches(".*#.*")) {
                    try {
                        pushUndo();
                        helpTextPane.scrollToReference(url.getRef());

                    } catch (Exception e) {
                        System.out.println(e);
                    }
                }
            }
        }
    }

    void undo() {
        if (undoStack.size() > 0) {
            removeOldHighlight();
            pushRedo();
            helpScrollPane.getVerticalScrollBar().setValue(undoStack.pop());
            setButtons();
        } else {
            //CommonCode.beep();
        }
    }

    void redo() {
        if (redoStack.size() > 0) {
            removeOldHighlight();
            pushUndo();
            helpScrollPane.getVerticalScrollBar().setValue(redoStack.pop());
            setButtons();
        } else {
            //CommonCode.beep();
        }
    }

    void pushUndo() {
        undoStack.push(helpScrollPane.getVerticalScrollBar().getValue());
        setButtons();
    }

    void pushRedo() {
        redoStack.push(helpScrollPane.getVerticalScrollBar().getValue());
        setButtons();
    }

    void setButtons() {
        undoButton.setEnabled(undoStack.size() > 0);
        redoButton.setEnabled(redoStack.size() > 0);
    }

    void removeOldHighlight() {
        if (oldHighlight != null) {
            highlighter.removeHighlight(oldHighlight);
            oldHighlight = null;
        }
    }

    void manageHelpTextField(KeyEvent evt) {
        String code = KeyEvent.getKeyText(evt.getKeyCode());
        // if a function key, go to main command switchboard
        if (code.matches("F\\d")) {
            //parent.handleKeyPressed(evt);
        } else {
            try {

                removeOldHighlight();
                doc = helpTextPane.getDocument();
                int len = doc.getLength();
                String content = doc.getText(0, len).toLowerCase();
                String search = findTextField.getText().toLowerCase();
                if (!search.equals(oldSearch)) {
                    oldPos = 0;
                }
                oldSearch = search;
                int p = content.indexOf(search, oldPos);
                if (p == -1) {
                    oldPos = 0;
                    p = content.indexOf(search, oldPos);
                }
                if (p >= 0) { // if found
                    findTextField.setForeground(Color.black);
                    pushUndo();
                    int slen = search.length();
                    Rectangle r = helpTextPane.modelToView(p);
                    // aim for the middle of the screen
                    int pos = r.y - helpScrollPane.getHeight() / 2;
                    // but don't try for the impossible
                    pos = (pos < 0) ? 0 : pos;
                    helpScrollPane.getVerticalScrollBar().setValue(pos);
                    // now highlight the found text in our nonfocused text pane
                    oldHighlight = highlighter.addHighlight(p, p + slen, highlightPainter);
                    oldPos = p + 1; // to find next case
                } else {
                    findTextField.setForeground(Color.red);
                    //CommonCode.beep();
                }
            } catch (Exception e) {
                System.out.println(e);
            }
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

        jPanel1 = new javax.swing.JPanel();
        undoButton = new javax.swing.JButton();
        redoButton = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        findTextField = new javax.swing.JTextField();
        helpScrollPane = new javax.swing.JScrollPane();
        helpTextPane = new javax.swing.JTextPane();

        setLayout(new java.awt.GridBagLayout());

        undoButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jnx/images/go-previous.png"))); // NOI18N
        undoButton.setToolTipText("Go back");
        undoButton.setFocusable(false);
        undoButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                undoButtonMouseClicked(evt);
            }
        });
        jPanel1.add(undoButton);

        redoButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jnx/images/go-next.png"))); // NOI18N
        redoButton.setToolTipText("Go forward");
        redoButton.setFocusable(false);
        redoButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                redoButtonMouseClicked(evt);
            }
        });
        jPanel1.add(redoButton);

        jLabel1.setText("Search:");
        jPanel1.add(jLabel1);

        findTextField.setToolTipText("<html>Quick search: type a search string,<br/>press Enter to find the next case</html>");
        findTextField.setFocusCycleRoot(true);
        findTextField.setMinimumSize(new java.awt.Dimension(150, 27));
        findTextField.setPreferredSize(new java.awt.Dimension(150, 27));
        findTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                findTextFieldKeyReleased(evt);
            }
        });
        jPanel1.add(findTextField);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        add(jPanel1, gridBagConstraints);

        helpScrollPane.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        helpTextPane.setContentType("text/html");
        helpTextPane.setEditable(false);
        helpTextPane.setFocusable(false);
        helpTextPane.addHyperlinkListener(new javax.swing.event.HyperlinkListener() {
            public void hyperlinkUpdate(javax.swing.event.HyperlinkEvent evt) {
                helpTextPaneHyperlinkUpdate(evt);
            }
        });
        helpScrollPane.setViewportView(helpTextPane);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 3);
        add(helpScrollPane, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    private void helpTextPaneHyperlinkUpdate(javax.swing.event.HyperlinkEvent evt) {//GEN-FIRST:event_helpTextPaneHyperlinkUpdate
        // TODO add your handling code here:
        manageHyperlinks(evt);
    }//GEN-LAST:event_helpTextPaneHyperlinkUpdate

    private void undoButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_undoButtonMouseClicked
        // TODO add your handling code here:
        undo();
}//GEN-LAST:event_undoButtonMouseClicked

    private void redoButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_redoButtonMouseClicked
        // TODO add your handling code here:
        redo();
}//GEN-LAST:event_redoButtonMouseClicked

    private void findTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_findTextFieldKeyReleased
        // TODO add your handling code here:
        manageHelpTextField(evt);
    }//GEN-LAST:event_findTextFieldKeyReleased

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField findTextField;
    private javax.swing.JScrollPane helpScrollPane;
    private javax.swing.JTextPane helpTextPane;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JButton redoButton;
    private javax.swing.JButton undoButton;
    // End of variables declaration//GEN-END:variables
}
