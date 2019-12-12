/*****************************************************************************/
/* Software Testing Automation Framework (STAF)                              */
/* (C) Copyright IBM Corp. 2002                                              */
/*                                                                           */
/* This software is licensed under the Eclipse Public License (EPL) V1.0.    */
/*****************************************************************************/

package com.ibm.staf.service.stax;

import org.python.core.PyObject;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

/**
 * Class that represents a Python Interpreter for a thread in a STAX job
 */
public class STAXPythonInterpreter extends PythonInterpreter
{
    /**
     * Create a new STAXPythonInterpreter for the main thread.
     * Don't use.  Instead, use:
     *   STAXPythonInterpreter(PyObjects locals, PySystemState pySystemState,
     *                         STAXJob job)
     * and pass it null for the first argument when creating the Python
     * Interpreter for the main thread.
     * @deprecated
     */ 
    public STAXPythonInterpreter()
    {
        super(null, new PySystemState());
    }

    /**
     * Create a new STAXPythonInterpreter for a child thread.
     * Don't use.  Instead, use:
     *   STAXPythonInterpreter(PyObjects locals, PySystemState pySystemState,
     *                         STAXJob job)
     * @param locals A PyObject containing a dictionary of the Python
     * variables for the parent thread
     * @deprecated
     */ 
    public STAXPythonInterpreter(PyObject locals)
    {
        super(locals);
    }

    /**
     * Create a new STAXPythonInterpreter for a thread in a STAX job.
     * Don't use.  Instead, use:
     *   STAXPythonInterpreter(PyObjects locals, PySystemState pySystemState,
     *                         STAXJob job)
     * @param locals A PyObject containing a dictionary of the Python
     * variables for the parent thread (or null if creating the Python
     * Interpreter for the main thread)
     * @param pySystemState The PySystemState object that is to be used by
     * the Python interpreter
     * @deprecated
     */ 
    public STAXPythonInterpreter(PyObject locals, PySystemState pySystemState)
    {
        super(locals, pySystemState);

        // If locals is null, that indicates that this is the Python
        // Interpreter for the main thread, so need to save the PySystemState
        // so can use it for all child threads.  Note that the PySystemState
        // stores information like stdout for the PythonInterpreter (and we
        // want Jython print output to be output to the same location for all
        // threads in a STAX job).

        if (locals == null)
            fPySystemState = pySystemState;
    }
    
    /**
     * Create a new STAXPythonInterpreter for a thread in a STAX job.
     * @param locals A PyObject containing a dictionary of the Python
     * variables for the parent thread (or null if creating the Python
     * Interpreter for the main thread)
     * @param pySystemState The PySystemState object that is to be used by
     * the Python interpreter
     * @param job The STAXJob object representing the STAX job which created
     * this STAXPythonInterpreter
     */ 
    public STAXPythonInterpreter(PyObject locals, PySystemState pySystemState,
    	                         STAXJob job)
    {
        super(locals, pySystemState);

        // If locals is null, that indicates that this is the Python
        // Interpreter for the main thread, so need to save the PySystemState
        // so can use it for all child threads.  Note that the PySystemState
        // stores information like stdout for the PythonInterpreter (and we
        // want Jython print output to be output to the same location for all
        // threads in a STAX job).

        if (locals == null)
            fPySystemState = pySystemState;
        
        // The STAXjob object that created this STAXPythonInterpreter is needed
        // when calling the clonePyi method to ensure that the
        // STAXPythonFunction_CloneGlobals method is called in a thread-safe
        // manner.
        fJob = job;
    }
    
    /**
     * Clone global variables in the Python Interpreter to use for a child
     * thread.
     * @return STAXPythonInterpreter object representing the new cloned
     * Python interpreter
     */ 
    public STAXPythonInterpreter clonePyi()
    {
        // The STAXPythonFunction_CloneGlobals method calls Jython's
        // copy.deepcopy method and the copy.deepcopy method can hang (see
        // Bug #1521) if it is not called in a thread safe manner because
        // copy.deepcopy needs to maintain a memo table, and it does nothing to
        // maintain thread safety of that structure.  So, this method must
        // synchronize on the STAX job when calling the 
        // STAXPythonFunction_CloneGlobals method to ensure that cloning
        // global variables is performed in a thread-safe manner when called by
        // STAXThread when creating a new thread and when called by
        // STAXFunctionAction when calling a function with a "local" scope.    	
        synchronized(fJob)
        {
            // Clone globals, making a deep copy (where possible)
            this.exec(
                "STAXClonedGlobals = STAXPythonFunction_CloneGlobals(globals())");
        }
        
        // Return a new PythonInterpreter using a cloned copy of globals
        PyObject clonedGlobals = this.get("STAXClonedGlobals");
        this.exec("del STAXClonedGlobals");
        
        // Cloned Python interpreters for child threads share the same
        // PySystemState

        return new STAXPythonInterpreter(clonedGlobals, fPySystemState, fJob);
    }

    private PySystemState fPySystemState = null;
    private STAXJob fJob = null;
}
