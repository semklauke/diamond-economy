package com.gmail.sneakdevs.diamondeconomy.sql;

import com.gmail.sneakdevs.diamondeconomy.DiamondEconomy;
import com.gmail.sneakdevs.diamondeconomy.config.DiamondEconomyConfig;

import java.io.File;
import java.sql.*;

public class SQLiteDatabaseManager implements DatabaseManager {
    public static String url;

    public static void createNewDatabase(File file) {
        url = "jdbc:sqlite:" + file.getPath().replace('\\', '/');

        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        createNewTable();
    }

    public Connection connect() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return conn;
    }

    private static void createNewTable() {
        try (Connection conn = DriverManager.getConnection(url); Statement stmt = conn.createStatement()) {
            for (String query : DiamondEconomy.tableRegistry) {
                stmt.execute(query);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addPlayer(String uuid, String name) {
        if (invalidUUID(uuid)) return;
        String sql = "INSERT INTO diamonds(uuid,name,money,taxlevel) VALUES(?,?,?,1)";

        try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1, uuid);
            pstmt.setString(2, name);
            pstmt.setInt(3, DiamondEconomyConfig.getInstance().startingMoney);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            updateName(uuid, name);
        }
    }

    public void updateName(String uuid, String name) {
        if (invalidUUID(uuid)) return;
        String sql = "UPDATE diamonds SET name = ? WHERE uuid = ?";

        try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, uuid);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setName(String uuid, String name) {
        if (invalidUUID(uuid)) return;
        String sql = "UPDATE diamonds SET name = ? WHERE uuid != ? AND name = ?";

        try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "a");
            pstmt.setString(2, uuid);
            pstmt.setString(3, name);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getBalanceFromUUID(String uuid){
        if (invalidUUID(uuid)) return -1;
        String sql = "SELECT uuid, money FROM diamonds WHERE uuid = '" + uuid + "'";

        try (Connection conn = this.connect(); Statement stmt  = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)){
            if (rs.next())
                return rs.getInt("money");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public String getNameFromUUID(String uuid){
        if (invalidUUID(uuid)) return null;
        String sql = "SELECT uuid, name FROM diamonds WHERE uuid = '" + uuid + "'";

        try (Connection conn = this.connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)){
            if(rs.next())
                return rs.getString("name");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int getBalanceFromName(String name){
        String sql = "SELECT name, money FROM diamonds WHERE name = '" + name + "'";

        try (Connection conn = this.connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)){
            if (rs.next())
                return rs.getInt("money");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public boolean setBalance(String uuid, int money) {
        if (invalidUUID(uuid)) return false;
        String sql = "UPDATE diamonds SET money = ? WHERE uuid = ?";

        try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (money >= 0 && money < Integer.MAX_VALUE) {
                pstmt.setInt(1, money);
                pstmt.setString(2, uuid);
                pstmt.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void setAllBalance(int money) {
        String sql = "UPDATE diamonds SET money = ?";

        try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (money >= 0 && money < Integer.MAX_VALUE) {
                pstmt.setInt(1, money);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean changeBalance(String uuid, int money) {
        if (invalidUUID(uuid)) return false;
        String sql = "UPDATE diamonds SET money = ? WHERE uuid = ?";

        try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int bal = getBalanceFromUUID(uuid);
            if (bal >= -1 && bal + money >= 0 && bal + money < Integer.MAX_VALUE) {
                pstmt.setInt(1, bal + money);
                pstmt.setString(2, uuid);
                pstmt.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void changeAllBalance(int money) {
        String sql = "UPDATE diamonds SET money = money + " + money + " WHERE " + Integer.MAX_VALUE + " > money + " + money + " AND 0 <= money + " + money;

        try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getUUIDFromName(String name) {
        String sql = "SELECT uuid FROM diamonds WHERE name = '" + name + "' COLLATE NOCASE";

        try (Connection conn = this.connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)){
            if (rs.next())
                return rs.getString("uuid");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    public String top(String uuid, int page){
        if (invalidUUID(uuid)) return null;
        String sql = "SELECT uuid, name, money FROM diamonds ORDER BY money DESC";

        String rankings = "";
        int i = 0;
        int playerRank = 0;
        int repeats = 0;

        try (Connection conn = this.connect(); Statement stmt  = conn.createStatement(); ResultSet rs    = stmt.executeQuery(sql)){
            while (rs.next() && (repeats < 10 || playerRank == 0)) {
                if (repeats / 10 + 1 == page) {
                    rankings = rankings.concat(rs.getRow() + ") " + rs.getString("name") + ": $" + rs.getInt("money") + "\n");
                    i++;
                }
                repeats++;
                if (uuid.equals(rs.getString("uuid"))) {
                    playerRank = repeats;
                }
            }
            if (i < 10) {
                rankings = rankings.concat("---End--- \n");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rankings.concat("Your rank is: " + playerRank);
    }

    public String rank(int rank){
        int repeats = 0;
        String sql = "SELECT name FROM diamonds ORDER BY money DESC";
        try (Connection conn = this.connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)){
            while (rs.next() ) {
                repeats++;
                if (repeats == rank) {
                    return rs.getString("name");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "No Player";
    }

    public int playerRank(String uuid){
        if (invalidUUID(uuid)) return -1;
        String sql = "SELECT uuid FROM diamonds ORDER BY money DESC";
        int repeats = 1;

        try (Connection conn = this.connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)){
            rs.next();
            while (!rs.getString("uuid").equals(uuid)) {
                rs.next();
                repeats++;
            }
            return repeats;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public boolean setTaxlevel(String uuid, int taxlevel) {
        if (invalidUUID(uuid)) return false;
        String sql = "UPDATE diamonds SET taxlevel = ? WHERE uuid = ?";

        try (Connection conn = this.connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, taxlevel);
            stmt.setString(2, uuid);
            return (stmt.executeUpdate() == 1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public int getTaxlevel(String uuid) {
        if (invalidUUID(uuid)) return -1;
        String sql = "SELECT taxlevel FROM diamonds WHERE uuid = ? LIMIT 1";

        try (Connection conn = this.connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid);
            ResultSet rs = stmt.executeQuery();
            if (rs.next())
                return rs.getInt("taxlevel");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public int taxAll(int percentage, int minTaxlevel) {
        if (percentage < 1 || percentage > 100 || minTaxlevel < 0)
            return -1;
        String sql = "UPDATE diamonds SET money = money - round(money * ?,0) WHERE taxlevel >= ?";

        try (Connection conn = this.connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, percentage * 0.01);
            stmt.setInt(2, minTaxlevel);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public boolean taxPlayerUUID(String uuid, int percentage) {
          if (percentage < 1 || percentage > 100 || invalidUUID(uuid))
              return false;
          String sql = "UPDATE diamonds SET money = money - round(money * ?,0) WHERE uuid = ?";

          try (Connection conn = this.connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
              stmt.setDouble(1, percentage * 0.01);
              stmt.setString(2, uuid);
              return (stmt.executeUpdate() == 1);
          } catch (SQLException e) {
              e.printStackTrace();
          }
          return false;
    }
    private boolean invalidUUID(String uuid) {
        return !uuid.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    }
}
