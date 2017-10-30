/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nquisition.hlibrary.model;

import java.io.File;
import java.util.*;
import java.util.List;
import java.io.*;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.*;
import javafx.scene.image.Image;

import javax.imageio.*;
import java.awt.image.*;
import java.awt.*;
import java.awt.geom.*;
import uk.co.jaimon.test.SimpleImageInfo;
import com.idrsolutions.image.png.*;
import com.nquisition.hlibrary.api.IGImage;
import com.nquisition.util.FileUtils;

/**
 *
 * @author Master
 */
public class GImage extends GEntry implements IGImage
{
    private static int NEXTID = 0;
    private int id;
    private String name;
    private transient javafx.scene.image.Image img = null;
    private transient GFolder parent;
    //private ArrayList<String> tags = new ArrayList<String>();
    private transient GImageList list = null;
    //FIXME quick and dirty fix, mb serialize GImageList's instead?
    private Integer listId = null, listPos = null;
    private String similarityString = null;
    private transient byte[] similarityBytes;
    
    private transient double whiteness;
    
    public static final transient int RESOLUTION = 16;
    public static final transient String COMMENT_SEPARATOR = ">>";
    
    public String toString()
    {
        String res = "[";
        for(String tag : this.getTags())
            res += tag + " ";
        res = res.trim() + "] " + name;
        return res;
    }
    
    public GImage()
    {
    	this.resetTags();
        name = "";
        parent = null;
        id = -1;
    }
    
    public GImage(String p)
    {
        this.resetTags();
        name = p;
        parent = null;
        id = NEXTID;
        NEXTID++;
    }
    
    public GImage(String n, GFolder p)
    {
        this.resetTags();
        name = n;
        parent = p;
        id = NEXTID;
        NEXTID++;
    }
    
    public GImage(String n, GFolder p, int id)
    {
        this.resetTags();
        name = n;
        parent = p;
        this.id = id;
        NEXTID = NEXTID>id?NEXTID:id+1;
    }
    
    public boolean isLoaded()
    {
        return img != null;
    }
    
    public void load()
    {
        File file;
        if(parent == null)
            file = new File(name);
        else
            file = new File(parent.getPath() + name);
        img = new javafx.scene.image.Image(file.toURI().toString());
    }
    
    public javafx.scene.image.Image cload()
    {
        if(!isLoaded())
            load();
        return img;
    }
    
    public void unload()
    {
        img = null;
    }
    
    public int getID()
    {
        return id;
    }
    
    public String getName()
    {
        return name;
    }
    
    public String getFullPath()
    {
        if(parent == null)
            return name;
        return parent.getPath() + name;
    }
    
    public GFolder getParent()
    {
        return parent;
    }
    
    public GFolder getTopLevelParent()
    {
    	return parent==null?null:parent.getTopLevelParent();
    }
    
    public boolean isOnTopLevel() {
    	return (parent == this.getTopLevelParent());
    }
    
    public void clearParent()
    {
        parent = null;
    }
    
    public void setName(String p)
    {
        name = p;
    }
    
    @Deprecated
    public String getString()
    {
        //id -> name -> listid -> listpos -> added -> viewed -> lastmod -> viewcount -> comment (>>) -> tags
        String separator = Database.DATA_SEPARATOR;
        StringBuilder res = new StringBuilder();
        res.append(Integer.toString(id) + separator + name);
        res.append(separator + ((list==null)?"-1":list.getID()));
        res.append(separator + ((list==null)?"-1":list.locate(this)));
        res.append(separator + this.getAdded());
        res.append(separator + this.getViewed());
        res.append(separator + this.getLastmod());
        res.append(separator + this.getViewcount());
        res.append(separator + this.getComment() + ">>");
        for(String tag : this.getTags())
        	res.append(separator + tag);
        res.append(separator);
        return res.toString();
    }
    
    public static GImage create(GFolder f, String name, boolean checkFileExists, int id, boolean addedNow)
    {
        if(checkFileExists)
        {
            List<GImage> images = f.getImages();
            for(GImage gi : images)
                if(gi.getName().equalsIgnoreCase(name))
                    return null;
        }
        GImage img;
        if(id <= -1)
            img = new GImage(name, f);
        else
            img = new GImage(name, f, id);
        if(addedNow)
            img.setAddedNow();
        f.addImage(img);
        return img;
    }
    
