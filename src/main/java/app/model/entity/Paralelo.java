package app.model.entity;

public class Paralelo {
    private int id;
    private String nombre;
    private int numEstudiantesMatriculados;
    private int idMateria; // fk
    private Integer idDocente; // fk, allow null
    private String espejo; // 'si', 'no', 'indiferente'
    
    // Virtual fields from joins
    private String materia;
    private String docente;

    public Paralelo() {}

    public Paralelo(int id, String nombre, int numEstudiantesMatriculados, int idMateria, Integer idDocente, String espejo) {
        this.id = id;
        this.nombre = nombre;
        this.numEstudiantesMatriculados = numEstudiantesMatriculados;
        this.idMateria = idMateria;
        this.idDocente = idDocente;
        this.espejo = espejo;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public int getNumEstudiantesMatriculados() { return numEstudiantesMatriculados; }
    public void setNumEstudiantesMatriculados(int numEstudiantesMatriculados) { this.numEstudiantesMatriculados = numEstudiantesMatriculados; }

    public int getIdMateria() { return idMateria; }
    public void setIdMateria(int idMateria) { this.idMateria = idMateria; }

    public Integer getIdDocente() { return idDocente; }
    public void setIdDocente(Integer idDocente) { this.idDocente = idDocente; }

    public String getEspejo() { return espejo; }
    public void setEspejo(String espejo) { this.espejo = espejo; }
    
    public String getMateria() { return materia; }
    public void setMateria(String materia) { this.materia = materia; }
    
    public String getDocente() { return docente; }
    public void setDocente(String docente) { this.docente = docente; }
    
    // Helper para tablas o combos
    public int getNumEstudiantes() { return numEstudiantesMatriculados; }

    @Override
    public String toString() {
        if (materia != null) {
            return materia + " - " + nombre + " (" + (docente != null ? docente : "Sin Docente") + ")";
        }
        return nombre;
    }
}