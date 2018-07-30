package com.nquisition.hlibrary.api;

import java.util.List;
import java.util.function.Predicate;

public interface UIHelper {
	void showImagesSatisfyingConditions(List<Predicate<ReadOnlyImageInfo>> conditions);
	void showImagesWithTags(String tagString);
}
