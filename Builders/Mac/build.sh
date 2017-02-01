#!/bin/bash



/Library/Java/JavaVirtualMachines/jdk1.8.0_121.jdk/Contents/Home/bin/javapackager -deploy -native dmg -srcfiles ../../dsb-gui/build/libs/dsb-gui-2.0.41-all.jar -outdir deploy -appclass com.spectralogic.dsbrowser.gui.Main -outfile DeepStorageBrowser -name DeepStorageBrowser -Bmac.signing-key-developer-id-app="3rd Party Mac Developer Application: Spectra Logic Corporation (YAUF7295LE)" -BappVersion=2.0.41 -Bicon=DeepStorageBrowser.icns -v
