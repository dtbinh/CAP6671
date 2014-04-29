package pacworld;

import agent.Agent;
import java.awt.*;
import javax.swing.*;

/** A package delivery GUI class. */
public class CustomPanel extends JPanel {

   private PackageState myState = null;
   private Color[] myColors = new Color[8];

   public CustomPanel() {
      super();

      // initialize colors for destinations
      myColors[0] = Color.red;
      myColors[1] = Color.green;
      myColors[2] = Color.blue;
      myColors[3] = Color.magenta;
      myColors[4] = Color.yellow;
      myColors[5] = Color.pink;
      myColors[6] = Color.cyan;
      myColors[7] = Color.orange;
   }

   public void paintComponent(Graphics g) {
      super.paintComponent(g);
      if (myState == null) return;

      g.setColor(Color.white);
      g.fillRect(0, 0, 5 * myState.getMapSize(), 5 * myState.getMapSize());

      Object[][] myMap = myState.getMap();
      if (myState.getDestinations().length > 8) return;
      for (int i = 0; i < myState.getDestinations().length; i++) {
         g.setColor(myColors[i]);
         g.drawOval(myState.getDestinations()[i].getX() * 5 - 2, 
               myState.getDestinations()[i].getY() * 5 - 2, 9, 9);
      }
      for (int i = 0; i < myState.getMapSize(); i++) {
         for (int j = 0; j < myState.getMapSize(); j++) {
            if (myMap[i][j] != null) {
               if (myMap[i][j] instanceof Package) {
                  Package p = (Package)(myMap[i][j]);
                  g.setColor(myColors[p.getDestId()]);
                  g.fillRect(5 * i, 5 * j, 5, 5);
               }
               else if (myMap[i][j] instanceof Agent) {
                  Agent a = (Agent)(myMap[i][j]);
                  if (myState.getPackageHeldByAgent(a) == null)
                     g.setColor(Color.black);
                  else g.setColor(Color.gray);
                  g.fillRect(5 * i, 5 * j, 5, 5);
               }
            } // myMap[i][j] != null
         } // for j
      } //for j
   }

   public void drawMap(PackageState state) {

      myState = state;
      repaint();
   }
}
