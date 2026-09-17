package app.model.entity;

public class Aula {
    private int id;
    private String edificio;
    private String piso;
    private String numero;
    private int capacidad;
    private String estado;
    private String disponibilidad;
    private int idTipoAula;
    private String tipoAula;

    public Aula() {}

    public Aula(int id, String edificio, String piso, String numero, int capacidad, String estado, String disponibilidad, int idTipoAula) {
        this.id = id;
        this.edificio = edificio;
        this.piso = piso;
        this.numero = numero;
        this.capacidad = capacidad;
        this.estado = estado;
        this.disponibilidad = disponibilidad;
        this.idTipoAula = idTipoAula;
    }

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getEdificio() { return edificio; }
    public void setEdificio(String edificio) { this.edificio = edificio; }

    public String getPiso() { return piso; }
    public void setPiso(String piso) { this.piso = piso; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public int getCapacidad() { return capacidad; }
    public void setCapacidad(int capacidad) { this.capacidad = capacidad; }

    //se pueden mover sillas y aumentar la capacidad
    public int getCapacidadFlexible() {
        return (capacidad + 3);
    }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getDisponibilidad() { return disponibilidad; }
    public void setDisponibilidad(String disponibilidad) { this.disponibilidad = disponibilidad; }

    public int getIdTipoAula() { return idTipoAula; }
    public void setIdTipoAula(int idTipoAula) { this.idTipoAula = idTipoAula; }

    public String getTipoAula() { return tipoAula; }
    public void setTipoAula(String tipoAula) { this.tipoAula = tipoAula; }

    @Override
    public String toString() {
        return edificio + "/" + piso + "/" + numero + " (" + capacidad + ")";
    }
}