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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import com.jgaap.backend.API;
import com.jgaap.generics.AnalysisDriver;
import com.jgaap.generics.AnalyzeException;
import com.jgaap.generics.Document;
import com.jgaap.generics.EventCuller;
import com.jgaap.generics.EventDriver;
import com.jgaap.generics.NeighborAnalysisDriver;


public abstract class CompoundExperiment
{
	// Fields
	private static Logger logger = Logger.getLogger(CompoundExperiment.class);
	private StatusLog statusLog;

	private Map<String, String> abbrMap;
	private Deque<IndividualExperiment> experiments;
	private List<EventDriver> eventDrivers;
	private List<AnalysisDriver> analysisDrivers;
	private List<EventCuller> eventCullers;

	private StringBuffer results;
	private List<Document> documents;
	private List<List<String>> experimentTable;

	private int minFeature;
	private int maxFeature;

	private final int workers = Runtime.getRuntime().availableProcessors();

	// Constructors
	public CompoundExperiment() throws IOException
	{
		// Do this first, because it could throw an error.
		abbrMap = Utils.getAbbreviationsMap();
		resetExperiment();
	}

	public CompoundExperiment(StatusLog l) throws IOException
	{
		// Do this first, because it could throw an error.
		abbrMap = Utils.getAbbreviationsMap();
		statusLog = l;
		resetExperiment();
	}

	// General public methods
	public void runExperiment() throws Exception
	{
		// Reset results each time we run a new experiment.
		// If there's a specified range of features to test, only test those.
		if (minFeature <= 0 && (maxFeature >= experimentTable.get(Utils.EXPERIMENT_TABLE_FEATURES_ROW).size())) {
			if (!prepareExperimentWithTable(experimentTable))
				throw new ExperimentRunException("Problem running experiment: Unable to use experiment table:\n"
						+ Utils.getStringFromTable(experimentTable, ",", "\n"));
		} else {
			// Because getFeatureExperimentTable(...) defines toIndex as exclusive, we need to add 1 to maxFeature.
			List<List<String>> table = Utils.getFeatureExperimentTable(experimentTable, minFeature, maxFeature+1);
			if (!prepareExperimentWithTable(table))
				throw new ExperimentRunException("Problem running experiment: Unable to use experiment table:\n"
						+ Utils.getStringFromTable(table, ",", "\n"));
		}
		/*
		// Without parallel: about 70 seconds for Adams, Barlow, Benezet, default features/classifiers/cullers
		ExecutorService experimentExecutor = Executors.newFixedThreadPool(workers);
		List<Future<String>> experimentsProcessing = new ArrayList<Future<String>>(experiments.size());
		//for (final IndividualExperiment exp : experiments){
			//statusLog.setStatus("exp: " + exp);
			//experimentsProcessing.add(experimentExecutor.submit(exp));
		//}
		experimentsProcessing.add(experimentExecutor.submit(experiments.pop()));
		experimentsProcessing.add(experimentExecutor.submit(experiments.pop()));
		experimentsProcessing.add(experimentExecutor.submit(experiments.pop()));
		experimentsProcessing.add(experimentExecutor.submit(experiments.pop()));
		experimentExecutor.shutdown();

		while (true) {
			if (experimentsProcessing.size() == 0){
				break;
			} else {
				Iterator<Future<String>> experimentIterator = experimentsProcessing.iterator();
				while (experimentIterator.hasNext()) {
					Future<String> futureExperiment = experimentIterator.next();
					if (futureExperiment.isDone()) {
						try {
						statusLog.setStatus("done |" + futureExperiment.get() + "|");
						//statusLog.setStatus("get: " + futureExperiment.get());
						results.append(futureExperiment.get());
						statusLog.setStatus("woooooo!!!");
						experimentIterator.remove();
						} catch (Exception e) {
							statusLog.setStatus("Ex: " + e);
							e.printStackTrace();
						}
					}
				}
			}
		}
		*/
				/*
				while(documentIterator.hasNext()){
					Future<Document> futureDocument = documentIterator.next();
					if(futureDocument.isDone()){
						Document document = futureDocument.get();
						if (document.hasFailed()){
							throw new Exception("There was a problem processing " + document.getTitle() + ". Experiment failed.");
						}
						logger.info("Document: "+document.getTitle()+" has finished processing.");
						documentIterator.remove();
					}
				}
			}
		}*/

		// Serial version

		while (!experiments.isEmpty()) {
			IndividualExperiment exp = experiments.pop();
			try {
				results.append(exp.call());
			} catch (AnalyzeException e) {
				authorquicktest.Utils.output("problem running experiment " + exp + ": " + e);
				throw e;
			}
		}

	}

