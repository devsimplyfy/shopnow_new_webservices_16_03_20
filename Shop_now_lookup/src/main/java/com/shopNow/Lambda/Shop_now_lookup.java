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
import java.util.Properties;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Shop_now_lookup implements RequestHandler<JSONObject, JSONObject> {

	private String USERNAME;
	private String PASSWORD;
	private String DB_URL;

	@SuppressWarnings("unchecked")
	public JSONObject handleRequest(JSONObject input, Context context) {
		LambdaLogger logger = context.getLogger();

		JSONObject errorPayload = new JSONObject();
		Connection conn=null;
		/*
		 * if(!input.containsKey("type")){ errorPayload.put("errorType", "BadRequest");
		 * errorPayload.put("httpStatus", 400); errorPayload.put("requestId",
		 * context.getAwsRequestId()); errorPayload.put("message",
		 * "JSON Input Object request key named 'type' is missing"); throw new
		 * RuntimeException(errorPayload.toJSONString()); }
		 * 
		 */

		// SELECT table1.*,table2.name AS parent FROM (SELECT * FROM lookup WHERE TYPE
		// ='City' AND STATUS=1 ) AS table1 INNER JOIN (SELECT * FROM lookup)AS table2
		// ON table1.parent=table2.id;

		JSONObject jsonObject_lookup_result = new JSONObject();
		JSONArray lookup_array = new JSONArray();
		String Str_msg = null;
		String type = input.get("type").toString();
		String parent_id = input.get("parent_id").toString();

		String select_sql = null;
		int flag = 0;
		if ((type == null || type == "") && (parent_id == null || parent_id == "")) {
			flag = 1;
			select_sql = "SELECT * FROM lookup where status=1";

		} else if ((type != null || type != "") && (parent_id == null || parent_id == "")) {

			flag = 2;

			select_sql = "SELECT * FROM lookup WHERE type ='" + type + "' and status=1";

		} else if ((type == null || type == "") && (parent_id != null || parent_id != "")) {
			flag = 3;
			select_sql = "SELECT * FROM lookup WHERE  parent=(select id from lookup where name='" + parent_id
					+ "') and status=1";

		} else {
			flag = 4;
			select_sql = "SELECT * FROM lookup WHERE type ='" + type
					+ "' and parent=(select id from lookup where name='" + parent_id + "') and status=1";

		}
		// logger.log(select_sql);
		// Get time from DB server
		Properties prop = new Properties();

		try {
			prop.load(getClass().getResourceAsStream("/application.properties"));
			DB_URL = prop.getProperty("url");
			USERNAME = prop.getProperty("username");
			PASSWORD = prop.getProperty("password");
		   conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);

			Statement stmt_look_type = conn.createStatement();
			ResultSet rs_lookup_type = null;
			if (flag == 2) {

				logger.log("\n flag=2  \n");

				rs_lookup_type = stmt_look_type.executeQuery(select_sql);
				if (rs_lookup_type.next() == false) {
					jsonObject_lookup_result.put("message", "type not found ");
					jsonObject_lookup_result.put("status", "0");
					return jsonObject_lookup_result;

				}

			} else if (flag == 3) {
				logger.log("\n flag=3  \n ");

				rs_lookup_type = stmt_look_type.executeQuery(select_sql);
				if (rs_lookup_type.next() == false) {
					jsonObject_lookup_result.put("message", "parent not found ");
					jsonObject_lookup_result.put("status", "0");
					return jsonObject_lookup_result;

				}

			}

			else if (flag == 4) {
				logger.log("\n  flag=4 \n ");

				rs_lookup_type = stmt_look_type.executeQuery(select_sql);
				if (rs_lookup_type.next() == false) {
					jsonObject_lookup_result.put("message", "type and parent not match ");
					jsonObject_lookup_result.put("status", "0");
					return jsonObject_lookup_result;

				}

			}
			// rs_lookup_type.close();
			// stmt_look_type.close();

			Statement select_stmt = conn.createStatement();
			// logger.log("\n " + select_sql + "\n ");
			ResultSet rs_lookup = select_stmt.executeQuery(select_sql);

			while (rs_lookup.next()) {

				JSONObject jsonObject_lookup = new JSONObject();
				jsonObject_lookup.put("id", rs_lookup.getInt("id"));
				jsonObject_lookup.put("name", rs_lookup.getString("name"));
				jsonObject_lookup.put("type", rs_lookup.getString("type"));
				jsonObject_lookup.put("parent", rs_lookup.getInt("parent"));

				lookup_array.add(jsonObject_lookup);

			}

		} catch (Exception e) {
			e.printStackTrace();
			logger.log("Caught exception: " + e);
			JSONObject jo_catch = new JSONObject();
			jo_catch.put("Exception", e.getMessage());
			return jo_catch;
		}

		finally {
			if (conn != null) {
				try {
					if (!conn.isClosed()) {
						conn.close();
					}
				} catch (SQLException e) {
					e.printStackTrace();
					logger.log("Caught exception: " + e);
				}
			}
		}
		jsonObject_lookup_result.put("Type", lookup_array);

		return jsonObject_lookup_result;
	}
}
