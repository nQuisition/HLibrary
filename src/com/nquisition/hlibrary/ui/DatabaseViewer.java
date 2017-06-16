/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nquisition.hlibrary.ui;

import com.nquisition.hlibrary.model.GFolder;
import com.nquisition.hlibrary.model.Database;
import com.nquisition.hlibrary.model.GImage;
import java.io.*;
import java.util.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.text.*;
import javafx.stage.*;
import javafx.event.*;

/**
 *
 * @author Master
 */
public class DatabaseViewer extends Stage
{
    private Database db;
    
    TreeView tree;
    TreeItem rootItem;
    //private final Node rootIcon = new ImageView(
    //    new Image(getClass().getResourceAsStream("folder_16.png")));
    
    
    
    public DatabaseViewer(Database d)
    {
        db = d;
        
        rootItem = new TreeItem ("Folders"/*, rootIcon*/);
        tree = new TreeView(rootItem);
        tree.setShowRoot(false);
        rootItem.setExpanded(true);
        
        for(GFolder f : db.getFolders())
        {
            addFolder(f, rootItem);
        }
        
        tree.setOnDragDetected(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent event) {
                System.out.println(((GFolder)((TreeItem)tree.getSelectionModel().getSelectedItem()).getValue()).getParent());
            }
        });
        
        StackPane root = new StackPane();
        root.getChildren().add(tree);
        this.setScene(new Scene(root, 500, 350));
    }
    
    public void addFolder(GFolder f, TreeItem parent)
    {
        TreeItem item = new TreeItem (f/*, rootIcon*/);
        item.setExpanded(false);
        parent.getChildren().add(item);
        for(GImage img : f.getImages())
            item.getChildren().add(new TreeItem(img));
        for(GFolder folder : f.getSubFolders())
            this.addFolder(folder, item);
    }
}
