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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

import javax.imageio.ImageIO;
import uk.co.jaimon.test.SimpleImageInfo;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

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
public final class FileUtils {
    private static final double VERTICALITY_THRESHOLD = 1.0d;
    
    private static final int TIMEOUT = 25000;
    private static final int DELAY = 1000;
    private static final int PROCESS_DELAY = 10;
    
    private FileUtils() { }
    
    public static void forEachFolderIn(File folder, boolean recursive, 
			Predicate<File> filter, Consumer<File> action) {
		File[] listOfFiles = folder.listFiles();
	    
	    for(File file : listOfFiles) {
	        if(file.isDirectory()) {
	        	if(recursive)
	            	forEachFolderIn(file, recursive, filter, action);
	        	if(filter == null || filter.test(file))
	        		action.accept(file);
	        }
	    }
	}

	public static void forEachFileIn(File folder, boolean recursive, 
			Predicate<File> filter, Consumer<File> action) {
		File[] listOfFiles = folder.listFiles();
	    
	    for(File file : listOfFiles) {
	        if(file.isDirectory() && recursive)
	        	forEachFileIn(file, recursive, filter, action);
	        else if(filter == null || filter.test(file))
	        	action.accept(file);
	    }
	}

	public static String toProperPath(String path) {
		return path.endsWith("\\")?path:path+"\\";
	}

	//TODO implement naming strategies, not just "<name> - <date>"
	public static String getAvailableFolderName(String rootName, String suggestedName) {
		//TODO also need to eliminate all weird symbols - introduce a class that will take care of
		//this name processing
		String processed = suggestedName.trim().replaceAll(" +", " ");
		String res = rootName + processed + "\\";
	    File temp = new File(res);
	    if(!temp.exists())
	        return res;
	    //TODO temporary fix
	    return rootName + processed + " - " + (new SimpleDateFormat("ddMMyy").format(new Date())) + "\\";
	}

	public static String getAvailableFileName(File root, File file) {
		return getAvailableFileName(root.getAbsolutePath(), file.getName());
	}

	//TODO implement naming strategies, not just "<name> (<number>)"
	public static String getAvailableFileName(String rootPath, String fileName) {
		String properRoot = toProperPath(rootPath);
		File target = new File(properRoot + fileName);
		String name = fileName;
	    int counter = 1;
	    while(target.exists()) {
	        int pos = fileName.lastIndexOf('.');
	        name = fileName.substring(0, pos) + " (" + counter + ")" + fileName.substring(pos);
	        counter++;
	        target = new File(properRoot + name);
	    }
	    return properRoot + name;
	}

	public static boolean checkImageVertical(File file, double thresholdV) throws IOException {
	    SimpleImageInfo imageInfo = new SimpleImageInfo(file);
	    double ratio = (double)imageInfo.getWidth()/imageInfo.getHeight();
	    return (ratio < thresholdV);
	}

	//TODO not a good idea to throw general exception
	public static boolean checkImageEXIFVertical(File file) throws Exception {
	    Metadata metadata = ImageMetadataReader.readMetadata(file);
	    ExifIFD0Directory dir = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
	    if(dir == null)
	        return false;
	    int orientation = dir.getInt(ExifIFD0Directory.TAG_ORIENTATION);
	    return (orientation == 8);
	}

	public static int checkFilePNG(File file) {
	    try {
	        SimpleImageInfo imageInfo = new SimpleImageInfo(file);
	        if(imageInfo.getMimeType().equalsIgnoreCase("image/png"))
	            return 1;
	        return 0;
	    } catch(IOException e) {
	        return -1;
	    }
	}

	public static boolean isImage(File file) {
	    return (file.isFile() && (file.getName().toLowerCase().endsWith(".jpg") ||
			    		file.getName().toLowerCase().endsWith(".png") ||
			    		file.getName().toLowerCase().endsWith(".jpeg")));
	}

	public static boolean isVerticalImage(File file) {
		try {
	    	if(isImage(file))
	    		return checkImageVertical(file, VERTICALITY_THRESHOLD);
		} catch(IOException e) { return false ; }
		return false;
	}

	public static boolean isEXIFVerticalImage(File file) {
		try {
	    	if(isImage(file))
	    		return checkImageEXIFVertical(file);
		} catch(Exception e) { return false ; }
		return false;
	}

	public static String renameImageBasedOnVerticality(String path, String name, boolean left) {
	    String properPath = toProperPath(path);
	    String fullPath = properPath + name;
	    String cname = name;
	
	    File src = new File(fullPath);
	    if(left) {
	        if(!(cname.startsWith("vert_") || cname.startsWith("末vert_")))
	            if(src.renameTo(new File(properPath + "末vert_" + cname)))
	                cname = "末vert_" + cname;
	    } else if(cname.startsWith("vert_") || cname.startsWith("末vert_")) {
	        String nm = cname.substring(cname.indexOf("_")+1);
	        if(src.renameTo(new File(properPath + nm)))
	            cname = nm;
	    }
	    
	    return cname;
	}

	public static Process rotateImageEx(String path, String name, boolean left) {
	    try {
	        String properPath = toProperPath(path);
	        String fullPath = properPath + name;
	    
	        File src = new File(fullPath);
	        System.out.println("Rotating " + fullPath);
	
	        SimpleImageInfo imageInfo = new SimpleImageInfo(src);
	
	        if(imageInfo.getMimeType().equalsIgnoreCase("image/jpeg")) {
	            int angle = left ? 270 : 90;
	            //TODO make a config variable for this utility location
	            System.out.println("Exexuting " + "D:\\jpegtran -rotate " +
	                    angle + " \"" + fullPath + "\" \"" + fullPath + "\"");
	            return Runtime.getRuntime().exec("D:\\jpegtran -rotate " +
	                    angle + " \"" + fullPath + "\" \"" + fullPath + "\"");
	        } else if(imageInfo.getMimeType().equalsIgnoreCase("image/png")) {
	            try {
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
	            } catch(IllegalArgumentException e) { return null; }
	        }
	    }
	    catch(IOException e) { return null; }
	    return null;
	}

