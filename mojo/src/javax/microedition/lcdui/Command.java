package javax.microedition.lcdui;

public class Command {
    public static final int SCREEN = 1;
    public static final int BACK = 2;
    public static final int CANCEL = 3;
    public static final int OK = 4;
    public static final int HELP = 5;
    public static final int STOP = 6;
    public static final int EXIT = 7;
    public static final int ITEM = 8;

    final String label;
    final int type;
    final int priority;
    private int id;
    private static int counter = 0;

	public Command(String label, int type, int priority) {
        this.label = label;
        this.type = type;
        this.priority = priority;
        id = counter++;
	}

    public String getLabel() {
        return label;
    }

    public int getType() {
        return type;
    }

    public int getPriority() {
        return priority;
    }

    public int getID() {
        return id;
    }
}
