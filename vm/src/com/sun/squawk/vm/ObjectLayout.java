//J2C:layout.c **DO NOT DELETE THIS LINE**

/*IFJ*/package com.sun.squawk.vm;

/**
 * This class defines accessor methods for objects that have a layout only known
 * to the VM. That is, they cannot be described by a standard Java class.
 *
 * This class also contain some accessor method to objects that are described by
 * a Java class. These methods are essentially wrappers around the
 * auto-generated methods in GeneratedObjectLayout that provide extra
 * convenience and/or safety checks for the VM.
 */
/*IFJ*/abstract public class ObjectLayout extends GeneratedObjectLayout {

/*-----------------------------------------------------------------------*\
 *     Class "java.lang.ObjectAssociation" accessor helper methods       *
\*-----------------------------------------------------------------------*/

/*MAC*/ void ObjectAssociation_setObject_safe(int $asn, int $val) {
            if (assuming() && $val != 0) {
                assumeInCollector();
            }
            ObjectAssociation_setObject($asn, $val);
        }

/*MAC*/ void ObjectAssociation_setNext_safe(int $asn, int $val) {
            if (assuming() && $val != 0) {
                assumeInCollector();
            }
            ObjectAssociation_setNext($asn, $val);
        }

/*-----------------------------------------------------------------------*\
 *           Class "java.lang.Class" accessor helper methods             *
\*-----------------------------------------------------------------------*/

/*MAC*/boolean Class_isArrayClass(int $klass)  { return Class_getElementType($klass) != 0; }

/*---------------------------------------------------------------------------*\
 *                             Class state access                            *
\*---------------------------------------------------------------------------*/

    /*
     * The data structure storing the state of a class is:
     *
     * struct ClassState {
     *     oop    class;
     *     oop    next;
     *     oop    pointerFields[pointerFieldCount];
     *     word   nonPointerFields[];
     * }
     *
     * The class pointed to by the header of this object is "[java.lang._global_"
     * whose class number is GLOBAL_ARRAY. This must be an array type so that
     * it can be converted to an array and indexed as an array from Java code.
     */

/*MAC*/int  ClassState_getClass(int $state)                     { return getWord($state, CLS_STATE_class);                      }
/*MAC*/int  ClassState_getNext(int $state)                      { return getWord($state, CLS_STATE_next);                       }

/*MAC*/void ClassState_setNext(int $state, int $val)            {        setOopRam($state, CLS_STATE_next, $val);               }
/*MAC*/void ClassState_setClass(int $state, int $val)           {        setOopRam($state, CLS_STATE_class, $val);              }


/*---------------------------------------------------------------------------*\
 *                              Stack chunk methods                          *
\*---------------------------------------------------------------------------*/

/*MAC*/int  StackChunk_getNext(int $stack)                      { return getWord($stack, STACK_next);                           }
/*MAC*/int  StackChunk_getPrev(int $stack)                      { return getWord($stack, STACK_prev);                           }
/*MAC*/int  StackChunk_getSize(int $stack)                      { return getWord($stack, STACK_size);                           }
/*MAC*/int  StackChunk_getLastLp(int $stack)                    { return getWord($stack, STACK_lastLp);                         }
/*MAC*/int  StackChunk_getFirstLp(int $stack)                   { return $stack + w2b(STACK_HEADER_SIZE + FRAME_HEADER_SIZE);   }

/*MAC*/void StackChunk_setNext(int $stack, int $value)          { setWord($stack, STACK_next, $value);                          }
/*MAC*/void StackChunk_setPrev(int $stack, int $value)          { setWord($stack, STACK_prev, $value);                          }
/*MAC*/void StackChunk_setSize(int $stack, int $value)          { setWord($stack, STACK_size, $value);                          }
/*MAC*/void StackChunk_setLastLp(int $stack, int $value)        { setWord($stack, STACK_lastLp, $value);                        }

/*if[CHENEY.COLLECTOR]*/
/*MAC*/int  StackChunk_getSelf(int $stack)                      { return getWord($stack, STACK_self);                           }
/*MAC*/void StackChunk_setSelf(int $stack, int $value)          { setWord($stack, STACK_self, $value);                          }
/*end[CHENEY.COLLECTOR]*/

/*if[LISP2.COLLECTOR]*/
/*MAC*/int  StackChunk_getList(int $stack)                      { return getWord($stack, STACK_list);                           }
/*MAC*/void StackChunk_setList(int $stack, int $value)          { setWord($stack, STACK_list, $value);                          }
/*end[LISP2.COLLECTOR]*/

/*---------------------------------------------------------------------------*\
 *                                Frame methods                              *
\*---------------------------------------------------------------------------*/

/*MAC*/int  Frame_getCurrentMp(int $frame)                      { return getWord($frame, FRAME_currentMp);                      }
/*MAC*/int  Frame_getCurrentIp(int $frame)                      { return getWord($frame, FRAME_currentIp);                      }
/*MAC*/int  Frame_getPreviousLp(int $frame)                     { return getWord($frame, FRAME_previousLp);                     }
/*MAC*/int  Frame_getCurrentCp(int $frame)                      { return getWord($frame, FRAME_currentCp);                      }
/*MAC*/int  Frame_getStackOffset(int $frame)                    { return getWord($frame, FRAME_stackOffset);                    }
/*MAC*/int  Frame_getLocal(int $frame, int $x)                  { return getWord($frame, $x);                                   }
/*MAC*/long Frame_getLocalLong(int $frame, int $x)              { return getLongAtWord($frame, $x);                             }

/*MAC*/void Frame_setCurrentMp(int $frame, int $value)          {        setWord($frame, FRAME_currentMp, $value);              }
/*MAC*/void Frame_setCurrentIp(int $frame, int $value)          {        setWord($frame, FRAME_currentIp, $value);              }
/*MAC*/void Frame_setPreviousLp(int $frame, int $value)         {        setWord($frame, FRAME_previousLp, $value);             }
/*MAC*/void Frame_setCurrentCp(int $frame, int $value)          {        setWord($frame, FRAME_currentCp, $value);              }
/*MAC*/void Frame_setStackOffset(int $frame, int $value)        {        setWord($frame, FRAME_stackOffset, $value);            }
/*MAC*/void Frame_setLocal(int $frame, int $x, int $value)      {        setWord($frame, $x, $value);                           }
/*MAC*/void Frame_setLocalLong(int $frame, int $x, long $value) {        setLongAtWord($frame, $x, $value);                     }


/*IFJ*/}
