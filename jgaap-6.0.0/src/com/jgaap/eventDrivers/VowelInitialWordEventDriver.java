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


/**
 * This event set is all "words" (NaiveWordEventDriver) beginning with vowels
 * "aeiouAEIOU"; extension may be necessary to include non-English vowels or
 * characters with diacritical marks like Danish ae digraph or German o-umlaut
 * 
 * @since 4.1
 **/
public class VowelInitialWordEventDriver extends EventDriver {

	@Override
	public String displayName() {
		return "Vowel-initial Words";
	}

	@Override
	public String tooltipText() {
		return "Words beginning with A, E, I, O, U (or lowercase equivalent)";
	}

	@Override
	public boolean showInGUI() {
		return true;
	}

	/** Underlying EventDriver from which Events are drawn. */
	public EventDriver underlyingevents = new NaiveWordEventDriver();

	@Override
	public EventSet createEventSet(Document ds) throws EventGenerationException {

		String vowels = "aeiouyAEIOUY";

		// Extract local field values based on parameter settings
		String param;

		if (!(param = (getParameter("underlyingEvents"))).equals("")) {
			try {
				setEvents(EventDriverFactory.getEventDriver(param));
			} catch (Exception e) {
				// System.out.println("Error: cannot create EventDriver " +
				// param);
				// System.out.println(" -- Using NaiveWordEventDriver");
				setEvents(new NaiveWordEventDriver());
			}
		}
		EventSet es = underlyingevents.createEventSet(ds);
		EventSet newEs = new EventSet();
		newEs.setAuthor(es.getAuthor());
		newEs.setNewEventSetID(es.getAuthor());
		String s;

		/**
		 * Check initial leter of each event and accept if vowel
		 */
		for (Event e : es) {
			s = e.toString();
			if (vowels.indexOf(s.charAt(0)) != -1)
				// should we clone event before adding? PMJ
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

}
