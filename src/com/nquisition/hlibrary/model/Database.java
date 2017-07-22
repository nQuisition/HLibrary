/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nquisition.hlibrary.model;

import com.nquisition.hlibrary.util.CreationTimeGFolderComparator;
import com.nquisition.hlibrary.ui.GalleryViewer;
import com.nquisition.hlibrary.Utils;
import com.nquisition.hlibrary.util.WindowsExplorerFileComparator;
import com.nquisition.util.Properties;
import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Collections;
import java.util.Arrays;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Master
 */
public class Database
{
    private static final transient Logger logger_local = LogManager.getLogger(Database.class.getName()+".local");
    private static final transient Logger logger_global = LogManager.getLogger(Database.class.getName()+".global");
    
    private final transient Logger logger;
    
    private final transient boolean local;
    
    public static final transient String DATA_SEPARATOR = "?";
    public static final transient String FOLDER_START = "<<";
    public static final transient String FOLDER_END = ">>";
    public static final transient String FOLDER_ROOT = "*";
    
    //private ArrayList<GImage> images;
    private ArrayList<GFolder> folders;
    //TODO untransient
    private transient ArrayList<GImageList> lists;
    private TagDictionary dict;
    
    private transient String location;
    
    public final void init()
    {
        //images = new ArrayList<GImage>();
        folders = new ArrayList<>();
        lists = new ArrayList<>();
        dict = new TagDictionary();
        dict.setLocation(Properties.get("dbroot", "dictname"));
    }
    
    //TODO for testing purposes
    public void findArtists()
    {
        for(GFolder f : folders)
        {
            String nm = f.getPath();
            if(nm.endsWith("\\"))
                nm = nm.substring(0, nm.length()-1);
            nm = nm.substring(nm.lastIndexOf('\\') + 1);
            if(nm.toLowerCase().contains("artist") || nm.toLowerCase().contains("pixiv"))
            {
                System.out.println(nm);
            }
        }
    }
    
    public Database(boolean local)
    {
        this.local = local;
        if(local)
            logger = logger_local;
        else
            logger = logger_global;
        logger.info("Initializing in {} mode", (local?"local":"global"));
        init();
    }
    
    public boolean isLocal()
    {
        return local;
    }
    
    public void setLocation(String l)
    {
        location = l;
    }
    
    public void info()
    {
    	logger.info("Number of root folders: " + folders.size());
    	logger.info("Number of images: " + getNumImages());
    }
    
    public void nullifyEmptyStrings()
    {
    	for(GFolder folder : folders)
    		folder.nullifyEmptyStrings();
    }
    
    public void setTagDictionary(TagDictionary dict)
    {
    	this.dict = dict;
    }
    
    public GFolder getFolderByName(String p)
    {
        for(GFolder f : folders)
        {
            GFolder res = f.getFolderByName(p, true);
            if(res != null)
                return res;
        }
        return null;
    }
    
    public boolean folderAlreadyAdded(String path)
    {
        for(GFolder f : folders)
        {
            if(f.getPath().equalsIgnoreCase(path))
                return true;
        }
        return false;
    }
    
    public GFolder getRootFolder(String path)
    {
        for(GFolder f : folders)
        {
            if(f.getPath().equalsIgnoreCase(path))
                return f;
        }
        return null;
    }
    
    //TODO probably move to GFolder class
    public GFolder createFolder(String p, GFolder par)
    {
        int pos = p.indexOf(DATA_SEPARATOR);
        String name = p;
        String params = "";
        if(pos > 0)
        {
            name = p.substring(0, pos);
            params = p.substring(pos).replace(DATA_SEPARATOR, " ").trim();
        }
        GFolder res = new GFolder(name, par);
        if(par != null)
            par.addSubFolder(res);
        
        ArrayList<String> tagArray = new ArrayList<>();
        if(!params.equalsIgnoreCase(""))
            tagArray = this.processTags(params.split("\\s+"));
        
        res.setTags(tagArray, true);
            
        return res;
    }
    
    public void addGFolder(GFolder folder, boolean check)
    {
    	if(check)
    		for(GFolder f : folders)
    			if(f.getPath().equals(folder.getPath()))
    				return;
    	folders.add(folder);
    }
    
    public GFolder getAddFolder(String path, GFolder par, boolean root)
    {
        GFolder f = null;
        if(par != null)
        {
            f = par.getFolderByName(path, false);
            if(f == null)
            {
                f = new GFolder(path);
                par.addSubFolder(f);
            }
        }
        if(root)
        {
            if(!this.folderAlreadyAdded(path))
            {
                if(par == null)
                    f = new GFolder(path);
                folders.add(f);
            }
            else
                f = this.getRootFolder(path);
        }
        return f;      
    }
    
