/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nquisition.hlibrary.model;

import java.util.*;
import java.io.*;

/**
 *
 * @author Master
 */
public class TagDictionary
{
    public static final String SEPARATOR = "?";
    private Map<String, String> dict;
    private String location;
    
    public void init()
    {
        dict = new TreeMap<String, String>();
    }
    
    public TagDictionary()
    {
        init();
    }
    
    public void setLocation(String l)
    {
        location = l;
    }
    
    public String getLocation()
    {
        return location;
    }
    
    public String getTag(String abbr)
    {
        if(abbr == null)
            return null;
        abbr = abbr.toLowerCase();
        String res = dict.get(abbr);
        return res==null?abbr:res;
    }
    
    public void removeAbbr(String abbr)
    {
        dict.remove(abbr);
    }
    
    public int addAbbreviation(String abbr, String tag)
    {
        abbr = abbr.toLowerCase();
        tag = tag.toLowerCase();
        if(dict.containsKey(abbr))
            return -1;
        dict.put(abbr, tag);
        return 1;
    }
    
    public void saveDict() throws IOException
    {
        File file = new File(location);
        BufferedWriter bw = new BufferedWriter(new FileWriter(file));
        for (Map.Entry<String, String> entry : dict.entrySet())
        {
            String k = entry.getKey();
            String v = entry.getValue();
            bw.write(k+"?"+v+"\n");
        }
        bw.close();
    }
    
    public void loadDict() throws IOException
    {
        init();
        BufferedReader br = new BufferedReader(new FileReader(new File(location)));
        String line;
        while((line = br.readLine()) != null)
        {
            String k = line.substring(0, line.indexOf(SEPARATOR));
            String v = line.substring(line.indexOf(SEPARATOR) + SEPARATOR.length());
            this.addAbbreviation(k, v);
        }
    }
    
    public void sort()
    {
        
    }
    
    public Map<String, String> getAbbrStrings()
    {
        return dict;
    }
}
