package com.jgaap.eventDrivers;

import com.jgaap.generics.Document;
import com.jgaap.generics.Event;
import com.jgaap.generics.EventDriver;
import com.jgaap.generics.EventGenerationException;
import com.jgaap.generics.EventSet;

import java.io.InputStreamReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Arrays;

public class LexicalStressEventDriver extends EventDriver {
	private Map<String, ArrayList<String>> cmuDictionary;
	Map<String, Integer> map = new TreeMap<String, Integer>(); //contains pattern and number of occurrences
	
	//finds POS groups
	final Pattern parenPattern = Pattern.compile("\\(.*\\)");
	final Pattern doubleParenPattern = Pattern.compile("[\\[].*[\\]]");
	
	@Override
	public String displayName() {
		return "Lexical Stress";
	}
	
	@Override
	public String tooltipText() {
		return "";
	}
	
	@Override
	public String longDescription() {
		return "Lexical stress feature using Part of Speech based pattern selection, implemented by Amanda Aebig.";
	}
	
	@Override
	public boolean showInGUI() {
		return true;
	}
	
	public LexicalStressEventDriver() {
		
		cmuDictionary = new TreeMap<String, ArrayList<String>>();

		String dictionaryPath = com.jgaap.JGAAPConstants.JGAAP_RESOURCE_PACKAGE + "CMUDictionary_POS_NoRepeats.txt";
		Scanner reader = new Scanner(new InputStreamReader(
				LexicalStressEventDriver.class.getResourceAsStream(dictionaryPath)));
	
		while(reader.hasNext()){

			String line=reader.nextLine();
				String word="";
				String POS="";
				String pattern="";
				String key="";
				String num="";
				
				String[] list=line.split("  ");

				Matcher m=doubleParenPattern.matcher(list[0]);
				Matcher m2=parenPattern.matcher(list[0]);

				if(m.find()){
					POS=m.group();
					
				}

				if(m2.find()){
					num=m2.group();
				
				}				
				
				list[0]=list[0].replaceAll("\\(.*\\)", "");
				list[0]=list[0].replaceAll("\\[.*\\]", "");

				
				word=list[0];
				pattern=list[1];
				
				key=word+num;
				
				
				ArrayList<String> wordInfo=new ArrayList<String>();
				
				
				if(!POS.equals(""))
				wordInfo.add(POS);

				wordInfo.add(pattern);

			

			
			    cmuDictionary.put(key, wordInfo);

		
			}
		reader.close();
		//System.out.println(cmuDictionary.toString());
		
	}
	
