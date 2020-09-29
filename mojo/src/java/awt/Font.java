package java.awt;
import com.sun.squawk.vm.ChannelOpcodes;

public class Font extends ID {

    static public final int PLAIN = 0;

    public Font() {
    }

    public Font(String name, int style, int size) {
        Native.execIO3(ChannelOpcodes.FONT_NEW, id(), style, size, 0, 0, 0, name, null, null);
    }

    public int getSize() {
        return (int)Native.execIO3(ChannelOpcodes.FONT_GETSIZE, id(), 0, 0, 0, 0, 0, null, null, null);
    }

}
