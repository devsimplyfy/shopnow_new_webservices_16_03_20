package com.shopNow.Lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Properties;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
//import org.springframework.jdbc.support.rowset.SqlRowSet;

public class Shop_now_Cart_AddItem implements RequestHandler<JSONObject, JSONObject> {
	private String USERNAME;
	private String PASSWORD;
	private String DB_URL;

	@SuppressWarnings({ "unchecked", "unused" })
	public JSONObject handleRequest(JSONObject input, Context context) {

		LambdaLogger logger = context.getLogger();

		JSONObject errorPayload = new JSONObject();

		if (!input.containsKey("userid")) {
			errorPayload.put("errorType", "BadRequest");
			errorPayload.put("httpStatus", 400);
			errorPayload.put("requestId", context.getAwsRequestId());
			errorPayload.put("message", "JSON Input Object request key named 'userid' is missing");
			throw new RuntimeException(errorPayload.toJSONString());
		}
		if (!input.containsKey("product_id")) {
			errorPayload.put("errorType", "BadRequest");
			errorPayload.put("httpStatus", 400);
			errorPayload.put("requestId", context.getAwsRequestId());
			errorPayload.put("message", "JSON Input Object request key named 'product_id' is missing");
			throw new RuntimeException(errorPayload.toJSONString());
		}
		if (!input.containsKey("quantity")) {
			errorPayload.put("errorType", "BadRequest");
			errorPayload.put("httpStatus", 400);
			errorPayload.put("requestId", context.getAwsRequestId());
			errorPayload.put("message", "JSON Input Object request key named 'quantity' is missing");
			throw new RuntimeException(errorPayload.toJSONString());
		}
		if (!input.containsKey("device_id")) {
			errorPayload.put("errorType", "BadRequest");
			errorPayload.put("httpStatus", 400);
			errorPayload.put("requestId", context.getAwsRequestId());
			errorPayload.put("message", "JSON Input Object request key named 'device_id' is missing");
			throw new RuntimeException(errorPayload.toJSONString());
		}

		JSONArray cart_Add_array = new JSONArray();
		JSONObject jsonObject_cartAdd_result = new JSONObject();

		Object userid1 = input.get("userid");
		Object product_id1 = input.get("product_id");
		Object quantity1 = input.get("quantity");
		Object device_id1 = input.get("device_id");
		String device_id = device_id1.toString();
		String vendor_id = null;
		long userid;
		int quantity;
		long product_id;
		String Str_msg;
		JSONObject jo_cartInsert = new JSONObject();
		Connection conn = null;

		if (userid1 == null || userid1 == "") {
			userid = 0;
		} else {

			userid = Long.parseLong(userid1.toString());
		}

		if (product_id1 == null || product_id1 == "") {

			product_id = 0;
		} else {
			product_id = Long.parseLong(product_id1.toString());

		}
		if (quantity1 == null || quantity1 == "") {
			quantity = 0;
			Str_msg = "Cart_Item not Inserted because quantity is not valid";
			jsonObject_cartAdd_result.put("status", "0");
			jsonObject_cartAdd_result.put("message", Str_msg);
			return jsonObject_cartAdd_result;

		} else {
			quantity = Integer.parseInt(quantity1.toString());
		}
		if ((device_id1 == null || device_id1 == "") && userid == 0) {

			Str_msg = "Please Enter either UserId or Device_id";
			jo_cartInsert.put("status", "0");
			jo_cartInsert.put("message", Str_msg);
			return jo_cartInsert;
		}

		Properties prop = new Properties();
		try {
			prop.load(getClass().getResourceAsStream("/application.properties"));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// Get time from DB server
		try {
			DB_URL = prop.getProperty("url");
			USERNAME = prop.getProperty("username");
			PASSWORD = prop.getProperty("password");
			 conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);

			Statement stmt_vendor = conn.createStatement();
			ResultSet resultSet_vendor = stmt_vendor.executeQuery("SELECT * FROM admin WHERE is_external='0'");

			int vendo_id = 0, vendor_id_isexternal = 0;

			ArrayList<Integer> arrlist = new ArrayList<Integer>();
			while (resultSet_vendor.next()) {

				vendo_id++;
				vendor_id_isexternal = resultSet_vendor.getInt("id");
				arrlist.add(vendor_id_isexternal);
			}

			resultSet_vendor.close();
			stmt_vendor.close();

			Statement stmt_products = conn.createStatement();
			ResultSet resultSet_products = stmt_products
					.executeQuery("SELECT * FROM products WHERE id='" + product_id +"'");

			if (resultSet_products.next()) {

				vendor_id = resultSet_products.getString("vendor_id");
				vendor_id_isexternal = Integer.parseInt(vendor_id);

			}
			else {
				Str_msg = " product id is not valid !";
				jo_cartInsert.put("status", "0");
				jo_cartInsert.put("message", Str_msg);
				conn.close();
				return jo_cartInsert;
						
			}

			resultSet_vendor.close();
			stmt_vendor.close();
			

			Statement stmt1_productid_v = conn.createStatement();
			Statement stmt1_productid = conn.createStatement();
			String sql_cart_add;
			ResultSet resultSet_productId_v;
			ResultSet resultSet_productId;

			if (arrlist.contains(vendor_id_isexternal)) {
				sql_cart_add = "SELECT id,stock,quantity FROM products where id='" + product_id + "' AND stock='true'";

						resultSet_productId_v = stmt1_productid_v.executeQuery(sql_cart_add);
						if (resultSet_productId_v.next() == false) {

							Str_msg = "product is currently not available";
							jo_cartInsert.put("status", "0");
							jo_cartInsert.put("message", Str_msg);
							conn.close();
							return jo_cartInsert;

						}
						else if(resultSet_productId_v.getInt("quantity")<quantity){
							
							Str_msg = "Currently available product quantity for "+resultSet_productId_v.getInt("id")+" is "+resultSet_productId_v.getInt("quantity");
							jo_cartInsert.put("status", "0");
							jo_cartInsert.put("message", Str_msg);
							conn.close();
							return jo_cartInsert;
								
						}	
			

			} else {

				sql_cart_add = "SELECT * FROM products where id='" + product_id + "' AND stock='true'";
				resultSet_productId = stmt1_productid.executeQuery(sql_cart_add);

			if (resultSet_productId.next() == false) {

				Str_msg = "Product is currently out of stock";
				jo_cartInsert.put("status", "0");
				jo_cartInsert.put("message", Str_msg);
				conn.close();
				return jo_cartInsert;

			}
		}
			if (userid == 0) {

				String sql2 = "SELECT UserId FROM cart_items where productId='" + product_id + "' and device_id='"
						+ device_id + "' and userId='0'";

				Statement stmt2 = conn.createStatement();
				ResultSet Cart_product_id = stmt2.executeQuery(sql2);

				if (Cart_product_id.next()) {
					long uid = Cart_product_id.getLong("UserId");
					if (uid > 0) {
						String sql3 = "INSERT  INTO cart_items (device_id,UserId,ProductId,VendorId,Quantity) VALUES('"
								+ device_id + "','" + userid + "','" + product_id + "','" + vendor_id + "'," + quantity
								+ ") ON DUPLICATE KEY UPDATE  Quantity =" + quantity;

						Statement stmt3 = conn.createStatement();
						int i = stmt3.executeUpdate(sql3);

						if (i > 0) {
							Str_msg = "CartItem inserted successfully";
							jo_cartInsert.put("status", "1");
							jo_cartInsert.put("message", Str_msg);

						}

						else {
							Str_msg = "Cart_Item not Inserted";
							jo_cartInsert.put("status", "0");
							jo_cartInsert.put("message", Str_msg);

						}

						return jo_cartInsert;

					}

					Str_msg = "product is present in cart please call Cart/update web service";
					jo_cartInsert.put("status", "0");
					jo_cartInsert.put("message", Str_msg);
					conn.close();
					return jo_cartInsert;

				} else {

					String sql4 = "INSERT  INTO cart_items (device_id,UserId,ProductId,VendorId,Quantity) VALUES('"
							+ device_id + "','" + userid + "','" + product_id + "','" + vendor_id + "'," + quantity
							+ ") ON DUPLICATE KEY UPDATE  Quantity =" + quantity;

					Statement stmt4 = conn.createStatement();
					int i = stmt4.executeUpdate(sql4);

					if (i > 0) {
						Str_msg = "CartItem inserted successfully";
						jo_cartInsert.put("status", "1");
						jo_cartInsert.put("message", Str_msg);

					}

					else {
						Str_msg = "Cart_Item not Inserted";
						jo_cartInsert.put("status", "0");
						jo_cartInsert.put("message", Str_msg);

					}
					conn.close();
					return jo_cartInsert;

				}

			} else {

				String sql5 = "SELECT id,status FROM customers where id='" + userid + "'";
				Statement stmt5 = conn.createStatement();
				ResultSet srs_customer_id = stmt5.executeQuery(sql5);

		
				String sql6 = "SELECT * FROM cart_items where UserId='" + userid + "' and ProductId=" + product_id;
				Statement stmt6 = conn.createStatement();
				ResultSet user_item_cart = stmt6.executeQuery(sql6);

			
				
				String sql7 = "SELECT * FROM cart_items where UserId=0  and ProductId='" + product_id
						+ "' and device_id='" + device_id + "'";
				Statement stmt7 = conn.createStatement();
				ResultSet user_item_cart1 = stmt7.executeQuery(sql7);

				if (srs_customer_id.next() == false) {

					Str_msg = "No User Found !";
					jo_cartInsert.put("status", "0");
					jo_cartInsert.put("message", Str_msg);
					conn.close();
					return jo_cartInsert;

				}
				else if(!srs_customer_id.getString("status").equalsIgnoreCase("1")) {
					
					Str_msg = "User not confirmed !";
					jo_cartInsert.put("status", "0");
					jo_cartInsert.put("message", Str_msg);
					conn.close();
					return jo_cartInsert;
					
					
				}

				// ------------------------------------------

				else if (user_item_cart.next()) {

					Str_msg = "product is present in cart please call Cart/update web service";
					jo_cartInsert.put("status", "0");
					jo_cartInsert.put("message", Str_msg);
					conn.close();
					return jo_cartInsert;

				}

				// --------------------------------------------
				else if (user_item_cart1.next()) {

					String sql8 = "UPDATE cart_items SET UserId='" + userid + "' WHERE ProductId='" + product_id
							+ "' AND device_id='" + device_id + "' and UserId = 0";

					Statement stmt8 = conn.createStatement();
					boolean i = stmt8.execute(sql8);

					// jdbcTemplate.execute(sql1);

					Str_msg = "CartItem update successfully";
					jo_cartInsert.put("status", "1");
					jo_cartInsert.put("message", Str_msg);
					conn.close();
					return jo_cartInsert;

				}

				// ---------------------------------------------------

				else {

					String sql9 = "INSERT  INTO cart_items (device_id,UserId,ProductId,VendorId,Quantity) VALUES('"
							+ device_id + "','" + userid + "','" + product_id + "','" + vendor_id + "','" + quantity
							+ "') ON DUPLICATE KEY UPDATE  Quantity ='" + quantity + "'";

					Statement stmt9 = conn.createStatement();
					int i = stmt9.executeUpdate(sql9);

					if (i > 0) {
						Str_msg = "CartItem inserted successfully";
						jo_cartInsert.put("status", "1");
						jo_cartInsert.put("message", Str_msg);

					}

					else {
						Str_msg = "Cart_Item not Inserted";
						jo_cartInsert.put("status", "0");
						jo_cartInsert.put("message", Str_msg);

					}

				}
				conn.close();
				return jo_cartInsert;

			}


		} catch (Exception e) {
			

			logger.log("Exception " + e);
			JSONObject jo_catch = new JSONObject();
			jo_catch.put("Exception", e.getMessage());
			return jo_catch;

		}
		finally {

			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		

	}

}
