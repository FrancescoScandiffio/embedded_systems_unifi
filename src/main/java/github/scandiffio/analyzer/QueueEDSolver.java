package github.scandiffio.analyzer;

import java.math.BigDecimal;
import java.math.BigInteger;

import github.scandiffio.function.FunctionInterface;

/**
 * This is an analyzer for queues, based on differential equations.
 */
public class QueueEDSolver {

	private double[][][][] pExtendedAlongTime;
	private double[][] stateProbabilitiesAlongTime;
	private double[] initialElementsDistribution;
	private double[] pArrival;
	private double[] pService;
	private BigDecimal[] arrivalLambdas;
	private BigDecimal[] serviceLambdas;
	private BigInteger queueSize;

	public QueueEDSolver(FunctionInterface arrivalDistribution, FunctionInterface serviceDistribution,
			BigInteger queueSize, double[] initialQueueDistribution) {

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

		this.queueSize = queueSize;
		this.initialElementsDistribution = initialQueueDistribution.clone();
		this.arrivalLambdas = DistributionExtender.getExtendedProbs(arrivalDistribution);
		this.serviceLambdas = DistributionExtender.getExtendedProbs(serviceDistribution);
		this.pArrival = new double[arrivalLambdas.length];
		this.pService = new double[serviceLambdas.length];

	}

	public QueueEDSolver(FunctionInterface arrivalDistribution, FunctionInterface serviceDistribution,
			BigInteger queueSize, BigInteger initialJobsInQueue) {
		this(arrivalDistribution, serviceDistribution, queueSize, createProbs(queueSize, initialJobsInQueue));
	}

	private static double[] createProbs(BigInteger queueSize, BigInteger initialJobsInQueue) {
		if (queueSize.intValue() <= 0)
			throw new IllegalArgumentException("QueueSize must be positive");

		if (initialJobsInQueue.intValue() < 0 || initialJobsInQueue.intValue() > queueSize.intValue())
			throw new IllegalArgumentException(
					"The number of jobs already in queue must be in range" + "[0,queueSize]");

		double[] probs = new double[queueSize.intValue() + 1];
		probs[initialJobsInQueue.intValue()] = 1.0;

		return probs;
	}

	public QueueEDSolver analyze(BigDecimal timeStep, BigInteger timeBound) {
		int timeBoundStep = (int) (timeBound.intValue() / timeStep.doubleValue()) + 1;
		double[] pCpu = new double[timeBoundStep];
		for (int t = 0; t < timeBoundStep; t++)
			pCpu[t] = 1.0;
		return (this.analyze(timeStep, timeBound, pCpu));
	}

