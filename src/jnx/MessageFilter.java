/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jnx;

/**
 *
 * @author lutusp
 */
public class MessageFilter implements ControlInterface {

    String value = "";

    public MessageFilter() {
        // default to accepting all messages
        value = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    }

    public void set_value(String s) {
        value = s;
    }

    public boolean accept(String s) {
        return value.indexOf(s) != -1;
    }

    public String get_value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
