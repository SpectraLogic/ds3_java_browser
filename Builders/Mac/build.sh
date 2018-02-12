#!/bin/bash

CODESIGN_IDENTITY='Developer ID Application: Spectra Logic Corporation (YAUF7295LE)'

VERSION='2.1.6'

#/Library/Java/JavaVirtualMachines/jdk1.8.0_144.jdk/Contents/Home/bin/javapackager -deploy -native dmg -srcfiles ../../dsb-gui/build/libs/dsb-gui-$VERSION-all.jar -outdir deploy -appclass com.spectralogic.dsbrowser.gui.Main -outfile BlackPearlEonBrowser -name BlackPearlEonBrowser -Bmac.signing-key-developer-id-app="$CODESIGN_IDENTITY" -BappVersion=$VERSION -Bicon=eonbrowse.icns -v
/Library/Java/JavaVirtualMachines/jdk1.8.0_162.jdk/Contents/Home/bin/javapackager -deploy -native dmg -srcfiles ../../dsb-gui/build/libs/dsb-gui-$VERSION-all.jar -outdir deploy -appclass com.spectralogic.dsbrowser.gui.Main -outfile BlackPearlEonBrowser -name BlackPearlEonBrowser -BappVersion=$VERSION -Bicon=eonbrowse.icns -v

codesign -s "$CODESIGN_IDENTITY" -v deploy/bundles/BlackPearlEonBrowser-$VERSION.dmg
