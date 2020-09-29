public abstract class $Object {
    public static $Object $null  = null;
    public static $True   $true  = new $True();
    public static $False  $false = new $False();

}

public abstract class $Boolean extends $Object {
}

public class $True extends $Boolean {
}

public class $False extends $Boolean {
}


public abstract class $Integer extends $Object {

    public boolean is$SmallInteger() { return this instanceof is$SmallInteger; }
    public int value()               { throw new Error(); }


    public abstract $Integer add($Integer x);    public abstract $Integer add(int x);
/*
    public abstract $Integer sub($Integer x);    public abstract $Integer sub(int x);
    public abstract $Integer mul($Integer x);    public abstract $Integer mul(int x);
    public abstract $Integer div($Integer x);    public abstract $Integer div(int x);
    public abstract $Integer rem($Integer x);    public abstract $Integer rem(int x);

    public abstract $Integer sll($Integer x);    public abstract $Integer sll(int x);
    public abstract $Integer srl($Integer x);    public abstract $Integer srl(int x);
    public abstract $Integer sra($Integer x);    public abstract $Integer sra(int x);

    public abstract $Integer or($Integer x);     public abstract $Integer or(int x);
    public abstract $Integer and($Integer x);    public abstract $Integer and(int x);
    public abstract $Integer xor($Integer x);    public abstract $Integer xor(int x);
*/
    public abstract $Boolean eq($Integer x);     public abstract $Boolean eq(int x);
    public abstract $Boolean ne($Integer x);     public abstract $Boolean ne(int x);
    public abstract $Boolean lt($Integer x);     public abstract $Boolean lt(int x);
    public abstract $Boolean le($Integer x);     public abstract $Boolean le(int x);
    public abstract $Boolean gt($Integer x);     public abstract $Boolean gt(int x);
    public abstract $Boolean ge($Integer x);     public abstract $Boolean ge(int x);

}

class $SmallInteger {


    static $SmallInteger[] sints = new $SmallInteger[65536];

    static {
        System.out.println(""+Runtime.getRuntime().freeMemory());

        for (int i = -32768 ; i < 32767 ; i++) {
            sints[i&0xFFFF] = new $SmallInteger(i);
        }
    }

    public static $SmallInteger create(int x) {
        $SmallInteger res = sints[x&0xFFFF];
        if (res.eq(x)) {
            return res;
        } else {
            return new $SmallInteger(x);
        }
    }




    private final int value = x;

    public $SmallInteger(int x) {
        value = x;
    }


    public int value() {
        return value;
    }


    public $Boolean eq(int x) { return value == x ? $true : $false; }
    public $Boolean ne(int x) { return value != x ? $true : $false; }
    public $Boolean lt(int x) { return value <  x ? $true : $false; }
    public $Boolean le(int x) { return value <= x ? $true : $false; }
    public $Boolean gt(int x) { return value >  x ? $true : $false; }
    public $Boolean ge(int x) { return value >= x ? $true : $false; }

    public $Boolean eq($Integer x) { return x.is$SmallInteger() ? eq(x.value()) : x.eq(value); }
    public $Boolean ne($Integer x) { return x.is$SmallInteger() ? ne(x.value()) : x.ne(value); }
    public $Boolean lt($Integer x) { return x.is$SmallInteger() ? lt(x.value()) : x.ge(value); }
    public $Boolean le($Integer x) { return x.is$SmallInteger() ? le(x.value()) : x.gt(value); }
    public $Boolean gt($Integer x) { return x.is$SmallInteger() ? gt(x.value()) : x.le(value); }
    public $Boolean ge($Integer x) { return x.is$SmallInteger() ? ge(x.value()) : x.lt(value); }

    public $Integer add_int(int x) {
        int res = value + x;
        return ((res ^ (res << 1)) >= 0) ? createSmallInt(res) : createLargeInt(res);
    }

    public $Integer add($Integer x) { return x.is$SmallInteger() ? add(x.value()) : x.add(value);  }
    public $Integer sub($Integer x) { return x.is$SmallInteger() ? sub(x.value()) : x.rsub(value); }
    public $Integer mul($Integer x) { return x.is$SmallInteger() ? mul(x.value()) : x.mul(value);  }
    public $Integer div($Integer x) { return x.is$SmallInteger() ? div(x.value()) : x.rdiv(value); }
    public $Integer rem($Integer x) { return x.is$SmallInteger() ? rem(x.value()) : x.rrem(value); }




    public $Integer add($Integer x) { try { add(x.value()); } catch (LargeValueException ex) { x.add(value); } }





    public $Integer add($Integer x) { return x.add_int(value); }


}


class $LargeInteger {
    $LargeInteger(int x) {
    }

    $Integer add(int x) {
    }
}