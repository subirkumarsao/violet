package com.lazybuds.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.servlet.ServletContext;
import javax.transaction.Transactional;

import org.h2.util.StringUtils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.face.FaceRecognizer;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.lazybuds.data.types.Face;
import com.lazybuds.data.types.ImageAttributes;
import com.lazybuds.db.dao.LabelDao;
import com.lazybuds.db.entity.Label;

@Controller
@Transactional
public class RecognitionController {

	@Autowired
	private CascadeClassifier faceDetector;

	@Autowired
	ServletContext servletContext;

	@Autowired
	FaceRecognizer faceRecognizer;

	@Autowired
	LabelDao labelDao;

	@Value("${temp_file_path}")
	String TEMP_FOLDER;

	@Value("${model_file_path}")
	String MODEL_FILE;

	@RequestMapping(value = "/face/recognize", method = RequestMethod.POST)
	public @ResponseBody List<Face> getFaceCoordinates(
			@RequestParam(value = "file", required = false) MultipartFile file,
			@RequestParam(value = "base64Image", required = false) String base64Image) throws IOException {
		List<Face> faces = new ArrayList<Face>();
		byte[] bytes;
		if (StringUtils.isNullOrEmpty(base64Image)) {
			bytes = file.getBytes();
		} else {
			bytes = Base64.getDecoder().decode(base64Image);
		}

		Mat image = Imgcodecs.imdecode(new MatOfByte(bytes), Imgcodecs.IMREAD_UNCHANGED);
		MatOfRect faceDetections = new MatOfRect();
		faceDetector.detectMultiScale(image, faceDetections);

		String fileName = UUID.randomUUID().toString();
		if (StringUtils.isNullOrEmpty(base64Image)) {
			Path path = Paths.get(servletContext.getRealPath("/images") + "/" + fileName);
			Files.write(path, bytes);
		}

		if (faceDetections.empty()) {
			return faces;
		}
		Mat mGray = new Mat();
		for (Rect rect : faceDetections.toArray()) {
			Mat croppedImage = image.submat(rect);
			Imgproc.cvtColor(croppedImage, mGray, Imgproc.COLOR_BGR2GRAY);
			Imgcodecs.imwrite(TEMP_FOLDER + "/" + UUID.randomUUID().toString() + ".jpg", mGray);
			int label = faceRecognizer.predict_label(mGray);
			faces.add(new Face(rect, labelDao.getById(label).getName()));

			faces.add(new Face(rect, ""));
		}
		return faces;
	}

	@RequestMapping(value = "/face/train", method = RequestMethod.POST)
	public @ResponseBody String trainFaces(@RequestParam(value = "file", required = false) MultipartFile file,
			@RequestParam(value = "base64Image", required = false) String base64Image,
			@RequestParam("label") String label) throws IOException {

		byte[] bytes;
		if (StringUtils.isNullOrEmpty(base64Image)) {
			bytes = file.getBytes();
		} else {
			bytes = Base64.getDecoder().decode(base64Image);
		}

		Mat image = Imgcodecs.imdecode(new MatOfByte(bytes), Imgcodecs.IMREAD_UNCHANGED);
		MatOfRect faceDetections = new MatOfRect();
		faceDetector.detectMultiScale(image, faceDetections);

		if (faceDetections.toArray().length != 1) {
			// we expect only one face
			return "Faces = "+faceDetections.toArray().length;
		}
		Rect rect = faceDetections.toArray()[0];

		Mat croppedImage = image.submat(rect);
		Mat mGray = new Mat();
		Imgproc.cvtColor(croppedImage, mGray, Imgproc.COLOR_BGR2GRAY);
		
		List<Mat> images = new ArrayList<>();
		images.add(mGray);

		int[] labelArray = new int[images.size()];
		Optional<Label> possibleLabel = labelDao.getByName(label);
		final int trainingLabel;
		if (possibleLabel.isPresent()) {
			trainingLabel = possibleLabel.get().getId();
		} else {
			trainingLabel = (int) labelDao.save(new Label(label));
		}
		Arrays.fill(labelArray, trainingLabel);
		MatOfInt matOfInt = new MatOfInt(labelArray);
		faceRecognizer.update(images, matOfInt);
		faceRecognizer.save(MODEL_FILE);

		return "OK";
	}

