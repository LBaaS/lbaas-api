LBaaS API server environment and build information
--------------------------------------------------
1) ubuntu 12.04 64 bit bit OS 

2) install lbaas api sources, clone from git repo

3) sudo apt-get install maven, needed to build

4) install java 7, needed for gearman java
sudo add-apt-repository ppa:webupd8team/java -y
sudo apt-get update
sudo apt-get install oracle-java7-installer

5) add gearman jar to .m2 repo, must be done by hand
mvn install:install-file -DgroupId=gearman -DartifactId=java-gearman-service -Dversion=0.6.5 -Dpackaging=jar -Dfile=gearman/java-gearman-service-0.6.5.jar

6) install mysql ( can be local for testing or on a system of your choice )
sudo apt-get install mysql-server ( root pwd should be 'lbaas' for testing )

7) build it
mvn clean install
mvn assembly:assembly ( should have no errors )

8) initialize DB
mysql/mysql -u root -p < lbaas.sql    ( build the DB, first time only, will drop DBs and create LBaaS schemas )

9) install gearman ( can be local for testing or on a system of your choice )
sudo apt-get install gearman-job-server     <<  should also start gearmand
Note, you may need to change /etc/default/gearman-job-server  to listen on 0.0.0.0 rather than 127.0.0.1 

10) check config params
lbaas.config defines run time params, this is setup for local testing by default

11) test it
curl localhost:8888/devices
{"devices":[]}                     << will return empty device listlook in logs/lbaas.log for run time info.

12) for logging & debug log4j path must vbe correct in lbaa.hs based on your deployment


