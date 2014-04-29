package pacworld;

import agent.Agent;

/** The representation of a Package World agent. Includes information about
    the agent's id, position, and whether or not it is holding anything. 
    This is part of the agent's state, and should not be used by any
    class outside of the simulator. */
public class PacAgentRep {

  Agent agent;
  private int x;
  private int y;
  private String message;            // a message sent by the agent

  /** The package currently held by the agent, or null if the agent is
      not holding a package. */
  private Package heldPackage;

  /** Did the agent (or the package it was holding) bump into something on
      its last move. */
  boolean bumped;

  public PacAgentRep(Agent a) {
    agent = a;
    x = 0;
    y = 0;
    heldPackage = null;
    bumped = false;
  }

  public boolean getBumped() {
    return bumped;
  }

  public String getId() {
    return agent.getId();
  }

  public Package getHeldPackage() {
    return heldPackage;
  }

  public int getX() {
    return x;
  }

  public int getY() {
    return y;
  }

  public String getMessage() {
    return message;
  }
  
  public boolean hasBumped() {
    return bumped;
  }

  public boolean isHolding() {
    if (heldPackage == null)
      return false;
    else
      return true;
  }

  public void setBumped(boolean bumped) {
    this.bumped = bumped;
  }

  public void setHeldPackage(Package p) {
    heldPackage = p;
  }

  public void setX(int x) {
    this.x = x;
  }

  public void setY(int y) {
    this.y = y;
  }

  public void setMessage(String message) {
     this.message = message;
  }
}
