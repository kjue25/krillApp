package krill;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

@SuppressWarnings("serial")
public class ResultsPanel extends JPanel {

	private JLabel Results;

	public ResultsPanel() {
		super(new MigLayout());
		Results = new JLabel("Results");
	}

	private void updatePanel(List<Double> lengths) {
		System.out.println("Updating panel");
		this.removeAll();
		this.add(Results, "wrap");
		int counter = 0;
		
		for (Double length : lengths){
			counter++;
			add(new JLabel(counter + ".   "
					+ String.format("%1$.3f", length) + " mm"), "wrap");
		}
		revalidate();
		repaint();
	}

	public void setResults(List<Double> lengths) {
		updatePanel(lengths == null ? new ArrayList<Double>() : lengths);
	}
	
	public void clearResults() {
		setResults(null);
	}

}
