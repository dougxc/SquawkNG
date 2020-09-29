/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
// Workfile:@(#)Checksum.java	1.4
// Version:1.4
// Date:05/01/02
// 
// Archive:  api21/javacard/security/Checksum.java 
// Modified:05/01/02 16:39:01
// */

package javacard.security;

/**
 * The <code>Checksum</code> class is the base class for CRC (cyclic redundancy check) checksum algorithms.
 * Implementations of Checksum algorithms must extend this class and implement all the abstract methods.
 * <p> A tear or card reset event resets a 
 * <code>Checksum</code> object to the initial state (state upon construction).
 * <p> Even if a transaction is in progress, update of intermediate result state in the implementation
 * instance shall not participate in the transaction.
 */

abstract public class Checksum{

    // algorithm options

    /**
     * ISO/IEC 3309 compliant 16 bit CRC algorithm. This algorithm uses the generator
     * polynomial : x^16+x^12+x^5+1. The default initial checksum value used by this alogrithm
     * is 0.
    */
    public static final byte ALG_ISO3309_CRC16        = 1;

   /**
    * ISO/IEC 3309 compliant 32 bit CRC algorithm. This algorithm uses the generator
    * polynomial : X^32 +X^26 +X^23 +X^22 +X^16 +X^12 +X^11 +X^10 +X^8 +X^7 +X^5 +X^4 +X^2 +X +1.
    * The default initial checksum value used by this alogrithm is 0.
    */
    public static final byte ALG_ISO3309_CRC32        = 2;


   /**
    * Creates a <code>Checksum</code> object instance of the selected algorithm.
    * @param algorithm the desired checksum algorithm.
    * Valid codes listed in ALG_ .. constants above e.g. {@link #ALG_ISO3309_CRC16}
    * @param externalAccess <code>true</code> indicates that the instance will be shared among
    * multiple applet instances and that the <code>Checksum</code> instance will also be accessed (via a <code>Shareable</code>.
    * interface) when the owner of the <code>Checksum</code> instance is not the currently selected applet.
    * If <code>true</code> the implementation must not allocate CLEAR_ON_DESELECT transient space for internal data.
    * @return the <code>Checksum</code> object instance of the requested algorithm.
    * @exception CryptoException with the following reason codes:<ul>
    * <li><code>CryptoException.NO_SUCH_ALGORITHM</code> if the requested algorithm
    * or shared access mode is not supported.</ul>
    */
    public static final Checksum getInstance(byte algorithm, boolean externalAccess) throws CryptoException{
        CryptoException.throwIt( CryptoException.NO_SUCH_ALGORITHM);
	return null;
	}

   /**
    *  Protected Constructor
    */
    protected Checksum(){}

   /**
    * Resets and initializes the <code>Checksum</code> object with the algorithm specific
    * parameters.
    * <p>Note:<ul>
    * <li><em>The ALG_ISO3309_CRC16 algorithm expects 2 bytes of parameter information in 
    * </em><code>bArray</code><em> representing the initial checksum value.</em>
    * <li><em>The ALG_ISO3309_CRC32 algorithm expects 4 bytes of parameter information in 
    * </em><code>bArray</code><em> representing the initial checksum value.</em>
    * </ul>
    * @param bArray byte array containing algorithm specific initialization info.
    * @param bOff offset within <code>bArray</code> where the algorithm specific data begins.
    * @param bLen byte length of algorithm specific parameter data
    * @exception CryptoException with the following reason codes:<ul>
    * <li><code>CryptoException.ILLEGAL_VALUE</code> if a byte array parameter option is not supported by the algorithm or if
    * the <code>bLen</code> is an incorrect byte length for the algorithm specific data.
    * </ul>
    */
    abstract public void init ( byte[] bArray, short bOff, short bLen )
	throws CryptoException;
 
    
   /**
    * Gets the Checksum algorithm. Valid codes listed in ALG_ .. constants above e.g. {@link #ALG_ISO3309_CRC16}
    * @return the algorithm code defined above.
    */
    abstract public byte getAlgorithm();

    /**
     * Generates a CRC checksum of all/last input data. The CRC engine processes input data
     * starting with the byte at offset <CODE>inOffset</CODE> and continuing on until the
     * byte at <CODE>(inOffset+inLength-1)</CODE> of the <CODE>inBuff</CODE> array. Within
     * each byte the processing proceeeds from the least significant bit to the most.
     * <p>Completes and returns the checksum computation.
     * The <code>Checksum</code> object is reset to the initial state(state upon construction)
     * when this method completes.
     * <p>Note:<ul>
     * <li><em>The ALG_ISO3309_CRC16 and ALG_ISO3309_CRC32 algorithms reset the initial checksum
     * value to 0. The initial checksum value can be re-initialized using the
     * </em>{@link #init(byte[], short, short)} <em> method.</em>
     * </ul>
     * <p>The input and output buffer data may overlap.
     * @param inBuff the input buffer of data to be checksummed
     * @param inOffset the offset into the input buffer at which to begin checksum generation
     * @param inLength the byte length to checksum
     * @param outBuff the output buffer, may be the same as the input buffer
     * @param outOffset the offset into the output buffer where the resulting checksum value begins
     * @return number of bytes of checksum output in <code>outBuff</code>
     */
    abstract public short doFinal(
        byte[] inBuff,
        short inOffset,
        short inLength,
        byte[] outBuff,
        short outOffset);

   /**
    * Accumulates a partial checksum of the input data. The CRC engine processes input data
     * starting with the byte at offset <CODE>inOffset</CODE> and continuing on until the
     * byte at <CODE>(inOffset+inLength-1)</CODE> of the <CODE>inBuff</CODE> array. Within
     * each byte the processing proceeeds from the least significant bit to the most.
    * <p>This method requires temporary storage of intermediate results. 
    * This may result in additional resource consumption and/or slow performance.
    * This method should only be used if all the input data required for the checksum
    * is not available in one byte array. The doFinal() method
    * is recommended whenever possible.
    * <p>Note:<ul>
    * <li><em>If <code>inLength</code> is 0 this method does nothing.</em>
    * </ul>
    * @param inBuff the input buffer of data to be checksummed
    * @param inOffset the offset into the input buffer at which to begin checksum generation
    * @param inLength the byte length to checksum
    * @see #doFinal
    */
    abstract public void update(
        byte[] inBuff,
        short inOffset,
        short inLength);
}
