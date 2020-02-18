#!/bin/bash

CODESIGN_IDENTITY='Developer ID Application: Spectra Logic Corporation (YAUF7295LE)'

VERSION='5.0.10'

/Library/Java/JavaVirtualMachines/amazon-corretto-8.jdk/Contents/Home/bin/javafxpackager -deploy -native dmg -srcfiles ../../dsb-gui/build/libs/dsb-gui-$VERSION-all.jar -outdir deploy -appclass com.spectralogic.dsbrowser.gui.Main -outfile BlackPearlEonBrowser -name BlackPearlEonBrowser -BappVersion=$VERSION -Bicon=eonbrowse.icns -v

#codesign -s "$CODESIGN_IDENTITY" -v deploy/bundles/BlackPearlEonBrowser-$VERSION.dmg
