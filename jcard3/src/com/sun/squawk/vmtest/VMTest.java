package com.sun.squawk.vmtest;
public class VMTest extends A{
    public byte virtualTest(){
        return (byte)4;
    }
    
    public void test(){
        if(this.arith() != 5)
            throw (new RuntimeException("Arith failed"));
        
        if(A.stat1() !=1)
            throw (new RuntimeException("Static failed"));

        if(this.virtualTest() != 4)
            throw (new RuntimeException("Virtual failed 1"));

        if(super.virtual1Test()!=5)
            throw (new RuntimeException("Virtual super failed"));

        if(this.invokeInt() !=5)
            throw (new RuntimeException("Interface failed"));

        if(this.const1Test()!=4080)
            throw (new RuntimeException("const failed"));

        if(this.arrayTest()!=200)
            throw (new RuntimeException("array failed"));

        if(this.fieldTest()!=120)
            throw (new RuntimeException("field failed. field="));
        
        if(A.staticFieldTest()!=100)
            throw (new RuntimeException("static field failed"));

        if(this.controlTest()!=10)
            throw (new RuntimeException("control failed"));

        if(!this.testString())
            throw (new RuntimeException("String failed"));
        
/*if[SYSTEM.STREAMS]*/
        System.out.println("All tests passed");
/*end[SYSTEM.STREAMS]*/
    }

    public static void main(String argv[]){
        VMTest t=new VMTest();
        t.test();
    }
}
