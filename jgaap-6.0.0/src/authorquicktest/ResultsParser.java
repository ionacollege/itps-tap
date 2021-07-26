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

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.TreeMap;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.jgaap.generics.Document;

public abstract class ResultsParser
{
	enum WEIGHT_METHOD { ACCURACY, PRECISION, RECALL };

	private static Logger logger = Logger.getLogger(ResultsParser.class);

	private WEIGHT_METHOD weightMethod;
	private double weightMethodThreshold;

	public ResultsParser()
	{
		weightMethod = WEIGHT_METHOD.ACCURACY;
		weightMethodThreshold = ExperimentSettings.DEFAULT_ACCURACY_THRESHOLD;
	}

	public void setWeightMethod(WEIGHT_METHOD method) { weightMethod = method; }

	public void setWeightMethodThreshold(double threshold)
	{
		if (threshold > 0)
			weightMethodThreshold = threshold;
	}

	public double getWeightMethodThreshold() { return weightMethodThreshold; }
	public WEIGHT_METHOD getWeightMethod() { return weightMethod; }

	public abstract List<Map<String, Double>> parseResults(CompoundExperiment testExp) throws Exception;

	public abstract boolean saveResultsAsCsvFile(String fileName);

	public abstract boolean saveResultsAsExcelFile(String fileName);

	public abstract boolean saveRawResultsAsTextFile(String fileName);

	protected double calculateEntropyFromValues(Collection<Double> values)
	{
		 double entropy = 0;
		 for (Double val : values) {
		     if (val > 0)
		         entropy -= val * Math.log(val) / Math.log(2);
		 }
		 return entropy;
	}

	protected double calculateExperimentAccuracy(List<String> answers, List<String> expResults)
	{
	    if (answers.size() != expResults.size()) {
            logger.fatal("Number of answers (" + answers.size()
            			+ ") and number of results (" + expResults.size()
            			+ ") do not match.");
	        return -1;
	    }

	    int numAnswers = answers.size();
	    int correctAnswers = 0;
	    // Tally up the correct answers.
	    for (int i = 0; i < numAnswers; i++) {
	        if (answers.get(i).equals(expResults.get(i)))
	            correctAnswers++;
	    }
	    // Divide the number right by the total number of
	    // answers to get the percent accuracy.
	    return ((double)correctAnswers / numAnswers);
	}

	/**
	 *
	 * @param {List<String>} authorAnswers - a list of the actual authors for each file; should
	 * 		parallel expResults
	 * @param {List<String>} expResults - a list of authors' names as the experiment results; should
	 * 		parallel authorAnswers
	 * @return {Map<String, Double>} a map of each author name to the experiment's precision for him/her
	 */
	protected Map<String, Double> generateAuthorPrecisionMapForExperiment(List<String> authorAnswers, List<String> expResults)
	{
		if (authorAnswers.size() != expResults.size())
			return null;

		Set<String> authorSet = new TreeSet<String>(authorAnswers);
		Map<String, Double> authorPrecisionMap = new TreeMap<String, Double>();
		for (String author : authorSet)
			authorPrecisionMap.put(author, calculateExperimentPrecisionForAuthor(authorAnswers, expResults, author));
		return authorPrecisionMap;
	}

	/**
	 *
	 * @param {List<String>} authorAnswers - a list of the actual authors for each file; should
	 * 		parallel expResults
	 * @param {List<String>} expResults - a list of authors' names as the experiment results; should
	 * 		parallel authorAnswers
	 * @return {Map<String, Double>} a map of each author name to the experiment's precision for him/her
	 */
	protected Map<String, Double> generateAuthorRecallMapForExperiment(List<String> authorAnswers, List<String> expResults)
	{
		if (authorAnswers.size() != expResults.size())
			return null;

		Set<String> authorSet = new TreeSet<String>(authorAnswers);
		Map<String, Double> authorRecallMap = new TreeMap<String, Double>();
		for (String author : authorSet)
			authorRecallMap.put(author, calculateExperimentRecallForAuthor(authorAnswers, expResults, author));
		return authorRecallMap;
	}

