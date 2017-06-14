/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nquisition.hlibrary.ui;

import com.nquisition.hlibrary.console.HConsole;
import com.nquisition.hlibrary.console.HConsoleEvent;
import com.nquisition.hlibrary.console.IConsoleListener;
import javafx.collections.*; 
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.*;

/**
 *
 * @author Master
 */
public class HConsoleViewer extends Stage implements IConsoleListener
{
    private HConsole console;
    private ListView<String> list;
    private ObservableList<String> lines;
    private TextField input;
    private boolean blocked;
    
    public HConsoleViewer(HConsole c, Stage parent)
    {
        this(parent);
        this.registerWithConsole(c);
    }
       
    public HConsoleViewer(Stage parent)
    {
        this.initOwner(parent);
        //this.initModality(Modality.APPLICATION_MODAL);
        
        lines = FXCollections.observableArrayList();
        
        list = new ListView<>(lines);
        list.setPrefSize(600,400);
        list.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        list.setEditable(false);
        
        input = new TextField();
        
        VBox box = new VBox(list, input);
        
        StackPane rootp = new StackPane();
        rootp.getChildren().add(box);
        
        Scene scene = new Scene(rootp, 620, 500);
        scene.addEventHandler(KeyEvent.KEY_PRESSED, (key) -> {
            if(null!=key.getCode()) switch (key.getCode()) {
                case ESCAPE:
                    //TODO does it work fine?
                    this.fireEvent(
                        new WindowEvent(
                            this,
                            WindowEvent.WINDOW_CLOSE_REQUEST
                        )
                    );
                    break;
                case ENTER:
                    if(console == null)
                        break;
                    String cmd = input.getText();
                    console.processCommand(cmd);
                    input.clear();
                    break;
            }
        });
        
        this.setScene(scene);
        
        this.setAlwaysOnTop(true);
        blocked = false;
    }

    @Override
    public final void registerWithConsole(HConsole c)
    {
        //TODO needed?
        if(console != null)
            return;
        
        console = c;
        console.registerListener(this);
        
        lines.addAll(console.getLines());
        
        this.addEventHandler(HConsoleEvent.LINE_ADDED, event ->
        {
            lines.add(event.getParameter());
        });
    }
    
    @Override
    public final boolean isAlive()
    {
        return this.isShowing();
    }
    
    @Override
    public final void handleConsoleEvent(HConsoleEvent ev)
    {
        this.fireEvent(ev);
    }
    
    public void block()
    {
        blocked = true;
    }
    
    public void unBlock()
    {
        blocked = false;
    }
    
    public boolean isBlocked()
    {
        return blocked;
    }
}
