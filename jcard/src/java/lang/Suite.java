package java.lang;

public class Suite {

    // These fields are required (in this order) for the romizer to accept this class.
    Class[]  classes;
    String   name;
    String[] dependentSuiteNames;
    int      references;

    static Suite[] dummy;
}


