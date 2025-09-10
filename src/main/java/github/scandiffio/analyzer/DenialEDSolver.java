package github.scandiffio.analyzer;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.apache.commons.math3.util.Precision;

import github.scandiffio.function.FunctionInterface;

/**
 * This is an analyzer for job denials, based on differential equations.
 *
 */
public class DenialEDSolver {

	private double[][][][][] extendedProbabilities;
	private double[][] exactlyKdenials;
	private double[][] atLeastKdenials;

	private double[][] initialDistribution;
	private double[] initialDenialsDistribution;
	private double[] pArrival;
	private double[] pService;
	private BigInteger queueSize;
	private BigInteger maxDenials;
	private BigDecimal[] arrivalLambdas;
	private BigDecimal[] serviceLambdas;

	/**
	 * Builds the Differential equation solver
	 * 
	 * @param arrivalDistribution        probability distribution of arrival events
	 * @param serviceDistribution        probability distribution of service events
	 * @param queueSize                  maximum size of the queue
	 * @param maxDenials                 maximum number of accepted denials
	 * @param initialQueueDistribution   probability distribution of queued job at
	 *                                   time t=0
	 * @param initialDenialsDistribution probability distribution of denied job at
	 *                                   time t=0
	 */
	public DenialEDSolver(FunctionInterface arrivalDistribution, FunctionInterface serviceDistribution,
			BigInteger queueSize, BigInteger maxDenials, double[] initialQueueDistribution,
			double[] initialDenialsDistribution) {

		if (queueSize.intValue() <= 0)
			throw new IllegalArgumentException("QueueSize must be positive");

		if (initialQueueDistribution.length != queueSize.intValue() + 1)
			throw new IllegalArgumentException(
					"The length of queue distribution probabilities must be" + "equal to queueSize+1");

		double sum = 0.0;
		for (double val : initialQueueDistribution) {
			sum += val;
			if (val < 0 || val > 1)
				throw new IllegalArgumentException("In initialQueueDistribution got a probability of " + val);
		}

		if (sum < 0.99 || sum > 1.01)
			throw new IllegalArgumentException(
					"The sum of the probabilities of the initial queued jobs must be 1, got " + sum);

		if (maxDenials.intValue() <= 0)
			throw new IllegalArgumentException("MaxDenials must be positive");

		if (initialDenialsDistribution.length != maxDenials.intValue() + 1)
			throw new IllegalArgumentException(
					"The length of denial distribution probabilities must be" + "equal to queueSize+1");

		sum = 0.0;
		for (double val : initialDenialsDistribution) {
			sum += val;
			if (val < 0 || val > 1)
				throw new IllegalArgumentException("In initialDenialsDistribution got a probability of " + val);
		}

		if (sum < 0.99 || sum > 1.01)
			throw new IllegalArgumentException(
					"The sum of the probabilities of the initial denied jobs must be 1, got " + sum);

		this.queueSize = queueSize;
		this.maxDenials = maxDenials;
		this.arrivalLambdas = DistributionExtender.getExtendedProbs(arrivalDistribution);
		this.serviceLambdas = DistributionExtender.getExtendedProbs(serviceDistribution);
		this.pArrival = new double[arrivalLambdas.length];
		this.pService = new double[serviceLambdas.length];
		initialDistribution = new double[queueSize.intValue() + 1][maxDenials.intValue() + 1];
		for (int n = 0; n <= queueSize.intValue(); n++)
			for (int k = 0; k <= maxDenials.intValue(); k++)
				initialDistribution[n][k] = initialQueueDistribution[n] * initialDenialsDistribution[k];
		this.initialDenialsDistribution = initialDenialsDistribution;
	}

