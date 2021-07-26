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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.log4j.Logger;

public class LeaveOneOutResultsParser extends ResultsParser {
	private static Logger logger = Logger.getLogger(LeaveOneOutResultsParser.class);

	private List<String> knownAuthors;
	private List<String> fileNames;
	private Map<String, Experiment> experiments;
	private CombinedExperiment combExp;

	private CompoundLeaveOneOutExperiment experiment;

	/**
	 *
	 * @param {String} results - the text results outputted by JGAAP
	 * @param {boolean} autoSetThreshold - true if it should use the old
	 * 		threshold or set it to the median accuracy
	 * @return {List<Map<String, Double>>} a list of supports for each author,
	 * 		each list item corresponds to a document of unknown authorship
	 * @throws Exception - if the results from the test experiment are empty
	 * 		or if the leave one out file is formatted incorrectly
	 */
	public List<Map<String, Double>> parseResults(CompoundExperiment looExp) throws ParseException, MissingAuthorException
	{
		return parseResults(looExp, true);
	}

	public List<Map<String, Double>> parseResults(CompoundExperiment looExp, boolean autoSetThreshold) throws ParseException, MissingAuthorException
	{
		if (looExp == null)
			return null;
		if (!(looExp instanceof CompoundLeaveOneOutExperiment))
			return null;

		experiment = (CompoundLeaveOneOutExperiment)looExp;

		String results = experiment.getResults();

		if (results == null || results.equals(""))
			throw new ParseException("There are no results to parse.");

		knownAuthors = new ArrayList<String>();
		fileNames = new ArrayList<String>();
		createAuthorListAndFileNameListFromResults(results, knownAuthors, fileNames, experiment.getDocuments());
		experiments = createExperimentsMapFromResults(results);
		combExp = new CombinedExperiment();

        // Calculate the accuracy of each experiment.
        for (Experiment exp : experiments.values()) {
        	exp.accuracy = calculateExperimentAccuracy(knownAuthors, exp.results);
        	exp.authorPrecisionMap = generateAuthorPrecisionMapForExperiment(knownAuthors, exp.results);
        	exp.authorRecallMap = generateAuthorRecallMapForExperiment(knownAuthors, exp.results);
        }
		double threshold = (getWeightMethod() == WEIGHT_METHOD.ACCURACY && autoSetThreshold)
												? autoSetAccuracyThreshold()
												: getWeightMethodThreshold();
		logger.info("Parsing with weight threshold: " + threshold);
        for (Experiment exp : experiments.values()) {
        	if (getWeightMethod() == WEIGHT_METHOD.ACCURACY && exp.accuracy >= threshold)
        		combExp.expAccuracySum += exp.accuracy;
	    	else if (getWeightMethod() == WEIGHT_METHOD.PRECISION)
	    		calculateWeightSumsForExperiment(combExp.expWeightSums, exp.results, exp.authorPrecisionMap, threshold);
	    	else if (getWeightMethod() == WEIGHT_METHOD.RECALL)
	    		calculateWeightSumsForExperiment(combExp.expWeightSums, exp.results, exp.authorRecallMap, threshold);
        }

        // Generate author supports.
        generateAuthorSupportFromExperiments(combExp, fileNames, new TreeSet<String>(knownAuthors), experiments, getWeightMethod());
        // Calculate combined experiment accuracy.
        combExp.accuracy = calculateExperimentAccuracy(knownAuthors, combExp.results);
        // Calculate entropies.
        for (int i = 0; i < combExp.supportMaps.size(); i++) {
            Map<String, Double> supportMap = combExp.supportMaps.get(i);
            combExp.supportEntropies.add(calculateEntropyFromValues(supportMap.values()));
        }

        return new ArrayList<Map<String, Double>>();
	}


	/**
	 * Calculates the weight sums for precision or recall lists.
	 * @param results
	 * @param authorWeightMap
	 * @param threshold
	 * @return
	 */
    void calculateWeightSumsForExperiment(List<Double> expWeightSums, List<String> results, Map<String, Double> authorWeightMap, double threshold) {
		for (int j = 0; j < results.size(); j++) {
			double weight = authorWeightMap.get(results.get(j));
			// If the weight is greater than the threshold,
			// add it to the sum.
			if (weight >= threshold) {
				if (expWeightSums.size() <= j)
					expWeightSums.add(weight);
				else
					expWeightSums.set(j, expWeightSums.get(j) + weight);
			} else {
				// If the weight is not greater than the threshold,
				// we still want there to be an entry in the list.
				// Insert 0 if there isn't.
				if (expWeightSums.size() <= j)
					expWeightSums.add(0.0);
			}
		}
    }

	/**
	 * Sets the accuracy threshold to be the median
	 * accuracy of the experiments.
	 * @return {double} the updated threshold
	 */
	public double autoSetAccuracyThreshold() {
		int expCount = experiments.size();
		List<Double> accuracies = new ArrayList<Double>(expCount);
		for (Experiment exp : experiments.values())
			accuracies.add(exp.accuracy);
		Collections.sort(accuracies);
		double threshold = expCount == 1
							? accuracies.get(0)
							: (expCount % 2 == 0
								? accuracies.get(expCount / 2)
								: 0.5 * (accuracies.get(expCount / 2) + accuracies.get(expCount / 2 + 1)));
		setWeightMethodThreshold(threshold);
		return threshold;
	}


	public boolean saveResultsAsCsvFile(String fileName)
	{
		return saveCsvFile(fileName, fileNames, knownAuthors, experiments, combExp, false);
	}

	public boolean saveResultsAsExcelFile(String fileName)
	{
		return saveExcelFile(fileName, fileNames, knownAuthors, experiment.getExperimentTable(),
				experiment.getDocuments(), experiments, combExp, true);
	}

	public boolean saveRawResultsAsTextFile(String fileName)
	{
		return saveTextFile(fileName, experiment.getExperimentTable(), experiment.getDocuments(), fileNames, experiment.getResults());
	}

	public String getLeaveOneOutCsv() {
		return getCsvOutput(fileNames, knownAuthors, experiments, combExp, false);
	}
}
