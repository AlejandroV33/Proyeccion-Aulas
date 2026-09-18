package app.controller.gestion;

import app.model.dao.TipoAulaDAO;
import app.model.entity.TipoAula;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

/**
 * Sub-controlador para la pestaña "Tipos de Aula".
 * Gestiona la tabla y los modales de creación/edición de tipos.
 */
public class TipoAulaTabController {

    private TableView<TipoAula> tablaTiposAulas;
    private TableColumn<TipoAula, String> colTipNombre;

    private final TipoAulaDAO tipoAulaDAO;
    private final ObservableList<TipoAula> masterTipos = FXCollections.observableArrayList();
    private Runnable onDataChanged;

    public TipoAulaTabController(TipoAulaDAO tipoAulaDAO) {
        this.tipoAulaDAO = tipoAulaDAO;
    }

    public void inicializar(TableView<TipoAula> tablaTiposAulas,
                            TableColumn<TipoAula, String> colTipNombre,
                            Runnable onDataChanged) {
        this.tablaTiposAulas = tablaTiposAulas;
        this.colTipNombre = colTipNombre;
        this.onDataChanged = onDataChanged;

        configurarTabla();
    }

    private void configurarTabla() {
        colTipNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        tablaTiposAulas.setItems(masterTipos);
        tablaTiposAulas.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && tablaTiposAulas.getSelectionModel().getSelectedItem() != null) {
                abrirEditor(tablaTiposAulas.getSelectionModel().getSelectedItem());
            }
        });
        cargarDatos();
    }

    public void cargarDatos() {
        masterTipos.setAll(tipoAulaDAO.listar());
    }

    public void abrirModalNuevo() {
        abrirEditor(new TipoAula());
    }

    private void abrirEditor(TipoAula t) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle(t.getId() == 0 ? "Nuevo Tipo" : "Editar Tipo");

        TextField txtNombre = new TextField(t.getNombre());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Nombre:"), 0, 0);
        grid.add(txtNombre, 1, 0);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK && !txtNombre.getText().isEmpty()) {
                t.setNombre(txtNombre.getText());
                tipoAulaDAO.guardar(t);
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
        TipoAula s = tablaTiposAulas.getSelectionModel().getSelectedItem();
        if (s == null) {
            app.util.AlertUtil.mostrarAdvertencia("Seleccione un tipo de aula de la tabla.");
            return;
        }

        boolean confirmar = app.util.AlertUtil.pedirConfirmacion("Confirmar Eliminacion", 
                "ADVERTENCIA\nEsta a punto de eliminar '" + s.getNombre() + "'.\n" +
                "Esto borrara en cascada aulas y requerimientos de materia relacionados. ¿Seguro?");

        if (confirmar) {
            tipoAulaDAO.eliminar(s.getId());
            cargarDatos();
            onDataChanged.run();
        }
    }
}
