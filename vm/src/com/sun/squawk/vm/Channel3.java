package com.sun.squawk.vm;
import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.microedition.io.*;
import com.sun.squawk.util.*;
import com.sun.squawk.vm.ChannelOpcodes;

/**
 * Special channel for mojo graphics.
 */
public class Channel3 extends Channel {

    static final boolean TRACE = false;

    IntHashtable objects  = new IntHashtable();
    IntHashtable graphics = new IntHashtable();

    Vector events = new Vector();
    boolean blocked;

    int fillarc_x;
    int fillarc_y;
    int fillarc_w;
    int fillarc_h;

    /*
     * trace
     */
    void trace(String s) {
        if (TRACE) {
            System.out.println(s);
        }
    }

    /*
     * getGraphics
     */
     Graphics getGraphics(int id) {
         Graphics g = (Graphics)graphics.get(id);
         if (g == null) {
             Object o = get(id);
             if (o instanceof C3Panel) {
                  g = ((C3Panel)o).getImage().getGraphics();
             } else {
                  g = ((Image)o).getGraphics();
             }
             graphics.put(id, g);
         }
         return g;
     }

    /*
     * get
     */
    Object get(int x) {
        Object res = objects.get(x);
        if (res == null) {
            throw new NullPointerException();
        }
        return res;
    }

    /*
     * put
     */
    void put(int x, Object o) {
        objects.put(x, o);
    }

    /*
     * Constructor
     */
    public Channel3(ChannelIO cio, int index, boolean debug) {
        super(cio, index, debug);
    }

    /*
     * addEvent
     */
    synchronized void addEvent(long op, long pid, long hi, long lo) {
        trace("[addEvent "+op+" "+pid+" "+hi+" "+lo+"]");
        events.addElement(new Long((op<<48) | (pid<<32) | (hi<<16) | lo));
        if (blocked) {
            blocked = false;
            result = 0; // blocked
            cio.unblock(index); // Unblock the channel
        }
    }

