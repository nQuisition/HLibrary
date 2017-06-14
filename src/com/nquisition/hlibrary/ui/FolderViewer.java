/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nquisition.hlibrary.ui;

import com.nquisition.hlibrary.util.CreationTimeFileComparator;
import com.nquisition.hlibrary.model.GImageList;
import com.nquisition.hlibrary.model.Gallery;
import com.nquisition.hlibrary.model.GFolder;
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
import javafx.beans.binding.*;

import org.apache.commons.io.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Master
 */
public final class FolderViewer extends HConsoleStage
{
    private static final Logger logger_local = LogManager.getLogger(FolderViewer.class.getName()+".local");
    private static final Logger logger_global = LogManager.getLogger(FolderViewer.class.getName()+".global");
    
    private final Logger logger;
    
    private Database db;
    private String root;
    
    private ListView<String> list1/*, list2*/;
    private ObservableList<String> data1/*, data2*/;
    private TextField tagInput;
    
    private boolean groupLists = true;
    private boolean groupOrientations = false;
    private boolean forceDisplayWholeList = true;
    
    private static final String PREFIX = "[*!!!%%%!!!%%%!!!*]";
       
    public FolderViewer(Database d, String folder)
    {
        super();
        db = d;
        logger = db.isLocal()?logger_local:logger_global;
        
        root = folder;
        
        data1 = FXCollections.observableArrayList();
        //data2 = FXCollections.observableArrayList();
        
        list1 = new ListView<>(data1);
        //list2 = new ListView<String>(data2);
        list1.setPrefSize(300, 400);
        //list2.setPrefSize(300, 400);
        list1.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        //list2.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        list1.setEditable(false);
        //list2.setEditable(false);
        //list2.setItems(list1.getSelectionModel().getSelectedItems());
        Button button0 = new Button("Thumbs");
        button0.setOnAction((ActionEvent e) -> {
            viewAddThumbs();
        });
        Button button1 = new Button("Go");
        button1.setOnAction((ActionEvent e) -> {
            viewAddFolders();
        });
        //tagInput = new TextField("fav :DarkArtsKai");
        tagInput = new TextField();
        Button button2 = new Button("Go");
        button2.setOnAction((ActionEvent e) -> {
            viewTaggedGallery();
        });
        
        Button button3 = new Button("Linked");
        button3.setOnAction((ActionEvent e) -> {
            showLinked();
        });
        
        Button button4 = new Button("Local DBs");
        button4.setOnAction((ActionEvent e) -> {
            LocalDatabaseViewer v = new LocalDatabaseViewer(root);
            v.show();
        });
        HBox box1 = new HBox(list1, new VBox(button0, button1)/*, list2*/);
        HBox box2 = new HBox(tagInput, button2, button3, button4);
        
        VBox wrapper = new VBox(box1, box2);
        
        ArrayList<String> folders = this.getFolders();
        for(int i = 0; i < folders.size(); i++)
            data1.add(folders.get(i));
        
        
        list1.setCellFactory(list -> {
            // usual list cell:
            ListCell<String> cell = new ListCell<String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? "" : item);
                }
            };
            
            BooleanBinding invalid = Bindings.createBooleanBinding(
                    () -> checkFolderName(cell.getText()), cell.textProperty(), cell.itemProperty());

            invalid.addListener((obs, wasInvalid, isNowInvalid) -> {
                if (!wasInvalid && isNowInvalid) {
                    cell.setStyle("-fx-text-fill:red;");
                } else {
                    cell.setStyle("");
                }
            });

