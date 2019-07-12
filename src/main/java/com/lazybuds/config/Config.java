package com.lazybuds.config;

import java.nio.file.Paths;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.opencv_java;
import org.opencv.face.FaceRecognizer;
import org.opencv.face.LBPHFaceRecognizer;
import org.opencv.objdetect.CascadeClassifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

@Configuration
public class Config {
	
	@Value("${model_file_path}")
	String MODEL_FILE;

	static {
		Loader.load(opencv_java.class);
	}

	@Bean
	public CascadeClassifier getCascadeClassifier(@Value("${classiferfilePath}") String classiferfilePath) {
		return new CascadeClassifier(Config.class.getClassLoader().getResource(classiferfilePath).getPath().substring(1));
	}
	
	@Bean
    public MultipartResolver multipartResolver() {
        return  new CommonsMultipartResolver();
    }
	
	@Bean
	public FaceRecognizer getFaceRecognizer() {
		FaceRecognizer faceRecognizer = LBPHFaceRecognizer.create();
		if(Paths.get(MODEL_FILE).toFile().exists()) {
			faceRecognizer.read(MODEL_FILE);			
		}
		return faceRecognizer;
	}
}
