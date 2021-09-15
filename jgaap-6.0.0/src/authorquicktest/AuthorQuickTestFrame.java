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

import javax.swing.JOptionPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.awt.Color;
import java.awt.Container;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;


public class AuthorQuickTestFrame extends JFrame implements StatusLog
{
	private static final long serialVersionUID = -2297402699643078768L;

	private final int NUMBER_OF_AUTHORS_TO_SHOW = 4;

	// GUI
	private JMenuBar menuBar;
	private JMenu fileMenu;
	private JMenuItem loadPackageMenuItem;
	private JMenuItem loadResultsFileMenuItem;
	private JMenuItem saveCsvFileMenuItem;
	private JMenuItem saveExcelFileMenuItem;
	private JMenuItem saveRawResultsTextMenuItem;

	private JLabel testFileLabel;

	private JTextField testFileTextField;

	private JButton browseButton;
	private JButton runButton;
	private JButton clearResultsButton;
	private JCheckBox savePNG;

	private JTextArea statusTextArea;
	private JScrollPane statusScrollPane;

	private JTabbedPane resultsTabbedPane;
	private List<ResultsPanel> resultsPanels;

	// Data
	private List<File> testFiles;
	//private String testFileNames;
	private String packageName;
	private List<Map<String, Double>> authorSupports;

	public AuthorQuickTestFrame()
	{
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		testFiles = new ArrayList<File>();

		createInterface();
		layoutInterface();
	}

	public void setStatus(String status)
	{
		if (status != null) {
			//System.out.println(status);
			statusTextArea.append(status + "\n");
		}
	}

	public void clearStatus()
	{
		statusTextArea.setText("");
	}

	public void setTestFileNames(String fileNames)
	{
		if (fileNames == null)
			return;

		// If the text field doesn't have the correct text
		// in it, then update it.
		if (!testFileTextField.getText().equals(fileNames))
			testFileTextField.setText(fileNames);

		// If the file doesn't exist, or if the path refers to a
		// directory, indicate it by making the text field yellow.
		testFiles = getFilesFromFileNames(fileNames);

		for (File file : testFiles) {
			if (file.exists() && file.isFile()) {
				testFileTextField.setBackground(Color.WHITE);
			} else {
				testFileTextField.setBackground(Color.YELLOW);
			}
		}
	}

	/**
	 *
	 * @param supportsMaps
	 * @return {boolean} - false if the number of supports maps
	 * 		does not match the number of test files, true otherwise
	 */
	public boolean setAuthorSupports(List<Map<String, Double>> supportsMaps)
	{
		// Re-enable GUI.
		browseButton.setEnabled(true);
		runButton.setEnabled(true);
		clearResultsButton.setEnabled(true);
		savePNG.setEnabled(true);
		testFileTextField.setEnabled(true);
		fileMenu.setEnabled(true);
		loadPackageMenuItem.setEnabled(true);
		loadResultsFileMenuItem.setEnabled(true);
		saveCsvFileMenuItem.setEnabled(true);
		saveExcelFileMenuItem.setEnabled(true);
		saveRawResultsTextMenuItem.setEnabled(true);

		// Clear out old data.
		authorSupports = supportsMaps;
		resultsPanels.clear();
		resultsTabbedPane.removeAll();

		if (supportsMaps == null)
			return false;
		if (testFiles.size() != supportsMaps.size())
			return false;

		resultsTabbedPane.setVisible(true);

		for (int i = 0; i < testFiles.size(); i++) {
			Map<String, Double> supportsMap = supportsMaps.get(i);
			String testFileName = testFiles.get(i).getName();
			ResultsPanel panel = new ResultsPanel(testFileName);
			panel.setAuthorSupports(supportsMap);
			resultsPanels.add(panel);
			resultsTabbedPane.addTab(testFileName, panel);
		}

		return true;
	}

	public List<File> getTestFiles() { return testFiles; }

	public List<Map<String, Double>> getAuthorSuports() { return authorSupports; }

	private List<File> getFilesFromFileNames(String fileNames)
	{
		if (fileNames == null)
			return null;

		List<File> files = new ArrayList<File>();

		if (fileNames.indexOf(";") == -1) {
			File file = new File(fileNames);
			files.add(file);
			return files;
		}

		String[] fileNamesArray = fileNames.split(";");
		for (String fileName : fileNamesArray)
			files.add(new File(fileName));

		return files;
	}

