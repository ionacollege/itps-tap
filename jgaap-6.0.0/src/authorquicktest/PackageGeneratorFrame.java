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
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.jgaap.generics.Document;

import authorquicktest.ResultsParser.WEIGHT_METHOD;

public class PackageGeneratorFrame extends JFrame implements StatusLog {

	// Leave as lower case because in the future it might be a setting that can be changed.
	private static final String defaultPackagePath = com.jgaap.JGAAPConstants.JGAAP_RESOURCE_PACKAGE + "all.txt";

	private static final long serialVersionUID = 3557449364519961402L;

	// GUI
	private JMenu fileMenu;
	private JMenuItem saveCsvFileMenuItem;
	private JMenuItem saveExcelFileMenuItem;
	private JMenuItem savePackageFileMenuItem;
	private JMenuItem saveRawResultsFileMenuItem;

	private JLabel corpusDirLabel;
	private JTextField corpusDirectoryTextField;
	private JButton browseButton;
	private JLabel authorsLabel;
	private JPanel authorListPanel;
	private JScrollPane authorScrollPane;
	private List<JCheckBox> authorCheckBoxes;
	private JCheckBox selectAllCheckBox;
	private JButton generatePackageButton;

	private JLabel featuresLabel;
	private JLabel classifiersLabel;
	private JLabel cullersLabel;
	private JLabel canonicizersLabel;

	private JTextField featuresTextField;
	private JTextField classifiersTextField;
	private JTextField cullersTextField;
	private JTextField canonicizersTextField;

	private ButtonGroup weightMethodGroup;
	private JLabel weightMethodLabel;
	private JRadioButton accuracyRadioButton;
	private JRadioButton precisionRadioButton;
	private JRadioButton recallRadioButton;
	private JLabel weightThresholdLabel;
	private JSpinner weightThresholdSpinner;
	private JCheckBox autoThresholdCheckBox;

	private JScrollPane statusScrollPane;
	private JTextArea statusTextArea;

	// Data
	File corpusDir;
	List<Document> documents;

	public PackageGeneratorFrame() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		authorCheckBoxes = new ArrayList<JCheckBox>();
		createInterface();
		layoutInterface();

		List<String> features = Arrays.asList(
				"Character NGrams|N:2",
				"Character NGrams|N:3",
				"Coarse POS Tagger",
				"First Word In Sentence",
				"Lexical Frequencies",
				"Lexical Stress",
				"MW Function Words",
				"Naming Reaction Times",
				"POS",
				"POS NGrams|N:2",
				"POS NGrams|N:3",
				"Prepositions",
				"Sorted Character NGram|N:2",
				"Sorted Character NGram|N:3",
				"Suffices",
				"Vowel-initial Words",
				"Word NGrams|N:2",
				"Word stems");
		List<String> classifiers = Arrays.asList(
//				"Linear SVM",
				"Nearest Neighbor Driver;Cosine Distance",
				"WEKA Multilayer Perceptron",
				"WEKA SMO");
		List<String> cullers = Arrays.asList(
				"Most Common Events");


