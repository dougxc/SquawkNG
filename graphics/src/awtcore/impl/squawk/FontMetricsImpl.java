
package awtcore.impl.squawk;

import java.awt.*;
import com.sun.squawk.vm.ChannelOpcodes;

public class FontMetricsImpl extends FontMetrics {

    static FontMetricsImpl defaultFontMetrics = FontMetricsImpl.create(new Font("plain", Font.PLAIN, 8));

    int fontIndex;

    static FontMetricsImpl create(Font font) {
        return new FontMetricsImpl(font);
    }

    private FontMetricsImpl(Font font) {
        super(font);
        fontIndex = createFontMetrics(font.getSize(), font.isBold() ? 1 : 0);
//if (font.getSize() == 0) throw new RuntimeException();
    }

    public int stringWidth(String s) {
        return fontStringWidth(fontIndex, s);
    }

    public int getHeight() {
        return fontGetHeight(fontIndex);
    }

    public int getAscent() {
        return fontGetAscent(fontIndex);
    }

    public int getDescent() {
        return fontGetDescent(fontIndex);
    }

    private int createFontMetrics(int size, int isBold) {
        return (int)Native.execIO2(ChannelOpcodes.CREATEFONTMETRICS, size, isBold, 0, 0, 0, 0, null, null, null);
    }
    private int fontStringWidth(int font, String string) {
        return (int)Native.execIO2(ChannelOpcodes.FONTSTRINGWIDTH, font, 0, 0, 0, 0, 0, string, null, null);
    }
    private int fontGetHeight(int font) {
        return (int)Native.execIO2(ChannelOpcodes.FONTGETHEIGHT, font, 0, 0, 0, 0, 0, null, null, null);
    }
    private int fontGetAscent(int font) {
        return (int)Native.execIO2(ChannelOpcodes.FONTGETASCENT, font, 0, 0, 0, 0, 0, null, null, null);
    }
    private int fontGetDescent(int font) {
        return (int)Native.execIO2(ChannelOpcodes.FONTGETDESCENT, font, 0, 0, 0, 0, 0, null, null, null);
    }

}
