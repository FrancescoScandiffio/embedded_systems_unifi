package github.scandiffio.experiments;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.oristool.math.expression.Variable;
import org.oristool.models.stpn.RewardRate;
import org.oristool.models.stpn.TransientSolution;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;

import github.scandiffio.analyzer.HardRealTimeAnalyzer;
import github.scandiffio.analyzer.SolverParametersContainer;
import github.scandiffio.analyzer.TaskSetAnalyzer;
import github.scandiffio.analyzer.TaskSetEntry;
import github.scandiffio.experiments.nets.simple_cpu.SimpleCpu;
import github.scandiffio.function.EXP;
import github.scandiffio.function.FunctionInterface;
import github.scandiffio.task.SoftRealTimeTask;
import github.scandiffio.utils.ResultWriter;

public class OneSoftTaskHasPeriodicBehaviour {

	public static void main(String[] args) {

		PetriNet periodicTasksNet = new PetriNet();
		Marking initialMarking = new Marking();

		String timeBound = "45";
		String timeStep = "0.1";
		String cpuReward = "cpu==1";

		BigInteger intTimeBound = new BigInteger(timeBound);
		BigDecimal decimalTimeStep = new BigDecimal(timeStep);

		SimpleCpu.build(periodicTasksNet, initialMarking);

		TransientSolution<Marking, Marking> hardTaskTransientSolution = HardRealTimeAnalyzer
				.runTransientAnalysis(periodicTasksNet, initialMarking, timeBound, timeStep);

		TransientSolution<Marking, RewardRate> cpuFreeSolution = TransientSolution.computeRewards(false,
				hardTaskTransientSolution, cpuReward);

		int samples = hardTaskTransientSolution.getSamplesNumber();

		double[] cpuFree = new double[samples];
		for (int t = 0; t < samples; t++) // samples here are n = bound/step + 1
			cpuFree[t] = cpuFreeSolution.getSolution()[t][0][0];

		// Soft analysis
		Variable x = new Variable("x");
		BigDecimal lambda = new BigDecimal("1");
		BigDecimal mu = new BigDecimal("1");

		FunctionInterface arrival = new EXP(x, lambda);
		FunctionInterface service = new EXP(x, mu);
		SoftRealTimeTask softTask = new SoftRealTimeTask(arrival, service, 1);

		// todo remove constructor
		BigInteger queueSize = new BigInteger("4");
		BigInteger maxDenials = new BigInteger("5");

		TaskSetEntry entry = new TaskSetEntry(softTask,
				new SolverParametersContainer(queueSize, maxDenials, BigInteger.ZERO, BigInteger.ZERO));
		TaskSetAnalyzer set = new TaskSetAnalyzer(cpuFree, intTimeBound, new BigDecimal(timeStep), entry);
		set.analyzeQueues();

		double[][] queueStatus = set.getQueueSolver(softTask.getId()).getStateProbabilitiesAlongTime();
		// System.out.println("QueueSize: " + queueSize.intValue() + " timeBound: " +
		// intTimeBound.intValue()
		// + " timeStep: " + decimalTimeStep.doubleValue());

		set.analyzeDenials();
		double[][] denialStatus = set.getDenialSolver(softTask.getId()).getAtLeastKdenials();

		ResultWriter.writeResultToCsv("QueueResults.csv", queueStatus, Double.valueOf(timeStep), "Jobs in queue");

		System.out.println(" ------------- ");

		ResultWriter.writeResultToCsv("DenialResults.csv", denialStatus, Double.valueOf(timeStep), "At least denials");

		// System.out.println("Cpu free probs ");
		// for (int h = 0; h < cpuFree.length; h++)
		// System.out.println(cpuFree[h]);

	}

}
