/*
 * Copyright 1994-2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */

package java.lang;

import java.io.*;
import java.util.*;
//import java.lang.isolate.*;
import javax.microedition.io.*;
import com.sun.squawk.util.*;

/**
 * A <i>thread</i> is a thread of execution in a program. The Java
 * Virtual Machine allows an application to have multiple threads of
 * execution running concurrently.
 * <p>
 * Every thread has a priority. Threads with higher priority are
 * executed in preference to threads with lower priority.
 * <p>
 * There are two ways to create a new thread of execution. One is to
 * declare a class to be a subclass of <code>Thread</code>. This
 * subclass should override the <code>run</code> method of class
 * <code>Thread</code>. An instance of the subclass can then be
 * allocated and started. For example, a thread that computes primes
 * larger than a stated value could be written as follows:
 * <p><hr><blockquote><pre>
 *     class PrimeThread extends Thread {
 *         long minPrime;
 *         PrimeThread(long minPrime) {
 *             this.minPrime = minPrime;
 *         }
 *
 *         public void run() {
 *             // compute primes larger than minPrime
 *             &nbsp;.&nbsp;.&nbsp;.
 *         }
 *     }
 * </pre></blockquote><hr>
 * <p>
 * The following code would then create a thread and start it running:
 * <p><blockquote><pre>
 *     PrimeThread p = new PrimeThread(143);
 *     p.start();
 * </pre></blockquote>
 * <p>
 * The other way to create a thread is to declare a class that
 * implements the <code>Runnable</code> interface. That class then
 * implements the <code>run</code> method. An instance of the class can
 * then be allocated, passed as an argument when creating
 * <code>Thread</code>, and started. The same example in this other
 * style looks like the following:
 * <p><hr><blockquote><pre>
 *     class PrimeRun implements Runnable {
 *         long minPrime;
 *         PrimeRun(long minPrime) {
 *             this.minPrime = minPrime;
 *         }
 *
 *         public void run() {
 *             // compute primes larger than minPrime
 *             &nbsp;.&nbsp;.&nbsp;.
 *         }
 *     }
 * </pre></blockquote><hr>
 * <p>
 * The following code would then create a thread and start it running:
 * <p><blockquote><pre>
 *     PrimeRun p = new PrimeRun(143);
 *     new Thread(p).start();
 * </pre></blockquote>
 * <p>
 *
 *
 * @author  unascribed
 * @see     java.lang.Runnable
 * @see     java.lang.Runtime#exit(int)
 * @see     java.lang.Thread#run()
 * @since   JDK1.0
 */

/*
 * Note - All the code in this class is run with preemption disabled
 */

public class Thread implements Runnable {

    /*@vmaccessed: */

    /**
     * The minimum priority that a thread can have.
     */
    public final static int MIN_PRIORITY = 1;

   /**
     * The default priority that is assigned to a thread.
     */
    public final static int NORM_PRIORITY = 5;

    /**
     * The maximum priority that a thread can have.
     */
    public final static int MAX_PRIORITY = 10;

    /**
     * Returns a reference to the currently executing thread object.
     *
     * @return  the currently executing thread.
     */
    public static Thread currentThread() {
        if (scheduler == null) {
            return null;
        }
        return scheduler.currentThread;
    }

    /**
     * setDaemon
     */
    public void setDaemon(boolean value) {
        throw new RuntimeException("Thread::setDaemon");
    }


    /**
     * Causes the currently executing thread to sleep (temporarily cease
     * execution) for the specified number of milliseconds. The thread
     * does not lose ownership of any monitors.
     *
     * @param      millis   the length of time to sleep in milliseconds.
     * @exception  InterruptedException if another thread has interrupted
     *             the current thread.  The <i>interrupted status</i> of the
     *             current thread is cleared when this exception is thrown.
     * @see        java.lang.Object#notify()
     */
    public static void sleep(long delta) throws InterruptedException {
        if (scheduler.trace) {
            Native.println("Thread sleep("+delta+") "+scheduler.currentThread.threadNumber);
        }
        if (delta < 0) {
            throw new IllegalArgumentException("negitive sleep time");
        }
        if (delta > 0) {
            startFinalizers();
            scheduler.timerQueue.add(scheduler.currentThread, delta);
            reschedule();
        }
    }

