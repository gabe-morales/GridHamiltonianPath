#	AUTHOR:		Gabriel Morales
#	FILE:		Makefile
#	PURPOSE:	Compile project source code

SRC=src
BIN=bin
PACKAGE=v4
MAIN=Driver
CLASSPATH=$(BIN)
CODE=$(SRC)\$(PACKAGE)\$(MAIN).java

all: $(BIN)
	javac -sourcepath $(SRC) -classpath $(CLASSPATH) -d $(BIN) -Xprefer:source $(CODE)

$(BIN):
	mkdir $@
	
clean:
	rd /s /q $(BIN)