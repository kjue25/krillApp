package krill;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;

import krill.KrillApp.KrillShapes;
import krill.KrillApp.LineState;
import net.miginfocom.swing.MigLayout;

public class PicturePanel extends JPanel implements MouseListener, MouseMotionListener{

	//BufferedImages
	private BufferedImage image;

	private JLabel title; //panel title

	private LineState state; //state for the MouseListener

	private EditType editing; //whether or not we are editing a line

	private enum EditType {
		NONE, 
		POINT1,
		POINT2
	}
	//Lists of points for drawing a piecewise line
	private List <Point2D.Double> piecewisePoints;
	private List <Point2D.Double> lastSetOfPoints;

	//Shapes
	private List <Shape> shapes;
	private List <KrillShapes> krillShapes;
	private Line2D.Double line;
	private Rectangle2D.Double point;
	private Rectangle2D.Double select;
	private Point2D.Double newPoint;
	private Line2D.Double editLine;
	private Line2D.Double piecewiseSegment;

	//List of lines to edit and related variables
	private int counter = 0; //which line or segment to edit
	private String segment = "no segment"; //when dealing with piecewise lines

	private Ruler ruler;

	private KrillApp app;

	public PicturePanel(KrillApp app) {
		super(new MigLayout());
		setImage(null);
		//setImages(images); 
		shapes = new ArrayList <Shape>();
		krillShapes = new ArrayList <KrillShapes>();
		piecewisePoints = new ArrayList <Point2D.Double>();
		lastSetOfPoints = new ArrayList <Point2D.Double>();
		addMouseListener(this);
		addMouseMotionListener(this);
		this.app = app;

		//Panel title
		title = new JLabel("Image Panel");
		add(title, "wrap"); 

		this.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR)); //type of cursor

		shapes = new ArrayList<Shape>(); 
	}


	//Called to set the state for the MouseListener
	public void setState(LineState assignedState){ state = assignedState; 
	System.out.println(" " + assignedState);
	}

	//Sets a single image to the panel
	public void setImage(BufferedImage image){
		this.image = image;
		repaint();
	}

	//Clears the array of shapes
	public void clearShapes(){
		shapes.clear();
		krillShapes.clear();
		repaint();
	}

	//Removes the last shape in the array
	public void undoShapes(){
		int n = shapes.size();
		if (n > 0){
			shapes.remove(n-1);
		}
		repaint();
	}

	//Adds the BufferedImages and shapes to the panel
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;

		//for one image
		if (image!=null){
			g2.drawImage(image, 0, 0, null); 
		} else
			System.out.println("image is null");

		//Drawing
		g2.setColor(Color.red);
		for (Shape shape : new ArrayList <Shape> (shapes)){
			g2.draw(shape);
		}

		int counter = 1;
		g2.setColor(Color.cyan); 
		for (KrillShapes krill : new ArrayList <KrillShapes> (krillShapes)) {
			g2.draw(krill.getEye());
			for (Line2D.Double segment : krill.getBody()){
				g2.draw(segment);
			}
			Rectangle boundingBox = krill.getBox();
			g2.drawString(counter++ + "", boundingBox.x + boundingBox.width/2, boundingBox.y + boundingBox.height/2);
		}
		g2.setColor(Color.green);
		if (ruler!=null)
			for (Line2D.Double rulerLine : ruler.getRulerLines()) {
				g2.draw(rulerLine); 
			}
	}

	public void setKrill(List<KrillShapes> krill){
		krillShapes.clear();
		krillShapes.addAll(krill);
		repaint();
	}

	public void setRuler(Ruler ruler){
		this.ruler = ruler;
		repaint();
	}

	//Finds the closest line (and its corresponding endpoint) to the selected point
	public Line2D.Double getClosestLine(double x, double y){
		Line2D.Double closest = null; 
		double curDistance = Double.MAX_VALUE;

		for (Shape thisShape : shapes){
			if (!(thisShape instanceof Line2D.Double)){
				//If the shape is not a line, exit
				editing = EditType.NONE;
				System.out.println("no line found");
				continue;
			}

			Line2D.Double thisLine = (Line2D.Double) thisShape;
			double newStartDifference = Calculations.calculateDistance(x, thisLine.getX1(), y, thisLine.getY1());
			double newEndDifference = Calculations.calculateDistance(x, thisLine.getX2(), y, thisLine.getY2());

			if (newStartDifference < curDistance){
				closest = thisLine;
				editing = EditType.POINT1;
				System.out.println("Editing point1");
				curDistance = newStartDifference;
			}
			if (newEndDifference < curDistance){
				closest = thisLine;
				editing = EditType.POINT2;
				System.out.println("Editing point2");
				curDistance = newEndDifference;
			}
		}

		if (curDistance < 50){
			return closest;
		}
		return null;
	}

	//MouseListener and MouseMotionListener
	private static final double POINT_WIDTH = 4;
	private static final double POINT_HEIGHT = 4;

	@Override
	public void mouseClicked(MouseEvent e) {
		System.out.println("MOUSECLICKED");
		double x = e.getX();
		double y = e.getY();

		if (state == LineState.POINT){
			point = new Rectangle2D.Double(x - POINT_WIDTH/2, y - POINT_HEIGHT/2, POINT_WIDTH, POINT_HEIGHT);
			shapes.add(point);
			repaint();
		} else if (state == LineState.PIECEWISE){
			newPoint = new Point2D.Double(x, y);
			piecewisePoints.add(newPoint);

			if (!piecewisePoints.isEmpty()){
				piecewiseSegment = new Line2D.Double(piecewisePoints.get(0), piecewisePoints.get(0));
				segment = "segment exists";
			}

			if (counter > 0 && segment == "segment exists"){
				piecewiseSegment.setLine(piecewisePoints.get(counter).getX(), piecewisePoints.get(counter).getY(), piecewisePoints.get(counter-1).getX(), piecewisePoints.get(counter-1).getY());
				shapes.add(piecewiseSegment);
			}

			repaint();
			counter++;

			if (e.getClickCount() > 1){
				segment = "no segment";	
				for (Point2D.Double point : piecewisePoints){
					lastSetOfPoints.add(point);
				}
				piecewisePoints.clear();
				repaint();
				counter = 0;
			}
		} else if (state == LineState.EDIT){
			editLine = getClosestLine(e.getX(), e.getY());
			if (e.getClickCount() > 1){
				editing = EditType.NONE;
				repaint();
			}
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void mousePressed(MouseEvent e) {	
		System.out.println("MOUSEPRESSED");

		if (state == LineState.STRAIGHT){
			double startX = e.getX();
			double startY = e.getY();
			line = new Line2D.Double(startX, startY, startX, startY);
			shapes.add(line);
		} else if (state == LineState.SELECT){
			select = new Rectangle2D.Double(e.getX(), e.getY(), 0, 0);
			shapes.add(select);
		} else if (state == LineState.EDITKRILL){
			editSetup(e.getPoint());
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {	
		System.out.println("MOUSERELEASED");
		if (state == LineState.EDITKRILL){
			editStop();
		}
	}

	@Override
	public void mouseDragged(MouseEvent e) {		
		System.out.println("MOUSEDRAGGED");

		if (state == LineState.STRAIGHT){
			double endX = e.getX();
			double endY = e.getY();
			line.setLine(line.getX1(), line.getY1(), endX, endY);
			repaint();
		} else if (state == LineState.SELECT){
			double cornerX = e.getX();
			double cornerY = e.getY();
			select.setRect(select.getX(), select.getY(), cornerX - (select.getX()), cornerY - (select.getY()));
			repaint();
		} else if (state == LineState.EDITKRILL){
			editDrag(e.getPoint());
		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {		
		double newX = e.getX();
		double newY = e.getY();

		if (state == LineState.PIECEWISE){
			if (segment == "segment exists"){
				piecewiseSegment.setLine(piecewiseSegment.getX1(), piecewiseSegment.getY1(), newX, newY);
				shapes.add(piecewiseSegment);
				repaint();
			}
		} else if (state == LineState.EDIT && editLine!= null){
			if (editing == EditType.POINT1){
				editLine.setLine(newX, newY, editLine.getX2(), editLine.getY2());
			} else if (editing == EditType.POINT2){
				editLine.setLine(editLine.getX1(), editLine.getY1(), newX, newY);
			}
			repaint();
		}
	}	


	private KrillShapes editKrill = null; 
	private Line2D.Double leftSegment = null;
	private Line2D.Double rightSegment = null;
	private static final int EDIT_LAST_SEGMENT = -1;
	private static final int PIXELRADIUS = 10;

	private void editSetup(Point p){
		int bestIndex = 0;
		//finds best krill within PIXELRADIUS; if none, returns null
		editKrill = findBestEditKrill(p);
		if (editKrill == null){ return; }

		setLeftRightSegments(editKrill, p);
		editDrag(p);
	}

	private void setLeftRightSegments(KrillShapes bestKrill, Point p){
		List<Line2D.Double> body = bestKrill.getBody(); 
		int first = Integer.MAX_VALUE, last = Integer.MIN_VALUE; 
		Line2D.Double left, right;
		
		for (int idx = 0; idx < body.size(); idx++){
			Line2D.Double krillSegment = body.get(idx); 
			double segmentDistanceP1 = KrillImageTools.calculateDistance(p, krillSegment.getP1());
			double segmentDistanceP2 = KrillImageTools.calculateDistance(p, krillSegment.getP2());
			if (segmentDistanceP1 < PIXELRADIUS || segmentDistanceP2 < PIXELRADIUS){
				first = Math.min(first, idx); 
				last = Math.max(last, idx); 
			}
		}
		
		//something bad happened...
		if (first == Integer.MAX_VALUE || last == Integer.MIN_VALUE) {
			right = null; 
			left = null;
			return;
		}

		List<Line2D.Double> newBody = new ArrayList<Line2D.Double>(body.size()); 
		if (first == 0){
			right = body.get(last);
			left = null;
			for (int i = last; i< body.size(); i++)
				newBody.add(body.get(i)); 
		} else if (last == body.size()-1){
			right = null;
			left = body.get(first);
			for (int i = 0; i <= first; i++) {
				newBody.add(body.get(i)); 
			}
		} else {
			left = body.get(first); 
			right = body.get(last); 
			if (left == right) {
				left = new Line2D.Double(right.getP1(), right.getP2()); 
				for (int i = 0; i<first; i++)
					newBody.add(body.get(i));
				newBody.add(right); 
				for (int i = last; i<body.size(); i++)
					newBody.add(body.get(i));
			} else {
				for (int i = 0; i<=first; i++){
					newBody.add(body.get(i));
				}
				for (int i = last; i<body.size(); i++){
					newBody.add(body.get(i));	
				}
			}
		}
		bestKrill.updateBody(newBody); 
		leftSegment = left;
		rightSegment = right; 

	}

	private KrillShapes findBestEditKrill(Point p){
		double bestDistance = Double.MAX_VALUE;
		KrillShapes bestKrill = null;

		for (KrillShapes krill : new ArrayList<KrillShapes>(krillShapes)){
			if (krill.getBox().contains(p)){
				for (Line2D.Double segment : krill.getBody()){
					double distance = KrillImageTools.calculateDistance(p, segment.getP1());
					if (distance < PIXELRADIUS && distance < bestDistance){
						bestDistance = distance;
						bestKrill = krill;
					}
				}
				Line2D.Double lastSegment = krill.getBody().get(krill.getBody().size()-1);
				double distance = KrillImageTools.calculateDistance(p, lastSegment.getP2());
				if (distance < PIXELRADIUS && distance < bestDistance){
					bestDistance = distance;
					bestKrill = krill;
				}
			}
		}
		return bestKrill;
	}

	private void editStop(){
		leftSegment = null;
		rightSegment = null;
		editKrill = null;
	}

	private void editDrag(Point p){
		//called when mouse is dragged
		//updates the lines with the p

		if (leftSegment!= null) {
			leftSegment.x2 = p.x;
			leftSegment.y2 = p.y;
		}
		if (rightSegment!= null){
			rightSegment.x1 = p.x;
			rightSegment.y1 = p.y;
		}
		repaint();

		if (editKrill!= null) editKrill.recalculate();

		app.recalculate();

	}

}
