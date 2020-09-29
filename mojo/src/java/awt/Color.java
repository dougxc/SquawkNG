package java.awt;

public class Color {

    public final static Color lightGray = new Color (0x0c0c0c0);
    public final static Color white     = new Color (0x0ffffff);
    public final static Color black     = new Color (0x0000000);

    final int rgb;

    public Color(int rgb) {
        this.rgb = rgb;
    }

    int rgb() {
        return rgb;
    }
}



