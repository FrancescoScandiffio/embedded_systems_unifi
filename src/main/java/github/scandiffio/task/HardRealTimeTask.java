package github.scandiffio.task;

import github.scandiffio.function.FunctionInterface;

public class HardRealTimeTask extends RealTimeTask {

	private double period;
	private double jitter;
	private double offset;
	private double deadline;

	/**
	 * @param arrivalDistribution probability distribution of arrival events
	 * @param serviceDistribution probability distribution of service events
	 * @param period
	 * @param jitter
	 * @param offset
	 * @param deadline
	 */
	public HardRealTimeTask(FunctionInterface arrivalDistribution, FunctionInterface serviceDistribution, double period,
			double jitter, double offset, double deadline) {
		super(arrivalDistribution, serviceDistribution);
		this.period = period;
		this.jitter = jitter;
		this.offset = offset;
		this.deadline = deadline;
	}

	/**
	 * @return the period
	 */
	public double getPeriod() {
		return period;
	}

	/**
	 * @param period the period to set
	 */
	public void setPeriod(double period) {
		this.period = period;
	}

	/**
	 * @return the jitter
	 */
	public double getJitter() {
		return jitter;
	}

	/**
	 * @param jitter the jitter to set
	 */
	public void setJitter(double jitter) {
		this.jitter = jitter;
	}

	/**
	 * @return the offset
	 */
	public double getOffset() {
		return offset;
	}

	/**
	 * @param offset the offset to set
	 */
	public void setOffset(double offset) {
		this.offset = offset;
	}

	/**
	 * @return the deadline
	 */
	public double getDeadline() {
		return deadline;
	}

	/**
	 * @param deadline the deadline to set
	 */
	public void setDeadline(double deadline) {
		this.deadline = deadline;
	}

}
