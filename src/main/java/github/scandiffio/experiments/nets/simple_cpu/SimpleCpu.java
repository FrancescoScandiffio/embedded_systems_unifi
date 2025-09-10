package github.scandiffio.experiments.nets.simple_cpu;

import java.math.BigDecimal;

import org.oristool.models.pn.Priority;
import org.oristool.models.stpn.MarkingExpr;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Place;
import org.oristool.petrinet.Transition;

public class SimpleCpu {
	public static void build(PetriNet net, Marking marking) {

		// Generating Nodes
		Place arrival = net.addPlace("arrival");
		Place cpu = net.addPlace("cpu");
		Place ready = net.addPlace("ready");
		Transition t0 = net.addTransition("t0");
		Transition t1 = net.addTransition("t1");
		Transition t2 = net.addTransition("t2");

		// Generating Connectors
		net.addPostcondition(t2, cpu);
		net.addPostcondition(t0, arrival);
		net.addPrecondition(arrival, t1);
		net.addPostcondition(t1, ready);
		net.addPrecondition(cpu, t1);
		net.addPrecondition(ready, t2);

		// Generating Properties
		marking.setTokens(arrival, 0);
		marking.setTokens(cpu, 1);

		marking.setTokens(ready, 0);
		t0.addFeature(
				StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("5"), MarkingExpr.from("1", net)));
		t0.addFeature(new Priority(0));
		t1.addFeature(
				StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("1", net)));
		t1.addFeature(new Priority(0));
		t2.addFeature(StochasticTransitionFeature.newUniformInstance(new BigDecimal("1"), new BigDecimal("3")));
	}
}
