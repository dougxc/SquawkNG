package java.awt;

import com.sun.squawk.util.*;
import com.sun.squawk.vm.ChannelOpcodes;

public class Graphics {

    final int gid;

    public Graphics(int gid) {
        this.gid = gid;
    }

    public void setColor(Color c) {
        Native.execIO3(ChannelOpcodes.GRAPHICS_SETCOLOR, gid, c.rgb(), 0, 0, 0, 0, null, null, null);
    }

    public Font getFont() {
        Font r = new Font();
        Native.execIO3(ChannelOpcodes.GRAPHICS_GETFONT, gid, r.id(), 0, 0, 0, 0, null, null, null);
        return r;
    }

    public FontMetrics getFontMetrics() {
        FontMetrics r = new FontMetrics();
        Native.execIO3(ChannelOpcodes.GRAPHICS_GETFONTMETRICS, gid, r.id(), 0, 0, 0, 0, null, null, null);
        return r;
    }

    public boolean drawImage(Image image, int x, int y, Object observer) {
        Native.execIO3(ChannelOpcodes.GRAPHICS_DRAWIMAGE, gid, image.id(), x, y, 0, 0, null, null, null);
        return true;
    }

    public void drawString(String s, int x, int y) {
        Native.execIO3(ChannelOpcodes.GRAPHICS_DRAWSTRING, gid, x, y, 0, 0, 0, s, null, null);
    }

    public void fillArc(int x, int y, int w, int h, int ba, int ea) {
        Native.execIO3(ChannelOpcodes.GRAPHICS_FILLARC1, gid, x,  y,  w, h, 0, null, null, null);
        Native.execIO3(ChannelOpcodes.GRAPHICS_FILLARC2, gid, ba, ea, 0, 0, 0, null, null, null);
    }

    public void drawLine (int x1, int y1, int x2, int y2) {
        Native.execIO3(ChannelOpcodes.GRAPHICS_DRAWLINE, gid, x1, y1, x2, y2, 0, null, null, null);
    }

    public void drawRect(int x, int y, int w, int h) {
        Native.execIO3(ChannelOpcodes.GRAPHICS_DRAWRECT, gid, x, y, w, h, 0, null, null, null);
    }

    public void fillRect(int x, int y, int w, int h) {
        Native.execIO3(ChannelOpcodes.GRAPHICS_FILLRECT, gid, x, y, w, h, 0, null, null, null);
    }

    public void fillPolygon(int[] a,int[] b,int c) {
        Native.execIO3(ChannelOpcodes.GRAPHICS_FILLPOLY, gid, c, 0, 0, 0, 0, a, b, null);
    }

    public void setClip(int x, int y, int w, int h) {
        Native.execIO3(ChannelOpcodes.GRAPHICS_SETCLIP, gid, x, y, w, h, 0, null, null, null);
    }

    public void translate(int x, int y) {
        Native.execIO3(ChannelOpcodes.GRAPHICS_TRANSLATE, gid, x, y, 0, 0, 0, null, null, null);
    }

}






