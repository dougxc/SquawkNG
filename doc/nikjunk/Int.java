/*

The following experement may be of interest. It is an attempt to quantify what Java would perform
like if integers (ints) were implemented as objects rather than primitive data types but without
changing the VM (clearly much better results would be attained if the VM were changed).

The basic idea is to:

  - Support an integer data type that has potentially infinate magnitude (like Smalltalk/Self)
  - Build a cache of integer objects costing a few MBs.
  - Implement a SmallInteger like object that is between -2**30 to 2**30-1
  - And a LargeInteger type that much larger but which is assumed to be very rare.

The cost is roughtly that simple integer operation are about 6.5 times slower if the LargeInteger
class is never actually loaded. This rises to about 13 times slower if the LargeInteger is loaded.
(This increase is presumably because the SmallInteger class is no longer the only possible receiver
and so the costs of method invocation increase.)

There is an even more interesting result from test3 (see below) when a Java "int" is being applied
to a SmallInteger object (such as might be the case where a constant was being added to a variable).
In this case the difference between LargeInteger being loaded is marginal. The result being roughly
5 times slower in both cases. The differnce between this test and the one where no Java primitives
are involved is that one one virtual function call is being performed. Perhaps the JIT is more
highly tuned to doing this rather than when there are two virtual dispatches in quick succession.

Even thought this is only a micro benchmark it is likely that the cost of using this trick is to
slow down the integer operations by a factor of between five and ten. What is interesting to note is
that the same benchmark run on the the latesr x86 version of Squwak is 42 times slower!

*/




public abstract class Int {

    private final static int POSINTS = 60000;
    private final static int NEGINTS = 10000;

    protected static SmallInteger[] posints, negints;

    static {
        posints = new SmallInteger[POSINTS];
        for (int i = 0; i < POSINTS ; i++) {
            posints[i] = new SmallInteger(i);
        }
        negints = new SmallInteger[NEGINTS];
        for (int i = 0; i < NEGINTS ; i++) {
            negints[i] = new SmallInteger(0-i);
        }
    }

    public static Int createInt(int x) {
        try {
            return posints[x];
        } catch(ArrayIndexOutOfBoundsException ex) {
            return createInt2(x);
        }
    }

    private static Int createInt2(int x) {
        try {
            return negints[0-x];
        } catch(ArrayIndexOutOfBoundsException ex) {
            return createInt3(x);
        }
    }

    private static Int createInt3(int x) {
        if ((x ^ (x << 1)) >= 0) {
            return new SmallInteger(x);
        } else {
            return Proxy.createLargeInteger(x);     //return new LargeInteger(x);
        }
    }

    protected static Int createInt(int x, int y) {  // returns x * y
        long res = x * y;
        long test = res >>> 32;
        if (test == 0 || test == -1L) {
            return createInt((int)res);
        } else {
            //return Proxy.createLargeInteger(x, y);
            return new LargeInteger(x, y);
        }
    }


    abstract public boolean eq(int x);
    abstract public boolean ne(int x);
    abstract public boolean lt(int x);
    abstract public boolean le(int x);
    abstract public boolean gt(int x);
    abstract public boolean ge(int x);

    abstract public boolean eq(Int x);
    abstract public boolean ne(Int x);
    abstract public boolean lt(Int x);
    abstract public boolean le(Int x);
    abstract public boolean gt(Int x);
    abstract public boolean ge(Int x);

    abstract public Int add(int x);
    abstract public Int sub(int x);
    abstract public Int rsub(int x);
    abstract public Int mul(int x);
    abstract public Int div(int x);
    abstract public Int rdiv(int x);
    abstract public Int rem(int x);
    abstract public Int rrem(int x);

    abstract public Int add(Int x);
    abstract public Int sub(Int x);
    abstract public Int mul(Int x);
    abstract public Int div(Int x);
    abstract public Int rem(Int x);


    public static void main(String[] args) {
        test1(true);     test1(true);      test1(true);
        test2(true);     test2(true);      test2(true);
        test3(true);     test3(true);      test3(true);
    }

