/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nquisition.hlibrary.model;

import com.nquisition.util.FileUtils;
import com.nquisition.hlibrary.util.CreationTimeGFolderComparator;
import com.nquisition.hlibrary.Utils;
import com.nquisition.hlibrary.api.IGFolder;

import java.util.*;
import java.io.*;

/**
 *
 * @author Master
 */
public class GFolder extends GEntry implements IGFolder
{
    private String path;
    private String alias;
    private transient GFolder parent;
    private List<GImage> images;
    private List<GFolder> subfolders;
    
    @Override
	public String toString()
    {
        return path;
    }
    
    public GFolder()
    {
        path = "";
        alias = "";
        parent = null;
        images = new ArrayList<>();
        subfolders = new ArrayList<>();
        this.resetTags();
    }
    
    public GFolder(String p)
    {
    	this();
        path = p;
        if(p.endsWith("\\"))
        	p = p.substring(0, p.length()-1);
        alias = p.substring(p.lastIndexOf('\\') + 1);
    }
    
    public GFolder(String p, GFolder par)
    {
        this(p);
        parent = par;
    }
    
    @Override
	public void nullifyEmptyStrings()
    {
    	super.nullifyEmptyStrings();
    	if(alias == null || alias.equals("") || alias.equals("null"))
    	{
    		String temp = path;
    		if(temp.endsWith("\\"))
    			temp = temp.substring(0, temp.length()-1);
    		alias = temp.substring(temp.lastIndexOf('\\') + 1);
    	}
    	
    	for(GImage image : images)
    		image.nullifyEmptyStrings();
    	for(GFolder folder : subfolders)
    		folder.nullifyEmptyStrings();
    }
    
    @Override
	public int getRating()
    {
        //TODO ............
        if(this.hasTag("0"))
            return 0;
        else if(this.hasTag("1"))
            return 1;
        else if(this.hasTag("2"))
            return 2;
        else if(this.hasTag("3"))
            return 3;
        else if(this.hasTag("4"))
            return 4;
        else if(this.hasTag("5"))
            return 5;
        return -1;
    }
            
    public GFolder getFolderByPath(String path, boolean recursive)
    {
        if(path.equalsIgnoreCase(this.path))
            return this;
        if(recursive)
        {
            for(GFolder f : subfolders)
            {
                GFolder res = f.getFolderByPath(path, true);
                if(res != null)
                    return res;
            }
        }
        
        return null;
    }
    
    public void subInPaths(String subFrom, String subTo) {
    	System.out.println(path.startsWith(subFrom)+ "\"" + path + "\" starts with \"" + subFrom + "\"");
	    if(path.startsWith(subFrom))
			this.setPath(subTo + path.substring(subFrom.length()));
	    for(GFolder folder : subfolders)
	    	folder.subInPaths(subFrom, subTo);
    }
    
    @Override
	public int getNumImages()
    {
    	return images.size();
    }
    
    public GFolder getTopLevelParent()
    {
    	if(parent != null)
    		return parent.getTopLevelParent();
    	return this;
    }
    
    public String getString()
    {
        String separator = Database.DATA_SEPARATOR;
        String res = this.getPath();
        for(String tag : this.getTags())
            res += separator + tag;
        res += separator;
        return res;
    }
    
    @Override
	public String getAlias()
    {
        return alias;
    }
    
    @Override
	public String getPath()
    {
        return path;
    }
    
    public GFolder getParent()
    {
        return parent;
    }
    
    @Override
	public List<GImage> getImages()
    {
        return new ArrayList<>(images);
    }
    
    public void getAllImages(List<GImage> list)
    {
        for(GImage i : images)
            list.add(i);
        for(GFolder f : subfolders)
            f.getAllImages(list);
    }
    
    public List<GFolder> getSubFolders()
    {
        return subfolders;
    }
    
    public void addImage(GImage img)
    {
        images.add(img);
    }
    
    public void addSubFolder(GFolder f)
    {
        f.setParent(this);
        subfolders.add(f);
    }
    
    public void setPath(String path)
    {
    	this.path = path;
    }
    
    public void setParent(GFolder par)
    {
        parent = par;
    }
    
    public void setAlias(String a)
    {
        alias = a;
    }
    
    public void printFolderRecursive(int num)
    {
        String indent = "";
        for(int i = 0; i < num; i++)
            indent = "---" + indent;
        for(GImage img : images)
        {
            System.out.println(indent + img.getName());
        }
        for(GFolder f : subfolders)
        {
            System.out.println(indent + "<" + f.getPath());
            f.printFolderRecursive(num+1);
        }
    }
    
