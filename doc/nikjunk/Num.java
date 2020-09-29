

public class Num  {

    static Integer[] sints = new Integer[65536];

    static {
        System.out.println(""+Runtime.getRuntime().freeMemory());

        for (int i = Short.MIN_VALUE ; i < Short.MAX_VALUE ; i++) {
            sints[i&0xFFFF] = new Integer(i);
        }
    }

/*
    public static Integer create(int x) {
        if (x >= Short.MIN_VALUE && x <= Short.MAX_VALUE) {
            return sints[x&0xFFFF];
        } else {
            return new Integer(x);
        }
    }
*/

    public static Integer create(int x) {
        Integer res = sints[x&0xFFFF];
        if (res.intValue() == x) {
            return res;
        } else {
            return new Integer(x);
        }
    }



    public static void main(String[] args) {

        for (int i = Short.MIN_VALUE ; i < Short.MAX_VALUE ; i++) {
            Integer n = create(i);
            if (n.intValue() != i) {
                throw new Error();
            }
        }

        System.out.println(""+Runtime.getRuntime().freeMemory());

    }

}


class SmallNumbers {
    int value;

    Number add(Number x) {
        if (x instanceof Integer) {
            int other = ((Integer)x).intValue();
            int res = value + other;
            if ((res ^ (res << 1)) >= 0) {
                return Num.create(res);
            }
        }
        return BigInteger.add(value, x);
    }
}


class LargeNumbers {
    int value;
}