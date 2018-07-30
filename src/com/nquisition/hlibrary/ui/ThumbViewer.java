/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nquisition.hlibrary.ui;

import com.nquisition.hlibrary.model.Gallery;
import com.nquisition.hlibrary.model.HFolderInfo;
import com.nquisition.hlibrary.HLibrary;
import com.nquisition.hlibrary.api.UIView;
import com.nquisition.hlibrary.model.Database;
import com.nquisition.hlibrary.model.HImageInfo;
import static com.nquisition.hlibrary.model.HImageInfo.RESOLUTION;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import javafx.geometry.*;
import javafx.event.*;
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
import javax.imageio.ImageIO;
import java.awt.image.*;
import javafx.embed.swing.SwingFXUtils;

/**
 *
 * @author Master
 */
public class ThumbViewer extends HConsoleStage
{
    private Database db;
    private ArrayList<ImageFrame> iFrames;
    
    public static final int THUMB_SIZE = 180;
    
    //TODO needs onClose operation/cleanup
    public ThumbViewer(Database d, ArrayList<HFolderInfo> folders)
    {
        super();
        db = d;
        int x=0, y=-1;
        int maxX = (int)(1250.0/(THUMB_SIZE+20))-1;
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        iFrames = new ArrayList<ImageFrame>();

        for(HFolderInfo f : folders)
        {
            x=0;
            y++;
            Text fname = new Text(f.getPath());
            fname.setFont(Font.font("Arial", FontWeight.BOLD, 24));
            gridPane.add(fname, 0, y, maxX+1, 1);
            y++;
            ArrayList<HImageInfo> imgs = new ArrayList<HImageInfo>();
            //TODO recursively add subfolders instead
            f.getAllImages(imgs);
            
            ArrayList<ImageFrame> end = new ArrayList<ImageFrame>();
            
            for(HImageInfo img : imgs)
            {
                //System.out.println(img.getFullPath());
                /*Image image = new Image(new File(img.getFullPath()).toURI().toString(), THUMB_SIZE, THUMB_SIZE, true, false);
                ImageView imv = new ImageView();
                imv.setImage(image);*/
                //TODO rotate the image instead?
                /*
                if(img.hasTag("vertical"))
                {
                    imv.setRotate(90);
                }*/

                /*imv.setFitHeight(THUMB_SIZE);
                imv.setFitWidth(THUMB_SIZE);
                imv.setPreserveRatio(true);*/

                ImageFrame imf = new ImageFrame(img);
                iFrames.add(imf);
                if(img.hasTag("horizontal"))
                {
                    gridPane.add(imf, x, y);
                    x++;
                    if(x > maxX)
                    {
                        x=0;
                        y++;
                    }
                }
                else /*if(img.hasTag("vertical"))*/
                {
                    end.add(imf);
                }
            }
            
            for(ImageFrame imf : end)
            {
                gridPane.add(imf, x, y);
                x++;
                if(x > maxX)
                {
                    x=0;
                    y++;
                }
            }
        }
        
        ScrollPane sp = new ScrollPane();
        sp.setContent(gridPane);
        
        /*StackPane rootp = new StackPane();
        rootp.getChildren().add(sp);*/
        sp.addEventHandler(KeyEvent.KEY_PRESSED, (key) -> {
                if(null!=key.getCode()) switch (key.getCode()) {
                    case ENTER:
                        ArrayList<HImageInfo> set = new ArrayList<HImageInfo>();
                        for(ImageFrame frame : iFrames)
                        {
                            if(frame.isSelected())
                                set.add(frame.getImage());
                        }
                        Gallery gal = new Gallery(db);
                        gal.addImages(set);
            
                        Map<String, Object> galParams = new HashMap<>();
                        galParams.put("gallery", gal);
                        UIView gw = HLibrary.getUIManager().buildFromFactory("GalleryViewer", galParams, false);
                        gw.show();
                        break;
                }
        });
        this.setScene(new Scene(sp, 1250, 700));
    }
    
    private class ImageFrame extends StackPane
    {
        private HImageInfo img;
        private ImageView imv;
        private Rectangle border;
        private boolean selected = false;
        
        public ImageFrame(HImageInfo im)
        {
            super();
            img = im;
            
            //------------------------------
            //BufferedImage newImage = null;
            /*try
            {
                BufferedImage otherImage = ImageIO.read(new File(img.getFullPath()));
                newImage = new BufferedImage(RESOLUTION*2, RESOLUTION*2, BufferedImage.TYPE_BYTE_GRAY);

                Graphics g = newImage.createGraphics();
                g.drawImage(otherImage, 0, 0, RESOLUTION*2, RESOLUTION*2, null);
                g.dispose();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }*/
            //--------------------------------
            
            //Image image = SwingFXUtils.toFXImage(newImage, null);
            Image image = new Image(new File(img.getFullPath()).toURI().toString(), THUMB_SIZE, THUMB_SIZE, true, false, true);
            imv = new ImageView();
            imv.setImage(image);
            //TODO rotate the image instead?
            
            if(img.hasTag("vertical"))
            {
                imv.setRotate(90);
            }
            imv.setFitHeight(THUMB_SIZE);
            imv.setFitWidth(THUMB_SIZE);
            imv.setPreserveRatio(true);
            int size = THUMB_SIZE+4;
            this.setPrefHeight(size);
            this.setPrefWidth(size);
            border = new Rectangle(0, 0, size, size);
            border.setFill(Color.TRANSPARENT);
            border.setStroke(Color.BLUE);
            border.setStrokeWidth(4);
            border.setVisible(false);
            this.getChildren().addAll(border, imv);
            imv.setOnMouseClicked(new EventHandler<MouseEvent>()
            {
                @Override
                public void handle(MouseEvent t)
                {
                    setSelected(!selected);
                }
            });
        }
        
        public void setSelected(boolean s)
        {
            border.setVisible(s);
            selected = s;
        }
        
        public boolean isSelected()
        {
            return selected;
        }
        
        public HImageInfo getImage()
        {
            return img;
        }
    }
}
