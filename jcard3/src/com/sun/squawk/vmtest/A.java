package com.sun.squawk.vmtest;

public class A implements I{
    public byte arith(){
        return (byte)((1 * 2)+3);
    }
    public static short stat1(){
        return 1;
    }
    public short stat1Test(){
        return A.stat1();
    }
    public byte virtual1Test(){
        return this.arith();
    }
    public byte invokeInt(){
        I i=new A();
        return i.arith();
    }
    public short const1Test(){
        int i=0x000fffff;
        byte b=0x0f;
        short s=0x0fff;
        return (short)(i*b+s);
    }
    public short arrayTest(){
        byte[] b=new byte[100];
        b[0]=100;
        return (short)(b[0]+b.length);
    }
    protected byte f1=100;
    public byte fieldTest(){
        byte f=f1;
        f1=20;
        byte f2=(byte)(f+f1);
        return f2;
    }
    static protected byte f2=50;
    public static byte staticFieldTest(){
        byte f=f2;
        f2=50;
        return (byte)(f+f2);
    }

    public short controlTest(){
        short s=0;
        for(int i=0;i<5;i++){
            s+=i;
        }
        return s;
    }

    public boolean testString(){
        String s="test";
        String s1="test";
        return (s.equals(s1));
    }
}
