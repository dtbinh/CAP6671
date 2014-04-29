/**
 * TAC Supply Chain Management Simulator
 * http://www.sics.se/tac/    tac-dev@sics.se
 *
 * Copyright (c) 2001-2005 SICS AB. All rights reserved.
 *
 * SICS grants you the right to use, modify, and redistribute this
 * software for noncommercial purposes, on the conditions that you:
 * (1) retain the original headers, including the copyright notice and
 * this text, (2) clearly document the difference between any derived
 * software and the original, and (3) acknowledge your use of this
 * software in pertaining publications and reports.  SICS provides
 * this software "as is", without any warranty of any kind.  IN NO
 * EVENT SHALL SICS BE LIABLE FOR ANY DIRECT, SPECIAL OR INDIRECT,
 * PUNITIVE, INCIDENTAL OR CONSEQUENTIAL LOSSES OR DAMAGES ARISING OUT
 * OF THE USE OF THE SOFTWARE.
 *
 * -----------------------------------------------------------------
 *
 * StatusInfo
 *
 * Author  : Joakim Eriksson, Niclas Finne, Sverker Janson
 * Created : Fri Feb 21 13:34:46 2003
 * Updated : $Date: 2005/06/09 17:48:35 $
 *           $Revision: 1.18 $
 */
package se.sics.tasim.aw.client;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import se.sics.isl.transport.Transportable;
import se.sics.tasim.props.ActiveOrders;
import se.sics.tasim.props.AdminContent;
import se.sics.tasim.props.SimulationStatus;
import se.sics.tasim.props.StartInfo;

public class StatusInfo extends InfoViewer implements ActionListener {

  private SimClient simClient;
  private InfoViewer child;
  private JPanel mainPanel;
  private JLabel simLabel;
  private JLabel dayLabel;
  private int numberOfDays = 0;

  private JButton joinSimButton;
  private JButton exitButton;

  private Timer timer = new Timer(1000, this);
  private JLabel serverTimeLabel;

  private boolean isSimulationRunning = false;
  private int simulationID;
  private long simulationEndTime = 0L;

  private long nextSimulationStarts = 0L;

  public StatusInfo(SimClient simClient, InfoViewer child) {
    this.simClient = simClient;
    this.child = child;
  }



  // -------------------------------------------------------------------
  //  InfoViewer interface
  // -------------------------------------------------------------------

  public void init(String agentName) {
    child.init(agentName);

    mainPanel = new JPanel(new BorderLayout());
    mainPanel.setBackground(Color.white);

    JPanel topPanel = new JPanel(new BorderLayout());
    topPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
    topPanel.setBackground(Color.white);
    dayLabel = new JLabel("Day: 0 / 0");
    topPanel.add(dayLabel, BorderLayout.EAST);
    serverTimeLabel = new JLabel();
    topPanel.add(serverTimeLabel, BorderLayout.WEST);

    simLabel = new JLabel("Waiting for next simulation...");
    topPanel.add(simLabel, BorderLayout.SOUTH);

    mainPanel.add(topPanel, BorderLayout.NORTH);
    mainPanel.add(child.getComponent(), BorderLayout.CENTER);

    JPanel buttonPanel = new JPanel();
    buttonPanel.add(joinSimButton = new JButton("Join Simulation"));
    joinSimButton.addActionListener(this);
    buttonPanel.add(exitButton = new JButton("Exit Agent"));
    exitButton.addActionListener(this);
    mainPanel.add(buttonPanel, BorderLayout.SOUTH);

    long currentTime = simClient.getServerTime();
    updateTime(currentTime);

    // Synchronize the timer at the seconds and start it
    timer.setInitialDelay((int)
			  ((currentTime / 1000) * 1000 + 1000 - currentTime));
    timer.setRepeats(true);
    timer.start();
  }

  public JComponent getComponent() {
    return mainPanel;
  }

  public void messageSent(String receiver, Transportable content) {
    child.messageSent(receiver, content);
  }

