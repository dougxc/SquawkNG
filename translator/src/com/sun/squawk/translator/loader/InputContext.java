
package com.sun.squawk.translator.loader;
import com.sun.squawk.translator.Type;

/**
 * An object implementing this class can be passed to the constructor a LinkageException
 * which in turn can extract a detailed context string for the exception.
 */
public interface InputContext {
    /**
     * Raise a LinkageException for a verification exception based one a code and a message.
     * @param code An index into a table of error messages for well known verification errors. This
     * parameter is ignored if it is less than 0 and it is an error if it is greater than or
     * equal to the length of the error message table being indexed.
     * @param msg An optional message component that is appended to the detailed message for the
     * generated LinkageException if it is not null.
     */
    public void verificationException(int code, String msg) throws LinkageException ;

    /**
     * Raise a LinkageException.
     * @param linkageErrorClass The subclass of LinkageError error class this represents.
     * @param msg An optional message component that is appended to the detailed message for the
     * generated LinkageException if it is not null.
     */
    public void linkageException(Type linkageErrorClass, String msg) throws LinkageException ;

    /**
     * Raise a LinkageException for a class format error.
     * @param errorClassName The error class in java.lang that this represents.
     * @param msg An optional message component that is appended to the detailed message for the
     * generated LinkageException if it is not null.
     */
    public void classFormatException(String msg) throws LinkageException ;
}
