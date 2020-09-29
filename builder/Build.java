import java.io.*;
import java.util.*;
import java.net.*;
import java.lang.reflect.*;
import com.sun.squawk.util.Find;

/**
 * This is the launcher for building parts (or all) of the Squawk VM. It is also
 * used to start the translator and the VM once it has been built.
 *
 */
public class Build {


/*---------------------------------------------------------------------------*\
 *                           Java compiler interface                         *
\*---------------------------------------------------------------------------*/

        public int javaCompile(Build env, String args, boolean verbose) throws Exception {
            args = env.fix(args);
            String tempName = "javacinput.tmp";
            delete(tempName);
            PrintStream out = new PrintStream(new FileOutputStream(tempName));
            out.println(args);
            out.close();
            int res = env.exec("javac @"+tempName);
            if (!verbose) {
                delete(tempName);
            }
            return res;
        }

/*---------------------------------------------------------------------------*\
 *                        C compiler interface                               *
\*---------------------------------------------------------------------------*/

    CCompiler ccompiler;


    /**
     * This class is the abstract insterface to the C ompiler and linker to be
     * used for building the native VM.
     */
    public static abstract class CCompiler {

        /**
         * The options that parameterise the compilation and linkage.
         */
        static class Options {
            boolean production = false;
            boolean tracing    = false;
            boolean o1         = false;
            boolean o2         = false;
            boolean stats      = false;
            boolean assume     = false;
            boolean excessiveGC= false;
            boolean macroize   = true;
            String  cflags     = "";
            String  lflags     = "";
        }

        final String  name;
        Options options;

        CCompiler(String name) {
            this.name    = name;
            this.options = options;
        }

        void setOptions(Options options) {
            this.options = options;
        }

        /**
         * Compile a given source file, placing the created object file in current
         * working directory.
         * @param env The env used to execute the C compiler.
         * @param includeDirs
         * @param source The source file name (which must end with ".c").
         * @return the name of the created object file.
         */
        public abstract String compile(Build env, String includeDirs, String source)throws Exception ;

        /**
         * Link a number of given
         * @param env
         * @param libs
         * @param libDirs
         * @param objectFiles
         * @param out The name of the file to be created by the link.
         */
        public abstract void link(Build env, String libs, String libDirs, String objectFiles, String out)throws Exception ;


        public abstract String sourceToObjectFile(String source);
        public abstract String includeSwitch();
        public abstract String libDirSwitch();
        public abstract String libSwitch();

    }

    /**
     * The interface for the "cl" Microsoft Visual C++ compiler.
     */
    public static class MscCompiler extends CCompiler {
        MscCompiler() {
            super("msc");
        }
        public String cflags() {
            StringBuffer buf = new StringBuffer();
            if (options.o1)                 { buf.append("/O1 ");              }
            if (options.o2)                 { buf.append("/O2 /Ox ");          }
            if (options.stats)              { buf.append("/DSTATS ");          }
            if (options.production)         { buf.append("/DPRODUCTION /MD "); }
            if (!options.o1 && !options.o2) { buf.append("/ZI ");              }
            if (options.tracing)            { buf.append("/DTRACING ");        }
            if (options.assume)             { buf.append("/DASSUME ");         }
            if (options.excessiveGC)        { buf.append("/DEXCESSIVEGC ");    }
            if (options.macroize)           { buf.append("/DMACROIZE ");       }
            return buf.append(options.cflags+" ").toString();
        }

        public String lflags() {
            StringBuffer buf = new StringBuffer();
            if (!options.o1 && !options.o2) {
                buf.append("/debug ");
            }
            return buf.append(options.lflags+" ").toString();
        }

        /**
         * Convert a C source file name to its corresponding object file name.
         * @param source
         * @return
         */
        public String sourceToObjectFile(String source) {
            String object = (new File(source)).getName();
            return object.substring(0, object.length() - 2) + ".obj";
        }

        public String compile(Build env, String includeDirs, String source) throws Exception {
            // Convert source file name to object file name
            String object = sourceToObjectFile(source);
            env.exec("cl", "/c /nologo /W3 "+cflags()+" "+includeDirs+" /Fo"+object+" "+source);
            return object;
        }

        public void link(Build env, String libs, String libDirs, String objectFiles, String out) throws Exception {
//            env.exec("link /nologo "+lflags()+" "+libDirs+" "+libs+" /out:"+out+" "+objectFiles);
            env.exec("link", "/nologo "+lflags()+" /out:"+out+" "+objectFiles);
        }

        public String includeSwitch()   { return "/I";           }
        public String libDirSwitch()    { return "/libpath:";    }
        public String libSwitch()       { return "/defaultlib:"; }


    }

    public abstract static class UnixCompiler extends CCompiler {
        UnixCompiler(String name) {
            super(name);
        }
        /**
         * Convert a C source file name to its corresponding object file name.
         * @param source
         * @return
         */
        public String sourceToObjectFile(String source) {
            String object = (new File(source)).getName();
            return object.substring(0, object.length() - 2) + ".o";
        }

        public String lflags() {
            StringBuffer buf = new StringBuffer();
            return buf.append(options.lflags+" ").toString();
        }

        public String includeSwitch()   { return "-I";    }
        public String libDirSwitch()    { return "-L";    }
        public String libSwitch()       { return "-l";    }

    }


    /**
     * The interface for the "gcc" compiler.
     */
    public static class GccCompiler extends UnixCompiler {
        GccCompiler() {
            super("gcc");
        }

        GccCompiler(String name) {
            super(name);
        }

        public String cflags() {
            StringBuffer buf = new StringBuffer();
            if (options.o1)                { buf.append("-O1 ");              }
            if (options.o2)                { buf.append("-O2 ");              }
            if (options.stats)             { buf.append("-DSTATS ");          }
            if (options.production)        { buf.append("-DPRODUCTION ");     }
            if (!options.o1 && !options.o2) { buf.append("-g ");              }
            if (options.tracing)           { buf.append("-DTRACING ");        }
            if (options.assume)            { buf.append("-DASSUME ");         }
            if (options.excessiveGC)       { buf.append("-DEXCESSIVEGC ");    }
            if (options.macroize)          { buf.append("-DMACROIZE ");       }
            return buf.append(options.cflags+" ").toString();
        }

        public String compile(Build env, String includeDirs, String source) throws Exception {
            String object = sourceToObjectFile(source);
            env.exec("gcc", cflags()+" "+includeDirs+" -c -o "+object+" "+source);
            return object;
        }

        public void link(Build env, String libs, String libDirs, String objectFiles, String out) throws Exception {
//            env.exec("gcc", "-o "+out+" "+lflags()+" "+libDirs+" "+libs+" "+objectFiles);
            String nativeThreadLibraryName, dynamicLoadingLibrary;
            if (env.os instanceof SunOS) {
                nativeThreadLibraryName = "thread";
                dynamicLoadingLibrary = "-ldl";
            } else if (env.os instanceof LinuxOS) {
                nativeThreadLibraryName = "pthread";
                dynamicLoadingLibrary = "-ldl";
            } else if (env.os instanceof MacOSX) {
                nativeThreadLibraryName = "pthread";
                dynamicLoadingLibrary = ""; /* no dynamic library loading of JVM on Mac OS X */
            } else {
                throw new RuntimeException("Native thread library name unknown for "+env.os.name());
            }
//            env.exec("gcc", "-l"+nativeThreadLibraryName+" "+dynamicLoadingLibrary+" "+libs+" -o "+out+" "+lflags()+objectFiles);
            env.exec("gcc", "-l"+nativeThreadLibraryName+" "+dynamicLoadingLibrary+" -o "+out+" "+lflags()+objectFiles);
        }
    }


    /**
     * The interface for the "gcc-macosx" compiler.
     */
    public static class GccMacOSXCompiler extends GccCompiler {
        GccMacOSXCompiler() {
            super("gcc-macosx");
        }

        // libSwitch returns a blank string, since it's used in front of the JVM library,
        // and in Mac OS X, the JVM library is a framework, not a regular Unix libary.  So
        // you don't need the -l before the framework name.
        public String libSwitch()       { return "";    }

        public String cflags() {
            StringBuffer buf = new StringBuffer();
            buf.append(super.cflags());
            buf.append("-DMACOSX");
            return buf.toString();
        }

    }


    /**
     * The interface for the "cc" compiler.
     */
    public static class CcCompiler extends UnixCompiler {
        CcCompiler() {
            super("cc");
        }

        public String cflags() {
            StringBuffer buf = new StringBuffer();
            if (options.o1)                { buf.append("-xO5 -xspace ");     }
            if (options.o2)                { buf.append("-xO5 ");             }
            if (options.stats)             { buf.append("-DSTATS ");          }
            if (options.production)        { buf.append("-DPRODUCTION ");     }
            if (!options.o1 && !options.o2) { buf.append("-g ");              }
            if (options.tracing)           { buf.append("-DTRACING ");        }
            if (options.assume)            { buf.append("-DASSUME ");         }
            if (options.excessiveGC)       { buf.append("-DEXCESSIVEGC ");    }
            if (options.macroize)          { buf.append("-DMACROIZE ");       }
            return buf.append(options.cflags+" ").toString();
        }

        public String compile(Build env, String includeDirs, String source) throws Exception {
            String object = sourceToObjectFile(source);
            env.exec("cc", cflags()+" "+includeDirs+" -c -o "+object+" "+source);
            return object;
        }

        public void link(Build env, String libs, String libDirs, String objectFiles, String out) throws Exception {
//            env.exec("cc", "-o "+out+" "+lflags()+" "+libDirs+" "+libs+" "+objectFiles);
            env.exec("cc", "-lthread -ldl -o "+out+" "+lflags()+objectFiles);
        }
    }

/*---------------------------------------------------------------------------*\
 *                        OS classes                                         *
\*---------------------------------------------------------------------------*/

    static final   String  PREVERIFIER_PREFIX = "tools/preverify";

    public static OS createOS(String osName) {
        if (osName.startsWith("windows")) {
            return new Build.WindowsOS();
        } else if (osName.startsWith("sun")) {
            return new Build.SunOS();
        } else if (osName.startsWith("lin")) {
            return new Build.LinuxOS();
        } else if (osName.startsWith("mac os x")) {
            return new Build.MacOSX();
        } else {
            return null;
        }
    }

    public static abstract class OS {

        public abstract String   name();
        public abstract String   executableExtension();
        public abstract String   jniLinkLibrary();
        public abstract String   jniLinkLibraryFile();
        public abstract String   jniRuntimeLibrary();
        public abstract String   batchFileName();
        public abstract String   batchExecPrefix();
        public abstract String   preverifier();
        public abstract String   getJniEnv();
        public abstract void     showJniEnvMsg(PrintStream out);

        public final String      javaExecutable()      {
            String sep = File.separator;
            return System.getProperty("java.home")+sep+"bin"+sep+"java"+executableExtension();
        };


        public final void jvmenv() throws Exception {
            PrintStream out = System.out;
            String javaLib;
            String jhome = System.getProperty("java.home");
            if (this instanceof WindowsOS) {
                String jvm = findOne(jhome, "jvm.dll", new String[] { "hotspot", "client", "" });
                if (jvm != null) {
                    out.println();
                    out.println("To configure the environment for Squawk, try the following command:");
                    out.println();
                    out.println("    set JVMDLL="+jvm);
                    out.println();
                } else {
                    out.println();
                    out.println("The JVMDLL environment variable must be set to the full path of 'jvm.dll'.");
                    out.println();
                }
            } else {
                String jvm      = findOne(jhome, "libjvm.so", new String[] { "hotspot", "client", "" });
                String verifier = findOne(jhome, "libverify.so", "");
                if (jvm != null && verifier != null) {
                    jvm      = (new File(jvm)).getParent();
                    verifier = (new File(verifier)).getParent();
                    out.println();
                    out.println("To configure the environment for Squawk, try the following command under bash:");
                    out.println();
                    out.println("    export LD_LIBRARY_PATH=\"$LD_LIBRARY_PATH:"+jvm+":"+verifier+"\"");
                    out.println();
                    out.println("or in csh/tcsh");
                    out.println();
                    out.println("    setenv LD_LIBRARY_PATH=\"$LD_LIBRARY_PATH:"+jvm+":"+verifier+"\"");
                    out.println();
                } else {
                    out.println();
                    out.println("The LD_LIBRARY_PATH environment variable must be set to include the directories");
                    out.println("containing 'libjvm.so' and 'libverify.so'.");
                    out.println();
                }
            }
        }



    }

