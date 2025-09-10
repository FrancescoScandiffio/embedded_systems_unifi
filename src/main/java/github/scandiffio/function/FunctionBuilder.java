package github.scandiffio.function;

import java.math.BigDecimal;

import org.oristool.math.expression.Variable;

public class FunctionBuilder {
	// TODO renderlo un vero builder con precisione indicata dall'utente ed uso di
	// BigDecimal al posto di double

	private FunctionBuilder() throws IllegalAccessException {
		throw new IllegalAccessException("It is not allowed to instantiate objects of class FunctionBuilder");
	}

	public static FunctionInterface createFunction(double variationCoeff, double mu) {
		if (variationCoeff <= 0.95) {// erlang + exp
			int shape = (int) (Math.ceil(1 / (variationCoeff * variationCoeff)) - 1);
			BigDecimal erlangLambda = BigDecimal.valueOf((-(mu * shape) + Math.sqrt(mu * mu * shape * shape
					- ((-shape - shape * shape) * (variationCoeff * variationCoeff * mu * mu - mu * mu))))
					/ (variationCoeff * variationCoeff * mu * mu - mu * mu));

			BigDecimal expLambda = BigDecimal.valueOf(1 / (mu - (shape / erlangLambda.doubleValue())));

			return new ErlangEXP(new Variable("x"), shape, erlangLambda, expLambda);
		} else

		if (0.95 <= variationCoeff && variationCoeff <= 1.05) { // cv ~ 1. exp
			return new EXP(new Variable("x"), BigDecimal.valueOf(1 / mu));
		}

		else { // CV > 1. hyperexp
			BigDecimal p_1 = new BigDecimal(0.5
					* (1 - Math.sqrt((variationCoeff * variationCoeff - 1) / (variationCoeff * variationCoeff + 1))));
			BigDecimal p_2 = BigDecimal.ONE.subtract(p_1);
			BigDecimal lambda_1 = BigDecimal.valueOf(p_1.doubleValue() * 2 / variationCoeff);
			BigDecimal lambda_2 = BigDecimal.valueOf(p_2.doubleValue() * 2 / variationCoeff);

			BigDecimal[] rates = { lambda_1, lambda_2 };
			BigDecimal[] probabilities = { p_1, p_2 };

			return new HyperEXP(new Variable("x"), rates, probabilities);

		}
	}

}