   /**
     * Allocates a new <code>Thread</code> object.
     * <p>
     * Threads created this way must have overridden their
     * <code>run()</code> method to actually do anything.
     *
     * @see     java.lang.Runnable
     */
    public Thread() {
        this(null);
    }

    /**
     * Allocates a new <code>Thread</code> object with a
     * specific target object whose <code>run</code> method
     * is called.
     *
     * @param   target   the object whose <code>run</code> method is called.
     */
    public Thread(Runnable target) {
        SchedulerState temp = null;
       /*
        * This is the path of the original thread. If the scheduler state has not been
        * created, then this is the bootstrap thread and the state must now be created.
        */
        if (scheduler == null) {
            temp = SchedulerState.create();
            scheduler = temp;
        }
        if (temp != null) {
            Native.assume(scheduler == temp);
        }

        this.threadNumber = scheduler.nextThreadNumber++;
        this.target       = target;
        this.state        = NEW;
        //this.isolate      = Isolate.getCurrentIsolate();
        if (scheduler.currentThread != null) {
            priority = scheduler.currentThread.getPriority();
        } else {
            priority = NORM_PRIORITY;
        }
    }

    /**
     * Constructor only called when creating the master isolate.
     */
    Thread(int threadNumber) {
        this.threadNumber = threadNumber;
        this.target       = target;
        this.state        = NEW;
        //this.isolate      = null;
        this.priority     = NORM_PRIORITY;
    }

    /**
     * setIsolate
     */
    //void setIsolate(Isolate isolate) {
    //    this.isolate = isolate;
    //}

   /**
    * Causes the currently executing thread object to temporarily pause
    * and allow other threads to execute.
    */
    public static void yield() {
        if (scheduler == null) {
            return;
        }
        if (scheduler.trace) {
            Native.println("Thread yield() "+scheduler.currentThread.threadNumber);
        }
        startFinalizers();
        scheduler.runnableThreads.add(scheduler.currentThread);
        reschedule();
    }

    /**
     * Causes this thread to begin execution; the Java Virtual Machine
     * calls the <code>run</code> method of this thread.
     * <p>
     * The result is that two threads are running concurrently: the
     * current thread (which returns from the call to the
     * <code>start</code> method) and the other thread (which executes its
     * <code>run</code> method).
     *
     * @exception  IllegalThreadStateException  if the thread was already
     *               started.
     * @see        java.lang.Thread#run()
     */

    public void start() {
        baptiseThread();                                        // Get any errors out of the way first.
        scheduler.runnableThreads.add(scheduler.currentThread); // Prefer the current thread to run before the new one
        scheduler.runnableThreads.add(this);
        reschedule();
    }


    /**
     * Special thread starter that does not assume that the code is being called
     * from a 'real' thread.
     */
    void startPrimaryThread() {
        baptiseThread();
        scheduler.runnableThreads.add(this);
        reschedule();
    }

    /**
     * startFinalizers
     */
    static void startFinalizers() {
/*if[FINALIZATION]*/
        while (true) {
            ObjectAssociation entry = Native.getFinalizer() ;
            if (entry == null) {
                 break;
            }
            Thread t = new Thread(entry);
            t.baptiseThread();
            scheduler.runnableThreads.add(t);
        }
/*end[FINALIZATION]*/
    }


    /**
     * baptiseThread
     */
    private void baptiseThread() {

       /*
        * Check that the new thread is only started once, and set its state.
        */
        if (state != NEW) {
            throw new IllegalThreadStateException();
        }
        state = ALIVE;

        scheduler.aliveThreads++;

       /*
        * Add the new thread to the list of runnable threads and reschedule the VM.
        */
        context = Native.getNewExecutionContext(this);
        if (context == null) {
            Native.gc();
            context = Native.getNewExecutionContext(this);
            if (context == null) {
                throw VMExtension.outOfMemoryError;
            }
        }
    }

