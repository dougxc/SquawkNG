/*
 * Copyright � 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */


/*
 * @(#)JCSystem.java	1.34 02/05/01
 */

package javacard.framework;

import com.sun.javacard.impl.PrivAccess;
import com.sun.javacard.impl.NativeMethods;
import com.sun.javacard.impl.GarbageCollector;
import com.sun.javacard.impl.PackedBoolean;
import com.sun.javacard.impl.AppletMgr;

/**
 * The <code>JCSystem</code> class includes a collection of methods to control
 * applet execution, resource management, atomic transaction management,
 * object deletion mechanism and inter-applet object sharing in Java Card.
 * All methods in <code>JCSystem</code> class are static methods.<p>
 *
 * The JCSystem class also includes methods to control the persistence
 * and transience of objects. The term <em>persistent</em> means that objects and their values
 * persist from one CAD session to the next, indefinitely.
 * Persistent object values are updated atomically using transactions.<p>
 * The <code>makeTransient...Array()</code> methods can be used to create <em>transient</em> arrays.
 * Transient array data is lost (in an undefined state, but the real data is unavailable)
 * immediately upon power loss, and is reset to the default value at the occurrence of certain events
 * such as card reset or deselect.
 * Updates to the values of transient arrays are not atomic and are not affected by transactions.
 * <p>
 * The JCRE maintains an atomic transaction commit buffer which is initialized on card reset
 * (or power on).
 * When a transaction is in progress, the JCRE journals all updates to persistent data space into
 * this buffer so that it can always guarantee, at commit time, that everything in the buffer
 * is written or nothing at all is written.
 * The <code>JCSystem</code> includes methods to  control an atomic transaction.
 * See <em>Java Card Runtime Environment (JCRE) Specification</em> for details.
 * <p>
 * @see SystemException
 * @see TransactionException
 * @see Applet
 */

public final class JCSystem  {

  private static final short API_VERSION = 0x0202;
    
  /** Constant to indicate persistent memory type
   */    
  public static final byte MEMORY_TYPE_PERSISTENT = 0;
  /** Constant to indicate transient memory of CLEAR_ON_RESET type
   */    
  public static final byte MEMORY_TYPE_TRANSIENT_RESET = 1;
  /** Constant to indicate transient memory of CLEAR_ON_DESELECT type
   */    
  public static final byte MEMORY_TYPE_TRANSIENT_DESELECT = 2;
    
  static PrivAccess thePrivAccess; // initialized by Dispatcher
    
  /**
   * Only JCRE can use constructor.
   */
  JCSystem(){}

  /**
   * This boolean shows if the implementation supports the object deletion mechanism
   */
  private static final boolean GC_SUPPORTED = true;

  /**
   * This event code indicates that the object is not transient.
   */
  public static final byte NOT_A_TRANSIENT_OBJECT = 0;

  /**
   * This event code indicates that the contents of the transient object are cleared
   * to the default value on card reset ( or power on ) event.
   */
  public static final byte CLEAR_ON_RESET = 1;

  /**
   * This event code indicates that the contents of the transient object are cleared
   * to the default value on applet deselection event or in <code>CLEAR_ON_RESET</code> cases.
   * <p>Notes:<ul>
   * <li><code>CLEAR_ON_DESELECT</code><em> transient objects can be accessed only when the applet
   * which created the object is in the same context as the currently selected applet.</em>
   * <li><em>The JCRE will throw a </em><code>SecurityException</code><em> if a
   * </em><code>CLEAR_ON_DESELECT</code><em> transient
   * object is accessed when the currently selected applet is not in the same context as
   * the applet which created the object.</em>
   * </ul>
   */
  public static final byte CLEAR_ON_DESELECT = 2;

