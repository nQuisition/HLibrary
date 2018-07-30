package com.nquisition.hlibrary.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.nquisition.hlibrary.model.Database;
import com.nquisition.hlibrary.model.HFolderInfo;
import com.nquisition.hlibrary.model.HImageInfo;
import com.nquisition.hlibrary.model.Gallery;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class SimilarityViewer extends HConsoleStage
{
    private Database db;
    private Label counter, rightCounter;
    private ImageView original, similar;
    private ImageView originalHistogram, similarHistogram;
    private Label originalName, similarName;
    private Label similarityLabel;
    private Gallery gallery;
    //private List<GImage> left, right;
    private Map<HImageInfo, List<HImageInfo>> images;
    private List<HImageInfo> indices; 
    private int pos = -1;
    private int rightPos = 0;
    
    public static final int IMAGE_SIZE = 400;
    
    //TODO needs onClose operation/cleanup
    public SimilarityViewer(Database d, Map<HImageInfo, List<HImageInfo>> map)
    {
        super();
        db = d;
        /*left = new ArrayList<>();
        right = new ArrayList<>();
        for(GImage key : map.keySet()) {
        	int mindiff = 1000000;
        	GImage value = null;
        	for(GImage img : map.get(key)) {
        		if(value == null) {
        			value = img;
        			mindiff = key.differenceFrom(img, Integer.MAX_VALUE, true);
        		} else {
        			int diff = key.differenceFrom(img, Integer.MAX_VALUE, true);
        			if(diff < mindiff) { 
        				diff = mindiff;
        				value = img;
        			}
        		}
        	}
        	left.add(key);
        	right.add(value);
        }*/
        images = map;
        indices = new ArrayList<>(map.keySet());
        
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setPadding(new Insets(20));
        
        counter = new Label();
        rightCounter = new Label();
        gridPane.add(counter, 0, 0);
        gridPane.add(rightCounter, 1, 0);
        
        original = new ImageView();
        original.setFitWidth(IMAGE_SIZE);
        original.setFitHeight(IMAGE_SIZE);
        original.setPreserveRatio(true);
        HBox box1 = new HBox();
        box1.setPrefWidth(IMAGE_SIZE + 200);
        box1.setPrefHeight(IMAGE_SIZE);
        box1.getChildren().add(original);
        
        similar = new ImageView();
        similar.setFitWidth(IMAGE_SIZE);
        similar.setFitHeight(IMAGE_SIZE);
        similar.setPreserveRatio(true);
        HBox box2 = new HBox();
        box2.setPrefWidth(IMAGE_SIZE + 200);
        box2.setPrefHeight(IMAGE_SIZE);
        box2.getChildren().add(similar);

        gridPane.add(box1, 0, 1);
        gridPane.add(box2, 1, 1);
        
        originalHistogram = new ImageView();
        originalHistogram.setFitWidth(IMAGE_SIZE);
        originalHistogram.setFitHeight(IMAGE_SIZE);
        originalHistogram.setPreserveRatio(false);
        HBox box1h = new HBox();
        box1h.setPrefWidth(IMAGE_SIZE);
        box1h.setPrefHeight(IMAGE_SIZE);
        box1h.getChildren().add(originalHistogram);
        
        similarHistogram = new ImageView();
        similarHistogram.setFitWidth(IMAGE_SIZE);
        similarHistogram.setFitHeight(IMAGE_SIZE);
        similarHistogram.setPreserveRatio(false);
        HBox box2h = new HBox();
        box2h.setPrefWidth(IMAGE_SIZE);
        box2h.setPrefHeight(IMAGE_SIZE);
        box2h.getChildren().add(similarHistogram);

        gridPane.add(box1h, 0, 2);
        gridPane.add(box2h, 1, 2);
        
        originalName = new Label();
        similarName = new Label();
        
        gridPane.add(originalName, 0, 3);
        gridPane.add(similarName, 1, 3);
        
        similarityLabel = new Label();
        
        gridPane.add(similarityLabel, 0, 4);
        
        gallery = new Gallery(db);
        gallery.addImages(db.getImages());
        
        nextImage();
        
        Button prevButton = new Button(">>");
        prevButton.setOnAction((ActionEvent e) -> {
            nextImage();
        });
        
        Button nextButton = new Button(">>");
        nextButton.setOnAction((ActionEvent e) -> {
            nextRightImage();
        });
        
        gridPane.add(prevButton, 0, 5);
        gridPane.add(nextButton, 1, 5);
        
        this.setScene(new Scene(gridPane, 1250, 1000));
    }
    
    public void nextImage() {
    	pos++;
    	rightPos = 0;
    	if(pos >= indices.size())
    		pos = 0;
    	setImages();
    }
    
    public void nextRightImage() {
    	rightPos++;
    	if(rightPos >= images.get(indices.get(pos)).size())
    		rightPos = 0;
    	setImages();
    }
    
    public void prevImage() {
    	pos--;
    	if(pos < 0)
    		pos = indices.size()-1;
    	setImages();
    }
    
    public void setImages() {
    	counter.setText((pos+1) + "/" + indices.size());
    	rightCounter.setText((rightPos+1) + "/" + images.get(indices.get(pos)).size());
    	HImageInfo leftImage = indices.get(pos);
    	HImageInfo rightImage = images.get(indices.get(pos)).size()>0?images.get(indices.get(pos)).get(rightPos):null;
    	original.setImage(leftImage.cload());
    	similar.setImage(rightImage!=null?rightImage.cload():null);
    	setSimilarityImage(leftImage, originalHistogram);
    	setSimilarityImage(rightImage, similarHistogram);
    	originalName.setText(leftImage.getFullPath());
    	similarName.setText(rightImage!=null?rightImage.getFullPath():"");
    	int diff = leftImage.differenceFrom(rightImage, -1, false);
    	double ppDiff = diff/(HImageInfo.RESOLUTION*HImageInfo.RESOLUTION); //(leftImage.cload().getWidth()*leftImage.cload().getHeight());
    	similarityLabel.setText("Difference: " + diff + "(" + ppDiff + "pp);\n w1: "
    			+ leftImage.getWhiteness() + ", w2: " + ((rightImage!=null)?rightImage.getWhiteness():""));
    }
    
    private static void setSimilarityImage(HImageInfo gimg, ImageView imv) {
    	try {
    		if(gimg == null) {
    			imv.setImage(null);
    			return;
    		}
        	double width, height;
        	Image img  = gimg.cload();
        	double aspectRatio = img.getWidth()/img.getHeight();
        	if(aspectRatio > 1) {
        		//horizontal
        		width = IMAGE_SIZE;
        		height = IMAGE_SIZE/aspectRatio;
        	} else {
        		width = IMAGE_SIZE*aspectRatio;
        		height = IMAGE_SIZE;
        	}
        	
        	//similarity.setFitWidth(width);
        	//similarity.setFitHeight(height);
        	imv.setImage(gimg.getSimilarityImage((int)width, (int)height));
        } catch(IOException e) {
        	e.printStackTrace();
        }
    }
    
    /*public void nextImage() {
    	Image img = gallery.getNext(false);
    	setImages(img);
    }
    
    public void prevImage() {
    	Image img = gallery.getPrev(false);
        setImages(img);
    }
    
    public void setImages(Image img) {
    	original.setImage(img);
        GImage gimg = gallery.getCurrentGImage();
        try {
        	double width, height;
        	double aspectRatio = img.getWidth()/img.getHeight();
        	if(aspectRatio > 1) {
        		//horizontal
        		width = IMAGE_SIZE;
        		height = IMAGE_SIZE/aspectRatio;
        	} else {
        		width = IMAGE_SIZE*aspectRatio;
        		height = IMAGE_SIZE;
        	}
        	
        	//similarity.setFitWidth(width);
        	//similarity.setFitHeight(height);
        	similarity.setImage(gimg.getSimilarityImage((int)width, (int)height));
        } catch(IOException e) {
        	e.printStackTrace();
        }
        
    }*/
}