    /**
     * If this thread was constructed using a separate
     * <code>Runnable</code> run object, then that
     * <code>Runnable</code> object's <code>run</code> method is called;
     * otherwise, this method does nothing and returns.
     * <p>
     * Subclasses of <code>Thread</code> should override this method.
     *
     * @see     java.lang.Thread#start()
     * @see     java.lang.Runnable#run()
     */
    public void run() {
        if (target != null) {
            target.run();
        }
    }

    /**
     * Tests if this thread is alive. A thread is alive if it has
     * been started and has not yet died.
     *
     * @return  <code>true</code> if this thread is alive;
     *          <code>false</code> otherwise.
     */
    public final boolean isAlive() {
        return state == ALIVE;
    }

    /**
     * isDead
     */
    protected boolean isDead() {
        return state == DEAD;
    }

    /**
     * Changes the priority of this thread.
     *
     * @param newPriority priority to set this thread to
     * @exception  IllegalArgumentException  If the priority is not in the
     *             range <code>MIN_PRIORITY</code> to
     *             <code>MAX_PRIORITY</code>.
     * @see        #getPriority
     * @see        java.lang.Thread#getPriority()
     * @see        java.lang.Thread#MAX_PRIORITY
     * @see        java.lang.Thread#MIN_PRIORITY
     */
    public final void setPriority(int newPriority) {
        if (newPriority > MAX_PRIORITY || newPriority < MIN_PRIORITY) {
            throw new IllegalArgumentException();
        }
        priority = newPriority;
    }

    /**
     * Returns this thread's priority.
     *
     * @return  this thread's name.
     * @see     #setPriority
     * @see     java.lang.Thread#setPriority(int)
     */
    public final int getPriority() {
        return priority;
    }

    /**
     * Returns the current number of active threads in the VM.
     *
     * @return the current number of active threads
     */
    public static int activeCount() {
        return scheduler.runnableThreads.size() + 1;
    }

    /**
     * Waits for this thread to die.
     *
     * @exception  InterruptedException if another thread has interrupted
     *             the current thread.  The <i>interrupted status</i> of the
     *             current thread is cleared when this exception is thrown.
     */
    public final void join() throws InterruptedException {
        if (this == scheduler.currentThread) {
            return;
        }
        while (isAlive()) {
           /*
            * Set the priority of the current thread equal to that of the
            * thread being joined, and then yield.
            */
            int save = scheduler.currentThread.priority;
            scheduler.currentThread.priority = priority;
            yield();
            scheduler.currentThread.priority = save;
        }
    }

    /**
     * Returns a string representation of this thread, including a unique number
     * that identifies the thread and the thread's priority.
     *
     * @return  a string representation of this thread.
     */
    public String toString() {
        // Can't use StringBuffer as threading may have not been initialized.
        return "Thread[".concat(String.valueOf(threadNumber)).concat(" (pri=").concat(String.valueOf(getPriority())).concat(")]");
    }


   /* ------------------------------------------------------------------------ *\
    *                              Private stuff                               *
   \* ------------------------------------------------------------------------ */

    /**
     * callRun - This is called by the VM when a new thread is started.
     * The call sequence is that Thread.start() calls Thread.reschedule()
     * calls Thread.switchAndEnablePreemption() which calls this function.
     */
    void callRun() {

        //isolate.addThread(this);

            if (scheduler.trace) {
                Native.println("Thread start ".concat(String.valueOf(threadNumber)));
            }

            try {
                run();
            } catch (ExitVMError ex) {
                // Print nothing because this is the result of System.exit();
            } catch (Throwable ex) {
                Native.print("Uncaught exception ");
                Native.println(ex);
                ex.printStackTrace();
            }
            if (scheduler.trace) {
                Native.println("Thread exit ".concat(String.valueOf(threadNumber)));
            }
            state = DEAD;

        //isolate.removeThread(this);

        scheduler.aliveThreads--;
        reschedule();
        Native.fatalVMError();  // Catch VM errors here.
    }

