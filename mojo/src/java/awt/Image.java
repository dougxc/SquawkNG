package java.awt;
import com.sun.squawk.vm.ChannelOpcodes;

public class Image extends ID {

    public Graphics getGraphics() {
//        int gid = (int)Native.execIO3(ChannelOpcodes.IMAGE_GETGRAPHICS, id(), 0, 0, 0, 0, 0, null, null, null);
//        return Graphics.create(gid, "Image", id());
        return new Graphics(id());
    }

    public int getWidth(Object observer)  {
        return (int)Native.execIO3(ChannelOpcodes.IMAGE_GETWIDTH, id(), 0, 0, 0, 0, 0, null, null, null);
    }

    public int getHeight(Object observer) {
        return (int)Native.execIO3(ChannelOpcodes.IMAGE_GETHEIGHT, id(), 0, 0, 0, 0, 0, null, null, null);
    }

}

