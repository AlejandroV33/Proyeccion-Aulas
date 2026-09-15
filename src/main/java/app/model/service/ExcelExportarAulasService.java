package app.model.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import app.model.entity.AulaOcupacion;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class ExcelExportarAulasService {

    public void exportarVisor(List<AulaOcupacion> aulas, File file) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Ocupación Aulas");

            Map<String, CellStyle> estilos = configurarEstilos(wb);
            dibujarEncabezados(sheet, estilos.get("header"));
            dibujarCuadricula(sheet, aulas, estilos);
            ajustarColumnas(sheet);

            try (FileOutputStream out = new FileOutputStream(file)) {
                wb.write(out);
            }
        }
    }

    private Map<String, CellStyle> configurarEstilos(Workbook wb) {
        Map<String, CellStyle> estilos = new HashMap<>();

        CellStyle headerStyle = wb.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        Font fontBold = wb.createFont(); fontBold.setBold(true);
        headerStyle.setFont(fontBold);
        setBorders(headerStyle);
        estilos.put("header", headerStyle);

        CellStyle aulaNormalStyle = wb.createCellStyle();
        aulaNormalStyle.setAlignment(HorizontalAlignment.CENTER);
        aulaNormalStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        aulaNormalStyle.setWrapText(true);
        setBorders(aulaNormalStyle);
        estilos.put("aulaNormal", aulaNormalStyle);

        CellStyle aulaShadedStyle = wb.createCellStyle();
        aulaShadedStyle.cloneStyleFrom(aulaNormalStyle);
        ((XSSFCellStyle) aulaShadedStyle).setFillForegroundColor(new XSSFColor(new byte[]{(byte) 230, (byte) 230, (byte) 230}, null));
        aulaShadedStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        estilos.put("aulaShaded", aulaShadedStyle);

        CellStyle horaStyle = wb.createCellStyle();
        horaStyle.setAlignment(HorizontalAlignment.CENTER);
        horaStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(horaStyle);
        estilos.put("hora", horaStyle);

        CellStyle bloqueStyle = wb.createCellStyle();
        bloqueStyle.setAlignment(HorizontalAlignment.CENTER);
        bloqueStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        bloqueStyle.setWrapText(true);
        ((XSSFCellStyle) bloqueStyle).setFillForegroundColor(new XSSFColor(new byte[]{(byte) 220, (byte) 240, (byte) 255}, null));
        bloqueStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBorders(bloqueStyle);
        estilos.put("bloque", bloqueStyle);

        CellStyle emptyStyle = wb.createCellStyle();
        setBorders(emptyStyle);
        estilos.put("empty", emptyStyle);

        CellStyle separadorStyle = wb.createCellStyle();
        ((XSSFCellStyle) separadorStyle).setFillForegroundColor(new XSSFColor(new byte[]{(byte) 200, (byte) 200, (byte) 200}, null));
        separadorStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        estilos.put("separador", separadorStyle);

        return estilos;
    }

    private void dibujarEncabezados(Sheet sheet, CellStyle headerStyle) {
        String[] headers = {"AULA", "HORA", "LUNES", "MARTES", "MIÉRCOLES", "JUEVES", "VIERNES", "SÁBADO"};
        Row rowHeader = sheet.createRow(0);
        rowHeader.setHeightInPoints(25);
        for (int i = 0; i < headers.length; i++) {
            Cell c = rowHeader.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(headerStyle);
        }
    }

    private void dibujarCuadricula(Sheet sheet, List<AulaOcupacion> aulas, Map<String, CellStyle> estilos) {
        int rowIndex = 1;
        for (int a = 0; a < aulas.size(); a++) {
            AulaOcupacion aula = aulas.get(a);
            boolean isShaded = (a % 2 != 0);
            CellStyle currentAulaStyle = isShaded ? estilos.get("aulaShaded") : estilos.get("aulaNormal");

            int startRow = rowIndex;
            int endRow = startRow + 12; // 13 horas (7 a 20)

            boolean[] horasOcupadas = new boolean[13];
            for (AulaOcupacion.Bloque b : aula.getBloques()) {
                for (int h = b.inicio; h < b.fin; h++) {
                    if (h >= 7 && h < 20) horasOcupadas[h - 7] = true;
                }
            }

            for (int h = 0; h < 13; h++) {
                Row r = sheet.createRow(startRow + h);
                r.setHeightInPoints(horasOcupadas[h] ? 45 : 18);

                for(int c = 0; c < 8; c++){
                    r.createCell(c).setCellStyle(estilos.get("empty"));
                }

                Cell cHora = r.getCell(1);
                int horaIn = 7 + h;
                cHora.setCellValue(horaIn + "-" + (horaIn + 1));
                cHora.setCellStyle(estilos.get("hora"));
            }

            Row firstRow = sheet.getRow(startRow);
            Cell cAula = firstRow.getCell(0);
            cAula.setCellValue(aula.getEdificio() + "/" + aula.getPiso() + "/" + aula.getNumero() + "\n(" + aula.getCapacidad() + ")");
            cAula.setCellStyle(currentAulaStyle);
            sheet.addMergedRegion(new CellRangeAddress(startRow, endRow, 0, 0));

            for (AulaOcupacion.Bloque b : aula.getBloques()) {
                int colDia = mapDiaAColumna(b.dia);
                if (colDia == -1) continue;

                int bStart = startRow + (b.inicio - 7);
                int bEnd = startRow + (b.fin - 1 - 7);

                if (bStart > bEnd || bStart < startRow || bEnd > endRow) continue;
                if (bStart < bEnd) {
                    sheet.addMergedRegion(new CellRangeAddress(bStart, bEnd, colDia, colDia));
                }

                Cell cBlock = sheet.getRow(bStart).getCell(colDia);
                cBlock.setCellValue(b.materia + " (" + b.paralelo + ")\n" + b.docente);
                cBlock.setCellStyle(estilos.get("bloque"));
            }

            Row sepRow = sheet.createRow(endRow + 1);
            sepRow.setHeightInPoints(10);
            for (int c = 0; c < 8; c++) {
                sepRow.createCell(c).setCellStyle(estilos.get("separador"));
            }
            sheet.addMergedRegion(new CellRangeAddress(endRow + 1, endRow + 1, 0, 7));

            rowIndex += 14;
        }
    }

    private void ajustarColumnas(Sheet sheet) {
        sheet.setColumnWidth(0, 15 * 256);
        sheet.setColumnWidth(1, 10 * 256);
        for (int i = 2; i < 8; i++) sheet.setColumnWidth(i, 20 * 256);
    }

    private void setBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private int mapDiaAColumna(String dia) {
        if (dia == null) return -1;
        String d = dia.toLowerCase().replace("é", "e").replace("á", "a");
        switch (d) {
            case "lunes": return 2; case "martes": return 3; case "miercoles": return 4;
            case "jueves": return 5; case "viernes": return 6; case "sabado": return 7;
            default: return -1;
        }
    }
}