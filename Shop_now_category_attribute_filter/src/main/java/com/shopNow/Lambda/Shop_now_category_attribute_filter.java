package com.shopNow.Lambda;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Shop_now_category_attribute_filter implements RequestHandler<JSONObject, JSONObject> {

	private String USERNAME;
	private String PASSWORD;
	private String DB_URL;

	@SuppressWarnings("unchecked")
	public JSONObject handleRequest(JSONObject j, Context context) {
		JSONObject errorPayload = new JSONObject();

		if (!j.containsKey("category_id")) {
			errorPayload.put("errorType", "BadRequest");
			errorPayload.put("httpStatus", 400);
			errorPayload.put("requestId", context.getAwsRequestId());
			errorPayload.put("message", "JSON Input Object request key named 'category_id' is missing");
			throw new RuntimeException(errorPayload.toJSONString());
		}

		LambdaLogger logger = context.getLogger();
		String category_id = j.get("category_id").toString();
	
/*		String min_price=j.get("min_price").toString();
		String max_price=j.get("max_price").toString();
	
		
		if(min_price.equalsIgnoreCase("")||min_price.equalsIgnoreCase("null") && max_price.equalsIgnoreCase("")) {
			
			
			
		}
		
		
		String filtername=min_price+"_"+max_price;
		
	*/	
		

		int flag = 0;

		if (category_id.equalsIgnoreCase("null") || category_id.equalsIgnoreCase("")) {

			flag = 1;

		} else {
			flag = 0;

		}
		JSONObject jsonObject_filter = new JSONObject();

		ResultSet resultSet1 = null, resultSet_option = null;
		Statement stmt2 = null, stmt_option = null;
		Connection con = null;

		Properties prop = new Properties();

		try {
			prop.load(getClass().getResourceAsStream("/application.properties"));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			logger.log("Caught exception: " + e1);
		}

		DB_URL = prop.getProperty("url");
		USERNAME = prop.getProperty("username");
		PASSWORD = prop.getProperty("password");

		try {
			con = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
		} catch (SQLException e1) {

			e1.printStackTrace();
			logger.log("Caught exception: " + e1);
			JSONObject jo_catch = new JSONObject();
			jo_catch.put("Exception", e1.getMessage());
			return jo_catch;
		}

		try {
			String sql_category_check = null;
			if (flag == 0) {
				sql_category_check = "select * from categories where id='" + category_id + "'";
				Statement stmt_category_check = con.createStatement();
				ResultSet resultSet_category_check = stmt_category_check.executeQuery(sql_category_check);

				if (!resultSet_category_check.next()) {

					jsonObject_filter.put("status", "0");
					jsonObject_filter.put("msg", "Category not found");
					return jsonObject_filter;

				}
			}

			String sql_product_att = null;
			if (flag == 1) {
				sql_product_att = "SELECT attributes_value.*,attributes.att_group_name,attributes.status FROM attributes_value LEFT JOIN attributes ON attributes_value.att_group_id=attributes.id ORDER BY attributes_value.att_group_id ASC";
			} else {

				sql_product_att = "SELECT attributes_value.*,attributes.att_group_name,attributes.status FROM attributes_value LEFT JOIN attributes ON attributes_value.att_group_id=attributes.id WHERE attributes_value.id IN (SELECT product_attributes.att_group_val_id FROM products INNER JOIN product_attributes ON products.id=product_attributes.product_id WHERE products.category_id='"
						+ category_id + "') ORDER BY attributes_value.att_group_id ASC";

			}
			// logger.log("\n Invoked products attribute \n" + sql_product_att);

			stmt2 = con.createStatement();
			resultSet1 = stmt2.executeQuery(sql_product_att);
			// logger.log("\n");
			JSONArray attribute_array = new JSONArray();
			JSONArray cat_att_value_arry = new JSONArray();
			JSONObject jsonObject_attribute = new JSONObject();

			int count = 0;
			int to_check = 0;
			while (resultSet1.next()) {
				if (count == 0) {
					count = count + 1;
					to_check = resultSet1.getInt("att_group_id");
					jsonObject_attribute.put("atgrpname", resultSet1.getString("att_group_name"));
					jsonObject_attribute.put("atgrpid", resultSet1.getString("att_group_id"));

					JSONObject jsonObject_attribute_1 = new JSONObject();

					jsonObject_attribute_1.put("attvalname", resultSet1.getString("att_value"));
					jsonObject_attribute_1.put("attvalid", resultSet1.getString("id"));

					cat_att_value_arry.add(jsonObject_attribute_1);

				} else {
					if (to_check == resultSet1.getInt("att_group_id")) {
						JSONObject jsonObject_attribute_1 = new JSONObject();

						jsonObject_attribute.put("atgrpname", resultSet1.getString("att_group_name"));
						jsonObject_attribute.put("atgrpid", resultSet1.getString("att_group_id"));

						jsonObject_attribute_1.put("attvalname", resultSet1.getString("att_value"));
						jsonObject_attribute_1.put("attvalid", resultSet1.getString("id"));

						cat_att_value_arry.add(jsonObject_attribute_1);

					} else {

						jsonObject_attribute.put("attributeval", cat_att_value_arry);
						attribute_array.add(jsonObject_attribute);
						to_check = resultSet1.getInt("att_group_id");

						jsonObject_attribute = new JSONObject();
						cat_att_value_arry = new JSONArray();

						jsonObject_attribute.put("atgrpname", resultSet1.getString("att_group_name"));
						jsonObject_attribute.put("atgrpid", resultSet1.getString("att_group_id"));

						JSONObject jsonObject_attribute_1 = new JSONObject();

						jsonObject_attribute_1.put("attvalname", resultSet1.getString("att_value"));
						jsonObject_attribute_1.put("attvalid", resultSet1.getString("id"));

						cat_att_value_arry.add(jsonObject_attribute_1);

					}
				}
			}

			jsonObject_attribute.put("attributeval", cat_att_value_arry);
			attribute_array.add(jsonObject_attribute);
			jsonObject_filter.put("attribute", attribute_array);

			resultSet1.close();
			stmt2.close();

			String sql_product_option = null;
			if (flag == 1) {

				sql_product_option = "SELECT product_option_value.id,product_option_value.value,product_option_value.group_id,product_option_group.option_name,product_option_group.status FROM product_option_value LEFT JOIN product_option_group ON product_option_value.group_id=product_option_group.id ORDER BY product_option_value.group_id ASC";
			} else {

				sql_product_option = "SELECT product_option_value.id,product_option_value.value,product_option_value.group_id,product_option_group.option_name,product_option_group.status FROM product_option_value LEFT JOIN product_option_group ON product_option_value.group_id=product_option_group.id WHERE product_option_value.id IN (SELECT product_options.opt_group_val_id FROM products INNER JOIN product_options ON products.id=product_options.product_id WHERE products.category_id='"
						+ category_id + "') ORDER BY product_option_value.group_id ASC";

			}
			// logger.log("\n Invoked products attribute \n" + sql_product_option);

			stmt_option = con.createStatement();
			resultSet_option = stmt_option.executeQuery(sql_product_option);
			// logger.log("\n");

			JSONArray option_array = new JSONArray();
			JSONArray cat_option_value_arry = new JSONArray();
			JSONObject jsonObject_options = new JSONObject();

			int count_opt = 0;
			int to_check_opt = 0;
			while (resultSet_option.next()) {
				if (count_opt == 0) {
					count_opt = count_opt + 1;
					to_check_opt = resultSet_option.getInt("group_id");
					jsonObject_options.put("optgrpname", resultSet_option.getString("option_name"));
					jsonObject_options.put("optgrpid", resultSet_option.getString("group_id"));

					JSONObject jsonObject_options_1 = new JSONObject();

					jsonObject_options_1.put("optvalname", resultSet_option.getString("value"));
					jsonObject_options_1.put("optvalid", resultSet_option.getString("id"));

					cat_option_value_arry.add(jsonObject_options_1);

				} else {
					if (to_check_opt == resultSet_option.getInt("group_id")) {
						JSONObject jsonObject_options_1 = new JSONObject();

						jsonObject_options.put("optgrpname", resultSet_option.getString("option_name"));
						jsonObject_options.put("optgrpid", resultSet_option.getString("group_id"));

						jsonObject_options_1.put("optvalname", resultSet_option.getString("value"));
						jsonObject_options_1.put("optvalid", resultSet_option.getString("id"));

						cat_option_value_arry.add(jsonObject_options_1);

					} else {

						jsonObject_options.put("optionval", cat_option_value_arry);
						option_array.add(jsonObject_options);
						to_check_opt = resultSet_option.getInt("group_id");

						jsonObject_options = new JSONObject();
						cat_option_value_arry = new JSONArray();

						jsonObject_options.put("optgrpname", resultSet_option.getString("option_name"));
						jsonObject_options.put("optgrpid", resultSet_option.getString("group_id"));

						JSONObject jsonObject_options_1 = new JSONObject();

						jsonObject_options_1.put("optvalname", resultSet_option.getString("value"));
						jsonObject_options_1.put("optvalid", resultSet_option.getString("id"));

						cat_option_value_arry.add(jsonObject_options_1);

					}
				}
			}

			jsonObject_options.put("optionval", cat_option_value_arry);
			option_array.add(jsonObject_options);
			jsonObject_filter.put("option", option_array);
			resultSet_option.close();
			stmt_option.close();

			String sql_category = null;
			if (flag == 0) {

				sql_category = "SELECT * FROM categories where id ='" + category_id + "'";

			} else {
				sql_category = "SELECT * FROM categories";
			}

			JSONArray category_array = new JSONArray();
			JSONObject jsonObject_category_result = new JSONObject();
			Statement stmt_category = con.createStatement();
			ResultSet resultSet_category = stmt_category.executeQuery(sql_category);

			while (resultSet_category.next()) {

				JSONObject jsonObject_category = new JSONObject();
				jsonObject_category.put("id", resultSet_category.getString(1));
				jsonObject_category.put("name", resultSet_category.getString(2));
				jsonObject_category.put("image", resultSet_category.getString(4));

				if (resultSet_category.getString(3) != null) {
					jsonObject_category.put("parent_id", resultSet_category.getString(3));
				}

				category_array.add(jsonObject_category);

			}
			if (category_array.isEmpty()) {
				jsonObject_category_result.put("message", "Category not found");
				jsonObject_category_result.put("status", "0");

			}

			jsonObject_filter.put("categories", category_array);
			resultSet_category.close();
			stmt_category.close();
			
			
			
			
		   String sql_price = null;
		   sql_price = "SELECT * FROM lookup WHERE type='Price_Filter'";

			JSONArray price_array = new JSONArray();
			
			Statement stmt_price = con.createStatement();
			ResultSet resultSet_price = stmt_price.executeQuery(sql_price);

			while (resultSet_price.next()) {
				JSONObject jsonObject_price_result = new JSONObject();
				jsonObject_price_result.put("id",resultSet_price.getInt("id"));
				String name_price=resultSet_price.getString("name");
				jsonObject_price_result.put("name",name_price );
				String price[] = null;
			
				if(name_price.contains(" to ")) {
				
			    price=name_price.split(" to ");
				jsonObject_price_result.put("minprice",price[0]);
				jsonObject_price_result.put("maxprice", price[1]);
				price_array.add(jsonObject_price_result);
				}
				else {
					int index=name_price.indexOf("+");
					//logger.log("\n index \n =" + index);
				    String min= name_price.substring(0,index);
					jsonObject_price_result.put("minprice",min);
					jsonObject_price_result.put("maxprice","");
					
					price_array.add(jsonObject_price_result);
				}
				
           
				

			}
			
			jsonObject_filter.put("price", price_array);
			resultSet_price.close();
			stmt_price.close();
			

			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.log("Caught exception: " + e);
			JSONObject jo_catch = new JSONObject();
			jo_catch.put("Exception", e.getMessage());
			return jo_catch;

		}

		finally {
			if (con != null) {
				try {
					if (!con.isClosed()) {

						con.close();
					}
				} catch (Exception e) {
					e.printStackTrace();
					logger.log("Caught exception: " + e);
					JSONObject jo_catch = new JSONObject();
					jo_catch.put("Exception", e.getMessage());
					return jo_catch;
				}
			}
		}

		return jsonObject_filter;
	}
}