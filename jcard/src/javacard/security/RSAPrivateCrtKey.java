/*
 * Copyright � 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
// Workfile:@(#)RSAPrivateCrtKey.java	1.10
// Version:1.10
// Date:02/01/02
// 
// Archive:  /Products/Europa/api21/javacard/security/RSAPrivateCrtKey.java 
// Modified:02/01/02 16:39:01
// Original author:  Andy
// */

package javacard.security;

/**
 * The <code>RSAPrivateCrtKey</code> interface is used to sign data using the RSA algorithm
 * in its Chinese Remainder Theorem form. It may also be used by the <code>javacardx.crypto.Cipher</code> class
 * to encrypt/decrypt messages.
 * <p>
 * Let <I>S</I> = <I>m</I><SUP><I>d</I></SUP> mod <I>n</I>,
 * where <I>m</I> is the data to be signed, <I>d</I> is the private key exponent,
 * and <I>n</I> is private key modulus composed of
 * two prime numbers <I>p</I> and <I>q</I>.
 * The following names are used in the initializer methods in this interface:
 * <p>
 * P, the prime factor <I>p</I><BR>
 * Q, the prime factor <I>q</I>.<BR>
 * PQ = <I>q</I><SUP>-1</SUP> mod <I>p</I><BR>
 * DP1 = <I>d</I> mod (<I>p</I> - 1)<BR>
 * DQ1 = <I>d</I> mod (<I>q</I> - 1)<BR>
 * <p>When all five components (P,Q,PQ,DP1,DQ1) of the key are set, the key is
 * initialized and ready for use.
 *
 * @see RSAPrivateKey
 * @see RSAPublicKey
 * @see KeyBuilder
 * @see Signature
 * @see javacardx.crypto.Cipher
 * @see javacardx.crypto.KeyEncryption
 *
 */

public interface RSAPrivateCrtKey extends PrivateKey {

  /**
   * Sets the value of the P parameter.
   * The plaintext data format is big-endian and right-aligned (the least significant bit is the least significant
   * bit of last byte). Input P parameter data is copied into the internal representation.
   * <p>Note:<ul>
   * <li><em>If the key object implements the </em><code>javacardx.crypto.KeyEncryption</code><em>
   * interface and the </em><code>Cipher</code><em> object specified via </em><code>setKeyCipher()</code><em>
   * is not </em><code>null</code><em>, the P parameter value is decrypted using the </em><code>Cipher</code><em> object.</em>
   * </ul>
   * @param buffer the input buffer
   * @param offset the offset into the input buffer at which the parameter value begins
   * @param length the length of the parameter
   * @exception CryptoException with the following reason code:<ul>
   * <li><code>CryptoException.ILLEGAL_VALUE</code> if the input parameter data length is inconsistent
   * with the implementation or if input data decryption is required and fails.
   * </ul>
   */
    void setP( byte[] buffer, short offset, short length) throws CryptoException;

  /**
   * Sets the value of the Q parameter.
   * The plaintext data format is big-endian and right-aligned (the least significant bit is the least significant
   * bit of last byte). Input Q parameter data is copied into the internal representation.
   * <p>Note:<ul>
   * <li><em>If the key object implements the </em><code>javacardx.crypto.KeyEncryption</code><em>
   * interface and the </em><code>Cipher</code><em> object specified via </em><code>setKeyCipher()</code><em>
   * is not </em><code>null</code><em>, the Q parameter value is decrypted using the </em><code>Cipher</code><em> object.</em>
   * </ul>
   * @param buffer the input buffer
   * @param offset the offset into the input buffer at which the parameter value begins
   * @param length the length of the parameter
   * @exception CryptoException with the following reason code:<ul>
   * <li><code>CryptoException.ILLEGAL_VALUE</code> if the input parameter data length is inconsistent
   * with the implementation or if input data decryption is required and fails.
   * </ul>
   */
    void setQ( byte[] buffer, short offset, short length) throws CryptoException;

  /**
   * Sets the value of the DP1 parameter.
   * The plaintext data format is big-endian and right-aligned (the least significant bit is the least significant
   * bit of last byte). Input DP1 parameter data is copied into the internal representation.
   * <p>Note:<ul>
   * <li><em>If the key object implements the </em><code>javacardx.crypto.KeyEncryption</code><em>
   * interface and the </em><code>Cipher</code><em> object specified via </em><code>setKeyCipher()</code><em>
   * is not </em><code>null</code><em>, the DP1 parameter value is decrypted using the </em><code>Cipher</code><em> object.</em>
   * </ul>
   * @param buffer the input buffer
   * @param offset the offset into the input buffer at which the parameter value begins
   * @param length the length of the parameter
   * @exception CryptoException with the following reason code:<ul>
   * <li><code>CryptoException.ILLEGAL_VALUE</code> if the input parameter data length is inconsistent
   * with the implementation or if input data decryption is required and fails.
   * </ul>
   */
    void setDP1( byte[] buffer, short offset, short length) throws CryptoException;

