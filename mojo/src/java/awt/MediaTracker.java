
package java.awt;
import com.sun.squawk.vm.ChannelOpcodes;

public class MediaTracker {

    int pid;

    public MediaTracker(Panel o) {
        pid = o.id();
    }

    public void addImage(Image image, int i) {
        Native.execIO3(ChannelOpcodes.MEDIATRACKER_WAITFOR, pid, image.id(), 0, 0, 0, 0, null, null, null);
    }

    public void waitForID(int i) throws InterruptedException {
    }
}
