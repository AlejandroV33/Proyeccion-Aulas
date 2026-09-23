package app.model.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import app.model.dao.*;
import app.model.entity.*;
import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExcelExtractorService {

    private final DocenteDAO docenteDAO = new DocenteDAO();
    private final MateriaDAO materiaDAO = new MateriaDAO();
    private final ParaleloDAO paraleloDAO = new ParaleloDAO();
    private final AulaDAO aulaDAO = new AulaDAO();
    private final HorarioDAO horarioDAO = new HorarioDAO();

    public void extraerEInyectar(File excelFile, Consumer<String> logger) throws Exception {
        logger.accept(">> Iniciando proceso de extracción profunda...");

        BaseDAO.startTransaction();
        try {
            // 1. VACIAR TABLAS DEPENDIENTES
            logger.accept(">> Limpiando tablas: Horarios, Paralelos y Docentes...");
            horarioDAO.vaciarTabla();
            paraleloDAO.vaciarTabla();
            docenteDAO.vaciarTabla();

            // Cachés en memoria para no duplicar
            Map<String, Integer> cacheDocentes = new HashMap<>();
            Map<String, Integer> cacheMaterias = new HashMap<>();
            Map<String, Integer> cacheParalelos = new HashMap<>();

            try (FileInputStream fis = new FileInputStream(excelFile);
                 Workbook wb = new XSSFWorkbook(fis)) {

                Sheet sheet = wb.getSheetAt(0);
                Row headerRow = sheet.getRow(0);
                Map<String, Integer> colMap = mapearColumnas(headerRow);

                logger.accept(">> Analizando " + sheet.getLastRowNum() + " filas encontradas...");

                for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;

                    // Verificar si la fila tiene materia, si no, es una fila vacía del excel y la saltamos
                    String materiaFull = getCell(row, colMap.get("MATERIA"));
                    if (materiaFull.isEmpty()) continue;

                    int idDocente = extraerDocente(row, colMap, cacheDocentes);
                    int idMateria = extraerMateria(row, colMap, cacheMaterias, materiaFull, logger);
                    int idParalelo = extraerParalelo(row, colMap, cacheParalelos, idMateria, idDocente);
                    Integer idAula = extraerAula(row, colMap);
                    extraerHorarios(row, colMap, idParalelo, idAula);
                }
                
                BaseDAO.commitTransaction();
                logger.accept(">> INYECCIÓN DE DATOS FINALIZADA EXITOSAMENTE.");
            }
        } catch (Exception e) {
            BaseDAO.rollbackTransaction();
            logger.accept(">> ERROR DURANTE LA EXTRACCION. HACIENDO ROLLBACK DE LA BASE DE DATOS.");
            throw e;
        }
    }

    private int extraerDocente(Row row, Map<String, Integer> colMap, Map<String, Integer> cacheDocentes) throws Exception {
        String nombreProf = getCell(row, colMap.get("PROFESOR"));
        if (nombreProf.trim().isEmpty() || nombreProf.trim().equalsIgnoreCase("SIN PROFESOR")) {
            nombreProf = "Sin profesor";
        }

        if (cacheDocentes.containsKey(nombreProf)) {
            return cacheDocentes.get(nombreProf);
        } else {
            Docente d = new Docente();
            d.setNombre(nombreProf);
            d.setCualquierPizarra("si");
            int idDocente = docenteDAO.insertarRetornandoId(d);
            cacheDocentes.put(nombreProf, idDocente);
            return idDocente;
        }
    }

    private int extraerMateria(Row row, Map<String, Integer> colMap, Map<String, Integer> cacheMaterias, String materiaFull, Consumer<String> logger) throws Exception {
        String nombreMateria = materiaFull;
        String codigoMateria = "";
        Matcher matcher = Pattern.compile("(.*?)\\s*\\(([^)]+)\\)$").matcher(materiaFull);
        if (matcher.find()) {
            nombreMateria = matcher.group(1).trim();
            codigoMateria = matcher.group(2).trim();
        }

        if (cacheMaterias.containsKey(codigoMateria)) {
            return cacheMaterias.get(codigoMateria);
        } else {
            Integer dbId = materiaDAO.buscarIdPorCodigoONombre(codigoMateria, nombreMateria);
            int idMateria;
            if (dbId != null) {
                idMateria = dbId;
            } else {
                int sem = 1;
                try { sem = Integer.parseInt(getCell(row, colMap.get("SEMESTRE"))); } catch(Exception ignored){}
                idMateria = materiaDAO.insertarMínimaRetornandoId(codigoMateria, nombreMateria, sem);
                logger.accept("  + Nueva materia registrada: " + codigoMateria);
            }
            cacheMaterias.put(codigoMateria, idMateria);
            return idMateria;
        }
    }

    private int extraerParalelo(Row row, Map<String, Integer> colMap, Map<String, Integer> cacheParalelos, int idMateria, int idDocente) throws Exception {
        String nombreParalelo = getCell(row, colMap.get("PARALELO"));
        if (nombreParalelo.isEmpty()) nombreParalelo = "S/N";

        int estLegalizados = 0;
        try { estLegalizados = (int) Double.parseDouble(getCell(row, colMap.get("ESTLEGALIZADOS"))); } catch(Exception ignored){}

        String keyParalelo = idMateria + "_" + idDocente + "_" + nombreParalelo;
        if (cacheParalelos.containsKey(keyParalelo)) {
            return cacheParalelos.get(keyParalelo);
        } else {
            int idParalelo = paraleloDAO.insertarRetornandoId(nombreParalelo, estLegalizados, idMateria, idDocente);
            cacheParalelos.put(keyParalelo, idParalelo);
            return idParalelo;
        }
    }

    private Integer extraerAula(Row row, Map<String, Integer> colMap) throws Exception {
        String textoAula = getCell(row, colMap.get("AULA"));
        return aulaDAO.buscarAulaInteligente(textoAula);
    }

    private void extraerHorarios(Row row, Map<String, Integer> colMap, int idParalelo, Integer idAula) throws Exception {
        String[] dias = {"LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO"};
        for (String diaStr : dias) {
            Integer idxDia = colMap.get(diaStr);
            if (idxDia == null) continue;

            String horas = getCell(row, idxDia);
            if (!horas.isEmpty()) {
                Matcher m = Pattern.compile("(\\d+)\\s*-\\s*(\\d+)").matcher(horas);
                while (m.find()) {
                    Horario h = new Horario();
                    h.setDia(diaStr.toLowerCase());
                    h.setHoraInicio(Integer.parseInt(m.group(1)));
                    h.setHoraFin(Integer.parseInt(m.group(2)));
                    h.setIdParalelo(idParalelo);
                    h.setIdAula(idAula);
                    horarioDAO.insertar(h);
                }
            }
        }
    }

    private Map<String, Integer> mapearColumnas(Row header) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < header.getLastCellNum(); i++) {
            Cell c = header.getCell(i);
            if (c != null && c.getCellType() == CellType.STRING) {
                String val = c.getStringCellValue().trim().toUpperCase();
                val = java.text.Normalizer.normalize(val, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
                map.put(val, i);
            }
        }
        return map;
    }

    private String getCell(Row row, Integer index) {
        if (index == null) return "";
        Cell c = row.getCell(index);
        if (c == null) return "";
        switch (c.getCellType()) {
            case STRING: return c.getStringCellValue().trim();
            case NUMERIC:
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(c)) {
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    cal.setTime(c.getDateCellValue());
                    int month = cal.get(java.util.Calendar.MONTH) + 1;
                    int day = cal.get(java.util.Calendar.DAY_OF_MONTH);
                    return Math.min(month, day) + "-" + Math.max(month, day);
                }
                double val = c.getNumericCellValue();
                return (val % 1 == 0) ? String.valueOf((long)val) : String.valueOf(val);
            case BOOLEAN: return String.valueOf(c.getBooleanCellValue());
            default: return "";
        }
    }
}