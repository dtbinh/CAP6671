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
 * ManufacturerInfo
 *
 * Author  : Joakim Eriksson, Niclas Finne, Sverker Janson
 * Created : Thu Feb 20 16:30:21 2003
 * Updated : $Date: 2004/09/07 15:22:04 $
 *           $Revision: 1.14 $
 */
package se.sics.tasim.tac03.aw;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.net.URL;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import com.botbox.util.ArrayUtils;
import se.sics.isl.gui.BarDiagram;
import se.sics.isl.gui.DotDiagram;
import se.sics.isl.gui.MessageModel;
import se.sics.isl.gui.MessageRenderer;
import se.sics.isl.transport.Transportable;
import se.sics.isl.util.FormatUtils;
import se.sics.tasim.aw.client.InfoViewer;
import se.sics.tasim.props.BOMBundle;
import se.sics.tasim.props.BankStatus;
import se.sics.tasim.props.ComponentCatalog;
import se.sics.tasim.props.DeliveryNotice;
import se.sics.tasim.props.DeliverySchedule;
import se.sics.tasim.props.FactoryStatus;
import se.sics.tasim.props.OfferBundle;
import se.sics.tasim.props.OrderBundle;
import se.sics.tasim.props.RFQBundle;
import se.sics.tasim.props.StartInfo;

/**
 * The <code>ManufacturerInfo</code> is a simple viewer for displaying
 * information about a manufacturer agent in TAC03 SCM
 * games/simulations.
 */
public class ManufacturerInfo extends InfoViewer {

  private static final Logger log =
    Logger.getLogger(ManufacturerInfo.class.getName());

  private JPanel mainPanel;
  private JList messageList;
  private MessageModel messageModel;

  private RFQBundle lastRFQsFromCustomer;
  private RFQBundle lastDaysRFQsFromCustomer;
  private OfferBundle lastOffersToCustomer;

  private TitledBorder accountBorder;
  private TitledBorder utilityBorder;
  private DotDiagram bankDiagram;
  private DotDiagram utilDiagram;
  private int[] bankAccount = new int[60];
  private int bankPos;
  private int[] utilization = new int[60];
  private int utilPos;

  private BarDiagram productDiagram;
  private int[] pcIDs;
  private int[] pcInventory;

  private BarDiagram componentDiagram;
  private int[] componentIDs;
  private int[] componentInventory;

  private String[] componentNames;
  private String[] pcNames;

  private ComponentCatalog catalog;
  private BOMBundle bomBundle;

  public ManufacturerInfo() {
  }

  public void init(String agentName) {
    mainPanel = new JPanel(new GridLayout(3, 2));
    mainPanel.setBackground(Color.white);

    JLabel label = new JLabel();
    URL iconURL =
      ManufacturerInfo.class.getResource("/images/manufacturer.jpg");
    if (iconURL != null) {
      ImageIcon icon = new ImageIcon(iconURL);
      label.setIcon(icon);
    }
    label.setBorder(BorderFactory.createTitledBorder("Agent " + agentName));
    mainPanel.add(label);

    messageModel = new MessageModel(50);
    messageList = new JList(messageModel);
    messageList.setCellRenderer(new MessageRenderer(messageModel));
    mainPanel.add(createScrollPane(messageList, "Latest Events", true));

    accountBorder = BorderFactory.createTitledBorder("Bank Account");

    Dimension diagramSize = new Dimension(200, 200);
    bankDiagram = new DotDiagram(1);
    bankDiagram.setBorder(accountBorder);
    bankDiagram.setPreferredSize(diagramSize);
    bankDiagram.setDotColor(0, Color.blue);
    mainPanel.add(bankDiagram);

    utilityBorder = BorderFactory.createTitledBorder("Factory Utilization");
    utilDiagram = new DotDiagram(1);
    utilDiagram.setMinMax(0, 100);
    utilDiagram.setDotColor(0, Color.blue);
    utilDiagram.setBorder(utilityBorder);
    utilDiagram.setPreferredSize(diagramSize);
    mainPanel.add(utilDiagram);

    bankDiagram.setData(0, bankAccount, bankPos, bankAccount.length);
    utilDiagram.setData(0, utilization, utilPos, utilization.length);

    productDiagram = new BarDiagram();
    productDiagram.setShowingValue(true);
    productDiagram.setBarColors(Color.blue, Color.red);
    productDiagram.setBorder(BorderFactory
			     .createTitledBorder("Product Inventory"));
    productDiagram.setPreferredSize(diagramSize);
    mainPanel.add(productDiagram);

    componentDiagram = new BarDiagram();
    componentDiagram.setShowingValue(true);
    componentDiagram.setValueColor(Color.black);
    componentDiagram.setBarColors(Color.yellow, Color.green);
    componentDiagram.setBorder(BorderFactory
			       .createTitledBorder("Component Inventory"));
    componentDiagram.setPreferredSize(diagramSize);
    mainPanel.add(componentDiagram);
  }

