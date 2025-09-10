package github.scandiffio.task;

import github.scandiffio.function.FunctionInterface;

/**
 * 
 * A soft real-time task.
 *
 */
public class SoftRealTimeTask extends RealTimeTask {

	private int priority;

	/**
	 * Builds a soft real-time task with given priority, arrival distribution and
	 * service distribution functions.
	 * 
	 * @param arrivalDistribution probability distribution of arrival events
	 * @param serviceDistribution probability distribution of service events
	 * @param priority            the priority the task has to get the system
	 *                            resources. Higher the value, lower the priority
	 *                            level.
	 */
	public SoftRealTimeTask(FunctionInterface arrivalDistribution, FunctionInterface serviceDistribution,
			int priority) {

		super(arrivalDistribution, serviceDistribution);
		this.priority = priority;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

}
