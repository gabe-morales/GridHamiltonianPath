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
	javac -sourcepath $(SRC) -cp $(CLASSPATH) -d $(BIN) $(CODE)

$(BIN):
	mkdir $@
	
clean:
	rd /s /q $(BIN)