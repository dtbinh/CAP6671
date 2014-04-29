package pacworld;

import agent.*;
import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;


/** Represents a state in the package world. */
public class PackageState extends State {

   // A pseudo-random number generator used in creating initial states
   private static Random rand = new Random();

   /** Construct a new package state. */
   public PackageState() {
      super();
   }

   /** Array containing state information for the agents. */
   protected HashMap<Agent,PacAgentRep> agentReps;

   protected ArrayList<Package> packages;
   protected Location[] destinations;
   // protected String[] messages;

   protected int numAgents = 0;
   protected int numPackages = 0;
   protected int origNumPackages = 0;
   protected int mapSize = 0;
   protected int numDests = 0;

   protected int idleCount = 0;

   /** The number of actions not including idling. */
   protected int workCount = 0;

   /** The number of moves with a package in hand. */
   protected int pacCount = 0;

   protected int messageCount = 0;
   protected long totalMessageLength = 0;
   protected int nonProductiveCount = 0;
   protected PacGUI gui = null;

   /** An array to that contains the locations of objects in the world. */
   protected Object[][] map;

   /** Use the given seed to generate a pseudo-random initial state for 
    * the vacuum world. */
   public static PackageState getInitState(long seed, List<Agent> agts,
         int numPackages, 
         int numDests, int mapSize) {
      rand = new Random(seed);
      return getInitState(agts, numPackages, numDests, mapSize);
   }

   /** Return a random initial state for the vacuum world. */
   public static PackageState getInitState(List<Agent> agts, int numPackages, 
         int numDests, int mapSize) {
      PackageState state;
      int i, x, y;
      
      state = new PackageState();
      state.origNumPackages = state.numPackages = numPackages;
      state.numAgents = agts.size();
      state.agentReps = new HashMap<Agent,PacAgentRep>();
      state.packages = new ArrayList<Package>();

      state.mapSize = mapSize;
      state.map = new Object[mapSize][mapSize];
      for (i = 0; i < mapSize; i++)
         for (int j = 0; j < mapSize; j++)
            state.map[i][j] = null;

      state.numDests = numDests;
      state.destinations = new Location[numDests];
      // Note: Random.nextFloat() generates numbers s.t. 0.0 <= n < 1.0
      // Casting a float to an int, truncates the fractional part
      for (i = 0; i < numDests; i++) {
         // we don't want to put destinations on the edge of the map
         x = (int)(rand.nextFloat() * (mapSize - 2)) + 1;
         y = (int)(rand.nextFloat() * (mapSize - 2)) + 1;
         state.destinations[i] = new Location(x, y);
      }

      for (i = 0; i < state.numPackages; i++) {
         int destId;
         do {
            x = (int)(rand.nextFloat() * mapSize);
            y = (int)(rand.nextFloat() * mapSize);
            destId = (int)(rand.nextFloat() * numDests);
         } while (state.map[x][y] != null || 
               (state.destinations[destId].getX() == x &&
                     state.destinations[destId].getY() == y));     
         Package p = new Package(i, destId, state.destinations[destId].getX(),
                     state.destinations[destId].getY());
         p.setX(x);
         p.setY(y);
         state.packages.add(p);
         state.map[x][y] = p;
      }

      for (i = 0; i < state.numAgents; i++) {
         Agent agt = agts.get(i);
         PacAgentRep arep = new PacAgentRep(agt);
         do {
            x = (int)(rand.nextFloat() * mapSize);
            y = (int)(rand.nextFloat() * mapSize);
         } while (state.map[x][y] != null);
         arep.setX(x);
         arep.setY(y);
         state.agentReps.put(agt,arep);
         state.map[x][y] = agt;
      }

      /*
      state.messages = new String[state.numAgents];
      for (i = 0; i < state.numAgents; i++)
         state.messages[i] = null;
       */ 
      // state.printState();

      return state;
   }

   public void printState() {
      int i;
      System.out.println("Agents:");
      Iterator<PacAgentRep> repList = agentReps.values().iterator();
      while (repList.hasNext()) {
         PacAgentRep arep = repList.next();
         System.out.println("\tAgent " + arep.getId() + ": x = " +
               arep.getX() + " y = " + arep.getY());
      }

      System.out.println("Packages:");
      for (i = 0; i < packages.size(); i++) {
         Package p = (Package)packages.get(i);
         if (p != null)
            System.out.println("\tPackage " + i + ": x = " + p.getX() + 
                  " y = " + p.getY() + " destination = (" + p.getDestX() + 
                  "," + p.getDestY() + ")");
         else
            System.out.println("\tPackage " + i + ": Delivered Successfully");
      }

      System.out.println("Destinations:");
      for (i = 0; i < numDests; i++) {
         Location p = destinations[i];
         System.out.println("\tDestination " + i + ": x = " + 
               p.getX() + " y = " + p.getY());
      }
      
      System.out.println("Messages:");
      String[] msgs = getMessages();
      for (i = 0; i < msgs.length; i++)
         System.out.println("\t" + msgs[i]);
   }

   protected PacAgentRep getAgentRep(Agent a) {
      return agentReps.get(a);
   }
   
   /** Return the X location of the specified agent. */
   public int getAgentX(Agent a) {
      PacAgentRep arep = getAgentRep(a);
      return arep.getX();
   }
   
   /** Return the Y location of the specified agent. */
   public int getAgentY(Agent a) {
      PacAgentRep arep = getAgentRep(a);
      return arep.getY();
   }

