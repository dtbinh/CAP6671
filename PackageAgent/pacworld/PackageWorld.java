/* Package World Simulator
   Author: Jason Boire
   Significant modifications by: Jeff Heflin
 */
package pacworld;

import agent.*;

import java.util.List;
import java.util.Date;
import javax.swing.JOptionPane;
import java.io.*;
import java.lang.reflect.Constructor;

/** A simulator for the package delivery world environment. */
public class PackageWorld extends Environment{

   /** The number of milliseconds used by agents to process percepts and
      choose actions. */
   protected long thinkTime = 0;

   public static int DEFAULT_NUM_AGENTS = 4;

   /** The length (and width) of the world. The world must be a square. */
   public static int DEFAULT_WORLD_SIZE = 50;

   /** Number of packages. */
   public static int DEFAULT_NUM_PACKAGES = 20;

   /** Number of destinations. */
   public static int DEFAULT_NUM_DESTINATIONS = 3;
   public static int MAX_NUM_DESTINATIONS = 8;

   protected int numAgents = 0;
   protected int worldSize = DEFAULT_WORLD_SIZE;
   protected int numPackages = DEFAULT_NUM_PACKAGES;
   protected int numDestinations = DEFAULT_NUM_DESTINATIONS;

   // protected boolean logActions = false;
   
   // note, actLog must be static for PacTest
   public static PrintStream actLog = null;
   
   public PackageWorld() {
      super();
   }

   public PackageWorld(PrintStream actLog) {
      super();
      this.actLog = actLog;
   }

   /** Add a new agent to the environment. Note, the agents List is
      actually an ArrayList of PacAgentReps. */
   public void addAgent(Agent agent) {

      // int id = agents.size();
      // PacAgentRep arep = new PacAgentRep(id, (PacAgent)agent);
      agents.add(agent);
   }

   /** Returns a list of the agents in the environment. */
   public List<Agent> getAgents() {
      return agents;
   }

   /** Clears all agents from the environment. */
   public void resetAgents() {
      agents.clear();
   }

   /** Returns a string containing score information for the agent team. */
   public String getScoreMsg() {
      PackageState s = (PackageState)state;

      String str = "Summary:\n";

      // note some the values below aren't calculated until we calculate
      // performance, so we must do that first!
      int perf = getTeamPerformanceMeasure();
      str += "\nPackages delivered: ";
      str += delivered + " / " + s.getOrigNumPackages();
      str += "\nTurns per package: " + avgEffort;
      str += "\nMessage length per turn: " + msgLengthPerTurn;
      str += "\nThink time per turn: " + thinkPerTurn;

      /*
      str += "\nTotal number of turns: ";
      str += count;
      str += "\nTotal Number of actions: ";    // not including IDLE
      str += s.getWorkCount();
      str += "\nMoves with packages: ";
      str += s.getPacCount();
      str += "\nTotal message count: ";
      str += s.getMessageCount();
      str += "\nTotal message length: ";
      str += s.getTotalMessageLength();
      str += "\nTotal think time (ms): ";
      str += thinkTime;
       */
      str += "\n\nPerformance Measure: ";
      str += perf;
      return str;
   }

   /** The simulation is complete when all packages have been delivered or
    * nothing has happened for two cycles or no packages have been picked up
    * or dropped off for 5 * n cycles. */
   protected boolean isComplete() {
      PackageState s = (PackageState)state;

      // create a local delivered variable
      int delivered = s.getOrigNumPackages() - s.getNumPackages();
      if (s.getNumPackages() == 0 || 
      		s.getIdleCount() >= 2 * getNumAgents() ||
      		(s.getNonProductiveCount() >= 5 * getNumAgents() * worldSize &&
      				// if the following is true, then delivering the rest of the packages
      				// will no longer be able to improve the score
      				(count * s.getOrigNumPackages() * 2) / (s.getNumPackages()) > 1000))
      {
         //s.printState();

         if (batch == false) {
            String str = "Simulation Complete.\n" + getScoreMsg();
            JOptionPane.showMessageDialog(null, str, "Final Results", 
                  JOptionPane.INFORMATION_MESSAGE);
         }
         return true;
      }
      else
         return false;
   }

   /** Return the performance measure of the agent in the current 
      environment. Since this is a team environment, we only
      return the team's performance measure, regardless of the 
      agent. */
   public int getPerformanceMeasure(Agent a) {
      return getTeamPerformanceMeasure();
   }


   /** Return the performance measure of a team of agents in a
    *  cooperative environment. Higher values are considered better. */
   public int getTeamPerformanceMeasure() {
      PackageState s = (PackageState)state;

      delivered = s.getOrigNumPackages() - s.getNumPackages();
      avgEffort = (float)count /  delivered;
      msgLengthPerTurn = (float)s.getTotalMessageLength() / count;
      thinkPerTurn = (float)thinkTime / count;

      int perf = (int)(((float)delivered / s.getOrigNumPackages() * 1000) - 
            (avgEffort * 2) - (msgLengthPerTurn / 100) - thinkPerTurn);
      return perf;
   }


   /** Create a percept for an agent. This implements the see: S -> P
      function. */
   protected Percept getPercept(Agent agt) {


      PacPercept p;

      if (state instanceof PackageState) {
         PackageState s = (PackageState) state;
         // PacAgentRep agt = (PacAgentRep) a;
         p = new PacPercept(s, agt);
         // System.out.println("Percept: " + p.toString());
         return p;
      }
      else {
         System.out.println("ERROR - state is not a PackageState object.");
         return null;
      }
   }

