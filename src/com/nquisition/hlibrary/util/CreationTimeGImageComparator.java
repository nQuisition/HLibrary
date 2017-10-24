/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nquisition.hlibrary.util;

import com.nquisition.hlibrary.model.GFolder;
import com.nquisition.hlibrary.model.GImage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;

/**
 *
 * @author Master
 */
public class CreationTimeGImageComparator implements Comparator<GImage>
{
    public int compare(GImage folder1, GImage folder2)
    {
        File f1 = new File(folder1.getFullPath());
        File f2 = new File(folder2.getFullPath());
        
        try
        {
            FileTime ft1 = Files.readAttributes(f1.toPath(), BasicFileAttributes.class).creationTime();
            FileTime ft2 = Files.readAttributes(f2.toPath(), BasicFileAttributes.class).creationTime();
            return ft1.compareTo(ft2);
        }
        catch(InvalidPathException e)
        {
            //System.out.println("OIOIOI!! \'" + f1.getName() + "\' :: \'" + f2.getName() + "\'");
            return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
        }
        catch(IOException e)
        {
            //System.out.println("OOPS!! \'" + f1.getName() + "\' :: \'" + f2.getName() + "\'");
            return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
        }
    }
}
