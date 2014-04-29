// Tui Popenoe
// 2014

// To execute:
// from src directory java pacworld.PackageWorldWithCommunication -rand <seed> PackageAgent <numAgents> <numPackages>
// <numDestinations> <worldSize>

package PackageAgent;

import pacworld.*;
import agent.*;
import java.io.*;
import java.util.Random;

public class PacAgentWithCommunication extends Agent {

	// Current Percept
    PacPercept cp;

    // TODO: initialize all to null
    public int[][] internalMap = new int[50][50];

    // An array of all packages currently visible to the agent
    private VisiblePackage[] visPackages = null;
    // The current package being looked at
    private VisiblePackage curPackage = null;
    // The selected package for pickup/delivery
    private VisiblePackage selPackage = null;

    // All agents visible currently visible to the agent
    private VisibleAgent[] visibleAgents = null;

    private VisibleAgent currentAgent = null;

	private int currentX = 0;
	// This agent's current Y coordinate
	private int currentY = 0;

	private int spiralDirection = 0;
	private int spiralCount = 1;
	private int spiralCurrent = 0;

	// The direction to move to deliver the package
	private int deliveryDirection = -1;
	// The distance to the designated delivery location
	private double deliveryDistance = -1;

	private double currentDistance = 0;

    Random rand = new Random();

    /** Provide a Percept to the agent. This function is called by the
	   environment at the beginning of each agent's turn. If the agent has
	   internal state, this method should also update it. */

   public PacAgentWithCommunication(int id) {
	super(id);
   }

   public void see(Percept p)
   {
       cp = (PacPercept) p;

       System.out.println("AGENT ID: "+id);
   }

   /** Have the agent select its next action to perform. Implements the
	   action0: Per -> Ac function or the action: I -> Ac function,
	   depending on whether or not the agent has internal state. Note,
	   Per (or I) is not passed as a parameter, because we assume this
	   information is recorded in an instance variable by the see() method. */
   public Action selectAction()
   {
	   System.out.println("**************************");
	   // Initialize the action to null
       Action a = null;

       // If a collision occured, try to move out of the way
       if(cp.feelBump()){
    	   a = new Move(this.randomDirection());
    	   return a;
       }

       // If the agent is not holding a package,
       // 1. look for a package to pickup
       // 2. If adjacent to a package, pick it up
       if(cp.getHeldPackage() == null){
    	   if(this.selPackage == null){
    		   a = this.findPackage();
    	   }
    	   else{
    		   a = this.getPackage();
    	   }
       }
       // If the agent is holding a package,
       // 1. Look for the delivery coordinates
       // 2. If not adjacent to delivery coordinates
       // move towards the delivery coordinates
       // 3. If adjacent to delivery coordinates, deliver the package
       else{
    	   a = this.deliverPackage();
       }

       // Return the selected action
       return a;
   }

   public Action findPackage(){
	   System.out.println("findPackage()");
	   // Update the agent's coordinates
	   this.getAgentCoordinates();
	   // If the selected package is null
	   // find it

	   this.findNearestPackage();

	   return this.fetchPackage();
   }

   public Action getPackage(){
	   System.out.println("getPackage()");
	   // Update the agents coordinates
	   this.getAgentCoordinates();

	   return this.fetchPackage();
   }

   public Action deliverPackage(){
	   System.out.println("deliverPackage()");
   		// Update the agent coordinates
   		this.getAgentCoordinates();

   		return this.getDeliveryAction();
   }

   public Action getDeliveryAction(){

	   this.calculateDeliveryVector();

		if(deliveryDistance == 1){
			// Reset the selected package
			this.selPackage = null;
   			return new Dropoff(this.deliveryDirection);

	   	}
	   	else{
		   return new Move(deliveryDirection);
	   	}
   }

   public Action fetchPackage(){
	   System.out.println("fetchPackage()");
	   // If we have selected a package
	   if(this.selPackage != null){
		   int packageDirection = this.calculateDirection(currentX, currentY,  this.selPackage.getX(), this.selPackage.getY());
		   double packageDistance = this.calculateDistance(currentX, currentY, this.selPackage.getX(), this.selPackage.getY());

		   System.out.println(packageDirection);
		   if(packageDistance == 1){
			   System.out.println("Picking up " + packageDirection);
			   return new Pickup(packageDirection);
		   }
		   else{
			   System.out.println("Moving " + packageDirection);
			   return new Move(packageDirection);
		   }
	   }
	   else{
		   System.out.println("Moving Spiral");
		   return new Move(this.spiralDirection());
	   }
   }

   // Look through the visible Agents and
   // Set the coordinates of the current agent
   public void getAgentCoordinates(){
	   this.visibleAgents = cp.getVisAgents();
	   System.out.println("Getting Coordinates");
	   for(int i = 0; i < visibleAgents.length; i++){
		   if(visibleAgents[i].getId() == this.getId()){
			   this.currentAgent = visibleAgents[i];
			   this.currentX = this.currentAgent.getX();
			   this.currentY = this.currentAgent.getY();
			   System.out.println("Current " + currentAgent.toString());
			   break;
		   }
		   else{
			   this.currentAgent = null;
		   }
	   }
   }

