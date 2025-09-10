package github.scandiffio.analyzer;

import github.scandiffio.task.SoftRealTimeTask;

public class TaskSetEntry {
	// TODO eh questo ricordo è nato per soddisfare "tasksetAnalyzer" e ha un po'
	// incasinato le cose perché devi dare un task con tanti parametri accessori

	private SoftRealTimeTask task;
	private SolverParametersContainer parameterContainer;

	public TaskSetEntry(SoftRealTimeTask task, SolverParametersContainer parameterContainer) {
		this.task = task;
		this.parameterContainer = parameterContainer;
	}

	public SoftRealTimeTask getTask() {
		return task;
	}

	public void setTask(SoftRealTimeTask task) {
		this.task = task;
	}

	public SolverParametersContainer getParameterContainer() {
		return parameterContainer;
	}

	public void setParameterContainer(SolverParametersContainer parameterContainer) {
		this.parameterContainer = parameterContainer;
	}

}