  /**
   * Used to check if the specified object is transient.
   * <p>Notes:<ul>
   * <em>This method returns </em><code>NOT_A_TRANSIENT_OBJECT</code><em> if the specified object is 
   * <code>null</code> or is not an array type.</em>
   * </ul>
   * @param theObj the object being queried.
   * @return <code>NOT_A_TRANSIENT_OBJECT</code>, <code>CLEAR_ON_RESET</code>, or <code>CLEAR_ON_DESELECT</code>.
   * @see #makeTransientBooleanArray(short, byte)
   * @see #makeTransientByteArray(short, byte)
   * @see #makeTransientShortArray(short, byte)
   * @see #makeTransientObjectArray(short, byte)
   */
  public static native byte isTransient(Object theObj);

  /**
   * Create a transient boolean array with the specified array length.
   * @param length the length of the boolean array.
   * @param event the <code>CLEAR_ON...</code> event which causes the array elements to be cleared.
   * @throws NegativeArraySizeException if the <CODE>length</CODE> parameter is negative
   * @exception SystemException with the following reason codes:<ul>
   * <li><code>SystemException.ILLEGAL_VALUE</code> if event is not a valid event code.
   * <li><code>SystemException.NO_TRANSIENT_SPACE</code> if sufficient transient space is not available.
   * <li><code>SystemException.ILLEGAL_TRANSIENT</code> if the current applet context
   * is not the currently selected applet context and <code>CLEAR_ON_DESELECT</code> is specified.
   * </ul>
   * @return the new transient boolean array
   */
  public static native boolean[] makeTransientBooleanArray(short length, byte event)
  throws NegativeArraySizeException, SystemException;

  /**
   * Create a transient byte array with the specified array length.
   * @param length the length of the byte array.
   * @param event the <code>CLEAR_ON...</code> event which causes the array elements to be cleared.
   * @throws NegativeArraySizeException if the <CODE>length</CODE> parameter is negative
   * @exception SystemException with the following reason codes:<ul>
   * <li><code>SystemException.ILLEGAL_VALUE</code> if event is not a valid event code.
   * <li><code>SystemException.NO_TRANSIENT_SPACE</code> if sufficient transient space is not available.
   * <li><code>SystemException.ILLEGAL_TRANSIENT</code> if the current applet context
   * is not the currently selected applet context and <code>CLEAR_ON_DESELECT</code> is specified.
   * </ul>
   * @return the new transient byte array
   */
  public static native byte[] makeTransientByteArray(short length, byte event)
  throws NegativeArraySizeException, SystemException;
    
  /**
   * Create a transient short array with the specified array length.
   * @param length the length of the short array.
   * @param event the <code>CLEAR_ON...</code> event which causes the array elements to be cleared.
   * @throws NegativeArraySizeException if the <CODE>length</CODE> parameter is negative
   * @exception SystemException with the following reason codes:<ul>
   * <li><code>SystemException.ILLEGAL_VALUE</code> if event is not a valid event code.
   * <li><code>SystemException.NO_TRANSIENT_SPACE</code> if sufficient transient space is not available.
   * <li><code>SystemException.ILLEGAL_TRANSIENT</code> if the current applet context
   * is not the currently selected applet context and <code>CLEAR_ON_DESELECT</code> is specified.
   * </ul>
   * @return the new transient short array
   */
  public static native short[] makeTransientShortArray(short length, byte event)
  throws NegativeArraySizeException, SystemException;
    
  /**
   * Create a transient array of <code>Object</code> with the specified array length.
   * @param length the length of the <code>Object</code> array.
   * @param event the <code>CLEAR_ON...</code> event which causes the array elements to be cleared.
   * @throws NegativeArraySizeException if the <CODE>length</CODE> parameter is negative
   * @exception SystemException with the following reason codes:<ul>
   * <li><code>SystemException.ILLEGAL_VALUE</code> if event is not a valid event code.
   * <li><code>SystemException.NO_TRANSIENT_SPACE</code> if sufficient transient space is not available.
   * <li><code>SystemException.ILLEGAL_TRANSIENT</code> if the current applet context
   * is not the currently selected applet context and <code>CLEAR_ON_DESELECT</code> is specified.
   * </ul>
   * @return the new transient Object array
   */
  public static native Object[] makeTransientObjectArray(short length, byte event)
  throws NegativeArraySizeException, SystemException;

