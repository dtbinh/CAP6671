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
 * RFQStore
 *
 * Author  : Joakim Eriksson, Niclas Finne, Sverker Janson
 * Created : Mon May 12 17:57:45 2003
 * Updated : $Date: 2005/06/08 14:22:49 $
 *           $Revision: 1.2 $
 */
package se.sics.tasim.tac03.aw;

/**
 * <code>RFQStore</code> contains a collection of RFQs.
 */
public class RFQStore {

  private static final int PRODUCT = 0;
  private static final int QUANTITY = 1;
  private static final int DUE_DATE = 2;
  private static final int SUBMIT_DATE = 3;
  private static final int PARTS = 4;

  private int[] data = new int[100 * PARTS];
  private String[] supplierAddresses = new String[100];
  private int startID;
  private int count = 0;

  public RFQStore() {
    this(1);
  }

  public RFQStore(int startID) {
    this.startID = startID;
  }

  /**
   * Returns true if the specified RFQ is in this store and false otherwise.
   *
   * @param rfqID the id of the RFQ
   * @return true if the specified RFQ is in this store and false otherwise
   */
  public boolean hasRFQ(int rfqID) {
    return (rfqID >= startID) && (rfqID < (startID + count));
  }

  /**
   * Returns the address of the supplier (receiver) for the specified RFQ.
   *
   * @param rfqID the id of the RFQ
   * @return the address of the supplier
   * @throws IllegalArgumentException if the RFQ did not exist
   */
  public String getSupplier(int rfqID) {
    if ((rfqID < startID) || (rfqID >= (startID + count))) {
      throw new IllegalArgumentException("RFQ " + rfqID + " not found");
    }
    return supplierAddresses[rfqID - startID];
  }

  /**
   * Returns the product in the specified RFQ
   *
   * @param rfqID the id of the RFQ
   * @return the id of the product in the specified RFQ
   * @throws IllegalArgumentException if the RFQ did not exist
   */
  public int getProductID(int rfqID) {
    return get(rfqID, PRODUCT);
  }

  /**
   * Returns the quantity in the specified RFQ
   *
   * @param rfqID the id of the RFQ
   * @return the quantity of products in the specified RFQ
   * @throws IllegalArgumentException if the RFQ did not exist
   */
  public int getQuantity(int rfqID) {
    return get(rfqID, QUANTITY);
  }

  /**
   * Returns the due date in the specified RFQ
   *
   * @param rfqID the id of the RFQ
   * @return the due date in the specified RFQ
   * @throws IllegalArgumentException if the RFQ did not exist
   */
  public int getDueDate(int rfqID) {
    return get(rfqID, DUE_DATE);
  }

  private int get(int rfqID, int type) {
    if ((rfqID < startID) || (rfqID >= (startID + count))) {
      throw new IllegalArgumentException("RFQ " + rfqID + " not found");
    }
    return data[(rfqID - startID) * PARTS + type];
  }

  /**
   * Returns the id of the first RFQ in this store or -1 if no RFQ exists.
   */
  public int getFirstID() {
    return count > 0 ? startID : -1;
  }

  /**
   * Returns the id of the last RFQ in this store or -1 if no RFQ exists.
   */
  public int getLastID() {
    return count > 0 ? startID + count : -1;
  }

  // Package protected because agents should not add RFQs themselves
  // to this container
  synchronized int addRFQ(String supplierAddress, int productID, int quantity,
			  int dueDate, int currentDate) {
    if (count == supplierAddresses.length) {
      String[] a = new String[count + 100];
      int[] d = new int[(count + 100) * PARTS];
      System.arraycopy(supplierAddresses, 0, a, 0, count);
      System.arraycopy(data, 0, d, 0, count * PARTS);
      supplierAddresses = a;
      data = d;
    }
    int id = count;
    int index = id * PARTS;
    supplierAddresses[id] = supplierAddress;
    data[index + PRODUCT] = productID;
    data[index + QUANTITY] = quantity;
    data[index + DUE_DATE] = dueDate;
    data[index + SUBMIT_DATE] = currentDate;
    count++;
    return id + startID;
  }

} // RFQStore
