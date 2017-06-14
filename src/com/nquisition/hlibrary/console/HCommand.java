/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nquisition.hlibrary.console;

import java.util.List;

/**
 *
 * @author Master
 */
public abstract class HCommand
{
    public static final int STATUS_OK = 0;
    public static final int INVALID_COMMAND = -1;
    public static final int INVALID_PARAMETER = -2;
    public static final int INVALID_TARGET = -3;
    
    private String name;
    private String[] aliases;
    
    protected HCommand(String n, String... a)
    {
        name = n;
        aliases = new String[a.length];
        System.arraycopy(a, 0, aliases, 0, a.length);
    }
    
    public boolean hasAlias(String alias)
    {
        if(alias.equalsIgnoreCase(name))
            return true;
        for(String s : aliases)
            if(s.equalsIgnoreCase(name))
                return true;
        return false;
    }
    
    public abstract int execute(HConsole console, List<String> params);
}
