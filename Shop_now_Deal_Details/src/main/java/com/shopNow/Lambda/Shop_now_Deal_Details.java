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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Shop_now_Deal_Details implements RequestHandler<JSONObject, JSONObject> {
	private String USERNAME;
	private String PASSWORD;
	private String DB_URL;

	@SuppressWarnings({ "unchecked", "unused" })
	public JSONObject handleRequest(JSONObject input, Context context) {

		LambdaLogger logger = context.getLogger();
		logger.log("Invoked JDBCSample.getCurrentTime");
		JSONObject errorPayload = new JSONObject();

		if (!input.containsKey("deal_id")) {
			errorPayload.put("errorType", "BadRequest");
			errorPayload.put("httpStatus", 400);
			errorPayload.put("requestId", context.getAwsRequestId());
			errorPayload.put("message", "JSON Input Object request key named 'dealid' is missing");
			throw new RuntimeException(errorPayload.toJSONString());
		}
		/*
		if (!input.containsKey("product_id")) {
			errorPayload.put("errorType", "BadRequest");
			errorPayload.put("httpStatus", 400);
			errorPayload.put("requestId", context.getAwsRequestId());
			errorPayload.put("message", "JSON Input Object request key named 'product_id' is missing");
			throw new RuntimeException(errorPayload.toJSONString());
		}
		if (!input.containsKey("category_id")) {
			errorPayload.put("errorType", "BadRequest");
			errorPayload.put("httpStatus", 400);
			errorPayload.put("requestId", context.getAwsRequestId());
			errorPayload.put("message", "JSON Input Object request key named 'category_id' is missing");
			throw new RuntimeException(errorPayload.toJSONString());
		}
*/
		Object deal_id = input.get("deal_id");
	   //Object product_id = input.get("product_id");
	   //Object device_id1 = input.get("category_id");
		
		
		
		long deal_id1;
		
		
		Connection conn = null;
		String Str_msg;
		
		JSONArray Deal_product_array = new JSONArray();
		JSONObject jo_deal_detail_final = new JSONObject();

		if (deal_id == null || deal_id == "") {
		
			Str_msg = "No deal_id Entered";
			jo_deal_detail_final.put("status", "0");
			jo_deal_detail_final.put("message", Str_msg);
			return jo_deal_detail_final;
			
		} else {

			deal_id1 = Long.parseLong(deal_id.toString());
		}



	/*	if (product_id == null || product_id == "") {

			Str_msg = "No product_id Entered";
			jo_deal_detail_final.put("status", "0");
			jo_deal_detail_final.put("message", Str_msg);

			return jo_deal_detail_final;
		}

	*/
		// Get time from DB server
		Properties prop = new Properties();
		try {
			prop.load(getClass().getResourceAsStream("/application.properties"));
			DB_URL = prop.getProperty("url");
			USERNAME = prop.getProperty("username");
			PASSWORD = prop.getProperty("password");
			conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
			
			
			String deal_detail_test1 = "SELECT * FROM deals WHERE id='"+deal_id+"'";
			Statement stmt_deal_test1 = conn.createStatement();
			ResultSet deal_resultSet_test1 = stmt_deal_test1.executeQuery(deal_detail_test1);
			
			if(deal_resultSet_test1.next()==false) {
				
				jo_deal_detail_final.put("status", "0");
				jo_deal_detail_final.put("message","Deal_id not valid");
				return jo_deal_detail_final;
			}
			deal_resultSet_test1.close();
			stmt_deal_test1.close();
			
			
			
			
			
			
			String deal_detail_test = "SELECT * FROM deal_detail WHERE deal_id='"+deal_id+"'";
			Statement stmt_deal_test = conn.createStatement();
			ResultSet deal_resultSet_test = stmt_deal_test.executeQuery(deal_detail_test);
			
			if(deal_resultSet_test.next()==false) {
				
				jo_deal_detail_final.put("status", "0");
				jo_deal_detail_final.put("message","currently Deal_id not apply any products");
				return jo_deal_detail_final;
			}
			deal_resultSet_test.close();
			stmt_deal_test.close();
		
			
		
			String deal_detail = "SELECT p.id,p.name,p.regular_price,p.sale_price,p.stock,p.image,deals.start_date,deals.end_date,deals.name AS dealname FROM products AS p RIGHT JOIN deal_detail AS d ON p.id=d.product_id  INNER JOIN deals ON d.deal_id=deals.id WHERE deals.id='"+deal_id+"'";
			logger.log("\n deal_detail "+deal_detail);
			
			Statement stmt_deal = conn.createStatement();
			ResultSet deal_resultSet = stmt_deal.executeQuery(deal_detail);

			
			
			
			while(deal_resultSet.next()) {
			jo_deal_detail_final.put("deal_name", deal_resultSet.getString("dealname"));	
			JSONObject jo_deal_detail = new JSONObject();
			jo_deal_detail.put("id", deal_resultSet.getString("id"));
			jo_deal_detail.put("name", deal_resultSet.getString("name"));
			jo_deal_detail.put("regular_price", deal_resultSet.getFloat("regular_price"));
			jo_deal_detail.put("sale_price", deal_resultSet.getFloat("sale_price"));
			jo_deal_detail.put("stock", deal_resultSet.getString("stock"));
			jo_deal_detail.put("image", deal_resultSet.getString("image"));
			Deal_product_array.add(jo_deal_detail);

			// -------------code for time remaining---------------------------------------------------

			SimpleDateFormat f1 = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
			String s1 = null;
			Date d2, d3 = null;

			String dateStop = deal_resultSet.getString("end_date");
			String dateStop1 = dateStop.substring(0, dateStop.length() - 3);
			long dateStop2 = Long.parseLong(dateStop1);
			String edate = f1.format(new java.util.Date(dateStop2 * 1000));

			String current = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(Calendar.getInstance().getTime());

			
				d2 = f1.parse(edate);
				d3 = f1.parse(current);

				// in milliseconds
				long diff = d2.getTime() - d3.getTime();

				if (diff > 0) {

					long diffSeconds = diff / 1000 % 60;
					long diffMinutes = diff / (60 * 1000) % 60;
					long diffHours = diff / (60 * 60 * 1000) % 24;
					long diffDays = diff / (24 * 60 * 60 * 1000);

					s1 = Long.toString(diffDays) + " days " + Long.toString(diffHours) + " hours "
							+ Long.toString(diffMinutes) + " minutes " + Long.toString(diffSeconds) + " seconds ";

					jo_deal_detail_final.put("time_remaining", s1);
				} else {
					jo_deal_detail_final.put("time_remaining", "deal is over");
				}
				
			}
			
			jo_deal_detail_final.put("product", Deal_product_array);
			
				

			conn.close();
		} catch (Exception e) {

			logger.log("Exception " + e);
			jo_deal_detail_final.put("status", "0");
			jo_deal_detail_final.put("message", e.getMessage());
		} finally {
			if (conn != null) {
				try {
					if (!conn.isClosed()) {

						conn.close();
					}
				} catch (Exception e) {
					e.printStackTrace();
					logger.log("Exception"+e.toString());
					JSONObject jo_catch = new JSONObject();
					jo_catch.put("Exception", e.getMessage());
					return jo_catch;
				}
			}
		}
		return jo_deal_detail_final;
	}
}
