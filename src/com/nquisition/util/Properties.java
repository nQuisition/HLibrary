/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nquisition.util;

import java.io.*;
import java.util.*;

/**
 *
 * @author Master
 */
public class Properties
{
    private static Map<String, String> props = new HashMap<String, String>();
    public static final int FILE_NOT_FOUND = -1;
    public static final int READ_ERROR = -2;
    
    private Properties()
    {
    }
    
    public static int read(String file)
    {
        File f = new File(file);
        try
        {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(f));
            String line;
            while ((line = bufferedReader.readLine()) != null)
            {
                //Format : "propName:prop"
                if(line.startsWith("//"))
                    continue;
                int div = line.indexOf(':');
                if(div == -1)
                    continue;
                String propName = line.substring(0, div);
                String prop = line.substring(div + 1);
                props.put(propName, prop);
            }
            bufferedReader.close();
            return 0;
        }
        catch(FileNotFoundException e)
        {
            return FILE_NOT_FOUND;
        }
        catch(IOException e)
        {
            return READ_ERROR;
        }
    }
    
    public static String get(String propName)
    {
        return props.get(propName);
    }
    
    public static String get(String propName1, String propName2)
    {
        return get(propName1) + get(propName2);
    }
}
