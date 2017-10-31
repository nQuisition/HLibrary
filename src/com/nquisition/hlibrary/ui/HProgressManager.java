package com.nquisition.hlibrary.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.nquisition.hlibrary.api.ProgressManager;
import com.nquisition.hlibrary.api.ProgressMonitor;
import com.sun.media.jfxmediaimpl.platform.Platform;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

//TODO create interface IProgressMonitor, 2 impl's - GUI and console, instead of requesting
//the progress bar, register with the monitor and it will return a token that can be used
//to inform the monitor of change of progress. It then updates appropriate bar and label
public class HProgressManager extends Stage implements ProgressManager {
	private static final int REFRESH_DELAY = 50;
	
	private Map<MultiProgressMonitor, Node> progressMonitors = new HashMap<>();
	private VBox container;
	
	public HProgressManager() {
		container = new VBox();
		container.setPadding(new Insets(20,0,0,20));
		container.setSpacing(20);
		StackPane rootp = new StackPane();
        rootp.getChildren().add(container);
        this.setScene(new Scene(rootp, 400, 200));
	}
	

	@Override
	public ProgressMonitor requestProgressMonitor(String taskName) {
		MultiProgressMonitor monitor = new MultiProgressMonitor(taskName);
		Node element = monitor.getUIElement();
		progressMonitors.put(monitor, element);
		container.getChildren().add(element);
		return monitor;
	}
	
	public void monitorComplete(MultiProgressMonitor monitor) {
		Node element = progressMonitors.get(monitor);
		if(element == null)
			return;
		container.getChildren().remove(element);
		progressMonitors.remove(monitor);
	}
	
	private class MultiProgressMonitor implements ProgressMonitor {
		private ProgressBar bar = new ProgressBar();
		private Label taskLabel = new Label("-");
		private Label progressLabel = new Label("-/-");
		private VBox taskContainer = new VBox();
		
		private AtomicInteger numDone = new AtomicInteger();
		private int numTotal;
		private Timeline numWatcher = new Timeline(
				new KeyFrame(Duration.millis(REFRESH_DELAY), e -> update()));
		
		public MultiProgressMonitor(String taskName) {
			//TODO
			bar.setPrefWidth(300.0d);
			taskLabel.setText(taskName);
			taskContainer.getChildren().addAll(taskLabel, bar, progressLabel); 
		}
		
		public void update() {
			double progress = 1.0d*numDone.get()/numTotal;
			bar.setProgress(progress);
			progressLabel.setText(numDone.get() + "/" + numTotal + " (" + String.format("%.1f", progress*100.0d) + "%)");
			if(numDone.get() >= numTotal) {
				numWatcher.stop();
				numTotal = 0;
				monitorComplete(this);
			}
		}
		
		public boolean isRunning() {
			return numTotal > 0;
		}
		
		@Override
		public void start(int total) {
			numDone.set(0);
			numTotal = total;
			bar.setProgress(0.0d);
			numWatcher.setCycleCount(Timeline.INDEFINITE);
			numWatcher.play();
		}
		
		@Override
		public void add(int count) {
			numDone.addAndGet(count);
		}

		@Override
		public void done() {
		}
		
		public Node getUIElement() {
			return taskContainer;
		}
	}
}
