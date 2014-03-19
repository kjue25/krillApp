package krill;

import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.util.ArrayList;
import java.util.List;

public class Ruler {

	//Creates a ruler
	private static final int NUM_RULER_LINES = 30; //number of lines on a ruler

	private static final int YMIN = 400; //starting y coordinate for the ruler stencil
	private static final int YMAX = 478; //ending y coordinate for the ruler stencil

	private static final int NUM_THETA_VALS = 4; //11 //should reach ~30 degrees; ~60 degrees total
	private static final double THETA_MIN = 0; //-(Math.PI)/6 //starts at -30 degrees
	private static final double THETA_DELTA = 0.1;  

	private static final int NUM_OFFSET_VALS = 30; 
	private static final double OFFSET_MIN = 150; //minimum x coordinate is 50
	private static final double OFFSET_DELTA = 3; 

	//FIXME
	private static final int NUM_GAP_VALS = 90; //from 8 to 12 
	private static final double GAP_MIN = 8; 
	private static final double GAP_DELTA = .05;
	
	private double theta, offset, gap;
	private List<Line2D.Double> lines; 
	
	//Finds the best-fit ruler
	public static double getOffset(int i) { return OFFSET_MIN + i* OFFSET_DELTA; }
	public static double getGap(int i) { return GAP_MIN + i* GAP_DELTA; }
	public static double getTheta(int i) { return THETA_MIN + i* THETA_DELTA; }

	public Ruler(double offset, double gap, double theta) {
		this.theta = theta;
		this.offset = offset;
		this.gap = gap;
		lines = getLines(offset, gap, theta); 
		
	}
	
	private static List<Line2D.Double> getLines(double offset, double gap, double theta) {
		ArrayList<Line2D.Double> rulerLines = new ArrayList<Line2D.Double>(); 

		for (int i = 0; i < NUM_RULER_LINES; i++) {
			//for each value, find the line and compute the total brightness
			double topX = offset + i*gap;
			double bottomX = topX + Math.tan(theta)*(YMAX - YMIN);
			Line2D.Double line = new Line2D.Double(topX, YMIN, bottomX, YMAX);
			rulerLines.add(line);
		}
		return rulerLines; 
		
	}
	
	//add a Ruler.getRulerPoints method to be called in KrillImageTools
	
	private static double getScore(BufferedImage image, List<Line2D.Double> rulerLines) {
		int brightness = 0;
		for (Line2D.Double line : rulerLines) {
			brightness += checkLine(image, line);
		} 
		double average = brightness/NUM_RULER_LINES; //compute the average pixel value by dividing by the total number of (pixels?) lines
		return average;
	}
	
	public List<Line2D.Double> getRulerLines() {
		return lines; 
	}

	public static Ruler getBestRuler(BufferedImage bufferedImage) {
		double bestScore = 0;
		Ruler bestRuler = null;
		for (int i = 0; i< NUM_OFFSET_VALS; i++){
			for (int j = 0; j< NUM_GAP_VALS; j++){
				for (int k = 0; k < NUM_THETA_VALS; k++){
					Ruler ruler = new Ruler(getOffset(i), getGap(j), getTheta(k));
					double score = ruler.getScore(bufferedImage, ruler.getRulerLines());
					if (score > bestScore) {
						bestScore = score;
						bestRuler = ruler;
					}
				}
			}
		}
		return bestRuler;
	}

	//IMAGE ANALYSIS for ruler
	//Returns the brightness of a line
	private static double checkLine(BufferedImage image, Line2D.Double line){
		Raster rasterImage = image.getData();
		int totalBrightness = 0;
		int counter = 0;
		int xStart = (int)Math.min(line.getX1(), line.getX2()); 
		int xEnd = (int) Math.max(line.getX1(), line.getX2()); 

		if (xStart == xEnd){ //vertical (or empty) line
			for (int y = YMIN; y <= YMAX; y++){
				totalBrightness += rasterImage.getSample(xStart, y, 0);
				counter++;
			} 
		} else{
			double m = (line.getY1() - line.getY2())/(line.getX1() - line.getX2());
			double b = line.getY1() - (m * line.getX1());

			for (int x = xStart; x <= xEnd; x++){
				double y1 = m * (x - (1/2)) + b;
				double y2 = m * (x + (1/2)) + b;
				double yMin = Math.max(image.getMinY(), Math.min(y1, y2)); 
				double yMax = Math.min(image.getHeight()-1, Math.max(y1, y2));

				for (int y = (int)yMin; y <= (int)yMax; y++){
					totalBrightness += rasterImage.getSample(x, y, 0);
					totalBrightness += rasterImage.getSample(x+1, y, 0);
					totalBrightness += rasterImage.getSample(x+2, y, 0);
					totalBrightness += rasterImage.getSample(x-1, y, 0);
					totalBrightness += rasterImage.getSample(x-2, y, 0);
					counter++;
				}
			}
		}
		return ((double)totalBrightness/counter);
	}
	
	public double getConversion() {
		return this.gap;
	}
}
