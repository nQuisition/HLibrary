package com.nquisition.hlibrary.ui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.nquisition.hlibrary.HLibrary;
import com.nquisition.hlibrary.api.ProgressMonitor;
import com.nquisition.hlibrary.api.UIView;
import com.nquisition.hlibrary.fxutil.HFXFactory;
import com.nquisition.hlibrary.model.Database;
import com.nquisition.hlibrary.model.DatabaseInterface;
import com.nquisition.hlibrary.model.GFolder;
import com.nquisition.hlibrary.model.GImage;
import com.nquisition.hlibrary.model.GImageList;
import com.nquisition.hlibrary.model.Gallery;
import com.nquisition.hlibrary.util.CreationTimeFileComparator;
import com.nquisition.util.FileUtils;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

final class FolderViewer extends UIView {
	private static final Logger logger_local = LogManager.getLogger(FolderViewer.class.getName()+".local");
    private static final Logger logger_global = LogManager.getLogger(FolderViewer.class.getName()+".global");
    
	private static final String[] AVAILABLE_POSITIONS = new String[] {"top", "bottom"};
    private final Logger logger;
    
    private DatabaseInterface dbInterface;
    private List<String> roots;
    
    private TreeView<FolderEntry> folderTree;
    private TextField tagInput;
    
    private List<Button> topButtons = new ArrayList<>();
    private List<Button> bottomButtons = new ArrayList<>();
    
    private boolean groupLists = true;
    private boolean groupOrientations = false;
    private boolean forceDisplayWholeList = true;
    
    private static class FolderEntry {
    	private String root;
    	private String path;
    	private GFolder folder;
    	private BooleanProperty hasLocalDB;
    	
    	public FolderEntry(String root, String path, GFolder folder, boolean hasLocalDB) {
    		this.root = root;
    		this.path = path;
    		this.folder = folder;
    		this.hasLocalDB = new SimpleBooleanProperty(hasLocalDB);
    	}
    	
    	public String getRoot() {
    		return root;
    	}
    	
    	public String getPath() {
    		return path;
    	}
    	
    	public GFolder getFolder() {
    		return folder;
    	}
    	
    	public void setFolder(GFolder folder) {
    		this.folder = folder;
    	}
    	
    	public boolean hasLocalDB() {
    		return hasLocalDB.get();
    	}
    	
    	public void setHasLocalDB(boolean b) {
    		hasLocalDB.set(b);
    	}
    	
    	public BooleanProperty hasLocalDBProperty() {
    		return hasLocalDB;
    	}
    	
    	public String toString() {
    		return path;
    	}
    }
       
    public FolderViewer(DatabaseInterface dbInterface, String root) {
        super();
        this.roots = new ArrayList<>();
        this.roots.add(root);
        this.dbInterface = dbInterface;
        //TODO change
        logger = dbInterface.getActiveDatabase().isLocal()?logger_local:logger_global;
    }
    
    @Override
	public void addElements(String pos, Node... elements) {
		if(!Arrays.asList(AVAILABLE_POSITIONS).contains(pos))
			return;
		for(Node node : elements) {
			//only allow buttons
			if(!(node instanceof Button))
				continue;
			if(pos.equalsIgnoreCase("top"))
				topButtons.add((Button)node);
			else if(pos.equalsIgnoreCase("bottom"))
				bottomButtons.add((Button)node);
		}
	}

