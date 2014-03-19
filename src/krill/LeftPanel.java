package krill;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import krill.KrillApp.LineState;
import net.miginfocom.swing.MigLayout;

@SuppressWarnings("serial")
public class LeftPanel extends JPanel implements MouseListener {
	
	//Buttons
	private JButton chooseFile;
	private JButton measure;
	private JButton label;
	private JButton lines;
	private JButton points;
	private JButton select;
	private JButton clear;
	private JButton undo;
	private JButton edit;
	private JButton saveFile;
	private JButton clearResults;
	private List <JPopupMenu> popUps;
	
	//Instances of other panels
	private KrillApp app;
	
	//BufferedImage
	private BufferedImage bufferImage;	
	
	//private Graphics g;
	
	public LeftPanel(KrillApp app) {	
		super (new MigLayout()); 
		this.app = app;
		add(new JLabel("Menu"), "wrap"); 
		
		popUps = new ArrayList <JPopupMenu>(); //array of JPopupMenus
		addMouseListener(this);
				
		//Creates a button labeled "Open File" that opens a JFileChooser when pressed
		chooseFile = new JButton("Open File");
		add(chooseFile, "wrap");
		chooseFile.addActionListener(chooseFilePressed);
		
		//Creates a button labeled "Measure" that opens a JPopupMenu when pressed
		measure = new JButton("Measure");
		add(measure, "wrap");
		measure.addActionListener(measurePressed);
		
		//Creates a button labeled "Label"
		label = new JButton("Label");
		add(label, "wrap");
		label.addActionListener(labelPressed);
		
		//Creates a button labeled "Lines" that opens a JPopupMenu when pressed
		lines = new JButton("Lines");
		add(lines, "wrap");
		lines.addActionListener(linesPressed);
		
		//Creates a button labeled "Points"
		points = new JButton("Points");
		add(points, "wrap");
		points.addActionListener(pointsPressed);
		
		//Creates a button labeled "Selection tool"
		select = new JButton("Selection tool");
		add(select, "wrap");
		select.addActionListener(selectPressed);
		
		//Creates a button labeled "Clear"
		clear = new JButton("Clear");
		add(clear, "wrap");
		clear.addActionListener(clearPressed);

		//Creates a button labeled "Undo"
		undo = new JButton("Undo");
		add(undo, "wrap");
		undo.addActionListener(undoPressed);
		
		//Creates a button labeled "Edit"
		edit = new JButton("Edit");
		add(edit, "wrap");
		edit.addActionListener(editPressed);
		
		saveFile = new JButton("Save Results");
		add(saveFile, "wrap");
		saveFile.addActionListener(saveFilePressed);
		
		clearResults = new JButton("Clear Results");
		add(clearResults, "wrap");
		clearResults.addActionListener(clearResultsPressed);
	}

	//MouseListener and MouseMotionListener
	@Override
	public void mouseClicked(MouseEvent arg0) {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub	
	}
	
	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub	
	}
	
	public void mousePressed(MouseEvent e){
		// TODO Auto-generated method stub
	}
	
	public void mouseReleased(MouseEvent e){
		for (JPopupMenu menu: popUps){
			menu.setVisible(false);
		}
		popUps.clear();
	}
	
	//ActionListerners for each button
	private ActionListener chooseFilePressed = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			JFileChooser selectFolder = new JFileChooser();
			selectFolder.setMultiSelectionEnabled(true);
			selectFolder.showOpenDialog(LeftPanel.this);
			System.out.println("abs path: " + selectFolder.getSelectedFile().getAbsolutePath());

			//Setting image
			BufferedImage selectedImage = ImageTools.loadImage(selectFolder.getSelectedFile());
			bufferImage = selectedImage;
			app.setImage(selectedImage);
			
			app.updateViewport();
		}
	};
	
	private ActionListener measurePressed = new ActionListener(){
		@Override
		public void actionPerformed(ActionEvent arg0) {
			JPopupMenu measureType = new JPopupMenu();
			popUps.add(measureType);
			JMenuItem measureAll = new JMenuItem("All");
			measureAll.addActionListener(measureAllPressed);
			measureType.add(measureAll);
			JMenuItem measureSelected = new JMenuItem("Selected");
			measureSelected.addActionListener(measureSelectedPressed);
			measureType.add(measureSelected);
			JMenuItem measureManual = new JMenuItem("Manual");
			measureManual.addActionListener(measureManualPressed);
			measureType.add(measureManual);
			measureType.setVisible(true);
			measureType.setLocation(measure.getLocation());
		}
	};
	
	private ActionListener measureAllPressed = new ActionListener(){
		
		@Override
		public void actionPerformed(ActionEvent e) {
			app.analyzeImage();
		}
	};
	
	private ActionListener measureSelectedPressed = new ActionListener(){
		
		@Override
		public void actionPerformed(ActionEvent e) {

		}	
	};
	
	private ActionListener measureManualPressed = new ActionListener(){

		@Override
		public void actionPerformed(ActionEvent e){

		}
	};
	
	private ActionListener labelPressed = new ActionListener(){
		
		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
		}	
	};
	
	private ActionListener linesPressed = new ActionListener(){
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			JPopupMenu lineType = new JPopupMenu();
			popUps.add(lineType);
			JMenuItem straight = new JMenuItem("Straight");
			straight.addActionListener(straightLinePressed);
			lineType.add(straight);
			JMenuItem piecewise = new JMenuItem("Piecewise");
			piecewise.addActionListener(piecewiseLinePressed);
			lineType.add(piecewise);
			lineType.setVisible(true);
			lineType.setLocation(lines.getLocation());		
		}	
	};
	
	private ActionListener straightLinePressed = new ActionListener(){
		
		@Override
		public void actionPerformed(ActionEvent e) {
			app.setEditState(LineState.STRAIGHT);
		}	
	};
	
	private ActionListener piecewiseLinePressed = new ActionListener(){
	
		@Override
		public void actionPerformed(ActionEvent e) {
			app.setEditState(LineState.PIECEWISE);
		}
	};
	
	private ActionListener pointsPressed = new ActionListener(){
	
		@Override
		public void actionPerformed(ActionEvent e) {
			app.setEditState(LineState.POINT);
		}	
	};
	
	private ActionListener selectPressed = new ActionListener(){
	
		@Override
		public void actionPerformed(ActionEvent e) {
			app.setEditState(LineState.SELECT);
		}	
	};
	
	private ActionListener clearPressed = new ActionListener(){
		
		@Override
		public void actionPerformed(ActionEvent arg0) {

		}	
	};
	
	private ActionListener undoPressed = new ActionListener(){

		@Override
		public void actionPerformed(ActionEvent e) {

		}	
	};
		
	private ActionListener editPressed = new ActionListener(){

		@Override
		public void actionPerformed(ActionEvent arg0) {
			app.setEditState(LineState.EDITKRILL);
		}
	};
	
	private ActionListener saveFilePressed = new ActionListener(){

		@Override
		public void actionPerformed(ActionEvent arg0) {
			JFileChooser save = new JFileChooser();
			save.showSaveDialog(LeftPanel.this);
			app.save(new File(save.getSelectedFile().getAbsolutePath()));
			System.out.println("abs path: " + save.getSelectedFile().getAbsolutePath());	
		}
		
	};
	
	private ActionListener clearResultsPressed = new ActionListener(){

		@Override
		public void actionPerformed(ActionEvent arg0) {

		}
		
	};



}
