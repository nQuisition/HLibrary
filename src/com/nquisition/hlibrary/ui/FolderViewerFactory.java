package com.nquisition.hlibrary.ui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.nquisition.hlibrary.HLibrary;
import com.nquisition.hlibrary.api.AbstractGUIFactory;
import com.nquisition.hlibrary.api.ExtendableGUIElement;
import com.nquisition.hlibrary.api.UIView;
import com.nquisition.hlibrary.fxutil.HFXFactory;
import com.nquisition.hlibrary.model.DatabaseInterface;
import com.nquisition.hlibrary.model.GFolder;
import com.nquisition.hlibrary.model.GImage;
import com.nquisition.hlibrary.model.GImageList;
import com.nquisition.hlibrary.model.Gallery;
import com.nquisition.hlibrary.util.CreationTimeFileComparator;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

public class FolderViewerFactory extends AbstractGUIFactory {
	private static final Logger logger_local = LogManager.getLogger(FolderViewer.class.getName()+".local");
    private static final Logger logger_global = LogManager.getLogger(FolderViewer.class.getName()+".global");
    
    //FIXME make constructor and pass those properties in it?
	@Override
	public FolderViewer build() {
		DatabaseInterface databaseInterface = (DatabaseInterface)this.getProperty("dbInterface");
		String root = this.getPropString("root");
		FolderViewer fw = new FolderViewer(databaseInterface, root);
		for(Entry<String, List<Node>> entry : this.getElementSet()) {
			System.out.println("Adding to " + entry.getKey());
			fw.addElements(entry.getKey(), entry.getValue().toArray(new Node[entry.getValue().size()]));
		}
		fw.constructGUI();
		return fw;
	}

	private static final class FolderViewer extends UIView
	{
		private static final String[] AVAILABLE_POSITIONS = new String[] {"top", "bottom"};
	    private final Logger logger;
	    
	    private DatabaseInterface dbInterface;
	    private String root;
	    
	    private ListView<String> list1;
	    private TextField tagInput;
	    
	    private List<Button> topButtons = new ArrayList<>();
	    private List<Button> bottomButtons = new ArrayList<>();
	    
	    private boolean groupLists = true;
	    private boolean groupOrientations = false;
	    private boolean forceDisplayWholeList = true;
	    
	    private static final String PREFIX = "[*!!!%%%!!!%%%!!!*]";
	       
	    public FolderViewer(DatabaseInterface dbInterface, String root)
	    {
	        super();
	        this.root = root;
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
			ObservableList<String> data1 = FXCollections.observableArrayList();
	        
	        /*----------------------------------------------------------
	         * Viewing/adding folders
	         *----------------------------------------------------------*/
	        list1 = new ListView<>(data1);
	        list1.setPrefSize(300, 400);
	        list1.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
	        list1.setEditable(false);
	        
	        Button thumbsButton = new Button("Thumbs");
	        thumbsButton.setOnAction( e -> viewAddThumbs() );
	        Button viewAddButton = new Button("Go");
	        viewAddButton.setOnAction( e -> viewAddFolders() );
	        
	        thumbsButton.setMaxWidth(Double.MAX_VALUE);
	        viewAddButton.setMaxWidth(Double.MAX_VALUE);
	        VBox topButtonsBox = new VBox();
	        topButtonsBox.setSpacing(5);
	        topButtonsBox.setPadding(new Insets(40, 10, 0, 10));
	        topButtonsBox.getChildren().addAll(thumbsButton, viewAddButton);
	        topButtonsBox.getChildren().addAll(topButtons);
	        HBox viewAddBox = new HBox();
	        viewAddBox.setPadding(new Insets(10, 10, 10, 10));
	        viewAddBox.getChildren().addAll(list1, topButtonsBox);
	        
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
	            LocalDatabaseViewer v = new LocalDatabaseViewer(root);
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
		}

		@Override
		public Object[] getData(String pos) {
			if(pos.equalsIgnoreCase("top"))
				return list1.getSelectionModel().getSelectedItems().toArray();
			if(pos.equalsIgnoreCase("bottom"))
				return tagInput.getText().trim().split(" ");
			return new Object[0];
		}

		@Override
		public String[] getAvailablePositions() {
			return AVAILABLE_POSITIONS;
		}
	    
