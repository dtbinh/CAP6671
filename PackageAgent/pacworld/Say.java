package pacworld;

import agent.Agent;
import agent.Action;
import agent.State;

/** A package delivery agent action that causes the agent to broadcast
    a message to all other agents. There is no limit on the range or
    size of the message. */
public class Say extends Action {
  private String str;

  public Say(String s) {
    str = s;
  }

  /** Return the state that results from the agent sending a message.
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
      System.out.println("ERROR - Argument to Pickup.execute() is not of type PacAgent");

    /* since we will be copying the state and bumps depend only on the
       current move, unset the bump condition */
    state.setBump(agent, false);
    state.setIdleCount(0);
    state.setMessage(agent, str);
    return state;
  }

  public String toString() {
    return "SAY " + str;
  }
}