	protected void generateAuthorSupportFromExperiments(CombinedExperiment combExp, final List<String> fileNames, final Set<String> authors,
            final Map<String, Experiment> experiments, WEIGHT_METHOD method) throws MissingAuthorException
	{
		 // Do the calculations to figure out how much support there is for
		 // each author for each file.
		 for (int i = 0; i < fileNames.size(); i++) {
		     Map<String, Double> supportMap = new TreeMap<String, Double>();
		     // Add every author to the support map, so that
		     // each is represented. There are duplicates in
		     // the author list, but they will be removed by
		     // the map structure.
		     for (String author : authors)
		         supportMap.put(author, 0.0);
		     // Calculate the support each author has by adding
		     // up the accuracies of all the experiments that
		     // support that author.
		     for (Experiment exp : experiments.values()) {
		         if (exp.results.size() != fileNames.size()) {
		             logger.fatal("Experiment " + exp.name + " has " + exp.results.size() + " results, but there are " + fileNames.size() + " files.");
		             return;
		         }

		         String expResult = exp.results.get(i);
			 	 double curSupport;
			 	 try {
			 		 curSupport = supportMap.get(expResult);
			 	 } catch (NullPointerException e) {
			 		 throw new MissingAuthorException(expResult);
			 	 }
		         // addSupport depends on the method used for weighting.
		         double addSupport;
		         if (method == WEIGHT_METHOD.ACCURACY)
		        	 addSupport = exp.accuracy;
		         else if (method == WEIGHT_METHOD.PRECISION)
		        	 addSupport = exp.authorPrecisionMap.get(expResult);
		         else
		        	 addSupport = exp.authorRecallMap.get(expResult);

		         if (addSupport >= weightMethodThreshold)
		             supportMap.put(expResult, curSupport + addSupport);
		     }
		     // Adjust the supports to a number between 0 and 1, and
		     // figure out who is the final result and how much support
		     // he/she has.
		     String result = "";
		     double maxSupport = 0;

		     for (String author : supportMap.keySet()) {
		         // The possible support an author can get is the added accuracies of all
		         // the experiments, which is not consistent. Therefore, we adjust it to
		         // a number between 0 and 1 by dividing the support the author has by the
		         // possible support he/she could have, and we change the value in the map.
		    	 double adjustedSupport;
		         if (method == WEIGHT_METHOD.ACCURACY)
		        	 adjustedSupport = supportMap.get(author) / combExp.expAccuracySum;
		         else
		        	 adjustedSupport = supportMap.get(author) / combExp.expWeightSums.get(i);
		         supportMap.put(author, adjustedSupport);

		         // Indicate if this is the author with the most support.
		         if (maxSupport < adjustedSupport) {
		             maxSupport = adjustedSupport;
		             result = author;
		         }
		     }
		     combExp.supportMaps.add(supportMap);
		     combExp.maxSupports.add(maxSupport);
		     combExp.results.add(result);
		 }
	}

	protected Map<String, Experiment> createExperimentsMapFromResults(final String results)
	{
		 Map<String, Experiment> experiments = new TreeMap<String, Experiment>();
		 String[] resultsList = results.split("\n\n+");
		 for (String result : resultsList) {
		     String[] resultLines = result.split("\n");
		     // The experiment name is on the first line.
		     String expName = getExperimentNameFromResults(resultLines[0]);
		     // The result is on the sixth line.
		     String expResult = getExperimentResultFromResults(resultLines[5]);

		     if (experiments.keySet().contains(expName)) {
		         experiments.get(expName).results.add(expResult);
		     } else {
		         // Add the experiment results to the map.
		         Experiment exp = new Experiment();
		         exp.name = expName;
		         exp.results.add(expResult);
		         experiments.put(expName, exp);
		     }
		 }
		 return experiments;
	}

	protected void createAuthorListAndFileNameListFromResults(final String results, List<String> authors, List<String> fileNames, List<Document> documents)
	{
		// Create a map associating an author with each file in the load file.
		List<Document> documentsList = documents;
	    Map<String, String> documentAuthorMap = new TreeMap<String, String>();
	    for (Document doc : documentsList) {
	    	// If the author is unknown, getAuthor() returns null. We want to
	    	// use an empty string instead.
	    	String author = (doc.getAuthor() != null) ? doc.getAuthor() : "";
	    	documentAuthorMap.put(doc.getFilePath(), author);
	    }

		String[] resultsList = results.split("\n\n+");
		for (String result : resultsList) {
		    String[] resultLines = result.split("\n");
		    // Add the new file name and author to the respective lists.
		    StringBuffer author = new StringBuffer();
		    StringBuffer fileName = new StringBuffer();

		    getAuthorAndFileNameFromResultsAndDocumentAuthorMap(resultLines[1], documentAuthorMap, author, fileName);
		    if (!fileNames.contains(fileName.toString())) {
		        fileNames.add(fileName.toString());
		        authors.add(author.toString());
		    }
		}
	}


