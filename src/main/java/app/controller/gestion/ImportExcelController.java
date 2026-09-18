package app.controller.gestion;

import app.model.service.ExcelExtractorService;
import app.model.service.ExcelPrepararService;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/**
 * Sub-controlador para la pestaña "Importar Excel".
 * Gestiona la descarga de plantillas, preparación de archivos Excel,
 * y la extracción/inyección de datos a la base de datos con backup.
 */
public class ImportExcelController {

    private TextArea txtConsolaExcel;
    private ProgressIndicator progressExcel;
    private TabPane mainTabPane;

    private final ExcelPrepararService excelPrepararService;
    private final ExcelExtractorService extractorService;
    private Runnable onDataImported;

    public ImportExcelController(ExcelPrepararService excelPrepararService,
                                 ExcelExtractorService extractorService) {
        this.excelPrepararService = excelPrepararService;
        this.extractorService = extractorService;
    }

    public void inicializar(TextArea txtConsolaExcel,
                            ProgressIndicator progressExcel,
                            TabPane mainTabPane,
                            Runnable onDataImported) {
        this.txtConsolaExcel = txtConsolaExcel;
        this.progressExcel = progressExcel;
        this.mainTabPane = mainTabPane;
        this.onDataImported = onDataImported;
    }

    private void logExcel(String mensaje) {
        Platform.runLater(() -> txtConsolaExcel.appendText(mensaje + "\n"));
    }

    // ==========================================
    // DESCARGAR EJEMPLO
    // ==========================================

    public void descargarEjemploExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Excel de Ejemplo");
        fileChooser.setInitialFileName("Ejemplo_FIQA.xlsx");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));

        File dest = fileChooser.showSaveDialog(mainTabPane.getScene().getWindow());
        if (dest != null) {
            try (InputStream in = getClass().getResourceAsStream("/ejemploExcel/ejemplo.xlsx")) {
                if (in == null) {
                    app.util.AlertUtil.mostrarError("No se encontro 'ejemplo.xlsx' en la carpeta resources del proyecto.");
                    return;
                }
                Files.copy(in, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                app.util.AlertUtil.mostrarInfo("Excel de ejemplo guardado con exito.");
            } catch (IOException e) {
                app.util.AlertUtil.mostrarError("Error al guardar: " + e.getMessage());
            }
        }
    }

    // ==========================================
    // PREPARAR EXCEL CRUDO
    // ==========================================

    public void prepararExcelCrudo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar Excel Crudo");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        File inputFile = fileChooser.showOpenDialog(mainTabPane.getScene().getWindow());

        if (inputFile != null) {
            txtConsolaExcel.clear();
            progressExcel.setVisible(true);

            javafx.concurrent.Task<org.apache.poi.ss.usermodel.Workbook> tarea = new javafx.concurrent.Task<>() {
                @Override
                protected org.apache.poi.ss.usermodel.Workbook call() throws Exception {
                    return excelPrepararService.preparaExcelMemoria(inputFile, mensaje -> logExcel(mensaje));
                }
            };

            tarea.setOnSucceeded(e -> {
                progressExcel.setVisible(false);
                org.apache.poi.ss.usermodel.Workbook wbProcesado = tarea.getValue();

                boolean confirmar = app.util.AlertUtil.pedirConfirmacion("Analisis Completado",
                        "El archivo se proceso correctamente y se detectaron choques.\n" +
                        "¿Desea exportar el archivo con las nuevas hojas de analisis de choques?");

                if (confirmar) {
                    exportarExcelProcesado(wbProcesado, inputFile);
                }
            });

            tarea.setOnFailed(e -> {
                progressExcel.setVisible(false);
                logExcel(">> ERROR FATAL: " + tarea.getException().getMessage());
            });

            new Thread(tarea).start();
        }
    }

    private void exportarExcelProcesado(org.apache.poi.ss.usermodel.Workbook wbProcesado, File inputFile) {
        FileChooser saveChooser = new FileChooser();
        saveChooser.setTitle("Guardar Excel Procesado");
        saveChooser.setInitialFileName(inputFile.getName().replace(".xlsx", "_procesado.xlsx"));
        saveChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        File outFile = saveChooser.showSaveDialog(mainTabPane.getScene().getWindow());

        if (outFile != null) {
            try (FileOutputStream out = new FileOutputStream(outFile)) {
                wbProcesado.write(out);
                logExcel(">> Archivo guardado en: " + outFile.getAbsolutePath());
                app.util.AlertUtil.mostrarInfo("Archivo exportado con exito.");
            } catch (Exception ex) {
                logExcel(">> Error al guardar: " + ex.getMessage());
            }
        }
    }

    // ==========================================
    // EXTRAER E INYECTAR DATOS
    // ==========================================

    public void extraerEInyectarDatos() {
        // 1. Alerta de advertencia critica (mantendremos la manual debido a los 3 botones)
        Alert advertencia = new Alert(Alert.AlertType.WARNING,
                "ATENCION: Esta accion borrara todos los horarios, paralelos y docentes actuales de la base de datos " +
                        "para reemplazarlos por los datos del archivo Excel.\n\n" +
                        "¿Desea hacer un respaldo de su base de datos actual antes de continuar?",
                ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
        advertencia.setTitle("Precaucion: Reescritura de Base de Datos");

        Optional<ButtonType> res = advertencia.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.CANCEL) return;

        // 2. Backup si el usuario acepta
        if (res.isPresent() && res.get() == ButtonType.YES) {
            if (!realizarBackup()) return;
        }

        // 3. Seleccionar Excel e inyectar
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccione el Excel Procesado (*.xlsx)");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.csv"));
        File inputFile = fileChooser.showOpenDialog(mainTabPane.getScene().getWindow());

        if (inputFile != null) {
            ejecutarInyeccion(inputFile);
        }
    }

    private boolean realizarBackup() {
        FileChooser saveChooser = new FileChooser();
        saveChooser.setTitle("Guardar Respaldo de Base de Datos");
        saveChooser.setInitialFileName("Respaldo_FIQA.db");
        saveChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Database Files", "*.db"));
        File backupFile = saveChooser.showSaveDialog(mainTabPane.getScene().getWindow());

        if (backupFile != null) {
            try {
                File dbActual = new File("proyeccion_facultad.db");
                Files.copy(dbActual.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                logExcel(">> Respaldo guardado exitosamente en: " + backupFile.getAbsolutePath());
                return true;
            } catch (Exception e) {
                app.util.AlertUtil.mostrarError("Error al crear respaldo: " + e.getMessage());
                return false;
            }
        }
        return false; // Cancelo el guardado
    }

    private void ejecutarInyeccion(File inputFile) {
        progressExcel.setVisible(true);
        txtConsolaExcel.clear();

        javafx.concurrent.Task<Void> tarea = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() throws Exception {
                extractorService.extraerEInyectar(inputFile, msg -> logExcel(msg));
                return null;
            }
        };

        tarea.setOnSucceeded(e -> {
            progressExcel.setVisible(false);
            app.util.AlertUtil.mostrarInfo("Base de datos actualizada correctamente. \nVaya a las pestañas para ver los nuevos datos.");
            onDataImported.run();
        });

        tarea.setOnFailed(e -> {
            progressExcel.setVisible(false);
            logExcel(">> ERROR FATAL EN EXTRACCIÓN: " + tarea.getException().getMessage());
            tarea.getException().printStackTrace();
        });

        new Thread(tarea).start();
    }
}
