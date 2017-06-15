/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nquisition.hlibrary.ui;

import com.nquisition.hlibrary.HLibrary;
import com.nquisition.hlibrary.fxutil.ColorMap;
import com.nquisition.hlibrary.fxutil.MultiColoredText;
import com.nquisition.hlibrary.model.Gallery;
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
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.stage.*;
import javafx.animation.*;
import javafx.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Master
 */
public class GalleryViewer extends HConsoleStage
{
    private static final Logger logger_local = LogManager.getLogger(GalleryViewer.class.getName()+".local");
    private static final Logger logger_global = LogManager.getLogger(GalleryViewer.class.getName()+".global");
    
    private final Logger logger;
    
    private Gallery gal;
    private ImageView imv;
    private Text linkNum;
    
    private HConsoleTextArea consoleTextArea;
    
    private TaggingOverlay taggingOverlay;
    private InfoOverlay infoOverlay;
    
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
    private boolean limitToFav = false;
    private boolean dragging = false;
    private double dragStartX = 0.0, dragStartY = 0.0;
    
    private GImage selected = null;
    private GImage previmg = null, curimg = null;
    private ArrayList<GImage> linkChain = null;
    private boolean tagging = false;
    private boolean forceRotate = false;
    
    private Database db;
    
    private long throttling = 20;
    private long lastload = 0;
    
    public GalleryViewer(Database d)
    {
        super();
        
        this.setDatabase(d);
        logger = db.isLocal()?logger_local:logger_global;
        
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
        linkNumBox.setAlignment(Pos.TOP_RIGHT);
        linkNumBox.setStyle("-fx-background-color: #FF0000");
        linkNumBox.setVisible(false);
        
        infoOverlay = new InfoOverlay();
        taggingOverlay = new TaggingOverlay();
        
        //TODO never added to the scene
        consoleTextArea = new HConsoleTextArea(this);
        consoleTextArea.setVisible(false);
        HLibrary.registerListenerWithConsole(consoleTextArea);
        
        root.getChildren().addAll(imv, moveIndicator, linkIndicator, folderChangeIndicator, taggingOverlay.getPane(), infoOverlay.getPane(), linkNumBox);

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
                        setTagging(true);
                        key.consume();
                    }
                    break;
                case F:
                    limitToFav = !limitToFav;
                    break;
                case R:
                    forceRotate = !forceRotate;
                    this.resetImage(false, false);
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
                    rankCurrentFolder(0);
                    break;
                case DIGIT1:
                    rankCurrentFolder(1);
                    break;
                case DIGIT2:
                    rankCurrentFolder(2);
                    break;
                case DIGIT3:
                    rankCurrentFolder(3);
                    break;
                case DIGIT4:
                    rankCurrentFolder(4);
                    break;
                case DIGIT5:
                    rankCurrentFolder(5);
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
    
    public void rankCurrentFolder(int r)
    {
        gal.rankCurrentFolder(r);
        resetImage(false, false);
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
        imv.setImage(gal.reloadImage());
        resetImage(false, true);
    }
    
    public final void setDatabase(Database d)
    {
        db = d;
    }
    
    public void setGallery(Gallery g)
    {
        gal = g;
        imv.setImage(gal.getNext(false));
        lastload = System.currentTimeMillis();
        this.resetImage(true, false);
    }
    
    public void setGallery(Gallery g, String fname)
    {
        gal = g;
        imv.setImage(gal.getByName(fname));
        lastload = System.currentTimeMillis();
        this.resetImage(true, false);
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
        
        Image im = gal.jump(num);
        imv.setImage(im);
        
        previmg = curimg;
        curimg = gal.getCurrentGImage();
        if(previmg != null && curimg != null && previmg.getParent() != curimg.getParent())
            flashFolderChanged();
        
        resetImage(true, false);
        
        lastload = System.currentTimeMillis();
    }
    
    public void jumpFolder(boolean forward)
    {
        long time = System.currentTimeMillis();
        if(time < lastload + throttling)
            return;
        
        Image im = gal.jumpFolder(forward, limitToFav);
        imv.setImage(im);
        
        previmg = curimg;
        curimg = gal.getCurrentGImage();
        if(previmg != null && curimg != null && previmg.getParent() != curimg.getParent())
            flashFolderChanged();
        
        resetImage(true, false);
        
        lastload = System.currentTimeMillis();
    }
    
    public void jumpOrientation()
    {
        long time = System.currentTimeMillis();
        if(time < lastload + throttling)
            return;
        
        Image im = gal.jumpOrientationWithinFolder();
        imv.setImage(im);
        resetImage(true, false);
        
        lastload = System.currentTimeMillis();
    }
    
    public void nextImage()
    {
        long time = System.currentTimeMillis();
        if(time < lastload + throttling)
            return;
        
        Image im = gal.getNext(limitToFav);
        imv.setImage(im);
        
        previmg = curimg;
        curimg = gal.getCurrentGImage();
        if(previmg != null && curimg != null && previmg.getParent() != curimg.getParent())
            flashFolderChanged();
        
        resetImage(true, false);
        
        lastload = System.currentTimeMillis();
    }
    