  /**
   * Returns the current major and minor version of the Java Card API.
   * @return version number as byte.byte (major.minor)
   */
  public static short getVersion()
  {
    return API_VERSION;
  }

  /**
   * Returns the JCRE owned instance of the <code>AID</code> object associated with
   * the current applet context.
   * Returns <code>null</code> if the <code>Applet.register()</code> method
   * has not yet been invoked.
   * <p>JCRE owned instances of <code>AID</code> are permanent JCRE
   * Entry Point Objects and can be accessed from any applet context.
   * References to these permanent objects can be stored and re-used.
   * <p>See <em>Java Card Runtime Environment (JCRE) Specification</em>, section 6.2.1 for details.
   * @return the <code>AID</code> object.
   */
  public static AID getAID()
  {
    return thePrivAccess.getAID( PrivAccess.getCurrentAppID() );
  }

  /**
   * Returns the JCRE owned instance of the <code>AID</code> object, if any, 
   * encapsulating the specified AID bytes in the <code>buffer</code> parameter
   * if there exists a successfully installed applet on the card whose instance AID
   * exactly matches that of the specified AID bytes.
   * <p>JCRE owned instances of <code>AID</code> are permanent JCRE
   * Entry Point Objects and can be accessed from any applet context.
   * References to these permanent objects can be stored and re-used.
   * <p>See <em>Java Card Runtime Environment (JCRE) Specification</em>, section 6.2.1 for details.
   * @param buffer byte array containing the AID bytes.
   * @param offset offset within buffer where AID bytes begin.
   * @param length length of AID bytes in buffer.
   * @return the <code>AID</code> object, if any; <code>null</code> otherwise. A VM exception
   * is thrown if <code>buffer</code> is <code>null</code>,
   * or if <code>offset</code> or <code>length</code> are out of range.
   */
  public static AID lookupAID( byte[] buffer, short offset, byte length )
  {
    byte test = buffer[0]; // throw NullPointerException if buffer==null
    return thePrivAccess.getAID(buffer, offset, length);
  }

  /**
   * Begins an atomic transaction. If a transaction is already in
   * progress (transaction nesting depth level != 0), a TransactionException is
   * thrown.
   * <p>Note:<ul>
   * <li><em>This method may do nothing if the <code>Applet.register()</code>
   * method has not yet been invoked. In case of tear or failure prior to successful
   * registration, the JCRE will roll back all atomically updated persistent state.</em>
   * </ul>
   * @exception TransactionException with the following reason codes:<ul>
   * <li><code>TransactionException.IN_PROGRESS</code> if a transaction is already in progress.</ul>
   * @see #commitTransaction()
   * @see #abortTransaction()
   */
  public static void beginTransaction() throws TransactionException{
    if(!AppletMgr.installTransactionFlag){
      NativeMethods.beginTransactionNative();
    }
  }
 
  /**
   * Aborts the atomic transaction. The contents of the commit
   * buffer is discarded.
   * <p>Notes:<ul>
   * <li><em>This method may do nothing if the <code>Applet.register()</code>
   * method has not yet been invoked. In case of tear or failure prior to successful
   * registration, the JCRE will roll back all atomically updated persistent state.</em>
   * <li><em>Do not call this method from within a transaction which creates new objects because
   * the JCRE may not recover the heap space used by the new object instances.</em>
   * <li><em>Do not call this method from within a transaction which creates new objects because
   * the JCRE may, to ensure the security of the card and to avoid heap space loss,
   * lock up the card session to force tear/reset processing.</em> 
   * <li><em>The JCRE ensures that any variable of reference type which references an object
   * instantiated from within this aborted transaction is equivalent to 
   * a </em><code>null</code><em> reference.</em>  
   * </ul>
   * @exception TransactionException with the following reason codes:<ul>
   * <li><code>TransactionException.NOT_IN_PROGRESS</code> if a transaction is not in progress.</ul>
   * @see #beginTransaction()
   * @see #commitTransaction()
   */
  public static void abortTransaction() throws TransactionException{
    if(!AppletMgr.installTransactionFlag){        
      NativeMethods.abortTransactionNative();
    }
  }

