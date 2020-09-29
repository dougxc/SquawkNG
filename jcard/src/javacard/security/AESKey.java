/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
// Workfile:@(#)AESKey.java	1.5
// Version:1.5
// Date:10/17/01
// 
// Archive:  /Products/Europa/api21/javacard/security/AESKey.java 
// Modified:10/17/01 15:19:07
// Original author:  Paul Montague
// */

package javacard.security;

/**
 * <code>AESKey</code> contains a 16/24/32 byte key for AES computations based
 * on the Rijndael algorithm. 
 * <p>When the key data is set, the key is initialized and ready for use.
 *<p>
 * @see KeyBuilder
 * @see Signature
 * @see javacardx.crypto.Cipher
 * @see javacardx.crypto.KeyEncryption
 * @since Java Card 2.2 
 */
    public interface AESKey extends SecretKey{

    /**
     * Sets the <code>Key</code> data. The plaintext length of input key data is 16/24/32 bytes.
     * The data format is big-endian and right-aligned (the least significant bit is the least significant
     * bit of last byte). Input key data is copied into the internal representation.
     * <p>Note:<ul>
     * <li><em>If the key object implements the </em><code>javacardx.crypto.KeyEncryption</code><em>
     * interface and the </em><code>Cipher</code><em> object specified via </em><code>setKeyCipher()</code><em>
     * is not </em><code>null</code><em>, </em><code>keyData</code><em> is decrypted using the </em><code>Cipher</code><em> object.</em>
     * </ul>
     * @param keyData byte array containing key initialization data
     * @param kOff offset within keyData to start
     * @exception CryptoException with the following reason code:<ul>
     * <li><code>CryptoException.ILLEGAL_VALUE</code> if the input key data length is inconsistent
     * with the implementation or if input data decryption is required and fails.
     * </ul>
     */
    void setKey( byte[] keyData, short kOff ) throws CryptoException;

    /**
     * Returns the <code>Key</code> data in plain text. The length of output key data is 16/24/32 bytes.
     * The data format is big-endian and right-aligned (the least significant bit is the least significant
     * bit of last byte).
     * @param keyData byte array to return key data
     * @param kOff offset within <code>keyData</code> to start.
     * @return the byte length of the key data returned.
     * @exception CryptoException with the following reason code:<ul>
     * <li><code>CryptoException.UNINITIALIZED_KEY</code> if the key data has not been
     * successfully initialized using the <code>AESKey.setKey</code> method since the
     * time the initialized state of the key was set to false.
     * </ul>
     * @see Key
     */
    byte getKey( byte[] keyData, short kOff ) throws CryptoException;

    }
