package com.jgaap.generics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;


/**
 * Generic WEKA classifier. In theory, WEKA classifiers can extend this and set
 * the underlying classifier, plus any special parameters, in getClassifier(),
 * and everything else will Just Work.
 * 
 * @author John Noecker Jr.
 * 
 */
public abstract class WEKAAnalysisDriver extends AnalysisDriver {

	private static List<WEKAAnalysisDriver> WEKA_ANALYSIS_DRIVERS;
	
	public Classifier classifier;

	private Set<String> allAuthorNames;
	private Set<Event> allEvents;
	private FastVector attributeList;
	private FastVector authorNames;
	private Instances trainingSet;

	@Override
	public abstract String displayName();

	@Override
	public abstract String tooltipText();

	@Override
	public abstract boolean showInGUI();

	public abstract Classifier getClassifier();

	public abstract void testRequirements(List<EventSet> knownList) throws AnalyzeException;

	public void train(List<EventSet> knownList) throws AnalyzeException {

		classifier = getClassifier();

		// Test requirements
		testRequirements(knownList);

		/*
		 * Generate event histograms, unique event list, and unique author list.
		 */
		List<EventHistogram> knownHistograms = new ArrayList<EventHistogram>();
		allAuthorNames = new HashSet<String>();
		allEvents = new HashSet<Event>();
		for (EventSet eventSet : knownList) {
			allAuthorNames.add(eventSet.getAuthor());
			EventHistogram currentKnownHistogram = eventSet.getHistogram();
			allEvents.addAll(eventSet.uniqueEvents());
			knownHistograms.add(currentKnownHistogram);
		}

		/*
		 * Put together WEKA "Instances" object, which defines the attributes
		 * (aka "events" or "features").
		 */
		attributeList = new FastVector(allEvents.size() + 1);

		authorNames = new FastVector(allAuthorNames.size());
		for (String currentAuthorName : allAuthorNames) {
			authorNames.addElement(currentAuthorName);
		}
		authorNames.addElement("Unknown");
		Attribute authorNameAttribute = new Attribute("authorName", authorNames);
		attributeList.addElement(authorNameAttribute);

		for (Event event : allEvents) {
			Attribute eventAttribute = new Attribute(event.getEvent());
			attributeList.addElement(eventAttribute); // Each unique event is an
														// attribute in WEKA
		}

		/*
		 * Create the training "Instances" object, which is essentially the set
		 * of feature vectors for the training data.
		 */
		trainingSet = new Instances("JGAAP", attributeList, knownList.size());
		trainingSet.setClassIndex(0); // The label (author name) is in position
										// 0.

		/*
		 * Put together the training set
		 */
		for (int i = 0; i < knownHistograms.size(); i++) {
			EventHistogram knownHistogram = knownHistograms.get(i);
			Instance currentTrainingDocument = new Instance(
					allEvents.size() + 1);
			currentTrainingDocument.setValue((Attribute) attributeList
					.elementAt(0), knownList.get(i).getAuthor());
			int j = 1; // Start counting events (at 1, since 0 is the author
						// label)
			for (Event event : allEvents) {
				currentTrainingDocument.setValue(
						(Attribute) attributeList.elementAt(j),
						knownHistogram.getNormalizedFrequency(event));
				j++;
			}
			trainingSet.add(currentTrainingDocument);
		}

		/*
		 * Train the classifier N.B. The classifier should be set in the
		 * constructor by all subclasses.
		 */
		try {
			classifier.buildClassifier(trainingSet);
		} catch (Exception e) {
			e.printStackTrace();
			throw new AnalyzeException("WEKA classifier not trained");
		}
	}

	/**
	 * Convert from the JGAAP event framework to the WEKA instance framework to
	 * perform analysis.
	 * 
	 * @throws AnalyzeException
	 */
	public List<Pair<String, Double>> analyze(EventSet unknownEventSet)
			throws AnalyzeException {
		/*
		 * Generate the test sets, classifying each one as we go
		 */
		List<Pair<String, Double>> result = new ArrayList<Pair<String, Double>>();
		EventHistogram currentUnknownHistogram = unknownEventSet.getHistogram();

		Instance currentTest = new Instance(allEvents.size() + 1);

		currentTest.setValue((Attribute) attributeList.elementAt(0), "Unknown");
		int i = 1; // Start at 1, again
		for (Event event : allEvents) {
			currentTest.setValue((Attribute) attributeList.elementAt(i),
					currentUnknownHistogram.getNormalizedFrequency(event));
			i++;
		}
		currentTest.setDataset(trainingSet);

		double[] probDistribution;
		try {
			probDistribution = classifier.distributionForInstance(currentTest);
		} catch (Exception e) {
			e.printStackTrace();
			throw new AnalyzeException("Could not classify with WEKA");
		}

		/*
		 * Loop through the probability distributions (by author), and add
		 * results as pairs (author, probability).
		 */
		int j = 0;
		for (String authorName : allAuthorNames) {
			result.add(new Pair<String, Double>(authorName,
					probDistribution[j], 2));
			j++;
		}
		Collections.sort(result);
		Collections.reverse(result); // Reverse since we want higher
										// probabilities first

		return result;
	}

	public String toString() {
		if (classifier != null) {
			return classifier.toString();
		} else {
			return "WEKAAnalysis. No classifier set.";
		}
	}
	
	public static List<WEKAAnalysisDriver> getWekaAnalysisDrivers() {
		if (WEKA_ANALYSIS_DRIVERS == null) {
			WEKA_ANALYSIS_DRIVERS = Collections.unmodifiableList(loadWekaAnalysisDrivers());
		}
		return WEKA_ANALYSIS_DRIVERS;
	}

	private static List<WEKAAnalysisDriver> loadWekaAnalysisDrivers() {
		List<WEKAAnalysisDriver> wekaAnalysisDrivers = new ArrayList<WEKAAnalysisDriver>();
		List<AnalysisDriver> analysisDrivers = new ArrayList<AnalysisDriver>();
		for (AnalysisDriver analysisDriver : analysisDrivers) {
			if (analysisDriver instanceof WEKAAnalysisDriver) {
				wekaAnalysisDrivers.add((WEKAAnalysisDriver) analysisDriver);
			}
		}
		Collections.sort(wekaAnalysisDrivers);
		return wekaAnalysisDrivers;
	}
}
