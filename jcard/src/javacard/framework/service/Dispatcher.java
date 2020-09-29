/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
 *  @(#)Dispatcher.java	1.29 02/05/02 
 */

package javacard.framework.service;

import javacard.framework.* ;
/**
 * A <CODE>Dispatcher</CODE> is used to build an application by aggregating several
 * services.
 * <p>
 * The dispatcher maintains a registry of <CODE>Service</CODE> objects. A <CODE>Service</CODE> is
 * categorized by the type of processing it performs :<ul>
 * <li><em>A pre-processing service pre-processes input data for the command being processed.
 * It is associated with the <CODE>PROCESS_INPUT_DATA</CODE> phase.</em>
 * <li><em>A command processing service processes the input data and generates output
 * data. It is associated with the <CODE>PROCESS_COMMAND</CODE> phase.</em>
 * <li><em>A post-processing service post-processes the generated output data.
 * It is associated with the <CODE>PROCESS_OUTPUT_DATA</CODE> phase.</em>
 * </ul>
 * The dispatcher simply dispatches incoming <code>APDU</code> object containing the
 * command being processed to the registered services.
 * @version 1.0
 */
public class Dispatcher {
    
    private Service[] services ;
    private byte[] servicePhases ;
    private short numServices ;
    
    /**
     * Identifies the null processing phase.
     */
    public static final byte PROCESS_NONE = (byte)0 ;
    
    /**
     * Identifies the input data processing phase.
     */
    public static final byte PROCESS_INPUT_DATA = (byte)1 ;
    
    /**
     * Identifies the main command processing phase.
     */
    public static final byte PROCESS_COMMAND = (byte)2 ;
    
    /**
     * Identifies the output data processing phase.
     */
    public static final byte PROCESS_OUTPUT_DATA = (byte)3 ;
    
    /**
     * Creates a <CODE>Dispatcher</CODE> with a designated maximum number of services.
     * @param maxServices the maximum number of services that can be registered to
     * this dispatcher.
     * @throws ServiceException with the following reason code:<ul>
     * <li><code>ServiceException.ILLEGAL_PARAM</code> if the maxServices parameter
     * is negative.
     * </ul>
     */
    public Dispatcher(short maxServices) throws ServiceException{
        if(maxServices < 0)
            ServiceException.throwIt(ServiceException.ILLEGAL_PARAM);
        services = new Service[maxServices] ;
        servicePhases = new byte[maxServices] ;
        this.numServices = 0;
    }
    
    /**
     * Atomically adds the specified service to the dispatcher registry for the
     * specified processing phase. Services are invoked in the order in which they
     * are added to the registry during the processing of that phase.
     * If the requested service is already registered for the specified processing
     * phase, this method does nothing.
     * @param service the Service to be added to the dispatcher.
     * @param phase the processing phase associated with this service
     * @throws ServiceException with the following reason code:<ul>
     * <li><code>ServiceException.DISPATCH_TABLE_FULL</code> if the maximum number
     * of registered services is exceeded.
     * <li><code>ServiceException.ILLEGAL_PARAM</code> if the phase parameter
     * is undefined or if the service parameter is null.
     * </ul>
     */
    public void addService(Service service, byte phase) throws ServiceException{
        if(phase < PROCESS_NONE || phase > PROCESS_OUTPUT_DATA)
            ServiceException.throwIt(ServiceException.ILLEGAL_PARAM);
        
        if(service == null)
            ServiceException.throwIt(ServiceException.ILLEGAL_PARAM);
        
        for(short i=(short)0 ; i < numServices ; i++)
            if ((services[i] == service) && (servicePhases[i] == phase))
                return ;
        
        if (this.numServices >= this.services.length)
            ServiceException.throwIt(ServiceException.DISPATCH_TABLE_FULL) ;
        
        boolean inTransaction = false;
        if (JCSystem.getTransactionDepth() == 0) {
            inTransaction = true;
            JCSystem.beginTransaction();
        }
        servicePhases[numServices] = phase ;
        services[numServices++] = service ;
        
        if (inTransaction) JCSystem.commitTransaction();
    }
    
    /**
     * Atomically removes the specified service for the specified processing phase
     * from the dispatcher registry. Upon removal, the slot used by the specified service in
     * the dispatcher registry is available for re-use. If the specified service
     * is not registered for the specified processing phase, this method does nothing.
     * @param service the <code>Service</code> to be deleted from the dispatcher.
     * @param phase the processing phase associated with this service
     * @throws ServiceException with the following reason code:<ul>
     * <li><code>ServiceException.ILLEGAL_PARAM</code> if the phase parameter
     * is unknown or if the service parameter is null.
     * </ul>
     */
    public void removeService(Service service, byte phase) throws ServiceException{
        if(service == null)
            ServiceException.throwIt(ServiceException.ILLEGAL_PARAM);
        
        if(phase < PROCESS_NONE || phase > PROCESS_OUTPUT_DATA)
            ServiceException.throwIt(ServiceException.ILLEGAL_PARAM);
        
        for(short i=(short)0 ; i < numServices ; i++)
            if ((services[i] == service) && (servicePhases[i] == phase)) {
                boolean inTransaction = false;
                if (JCSystem.getTransactionDepth() == 0) {
                    inTransaction = true;
                    JCSystem.beginTransaction();
                }
                
                for ( short j=(short)i;j<(short)(numServices-1); j++ ) {
                    services[j] = services[(short)(j+1)] ;
                    servicePhases[j] = servicePhases[(short)(j+1)] ;
                }
                numServices--;
                if (inTransaction) JCSystem.commitTransaction();
                return ;
            }
    }
    
    /** Manages the processing of the command in the <CODE>APDU</CODE> object. This method is called when
     * only partial processing using the registered services is required or when the APDU response
     * folowing an error during the processing needs to be controlled.
     * <p>
     * It sequences through the registered services by calling
     * the appopriate processing methods. Processing starts with the phase
     * indicated in the input parameter. Services registered for that processing
     * phase are called in the sequence in which they were registered until all
     * the services for the processing phase have been called or a service indicates
     * that processing for that phase is complete by returning <CODE>true</CODE> from its
     * processing method. The dispatcher then
     * processes the next phases in a similar manner until all the phases have been processed. The
     * <CODE>PROCESS_OUTPUT_DATA</CODE> processing phase is performed only if the command
     * processing has completed normally (<CODE>APDU</CODE> object state is <CODE>APDU.STATE_OUTGOING</CODE>).
     * <p>The processing sequence is <CODE>PROCESS_INPUT_DATA</CODE> phase, followed by
     * the <CODE>PROCESS_COMMAND</CODE> phase and lastly the <CODE>PROCESS_OUTPUT_DATA</CODE>.
     * The processing is performed as follows:
     * <ul><li><CODE>PROCESS_INPUT_DATA</CODE> phase invokes the <CODE>Service.processDataIn(APDU)</CODE> method
     * <li><CODE>PROCESS_COMMAND</CODE> phase invokes the <CODE>Service.processCommand(APDU)</CODE> method
     * <li><CODE>PROCESS_OUTPUT_DATA</CODE> phase invokes the <CODE>Service.processDataOut(APDU)</CODE> method
     * </ul>
     * If the command processing completes normally, the output data, assumed to be in the
     * APDU buffer in the Common Service Format (CSF) defined in <code>BasicService</code>,
     * is sent using <CODE>APDU.sendBytes</CODE> and the response status is generated by
     * throwing an <CODE>ISOException</CODE> exception.
     * If the command could not be processed, <CODE>null</CODE> is returned. If any exception is thrown
     * by a Service during the processing, that exception is returned.
     * <p>
     * @param command the APDU object containing the command to be processed
     * @param phase the processing phase to perform first
     * @return an exception that occurred during the processing of the command,
     * or <code>null</code> if the command could not be processed.
     * @throws ServiceException with the following reason code:<ul>
     * <li><code>ServiceException.ILLEGAL_PARAM</code> if the phase parameter
     * is PROCESS_NONE or an undefined value.
     * </ul>
     * @see BasicService
     */
    public Exception dispatch(APDU command, byte phase) throws ServiceException {
 
        if ( (phase < PROCESS_INPUT_DATA) || (phase > PROCESS_OUTPUT_DATA))
            ServiceException.throwIt(ServiceException.ILLEGAL_PARAM);
        byte[] buffer = command.getBuffer() ;
        
        try {
            // First attempts the input data phase
            for(short i=(short)0 ; i<numServices ; i++) {
                // First checks if there is some processing to do
                if ((phase > PROCESS_INPUT_DATA)/* ||
                (command.getCurrentState() >= APDU.STATE_OUTGOING) */ ) {
                    break;
                }
                
                
                // Then checks if the service is applicable
                if (servicePhases[i] == PROCESS_INPUT_DATA)
                    if (services[i].processDataIn(command))
                        break ;
            }
            
            // Then attempts the main command processing phase
            for(short i=(short)0 ; i<numServices ; i++) {
                // First checks if there is some processing to do
                if ((phase > PROCESS_COMMAND)   /*   ||
                (command.getCurrentState() >= APDU.STATE_OUTGOING)    */    )
                    break ;
                
                // Then checks if the service is applicable
                if (servicePhases[i] == PROCESS_COMMAND) {
                    if
                    (services[i].processCommand(command))
                        break ;
                }
            }
            
            // If the command has not been processed, no postprocessing to do
            if (command.getCurrentState() < APDU.STATE_OUTGOING)
                return null ;
            
            // Finally does the outgoing data phase
            for(short i=(short)0 ; i<numServices ; i++) {
                // Then checks if the service is applicable
                if (servicePhases[i] == PROCESS_OUTPUT_DATA)
                    if (services[i].processDataOut(command))
                        break ;
            }
        } catch(Exception e) {
            return e ;
        }
        
        // If everything went fine, sends the output
        
        short outLength = (short)(buffer[4] & 0x00ff) ;
        command.setOutgoingLength(outLength) ;
        command.sendBytes((short)5, outLength) ;
        ISOException.throwIt(Util.getShort(buffer, (short)2)) ;
        // This statement is never reached
        return null ;
    }
    
    /** Manages the entire processing of the command in the APDU object
     * input parameter. This method is called to delegate the complete
     * processing of the incoming APDU command to the configured services.
     * <p>This method uses the {@link #dispatch(APDU,byte)} method with
     * <CODE>PROCESS_DATA_IN</CODE> as the input phase parameter to sequence through the
     * the services registered for all three phases : <CODE>PROCESS_DATA_IN</CODE> followed
     * by <CODE>PROCESS_DATA_COMMAND</CODE> and lastly <CODE>PROCESS_DATA_OUT</CODE>.
     * <p>If the command processing completes normally, the output data is sent using
     * <CODE>APDU.sendBytes</CODE> and the response status is generated by throwing an
     * <CODE>ISOException</CODE> exception or by simply returning (for status = 0x9000).
     * If an exception is thrown by any Service during
     * the processing, <CODE>ISO7816.SW_UNKNOWN</CODE> response status code is generated
     * by throwing an <CODE>ISOException</CODE>. If the command could not be processed
     * <CODE>ISO7816.SW_INS_NOT_SUPPORTED</CODE> response status is generated by throwing
     * an <CODE>ISOException</CODE>.
     * @param command the APDU object containing command to be processed.
     * @throws ISOException with the response bytes per ISO 7816-4
     */
    public void process(APDU command) throws ISOException {
        
        Exception e = dispatch(command, PROCESS_INPUT_DATA) ;
        
        if (e == null)
            ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED) ;
        else
            ISOException.throwIt(ISO7816.SW_UNKNOWN) ;
    }
}