	@RequestMapping(value = "/face/train/zip", method = RequestMethod.POST)
	public @ResponseBody String trainFacesZip(@RequestParam("file") MultipartFile file,
			@RequestParam("label") String label) throws IOException {

		// String fileName = UUID.randomUUID().toString();
		Path path = Paths.get(TEMP_FOLDER + "/" + file.getOriginalFilename());
		Files.write(path, file.getBytes());

		ZipFile zipFile = new ZipFile(path.toFile());
		Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
		List<Mat> images = new ArrayList<>();

		while (enumeration.hasMoreElements()) {
			ZipEntry entry = enumeration.nextElement();
			if (entry.isDirectory()) {
				continue;
			}
			byte[] bytes = zipFile.getInputStream(entry).readAllBytes();
			Mat image = Imgcodecs.imdecode(new MatOfByte(bytes), Imgcodecs.IMREAD_UNCHANGED);
			MatOfRect faceDetections = new MatOfRect();
			faceDetector.detectMultiScale(image, faceDetections);

			if (faceDetections.toArray().length != 1) {
				// we expect only one face
				continue;
			}
			Rect rect = faceDetections.toArray()[0];

			Mat croppedImage = image.submat(rect);
			Mat mGray = new Mat();
			Imgproc.cvtColor(croppedImage, mGray, Imgproc.COLOR_BGR2GRAY);
			images.add(mGray);

		}
		zipFile.close();
		int[] labelArray = new int[images.size()];
		Optional<Label> possibleLabel = labelDao.getByName(label);
		final int trainingLabel;
		if (possibleLabel.isPresent()) {
			trainingLabel = possibleLabel.get().getId();
		} else {
			trainingLabel = (int) labelDao.save(new Label(label));
		}
		Arrays.fill(labelArray, trainingLabel);
		MatOfInt matOfInt = new MatOfInt(labelArray);
		faceRecognizer.update(images, matOfInt);
		faceRecognizer.save(MODEL_FILE);

		return "OK";
	}

	@RequestMapping(value = "/face/train/local", method = RequestMethod.POST)
	public @ResponseBody String trainLocal(@RequestParam("rootPath") String rootPath,
			@RequestParam("label") String label) throws IOException {
		Path path = Paths.get(rootPath);
		if (!path.toFile().exists() || path.toFile().isFile()) {
			return "Invalid directory path!";
		}
		Optional<Label> possibleLabel = labelDao.getByName(label);
		final int trainingLabel;
		if (possibleLabel.isPresent()) {
			trainingLabel = possibleLabel.get().getId();
		} else {
			trainingLabel = (int) labelDao.save(new Label(label));
		}

		trainOnDirectory(path.toFile(), trainingLabel);

		return "OK";
	}

	private void trainOnDirectory(final File directory, final int label) {
		List<Mat> images = new ArrayList<>();
		for (final File file : directory.listFiles()) {
			if (file.isDirectory()) {
				trainOnDirectory(file, label);
				continue;
			}
			System.out.println(file.getAbsolutePath());

			final byte[] bytes;
			try {
				bytes = Files.readAllBytes(file.toPath());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			Mat image = Imgcodecs.imdecode(new MatOfByte(bytes), Imgcodecs.IMREAD_UNCHANGED);
			MatOfRect faceDetections = new MatOfRect();
			faceDetector.detectMultiScale(image, faceDetections);

			if (faceDetections.toArray().length != 1) {
				// we expect only one face
				continue;
			}
			Rect rect = faceDetections.toArray()[0];

			Mat croppedImage = image.submat(rect);
			Imgcodecs.imwrite(TEMP_FOLDER + "/" + file.getName(), croppedImage);
			Mat mGray = new Mat();
			Imgproc.cvtColor(croppedImage, mGray, Imgproc.COLOR_BGR2GRAY);
			images.add(mGray);

		}
		if (images.isEmpty()) {
			// empty folder
		} else {
			int[] labelArray = new int[images.size()];
			Arrays.fill(labelArray, label);
			MatOfInt matOfInt = new MatOfInt(labelArray);
			faceRecognizer.update(images, matOfInt);

			faceRecognizer.save(MODEL_FILE);
		}
	}
}
