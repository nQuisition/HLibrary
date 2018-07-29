/*

 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nquisition.hlibrary.model;

import java.util.*;

import org.apache.logging.log4j.core.util.NameUtil;

import java.io.*;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.image.*;
import java.text.*;

/**
 *
 * @author Master
 */
public class Gallery
{
    private List<GImage> images;
    private IntegerProperty curimg = new SimpleIntegerProperty(-1) {
    	
    };
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
    
    public void getByName(String fname)
    {
        int index = 0;
        for(int i = 0; i < images.size(); i++)
            if(images.get(i).getName().equalsIgnoreCase(fname))
            {
                index = i;
                break;
            }
        curimg.set(index);
        //return getCurrent();
    }
    
    public void getNext(boolean fav)
    {
        if(images.size() <= 0)
            return;
        if(images.size() <= 0)
            return;
        if(fav)
            curimg.set(this.nextIndexWithTag(curimg.get(), "fav", false));
        else
        {
        	int index = curimg.get()+1;
            if(index >= images.size())
            	index = 0;
            curimg.set(index);
        }
        //return getCurrent();
    }
    
    public void getPrev(boolean fav)
    {
        if(images.size() <= 0)
            return;
        if(fav)
            curimg.set(this.prevIndexWithTag(curimg.get(), "fav", false));
        else
        {
        	int index = curimg.get()-1;
            if(index < 0)
            	index = images.size()-1;
            curimg.set(index);
        }
        //return getCurrent();
    }
    
    public void jump(int num)
    {
        if(images.size() <= 0)
            return;
        int index = curimg.get() + num;
        while(index < 0)
        	index = index + images.size();
        while(index >= images.size())
        	index = index - images.size();
        curimg.set(index);
        //return getCurrent();
    }
    
    public void navigateTo(int num)
    {
        if(images.size() <= 0 || num < 0 || num >= images.size())
            return;
        curimg.set(num);
        //return getCurrent();
    }
    
    public void jumpFolder(boolean forward, boolean fav)
    {
        if(images.size() <= 0)
            return;
        int ci = curimg.get();
        GFolder f = images.get(ci).getParent();
        int size = images.size();
        for(int i = 1; i < size; i++)
        {
            if(forward)
            {
                if(f != images.get((ci+i)>=size?ci+i-size:ci+i).getParent())
                {
                    int index = (ci+i)>=size?ci+i-size:ci+i;
                    if(fav)
                        index = this.nextIndexWithTag(curimg.get(), "fav", true);
                    curimg.set(index);
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
                    curimg.set(rewindFolder(pos, fav));
                    break;
                }
            }
        }
        //return getCurrent();
    }
    
