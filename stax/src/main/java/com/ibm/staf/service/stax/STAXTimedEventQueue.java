/*****************************************************************************/
/* Software Testing Automation Framework (STAF)                              */
/* (C) Copyright IBM Corp. 2002                                              */
/*                                                                           */
/* This software is licensed under the Eclipse Public License (EPL) V1.0.    */
/*****************************************************************************/

package com.ibm.staf.service.stax;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.*;

public class STAXTimedEventQueue extends Thread
{
    static SimpleDateFormat sDateTime = new SimpleDateFormat(
        "yyyyMMdd-HH:mm:ss");
    
    /**
     * Starts the Timed Event Queue thread.
     */
    public STAXTimedEventQueue()
    {
        start();
    }

    /**
     * Ends the Timed Event Queue thread.
     */
    public synchronized void end()
    {
        fComplete = true;
        notify();
    }

    /**
     * Adds a timed event to the Timed Event Queue.
     */
    public synchronized void addTimedEvent(STAXTimedEvent timedEvent)
    {
        fTimedEvents.add(timedEvent);
        notify();
    }

    /**
     * Removes a timed event from the Timed Event Queue.
     */
    public synchronized void removeTimedEvent(STAXTimedEvent timedEvent)
    {
        fTimedEvents.remove(timedEvent);
        notify();
    }

    /**
     * Gets the number of entries in the Timed Event Queue.
     */
    public synchronized int size()
    {
        return fTimedEvents.size();
    }
    
    /**
     * Gets the contents of the Timed Event Queue in the form of a List of
     * map class objects.
     */
    public synchronized List<Map<String, Object>> getTimedEvents()
    {
        List<Map<String, Object>> timedEventList =
            new ArrayList<Map<String, Object>>();   
        
        // Create ascending iterator for fTimedEvents
        Iterator<STAXTimedEvent> iterator = fTimedEvents.iterator(); 
            
        while (iterator.hasNext())
        {                               
            STAXTimedEvent timedEvent = iterator.next();
                
            Map<String, Object> timedEventMap = new TreeMap<String, Object>();
            timedEventMap.put(
                "staf-map-class-name", STAX.sTimedEventMapClassName);
            
            // Convert notification time to a readable date-time format
            
            Date notificationTime = new Date(timedEvent.getNotificationTime());
            timedEventMap.put(
                "notificationTime", sDateTime.format(notificationTime));
                
            // Note: Using a map even though it only contains one field
            // because may add more fields in the future such as the
            // source of the timed Event (e.g. timer, block, process).
                
            timedEventList.add(timedEventMap); 
        }          
      
        return timedEventList;
    }
    
    /**
     * Get the debug state of the Timed Event Queue thread.
     * Note: This method is only used if debugging is enabled in STAXTimerAction
     */
    public String getInfo()
    {
        return "TimedEvent State=" + fDebugState;
    }

    /**
     * This is the run method for the Timed Event Queue thread.  It waits for
     * each timed event to expire and when it does, it notifies the listener
     * and then continues this process for the next timed event in the queue.
     */
    public void run()
    {
        while (true)
        {
            try
            {
                synchronized (this)
                {
                    if (fComplete) return;

                    if (fTimedEvents.size() == 0)
                    {
                        fDebugState = 1;
                        try
                        {
                            wait();
                            fDebugState = 2;
                            continue;
                        }
                        catch (InterruptedException e)
                        {
                            throw e;
                        }
                    }

                    STAXTimedEvent timedEvent = null;

                    timedEvent = fTimedEvents.first();

                    long timeout = timedEvent.getNotificationTime() -
                        System.currentTimeMillis();
                
                    if (timeout <= 0)
                    {
                        // Timeout has expired
                        fDebugState = 3;

                        timedEvent.getTimedEventListener().timedEventOccurred(
                            timedEvent);

                        fDebugState = 4;
                        fTimedEvents.remove(timedEvent);
                    }
                    else
                    {
                        // Wait for the timed event to expire
                        fDebugState = 5;
                        try
                        {
                            wait(timeout);
                            fDebugState = 6;
                        }
                        catch (InterruptedException e)
                        {
                            throw e;
                        }
                    }
                }
            } // End try block
            catch (Throwable t)
            {
                String message = "STAXTimedEventQueue.run(): fDebugState=" +
                    fDebugState;

                if (t != null)
                {
                    // Add the Java stack trace to the message

                    StringWriter sw = new StringWriter();
                    t.printStackTrace(new PrintWriter(sw));

                    if (t.getMessage() != null)
                        message += "\n" + t.getMessage() + "\n" + sw.toString();
                    else
                        message += "\n" + sw.toString();
                }

                // Log an error message in the STAX JVM log

                STAX.logToJVMLog("Error", message);
            }
        }
    }

    /**
     * This class is used to sort the conditions in the condition set for
     * timed events in the Timed Event Queue.
     */
    class TimedEventComparator implements Comparator<STAXTimedEvent>
    {
        public int compare(STAXTimedEvent t1, STAXTimedEvent t2)
        {
            if (t1.getNotificationTime() == t2.getNotificationTime())
            {
                if (t1.hashCode() == t2.hashCode()) return 0;
                else if (t1.hashCode() < t2.hashCode()) return -1;
            }
            else if (t1.getNotificationTime() < t2.getNotificationTime())
            {
                return -1;
            }

            return 1;
        }
    }

    boolean fComplete = false;
    TreeSet<STAXTimedEvent> fTimedEvents =
        new TreeSet<STAXTimedEvent>(new TimedEventComparator());
    int fDebugState = 0;
}
