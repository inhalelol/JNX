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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author lutusp
 */
final public class Complex {

    double re = 0;
    double im = 0;

    public Complex(double re, double im) {
        this.re = re;
        this.im = im;
    }

    public Complex(Complex comp) {
        this.re = comp.re;
        this.im = comp.im;
    }

    public Complex() {
    }

    public void mult(double v) {
        re *= v;
        im *= v;
    }
    
    public void mult(Complex v) {
        re *= v.re;
        im *= v.im;
    }

    public void add(Complex v) {
        re += v.re;
        im += v.im;
    }

    public void sub(Complex v) {
        re -= v.re;
        im -= v.im;
    }

    public void assign(Complex comp) {
        re = comp.re;
        im = comp.im;
    }

    public double mag() {
       return Math.sqrt(re*re+im*im);
    }
}