  /**
   * Commits an atomic transaction. The contents of commit
   * buffer is atomically committed. If a transaction is not in
   * progress (transaction nesting depth level == 0) then a TransactionException is
   * thrown.
   * <p>Note:<ul>
   * <li><em>This method may do nothing if the <code>Applet.register()</code>
   * method has not yet been invoked. In case of tear or failure prior to successful
   * registration, the JCRE will roll back all atomically updated persistent state.</em>
   * </ul> 
   * @exception TransactionException with the following reason codes:<ul>
   * <li><code>TransactionException.NOT_IN_PROGRESS</code> if a transaction is not in progress.</ul>
   * @see #beginTransaction()
   * @see #abortTransaction()
   */
  public static void commitTransaction() throws TransactionException{
    if(!AppletMgr.installTransactionFlag){        
      NativeMethods.commitTransactionNative();
    }
  }

  /**
   * Returns the current transaction nesting depth level. At present,
   * only 1 transaction can be in progress at a time.
   * @return 1 if transaction in progress, 0 if not.
   */
  public static native byte getTransactionDepth();

  /**
   * Returns the number of bytes left in the commit buffer.
   * <p> Note :<ul>
   * <li><em>If the number of bytes left in the commit buffer is greater than
   * 32767, then this method returns 32767.</em>
   * </ul>
   * @return the number of bytes left in the commit buffer
   * @see #getMaxCommitCapacity()
   */
  public static native short getUnusedCommitCapacity();

  /**
   * Returns the total number of bytes in the commit buffer.
   * This is approximately the maximum number of bytes of
   * persistent data which can be modified during a transaction.
   * However, the transaction subsystem requires additional bytes
   * of overhead data to be included in the commit buffer, and this
   * depends on the number of fields modified and the implementation
   * of the transaction subsystem. The application cannot determine
   * the actual maximum amount of data which can be modified during
   * a transaction without taking these overhead bytes into consideration.
   * <p> Note :<ul>
   * <li><em>If the total number of bytes in the commit buffer is greater than
   * 32767, then this method returns 32767.</em>
   * </ul>
   * @return the total number of bytes in the commit buffer
   * @see #getUnusedCommitCapacity()
   */
  public static native short getMaxCommitCapacity();

  /**
   * This method is called to obtain the JCRE owned instance of the <code>AID</code> object associated
   * with the previously active applet context. This method is typically used by a server applet,
   * while executing a shareable interface method to determine the identity of its client and
   * thereby control access privileges.
   * <p>JCRE owned instances of <code>AID</code> are permanent JCRE
   * Entry Point Objects and can be accessed from any applet context.
   * References to these permanent objects can be stored and re-used.
   * <p>See <em>Java Card Runtime Environment (JCRE) Specification</em>, section 6.2.1 for details.
   * @return the <code>AID</code> object of the previous context, or <code>null</code> if JCRE.
   */
  public static AID getPreviousContextAID()
  {
    byte prevCtx = NativeMethods.getPreviousContext();

    if (prevCtx == PrivAccess.JCRE_CONTEXTID) {
      return null;
    }

    return thePrivAccess.getAID((byte)(prevCtx & PrivAccess.APPID_BITMASK));
  }