	    private static boolean checkFolderName(String txt)
	    {
	        if(txt == null)
	            return false;
	        return txt.startsWith(PREFIX);
	    }
	    
	    public void viewAddFolders()
	    {
	        ArrayList<String> folders = new ArrayList<>();
	        for(String str : list1.getSelectionModel().getSelectedItems())
	        {
	            folders.add(str);
	        }
	        
	        for(int i = 0; i < folders.size(); i++)
	        {
	            if(!folders.get(i).startsWith(PREFIX))
	            {
	                if(!dbInterface.addDirectory(root + folders.get(i), 1))
	                {
	                    logger.warn("Failed to add directory/subdirectories to the database");
	                }
	            }
	            else
	            {
	                folders.set(i, folders.get(i).substring(PREFIX.length()));
	            }
	        }
	        dbInterface.checkVerticality(1.0, 1.0, true);

	        //TODO
	        Gallery gal = new Gallery(dbInterface.getActiveDatabase());
	        ArrayList<GImage> end = new ArrayList<>();
	        ArrayList<GImage> start = new ArrayList<>();
	        for(String f : folders)
	        {
	            ArrayList<GImage> imgs = new ArrayList<>();
	            //TODO
	            GFolder gfolder = dbInterface.getActiveDatabase().getRootFolder(root + f + "\\");
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

	        Map<String, Object> galParams = new HashMap<>();
            galParams.put("gallery", gal);
            UIView gw = HLibrary.getUIManager().buildFromFactory("GalleryViewer", galParams, false);
	        gw.show();

	        //TODO Reset list1, since there will be more folders with PREFIX
	    }
	    
	    public void viewAddThumbs()
	    {
	        ArrayList<String> folders = new ArrayList<String>();
	        for(String str : list1.getSelectionModel().getSelectedItems())
	        {
	            folders.add(str);
	        }
	        
	        for(int i = 0; i < folders.size(); i++)
	        {
	            if(!folders.get(i).startsWith(PREFIX))
	            {
	                if(!dbInterface.addDirectory(root + folders.get(i), 1))
	                {
	                    logger.warn("Failed to add directory/subdirectories to the database");
	                }
	            }
	            else
	                folders.set(i, folders.get(i).substring(PREFIX.length()));
	        }
	        dbInterface.checkVerticality(1.0, 1.0, true);

	        ArrayList<GFolder> start = new ArrayList<>();
	        for(String f : folders)
	        {
	        	//TODO
	            GFolder gfolder = dbInterface.getActiveDatabase().getRootFolder(root + f + "\\");
	            if(gfolder != null)
	                start.add(gfolder);
	        }

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
	    
	    public void viewTaggedGallery()
	    {
	        ArrayList<String> allowed = new ArrayList<>(), restricted = new ArrayList<String>(), names = new ArrayList<String>();
	        String[] arr = tagInput.getText().trim().split(" ");
	        for(int i = 0; i < arr.length; i++)
	        {
	            if(arr[i] == null || arr[i].length()<=0)
	                continue;
	            if(arr[i].charAt(0) == '-')
	            	//TODO
	                restricted.add(dbInterface.getActiveDatabase().getTag(arr[i].substring(1)));
	            else if(arr[i].charAt(0) == ':')
	                names.add(arr[i].substring(1));
	            else
	            	//TODO
	                allowed.add(dbInterface.getActiveDatabase().getTag(arr[i]));
	        }
	        
	        //TODO
	        Gallery gal = new Gallery(dbInterface.getActiveDatabase());
	        ArrayList<GImage> end = new ArrayList<>();
	        ArrayList<GImage> start = new ArrayList<>();
	        ArrayList<Integer> visitedLists = new ArrayList<>();
	        for(GImage img : dbInterface.getImages())
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
	        	Map<String, Object> galParams = new HashMap<>();
	            galParams.put("gallery", gal);
	            UIView gw = HLibrary.getUIManager().buildFromFactory("GalleryViewer", galParams, false);
	            gw.show();
	        }
	    }
	    
	    //TODO move
	    public void copyFavs()
	    {
	        String location = "G:\\New Folder\\";
	        int counter = 0;
	        ArrayList<Process> procs = new ArrayList<>();
	        for(GImage img : dbInterface.getImages())
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
	            	//TODO
	                if(dbInterface.getActiveDatabase().folderAlreadyAdded(listOfFiles[i].getCanonicalPath() + "\\"))
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
}
