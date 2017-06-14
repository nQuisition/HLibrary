/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nquisition.util;

import com.idrsolutions.image.png.PngEncoder;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import javax.imageio.ImageIO;
import uk.co.jaimon.test.SimpleImageInfo;
import java.util.*;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.*;
import com.drew.metadata.exif.ExifIFD0Directory;
import java.text.SimpleDateFormat;

/**
 *
 * @author Master
 */
public class FileUtils
{
    public static final int NUM_PROCESSES = 8;
    
    private static final int TIMEOUT = 25000;
    private static final int DELAY = 1000;
    
    //TODO too much copy-pasting among different functions
    public static ArrayList<String> rotateImagesBasedOnEXIF(String path, boolean recursive)
    {
        File fldr = new File(path);
        File[] list = fldr.listFiles();
        ArrayList<String> fails = new ArrayList<String>();
        ArrayList<Process> procs = new ArrayList<Process>();
        
        for(File file : list)
        {
            if(file.isDirectory() && recursive)
            {
                ArrayList<String> tempFails = rotateImagesBasedOnEXIF(path.endsWith("\\")?path+file.getName()+"\\":path+"\\"+file.getName()+"\\", recursive);
                System.out.println(tempFails.size());
                if(tempFails.size() > 0)
                    Collections.copy(fails, tempFails);
            }
            else if(isImage(file))
            {
                try
                {
                    Metadata metadata = ImageMetadataReader.readMetadata(file);
                    ExifIFD0Directory dir = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
                    if(dir == null)
                        continue;
                    int orientation = dir.getInt(ExifIFD0Directory.TAG_ORIENTATION);
                    if(orientation != 8)
                        continue;
                    System.out.println(path + file.getName());
                    
                    int idleCount = 0;
                    
                    while(procs.size() >= NUM_PROCESSES)
                    {
                        Iterator<Process> iter = procs.iterator();
                        while (iter.hasNext())
                        {
                            Process p = iter.next();
                            if(!p.isAlive())
                            {
                                p = null;
                                iter.remove();
                            }
                        }
                        idleCount++;
                        Thread.sleep(10);
                    }
                    System.out.println("Idled " + idleCount);
                    
                    Process p = rotateImageEx(path, file.getName(), true);
                    if(p != null)
                        procs.add(p);
                    else if(checkFilePNG(path + file.getName()) != 1)
                        fails.add(file.getName());
                }
                catch(InterruptedException e)
                {
                    
                }
                catch(Exception e)
                {
                    fails.add(file.getName());
                    continue;
                }
            }
        }
        
        return fails;
    }
    
    public static String getValidFolderName(String root, String suggested)
    {
        String res = root + suggested + "\\";
        File temp = new File(res);
        if(!temp.exists())
            return res;
        //TODO temporary fix
        return root + suggested + "!_!_!" + (new SimpleDateFormat("ddMMyy").format(new Date())) + "\\";
    }
    
    public static String unpackFile(String folder, String name)
    {
        String fldr = folder;
        if(!fldr.endsWith("\\"))
            fldr = fldr + "\\";
        String src = "\"" + fldr + name + "\"";
        String res = getValidFolderName(fldr, name.substring(0, name.lastIndexOf('.')).trim());
        String dest = "\"" + res + "\"";
        
        int elapsed = 0;
        RandomAccessFile stream = null;
        while(elapsed <= TIMEOUT)
        {
            try
            {
                stream = new RandomAccessFile(new File(fldr + name), "r");
                stream.close();
                break;
            }
            catch(Exception e)
            {
                System.out.println(e.getMessage());
                elapsed += DELAY;
                try
                {
                    Thread.sleep(DELAY);
                }
                catch(InterruptedException x)
                {
                    
                }
            }
        }
        
        if(elapsed > TIMEOUT)
            return null;
        
        ProcessBuilder pb = new ProcessBuilder("C:\\Program Files\\WinRAR\\WinRAR.exe", "e", "-o-", "-or", "-ibck",
                src, dest);
        pb.redirectErrorStream(true);
        try
        {
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {}

            process.waitFor();
        }
        catch(IOException e)
        {
            return null;
        }
        catch(InterruptedException e)
        {
            return null;
        }
        
        return res;
    }
    
