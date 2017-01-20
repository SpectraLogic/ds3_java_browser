#!/bin/sh
 
PACKAGE_NAME="deepstoragebrowser"
PACKAGE_VERSION="2.1"
SOURCE_DIR=$PWD
TEMP_DIR="/tmp"

mkdir -p $TEMP_DIR/debian/DEBIAN
mkdir -p $TEMP_DIR/debian/lib
mkdir -p $TEMP_DIR/debian/usr/deepstoragebrowser
mkdir -p $TEMP_DIR/debian/usr/share/applications
mkdir -p $TEMP_DIR/debian/usr/share/$PACKAGE_NAME
mkdir -p $TEMP_DIR/debian/usr/share/doc/$PACKAGE_NAME
mkdir -p $TEMP_DIR/debian/usr/share/common-licenses/$PACKAGE_NAME

echo "hello"
echo "Package: $PACKAGE_NAME" > $TEMP_DIR/debian/DEBIAN/control
echo "Version: $PACKAGE_VERSION" >> $TEMP_DIR/debian/DEBIAN/control
cat control >> $TEMP_DIR/debian/DEBIAN/control
cat postrm >> $TEMP_DIR/debian/DEBIAN/postrm


chmod 0755 $TEMP_DIR/debian/DEBIAN/postrm

cat preinst >> $TEMP_DIR/debian/DEBIAN/preinst

chmod 0755 $TEMP_DIR/debian/DEBIAN/preinst

 
cp *.desktop $TEMP_DIR/debian/usr/share/applications/
cp copyright $TEMP_DIR/debian/usr/share/common-licenses/$PACKAGE_NAME/ # results in no copyright warning
cp copyright $TEMP_DIR/debian/usr/share/doc/$PACKAGE_NAME/ # results in obsolete location warning
echo hii 11
cp *.jar $TEMP_DIR/debian/usr/share/$PACKAGE_NAME/
echo hii 22
cp $PACKAGE_NAME $TEMP_DIR/debian/usr/deepstoragebrowser/
echo "$PACKAGE_NAME ($PACKAGE_VERSION) trusty; urgency=low" > changelog
echo "  * Rebuild" >> changelog
echo " -- Spectra Logic Corporation <www.spectralogic.com> `date -R`" >> changelog
gzip -9c changelog > $TEMP_DIR/debian/usr/share/doc/$PACKAGE_NAME/changelog.gz
 echo png
cp *.png $TEMP_DIR/debian/usr/share/$PACKAGE_NAME/
chmod 0664 $TEMP_DIR/debian/usr/share/$PACKAGE_NAME/*png
echo pngdone

PACKAGE_SIZE=`du -bs $TEMP_DIR/debian | cut -f 1`
PACKAGE_SIZE=$((PACKAGE_SIZE/1024))
echo "Installed-Size: $PACKAGE_SIZE" >> $TEMP_DIR/debian/DEBIAN/control
chown -R root $TEMP_DIR/debian/
chgrp -R root $TEMP_DIR/debian/
 
cd $TEMP_DIR/
dpkg --build debian
mv debian.deb $SOURCE_DIR/$PACKAGE_NAME-$PACKAGE_VERSION.deb
rm -r $TEMP_DIR/debian
