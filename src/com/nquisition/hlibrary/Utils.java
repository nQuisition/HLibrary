/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nquisition.hlibrary;

import com.nquisition.hlibrary.model.GImage;
import java.io.*;
import java.util.*;
import java.nio.file.*;
import uk.co.jaimon.test.SimpleImageInfo;

/**
 *
 * @author Master
 */
public class Utils
{
    public static final int TYPE_ALL = 0;
    public static final int TYPE_IMAGE = 1;
    
    public static ArrayList<String> getAllFilesRec(
            String path, boolean recursive, int type)
            throws IOException
    {
        File p = new File(path);
        return Utils.getAllFilesRec(p, recursive, type);
    }
    
    public static ArrayList<String> getAllFilesRec(
            File p, boolean recursive, int type)
            throws IOException
    {
        ArrayList<String> res = new ArrayList<String>();
        File[] listOfFiles = p.listFiles();
        //Arrays.sort(listOfFiles);
        
        for (File file : listOfFiles)
        {
            if (file.isFile())
            {
                String name = file.getName().toLowerCase();
                if(type == TYPE_ALL || (type == TYPE_IMAGE && (name.endsWith(".jpg")
                        || name.endsWith(".jpeg") || name.endsWith(".png"))))
                {
                    //System.out.println(file.getCanonicalPath());
                    res.add(file.getCanonicalPath());
                }
            }
            else if(recursive && file.isDirectory())
            {
                ArrayList<String> temp = Utils.getAllFilesRec(file, recursive, type);
                for(String fn : temp)
                    res.add(fn);
            }
        }
        
        return res;
    }
    
    public static void moveFiles(String src, String dest, String key)
            throws IOException
    {
        moveFiles(src, dest, key, dest);
    }
    
    public static void moveFiles(String src, String dest, String key, String origin)
            throws IOException
    {
        if(src.startsWith(origin))
            return;
        File p = new File(src);
        File d = new File(dest);
        boolean dirMade = false;
        
        File[] listOfFiles = p.listFiles();
        //Arrays.sort(listOfFiles);
        
        for (File file : listOfFiles)
        {
            if (file.isFile())
            {
                if(file.getName().startsWith(key))
                {
                    if(!dirMade)
                        d.mkdirs();
                    try
                    {
                        Files.move(file.toPath(), Paths.get(dest + "\\" + file.getName()));
                    }
                    catch(FileAlreadyExistsException e)
                    {
                        String fn = file.getName();
                        String name = fn.substring(0,fn.lastIndexOf('.'));
                        String ext = fn.substring(fn.lastIndexOf('.') + 1);
                        name = name + " " + System.currentTimeMillis() + ext;
                        Files.move(file.toPath(), Paths.get(dest + "\\" + name));
                    }
                }
            }
            else if(file.isDirectory())
            {
                System.out.println(file.getCanonicalPath() + " ==> " + dest+"\\"+file.getName());
                moveFiles(file.getCanonicalPath(), dest+"\\"+file.getName(), key, origin);
            }
        }
    }
    
    public static int checkImageVertical(GImage img, double thresholdV, double thresholdH)
    {
        try
        {
            if(thresholdV <= 0 || thresholdH <= 0)
                return -2;
            SimpleImageInfo imageInfo = new SimpleImageInfo(new File(img.getFullPath()));
            double ratio = (double)imageInfo.getWidth()/imageInfo.getHeight();
            if(ratio <= thresholdV)
                return 1;
            if(ratio >= thresholdH)
                return -1;
            return 0;
        }
        catch(IOException e)
        {
            return -3;
        }
    }
    
    public static String getFileName(String fullPath)
    {
        int pos = fullPath.lastIndexOf('\\');
        if(pos > 0)
            return fullPath.substring(pos + 1);
        return null;
    }
    
    public static String getFilePath(String fullPath)
    {
        int pos = fullPath.lastIndexOf('\\');
        if(pos > 0)
            return fullPath.substring(0, pos + 1);
        return null;
    }
}
