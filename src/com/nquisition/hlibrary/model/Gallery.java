/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nquisition.hlibrary.model;

import java.util.*;

import org.apache.logging.log4j.core.util.NameUtil;

import java.io.*;
import javafx.scene.image.*;
import java.text.*;

/**
 *
 * @author Master
 */
public class Gallery
{
    private List<GImage> images;
    private int curimg = -1;
    private int numcache = 2;
    private List<GImage> cached = new ArrayList<>();
    
    public static final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yy HH:mm:ss");
    
    private Database db;
    
    public Gallery(Database d)
    {
        images = new ArrayList<>();
        db = d;
    }
    
    public void addImages(List<GImage> imgs)
    {
        for(GImage img : imgs)
            images.add(img);
    }
    
    private int nextIndexWithTag(int startPos, String tag, boolean includeCurrent)
    {
        int start = startPos+1;
        if(includeCurrent)
            start = startPos;
        for(int i = start; i < images.size(); i++)
            if(images.get(i).hasTag(tag))
                return i;
        for(int i = 0; i < startPos; i++)
            if(images.get(i).hasTag(tag))
                return i;
        return startPos;
    }
    
    private int prevIndexWithTag(int startPos, String tag, boolean includeCurrent)
    {
        int start = startPos-1;
        if(includeCurrent)
            start = startPos;
        for(int i = start; i >= 0; i--)
            if(images.get(i).hasTag(tag))
                return i;
        for(int i = images.size()-1; i > startPos; i--)
            if(images.get(i).hasTag(tag))
                return i;
        return startPos;
    }
    
    public Image getByName(String fname)
    {
        curimg = 0;
        for(int i = 0; i < images.size(); i++)
            if(images.get(i).getName().equalsIgnoreCase(fname))
            {
                curimg = i;
                break;
            }
        return getCurrent();
    }
    
    public Image getNext(boolean fav)
    {
        if(images.size() <= 0)
            return null;
        if(images.size() <= 0)
            return null;
        if(fav)
            curimg = this.nextIndexWithTag(curimg, "fav", false);
        else
        {
            curimg++;
            if(curimg >= images.size())
                curimg = 0;
        }
        return getCurrent();
    }
    
    public Image getPrev(boolean fav)
    {
        if(images.size() <= 0)
            return null;
        if(fav)
            curimg = this.prevIndexWithTag(curimg, "fav", false);
        else
        {
            curimg--;
            if(curimg < 0)
                curimg = images.size()-1;
        }
        return getCurrent();
    }
    
    public Image jump(int num)
    {
        if(images.size() <= 0)
            return null;
        curimg += num;
        while(curimg < 0)
            curimg = curimg + images.size();
        while(curimg >= images.size())
            curimg = curimg - images.size();
        return getCurrent();
    }
    
    public Image jumpFolder(boolean forward, boolean fav)
    {
        if(images.size() <= 0)
            return null;
        int ci = curimg;
        GFolder f = images.get(ci).getParent();
        int size = images.size();
        for(int i = 1; i < size; i++)
        {
            if(forward)
            {
                if(f != images.get((ci+i)>=size?ci+i-size:ci+i).getParent())
                {
                    curimg = (ci+i)>=size?ci+i-size:ci+i;
                    if(fav)
                        curimg = this.nextIndexWithTag(curimg, "fav", true);
                    break;
                }
            }
            else
            {
                if(f != images.get((ci-i)<0?ci-i+size:ci-i).getParent())
                {
                    int pos = (ci-i)<0?ci-i+size:ci-i;
                    if(fav)
                    {
                        pos = this.prevIndexWithTag(pos, "fav", true);
                    }
                    curimg = rewindFolder(pos, fav);
                    break;
                }
            }
        }
        return getCurrent();
    }
    
    public Image jumpOrientationWithinFolder()
    {
        if(images.size() <= 0)
            return null;
        GFolder f = this.images.get(curimg).getParent();
        int ci = curimg;
        int size = this.images.size();
        String targetOrientation = this.images.get(curimg).hasTag("vertical")?"horizontal":"vertical";
        for(int i = 1; i < size; i++)
        {
            int pos = (ci+i)>=size?ci+i-size:ci+i;
            if(images.get(pos).hasTag(targetOrientation) && f == images.get(pos).getParent())
            {
                curimg = pos;
                break;
            }
        }
        return getCurrent();
    }
    
    public int rewindFolder(int pos, boolean fav)
    {
        GFolder f = images.get(pos).getParent();
        int index = pos;
        int size = images.size();
        for(int i = 1; i < size; i++)
        {
            int pos1 = (pos-i)<0?pos-i+size:pos-i;
            //System.out.println(pos1);
            if(images.get(pos1).hasTag("fav") && f == images.get(pos1).getParent())
            {
                index = pos1;
            }
            if(f != images.get(pos1).getParent())
            {
                if(fav)
                    return index;
                else
                    return (pos1+1)>=size?0:pos1+1;
            }
        }
        return -1;
    }
    