   /** Execute an agent's action and update the environment's state. */
   protected void updateState(Agent a, Action action) {

      super.updateState(a, action);
      count++;
   }

   /** Return the size of the grid for the environment. */
   public int getWorldSize() {
      return worldSize;
   }

   /** Return the number of agents in the environment. */
   public int getNumAgents() {
      return agents.size();
   }

   /** Associate a GUI with the environment. */
   public void setGUI(PacGUI gui) {
      pg = gui;
   }

   
   /** Run the simulation starting from a given state. This consists of
      a sense-act loop for each agent. */
   public void start(State initState) {

      Percept p;
      Action action;
      Agent a;
      Date d1, d2;

      state = initState;
      state.display();

      while (!isComplete()) {
         // if neither step nor run was pressed, wait
         while(!pg.step() && !pg.run()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
               e.printStackTrace();
            }
        }
         
         PackageState s = (PackageState)state;
         // s.printState();
         for (int i=0; i < getNumAgents(); i++) {
            a = agents.get(i);
            p = getPercept(a);
            // ((PacPercept)p).printPercept();
            d1 = new Date();
            a.see(p);
            action = a.selectAction();
            d2 = new Date();
            thinkTime += (d2.getTime() - d1.getTime());
            logMessage("Agent " + a.getId() + ": Action = " + action);
            // logMessage("Total think time= " + thinkTime + "ms");

            if (action == null) {
               logMessage("\t" + "Substituting Idle() for null action");
               action = new Idle();
            }
            updateState(a, action);
            state.display();
            try {
               Thread.sleep(pg.getDelay());
               // Thread.sleep(100);
            }
            catch (InterruptedException ie) {
               ie.printStackTrace();
            }
         }
         pg.setStep(false);    // after completing a step, turn step off
         logMessage("");
      }
   }

   /** Outputs a message to the log and the GUI. */
   public void logMessage(String message) {
      if (actLog != null)
         actLog.println(message);
      /*
       else
         System.out.println(message);
         */
      pg.addToActionLog(message);
   }
   
 
   /** Starts the program. */
   public static void main(String[] args) {

      PackageWorld world;
      PackageState initState;
      Class agentClass = null;               // the class for the agent
      int numAgents = DEFAULT_NUM_AGENTS;
      boolean useSeed = false;
      long seed = 0;
      
      world = new PackageWorld();
      world.count = 0;

      int pos = 0;
      if (args.length > 2) {
         if (args[0].equals("-rand")) {
            useSeed = true;
            seed = Integer.parseInt(args[1]);
            pos = 2;
         }
      }
      if (args.length > pos) {
        	String agentName = null;
      	try {
      		agentName = args[pos] + ".PacAgent";
           	pos++;
      		ClassLoader myClassLoader = ClassLoader.getSystemClassLoader();
      		agentClass = myClassLoader.loadClass(agentName);
      	} catch (Exception ex) {
      		System.err.println("ERROR: Loading class " + agentName);
      		System.exit(1);
      	}      	
      }
      else {
      	System.err.println("Usage: java PackageWorld [-rand seed] agentClass [numAgents] [numPackages] [numDestinations] [worldSize]");
      }
      if (args.length > pos) {
         numAgents = Integer.parseInt(args[pos++]);
      }
      if (args.length > pos) {
         world.numPackages = Integer.parseInt(args[pos++]);
      }
      if (args.length > pos) {
         world.numDestinations = Integer.parseInt(args[pos++]);
         if (world.numDestinations > 8) {
            System.out.println("A maximum of " + MAX_NUM_DESTINATIONS +
               " destinations are allowed.");
            System.exit(1);
         }        
      }
      if (args.length > pos) {
         world.worldSize = Integer.parseInt(args[pos++]);
      }
      if (args.length > pos){
         System.out.println("Usage: java pacworld.PackageWorld [-rand seed] agentClass [numAgents] [numPackages] [numDestinations] [worldSize]");
         System.exit(1);
      }

   	// in order to create an instance using anything other than the no-arg
      // constructor, you need to first get the instructor from the class
      // and then call new instance on it
      try {
      	Constructor agentConstructor = agentClass.getConstructor(int.class);
      	for (int i = 0; i < numAgents; i++) {
      		Object[] conArgs = new Object[] {i};
      		Object o = agentConstructor.newInstance(conArgs);
      		Agent agent = (Agent)o;
      		world.addAgent(agent);
      	}
      } catch (Exception ex) {
      	System.err.println("ERROR - Attempting to instantiate agents");
      	System.exit(1);
      }
      if (useSeed == false)
         initState = PackageState.getInitState(world.getAgents(), world.numPackages,
                                             world.numDestinations, world.worldSize);
      else
         initState = PackageState.getInitState(seed, world.getAgents(), world.numPackages,
                                             world.numDestinations, world.worldSize);
         
      PacGUI pg = new PacGUI(world);
      pg.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
      world.setGUI(pg);
      initState.setGUI(pg);

      world.start(initState);
      world.pg.dispose();
      System.exit(0);
   }

   /** The GUI that displays the current world */
   public PacGUI pg = null;
   
   protected PackageState initState = null;

   /** The number of turns (each agent's sense/act pair is a turn) */
   protected int count;

   /** The number of packages successfully delivered. */
   protected int delivered;

   protected float avgEffort;
   protected float msgLengthPerTurn;
   protected float thinkPerTurn;

   /** Run in batch mode? If true, then simulation automatically runs when
      started and doesn't display a summary dialog window. */
   protected static boolean batch = false;

}
