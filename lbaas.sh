#!/bin/bash
JAR="/opt/lbaasapi/lbaasapi-0.1.16-jar-with-dependencies.jar"
KILLPATTERN="/opt/lbaasapi/lbaasapi-"
LOGCFG="file:/opt/lbaasapi/log4j.properties"
CONFIG="./lbaas.config"
NOW=$(date +"%F-%H%M%S")
LBAASSTDOUT=/var/log/lbaasapi/lbaas-stdout-$NOW.log
LBAASLOG=/var/log/lbaasapi/lbaas.log

if [ $# -lt 1 ]
then
	echo "lbaas.sh start | stop" 
        exit 0
fi

if [ $1 = "start" ]
then
	echo "starting lbaasapi ..."
        RPID=$(ps -ef | grep $KILLPATTERN | grep java | awk '{print $2}' | head -1)
	if [ -z $RPID ]
	then
		echo "application     :" $JAR
		echo "logging cfg     :" $LOGCFG
		echo "lbaasapi stdout :" $LBAASSTDOUT
		echo "lbaasapi log    :" $LBAASLOG
		java -Dlog4j.configuration=$LOGCFG -jar $JAR $CONFIG > $LBAASSTDOUT 2>&1 & 
		echo "started"
		exit 0
	else
		echo "lbaasapi already running! version : " $(ps -ef | grep $KILLPATTERN)
		exit 0
	fi
fi

if [ $1 = "stop" ]
then
	echo "stopping lbaasapi ..."
	RPID=$(ps -ef | grep $KILLPATTERN | grep java | awk '{print $2}' | head -1)
	if [ -z $RPID ]
	then
		echo "lbaasapi does not appear to be running!"
		exit 0
	fi
	echo "stopping pid " $RPID
	kill $RPID
	echo "stopped : "
	exit 0
fi

echo "lbaasapi unknown option" $1
exit 0 
