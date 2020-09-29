#!sh
javac -d . -g *.java ../j2se/src/com/sun/squawk/util/Find.java ../loader/src/com/sun/squawk/xml/Tag.java ../define/src/com/sun/squawk/vm/*.java
jar cfm ../build.jar MANIFEST.MF *.class com/sun/squawk *.xml
rm *.class
