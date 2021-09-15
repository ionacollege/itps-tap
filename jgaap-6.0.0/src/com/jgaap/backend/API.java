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
 * This file authored by Sean Campbell 2014-2021
 *
 * This is an experimental package and will be cleaned up and published with
 * a more recent version of the JGAAP software.
 */
package com.jgaap.backend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import com.jgaap.classifiers.NearestNeighborDriver;
import com.jgaap.generics.*;
import com.jgaap.languages.English;

import authorquicktest.StatusLog;

/**
 *
 * This class provides a simple interface into jgaap for use in
 * other software packages and for development of any human interfaces.
 *
 * Instructions for using the JGAAP API:
 *
 * First add documents both known and unknown
 *
 * All other settings can be performed in any order which are setLanguage, addCanonicizer,
 * addEventDriver, addEventCuller, addAnalysisDriver, addDistanceFunction
 * Note: of the settings only one EventDriver and one AnalysisDriver are required to run an experiment
 *
 * The execute method is then used to start the experiment running
 *
 * Results are placed in unknown documents to access them simple use the getUnknownDocuments method in the API
 * The results can be retrieved as a List<Pair<String, Double>> this is a sorted list
 * from most likely to least likely author followed by a score generated based on your settings using the getRawResult method
 * You can also get a Map of Maps of the raw results (Map<EventDriver, Map<AnalysisDriver, List<String, Double>>>) with the getRawResults method
 * They can also be retrieved as a string using either the getFormattedResult or getResult methods.
 *
 * For examples of how to use the API class see the com.jgaap.ui package for a GUI example
 * or the com.jgaap.backend.CLI class for a command line example
 *
 * @author Michael Ryan
 * @since 5.0.0
 */
public class API {

	static Logger logger = Logger.getLogger(API.class);

	private Document unknownDocument;
	private List<Document> documents;
	private Language language;
	private List<EventDriver> eventDrivers;
	private List<EventCuller> eventCullers;
	private List<AnalysisDriver> analysisDrivers;
	private StatusLog statusLog;

	private final int workers = Runtime.getRuntime().availableProcessors();

	private static final API INSTANCE = new API();

	private API() {
		unknownDocument = null;
		documents = new ArrayList<Document>();
		language = new English();
		eventDrivers = new ArrayList<EventDriver>();
		eventCullers = new ArrayList<EventCuller>();
		analysisDrivers = new ArrayList<AnalysisDriver>();
	}

	/**
	 * This allows a singleton of the api to be used in the gui
	 * or any program that needs to access a single copy of JGAAP
	 * from multiple classes
	 *
	 * @return a reference to the singleton API
	 */
	public static API getInstance(){
		return INSTANCE;
	}

	/**
	 * This is a unique instance of the api to be used when running
	 * bulk experiments and you want to reset everything or if you
	 * want to thread running more than one experiment at a time
	 * as in the class com.jgaap.backend.ExperimentEngine
	 *
	 * @return a unique API instance
	 */
	public static API getPrivateInstance(){
		return new API();
	}

	/**
	 * Sets the status log to output important
	 * general messages to the user.
	 * @param log
	 */
	public void setStatusLog(StatusLog log) {
		statusLog = log;
	}

	/**
	 *
	 * This allows for the addition of documents to the system.
	 * Both Training (known) and Sample (unknown) documents must be provided before running an experiment.
	 * Training Documents are added by providing an author(tag) for them.
	 * Sample documents are added when no author(tag) is given.
	 *
	 * @param filepath - the system file path or URL to a document
	 * @param author - the author of this document or the tag being applied to this document, if null or the empty string this document is considered unknown and is one of those classified
	 * @param title - Some means of identifying the document, if null or the empty string are provided a title will be generated from the file name
	 * @return - a reference to the document generated
	 * @throws Exception - if there is a problem loading the document from file web or parsing file format
	 */
	public Document addDocument(String filepath, String author, String title)
	{
		Document document = new Document(filepath, author, title);
		documents.add(document);
		logger.info("Adding Document "+document.toString());
		return document;
	}

