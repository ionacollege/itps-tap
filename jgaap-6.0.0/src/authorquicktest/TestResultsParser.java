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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;

public class TestResultsParser extends ResultsParser {
	private static Logger logger = Logger.getLogger(TestResultsParser.class);

	private CompoundTestExperiment testExperiment;

	private String unknownResults;
	private List<String> unknownFileNames;
	private Map<String, Experiment> unknownExperiments;
	private CombinedExperiment unknownCombExp;

	private Set<String> authors;
	// Maps experiment name (e.g. Cg2LsvmMce) to its accuracy.
	private Map<String, Double> experiments;
	// We store the accuracy separately from unknownCombExp
	// because it only changes when the load file changes, while
	// unknownCombExp changes every time results are parsed.
	double combinedExperimentAccuracy;

	public TestResultsParser() {
		super();
	}


	/**
	 *
	 * @param {String} results - the text results outputted by JGAAP
	 * @return {List<Map<String, Double>>} a list of supports for each author,
	 * 		each list item corresponds to a document of unknown authorship
	 * @throws Exception - if the results from the test experiment are empty
	 * 		or if the leave one out file is formatted incorrectly
	 */
	public List<Map<String, Double>> parseResults(CompoundExperiment testExp) throws Exception
	{
		if (testExp == null)
			return null;
		if (!(testExp instanceof CompoundTestExperiment))
			return null;

		testExperiment = (CompoundTestExperiment)testExp;

		String results = testExperiment.getResults();

		if (results == null || results.equals(""))
			throw new Exception("There are no results to parse.");
		readLeaveOneOutResults(testExperiment.getLeaveOneOutResultsAsCSV());

		// Reinitialize everything, so we don't use anything
		// from a previous parsing.
		unknownResults = results;
		unknownFileNames  = new ArrayList<String>();
		unknownCombExp = new CombinedExperiment();
		unknownCombExp.accuracy = combinedExperimentAccuracy;
		unknownExperiments = new TreeMap<String, Experiment>();

		List<String> a = new ArrayList<String>();
		createAuthorListAndFileNameListFromResults(unknownResults, a, unknownFileNames, testExperiment.getDocuments());
		logger.info("Creating list of experiments from results...");
		unknownExperiments = createExperimentsMapFromResults(unknownResults);

		logger.info("Calculating sum of weights...");
		//unknownCombExp.expWeightSum = calculateWeightSum(unknownExperiments);
		for (Experiment exp : unknownExperiments.values()) {
			exp.accuracy = experiments.get(exp.name);
        	//exp.authorPrecisionMap = generateAuthorPrecisionMapForExperiment(knownAuthors, exp.results);
        	//exp.authorRecallMap = generateAuthorRecallMapForExperiment(knownAuthors, exp.results);
			if (exp.accuracy > getWeightMethodThreshold())
				unknownCombExp.expAccuracySum += exp.accuracy;
		}

		logger.info("Generating author supports from experiments for documents of unknown authorship...");
		generateAuthorSupportFromExperiments(unknownCombExp, unknownFileNames, authors, unknownExperiments, getWeightMethod());
		logger.info("Calculating entropies for documents of unknown authorship...");

		for (int i = 0; i < unknownCombExp.supportMaps.size(); i++) {
			Map<String, Double> supportMap = unknownCombExp.supportMaps.get(i);
			unknownCombExp.supportEntropies.add(calculateEntropyFromValues(supportMap.values()));
		}

		logger.info("Done parsing results.");

		return unknownCombExp.supportMaps;
	}

	public boolean saveResultsAsCsvFile(String fileName)
	{
		// Fill the list with "(unknown)" so that all test files will show up
		// with "(unknown)" as the author in the output file.
		List<String> unknownAuthors = new ArrayList<String>();
		for (int i = 0; i < unknownFileNames.size(); i++)
			unknownAuthors.add("(unknown)");

		return saveCsvFile(fileName, unknownFileNames, unknownAuthors, unknownExperiments, unknownCombExp, false);
	}

	public boolean saveResultsAsExcelFile(String fileName)
	{
		// Fill the list with "(unknown)" so that all test files will show up
		// with "(unknown)" as the author in the output file.
		List<String> unknownAuthors = new ArrayList<String>();
		for (int i = 0; i < unknownFileNames.size(); i++)
			unknownAuthors.add("(unknown)");

		return saveExcelFile(fileName, unknownFileNames, unknownAuthors, testExperiment.getExperimentTable(),
				testExperiment.getDocuments(), unknownExperiments, unknownCombExp, false);
	}

	public boolean saveRawResultsAsTextFile(String fileName)
	{
		return saveTextFile(fileName, testExperiment.getExperimentTable(), testExperiment.getDocuments(), unknownFileNames, unknownResults);
	}

