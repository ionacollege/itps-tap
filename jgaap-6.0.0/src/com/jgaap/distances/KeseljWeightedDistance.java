/*
 * JGAAP -- a graphical program for stylometric authorship attribution
 * Copyright (C) 2009,2011 by Patrick Juola
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
/**
 **/
package com.jgaap.distances;

import com.jgaap.generics.DistanceFunction;
import com.jgaap.generics.Event;
import com.jgaap.generics.EventHistogram;
import com.jgaap.generics.EventSet;

/**
 * Histogram distance as weighted by Keselj (2003).
 * N.b. this was the AAAC 2004 winner when used with "common N-grams"
 * 
 * @author Juola
 * @version 4.2
 */
public class KeseljWeightedDistance extends DistanceFunction {
	public String displayName() {
		return "Keselj-weighted Distance";
	}

	public String tooltipText() {
		return "Histogram Distance (L2 Norm) with Keselj-weighting based on overall frequency";
	}

	public boolean showInGUI() {
		return true;
	}

	/**
	 * Returns Keselj-weighted distance between event sets es1 and es2.
	 * 
	 * @param es1
	 *            The first EventSet
	 * @param es2
	 *            The second EventSet
	 * @return the (Euclidean) histogram distance between them
	 */

	@Override
	public double distance(EventSet es1, EventSet es2) {
		EventHistogram h1 = es1.getHistogram();
		EventHistogram h2 = es2.getHistogram();
		double distance = 0.0;

		for (Event event : h1) {
			double fa, fx;

			fa = h1.getRelativeFrequency(event);
			fx = h2.getRelativeFrequency(event);

			// note denominator for Keselj-weighting
			distance += ( (fa-fx)*(fa-fx) )/
				    ( (fa+fx)*(fa+fx) );
			// also note what happens if fx or fa == 0

			//System.out.println("fa is " + fa);
			//System.out.println("fx is " + fx);
			//System.out.println("distance is " + distance);
		}

		for (Event event : h2) {

			//  note that if fx == 0, the formula above == 1

			if (h1.getAbsoluteFrequency(event) == 0) {
				// so we simplify a bit
				distance += 1.0;
			}
		}
		//System.out.println("Calculation complete");
		return distance;
	}
}
