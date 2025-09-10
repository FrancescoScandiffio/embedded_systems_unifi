package github.scandiffio.analyzer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import org.oristool.models.gspn.chains.DTMCStationary;
import org.oristool.models.gspn.chains.DTMCStationary.Builder;

import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;

/**
 * This analyzer compute the steady state distribution of the length of the
 * queue at the beginning of each period in a analytic way.
 */
public class DTMCSteadyStateAnalyzer extends SteadyStateAnalyzer {

	private class QueueState {
	}

	@Override
	public double[] getSteadyStateDistribution(QueueEDSolver analyzer, BigDecimal timeStep, BigInteger hyperPeriod) {
		double[][] pkjMatrix = this.getPkjMatrix(analyzer, timeStep, hyperPeriod);

		Map<Integer, QueueState> states = new HashMap<Integer, QueueState>();

		MutableValueGraph<QueueState, Double> mvg = ValueGraphBuilder.directed().allowsSelfLoops(true).build();
		for (int i = 0; i < analyzer.getSize().intValue() + 1; i++) {
			QueueState queueState = new QueueState();
			states.put(i, queueState);
			mvg.addNode(queueState);
		}
		for (int k = 0; k < analyzer.getSize().intValue() + 1; k++) {
			for (int j = 0; j < analyzer.getSize().intValue() + 1; j++) {
				mvg.putEdgeValue(states.get(k), states.get(j), pkjMatrix[k][j]);
			}
		}
		Builder<QueueState> dtmcStBuilder = DTMCStationary.builder();
		DTMCStationary<QueueState> dtmcSt = dtmcStBuilder.build();
		Map<QueueState, Double> steadyStateMap = dtmcSt.apply(mvg);

		double[] steadyState = new double[analyzer.getSize().intValue() + 1];

		for (int i = 0; i < analyzer.getSize().intValue() + 1; i++) {
			steadyState[i] = steadyStateMap.get(states.get(i));
		}

		return steadyState;
	}

}
