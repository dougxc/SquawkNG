//if[SQUAWK]
/*
 * Copyright 1994-2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */

package java.lang;

public class String {

    char[] value;
    int offset;
    int count;
/*if[EXCLUDE]*/


    public String()                                         { throw new Error(); }
    public String(String value)                             { throw new Error(); }
    public String(char value[])                             { throw new Error(); }
    public String(char value[], int offset, int count)      { throw new Error(); }

    public int length()                                     { throw new Error(); }
    public char charAt(int index)                           { throw new Error(); }
    public boolean equals(Object anObject)                  { throw new Error(); }
    public boolean startsWith(String prefix)                { throw new Error(); }
    public boolean endsWith(String suffix)                  { throw new Error(); }
    public int hashCode()                                   { throw new Error(); }
    public int indexOf(int ch)                              { throw new Error(); }
    public int lastIndexOf(int ch)                          { throw new Error(); }
    public int indexOf(String str)                          { throw new Error(); }
    public String substring(int beginIndex)                 { throw new Error(); }
    public String substring(int beginIndex, int endIndex)   { throw new Error(); }
    public String replace(char oldChar, char newChar)       { throw new Error(); }
    public String toLowerCase()                             { throw new Error(); }
    public String toUpperCase()                             { throw new Error(); }
    public String trim()                                    { throw new Error(); }
    public String toString()                                { throw new Error(); }
    public char[] toCharArray()                             { throw new Error(); }
/*end[EXCLUDE]*/

}