	protected String getCsvOutput(final List<String> fileNames, final List<String> authors,
            final Map<String, Experiment> experiments, final CombinedExperiment combExp, boolean savePrecisionAndRecall)
	{
		String fileNamesAndAuthorsOutput = getFileNamesAndAuthorsCsvOutput(fileNames, authors);
		// Output for the results of the experiments.
		String experimentsOutput = getExperimentsCsvOutput(experiments);
		// Output for the results of the combined experiment.
		String combinedExperimentOutput = getCombinedExperimentCsvOutput(combExp);

		String[] fileNamesAndAuthorsOutputLines = fileNamesAndAuthorsOutput.split("\n");
		String[] combinedExperimentOutputLines = combinedExperimentOutput.split("\n");
		String[] experimentsOutputLines = experimentsOutput.split("\n");

		StringBuffer finalOutput = new StringBuffer();
		int i = 0;
		finalOutput.append(",," + combinedExperimentOutputLines[i] + "," + experimentsOutputLines[i] + "\n");
		for (i = 1; i < fileNamesAndAuthorsOutputLines.length+1; i++)
			finalOutput.append(fileNamesAndAuthorsOutputLines[i-1] + "," + combinedExperimentOutputLines[i] + "," + experimentsOutputLines[i] + "\n");
		// There is a blank line in experimentsOutputLines and combinedExperimentOutputLines, so we add 1 to i and
		// just add in the extra newline ourselves at the beginning.
		i++;
		finalOutput.append("\nCorrectly classified," + "," + combinedExperimentOutputLines[i] + "," + experimentsOutputLines[i] + "\n");
		String method = weightMethod == WEIGHT_METHOD.ACCURACY ? "accuracy" : (weightMethod == WEIGHT_METHOD.PRECISION ? "precision" : "recall");
		finalOutput.append("Weight method," + method + ",Weight method threshold," + weightMethodThreshold);

		if (savePrecisionAndRecall)
		   finalOutput.append("\n\n" + getPrecisionAndRecallOutput(authors, experiments, combExp));

		return finalOutput.toString();
	}

	/**
	 * Saves the results of the parsing to a CSV file.
	 * @param {String} saveFileName - the path and name of the file to save
	 * @param {List<String>} fileNames - the file names of the tested files
	 * @param {List<String>} authors - the authors of the tested files
	 * @param {Map<String, Experiment>} experiments - a map connecting the name of the experiments
	 * 		(i.e. LsvmCg2Mce) to the actual experiments
	 * @param {CombinedExperiment} combExp - the combined experiment to output
	 * @param {boolean} savePrecisionAndRecall - true if precision and recall of experiments
	 * 		should be included in the output file; false otherwise
	 * @return {boolean} - true if the save was successful, false if not
	 */
	protected boolean saveCsvFile(final String saveFileName, final List<String> fileNames, final List<String> authors,
            final Map<String, Experiment> experiments, final CombinedExperiment combExp, boolean savePrecisionAndRecall)
	{
		PrintWriter writer;
		try {
			// Try to open a writer for the file. If it fails, the function
			// will return false.
			writer = new PrintWriter(saveFileName);
		} catch (FileNotFoundException e) {
			// Log an error and return false to indicate the file could not be saved.
			logger.error("Unable to save CSV File. Could not find file " + saveFileName + ": " + e);
			return false;
		}

		writer.println(getCsvOutput(fileNames, authors, experiments, combExp, savePrecisionAndRecall));
		writer.close();

		// If everything goes well, return true
		// to indicate the file was saved.
		return true;
	}