	/**
	 * 
	 * Builds the Differential equation solver
	 * 
	 * @param arrivalDistribution probability distribution of arrival events
	 * @param serviceDistribution probability distribution of service events
	 * @param queueSize           maximum size of the queue
	 * @param maxDenials          maximum number of accepted denials
	 * @param initialJobsInQueue  number of already queued job at time t=0
	 * @param initialDenials      number of already denied job at time t=0
	 */
	public DenialEDSolver(FunctionInterface arrivalDistribution, FunctionInterface serviceDistribution,
			BigInteger queueSize, BigInteger maxDenials, BigInteger initialJobsInQueue, BigInteger initialDenials) {
		this(arrivalDistribution, serviceDistribution, queueSize, maxDenials,
				createProbs(queueSize, initialJobsInQueue), createProbs(maxDenials, initialDenials));
	}

	private static double[] createProbs(BigInteger maxSize, BigInteger initialJobs) {
		if (maxSize.intValue() <= 0)
			throw new IllegalArgumentException("QueueSize and maxDenials must be positive");

		if (initialJobs.intValue() < 0 || initialJobs.intValue() > maxSize.intValue())
			throw new IllegalArgumentException(
					"The number of jobs already in queue must be in range" + "[0,queueSize]");

		double[] probs = new double[maxSize.intValue() + 1];
		probs[initialJobs.intValue()] = 1.0;

		return probs;
	}

	/**
	 * Build and solve the CTMC
	 * 
	 * @param timeStep  analysis time step
	 * @param timeBound analysis end time
	 * @return
	 */
	public DenialEDSolver analyze(BigDecimal timeStep, BigInteger timeBound) {
		int timeBoundStep = (int) (timeBound.intValue() / timeStep.doubleValue()) + 1;
		double[] pCpu = new double[timeBoundStep];
		for (int t = 0; t < timeBoundStep; t++)
			pCpu[t] = 1.0;
		return (this.analyze(timeStep, timeBound, pCpu));
	}

