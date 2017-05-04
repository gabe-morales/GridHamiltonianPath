#!bin/sh
#	AUTHOR:		Gabriel Morales
#	FILE:		driver.sh
#	PURPOSE:	Runs code in compact way

BIN=bin
PACKAGE=v4
PROGRAM=Driver
CLASSPATH=$BIN

java -cp "$CLASSPATH" $PACKAGE.$PROGRAM "$@"