#!/bin/bash

VERSION='5.0.3'

javapackager -deploy -native deb -srcfiles ../../dsb-gui/build/libs/dsb-gui-$VERSION-all.jar -outdir deploy -appclass com.spectralogic.dsbrowser.gui.Main -outfile BlackPearlEonBrowser -name BlackPearlEonBrowser -Bicon=icon_128x128.png -BappVersion=$VERSION -v