    public void prevImage()
    {
        long time = System.currentTimeMillis();
        if(time < lastload + throttling)
            return;
        
        Image im = gal.getPrev(limitToFav);
        imv.setImage(im);
        
        previmg = curimg;
        curimg = gal.getCurrentGImage();
        if(previmg != null && curimg != null && previmg.getParent() != curimg.getParent())
            flashFolderChanged();
        
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
        if((tagging && gal.getCurrentGImage().hasTag("vertical")) || forceRotate)
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
    
    public void setTagging(boolean t)
    {
        infoOverlay.setTagsVisible(!t);
        taggingOverlay.setVisible(t);
        tagging = t;
        resetImage(false, false);
    }
    
    private class TaggingOverlay
    {
        private final StackPane taggingPane;
        private final TextFlow abbrs1, abbrs2;
        private final TextField tagInput;
        
        public TaggingOverlay()
        {
            taggingPane = new StackPane();
        
            abbrs1 = new TextFlow();
            abbrs2 = new TextFlow();
            HBox abbrsWrapper = new HBox();
            abbrsWrapper.getChildren().addAll(abbrs1, abbrs2);
            abbrsWrapper.setAlignment(Pos.CENTER_RIGHT);
            abbrs1.setVisible(true);
            abbrs2.setVisible(true);

            Rectangle abbrsbg = new Rectangle(0, 0, 400, height);
            abbrsbg.setFill(new Color(1.0, 1.0, 1.0, 0.7));
            abbrsbg.setVisible(true);
            HBox abbrsbgWrapper = new HBox();
            abbrsbgWrapper.getChildren().addAll(abbrsbg);
            abbrsbgWrapper.setAlignment(Pos.CENTER_RIGHT);

            tagInput = new TextField();
            tagInput.setStyle("-fx-control-inner-background: #000000");
            VBox tagInputWrapper = new VBox();
            tagInputWrapper.getChildren().addAll(tagInput);
            tagInputWrapper.setAlignment(Pos.BOTTOM_LEFT);
            tagInput.setVisible(true);

            taggingPane.getChildren().addAll(abbrsbgWrapper, abbrsWrapper, tagInputWrapper);
            taggingPane.setVisible(false);
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
                            setTagging(false);
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
                        setTagging(false);
                        key.consume();
                        break;
                    case SPACE:
                        String text = tagInput.getText();
                        if(!text.endsWith(" ") && !(text.length()<1))
                        {
                            String tag = text.substring(text.trim().lastIndexOf(" ") + 1);
                            //System.out.println("\"" + tag + "\"");
                            tag = db.processTag(tag);
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
        }
        
        public StackPane getPane()
        {
            return taggingPane;
        }
        
        public void setVisible(boolean visible)
        {
            taggingPane.setVisible(visible);
        }
        
        public void reset()
        {
            tagInput.setText(gal.getTagString());
            tagInput.positionCaret(tagInput.getText().length());
            if(tagging)
                fillAbbrs();
        }
        
        public void saveTags()
        {
            String t = tagInput.getText();
            if(t.trim().equalsIgnoreCase(""))
                return;
            String[] temp = t.split("\\s+");
            ArrayList<String> tags = db.processTags(temp);
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
            Map<String, String> dict = db.getAbbrStrings();

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
    	private final Font defaultFont = Font.font("Arial", FontWeight.BOLD, 18);
    	
        private final StackPane infoPane;
        
        //FIXME When tags do not fit to page, the info moves off-screen to the left
        //for some reason
        private final Text tags;
        private final MultiColoredText info;
        private final TextArea commentArea;
        
        public InfoOverlay()
        {
            infoPane = new StackPane();
            
            info = new MultiColoredText(5, defaultFont, infoColorMap);
            
            VBox infoWrapper = new VBox();
            infoWrapper.getChildren().addAll(info);
            infoWrapper.setAlignment(Pos.TOP_LEFT);
        
            tags = new Text("");
            tags.setFont(defaultFont);
            tags.setFill(Color.BLUE);
            
            VBox tagsWrapper = new VBox();
            tagsWrapper.getChildren().addAll(tags);
            tagsWrapper.setAlignment(Pos.BOTTOM_LEFT);
            tags.setVisible(true);
            info.setVisible(true);
            
            commentArea = new TextArea();
            commentArea.setPrefRowCount(10);
            commentArea.setPrefColumnCount(100);
            commentArea.setWrapText(true);
            commentArea.setPrefWidth(300);
            commentArea.setMaxWidth(300);
            VBox commentAreaWrapper = new VBox();
            commentAreaWrapper.getChildren().addAll(commentArea);
            commentAreaWrapper.setAlignment(Pos.CENTER_LEFT);
            commentArea.setVisible(false);
            
            infoPane.getChildren().addAll(infoWrapper, tagsWrapper, commentAreaWrapper);
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
        
        public StackPane getPane()
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
        	infoText[0] = gal.getCurPos()+1 + "/" + gal.getSize() + " ";
        	infoText[1] = curNamePieces[0];
        	infoText[2] = curNamePieces.length < 3 ? "" : curNamePieces[1];
        	infoText[3] = curNamePieces[curNamePieces.length-1];
        	infoText[4] = 
        			"   Added : " + gal.getAdded() + "   Viewed : " + gal.getViewed() + "   Mod : " + gal.getLastmod();

            info.setText(infoText);
            tags.setText(gal.getTagString());
            commentArea.setText(gal.getCurrentComment());
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
}
