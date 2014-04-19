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
 * Order
 *
 * Author  : Joakim Eriksson, Niclas Finne, Sverker Janson
 * Created : Wed May 07 12:34:00 2003
 * Updated : $Date: 2005/06/07 21:29:41 $
 *           $Revision: 1.3 $
 */

package se.sics.tasim.tac03.aw;
import se.sics.tasim.props.OfferBundle;
import se.sics.tasim.props.RFQBundle;

/**
 * <code>Order</code> is a representation of an order.
 */
public class Order {

  private static final int DELIVERED = 1;
  private static final int CANCELED = 2;

  private String otherAgent;
  private int orderID = -1;
  private int offerID;
  private int rfqID;
  private int productID;
  private int quantity;
  private int dueDate;
  private int penalty;
  private int unitPrice;

  private int flags = 0;
  private int activeFlag = 0;

  public Order(String otherAgent, int offerID,
	       RFQBundle rfq, int rfqIndex, int unitPrice) {
    this.otherAgent = otherAgent;
    this.offerID = offerID;
    this.rfqID = rfq.getRFQID(rfqIndex);
    this.productID = rfq.getProductID(rfqIndex);
    this.quantity = rfq.getQuantity(rfqIndex);
    this.dueDate = rfq.getDueDate(rfqIndex);
    this.penalty = rfq.getPenalty(rfqIndex);
    this.unitPrice = unitPrice;
  }

  public Order(String otherAgent, int orderID,
	       OfferBundle offers, int offerIndex,
	       int productID) {
    this.otherAgent = otherAgent;
    this.orderID = orderID;

    this.rfqID = offers.getRFQID(offerIndex);
    this.productID = productID;
    this.penalty = 0;

    this.offerID = offers.getOfferID(offerIndex);
    this.quantity = offers.getQuantity(offerIndex);
    this.dueDate = offers.getDueDate(offerIndex);
    this.unitPrice = offers.getUnitPrice(offerIndex);
  }

  public Order(String otherAgent, int orderID,
	       int productID, int quantity,
	       int unitPrice, int dueDate, int penalty) {
    this.otherAgent = otherAgent;
    this.orderID = orderID;

    this.rfqID = -1;
    this.offerID = -1;

    this.productID = productID;
    this.quantity = quantity;
    this.unitPrice = unitPrice;
    this.dueDate = dueDate;
    this.penalty = penalty;
  }

  /**
   * Returns true if this preliminary order (offer) has been accepted
   * and false otherwise (used internally by SCMAgent).
   */
  boolean isOrdered() {
    return orderID >= 0;
  }

  /**
   * Sets the order id of this order (used internally by SCMAgent) and
   * marks it as accepted.
   *
   * @throws IllegalStateException if the order ID already been set
   */
  void setOrdered(int orderID) {
    if (this.orderID >= 0) {
      throw new IllegalStateException("already ordered");
    }
    this.orderID = orderID;
  }

  /**
   * Returns true if this order is active (not delivered or canceled)
   * and false otherwise.
   */
  public boolean isActive() {
    return activeFlag == 0;
  }

  /**
   * Returns true if this order has been delivered and false otherwise.
   */
  public boolean isDelivered() {
    return (activeFlag & DELIVERED) != 0;
  }

  /**
   * Marks this order as delivered.
   */
  public void setDelivered() {
    this.activeFlag |= DELIVERED;
  }

  /**
   * Returns true if this order has been canceled and false otherwise.
   */
  public boolean isCanceled() {
    return (activeFlag & CANCELED) != 0;
  }

  /**
   * Marks this order as canceled.
   */
  public void setCanceled() {
    this.activeFlag |= CANCELED;
  }

  /**
   * Return the address of the other part (buyer/seller) of this order.
   */
  public String getOtherAgent() {
    return otherAgent;
  }

  /**
   * Returns the id of this order.
   */
  public int getOrderID() {
    return orderID;
  }

  /**
   * Returns the id of the accepted offer or -1 if the offer is not known.
   */
  public int getOfferID() {
    return offerID;
  }

  /**
   * Returns the id of the RFQ that was ordered or -1 if the RFQ is not known.
   */
  public int getRFQID() {
    return rfqID;
  }

  /**
   * Returns the product in this order.
   */
  public int getProductID() {
    return productID;
  }

  /**
   * Returns the quantity of products in this order.
   */
  public int getQuantity() {
    return quantity;
  }

  /**
   * Returns the latest delivery date of this order.
   */
  public int getDueDate() {
    return dueDate;
  }

  /**
   * Returns the penalty for late deliveries for this order.
   */
  public int getPenalty() {
    return penalty;
  }

  /**
   * Returns the unit price in this order.
   */
  public int getUnitPrice() {
    return unitPrice;
  }

  /**
   * Returns the user flags in this order. The user flags are free to
   * be used for any purpose by agent implementations.
   */
  public int getFlags() {
    return flags;
  }

  /**
   * Sets the user flags in this order. The user flags are free to be
   * used for any purpose by agent implementations.
   */
  public void setFlags(int flags) {
    this.flags = flags;
  }

} // Order