    static void test1(boolean print) {

        // Test1 using primitive ints

        int a = 0;
        long start = System.currentTimeMillis();

        for (int x = 0 ; x < 100 ; x++) {
            for (int y = 0 ; y < 100 ; y++) {
                for (int i = 0 ; i < 100 ; i++) {
                    for (int j = 0 ; j < 100 ; j++) {
                        int k = i * j;
                        int l = k + 1;
                        if (l == 100) {
                            a++;
                        }
                    }
                }
            }
        }

        if (print) System.out.println("int test1 -- a="+a+" ("+(System.currentTimeMillis()-start)+")");

    }


    static void test2(boolean print) {

        // Test2 using object integers

        Int ZERO    = createInt(0);
        Int ONE     = createInt(1);
        Int HUNDRED = createInt(100);
        Int A = ZERO;

        long start = System.currentTimeMillis();

        for (Int x = ZERO ; x.lt(HUNDRED) ; x = x.add(ONE)) {
            for (Int y = ZERO ; y.lt(HUNDRED) ; y = y.add(ONE)) {
                for (Int i = ZERO ; i.lt(HUNDRED) ; i = i.add(ONE)) {
                    for (Int j = ZERO ; j.lt(HUNDRED) ; j = j.add(ONE)) {
                        Int k = i.mul(j);
                        Int l = k.add(ONE);
                        if (l.eq(HUNDRED)) {
                            A = A.add(ONE);
                        }
                    }
                }
            }
        }
        if (print) System.out.println("Int test2 -- A="+((SmallInteger)A).value+" ("+(System.currentTimeMillis()-start)+")");
    }


    static void test3(boolean print) {

        // Test3 using object integers, but with primitive int constants

        Int ZERO    = createInt(0);
        Int ONE     = createInt(1);
        Int HUNDRED = createInt(100);
        Int A = ZERO;

        long start = System.currentTimeMillis();

        for (Int x = ZERO ; x.lt(100) ; x = x.add(1)) {
            for (Int y = ZERO ; y.lt(100) ; y = y.add(1)) {
                for (Int i = ZERO ; i.lt(100) ; i = i.add(1)) {
                    for (Int j = ZERO ; j.lt(100) ; j = j.add(1)) {
                        Int k = i.mul(j);
                        Int l = k.add(1);
                        if (l.eq(100)) {
                            A = A.add(1);
                        }
                    }
                }
            }
        }
        if (print) System.out.println("Int test3 -- A="+((SmallInteger)A).value+" ("+(System.currentTimeMillis()-start)+")");
    }

}


class SmallInteger extends Int {

    final int value;

    public SmallInteger(int x) {
        value = x;
    }

    public boolean eq(int x) { return value == x; }
    public boolean ne(int x) { return value != x; }
    public boolean lt(int x) { return value <  x; }
    public boolean le(int x) { return value <= x; }
    public boolean gt(int x) { return value >  x; }
    public boolean ge(int x) { return value >= x; }

    public boolean eq(Int x) { return x.eq(value); }
    public boolean ne(Int x) { return x.ne(value); }
    public boolean lt(Int x) { return x.gt(value); }
    public boolean le(Int x) { return x.ge(value); }
    public boolean gt(Int x) { return x.lt(value); }
    public boolean ge(Int x) { return x.le(value); }

    public Int add(int x)    { return createInt(value + x); }
    public Int sub(int x)    { return createInt(value - x); }
    public Int rsub(int x)   { return createInt(x - value); }
    public Int mul(int x)    { return createInt(x, value);  }
    public Int div(int x)    { return createInt(value / x); }
    public Int rdiv(int x)   { return createInt(x / value); }
    public Int rem(int x)    { return createInt(value % x); }
    public Int rrem(int x)   { return createInt(x % value); }

    //public Int add(Int x)    { return (x instanceof SmallInteger) ? createInt(((SmallInteger)x).value + value) : x.add(value);   } // Slower on 1.4.1
    public Int add(Int x)    { return x.add(value);  }
    public Int sub(Int x)    { return x.rsub(value); }
    public Int mul(Int x)    { return x.mul(value);  }
    public Int div(Int x)    { return x.rdiv(value); }
    public Int rem(Int x)    { return x.rrem(value); }
}



