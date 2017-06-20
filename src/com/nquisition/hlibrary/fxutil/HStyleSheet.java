package com.nquisition.hlibrary.fxutil;

import com.nquisition.fxutil.FXFactory;

import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.Border;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class HStyleSheet
{
	private Color menuBackgroundColor = new Color(0.92, 0.92, 0.92, 1.0);
	private Background menuBackground = FXFactory.createSimpleBackground(menuBackgroundColor);
	private Color menuBorderColor = Color.DARKGRAY;
	private Border menuBorder = FXFactory.createSimpleBorder(menuBorderColor, 2);
	private Stop[] sectionTitleGradientStops = new Stop[] { new Stop(0, Color.WHITE), new Stop(1, Color.LIGHTGRAY) };
	private LinearGradient sectionTitleGradient = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, sectionTitleGradientStops);
	private Background sectionTitleBackground = FXFactory.createSimpleBackground(sectionTitleGradient);

	private Font defaultInfoFont = Font.font("Arial", FontWeight.BOLD, 18);
	private Font defaultMenuFont = Font.getDefault();
	private Font sectionTitleFont = Font.font(Font.getDefault().getFamily(), FontWeight.BOLD, Font.getDefault().getSize());
	
	private Image starFull = new Image("starfull.png");
	private Image starEmpty = new Image("starempty.png");
	private Image starNoRating = new Image("stardisabled.png");
	
	public Background getMenuBackground()
	{
		return menuBackground;
	}

	public Border getMenuBorder()
	{
		return menuBorder;
	}

	public Background getSectionTitleBackground()
	{
		return sectionTitleBackground;
	}

	public Font getDefaultInfoFont()
	{
		return defaultInfoFont;
	}
	
	public Font getDefaultMenuFont()
	{
		return defaultMenuFont;
	}

	public Font getSectionTitleFont()
	{
		return sectionTitleFont;
	}

	public Image getStarFull()
	{
		return starFull;
	}

	public Image getStarEmpty()
	{
		return starEmpty;
	}
	
	public Image getStarNoRating()
	{
		return starNoRating;
	}
}
