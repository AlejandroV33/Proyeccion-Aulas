package app.controller.gestion;

import app.model.dao.DocenteDAO;
import app.model.dao.MateriaDAO;
import app.model.dao.ParaleloDAO;
import app.model.entity.Docente;
import app.model.entity.Materia;
import app.model.entity.ParaleloFila;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

/**
 * Sub-controlador para la pestaña "Paralelos".
 * Gestiona la tabla con filtros múltiples (materia, docente, nombre)
 * y modales de creación/edición con navegación a otras pestañas.
 */
public class ParaleloTabController {

    private TextField txtBuscarParaleloMat, txtBuscarParaleloDoc, txtBuscarParaleloNom;
    private TableView<ParaleloFila> tablaParalelos;
    private TableColumn<ParaleloFila, String> colParaleloMat, colParaleloDoc, colParaleloNom;
    private TableColumn<ParaleloFila, Integer> colParaleloEst;

    private TabPane tabPaneEdicion;
    private Tab tabMaterias;

    private final ParaleloDAO paraleloDAO;
    private final MateriaDAO materiaDAO;
    private final DocenteDAO docenteDAO;

    private final ObservableList<ParaleloFila> masterParalelos = FXCollections.observableArrayList();
    private FilteredList<ParaleloFila> filteredParalelos;
    private Runnable onDataChanged;

    public ParaleloTabController(ParaleloDAO paraleloDAO, MateriaDAO materiaDAO, DocenteDAO docenteDAO) {
        this.paraleloDAO = paraleloDAO;
        this.materiaDAO = materiaDAO;
        this.docenteDAO = docenteDAO;
    }

    public void inicializar(TextField txtBuscarParaleloMat,
                            TextField txtBuscarParaleloDoc,
                            TextField txtBuscarParaleloNom,
                            TableView<ParaleloFila> tablaParalelos,
                            TableColumn<ParaleloFila, String> colParaleloMat,
                            TableColumn<ParaleloFila, String> colParaleloDoc,
                            TableColumn<ParaleloFila, String> colParaleloNom,
                            TableColumn<ParaleloFila, Integer> colParaleloEst,
                            TabPane tabPaneEdicion,
                            Tab tabMaterias,
                            Runnable onDataChanged) {
        this.txtBuscarParaleloMat = txtBuscarParaleloMat;
        this.txtBuscarParaleloDoc = txtBuscarParaleloDoc;
        this.txtBuscarParaleloNom = txtBuscarParaleloNom;
        this.tablaParalelos = tablaParalelos;
        this.colParaleloMat = colParaleloMat;
        this.colParaleloDoc = colParaleloDoc;
        this.colParaleloNom = colParaleloNom;
        this.colParaleloEst = colParaleloEst;
        this.tabPaneEdicion = tabPaneEdicion;
        this.tabMaterias = tabMaterias;
        this.onDataChanged = onDataChanged;

        configurarTabla();
    }