	public QueueEDSolver analyze(BigDecimal timeStep, BigInteger timeBound, double[] cpuFreeProbs) {

		int timeBoundStep = (int) (timeBound.intValue() / timeStep.doubleValue()) + 1;

		if (timeBoundStep != cpuFreeProbs.length)
			throw new IllegalArgumentException("CpuProbabilities.length must be equal to timeBound/timeStep +1");

		for (double val : cpuFreeProbs)
			if (val < 0 || val > 1.01)
				throw new IllegalArgumentException("CpuProbabilities must in the range [0,1]. Got " + val);

		for (int i = 0; i < pArrival.length; i++)
			this.pArrival[i] = taylorFirstOrderExpansion(this.arrivalLambdas[i], timeStep.doubleValue());

		for (int i = 0; i < pService.length; i++)
			this.pService[i] = taylorFirstOrderExpansion(this.serviceLambdas[i], timeStep.doubleValue());

		int N = queueSize.intValue();
		int H = pArrival.length - 1;
		int L = pService.length - 1;
		this.stateProbabilitiesAlongTime = new double[timeBoundStep][N + 1];
		this.pExtendedAlongTime = new double[timeBoundStep][N + 1][H + 1][L + 1];
		double[][][] pPrevious;
		double[][][] pCurrent = new double[N + 1][pArrival.length][pService.length];

		for (int i = 0; i < initialElementsDistribution.length; i++) {
			this.pExtendedAlongTime[0][i][0][0] = initialElementsDistribution[i];
			this.stateProbabilitiesAlongTime[0][i] = initialElementsDistribution[i];
			pCurrent[i][0][0] = initialElementsDistribution[i];

		}

		double pCpuFree;

		for (int t = 1; t < timeBoundStep; t++) {
			pPrevious = pCurrent;
			pCurrent = new double[N + 1][pArrival.length][pService.length];
			pCpuFree = cpuFreeProbs[t - 1];

			if (L == 0) { // servizio con una sola transizione
				pCurrent[0][0][0] = pPrevious[0][0][0] + pPrevious[1][0][0] * pCpuFree * pService[0]
						- pPrevious[0][0][0] * pArrival[0];

				for (int h = 1; h <= H; h++)
					pCurrent[0][h][0] = pPrevious[0][h][0] + pPrevious[1][h][0] * pCpuFree * pService[0]
							+ pPrevious[0][h - 1][0] * pArrival[h - 1] - pPrevious[0][h][0] * pArrival[h];

				for (int n = 1; n < N; n++)
					for (int h = 1; h <= H; h++)
						pCurrent[n][h][0] = pPrevious[n][h][0] + pPrevious[n][h - 1][0] * pArrival[h - 1]
								+ pPrevious[n + 1][h][0] * pCpuFree * pService[0]
								- pPrevious[n][h][0] * (pArrival[h] + pCpuFree * pService[0]);

				for (int n = 1; n < N; n++)
					pCurrent[n][0][0] = pPrevious[n][0][0] + pPrevious[n - 1][H][0] * pArrival[H]
							+ pPrevious[n + 1][0][0] * pCpuFree * pService[0]
							- pPrevious[n][0][0] * (pArrival[0] + pCpuFree * pService[0]);

				pCurrent[N][0][0] = pPrevious[N][0][0] + pPrevious[N - 1][H][0] * pArrival[H]
						+ pPrevious[N][H][0] * pArrival[H]
						- pPrevious[N][0][0] * (pArrival[0] + pCpuFree * pService[0]);

				for (int h = 1; h <= H; h++)
					pCurrent[N][h][0] = pPrevious[N][h][0] + pPrevious[N][h - 1][0] * pArrival[h - 1]
							- pPrevious[N][h][0] * (pArrival[h] + pCpuFree * pService[0]);

			} else { // servizio in almeno 2 fasi

				pCurrent[0][0][0] = pPrevious[0][0][0] + pPrevious[0][0][L] * pCpuFree * pService[L]
						- pPrevious[0][0][0] * pArrival[0];

				pCurrent[0][0][1] = pPrevious[0][0][1] + pPrevious[1][0][0] * pCpuFree * pService[0]
						- pPrevious[0][0][1] * (pArrival[0] + pCpuFree * pService[1]);

				for (int l = 2; l <= L; l++)
					pCurrent[0][0][l] = pPrevious[0][0][l] + pPrevious[0][0][l - 1] * pCpuFree * pService[l - 1]
							- pPrevious[0][0][l] * (pArrival[0] + pCpuFree * pService[l]);

				for (int h = 1; h <= H; h++)
					pCurrent[N][h][1] = pPrevious[N][h][1] + pPrevious[N][h - 1][1] * pArrival[h - 1]
							- pPrevious[N][h][1] * (pArrival[h] + pCpuFree * pService[1]);

				for (int h = 1; h <= H; h++)
					pCurrent[0][h][0] = pPrevious[0][h][0] + pPrevious[0][h - 1][0] * pArrival[h - 1]
							+ pPrevious[0][h][L] * pCpuFree * pService[L] - pPrevious[0][h][0] * pArrival[h];

				for (int n = 0; n < N; n++) {
					for (int h = 1; h <= H; h++) {
						pCurrent[n][h][1] = pPrevious[n][h][1] + pPrevious[n][h - 1][1] * pArrival[h - 1]
								+ pPrevious[n + 1][h][0] * pCpuFree * pService[0]
								- pPrevious[n][h][1] * (pArrival[h] + pCpuFree * pService[1]);
					}
				}

				for (int n = 1; n < N; n++)
					pCurrent[n][0][0] = pPrevious[n][0][0] + pPrevious[n - 1][H][0] * pArrival[H]
							+ pPrevious[n][0][L] * pCpuFree * pService[L]
							- pPrevious[n][0][0] * (pArrival[0] + pCpuFree * pService[0]);

				for (int n = 1; n < N; n++) {
					for (int l = 2; l <= L; l++) {
						pCurrent[n][0][l] = pPrevious[n][0][l] + pPrevious[n][0][l - 1] * pCpuFree * pService[l - 1]
								+ pPrevious[n - 1][H][l] * pArrival[H]
								- pPrevious[n][0][l] * (pArrival[0] + pCpuFree * pService[l]);
					}
				}

				pCurrent[N][0][1] = pPrevious[N][0][1] + pPrevious[N - 1][H][1] * pArrival[H]
						+ pPrevious[N][H][1] * pArrival[H]
						- pPrevious[N][0][1] * (pArrival[0] + pCpuFree * pService[1]);

				pCurrent[N][0][0] = pPrevious[N][0][0] + pPrevious[N][H][0] * pArrival[H]
						+ pPrevious[N - 1][H][0] * pArrival[H] + pPrevious[N][0][L] * pCpuFree * pService[L]
						- pPrevious[N][0][0] * (pArrival[0] + pCpuFree * pService[0]);

				for (int l = 2; l <= L; l++)
					pCurrent[N][0][l] = pPrevious[N][0][l] + pPrevious[N][H][l] * pArrival[H]
							+ pPrevious[N - 1][H][l] * pArrival[H] + pPrevious[N][0][l - 1] * pCpuFree * pService[l - 1]
							- pPrevious[N][0][l] * (pArrival[0] + pCpuFree * pService[l]);

				for (int n = 0; n <= N; n++) {
					for (int h = 1; h <= H; h++) {
						for (int l = 2; l <= L; l++) {
							pCurrent[n][h][l] = pPrevious[n][h][l] + pPrevious[n][h - 1][l] * pArrival[h - 1]
									+ pPrevious[n][h][l - 1] * pCpuFree * pService[l - 1]
									- pPrevious[n][h][l] * (pArrival[h] + pCpuFree * pService[l]);
						}
					}
				}

				for (int n = 1; n <= N; n++) {
					for (int h = 1; h <= H; h++) {
						pCurrent[n][h][0] = pPrevious[n][h][0] + pPrevious[n][h - 1][0] * pArrival[h - 1]
								+ pPrevious[n][h][L] * pCpuFree * pService[L]
								- pPrevious[n][h][0] * (pArrival[h] + pCpuFree * pService[0]);
					}
				}

				for (int n = 1; n < N; n++)
					pCurrent[n][0][1] = pPrevious[n][0][1] + pPrevious[n + 1][0][0] * pCpuFree * pService[0]
							+ pPrevious[n - 1][H][1] * pArrival[H]
							- pPrevious[n][0][1] * (pArrival[0] + pCpuFree * pService[1]);

			}

			for (int n = 0; n <= N; n++) {
				for (int h = 0; h <= H; h++) {
					for (int l = 0; l <= L; l++) {
						if (pCurrent[n][h][l] < 0) {
							System.out.println(pCurrent[n][h][l]);
							throw new IllegalArgumentException(
									"Negative probabilities result. " + "Please, try with a lower timeStep.");
						}

						this.pExtendedAlongTime[t][n][h][l] = pCurrent[n][h][l];
						this.stateProbabilitiesAlongTime[t][n] += pCurrent[n][h][l];
						if (this.stateProbabilitiesAlongTime[t][n] > 1.0000002
								|| this.stateProbabilitiesAlongTime[t][n] < 0.0) {
							System.out.println("time: " + t + " k " + n + " esattamente: "
									+ this.stateProbabilitiesAlongTime[t][n]);
							throw new IllegalArgumentException("queue state error");
						}
					}
				}

			}
		}

		return this;
	}