    public static class WindowsOS extends OS {
        public String   name()                { return System.getProperty("os.name"); }
        public String   executableExtension() { return ".exe";      }
        public String   jniLinkLibrary()      { return "jvm";   }
        public String   jniLinkLibraryFile()  { return "jvm.lib";   }
        public String   jniRuntimeLibrary()   { return "jvm.dll";   }
        public String   batchFileName()       { return "temp.bat";  }
        public String   batchExecPrefix()     { return "";          }
        public String   preverifier()         { return fix(PREVERIFIER_PREFIX+".exe"); }

        public String getJniEnv() {
            String javaHome = System.getProperty("java.home");
            try {
                String env = findOne(javaHome, "jvm.dll", new String[] {"hotspot", "client", ""});
                if (env != null) {
                    return "JVMDLL=" + env;
                }
            } catch (Exception ex) {
            }
            return null;
        }

        public void showJniEnvMsg(PrintStream out) {
            String env = getJniEnv();
            if (env != null) {
                out.println();
                out.println("To configure the environment for Squawk, try the following command:");
                out.println();
                out.println("    set "+env);
                out.println();
            } else {
                out.println();
                out.println("The JVMDLL environment variable must be set to the full path of 'jvm.dll'.");
                out.println();
            }
        }

    }

    public static abstract class UnixOS extends OS {
        public String   name()                { return System.getProperty("os.name"); }
        public String   executableExtension() { return "";            }
        public String   jniLinkLibrary()      { return "jvm";         }
        public String   jniLinkLibraryFile()  { return "libjvm.so";   }
        public String   jniRuntimeLibrary()   { return "libjvm.so";   }
        public String   batchFileName()       { return "temp.sh";     }
        public String   batchExecPrefix()     { return "sh";          }

        public String getJniEnv() {
            String javaHome = System.getProperty("java.home");
            try {
                String jvm = findOne(javaHome, "libjvm.so", new String[] {"hotspot", "client", ""});
                String verifier = findOne(javaHome, "libverify.so", "");
                if (jvm != null && verifier != null) {
                    jvm = (new File(jvm)).getParent();
                    verifier = (new File(verifier)).getParent();
                    return "LD_LIBRARY_PATH=" + jvm + ":" + verifier;
                }
            } catch (Exception ex) {
            }
            return null;
        }

        public void showJniEnvMsg(PrintStream out) {
            String env = getJniEnv();
            if (env != null) {
                out.println();
                out.println("To configure the environment for Squawk, try the following command under bash:");
                out.println();
                out.println("    export "+env+":$LD_LIBRARY_PATH");
                out.println();
                out.println("or in csh/tcsh");
                out.println();
                out.println("    setenv "+env+":$LD_LIBRARY_PATH");
                out.println();
            } else {
                out.println();
                out.println("The LD_LIBRARY_PATH environment variable must be set to include the directories");
                out.println("containing 'libjvm.so' and 'libverify.so'.");
                out.println();
            }
        }
    }

    public static class LinuxOS extends UnixOS   {
        public String   preverifier()   { return fix(PREVERIFIER_PREFIX+".linux"); }
    }

    public static class MacOSX extends UnixOS {
        public String   jniLinkLibrary()      { return "/System/Library/Frameworks/JavaVM.framework/JavaVM"; }
        public String   jniLinkLibraryFile()  { return "src.jar"; } // not used in Mac OS X; use dummy

        public String   preverifier()   { return fix(PREVERIFIER_PREFIX+".macosx"); }
    }

    public static class SunOS extends UnixOS {
        public String   preverifier()   { return fix(PREVERIFIER_PREFIX+".solaris"); }
    }

    private OS os;

/*---------------------------------------------------------------------------*\
 *                        Command classes                                    *
\*---------------------------------------------------------------------------*/

    /**
     * A Command instance describes a builder command.
     */
    abstract class Command {

        private final String name;
        private final String name2;
        public final String name()  { return name; }
        public final String name2() { return name2; }

        protected Command(String name) {
            this.name = name;
            this.name2 = "";
        }

        protected Command(String name, String name2) {
            this.name  = name;
            this.name2 = name2;
        }

        /**
         * @param cmd
         * @param args
         * @return
         */
        public abstract int run(String[] args) throws Exception;
    }

    abstract class CompilationCommand extends Command {
        CompilationCommand(String name) {
            super(name);
        }
        CompilationCommand(String name, String name2) {
            super(name, name2);
        }
    }

    abstract class CompilationCommandIfPresent extends CompilationCommand {
        CompilationCommandIfPresent(String name) {
            super(name);
        }
    }

    private boolean compilationSupported = true;