   /*
    * execute
    */
    synchronized int execute(
                 int op,
                 int id,
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

            case ChannelOpcodes.FRAME_GETEVENT: {
                if (events.size() == 0) {
                    blocked = true;
                    return index; // Block the channel
                }
                Long event = (Long)events.firstElement();
                events.removeElementAt(0);
                result = event.longValue();
                break;
            }
            case ChannelOpcodes.FRAME_NEW: {
                trace("["+id+"] ChannelOpcodes.FRAME_NEW "+o1);
                String s = (String)o1;
                Frame  f = new Frame(s);
                put(id, f);
                f.addWindowListener(new WindowAdapter() {
                    public void windowClosing(WindowEvent e) {
                        addEvent(ChannelOpcodes.AWTEVENT_exit, 0, 0, 0);
                    }
                });
                break;
            }
            case ChannelOpcodes.FRAME_SETVISIBLE: {
                trace("["+id+"] ChannelOpcodes.FRAME_SETVISIBLE");
                Frame f = (Frame)get(id);
                f.setVisible(i2==1);
                break;
            }
            case ChannelOpcodes.FRAME_SETSIZE: {
                trace("["+id+"] ChannelOpcodes.FRAME_SETSIZE");
                Frame f = (Frame)get(id);
                f.setSize(i2, i3);
                break;
            }
            case ChannelOpcodes.FRAME_ADD: {
                trace("["+id+"] ChannelOpcodes.FRAME_ADD "+i2+" ("+i3+" "+i4+")");
                Frame f   = (Frame)get(id);
                C3Panel p = (C3Panel)get(i2);
                p.setDim(i3, i4);
                f.add(p);
                break;
            }
            case ChannelOpcodes.FONT_NEW: {
                trace("["+id+"] ChannelOpcodes.FONT_NEW "+o1+" "+i2+" "+i3);
                String s  = (String)o1;
                Font f = new Font(s, i2, i3);
                put(id, f);
                break;
            }
            case ChannelOpcodes.FONT_GETSIZE: {
                Font f = (Font)get(id);
                result = f.getSize();
                trace("["+id+"] ChannelOpcodes.FONT_GETSIZE => "+result);
                break;
            }
            case ChannelOpcodes.FONTMETRICS_CHARWIDTH: {
                FontMetrics f = (FontMetrics)get(id);
                result = f.charWidth((char)i2);
                trace("["+id+"] ChannelOpcodes.FONTMETRICS_CHARWIDTH "+i2+" => "+result);
                break;
            }
            case ChannelOpcodes.FONTMETRICS_STRINGWIDTH: {
                String s = (String)o1;
                FontMetrics f = (FontMetrics)get(id);
                result = f.stringWidth(s);
                trace("["+id+"] ChannelOpcodes.FONTMETRICS_STRINGWIDTH "+o1+" => "+result);
                break;
            }
            case ChannelOpcodes.FONTMETRICS_GETHEIGHT: {
                FontMetrics f = (FontMetrics)get(id);
                result = f.getHeight();
                trace("["+id+"] ChannelOpcodes.FONTMETRICS_GETHEIGHT => "+result);
                break;
            }
           case ChannelOpcodes.GRAPHICS_SETCOLOR: {
                trace("{"+id+"} ChannelOpcodes.GRAPHICS_SETCOLOR "+i2);
                Graphics g = getGraphics(id);
                g.setColor(new Color(i2));
                break;
            }
            case ChannelOpcodes.GRAPHICS_GETFONT: {
                trace("{"+id+"} ChannelOpcodes.GRAPHICS_GETFONT => "+i2);
                Graphics g = getGraphics(id);
                Font f = g.getFont();
                put(i2, f);
                break;
            }
            case ChannelOpcodes.GRAPHICS_GETFONTMETRICS: {
                trace("{"+id+"} ChannelOpcodes.GRAPHICS_GETFONTMETRICS => "+i2);
                Graphics g = getGraphics(id);
                FontMetrics f = g.getFontMetrics();
                put(i2, f);
                break;
            }
            case ChannelOpcodes.GRAPHICS_DRAWIMAGE: {
                trace("{"+id+"} ChannelOpcodes.GRAPHICS_DRAWIMAGE "+i2+" "+i3+" "+i4);
                Graphics g = getGraphics(id);
                Image   im = (Image)get(i2);
                g.drawImage(im, i3, i4, null);
                break;
            }
            case ChannelOpcodes.GRAPHICS_DRAWSTRING: {
                trace("{"+id+"} ChannelOpcodes.GRAPHICS_DRAWSTRING "+o1+" "+i2+" "+i3);
                Graphics g = getGraphics(id);
                String   s = (String)o1;
                g.drawString(s, i2, i3);
                break;
            }
            case ChannelOpcodes.GRAPHICS_FILLARC1: {
                trace("{"+id+"} ChannelOpcodes.GRAPHICS_FILLARC1 "+i2+" "+i3+" "+i4+" "+i5);
                fillarc_x = i2;
                fillarc_y = i3;
                fillarc_w = i4;
                fillarc_h = i5;
                break;
            }
            case ChannelOpcodes.GRAPHICS_FILLARC2: {
                trace("{"+id+"} ChannelOpcodes.GRAPHICS_FILLARC2 "+i2+" "+i3);
                Graphics g = getGraphics(id);
                int ba = i2;
                int ea = i3;
                g.fillArc(fillarc_x, fillarc_y, fillarc_w, fillarc_h, ba, ea);
                break;
            }
            case ChannelOpcodes.GRAPHICS_DRAWLINE: {
                trace("{"+id+"} ChannelOpcodes.GRAPHICS_DRAWLINE "+i2+" "+i3+" "+i4+" "+i5);
                Graphics g = getGraphics(id);
                g.drawLine(i2, i3, i4, i5);
                break;
            }
            case ChannelOpcodes.GRAPHICS_DRAWRECT: {
                trace("{"+id+"} ChannelOpcodes.GRAPHICS_DRAWRECT "+i2+" "+i3+" "+i4+" "+i5);
                Graphics g = getGraphics(id);
                g.drawRect(i2, i3, i4, i5);
                break;
            }
            case ChannelOpcodes.GRAPHICS_FILLRECT: {
                trace("{"+id+"} ChannelOpcodes.GRAPHICS_FILLRECT "+i2+" "+i3+" "+i4+" "+i5);
                Graphics g = getGraphics(id);
                g.fillRect(i2, i3, i4, i5);
                break;
            }
            case ChannelOpcodes.GRAPHICS_FILLPOLY: {
                trace("{"+id+"} ChannelOpcodes.GRAPHICS_FILLPOLY "+asString((int[])o1, i2)+" "+asString((int[])o2, i2));
                Graphics g = getGraphics(id);
                g.fillPolygon((int[])o1, (int[])o2, i2);
                break;
            }
            case ChannelOpcodes.GRAPHICS_SETCLIP: {
                trace("{"+id+"} ChannelOpcodes.GRAPHICS_SETCLIP "+i2+" "+i3+" "+i4+" "+i5);
                Graphics g = getGraphics(id);
                g.setClip(i2, i3, i4, i5);
                break;
            }
            case ChannelOpcodes.GRAPHICS_TRANSLATE: {
                trace("{"+id+"} ChannelOpcodes.GRAPHICS_TRANSLATE "+i2+" "+i3);
                Graphics g = getGraphics(id);
                g.translate(i2, i3);
                break;
            }
            case ChannelOpcodes.IMAGE_GETGRAPHICS: {
                Image im = (Image)get(id);
                result = id;
                trace("["+id+"] ChannelOpcodes.IMAGE_GETGRAPHICS => "+result);
                break;
            }
            case ChannelOpcodes.IMAGE_GETWIDTH: {
                Image im = (Image)get(id);
                result = im.getWidth(null);
                trace("["+id+"] ChannelOpcodes.IMAGE_GETWIDTH => "+result);
                break;
            }
            case ChannelOpcodes.IMAGE_GETHEIGHT: {
                Image im = (Image)get(id);
                result = im.getHeight(null);
                trace("["+id+"] ChannelOpcodes.IMAGE_GETHEIGHT => "+result);
                break;
            }
            case ChannelOpcodes.MEDIATRACKER_WAITFOR: {
                trace("["+id+"] ChannelOpcodes.MEDIATRACKER_WAITFOR "+i2);
                Panel p  = (Panel)get(id);
                Image im = (Image)get(i2);
                MediaTracker t = new MediaTracker(p);
                t.addImage(im, 0);
                try { t.waitForID(0); } catch(InterruptedException ex) {};
                break;
            }
            case ChannelOpcodes.PANEL_NEW: {
                trace("["+id+"] ChannelOpcodes.PANEL_NEW");
                Panel p = C3Panel.create(this, id);
                put(id, p);
                break;
            }
            case ChannelOpcodes.PANEL_REPAINT: {
                trace("["+id+"] ChannelOpcodes.PANEL_REPAINT");
                Panel p  = (Panel)get(id);
                p.repaint();
                break;
            }
            case ChannelOpcodes.PANEL_REPAINT2: {
                trace("["+id+"] ChannelOpcodes.PANEL_REPAINT2 "+i2+" "+i3+" "+i4+" "+i5+" "+i6);
                Panel p  = (Panel)get(id);
                p.repaint(i2, i3, i4, i5, i6);
                break;
            }
            case ChannelOpcodes.PANEL_CREATEIMAGE: {
                trace("["+id+"] ChannelOpcodes.PANEL_CREATEIMAGE "+i3+" "+i4+" => "+i2);
                Panel p  = (Panel)get(id);
                Image im = p.createImage(i3, i4);
                put(i2, im);
                break;
            }
            case ChannelOpcodes.PANEL_REMOVE: {
                trace("["+id+"] ChannelOpcodes.PANEL_REMOVE "+i2);
                Panel p  = (Panel)get(id);
                p.remove(i2);
                break;
            }
            case ChannelOpcodes.PANEL_INVALIDATE: {
                trace("["+id+"] ChannelOpcodes.PANEL_INVALIDATE");
                Panel p  = (Panel)get(id);
                p.invalidate();
                break;
            }
            case ChannelOpcodes.PANEL_VALIDATE: {
                trace("["+id+"] ChannelOpcodes.PANEL_VALIDATE");
                Panel p  = (Panel)get(id);
                p.validate();
                break;
            }
            case ChannelOpcodes.PANEL_REQUESTFOCUS: {
                trace("["+id+"] ChannelOpcodes.PANEL_REQUESTFOCUS");
                Panel p  = (Panel)get(id);
                p.requestFocus();
                break;
            }
            case ChannelOpcodes.PANEL_ADD: {
                trace("["+id+"] ChannelOpcodes.PANEL_ADD "+i2+" "+i3+" ("+i4+" "+i5+")");
                C3Panel p  = (C3Panel)get(id);
                C3Panel p2 = (C3Panel)get(i2);
                p2.setDim(i4, i5);
                p.add(p2, i3);
                break;
            }
            case ChannelOpcodes.PANEL_ADD2: {
                trace("["+id+"] ChannelOpcodes.PANEL_ADD2 "+i2+" "+o1+" ("+i3+" "+i4+")");
                C3Panel  p  = (C3Panel)get(id);
                C3Panel  p2 = (C3Panel)get(i2);
                String s  = (String)o1;
                p2.setDim(i3, i4);
                p.add(p2, s);
                break;
            }
            case ChannelOpcodes.PANEL_ADDLABEL: {
                trace("["+id+"] ChannelOpcodes.PANEL_ADDLABEL "+o1);
                Panel  p = (Panel)get(id);
                String s = (String)o1;
                p.add(new Label(s));
                break;
            }
            case ChannelOpcodes.PANEL_SETBACKGROUND: {
                trace("["+id+"] ChannelOpcodes.PANEL_SETBACKGROUND "+i2);
                Panel  p = (Panel)get(id);
                p.setBackground(new Color(i2));
                break;
            }
            case ChannelOpcodes.PANEL_SETBORDERLAYOUT: {
                trace("["+id+"] ChannelOpcodes.PANEL_SETBORDERLAYOUT");
                Panel  p = (Panel)get(id);
                p.setLayout(new BorderLayout());
                break;
            }
            case ChannelOpcodes.PANEL_GETHEIGHT: {
                Panel  p = (Panel)get(id);
                result = p.getHeight();
                trace("["+id+"] ChannelOpcodes.PANEL_GETHEIGHT => "+result);
                break;
            }
            case ChannelOpcodes.PANEL_GETWIDTH: {
                Panel  p = (Panel)get(id);
                result = p.getWidth();
                trace("["+id+"] ChannelOpcodes.PANEL_GETWIDTH => "+result);
                break;
            }
            case ChannelOpcodes.PANEL_GETFONTMETRICS: {
                trace("["+id+"] ChannelOpcodes.PANEL_GETFONTMETRICS "+i2+" => "+i3);
                Panel  p = (Panel)get(id);
                Font   f = (Font)get(i2);
                FontMetrics fm = p.getFontMetrics(f);
                put(i3, fm);
                break;
            }
            case ChannelOpcodes.TOOLKIT_CREATEIMAGE: {
                trace("["+id+"] ChannelOpcodes.TOOLKIT_CREATEIMAGE "+o1+" "+i2+" "+i2);
                Image im = Toolkit.getDefaultToolkit().createImage((byte[])o1, i2, i3);
                put(id, im);
                break;
            }

            default: throw new RuntimeException("Illegal channel operation "+op);
        }

