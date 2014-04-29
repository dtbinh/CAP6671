package pacworld;

/** The representation of an agent in an agent's percept. This includes
    the agent's id and current location. */
public class VisibleAgent {

   private String id;
   private int x = 0;
   private int y = 0;

   public VisibleAgent(String id, int x, int y) {
      this.id = id;
      this.x = x;
      this.y = y;
   }

   /** Get the unique string id for the agent. */
   public String getId() {
      return id;
   }

   /** Get the X location of the agent. */
   public int getX() {
      return x;
   }

   /** Get the Y location of the agent. */
   public int getY() {
      return y;
   }

   /** Tests if this object is equivalent to another object by comparing the id's of the agents. */
   public boolean equals(Object obj) {

      VisibleAgent otherAgent;

      if (obj instanceof VisibleAgent) {
         otherAgent = (VisibleAgent)obj;
         if (id.equals(otherAgent.getId()))
            return true;
      }
      return false;
   }

   public String toString() {
      return "Agent " + id + ": Loc=(" + x + ", " + y + ")";
   }
}