	/**
	 * Build and solve the CTMC given the probability of cpu free at each time
	 * 
	 * @param timeStep             analysis time step
	 * @param timeBound            analysis end time
	 * @param cpuFreeProbabilities probability of cpu free at each time TODO
	 * @return the solver itself.
	 */
	public DenialEDSolver analyze(BigDecimal timeStep, BigInteger timeBound, double[] cpuFreeProbabilities) {

		int timeBoundStep = (int) (timeBound.intValue() / timeStep.doubleValue()) + 1;

		if (timeBoundStep != cpuFreeProbabilities.length)
			throw new IllegalArgumentException("CpuProbabilities.length must be equal to timeBound/timeStep +1");

		for (double val : cpuFreeProbabilities)
			if (val < 0 || val > 1.01)
				throw new IllegalArgumentException("CpuProbabilities must in the range [0,1]. Got " + val);

		for (int i = 0; i < pArrival.length; i++)
			this.pArrival[i] = taylorFirstOrderExpansion(this.arrivalLambdas[i], timeStep.doubleValue());

		for (int i = 0; i < pService.length; i++)
			this.pService[i] = taylorFirstOrderExpansion(this.serviceLambdas[i], timeStep.doubleValue());

		double pCpuFree;
		int N = queueSize.intValue();
		int K = maxDenials.intValue();
		int H = pArrival.length - 1;
		int L = pService.length - 1;

		this.extendedProbabilities = new double[timeBoundStep][N + 1][K + 1][H + 1][L + 1];
		this.exactlyKdenials = new double[timeBoundStep][K + 1];
		double[][][][] pPrevious;
		double[][][][] pCurrent = new double[N + 1][K + 1][H + 1][L + 1];

		for (int k = 0; k <= K; k++) {
			this.exactlyKdenials[0][k] = initialDenialsDistribution[k];
			for (int n = 0; n <= N; n++) {
				this.extendedProbabilities[0][n][k][0][0] = initialDistribution[n][k];
				pCurrent[n][k][0][0] = initialDistribution[n][k];
			}
		}

		if (L != 0) {

			for (int t = 1; t < timeBoundStep; t++) {
				pPrevious = pCurrent;
				pCurrent = new double[N + 1][K + 1][H + 1][L + 1];
				pCpuFree = cpuFreeProbabilities[t - 1];

				for (int k = 0; k <= K; k++) {

					pCurrent[0][k][0][0] = pPrevious[0][k][0][0] + pPrevious[0][k][0][L] * pCpuFree * pService[L]
							- pPrevious[0][k][0][0] * pArrival[0];

					pCurrent[0][k][0][1] = pPrevious[0][k][0][1] + pPrevious[1][k][0][0] * pCpuFree * pService[0]
							- pPrevious[0][k][0][1] * (pArrival[0] + pCpuFree * pService[1]);

					for (int l = 2; l <= L; l++)
						pCurrent[0][k][0][l] = pPrevious[0][k][0][l]
								+ pPrevious[0][k][0][l - 1] * pCpuFree * pService[l - 1]
								- pPrevious[0][k][0][l] * (pArrival[0] + pCpuFree * pService[l]);

					for (int h = 1; h <= H; h++)
						pCurrent[N][k][h][1] = pPrevious[N][k][h][1] + pPrevious[N][k][h - 1][1] * pArrival[h - 1]
								- pPrevious[N][k][h][1] * (pArrival[h] + pCpuFree * pService[1]);

					for (int h = 1; h <= H; h++)
						pCurrent[0][k][h][0] = pPrevious[0][k][h][0] + pPrevious[0][k][h - 1][0] * pArrival[h - 1]
								+ pPrevious[0][k][h][L] * pCpuFree * pService[L] - pPrevious[0][k][h][0] * pArrival[h];

					for (int n = 0; n < N; n++) {
						for (int h = 1; h <= H; h++) {
							pCurrent[n][k][h][1] = pPrevious[n][k][h][1] + pPrevious[n][k][h - 1][1] * pArrival[h - 1]
									+ pPrevious[n + 1][k][h][0] * pCpuFree * pService[0]
									- pPrevious[n][k][h][1] * (pArrival[h] + pCpuFree * pService[1]);
						}
					}

					for (int n = 1; n < N; n++)
						pCurrent[n][k][0][0] = pPrevious[n][k][0][0] + pPrevious[n - 1][k][H][0] * pArrival[H]
								+ pPrevious[n][k][0][L] * pCpuFree * pService[L]
								- pPrevious[n][k][0][0] * (pArrival[0] + pCpuFree * pService[0]);

					for (int n = 1; n < N; n++) {
						for (int l = 2; l <= L; l++) {
							pCurrent[n][k][0][l] = pPrevious[n][k][0][l]
									+ pPrevious[n][k][0][l - 1] * pCpuFree * pService[l - 1]
									+ pPrevious[n - 1][k][H][l] * pArrival[H]
									- pPrevious[n][k][0][l] * (pArrival[0] + pCpuFree * pService[l]);
						}
					}

					for (int n = 0; n <= N; n++) {
						for (int h = 1; h <= H; h++) {
							for (int l = 2; l <= L; l++) {
								pCurrent[n][k][h][l] = pPrevious[n][k][h][l]
										+ pPrevious[n][k][h - 1][l] * pArrival[h - 1]
										+ pPrevious[n][k][h][l - 1] * pCpuFree * pService[l - 1]
										- pPrevious[n][k][h][l] * (pArrival[h] + pCpuFree * pService[l]);
							}
						}
					}

					for (int n = 1; n <= N; n++) {
						for (int h = 1; h <= H; h++) {
							pCurrent[n][k][h][0] = pPrevious[n][k][h][0] + pPrevious[n][k][h - 1][0] * pArrival[h - 1]
									+ pPrevious[n][k][h][L] * pCpuFree * pService[L]
									- pPrevious[n][k][h][0] * (pArrival[h] + pCpuFree * pService[0]);
						}
					}

					for (int n = 1; n < N; n++)
						pCurrent[n][k][0][1] = pPrevious[n][k][0][1]
								+ pPrevious[n + 1][k][0][0] * pCpuFree * pService[0]
								+ pPrevious[n - 1][k][H][1] * pArrival[H]
								- pPrevious[n][k][0][1] * (pArrival[0] + pCpuFree * pService[1]);

					if (k == 0) {

						pCurrent[N][k][0][1] = pPrevious[N][k][0][1] + pPrevious[N - 1][k][H][1] * pArrival[H]
								- pPrevious[N][k][0][1] * (pArrival[0] + pCpuFree * pService[1]);

						pCurrent[N][k][0][0] = pPrevious[N][k][0][0] + pPrevious[N - 1][k][H][0] * pArrival[H]
								+ pPrevious[N][k][0][L] * pCpuFree * pService[L]
								- pPrevious[N][k][0][0] * (pArrival[0] + pCpuFree * pService[0]);

						for (int l = 2; l <= L; l++)
							pCurrent[N][k][0][l] = pPrevious[N][k][0][l] + pPrevious[N - 1][k][H][l] * pArrival[H]
									+ pPrevious[N][k][0][l - 1] * pCpuFree * pService[l - 1]
									- pPrevious[N][k][0][l] * (pArrival[0] + pCpuFree * pService[l]);

					} else {

						pCurrent[N][k][0][1] = pPrevious[N][k][0][1] + pPrevious[N - 1][k][H][1] * pArrival[H]
								- pPrevious[N][k][0][1] * (pArrival[0] + pCpuFree * pService[1])
								+ pPrevious[N][k - 1][H][1] * pArrival[H];

						pCurrent[N][k][0][0] = pPrevious[N][k][0][0] + pPrevious[N - 1][k][H][0] * pArrival[H]
								+ pPrevious[N][k][0][L] * pCpuFree * pService[L]
								- pPrevious[N][k][0][0] * (pArrival[0] + pCpuFree * pService[0])
								+ pPrevious[N][k - 1][H][0] * pArrival[H];

						for (int l = 2; l <= L; l++)
							pCurrent[N][k][0][l] = pPrevious[N][k][0][l] + pPrevious[N - 1][k][H][l] * pArrival[H]
									+ pPrevious[N][k][0][l - 1] * pCpuFree * pService[l - 1]
									- pPrevious[N][k][0][l] * (pArrival[0] + pCpuFree * pService[l])
									+ pPrevious[N][k - 1][H][l] * pArrival[H];

					}

				}
				for (int n = 0; n <= N; n++) {
					for (int k = 0; k <= K; k++) {
						for (int h = 0; h <= H; h++) {
							for (int l = 0; l <= L; l++) {
								if (pCurrent[n][k][h][l] < 0 || pCurrent[n][k][h][l] > 1.0) {
									System.out.println(pCurrent[n][k][h][l]);
									throw new IllegalArgumentException(
											"Negative probabilities result. " + "Please, try with a lower timeStep.");
								}
								this.extendedProbabilities[t][n][k][h][l] = pCurrent[n][k][h][l];
							}
						}
					}
				}
			}
		} else { // L==0
			for (int t = 1; t < timeBoundStep; t++) {
				pPrevious = pCurrent;
				pCurrent = new double[N + 1][K + 1][H + 1][L + 1];
				pCpuFree = cpuFreeProbabilities[t - 1];

				for (int k = 0; k <= K; k++) {

					pCurrent[0][k][0][0] = pPrevious[0][k][0][0] + pPrevious[1][k][0][L] * pCpuFree * pService[L]
							- pPrevious[0][k][0][0] * pArrival[0];

					for (int n = 1; n < N; n++)
						pCurrent[n][k][0][0] = pPrevious[n][k][0][0] + pPrevious[n - 1][k][H][0] * pArrival[H]
								+ pPrevious[n + 1][k][0][L] * pCpuFree * pService[L]
								- pPrevious[n][k][0][0] * (pArrival[0] + pCpuFree * pService[L]);

					pCurrent[N][k][0][0] = pPrevious[N][k][0][0] + pPrevious[N - 1][k][H][0] * pArrival[H]
							- pPrevious[N][k][0][0] * (pArrival[0] + pCpuFree * pService[0]);

					for (int h = 1; h <= H; h++)
						pCurrent[0][k][h][0] = pPrevious[0][k][h][0] + pPrevious[0][k][h - 1][0] * pArrival[h - 1]
								+ pPrevious[1][k][h][L] * pCpuFree * pService[L] - pPrevious[0][k][h][0] * pArrival[h];

					for (int n = 1; n < N; n++)
						for (int h = 1; h <= H; h++)
							pCurrent[n][k][h][0] = pPrevious[n][k][h][0] + pPrevious[n][k][h - 1][0] * pArrival[h - 1]
									+ pPrevious[n + 1][k][h][L] * pCpuFree * pService[L]
									- pPrevious[n][k][h][0] * (pArrival[h] + pCpuFree * pService[0]);
					for (int h = 1; h <= H; h++)
						pCurrent[N][k][h][0] = pPrevious[N][k][h][0] + pPrevious[N][k][h - 1][0] * pArrival[h - 1]
								- pPrevious[N][k][h][0] * (pArrival[h] + pCpuFree * pService[L]);

					if (k > 0)
						pCurrent[N][k][0][0] += pPrevious[N][k - 1][H][0] * pArrival[H];

				}
				for (int n = 0; n <= N; n++) {
					for (int k = 0; k <= K; k++) {
						for (int h = 0; h <= H; h++) {
							for (int l = 0; l <= L; l++) {
								if (pCurrent[n][k][h][l] < 0 || pCurrent[n][k][h][l] > 1.0) {
									System.out.println(pCurrent[n][k][h][l]);
									throw new IllegalArgumentException(
											"Negative probabilities result. " + "Please, try with a lower timeStep.");
								}
								this.extendedProbabilities[t][n][k][h][l] = pCurrent[n][k][h][l];
								this.exactlyKdenials[t][k] += pCurrent[n][k][h][l];
								if (this.exactlyKdenials[t][k] > 1.0000002 || this.exactlyKdenials[t][k] < 0.0) {
									System.out.println(
											"time: " + t + " k " + k + " esattamente: " + this.exactlyKdenials[t][k]);
									throw new IllegalArgumentException("Exactly k > 1");
								}

							}
						}
					}
				}
			}

		}
		computeAtLeastKdenials(timeBoundStep, K);
		return this;
	}

