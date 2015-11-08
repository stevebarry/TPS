# TPS

## What it is
Simple Java Utility for querying a Teradata server and writing the text of all stored procedures to a table.

##  How it works
It queries the Teradata metadata to get a list of all stored procedures, then for each stored procedure it runs the SHOW STORED PROCEDURE command to get the full CREATE STORED PROCEDURE text, then inserts this text into a table.

## What you need to run it
You will need Java running on your machine, any version from 1.5 onwards. If you are not sure what version of Java you have, then open the command line and type 
```cmd
java -version 
```
The application uses version 15.10 Teradata Drivers, which are compatible with versions 15.10, 15.0, 14.10, 14.0 and 13.10 of the Teradata database. If you require compatibility with older versions of the Teradata database  please raise an issue.


##How to use it
Step 1. Create a table on your database with the following structure:
```SQL
CREATE SET TABLE <database>.<table_name>
     (
      procedureName VARCHAR(30),
      databaseName VARCHAR(30),
      sourceText CLOB(1048576),
      row_update_date TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6),
      status VARCHAR(10)
      )
	PRIMARY INDEX ( procedureName, databaseName); 
```
Step 2. Download the latest release of the application
    https://github.com/stevebarry/TPS/releases/download/v0.5/TPS.jar

Step 3. Browse to where you have put the jar file in step 2, then call it from the command line as follows:
```cmd
java -jar TPS.jar <target database> <target table> <db server> <username> <password>
```
* **target database** is the database where you have created the table in step 3
* **target table** is the table you have created in step 3
* **db server** is the name of the database server
* **username** is the username used to log on to this database (must have SELECT/INSERT/DELETE access to the new table, plus SELECT access to dbc.tables and SHOW PROCEDURE privileges)
* **password** password of user

example:
```cmd
java -jar TPS.jar my_db procedure_source 172.16.252.131 dbc dbc
```
## Other Considerations
The SHOW PROCEDURE command may result in an error for some procedures. If this happens the procedure is still recorded in the target table, but with a status of 'Failure', and the error message is stored in the sourceText column.