		featuresTextField.setText(String.join(",", features));
		classifiersTextField.setText(String.join(",", classifiers));
		cullersTextField.setText(String.join(",", cullers));
		canonicizersTextField.setText("");
	}

	public void setStatus(String status)
	{
		if (status != null) {
			//System.out.println(status);
			statusTextArea.append(status + "\n");
		}
	}

	private void setCorpusDirectory(File dir) {
		if (!dir.exists()) {
			corpusDirectoryTextField.setBackground(Color.YELLOW);
			return;
		}
		if (!dir.isDirectory()) {
			corpusDirectoryTextField.setBackground(new Color(206, 226, 244));
			corpusDir = dir;
			return;
		}

		corpusDirectoryTextField.setBackground(Color.WHITE);
		corpusDir = dir;
		// Remove old check boxes.
		authorListPanel.removeAll();
		authorCheckBoxes.clear();
		// Create new check boxes based on subdirectories.
		selectAllCheckBox.setSelected(true);
		for (File file : dir.listFiles()) {
			if (file.isDirectory() && !file.isHidden()) {
				JCheckBox checkBox = new JCheckBox(file.getName());
				checkBox.setSelected(true);
				authorCheckBoxes.add(checkBox);
				authorListPanel.add(checkBox);
			}
		}
		// Force update of panel.
		authorListPanel.revalidate();
		authorListPanel.repaint();
	}

	private List<File> authorDirs() {
		List<File> dirs = new ArrayList<File>();
		for (JCheckBox checkBox : authorCheckBoxes)
			if (checkBox.isSelected())
				dirs.add(new File(corpusDir.getAbsolutePath() + "/" + checkBox.getText()));
		return dirs;
	}

	private List<List<String>> getExperimentTable() {
		List<List<String>> expTable = new ArrayList<List<String>>();
		expTable.add(Arrays.asList(featuresTextField.getText().split(",")));
		expTable.add(Arrays.asList(classifiersTextField.getText().split(",")));
		expTable.add(Arrays.asList(cullersTextField.getText().split(",")));
		expTable.add(Arrays.asList(canonicizersTextField.getText().split(",")));
		return expTable;
	}

	private void createInterface() {
		// Menus
		saveCsvFileMenuItem = new JMenuItem("Save CSV File");
		saveCsvFileMenuItem.addActionListener(new PGSaveFileListener(this, ".csv", "Save results file as CSV..."));
		saveExcelFileMenuItem = new JMenuItem("Save Excel File (Alt+E)");
		saveExcelFileMenuItem.setMnemonic(KeyEvent.VK_E);
		saveExcelFileMenuItem.addActionListener(new PGSaveFileListener(this, ".xlsx", "Save results file as Excel spreadsheet..."));
		savePackageFileMenuItem = new JMenuItem("Save Package File (Alt+P)");
		savePackageFileMenuItem.setMnemonic(KeyEvent.VK_P);
		savePackageFileMenuItem.addActionListener(new PGSaveFileListener(this, ".txt", "Save package..."));
		saveRawResultsFileMenuItem = new JMenuItem("Save Raw Results File  (Alt+R)");
		saveRawResultsFileMenuItem.setMnemonic(KeyEvent.VK_R);
		saveRawResultsFileMenuItem.addActionListener(new PGSaveFileListener(this, ".out", "Save raw results..."));

		fileMenu = new JMenu("File (Alt+F)");
		fileMenu.setMnemonic(KeyEvent.VK_F);
		//fileMenu.add(saveCsvFileMenuItem);
		fileMenu.add(saveExcelFileMenuItem);
		fileMenu.add(savePackageFileMenuItem);
		fileMenu.add(saveRawResultsFileMenuItem);

		JMenuBar menuBar = new JMenuBar();
		menuBar.add(fileMenu);
		setJMenuBar(menuBar);

		// And the rest of the GUI
		corpusDirLabel = new JLabel("Corpus Directory:");
		corpusDirectoryTextField = new JTextField();
		corpusDirectoryTextField.getDocument().addDocumentListener(new AuthorDirTextFieldChangedListener());
		browseButton = new JButton("Browse (Alt+B)");
		browseButton.setMnemonic(KeyEvent.VK_B);
		browseButton.addActionListener(new GetAuthorDirectoryListener());
		authorsLabel = new JLabel("Authors");
		selectAllCheckBox = new JCheckBox("Select All");
		selectAllCheckBox.addActionListener(new SelectAllListener());
		authorListPanel = new JPanel();
		authorScrollPane = new JScrollPane(authorListPanel);
		generatePackageButton = new JButton("Generate Package (Alt+G)");
		generatePackageButton.setMnemonic(KeyEvent.VK_G);
		generatePackageButton.addActionListener(new GeneratePackageListener());

		canonicizersLabel = new JLabel("Canonicizers");
		featuresLabel = new JLabel("Features");
		classifiersLabel = new JLabel("Classifiers");
		cullersLabel = new JLabel("Cullers");

		canonicizersTextField = new JTextField();
		featuresTextField = new JTextField();
		classifiersTextField = new JTextField();
		cullersTextField = new JTextField();

		weightMethodLabel = new JLabel("Weight Method");
		accuracyRadioButton = new JRadioButton("Accuracy");
		accuracyRadioButton.setSelected(true);
		accuracyRadioButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				autoThresholdCheckBox.setEnabled(true);
			}
		});
		precisionRadioButton = new JRadioButton("Precision");
		precisionRadioButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				autoThresholdCheckBox.setSelected(false);
				autoThresholdCheckBox.setEnabled(false);
				weightThresholdSpinner.setEnabled(true);
			}
		});
		recallRadioButton = new JRadioButton("Recall");
		recallRadioButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				autoThresholdCheckBox.setSelected(false);
				autoThresholdCheckBox.setEnabled(false);
				weightThresholdSpinner.setEnabled(true);
			}
		});
		weightMethodGroup = new ButtonGroup();
		weightMethodGroup.add(accuracyRadioButton);
		weightMethodGroup.add(precisionRadioButton);
		weightMethodGroup.add(recallRadioButton);
		weightThresholdLabel = new JLabel("Weight Threshold");
		SpinnerNumberModel weightThresholdSpinnerModel = new SpinnerNumberModel(0.5, 0.01, 1.0, 0.1);
		weightThresholdSpinner = new JSpinner(weightThresholdSpinnerModel);
		autoThresholdCheckBox = new JCheckBox("Auto Threshold");
		autoThresholdCheckBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				weightThresholdSpinner.setEnabled(!autoThresholdCheckBox.isSelected());
			}
		});
		autoThresholdCheckBox.setSelected(true);
		weightThresholdSpinner.setEnabled(false);

		statusTextArea = new JTextArea();
		statusTextArea.setEditable(false);
		statusScrollPane = new JScrollPane(statusTextArea);
	}

	private void layoutInterface() {
		Container contentPane = getContentPane();

		// Main layout
		contentPane.setLayout(new GridBagLayout());

		JPanel corpusDirPanel = new JPanel();
		corpusDirPanel.setLayout(new GridBagLayout());
		GridBagConstraints cdc1 = new GridBagConstraints();
		cdc1.insets = new Insets(0, 10, 0, 0); // Left padding
		GridBagConstraints cdc2 = new GridBagConstraints();
		cdc2.fill = GridBagConstraints.HORIZONTAL;
		cdc2.weightx = 1.0;
		corpusDirPanel.add(corpusDirLabel, cdc1);
		corpusDirPanel.add(corpusDirectoryTextField, cdc2);
		corpusDirPanel.add(browseButton);

		JPanel authorPanel = new JPanel();
		authorPanel.setLayout(new GridBagLayout());
		JPanel authorsLabelPanel = new JPanel();
		authorsLabelPanel.setLayout(new BoxLayout(authorsLabelPanel, BoxLayout.LINE_AXIS));
		authorsLabelPanel.add(authorsLabel);
		authorsLabelPanel.add(Box.createHorizontalGlue());
		authorsLabelPanel.add(selectAllCheckBox);
		GridBagConstraints apc1 = new GridBagConstraints();
		apc1.fill = GridBagConstraints.BOTH;
		apc1.weightx = 1.0;
		apc1.gridy = 0;
		authorPanel.add(corpusDirPanel, apc1);
		GridBagConstraints apc2 = new GridBagConstraints();
		apc2.fill = GridBagConstraints.BOTH;
		apc2.weightx = 1.0;
		apc2.insets = new Insets(0, 0, 10, 20);
		apc2.gridy = 1;
		authorPanel.add(authorsLabelPanel, apc2);
		GridBagConstraints apc3 = new GridBagConstraints();
		apc3.fill = GridBagConstraints.BOTH;
		apc3.weighty = 1.0;
		apc3.weightx = 1.0;
		apc3.gridy = 2;
		authorListPanel.setLayout(new BoxLayout(authorListPanel, BoxLayout.PAGE_AXIS));
		authorPanel.add(authorScrollPane, apc3);

		JPanel weightMethodPanel = new JPanel();
		weightMethodPanel.setLayout(new BoxLayout(weightMethodPanel, BoxLayout.LINE_AXIS));
		weightMethodPanel.add(weightMethodLabel);
		weightMethodPanel.add(accuracyRadioButton);
//		weightMethodPanel.add(precisionRadioButton);
//		weightMethodPanel.add(recallRadioButton);
		JPanel weightThresholdPanel = new JPanel();
		weightThresholdPanel.setLayout(new BoxLayout(weightThresholdPanel, BoxLayout.LINE_AXIS));
		weightThresholdPanel.add(weightThresholdLabel);
		weightThresholdPanel.add(weightThresholdSpinner);
		weightThresholdPanel.add(autoThresholdCheckBox);

		JPanel experimentTablePanel = new JPanel();
		experimentTablePanel.setLayout(new BoxLayout(experimentTablePanel, BoxLayout.PAGE_AXIS));
		experimentTablePanel.add(weightMethodPanel);
		experimentTablePanel.add(weightThresholdPanel);
		experimentTablePanel.add(featuresLabel);
		experimentTablePanel.add(featuresTextField);
		experimentTablePanel.add(classifiersLabel);
		experimentTablePanel.add(classifiersTextField);
		experimentTablePanel.add(cullersLabel);
		experimentTablePanel.add(cullersTextField);
		experimentTablePanel.add(canonicizersLabel);
		experimentTablePanel.add(canonicizersTextField);
		experimentTablePanel.add(generatePackageButton);

		GridBagConstraints c1 = new GridBagConstraints();
		c1.fill = GridBagConstraints.BOTH;
		c1.weightx = 0.5;
		c1.weighty = 0.3;
		c1.insets = new Insets(10, 10, 10, 10);
		GridBagConstraints c2 = new GridBagConstraints();
		c2.fill = GridBagConstraints.BOTH;
		c2.weightx = 0.5;
		c2.insets = new Insets(10, 10, 10, 10);
		GridBagConstraints c3 = new GridBagConstraints();
		//c3.fill = GridBagConstraints.BOTH;
		//c3.weightx = 0.5;
		c3.gridy = 1;
		c3.gridwidth = 2;
		//c3.insets = new Insets(10, 10, 10, 10);
		GridBagConstraints c4 = new GridBagConstraints();
		c4.fill = GridBagConstraints.BOTH;
		c4.weighty = 0.2;
		c4.insets = new Insets(10, 10, 10, 10);
		c4.gridy = 2;
		c4.gridwidth = GridBagConstraints.REMAINDER;
		contentPane.add(authorPanel, c1);
		contentPane.add(experimentTablePanel, c2);
		//contentPane.add(weightMethodPanel, c3);
		contentPane.add(statusScrollPane, c4);
	}

	private class PGSaveFileListener extends SaveFileListener {
		public PGSaveFileListener(JFrame f, String type, String prompt) {
			super(f, type, prompt);
		}

		@Override
		protected boolean save(String fileType, String saveFileName) {
			// Some sloppy ifs to call the correct save method in PackageGenerator.
			boolean isFileSaved;
			if (fileType.equalsIgnoreCase(".csv"))
				isFileSaved = authorquicktest.PackageGenerator.saveCsvFile(saveFileName);
			else if (fileType.equalsIgnoreCase(".xlsx"))
				isFileSaved = authorquicktest.PackageGenerator.saveExcelFile(saveFileName);
			else if (fileType.equalsIgnoreCase(".txt"))
				isFileSaved = authorquicktest.PackageGenerator.savePackageFile(saveFileName, getExperimentTable(), documents);
			else if (fileType.equalsIgnoreCase(".out"))
				isFileSaved = authorquicktest.PackageGenerator.saveRawResultsFile(saveFileName);
			else
				isFileSaved = false;
			return isFileSaved;
		}
	}

	private class GetAuthorDirectoryListener implements ActionListener {
		public GetAuthorDirectoryListener() {}

		@Override
		public void actionPerformed(ActionEvent event) {
			JFileChooser dirChooser = new JFileChooser(corpusDir);
			dirChooser.setDialogTitle("Select the directory containing the author corpus...");
			dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			if (dirChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
				corpusDirectoryTextField.setText(dirChooser.getSelectedFile().getAbsolutePath());
			}
		}
	}

	private class GeneratePackageListener implements ActionListener {
		public GeneratePackageListener() {}

		@Override
		public void actionPerformed(ActionEvent event) {
			new Thread(new Runnable() {
				public void run() {
					if (!corpusDir.isDirectory()) {
						JOptionPane.showMessageDialog(PackageGeneratorFrame.this, "Need to choose a directory for the corpus. Running from\na results files may be added in the future.",
								"Unable to Generate Package", JOptionPane.WARNING_MESSAGE);
						return;
					}

					statusTextArea.setText("");
					documents = new ArrayList<Document>();
					documents = authorquicktest.Utils.getCorpusFromDirs(authorDirs());
					for (Document doc : documents) {
						try {
							doc.load();
						} catch (Exception e) {
							PackageGeneratorFrame.this.setStatus("Problem loading " + doc.getTitle() + " by " + doc.getAuthor() + ". Unable to continue.");
							return;
						}
					}

					saveCsvFileMenuItem.setEnabled(false);
					saveExcelFileMenuItem.setEnabled(false);
					savePackageFileMenuItem.setEnabled(false);
					browseButton.setEnabled(false);
					accuracyRadioButton.setEnabled(false);
					precisionRadioButton.setEnabled(false);
					recallRadioButton.setEnabled(false);
					weightThresholdSpinner.setEnabled(false);
					autoThresholdCheckBox.setEnabled(false);
					generatePackageButton.setEnabled(false);
					corpusDirectoryTextField.setEnabled(false);
					canonicizersTextField.setEnabled(false);
					featuresTextField.setEnabled(false);
					classifiersTextField.setEnabled(false);
					cullersTextField.setEnabled(false);
					selectAllCheckBox.setEnabled(false);
					for (JCheckBox checkBox : authorCheckBoxes)
						checkBox.setEnabled(false);

					List<List<String>> expTable = new ArrayList<List<String>>();
					expTable.add(Arrays.asList(featuresTextField.getText().split(",")));
					expTable.add(Arrays.asList(classifiersTextField.getText().split(",")));
					expTable.add(Arrays.asList(cullersTextField.getText().split(",")));
					expTable.add(Arrays.asList(canonicizersTextField.getText().split(",")));

					WEIGHT_METHOD weightMethod = accuracyRadioButton.isSelected() ? WEIGHT_METHOD.ACCURACY
													: (precisionRadioButton.isSelected() ? WEIGHT_METHOD.PRECISION
													: WEIGHT_METHOD.RECALL);
					double weightThreshold = autoThresholdCheckBox.isSelected() ? 0.0 : (double)weightThresholdSpinner.getValue();
					PackageGenerator.generatePackage(documents, expTable, weightMethod, weightThreshold);

					saveCsvFileMenuItem.setEnabled(true);
					saveExcelFileMenuItem.setEnabled(true);
					savePackageFileMenuItem.setEnabled(true);
					browseButton.setEnabled(true);
					accuracyRadioButton.setEnabled(true);
					precisionRadioButton.setEnabled(true);
					recallRadioButton.setEnabled(true);
					weightThresholdSpinner.setEnabled(!autoThresholdCheckBox.isSelected());
					autoThresholdCheckBox.setEnabled(accuracyRadioButton.isSelected());
					generatePackageButton.setEnabled(true);
					corpusDirectoryTextField.setEnabled(true);
					canonicizersTextField.setEnabled(true);
					featuresTextField.setEnabled(true);
					classifiersTextField.setEnabled(true);
					cullersTextField.setEnabled(true);
					selectAllCheckBox.setEnabled(true);
					for (JCheckBox checkBox : authorCheckBoxes)
						checkBox.setEnabled(true);
				}
			}).start();
		}
	}

	private class SelectAllListener implements ActionListener {
		public SelectAllListener() {}

		@Override
		public void actionPerformed(ActionEvent event) {
			boolean isSelected = selectAllCheckBox.isSelected();
			for (JCheckBox checkBox : authorCheckBoxes)
				checkBox.setSelected(isSelected);
		}
	}

	private class AuthorDirTextFieldChangedListener implements DocumentListener
	{
		@Override
		public void changedUpdate(DocumentEvent event) {
			return;
		}

		@Override
		public void insertUpdate(DocumentEvent e) {
			setCorpusDirectory(new File(corpusDirectoryTextField.getText()));
		}

		@Override
		public void removeUpdate(DocumentEvent e) {
			setCorpusDirectory(new File(corpusDirectoryTextField.getText()));
		}
	}
}
