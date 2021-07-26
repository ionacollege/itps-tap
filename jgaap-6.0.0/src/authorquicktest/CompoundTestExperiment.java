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
import java.util.ArrayList;
import java.util.List;
import com.jgaap.generics.Document;

public class CompoundTestExperiment extends CompoundExperiment
{
	// Fields
	private String leaveOneOutResultsCsv;

	// Constructors
	public CompoundTestExperiment() throws IOException
	{
		super();
	}
	public CompoundTestExperiment(StatusLog l) throws IOException
	{
		super(l);
	}

	// General public methods
	public boolean loadPackage(String fileName)
	{
		StringBuffer looResults = new StringBuffer();
		List<Document> docs = new ArrayList<Document>();
		List<List<String>> expTable = new ArrayList<List<String>>();

		if (!AuthorPackageIO.readAuthorPackage(fileName, looResults, expTable, docs))
			return false;

		// Reinitialize variables, so we don't have anything left
		// over from the last experiment.
		resetExperiment();

		leaveOneOutResultsCsv = looResults.toString();

		setDocuments(docs);
		setExperimentTable(expTable);

		return true;
	}

	public String getLeaveOneOutResultsAsCSV()
	{
		return leaveOneOutResultsCsv;
	}

	// Protected methods
	protected void createIndividualExperiments(List<Document> documents)
	{
		addIndividualExperiment(new IndividualExperiment(documents));
	}
}