   /** Return the package (if any) held by the specified agent. If the agent is
    *    not holding a package, return NULL.
    */
   public Package getPackageHeldByAgent(Agent a) {
      PacAgentRep arep = getAgentRep(a);
      return arep.getHeldPackage();
   }
   /** Modify the state so that the agent is holding the selected package.
      If the package is already held by an agent, then do nothing. */
   public void pickup(Agent a, Package p) {
      // technically, the following fields should be incremented
      // by the Pickup action (so that trying to pick up something other
      // than a package counts as work!)
      workCount++;
      nonProductiveCount++;

      PacAgentRep arep = getAgentRep(a);
      if (p.getAgent() != null) 
         System.out.println("Agent " + a.getId() + " tried to grab a " +
         "package held by another agent!");
      else {
         arep.setHeldPackage(p);
         p.setAgent(arep);
      }
   }

   /** Change the agent's position. Update both the list of agent locations
      and the map.*/
   public void repositionAgent(Agent a, int newX, int newY) {
      int oldx, oldy;

      workCount++;
      nonProductiveCount++;
      PacAgentRep arep = getAgentRep(a);
      oldx = arep.getX();
      oldy = arep.getY();
      arep.setX(newX);
      arep.setY(newY);
      // make sure we haven't moved something to agent's position first
      if (map[oldx][oldy] == a)
         map[oldx][oldy] = null;      
      map[newX][newY] = a;
   }

   /** Change a package's position. Update the package's location and
      the map. */
   public void repositionPackage(Package p, int newX, int newY) {
      int oldx, oldy;

      pacCount++;
      oldx = p.getX();
      oldy = p.getY();
      p.setX(newX);
      p.setY(newY);
      // it is possible that the agent have moved in the direction of
      // of the package, so we must be careful not to overwrite it.
      if (map[oldx][oldy] instanceof Package)
         map[oldx][oldy] = null;      
      map[newX][newY] = p;
   }


   /** Set an indicator that the robot bumped into an obstacle 
      during its previous move. */
   public void setBump(Agent a, boolean bump) {
      PacAgentRep arep = getAgentRep(a);
      arep.setBumped(bump);
   }

   /** Return true if the specified location has an obstacle in it. */
   public boolean hasObstacle(int x, int y) {

      if (map[x][y] != null)
         return true;
      else
         return false;
   }

   /** Return the object located at the specified location. If there is
      no object there, returns null. */
   public Object getObjectAt(int x, int y) {
      return map[x][y];
   }

   /** Return true if the agent bumped into an obstacle on its previous
      move. */
   public boolean bumped(Agent a) {
      PacAgentRep arep = getAgentRep(a);
      return arep.hasBumped();
   }

   /** Return true if the location is within bounds of the state's map. */
   public boolean inBounds(int x, int y) {

      if (x >= 0 && x < mapSize && y >= 0 && y < mapSize)
         return true;
      else
         return false;
   }

   public void dropPackage(Agent a, int dropX, int dropY) {
      // technically, the following fields should be incremented
      // by the Dropoff action (so that trying to drop something in
      // an occupied square counts as work!)
      workCount++;
      nonProductiveCount++;
      PacAgentRep arep = getAgentRep(a);
      Package p = arep.getHeldPackage();
      if (p != null) {
         p.setAgent(null);
         arep.setHeldPackage(null);
         int x = p.getX();
         int y = p.getY();

         // if package is dropped at its destination, then it disappears
         if (dropX == p.getDestX() && dropY == p.getDestY()) {
            packages.set(p.getId(), null);
            p = null;
            map[x][y] = null;
            numPackages--;
            nonProductiveCount = 0;
         }
         // if package is dropped in a different location from where it was
         else if (x != dropX || y != dropY) {
            map[x][y] = null;
            map[dropX][dropY] = p;
            p.setX(dropX);
            p.setY(dropY);
         }
         // otherwise, package stays where it is
      }
      else
         System.out.println("Not holding a package.");
   }

   public int getNumPackages() {
      return numPackages;
   }

   public int getOrigNumPackages() {
      return origNumPackages;
   }

   public int getMapSize() {
      return mapSize;
   }

   public int getIdleCount() {
      return idleCount;
   }

   public void setIdleCount(int ic) {
      if (ic > idleCount)
         nonProductiveCount++;
      idleCount = ic;
   }

   public List<Package> getPackages() {
      return packages;
   }

   public Collection<PacAgentRep> getAgentReps() {
      return agentReps.values();
   }
   
   public String[] getMessages() {
      ArrayList<String> messages = new ArrayList<String>();
      Iterator<PacAgentRep> agtItr = agentReps.values().iterator();
      while (agtItr.hasNext()) {
         PacAgentRep arep = agtItr.next();
         String msg = arep.getMessage();
         if (msg != null)
            messages.add(msg);
      }
      return messages.toArray(new String[messages.size()]);
   }

   public Location[] getDestinations() {
      return destinations;
   }

   public int getWorkCount() {
      return workCount;
   }

   public int getPacCount() {
      return pacCount;
   }

   public int getMessageCount() {
      return messageCount;
   }

   public long getTotalMessageLength() {      
      return totalMessageLength;
   }

   public int getNonProductiveCount() {
      return nonProductiveCount;
   }

   /** Set whatever message was said by the agent in its previous move. */
   public void setMessage(Agent a, String s) {
      PacAgentRep arep = getAgentRep(a);
      arep.setMessage(s);
      if (s != null) {
         workCount++;
         nonProductiveCount++;
         messageCount++;
         totalMessageLength += s.length();
      }
   }

   /** Print an output of the state to the screen. */
   public void display() {
      if (gui != null)
         gui.drawMap(this);
   }

   public void setGUI(PacGUI pg) {
      gui = pg;
   }

   /** Returns the map. This is needed for the CustomPanel class. */
   public Object[][] getMap() {
      return map;
   }

   /*
   public PacAgentRep[] getAgentReps() {
      return agentReps;
   }
    */
}



