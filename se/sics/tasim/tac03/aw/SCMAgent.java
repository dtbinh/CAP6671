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
 * SCMAgent
 *
 * Author  : Joakim Eriksson, Niclas Finne, Sverker Janson
 * Created : Mon May 12 17:32:36 2003
 * Updated : $Date: 2005/06/08 20:31:24 $
 *           $Revision: 1.12 $
 */
package se.sics.tasim.tac03.aw;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.logging.Logger;

import com.botbox.util.ArrayUtils;
import se.sics.isl.transport.Transportable;
import se.sics.tasim.aw.Agent;
import se.sics.tasim.aw.Message;
import se.sics.tasim.props.ActiveOrders;
import se.sics.tasim.props.BOMBundle;
import se.sics.tasim.props.BankStatus;
import se.sics.tasim.props.ComponentCatalog;
import se.sics.tasim.props.DeliveryNotice;
import se.sics.tasim.props.DeliverySchedule;
import se.sics.tasim.props.FactoryStatus;
import se.sics.tasim.props.InventoryStatus;
import se.sics.tasim.props.MarketReport;
import se.sics.tasim.props.OfferBundle;
import se.sics.tasim.props.OrderBundle;
import se.sics.tasim.props.PriceReport;
import se.sics.tasim.props.ProductionSchedule;
import se.sics.tasim.props.RFQBundle;
import se.sics.tasim.props.SimulationStatus;
import se.sics.tasim.props.StartInfo;

/**
 * The abstract class <code>SCMAgent</code> extends <code>{@link
 * Agent}</code> with support functionality for TAC
 * SCM. <code>SCMAgent</code> provides the following features for the
 * agent developer:
 *
 * <ul>
 * <li> APIs for accessing game information.
 * <li> Automatic bookkeeping of RFQs, orders, and inventory.
 * <li> Support for creating production schedules based on inventory and capacity.
 * <li> Provides an easier API for handling messages.
 * </ul>
 */
public abstract class SCMAgent extends Agent {

  private static final Logger log =
    Logger.getLogger(SCMAgent.class.getName());

  private Object lock = new Object();

  private boolean isValidating = true;

  // Simulation information
  private StartInfo startInfo;
  private BOMBundle bomBundle;
  private ComponentCatalog catalog;
  private String factoryAddress;
  private int factoryCapacity;
  private int daysBeforeVoid;
  private boolean isInitialized;

  private int currentDate = 0;
  private boolean isAwaitingNewDay = false;

  private InventoryStatus currentInventory;
  private BankStatus currentBankStatus;

  // Customer handling
  private String customerAddress;
  private RFQBundle currentCustomerRFQs;
  private ArrayList customerOffers = new ArrayList();
  private OfferBundle customerOfferBundle;
  private OrderStore customerOrders = new OrderStore();

  // Supplier handling
  private Hashtable supplierRFQTable = new Hashtable();
  private RFQStore supplierRFQs = new RFQStore(1);

  private Hashtable supplierOrderTable = new Hashtable();
  private OrderStore supplierOrders = new OrderStore(1);

  // Factory schedules
  private ProductionSchedule productionSchedule;
  private DeliverySchedule deliverySchedule;

  // Next day's inventory
  private InventoryStatus nextDayInventory = new InventoryStatus();
  private ProductionSchedule lastDayProduction;
  private ArrayList supplierDeliveries = new ArrayList();
  private int freeFactoryCapacity = 0;


  // -------------------------------------------------------------------
  //  Setup handling
  // -------------------------------------------------------------------

  public SCMAgent() {
  }

  /**
   * Called when a game/simulation is about to start.
   */
  protected final void simulationSetup() {
  }

  /**
   * Called when a game/simulation is finished and the agent should
   * free its resources.
   */
  protected final void simulationFinished() {
    isInitialized = false;
    simulationEnded();
  }

  private void checkInitialized() {
    if (!isInitialized
	&& this.bomBundle != null
	&& this.catalog != null
	&& this.startInfo != null
	&& this.factoryAddress != null) {
      isInitialized = true;
      simulationStarted();
    }
  }


  // -------------------------------------------------------------------
  //  Message handling
  // -------------------------------------------------------------------