	/**
	 * Create the GUI elements for the interface but does not
	 * lay them out on the frame.
	 */
	private void createInterface()
	{
		// Menus
		loadPackageMenuItem = new JMenuItem("Load author package (ALt+L)");
		loadPackageMenuItem.setMnemonic(KeyEvent.VK_L);
		loadPackageMenuItem.addActionListener(new LoadPackageListener());

		loadResultsFileMenuItem = new JMenuItem("Load Raw Results File");
		//loadResultsFileMenuItem.addActionListener();
		loadResultsFileMenuItem.setEnabled(false);
		saveCsvFileMenuItem = new JMenuItem("Save CSV File");
		saveCsvFileMenuItem.addActionListener(new AQTSaveFileListener(this, ".csv", "Save results file as CSV..."));
		saveCsvFileMenuItem.setEnabled(false);
		saveExcelFileMenuItem = new JMenuItem("Save Excel File (Alt+E)");
		saveExcelFileMenuItem.setMnemonic(KeyEvent.VK_E);
		saveExcelFileMenuItem.addActionListener(new AQTSaveFileListener(this, ".xlsx", "Save results as Excel spreadsheet..."));
		saveExcelFileMenuItem.setEnabled(false);
		saveRawResultsTextMenuItem = new JMenuItem("Save Raw Results as Text File (Alt+S)");
		saveRawResultsTextMenuItem.setMnemonic(KeyEvent.VK_S);
		saveRawResultsTextMenuItem.addActionListener(new AQTSaveFileListener(this, ".txt", "Save raw results as a text file..."));
		saveRawResultsTextMenuItem.setEnabled(false);

		fileMenu = new JMenu("File (Alt-F)");
		fileMenu.setMnemonic(KeyEvent.VK_F);
		fileMenu.add(loadPackageMenuItem);
		//fileMenu.add(loadResultsFileMenuItem);
		//fileMenu.add(saveCsvFileMenuItem);
		fileMenu.add(saveExcelFileMenuItem);
		fileMenu.add(saveRawResultsTextMenuItem);

		menuBar = new JMenuBar();
		menuBar.add(fileMenu);
		setJMenuBar(menuBar);

		// And the rest of the interface.
		testFileLabel = new JLabel("Test file");
		testFileTextField = new JTextField();
		testFileTextField.getDocument().addDocumentListener(new TestFileTextFieldChangedListener());

		browseButton = new JButton("Browse (Alt+B)");
		browseButton.setMnemonic(KeyEvent.VK_B);
		browseButton.addActionListener(new BrowseButtonListener());

		runButton = new JButton("Run experiment (Alt+R)");
		runButton.setMnemonic(KeyEvent.VK_R);
		runButton.addActionListener(new RunButtonListener());

		savePNG = new JCheckBox("Save png image");
		savePNG.setSelected(true);

		clearResultsButton = new JButton("Clear results");
		clearResultsButton.addActionListener(new ClearResultsButtonListener());

		statusTextArea = new JTextArea();
		statusTextArea.setEditable(false);
		statusScrollPane = new JScrollPane(statusTextArea);

		resultsTabbedPane = new JTabbedPane();
		resultsPanels = new ArrayList<ResultsPanel>();

		statusTextArea.setVisible(true);
	}

	/**
	 * Lays out the interface elements created in the createInterface() method.
	 */
	private void layoutInterface()
	{
		Container contentPane = getContentPane();

		// Test file panel.
		JPanel testFilePanel = new JPanel();
		testFilePanel.setLayout(new GridBagLayout());
		GridBagConstraints tfc1 = new GridBagConstraints();
		tfc1.insets = new Insets(0, 10, 0, 0); // Left padding
		GridBagConstraints tfc2 = new GridBagConstraints();
		tfc2.fill = GridBagConstraints.HORIZONTAL;
		tfc2.weightx = 1.0;
		testFilePanel.add(testFileLabel, tfc1);
		testFilePanel.add(testFileTextField, tfc2);
		testFilePanel.add(browseButton);
		testFilePanel.add(savePNG);
		testFilePanel.add(runButton);
		//testFilePanel.add(clearResultsButton);

		// Main layout.
		contentPane.setLayout(new GridBagLayout());

		GridBagConstraints c1 = new GridBagConstraints();
		c1.gridy = 0;
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.insets = new Insets(10, 0, 0, 0);
		c1.weightx = 1.0;

		GridBagConstraints c2 = new GridBagConstraints();
		c2.gridy = 1;
		c2.fill = GridBagConstraints.BOTH;
		c2.weighty = 0.9;

		GridBagConstraints c3 = new GridBagConstraints();
		c3.gridy = 2;
		c3.fill = GridBagConstraints.BOTH;
		c3.weightx = 1.0;
		c3.weighty = 0.1;

		contentPane.add(testFilePanel, c1);
		contentPane.add(resultsTabbedPane, c2);
		contentPane.add(statusScrollPane, c3);
	}


