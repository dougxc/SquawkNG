/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
// Workfile:@(#)DSAPublicKey.java	1.10
// Version:1.10
// Date:02/01/02
// 
// Archive:  /Products/Europa/api21/javacard/security/DSAPublicKey.java 
// Modified:02/01/02 16:39:01
// Original author:  Andy
// */

package javacard.security;

/**
 * The <code>DSAPublicKey</code> interface is used to verify signatures
 * on signed data using the DSA algorithm.
 * An implementation of <code>DSAPublicKey</code> interface must also implement
 * the <code>DSAKey</code> interface methods.
 * <p>When all four components of the key (Y,P,Q,G) are set, the key is
 * initialized and ready for use.
 * @see DSAPrivateKey
 * @see KeyBuilder
 * @see Signature
 * @see javacardx.crypto.KeyEncryption
 */

public interface DSAPublicKey extends PublicKey, DSAKey{

   /**
   * Sets the value of the key. When the base, prime and subprime parameters are initialized
   * and the key value is set, the key is ready for use.
   * The plaintext data format is big-endian and right-aligned (the least significant bit is the least significant
   * bit of last byte). Input key data is copied into the internal representation.
   * <p>Note:<ul>
   * <li><em>If the key object implements the </em><code>javacardx.crypto.KeyEncryption</code><em>
   * interface and the </em><code>Cipher</code><em> object specified via </em><code>setKeyCipher()</code><em>
   * is not </em><code>null</code><em>, the key value is decrypted using the </em><code>Cipher</code><em> object.</em>
   * </ul>
   * @param buffer the input buffer
   * @param offset the offset into the input buffer at which the key value begins
   * @param length the length of the key value
   * @exception CryptoException with the following reason code:<ul>
   * <li><code>CryptoException.ILLEGAL_VALUE</code> if the input key data length is inconsistent
   * with the implementation or if input data decryption is required and fails.
   * </ul>
   */
    void setY( byte[] buffer, short offset, short length) throws CryptoException;

  /**
   * Returns the value of the key in plain text.
   * The data format is big-endian and right-aligned (the least significant bit is the least significant
   * bit of last byte).
   * @param buffer the output buffer
   * @param offset the offset into the input buffer at which the key value starts
   * @return the byte length of the key value returned
   * @exception CryptoException with the following reason code:<ul>
   * <li><code>CryptoException.UNINITIALIZED_KEY</code> if the value of the key has not been
   * successfully initialized using the <code>DSAPublicKey.setY</code> method since the
   * time the initialized state of the key was set to false.
   * </ul>
   * @see Key
   */
    short getY( byte[] buffer, short offset );
 }
