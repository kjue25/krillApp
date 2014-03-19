package krill;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * ImageTools is a static class that holds image utility functions
 */
public class ImageTools {
	
	public static BufferedImage loadImage(String fileName) {
		try {
			return ImageIO.read(ImageTools.class.getResource(fileName));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static BufferedImage loadImage(File file) {
		try {
			return ImageIO.read(file);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static List<BufferedImage> loadImageArray(File[] files) {	
		List<File> fileArray = new ArrayList<File>();
		
		for (File arrayFile : files){
			fileArray.add(arrayFile);
		}
		return loadImages(fileArray);
	}
	
	public static List<BufferedImage> loadImages(List<File> files) {
		try {
			if (files != null) {
				List<BufferedImage> images = new ArrayList<BufferedImage>(
						files.size());
				for (File file : files) {
					images.add(ImageIO.read(file));
				}
				return images;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Collections.emptyList();
	}
}
