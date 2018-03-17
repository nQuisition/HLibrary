package com.nquisition.hlibrary.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.nquisition.fxutil.ColorMap;
import com.nquisition.fxutil.FXFactory;
import com.nquisition.fxutil.MultiColoredText;
import com.nquisition.fxutil.RatingBar;
import com.nquisition.hlibrary.HLibrary;
import com.nquisition.hlibrary.api.UIView;
import com.nquisition.hlibrary.fxutil.HFXFactory;
import com.nquisition.hlibrary.fxutil.HStyleSheet;
import com.nquisition.hlibrary.model.DatabaseInterface;
import com.nquisition.hlibrary.model.GImage;
import com.nquisition.hlibrary.model.Gallery;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

final class GalleryViewer extends UIView
{
	private static final Logger logger_local = LogManager.getLogger(GalleryViewer.class.getName()+".local");
    private static final Logger logger_global = LogManager.getLogger(GalleryViewer.class.getName()+".global");
    
	private final Logger logger;
    
    private final GalleryViewer instance = this;
    
    //TODO move to UI control
    private HStyleSheet styleSheet = new HStyleSheet();
    
    private Gallery gal = null;
    private ImageView imv;
    private Text linkNum;
    
    private HConsoleTextArea consoleTextArea;
    
    private TaggingOverlay taggingOverlay;
    private InfoOverlay infoOverlay;
    private SideMenu sideMenu;
    private ThumbViewer thumbViewer;
    
    private VBox linkNumBox;
    private Rectangle moveIndicator, linkIndicator, folderChangeIndicator;
    private Scene scene;
    private int width = 1920;
    private int height = 1080;
    private double minzoom = 1.0;
    private double maxzoom = 3.0;
    private double zoomstep = 0.25;
    private double curzoom = 1.0;
    private int movestep = 50;
    private double trX = 0;
    private double trY = 0;
    private boolean dragging = false;
    private double dragStartX = 0.0, dragStartY = 0.0;
    
    private GImage selected = null;
    private GImage previmg = null, curimg = null;
    private ArrayList<GImage> linkChain = null;
    
    private BooleanProperty tagging = new SimpleBooleanProperty(false);
    private BooleanProperty forceRotate = new SimpleBooleanProperty(false);
    private BooleanProperty limitToFav = new SimpleBooleanProperty(false);
    
    private IntegerProperty curRating = new SimpleIntegerProperty(0);
    
    private IntegerProperty curImage = new SimpleIntegerProperty(-1);
    
    private DatabaseInterface dbInterface;
    
    private long throttling = 20;
    private long lastload = 0;
    
    private ChangeListener<Number> curRatingChanged;
    
    public GalleryViewer(DatabaseInterface databaseInterface) {
    	super();
    	this.dbInterface = databaseInterface;
    	//TODO change
    	logger = dbInterface.getActiveDatabase().isLocal()?logger_local:logger_global;
    }
    
    @Override
	public void addElements(String pos, Node... elements) {
    	
	}
    
    @Override
	public void constructGUI() {
        this.setFullScreen(true);
        
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: black");
        
        scene = new Scene(root, width, height, Color.BLACK);
        
        imv = new ImageView();
        imv.setFitHeight(scene.getHeight());
        imv.setFitWidth(scene.getWidth());
        imv.setPreserveRatio(true);
        
        moveIndicator = new Rectangle(0, 0, width, height);
        moveIndicator.setFill(Color.TRANSPARENT);
        moveIndicator.setStroke(Color.DARKGREEN);
        moveIndicator.setStrokeWidth(15);
        moveIndicator.setVisible(false);
        
        folderChangeIndicator = new Rectangle(0, 0, width, height);
        folderChangeIndicator.setFill(Color.TRANSPARENT);
        folderChangeIndicator.setStroke(Color.GREENYELLOW);
        folderChangeIndicator.setOpacity(0.0f);
        folderChangeIndicator.setStrokeWidth(150);
        folderChangeIndicator.setVisible(true);
        
        linkIndicator = new Rectangle(0, 0, width, height);
        linkIndicator.setFill(Color.TRANSPARENT);
        linkIndicator.setStroke(Color.CYAN);
        linkIndicator.setStrokeWidth(15);
        linkIndicator.setVisible(false);
        
        linkNum = new Text("");
        linkNum.setFont(Font.font("Arial", FontWeight.BOLD, 38));
        linkNum.setFill(Color.YELLOW);
        
        linkNumBox = new VBox();
        linkNumBox.getChildren().addAll(linkNum);
        linkNumBox.setMaxSize(50, 50);
        StackPane.setAlignment(linkNumBox, Pos.TOP_RIGHT);
        linkNumBox.setBackground(new Background(new BackgroundFill(Color.RED, CornerRadii.EMPTY, Insets.EMPTY)));
        linkNumBox.setVisible(false);
        
        infoOverlay = new InfoOverlay();
        infoOverlay.setCommentVisible(false);
        infoOverlay.setInfoVisible(true);
        infoOverlay.setTagsVisible(true);
        taggingOverlay = new TaggingOverlay();
        taggingOverlay.setVisible(false);
        sideMenu = new SideMenu();
        sideMenu.setVisible(false);
        thumbViewer = new ThumbViewer();
        thumbViewer.setVisible(false);
        
        //TODO never added to the scene
        consoleTextArea = new HConsoleTextArea(this);
        consoleTextArea.setVisible(false);
        HLibrary.registerListenerWithConsole(consoleTextArea);
        
        root.getChildren().addAll(imv, moveIndicator, linkIndicator, folderChangeIndicator, 
        		taggingOverlay.getPane(), infoOverlay.getPane(), linkNumBox, sideMenu.getPane(), 
        		thumbViewer.getPane());

        this.initEventHandlers();
        
        this.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        this.setTitle("HGallery");
        this.setScene(scene);
        //this.show();
    }
    
