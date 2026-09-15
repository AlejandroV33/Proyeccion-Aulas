package app.controller.gestion;

import app.model.dao.HorarioDAO;
import app.model.entity.AulaOcupacion;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Sub-controlador para la pestaña "Visor de Aulas (Ocupación)".
 * Muestra la ocupación semanal de las aulas con filtros por edificio,
 * ordenamiento configurable, y exportación a Excel.
 */
public class VisorAulasController {

    private ComboBox<String> comboFiltroEdificio, comboOrdenOcupacion;
    private TableView<AulaOcupacion> tablaVisorAulas;
    private TableColumn<AulaOcupacion, String> colVisAula, colVisLun, colVisMar, colVisMie, colVisJue, colVisVie;
    private CheckBox chkSoloOcupadas;
    private TabPane mainTabPane;

    private final HorarioDAO horarioDAO;
    private final ObservableList<AulaOcupacion> masterOcupacion = FXCollections.observableArrayList();

    public VisorAulasController(HorarioDAO horarioDAO) {
        this.horarioDAO = horarioDAO;
    }

    public void inicializar(ComboBox<String> comboFiltroEdificio,
                            ComboBox<String> comboOrdenOcupacion,
                            TableView<AulaOcupacion> tablaVisorAulas,
                            TableColumn<AulaOcupacion, String> colVisAula,
                            TableColumn<AulaOcupacion, String> colVisLun,
                            TableColumn<AulaOcupacion, String> colVisMar,
                            TableColumn<AulaOcupacion, String> colVisMie,
                            TableColumn<AulaOcupacion, String> colVisJue,
                            TableColumn<AulaOcupacion, String> colVisVie,
                            CheckBox chkSoloOcupadas,
                            TabPane mainTabPane) {
        this.comboFiltroEdificio = comboFiltroEdificio;
        this.comboOrdenOcupacion = comboOrdenOcupacion;
        this.tablaVisorAulas = tablaVisorAulas;
        this.colVisAula = colVisAula;
        this.colVisLun = colVisLun;
        this.colVisMar = colVisMar;
        this.colVisMie = colVisMie;
        this.colVisJue = colVisJue;
        this.colVisVie = colVisVie;
        this.chkSoloOcupadas = chkSoloOcupadas;
        this.mainTabPane = mainTabPane;

        configurarVisor();
    }

    // ==========================================
    // CONFIGURACIÓN
    // ==========================================

