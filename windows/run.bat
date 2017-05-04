::	AUTHOR:		Gabriel Morales
::	FILE:		driver.cmd
::	PURPOSE:	Runs code in compact way

@echo off
set BIN=bin
set PACKAGE=v4
set PROGRAM=Driver
set CLASSPATH=%BIN%

java -cp "%CLASSPATH%" %PACKAGE%.%PROGRAM% %*