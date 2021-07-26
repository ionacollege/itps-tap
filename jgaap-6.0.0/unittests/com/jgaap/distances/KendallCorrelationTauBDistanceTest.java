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
 * 
 */
package com.jgaap.distances;

import static org.junit.Assert.*;

import java.util.Vector;

import org.junit.Test;

import com.jgaap.generics.Event;
import com.jgaap.generics.EventSet;

/**
 * @author michael
 *
 */
public class KendallCorrelationTauBDistanceTest {

	/**
	 * Test method for {@link com.jgaap.distances.KendallCorrelationDistance#distance(com.jgaap.generics.EventSet, com.jgaap.generics.EventSet)}.
	 */
	@Test
	public void testDistance() {
		EventSet es1 = new EventSet();
		EventSet es2 = new EventSet();
		Vector<Event> test1 = new Vector<Event>();
		test1.add(new Event("alpha"));
		test1.add(new Event("alpha"));
		test1.add(new Event("alpha"));
		test1.add(new Event("alpha"));
		test1.add(new Event("beta"));
		test1.add(new Event("beta"));
		test1.add(new Event("beta"));
		test1.add(new Event("gamma"));
		test1.add(new Event("gamma"));
		test1.add(new Event("delta"));
		es1.addEvents(test1);
		es2.addEvents(test1);
		double result = new KendallCorrelationTauBDistance().distance(es1, es2);
		System.out.println(result);
		assertTrue( result == 0);

		es1=new EventSet();
		es2=new EventSet();
		test1 = new Vector<Event>();
		Vector<Event> test2 = new Vector<Event>();
		test1.add(new Event("A"));
		test1.add(new Event("A"));
		test1.add(new Event("A"));
		test1.add(new Event("A"));
		test1.add(new Event("A"));
		test1.add(new Event("B"));
		test1.add(new Event("B"));
		test1.add(new Event("B"));
		test1.add(new Event("B"));
		test1.add(new Event("C"));
		test1.add(new Event("C"));
		test1.add(new Event("C"));
		test1.add(new Event("D"));
		test1.add(new Event("D"));
		test1.add(new Event("E"));

		test2.add(new Event("A"));
		test2.add(new Event("A"));
		test2.add(new Event("A"));
		test2.add(new Event("B"));
		test2.add(new Event("C"));
		test2.add(new Event("C"));
		test2.add(new Event("C"));
		test2.add(new Event("C"));
		test2.add(new Event("D"));
		test2.add(new Event("D"));
		test2.add(new Event("D"));
		test2.add(new Event("D"));
		test2.add(new Event("D"));
		test2.add(new Event("E"));
		test2.add(new Event("E"));

		es1.addEvents(test1);
		es2.addEvents(test2);
		result = new KendallCorrelationTauBDistance().distance(es1, es2);
		System.out.println(result);
		assertTrue(DistanceTestHelper.inRange(result, 1.2, 0.0000000001));

		es1=new EventSet();
		es2=new EventSet();
		test1 = new Vector<Event>();
		test2 = new Vector<Event>();

		test1.add(new Event("A"));
		test1.add(new Event("A"));
		test1.add(new Event("A"));
		test1.add(new Event("B"));
		test1.add(new Event("B"));
		test1.add(new Event("C"));

		test2.add(new Event("C"));
		test2.add(new Event("C"));
		test2.add(new Event("C"));
		test2.add(new Event("B"));
		test2.add(new Event("B"));
		test2.add(new Event("A"));

		es1.addEvents(test1);
		es2.addEvents(test2);
		result = new KendallCorrelationTauBDistance().distance(es1, es2);
		System.out.println(result);
		assertTrue(DistanceTestHelper.inRange(result, 2.0, 0.0000000001));
	}

}