	public static List<String> rotateImagesSatisfying(String path, Predicate<File> condition,
			boolean rename, boolean recursive) {
	    File folder = new File(path);
	    List<String> fails = new ArrayList<>();
	    ProcessQueue queue = new ProcessQueue();
	    
	    forEachFileIn(folder, recursive, condition, file -> {
	    	String fileName = file.getName();
	    	if(rename)
	    		fileName = renameImageBasedOnVerticality(path, fileName, true);
	    	
	        queue.waitForSlot(PROCESS_DELAY);
	        
	        Process p = rotateImageEx(path, fileName, true);
	        if(p != null)
	            queue.add(p);
	        else if(checkFilePNG(file) != 1)
	            fails.add(fileName);
	    });
	    
	    return fails;
	}

	public static List<String> rotateImagesBasedOnEXIF(String path, boolean recursive) {
	    return rotateImagesSatisfying(path, FileUtils::isEXIFVerticalImage, false, recursive);
	}

	public static List<String> unconditionalRotateEx(String path) {
		return rotateImagesSatisfying(path, FileUtils::isImage, true, true);
	}

	public static List<String> conditionalRotateEx(String path) {
	    return rotateImagesSatisfying(path, FileUtils::isVerticalImage, true, true);
	}

	public static String unpackFile(String folderName, String fileName) {
        String properPath = toProperPath(folderName);
        String src = "\"" + properPath + fileName + "\"";
        String res = getAvailableFolderName(properPath, fileName.substring(0, fileName.lastIndexOf('.')).trim());
        String dest = "\"" + res + "\"";
        
        int elapsed = 0;
        RandomAccessFile stream = null;
        while(elapsed <= TIMEOUT) {
            try {
                stream = new RandomAccessFile(new File(properPath + fileName), "r");
                stream.close();
                break;
            } catch(Exception e) {
                elapsed += DELAY;
                try {
                    Thread.sleep(DELAY);
                } catch(InterruptedException x) { }
            }
        }
        
        if(elapsed > TIMEOUT)
            return null;
        
        ProcessBuilder pb = new ProcessBuilder("C:\\Program Files\\WinRAR\\WinRAR.exe", "e", "-o-", "-or", "-ibck",
                src, dest);
        pb.redirectErrorStream(true);
        try {
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {}

            process.waitFor();
        } catch(IOException e) {
            return null;
        } catch(InterruptedException e) {
            return null;
        }
        if(res.endsWith("\\"))
        	res = res.substring(0, res.length()-1);
        return res.substring(res.lastIndexOf('\\')+1);
    }
    
    public static List<String> changeAllToWritable(File folder) {
	    List<String> fails = new ArrayList<>();
	    forEachFileIn(folder, true, null, file -> {
	    	if(!file.setWritable(true)) {
	            System.err.println("Cannot change permissions for file " + file.getAbsolutePath());
	            fails.add(file.getAbsolutePath());
	        }
	    });
	    
	    return fails;
	}

	public static void moveFileToFolder(File file, File destination) {
    	String targetName = getAvailableFileName(destination, file);
        file.renameTo(new File(targetName));
        file.delete();
    }
    
    public static void moveAllFilesFromFolder(File folder, File destination) {
	    forEachFileIn(folder, false, null, file -> moveFileToFolder(file, destination) );
	}

	public static void moveAllFiles(File root) {
	    forEachFolderIn(root, true, null, folder -> moveAllFilesFromFolder(folder, root) );
	    forEachFolderIn(root, true, null, folder -> folder.delete() );
	}

	public static FileTime getCreationDate(String path) {
		FileTime ft = null;
		try {
	    	File file = new File(path);
	    	ft = Files.readAttributes(file.toPath(), BasicFileAttributes.class).creationTime();
		} catch(IOException e) {
			e.printStackTrace();
		}
		return ft;
	}

	public static void setFileCreationDate(String filePath, FileTime creationDate) {
		try {
	        Path p = Paths.get(filePath);
	        Files.setAttribute(p, "creationTime", creationDate);
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	public static void setFileCreationDate(String filePath, Date creationDate) {
	       FileTime time = FileTime.fromMillis(creationDate.getTime());
	       setFileCreationDate(filePath, time);
	}

	public static void createFolderWithDate(String path, FileTime creationDate) {
    	File dir = new File(path);
    	dir.mkdirs();
    	setFileCreationDate(path, creationDate);
    }
    
    public static void createFolderWithFileDate(String path, String filePath) {
	    createFolderWithDate(path, getCreationDate(filePath));
    }

	@Deprecated
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

	@Deprecated
	public static List<String> conditionalRotate(String path, int sleepTime, double thresholdV)
	{
	    File fldr = new File(path);
	    File[] list = fldr.listFiles();
	    List<String> fails = new ArrayList<String>();
	    for(File file : list)
	    {
	        if(file.isDirectory())
	        {
	            List<String> tempFails = conditionalRotate(path.endsWith("\\")?path+file.getName()+"\\":path+"\\"+file.getName()+"\\", sleepTime, thresholdV);
	            Collections.copy(fails, tempFails);
	        }
	        else if(isImage(file))
	        {
	            try
	            {
	                if(!checkImageVertical(file, thresholdV))
	                    continue;
	                String newName = renameImageBasedOnVerticality(path, file.getName(), true);
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

	@Deprecated
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

	@Deprecated
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
}
