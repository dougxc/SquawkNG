package com.sun.squawk.vm;
import com.sun.squawk.util.*;
import java.util.*;
import com.sun.squawk.vm.ChannelOpcodes;

public class ChannelIO extends SquawkConstants {

    Memory  mem;
    boolean debug;
    IntHashtable channels = new IntHashtable();
    int nextChan = 4; // Channel 0, 1, 2, and 3 are reserved
    Channel1 chan1;
    Channel2 chan2;
    Channel3 chan3;
    Vector events = new Vector();


    /*
     * Constructor
     */
    public ChannelIO(boolean debug) {
        this.debug = debug;
        chan1 = new Channel1(this, 1, debug);
        chan2 = new Channel2(this, 2, debug, chan1);
        chan3 = new Channel3(this, 2, debug);

        channels.put(1, chan1);
        channels.put(2, chan2);
        channels.put(3, chan3);
    }

    /*
     * Create channel
     */
    Channel createChannel() {
        while (channels.get(nextChan) != null) {
            nextChan++;
        }
        int chan = nextChan++;
        Channel res = new Channel(this, chan, debug);
        channels.put(chan, res);
        return res;
    }


    /**
     * Execute a native function.
     * @param chan
     * @param op
     * @param i1
     * @param i2
     * @param i3
     * @param i4
     * @param i5
     * @param i6
     * @param o1
     * @param o2
     * @param o3
     * @return 0 if the function executed without blocking, a positive number 'x'
     * if the function would block where 'x' identifys the channel that needs to
     * be blocked. If the function wants to cause System.exit, then -1 is returned.
     *
     */
    int execute(
                int chan,
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

        Channel c = (chan == 0 && op == ChannelOpcodes.GETCHANNEL) ? createChannel() : (Channel)channels.get(chan);
        if (c == null) {
            return EXNO_NoConnection;
        }
        int res;
        try {
            res = c.execute(op, i1, i2, i3, i4, i5, i6, o1, o2, o3);
        } catch (Throwable t) {
            t.printStackTrace();
            return EXNO_NoConnection;
        }

        if (chan == 0 && op == ChannelOpcodes.FREECHANNEL) {
            channels.remove(chan);
        }

        return (chan == 0 && op == ChannelOpcodes.GETCHANNEL) ? c.index : res;
    }

    /*
     * result
     */
    long result(int chan) {
        Channel c = (Channel)channels.get(chan);
        if (c == null) {
            return EXNO_NoConnection;
        }
        return c.result();
    }


    /*
     * close
     */
    void close() {
        Enumeration e = channels.elements();
        while (e.hasMoreElements()) {
            Channel c = (Channel)e.nextElement();
            if (c != null) {
                c.close();
            }
        }
    }


    /*
     * waitFor
     */
    synchronized void waitFor(long time) {
        try { wait(time); } catch(InterruptedException ex) {}
    }


    /*
     * unblock
     */
    synchronized void unblock(int event) {
        events.addElement(new Integer(event));
        notify();
    }


    /*
     * getEvent
     */
    synchronized int getEvent() {
        if (events.size() > 0) {
            Integer event = (Integer)events.firstElement();
            events.removeElementAt(0);
            return event.intValue();
        } else {
            return 0;
        }
    }



}


