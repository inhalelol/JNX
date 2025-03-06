/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jnx;

/**
 *
 * @author lutusp
 */
public class SpectrumDisplayManager {

    JNX parent;
    FFT fft;
    ScopePanel scope_panel;
    double hfactor = 8;
    int array_size = 1000;
    int fft_array_size = 2048;
    int array_mult_factor = 5;
    double array_ratio;
    int rate = 0;
    double[] real_data;
    int src_pointer = 0;
    int dest_pointer = 0;

    public SpectrumDisplayManager(JNX p, ScopePanel sp) {
        scope_panel = sp;
        parent = p;
        fft = new FFT();
        reset_params(array_size);
    }

    private void reset_params(int as) {
        array_size = as;
        // use zero padding to provide smooth
        // frequency scale changes --
        // the fft array size is always equal to
        // the next power of 2 greater than the
        // real-data array size
        int p = (int) ((Math.log(array_size) / Math.log(2)) + 1);
        int fft_as = (int) Math.pow(2, p);
        if (fft_as != fft_array_size) {
            fft_array_size = fft_as;
            fft.initialize(fft_array_size, false);
        }
        array_ratio = (double)fft_array_size/array_size;
        //parent.p("reset params " + array_size + "," + fft_array_size);
        real_data = new double[array_size/2];
        scope_panel.reset_scope_params(array_size/2);
        //parent.p(array_size + "," + rate);
    }

    public void add_data(double v) {
        // full-scale frequency = array_size * hfactor / 2
        rate = (int) (parent.receiver.sample_rate / hfactor);
        scope_panel.set_hcal(array_size * hfactor / 2);
        if (src_pointer >= rate) {
            //parent.p("src_pointer > rate");
            fft.fft1();
            Complex[] data = fft.outputArray();
            int len = real_data.length;
            for (int i = 0; i < len; i++) {
                real_data[i] = data[i].mag();
            }
            scope_panel.write_array(real_data);
            src_pointer = 0;
            update_controls();
        } else {
            dest_pointer = (int) (src_pointer * array_size * array_ratio) / rate;
            fft.set_value(dest_pointer, v);
            src_pointer++;
        }
    }

    private void update_controls() {
        int p = (int) scope_panel.horizontal_control.get_dvalue();
        p = Math.max(1, p);
        p *= array_mult_factor;
        if (p != array_size) {
            reset_params(p);
        }
    }
}