  /**
   * Called when a message has been received. This method should not
   * be overridden.
   *
   * @param message the received message
   */
  protected void messageReceived(Message message) {
    Transportable content = message.getContent();

    if (isAwaitingNewDay) {
      // This message is the first message on a new day
      isAwaitingNewDay = false;
      currentDate++;
    }

    if (!isInitialized) {
      if (content instanceof ComponentCatalog) {
	this.catalog = (ComponentCatalog) content;

      } else if (content instanceof BOMBundle) {
	this.bomBundle = (BOMBundle) content;

      } else if (content instanceof StartInfo) {
	// Start information for manufacturers
	this.startInfo = (StartInfo) content;
	this.daysBeforeVoid =
	  startInfo.getAttributeAsInt("customer.daysBeforeVoid",
				      this.daysBeforeVoid);
	this.factoryAddress =
	  startInfo.getAttribute("factory.address");
	this.factoryCapacity =
	  startInfo.getAttributeAsInt("factory.capacity", 2000);

      } else if (content instanceof ActiveOrders) {
	// Report of active orders from the server
	ActiveOrders activeOrders = (ActiveOrders) content;
	if (activeOrders.getCurrentDate() > currentDate) {
	  currentDate = activeOrders.getCurrentDate();
	}

	int numCustomerOrders = 0;
	int numSupplierOrders = 0;
	for (int i = 0, n = activeOrders.getCustomerOrderCount(); i < n; i++) {
	  int orderID = activeOrders.getCustomerOrderID(i);
	  if (customerOrders.getOrder(orderID) == null) {
	    // The order was not already known
	    Order order = new Order(activeOrders.getCustomerAddress(i),
				    orderID,
				    activeOrders.getCustomerProductID(i),
				    activeOrders.getCustomerQuantity(i),
				    activeOrders.getCustomerUnitPrice(i),
				    activeOrders.getCustomerDueDate(i),
				    activeOrders.getCustomerPenalty(i));
	    customerOrders.addOrder(order);
	    numCustomerOrders++;
	  }
	}

	for (int i = 0, n = activeOrders.getSupplierOrderCount(); i < n; i++) {
	  int orderID = activeOrders.getSupplierOrderID(i);
	  if (supplierOrders.getOrder(orderID) == null) {
	    // The order was not already known
	    Order order = new Order(activeOrders.getSupplierAddress(i),
				    orderID,
				    activeOrders.getSupplierProductID(i),
				    activeOrders.getSupplierQuantity(i),
				    activeOrders.getSupplierUnitPrice(i),
				    activeOrders.getSupplierDueDate(i),
				    activeOrders.getSupplierPenalty(i));
	    supplierOrders.addOrder(order);
	    numSupplierOrders++;
	  }
	}
	if (numCustomerOrders > 0 || numSupplierOrders > 0) {
	  log.warning("restored " + numCustomerOrders
		      + " active customer orders and "
		      + numSupplierOrders + " supplier orders");
	}

      } else {
	// Ignore any other messages until all startup information has
	// been received
	log.warning("ignoring message '" + content.getTransportName()
		    + "' when awaiting initialization");
      }

      // Check if we have received all startup information
      checkInitialized();

    } else if (content instanceof OfferBundle) {
      OfferBundle offerBundle = (OfferBundle) content;
      // In TAC SCM only suppliers send offers to manufacturers
      handleSupplierOffers(message.getSender(), offerBundle);

    } else if (content instanceof RFQBundle) {
      RFQBundle rfqBundle = (RFQBundle) content;

      this.customerAddress = message.getSender();
      // Any offers from the previous day are no longer valid
      this.customerOfferBundle = null;
      this.currentCustomerRFQs = rfqBundle;
      // In TAC SCM only customers send RFQs to manufacturers
      handleCustomerRFQs(rfqBundle);

    } else if (content instanceof OrderBundle) {
      OrderBundle orderBundle = (OrderBundle) content;
      // In TAC SCM only customers send orders to manufacturers
      Order[] orders = addCustomerOrders(orderBundle);
      if (orders != null) {
	handleCustomerOrders(orders);
      }

    } else if (content instanceof DeliveryNotice) {
      DeliveryNotice notice = (DeliveryNotice) content;
      // In TAC SCM only suppliers delivers to manufacturers
      synchronized (lock) {
	// Mark the delivered orders as delivered.
	for (int i = 0, n = notice.size(); i < n; i++) {
	  Order order = supplierOrders.getOrder(notice.getOrderID(i));
	  if (order != null) {
	    order.setDelivered();
	  } else {
	    log.warning("received delivery of unknown order "
			+ notice.getOrderID(i));
	  }
	}
	supplierDeliveries.add(notice);
      }
      handleSupplierDelivery(notice);

    } else if (content instanceof FactoryStatus) {
      FactoryStatus status = (FactoryStatus) content;
      this.currentInventory = status;
      handleFactoryStatus(status);

    } else if (content instanceof BankStatus) {
      BankStatus status = (BankStatus) content;
      this.currentBankStatus = status;
      handleBankStatus(status);

    } else if (content instanceof SimulationStatus) {
      SimulationStatus status = (SimulationStatus) content;
      currentDate = status.getCurrentDate();

      synchronized (lock) {
	// Time to generate next day inventory
	nextDayInventory = new InventoryStatus(currentInventory);
	// The deliveries that arrives today are available for next days
	// production
	if (supplierDeliveries.size() > 0) {
	  for (int i = 0, n = supplierDeliveries.size(); i < n; i++) {
	    DeliveryNotice notice = (DeliveryNotice) supplierDeliveries.get(i);

	    for (int j = 0, m = notice.size(); j < m; j++) {
	      nextDayInventory.addInventory(notice.getProductID(j),
					    notice.getQuantity(j));
	    }
	  }
	  supplierDeliveries.clear();
	}
	if (lastDayProduction != null) {
	  // Add the production for the current day to the inventory for
	  // tomorrow
	  for (int i = 0, n = lastDayProduction.size(); i < n; i++) {
	    nextDayInventory.addInventory(lastDayProduction.getProductID(i),
					  lastDayProduction.getQuantity(i));
	  }
	  lastDayProduction = null;
	}
	this.freeFactoryCapacity = factoryCapacity;
      }

      // No more messages will be received from the server until next day
      isAwaitingNewDay = true;

      handleSimulationStatus(status);

    } else if (content instanceof PriceReport) {
      handlePriceReport((PriceReport) content);

    } else if (content instanceof MarketReport) {
      handleMarketReport((MarketReport) content);

    } else {
      log.warning("ignoring unhandled message '" + content.getTransportName()
		  + '\'');
    }
  }