    public GImageList getAddList(int id)
    {
        for(GImageList l : lists)
            if(l.getID() == id)
                return l;
        GImageList l = new GImageList(id);
        lists.add(l);
        return l;
    }
    
    public ArrayList<String> processTags(String[] tags)
    {
        ArrayList<String> res = new ArrayList<>();
        if(tags != null)
            for(int i = 0; i < tags.length; i++)
            {
                String tag = tags[i];
                tag = processTag(tag);
                if(tag != null)
                    res.add(processTag(tag));
            }
        return res;
    }
    
    public String processTag(String tag)
    {
        int pos = tag.indexOf('-');
        if(pos >= 0)
        {
            String k = tag.substring(0, pos);
            String v = tag.substring(pos+1);
            
            if(v == null || v.length() <= 0)
                tag = tag.substring(0, tag.length()-1);
            else if(k == null || k.length() <= 0)
            {
                tag = tag.substring(1);
                dict.removeAbbr(tag);
                tag = null;
            }
            else
            {
                dict.addAbbreviation(k, v);
                tag = k;
            }
        }
        return dict.getTag(tag);
    }
    
    public int addDirectory(String p, int maxd)
    {
        logger.info("Adding folder \"{}\" with max depth {}..", p, maxd);
        int res = this.addDirectory(new File(p), null, 0, maxd);
        logger.info("Added {} images from \"{}\"", res, p);
        return res;
    }
    
    //TODO okay to leave it private? Need it at all?
    private int addDirectory(String p, GFolder f, int maxd)
    {
        return this.addDirectory(new File(p), f, 0, maxd);
    }
    
    //TODO okay to leave it private?
    private int addDirectory(File p, GFolder f, int d, int maxd)
    {
        File[] listOfFiles = p.listFiles();
        Arrays.sort(listOfFiles, new WindowsExplorerFileComparator());
        
        //Arrays.sort(listOfFiles);
        boolean isRoot = (d < maxd);
        String ppath;
        try
        {
            ppath = p.getCanonicalPath();
            if(d != 0)
                logger.info("Adding subfolder \"{}\", depth: {}/{}", ppath, d, maxd);
        }
        catch(IOException e)
        {
            logger.error("Unable to get canonical path of \"" + p.getAbsolutePath() + "\"", e);
            return -1;
        }
        GFolder curFolder = this.getAddFolder(ppath + "\\", f, isRoot);
        
        int res = 0;
        
        for (File file : listOfFiles)
        {
            if (file.isFile())
            {
                String name = file.getName();
                String testname = name.toLowerCase();
                if(testname.endsWith(".jpg")
                        || testname.endsWith(".jpeg") || testname.endsWith(".png"))
                {
                    GImage.create(curFolder, name, true, -1, true);
                    res++;
                }
            }
            else if(file.isDirectory())
            {
                res = res + this.addDirectory(file, curFolder, d+1, maxd);
            }
        }
        return res;
    }
    
    public GImage getImageByID(int id)
    {
        for(GImage img : getImages())
            if(img.getID() == id)
                return img;
        return null;
    }
    
    public void printRootFolders()
    {
        for(GFolder f : folders)
        {
            System.out.println(f.getPath());
        }
    }
    
    public void printFileTree(int pos)
    {
        folders.get(pos).printFolderRecursive(0);
    }
    
    public ArrayList<GImage> getImages()
    {
        FolderListPair flp = this.getRootFolders();
        ArrayList<GImage> res = new ArrayList<>();
        for(GFolder f : flp.fldrs)
        {
            //TODO passing arraylist to be modified.. No good?
            f.getAllImages(res);
        }
        return res;
    }
    
    public ArrayList<GImageList> getImageLists()
    {
        return lists;
    }
    
    public ArrayList<GFolder> getFolders()
    {
        return folders;
    }
    
    public int getNumImages()
    {
        //TODO inefficient?
        return this.getImages().size();
    }
    
