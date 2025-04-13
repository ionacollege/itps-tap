JGAAP is released under the AGPL v3.0 a copy should be included
if not it can be found at http://www.gnu.org/licenses/agpl.html

authorquicktest is an experimental package. We are in the process of making it
more maintainable by cleaning up the code and getting it to work with the latest
version of JGAAP.

We are not responsible for the results outputted by this software. It is meant
for research purposes only.


# Run using Eclipse

* Download Eclipse IDE.

* Download code from GitHub:

  ```
  git clone https://github.com/ionacollege/itps-tap
  ```

* In Eclipse, choose `File > New > Other`. In the dialog that pops up, choose `Java > Java Project From Existing Ant Buildfile`.
* For the Ant buildfile, choose the file `jgaap-6.0.0/src/build.xml`.
* The project files should now all show up in the `Package Explorer` pane on the left side.
* Under `JGAAP/src`, right-click `authorquicktest` and choose `Run As > Java Application`.
* Another dialog should pop up with several options.
* If you are testing documents against an existing author package, choose `AuthorQuickTest - authorquicktest`.
* If you are generating a new author package, choose `PackageGenerator - authorquicktest`.


# Files changed from 2015-01-18

* authorquicktest.Utils
* ExperimentEngine
* API
* Document (got rid of "throws Exception" on constructor)


# Files created since 2015-01-18

* CompoundExperiment
* CompoundLeaveOneOutExperiment
* CompoundTestExperiment
* AuthorPackageIO
