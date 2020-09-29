package java.lang;


public class TckRunner {

    public static void main(String[] args) {

        boolean neg = false;
        VMExtension.tckMode = true;
        int nameArg = 0;

        if (args.length > 0) {
            if (args[0].equals("+")) {
                nameArg = 1;
            } else if (args[0].equals("-")) {
                nameArg = 1;
                VMExtension.negativeTckTest = true;
            }
        } else {
            usage(args);
        }

       /*
        * Strip off the first argument
        */
        String className = args[nameArg];
        Object old = args;
        args = new String[args.length - nameArg - 1];
        System.arraycopy(old, nameArg+1, args, 0, args.length);

       /*
        * Lookup the class
        */
        Klass cls = null;
        try {
            cls = Klass.forName(className);
            cls.main(args);
            System.out.println("TckRunner -- Failed: returned from main()");
            System.exit(99);
        } catch(ClassNotFoundException ex) {
            System.out.println("TckRunner -- Cannot find class " + className);
            System.exit(99);
        }

    }


    static void usage(String[] args) {
        System.out.print("TckRunner -- Bad cmd line:");
        for (int i = 0; i < args.length; i++) {
            System.out.print(" " + args[i]);
        }
        System.out.println();
        System.exit(99);
    }


}
