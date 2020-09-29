/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
// Workfile:@(#)KeyBuilder.java	1.16
// Version:1.16
// Date:04/15/02
// */

package javacard.security;

/**
 * The <code>KeyBuilder</code> class is a key object factory.
 *
 */

public class KeyBuilder{

	// keyType parameter options

	/**
     * <code>Key</code> object which implements interface type <code>DESKey</code>
     * with CLEAR_ON_RESET transient key data.
     * <p>This <code>Key</code> object implicitly performs a <code>clearKey()</code> on
     * power on or card reset.
     */
	public static final byte TYPE_DES_TRANSIENT_RESET      = 1;

	/**
     * <code>Key</code> object which implements interface type <code>DESKey</code>
     * with CLEAR_ON_DESELECT transient key data.
     * <p>This <code>Key</code> object implicitly performs a <code>clearKey()</code> on
     * power on, card reset and applet deselection.
     */
	public static final byte TYPE_DES_TRANSIENT_DESELECT   = 2;

	/**
     * <code>Key</code> object which implements interface type <code>DESKey</code> with persistent key data.
     */
	public static final byte TYPE_DES                      = 3;

	/**
     * <code>Key</code> object which implements interface type <code>RSAPublicKey</code>.
     */
	public static final byte TYPE_RSA_PUBLIC               = 4;

	/**
     * <code>Key</code> object which implements interface type <code>RSAPrivateKey</code> which
     * uses modulus/exponent form.
     */
	public static final byte TYPE_RSA_PRIVATE              = 5;

	/**
     * <code>Key</code> object which implements interface type <code>RSAPrivateCrtKey</code> which
     * uses Chinese Remainder Theorem.
     */
	public static final byte TYPE_RSA_CRT_PRIVATE          = 6;

	/**
     * <code>Key</code> object which implements the interface type <code>DSAPublicKey</code>
     * for the DSA algorithm.
     */
	public static final byte TYPE_DSA_PUBLIC               = 7;

    /**
     * <code>Key</code> object which implements the interface type <code>DSAPrivateKey</code>
     * for the DSA algorithm.
     */
	public static final byte TYPE_DSA_PRIVATE              = 8;

    /**
     * Key object which implements the interface type <code>ECPublicKey</code>
     * for EC operations over fields of characteristic 2 with polynomial
     * basis.
     */
        public static final byte TYPE_EC_F2M_PUBLIC = 9;

    /**
     * Key object which implements the interface type <code>ECPrivateKey</code>
     * for EC operations over fields of characteristic 2 with polynomial
     * basis.
     */
       public static final byte TYPE_EC_F2M_PRIVATE = 10;

    /**
     * Key object which implements the interface type <code>ECPublicKey</code>
     * for EC operations over large prime fields.
     */
       public static final byte TYPE_EC_FP_PUBLIC       = 11;

    /**
     * Key object which implements the interface type <code>ECPrivateKey</code>
     * for EC operations over large prime fields.
     */
       public static final byte TYPE_EC_FP_PRIVATE      = 12;
        
    /**
     * <code>Key</code> object which implements interface type <code>AESKey</code>
     * with CLEAR_ON_RESET transient key data.
     * <p>This <code>Key</code> object implicitly performs a <code>clearKey()</code> on
     * power on or card reset.
     */
	public static final byte TYPE_AES_TRANSIENT_RESET      = 13;

	/**
     * <code>Key</code> object which implements interface type <code>AESKey</code>
     * with CLEAR_ON_DESELECT transient key data.
     * <p>This <code>Key</code> object implicitly performs a <code>clearKey()</code> on
     * power on, card reset and applet deselection.
     */
      	public static final byte TYPE_AES_TRANSIENT_DESELECT   = 14;

        /**
     * <code>Key</code> object which implements interface type <code>AESKey</code> with persistent key data.
     */
	public static final byte TYPE_AES                  = 15;
       
       
       // keyLength parameter options
    /**
     * DES Key Length <code>LENGTH_DES</code> = 64.
     */
       public static final short LENGTH_DES  = 64;

    /**
     * DES Key Length <code>LENGTH_DES3_2KEY</code> = 128.
     */
       public static final short LENGTH_DES3_2KEY = 128;

    /**
     * DES Key Length <code>LENGTH_DES3_3KEY</code> = 192.
     */
       public static final short LENGTH_DES3_3KEY = 192;

    /**
     * RSA Key Length <code>LENGTH_RSA_512</code> = 512.
     */
       public static final short LENGTH_RSA_512 = (short)512;
        
    /**
     * RSA Key Length <code>LENGTH_RSA_736</code> = 736.
     */
       public static final short LENGTH_RSA_736 = (short)736;

    /**
     * RSA Key Length <code>LENGTH_RSA_768</code> = 768.
     */
       public static final short LENGTH_RSA_768 = (short)768;
        
