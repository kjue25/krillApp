package krill;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import krill.KrillApp.KrillShapes;
import krill.KrillImageTools.ImageResults;

public class Calculations {

	private Ruler ruler;
	private List<KrillShapes> krillShapes; 
	private KrillApp app;

	public Calculations(KrillApp app){
		ruler = null;
		krillShapes = new ArrayList<KrillShapes>();
		this.app = app;
	}

	public static class KrillPoints {

		private List<Point> brightPoints;
		private Rectangle box;

		public KrillPoints(List<Point> brightPoints, WritableRaster image) {
			this.brightPoints = brightPoints;
			this.box = KrillImageTools.getBoundingBox(brightPoints, KrillApp.KrillShapes.BUFFER);
		}

		public int getX() { return box.x;}
		public int getY() { return box.y;}
		public List<Point> getBrightPoints() { return brightPoints;}
		public Rectangle getBox() {return box;}
	}


	//Given two points, calculates the distance between them
	public static double calculateDistance(double x1, double x2, double y1, double y2){
		double xDiff = (x1 - x2);
		double yDiff = (y1 - y2);
		double difference = Math.sqrt((Math.pow(xDiff, 2)) + (Math.pow(yDiff, 2)));
		return difference;
	}
	
	private void setRuler(final Ruler bestRuler){
		this.ruler = bestRuler;
		app.setRuler(ruler);
	}

	private void runRuler(final BufferedImage image){
		if (image == null) return;

		Thread rulerThread = new Thread(new Runnable() {

			@Override
			public void run() {
				final Ruler mmRuler = Ruler.getBestRuler(image);
				System.out.println("Conversion: " + mmRuler.getConversion());
				setRuler(mmRuler);
			}
		});

		rulerThread.start();
	}

	private void runKrill(final BufferedImage image){
		if (image == null) return;

		Thread krillThread = new Thread(new Runnable(){

			@Override
			public void run() {
				System.out.println("Image is not null");
				WritableRaster smoothed = KrillImageTools.smooth(image); 
				BufferedImage smoothedImage = new BufferedImage(image.getColorModel(), smoothed, true, null);
				ImageResults binned = KrillImageTools.bin(smoothed); 
				BufferedImage binnedImage = new BufferedImage(image.getColorModel(), binned.getBinnedRaster(), true, null);
				List<KrillPoints> allKrill = KrillImageTools.findKrill(binned.getBrightest(), binned.getBinnedRaster());

				sortKrillByPosition(allKrill); 

				WritableRaster raster = binned.getBinnedRaster(); 
				for (KrillPoints krillPoints : allKrill) {
					Ellipse2D.Double eye = KrillImageTools.findEye(krillPoints.brightPoints, image.getRaster(), krillPoints.getBox()); //temporary
					if (eye != null){
						List<Line2D.Double> edge = KrillImageTools.getKrillLines((int)(eye.width + eye.x), krillPoints.getBrightPoints());
						double length = KrillImageTools.getKrillLength(edge);
						KrillShapes krill = new KrillShapes(edge, eye, krillPoints.box, length);
						krillShapes.add(krill);
						System.out.println("adding krill"); 
						app.addKrill(krill, ruler); 
					}
				}
			}
		});

		krillThread.start();
	}

	public void analyzeImage(final BufferedImage image){
		clearImage(); 
		if (image== null) return; 
		runRuler(image);
		runKrill(image);
	}

	private void clearImage() {
		ruler = null; 
		krillShapes = new ArrayList<KrillShapes>(); 
	}

	public static void sortKrillByPosition(List<KrillPoints> allKrillList){
		int totalX = 0;
		for (KrillPoints krill: allKrillList)
			totalX += krill.getX();
		final int averageX = totalX/allKrillList.size();

		Collections.sort(allKrillList, new Comparator<KrillPoints>() {

			public int getBin(KrillPoints krillBox){
				if (krillBox.getX() < averageX)
					return 0;
				else
					return 1;
			}

			@Override
			public int compare(KrillPoints krill1, KrillPoints krill2) {
				int diff = getBin(krill1) - getBin(krill2);
				return diff == 0 ? krill1.getY() - krill2.getY() : diff; 
			}
		});
	}

}
