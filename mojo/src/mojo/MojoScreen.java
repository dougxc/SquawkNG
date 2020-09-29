package mojo;

import java.awt.*;
import java.awt.event.*;

public class MojoScreen extends Panel {
    private Dimension dim;
    private Panel dummy;
    private SoftButtonPanel softButtonPanel;

    public MojoScreen() {
        dim = new Dimension(Main.WIDTH, Main.HEIGHT);
        dummy = new Panel();
        softButtonPanel = new SoftButtonPanel();

        setBackground(Color.black);
        setLayout(new BorderLayout());
        add(dummy, "Center");
        add(softButtonPanel, "South");
    }

    public SoftButtonPanel getSoftButtonPanel() {
        return softButtonPanel;
    }
}
