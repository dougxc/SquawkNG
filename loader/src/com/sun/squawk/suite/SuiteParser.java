package com.sun.squawk.suite;

import java.io.*;

/**
 * This interface is implemented to provide the SuiteLoader with a means for
 * loading suites from an implementation dependent source.
 */
public interface SuiteParser {

    /**
     * Read a suite header from the source.
     *
     * @return the suite header read.
     * @throws IOException
     */
    public SuiteHeader readSuiteHeader() throws IOException;

    /**
     * Read a class from the source.
     *
     * @return the class read or null if there is no class at the current
     * position in the input stream.
     * @throws IOException
     */
    public SuiteClass readClass() throws IOException;

    /**
     * Read a sequence of 'Attribute_info' structures from the source that are
     * associated with a given class.
     *
     * @param sc The class to which the attributes pertain.
     * @throws IOException
     */
    public void readClassAttributes(SuiteClass sc) throws IOException;

    /**
     * Read a method implementation from the source.
     *
     * @return the method implementation read or null if there is no method
     * implementation at the current position in the input stream.
     * @throws IOException
     */
    public SuiteMethodImpl readMethodImpl() throws IOException;

    /**
     * Read a sequence of 'Attribute_info' structures from the source that are
     * associated with a given method implementation.
     *
     * @param methodImpl The method implementation to which the
     * attributes pertain.
     * @throws IOException
     */
    public void readMethodImplAttributes(SuiteMethodImpl methodImpl) throws IOException;

    /**
     * Advance the source read position to the start of the next suite.
     *
     * @return this parser with the read position placed at the start of the
     * next suite in the source or null if the source is at EOF.
     * @throws IOException if there is a problem reading from the source
     * @throws LinkageError if the data in the source does not appear to be
     * in the format expected by the parser.
     */
    public SuiteParser nextSuite() throws IOException;

/*if[DEBUG.LOADER]*/
    /**
     * Print a collection of implementation specific statistics about the
     * state of the parser to a specified PrintStream.
     *
     * @param out
     */
    public void printStats(PrintStream out);

/*end[DEBUG.LOADER]*/

}