/*
 * @(#)SignatureIterator.java	1.2 01/11/27 19:36:45
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend.oops;

import javac1.BasicType;
import javac1.ci.Symbol;

/** <P> SignatureIterators iterate over a Java signature (or parts of it).
    (Syntax according to: "The Java Virtual Machine Specification" by
    Tim Lindholm & Frank Yellin; section 4.3 Descriptors; p. 89ff.) </P>

    <P> Example: Iterating over
<PRE>
([Lfoo;D)I
0123456789
</PRE>

    using </P>

<PRE>
iterateParameters() calls: do_array(2, 7); do_double();
iterateReturntype() calls:                              do_int();
iterate()           calls: do_array(2, 7); do_double(); do_int();

is_returnType()        is: false         ; false      ; true
</PRE>
*/

public abstract class SignatureIterator {
  protected Symbol _signature;       // the signature to iterate over
  protected int    _index;           // the current character index (only valid during iteration)
  protected int    _parameter_index; // the current parameter index (0 outside iteration phase)
 
  protected void expect(char c) {
    if (_signature.toString().charAt(_index) != c) {
      throw new RuntimeException("expecting '" + c + "'");
    }
    _index++;
  }
  protected void skipOptionalSize() {
    char c = _signature.toString().charAt(_index);
    while ('0' <= c && c <= '9') {
      c = _signature.toString().charAt(++_index);
    }
  }
  // returns the parameter size in words (0 for void)
  protected int parseType() {
    switch(_signature.toString().charAt(_index)) {
    case 'B': doByte  (); _index++; return BasicType.TYPE_TO_SIZE[BasicType.BYTE];
    case 'C': doChar  (); _index++; return BasicType.TYPE_TO_SIZE[BasicType.CHAR];
    case 'D': doDouble(); _index++; return BasicType.TYPE_TO_SIZE[BasicType.DOUBLE];
    case 'F': doFloat (); _index++; return BasicType.TYPE_TO_SIZE[BasicType.FLOAT];
    case 'I': doInt   (); _index++; return BasicType.TYPE_TO_SIZE[BasicType.INT];
    case 'J': doLong  (); _index++; return BasicType.TYPE_TO_SIZE[BasicType.LONG];
    case 'S': doShort (); _index++; return BasicType.TYPE_TO_SIZE[BasicType.SHORT];
    case 'Z': doBool  (); _index++; return BasicType.TYPE_TO_SIZE[BasicType.BOOLEAN];
    case 'V':
      {
        if (!isReturnType()) {
          throw new RuntimeException("illegal parameter type V (void)");
        }

        doVoid(); _index++;
        return BasicType.TYPE_TO_SIZE[BasicType.VOID];
      }
    case 'L':
      {
        int begin = ++_index;
        while (_signature.toString().charAt(_index++) != ';') {}
        doObject(begin, _index);
        return BasicType.TYPE_TO_SIZE[BasicType.OBJECT];
      }
    case '[':
      {
        int begin = ++_index;
        skipOptionalSize();
        while (_signature.toString().charAt(_index) == '[') {
          _index++;
          skipOptionalSize();
        }
        if (_signature.toString().charAt(_index) == 'L') {
          while (_signature.toString().charAt(_index++) != ';') {}
        } else {
          _index++;
        }
        doArray(begin, _index);
        return BasicType.TYPE_TO_SIZE[BasicType.ARRAY];
      }
    }
    throw new RuntimeException("Should not reach here");
  }
  protected void checkSignatureEnd() {
    if (_index < _signature.toString().length()) {
      System.err.println("too many chars in signature ");
      System.err.println(_signature.toString());
      System.err.println(" @ " + _index);
    }
  }

  public SignatureIterator(Symbol signature) {
    _signature       = signature;
    _parameter_index = 0;
  }

  //
  // Iteration
  //

  // dispatches once for field signatures
  public void dispatchField() {
    // no '(', just one (field) type
    _index = 0;
    _parameter_index = 0;
    parseType();
    checkSignatureEnd();
  }

  // iterates over parameters only
  public void iterateParameters() {
    // Parse parameters
    _index = 0;
    _parameter_index = 0;
    expect('(');
    while (_signature.toString().charAt(_index) != ')') {
      _parameter_index += parseType();
    }
    expect(')');
    _parameter_index = 0; // so isReturnType() is false outside iteration
  }

  // iterates over returntype only
  public void iterateReturntype() {
    // Ignore parameters
    _index = 0;
    expect('(');
    while (_signature.toString().charAt(_index) != ')') {
      _index++;
    }
    expect(')');
    // Parse return type
    _parameter_index = -1;
    parseType();
    checkSignatureEnd();
    _parameter_index = 0; // so isReturnType() is false outside iteration
  }

  // iterates over whole signature
  public void iterate() {
    // Parse parameters
    _index = 0;
    _parameter_index = 0;
    expect('(');
    while (_signature.toString().charAt(_index) != ')') {
      _parameter_index += parseType();
    }
    expect(')');
    // Parse return type
    _parameter_index = -1;
    parseType();
    checkSignatureEnd();
    _parameter_index = 0; // so isReturnType() is false outside iteration
  }

  // Returns the word index of the current parameter; returns a negative value at the return type 
  public int  parameterIndex()               { return _parameter_index; }
  public boolean isReturnType()              { return (parameterIndex() < 0); }

  // Basic types
  public abstract void doBool  ();
  public abstract void doChar  ();
  public abstract void doFloat ();
  public abstract void doDouble();
  public abstract void doByte  ();
  public abstract void doShort ();
  public abstract void doInt   ();
  public abstract void doLong  ();
  public abstract void doVoid  ();

  // Object types (begin indexes the first character of the entry, end
  // indexes the first character after the entry)
  public abstract void doObject(int begin, int end);
  public abstract void doArray (int begin, int end);
}
