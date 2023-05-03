package moa.classifiers.core.driftdetection;

import java.util.LinkedList;

import org.apache.mahout.math.Arrays;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
/**

 * @author Daniel Nowak (daniel dot nowak dot assis at gmail dot com)
 * @version $Revision: 1 $
 */
public class ROCAUC {
	
	public LinkedList<double[][]> confusionMatrixes;
	
	double[] thresholds;
	
    public IntOption numThresholds = new IntOption(
            "numThresholds",
            'n',
            "Number of thresholds for integral calculation",
            20, 2, Integer.MAX_VALUE);
    
    
	public static double trapezoidalRule(double[] x, double[] y) {
	    double sum = 0.0;
	    for (int i = 1; i < x.length; i++) {
	        double h = x[i] - x[i-1];
	        double area = h * (y[i] + y[i-1]) / 2.0;
	        sum += area;
	    }
	    return sum;
	}
	
	public void reset() {
		this.thresholds = new double[this.numThresholds.getValue()];
		this.confusionMatrixes = new LinkedList<double[][]>();
		for (int i = 0; i < this.thresholds.length; i++) {
			this.thresholds[i] = (double) i / (this.thresholds.length - 1);
			double[][] matrixes = {{0.0, 0.0},{0.0, 0.0}};
			this.confusionMatrixes.add(matrixes);
		}
		this.thresholds[0] -= 1e-7;
		this.thresholds[this.thresholds.length - 1] += 1e-7;
	}
	
	public ROCAUC update(int yTrue, double yPred) {
		for(int i = 0; i < this.thresholds.length; i++) {
			this.confusionMatrixes.get(i)[yTrue][yPred > this.thresholds[i] ? 1 : 0] += 1;
		}
		return this;
	}
	
	public double getAUC() {
		double[] truePositeRates = new double[this.thresholds.length];
		double[] falsePositeRates = new double[this.thresholds.length];
		
		for(int i = 0; i < this.thresholds.length; i++) {
			double tp = this.confusionMatrixes.get(i)[1][1];
			double tn = this.confusionMatrixes.get(i)[0][0];
			double fp = this.confusionMatrixes.get(i)[0][1];
			double fn = this.confusionMatrixes.get(i)[1][0];
			//System.out.println(tp);
			//System.out.println(fn);
			
			truePositeRates[i] = safeDivision(tp, tp + fn);
			falsePositeRates[i] = safeDivision(fp, fp + tn);
		}
		//System.out.println(Arrays.toString(falsePositeRates));
		return Math.abs(trapezoidalRule(falsePositeRates,truePositeRates));
	}
	
	public double safeDivision(double a, double b) {
		return b > 0 ? a / b : 0;
	}

}