  /**
   * This method is called to obtain the amount of memory of the specified
   * type that is available to the applet. Note that implementation
   * dependent memory overhead structures may also use the same memory pool.
   * <p>Notes:<ul>
   * <li><em>The number of bytes returned is only an upper bound on the amount
   * of memory available due to overhead requirements.</em>
   * <li><em>Allocation of CLEAR_ON_RESET transient objects may affect the
   * amount of CLEAR_ON_DESELECT transient memory available.</em>
   * <li><em>Allocation of CLEAR_ON_DESELECT transient objects may affect the
   * amount of CLEAR_ON_RESET transient memory available.</em>
   * <li><em>If the number of available bytes is greater than 32767, then
   * this method returns 32767.</em>
   * <li><em>The returned count is not an indicator of the size of object which
   * may be created since memory fragmentation is possible.</em>
   * </ul>
   * @param memoryType the type of memory being queried. One of the <CODE>MEMORY_TYPE_</CODE>..
   * constants defined above. See {@link #MEMORY_TYPE_PERSISTENT}
   * @return the upper bound on available bytes of memory for the specified type
   * @exception SystemException with the following reason codes:<ul>
   * <li><code>SystemException.ILLEGAL_VALUE</code> if <code>memoryType</code> is not a
   * valid memory type.
   * </ul>
   */
  public static short getAvailableMemory(byte memoryType) throws SystemException{
    switch(memoryType) {
    case MEMORY_TYPE_PERSISTENT:
      return NativeMethods.E2P_Available();
    case MEMORY_TYPE_TRANSIENT_RESET:
      return NativeMethods.rtr_Available();
    case MEMORY_TYPE_TRANSIENT_DESELECT:
      return NativeMethods.dtr_Available();
    default:
      SystemException.throwIt(SystemException.ILLEGAL_VALUE);
    }
    return 0;
  }
    
  /**
   * This method is called by a client applet to get a server applet's
   * shareable interface object. <p>This method returns <code>null</code>
   * if the <code>Applet.register()</code> has not yet been invoked or
   * if the server does not exist or if the server returns <code>null</code>.
   * @param serverAID the AID of the server applet.
   * @param parameter optional parameter data.
   * @return the shareable interface object or <code>null</code>.
   * @see javacard.framework.Applet#getShareableInterfaceObject(AID, byte)
   */
  public static Shareable getAppletShareableInterfaceObject(AID serverAID, byte parameter)
  {  
    return thePrivAccess.getSharedObject(serverAID, parameter);
  }
    
  /**
   * This method is used to determine if the Java Card implementation supports
   * the object deletion mechanism.
   * @return <CODE>true</CODE> if the object deletion mechanism is supported, <CODE>false</CODE> otherwise.
   */
  public static boolean isObjectDeletionSupported(){
    return GC_SUPPORTED;
  }

  /**
   * This method is invoked by the applet to trigger the object deletion
   * service of the JCRE. If the JCRE implements the object deletion mechanism,
   * the request is merely logged at this time. The JCRE
   * must schedule the object deletion service prior to the
   * next invocation of the <CODE>Applet.process()</CODE> method. The object deletion
   * mechanism must ensure that : <ul>
   * <li>Any unreferenced persistent object owned by the current applet context
   * is deleted and the associated space is recovered for reuse prior to the
   * next invocation of the <CODE>Applet.process()</CODE> method.
   * <li>Any unreferenced <CODE>CLEAR_ON_DESELECT</CODE> or <CODE>CLEAR_ON_RESET</CODE>
   * transient object owned by the current applet context is deleted
   * and the associated space is recovered for reuse before the next card reset session.
   * </ul>
   * @exception SystemException with the following reason codes:<ul>
   * <li><code>SystemException.ILLEGAL_USE</code> if the object deletion mechanism is
   * not implemented.
   * </ul>
   */
  public static void requestObjectDeletion() throws SystemException{
    thePrivAccess.setGCRequestedFlag(true);
  }

  /**
   * This method is called to obtain the logical channel number assigned to
   * the currently selected applet instance. The assigned logical channel is
   * the logical channel on which the currently selected applet instance is
   * or will be the active applet instance. This logical channel number is always
   * equal to the origin logical channel number returned by the APDU.getCLAChannel()
   * method except during selection and deselection via the MANAGE CHANNEL APDU command.
   * If this method is called from the <CODE>Applet.select()</CODE>, <CODE>Applet.deselect(</CODE>),
   * <CODE>MultiSelectable.select(boolean)</CODE> and <CODE>MultiSelectable.deselect(boolean)</CODE> methods
   * during MANAGE CHANNEL APDU command processing, the logical channel number
   * returned may be different.
   * @return the logical channel number in the range 0-3 assigned to the
   * currently selected applet instance.
   */
  public static byte getAssignedChannel()
  {
    return NativeMethods.getCurrentlySelectedChannel();
  }

}