    public void jumpOrientationWithinFolder()
    {
        if(images.size() <= 0)
            return;
        GFolder f = this.images.get(curimg.get()).getParent();
        int ci = curimg.get();
        int size = this.images.size();
        String targetOrientation = this.images.get(curimg.get()).hasTag("vertical")?"horizontal":"vertical";
        for(int i = 1; i < size; i++)
        {
            int pos = (ci+i)>=size?ci+i-size:ci+i;
            if(images.get(pos).hasTag(targetOrientation) && f == images.get(pos).getParent())
            {
                curimg.set(pos);
                break;
            }
        }
        //return getCurrent();
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
    
    public Map<GImage, Integer> getFavs() {
    	Map<GImage, Integer> res = new HashMap<>();
    	for(int i = 0; i < images.size(); i++) {
    		if(images.get(i).hasTag("fav"))
    			res.put(images.get(i), i);
    	}
    	return res;
    }
    
    public String getCurrentComment()
    {
        if(curimg.get() < 0 || images.size() <= 0)
            return "";
        String res = images.get(curimg.get()).getComment();
        if(res == null)
            res = "";
        return res;
    }
    
    public void setCurrentComment(String c)
    {
        if(curimg.get() < 0 || images.size() <= 0)
            return;
        if(c.equals(""))
            images.get(curimg.get()).setComment(null);
        else
            images.get(curimg.get()).setComment(c);
    }
    
    public void currentImageViewed()
    {
        if(curimg.get() < 0 || images.size() <= 0)
            return;
        images.get(curimg.get()).setViewedNow();
        //TODO increase viewcount here
    }
    
    public void currentImageModified()
    {
        if(curimg.get() < 0 || images.size() <= 0)
            return;
        images.get(curimg.get()).setLastmodNow();
    }
    
    public String getAdded()
    {
        if(curimg.get() < 0 || images.size() <= 0)
            return "";
        long v = images.get(curimg.get()).getAdded();
        if(v < 0)
            return "ERROR";
        Date resultdate = new Date(v);
        return sdf.format(resultdate);
    }
    
    public String getViewed()
    {
        if(curimg.get() < 0 || images.size() <= 0)
            return "";
        long v = images.get(curimg.get()).getViewed();
        if(v < 0)
            return "Never";
        Date resultdate = new Date(v);
        return sdf.format(resultdate);
    }
    
    public String getLastmod()
    {
        if(curimg.get() < 0 || images.size() <= 0)
            return "";
        long m = images.get(curimg.get()).getLastmod();
        if(m < 0)
            return "Never";
        Date resultdate = new Date(m);
        return sdf.format(resultdate);
    }
    
    public Image getCurrent()
    {
        if(curimg.get() < 0 || images.size() <= 0)
            return null;
        Image res = images.get(curimg.get()).cload();
        cached.add(images.get(curimg.get()));
        if(cached.size()>numcache)
            cacheRemoveFurthest();
        return res;
    }
    
    //TODO maybe possible to do swaps without giving
    //GImage objects to viewer?
    public GImage getCurrentGImage()
    {
        if(curimg.get() < 0 || images.size() <= 0)
            return null;
        return images.get(curimg.get());
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
        if(curimg.get() < 0 || images.size() <= 0)
            return -1000;
        GImage cur = images.get(curimg.get());
        return linkTo(img, cur);
    }
    
    public int moveAfterCurrent(GImage img)
    {
        if(curimg.get() < 0 || images.size() <= 0)
            return -1000;
        GImage cur = images.get(curimg.get());
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
        if(curimg.get() < 0 || images.size() <= 0)
            return "";
        GImage current = images.get(curimg.get());
        return (current.getParent()==null)?current.getName():
                current.getParent().getPath()+current.getName();
    }
    
    public String getCurrentName()
    {
    	if(curimg.get() < 0 || images.size() <= 0)
            return "";
    	return images.get(curimg.get()).getName();
    }
    
    /**
     * Get current image's path in 2/3 pieces - root folder + {subfolders} + image name
     * @return
     */
    public String[] getCurrentNameFullPiecewise()
    {
    	if(curimg.get() < 0 || images.size() <= 0)
            return new String[]{"", ""};
    	GImage current = images.get(curimg.get());
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
    
    @Deprecated
    //Listen to curimg instead!
    public int getCurrentPosition()
    {
        return curimg.get();
    }
    
    @Deprecated
    //Listen to curimg instead??
    public int getCurrentPositionWithinFolder()
    {
    	//TODO better way?
    	GFolder curFolder = images.get(curimg.get()).getParent();
    	int index = curimg.get();
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
    			return curimg.get()-index-1;
    	}
    	return curimg.get();
    }
    
    @Deprecated
    //Listen to curimg instead??
    public int getCurrentFolderSize()
    {
    	//TODO better way?
    	GFolder curFolder = images.get(curimg.get()).getParent();
    	int index = curimg.get();
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
    	index = curimg.get();
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
    
    public void setTags(List<String> t)
    {
        if(curimg.get() < 0 || images.size() <= 0)
            return;
        images.get(curimg.get()).setTags(t, true);
    }
    
    /*public Image removeCurrent()
    {
        if(curimg.get() < 0 || images.size() <= 0)
            return null;
        images.remove(curimg);
        if(curimg >= images.size())
            curimg = images.size()-1;
        return getCurrent();
    }*/
    
    public String getTagString()
    {
        if(curimg.get() < 0 || images.size() <= 0)
            return "";
        List<String> tags = images.get(curimg.get()).getTags();
        String res = "";
        for(String tag : tags)
            res += tag + " ";
        return res.trim();
    }
    
    public void invertOrientationTag()
    {
        if(curimg.get() < 0 || images.size() <= 0)
            return;
        GImage cur = images.get(curimg.get());
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
        if(curimg.get() < 0 || images.size() <= 0)
            return;
        if(toggle && images.get(curimg.get()).hasTag(tag))
            images.get(curimg.get()).dropTag(tag);
        else
            images.get(curimg.get()).addTag(tag, true);
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
    	if(curimg.get() < 0 || images.size() <= 0 || images.get(curimg.get()).getParent() == null)
            return -1;
    	return images.get(curimg.get()).getParent().getRating();
    }
    
    public void rateCurrentFolder(int r)
    {
        if(curimg.get() < 0 || images.size() <= 0)
            return;
        //TODO yeeeeeeea...
        for(int i = 0; i <= 5; i++)
            if(images.get(curimg.get()).getParent().hasTag(String.valueOf(i)))
            {
                images.get(curimg.get()).getParent().dropTag(String.valueOf(i));
                break;
            }
        images.get(curimg.get()).getParent().addTag(String.valueOf(r), true);
    }
    
    //TODO doesn't belong here?
    public void rotateImage(boolean left)
    {
        if(curimg.get() < 0 || images.size() <= 0)
            return;
        images.get(curimg.get()).rotate(left, -20);
    }
    
    public void reloadImage()
    {
        if(curimg.get() < 0 || images.size() <= 0)
            return;
        images.get(curimg.get()).unload();
        //FIXME ugly hack
        int index = curimg.get();
        curimg.set(-1);
        curimg.set(index);
    }
    
    public void bindToCurrentImageProperty(IntegerProperty prop) {
    	prop.bind(curimg);
    }
}
