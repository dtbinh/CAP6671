package pacworld;

/** A package for the package delivery world. */
public class Package {

   private int x = 0;
   private int y = 0;
   private int destX = 0;
   private int destY = 0;
   private int destId = 0;
   private PacAgentRep a = null;
   private int id = 0;

   /** Old version of constructor. */
   /*
   public Package(int i) {
      a = null;
      id = i;
   }
   */
   
   /** Create a package by specifying its id and destination. */
   public Package(int i, int destId, int destX, int destY) {
      a = null;
      id = i;
      this.destId = destId;
      this.destX = destX;
      this.destY = destY;      
   }
   
   /** Return the agent that is holding the package. Return null if no agent is
    * holding it.
    * @return
    */
   public PacAgentRep getAgent() {
      return a;
   }

   public int getId() {
      return id;
   }

   public int getDestId() {
      return destId;
   }

   public int getDestX() {
      return destX;
   }

   public int getDestY() {
      return destY;
   }

   public int getX() {
      return x;
   }

   public int getY() {
      return y;
   }

   /*
   public void setDestination(int destId, int destX, int destY) {

      this.destId = destId;
      this.destX = destX;
      this.destY = destY;
   }
*/
   
   public void setX(int newX) {
      x = newX;
   }

   public void setY(int newY) {
      y = newY;
   }

   public void setAgent(PacAgentRep agt) {
      a = agt;
   }

   public boolean equals(Object obj) {

      Package otherPac;

      if (obj instanceof Package) {
         otherPac = (Package)obj;
         if (id == otherPac.getId())
            return true;
      }
      return false;
   }

}
