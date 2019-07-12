package com.lazybuds.data.types;

import org.opencv.core.Rect;

public class ImageAttributes {
	
	private String fileName;
	private Rect[] faces;
	private String label;
	
	public ImageAttributes(String fileName, Rect[] faces, String label) {
		super();
		this.fileName = fileName;
		this.faces = faces;
		this.label = label;
	}
	
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public Rect[] getFaces() {
		return faces;
	}
	public void setFaces(Rect[] faces) {
		this.faces = faces;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	
	
}
