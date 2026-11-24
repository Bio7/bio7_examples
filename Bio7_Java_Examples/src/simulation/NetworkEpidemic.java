package simulation;

import java.util.ArrayList;

import com.eco.bio7.simulation.Action;
import com.eco.bio7.simulation.Agent;
import com.eco.bio7.simulation.AgentModel;
import com.eco.bio7.simulation.Param;
import com.eco.bio7.simulation.SpatialIndex;

/**
 * An epidemic (SIR) on a network of contacts. An infected person (red) infects
 * each susceptible contact (blue) with a probability per step and recovers
 * (grey) with another probability. The contacts are the links of a network
 * (drawn as lines; a link which passed on the infection turns red):
 * <ul>
 * <li>"Moving contacts": the people walk; in each step all pairs closer than
 * the contact distance are linked (a SpatialIndex: fast for thousands of
 * people),</li>
 * <li>"Small world", "Scale-free" and "Random": a fixed network
 * (linkSmallWorld, linkScaleFree, linkRandom), arranged with a spring layout
 * (layoutSpring).</li>
 * </ul>
 * Charts: the susceptible, infected and recovered people and the number of
 * contacts. The simulation stops when no one is infected (stop()). The model
 * records the share of the infected people (record) for experiments: the button "Experiment..." of the settings (e.g., the infection
 * 0.02:0.1:0.02 with 5 repetitions) or the class Experiment.
 * <p>
 * A click on the floor of the 3D view (also in the HTML export) infects the
 * nearest person (the method click).
 */
public class NetworkEpidemic extends AgentModel<NetworkEpidemic.Person> {

	@Param(label = "People", min = 50, max = 3000, restart = true)
	int people = 400;

	@Param(label = "Network", options = { "Moving contacts", "Small world", "Scale-free", "Random" }, restart = true)
	String network = "Moving contacts";

	@Param(label = "Contacts of a person (fixed networks)", min = 2, max = 20, restart = true)
	int contacts = 6;

	@Param(label = "Contact distance (moving)", min = 1, max = 15, step = 0.5, group = "Moving")
	double contactDistance = 4;

	@Param(label = "Speed of the people", min = 0, max = 3, step = 0.1, group = "Moving")
	double speed = 0.8;

	@Param(label = "Infection per contact and step", min = 0, max = 0.5, step = 0.005, group = "Disease")
	double infection = 0.05;

	@Param(label = "Recovery per step", min = 0, max = 0.3, step = 0.005, group = "Disease")
	double recovery = 0.04;

	@Param(label = "Infected at the start", min = 1, max = 30, restart = true, group = "Disease")
	int initial = 3;

	/** A person: the state (S, I or R) and the direction of the walk. */
	static class Person extends Agent {
		char state = 'S';
		double angle;

		Person(double x, double y, double angle) {
			super(x, y, 2);
			this.angle = angle;
		}
	}

	int peak;

	public void init() {
		box(100, 100, 4);
		floor = "grid";
		agentRadius = 0.9;
		for (int i = 0; i < people; i++) {
			agents.add(new Person(random(0, 100), random(0, 100), random(0, 360)));
		}
		if (network.equals("Small world")) {
			linkSmallWorld(contacts, 0.1);
		} else if (network.equals("Scale-free")) {
			linkScaleFree(Math.max(1, contacts / 2));
		} else if (network.equals("Random")) {
			linkRandom(contacts / (double) Math.max(1, people - 1));
		}
		if (!network.equals("Moving contacts")) {
			layoutCircle();
			layoutSpring(80);
		}
		for (int k = 0; k < Math.min(people, initial); k++) {
			agents.get(randomInt(agents.size())).state = 'I';
		}
		peak = 0;
		show();
	}

	public void step() {
		if (network.equals("Moving contacts")) {
			/* The people walk (a torus), the contacts of this step: all pairs within the distance */
			for (Person p : agents) {
				p.angle += random(-30, 30);
				p.x += speed * cosDeg(p.angle);
				p.y += speed * sinDeg(p.angle);
				wrapInBox(p);
			}
			clearLinks();
			SpatialIndex<Person> index = index(contactDistance);
			index.forEachPair(contactDistance, (a, b) -> link(a, b));
		}
		/* The infections along the links (all decide first, then all change) */
		ArrayList<Person> infected = new ArrayList<>(), recovered = new ArrayList<>();
		for (Person p : agents) {
			if (p.state == 'I') {
				for (Person o : linkNeighbors(p)) {
					if (o.state == 'S' && chance(infection)) {
						infected.add(o);
						setLinkColor(p, o, "#e53935");
					}
				}
			}
		}
		for (Person p : agents) {
			if (p.state == 'I' && chance(recovery)) {
				recovered.add(p);
			}
		}
		for (Person p : infected) {
			p.state = 'I';
		}
		for (Person p : recovered) {
			p.state = 'R';
		}
		show();
		/* The epidemic is over: the simulation stops (an experiment ends the run) */
		int infectedNow = 0;
		for (Person p : agents) {
			if (p.state == 'I') {
				infectedNow++;
			}
		}
		if (infectedNow == 0) {
			stop();
		}
	}

	void show() {
		int s = 0, i = 0, r = 0;
		for (Person p : agents) {
			if (p.state == 'S') {
				s++;
				p.color = "#2f6fed";
			} else if (p.state == 'I') {
				i++;
				p.color = "#e53935";
			} else {
				r++;
				p.color = "#9aa0a6";
			}
		}
		peak = Math.max(peak, i);
		chart("Susceptible", s);
		chart("Infected", i);
		chart("Recovered", r);
		chart("Network", "Contacts", linkCount());
		/* For experiments: the share of the people who were infected and the peak */
		record("Infected share", (i + r) / (double) Math.max(1, agents.size()));
		record("Peak share", peak / (double) Math.max(1, agents.size()));
	}

	/** A click on the floor of the view: the nearest person (within 8) is infected. */
	public void click(double x, double y) {
		Person near = null;
		double best = 64;
		for (Person p : agents) {
			double d = (p.x - x) * (p.x - x) + (p.y - y) * (p.y - y);
			if (d < best) {
				best = d;
				near = p;
			}
		}
		if (near != null) {
			near.state = 'I';
			show();
		}
	}

	@Action(label = "Infect 5 people")
	public void infect() {
		for (int k = 0; k < 5; k++) {
			agents.get(randomInt(agents.size())).state = 'I';
		}
		show();
	}
}