    /**
     * The set of commands supported by the builder.
     */
    private Vector commands = new Vector(Arrays.asList(new Command[] {
        new CompilationCommand("clean") {
            public int run(String[] args) throws Exception {
                stdout.println("Cleaning...");
                clean(new File("."), ".class");
                clean(new File("vm/bld"), ".c");
                clean(new File("vm/bld"), ".h");
                clean(new File("vm/bld"), ".exe");
                clean(new File("vm/bld"), ".obj");
                clean(new File("j2se/javacc"), ".java");
                clean(new File("temp/src"), ".java");
                clean(new File("tck/gen"), ".java");
                stdout.println("Finished cleaning.");
                return 0;
            }
        },
        new Command("traceviewer") {
            public int run(String[] args) throws Exception {
                stdout.println("Running traceviewer...");
                String options = join(args, 0, args.length, " ");
                return java("-Xms128M -Xmx128M -cp j2se/classes;j2me/classes;j2se/brazil-1.1-min.jar", "com.sun.squawk.traceviewer.TraceViewer", options);
            }
        },
        new Command("profiler") {
            public int run(String[] args) throws Exception {
                stdout.println("Running profiler...");
                String options = join(args, 0, args.length, " ");
                return java("-Xms128M -Xmx128M -cp j2se/classes;j2me/classes;j2se/brazil-1.1-min.jar", "com.sun.squawk.profiler.Profiler", options);
            }
        },
        new Command("classpathtest") {
            public int run(String[] args) throws Exception {
                stdout.println("Running classpathtest...");
                String options = join(args, 0, args.length, " ");
                return java("-cp j2se/classes;j2me/classes;j2se/brazil-1.1-min.jar", "com.sun.squawk.io.j2me.classpath.Protocol", options);
            }
        },
        new Command("brazil") {
            public int run(String[] args) throws Exception {
                String options = join(args, 0, args.length, "+");

                // Give the translator a default classpath
                if (options.indexOf("-cp") == -1 && options.indexOf("-classpath") == -1) {
                    options = "-cp+"+fix("j2me/classes;translator/classes;samples/classes")+"+"+options;
                }
                if (options.length() > 0) {
                    options = " options "+options;
                }

                stdout.println("Running translator in brazil...");
                return java("-Xbootclasspath#a:translator/classes;j2se/classes;j2me/classes;j2se/brazil-1.1-min.jar", "sunlabs.brazil.server.Main -l 2 -port 9090 -handler com.sun.squawk.translator.BrazilHandler", options);
            }
        },
        new Command("translate") {
            public int run(String[] args) throws Exception {
                stdout.println("Running translator...");
                String options = join(args, 0, args.length, " ");
                return java("-Xbootclasspath#a:j2se/classes;j2me/classes;translator/classes", "com.sun.squawk.translator.main.Main", options);
            }
        },
        new Command("javawand") {
            public int run(String[] args) throws Exception {
                stdout.println("Running JavaWand inliner & finalizer...");
                String options = join(args, 0, args.length, " ");
                return java("-cp tools/javawand.jar", "com.sun.javawand.tools.opt.Optimizer", options);
            }
        },
        new Command("romizetck") {
            public int run(String[] args) throws Exception {
                stdout.println("Translating and romizing TCK suites...");
                TCK tck = new CLDC_TCK1_0a();
                tck.romizetck(args, Build.this);
                return 0;
            }
        },
        new Command("verify") {
            void usage(String errMsg, PrintStream out) {
                if (errMsg != null) {
                    out.println(errMsg);
                }
                out.println("usage: verify [options] [class | @file]*");
                out.println("where options include:");
                out.println("    -cp <path>          the classpath where the classes can be found");
                out.println("    -v                  verbose execution");
                out.println("    -h                  show this help message and exit");
                out.println("");
                out.println("The @file arg specifies a file containing a list of");
                out.println("class names, one per line");
            }
            public int run(String[] args) throws Exception {
                stdout.println("Running translator as verifier...");
                String options = "-v ";
                if (args.length == 0) {
                    usage(null, stdout);
                    return 0;
                }
                for (int i = 0; i != args.length; ++i) {
                    String arg = args[i];
                    if (arg.equals("-cp")) {
                        options += "-cp " + args[++i];
                    } else if (arg.startsWith("-v")) {
                        options += " -traceloading";
                    } else if (arg.startsWith("-h")) {
                        usage(null, stdout);
                        return 0;
                    } else {
                        options += " " + arg;
                    }
                }
                return java("-Xbootclasspath#a:j2se/classes;j2me/classes;translator/classes", "com.sun.squawk.translator.main.Main", options);
            }
        },
        new Command("runtck") {
            public int run(String[] args) {
                stdout.println("Running TCK suites...");
                TCK tck = new CLDC_TCK1_0a();
                tck.runtck(args);
                return 0;
            }
        },
        new Command("dumptck") {
            public int run(String[] args) throws Exception {
                stdout.println("Listing TCK ...");
                String arg = join(args, 0, args.length, " ");
                boolean pos = arg.indexOf("-exclPos") == -1;
                boolean neg = arg.indexOf("-exclNeg") == -1;
                boolean skipped = arg.indexOf("-exclSkipped") == -1;
                boolean raw = arg.indexOf("-raw") != -1;
                TCK tck = new CLDC_TCK1_0a();
                if (!raw) {
                    tck.toXML(System.out, pos, neg, skipped);
                } else {
                    tck.dump(System.out, pos, neg, skipped);
                }
                return 0;
            }
        },
        new Command("testgc") {
            public int run(String[] args) throws Exception {
                stdout.println("Running testgc...");
                return java("-cp vm/classes", "com.sun.squawk.vm.ObjectMemoryTester", "");
            }
        },
        new Command("jvmenv") {
            public int run(String[] args) throws Exception {
                os.showJniEnvMsg(stdout);
                return 0;
            }
        },
        new Command("squawk") {
            public int run(String[] args) throws Exception {
                String options = join(args, 0, args.length, " ");
                String cp = "j2se/classes;vm/classes;j2me/classes";
                if (exists("vm/gcspy.jar")) {
                    cp += ";vm/gcspy.jar";
                }
                return java("-Xbootclasspath#a:"+cp, "com.sun.squawk.vm.Interpreter", options);
            }
        },
        new Command("squawkc") {
            public int run(String[] args) throws Exception {
                String options = join(args, 0, args.length, " ");
                String[] envp = null;
                String env = os.getJniEnv();
                if (env != null) {
                    envp = new String[] { env };
                }
                return exec(fix("vm/bld/squawk"+os.executableExtension()+" -Xcp:vm/classes;j2se/classes;j2me/classes"), options, envp);
            }
        },
        new Command("romize") {
            public int run(String[] args) throws Exception {
                stdout.println("Running romizer...");
                String options = join(args, 0, args.length, " ");
                return java("-Xbootclasspath#a:j2se/classes;romizer/classes;vm/classes;j2me/classes;translator/classes", "java.lang.Romizer", options);
            }
        },
        new Command("gcspy") {
            public int run(String[] args) throws Exception {
                if (exists("vm/gcspy.jar")) {
                    stdout.println("Running gcspy client ...");
                    String options = join(args, 0, args.length, " ");
                    return java("-cp vm/gcspy.jar", "gcspy.Main", options);
                } else {
                    stderr.println("Cannot run gcspy client as "+fix("vm/gcspy.jar")+" doesn't exist");
                    return 1;
                }
            }
        },
        new Command("jpp") {
            public int run(String[] args) throws Exception {
                stdout.println("Preprocessing files...");
                if (args.length == 0) {
                    stderr.println("jpp requires at least input source directory argument");
                    return 1;
                }
                String outDir = args.length > 1 ? args[1] : null;
                preprocessSource(find(args[0], ".java"), clearJppLines, outDir);
                return 0;
            }
        },
        new CompilationCommand("j2me") {
            public int run(String[] args) throws Exception {
                stdout.println("Building j2me...");
                return javac_j2me(null, "j2me", defines + find("j2me/src", ".java") + find("loader/src", ".java"));
            }
        },
        new CompilationCommandIfPresent("jcard") {
            public int run(String[] args) throws Exception {
                stdout.println("Building jcard...");
                return javac_j2me(null, "jcard", find("jcard/src", ".java"));
            }
        },
        new CompilationCommandIfPresent("jcard3") {
            public int run(String[] args) throws Exception {
                stdout.println("Building jcard3...");
                return javac_j2me(null, "jcard3", defines + find("jcard3/src", ".java")  + find("loader/src", ".java"));
            }
        },
        new CompilationCommandIfPresent("graphics") {
            public int run(String[] args) throws Exception {
                stdout.println("Building graphics...");
                return javac_j2me("j2me/classes;", "graphics", find("graphics/src", ".java"));
            }
        },
        new CompilationCommandIfPresent("samples") {
            public int run(String[] args) throws Exception {
                stdout.println("Building samples...");
                return javac_j2me("j2me/classes;graphics/classes;", "samples", find("samples/src", ".java"));
            }
        },
        new CompilationCommandIfPresent("compiler") {
            public int run(String[] args) throws Exception {
                stdout.println("Building compiler...");
                return javac_j2me("j2me/classes", "compiler", find("compiler/src", ".java"));
            }
        },
        new CompilationCommandIfPresent("mojo") {
            public int run(String[] args) throws Exception {
                stdout.println("Building mojo...");
                return javac_j2me("j2me/classes;", "mojo", find("mojo/src", ".java"));
            }
        },
        new CompilationCommand("translator") {
            public int run(String[] args) throws Exception {
                stdout.println("Building translator...");
                return javac_j2me("j2me/classes;", "translator", find("translator/src", ".java"));
            }
        },
        new CompilationCommand("j2se") {
            public int run(String[] args) throws Exception {
                stdout.println("Building j2se...");
                return javac_j2se("j2me/classes;translator/classes;j2se/Regex.jar;j2se/bcel-5.0.jar;j2se/brazil-1.1-min.jar", "j2se", find("j2se/src", ".java"));
            }
        },
        new CompilationCommand("gol") {
            public int run(String[] args) throws Exception {
                stdout.println(fix("Building vm/src/com/sun/squawk/vm/GeneratedObjectLayout.java..."));
                return java("-Xbootclasspath#a:j2se/classes;j2me/classes;translator/classes", "com.sun.squawk.translator.main.Main",
                            "-cp j2me/classes -o vm/src/com/sun/squawk/vm/GeneratedObjectLayout.java -format olg @j2me");
            }
        },
        new CompilationCommandIfPresent("checkpoint") {
            public int run(String[] args) throws Exception {
                try {
                    Class.forName("sun.misc.Unsafe");
                    stdout.println("Building checkpoint...");
                    return javac_j2se(null, "checkpoint", find("checkpoint/src", ".java"));
                } catch (ClassNotFoundException cnfe) {
                    stdout.println("Cannot build checkpoint - requires sun.misc.Unsafe class ...");
                    return 0;
                }
            }
        },
        new CompilationCommand("vm") {
            private String compile(String includeDirs, String source) throws Exception {
                stdout.println("Compiling "+source+" ...");
                return ccompiler.compile(Build.this, includeDirs, source);
            }
            private void link(String libs, String libDirs, String objectFiles, String out) throws Exception {
                stdout.println("Linking "+out+" ...");
                ccompiler.link(Build.this, libs, libDirs, objectFiles, out);
            }
            public int run(String[] args) throws Exception {
                stdout.println("Building vm...");
                clean(new File("vm/bld"), "");
                String cp = "j2se/classes;j2me/classes;";
                if (exists("vm/gcspy.jar")) {
                    cp += "vm/gcspy.jar;";
                }
                javac_j2se(cp, "vm", defines + find("vm/src", ".java"));

                j2c(defines + find("vm/src", ".java"), "vm/bld");

                if (ccompiler != null) {
                    String javaHome = System.getProperty("java.home");
//stdout.println("java.library.path="+System.getProperty("java.library.path"));
//stdout.println("java.home        ="+System.getProperty("java.home"));

                    if (javaHome.endsWith("jre")) {
                        javaHome = javaHome.substring(0, javaHome.length()-4);
                    }

                    String jniIncludeDir = (new File(findOne(fix(javaHome+"/include"), "jni.h", (String)null))).getParent();
                    String jniLib        = os.jniLinkLibrary();
                    String jniLibFile    = findOne(javaHome, os.jniLinkLibraryFile(), "client");


                    if (jniLibFile == null || jniLibFile.length() == 0) {
                        stderr.println("Can't find a '.../client' directory containing "+jniLib);
                        return 1;
                    }

                    String includeDirs = ccompiler.includeSwitch()+jniIncludeDir;
                    includeDirs += " "+ccompiler.includeSwitch()+fix("../rts/"+ccompiler.name);

                    cd("vm/bld");
                    String objectFiles = compile(includeDirs, fix("../rts/"+ccompiler.name+"/os.c")) + " " +
                                         compile(includeDirs, fix("squawk.c"));

                    link(ccompiler.libSwitch()+os.jniLinkLibrary(),
                         ccompiler.libDirSwitch()+(new File(jniLibFile)).getParent(),
                         objectFiles,
                         "squawk"+os.executableExtension());
                    cd("../..");

                }
                return 0;
            }
        },
        new CompilationCommand("romizer") {
            public int run(String[] args) throws Exception {
                stdout.println("Building romizer...");
                String classes = find("romizer/src", ".java") +
                                 " loader/src/java/lang/StringOfSymbols.java" +
                                 " j2me/src/java/io/PersistentMemoryOutputStream.java" +
                                 " j2me/src/java/lang/Native.java";
                setRomizerProperties(true);
                int res = javac_j2se("j2se/classes;j2me/classes;vm/classes;translator/classes", "romizer", classes);
                setRomizerProperties(false);
                return res;

            }
        },
        new CompilationCommandIfPresent("tck") {
            public int run(String[] args) throws Exception {
                stdout.println("Building tck...");
                return javac_j2me("j2me/classes;tck/tck.jar", "tck", find("tck/src", ".java"));
            }
        },
        new CompilationCommand("demo", "vm") {
            public int run(String[] args) throws Exception {
                stdout.println("Building demo...");


                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("MANIFEST.MF")));
                PrintWriter out = new PrintWriter(bw);
                out.println("Main-Class: com.sun.squawk.vm.Interpreter");
                out.close();

                exec("jar cfm demo/squawk.jar MANIFEST.MF");

                bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("jarlist")));
                out = new PrintWriter(bw);
                out.println(
                                  filter(fix("j2me/classes"), find(fix("j2me/classes"), ".class")) +
                                  filter(fix("j2se/classes"), find(fix("j2se/classes"), ".class")) +
                                  filter(fix("vm/classes"), find(fix("vm/classes"), ".class")) +
                                  find(fix("samples/src/example/chess"), ".gif")+
                                  find(fix("samples/src/example/mpeg"), ".mpg")
                );
                out.close();

                exec(fix("jar uf demo/squawk.jar @jarlist"));
                //exec(fix("cp vm/bld/squawk"+os.executableExtension()+" demo"));
                cp("vm/bld/squawk"+os.executableExtension(), "demo/squawk"+os.executableExtension());

                if (!verbose) {
                    delete("MANIFEST.MF");
                    delete("jarlist");
                }
                return 0;
            }

            String fixFilterEntry(String entry) {
                entry = entry.replace('.', File.separatorChar).concat(".class");
                return entry;
            }

            String filter(String dir, String results) {
                StringTokenizer st = new StringTokenizer(results);
                StringBuffer buf = new StringBuffer(1000);
                while (st.hasMoreTokens()) {
                    String token = st.nextToken().substring(dir.length()+1);
                    if (filter.contains(token)) {
                        buf.append(" -C ").append(dir).append(' ').append(token);
                    }
                }
                return buf.append(' ').toString();
            }

            Set filter = new HashSet(Arrays.asList(new String[] {
                fixFilterEntry("jvmenv"),
                fixFilterEntry("jvmenv$1"),
                fixFilterEntry("com.sun.squawk.util.Find"),
                fixFilterEntry("com.sun.squawk.io.ConnectionBase"),
                fixFilterEntry("com.sun.squawk.io.j2se.UniversalFilterInputStream"),
                fixFilterEntry("com.sun.squawk.io.j2se.UniversalFilterOutputStream"),
                fixFilterEntry("com.sun.squawk.io.j2se.zip.Protocol"),
                fixFilterEntry("com.sun.squawk.io.j2se.file.Protocol"),
                fixFilterEntry("com.sun.squawk.util.IntHashtable$Entry"),
                fixFilterEntry("com.sun.squawk.util.IntHashtable"),
                fixFilterEntry("com.sun.squawk.vm.BytecodeMnemonics"),
                fixFilterEntry("com.sun.squawk.vm.Bytecodes"),
                fixFilterEntry("com.sun.squawk.vm.C3Panel"),
                fixFilterEntry("com.sun.squawk.vm.Channel"),
                fixFilterEntry("com.sun.squawk.vm.Channel1"),
                fixFilterEntry("com.sun.squawk.vm.Channel2"),
                fixFilterEntry("com.sun.squawk.vm.Channel2$1"),
                fixFilterEntry("com.sun.squawk.vm.Channel2$2"),
                fixFilterEntry("com.sun.squawk.vm.Channel2codes"),
                fixFilterEntry("com.sun.squawk.vm.Channel3"),
                fixFilterEntry("com.sun.squawk.vm.Channel3$1"),
                fixFilterEntry("com.sun.squawk.vm.Channel3codes"),
                fixFilterEntry("com.sun.squawk.vm.ChannelIO"),
                fixFilterEntry("com.sun.squawk.vm.ClassNumbers"),
                fixFilterEntry("com.sun.squawk.vm.CheneyCollector"),
                fixFilterEntry("com.sun.squawk.vm.CurrentSegment"),
                fixFilterEntry("com.sun.squawk.vm.ObjectAssociations"),
                fixFilterEntry("com.sun.squawk.vm.SegmentAccess"),
                fixFilterEntry("com.sun.squawk.vm.HeaderLayout"),
                fixFilterEntry("com.sun.squawk.vm.HeapTracing"),
                fixFilterEntry("com.sun.squawk.vm.Interpret"),
                fixFilterEntry("com.sun.squawk.vm.Interpreter"),
                fixFilterEntry("com.sun.squawk.vm.InterpreterOptions"),
                fixFilterEntry("com.sun.squawk.vm.InterpreterOptions$1"),
                fixFilterEntry("com.sun.squawk.vm.Lisp2Collector"),
                fixFilterEntry("com.sun.squawk.vm.Memory"),
                fixFilterEntry("com.sun.squawk.vm.MemoryAllocator"),
                fixFilterEntry("com.sun.squawk.vm.NativeBuffers"),
                fixFilterEntry("com.sun.squawk.vm.StringAccess"),
                fixFilterEntry("com.sun.squawk.vm.GeneratedObjectLayout"),
                fixFilterEntry("com.sun.squawk.vm.ObjectLayout"),
                fixFilterEntry("com.sun.squawk.vm.ObjectMemory"),
                fixFilterEntry("com.sun.squawk.vm.PersistentMemory"),
                fixFilterEntry("com.sun.squawk.vm.PersistentCollector"),
                fixFilterEntry("com.sun.squawk.vm.PlatformAbstraction"),
                fixFilterEntry("com.sun.squawk.vm.SegmentRelocation"),
                fixFilterEntry("com.sun.squawk.vm.Segments"),
                fixFilterEntry("com.sun.squawk.vm.SquawkConstants"),
                fixFilterEntry("com.sun.squawk.vm.WriteBarrier"),
                fixFilterEntry("com.sun.squawk.vm.BitVector"),
                fixFilterEntry("com.sun.squawk.vm.MarkStack"),
                fixFilterEntry("javax.microedition.io.ConnectionNotFoundException"),
                fixFilterEntry("javax.microedition.io.Connection"),
                fixFilterEntry("javax.microedition.io.Connector"),
                fixFilterEntry("javax.microedition.io.InputConnection"),
                fixFilterEntry("javax.microedition.io.OutputConnection"),
                fixFilterEntry("javax.microedition.io.StreamConnection")
            }));
        }
    }));


    /**
     * Copy a file.
     * @param from
     * @param to
     * @throws IOException
     */
    void cp(String from, String to) throws IOException {
        File fromFile = new File(from);
        InputStream  is = new BufferedInputStream(new FileInputStream(from));
        OutputStream os = new BufferedOutputStream(new FileOutputStream(to));

        int ch = is.read();
        while (ch != -1) {
            os.write(ch);
            ch = is.read();
        }
        is.close();
        os.close();
    }

