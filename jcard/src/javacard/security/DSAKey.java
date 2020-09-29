/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
// Workfile:@(#)DSAKey.java	1.10
// Version:1.10
// Date:02/01/02
// 
// Archive:  /Products/Europa/api21/javacard/security/DSAKey.java 
// Modified:02/01/02 16:39:01
// Original author:  Andy
// */

package javacard.security;

/**
 * The </code>DSAKey</code> interface is the base interface for the DSA algorithms private and
 * public key implementations. A DSA private key implementation must also implement
 * the <code>DSAPrivateKey</code> interface methods. A DSA public key implementation must also implement
 * the <code>DSAPublicKey</code> interface methods.
 * <p>When all four components of the key (X or Y,P,Q,G) are set, the key is
 * initialized and ready for use.
 * <p>
 *
 * @see DSAPublicKey
 * @see DSAPrivateKey
 * @see KeyBuilder
 * @see Signature
 * @see javacardx.crypto.KeyEncryption
 *
 */

public interface DSAKey {

  /**
   * Sets the prime parameter value of the key.
   * The plaintext data format is big-endian and right-aligned (the least significant bit is the least significant
   * bit of last byte). Input prime parameter data is copied into the internal representation.
   * <p>Note:<ul>
   * <li><em>If the key object implements the </em><code>javacardx.crypto.KeyEncryption</code><em>
   * interface and the </em><code>Cipher</code><em> object specified via </em><code>setKeyCipher()</code><em>
   * is not </em><code>null</code><em>, the prime parameter value is decrypted using the </em><code>Cipher</code><em> object.</em>
   * </ul>
   * @param buffer the input buffer
   * @param offset the offset into the input buffer at which the prime parameter value begins
   * @param length the length of the prime parameter value
   * @exception CryptoException with the following reason code:<ul>
   * <li><code>CryptoException.ILLEGAL_VALUE</code> if the input parameter data length is inconsistent
   * with the implementation or if input data decryption is required and fails.
   * </ul>
   */
    void setP( byte[] buffer, short offset, short length) throws CryptoException;

  /**
   * Sets the subprime parameter value of the key.
   * The plaintext data format is big-endian and right-aligned (the least significant bit is the least significant
   * bit of last byte). Input subprime parameter data is copied into the internal representation.
   * <p>Note:<ul>
   * <li><em>If the key object implements the </em><code>javacardx.crypto.KeyEncryption</code><em>
   * interface and the </em><code>Cipher</code><em> object specified via </em><code>setKeyCipher()</code><em>
   * is not </em><code>null</code><em>, the subprime parameter value is decrypted using the </em><code>Cipher</code><em> object.</em>
   * </ul>
   * @param buffer the input buffer
   * @param offset the offset into the input buffer at which the subprime parameter value begins
   * @param length the length of the subprime parameter value
   * @exception CryptoException with the following reason code:<ul>
   * <li><code>CryptoException.ILLEGAL_VALUE</code> if the input parameter data length is inconsistent
   * with the implementation or if input data decryption is required and fails.
   * </ul>
   */
    void setQ( byte[] buffer, short offset, short length) throws CryptoException;

  /**
   * Sets the base parameter value of the key.
   * The plaintext data format is big-endian and right-aligned (the least significant bit is the least significant
   * bit of last byte). Input base parameter data is copied into the internal representation.
   * <p>Note:<ul>
   * <li><em>If the key object implements the </em><code>javacardx.crypto.KeyEncryption</code><em>
   * interface and the </em><code>Cipher</code><em> object specified via </em><code>setKeyCipher()</code><em>
   * is not </em><code>null</code><em>, the base parameter value is decrypted using the </em><code>Cipher</code><em> object.</em>
   * </ul>
   * @param buffer the input buffer
   * @param offset the offset into the input buffer at which the base parameter value begins
   * @param length the length of the base parameter value
   * @exception CryptoException with the following reason code:<ul>
   * <li><code>CryptoException.ILLEGAL_VALUE</code> if the input parameter data length is inconsistent
   * with the implementation or if input data decryption is required and fails.
   * </ul>
   */
    void setG( byte[] buffer, short offset, short length) throws CryptoException;

  /**
   * Returns the prime parameter value of the key in plain text.
   * The data format is big-endian and right-aligned (the least significant bit is the least significant
   * bit of last byte).
   * @param buffer the output buffer
   * @param offset the offset into the output buffer at which the prime parameter value starts
   * @return the byte length of the prime parameter value returned
   * @exception CryptoException with the following reason code:<ul>
   * <li><code>CryptoException.UNINITIALIZED_KEY</code> if the prime parameter has not been
   * successfully initialized using the <code>DSAKey.setP</code> method since the
   * time the initialized state of the key was set to false.
   * </ul>
   * @see Key
   */
    short getP( byte[] buffer, short offset );

  /**
   * Returns the subprime parameter value of the key in plain text.
   * The data format is big-endian and right-aligned (the least significant bit is the least significant
   * bit of last byte).
   * @param buffer the output buffer
   * @param offset the offset into the output buffer at which the subprime parameter value begins
   * @return the byte length of the subprime parameter value returned
   * @exception CryptoException with the following reason code:<ul>
   * <li><code>CryptoException.UNINITIALIZED_KEY</code> if the subprime parameter has not been
   * successfully initialized using the <code>DSAKey.setQ</code> method since the
   * time the initialized state of the key was set to false.
   * </ul>
   * @see Key
   */
    short getQ( byte[] buffer, short offset );

  /**
   * Returns the base parameter value of the key in plain text.
   * The data format is big-endian and right-aligned (the least significant bit is the least significant
   * bit of last byte).
   * @param buffer the output buffer
   * @param offset the offset into the output buffer at which the base parameter value begins
   * @return the byte length of the base parameter value returned
   * @exception CryptoException with the following reason code:<ul>
   * <li><code>CryptoException.UNINITIALIZED_KEY</code> if the base parameter has not been
   * successfully initialized using the <code>DSAKey.setG</code> method since the
   * time the initialized state of the key was set to false.
   * </ul>
   * @see Key
   */
    short getG( byte[] buffer, short offset );

 }
