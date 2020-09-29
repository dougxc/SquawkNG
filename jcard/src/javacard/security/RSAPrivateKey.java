/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
// Workfile:@(#)RSAPrivateKey.java	1.10
// Version:1.10
// Date:02/01/02
// 
// Archive:  /Products/Europa/api21/javacard/security/RSAPrivateKey.java 
// Modified:02/01/02 16:39:01
// Original author:  Andy
// */

package javacard.security;

/**
 * The <code>RSAPrivateKey</code> class is used to sign data using the RSA algorithm
 * in its modulus/exponent form. It may also be used by the <code>javacardx.crypto.Cipher</code> class
 * to encrypt/decrypt messages.
 * <p>When both the modulus and exponent of the key are set, the key is
 * initialized and ready for use.
 *
 * @see RSAPublicKey
 * @see RSAPrivateCrtKey
 * @see KeyBuilder
 * @see Signature
 * @see javacardx.crypto.Cipher
 * @see javacardx.crypto.KeyEncryption
 *
 */

public interface RSAPrivateKey extends PrivateKey {

  /**
   * Sets the modulus value of the key.
   * The plaintext data format is big-endian and right-aligned (the least significant bit is the least significant
   * bit of last byte). Input modulus data is copied into the internal representation.
   * <p>Note:<ul>
   * <li><em>If the key object implements the </em><code>javacardx.crypto.KeyEncryption</code><em>
   * interface and the </em><code>Cipher</code><em> object specified via </em><code>setKeyCipher()</code><em>
   * is not </em><code>null</code><em>, the modulus value is decrypted using the </em><code>Cipher</code><em> object.</em>
   * </ul>
   * @param buffer the input buffer
   * @param offset the offset into the input buffer at which the modulus value begins
   * @param length the length of the modulus
   * @exception CryptoException with the following reason code:<ul>
   * <li><code>CryptoException.ILLEGAL_VALUE</code> if the input modulus data length is inconsistent
   * with the implementation or if input data decryption is required and fails.
   * </ul>
   */
    void setModulus( byte[] buffer, short offset, short length) throws CryptoException;

  /**
   * Sets the private exponent value of the key.
   * The plaintext data format is big-endian and right-aligned (the least significant bit is the least significant
   * bit of last byte). Input exponent data is copied into the internal representation.
   * <p>Note:<ul>
   * <li><em>If the key object implements the </em><code>javacardx.crypto.KeyEncryption</code><em>
   * interface and the </em><code>Cipher</code><em> object specified via </em><code>setKeyCipher()</code><em>
   * is not </em><code>null</code><em>, the exponent value is decrypted using the </em><code>Cipher</code><em> object.</em>
   * </ul>
   * @param buffer the input buffer
   * @param offset the offset into the input buffer at which the exponent value begins
   * @param length the length of the exponent
   * @exception CryptoException with the following reason code:<ul>
   * <li><code>CryptoException.ILLEGAL_VALUE</code> if the input exponent data length is inconsistent
   * with the implementation or if input data decryption is required and fails.
   * </ul>
   */
    void setExponent( byte[] buffer, short offset, short length) throws CryptoException;

  /**
   * Returns the modulus value of the key in plain text.
   * The data format is big-endian and right-aligned (the least significant bit is the least significant
   * bit of last byte).
   * @param buffer the output buffer
   * @param offset the offset into the output buffer at which the modulus value starts
   * @return the byte length of the modulus value returned
   * @exception CryptoException with the following reason code:<ul>
   * <li><code>CryptoException.UNINITIALIZED_KEY</code> if the modulus value
   * of the key has not been
   * successfully initialized using the <code>RSAPrivateKey.setModulus</code> method since the
   * time the initialized state of the key was set to false.
   * </ul>
   * @see Key
   */
    short getModulus( byte[] buffer, short offset );

  /**
   * Returns the private exponent value of the key in plain text.
   * The data format is big-endian and right-aligned (the least significant bit is the least significant
   * bit of last byte).
   * @param buffer the output buffer
   * @param offset the offset into the output buffer at which the exponent value begins
   * @return the byte length of the private exponent value returned
   * @exception CryptoException with the following reason code:<ul>
   * <li><code>CryptoException.UNINITIALIZED_KEY</code> if the private exponent value
   * of the key has not been
   * successfully initialized using the <code>RSAPrivateKey.setExponent</code> method since the
   * time the initialized state of the key was set to false.
   * </ul>
   * @see Key
   */

    short getExponent( byte[] buffer, short offset );

 }