/*---------------------------------------------------------------------------*\
 *                        Constants                                          *
\*---------------------------------------------------------------------------*/


    private final static boolean NESTEDSECTIONS     = true;
    private final static Integer ONE                = new Integer(1);



    final String defines;

    boolean ignoreErrors = false;
    boolean verbose  = false;
    boolean message  = false;
    boolean useTimer = false;
    boolean clearJppLines = false;
    String  buildPropsFile = "build.properties";

/*---------------------------------------------------------------------------*\
 *             Standard output streams                                       *
\*---------------------------------------------------------------------------*/

    private PrintStream stdout;
    private PrintStream stderr;

    public PrintStream setOut(PrintStream ps) {
        stdout = ps;
        ps = System.out;
        System.setOut(stdout);
        return ps;
    }

    public PrintStream setErr(PrintStream ps) {
        stderr = ps;
        ps = System.err;
        System.setErr(stderr);
        return ps;
    }

    public PrintStream stdout() { return stdout; }
    public PrintStream stderr() { return stderr; }


/*---------------------------------------------------------------------------*\
 *                      General file utilities                               *
\*---------------------------------------------------------------------------*/

    /**
     * Ensures a specified directory exists, creating it if necessary.
     *
     * @param  path  the path of the directory to test
     */
    public static void ensureDirExists(String path) {
        File directory = new File(fix(path));
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new CommandFailedException("Could not create directory: "+path, 1);
            }
        } else {
            if (!directory.isDirectory()) {
                throw new CommandFailedException("Path is not a directory: " + path, 1);
            }
        }
    }

    /**
     * Find all files in a given directory.
     * @param dir The directory to search from.
     * @param suffix The suffix used to filter the results or null for no filtering.
     * @return The results in string separated by spaces.
     * @throws Exception
     */
    public static String find(String dir, String suffix) throws Exception {
        StringBuffer sb = new StringBuffer();
        Vector vec = new Vector();
        com.sun.squawk.util.Find.find(new File(dir), suffix, vec);

        for (Enumeration e = vec.elements(); e.hasMoreElements();){
            String f = (String)e.nextElement();
            sb.append(" ");
            sb.append(f);

        }
//stdout.println("find "+dir+":"+suffix+" = "+   sb.toString());
        return sb.toString();
    }

    /**
     * Find a single file matching a specified suffix and an optional
     * extra filter.
     * @param dir The directory
     * @param suffix
     * @param match
     * @return
     * @throws Exception
     */
    public static String findOne(String dir, String suffix, String match) throws Exception {
        String results = find(dir, suffix).trim();

//System.out.println("dir="+dir);
//System.out.println("suffix="+suffix);
//System.out.println("match="+match);
//System.out.println("results="+results);

        StringTokenizer st = new StringTokenizer(results);
        String result = st.nextToken();
        while (st.hasMoreTokens() && match != null && result.indexOf(match) == - 1) {
            result = st.nextToken();
        }
        return result;
    }

    public static String findOne(String dir, String suffix, String[] matches) throws Exception {
        String result = null;
        for (int i = 0; i != matches.length && result == null; i++) {
            result = findOne(dir, suffix, matches[i]);
        }
        return result;
    }

    public static boolean exists(String name) {
        return (new File(fix(name))).exists();
    }

    /**
     * Delete a file.
     * @param name
     * @return
     */
    public static boolean delete(String name) {
        return delete(new File(name));
    }

    /**
     * Delete a file.
     * @param name
     * @return
     */
    public static boolean delete(File file) {
        return file.delete();
    }

    /**
     * Delete all files under a specified directory that match a specified suffix.
     * @param dir
     * @param suffix
     * @return
     */
    public void clean(File dir, String suffix) throws Exception {
        String[] list = dir.list();
        File[] files = dir.listFiles();
        if (files != null) {
            for(int i = 0 ; i < files.length ; i++) {
                File f = files[i];
                if (f.isDirectory()) {
                    clean(f, suffix);
                } else {
                    if (f.getName().endsWith(suffix)) {
                        delete(f);
                    }
                }
            }
        }
    }