	protected static double taylorFirstOrderExpansion(BigDecimal lambda, double value) {
		return lambda.multiply(new BigDecimal(value)).doubleValue();
	}

	public void debugProbs(int time) {
		int N = queueSize.intValue();
		int H = pArrival.length - 1;
		int L = pService.length - 1;
		System.out.println("N:" + N + "\n H:" + pArrival.length + "\n L:" + pService.length + "\n totale stati: "
				+ (N + 1) * pArrival.length * pService.length);
		double[][] debug = new double[time][N + 1];
		double val = 0.0;
		double sum = 0.0;
		for (int t = 0; t < time; t++) {
			for (int n = 0; n <= N; n++) {
				val = 0.0;
				for (int h = 0; h <= H; h++) {
					for (int l = 0; l <= L; l++) {
						val += pExtendedAlongTime[t][n][h][l];
					}
				}
				debug[t][n] = val;
			}
		}

		for (int t = 0; t < time; t++) {
			System.out.println("TEMPO T:" + t);
			sum = 0.0;
			for (int n = 0; n <= N; n++)
				sum += debug[t][n];
			System.out.println(sum);

		}

	}

	public double[][] getStateProbabilitiesAlongTime() {
		return stateProbabilitiesAlongTime;
	}

	private double getServiceFunction(int index) {
		return 1;
	}

	private double getArrivalFunction(int index) {
		return 1;
	}

	public double[][][][] getpExtendedAlongTime() {
		return pExtendedAlongTime;
	}

	public double[] getInitialElementsDistribution() {
		return initialElementsDistribution;
	}

	public BigInteger getSize() {
		return queueSize;
	}

}
