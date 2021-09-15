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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.jgaap.JGAAPConstants;
import com.jgaap.backend.CSVIO;
import com.jgaap.generics.Document;

import edu.stanford.nlp.util.StringUtils;

public class AuthorPackageIO
{
	private static final int MAGIC_NUMBER = 0x7F2A40CC;
	private static final int SECTION_START_NUMBER = 0x65EE8DB1;
	private static final String FILE_CHARSET = "UTF-8";


	private static class PackageDetails
	{
		public String packageFilePath;
		public String eeFilePath;
		public String loadFilePath;
		public Path leaveOneOutResultsPath;
	}

	public static void main(String args[])
	{
		List<Document> docs = new ArrayList<Document>();

		//
		PackageDetails juniusPackage = new PackageDetails();
		juniusPackage.packageFilePath = "/Users/Sean/Documents/College/Research/Thomas Paine/PaineProject/Packages/junius.pack";
		juniusPackage.eeFilePath = "/Users/Sean/Documents/College/Research/Thomas Paine/PaineProject/Packages/all/ee_all.csv";
		juniusPackage.loadFilePath = "/Users/Sean/Documents/College/Research/Thomas Paine/PaineProject/Packages/junius/load-junius.csv";
		juniusPackage.leaveOneOutResultsPath = Paths.get("/Users/Sean/Documents/College/Research/Thomas Paine/PaineProject/Packages/junius/results-junius.csv");

		PackageDetails americanPackage = new PackageDetails();
		americanPackage.packageFilePath = "/Users/Sean/Documents/College/Research/Thomas Paine/PaineProject/Packages/american.pack";
		americanPackage.eeFilePath = "/Users/Sean/Documents/College/Research/Thomas Paine/PaineProject/Packages/all/ee_all.csv";
		americanPackage.loadFilePath = "/Users/Sean/Documents/College/Research/Thomas Paine/PaineProject/Packages/american/load-american.csv";
		americanPackage.leaveOneOutResultsPath = Paths.get("/Users/Sean/Documents/College/Research/Thomas Paine/PaineProject/Packages/american/results-american.csv");

		PackageDetails americanRedonePackage = new PackageDetails();
		americanRedonePackage.packageFilePath = "/Users/Sean/Documents/College/Research/Thomas Paine/PaineProject/Packages/american-redone.pack";
		americanRedonePackage.eeFilePath = "/Users/Sean/Documents/College/Research/Thomas Paine/PaineProject/Packages/all/ee_all.csv";
		americanRedonePackage.loadFilePath = "/Users/Sean/Documents/College/Research/Thomas Paine/PaineProject/Packages/american-redone/load-american-redone.csv";
		americanRedonePackage.leaveOneOutResultsPath = Paths.get("/Users/Sean/Documents/College/Research/Thomas Paine/PaineProject/Packages/american-redone/results-american-redone.csv");

		PackageDetails pkg = americanRedonePackage;
		//

		String leaveOneOutResultsCsv = "";
		try {
			leaveOneOutResultsCsv = StringUtils.join(Files.readAllLines(pkg.leaveOneOutResultsPath, StandardCharsets.UTF_8), "\n");
		} catch (Exception e) {
			System.out.println("Error: " + e);
			return;
		}

		List<List<String>> experimentTable = new ArrayList<List<String>>();
		try {
			experimentTable = com.jgaap.backend.CSVIO.readCSV(pkg.eeFilePath);
		} catch (Exception e) {
			System.out.println("Error: " + e);
		}

		try {
			List<List<String>> docList = com.jgaap.backend.CSVIO.readCSV(pkg.loadFilePath);
			for (List<String> docEntry : docList) {
				Document doc = new Document(docEntry.get(1), docEntry.get(0));
				doc.load();
				docs.add(doc);
			}
		} catch (Exception e) {
			System.out.println("Error: " + e);
		}


		writeAuthorPackage(pkg.packageFilePath, leaveOneOutResultsCsv, experimentTable, docs);

		StringBuffer readLooResultsCsv = new StringBuffer();
		List<Document> readDocs = new ArrayList<Document>();
		List<List<String>> readExpTable = new ArrayList<List<String>>();

		readAuthorPackage(pkg.packageFilePath, readLooResultsCsv, readExpTable, readDocs);

		//
		System.out.println(readLooResultsCsv);
		System.out.println("#####################");
		System.out.println("#####################");
		System.out.println(readExpTable);
		System.out.println("#####################");
		System.out.println("#####################");
		System.out.println(readDocs);
		//

	}

