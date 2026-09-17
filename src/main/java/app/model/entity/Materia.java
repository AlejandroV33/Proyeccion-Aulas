package app.model.entity;

public class Materia {
    private int id;
    private String codigo;
    private String nombre;
    private String departamento;
    private int creditos;
    private int horas;
    private int semestre;
    private int idTipoAulaReq;
    private String tipoAulaReq; // Nombre del tipo

    public Materia() {}

    public Materia(int id, String codigo, String nombre, String departamento, int creditos, int horas, int semestre, int idTipoAulaReq) {
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
        this.departamento = departamento;
        this.creditos = creditos;
        this.horas = horas;
        this.semestre = semestre;
        this.idTipoAulaReq = idTipoAulaReq;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDepartamento() { return departamento; }
    public void setDepartamento(String departamento) { this.departamento = departamento; }

    public int getCreditos() { return creditos; }
    public void setCreditos(int creditos) { this.creditos = creditos; }

    public int getHoras() { return horas; }
    public void setHoras(int horas) { this.horas = horas; }

    public int getSemestre() { return semestre; }
    public void setSemestre(int semestre) { this.semestre = semestre; }

    public int getIdTipoAulaReq() { return idTipoAulaReq; }
    public void setIdTipoAulaReq(int idTipoAulaReq) { this.idTipoAulaReq = idTipoAulaReq; }

    public int getAulaRequerida() { return idTipoAulaReq; }
    public void setAulaRequerida(int aulaRequerida) { this.idTipoAulaReq = aulaRequerida; }

    public String getTipoAulaReq() { return tipoAulaReq; }
    public void setTipoAulaReq(String tipoAulaReq) { this.tipoAulaReq = tipoAulaReq; }

    @Override
    public String toString() {
        return nombre;
    }
}