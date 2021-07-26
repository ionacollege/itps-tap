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

public class PreProc {

	/**
	 * @param args
	 */

	public static void main(String[] args) {
		String[] argJGAAP = {};

		//argJGAAP = new String[] { "-ee", "/Users/Sean/Documents/College/Research/Thomas Paine/Projects/To The People/EE Files/ee-all-HsDk-test-to-the-people-LsvmMpNncosdSmo-Cpost-Mce.csv" };
		//argJGAAP = new String[] { "-ee", "/Users/Sean/Documents/College/Research/Thomas Paine/Projects/To The People/EE Files/ee-all-HsDk-test-to-the-people-LsvmMpNncosdSmo-Cg2Cg3FwisLfreqMwfw-Mce.csv" };
		//argJGAAP = new String[] { "-ee", "/Users/Sean/Documents/College/Research/Thomas Paine/Projects/To The People/EE Files/ee-all-HsDk-test-to-the-people-LsvmMpNncosdSmo-NrtPosPosg2Posg3PrepScg2-Mce.csv" };
		//argJGAAP = new String[] { "-ee", "/Users/Sean/Documents/College/Research/Thomas Paine/Projects/To The People/EE Files/ee-all-HsDk-test-to-the-people-LsvmMpNncosdSmo-Scg3SufViwWg2Ws-Mce.csv" };

		argJGAAP = new String[] { "-el", "/Users/Sean/Documents/College/Research/Thomas Paine/paineproject/Packages/american-redone/EE Files/ee-american-redone-LsvmMpNncosdSmo-Cg2Cg3FwisLfreqMwfw-Mce.csv" };
		//argJGAAP = new String[] { "-el", "/Users/Sean/Documents/College/Research/Thomas Paine/paineproject/Packages/american-redone/EE Files/ee-american-redone-LsvmMpNncosdSmo-Cg2Cg3FwisLfreqMwfw-Mce.csv" };
		//argJGAAP = new String[] { "-el", "/Users/Sean/Documents/College/Research/Thomas Paine/paineproject/Packages/american-redone/EE Files/ee-american-redone-LsvmMpNncosdSmo-NrtPosPosg2Posg3PrepScg2-Mce.csv" };
		//argJGAAP = new String[] { "-el", "/Users/Sean/Documents/College/Research/Thomas Paine/paineproject/Packages/american-redone/EE Files/ee-american-redone-LsvmMpNncosdSmo-Scg3SufViwWg2Ws-Mce.csv" };

		com.jgaap.JGAAP.main(argJGAAP);
	}


}
