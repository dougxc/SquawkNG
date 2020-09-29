
package awtcore.impl.squawk;

import java.awt.*;
import java.awt.image.*;
import com.sun.squawk.vm.ChannelOpcodes;

public class ImageImpl extends Image {

    int   imageIndex;
    int[] imageRgb;

    ImageImpl(MemoryImageSource memoryImage) {
        imageIndex = createMemoryImage(
                                        memoryImage.hs,
                                        memoryImage.vs,
                                        memoryImage.rgb.length,
                                        memoryImage.stride
                                      );
        imageRgb = memoryImage.rgb;
    }

    ImageImpl(byte[] data, int offset, int length) {
        imageIndex = createImage(data, offset, length);
    }

    ImageImpl(String ressourceName) {
        imageIndex = getImage(ressourceName);
    }

    public int getWidth (ImageObserver o) {
        return imageWidth(imageIndex);
    }

    public int getHeight (ImageObserver o) {
        return imageHeight(imageIndex);
    }

    public void flush () {
        flush0(imageIndex, imageRgb);
    }


    private int createImage(byte[] data, int offset, int length) {
        return (int)Native.execIO2(ChannelOpcodes.CREATEIMAGE, offset, length, 0, 0, 0, 0, data, null, null);
    }
    private int createMemoryImage(int hs, int vs, int length, int stride) {
        return (int)Native.execIO2(ChannelOpcodes.CREATEMEMORYIMAGE, hs, vs, length, stride, 0, 0, null, null, null);
    }
    private int getImage(String ressourceName) {
        return (int)Native.execIO2(ChannelOpcodes.GETIMAGE, 0, 0, 0, 0, 0, 0, ressourceName, null, null);
    }
    private int imageWidth(int number) {
        return (int)Native.execIO2(ChannelOpcodes.IMAGEWIDTH, number, 0, 0, 0, 0, 0, null, null, null);
    }
    private int imageHeight(int number) {
        return (int)Native.execIO2(ChannelOpcodes.IMAGEHEIGHT, number, 0, 0, 0, 0, 0, null, null, null);
    }
    private void flush0(int number, int[] image) {
        Native.execIO2(ChannelOpcodes.FLUSHIMAGE, number, 0, 0, 0, 0, 0, image, null, null);
    }

}

