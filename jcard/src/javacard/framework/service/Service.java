/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */


/*
 *     %W% %E%
 */

package javacard.framework.service;

import javacard.framework.* ;

/**
 * This is the base interface for the service framework in Java Card.
 * A <CODE>Service</CODE> is an object that is able to perform partial or complete
 * processing on a set of incoming commands encapsulated in an APDU.
 * <p> Services collaborate in pre-processing, command processing and
 * post-processing of incoming <CODE>APDU</CODE> commands. They share the same <CODE>APDU</CODE> object
 * by using the communication framework and the Common Service Format (CSF)
 * defined in <code>BasicService</code>.
 * An application is built by combining pre-built and newly defined Services
 * within a <code>Dispatcher</code> object.
 * @see BasicService
 *
 * @version 1.0
 */
public interface Service {


    /** Pre-processes the input data for the command in the <CODE>APDU</CODE> object.
     * When invoked, the APDU object
     * should either be in <CODE>STATE_INITIAL</CODE> with the APDU buffer in the Init format
     * or in <CODE>STATE_FULL_INCOMING</CODE> with the APDU buffer in the Input Ready format
     * defined in <CODE>BasicService</CODE>.
     * <p>The method must return <CODE>true</CODE> if no more
     * pre-processing should be performed, and <CODE>false</CODE> otherwise.
     * In particular, it must return <CODE>false</CODE> if it has not performed any
     * processing on the command.
     * <P>
     * After normal completion, the <CODE>APDU</CODE> object is usually in <CODE>STATE_FULL_INCOMING</CODE>
     * with the APDU buffer in the Input Ready format defined in <CODE>BasicService</CODE>.
     * However, in some cases if the Service processes the command entirely,
     * the <CODE>APDU</CODE> object may be in <CODE>STATE_OUTGOING</CODE>
     * with the APDU buffer in the Output Ready format defined in <CODE>BasicService</CODE>.
     * @param apdu the <CODE>APDU</CODE> object containing the command being processed.
     * @return <code>true</code> if input processing is finished, <CODE>false</CODE> otherwise.
     */
  public boolean processDataIn(APDU apdu) ;

  /** Processes the command in the <CODE>APDU</CODE> object.
   * When invoked, the <CODE>APDU</CODE> object
   * should normally be in <CODE>STATE_INITIAL</CODE> with the APDU buffer in the Init format
   * or in <CODE>STATE_FULL_INCOMING</CODE> with the <CODE>APDU</CODE> buffer in the Input Ready format
   * defined in <CODE>BasicService</CODE>. However, in some cases, if a pre-processing
   * service has processed the command entirely, the <CODE>APDU</CODE> object may be in
   * <CODE>STATE_OUTGOING</CODE> with the APDU buffer in the Output Ready
   * format defined in <CODE>BasicService</CODE>.
   * <p>The method must return <CODE>true</CODE> if no more command
   * processing is required, and <CODE>false</CODE> otherwise.
   * In particular, it should
   * return <CODE>false</CODE> if it has not performed any processing on the command.
   * <P>
   * After normal completion, the <CODE>APDU</CODE> object must be in
   * <CODE>STATE_OUTGOING</CODE> and the output response must be in the
   * APDU buffer in the Output Ready format defined in <CODE>BasicService</CODE>.
   * @param apdu the <CODE>APDU</CODE> object containing the command being processed.
   * @return <code>true</code> if the command has been processed, <CODE>false</CODE> otherwise..
   */
  public boolean processCommand(APDU apdu) ;

   /** Post-processes the output data for the command in the <CODE>APDU</CODE> object.
    * When invoked, the <CODE>APDU</CODE>
    * object should be in <CODE>STATE_OUTGOING</CODE> with the APDU buffer in
    * the Output Ready format defined in <CODE>BasicService</CODE>.
    * <p>The method should return <CODE>true</CODE> if
    * no more post-processing is required, and <CODE>false</CODE> otherwise. In particular,
    * it should return <CODE>false</CODE> if it has not performed any processing on the
    * command.
    * <P>
    * After normal completion, the <CODE>APDU</CODE> object should must be in
    * <CODE>STATE_OUTGOING</CODE> and the output response must be in the
    * APDU buffer in the Output Ready format defined in <CODE>BasicService</CODE>.
    *
    * @param apdu the <CODE>APDU</CODE> object containing the command being processed.
    * @return <code>true</code> if output processing is finished, <CODE>false</CODE> otherwise.
    */
  public boolean processDataOut(APDU apdu) ;

}
