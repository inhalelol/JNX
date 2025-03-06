/***************************************************************************
 *   Copyright (C) 20011 by Paul Lutus                                      *
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
final public class FFTPrecalc {

    int imax;
    int istep;
    double theta;
    double wtemp;
    double wpr, wpi;
    public FFTPrecalc(int imax, double sp) {
        this.imax = imax;
        istep = imax << 1;
        theta = sp / istep;
        wtemp = Math.sin(0.5 * theta);
        wpr = -2.0 * wtemp * wtemp;
        wpi = Math.sin(theta);
    }
}