/*---------------------------------------------------------------------------*\
 *                      Command line interface                               *
\*---------------------------------------------------------------------------*/

    public static Vector parseLines(String file, Vector v) throws IOException {
        FileReader fr = null;
        if (v == null) {
            v = new Vector();
        }
        try {
            BufferedReader br = new BufferedReader(fr = new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                v.addElement(line);
            }
        } finally {
            if (fr != null) {
                fr.close();
            }
        }
        return v;
    }

    /**
     * Parse a file containing command lines and add the corresponding
     * invocations to a given vector.
     * @param cmdFile
     * @param invocations
     */
    public static void parseCommandFile(String cmdFile, Vector invocations) {
        try {
            if (!cmdFile.endsWith(".bld")) {
                cmdFile = "bld/"+cmdFile+".bld";
            }
            Vector lines = parseLines(cmdFile, null);
            for (Enumeration e = lines.elements(); e.hasMoreElements();) {
                String invocation = (String)e.nextElement();
                invocation = invocation.trim();
                if (invocation.length() == 0 || invocation.charAt(0) == '#') {
                    continue;
                }

                // If this is another command file, include its commands 'in situ'
                if (invocation.charAt(0) == '@') {
                    parseCommandFile(invocation.substring(1), invocations);
                } else {
                    StringTokenizer st = new StringTokenizer(invocation);
                    String[] args = new String[st.countTokens()];
                    for (int i = 0; i != args.length; i++) {
                        args[i] = st.nextToken();
                    }
                    invocations.addElement(args);
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return;
        }

    }

    /**
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        Vector invocations = new Vector();
        Build instance     = new Build(System.out, System.err);

        args = instance.parseArgs(args);
        instance.stdout.println("os="+instance.os.name());
        instance.stdout.println("java.home="+System.getProperty("java.home"));
        instance.stdout.println("java.vm.version="+System.getProperty("java.vm.version"));
        instance.stdout.println("C compiler="+instance.ccompiler.name);
        if (instance.verbose) {
            instance.stdout.println("Builder properties:");
            Enumeration keys = instance.props.keys();
            Enumeration values = instance.props.elements();
            while (keys.hasMoreElements()) {
                instance.stdout.println("    "+keys.nextElement()+'='+values.nextElement());
            }
        }

        if (args != null) {
            String cmd = args[0];
            if (cmd.charAt(0) == '@') {
                if (args.length != 1) {
                    System.err.println("Warning: args after '"+cmd+"' ignored.");
                }
                parseCommandFile(cmd.substring(1), invocations);
            } else {
                invocations.addElement(args);
            }
        }

        int exitCode = 0;
        for (Enumeration e = invocations.elements(); e.hasMoreElements();) {
            args = (String[])e.nextElement();

            // If the invocation is from a command file, it may specify its
            // own builder options which override the current ones.
            if (args[0].charAt(0) == '-') {
                args = instance.parseArgs(args);
            }
            try {
                int result = instance.run(args);
                if (result != 0 && !instance.ignoreErrors) {
                    System.exit(result);
                } else {
                    exitCode += result;
                }
            } catch (CommandFailedException ex) {
                System.err.println(ex);
                if (!instance.ignoreErrors) {
                    System.exit(ex.exitVal);
                } else {
                    exitCode += ex.exitVal;
                }
            }
        }
        System.exit(exitCode);
    }

    /**
     * A wrapper to print a usage message line conditional upon whether or not
     * it relates to compilation.
     * @param isCompilationMsg
     * @param out
     * @param line
     */
    private void usageln(boolean isCompilationMsg, PrintStream out, String line) {
        if (!isCompilationMsg || compilationSupported) {
            out.println(line);
        }
    }

    /**
     * Print the usage message.
     * @param errMsg An optional error message.
     */
    void usage (String errMsg, boolean exit) {

        PrintStream out = stderr;

        if (errMsg != null) {
            out.println(errMsg);
        }
        usageln(false, out, "Usage: build [ build-options] [ command [ command_options ] | @batch ]");
        usageln(false, out, "where build-options include:");
        usageln(false, out, "    -stdout <file>      redirect stdout to <file>");
        usageln(false, out, "    -stderr <file>      redirect stderr to <file>");
        usageln(false, out, "    -os <name>          the operating system for native executables");
        usageln(false, out, "                        Supported: 'windows', 'linux' or 'sunos' or 'macosx'");
        usageln(true,  out, "    -comp <name>        the C compiler used to build native executables");
        usageln(true, out,  "                        Supported: 'msc', 'gcc' or 'cc' or 'gcc-macosx'");
        usageln(false, out, "    -jmem:<mem>         java memory option shortcut for '-java:-Xmx<mem>'");
        usageln(false, out, "    -java:<opts>        extra java options (e.g. '-java:-Xms128M')");
        usageln(true, out,  "    -javac:<opts>       extra javac options (e.g. '-javac:-g:none')");
        usageln(true, out,  "    -bco[:<opts>]       BCO [ with options ] (default = '-01' with OBF)");
        usageln(true, out,  "    -nobco <file>       list of classes not to be BCO'ed");
        usageln(true, out,  "    -cflags:<opts>      extra C compiler options (e.g. '-cflags:-g')");
        usageln(true, out,  "    -lflags:<opts>      extra C linker options (e.g. '-lflags:/DLL')");
        usageln(true, out,  "    -o1                 optimize C compilation/linking for minimal size");
        usageln(true, out,  "    -o2                 optimize C compilation/linking for speed");
        usageln(true, out,  "    -stats              include support for stats gathering/dumping in the VM");
        usageln(true, out,  "    -tracing            include support for tracing in the VM");
        usageln(true, out,  "    -assume             include support for assertions in the VM");
        usageln(true, out,  "    -excessiveGC        include support for excessive GC in the VM");
        usageln(true, out,  "    -prod               build the production version of the VM");
        usageln(false, out, "    -t                  time the execution of the command(s) executed");
        usageln(true, out,  "    -cl                 clear lines excluded by Java preprocessor (instead");
        usageln(true, out,  "                        of commenting them out)");
        usageln(false, out, "    -k                  keep executing subsequent commands in a batch file");
        usageln(false, out, "                        even if a single command fails (c.f. '-k' in gnumake)");
        usageln(false, out, "    -verbose            verbose execution");
        usageln(false, out, "    -message            print message when running javac/preverifier/BCO");
        usageln(true, out,  "    -props <file>       add to/override default properties from <file>");
        usageln(true, out,  "    -include <prop>     set 'prop' to true for java source code preprocessor");
        usageln(true, out,  "    -exclude <prop>     set 'prop' to false for java source code preprocessor");
        usageln(false, out, "    -help               show this usage message and exit");
        usageln(false, out, "");
        usageln(true, out, "The '-tracing' '-stats', '-assume' and '-excessiveGC' options are");
        usageln(true, out, "implicitly enabled if the '-prod' option is not specified.");
        usageln(true, out, "");
        usageln(false, out, "The supported commands are (compilation commands are marked with '*'):");
        usageln(true, out, "If no command is given then all compilation commands are run sequentially.");
        for (Enumeration e = commands.elements(); e.hasMoreElements();) {
            Command c = (Command)e.nextElement();
            usageln(false, out, "    "+c.name()+(c instanceof CompilationCommand ? " *" : ""));
        }

        if (exit) {
            System.exit(1);
        }
    }

    /**
     * Get the argument to a command line option. If the argument is not provided,
     * then a usage message is printed and the system exits.
     * @param args The command line arguments.
     * @param index The index at which the option's argument is located
     * @param opt The name of the option.
     * @return the options argument.
     */
    public String getOptArg(String[] args, int index, String opt) {
        if (index >= args.length) {
            usage("The " + opt + " option requires an argument.", true);
        }
        return args[index];
    }

    private void loadBuildProperties(String file) throws IOException {
        props.load(new FileInputStream(file));
        setRomizerProperties(false);
        buildPropsFile = file;
    }

    private void setRomizerProperties(boolean b) {
        if (b) {
            props.setProperty("ROMIZER",    "true");
            props.setProperty("NOTROMIZER", "false");
        } else {
            props.setProperty("ROMIZER",    "false");
            props.setProperty("NOTROMIZER", "true");
        }
    }

    /**
     * Create an instance of the builder to run a single command.
     * @param args
     * @param standardOut
     * @param standardErr
     * @throws Exception
     */
    public Build(PrintStream standardOut, PrintStream standardErr) throws Exception {

        this.defines    = find("define/src", ".java");
        this.cwd        = new File(System.getProperty("user.dir"));

        setOut(standardOut);
        setErr(standardErr);

        try {
            loadBuildProperties(buildPropsFile);
        } catch (IOException ex) {
            // If there's no default build.properties file, then no compilation
            // can be done
            Vector newCommands = new Vector(commands.size());
            for (Enumeration e = commands.elements(); e.hasMoreElements(); ) {
                Object c = e.nextElement();
                if (!(c instanceof CompilationCommand)) {
                    newCommands.addElement(c);
                }
                commands = newCommands;
            }
            compilationSupported = false;
        }

    }

    /**
     * Parse the command line args passed to the builder.
     * @param args
     * @return the tail of the given 'args' that gives the command and
     * its arguments.
     * @throws Exception
     */
    public String[] parseArgs(String[] args) throws Exception {
        int argp        = 0;
        String osName   = System.getProperty("os.name" ).toLowerCase();
        CCompiler.Options cOptions = new CCompiler.Options();
        String bcoOptions = null;

        while (args.length > argp) {
            String arg = args[argp];
            if (arg.charAt(0) != '-') {
                break;
            }
            if (arg.equals("-stdout")) {
                this.stdout = new PrintStream(new FileOutputStream(getOptArg(args, ++argp, "-stdout")));
                System.setOut(stdout);
            } else if (arg.equals("-stderr")) {
                this.stderr = new PrintStream(new FileOutputStream(getOptArg(args, ++argp, "-stderr")));
                System.setErr(stderr);
            } else if (arg.equals("-os")) {
                osName = getOptArg(args, ++argp, "-os").toLowerCase();
            } else if (arg.equals("-comp")) {
                String compName = getOptArg(args, ++argp, "-comp").toLowerCase();
                if (compName.equals("msc")) {
                    ccompiler = new MscCompiler();
                } else if (compName.equals("gcc")) {
                    ccompiler = new GccCompiler();
                } else if (compName.equals("gcc-macox")) {
                    ccompiler = new GccMacOSXCompiler();
                } else if (compName.equals("cc")) {
                    ccompiler = new CcCompiler();
                } else {
                    usage("Non-supported C compiler: "+compName, true);
                }

            } else if (arg.startsWith("-cflags:")) {
                cOptions.cflags += " " + arg.substring("-cflags:".length());
            } else if (arg.startsWith("-lflags:")) {
                cOptions.lflags += " " + arg.substring("-lflags:".length());
            } else if (arg.startsWith("-jmem:")) {
                String mem = arg.substring("-jmem:".length());
                javaOptions += " -Xms" + mem + " -Xmx" + mem;
            } else if (arg.startsWith("-java:")) {
                javaOptions += " " + arg.substring("-java:".length());
            } else if (arg.startsWith("-javac:")) {
                javacOptions += " " + arg.substring("-javac:".length());
            } else if (arg.startsWith("-bco")) {
                if (arg.startsWith("-bco:") && arg.length() > "-bco:".length()) {
                    if (bcoOptions == null) {
                        bcoOptions = "";
                    } else {
                        bcoOptions += " ";
                    }
                    bcoOptions += arg.substring("-bco:".length());
                } else {
                    bcoOptions = "-O:OBF:INL:CF:CNPfast:COPfast";
                }
            } else if (arg.equals("-nobco")) {
                nobcoFile = getOptArg(args, ++argp, "-nobco");
            } else if (arg.equals("-o1")) {
                cOptions.o1 = true;
            } else if (arg.equals("-o2")) {
                cOptions.o2 = true;
            } else if (arg.equals("-stats")) {
                cOptions.stats = true;
            } else if (arg.equals("-nomacroize")) {
                cOptions.macroize = false;
            } else if (arg.startsWith("-prod")) {
                cOptions.production = true;
            } else if (arg.equals("-tracing")) {
                cOptions.tracing = true;
            } else if (arg.equals("-assume")) {
                cOptions.assume = true;
            } else if (arg.equals("-excessiveGC")) {
                cOptions.excessiveGC = true;
            } else if (arg.equals("-t")) {
                useTimer = true;
            } else if (arg.equals("-cl")) {
                clearJppLines = true;
            } else if (arg.equals("-k")) {
                ignoreErrors = true;
            } else if (arg.equals("-verbose")) {
                verbose = true;
            } else if (arg.equals("-message")) {
                message = true;
            } else if (arg.equals("-props")) {
                String name = getOptArg(args, ++argp, "-props");
                loadBuildProperties(name);
            } else if (arg.equals("-exclude")) {
                String name = getOptArg(args, ++argp, "-exclude");
                props.put(name, "false");
            } else if (arg.equals("-include")) {
                String name = getOptArg(args, ++argp, "-include");
                props.put(name, "true");
            } else if (arg.equals("-help")) {
                usage(null, true);
            } else {
                usage("Unknown option "+arg, true);
            }
            argp++;
        }

        if (bcoOptions != null) {
            this.bcoOptions = bcoOptions;
        }

        // The -stats, -tracing, -excessiveGC and -assume options are turned on by default
        // if -production was not specified
        if (!cOptions.production) {
            cOptions.tracing     = true;
            cOptions.stats       = true;
            cOptions.assume      = true;
            cOptions.excessiveGC = true;
        }


        // Configure OS
        os = createOS(osName);
        if (os == null) {
            usage("Non-supported OS: "+osName, true);
        }

        // Choose a default compiler
        if (ccompiler == null) {
            if (os instanceof WindowsOS) {
                ccompiler = new MscCompiler();
            } else if (os instanceof LinuxOS) {
                ccompiler = new GccCompiler();
            } else if (os instanceof MacOSX) {
                ccompiler = new GccMacOSXCompiler();
            } else if (os instanceof SunOS) {
                ccompiler = new CcCompiler();
            }
        }
        ccompiler.setOptions(cOptions);

        if (argp == args.length) {
            return new String[] { "all" };
        } else {
            String[] cmdAndArgs = new String[args.length - argp];
            System.arraycopy(args, argp, cmdAndArgs, 0, cmdAndArgs.length);
            return cmdAndArgs;
        }
    }

    public int run(String[] cmdAndArgs) throws Exception {
        String cmd = cmdAndArgs[0];
        String[] cmdArgs;
        if (cmdAndArgs.length > 1) {
            cmdArgs = new String[cmdAndArgs.length - 1];
            System.arraycopy(cmdAndArgs, 1, cmdArgs, 0, cmdArgs.length);
        } else {
            cmdArgs = new String[0];
        }

        long start = System.currentTimeMillis();;
        try {
            boolean all = cmd.equals("all");
            boolean found = false;
            for (Enumeration e = commands.elements(); e.hasMoreElements(); ) {
                Command c = (Command)e.nextElement();
                if (c.name().equals(cmd) || c.name2().equals(cmd)) {
                    c.run(cmdArgs);
                    found = true;
                    //break;
                } else if (all && c instanceof CompilationCommand) {
                    if (c instanceof CompilationCommandIfPresent) {
                        if (new File(c.name()).exists() == false) {
                            continue;
                        }
// TEMP
if (c.name().equals("jcard3")) {
    continue;
}
                    }
                    try {
                        c.run(cmdArgs);
                    } catch (CommandFailedException ex) {
                        if (!ignoreErrors) {
                            throw ex;
                        }
                        System.err.println(ex);
                    }
                }
            }

            if (!all && !found) {
                stderr.println("Unknown command: " + cmd + " (args="+join(cmdArgs, 0, cmdArgs.length, " ")+")");
                return 1;
            }

        } finally {
            if (useTimer) {
                stdout.println("Time: "+(System.currentTimeMillis()-start)+"ms");
            }
        }


        return 0;
    }

/*---------------------------------------------------------------------------*\
 *                      Java source preprocessor                             *
\*---------------------------------------------------------------------------*/

    private Properties props = new Properties();
    private Hashtable messagesOutput = new Hashtable();

    /**
     * Preprocess a Java source file converting it into another Java
     * source file. This process comments out any lines between
     * preprocessing directives of the form:
     *
     *    /*if[<label>]* /   <-- close C-style comment - space added for
     *    /*end[<label>]* /  <-- syntactic correctness of this source file
     *
     * where the directive is at the beginning of a new line and the value
     * of the property named by <label> is 'false'. The properties are
     * specified in the 'build.properties' file.
     * @param filelist
     * @param clearLines If true, then excluded and preprocessing directive
     * lines are made empty as opposed to being commented.
     * This enables preprocessing directives to be placed in methods that may
     * be converted to macros by j2c. It also enables creating sources that
     * free from redundant stuff - good for extracting a Java Card API for
     * example
     * @return
     * @throws Exception
     */
    public String preprocessSource(String filelist, boolean clearLines, String outDir) throws Exception {
        StringBuffer outfilelist = new StringBuffer();
        String[] files = cut(fix(filelist));
        if (outDir == null) {
            outDir = "temp";
        }
        for (int i = 0 ; i < files.length ; i++) {
            String infile = files[i];

            // Replace the top-level element of the file's path with 'temp'
            int sep = infile.indexOf(File.separatorChar);
            if (sep == -1) {
                throw new CommandFailedException("Cannot find "+File.separator+" in: "+infile);
            }
            String outfile = outDir+infile.substring(sep);

            // Open the streams for the input file and the generated file
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(infile)));
            new File(new File(outfile).getParent()).mkdirs();
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outfile)));
            PrintWriter out = new PrintWriter(bw);

            boolean keep = true;
            boolean output = true;

            Stack lastState = new Stack();
            Stack currentLabel = new Stack();
            currentLabel.push("");

            String line = br.readLine();
            int lno = 0;

            try {
                while(line != null) {
                    lno++;
                    /*
                     * "//if[..."
                     */
                    if (line.startsWith("//if[")) {
                        int end = line.indexOf(']');
                        if (end == -1) {
                            throw new CommandFailedException("Cannot find ] in "+infile+":"+lno);
                        }
                        String sect = line.substring(5, end);
                        String prop = props.getProperty(sect);
                        if (prop == null) {
                            throw new CommandFailedException("No setting for \""+sect+"\" in "+buildPropsFile+" ("+infile+":"+lno+")");
                        }
                        if (prop.equals("false")) {
                            stdout.println("Rejecting "+sect+ " in "+infile);
                            keep = false;
                            break;
                        }
                    } else
                    /*
                     * "/*if[...]* /"
                     */
                    if (line.startsWith("/*if[")) {
                        if (!output && !NESTEDSECTIONS) {
                            throw new CommandFailedException("Cannot nest conditional sections "+infile+":"+lno);
                        }
                        int end = line.indexOf(']');
                        if (end == -1) {
                            throw new CommandFailedException("Cannot find ] in "+infile+":"+lno);
                        }

                        lastState.push(output ? Boolean.TRUE : Boolean.FALSE);

                        String label = line.substring(5, end);
                        String prop = props.getProperty(label);
                        if (prop == null) {
                            throw new CommandFailedException("No setting for \""+label+"\" in "+buildPropsFile+" ("+infile+":"+lno+")");
                        }
                        if (prop.equals("false")) {
                            String msg = "Excluding "+label+" in "+infile;
                            if (messagesOutput.get(msg) == null) {
                                messagesOutput.put(msg, msg);
                                stdout.println(msg);
                            }
                            output = false;
                        }
                        if (clearLines) {
                            line = "";
                        }
                        currentLabel.push(label);
                    } else
                    /*
                     * "/*end[...]* /"
                     */
                    if (line.startsWith("/*end[")) {
                        if (currentLabel.empty() || currentLabel.peek() == "") {
                            throw new CommandFailedException("end[] with no if[] in "+infile+":"+lno);
                        }
                        int end = line.indexOf(']');
                        if (end == -1) {
                            throw new CommandFailedException("Cannot find ] in "+infile+":"+lno);
                        }

                        String label = (String)currentLabel.pop();
                        output = (lastState.pop() == Boolean.TRUE);

                        if (!label.equals(line.substring(6, end))) {
                            throw new CommandFailedException("if/end tag missmatch in "+infile+":"+lno);
                        }
                        if (clearLines) {
                            line = "";
                        }
                    } else {
                        if (!output) {
                            if (clearLines) {
                                line = "";
                            } else {
                                line = "//"+line;
                            }
                        } else {
                            /*
                             * "/*VAL* /"
                             */
                            int index = line.indexOf("/*VAL*/");
                            if (index != -1) {
                                int defaultIndex = index + "/*VAL*/".length();

                                int propIndex = line.indexOf("/*", defaultIndex);
                                int propEndIndex = (propIndex == -1) ? -1 : line.indexOf("*/", propIndex);
                                if (propIndex == -1 || propEndIndex == -1) {
                                    throw new CommandFailedException("Cannot find /*<property name>*/ after /*VAL*/ in "+infile+":"+lno);
                                }

                                String propName     = line.substring(propIndex+2, propEndIndex);

                                String prop = props.getProperty(propName);
                                if (prop != null) {
                                    StringBuffer buf = new StringBuffer(line.length());
                                    buf.append(line.substring(0, defaultIndex)).
                                        append(prop).
                                        append(line.substring(propIndex));
                                    line = buf.toString();
                                }
                            } else
                            /*
                             * "/*@vmaccessed: ..."
                             */
                            if ((index = line.indexOf("/*@vmaccessed:")) != -1) {
                                // Find the name of the variable to which the extra
                                // VMAccessed class attribute flags apply.
                                boolean isClassLevelFlags = true;
                                String fieldName = getFieldNameForVMAccessedAttribute(line, index);

                                // Get the attributes
                                String flags = getVMAccessedFlags(line.substring(index+"/*@vmaccessed:".length()));

                                // Prepend the synthesized static field declaration
                                // that encapsulates the attributes to the current
                                // line. The cannot be private as obfuscators (such
                                // as BCO) may rename it.
                                line = "static final int "+fieldName+" = "+flags+"; " + line;
                            } else
                            /*
                             * "/*@stub: ..."
                             */
                            if ((index = line.indexOf("/*@stub:")) != -1) {
                                if (props.getProperty("ROMIZER").equals("true")) {
                                    // Find the index of the preceeding ';'
                                    int semicolon = line.lastIndexOf(';', index);
                                    int nat = line.indexOf("native");
                                    if (semicolon == -1) {
                                        throw new CommandFailedException("missing ';' in /*@stub: ... */ line in "+infile+":"+lno);
                                    }
                                    if (nat == -1) {
                                        throw new CommandFailedException("missing 'native' in /*@stub: ... */ line in "+infile+":"+lno);
                                    }
                                    int closeComment = line.indexOf("*/", index);
                                    line = line.substring(0, nat) + "/*native*/" +
                                           line.substring(nat + "native".length(), semicolon) +
                                           line.substring(index + "/*@stub:".length(), closeComment);

                                }
                            }
                        }
                    }

                    out.println(line);
                    line = br.readLine();
                }
            } catch (Exception ex) {
                if (ex instanceof CommandFailedException) {
                    throw ex;
                }
                ex.printStackTrace();
                throw new CommandFailedException("Uncaught exception while processing "+infile+":"+lno);
            }

            if (currentLabel.size() != 1) {
                String open = (String)currentLabel.pop();
                throw new CommandFailedException("no end[] for "+open+" in "+infile+":"+lno);
            }

            out.close();
            bw.close();
            br.close();

            if (keep) {
                outfilelist.append(" "+outfile);
            } else {
                delete(outfile);
            }
        }

        return outfilelist.toString();
    }

    /**
     * Move an index into a string beyond any whitespace.
     *
     * @param s
     * @param pos
     * @param forward
     * @return
     */
    private int skipWhitespace(String s, int pos, boolean forward) {
        if (forward) {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) {
                ++pos;
            }
        } else {
            while (pos >= 0 && Character.isWhitespace(s.charAt(pos))) {
                --pos;
            }
        }
        return pos;
    }

    /**
     * Parse to current line which contains the "/*@vmaccessed: ... *\/" pattern
     * to see if it is being applied to a field declaration, a method declaration
     * or the enclosing class declaration. The name of the extra static
     * field that will be inserted to pass these attributes through to the
     * translator is returned.
     *
     * The "/*@vmaccessed: ... *\/" pattern for a field declaration must come
     * after the ';' delimiting the declaration. For a method declaration it
     * must come after the closing ')' of the declaration. If neither of these
     * delimiters are found, the pattern is presumed to be for the enclosing
     * class.
     *
     * @param line
     * @param vmaccIndex
     * @return
     */
    private String getFieldNameForVMAccessedAttribute(String line, int vmaccIndex) {

        int pos = vmaccIndex - 1;
        pos = skipWhitespace(line, pos, false);

        char delim = pos > 0 ? line.charAt(pos) : 0;
        if (delim == ';') {
            // This is a field declaration. Extract the field name and
            // return it.
            while (!Character.isJavaIdentifierPart(line.charAt(pos-1))) {
                --pos;
            }
            int start = pos - 1;
            while (Character.isJavaIdentifierPart(line.charAt(start-1))) {
                --start;
            }
            return line.substring(start, pos) + "$vmaccessed_field";
        } else if (delim == ')') {
            // This is a method declaration. Extract the method name and
            // return it.
            while (line.charAt(pos) != '(') {
                --pos;
            }
            while (!Character.isJavaIdentifierPart(line.charAt(pos-1))) {
                --pos;
            }
            int start = pos - 1;
            while (Character.isJavaIdentifierPart(line.charAt(start-1))) {
                --start;
            }
            return line.substring(start, pos) + "$vmaccessed_method";
        } else {
            return "class" + "$vmaccessed";
        }
    }

    private String getVMAccessedFlags(String flagNames) {
        String suiteFlagNames = "0";
        if (flagNames.indexOf("eeprom") != -1) {
            suiteFlagNames += " | com.sun.squawk.vm.SquawkConstants.VMACC_EEPROM";
        }
        if (flagNames.indexOf("unsigned") != -1) {
            suiteFlagNames += " | com.sun.squawk.vm.SquawkConstants.VMACC_UNSIGNED";
        }
        if (flagNames.indexOf("read") != -1) {
            suiteFlagNames += " | com.sun.squawk.vm.SquawkConstants.VMACC_READ";
        }
        if (flagNames.indexOf("write") != -1) {
            suiteFlagNames += " | com.sun.squawk.vm.SquawkConstants.VMACC_WRITE";
        }
        if (flagNames.indexOf("wbopaque") != -1) {
            suiteFlagNames += " | com.sun.squawk.vm.SquawkConstants.VMACC_WBOPAQUE";
        }
        if (flagNames.indexOf("call") != -1) {
            suiteFlagNames += " | com.sun.squawk.vm.SquawkConstants.VMACC_CALL";
        }
        return suiteFlagNames;
    }

