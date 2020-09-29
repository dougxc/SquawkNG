package java.awt;

import java.awt.event.*;

import com.sun.squawk.util.*;
import com.sun.squawk.vm.ChannelOpcodes;

public class Panel extends ID {

    static IntHashtable table = new IntHashtable();

    static Panel getPanel(int id) {
        return (Panel)table.get(id);
    }

    public Panel() {
        Native.execIO3(ChannelOpcodes.PANEL_NEW, id(), 0, 0, 0, 0, 0, null, null, null);
        table.put(id(), this);
    }

    public void update(Graphics g) {
        paint(g);
    }

    public void paint(Graphics g) {
    }

    public void repaint() {
        update(new Graphics(id()));
        Native.execIO3(ChannelOpcodes.PANEL_REPAINT, id(), 0, 0, 0, 0, 0, null, null, null);
    }

    public void repaint(int a, int x, int y, int w, int h) {
         repaint();
        //Native.execIO3(ChannelOpcodes.PANEL_REPAINT2, id(), a, x, y, w, h, null, null, null);
    }

    public Image createImage(int x, int y) {
        Image r = new Image();
        Native.execIO3(ChannelOpcodes.PANEL_CREATEIMAGE, id(), r.id(), x, y, 0, 0, null, null, null);
        return r;
    }

    public void remove(int a) {
        Native.execIO3(ChannelOpcodes.PANEL_REMOVE, id(), a, 0, 0, 0, 0, null, null, null);
    }

    public void invalidate() {
        Native.execIO3(ChannelOpcodes.PANEL_INVALIDATE, id(), 0, 0, 0, 0, 0, null, null, null);
    }

    public void validate() {
        Native.execIO3(ChannelOpcodes.PANEL_VALIDATE, id(), 0, 0, 0, 0, 0, null, null, null);
    }

    public void requestFocus() {
        Native.execIO3(ChannelOpcodes.PANEL_REQUESTFOCUS, id(), 0, 0, 0, 0, 0, null, null, null);
    }

    public void add(Panel p, int y) {
        Dimension d = p.getPreferredSize();
        int w = -1;
        int h = -1;
        if (d != null) {
            w = d.width;
            h = d.height;
        }
        Native.execIO3(ChannelOpcodes.PANEL_ADD, id(), p.id(), y, w, h, 0, null, null, null);
    }

    public void add(Panel p, String y) {
        Dimension d = p.getPreferredSize();
        int w = -1;
        int h = -1;
        if (d != null) {
            w = d.width;
            h = d.height;
        }
        Native.execIO3(ChannelOpcodes.PANEL_ADD2, id(), p.id(), w, h, 0, 0, y, null, null);
    }

    public void addLabel(String txt) {
        Native.execIO3(ChannelOpcodes.PANEL_ADDLABEL, id(), 0, 0, 0, 0, 0, txt, null, null);
        //add(new Label(txt));
    }

    public void setBackground(Color c) {
        Native.execIO3(ChannelOpcodes.PANEL_SETBACKGROUND, id(), c.rgb(), 0, 0, 0, 0, null, null, null);
    }

    public void setLayout(BorderLayout l) {
        Native.execIO3(ChannelOpcodes.PANEL_SETBORDERLAYOUT, id(), 0, 0, 0, 0, 0, null, null, null);
    }

    public int getHeight() {
        return (int)Native.execIO3(ChannelOpcodes.PANEL_GETHEIGHT, id(), 0, 0, 0, 0, 0, null, null, null);
    }

    public int getWidth() {
        return (int)Native.execIO3(ChannelOpcodes.PANEL_GETWIDTH, id(), 0, 0, 0, 0, 0, null, null, null);
    }

    public FontMetrics getFontMetrics(Font f) {
        FontMetrics r = new FontMetrics();
        Native.execIO3(ChannelOpcodes.PANEL_GETFONTMETRICS, id(), f.id(), r.id(), 0, 0, 0, null, null, null);
        return r;
    }

    KeyListener         keyListener;
    MouseListener       mouseListener;
    MouseMotionListener mouseMotionListener;
    FocusListener       focusListener;

    public void addKeyListener(KeyListener kl) {
        keyListener = kl;
    }

    public void addMouseListener(MouseListener ml) {
        mouseListener = ml;
    }

    public void addMouseMotionListener(MouseMotionListener mml) {
        mouseMotionListener = mml;
    }

    public void addFocusListener(FocusListener fl) {
        focusListener = fl;
    }

    public void paintEvent(int gid) {
        //Graphics g = Graphics.create(gid, "Panel.paintEvent", id());
        //paint(g);
    }

    public void updateEvent(int gid) {
        //Graphics g = Graphics.create(gid, "Panel.updateEvent", id());
        //update(g);
    }






    public Dimension getMinimumSize() {
        return null;
    }

    public Dimension getMaximumSize() {
        return null;
    }

    public Dimension getPreferredSize() {
        return null;
    }

}
