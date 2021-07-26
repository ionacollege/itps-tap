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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JOptionPane;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import com.jgaap.JGAAPConstants;
import com.jgaap.backend.CSVIO;
import com.jgaap.generics.Document;

import edu.stanford.nlp.util.StringUtils;

public class Utils
{
	public static int EXPERIMENT_TABLE_FEATURES_ROW = 0;
	public static int EXPERIMENT_TABLE_CLASSIFIERS_ROW = 1;

	private static int NUMBER_OF_ROWS_IN_EXPERIMENT_TABLE = 4;

	/**
	 * Populates the lists with information from the given ee file. Lists cannot be null and will
	 * be cleared before being populated, so any data in them will be overwritten.
	 * @param {String} eeFilePath - the file path to the ee file containing the experiment information
	 * @param {List<String>} features - a list that will be populated with the features in the experiment
	 * @param {List<String>} classifiers - a list that will be populated with the classifiers  in the experiment
	 * @param {List<String>} eventCullers - a list that will be populated with the event cullers in the experiment
	 * @param {List<String>} canonicizers - a list that will be populated with the canonicizers in the experiment
	 * @throws IOException - if there is a problem reading from the EE file with the given path
	 */
	public static void getExperimentComponentsFromEEFile(String eeFilePath, List<String> features, List<String> classifiers,
			List<String> eventCullers, List<String> canonicizers) throws Exception
	{
		List<List<String>> experimentTable;
		if (eeFilePath.startsWith(com.jgaap.JGAAPConstants.JGAAP_RESOURCE_PACKAGE)) {
			experimentTable = com.jgaap.backend.CSVIO.readCSV(com.jgaap.JGAAP.class.getResourceAsStream(eeFilePath));
		} else {
			experimentTable = com.jgaap.backend.CSVIO.readCSV(eeFilePath);
		}

		getExperimentComponentsFromExperimentTable(experimentTable, features, classifiers, eventCullers, canonicizers);
	}

	/**
	 * Populates the lists with information from the given experiment table. Lists cannot be null
	 * and will be cleared before being populated, so any data in them will be overwritten. While
	 * it would be easier to do this inline, separating it into its own method allows us to easily
	 * change the format of the EE file and how it's read in only one place.
	 * @param {List<List<String>>} experimentTable - the
	 * @param {List<String>} features - a list that will be populated with the features in the experiment
	 * @param {List<String>} classifiers - a list that will be populated with the classifiers  in the experiment
	 * @param {List<String>} eventCullers - a list that will be populated with the event cullers in the experiment
	 * @param {List<String>} canonicizers - a list that will be populated with the canonicizers in the experiment
	 * @throws IOException - if there is a problem reading from the EE file with the given path
	 */
	public static void getExperimentComponentsFromExperimentTable(List<List<String>> experimentTable, List<String> features, List<String> classifiers,
			List<String> eventCullers, List<String> canonicizers) throws Exception
	{
		if (experimentTable.size() != NUMBER_OF_ROWS_IN_EXPERIMENT_TABLE)
			throw new Exception("Experiment table does not follow the structure of an EE file.");
		// Clear out stuff in lists
		features.clear();
		classifiers.clear();
		eventCullers.clear();
		canonicizers.clear();

		features.addAll(experimentTable.get(0));
		classifiers.addAll(experimentTable.get(1));
		eventCullers.addAll(experimentTable.get(2));
		canonicizers.addAll(experimentTable.get(3));
	}

	/**
	 * Returns a map of the author corpus that a load file represents
	 * @param {String} loadFilePath - the file path to the load file holding the corpus
	 * @return {Map<String, List<String>>} - a map associated each author name with a list of
	 * 		document names that are in their corpus
	 * @throws IOException - if there is a problem reading from the load file with the given path
	 */
	public static Map<String, List<String>> getAuthorCorpusFromLoadFile(String loadFilePath) throws IOException
	{
		List<List<String>> loadFileTable;
		if (loadFilePath.startsWith(com.jgaap.JGAAPConstants.JGAAP_RESOURCE_PACKAGE)) {
			loadFileTable = com.jgaap.backend.CSVIO.readCSV(com.jgaap.JGAAP.class.getResourceAsStream(loadFilePath));
		} else {
			loadFileTable = com.jgaap.backend.CSVIO.readCSV(loadFilePath);
		}

		Map<String, List<String>> authorCorpus = new TreeMap<String, List<String>>();

		for (List<String> entry : loadFileTable) {
			String author = entry.get(0);
			String filePath = entry.get(1);
			String fileName = filePath.substring(filePath.lastIndexOf("/")+1, filePath.length());
			if (!authorCorpus.containsKey(author)) {
				List<String> docs = new ArrayList<String>();
				docs.add(fileName);
				authorCorpus.put(author, docs);
			} else {
				authorCorpus.get(author).add(fileName);
			}
	    }

		return authorCorpus;
	}

