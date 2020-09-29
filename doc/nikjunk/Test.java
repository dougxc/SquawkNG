

public class Test extends Super {

    int a = 1;

    Test() {
       this(1);
    }

    Test(int i) {
       super(i);
    }

    int foo() {
        int x = 1;
        return x;
    }

    static void yyy() {
        Test t = new Test();
        t.xxx();
    }

}


class Super {
    Super(int a) {
       super();
    }

    void xxx() {
    }
}