    private void configurarVisor() {
        // Llenar filtros
        comboFiltroEdificio.setItems(FXCollections.observableArrayList("Todos", "E17", "E18", "E19", "E22"));
        comboFiltroEdificio.getSelectionModel().select("E17");

        comboOrdenOcupacion.setItems(FXCollections.observableArrayList(
                "Ordenar por Piso (Por Defecto)",
                "Más Ocupadas (Total de la Semana)",
                "Más Ocupadas (Lunes)",
                "Más Ocupadas (Martes)",
                "Más Ocupadas (Miércoles)",
                "Más Ocupadas (Jueves)",
                "Más Ocupadas (Viernes)"
        ));
        comboOrdenOcupacion.getSelectionModel().selectFirst();

        // Configurar columnas
        colVisAula.setCellValueFactory(new PropertyValueFactory<>("aulaDesc"));
        colVisLun.setCellValueFactory(new PropertyValueFactory<>("lunesText"));
        colVisMar.setCellValueFactory(new PropertyValueFactory<>("martesText"));
        colVisMie.setCellValueFactory(new PropertyValueFactory<>("miercolesText"));
        colVisJue.setCellValueFactory(new PropertyValueFactory<>("juevesText"));
        colVisVie.setCellValueFactory(new PropertyValueFactory<>("viernesText"));

        // Pintar celda de aula según tipo (laboratorio vs común)
        colVisAula.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-alignment: CENTER; -fx-font-weight: bold; -fx-background-color: #ecf0f1;");
                } else {
                    setText(item);
                    AulaOcupacion aula = getTableView().getItems().get(getIndex());

                    if (aula.getTipoAula() != null && !aula.getTipoAula().toUpperCase().contains("COMUN")) {
                        setStyle("-fx-alignment: CENTER; -fx-font-weight: bold; " +
                                "-fx-background-color: rgba(250,239,202,0.87); " +
                                "-fx-border-color: #fae6c2; -fx-border-width: 0 3px 0 0;");
                    } else {
                        setStyle("-fx-alignment: CENTER; -fx-font-weight: bold; -fx-background-color: #ecf0f1;");
                    }
                }
            }
        });

        tablaVisorAulas.setFixedCellSize(Region.USE_COMPUTED_SIZE);

        // Configurar multilínea (text wrapping) para los horarios
        configurarColumnaMultilinea(colVisLun);
        configurarColumnaMultilinea(colVisMar);
        configurarColumnaMultilinea(colVisMie);
        configurarColumnaMultilinea(colVisJue);
        configurarColumnaMultilinea(colVisVie);

        // Listeners para actualizar al cambiar filtros
        comboFiltroEdificio.setOnAction(e -> aplicarFiltrosYOrden());
        comboOrdenOcupacion.setOnAction(e -> aplicarFiltrosYOrden());
        chkSoloOcupadas.setOnAction(e -> aplicarFiltrosYOrden());

        cargarDatos();
    }

    private void configurarColumnaMultilinea(TableColumn<AulaOcupacion, String> col) {
        col.setCellFactory(tc -> new TableCell<>() {
            private final Text text = new Text();
            private final VBox vbox = new VBox(text);

            {
                text.setStyle("-fx-font-size: 11px;");
                text.wrappingWidthProperty().bind(col.widthProperty().subtract(15));
                vbox.setPadding(new Insets(8, 5, 8, 5));
                vbox.setAlignment(Pos.TOP_LEFT);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.trim().isEmpty()) {
                    setGraphic(null);
                } else {
                    text.setText(item);
                    setGraphic(vbox);
                }
            }
        });
    }

    // ==========================================
    // CARGA Y FILTRADO
    // ==========================================

    public void cargarDatos() {
        masterOcupacion.setAll(horarioDAO.listarOcupacionAulas());
        aplicarFiltrosYOrden();
    }

    private void aplicarFiltrosYOrden() {
        String edif = comboFiltroEdificio.getValue();
        String orden = comboOrdenOcupacion.getValue();
        if (edif == null || orden == null) return;

        boolean soloOcupadas = chkSoloOcupadas.isSelected();

        // 1. Filtrar por edificio y ocupación
        List<AulaOcupacion> filtradas = masterOcupacion.stream()
                .filter(a -> edif.equals("Todos") || a.getEdificio().equalsIgnoreCase(edif))
                .filter(a -> !soloOcupadas || a.getHorasTotal() > 0)
                .collect(Collectors.toList());

        // 2. Aplicar ordenamiento
        filtradas.sort((a, b) -> {
            if (orden.contains("Piso")) {
                int p1 = PisoUtils.obtenerValorPiso(a.getPiso());
                int p2 = PisoUtils.obtenerValorPiso(b.getPiso());
                if (p1 == p2) return a.getNumero().compareTo(b.getNumero());
                return Integer.compare(p1, p2);
            } else if (orden.contains("Total")) {
                return Integer.compare(b.getHorasTotal(), a.getHorasTotal());
            } else if (orden.contains("Lunes")) {
                return Integer.compare(b.getHorasLunes(), a.getHorasLunes());
            } else if (orden.contains("Martes")) {
                return Integer.compare(b.getHorasMartes(), a.getHorasMartes());
            } else if (orden.contains("Miércoles")) {
                return Integer.compare(b.getHorasMiercoles(), a.getHorasMiercoles());
            } else if (orden.contains("Jueves")) {
                return Integer.compare(b.getHorasJueves(), a.getHorasJueves());
            } else if (orden.contains("Viernes")) {
                return Integer.compare(b.getHorasViernes(), a.getHorasViernes());
            }
            return 0;
        });

        tablaVisorAulas.setItems(FXCollections.observableArrayList(filtradas));
    }

    // ==========================================
    // EXPORTAR EXCEL
    // ==========================================

    public void exportarExcelVisor() {
        if (tablaVisorAulas.getItems().isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "No hay datos visibles en la tabla para exportar.").show();
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Se exportará un Excel estructurado con los registros actualmente visibles " +
                        "y los filtros aplicados en la tabla.\n¿Desea continuar?",
                ButtonType.OK, ButtonType.CANCEL);
        alert.setHeaderText("Exportar Reporte de Aulas");

        alert.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Guardar Reporte de Aulas");
                fileChooser.setInitialFileName("Ocupacion_Aulas_Filtrado.xlsx");
                fileChooser.getExtensionFilters().add(
                        new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));

                File file = fileChooser.showSaveDialog(mainTabPane.getScene().getWindow());

                if (file != null) {
                    try {
                        new app.model.service.ExcelExportarAulasService()
                                .exportarVisor(tablaVisorAulas.getItems(), file);
                        new Alert(Alert.AlertType.INFORMATION,
                                "¡Excel exportado exitosamente en:\n" + file.getAbsolutePath()).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                        new Alert(Alert.AlertType.ERROR,
                                "Error al crear el archivo Excel: " + e.getMessage()).show();
                    }
                }
            }
        });
    }
}