	/**
	 * Adds a previously generated document to the jgaap system.
	 *
	 * @param document - a file that has already been loaded as a Document
	 * @return - a reference to the document generated
	 */
	public Document addDocument(Document document) {
		documents.add(document);
		logger.info("Adding Document "+document.toString());
		return document;
	}

	/**
	 * Removes a document from the system.
	 *
	 * @param document - a reference to the document that is to be removed
	 * @return - true on success false on failure
	 */
	public Boolean removeDocument(Document document) {
		logger.info("Removing Document "+document.toString());
		return documents.remove(document);
	}

	/**
	 * Removes all documents loaded into the system.
	 */
	public void removeAllDocuments() {
		documents.clear();
	}

	/**
	 * Get a List of all Documents currently loaded into jgaap
	 *
	 * @return - a List of Documents loaded into the system
	 */
	public List<Document> getDocuments() {
		return documents;
	}

	/**
	 * Get a List of all currently loaded Documents that do not have an author(tag)
	 *
	 * @return List of Documents without authors
	 */
	public List<Document> getUnknownDocuments() {
		List<Document> unknownDocuments = new ArrayList<Document>();
		for (Document document : documents) {
			if (!document.isAuthorKnown()) {
				unknownDocuments.add(document);
			}
		}
		return unknownDocuments;
	}

	/**
	 * Get a List of Documents currently loaded into the system that have a author(tag)
	 *
	 * @return List of Documents with authors
	 */
	public List<Document> getKnownDocuments() {
		List<Document> knownDocuments = new ArrayList<Document>();
		for (Document document : documents) {
			if (document.isAuthorKnown()) {
				knownDocuments.add(document);
			}
		}
		return knownDocuments;
	}

	/**
	 * Get a List of Documents that all have the same author(tag)
	 *
	 * @param author - the author(tag) to select documents on
	 * @return - List of Documents limited by the author provided
	 */
	public List<Document> getDocumentsByAuthor(String author) {
		List<Document> authorDocuments = new ArrayList<Document>();
		for (Document document : documents) {
			if (document.isAuthorKnown()) {
				if (author.equalsIgnoreCase(document.getAuthor())) {
					authorDocuments.add(document);
				}
			}
		}
		return authorDocuments;
	}

	/**
	 * Get a List of all unique authors(tags) applied to Known(Training) Documents
	 *
	 * @return List of authors
	 */
	public List<String> getAuthors() {
		Set<String> authors = new HashSet<String>();
		for (Document document : documents) {
			if (document.isAuthorKnown()) {
				authors.add(document.getAuthor());
			}
		}
		List<String> authorsList = new ArrayList<String>(authors);
		Collections.sort(authorsList);
		return authorsList;
	}

	/**
	 * Loads the documents from the file system
	 * @throws Exception
	 */
	public void loadDocuments() throws Exception{
		for(Document document : documents){
			document.load();
		}
	}

	/**
	 * Adds the specified canonicizer to all documents currently loaded in the system.
	 *
	 * @param action - the unique string name representing a canonicizer (displayName())
	 * @throws Exception - if the canonicizer specified cannot be found or instanced
	 */
	public void addCanonicizer(String action) throws Exception {
		for (Document document : documents) {
			addCanonicizer(action, document);
		}
	}

	/**
	 * Adds the specified canonicizer to all Documents that have the DocType docType.
	 *
	 * @param action - the unique string name representing a canonicizer (displayName())
	 * @param docType - The DocType this canonicizer is restricted to
	 * @throws Exception - if the canonicizer specified cannot be found or instanced
	 */
	public void addCanonicizer(String action, DocType docType) throws Exception {
		for (Document document : documents) {
			if (document.getDocType().equals(docType)) {
				addCanonicizer(action, document);
			}
		}
	}

	/**
	 * Add the Canonicizer specified to the document referenced.
	 *
	 * @param action - the unique string name representing a canonicizer (displayName())
	 * @param document - the Document to add the canonicizer to
	 * @return - a reference to the canonicizer added
	 * @throws Exception - if the canonicizer specified cannot be found or instanced
	 */
	public Canonicizer addCanonicizer(String action, Document document)
			throws Exception {
		Canonicizer canonicizer = CanonicizerFactory.getCanonicizer(action);
		document.addCanonicizer(canonicizer);
		logger.info("Adding Canonicizer "+canonicizer.displayName()+" to Document "+document.toString());
		return canonicizer;
	}

