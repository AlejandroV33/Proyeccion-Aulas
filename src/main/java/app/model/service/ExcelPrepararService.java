package app.model.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.text.Normalizer;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ExcelPrepararService {

    // Palabras a ignorar al comparar nombres
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList("DE", "LA", "LAS", "EL", "LOS", "DEL", "Y", "SAN"));

    public Workbook preparaExcelMemoria(File inputFile, Consumer<String> logger) throws Exception {
        logger.accept(">> cargando archivo excel: " + inputFile.getName());

        FileInputStream is = new FileInputStream(inputFile);
        XSSFWorkbook wb = new XSSFWorkbook(is);
        Sheet ws = wb.getSheetAt(0);

        Row headerRow = ws.getRow(0);
        if (headerRow == null) {
            throw new Exception("Error: la primera hoja está vacía o no tiene fila de encabezado.");
        }

        int maxCols = headerRow.getLastCellNum() <= 0 ? 0 : headerRow.getLastCellNum();
        List<String> encabezados = new ArrayList<>();
        for (int c = 0; c < maxCols; c++) encabezados.add(getCellString(headerRow.getCell(c)));

        List<String> encabezadosNorm = encabezados.stream().map(ExcelPrepararService::normalizarParaBusqueda).collect(Collectors.toList());

        Integer colMatIdx = null;
        Integer colProfIdx = null;

        for (int i = 0; i < encabezadosNorm.size(); i++) {
            if (encabezadosNorm.get(i).equals("MATERIA") && colMatIdx == null) colMatIdx = i;
            if (encabezadosNorm.get(i).equals("PROFESOR") && colProfIdx == null) colProfIdx = i;
        }

        if (colMatIdx == null) throw new Exception("Error: no se encontró la columna 'Materia'");
        if (colProfIdx == null) throw new Exception("Error: no se encontró la columna 'Profesor'");

        logger.accept(">> insertando columna 'Semestre'...");
        agregarColumnaSemestre(wb, ws, colMatIdx, maxCols);
        maxCols = Math.max(maxCols, colMatIdx + 2); // Actualizar maxCols tras insertar

        logger.accept(">> unificando textos inteligentemente y preservando tildes/eñes...");
        unificarTextosInteligente(ws, colMatIdx, colProfIdx);

        logger.accept(">> corrigiendo auto-formatos de fecha en las horas de clase...");
        repararFechasHorarios(wb, ws, headerRow, maxCols);

        logger.accept(">> analizando posibles choques de horarios...");
        generarHojasDeChoques(wb, ws, headerRow, maxCols, logger);

        logger.accept(">> analisis completado con exito. listo para guardar.");
        return wb;
    }

    private void agregarColumnaSemestre(Workbook wb, Sheet ws, int colMatIdx, int maxCols) {
        int colSemIdx = colMatIdx + 1;
        insertarColumna(ws, colSemIdx, maxCols);

        Row headerRow = ws.getRow(0);
        headerRow.getCell(colSemIdx).setCellValue("Semestre");
        CellStyle estiloSem = wb.createCellStyle();
        Font fuente = wb.createFont();
        fuente.setFontName("Verdana"); fuente.setFontHeightInPoints((short)4);
        estiloSem.setFont(fuente);
        estiloSem.setBorderTop(BorderStyle.THIN); estiloSem.setBorderBottom(BorderStyle.THIN);
        estiloSem.setBorderLeft(BorderStyle.THIN); estiloSem.setBorderRight(BorderStyle.THIN);
        headerRow.getCell(colSemIdx).setCellStyle(estiloSem);

        int lastRow = ws.getLastRowNum();
        for (int r = 1; r <= lastRow; r++) {
            Row row = ws.getRow(r);
            if (row == null) continue;
            String semestre = obtenerSemestre(getCellString(row.getCell(colMatIdx)));
            Cell celSem = row.getCell(colSemIdx);
            if (celSem == null) celSem = row.createCell(colSemIdx);
            celSem.setCellValue(semestre);
            celSem.setCellStyle(estiloSem);
        }
    }

    private void unificarTextosInteligente(Sheet ws, int colMatIdx, int colProfIdx) {
        int lastRow = ws.getLastRowNum();
        Map<String, String> matUnificada = new HashMap<>();
        Set<String> nombresRawOriginales = new HashSet<>();

        for (int r = 1; r <= lastRow; r++) {
            Row row = ws.getRow(r);
            if (row == null) continue;

            Cell cMat = row.getCell(colMatIdx);
            if (cMat != null && cMat.getCellType() != CellType.BLANK) {
                String matOrig = getCellString(cMat).trim().toUpperCase();
                matUnificada.putIfAbsent(normalizarParaBusqueda(matOrig), matOrig);
            }

            Cell cProf = row.getCell(colProfIdx);
            if (cProf != null && cProf.getCellType() != CellType.BLANK) {
                String profOrig = getCellString(cProf).trim().toUpperCase();
                if (!profOrig.isEmpty() && !profOrig.equalsIgnoreCase("SIN PROFESOR")) {
                    nombresRawOriginales.add(profOrig);
                }
            }
        }

        List<String> nombresOrdenados = new ArrayList<>(nombresRawOriginales);
        nombresOrdenados.sort((a, b) -> Integer.compare(b.length(), a.length()));

        Map<String, String> diccionarioProfesores = new HashMap<>();
        List<String> canonicosOriginales = new ArrayList<>();

        for (String nombreOrig : nombresOrdenados) {
            String normBusqueda = normalizarParaBusqueda(nombreOrig);
            Set<String> palabrasSigCorto = obtenerPalabrasSignificativas(normBusqueda);
            boolean encontrado = false;

            for (String canonicoOrig : canonicosOriginales) {
                Set<String> palabrasSigCanonico = obtenerPalabrasSignificativas(normalizarParaBusqueda(canonicoOrig));

                if (palabrasSigCanonico.containsAll(palabrasSigCorto)) {
                    diccionarioProfesores.put(normBusqueda, canonicoOrig);
                    encontrado = true;
                    break;
                }
            }

            if (!encontrado) {
                canonicosOriginales.add(nombreOrig);
                diccionarioProfesores.put(normBusqueda, nombreOrig);
            }
        }

        for (int r = 1; r <= lastRow; r++) {
            Row row = ws.getRow(r);
            if (row == null) continue;

            Cell cMat = row.getCell(colMatIdx);
            if (cMat != null && cMat.getCellType() != CellType.BLANK) {
                String matOrig = getCellString(cMat).trim().toUpperCase();
                cMat.setCellValue(matUnificada.get(normalizarParaBusqueda(matOrig)));
            }

            Cell cProf = row.getCell(colProfIdx);
            if (cProf != null && cProf.getCellType() != CellType.BLANK) {
                String profOrig = getCellString(cProf).trim().toUpperCase();
                if (profOrig.equalsIgnoreCase("SIN PROFESOR") || profOrig.isEmpty()) {
                    cProf.setCellValue("Sin profesor");
                } else {
                    cProf.setCellValue(diccionarioProfesores.getOrDefault(normalizarParaBusqueda(profOrig), profOrig));
                }
            }
        }
    }

    private void repararFechasHorarios(Workbook wb, Sheet ws, Row headerRow, int maxCols) {
        List<String> encabezados = new ArrayList<>();
        for (int c = 0; c < maxCols; c++) encabezados.add(getCellString(headerRow.getCell(c)));
        List<String> encabezadosNorm = encabezados.stream().map(ExcelPrepararService::normalizarParaBusqueda).collect(Collectors.toList());

        List<Integer> indicesDias = Arrays.asList("LUNES","MARTES","MIERCOLES","JUEVES","VIERNES","SABADO").stream()
                .map(encabezadosNorm::indexOf).filter(i -> i >= 0).collect(Collectors.toList());

        CellStyle estiloTexto = wb.createCellStyle();
        estiloTexto.setDataFormat(wb.createDataFormat().getFormat("@"));

        int lastRow = ws.getLastRowNum();
        for (int r = 1; r <= lastRow; r++) {
            Row row = ws.getRow(r);
            if (row == null) continue;
            for (Integer idxDia : indicesDias) {
                Cell cDia = row.getCell(idxDia);
                if (cDia != null) {
                    String valCrudo = getCellString(cDia);
                    String valReparado = repararHorarioVisual(valCrudo);
                    cDia.setCellType(CellType.STRING);
                    cDia.setCellValue(valReparado);
                    cDia.setCellStyle(estiloTexto);
                }
            }
        }
    }

    private void generarHojasDeChoques(Workbook wb, Sheet ws, Row headerRow, int maxCols, Consumer<String> logger) {
        List<String> encabezados = new ArrayList<>();
        for (int c = 0; c < maxCols; c++) encabezados.add(getCellString(headerRow.getCell(c)));
        List<String> encabezadosNorm = encabezados.stream().map(ExcelPrepararService::normalizarParaBusqueda).collect(Collectors.toList());

        int colProf = encabezadosNorm.indexOf("PROFESOR");
        int colMateria = encabezadosNorm.indexOf("MATERIA");
        int colSem = encabezadosNorm.indexOf("SEMESTRE");
        List<Integer> indicesDias = Arrays.asList("LUNES","MARTES","MIERCOLES","JUEVES","VIERNES","SABADO").stream()
                .map(encabezadosNorm::indexOf).filter(i -> i >= 0).collect(Collectors.toList());

        if (colProf < 0 || colMateria < 0 || colSem < 0 || indicesDias.isEmpty()) {
            logger.accept("alerta: faltan columnas de dias. no se generaran hojas de choques.");
            return;
        }

        int lastRow = ws.getLastRowNum();
        List<List<String>> filasDatos = new ArrayList<>();
        for (int r = 1; r <= lastRow; r++) {
            Row row = ws.getRow(r);
            if (row == null) continue;
            List<String> filaVals = new ArrayList<>();
            boolean any = false;
            for (int c = 0; c < maxCols; c++) {
                String val = getCellString(row.getCell(c));
                filaVals.add(val);
                if (!val.trim().isEmpty()) any = true;
            }
            if (any) filasDatos.add(filaVals);
        }

        Map<String, List<List<String>>> profMap = new HashMap<>();
        for (List<String> fila : filasDatos) {
            String prof = fila.get(colProf);
            if (!prof.trim().isEmpty() && !prof.equals("Sin profesor")) {
                String profNorm = normalizarParaBusqueda(prof);
                profMap.computeIfAbsent(profNorm, k -> new ArrayList<>()).add(fila);
            }
        }

        Set<List<String>> choquesProfesorSet = new HashSet<>();
        for (List<List<String>> grupo : profMap.values()) {
            for (int i = 0; i < grupo.size(); i++) {
                for (int j = i+1; j < grupo.size(); j++) {
                    if (hayChoqueRegistros(grupo.get(i), grupo.get(j), indicesDias)) {
                        choquesProfesorSet.add(grupo.get(i)); choquesProfesorSet.add(grupo.get(j));
                    }
                }
            }
        }

        List<List<String>> filasQuimica = new ArrayList<>(), filasAgro = new ArrayList<>();
        for (List<String> fila : filasDatos) {
            String pref = obtenerPrefijoCodigo(fila.get(colMateria));
            if (pref.equals("IQMD")) filasQuimica.add(fila);
            else if (pref.equals("AGRD")) filasAgro.add(fila);
            else if (!pref.equals("TITD")) { filasQuimica.add(fila); filasAgro.add(fila); }
        }

        crearHojaConFilas(wb, "Choques_Sem_Quimica", encabezados, encontrarChoquesSemestre(filasQuimica, colSem, colMateria, indicesDias));
        crearHojaConFilas(wb, "Choques_Sem_Agro", encabezados, encontrarChoquesSemestre(filasAgro, colSem, colMateria, indicesDias));
        crearHojaConFilas(wb, "Choques_Profesores", encabezados, choquesProfesorSet);
    }

    // ==========================================
    // MÉTODOS DE NORMALIZACIÓN SEGUROS
    // ==========================================
    private static String normalizarParaBusqueda(String t) {
        if (t == null) return "";
        String upper = t.trim().toUpperCase();
        upper = upper.replace("Ñ", "@@N_TILDE@@");
        String norm = Normalizer.normalize(upper, Normalizer.Form.NFKD).replaceAll("\\p{M}", "");
        return norm.replace("@@N_TILDE@@", "Ñ");
    }

    private static Set<String> obtenerPalabrasSignificativas(String nombreNormalizado) {
        Set<String> significativas = new HashSet<>();
        if (nombreNormalizado == null || nombreNormalizado.isEmpty()) return significativas;
        for (String p : nombreNormalizado.split("\\s+")) {
            if (!STOP_WORDS.contains(p)) significativas.add(p);
        }
        return significativas;
    }

    // ==========================================
    // MÉTODOS AUXILIARES: EL RESCATE DE FECHAS
    // ==========================================
    private static String repararHorarioVisual(String t) {
        if (t == null || t.trim().isEmpty()) return "";
        String s = t.trim().toLowerCase();

        Map<String, Integer> months = new HashMap<>();
        months.put("ene", 1); months.put("feb", 2); months.put("mar", 3);
        months.put("abr", 4); months.put("may", 5); months.put("jun", 6);
        months.put("jul", 7); months.put("ago", 8); months.put("sep", 9);
        months.put("sept", 9); months.put("oct", 10); months.put("nov", 11);
        months.put("dic", 12);

        StringBuilder out = new StringBuilder();
        Matcher m = Pattern.compile("([0-9a-z]+)\\s*-\\s*([0-9a-z]+)").matcher(s);
        int lastEnd = 0;

        while (m.find()) {
            out.append(s, lastEnd, m.start());

            int h1 = -1, h2 = -1;
            String p1 = m.group(1), p2 = m.group(2);

            if (p1.matches("\\d+")) h1 = Integer.parseInt(p1);
            else if (months.containsKey(p1)) h1 = months.get(p1);

            if (p2.matches("\\d+")) h2 = Integer.parseInt(p2);
            else if (months.containsKey(p2)) h2 = months.get(p2);

            if (h1 != -1 && h2 != -1) {
                int inicio = Math.min(h1, h2);
                int fin = Math.max(h1, h2);

                // Si escribieron "11-1" realmente querían decir "11-13" (11 a 1 de la tarde)
                if (inicio <= 6 && fin >= 9) {
                    out.append(fin).append("-").append(inicio + 12);
                } else {
                    out.append(inicio).append("-").append(fin);
                }
            } else {
                out.append(m.group(0)); // Lo dejamos intacto si no era una hora
            }
            lastEnd = m.end();
        }
        out.append(s.substring(lastEnd));
        return out.toString().isEmpty() ? t : out.toString();
    }

    private static String getCellString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(cell.getDateCellValue());
                    return (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.DAY_OF_MONTH);
                }
                double val = cell.getNumericCellValue();
                return (val % 1 == 0) ? String.valueOf((long)val) : String.valueOf(val);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default: return "";
        }
    }

    private static void crearHojaConFilas(Workbook wb, String nom, List<String> enc, Collection<List<String>> filas) {
        Sheet s = wb.createSheet(nom); Row h = s.createRow(0);
        for (int i = 0; i < enc.size(); i++) h.createCell(i).setCellValue(enc.get(i));
        List<List<String>> ord = filas.stream().sorted(Comparator.comparing(f -> String.join("|", f))).toList();
        int r = 1;
        for (List<String> fila : ord) {
            Row row = s.createRow(r++);
            for (int c = 0; c < fila.size(); c++) row.createCell(c).setCellValue(fila.get(c));
        }
    }

    private static void insertarColumna(Sheet sheet, int insertColIndex, int maxColsEstimate) {
        for (int r = 0; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            int lastCell = row.getLastCellNum() < 0 ? maxColsEstimate : row.getLastCellNum();
            for (int c = Math.max(lastCell, insertColIndex); c >= insertColIndex; c--) {
                Cell from = row.getCell(c); Cell to = row.getCell(c + 1);
                if (to == null) to = row.createCell(c + 1);
                if (from != null) {
                    to.setCellStyle(from.getCellStyle());
                    switch (from.getCellType()) {
                        case STRING: to.setCellValue(from.getStringCellValue()); break;
                        case NUMERIC: to.setCellValue(from.getNumericCellValue()); break;
                        case BOOLEAN: to.setCellValue(from.getBooleanCellValue()); break;
                        default: to.setBlank(); break;
                    }
                    from.setBlank();
                } else to.setBlank();
            }
        }
    }

    private static String obtenerSemestre(String mat) {
        if (mat == null || mat.trim().isEmpty()) return "";
        String m = mat.trim().toUpperCase();
        if (m.contains("TITD101")) return "8"; if (m.contains("TITD201")) return "9";
        Matcher match = Pattern.compile("\\(([^)]+)\\)\\s*$").matcher(m);
        if (match.find()) { Matcher num = Pattern.compile("\\d").matcher(match.group(1)); if (num.find()) return num.group(0); }
        return "";
    }

    private static List<int[]> obtenerHorarios(String t) {
        List<int[]> res = new ArrayList<>();
        if (t == null) return res;
        Matcher m = Pattern.compile("(\\d+)\\s*-\\s*(\\d+)").matcher(t);
        while (m.find()) { try { res.add(new int[]{Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))}); } catch (Exception e){} }
        return res;
    }

    private static boolean hayChoqueRegistros(List<String> f1, List<String> f2, List<Integer> idxDias) {
        for (Integer i : idxDias) {
            List<int[]> h1 = obtenerHorarios(i < f1.size() ? f1.get(i) : "");
            List<int[]> h2 = obtenerHorarios(i < f2.size() ? f2.get(i) : "");
            for (int[] a : h1) for (int[] b : h2) if (Math.max(a[0], b[0]) < Math.min(a[1], b[1])) return true;
        } return false;
    }

    private static String obtenerPrefijoCodigo(String mat) {
        if (mat == null) return "";
        Matcher m = Pattern.compile("\\(([A-Z]+)\\d").matcher(mat.trim().toUpperCase());
        return m.find() ? m.group(1) : "";
    }

    private static Set<List<String>> encontrarChoquesSemestre(List<List<String>> filas, int cSem, int cMat, List<Integer> idxDias) {
        Map<String, Map<String, List<List<String>>>> semDict = new HashMap<>();
        for (List<String> f : filas) {
            String sem = cSem < f.size() ? f.get(cSem) : "", mat = cMat < f.size() ? f.get(cMat) : "";
            if (sem != null && !sem.isEmpty() && mat != null && !mat.isEmpty()) {
                // AGRUPAR MATERIAS USANDO SU NOMBRE NORMALIZADO PARA EVITAR TILDES (ej. QUÍMICA = QUIMICA)
                String matNorm = normalizarParaBusqueda(mat);
                semDict.computeIfAbsent(sem, k -> new HashMap<>())
                        .computeIfAbsent(matNorm, k -> new ArrayList<>()).add(f);
            }
        }

        Set<List<String>> choques = new HashSet<>();
        for (Map<String, List<List<String>>> materias : semDict.values()) {
            List<String> matList = new ArrayList<>(materias.keySet());
            for (int i = 0; i < matList.size(); i++) {
                for (int j = i+1; j < matList.size(); j++) {
                    List<List<String>> fA = materias.get(matList.get(i)), fB = materias.get(matList.get(j));
                    boolean chq = true;
                    for (List<String> a : fA) {
                        for (List<String> b : fB) if (!hayChoqueRegistros(a, b, idxDias)) { chq = false; break; }
                        if (!chq) break;
                    }
                    if (chq) { choques.addAll(fA); choques.addAll(fB); }
                }
            }
        } return choques;
    }
}


