package app.controller;

import app.model.dao.HorarioDAO;
import app.model.dao.VistaResultadoDAO;
import app.model.service.ExcelService;
import app.model.service.SimulatedAnnealingService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;

/**
 * Controlador del módulo de Asignación de Aulas.
 * Ejecuta el algoritmo de Simulated Annealing en un hilo secundario
 * y permite exportar los resultados a Excel.
 */
public class AsignacionController {

    @FXML private TextArea txtConsola;
    @FXML private Button btnAsignar;
    @FXML private ProgressIndicator progressIndicator;

    private final HorarioDAO horarioDAO = new HorarioDAO();
    private final SimulatedAnnealingService algoritmo = new SimulatedAnnealingService();
    private final ExcelService excelService = new ExcelService();
    private final VistaResultadoDAO vistaDAO = new VistaResultadoDAO();

    @FXML
    public void initialize() {
        escribirConsola("sistema listo. presione 'ejecutar algoritmo' para comenzar.");
    }

    @FXML
    public void limpiarAulas() {
        horarioDAO.limpiarAsignaciones();
        escribirConsola("todas las aulas han sido liberadas (estado null).");
    }

    @FXML
    public void ejecutarAlgoritmo() {
        btnAsignar.setDisable(true);
        progressIndicator.setVisible(true);
        txtConsola.clear();

        Task<Void> tareaAsignacion = new Task<>() {
            @Override
            protected Void call() {
                algoritmo.ejecutarMejorDeTres(mensaje ->
                        Platform.runLater(() -> escribirConsola(mensaje)));
                return null;
            }
        };

        tareaAsignacion.setOnSucceeded(e -> {
            restaurarUI();
            escribirConsola("--- proceso completado. ya puede exportar o cerrar ---");
        });

        tareaAsignacion.setOnFailed(e -> {
            restaurarUI();
            escribirConsola("error critico durante la ejecucion: " + tareaAsignacion.getException().getMessage());
        });

        // Hilo daemon para que no bloquee el cierre de la aplicación
        Thread hilo = new Thread(tareaAsignacion);
        hilo.setDaemon(true);
        hilo.start();
    }

    @FXML
    public void exportarExcel() {
        String path = System.getProperty("user.home") + "/Desktop/Reporte_Aulas_FIQA.xlsx";
        try {
            excelService.generarReporte(path, vistaDAO.listarResultados());
            escribirConsola("excel exportado correctamente a: " + path);

            app.util.AlertUtil.mostrarInfo("reporte generado exitosamente en el escritorio.");
        } catch (Exception e) {
            escribirConsola("error al exportar excel: " + e.getMessage());
        }
    }

    /** Restaura el estado de la UI tras completar o fallar el algoritmo. */
    private void restaurarUI() {
        btnAsignar.setDisable(false);
        progressIndicator.setVisible(false);
    }

    private void escribirConsola(String texto) {
        txtConsola.appendText(texto + "\n");
    }
}