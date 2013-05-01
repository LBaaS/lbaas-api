# build debian package for binaries and related files
PACKAGE_NAME=lbaasapi
PACKAGE_VERSION=1.19
PACKAGE_OWNER=pemellquist@gmail.com
PACKAGE_CONTROL=debian/DEBIAN/control
PACKAGE_DESCRIPT="LBaaS API server binaries"
PACKAGE_LOCATION="opt/lbaasapi"
PACKAGE_JAR="target/lbaasapi-0.1.19-jar-with-dependencies.jar"

# create debian package directory and initialize
rm -r -f $PACKAGE_NAME-$PACKAGE_VERSION
mkdir -p $PACKAGE_NAME-$PACKAGE_VERSION
cd $PACKAGE_NAME-$PACKAGE_VERSION 
yes | dh_make -n -s -i -e $PACKAGE_OWNER  > dh_make.txt 
rm debian/README* debian/*.ex debian/*.EX debian/copyright debian/docs debian/control
mkdir debian/DEBIAN
mkdir debian/opt
mkdir -p debian/var/log/lbaasapi
mkdir -p debian/etc/init.d
mkdir debian/$PACKAGE_LOCATION

# create control file
echo "Package: "$PACKAGE_NAME >> $PACKAGE_CONTROL 
echo "Version: "$PACKAGE_VERSION >> $PACKAGE_CONTROL 
echo "Section: web" >> $PACKAGE_CONTROL
echo "Priority: optional" >> $PACKAGE_CONTROL
echo "Architecture: all" >> $PACKAGE_CONTROL
echo "Maintainer: "$PACKAGE_OWNER >> $PACKAGE_CONTROL 
echo "Description: "$PACKAGE_DESCRIPT >> $PACKAGE_CONTROL

# copy LBaaS specific files to target location
cp ../keystore.jks debian/$PACKAGE_LOCATION
cp ../keystone-keystore.jks debian/$PACKAGE_LOCATION
cp ../keystone-truststore.jks debian/$PACKAGE_LOCATION
cp ../lbaas.config debian/$PACKAGE_LOCATION
cp ../mysql/lbaas.sql debian/$PACKAGE_LOCATION
cp ../README.md debian/$PACKAGE_LOCATION
cp ../log4j.properties debian/$PACKAGE_LOCATION
cp ../lbaas.sh debian/$PACKAGE_LOCATION
cp ../$PACKAGE_JAR debian/$PACKAGE_LOCATION
cp ../lbaasapi.initd debian/etc/init.d/lbaasapi

# make the package
dpkg --build debian >> dh_make.txt
mv debian.deb $PACKAGE_NAME-$PACKAGE_VERSION.deb
echo "LBaaS API server Debian Package Created in : "$PACKAGE_NAME-$PACKAGE_VERSION/$PACKAGE_NAME-$PACKAGE_VERSION.deb

cd ..


