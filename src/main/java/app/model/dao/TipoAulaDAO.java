package app.model.dao;

import app.exception.DatabaseException;
import app.model.entity.TipoAula;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TipoAulaDAO extends BaseDAO {

    private static final String SQL_SELECT_ALL = "SELECT id, nombre FROM tipos_aulas ORDER BY nombre";
    private static final String SQL_INSERT = "INSERT INTO tipos_aulas (nombre) VALUES (?)";
    private static final String SQL_UPDATE = "UPDATE tipos_aulas SET nombre = ? WHERE id = ?";
    private static final String SQL_DELETE = "DELETE FROM tipos_aulas WHERE id = ?";

    public List<TipoAula> listar() {
        List<TipoAula> lista = new ArrayList<>();
        try (PreparedStatement stmt = getConnection().prepareStatement(SQL_SELECT_ALL);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(new TipoAula(rs.getInt("id"), rs.getString("nombre")));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error al listar tipos de aula", e);
        }
        return lista;
    }

    public void insertar(TipoAula tipo) {
        try (PreparedStatement stmt = getConnection().prepareStatement(SQL_INSERT)) {
            stmt.setString(1, tipo.getNombre());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error al crear tipo aula", e);
        }
    }

    public void actualizar(TipoAula tipo) {
        try (PreparedStatement stmt = getConnection().prepareStatement(SQL_UPDATE)) {
            stmt.setString(1, tipo.getNombre());
            stmt.setInt(2, tipo.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error al actualizar tipo aula", e);
        }
    }

    public void guardar(TipoAula tipo) {
        boolean esNuevo = tipo.getId() == 0;
        String sql = esNuevo ? SQL_INSERT : SQL_UPDATE;
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, tipo.getNombre());
            if (!esNuevo) {
                stmt.setInt(2, tipo.getId());
            }
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error al guardar tipo de aula", e);
        }
    }

    public void eliminar(int id) {
        try (PreparedStatement stmt = getConnection().prepareStatement(SQL_DELETE)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error al eliminar tipo de aula", e);
        }
    }
}