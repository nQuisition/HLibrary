/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nquisition.hlibrary;

import com.nquisition.hlibrary.ui.FolderViewer;
import com.nquisition.hlibrary.ui.DatabaseViewer;
import com.nquisition.hlibrary.ui.GalleryViewer;
import com.nquisition.hlibrary.model.Gallery;
import com.nquisition.hlibrary.model.Database;
import com.nquisition.hlibrary.model.GImage;
import com.nquisition.hlibrary.console.HConsole;
import com.nquisition.hlibrary.console.HConsoleAppender;
import com.nquisition.hlibrary.console.IConsoleListener;
import com.nquisition.hlibrary.ui.HConsoleStage;
import com.nquisition.hlibrary.ui.HConsoleViewer;
import javafx.application.Application;
import javafx.stage.Stage;
import java.util.*;
import javafx.application.Platform;
import com.nquisition.util.Properties;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

/**
 *
 * @author Master
 */
public class HLibrary extends Application
{
    private static final Logger logger = LogManager.getLogger(HLibrary.class);
    private static HLibrary instance;
    
    private Database db;
    private HConsole console;
    
    static
    {
        String pid = ManagementFactory.getRuntimeMXBean().getName().replaceAll("@.*", "");
        ThreadContext.put("pid", pid);
    }
    
    private GalleryViewer viewer;
    private FolderViewer fviewer;
    private DatabaseViewer manager;
    
