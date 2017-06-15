package com.nquisition.hlibrary.fxutil;

import java.util.HashMap;
import java.util.Map;

import javafx.scene.paint.Color;

public class ColorMap
{
	private Map<String, Color> map;
	
	public ColorMap()
	{
		map = new HashMap<>();
	}
	
	public ColorMap(Color defaultColor)
	{
		this();
		if(defaultColor != null)
			this.setDefaultColor(defaultColor);
	}
	
	public ColorMap(Color defaultColor, Color firstColor, Color lastColor,
			Color evenColor, Color oddColor)
	{
		this(defaultColor);
		if(firstColor != null)
			this.setFirstColor(firstColor);
		if(lastColor != null)
			this.setLastColor(lastColor);
		if(evenColor != null)
			this.setEvenColor(evenColor);
		if(oddColor != null)
			this.setOddColor(oddColor);
	}
	
	public ColorMap(Color defaultColor, Color firstColor, Color lastColor)
	{
		this(defaultColor,firstColor, lastColor, null, null);
	}
	
	public void setColor(String key, Color col)
	{
		map.put(key, col);
	}
	
	public void setColor(int key, Color col)
	{
		map.put(String.valueOf(key), col);
	}
	
	public void setDefaultColor(Color col)
	{
		map.put("d", col);
	}
	
	public void setFirstColor(Color col)
	{
		map.put("0", col);
	}
	
	public void setLastColor(Color col)
	{
		map.put("l", col);
	}
	
	public void setEvenColor(Color col)
	{
		map.put("e", col);
	}
	
	public void setOddColor(Color col)
	{
		map.put("o", col);
	}
	
	public Color getColor(String key)
	{
		return map.get(key);
	}
	
	public Color getColorForIndex(int index)
	{
		if(map.containsKey(String.valueOf(index)))
			return map.get(String.valueOf(index));
		if(index%2 == 0 && map.containsKey("e"))
			return map.get("e");
		if(index%2 == 1 && map.containsKey("o"))
			return map.get("o");
		
		return map.get("d");
	}
	
	public Color getColorForIndex(int index, int lastIndex)
	{
		if(index == lastIndex && map.containsKey("l"))
			return map.get("l");
		return getColorForIndex(index);
	}
}
