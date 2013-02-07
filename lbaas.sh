#!/bin/bash
JAR="/opt/lbaasapi/lbaasapi-0.1.6-jar-with-dependencies.jar"
LOGCFG="file:/opt/lbaasapi/log4j.properties"
CONFIG="./lbaas.config"

if [ $# -lt 1 ]
then
	echo "lbaas.sh start | stop" 
        exit 0
fi

if [ $1 = "start" ]
then
	echo "starting lbaas ..."
        RPID=$(ps -ef | grep $JAR | grep java | awk '{print $2}' | head -1)
	if [ -z $RPID ]
	then
		echo "application :" $JAR
		echo "logging cfg :" $LOGCFG
		java -Dlog4j.configuration=$LOGCFG -jar $JAR $CONFIG > /var/log/lbaasapi/launch.log 2>&1 & 
		echo "started"
		exit 0
	else
		echo "lbaasapi already running!"
		exit 0
	fi
fi

if [ $1 = "stop" ]
then
	echo "stopping lbaas ..."
	RPID=$(ps -ef | grep $JAR | grep java | awk '{print $2}' | head -1)
	if [ -z $RPID ]
	then
		echo "lbaasapi does not appear to be running!"
		exit 0
	fi
	echo "stopping pid " $RPID
	kill $RPID
	echo "stopped"
	exit 0
fi

echo "lbaasapi unknown option" $1
exit 0 
