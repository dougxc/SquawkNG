

public class StringLocker {
    public static void main(String[] args) throws Exception{
        String name = args[0];
        int delay = Integer.parseInt(args[1]);
        synchronized(String.class) {
            long start = System.currentTimeMillis();
            int rem;
            do {
                rem = delay - (int)((System.currentTimeMillis() - start) / 1000);
                System.out.println(name + " locking String for " + rem + " more seconds...");
            } while(rem > 0);
        }
        System.out.println(name + " unlocked String");
    }
}