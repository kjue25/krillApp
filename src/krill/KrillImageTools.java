package krill;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import krill.Calculations.KrillPoints;

public class KrillImageTools {

	private static final int[] weights = new int[] { 1, 4, 7, 4, 1, 4, 16, 26,
			16, 4, 7, 26, 41, 26, 7, 4, 16, 26, 16, 4, 1, 4, 7, 4, 1 };
	private static final int SUM_WEIGHTS = 273;
	private static final int DIMENSION = 5;

	public static WritableRaster smooth(BufferedImage image) {
		//takes a BufferedImage, smoothes the data, and returns a WritableRaster
		Raster rasterImage = image.getData();
		WritableRaster smoothedImage = rasterImage.createCompatibleWritableRaster();

		for (int x = 0; x < image.getWidth() - DIMENSION; x++) {
			for (int y = 0; y < image.getHeight() - DIMENSION; y++) {

				double pixelValueTotal = 0;
				for (int i = 0; i < DIMENSION; i++) {
					for (int j = 0; j < DIMENSION; j++) {
						pixelValueTotal += weights[DIMENSION * j + i]
								* (rasterImage.getSample(x + i, y + j, 0));
					}
				}
				double pixelValue = pixelValueTotal / SUM_WEIGHTS;
				smoothedImage.setSample(x, y, 0, pixelValue);
			}
		}
		System.out.println("end smoothed pixelValues");
		return smoothedImage;
	}

	public static class ImageResults {
		private List<Point> darkestPoints, middlePoints, brightestPoints;
		private WritableRaster binnedRaster;

		public ImageResults(List<Point> darkest, List<Point> middle,
				List<Point> brightest, WritableRaster binned) {
			this.darkestPoints = darkest;
			this.middlePoints = middle;
			this.brightestPoints = brightest;
			this.binnedRaster = binned;
		}

		public List<Point> getDarkest() {
			return darkestPoints;
		}

		public List<Point> getMiddle() {
			return middlePoints;
		}

		public List<Point> getBrightest() {
			return brightestPoints;
		}

		public WritableRaster getBinnedRaster() {
			return binnedRaster;
		}

	}

	private static class Histogram{
	
		private int[] brightnessBins;
		private int numPoints;
		
		public Histogram(WritableRaster image){
			brightnessBins = getBrightness(image);
			numPoints = image.getHeight()*image.getWidth();
		}
		
		private static int[] getBrightness(WritableRaster image){
			int[] brightnessBins = new int[256];
			for (int x = 0; x < image.getWidth(); x++){
				for (int y = 0; y < image.getHeight(); y++){
					int brightness = image.getSample(x, y, 0);
					brightnessBins[brightness]++;
				}
			}
			return brightnessBins;
		}
		
		public int percentileBrightness(int percentile){
			int percentilePoints = numPoints * percentile/100;
			int pointsPassed = 0;
			int brightness = 0;
			for (int x = 0; x < 255; x++){
				pointsPassed += brightnessBins[x];
				brightness = x;
				if (pointsPassed > percentilePoints){
					return brightness;
				}
			}
			return brightness;
		}
		
	}
	
	
	public static ImageResults bin(WritableRaster smoothedImage) {
		//takes a smoothed image and returns a binned WritableRaster
		List<Point> darkest = new ArrayList<Point>();
		List<Point> middle = new ArrayList<Point>();
		List<Point> brightest = new ArrayList<Point>();
		WritableRaster binnedRaster = smoothedImage.createCompatibleWritableRaster();
		Histogram brightnessRange = new Histogram(smoothedImage);

		for (int x = 0; x < smoothedImage.getWidth(); x++) {
			for (int y = 0; y < smoothedImage.getHeight(); y++) {
				//change bin values to percentages
				Point point = new Point(x, y);
				int val = smoothedImage.getSample(x, y, 0);
				if (val < brightnessRange.percentileBrightness(30)) { //dark spots; hard-coded brightness
					binnedRaster.setSample(x, y, 0, 1);
					darkest.add(point);
				} else if (val > brightnessRange.percentileBrightness(75)) { //bright spots (ie. krill); hard-coded brightness
					binnedRaster.setSample(x, y, 0, 120);
					brightest.add(point);
				} else { //brightness is between 50 and 100
					binnedRaster.setSample(x, y, 0, 60);
					middle.add(point);
				}
			}
		}
		return new ImageResults(darkest, middle, brightest, binnedRaster);
	}



	public static List<KrillPoints> findKrill(List<Point> brightestPoints, WritableRaster binnedImage) {
		//takes a binned image and finds the Krill in the brightest bin
		List<KrillPoints> allKrill = new ArrayList<KrillPoints>();
		WritableRaster krillRaster = binnedImage.createCompatibleWritableRaster();

		for (Point point : brightestPoints) {
			if (point.y > 375) continue; //temporary hard-coded value to avoid ruler; use Ruler.getRulerPoints
			List<Point> krillPoints = new ArrayList<Point>();
			int color = 128 + (allKrill.size() * 50) % 128;
			floodFill((int) point.getX(), (int) point.getY(), binnedImage, krillPoints, krillRaster, color);
			if (krillPoints.size() < 1000) continue;
			allKrill.add(new KrillPoints(krillPoints, krillRaster));
		}
		return allKrill;
	}

