public class Test3 {

    public static void main(String[] args) {
        int good = 0, bad = 0;
        boolean[][] a = new boolean[16][16];
        for (int i = -8 ; i < 8 ; i++) {
            for (int j = -8 ; j < 8 ; j++) {
                int res = i + j;
                if (res < -8 || res >= 8) {
                    printbad(i, j, res);
                    bad++;
                } else {
                    good++;
                    a[8+i][8+j] = true;
                }
            }
        }
        System.out.println("good "+good+" bad "+bad);

        for (int i = 0 ; i < 16 ; i++) {
            for (int j = 0 ; j < 16 ; j++) {
                System.out.print((a[i][j]) ? "* " : "  ");
            }
            System.out.println();
        }
    }

    static void printbad(int i, int j, int res) {
        System.out.print(""+i+" + "+j+" = "+res);
        System.out.println("\t"+bits(i)+"\t"+bits(j)+"\t"+bits(res));
    }

    static String bits(int i) {
        i &= 0xC;
        i >>= 2;
        switch(i) {
            case 0: return "00";
            case 1: return "01";
            case 2: return "10";
            case 3: return "11";
        }
        return "error";
    }

}