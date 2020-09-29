//J2C:opcodes.h **DO NOT DELETE THIS LINE**
/*
 * Copyright 1994-2002 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */

/*IFJ*/package com.sun.squawk.vm;
/*IFJ*/public final class ChannelOpcodes {

    public final static int

        /* I/O channel opcodes */

        GETCHANNEL               = 1,
        FREECHANNEL              = 2,
        OPEN                     = 3,
        CLOSE                    = 4,
        ACCEPT                   = 5,
        OPENINPUT                = 6,
        CLOSEINPUT               = 7,
        WRITEREAD                = 8,
        READBYTE                 = 9,
        READSHORT                = 10,
        READINT                  = 11,
        READLONG                 = 12,
        READBUF                  = 13,
        SKIP                     = 14,
        AVAILABLE                = 15,
        MARK                     = 16,
        RESET                    = 17,
        MARKSUPPORTED            = 18,
        OPENOUTPUT               = 19,
        FLUSH                    = 20,
        CLOSEOUTPUT              = 21,
        WRITEBYTE                = 22,
        WRITESHORT               = 23,
        WRITEINT                 = 24,
        WRITELONG                = 25,
        WRITEBUF                 = 26,

        /* Opcodes for KAWT graphics API */

        SCREENWIDTH          = 27,
        SCREENHEIGHT         = 28,
        BEEP                 = 29,
        SETOFFSCREENMODE     = 30,
        FLUSHSCREEN          = 31,
        CREATEIMAGE          = 32,
        CREATEMEMORYIMAGE    = 33,
        GETIMAGE             = 34,
        IMAGEWIDTH           = 35,
        IMAGEHEIGHT          = 36,
        DRAWIMAGE            = 37,
        FLUSHIMAGE           = 38,
        CREATEFONTMETRICS    = 39,
        FONTSTRINGWIDTH      = 40,
        FONTGETHEIGHT        = 41,
        FONTGETASCENT        = 42,
        FONTGETDESCENT       = 43,
        SETFONT              = 44,
        SETCOLOR             = 45,
        SETCLIP              = 46,
        DRAWSTRING           = 47,
        DRAWLINE             = 48,
        DRAWOVAL             = 49,
        DRAWRECT             = 50,
        FILLRECT             = 51,
        DRAWROUNDRECT        = 52,
        FILLROUNDRECT        = 53,
        FILLARC              = 54,
        FILLPOLYGON          = 55,
        REPAINT              = 56,

        /* Opcodes for MOJO graphics API */

        FRAME_GETEVENT           = 57,
        FRAME_NEW                = 58,
        FRAME_SETVISIBLE         = 59,
        FRAME_SETSIZE            = 60,
        FRAME_ADD                = 61,
        FONT_NEW                 = 62,
        FONT_GETSIZE             = 63,
        FONTMETRICS_CHARWIDTH    = 64,
        FONTMETRICS_STRINGWIDTH  = 65,
        FONTMETRICS_GETHEIGHT    = 66,
        GRAPHICS_SETCOLOR        = 67,
        GRAPHICS_GETFONT         = 68,
        GRAPHICS_GETFONTMETRICS  = 69,
        GRAPHICS_DRAWIMAGE       = 70,
        GRAPHICS_DRAWSTRING      = 71,
        GRAPHICS_FILLARC1        = 72,
        GRAPHICS_FILLARC2        = 73,
        GRAPHICS_DRAWLINE        = 74,
        GRAPHICS_DRAWRECT        = 75,
        GRAPHICS_FILLRECT        = 76,
        GRAPHICS_FILLPOLY        = 77,
        GRAPHICS_SETCLIP         = 78,
        GRAPHICS_TRANSLATE       = 79,
        IMAGE_GETGRAPHICS        = 80,
        IMAGE_GETWIDTH           = 81,
        IMAGE_GETHEIGHT          = 82,
        MEDIATRACKER_WAITFOR     = 83,
        PANEL_NEW                = 84,
        PANEL_REPAINT            = 85,
        PANEL_REPAINT2           = 86,
        PANEL_CREATEIMAGE        = 87,
        PANEL_REMOVE             = 88,
        PANEL_INVALIDATE         = 89,
        PANEL_VALIDATE           = 90,
        PANEL_REQUESTFOCUS       = 91,
        PANEL_ADD                = 92,
        PANEL_ADD2               = 93,
        PANEL_ADDLABEL           = 94,
        PANEL_SETBACKGROUND      = 95,
        PANEL_SETBORDERLAYOUT    = 96,
        PANEL_GETHEIGHT          = 97,
        PANEL_GETWIDTH           = 98,
        PANEL_GETFONTMETRICS     = 99,
        TOOLKIT_CREATEIMAGE      = 100,

        AWTEVENT_exit               = 0,
        AWTEVENT_update             = 1,
        AWTEVENT_paint              = 2,
        AWTEVENT_keyPressed         = 3,
        AWTEVENT_keyReleased        = 4,
        AWTEVENT_keyTyped           = 5,
        AWTEVENT_mousePressed       = 6,
        AWTEVENT_mouseReleased      = 7,
        AWTEVENT_mouseClicked       = 8,
        AWTEVENT_mouseExited        = 9,
        AWTEVENT_mouseMoved         = 10,
        AWTEVENT_mouseDragged       = 11,
        AWTEVENT_focusGained        = 12,

        DUMMY         = 999;



/*IFJ*/}
