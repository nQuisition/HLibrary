/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nquisition.hlibrary.model;

import java.io.File;
import java.util.*;
import java.io.*;
import javafx.scene.image.*;
import javax.imageio.*;
import java.awt.image.*;
import java.awt.*;
import java.awt.geom.*;
import uk.co.jaimon.test.SimpleImageInfo;
import com.idrsolutions.image.png.*;
import com.nquisition.util.FileUtils;

/**
 *
 * @author Master
 */
public class GImage extends GEntry
{
    private static int NEXTID = 0;
    private int id;
    private String name;
    private javafx.scene.image.Image img = null;
    private GFolder parent;
    //private ArrayList<String> tags = new ArrayList<String>();
    private GImageList list = null;
    private byte[] similarityString;
    
    public static final int RESOLUTION = 8;
    public static final String COMMENT_SEPARATOR = ">>";
    
    public String toString()
    {
        String res = "[";
        for(String tag : this.getTags())
            res += tag + " ";
        res = res.trim() + "] " + name;
        return res;
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
        return img == null?false:true;
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
    
    public boolean isOnTopLevel()
    {
    	if(parent == this.getTopLevelParent())
    		return true;
    	return false;
    }
    
    public void clearParent()
    {
        parent = null;
    }
    
    public void setName(String p)
    {
        name = p;
    }
    
    public String getString()
    {
        //id -> name -> listid -> listpos -> added -> viewed -> lastmod -> viewcount -> comment (>>) -> tags
        String separator = Database.DATA_SEPARATOR;
        String res = "" + id + separator + name;
        res += separator + ((list==null)?"-1":list.getID());
        res += separator + ((list==null)?"-1":list.locate(this));
        res += separator + this.getAdded();
        res += separator + this.getViewed();
        res += separator + this.getLastmod();
        res += separator + this.getViewcount();
        res += separator + this.getComment() + ">>";
        for(String tag : this.getTags())
            res += separator + tag;
        res += separator;
        return res;
    }
    
    public static GImage create(GFolder f, String name, boolean checkFileExists, int id, boolean addedNow)
    {
        if(checkFileExists)
        {
            ArrayList<GImage> images = f.getImages();
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
        //images.add(img);
        f.addImage(img);
        return img;
    }
    
    public static GImage fromString(GFolder f, String str, boolean checkFileExists, Database db)
    {
        String line = str;
        String separator = Database.DATA_SEPARATOR;
        int sepLength = separator.length();
        
        //id -> name -> listid -> listpos -> added -> viewed -> lastmod -> viewcount -> comment (>>) -> tags
        int r_id = Integer.valueOf(line.substring(0, line.indexOf(separator)));
        line = line.substring(line.indexOf(separator)+sepLength);
        String r_name = line.substring(0, line.indexOf(separator));
        line = line.substring(line.indexOf(separator)+sepLength);
        int r_listid = Integer.valueOf(line.substring(0, line.indexOf(separator)));
        line = line.substring(line.indexOf(separator)+sepLength);
        int r_listpos = Integer.valueOf(line.substring(0, line.indexOf(separator)));
        line = line.substring(line.indexOf(separator)+sepLength);

        long r_added = Long.valueOf(line.substring(0, line.indexOf(separator)));
        line = line.substring(line.indexOf(separator)+sepLength);
        long r_viewed = Long.valueOf(line.substring(0, line.indexOf(separator)));
        line = line.substring(line.indexOf(separator)+sepLength);
        long r_lastmod = Long.valueOf(line.substring(0, line.indexOf(separator)));
        line = line.substring(line.indexOf(separator)+sepLength);
        int r_viewcount = Integer.valueOf(line.substring(0, line.indexOf(separator)));
        line = line.substring(line.indexOf(separator)+sepLength);
        String r_comment = line.substring(0, line.indexOf(GImage.COMMENT_SEPARATOR));
        line = line.substring(line.indexOf(GImage.COMMENT_SEPARATOR)+GImage.COMMENT_SEPARATOR.length()+sepLength);
        
        GImage img = GImage.create(f, r_name, checkFileExists, r_id, false);
        if(r_listid == -1)
            img.setList(null);
        else
        {
            GImageList l = db.getAddList(r_listid);
            l.setImage(r_listpos, img);
        }
        img.setAdded(r_added);
        if(r_viewed >= 0)
            img.setViewed(r_viewed);
        if(r_lastmod >= 0)
            img.setLastmod(r_lastmod);
        img.setViewcount(r_viewcount);
        if(r_comment != null && !r_comment.equals(""));
            img.setComment(r_comment);
            
        String str1 = line.replace(separator, " ").trim();
        ArrayList<String> tagArray = new ArrayList<String>();
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
    
    public boolean nameFolderContains(ArrayList<String> tgs)
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
    
    public Byte getSimilarityStringByte(int i)
    {
        if(similarityString == null)
            return null;
        return similarityString[i];
    }
    
    public int differenceFrom(GImage img, int threshold, boolean orientation)
    {
        int res = 0;
        if(similarityString == null)
            return -1;
        for(int i = 0; i < RESOLUTION*RESOLUTION; i++)
        {
            Byte b;
            if((b = img.getSimilarityStringByte(i)) == null)
                return -1;
            int b1 = (int)(b.byteValue())&0xFF;
            int b2 = (int)(similarityString[i])&0xFF;
            res += Math.abs(b1 - b2);
            if(res > threshold)
                break;
        }
        
        if(res == 0 || !orientation)
            return res;
        
        int res1 = 0;
        int res2 = 0;
        for(int i = 0; i < RESOLUTION; i++)
            for(int j = 0; j < RESOLUTION; j++)
            {
                Byte b = img.getSimilarityStringByte((RESOLUTION-j-1)*RESOLUTION + i);
                int b1 = (int)(b.byteValue())&0xFF;
                b = img.getSimilarityStringByte(j*RESOLUTION + RESOLUTION-i-1);
                int b2 = (int)(b.byteValue())&0xFF;
                int bb = (int)(similarityString[i*RESOLUTION + j])&0xFF;
                res1 += Math.abs(b1 - bb);
                res2 += Math.abs(b2 - bb);
                if(res1 > threshold && res2 > threshold)
                    break;
            }
        return Math.min(res, Math.min(res1, res2));
    }
    
    public boolean computeSimilarityString() throws IOException
    {
        try
        {
            similarityString = new byte[RESOLUTION*RESOLUTION];
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
                    if(j == 0)
                        System.out.print(gray + "::" + (newImage.getRGB(i, j)&0xFF) + " ");
                    if(gray == 0)
                        count++;
                    similarityString[j*RESOLUTION + i] = gray;
                }
            /*for(int i = 0; i < RESOLUTION; i++)
                System.out.print(similarityString[i] + " ");*/
            System.out.println(this.getFullPath());
        }
        catch(IIOException e)
        {
            System.out.println("Image " + this.getFullPath() + " cannot be loaded"
                    + "or has an unsupported format.");
            similarityString = null;
            return false;
        }
        return true;
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
    
    public void setList(GImageList l)
    {
        list = l;
    }
    
    public GImageList getList()
    {
        return list;
    }
}
