package github.scandiffio.function;

import java.math.BigDecimal;

import org.oristool.math.OmegaBigDecimal;
import org.oristool.math.domain.DBMZone;
import org.oristool.math.expression.Exmonomial;
import org.oristool.math.expression.Expolynomial;
import org.oristool.math.expression.ExponentialTerm;
import org.oristool.math.expression.Variable;

/**
 * The Exponential PDF.
 */
public class EXP implements FunctionInterface {

	private DBMZone domain;
	private Expolynomial density;

	/**
	 * Builds the function {@code e^(-lambda x)} over {@code [0, +infty)}.
	 *
	 * @param x      PDF variable
	 * @param lambda rate (before the negation)
	 */
	public EXP(Variable x, BigDecimal lambda) {

		OmegaBigDecimal eft;
		OmegaBigDecimal lft;
		if (lambda.compareTo(BigDecimal.ZERO) > 0) {
			eft = OmegaBigDecimal.ZERO;
			lft = OmegaBigDecimal.POSITIVE_INFINITY;
		} else if (lambda.compareTo(BigDecimal.ZERO) < 0) {
			eft = OmegaBigDecimal.NEGATIVE_INFINITY;
			lft = OmegaBigDecimal.ZERO;
		} else
			throw new IllegalArgumentException("The lambda rate must different than zero");

		domain = new DBMZone(x);
		domain.setCoefficient(x, Variable.TSTAR, lft);
		domain.setCoefficient(Variable.TSTAR, x, eft.negate());

		density = new Expolynomial();
		Exmonomial exmon = new Exmonomial(new OmegaBigDecimal(lambda).abs());
		exmon.addAtomicTerm(new ExponentialTerm(x, lambda));
		density.addExmonomial(exmon);
	}

	public EXP(EXP e) {
		this(e.getVariable(), e.getLambda());
	}

	public BigDecimal getLambda() {
		return density.getExmonomials().get(0).getConstantTerm().bigDecimalValue();
	}

	/**
	 * Returns the variable of this PDF.
	 *
	 * @return variable of this PDF
	 */
	public Variable getVariable() {

		for (Variable v : domain.getVariables())
			if (!v.equals(Variable.TSTAR))
				return v;

		return null;
	}

	@Override
	public boolean equals(Object obj) {

		if (this == obj)
			return true;

		if (!(obj instanceof EXP))
			return false;

		EXP other = (EXP) obj;

		return this.getVariable().equals(other.getVariable()) && this.getLambda().compareTo(other.getLambda()) == 0;
	}

	@Override
	public int hashCode() {

		int result = 17;

		result = 31 * result + this.getLambda().hashCode();
		result = 31 * result + this.getVariable().hashCode();

		return result;
	}

}
