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

public class ExperimentSettings
{
	public static final String DEFAULT_LEAVE_ONE_OUT_RESULTS_FILE_PATH = "/com/jgaap/resources/leave_one_out_results.csv";
	public static final double DEFAULT_ACCURACY_THRESHOLD = 0.50;

	private String leaveOneOutResultsFilePath;
	private double accuracyThreshold;

	private static final ExperimentSettings INSTANCE = new ExperimentSettings();

	public static ExperimentSettings getInstance() { return INSTANCE; }

	public ExperimentSettings()
	{
		leaveOneOutResultsFilePath = DEFAULT_LEAVE_ONE_OUT_RESULTS_FILE_PATH;
		accuracyThreshold = DEFAULT_ACCURACY_THRESHOLD;
	}

	public void setAccuracyThreshold(double threshold)
	{
		if (threshold > 0)
			accuracyThreshold = threshold;
	}

	public double getAccuracyThreshold() { return accuracyThreshold; }

	public void setLeaveOneOutResultsFilePath(String path)
	{
		leaveOneOutResultsFilePath = path;
	}

	public void resetLeaveOneOutResultsFilePath()
	{
		leaveOneOutResultsFilePath = DEFAULT_LEAVE_ONE_OUT_RESULTS_FILE_PATH;
	}

	public String getLeaveOneOutResultsFilePath() { return leaveOneOutResultsFilePath; }

}