    //TODO move to utility class\
    //DEPRECATED
    public static boolean unzipFile(String folder, String name)
    {
        String fldr = folder;
        if(!fldr.endsWith("\\"))
            fldr = fldr + "\\";
        String src = fldr + name;
        String dest = fldr + name.substring(0, name.lastIndexOf('.')).trim();
        
        ZipFile zipFile;
        try
        {
            zipFile =  new ZipFile(src);
        }
        catch(ZipException e)
        {
            e.printStackTrace();
            return false;
        }
        
        int elapsed = 0;
        
        while(elapsed <= TIMEOUT)
        {
            try
            {
                zipFile.extractAll(dest);
                return true;
            }
            catch(ZipException e)
            {
                e.printStackTrace();
                //System.out.println("Can't unzip! Elapsed " + elapsed);
                elapsed += DELAY;
                try
                {
                    Thread.sleep(DELAY);
                }
                catch(InterruptedException x)
                {
                    
                }
            }
        }
        
        return false;
    }
    
    //TODO move to utility class
    //DEPRECATED
    public static boolean unrarFile(String folder, String name)
    {
        String fldr = folder;
        if(!fldr.endsWith("\\"))
            fldr = fldr + "\\";
        String src = "\"" + fldr + name + "\"";
        String dest = "\"" + fldr + name.substring(0, name.lastIndexOf('.')).trim() + "\\\"";
        
        int elapsed = 0;
        RandomAccessFile stream = null;
        while(elapsed <= TIMEOUT)
        {
            try
            {
                stream = new RandomAccessFile(new File(fldr + name), "r");
                stream.close();
                break;
            }
            catch(Exception e)
            {
                System.out.println(e.getMessage());
                elapsed += DELAY;
                try
                {
                    Thread.sleep(DELAY);
                }
                catch(InterruptedException x)
                {
                    
                }
            }
        }
        
        if(elapsed > TIMEOUT)
            return false;
        
        ProcessBuilder pb = new ProcessBuilder("C:\\Program Files\\WinRAR\\unrar.exe", "e", "-o-", "-or",
                src, dest);
        pb.redirectErrorStream(true);
        try
        {
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {}

            process.waitFor();
        }
        catch(IOException e)
        {
            return false;
        }
        catch(InterruptedException e)
        {
            return false;
        }
        
        return true;
    }
    
    public static boolean isImage(File file)
    {
        if(file.isFile() && (file.getName().toLowerCase().endsWith(".jpg") ||
			    		file.getName().toLowerCase().endsWith(".png") ||
			    		file.getName().toLowerCase().endsWith(".jpeg")))
            return true;
        return false;
    }
    
    public static ArrayList<String> changeAllToWritable(String path)
    {
        File fldr = new File(path);
        File[] list = fldr.listFiles();
        ArrayList<String> fails = new ArrayList<String>();
        
        for(File file : list)
        {
            if(file.isDirectory())
            {
                ArrayList<String> tempFails = changeAllToWritable(path.endsWith("\\")?path+file.getName()+"\\":path+"\\"+file.getName()+"\\");
                Collections.copy(fails, tempFails);
            }
            else if(!file.setWritable(true))
            {
                System.err.println("Cannot change permissions for file " + file.getAbsolutePath());
                fails.add(file.getAbsolutePath());
            }
        }
        
        return fails;
    }
    
    public static void moveAllFiles(String root)
    {
        File fldr = new File(root);
        File[] list = fldr.listFiles();
        
        for(File file : list)
        {
            if(file.isDirectory())
            {
                moveAllFilesFromFolder(file, root);
            }
        }
    }
    
    public static void moveAllFilesFromFolder(File fldr, String root)
    {
        File rf = new File(root);
        File[] list = fldr.listFiles();
        ArrayList<File> dirs = new ArrayList<File>();
        
        for(File file : list)
        {
            if(file.isDirectory())
            {
                dirs.add(file);
                continue;
            }
            
            String name = file.getName();
            String properRoot = root.endsWith("\\")?root:root+"\\";
            
            File target = new File(properRoot + name);
            int counter = 1;
            while(target.exists())
            {
                int pos = file.getName().lastIndexOf('.');
                name = file.getName().substring(0, pos) + " (" + counter + ")" + file.getName().substring(pos);
                counter++;
                target = new File(properRoot + name);
            }
            
            file.renameTo(target);
            file.delete();
        }
        
        for(File file : dirs)
        {
            moveAllFilesFromFolder(file, root);
        }
        
        fldr.delete();
    }
    
