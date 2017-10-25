/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nquisition.hlibrary.ui;

import com.nquisition.hlibrary.util.CreationTimeFileComparator;
import com.nquisition.hlibrary.model.Gallery;
import com.nquisition.hlibrary.HLibrary;
import com.nquisition.hlibrary.api.UIView;
import com.nquisition.hlibrary.model.Database;
import com.nquisition.hlibrary.model.GImage;
import java.io.*;
import java.util.*;
import javafx.collections.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.event.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Master
 */
public class LocalDatabaseViewer extends HConsoleStage
{
    /*private static final Logger logger_local = LogManager.getLogger(LocalDatabaseViewer.class.getName()+".local");
    private static final Logger logger_global = LogManager.getLogger(LocalDatabaseViewer.class.getName()+".global");
    
    private final Logger logger;*/
    
    private Database db;
    private String root;
    
    private ListView<String> list1;
    private ObservableList<String> data1;
       
    //TODO needs onClose operation/cleanup
    public LocalDatabaseViewer(String folder)
    {
        super();
        db = new Database(true);
        
        root = folder;
        
        data1 = FXCollections.observableArrayList();
        //data2 = FXCollections.observableArrayList();
        
        list1 = new ListView<>(data1);
        list1.setPrefSize(600, 500);
        list1.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        list1.setEditable(false);
        
        Button button1 = new Button("Go");
        button1.setOnAction((ActionEvent e) -> { viewFolders(); });
        VBox box1 = new VBox(list1, button1);
        
        ArrayList<String> folders = this.getFolders();
        for(int i = 0; i < folders.size(); i++)
            data1.add(folders.get(i));
        
        StackPane rootp = new StackPane();
        rootp.getChildren().add(box1);
        this.setScene(new Scene(rootp, 800, 600));
        
        System.out.println("Init finished");
    }
    
    public void viewFolders()
    {
        ArrayList<String> folders = new ArrayList<>();
        //for(String str : list2.getItems())
        for(String str : list1.getSelectionModel().getSelectedItems())
        {
            folders.add(str);
        }
        
        for(int i = 0; i < folders.size(); i++)
        {
            int pos = folders.get(i).indexOf('-');
            if(pos >= 0)
                folders.set(i, folders.get(i).substring(pos + 2));
        }
        //db.checkVerticality(1.0, 1.0, true);

        Gallery gal = new Gallery(db);
        ArrayList<GImage> start = new ArrayList<>();
        for(String f : folders)
        {
            ArrayList<GImage> imgs = new ArrayList<>();
            db.getRootFolder(root + f + "\\").getAllImages(imgs);
            for(GImage img : imgs)
            {
                start.add(img);
            }
        }

        gal.addImages(start);

        Map<String, Object> galParams = new HashMap<>();
        galParams.put("gallery", gal);
        UIView gw = HLibrary.getUIManager().buildFromFactory("GalleryViewer", galParams, false);
        gw.show();
        
    }
    
    public final ArrayList<String> getFolders()
    {
        ArrayList<String> res = new ArrayList<>();
        File p = new File(root);
        if(!p.exists() || !p.isDirectory())
            return null;
        File[] listOfFiles = p.listFiles();
        /*Arrays.sort(listOfFiles, new Comparator<File>(){
            public int compare(File f1, File f2)
            {
                return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
            } });*/
        Arrays.sort(listOfFiles, new CreationTimeFileComparator());
        for(int i = 0; i < listOfFiles.length; i++)
        {
            if(!listOfFiles[i].isDirectory())
                continue;
            try
            {
                //TODO don't hardcode file name?
                File dbFile = new File(listOfFiles[i], "db.hdb");
                if(!dbFile.exists())
                    continue;
                String fl = dbFile.getCanonicalPath();
                String path = listOfFiles[i].getCanonicalPath();
                if(!path.endsWith("\\"))
                    path = path + "\\";
                db.loadDatabase(fl, false);
                
                System.out.println(db.getFolderByName(path).getRating() + " - " + listOfFiles[i].getName());
                //System.out.println(path + " :: " + db.getFolderByName(path));
                
                res.add(db.getFolderByName(path).getRating() + " - " + listOfFiles[i].getName());
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        }
        
        return res;
    }
}