	/**
	 * Saves the results of the parsing to an Excel file.
	 * @param {String} saveFileName - the path and name of the file to save
	 * @param {List<String>} fileNames - the file names of the tested files
	 * @param {List<String>} authors - the authors of the tested files
	 * @param {Map<String, Experiment>} experiments - a map connecting the name of the experiments
	 * 		(i.e. LsvmCg2Mce) to the actual experiments
	 * @param {CombinedExperiment} combExp - the combined experiment to output
	 * @param {boolean} savePrecisionAndRecall - true if precision and recall of experiments
	 * 		should be included in the output file; false otherwise
	 * @return {boolean} - true if the save was successful, false if not
	 */
	protected boolean saveExcelFile(final String saveFileName, final List<String> fileNames, final List<String> authors, List<List<String>> experimentTable,
            List<Document> documents, final Map<String, Experiment> experiments, final CombinedExperiment combExp, boolean savePrecisionAndRecall)
	{
		// Try to create an output stream. If there's a problem with
		// the file name, throw an error and don't bother doing anything else.
		FileOutputStream out;

		try {
			out = new FileOutputStream(saveFileName);
		} catch (FileNotFoundException e) {
			// Log an error and return false to indicate the file could not be saved.
			logger.error("Unable to save Excel File. Could not find file " + saveFileName + ": " + e);
			return false;
		}

		XSSFWorkbook outWorkbook = new XSSFWorkbook();

		// Output the results
		XSSFSheet resultsSheet = outWorkbook.createSheet("Results");
		// Output the file names and authors in the first two columns. Start on row 2.
		String fileNamesAndAuthorsOutput = getFileNamesAndAuthorsCsvOutput(fileNames, authors);
		Utils.writeCsvToExcelSheet(fileNamesAndAuthorsOutput, resultsSheet, 1, 0);
		Cell accuracyCell = resultsSheet.createRow(fileNames.size()+2).createCell(0);
		accuracyCell.setCellValue("Correctly classified");
		// Write the combined experiment results after the list of files and authors.
		String combinedExperimentOutput = getCombinedExperimentCsvOutput(combExp);
		Utils.writeCsvToExcelSheet(combinedExperimentOutput, resultsSheet, 0, 2);
		// Output for the results of the experiments.
		String experimentsOutput = getExperimentsCsvOutput(experiments);
		Utils.writeCsvToExcelSheet(experimentsOutput, resultsSheet, 0, resultsSheet.getRow(0).getLastCellNum() + 1);

		// Freeze first row and first two columns and adjust column widths to fit text.
		resultsSheet.createFreezePane(2, 1);
		for (int i = 0; i < resultsSheet.getRow(0).getLastCellNum(); i++) {
			// Don't resize columns that are blank.
			Cell cell = resultsSheet.getRow(1).getCell(i);
			if (cell != null
				&& cell.getCellType() != Cell.CELL_TYPE_BLANK
				&& (cell.getCellType() == Cell.CELL_TYPE_STRING && !cell.getStringCellValue().equals("")))
				resultsSheet.autoSizeColumn(i);
		}
		/*
		// Output leave one out experiment results.
		XSSFSheet looResultsSheet = outWorkbook.createSheet("LOO Results");
		InputStream looStream = com.jgaap.JGAAP.class.getResourceAsStream(ExperimentSettings.getInstance().getLeaveOneOutResultsFilePath());

		String looCsv;
		try {
			byte[] looBytes = new byte[looStream.available()];
			looStream.read(looBytes);
			looCsv = new String(looBytes);
		} catch (IOException e) {
			logger.error("Problem reading leave one out results file to output in Excel sheet: " + e);
			try {
				out.close();
				outWorkbook.close();
			} catch (IOException f) {
				return false;
			}
			return false;
		}
		// Write the CSV to the sheet.
		Utils.writeCsvToExcelSheet(looCsv, looResultsSheet, 0, 0);
		// And autoresize the columns.
		for (int i = 0; i < looResultsSheet.getRow(0).getLastCellNum(); i++) {
			// Don't resize columns that are blank.
			Cell cell = looResultsSheet.getRow(1).getCell(i);
			if (cell != null
				&& cell.getCellType() != Cell.CELL_TYPE_BLANK
				&& (cell.getCellType() == Cell.CELL_TYPE_STRING && !cell.getStringCellValue().equals("")))
				looResultsSheet.autoSizeColumn(i);
		}
		*/

		// Output recall and precision information if indicated.
		if (savePrecisionAndRecall) {
			XSSFSheet precisionAndRecallSheet = outWorkbook.createSheet("Precision and Recall");

			StringBuffer expNameCsv = new StringBuffer(",");
			for (String expName : experiments.keySet())
				expNameCsv.append("," + expName);

			Utils.writeCsvToExcelSheet(expNameCsv.toString(), precisionAndRecallSheet, 0, 0);
			Utils.writeCsvToExcelSheet(getPrecisionAndRecallOutput(authors, experiments, combExp), precisionAndRecallSheet, 1, 0);

			for (int i = 0; i < precisionAndRecallSheet.getRow(0).getLastCellNum(); i++)
				precisionAndRecallSheet.autoSizeColumn(i);
			precisionAndRecallSheet.createFreezePane(2, 1);
		}

		// Output information about the experiment.
		Font boldFont = outWorkbook.createFont();
		boldFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
		CellStyle boldStyle = outWorkbook.createCellStyle();
		boldStyle.setFont(boldFont);

		XSSFSheet experimentInfoSheet = outWorkbook.createSheet("Experiment Info");

		Row accuracyRow = experimentInfoSheet.createRow(0);
		accuracyRow.createCell(0).setCellValue("Weight method");
		accuracyRow.getCell(0).setCellStyle(boldStyle);
		accuracyRow.createCell(1).setCellValue(weightMethod.toString().toLowerCase());
		accuracyRow.createCell(2).setCellValue("Weight method threshold");
		accuracyRow.getCell(2).setCellStyle(boldStyle);
		accuracyRow.createCell(3).setCellValue(weightMethodThreshold);

		List<String> features = new ArrayList<String>();
		List<String> classifiers = new ArrayList<String>();
		List<String> eventCullers = new ArrayList<String>();
		List<String> canonicizers = new ArrayList<String>();
		try {
			Utils.getExperimentComponentsFromExperimentTable(experimentTable, features, classifiers, eventCullers, canonicizers);
		} catch (Exception e) {
			logger.error("Unable to read EE file to write experiment information to Excel file: " + e);
			try {
				out.close();
				//outWorkbook.close();
			} catch (IOException f) {
				return false;
			}
			return false;
		}

		// Output experiment features.
		experimentInfoSheet.createRow(2).createCell(0).setCellValue("Features");
		experimentInfoSheet.getRow(2).getCell(0).setCellStyle(boldStyle);
		outputExperimentComponentsToSheet(experimentInfoSheet, features, 3);

		// Output experiment classifiers.
		experimentInfoSheet.createRow(5).createCell(0).setCellValue("Classifiers");
		experimentInfoSheet.getRow(5).getCell(0).setCellStyle(boldStyle);
		outputExperimentComponentsToSheet(experimentInfoSheet, classifiers, 6);

		// Output experiment event cullers.
		experimentInfoSheet.createRow(8).createCell(0).setCellValue("Event Cullers");
		experimentInfoSheet.getRow(8).getCell(0).setCellStyle(boldStyle);
		outputExperimentComponentsToSheet(experimentInfoSheet, eventCullers, 9);

		// Output experiment canonicizers.
		experimentInfoSheet.createRow(11).createCell(0).setCellValue("Canonicizers");
		experimentInfoSheet.getRow(11).getCell(0).setCellStyle(boldStyle);
		outputExperimentComponentsToSheet(experimentInfoSheet, canonicizers, 12);

		// Output corpus.
		experimentInfoSheet.createRow(14).createCell(0).setCellValue("Authors");
		experimentInfoSheet.getRow(14).getCell(0).setCellStyle(boldStyle);

		outputAuthorCorpus(documents, experimentInfoSheet, 15);

		try {
			// Close everything out.
			outWorkbook.write(out);
			out.close();
			//outWorkbook.close();
		} catch (IOException e) {
			logger.error("Unable to write data to Excel file: " + e);
			return false;
		}

		return true;
	}