            return cell;
        });

        
        StackPane rootp = new StackPane();
        rootp.getChildren().add(wrapper);
        this.setScene(new Scene(rootp, 800, 600));
        
        //copyFavs();
    }
    
    private boolean checkFolderName(String txt)
    {
        if(txt == null)
            return false;
        return txt.startsWith(PREFIX);
    }
    
    public void viewAddFolders()
    {
        ArrayList<String> folders = new ArrayList<>();
        //for(String str : list2.getItems())
        for(String str : list1.getSelectionModel().getSelectedItems())
        {
            folders.add(str);
        }
        
        for(int i = 0; i < folders.size(); i++)
        {
            if(!folders.get(i).startsWith(PREFIX))
            {
                if(db.addDirectory(root + folders.get(i), 1) < 0)
                {
                    logger.warn("Failed to add directory/subdirectories to the database");
                }
            }
            else
            {
                folders.set(i, folders.get(i).substring(PREFIX.length()));
            }
        }
        db.checkVerticality(1.0, 1.0, true);

        Gallery gal = new Gallery(db);
        ArrayList<GImage> end = new ArrayList<>();
        ArrayList<GImage> start = new ArrayList<>();
        for(String f : folders)
        {
            ArrayList<GImage> imgs = new ArrayList<>();
            GFolder gfolder = db.getRootFolder(root + f + "\\");
            if(gfolder == null)
                continue;
            gfolder.getAllImages(imgs);
            for(GImage img : imgs)
            {
               if(img.hasTag("horizontal"))
                {
                    start.add(img);
                }
                else if(img.hasTag("vertical"))
                {
                    end.add(img);
                }
            }
        }
        for(GImage img : end)
            start.add(img);
        end = null;
        gal.addImages(start);

        GalleryViewer gw = new GalleryViewer(db);
        gw.setGallery(gal);
        gw.show();

        //TODO Reset list1, since there will be more folders with PREFIX
    }
    
    public void viewAddThumbs()
    {
        ArrayList<String> folders = new ArrayList<String>();
        //for(String str : list2.getItems())
        for(String str : list1.getSelectionModel().getSelectedItems())
        {
            folders.add(str);
        }
        
        for(int i = 0; i < folders.size(); i++)
        {
            if(!folders.get(i).startsWith(PREFIX))
            {
                if(db.addDirectory(root + folders.get(i), 1) < 0)
                {
                    logger.warn("Failed to add directory/subdirectories to the database");
                }
            }
            else
                folders.set(i, folders.get(i).substring(PREFIX.length()));
        }
        db.checkVerticality(1.0, 1.0, true);

        ArrayList<GFolder> start = new ArrayList<>();
        for(String f : folders)
        {
            GFolder gfolder = db.getRootFolder(root + f + "\\");
            if(gfolder != null)
                start.add(gfolder);
        }

        ThumbViewer tv = new ThumbViewer(db, start);
        tv.show();

        //TODO Reset list1, since there will be more folders with PREFIX
    }
    
    public void showLinked()
    {
        Gallery gal = new Gallery(db);
        ArrayList<GImage> list = new ArrayList<>();
        System.out.println(db.getImageLists().size());
        for(GImageList gil : db.getImageLists())
        {
            for(GImage img : gil.getImages())
            {
                list.add(img);
            }
        }
        
        gal.addImages(list);

        if(list.size() <= 0)
        {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setContentText("No image lists found!");
            alert.showAndWait();
        }
        else
        {
            GalleryViewer gw = new GalleryViewer(db);
            gw.setGallery(gal);
            gw.show();
        }
    }
    
    public void viewTaggedGallery()
    {
        ArrayList<String> allowed = new ArrayList<>(), restricted = new ArrayList<String>(), names = new ArrayList<String>();
        String[] arr = tagInput.getText().trim().split(" ");
        for(int i = 0; i < arr.length; i++)
        {
            if(arr[i] == null || arr[i].length()<=0)
                continue;
            if(arr[i].charAt(0) == '-')
                restricted.add(db.getTag(arr[i].substring(1)));
            else if(arr[i].charAt(0) == ':')
                names.add(arr[i].substring(1));
            else
                allowed.add(db.getTag(arr[i]));
        }
        
        Gallery gal = new Gallery(db);
        ArrayList<GImage> end = new ArrayList<>();
        ArrayList<GImage> start = new ArrayList<>();
        ArrayList<Integer> visitedLists = new ArrayList<>();
        for(GImage img : db.getImages())
        {
            if((!img.hasAllTags(allowed) || !img.hasNoTags(restricted)) && (names.size() <= 0 || !img.nameFolderContains(names)))
                continue;
            
            GImageList l = img.getList();
            if(l != null && groupLists)
            {
                if(visitedLists.contains(l.getID()))
                    continue;
                ArrayList<GImage> imgs = l.getImages();
                for(GImage imga : imgs)
                {
                    if(forceDisplayWholeList 
                            || !(!imga.hasAllTags(allowed) || !imga.hasNoTags(restricted)) && (names.size() <= 0 || !imga.nameFolderContains(names)))
                    {
                        if(groupOrientations || imga.hasTag("horizontal"))
                        {
                            start.add(imga);
                        }
                        else if(imga.hasTag("vertical"))
                        {
                            end.add(imga);
                        }
                    }
                }
                visitedLists.add(l.getID());
                continue;
            }
                
            
            if(img.hasTag("horizontal"))
            {
                start.add(img);
            }
            else if(img.hasTag("vertical"))
            {
                end.add(img);
            }
        }
        
        for(GImage img : end)
            start.add(img);
        end = null;
        
        gal.addImages(start);

        if(start.size() <= 0)
        {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setContentText("No images that match tags found!");
            alert.showAndWait();
        }
        else
        {
            GalleryViewer gw = new GalleryViewer(db);
            gw.setGallery(gal);
            gw.show();
        }
    }
    
    //TODO move
    public void copyFavs()
    {
        String location = "G:\\New Folder\\";
        int counter = 0;
        ArrayList<Process> procs = new ArrayList<>();
        for(GImage img : db.getImages())
        {
            if(!img.hasTag("fav"))
                continue;
            
            String prefix = getPrefix(counter);
            counter++;
            if(img.hasTag("vertical"))
                prefix = "末vert_" + prefix;
            
            File src = new File(img.getFullPath());
            File dest = new File(location + prefix + img.getName());
            try
            {
                FileUtils.copyFile(src, dest);
                
                if(img.hasTag("vertical"))
                {
                    /*while(procs.size() >= NUM_PROCESSES)
                    {
                        Iterator<Process> iter = procs.iterator();
                        while (iter.hasNext())
                        {
                            Process p = iter.next();
                            if(!p.isAlive())
                            {
                                p = null;
                                iter.remove();
                            }
                        }
                        Thread.sleep(10);
                    }

                    Process p = rotateImageEx(location, prefix + img.getName(), false);
                    if(p != null)
                        procs.add(p);*/
                }
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
            /*catch(InterruptedException e)
            {
                
            }*/
        }
    }
    //TODO move
    private String getPrefix(int c)
    {
        String res = "" + c + "_";
        if(c < 10)
            res = "00000" + res;
        else if(c < 100)
            res = "0000" + res;
        else if(c < 1000)
            res = "000" + res;
        else if(c < 10000)
            res = "00" + res;
        else if(c < 100000)
            res = "0" + res;
        return res;
    }
    
    public ArrayList<String> getFolders()
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
                if(db.folderAlreadyAdded(listOfFiles[i].getCanonicalPath() + "\\"))
                    res.add(PREFIX + listOfFiles[i].getName());
                else
                    res.add(listOfFiles[i].getName());
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        }
        
        return res;
    }
}