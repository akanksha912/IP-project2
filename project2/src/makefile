include makefile.inc

.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java
	mv *.class $(BUILD_DIR)

CLASSES = \
        Header.java \
       # Blah.java \
       # Library.java \
       # Main.java 

default: classes

classes: $(CLASSES:.java=.class)

clean:
	cd $(BUILD_DIR);$(RM) *.class