	/**
	 * Returns a map of the author corpus that a load file represents
	 * @param {List<Document>} documentsList - the list of documents in the corpus
	 * @return {Map<String, List<String>>} - a map associated each author name with a list of
	 * 		document names that are in their corpus
	 */
	public static Map<String, List<String>> getAuthorCorpusFromDocumentsList(List<Document> documentsList)
	{
		Map<String, List<String>> authorCorpus = new TreeMap<String, List<String>>();

		for (Document doc : documentsList) {
			String author = (doc.getAuthor() != null) ? doc.getAuthor() : "";
			String title = doc.getTitle();
			if (!authorCorpus.containsKey(author)) {
				List<String> docs = new ArrayList<String>();
				docs.add(title);
				authorCorpus.put(author, docs);
			} else {
				authorCorpus.get(author).add(title);
			}
	    }

		return authorCorpus;
	}



	/**
	 * Given text in CSV output, writes it to an Excel sheet
	 * starting at the given row and column.
	 * @param {String} csvOutput - the CSV to be written to the Excel sheet
	 * @param {XSSFSheet} sheet - the Excel sheet to be written to
	 * @param {int} startRow - the row to start writing on
	 * @param {int} startCol - the column to start writing on
	 */
	public static void writeCsvToExcelSheet(String csvOutput, XSSFSheet sheet, int startRow, int startCol)
	{
		int curRow = startRow;
		int curCol;

		String[] outputRows = csvOutput.split("\n");
		for (String outputRow : outputRows) {
			curCol = startCol;
			String[] cols = outputRow.split(",");
			// Get the current row. If it doesn't exist, make a new one.
			Row row = sheet.getRow(curRow);
			if (row == null)
				row = sheet.createRow(curRow);
			curRow++;
			for (String col : cols) {
				Cell cell = row.createCell(curCol++);
				// Convert cell value to a number if you can.
				// If not, just output the string.
				try {
					cell.setCellValue(Double.parseDouble(col));
				} catch (NumberFormatException e) {
					cell.setCellValue(col);
				}
			}
		}
	}


	/**
	 * Returns a map of the experiment components to their abbreviations.
	 * @return {Map<String, String>} - a map from the names of components to their abbreviations
	 * @throws IOException
	 */
	static public Map<String, String> getAbbreviationsMap() throws IOException
	{
		Map<String, String> abbrMap = new HashMap<String,String>();
		try {
			List<List<String>> abbrList = CSVIO.readCSV(com.jgaap.JGAAP.class.getResourceAsStream(JGAAPConstants.JGAAP_RESOURCE_PACKAGE + "jgaap_abbreviations.csv"));

			for (List<String> abbr : abbrList) {
				if (abbr.size() == 2)
					abbrMap.put(abbr.get(0), abbr.get(1));
			}
		} catch (IOException e) {
			System.err.format("Problem loading abbreviations file: %s\n", e);
			throw new IOException("Unable to run experiment. There was a problem loading the abbreviations file: " + e);
		}
		return abbrMap;
	}


	/**
	 * Given text in a format with the given delimiters, converts it to a table with rows and columns.
	 * @param {String} csv - the string to convert to a 2D table of rows and columns
	 * @return {List<List<String>>} - the table with the string data
	 */
	static public List<List<String>> getTableFromString(String str, String colDelimiter, String rowDelimiter)
	{
		List<List<String>> table = new ArrayList<List<String>>();

		String[] strRows = str.split(rowDelimiter, -1);
		for (String row : strRows)
			table.add(Arrays.asList(row.split(colDelimiter, -1)));

		return table;
	}

	/**
	 * Converts a table to a string with colDelimiter between columns and
	 * rowDelimiter between rows.
	 * @param {List<List<String>} table - the table to convert to a string
	 * @return {String} - output formatted with the given delimiters
	 */
	static public String getStringFromTable(List<List<String>> table, String colDelimiter, String rowDelimiter)
	{
		List<String> condensedTable1 = new ArrayList<String>();
		for (List<String> row : table)
			condensedTable1.add(StringUtils.join(row, colDelimiter));
		String condensedTable2 = StringUtils.join(condensedTable1, rowDelimiter);
		return condensedTable2;
	}


	/**
	 * Creates and returns an experiment table containing a subset of the
	 * features in the original experiment table.
	 * @param {List<List<String>>} experimentTable - the original experiment table
	 * @param {int} fromIndex - the index of the first feature to include, inclusive
	 * @param {int} toIndex - the index of the last feature to include, exclusive
	 * @return {List<List<String>>} - an experiment table that is a copy of the
	 * 		original but with only the subset of features between fromIndex and
	 * 		toIndex
	 */
	public static List<List<String>> getFeatureExperimentTable(List<List<String>> table, int fromIndex, int toIndex)
	{
		List<List<String>> featureTable = new ArrayList<List<String>>();
		// Make sure the sublist falls within the range of the
		// length of the list.
		if (fromIndex < 0)
			fromIndex = 0;
		if (toIndex > table.get(EXPERIMENT_TABLE_FEATURES_ROW).size())
			toIndex = table.get(EXPERIMENT_TABLE_FEATURES_ROW).size();

		for (int i = 0; i < NUMBER_OF_ROWS_IN_EXPERIMENT_TABLE; i++) {
			if (i != EXPERIMENT_TABLE_FEATURES_ROW) {
				featureTable.add(table.get(i));
			} else {
				featureTable.add(table.get(EXPERIMENT_TABLE_FEATURES_ROW).subList(fromIndex, toIndex));
			}
		}

		return featureTable;
	}