// Proxy class that is needed to avoid LargeInteger being loaded if it is not used

class Proxy {
    public static Int createLargeInteger(int x) {
        return new LargeInteger(x);
    }

    public static Int createLargeInteger(int x, int y) {
        return new LargeInteger(x, y);
    }
}


// Dummy LargeInteger class.

class LargeInteger extends Int {
    LargeInteger(int x) {
        throw new Error();
    }

    LargeInteger(int x, int y) {
        throw new Error();
    }

    public boolean eq(int x) { throw new Error(); }
    public boolean ne(int x) { throw new Error(); }
    public boolean lt(int x) { throw new Error(); }
    public boolean le(int x) { throw new Error(); }
    public boolean gt(int x) { throw new Error(); }
    public boolean ge(int x) { throw new Error(); }

    public boolean eq(Int x) { throw new Error(); }
    public boolean ne(Int x) { throw new Error(); }
    public boolean lt(Int x) { throw new Error(); }
    public boolean le(Int x) { throw new Error(); }
    public boolean gt(Int x) { throw new Error(); }
    public boolean ge(Int x) { throw new Error(); }

    public Int add(int x)    { throw new Error(); }
    public Int sub(int x)    { throw new Error(); }
    public Int rsub(int x)   { throw new Error(); }
    public Int mul(int x)    { throw new Error(); }
    public Int div(int x)    { throw new Error(); }
    public Int rdiv(int x)   { throw new Error(); }
    public Int rem(int x)    { throw new Error(); }
    public Int rrem(int x)   { throw new Error(); }

    public Int add(Int x)    { throw new Error(); }
    public Int sub(Int x)    { throw new Error(); }
    public Int mul(Int x)    { throw new Error(); }
    public Int div(Int x)    { throw new Error(); }
    public Int rem(Int x)    { throw new Error(); }
}


/*


Results:

-----------------------------------------------------------------------
Java(TM) 2 Runtime Environment, Standard Edition (build 1.4.1-beta-b14)
Java HotSpot(TM) Server VM (build 1.4.1-beta-b14, mixed mode)
-----------------------------------------------------------------------


 Using Hotspot server compiler and with LargeInteger not loaded:

    int test1 -- a=60000 (731)
    Int test2 -- A=60000 (4717)
    Int test3 -- A=60000 (3625)

 With LargeInteger loaded:

    int test1 -- a=60000 (721)
    Int test2 -- A=60000 (9644)
    Int test3 -- A=60000 (3886)


----------------------
Using squeak3.2 (4956)
----------------------


        ^Time millisecondsToRun: [

            | a |
            a := 0.

            0 to: 100 do: [ :x |
                0 to: 100 do: [ :y |
                    0 to: 100 do: [ :i |
                        0 to: 100 do: [ :j | | k l |
                            k := i * j.
                            l := k + 1.
                            (l = 100) ifTrue: [
                                 a := a + 1.
                            ].
                        ].
                    ].
                ].
            ].
        ].

    Produces: 33462

        ^Time millisecondsToRun: [

            | a |
            a := 0.

            100 timesRepeat: [
                100 timesRepeat: [
                    0 to: 100 do: [ :i|
                        0 to: 100 do: [ :j| | k l |
                            k := i * j.
                            l := k + 1.
                            (l = 100) ifTrue: [
                                 a := a + 1.
                            ].
                        ].
                    ].
                ].
            ].
        ].

    Produces: 31647


























OLD RESULTS using 1.3.1





---------------------------------------------------------------------
Java(TM) 2 Runtime Environment, Standard Edition (build 1.3.1_03-b03)
Java HotSpot(TM) Server VM (build 1.3.1_03-b03, mixed mode)
---------------------------------------------------------------------

 Using Hotspot server compiler and with LargeInteger not loaded:

    int test1 -- a=60000 (1162)
    Int test2 -- A=60000 (10405)
    Int test3 -- A=60000 (5698)

 With LargeInteger loaded:

    int test  -- a=60000 (1152)
    Int test  -- A=60000 (17325)
    Int test2 -- A=60000 (12017)

*/