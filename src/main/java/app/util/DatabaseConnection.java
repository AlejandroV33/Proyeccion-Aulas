package app.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import app.exception.DatabaseException;

public class DatabaseConnection {

    private static final String APP_FOLDER = "proyeccion_facultad";
    private static final String DB_NAME = "proyeccion_facultad.db";
    private static Connection instance;

    private static void copyDatabaseIfNotExists(File databaseFile) {
        if (databaseFile.exists()) {
            return;
        }

        try (InputStream is = DatabaseConnection.class.getResourceAsStream("/db/" + DB_NAME);
             FileOutputStream fos = new FileOutputStream(databaseFile)) {

            if (is == null) {
                return; // si no hay base en resources simplemente se creará vacía
            }

            byte[] buffer = new byte[4096];
            int length;

            while ((length = is.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
        } catch (IOException e) {
            throw new DatabaseException("Error copiando la base de datos inicial: " + e.getMessage(), e);
        }
    }

    private static synchronized Connection connect() {
        try {
            String userHome = System.getProperty("user.home");
            File appDirectory = new File(userHome, APP_FOLDER);

            if (!appDirectory.exists()) {
                boolean created = appDirectory.mkdirs();
                if (!created) {
                    throw new DatabaseException("No se pudo crear el directorio de la aplicacion");
                }
            }

            File databaseFile = new File(appDirectory, DB_NAME);
            copyDatabaseIfNotExists(databaseFile);
            String url = "jdbc:sqlite:" + databaseFile.getAbsolutePath();

            Connection conn = DriverManager.getConnection(url);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
                stmt.execute("PRAGMA journal_mode = WAL;");
                stmt.execute("PRAGMA synchronous = NORMAL;");
                stmt.execute("PRAGMA busy_timeout = 5000;");
            }
            return conn;
        } catch (SQLException e) {
            throw new DatabaseException("Error de conexión a la base de datos", e);
        } catch (SecurityException e) {
            throw new DatabaseException("Permisos insuficientes para crear el directorio de la aplicación", e);
        }
    }

    public static Connection getConnection() {
        if (instance != null) {
            try {
                if (!instance.isClosed()) {
                    return instance;
                }
            } catch (SQLException e) {
                // Ignore and reconnect
            }
        }
        instance = connect();
        return instance;
    }

    public static void init() {
        String sqlMaterias = """
                CREATE TABLE IF NOT EXISTS materias (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    codigo TEXT NOT NULL,
                    nombre TEXT NOT NULL,
                    departamento TEXT,
                    creditos INTEGER,
                    horas INTEGER,
                    semestre INTEGER NOT NULL,
                    aula_requerida INTEGER NOT NULL DEFAULT 1,
                    FOREIGN KEY (aula_requerida) REFERENCES tipos_aulas(id) ON DELETE SET DEFAULT
                );
                """;

        String sqlDocentes = """
                CREATE TABLE IF NOT EXISTS docentes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre TEXT NOT NULL,
                    cualquier_pizarra TEXT NOT NULL DEFAULT 'si' CHECK(cualquier_pizarra IN ('si', 'no'))
                );
                """;

        String sqlAulas = """
                CREATE TABLE IF NOT EXISTS aulas (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    edificio TEXT NOT NULL,
                    piso TEXT NOT NULL,
                    numero TEXT NOT NULL,
                    capacidad INTEGER,
                    estado TEXT NOT NULL DEFAULT 'activo' CHECK(estado IN ('activo', 'inactivo')),
                    disponibilidad TEXT NOT NULL DEFAULT 'libre' CHECK(disponibilidad IN ('libre', 'ocupada')),
                    usar_en_algoritmo TEXT NOT NULL DEFAULT 'si' CHECK(usar_en_algoritmo IN ('si', 'no')),
                    id_tipo_aula INTEGER NOT NULL DEFAULT 1,
                    FOREIGN KEY (id_tipo_aula) REFERENCES tipos_aulas(id) ON DELETE SET DEFAULT
                );
                """;

        String sqlParalelos = """
                CREATE TABLE IF NOT EXISTS paralelos (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre TEXT NOT NULL,
                    num_estudiantes_matriculados INTEGER,
                    id_materia INTEGER NOT NULL,
                    id_docente INTEGER,
                    espejo TEXT NOT NULL DEFAULT 'indiferente' CHECK(espejo IN ('si', 'no', 'indiferente')),
                    FOREIGN KEY (id_materia) REFERENCES materias(id) ON DELETE CASCADE,
                    FOREIGN KEY (id_docente) REFERENCES docentes(id) ON DELETE SET NULL
                );
                """;

        String sqlHorarios = """
                CREATE TABLE IF NOT EXISTS horarios (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    dia TEXT NOT NULL,
                    hora_inicio INTEGER NOT NULL,
                    hora_fin INTEGER NOT NULL,
                    id_paralelo INTEGER NOT NULL,
                    id_aula INTEGER,                
                    -- Métricas de eficiencia de este horario específico
                    proporcion_ocupacion TEXT,      
                    indice_ocupacion REAL,           
                    indice_ajuste_ocupacion REAL,                  
                    FOREIGN KEY (id_paralelo) REFERENCES paralelos(id) ON DELETE CASCADE,
                    FOREIGN KEY (id_aula) REFERENCES aulas(id) ON DELETE SET NULL,
                
                    -- Restricciones de integridad 
                    UNIQUE(id_aula, dia, hora_inicio),
                    UNIQUE(id_paralelo, dia, hora_inicio)
                );                
                """;

        String sqlTiposAulas = """
                CREATE TABLE IF NOT EXISTS tipos_aulas (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre TEXT NOT NULL UNIQUE
                );
                """;

        String sqlVistaResultadoFinal = """
                CREATE VIEW IF NOT EXISTS resultado_final_total AS
                SELECT
                    p.id AS id_paralelo,
                    d.id AS id_docente,
                    d.nombre AS profesor,
                    m.id AS id_materia,
                    m.nombre AS materia,
                    m.semestre,
                    m.departamento,
                    m.creditos,
                    m.horas,
                    p.nombre AS paralelo,
                    p.num_estudiantes_matriculados,
                    a.edificio,
                    a.piso,
                    a.numero,
                    a.capacidad,
                    a.estado,
                    a.disponibilidad,
                    ta.nombre AS tipo_aula,
                    
                    -- Tomamos el promedio o maximo de las metricas de los horarios agrupados
                    MAX(h.proporcion_ocupacion) as proporcion_ocupacion,
                    MAX(h.indice_ocupacion) as indice_ocupacion,
                    MAX(h.indice_ajuste_ocupacion) as indice_ajuste_ocupacion,
            
                    GROUP_CONCAT(CASE WHEN h.dia = 'lunes'
                        THEN h.hora_inicio || '-' || h.hora_fin END) AS lunes,
            
                    GROUP_CONCAT(CASE WHEN h.dia = 'martes'
                        THEN h.hora_inicio || '-' || h.hora_fin END) AS martes,
            
                    GROUP_CONCAT(CASE WHEN h.dia = 'miercoles'
                        THEN h.hora_inicio || '-' || h.hora_fin END) AS miercoles,
            
                    GROUP_CONCAT(CASE WHEN h.dia = 'jueves'
                        THEN h.hora_inicio || '-' || h.hora_fin END) AS jueves,
            
                    GROUP_CONCAT(CASE WHEN h.dia = 'viernes'
                        THEN h.hora_inicio || '-' || h.hora_fin END) AS viernes,
            
                    GROUP_CONCAT(CASE WHEN h.dia = 'sabado'
                        THEN h.hora_inicio || '-' || h.hora_fin END) AS sabado
            
                FROM paralelos p
                LEFT JOIN docentes d ON p.id_docente = d.id
                LEFT JOIN materias m ON p.id_materia = m.id
                LEFT JOIN horarios h ON p.id = h.id_paralelo
                LEFT JOIN aulas a ON h.id_aula = a.id
                LEFT JOIN tipos_aulas ta ON a.id_tipo_aula = ta.id
            
                GROUP BY p.id, a.id
                ORDER BY d.nombre;
            """;

        try (Statement stmt = getConnection().createStatement()) {

            stmt.execute(sqlTiposAulas);
            stmt.execute(sqlMaterias);
            stmt.execute(sqlDocentes);
            stmt.execute(sqlAulas);
            stmt.execute(sqlParalelos);
            stmt.execute(sqlHorarios);
            stmt.execute(sqlVistaResultadoFinal);

        } catch (SQLException e) {
            throw new DatabaseException("Error al iniciar base de datos: " + e.getMessage(), e);
        }
    }
}