    @Deprecated
    public static GImage fromString(GFolder f, String str, boolean checkFileExists, Database db)
    {
        String line = str;
        String separator = Database.DATA_SEPARATOR;
        int sepLength = separator.length();
        
        //id -> name -> listid -> listpos -> added -> viewed -> lastmod -> viewcount -> comment (>>) -> tags
        int rId = Integer.parseInt(line.substring(0, line.indexOf(separator)));
        line = line.substring(line.indexOf(separator)+sepLength);
        String rName = line.substring(0, line.indexOf(separator));
        line = line.substring(line.indexOf(separator)+sepLength);
        int rListid = Integer.parseInt(line.substring(0, line.indexOf(separator)));
        line = line.substring(line.indexOf(separator)+sepLength);
        int rListpos = Integer.parseInt(line.substring(0, line.indexOf(separator)));
        line = line.substring(line.indexOf(separator)+sepLength);

        long rAdded = Long.parseLong(line.substring(0, line.indexOf(separator)));
        line = line.substring(line.indexOf(separator)+sepLength);
        long rViewed = Long.parseLong(line.substring(0, line.indexOf(separator)));
        line = line.substring(line.indexOf(separator)+sepLength);
        long rLastmod = Long.parseLong(line.substring(0, line.indexOf(separator)));
        line = line.substring(line.indexOf(separator)+sepLength);
        int rViewcount = Integer.parseInt(line.substring(0, line.indexOf(separator)));
        line = line.substring(line.indexOf(separator)+sepLength);
        String rComment = line.substring(0, line.indexOf(GImage.COMMENT_SEPARATOR));
        line = line.substring(line.indexOf(GImage.COMMENT_SEPARATOR)+GImage.COMMENT_SEPARATOR.length()+sepLength);
        
        GImage img = GImage.create(f, rName, checkFileExists, rId, false);
        if(rListid == -1)
            img.setList(null);
        else
        {
            GImageList l = db.getAddList(rListid);
            l.setImage(rListpos, img);
        }
        img.setAdded(rAdded);
        if(rViewed >= 0)
            img.setViewed(rViewed);
        if(rLastmod >= 0)
            img.setLastmod(rLastmod);
        img.setViewcount(rViewcount);
        if(rComment != null && !rComment.equals(""))
            img.setComment(rComment);
            
        String str1 = line.replace(separator, " ").trim();
        List<String> tagArray = new ArrayList<>();
        if(!str1.equalsIgnoreCase(""))
            tagArray = db.processTags(str1.split("\\s+"));
        
        img.setTags(tagArray, true);
        
        return img;
    }
    
    public int detachFromList()
    {
        if(list == null)
            return 1;
        return list.detachImage(this);
    }
    
    public boolean nameFolderContains(List<String> tgs)
    {
        for(String t : tgs)
            if(!this.nameFolderContains(t))
                return false;
        return true;
    }
    
    public boolean nameFolderContains(String s)
    {
        return this.getFullPath().toLowerCase().contains(s.toLowerCase());
    }
    
    public Byte getSimilarityByte(int i)
    {
        if(similarityBytes == null)
            return null;
        return similarityBytes[i];
    }
    
    public void setSimilarityBytes(Byte[] bytes) {
    	if(bytes == null) {
    		similarityBytes = null;
    		similarityString = null;
    		return;
    	}
    	similarityBytes = new byte[bytes.length];
    	for(int i = 0; i < bytes.length; i++)
    		similarityBytes[i] = bytes[i];
    	computeSimilarityString();
    }
    
    public boolean isSimilarityBytesComputed() {
    	return !(similarityBytes == null || similarityBytes.length != RESOLUTION*RESOLUTION);
    }
    
    public int differenceFrom(GImage img, int threshold, boolean orientation)
    {
        int res = 0;
        if(similarityBytes == null)
            return -1;
        for(int i = 0; i < RESOLUTION*RESOLUTION; i++)
        {
            Byte b;
            if((b = img.getSimilarityByte(i)) == null)
                return -1;
            int b1 = (int)(b.byteValue())&0xFF;
            int b2 = (int)(similarityBytes[i])&0xFF;
            res += Math.abs(b1 - b2);
            if(threshold > 0 && res > threshold)
                break;
        }
        
        if(res == 0 || !orientation)
            return res;
        
        int res1 = 0;
        int res2 = 0;
        for(int i = 0; i < RESOLUTION; i++)
            for(int j = 0; j < RESOLUTION; j++)
            {
                Byte b = img.getSimilarityByte((RESOLUTION-j-1)*RESOLUTION + i);
                int b1 = (int)(b.byteValue())&0xFF;
                b = img.getSimilarityByte(j*RESOLUTION + RESOLUTION-i-1);
                int b2 = (int)(b.byteValue())&0xFF;
                int bb = (int)(similarityBytes[i*RESOLUTION + j])&0xFF;
                res1 += Math.abs(b1 - bb);
                res2 += Math.abs(b2 - bb);
                if(threshold > 0 && res1 > threshold && res2 > threshold)
                    break;
            }
        return Math.min(res, Math.min(res1, res2));
    }
    
