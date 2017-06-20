package com.nquisition.fxutil;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;

public class FXFactory
{
	private FXFactory() {}
	
	public static Background createSimpleBackground(Paint paint)
	{
		return new Background(new BackgroundFill(paint, CornerRadii.EMPTY, Insets.EMPTY));
	}
	
	public static Border createSimpleBorder(Color color, int padding)
	{
		return new Border(new BorderStroke(color, BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
				BorderWidths.DEFAULT, new Insets(padding)));
	}
	
	public static Label createLabelVerticalGradient(Font font, int width, Color topColor, Color bottomColor, Insets padding)
	{
		Stop[] stops = new Stop[] {new Stop(0, topColor), new Stop(1, bottomColor)};
		LinearGradient gradient = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, stops);
		
		return createLabelGradient(font, width, gradient, padding);
	}
	
	public static Label createLabelGradient(Font font, int width, LinearGradient gradient, Insets padding)
	{
		return createLabel(font, width, new Background(new BackgroundFill(gradient, CornerRadii.EMPTY, Insets.EMPTY)), padding);
	}
	
	public static Label createLabel(Font font, int width, Background background, Insets padding)
	{
		Label res = new Label();
		res.setFont(font);
		if(width > 0)
			res.setMaxWidth(width);
		else
			res.setMaxWidth(Double.MAX_VALUE);
		if(background != null)
			res.setBackground(background);
		res.setPadding(padding);
		
		return res;
	}
	
	public static CheckBox createUnfocusedCheckBox(String label)
	{
		CheckBox res = new CheckBox(label) { public void requestFocus() {} };
		return res;
	}
	
	public static Button createUnboundedButton(String label)
	{
		Button res = new Button(label);
		res.setMaxWidth(Double.MAX_VALUE);
		return res;
	}
}