	/**
	 * Removes the first instance of the canoniciser corresponding to the action(displayName())
	 * from the Document referenced.
	 *
	 * @param action - the unique string name representing a canonicizer (displayName())
	 * @param document - a reference to the Document to remove the canonicizer from
	 */
	public void removeCanonicizer(String action, Document document) {
		document.removeCanonicizer(action);
	}

	/**
	 * Removes the first occurrence of the canonicizer corresponding to the action(displayName())
	 * from every document
	 *
	 * @param action - the unique string name representing a canonicizer (displayName())
	 */
	public void removeCanonicizer(String action) {
		for (Document document : documents) {
			removeCanonicizer(action, document);
		}
	}

	/**
	 * Removes the first occurrence of the canonicizer from every Document of the DocType docType
	 *
	 * @param action - the unique string name representing a canonicizer (displayName())
	 * @param docType - the DocType to remove the canonicizer from
	 */
	public void removeCanonicizer(String action, DocType docType) {
		for (Document document : documents) {
			if (document.getDocType().equals(docType)) {
				removeCanonicizer(action, document);
			}
		}
	}

	/**
	 * Removes all canonicizers from Documents with the DocType docType
	 *
	 * @param docType - the DocType to remove canonicizers from
	 */
	public void removeAllCanonicizers(DocType docType) {
		for (Document document : documents) {
			document.clearCanonicizers();
		}
	}

	/**
	 * Removes all canonicizers from All Documents loaded in the system
	 */
	public void removeAllCanonicizers() {
		for (Document document : documents) {
			document.clearCanonicizers();
		}
	}

	/**
	 * Add an Event Driver which will be used to
	 * eventify(Generate a List of Events order in the sequence they are found in the document)
	 * all of the documents
	 * @param action - the identifier for the EventDriver to add (displayName())
	 * @return - a reference to the added EventDriver
	 * @throws Exception - If the action is not found or the EventDriver cannot be instanced
	 */
	public EventDriver addEventDriver(String action) throws Exception {
		EventDriver eventDriver = EventDriverFactory.getEventDriver(action);
		eventDrivers.add(eventDriver);
		logger.info("Adding EventDriver "+eventDriver.displayName());
		return eventDriver;
	}

	/**
	 * Removes the Event Driver reference from the system
	 * @param eventDriver - the EventDriver to be removed
	 * @return - true if successful false if failure
	 */
	public Boolean removeEventDriver(EventDriver eventDriver) {
		logger.info("Removing EventDriver "+eventDriver.displayName());
		return eventDrivers.remove(eventDriver);
	}

	/**
	 * Removes all EventDrivers from the system
	 */
	public void removeAllEventDrivers() {
		eventDrivers.clear();
		for (Document document : documents) {
			document.clearEventSets();
		}
	}

	/**
	 * Gets a List of all EventDrivers currently loaded in the system
	 * @return List of All loaded EventDrivers
	 */
	public List<EventDriver> getEventDrivers() {
		return eventDrivers;
	}

	/**
	 * Add an Event Culler to the system
	 *
	 * @param action - unique identifier for the event culler to add (displayName())
	 * @return - a reference to the added event culler
	 * @throws Exception - if the EventCuller cannot be found or cannor be instanced
	 */
	public EventCuller addEventCuller(String action) throws Exception {
		EventCuller eventCuller = EventCullerFactory.getEventCuller(action);
		eventCullers.add(eventCuller);
		logger.info("Adding EventCuller "+eventCuller.displayName());
		return eventCuller;
	}

	/**
	 * Remove the supplied EventCuller from the system
	 *
	 * @param eventCuller - EventCuller to be removed
	 * @return - true if success false if failure
	 */
	public Boolean removeEventCuller(EventCuller eventCuller) {
		logger.info("Removing EventCuller "+eventCuller.displayName());
		return eventCullers.remove(eventCuller);
	}