    private void configurarTabla() {
        colParaleloMat.setCellValueFactory(new PropertyValueFactory<>("materia"));
        colParaleloDoc.setCellValueFactory(new PropertyValueFactory<>("docente"));
        colParaleloNom.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colParaleloEst.setCellValueFactory(new PropertyValueFactory<>("numEstudiantes"));

        filteredParalelos = new FilteredList<>(masterParalelos, p -> true);
        txtBuscarParaleloMat.textProperty().addListener((o, old, n) -> actualizarFiltro());
        txtBuscarParaleloDoc.textProperty().addListener((o, old, n) -> actualizarFiltro());
        txtBuscarParaleloNom.textProperty().addListener((o, old, n) -> actualizarFiltro());
        tablaParalelos.setItems(new SortedList<>(filteredParalelos));

        // Doble clic para editar paralelo
        tablaParalelos.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && tablaParalelos.getSelectionModel().getSelectedItem() != null) {
                abrirEditor(tablaParalelos.getSelectionModel().getSelectedItem());
            }
        });

        cargarDatos();
    }

    private void actualizarFiltro() {
        String fMat = txtBuscarParaleloMat.getText().toLowerCase();
        String fDoc = txtBuscarParaleloDoc.getText().toLowerCase();
        String fNom = txtBuscarParaleloNom.getText().toLowerCase();

        filteredParalelos.setPredicate(p -> {
            boolean m = p.getMateria() != null && p.getMateria().toLowerCase().contains(fMat);
            boolean d = p.getDocente() != null && p.getDocente().toLowerCase().contains(fDoc);
            boolean n = p.getNombre() != null && p.getNombre().toLowerCase().contains(fNom);
            return m && d && n;
        });
    }

    public void cargarDatos() {
        masterParalelos.setAll(paraleloDAO.listarTabla());
    }

    public void abrirModalNuevo() {
        abrirEditor(new ParaleloFila());
    }

    private void abrirEditor(ParaleloFila p) {
        boolean esNuevo = p.getId() == 0;
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle(esNuevo ? "Registrar Nuevo Paralelo" : "Editar Paralelo");
        dialog.setHeaderText("Gestión de Paralelo y Asignación de Docente");

        // Campos
        TextField txtNombre = new TextField(p.getNombre());
        txtNombre.setPromptText("Ej. GR1");
        Spinner<Integer> spinEst = new Spinner<>(1, 150, esNuevo ? 30 : p.getNumEstudiantes(), 1);

        // ComboBoxes de materia y docente
        ComboBox<Materia> comboMat = new ComboBox<>(FXCollections.observableArrayList(materiaDAO.listar()));
        ComboBox<Docente> comboDoc = new ComboBox<>(FXCollections.observableArrayList(docenteDAO.listar()));
        comboMat.setPrefWidth(200);
        comboDoc.setPrefWidth(200);

        // Preseleccionar valores existentes
        if (!esNuevo) {
            comboMat.getItems().stream().filter(m -> m.getId() == p.getIdMateria()).findFirst()
                    .ifPresent(comboMat.getSelectionModel()::select);
            if (p.getIdDocente() != null) {
                comboDoc.getItems().stream().filter(d -> d.getId() == p.getIdDocente()).findFirst()
                        .ifPresent(comboDoc.getSelectionModel()::select);
            }
        }

        // Botones de salto rápido [+] para crear nueva materia/docente
        Button btnNuevaMat = new Button("+");
        btnNuevaMat.setOnAction(e -> {
            dialog.close();
            tabPaneEdicion.getSelectionModel().select(tabMaterias);
        });

        Button btnNuevoDoc = new Button("+");
        btnNuevoDoc.setOnAction(e -> {
            dialog.close();
            tabPaneEdicion.getSelectionModel().select(tabMaterias);
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Materia:"), 0, 0);           grid.add(new HBox(5, comboMat, btnNuevaMat), 1, 0);
        grid.add(new Label("Docente:"), 0, 1);            grid.add(new HBox(5, comboDoc, btnNuevoDoc), 1, 1);
        grid.add(new Label("Paralelo (Nombre):"), 0, 2);  grid.add(txtNombre, 1, 2);
        grid.add(new Label("Est. Matriculados:"), 0, 3);  grid.add(spinEst, 1, 3);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK && comboMat.getValue() != null && !txtNombre.getText().trim().isEmpty()) {
                p.setNombre(txtNombre.getText().trim().toUpperCase());
                p.setNumEstudiantes(spinEst.getValue());
                p.setIdMateria(comboMat.getValue().getId());
                p.setIdDocente(comboDoc.getValue() != null ? comboDoc.getValue().getId() : null);
                paraleloDAO.guardar(p);
                return true;
            }
            return false;
        });

        dialog.showAndWait().ifPresent(guardado -> {
            if (guardado) {
                cargarDatos();
                onDataChanged.run();
            }
        });
    }

    public void eliminar() {
        ParaleloFila selec = tablaParalelos.getSelectionModel().getSelectedItem();
        if (selec == null) {
            new Alert(Alert.AlertType.WARNING, "Seleccione un paralelo de la tabla.").show();
            return;
        }

        // Advertencia de eliminación en cascada
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "ADVERTENCIA CRÍTICA \n\nEstá a punto de eliminar el paralelo '" + selec.getNombre() +
                        "' de la materia '" + selec.getMateria() + "'.\n\n" +
                        "Esto BORRARÁ EN CASCADA todos los horarios de clases asignados a este paralelo en la base de datos.\n" +
                        "¿Está completamente seguro?",
                ButtonType.YES, ButtonType.NO);

        alert.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                paraleloDAO.eliminar(selec.getId());
                cargarDatos();
                onDataChanged.run();
            }
        });
    }
}
