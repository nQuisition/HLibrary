package com.nquisition.fxutil;

import com.sun.corba.se.impl.orbutil.graph.Node;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

public class RatingBar
{
	private final int maxRating;
	private final Image fullIcon, emptyIcon, noRatingIcon;
	private final int size;
	
	private HBox container;
	private ImageView[] icons;
	
	private IntegerProperty curRating;
	
	public RatingBar(int maxRating, int size, Image fullIcon, Image emptyIcon, Image noRatingIcon)
	{
		this.maxRating = maxRating;
		this.fullIcon = fullIcon;
		this.emptyIcon = emptyIcon;
		this.noRatingIcon = noRatingIcon;
		this.size = size;
		
		init();
	}
	
	public final void init()
	{
		curRating = new SimpleIntegerProperty(-1);
		curRating.addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
        {
        	revertToCurRating();
        });
		
		icons = new ImageView[maxRating];
		for(int i = 0; i < icons.length; i++)
		{
			icons[i] = new ImageView();
			icons[i].setFitWidth(size);
			icons[i].setFitHeight(size);
			icons[i].setImage(noRatingIcon);
			icons[i].setPickOnBounds(true);
		}
		
		container = new HBox();
		container.setSpacing(0);
		container.setAlignment(Pos.BASELINE_CENTER);
		container.getChildren().addAll(icons);
		//TODO efficient? Can also manage this in icons[i].onMouseEntered
		container.setOnMouseMoved(mevent -> {
			/*int x = (int)mevent.getX();
			x = x + size/2;
			int pos = (int)Math.ceil(x/size);
			if(pos < 0)
				pos = 0;
			if(pos > maxRating)
				pos = maxRating;*/

			int pos = getNumStarAtMouse(mevent);
			for(int i = 0; i < icons.length; i++)
				icons[i].setImage(i < pos ? fullIcon : emptyIcon);
		});
		
		container.setOnMouseExited(mevent -> {
			revertToCurRating();
		});
		
		container.setOnMouseClicked(mevent -> {
			/*int x = (int)mevent.getX();
			x = x + size/2;
			int pos = (int)Math.ceil(x/size);
			if(pos < 0)
				pos = 0;
			if(pos > maxRating)
				pos = maxRating;*/
			
			int pos = getNumStarAtMouse(mevent);
			curRating.set(pos);
		});
	}
	
	public int getNumStarAtMouse(MouseEvent mevent)
	{
		EventTarget target = mevent.getTarget();
		int pos = 0;
		if(target instanceof ImageView)
		{
			ImageView imv = (ImageView)target;
			for(int i = 0; i < icons.length; i++)
				if(icons[i] == imv)
				{
					pos = i+1;
					break;
				}
		}
		else if(mevent.getX() > container.getWidth()/2)
			pos = maxRating;
		return pos;
	}
	
	private void revertToCurRating()
	{
		if(curRating.get() < 0)
			for(int i = 0; i < icons.length; i++)
				icons[i].setImage(noRatingIcon);
		else
			for(int i = 0; i < icons.length; i++)
				icons[i].setImage(i < curRating.get() ? fullIcon : emptyIcon);
	}
	
	public void bindCurRating(IntegerProperty property)
	{
		curRating.bindBidirectional(property);
	}
	
	public void setCurRating(int rating)
	{
		curRating.set(rating);
	}
	
	public int getCurRating()
	{
		return curRating.get();
	}
	
	public Parent getContainer()
	{
		return container;
	}
}