  private void checkInitialized() {
    BOMBundle bundle = this.bomBundle;
    ComponentCatalog catalog = this.catalog;
    if (pcIDs == null && bundle != null && catalog != null) {
      // Initialize the product inventory

      int size = bundle.size();
      pcIDs = new int[size];
      pcInventory = new int[size];
      pcNames = new String[size];
      for (int i = 0; i < size; i++) {
	pcIDs[i] = bundle.getProductID(i);
	pcNames[i] = bundle.getProductName(i) + " ["
	  + pcIDs[i] + "]: ";
      }
      productDiagram.setNames(pcNames);
      productDiagram.setToolTipVisible(true);

      size = catalog.size();
      componentNames = new String[size];
      componentIDs = new int[size];
      componentInventory = new int[size];
      for (int i = 0; i < size; i++) {
	componentIDs[i] = catalog.getProductID(i);
	componentNames[i] = catalog.getProductName(i) + " [" +
	  componentIDs[i] + "]: ";
      }
      componentDiagram.setNames(componentNames);
      componentDiagram.setToolTipVisible(true);
    }
  }

  protected void addMessage(String message) {
    addMessage(message, MessageModel.NONE);
  }

  protected void addMessage(String message, int flag) {
    messageModel.addMessage(message, flag);
    messageList.ensureIndexIsVisible(messageModel.getSize() - 1);
  }

//   protected String getProductName(int rfqID) {
//     String name = getProductName(lastDaysRFQsFromCustomer, rfqID);
//     if (name == null) {
//       name = getProductName(lastRFQsFromCustomer, rfqID);
//       if (name == null) {
// 	name = "PCs";
//       }
//     }
//     return name;
//   }

//   private String getProductName(RFQBundle bundle, int rfqID) {
//     if (bundle != null) {
//       int index = bundle.getIndexFor(rfqID);
//       if (index >= 0) {
// 	int productID = bundle.getProductID(index);
// 	BOMBundle bomBundle = this.bomBundle;
// 	if (bomBundle != null
// 	    && ((index = bomBundle.getIndexFor(productID)) >= 0)) {
// 	  return bomBundle.getProductName(index);
// 	} else {
// 	  return "PC-" + productID;
// 	}
//       }
//     }
//     return null;
//   }


  // -------------------------------------------------------------------
  //  InfoViewer API
  // -------------------------------------------------------------------

  public JComponent getComponent() {
    return mainPanel;
  }

  public void messageSent(final String receiver, final Transportable content) {
    // Use the AWT dispatcher thread to handle all messages and
    // perform all graphical updates
    SwingUtilities.invokeLater(new Runnable() {
	public void run() {
	  handleMessageSent(receiver, content);
	}
      });
  }

  private void handleMessageSent(String receiver, Transportable content) {
    Class type = content.getClass();
    if (type == OfferBundle.class) {
      OfferBundle bundle = (OfferBundle) content;
      // Assume for now that all offers are to customers (the
      // manufacturer should never send offers to any suppliers). FIX THIS!!!
      this.lastOffersToCustomer = bundle;

      int count = bundle.size();
      int totalQuantity = 0;
      long totalPrice = 0L;
      for (int i = 0; i < count; i++) {
	int quantity = bundle.getQuantity(i);
	totalQuantity += quantity;
	totalPrice += quantity * bundle.getUnitPrice(i);
      }
      addMessage("offered " + totalQuantity + " PC"
		 + (totalQuantity > 1 ? "s" : "")
		 + " for $"
		 + FormatUtils.formatAmount(totalPrice));

    } else if (type == DeliverySchedule.class) {
      // Assume manufacturers only sends delivery schedules to its
      // factory for now. FIX THIS!!!
      DeliverySchedule schedule = (DeliverySchedule) content;
      int totalQuantity = schedule.getTotalQuantity();
      addMessage("requested delivery of " + totalQuantity
		 + " PC" + (totalQuantity > 1 ? "s" : ""));
    }
  }

