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
 * WolfAgent.java
 *
 * Author  : Tui Popenoe
 * Created : Sat April 19 01:47:55 2014
 * Updated : $Date: 2014/04/19 22:34:39
 *           $Revision: 1.00
 */

import java.util.Random;
import java.util.logging.Logger;
import java.util.HashMap;
import java.util.ArrayList;

import se.sics.tasim.props.BOMBundle;
import se.sics.tasim.props.ComponentCatalog;
import se.sics.tasim.props.InventoryStatus;
import se.sics.tasim.props.FactoryStatus;
import se.sics.tasim.props.OfferBundle;
import se.sics.tasim.props.OrderBundle;
import se.sics.tasim.props.RFQBundle;
import se.sics.tasim.props.SimulationStatus;
import se.sics.tasim.props.StartInfo;
import se.sics.tasim.tac03.aw.Order;
import se.sics.tasim.tac03.aw.OrderStore;
import se.sics.tasim.tac03.aw.RFQStore;
import se.sics.tasim.tac03.aw.SCMAgent;

/**
 * The <code>WolfAgent</code> is an example of a simple
 * Manufacturer agent using <code>{@link SCMAgent}</code> to simplify
 * the implementation.<p>
 *
 * This example manufacturer uses strict build to order. In optimal
 * case be possible to deliver in 6 days (including one day for the
 * suppliers to produce the supply).<p>
 *
 * <dl>
 * <dt>Day D:
 * <dd>receive RFQ from customer and send offer to customer
 * <dt>Day D + 1:
 * <dd>receive order from customer and send RFQ to suppliers
 * <dt>Day D + 2:
 * <dd>receive offers from suppliers and send orders for supply
 * <dt>Day D + 3:
 * <dd>suppliers produce the requested supply
 * <dt>Day D + 4:
 * <dd>delivery of supply from suppliers
 * <dt>Day D + 5:
 * <dd>assembling products
 * <dt>Day D + 6:
 * <dd>delivery to customer
 * </dl>
 *<p>
 * This means that the example manufacturer will never bid for
 * requests with a too short due date.<p>
 *
 * Features of the example:
 * <ul>
 * <li> Orders supply from randomly selected supplier
 * <li> Pricing based on random function of the reserve price
 * <li> Bids on all customer RFQs with sufficiently late due date
 * <li> Does not bid for customers orders beyond the end of the game
 * <li> Ignores factory capacity limitations when bidding for customer orders
 * <li> Assembles the PCs as soon as possible
 * <li> Delivers to customers on due date (customers will not pay
 * earlier anyway)
 * <li> Removes too late orders when the customer cancels them and
 * reuses the components/products for other orders.
 * <li> Assumes that suppliers will deliver in time
 * </ul>
 */
public class WolfAgent extends SCMAgent {

  private static final Logger log =
    Logger.getLogger(WolfAgent.class.getName());

  private Random random = new Random();

  /** Latest possible due date when bidding for customer orders */
  private int lastBidDueDate;

  // Store a hashtable with productID and quantity demanded
  private HashMap<Integer, Integer> rfqHash;

  // Store a hashtable with the productID and the price discount offered
  private HashMap<Integer, Float> priceHash;

  /** Offer price discount factor when bidding for customer orders */
  private float priceDiscountFactor = 0.2f;

  /** Array list of price discounts that result in a successful bid */
  private ArrayList<Float> successfulBidDiscounts = new ArrayList<Float>();

  /** Bookkeeper for component demand for accepted customer orders */
  private InventoryStatus componentDemand = new InventoryStatus();

  public WolfAgent() {
  }

  /**
   * Called when the agent received all startup information and it is
   * time to start participating in the simulation.
   */
  protected void simulationStarted() {
    StartInfo info = getStartInfo();
    // Calculate the latest possible due date that can be produced for
    // and delivered in this game/simulation
    this.lastBidDueDate = info.getNumberOfDays() - 2;
  }

  /**
   * Called when a game/simulation has ended and the agent should
   * free its resources.
   */
  protected void simulationEnded() {
  }

  /**
   * STEP 1:
   * Called when a bundle of RFQs have been received from the
   * customers. In TAC03 SCM the customers only send one bundle per
   * day and the same RFQs are sent to all manufacturers.
   *
   * @param rfqBundle a bundle of RFQs
   */
    protected void handleCustomerRFQs(RFQBundle rfqBundle) {
        int currentDate = getCurrentDate();
        this.rfqHash = new HashMap<Integer, Integer>();

        FactoryStatus factoryStatus = new FactoryStatus(componentDemand);
        // If the factory utilization is too high, don't accept new offers
        if(factoryStatus.getUtilization() > 0.9){
            // TODO: check if orders can be filled using exisiting inventory
        }
        else{
            for (int i = 0, n = rfqBundle.size(); i < n; i++) {
              int dueDate = rfqBundle.getDueDate(i);
              // Only bid for quotes to which we have time to produce PCs and
              // where the delivery time is not beyond the end of the game.
              if((dueDate - currentDate) >= 6 && (dueDate <= lastBidDueDate))
              {

               // The maximal price the sender of the request is willing to pay
               // per unit in the order
               int resPrice = rfqBundle.getReservePricePerUnit(i);

               // This stores the quantity and the ID for each product demanded
               // Allows us to compare if the product requested received a
               // winning bid
               int tempID = rfqBundle.getProductID(i);
               int tempQuantity = rfqBundle.getQuantity(i);
               rfqHash.put(tempID, tempQuantity);

               // Find a price to offer to the customer
               int offeredPrice = createCustomerOfferPrice(tempID, resPrice);

               // Adds an offer to the customers in response to a RFQ
               addCustomerOffer(rfqBundle, i, offeredPrice);
              }
            }
        }


        // Finished adding offers. Send all offers to the customers.
        sendCustomerOffers();
    }