/*---------------------------------------------------------------------------*\
 *                        javac execution                                    *
\*---------------------------------------------------------------------------*/

    String javacOptions = "-g";
    String bcoOptions = null;
    String nobcoFile = null;

    /**
     * Run BCO over all the classes in a specified directory.
     * @param dir
     * @return
     */
    public void bco(String dir) throws Exception {
        // Clean out the backup files from the last BCO build of this target
        clean(new File(dir), ".class.old");
        if (message) {
            stdout.println("[running BCO optimizer ...]");
        }
        int res;

        // Find the classes to process
        String files = find(dir, ".class");

        // Remove the classes specified by '-nobco'
        if (nobcoFile != null) {
            Vector lines = parseLines(nobcoFile, null);
            for (Enumeration e = lines.elements(); e.hasMoreElements();) {
                String line = (String)e.nextElement();

                String classFile = fix(dir) + File.separatorChar + line.replace('.', File.separatorChar) + ".class";
                int index = files.indexOf(classFile);
                if (index != -1) {
                    files = files.substring(0, index) +
                            files.substring(index+classFile.length());
                }
            }
        }

        if ((res = java("-cp tools/BCO.jar;tools/javawand.jar", "bco.BCO ", bcoOptions + " " +files)) != 0) {
            throw new CommandFailedException("BCO returned "+res, res);
        }
    }

    /*
     * javac
     */
    public void javac(String classPath, String dir, String outdir, String files) throws Exception {
        files = preprocessSource(files, clearJppLines, null);
        if (message) {
            stdout.println("[running javac ...]");
        }
        int res = javaCompile(this, classPath+" "+javacOptions+" -d "+dir+"/"+outdir+" "+files, verbose);
        if (!verbose) {
            clean(new File("temp/src"), ".java"); // delete temp files if -verbose is not used
        }
        if (res != 0) {
            throw new CommandFailedException("javac returned "+res, res);
        }
    }

    /*
     * javac_j2se
     */
    public int javac_j2se(String classPath, String dir, String files) throws Exception {
        if (classPath != null) {
            classPath = "-classpath "+classPath;
        } else {
            classPath = "";
        }
        String source = "";

        // Clean out the class files from the last build of this target in
        // case it was build with different preprocessor settings
        clean(new File(dir), ".class");

        javac(source+classPath, dir, "classes", files);

        if (bcoOptions != null) {
            bco(dir+"/classes");
        }

        return 0;
    }

    /**
     * Run the CLDC preverifier over a set of classes and write the resulting
     * classes to a specified directory.
     * @param classPath Directories in which to look for classes.
     * @param outDir Directory in which output is written.
     * @param classes
     * @throws CommandFailedException
     */
    public void preverify(String classPath, String outDir, String classes) throws Exception {
        if (message) {
            stdout.println("[running preverifier ...]");
        }
        int res = exec(os.preverifier()+classPath+" -d "+outDir+" " + classes);
        if (res != 0) {
            throw new CommandFailedException("Preverifier failed", res);
        }
    }

    /**
     * Compile a set of classes against the CLDC API and preverify them.
     * @param classPath
     * @param dir
     * @param files
     * @throws CommandFailedException
     */
    public int javac_j2me(String classPath, String dir, String files) throws Exception {
        String javacClassPath;
        String prevClassPath;

        // Clean out the class files from the last build of this target in
        // case it was build with different preprocessor settings
        clean(new File(dir), ".class");

        if (classPath != null) {
            javacClassPath = "-bootclasspath "+classPath;
            prevClassPath  = " -classpath "+classPath;
        } else {
            javacClassPath = "-bootclasspath .";
            prevClassPath  = "";
        }

        String tmpDir = dir+"/tmpclasses";
        javac(javacClassPath, dir, "tmpclasses", files);

        if (bcoOptions != null) {
            // Run the preverifier first so that BCO doesn't have to deal
            // with JSR and RETs
            preverify(prevClassPath, dir+"/classes", tmpDir);

            // Move all the preverified classes back into the "tmpclasses" dir
            String fromDir = dir + "/classes";
            int prefix = fromDir.length();
            files = find(fromDir, ".class");

            String options = "-d "+dir+"/tmpclasses -nowarn -inline:20 -finalize -crossclass";
            if (classPath != null && classPath.length() != 0) {
                options += " -exe";
            }
            java("-cp tools/javawand.jar", "com.sun.javawand.tools.opt.Optimizer", options+" "+ files);
//            StringTokenizer st = new StringTokenizer(files);
//            while (st.hasMoreTokens()) {
//                String from = st.nextToken();
//                String to = tmpDir + from.substring(prefix);
//                cp(from, to);
//            }

            // Now run BCO
            bco(tmpDir);
        }

        // (Re)run preverifier on output from javac or BCO
        preverify(prevClassPath, dir+"/classes", tmpDir);

        return 0;
    }