	@Override
	public EventSet createEventSet(Document document) {
		
		
		EventSet eventSet = new EventSet();
		
		
		StanfordPartOfSpeechEventDriver SPOS = new StanfordPartOfSpeechEventDriver();
		
		//get the document name
		//if the document name does not already exist in the taggeddocs folder, tag the text
		//the stanford pos tagger will tag the text and write it to a new file in the taggeddocs folder with the same name
		//open the file in the taggeddocs folder
		
		File taggedDocsDir = new File("TaggedDocs");
		if (!taggedDocsDir.exists()) {
			taggedDocsDir.mkdirs();
		}
		String docName=document.getTitle();
		File file = new File("TaggedDocs/"+docName);
		//if the tagged file does not exist, create the tagged file
		//the file must have the exact same name of the input file  - case sensitive
		 if(!file.exists()){
		      System.out.println("Tagged file does not exist");
		      try {
		    	   SPOS.tagText(document);
		       } catch (EventGenerationException e) {
		    	   	 e.printStackTrace();
		       }
		  }
		  
		       
		      
	
		 //open the tagged file
		  File taggedDoc = new File("TaggedDocs/" + docName);										
			Scanner in;
			
			//array to hold words+POS tag from the tagged doc
			ArrayList <String> words = new ArrayList <String> ();
			
			//parse the tagged text and add each word+POS to an array, words
			try {
				in = new Scanner(taggedDoc);
				in.useDelimiter("[^\\w\\-]+|--+"); 												

										
				while (in.hasNext()){														
				
					String word = in.next().toUpperCase();	
					

					boolean isUpper=false;
					for(int i=0; i < word.length(); i++) {
	    				 
	    				  char ch = word.charAt(i);

	    				  if(Character.isUpperCase(ch)){
	    				  	isUpper=true;
	    				
	    				}
	    				  else{
	    				  	isUpper=false;
	    				  	

	    				}

	    			}

	    			if((isUpper) && (word.contains("_")) && (!word.startsWith("_"))){
	    				words.add(word);
	    				
	    			}
	    			
				}
				
									
			} catch (FileNotFoundException e) {
				
				e.printStackTrace();
			}												
			
		System.out.println(words.toString());
		
		ArrayList <ArrayList<String>> patterns = new ArrayList <ArrayList<String>> ();	
		 
		for (String w: words){													
			
			String[] wordParts = w.split("_");		
			
			
			String textWord=wordParts[0];		
			String textPOS=wordParts[1];	
			//System.out.println("The word is "+textWord+" with POS "+textPOS);
		
			patterns.clear();

			if(cmuDictionary.get(textWord)!=null){
			patterns.add(cmuDictionary.get(textWord));
			}

			// if the word is not in the dictionary:
			else{
				String lexicalP="";
				Integer num = map.get(lexicalP);										
				map.put(lexicalP, (num == null) ? 1 : num + 1);	
				//System.out.println("No entry for word "+textWord);
			}

			if(cmuDictionary.get(textWord+"(1)")!=null){
			patterns.add(cmuDictionary.get(textWord+"(1)"));
			}

			if(cmuDictionary.get(textWord+"(2)")!=null){	
			patterns.add(cmuDictionary.get(textWord+"(2)"));
			}

			if(cmuDictionary.get(textWord+"(3)")!=null){
			patterns.add(cmuDictionary.get(textWord+"(3)"));
			}

			
			ArrayList<String> matchPatterns=new ArrayList<String>();
			ArrayList<String> noMatchPatterns=new ArrayList<String>();

			//if there is only one pattern to choose from
			//choose the first pattern
			if(patterns.size()==1){
				for(int i=0;i<patterns.size();i++){
					for(int j=0;j<patterns.get(i).size();j++){


						if(patterns.get(i).get(j).matches(".*\\d+.*")){
						//System.out.println(patterns.get(i).get(j));
						String lexicalP=patterns.get(i).get(j);
						eventSet.addEvent(new Event(lexicalP));
						Integer num = map.get(lexicalP);										
						map.put(lexicalP, (num == null) ? 1 : num + 1);	
						}


					}
				}
			}

			//if there are multiple patterns to choose from:
			else{
			
			for(int i=0;i<patterns.size();i++){
				for(int j=0;j<patterns.get(i).size()-1;j++){

					//int size=patterns.get(i).size();

					//remove brackets from pos list
					//seprate by comma 
					String list=patterns.get(i).get(j);
					list=list.replaceAll("[\\[\\](){}]","");
					String[] sepPOS=list.split(",");

					
					if(Arrays.asList(sepPOS).contains(textPOS)){
						

						//System.out.println("the list contains "+textPOS+", adding pattern "+patterns.get(i).get(j+1));
						matchPatterns.add(patterns.get(i).get(j+1));
					}
					else{
						
						//output to file words where POS in text does not match POS in dictionary
						

						
						
						noMatchPatterns.add(patterns.get(i).get(j+1));
						//System.out.println("the list does not contain "+textPOS);
					}
					
										
				}

				
		
			}

					//there were pos matches, choose pattern at random
				
					if(!matchPatterns.isEmpty()){
					//System.out.println("in the match method");
					int index = new Random().nextInt(matchPatterns.size());				
					String lexicalP=matchPatterns.get(index);
					eventSet.addEvent(new Event(lexicalP));
					//System.out.println("Chose pattern "+lexicalP);
					Integer num = map.get(lexicalP);										
					map.put(lexicalP, (num == null) ? 1 : num + 1);	

				}
					//there were no pos matches, choose pattern at random
					else if((matchPatterns.isEmpty()) && (!noMatchPatterns.isEmpty())){

							

					int index = new Random().nextInt(noMatchPatterns.size());				
					String lexicalP=noMatchPatterns.get(index);
					eventSet.addEvent(new Event(lexicalP));
					//System.out.println("Chose pattern "+lexicalP);
					Integer num = map.get(lexicalP);										
					map.put(lexicalP, (num == null) ? 1 : num + 1);	

				}


		}
		
	}
	

		

	//System.out.println(map.toString());

	
											
	map.remove("");																
	
		 
		
		
		return eventSet;
		
	}
}