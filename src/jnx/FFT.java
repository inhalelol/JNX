/***************************************************************************
 *   Copyright (C) 2011
 * by Paul Lutus                                      *
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

import java.util.*;
/**
 *
 * @author lutusp
 */
final public class FFT {

    private int size = 0;
    private boolean valid = false;
    private Complex[] in_data = null,  out_data = null;
    private List<FFTPrecalc> fftPrecalc;
    FFTPrecalc tcalc;
    private final double pi2 = Math.PI * 2.0;
    private double scale;
    private double fft_pi2;
    private boolean inverse;

    public FFT() {
    }

    boolean test_pwr2(int n) {
        return (n >= 2 && ((n & (n - 1)) == 0));
    }

    int rev_bits(int index, int size) {
        int rev = 0;
        for (; size > 1; size >>= 1) {
            rev = (rev << 1) | (index & 1);
            index >>= 1;
        }
        return rev;
    }

    public void initialize(int n, boolean inverse) {
        this.inverse = inverse;
        Complex tc;
        fft_pi2 = (inverse) ? -pi2 : pi2;
        try {
            if (size != n) {
                if (!test_pwr2(n)) {
                    throw (new Exception("Error: array size is not a power of 2\n"));
                } else {
                    size = n;
                    valid = true;
                    in_data = new Complex[n];
                    out_data = new Complex[n];
                    scale = 1.0 / size;
                    int rb;
                    for (int i = 0; i < n; i++) {
                        tc = new Complex();
                        rb = rev_bits(i, n);
                        in_data[i] = tc;
                        out_data[rb] = tc;
                    }
                    fftPrecalc = new ArrayList<FFTPrecalc>();
                    
                    int imax = 1;
                    while(imax < size) {
                        tcalc = new FFTPrecalc(imax,fft_pi2);
                        fftPrecalc.add(tcalc);
                        imax = tcalc.istep;
                    }
                }
            }
            
        } catch (Exception e) {
            System.out.println(getClass().getName() + ": Error: " + e);
        }
    }

    void resize(int n, boolean inverse) {
        initialize(n, inverse);
    }

    boolean valid() {
        return valid;
    }

    int size() {
        return size;
    }

    public Complex[] inputArray() {
        return in_data;
    }

    public Complex[] outputArray() {
        return out_data;
    }

    public void set_value(int i,double v) {
        in_data[i].re = v;
    }

    void fft1() {
        if (valid && out_data != null) {
            int imax, istep, m, i, j, k;
            double wtemp, wr, wpr, wpi, wi, theta;
            Complex ac, bc;
            Complex tc = new Complex();
            FFTPrecalc t;
            Iterator<FFTPrecalc> it = fftPrecalc.iterator();
            // Danielson-Lanzcos method
            // with some precomputation
            while (it.hasNext()) {
                tcalc = it.next();
                imax = tcalc.imax;
                istep = tcalc.istep;
                wpr = tcalc.wpr;
                wpi = tcalc.wpi;
                wr = 1.0;
                wi = 0.0;
                for (m = 0; m < imax; ++m) {
                    for (i = m; i < size; i += istep) {
                        j = i + imax;
                        ac = out_data[j];
                        bc = out_data[i];
                        tc.re = wr * ac.re - wi * ac.im;
                        tc.im = wr * ac.im + wi * ac.re;
                        ac.re = bc.re - tc.re;
                        ac.im = bc.im - tc.im;
                        bc.add(tc);
                    }
                    wr = (wtemp = wr) * wpr - wi * wpi + wr;
                    wi = wi * wpr + wtemp * wpi + wi;
                }
            }
            if (!inverse) {
                for (k = 0; k < size; k++) {
                    out_data[k].mult(scale);
                }
            }
        }
    }
}
