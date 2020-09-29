package com.sun.squawk.translator.suite;

import com.sun.squawk.translator.Translator;
import com.sun.squawk.translator.loader.LinkageException;

/**
 * Implementations of this interface are used by the translator to load a
 * suite from a suite file.
 */
public interface SuiteLoader {
    /**
     * Load a suite from a specified file.
     * @param vm The translation context.
     * @param fileName The file of the file to load.
     * @return the name of the suite loaded.
     * @throws LinkageException
     */
    public String loadSuite(Translator vm, String fileName) throws LinkageException;
}