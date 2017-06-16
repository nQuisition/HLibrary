/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nquisition.hlibrary.console;

import javafx.application.Platform;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.Serializable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Plugin(
    name = "HConsoleAppender",
    category = "Core",
    elementType = "appender",
    printObject = true)
public final class HConsoleAppender extends AbstractAppender
{
    private static HConsole console;

    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();


    protected HConsoleAppender(String name, Filter filter,
                               Layout<? extends Serializable> layout,
                               final boolean ignoreExceptions)
    {
        super(name, filter, layout, ignoreExceptions);
    }

    @Override
    public void append(LogEvent event)
    {
        readLock.lock();

        final String message = new String(getLayout().toByteArray(event));

        try
        {
            Platform.runLater(() ->
            {
                try
                {
                    if (console != null)
                    {
                        console.addLine(message);
                    }
                }
                catch (final Throwable t)
                {
                  System.out.println("Error while appending to HConsole: "
                      + t.getMessage());
                }
            });
        }
        catch (final IllegalStateException ex)
        {
          ex.printStackTrace();
        }
        finally
        {
          readLock.unlock();
        }
    }

    @PluginFactory
    public static HConsoleAppender createAppender(
        @PluginAttribute("name") String name,
        @PluginElement("Layout") Layout<? extends Serializable> layout,
        @PluginElement("Filter") final Filter filter) {
        if (name == null)
        {
            LOGGER.error("No name provided for HConsoleAppender");
            return null;
        }
        if (layout == null)
        {
            layout = PatternLayout.createDefaultLayout();
        }
        return new HConsoleAppender(name, filter, layout, true);
    }

    public static void setHConsole(HConsole c)
    {
        HConsoleAppender.console = c;
    }
}