        return 0;
    }

    String asString(int[] x, int lth) {
         String r = "[";
         for (int i = 0 ; i < lth ; i++) {
             if (i != 0) {
                  r = r.concat(",");
             }
             r = r.concat(""+x[i]);
         }
         r = r.concat("]");
         return r;
    }
}


class C3Panel extends Panel implements KeyListener, MouseListener, MouseMotionListener, FocusListener {

    Channel3 c3;
    int id;
    Dimension dim;
    private Image image;

    static C3Panel create(Channel3 c3, int id) {
        C3Panel p = new C3Panel(c3, id);
        return p;
    }

    /*
     * C3Panel
     */
    private C3Panel(Channel3 c3, int id) {
        this.c3 = c3;
        this.id = id;
        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        addFocusListener(this);
    }

    Image getImage() {
        if (image == null) {
            image = createImage(200, 200);
c3.trace("=======================================================  C3Panel "+id+" image = "+image);
        }
        return image;
    }

    void setDim(int x, int y) {
        if (x != -1) {
            dim = new Dimension(x, y);
        }
    }

    public void keyPressed(KeyEvent ke) {
        c3.addEvent(ChannelOpcodes.AWTEVENT_keyPressed, id, ke.getKeyCode(), ke.getKeyChar());
    }

    public void keyReleased(KeyEvent ke) {
        c3.addEvent(ChannelOpcodes.AWTEVENT_keyReleased, id, ke.getKeyCode(), ke.getKeyChar());
    }