    /**
     * RSA Key Length <code>LENGTH_RSA_896</code> = 896.
     */
       public static final short LENGTH_RSA_896 = (short)896;

    /**
     * RSA Key Length <code>LENGTH_RSA_1024</code> = 1024.
     */
       public static final short LENGTH_RSA_1024 = (short)1024;
       
    /**
     * RSA Key Length <code>LENGTH_RSA_1280</code> = 1280.
     */
       public static final short LENGTH_RSA_1280 = (short)1280;
       
     /**
     * RSA Key Length <code>LENGTH_RSA_1536</code> = 1536.
     */
       public static final short LENGTH_RSA_1536 = (short)1536;
       
    /**
     * RSA Key Length <code>LENGTH_RSA_1984</code> = 1984.
     */
       public static final short LENGTH_RSA_1984 = (short)1984;

    /**
     * RSA Key Length <code>LENGTH_RSA_2048</code> = 2048.
     */
       public static final short LENGTH_RSA_2048 = (short)2048;
       
    /**
     * DSA Key Length <code>LENGTH_DSA_512</code> = 512.
     */
       public static final short LENGTH_DSA_512 = (short)512;

    /**
     * DSA Key Length <code>LENGTH_DSA_768</code> = 768.
     */
       public static final short LENGTH_DSA_768 = (short)768;

    /**
     * DSA Key Length <code>LENGTH_DSA_1024</code> = 1024.
     */
       public static final short LENGTH_DSA_1024 = (short)1024;

       /**
        * EC Key Length <CODE>LENGTH_EC_FP_112</CODE> = 112.
        */
       public static final short LENGTH_EC_FP_112 = 112;

       /**
        * EC Key Length <CODE>LENGTH_EC_F2M_113</CODE> = 113.
        */
       public static final short LENGTH_EC_F2M_113 = 113;
       
       /**
        * EC Key Length <CODE>LENGTH_EC_FP_128</CODE> = 128.
        */
       public static final short LENGTH_EC_FP_128 = 128;

       /**
        * EC Key Length <CODE>LENGTH_EC_F2M_131</CODE> = 131.
        */
       public static final short LENGTH_EC_F2M_131 = 131;

       /**
        * EC Key Length <CODE>LENGTH_EC_FP_160</CODE> = 160.
        */
       public static final short LENGTH_EC_FP_160 = 160;

       /**
        * EC Key Length <CODE>LENGTH_EC_F2M_163</CODE> = 163.
        */
       public static final short LENGTH_EC_F2M_163 = 163;
       
       /**
        * EC Key Length <CODE>LENGTH_EC_FP_192</CODE> = 192.
        */
       public static final short LENGTH_EC_FP_192 = 192;

       /**
        * EC Key Length <CODE>LENGTH_EC_F2M_193</CODE> = 193.
        */
       public static final short LENGTH_EC_F2M_193 = 193;
      
    /**
     * AES Key Length <code>LENGTH_AES_128</code> = 128.
     */
       public static final short LENGTH_AES_128= 128;

    /**
     * AES Key Length <code>LENGTH_AES_192</code> = 192.
     */
       public static final short LENGTH_AES_192= 192;

    /**
     * AES Key Length <code>LENGTH_AES_256</code> = 256.
     */
       public static final short LENGTH_AES_256= 256;  
       
        /**
         * Creates uninitialized cryptographic keys for signature and cipher algorithms. Only instances created
         * by this method may be the key objects used to initialize instances of
         * <code>Signature</code>, <code>Cipher</code> and <code>KeyPair</code>.
         * Note that the object returned must be cast to their appropriate key type interface.
         * @param keyType the type of key to be generated. Valid codes listed in TYPE.. constants.
         * See {@link #TYPE_DES_TRANSIENT_RESET}
         * @param keyLength the key size in bits. The valid key bit lengths are key type dependent. Some common
         * key lengths are listed above above in the LENGTH_.. constants. See {@link #LENGTH_DES}
         * @param keyEncryption if <code>true</code> this boolean requests a key implementation
         * which implements the <code>javacardx.crypto.KeyEncryption</code> interface.
         * The key implementation returned may implement the <code>javacardx.crypto.KeyEncryption</code>
         * interface even when this parameter is <code>false</code>.
         * @return the key object instance of the requested key type, length and encrypted access.
         * @exception CryptoException with the following reason codes:<ul>
         * <li><code>CryptoException.NO_SUCH_ALGORITHM</code> if the requested algorithm
         * associated with the specified type, size of key and key encryption interface is not supported.</ul>
         */
	public static Key buildKey( byte keyType, short keyLength, boolean keyEncryption ) throws CryptoException {

	    switch ( keyType ){
	        default :
	            CryptoException.throwIt( CryptoException.NO_SUCH_ALGORITHM );
	    }
	    return null;
	}

	/**
	 * No constructor
	 */
	KeyBuilder() {}

}
