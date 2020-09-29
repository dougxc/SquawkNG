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
 * Special channel for graphics.
 */
public class Channel2 extends Channel implements FocusListener {

    Channel1 chan1;

    IntHashtable fonts  = new IntHashtable();
    IntHashtable images = new IntHashtable();
    IntHashtable rgbs   = new IntHashtable();

    int nextImageNumber = 0;
    Frame f;
    Panel p;
    Graphics gg;
    Image imgBuf;
    Graphics imgBuf_g;
    boolean offScreen = false;
    private static MediaTracker tracker;

    int screenWidth  = 300;
    int screenHeight = 300;



    static final boolean TRACE = false;

    /*
     * trace
     */
    void trace(String s) {
//        System.out.println(s);
    }


    /*
     * Constructor
     */
    public Channel2(ChannelIO cio, int index, boolean debug, Channel1 chan1) {
        super(cio, index, debug);
        this.chan1 = chan1;
    }


    /*
     * setupGraphics
     */
    private void setupGraphics() {
        if (TRACE) trace("setupGraphics "+screenWidth+":"+screenHeight);

        f = new Frame();

        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (TRACE) trace("bye...");
                chan1.addEvent(3<<16, 0);
            }
        });

        p = new Panel() {
            public void paint(Graphics g) {
                chan1.addEvent(0, 0);
            }
        };
        p.addKeyListener(chan1);
        p.addMouseListener(chan1);
        p.addMouseMotionListener(chan1);
        f.addFocusListener(this);

        f.setSize(screenWidth+8, screenHeight+27);

        f.add(p);

        f.setVisible(true);
        gg = p.getGraphics();

        tracker = new MediaTracker(p);
    }


    /*
     * getGraphics
     */
    Graphics getGraphics() {
        if (gg == null) {
            setupGraphics();
        }
        // MIDP apps are double buffered, regular kawt apps are not
        if (offScreen && imgBuf == null) {
//System.out.println("get imgBuf");
            p.setBackground(Color.black);
            imgBuf = p.createImage(f.getWidth(), f.getHeight());
            imgBuf_g = imgBuf.getGraphics();
            imgBuf_g.setColor(Color.blue);
            imgBuf_g.fillRect(0, 0, f.getWidth(), f.getHeight());

        }

        if (offScreen) {
            return imgBuf_g;
        } else {
            return gg;
        }
    }


    /*
     * flushScreen
     */
    void flushScreen() {
        if (offScreen && gg != null && imgBuf != null) {
            gg.drawImage(imgBuf, 0, 0, p);
if (TRACE) trace("**flushScreen**");
        }
    }


   /*
    * execute
    */
    int execute(
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
//result = 9999;
        switch (op) {
            case ChannelOpcodes.SCREENWIDTH: {
                result = screenWidth;
                break;
            }
            case ChannelOpcodes.SCREENHEIGHT: {
                result = screenHeight;
                break;
            }
            case ChannelOpcodes.BEEP: {                                 // in awtcore.impl.squawk.ToolkitImpl
                Toolkit.getDefaultToolkit().beep();
                break;
            }
            case ChannelOpcodes.SETOFFSCREENMODE: {                     // in awtcore.impl.squawk.ToolkitImpl
                if (TRACE) trace("setOffScreenMode");
                offScreen = true;
                break;
            }
            case ChannelOpcodes.FLUSHSCREEN: {                          // in awtcore.impl.squawk.ToolkitImpl
                if (TRACE) trace("setOnScreen");
                flushScreen();
                break;
            }
            case ChannelOpcodes.CREATEIMAGE: {                          // in awtcore.impl.squawk.ImageImpl
                byte[] buf = (byte[])o1;
                int offset =         i1;
                int length =         i2;
                getGraphics();
                Image img = Toolkit.getDefaultToolkit().createImage(buf);
                tracker.addImage(img, 0);
                try {
                    tracker.waitForID(0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
//getGraphics();
//Image img = p.createImage(10, 10);
                result = nextImageNumber++;
                images.put((int)result, img);
                if (TRACE) trace("createImage "+result);
                break;
            }
            case ChannelOpcodes.CREATEMEMORYIMAGE: {                    // in awtcore.impl.squawk.ImageImpl
                int hs     =        i1;
                int vs     =        i2;
                int rgblth =        i3;
                int stride =        i4;
                getGraphics();
                int[] rgb = new int[rgblth];
                DirectColorModel colormodel = new DirectColorModel(24, 0x0000ff, 0x00ff00, 0xff0000);
                MemoryImageSource imageSource = new MemoryImageSource(hs, vs, colormodel, rgb, 0, stride );
                Image img = Toolkit.getDefaultToolkit().createImage(imageSource);
                result = nextImageNumber++;
                images.put((int)result, img);
                rgbs.put  ((int)result, rgb);
                if (TRACE) trace("createMemoryImage "+result);
                break;
            }
            case ChannelOpcodes.GETIMAGE: {                             // in awtcore.impl.squawk.ImageImpl
                String s = (String)o1;
                getGraphics();
                Image img = Toolkit.getDefaultToolkit().getImage(s.replace('/', File.separatorChar));
                tracker.addImage(img, 0);
                try {
                    tracker.waitForID(0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
//System.out.println("res="+tracker.isErrorAny());
//Image img = p.createImage(10, 10);
                result = nextImageNumber++;
                if (TRACE) trace("getImage "+result+" "+s);
                images.put((int)result, img);
                break;
            }
            case ChannelOpcodes.IMAGEWIDTH: {                           // in awtcore.impl.squawk.ImageImpl
                if (TRACE) trace("imageWidth");
                int index = i1;
                Image img = (Image)images.get(index);
                result = img.getWidth(null);
                break;
            }
            case ChannelOpcodes.IMAGEHEIGHT: {                          // in awtcore.impl.squawk.ImageImpl
                if (TRACE) trace("imageHeight");
                int index = i1;
                Image img = (Image)images.get(index);
                result = img.getHeight(null);
                break;
            }
            case ChannelOpcodes.DRAWIMAGE: {                            // in awtcore.impl.squawk.ImageImpl
                int index = i1;
                int     x = i2;
                int     y = i3;
                if (TRACE) trace("drawImage0 "+index+" at "+x+":"+y );
                Image img = (Image)images.get(index);
                getGraphics().drawImage(img, x, y, null);
                break;
            }
            case ChannelOpcodes.FLUSHIMAGE: {                           // in awtcore.impl.squawk.ImageImpl
                int   index =        i1;
                int[] rgb   = (int[])o1;
                if (TRACE) trace("flush0 "+index+" "+rgb);
                if (rgb != null) {
                    int[] realrgb = (int[])rgbs.get(index);
                    if (realrgb == null) {
                        System.out.println("Cannot find rgb buffer for image "+index);
                        System.exit(1);
                    }
                    if (realrgb.length != rgb.length) {
                        System.out.println("Bad flushimage rgb buffer length -- realrgb.length = "+realrgb.length+"rgb.length = "+rgb.length);
                        System.exit(1);
                    }
                    System.arraycopy(rgb, 0, realrgb, 0, realrgb.length);
                }
                Image img = (Image)images.get(index);
                img.flush();
                break;
            }
            case ChannelOpcodes.CREATEFONTMETRICS: {                    // in awtcore.impl.squawk.FontMetricsImpl
                int size   = i1;
                int isBold = i2;
                int sizeBold = size << 16 + isBold;
                FontMetrics metrics = (FontMetrics)fonts.get(sizeBold);
                if (metrics == null) {
                    metrics = Toolkit.getDefaultToolkit().getFontMetrics(new Font("TimesRoman", isBold==1 ? Font.BOLD : Font.PLAIN, size));
                    fonts.put(sizeBold, metrics);
                }
                if (TRACE) trace("createFontMetrics "+sizeBold+" = "+(metrics == null ? null : metrics.getFont()));
                result = sizeBold;
                break;
            }
            case ChannelOpcodes.FONTSTRINGWIDTH: {                      // in awtcore.impl.squawk.FontMetricsImpl
                int sizeBold = i1;
                String s     = (String)o1;
                FontMetrics metrics = (FontMetrics)fonts.get(sizeBold);
                result = metrics.stringWidth(s);
                if (TRACE) trace("fontStringWidth "+sizeBold+ ":"+s+" = "+result);
                break;
            }
            case ChannelOpcodes.FONTGETHEIGHT: {                        // in awtcore.impl.squawk.FontMetricsImpl
                int sizeBold = i1;
                FontMetrics metrics = (FontMetrics)fonts.get(sizeBold);
                result = metrics.getHeight();
                if (TRACE) trace("fontGetHeight "+sizeBold+" = "+result);
                break;
            }
            case ChannelOpcodes.FONTGETASCENT: {                        // in awtcore.impl.squawk.FontMetricsImpl
                int sizeBold = i1;
                FontMetrics metrics = (FontMetrics)fonts.get(sizeBold);
                result = metrics.getAscent();
                if (TRACE) trace("fontGetHeight "+sizeBold+" = "+result);
                break;
            }
            case ChannelOpcodes.FONTGETDESCENT: {                       // in awtcore.impl.squawk.FontMetricsImpl
                int sizeBold = i1;
                FontMetrics metrics = (FontMetrics)fonts.get(sizeBold);
                result = metrics.getDescent();
                if (TRACE) trace("fontGetHeight "+sizeBold+" = "+result);
                break;
            }

            case ChannelOpcodes.SETFONT: {                              // awtcore.impl.squawk.GraphicsImpl
                int sizeBold = i1;
                FontMetrics metrics = (FontMetrics)fonts.get(sizeBold);
                if (TRACE) trace("setFont0 "+metrics.getFont());
                getGraphics().setFont(metrics.getFont());
                break;
            }
            case ChannelOpcodes.SETCOLOR: {                             // awtcore.impl.squawk.GraphicsImpl
                int c  = i1;
                if (TRACE) trace("setColor0 "+c);
                getGraphics().setColor(new Color(c));
                break;
            }
            case ChannelOpcodes.SETCLIP: {                              // awtcore.impl.squawk.GraphicsImpl
                int x  = i1;
                int y  = i2;
                int w  = i3;
                int h  = i4;
                if (TRACE) trace("setClip0 "+x+":"+y+":"+w+":"+h);
                getGraphics().setClip(x, y, w, h);
                break;
            }

            case ChannelOpcodes.DRAWSTRING: {                            // awtcore.impl.squawk.GraphicsImpl
                String s = (String)o1;
                int x    =         i1;
                int y    =         i2;
                if (TRACE) trace("drawString0 \""+s+"\" "+x+":"+y);
                getGraphics().drawString(s, x, y);
                break;
            }

            case ChannelOpcodes.DRAWLINE: {                             // awtcore.impl.squawk.GraphicsImpl
                int x  = i1;
                int y  = i2;
                int w  = i3;
                int h  = i4;
                if (TRACE) trace("drawLine0 "+x+":"+y+":"+w+":"+h);
                getGraphics().drawLine(x, y, w, h);
                break;
            }
            case ChannelOpcodes.DRAWOVAL: {                             // awtcore.impl.squawk.GraphicsImpl
                int x  = i1;
                int y  = i2;
                int w  = i3;
                int h  = i4;
                if (TRACE) trace("drawOval0 "+x+":"+y+":"+w+":"+h);
                getGraphics().drawOval(x, y, w, h);
                break;
            }

            case ChannelOpcodes.DRAWRECT: {                             // awtcore.impl.squawk.GraphicsImpl
                int x  = i1;
                int y  = i2;
                int w  = i3;
                int h  = i4;
                if (TRACE) trace("drawRect0 "+x+":"+y+":"+w+":"+h);
                getGraphics().drawRect(x, y, w, h);
                break;
            }
            case ChannelOpcodes.FILLRECT: {                             // awtcore.impl.squawk.GraphicsImpl
                int x  = i1;
                int y  = i2;
                int w  = i3;
                int h  = i4;
                if (TRACE) trace("fillRect0 "+x+":"+y+":"+w+":"+h);
                getGraphics().fillRect(x, y, w, h);
                break;
            }
            case ChannelOpcodes.DRAWROUNDRECT: {                        // awtcore.impl.squawk.GraphicsImpl
                int x  = i1;
                int y  = i2;
                int w  = i3;
                int h  = i4;
                int aw = i5;
                int ah = i6;
                if (TRACE) trace("drawRoundRect0 "+x+":"+y+":"+w+":"+h+":"+aw+":"+ah);
                getGraphics().drawRoundRect(x, y, w, h, aw, ah);
                break;
            }
            case ChannelOpcodes.FILLROUNDRECT: {                        // awtcore.impl.squawk.GraphicsImpl
                int x  = i1;
                int y  = i2;
                int w  = i3;
                int h  = i4;
                int aw = i5;
                int ah = i6;
                if (TRACE) trace("fillRoundRect0 "+x+":"+y+":"+w+":"+h+":"+aw+":"+ah);
                getGraphics().fillRoundRect(x, y, w, h, aw, ah);
                break;
            }
            case ChannelOpcodes.FILLARC: {                              // awtcore.impl.squawk.GraphicsImpl
                int x  = i1;
                int y  = i2;
                int w  = i3;
                int h  = i4;
                int ba = i5;
                int ea = i6;
                if (TRACE) trace("fillArc0 "+x+":"+y+":"+w+":"+h+":"+ba+":"+ea);
                getGraphics().fillArc(x, y, w, h, ba, ea);
                break;
            }
            case ChannelOpcodes.FILLPOLYGON: {                          // awtcore.impl.squawk.GraphicsImpl
                int[] x     = (int[])o1;
                int[] y     = (int[])o2;
                int   count =        i1;
                if (TRACE) trace("fillPolygon0 "+count);
                getGraphics().fillPolygon(x, y, count);
                break;
            }
            case ChannelOpcodes.REPAINT: {                              // awtcore.impl.squawk.GraphicsImpl
                if (TRACE) trace("repaint0");
                p.repaint();
                break;
            }

            default: throw new RuntimeException("Illegal channel operation "+op);
        }

        return 0;
    }


    /*
     * focusGained
     */
    public void focusGained(FocusEvent e) {
        p.requestFocus();
        flushScreen();
    }


    /*
     * focusLost
     */
    public void focusLost(FocusEvent e) {
    }

}