	protected boolean saveTextFile(String fileName, List<List<String>> experimentTable, List<Document> documents, List<String> fileNames, String results)
	{
		File file = new File(fileName);
		PrintWriter writer;
		try {
			writer = new PrintWriter(file);
		} catch (FileNotFoundException e) {
			return false;
		}

		// Get the information about the experiment.
		List<String> features = new ArrayList<String>();
		List<String> classifiers = new ArrayList<String>();
		List<String> eventCullers = new ArrayList<String>();
		List<String> canonicizers = new ArrayList<String>();
		try {
			Utils.getExperimentComponentsFromExperimentTable(experimentTable, features, classifiers, eventCullers, canonicizers);
		} catch (Exception e) {
			logger.error("Unable to read EE file to write experiment information to text file: " + e);
			writer.close();
			return false;
		}

		// Get the corpus for the experiment.
		Map<String, List<String>> authorCorpus = Utils.getAuthorCorpusFromDocumentsList(documents);

		// Now output that information.
		writer.write("# Test files: " + fileNames + "\n");
		writer.write("# Features: " + features + "\n");
		writer.write("# Classifiers: " + classifiers + "\n");
		writer.write("# Event Cullers: " + eventCullers + "\n");
		writer.write("# Canonicizers: " + canonicizers + "\n\n");
		writer.write("# Authors:\n");
		for (String author : authorCorpus.keySet()) {
			writer.write("# " + author + "\n");
			for (String doc : authorCorpus.get(author))
				writer.write("# - " + doc + "\n");
		}

		writer.write("\n\n");

		// And finally output the results and close out.
		writer.write(results);
		writer.close();

		return true;
	}

