package github.scandiffio.analyzer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;

import github.scandiffio.task.SoftRealTimeTask;

/**
 * This is a task set analyzer: it instantiates the equation solvers and uses
 * them to analyze the queue status and denial number of the tasks in the given
 * task-set
 *
 */
public class TaskSetAnalyzer {

	private LinkedHashMap<Integer, QueueEDSolver> queueSolvers;
	private LinkedHashMap<Integer, DenialEDSolver> denialSolvers;
	private ArrayList<SoftRealTimeTask> taskSet;
	private double[] firstCpuFree;
	private BigInteger timeBound;
	private BigDecimal timeStep;

	/**
	 * Builds the task-set analyzer. It sorts tasks by priority and creates equation
	 * solvers. Each task is associated with one QueueEDSolver and one
	 * DenialEDSolver
	 * 
	 * @param hardCpuFree  probability that the hard real-time tasks of the task-set
	 *                     are not using the processor
	 * @param timeBound    analysis end time
	 * @param timeStep     analysis time step
	 * @param inputTaskSet set of entries that make up the task-set to be parsed
	 */

	// TODO BigDecimal al posto di BigInteger come timeBound
	public TaskSetAnalyzer(double[] hardCpuFree, BigInteger timeBound, BigDecimal timeStep,
			TaskSetEntry... inputTaskSet) {

		int timeBoundStep = (int) (timeBound.intValue() / timeStep.doubleValue()) + 1;
		if (timeBoundStep != hardCpuFree.length)
			throw new IllegalArgumentException("CpuProbabilities.length must be equal to timeBound/timeStep +1");

		for (double val : hardCpuFree)
			if (val < 0 || val > 1.01)
				throw new IllegalArgumentException("CpuProbabilities must in the range [0,1]. Got " + val);

		this.firstCpuFree = hardCpuFree.clone();
		this.timeBound = timeBound;
		this.timeStep = timeStep;
		this.taskSet = new ArrayList<SoftRealTimeTask>();
		this.queueSolvers = new LinkedHashMap<Integer, QueueEDSolver>();
		this.denialSolvers = new LinkedHashMap<Integer, DenialEDSolver>();

		SoftRealTimeTask task;
		SolverParametersContainer container;
		QueueEDSolver queueSolver;
		DenialEDSolver denialSolver;

		for (TaskSetEntry entry : inputTaskSet) {
			task = entry.getTask();
			if (taskSet.contains(task))
				throw new IllegalArgumentException(
						"Cannot add the same task (same id) more than once. Task with id " + task.getId());

			container = entry.getParameterContainer();
			queueSolver = new QueueEDSolver(task.getArrivalDistribution(), task.getServiceDistribution(),
					container.getQueueSize(), container.getQueuedJobsDistribution());

			denialSolver = new DenialEDSolver(task.getArrivalDistribution(), task.getServiceDistribution(),
					container.getQueueSize(), container.getMaxDenials(), container.getQueuedJobsDistribution(),
					container.getInitialDenialsDistribution());

			this.taskSet.add(task);
			this.queueSolvers.put(task.getId(), queueSolver);
			this.denialSolvers.put(task.getId(), denialSolver);
		}

		taskSet.sort(Comparator.comparing(SoftRealTimeTask::getPriority));

	}

	/**
	 * Adds an entry to the task-set
	 * 
	 * @param entry new entry to be added
	 */
	public void addEntry(TaskSetEntry entry) {
		SoftRealTimeTask task = entry.getTask();
		if (taskSet.contains(task))
			throw new IllegalArgumentException(
					"Cannot add the same task (same id) more than once. Task with id " + task.getId());
		SolverParametersContainer container = entry.getParameterContainer();
		QueueEDSolver queueSolver = new QueueEDSolver(task.getArrivalDistribution(), task.getServiceDistribution(),
				container.getQueueSize(), container.getQueuedJobsDistribution());

		DenialEDSolver denialSolver = new DenialEDSolver(task.getArrivalDistribution(), task.getServiceDistribution(),
				container.getQueueSize(), container.getMaxDenials(), container.getQueuedJobsDistribution(),
				container.getInitialDenialsDistribution());

		this.taskSet.add(task);
		this.queueSolvers.put(task.getId(), queueSolver);
		this.denialSolvers.put(task.getId(), denialSolver);
		taskSet.sort(Comparator.comparing(SoftRealTimeTask::getPriority));

	}

	/**
	 * Removes a task from the task-set
	 * 
	 * @param taskId id of the task to be removed
	 */
	public void removeEntry(int taskId) {
		if (taskSet.isEmpty())
			throw new IllegalArgumentException("Can not remove any object from empty task set");
		queueSolvers.remove(taskId);
		denialSolvers.remove(taskId);
		taskSet.removeIf(n -> (n.getId() == taskId));
	}