    /**
     * Context switch to another thread
     */
    private static void reschedule() {
        Thread thread;
        boolean oldIsDead = false;

        Thread oldThread  = scheduler.currentThread;

        if (oldThread != null && oldThread.state == DEAD) {
            oldIsDead = true;
        }

        scheduler.currentThread = null; // safety

       /*
        * Loop until there is something to do
        */
        for (;;) {

           /*
            * Add any threads that are ready to be restarted.
            */
            int event;
            while ((event = Native.getEvent()) != 0) {
                signalEvent(event);
            }

           /*
            * Add any threads waiting for a certain time that
            * are now due.
            */
            while ((thread = scheduler.timerQueue.next()) != null) {
                Monitor monitor = thread.monitor;
                if (monitor != null) {

                   /*
                    * If the thread is wait()ing on a monitor then remove it
                    * from the conditional variable wait queue and reclaim the
                    * lock on the monitor. (If this is available then the thread
                    * will be added to the run queue.)
                    */
                    monitor.removeCondvarWait(thread);
                    addMonitorWait(monitor, thread);
                } else {

                   /*
                    * Otherwise it is just waking up from a sleep() so it is now
                    * ready to run
                    */
                    scheduler.runnableThreads.add(thread);
                }
            }

           /*
            * Break if there is something to do
            */
            if ((thread = scheduler.runnableThreads.next()) != null) {
                break;
            }

           /*
            * Stop if there are no runnable threads
            */
            if (scheduler.aliveThreads == 0) {
                VMExtension.stopVM(0);
            }

           /*
            * Wait for an event or until timeout
            */
            long delta = scheduler.timerQueue.nextDelta();
            if (delta > 0) {
                Native.waitForEvent(delta);
            }
        }

       /*
        * Set the current thread
        */
        scheduler.currentThread = thread;
        Native.assume(scheduler.currentThread != null);

       /*
        * Set up the correct isolate context
        */
        //thread.isolate.makeCurrent();

       /*
        * The following will either return to a previous context or cause
        * VMExtension.callrun() to be entered if currentThread is a new thread.
        */
        Native.setExecutionContext(oldThread, oldIsDead, thread, thread.threadNumber);
    }



    /**
     * Block a thread
     */
    static void waitForEvent(int event) {
        startFinalizers();
        scheduler.events.put(event, scheduler.currentThread);
        reschedule();
    }


    /**
     * Restart a blocked thread
     */
    private static void signalEvent(int event) {
        Thread thread = (Thread)scheduler.events.remove(event);
        Native.assume(thread != null);
        scheduler.runnableThreads.add(thread);
    }


    /**
     * throwBadMonitorStateException
     */
    private static void throwBadMonitorStateException() {
/*if[FATALMONITORERRORS]*/
        Native.fatalVMError();
/*end[FATALMONITORERRORS]*/
        throw new IllegalMonitorStateException();
    }

    /**
     * addMonitorWait
     */
    private static void addMonitorWait(Monitor monitor, Thread thread) {

       /*
        * Check the nesting depth
        */

        Native.assume(thread.monitorDepth > 0);
       /*
        * Add to the wait queue
        */

        monitor.addMonitorWait(thread);
       /*
        * If the wait queue has no owner then try and start a
        * waiting thread.
        */

        if (monitor.owner == null) {
            removeMonitorWait(monitor);
        }
    }

    /**
     * removeMonitorWait
     */
    private static void removeMonitorWait(Monitor monitor) {

       /*
        * Try and remove a thread from the wait queue
        */
        Thread waiter = monitor.removeMonitorWait();
        if (waiter != null) {

           /*
            * Set the monitor's ownership and nesting depth
            */
            monitor.owner = waiter;
            monitor.depth = waiter.monitorDepth;
            Native.assume(waiter.monitorDepth > 0);

           /*
            * Restart execution of the thread
            */
            scheduler.runnableThreads.add(waiter);

        } else {

           /*
            * No thread is waiting for this monitor,
            * so mark it as unused
            */
            monitor.owner = null;
            monitor.depth = 0;
        }
    }


