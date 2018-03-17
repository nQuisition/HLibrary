package com.nquisition.hlibrary.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.nquisition.hlibrary.HLibrary;
import com.nquisition.hlibrary.api.IDatabaseInterface;
import com.nquisition.hlibrary.api.IGImage;
import com.nquisition.hlibrary.api.ProgressManager;
import com.nquisition.hlibrary.api.ProgressMonitor;

public class DatabaseInterface implements IDatabaseInterface
{
	private static final Logger logger = LogManager.getLogger(DatabaseInterface.class);
	private Map<String, Database> databases = new HashMap<>();
	private Database activeDatabase = null;
	
	//TODO think this method should be removed
	public Database getActiveDatabase()
	{
		return activeDatabase;
	}
	
	//TODO think this method should be removed
	public Database getDatabase(String location) {
		if(!databases.containsKey(location)) {
			logger.warn("Database \"" + location + "\" is not loaded!");
			return null;
		}
		return databases.get(location);
	}
	
	public boolean loadDatabase(String fileName, boolean local) {
		return loadDatabase(fileName, local, true);
	}
	
	public boolean loadDatabase(String fileName, boolean local, boolean setAsActive) {
		if(databases.containsKey(fileName)) {
			logger.info("Database \"" + fileName + "\" is already loaded, skipping");
			return true;
		}
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
		Database db = null;
		if(isJson) {
			db = readDatabaseFromJson(fileName, local);
			//No images after loading -> fail
			//TODO maybe change this and allow databases to have some other metadata (like its location)
			if(db == null || db.getImages().size() <= 0)
				return false;
		} else {
			//Legacy format
			db = new Database(local);
			db.setLocation(fileName);
	
	        //TODO deffinitely there are better actions if it fails
	        if(db.loadDatabase() < 0) {
	            logger.fatal("Unable to load database!");
	            return false;
	        }
		}
		db.sortFolders();
		databases.put(fileName, db);
		logger.info("Loaded \"" + fileName + "\" " + db);
		if(setAsActive)
			activeDatabase = db;
        return true;
	}
	
	public boolean createDatabase(String location, boolean local) {
		return createDatabase(location, local, true);
	}
	
	//TODO use this in loadDatabase when the specified file doesn't exist?
	public boolean createDatabase(String location, boolean local, boolean setAsActive) {
		if(databases.containsKey(location)) {
			logger.info("Database \"" + location + "\" is already loaded, skipping");
			return true;
		}
		File f = new File(location);
		if(f.exists())
			return false;
		try {
			f.createNewFile();
		} catch (IOException e) {
			logger.warn("An exception has occured", e);
			return false;
		}
		
		Database db = new Database(local);
		db.setLocation(location);
		databases.put(location, db);
		if(setAsActive)
			activeDatabase = db;
		return true;
	}
	
	public boolean saveDatabase(String location) {
		if(!databases.containsKey(location)) {
			logger.warn("Database \"" + location + "\" is not loaded!");
			return false;
		}
		Database db = databases.get(location);
		return saveDatabase(db);
	}
	
