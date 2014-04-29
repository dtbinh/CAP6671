package vacworld;

import agent.Percept;
import agent.Agent;

/** A percept in the vacuum cleaning world. */
public class VacPercept extends Percept{

	private boolean dirt;
	private boolean obstacle;
	private boolean bump;

	/** Construct a vacuum world percept. If the agent is in a square that
      has dirt, then create a percept that sees dirt. If the agent is
      directly in front of and facing an obstacle, then the agent can
      see the obstacle. If the agent moved into an obstacle on the
      previous turn the agent will feel a bump. */
	public VacPercept(VacuumState state, Agent agent) {

		super(state,agent);

		int x, y;
		int viewX, viewY;
		int dir;

		x = state.getAgentX();
		y = state.getAgentY();
		dir = state.getAgentDir();

		// determine dirt
		if (state.hasDirt(x,y))
			dirt = true;
		else
			dirt = false;

		// determine obstacle
		viewX = x + Direction.DELTA_X[dir];
		viewY = y + Direction.DELTA_Y[dir];
		if (state.hasObstacle(viewX, viewY))
			obstacle = true;
		else
			obstacle = false;

		// determine bump
		if (state.bumped())
			bump = true;
		else
			bump = false;
	}

	/** Returns true if the percept reflects that the agent is over dirt. */
	public boolean seeDirt() {

		return dirt;
	}

	/** Returns true if the percept reflects that the square immediately
      in front of the agent contains an obstacle. */
	public boolean seeObstacle() {

		return obstacle;
	}

	/** Returns true if the percept reflects that the agent bumped into
      an obstacle as a result of its most recent action. */
	public boolean feelBump() {

		return bump;
	}

	public String toString() {

		StringBuffer pstring;

		pstring = new StringBuffer(5);
		if (dirt == true)
			pstring.append("DIRT ");
		if (obstacle == true)
			pstring.append("OBSTACLE ");
		if (bump == true)
			pstring.append("BUMP");
		return pstring.toString();
	}
}
