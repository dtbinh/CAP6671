package pacworld;

import agent.Agent;
import agent.Action;
import agent.State;

/** A package delivery agent action that causes the agent to do nothing. */
public class Idle extends Action {

  public Idle() {
  }

  /** Return the state that results from the agent doing nothing.
      In order to avoid creating unnecessary objects, we
      do not create a new state, but instead modify the old one. This
      would have to change if the Environment needs to maintain a history
      of states. */
  public State execute(Agent agent, State s) {
    PackageState state = null;
    
    if (s instanceof PackageState)
      state = (PackageState)s;
    else
      System.out.println("ERROR - Argument to Idle.execute() is not of type PackageState");

    if (!(agent instanceof Agent))
      System.out.println("ERROR - Argument to Pickup.execute() is not of type Agent");

    /* since we will be copying the state and bumps depend only on the
       current move, unset the bump condition */
    state.setBump(agent, false);
    state.setIdleCount(state.getIdleCount() + 1);
    state.setMessage(agent, null);

    return state;
  }

  public String toString() {
    return "IDLE";
  }
}
