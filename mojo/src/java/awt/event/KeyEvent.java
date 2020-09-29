

package java.awt.event;

import java.awt.*;

public class KeyEvent {

    public static final int KEY_TYPED         = 400;
    public static final int KEY_PRESSED       = 401;
    public static final int KEY_RELEASED      = 402;

    public static final int VK_ENTER          = '\n';
    public static final int VK_BACK_SPACE     = '\b';
    public static final int VK_TAB            = '\t';
    public static final int VK_CANCEL         = 0x03;
    public static final int VK_CLEAR          = 0x0C;
    public static final int VK_SHIFT          = 0x10;
    public static final int VK_CONTROL        = 0x11;
    public static final int VK_ALT            = 0x12;
    public static final int VK_PAUSE          = 0x13;
    public static final int VK_CAPS_LOCK      = 0x14;
    public static final int VK_ESCAPE         = 0x1B;
    public static final int VK_SPACE          = 0x20;
    public static final int VK_PAGE_UP        = 0x21;
    public static final int VK_PAGE_DOWN      = 0x22;
    public static final int VK_END            = 0x23;
    public static final int VK_HOME           = 0x24;
    public static final int VK_LEFT           = 0x25;
    public static final int VK_UP             = 0x26;
    public static final int VK_RIGHT          = 0x27;
    public static final int VK_DOWN           = 0x28;
    public static final int VK_COMMA          = 0x2C;
    public static final int VK_MINUS          = 0x2D;
    public static final int VK_PERIOD         = 0x2E;
    public static final int VK_SLASH          = 0x2F;
    public static final int VK_SEMICOLON      = 0x3B;
    public static final int VK_EQUALS         = 0x3D;
    public static final int VK_OPEN_BRACKET   = 0x5B;
    public static final int VK_BACK_SLASH     = 0x5C;
    public static final int VK_CLOSE_BRACKET  = 0x5D;
    public static final int VK_NUMPAD0        = 0x60;
    public static final int VK_NUMPAD1        = 0x61;
    public static final int VK_NUMPAD2        = 0x62;
    public static final int VK_NUMPAD3        = 0x63;
    public static final int VK_NUMPAD4        = 0x64;
    public static final int VK_NUMPAD5        = 0x65;
    public static final int VK_NUMPAD6        = 0x66;
    public static final int VK_NUMPAD7        = 0x67;
    public static final int VK_NUMPAD8        = 0x68;
    public static final int VK_NUMPAD9        = 0x69;
    public static final int VK_MULTIPLY       = 0x6A;
    public static final int VK_ADD            = 0x6B;
    public static final int VK_SEPARATER      = 0x6C;
    public static final int VK_SEPARATOR      = VK_SEPARATER;
    public static final int VK_SUBTRACT       = 0x6D;
    public static final int VK_DECIMAL        = 0x6E;
    public static final int VK_DIVIDE         = 0x6F;
    public static final int VK_DELETE         = 0x7F;
    public static final int VK_NUM_LOCK       = 0x90;
    public static final int VK_SCROLL_LOCK    = 0x91;
    public static final int VK_F1             = 0x70;
    public static final int VK_F2             = 0x71;
    public static final int VK_F3             = 0x72;
    public static final int VK_F4             = 0x73;
    public static final int VK_F5             = 0x74;
    public static final int VK_F6             = 0x75;
    public static final int VK_F7             = 0x76;
    public static final int VK_F8             = 0x77;
    public static final int VK_F9             = 0x78;
    public static final int VK_F10            = 0x79;
    public static final int VK_F11            = 0x7A;
    public static final int VK_F12            = 0x7B;


    char keyCode;
    char keyChar;


    public KeyEvent(int keyCode, int keyChar) {
        this.keyCode = (char)keyCode;
        this.keyChar = (char)keyChar;
    }

    public char getKeyChar () {
        return keyChar;
    }

    public int getKeyCode () {
        return keyCode;
    }

}
