class Park {

//Example:

    synchronized void foo() {
        System.out.println("Hello World");
    }

//Becomes logically:

    void foo() {
        synchronized(this) {
            System.out.println("Hello World");
        }
    }

}

/*
In the class file:


    Class:      Test
    Method:     foo()
    Access:     ACC_SYNCHRONIZED
    Body:
           0: getstatic #2 <Field java.io.PrintStream out>
           3: ldc #3 <String "Hello World">
           5: invokevirtual #4 <Method void println(java.lang.String)>
           8: return

    Exception table:



Becomes:


    Class:      Test
    Method:     foo()
    Access:     Nothing
    Body:
           0: aload_0
           1: monitorenter
           2: getstatic #2 <Field java.io.PrintStream out>
           5: ldc #3 <String "Hello World">
           7: invokevirtual #4 <Method void println(java.lang.String)>
          10: aload_0
          11: monitorexit
          12: return
          13: astore_1
          14: aload_0
          15: monitorexit
          16: aload_1
          17: athrow
          18: return

    Exception table:
       from   to  target type
         2    12    13   any
*/