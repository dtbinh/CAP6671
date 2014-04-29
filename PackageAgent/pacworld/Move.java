package pacworld;

import agent.Agent;
import agent.Action;
import agent.State;

/** A package delivery agent action that causes the agent to advance one
    step in the given direction. */
public class Move extends Action {

   /** Construct a new move direction. The parameter should use one of the
    * predefined constants from pacworld.Direction. */
   public Move(int d) {
      if (d < 0 || d > 3)
         System.out.println("ERROR - Invalid direction for Move: " + d);
      dir = d;
   }

   /** Return the state that results from the agent moving in the given direction
      in the given state. In order to avoid creating unnecessary objects, we
      do not create a new state, but instead modify the old one. This
      would have to change if the Environment needs to maintain a history
      of states. */
   public State execute(Agent agent, State s) {

      int x,y;
      int newX, newY;
      Package p;
      int pacX = 0;
      int pacY = 0;
      int newPX = 0;
      int newPY = 0;
      PackageState state = null;

      if (s instanceof PackageState)
         state = (PackageState)s;
      else
         System.out.println("ERROR - Argument to Move.execute() is not of type PackageState");

      if (!(agent instanceof Agent))
         System.out.println("ERROR - Argument to Pickup.execute() is not of type Agent");

      /* since we will be copying the state and bumps depend only on the
       current move, unset the bump condition */
      state.setBump(agent, false);

      state.setIdleCount(0);
      state.setMessage(agent, null);

      x = state.getAgentX(agent);
      y = state.getAgentY(agent);
      p = state.getPackageHeldByAgent(agent);
      if (p != null) {
         pacX = p.getX();
         pacY = p.getY();
      }

      if (dir >= 0 && dir < 8) {
         newX = x + Direction.DELTA_X[dir];
         newY = y + Direction.DELTA_Y[dir];
         if (p!= null) {
            newPX = pacX + Direction.DELTA_X[dir];
            newPY = pacY + Direction.DELTA_Y[dir];
         }
      }
      else {
         System.out.println("ERROR - Invalid direction for agent.");
         newX = x;
         newY = y;
      }


      /* if there is an obstacle in front, the agent doesn't move but
       instead feels a bump. */
      if (p != null) {            // package can bump into things too!
         // note, a package carried by the agent does not count as an obstacle
         // and the agent does not count as an obstacle for the package.
         if (state.inBounds(newX,newY) && state.inBounds(newPX,newPY)
               && ((newX == pacX && newY == pacY && !state.hasObstacle(newPX, newPY))
                     || (newPX == x && newPY == y && !state.hasObstacle(newX, newY))
                     || (!state.hasObstacle(newX, newY) && !state.hasObstacle(newPX, newPY)))) {
            state.repositionAgent(agent, newX, newY);
            state.repositionPackage(p, newPX, newPY);
         }
         else
            state.setBump(agent, true);
      }
      else {
         if (state.inBounds(newX,newY) && !state.hasObstacle(newX, newY)) {
            state.repositionAgent(agent, newX, newY);
         } 
         else
            state.setBump(agent, true);
      }
      return state;
   }

   public String toString() {
      return "MOVE " + Direction.toString(dir);
   }

   private int dir;
}
