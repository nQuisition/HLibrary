/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nquisition.hlibrary.console;

import javafx.event.Event;
import javafx.event.EventType;

/**
 *
 * @author Master
 */
public class HConsoleEvent extends Event
{
    public static final EventType<HConsoleEvent> LINE_ADDED = new EventType<>("LINE_ADDED");

    private final String parameter;

    public HConsoleEvent(String parameter, EventType<HConsoleEvent> eventType)
    {
        super(eventType);
        this.parameter = parameter;
    }

    public String getParameter()
    {
        return parameter;
    }
}