  public void messageReceived(String sender, Transportable content) {
    if (content instanceof SimulationStatus) {
      SimulationStatus status = (SimulationStatus) content;
      int currentDay = status.getCurrentDate();
      if (currentDay < numberOfDays) {
	setButtons(null, "Day: " + currentDay + " / " + (numberOfDays - 1), 0);
      }

    } else if (content instanceof ActiveOrders) {
      ActiveOrders active = (ActiveOrders) content;
      int currentDay = active.getCurrentDate();
      if (currentDay > 0 && currentDay < numberOfDays) {
	setButtons(null, "Day: " + currentDay + " / " + (numberOfDays - 1), 0);
      }

    } else if (content instanceof StartInfo) {
      startSimulation((StartInfo) content);

    } else if (content instanceof AdminContent) {
      AdminContent adminContent = (AdminContent) content;
      int type = adminContent.getType();
      if (adminContent.isError()) {
	// Ignore errors
      } else if (type == AdminContent.JOIN_SIMULATION
		 || type == AdminContent.NEXT_SIMULATION) {
	long startTime = adminContent.getAttributeAsLong("startTime", 0L);
	if (startTime > 0 && !isSimulationRunning) {
	  int simulationID = adminContent.getAttributeAsInt("simulation", -1);
	  StringBuffer sb = new StringBuffer();
	  if (startTime > simClient.getServerTime()) {
	    nextSimulationStarts = startTime;
	    sb.append("Next simulation");
	    if (simulationID >= 0) {
	      sb.append(' ').append(simulationID);
	    }
	    sb.append(" starts at ");
	    appendTime(sb, nextSimulationStarts);
	  } else {
	    sb.append("Simulation ");
	    if (simulationID >= 0) {
	      sb.append(' ').append(simulationID);
	    }
	    sb.append(" already started");
	  }
	  setButtons(sb.toString(), null, 0);
	}
      }
    }

    child.messageReceived(sender, content);
  }

  private synchronized void startSimulation(StartInfo info) {
    numberOfDays = info.getNumberOfDays();
    simulationID = info.getSimulationID();
    simulationEndTime = info.getEndTime();

    isSimulationRunning = true;
    nextSimulationStarts = 0L;

    setButtons("Playing in simulation " + simulationID,
	       "Day: 0 / " + (numberOfDays - 1),
	       -1);
  }

  // Also used for notification from the SimClient
  void stopSimulation() {
    boolean stopped = isSimulationRunning;
    if (stopped) {
      synchronized (this) {
	if (stopped = isSimulationRunning) {
	  // Simulation has stopped
	  String message;
	  if (nextSimulationStarts > simClient.getServerTime()) {
	    StringBuffer sb = new StringBuffer();
	    sb.append("Next simulation starts at ");
	    appendTime(sb, nextSimulationStarts);
	    message = sb.toString();
	  } else {
	    message = "Simulation " + simulationID + " has finished";
	  }
	  setButtons(message, null, 1);
	  isSimulationRunning = false;
	}
      }
    }
  }

  private void setButtons(final String simLabelText,
			  final String dayLabelText,
			  final int joinSimButtonEnabled) {
    SwingUtilities.invokeLater(new Runnable() {
	public void run() {
	  if (joinSimButtonEnabled != 0) {
	    joinSimButton.setEnabled(joinSimButtonEnabled > 0);
	  }
	  if (simLabelText != null) {
	    simLabel.setText(simLabelText);
	  }
	  if (dayLabelText != null) {
	    dayLabel.setText(dayLabelText);
	  }
	}
      });
  }


  // -------------------------------------------------------------------
  //  Server time handling
  // -------------------------------------------------------------------

  // Only called from init and with the AWT dispatch thread
  private void updateTime(long serverTime) {
    StringBuffer sb = new StringBuffer();
    sb.append("Server time: ");
    appendTime(sb, serverTime);
    serverTimeLabel.setText(sb.toString());

    if (isSimulationRunning && (serverTime >= simulationEndTime)) {
      joinSimButton.setEnabled(true);
//       stopSimulation();
    }
  }

  private StringBuffer appendTime(StringBuffer sb, long time) {
    time /= 1000;
    long sek = time % 60;
    long minutes = (time / 60) % 60;
    long hours = (time / 3600) % 24;
    if (hours < 10) sb.append('0');
    sb.append(hours).append(':');
    if (minutes < 10) sb.append('0');
    sb.append(minutes).append(':');
    if (sek < 10) sb.append('0');
    sb.append(sek);
    return sb;
  }



  // -------------------------------------------------------------------
  //  ActionListener
  // -------------------------------------------------------------------

  public void actionPerformed(ActionEvent event) {
    Object source = event.getSource();
    if (source == timer) {
      long serverTime = simClient.getServerTime();
      updateTime(serverTime);
    } else if (source == joinSimButton) {
      simClient.requestJoinSimulation();
    } else if (source == exitButton) {
      simClient.requestQuit();
    }
  }

} // StatusInfo
