/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nquisition.hlibrary.console;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Master
 */
public class HCommandManager
{
    private final HConsole console;
    private final List<HCommand> commands;
    
    public HCommandManager(HConsole console)
    {
        this.console = console;
        commands = new ArrayList<>();
        commands.add(new HCommandGW());
    }
    
    public int execute(String cmd)
    {
        List<String> parsed = new ArrayList<>();
        Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
        Matcher regexMatcher = regex.matcher(cmd);
        
        while (regexMatcher.find())
        {
            if (regexMatcher.group(1) != null)
            {
                parsed.add(regexMatcher.group(1));
            }
            else if (regexMatcher.group(2) != null)
            {
                parsed.add(regexMatcher.group(2));
            }
            else
            {
                parsed.add(regexMatcher.group());
            }
        }
        
        if(parsed.size() <= 0)
            return HCommand.INVALID_COMMAND;
        HCommand command = this.getCommand(parsed.get(0));
        if(command == null)
            return HCommand.INVALID_COMMAND;
        parsed.remove(0);
        return command.execute(console, parsed);
    }
    
    private HCommand getCommand(String cmd)
    {
        for(HCommand c : commands)
            if(c.hasAlias(cmd))
                return c;
        return null;
    }
}
