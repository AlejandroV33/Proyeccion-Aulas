package app.model.entity;

import java.util.ArrayList;
import java.util.List;

public class AulaOcupacion {
    private int idAula;
    private String edificio;
    private String piso;
    private String numero;
    private int capacidad;

    private List<String> lunes = new ArrayList<>();
    private List<String> martes = new ArrayList<>();
    private List<String> miercoles = new ArrayList<>();
    private List<String> jueves = new ArrayList<>();
    private List<String> viernes = new ArrayList<>();

    private int horasLunes, horasMartes, horasMiercoles, horasJueves, horasViernes, horasTotal;

    // Getters
    public int getIdAula() { return idAula; }
    public String getEdificio() { return edificio; }
    public String getPiso() { return piso; }
    public String getNumero() { return numero; }
    public int getCapacidad() { return capacidad; }

    public int getHorasLunes() { return horasLunes; }
    public int getHorasMartes() { return horasMartes; }
    public int getHorasMiercoles() { return horasMiercoles; }
    public int getHorasJueves() { return horasJueves; }
    public int getHorasViernes() { return horasViernes; }
    public int getHorasTotal() { return horasTotal; }

    // Textos concatenados para la tabla
    public String getAulaDesc() { return piso + " / " + numero + "\nCap: " + capacidad; }
    public String getLunesText() { return String.join("\n\n", lunes); }
    public String getMartesText() { return String.join("\n\n", martes); }
    public String getMiercolesText() { return String.join("\n\n", miercoles); }
    public String getJuevesText() { return String.join("\n\n", jueves); }
    public String getViernesText() { return String.join("\n\n", viernes); }

    private String tipoAula;

    // Setters
    public void setIdAula(int idAula) { this.idAula = idAula; }
    public void setEdificio(String edificio) { this.edificio = edificio; }
    public void setPiso(String piso) { this.piso = piso; }
    public void setNumero(String numero) { this.numero = numero; }
    public void setCapacidad(int capacidad) { this.capacidad = capacidad; }
    public String getTipoAula() { return tipoAula; }
    public void setTipoAula(String tipoAula) { this.tipoAula = tipoAula; }

    // metodo de agrupación
    public void agregarClase(String dia, int inicio, int fin, String materia, String paralelo, String docenteRaw) {
        if (dia == null) return;

        // 1. Formateo Inteligente del Docente
        String docenteFormateado = "Sin profesor";
        if (docenteRaw != null && !docenteRaw.trim().isEmpty() && !docenteRaw.equalsIgnoreCase("Sin profesor")) {
            String[] palabras = docenteRaw.trim().split("\\s+");
            if (palabras.length >= 3) {
                docenteFormateado = palabras[0] + " " + palabras[2]; // Primera y tercera palabra
            } else if (palabras.length == 2) {
                docenteFormateado = palabras[0] + " " + palabras[1]; // Ambas palabras
            } else {
                docenteFormateado = palabras[0]; // Solo una palabra
            }
        }

        // 2. Construcción del texto del bloque
        String texto = " " + inicio + ":00 a " + fin + ":00\n " + materia + " (" + paralelo + ")\n " + docenteFormateado;

        int duracion = fin - inicio;
        horasTotal += duracion;

        switch(dia.toLowerCase()) {
            case "lunes": lunes.add(texto); horasLunes += duracion; break;
            case "martes": martes.add(texto); horasMartes += duracion; break;
            case "miercoles": miercoles.add(texto); horasMiercoles += duracion; break;
            case "jueves": jueves.add(texto); horasJueves += duracion; break;
            case "viernes": viernes.add(texto); horasViernes += duracion; break;
        }

        bloques.add(new Bloque(dia, inicio, fin, materia, paralelo, docenteFormateado));
    }

    // --- NUEVO: ESTRUCTURA PARA GUARDAR DATOS CRUDOS PARA EL EXCEL ---
    public static class Bloque {
        public String dia; public int inicio; public int fin;
        public String materia; public String paralelo; public String docente;

        public Bloque(String dia, int inicio, int fin, String materia, String paralelo, String docente) {
            this.dia = dia; this.inicio = inicio; this.fin = fin;
            this.materia = materia; this.paralelo = paralelo; this.docente = docente;
        }
    }

    private List<Bloque> bloques = new ArrayList<>();
    public List<Bloque> getBloques() { return bloques; }
}