	@Override
	public void constructGUI() {
		ObservableList<FolderEntry> data1 = FXCollections.observableArrayList();
        
        /*----------------------------------------------------------
         * Viewing/adding folders
         *----------------------------------------------------------*/
        createTreeView();
        folderTree.setPrefSize(300, 400);
        folderTree.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        folderTree.setEditable(false);
        
        Button thumbsButton = new Button("Thumbs");
        thumbsButton.setOnAction( e -> viewAddThumbs() );
        Button viewAddButton = new Button("Go");
        viewAddButton.setOnAction( e -> viewAddFolders() );
        Button createLocalButton = new Button("Create Local DBs");
        createLocalButton.setOnAction( e -> createLocalDBs() );
        Button viewLocalDatabaseButton = new Button("View Local");
        viewLocalDatabaseButton.setOnAction( e -> viewLocalDBs() );
        Button addRootButton = new Button("Add root folder...");
        addRootButton.setOnAction( e -> {
        	DirectoryChooser chooser = new DirectoryChooser();
        	chooser.setTitle("Folders");
        	File defaultDirectory = new File("D:\\");
        	chooser.setInitialDirectory(defaultDirectory);
        	File selectedDirectory = chooser.showDialog(this);
        	if(selectedDirectory != null)
        		this.addRoot(selectedDirectory.getAbsolutePath() + "\\");
        });
        Button similarityStringsButton = new Button("Sim Strings");
        similarityStringsButton.setOnAction( e -> computeSimilarityStrings() );
        Button partitionButton = new Button("Partition");
        partitionButton.setOnAction( e -> {
        	ExecutorService es = Executors.newSingleThreadExecutor();
        	FolderEntry folder = folderTree.getSelectionModel().getSelectedItem().getValue();
        	String dbPath = folder.root + folder.path + "\\db.hdb";
            es.submit(dbInterface.computeSimilarityStrings(dbPath));
            Future<Map<GImage, List<GImage>>> similars = es.submit(dbInterface.findPartitions(dbPath, 10000));
            es.submit(() -> {
            	try {
            		Map<GImage, List<GImage>> map = similars.get();
            		Platform.runLater(() -> {
            			SimilarityViewer sw = new SimilarityViewer(dbInterface.getActiveDatabase(), map);
            			sw.show();
            		});
            	} catch (Exception ex) {
    				ex.printStackTrace();
    			}
            });
        });
        
        thumbsButton.setMaxWidth(Double.MAX_VALUE);
        viewAddButton.setMaxWidth(Double.MAX_VALUE);
        VBox topButtonsBox = new VBox();
        topButtonsBox.setSpacing(5);
        topButtonsBox.setPadding(new Insets(40, 10, 0, 10));
        topButtonsBox.getChildren().addAll(thumbsButton, viewAddButton, createLocalButton, 
        		viewLocalDatabaseButton, addRootButton, similarityStringsButton, partitionButton);
        topButtonsBox.getChildren().addAll(topButtons);
        HBox viewAddBox = new HBox();
        viewAddBox.setPadding(new Insets(10, 10, 10, 10));
        viewAddBox.getChildren().addAll(folderTree, topButtonsBox);
        
        /*----------------------------------------------------------
         * Viewing gallery (tagged, linked, local DBs)
         *----------------------------------------------------------*/
        tagInput = new TextField();
        Button viewTaggedButton = HFXFactory.createUnboundedButton("Go");
        viewTaggedButton.setOnAction( e -> viewTaggedGallery() );
        
        Button viewLinkedButton = HFXFactory.createUnboundedButton("Linked");
        viewLinkedButton.setOnAction( e -> showLinked() );
        
        Button viewLocalButton = HFXFactory.createUnboundedButton("Local DBs");
        viewLocalButton.setOnAction( e -> {
            LocalDatabaseViewer v = new LocalDatabaseViewer(roots.get(0));
            v.show();
        });
        
        TilePane bottomButtonsBox = new TilePane(Orientation.HORIZONTAL);
        bottomButtonsBox.setPadding(new Insets(10, 0, 0, 0));
        bottomButtonsBox.setHgap(10.0);
        bottomButtonsBox.setVgap(8.0);
        bottomButtonsBox.getChildren().addAll(viewTaggedButton, viewLinkedButton, viewLocalButton);
        bottomButtonsBox.getChildren().addAll(bottomButtons);
        VBox viewGalleryBox = new VBox();
        viewGalleryBox.setPadding(new Insets(20, 10, 10, 10));
        viewGalleryBox.getChildren().addAll(tagInput, bottomButtonsBox);
        
        VBox wrapper = new VBox(viewAddBox, viewGalleryBox);
        
        folderTree.setCellFactory(treeView -> {
        	return new TreeCell<FolderEntry>() {
                @Override
                protected void updateItem(FolderEntry item, boolean empty) {
                    super.updateItem(item, empty);
                    if(item != null) {
                    	String prefix = "";
                    	if(item.hasLocalDB()) {
                    		String folderName = item.getRoot() + item.getPath();
                    		Database db = dbInterface.getDatabase(folderName + "\\db.hdb");
                    		GFolder folder = db.getFolderByPath(folderName + "\\");
                    		if(folder == null) {
                    			return;
                    		}
                    		if(folder.getRating() >= 0)
                    			prefix = "(" + folder.getRating() + ") ";
                    	}
                    	setText(empty ? "" : prefix + item.getPath());
                    	
                    	//TODO a lot of unnecessary updates?
                    	if(item.getFolder() != null) {
                    		setStyle("-fx-text-fill:red; -fx-font-weight:bold;");
                    	} else if(item.hasLocalDB()) {
                    		if(prefix.length() > 0)
                    			setStyle("-fx-text-fill:purple; -fx-font-weight:bold;");
                    		else
                    			setStyle("-fx-text-fill:blue; -fx-font-weight:bold;");
                    	} else {
                    		setStyle("");
                    	}
                    } else {
                    	setText("");
                    }
                }
            };
        });

        
        StackPane rootp = new StackPane();
        rootp.getChildren().add(wrapper);
        this.setScene(new Scene(rootp, 800, 600));
	}
	
