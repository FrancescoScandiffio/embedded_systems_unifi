package github.scandiffio.function;

import java.math.BigDecimal;

import org.oristool.math.expression.Variable;

public class ErlangEXP implements FunctionInterface {

	private final Variable var;
	private final int erlangShape;
	private BigDecimal erlangLambda;
	private BigDecimal expLambda;

	public ErlangEXP(Variable x, int erlangShape, BigDecimal erlangLambda, BigDecimal expLambda) {
		this.var = x;
		this.erlangLambda = erlangLambda;
		this.expLambda = expLambda;
		this.erlangShape = erlangShape;
	}

	public int getErlangShape() {
		return erlangShape;
	}

	public BigDecimal getErlangLambda() {
		return erlangLambda;
	}

	public BigDecimal getExponentialLambda() {
		return expLambda;
	}

	public Variable getVariable() {
		return var;
	}
}