    /**
    * STEP 2:
    * Create a price for the order that is profitable and will be accepted
    * Anything higher than the reserve price will not be accepted
    * @param reservePrice the maximum price a customer will pay
    */
    protected int createCustomerOfferPrice(int id, int reservePrice){
        this.priceHash = new HashMap<Integer, Float>();

        float priceDiscountFloat =
        (1.0f - random.nextFloat() * priceDiscountFactor);

        // Allows us to compare rfqHash to priceHash to see if bid was
        // successful
        this.priceHash.put(id, priceDiscountFloat);
        // TODO: Change to a non-random method
        // Use random bids for first 50 days of simulation
        if(this.getCurrentDate() < 50){
            return (int)(reservePrice * priceDiscountFloat);
        }
        // After that, use average discount factor of winning bids
        else{
            return (int)(reservePrice * calculateAverageWinningBid());
        }

    }

    protected float calculateAverageWinningBid(){
        float average = 0.0f;
        for(int i = 0; i < successfulBidDiscounts.size(); i++){
            average += successfulBidDiscounts.get(i);
        }

        return average / successfulBidDiscounts.size();
    }

  /**
   * STEP 3:
   * Called when a bundle of orders have been received from the
   * customers. In TAC03 SCM the customers only send one order bundle
   * per day as response to offers (and only if they want to order
   * something).
   *
   * @param newOrders the new customer orders
   */
    protected void handleCustomerOrders(Order[] newOrders) {
        // Add the component demand for the new customer orders
        BOMBundle bomBundle = getBOMBundle();
        // Initialize an order from the order Array, ID and quantity
        // Save space from initializing every loop
        Order order;
        int productID = 0;
        int quantity = 0;

        for (int i = 0, n = newOrders.length; i < n; i++) {
            order = newOrders[i];
            productID = order.getProductID();
            quantity = order.getQuantity();

            // Keep track of successful bid discounts here
            // Check the rfq hash
            Integer rfqQuantity = this.rfqHash.get(productID);
            if(rfqQuantity != null){
                if(rfqQuantity == quantity){
                    // Get the discount offered on the product
                    float tempPrice = this.priceHash.get(productID);
                    this.successfulBidDiscounts.add(tempPrice);
                }
            }

            // Get the components required to produce the product
            int[] components = bomBundle.getComponentsForProductID(productID);
            if (components != null) {
                for (int j = 0, m = components.length; j < m; j++) {
                    // Add the required components to the inventory
                    // This assumes the can be supplied
                    // TODO: Update inventory with the actual quantity supplied
                    componentDemand.addInventory(components[j], quantity);
                }
            }
        }
        // Agent creates the orders to supplier
        this.handleSupplierOrders();

        // Agent sends the RFQ's to suppliers
        this.sendSupplierRFQs();
    }

  /**
   * STEP 4:
   * Handles the creation of RFQs to suppliers
   */
  protected void handleSupplierOrders(){
    ComponentCatalog catalog = getComponentCatalog();
    int currentDate = getCurrentDate();

    for(int i = 0, n = componentDemand.getProductCount(); i<n;i++){
      // Get the quantity demanded of a particular component
      int quantity = componentDemand.getQuantity(i);
      if(quantity > 0){
        int productID = componentDemand.getProductID(i);
        String[] suppliers = catalog.getSuppliersForProduct(productID);
        if(suppliers != null){
          int supIndex = calculateBestSupplier(suppliers);
          // TODO: Create a method to calculate reserve price for suppliers
          int reservePrice = calculateSupplierReservePrice();
          addSupplierRFQ(suppliers[supIndex], productID, quantity,
           reservePrice, currentDate+2);

          // Assume that the supplier will be able to dliver the components
          componentDemand.addInventory(productID, -quantity);
        }
        else{
          log.severe("No suppliers for product " + productID);
        }
      }
    }
  }

  // STEP 5:
  // Chooses the best supplier for a given component
  protected int calculateBestSupplier(String[] suppliers){
    //TODO: replace this method with a non-random supplier based on
    // past history
    return random.nextInt(suppliers.length);
  }

  // STEP 6:
  // Calculate a price to order components at while still being profitable
  // TODO: Currently unbounded at 0
  protected int calculateSupplierReservePrice(){
    return 0;
  }