/*---------------------------------------------------------------------------*\
 *                        Java execution                                     *
\*---------------------------------------------------------------------------*/

    String javaOptions = "";

    public int java(String vmArgs, String mainClass, String appArgs) throws Exception {
        return exec(os.javaExecutable()+" "+vmArgs+" "+javaOptions+" "+mainClass, appArgs);
    }

    /*
     * fix
     */
    public static String fix(String str) {
        str = str.replace(';', File.pathSeparatorChar);
        str = str.replace('/', File.separatorChar);
        str = str.replace('#', '/');
        return str;
    }


    /*
     * cut
     */
    public static String[] cut(String str, int preambleSize) {
        StringTokenizer st = new StringTokenizer(str, " ");
        String res[] = new String[st.countTokens()+preambleSize];
        while (st.hasMoreTokens()) {
            res[preambleSize++] = st.nextToken();
        }
        return res;
    }

    public static String join(String[] parts, int offset, int length, String delim) {
        StringBuffer buf = new StringBuffer(1000);
        for (int i = offset; i != (offset+length); i++) {
            buf.append(parts[i]);
            if (i != (offset+length)-1) {
                buf.append(delim);
            }
        }
        return buf.toString();
    }

    /*
     * cut
     */
    public static String[] cut(String str) {
        return cut(str, 0);
    }


