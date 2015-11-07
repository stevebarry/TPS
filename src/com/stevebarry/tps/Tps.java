package com.stevebarry.tps;

/**
 * 
CREATE SET TABLE us5008.procedure_source
     (
      procedureName VARCHAR(30),
      databaseName VARCHAR(30),
      sourceText CLOB(1048576),
      row_update_date TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6),
      status VARCHAR(10)
      )
	PRIMARY INDEX ( procedureName, databaseName);
 * 
 */

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Tps {

	static Connection conn;
	static List<Procedure> procedures;

	static String targetDB;
	static String targetTable;
	static String server;
	static String username;
	static String password;

	public static void main(String[] args) {

		// parameters passed in at runtime
		targetDB = args[0];
		targetTable = args[1];
		server = args[2];
		username = args[3];
		password = args[4];

		// Loading the Teradata JDBC driver
		try {
			Class.forName("com.teradata.jdbc.TeraDriver");
			System.out.println("JDBC driver loaded.");

			// connect to Database
			try {
				conn = DriverManager.getConnection("jdbc:teradata://" + server, username, password);
				System.out.println("JDBC Connection established.");

				// delete existing records from table
				delete();
				// retrieve a list of procedures from dbc.tables
				selectProcedures();
				// execute SHOW PROCEDURE for each procedure, inserting the
				// results to target table
				insertProcedureSource();

				System.out.println("Application Complete.");

			} catch (SQLException e) {
				System.out.println("Error encountered while connecting to database");
				e.printStackTrace();
			}

		} catch (ClassNotFoundException e) {
			System.out.println("Error loading JDBC driver");
			e.printStackTrace();
		}

	} // end main

	static void delete() {

		// Delete existing records from target table
		String sqlDelete = "DELETE FROM " + targetDB + "." + targetTable + ";";

		try {
			int activityCount;

			PreparedStatement psDelete = conn.prepareStatement(sqlDelete);
			activityCount = psDelete.executeUpdate();
			psDelete.close();
			System.out.println("Delete completed successfully: " + activityCount + " row(s) deleted.");

		} catch (SQLException e) {
			System.out.println("Error encountered while deleting: " + e.getMessage());
		}

	}

	static void selectProcedures() {

		procedures = new ArrayList<Procedure>();

		// SQL to get list of procedures from dbc.tables
		String sqlProcedureList = "SELECT TRIM(T.DatabaseName),TRIM(T.TableName)\r\n" + "  FROM DBC.Tables T\r\n"
				+ "WHERE T.TableKind = 'P'";

		try {
			PreparedStatement ps = conn.prepareStatement(sqlProcedureList);
			ResultSet rs = ps.executeQuery();

			int procedureCounter = 0;

			while (rs.next()) {
				// for each stored procedure, create new StoredProcedure object
				// and add this to the list
				Procedure p = new Procedure();
				p.setDatabaseName(rs.getString(1));
				p.setProcedureName(rs.getString(2));
				procedures.add(p);
				procedureCounter++;
			}
			rs.close();
			ps.close();
			System.out.println("Procedure List built with " + procedureCounter + " procedure(s).");
		} catch (SQLException e) {
			System.out.println("Could not run SQL '" + sqlProcedureList + "': " + e.getMessage());
		}

	}

	static void insertProcedureSource() {

		// loop through each procedure, executing SHOW PROCEDURE then INSERT for each 
		int insertCounter = 0;

		for (Procedure p : procedures) {

			String createStatement = "";
			String status = "Success";

			// execute SHOW PROCEDURE
			String sqlShow = "SHOW PROCEDURE " + p.getDatabaseName() + "." + p.getProcedureName() + ";";
			try {
				PreparedStatement ps = conn.prepareStatement(sqlShow);
				ResultSet rs = ps.executeQuery();
				while (rs.next()) {
					createStatement += rs.getString(1);
				}
				rs.close();
				ps.close();
			} catch (SQLException e) {
				// if the Show Procedure statement fails, store the error
				// message in the DB with a status of Failure
				createStatement = e.getMessage();
				status = "Failure";
			}

			try {

				String sqlInsert = "INSERT INTO " + targetDB + "." + targetTable
						+ " (databaseName, procedureName, sourceText, status) VALUES (?,?,?,?);";

				PreparedStatement psInsert = conn.prepareStatement(sqlInsert);
				psInsert.setString(1, p.getDatabaseName());
				psInsert.setString(2, p.getProcedureName());

				// extra byte stream logic for sourceText field to cater for
				// CLOB
				byte[] bytes = createStatement.getBytes(StandardCharsets.UTF_8);
				InputStream stream = new ByteArrayInputStream(bytes);
				psInsert.setAsciiStream(3, stream, bytes.length);

				psInsert.setString(4, status);
				psInsert.execute();
				psInsert.close();
				insertCounter++;

			} catch (SQLException e) {
				System.out.println("Could not run SQL: " + e.getMessage());
			}

		}
		System.out.println("Insert complete: " + insertCounter + " record(s) inserted.");

	}

}