package app.controller.gestion;

import app.model.dao.MateriaDAO;
import app.model.dao.TipoAulaDAO;
import app.model.entity.Materia;
import app.model.entity.TipoAula;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

/**
 * Sub-controlador para la pestaña "Materias".
 * Gestiona la tabla con filtro de búsqueda y modales de creación/edición,
 * incluyendo la selección de tipo de aula requerida.
 */
public class MateriaTabController {

    private TextField txtBuscarMateriaNom;
    private TableView<Materia> tablaMaterias;
    private TableColumn<Materia, String> colMatCodigo, colMatNombre, colMatDepto, colMatTipo;
    private TableColumn<Materia, Integer> colMatSemestre, colMatCreditos, colMatHoras;

    private final MateriaDAO materiaDAO;
    private final TipoAulaDAO tipoAulaDAO;
    private final ObservableList<Materia> masterMaterias = FXCollections.observableArrayList();
    private FilteredList<Materia> filteredMaterias;
    private Runnable onDataChanged;

    public MateriaTabController(MateriaDAO materiaDAO, TipoAulaDAO tipoAulaDAO) {
        this.materiaDAO = materiaDAO;
        this.tipoAulaDAO = tipoAulaDAO;
    }

    public void inicializar(TextField txtBuscarMateriaNom,
                            TableView<Materia> tablaMaterias,
                            TableColumn<Materia, String> colMatCodigo,
                            TableColumn<Materia, String> colMatNombre,
                            TableColumn<Materia, String> colMatDepto,
                            TableColumn<Materia, String> colMatTipo,
                            TableColumn<Materia, Integer> colMatSemestre,
                            TableColumn<Materia, Integer> colMatCreditos,
                            TableColumn<Materia, Integer> colMatHoras,
                            Runnable onDataChanged) {
        this.txtBuscarMateriaNom = txtBuscarMateriaNom;
        this.tablaMaterias = tablaMaterias;
        this.colMatCodigo = colMatCodigo;
        this.colMatNombre = colMatNombre;
        this.colMatDepto = colMatDepto;
        this.colMatTipo = colMatTipo;
        this.colMatSemestre = colMatSemestre;
        this.colMatCreditos = colMatCreditos;
        this.colMatHoras = colMatHoras;
        this.onDataChanged = onDataChanged;

        configurarTabla();
    }

