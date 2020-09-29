package mojo;

import java.awt.*;
import java.awt.event.*;
import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;

public class Main implements WindowListener {
	public static int WIDTH = 200;
	public static int HEIGHT = 200;
    public static final int SOFTBUTTON_HEIGHT = 20;
	private static MojoScreen screen = new MojoScreen();

	public Main(String s) {
		String string;
		MIDlet midlet = null;

		string = System.getProperty("mojo.screen.width");
		if (string != null) {
			try {WIDTH = Integer.parseInt(string);}
			catch (NumberFormatException nfe) {}
		}

		string = System.getProperty("mojo.screen.height");
		if (string != null) {
			try {HEIGHT = Integer.parseInt(string);}
			catch (NumberFormatException nfe) {}
		}

		Frame f = new Frame("Mojo");
		f.addWindowListener(this);
        f.setSize(WIDTH + 8, HEIGHT + 27 + SOFTBUTTON_HEIGHT);
		f.add(screen);
		f.setVisible(true);

        try {midlet = (MIDlet)Class.forName(s).newInstance();}
        catch (Exception e) {
            System.out.println("problem instantiating MIDlet: " + e);
            System.exit(-1);
        }

		try {midlet.publicStartApp();}
		catch (MIDletStateChangeException e) {
			System.out.println("problem starting MIDlet: " + e);
			System.exit(-1);
		}
	}

	public static MojoScreen getScreen() {
		return screen;
	}

    public void windowActivated(WindowEvent e) {
        Display d = Display.getDisplay();
        Displayable current = d.getCurrent();
        if (current != null) {
            Panel proxy = current.getProxy();
            proxy.repaint();
            System.out.println("proxy.repaint()");
            proxy.requestFocus();
            System.out.println("proxy.requestFocus()");
        }

        System.out.println("windowActivated()");
    }

    public void windowClosed(WindowEvent e) {
        System.out.println("Main.windowClosed()");
    }

    public void windowClosing(WindowEvent e) {
        System.out.println("Main.windowClosing()");
        System.out.println("bye...");
        System.exit(0);
    }

    public void windowDeactivated(WindowEvent e) {
        System.out.println("Main.windowDeactivated()");
    }

    public void windowDeiconified(WindowEvent e) {
        System.out.println("Main.windowDeiconified()");
    }

    public void windowIconified(WindowEvent e) {
        System.out.println("Main.windowIconified()");
    }

    public void windowOpened(WindowEvent e) {
        System.out.println("Main.windowOpened()");
    }

	public static void main(String[] arg) {
		if (arg.length < 1) {
			System.out.println("usage: Main <midlet>");
			System.exit(-1);
		}

		new Main(arg[0]);
	}
}