    @Override
    public void start(Stage primaryStage)
    {
        if(instance != null)
        {
            logger.fatal("An instance of the application is already running in this process!");
            Platform.exit();
            System.exit(-2);
        }
        instance = this;
        
        console = new HConsole();
        HConsoleAppender.setHConsole(console);
        
        logger.info("-------------------------");
        int res = Properties.read("C:\\NetBeansProjects\\HLibrary\\config.txt");
        //HLogger runslogger = new HLogger("hlibrary.runs", true, "log_runs.txt");
        if(res != 0)
        {
            logger.fatal("Error reading properties file!");
            pressEnterToContinue();
            Platform.exit();
            System.exit(-1);
        }
        logger.info("Properties file loaded; HLibrary starting");
        
        Parameters prms = getParameters();
        List<String> params = prms.getRaw();
        /*System.out.println(params.size());
        for(String p : params)
            System.out.println(p);*/
        if(params.size() > 0 && params.get(0).equals("-v"))
        {
            //logger = new HLogger("hlibrary.local", true, Properties.get("dbroot") + "log_local.txt");
            String src = params.get(1);
            
            logger.info("Parameter -v found; Starting in local mode. Next parameter is \"{}\"", src);
            
            db = new Database(true);
            
            if(src == null || src.equals("") || !src.startsWith("--src="))
            {
                logger.fatal("Source parameter not specified! Exiting");
                pressEnterToContinue();
                Platform.exit();
                System.exit(-1);
            }
            
            if(src.endsWith("\""))
                src = src.substring(src.indexOf('=') + 2, src.length()-1);
            else
                src = src.substring(src.indexOf('=') + 1, src.length());
            if(src.equals(""))
            {
                logger.fatal("Illegal source parameter! Exiting");
                pressEnterToContinue();
                Platform.exit();
                System.exit(-1);
            }
            
            if(!src.endsWith(".hdb"))
            {
                logger.info("Initializing with file \"{}\"", src);
                String folder = Utils.getFilePath(src);
                String fname = Utils.getFileName(src);
                if(folder == null || fname == null)
                {
                    logger.fatal("Cannot get source file name or path! Exiting");
                    pressEnterToContinue();
                    Platform.exit();
                    System.exit(-1);
                }
                File f = new File(folder + "db.hdb");

                try
                {
                    db.setLocation(f.getCanonicalPath());
                }
                catch(IOException e)
                {
                    logger.fatal("Unable to resolve canonical path of \"" + f.getAbsolutePath() + "\"", e);
                    pressEnterToContinue();
                    Platform.exit();
                    System.exit(-1);
                }
                if(!f.exists())
                {
                    logger.info("Initializing folder; Rotating images..");
                    com.nquisition.util.FileUtils.rotateImagesBasedOnEXIF(folder, true);
                    logger.info("Success!");
                    try
                    {
                        f.createNewFile();
                    }
                    catch(IOException e)
                    {
                        logger.fatal("Unable to create new file \"" + f.getAbsolutePath() + "\"", e);
                        pressEnterToContinue();
                        Platform.exit();
                        System.exit(-1);
                    }
                    if(db.addDirectory(folder, 1) < 0)
                    {
                        logger.warn("Problems adding directory to database; Some directories may not have been added");
                    }
                    if(db.saveDatabase() < 0)
                    {
                        logger.warn("Unable to save database; Any changes made most likely will be lost");
                    }
                }
                else
                {
                    if(db.loadDatabase() < 0)
                    {
                        logger.fatal("Unable to load database; Exiting..");
                        pressEnterToContinue();
                        Platform.exit();
                        System.exit(-1);
                    }
                }

                Gallery gal = new Gallery(db);

                gal.addImages(db.getImages());

                GalleryViewer gw = new GalleryViewer(db);
                gw.setGallery(gal, fname);
                gw.setOnCloseRequest(event -> {
                    if(db.saveDatabase()<0)
                    {
                        logger.warn("Unable to save database; Any changes made will be lost");
                    }
                    logger.info("Closing application");
                    Platform.exit();
                    //pressEnterToContinue();
                });

                logger.info("Initialization complete");
                gw.show();
            }
            else
            {
                logger.info("Initializing with database \"{}\"", src);
                db.setLocation(src);
                //TODO there are better actions in case it fails to load?
                if(db.loadDatabase() < 0)
                {
                    logger.fatal("Unable to load database; Exiting..");
                    pressEnterToContinue();
                    Platform.exit();
                    System.exit(-1);
                }
                //TODO almost the same as another if-code, reuse!

                Gallery gal = new Gallery(db);

                gal.addImages(db.getImages());

                GalleryViewer gw = new GalleryViewer(db);
                gw.setGallery(gal);
                gw.setOnCloseRequest(event -> {
                    if(db.saveDatabase()<0)
                    {
                        logger.warn("Unable to save database; Any changes made will be lost");
                    }
                    logger.info("Closing application");
                    Platform.exit();
                    //pressEnterToContinue();
                });

                logger.info("Initialization complete");
                gw.show();
            }
        }
        else
        {
            //logger = new HLogger("hlibrary.main", true, Properties.get("dbroot") + "log_main.txt");

            logger.info("Starting in global mode");
            db = new Database(false);

            db.setLocation(Properties.get("dbroot","dbname"));

            //TODO deffinitely there are better actions if it fails
            if(db.loadDatabase() < 0)
            {
                logger.fatal("Unable to load database; Exiting..");
                Platform.exit();
                System.exit(-1);
            }
            /*System.out.println("Starting purge..");
            db.purgeDatabase();
            System.out.println("Done!");*/
            //db.dropOrientationTags();
            //db.checkVerticality(0.8, 1.2);
            //db.addDirectory("D:\\temp1\\Новая папка", 2);
            //db.purgeDatabase();
            //System.out.println(db.getNumImages());
            //db.addDirectory("D:\\temp1\\Новая папка", 1);
            //db.checkVerticality(0.8, 1.2);
            //System.out.println(db.getNumImages());
            //db.rotateVertical(0.8, 20);

            /*Gallery gal = new Gallery();
            ArrayList<GImage> imgs = new ArrayList<GImage>();
            for(GImage img : db.getImages())
            {
                if(img.hasTag("horizontal"))
                    imgs.add(img);
            }
            for(GImage img : db.getImages())
            {
                if(img.hasTag("vertical"))
                    imgs.add(img);
            }
            gal.addImages(imgs);
            System.out.println(gal.getSize());*/

            //db.computeSimilarityStrings();

            db.sortFolders();
            //db.checkFiles();

            //db.findArtists();

            ArrayList<GImage> images = db.getImages();
            ArrayList<String> tags = new ArrayList<>();
            for(GImage img : images)
            {
                ArrayList<String> tgs = img.getTags();
                for(String t : tgs)
                {
                    if(!tags.contains(t))
                        tags.add(t);
                }
            }
            Collections.sort(tags);
            logger.info("{} tags found", tags.size());
            String alltags = "";
            for(String t : tags)
                alltags += t.toUpperCase() + " ";
            logger.debug(alltags);

            //db.computeSimilarityStrings();

            fviewer = new FolderViewer(db, Properties.get("galroot"));
            fviewer.setOnCloseRequest(event -> {
                if(db.saveDatabase() < 0)
                {
                    logger.warn("Saving database while closing failed!");
                }
                logger.info("Closing application");
                Platform.exit();
            });
            logger.info("Initialization complete");
            fviewer.show();


            //manager = new DatabaseViewer(db);
            //manager.show();

            //Utils.moveFiles("D:\\temp1\\Новая папка", "D:\\temp1\\Новая папка\\Discarded", "Копия");
            /*Path directory = Paths.get("D:\\temp1\\Новая папка\\Discarded");
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
           public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                   Files.delete(file);
                   return FileVisitResult.CONTINUE;
           }

           @Override
           public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                   Files.delete(dir);
                   return FileVisitResult.CONTINUE;
           }

            });*/
        }
    }
    
    private Database getDatabase()
    {
        return db;
    }
    
    public static void changeConsoleFocus(HConsoleStage stage, boolean inFocus)
    {
        instance.console.changeFocus(stage, inFocus);
    }
    
    public static void registerListenerWithConsole(IConsoleListener l)
    {
        l.registerWithConsole(instance.console);
    }
    
    public static void showConsole(HConsoleStage parent)
    {
        HConsoleViewer v = new HConsoleViewer(instance.console, parent);
        v.show();
    }
    
    private static void pressEnterToContinue()
    { 
        System.out.println();
        System.out.println("Press enter to continue...");
        try
        {
            System.in.read();
        } 
        catch(Exception e)
        {}  
    }
}
