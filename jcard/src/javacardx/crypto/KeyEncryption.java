/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
// Workfile:@(#)KeyEncryption.java	1.4
// Version:1.4
// Date:02/01/02
// 
// Archive:  /Products/Europa/api21/javacardx/crypto/KeyEncryption.java 
// Modified:02/01/02 16:39:01
// Original author:  Andy
// */

package javacardx.crypto;

/**
 * <code>KeyEncryption</code> interface defines the methods used to enable encrypted
 * key data access to a key implementation.
 *<p>
 * @see javacard.security.KeyBuilder
 * @see Cipher
 */
    public interface KeyEncryption{

    /**
     * Sets the <code>Cipher</code> object to be used to decrypt the input key data
     * and key parameters in the set methods.<p>
     * Default <code>Cipher</code> object is <code>null</code> - no decryption performed.
     * @param keyCipher the decryption <code>Cipher</code> object to decrypt the input key data.
     * <code>null</code> parameter indicates that no decryption is required.
     */
    void setKeyCipher( Cipher keyCipher );

    /**
     * Returns the <code>Cipher</code> object to be used to decrypt the input key data
     * and key parameters in the set methods.<p>
     * Default is <code>null</code> - no decryption performed.
     * @return keyCipher the decryption <code>Cipher</code> object to decrypt the input key data.
     * <code>null</code> return indicates that no decryption is performed.
     */
    Cipher getKeyCipher();

    }
