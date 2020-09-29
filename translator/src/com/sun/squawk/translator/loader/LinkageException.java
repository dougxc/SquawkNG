package com.sun.squawk.translator.loader;

import com.sun.squawk.translator.Type;
import java.io.PrintStream;

/**
 * This class is used to mimic the semantics of java.lang.LinkageError.
 * That class cannot be used directly as doing so would potentially
 * lead to confusion as to whether an error occurred during the loading
 * of a translator class or while the translator itself was loading
 * a class.
 */
public class LinkageException extends Exception {

    /**
     * An object implementing this class can be passed to the constructor a LinkageException
     * which in turn can extract a detailed context string for the exception.
     */
    public static interface Context {
        /**
         * Return a detailed context string.
         */
        public String getContext();
    }

    /**
     * Constructor.
     * @param errorClass
     * @param details
     */
    public LinkageException(Type errorClass, String details) {
        this(errorClass, details, null, null);
    }

    /**
     * Constructor that take a context.
     * @param errorClass
     * @param details
     * @param context
     */
    public LinkageException(Type errorClass, String details, Context context) {
        this(errorClass, details, context, null);
    }

    /**
     * Constructor that takes a cause.
     * @param errorClass
     * @param details
     * @param cause
     */
    public LinkageException(Type errorClass, String details, Throwable cause) {
        this(errorClass, details, null, cause);
    }

    /**
     * Constructor that takes a context and a cause.
     * @param errorClass
     * @param details
     * @param context
     */
    public LinkageException(Type errorClass, String details, Context context, Throwable cause) {
        super(details);
        this.errorClass = errorClass;
        this.cause      = cause;
        addContext(context);
    }

    /**
     * Return the LinkageError class represented by this exception.
     */
    public Type errorClass() {
        return errorClass;
    }

    public void addContext(Context context) {
        if (context != null) {
            if (contexts == null) {
                contexts = new Object[] {context};
            }
            else if (contexts[0] != context) {
                Object[] old = contexts;
                contexts = new Object[contexts.length + 1];
                System.arraycopy(old, 0, contexts, 1, old.length);
                contexts[0] = context;
            }
        }
    }

    /**
     *
     * @return
     */
    public String getMessage() {
        String message = errorClass + ": ";
        if (contexts != null) {
            for (int i = 0; i != contexts.length; ++i) {
                Context context = (Context)contexts[i];
                message += context.getContext() + ": ";
            }
        }
        message += super.getMessage();
        if (cause != null) {
            if (message.length() != 0) {
                message += " (cause: ";
            }
            message += cause + ")";
        }
        return message;
    }

    public void printStackTrace(PrintStream err) {
        super.printStackTrace(err);
        if (cause != null) {
            err.print("Caused by ");
            cause.printStackTrace(err);
        }
    }

    /** The LinkageError (sub)class represented by this exception. */
    private final Type errorClass;
    /** The cause of this exception (if any). */
    private final Throwable cause;
    /** The context(s) of this exception (if any). */
    private Object[] contexts;
}