package moa.classifiers.core.driftdetection;

import java.util.LinkedList;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;

import moa.classifiers.Classifier;
import moa.core.ObjectRepository;
import moa.core.Utils;
import moa.options.ClassOption;
import moa.tasks.TaskMonitor;
import moa.classifiers.Classifier;
/**

 * @author Daniel Nowak (daniel dot nowak dot assis at gmail dot com)
 * @version $Revision: 1 $
 */
public class D3 extends AbstractUnsupervisedChangeDetector  {

	public ClassOption discriminativeLearnerOption = new ClassOption("discriminativeOption", 'l',
            "Discriminative Learner to train.", Classifier.class, "trees.HoeffdingTree -g 50 -c 0.01");
	
	public IntOption windowSize = new IntOption(
            "windowSize",
            'w',
            "Number of thresholds for integral calculation",
            100, 2, Integer.MAX_VALUE);
	
	public FloatOption AucThresholdOption = new FloatOption("AucThresholdOption", 'a',
            "Auc Threshold for Drift Detection.", 0.7, 0.0, 1.0);
	
	Classifier discriminativeClassifier;
	
	ROCAUC auc;
	
	LinkedList<Instance> OldDataWindow = new LinkedList<Instance>();
	LinkedList<Instance> NewDataWindow = new LinkedList<Instance>();
	
	int subWindowSize = 0;
	
	long instSeen = 0;
	
	
	@Override
	public void input_wi(Instance inst) {
		instSeen += 1;
		if(this.discriminativeClassifier == null) {
			this.init(inst);
		}
		
		this.isChangeDetected = false;
		if(this.OldDataWindow.size() <= subWindowSize) {
			this.OldDataWindow.add(inst);
			return;
		}
		//System.out.println(this.NewDataWindow);
		this.NewDataWindow.add(inst);
		Instance oldInst = OldDataWindow.removeFirst().copy();
		oldInst.setClassValue(0);
		
		Instance newInst = inst.copy();
		newInst.setClassValue(1);
		
		this.discriminativeClassifier.trainOnInstance(oldInst);
		this.discriminativeClassifier.trainOnInstance(newInst);
		//update AUC
		auc.update(0, Utils.maxIndex(this.discriminativeClassifier.getVotesForInstance(oldInst)));
		auc.update(1, Utils.maxIndex(this.discriminativeClassifier.getVotesForInstance(newInst)));
		
		if(this.NewDataWindow.size() == subWindowSize) {
			double aucScore = auc.getAUC();
			this.isChangeDetected = aucScore > AucThresholdOption.getValue() || aucScore < AucThresholdOption.getValue() - 0.5;
			this.discriminativeClassifier = null;
		}
	}

	@Override
	public long getNumInstances() {
		// TODO Auto-generated method stub
		return this.OldDataWindow.size();
	}

	@Override
	public void input(double inputValue) {
		
	}
	
	public void init(Instance inst) {
		this.discriminativeClassifier = (Classifier) getPreparedClassOption(this.discriminativeLearnerOption);
		this.discriminativeClassifier.resetLearning();
		this.auc = new ROCAUC();
		this.auc.reset();
		if(!this.NewDataWindow.isEmpty()) {
			this.OldDataWindow = (LinkedList<Instance>) this.NewDataWindow.clone();
			this.NewDataWindow = new LinkedList<Instance>();
		}else {
			this.NewDataWindow = new LinkedList<Instance>();
			this.OldDataWindow = new LinkedList<Instance>();
		}
		this.OldDataWindow = new LinkedList<Instance>();
		this.subWindowSize = this.windowSize.getValue() / 2;
	}

	@Override
	public void getDescription(StringBuilder sb, int indent) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
		// TODO Auto-generated method stub
		
	}
	
}