	/**
	 * Removes all loaded EventCullers from the system
	 */
	public void removeAllEventCullers() {
		eventCullers.clear();
	}

	/**
	 * Get a List of all EventCullers currently loaded in the system
	 * @return List of EventCullers loaded
	 */
	public List<EventCuller> getEventCullers() {
		return eventCullers;
	}

	/**
	 * Add an AnalysisDriver to the system as referenced by the action.
	 *
	 * NOTE! for legacy purposes this methods also accepts actions that reference DistanceFunctions
	 *  it will use the supplied distance function along with the NeighborAnalysisDriver NearestNeighborDriver
	 *  which had been the only NeighbotAnalysisDriver prior to version 5.0
	 *  !!!WARNING!!! There are no guarantees that this functionality will remain in future releases please use addDistanceFunction
	 *
	 * @param action - the unique identifier for a AnalysisDriver (alternately a DistanceFunction)
	 * @return - a reference to the generated Analysis Driver
	 * @throws Exception - If the AnalysisDriver cannot be found or if it cannot be instanced
	 */
	public AnalysisDriver addAnalysisDriver(String action) throws Exception {
		AnalysisDriver analysisDriver;
		try {
			analysisDriver = AnalysisDriverFactory.getAnalysisDriver(action);
		} catch (Exception e) {
			logger.warn("Unable to load action "+action+" as AnalysisDriver attempting to load as DistanceFunction using NearestNeighborDriver", e);
			analysisDriver = new NearestNeighborDriver();
			addDistanceFunction(action, analysisDriver);
		}
		logger.info("Adding AnalysisDriver "+analysisDriver.displayName());
		analysisDrivers.add(analysisDriver);
		return analysisDriver;
	}

	/**
	 * Removed the passed AnalysisDriver from the system
	 * @param analysisDriver - reference to the AnalysisDriver to be removed
	 * @return True if success false if failure
	 */
	public Boolean removeAnalysisDriver(AnalysisDriver analysisDriver) {
		logger.info("Removing AnalysisDriver "+analysisDriver.displayName());
		return analysisDrivers.remove(analysisDriver);
	}

	/**
	 * Removes all AnalysisDrivers from the system
	 */
	public void removeAllAnalysisDrivers() {
		analysisDrivers.clear();
	}

	/**
	 * Adds a DistanceFunction to the AnalysisDriver supplied.
	 * Only AnalysisDrivers that extend the NeighborAnalysisDriver can be used
	 *
	 * @param action - unique identifier for the DistanceFunction you want to add
	 * @param analysisDriver - a reference to the AnalysisDriver you want the distance added to
	 * @return - a reference to the generated DistanceFunction
	 * @throws Exception - if the AnalysisDriver does not extend NeighborAnalysisDriver or if the DistanceFunction cannot be found the DistanceFunction cannot be instanced
	 */
	public DistanceFunction addDistanceFunction(String action,
			AnalysisDriver analysisDriver) throws Exception {
		DistanceFunction distanceFunction = DistanceFunctionFactory
				.getDistanceFunction(action);
		((NeighborAnalysisDriver) analysisDriver).setDistance(distanceFunction);
		return distanceFunction;
	}

	/**
	 * Get a List of All AnalysisDrivers currently loaded on the system
	 * @return List of All AnalysisDrivers
	 */
	public List<AnalysisDriver> getAnalysisDrivers() {
		return analysisDrivers;
	}

	/**
	 * Get the current Language JGAAP is set to be working on
	 * @return
	 */
	public Language getLanguage(){
		return language;
	}

	/**
	 * Set the Language that JGAAP will operate in.
	 * This restricts what methods are available, changes the charset that is expected when reading files, and will add any pre-processing that is needed
	 * @param action - the Language to operate under
	 * @return - a Reference to the language object selected
	 * @throws Exception - if the language cannot be found or cannot be instanced
	 */
	public Language setLanguage(String action) throws Exception {
		language = LanguageFactory.getLanguage(action);
		return language;
	}

