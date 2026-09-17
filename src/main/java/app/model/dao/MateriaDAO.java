package app.model.dao;

import app.exception.DatabaseException;
import app.model.entity.Materia;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MateriaDAO extends BaseDAO {

    private static final String SQL_SELECT_ALL = "SELECT * FROM materias ORDER BY nombre";
    private static final String SQL_SELECT_WITH_TIPO = "SELECT m.*, t.nombre as tipo_req FROM materias m LEFT JOIN tipos_aulas t ON m.aula_requerida = t.id ORDER BY m.nombre";
    private static final String SQL_INSERT = "INSERT INTO materias (codigo, nombre, departamento, creditos, horas, semestre, aula_requerida) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_UPDATE = "UPDATE materias SET codigo=?, nombre=?, departamento=?, creditos=?, horas=?, semestre=?, aula_requerida=? WHERE id=?";
    private static final String SQL_DELETE = "DELETE FROM materias WHERE id = ?";
    private static final String SQL_SEARCH_ID = "SELECT id FROM materias WHERE codigo = ? OR nombre = ? LIMIT 1";
    private static final String SQL_INSERT_MIN = "INSERT INTO materias (codigo, nombre, departamento, creditos, horas, semestre, aula_requerida) VALUES (?, ?, 'S/D', 3, 6, ?, 1)";

    // UI specific queries
    private static final String SQL_INSERT_BASIC = "INSERT INTO materias (codigo, nombre, semestre, departamento, creditos, horas, aula_requerida) VALUES (?,?,?,?,?,?,?)";
    private static final String SQL_UPDATE_BASIC = "UPDATE materias SET codigo=?, nombre=?, semestre=?, departamento=?, creditos=?, horas=?, aula_requerida=? WHERE id=?";

    public List<Materia> listar() {
        List<Materia> lista = new ArrayList<>();
        try (PreparedStatement stmt = getConnection().prepareStatement(SQL_SELECT_ALL);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(new Materia(
                        rs.getInt("id"),
                        rs.getString("codigo"),
                        rs.getString("nombre"),
                        rs.getString("departamento"),
                        rs.getInt("creditos"),
                        rs.getInt("horas"),
                        rs.getInt("semestre"),
                        rs.getInt("aula_requerida")
                ));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error al listar materias: " + e.getMessage(), e);
        }
        return lista;
    }

    public void insertar(Materia materia) {
        try (PreparedStatement stmt = getConnection().prepareStatement(SQL_INSERT)) {
            stmt.setString(1, materia.getCodigo());
            stmt.setString(2, materia.getNombre());
            stmt.setString(3, materia.getDepartamento());
            stmt.setInt(4, materia.getCreditos());
            stmt.setInt(5, materia.getHoras());
            stmt.setInt(6, materia.getSemestre());
            stmt.setInt(7, materia.getAulaRequerida());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error al insertar materia: " + e.getMessage(), e);
        }
    }

    public void actualizar(Materia materia) {
        try (PreparedStatement stmt = getConnection().prepareStatement(SQL_UPDATE)) {
            stmt.setString(1, materia.getCodigo());
            stmt.setString(2, materia.getNombre());
            stmt.setString(3, materia.getDepartamento());
            stmt.setInt(4, materia.getCreditos());
            stmt.setInt(5, materia.getHoras());
            stmt.setInt(6, materia.getSemestre());
            stmt.setInt(7, materia.getAulaRequerida());
            stmt.setInt(8, materia.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error al actualizar materia: " + e.getMessage(), e);
        }
    }

    public List<Materia> listarTabla() {
        List<Materia> lista = new ArrayList<>();
        try (PreparedStatement stmt = getConnection().prepareStatement(SQL_SELECT_WITH_TIPO);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Materia m = new Materia();
                m.setId(rs.getInt("id"));
                m.setCodigo(rs.getString("codigo"));
                m.setNombre(rs.getString("nombre"));
                m.setSemestre(rs.getInt("semestre"));
                m.setDepartamento(rs.getString("departamento"));
                m.setCreditos(rs.getInt("creditos"));
                m.setHoras(rs.getInt("horas"));
                m.setIdTipoAulaReq(rs.getInt("aula_requerida"));
                m.setTipoAulaReq(rs.getString("tipo_req"));
                lista.add(m);
            }
        } catch (SQLException e) { 
            throw new DatabaseException("Error al listar tabla de materias", e);
        }
        return lista;
    }

    public void guardar(Materia m) {
        boolean nuevo = m.getId() == 0;
        String sql = nuevo ? SQL_INSERT_BASIC : SQL_UPDATE_BASIC;
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, m.getCodigo());
            stmt.setString(2, m.getNombre());
            stmt.setInt(3, m.getSemestre());
            stmt.setString(4, m.getDepartamento());
            stmt.setInt(5, m.getCreditos());
            stmt.setInt(6, m.getHoras());
            stmt.setInt(7, m.getIdTipoAulaReq());
            if (!nuevo) stmt.setInt(8, m.getId());
            stmt.executeUpdate();
        } catch (SQLException e) { 
            throw new DatabaseException("Error al guardar materia", e);
        }
    }

    public void eliminar(int id) {
        try (PreparedStatement stmt = getConnection().prepareStatement(SQL_DELETE)) { 
            stmt.setInt(1, id); 
            stmt.executeUpdate(); 
        } catch (SQLException e) { 
            throw new DatabaseException("Error al eliminar materia", e);
        }
    }

    public Integer buscarIdPorCodigoONombre(String codigo, String nombre) {
        try (PreparedStatement stmt = getConnection().prepareStatement(SQL_SEARCH_ID)) {
            stmt.setString(1, codigo); 
            stmt.setString(2, nombre);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        } catch (SQLException e) { 
            throw new DatabaseException("Error al buscar materia por codigo o nombre", e);
        }
        return null;
    }

    public int insertarMínimaRetornandoId(String codigo, String nombre, int semestre) {
        try (PreparedStatement stmt = getConnection().prepareStatement(SQL_INSERT_MIN, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, codigo); 
            stmt.setString(2, nombre); 
            stmt.setInt(3, semestre);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) { 
            throw new DatabaseException("Error al insertar materia mínima", e);
        }
        return -1;
    }
}