    public static ArrayList<String> unconditionalRotateEx(String path, double thresholdV)
    {
        File fldr = new File(path);
        File[] list = fldr.listFiles();
        ArrayList<String> fails = new ArrayList<String>();
        ArrayList<Process> procs = new ArrayList<Process>();
        
        for(File file : list)
        {
            if(file.isDirectory())
            {
                ArrayList<String> tempFails = unconditionalRotateEx(path.endsWith("\\")?path+file.getName()+"\\":path+"\\"+file.getName()+"\\", thresholdV);
                Collections.copy(fails, tempFails);
            }
            else if(isImage(file))
            {
                try
                {
                    String newName = renameImage(path, file.getName(), true);
                    int idleCount = 0;
                    
                    while(procs.size() >= NUM_PROCESSES)
                    {
                        Iterator<Process> iter = procs.iterator();
                        while (iter.hasNext())
                        {
                            Process p = iter.next();
                            if(!p.isAlive())
                            {
                                p = null;
                                iter.remove();
                            }
                        }
                        idleCount++;
                        Thread.sleep(10);
                    }
                    System.out.println("Idled " + idleCount);
                    
                    Process p = rotateImageEx(path, newName, true);
                    if(p != null)
                        procs.add(p);
                    else if(checkFilePNG(path + newName) != 1)
                        fails.add(newName);
                }
                catch(InterruptedException e)
                {
                    
                }
            }
        }
        
        return fails;
    }
    
    public static ArrayList<String> conditionalRotateEx(String path, double thresholdV)
    {
        File fldr = new File(path);
        File[] list = fldr.listFiles();
        ArrayList<String> fails = new ArrayList<String>();
        ArrayList<Process> procs = new ArrayList<Process>();
        
        for(File file : list)
        {
            if(file.isDirectory())
            {
                ArrayList<String> tempFails = conditionalRotateEx(path.endsWith("\\")?path+file.getName()+"\\":path+"\\"+file.getName()+"\\", thresholdV);
                Collections.copy(fails, tempFails);
            }
            else if(isImage(file))
            {
                try
                {
                    if(checkImageVertical(file.getCanonicalPath(), thresholdV) == 0)
                        continue;
                    String newName = renameImage(path, file.getName(), true);
                    int idleCount = 0;
                    
                    while(procs.size() >= NUM_PROCESSES)
                    {
                        Iterator<Process> iter = procs.iterator();
                        while (iter.hasNext())
                        {
                            Process p = iter.next();
                            if(!p.isAlive())
                            {
                                p = null;
                                iter.remove();
                            }
                        }
                        idleCount++;
                        Thread.sleep(10);
                    }
                    System.out.println("Idled " + idleCount);
                    
                    Process p = rotateImageEx(path, newName, true);
                    if(p != null)
                        procs.add(p);
                    else if(checkFilePNG(path + newName) != 1)
                        fails.add(newName);
                }
                catch(IOException e)
                {
                    fails.add(file.getName());
                    e.printStackTrace();
                }
                catch(InterruptedException e)
                {
                    
                }
            }
        }
        
        return fails;
    }
    
    public static ArrayList<String> conditionalRotate(String path, int sleepTime, double thresholdV)
    {
        File fldr = new File(path);
        File[] list = fldr.listFiles();
        ArrayList<String> fails = new ArrayList<String>();
        for(File file : list)
        {
            if(file.isDirectory())
            {
                ArrayList<String> tempFails = conditionalRotate(path.endsWith("\\")?path+file.getName()+"\\":path+"\\"+file.getName()+"\\", sleepTime, thresholdV);
                Collections.copy(fails, tempFails);
            }
            else if(isImage(file))
            {
                try
                {
                    if(checkImageVertical(file.getCanonicalPath(), thresholdV) == 0)
                        continue;
                    String newName = renameImage(path, file.getName(), true);
                    if(rotateImage(path, newName, true, sleepTime) != 0)
                        fails.add(newName);
                }
                catch(IOException e)
                {
                    fails.add(file.getName());
                    e.printStackTrace();
                }
            }
        }
        
        return fails;
    }
    
    public static int checkImageVertical(String fullPath, double thresholdV) throws IOException
    {
        SimpleImageInfo imageInfo = new SimpleImageInfo(new File(fullPath));
        double ratio = (double)imageInfo.getWidth()/imageInfo.getHeight();
        if(ratio <= thresholdV)
            return 1;
        return 0;
    }
    