	private static boolean saveDatabase(Database db) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
        	db.nullifyEmptyStrings();
        	String json = gson.toJson(db);
        	PrintWriter out = new PrintWriter(db.getLocation(), "UTF-8");
        	out.write(json);
        	out.close();
        } catch(IOException e) {
        	logger.warn("Saving database failed!", e);
        	return false;
        }
        return true;
	}
	
	public boolean saveActiveDatabase() {
		return saveDatabase(activeDatabase);
	}
	
	public void unloadDatabase(String location, boolean save) {
		Database db = databases.get(location);
		if(db == null)
			return;
		if(db == activeDatabase) {
			logger.warn("Cannot unload the active database!");
			return;
		}
		if(save)
			saveDatabase(db);
		databases.remove(location);
	}
	
	@Override
	public List<GFolder> getActiveFolders() {
		return new ArrayList<>(activeDatabase.getFolders());
	}
	
	@Override
	public List<GImage> getActiveImages() {
		return new ArrayList<>(activeDatabase.getImages());
	}
	
	public void activeInfo() {
		activeDatabase.info();
	}
	
	public boolean addDirectory(String dbLocation, String path, int depth) {
		if(!databases.containsKey(dbLocation)) {
			logger.warn("Database \"" + dbLocation + "\" is not loaded!");
			return false;
		}
		return databases.get(dbLocation).addDirectory(path, depth) >= 0;
	}
	
	@Override
	public boolean addDirectoryToActive(String path, int depth) {
		return activeDatabase.addDirectory(path, depth) >= 0;
	}
	
	//TODO memory leaks?
	public Callable<Integer> computeSimilarityStrings(String dbLocation) {
		Database db;
		if(dbLocation == null)
			db = this.getActiveDatabase();
		else
			db = this.getDatabase(dbLocation);
		if(db == null) {
			//TODO log, warn etc;
			return null;
		}

		List<GImage> images = new ArrayList<>(db.getImages()); 
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
			db.saveDatabase();
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
		public Callable<Integer> computeSimilarityStrings(GFolder folder) {
			List<GImage> images = folder.getImages();
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
	public Callable<Map<GImage, List<GImage>>> findSimilarImages(String dbLocation, int threshold) {
		Database db;
		if(dbLocation == null)
			db = this.getActiveDatabase();
		else
			db = this.getDatabase(dbLocation);
		if(db == null) {
			//TODO log, warn etc;
			return null;
		}

		List<GImage> images = new ArrayList<>(db.getImages()); 
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
	                    //if(img.getParent() != images.get(j).getParent() || diff == 0) {
	                    	if(!res.containsKey(img))
	                    		res.put(img, new ArrayList<>());
	                    	res.get(img).add(images.get(j));
	                    //}
	                }
	            }
	        });
			return res;
		};
	}
	
	//TODO memory leaks?
		//TODO compute similarity or leave like this?
		//Assumes similarity is computed; skips images without similarity info
		public Callable<Map<GImage, List<GImage>>> findPartitions(String dbLocation, int threshold) {
			Database db;
			if(dbLocation == null)
				db = this.getActiveDatabase();
			else
				db = this.getDatabase(dbLocation);
			if(db == null) {
				//TODO log, warn etc;
				return null;
			}

			List<GImage> images = new ArrayList<>(db.getImages()); 
			ProgressMonitor progressMonitor = HLibrary.requestProgressMonitor("Finding similar images");
			progressMonitor.start(images.size());
			
			//TODO detect sketches and ignore them
			return () -> {
				Map<GImage, List<GImage>> res = new LinkedHashMap<>();
				int next = 0;
				while(true) {
					if(next >= images.size())
						break;
					
					GImage img = images.get(next);
					int lastIndex = next;
					
					progressMonitor.add(1);
					if(!res.containsKey(img))
                		res.put(img, new ArrayList<>());
					
					if(next >= images.size()-1)
						break;
					
		            for(int j = next+1; j < images.size(); j++) {
		            	GImage image2 = images.get(j);
		            	
		            	boolean found = false;
		            	int minDiff = -1;
		            	for(int k = next; k <= lastIndex; k++) {
			                int diff = images.get(k).differenceFrom(images.get(j), -1, false);
			                if(minDiff < 0 || diff < minDiff)
			                	minDiff = diff;
			                if(diff < threshold && diff >= 0) {
			                    //if(img.getParent() != images.get(j).getParent() || diff == 0) {
			                    	res.get(img).add(images.get(j));
			                    	found = true;
			                    	break;
			                    //}
			                }
		            	}
		            	if(!found) {
		            		System.out.println("Breaking at " + (j-next) + " with min diff of " + minDiff); 
		                	next = j;
		                	break;
		                }
		            	lastIndex++;
		            }
		        }
				return res;
			};
		}
	
	private Database readDatabaseFromJson(String fileName, boolean local)
	{
		JsonReader reader = null;
		try
		{
			reader = new JsonReader(new InputStreamReader(new FileInputStream(fileName), "UTF-8"));
		}
		catch(FileNotFoundException e)
		{
			logger.warn("File not found " + fileName, e);
			return null;
		} catch(UnsupportedEncodingException e) {
			logger.warn("Unsupported encoding " + fileName, e);
			return null;
		}
		Database db = null;
		try
		{
			db = new Database(local);
			db.setLocation(fileName);
			reader.beginObject();
			while(reader.hasNext())
			{
				String name = reader.nextName();
				if(name.equals("folders"))
				{
					reader.beginArray();
					while(reader.hasNext())
					{
						db.addGFolder(readGFolderFromJson(reader), false);
					}
					reader.endArray();
				}
				else if(name.equals("dict"))
				{
					db.setTagDictionary(readTagDictionaryFromJson(reader));
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
			return null;
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
		return db;
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
	public void checkActiveVerticality(double tV, double tH, boolean nameOnly) {
		activeDatabase.checkVerticality(tV, tH, nameOnly);
	}
	
	public List<GImage> getActiveImagesSatisfyingConditions(List<Predicate<IGImage>> conditions) {
		List<GImage> images = activeDatabase.getImages();
		List<GImage> res = new ArrayList<>(); 
		for(GImage image : images) {
			boolean failed = false;
			for(Predicate<IGImage> predicate : conditions) {
				if(!predicate.test(image)) {
					failed = true;
					break;
				}
			}
			if(failed)
				continue;
			res.add(image);
		}
		return res;
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