    private void initEventHandlers()
    {
        taggingOverlay.initEventHandlers();
        infoOverlay.initEventHandlers();
        sideMenu.initEventHandlers();
        thumbViewer.initEventHandlers();
        
        tagging.addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
        {
        	setTagging(newValue);
        });
        forceRotate.addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
        {
        	resetImage(false, false);
        });
        
        curRatingChanged = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
        {
        	gal.rateCurrentFolder(newValue.intValue());
        };
        curRating.addListener(curRatingChanged);
        
        scene.addEventHandler(KeyEvent.KEY_PRESSED, (key) -> {
            if(null!=key.getCode()) switch (key.getCode()) {
                case RIGHT:
                    nextImage();
                    break;
                case LEFT:
                    prevImage();
                    break;
                /*case DELETE:
                    removeImage();
                    break;*/
                case E:
                    if(key.isControlDown())
                    {
                        tagging.set(true);
                        key.consume();
                    }
                    break;
                case F:
                    limitToFav.set(!limitToFav.get());
                    break;
                case R:
                    forceRotate.set(!forceRotate.get());
                    break;
                case Q:
                    invertOrientationTag();
                    break;
                case Z:
                    rotateImage(true);
                    break;
                case X:
                    rotateImage(false);
                    break;
                case C:
                    infoOverlay.toggleCommentVisible();
                    break;
                case W:
                    this.move(0, -1);
                    break;
                case S:
                    this.move(0, 1);
                    break;
                case A:
                    this.move(-1, 0);
                    break;
                case D:
                    this.move(1, 0);
                    break;
                case M:
                    selectImage();
                    break;
                case N:
                    moveSelected();
                    break;
                case B:
                    linkSelected();
                    break;
                case L:
                    if(!key.isControlDown())
                        chainLink();
                    else
                        finishChainLink();
                    break;
                case O:
                    try
                    {
                        Runtime.getRuntime().exec("explorer.exe /select," + gal.getCurrentNameFull());
                    }
                    catch(IOException ex)
                    {
                        logger.warn("Cannot execute explorer.exe at \"" + gal.getCurrentNameFull() + "\"", ex);
                    }
                    break;
                case SPACE:
                    if(key.isControlDown())
                        jumpOrientation();
                    else
                        jumpFolder(true);
                    break;
                case BACK_SPACE:
                    jumpFolder(false);
                    break;
                case UP:
                    favCurrent();
                    break;
                case DOWN:
                    lowCurrent();
                    nextImage();
                    break;
                case DIGIT0:
                    curRating.set(0);
                    break;
                case DIGIT1:
                	curRating.set(1);
                    break;
                case DIGIT2:
                	curRating.set(2);
                    break;
                case DIGIT3:
                	curRating.set(3);
                    break;
                case DIGIT4:
                	curRating.set(4);
                    break;
                case DIGIT5:
                	curRating.set(5);
                    break;
                case ESCAPE:
                    if(selected != null || linkChain != null)
                        clearSelection();
                    else
                    {
                        this.fireEvent(new WindowEvent(
                                this,
                                WindowEvent.WINDOW_CLOSE_REQUEST));
                        //this.close();
                    }
                    break;
                case BACK_QUOTE:
                    HLibrary.showConsole(this);
                    break;
                default:
                    break;
            }
        });
        
        scene.addEventHandler(MouseEvent.MOUSE_CLICKED, (mevent) -> {
            if(null != mevent.getButton() && !infoOverlay.isCommentAreaActive())
            switch (mevent.getButton())
            {
                case PRIMARY:
                    if(dragging)
                        dragging = false;
                    else
                    {
                        if(mevent.getX() < width/2)
                            prevImage();
                        else
                            nextImage();
                    }
                    break;
                case SECONDARY:
                    //this.invertOrientationTag();
                    favCurrent();
                    break;
                case MIDDLE:
                    if(mevent.getX() < width/2)
                        rotateImage(true);
                    else
                        rotateImage(false);
                    break;
                default:
                    break;
            }
        });
        
        scene.addEventHandler(ScrollEvent.SCROLL, (sevent) -> {
            if(sevent.getTextDeltaY() < 0)
            {
                curzoom -= zoomstep;
                if(curzoom < minzoom)
                    curzoom = minzoom;
            }
            else
            {
                curzoom += zoomstep;
                if(curzoom > maxzoom)
                    curzoom = maxzoom;
            }
            imv.setFitHeight(scene.getHeight()*curzoom);
            imv.setFitWidth(scene.getWidth()*curzoom);
            //this.computeTranslations(origzoom, sevent.getSceneX(), sevent.getSceneY());
            //imv.setTranslateX(trX);
            //imv.setTranslateY(trY);
            this.computeTranslationsOut();
        });
        
        scene.setOnMouseDragged((MouseEvent t) -> {
            //double offsetX = t.getSceneX() - orgSceneX;
            //double offsetY = t.getSceneY() - orgSceneY;
            //double newTranslateX = orgTranslateX + offsetX;
            //double newTranslateY = orgTranslateY + offsetY;
            
            //((Circle)(t.getSource())).setTranslateX(newTranslateX);
            //((Circle)(t.getSource())).setTranslateY(newTranslateY);
            if(curzoom == 1.0)
                return;
            if(!dragging)
            {
                dragging = true;
                dragStartX = t.getSceneX();
                dragStartY = t.getSceneY();
            }
            double posX = t.getSceneX();
            double posY = t.getSceneY();
            
            drag(dragStartX-posX, dragStartY-posY);
            
            dragStartX = posX;
            dragStartY = posY;
        });
        
        scene.setOnMouseMoved((MouseEvent t) -> {
        	
        	//FIXME is there a more efficient way instead of calling this every time
        	//the mouse is moved?
            int x = (int)t.getSceneX();
            int y = (int)t.getSceneY();
            
            if(x > width - 100)
            	sideMenu.setVisible(true);
            else
            	sideMenu.setVisible(false);
            
            if(y < 250)
            	thumbViewer.setVisible(true);
            else
            	thumbViewer.setVisible(false);
        });
    }
    
    @Override
	public Object[] getData(String pos) {
		return new Object[0];
	}

	@Override
	public String[] getAvailablePositions() {
		return new String[0];
	}
    
    public void chainLink()
    {
        if(selected != null)
            return;
        if(linkChain == null)
        {
            linkChain = new ArrayList<>();
            linkIndicator.setVisible(true);
        }
        GImage cur = gal.getCurrentGImage();
        int pos = linkChainContainsID(cur.getID());
        if(pos >= 0)
        {
            linkChain.remove(pos);
        }
        else if(pos == -1)
        {
            linkChain.add(cur);
        }
        
        if(linkChain.size() <= 0)
        {
            clearSelection();
        }
        resetLinkNum();
    }
    
    public int linkChainContainsID(int id)
    {
        if(linkChain == null)
            return -2;
        for(int i = 0; i < linkChain.size(); i++)
            if(linkChain.get(i).getID() == id)
                return i;
        return -1;
    }
    
    public void selectImage()
    {
        if(linkChain != null)
            return;
        selected = gal.getCurrentGImage();
        moveIndicator.setVisible(true);
    }
    
    public void clearSelection()
    {
        selected = null;
        linkChain = null;
        moveIndicator.setVisible(false);
        linkIndicator.setVisible(false);
        resetLinkNum();
    }
    
    public void moveSelected()
    {
        if(selected == null)
            return;
        int res = gal.moveAfterCurrent(selected);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        
        switch (res)
        {
            case 0:
                clearSelection();
                break;
            case 1:
                alert.setContentText("The images are in different folders!");
                break;
            case -1:
                alert.setContentText("Folder doesn't contain one of the images!");
                break;
            case -2:
                alert.setContentText("Can't locate image to move in the folder!");
                break;
            case -3:
                alert.setContentText("Can't locate current image in folder!");
                break;
            case -10:
                alert.setContentText("Image to move is no longer in the gallery!");
                clearSelection();
                break;
            default:
                alert.setContentText("Image List error " + res + "!");
                break;
        }
        
        if(res != 0)
            alert.showAndWait();
    }
    
    public void linkSelected()
    {
        if(selected == null)
            return;
        int res = gal.linkToCurrent(selected);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        
        switch (res)
        {
            case 0:
                clearSelection();
                break;
            /*case 1:
                alert.setContentText("The images are in different folders!");
                break;
            case -1:
                alert.setContentText("Folder doesn't contain one of the images!");
                break;
            case -2:
                alert.setContentText("Can't locate image to move in the folder!");
                break;
            case -3:
                alert.setContentText("Can't locate current image in folder!");
                break;*/
            case -10:
                alert.setContentText("Image to move is no longer in the gallery!");
                clearSelection();
                break;
            default:
                alert.setContentText("Image List error " + res + "!");
                break;
        }
        
        if(res != 0)
            alert.showAndWait();
    }
    
    public void finishChainLink()
    {
        if(linkChain == null || linkChain.size() <= 1)
            return;
        int res = 0;
        for(int i = 0; i < linkChain.size()-1; i++)
        {
            res = gal.linkTo(linkChain.get(i+1), linkChain.get(i));
            if(res != 0)
                break;
        }
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        
        switch (res)
        {
            case 0:
                clearSelection();
                break;
            /*case 1:
                alert.setContentText("The images are in different folders!");
                break;
            case -1:
                alert.setContentText("Folder doesn't contain one of the images!");
                break;
            case -2:
                alert.setContentText("Can't locate image to move in the folder!");
                break;
            case -3:
                alert.setContentText("Can't locate current image in folder!");
                break;*/
            case -10:
                alert.setContentText("Image to move is no longer in the gallery!");
                clearSelection();
                break;
            default:
                alert.setContentText("Image List error " + res + "!");
                break;
        }
        
        if(res != 0)
            alert.showAndWait();
    }
    
    public void favCurrent()
    {
        gal.favCurrent(true);
        resetImage(false, true);
    }
    
    public int getCurrentRating()
    {
    	return gal.getCurrentRating();
    }
    
    public void lowCurrent()
    {
        gal.lowCurrent(true);
        resetImage(false, true);
    }
    
    public void invertOrientationTag()
    {
        gal.invertOrientationTag();
        resetImage(false, true);
    }
    
    public void rotateImage(boolean left)
    {
        gal.rotateImage(left);
        gal.invertOrientationTag();
        gal.reloadImage();
        resetImage(false, true);
    }
    
    public void setGallery(Gallery g, String fname)
    {
    	if(gal != null)
    		return;
	    gal = g;
	    gal.bindToCurrentImageProperty(curImage);
	    curImage.addListener((o, oldValue, newValue) -> {
	    	if(newValue.intValue() == -1)
	    		return;
	    	imv.setImage(gal.getCurrent());
	    	previmg = curimg;
	        curimg = gal.getCurrentGImage();
	        if(previmg != null && curimg != null && previmg.getParent() != curimg.getParent())
	            flashFolderChanged();
	    });
	    if(fname == null)
	    	gal.getNext(false);
	    else
	    	gal.getByName(fname);
	    lastload = System.currentTimeMillis();
	    this.resetImage(true, false);
    	
    	thumbViewer.initFrames();
    }
    
    public void move(int x, int y, int step)
    {
        double maxX = this.getMaxXTranslation(), maxY = this.getMaxYTranslation();
        trX = trX - x*step;
        trY = trY - y*step;
        if(trX > maxX || trX < -maxX)
            trX = Math.signum(trX)*maxX;
        if(trY > maxY || trY < -maxY)
            trY = Math.signum(trY)*maxY;
        imv.setTranslateX(trX);
        imv.setTranslateY(trY);
    }
    
    public void move(int x, int y)
    {
        move(x, y, movestep);
    }
    
    public void drag(double x, double y)
    {
        move((int)x, (int)y, 1);
    }
    
    public void computeTranslations(double z, double x, double y)
    {
        double ptx = -trX;
        double pty = -trY;
        double maxX = this.getMaxXTranslation(), maxY = this.getMaxYTranslation();
        trX = -(x-width/2.0+ptx)*(1+(curzoom-z)/z);
        trY = -(y-height/2.0+pty)*(1+(curzoom-z)/z);
        if(trX > maxX || trX < -maxX)
            trX = Math.signum(trX)*maxX;
        if(trY > maxY || trY < -maxY)
            trY = Math.signum(trY)*maxY;
    }
    
    public void computeTranslationsOut()
    {
        move(0, 0, 1);
    }
    
    public double getMaxXTranslation()
    {
        double maxX = 0;
        double ix = imv.getBoundsInParent().getWidth();
        if(ix >= width)
            maxX = (ix-width)/2.0;
        return maxX;
    }
    
    public double getMaxYTranslation()
    {
        double maxY = 0;
        double iy = imv.getBoundsInParent().getHeight();
        if(iy >= height)
            maxY = (iy-height)/2.0;
        return maxY;
    }
    
    public void jump(int num)
    {
        long time = System.currentTimeMillis();
        if(time < lastload + throttling)
            return;
        
        gal.jump(num);
        resetImage(true, false);
        
        lastload = System.currentTimeMillis();
    }
    
    public void navigateTo(int num)
    {
        long time = System.currentTimeMillis();
        if(time < lastload + throttling)
            return;
        
        gal.navigateTo(num);
        resetImage(true, false);
        
        lastload = System.currentTimeMillis();
    }
    
    public void jumpFolder(boolean forward)
    {
        long time = System.currentTimeMillis();
        if(time < lastload + throttling)
            return;
        
        gal.jumpFolder(forward, limitToFav.get());
        resetImage(true, false);
        
        lastload = System.currentTimeMillis();
    }
    
    public void jumpOrientation()
    {
        long time = System.currentTimeMillis();
        if(time < lastload + throttling)
            return;
        
        gal.jumpOrientationWithinFolder();
        resetImage(true, false);
        
        lastload = System.currentTimeMillis();
    }
    
    public void nextImage()
    {
        long time = System.currentTimeMillis();
        if(time < lastload + throttling)
            return;
        
        gal.getNext(limitToFav.get());
        resetImage(true, false);
        
        lastload = System.currentTimeMillis();
    }
    
    public void prevImage()
    {
        long time = System.currentTimeMillis();
        if(time < lastload + throttling)
            return;
        
        gal.getPrev(limitToFav.get());
        resetImage(true, false);
        
        lastload = System.currentTimeMillis();
    }
    
    public void flashFolderChanged()
    {
        FadeTransition ft = new FadeTransition(Duration.millis(500), folderChangeIndicator);
        ft.setFromValue(0.7);
        ft.setToValue(0.0);
        ft.setCycleCount(1);
        ft.setAutoReverse(false);
        ft.play();
    }
    
    /*public void removeImage()
    {
        long time = System.currentTimeMillis();
        if(time < lastload + throttling)
            return;
        
        Image im = gal.removeCurrent();
        imv.setImage(im);
        resetImage();
        
        lastload = System.currentTimeMillis();
    }*/
    
    public void resetImage(boolean newView, boolean newMod)
    {
        if((tagging.get() && gal.getCurrentGImage().hasTag("vertical")) || forceRotate.get())
        {
            imv.setRotate(90);
            imv.setFitHeight(scene.getWidth());
            imv.setFitWidth(scene.getHeight());
        }
        else
        {
            imv.setRotate(0);
            imv.setFitHeight(scene.getHeight());
            imv.setFitWidth(scene.getWidth());
        }
        
        if(newMod)
            gal.currentImageModified();
        
        //TODO hacky, but have to do it so that we don't have to set  folder's 
        //rating right after getting it
        curRating.removeListener(curRatingChanged);
        curRating.set(getCurrentRating());
        curRating.addListener(curRatingChanged);
        
        curzoom = 1.0;
        trX = 0;
        trY = 0;
        imv.setTranslateX(0);
        imv.setTranslateY(0);
        
        taggingOverlay.reset();
        infoOverlay.reset();
        
        resetLinkNum();
        
        if(newView)
            gal.currentImageViewed();
    }
    
    public void resetLinkNum()
    {
        if(gal.getCurrentGImage() == null)
            return;
        int id = gal.getCurrentGImage().getID();
        int pos = this.linkChainContainsID(id);
        if(pos < 0)
        {
            linkNumBox.setVisible(false);
            return;
        }
        linkNum.setText(String.valueOf(pos+1));
        linkNumBox.setVisible(true);
    }
    
    public void setTagging(boolean value)
    {
        infoOverlay.setTagsVisible(!value);
        taggingOverlay.setVisible(value);
        resetImage(false, false);
    }
    
    //FIXME redo, this is just temporary method
    public void addKeyEventHandler(EventHandler<? super KeyEvent> handler) {
    	scene.addEventHandler(KeyEvent.KEY_PRESSED, handler);
    }
    
    private class TaggingOverlay
    {
        private final StackPane taggingPane;
        
        private final TextFlow abbrs1, abbrs2;
        private final TextField tagInput;
        
        public TaggingOverlay()
        {
            abbrs1 = new TextFlow();
            abbrs2 = new TextFlow();
            HBox abbrsWrapper = new HBox();
            abbrsWrapper.getChildren().addAll(abbrs1, abbrs2);
            abbrsWrapper.setMaxHeight(height);
            abbrsWrapper.setMaxWidth(400);
            abbrsWrapper.setBackground(FXFactory.createSimpleBackground(new Color(1.0, 1.0, 1.0, 0.7)));
            abbrsWrapper.setAlignment(Pos.CENTER_RIGHT);
            StackPane.setAlignment(abbrsWrapper, Pos.CENTER_RIGHT);
            //FIXME Redo the whole abbrs thing
            abbrsWrapper.visibleProperty().bind(abbrs1.visibleProperty());
            abbrs1.setVisible(true);
            abbrs2.setVisible(true);

            tagInput = new TextField();
            tagInput.setStyle("-fx-control-inner-background: #000000");
            tagInput.setFocusTraversable(false);
            StackPane.setAlignment(tagInput, Pos.BOTTOM_LEFT);
            tagInput.setVisible(true);

            taggingPane = new StackPane(abbrsWrapper, tagInput);
        }
        
        public final void initEventHandlers()
        {
            tagInput.addEventHandler(KeyEvent.KEY_PRESSED, (key) -> {
                if(null!=key.getCode()) switch (key.getCode()) {
                    case ENTER:
                        if(key.isControlDown())
                        {
                            resetImage(false, false);
                        }
                        else if(key.isShiftDown())
                        {
                            prevImage();
                        }
                        else
                        {
                            saveTags();
                            nextImage();
                        }
                        key.consume();
                        break;
                    case E:
                        if(key.isControlDown())
                        {
                        	tagging.set(false);
                            key.consume();
                        }
                        else
                        {
                            Timeline timeline1 = new Timeline(new KeyFrame(
                                Duration.millis(25),
                                ae -> fillAbbrs()));
                            timeline1.play();
                        }
                        break;
                    /*case SHIFT:
                        prevImage();
                        key.consume();
                        break;*/
                    case ESCAPE:
                        tagging.set(false);
                        key.consume();
                        break;
                    case SPACE:
                        String text = tagInput.getText();
                        if(!text.endsWith(" ") && !(text.length()<1))
                        {
                            String tag = text.substring(text.trim().lastIndexOf(" ") + 1);
                            //System.out.println("\"" + tag + "\"");
                            //TODO
                            tag = dbInterface.getActiveDatabase().processTag(tag);
                            int pos = text.trim().lastIndexOf(" ") + 1;
                            if(tag == null)
                            {
                                pos -= 1;
                                tag = "";
                            }
                            tagInput.setText(text.substring(0, pos)+tag);
                            tagInput.positionCaret(tagInput.getText().length());
                            Timeline timeline = new Timeline(new KeyFrame(
                                    Duration.millis(25),
                                    ae -> fillAbbrs()));
                            timeline.play();
                        }
                        break;
                    default:
                        //TODO better solution?
                        Timeline timeline1 = new Timeline(new KeyFrame(
                                Duration.millis(25),
                                ae -> fillAbbrs()));
                        timeline1.play();
                        break;
                }
            });
            
            //TODO releasing mouse button outside of tagInput fires the event to underlying pane,
            //thus going to prev/next image
        }
        
        public Parent getPane()
        {
            return taggingPane;
        }
        
        public void setVisible(boolean visible)
        {
            taggingPane.setVisible(visible);
            if(visible)
            	tagInput.requestFocus();
        }
        
        public void reset()
        {
            tagInput.setText(gal.getTagString());
            tagInput.positionCaret(tagInput.getText().length());
            if(tagging.get())
                fillAbbrs();
        }
        
        public void saveTags()
        {
            String t = tagInput.getText();
            if(t.trim().equalsIgnoreCase(""))
                return;
            String[] temp = t.split("\\s+");
            //TODO
            ArrayList<String> tags = dbInterface.getActiveDatabase().processTags(temp);
            gal.setTags(tags);
            gal.currentImageModified();
        }
        
        public void fillAbbrs()
        {
            /*ArrayList<String> a = db.getAbbrStrings();
            ArrayList<String> b = new ArrayList<String>();
            int mid = a.size()/2;
            for(int i = mid; i < a.size(); i++)
                b.add(a.get(i));
            int st = a.size()-1;
            for(int i = st; i >= mid; i--)
                a.remove(i);
            String res = "";
            for(int i = 0; i < Math.min(a.size(), b.size()); i++)
            {
                String s1 = a.get(i).toUpperCase();
                String s2 = b.get(i).toUpperCase();
                res += s1 + "\t\t\t" + s2 + "\n";
            }
            if(a.size() > b.size())
            {
                for(int i = b.size(); i<a.size(); i++)
                    res += "\t\t\t" + a.get(i);
            }
            else if(a.size() < b.size())
            {
                for(int i = a.size(); i<b.size(); i++)
                    res += "\t\t\t" + b.get(i).toUpperCase() + "\n";
            }*/
            //TODO slow, can do without creating everything anew each time?
            abbrs1.getChildren().clear();
            abbrs2.getChildren().clear();
            //TODO
            Map<String, String> dict = dbInterface.getActiveDatabase().getAbbrStrings();

            String t = tagInput.getText();
            if(t.trim().equalsIgnoreCase(""))
                return;
            String[] temp = t.split("\\s+");

            //String res1 = "", res2 = "";
            boolean[] mask = new boolean[temp.length];
            for(int i = 0; i < mask.length; i++)
                mask[i] = false;
            int maxnum=92;
            int num=0;
            for (Map.Entry<String, String> entry : dict.entrySet())
            {
                String k = entry.getKey().toUpperCase();
                String v = entry.getValue().toUpperCase();
                Paint paint = Color.DARKBLUE;
                Text txt = new Text(k+"\t"+v+"\n");
                for(int i = 0; i < temp.length; i++)
                {
                    if(temp[i].equalsIgnoreCase(v))
                    {
                        paint = Color.GREEN;
                        mask[i] = true;
                        break;
                    }
                    else if(temp[i].equalsIgnoreCase(k))
                    {
                        paint = Color.MAGENTA;
                        mask[i] = true;
                        break;
                    }
                    /*else
                    {
                        txt.setOnMousePressed(new EventHandler<MouseEvent>()
                        {
                            public void handle(MouseEvent mevent)
                            {
                                if(null != mevent.getButton())
                                switch (mevent.getButton())
                                {
                                    case PRIMARY:
                                        tagInput.appendText(k.toLowerCase());
                                        break;
                                    default:
                                        break;
                                }
                            System.out.println("CLICK");
                            mevent.consume();
                            }
                        });
                    }*/
                }

                txt.setFont(Font.font("Courier New", FontWeight.BOLD, 10));
                txt.setFill(paint);
                if(num<maxnum)
                    abbrs1.getChildren().add(txt);
                else
                    abbrs2.getChildren().add(txt);
                num++;
            }

            for(int i = 0; i < mask.length; i++)
            {
                if(mask[i])
                    continue;
                Text txt = new Text(temp[i].toUpperCase()+"\n");
                txt.setFont(Font.font("Courier New", FontWeight.BOLD, 10));
                txt.setFill(Color.DARKGOLDENROD);
                if(num<maxnum)
                    abbrs1.getChildren().add(txt);
                else
                    abbrs2.getChildren().add(txt);
                num++;
            }
        }
    }
    
    private class InfoOverlay
    {
    	//Info format is :
    	//	0			1		2			3		(4		5	6)
    	//num/total rootPath subfolders imageName added viewed mod
    	private final ColorMap infoColorMap = new ColorMap(Color.BLUE);
    	{
    		infoColorMap.setColor(1, Color.LIGHTBLUE);
    		infoColorMap.setColor(3, Color.YELLOW);
    	}
    	
        private final StackPane infoPane;
        
        private final Text tags;
        private final MultiColoredText info;
        private final TextArea commentArea;
        
        public InfoOverlay()
        {
            info = new MultiColoredText(5, styleSheet.getDefaultInfoFont(), infoColorMap);
            
            VBox infoWrapper = new VBox();
            infoWrapper.setPadding(new Insets(5,0,0,5));
            infoWrapper.getChildren().addAll(info);
            infoWrapper.setAlignment(Pos.TOP_LEFT);
            StackPane.setAlignment(infoWrapper, Pos.TOP_LEFT);
            infoWrapper.visibleProperty().bind(info.visibleProperty());
        
            tags = new Text("");
            tags.setFont(styleSheet.getDefaultInfoFont());
            tags.setFill(Color.BLUE);
            tags.setWrappingWidth(width);
            
            VBox tagsWrapper = new VBox();
            tagsWrapper.setPadding(new Insets(0,0,5,5));
            tagsWrapper.getChildren().addAll(tags);
            tagsWrapper.setAlignment(Pos.BOTTOM_LEFT);
            StackPane.setAlignment(tagsWrapper, Pos.BOTTOM_LEFT);
            tagsWrapper.visibleProperty().bind(tags.visibleProperty());
            tags.setVisible(true);
            info.setVisible(true);
            
            commentArea = new TextArea();
            commentArea.setPrefRowCount(10);
            commentArea.setPrefColumnCount(100);
            commentArea.setWrapText(true);
            //commentArea.setPrefWidth(300);
            //commentArea.setMaxWidth(300);
            VBox commentAreaWrapper = new VBox();
            //FIXME redo the whole commentArea thing
            commentAreaWrapper.setMaxWidth(300);
            commentAreaWrapper.visibleProperty().bind(commentArea.visibleProperty());
            commentAreaWrapper.getChildren().addAll(commentArea);
            commentAreaWrapper.setAlignment(Pos.CENTER_LEFT);
            StackPane.setAlignment(commentAreaWrapper, Pos.CENTER_LEFT);
            commentArea.setVisible(false);
            
            infoPane = new StackPane(infoWrapper, tagsWrapper, commentAreaWrapper);
            
            //TODO performance?
            infoPane.setPickOnBounds(false);
            infoWrapper.setPickOnBounds(false);
        }
        
        public final void initEventHandlers()
        {
            commentArea.addEventHandler(KeyEvent.KEY_PRESSED, (key) ->{
                if(null!=key.getCode()) switch (key.getCode())
                {
                    case ESCAPE:
                        commentArea.setVisible(false);
                        key.consume();
                        break;
                    case ENTER:
                        this.saveComment();
                        commentArea.setVisible(false);
                        key.consume();
                        break;
                    case SPACE:
                        if(key.isShiftDown())
                            key.consume();
                        break;
                    default:
                        break;
                }
            });
        }
        
        public Parent getPane()
        {
            return infoPane;
        }
        
        public void saveComment()
        {
            String c = commentArea.getText().trim();
            String c1 = gal.getCurrentComment();
            if(c1 != null && c.equals(c1))
                return;
            if(c1 == null && (c == null || c.equalsIgnoreCase("")))
                return;
            gal.setCurrentComment(c);
            resetImage(false, true);
        }
        
        public void reset()
        {
        	String[] curNamePieces = gal.getCurrentNameFullPiecewise();
        	String[] infoText = new String[5];
        	infoText[0] = gal.getCurrentPosition()+1 + "/" + gal.getSize() + " ("
        					+ (gal.getCurrentPositionWithinFolder()+1) + "/" + gal.getCurrentFolderSize() + ") ";
        	infoText[1] = curNamePieces[0];
        	infoText[2] = curNamePieces.length < 3 ? "" : curNamePieces[1];
        	infoText[3] = curNamePieces[curNamePieces.length-1];
        	infoText[4] = 
        			"   Added : " + gal.getAdded() + "   Viewed : " + gal.getViewed() + "   Mod : " + gal.getLastmod();

            info.setText(infoText);
            tags.setText(gal.getTagString());
            commentArea.setText(gal.getCurrentComment());
        }
        
        public BooleanProperty getInfoVisibleProperty()
        {
        	return info.visibleProperty();
        }
        
        public BooleanProperty getTagsVisibleProperty()
        {
        	return tags.visibleProperty();
        }
        
        public BooleanProperty getCommentVisibleProperty()
        {
        	return commentArea.visibleProperty();
        }
        
        public void setTagsVisible(boolean visible)
        {
            tags.setVisible(visible);
        }
        
        public void setInfoVisible(boolean visible)
        {
            info.setVisible(visible);
        }
        
        public void setCommentVisible(boolean visible)
        {
            commentArea.setVisible(visible);
        }
        
        public void toggleTagsVisible()
        {
            tags.setVisible(!tags.isVisible());
        }
        
        public void toggleInfoVisible()
        {
            info.setVisible(!info.isVisible());
        }
        
        public void toggleCommentVisible()
        {
            commentArea.setVisible(!commentArea.isVisible());
        }
        
        public boolean isCommentAreaActive()
        {
            return commentArea.isVisible();
        }
    }
    
    private class SideMenu
    {
    	private StackPane menuPane;
    	
    	private VBox rightMenu;
    	
    	public SideMenu()
    	{
    		//*-------------------------------------------------
    		//* RIGHT MENU
    		//*-------------------------------------------------
    		
    		rightMenu = new VBox();
    		rightMenu.setBackground(styleSheet.getMenuBackground());
    		rightMenu.setBorder(styleSheet.getMenuBorder());
    		
    		//* GENERAL GROUP
    		Label generalGroupLabel = HFXFactory.createSectionTitleLabel("General", styleSheet);
    		
    		VBox generalGroupContent = new VBox();
    		generalGroupContent.setPadding(new Insets(10,10,10,10));
    		generalGroupContent.setSpacing(2);
    		CheckBox cbDisplayInfo = HFXFactory.createBoundCheckBox("Display info", infoOverlay.getInfoVisibleProperty());
    		//TODO when exiting tagging mode, "tags" visibility gets set to true
    		//regardless of what it was before entering tagging mode
    		CheckBox cbDisplayTags = HFXFactory.createBoundCheckBox("Display tags", infoOverlay.getTagsVisibleProperty());
    		CheckBox cbDisplayComment = HFXFactory.createBoundCheckBox("Display comment", infoOverlay.getCommentVisibleProperty());
    		CheckBox cbTagging = HFXFactory.createBoundCheckBox("Tagging mode", tagging);
    		//Disable the "Display tags" checkbox while tagging
    		cbDisplayTags.disableProperty().bind(tagging);
    		CheckBox cbLimitToFavs = HFXFactory.createBoundCheckBox("Limit to favs", limitToFav);
    		CheckBox cbForceRotate = HFXFactory.createBoundCheckBox("Force rotate", forceRotate);
    		generalGroupContent.getChildren().addAll(cbDisplayInfo, cbDisplayTags, cbDisplayComment,
    				cbTagging, cbLimitToFavs, cbForceRotate);
    		
    		//* IMAGE ACTIONS GROUP
    		Label imageGroupLabel = HFXFactory.createSectionTitleLabel("Image Actions", styleSheet);
    		
    		VBox imageGroupContent = new VBox();
    		imageGroupContent.setPadding(new Insets(10,10,10,10));
    		imageGroupContent.setSpacing(2);
    		Label rotateLabel = HFXFactory.createMenuLabel("Rotate image", styleSheet);
    		HBox rotateButtonsWrapper = new HBox();
    		rotateButtonsWrapper.setSpacing(5);
    		rotateButtonsWrapper.setAlignment(Pos.BASELINE_CENTER);
    		Button rotateRight = HFXFactory.createUnboundedButton("Right");
    		rotateRight.setOnAction(event -> { rotateImage(false); });
    		Button rotateLeft = HFXFactory.createUnboundedButton("Left");
    		rotateLeft.setOnAction(event -> { rotateImage(true); });
    		Button invertOrientation = HFXFactory.createUnboundedButton("Invert orientation");
    		invertOrientation.setOnAction(event -> { invertOrientationTag(); });
    		rotateButtonsWrapper.getChildren().addAll(rotateLeft, rotateRight, invertOrientation);
    		imageGroupContent.getChildren().addAll(rotateLabel, rotateButtonsWrapper);
    		
    		//* FOLDER RATING
    		Label ratingGroupLabel = HFXFactory.createSectionTitleLabel("Folder Rating", styleSheet);
    		VBox ratingGroupContent = new VBox();
    		ratingGroupContent.setPadding(new Insets(10,10,10,10));
    		ratingGroupContent.setSpacing(2);
    		RatingBar ratingBar = new RatingBar(5, 32, styleSheet.getStarFull(),
    				styleSheet.getStarEmpty(), styleSheet.getStarNoRating());
    		ratingBar.bindCurRating(curRating);
    		ratingGroupContent.getChildren().addAll(ratingBar.getContainer());
    		
    		//* TEST
    		Label testGroupLabel = HFXFactory.createSectionTitleLabel("Test", styleSheet);
    		VBox testGroupContent = new VBox();
    		testGroupContent.setPadding(new Insets(10,10,10,10));
    		testGroupContent.setSpacing(2);
    		Button testProgress = HFXFactory.createUnboundedButton("Test progress window");
    		testProgress.setOnAction(event -> {
    			Stage dialog = new Stage();
    			VBox dialogRoot = new VBox();
    			ProgressBar pb = new ProgressBar(0);
    			pb.setPrefWidth(400);
    			dialogRoot.setAlignment(Pos.CENTER);
    			dialogRoot.getChildren().addAll(pb);
    			Scene dialogScene = new Scene(dialogRoot, 500, 100);
    			dialog.setScene(dialogScene);
    			dialog.initOwner(instance);
    			dialog.initModality(Modality.APPLICATION_MODAL);
    			dialog.show();
    			
    		});
    		testGroupContent.getChildren().addAll(testProgress);
    		
    		rightMenu.getChildren().addAll(generalGroupLabel, generalGroupContent,
    				imageGroupLabel, imageGroupContent, ratingGroupLabel, ratingGroupContent,
    				testGroupLabel, testGroupContent);
    		//TODO better way to stop the menu from taking the whole height?
    		VBox rightMenuTopDummy = new VBox();
    		rightMenuTopDummy.setPrefHeight(height);
    		VBox rightMenuBottomDummy = new VBox();
    		rightMenuBottomDummy.setPrefHeight(height);
    		VBox rightMenuWrapper = new VBox();
    		rightMenuWrapper.setMaxWidth(300);
    		rightMenuWrapper.getChildren().addAll(rightMenuTopDummy, rightMenu, rightMenuBottomDummy);
    		
    		//*-------------------------------------------------
    		//* PARENT PANE
    		//*-------------------------------------------------
    		
    		menuPane = new StackPane();
    		StackPane.setAlignment(rightMenuWrapper, Pos.CENTER_RIGHT);
    		menuPane.getChildren().addAll(rightMenuWrapper);
    		menuPane.setPickOnBounds(false);
    	}
    	
    	public void initEventHandlers()
    	{
    		//Prevent clicked events from going through to elements below
    		rightMenu.setOnMouseClicked(mevent -> {
    			mevent.consume();
    		});
    		
    		//Prevent the menu from disappearing if it is still being moused over
    		rightMenu.setOnMouseMoved(mevent -> {
    			mevent.consume();
    		});
    	}
    	
    	public Parent getPane()
    	{
    		return menuPane;
    	}
    	
    	public void setVisible(boolean visible)
    	{
    		menuPane.setVisible(visible);
    	}
    }
    
    private class ThumbViewer {
    	private StackPane thumbPane;
    	Map<GImage, Integer> favs;
    	List<HImageFrame> frames;
    	
    	private HBox thumbBox;
    	private ScrollPane sp;
    	
    	public ThumbViewer() {
    		
    		//*-------------------------------------------------
    		//* THUMB VIEWER
    		//*-------------------------------------------------
    		
    		thumbBox = new HBox();
    		thumbBox.setBackground(styleSheet.getMenuBackground());
    		thumbBox.setBorder(styleSheet.getMenuBorder());
    		thumbBox.setPrefHeight(250);
    		sp = new ScrollPane();
    		sp.setContent(thumbBox);
    		sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    		sp.setOnScroll(event -> {
    		    if(event.getDeltaY() != 0) {
    		        sp.setHvalue(sp.getHvalue() - event.getDeltaY());
    		    }
    		});
    		
    		//TODO better way to stop the menu from taking the whole height?
    		VBox thumbBoxBottomDummy = new VBox();
    		thumbBoxBottomDummy.setPrefHeight(height-250);
    		VBox thumbBoxWrapper = new VBox();
    		thumbBoxWrapper.setPrefWidth(width);
    		thumbBoxWrapper.getChildren().addAll(sp, thumbBoxBottomDummy);
    		
    		//*-------------------------------------------------
    		//* PARENT PANE
    		//*-------------------------------------------------
    		
    		thumbPane = new StackPane();
    		StackPane.setAlignment(thumbBoxWrapper, Pos.TOP_CENTER);
    		thumbPane.getChildren().addAll(thumbBoxWrapper);
    		thumbPane.setPickOnBounds(false);
    	}
    	
    	public void initFrames() {
    		logger.info("Starting");
    		favs = gal.getFavs();
    		logger.info("Got favs");
    		int thumbSize = 180;
    		
    		frames = new ArrayList<>();
    		
    		ArrayList<Entry<GImage, Integer>> favsList = new ArrayList<>();
    		favsList.addAll(favs.entrySet());
    		favsList.sort((a,b) -> Integer.compare(a.getValue(), b.getValue()));
    		
    		for(Entry<GImage, Integer> entry : favsList) {
    			HImageFrame frame = new HImageFrame(entry.getKey(), entry.getValue(), thumbSize);
    			
    			frame.setBorderVisible(true);
    			frame.setOnMouseClicked(event -> {
    				navigateTo(frame.getGalPos());
    			});
    			frames.add(frame);
    		}
    		
    		logger.info("Made List");
    		
    		thumbBox.getChildren().addAll(frames);
    		
    		logger.info("Done");
    	}
    	
    	public void initEventHandlers() {
    		//Prevent clicked events from going through to elements below
    		sp.setOnMouseClicked(mevent -> {
    			mevent.consume();
    		});
    		
    		//Prevent the menu from disappearing if it is still being moused over
    		sp.setOnMouseMoved(mevent -> {
    			mevent.consume();
    		});
    	}
    	
    	public Parent getPane() {
    		return thumbPane;
    	}
    	
    	public void setVisible(boolean visible) {
    		thumbPane.setVisible(visible);
    	}
    }
}