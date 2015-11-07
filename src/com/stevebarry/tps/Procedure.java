package com.stevebarry.tps;

public class Procedure {

	private String DatabaseName;
	private String ProcedureName;

	public String getDatabaseName() {
		return DatabaseName;
	}

	public void setDatabaseName(String databaseName) {
		DatabaseName = databaseName;
	}

	public String getProcedureName() {
		return ProcedureName;
	}

	public void setProcedureName(String procedureName) {
		ProcedureName = procedureName;
	}

}