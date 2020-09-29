
import java.io.*;
import java.util.*;


public class StreamProducer extends Thread {
    OutputStream out;
    InputStream in;

    StreamProducer(OutputStream out, InputStream in) {
        this.in = in;
        this.out = out;
    }

    public void run() {
        try {
            InputStreamReader isr = new InputStreamReader(in);
            BufferedReader br = new BufferedReader(isr);
            String line=null;
            while ((line = br.readLine()) != null) {
//System.err.println("StreamProducer: "+line);
                out.write(line.getBytes());
                out.flush();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        //out.println("StreamProducer terminated");
    }
}