    /**
     * monitiorEnter
     */
    static void monitorEnter(Object object) {
        Native.assume(scheduler.currentThread != null);
        Monitor monitor = getMonitor(object);
        if (monitor.owner == null) {

           /*
            * Unowned monitor, make the current thread the owner
            */
            monitor.owner = scheduler.currentThread;
            monitor.depth = 1;

        } else if (monitor.owner == scheduler.currentThread) {

           /*
            * Thread already owns the monitor, increment depth
            */
            monitor.depth++;
            Native.assume(monitor.depth < 32767);

        } else {

           /*
            * Add to the wait queue and set the depth for when thread
            * is restarted
            */
            scheduler.currentThread.monitorDepth = 1;
            addMonitorWait(monitor, scheduler.currentThread);
            reschedule();

           /*
            * Safety...
            */
            Native.assume(monitor.owner == scheduler.currentThread);
            scheduler.currentThread.monitor = null;
            scheduler.currentThread.monitorDepth = 0;
        }
    }

    /**
     * monitiorExit
     */
    static void monitorExit(Object object) {
        Native.assume(scheduler.currentThread != null);
        Monitor monitor = getMonitor(object);

       /*
        * Throw an exception if things look bad
        */
        if (monitor.owner != scheduler.currentThread) {
            throwBadMonitorStateException();
        }

       /*
        * Safety.
        */
        Native.assume(monitor.depth > 0);

       /*
        * Try and restart a thread if the nesting depth is zero
        */
        if (--monitor.depth == 0) {
            removeMonitorWait(monitor);
        }

       /*
        * Free off the monitor if it has no owner
        */
        if (monitor.owner == null && monitor.condvarQueue == null) {
            freeMonitor(object, monitor);
        }
    }



    /**
     * monitiorWait
     */
    static void monitorWait(Object object, long delta) throws InterruptedException {
        Monitor monitor = getMonitor(object);

       /*
        * Throw an exception if things look bad
        */
        if (monitor.owner != scheduler.currentThread) {
            throwBadMonitorStateException();
        }

       /*
        * Add to timer queue if time is > 0
        */
        if (delta > 0) {
            scheduler.timerQueue.add(scheduler.currentThread, delta);
        }

       /*
        * Save the nesting depth so it can be restored when it
        * regains the monitor.
        */
        scheduler.currentThread.monitorDepth = monitor.depth;

       /*
        * Add to the wait queue
        */
        monitor.addCondvarWait(scheduler.currentThread);

       /*
        * Having relinquishing the monitor get the next thread
        * off the wait queue.
        */
        removeMonitorWait(monitor);

       /*
        * Wait for a notify or a timeout.
        */
        Native.assume(monitor.condvarQueue != null);
        Native.assume(scheduler.currentThread.monitor == monitor);
        reschedule();

       /*
        * Safety...
        */
        Native.assume(monitor.owner == scheduler.currentThread);
        scheduler.currentThread.monitor = null;
        scheduler.currentThread.monitorDepth = 0;
    }


    /**
     * monitorNotify
     */
    static void monitorNotify(Object object, boolean notifyAll) {
        Monitor monitor = getMonitor(object);
        int count = 0;

       /*
        * Throw an exception if things look bad
        */
        if (monitor.owner != scheduler.currentThread) {
            throwBadMonitorStateException();
        }

       /*
        * Try and restart a thread
        */
        do {

           /*
            * Get the next waiting thread
            */
            Thread waiter = monitor.removeCondvarWait();
            if (waiter == null) {
                break;
            }

           /*
            * Bump the release counter.
            */
            count++;

           /*
            * Remove timeout is there was one and restart
            */
            scheduler.timerQueue.remove(waiter);
            addMonitorWait(monitor, waiter);

       /*
        * Loop if it this is a notifyAll operation.
        */
        } while (notifyAll);

       /*
        * If the notify released something then yield.
        */
        if (count > 0) {
            scheduler.runnableThreads.add(scheduler.currentThread);
            reschedule();
        }
    }


