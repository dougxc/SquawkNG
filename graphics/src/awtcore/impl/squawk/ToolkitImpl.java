

package awtcore.impl.squawk;

import java.io.*;
import java.util.*;

import java.awt.*;
import java.awt.image.*;
import com.sun.squawk.vm.ChannelOpcodes;

public class ToolkitImpl extends Toolkit {

    static {
        new EventDispatcher().start();
    }

    public ToolkitImpl() {
    }

    public FontMetrics getFontMetrics(Font font) {
        return FontMetricsImpl.create(font);
    }

    public Dimension getScreenSize() {
        return new Dimension(screenWidth(), screenHeight());
    }

    public Image createImage(byte [] data, int offset, int length) {
        return new awtcore.impl.squawk.ImageImpl(data, offset, length);
    }

    //public void sync() {
    //    if (GraphicsImpl.dirty) {
    //        repaint();
    //    }
    //}

    public Image createImage (ImageProducer producer) {
        return new ImageImpl((MemoryImageSource )producer);
    }


    public Image getImage (String ressourceName) {
        return new ImageImpl(ressourceName);
    }


    static int screenWidth() {
        return (int)Native.execIO2(ChannelOpcodes.SCREENWIDTH, 0, 0, 0, 0, 0, 0, null, null, null);
    }
    static int screenHeight() {
        return (int)Native.execIO2(ChannelOpcodes.SCREENHEIGHT, 0, 0, 0, 0, 0, 0, null, null, null);
    }
    public void beep() {
        Native.execIO2(ChannelOpcodes.BEEP, 0, 0, 0, 0, 0, 0, null, null, null);
    }
    public void flushScreen() {
        Native.execIO2(ChannelOpcodes.FLUSHSCREEN, 0, 0, 0, 0, 0, 0, null, null, null);
    }


}



