/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nquisition.hlibrary.util;

import java.util.*;
import java.io.*;

/**
 *
 * @author Master
 */
public class WindowsExplorerFileComparator implements Comparator<File>
{
    public int compare(File s1, File s2)
    {
        WindowsExplorerStringComparator w = new WindowsExplorerStringComparator();
        return w.compare(s1.getName(), s2.getName());
    }
}