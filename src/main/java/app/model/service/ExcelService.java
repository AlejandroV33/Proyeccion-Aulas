package app.model.service;

import app.model.dao.VistaResultadoDAO;
import app.model.entity.ResultadoFinal;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

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
    }

    private void crearHojaCronograma(Workbook wb, List<ResultadoFinal> datos) {
        Sheet sheet = wb.createSheet("1. Cronograma");
        String[] headers = {"Docente", "Materia", "Sem", "Paralelo", "Aula Asignada", "Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado"};

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
            row.createCell(1).setCellValue(r.getMateria());
            row.createCell(2).setCellValue(r.getSemestre());
            row.createCell(3).setCellValue(r.getParalelo());

            String aula = (r.getAulaNumero() != null) ? r.getEdificio() + "/" + r.getPiso() + "/" + r.getAulaNumero() : "SIN ASIGNAR";
            row.createCell(4).setCellValue(aula);

            row.createCell(5).setCellValue(r.getLunes());
            row.createCell(6).setCellValue(r.getMartes());
            row.createCell(7).setCellValue(r.getMiercoles());
            row.createCell(8).setCellValue(r.getJueves());
            row.createCell(9).setCellValue(r.getViernes());
            row.createCell(10).setCellValue(r.getSabado());
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

                double avgOcup = listaPiso.stream().mapToDouble(ResultadoFinal::getIndiceOcupacion).average().orElse(0.0);
                double avgAjuste = listaPiso.stream().mapToDouble(ResultadoFinal::getIndiceAjusteOcupacion).average().orElse(0.0);

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
        double totalAvgOcup = asignados.stream().mapToDouble(ResultadoFinal::getIndiceOcupacion).average().orElse(0.0);
        double totalAvgAjuste = asignados.stream().mapToDouble(ResultadoFinal::getIndiceAjusteOcupacion).average().orElse(0.0);

        Row rowTotal = sheet.createRow(rowNum);
        rowTotal.createCell(0).setCellValue("TOTAL FACULTAD");
        rowTotal.createCell(1).setCellValue(asignados.size());
        rowTotal.createCell(2).setCellValue(totalAvgOcup);
        rowTotal.getCell(2).setCellStyle(numberStyle);
        rowTotal.createCell(3).setCellValue(totalAvgAjuste);
        rowTotal.getCell(3).setCellStyle(numberStyle);

        for(int i=0; i<4; i++) sheet.autoSizeColumn(i);
    }

    private void crearHojaDetalleTecnico(Workbook wb, List<ResultadoFinal> datos) {
        Sheet sheet = wb.createSheet("3. Detalle Técnico");

        String[] headers = {
                "ID Paralelo", "Materia", "Sem", "Paralelo", "Matriculados",
                "Edificio", "Piso", "Aula", "Capacidad",
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
            row.createCell(1).setCellValue(r.getMateria());
            row.createCell(2).setCellValue(r.getSemestre());
            row.createCell(3).setCellValue(r.getParalelo());
            row.createCell(4).setCellValue(r.getNumEstudiantes());

            if (r.getEdificio() != null) {
                row.createCell(5).setCellValue(r.getEdificio());
                row.createCell(6).setCellValue(r.getPiso());
                row.createCell(7).setCellValue(r.getAulaNumero());
                row.createCell(8).setCellValue(r.getCapacidad());
                row.createCell(9).setCellValue(r.getProporcionOcupacion());

                Cell cOcup = row.createCell(10);
                cOcup.setCellValue(r.getIndiceOcupacion());
                cOcup.setCellStyle(numberStyle);

                Cell cAjuste = row.createCell(11);
                cAjuste.setCellValue(r.getIndiceAjusteOcupacion());
                cAjuste.setCellStyle(numberStyle);
            } else {
                row.createCell(5).setCellValue("SIN ASIGNAR");
                // Celdas vacías o indicando error en el resto
                Cell err = row.createCell(11);
                err.setCellValue("ERROR / NO DISP");
            }
        }

        for(int i=0; i<headers.length; i++) sheet.autoSizeColumn(i);
        sheet.setAutoFilter(new CellRangeAddress(0, rowNum-1, 0, headers.length-1));
    }
}