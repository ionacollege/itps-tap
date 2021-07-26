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

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.FastScatterPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import com.jgaap.backend.EventCullerFactory;
import com.jgaap.backend.EventDriverFactory;
import com.jgaap.backend.Utils;
import com.jgaap.generics.Document;
import com.jgaap.generics.Event;
import com.jgaap.generics.EventCuller;
import com.jgaap.generics.EventCullingException;
import com.jgaap.generics.EventDriver;
import com.jgaap.generics.EventGenerationException;
import com.jgaap.generics.EventHistogram;
import com.jgaap.generics.EventSet;

public class TestBed
{
	public static final String loadFilePath = "/Users/Sean/Documents/College/Research/Thomas Paine/Blickle/load_ncur.csv";
	//public static final String loadFilePath = "/com/jgaap/resources/load_all_author_files.csv";
	//public static final String eeFilePath = "/com/jgaap/resources/ee_all.csv";

	public static void main(String args[]) throws Exception
	{
		// Load the documents into the API.
		//List<Document> documents = Utils.getDocumentsFromCSV(com.jgaap.backend.CSVIO.readCSV(com.jgaap.JGAAP.class.getResourceAsStream(loadFilePath)));
		List<Document> documents = Utils.getDocumentsFromCSV(com.jgaap.backend.CSVIO.readCSV(loadFilePath));
		for (Document document : documents) {
			document.load();
		}

		//Document document = new Document("/Users/Sean/Documents/College/Research/Thomas Paine/Author Files/Adams/Adams Defense 1.txt", "Adams");
		//document.load();

		List<EventSet> eventSets = new ArrayList<EventSet>();
		List<EventDriver> eventDrivers = new ArrayList<EventDriver>();
		List<String> actions = new ArrayList<String>();
		actions.add("Character NGrams|N:2");
		actions.add("MW Function Words");

		for (String action : actions) {
			EventDriver eventDriver = EventDriverFactory.getEventDriver(action);
			eventDrivers.add(eventDriver);
		}

		for (Document document : documents) {
			for (EventDriver eventDriver : eventDrivers) {
				try{
					EventSet set = eventDriver.createEventSet(document);
					eventSets.add(set);
					document.addEventSet(eventDriver,set);
				} catch (EventGenerationException e) {
					throw new Exception("Could not Eventify with "+eventDriver.displayName()+" on File:"+document.getFilePath()+" Title:"+document.getTitle(),e);
				}
			}
		}

		EventCuller culler = EventCullerFactory.getEventCuller("Most Common Events|N:50");

		try {
			eventSets = culler.cull(eventSets);
		} catch (EventCullingException e) {
			System.out.println(e);
		}


		for (EventSet set : eventSets) {
			//System.out.println("Set");
			EventHistogram hist = new EventHistogram(set);
			Set<Event> mySet = new TreeSet<Event>();
			for (Event event : set) {
				mySet.add(event);
			}
			//for (Event event : mySet)
				//System.out.println(event + ": " + hist.getRelativeFrequency(event));
		}

		Map<String, List<double[]>> tempDataMap = new TreeMap<String, List<double[]>>();

		for (Document document : documents) {
			EventSet set = document.getEventSet(eventDrivers.get(1));
			EventHistogram hist = new EventHistogram(set);

			String author = document.getAuthor();
			double[] data = {
					//hist.getRelativeFrequency(new Event("of")),
					//hist.getRelativeFrequency(new Event("and"))
					Math.pow(hist.getRelativeFrequency(new Event("the")), 1),
					Math.pow(hist.getRelativeFrequency(new Event("for")), 1)
			};

			if (tempDataMap.containsKey(author))
				tempDataMap.get(author).add(data);
			else {
				List<double[]> dataList = new ArrayList<double[]>();
				dataList.add(data);
				tempDataMap.put(author, dataList);
			}
		}

		Map<String, double[][]> dataMap = new TreeMap<String, double[][]>();

		for (String author : tempDataMap.keySet()) {
			final int NUM_DOCS = tempDataMap.get(author).size();
			double[][] data = new double[2][NUM_DOCS];
			for (int i = 0; i < NUM_DOCS; i++) {
				data[0][i] = tempDataMap.get(author).get(i)[0];
				data[1][i] = tempDataMap.get(author).get(i)[1];
			}
			dataMap.put(author, data);
		}

		System.out.println(dataMap);



		JFrame frame = new JFrame("TestBed");
		ChartPanel cp = new ChartPanel(createChart(dataMap));
		frame.getContentPane().add(cp);
		frame.setSize(700, 500);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);

	}


	private static JFreeChart createChart(Map<String, double[][]> dataMap)
	{

		DefaultXYDataset dataset = new DefaultXYDataset();
		for (String key : dataMap.keySet())
			dataset.addSeries(key, dataMap.get(key));

		JFreeChart chart = ChartFactory.createScatterPlot(
				"Comparing Documents written by Paine and Jefferson",		// chart title
				"the",				// x axis title
				"for",				// y axis title
				dataset
		);

		return chart;
	}

}
