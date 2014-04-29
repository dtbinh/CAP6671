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
 * MessageSender
 *
 * Author  : Joakim Eriksson, Niclas Finne, Sverker Janson
 * Created : Thu Jun 12 13:48:31 2003
 * Updated : $Date: 2003/06/12 14:01:21 $
 *           $Revision: 1.2 $
 */
package se.sics.tasim.aw.client;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.botbox.util.ArrayQueue;
import se.sics.tasim.aw.Message;

/**
 */
public class MessageSender extends Thread {

  private static final Logger log =
    Logger.getLogger(MessageSender.class.getName());

  private final ServerConnection connection;
  private ArrayQueue messageQueue = new ArrayQueue();
  private boolean isClosed = false;

  public MessageSender(ServerConnection connection, String name) {
    super(name);
    this.connection = connection;
    start();
  }

  public boolean isClosed() {
    return isClosed;
  }

  public synchronized void close() {
    if (!isClosed) {
      this.isClosed = true;
      messageQueue.clear();
      messageQueue.add(null);
      notify();
    }
  }

  public synchronized boolean addMessage(Message message) {
    if (isClosed) {
      return false;
    }
    messageQueue.add(message);
    notify();
    return true;
  }

  private synchronized Message nextMessage() {
    while (messageQueue.size() == 0) {
      try {
	wait();
      } catch (InterruptedException e) {
      }
    }
    return (Message) messageQueue.remove(0);
  }



  // -------------------------------------------------------------------
  // Message sending handling
  // -------------------------------------------------------------------

  public void run() {
    do {
      Message msg = null;
      try {
	msg = nextMessage();
	if (msg != null) {
	  connection.deliverMessage(msg);
	}

      } catch (ThreadDeath e) {
	log.log(Level.SEVERE, "message thread died", e);
	throw e;

      } catch (Throwable e) {
	log.log(Level.SEVERE, "could not handle message " + msg, e);
      }
    } while (!isClosed);
  }

} // MessageSender
