/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
 * @(#)RMIService.java	1.4 02/05/01
 */

package javacard.framework.service;

import javacard.framework.* ;

import java.rmi.* ;

import com.sun.javacard.impl.NativeMethods;
import com.sun.javacard.impl.PrivAccess;

/**
 * An implementation of a service that is used to process Java Card RMI (JCRMI)
 * requests for remotely accessible objects.
 *
 * @version 1.0
 */
public class RMIService extends BasicService implements RemoteService {

    /** The default INS value (0x38) used for the remote method invocation command
     * (INVOKE) in the Java Card RMI protocol.
     */
    public static final byte DEFAULT_RMI_INVOKE_INSTRUCTION = 0x38;

    private static final byte FCI_TAG = 0x6F;
    private static final byte APPLICATION_TAG = 0x6E;
    private static final byte RMI_TAG = 0x5E;

    private static final short RMI_VERSION = 0x0202;

    private static final byte NORMAL_TAG = (byte)0x81;
    private static final byte EXCEPTION_TAG = (byte)0x82;
    private static final byte EXCEPTION_SUBCLASS_TAG = (byte)0x83;
    private static final byte ERROR_TAG = (byte)0x99;

    private static final short OBJECT_ERROR = (short)0x0001;
    private static final short METHOD_ERROR = (short)0x0002;
    private static final short SIGNATURE_ERROR = (short)0x0003;
    private static final short PARAM_RESOURCES_ERROR = (short)0x0004;
    private static final short RESPONSE_RESOURCES_ERROR = (short)0x0005;
    private static final short PROTOCOL_ERROR = (short)0x0006;
    private static final short OTHER_ERROR = (short)0xFFFF;


    private static final byte OBJECT_ID_OFFSET = (byte)5;
    private static final byte METHOD_ID_OFFSET = (byte)7;
    private static final byte PARAM_OFFSET = (byte)9;

    private static final byte TAG_OFFSET = (byte)5;
    private static final byte RESULT_OFFSET = (byte)6;

    private static final byte TYPE_C = 0x01; // types of remote ref
    private static final byte TYPE_I = 0x02;
    private static final byte UNDEFINED_TYPE = 0x00;

    private byte ref_type = UNDEFINED_TYPE;  // initial type


    Remote initialObject = null;
    byte invoke_instr;
    byte next_invoke_instr = DEFAULT_RMI_INVOKE_INSTRUCTION;


    private Object[] expArray;

    /**
     * Creates a new <CODE>RMIService</CODE> and sets the specified remote object
     * as the initial reference for the applet.
     * The initial reference will be published to the client in response
     * to the SELECT APDU command processed by this object.
     * <p> The <CODE>RMIService</CODE> instance may create session data to manage exported
     * remote objects for the current applet session in <CODE>CLEAR_ON_DESELECT</CODE>
     * transient space.
     * @param initialObject the remotely accessible initial object.
     * @throws NullPointerException if the <CODE>initialObject</CODE>
     * parameter is <code>null</code>.
     */
    public RMIService( Remote initialObject ) throws NullPointerException{
        initialObject.equals(null); // force NullPointerException if necessary
        this.initialObject = initialObject;
        this.expArray = JCSystem.makeTransientObjectArray( (short)8,
        (byte)JCSystem.CLEAR_ON_DESELECT );

    }

    /**
     * Defines the instruction byte to be used in place of
     * <CODE>DEFAULT_RMI_INVOKE_INSTRUCTION</CODE> in the JCRMI protocol for the
     * INVOKE commands used to access the <CODE>RMIService</CODE> for remote method
     * invocations.
     * <p>Note:<ul>
     * <li><em>The new instruction byte goes into effect next time this <CODE>RMIService</CODE>
     * instance processes an applet SELECT command. The JCRMI protocol until then
     * is unchanged.</em>
     * </ul>
     * @param ins the instruction byte.
     */
    public void setInvokeInstructionByte(byte ins) {
        this.next_invoke_instr = ins;
    }

    /** Processes the command within the <CODE>APDU</CODE> object. When invoked, the APDU object
     * should either be in <CODE>STATE_INITIAL</CODE> with the APDU buffer in the Init format
     * or in <CODE>STATE_FULL_INCOMING</CODE> with the APDU buffer in the Input Ready format
     * defined in <CODE>BasicService</CODE>.
     * <p> This method first checks if the command in the <CODE>APDU</CODE> object is a Java Card
     * RMI access command. The Java Card RMI access commands currently defined are:
     * Applet SELECT and INVOKE. If it is not a Java Card RMI access command, this method does
     * nothing and returns false.
     * <p>If the command is a Java Card RMI access command, this method processes the command
     * and generates the response to be returned to the terminal.
     * For a detailed description of the APDU protocol used in Java Card RMI access commands
     * please see the Remote Method Invocation Service chapter of <em>Java Card Runtime Environment (JCRE)
     * Specification</em>.
     * <p>Java Card RMI access commands are processed as follows:
     * <ul>
     * <li>An applet SELECT command results in a Java Card RMI information
     * structure in FCI format containing the initial reference object
     * as the response to be returned to the terminal.
     * <li>An INVOKE command results in the following sequence -
     * </ul><ol>
     * <li><em>The remote object is located. A remote object is accessible only if
     * it was returned by this <CODE>RMIService</CODE> instance and since that time
     * some applet instance or the other from within the applet package has been
     * an active applet instance.</em>
     * <li><em>The method of the object is identified</em>
     * <li><em>Primitive input parameters are unmarshalled onto the stack. Array type
     * input parameters are created as global arrays(See <em>Java Card Runtime Environment (JCRE)
     * Specification</em>)</em> and references to these are pushed onto the stack.
     * <li><em>An INVOKEVIRTUAL bytecode to the remote method is simulated</em>
     * <li> <em>Upon return from the method, method return or exception information
     * is marshalled from the stack as the response to be returned to the
     * terminal</em>
     * </ol><p>
     * After normal completion, this method returns <CODE>true</CODE> and the APDU object is
     * in <CODE>STATE_OUTGOING</CODE> and the output response is in the
     * APDU buffer in the Output Ready format defined in <CODE>BasicService</CODE>.
     * @param apdu the APDU object containing the command being processed.
     * @return <code>true</code> if the command has been processed, <CODE>false</CODE> otherwise
     * @throws ServiceException with the following reason codes:<ul>
     * <li><code>ServiceException.CANNOT_ACCESS_IN_COMMAND</code> if this is a
     * Java Card RMI access command and the APDU object is not in STATE_INITIAL or in
     * STATE_FULL_INCOMING
     * <li><code>ServiceException.REMOTE_OBJECT_NOT_EXPORTED</code> if the
     * remote method returned a remote object which has not been exported.
     * </ul>
     * @throws SecurityException if one of the following conditions is met:<ul>
     * <li> if this is a Java Card RMI INVOKE command and a firewall security violation
     * occurred while trying to simulate an INVOKEVIRTUAL bytecode on the remote
     * object.
     * <li> if internal storage in <code>CLEAR_ON_DESELECT</code> transient space
     * is accessed when the currently active context is not the context of the
     * currently selected applet.
     * </ul>
     * @see CardRemoteObject
     */
    public boolean processCommand(APDU apdu){


        if(selectingApplet()) {
            return processSelect(apdu);
        }
        else {
            return processInvoke(apdu);
        }
    }

    /**
     *
     * Prepares APDU in the following format
     *   0     1    2     3     4    5         6      7         8
     * +-----------------------------------------------------------------+
     * | CLA | INS | SW1 | SW2 | Le | FCI_TAG | len1 | APP_TAG | len2  |
     * +-----------------------------------------------------------------+
     *
     *   9         10    11-12          13          14    15-...
     * +-----------------------------------------------------------------+
     * | RMI_TAG | len3 | RMI_VERSION | INV_INSTR | TAG | Data...        |
     * +-----------------------------------------------------------------+
     *
     * where (tag + data) = (NORMAL_TAG + INITIAL REF)
     *                  or  (ERROR_TAG  + RESPONSE_RESOURCES_ERROR)
     *                           (if the ref does not fit into APDU buffer)
     *
     */
    private boolean processSelect(APDU apdu) {

        receiveInData(apdu);

        byte[] buffer =  apdu.getBuffer();

        ref_type = UNDEFINED_TYPE;

        byte P1 = getP1(apdu);
        byte P2 = getP2(apdu);

        if(P1 == 0x04) {
            if(P2 == 0x00) ref_type = TYPE_C;
            else if(P2 == 0x10) ref_type = TYPE_I;
        }

        this.invoke_instr = this.next_invoke_instr;

        buffer[5] = FCI_TAG;
        buffer[7] = APPLICATION_TAG;
        buffer[9] = RMI_TAG;


        Util.setShort(buffer, (short)11, RMI_VERSION);

        buffer[13] = invoke_instr;

        short offset = 14;

        try {
            if(ref_type == UNDEFINED_TYPE) {
                offset = writeTagAndError(PROTOCOL_ERROR, buffer, offset);
            }
            else {
                buffer[offset++] = NORMAL_TAG;
                offset = writeRef(initialObject, buffer, offset);
            }
        }
        catch(ArrayIndexOutOfBoundsException e) {  // name does not fit into APDU
            offset = writeTagAndError(RESPONSE_RESOURCES_ERROR, buffer, (short)14);
        }

        short len3 = (short)(offset - 11);
        short len2 = (short)(len3+2);
        short len1 = (short)(len2+2);
        short le =   (short)(len1+2);

        buffer[6] = (byte)len1;
        buffer[8] = (byte)len2;
        buffer[10] = (byte)len3;

        setOutputLength(apdu, le);
        succeed(apdu);

        return true;
    }


    private boolean processInvoke(APDU command) {


        byte[] buffer =  command.getBuffer();

        // the service ignores (return false) commands it does
        //                            not know how to process

        if((byte)(getCLA(command)&0xF0) != (byte)0x80) {
            return false;
        }

        if(getINS(command) != invoke_instr) {
            return false;
        }

        if(ref_type == UNDEFINED_TYPE) {
            return false;
        }

        receiveInData(command);

        try {

            short offset = TAG_OFFSET;
            // wrong P1P2 or data too short (must be at least 4 bytes: objID+mthdID)
            if(Util.getShort(buffer, (short)2) != RMI_VERSION || buffer[4]<(byte)4 ) {
                offset = writeTagAndError(PROTOCOL_ERROR, buffer, TAG_OFFSET);
            }
            else {
                short objID = Util.getShort(buffer, OBJECT_ID_OFFSET);

                if( !this.isExportedForSession(objID)) {
                    offset = writeTagAndError(OBJECT_ERROR, buffer, TAG_OFFSET);
                }
                else {

                    short mthdID = Util.getShort(buffer, METHOD_ID_OFFSET);

                    short mthdAddr = RMINativeMethods.getRemoteMethodInfo(objID, mthdID);
                    short mthdType = 100;

                    switch(mthdAddr) {
                        case (short)0xFF01:   // obj not found
                        case (short)0xFF02:   // obj not remote
                            offset = writeTagAndError(OBJECT_ERROR, buffer, TAG_OFFSET);
                            break;
                        case (short)0xFF03:    // method not found
                            offset = writeTagAndError(METHOD_ERROR, buffer, TAG_OFFSET);
                            break;
                        case (short)0xFFFF:
                            offset = writeTagAndError(OTHER_ERROR, buffer, TAG_OFFSET);
                            break;
                        default:              // no error
                            mthdType = RMINativeMethods.getReturnType(mthdAddr);
                    }

                    boolean result_not_exported = false;
                    try {
                        switch(mthdType) {
                            case 1:
                                RMINativeMethods.rmi_invoker_void(mthdAddr, objID, buffer, PARAM_OFFSET);

                                offset = TAG_OFFSET;
                                buffer[offset++] = NORMAL_TAG;
                                break;
                            case 2:
                                boolean bo;
                                bo= RMINativeMethods.rmi_invoker_boolean(mthdAddr,
                                objID,
                                buffer,
                                PARAM_OFFSET);

                                buffer[TAG_OFFSET] = NORMAL_TAG;
                                if(bo) {
                                    buffer[RESULT_OFFSET] = 1;
                                }
                                else {
                                    buffer[RESULT_OFFSET] = 0;
                                }
                                offset = (short)(RESULT_OFFSET+1);
                                break;
                            case 3:
                                byte by;
                                by = RMINativeMethods.rmi_invoker_byte(mthdAddr, objID, buffer, PARAM_OFFSET);
                                buffer[TAG_OFFSET] = NORMAL_TAG;
                                offset = RESULT_OFFSET;
                                buffer[offset++] = by;
                                break;
                            case 4:
                                short sh;
                                sh = RMINativeMethods.rmi_invoker_short(mthdAddr, objID, buffer, PARAM_OFFSET);
                                buffer[TAG_OFFSET] = NORMAL_TAG;
                                offset = Util.setShort(buffer, RESULT_OFFSET, sh);
                                break;
                            case 5:
                                int in;
                                in = RMINativeMethods.rmi_invoker_int(mthdAddr, objID, buffer, PARAM_OFFSET);
                                buffer[TAG_OFFSET] = NORMAL_TAG;
                                offset = writeInt(in, buffer, RESULT_OFFSET);
                                break;
                            case 0xA:
                            case 0xB:
                            case 0xC:
                            case 0xD:
                                Object res_array = RMINativeMethods.rmi_invoker_array(mthdAddr,
                                objID,
                                buffer,
                                PARAM_OFFSET);
                                buffer[TAG_OFFSET] = NORMAL_TAG;
                                try {
                                    offset = RMINativeMethods.writeArray(res_array, buffer, RESULT_OFFSET);
                                }
                                catch(ArrayIndexOutOfBoundsException e) {
                                    offset = writeTagAndError(RESPONSE_RESOURCES_ERROR, buffer, TAG_OFFSET);
                                }
                                break;
                            case 0x6:
                                Remote ref;
                                ref = (Remote)RMINativeMethods.rmi_invoker_reference(mthdAddr,
                                objID,
                                buffer,
                                PARAM_OFFSET);
                                if(CardRemoteObject.isExported(ref) == false
                                &&
                                !this.isExportedForSession(NativeMethods.getObjectID(ref))) {
                                    result_not_exported = true;
                                    ServiceException.throwIt(ServiceException.REMOTE_OBJECT_NOT_EXPORTED);
                                }
                                try {
                                    buffer[TAG_OFFSET] = NORMAL_TAG;
                                    offset = writeRef(ref, buffer, RESULT_OFFSET);
                                }
                                catch(ArrayIndexOutOfBoundsException e) {
                                    offset = writeTagAndError(RESPONSE_RESOURCES_ERROR, buffer, TAG_OFFSET);
                                }
                                break;
                            case 100:   // method error
                                break;
                            default:    // should never come here
                                offset = writeTagAndError(OTHER_ERROR, buffer, TAG_OFFSET);
                                break;
                        }
                    }
                    catch(Throwable t) {
                        if(result_not_exported == true) {
                            ServiceException.throwIt(ServiceException.REMOTE_OBJECT_NOT_EXPORTED);
                        }

                        offset = writeTagAndThrowable(t, buffer, TAG_OFFSET);
                    }

                }
            }
            setOutputLength(command, (short)(offset - TAG_OFFSET));
            succeedWithStatusWord(command, (short) 0x9000);

            return true; // command is processed

        }
        finally {
            RMINativeMethods.deleteAllTempArrays();
        }
    }

  /*
   * Methods short write*(data, buffer, offset), return # of bytes written
   *
   *
   */

    // Codes for Java Exceptions

    private static final byte Throwable_Type = (byte)0x00;
    private static final byte ArithmeticException_Type = (byte)0x01;
    private static final byte ArrayIndexOutOfBoundsException_Type = (byte)0x02;
    private static final byte ArrayStoreException_Type = (byte)0x03;
    private static final byte ClassCastException_Type = (byte)0x04;
    private static final byte Exception_Type = (byte)0x05;
    private static final byte IndexOutOfBoundsException_Type = (byte)0x06;
    private static final byte NegativeArraySizeException_Type = (byte)0x07;
    private static final byte NullPointerException_Type = (byte)0x08;
    private static final byte RuntimeException_Type = (byte)0x09;
    private static final byte SecurityException_Type = (byte)0x0A;
    private static final byte java_io_IOException_Type = (byte)0x0B;
    private static final byte java_rmi_RemoteException_Type = (byte)0x0C;

    // Java Card Exceptions

    private static final byte APDUException_Type = (byte)0x20;
    private static final byte CardException_Type = (byte)0x21;
    private static final byte CardRuntimeException_Type = (byte)0x22;
    private static final byte ISOException_Type = (byte)0x23;
    private static final byte PINException_Type = (byte)0x24;
    private static final byte SystemException_Type = (byte)0x25;
    private static final byte TransactionException_Type = (byte)0x26;
    private static final byte UserException_Type = (byte)0x27;
    private static final byte CryptoException_Type = (byte)0x30;
    private static final byte ServiceException_Type = (byte)0x40;

    private short writeTagAndError(short reason, byte[] buffer, short offset) {
        buffer[offset++] = ERROR_TAG;
        return Util.setShort(buffer, offset, reason);
    }

    private short writeTagAndThrowable(Throwable data, byte[] buffer, short offset) {
        byte tag = 0;
        byte type = 0;
        short reason = 0;

        if(RMINativeMethods.isAPIException(data)) {
            tag = EXCEPTION_TAG;
        }
        else {
            tag = EXCEPTION_SUBCLASS_TAG;
        }


        try {
            throw data;
        }
        catch(ArithmeticException e) {
            type = ArithmeticException_Type;
        }
        catch(ArrayIndexOutOfBoundsException e) {
            type = ArrayIndexOutOfBoundsException_Type;
        }
        catch(ArrayStoreException e) {
            type = ArrayStoreException_Type;
        }
        catch(ClassCastException e) {
            type = ClassCastException_Type;
        }
        catch(IndexOutOfBoundsException e) {
            type = IndexOutOfBoundsException_Type;
        }
        catch(NegativeArraySizeException e) {
            type = NegativeArraySizeException_Type;
        }
        catch(NullPointerException e) {
            type = NullPointerException_Type;
        }
        catch(SecurityException e) {
            type = SecurityException_Type;
        }
        catch(java.rmi.RemoteException e) {
            type = java_rmi_RemoteException_Type;
        }
        catch(java.io.IOException e) {
            type = java_io_IOException_Type;
        }
        catch(APDUException e) {
            type = APDUException_Type;
            reason = e.getReason();
        }
        catch(ISOException e) {
            type = ISOException_Type;
            reason = e.getReason();
        }
        catch(PINException e) {
            type = PINException_Type;
            reason = e.getReason();
        }
        catch(SystemException e) {
            short errorCode = RMINativeMethods.getRMIErrorCode();
            if(errorCode == SIGNATURE_ERROR
            || errorCode == PARAM_RESOURCES_ERROR) {
                return writeTagAndError(errorCode, buffer, TAG_OFFSET);
            }
            type = SystemException_Type;
            reason = e.getReason();
        }
        catch(TransactionException e) {
            type = TransactionException_Type;
            reason = e.getReason();
        }
        catch(UserException e) {
            type = UserException_Type;
            reason = e.getReason();
        }
        catch(javacard.security.CryptoException e) {
            type = CryptoException_Type;
            reason = e.getReason();
        }
        catch(ServiceException e) {
            type = ServiceException_Type;
            reason = e.getReason();
        }
        catch(CardRuntimeException e) {
            type = CardRuntimeException_Type;
            reason = e.getReason();
        }
        catch(CardException e) {
            type = CardException_Type;
            reason = e.getReason();
        }
        catch(RuntimeException e) {
            type = RuntimeException_Type;
        }
        catch(Exception e) {
            type = Exception_Type;
        }
        catch(Throwable e) {
            type = Throwable_Type;
        }


        buffer[offset++] = tag;
        buffer[offset++] = type;
        return Util.setShort(buffer, offset, reason);
    }


    private short writeRef(Remote data, byte[] buffer, short offset) {
        if(data == null) return writeNullRef(buffer, offset);
        short objID = (short) NativeMethods.getObjectID(data);

        if(!CardRemoteObject.isExported(data)  &&   !this.isExportedForSession(objID)) {
            return writeNullRef(buffer, offset);
        }
        this.exportForSession(data);

        offset = Util.setShort(buffer, offset, objID);
        offset = RMINativeMethods.getAnticollisionString(data, buffer, (byte)offset);
        if(ref_type == TYPE_C)  // ref with class name
        {
            short nameAddr = RMINativeMethods.getClassNameAddress(data);
            offset = PrivAccess.getPrivAccess().getPkgNameForClass(nameAddr,buffer,(byte)offset);
            offset = RMINativeMethods.copyStringIntoBuffer(nameAddr, buffer, offset);
        }
        else     // ref with interfaces
        {
            byte intfCount = RMINativeMethods.getRemoteInterfaceNumber(data);
            buffer[offset++] = intfCount;

            byte lastPkgID = (byte)0xFF;
            for(byte i = 0; i<intfCount; ++i) {
                short intfAddr = RMINativeMethods.getRemoteInterfaceAddress(data, i);
                byte pkgID = PrivAccess.getPrivAccess().getPkgIDForAddress(intfAddr);
                if(pkgID == lastPkgID) {
                    buffer[offset++] = 0;
                }
                else {
                    offset = PrivAccess.getPrivAccess().getPkgNameForClass(intfAddr,
                    buffer,
                    (byte)offset);
                }
                lastPkgID = pkgID;
                offset = RMINativeMethods.copyInterfaceNameIntoBuffer(intfAddr, buffer, offset);
            }
        }
        return offset;
    }



    private short writeNullRef(byte[] buffer, short offset) {
        return Util.setShort(buffer, offset, (short)0xFFFF);
    }


  /*
   *    Object[] expArray: -  refs to the objects
   *
   */



    private void exportForSession(Remote obj) throws ArrayIndexOutOfBoundsException{
        short objID = NativeMethods.getObjectID(obj);
        if(!this.isExportedForSession(objID)) {
            for(short i=0; ; ++i) {
                if(expArray[i] == null) {
                    expArray[i] = obj;
                    return;          // success
                }
            }
            // intentionally throws ArrayIndexOutOfBounds if no slots available
        }
        return;  // reachable only if the object is already exported
    }

    private boolean isExportedForSession(short objID) {
        for(short i=0; i<(short)expArray.length; ++i) {
            if(NativeMethods.getObjectID(expArray[i]) == objID) {
                return true;
            }
        }
        return false;
    }

  /*
   * write* methods return the next offset
   *
   **/

    short writeInt(int data, byte[] buffer, short offset) {
        offset = Util.setShort(buffer, offset, (short)(data>>16));
        return  Util.setShort(buffer, offset, (short)(data));
    }



}