	/**
	 * Pipelines the independent aspects of loading and processing a document into separate threads
	 *
	 * Load the text from disk or the web
	 * Take into account any special treatment based on the language currently selected
	 * Place the text into canonical form using the Canonicizers
	 * Use the EventDrivers to transform the text into EventSets
	 *
	 * @throws Exception
	 */
	private void loadCanonicizeEventify() throws Exception {
		ExecutorService loadCanonicizeEventifyExecutor = Executors.newFixedThreadPool(workers);
		List<Future<Document>> documentsProcessing = new ArrayList<Future<Document>>(documents.size());
		for(final Document document : documents){
			Callable<Document> work = new Callable<Document>() {
				@Override
				public Document call() throws Exception {
					try {
						document.setLanguage(language);
						//document.load();
						document.processCanonicizers();
						for (EventDriver eventDriver : eventDrivers) {
							try{
								document.addEventSet(eventDriver,eventDriver.createEventSet(document));
							} catch (EventGenerationException e) {
								logger.error("Could not Eventify with "+eventDriver.displayName()+" on File:"+document.getFilePath()+" Title:"+document.getTitle(),e);
								throw new Exception("Could not Eventify with "+eventDriver.displayName()+" on File:"+document.getFilePath()+" Title:"+document.getTitle(),e);
							}
						}
						document.readStringText("");
					} catch (LanguageParsingException e) {
						logger.fatal("Could not Parse Language: "+language.displayName()+" on File:"+document.getFilePath()+" Title:"+document.getTitle(),e);
						document.failed();
						throw e;
					} catch (CanonicizationException e) {
						logger.fatal("Could not Canonicize File: "+document.getFilePath()+" Title:"+document.getTitle(),e);
						document.failed();
						throw e;
					} catch (Exception e) {
						logger.fatal("Could not load File: "+document.getFilePath()+" Title:"+document.getTitle(),e);
						document.failed();
						throw e;
					}
					return document;
				}

			};
			documentsProcessing.add(loadCanonicizeEventifyExecutor.submit(work));
		}
		loadCanonicizeEventifyExecutor.shutdown();

		int doneCount = 0;
		while(true){
			if (documentsProcessing.size()==0){
				break;
			} else {
				Iterator<Future<Document>> documentIterator = documentsProcessing.iterator();
				while(documentIterator.hasNext()){
					Future<Document> futureDocument = documentIterator.next();
					if(futureDocument.isDone()){
						Document document = futureDocument.get();
						if (document.hasFailed()){
							throw new Exception("There was a problem processing " + document.getTitle() + ". Experiment failed.");
						}
						if (statusLog != null)
							statusLog.setStatus("Document " + (++doneCount) + " of " + documents.size() + ": " + document + " has finished processing.");
						logger.info("Document: "+document.getTitle()+" has finished processing.");
						documentIterator.remove();
					}
				}
			}
		}
	}

	/**
	 * Events are culled from EventSets across all Documents on a per EventDriver basis
	 * @throws EventCullingException
	 */
	private void cull() throws EventCullingException {
		logger.info("culling documents");
		if(eventCullers.isEmpty()) return;
		List<EventSet> eventSets = new ArrayList<EventSet>(documents.size());
		for (EventDriver eventDriver : eventDrivers) {
			for (Document document : documents) {
				eventSets.add(document.getEventSet(eventDriver));
			}

			for (EventCuller culler : eventCullers) {
				try {
					eventSets = culler.cull(eventSets);
				} catch (EventCullingException e) {
					logger.error("Problem culling: " + e + ". Using existing events without culling.");
					if (statusLog != null)
						statusLog.setStatus("Problem culling: " + e + ". Using existing events.");
					else throw e;
				}
			}
			for (Document document : documents) {
				document.addEventSet(eventDriver, eventSets.remove(0));
			}
			eventSets.clear();
		}
	}

