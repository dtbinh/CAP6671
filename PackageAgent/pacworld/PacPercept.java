package pacworld;

import agent.Percept;
import agent.Agent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/** A percept in the package delivery world. */
public class PacPercept extends Percept{

   protected boolean bump;
   protected ArrayList<VisibleAgent> visAgents;
   protected ArrayList<VisiblePackage> visPackages;
   protected String[] messages;
   protected VisiblePackage heldPackage;
   protected int worldSize;


   /** Construct a package delivery world percept. The agents can
      see everything in 9x9 grid centered on it. If the agent moved into an
      obstacle on the previous turn the agent will feel a bump. */
   public PacPercept(PackageState state, Agent agent) {

      super(state,agent);

      worldSize = state.getMapSize();

      int ax = state.getAgentX(agent);
      int ay = state.getAgentY(agent);
      
      Collection<PacAgentRep> agentReps = state.getAgentReps();
      visAgents = new ArrayList<VisibleAgent>();
      Iterator<PacAgentRep> agtItr = agentReps.iterator();
      int tx, ty;
      while (agtItr.hasNext()) {
         PacAgentRep arep = agtItr.next();
         tx = arep.getX();
         ty = arep.getY();
         if (inRange(ax,ay,tx,ty)) {
            VisibleAgent vagent = new VisibleAgent(arep.getId(), arep.getX(), 
                                                   arep.getY());
            visAgents.add(vagent);
         }
      }

      List<Package> packages = state.getPackages();
      visPackages = new ArrayList<VisiblePackage>();
      for (int i=0; i < state.getOrigNumPackages(); i++) {
         if (packages.get(i) != null) {
            Package pack = (Package)(packages.get(i));
            tx = pack.getX();
            ty = pack.getY();
            if (inRange(ax,ay,tx,ty)) {
               VisiblePackage vpack;
               vpack = new VisiblePackage(pack);
               visPackages.add(vpack);
               
               // added 9/29/09
               PacAgentRep arep = pack.getAgent(); 
               if (arep != null && arep.agent == agent)
                  heldPackage = vpack;
            }
         }
      }
      messages = state.getMessages().clone();

      // determine bump
      if (state.bumped(agent))
         bump = true;
      else
         bump = false;

      // printPercept();
   }

   /** Returns true if the percept reflects that the agent bumped into
      an obstacle as a result of its most recent action. */
   public boolean feelBump() {
      return bump;
   }


   /** Returns the length of the world. Note, the world is a square,
      so this also doubles as its height. */
   public int getWorldSize() {
      return worldSize;
   }

   /** Returns an array of the visible agents. Each element is of type
      VisibleAgent. */
   public VisibleAgent[] getVisAgents() {
      return visAgents.toArray(new VisibleAgent[visAgents.size()]);
   }

   /** Return an array of the visible packages. Each element is of type
      VisiblePackage. Only packages that have not been delivered will
      appear. */
   public VisiblePackage[] getVisPackages() {
      return visPackages.toArray(new VisiblePackage[visPackages.size()]);
   }

   /** Returns an array of strings representing messages sent since the
      agent's last turn. There are only as many elements as messages
      sent, and there is no identification of which agent sent which
      message. */
   public String[] getMessages() {
      return messages;
   }

   /** Returns the visible package that the agent is currently holding.
    * If the agent is not holding a package, returns null. Note, since
    * the agent can see all packages in the 9x9 square centered on 
    * itself, this package is also returned by getVisPackages().
    * @return
    */
   public VisiblePackage getHeldPackage() {
      return heldPackage;
   }
   
   /* Print information about the percept to the console. Useful for
    * debugging.
    */
   public void printPercept() {
      System.out.println();
      System.out.println("Percept:");
      for (int i=0; i < visAgents.size(); i++)
         System.out.println(visAgents.get(i));
      for (int i=0; i < visPackages.size(); i++)
         System.out.println(visPackages.get(i));
      for (int i=0; i < messages.length; i++)
         System.out.println(messages[i]);
      System.out.println();
      System.out.println();
   }

   /* Given the x,y location of an agent and the x,y location of a
    * target, return true if the target is within sight range of
    * the agent. The agent can see everything in a 5x5 square centered
    * on it.
    */
   protected boolean inRange(int ax, int ay, int tx, int ty) {
      return (tx >= ax-4) && (tx <= ax+4) && (ty >= ay-4) && (ty <= ay+4); 
   }
}
