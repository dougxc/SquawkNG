
import java.io.*;
import java.util.*;


public class StreamGobbler extends Thread {
    InputStream is;
    PrintStream out;
    String prefix;

    StreamGobbler(InputStream is, PrintStream out, String prefix) {
        this.is = is;
        this.out = out;
        this.prefix = prefix;
    }

    public void run() {
        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line=null;
            while ((line = br.readLine()) != null) {
                if (prefix != null) {
                    out.print(prefix);
                }
                out.println(line);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        //out.println("StreamGobbler terminated");
    }
}

