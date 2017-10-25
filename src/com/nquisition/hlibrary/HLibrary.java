/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nquisition.hlibrary;

import com.nquisition.hlibrary.model.Gallery;
import com.nquisition.hlibrary.model.Database;
import com.nquisition.hlibrary.model.DatabaseInterface;
import com.nquisition.hlibrary.model.GFolder;
import com.nquisition.hlibrary.model.GImage;
import com.nquisition.hlibrary.api.UIView;
import com.nquisition.hlibrary.console.HConsole;
import com.nquisition.hlibrary.console.HConsoleAppender;
import com.nquisition.hlibrary.console.IConsoleListener;
import com.nquisition.hlibrary.ui.DefaultUIManager;
import com.nquisition.hlibrary.ui.HConsoleStage;
import com.nquisition.hlibrary.ui.HConsoleViewer;
import com.nquisition.hlibrary.ui.SimilarityViewer;
import com.nquisition.hlibrary.ui.UIManager;

import javafx.application.Application;
import javafx.stage.Stage;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

import javafx.application.Platform;

import com.nquisition.util.FileUtils;
import com.nquisition.util.Properties;

import java.io.File;
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
    
    private DatabaseInterface dbInterface;
    private UIManager uiManager;
    private HConsole console;
    private List<String> params = null;
    
    static
    {
        String pid = ManagementFactory.getRuntimeMXBean().getName().replaceAll("@.*", "");
        ThreadContext.put("pid", pid);
    }
    
    @Override
    public void start(Stage primaryStage)
    {
        if(instance != null) {
            logger.fatal("An instance of the application is already running in this process!");
            Platform.exit();
            System.exit(-2);
        }
        instance = this;
        
        console = new HConsole();
        HConsoleAppender.setHConsole(console);
        
        logger.info("-------------------------");
        String cfgFolder = FileUtils.toProperFolderPath(System.getProperty("user.home")) + "HLibrary\\";
        if(!new File(cfgFolder).exists()) {
        	//TODO init config
        }
        int res = Properties.read(cfgFolder + "config.txt");
        //HLogger runslogger = new HLogger("hlibrary.runs", true, "log_runs.txt");
        if(res != 0) {
            logger.fatal("Error reading properties file!");
            pressEnterToContinue();
            Platform.exit();
            System.exit(-1);
        }
        logger.info("Properties file loaded; HLibrary starting");
        
        Parameters prms = getParameters();
        params = prms.getRaw();
        /*System.out.println(params.size());
        for(String p : params)
            System.out.println(p);*/
        
        dbInterface = new DatabaseInterface(null);
        
        //TODO putting dbinterface into uimanager before database is loaded - is it OK?
        Map<String, Object> uiParams = new HashMap<>();
        uiParams.put("dbInterface", dbInterface);
        uiParams.put("root", Properties.get("galroot"));
        uiManager = new DefaultUIManager();
        uiManager.constructDefaults(uiParams);
        
        if(!params.isEmpty() && params.get(0).equals("-v")) {
            this.startLocal();
        } else if(!params.isEmpty() && params.get(0).equals("-test")) {
        	this.startGlobal(cfgFolder);
        } else {
        	//TODO remove
            this.startGlobal();
        }
        //FIXME REMOVE!
        /*else if(params.size() > 0 && params.get(0).equals("-fix2")) {
        	db = new Database(true);
            
            String galroot = Properties.get("galroot");
            if(!galroot.endsWith("\\"))
            	galroot = galroot + "\\";
            String bucketRoot = galroot + "Buckets\\";
            String resRoot = galroot + "Sorted Results\\";
            File resFolder = new File(resRoot);
            if(!resFolder.exists())
            	resFolder.mkdir();
            File[] temp = new File(bucketRoot).listFiles();
            String folderName = null;
            for(File file : temp) {
            	if(file.getAbsolutePath().endsWith("bucket_4"))
            		continue;
            	if(file.listFiles().length > 0) {
            		db.addDirectory(file.getAbsolutePath(), 1);
            		folderName = file.getName();
            		break;
            	}
            }
            if(folderName == null)
            	return;
            final String fldrName = folderName;
            //db.addDirectory(bucketRoot, 2);
            db.sortFolders();
            db.sortImagesByCreated();
            
            Gallery gal = new Gallery(db);
            gal.addImages(db.getImages());
            GalleryViewer gw = new GalleryViewer(db);
            gw.setGallery(gal);
            HLibraryFixer2 hlf2 = new HLibraryFixer2(db);
            gw.setOnCloseRequest(event -> {
            	hlf2.process(resRoot, fldrName);
                logger.info("Closing application");
                Platform.exit();
            });
            gw.show();
            gw.addKeyEventHandler((key) -> {
            	if(null!=key.getCode()) switch (key.getCode()) {
                case Y:
                	gw.jump(10);
                	break;
                case T:
                	gw.jump(-10);
                default:
                	break;
            	}
            });
        }*/
        
        /*File file = new File("D:\\temp2\\");
        File[] list = file.listFiles();
        for(File f : list) {
        	if(!f.isDirectory())
        		continue;
        	String name = f.getName();
        	EXHInfo entry = EXHNameParser.parseName(name);
        	System.out.println(name);
        	System.out.println(entry.info("\t"));
        }
        
        try {
        	long time = System.currentTimeMillis();
        	Map<GImage, List<GImage>> map = db.computeSimilarityStrings();
        	logger.debug((System.currentTimeMillis()-time)/1000.0d);
        	SimilarityViewer sw = new SimilarityViewer(db, map);
            sw.show();
        } catch(IOException e) {
        	e.printStackTrace();
        }*/
        
        
        /*
        Database db = dbInterface.getActiveDatabase();
        List<GImage> fullList = db.getImages();
        int sampleSize = 500;
        
        final List<GImage> list = fullList.subList(0, sampleSize);
        
        long time = System.currentTimeMillis();
        for(GImage img : list) {
        	img.setSimilarityBytes(null);
        	try {
        		img.computeSimilarity();
        	} catch (IOException e) {
    			e.printStackTrace();
    		}
        }
        logger.debug((System.currentTimeMillis() - time)/1000.0d);
        time = System.currentTimeMillis();
        list.stream().parallel().forEach((img) -> {
        	img.setSimilarityBytes(null);
        	try {
				img.computeSimilarity();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        });
        logger.debug((System.currentTimeMillis() - time)/1000.0d);
        time = System.currentTimeMillis();
        ForkJoinPool forkJoinPool = new ForkJoinPool(2);

        try {
	        forkJoinPool.submit(() -> 
	        	list.stream().parallel().forEach((img) -> {
		        	img.setSimilarityBytes(null);
		        	try {
						img.computeSimilarity();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		        })
	        ).get();
        } catch(ExecutionException e) {
			e.printStackTrace();
		} catch(InterruptedException e) {
			
		}
        logger.debug((System.currentTimeMillis() - time)/1000.0d);
        */
    }
    
    public void startGlobal(String cfgFolder) {
    	logger.info("[TEST] Starting in global mode");
    	String dbFilePath = cfgFolder + Properties.get("dbjson");
    	if(!dbInterface.loadDatabase(dbFilePath, false)) {
            logger.fatal("Database file doesn't exist or is in invalid format");
            Platform.exit();
            System.exit(-1);
        }
        
        dbInterface.getFolders().stream()
        		.sorted((a,b) -> a.getFavPercentage(false)<b.getFavPercentage(false)?1:a.getFavPercentage(false)>b.getFavPercentage(false)?-1:0)
        		.forEach(a -> System.out.println((Math.round(a.getFavPercentage(false)*10000D)/100D) + "% :: " + a.getPath()));
        
        dbInterface.info();
        
        /*Database db1 = new Database(false);
        db1.setLocation(Properties.get("dbroot","dbname"));
        db1.loadDatabase();
        db1.info();*/

        List<GImage> images = dbInterface.getImages();
        List<String> tags = new ArrayList<>();
        for(GImage img : images)
        {
            List<String> tgs = img.getTags();
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
        
        UIView fviewer = uiManager.buildFromFactory("FolderViewer", true);
        logger.info("Initialization complete");
        fviewer.show();
    }
    
    public void startLocal() {
    	String src = params.get(1);
        
        logger.info("Parameter -v found; Starting in local mode. Next parameter is \"{}\"", src);
        
        if(src == null || src.equals("") || !src.startsWith("--src=")) {
            logger.fatal("Source parameter not specified! Exiting");
            pressEnterToContinue();
            Platform.exit();
            System.exit(-1);
        }
        
        if(src.endsWith("\""))
            src = src.substring(src.indexOf('=') + 2, src.length()-1);
        else
            src = src.substring(src.indexOf('=') + 1, src.length());
        if(src.equals("")) {
            logger.fatal("Illegal source parameter! Exiting");
            pressEnterToContinue();
            Platform.exit();
            System.exit(-1);
        }
        
        if(!src.endsWith(".hdb")) {
        	//TODO
            logger.info("Initializing with file \"{}\"", src);
            String folder = Utils.getFilePath(src);
            String fname = Utils.getFileName(src);
            if(folder == null || fname == null) {
                logger.fatal("Cannot get source file name or path! Exiting");
                pressEnterToContinue();
                Platform.exit();
                System.exit(-1);
            }
            File f = new File(folder + "db.hdb");
            
            if(!f.exists()) {
                logger.info("Initializing folder; Rotating images..");
                FileUtils.rotateImagesBasedOnEXIF(folder, true);
                logger.info("Success!");
                if(!dbInterface.createDatabase(folder + "db.hdb", true)) {
                    logger.fatal("Unable to create new file \"" + (folder + "db.hdb") + "\"");
                    pressEnterToContinue();
                    Platform.exit();
                    System.exit(-1);
                }
                if(!dbInterface.addDirectory(folder, 1)) {
                    logger.warn("Problems adding directory to database; Some directories may not have been added");
                }
                if(!dbInterface.saveDatabase()) {
                    logger.warn("Unable to save database; Any changes made most likely will be lost");
                }
            } else {
                if(!dbInterface.loadDatabase(folder + "db.hdb", true)) {
                    logger.fatal("Unable to load database; Exiting..");
                    pressEnterToContinue();
                    Platform.exit();
                    System.exit(-1);
                }
            }

            //TODO
            Gallery gal = new Gallery(dbInterface.getActiveDatabase());

            gal.addImages(dbInterface.getImages());

            Map<String, Object> galParams = new HashMap<>();
            galParams.put("gallery", gal);
            galParams.put("startFrom", fname);
            UIView gw = uiManager.buildFromFactory("GalleryViewer", galParams, true);
            logger.info("Initialization complete");
            gw.show();
        } else {
            logger.info("Initializing with database \"{}\"", src);
            if(!dbInterface.loadDatabase(src, true)) {
                logger.fatal("Database file doesn't exist or is in invalid format");
                Platform.exit();
                System.exit(-1);
            }
            
            //TODO almost the same as another if-code, reuse!

            //TODO
            Gallery gal = new Gallery(dbInterface.getActiveDatabase());

            gal.addImages(dbInterface.getImages());

            Map<String, Object> galParams = new HashMap<>();
            galParams.put("gallery", gal);
            UIView gw = uiManager.buildFromFactory("GalleryViewer", galParams, true);
            logger.info("Initialization complete");
            gw.show();
        }
    }
    
    //TODO merge with the other startGlobal method
    @Deprecated
    public void startGlobal() {
    	if(!dbInterface.loadDatabase(Properties.get("dbroot","dbname"), false)) {
            logger.fatal("Database file doesn't exist or is in invalid format");
            Platform.exit();
            System.exit(-1);
        }

        List<GImage> images = dbInterface.getImages();
        List<String> tags = new ArrayList<>();
        for(GImage img : images)
        {
            List<String> tgs = img.getTags();
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

        UIView fviewer = uiManager.buildFromFactory("FolderViewer", true);
        logger.info("Initialization complete");
        fviewer.show();
    }
    
    public void saveAndExit(boolean waitForEnter) {
    	if(!dbInterface.saveDatabase()) {
        	//TODO saving failed, prompt!
        }
        logger.info("Closing application");
        Platform.exit();
        if(waitForEnter)
        	pressEnterToContinue();
    }
    
    public static boolean criticalCloseRequested() {
    	instance.saveAndExit(false);
    	return true;
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
    
    public static UIManager getUIManager() {
    	return instance.uiManager;
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
    
    public static void main(String[] args)
    {
    	launch(args);
    }
}