	private class ResultsPanel extends JPanel
	{
		private static final long serialVersionUID = 8681954955710048237L;

		// GUI
		private ChartPanel chartPanel;

		private List<JLabel> authorLabels;
		private List<JLabel> supportLabels;

		// Data
		private String testFileName;

		@SuppressWarnings("unused")
		public ResultsPanel()
		{
			testFileName = "";
			setupInterface();
		}

		public ResultsPanel(String fileName)
		{
			setTestFileName(fileName);
			setupInterface();
		}

		public void setTestFileName(String fileName)
		{
			testFileName = fileName;
		}

		@SuppressWarnings("unused")
		public String getTestFileName() { return testFileName; }

		public void setAuthorSupports(Map<String, Double> supportsMap)
		{
			List<String> authors = new ArrayList<String>();
			List<Double> supports = new ArrayList<Double>();

			getMaxSupports(supportsMap, NUMBER_OF_AUTHORS_TO_SHOW, authors, supports);

			for (int i = 0; i < NUMBER_OF_AUTHORS_TO_SHOW; i++) {
				authorLabels.get(i).setText((i+1) + ". " + authors.get(i));
				supportLabels.get(i).setText("" + supports.get(i));
			}
			chartPanel.setChart(createChartFromAuthorSupports(supportsMap));
		}

		private void getMaxSupports(final Map<String, Double> supportsMap, int numberOfAuthors, List<String> authors, List<Double> supports)
		{
			// Clear the lists in case
			// they aren't already empty.
			authors.clear();
			supports.clear();

			// Initialize as negative so that it will get the maximum
			// support in the map (supports can never be negative).
			for (int i = 0; i < numberOfAuthors; i++) {
				String maxAuthor = "";
				double maxSupport = -1;
				for (Map.Entry<String, Double> entry : supportsMap.entrySet()) {
					if (entry.getValue() > maxSupport && !authors.contains(entry.getKey())) {
						maxAuthor = entry.getKey();
						maxSupport = entry.getValue();
					}
				}
				authors.add(maxAuthor);
				supports.add(maxSupport);
			}
		}

		private JFreeChart createChartFromAuthorSupports(final Map<String, Double> authorSupports)
		{
			DefaultCategoryDataset dataset = new DefaultCategoryDataset();

			List<Integer> thomasPaineIndices = new ArrayList<>();
			if (authorSupports != null) {
				int i = 0;
				for (String author : authorSupports.keySet()) {
					dataset.addValue(authorSupports.get(author), "author", author);
					if (author.toLowerCase().contains("paine"))
						thomasPaineIndices.add(i);
					i++;
				}
			}

			String docTitle = "";

			String[] testFileNameArray = testFileName.split("/");
			if (testFileNameArray.length > 0) {
				String[] docTitleArray = testFileNameArray[testFileNameArray.length-1].split("\\.");
				if (docTitleArray.length > 0) {
					if (docTitleArray[0].startsWith("Unknown "))
						docTitle = docTitleArray[0].substring(8);
					else
						docTitle = docTitleArray[0];
				}
			}

			String chartTitle = "Comparison of Supports " + "for \"" + docTitle+ "\"";// + "\nAccuracy Threshold: 62%";

			JFreeChart chart = ChartFactory.createBarChart(
					chartTitle,					// chart title
					"Author",					// x axis label
					"Support",					// y axis label
					dataset,					// data
					PlotOrientation.VERTICAL,	// orientation
					false,						// include legend?
					true,						// tooltips?
					false						// URLs?
			);

			// Customize the plot.
			CategoryPlot plot = chart.getCategoryPlot();
			plot.setBackgroundPaint(Color.WHITE);
			plot.setRangeGridlinePaint(Color.GRAY);

			BarRenderer renderer = new CustomBarRenderer(thomasPaineIndices);
			plot.setRenderer(renderer);

			// Set rotation on category labels.
			plot.getDomainAxis().setCategoryLabelPositions(
					CategoryLabelPositions.createUpRotationLabelPositions(Math.PI / 4.0)
			);

			try {
				boolean isSaveSelected = savePNG.isSelected();
				if (isSaveSelected) {
					OutputStream out = new FileOutputStream(testFiles.get(0).getParent()+ "/"+docTitle+"-"+packageName+".png");
					ChartUtilities.writeChartAsPNG(out, chart, 700, 500);
					out.close();
				}
			} catch (IOException ex) {
				AuthorQuickTestFrame.this.setStatus("Problem saving a picture."+ex);
			}

			return chart;
		}

