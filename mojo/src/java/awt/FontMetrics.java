package java.awt;
import com.sun.squawk.vm.ChannelOpcodes;

public class FontMetrics extends ID {

    public int charWidth(char c) {
        return (int)Native.execIO3(ChannelOpcodes.FONTMETRICS_CHARWIDTH, id(), c, 0, 0, 0, 0, null, null, null);
    }

    public int stringWidth(String s) {
        return (int)Native.execIO3(ChannelOpcodes.FONTMETRICS_STRINGWIDTH, id(), 0, 0, 0, 0, 0, s, null, null);
    }

    public int getHeight() {
        return (int)Native.execIO3(ChannelOpcodes.FONTMETRICS_GETHEIGHT, id(), 0, 0, 0, 0, 0, null, null, null);
    }
}