  /**
   * Sets the value of the DQ1 parameter.
   * The plaintext data format is big-endian and right-aligned (the least significant bit is the least significant
   * bit of last byte). Input DQ1 parameter data is copied into the internal representation.
   * <p>Note:<ul>
   * <li><em>If the key object implements the </em><code>javacardx.crypto.KeyEncryption</code><em>
   * interface and the </em><code>Cipher</code><em> object specified via </em><code>setKeyCipher()</code><em>
   * is not </em><code>null</code><em>, the DQ1 parameter value is decrypted using the </em><code>Cipher</code><em> object.</em>
   * </ul>
   * @param buffer the input buffer
   * @param offset the offset into the input buffer at which the parameter value begins
   * @param length the length of the parameter
   * @exception CryptoException with the following reason code:<ul>
   * <li><code>CryptoException.ILLEGAL_VALUE</code> if the input parameter data length is inconsistent
   * with the implementation or if input data decryption is required and fails.
   * </ul>
   */
    void setDQ1( byte[] buffer, short offset, short length) throws CryptoException;

  /**
   * Sets the value of the PQ parameter.
   * The plaintext data format is big-endian and right-aligned (the least significant bit is the least significant
   * bit of last byte). Input PQ parameter data is copied into the internal representation.
   * <p>Note:<ul>
   * <li><em>If the key object implements the </em><code>javacardx.crypto.KeyEncryption</code><em>
   * interface and the </em><code>Cipher</code><em> object specified via </em><code>setKeyCipher()</code><em>
   * is not </em><code>null</code><em>, the PQ parameter value is decrypted using the </em><code>Cipher</code><em> object.</em>
   * </ul>
   * @param buffer the input buffer
   * @param offset the offset into the input buffer at which the parameter value begins
   * @param length the length of the parameter
   * @exception CryptoException with the following reason code:<ul>
   * <li><code>CryptoException.ILLEGAL_VALUE</code> if the input parameter data length is inconsistent
   * with the implementation or if input data decryption is required and fails.
   * </ul>
   */
    void setPQ( byte[] buffer, short offset, short length) throws CryptoException;

  /**
   * Returns the value of the P parameter in plain text.
   * The data format is big-endian and right-aligned (the least significant bit is the least significant
   * bit of last byte).
   * @param buffer the output buffer
   * @param offset the offset into the output buffer at which the parameter value begins
   * @return the byte length of the P parameter value returned
   * @exception CryptoException with the following reason code:<ul>
   * <li><code>CryptoException.UNINITIALIZED_KEY</code> if the value of P parameter
   * has not been
   * successfully initialized using the <code>RSAPrivateCrtKey.setP</code> method since the
   * time the initialized state of the key was set to false.
   * </ul>
   * @see Key
   */
    short getP( byte[] buffer, short offset );

  /**
   * Returns the value of the Q parameter in plain text.
   * The data format is big-endian and right-aligned (the least significant bit is the least significant
   * bit of last byte).
   * @param buffer the output buffer
   * @param offset the offset into the output buffer at which the parameter value begins
   * @return the byte length of the Q parameter value returned
   * @exception CryptoException with the following reason code:<ul>
    * <li><code>CryptoException.UNINITIALIZED_KEY</code> if the value of Q parameter
   * has not been
   * successfully initialized using the <code>RSAPrivateCrtKey.setQ</code> method since the
   * time the initialized state of the key was set to false.
   * </ul>
   * @see Key
   */
    short getQ( byte[] buffer, short offset );

  /**
   * Returns the value of the DP1 parameter in plain text.
   * The data format is big-endian and right-aligned (the least significant bit is the least significant
   * bit of last byte).
   * @param buffer the output buffer
   * @param offset the offset into the output buffer at which the parameter value begins
   * @return the byte length of the DP1 parameter value returned
   * @exception CryptoException with the following reason code:<ul>
    * <li><code>CryptoException.UNINITIALIZED_KEY</code> if the value of DP1 parameter
   * has not been
   * successfully initialized using the <code>RSAPrivateCrtKey.setDP1</code> method since the
   * time the initialized state of the key was set to false.
   * </ul>
   * @see Key
   */
    short getDP1( byte[] buffer, short offset );

  /**
   * Returns the value of the DQ1 parameter in plain text.
   * The data format is big-endian and right-aligned (the least significant bit is the least significant
   * bit of last byte).
   * @param buffer the output buffer
   * @param offset the offset into the output buffer at which the parameter value begins
   * @return the byte length of the DQ1 parameter value returned
   * @exception CryptoException with the following reason code:<ul>
   * <li><code>CryptoException.UNINITIALIZED_KEY</code> if the value of DQ1 parameter
   * has not been
   * successfully initialized using the <code>RSAPrivateCrtKey.setDQ1</code> method since the
   * time the initialized state of the key was set to false.
   * </ul>
   * @see Key
   */
    short getDQ1( byte[] buffer, short offset );

  /**
   * Returns the value of the PQ parameter in plain text.
   * The data format is big-endian and right-aligned (the least significant bit is the least significant
   * bit of last byte).
   * @param buffer the output buffer
   * @param offset the offset into the output buffer at which the parameter value begins
   * @return the byte length of the PQ parameter value returned
   * @exception CryptoException with the following reason code:<ul>
   * <li><code>CryptoException.UNINITIALIZED_KEY</code> if the value of PQ parameter
   * has not been
   * successfully initialized using the <code>RSAPrivateCrtKey.setPQ</code> method since the
   * time the initialized state of the key was set to false.
   * </ul>
   * @see Key
   */
    short getPQ( byte[] buffer, short offset );

 }

