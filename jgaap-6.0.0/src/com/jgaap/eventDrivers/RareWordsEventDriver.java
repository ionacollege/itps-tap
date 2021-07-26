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
package com.jgaap.eventDrivers;

import com.jgaap.backend.EventDriverFactory;
import com.jgaap.generics.Document;
import com.jgaap.generics.Event;
import com.jgaap.generics.EventDriver;
import com.jgaap.generics.EventGenerationException;
import com.jgaap.generics.EventSet;
import com.jgaap.generics.EventHistogram;

/**
 * This event set is all events occurring only once of an underlying event model
 * * (parameterized as underlyingevents)
 * 
 * @author Patrick Juola
 * @since 5.0
 **/
public class RareWordsEventDriver extends EventDriver {

	public RareWordsEventDriver() {
		addParams("M", "M", "1", new String[] { "1", "2", "3", "4", "5", "6",
				"7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17",
				"18", "19", "20", "21", "22", "23", "24", "25", "26", "27",
				"28", "29", "30", "31", "32", "33", "34", "35", "36", "37",
				"38", "39", "40", "41", "42", "43", "44", "45", "46", "47",
				"48", "49", "50" }, false);
		addParams("N", "N", "2", new String[] { "1", "2", "3", "4", "5", "6",
				"7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17",
				"18", "19", "20", "21", "22", "23", "24", "25", "26", "27",
				"28", "29", "30", "31", "32", "33", "34", "35", "36", "37",
				"38", "39", "40", "41", "42", "43", "44", "45", "46", "47",
				"48", "49", "50" }, false);
	}

	@Override
	public String displayName() {
		return "Rare Words";
	}

	@Override
	public String tooltipText() {
		return "Rare words such as Words appearing only once or twice per document";
	}

	@Override
	public boolean showInGUI() {
		return true;
	}

	/** Underlying EventDriver from which Events are drawn. */
	public EventDriver underlyingevents = new NaiveWordEventDriver();
	public int M = 1, N = 2;

	@Override
	public EventSet createEventSet(Document ds) throws EventGenerationException {

		String param;
		if (!(param = (getParameter("underlyingEvents"))).equals("")) {
			try {
				setEvents(EventDriverFactory.getEventDriver(param));
			} catch (Exception e) {
				System.out.println("Error: cannot create EventDriver " + param);
				System.out.println(" -- Using NaiveWordEventDriver");
				setEvents(new NaiveWordEventDriver());
			}
		}

		// lots of error checking
		if (!(param = (getParameter("N"))).equals("")) {
			try {
				int value = Integer.parseInt(param);
				setN(value);
			} catch (NumberFormatException e) {
				System.out.println("Warning: cannot parse N(upper bound):"
						+ param + " as int");
				System.out.println(" -- Using default value (3)");
				setN(3);
			}
		}
		if (!(param = (getParameter("M"))).equals("")) {
			try {
				int value = Integer.parseInt(param);
				setM(value);
			} catch (NumberFormatException e) {
				System.out.println("Warning: cannot parse M(lower bound):"
						+ param + " as int");
				System.out.println(" -- Using default value (2)");
				setM(2);
			}
		}
		EventSet es = underlyingevents.createEventSet(ds);
		EventSet newEs = new EventSet();
		newEs.setAuthor(es.getAuthor());
		newEs.setNewEventSetID(es.getAuthor());

		/**
		 * Create histogram with all events from stream
		 */
		EventHistogram hist = es.getHistogram();

		/**
		 * Re-search event stream for rare events as measured by histogram
		 * count. If count is 1, it's a hapax, etc.
		 */
		System.out.println("M = " + M + "; N = " + N);
		for (Event e : es) {
			int n = hist.getAbsoluteFrequency(e);
			System.out.println(e.toString() + " " + n);
			if (n >= M && n <= N)
				newEs.addEvent(e);
		}
		return newEs;
	}

	/**
	 * Get EventDriver for relevant Events *
	 * 
	 * @return underlying EventDriver
	 */
	public EventDriver getEvents() {
		return underlyingevents;
	}

	/**
	 * Set EventDriver for relevant Events *
	 * 
	 * @param underlyingevents
	 *            underlying EventDriver
	 */
	public void setEvents(EventDriver underlyingevents) {
		this.underlyingevents = underlyingevents;
	}

	public int getM() {
		return M;
	}

	public void setM(int M) {
		this.M = M;
	}

	public int getN() {
		return N;
	}

	public void setN(int N) {
		this.N = N;
	}

}
