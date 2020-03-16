package com.shopNow.Lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Properties;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Shop_now_Order_cancel implements RequestHandler<JSONObject, JSONObject> {

	private String USERNAME;
	private String PASSWORD;
	private String DB_URL;

	@SuppressWarnings({ "unchecked", "unused" })
	public JSONObject handleRequest(JSONObject input, Context context) {

		LambdaLogger logger = context.getLogger();

		JSONArray cart_Add_array = new JSONArray();
		JSONObject jsonObject_cancelorder_result = new JSONObject();

		Object userid1 = input.get("userid").toString();

		String product_id1 = input.get("product_id").toString();
		String order_id = input.get("order_id").toString();
		String order_cancel_reason = input.get("order_cancel_reason").toString();
		if (order_cancel_reason.contains("'")) {
			order_cancel_reason = order_cancel_reason.replaceAll("'", "''");

		}
		
		DecimalFormat df = new DecimalFormat("0.00");

		String Str_msg;
		String orderNumber = null;
		Connection conn = null;
		int flag = 0;
		float sale_price = 0, grant_total = 0, shipping_charge = 0;
		float refund_sale_price = 0, refund_grant_total = 0, refund_shipping_charge = 0, refund_shipping_discounts;
		String refund_transaction_id = null, refund_payment_status = null, refund_mode_of_payment = null,
				refund_date_of_order_paid = null;

		String refund_type = "order_cancel";
		String refund_status = "not yet initiated";
		String sql_refund_insert = null;

		if (userid1 == null || userid1 == "") {

			Str_msg = "userID is null";
			jsonObject_cancelorder_result.put("status", "0");
			jsonObject_cancelorder_result.put("message", Str_msg);
			return jsonObject_cancelorder_result;

		}
		long userid = Long.parseLong(userid1.toString());

		if (order_id == null || order_id == "") {

			Str_msg = "OrderID is  null";
			jsonObject_cancelorder_result.put("status", "0");
			jsonObject_cancelorder_result.put("message", Str_msg);
			return jsonObject_cancelorder_result;

		}

		if (product_id1 == null || product_id1 == "") {
			flag = 0;

		} else {
			flag = 1;

		}
		// -----database connection-----------

		String order_number = null;
		int quantity = 0, shipping_address_id = 0;

		String sql_cust_order_product;
		String sql_order_update = null;
		String sql_cust_order_product_count;
		String sql_pro = null;
		int count = 0;
		int ved_id = 0;
		int order_product_vendor_id = 0;
		float discount, grand_sub_total;

		Properties prop = new Properties();

		try {
			prop.load(getClass().getResourceAsStream("/application.properties"));
			DB_URL = prop.getProperty("url");
			USERNAME = prop.getProperty("username");
			PASSWORD = prop.getProperty("password");
			conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
		} catch (Exception e) {
			e.printStackTrace();
			JSONObject jo_catch = new JSONObject();
			jo_catch.put("Exception", e.getMessage());
			return jo_catch;

		}

		// Get time from DB server
		try {

			Statement stmt_customer = conn.createStatement();
			ResultSet srs_customer = stmt_customer
					.executeQuery("SELECT id, status FROM customers where id='" + userid + "'");

			if (srs_customer.next() == false) {

				Str_msg = "No user found!";
				jsonObject_cancelorder_result.put("status", "0");
				jsonObject_cancelorder_result.put("message", Str_msg);
				return jsonObject_cancelorder_result;
			}

			else if (!srs_customer.getString("status").equalsIgnoreCase("1")) {
				Str_msg = "User not confirmed !";
				jsonObject_cancelorder_result.put("status", "0");
				jsonObject_cancelorder_result.put("message", Str_msg);
				return jsonObject_cancelorder_result;
			}

			srs_customer.close();
			stmt_customer.close();

			String Sql_order_user = "select * from customer_orders where order_id='" + order_id + "' and customer_id='"
					+ userid + "'";
			//logger.log("\n Sql_order_cancel \n" + Sql_order_user);
			Statement stmt_user = conn.createStatement();
			ResultSet srs_order_user = stmt_user.executeQuery(Sql_order_user);

			if (srs_order_user.next() == false) {

				Str_msg = "user have no order";
				jsonObject_cancelorder_result.put("status", "0");
				jsonObject_cancelorder_result.put("message", Str_msg);
				return jsonObject_cancelorder_result;
			} else if (srs_order_user.getString("order_status_code").equalsIgnoreCase("cancel")) {

				Str_msg = "order alredy canceled";
				jsonObject_cancelorder_result.put("status", "0");
				jsonObject_cancelorder_result.put("message", Str_msg);
				return jsonObject_cancelorder_result;

			} else if (srs_order_user.getString("order_status_code").equalsIgnoreCase("shipped")) {
				Str_msg = "order alredy shipped";
				jsonObject_cancelorder_result.put("status", "0");
				jsonObject_cancelorder_result.put("message", Str_msg);
				return jsonObject_cancelorder_result;

			}

			discount = srs_order_user.getFloat("discounts");
			grand_sub_total = srs_order_user.getFloat("sub_total");

			srs_order_user.close();
			stmt_user.close();

			String sql_order_product_unic1 = "SELECT count(*) as number_of_product_in_order FROM customer_order_details where order_id='"
					+ order_id + "'";
			//logger.log("\n sql_order_product_unic1 : \n " + sql_order_product_unic1);
			Statement stmt_cust_unic1 = conn.createStatement();
			ResultSet cust_order_product_unic1 = stmt_cust_unic1.executeQuery(sql_order_product_unic1);
			int count_flag = 0;
			if (cust_order_product_unic1.next()) {
				if (cust_order_product_unic1.getInt("number_of_product_in_order") == 1) {

					flag = 0;

				} else {

					count_flag = 1;

				}

			}

			if (flag == 1) {

				int product_id = 0, vendor_id1 = 0;
				float effective_sale_price = 0, discount_product = 0;
				float total_sub=0,total_grand=0,total_discount=0,total_shipping=0;

				logger.log("\n flag :" + flag + "\n");

				String Sql_order_cancel1 = "select * from customer_orders where order_id='" + order_id + "'";

		//		logger.log("\n Sql_order_cancel \n" + Sql_order_cancel1);
				Statement stmt1 = conn.createStatement();
				ResultSet srs_order_cancel1 = stmt1.executeQuery(Sql_order_cancel1);

				if (srs_order_cancel1.next() == false) {

					Str_msg = "order is not present";
					jsonObject_cancelorder_result.put("status", "0");
					jsonObject_cancelorder_result.put("message", Str_msg);
					return jsonObject_cancelorder_result;
				} else {
					if (srs_order_cancel1.getString("order_status_code").equalsIgnoreCase("cancel")) {

						Str_msg = "order alredy canceled";
						jsonObject_cancelorder_result.put("status", "0");
						jsonObject_cancelorder_result.put("message", Str_msg);
						return jsonObject_cancelorder_result;

					} else if (srs_order_cancel1.getString("order_status_code").equalsIgnoreCase("shipped")) {

						Str_msg = "order alredy shipped";
						jsonObject_cancelorder_result.put("status", "0");
						jsonObject_cancelorder_result.put("message", Str_msg);
						return jsonObject_cancelorder_result;

					}
					else if (srs_order_cancel1.getString("order_status_code").equalsIgnoreCase("delivered")) {

						Str_msg = "order alredy delivered";
						jsonObject_cancelorder_result.put("status", "0");
						jsonObject_cancelorder_result.put("message", Str_msg);
						return jsonObject_cancelorder_result;

					}
					//total_discount = srs_order_cancel1.getFloat("discounts");
					order_number = srs_order_cancel1.getString("order_number");
					shipping_address_id = srs_order_cancel1.getInt("delivery_address_id");
					refund_payment_status = srs_order_cancel1.getString("payment_status");
					//total_sub=srs_order_cancel1.getFloat("sub_total");
					//total_grand=srs_order_cancel1.getFloat("grand_total");
				    //total_shipping=srs_order_cancel1.getFloat("shipping");
					

				}
				srs_order_cancel1.close();
				stmt1.close();

				String sql_order_product = "SELECT * FROM customer_order_details where order_id='" + order_id
						+ "' and product_id='" + product_id1 + "'";

				Statement stmt_cust1 = conn.createStatement();
				ResultSet cust_order_product1 = stmt_cust1.executeQuery(sql_order_product);

		//		logger.log("\nsql_cust_order_product \n" + sql_order_product);

				if (cust_order_product1.next()==false) {
					
					
					Str_msg = "product is not presant in this order";
					jsonObject_cancelorder_result.put("status", "0");
					jsonObject_cancelorder_result.put("message", Str_msg);
					return jsonObject_cancelorder_result;
					
					
				}
				else {

					if (cust_order_product1.getString("delivery_status_code").equalsIgnoreCase("cancel")) {

						Str_msg = "product alredy canceled from this order";
						jsonObject_cancelorder_result.put("status", "0");
						jsonObject_cancelorder_result.put("message", Str_msg);
						return jsonObject_cancelorder_result;

					} else if (cust_order_product1.getString("delivery_status_code").equalsIgnoreCase("shipped")) {

						Str_msg = "product alredy shipped";
						jsonObject_cancelorder_result.put("status", "0");
						jsonObject_cancelorder_result.put("message", Str_msg);
						return jsonObject_cancelorder_result;
					} else if (cust_order_product1.getString("delivery_status_code").equalsIgnoreCase("Order placed")
							|| cust_order_product1.getString("delivery_status_code")
									.equalsIgnoreCase("delivery_status_code")) {

						product_id = cust_order_product1.getInt("product_id");
						vendor_id1 = cust_order_product1.getInt("vendor_id");
						quantity = cust_order_product1.getInt("quantity");
						sale_price = cust_order_product1.getFloat("price") * quantity;
						order_product_vendor_id = cust_order_product1.getInt("vendor_id");
						shipping_charge = cust_order_product1.getFloat("shipping_charge");
						effective_sale_price = cust_order_product1.getFloat("effective_price");
						discount_product = cust_order_product1.getFloat("discounts");

					} else {

						Str_msg = "product deleviry status in order_detail not valid presant in this order";
						jsonObject_cancelorder_result.put("status", "0");
						jsonObject_cancelorder_result.put("message", Str_msg);
						return jsonObject_cancelorder_result;

					}
					cust_order_product1.close();
					stmt_cust1.close();

					String sql_orde = "SELECT * FROM customer_order_details where order_id='" + order_id+ "' and product_id!="+ product_id1;
				//	logger.log("\n sql_order_product_unic" + sql_orde);
					Statement stmt_cu = conn.createStatement();
					ResultSet cust_ord = stmt_cu.executeQuery(sql_orde);

					int cs = 0, co = 0, totalproductinorder = 0, orderCancel = 0,order_delivered=0,order_return_request=0,order_return_request_start=0,order_return=0;
					
					
					
					while (cust_ord.next()) {
						totalproductinorder++;
						
						
						
		     		float quantity1 = cust_ord.getInt("quantity");
					float sale_price1 = cust_ord.getFloat("price") * quantity1;														
					total_sub=total_sub+sale_price1;					
					float discount_product1 = cust_ord.getFloat("discounts");
					total_discount=total_discount+discount_product1;
						
						String status = cust_ord.getString("delivery_status_code");
						if (status.equalsIgnoreCase("shipped")) {
							cs++;
						} else if (status.equalsIgnoreCase("delivery_status_code")
								|| status.equalsIgnoreCase("Order placed")) {

							co++;
						} else if (status.equalsIgnoreCase("cancel")) {

							orderCancel++;
						}else if (status.equalsIgnoreCase("delivered")) {

							order_delivered++;
						}else if (status.equalsIgnoreCase("return_request") ) {

							order_return_request++;
						}else if (status.equalsIgnoreCase("return_proccess_start") ) {

							order_return_request_start++;
						}else if (status.equalsIgnoreCase("return") ) {

							order_return++;
						}

					}

					
					int temp=orderCancel+cs;
					int temp1=orderCancel+order_delivered;
					int temp2=orderCancel+order_return_request;
					
			
					String sql_order_update1 = null;
					if (totalproductinorder == cs||totalproductinorder==temp) {

						 sql_order_update1 = "UPDATE customer_orders SET order_status_code='shipped' WHERE order_id='"
								+ order_id + "'";		
						 
						//	logger.log("\n sql_order_update \n" + sql_order_update1);
							Statement stmt_order_del1 = conn.createStatement();
							stmt_order_del1.executeUpdate(sql_order_update1);
							stmt_order_del1.close();
							

					}
					else if(totalproductinorder==temp) {
						
						 sql_order_update1 = "UPDATE customer_orders SET order_status_code='shipped' WHERE order_id='"
									+ order_id + "'";		
							 
							//	logger.log("\n Hello sql_order_update \n" + sql_order_update1);
								Statement stmt_order_del1 = conn.createStatement();
								stmt_order_del1.executeUpdate(sql_order_update1);
								stmt_order_del1.close();		
						
					}else if(totalproductinorder == orderCancel) {
						
						String shipping="SELECT DISTINCT(vendor_id),shipping_charge FROM customer_order_details WHERE order_id=113";
						Statement stmt_ship = conn.createStatement();
						ResultSet total_ship = stmt_ship.executeQuery(shipping);
						
						while(total_ship.next()) {
							
							total_shipping=total_shipping+total_ship.getInt("shipping_charge");
							
						}
						total_ship.close();
						stmt_ship.close();
						
						total_sub=total_sub+sale_price;
						total_discount=total_discount+discount_product;
						total_grand=total_sub+total_shipping-total_discount;
						
						

					  sql_order_update1 = "UPDATE customer_orders SET order_status_code='cancel',sub_total="+total_sub+",shipping="+total_shipping+",grand_total="+total_grand+",discounts="+total_discount+"  WHERE order_id='"
								+ order_id + "'";
					  
					//	logger.log("\n sql_order_update \n" + sql_order_update1);
						Statement stmt_order_del1 = conn.createStatement();
						stmt_order_del1.executeUpdate(sql_order_update1);
						stmt_order_del1.close();
						
						sql_pro = "update products set quantity=quantity + " + quantity + " , stock='true' where id="
								+ product_id + " and vendor_id not in (SELECT id FROM admin WHERE is_external=1)";
					
						
						Statement stmt_cust_order_total = conn.createStatement();
						stmt_cust_order_total.executeUpdate(sql_pro);
						stmt_cust_order_total.close();
						
						
						float total=effective_sale_price+shipping_charge;	
						
						sql_refund_insert = "insert into refund(customer_id,order_id,order_number,refund_grant_total,refund_status,refund_type,product_id,order_cancel_reason,discounts,shipping)values"
								+ "('" + userid + "','" + order_id + "','" + order_number + "','" + total + "','"
								+ refund_status + "','" + refund_type + "','" + product_id + "','" + order_cancel_reason
								+ "'," + discount_product + "," + shipping_charge + ")";


						if (refund_payment_status.equalsIgnoreCase("done")) {
							Statement stmt_refund_insert = conn.createStatement();
							logger.log("\n stmt_refund_insert \n" + sql_refund_insert);
							int insert_add = stmt_refund_insert.executeUpdate(sql_refund_insert);
							stmt_refund_insert.close();

						} else {

							logger.log("\n we cant refund because your payment not done susscefully \n");
						}
						
						
						String sql_ordertraking_update = "UPDATE order_tracking SET status='cancel',expected_date_of_delivery=NULL  WHERE order_id='"
								+ order_id + "' and vendor_id='" + vendor_id1 + "'";

						logger.log("\n sql_ordertraking_update \n" + sql_ordertraking_update);
						Statement stmt_ordertracking_del = conn.createStatement();
						stmt_ordertracking_del.executeUpdate(sql_ordertraking_update);
						stmt_ordertracking_del.close();
						
						
						
						
						String sql_update_order_detail = "update customer_order_details set delivery_status_code='cancel',expected_date_of_delivery=NULL where order_number='"
								+ order_number + "' and product_id='" + product_id1 + "'";

						logger.log("\n sql_cust_order_product_del \n" + sql_update_order_detail);

						Statement stmt_cust_del = conn.createStatement();
						stmt_cust_del.executeUpdate(sql_update_order_detail);
						stmt_cust_del.close();			
						Str_msg = "Last product from Order cancel Successfully! Thank You !! ";
						jsonObject_cancelorder_result.put("status", "1");
						jsonObject_cancelorder_result.put("message", Str_msg);
						return jsonObject_cancelorder_result;

						
						

					}else if(order_delivered>0) {
						
						 sql_order_update1 = "UPDATE customer_orders SET order_status_code='delivered' WHERE order_id='"
									+ order_id + "'";		
							 
								logger.log("\n Hello sql_order_update \n" + sql_order_update1);
								Statement stmt_order_del1 = conn.createStatement();
								stmt_order_del1.executeUpdate(sql_order_update1);
								stmt_order_del1.close();		
						
					}
					else if(order_return_request>0) {
						
						 sql_order_update1 = "UPDATE customer_orders SET order_status_code='return_request' WHERE order_id='"
									+ order_id + "'";		
							 
								logger.log("\n Hello sql_order_update \n" + sql_order_update1);
								Statement stmt_order_del1 = conn.createStatement();
								stmt_order_del1.executeUpdate(sql_order_update1);
								stmt_order_del1.close();		
						
					}else if(order_return_request_start>0) {
						
						 sql_order_update1 = "UPDATE customer_orders SET order_status_code='return_request_start' WHERE order_id='"
									+ order_id + "'";		
							 
								logger.log("\n Hello sql_order_update \n" + sql_order_update1);
								Statement stmt_order_del1 = conn.createStatement();
								stmt_order_del1.executeUpdate(sql_order_update1);
								stmt_order_del1.close();		
						
					}


					
				
					
					
					

					sql_pro = "update products set quantity=quantity + " + quantity + " , stock='true' where id="
							+ product_id + " and vendor_id not in (SELECT id FROM admin WHERE is_external=1)";
								

					sql_cust_order_product_count = "SELECT * FROM customer_order_details where order_number='"
							+ order_number + "' and delivery_status_code in ('delivery_status_code','Order placed')";

					logger.log("\nsql_cust_order_product \n" + sql_cust_order_product_count);

					Statement stmt_cust_count = conn.createStatement();
					ResultSet cust_order_product_count = stmt_cust_count.executeQuery(sql_cust_order_product_count);

					ArrayList<Integer> arrlist_for_NA_products = new ArrayList<Integer>();

					while (cust_order_product_count.next()) {

						int vendor_id = cust_order_product_count.getInt("vendor_id");
						ved_id = vendor_id;

						if (order_product_vendor_id == vendor_id) {

							arrlist_for_NA_products.add(vendor_id);
							count++;
						}

					}
					logger.log("\n count " + count + "\n");
					logger.log("\n arrlist_for_NA_products " + arrlist_for_NA_products + "\n");

					cust_order_product_count.close();
					stmt_cust_count.close();

					Statement stmt_pro = conn.createStatement();
					stmt_pro.executeUpdate(sql_pro);
					stmt_pro.close();

					if (count == 1) {

						String sql_cust_order_product_shipped = "SELECT * FROM customer_order_details where order_number='"
								+ order_number + "' and delivery_status_code ='shipped' and vendor_id='"
								+ order_product_vendor_id + "'";

						Statement stmt_cust_shipped = conn.createStatement();
						ResultSet cust_order_product_shipped = stmt_cust_shipped
								.executeQuery(sql_cust_order_product_shipped);

						float total;
						String tracking_status="cancel";
						if (cust_order_product_shipped.next()) {

							total = effective_sale_price;
							shipping_charge = 0;
							tracking_status="shipped";
							
							

						}

						sql_order_update = "UPDATE customer_orders SET  sub_total=sub_total - " + sale_price
								+ " , grand_total=grand_total- " + effective_sale_price + "-" + shipping_charge
								+ " ,shipping=shipping-" + shipping_charge + ",discounts=discounts-" + discount_product
								+ " WHERE order_number='" + order_number + "'";

						total = effective_sale_price + shipping_charge;
						

						sql_refund_insert = "insert into refund(customer_id,order_id,order_number,refund_grant_total,refund_status,refund_type,product_id,order_cancel_reason,discounts,shipping)values"
								+ "('" + userid + "','" + order_id + "','" + order_number + "','" + total + "','"
								+ refund_status + "','" + refund_type + "','" + product_id + "','" + order_cancel_reason
								+ "'," + discount_product + "," + shipping_charge + ")";

						
						
						
						String sql_ordertraking_update;
						if(tracking_status.equalsIgnoreCase("cancel")) {
						
						 sql_ordertraking_update = "UPDATE order_tracking SET status='"+tracking_status+"',expected_date_of_delivery=NULL  WHERE order_id='"
								+ order_id + "' and vendor_id='" + vendor_id1 + "'";
						}else {
							
							 sql_ordertraking_update = "UPDATE order_tracking SET status='"+tracking_status+"'  WHERE order_id='"
									+ order_id + "' and vendor_id='" + vendor_id1 + "'";
							
						}
					
						
						
						//logger.log("\n sql_ordertraking_update \n" + sql_ordertraking_update);
						Statement stmt_ordertracking_del = conn.createStatement();
						stmt_ordertracking_del.executeUpdate(sql_ordertraking_update);
						stmt_ordertracking_del.close();

					
					
					
					
					} else {
						
						shipping_charge=0;

						sql_order_update = "UPDATE customer_orders SET  sub_total=sub_total - " + sale_price
								+ " , grand_total=grand_total- " + effective_sale_price + ",discounts=discounts-"
								+ discount_product + " WHERE order_number='" + order_number + "'";

						sql_refund_insert = "insert into refund(customer_id,order_id,order_number,refund_grant_total,refund_status,refund_type,product_id,order_cancel_reason,discounts,shipping)values"
								+ "('" + userid + "','" + order_id + "','" + order_number + "','" + effective_sale_price
								+ "','" + refund_status + "','" + refund_type + "','" + product_id + "','"
								+ order_cancel_reason + "'," + discount_product + "," + shipping_charge + ")";

					}

					//logger.log("\n sql_cust_order_product_del \n" + sql_order_update);

					String sql_update_order_detail = "update customer_order_details set delivery_status_code='cancel',expected_date_of_delivery=NULL where order_number='"
							+ order_number + "' and product_id='" + product_id1 + "'";

					//logger.log("\n sql_cust_order_product_del \n" + sql_update_order_detail);

					Statement stmt_cust_del = conn.createStatement();
					stmt_cust_del.executeUpdate(sql_update_order_detail);
					stmt_cust_del.close();

					Statement stmt_cust_order_total = conn.createStatement();
					stmt_cust_order_total.executeUpdate(sql_order_update);
					stmt_cust_del.close();

					if (refund_payment_status.equalsIgnoreCase("done")) {
						Statement stmt_refund_insert = conn.createStatement();
						//logger.log("\n stmt_refund_insert \n" + sql_refund_insert);
						int insert_add = stmt_refund_insert.executeUpdate(sql_refund_insert);
						stmt_refund_insert.close();

					} else {

						logger.log("\n we cant refund because your payment not done susscefully \n");
					}

				}
				cust_order_product1.close();
				stmt_cust1.close();

			}

			else {
				// -------------------------------------------------------------this section for
				// complate order
				// cancel----------------------------------------------------------------------------------------------

				logger.log("\n flag :" + flag + "\n");

				String sql_order_product1 = "SELECT id FROM customer_order_details where order_id='" + order_id
						+ "' and delivery_status_code='shipped' limit 1";

				Statement stmt_cust1 = conn.createStatement();
				ResultSet cust_order_product1 = stmt_cust1.executeQuery(sql_order_product1);

				if (cust_order_product1.next()) {

					Str_msg = "some part of order's products is/are already shipped so you can't cancel order";
					jsonObject_cancelorder_result.put("status", "0");
					jsonObject_cancelorder_result.put("message", Str_msg);
					return jsonObject_cancelorder_result;

				}
				cust_order_product1.close();
				stmt_cust1.close();

				String Sql_order_cancel = "select * from customer_orders where order_id='" + order_id + "'";
				//logger.log("\n Sql_order_cancel \n" + Sql_order_cancel);
				Statement stmt = conn.createStatement();
				ResultSet srs_order_cancel = stmt.executeQuery(Sql_order_cancel);

				if (srs_order_cancel.next() == false) {

					Str_msg = "order is not present";
					jsonObject_cancelorder_result.put("status", "0");
					jsonObject_cancelorder_result.put("message", Str_msg);
					return jsonObject_cancelorder_result;
				} else if (srs_order_cancel.getString("order_status_code").equalsIgnoreCase("cancel")) {

					Str_msg = "order alredy canceled";
					jsonObject_cancelorder_result.put("status", "0");
					jsonObject_cancelorder_result.put("message", Str_msg);
					return jsonObject_cancelorder_result;

				} else if (srs_order_cancel.getString("order_status_code").equalsIgnoreCase("shipped")) {
					Str_msg = "order alredy shipped";
					jsonObject_cancelorder_result.put("status", "0");
					jsonObject_cancelorder_result.put("message", Str_msg);
					return jsonObject_cancelorder_result;

				} else {

					order_number = srs_order_cancel.getString("order_number");
					refund_transaction_id = srs_order_cancel.getString("transaction_id");
					refund_payment_status = srs_order_cancel.getString("payment_status");
					refund_mode_of_payment = srs_order_cancel.getString("mode_of_payment");
					refund_date_of_order_paid = srs_order_cancel.getString("date_of_order_paid");
					shipping_address_id = srs_order_cancel.getInt("delivery_address_id");
					refund_sale_price = srs_order_cancel.getFloat("sub_total");
					refund_grant_total = srs_order_cancel.getFloat("grand_total");
					refund_shipping_charge = srs_order_cancel.getFloat("shipping");

					srs_order_cancel.getFloat("tax");
					refund_shipping_discounts = srs_order_cancel.getFloat("discounts");

				}
				srs_order_cancel.close();
				stmt.close();

				// Update product quantity when entire order cancel

				String sql_order_product = "SELECT * FROM customer_order_details where order_id='" + order_id
						+ "' and delivery_status_code!='cancel'";

				//logger.log("\n sql_order_product \n" + sql_order_product);

				Statement stmt_cust_order = conn.createStatement();
				ResultSet cust_order_products = stmt_cust_order.executeQuery(sql_order_product);

				StringBuilder str_product_id = new StringBuilder();

				while (cust_order_products.next()) {

					int productid_order = cust_order_products.getInt("product_id");
					str_product_id.append(productid_order);
					str_product_id.append(",");

					int quantity1 = cust_order_products.getInt("quantity");

					sql_pro = "update products set quantity=quantity + " + quantity1 + " , stock='true' where id="
							+ productid_order + " and vendor_id not in (SELECT id FROM admin WHERE is_external=1)";

					//logger.log("\n sql_pro \n" + sql_pro);
					Statement stmt_order_product_update = conn.createStatement();
					stmt_order_product_update.executeUpdate(sql_pro);
					stmt_order_product_update.close();

				}

				String refund_products_id = str_product_id.toString().substring(0,
						str_product_id.toString().length() - 1);
				//logger.log("\n String Array : " + refund_products_id);

				cust_order_products.close();
				stmt_cust_order.close();

				sql_order_update = "UPDATE customer_orders SET order_status_code='cancel'  WHERE order_id='" + order_id
						+ "'";

				//logger.log("\n sql_order_update \n" + sql_order_update);
				Statement stmt_order_del = conn.createStatement();
				stmt_order_del.executeUpdate(sql_order_update);
				stmt_order_del.close();

				// Cancel from tracking table

				String sql_ordertraking_update = "UPDATE order_tracking SET status='cancel',expected_date_of_delivery=NULL  WHERE order_id='"
						+ order_id + "'";

				//logger.log("\n sql_ordertraking_update \n" + sql_ordertraking_update);
				Statement stmt_ordertracking_del = conn.createStatement();
				stmt_ordertracking_del.executeUpdate(sql_ordertraking_update);
				stmt_ordertracking_del.close();

				// Insert Data for refund

				if (refund_payment_status.equalsIgnoreCase("Done")) {

					Statement stmt_refund_insert = conn.createStatement();

					sql_refund_insert = "insert into refund(customer_id,order_id,order_number,refund_grant_total,refund_status,refund_type,product_id,order_cancel_reason,discounts,shipping)values"
							+ "('" + userid + "','" + order_id + "','" + order_number + "','" + refund_grant_total
							+ "','" + refund_status + "','" + refund_type + "','" + refund_products_id + "','"
							+ order_cancel_reason + "'," + refund_shipping_discounts + "," + refund_shipping_charge
							+ ")";

					//logger.log("\n stmt_refund_insert \n" + sql_refund_insert);
					int insert_add = stmt_refund_insert.executeUpdate(sql_refund_insert);

					stmt_refund_insert.close();
				}

				String sql_order_product_cancel1 = "SELECT * FROM customer_order_details where order_id='" + order_id
						+ "' and delivery_status_code='cancel'";

				Statement stmt_cust_cancel1 = conn.createStatement();
				ResultSet cust_order_product_cancel1 = stmt_cust_cancel1.executeQuery(sql_order_product_cancel1);

				float sale_price_total = 0, discount_add = 0, shipping_total = 0;
				int flag_1 = 0;
				while (cust_order_product_cancel1.next()) {
					flag_1 = 1;

					int product_id_refund = cust_order_product_cancel1.getInt("product_id");
					float sale_price_ref = cust_order_product_cancel1.getFloat("price");
					int product_quantity = cust_order_product_cancel1.getInt("quantity");

					float effective = cust_order_product_cancel1.getFloat("effective_price");
					float product_discount = cust_order_product_cancel1.getFloat("discounts");

					sale_price_total = sale_price_ref * product_quantity;

					String sql_order_product_refund = "SELECT * FROM refund where order_id='" + order_id
							+ "' and product_id='" + product_id_refund + "'";

					Statement stmt_cust_refund = conn.createStatement();
					ResultSet cust_order_product_refund = stmt_cust_refund.executeQuery(sql_order_product_refund);

					if (cust_order_product_refund.next()) {

						float refunf_grant_total = cust_order_product_refund.getFloat("refund_grant_total");
						// float shipping =cust_order_product_refund.getFloat("shipping");
						// discount_add=discount_add+product_wise_discount;
						// float product_wise_discount =cust_order_product_refund.getFloat("discounts");
						// shipping_charge=shipping_charge+shipping;

						//logger.log("refund_grant_total" + refunf_grant_total);
						// logger.log("refund_grant_total" + discount_add);
						// logger.log("refund_grant_total" + shipping_charge);

						if (refunf_grant_total - effective == 0) {

							shipping_total = 0;

						} else {

							shipping_total = refunf_grant_total - effective;

						}

						//logger.log("\n shipping_total \n " + shipping_total);

						//logger.log("\n shipping_charge \n " + shipping_charge);

					}
					cust_order_product_refund.close();
					stmt_cust_refund.close();

					String sql_order_update1 = "UPDATE customer_orders SET  sub_total=sub_total + " + sale_price_total
							+ " , grand_total=grand_total+ " + effective + "+" + shipping_total + ",shipping=shipping+"
							+ shipping_total + ",discounts=discounts +" + product_discount + " WHERE order_id='"
							+ order_id + "'";

					//logger.log("\n sql_order_update \n" + sql_order_update1);
					Statement stmt_order_del1 = conn.createStatement();
					stmt_order_del1.executeUpdate(sql_order_update1);
					stmt_order_del1.close();

				}
				cust_order_product_cancel1.close();
				stmt_cust_cancel1.close();

				// Update customer_order_details_table when entire order cancel

				String sql_update_order_detail = "update customer_order_details set delivery_status_code='cancel',expected_date_of_delivery=NULL where order_id='"
						+ order_id + "'";

				//logger.log("\n sql_cust_order_product_del \n" + sql_update_order_detail);

				Statement stmt_cust_del = conn.createStatement();
				stmt_cust_del.executeUpdate(sql_update_order_detail);
				stmt_cust_del.close();

				// Update customer_order_table when entire order cancel

			}
		} catch (

		Exception e) {
			e.printStackTrace();
			logger.log(e.toString());
			JSONObject jo_catch = new JSONObject();
			jo_catch.put("Exception", e.getMessage());
			return jo_catch;

		} finally {
			if (conn != null) {
				try {
					if (!conn.isClosed()) {

						conn.close();
					}
				} catch (Exception e) {
					e.printStackTrace();
					JSONObject jo_catch = new JSONObject();
					jo_catch.put("Exception", e.getMessage());
					return jo_catch;
				}
			}
		}

		Str_msg = "Order cancel Successfully! Thank You !! ";
		jsonObject_cancelorder_result.put("status", "1");
		jsonObject_cancelorder_result.put("message", Str_msg);
		return jsonObject_cancelorder_result;

	}

}
