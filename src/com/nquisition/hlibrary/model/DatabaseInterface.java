package com.nquisition.hlibrary.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.nquisition.hlibrary.HLibrary;
import com.nquisition.hlibrary.api.IDatabaseInterface;
import com.nquisition.hlibrary.api.ProgressManager;
import com.nquisition.hlibrary.api.ProgressMonitor;

public class DatabaseInterface implements IDatabaseInterface
{
	private static final Logger logger = LogManager.getLogger(DatabaseInterface.class);
	private Database activeDatabase;
	
	public DatabaseInterface(Database db)
	{
		activeDatabase = db;
	}
	
	public Database getActiveDatabase()
	{
		return activeDatabase;
	}
	
	public boolean loadDatabase(String fileName, boolean local) {
		//check if file is JSON
		boolean isJson = false;
		try(BufferedReader in = new BufferedReader(new FileReader(fileName))) {
			String line = in.readLine();
			if(line.startsWith("{"))
				isJson = true;
		} catch(FileNotFoundException e) {
			//file not found
			activeDatabase = null;
			return false;
		} catch(IOException e) {
			//file is empty or exception when closing
			activeDatabase = null;
			return false;
		}
		if(isJson) {
			boolean res = readDatabaseFromJson(fileName, local);
			//No images after loading -> fail
			//TODO maybe change this and allow databases to have some other metadata (like its location)
			if(!res || activeDatabase.getImages().size() <= 0)
				return false;
		} else {
			//Legacy format
			activeDatabase = new Database(local);
			activeDatabase.setLocation(fileName);
	
	        //TODO deffinitely there are better actions if it fails
	        if(activeDatabase.loadDatabase() < 0) {
	            logger.fatal("Unable to load database!");
	            activeDatabase = null;
	            return false;
	        }
		}
		activeDatabase.sortFolders();
        return true;
	}
	
	//TODO use this in loadDatabase when the specified file doesn't exist?
	public boolean createDatabase(String location, boolean local) {
		File f = new File(location);
		if(f.exists())
			return false;
		try {
			f.createNewFile();
		} catch (IOException e) {
			logger.warn("An exception has occured", e);
			return false;
		}
		activeDatabase = new Database(local);
		activeDatabase.setLocation(location);
		return true;
	}
	
