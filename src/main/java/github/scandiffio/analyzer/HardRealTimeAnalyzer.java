package github.scandiffio.analyzer;

import java.math.BigDecimal;
import java.util.List;

import org.oristool.models.stpn.TransientSolution;
import org.oristool.models.stpn.trans.TreeTransient;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;

public class HardRealTimeAnalyzer {

	public static TransientSolution<Marking, Marking> runTransientAnalysis(PetriNet petriNet, Marking initialMarking,
			String timeBound, String timeStep) {
		TreeTransient analysis = TreeTransient.builder().greedyPolicy(new BigDecimal(timeBound), BigDecimal.ZERO)
				.timeStep(new BigDecimal(timeStep)).build();

		// TODO sostituire print con log
		System.out.println("DEBUG: running transient analysis");
		TransientSolution<Marking, Marking> result = analysis.compute(petriNet, initialMarking);
		System.out.print("Transient analysis complete");
		return result;
	}

	public boolean checkSolutionForDeadline(TransientSolution solution, List<List<String>> tasksPlaces) {

		List<Marking> stateMarkings = solution.getColumnStates();
		if (stateMarkings.isEmpty())
			throw new IllegalArgumentException("Empty states");

		Marking currentStateMarking = null;
		boolean deadlineMiss = false;
		int tokenSum = 0;
		int i = stateMarkings.size() - 1;

		while (i >= 0 && !deadlineMiss) { // start from the end. If there is a constraint violation, it is in the last
											// explored states
			currentStateMarking = stateMarkings.get(i);
			for (List<String> taskPlaces : tasksPlaces) { // for each list of places. One list each task
				tokenSum = 0;
				for (String placeName : taskPlaces) // for each place
					tokenSum += currentStateMarking.getTokens(placeName); // sum the number of tokens
				if (tokenSum > 1) // if the sum is > 1, there are at least two tokens for the same task at the
									// same time
					deadlineMiss = true;
				i--;
			}
		}
		return deadlineMiss;

	}

}
