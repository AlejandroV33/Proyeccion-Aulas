package app.controller;

import app.model.dao.VistaResultadoDAO;
import app.model.entity.ResultadoFinal;
import app.model.service.ExcelService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Controlador de la vista principal (solo lectura).
 * Muestra los resultados de la proyección de aulas con filtros de búsqueda
 * y permite navegar a la vista de Gestión o abrir el módulo de Asignación.
 */
public class MainController {

    @FXML private TableView<ResultadoFinal> tablaResultados;
    @FXML private TableColumn<ResultadoFinal, String> colMateria;
    @FXML private TableColumn<ResultadoFinal, Integer> colSemestre;
    @FXML private TableColumn<ResultadoFinal, String> colParalelo;
    @FXML private TableColumn<ResultadoFinal, Integer> colCupos;
    @FXML private TableColumn<ResultadoFinal, String> colDocente;
    @FXML private TableColumn<ResultadoFinal, String> colAula;
    @FXML private TableColumn<ResultadoFinal, String> colTipoAula;
    @FXML private TableColumn<ResultadoFinal, String> colLunes;
    @FXML private TableColumn<ResultadoFinal, String> colMartes;
    @FXML private TableColumn<ResultadoFinal, String> colMiercoles;
    @FXML private TableColumn<ResultadoFinal, String> colJueves;
    @FXML private TableColumn<ResultadoFinal, String> colViernes;

    @FXML private TextField txtBuscarMateria;
    @FXML private TextField txtBuscarDocente;
    @FXML private Label lblEstado;

    private final VistaResultadoDAO vistaDAO = new VistaResultadoDAO();
    private final ExcelService excelService = new ExcelService();

    private final ObservableList<ResultadoFinal> masterData = FXCollections.observableArrayList();
    private FilteredList<ResultadoFinal> filteredData;

    @FXML
    public void initialize() {
        configurarColumnas();
        cargarDatos();
        configurarFiltros();
    }

    private void configurarColumnas() {
        // mapeo simple de solo lectura
        colMateria.setCellValueFactory(new PropertyValueFactory<>("materia"));
        colSemestre.setCellValueFactory(new PropertyValueFactory<>("semestre"));
        colParalelo.setCellValueFactory(new PropertyValueFactory<>("paralelo"));
        colCupos.setCellValueFactory(new PropertyValueFactory<>("numEstudiantes"));
        colDocente.setCellValueFactory(new PropertyValueFactory<>("profesor"));
        colAula.setCellValueFactory(new PropertyValueFactory<>("aulaNumero"));
        colTipoAula.setCellValueFactory(new PropertyValueFactory<>("tipoAula"));
        colLunes.setCellValueFactory(new PropertyValueFactory<>("lunes"));
        colMartes.setCellValueFactory(new PropertyValueFactory<>("martes"));
        colMiercoles.setCellValueFactory(new PropertyValueFactory<>("miercoles"));
        colJueves.setCellValueFactory(new PropertyValueFactory<>("jueves"));
        colViernes.setCellValueFactory(new PropertyValueFactory<>("viernes"));
    }

    @FXML
    public void cargarDatos() {
        masterData.setAll(vistaDAO.listarResultados());
        lblEstado.setText("registros cargados: " + masterData.size());
    }

    private void configurarFiltros() {
        filteredData = new FilteredList<>(masterData, p -> true);

        // ambos listeners invocan el mismo método de actualización
        txtBuscarMateria.textProperty().addListener((obs, old, val) -> actualizarPredicadoFiltro());
        txtBuscarDocente.textProperty().addListener((obs, old, val) -> actualizarPredicadoFiltro());

        // conectar lista filtrada con la tabla y permitir ordenamiento por columnas
        SortedList<ResultadoFinal> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tablaResultados.comparatorProperty());
        tablaResultados.setItems(sortedData);
    }

    private void actualizarPredicadoFiltro() {
        String filtroMateria = txtBuscarMateria.getText().toLowerCase();
        String filtroDocente = txtBuscarDocente.getText().toLowerCase();

        filteredData.setPredicate(resultado -> {
            boolean coincideMateria = filtroMateria.isEmpty()
                    || (resultado.getMateria() != null && resultado.getMateria().toLowerCase().contains(filtroMateria));
            boolean coincideDocente = filtroDocente.isEmpty()
                    || (resultado.getProfesor() != null && resultado.getProfesor().toLowerCase().contains(filtroDocente));
            return coincideMateria && coincideDocente;
        });

        lblEstado.setText("mostrando " + filteredData.size() + " registros.");
    }

    @FXML
    public void exportarExcel() {
        String path = System.getProperty("user.home") + "/Desktop/Reporte_Aulas_FIQA.xlsx";
        try {
            excelService.generarReporte(path, masterData);
            lblEstado.setText("excel exportado a: " + path);
            app.util.AlertUtil.mostrarInfo("reporte generado exitosamente en el escritorio.");
        } catch (Exception e) {
            app.util.AlertUtil.mostrarError("error al exportar excel: " + e.getMessage());
        }
    }

    @FXML
    public void irGestion() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/GestionView.fxml"));
            Parent nuevaVista = loader.load();
            tablaResultados.getScene().setRoot(nuevaVista);
        } catch (IOException e) {
            app.util.AlertUtil.mostrarError("error al cambiar de vista: " + e.getMessage());
        }
    }

    @FXML
    public void abrirAsignacion() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/AsignacionView.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/styles.css").toExternalForm());

            Stage stage = new Stage();
            stage.setTitle("Asignación de Aulas");
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);

            // recargar datos en la tabla principal cuando se cierre el modal
            stage.setOnHidden(e -> cargarDatos());

            stage.showAndWait();
        } catch (IOException e) {
            app.util.AlertUtil.mostrarError("error al abrir ventana de asignación: " + e.getMessage());
        }
    }
}