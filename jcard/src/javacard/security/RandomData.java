/*
 * Copyright � 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
// Workfile:@(#)RandomData.java	1.11
// Version:1.11
// Date:04/22/02
// 
// Archive:  /Products/Europa/api21/javacard/security/RandomData.java 
// Modified:04/22/02 16:39:01
// Original author:  Andy
// */

package javacard.security;

//
/**
 * The <code>RandomData</code> abstract class is the base class for random number generation.
 * Implementations of RandomData
 * algorithms must extend this class and implement all the abstract methods.
 */

abstract public class RandomData{

  // Random Number algorithm options

    /**
    * Utility pseudo random number generation algorithms. The random number sequence
    * generated by this algorithm need not be the same even if seeded with the same
    * seed data.
    * <p> Even if a transaction is in progress, the update of the internal state
    * shall not participate in the transaction.
    */
    public static final byte ALG_PSEUDO_RANDOM        = 1;

    /**
     * Cryptographically secure random number generation algorithms.
    */
    public static final byte ALG_SECURE_RANDOM        = 2;

  /**
   * Protected constructor for subclassing.
   */
    protected RandomData(){}

    /**
     * Creates a <code>RandomData</code> instance of the selected algorithm.
     * The pseudo random <code>RandomData</code> instance's seed is initialized to a internal default value.
     * @param algorithm the desired random number algorithm. Valid codes listed in ALG_ .. constants above. See {@link #ALG_PSEUDO_RANDOM}
     * @return the <code>RandomData</code> object instance of the requested algorithm.
     * @exception CryptoException with the following reason codes:<ul>
     * <li><code>CryptoException.NO_SUCH_ALGORITHM</code> if the requested algorithm is not supported.</ul>
     */
	public static final RandomData getInstance(byte algorithm) throws CryptoException{

	    switch ( algorithm ){
	        case ALG_PSEUDO_RANDOM :
	        case ALG_SECURE_RANDOM :
		default:
	            CryptoException.throwIt( CryptoException.NO_SUCH_ALGORITHM);
	    }
            return null;
	 }

  /**
   * Generates random data.
   * @param buffer the output buffer
   * @param offset the offset into the output buffer
   * @param length the length of random data to generate
   * @exception CryptoException with the following reason codes:<ul>
   * <li><code>CryptoException.ILLEGAL_VALUE</code> if the <code>length</code> parameter is
   * zero.</ul>
   */
   abstract public void generateData(
        byte[] buffer,
        short offset,
        short length) throws CryptoException;
 //   {
 //   	Randomness.generate(buffer,offset,length);
 //   }

  /**
   * Seeds the random data generator.
   * @param buffer the input buffer
   * @param offset the offset into the input buffer
   * @param length the length of the seed data
   */
    abstract public void setSeed(
        byte[] buffer,
        short offset,
        short length);
//    {
//    	Randomness.setSeed(buffer,offset,length);
//    }
}
