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
package com.jgaap.eventDrivers;

import static org.junit.Assert.*;

import java.util.Vector;

import org.junit.Test;

import com.jgaap.generics.Document;
import com.jgaap.generics.Event;
import com.jgaap.generics.EventGenerationException;
import com.jgaap.generics.EventSet;

/**
 * @author student
 *
 */
public class PorterStemmerWithIrregularEventDriverTest {

	/**
	 * Test method for {@link com.jgaap.eventDrivers.PorterStemmerWithIrregularEventDriver#createEventSet(com.jgaap.generics.Document)}.
	 * @throws EventGenerationException 
	 */
	@Test
	public void testCreateEventSet() throws EventGenerationException {
		
		/* test case 1 -- no punctuation */
		Document doc = new Document();
		doc.readStringText(
"tests Tested TESTING TeSt " +
"867-5309 " +
"a aaron abaissiez abandon abandoned abase abash abate abated abatement "+
"abatements abates abbess abbey abbeys abbominable abbot abbots abbreviated "+
"abed abel aberga abergavenny abet abetting abhominable abhor abhorr abhorred "+
"abhorring abhors abhorson abide abides abilities ability abject abjectly "+
"abjects abjur abjure able abler aboard abode aboded abodements aboding "+
"abominable abominably abominations abortive abortives abound abounding "+
"about above abr abraham abram abreast abridg abridge abridged abridgment "+
"abroach abroad abrogate abrook abrupt abruption abruptly absence absent "+
"absey absolute absolutely absolv absolver abstains abstemious abstinence "+
"abstract absurd absyrtus abundance abundant abundantly abus abuse abused "+
"abuser abuses abusing abutting aby abysm ac academe academes accent accents "+
"accept acceptable acceptance accepted accepts access accessary accessible "+
"accidence accident accidental accidentally accidents accite accited accites "+
"acclamations accommodate accommodated accommodation accommodations "+
"accommodo accompanied accompany accompanying accomplices accomplish "+
"accomplished accomplishing accomplishment accompt accord accordant accorded "+
"accordeth according accordingly accords accost accosted account accountant "+
"accounted accounts accoutred accoutrement accoutrements accrue alumni caught");


		EventSet sampleEventSet = new PorterStemmerWithIrregularEventDriver().createEventSet(doc);
		EventSet expectedEventSet = new EventSet();
		Vector<Event> tmp = new Vector<Event>();

		tmp.add(new Event("test"));
		tmp.add(new Event("Test"));
		tmp.add(new Event("TEST"));
		tmp.add(new Event("TeSt"));

		tmp.add(new Event("867-5309"));

		tmp.add(new Event("a"));
		tmp.add(new Event("aaron"));
		tmp.add(new Event("abaissiez"));
		tmp.add(new Event("abandon"));
		tmp.add(new Event("abandon"));
		tmp.add(new Event("abas"));
		tmp.add(new Event("abash"));
		tmp.add(new Event("abat"));
		tmp.add(new Event("abat"));
		tmp.add(new Event("abat"));
		tmp.add(new Event("abat"));
		tmp.add(new Event("abat"));
		tmp.add(new Event("abbess"));
		tmp.add(new Event("abbei"));
		tmp.add(new Event("abbei"));
		tmp.add(new Event("abbomin"));
		tmp.add(new Event("abbot"));
		tmp.add(new Event("abbot"));
		tmp.add(new Event("abbrevi"));
		tmp.add(new Event("ab"));
		tmp.add(new Event("abel"));
		tmp.add(new Event("aberga"));
		tmp.add(new Event("abergavenni"));
		tmp.add(new Event("abet"));
		tmp.add(new Event("abet"));
		tmp.add(new Event("abhomin"));
		tmp.add(new Event("abhor"));
		tmp.add(new Event("abhorr"));
		tmp.add(new Event("abhor"));
		tmp.add(new Event("abhor"));
		tmp.add(new Event("abhor"));
		tmp.add(new Event("abhorson"));
		tmp.add(new Event("abid"));
		tmp.add(new Event("abid"));
		tmp.add(new Event("abil"));
		tmp.add(new Event("abil"));
		tmp.add(new Event("abject"));
		tmp.add(new Event("abjectli"));
		tmp.add(new Event("abject"));
		tmp.add(new Event("abjur"));
		tmp.add(new Event("abjur"));
		tmp.add(new Event("abl"));
		tmp.add(new Event("abler"));
		tmp.add(new Event("aboard"));
		tmp.add(new Event("abod"));
		tmp.add(new Event("abod"));
		tmp.add(new Event("abod"));
		tmp.add(new Event("abod"));
		tmp.add(new Event("abomin"));
		tmp.add(new Event("abomin"));
		tmp.add(new Event("abomin"));
		tmp.add(new Event("abort"));
		tmp.add(new Event("abort"));
		tmp.add(new Event("abound"));
		tmp.add(new Event("abound"));
		tmp.add(new Event("about"));
		tmp.add(new Event("abov"));
		tmp.add(new Event("abr"));
		tmp.add(new Event("abraham"));
		tmp.add(new Event("abram"));
		tmp.add(new Event("abreast"));
		tmp.add(new Event("abridg"));
		tmp.add(new Event("abridg"));
		tmp.add(new Event("abridg"));
		tmp.add(new Event("abridg"));
		tmp.add(new Event("abroach"));
		tmp.add(new Event("abroad"));
		tmp.add(new Event("abrog"));
		tmp.add(new Event("abrook"));
		tmp.add(new Event("abrupt"));
		tmp.add(new Event("abrupt"));
		tmp.add(new Event("abruptli"));
		tmp.add(new Event("absenc"));
		tmp.add(new Event("absent"));
		tmp.add(new Event("absei"));
		tmp.add(new Event("absolut"));
		tmp.add(new Event("absolut"));
		tmp.add(new Event("absolv"));
		tmp.add(new Event("absolv"));
		tmp.add(new Event("abstain"));
		tmp.add(new Event("abstemi"));
		tmp.add(new Event("abstin"));
		tmp.add(new Event("abstract"));
		tmp.add(new Event("absurd"));
		tmp.add(new Event("absyrtu"));
		tmp.add(new Event("abund"));
		tmp.add(new Event("abund"));
		tmp.add(new Event("abundantli"));
		tmp.add(new Event("abu"));
		tmp.add(new Event("abus"));
		tmp.add(new Event("abus"));
		tmp.add(new Event("abus"));
		tmp.add(new Event("abus"));
		tmp.add(new Event("abus"));
		tmp.add(new Event("abut"));
		tmp.add(new Event("abi"));
		tmp.add(new Event("abysm"));
		tmp.add(new Event("ac"));
		tmp.add(new Event("academ"));
		tmp.add(new Event("academ"));
		tmp.add(new Event("accent"));
		tmp.add(new Event("accent"));
		tmp.add(new Event("accept"));
		tmp.add(new Event("accept"));
		tmp.add(new Event("accept"));
		tmp.add(new Event("accept"));
		tmp.add(new Event("accept"));
		tmp.add(new Event("access"));
		tmp.add(new Event("accessari"));
		tmp.add(new Event("access"));
		tmp.add(new Event("accid"));
		tmp.add(new Event("accid"));
		tmp.add(new Event("accident"));
		tmp.add(new Event("accident"));
		tmp.add(new Event("accid"));
		tmp.add(new Event("accit"));
		tmp.add(new Event("accit"));
		tmp.add(new Event("accit"));
		tmp.add(new Event("acclam"));
		tmp.add(new Event("accommod"));
		tmp.add(new Event("accommod"));
		tmp.add(new Event("accommod"));
		tmp.add(new Event("accommod"));
		tmp.add(new Event("accommodo"));
		tmp.add(new Event("accompani"));
		tmp.add(new Event("accompani"));
		tmp.add(new Event("accompani"));
		tmp.add(new Event("accomplic"));
		tmp.add(new Event("accomplish"));
		tmp.add(new Event("accomplish"));
		tmp.add(new Event("accomplish"));
		tmp.add(new Event("accomplish"));
		tmp.add(new Event("accompt"));
		tmp.add(new Event("accord"));
		tmp.add(new Event("accord"));
		tmp.add(new Event("accord"));
		tmp.add(new Event("accordeth"));
		tmp.add(new Event("accord"));
		tmp.add(new Event("accordingli"));
		tmp.add(new Event("accord"));
		tmp.add(new Event("accost"));
		tmp.add(new Event("accost"));
		tmp.add(new Event("account"));
		tmp.add(new Event("account"));
		tmp.add(new Event("account"));
		tmp.add(new Event("account"));
		tmp.add(new Event("accoutr"));
		tmp.add(new Event("accoutr"));
		tmp.add(new Event("accoutr"));
		tmp.add(new Event("accru"));
		tmp.add(new Event("alumnus"));
		tmp.add(new Event("catch"));
		expectedEventSet.addEvents(tmp);

		//System.out.println(expectedEventSet.toString());
		//System.out.println(sampleEventSet.toString());
		assertTrue(expectedEventSet.equals(sampleEventSet));
		
	}

}