    @Override
	public boolean computeSimilarity(boolean forceRecompute) throws IOException
    {
    	if((similarityBytes == null || similarityBytes.length != RESOLUTION*RESOLUTION) && similarityString != null) {
    		return similarityFromString(similarityString);
    	}
    	if(!forceRecompute && similarityBytes != null && similarityBytes.length == RESOLUTION*RESOLUTION)
    		return true;
    	
        try
        {
            similarityBytes = new byte[RESOLUTION*RESOLUTION];
            BufferedImage otherImage = ImageIO.read(new File(this.getFullPath()));
            BufferedImage newImage = new BufferedImage(RESOLUTION, RESOLUTION, BufferedImage.TYPE_BYTE_GRAY);

            Graphics g = newImage.createGraphics();
            g.drawImage(otherImage, 0, 0, RESOLUTION, RESOLUTION, null);
            g.dispose();
            int count = 0;
            for(int j = 0; j < newImage.getHeight(); j++)
                for(int i = 0; i < newImage.getWidth(); i++)
                {
                    
                    byte gray = (byte)(newImage.getRGB(i, j)&0xFF);
                    if(gray == 0)
                        count++;
                    similarityBytes[j*RESOLUTION + i] = gray;
                }
        }
        catch(IIOException e)
        {
            System.out.println("Image " + this.getFullPath() + " cannot be loaded"
                    + "or has an unsupported format.");
            similarityBytes = null;
            return false;
        }
        computeSimilarityString();
        return true;
    }
    
    public void computeSimilarityString() {
    	StringBuilder bld = new StringBuilder();
    	similarityString = "";
    	for(int i = 0; i < similarityBytes.length; i++)
    		bld.append((char)(similarityBytes[i]));
    	similarityString = bld.toString();
    }
    
    public boolean similarityFromString(String similarity) {
    	if(similarity.length() != RESOLUTION*RESOLUTION) {
    		System.out.println("Error! " + this.getFullPath() + " Expected length " + RESOLUTION*RESOLUTION +
    				", got " + similarity.length());
    		return false;
    	}
    	similarityString = similarity;
    	similarityBytes = new byte[RESOLUTION*RESOLUTION];
    	for(int i = 0; i < similarity.length(); i++) {
    		similarityBytes[i] = (byte)(similarity.charAt(i));
    	}
    	return true;
    }
    
    public String getSimilarityString() {
    	return similarityString;
    }
    
    public void computeWhiteness() {
    	int sum = 0;
    	for(int i = 0; i < RESOLUTION*RESOLUTION; i++) {
            sum += (int)(similarityBytes[i])&0xFF;
        }
    	whiteness = (double)sum/(RESOLUTION*RESOLUTION)/255.0d;
    }
    
    public double getWhiteness() {
    	return whiteness;
    }
    
    public void rotate(boolean left, int sleepTime)
    {
        String cname = FileUtils.renameImage(parent==null?"":parent.getPath(), name, left);
        setName(cname);
        
        int res = FileUtils.rotateImage(parent==null?"":parent.getPath(), name, left, sleepTime);
        
        if(res == -1)
        {
            System.out.println("IllegalArgumentException : Problem rotating file " + getFullPath());
        }
        else if(res == -2)
        {
            System.out.println("IOException : Problem rotating file " + getFullPath());
        }
        else if(res == -3)
        {
            System.out.println("InterruptedException : Problem rotating file " + getFullPath());
        }
    }
    
    //TODO no good? But needed atm
    public void setId(int id)
    {
    	this.id = id;
    	NEXTID = NEXTID>id?NEXTID:id+1;
    }
    
    public void setParent(GFolder parent)
    {
    	this.parent = parent;
    }
    
    public void setList(GImageList l)
    {
        list = l;
    }
    
    public GImageList getList()
    {
        return list;
    }
    
    public Image getSimilarityImage(int width, int height) throws IOException {
    	if(similarityBytes == null || similarityBytes.length != RESOLUTION*RESOLUTION)
    		this.computeSimilarity(true);
    	BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
    	
    	// Get the backing pixels, and copy into it
    	byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
    	for(int i = 0; i < data.length; i++) {
    		int y = Math.floorDiv(i, width);
    		int x = i%width;
    		int xres = (int)Math.ceil((double)width/(double)RESOLUTION);
    		int yres = (int)Math.ceil((double)height/(double)RESOLUTION);
    		data[i] = similarityBytes[Math.floorDiv(x, xres) + Math.floorDiv(y, yres) * RESOLUTION];
    	}
    	return SwingFXUtils.toFXImage(image, null);
    }
}