	protected String getFileNamesAndAuthorsCsvOutput(final List<String> fileNames, final List<String> authors)
	{
		 StringBuffer output = new StringBuffer();
		 for (int i = 0; i < fileNames.size(); i++)
			 output.append(fileNames.get(i).substring(fileNames.get(i).lastIndexOf("/") + 1)).append(",").append(authors.get(i)).append("\n");
		 return output.toString();
	}

	protected String getExperimentsCsvOutput(final Map<String, Experiment> experiments)
	{
		 StringBuffer output = new StringBuffer();
		 // Output experiment names.
		 // We need to capture an experiment name (doesn't matter which one)
		 // for the next loop.
		 String anyExpName = "";
		 for (String expName : experiments.keySet()) {
		     output.append(expName).append(",");
		     anyExpName = expName;
		 }
		 output.append("\n");

		 // Output experiments results. All experiment results should
		 // be of the same size, so it's okay to just use any.
		 for (int i = 0; i < experiments.get(anyExpName).results.size(); i++) {
		     for (Experiment exp : experiments.values())
		         output.append(exp.results.get(i)).append(",");
		     output.append("\n");
		 }

		 // Output accuracy information.
		 output.append("\n");
		 for (Experiment exp : experiments.values())
		     output.append(exp.accuracy + ",");

		 return output.toString();
	}

	protected String getCombinedExperimentCsvOutput(final CombinedExperiment combExp)
	{
		 if (combExp.supportMaps.size() != combExp.supportEntropies.size()) {
		     logger.fatal("Different number of support maps and entropies.");
		     return "";
		 }

		 StringBuffer output = new StringBuffer("");

		 // Output author names at the top.
		 output.append("Result,Max Support,Entropy,,");
		 for (String author : combExp.supportMaps.get(0).keySet())
		     output.append(author).append(",");
		 output.append("\n");

		 // Output supports, result, and the max support.
		 for (int i = 0; i < combExp.supportMaps.size(); i++) {
		     Map<String, Double> supportMap = combExp.supportMaps.get(i);
		     output.append(combExp.results.get(i) + "," + combExp.maxSupports.get(i)
		             + "," + combExp.supportEntropies.get(i) + ",,");
		     for (Double support : supportMap.values())
		         output.append(support + ",");
		     output.append("\n");
		 }

		 // Add commas after the accuracy to make this row as long as the others.
		 // This eliminates problems when combining it with other outputs on the
		 // same row (as in saveCsvFile(...) ).
		 int numAuthors = combExp.supportMaps.get(0).keySet().size();
		 output.append("\n" + combExp.accuracy).append(new String(new char[numAuthors+4]).replace("\0", ","));

		 return output.toString();
	}


	private double calculateExperimentRecallForAuthor(final List<String> answers, final List<String> expResults, final String author)
	{
		// Can't do it if the number of experiments doesn't equal
		// the number of answers. Return -1 to indicate an error.
		if (answers.size() != expResults.size())
			return -1;

		int numWrittenByAuthor = 0;
		int numWrittenByAndClassifiedAsAuthor = 0;

		for (int i = 0; i < answers.size(); i++) {
			if (answers.get(i).equals(author)) {
				numWrittenByAuthor++;
				if (expResults.get(i).equals(author)) {
					numWrittenByAndClassifiedAsAuthor++;
				}
			}
		}
		return ((double)numWrittenByAndClassifiedAsAuthor / numWrittenByAuthor);
	}

	private double calculateExperimentPrecisionForAuthor(final List<String> answers, final List<String> expResults, final String author)
	{
		// Can't do it if the number of experiments doesn't equal
		// the number of answers. Return -1 to indicate an error.
		if (answers.size() != expResults.size())
		    return -1;

		int numClassifiedAsAuthor = 0;
		int numClassifiedAsAndWrittenByAuthor = 0;

		for (int i = 0; i < answers.size(); i++) {
			if (expResults.get(i).equals(author)) {
				numClassifiedAsAuthor++;
				if (answers.get(i).equals(author)) {
					numClassifiedAsAndWrittenByAuthor++;
				}
			}
		}
		return ((double)numClassifiedAsAndWrittenByAuthor / numClassifiedAsAuthor);
	}

	/*
	private static void getAuthorAndFileNameFromResultsByDocSize(final String results, StringBuffer author, StringBuffer fileName)
	{
		 // Split line by ".txt" string and take the first part.
		 // Then, only take up until the first space.
		 fileName.replace(0, fileName.length(), results.split(".txt")[0]);
		 // The author is to the left of the first space, and the file name
		 // is to the right.
		 Pattern digitPattern = Pattern.compile("[0-9]");
		 Matcher digitMatcher = digitPattern.matcher(fileName.toString());
		 digitMatcher.find();
		 author.replace(0, author.length(), fileName.substring(0, digitMatcher.start()));
	}
	*/