	private void readLeaveOneOutResults(String leaveOneOutResultsCsv) throws Exception
	{
		List<List<String>> leaveOneOutResultsTable = Utils.getTableFromString(leaveOneOutResultsCsv, ",", "\n");

		// This row will contain the experiment names.
		List<String> topRow = leaveOneOutResultsTable.get(0);

		authors = new TreeSet<String>();
		experiments = new TreeMap<String, Double>();

		List<String> accuracyRow = null;
		// Used to get the threshold, which falls
		// in the row under the accuracy row.
		int accuracyRowNum;
		for (accuracyRowNum = 0; accuracyRowNum < leaveOneOutResultsTable.size(); accuracyRowNum++) {
			List<String> row = leaveOneOutResultsTable.get(accuracyRowNum);
			// The row holding the accuracies of each experiment
			// says "Correctly classified" in the first cell.
			if (row.get(0).equals("Correctly classified"))
				accuracyRow = row;
			// If we haven't gotten to the accuracy row yet and the cell isn't empty,
			// it has an author's name. Add it to the set of authors.
			if ((accuracyRow == null) && (row.size() > 1) && (!row.get(1).equals("")))
				authors.add(row.get(1));
		}

		// Throw an error if no row starts with "Correctly classified".
		if (accuracyRow == null
				//|| leaveOneOutResultsTable.size() < accuracyRowNum + 1
				//|| !leaveOneOutResultsTable.get(accuracyRowNum+1).get(0).equals("Weight method")
				) {
			throw new Exception("The leave one out file is formatted incorrectly. "
					+ "No row starts with \"Correctly classified\" to indicate it contains accuracy information.");
		}

		if (topRow.get(2).equals("Result")) {
			combinedExperimentAccuracy = loadAccuraciesWithNewFormat(topRow, accuracyRow, experiments);
		} else {
			combinedExperimentAccuracy = loadAccuraciesWithOldFormat(topRow, accuracyRow, experiments);
		}
	}

	private double loadAccuraciesWithOldFormat(List<String> topRow, List<String> accuracyRow, Map<String, Double> exps) throws Exception {
		// Experiment accuracies start in the third column. Create the list of experiments.
		int col;
		for (col = 2; col < topRow.size() && !topRow.get(col).equals(""); col++) {
			try {
				exps.put(topRow.get(col), Double.parseDouble(accuracyRow.get(col)));
			} catch (NumberFormatException e) {
				throw new Exception("The leave one out file is formatted incorrectly. "
						+ "There was a problem reading the experiment accuracies: " + e);
			} catch (IndexOutOfBoundsException e) {
				throw new Exception("The leave one out file is formatted incorrectly. "
						+ "There was a problem reading the experiment accuracies: " + e);
			}
		}

		// Read the combined accuracy, which is two columns after the last individual experiment accuracy.
		// (We already added 1 in the for loop, so we only add 1 now.)
		try {
			return Double.parseDouble(accuracyRow.get(col+1));
		} catch (NumberFormatException e) {
			throw new Exception("Unable to parse results. The leave one out file is formatted incorrectly. "
					+ "There was a problem reading the combined experiment accuracy: " + e);
		} catch (IndexOutOfBoundsException e) {
			throw new Exception("Unable to parse results. The leave one out file is formatted incorrectly. "
					+ "There was a problem reading the combined experiment accuracy: " + e);
		}
	}

	private double loadAccuraciesWithNewFormat(List<String> topRow, List<String> accuracyRow, Map<String, Double> exps) throws Exception {
		// Start in column 9, because that's the first possible column for experiment accuracies.
		int col = 7;
		// Find where the accuracies start.
		while (accuracyRow.get(++col).equals(""));
		// Now that we've found the experiment accuracies, create the list of experiments.
		for (; col < accuracyRow.size(); col++) {
			try {
				if (accuracyRow.get(col).equals(""))
					break;
				exps.put(topRow.get(col), Double.parseDouble(accuracyRow.get(col)));
			} catch (NumberFormatException e) {
				throw new Exception("The leave one out file is formatted incorrectly. "
						+ "There was a problem reading the experiment accuracies: " + e);
			}
		}

		// Read the combined accuracy, which is in the third column.
		try {
			return Double.parseDouble(accuracyRow.get(2));
		} catch (NumberFormatException e) {
			throw new Exception("Unable to parse results. The leave one out file is formatted incorrectly. "
					+ "There was a problem reading the combined experiment accuracy: " + e);
		} catch (IndexOutOfBoundsException e) {
			throw new Exception("Unable to parse results. The leave one out file is formatted incorrectly. "
					+ "There was a problem reading the combined experiment accuracy: " + e);
		}
	}
}
