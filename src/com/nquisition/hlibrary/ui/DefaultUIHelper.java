package com.nquisition.hlibrary.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.nquisition.hlibrary.HLibrary;
import com.nquisition.hlibrary.api.ReadOnlyImageInfo;
import com.nquisition.hlibrary.api.UIHelper;
import com.nquisition.hlibrary.api.UIManager;
import com.nquisition.hlibrary.api.UIView;
import com.nquisition.hlibrary.model.DatabaseInterface;
import com.nquisition.hlibrary.model.HImageInfo;
import com.nquisition.hlibrary.model.GImageList;
import com.nquisition.hlibrary.model.Gallery;

import javafx.scene.control.Alert;

public class DefaultUIHelper implements UIHelper {
	private DatabaseInterface dbInterface;
	private UIManager manager;
	
	public DefaultUIHelper(UIManager manager, DatabaseInterface dbInterface) {
		this.manager = manager;
		this.dbInterface = dbInterface;
	}

	@Override
	public void showImagesSatisfyingConditions(List<Predicate<ReadOnlyImageInfo>> imageConditions) {
		Gallery gal = new Gallery(dbInterface.getActiveDatabase());
        List<HImageInfo> end = new ArrayList<>();
        List<HImageInfo> start = new ArrayList<>();
        
        for(HImageInfo img : dbInterface.getActiveImagesSatisfyingConditions(imageConditions)) {
            if(img.hasTag("horizontal")) {
                start.add(img);
            } else if(img.hasTag("vertical")) {
                end.add(img);
            }
        }
        
        for(HImageInfo img : end)
            start.add(img);
        
        gal.addImages(start);

        if(start.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setContentText("No images that match conditions found!");
            alert.showAndWait();
        } else {
        	Map<String, Object> galParams = new HashMap<>();
            galParams.put("gallery", gal);
            UIView gw = manager.buildFromFactory("GalleryViewer", galParams, false);
            gw.show();
        }
	}
	
	public void showImagesWithTags(String tagString) {
		List<String> allowed = new ArrayList<>();
		List<String> restricted = new ArrayList<>();
		
        String[] arr = tagString.trim().split(" ");
        for(int i = 0; i < arr.length; i++) {
            if(arr[i] == null || arr[i].length()<=0)
                continue;
            if(arr[i].charAt(0) == '-')
                restricted.add(arr[i].substring(1));
            else
                allowed.add(arr[i]);
        }
        List<Predicate<ReadOnlyImageInfo>> conditions = new ArrayList<>();
        for(String cond : allowed) {
        	boolean isFolder = false;
        	if(cond.charAt(0) == ':') {
        		isFolder = true;
        		cond = cond.substring(1);
        	}
        	String condFinal = cond;
        	int separatorIndex = cond.indexOf(':');
        	if(separatorIndex > 0) {
        		String propName = cond.substring(0, separatorIndex);
        		String propValue = cond.substring(separatorIndex+1);
        		if(isFolder)
        			conditions.add(image -> (image.getParent() != null &&
        				image.getParent().getProperty(propName) != null && 
        				image.getParent().getProperty(propName).toString().equalsIgnoreCase(propValue)));
        		else
        			conditions.add(image -> (image.getProperty(propName) != null && 
        				image.getProperty(propName).toString().equalsIgnoreCase(propValue)));
        	} else {
        		if(isFolder)
        			conditions.add(image -> image.getParent() != null && image.getParent().hasTag(condFinal));
        		else
        			conditions.add(image -> image.hasTag(condFinal));
        	}
        }
        for(String cond : restricted) {
        	boolean isFolder = false;
        	if(cond.charAt(0) == ':') {
        		isFolder = true;
        		cond = cond.substring(1);
        	}
        	String condFinal = cond;
        	int separatorIndex = cond.indexOf(':');
        	if(separatorIndex > 0) {
        		String propName = cond.substring(0, separatorIndex);
        		String propValue = cond.substring(separatorIndex+1);
        		if(isFolder)
        			conditions.add(image -> !(image.getParent() != null &&
        				image.getParent().getProperty(propName) != null && 
        				image.getParent().getProperty(propName).toString().equalsIgnoreCase(propValue)));
        		else
        			conditions.add(image -> !(image.getProperty(propName) != null && 
        				image.getProperty(propName).toString().equalsIgnoreCase(propValue)));
        	} else if(separatorIndex < 0) {
        		if(isFolder)
        			conditions.add(image -> !(image.getParent() != null && image.getParent().hasTag(condFinal)));
        		else
        			conditions.add(image -> !image.hasTag(condFinal));
        	}
        }
        showImagesSatisfyingConditions(conditions);
	}
}