  // -------------------------------------------------------------------
  //  Information access
  // -------------------------------------------------------------------

  /**
   * Returns the start info for current game/simulation.
   */
  protected StartInfo getStartInfo() {
    return startInfo;
  }

  /**
   * Returns the BOM bundle for current game/simulation.
   */
  protected BOMBundle getBOMBundle() {
    return bomBundle;
  }

  /**
   * Returns the component catalog for current game/simulation.
   */
  protected ComponentCatalog getComponentCatalog() {
    return catalog;
  }

  /**
   * Returns the address for the manufacturer's factory.
   */
  protected String getFactoryAddress() {
    return factoryAddress;
  }

  /**
   * Returns the capacity per day for the manufacturer's factory.
   */
  protected int getFactoryCapacity() {
    return factoryCapacity;
  }

  /**
   * Returns the number of days a delivery can be late before the
   * customer cancels the order. In TAC SCM only customers can
   * cancel orders.
   */
  protected int getDaysBeforeVoid() {
    return daysBeforeVoid;
  }

  /**
   * Returns the current date in the current game/simulation.
   */
  protected int getCurrentDate() {
    return currentDate;
  }

  /**
   * Returns the last received bank status.  The bank status is
   * received each day before the simulation status.
   */
  protected BankStatus getCurrentBankStatus() {
    return currentBankStatus;
  }

  /**
   * Returns the last received inventory.  The inventory is
   * received each day before the simulation status.
   */
  protected InventoryStatus getCurrentInventory() {
    return currentInventory;
  }

  /**
   * Returns the calculated inventory for next day.
   *
   * The inventory for next day includes components that will be
   * available next day (not in the this day's inventory) and also the
   * products that are produced this day. It is used to bookkeep the
   * production and deliveries for next day to ease the generation of
   * delivery and production schedules.
   */
  protected InventoryStatus getInventoryForNextDay() {
    return nextDayInventory;
  }

