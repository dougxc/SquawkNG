Squawk VM source package.

To run:

    1, Have a version of at least Java 1.3 installed
    2, cd run
    3, Execute one of the command lines in the file "example.txt"

To build:

    1, Have a version of at least Java 1.3 installed
    2, If using Windows have MKS Toolkit version 6.2a installed
    3, java -jar build.jar [ -options ] [ target ]
       where options include:

       Native compilation options:

       -cc            build the native VM with 'cc'
       -gcc           build the native VM with 'gcc'
       -msc           build the native VM with 'msc'
       -o1            optimize for space
       -o2            optimize for speed
       -prod          do a production build
       -stats         include VM stats gathering/printing

       General options:

       -t             time the command
       -noshell       don't use a OS shell to execute native commands
       -verbose       verbose mode
       -gui           start the GUI version of the builder
       
      (leave 'target' blank to build everything)
      
To clean:

    1, sh del.sh

To run native Squawk VM on Windows:

    1. Ensure that the PATH environment variable includes the
       directory containing jvm.dll. This will most likely be
       
              $JDK\jre\bin\client
       
    2. Executable is vm/bld/squawk.exe

To run native Squawk VM on Linux/Solaris:

    1. Ensure that the LD_LIBRARY_PATH environment variable includes the
       directory containing libjvm.so as well as the other JVM libraries.
       This will most likely be:
     
              $JDK/jre/lib/$ARCH/client:$JDK/jre/lib/$ARCH

       where ARCH is 'sparc' or '386'.

    2. Executable is vm/bld/squawk

