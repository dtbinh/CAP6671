// Tui Popenoe 2014
// Copyright 2014


// Execute from the command line in the src directory using
// java -cp . vacworld.VacuumWorld -rand <number> u79729892
// where <number> is an integer

package VacAgent;

import agent.*;
import vacworld.*;
import java.util.Random;

public class VacAgent extends Agent {

    // Direction booleans.
    // These are flipped to true depending on the input
    // from the percept passed to the agent.
    // The agent then selects and action and returns these
    // to false.
    private boolean dirtDetected = false;
    private boolean turnLeft = false;
    private boolean turnRight = false;
    private boolean goForward = false;
    private boolean shutOff = false;

    // Used to seed random
    private Random random = new Random();

    // Change this if the number of dirt is known. Otherwise,
    // the vacuum will continue searching until it hits the move limit
    private int dirtCount = 4;

    // Change this to allow more or fewer moves to the vacuum.
    private int moveLimit = 100;

    // Current number of moves performed.
    // numMoves is incremented each time an action is taken.
    // if numMoves reaches the moveLimit, the vacuum will shutdown
    private int numMoves = 0;

    @Override
    public void see(Percept p) {

        // See if
        //if((VacPercept)p.){
        //  System.out.println(p.x + " " + p.y);
        //}


        // Increment numMoves. If over the moveLimit,
        // the vacuum will shutdown.
        numMoves++;

        // If dirt is detected, vacuum it up.
        // Reduce the count of dirt remaining in the
        // simulation.
        if(((VacPercept) p).seeDirt()){
            this.dirtDetected = true;
            dirtCount--;

            return;
        }

        // If an obstacle is detected, turn
        // randomly, to avoid infinite loop scenarios
        if(((VacPercept) p).seeObstacle()){

            this.selectRandomDirection();
            return;
        }

        // If a bump is felt decide what to do
        if(((VacPercept) p).feelBump()){
            this.selectRandomDirection();

            return;
        }

        // If all the dirt has been cleaned,
        if(this.dirtCount == 0 || (numMoves >= moveLimit)){
            this.shutOff = true;

            return;
        }

        // If there are no other inputs,
        // Go forward
        else{
            this.goForward = true;

            return;
        }
    }

    // Select a random direction to turn
    public void selectRandomDirection(){
        int randInt = random.nextInt(100);

        if(randInt <50)
        {
            this.turnRight = true;
        }
        else
        {
            this.turnLeft = true;
        }

        return;
    }

    @Override
    public Action selectAction() {
        if(this.dirtDetected){
            System.out.println("Suck Dirt");
            this.dirtDetected = false;
            return new SuckDirt();
        }
        if(this.turnRight){
            this.turnRight = false;
            return new TurnRight();
        }
        if(this.turnLeft){
            this.turnLeft = false;
            return new TurnLeft();
        }
        if(this.goForward){
            this.goForward = false;
            return new GoForward();
        }
        if(this.shutOff){
            this.shutOff = false;
            return new ShutOff();
        }

        // Else return a null action
        return null;
    }

    // Return the user ID
    @Override
    public String getId() {
        return "u79729892";
    }

    public String toString(){
        System.out.println("dirtDetected: " + dirtDetected);
        System.out.println("turnLeft: " + turnLeft);
        System.out.println("turnRight: " + turnRight);
        System.out.println("goForward: " + goForward);
        System.out.println("shutOff: " + shutOff);

        return "\n";
    }
}
