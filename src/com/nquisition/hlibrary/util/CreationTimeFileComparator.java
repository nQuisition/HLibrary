/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nquisition.hlibrary.util;

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
public class CreationTimeFileComparator implements Comparator<File>
{
    public int compare(File f1, File f2)
    {
        try
        {
            FileTime ft1 = Files.readAttributes(f1.toPath(), BasicFileAttributes.class).creationTime();
            FileTime ft2 = Files.readAttributes(f2.toPath(), BasicFileAttributes.class).creationTime();
            return ft1.compareTo(ft2);
        }
        catch(InvalidPathException e)
        {
            return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
        }
        catch(IOException e)
        {
            return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
        }
    }
}
