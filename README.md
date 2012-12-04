LBaaS API server environment and build information ( what's needed for development )
------------------------------------------------------------------------------------
1) Get ubuntu OS ,12.04 64 bit bit OS 
(other OSs are possible but these instructions assume Ubuntu & debian packages) 

2) Install lbaas api sources, clone from git repo
git clone https://github.com/LBaaS/lbaas-api.git lbaas-directory 

3) Install maven, needed to building
sudo apt-get install maven

4) Install java 7, needed for gearman java
sudo add-apt-repository ppa:webupd8team/java -y
sudo apt-get update
sudo apt-get install oracle-java7-installer

5) Add gearman jar to maven .m2 repo, must be done by hand. The .jar is included in sources from git repo
mvn install:install-file -DgroupId=gearman -DartifactId=java-gearman-service -Dversion=0.6.5 -Dpackaging=jar -Dfile=gearman/java-gearman-service-0.6.5.jar

5.1) Install HPCS Specific middleware, requires access to internal repository
create a local directory called 'cs'
download CsMiddleware-3.24.0.jar from http://keg.dev.uswest.hpcloud.net:8090/archiva/browse/com.hp.csbu.cc/CsMiddleware/3.24.0 into 'cs'
mvn install:install-file -DgroupId=com.hp.csbu.cc -DartifactId=CsMiddleware -Dversion=3.24.0 -Dpackaging=jar -Dfile=cs/CsMiddleware-3.24.0.jar

5.2) Install HPCS Specific thrift service, requires access to internal repository
copy into 'cs' directory
download CsThriftService:jar:3.9.1 from http://keg.dev.uswest.hpcloud.net:8090/archiva/browse/com.hp.csbu.cc/CsThriftService/3.9.1 into 'cs'
mvn install:install-file -DgroupId=com.hp.csbu.cc -DartifactId=CsThriftService -Dversion=3.9.1 -Dpackaging=jar -Dfile=cs/CsThriftService:jar:3.9.1


6) Install mysql ( for local testing on same system as API server )
sudo apt-get install mysql-server ( root pwd should be 'lbaas' for testing )

7) Build LBaaS API server 
mvn clean install
mvn assembly:assembly ( should have no errors )

8) Initialize DB
mysql/mysql -u root -p < lbaas.sql    ( build the DB, first time only, will drop DBs and create LBaaS schemas )

9) Install gearman job server ( can be local for testing or on a system of your choice )
sudo apt-get install gearman-job-server     <<  should also start gearmand
Note, you may need to change /etc/default/gearman-job-server  to listen on 0.0.0.0 rather than 127.0.0.1 

10) check config params
lbaas.config defines run time params, this is setup for local testing by default, file has comments 

11) Run it
./lbaas.sh is the start and stop script. To have logs locally create a logs directory.
./lbaas.sh start
logs/launch.log will show how it started
logs/lbaas.log will log based on the log4j config

12) Test it
curl https://localhost:8889/v1/devices
{"devices":[]}                     << will return empty device list, look in logs/lbaas.log for run time info.