	private void createTreeView() {
		TreeItem<FolderEntry> dummyRoot = new TreeItem<>();
		folderTree = new TreeView<>(dummyRoot);
		folderTree.setShowRoot(false);
		
		for(String root : roots) {
			addRootNode(root);
		}
	}
	
	private void addRoot(String root) {
		roots.add(root);
		addRootNode(root);
	}
	
	private void addRootNode(String root) {
		List<FolderEntry> folders = this.getFolders(root);
		TreeItem<FolderEntry> rootNode = new TreeItem<>(new FolderEntry("", root, null, false));
		rootNode.setExpanded(true);
		
		for(FolderEntry entry : folders) {
			rootNode.getChildren().add(new TreeItem<>(entry));
		}
		
		folderTree.getRoot().getChildren().add(rootNode);
	}

	@Override
	public Object[] getData(String pos) {
		if(pos.equalsIgnoreCase("top"))
			return folderTree.getSelectionModel().getSelectedItems().toArray();
		if(pos.equalsIgnoreCase("bottom"))
			return tagInput.getText().trim().split(" ");
		return new Object[0];
	}

	@Override
	public String[] getAvailablePositions() {
		return AVAILABLE_POSITIONS;
	}
    
    public void viewAddFolders() {
        List<FolderEntry> folders = new ArrayList<>();
        for(TreeItem<FolderEntry> entry : folderTree.getSelectionModel().getSelectedItems()) {
            folders.add(entry.getValue());
        }
        
        for(int i = 0; i < folders.size(); i++) {
            if(folders.get(i).getFolder() == null 
            		&& !dbInterface.addDirectoryToActive(folders.get(i).getRoot() + folders.get(i).getPath(), 1)) {
                logger.warn("Failed to add directory/subdirectories to the database");
            }
        }
        dbInterface.checkActiveVerticality(1.0, 1.0, true);

        //TODO
        Gallery gal = new Gallery(dbInterface.getActiveDatabase());
        ArrayList<GImage> end = new ArrayList<>();
        ArrayList<GImage> start = new ArrayList<>();
        for(FolderEntry f : folders)
        {
            ArrayList<GImage> imgs = new ArrayList<>();
            //TODO
            GFolder gfolder = dbInterface.getActiveDatabase().getRootFolderByPath(f.getRoot() + f.getPath() + "\\");
            if(gfolder == null)
                continue;
            f.setFolder(gfolder);
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
        gal.addImages(start);
        
        folderTree.refresh();

        Map<String, Object> galParams = new HashMap<>();
        galParams.put("gallery", gal);
        UIView gw = HLibrary.getUIManager().buildFromFactory("GalleryViewer", galParams, false);
        gw.show();

        //TODO Reset list1, since there will be more folders with PREFIX
    }
    
    public void createLocalDBs() {
    	//FIXME need different monitor without Timeline
    	ProgressMonitor monitor = HLibrary.requestProgressMonitor("Creating local DBs");
    	monitor.start(folderTree.getSelectionModel().getSelectedItems().size());
    	for(TreeItem<FolderEntry> treeItem : folderTree.getSelectionModel().getSelectedItems()) {
    		FolderEntry entry = treeItem.getValue();
    		monitor.add(1);
    		String folder = entry.getRoot() + entry.getPath();
    		if(!folder.endsWith("\\"))
    			folder = folder + "\\";
    		File f = new File(folder + "db.hdb");
            
            if(!f.exists()) {
                logger.info("Initializing local DB in folder \"" + folder + "\"; Rotating images..");
                FileUtils.rotateImagesBasedOnEXIF(folder, true);
                logger.info("Success!");
                if(!dbInterface.createDatabase(folder + "db.hdb", true, false)) {
                    logger.fatal("Unable to create new file \"" + (folder + "db.hdb") + "\"");
                    continue;
                }
                if(!dbInterface.addDirectory(folder + "db.hdb", folder, 1)) {
                    logger.warn("Problems adding directory to database; Some directories may not have been added");
                }
                dbInterface.getDatabase(folder + "db.hdb").checkVerticality(1.0, 1.0, true);
                if(!dbInterface.saveDatabase(folder + "db.hdb")) {
                    logger.warn("Unable to save database; Any changes made most likely will be lost");
                } else {
                	entry.setHasLocalDB(true);
                }
            }
        }
    	folderTree.refresh();
    }
    
    public void viewLocalDBs() {
        List<FolderEntry> folders = new ArrayList<>();
        for(TreeItem<FolderEntry> treeItem : folderTree.getSelectionModel().getSelectedItems()) {
        	FolderEntry entry = treeItem.getValue();
        	if(entry.hasLocalDB())
        		folders.add(entry);
        }
        
        if(folders.isEmpty())
        	return;
        
        //TODO ugly
        FolderEntry folder = folders.get(0);
        String location = folder.getRoot() + folder.getPath() + "\\db.hdb";
        if(!dbInterface.loadDatabase(location, true, false)) {
        	logger.error("Failed to load database \"" + location + "\"");
        	return;
        }

        //TODO
        Gallery gal = new Gallery(dbInterface.getDatabase(location));
        List<GImage> end = new ArrayList<>();
        List<GImage> start = new ArrayList<>();
      //TODO
        List<GImage> imgs = dbInterface.getDatabase(location).getImages();
        System.out.println(imgs.size());
        
        for(GImage img : imgs) {
            if(img.hasTag("vertical")) {
                end.add(img);
            } else {
            	start.add(img);
            }
        }
            
        for(GImage img : end)
            start.add(img);
        gal.addImages(start);

        Map<String, Object> galParams = new HashMap<>();
        galParams.put("gallery", gal);
        UIView gw = HLibrary.getUIManager().buildFromFactory("GalleryViewer", galParams, false);
        gw.setOnCloseRequest(e -> dbInterface.saveDatabase(location));
        gw.show();

        //TODO Reset list1, since there will be more folders with PREFIX
    }
    
    public void computeSimilarityStrings() {
    	ExecutorService es = Executors.newSingleThreadExecutor();
        for(TreeItem<FolderEntry> entry : folderTree.getSelectionModel().getSelectedItems()) {
        	FolderEntry folder = entry.getValue();
        	if(folder.getFolder() != null)
        		es.submit(dbInterface.computeSimilarityStrings(folder.getFolder()));
        	else if(folder.hasLocalDB())
        		es.submit(dbInterface.computeSimilarityStrings(folder.getRoot() + folder.getPath() + "\\db.hdb"));
        }
    }
    
    public void viewAddThumbs() {
        List<FolderEntry> folders = new ArrayList<>();
        for(TreeItem<FolderEntry> entry : folderTree.getSelectionModel().getSelectedItems()) {
            folders.add(entry.getValue());
        }
        
        for(int i = 0; i < folders.size(); i++)
        {
        	if(folders.get(i).getFolder() == null 
            		&& !dbInterface.addDirectoryToActive(folders.get(i).getRoot() + folders.get(i).getPath(), 1)) {
                logger.warn("Failed to add directory/subdirectories to the database");
            }
        }
        dbInterface.checkActiveVerticality(1.0, 1.0, true);

        ArrayList<GFolder> start = new ArrayList<>();
        for(FolderEntry f : folders)
        {
        	//TODO
            GFolder gfolder = dbInterface.getActiveDatabase().getRootFolderByPath(f.getRoot() + f.getPath() + "\\");
            f.setFolder(gfolder);
            if(gfolder != null)
                start.add(gfolder);
        }

        folderTree.refresh();
        //TODO
        ThumbViewer tv = new ThumbViewer(dbInterface.getActiveDatabase(), start);
        tv.show();

        //TODO Reset list1, since there will be more folders with PREFIX
    }
    
    public void showLinked()
    {
    	//TODO
        Gallery gal = new Gallery(dbInterface.getActiveDatabase());
        ArrayList<GImage> list = new ArrayList<>();
        //TODO
        System.out.println(dbInterface.getActiveDatabase().getImageLists().size());
        //TODO
        for(GImageList gil : dbInterface.getActiveDatabase().getImageLists())
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
        	Map<String, Object> galParams = new HashMap<>();
            galParams.put("gallery", gal);
            UIView gw = HLibrary.getUIManager().buildFromFactory("GalleryViewer", galParams, false);
            gw.show();
        }
    }
    
