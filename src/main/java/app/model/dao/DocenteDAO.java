package app.model.dao;

import app.exception.DatabaseException;
import app.model.entity.Docente;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DocenteDAO extends BaseDAO {

    private static final String SQL_SELECT_ALL = "SELECT id, nombre, cualquier_pizarra FROM docentes ORDER BY nombre";
    private static final String SQL_INSERT = "INSERT INTO docentes (nombre, cualquier_pizarra) VALUES (?, ?)";
    private static final String SQL_UPDATE = "UPDATE docentes SET nombre = ?, cualquier_pizarra = ? WHERE id = ?";
    private static final String SQL_DELETE = "DELETE FROM docentes WHERE id = ?";
    private static final String SQL_DELETE_ALL = "DELETE FROM docentes";
    private static final String SQL_RESET_SEQ = "UPDATE sqlite_sequence SET seq = 0 WHERE name = 'docentes'";

    public List<Docente> listar() {
        List<Docente> lista = new ArrayList<>();
        try (PreparedStatement stmt = getConnection().prepareStatement(SQL_SELECT_ALL);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(new Docente(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getString("cualquier_pizarra")
                ));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error al listar docentes", e);
        }
        return lista;
    }

    public void insertar(Docente docente) {
        try (PreparedStatement stmt = getConnection().prepareStatement(SQL_INSERT)) {
            stmt.setString(1, docente.getNombre());
            stmt.setString(2, docente.getCualquierPizarra());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error al insertar docente", e);
        }
    }

    public void actualizar(Docente docente) {
        try (PreparedStatement stmt = getConnection().prepareStatement(SQL_UPDATE)) {
            stmt.setString(1, docente.getNombre());
            stmt.setString(2, docente.getCualquierPizarra());
            stmt.setInt(3, docente.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error al actualizar docente", e);
        }
    }

    public void guardar(Docente docente) {
        boolean esNuevo = docente.getId() == 0;
        String sql = esNuevo ? SQL_INSERT : SQL_UPDATE;
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, docente.getNombre());
            stmt.setString(2, docente.getCualquierPizarra());
            if (!esNuevo) {
                stmt.setInt(3, docente.getId());
            }
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error al guardar docente", e);
        }
    }

    public void eliminar(int id) {
        try (PreparedStatement stmt = getConnection().prepareStatement(SQL_DELETE)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error al eliminar docente", e);
        }
    }

    public void vaciarTabla() {
        try (PreparedStatement stmt = getConnection().prepareStatement(SQL_DELETE_ALL)) {
            stmt.executeUpdate();
            getConnection().prepareStatement(SQL_RESET_SEQ).executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error al vaciar tabla docentes", e);
        }
    }

    public int insertarRetornandoId(Docente d) {
        try (PreparedStatement stmt = getConnection().prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, d.getNombre());
            stmt.setString(2, d.getCualquierPizarra() != null ? d.getCualquierPizarra() : "si");
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) { 
            throw new DatabaseException("Error al insertar docente retornando id", e);
        }
        return -1;
    }
}