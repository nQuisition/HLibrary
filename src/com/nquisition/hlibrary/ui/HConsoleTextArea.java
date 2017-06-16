/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nquisition.hlibrary.ui;

import com.nquisition.hlibrary.console.HConsole;
import com.nquisition.hlibrary.console.HConsoleEvent;
import com.nquisition.hlibrary.console.IConsoleListener;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Background;
import javafx.stage.Stage;

/**
 *
 * @author Master
 */
public class HConsoleTextArea extends TextArea implements IConsoleListener
{
    private Stage parent;
    private HConsole console;
    
    public HConsoleTextArea(Stage p)
    {
        super();
        parent = p;
        this.setEditable(false);
        this.setPrefRowCount(10);
        this.setPrefColumnCount(100);
        this.setWrapText(true);
        this.setPrefWidth(800);
        this.setMaxWidth(800);
        this.setBackground(Background.EMPTY);
        this.setText("");
    }
    
    @Override
    public void registerWithConsole(HConsole c)
    {
        //TODO needed?
        if(console != null)
            return;
        
        console = c;
        console.registerListener(this);
        
        for(String line : console.getLines())
            this.appendText(line + "\n");
        this.addEventHandler(HConsoleEvent.LINE_ADDED, event ->
        {
            this.appendText(event.getParameter() + "\n");
        });
    }

    @Override
    public boolean isAlive()
    {
        return parent.isShowing();
    }

    @Override
    public void handleConsoleEvent(HConsoleEvent ev)
    {
        this.fireEvent(ev);
    }
    
}
