
public class Foo implements test {

    static String clinit = "Test Failure";

    public static void main(String[] x) {
        foo1(x);
        foo2(x);
        foo3(x);
        foo4(x);
        foo5(x);
    }

    // instanceof should not call <clinit>
    public static void foo1(Object x) {
        if (!(x instanceof test)) {
            System.out.println("foo1 success");
        }
    }

    // checkcast should not call <clinit>
    public static void foo2(Object x) {
        try {
            test z = (test)x;
        } catch(Exception ex) {
            System.out.println("foo2 success");
            return;
        }
    }

    // anewarray should not call <clinit>
    public static void foo3(Object x) {
        test[] z = new test[1];
        System.out.println("foo3 success");
    }

    // invokeintereface should not call <clinit>
    public static void foo4(Object x) {
       test t = new Foo();
       t.f();
    }

    public void f() {
        System.out.println("foo4 success");
    }

    // getstatic should call <clinit>
    public static void foo5(Object x) {
       clinit = "foo5 success";
       int i = test.foo;
    }

    public static int testclinit() {
        System.out.println(clinit);
        return 1;
    }
}

interface test {
    final static int foo = Foo.testclinit();
    public void f();
}