package app.model.dao;

import app.model.entity.ResultadoFinal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;

public class VistaResultadoDAO extends BaseDAO {

    public List<ResultadoFinal> listarResultados() {
        List<ResultadoFinal> lista = new ArrayList<>();
        // consulta directa a la vista creada en init()
        String sql = "SELECT * FROM resultado_final_total";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                ResultadoFinal r = new ResultadoFinal();

                // datos principales para identificar el paralelo al editar
                r.setIdParalelo(rs.getInt("id_paralelo"));
                r.setIdDocente(rs.getInt("id_docente"));
                r.setIdMateria(rs.getInt("id_materia"));

                // datos informativos
                r.setProfesor(rs.getString("profesor"));
                r.setMateria(rs.getString("materia"));
                r.setCodigoMateria(rs.getString("codigo_materia"));
                r.setSemestre(rs.getInt("semestre"));
                r.setDepartamento(rs.getString("departamento"));
                r.setCreditos(rs.getInt("creditos"));
                r.setHoras(rs.getInt("horas"));
                r.setParalelo(rs.getString("paralelo"));
                r.setNumEstudiantes(rs.getInt("num_estudiantes_matriculados"));

                // datos de aula asignada (puede ser null si aun no hay asignacion)
                r.setEdificio(rs.getString("edificio"));
                r.setPiso(rs.getString("piso"));
                r.setAulaNumero(rs.getString("numero"));
                r.setCapacidad(rs.getInt("capacidad"));
                r.setTipoAula(rs.getString("tipo_aula"));

                // horarios concatenados
                r.setLunes(rs.getString("lunes"));
                r.setMartes(rs.getString("martes"));
                r.setMiercoles(rs.getString("miercoles"));
                r.setJueves(rs.getString("jueves"));
                r.setViernes(rs.getString("viernes"));
                r.setSabado(rs.getString("sabado"));

                r.setProporcionOcupacion(rs.getString("proporcion_ocupacion"));
                r.setIndiceOcupacion(rs.getDouble("indice_ocupacion"));
                r.setIndiceAjusteOcupacion(rs.getDouble("indice_ajuste_ocupacion"));

                lista.add(r);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "error al cargar vista resultado: " + e.getMessage());
        }
        return lista;
    }
}