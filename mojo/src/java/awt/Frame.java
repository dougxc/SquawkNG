package java.awt;

import java.io.*;
import java.awt.event.*;
import com.sun.squawk.vm.ChannelOpcodes;

public class Frame extends ID {

    public Frame(String t) {
        Native.execIO3(ChannelOpcodes.FRAME_NEW, id(), 0, 0, 0, 0, 0, t, null, null);
    }

    public void addWindowListener(WindowListener wl) {
    }

    public void setVisible(boolean x) {
        Native.execIO3(ChannelOpcodes.FRAME_SETVISIBLE, id(), x?1:0, 0, 0, 0, 0, null, null, null);
    }

    public void setSize(int x, int y) {
        Native.execIO3(ChannelOpcodes.FRAME_SETSIZE, id(), x, y, 0, 0, 0, null, null, null);
    }

    public void add(Panel p) {
        Dimension d = p.getPreferredSize();
        int x = -1;
        int y = -1;
        if (d != null) {
            x = d.width;
            y = d.height;
        }
        Native.execIO3(ChannelOpcodes.FRAME_ADD, id(), p.id(), x, y, 0, 0, null, null, null);
    }


    // Event system


    static {
        new Thread() {
            public void run() {
                while(true) {
                    long key = Native.execIO3(ChannelOpcodes.FRAME_GETEVENT, 0, 0, 0, 0, 0, 0, null, null, null);

                    int key1 = (int)(key >> 32);
                    int key2 = (int)(key);

                    int op     = (key1 >> 16) & 0xFFFF;
                    int pid    =  key1 & 0xFFFF;
                    Panel p    =  Panel.getPanel(pid);
                    int key2_H = (key2 >> 16) & 0xFFFF;
                    int key2_L =  key2 & 0xFFFF;

                    //System.err.println("%%%%%%%%%%% Got event "+op+":"+pid+":"+key2_H+":"+key2_L);

                    switch(op) {
                        case ChannelOpcodes.AWTEVENT_exit:         System.exit(0);
                        case ChannelOpcodes.AWTEVENT_update:       p.updateEvent(key2_H);                                              break;
                        case ChannelOpcodes.AWTEVENT_paint:        p.paintEvent(key2_H);                                               break;

                        case ChannelOpcodes.AWTEVENT_keyPressed:   if (p.keyListener != null)         p.keyListener.keyPressed(new KeyEvent(key2_H, key2_L));             break;
                        case ChannelOpcodes.AWTEVENT_keyReleased:  if (p.keyListener != null)         p.keyListener.keyReleased(new KeyEvent(key2_H, key2_L));            break;
                        case ChannelOpcodes.AWTEVENT_keyTyped:     if (p.keyListener != null)         p.keyListener.keyTyped(new KeyEvent(key2_H, key2_L));               break;

                        case ChannelOpcodes.AWTEVENT_mousePressed: if (p.mouseListener != null)       p.mouseListener.mousePressed(new MouseEvent(key2_H, key2_L));       break;
                        case ChannelOpcodes.AWTEVENT_mouseReleased:if (p.mouseListener != null)       p.mouseListener.mouseReleased(new MouseEvent(key2_H, key2_L));      break;
                        case ChannelOpcodes.AWTEVENT_mouseClicked: if (p.mouseListener != null)       p.mouseListener.mouseClicked(new MouseEvent(key2_H, key2_L));       break;
                        case ChannelOpcodes.AWTEVENT_mouseExited:  if (p.mouseListener != null)       p.mouseListener.mouseExited(new MouseEvent(key2_H, key2_L));        break;

                        case ChannelOpcodes.AWTEVENT_mouseMoved:   if (p.mouseMotionListener != null) p.mouseMotionListener.mouseMoved(new MouseEvent(key2_H, key2_L));   break;
                        case ChannelOpcodes.AWTEVENT_mouseDragged: if (p.mouseMotionListener != null) p.mouseMotionListener.mouseDragged(new MouseEvent(key2_H, key2_L)); break;

                        case ChannelOpcodes.AWTEVENT_focusGained:  if (p.focusListener != null)       p.focusListener.focusGained(null);                                  break;

                        default:                    System.out.println("Bad event "+op+":"+pid+":"+key2_H+":"+key2_L);  break;
                    }
                }
            }
        }.start();
    }

}