    public String getCurrentComment()
    {
        if(curimg < 0 || images.size() <= 0)
            return "";
        String res = images.get(curimg).getComment();
        if(res == null)
            res = "";
        return res;
    }
    
    public void setCurrentComment(String c)
    {
        if(curimg < 0 || images.size() <= 0)
            return;
        if(c.equals(""))
            images.get(curimg).setComment(null);
        else
            images.get(curimg).setComment(c);
    }
    
    public void currentImageViewed()
    {
        if(curimg < 0 || images.size() <= 0)
            return;
        images.get(curimg).setViewedNow();
        //TODO increase viewcount here
    }
    
    public void currentImageModified()
    {
        if(curimg < 0 || images.size() <= 0)
            return;
        images.get(curimg).setLastmodNow();
    }
    
    public String getAdded()
    {
        if(curimg < 0 || images.size() <= 0)
            return "";
        long v = images.get(curimg).getAdded();
        if(v < 0)
            return "ERROR";
        Date resultdate = new Date(v);
        return sdf.format(resultdate);
    }
    
    public String getViewed()
    {
        if(curimg < 0 || images.size() <= 0)
            return "";
        long v = images.get(curimg).getViewed();
        if(v < 0)
            return "Never";
        Date resultdate = new Date(v);
        return sdf.format(resultdate);
    }
    
    public String getLastmod()
    {
        if(curimg < 0 || images.size() <= 0)
            return "";
        long m = images.get(curimg).getLastmod();
        if(m < 0)
            return "Never";
        Date resultdate = new Date(m);
        return sdf.format(resultdate);
    }
    
    public Image getCurrent()
    {
        if(curimg < 0 || images.size() <= 0)
            return null;
        Image res = images.get(curimg).cload();
        cached.add(images.get(curimg));
        if(cached.size()>numcache)
            cacheRemoveFurthest();
        return res;
    }
    
    //TODO maybe possible to do swaps without giving
    //GImage objects to viewer?
    public GImage getCurrentGImage()
    {
        if(curimg < 0 || images.size() <= 0)
            return null;
        return images.get(curimg);
    }
    
    /**
     * 
     * @param img1 image to link
     * @param img2 image to link to
     * @return 
     */
    public int linkTo(GImage img1, GImage img2)
    {
        int res = db.addToList(img1, img2);
        if(res != 0)
            return res;
        
        //TODO check if we want to group images in the same list
        int pos1 = locate(img1);
        int pos2 = locate(img2);
        if(pos1 == -1)
            return -10;
        images.remove(pos1);
        if(pos1 <= pos2)
            pos2 -= 1;
        images.add(pos2+1, img1);
        
        return 0;
    }
    
    public int linkToCurrent(GImage img)
    {
        if(curimg < 0 || images.size() <= 0)
            return -1000;
        GImage cur = images.get(curimg);
        return linkTo(img, cur);
    }
    
    public int moveAfterCurrent(GImage img)
    {
        if(curimg < 0 || images.size() <= 0)
            return -1000;
        GImage cur = images.get(curimg);
        //TODO allow moving even though different folders?
        if(cur.getParent() != img.getParent())
            return 1;
        
        //TODO need to consider if there are images in the same list as cur
        //after it and img is not in that list -> need to place img after
        //the last image in the list? (if "group lists" is enabled)
        int pos1 = locate(img);
        int pos2 = locate(cur);
        if(pos1 == -1)
            return -10;
        images.remove(pos1);
        if(pos1 <= pos2)
            pos2 -= 1;
        images.add(pos2+1, img);
        
        int res = cur.getParent().moveImageAfter(img, cur);
        if(res < 0)
            return res;
        
        if(cur.getList() != null && img.getList() != null && cur.getList() == img.getList())
        {
            res = cur.getList().moveImageAfter(img, cur);
            if(res < 0)
                return res-10;
        }
        
        return 0;
    }
    
    public int locate(GImage img)
    {
        for(int i = 0; i < images.size(); i++)
            if(img == images.get(i))
                return i;
        return -1;
    }
    
    public String getCurrentNameFull()
    {
        if(curimg < 0 || images.size() <= 0)
            return "";
        GImage current = images.get(curimg);
        return (current.getParent()==null)?current.getName():
                current.getParent().getPath()+current.getName();
    }
    
    public String getCurrentName()
    {
    	if(curimg < 0 || images.size() <= 0)
            return "";
    	return images.get(curimg).getName();
    }
    
