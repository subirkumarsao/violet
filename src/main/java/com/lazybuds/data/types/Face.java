package com.lazybuds.data.types;

import org.opencv.core.Rect;

public class Face {
	private Rect face;
	private String label;
	
	
	public Face(Rect face, String label) {
		super();
		this.face = face;
		this.label = label;
	}
	
	public Rect getFace() {
		return face;
	}
	public void setFace(Rect face) {
		this.face = face;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	
	
}
