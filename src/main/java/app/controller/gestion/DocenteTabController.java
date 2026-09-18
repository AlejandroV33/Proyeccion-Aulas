package app.controller.gestion;

import app.model.dao.DocenteDAO;
import app.model.entity.Docente;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

/**
 * Sub-controlador para la pestaña "Docentes".
 * Gestiona la tabla con filtro de búsqueda y modales de creación/edición.
 */
public class DocenteTabController {

    private TableView<Docente> tablaDocentes;
    private TableColumn<Docente, String> colDocNombre, colDocPizarra;
    private TextField txtBuscarDocenteNom;

    private final DocenteDAO docenteDAO;
    private final ObservableList<Docente> masterDocentes = FXCollections.observableArrayList();
    private FilteredList<Docente> filteredDocentes;
    private Runnable onDataChanged;

    public DocenteTabController(DocenteDAO docenteDAO) {
        this.docenteDAO = docenteDAO;
    }

    public void inicializar(TextField txtBuscarDocenteNom,
                            TableView<Docente> tablaDocentes,
                            TableColumn<Docente, String> colDocNombre,
                            TableColumn<Docente, String> colDocPizarra,
                            Runnable onDataChanged) {
        this.txtBuscarDocenteNom = txtBuscarDocenteNom;
        this.tablaDocentes = tablaDocentes;
        this.colDocNombre = colDocNombre;
        this.colDocPizarra = colDocPizarra;
        this.onDataChanged = onDataChanged;

        configurarTabla();
    }

    private void configurarTabla() {
        colDocNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colDocPizarra.setCellValueFactory(new PropertyValueFactory<>("cualquierPizarra"));

        filteredDocentes = new FilteredList<>(masterDocentes, p -> true);
        txtBuscarDocenteNom.textProperty().addListener((o, old, n) ->
                filteredDocentes.setPredicate(d ->
                        n.isEmpty() || d.getNombre().toLowerCase().contains(n.toLowerCase())));
        tablaDocentes.setItems(new SortedList<>(filteredDocentes));

        tablaDocentes.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && tablaDocentes.getSelectionModel().getSelectedItem() != null) {
                abrirEditor(tablaDocentes.getSelectionModel().getSelectedItem());
            }
        });

        cargarDatos();
    }

    public void cargarDatos() {
        masterDocentes.setAll(docenteDAO.listar());
    }

    public void abrirModalNuevo() {
        abrirEditor(new Docente());
    }

    private void abrirEditor(Docente d) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle(d.getId() == 0 ? "Nuevo Docente" : "Editar Docente");

        TextField txtNombre = new TextField(d.getNombre());
        ComboBox<String> comboPiz = new ComboBox<>(FXCollections.observableArrayList("si", "no"));
        comboPiz.setValue(d.getId() == 0 ? "si" : d.getCualquierPizarra());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Nombre Completo:"), 0, 0);
        grid.add(txtNombre, 1, 0);
        grid.add(new Label("¿Cualquier Pizarra?:"), 0, 1);
        grid.add(comboPiz, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK && !txtNombre.getText().isEmpty()) {
                d.setNombre(txtNombre.getText());
                d.setCualquierPizarra(comboPiz.getValue());
                docenteDAO.guardar(d);
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
        Docente s = tablaDocentes.getSelectionModel().getSelectedItem();
        if (s == null) {
            app.util.AlertUtil.mostrarAdvertencia("Seleccione un docente de la tabla.");
            return;
        }

        boolean confirmar = app.util.AlertUtil.pedirConfirmacion("Confirmar Eliminacion", 
                "ADVERTENCIA\nEsta a punto de eliminar a '" + s.getNombre() + "'.\n" +
                "Esto puede borrar en cascada otros registros relacionados. ¿Seguro?");

        if (confirmar) {
            docenteDAO.eliminar(s.getId());
            cargarDatos();
            onDataChanged.run();
        }
    }
}
