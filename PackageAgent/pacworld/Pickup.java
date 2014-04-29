package pacworld;

import agent.Agent;
import agent.Action;
import agent.State;

/** A package delivery agent action that causes the agent to pickup a
    package that is located next to the agent in the specified direction.
 */
public class Pickup extends Action {

   /* Construct a new Pickup action. The parameter should use one of the
    * predefined constants from pacworld.Direction. */
   public Pickup(int d) {
      if (d < 0 || d > 3)
         System.out.println("ERROR - Invalid direction for Pickup: " + d);
      direction = d;
   }

   /** Return the state that results from the agent picking up a package in 
      the given direction. In order to avoid creating unnecessary objects, we
      do not create a new state, but instead modify the old one. This
      would have to change if the Environment needs to maintain a history
      of states. */
   public State execute(Agent agent, State s) {

      int x,y;
      int pacX, pacY;
      PackageState state = null;

      if (s instanceof PackageState)
         state = (PackageState)s;
      else
         System.out.println("ERROR - Argument to Pickup.execute() is not of type PackageState");

      if (!(agent instanceof Agent))
         System.out.println("ERROR - Argument to Pickup.execute() is not of type Agent");

      /* since we will be copying the state and bumps depend only on the
       current move, unset the bump condition */
      state.setBump(agent, false);

      state.setIdleCount(0);
      state.setMessage(agent, null);

      if (direction < 0 || direction > 3) {
         System.out.println("NO OP: Invalid direction for Pickup: " + direction);
         return state;
      }

      Package ph = state.getPackageHeldByAgent(agent);
      if (ph != null) {
         System.out.println("ERROR - Agent passed to Pickup.execute() already has a Package");
         return state;
      }

      x = state.getAgentX(agent);
      y = state.getAgentY(agent);
      pacX = x + Direction.DELTA_X[direction];
      pacY = y + Direction.DELTA_Y[direction];

      if (state.inBounds(pacX,pacY) == false) {
         System.out.println("NO OP: Can't pickup out of bounds");
         return state;
      }

      Object obj = state.getObjectAt(pacX, pacY);
      if (obj instanceof Package) {
         Package p = (Package)obj;
         if (p.getAgent() != null) {
            System.out.println("NO OP: Package already held by another agent!");
            return state;
         }
         else
            state.pickup(agent, (Package)obj); 
      }

      return state;
   }

   public String toString() {
      return "PICKUP " + Direction.toString(direction);
   }

   private int direction;
}
