package pacworld;

import agent.Agent;
import agent.Action;
import agent.State;

/** A package delivery agent action that causes the agent to drop a package
    in the specified direction. If the space is not already occupied, the
    package will be left there. If the space is occupied then the action 
    will silently fail, and the agent will be left holding the package.
    If the package is dropped on its delivery destination it will
    disappear, but it will not dissappear if dropped on the incorrect
    delivery destination. Note, the agent does not need a clear
    space to rotate the package to the desired direction. */
public class Dropoff extends Action {

   private int direction;

   /** Construct a new Dropoff action. The parameter should use one of the
    * predefined constants from pacworld.Direction.
    * @param d - The direction to drop the Package in.
    */
   public Dropoff(int d) {
      if (d < 0 || d > 3)
         System.out.println("ERROR - Invalid direction for Dropoff: " + d);
      direction = d;
   }

   /** Return the state that results from the agent dropping a package in the 
    * given direction in the given state. In order to avoid creating unnecessary 
    * objects, we do not create a new state, but instead modify the old one. This
    * would have to change if the Environment needs to maintain a history
    * of states. */
   public State execute(Agent a, State s) {

      PackageState state = null;
      Agent agent = null;

      if (s instanceof PackageState)
         state = (PackageState)s;
      else
         System.out.println("ERROR - Argument to Dropoff.execute() is not of type PackageState");

      if (a instanceof Agent)
         agent = (Agent)a;
      else
         System.out.println("ERROR - Argument to Dropoff.execute() is not of type PacAgent");

      /* since we will be copying the state and bumps depend only on the
       current move, unset the bump condition */
      state.setBump(agent, false);

      state.setIdleCount(0);
      state.setMessage(agent, null);

      if (direction < 0 || direction > 3) {
         System.out.println("NO OP: Invalid direction for Dropoff: " + direction);
         return state;
      }

      int x, y, pacX, pacY;
      x = state.getAgentX(a);
      y = state.getAgentY(a);
      pacX = x + Direction.DELTA_X[direction];
      pacY = y + Direction.DELTA_Y[direction];
      if (state.inBounds(pacX,pacY) == false) {
         System.out.println("NO OP: Can't drop package out of bounds");
         return state;
      }
      if (state.getObjectAt(pacX, pacY) == null ||
            state.getObjectAt(pacX,pacY).equals(state.getPackageHeldByAgent(a)))
         state.dropPackage(agent, pacX, pacY);
      else
         System.out.println("Couldn't drop package due to obstacle.");

      return state;
   }

   public String toString() {
      return "DROPOFF " + Direction.toString(direction);
   }
}
