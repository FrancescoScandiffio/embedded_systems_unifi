package github.scandiffio.task;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import github.scandiffio.function.FunctionInterface;

public class RealTimeTask {

	private static AtomicInteger globalId = new AtomicInteger(0);

	private int id;
	private FunctionInterface arrivalDistribution;
	private FunctionInterface serviceDistribution;

	/**
	 * @param arrivalDistribution probability distribution of arrival events
	 * @param serviceDistribution probability distribution of service events
	 */
	public RealTimeTask(FunctionInterface arrivalDistribution, FunctionInterface serviceDistribution) {
		this.id = globalId.getAndAdd(1);
		this.arrivalDistribution = arrivalDistribution;
		this.serviceDistribution = serviceDistribution;
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @return the arrivalDistribution
	 */
	public FunctionInterface getArrivalDistribution() {
		return arrivalDistribution;
	}

	/**
	 * @param arrivalDistribution the arrivalDistribution to set
	 */
	public void setArrivalDistribution(FunctionInterface arrivalDistribution) {
		this.arrivalDistribution = arrivalDistribution;
	}

	/**
	 * @return the serviceDistribution
	 */
	public FunctionInterface getServiceDistribution() {
		return serviceDistribution;
	}

	/**
	 * @param serviceDistribution the serviceDistribution to set
	 */
	public void setServiceDistribution(FunctionInterface serviceDistribution) {
		this.serviceDistribution = serviceDistribution;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}

		SoftRealTimeTask task = (SoftRealTimeTask) obj;
		return task.getId() == this.getId();
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

}