	// Accessor methods
	public String getResults()
	{
		return results.toString();
	}

	public List<Document> getDocuments()
	{
		// Send a copy of the list back, so they don't edit
		// the experiment's actual documents list.
		return getCopyOfDocumentsList(documents);
	}

	public List<List<String>> getExperimentTable()
	{
		// Send a copy of the list back, so they don't edit
		// the experiment's actual experiment table.
		return getCopyOfExperimentTable(experimentTable);
	}

	// Mutator methods
	public boolean setTestDocuments(List<Document> testDocs)
	{
		// Reset results each time we run a new experiment.
		results = new StringBuffer("");
		// Remove all old test documents.
		for (Iterator<Document> it = documents.iterator(); it.hasNext(); ) {
			Document doc = it.next();
			if (doc.getAuthor() == null)
				it.remove();
		}

		// Make sure it is a test document (unknown author).
		// Then make a copy and add it to the document list.
		for (Document doc : testDocs) {
			if (doc.getAuthor() != null)
				return false;
			else
				documents.add(new Document(doc));
		}
		return true;
	}

	public void setFeatureRange(int min, int max)
	{
		minFeature = min;
		maxFeature = max;
	}

	public StatusLog getStatusLog() { return statusLog; }
	public void setStatusLog(StatusLog l) { statusLog = l; }

	// Protected mutators
	protected void setExperimentTable(List<List<String>> table)
	{
		experimentTable = getCopyOfExperimentTable(table);
		minFeature = 0;
		maxFeature = table.get(Utils.EXPERIMENT_TABLE_FEATURES_ROW).size() - 1;
	}

	protected void setDocuments(List<Document> docs)
	{
		documents = getCopyOfDocumentsList(docs);
	}

	protected void resetExperiment()
	{
		experiments = new ArrayDeque<IndividualExperiment>();
		eventDrivers = new ArrayList<EventDriver>();
		analysisDrivers = new ArrayList<AnalysisDriver>();
		eventCullers = new ArrayList<EventCuller>();

		results = new StringBuffer();
		documents = new ArrayList<Document>();
		experimentTable = new ArrayList<List<String>>();
	}

	// Protected methods
	protected abstract void createIndividualExperiments(List<Document> documents);

	protected void addIndividualExperiment(IndividualExperiment exp)
	{
		experiments.add(exp);
	}

	// Private methods
	private void loadDocumentsIntoApi(API api)
	{
		// Load the documents into the API.
		// Use getDocuments(), because we want to maintain the original
		// documents as they are in case we run another experiment, and
		// getDocuments() will send back a copy of the documents, not
		// the originals.
		List<Document> docs = getDocuments();
		for (Document document : docs) {
			api.addDocument(document);
		}
	}

	/**
	 * Event drivers, analysis drivers, event cullers, and
	 * canonicizers all need to be loaded into the API.
	 * This is done by passing strings, which the API either
	 * understands and loads the corresponding component,
	 * or throws an error.
	 * @param table
	 * @param api
	 * @throws Exception
	 */
	private void loadExperimentTableIntoApi(List<List<String>> table, API api) throws Exception
	{
		List<String> eventDriverList = new ArrayList<String>();
		List<String> analysisDriverList = new ArrayList<String>();
		List<String> eventCullerList = new ArrayList<String>();
		List<String> canonicizerList = new ArrayList<String>();

		eventDrivers.clear();
		analysisDrivers.clear();
		eventCullers.clear();

		Utils.getExperimentComponentsFromExperimentTable(table,
				eventDriverList, analysisDriverList, eventCullerList, canonicizerList);

		try {
			// Load canonicizers into the API.
			for (String canonicizer : canonicizerList) {
				if(!canonicizer.isEmpty())
					api.addCanonicizer(canonicizer);
			}
			// Load event drivers into the API.
			for (String ed : eventDriverList) {
				eventDrivers.add(api.addEventDriver(ed));
			}
			// Load event cullers into the API.
			for (String eventCuller : eventCullerList) {
				if (eventCuller != null && !"".equalsIgnoreCase(eventCuller))
					eventCullers.add(api.addEventCuller(eventCuller.trim()));
			}
			// Load analysis drivers into the API.
			for (String ad : analysisDriverList) {
				String[] driverArray = ad.split(";");

				String driver = driverArray[0];
				AnalysisDriver analysisDriver = api.addAnalysisDriver(driver);

				if (driverArray.length > 1) {
					String distance = driverArray[1];
					NeighborAnalysisDriver neighborAnalysisDriver = (NeighborAnalysisDriver)analysisDriver;
					if (neighborAnalysisDriver != null) {
						neighborAnalysisDriver.setDistance(api.addDistanceFunction(distance, analysisDriver));
					}
				}
				analysisDrivers.add(analysisDriver);

			}
		} catch (Exception e) {
			logger.error("Failed to load experiment component into API...", e);
			throw new LoadExperimentException("Unable to run experiment. There was a problem loading experiment components into the API: " + e);
		}
	}







