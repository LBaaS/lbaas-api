API server env + build
--------------------------------------------------
1) standard medium  / ubuntu 12.04 64 bit 

2) install lbaas api sources

3) sudo apt-get install maven

4) install java 7 ( needed for gearman java )
sudo add-apt-repository ppa:webupd8team/java -y
sudo apt-get update
sudo apt-get install oracle-java7-installer

5) add gearman jar to .m2 repo, must be done by hand
mvn install:install-file -DgroupId=gearman -DartifactId=java-gearman-service -Dversion=0.6.5 -Dpackaging=jar -Dfile=gearman/java-gearman-service-0.6.5.jar

6) install mysql
sudo apt-get install mysql-server ( root pwd should be 'lbaas' for testing )

7) build it
mvn clean install
mvn assembly:assembly

8) run it
mysql/mysql -u root -p < lbaas.sql    ( build the DB, first time only, will drop DBs and create LBaaS schemas )

9) test it
curl localhost:8888/devices
{"devices":[]}                     << will return empty device listlook in logs/crudservice.log for run time info.
