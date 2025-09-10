package github.scandiffio.analyzer;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * This class provides a common interface for steady state analyzers.
 */

public abstract class SteadyStateAnalyzer {

	/**
	 * It compute and returns the steady state distribution of the length of the
	 * queue at the beginning of each period.
	 * 
	 * @param carFlow  the carFlow of which compute the steady state
	 * @param analyzer the queue analyzer that should be used
	 * @param timeStep the temporal resolution of the transient analysis of the
	 *                 first period (needed for the steady state analysis)
	 * @return the steady state distribution of the length of the queue at the
	 *         beginning of each period
	 */
	public abstract double[] getSteadyStateDistribution(QueueEDSolver analyzer, BigDecimal timeStep,
			BigInteger timeBound);

	protected double[][] getPkjMatrix(QueueEDSolver analyzer, BigDecimal timeStep, BigInteger hyperPeriod) {
		int hyperPeriodStep = new BigDecimal(hyperPeriod).divide(timeStep).intValue() + 1;

		double[][] pkjMatrix = new double[analyzer.getSize().intValue() + 1][analyzer.getSize().intValue() + 1];
		for (int k = 0; k <= analyzer.getSize().intValue(); k++) {
			double[][] stateProbabilitiesAlongFirstPeriod = analyzer.analyze(timeStep, hyperPeriod)
					.getStateProbabilitiesAlongTime();
			for (int j = 0; j <= analyzer.getSize().intValue(); j++) {
				pkjMatrix[k][j] = stateProbabilitiesAlongFirstPeriod[hyperPeriodStep - 1][j];
			}
		}

		return pkjMatrix;
	}

}
