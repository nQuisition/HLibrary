package com.nquisition.hlibrary.api;

import java.util.List;

//FIXME not really read only, since has addTag
public interface ReadOnlyEntryInfo extends PropertyContainer {
	long getAdded();
	long getLastmod();
	long getViewed();
	int getViewcount();
	void setLastmodNow();
	String getComment();
	boolean hasTag(String t);
	boolean hasAllTags(List<String> tgs);
	boolean hasNoTags(List<String> tgs);
	void addTag(String t);
}
