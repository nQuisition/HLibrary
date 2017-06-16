/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nquisition.hlibrary.util;

import com.nquisition.hlibrary.model.GFolder;
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
public class CreationTimeGFolderComparator implements Comparator<GFolder>
{
    public int compare(GFolder folder1, GFolder folder2)
    {
        File f1 = new File(folder1.getPath());
        File f2 = new File(folder2.getPath());
        //TODO ughhhhh
        String l1 = folder1.getPath().substring(0, folder1.getPath().length()-1);
        String l2 = folder2.getPath().substring(0, folder2.getPath().length()-1);
        l1 = l1.substring(l1.lastIndexOf("\\") + 1);
        l2 = l2.substring(l2.lastIndexOf("\\") + 1);
        if(l1.startsWith("Новая папка") && l2.startsWith("Новая папка"))
        {
            return new WindowsExplorerStringComparator().compare(folder1.getPath(), folder2.getPath());
        }
        //
        
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
