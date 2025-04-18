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

import com.jgaap.JGAAPConstants;
import com.jgaap.generics.Document;
import com.jgaap.generics.EventDriver;
import com.jgaap.generics.EventGenerationException;
import com.jgaap.generics.EventSet;


/**
 * 
 */
public class SWAaron041114EventDriver extends EventDriver {


    @Override
    public String displayName(){
    	return "SWAaron041114";
    }
    
    @Override
    public String tooltipText(){
    	return "Commonly used words in the corpus";
    }

    @Override
    public String longDescription(){
    	return "Special Words from Aaron's email on April 11, 2014.";
    }
    
    @Override
    public boolean showInGUI(){
    	return true;
    }

    /** Static field for efficiency */
    // private static EventDriver e;
    // Peter Jan 21 2010 ^ if this is static, then why are we setting it in the constructor?
    private EventDriver e;

    /** Default constructor. Sets parameters for WhiteList */
    // Peter Jan 21 2010 - this needs to be public for autopopulator
    public SWAaron041114EventDriver() {
        e = new WhiteListEventDriver();
        e.setParameter("underlyingEvents", "Words");
        e.setParameter("filename", JGAAPConstants.JGAAP_RESOURCE_PACKAGE
                + "SWAaron041114.dat");
    }

    /** Creates EventSet using prepositions word list 
     * @throws EventGenerationException */
    @Override
    public EventSet createEventSet(Document ds) throws EventGenerationException {
        return e.createEventSet(ds);
    }

}
