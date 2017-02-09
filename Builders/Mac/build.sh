#!/bin/bash

CODESIGN_IDENTITY='Developer ID Application: Spectra Logic Corporation (YAUF7295LE)'

VERSION='2.0.45'

#/Library/Java/JavaVirtualMachines/jdk1.8.0_121.jdk/Contents/Home/bin/javapackager -deploy -native dmg -srcfiles ../../dsb-gui/build/libs/dsb-gui-2.0.44-all.jar -outdir deploy -appclass com.spectralogic.dsbrowser.gui.Main -outfile DeepStorageBrowser -name DeepStorageBrowser -Bmac.signing-key-developer-id-app="3rd Party Mac Developer Application: Spectra Logic Corporation (YAUF7295LE)" -BappVersion=2.0.44 -Bicon=DeepStorageBrowser.icns -v
/Library/Java/JavaVirtualMachines/jdk1.8.0_121.jdk/Contents/Home/bin/javapackager -deploy -native dmg -srcfiles ../../dsb-gui/build/libs/dsb-gui-$VERSION-all.jar -outdir deploy -appclass com.spectralogic.dsbrowser.gui.Main -outfile BlackPearlEonBrowser -name BlackPearlEonBrowser -Bmac.signing-key-developer-id-app="$CODESIGN_IDENTITY" -BappVersion=$VERSION -Bicon=iconWithBackground.icns -v

codesign -s "$CODESIGN_IDENTITY" -v deploy/bundles/BlackPearlEonBrowser-$VERSION.dmg
