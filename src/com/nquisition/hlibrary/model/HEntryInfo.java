/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nquisition.hlibrary.model;

import java.util.*;
import java.util.Map.Entry;

import com.nquisition.hlibrary.api.ReadOnlyEntryInfo;

/**
 *
 * @author Master
 */
public class HEntryInfo implements ReadOnlyEntryInfo
{
    private String comment = null;
    private List<String> tags;
    private long added = -1;
    private long lastmod = -1;
    private long viewed = -1;
    private int viewcount = 0;
    
    private Map<String, Object> customProps = new HashMap<>();
    
    //TODO maybe rename this one?
    public void nullifyEmptyStrings()
    {
    	if(comment != null && (comment.equals("") || comment.equals("null")))
    		comment = null;
    	customProps.entrySet().removeIf(entry -> entry.getValue() == null);
    }
    
    @Override
	public long getAdded()
    {
        return added;
    }
    
    @Override
	public long getLastmod()
    {
        return lastmod;
    }
    
    @Override
	public long getViewed()
    {
        return viewed;
    }
    
    @Override
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
    
    @Override
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
    
    @Override
	public String getComment()
    {
        return comment;
    }
    
    public void resetTags()
    {
        tags = new ArrayList<>();
    }
    
    public void setTags(List<String> t, boolean check)
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
    
    @Override
	public void addTag(String t) {
        addTag(t, true);
    }
    
    public void addTag(String t, boolean check)
    {
        if(check && this.hasTag(t)) {
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
    
    @Override
	public boolean hasTag(String t)
    {
        for(String t1 : tags)
                if(t.equalsIgnoreCase(t1))
                    return true;
        return false;
    }
    
    @Override
	public boolean hasAllTags(List<String> tgs)
    {
        for(String t : tgs)
            if(!this.hasTag(t))
                return false;
        return true;
    }
    
    @Override
	public boolean hasNoTags(List<String> tgs)
    {
        for(String t : tgs)
            if(this.hasTag(t))
                return false;
        return true;
    }
    
    public List<String> getTags()
    {
        return new ArrayList<>(tags);
    }
    
    public void sortTags()
    {
        Collections.sort(tags);
    }

	@Override
	public void setProperty(String name, Object value) {
		customProps.put(name, value);
	}

	@Override
	public Object getProperty(String name) {
		return customProps.getOrDefault(name, null);
	}
}
