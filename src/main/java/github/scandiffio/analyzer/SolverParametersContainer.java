package github.scandiffio.analyzer;

import java.math.BigInteger;

/**
 * This is a container for the parameters needed by the equation solvers
 * 
 *
 */
public class SolverParametersContainer {

	private BigInteger queueSize;
	private BigInteger maxDenials;
	private double[] queuedJobsDistribution;
	private double[] initialDenialsDistribution;

	/**
	 * Build the container
	 * 
	 * @param queueSize  max size of the queue of a process
	 * @param maxDenials max number of allowed denials
	 * @param queuedJobs number of jobs in queue at time t=0
	 * @param deniedJobs number of denials at time t=0
	 */
	public SolverParametersContainer(BigInteger queueSize, BigInteger maxDenials, BigInteger queuedJobs,
			BigInteger deniedJobs) {
		this(queueSize, maxDenials, createProbs(queuedJobs, queueSize), createProbs(deniedJobs, maxDenials));
	}

	/**
	 * Build the container with given probability distribution of queued or denied
	 * jobs
	 * 
	 * @param queueSize                  max size of the queue of a process
	 * @param maxDenials                 max number of allowed denials
	 * @param queuedJobsDistribution     probability distribution of queued jobs at
	 *                                   time t=0
	 * @param initialDenialsDistribution probability distribution of denied jobs at
	 *                                   time t=0
	 */
	public SolverParametersContainer(BigInteger queueSize, BigInteger maxDenials, double[] queuedJobsDistribution,
			double[] initialDenialsDistribution) {

		if (queueSize.intValue() <= 0)
			throw new IllegalArgumentException("QueueSize must be greater than zero");

		if (maxDenials.intValue() < 0)
			throw new IllegalArgumentException("MaxDenials must be greater than zero");

		if (queuedJobsDistribution.length != queueSize.intValue() + 1)
			throw new IllegalArgumentException("queuedJobsDistribution.length must be equal to queueSize +1");

		if (initialDenialsDistribution.length != maxDenials.intValue() + 1)
			throw new IllegalArgumentException("deniedJobsDistribution.length must be equal to maxDenials +1");

		double sum = 0.0;
		for (double val : queuedJobsDistribution) {
			if (val < 0.0 || val > 1.01)
				throw new IllegalArgumentException("A single probability must be in range [0,1]. Got " + val);
			sum += val;
		}
		if (sum < 0.99 || sum > 1.01)
			throw new IllegalArgumentException("The sum of queued probabilities must be 1. Got " + sum);

		sum = 0.0;
		for (double val : initialDenialsDistribution) {
			if (val < 0.0 || val > 1.01)
				throw new IllegalArgumentException("A single probability must be in range [0,1]. Got " + val);
			sum += val;
		}

		if (sum < 0.99 || sum > 1.01)
			throw new IllegalArgumentException("The sum of denial probabilities must be 1. Got " + sum);

		this.queueSize = queueSize;
		this.maxDenials = maxDenials;
		this.queuedJobsDistribution = queuedJobsDistribution;
		this.initialDenialsDistribution = initialDenialsDistribution;
	}

	private static double[] createProbs(BigInteger value, BigInteger maximum) {
		int val = value.intValue();
		int max = maximum.intValue();
		if (val < 0 || val > max)
			throw new IllegalArgumentException("Selected value : " + val + " must be in range[0," + max + "]");
		double[] probs = new double[max + 1];
		probs[val] = 1.0;
		return probs;
	}

	public BigInteger getQueueSize() {
		return queueSize;
	}

	public BigInteger getMaxDenials() {
		return maxDenials;
	}

	public double[] getQueuedJobsDistribution() {
		return queuedJobsDistribution;
	}

	public double[] getInitialDenialsDistribution() {
		return initialDenialsDistribution;
	}

}
