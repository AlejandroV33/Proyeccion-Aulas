package app.model.dao;

import app.util.DatabaseConnection;
import app.exception.DatabaseException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class BaseDAO {

    protected Connection getConnection() {
        return DatabaseConnection.getConnection();
    }

    protected void close(PreparedStatement stmt) {
        if (stmt != null) {
            try { stmt.close(); } catch (SQLException e) { /* ignorar */ }
        }
    }

    protected void close(ResultSet rs) {
        if (rs != null) {
            try { rs.close(); } catch (SQLException e) { /* ignorar */ }
        }
    }

    // --- MANEJO DE TRANSACCIONES ---
    public static void startTransaction() {
        try {
            Connection conn = DatabaseConnection.getConnection();
            if (conn != null) {
                conn.setAutoCommit(false);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error al iniciar transaccion", e);
        }
    }

    public static void commitTransaction() {
        try {
            Connection conn = DatabaseConnection.getConnection();
            if (conn != null) {
                conn.commit();
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error al hacer commit de transaccion", e);
        }
    }

    public static void rollbackTransaction() {
        try {
            Connection conn = DatabaseConnection.getConnection();
            if (conn != null && !conn.getAutoCommit()) {
                conn.rollback();
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error al hacer rollback de transaccion", e);
        }
    }
}