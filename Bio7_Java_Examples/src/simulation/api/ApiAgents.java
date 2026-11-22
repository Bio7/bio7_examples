package simulation.api;

import java.util.ArrayList;

import com.eco.bio7.simulation.Action;
import com.eco.bio7.simulation.Agent;
import com.eco.bio7.simulation.AgentModel;
import com.eco.bio7.simulation.Param;

/**
 * All methods of an AgentModel (agents in a 3D box, displayed in the 3D view of
 * Bio7 and in the WebGL view of the HTML page): particles which move, are
 * colored by the number of their neighbors and follow the nearest particle.
 * <p>
 * The agents are objects of a static nested class which extends Agent (the
 * fields x, y, z and color). The fields of the model: agents (an ArrayList),
 * boxWidth, boxHeight, boxDepth, agentRadius, agentColor. The methods: box,
 * distance, neighbors, nearest, keepInBox, wrapInBox.
 */
public class ApiAgents extends AgentModel<ApiAgents.Particle> {

	@Param(label = "Number of particles", min = 10, max = 400, restart = true)
	int particles = 120;

	@Param(label = "Neighbor radius", min = 1, max = 30, step = 0.5)
	double radius = 10;

	@Param(label = "Following the nearest", min = 0, max = 0.2, step = 0.005)
	double follow = 0.02;

	@Param(label = "At the walls", options = { "Stay in the box", "Wrap around" })
	String walls = "Stay in the box";

	/** A particle: the position and the color (Agent), a velocity (own fields). */
	static class Particle extends Agent {
		double vx, vy, vz;

		Particle(double x, double y, double z) {
			super(x, y, z);
		}
	}

	public void init() {
		/* box(w, h, d): the size of the box (boxWidth, boxHeight, boxDepth; z is the height) */
		box(100, 100, 50);
		/* agentRadius and agentColor: the spheres of the agents without an own color */
		agentRadius = 1.2;
		agentColor = "#1f77b4";
		for (int i = 0; i < particles; i++) {
			Particle p = new Particle(random() * boxWidth, random() * boxHeight, random() * boxDepth);
			p.vx = random(-1, 1);
			p.vy = random(-1, 1);
			p.vz = random(-0.5, 0.5);
			/* agents: the list of the agents */
			agents.add(p);
		}
	}

	public void step() {
		double meanNeighbors = 0;
		for (Particle p : agents) {
			/* nearest(agent, radius): the nearest other agent within the radius (null if none) */
			Particle n = nearest(p, radius);
			if (n != null) {
				p.vx += (n.x - p.x) * follow;
				p.vy += (n.y - p.y) * follow;
				p.vz += (n.z - p.z) * follow;
				/* distance(a, b): the distance of two agents */
				if (distance(p, n) < 2) {
					p.vx = -p.vx;
					p.vy = -p.vy;
				}
			}
			/* neighbors(agent, radius): the other agents within the radius */
			ArrayList<Particle> near = neighbors(p, radius);
			meanNeighbors += near.size();
			/* color: an own color of the agent (null: agentColor) */
			p.color = near.size() > 5 ? "#d62728" : near.size() > 2 ? "#ff7f0e" : null;
		}
		for (Particle p : agents) {
			double s = Math.sqrt(p.vx * p.vx + p.vy * p.vy + p.vz * p.vz);
			if (s > 1.5) {
				p.vx *= 1.5 / s;
				p.vy *= 1.5 / s;
				p.vz *= 1.5 / s;
			}
			p.x += p.vx;
			p.y += p.vy;
			p.z += p.vz;
			if (walls.startsWith("Stay")) {
				/* keepInBox(agent): the position limited to the box */
				if (p.x < 0 || p.x > boxWidth) {
					p.vx = -p.vx;
				}
				if (p.y < 0 || p.y > boxHeight) {
					p.vy = -p.vy;
				}
				if (p.z < 0 || p.z > boxDepth) {
					p.vz = -p.vz;
				}
				keepInBox(p);
			} else {
				/* wrapInBox(agent): x and y on a torus, z limited */
				wrapInBox(p);
			}
		}
		chart("Mean number of neighbors", meanNeighbors / agents.size());
	}

	@Action(label = "Add 20 particles")
	public void addParticles() {
		for (int i = 0; i < 20; i++) {
			agents.add(new Particle(random() * boxWidth, random() * boxHeight, random() * boxDepth));
		}
	}
}