  protected void reserveInventoryForNextDay(int productID, int quantity) {
    if (quantity == 0) {
      return;
    }

    synchronized (lock) {
      int existingInventory =
	nextDayInventory.getInventoryQuantity(productID);
      if (quantity < 0) {
	throw new IllegalArgumentException("can not add to inventory");
      }
      if (quantity > existingInventory) {
	throw new IllegalArgumentException("can not reserve more than "
					   + "exists in inventory: "
					   + quantity + '>'
					   + existingInventory);
      }
      nextDayInventory.addInventory(productID, -quantity);
    }
  }

  /**
   * Returns the calculated free factory capacity after removing the
   * capacity needed for the next day's production.
   */
  protected int getFreeFactoryCapacityForNextDay() {
    return freeFactoryCapacity;
  }


  // -------------------------------------------------------------------
  //  Help methods for customer handling
  // -------------------------------------------------------------------

  /**
   * Returns the last received RFQs from the customers.
   */
  protected RFQBundle getCustomerRFQs() {
    return currentCustomerRFQs;
  }

  /**
   * Returns the customer order store. Information about the active
   * customer orders (not yet delivered) can be found in the customer
   * order store.
   */
  protected OrderStore getCustomerOrders() {
    return customerOrders;
  }

  /**
   * Adds an offer to be sent to the customers in response to a
   * RFQ.
   *
   * <p>Note: to send the offers to the customers, the
   * <code>sendCustomerOffers</code> method must be called after all
   * offers been added. This must be done before the day ends!
   *
   * @param rfqBundle the RFQ bundle from the customers
   * @param rfqIndex the index of the RFQ to bid on
   * @param offeredUnitPrice the offered price per unit
   * @return the allocated offer ID
   * @see #sendCustomerOffers
   */
  protected int addCustomerOffer(RFQBundle rfqBundle, int rfqIndex,
				 int offeredUnitPrice) {
    if (customerAddress == null) {
      throw new IllegalStateException("no customer RFQ received");
    }

    synchronized (lock) {
      OfferBundle offers = this.customerOfferBundle;
      if (offers == null) {
	offers = this.customerOfferBundle = new OfferBundle();
      }
      int offerID = customerOffers.size();
      Order offer = new Order(customerAddress, offerID,
			      rfqBundle, rfqIndex, offeredUnitPrice);
      // add offer
      customerOffers.add(offer);
      offers.addOffer(offerID, rfqBundle, rfqIndex, offeredUnitPrice);
      return offerID;
    }
  }

  /**
   * Sends any pending offers to the customers.
   */
  protected void sendCustomerOffers() {
    OfferBundle offers;
    synchronized (lock) {
      offers = this.customerOfferBundle;
      this.customerOfferBundle = null;
    }
    if (offers != null && offers.size() > 0) {
      // In TAC SCM one customer agent represents all customers and
      // all customer offers will be sent to this agent. In other
      // words, there is only one customer address to sent to.
      sendMessage(customerAddress, offers);
    }
  }

  /**
   * Adds the received customer orders to the customer order store.
   *
   * @param orders the received orders from the customers
   * @return a list with the new orders or <CODE>null</CODE> if no new
   * orders was found in the order bundle
   */
  private Order[] addCustomerOrders(OrderBundle orders) {
    synchronized (lock) {
      Order[] newOrders = new Order[orders.size()];
      int newOrderCount = 0;

      for (int i = 0, n = orders.size(); i < n; i++) {
	int orderID = orders.getOrderID(i);
	int offerID = orders.getOfferID(i);
	Order offer;
	if ((offerID >= customerOffers.size())
	    || ((offer = (Order) customerOffers.get(offerID)).isOrdered())) {
	  log.severe("illegal customer order " + orderID
		     + " for offer " + offerID);

	} else {
	  offer.setOrdered(orderID);
	  customerOrders.addOrder(offer);
	  newOrders[newOrderCount++] = offer;
	}
      }

      if (newOrderCount < newOrders.length) {
	newOrders = (Order[]) ArrayUtils.setSize(newOrders, newOrderCount);
      }
      return newOrders;
    }
  }


  // -------------------------------------------------------------------
  //  Help methods for supplier handling
  // -------------------------------------------------------------------