	public void debugProbs(int time) {
		int N = queueSize.intValue();
		int K = maxDenials.intValue();
		int H = pArrival.length - 1;
		int L = pService.length - 1;
		System.out.println("N:" + N + "\n K:" + K + "\n H:" + pArrival.length + "\n L:" + pService.length
				+ "\n totale stati: " + (N + 1) * (K + 1) * pArrival.length * pService.length);
		double val = 0.0;
		for (int t = 0; t < time; t++) {
			val = 0.0;
			System.out.println("TEMPO T:" + t);

			for (int n = 0; n <= N; n++) {
				for (int k = 0; k <= K; k++) {
					for (int h = 0; h <= H; h++) {
						for (int l = 0; l <= L; l++) {
							val += (extendedProbabilities[t][n][k][h][l]);
						}
					}

				}
			}
			System.out.println(Precision.round(val, 6));

		}
	}

	protected static double taylorFirstOrderExpansion(BigDecimal lambda, double value) {
		return lambda.multiply(new BigDecimal(value)).doubleValue();
	}

	private void computeAtLeastKdenials(int timeBoundStep, int K) {

		atLeastKdenials = new double[timeBoundStep][K + 1];
		double value;
		for (int t = 0; t < timeBoundStep; t++) {
			atLeastKdenials[t][0] = 1.0;
			for (int k = 1; k <= K; k++) {
				value = 0.0;
				for (int h = 0; h < k; h++)
					value += exactlyKdenials[t][h];

				atLeastKdenials[t][k] = 1.0 - value;
				if (1.0 - value > 1.0)
					throw new IllegalArgumentException();
			}
		}
	}

	public double[][] getAtLeastKdenials() {
		return atLeastKdenials;
	}

	public double[][] getExactlykDenials() {
		return exactlyKdenials;
	}

	public double[][][][][] getExtendedDenials() {
		return this.extendedProbabilities;
	}

}
