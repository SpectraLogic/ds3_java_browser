Building the Browser Yourself
=============================

To build the BlackPearl Eon Browser manually (we'll refer to it as Eon Browser in the rest of the instructions), you will need the [Java 8](http://www.oracle.com/technetwork/java/javase/downloads/index.html) JDK installed.  Once the Java 8 JDK has been installed you can use [git](https://git-scm.com/) to download the sdk directly, or you can download a source zip for each release.

Once the source code has been copied to your local system, cd into the root directory, and run `./gradlew run` from the command line.  On Windows the command to run is: `./gradlew.bat run`  This will build and run Eon Browser.

Here is an example of using git to download the code and then build it using a bash shell.

```shell

$ git clone https://github.com/SpectraLogic/ds3_java_browser.git
$ cd ds3_java_browser
$ ./gradlew run
```

To build a distributable copy of Eon Browser run `./gradlew distTar` or `./gradlew distZip`.  Depending on which one is used you'll either get a zip file or a tar file generated that has the built Eon Browser code in it. **Note:** You can also build both at the same time by running `./gradlew distTar distZip`.  The bundles are written to `./dsb-gui/build/distributions`

The built code does not include a Java JRE, so any system where you plan on running the browser must have the Java 8 JRE installed.

Once you've copied and extracted the zip or tar file, you can run the application by first cding into the root of the extracted code and running `./bin/dsb-gui`

**NOTE:** these instructions do not show how to build an installer for the browser.  If an installer is desired, please contact us by going to our [Google Group](https://groups.google.com/forum/#!forum/spectralogicds3-sdks) and asking us there.