    public FolderListPair getRootFolders()
    {
        FolderListPair pair = new FolderListPair();
        
        ArrayList<GFolder> fldrs = new ArrayList<>();
        for(GFolder f : folders)
            fldrs.add(f);
        ArrayList<GFolder> roots = new ArrayList<>();
        int j = 0;
        while(j < fldrs.size())
        {
            GFolder f = fldrs.get(j);
            boolean flag = false;
            for(int i = 0; i < fldrs.size(); i++)
            {
                if(i == j)
                    continue;
                GFolder f1 = fldrs.get(i);
                //f1 contains f as subfolder
                if(f1.getFolderByName(f.getPath(), true) != null)
                {
                    flag = true;
                    roots.add(f);
                    fldrs.remove(j);
                    break;
                }
            }
            if(!flag)
                j++;
        }
        
        pair.fldrs = fldrs;
        pair.roots = roots;
        return pair;
    }
    
    public int saveDatabase()
    {
        return this.saveDatabase(location);
    }
    
    public int saveDatabase(String name)
    {
        logger.info("Saving database to \"{}\"", name);
        File file = new File(name);
        BufferedWriter bw;
        try
        {
            bw = new BufferedWriter(new OutputStreamWriter(
        new FileOutputStream(file), "UTF-8"));
        }
        catch(IOException e)
        {
            logger.error("Cannot open file \"" + name + "\" to save database!", e);
            return -1;
        }
        
        try
        {
            bw.write(dict.getLocation()+"\n");
            dict.saveDict();

            FolderListPair pair = this.getRootFolders();

            for(GFolder f : pair.fldrs)
            {
                this.writeFolder(f, pair.roots, bw, true);
            }
            bw.close();
        }
        catch(IOException e)
        {
            logger.error("Cannot write to file \"" + name + "\"!", e);
            return -2;
        }
        return 0;
    }
    
    private void writeFolder(GFolder f, ArrayList<GFolder> roots, 
            BufferedWriter bw, boolean initial) throws IOException
    {
        String str = FOLDER_START;
        if(initial)
            str += FOLDER_ROOT;
        else
        {
            for(int i = 0; i < roots.size(); i++)
            {
                if(roots.get(i) == f)
                {
                    str += FOLDER_ROOT;
                    roots.remove(i);
                    break;
                }
            }
        }
        str += f.getString() + "\n";
        bw.write(str);
        for(GImage img : f.getImages())
            bw.write(img.getString() + "\n");
        for(GFolder folder : f.getSubFolders())
            this.writeFolder(folder, roots, bw, false);
        bw.write(FOLDER_END + "\n");
    }
    
    public int loadDatabase()
    {
        File f = new File(location);
        if(f.exists() && f.isFile())
            return this.loadDatabase(location);
        init();
        return 1;
    }
    
    public int loadDatabase(String name)
    {
        return loadDatabase(name, true);
    }
    
    public int loadDatabase(String name, boolean init)
    { 
        if(init)
            init();
        
        logger.info("Loading database from \"{}\"..", name);
        
        int _fsize = this.folders.size();
        if(_fsize > 0)
            logger.info("Currently the database contains {} root folders and {} images", _fsize, this.getNumImages());
        
        BufferedReader br;
        try
        {
            br = new BufferedReader(new InputStreamReader(
                      new FileInputStream(name), "UTF8"));
        }
        catch(IOException e)
        {
            logger.error("Cannot open file \"" + name + "\" to load database!", e);
            return -1;
        }
        String line;
        
        try
        {
            line = br.readLine();
            logger.info("Dictionary set to \"{}\"", line);
            dict.setLocation(line);
            dict.loadDict();

            GFolder curFolder = null;
            while((line = br.readLine()) != null)
            {
                 if(line.startsWith(FOLDER_START))
                 {
                     //FOLDER
                     line = line.substring(FOLDER_START.length());
                     boolean isRoot = false;
                     if(line.startsWith(FOLDER_ROOT))
                     {
                         isRoot = true;
                         line = line.substring(FOLDER_ROOT.length());
                     }
                     curFolder = this.createFolder(line, curFolder);
                     if(isRoot)
                         folders.add(curFolder);
                 }
                 else if(line.startsWith(FOLDER_END))
                 {
                     curFolder = curFolder.getParent();
                 }
                 else
                 {
                     GImage.fromString(curFolder, line, true, this);
                 }
            }
            br.close();
        }
        catch(IOException e)
        {
            logger.error("Cannot read from file \"" + name + "\"!", e);
            return -2;
        }
        logger.info("Done loading! Now the database has {} root folders and {} images", this.folders.size(), this.getNumImages());
        return 0;
    }
    
    public TagDictionary getDictionary()
    {
        return dict;
    }
    
    public void checkFiles()
    {
        ArrayList<GImage> i = this.getImages();
        for(GImage img : i)
        {
            File f = new File(img.getFullPath());
            if(!f.exists())
                System.out.println(img.getFullPath());
        }
    }
    
