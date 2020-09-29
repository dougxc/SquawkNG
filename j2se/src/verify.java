import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Vector;
import java.util.Enumeration;
import java.io.*;

/**
 * This is a harness for running a set of classes through the verifier of
 * the host JVM. The host JVM may need to be invoked with special flags to
 * force running the verifier on classes loaded from the local file system.
 * For example, Sun's Hotspot JVM requires the '-Xverify:all'
 * flag to be specified. In additions, the path for all the classes to be
 * verified must be included in the class path passed to the JVM.
 */
public class verify {

    private static void usage(String errMsg) {
        PrintStream out = System.out;
        if (errMsg != null) {
            out.println(errMsg);
        }
        out.println("usage: verify <options> <classes>");
        out.println();
        out.println("    -v        Verbose operation");
        out.println("    -h        Show this help message and exit");
        out.println();
        out.println("Any <classes> arg starting with '@' is interpreted as the");
        out.println("name of a file containing a list of classes.");
        out.println();
    }

    public static void main(String[] args) {
        int i = 0;
        boolean verbose = false;
        Class c = verify.class;

        while (i != args.length) {
            String arg = args[i];
            if (arg.charAt(0) != '-') {
                break;
            }
            if (arg.equals("-v")) {
                verbose = true;
            } else if (arg.equals("-h")) {
                usage(null);
                return;
            } else {
                usage("Unknown flag: "+arg);
                System.exit(1);
            }
            i++;
        }

        Vector uncompletedThreads = new Vector();
        Vector classes = new Vector();
        while (i != args.length) {
            String arg = args[i];
            if (arg.charAt(0) == '@') {
                String classesFile = arg.substring(1);
                try {
                    BufferedReader br = new BufferedReader(new FileReader(classesFile));
                    String klass;
                    while ((klass = br.readLine()) != null) {
                        klass = klass.trim();
                        if (klass.length() == 0 || klass.charAt(0) == '#') {
                            continue;
                        } else {
                            classes.addElement(klass);
                        }
                    }
                } catch (IOException ex) {
                    System.err.println("IO error while reading '"+classesFile+"': "+ex);
                    System.exit(1);
                }
            } else {
                classes.addElement(arg);
            }
            i++;
        }
        args = new String[classes.size()];
        classes.copyInto(args);


        int passed = 0;
        int failed = 0;

        i = 0;
        while (i != args.length) {
            final String klass = args[i];
            VerificationThread runner = new VerificationThread(klass, verbose);
            runner.start();
            try {
                runner.join(1000);
            } catch (InterruptedException ex) {
            }

            int counter = 10;
            while (runner.isAlive() && counter > 0) {
                if (verbose) {
                    System.out.println("waiting "+counter+" more seconds for verification of "+klass+" to complete");
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                }
                counter--;
            }

            if (runner.isAlive()) {
                uncompletedThreads.addElement(runner);
            } else {
                if (runner.passed) {
                    passed++;
                } else {
                    failed++;
                }
            }

            i++;
        }

        for (Enumeration e = uncompletedThreads.elements(); e.hasMoreElements();) {
            VerificationThread vt = (VerificationThread)e.nextElement();
            if (vt.isAlive()) {
                failed++;
                System.out.println("failed: "+vt.klass);
                if (verbose) {
                    System.out.println("Verification thread incomplete: ");
                }
            } else {
                passed++;
            }
        }

        System.out.println("Total passed: "+passed);
        System.out.println("Total failed: "+failed);

        System.exit(0);
    }

    static class VerificationThread extends Thread {
        final String klass;
        final boolean verbose;
        boolean passed;

        VerificationThread(String klass, boolean verbose) {
            this.klass = klass;
            this.verbose = verbose;
        }
        public void run() {
            if (verbose) {
                System.out.println("verifying: " + klass);
            }
            try {
                Class.forName(klass);
                passed = true;
            } catch (ExceptionInInitializerError e) {
                System.out.println("failed: "+klass);
                stackTrace(e.getException());
            } catch (Error e) {
                System.out.println("failed: "+klass);
                stackTrace(e);
            } catch (ClassNotFoundException cnfe) {
                System.out.println("failed: "+klass);
                stackTrace(cnfe);
            }
            if (passed) {
                System.out.println("passed: "+klass);
            }
        }

        private void stackTrace(Throwable t) {
            if (verbose) {
                t.printStackTrace(System.out);
            }
        }
    }
}

