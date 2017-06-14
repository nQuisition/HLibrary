/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nquisition.hlibrary.console;

import com.nquisition.hlibrary.ui.GalleryViewer;
import com.nquisition.hlibrary.ui.HConsoleStage;
import java.util.*;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Master
 */
public class HConsole
{
    private final Logger logger = LogManager.getLogger(HConsole.class.getName());
    
    private final HCommandManager manager;
    
    private List<String> lines;
    private List<IConsoleListener> listeners;
    
    private HConsoleStage focus;
       
    public HConsole()
    {
        lines = new ArrayList<>();
        listeners = new ArrayList<>();
        manager = new HCommandManager(this);
    }
    
    public void addLine(String line)
    {
        lines.add(line);
        notifyLineAdded(line);
    }
    
    public List<String> getLines()
    {
        ArrayList<String> res = new ArrayList<>(lines);
        return res;
    }
    
    public void registerListener(IConsoleListener listener)
    {
        unregisterDeadListeners();
        //TODO check if it is already in the list
        listeners.add(listener);
    }
    
    public void unregisterDeadListeners()
    {
        //TODO is  it enough?
        Iterator i = listeners.iterator();
        while(i.hasNext())
        {
            IConsoleListener listener = (IConsoleListener)i.next();
            if(!listener.isAlive())
            {
                System.out.println("Unregistering " + listener);
                i.remove();
            }
        }
    }
    
    public void notifyLineAdded(String line)
    {
        HConsoleEvent ev = new HConsoleEvent(line, HConsoleEvent.LINE_ADDED);
        for(IConsoleListener v : listeners)
            v.handleConsoleEvent(ev);
    }
    
    public void changeFocus(HConsoleStage s, boolean inFocus)
    {
        HConsoleStage old = focus;
        if(inFocus)
            focus = s;
        else if(focus == s)
            focus = null;
        addLine("Focus changed : " + old + " -> " + focus);
    }
    
    public HConsoleStage getFocus()
    {
        return focus;
    }
    
    public void processCommand(String cmd)
    {
        logger.info("\"" + cmd + "\"");
        
        int res = manager.execute(cmd);
        logger.info("Command executed with return code " + res);
    }
}
