package com.trxs.pulse;

import java.sql.*;

/**
 * Created by Fant.J.
 * 2018/3/5 21:38
 */
public class DatabaseMetaDataTest
{
    public static void main(String[] args)
    {
        String url = "jdbc:mysql://localhost:3306/user";
        String user = "root";
        String password = "root";

        Connection connection =null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection(url,user,password);

            DatabaseMetaData databaseMetaData = connection.getMetaData();

            int    majorVersion   = databaseMetaData.getDatabaseMajorVersion();
            int    minorVersion   = databaseMetaData.getDatabaseMinorVersion();

            String productName    = databaseMetaData.getDatabaseProductName();
            String productVersion = databaseMetaData.getDatabaseProductVersion();

            System.out.println("数据库属性信息："+majorVersion+" "+minorVersion+" "+productName+" "+productVersion);

            int driverMajorVersion = databaseMetaData.getDriverMajorVersion();
            int driverMinorVersion = databaseMetaData.getDriverMinorVersion();

            System.out.println("驱动信息："+driverMajorVersion+" "+driverMinorVersion);

/*            String   catalog          = null;
            String   schemaPattern    = null;
            String   tableNamePattern = null;
            String[] types            = null;

            ResultSet result = databaseMetaData.getTables(
                    catalog, schemaPattern, tableNamePattern, types );

            while(result.next()) {
                String tableName = result.getString(3);
                System.out.println(tableName);
            }*/


 /*           String   catalog           = null;
            String   schemaPattern     = null;
            String   tableNamePattern  = "user";
            String   columnNamePattern = null;


            ResultSet result = databaseMetaData.getColumns(
                    catalog, schemaPattern,  tableNamePattern, columnNamePattern);

            while(result.next()){
                String columnName = result.getString(4);
                int    columnType = result.getInt(5);
                System.out.println(columnName+" "+columnType+" ");
            }*/

            String   catalog   = null;
            String   schema    = null;
            String   tableName = "user";

            ResultSet result = databaseMetaData.getPrimaryKeys(
                    catalog, schema, tableName);

            while(result.next()){
                String columnName = result.getString(4);
                System.out.println(columnName);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
