package com.nquisition.fxutil;

import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class MultiColoredText extends TextFlow
{
	//TODO allow adding more pieces, so prob change to List?
	private Text[] pieces;
	
	public MultiColoredText(Font font, Color... colors)
	{
		super();
		if(colors == null || colors.length == 0)
			return;
		pieces = new Text[colors.length];
		for(int i = 0; i < pieces.length; i++)
		{
			pieces[i] = new Text("");
			pieces[i].setFont(font);
			pieces[i].setFill(colors[i]);
		}
		this.getChildren().addAll(pieces);
	}
	
	public MultiColoredText(int num, Font font, ColorMap colors)
	{
		super();
		if(colors == null || num <= 0)
			return;
		pieces = new Text[num];
		for(int i = 0; i < pieces.length; i++)
		{
			pieces[i] = new Text("");
			pieces[i].setFont(font);
			pieces[i].setFill(colors.getColorForIndex(i, pieces.length-1));
		}
		this.getChildren().addAll(pieces);
	}
	
	public void setText(String... text)
	{
		if(text == null || pieces == null)
			return;
		int len = Math.min(text.length, pieces.length);
		for(int i = 0; i < len; i++)
		{
			//TODO have to do this because otherwise the empty Text will "steal" the
			//first character of the next Text and paint it in it's color
			if(text[i] == null || text[i].equals(""))
			{
				if(pieces[i].isManaged())
				{
					pieces[i].setManaged(false);
					pieces[i].setVisible(false);
				}
			}
			else
			{
				pieces[i].setText(text[i]);
				if(!pieces[i].isManaged())
				{
					pieces[i].setManaged(true);
					pieces[i].setVisible(true);
				}
			}
		}
	}
}