  /**
   * Returns the supplier RFQ store.
   */
  protected RFQStore getSupplierRFQs() {
    return supplierRFQs;
  }

  /**
   * Returns the supplier order store.
   */
  protected OrderStore getSupplierOrders() {
    return supplierOrders;
  }

  /**
   * Adds a RFQ to be sent to the specified supplier.
   *
   * <p>Note: to send the RFQs to the suppliers, the
   * <code>sendSupplierRFQs</code> method must be called after all
   * RFQs been added. This must be done before the day ends!
   *
   * @param supplierAddress the address of the supplier
   * @param productID the requested component
   * @param quantity the requested quantity
   * @param dueDate the requested delivery date
   * @return the allocated RFQ id
   * @see #addSupplierRFQ(String,int,int,int,int)
   * @see #sendSupplierRFQs
   * @deprecated
   *   Replaced by <code>addSupplierRFQ(String,int,int,int,int)</code>
   */
  protected int addSupplierRFQ(String supplierAddress,
			       int productID, int quantity,
			       int dueDate) {
    return addSupplierRFQ(supplierAddress, productID, quantity, 0, dueDate);
  }

  /**
   * Adds a RFQ to be sent to the specified supplier.
   *
   * <p>Note: to send the RFQs to the suppliers, the
   * <code>sendSupplierRFQs</code> method must be called after all
   * RFQs been added. This must be done before the day ends!
   *
   * @param supplierAddress the address of the supplier
   * @param productID the requested component
   * @param quantity the requested quantity
   * @param reservePricePerUnit the reservation price or 0 for no
   * price constraints
   * @param dueDate the requested delivery date
   * @return the allocated RFQ id
   * @see #sendSupplierRFQs
   */
  protected int addSupplierRFQ(String supplierAddress,
			       int productID, int quantity,
			       int reservePricePerUnit,
			       int dueDate) {
    synchronized (lock) {
      int rfqID = supplierRFQs.addRFQ(supplierAddress, productID,
				      quantity, dueDate, currentDate);

      RFQBundle bundle = (RFQBundle) supplierRFQTable.get(supplierAddress);
      if (bundle == null) {
	bundle = new RFQBundle();
	supplierRFQTable.put(supplierAddress, bundle);
      }
      bundle.addRFQ(rfqID, productID, quantity, reservePricePerUnit, dueDate,
		    0);
      return rfqID;
    }
  }

  /**
   * Sends any pending RFQs to the suppliers.
   */
  protected void sendSupplierRFQs() {
    if (supplierRFQTable.size() > 0) {
      synchronized (supplierRFQTable) {
	Enumeration keyIterator = supplierRFQTable.keys();
	while (keyIterator.hasMoreElements()) {
	  String supplierAddress = (String) keyIterator.nextElement();
	  RFQBundle rfqBundle = (RFQBundle)
	    supplierRFQTable.get(supplierAddress);
	  sendMessage(supplierAddress, rfqBundle);
	}
	supplierRFQTable.clear();
      }
    }
  }

  /**
   * Adds an order to be sent to a supplier in response to an offer.
   *
   * <p>Note: to send the orders to the suppliers, the
   * <code>sendSupplierOrders</code> method must be called after all
   * orders been added. This must be done before the day ends!
   *
   * @param supplierAddress the supplier to order from
   * @param offers the offer bundle from the supplier
   * @param offerIndex the index of the offer in the bundle
   * @return the new order
   * @see #sendSupplierOrders
   */
  protected Order addSupplierOrder(String supplierAddress,
				   OfferBundle offers, int offerIndex) {
    synchronized (lock) {
      int rfqID = offers.getRFQID(offerIndex);
      int productID = supplierRFQs.getProductID(rfqID);
      Order order = supplierOrders.createOrder(supplierAddress,
					       offers, offerIndex,
					       productID);
      int orderID = order.getOrderID();

      OrderBundle bundle = (OrderBundle)
	supplierOrderTable.get(supplierAddress);
      if (bundle == null) {
	bundle = new OrderBundle();
	supplierOrderTable.put(supplierAddress, bundle);
      }
      bundle.addOrder(orderID, offers.getOfferID(offerIndex));
      return order;
    }
  }

