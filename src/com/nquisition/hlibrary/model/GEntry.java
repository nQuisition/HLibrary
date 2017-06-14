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
public class GEntry
{
    private String comment;
    private ArrayList<String> tags;
    private long added = -1, lastmod = -1, viewed = -1;
    private int viewcount = 0;
    
    public long getAdded()
    {
        return added;
    }
    
    public long getLastmod()
    {
        return lastmod;
    }
    
    public long getViewed()
    {
        return viewed;
    }
    
    public int getViewcount()
    {
        return viewcount;
    }
    
    public void setAddedNow()
    {
        setAdded(-1);
    }
    
    public void setAdded(long a)
    {
        if(a < 0)
            added = System.currentTimeMillis();
        else
            added = a;
    }
    
    public void setLastmodNow()
    {
        setLastmod(-1);
    }
    
    public void setLastmod(long a)
    {
        if(a < 0)
            lastmod = System.currentTimeMillis();
        else
            lastmod = a;
    }
    
    public void setViewedNow()
    {
        setViewed(-1);
    }
    
    public void setViewed(long a)
    {
        if(a < 0)
            viewed = System.currentTimeMillis();
        else
            viewed = a;
    }
    
    public void addViewcount()
    {
        viewcount++;
    }
    
    public void setViewcount(int vc)
    {
        viewcount = vc;
    }
    
    public void setComment(String c)
    {
        comment = c;
    }
    
    public String getComment()
    {
        return comment;
    }
    
    public void resetTags()
    {
        tags = new ArrayList<String>();
    }
    
    public void setTags(ArrayList<String> t, boolean check)
    {
        resetTags();
        for(String tag : t)
        {
            if(check && this.hasTag(tag))
                continue;
            tags.add(tag);
        }
        sortTags();
    }
    
    public void addTag(String t, boolean check)
    {
        if(check)
        {
            if(this.hasTag(t))
                return;
        }
        tags.add(t.toLowerCase());
        sortTags();
    }
    
    public void dropTag(String t)
    {
        for(int i = 0; i < tags.size(); i++)
        {
            if(tags.get(i).equalsIgnoreCase(t))
            {
                tags.remove(i);
                return;
            }
        }
    }
    
    public boolean hasTag(String t)
    {
        for(String t1 : tags)
                if(t.equalsIgnoreCase(t1))
                    return true;
        return false;
    }
    
    public boolean hasAllTags(ArrayList<String> tgs)
    {
        for(String t : tgs)
            if(!this.hasTag(t))
                return false;
        return true;
    }
    
    public boolean hasNoTags(ArrayList<String> tgs)
    {
        for(String t : tgs)
            if(this.hasTag(t))
                return false;
        return true;
    }
    
    public ArrayList<String> getTags()
    {
        return tags;
    }
    
    public void sortTags()
    {
        Collections.sort(tags);
    }
}