	/**
	 * All loaded AnalysisDrivers are run over All EventSets comparing the Unknown(sample) to the Known(training) Documents.
	 */
	public void analyzeLeaveOneOut() throws AnalyzeException {
		//logger.info("analyze leave-one-out on unknown document: " + unknownDocument);
		if (statusLog != null)
			statusLog.setStatus("Leaving out: " + unknownDocument);
		int i = 0;
		//logger.info("event drivers (" + eventDrivers.size() + "): " + eventDrivers);
		//logger.info("analysis drivers (" + analysisDrivers.size() + "): " + analysisDrivers);
		//statusLog.setStatus("01");
		for (EventDriver eventDriver : eventDrivers) {
			//statusLog.setStatus("02");
			logger.info("analyzing event set for event driver: " + eventDriver.displayName());
			if (statusLog != null)
				statusLog.setStatus("\tAnalyzing feature " + (++i) + " of " + eventDrivers.size() + "..." + eventDriver.displayName());

			//statusLog.setStatus("03");
			List<EventSet> knownEventSets = new ArrayList<EventSet>(documents.size());
			for (Document knownDocument : documents) {
				//statusLog.setStatus("04");
				//statusLog.setStatus("es " + knownDocument.getEventSet(eventDriver));
				knownEventSets.add(knownDocument.getEventSet(eventDriver));
			}
			//statusLog.setStatus("05");
			for (AnalysisDriver analysisDriver : analysisDrivers) {
				//statusLog.setStatus("06");
				logger.info("training " + analysisDriver.displayName());
				analysisDriver.train(knownEventSets);
				//ExecutorService analysisExecutor = Executors.newFixedThreadPool(workers);
				//statusLog.setStatus("07");
				if (analysisDriver instanceof ValidationDriver) {
					//statusLog.setStatus("08");
					for (Document knownDocument : documents) {
						//TODO: change to threaded here
						logger.info("Analyzing "+knownDocument.toString());
						knownDocument.addResult(analysisDriver, eventDriver,analysisDriver.analyze(knownDocument.getEventSet(eventDriver)));
					}
				} else {
					//statusLog.setStatus("09");
					//TODO: change to threaded here
					logger.info("Analyzing "+unknownDocument.toString());
					List<Pair<String, Double>> tmp = analysisDriver.analyze(unknownDocument.getEventSet(eventDriver));
					unknownDocument.addResult(analysisDriver, eventDriver, tmp);
				}
				//statusLog.setStatus("010");
			}
		}
	}

	/**
	 * All loaded AnalysisDrivers are run over All EventSets comparing the Unknown(sample) to the Known(training) Documents.
	 */
	public void analyzeTestDocuments() throws AnalyzeException {

		List<Document> knownDocuments = new ArrayList<Document>();
		List<Document> unknownDocuments = new ArrayList<Document>();
		for (Document document : documents) {
			if (document.isAuthorKnown()) {
				knownDocuments.add(document);
			} else {
				unknownDocuments.add(document);
			}
		}
		int i = 0;
		for (EventDriver eventDriver : eventDrivers) {
			if (statusLog != null)
				statusLog.setStatus("Running feature " + (++i) + " of " + eventDrivers.size() + "..." + eventDriver.displayName());
			List<EventSet> knownEventSets = new ArrayList<EventSet>(documents.size());

			System.out.println("KnownDocs: " + knownDocuments);
			StringBuffer kes = new StringBuffer();
			for (Document knownDocument : knownDocuments) {
				knownEventSets.add(knownDocument.getEventSet(eventDriver));
				kes.append(knownDocument.getEventSet(eventDriver)).append("\n\n");
			}

			for (AnalysisDriver analysisDriver : analysisDrivers) {
				logger.info("Training "+analysisDriver.displayName());
				analysisDriver.train(knownEventSets);
				//ExecutorService analysisExecutor = Executors.newFixedThreadPool(workers);
				if (analysisDriver instanceof ValidationDriver) {
					for (Document knownDocument : documents) {
						//TODO: change to threaded here
						logger.info("Analyzing "+knownDocument.toString());
						knownDocument.addResult(analysisDriver, eventDriver,analysisDriver.analyze(knownDocument.getEventSet(eventDriver)));
					}
				} else {
					for (Document unknown : unknownDocuments) {
						//TODO: change to threaded here
						logger.info("Analyzing "+unknown.toString());
						List<Pair<String, Double>> tmp = analysisDriver.analyze(unknown.getEventSet(eventDriver));
						unknown.addResult(analysisDriver, eventDriver,tmp);
					}
				}
			}
		}
	}

