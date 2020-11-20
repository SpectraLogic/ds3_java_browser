![Eon Browser](https://github.com/SpectraLogic/ds3_java_browser/blob/master/Builders/Mac/EON%20Browser%20Final%20Logo.png)
====================

[![Build Status](https://travis-ci.com/SpectraLogic/ds3_java_browser.svg?branch=master)](https://travis-ci.com/SpectraLogic/ds3_java_browser) [![Apache V2 License](http://img.shields.io/badge/license-Apache%20V2-blue.svg)](https://github.com/SpectraLogic/ds3_java_sdk/blob/master/LICENSE.md)

## Overview
Spectra Logic’s BlackPearl Eon Browser is a free and open-source Spectra® BlackPearl® client (application) that provides users an easy-to-use tool to archive files to the BlackPearl Deep Storage Gateway. The intuitive graphical interface facilitates the management and transfer of files between local drives or network shares and BlackPearl. The Deep Storage Browser’s simple user interface is similar to an FTP client, allowing users to search, select, group, archive, and restore by simply dragging and dropping files between their file system and BlackPearl.

## Releases
The latest release can be found at [Releases](https://github.com/SpectraLogic/ds3_java_browser/releases) and includes pre-built installers for Windows, Mac, and Ubuntu.  For other Operating Systems and Platforms, please see the guide on building it [manually](BUILD.md).

## Yum repository
Create a file named spectralogic.repo in /etc/yum.repos.d containing the following:
```
#Bintray-spectralogic
[bintray-spectralogic-eon_browser_rpm]
name=spectralogic-eon_browser
baseurl=https://dl.bintray.com/spectralogic/eon_browser_rpm
gpgcheck=0
enabled=1
```
Now you can install Eon Browser via Yum:
```
sudo yum install eonbrowser
```
To run Eon Browser simply invoke it:
```
eonbrowser
```

## Apt repository
Add the following to the bottom of /etc/apt/sources.list
```
deb [trusted=yes] https://dl.bintray.com/spectralogic/eon_browser_deb stable main
```
Update the list of available packages:
```
sudo apt-get update
```
Install Eon Browser:
```
sudo apt-get install eonbrowser
```
To run Eon Browser simply invoke it from the command line:
```
eonbrowser
```
## Contact Us
Join us at our [Google Groups](https://groups.google.com/d/forum/spectralogicds3-sdks) forum to ask questions, or see frequently asked questions.

## Contributing
If you would like to contribute to the source code, sign the [Contributors Agreement](https://developer.spectralogic.com/contributors-agreement/) and make sure that your source conforms to our [Java Style Guide](https://github.com/SpectraLogic/spectralogic.github.com/wiki/Java-Style-Guide).  For an overview of how we use Github, please review our [Github Workflow](https://github.com/SpectraLogic/spectralogic.github.com/wiki/Github-Workflow).

## Third Party License
This project uses open source third-party software, licenses can be found at [Third-party](/dsb-gui/src/main/resources/ThirdParty/ControlsFX-License.txt)