	/**
	 * Launches task queue analysis
	 */
	public void analyzeQueues() {
		if (taskSet.isEmpty())
			throw new IllegalArgumentException("Can not analyze an empty task set");
		double[] currentCpuFree = firstCpuFree;
		System.out.println("------- Analyze queue task id: " + taskSet.get(0).getId() + " -------");
		QueueEDSolver solver = queueSolvers.get(taskSet.get(0).getId()).analyze(timeStep, timeBound, firstCpuFree);
		for (int i = 1; i < taskSet.size(); i++) {
			System.out.println("\n------- Analyze queue task id: " + taskSet.get(i).getId() + " -------");
			currentCpuFree = computeNextCpuProbs(currentCpuFree, solver.getpExtendedAlongTime());
			solver = queueSolvers.get(taskSet.get(i).getId()).analyze(timeStep, timeBound, currentCpuFree);
		}
	}

	/**
	 * Launches task denial analysis
	 */
	public void analyzeDenials() {
		if (taskSet.isEmpty())
			throw new IllegalArgumentException("Can not analyze an empty task set");
		double[] currentCpuFree = firstCpuFree;
		System.out.println("------- Analyze denials task id: " + taskSet.get(0).getId() + " ------- ");
		DenialEDSolver solver = denialSolvers.get(taskSet.get(0).getId()).analyze(timeStep, timeBound, firstCpuFree);
		for (int i = 1; i < taskSet.size(); i++) {
			System.out.println("------- Analyze denials task id: " + taskSet.get(i).getId() + " ------- ");
			currentCpuFree = computeNextCpuProbs(currentCpuFree, solver.getExtendedDenials());
			solver = denialSolvers.get(taskSet.get(i).getId()).analyze(timeStep, timeBound, currentCpuFree);
		}

	}

	/**
	 * Changes the duration time of the analysis and/or the time step
	 * 
	 * @param hardCpuFree new probability that hard real-time tasks of the task-set
	 *                    are not using the processor. The length of the array must
	 *                    be consistent with the new timeBound and timeStep values
	 * @param timeBound   new analysis time bound
	 * @param timeStep    new analysis time step
	 */
	public void changeTimeInterval(double[] hardCpuFree, BigInteger timeBound, BigDecimal timeStep) {
		int timeBoundStep = (int) (timeBound.intValue() / timeStep.doubleValue()) + 1;
		if (timeBoundStep != hardCpuFree.length)
			throw new IllegalArgumentException("CpuProbabilities.length must be equal to timeBound/timeStep +1");

		for (double val : hardCpuFree)
			if (val < 0 || val > 1.01)
				throw new IllegalArgumentException("CpuProbabilities must in the range [0,1]. Got " + val);

		this.firstCpuFree = hardCpuFree.clone();
		this.timeBound = timeBound;
		this.timeStep = timeStep;
	}

	private double[] computeNextCpuProbs(double[] previousCpuFree, double[][][][] previousStateProbabilities) {
		double[] nextProbs = new double[firstCpuFree.length];
		for (int t = 0; t < nextProbs.length; t++)
			nextProbs[t] = previousCpuFree[t] * previousStateProbabilities[t][0][0][0];
		return nextProbs;
	}

	private double[] computeNextCpuProbs(double[] previousCpuFree, double[][][][][] previousStateProbabilities) {
		double[] nextProbs = new double[firstCpuFree.length];
		for (int t = 0; t < nextProbs.length; t++)
			nextProbs[t] = previousCpuFree[t] * previousStateProbabilities[t][0][0][0][0];
		return nextProbs;
	}

	/**
	 * Returns the QueueEDSolver associated with the given task
	 * 
	 * @param taskId id of the task
	 * @return the QueueEDSolver associated with the given task
	 */
	public QueueEDSolver getQueueSolver(int taskId) {
		return queueSolvers.get(taskId);
	}

	/**
	 * Returns the DenialEDSolver associated with the given task
	 * 
	 * @param taskId id of the task
	 * @return the DenialEDSolver associated with the given task
	 */
	public DenialEDSolver getDenialSolver(int taskId) {
		return denialSolvers.get(taskId);
	}

	/**
	 * 
	 * @return a list with all the QueueEDSolver associated with the tasks in the
	 *         task-set. DIRE IN CHE ORDINE VENGONO RESTITUITI
	 */
	public ArrayList<QueueEDSolver> getAllQueueSolvers() {
		ArrayList<QueueEDSolver> queueList = new ArrayList<QueueEDSolver>();
		for (QueueEDSolver solver : queueSolvers.values())
			queueList.add(solver);

		return queueList;
	}

	/**
	 * 
	 * @return a list with all the DenialEDSolver associated with the tasks in the
	 *         task-set. DIRE IN CHE ORDINE VENGONO RESTITUITI
	 */
	public ArrayList<DenialEDSolver> getAllDenialSolvers() {
		ArrayList<DenialEDSolver> denialList = new ArrayList<DenialEDSolver>();
		for (DenialEDSolver solver : denialSolvers.values())
			denialList.add(solver);

		return denialList;
	}

	public ArrayList<SoftRealTimeTask> getTaskSet() {
		return taskSet;
	}

}
