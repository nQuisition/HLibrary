/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nquisition.hlibrary.console;

/**
 *
 * @author Master
 */
public interface IConsoleListener
{
    public void registerWithConsole(HConsole console);
    public boolean isAlive();
    public void handleConsoleEvent(HConsoleEvent ev);
}