    /**
     * Get current image's path in 2/3 pieces - root folder + {subfolders} + image name
     * @return
     */
    public String[] getCurrentNameFullPiecewise()
    {
    	if(curimg < 0 || images.size() <= 0)
            return new String[]{"", ""};
    	GImage current = images.get(curimg);
    	GFolder topParent = current.getTopLevelParent();
    	int num = current.isOnTopLevel() ? 2 : 3;
    	String[] res = new String[num];
    	res[num-1] = current.getName();
    	res[0] = topParent == null ? "" : topParent.getPath();
    	if(num > 2)
    	{
    		//parent is not null in this case
    		res[1] = current.getParent().getPath().substring(topParent.getPath().length());
    	}
    	return res;
    }
    
    public void cacheRemoveFurthest()
    {
        //TODO not correct but will do for now
        cached.get(0).unload();
        cached.remove(0);
    }
    
    public void setNumCache(int n)
    {
        numcache = n;
    }
    
    public int getSize()
    {
        return images.size();
    }
    
    public int getCurrentPosition()
    {
        return curimg;
    }
    
    public int getCurrentPositionWithinFolder()
    {
    	//TODO better way?
    	GFolder curFolder = images.get(curimg).getParent();
    	int index = curimg;
    	while(true)
    	{
    		index--;
    		if(index < 0)
    			//index = images.size()-1;
    			break;
    		/*if(index == curimg)
    			break;*/
    		if(images.get(index).getParent() != curFolder)
    			//return curimg > index ? curimg-index : curimg + images.size() - index;
    			return curimg-index-1;
    	}
    	return curimg;
    }
    
    public int getCurrentFolderSize()
    {
    	//TODO better way?
    	GFolder curFolder = images.get(curimg).getParent();
    	int index = curimg;
    	int start = 0;
    	while(true)
    	{
    		index--;
    		if(index < 0)
    			break;
    		/*if(index == curimg)
    			break;*/
    		if(images.get(index).getParent() != curFolder)
    		{
    			start = index + 1;
    			break;
    		}
    	}
    	index = curimg;
    	while(true)
    	{
    		index++;
    		if(index >= images.size())
    			break;
    		/*if(index == curimg)
    			break;*/
    		if(images.get(index).getParent() != curFolder)
    			break;
    	}
    	return index-start;
    }
    
    public void setTags(ArrayList<String> t)
    {
        if(curimg < 0 || images.size() <= 0)
            return;
        images.get(curimg).setTags(t, true);
    }
    
    public Image removeCurrent()
    {
        if(curimg < 0 || images.size() <= 0)
            return null;
        images.remove(curimg);
        if(curimg >= images.size())
            curimg = images.size()-1;
        return getCurrent();
    }
    
    public String getTagString()
    {
        if(curimg < 0 || images.size() <= 0)
            return "";
        List<String> tags = images.get(curimg).getTags();
        String res = "";
        for(String tag : tags)
            res += tag + " ";
        return res.trim();
    }
    
    public void invertOrientationTag()
    {
        if(curimg < 0 || images.size() <= 0)
            return;
        GImage cur = images.get(curimg);
        if(cur.hasTag("mixed"))
        {
            cur.dropTag("mixed");
            cur.addTag("horizontal", true);
            return;
        }
        if(cur.hasTag("horizontal"))
        {
            cur.dropTag("horizontal");
            cur.addTag("vertical", true);
            return;
        }
        if(cur.hasTag("vertical"))
        {
            cur.dropTag("vertical");
            cur.addTag("horizontal", true);
        }
    }
    
    public void addTagToCurrent(String tag, boolean toggle)
    {
        if(curimg < 0 || images.size() <= 0)
            return;
        if(toggle && images.get(curimg).hasTag(tag))
            images.get(curimg).dropTag(tag);
        else
            images.get(curimg).addTag(tag, true);
    }
    
    public void favCurrent(boolean toggle)
    {
        this.addTagToCurrent("fav", toggle);
    }
    
    public void lowCurrent(boolean toggle)
    {
        this.addTagToCurrent("low", toggle);
    }
    
    public int getCurrentRating()
    {
    	if(curimg < 0 || images.size() <= 0)
            return -1;
    	return images.get(curimg).getParent().getRating();
    }
    
    public void rateCurrentFolder(int r)
    {
        if(curimg < 0 || images.size() <= 0)
            return;
        //TODO yeeeeeeea...
        for(int i = 0; i <= 5; i++)
            if(images.get(curimg).getParent().hasTag(String.valueOf(i)))
            {
                images.get(curimg).getParent().dropTag(String.valueOf(i));
                break;
            }
        images.get(curimg).getParent().addTag(String.valueOf(r), true);
    }
    
    //TODO doesn't belong here?
    public void rotateImage(boolean left)
    {
        if(curimg < 0 || images.size() <= 0)
            return;
        images.get(curimg).rotate(left, -20);
    }
    
    public Image reloadImage()
    {
        if(curimg < 0 || images.size() <= 0)
            return null;
        images.get(curimg).unload();
        return images.get(curimg).cload();
    }
}