    public static String renameImage(String path, String name, boolean left)
    {
        String properPath = path;
        if(!properPath.endsWith("\\"))
            properPath = properPath + "\\";
        String fullPath = properPath + name;
        String cname = name;

        File src = new File(fullPath);
        if(left)
        {
            if(!(cname.startsWith("vert_") || cname.startsWith("末vert_")))
                if(src.renameTo(new File(properPath + "末vert_" + cname)))
                    cname = "末vert_" + cname;
        }
        else if(cname.startsWith("vert_") || cname.startsWith("末vert_"))
        {
            String nm = cname.substring(cname.indexOf("_")+1);
            if(src.renameTo(new File(properPath + nm)))
                cname = nm;
        }
        
        return cname;
    }
    
    public static int checkFilePNG(String fullPath)
    {
        try
        {
            File src = new File(fullPath);
            SimpleImageInfo imageInfo = new SimpleImageInfo(src);
            if(imageInfo.getMimeType().equalsIgnoreCase("image/png"))
                return 1;
            return 0;
        }
        catch(IOException e)
        {
            return -1;
        }
    }
    
    public static Process rotateImageEx(String path, String name, boolean left)
    {
        try
        {
            String properPath = path;
            if(!properPath.endsWith("\\"))
                properPath = properPath + "\\";
            String fullPath = properPath + name;
        
            File src = new File(fullPath);
            System.out.println("Rotating " + fullPath);

            SimpleImageInfo imageInfo = new SimpleImageInfo(src);

            if(imageInfo.getMimeType().equalsIgnoreCase("image/jpeg"))
            {
                int angle = left ? 270 : 90;
                //TODO make a config variable for this utility location
                Process p = Runtime.getRuntime().exec("D:\\jpegtran -rotate " +
                        angle + " \"" + fullPath + "\" \"" + fullPath + "\"");
                return p;
            }
            if(imageInfo.getMimeType().equalsIgnoreCase("image/png"))
            {
                try
                {
                    PngEncoder encoder = new PngEncoder();
                    BufferedImage img = ImageIO.read(src);
                    int w = img.getWidth();
                    int h = img.getHeight();
                    BufferedImage rotated = new BufferedImage(h, w, img.getType());
                    AffineTransform transform = new AffineTransform();
                    Graphics2D g = rotated.createGraphics();
                    transform.translate(0, w);
                    double angle = left ? -Math.PI/2 : Math.PI/2;
                    transform.rotate(angle);
                    g.drawImage(img, transform, null);
                    OutputStream out = new FileOutputStream(src);
                    encoder.write(rotated, out);
                    out.close();
                }
                catch(IllegalArgumentException e)
                {
                    return null;
                }
            }
        }
        catch(IOException e)
        {
            return null;
        }
        return null;
    }
    
    public static int rotateImage(String path, String name, boolean left, int sleepTime)
    {
        try
        {
            String properPath = path;
            if(!properPath.endsWith("\\"))
                properPath = properPath + "\\";
            String fullPath = properPath + name;
        
            File src = new File(fullPath);
            System.out.println("Rotating " + fullPath);

            SimpleImageInfo imageInfo = new SimpleImageInfo(src);

            if(imageInfo.getMimeType().equalsIgnoreCase("image/jpeg"))
            {
                int angle = left ? 270 : 90;
                //TODO make a config variable for this utility location
                Process p = Runtime.getRuntime().exec("D:\\jpegtran -rotate " +
                        angle + " \"" + fullPath + "\" \"" + fullPath + "\"");
                if(sleepTime < 0)
                    p.waitFor();
                else
                    Thread.sleep(sleepTime);
            }
            if(imageInfo.getMimeType().equalsIgnoreCase("image/png"))
            {
                try
                {
                    PngEncoder encoder = new PngEncoder();
                    BufferedImage img = ImageIO.read(src);
                    int w = img.getWidth();
                    int h = img.getHeight();
                    BufferedImage rotated = new BufferedImage(h, w, img.getType());
                    AffineTransform transform = new AffineTransform();
                    Graphics2D g = rotated.createGraphics();
                    transform.translate(0, w);
                    double angle = left ? -Math.PI/2 : Math.PI/2;
                    transform.rotate(angle);
                    g.drawImage(img, transform, null);
                    OutputStream out = new FileOutputStream(src);
                    encoder.write(rotated, out);
                    out.close();
                }
                catch(IllegalArgumentException e)
                {
                    return -1;
                }
            }
        }
        catch(IOException e)
        {
            return -2;
        }
        catch(InterruptedException e)
        {
            return -3;
        }
        return 0;
    }
}