/*---------------------------------------------------------------------------*\
 *                      System command execution                             *
\*---------------------------------------------------------------------------*/

    private File cwd;

    /**
     * Change the current working directory. This only effects the working directory
     * of commands run through 'exec()'.
     * @param dir
     */
    public void cd(String dir) {
        dir = fix(dir);
        if (verbose) {
            stdout.println("cd "+dir);
        }
        File fDir = new File(dir);
        if (fDir.isAbsolute()) {
            cwd = fDir;
        } else {
            try {
                cwd = (new File(cwd, dir)).getCanonicalFile();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    /*
     * exec
     */
    public int exec(String cmd) throws Exception {
        return exec(cmd, "");
    }

    /*
     * exec
     */
    public int exec(String cmd, String options) throws Exception {
        return exec(cmd, options, null);
    }

    public int exec(String cmd, String options, String[] envp) throws Exception {

        PrintStream out = stdout;
        PrintStream err = stderr;

        cmd = fix(cmd);
        if (options != null && options.length() > 0) {
            cmd += " " + options;
        }
        if (verbose) {
            stdout.println("EXEC: "+cmd);
        }

        Process proc = null;
        try {
            proc = Runtime.getRuntime().exec(cmd, envp, cwd);
            StreamGobbler errorGobbler  = new StreamGobbler(proc.getErrorStream(), err, "");
            StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream(), out, "");
            errorGobbler.start();
            outputGobbler.start();

            int exitVal = proc.waitFor();
            errorGobbler.join();
            outputGobbler.join();

            if (verbose || exitVal != 0) {
                stdout.println("EXEC result =====> " + exitVal);
            }
            if (exitVal != 0) {
                throw new CommandFailedException("Process.exec("+cmd+") returned "+exitVal, exitVal);
            }
            return exitVal;
        } catch (InterruptedException ie) {
            return -1;
        } finally {
            // Ensure that the native process (if any is killed).
            if (proc != null) {
                proc.destroy();
                proc = null;
            }
        }
    }

/*---------------------------------------------------------------------------*\
 *                        Java to C converter                                *
\*---------------------------------------------------------------------------*/

    private static final String OPEN_C_COMMENT  = "/*";
    private static final String CLOSE_C_COMMENT = "*/";


    /**
     * This is a stream tokenizer that keeps track of the spaces preceeding
     * each token.
     */
    static class SpaceStreamTokenizer extends StreamTokenizer {
        private final static char[] SPACE_CHARS = new char[10000];
        static {
            for (int i = 0; i != SPACE_CHARS.length; i++) {
                SPACE_CHARS[i] = ' ';
            }
        }

        private int spaces;
        public int nextToken() throws IOException {
            spaces = 0;
            int t = super.nextToken();
            while (t == ' ') {
                spaces++;
                t = super.nextToken();
            }
            return t;
        }

        public void appendSpaces(StringBuffer buf) {
            if (spaces > 0) {
                buf.append(SPACE_CHARS, 0, spaces);
            }
        }

        public String getSpaces() {
            return new String(SPACE_CHARS, 0, spaces);
        }

        SpaceStreamTokenizer(String line) {
            super(new StringReader(line));
            for (char ch = ' '; ch != 0x7F; ch++) {
                if (Character.isJavaIdentifierPart(ch)) {
                    super.wordChars(ch, ch);
                }
            }
            super.ordinaryChar(' ');
            // Turn off string recognition
            super.ordinaryChar('"');
            super.ordinaryChar('\'');
            // Turn off special comment recognition
            super.ordinaryChar('/');
        }
    }

    static class MacroDefinition {
        /** A counter used to give each macro local variable a unique name. */
        private static int macroLocalVariableSuffix;

        /** The current block nesting level (i.e. number of open '{'s). */
        int nestingLevel;
        /** The original first line of the macro. */
        String functionDeclLine;
        /** */
//        String macroDeclLine;
        /** The lines of the macro with the parameter substitution. */
        Vector lines;
        /** The block of inner locals declaration and initialisations. */
        String innerLocals;
        /** A flag determining whether or not the macro actual needs to
            be rewritten as an inline function. */
        boolean inline;
        /** A flag determining if the macro return type is void. */
        boolean isVoid;
        /** A remembered set of the parameters that have been used. */
        Vector usedParms;
        /** A map from parameter names (without the leading '$') to the
            name of the inner scope local variable used to guarantee
            the idempotent semantics of the actual parameter. */
        Hashtable locals;
        int statementCount;

        StringBuffer currentLine;

        /**
         * Parse a function declaration and reformat it into a macro declaration.
         * @param st
         * @param macro
         * @param parameterMap Map of parameter names to local variable names.
         * @return
         * @throws IOException
         */
        MacroDefinition(SpaceStreamTokenizer st, String line) throws IOException {
            this.functionDeclLine = line;

            currentLine = new StringBuffer(100);

            // Parse the access modifiers, return type and name of the function
            Vector tokens = new Vector();
            int token;
            while ((token = st.nextToken()) != '(') {
                if (token == st.TT_WORD) {
                    tokens.addElement(st.getSpaces()+st.sval);
                }
            }

            // Build the first line of the macro definition
            for (Enumeration e = tokens.elements(); e.hasMoreElements();) {
                String t = (String)e.nextElement();
                if (e.hasMoreElements()) {
                    if (t.endsWith("void")) {
                        isVoid = true;
                    }
                } else {
                    // This is the function/macro name
                    currentLine.append(t).append('(');
                }
            }

            locals = new Hashtable();
            if (!isVoid) {
                usedParms = new Vector();
            }

            // Parse the function parameters
            String parm = null;
            int dims = 0;
            StringBuffer localsDecl = (isVoid ? new StringBuffer() : null);
            do {
                token = st.nextToken();
                if (token == st.TT_WORD) {
                    parm = st.sval;
                    if (isVoid) {
                        st.appendSpaces(localsDecl);
                        localsDecl.append(st.sval);
                    }
                } else if (token == ',' || token == ')') {
                    if (parm != null) {
                        currentLine.append(createLocal(parm, dims, localsDecl));
                        if (token == ',') {
                            currentLine.append(", ");
                        }
                    }
                    parm = null;
                    dims = 0;
                } else if (token == '[' || token == ']') {
                    if (isVoid && token == ']') {
                        dims++;
                    }
                } else {
                    if (token == st.TT_EOF || token == st.TT_EOL) {
                        throw new IOException("Macro declaration (up to '{') must be on one line");
                    } else {
                        throw new IOException("Unexpected token while parsing macro declaration: "+st.sval);
                    }
                }

            } while (token != ')');
            currentLine.append(") { ");

            if (st.nextToken() != '{') {
                throw new IOException("missing opening '{'");
            }
            nestingLevel++;

            if (isVoid) {
                currentLine.append(localsDecl.toString().replace('$', ' '));
            }
        }

        /**
         * Parse the stream up to and including the next ';' adding the parsed
         * content to the macro string being built.
         * @param st
         * @param macro
         * @return
         * @throws IOException
         */
        void parseStatements(SpaceStreamTokenizer st) throws IOException {
            int token = -1;
            while ((token = st.nextToken()) != st.TT_EOF) {
                switch (token) {
                    case SpaceStreamTokenizer.TT_WORD: {
                        st.appendSpaces(currentLine);
                        String parm = st.sval;
                        boolean isParm = parm.charAt(0) == '$';
                        if (isParm) {
                            if (!isVoid) {
                                if (usedParms.contains(parm)) {
                                    inline = true;
                                } else {
                                    usedParms.add(parm);
                                }
                            }
                            parm = (String)locals.get(parm);
                            currentLine.append('(').append(parm).append(')');
                        } else {
                            currentLine.append(parm);
                        }
                        break;
                    }
                    case SpaceStreamTokenizer.TT_NUMBER: {
                        st.appendSpaces(currentLine);
                        // Cast the value to an int if it is an int value so that
                        // the StringBuffer does not append a double representation
                        // for non-double numbers.
                        if ((double)((int)st.nval) == st.nval) {
                            currentLine.append((int)st.nval);
                        } else {
                            currentLine.append(st.nval);
                        }
                        break;
                    }
                    case ';': {
                        ++statementCount;
                        if (!isVoid && statementCount > 1) {
                            inline = true;
                        }
                        st.appendSpaces(currentLine);
                        currentLine.append(';');

                        break;
                    }
                    case '{': {
                        ++nestingLevel;
                        st.appendSpaces(currentLine);
                        currentLine.append('{');
                        break;
                    }
                    case '}': {
                        --nestingLevel;
                        st.appendSpaces(currentLine);
                        currentLine.append('}');
                        break;
                    }
                    default: {
                        char ctoken = (char)token;
                        st.appendSpaces(currentLine);
                        currentLine.append(ctoken);
                        break;
                    }
                }
            }

            if (lines == null) {
                lines = new Vector();
            }
            lines.addElement(currentLine.toString());

            if (nestingLevel == 0) {
                currentLine = null;
                finishedParsing();
            } else {
                currentLine = new StringBuffer(100);
            }
        }

        /**
         * Create an inner scoped local variable for a macro parameter. The local
         * variable will have a name based on the macro parameter name concatenated
         * with a unique suffix to ensure that all macro local variables have a
         * unique name.
         * @param parm The fucntion parameter (which must start with '$').
         * @param localsDecl The string buffer being used to build up the
         * declaration and initialisation
         * @return 'parm' with the leading '$' stripped off and a unique suffix
         * appended to it.
         */
        private String createLocal(String parm, int dims, StringBuffer localsDecl) throws IOException {
            if (parm.charAt(0) != '$') {
                throw new IOException("Macro parameters must start with '$'");
            }

            String base = parm.substring(1);
            String uniqParm = base + "_" + (macroLocalVariableSuffix++);

            if (isVoid) {
                String suffix = "_" + (macroLocalVariableSuffix++);
                String local = base + suffix;
                locals.put(parm, local);
                if (dims != 0) {
                    int index = localsDecl.toString().lastIndexOf("$");
                    localsDecl.insert(index+1, ASTERISKS, 0, dims);
                }
                localsDecl.append(suffix + " = " + uniqParm + "; ");
            } else {
                locals.put(parm, uniqParm);
            }
            return uniqParm;
        }
        private static final char[] ASTERISKS = { '*', '*', '*', '*' };

        void finishedParsing() {
            if (inline) {
                String l = functionDeclLine;
                // Replace parameters with their unique names
                StringBuffer buf = new StringBuffer(l.length());

                int end = 0;
                while (true) {
                    int index = l.indexOf('$', end);
                    if (index != -1) {
                        if (end < index) {
                            buf.append(l.substring(end, index));
                        }

                        end = index;
                        while (end < l.length() &&
                               Character.isJavaIdentifierPart(l.charAt(end))) {
                            end++;
                        }
                        String uniqParm = (String)locals.get(l.substring(index, end));
                        buf.append(uniqParm);
                    } else {
                        buf.append(l.substring(end));
                        break;
                    }
                }
                functionDeclLine = buf.toString();

            }
            if (!isVoid && statementCount == 1 && !inline) {
                for (int i = 0; i != lines.size(); i++) {
                    String line = (String)lines.elementAt(i);
                    int retIndex = line.indexOf("return");
                    if (retIndex != -1) {
                        line = line.substring(0, retIndex) +
                               line.substring(retIndex+"return".length());
                    }
                    line = line.replace('{', '(').
                                replace('}', ')').
                                replace(';', ' ');
                    lines.setElementAt(line, i);
                }
            }
        }
    }

    /**
     * Preprocess a Java source file to turn it into a C source file.
     * @param filelist
     * @param outdir
     * @throws Exception
     */
    public void j2c(String filelist, String outdir) throws Exception {
        filelist = preprocessSource(fix(filelist), true, null);
        String[] files = cut(filelist);
        for (int i = 0 ; i < files.length ; i++) {
            int lineNo = 1;
            BufferedReader br = new BufferedReader(new FileReader(files[i]));
            String line = br.readLine();
            if (line.startsWith("//J2C:")) {
                line = line.substring(6);
                int index = line.indexOf(' ');
                if (index > 0) {
                    line = line.substring(0, index);
                }

                String fileName = fix(outdir+"/"+line);
                FileOutputStream fos = new FileOutputStream(fileName);
                PrintStream out = new PrintStream(fos);
                out.println("/**** Created by Squawk builder from \""+files[i]+"\" ****/");

                // The current macro definition being parsed.
                MacroDefinition macro = null;

                // Flags whether or not a set of constants is being defined
                boolean inConstantsDecl      = false;
                boolean inConstantsArrayDecl = false;

                while ((line = br.readLine()) != null) {
                    lineNo++;
                    String prefix = null;
                    if (line.startsWith("/*IFJ*/")) {
                        line = "/**** Line deleted by Squawk builder ****/";
                    } else if (line.startsWith("//IFC//")) {
                        line = line.substring(7);
                        if (line.length() > 0 && line.charAt(0) != '#') {
                            prefix = "       ";
                        }
                    } else {
                        index = line.indexOf("static final");
                        if (index == -1) {
                            index = line.indexOf("final static");
                        }
                        if (index != -1 ) {
                            String rest = line.substring(index + "static final".length()).trim();
                            if (rest.endsWith("int") ||
                                rest.endsWith("char") ||
                                rest.endsWith("short") ||
                                rest.endsWith("byte")) {
                                line = "enum { ";
                                inConstantsDecl = true;
                            }
                            else {
                                if (rest.startsWith("String[]")) {
                                    rest = rest.substring("String[]".length()).trim();
                                    String name = rest.substring(0, rest.indexOf(' '));
                                    rest = rest.substring(name.length());
                                    line = "char* " + name + "[]" + rest;
                                    inConstantsArrayDecl = true;
                                } else
                                if (rest.startsWith("String[]")    ||
                                    rest.startsWith("boolean[]")   ||
                                    rest.startsWith("int[]")       ||
                                    rest.startsWith("char[]")      ||
                                    rest.startsWith("short[]")     ||
                                    rest.startsWith("byte[]")) {

                                    String type = rest.substring(0, rest.indexOf('['));
                                    if (type.equals("String")) {
                                        type = "char*";
                                    }

                                    rest = rest.substring(type.length()+2).trim();
                                    String name = rest.substring(0, rest.indexOf(' '));
                                    rest = rest.substring(name.length());
                                    line = type + " " + name + "[]" + rest;
                                    inConstantsArrayDecl = true;
                                }
                            }
                        } else {
                            if (inConstantsDecl) {
                                int semicolon = line.lastIndexOf(';');
                                if (semicolon != -1) {
                                    line = line.substring(0, semicolon) + '}' + line.substring(semicolon);
                                    inConstantsDecl = false;
                                }
                            } else if (inConstantsArrayDecl) {
                                if (line.lastIndexOf(';') != -1) {
                                    inConstantsArrayDecl = false;
                                }
                            }
                        }
                    }

                    boolean replaceLongs = (line.length() > 0 && line.charAt(0) != '#');

                    line = replaceConstants(line, fileName, lineNo);

                    // Expand /*MAC*/ lines
                    boolean isMacroDefLine = line.startsWith("/*MAC*/");
                    if ((isMacroDefLine || macro != null) && ccompiler.options.macroize) {

                        if (isMacroDefLine) {
                            line = line.substring("/*MAC*/".length()).trim();
                        }

                        SpaceStreamTokenizer st = new SpaceStreamTokenizer(line);
                        prefix = null;
                        try {
                            if (isMacroDefLine) {
                                macro = new MacroDefinition(st, line);
                            }
                            macro.parseStatements(st);

                            if (macro.nestingLevel == 0) {
                                Enumeration e = macro.lines.elements();
                                String macroDecl = (String)e.nextElement();
                                // Emit inlined function or macro definition
                                if (macro.inline) {
                                    line = macro.functionDeclLine;
                                    line = "INLINE "+line.replace('$', ' ');
                                } else {
                                    line = "#define " + macroDecl;
                                    if (e.hasMoreElements()) {
                                        line += " \\";
                                    }
                                }
                                writeLine(out, line, replaceLongs);

                                // Emit the rest of the lines
                                while (e.hasMoreElements()) {
                                    line = (String)e.nextElement();
                                    if (!macro.inline && e.hasMoreElements()) {
                                        line += " \\";
                                    }
                                    writeLine(out, line, replaceLongs);
                                }

                                macro = null;
                            }

                        } catch (IOException ioe) {
                            throw new CommandFailedException("Could not parse macro in "+files[i]+":"+lineNo+": "+ioe.getMessage());
                        }
                    } else {
                        if (line.startsWith("//")) {
                            line = "";
                        } else {
                            if (prefix != null) {
                                out.print(prefix);
                            }

                            if (line.indexOf('$') != -1) {
                                if (ccompiler.options.macroize) {
                                    stderr.println("Warning: replacing '$' with '#' in "+fileName+":"+lineNo);
                                    line = line.replace('$', '#');
                                } else {
                                    line = line.replace('$', '_');
                                }
                            }
                        }
                        writeLine(out, line, replaceLongs);
                    }
                }

                if (macro != null) {
                    throw new IOException("unclosed macro");
                }
                out.close();
                fos.close();
            }
        }
        if (!verbose) {
            clean(new File("temp/src"), ".java"); // delete temp files if -verbose is not used
        }
    }

    private static final Set CONSTANT_PREFIXES = new HashSet(Arrays.asList(new String[] {
        "OPC.",
        "CNO.",
        "SquawkConstants.",
        "ChannelOpcodes.",
        "MathOpcodes.",
        "Mnemonics."
    }));

    /**
     * Replace all constants defined in classes in the com.sun.squawk.vm.*
     * package on the given line with their values.
     *
     * @param line
     * @return
     */
    private String replaceConstants(String line, String file, int lineNo) {
        // Replace constants in com.sun.squawk.vm.* classes with
        // their values.
        if (line.indexOf('.') != -1) {
            for (Iterator iterator = CONSTANT_PREFIXES.iterator(); iterator.hasNext();) {
                String constantPrefix = (String)iterator.next();
                int index = line.indexOf(constantPrefix);
                while (index != -1) {
                    int period = index + (constantPrefix.length() - 1);
                    int end = period + 1;
                    while (Character.isJavaIdentifierPart(line.charAt(end))) {
                        end++;
                    }

                    String constant = line.substring(index, end);
                    String className = "com.sun.squawk.vm."+line.substring(index, period);
                    String fieldName = line.substring(period+1, end);

                    Exception e = null;
                    try {
                        Class klass = Class.forName(className);
                        Field field = klass.getField(fieldName);
                        Class fieldType = field.getType();
                        Object value;
                        if (fieldType == Character.TYPE) {
                            int ch = ((Character)field.get(null)).charValue();
                            value = String.valueOf(ch);
                        } else if (fieldType.isPrimitive()) {
                            value = field.get(null).toString();
                        } else {
                            value = fieldName;
                        }
//System.err.println("["+file+":"+lineNo+"] "+constant+"="+value);
                        String lineEnd = line.substring(end);
                        line = line.substring(0, index) + value + "/*" +
                            constant +  "*/" + lineEnd;


                        index = line.indexOf(constantPrefix, line.length() - lineEnd.length());
                    } catch (IllegalAccessException ex) {
                        e = ex;
                    } catch (IllegalArgumentException ex) {
                        e = ex;
                    } catch (SecurityException ex) {
                        e = ex;
                    } catch (NoSuchFieldException ex) {
                        e = ex;
                    } catch (ClassNotFoundException ex) {
                        e = ex;
                    }
                    if (e != null) {
                        e.printStackTrace();
                        throw new CommandFailedException("Error while replacing constant '"+constant+
                            "' in "+file+":"+lineNo);
                    }
                }
            }
        }
        return line;
    }

    /*
     * writeLine
     */
    void writeLine(PrintStream out, String line, boolean replaceLongs) throws Exception {
        String delims = " \t\n\r(){/";
        StringTokenizer st = new StringTokenizer(line, delims, true);
        while(st.hasMoreTokens()) {
            String next = st.nextToken();
            if (replaceLongs) {
                if (next.equals("ulong")) {
                    next = "ujlong";
                } else if (next.equals("long")) {
                    next = "jlong";
                }
            }
            if (next.charAt(0) != '\r' && next.charAt(0) != '\n') {
                out.print(next);
            }
        }
        out.println();
    }
}

class CommandFailedException extends RuntimeException {
    public final int exitVal;
    CommandFailedException(String msg, int exitVal) {
        super(msg);
        this.exitVal = exitVal;
    }
    CommandFailedException(String msg) {
        super(msg);
        this.exitVal = 1;
    }
}