   public void findNearestPackage(){
	   System.out.println("Finding Nearest Package");
	   this.visPackages = cp.getVisPackages();
	   double minDistance = 100;

	   // If packages can be seen by the agent,
	   // don't bother looking at the internal map
	   if(this.visPackages.length > 0){
	   		// Loop through the visible packages
		   for(int i = 0; i < this.visPackages.length; i++){
			   if(visPackages[i].isHeld()){
				   	// skip this package
			   		// Mark Package as held in the internal map.
			   		internalMap[this.visPackages[i].getX()][this.visPackages[i].getY()] = 3;
				   continue;
			   }
			   else{
			   		// Mark the internal map with a free package
			   		internalMap[this.visPackages[i].getX()][this.visPackages[i].getY()] = 2;
			   }
			   System.out.println("Inspecting " + this.visPackages[i].toString());

			   // Calculate the distance between the agent's current location
			   // and the currentPackage's location
			   this.currentDistance = this.calculateDistance(currentX, currentY, this.visPackages[i].getX(), this.visPackages[i].getY());
			   System.out.println(currentDistance);
			   if( this.currentDistance < minDistance){
				   minDistance = this.currentDistance;
				   System.out.println("Min Distance: " + minDistance);
				   // Set the current package to pickup
				   this.selPackage = this.visPackages[i];
			   }
		   }
	   }
	   // If a nearby package isn't found, look at the internal map before choosing
	   // random movement
	   else{
	   		for(int i=0; i < this.internalMap.length; i++){
	   			for(int j=0; j < this.internalMap.length; j++){
	   				if(this.internalMap[i][j] == 2){
	   					this.currentDistance = this.calculateDistance(currentX, currentY,i, j);
	   					System.out.println("Current Distance using InternalMap: " + currentDistance);

	   					if(this.currentDistance < minDistance){
	   						// Create a dummy package so the agent will move towards
	   						// the coordinates
	   						//this.selPackage = new VisiblePackage(new pacworld.Package());
	   					}
	   				}
	   			}
	   		}

	   }
	   if(this.selPackage != null){
		   // Display the results of the selected package
		   System.out.println("Selected " + this.selPackage.toString());
	   }
	   // Reset current packages
	   this.curPackage = null;
   }

   public int randomDirection(){
	   return rand.nextInt(4);
   }

   public int spiralDirection(){
	   if(spiralCurrent < spiralCount){
		   spiralCurrent++;
		   return spiralDirection;
	   }
	   else{
		   spiralCount++;
		   spiralCurrent=0;
		   if(spiralDirection ==3){
			   spiralDirection = 0;
		   }
		   else{
			   spiralDirection++;
		   }
		   return spiralDirection;
	   }
   }

   public void calculateDeliveryVector(){
	   	this.deliveryDistance = this.calculateDistance(this.currentX, this.currentY, this.selPackage.getDestX(), this.selPackage.getDestY());
		this.deliveryDirection = this.calculateDirection(this.currentX,  this.currentY, this.selPackage.getDestX(), this.selPackage.getDestY());
   }

   // Calculates the distance between an agent and a target package
   // Returns an integer
   public double calculateDistance(int ax, int ay, int tx, int ty){
	   double distance = Math.sqrt((Math.pow((ax-tx), 2) + Math.pow((ay-ty), 2)));
	   System.out.println("Distance between agent and target: " + distance);
	   return distance;
   }

   /**Note, the grid is a 0 indexed grid, with 0,0 in the top left
   (0,0) (1,0) (2,0) (3,0) (4,0)
   (0,1) (1,1) (2,1) (3,1) (4,1)
   (0,2) (1,2) (2,2) (3,2) (4,2)
   (0,3) (1,3) (2,3) (3,3) (4,3)
   (0,4) (1,4) (2,4) (3,4) (4,4)**/
   public int calculateDirection(int ax, int ay, int tx, int ty){
	   if((ax - tx) > 0){
		   // Diagonal NW
		   if((ay - ty) > 0){
			   //NWW
			   if(Math.abs(ax - tx) >= Math.abs(ay-ty)){
				   System.out.println("NWW");
				   return 3;
			   }
			   // NNW
			   else{
				   System.out.println("NNW");
				   return 0;
			   }
		   }
		   // Diagonal SW
		   else if((ay - ty) < 0){
			   //SWW
			   if(Math.abs(ax - tx) >= Math.abs(ay-ty)){
				   System.out.println("SWW");
				   return 3;
			   }
			   //SSW
			   else{
				   System.out.println("SSW");
				   return 2;
			   }
		   }
		   // No Vertical component, move West
		   else{
			   System.out.println("W");
			   return 3;
		   }
	   }
	   else if((ax - tx) < 0){
		   // Diagonal NE
		   if((ay - ty) > 0){
			   //NEE
			   if(Math.abs(ax - tx) >= Math.abs(ay-ty)){
				   System.out.println("NEE");
				   return 1;
			   }
			   // NNE
			   else{
				   System.out.println("NNE");
				   return 0;
			   }
		   }
		   // Diagonal SE
		   else if((ay - ty) < 0){
			   //SEE
			   if(Math.abs(ax - tx) >= Math.abs(ay-ty)){
				   System.out.println("SEE");
				   return 1;
			   }
			   //SSE
			   else{
				   System.out.println("SSE");
				   return 2;
			   }
		   }
		   // No Vertical component, move east
		   else{
			   System.out.println("E");
			   return 1;
		   }
	   }
	   // No Horizontal component
	   else{
		   if((ay - ty) > 0){
			   System.out.println("N");
			   return 0;
		   }
		   else if((ay - ty) < 0){
			   System.out.println("S");
			   return 2;
		   }
		   else{
			   // An Error
			   System.out.println("ERROR in calculating Direction");
			   return -1;
		   }
	   }
   }
}
