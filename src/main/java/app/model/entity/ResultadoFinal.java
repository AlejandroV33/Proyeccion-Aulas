package app.model.entity;

public class ResultadoFinal {
    private int idParalelo;
    private String profesor;
    private int idDocente;
    private String materia;
    private String codigoMateria;
    private int idMateria;
    private int semestre;
    private String departamento;
    private int creditos;
    private int horas;
    private String paralelo;
    private int numEstudiantes;
    private String edificio;
    private String piso;
    private String aulaNumero;
    private int capacidad;
    private String tipoAula;
    private double indiceOcupacion;
    private double indiceAjusteOcupacion;
    private String proporcionOcupacion;

    // horarios concatenados por dia
    private String lunes;
    private String martes;
    private String miercoles;
    private String jueves;
    private String viernes;
    private String sabado;

    public ResultadoFinal() {}

    // getters y setters
    public int getIdParalelo() { return idParalelo; }
    public void setIdParalelo(int idParalelo) { this.idParalelo = idParalelo; }

    public String getProfesor() { return profesor; }
    public void setProfesor(String profesor) { this.profesor = profesor; }

    public int getIdDocente() { return idDocente; }
    public void setIdDocente(int idDocente) { this.idDocente = idDocente; }

    public String getMateria() { return materia; }
    public void setMateria(String materia) { this.materia = materia; }

    public String getCodigoMateria() { return codigoMateria; }
    public void setCodigoMateria(String codigoMateria) { this.codigoMateria = codigoMateria; }

    public int getIdMateria() { return idMateria; }
    public void setIdMateria(int idMateria) { this.idMateria = idMateria; }

    public int getSemestre() { return semestre; }
    public void setSemestre(int semestre) { this.semestre = semestre; }

    public String getDepartamento() { return departamento; }
    public void setDepartamento(String departamento) { this.departamento = departamento; }

    public int getCreditos() { return creditos; }
    public void setCreditos(int creditos) { this.creditos = creditos; }

    public int getHoras() { return horas; }
    public void setHoras(int horas) { this.horas = horas; }

    public String getParalelo() { return paralelo; }
    public void setParalelo(String paralelo) { this.paralelo = paralelo; }

    public int getNumEstudiantes() { return numEstudiantes; }
    public void setNumEstudiantes(int numEstudiantes) { this.numEstudiantes = numEstudiantes; }

    public String getEdificio() { return edificio; }
    public void setEdificio(String edificio) { this.edificio = edificio; }

    public String getPiso() { return piso; }
    public void setPiso(String piso) { this.piso = piso; }

    public String getAulaNumero() { return aulaNumero; }
    public void setAulaNumero(String aulaNumero) { this.aulaNumero = aulaNumero; }

    public int getCapacidad() { return capacidad; }
    public void setCapacidad(int capacidad) { this.capacidad = capacidad; }

    public String getTipoAula() { return tipoAula; }
    public void setTipoAula(String tipoAula) { this.tipoAula = tipoAula; }

    public String getLunes() { return lunes; }
    public void setLunes(String lunes) { this.lunes = lunes; }

    public String getMartes() { return martes; }
    public void setMartes(String martes) { this.martes = martes; }

    public String getMiercoles() { return miercoles; }
    public void setMiercoles(String miercoles) { this.miercoles = miercoles; }

    public String getJueves() { return jueves; }
    public void setJueves(String jueves) { this.jueves = jueves; }

    public String getViernes() { return viernes; }
    public void setViernes(String viernes) { this.viernes = viernes; }

    public String getSabado() { return sabado; }
    public void setSabado(String sabado) { this.sabado = sabado; }

    public double getIndiceOcupacion() { return indiceOcupacion; }
    public void setIndiceOcupacion(double indiceOcupacion) { this.indiceOcupacion = indiceOcupacion; }

    public double getIndiceAjusteOcupacion() { return indiceAjusteOcupacion; }
    public void setIndiceAjusteOcupacion(double indiceAjusteOcupacion) { this.indiceAjusteOcupacion = indiceAjusteOcupacion; }

    public String getProporcionOcupacion() { return proporcionOcupacion; }
    public void setProporcionOcupacion(String proporcionOcupacion) { this.proporcionOcupacion = proporcionOcupacion; }
}