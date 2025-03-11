package io.github.inhalelol.jnx;

import com.fazecast.jSerialComm.SerialPort;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author inhalelol
 */
public class SerialComm {
    JNX parent;
    int port_index;
    int baud;
    List<String> names;
    SerialPort port;

    public SerialComm(JNX p) {
        parent = p;
        names = get_ports_names();
        this.print_ports(this.get_ports());
    }

    private SerialPort[] get_ports() {
        return SerialPort.getCommPorts();
    }

    public void closePort() {
        if (this.port != null) {
            System.out.println("Close " + this.port.getSystemPortName());
            this.port.closePort();
        }
    }

    public java.util.List<String> get_ports_names() {
        List<String> names = new ArrayList<>();
        SerialPort[] ports = this.get_ports();
        for (SerialPort port : ports) {
            names.add(port.getSystemPortName());
        }
        return names;
    }

    private void print_ports(SerialPort[] ports) {
        System.out.println("Available ports:");
        for (SerialPort port : ports) {
            System.out.println(port.getSystemPortName());
        }
    }

    private void open_port(int index, int baud) {
        this.port = SerialPort.getCommPorts()[index];
        this.port.setComPortParameters(baud, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
        this.port.openPort();
        System.out.println("Open " + this.port.getSystemPortName() + " " + this.port.getBaudRate());
    }

    public void set_port(String name) {
        int index = names.indexOf(name);
        int baud = Integer.parseInt(this.parent.serial_baud_selection.get_value());

        if (index != this.port_index | this.port == null | baud != this.baud) {
            closePort();
            open_port(index, baud);
            this.port_index = index;
            this.baud = baud;
        }
    }

    public void write_serial(String s) {
        set_port(parent.serial_port_selection.get_value());
        byte[] buf = s.getBytes();
        int bytesWritten = this.port.writeBytes(buf, buf.length);
        // this.port.flushIOBuffers();
    }
}
