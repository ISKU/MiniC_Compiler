import java.util.ArrayList;

public class BasicBlock {
	public static int blockSize = 1;
	public String blockNumber;
	public ArrayList<String> labels;
	public ArrayList<String> noLabels;
	public ArrayList<BasicBlock> predecessors;
	public ArrayList<BasicBlock> successors;
	
	public BasicBlock(String blockNumber) {
		 labels = new ArrayList<>();
		 noLabels = new ArrayList<>();
		 predecessors = new ArrayList<>();
		 successors = new ArrayList<>();
		 this.blockNumber = blockNumber;
	}
	
	public BasicBlock(String blockNumber, ArrayList<String> labelList, ArrayList<String> noLabelList) {
		 labels = new ArrayList<>();
		 labels.addAll(labelList);
		 noLabels = new ArrayList<>();
		 noLabels.addAll(noLabelList);
		 predecessors = new ArrayList<>();
		 successors = new ArrayList<>();
		 this.blockNumber = blockNumber;
	}
	
	public void addPredecessor(BasicBlock predecessor) {
		predecessors.add(predecessor);
	}
	
	public void addSuccessors(BasicBlock successor) {
		successors.add(successor);
	}
	
	public String print() {
		StringBuilder code = new StringBuilder();
		for(String instr : labels)
			code.append(instr).append("\n");
		return code.toString();
	}
	
	public String printCode() {
		StringBuilder code = new StringBuilder();
		for(String instr : noLabels)
			code.append(instr).append("\n");
		return code.toString();
	}
	
	@Override
	public String toString() {
		StringBuilder info = new StringBuilder();
		
		info.append("Predecessors: ");
		for (int index = 0; index < predecessors.size(); index++)
			info.append(predecessors.get(index).blockNumber).append(" ");
		info.append("\n");
		
		info.append("Successors: ");
		for (int index = 0; index < successors.size(); index++)
			info.append(successors.get(index).blockNumber).append(" ");
		info.append("\n");
		
		return info.toString();
	}
} 
