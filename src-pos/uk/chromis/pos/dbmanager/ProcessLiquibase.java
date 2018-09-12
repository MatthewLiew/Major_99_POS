/*
**    Chromis POS  - The New Face of Open Source POS
**    Copyright (c)2015-2016
**    http://www.chromis.co.uk
**
**    This file is part of Chromis POS Version V0.60.2 beta
**
**    Chromis POS is free software: you can redistribute it and/or modify
**    it under the terms of the GNU General Public License as published by
**    the Free Software Foundation, either version 3 of the License, or
**    (at your option) any later version.
**
**    Chromis POS is distributed in the hope that it will be useful,
**    but WITHOUT ANY WARRANTY; without even the implied warranty of
**    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
**    GNU General Public License for more details.
**
**    You should have received a copy of the GNU General Public License
**    along with Chromis POS.  If not, see <http://www.gnu.org/licenses/>
**
**
 */
package uk.chromis.pos.dbmanager;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import uk.chromis.pos.forms.AppConfig;
import uk.chromis.pos.forms.DriverWrapper;
import uk.chromis.pos.util.AltEncrypter;

/**
 *
 * @author John
 */
public class ProcessLiquibase {

    static final Dimension SCREEN_DIMENSION = Toolkit.getDefaultToolkit().getScreenSize();

    public static Boolean DBFAILED = true;
    private static Connection con;
    private static PreparedStatement stmt2;

    public ProcessLiquibase() {

    }