    private void configurarTabla() {
        colMatCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colMatNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colMatDepto.setCellValueFactory(new PropertyValueFactory<>("departamento"));
        colMatTipo.setCellValueFactory(new PropertyValueFactory<>("tipoAulaReq"));
        colMatSemestre.setCellValueFactory(new PropertyValueFactory<>("semestre"));
        colMatCreditos.setCellValueFactory(new PropertyValueFactory<>("creditos"));
        colMatHoras.setCellValueFactory(new PropertyValueFactory<>("horas"));

        filteredMaterias = new FilteredList<>(masterMaterias, p -> true);
        txtBuscarMateriaNom.textProperty().addListener((o, old, n) ->
                filteredMaterias.setPredicate(m ->
                        n.isEmpty() || m.getNombre().toLowerCase().contains(n.toLowerCase())));
        tablaMaterias.setItems(new SortedList<>(filteredMaterias));

        tablaMaterias.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && tablaMaterias.getSelectionModel().getSelectedItem() != null) {
                abrirEditor(tablaMaterias.getSelectionModel().getSelectedItem());
            }
        });

        cargarDatos();
    }

    public void cargarDatos() {
        masterMaterias.setAll(materiaDAO.listarTabla());
    }

    public void abrirModalNueva() {
        abrirEditor(new Materia());
    }

    private void abrirEditor(Materia m) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle(m.getId() == 0 ? "Nueva Materia" : "Editar Materia");

        TextField txtCodigo = new TextField(m.getCodigo() != null ? m.getCodigo() : "");
        TextField txtNombre = new TextField(m.getNombre() != null ? m.getNombre() : "");
        TextField txtDepto = new TextField(m.getDepartamento() != null ? m.getDepartamento() : "");

        // Spinners numéricos editables manualmente
        Spinner<Integer> spinCreditos = new Spinner<>(0, 100, m.getId() == 0 ? 0 : m.getCreditos(), 1);
        spinCreditos.setEditable(true);
        Spinner<Integer> spinHoras = new Spinner<>(0, 100, m.getId() == 0 ? 0 : m.getHoras(), 1);
        spinHoras.setEditable(true);
        Spinner<Integer> spinSemestre = new Spinner<>(0, 20, m.getId() == 0 ? 0 : m.getSemestre(), 1);
        spinSemestre.setEditable(true);

        // Hack para forzar que JavaFX guarde el valor escrito a mano si no se presiona Enter
        configurarSpinnerFocusHack(spinCreditos);
        configurarSpinnerFocusHack(spinHoras);
        configurarSpinnerFocusHack(spinSemestre);

        ComboBox<TipoAula> comboTipo = new ComboBox<>(FXCollections.observableArrayList(tipoAulaDAO.listar()));

        // Selección por defecto: COMÚN (id 1) si es nuevo
        if (m.getId() == 0) {
            comboTipo.getItems().stream().filter(t -> t.getId() == 1).findFirst()
                    .ifPresent(comboTipo.getSelectionModel()::select);
        } else {
            comboTipo.getItems().stream().filter(t -> t.getId() == m.getIdTipoAulaReq()).findFirst()
                    .ifPresent(comboTipo.getSelectionModel()::select);
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Código:"), 0, 0);     grid.add(txtCodigo, 1, 0);
        grid.add(new Label("Nombre:"), 0, 1);      grid.add(txtNombre, 1, 1);
        grid.add(new Label("Departamento:"), 0, 2); grid.add(txtDepto, 1, 2);
        grid.add(new Label("Créditos:"), 0, 3);    grid.add(spinCreditos, 1, 3);
        grid.add(new Label("Horas:"), 0, 4);       grid.add(spinHoras, 1, 4);
        grid.add(new Label("Semestre:"), 0, 5);    grid.add(spinSemestre, 1, 5);
        grid.add(new Label("Aula Req.:"), 0, 6);   grid.add(comboTipo, 1, 6);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK && !txtNombre.getText().trim().isEmpty() && comboTipo.getValue() != null) {
                m.setCodigo(txtCodigo.getText().trim().toUpperCase());
                m.setNombre(txtNombre.getText().trim().toUpperCase());
                m.setDepartamento(txtDepto.getText().trim().toUpperCase());
                m.setCreditos(spinCreditos.getValue());
                m.setHoras(spinHoras.getValue());
                m.setSemestre(spinSemestre.getValue());
                m.setIdTipoAulaReq(comboTipo.getValue().getId());
                materiaDAO.guardar(m);
                return true;
            }
            return false;
        });

        dialog.showAndWait().ifPresent(res -> {
            if (res) {
                cargarDatos();
                onDataChanged.run();
            }
        });
    }

    public void eliminar() {
        Materia s = tablaMaterias.getSelectionModel().getSelectedItem();
        if (s == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "ADVERTENCIA️\nEstá a punto de eliminar '" + s.getNombre() + "' de Materias.\n" +
                        "Esto puede borrar en cascada otros registros relacionados. ¿Seguro?",
                ButtonType.YES, ButtonType.NO);

        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                materiaDAO.eliminar(s.getId());
                cargarDatos();
                onDataChanged.run();
            }
        });
    }

    private void configurarSpinnerFocusHack(Spinner<Integer> spinner) {
        spinner.focusedProperty().addListener((o, w, isNow) -> {
            if (!isNow) {
                try {
                    spinner.getValueFactory().setValue(Integer.parseInt(spinner.getEditor().getText()));
                } catch (NumberFormatException e) {
                    // Ignorar entrada inválida
                }
            }
        });
    }
}
