/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/* 
 * @(#)AID.java	1.14 02/05/01
 */

package javacard.framework;

import com.sun.javacard.impl.NativeMethods;

/**
 * This class encapsulates the Application Identifier (AID) associated with
 * an applet. An AID is defined in ISO 7816-5 to be a sequence of bytes between
 * 5 and 16 bytes in length.<p>
 *
 * The JCRE creates instances of <code>AID</code> class to identify and manage every applet on 
 * the card. Applets need not create instances of this class.
 * An applet may request and use the JCRE 
 * owned instances to identify itself and other applet instances.
 * <p>JCRE owned instances of <code>AID</code> are permanent JCRE Entry Point Objects
 * and can be accessed from any applet context. References to these permanent objects
 * can be stored and re-used.
 * <p>An applet instance can obtain a reference to JCRE owned instances of its own <code>AID</code>
 * object by using the <code>JCSystem.getAID()</code> method and another applet's <code>AID</code> object
 * via the <code>JCSystem.lookupAID()</code> method.
 * <p>An applet uses <code>AID</code> instances to request to share another applet's
 * object or to control access to its own shared object from another applet.
 * See <em>Java Card Runtime Environment (JCRE) Specification</em>, section 6.2 for details.</em>
 * @see JCSystem
 * @see SystemException
 */
public class AID{

  byte[] theAID;

  /**
   * The JCRE uses this constructor to create a new <code>AID</code> instance
   * encapsulating the specified AID bytes.
   * @param bArray the byte array containing the AID bytes.
   * @param offset the start of AID bytes in bArray.
   * @param length the length of the AID bytes in bArray.
   * @exception SecurityException if the <CODE>bArray</CODE> array is not accessible in the caller's context.
   * @exception SystemException with the following reason code:<ul>
   * <li><code>SystemException.ILLEGAL_VALUE</code> if the <code>length</code> parameter is
   * less than <code>5</code> or greater than <code>16</code>.
   * </ul>
   * @throws NullPointerException if the <CODE>bArray</CODE> parameter is <CODE>null</CODE>
   * @throws ArrayIndexOutOfBoundsException if the <CODE>offset</CODE> parameter or <CODE>length</CODE> parameter is negative or
   * if <CODE>offset+length</CODE> is greater than the length of the
   * <CODE>bArray</CODE> parameter
   */
  public AID( byte[] bArray, short offset, byte length )
    throws SystemException, NullPointerException, ArrayIndexOutOfBoundsException, SecurityException{
      NativeMethods.checkArrayArgs( bArray, offset, length );
      NativeMethods.checkPreviousContextAccess(bArray);
      if (length < 5 || length>16) SystemException.throwIt(SystemException.ILLEGAL_VALUE);
      theAID = new byte[length];
      Util.arrayCopy( bArray, offset, theAID, (short)0, length );
  }

  /**
   * Called to get all the AID bytes encapsulated within <code>AID</code> object.
   * @param dest byte array to copy the AID bytes.
   * @param offset within dest where the AID bytes begin.
   * @return the length of the AID bytes.
   * @exception SecurityException if the <CODE>dest</CODE> array is not accessible in the caller's context.
   * @throws NullPointerException if the <CODE>dest</CODE> parameter is <CODE>null</CODE>
   * @throws ArrayIndexOutOfBoundsException if the <CODE>offset</CODE> parameter is negative
   * or <CODE>offset+</CODE>length of AID bytes  is greater than the length of the <CODE>dest</CODE> array
   */
  public final byte getBytes (byte[] dest, short offset)
    throws NullPointerException, ArrayIndexOutOfBoundsException, SecurityException{
      NativeMethods.checkArrayArgs( dest, offset, (short)theAID.length );
      NativeMethods.checkPreviousContextAccess(dest);
      Util.arrayCopy( theAID, (short)0, dest, offset, (short)theAID.length );
      return (byte) theAID.length;}

  /**
   * Compares the AID bytes in <code>this</code> <code>AID</code> instance to the AID bytes in the
   * specified object.
   * The result is <code>true</code> if and only if the argument is not <code>null</code>
   * and is an <code>AID</code> object that encapsulates the same AID bytes as <code>this</code>
   * object.
   * <p>This method does not throw <code>NullPointerException</code>.
   * @param anObject the object to compare <code>this</code> <code>AID</code> against.
   * @return <code>true</code> if the AID byte values are equal, <code>false</code> otherwise.
   * @exception SecurityException if <CODE>anObject</CODE> object is not accessible in the caller's context.
   */
  public final boolean equals( Object anObject ) throws SecurityException
  {
    if (anObject==null) {
      return false;
    }
    NativeMethods.checkPreviousContextAccess(anObject);
    if ( ! (anObject instanceof AID) || ((AID)anObject).theAID.length != theAID.length) return false; 
    return (Util.arrayCompare(((AID)anObject).theAID, (short)0, theAID, (short)0, (short)theAID.length) == 0);
  }