	public static boolean readAuthorPackage(String filePathString, StringBuffer leaveOneOutCsv, List<List<String>> experimentTable, List<Document> documents)
	{
		// Check for nulls and clear variables so we're not
		// adding to something that already has stuff in it.
		if (filePathString == null || leaveOneOutCsv == null || experimentTable == null || documents == null)
			return false;
		leaveOneOutCsv.delete(0, leaveOneOutCsv.length());
		experimentTable.clear();
		documents.clear();

		try {
			BufferedReader reader;
			// Read the file.
			if (filePathString.startsWith(com.jgaap.JGAAPConstants.JGAAP_RESOURCE_PACKAGE)) {
				reader = new BufferedReader(new InputStreamReader(
						AuthorPackageIO.class.getResourceAsStream(filePathString)));
			} else {
				Path filePath = Paths.get(filePathString);
				reader = Files.newBufferedReader(filePath, Charset.forName(FILE_CHARSET));
			}

			String line = reader.readLine();
			// If there is no first line, we can't
			// read the file.
			if (line == null)
				return false;
			// Make sure the file has the correct magic number,
			// indicating it's the correct file type.
			try {
				int fileMagicNumber = Integer.parseInt(line);
				if (fileMagicNumber != MAGIC_NUMBER)
					return false;
			} catch (NumberFormatException e) {
				return false;
			}


			// Possible values...
			// 0  - leave one out results file
			// 1  - experiment table
			// 2+ - documents
			int currentSection = 0;

			StringBuffer docText = new StringBuffer();
			StringBuffer experimentTableText = new StringBuffer();
			Document doc = null;

			while ((line = reader.readLine()) != null) {
				try {
					// Try to convert everything to a number.
					// If it's the section start number, we
					// advance the section. Otherwise, we add
					// the line to a variable (depending on
					// what section we're in).
					int number = Integer.parseInt(line);
					if (number == SECTION_START_NUMBER) {
						// The section start number indicates
						// we are reading a new section.
						currentSection++;
						// If current section > 1, then we are in the
						// documents sections.
						if (currentSection > 1) {
							// If doc is not null, then this is not the first
							// document. We need to set the text of the document
							// and add it to the documents list before moving on
							// to the next document.
							if (doc != null) {
								doc.readStringText(docText.toString());
								documents.add(doc);
							}
							// If we're just starting this document, create a
							// new document and document text and set the
							// title and author.
							doc = new Document();
							docText = new StringBuffer();
							doc.setTitle(reader.readLine());
							doc.setAuthor(reader.readLine());
							doc.setFilePath(doc.getAuthor() + "/" + doc.getTitle());
						}
					}
				} catch (NumberFormatException e) {
					if (currentSection > 1) {
						// Documents start at the 3rd section.
						docText.append(line).append("\n");
					} else if (currentSection == 1) {
						// Experiment table is in the 2nd section.
						if (experimentTableText.length() > 0)
							experimentTableText.append("\n");
						experimentTableText.append(line);
					} else {
						// Leave one out results are in the 1st section.
						leaveOneOutCsv.append(line).append("\n");
					}
				}
			}
			// And let's actually create the experiment table, since we have the string.
			//System.out.println("|"+experimentTableText.toString()+"|");
			experimentTable.addAll(Utils.getTableFromString(experimentTableText.toString(), ",", "\n"));
		} catch (IOException e) {
			System.err.format("Error reading file: %s\n", e);
			return false;
		}

		return true;
	}


	public static boolean writeAuthorPackage(String filePathString, String leaveOneOutCsv, List<List<String>> experimentTable, List<Document> documents)
	{
		if (filePathString == null || leaveOneOutCsv == null || experimentTable == null || documents == null)
			return false;

		Path filePath = Paths.get(filePathString);

		// Write the file.
		try (BufferedWriter writer = Files.newBufferedWriter(filePath, Charset.forName(FILE_CHARSET))) {
			// Write number to indicate file type.
			writer.write(MAGIC_NUMBER + "\n");
			// Write the CSV for the leave one out experiment.
			writer.write(leaveOneOutCsv + "\n");

			// Write experiment table.
			writer.write(SECTION_START_NUMBER + "\n");
			writer.write(Utils.getStringFromTable(experimentTable, ",", "\n") + "\n");

			// Write the documents.
			for (Document doc : documents) {
				writer.write(SECTION_START_NUMBER + "\n");
				writer.write(doc.getTitle() + "\n");
				writer.write(doc.getAuthor() + "\n");
				writer.write(new String(doc.getText()) + "\n");
			}

			// The reader "saves" the document contents once it reaches
			// the section start number, so we'll output one at the end
			// to make sure it gets the last document.
			writer.write(SECTION_START_NUMBER + "\n");
		} catch (IOException e) {
			System.err.format("Error writing file: %s\n", e);
			return false;
		}

		return true;
	}
}