    public void removeImage(GImage img)
    {
        for(int i = 0; i < images.size(); i++)
        {
            if(images.get(i)==img)
            {
                images.remove(i);
                return;
            }
        }
    }
    
    public boolean folderExists()
    {
        File f = new File(path);
        return f.exists();
    }
    
    public void purge(boolean recursive) throws IOException
    {
        Iterator<GImage> i = images.iterator();
        while (i.hasNext())
        {
            GImage img = i.next();
            File f = new File(img.getFullPath());
            if(!f.exists())
            {
                System.out.println("Purging " + f.getCanonicalPath());
                img.clearParent();
                i.remove();
            }
        }
        
        if(!recursive)
            return;
        
        Iterator<GFolder> j = subfolders.iterator();
        while (j.hasNext())
        {
            GFolder fl = j.next();
            fl.purge(recursive);
            if(!fl.folderExists())
                j.remove();
        }
    }
    
    public void refresh(boolean chkFolders, boolean recursive) throws IOException
    {
        this.purge(false);
        File dir = new File(path);
        File[] list = dir.listFiles();
        for(File f : list)
        {
            if(FileUtils.isImage(f))
            {
                GImage.create(this, f.getName(), true, -1, true);
            }
            else if(chkFolders && f.isDirectory())
            {
                String nm1 = f.getName().endsWith("\\")?f.getName():f.getName()+"\\";
                if(this.containsSubfolder(nm1))
                    continue;
                //TODO add subfolder!
            }
        }
        
        this.checkVerticalityDefault(false);
        Comparator<GFolder> comp = new CreationTimeGFolderComparator();
        Collections.sort(subfolders, comp);
        
        //TODO sort files?!
        
        if(recursive)
            for(GFolder sub : subfolders)
                sub.refresh(chkFolders, recursive);
    }
    
    public boolean containsSubfolder(String name)
    {
        for(GFolder sub : subfolders)
        {
            String tmp = sub.getPath().endsWith("\\")?sub.getPath().substring(0, sub.getPath().length()-1):sub.getPath();
            String nm2 = tmp.substring(tmp.lastIndexOf('\\') + 1);
            if(name.equals(nm2 + "\\"))
                return true;
        }
        return false;
    }
    
    //TODO this probably belongs in the Database still...
    public void checkVerticality(double tV, double tH, boolean nameOnly, boolean recursive)
    {
        for(GImage img : getImages())
        {
            if(img.hasTag("vertical") || img.hasTag("horizontal") || 
                    img.hasTag("mixed") || img.hasTag("horizontal_forced") ||
                    img.hasTag("vertical_forced"))
                continue;
            if(img.getName().startsWith("末vert_") || img.getName().startsWith("vert_"))
            {
                img.addTag("vertical", true);
                continue;
            }
            if(!nameOnly)
            {
                int res = Utils.checkImageVertical(img, tV, tH);
                if(res == 1)
                    img.addTag("vertical", true);
                else if(res == -1)
                    img.addTag("horizontal", true);
                else if(res == 0)
                    img.addTag("mixed", true);
                else if(res == -3)
                    System.out.println("!!! >> Cannot load image " + img.getName());
            }
            else
                img.addTag("horizontal", true);
        }
        if(recursive)
            for(GFolder fld : getSubFolders())
                fld.checkVerticality(tV, tH, nameOnly, recursive);
    }
    
    public void checkVerticalityDefault(boolean recursive)
    {
        this.checkVerticality(1.0, 1.0, true, recursive);
    }
    
    public void sortSubfolders(Comparator<GFolder> comp)
    {
        Collections.sort(this.subfolders, comp);
        for(GFolder f : this.subfolders)
            f.sortSubfolders(comp);
    }
    
    /**
     * 
     * @param img1 Image to move
     * @param img2 Image after which to place
     */
    public int moveImageAfter(GImage img1, GImage img2)
    {
        if(img1.getParent() != this || img2.getParent() != this)
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
    
    public double getFavPercentage(boolean recursive) {
    	double numFavs = images.stream().filter(a -> a.hasTag("fav")).count();
    	return numFavs/images.size();
    }
    
    public void sortImages(Comparator<GImage> comp) {
    	Collections.sort(this.images, comp);
        for(GFolder f : this.subfolders)
            f.sortImages(comp);
    }
}