	private void loadExperimentTableIntoApi2(List<List<String>> table, API api) throws Exception
	{
		List<String> eventDriverList = new ArrayList<String>();
		List<String> analysisDriverList = new ArrayList<String>();
		List<String> eventCullerList = new ArrayList<String>();
		List<String> canonicizerList = new ArrayList<String>();

		analysisDrivers.clear();

		Utils.getExperimentComponentsFromExperimentTable(table,
				eventDriverList, analysisDriverList, eventCullerList, canonicizerList);

		try {
			// Load event drivers into the API.
			for (String ed : eventDriverList) {
				api.addEventDriver(ed);
			}
			// Load analysis drivers into the API.
			for (String ad : analysisDriverList) {
				String[] driverArray = ad.split(";");

				String driver = driverArray[0];
				AnalysisDriver analysisDriver = api.addAnalysisDriver(driver);

				if (driverArray.length > 1) {
					String distance = driverArray[1];
					NeighborAnalysisDriver neighborAnalysisDriver = (NeighborAnalysisDriver)analysisDriver;
					if (neighborAnalysisDriver != null) {
						neighborAnalysisDriver.setDistance(api.addDistanceFunction(distance, analysisDriver));
					}
				}
				analysisDrivers.add(analysisDriver);

			}
		} catch (Exception e) {
			logger.error("Failed to load experiment component into API...", e);
			throw new LoadExperimentException("Unable to run experiment. There was a problem loading experiment components into the API: " + e);
		}
	}







	/**
	 *
	 * @return {boolean} - false if there aren't documents, event drivers, or analysis drivers,
	 * 		or if the feature range is out of bounds; true otherwise
	 * @throws Exception
	 */
	private boolean prepareExperimentWithTable(List<List<String>> table) throws Exception
	{
		if (table == null || table.isEmpty() || documents == null || documents.isEmpty())
			return false;

		API api = API.getPrivateInstance();
		api.setStatusLog(statusLog);

		loadDocumentsIntoApi(api);
		loadExperimentTableIntoApi(table, api);
		prepareApi(api);

		if (eventDrivers.isEmpty() || analysisDrivers.isEmpty())
			return false;

		createIndividualExperiments(api.getDocuments());
		return true;
	}


	/**
	 * Prepare the given API and alert user of any errors that
	 * occur. Preparing includes clearing event sets from the
	 * documents, reloading documents, canonicizing documents,
	 * creating events sets for the documents, and culling
	 * the results.
	 * @param api
	 * @throws Exception
	 */
	private void prepareApi(API api) throws Exception
	{
		try {
			api.prepare();
		} catch (Exception e) {
			logger.error("Failed to prepare API...", e);
			throw new Exception("Unable to run experiment. There was a problem preparing the API: " + e);
		}
	}


	// Private methods
	private List<List<String>> getCopyOfExperimentTable(List<List<String>> oldTable)
	{
		List<List<String>> table = new ArrayList<List<String>>(oldTable.size());
		for (List<String> row : oldTable) {
			List<String> tableRow = new ArrayList<String>(row.size());
			tableRow.addAll(row);
			table.add(tableRow);
		}
		return table;
	}

	private List<Document> getCopyOfDocumentsList(List<Document> oldList)
	{
		List<Document> docs = new ArrayList<Document>(oldList.size());
		for (Document doc : oldList)
			docs.add(new Document(doc));
		return docs;
	}

