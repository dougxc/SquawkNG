package com.sun.squawk.vm;
import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.microedition.io.*;
import com.sun.squawk.vm.ChannelOpcodes;

/**
 * Special channel for input events.
 */
public class Channel1 extends Channel implements KeyListener, MouseListener, MouseMotionListener {

    Vector events = new Vector();
    boolean blocked;


    /*
     * trace
     */
    void trace(String s) {
//        System.out.println(s);
    }


   /*
    * Constructor
    */
    public Channel1(ChannelIO cio, int index, boolean debug) {
        super(cio, index, debug);
    }


   /*
    * execute
    */
    synchronized int execute(
                             int op,
                             int i1,
                             int i2,
                             int i3,
                             int i4,
                             int i5,
                             int i6,
                             Object o1,
                             Object o2,
                             Object o3
                            ) {

        switch (op) {
            case ChannelOpcodes.READLONG: {
                if (events.size() == 0) {
                    blocked = true;
                    return index; // Block the channel
                }
                Long event = (Long)events.firstElement();
                events.removeElementAt(0);
                result = event.longValue();
                break;
            }
            default: throw new RuntimeException("Illegal channel operation "+op);
        }
        return 0;
    }


    /*
     * addEvent
     */
    synchronized void addEvent(long a, long b) {
        events.addElement(new Long(a<<32 | b));
        if (blocked) {
            blocked = false;
            result = 0; // blocked
            cio.unblock(index); // Unblock the channel
        }
    }


    /*
     * keyPressed
     */
    public void keyPressed(KeyEvent e) {
        trace("keyPressed "+e.getKeyCode()+":"+e.getKeyChar());

        if (e.getKeyCode() >= 32 /*|| e.getKeyCode() == 0xA*/) {
            addEvent(1<<16 | e.getID(), e.getKeyCode() << 16 | e.getKeyChar());
        }
    }


    /*
     * keyTyped
     */
    public void keyTyped(KeyEvent e) {
        trace("keyTyped "+e);
        if (e.getKeyChar() >= 32) {
            addEvent(1<<16 | e.getID(), e.getKeyCode() << 16 | e.getKeyChar());
        } else {
            addEvent(1<<16 | 401, e.getKeyChar() << 16 | e.getKeyChar());
        }
    }


    /*
     * keyReleased
     */
    public void keyReleased(KeyEvent e) {
        trace("keyReleased "+e);
        addEvent(1<<16 | e.getID(), e.getKeyCode() << 16 | e.getKeyChar());
    }


    /*
     * mousePressed
     */
    public void mousePressed (MouseEvent e) {
        trace("mousePressed "+e);
        addEvent(2<<16 | e.getID(), e.getX() << 16 | e.getY());
    }


    /*
     * mouseReleased
     */
    public void mouseReleased (MouseEvent e) {
        trace("mouseReleased "+e);
        addEvent(2<<16 | e.getID(), e.getX() << 16 | e.getY());
    }


    /*
     * mouseClicked
     */
    public void mouseClicked (MouseEvent e) {
        trace("mouseClicked "+e);
        addEvent(2<<16 | e.getID(), e.getX() << 16 | e.getY());
    }


    /*
     * mouseEntered
     */
    public void mouseEntered (MouseEvent e) {
    }


    /*
     * mouseExited
     */
    public void mouseExited (MouseEvent e) {
    }


    /*
     * mouseMoved
     */
    public void mouseMoved (MouseEvent e) {
 //       trace("mouseMoved "+e);
 //       addEvent(2<<16 | e.getID(), e.getX() << 16 | e.getY());
    }


    /*
     * mouseDragged
     */
    public void mouseDragged (MouseEvent e) {
        trace("mouseDragged "+e);
        addEvent(2<<16 | e.getID(), e.getX() << 16 | e.getY());
    }

}

