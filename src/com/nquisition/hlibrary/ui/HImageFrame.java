package com.nquisition.hlibrary.ui;

import java.io.File;

import com.nquisition.hlibrary.model.GImage;

import javafx.event.EventHandler;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class HImageFrame extends StackPane
{
	private int thumbSize;
	private int galPos;
    private GImage img;
    private ImageView imv;
    private Rectangle border;
    
    public HImageFrame(GImage im, int galPos, int thumbSize) {
        super();
        img = im;
        this.galPos = galPos;
        this.thumbSize = thumbSize;
        
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
        Image image = new Image(new File(img.getFullPath()).toURI().toString(), thumbSize, thumbSize, true, false, true);
        imv = new ImageView();
        imv.setImage(image);
        //TODO rotate the image instead?
        
        if(img.hasTag("vertical"))
        {
            imv.setRotate(90);
        }
        imv.setFitHeight(thumbSize);
        imv.setFitWidth(thumbSize);
        imv.setPreserveRatio(true);
        int size = thumbSize+4;
        this.setPrefHeight(size);
        this.setPrefWidth(size);
        border = new Rectangle(0, 0, size, size);
        border.setFill(Color.TRANSPARENT);
        border.setStroke(Color.BLUE);
        border.setStrokeWidth(4);
        border.setVisible(false);
        this.getChildren().addAll(border, imv);
    }
    
    public void setOnClick(EventHandler<MouseEvent> handler) {
    	imv.setOnMouseClicked(handler);
    }
    
    public void setBorderVisible(boolean visible) {
    	border.setVisible(visible);
    }
    
    public GImage getImage() {
        return img;
    }
    
    public int getGalPos() {
    	return galPos;
    }
}