    /**
     * getMonitor
     */
    private static Monitor getMonitor(Object object) {
        Monitor monitor =  ObjectAssociation.getMonitor(object);
        monitor.isInUse = true;
        return monitor;
    }

    /**
     * freeMonitor
     */
    private static void freeMonitor(Object object, Monitor monitor) {
        Native.assume(monitor.owner == null);
        Native.assume(monitor.monitorQueue == null);
        Native.assume(monitor.condvarQueue == null);
        Native.assume(monitor.depth == 0);
        monitor.isInUse = false;
        ObjectAssociation.purgeAssociations(object);
    }

    /**
     * setInQueue
     */
    protected void setInQueue() {
        Native.assume(!inQueue);
        inQueue = true;
    }

    /**
     * setNotInQueue
     */
    protected void setNotInQueue() {
        Native.assume(inQueue);
        inQueue = false;
    }

    /**
     * setInTimerQueue
     */
    protected void setInTimerQueue() {
        Native.assume(!inTimerQueue);
        inTimerQueue = true;
    }

    /**
     * setNotInTimerQueue
     */
    protected void setNotInTimerQueue() {
        Native.assume(inTimerQueue);
        inTimerQueue = false;
    }

   /* ------------------------------------------------------------------------ *\
    *                               Global state                               *
   \* ------------------------------------------------------------------------ */

    /**
     * This is the pointer to the object holding all the global threading state
     * that must be shared across all Isolates. It's value is initialized in the
     * Thread.startPrim method.
     */
    static SchedulerState scheduler;

    /**
     * Return whether or the threading system has been initialized. This is
     * a pre-requisite for a number of actions, the use of StringBuffer (which
     * has synchronized methods) being a good example.
     */
    static boolean threadingInitialized() { return scheduler != null; }

    static boolean tracing() { return scheduler != null && scheduler.trace; }

   /* ------------------------------------------------------------------------ *\
    *                              Instance state                              *
   \* ------------------------------------------------------------------------ */

    private final static int NEW   = 0;
    private final static int ALIVE = 1;
    private final static int DEAD  = 2;

    /**
     * The saved activation frame of a non-active thread.
     */
    private Object context;  /*@vmaccessed: read write */

    private   Runnable target;          /* Target to run (if run() is not overridden)                        */
    int       priority;                 /* Execution priority                                                */
    private   int state;                /* Aliveness                                                         */
    boolean   inQueue;                  /* Flag to show if thread is in a queue                              */
    Thread    nextThread;               /* For enqueueing in the ready, monitor wait, or condvar wait queues */
    boolean   inTimerQueue;             /* Flag to show if thread is in a queue                              */
    Thread    nextTimerThread;          /* For enqueueing in the timer queue                                 */
    long      time;                     /* Time to emerge from the timer queue                               */
    short     monitorDepth;             /* Saved monitor nesting depth                                       */
    Monitor   monitor;                  /* Monitor when thread is in the condvar queue                       */
    final int threadNumber;             /* The 'name' of the thread                                          */
  //Isolate   isolate;                  /* The Isolate under which the thread is running                     */

}



/* ======================================================================== *\
 *                                 Monitor                                  *
\* ======================================================================== */

/*
 * Note - All the code in the following class is run with preemption disabled
 */

class Monitor {

    /*@vmaccessed: */

    /**
     * The thread that owns the monitor.
     */
    Thread owner;

    /**
     * Queue of threads waiting to claim the monitor.
     */
    Thread monitorQueue;
    /**
     * Queue of threads waiting to claim the object.
     */
    Thread condvarQueue;

