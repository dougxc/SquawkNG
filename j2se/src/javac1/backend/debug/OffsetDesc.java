/*
 * @(#)OffsetDesc.java                  1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend.debug;

/**
 * Stores various offsets during code generation.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class OffsetDesc {
    /**
     * The offset of the interpreter entry point.
     */
    private int iepOffset;

    /**
     * The offset of the standard entry point.
     */
    private int epOffset;

    /**
     * The offset of the verified entry point.
     */
    private int vepOffset;

    /**
     * The offset of the code begin.
     */
    private int codeOffset;
    
    /**
     * The offset of the entry point for OSR.
     */
    private int osrOffset;

    /**
     * Constructs a new offset descriptor.
     */
    public OffsetDesc() {
        iepOffset = 0;
        epOffset = 0;
        vepOffset = 0;
        codeOffset = 0;
        osrOffset = 0;
    }

    /**
     * Sets the offset of the interpreter entry point.
     *
     * @param  iepOffset  interpreter entry point offset
     */
    public void setIepOffset(int iepOffset) {
        this.iepOffset = iepOffset;
    }

    /**
     * Returns the offset of the interpreter entry point.
     *
     * @return  interpreter entry point offset
     */
    public int getIepOffset() {
        return iepOffset;
    }

    /**
     * Sets the offset of the standard entry point.
     *
     * @param  epOffset  entry point offset
     */
    public void setEpOffset(int epOffset) {
        this.epOffset = epOffset;
    }

    /**
     * Returns the offset of the standard entry point.
     *
     * @return  entry point offset
     */
    public int getEpOffset() {
        return epOffset;
    }

    /**
     * Sets the offset of the verified entry point.
     *
     * @param  vepOffset  verified entry point offset
     */
    public void setVepOffset(int vepOffset) {
        this.vepOffset = vepOffset;
    }

    /**
     * Returns the offset of the verified entry point.
     *
     * @return  verified entry point offset
     */
    public int getVepOffset() {
        return vepOffset;
    }

    /**
     * Sets the offset of the code begin.
     *
     * @param  codeOffset  code offset
     */
    public void setCodeOffset(int codeOffset) {
        this.codeOffset = codeOffset;
    }

    /**
     * Returns the offset of the code begin.
     *
     * @return  code offset
     */
    public int getCodeOffset() {
        return codeOffset;
    }
    
    /**
     * Sets the offset of the entry point for OSR.
     *
     * @param  osrOffset  OSR offset
     */
    public void setOsrOffset(int osrOffset) {
        this.osrOffset = osrOffset;
    }
    
    /**
     * Returns the offset of the entry point for OSR.
     *
     * @return  OSR offset
     */
    public int getOsrOffset() {
        return osrOffset;
    }
}