	// Private classes

	// Implements Callable because it originally was called using
	// an ExecutorService object. We'll leave it in case we want
	// to go back to that method.
	protected class IndividualExperiment implements Callable<String>
	{
		protected List<Document> expDocuments;

		public IndividualExperiment(List<Document> docs)
		{
			this.expDocuments = docs;
		}

		@Override
		public String call() throws AnalyzeException {
			API api = getSetupApi();
			api.setStatusLog(statusLog);

			try {
				api.analyzeTestDocuments();

				List<Document> unknowns = api.getUnknownDocuments();
				StringBuffer buffer = new StringBuffer();

				for (Document unknownDocument : unknowns)
					buffer.append(getResultsForDocument(unknownDocument));

				return buffer.toString();

			} catch (AnalyzeException e) {
				// TODO: Don't use fileName...
				String fileName = "";
				logger.error("Could not run experiment " + fileName, e);
				Utils.output("Could not run experiment " + fileName + ": " + e);
				throw new AnalyzeException("There was a problem analyzing the test document " + fileName + ": " + e);
			}
		}

		/**
		 * Creates an API and sets it up with the documents,
		 * event drivers, and analysis drivers it needs to
		 * run the experiment.
		 * @return {API} - the set up API
		 */
		protected API getSetupApi()
		{
			API api = API.getPrivateInstance();

			api.setDocuments(expDocuments);
			/*
			List<EventDriver> eds = new ArrayList<EventDriver>(eventDrivers.size());
			for (EventDriver eventDriver : eventDrivers)
				eds.add(eventDriver);
			List<AnalysisDriver> ads = new ArrayList<AnalysisDriver>(analysisDrivers.size());
			for (AnalysisDriver analysisDriver : analysisDrivers)
				ads.add(analysisDriver);

			try {
				loadExperimentTableIntoApi2(experimentTable, api);
			} catch (Exception e) {
				statusLog.setStatus("Problem loading experiment table into API: " + e);
			}
			api.setEventDrivers(eds);
			//api.setAnalysisDrivers(ads);*/
			api.setEventDrivers(eventDrivers);
			api.setAnalysisDrivers(analysisDrivers);

			return api;
		}

		/**
		 * Formats the results for a given document so that they can
		 * be parsed by the results parser.
		 * @param {Document} unknownDocument - the document to analyze
		 * @return {String} - a string of the results of the analysis
		 */
		protected String getResultsForDocument(Document unknownDocument)
		{
			StringBuffer buffer = new StringBuffer();
			for (AnalysisDriver analysisDriver : analysisDrivers) {
				for (EventDriver eventDriver : eventDrivers) {
					// Most of this here is to format the name of the experiment correctly.
					String analysisDriverName = analysisDriver.displayName();
					String distance = "";

					// If the analysis driver uses a distance function, we need to include that in
					// the display name. If it cannot be cast into a NeighborAnalysisDriver, we'll
					// just catch the exception and do nothing.
					try {
						NeighborAnalysisDriver neighborAnalysisDriver = (NeighborAnalysisDriver)analysisDriver;
						if (neighborAnalysisDriver != null) {
							distance = ";" + neighborAnalysisDriver.getDistanceFunction().displayName();
						}

						// Get rid of "with metric..."
						// I feel like there's a better way of dealing with this.
						List<String> adNameArray = Arrays.asList(analysisDriverName.split(" "));
						int index = adNameArray.indexOf("with");
						StringBuilder adName = new StringBuilder();
						for (int i = 0; i < index; i++) {
							adName.append(adNameArray.get(i) + " ");
						}
						analysisDriverName = adName.toString().trim();

					} catch (ClassCastException e) {}

					if (eventCullers.size() > 1)
						Utils.output("More than one event culler..." + eventCullers);

					String k = analysisDriver.getParameter("k");
					String n = eventDriver.getParameter("n");
					String experimentName =
							  abbrMap.get(analysisDriverName + distance) + k
							+ abbrMap.get(eventDriver.displayName()) + n
							+ abbrMap.get(eventCullers.get(0).displayName());
					buffer.append(experimentName + "\n");
					buffer.append(unknownDocument.getFormattedResult(analysisDriver, eventDriver));
				}
			}
			return buffer.toString();
		}
	}
}
