package com.nquisition.hlibrary.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.nquisition.hlibrary.HLibrary;
import com.nquisition.hlibrary.api.IGImage;
import com.nquisition.hlibrary.api.UIHelper;
import com.nquisition.hlibrary.api.UIManager;
import com.nquisition.hlibrary.api.UIView;
import com.nquisition.hlibrary.model.DatabaseInterface;
import com.nquisition.hlibrary.model.GImage;
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
	public void showImagesSatisfyingConditions(List<Predicate<IGImage>> conditions) {
		Gallery gal = new Gallery(dbInterface.getActiveDatabase());
        List<GImage> end = new ArrayList<>();
        List<GImage> start = new ArrayList<>();
        
        for(GImage img : dbInterface.getImagesSatisfyingConditions(conditions)) {
            if(img.hasTag("horizontal")) {
                start.add(img);
            } else if(img.hasTag("vertical")) {
                end.add(img);
            }
        }
        
        for(GImage img : end)
            start.add(img);
        
        gal.addImages(start);

        if(start.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setContentText("No images that match tags found!");
            alert.showAndWait();
        } else {
        	Map<String, Object> galParams = new HashMap<>();
            galParams.put("gallery", gal);
            UIView gw = manager.buildFromFactory("GalleryViewer", galParams, false);
            gw.show();
        }
	}

}
