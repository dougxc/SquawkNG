#!sh
javac CodeGen.java
java -cp . CodeGen 1 > ../define/src/com/sun/squawk/vm/OPC.java
java -cp . CodeGen 2 > ../loader/src/com/sun/squawk/suite/TagLookup.java
java -cp . CodeGen 3 > ../define/src/com/sun/squawk/vm/Mnemonics.java
rm *.class
