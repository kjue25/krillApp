package krill;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JScrollPane;

import net.miginfocom.swing.MigLayout;

public class KrillApp extends JFrame {

	private static Dimension DEFAULT_SIZE = new Dimension(1200, 600); //panel dimensions
	private static String APP_NAME = "KrillApp"; //panel title
	
	//Instances of other panels
	private LeftPanel leftPanel; 
	private PicturePanel picturePanel;
	private ResultsPanel resultsPanel;
	private JScrollPane pictures;
	private Calculations calculations;
	private BufferedImage image;
	private List<KrillShapes> krillShapes;
	private Ruler ruler;
	
	public enum LineState {
		STRAIGHT,
		PIECEWISE, SELECT, POINT, EDIT, EDITKRILL
	}
	
	public KrillApp() {
		super(APP_NAME);
		this.setLayout(new MigLayout());
		setSize(DEFAULT_SIZE); 
		
		//Initialized panels
		resultsPanel = new ResultsPanel();
		picturePanel = new PicturePanel(this);
		leftPanel = new LeftPanel(this); 
		pictures = new JScrollPane(picturePanel);
		calculations = new Calculations(this);
		
		krillShapes = new ArrayList<KrillShapes>();
			
		//Add the panels with the left panel filling 30% of the width and the imagePanel filling the remaining 70%
		this.add(leftPanel, "width 30%, height 100%"); 
		this.add(pictures, "width 60%, height 100%");
		this.add(resultsPanel, "width 10%, height 100%");	
	}
	
	public static class KrillShapes{
		private List <Line2D.Double> body;
		private Ellipse2D.Double eye;
		private Rectangle box;
		private double pixelLength; 

		public KrillShapes(List <Line2D.Double> body, Ellipse2D.Double eye, Rectangle box, double pixelLength) {
			this.body = body; 
			this.eye = eye; 
			this.box = box; 
			this.pixelLength = pixelLength; 
		}

		public double getPixelLength() {return pixelLength;}
		public Ellipse2D.Double getEye() {return eye;}
		public List<Line2D.Double> getBody() { return body;}
		public Rectangle getBox() {return box;}
		
		public void recalculate() {
			pixelLength = KrillImageTools.getKrillLength(body);
			updateBox();
		}

		public void updateBody(List<Line2D.Double> newBody) {
			body = newBody;
			recalculate();
		}
		
		public static final int BUFFER = 5;
		private void updateBox(){
			double xMin = Double.MAX_VALUE;
			double yMin = Double.MAX_VALUE;
			double xMax = Double.MIN_VALUE;
			double yMax = Double.MIN_VALUE;
			for(Line2D.Double segment : body){
				xMin = Math.min(segment.x1, xMin);
				xMin = Math.min(segment.x2, xMin);

				xMax = Math.max(segment.x1, xMax);
				xMax = Math.max(segment.x2, xMax);
				
				yMin = Math.min(segment.y1, yMin);
				yMin = Math.min(segment.y2, yMin);
				
				yMax = Math.max(segment.x1, yMax);
				yMax = Math.max(segment.x2, xMax);
			}
			box.setBounds((int)(xMin-BUFFER), (int)(yMin-BUFFER), (int)(xMax-xMin+BUFFER*2), (int)(yMax-yMin+BUFFER*2));
		}
		
	}
	
	//Updates JScrollPanel's viewport
	public void updateViewport(){
		pictures.setViewportView(picturePanel);
	}
		
	public void load() {
		setVisible(true); 
	}
	
	public void setImage(BufferedImage image){
		krillShapes.clear();
		ruler = null;
		this.image = image; 
		picturePanel.setImage(image);
	}
	
	public void analyzeImage(){
		krillShapes.clear();
		ruler = null;
		if (image == null) return; 
		calculations.analyzeImage(image);
	}
	
	public void addKrill(KrillShapes krill, Ruler ruler){
		krillShapes.add(krill);
		picturePanel.setKrill(krillShapes);
		recalculate();
	}
	
	private static List<Double> getLengths(List<KrillShapes> krill, double conversion){
		List<Double> bodies = new ArrayList<Double>();
		for (KrillShapes k : krill)
			bodies.add(k.getPixelLength()/conversion);
		return bodies;
	}
	
	public void setRuler(Ruler ruler){
		this.ruler = ruler;
		picturePanel.setRuler(ruler);
	}
	
	public void setEditState(LineState assignedState){
		picturePanel.setState(assignedState);
	}
	
	private void exportResults(List <Double> mmLengths, File f) throws IOException{
		FileWriter writer = new FileWriter(f);
		for (Double length : mmLengths) {
			writer.write(length +"\n");
		}
		writer.close();
	}
	
	public void save(File f){
		try {
			exportResults(getLengths(krillShapes, ruler.getConversion()), f);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}			
	}
	
	public void recalculate(){
		if (ruler == null) return;
		resultsPanel.setResults(getLengths(krillShapes, ruler.getConversion()));
	}
	
	public static void main(String[] args) {
		System.out.println("Loading KrillApp...");
		new KrillApp().load(); 
	}
}
