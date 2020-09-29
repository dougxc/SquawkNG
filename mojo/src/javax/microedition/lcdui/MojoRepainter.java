package javax.microedition.lcdui;


public class MojoRepainter implements Runnable {
	private static java.util.Hashtable list = new java.util.Hashtable();
	private static MojoRepainter instance;
	private static java.awt.Panel panel;

	static {
		instance = new MojoRepainter();
		Thread thread = new Thread(instance);
		thread.setDaemon(true);
		thread.start();
	}

	private MojoRepainter() {
	}

	public static /*synchronized*/ void repaint(java.awt.Panel p) {
		//list.put(p, p);
		panel = p;
	}

	public void run() {
		for (;;) {
			try {Thread.sleep(30);}
			catch (InterruptedException e) {}

			/*synchronized (this) {*/
				//panel.repaint();

				/*
				System.out.println("list.size() = " + list.size());

				java.util.Enumeration enum = list.keys();

				while (enum.hasMoreElements()) {
					((java.awt.Panel)enum.nextElement()).repaint();
				}

				list.clear();
				*/
			/*}*/
		}
	}
}