    /**
     * Nesting depth.
     */
    short depth;

    /**
     * Flag indicating if the monitor is in use.
     */
    boolean isInUse; /*@vmaccessed: read */

    /*
     * Constructor
     */
    public Monitor() {
    }

    /**
     * addMonitorWait
     */
    void addMonitorWait(Thread thread) {
        thread.setInQueue();
        Native.assume(thread.nextThread == null);
        Thread next = monitorQueue;
        if (next == null) {
            monitorQueue = thread;
        } else {
            while (next.nextThread != null) {
                next = next.nextThread;
            }
            next.nextThread = thread;
        }
    }

    /**
     * removeMonitorWait
     */
    Thread removeMonitorWait() {
        Thread thread = monitorQueue;
        if (thread != null) {
            monitorQueue = thread.nextThread;
            thread.setNotInQueue();
            thread.nextThread = null;
        }
        return thread;
    }

    /**
     * addCondvarWait
     */
    void addCondvarWait(Thread thread) {
        thread.setInQueue();
        thread.monitor = this;
        Native.assume(thread.nextThread == null);
        Thread next = condvarQueue;
        if (next == null) {
            condvarQueue = thread;
        } else {
            while (next.nextThread != null) {
                next = next.nextThread;
            }
            next.nextThread = thread;
        }
    }

    /**
     * removeCondvarWait
     */
    Thread removeCondvarWait() {
        Thread thread = condvarQueue;
        if (thread != null) {
            condvarQueue = thread.nextThread;
            thread.setNotInQueue();
            thread.monitor = null;
            thread.nextThread = null;
        }
        return thread;
    }

    /**
     * removeCondvarWait
     */
    void removeCondvarWait(Thread thread) {
        if (thread.inQueue) {
            Thread next = condvarQueue;
            Native.assume(next != null);
            if (next == thread) {
                condvarQueue = thread.nextThread;
            } else {
                while (next.nextThread != thread) {
                    next = next.nextThread;
                }
                if (next.nextThread == thread) {
                    next.nextThread = thread.nextThread;
                }
            }
            thread.setNotInQueue();
            thread.monitor = null;
            thread.nextThread = null;
        }
    }

/*
    void removeCondvarWait(Thread thread) {
        if (thread.inQueue) {
            thread.setNotInQueue();
            thread.monitor = null;
            Thread next = condvarQueue;
            if (next != null) {
                if (next == thread) {
                    condvarQueue = thread.nextThread;
                    thread.nextThread = null;
                    return;
                }
                while (next.nextThread != thread) {
                    next = next.nextThread;
                }
                if (next.nextThread == thread) {
                    next.nextThread = thread.nextThread;
                }
                thread.nextThread = null;
                return;
            }
        }
    }
*/
}


/* ======================================================================== *\
 *                               ThreadQueue                                *
\* ======================================================================== */

class ThreadQueue {

    Thread first;
    int count;

    /**
     * add
     */
    void add(Thread thread) {
//        print("add++");
        if (thread != null) {
            thread.setInQueue();
            if (first == null) {
                first = thread;
            } else {
                if (first.priority < thread.priority) {
                    thread.nextThread = first;
                    first = thread;
                } else {
                    Thread last = first;
                    while (last.nextThread != null && last.nextThread.priority >= thread.priority) {
                        last = last.nextThread;
                    }
                    thread.nextThread = last.nextThread;
                    last.nextThread = thread;
                }
            }
            count++;
        }
//        print("add--");
    }

    /**
     * size
     */
    int size() {
        return count;
    }

    /**
     * next
     */
    Thread next() {
//        print("sub++");
        Thread thread = first;
        if (thread != null) {
            thread.setNotInQueue();
            first = thread.nextThread;
            thread.nextThread = null;
            count--;
        }
//        print("sub--");
        return thread;
    }


