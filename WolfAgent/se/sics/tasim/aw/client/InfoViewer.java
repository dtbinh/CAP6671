/**
 * TAC Supply Chain Management Simulator
 * http://www.sics.se/tac/    tac-dev@sics.se
 *
 * Copyright (c) 2001-2003 SICS AB. All rights reserved.
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
 * InfoViewer
 *
 * Author  : Joakim Eriksson, Niclas Finne, Sverker Janson
 * Created : Thu Feb 20 22:31:22 2003
 * Updated : $Date: 2003/02/21 13:46:56 $
 *           $Revision: 1.2 $
 * Purpose :
 *
 */
package se.sics.tasim.aw.client;
import javax.swing.JComponent;
import se.sics.isl.transport.Transportable;

public abstract class InfoViewer {

  public abstract void init(String agentName);

  public abstract JComponent getComponent();

  public abstract void messageSent(String receiver, Transportable content);

  public abstract void messageReceived(String sender, Transportable content);

} // InfoViewer