    public Boolean performAction(String changelog) {
        String db_user = (AppConfig.getInstance().getProperty("db.user"));
        String db_url = (AppConfig.getInstance().getProperty("db.URL"));
        String db_password = (AppConfig.getInstance().getProperty("db.password"));

        if (db_user != null && db_password != null && db_password.startsWith("crypt:")) {
            AltEncrypter cypher = new AltEncrypter("cypherkey" + db_user);
            db_password = cypher.decrypt(db_password.substring(6));
        }

        Liquibase liquibase = null;
        try {
            Connection con = DriverManager.getConnection(db_url, db_user, db_password);
            ClassLoader cloader = new URLClassLoader(new URL[]{new File(AppConfig.getInstance().getProperty("db.driverlib")).toURI().toURL()});
            DriverManager.registerDriver(new DriverWrapper((Driver) Class.forName(AppConfig.getInstance().getProperty("db.driver"), true, cloader).newInstance()));
// lets check if the database has passed new database test
            try {

                PreparedStatement stmt2 = con.prepareStatement("SELECT COUNT(*) FROM DATABASECHANGELOG WHERE ID='V0.70 indicator'");
                ResultSet rs = stmt2.executeQuery();
                if (rs.next()) {
                    if (rs.getInt(1) != 0) {
                        changelog = "uk/chromis/pos/liquibase/scripts/update/dbupdate.xml";
                    }
                }

                stmt2 = con.prepareStatement("SELECT COUNT(*) FROM DATABASECHANGELOG WHERE ID='New Database'");
                rs = stmt2.executeQuery();
                if (rs.next()) {
                    if (rs.getInt(1) != 0) {
                        changelog = "uk/chromis/pos/liquibase/upgrade/systemupdate.xml";
                    }
                }
            } catch (SQLException ex) {
            }
// Ensure there are no liquibase locks
            try {
                stmt2 = con.prepareStatement("DROP TABLE DATABASECHANGELOGLOCK");
                stmt2.executeUpdate();
            } catch (SQLException ex) {
            }

            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(DriverManager.getConnection(db_url, db_user, db_password)));
            liquibase = new Liquibase(changelog, new ClassLoaderResourceAccessor(), database);
            liquibase.setRedirect(true);
            liquibase.setErrorFile(System.getProperty("user.dir") + "/liquibase.log");
            liquibase.update("implement");
            DBFAILED = false;

        } catch (DatabaseException | MalformedURLException | SQLException | ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            Logger.getLogger(ProcessLiquibase.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (LiquibaseException ex) {
            return false;
        } finally {
            if (con != null) {
                try {
                    con.rollback();
                    con.close();
                } catch (SQLException e) {
                    //nothing to do
                }
            }
        }
        insertMenuEntry(db_user, db_url, db_password);  //insert in to menu.root
        insertNewButtons(db_user, db_url, db_password); //insert in to ticket.buttons
        return true;
    }

    private static void insertMenuEntry(String db_user, String db_url, String db_password) {
        try {
            Connection con = DriverManager.getConnection(db_url, db_user, db_password);
            String decodedData = "";
            Statement stmt = (Statement) con.createStatement();
            // get the menu from the resources table
            String SQL = "SELECT * FROM RESOURCES WHERE NAME='Menu.Root'";
            ResultSet rs = stmt.executeQuery(SQL);
            while (rs.next()) {
                byte[] bytesData = rs.getBytes("CONTENT");
                decodedData = new String(bytesData);
            }
            // get the date from the menu entries table
            SQL = "SELECT * FROM MENUENTRIES ";
            rs = stmt.executeQuery(SQL);
            // while we have some entries lets process them
            while (rs.next()) {
                // lets check if the entry is in the menu
                int index1 = decodedData.indexOf(rs.getString("ENTRY"));
                if (index1 == -1) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(rs.getString("FOLLOWS"));
                    sb.append("\");\n        submenu.addPanel(\"");
                    sb.append(rs.getString("GRAPHIC"));
                    sb.append("\", \"");
                    sb.append(rs.getString("TITLE"));
                    sb.append("\", \"");
                    sb.append(rs.getString("ENTRY"));
                    decodedData = decodedData.replaceAll(rs.getString("FOLLOWS"), sb.toString());
                    byte[] bytesData = decodedData.getBytes();
                    String SQL2 = "UPDATE RESOURCES SET CONTENT = ? WHERE NAME = 'Menu.Root' ";
                    PreparedStatement stmt2 = con.prepareStatement(SQL2);
                    stmt2.setBytes(1, bytesData);
                    stmt2.executeUpdate();
                }
            }
            SQL = "DELETE FROM MENUENTRIES ";
            PreparedStatement stmt2 = con.prepareStatement(SQL);
            stmt2.executeUpdate();
        } catch (SQLException ex) {
            Logger.getLogger(ProcessLiquibase.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    private static void insertNewButtons(String db_user, String db_url, String db_password) {
        try {
            Connection con = DriverManager.getConnection(db_url, db_user, db_password);
            String decodedData = "";
            Statement stmt = (Statement) con.createStatement();
            // get the buttons from the resources table
            String SQL = "SELECT * FROM RESOURCES WHERE NAME='Ticket.Buttons'";
            ResultSet rs = stmt.executeQuery(SQL);
            while (rs.next()) {
                byte[] bytesData = rs.getBytes("CONTENT");
                decodedData = new String(bytesData);
            }
            // get the date from the menu entries table
            SQL = "SELECT * FROM NEWBUTTON ";
            rs = stmt.executeQuery(SQL);
            // while we have some entries lets process them
            while (rs.next()) {
                // lets check if the entry is in the menu
                int index1 = decodedData.indexOf(rs.getString("ENTRY"));
                if (index1 == -1) {
                    StringBuilder sb = new StringBuilder();
                    if (rs.getString("ENTRY").substring(0, 2).equals("!!")) {
                        sb.append("    <!-- ");
                        sb.append(rs.getString("ENTRY").substring(2, rs.getString("ENTRY").length()));
                        sb.append(" -->\n");

                    } else {
                        sb.append("    <!-- <");
                        sb.append(rs.getString("ENTRY"));
                        sb.append("/> -->\n");
                    }
                    sb.append("</configuration>");
                    decodedData = decodedData.replaceAll("</configuration>", sb.toString());
                    byte[] bytesData = decodedData.getBytes();
                    String SQL2 = "UPDATE RESOURCES SET CONTENT = ? WHERE NAME = 'Ticket.Buttons' ";
                    PreparedStatement stmt2 = con.prepareStatement(SQL2);
                    stmt2.setBytes(1, bytesData);
                    stmt2.executeUpdate();
                }
            }
            SQL = "DELETE FROM NEWBUTTON ";
            PreparedStatement stmt2 = con.prepareStatement(SQL);
            stmt2.executeUpdate();
        } catch (SQLException ex) {
            Logger.getLogger(ProcessLiquibase.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                }
            }
        }
    }
}
