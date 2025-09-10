package github.scandiffio.analyzer;

import java.math.BigDecimal;

import github.scandiffio.function.EXP;
import github.scandiffio.function.ErlangEXP;
import github.scandiffio.function.FunctionInterface;
import github.scandiffio.function.HyperEXP;

public class DistributionExtender {

	private DistributionExtender() {
		throw new UnsupportedOperationException("It is not allowed to instantiate objects of this class");
	}

	/**
	 * Extracts the lambda of the exponentials that form the given distribution
	 * 
	 * @param function distribution to be expanded
	 * @return list of lambda of the exponentials in the distribution
	 */

	public static BigDecimal[] getExtendedProbs(FunctionInterface function) {
		BigDecimal[] probabilities;

		if (function instanceof EXP) {
			probabilities = new BigDecimal[1];
			probabilities[0] = ((EXP) function).getLambda();
		} else {
			if (function instanceof HyperEXP) {
				HyperEXP hyperExp = ((HyperEXP) function);
				probabilities = new BigDecimal[1];
				BigDecimal sum = BigDecimal.ZERO;
				BigDecimal[] rates = hyperExp.getRates();
				BigDecimal[] probs = hyperExp.getProbabilities();

				for (int i = 0; i < rates.length; i++)
					sum = sum.add(rates[i].multiply(probs[i]));
				probabilities[0] = sum;
			} else {
				if (function instanceof ErlangEXP) {
					ErlangEXP hypoExp = ((ErlangEXP) function);
					probabilities = new BigDecimal[hypoExp.getErlangShape() + 1];
					BigDecimal lambda1 = hypoExp.getErlangLambda();
					for (int i = 0; i < probabilities.length - 1; i++)
						probabilities[i] = lambda1;
					probabilities[probabilities.length - 1] = hypoExp.getExponentialLambda();
				} else
					throw new IllegalArgumentException(
							"Only function of type EXP, HyperExp and HypoExp are allowed for this EDSolver");
			}
		}
		return probabilities;
	}

}