	/**
	 * Given a full file path, returns just the name of the file.
	 * @param {String} filePath - the full file path to a file
	 * @return {String} - the name of the file including extension,
	 * 		but without directories
	 */
	public static String getFileNameFromFilePath(String filePath)
	{
		return filePath.substring(filePath.lastIndexOf("/") + 1, filePath.length());
	}

	/**
	 * Output message with pound signs above and below
	 * to make it stand out. Primarily for debugging.
	 * @param text
	 */
	public static void output(String text)
	{
		StringBuffer b = new StringBuffer();

		final int NUM_POUNDS = 40;
		final int NUM_LINES = 3;

		for (int j = 0; j < NUM_LINES; j++) {
			for (int i = 0; i < NUM_POUNDS; i++)
				System.out.print("#");
			System.out.println();
		}
		System.out.println(text);
		b.append(text).append("\n\n");

		for (int j = 0; j < NUM_LINES; j++) {
			for (int i = 0; i < NUM_POUNDS; i++)
				System.out.print("#");
			System.out.println();
		}
	}

	/**
	 * Tries to get extra memory by restarting. Prompts
	 * the user for restart if there isn't enough.
	 * @param promptRestart
	 */
	public static boolean requestMemory(int requestedMemoryMb)
	{
		// Get the memory available in megabytes.
		long maxMemoryMb = Runtime.getRuntime().maxMemory() / (long)Math.pow(2, 20);
		System.out.println("Memory available: " + maxMemoryMb + "MB");

		// If we have less than 80% of the requested memory, restart
		// and ask for half the previous requested memory.
		if (maxMemoryMb < requestedMemoryMb * 0.8) {
			// In order to make running the JAR as easy as possible, we want it to run just
			// by double clicking. This feels like a bit of a hack, but seems to be the easiest
			// way of doing it without using libraries to create platform dependent executables.
			try {
				String jarFilePath = new File(AuthorQuickTest.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getAbsolutePath();
				String command = "java -Xmx" + (requestedMemoryMb/2) + "M -jar " + jarFilePath + " " + (requestedMemoryMb/2);
				System.out.println("Running command to restart: " + command);
				Process proc = Runtime.getRuntime().exec(command);
				// Wait for a bit to allow the process to start.
				Thread.sleep(500);
				// If the process exits fine, then close this program as well.
				// If the process hasn't exited yet, that's what we want.
				// proc.exitValue() will throw an IllegalThreadStateException,
				// and we will close out the program as well.
				if (proc.exitValue() == 0)
					System.exit(0);
			} catch (IllegalThreadStateException e) {
				// This error is thrown when exitValue() is called on a process
				// that hasn't exited yet.
				System.exit(0);
			} catch (Exception e) {
				// If anything goes wrong, just run as-is.
				// No need to handle the exception.
				// But might as well output a message to the
				// command line for debugging purposes.
				System.out.println("Problem restarting: " + e);
				return false;
			}
			JOptionPane.showMessageDialog(null, "Unable to restart program. Running with current memory restrictions.", "Problem Restarting", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return false;
	}

	/**
	 *
	 * @param {List<File>} dirs - a list of directories to
	 * 		generate documents from. The names of the directories
	 * 		are used as the author names
	 * @return {List<Document>} a list of documents generated
	 * 		from the files in a given list of directories,
	 * 		including all the subdirectories. The author name
	 * 		is set to be the name of the directory
	 */
	public static List<Document> getCorpusFromDirs(List<File> dirs) {
		List<Document> docs = new ArrayList<Document>();
		for (File dir : dirs)
			if (dir.isDirectory())
				docs.addAll(getDocumentsFromDir(dir));
		return docs;
	}

	/**
	 * Returns a list of documents generated from the files in
	 * a given directory. Includes files in all subdirectories.
	 * The author name is set to be the name of the directory
	 * passed in as an argument.
	 * @param {File} dir - the source directory
	 * @return {List<Document>} a list of Documents, one for
	 * 		each file in the directory, or null if the argument
	 * 		is not a directory
	 */
	public static List<Document> getDocumentsFromDir(File dir) {
		// If the File passed in isn't a
		// directory, just return null.
		if (!dir.isDirectory())
			return null;

		List<Document> docs = new ArrayList<Document>();
		for (File file : dir.listFiles()) {
			// If it's a file, add it to the list.
			if (file.isFile() && !file.isHidden())
				docs.add(new Document(file.getAbsolutePath(), dir.getName()));
			// If it's a folder, we use this method recursively.
			else if (file.isDirectory() && !file.isHidden())
				docs.addAll(getDocumentsFromDir(file));
		}
		return docs;
	}
}
