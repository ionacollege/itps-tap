/*
 * JGAAP -- a graphical program for stylometric authorship attribution
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
 * This file authored by Sean Campbell 2014-2021
 *
 * This is an experimental package and will be cleaned up and published with
 * a more recent version of the JGAAP software.
 */
package authorquicktest;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.jgaap.generics.Document;

public class AuthorQuickTest
{
	private static final String PROGRAM_TITLE = "JGAAP Author Quick Test";
	// Leave as lower case because in the future it might be a setting that can be changed.
	private static final String defaultPackagePath = com.jgaap.JGAAPConstants.JGAAP_RESOURCE_PACKAGE + "all.txt";

	private static Logger logger = Logger.getLogger("com.jgaap");

	private static AuthorQuickTestFrame quickTestFrame;
	private static CompoundTestExperiment experiment;
	private static TestResultsParser parser;

	public static void main(String[] args)
	{
		// Ask for memory
		int requestedMemoryMb = 4096;
		if (args.length > 0) {
			try {
				requestedMemoryMb = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {}
		}
		Utils.requestMemory(requestedMemoryMb);

		BasicConfigurator.configure();
    	logger.setLevel(Level.INFO);

    	// Get the parser ready.
		parser = new TestResultsParser();
		//parser.setWeightMethodThreshold(0.5);

		// Set up the GUI for the program.
    	quickTestFrame = new AuthorQuickTestFrame();
		quickTestFrame.setTitle(PROGRAM_TITLE);
		quickTestFrame.setSize(700, 500);
		// Center the frame on the screen.
		quickTestFrame.setLocationRelativeTo(null);
		quickTestFrame.setVisible(true);

    	long maxMemoryMb = Runtime.getRuntime().maxMemory() / (long)Math.pow(2, 20);
    	quickTestFrame.setStatus("Memory available: " + maxMemoryMb + "MB");

		try {
			experiment = new CompoundTestExperiment(quickTestFrame);
			loadDefaultPackage();
		} catch (Exception e) {
			quickTestFrame.setStatus("Problem creating experiment: " + e);
		}
	}

	/**
	 * Interface between the GUI (AuthorQuickTestFrame) and the backend (ExperimentEngine).
	 * @param testFileName
	 */
	public static void testFiles(List<String> testFileNames)
	{
		int numFeatures = 1;

		// Load test files.
		List<Document> testDocs = new ArrayList<Document>(testFileNames.size());
		for (String testFile : testFileNames) {
			Document doc = new Document(testFile, "");
			try {
				doc.load();
			} catch (Exception e) {
				quickTestFrame.setStatus("Problem loading test file. Unable to continue.");
				return;
			}
			testDocs.add(doc);
		}
		experiment.setTestDocuments(testDocs);


		// Parse features so we can do one at a time.
		List<String> features = new ArrayList<String>();
		try {
			Utils.getExperimentComponentsFromExperimentTable(experiment.getExperimentTable(),
					features, new ArrayList<String>(),
					new ArrayList<String>(), new ArrayList<String>());
		} catch (Exception e) {
			quickTestFrame.setStatus("Problem getting feature list from experiment: " + e);
		}


		// Test a limited number of features (determined by numFeatures)
		// to prevent the program from crashing because it's out of memory.
		// Keep track of time elapsed.
		long startTime = System.currentTimeMillis();
		//for (int i = 0; i < 1; i++) {
		for (int i = 0; i < features.size(); i += numFeatures) {
			experiment.setFeatureRange(i, i+numFeatures-1);
			if (numFeatures > 1)
				quickTestFrame.setStatus("Running features " + (i+1) + " to " + (i+numFeatures) + " of " + features.size() + "..." + features.get(i));
			else
				quickTestFrame.setStatus("Running feature " + (i+1) + " of " + features.size() + "..." + features.subList(i, Math.min(i+numFeatures, features.size())));
			try {
				experiment.runExperiment();
			} catch (Exception e) {
				quickTestFrame.setStatus("Error while running experiment: " + e);
				quickTestFrame.setAuthorSupports(null);
			}
			quickTestFrame.setStatus("Elapsed time: " + ((System.currentTimeMillis() - startTime) / 1000) + " sec");
		}

		// Parse the results and output any errors.
		quickTestFrame.setStatus("Parsing results...");
		try {
			List<Map<String, Double>> supportMaps = parser.parseResults(experiment);
    		if (!quickTestFrame.setAuthorSupports(supportMaps))
    			quickTestFrame.setStatus("Problem with supports maps. Unable to display results.");
			quickTestFrame.setStatus("Elapsed time: " + ((System.currentTimeMillis() - startTime) / 1000) + " sec");
			quickTestFrame.setStatus("Done.");
		} catch (Exception e) {
			quickTestFrame.setStatus("Error while parsing results: " + e);
			quickTestFrame.setAuthorSupports(null);
		}
	}

	public static boolean saveCsvFile(String fileName)
	{
		return parser.saveResultsAsCsvFile(fileName);
	}

	public static boolean saveExcelFile(String fileName)
	{
		return parser.saveResultsAsExcelFile(fileName);
	}

	public static boolean saveTextFileOfRawResults(String fileName)
	{
		return parser.saveRawResultsAsTextFile(fileName);
	}

	public static boolean loadAuthorPackage(String filePath)
	{
		if (!experiment.loadPackage(filePath))
			return false;

		String packageName = Utils.getFileNameFromFilePath(filePath);
		quickTestFrame.setTitle(PROGRAM_TITLE + " - " + packageName);
		return true;
	}

	public static void loadDefaultPackage()
	{
		if (loadAuthorPackage(defaultPackagePath))
			quickTestFrame.setTitle(quickTestFrame.getTitle() + " (Default)");
		else
			quickTestFrame.setStatus("Unable to load default package. Cannot continue.");
	}
}
