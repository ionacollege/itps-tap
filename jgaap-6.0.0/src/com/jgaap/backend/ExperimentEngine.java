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
 *
 * Modified by Sean Campbell 2014-2021
 */
package com.jgaap.backend;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.log4j.Logger;

import com.jgaap.JGAAPConstants;
import com.jgaap.generics.*;

/**
 * Experiment Engine This class takes a csv file of experiments and then will
 * run them one after the other and generates result files in the tmp directory
 *
 * @author Mike Ryan
 */
public class ExperimentEngine {

	private static StringBuffer results = new StringBuffer();

	static Logger logger = Logger.getLogger(ExperimentEngine.class);

	private static final int workers = 2;

	/**
	 * This method generates unique file names and a directory structure to save
	 * the results of an experiment run
	 *
	 * @param canons
	 *            the canonicizors used
	 * @param event
	 *            the event used
	 * @param analysis
	 *            the analysis method or distance function used
	 * @param experimentName
	 *            the given name of this experiment specified on the top line of
	 *            the experiment csv file
	 * @param number
	 *            the identifier given to this experiment
	 * @return the location of where the file will be written
	 */
	public static String fileNameGen(List<String> canons, String event,
			String[] eventCullers, String analysis, String experimentName,
			String number) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date date = new Date();
		Iterator<String> iterator = canons.iterator();
		StringBuilder canonNameBuilder = new StringBuilder();
		while (iterator.hasNext()) {
			canonNameBuilder.append(iterator.next().trim()).append(" ");
		}
		String canonName = canonNameBuilder.toString().trim();
		if(canonName.isEmpty())
			canonName = "none";
		iterator = Arrays.asList(eventCullers).iterator();
		StringBuilder cullerNameBuilder = new StringBuilder();
		while (iterator.hasNext()) {
			cullerNameBuilder.append(iterator.next().trim()).append(" ");
		}
		String cullerName = cullerNameBuilder.toString().trim();
		if(cullerName.isEmpty())
			cullerName = "none";
		String path = JGAAPConstants.JGAAP_TMPDIR
				+ canonName.replace("/", "\\/") + "/"
				+ event.trim().replace("/", "\\/") + "/"
				+ cullerName.replace("/", "\\/") + "/"
				+ analysis.trim().replace("/", "\\/") + "/";
		path = path.replaceAll(":", "-");
		File file = new File(path);
		boolean newDirs = file.mkdirs();
		if (!newDirs) {
			; // Nothing (check added to satisfy static analysis / show we are
				// aware of this)
		}
		return (path + experimentName + number + dateFormat.format(date) + ".txt");
	}

	/**
	 * This method will iterate a the rows of a csv file of experiments running
	 * jgaap on each one and then generate a results file for it
	 *
	 * @param listPath
	 *            the location of the csv file of experiments
	 */

	public static void runExperiment(String listPath, boolean isLeaveOneOut) throws Exception
	{
		try {
			if (listPath.startsWith(JGAAPConstants.JGAAP_RESOURCE_PACKAGE)) {
				runExperiment(CSVIO.readCSV(com.jgaap.JGAAP.class.getResourceAsStream(listPath)), isLeaveOneOut);
			} else {
				runExperiment(CSVIO.readCSV(listPath), isLeaveOneOut);
			}
		} catch (IOException e) {
			logger.fatal("Problem processing experiment file: " + listPath, e);
			authorquicktest.Utils.output("Problem processing experiment file: " + listPath + ", " + e);
			throw new IOException("Unable to run experiment. There was a problem processing the experiment file: " + e);
		}

	}

	public static void runExperiment(List<List<String>> experimentTable, boolean isLeaveOneOut) throws Exception
	{
		String documentsPath = experimentTable.get(1).get(0).trim();
		List<List<String>> documentList;
		try {
			if (documentsPath.startsWith(JGAAPConstants.JGAAP_RESOURCE_PACKAGE)) {
				documentList = CSVIO.readCSV(com.jgaap.JGAAP.class.getResourceAsStream(documentsPath));
			} else {
				documentList = CSVIO.readCSV(documentsPath);
			}
			runExperiment(experimentTable, documentList, isLeaveOneOut);
		} catch (IOException e) {
			logger.fatal("Unable to read documents file: " + e);
			authorquicktest.Utils.output("Unable to read documents file: " + e);
			throw new IOException("Unable to run experiment. There was a problem reading the documents file: " + e);
		}
	}

	public static void runExperiment(List<List<String>> experimentTable, List<List<String>> documentList, boolean isLeaveOneOut) throws Exception
	{
		// If you're rewriting this, it used to use ExperimentExecutor, which I believe was used for multithreading.
		// For reference, you can look at an older version.
		final String experimentName = experimentTable.get(0).get(0);

		List<String> canonicizers = new ArrayList<String>();
		List<String> eventDrivers = new ArrayList<String>();
		List<String> eventCullers = new ArrayList<String>();
		List<String> analysisMethods = new ArrayList<String>();

		List<List<String>> modifiedExpTable = experimentTable.subList(2, 6);
		authorquicktest.Utils.getExperimentComponentsFromExperimentTable(modifiedExpTable, eventDrivers, analysisMethods, eventCullers, canonicizers);

		//CompoundExperiment compExperiment = new CompoundExperiment(canonicizers, eventDrivers, eventCullers, analysisMethods, documentList, experimentName, isLeaveOneOut);
		//compExperiment.runExperiment();
	}

}
