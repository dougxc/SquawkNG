package com.sun.squawk.translator.ir;

import com.sun.squawk.translator.Type;
import com.sun.squawk.translator.Assert;

/**
 * This class represents an exception handler in terms of IR instructions.
 */
public final class ExceptionHandlerTable {

    public static class Entry {
        /** The pseudo-instruction denoting the start of the code range protected by this handler. */
        private HandlerEnter   start;
        /** The pseudo-instruction denoting the end of the code range protected by this handler. */
        private HandlerExit    end;
        /** The pseudo-instruction denoting the entry point in the code of this handler. */
        private LoadException  handlerEntry;
        /** The address of the entry point in the code of this handler. */
        private Target         handlerEntryTarget;
        /** The type of exception caught by this handler. */
        private Type catchType;

        /**
         * Constructor.
         */
        public Entry(HandlerEnter start, Target handlerEntryTarget) {
            this.start     = start;
            this.handlerEntryTarget = handlerEntryTarget;
            this.catchType = handlerEntryTarget.getExceptionType();
        }

        public void setEnd(HandlerExit end)         {  this.end    = end;     }
        public void setHandlerEntry(LoadException handlerEntry) { this.handlerEntry = handlerEntry; }

        public HandlerEnter tryStart()      { return start;        }
        public HandlerExit  tryEnd()        { return end;          }
        public LoadException handlerEntry() { return handlerEntry; }
        public Target handlerEntryTarget()  { return handlerEntryTarget; }
        public Type catchType()             { return catchType; }
    }

    /** The entries in the table. */
    private final Entry[] entries;

    public ExceptionHandlerTable(Entry[] entries) {
        Assert.that(entries != null && entries.length > 0);
        this.entries = entries;
    }

    public Entry lookup(Target target) {
        for (int i = 0; i != entries.length; i++) {
            if (entries[i].handlerEntryTarget() == target) {
                return entries[i];
            }
        }
        return null;
    }

    public Entry[] entries() {
        return entries;
    }
}

