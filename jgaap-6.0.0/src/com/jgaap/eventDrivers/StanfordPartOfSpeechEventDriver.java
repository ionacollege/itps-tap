package com.jgaap.eventDrivers;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.jgaap.generics.Document;
import com.jgaap.generics.Event;
import com.jgaap.generics.EventDriver;
import com.jgaap.generics.EventGenerationException;
import com.jgaap.generics.EventSet;

import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

/**
 * 
 * @author Michael Ryan
 *
 */
public class StanfordPartOfSpeechEventDriver extends EventDriver {
	
	static private Logger logger = Logger.getLogger(
			com.jgaap.eventDrivers.StanfordPartOfSpeechEventDriver.class);
	
	private volatile MaxentTagger tagger = null;
		
	public StanfordPartOfSpeechEventDriver() {
		addParams("tagginModel", "Model", "english-bidirectional-distsim", 
				new String[] { "arabic-accurate","arabic-fast.tagger","chinese",
				"english-bidirectional-distsim","english-left3words-distsim",
				"french",
				//"german-dewac",
				"german-fast"
				//,"german-hgc"
				}, false);
	}
	
	@Override
	public String displayName() {
		return "Stanford Part of Speech";
	}

	@Override
	public String tooltipText() {
		return "A Part of Speech Tagger using the MaxentTagger developed by the Stanford NLP Group http://nlp.stanford.edu";
	}

	@Override
	public boolean showInGUI() {
		return true;
	}

	
	@Override
	public EventSet createEventSet(Document doc)
			throws EventGenerationException {
		if (tagger == null)
			synchronized (this) {
				if (tagger == null) {
					String taggingModel = getParameter("taggingModel");
					if ("".equals(taggingModel)){
						 taggingModel = "english-bidirectional-distsim";
					}
					taggingModel = "com/jgaap/resources/models/postagger/"+taggingModel+".tagger";
					try {
						tagger = new MaxentTagger(taggingModel);
					} catch (Exception e) {
						throw new EventGenerationException(
								"Could not instance Maxent Tagger with model located at "+taggingModel);
					}
				}
			}
		List<ArrayList<TaggedWord>> taggedSentences = tagger
				.process(MaxentTagger.tokenizeText(new StringReader(doc.stringify())));
		
		EventSet eventSet = new EventSet(taggedSentences.size());
		for (ArrayList<TaggedWord> sentence : taggedSentences) {
			for (TaggedWord taggedWord : sentence) {
				eventSet.addEvent(new Event(taggedWord.tag()));
			}
		}
		return eventSet;
	}
	
	
	//tagText INTEGRATED BY AMANDA AEBIG
	//tags each word in a text with its part of speech, words are tagged with an underscore between the word and its part of speech (ex: the_DT)
	//tagged texts are added to the TaggedDocs folder; they will have the same name as the original document
	//in the event of out of memory errors, use the following JVM args:
	/*
	 * 	-Xmx10G
		-XX:+UseG1GC
		-XX:+UseStringDeduplication
	 */
	public void tagText(Document doc)
			throws EventGenerationException {
		if (tagger == null)
			synchronized (this) {
				if (tagger == null) {
					String taggingModel = getParameter("taggingModel");
					if ("".equals(taggingModel)){
						 taggingModel = "english-bidirectional-distsim";
					}
					taggingModel = "com/jgaap/resources/models/postagger/"+taggingModel+".tagger";
					try {
						tagger = new MaxentTagger(taggingModel);
					} catch (Exception e) {
						throw new EventGenerationException(
								"Could not instance Maxent Tagger with model located at "+taggingModel);
					}
				}
			}
		List<ArrayList<TaggedWord>> taggedSentences = tagger
				.process(MaxentTagger.tokenizeText(new StringReader(doc.stringify())));
		
		
		List<String> taggedWords=new ArrayList<>();
		for (ArrayList<TaggedWord> sentence : taggedSentences) {
			for (TaggedWord taggedWord : sentence) {
				String word=taggedWord.toString();
				word=word.replace("/", "_");
				taggedWords.add(word);
				
			
				
				
			}
		}
		
		try {
			
			PrintWriter writer = new PrintWriter("TaggedDocs/" + doc.getTitle(), "UTF-8");
			for(int i=0;i<taggedWords.size();i++){
				writer.write(taggedWords.get(i)+"\n");
			}
			
			writer.close();
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
		
			e.printStackTrace();
		}
		
		
		System.out.println(doc.getTitle());
		
	}
	

}