    public void viewTaggedGallery() {
        HLibrary.getUIManager().getUIHelper().showImagesWithTags(tagInput.getText());
    }
    
  //TODO move
    public void copyFavs()
    {
        String location = "G:\\New Folder (3)\\";
        int counter = 0;
        ArrayList<Process> procs = new ArrayList<>();
        for(GImage img : dbInterface.getActiveImages())
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
            	org.apache.commons.io.FileUtils.copyFile(src, dest);
                
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
    
    private List<FolderEntry> getFolders(String root) {
        List<FolderEntry> res = new ArrayList<>();
        File p = new File(root);
        if(!p.exists() || !p.isDirectory())
            return new ArrayList<>();
        File[] listOfFiles = p.listFiles();
        Arrays.sort(listOfFiles, new CreationTimeFileComparator());
        for(int i = 0; i < listOfFiles.length; i++) {
            if(!listOfFiles[i].isDirectory())
                continue;

            String fileName = listOfFiles[i].getAbsolutePath() + "\\db.hdb";
            File check = new File(fileName);
            boolean exists  = check.exists();
            if(exists) {
            	dbInterface.loadDatabase(fileName, true, false);
            }
        	//TODO
            GFolder folder = dbInterface.getActiveDatabase().getFolderByPath(listOfFiles[i].getAbsolutePath() + "\\");
            res.add(new FolderEntry(root, listOfFiles[i].getName(), folder, exists));
        }
        
        return res;
    }
}