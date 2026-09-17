package app.model.dao;

import app.exception.DatabaseException;
import app.model.entity.Aula;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AulaDAO extends BaseDAO {

    private static final String SQL_SELECT_ALL = "SELECT * FROM aulas ORDER BY edificio, numero";
    private static final String SQL_SELECT_WITH_TIPO = "SELECT a.*, t.nombre as tipo_aula FROM aulas a LEFT JOIN tipos_aulas t ON a.id_tipo_aula = t.id ORDER BY a.edificio, a.numero";
    private static final String SQL_INSERT = "INSERT INTO aulas (edificio, piso, numero, capacidad, estado, disponibilidad, id_tipo_aula) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_UPDATE = "UPDATE aulas SET edificio=?, piso=?, numero=?, capacidad=?, estado=?, disponibilidad=?, id_tipo_aula=? WHERE id=?";
    private static final String SQL_DELETE = "DELETE FROM aulas WHERE id = ?";
    private static final String SQL_SEARCH_INTELLIGENT = "SELECT id FROM aulas WHERE edificio LIKE ? AND numero LIKE ? LIMIT 1";
    
    // UI specific queries
    private static final String SQL_INSERT_BASIC = "INSERT INTO aulas (edificio, piso, numero, capacidad, estado, id_tipo_aula) VALUES (?,?,?,?,?,?)";
    private static final String SQL_UPDATE_BASIC = "UPDATE aulas SET edificio=?, piso=?, numero=?, capacidad=?, estado=?, id_tipo_aula=? WHERE id=?";

    public List<Aula> listar() {
        List<Aula> lista = new ArrayList<>();
        try (PreparedStatement stmt = getConnection().prepareStatement(SQL_SELECT_ALL);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Aula a = new Aula();
                a.setId(rs.getInt("id"));
                a.setEdificio(rs.getString("edificio"));
                a.setPiso(rs.getString("piso"));
                a.setNumero(rs.getString("numero"));
                a.setCapacidad(rs.getInt("capacidad"));
                a.setEstado(rs.getString("estado"));
                a.setDisponibilidad(rs.getString("disponibilidad"));
                a.setIdTipoAula(rs.getInt("id_tipo_aula"));
                lista.add(a);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error al listar aulas: " + e.getMessage(), e);
        }
        return lista;
    }

    public void insertar(Aula aula) {
        try (PreparedStatement stmt = getConnection().prepareStatement(SQL_INSERT)) {
            stmt.setString(1, aula.getEdificio());
            stmt.setString(2, aula.getPiso());
            stmt.setString(3, aula.getNumero());
            stmt.setInt(4, aula.getCapacidad());
            stmt.setString(5, aula.getEstado());
            stmt.setString(6, aula.getDisponibilidad());
            stmt.setInt(7, aula.getIdTipoAula());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error al insertar aula: " + e.getMessage(), e);
        }
    }

    public void actualizar(Aula aula) {
        try (PreparedStatement stmt = getConnection().prepareStatement(SQL_UPDATE)) {
            stmt.setString(1, aula.getEdificio());
            stmt.setString(2, aula.getPiso());
            stmt.setString(3, aula.getNumero());
            stmt.setInt(4, aula.getCapacidad());
            stmt.setString(5, aula.getEstado());
            stmt.setString(6, aula.getDisponibilidad());
            stmt.setInt(7, aula.getIdTipoAula());
            stmt.setInt(8, aula.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error al actualizar aula: " + e.getMessage(), e);
        }
    }

    public List<Aula> listarTabla() {
        List<Aula> lista = new ArrayList<>();
        try (PreparedStatement stmt = getConnection().prepareStatement(SQL_SELECT_WITH_TIPO); 
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Aula a = new Aula();
                a.setId(rs.getInt("id"));
                a.setEdificio(rs.getString("edificio"));
                a.setPiso(rs.getString("piso"));
                a.setNumero(rs.getString("numero"));
                a.setCapacidad(rs.getInt("capacidad"));
                a.setEstado(rs.getString("estado"));
                a.setIdTipoAula(rs.getInt("id_tipo_aula"));
                a.setTipoAula(rs.getString("tipo_aula"));
                lista.add(a);
            }
        } catch (SQLException e) { 
            throw new DatabaseException("Error al listar tabla de aulas", e);
        }
        return lista;
    }

    public void guardar(Aula a) {
        boolean nuevo = a.getId() == 0;
        String sql = nuevo ? SQL_INSERT_BASIC : SQL_UPDATE_BASIC;
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, a.getEdificio());
            stmt.setString(2, a.getPiso());
            stmt.setString(3, a.getNumero());
            stmt.setInt(4, a.getCapacidad());
            stmt.setString(5, a.getEstado());
            stmt.setInt(6, a.getIdTipoAula());
            if (!nuevo) stmt.setInt(7, a.getId());
            stmt.executeUpdate();
        } catch (SQLException e) { 
            throw new DatabaseException("Error al guardar aula", e);
        }
    }

    public void eliminar(int id) {
        try (PreparedStatement stmt = getConnection().prepareStatement(SQL_DELETE)) { 
            stmt.setInt(1, id); 
            stmt.executeUpdate(); 
        } catch (SQLException e) { 
            throw new DatabaseException("Error al eliminar aula", e);
        }
    }

    public Integer buscarAulaInteligente(String textoExcel) {
        if (textoExcel == null || textoExcel.trim().isEmpty() || textoExcel.contains("SIN EDIFICIO") || textoExcel.contains("SE/")) return null;

        String[] partes = textoExcel.split("/");
        if (partes.length < 3) return null;

        String edificio = partes[0].trim();
        String numero = partes[2].trim();

        try (PreparedStatement stmt = getConnection().prepareStatement(SQL_SEARCH_INTELLIGENT)) {
            stmt.setString(1, "%" + edificio + "%");
            stmt.setString(2, "%" + numero + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        } catch (SQLException e) { 
            throw new DatabaseException("Error al buscar aula inteligente", e);
        }
        return null;
    }
}