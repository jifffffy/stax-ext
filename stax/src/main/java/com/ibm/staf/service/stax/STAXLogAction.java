/*****************************************************************************/
/* Software Testing Automation Framework (STAF)                              */
/* (C) Copyright IBM Corp. 2002                                              */
/*                                                                           */
/* This software is licensed under the Eclipse Public License (EPL) V1.0.    */
/*****************************************************************************/

package com.ibm.staf.service.stax;

import com.ibm.staf.STAFResult;
import com.ibm.staf.STAFUtil;

import java.util.HashMap;

public class STAXLogAction extends STAXActionDefaultImpl
{
    // Valid strings for the INVALIDLOGLEVELACTION option and their codes

    public static final int RAISESIGNAL = 1;
    public static final String RAISESIGNAL_STRING = new String("RaiseSignal");
    public static final int LOGINFO = 2;
    public static final String LOGINFO_STRING = new String("LogInfo");
    
    public STAXLogAction()
    { /* Do Nothing */ }

    public STAXLogAction(String message, String level, int logfile)
    { 
        fUnevalMessage = message;
        fMessage = message;
        fUnevalLevel = level;
        fLevel = level;
        fLogfile = logfile;
    }

    public STAXLogAction(String message, String level, String messageAttr,
                         String ifAttr, int logfile)
    { 
        fUnevalMessage = message;
        fMessage = message;
        fUnevalLevel = level;
        fLevel = level;
        fUnevalMessageAttr = messageAttr;
        fUnevalIf = ifAttr;
        fLogfile = logfile;
    }

    public String getMessage() { return fMessage; } 
    public void setMessage(String message)
    {
        fUnevalMessage = message;
        fMessage = message;
    }

    public String getLevel() { return fLevel; }
    public void setLevel(String level)
    { 
        fUnevalLevel = level;
        fLevel = level;
    }

    public boolean getMessageAttr() { return fMessageAttr; }
    public void setMessageAttr(String messageAttr)
    {
        fUnevalMessageAttr = messageAttr;
    }

    public boolean getIf() { return fIf; }
    public void setIf(String ifValue)
    { 
        fUnevalIf = ifValue;
    }

    public int getLogfile() { return fLogfile; } 
    public void setLogfile(int logfile) { fLogfile = logfile; }

    public String getXMLInfo()
    {
        StringBuffer info = new StringBuffer("<log");

        if (!fUnevalLevel.equals("'info'"))
            info.append(" level=\"").append(fUnevalLevel).append("\"");
        if (!fUnevalMessageAttr.equals("STAXMessageLog"))
            info.append(" message=\"").append(fUnevalMessageAttr).append("\"");
        if (!fUnevalIf.equals("1"))
            info.append(" if=\"").append(fUnevalIf).append("\"");
        info.append(">").append(fUnevalMessage).append("</log>");

        return info.toString();
    } 

    public String getInfo()
    {
        int msgLength = fMessage.length();
        if (msgLength > 40)
            return fMessage.substring(0, 40) + "...";
        else
            return fMessage;
    } 

    public String getDetails()
    {
        return "Level:" + fLevel +
               ";Message:" + fMessage +
               ";MessageAttr:" + fMessageAttr +
               ";If:" + fIf +
               ";Logfile:" + fLogfile;
    } 

