package github.scandiffio.function;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.oristool.math.OmegaBigDecimal;
import org.oristool.math.domain.DBMZone;
import org.oristool.math.expression.Exmonomial;
import org.oristool.math.expression.Expolynomial;
import org.oristool.math.expression.ExponentialTerm;
import org.oristool.math.expression.Variable;

/**
 * The HyperExp PDF.
 */
public class HyperEXP implements FunctionInterface {

	private final Variable var;
	private final BigDecimal[] rates;
	private final BigDecimal[] arcProbabilities;
	private DBMZone domain;
	private Expolynomial density;

	/**
	 * z Builds an hyperExp with probabilities[i] as arc weight for rates[i]
	 *
	 * @param x     variable
	 * @param p     probabilities where p_i refers to rates_i
	 * @param rates rates
	 */
	public HyperEXP(Variable x, BigDecimal[] rates, BigDecimal[] arcProbabilities) {
		this.domain = hyperDomain(x);
		this.density = hyperDensity(x, rates, arcProbabilities);
		this.var = x;
		this.rates = rates;
		this.arcProbabilities = arcProbabilities;

	}

	/**
	 * Builds an hyperExp with auto arc weights
	 *
	 * @param x     variable
	 * @param rates rates
	 */

	public HyperEXP(Variable x, BigDecimal[] rates) {
		this(x, rates, buildProbabilities(rates));
	}

	private static DBMZone hyperDomain(Variable x) {
		DBMZone domain = new DBMZone(x);
		domain.setCoefficient(x, Variable.TSTAR, OmegaBigDecimal.POSITIVE_INFINITY);
		domain.setCoefficient(Variable.TSTAR, x, OmegaBigDecimal.ZERO);
		return domain;
	}

	private static BigDecimal[] buildProbabilities(BigDecimal[] rates) {
		if (rates.length <= 0)
			throw new IllegalArgumentException("rates[] length must be greater than zero");
		for (int i = 0; i < rates.length; i++) {
			if (rates[i].compareTo(BigDecimal.ZERO) <= 0)
				throw new IllegalArgumentException("All rates rate must be greater than zero");
		}

		BigDecimal[] weights = new BigDecimal[rates.length];
		BigDecimal sum = BigDecimal.ZERO;

		for (int i = 0; i < rates.length; i++)
			sum = sum.add(rates[i]);
		for (int i = 0; i < rates.length; i++)
			weights[i] = (rates[i].divide(sum, 4, RoundingMode.HALF_UP));

		return weights;
	}

	private static Expolynomial hyperDensity(Variable x, BigDecimal[] rates, BigDecimal[] probabilities) {
		if (rates.length != probabilities.length || rates.length <= 0 || probabilities.length <= 0)
			throw new IllegalArgumentException("The rates[] length and probabilities[] length must be equal."
					+ " They also must be greater than zero");
		double pTotal = 0.0;
		for (int i = 0; i < rates.length; i++) {
			if (rates[i].compareTo(BigDecimal.ZERO) <= 0)
				throw new IllegalArgumentException("All rates rate must be greater than zero");
			if (probabilities[i].compareTo(BigDecimal.ZERO) < 0)
				throw new IllegalArgumentException("All p_i must be >= 0");
			pTotal += probabilities[i].doubleValue();
		}
		if (pTotal > 1.0001 || pTotal <= 0.89)
			throw new IllegalArgumentException("The sum of all probabilities must be 1, got " + pTotal);

		Expolynomial expol = new Expolynomial();
		Exmonomial exmon;

		for (int i = 0; i < rates.length; i++) {
			exmon = new Exmonomial(probabilities[i].multiply(rates[i]));
			exmon.addAtomicTerm(new ExponentialTerm(Variable.X, rates[i]));
			expol.addExmonomial(exmon); // p_i * pdf di Y_i, dove Y sono tutte EXP
		}

		return expol; // sum(p_i * Y_i), i= 0...n
	}

	public HyperEXP(HyperEXP hyperExp) {
		this(hyperExp.getVariable(), hyperExp.getProbabilities(), hyperExp.getRates());
	}

	public BigDecimal[] getProbabilities() {
		return arcProbabilities;
	}

	public BigDecimal[] getRates() {
		return rates;
	}

	public Variable getVariable() {
		return var;
	}
}
