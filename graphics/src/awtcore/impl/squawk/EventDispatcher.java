package awtcore.impl.squawk;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import com.sun.squawk.vm.ChannelOpcodes;

public class EventDispatcher extends Thread {

    private static void post(AWTEvent e) {
//System.err.println("Posting event: "+e);
        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(e);
    }

    public void run() {
        while(true) {
            long key = getKeystroke();

            int key1 = (int)(key >> 32);
            int key2 = (int)(key);

            int key1_H = (key1 >> 16) & 0xFFFF;
            int key1_L =  key1 & 0xFFFF;
            int key2_H = (key2 >> 16) & 0xFFFF;
            int key2_L =  key2 & 0xFFFF;

//System.err.println("Got event "+key1_H+":"+key1_L+":"+key2_H+":"+key2_L);

            if (key1_H == 0) {
                if(Toolkit.getTopWindow() != null) {
                    Toolkit.getTopWindow().repaint();
                }
            } else if (key1_H == 1) {
                post(new KeyEvent(null, key1_L, 0, 0, key2_H, (char)key2_L));
            } else if (key1_H == 2) {
                post(new MouseEvent (null, key1_L, 0, 0, key2_H, key2_L, 0, false));
            } else if (key1_H == 3) {
                System.exit(0);
            } else {
                System.out.println("Bad event "+key1_H+":"+key1_L+":"+key2_H+":"+key2_L);
            }
        }
    }

    private long getKeystroke() {
        try {
            return Native.execIO(1, ChannelOpcodes.READLONG, 0, 0, 0, 0, 0, 0, null, null, null);
        } catch(IOException ex) {
            ex.printStackTrace();
            throw new RuntimeException("I/O exception reading channel 1 "+ex);
        }
    }

}