  /**
   * STEP 7:
   * Called when a bundle of offers have been received from a
   * supplier. In TAC03 SCM suppliers only send one offer bundle per
   * day in reply to RFQs (and only if they had something to offer).
   *
   * @param supplierAddress the supplier that sent the offers
   * @param offers a bundle of offers
   */
  protected void handleSupplierOffers(String supplierAddress,
                      OfferBundle offers) {
    // Earliest complete is always after partial offers so the offer
    // bundle is traversed backwards to always accept earliest offer
    // instead of the partial (the server will ignore the second
    // order for the same offer).

    // Set cheapest offer to last index
    int cheapestOfferIndex = offers.size()-1;
    // Initialize the beginning price to the last price in the array
    int currentUnitPrice = offers.getUnitPrice(offers.size() -1);

    // The ratio over the cheapest price we are willing to spend.
    double supplierPriceTolerance = 1.1;

    // TODO: set correctQuantity quantity in RFQID of offers.getRFQID(i)
    int correctQuantity = 0;
    int bestOffer = offers.size() -1;
    // Loop through the offers and determine the cheapest offer
    for (int i = offers.size() - 1; i >= 0; i--) {
      // Only order if quantity > 0 (otherwise it is only a price quote)

      if(offers.getUnitPrice(i) < currentUnitPrice){
        currentUnitPrice = offers.getUnitPrice(i);
        cheapestOfferIndex = i;
      }
    }
    // Loop through the offers, if an offer has the correct quantity and
    // is within our price tolerance of the cheapest price, set it as the best
    // offer.
    for (int i = offers.size() -1; i>=0; i--){

      if (offers.getQuantity(i) > 0 && offers.getQuantity(i) >= correctQuantity){
        if(offers.getUnitPrice(i) <= cheapestOfferIndex*supplierPriceTolerance){
          bestOffer = i;
        }
      }
    }

    addSupplierOrder(supplierAddress, offers, bestOffer);
    sendSupplierOrders();
  }

  /**
   * STEP 8:
   * Called when a simulation status has been received and that all
   * messages from the server this day have been received. The next
   * message will be for the next day.
   *
   * @param status a simulation status
   */
  protected synchronized void handleSimulationStatus(SimulationStatus status) {

    // The inventory for next day is calculated with todays deliveries
    // and production and is changed when production and delivery
    // requests are made.
    InventoryStatus inventory = getInventoryForNextDay();

    // Generate production and delivery schedules
    int currentDate = getCurrentDate();
    int latestDueDate = currentDate - getDaysBeforeVoid() + 2;

    OrderStore customerOrders = getCustomerOrders();
    Order[] orders = customerOrders.getActiveOrders();
    if (orders != null) {
      for (int i = 0, n = orders.length; i < n; i++) {
    Order order = orders[i];
    int productID = order.getProductID();
    int dueDate = order.getDueDate();
    int orderedQuantity = order.getQuantity();
    int inventoryQuantity = inventory.getInventoryQuantity(productID);

    if ((currentDate >= (dueDate - 1)) && (dueDate >= latestDueDate)
        && addDeliveryRequest(order)) {
      // It was time to deliver this order and it could be
      // delivered (the method above ensures this). The order has
      // automatically been marked as delivered and the products
      // have been removed from the inventory status (to avoid
      // delivering the same products again).

    } else if (dueDate <= latestDueDate) {

      // It is too late to produce and deliver this order
      log.info("canceling to late order " + order.getOrderID()
           + " (dueDate=" + order.getDueDate()
           + ",date=" + currentDate + ')');
      cancelCustomerOrder(order);

    } else if (inventoryQuantity >= orderedQuantity) {

      // There is enough products in the inventory to fulfill this
      // order and nothing more should be produced for it. However
      // to avoid reusing these products for another order they
      // must be reserved.
      reserveInventoryForNextDay(productID, orderedQuantity);

    } else if (addProductionRequest(productID,
                    orderedQuantity - inventoryQuantity)) {
      // The method above will ensure that the needed components
      // was available and that the factory had enough free
      // capacity. It also removed the needed components from the
      // inventory status.

      // Any existing products have been allocated to this order
      // and must be reserved to avoid using them in another
      // production or delivery.
      reserveInventoryForNextDay(productID, inventoryQuantity);

    } else {
      // Otherwise the production could not be done (lack of
      // free factory cycles or not enough components in
      // inventory) and nothing can be done for this order at
      // this time.
    }
      }
    }

    sendFactorySchedules();
  }

  private void cancelCustomerOrder(Order order) {
    order.setCanceled();

    // The components for the canceled order are now available to be
    // used in other orders.
    int[] components =
      getBOMBundle().getComponentsForProductID(order.getProductID());
    if (components != null) {
      int quantity = order.getQuantity();
      for (int j = 0, m = components.length; j < m; j++) {
    componentDemand.addInventory(components[j], -quantity);
      }
    }
  }

} //WolfAgent