	/**
	 * Performs the canonicize eventify cull and analyze methods since a strict order has to be enforced when using them
	 * @throws Exception
	 */
	public void execute() throws Exception {
		//clearData();
		//loadCanonicizeEventify();
		//cull();
		analyzeTestDocuments();
	}

	public void prepare() throws Exception {
		logger.info("preparing API");
		clearData();
		loadCanonicizeEventify();
		cull();
	}

	/**
	 * This method is used in conjunction with a leave one out experiment.
	 * It searches for the document with the same title and author as the
	 * document given, removes it from the list of documents, and sets it
	 * as the unknown document. First, however, it adds the last unknown
	 * document back into the list of knowns.
	 * @param unknown - the document to be left out in this experiment
	 * @return - true if the unknown document was set, false otherwise
	 */
	public boolean setUnknownDocument(Document unknown) {
		if (unknownDocument != null) {
			documents.add(unknownDocument);
		}
		for (Document doc : documents) {
			// We define two documents being the same in this case as having the same author and title.
			if (doc.getAuthor().equals(unknown.getAuthor()) && doc.getTitle().equals(unknown.getTitle())) {
				unknownDocument = doc;
				documents.remove(doc);
				return true;
			}
		}
		return false;
	}


	public void setDocuments(final List<Document> docs) {
		this.documents = docs;
	}

	public void setEventDrivers(List<EventDriver> drivers) {
		this.eventDrivers = drivers;
	}

	public void setAnalysisDrivers(List<AnalysisDriver> drivers) {
		this.analysisDrivers = drivers;
	}

	/**
	 * @deprecated Use clearData()
	 * Removes all results generated by the analysis methods
	 */
	@Deprecated
	public void clearResults() {
		List<Document> documents = getUnknownDocuments();
		for (Document document : documents) {
			document.clearResults();
		}
	}

	/**
	 * @deprecated User clearData()
	 * Removes all EventSets generated by EventDriver
	 */
	@Deprecated
	public void clearEventSets() {
		for(Document document : documents){
			document.clearEventSets();
		}
	}

	/**
	 * Removes canonicizors from all documents
	 */
	public void clearCanonicizers() {
		for(Document document : documents){
			document.clearCanonicizers();
		}
	}

	/**
	 * Removes all Generated data from a run but leaves all settings untouched
	 */
	public void clearData() {
		for(Document document : documents){
			logger.info("clearing event sets from: " + document);
			document.clearEventSets();
		}
	}

	/**
	 * Get a List of All Canonicizers that are available to be used
	 * @return List of All Canonicizers
	 */
	public List<Canonicizer> getAllCanonicizers() {
		return Canonicizer.getCanonicizers();
	}

	/**
	 * Get a List of All EventDrivers that are available to be used
	 * @return List of All EventDrivers
	 */
	public List<EventDriver> getAllEventDrivers() {
		return EventDriver.getEventDrivers();
	}

	/**
	 * Get a List of All EventCuller that are available to be used
	 * @return List of All EventCullers
	 */
	public List<EventCuller> getAllEventCullers() {
		return EventCuller.getEventCullers();
	}

	/**
	 * Get a List of All AnalysisDriver that are available to be used
	 * @return List of All AnalysisDrivers
	 */
	public List<AnalysisDriver> getAllAnalysisDrivers() {
		return AnalysisDriver.getAnalysisDrivers();
	}

	/**
	 * Get a List of All DistanceFunctions that are available to be used
	 * @return List of All DistanceFunctions
	 */
	public List<DistanceFunction> getAllDistanceFunctions() {
		return DistanceFunction.getDistanceFunctions();
	}

	/**
	 * Get a List of All Languages that are available to be used
	 * @return List of All Languages
	 */
	public List<Language> getAllLanguages() {
		return Language.getLanguages();
	}
}
