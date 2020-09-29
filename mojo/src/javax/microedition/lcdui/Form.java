package javax.microedition.lcdui;

public class Form extends Screen {

    public Form(String txt) {
        super(txt);

        proxy.setBackground(java.awt.Color.white);

        proxy.addLabel(txt);
    }

    public Form(String title, Item[] items) {
        super(title);
    }

    private String asciifyString(String s) {
        StringBuffer buf = new StringBuffer();
        char c;

        for (int i=0; i<s.length(); i++) {
            switch (c = s.charAt(i)) {
                case '\n': buf.append(" "); break;
                default: buf.append(c); break;
            }
        }

        return buf.toString();
    }

    public void append(String s) {
        proxy.addLabel(asciifyString(s));
    }

    public void append(StringItem s) {
        proxy.addLabel(asciifyString(s.getLabel() + s.getText()));
    }

    public void append(Item s) {
    }

    public Item get(int index) {
        return null;
    }
}
