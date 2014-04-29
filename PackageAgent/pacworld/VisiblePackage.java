package pacworld;

/** The representation of a package in an agent's percept. This includes
    the package's id, current location, delivery location, and whether or
    not it is held by an agent. */
public class VisiblePackage {

   private int id = 0;
   private int x = 0;
   private int y = 0;
   private int destX = 0;
   private int destY = 0;
   private boolean held = false;

   /** Create a new percept of a package using the package itself. */
   public VisiblePackage(Package pack) {

      id = pack.getId();
      x = pack.getX();
      y = pack.getY();
      destX = pack.getDestX();
      destY = pack.getDestY();
      if (pack.getAgent() == null)
         held = false;
      else
         held = true;
   }

   /** Get the X coordinate of the location to deliver the package. */
   public int getDestX() {
      return destX;
   }

   /** Get the Y coordinate of the location to deliver the package. */
   public int getDestY() {
      return destY;
   }

   /** Is the package currently held by some agent? */
   public boolean isHeld() {
      return held;
   }

   /** Get the unique integer id for the package. */
   public int getId() {
      return id;
   }

   /** Get the X location of the package. */
   public int getX() {
      return x;
   }

   /** Get the Y location of the package. */
   public int getY() {
      return y;
   }

   /** Tests if this object is equivalent to another object by comparing the id's of the packages. */
   public boolean equals(Object obj) {

      VisiblePackage otherPac;

      if (obj instanceof VisiblePackage) {
         otherPac = (VisiblePackage)obj;
         if (id == otherPac.getId())
            return true;
      }
      return false;
   }

   public String toString() {
      return "Package " + id + ": Loc=(" + x + ", " + y + "), " +
      "Dest=(" + destX + ", " + destY + "), Held=" + held;
   }
}