    /**
     * print
     */
/*
    void print(String msg) {
        if (VMExtension.systemUp) {
            Native.print("Thread queue ");
            Native.print(msg);
            Native.print(":");

            Thread thread = first;
            while (thread != null) {
                Native.print(" ");
                Native.print(thread.threadNumber);
                thread = thread.nextThread;
            }
            Native.println();
        }
    }
*/
}


/* ======================================================================== *\
 *                                TimerQueue                                *
\* ======================================================================== */

/*
 * Note - All the code in the following class is run with preemption disabled
 */

class TimerQueue {

    Thread first;

    /**
     * add
     */
    void add(Thread thread, long delta) {
        Native.assume(thread.nextTimerThread == null);
        thread.setInTimerQueue();
        thread.time = System.currentTimeMillis() + delta;
        if (thread.time < 0) {

           /*
            * If delta is so huge that the time went negative then just make
            * it a very large value. The universe will end before the error
            * is detected!
            */
            thread.time = Long.MAX_VALUE;
        }
        if (first == null) {
            first = thread;
        } else {
            if (first.time > thread.time) {
                thread.nextTimerThread = first;
                first = thread;
            } else {
                Thread last = first;
                while (last.nextTimerThread != null && last.nextTimerThread.time < thread.time) {
                    last = last.nextTimerThread;
                }
                thread.nextTimerThread = last.nextTimerThread;
                last.nextTimerThread = thread;
            }
        }
    }

    /**
     * next
     */
    Thread next() {
        Thread thread = first;
        if (thread == null || thread.time > System.currentTimeMillis()) {
            return null;
        }
        first = first.nextTimerThread;
        thread.setNotInTimerQueue();
        thread.nextTimerThread = null;
        Native.assume(thread.time != 0);
        thread.time = 0;
        return thread;
    }

    /**
     * remove
     */
    void remove(Thread thread) {
        if (first == null || thread.time == 0) {
            Native.assume(!thread.inQueue);
            return;
        }
        thread.setNotInTimerQueue();
        if (thread == first) {
            first = thread.nextTimerThread;
            thread.nextTimerThread = null;
            return;
        }
        Thread p = first;
        while (p.nextTimerThread != null) {
            if (p.nextTimerThread == thread) {
                p.nextTimerThread = thread.nextTimerThread;
                thread.nextTimerThread = null;
                return;
            }
            p = p.nextTimerThread;
        }
        Native.fatalVMError();
    }

    /**
     * nextDelta
     */
    long nextDelta() {
        if (first != null) {
            long now = System.currentTimeMillis();
            if (now >= first.time) {
                return 0;
            }
            long res = first.time - now;
            if (VMExtension.tckMode && res > (1000*60)) {
                Native.print("Long wait in TCK ");
                Native.print(res);
                Native.println();
                System.exit(99);
            }
            return res;
        } else {
            if (VMExtension.tckMode) {
                Native.println("Infinite wait in TCK");
                System.exit(99);
            }
            return Long.MAX_VALUE;
        }
    }

}

/* ======================================================================== *\
 *                         SchedulerState                                   *
\* ======================================================================== */

/**
 * This class contains all the threading state that must be global across all Isolates.
 */
class SchedulerState {
    Thread          currentThread;      /* The current thread                           */
    int             aliveThreads;       /* Number of alive threads                      */
    ThreadQueue     runnableThreads;    /* Queue of runnable threads                    */
    TimerQueue      timerQueue;         /* Queue of timed waiting threads               */
    int             nextThreadNumber;   /* The 'name' of the next thread                */
    boolean         trace;              /* Trace flag                                   */
    IntHashtable    events;             /* Hashtable of pending threads                 */
    //int             nextHashCode;       /* The next hashcode to use                     */

    private SchedulerState() {
        nextThreadNumber    = 1; // Thread 0 is the bootstrap thread
        aliveThreads        = 0;
        //nextHashCode        = 0;
        trace               = false;
        runnableThreads     = new ThreadQueue();
        timerQueue          = new TimerQueue();
        events              = new IntHashtable(7);
    }

    static SchedulerState create() {
        return new SchedulerState();
    }
}