  public void messageReceived(final String sender, final Transportable content)
  {
    // Use the AWT dispatcher thread to handle all messages and
    // perform all graphical updates
    SwingUtilities.invokeLater(new Runnable() {
	public void run() {
	  handleMessageReceived(sender, content);
	}
      });
  }

  private void handleMessageReceived(String sender, Transportable content) {
    Class type = content.getClass();
    if (type == RFQBundle.class) {
      // Assume only customer sends RFQs to manufacturers for now. FIX THIS!!!
      RFQBundle bundle = (RFQBundle) content;
      // Since both orders from customers and RFQs comes from the
      // customer in the morning, we must remember last days RFQs too.
      lastDaysRFQsFromCustomer = lastRFQsFromCustomer;
      lastRFQsFromCustomer = bundle;

      int count = bundle.size();
      int totalQuantity = bundle.getTotalQuantity();
      addMessage(totalQuantity + " PC" + (totalQuantity > 1 ? "s" : "")
		 + " requested in " + count + " RFQ" + (count > 1 ? "s" : ""));

    } else if (type == OrderBundle.class) {
      handleOrders(sender, (OrderBundle) content);

    } else if (type == FactoryStatus.class) {
      handleInventory((FactoryStatus) content);

    } else if (type == BankStatus.class) {
      handleBankStatus(sender, (BankStatus) content);

    } else if (type == DeliveryNotice.class) {
      // Assume only suppliers delivers to manufacturers for now. FIX THIS!!!
      DeliveryNotice notice = (DeliveryNotice) content;
      int count = notice.size();
      if (count < 4) {
	ComponentCatalog catalog = this.catalog;
	int index;
	for (int i = 0; i < count; i++) {
	  int quantity = notice.getQuantity(i);
	  int productID = notice.getProductID(i);
	  if (catalog != null
	      && ((index = catalog.getIndexFor(productID)) >= 0)) {
	    addMessage("" + quantity + ' ' + sender + ' '
		       + catalog.getProductName(index) + " delivered");
	  } else {
	    addMessage("" + quantity + ' ' + sender
		       + " component" + (quantity > 1 ? "s" : "")
		       + " delivered");
	  }
	}
      } else {
	int totalQuantity = notice.getTotalQuantity();
	addMessage("" + totalQuantity + ' ' + sender
		   + " component" + (totalQuantity > 1 ? "s" : "")
		   + " delivered");
      }

    } else if (type == BOMBundle.class) {
      bomBundle = (BOMBundle) content;
      checkInitialized();

    } else if (type == ComponentCatalog.class) {
      catalog = (ComponentCatalog) content;
      checkInitialized();

    } else if (type == StartInfo.class) {
      // New simulation: clear any old information
      messageModel.clear();
      for (int i = 0, n = bankAccount.length; i < n; i++) {
	bankAccount[i] = 0;
      }
      bankPos = 0;
      for (int i = 0, n = utilization.length; i < n; i++) {
	utilization[i] = 0;
      }
      utilPos = 0;
      pcIDs = pcInventory = null;
      pcNames = null;
      productDiagram.setNames(null);
      productDiagram.setToolTipVisible(false);
      productDiagram.setData(null);
      componentIDs = componentInventory = null;
      componentNames = null;
      componentDiagram.setNames(null);
      componentDiagram.setToolTipVisible(false);
      componentDiagram.setData(null);
    }
  }

  private void handleOrders(String sender, OrderBundle orders) {
    OfferBundle offers = this.lastOffersToCustomer;
    int orderNumber = orders.size();
    if (offers == null) {
      // No previously sent offers??? What to do here?? FIX THIS!!!
      addMessage("got " + orderNumber + " order"
		 + (orderNumber > 1 ? "s" : "")
		 + " (info missing)");

    } else {
      int totalQuantity = 0;
      long totalPrice = 0L;
      for (int i = 0; i < orderNumber; i++) {
	int offerID = orders.getOfferID(i);
	int index = offers.getIndexFor(offerID);
	if (index >= 0) {
	  int quantity = offers.getQuantity(index);
	  totalQuantity += quantity;
	  totalPrice += quantity * offers.getUnitPrice(index);
	} else {
	  // What should be done here when no offer was found??? FIX THIS!!!
	}
      }
      addMessage(totalQuantity + " PC"
		 + (totalQuantity == 1 ? "" : "s")
		 + " for $"
		 + FormatUtils.formatAmount(totalPrice) + " ordered");
    }
  }