  /**
   * Checks if the specified AID bytes in <code>bArray</code> are the same as those encapsulated
   * in <code>this</code> <code>AID</code> object.
   * The result is <code>true</code> if and only if the <code>bArray</code> argument is not <code>null</code>
   * and the AID bytes encapsulated in <code>this</code> <code>AID</code> object are equal to
   * the specified AID bytes in <code>bArray</code>.
   * <p>This method does not throw <code>NullPointerException</code>.
   * @param bArray containing the AID bytes
   * @param offset within bArray to begin
   * @param length of AID bytes in bArray
   * @return <code>true</code> if equal, <code>false</code> otherwise.
   * @exception SecurityException if the <CODE>bArray</CODE> array is not accessible in the caller's context.
   * @throws ArrayIndexOutOfBoundsException if the <CODE>offset</CODE> parameter or <CODE>length</CODE> parameter is negative or
   * if <CODE>offset+length</CODE> is greater than the length of the
   * <CODE>bArray</CODE> parameter
   */
  public final boolean equals( byte[] bArray, short offset, byte length )
    throws ArrayIndexOutOfBoundsException, SecurityException{
      try {
        NativeMethods.checkArrayArgs( bArray, offset, length );
      }catch (NullPointerException ne){
        return false;
      }
      NativeMethods.checkPreviousContextAccess(bArray);

      return ((length == theAID.length) &&
	      (Util.arrayCompare(bArray, offset, theAID, (short)0, length) == 0));
  }

  /**
   * Checks if the specified partial AID byte sequence matches the first <code>length</code> bytes
   * of the encapsulated AID bytes within <code>this</code> <code>AID</code> object.
   * The result is <code>true</code> if and only if the <code>bArray</code> argument is not <code>null</code>
   * and the input <code>length</code> is less than or equal to the length of the encapsulated AID
   * bytes within <code>this</code> <code>AID</code> object and the specified bytes match.
   * <p>This method does not throw <code>NullPointerException</code>.
   * @param bArray containing the partial AID byte sequence
   * @param offset within bArray to begin
   * @param length of partial AID bytes in bArray
   * @return <code>true</code> if equal, <code>false</code> otherwise.
   * @exception SecurityException if the <CODE>bArray</CODE> array is not accessible in the caller's context.
   * @throws ArrayIndexOutOfBoundsException if the <CODE>offset</CODE> parameter or <CODE>length</CODE> parameter is negative or
   * if <CODE>offset+length</CODE> is greater than the length of the
   * <CODE>bArray</CODE> parameter
   */
  public final boolean partialEquals( byte[] bArray, short offset, byte length )
    throws ArrayIndexOutOfBoundsException, SecurityException{
      try {
        NativeMethods.checkArrayArgs( bArray, offset, length );
      }catch (NullPointerException ne){
        return false;
      }
      NativeMethods.checkPreviousContextAccess(bArray);
      if (length > theAID.length) return false;
      return (Util.arrayCompare(bArray, offset, theAID, (short)0, length) == 0);
  }
   
  /**
   * Checks if the RID (National Registered Application provider identifier) portion of the encapsulated
   * AID bytes within the <code>otherAID</code> object matches
   * that of <code>this</code> <code>AID</code> object.
   * The first 5 bytes of an AID byte sequence is the RID. See ISO 7816-5 for details.
   * The result is <code>true</code> if and only if the argument is not <code>null</code>
   * and is an <code>AID</code> object that encapsulates the same RID bytes as <code>this</code>
   * object.
   * <p>This method does not throw <code>NullPointerException</code>.
   * @param otherAID the <code>AID</code> to compare against.
   * @return <code>true</code> if the RID bytes match, <code>false</code> otherwise.
   * @exception SecurityException if the <CODE>otherAID</CODE> object is not accessible in the caller's context.
   */
  public final boolean RIDEquals ( AID otherAID ) throws SecurityException
  {
    if (otherAID==null) return false;
    NativeMethods.checkPreviousContextAccess(otherAID);
    if ( Util.arrayCompare( theAID, (short)0, otherAID.theAID, (short)0, (short)5 ) == 0 )
      return true;
    return false;
  }
  
  /**
   * Called to get part of the AID bytes encapsulated within the <code>AID</code> object starting
   * at the specified offset for the specified length.
   * @param aidOffset offset within AID array to begin copying bytes.
   * @param dest the destination byte array to copy the AID bytes into.
   * @param oOffset offset within dest where the output bytes begin.
   * @param oLength the length of bytes requested in <code>dest</code>. <code>0</code>
   * implies a request to copy all remaining AID bytes.
   * @return the actual length of the bytes returned in <code>dest</code>.
   * @exception SecurityException if the <CODE>dest</CODE> array is not accessible in the caller's context.
   * @throws NullPointerException if the <CODE>dest</CODE> parameter is <CODE>null</CODE>
   * @throws ArrayIndexOutOfBoundsException if the <code>aidOffset</code> parameter is
   * negative or greater than the length of the encapsulated AID bytes or the
   * <CODE>oOffset</CODE> parameter is negative
   * or <CODE>oOffset+length of bytes requested is greater than the length of the
   * <CODE>dest</CODE> array
   */
  public final byte getPartialBytes (short aidOffset, byte[] dest, short oOffset, byte oLength)
    throws NullPointerException, ArrayIndexOutOfBoundsException, SecurityException{
      short copyLen= oLength;
      if (oLength == (short)0) {
	copyLen = (short) (theAID.length - aidOffset);
      }
      NativeMethods.checkArrayArgs( dest, oOffset, copyLen );
      NativeMethods.checkPreviousContextAccess(dest);
      Util.arrayCopy( theAID, aidOffset, dest, oOffset, copyLen );
      return (byte) copyLen;
  }
}
