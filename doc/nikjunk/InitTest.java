public class InitTest {
    static void main(String[] args) {
        new c1(new c2());
    }
}

class c1 {
    static {
       System.out.println("c1 <clinit>");
    }
    c1(c2 x) {
       System.out.println("c1 <init>");
    }
}

class c2 {
    static {
       System.out.println("c2 <clinit>");
    }
    c2() {
       System.out.println("c2 <init>");
    }
}


/*
Bytecodes:

    Method void main(java.lang.String[])
       0 new #2 <Class c1>
       3 dup
       4 new #3 <Class c2>
       7 dup
       8 invokespecial #4 <Method c2()>
      11 invokespecial #5 <Method c1(c2)>
      14 pop
      15 return


Result:

    c1 <clinit>
    c2 <clinit>
    c2 <init>
    c1 <init>
*/