    public void execute(STAXThread thread)
    {
        fThread = thread;
        String evalElem = getElement();
        String evalAttr = "if";
        
        try
        {
            fIf = thread.pyBoolEval(fUnevalIf);
            
            if (!fIf)
            {   // Ignore log element if "if" attribute evaluates to FALSE
                fThread.popAction();
                return;
            }

            evalAttr = STAXElementInfo.NO_ATTRIBUTE_NAME;
            fMessage = thread.pyStringEval(fUnevalMessage);

            evalAttr = "level";
            fLevel = thread.pyStringEval(fUnevalLevel);

            evalAttr = "message";
            fMessageAttr = thread.pyBoolEval(fUnevalMessageAttr);
        }
        catch (STAXPythonEvaluationException e)
        {
            fThread.popAction();

            setElementInfo(new STAXElementInfo(evalElem, evalAttr));

            fThread.setSignalMsgVar(
                "STAXPythonEvalMsg", STAXUtil.formatErrorMessage(this), e);

            fThread.raiseSignal("STAXPythonEvaluationError");
            return;
        }
        
        if (fMessageAttr)
        {
            // Send a message to the STAXMonitor (via an event)

            STAXTimestamp timestamp = new STAXTimestamp();

            HashMap<String, String> messageMap = new HashMap<String, String>();
            messageMap.put("messagetext", timestamp.getTimestampString() + 
                " " + STAFUtil.maskPrivateData(fMessage));

            fThread.getJob().generateEvent(
                STAXMessageActionFactory.STAX_MESSAGE, messageMap);
        }

        // Log the message

        STAFResult result = fThread.getJob().log(fLogfile, fLevel, fMessage);

        if ((result.rc == 4004) &&
            (fThread.getJob().getInvalidLogLevelAction() == LOGINFO))
        {
            // Invalid log level.  Log using "Info" for the log level.
            result = fThread.getJob().log(fLogfile, "Info", fMessage);
        }
        
        if (result.rc != 0 && result.rc != 2)
        {
            // Raise a STAXLogError signal

            fThread.popAction();

            String logFileName = "STAX Job User Log";

            if (fLogfile == STAXJob.JOB_LOG)
            {
                logFileName = "STAX Job Log";
            }
            else if (fLogfile == STAXJob.SERVICE_LOG)
            {
                logFileName = "STAX Service Log";
            }

            String msg = "Request to LOG service to log to the " +
                logFileName + " failed with RC: " + result.rc +
                " Result: " + result.result;

            if (result.rc == 4004)
                msg += "\n\nInvalid log level: " + fLevel;
            else
                msg += "\n\nLevel: " + fLevel + "  Message: " + fMessage;

            setElementInfo(new STAXElementInfo(
                evalElem, STAXElementInfo.NO_ATTRIBUTE_NAME, msg));

            fThread.setSignalMsgVar(
                "STAXLogMsg", STAXUtil.formatErrorMessage(this));
            
            fThread.raiseSignal("STAXLogError");

            return;
        } 
         
        fThread.popAction();
    }

    public void handleCondition(STAXThread thread, STAXCondition cond)
    {
        thread.popAction();
    }

    public STAXAction cloneAction()
    {
        STAXLogAction clone = new STAXLogAction();

        clone.setElement(getElement());
        clone.setLineNumberMap(getLineNumberMap());
        clone.setXmlFile(getXmlFile());
        clone.setXmlMachine(getXmlMachine());

        clone.fUnevalMessage = fUnevalMessage;
        clone.fMessage = fMessage;
        clone.fUnevalLevel = fUnevalLevel;
        clone.fLevel = fLevel;
        clone.fUnevalMessageAttr = fUnevalMessageAttr;
        clone.fMessageAttr = fMessageAttr;
        clone.fUnevalIf = fUnevalIf;
        clone.fIf = fIf;
        clone.fLogfile = fLogfile;

        return clone;
    }   

    /**
     * Checks if a specified Invalid Log Level Action value is valid.
     * @param output A String containing the Invalid Log Level Action
     * @return STAFResult A STAFResult object.  If not valid, returns a
     * STAFResult with an InvalidValue RC and an error message in the result.
     * If valid, returns RC 0 with the integer version of the Invalid Log Level
     * Action string value in the result.
     */
    public static STAFResult isValidInvalidLogLevelAction(String action)
    {
        if (action.equalsIgnoreCase(RAISESIGNAL_STRING))
        {
            return new STAFResult(
                STAFResult.Ok, Integer.toString(RAISESIGNAL));
        }    	
        else if (action.equalsIgnoreCase(LOGINFO_STRING))
        {
            return new STAFResult(
                STAFResult.Ok, Integer.toString(LOGINFO));
        }
        else
        {
            return new STAFResult(
                STAFResult.InvalidValue,
                "Invalid value for INVALIDLOGLEVELACTION: " + action +
                ".  Valid values: " + RAISESIGNAL_STRING + " or " +
                LOGINFO_STRING);
        }
    }

    /**
     * Converts an int Invalid Log Level Action flag to it's string
     * representation.
     * @param output An int representing the Invalid Log Level Action flag
     */
    public static String getInvalidLogLevelActionAsString(int action)
    {
        if (action == RAISESIGNAL)
            return RAISESIGNAL_STRING;
        else 
            return LOGINFO_STRING;
    } 
    
    STAXThread fThread = null;

    private String fUnevalMessage = new String();
    private String fUnevalLevel = new String("'info'");
    private String fUnevalMessageAttr = "STAXMessageLog";
    private String fUnevalIf = "1";
    private String fMessage = new String();
    private String fLevel = new String();
    private boolean fMessageAttr = false;
    private boolean fIf = true;
    private int fLogfile;
}