	private void getAuthorAndFileNameFromResultsAndDocumentAuthorMap(final String results, Map<String, String> documentAuthorMap, StringBuffer author, StringBuffer fileName)
	{
		String[] resultsParts = results.split("\\.[A-Za-z]+ ", 2);
		if (resultsParts.length < 2)
			return;
		String filePath = resultsParts[1];

		// If the file path is not in the load file, then the author is unknown.
		if (documentAuthorMap.containsKey(filePath))
			author.replace(0, author.length(), documentAuthorMap.get(filePath));
		else
			author.replace(0, author.length(), "");

		// The file name is the substring up to the first period in the results.
		// Macs use "/" as a folder delimiter; Windows uses "\".
		//int fileNameStartIndex = Math.max(filePath.lastIndexOf("/"), filePath.lastIndexOf("\\")) + 1;
		//fileName.replace(0, author.length(), filePath.substring(fileNameStartIndex));
		fileName.replace(0, fileName.length(), filePath);
	}

	private String getExperimentNameFromResults(final String results)
	{
		 // Chop off all except the last two hyphens.
		 // The last two hyphens are in the date that
		 // is automatically added by JGAAP to the end of
		 // the experiment name. Therefore, the actual
		 // experiment name is before those but after all
		 // the others.
		 String expName = results;
		 int numHyphens = expName.length() - expName.replace("-", "").length();

		 for (int i = 0; i < numHyphens; i++)
		     expName = expName.substring(expName.indexOf("-") + 1, expName.length());
		 // Remove date (not needed with new results).
		 //expName.remove(expName.length() - 14, 14);
		 // Get rid of initial number.
		 String digit = expName.substring(0, 1);
		 boolean firstCharIsDigit = digit.matches("[0-9]");
		 while (firstCharIsDigit) {
		     expName = expName.substring(1, expName.length());
		     digit = expName.substring(0, 1);
		     firstCharIsDigit = digit.matches("[0-9]");
		 }

		 return expName;
	}

	private String getExperimentResultFromResults(final String results)
	{
		// The results should be in the format "#. results other_info"
		// So we look for the first expression that begins with a space,
		// followed by a series of letters, numbers, and spaces, and ends
		// with another space.
		String regexp = " [A-Za-z0-9 ]+ ";
		Matcher m = Pattern.compile(regexp).matcher(results);
		if (!m.find())
			return null;
		String match = m.group();
		return match.substring(1, match.length() - 1);
	}

	private String getPrecisionAndRecallOutput(final List<String> answers, final Map<String, Experiment> experiments,
			final CombinedExperiment combExp)
	{
		 StringBuffer output = new StringBuffer("");

		 Set<String> authors = new TreeSet<String>();
		 authors.addAll(answers);
		 for (String author : authors) {
		     StringBuffer precisionOutput = new StringBuffer(author);
		     StringBuffer recallOutput = new StringBuffer(author);
		     precisionOutput.append(",Precision,");
		     recallOutput.append(",Recall,");
		     for (Experiment exp : experiments.values()) {
		         double precision = calculateExperimentPrecisionForAuthor(answers, exp.results, author);
		         double recall = calculateExperimentRecallForAuthor(answers, exp.results, author);
		         precisionOutput.append(precision + ",");
		         recallOutput.append(recall + ",");
		     }

		     precisionOutput.append("," + calculateExperimentPrecisionForAuthor(answers, combExp.results, author));
		     recallOutput.append("," + calculateExperimentRecallForAuthor(answers, combExp.results, author));

		     output.append(precisionOutput + "\n" + recallOutput + "\n");
		 }

		 return output.toString();
	}

	@SuppressWarnings("unused")
	private boolean saveCsvFile(final String saveFileName, final List<String> fileNames, final List<String> authors,
            final Map<String, Experiment> experiments, final CombinedExperiment combExp)
	{
		return saveCsvFile(saveFileName, fileNames, authors, experiments, combExp, true);
	}



