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
 * OrderStore
 *
 * Author  : Joakim Eriksson, Niclas Finne, Sverker Janson
 * Created : Wed May 14 11:01:42 2003
 * Updated : $Date: 2005/06/07 21:35:18 $
 *           $Revision: 1.3 $
 */
package se.sics.tasim.tac03.aw;
import com.botbox.util.ArrayUtils;
import se.sics.tasim.props.OfferBundle;

/**
 * <code>OrderStore</code> contains a collection of orders.
 */
public class OrderStore {

  private int nextOrderID;

  // The added and created orders in due date order
  private Order[] orderList = new Order[512];
  private int orderCount = 0;
  private int activeStartIndex = 0;

  public OrderStore() {
    this(1);
  }

  public OrderStore(int startID) {
    this.nextOrderID = startID;
  }

  /**
   * Returns the order with the specified id.
   *
   * @param orderID the id of the order
   * @return the order with the specified order id or
   * <CODE>null</CODE> if the order was not found
   */
  public synchronized Order getOrder(int orderID) {
    if (orderID >= nextOrderID) {
      // No order id this high has been seen so far
      return null;
    }

    // Search among the active orders first
    for (int i = activeStartIndex; i < orderCount; i++) {
      if (orderList[i].getOrderID() == orderID) {
	return orderList[i];
      }
    }
    // Search among the inactive orders
    for (int i = 0; i < activeStartIndex; i++) {
      if (orderList[i].getOrderID() == orderID) {
	return orderList[i];
      }
    }
    return null;
  }

  /**
   * Returns the active (not delivered or canceled) orders in due date
   * order or <CODE>null</CODE> if no active orders was found.
   */
  public synchronized Order[] getActiveOrders() {
    // Skip past all delivered or cancelled orders in the start to
    // avoid checking them every day
    while ((activeStartIndex < orderCount)
	   && !(orderList[activeStartIndex].isActive())) {
      activeStartIndex++;
    }

    if (activeStartIndex == orderCount) {
      // No active orders
      return null;
    }

    int activeCount = 0;
    Order[] activeOrders = new Order[orderCount - activeStartIndex];
    for (int i = activeStartIndex; i < orderCount; i++) {
      Order order = orderList[i];
      if (order.isActive()) {
	activeOrders[activeCount++] = order;
      }
    }
    if (activeCount < activeOrders.length) {
      activeOrders = (Order[]) ArrayUtils.setSize(activeOrders, activeCount);
    }
    return activeOrders;
  }

  // Package protected because agents should not add orders themselves
  // to this store.
  synchronized void addOrder(Order order) {
    int dueDate = order.getDueDate();
    int index = orderCount;

    // Make sure next generated order id is larger than highest seen order id
    int orderID = order.getOrderID();
    if (orderID >= nextOrderID) {
      nextOrderID = orderID + 1;
    }

    // Reserve space for the new order
    if (orderCount == orderList.length) {
      // Perhaps the inactive orders should be removed? FIX THIS!!!
      orderList = (Order[]) ArrayUtils.setSize(orderList, orderCount + 256);
    }

    // Insert the new order to have the orders sorted in due date order
    Order previousOrder;
    while ((index > activeStartIndex) &&
	   ((previousOrder = orderList[index - 1])
	    .getDueDate() > dueDate)) {
      orderList[index] = previousOrder;
      index--;
    }
    orderList[index] = order;
    orderCount++;
  }

  // Package protected because agents should not add orders themselves
  // to this container
  synchronized Order createOrder(String agentAddress, OfferBundle offers,
				 int offerIndex, int productID) {
    int orderID = nextOrderID++;
    Order order = new Order(agentAddress, orderID,
			    offers, offerIndex, productID);
    addOrder(order);
    return order;
  }

} // OrderStore