	public boolean saveDatabase() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
        	activeDatabase.nullifyEmptyStrings();
        	String json = gson.toJson(activeDatabase);
        	PrintWriter out = new PrintWriter(activeDatabase.getLocation());
        	out.write(json);
        	out.close();
        } catch(IOException e) {
        	logger.warn("Saving database failed!", e);
        	return false;
        }
        return true;
	}
	
	@Override
	public List<GFolder> getFolders() {
		return new ArrayList<>(activeDatabase.getFolders());
	}
	
	@Override
	public List<GImage> getImages() {
		return new ArrayList<>(activeDatabase.getImages());
	}
	
	public void info() {
		activeDatabase.info();
	}
	
	@Override
	public boolean addDirectory(String path, int depth) {
		return activeDatabase.addDirectory(path, depth) >= 0;
	}
	
	//TODO memory leaks?
	public Callable<Integer> computeSimilarityStrings() {
		List<GImage> images = this.getImages(); 
		ProgressMonitor progressMonitor = HLibrary.requestProgressMonitor("Computing similarity strings");
		progressMonitor.start(images.size());
		
		//TODO use javafx tasks?
		return () -> {
			images.stream().parallel().forEach(img -> {
	        	//img.setSimilarityBytes(null);
	        	progressMonitor.add(1);
	        	try {
					img.computeSimilarity(false);
				} catch (IOException e) {
					e.printStackTrace();
				}
	        });
			return 1;
		};
		
		/*new Thread() {
			@Override
			public void run() {
				images.stream().parallel().forEach((img) -> {
		        	//img.setSimilarityBytes(null);
		        	progressMonitor.add(1);
		        	int res = count.addAndGet(1);
		        	if(res % 10 == 0)
		        		System.out.println(res + "/" + images.size());
		        	try {
						img.computeSimilarity(false);
					} catch (IOException e) {
						e.printStackTrace();
					}
		        });
			}
		}.start();*/
	}
	
	//TODO memory leaks?
	//TODO compute similarity or leave like this?
	//Assumes similarity is computed; skips images without similarity info
	public Callable<Map<GImage, List<GImage>>> findSimilarImages(int threshold) {
		List<GImage> images = this.getImages();
		ProgressMonitor progressMonitor = HLibrary.requestProgressMonitor("Finding similar images");
		progressMonitor.start(images.size());
		
		//TODO detect sketches and ignore them
		return () -> {
			Map<GImage, List<GImage>> res = new HashMap<>();
			images.stream().parallel().forEach(img -> {
				progressMonitor.add(1);
	            for(int j = 0; j < images.size(); j++) {
	            	GImage image2 = images.get(j);
	            	
	            	if(img == image2)
	            		continue;
	                int diff = img.differenceFrom(images.get(j), threshold, false);
	                if(diff < threshold && diff >= 0) {
	                    if(img.getParent() != images.get(j).getParent() || diff == 0) {
	                    	if(!res.containsKey(img))
	                    		res.put(img, new ArrayList<>());
	                    	res.get(img).add(images.get(j));
	                    }
	                }
	            }
	        });
			return res;
		};
	}
	
	public boolean readDatabaseFromJson(String fileName, boolean local)
	{
		JsonReader reader;
		try
		{
			reader = new JsonReader(new InputStreamReader(new FileInputStream(fileName)));
		}
		catch(FileNotFoundException e)
		{
			logger.warn("File not found " + fileName, e);
			return false;
		}
		
		try
		{
			activeDatabase = new Database(local);
			activeDatabase.setLocation(fileName);
			reader.beginObject();
			while(reader.hasNext())
			{
				String name = reader.nextName();
				if(name.equals("folders"))
				{
					reader.beginArray();
					while(reader.hasNext())
					{
						activeDatabase.addGFolder(readGFolderFromJson(reader), false);
					}
					reader.endArray();
				}
				else if(name.equals("dict"))
				{
					activeDatabase.setTagDictionary(readTagDictionaryFromJson(reader));
		    	}
				else
				{
		    	   reader.skipValue();
		    	}
		     }
		     reader.endObject();
		}
		catch(IOException e)
		{
			logger.warn("An exception has occured", e);
			return false;
		}
		finally
		{
			try
			{
				reader.close();
			}
			catch(IOException e)
			{
				logger.warn("An exception has occured", e);
			}
		}
		return true;
	}
	
	private static TagDictionary readTagDictionaryFromJson(JsonReader reader) throws IOException
	{
		TagDictionary dictionary = new TagDictionary();
		reader.beginObject();
		while(reader.hasNext())
		{
			String name = reader.nextName();
			if(name.equals("dict"))
			{
				reader.beginObject();
				while(reader.hasNext())
				{
					String abbr = reader.nextName();
					String tag = reader.nextString();
					dictionary.addAbbreviation(abbr, tag);
				}
				reader.endObject();
			}
			else
			{
	    	   reader.skipValue();
	    	}
		}
		reader.endObject();
		return dictionary;
	}
	
	private GFolder readGFolderFromJson(JsonReader reader) throws IOException
	{
		GFolder folder = new GFolder();
		
		reader.beginObject();
		while(reader.hasNext())
		{
			String name = reader.nextName();
			if(name.equals("path"))
			{
				folder.setPath(reader.nextString());
			}
			else if(name.equals("alias"))
			{
				folder.setAlias(reader.nextString());
			}
			else if(name.equals("images"))
			{
				reader.beginArray();
				while(reader.hasNext())
				{
					folder.addImage(readGImageFromJson(reader, folder));
				}
				reader.endArray();
			}
			else if(name.equals("subfolders"))
			{
				reader.beginArray();
				while(reader.hasNext())
				{
					folder.addSubFolder(readGFolderFromJson(reader));
				}
				reader.endArray();
			}
			else
			{
				processGEntryField(folder, name, reader);
	    	}
		}
		reader.endObject();
		
		return folder;
	}
	
	private static GImage readGImageFromJson(JsonReader reader, GFolder parent) throws IOException
	{
		GImage image = new GImage();
		image.setParent(parent);
		
		reader.beginObject();
		while(reader.hasNext())
		{
			String name = reader.nextName();
			if(name.equals("id"))
			{
				image.setId(reader.nextInt());
			}
			else if(name.equals("name"))
			{
				image.setName(reader.nextString());
			}
			else if(name.equals("similarityString"))
			{
				image.similarityFromString(reader.nextString());
			}
			else
			{
				processGEntryField(image, name, reader);
	    	}
		}
		reader.endObject();
		
		return image;
	}
	
	private static void processGEntryField(GEntry entry, String fieldName, JsonReader reader) throws IOException
	{
		if(fieldName.equals("comment"))
		{
			entry.setComment(reader.nextString());
		}
		else if(fieldName.equals("added"))
		{
			entry.setAdded(reader.nextLong());
		}
		else if(fieldName.equals("lastmod"))
		{
			entry.setLastmod(reader.nextLong());
		}
		else if(fieldName.equals("viewed"))
		{
			entry.setViewed(reader.nextLong());
		}
		else if(fieldName.equals("viewcount"))
		{
			entry.setViewcount(reader.nextInt());
		}
		else if(fieldName.equals("tags"))
		{
			ArrayList<String> tags = new ArrayList<>();
			reader.beginArray();
			while(reader.hasNext())
			{
				tags.add(reader.nextString());
			}
			reader.endArray();
			entry.setTags(tags, false);
		}
		else if(fieldName.equals("customProps")) {
			processCustomProps(entry, reader);
		} else {
			reader.skipValue();
		}
	}
	
	public static void processCustomProps(GEntry entry, JsonReader reader) throws IOException {
		reader.beginObject();
		while(reader.hasNext()) {
			String fieldName = reader.nextName();
			HLibrary.getPropManager().readPropertyFromJson(entry, fieldName, reader);
		}
		reader.endObject();
	}
	
	//TODO belongs here?
	public void checkVerticality(double tV, double tH, boolean nameOnly) {
		activeDatabase.checkVerticality(tV, tH, nameOnly);
	}
	
	public static boolean convert(String inFile, String outFile) {
		File f = new File(inFile);
        if(!(f.exists() && f.isFile()))
            return false;
        
		Database db = new Database(false);
		db.init();

        db.setLocation(inFile);
        if(db.loadDatabase() < 0) {
            return false;
        }
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
        	db.nullifyEmptyStrings();
        	String json = gson.toJson(db);
        	PrintWriter out = new PrintWriter(outFile);
        	out.write(json);
        	out.close();
        }
        catch(IOException e) {
        	logger.warn("An exception has occured", e);
        	return false;
        }
        return true;
	}
}
