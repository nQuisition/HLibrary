/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nquisition.hlibrary.model;

import java.util.*;

/**
 *
 * @author Master
 */
public class GImageList extends GEntry
{
    private static int NEXTID = 0;
    private int id;
    private List<GImage> images;
    
    public GImageList()
    {
        this.resetTags();
        images = new ArrayList<>();
        id = NEXTID;
        NEXTID++;
    }
    
    public GImageList(int id)
    {
        this.resetTags();
        images = new ArrayList<>();
        this.id = id;
        NEXTID = NEXTID>id?NEXTID:id+1;
    }
    
    public int getID()
    {
        return id;
    }
    
    public int addImage(GImage img)
    {
        int res = this.insertImageAfter(img, null, true);
        if(res == 0)
            img.setList(this);
        return res;
    }
    
    /**
     * 
     * @param img1 Image to add
     * @param img2 Image after which to place
     */
    public int addImageAfter(GImage img1, GImage img2)
    {
        int res = this.insertImageAfter(img1, img2, true);
        if(res == 0)
            img1.setList(this);
        return res;
    }
    
    public int setImage(int pos, GImage img)
    {
        img.setList(this);
        for(int i = images.size(); i <= pos; i++)
            images.add(null);
        GImage res = images.set(pos, img);
        if(res != null)
            return -1;
        return 0;
    }
    
    public int detachImage(GImage img)
    {
        int pos = this.locate(img);
        if(pos < 0)
            return pos;
        img.setList(null);
        images.remove(pos);
        return 0;
    }
    
    public List<GImage> getImages()
    {
        return images;
    }
    
    /**
     * 
     * @param img1 Image to add
     * @param img2 Image after which to place; null if last image
     */
    public int insertImageAfter(GImage img1, GImage img2, boolean check)
    {
        if(check && locate(img1) >= 0)
            return -1;
        int pos = images.size();
        if(img2 != null)
        {
            pos = locate(img2);
            if(pos == -1)
                return -2;
            pos++;
        }
        
        images.add(pos, img1);
        return 0;
    }
    
    /**
     * 
     * @param img1 Image to move
     * @param img2 Image after which to place
     */
    public int moveImageAfter(GImage img1, GImage img2)
    {
        if(img1.getList() != this || img2.getList() != this)
            return -1;
        int pos1 = locate(img1);
        int pos2 = locate(img2);
        if(pos1 == -1)
            return -2;
        if(pos2 == -1)
            return -3;
        GImage img = images.remove(pos1);
        if(pos1 <= pos2)
            pos2 -= 1;
        images.add(pos2+1, img);
        return 0;
    }
    
    public int locate(GImage img)
    {
        for(int i = 0; i < images.size(); i++)
            if(img == images.get(i))
                return i;
        return -1;
    }
}