		private void setupInterface()
		{
			// Create interface.
			chartPanel = new ChartPanel(createChartFromAuthorSupports(null));
			chartPanel.setOpaque(true);
			chartPanel.setBackground(Color.WHITE);

			authorLabels = new ArrayList<JLabel>(NUMBER_OF_AUTHORS_TO_SHOW);
			supportLabels = new ArrayList<JLabel>(NUMBER_OF_AUTHORS_TO_SHOW);

			for (int i = 0; i < NUMBER_OF_AUTHORS_TO_SHOW; i++) {
				authorLabels.add(new JLabel(""));
				supportLabels.add(new JLabel(""));
			}

			// Layout interface.
			JPanel authorSupportPanel = new JPanel();
			authorSupportPanel.setLayout(new GridLayout(authorLabels.size(), 2));

			// - Author support.
			for (int i = 0; i < authorLabels.size(); i++) {
				authorSupportPanel.add(authorLabels.get(i));
				authorSupportPanel.add(supportLabels.get(i));
			}

			setLayout(new GridBagLayout());

			GridBagConstraints rc1 = new GridBagConstraints();
			rc1.insets = new Insets(20, 10, 0, 0);
			rc1.anchor = GridBagConstraints.PAGE_START;

			GridBagConstraints rc2 = new GridBagConstraints();
			rc2.insets = new Insets(20, 10, 10, 10);
			rc2.fill = GridBagConstraints.BOTH;
			rc2.weightx = 1.0;
			rc2.weighty = 1.0;

			add(authorSupportPanel, rc1);
			add(chartPanel, rc2);
		}
	}


