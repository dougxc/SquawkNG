//if[VM.GCSPY]
//IFC//#if 0
package com.sun.squawk.vm;

public interface GCspy {

    public final static int

        GCSPY_EVENT_START_PARTIAL_GC = 0,
        GCSPY_EVENT_END_PARTIAL_GC   = 1,
        GCSPY_EVENT_START_FULL_GC    = 2,
        GCSPY_EVENT_END_FULL_GC      = 3,
        GCSPY_EVENT_ALLOC_OBJECT     = 4,
        GCSPY_EVENT_ALLOC_ARRAY      = 5;

    public static final String[] GCSPY_EVENT_NAMES = {
        "Start partial GC",
        "End partial GC",
        "Start full GC",
        "End full GC",
        "Alloc object",
        "Alloc array"
    };

    void gcspyEvent(int eventID);

}
//IFC//#endif