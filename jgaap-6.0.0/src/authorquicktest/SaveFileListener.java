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

import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

public abstract class SaveFileListener implements ActionListener
{
	JFrame frame;
	String fileType;
	String fileDialogPrompt;

	public SaveFileListener(JFrame f, String type, String prompt)
	{
		frame = f;
		fileType = type;
		fileDialogPrompt = prompt;
	}

	@Override
	public void actionPerformed(ActionEvent event)
	{
		FileDialog saveFileDialog = new FileDialog(frame, fileDialogPrompt, FileDialog.SAVE);
		saveFileDialog.setVisible(true);

		String fileName = saveFileDialog.getFile();
		if (fileName != null) {
			// Add the file type to the end of the filename if it isn't already there.
			String saveFileName = fileName.endsWith(fileType)
					? (saveFileDialog.getDirectory() + fileName)
					: (saveFileDialog.getDirectory() + fileName + fileType);
			// Let the user know if something went wrong saving the file.
			if (!save(fileType, saveFileName)) {
				JOptionPane.showMessageDialog(frame,
						"There was a problem saving the file " + fileName + ".", "Unable to save file",
						JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	protected abstract boolean save(String fileType, String saveFileName);
}
