package java.lang;

public class MinHeap {
    static String[] getNull() {
        return null;
    }
    public static void main(String[] args) {
        args = getNull();
        Native.gc();
        Native.setMinimumHeapMode(true);
        Native.getHeapHighWaterMark();
    }
}