	/**
	 * updates krillPts, krillRaster for a single krill
	 * 
	 * @param x
	 * @param y
	 * @param binnedImage
	 * @param krillPts
	 * @param krillRaster
	 * @param color
	 */
	public static void floodFill(int xx, int yy, WritableRaster binnedImage, List<Point> krillPts, WritableRaster krillRaster, int color) {
		Set<Point> toVisit = new HashSet<Point>();
		toVisit.add(new Point(xx, yy));
		Set<Point> newToVisit = new HashSet<Point>();

		while (!toVisit.isEmpty()) {
			for (Point p : toVisit) {
				int x = p.x;
				int y = p.y;
				if (x < 0 || x >= binnedImage.getWidth()) continue;
				if (y < 0 || y >= binnedImage.getHeight()) continue;
				if (binnedImage.getSample(x, y, 0) != 120 || krillRaster.getSample(x, y, 0) != 0) continue;
				krillRaster.setSample(x, y, 0, color);
				krillPts.add(p);
				newToVisit.add(new Point(x - 1, y));
				newToVisit.add(new Point(x + 1, y));
				newToVisit.add(new Point(x, y - 1));
				newToVisit.add(new Point(x, y + 1));
			}

			if (newToVisit.isEmpty()) break;
			toVisit.clear();
			Set<Point> temp = toVisit;
			toVisit = newToVisit;
			newToVisit = temp;
		}
	}

	//In pixels
	private static final double MIN_EYE_RADIUS = 5; 
	private static final double NUM_RADII = 3;
	private static final double RADIUS_INCREMENT = 1;

	private static double getDiameter(int i) {
		return 2 * (MIN_EYE_RADIUS + i * RADIUS_INCREMENT);
	}

	//IMAGE ANALYSIS for krill
	//Returns the brightness of a circle
	public static double checkCircle(WritableRaster rasterImage, Ellipse2D.Double circle) {
		int totalBrightness = 0;
		int counter = 0;

		double minX = circle.getMinX();
		double maxX = circle.getMaxX();
		double minY = circle.getMinY();
		double maxY = circle.getMaxY();

		for (int x = (int) minX; x <= (int) maxX; x++) {
			for (int y = (int) minY; y <= (int) maxY; y++) {
				if (x < 0 || x >= rasterImage.getWidth() || y < 0 || y >= rasterImage.getHeight()) continue;
				if (circle.contains(x, y)) {
					totalBrightness += rasterImage.getSample(x, y, 0);
					counter++;
				}
			}
		}
		return ((double) totalBrightness / counter);
	}
	

	public static Rectangle getBoundingBox(List<Point> points, int margin) {
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;

		for (Point point : points) {
			maxX = Math.max(maxX, point.x + margin);
			minX = Math.min(minX, point.x - margin);
			maxY = Math.max(maxY, point.y + margin);
			minY = Math.min(minY, point.y - margin);
		}
		return new Rectangle(minX, minY, maxX - minX, maxY - minY);
	}

	public static Ellipse2D.Double findEye(List<Point> krill, WritableRaster rasterImage, Rectangle body) {
		double largestBrightnessDiff = 0;
		Ellipse2D.Double bestEye = null;

		for (int x = body.x; x < body.x + body.getWidth(); x++) {
			for (int y = body.y; y < body.y + body.getHeight(); y++) {
				if (x < 0 || x >= rasterImage.getWidth() || y < 0 || y >= rasterImage.getHeight()) continue;
				int pixelValue = rasterImage.getSample(x, y, 0);
				if (pixelValue > 75) continue; //adjust value
				for (int i = 0; i < NUM_RADII; i++) {
					Ellipse2D.Double eye = new Ellipse2D.Double(x, y, getDiameter(i), getDiameter(i));
					double innerBrightness = checkCircle(rasterImage, eye);
					if (innerBrightness > 75) continue; //adjust value
					double outerRingBrightness = getOuterRingBrightPixels(eye, 5, rasterImage, krill);
					if (outerRingBrightness > largestBrightnessDiff){
						largestBrightnessDiff = outerRingBrightness;
						bestEye = eye;
					}
				}
			}

		}
		return bestEye;
	}