  /**
   * Sends any pending orders to the suppliers.
   */
  protected void sendSupplierOrders() {
    if (supplierOrderTable.size() > 0) {
      synchronized (supplierOrderTable) {
	Enumeration keyIterator = supplierOrderTable.keys();
	while (keyIterator.hasMoreElements()) {
	  String supplierAddress = (String) keyIterator.nextElement();
	  OrderBundle orderBundle = (OrderBundle)
	    supplierOrderTable.get(supplierAddress);
	  sendMessage(supplierAddress, orderBundle);
	}
	supplierOrderTable.clear();
      }
    }
  }


  // -------------------------------------------------------------------
  // Help methods for factory handling. These methods use the next day
  // inventory to keep track what can be produced/delivered or not
  // -------------------------------------------------------------------

  /**
   * Adds a delivery request to the delivery schedule for next day if
   * possible.
   *
   * This method is using the calculated inventory for next day to
   * determine if the delivery can be made or not. If it can be made
   * the delivered products are removed from the inventory status and
   * the order is marked as delivered.
   *
   * <p> Note: the <code>sendFactorySchedules</code> method must be
   * used to send the delivery schedule to the factory after all
   * deliveries have been added. This must be done before the day
   * ends!
   *
   * @param order the order to deliver for.
   * @return true if it is possible to deliver the order and false otherwise.
   * @see #sendFactorySchedules
   */
  protected boolean addDeliveryRequest(Order order) {
    synchronized (lock) {
      int productID = order.getProductID();
      int quantity = order.getQuantity();
      if (nextDayInventory.getInventoryQuantity(productID) < quantity) {
	// Not enough products in inventory
	return false;
      }

      DeliverySchedule schedule = this.deliverySchedule;
      if (schedule == null) {
	schedule = this.deliverySchedule = new DeliverySchedule();
      }
      schedule.addDelivery(productID, quantity, order.getOrderID(),
			   order.getOtherAgent());

      // Remove the delivered products from the inventory
      nextDayInventory.addInventory(productID, -quantity);

      // Mark the order as delivered
      order.setDelivered();
      return true;
    }
  }

  /**
   * Adds a production request to the production schedule for next day
   * if possible.
   *
   * This method is using the calculated inventory for next day to
   * determine if the production can be made or not. If it can be made
   * the components used for the production are removed from the
   * inventory status.
   *
   * <p> Note: the <code>sendFactorySchedules</code> method must be
   * used to send the production schedule to the factory after all
   * production requests have been added. This must be done before the
   * day ends!
   *
   * @param productID the product to produce
   * @param quantity the number of products to produce
   * @return true if it is possible to produce the specified products
   * and false otherwise.
   * @see #sendFactorySchedules
   */
  protected boolean addProductionRequest(int productID, int quantity) {
    int index = bomBundle.getIndexFor(productID);
    if (index < 0) {
      throw new IllegalArgumentException("product " + productID
					 + " was not found in the BOM");
//       return false;
    }
    int cyclesReq = bomBundle.getAssemblyCyclesRequired(index);
    if (cyclesReq <= 0) {
      throw new IllegalArgumentException("no assembly cycle specification "
					 + "for product " + productID
					 + " was found in the BOM");
//       return false;
    }
    int[] components = bomBundle.getComponents(index);
    if (components == null) {
      throw new IllegalArgumentException("no components "
					 + "for product " + productID
					 + " was found in the BOM");
//       return false;
    }

    synchronized (lock) {
      if ((quantity * cyclesReq) > freeFactoryCapacity) {
	// Not enough free factory cycles to produce this request
	return false;
      }
      if (!hasAvailableComponents(components, quantity, nextDayInventory)) {
	// The needed components are not available in the inventory
	return false;
      }

      // This product can be done

      // The factory will have less free capacity after producing
      // this order
      freeFactoryCapacity -= quantity * cyclesReq;

      // Remove the consumed component from the free inventory
      for (int j = 0, m = components.length; j < m; j++) {
	nextDayInventory.addInventory(components[j], -quantity);
      }

      ProductionSchedule schedule = this.productionSchedule;
      if (schedule == null) {
	schedule = this.productionSchedule = new ProductionSchedule();
      }
      schedule.addProduction(productID, quantity);
      return true;
    }
  }