  private void handleBankStatus(String sender, BankStatus status) {
    // Set the value for this day (the last value will be shown in
    // the diagram)
    long currentBankAccount = status.getAccountStatus();
    accountBorder.setTitle("Bank Account ($"
			   + FormatUtils.formatAmount(currentBankAccount)
			   + ')');
    bankAccount[bankPos] =
      currentBankAccount >= Integer.MAX_VALUE
      ? Integer.MAX_VALUE
      : (currentBankAccount <= Integer.MIN_VALUE
	 ? Integer.MIN_VALUE
	 : (int) currentBankAccount
	 );
    bankPos = (bankPos + 1) % bankAccount.length;
    if (bankDiagram != null) {
      bankDiagram.setData(0, bankAccount, bankPos, bankAccount.length);
    }

    int count = status.getPenaltyCount();
    if (count > 0) {
      long totalPenalty = status.getTotalPenaltyAmount();
      addMessage("penalty $" + FormatUtils.formatAmount(totalPenalty)
		 + " for " + count + " late deliver"
		 + (count == 1 ? "y" : "ies"),
		 MessageModel.WARNING);
    }
  }

  private void handleInventory(FactoryStatus inventory) {
    if (pcIDs == null) {
      log.severe("could not handle inventory because missing "
		 + (this.bomBundle == null
		    ? "BOMBundle"
		    : "ComponentCatalog"));
      return;
    }

    for (int i = 0, n = pcInventory.length; i < n; i++) {
      pcInventory[i] = 0;
    }
    for (int i = 0, n = componentInventory.length; i < n; i++) {
      componentInventory[i] = 0;
    }

    for (int i = 0, n = inventory.getProductCount(); i < n; i++) {
      int id = inventory.getProductID(i);
      int index = ArrayUtils.indexOf(componentIDs, id);
      if (index >= 0) {
	componentInventory[index] = inventory.getQuantity(i);
      } else if ((index = ArrayUtils.indexOf(pcIDs, id)) >= 0) {
	pcInventory[index] = inventory.getQuantity(i);
      } else {
	// What should we do here if the component/product was not found?
	log.severe("unknown product in InventoryStatus: " + id);
      }
    }

    productDiagram.setData(pcInventory);
    componentDiagram.setData(componentInventory);

    // Update the utilization
    int currentUtility = (int) (inventory.getUtilization() * 100);
    utilization[utilPos] = currentUtility;
    utilPos = (utilPos + 1) % utilization.length;
    utilityBorder.setTitle("Factory Utilization (" + currentUtility + "%)");
    utilDiagram.setData(0, utilization, utilPos, utilization.length);
  }



  // -------------------------------------------------------------------
  //  Utilities
  // -------------------------------------------------------------------

  protected JScrollPane createScrollPane(JComponent component) {
    return createScrollPane(component, null, false);
  }

  protected JScrollPane createScrollPane(JComponent component, String title) {
    return createScrollPane(component, title, false);
  }

  protected JScrollPane createScrollPane(JComponent component, String title,
					 boolean horizontalScrollbar) {
    JScrollPane scrollPane =
      new JScrollPane(component,
		      JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
		      horizontalScrollbar
		      ? JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
		      : JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    Color color = component.getBackground();
    if (color == null) {
      color = Color.white;
    }
    scrollPane.setBackground(color);
    scrollPane.getViewport().setBackground(color);
    if (title != null) {
      scrollPane.setBorder(BorderFactory.createTitledBorder(title));
    }

    JScrollBar scrollBar = scrollPane.getVerticalScrollBar();
    scrollBar.setPreferredSize(new Dimension(10, scrollBar.getHeight()));
    if (horizontalScrollbar) {
      scrollBar = scrollPane.getHorizontalScrollBar();
      scrollBar.setPreferredSize(new Dimension(scrollBar.getWidth(), 10));
    }
    return scrollPane;
  }

} // ManufacturerInfo