	private static double getOuterRingBrightness(Ellipse2D.Double eye, int margin, Raster rasterImage) {
		int brightness = 0;
		int count = 0;
		Ellipse2D.Double eyeRing = new Ellipse2D.Double(eye.x, eye.y, eye.getWidth() + margin, eye.getHeight() + margin);
		for (int k = (int) eyeRing.getMinX(); k < eyeRing.getMaxX(); k++) {
			for (int j = (int) eyeRing.getMinY(); j < eyeRing.getMaxY(); j++) {
				if (k < 0 || k >= rasterImage.getWidth() || j < 0 || j >= rasterImage.getHeight()) continue;
				if (eyeRing.contains(k, j) && !eye.contains(k, j))
					brightness += rasterImage.getSample(k, j, 0);
			}
		}
		return ((double) brightness) / count;
	}
	
	private static double getOuterRingBrightPixels(Ellipse2D.Double eye, int margin, Raster rasterImage, List<Point> brightPoints){
		int brightness = 0;
		Ellipse2D.Double eyeRing = new Ellipse2D.Double(eye.x, eye.y, eye.getWidth() + margin, eye.getHeight() + margin);
		for (int k = (int) eyeRing.getMinX(); k < eyeRing.getMaxX(); k++){
			for(int j = (int) eyeRing.getMinY(); j < eyeRing.getMaxY(); j++){
				if (k < 0 || k >= rasterImage.getWidth() || j < 0 || j >= rasterImage.getHeight()) continue;
				if (brightPoints.contains(new Point((int)(k-PIXEL_OFFSET), (int)(j-PIXEL_OFFSET)))) //ACCOUNT FOR OFFSET BETWEEN rasterImage and binnedImage
					brightness++;
			}
		}
		return brightness;
	}
	
	//Given two points, calculates the distance between them
	private static double calculateDistance(double x1, double x2, double y1, double y2){
		double xDiff = (x1 - x2);
		double yDiff = (y1 - y2);
		double difference = Math.sqrt((Math.pow(xDiff, 2)) + (Math.pow(yDiff, 2)));
		return difference;
	}
	
	public static double calculateDistance(Point2D a, Point2D b){
		return (calculateDistance(a.getX(), b.getX(), a.getY(), b.getY()));
	}
	
	/**
	 * finds the length of one krill
	 * @param brightKrillPoints
	 * @return
	 */
	private static final int BIN_WIDTH = 3;
	private static final double PIXEL_OFFSET = 2.5; //based on smoothing function
	public static List<Line2D.Double> getKrillLines(int startX, List<Point> brightKrillPoints) { //break into two fxns
		int maxX = Integer.MIN_VALUE;
		List<Point> allEdgePoints = new ArrayList<Point>();
		List<Point> averageEdgePoints = new ArrayList<Point>();
		List<Line2D.Double> edgeLines = new ArrayList<Line2D.Double>();
		
		//Find the max xVal, the stop coordinate
		//Assumes that a krill is not curved under itself
		for (Point point : brightKrillPoints){
			if (point.x > maxX)
				maxX = point.x;
		}
		
		//For each x value in [startX, max x value], find the max y
		for (int i = startX; i < maxX; i++){
			int minY = Integer.MAX_VALUE;
			for (Point point : brightKrillPoints){
				if (point.x != i) continue; //if the point has the right xVal
				if (point.y < minY){
					minY = point.y;
				}
			}
			allEdgePoints.add(new Point(i, minY));
			if (i == startX)
				averageEdgePoints.add(new Point(i, minY));
		}
		
		//Average across neighboring x values
		int whichEdgePoint = 0;
		for (int i = startX; i < maxX-BIN_WIDTH; i++){
			double ySum = 0;
			double xSum = 0;
			for (int j = 0; j < BIN_WIDTH; j++){
				if (whichEdgePoint >= allEdgePoints.size()) continue;
				ySum += allEdgePoints.get(whichEdgePoint).getY();
				xSum += allEdgePoints.get(whichEdgePoint).getX();
				whichEdgePoint++;
			}
			double yAverage = ySum/BIN_WIDTH;
			double xAverage = xSum/BIN_WIDTH;
			averageEdgePoints.add(new Point((int)xAverage, (int)yAverage));
			i = i+BIN_WIDTH-1;
		}
		if (allEdgePoints.size()!= 0)
			averageEdgePoints.add(allEdgePoints.get(allEdgePoints.size()-1));		
		
		//compute the distance between points and sum
		for (int k = 0; k < averageEdgePoints.size()-1; k++){
			edgeLines.add(new Line2D.Double(averageEdgePoints.get(k).x + PIXEL_OFFSET, averageEdgePoints.get(k).y + PIXEL_OFFSET, averageEdgePoints.get(k+1).x + PIXEL_OFFSET, averageEdgePoints.get(k+1).y + PIXEL_OFFSET));
		}

		return edgeLines;
	}
	
	public static double getKrillLength(List<Line2D.Double> segments){
		double lengthOfLine = 0;
		for (Line2D.Double segment : segments){
			lengthOfLine += calculateDistance(segment.x1, segment.x2, segment.y1, segment.y2);
		}
		return lengthOfLine;
	}
}
