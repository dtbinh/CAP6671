package pacworld;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.Hashtable;

/** A package delivery GUI class. */
public class PacGUI extends JFrame {
   private JButton resetButton;
   private JButton runButton;
   private JButton stepButton;
   private JButton completeButton;
   private JPanel buttonPanel;
   private CustomPanel mapPanel;
   private JTextArea actionLogArea; 
   private boolean step = false;
   private boolean run = false;
   private boolean dontUpdate = false;
   private boolean reset = false;
   private int i = 0;
   private int delay = 100;              // the delay in msec
   private PackageWorld world = null;

   public PacGUI(PackageWorld world, boolean autorun) {

      super("Package Delivery Environment");
      this.world = world;
      run = autorun;

      mapPanel = new CustomPanel();
      mapPanel.setPreferredSize(new Dimension(world.getWorldSize() * 5, world.getWorldSize() * 5));
      reset = false;

      runButton = new JButton("Run");
      runButton.addActionListener(
            new ActionListener() {
               public void actionPerformed(ActionEvent event)
               {
                  dontUpdate = false;
                  run = true;
               }
            }
      );

      /*
    resetButton = new JButton("Reset");
    resetButton.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent event)
        {
          reset = true;
          run = true;
          PackageWorld.setReset(true);
        }
      }
    );
       */

      stepButton = new JButton("Step");
      stepButton.addActionListener(
            new ActionListener() {
               public void actionPerformed(ActionEvent event)
               {
                  dontUpdate = false;
                  step = true;
                  run = false;
               }
            }
      );

      completeButton = new JButton("Complete");
      completeButton.addActionListener(
            new ActionListener() {
               public void actionPerformed(ActionEvent event)
               {
                  run = true;
                  dontUpdate = true;
               }
            }
      );

      buttonPanel = new JPanel();
      buttonPanel.setLayout(new GridLayout(1, 4));
      buttonPanel.add(runButton);
      buttonPanel.add(stepButton);
      buttonPanel.add(completeButton);
      // buttonPanel.add(resetButton);

      // added JDH: 11/12/09
      // JPanel sliderPanel = new JPanel();
      JLabel sliderLabel = new JLabel("Simulation Speed", JLabel.CENTER);
      sliderLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
      JSlider speedControl = new JSlider();
      speedControl.addChangeListener(
            new ChangeListener() {
               public void stateChanged(ChangeEvent e) {
                  JSlider source = (JSlider)e.getSource();
                  if (!source.getValueIsAdjusting()) {
                      int speed = (int)source.getValue();
                      delay = (int)Math.pow(10, (100 - speed) / 25.0);
                      System.out.println("Speed = " + speed + "; Delay set to " + delay);
                                            
                      /*
                          timer.setDelay(delay);
                          timer.setInitialDelay(delay * 10);
                          if (frozen) startAnimation();
                          */
                  }
              }
            });
      speedControl.setMajorTickSpacing(25);
      speedControl.setMinorTickSpacing(5);
      speedControl.setPaintTicks(true);
      //Create the label table
      Hashtable labelTable = new Hashtable();
      labelTable.put( new Integer( 0 ), new JLabel("Slow") );
      labelTable.put( new Integer( 100 ), new JLabel("Fast") );
      speedControl.setLabelTable( labelTable );
      speedControl.setPaintLabels(true);
      // sliderPanel.add(speedControl);
      
      JPanel controlPanel = new JPanel();
      controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
      controlPanel.add(buttonPanel);
      controlPanel.add(sliderLabel);
      controlPanel.add(speedControl);
      // controlPanel.add(sliderPanel);

      actionLogArea = new JTextArea(22, 50);
      JScrollPane scrollPane = new JScrollPane(actionLogArea); 
      actionLogArea.setEditable(false);
      actionLogArea.append("Action Log:\n");
      actionLogArea.append("-----------\n");

      JPanel leftColPanel = new JPanel();
      leftColPanel.setLayout(new BoxLayout(leftColPanel, BoxLayout.Y_AXIS));
      leftColPanel.add(mapPanel);
      leftColPanel.add(controlPanel);
      Container container = getContentPane();
      container.setLayout(new BoxLayout(container, BoxLayout.X_AXIS));
      container.add(leftColPanel);
      container.add(scrollPane);

      // container.add(mapPanel, BorderLayout.CENTER);
      // container.add(controlPanel, BorderLayout.SOUTH);

      // setSize(5 * world.getWorldSize() + 10, 5 * world.getWorldSize() + 60);
      pack();
      setVisible(true);
   }

   /** Constructor that defaults to not auto-run mode. */
   public PacGUI(PackageWorld world) {
      this(world, false);
   }

   public void drawMap(PackageState state) {
      /*
    if (reset)
      throw new Exception("reset simulator");
       */
      if (dontUpdate)
         return;
      // if (i == 0)
         mapPanel.drawMap(state); 
      /*
      i++;
      if (i >= world.getNumAgents())
         i = 0;
      while(!step && !run) {
      }
      if (!run && i == 0)
         step = false;
         
      else
      
      if (run && i == 0) {
         try {
            Thread.sleep(100);
         }
         catch (InterruptedException ie) {
         }
      }
      */
   }

   /** Returns the delay in msecs as set by the slider. */
   public int getDelay() {
   	// don't delay when the "Complete" button is pressed
      if (!dontUpdate)
      	return delay;
      else
      	return 1;
   }
   
   public boolean dontUpdate() {
      return dontUpdate;
   }
   
   public boolean step() {
      return step;
   }
   
   public void setStep(boolean step) {
      this.step = step;
   }
   
   public boolean run() {
      return run;
   }
   
   public void addToActionLog(String message) {
      actionLogArea.append(" " + message + "\n");
      actionLogArea.setCaretPosition(actionLogArea.getDocument().getLength());
   }
}
