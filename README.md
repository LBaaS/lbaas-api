LBaaS API server environment and build information ( what's needed for development )
------------------------------------------------------------------------------------
Steps 1-10 describe how to set up a development environment and how to build and package
the code. Steps 10 forward define how to install and run the package.

1) Get ubuntu OS, 12.04 64 bit bit OS is recommended
(other OSs are possible but these instructions assume Ubuntu & Debian packages) 

2) Install LBaaS api sources, clone from git repo
git clone https://github.com/LBaaS/lbaas-api.git your-lbaas-directory 

3) Install maven, needed to building
sudo apt-get install maven2 ( Note maven 2 is required )

4) Install java 7, needed for gearman java
sudo add-apt-repository ppa:webupd8team/java -y
sudo apt-get update
sudo apt-get install oracle-java7-installer

5) Add gearman jar to maven .m2 repo, must be done by hand. The .jar is included in sources from git repo
mvn install:install-file -DgroupId=gearman -DartifactId=java-gearman-service -Dversion=0.6.5 -Dpackaging=jar -Dfile=gearman/java-gearman-service-0.6.5.jar

6) Install HPCS Specific middleware, requires access to internal repository
create a local directory called 'cs'
download CsMiddleware-3.24.0.jar from http://keg.dev.uswest.hpcloud.net:8090/archiva/browse/com.hp.csbu.cc/CsMiddleware/3.24.0 into 'cs'
mvn install:install-file -DgroupId=com.hp.csbu.cc -DartifactId=CsMiddleware -Dversion=3.24.0 -Dpackaging=jar -Dfile=cs/CsMiddleware-3.24.0.jar

7) Install HPCS Specific thrift service, requires access to internal repository
copy into 'cs' directory
download CsThriftService:jar:3.9.1 from http://keg.dev.uswest.hpcloud.net:8090/archiva/browse/com.hp.csbu.cc/CsThriftService/3.9.1 into 'cs'
mvn install:install-file -DgroupId=com.hp.csbu.cc -DartifactId=CsThriftService -Dversion=3.9.1 -Dpackaging=jar -Dfile=cs/CsThriftService:jar:3.9.1

8) Build LBaaS API server 
mvn clean install
mvn assembly:assembly ( should have no errors )

9) Make a debian package of binaries
./makedeb.sh


The following steps are for installing and running the LBaaS API server.
---------------------------------------------------------------------------

10) Install mysql 
You need mysql server running somewhere. This can be on the same server where you will be running the LBaaS API server
or it can be on its own server on the network.  Address, User and pwd need to match the values in lbaas.config.
sudo apt-get install mysql-server

11) Initialize DB
A sql script to init the DB can be found in the sources 'mysql/lbaas.sql' and is also installed by the debian package within '/opt/lbaasapi'.
Note! this will nuke the existing DB, so be aware everything in the lbaas DB will be deleted.
mysql/mysql -u root -p < lbaas.sql    

12) Install gearman job server 
You need gearman job server running somewhere. This can be on the same server where you will be running the LBaaS API server
or it can be on its own server on the network. Address and port # need to match the values in lbaas.config.
sudo apt-get install gearman-job-server     
Note, you may need to change /etc/default/gearman-job-server  to listen on 0.0.0.0 rather than 127.0.0.1 

13) Install Debian package
Using the debian package created in the build steps or obtained otherwise, install it on the server where you wish to run the LBaaS API server.
sudo dpkg -i lbaasapi-1.0/lbaasapi-1.0.deb
This will have installed the LBaaS API runtime on your system. Note, in order to run you will need a Java runtime environment as defined in step #4 above.
The following directories and files will have been created.

/opt/lbaasapi
Installed binaries, certificate files, logging properties, lbaas.config, run script and sql schema    

/var/log/lbaasapi
destination for logging files

/etc/init.d/lbaasapi
service start, stop and restart init.d script


14) Configure LBaaS API server
/opt/lbaasapi/lbaas.config defines run time params. Make any requirements.

15) Run it
sudo service lbaasapi start
check the logs to see hows it is running

16) Test it
curl -k https://address-where-its-running:8889/v1/devices
{"devices":[]}                     << will return empty device list, look in var/log/lbaasapi/lbaas.log for run time info.