    public void keyTyped(KeyEvent ke) {
        c3.addEvent(ChannelOpcodes.AWTEVENT_keyTyped, id, ke.getKeyCode(), ke.getKeyChar());
    }

    public void mousePressed(MouseEvent e) {
        c3.addEvent(ChannelOpcodes.AWTEVENT_mousePressed, id, e.getX(), e.getY());
    }

    public void mouseReleased(MouseEvent e) {
        c3.addEvent(ChannelOpcodes.AWTEVENT_mouseReleased, id, e.getX(), e.getY());
    }

    public void mouseClicked(MouseEvent e) {
        c3.addEvent(ChannelOpcodes.AWTEVENT_mouseClicked, id, e.getX(), e.getY());
    }

    public void mouseMoved(MouseEvent e) {
//        c3.addEvent(ChannelOpcodes.AWTEVENT_mouseMoved, id, e.getX(), e.getY());
    }

    public void mouseDragged(MouseEvent e) {
//        c3.addEvent(ChannelOpcodes.AWTEVENT_mouseDragged, id, e.getX(), e.getY());
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
//        c3.addEvent(ChannelOpcodes.AWTEVENT_mouseExited, id, e.getX(), e.getY());
    }

    public void focusGained(FocusEvent e) {
//        c3.addEvent(ChannelOpcodes.AWTEVENT_focusGained, id, 0, 0);
    }

    public void focusLost(FocusEvent e) {
    }

/*
    public void update(Graphics g) {
        int gid = c3.getGid(g, "update");
        c3.graphics[gid].setColor(new Color(0));
        c3.graphics[gid].drawString("update "+gid, 3, 14);
        c3.addEvent(ChannelOpcodes.AWTEVENT_update, id, 0, 0);
        g.drawImage(image, 0, 0, this);
    }
*/
    public void paint(Graphics g) {
/*
        int gid = c3.getGid(g, "paint");
        c3.graphics[gid].setColor(new Color(0));
        c3.graphics[gid].drawString("paint "+gid, 3, 14);
        c3.addEvent(ChannelOpcodes.AWTEVENT_paint, id, gid, 0);
*/
        if (image != null) {
            g.drawImage(image, 0, 0, this);
        }
    }


    public Dimension getMinimumSize() {
        return (dim == null) ? super.getMinimumSize() : dim;
    }

    public Dimension getMaximumSize() {
        return (dim == null) ? super.getMaximumSize() : dim;
    }

    public Dimension getPreferredSize() {
        return (dim == null) ? super.getPreferredSize() : dim;
    }
}