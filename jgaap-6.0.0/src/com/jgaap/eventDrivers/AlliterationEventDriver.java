package com.jgaap.eventDrivers;

import com.jgaap.generics.Document;
import com.jgaap.generics.EventDriver;
import com.jgaap.generics.EventSet;

public class AlliterationEventDriver extends EventDriver {
	@Override
	public String displayName() {
		return "Alliteration";
	}
	
	@Override
	public String tooltipText() {
		return "";
	}
	
	@Override
	public String longDescription() {
		return "Alliteration feature created by Dr. Ivanov.";
	}
	
	@Override
	public boolean showInGUI() {
		return true;
	}
	
	@Override
	public EventSet createEventSet(Document document) {
		int skipRange = 3;
		return new EventSet();
	}
}