	private class BrowseButtonListener implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent event)
		{
			FileDialog testFileDialog = new FileDialog(AuthorQuickTestFrame.this, "Choose a file to test...");
			testFileDialog.setMultipleMode(true);
			testFileDialog.setVisible(true);

			if (testFileDialog.getFiles() != null && testFileDialog.getFiles().length > 0) {

				File[] files = testFileDialog.getFiles();
				StringBuilder fileNamesStringBuilder = new StringBuilder();
				for (int i = 0; i < files.length; i++) {
					fileNamesStringBuilder.append(files[i].getAbsolutePath());
					if (i < files.length - 1)
						fileNamesStringBuilder.append(";");
				}
				//testFileTextField.setText(fileNamesStringBuilder.toString());
				setTestFileNames(fileNamesStringBuilder.toString());
			}
		}
	}



	private class RunButtonListener implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent event)
		{
			new Thread(new Runnable() {
				public void run() {

					// If there aren't any test files, we can't do anything.
					if (testFiles == null || testFiles.size() == 0) {
						JOptionPane.showMessageDialog(AuthorQuickTestFrame.this,
								  "Please enter one or more files to test or browse for\n"
								+ "a file or files from your computer.", "No File to Test",
								JOptionPane.ERROR_MESSAGE);
						return;
					}

					// If any of the files don't exist or are actually directories,
					// not files, then don't run the experiment.
					List<String> testFileNames = new ArrayList<String>();

					for (File file : testFiles) {
						if (file.exists()) {
							if (!file.isDirectory()) {
								testFileNames.add(file.getAbsolutePath());
							} else {
								JOptionPane.showMessageDialog(AuthorQuickTestFrame.this,
										  "Unable to run the experiment because at least one of the\n"
										+ "paths you have entered refers to a directory. Please enter a\n"
										+ "valid file path or browse for a file on your computer.",
										"Cannot Run Experiment on Directory",
										JOptionPane.ERROR_MESSAGE);
								return;
							}
						} else {
							JOptionPane.showMessageDialog(AuthorQuickTestFrame.this,
									  "Unable to run the experiment because at least one of the\n"
									+ "files you have entered does not exist. Please enter a valid\n"
									+ "file path or browse for a file on your computer.", "File Not Found",
									JOptionPane.ERROR_MESSAGE);
							return;
						}

					}

					// If all files exist, and none are directories, run
					// the experiment. We don't need an if statement,
					// because the function will have returned if anything
					// went awry (yes...awry).
					browseButton.setEnabled(false);
					runButton.setEnabled(false);
					clearResultsButton.setEnabled(false);
					testFileTextField.setEnabled(false);
					fileMenu.setEnabled(false);
					statusTextArea.setText("");
					statusTextArea.setVisible(true);
					resultsTabbedPane.setVisible(false);
					setStatus("Running experiment...");

					authorquicktest.AuthorQuickTest.testFiles(testFileNames);
				}
			}).start();
		}
	}

	private class ClearResultsButtonListener implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent event)
		{
			testFileTextField.setText("");
			clearStatus();
			//authorquicktest.AuthorQuickTest.clearResults();
		}
	}

	private class TestFileTextFieldChangedListener implements DocumentListener
	{
		@Override
		public void changedUpdate(DocumentEvent event) {
			return;
		}

		@Override
		public void insertUpdate(DocumentEvent e) {
			setTestFileNames(testFileTextField.getText());
		}

		@Override
		public void removeUpdate(DocumentEvent e) {
			setTestFileNames(testFileTextField.getText());
		}
	}

	private class LoadPackageListener implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent event)
		{
			FileDialog fileDialog = new FileDialog(AuthorQuickTestFrame.this, "Choose an author package to load...");
			fileDialog.setVisible(true);

			packageName=fileDialog.getFile();
			if (packageName.indexOf(".") > 0) {
				   packageName=packageName.substring(0, packageName.lastIndexOf("."));
			}

			if (fileDialog.getFile() != null) {
				if (!authorquicktest.AuthorQuickTest.loadAuthorPackage(fileDialog.getDirectory() + fileDialog.getFile()))
					JOptionPane.showMessageDialog(AuthorQuickTestFrame.this,
							  "There was a problem loading the author package. Please make "
							+ "sure it is a valid package file.",
							"Unable to load author package",
							JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private class AQTSaveFileListener extends SaveFileListener {
		public AQTSaveFileListener(JFrame f, String type, String prompt) {
			super(f, type, prompt);
		}

		@Override
		protected boolean save(String fileType, String saveFileName) {
			// Some sloppy ifs to call the correct save method in AuthorQuickTest.
			boolean isFileSaved;
			if (fileType.equalsIgnoreCase(".csv"))
				isFileSaved = authorquicktest.AuthorQuickTest.saveCsvFile(saveFileName);
			else if (fileType.equalsIgnoreCase(".xlsx"))
				isFileSaved = authorquicktest.AuthorQuickTest.saveExcelFile(saveFileName);
			else if (fileType.equalsIgnoreCase(".txt"))
				isFileSaved = authorquicktest.AuthorQuickTest.saveTextFileOfRawResults(saveFileName);
			else {
				isFileSaved = false;
			}
			return isFileSaved;
		}
	}

	private class CustomBarRenderer extends BarRenderer {
		private Map<Integer, Boolean> specialColumns;

		public CustomBarRenderer(List<Integer> cols) {
			setBarPainter(new StandardBarPainter());
			setShadowVisible(false);
			specialColumns = new HashMap<Integer, Boolean>();
			for (int col : cols) {
				specialColumns.put(col, true);
			}
		}

		 /**
         * Returns the paint for an item. Overrides the default behavior inherited from
         * AbstractSeriesRenderer.
         *
         * @param row The series.
         * @param column The category.
         *
         * @return The item color.
         */
        public Paint getItemPaint(final int row, final int column) {
        	if (specialColumns.containsKey(column))
    			return new Color(167, 75, 68);
        	else
        		return new Color(102, 161, 222);
        }
	}
}
