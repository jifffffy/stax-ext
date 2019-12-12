/*****************************************************************************/
/* Software Testing Automation Framework (STAF)                              */
/* (C) Copyright IBM Corp. 2002                                              */
/*                                                                           */
/* This software is licensed under the Eclipse Public License (EPL) V1.0.    */
/*****************************************************************************/

package com.ibm.staf.service.stax;

public class STAXTimerAction extends STAXActionDefaultImpl
                             implements STAXTimedEventListener
{
    static final int INIT = 0;
    static final int CALLED_ACTION = 1;
    static final int COMPLETE = 2;

    static final String INIT_STRING = "INIT";
    static final String CALLED_ACTION_STRING = "CALLED_ACTION";
    static final String COMPLETE_STRING = "COMPLETE";
    static final String STATE_UNKNOWN_STRING = "UNKNOWN";
    
    // Used this variable to debug Bug #1511 "Timer code hangs upon execution".
    // When set to true, more information is provided for a timer element in
    // the call stack on a STAX QUERY JOB <Job#> THREAD <Thread#> request,
    // including information from the STAXTimedEventQueue.
    static final boolean DEBUG_TIMER = false;

    public STAXTimerAction()
    { /* Do Nothing */ }

    public STAXTimerAction(String durationString, STAXAction action)
    {
        fUnevalDurationString = durationString;
        fDurationString = durationString;
        fAction = action;
    }

    public void setDuration(String durationString)
    {
        fUnevalDurationString = durationString;
    }

    public void setTimerAction(STAXAction action)
    {
        fAction = action;
    }

    public String getStateAsString()
    {
        switch (fState)
        {
            case INIT:
                return INIT_STRING;
            case CALLED_ACTION:
                return CALLED_ACTION_STRING;
            case COMPLETE:
                return COMPLETE_STRING;
            default:
                return STATE_UNKNOWN_STRING;
        }
    }

    public String getXMLInfo()
    {
        return  "<timer duration=\"" + fDurationString + "\">";  
    }

    public String getInfo()
    {
        if (!DEBUG_TIMER)
        {
        	return fDurationString;       
        }
        else
        {
            // Add more information to help debug problems with a timer action
        	
            String timedEventQueueInfo = "N/A";

            if (fThread != null)
            {
                try
                {
                    timedEventQueueInfo = fThread.getJob().getTimedEventQueue().
                        getInfo();
                }
                catch (Throwable t)
                {
                    STAX.logToJVMLog(
                        "Error", fThread,
                        "STAXTimerAction::getInfo() - " +
                        "Caught exception at " +
                        "fThread.getJob().getTimedEventQueue().getInfo()");
                }
            }
            
            return fDurationString + ";" + fDebugState + ";" +
                getStateAsString() + ";" + timedEventQueueInfo;
        }        
    }

    public String getDetails()
    {
        return "Duration:" + fDurationString + 
               ";State:" + getStateAsString() +
               ";Action:" + fAction + 
               ";Thread:" + fThread +
               ";TimedEvent:" + fTimedEvent + 
               ";TimerExpiredCondition:" + fTimerExpiredCondition;
    }

    public void execute(STAXThread thread)
    {
        synchronized (this)
        {
            if (fState == INIT)
            {
                fThread = thread;
                long multiplier = 1;
                String tempDurationString = null;

                try
                {
                    fDurationString = fThread.pyStringEval(fUnevalDurationString);
                    
                    if (fDurationString.length() == 0)
                    {
                        throw new NumberFormatException(
                            "For an empty input string: \"\"");
                    }

                    switch (fDurationString.charAt(fDurationString.length() - 1))
                    {
                        case 's': { multiplier = 1000; break; }
                        case 'm': { multiplier = 60000; break; }
                        case 'h': { multiplier = 3600000; break; }
                        case 'd': { multiplier = 24 * 3600000; break; }
                        case 'w': { multiplier = 7 * 24 * 3600000; break; }
                        case 'y': { multiplier = 365 * 24 * 3600000; break; }
                        default: break;
                    }

                    if (multiplier == 1)
                        tempDurationString = fDurationString;
                    else
                        tempDurationString = fDurationString.substring(0,
                                             fDurationString.length() - 1);

                    fDuration = Long.parseLong(tempDurationString);
                    fDuration *= multiplier;
                }
                catch (NumberFormatException e)
                {
                    fState = COMPLETE;
                    fThread.popAction();
                    fThread.pySetVar("RC", new Integer(-1));

                    String msg = "The duration may be expressed in " +
                        "milliseconds, seconds, minutes, hours, days, weeks,"+
                        " or years and must be a valid Python string.  " +
                        "Its format is <Number>[s|m|h|d|w] where <Number> " +
                        "is an integer >= 0 and indicates milliseconds " +
                        "unless one of the following suffixes is specified:" +
                        "  s (for seconds), m (for minutes), h (for hours)," +
                        " d (for days), w (for weeks), or y (for years).  " +
                        "For example:\n" +
                        "  duration=\"'50'\" specifies 50 milliseconds\n" +
                        "  duration=\"'10s'\" specifies 10 seconds\n" +
                        "  duration=\"'5m'\" specifies 5 minutes\n" +
                        "  duration=\"'2h'\" specifies 2 hours\n" +
                        "  duration=\"'3d'\" specifies 3 days\n" +
                        "  duration=\"'1w'\" specifies 1 week\n" +
                        "  duration=\"'1y'\" specifies 1 year";

                    setElementInfo(new STAXElementInfo(
                        getElement(), "duration", msg));

                    fThread.setSignalMsgVar(
                        "STAXInvalidTimerValueMsg",
                        STAXUtil.formatErrorMessage(this) + "\n\n" + e);

                    fThread.raiseSignal("STAXInvalidTimerValue");

                    return;
                }
                catch (STAXPythonEvaluationException e)
                {
                    fState = COMPLETE;
                    fThread.popAction();
                    fThread.pySetVar("RC", new Integer(-1));

                    setElementInfo(new STAXElementInfo(
                        getElement(), "duration"));

                    fThread.setSignalMsgVar(
                        "STAXPythonEvalMsg",
                        STAXUtil.formatErrorMessage(this), e);

                    fThread.raiseSignal("STAXPythonEvaluationError");

                    return;
                }

                fThread.pushAction(fAction.cloneAction());
                fState = CALLED_ACTION;

                fTimedEvent = new STAXTimedEvent(System.currentTimeMillis() +
                                                 fDuration, this);
                fDebugState = 1;

                fThread.getJob().getTimedEventQueue().addTimedEvent(
                    fTimedEvent);

                fDebugState = 2; // Unexpired timer state
            }
            else if (fState == CALLED_ACTION)
            {
                fDebugState = 3;
                fState = COMPLETE;

                fThread.getJob().getTimedEventQueue().removeTimedEvent(
                    fTimedEvent);

                fDebugState = 4;
                fThread.popAction();
                fThread.pySetVar("RC", new Integer(0));
            }
            else
            {
                fDebugState = 5;
                fState = COMPLETE;
                fThread.popAction();
            }
        }
    }

    public void handleCondition(STAXThread thread, STAXCondition cond)
    {
        synchronized (this)
        {
            if (cond instanceof STAXTimerExpiredCondition)
            {
                thread.pySetVar("RC", new Integer(1));
            }
            else if (fState == CALLED_ACTION)
            {
                fDebugState = 6;
                thread.getJob().getTimedEventQueue().removeTimedEvent(
                    fTimedEvent);
            }

            fDebugState = 7;
            fState = COMPLETE;
            thread.removeCondition(fTimerExpiredCondition);
            thread.popAction();
        }
    }

    public STAXAction cloneAction()
    {
        STAXTimerAction clone = new STAXTimerAction(
            fUnevalDurationString, fAction);

        clone.setElement(getElement());
        clone.setLineNumberMap(getLineNumberMap());
        clone.setXmlFile(getXmlFile());
        clone.setXmlMachine(getXmlMachine());

        return clone;
    }

    // STAXTimedEventListener method

    public void timedEventOccurred(STAXTimedEvent timedEvent)
    {
        fDebugState = 8;
        
        synchronized (this)
        {
            fDebugState = 9;
            if (fState != COMPLETE)
            {
                fThread.addCondition(fTimerExpiredCondition);
                fThread.schedule();
            }
        }
    }

    private int fState = INIT;
    private String fUnevalDurationString = null;
    private String fDurationString = null;
    private long fDuration = 0;
    private STAXAction fAction = null;
    private STAXThread fThread = null;
    private STAXTimedEvent fTimedEvent = null;
    private STAXTimerExpiredCondition fTimerExpiredCondition =
        new STAXTimerExpiredCondition("Timer");
    int fDebugState = 0;
}