  private boolean hasAvailableComponents(int[] components, int quantity,
					 InventoryStatus inventory) {
    for (int j = 0, m = components.length; j < m; j++) {
      if (inventory.getInventoryQuantity(components[j]) < quantity) {
	return false;
      }
    }
    return true;
  }

  /**
   * Sends the delivery and production schedules to the manufacturer's
   * factory.
   */
  protected void sendFactorySchedules() {
    ProductionSchedule productionSchedule;
    DeliverySchedule deliverySchedule;
    synchronized (lock) {
      productionSchedule = this.productionSchedule;
      if (productionSchedule != null) {
	if (lastDayProduction != null) {
	  throw new IllegalStateException("schedules for next day already "
					  + "sent to factory");
	}
	this.lastDayProduction = productionSchedule;
	this.productionSchedule = null;
      }

      deliverySchedule = this.deliverySchedule;
      this.deliverySchedule = null;
    }

    if (productionSchedule != null) {
      sendMessage(factoryAddress, productionSchedule);
    }
    if (deliverySchedule != null) {
      sendMessage(factoryAddress, deliverySchedule);
    }
  }


  // -------------------------------------------------------------------
  //  Send all pending information
  // -------------------------------------------------------------------

  /**
   * Convenient method to make sure all offer, orders, delivery- and
   * production schedules have been sent.
   */
  protected void sendAll() {
    sendSupplierRFQs();
    sendSupplierOrders();
    sendFactorySchedules();
    sendCustomerOffers();
  }


  // -------------------------------------------------------------------
  //  Agent implementation
  // -------------------------------------------------------------------

  /**
   * Called when the agent received all startup information and it is
   * time to start participating in the simulation.
   */
  protected abstract void simulationStarted();

  /**
   * Called when a game/simulation has ended and the agent should
   * free its resources.
   */
  protected abstract void simulationEnded();

  /**
   * Called when a bundle of RFQs have been received from the
   * customers. In TAC SCM the customers only send one bundle per
   * day and the same RFQs are sent to all manufacturers.
   *
   * @param rfqBundle a bundle of RFQs
   */
  protected abstract void handleCustomerRFQs(RFQBundle rfqBundle);

  /**
   * Called when a bundle of orders have been received from the
   * customers. In TAC SCM the customers only send one order bundle
   * per day as response to offers (and only if they want to order
   * something).
   *
   * @param newOrders the new customer orders
   */
  protected abstract void handleCustomerOrders(Order[] newOrders);

  /**
   * Called when a bundle of offers have been received from a
   * supplier. In TAC SCM suppliers only send on offer bundle per
   * day in reply to RFQs (and only if they had something to offer).
   *
   * @param supplierAddress the supplier that sent the offers
   * @param offers a bundle of offers
   */
  protected abstract void handleSupplierOffers(String supplierAddress,
					       OfferBundle offers);

  /**
   * Called when a delivery notice has been received from a
   * supplier. In TAC SCM only the suppliers send delivery notices
   * to manufacturers. A delivery notice contains the components that
   * will be delivered during the day and these components will be
   * available for production the next day. The supplier address can
   * be determined with the <code>getSupplier</code> method in the
   * delivery notice.
   *
   * @param notice a delivery notice with a list of components that
   * are about to be delivered
   * @see se.sics.tasim.props.DeliveryNotice#getSupplier
   */
  protected void handleSupplierDelivery(DeliveryNotice notice) {
  }

  /**
   * Called each day when the factory status is received.
   *
   * @param status the latest factory status (factory utilization and
   * inventory)
   */
  protected void handleFactoryStatus(FactoryStatus status) {
  }

  /**
   * Called each day when the bank status is received.
   *
   * @param status the latest bank status
   */
  protected void handleBankStatus(BankStatus status) {
  }

  /**
   * Called each day when the price report is received.
   *
   * @param report the price report for previous day
   */
  protected void handlePriceReport(PriceReport report) {
  }

  /**
   * Called when a market report is received.
   *
   * @param report the latest market report
   */
  protected void handleMarketReport(MarketReport report) {
  }

  /**
   * Called when a simulation status has been received and that all
   * messages from the server this day have been received. The next
   * message will be for the next day.
   *
   * @param status a simulation status
   */
  protected abstract void handleSimulationStatus(SimulationStatus status);

} // SCMAgent