	/**
	 *
	 * @param sheet
	 * @param rowNum
	 * @return
	 */
	private void outputAuthorCorpus(List<Document> documents, XSSFSheet sheet, int rowNum)
	{
		Map<String, List<String>> authorCorpus = Utils.getAuthorCorpusFromDocumentsList(documents);

		List<String> authors = new ArrayList<String>();
		authors.addAll(authorCorpus.keySet());
		outputExperimentComponentsToSheet(sheet, authors, rowNum);

		int curCol = 0;
		int curRow = rowNum + 1;
		for (String author : authors) {
			for (String doc : authorCorpus.get(author)) {
				Row row = sheet.getRow(curRow);
				if (row == null)
					row = sheet.createRow(curRow);
				curRow++;
				row.createCell(curCol).setCellValue(doc);
			}
			// Go back to the original row.
			curRow = rowNum + 1;
			// Move over one column.
			curCol++;
		}
	}

	/**
	 *
	 * @param sheet
	 * @param components
	 * @param rowNum
	 */
	private void outputExperimentComponentsToSheet(XSSFSheet sheet, Collection<String> components, int rowNum)
	{
		Font italicFont = sheet.getWorkbook().createFont();
		italicFont.setItalic(true);
		CellStyle italicStyle = sheet.getWorkbook().createCellStyle();
		italicStyle.setFont(italicFont);

		if (components.isEmpty()) {
			Cell cell = sheet.createRow(rowNum).createCell(0);
			cell.setCellStyle(italicStyle);
			cell.setCellValue("None");
		} else {
			Row row = sheet.createRow(rowNum);
			int colNum = 0;
			for (String component : components)
				row.createCell(colNum++).setCellValue(component);
		}
	}

	/**
	 * Holds information about an individual experiment.
	 * (Combination of classifier and feature.)
	 * @author Sean
	 *
	 */
	protected class Experiment
	{
		public Experiment() {
			results = new ArrayList<String>();
			authorPrecisionMap = new TreeMap<String, Double>();
			authorRecallMap = new TreeMap<String, Double>();
		}

		public String name;
		public double accuracy;
		public List<String> results;
		public Map<String, Double> authorPrecisionMap;
		public Map<String, Double> authorRecallMap;

	}

	/**
	 * Holds information about a combined experiment.
	 * (Set of individual experiments.)
	 * @author Sean
	 *
	 */
	protected class CombinedExperiment
	{
		public CombinedExperiment()
		{
			expAccuracySum = 0;
			expWeightSums = new ArrayList<Double>();
			results = new ArrayList<String>();
			maxSupports = new ArrayList<Double>();
			supportEntropies = new ArrayList<Double>();
			supportMaps = new ArrayList<Map<String, Double>>();
		}

		// Accuracy of the combined method.
		public double accuracy;
		// List of author results of test files.
		public List<String> results;
		// Sum of accuracies of individual methods that
		// make up the combined method.
		public double expAccuracySum;
		// Sum of precisions or recalls (depending on
		// method given). Is different for each file,
		// which is why we need a list.
		public List<Double> expWeightSums;
		// List of the maximum supports of test files.
		public List<Double> maxSupports;
		// List of entropies of the authors supports
		// of the test files.
		public List<Double> supportEntropies;
		// A list of maps associating authors with their
		// support for each test file.
		public List<Map<String, Double>> supportMaps;

		// TODO: all lists must be parallel so that they
		// refer to the same files. (Which is stored outside
		// the CombinedExperiment class. That should potentially
		// be changed.)
	}
}

/*
private static double calculateExperimentAccuracy(final List<String> answers, final List<String> expResults)
{
	 if (answers.size() != expResults.size()) {
	     com.jgaap.backend.ExperimentEngine.output("Number of answers (" + answers.size()
	              + ") and number of results (" + expResults.size()
	              + ") do not match.");
	     return -1;
	 }

	 int numAnswers = answers.size();
	 int correctAnswers = 0;
	 // Tally up the correct answers.
	 for (int i = 0; i < numAnswers; i++) {
	     if (answers.get(i).equals(expResults.get(i)))
	         correctAnswers++;
	 }
	 // Divide the number right by the total number of
	 // answers to get the percent accuracy.
	 return ((double)correctAnswers / numAnswers);
}
*/

/*
private static Set<String> getSetOfExperimentComponentsFromExperimentNames(List<String> expNames)
{
	Set<String> components = new TreeSet<String>();

	Pattern capitalsPattern = Pattern.compile("[A-Z]");

	 for (String expName : expNames) {
		 Matcher capitalsMatcher = capitalsPattern.matcher(expName);
		 while (capitalsMatcher.find()) {
			 components.add(expName.substring(capitalsMatcher.start(), capitalsMatcher.end()));
		 }
	 }

	 return components;
}
*/