    public void purgeDatabase() throws IOException
    {
        Iterator<GFolder> j = folders.iterator();
        while (j.hasNext())
        {
            GFolder f = j.next();
            System.out.println("Purging " + f.getPath());
            f.purge(true);
            if(!f.folderExists())
                j.remove();
        }
    }
    
    public void dropOrientationTags()
    {
        for(GFolder f : folders)
            dropOrientationTags(f);
    }
    
    public void dropOrientationTags(GFolder f)
    {
        for(GImage img : f.getImages())
        {
            img.dropTag("vertical");
            img.dropTag("horizontal");
            img.dropTag("mixed");
            img.dropTag("horizontal_forced");
            img.dropTag("vertical_forced");
        }
        for(GFolder fld : f.getSubFolders())
            dropOrientationTags(fld);
    }
    
    public void checkVerticality(double tV, double tH, boolean nameOnly)
    {
        for(GFolder f : folders)
        {
            //System.out.println("--------------------------------------------" + f.getPath());
            f.checkVerticality(tV, tH, nameOnly, true);
        }
    }
    
    public void rotateVertical(double tV, int sleepTime)
    {
        for(GFolder f : folders)
            rotateVertical(f, tV, sleepTime);
    }
    
    public void rotateVertical(GFolder f, double tV, int sleepTime)
    {
        for(GImage img : f.getImages())
        {
            if(img.hasTag("vertical") && Utils.checkImageVertical(img, tV, 1.0) == 1)
            {
                img.rotate(true, sleepTime);
            }
        }
        for(GFolder fld : f.getSubFolders())
            rotateVertical(fld, tV, sleepTime);
    }
    
    /**
     * img1 -link-> img2
     * @param img1 image to link
     * @param img2 image to link to
     */
    public int addToList(GImage img1, GImage img2)
    {
        int res = 0;
        if(img1 == null || img2 == null)
            return 100;
        if(img1.getList() != null)
        {
            res = img1.detachFromList();
            if(res != 0)
                return res-100;
        }
        GImageList list;
        if(img2.getList() == null)
        {
            list = new GImageList();
            this.lists.add(list);
            res = list.addImage(img2);
            if(res != 0)
                return res-200;
        }
        else
            list = img2.getList();
        res = list.addImageAfter(img1, img2);
        if(res != 0)
            return res-300;
        return 0;
    }
    
    public Map<String, String> getAbbrStrings()
    {
        return dict.getAbbrStrings();
    }
    
    public String getTag(String abbr)
    {
        return dict.getTag(abbr);
    }
    
    public void computeSimilarityStrings() throws IOException
    {
        //TODO use Properties
        BufferedWriter bw = new BufferedWriter(new FileWriter(new File("C:\\Test\\file.txt")));
        ArrayList<GImage> images = this.getImages();
        for(int i = 0; i < 4000; i++)
        {
            System.out.print(i+1 + "/" + 4000 + " - ");
            if(!images.get(i).computeSimilarityString())
                bw.write("Problem loading file " + images.get(i).getFullPath() + "\n");
            //else
            //    bw.write(i+1 + "/1000 - " + images.get(i).getSimilarityString().length() + " :: " + images.get(i).getSimilarityString() + "\n");
        }
        bw.close();
        ArrayList<GImage> matches = new ArrayList<>();
        for(int i = 0; i < 4000; i++)
        {
            GImage img = images.get(i);
            //System.out.println(i+1+"/1000 " + img.getFullPath());
            for(int j = i+1; j < 4000; j++)
            {
                int diff = img.differenceFrom(images.get(j), 100, false);
                if(diff < 100 && diff >= 0)
                {
                    if(img.getParent() != images.get(j).getParent())
                    {
                        matches.add(img);
                        matches.add(images.get(j));
                        System.out.println(diff + " " + img.getFullPath() + " :: " + images.get(j).getFullPath());
                    }
                }
            }
        }
        
        if(matches.size()>0)
        {
            Gallery gal = new Gallery(this);
            gal.addImages(matches);

            GalleryViewer gw = new GalleryViewer(this);
            gw.setGallery(gal);
            gw.show();
        }
    }
    
    public void sortFolders()
    {
        Comparator<GFolder> comp = new CreationTimeGFolderComparator();
        Collections.sort(this.folders, comp);
        for(GFolder f : this.folders)
            f.sortSubfolders(comp);
    }
}

class FolderListPair
{
    public ArrayList<GFolder> fldrs, roots;
}