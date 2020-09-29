package com.sun.squawk.vm;
import java.io.*;
import javax.microedition.io.*;
import com.sun.squawk.vm.ChannelOpcodes;

public class Channel extends SquawkConstants {

    int index;
    long result;
    Connection con;
    DataInputStream dis;
    DataOutputStream dos;
    ChannelIO cio;

    // debugging
    DataOutputStream inLog;
    DataOutputStream outLog;

   /*
    * Constructor
    */
    public Channel(ChannelIO cio, int index, boolean debug) {
        this.cio = cio;
        this.index = index;
        if (debug) {
            try {
                this.inLog = new DataOutputStream(new FileOutputStream("channel"+index+".input"));
                this.outLog = new DataOutputStream(new FileOutputStream("channel"+index+".output"));
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    /**
     * Execute a native function.
     * @param op
     * @param i1
     * @param i2
     * @param i3
     * @param iij
     * @param i5
     * @param i6 lkjlk
     * @param o1
     * @param o2
     * @param o3
     * @return 0 if the function executed without blocking, a positive number 'x'
     * if the function would block where 'x' identifys the channel that needs to
     * be blocked. If the function wants to cause System.exit, then -1 is returned.
     *
     */
    int execute(int op,
                int i1,
                int i2,
                int i3,
                int i4,
                int i5,
                int i6,
                Object o1,
                Object o2,
                Object o3) {
        try {
            switch (op) {
                case ChannelOpcodes.GETCHANNEL:
                case ChannelOpcodes.FREECHANNEL: {
                    break;
                }

                case ChannelOpcodes.OPEN: {
                    String prot = (String)o1;
                    String name = (String)o2;
                    int    mode = i1;
                    int    tmo  = i2;
//System.out.println("ChannelOpcodes.OPEN "+name);
                    con = Connector.open(prot+":"+name, mode, tmo==1);
                    break;
                }

                case ChannelOpcodes.CLOSE: {
                    con.close();
                    break;
                }

                case ChannelOpcodes.ACCEPT: { // This is not really going to work because the whole VM will block
                    StreamConnection sc = ((StreamConnectionNotifier)con).acceptAndOpen();
                    Channel chan = cio.createChannel();
                    chan.con = sc;
                    result = chan.index;
                    break;
                }

                case ChannelOpcodes.OPENINPUT: {
                    dis = ((InputConnection)con).openDataInputStream();
                    break;
                }

                case ChannelOpcodes.CLOSEINPUT: {
                    dis.close();
                    break;
                }

                case ChannelOpcodes.READBYTE: {
                    result = dis.read();
                    if (inLog != null) {
                        inLog.writeByte((byte)result);
                    }
//System.out.println("ChannelOpcodes.READBYTE: "+result+(!Character.isISOControl((char)result) ? " ('"+(char)result+"')" : ""));
                    break;
                }

                case ChannelOpcodes.READSHORT: {
                    result = dis.readShort();
                    if (inLog != null) {
                        inLog.writeShort((byte)result);
                    }
//System.out.println("ChannelOpcodes.READSHORT: "+result);
                    break;
                }

                case ChannelOpcodes.READINT: {
                    result = dis.readInt();
                    if (inLog != null) {
                        inLog.writeInt((byte)result);
                    }
//System.out.println("ChannelOpcodes.READINT: "+result);
                    break;
                }

                case ChannelOpcodes.READLONG: {
                    result = dis.readLong();
                    if (inLog != null) {
                        inLog.writeLong(result);
                    }
//System.out.println("ChannelOpcodes.READLONG: "+mem.getLong(buf, 0));
                    break;
                }

                case ChannelOpcodes.READBUF: {
                    byte[] buf = (byte[])o3;
                    int off = i1;
                    int len = i2;
                    result = dis.read(buf, off, len);
                    if (inLog != null) {
                        for (int i = off ; i < off+len ; i++) {
                            inLog.writeByte(buf[i]);
                        }
                    }
//System.out.println("ChannelOpcodes.READBUF: "+lth+" bytes read");
                    break;
                }

                case ChannelOpcodes.SKIP: {
                    long l = ((long)i1 << 32) + (i2 & 0xFFFFFFFFL);
                    result = dis.skip(l);
                    break;
                }

                case ChannelOpcodes.AVAILABLE: {
                    result = dis.available();
                    break;
                }

                case ChannelOpcodes.MARK: {
                    int limit = i1;
                    dis.mark(limit);
                    break;
                }

                case ChannelOpcodes.RESET: {
                    dis.reset();
                    break;
                }

                case ChannelOpcodes.MARKSUPPORTED: {
                    result = dis.markSupported() ? 1 : 0;
                    break;
                }

                case ChannelOpcodes.OPENOUTPUT: {
                    dos = ((OutputConnection)con).openDataOutputStream();
                    break;
                }

                case ChannelOpcodes.FLUSH: {
                    dos.flush();
                    break;
                }

                case ChannelOpcodes.CLOSEOUTPUT: {
                    dos.close();
                    break;
                }

                case ChannelOpcodes.WRITEBYTE: {
                    int ch = i1;
                    dos.write(ch);
                    if (outLog != null) {
                        outLog.writeByte(ch);
                    }
//System.out.println("ChannelOpcodes.WRITEBYTE: "+ch+(!Character.isISOControl((char)ch) ? " ('"+(char)ch+"')" : ""));
                    break;
                }

                case ChannelOpcodes.WRITESHORT: {
                    int val = i1;
                    dos.writeShort(val);
                    if (outLog != null) {
                        outLog.writeShort(val);
                    }
//System.out.println("ChannelOpcodes.WRITESHORT: "+val);
                    break;
                }

                case ChannelOpcodes.WRITEINT: {
                    int val = i1;
                    dos.writeInt(val);
                    if (outLog != null) {
                        outLog.writeInt(val);
                    }
//System.out.println("ChannelOpcodes.WRITEINT: "+val);
                    break;
                }

                case ChannelOpcodes.WRITELONG: {
                    long l = ((long)i1 << 32) + (i2 & 0xFFFFFFFFL);
                    dos.writeLong(l);
                    if (outLog != null) {
                        outLog.writeLong(l);
                    }
                    break;
                }

                case ChannelOpcodes.WRITEBUF: {
                    byte[] buf = (byte[])o1;
                    int off = i1;
                    int len = i2;
                    dos.write(buf, off, len);
                    if (outLog != null) {
                        for (int i = off; i < off+len ; i++) {
                            outLog.writeByte(buf[i]);
                        }
                    }
//System.out.println("ChannelOpcodes.WRITEBUF: "+len+" bytes written ("+new String(buf, off, len)+")");
                    break;
                }

            }
            return 0;
        } catch(ConnectionNotFoundException ex) {
            return EXNO_NoConnection;
        } catch(EOFException ex) {
            return EXNO_EOFException;
        } catch(IOException ex) {
            return EXNO_IOException;
        } catch(ClassCastException ex) {
            return EXNO_IOException;
        } catch(NullPointerException ex) {
            return EXNO_IOException;
        }
    }

   /*
    * result
    */
    long result() {
//System.err.println("result="+result);
        return result;
    }

    /*
     * close
     */
    void close() {
        if (inLog != null) {
            try {
                inLog.close();
            } catch (IOException ioe) {
            }
        }
        if (outLog != null) {
            try {
                outLog.close();
            } catch (IOException ioe) {
            }
        }
    }
}

