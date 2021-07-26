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

import java.io.IOException;
import java.util.List;

import javax.swing.JOptionPane;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

//import com.google.common.io.Files;
import com.jgaap.generics.Document;

import authorquicktest.ResultsParser.WEIGHT_METHOD;

public class PackageGenerator {
	private static Logger logger = Logger.getLogger(PackageGenerator.class);

	private static final String PROGRAM_TITLE = "JGAAP Package Generator";
	private static LeaveOneOutResultsParser parser;
	private static PackageGeneratorFrame packageGeneratorFrame;
	private static CompoundLeaveOneOutExperiment experiment;

	public static void main(String[] args) {
		int requestedMemoryMb = 8192;
		if (args.length > 0) {
			try {
				requestedMemoryMb = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {}
		}
		Utils.requestMemory(requestedMemoryMb);
		BasicConfigurator.configure();
    	logger.setLevel(Level.INFO);

    	parser = new LeaveOneOutResultsParser();

    	packageGeneratorFrame = new PackageGeneratorFrame();
    	packageGeneratorFrame.setTitle(PROGRAM_TITLE);
    	packageGeneratorFrame.setSize(800, 600);
    	// Center the frame on the screen.
    	packageGeneratorFrame.setLocationRelativeTo(null);
    	packageGeneratorFrame.setVisible(true);

    	long maxMemoryMb = Runtime.getRuntime().maxMemory() / (long)Math.pow(2, 20);
		packageGeneratorFrame.setStatus("Memory available: " + maxMemoryMb + "MB");
   	}

	public static boolean saveCsvFile(String fileName) {
		return parser.saveResultsAsCsvFile(fileName);
	}

	public static boolean saveExcelFile(String fileName) {
		return parser.saveResultsAsExcelFile(fileName);
	}

	public static boolean savePackageFile(String fileName, List<List<String>> experimentTable, List<Document> documents) {
		return AuthorPackageIO.writeAuthorPackage(fileName, parser.getLeaveOneOutCsv(), experimentTable, documents);
	}

	public static boolean saveRawResultsFile(String fileName) {
		return com.jgaap.backend.Utils.saveFile(fileName, experiment.getResults());
	}

	public static void generatePackage(List<Document> docs, List<List<String>> expTable,
										WEIGHT_METHOD weightMethod, double weightThreshold) {
		// Keep track of time elapsed.
		long startTime = System.currentTimeMillis();
		try {
			experiment = new CompoundLeaveOneOutExperiment(packageGeneratorFrame);
			System.out.println(docs);
			experiment.setDocuments(docs);
			experiment.setExperimentTable(expTable);
			packageGeneratorFrame.setStatus("Starting experiment...");

			experiment.runExperiment();

			packageGeneratorFrame.setStatus("Parsing results...");
			parser.setWeightMethod(weightMethod);
			if (weightThreshold > 0)
				parser.setWeightMethodThreshold(weightThreshold);

			parser.parseResults(experiment, weightThreshold <= 0);
			packageGeneratorFrame.setStatus("Done.");
			packageGeneratorFrame.setStatus("Elapsed time: " + ((System.currentTimeMillis() - startTime) / 1000) + " sec");
			JOptionPane.showMessageDialog(packageGeneratorFrame, "Package created successfully. Save package file to use for testing.",
											"Success", JOptionPane.INFORMATION_MESSAGE);

		} catch(IOException e) {
			packageGeneratorFrame.setStatus("Elapsed time: " + ((System.currentTimeMillis() - startTime) / 1000) + " sec");
			packageGeneratorFrame.setStatus("Unable to create compound experiment: " + e);
			JOptionPane.showMessageDialog(packageGeneratorFrame, "Package creation failed. Unable to create compound experiment:\n" + e,
											"Package creation failed", JOptionPane.ERROR_MESSAGE);
		} catch (MissingAuthorException e) {
			packageGeneratorFrame.setStatus("Elapsed time: " + ((System.currentTimeMillis() - startTime) / 1000) + " sec");
			packageGeneratorFrame.setStatus("Unable to parse experiment results: " + e);
			JOptionPane.showMessageDialog(packageGeneratorFrame, "Failed to generate package: " + e,
					"Package creation failed", JOptionPane.ERROR_MESSAGE);
		} catch (ParseException e) {
			packageGeneratorFrame.setStatus("Elapsed time: " + ((System.currentTimeMillis() - startTime) / 1000) + " sec");
			packageGeneratorFrame.setStatus("Unable to parse experiment results: " + e);
			JOptionPane.showMessageDialog(packageGeneratorFrame, "Package creation failed. Unable to parse experiment results:\n" + e,
											"Package creation failed", JOptionPane.ERROR_MESSAGE);
		}  catch(Exception e) {
			packageGeneratorFrame.setStatus("Elapsed time: " + ((System.currentTimeMillis() - startTime) / 1000) + " sec");
			packageGeneratorFrame.setStatus("Unable to run experiment: " + e);
			JOptionPane.showMessageDialog(packageGeneratorFrame, "Package creation failed. Unable to run experiment:\n" + e,
											"Package creation failed", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}
}
