package app.model.dao;

import app.exception.DatabaseException;
import app.model.entity.Paralelo;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ParaleloDAO extends BaseDAO {

    private static final String SQL_SELECT_ALL = "SELECT * FROM paralelos";
    private static final String SQL_INSERT = "INSERT INTO paralelos (nombre, num_estudiantes_matriculados, id_materia, id_docente, espejo) VALUES (?, ?, ?, ?, ?)";
    private static final String SQL_UPDATE = "UPDATE paralelos SET nombre=?, num_estudiantes_matriculados=?, id_materia=?, id_docente=?, espejo=? WHERE id=?";
    private static final String SQL_SELECT_DETALLES = "SELECT p.id, m.nombre as materia, p.nombre as paralelo, d.nombre as docente FROM paralelos p JOIN materias m ON p.id_materia = m.id LEFT JOIN docentes d ON p.id_docente = d.id ORDER BY m.nombre, p.nombre";
    private static final String SQL_SELECT_TABLA = "SELECT p.id, p.nombre, p.num_estudiantes_matriculados, p.espejo, m.id as id_materia, m.nombre as materia, d.id as id_docente, d.nombre as docente FROM paralelos p JOIN materias m ON p.id_materia = m.id LEFT JOIN docentes d ON p.id_docente = d.id ORDER BY m.nombre, p.nombre";
    private static final String SQL_INSERT_BASIC = "INSERT INTO paralelos (nombre, num_estudiantes_matriculados, id_materia, id_docente) VALUES (?, ?, ?, ?)";
    private static final String SQL_UPDATE_BASIC = "UPDATE paralelos SET nombre=?, num_estudiantes_matriculados=?, id_materia=?, id_docente=? WHERE id=?";
    private static final String SQL_DELETE = "DELETE FROM paralelos WHERE id = ?";
    private static final String SQL_DELETE_ALL = "DELETE FROM paralelos";
    private static final String SQL_RESET_SEQ = "UPDATE sqlite_sequence SET seq = 0 WHERE name = 'paralelos'";

    public List<Paralelo> listar() {
        List<Paralelo> lista = new ArrayList<>();
        try (PreparedStatement stmt = getConnection().prepareStatement(SQL_SELECT_ALL);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(new Paralelo(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getInt("num_estudiantes_matriculados"),
                        rs.getInt("id_materia"),
                        rs.getObject("id_docente") != null ? rs.getInt("id_docente") : null,
                        rs.getString("espejo")
                ));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error al listar paralelos", e);
        }
        return lista;
    }

    public void insertar(Paralelo p) {
        try (PreparedStatement stmt = getConnection().prepareStatement(SQL_INSERT)) {
            stmt.setString(1, p.getNombre());
            stmt.setInt(2, p.getNumEstudiantesMatriculados());
            stmt.setInt(3, p.getIdMateria());
            if (p.getIdDocente() != null && p.getIdDocente() > 0) stmt.setInt(4, p.getIdDocente());
            else stmt.setNull(4, java.sql.Types.INTEGER);
            stmt.setString(5, p.getEspejo());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error al insertar paralelo", e);
        }
    }

    public void actualizar(Paralelo p) {
        try (PreparedStatement stmt = getConnection().prepareStatement(SQL_UPDATE)) {
            stmt.setString(1, p.getNombre());
            stmt.setInt(2, p.getNumEstudiantesMatriculados());
            stmt.setInt(3, p.getIdMateria());
            if (p.getIdDocente() != null && p.getIdDocente() > 0) stmt.setInt(4, p.getIdDocente());
            else stmt.setNull(4, java.sql.Types.INTEGER);
            stmt.setString(5, p.getEspejo());
            stmt.setInt(6, p.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error al actualizar paralelo", e);
        }
    }

    public List<Paralelo> listarDetalles() {
        List<Paralelo> lista = new ArrayList<>();
        try (PreparedStatement stmt = getConnection().prepareStatement(SQL_SELECT_DETALLES);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Paralelo p = new Paralelo();
                p.setId(rs.getInt("id"));
                p.setMateria(rs.getString("materia"));
                p.setNombre(rs.getString("paralelo"));
                p.setDocente(rs.getString("docente"));
                lista.add(p);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error al listar detalles de paralelos", e);
        }
        return lista;
    }

    public List<Paralelo> listarTabla() {
        List<Paralelo> lista = new ArrayList<>();
        try (PreparedStatement stmt = getConnection().prepareStatement(SQL_SELECT_TABLA);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Paralelo f = new Paralelo();
                f.setId(rs.getInt("id"));
                f.setNombre(rs.getString("nombre"));
                f.setNumEstudiantesMatriculados(rs.getInt("num_estudiantes_matriculados"));
                f.setEspejo(rs.getString("espejo"));
                f.setIdMateria(rs.getInt("id_materia"));
                f.setMateria(rs.getString("materia"));
                int idDoc = rs.getInt("id_docente");
                if (!rs.wasNull()) {
                    f.setIdDocente(idDoc);
                    f.setDocente(rs.getString("docente"));
                } else {
                    f.setDocente("SIN DOCENTE");
                }
                lista.add(f);
            }
        } catch (SQLException e) { 
            throw new DatabaseException("Error al listar tabla de paralelos", e);
        }
        return lista;
    }

    public void guardar(Paralelo p) {
        boolean esNuevo = p.getId() == 0;
        String sql = esNuevo ? SQL_INSERT_BASIC : SQL_UPDATE_BASIC;

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, p.getNombre());
            stmt.setInt(2, p.getNumEstudiantesMatriculados());
            stmt.setInt(3, p.getIdMateria());
            if (p.getIdDocente() == null || p.getIdDocente() <= 0) stmt.setNull(4, java.sql.Types.INTEGER);
            else stmt.setInt(4, p.getIdDocente());

            if (!esNuevo) stmt.setInt(5, p.getId());

            stmt.executeUpdate();
        } catch (SQLException e) { 
            throw new DatabaseException("Error al guardar paralelo", e);
        }
    }

    public void eliminar(int id) {
        try (PreparedStatement stmt = getConnection().prepareStatement(SQL_DELETE)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) { 
            throw new DatabaseException("Error al eliminar paralelo", e);
        }
    }

    public void vaciarTabla() {
        try (PreparedStatement stmt = getConnection().prepareStatement(SQL_DELETE_ALL)) {
            stmt.executeUpdate();
            getConnection().prepareStatement(SQL_RESET_SEQ).executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error al vaciar tabla paralelos", e);
        }
    }

    public int insertarRetornandoId(String nombre, int matriculados, int idMateria, int idDocente) {
        try (PreparedStatement stmt = getConnection().prepareStatement(SQL_INSERT_BASIC, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, nombre); 
            stmt.setInt(2, matriculados);
            stmt.setInt(3, idMateria); 
            stmt.setInt(4, idDocente);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) { 
            throw new DatabaseException("Error al insertar paralelo retornando id", e);
        }
        return -1;
    }
}