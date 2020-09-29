chmod +w Interpret.java
cat Interpret.mpp | mpp -t 100000 -f >Interpret.java
chmod -w Interpret.java