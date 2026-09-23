package app.model.service;

import app.model.dao.VistaResultadoDAO;
import app.model.entity.ResultadoFinal;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;

import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExcelService {

    // Estilos globales
    private CellStyle headerStyle;
    private CellStyle groupHeaderStyle;
    private CellStyle subHeaderStyle;
    private CellStyle numberStyle;

    public void generarReporte(String rutaArchivo, List<ResultadoFinal> datos) {
        System.out.println(">> generando excel avanzado: " + rutaArchivo);

        try (Workbook workbook = new XSSFWorkbook()) {
            crearEstilos(workbook);

            // 1. HOJA CRONOGRAMA (Visual)
            crearHojaCronograma(workbook, datos);

            // 2. HOJA ESTADISTICAS (Agrupado Edificio -> Piso)
            crearHojaEstadisticas(workbook, datos);

            // 3. HOJA DETALLE TECNICO (Datos crudos con metricas)
            crearHojaDetalleTecnico(workbook, datos);

            try (FileOutputStream fileOut = new FileOutputStream(rutaArchivo)) {
                workbook.write(fileOut);
                System.out.println(">> excel creado exitosamente en el escritorio.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error generando Excel: " + e.getMessage());
        }
    }

    private CellStyle orangeStyle;
    private CellStyle blueStyle;
    private CellStyle yellowStyle;

    private void crearEstilos(Workbook wb) {
        headerStyle = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(font);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        groupHeaderStyle = wb.createCellStyle();
        Font groupFont = wb.createFont();
        groupFont.setBold(true);
        groupFont.setFontHeightInPoints((short)12);
        groupHeaderStyle.setFont(groupFont);
        groupHeaderStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        groupHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        subHeaderStyle = wb.createCellStyle();
        Font subFont = wb.createFont();
        subFont.setBold(true);
        subFont.setItalic(true);
        subHeaderStyle.setFont(subFont);
        subHeaderStyle.setFillForegroundColor(IndexedColors.LEMON_CHIFFON.getIndex());
        subHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        numberStyle = wb.createCellStyle();
        numberStyle.setDataFormat(wb.createDataFormat().getFormat("0.000"));

        orangeStyle = wb.createCellStyle();
        ((XSSFCellStyle) orangeStyle).setFillForegroundColor(new XSSFColor(new byte[]{(byte) 255, (byte) 230, (byte) 204}, null));
        orangeStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        blueStyle = wb.createCellStyle();
        ((XSSFCellStyle) blueStyle).setFillForegroundColor(new XSSFColor(new byte[]{(byte) 204, (byte) 235, (byte) 255}, null));
        blueStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        yellowStyle = wb.createCellStyle();
        ((XSSFCellStyle) yellowStyle).setFillForegroundColor(new XSSFColor(new byte[]{(byte) 255, (byte) 255, (byte) 204}, null));
        yellowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    }

    private void crearHojaCronograma(Workbook wb, List<ResultadoFinal> datos) {
        Sheet sheet = wb.createSheet("1. Cronograma");
        String[] headers = {"Docente", "Materia", "Tipo Aula", "Sem", "Paralelo", "Aula Asignada", "Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado"};

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        for (ResultadoFinal r : datos) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(r.getProfesor());
            
            String materiaFull = r.getMateria() + (r.getCodigoMateria() != null ? " (" + r.getCodigoMateria() + ")" : "");
            row.createCell(1).setCellValue(materiaFull);
            
            String tipo = (r.getTipoAula() != null && !r.getTipoAula().trim().equalsIgnoreCase("comun")) ? "Labo" : "Comun";
            row.createCell(2).setCellValue(tipo);
            
            row.createCell(3).setCellValue(r.getSemestre());
            row.createCell(4).setCellValue(r.getParalelo());

            String aula = (r.getAulaNumero() != null) ? r.getEdificio() + "/" + r.getPiso() + "/" + r.getAulaNumero() : "SIN ASIGNAR";
            row.createCell(5).setCellValue(aula);

            row.createCell(6).setCellValue(r.getLunes());
            row.createCell(7).setCellValue(r.getMartes());
            row.createCell(8).setCellValue(r.getMiercoles());
            row.createCell(9).setCellValue(r.getJueves());
            row.createCell(10).setCellValue(r.getViernes());
            row.createCell(11).setCellValue(r.getSabado());
        }

        for(int i=0; i<headers.length; i++) sheet.autoSizeColumn(i);
        sheet.setAutoFilter(new CellRangeAddress(0, rowNum-1, 0, headers.length-1));
    }

    private void crearHojaEstadisticas(Workbook wb, List<ResultadoFinal> datos) {
        Sheet sheet = wb.createSheet("2. Estadísticas Eficiencia");
        int rowNum = 0;

        // Filtrar solo los asignados para estadísticas
        List<ResultadoFinal> asignados = datos.stream()
                .filter(d -> d.getEdificio() != null)
                .toList();

        // Agrupar por Edificio
        Map<String, List<ResultadoFinal>> porEdificio = asignados.stream()
                .collect(Collectors.groupingBy(ResultadoFinal::getEdificio));

        // Títulos de columnas
        Row titleRow = sheet.createRow(rowNum++);
        titleRow.createCell(0).setCellValue("Ubicación");
        titleRow.createCell(1).setCellValue("Cantidad Aulas Usadas");
        titleRow.createCell(2).setCellValue("Indice Ocupación Promedio (Obj: 0.9)");
        titleRow.createCell(3).setCellValue("Indice Ajuste Promedio (Obj: 0.0)");

        for(int i=0; i<4; i++) titleRow.getCell(i).setCellStyle(headerStyle);

        // Iterar Edificios
        for (String edificio : porEdificio.keySet()) {
            List<ResultadoFinal> listaEdif = porEdificio.get(edificio);

            // Cabecera Edificio
            Row rowEdif = sheet.createRow(rowNum++);
            rowEdif.createCell(0).setCellValue("EDIFICIO: " + edificio);
            rowEdif.getCell(0).setCellStyle(groupHeaderStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 3));

            // Agrupar por Piso
            Map<String, List<ResultadoFinal>> porPiso = listaEdif.stream()
                    .collect(Collectors.groupingBy(ResultadoFinal::getPiso));

            for (String piso : porPiso.keySet()) {
                List<ResultadoFinal> listaPiso = porPiso.get(piso);

                double avgOcup = listaPiso.stream()
                        .mapToDouble(ResultadoFinal::getIndiceOcupacion)
                        .filter(Double::isFinite) // Evitar NaN o Infinity por division para 0
                        .average().orElse(0.0);
                        
                double avgAjuste = listaPiso.stream()
                        .mapToDouble(ResultadoFinal::getIndiceAjusteOcupacion)
                        .filter(Double::isFinite)
                        .average().orElse(0.0);

                Row rowPiso = sheet.createRow(rowNum++);
                rowPiso.createCell(0).setCellValue("   Piso: " + piso); // Sangría visual
                rowPiso.createCell(1).setCellValue(listaPiso.size());

                Cell cellOcup = rowPiso.createCell(2);
                cellOcup.setCellValue(avgOcup);
                cellOcup.setCellStyle(numberStyle);

                Cell cellAjuste = rowPiso.createCell(3);
                cellAjuste.setCellValue(avgAjuste);
                cellAjuste.setCellStyle(numberStyle);
            }
        }

        // TOTAL GENERAL
        rowNum++;
        double totalAvgOcup = asignados.stream()
                .mapToDouble(ResultadoFinal::getIndiceOcupacion)
                .filter(Double::isFinite)
                .average().orElse(0.0);
                
        double totalAvgAjuste = asignados.stream()
                .mapToDouble(ResultadoFinal::getIndiceAjusteOcupacion)
                .filter(Double::isFinite)
                .average().orElse(0.0);

        Row rowTotal = sheet.createRow(rowNum++);
        rowTotal.createCell(0).setCellValue("TOTAL FACULTAD");
        rowTotal.createCell(1).setCellValue(asignados.size());
        rowTotal.createCell(2).setCellValue(totalAvgOcup);
        rowTotal.getCell(2).setCellStyle(numberStyle);
        rowTotal.createCell(3).setCellValue(totalAvgAjuste);
        rowTotal.getCell(3).setCellStyle(numberStyle);
        rowNum++;

        // EXPLICACION DE METRICAS a la derecha (iniciando en fila 1, col 5)
        int expRow = 1;
        Row row = sheet.getRow(expRow);
        if(row == null) row = sheet.createRow(expRow);
        Cell cHeader = row.createCell(5);
        cHeader.setCellValue("GUÍA DE INTERPRETACIÓN DE MÉTRICAS");
        cHeader.setCellStyle(groupHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(expRow, expRow, 5, 8));
        expRow++;

        String[] lineasGuia = {
            "1. Índice de Ocupación Promedio:",
            "   - Se calcula dividiendo la cantidad de estudiantes sobre la capacidad máxima.",
            "   - Un valor de 0.9 significa ocupación al 90%.",
            "   - Un valor > 1.0 significa sobrecupo.",
            "   - Objetivo ideal: ~0.9.",
            "",
            "2. Índice de Ajuste Promedio (Penalización):",
            "   - Costo energético que evalúa la calidad matemática de la asignación.",
            "   - Valores cercanos a 0.0 indican un ajuste perfecto (armónico).",
            "   - Valores altos indican que se usó un aula poco óptima como último recurso.",
            "",
            "3. Restricciones Evaluadas por el Algoritmo (Simulated Annealing):",
            "   - FUERTES (Obligatorias):",
            "     * Cruce de horarios: Evita aulas superpuestas en el mismo día y hora.",
            "     * Tipo de aula: Cumple requisitos específicos (ej. Laboratorios de Computación).",
            "     * Capacidad: Intenta respetar el aforo físico + un 15% flexible máximo.",
            "   - SUAVES (Optimizadas):",
            "     * Distancia de Docentes: Minimiza caminatas largas entre edificios consecutivos.",
            "     * Dispersión de Aulas: Intenta mantener los paralelos de una materia en el mismo piso/edificio.",
            "     * Anclaje a Laboratorios: Agrupa las clases teóricas cerca de las prácticas."
        };

        for (String linea : lineasGuia) {
            row = sheet.getRow(expRow);
            if(row == null) row = sheet.createRow(expRow);
            row.createCell(5).setCellValue(linea);
            expRow++;
        }

        for(int i=0; i<4; i++) sheet.autoSizeColumn(i);
        sheet.autoSizeColumn(5);
    }

    private void crearHojaDetalleTecnico(Workbook wb, List<ResultadoFinal> datos) {
        Sheet sheet = wb.createSheet("3. Detalle Técnico");

        String[] headers = {
                "ID Paralelo", "Docente", "Materia", "Tipo Aula", "Sem", "Paralelo", "Matriculados",
                "Aula Asignada", "Capacidad",
                "Proporcion", "Indice Ocupación", "Indice Ajuste (Costo)"
        };

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        for (ResultadoFinal r : datos) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(r.getIdParalelo());
            row.createCell(1).setCellValue(r.getProfesor());
            
            String materiaFull = r.getMateria() + (r.getCodigoMateria() != null ? " (" + r.getCodigoMateria() + ")" : "");
            row.createCell(2).setCellValue(materiaFull);
            
            String tipo = (r.getTipoAula() != null && !r.getTipoAula().trim().equalsIgnoreCase("comun")) ? "Labo" : "Comun";
            row.createCell(3).setCellValue(tipo);
            
            row.createCell(4).setCellValue(r.getSemestre());
            row.createCell(5).setCellValue(r.getParalelo());
            row.createCell(6).setCellValue(r.getNumEstudiantes());

            if (r.getEdificio() != null) {
                String aula = r.getEdificio() + "/" + r.getPiso() + "/" + r.getAulaNumero();
                row.createCell(7).setCellValue(aula);
                row.createCell(8).setCellValue(r.getCapacidad());
                
                Cell cProp = row.createCell(9);
                cProp.setCellValue(r.getProporcionOcupacion());
                
                if (r.getCapacidad() == 0) {
                    cProp.setCellStyle(yellowStyle);
                } else if (r.getNumEstudiantes() == 0) {
                    cProp.setCellStyle(blueStyle);
                } else if (r.getIndiceOcupacion() > 1.0) {
                    cProp.setCellStyle(orangeStyle);
                }

                Cell cOcup = row.createCell(10);
                cOcup.setCellValue(r.getIndiceOcupacion());
                cOcup.setCellStyle(numberStyle);

                Cell cAjuste = row.createCell(11);
                cAjuste.setCellValue(r.getIndiceAjusteOcupacion());
                cAjuste.setCellStyle(numberStyle);
            } else {
                row.createCell(7).setCellValue("SIN ASIGNAR");
                // Celdas vacías o indicando error en el resto
                Cell err = row.createCell(11);
                err.setCellValue("ERROR / NO DISP");
            }
        }

        for(int i=0; i<headers.length; i++) sheet.autoSizeColumn(i);
        sheet.setAutoFilter(new CellRangeAddress(0, rowNum-1, 0, headers.length-1));
    }
}