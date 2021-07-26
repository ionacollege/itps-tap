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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import com.jgaap.backend.API;
import com.jgaap.generics.AnalyzeException;
import com.jgaap.generics.Document;

public class CompoundLeaveOneOutExperiment extends CompoundExperiment
{
	private static Logger logger = Logger.getLogger(CompoundLeaveOneOutExperiment.class);

	// Constructors
	public CompoundLeaveOneOutExperiment() throws IOException
	{
		super();
	}
	public CompoundLeaveOneOutExperiment(StatusLog l) throws IOException
	{
		super(l);
	}

	// Protected methods
	protected void createIndividualExperiments(List<Document> documents)
	{
		logger.info("Creating individual experiments.");
		for (int i = 0; i < documents.size(); i++) {
			List<Document> docs = new ArrayList<Document>(documents.size());
			for (Document doc : documents)
				docs.add(new Document(doc));

			Document doc = docs.get(i);
			addIndividualExperiment(new LeaveOneOutExperiment(docs, doc));
		}
	}

	// Private classes
	private class LeaveOneOutExperiment
	extends IndividualExperiment implements Callable<String> {

		private Document unknownDocument;

		public LeaveOneOutExperiment(
				List<Document> documents,
				Document unknownDocument) {
			super(documents);
			this.unknownDocument = unknownDocument;
		}

		@Override
		public String call() throws AnalyzeException {
			//getStatusLog().setStatus("1");
			API api = getSetupApi();
			//getStatusLog().setStatus("2");
			api.setStatusLog(getStatusLog());

			//getStatusLog().setStatus("3");
			logger.info("Setting unknown document: " + unknownDocument);
			//getStatusLog().setStatus("4");
			api.setUnknownDocument(unknownDocument);

			try {
				//getStatusLog().setStatus("5");
				api.analyzeLeaveOneOut();
				//getStatusLog().setStatus("6");
				String docResults = getResultsForDocument(unknownDocument);
				//getStatusLog().setStatus("7");
				return docResults;
			} catch (AnalyzeException e) {
				//getStatusLog().setStatus("8");
				// TODO: Don't use fileName...
				String fileName = "";
				//logger.error("Could not run experiment " + fileName, e);
				logger.error("Could not run experiment for unknown document: " + unknownDocument, e);
				throw new AnalyzeException("There was a problem analyzing the document " + fileName + ": " + e);
			}
		}

	}
}
