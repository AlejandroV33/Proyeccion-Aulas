package app.controller.gestion;

import app.model.dao.AulaDAO;
import app.model.dao.TipoAulaDAO;
import app.model.entity.Aula;
import app.model.entity.TipoAula;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.util.List;

/**
 * Sub-controlador para la pestaña "Aulas".
 * Gestiona la tabla con filtro de búsqueda progresivo (Ej. E17/P2/E004),
 * ordenamiento lógico de pisos, y modales de creación/edición.
 */
public class AulaTabController {

    private TextField txtBuscarAula;
    private TableView<Aula> tablaAulas;
    private TableColumn<Aula, String> colAulEdificio, colAulPiso, colAulNumero, colAulTipo, colAulEstado;
    private TableColumn<Aula, Integer> colAulCapacidad;

    private final AulaDAO aulaDAO;
    private final TipoAulaDAO tipoAulaDAO;
    private final ObservableList<Aula> masterAulas = FXCollections.observableArrayList();
    private FilteredList<Aula> filteredAulas;
    private Runnable onDataChanged;

    public AulaTabController(AulaDAO aulaDAO, TipoAulaDAO tipoAulaDAO) {
        this.aulaDAO = aulaDAO;
        this.tipoAulaDAO = tipoAulaDAO;
    }

    public void inicializar(TextField txtBuscarAula,
                            TableView<Aula> tablaAulas,
                            TableColumn<Aula, String> colAulEdificio,
                            TableColumn<Aula, String> colAulPiso,
                            TableColumn<Aula, String> colAulNumero,
                            TableColumn<Aula, Integer> colAulCapacidad,
                            TableColumn<Aula, String> colAulTipo,
                            TableColumn<Aula, String> colAulEstado,
                            Runnable onDataChanged) {
        this.txtBuscarAula = txtBuscarAula;
        this.tablaAulas = tablaAulas;
        this.colAulEdificio = colAulEdificio;
        this.colAulPiso = colAulPiso;
        this.colAulNumero = colAulNumero;
        this.colAulCapacidad = colAulCapacidad;
        this.colAulTipo = colAulTipo;
        this.colAulEstado = colAulEstado;
        this.onDataChanged = onDataChanged;

        configurarTabla();
    }

    private void configurarTabla() {
        colAulEdificio.setCellValueFactory(new PropertyValueFactory<>("edificio"));
        colAulPiso.setCellValueFactory(new PropertyValueFactory<>("piso"));
        colAulNumero.setCellValueFactory(new PropertyValueFactory<>("numero"));
        colAulCapacidad.setCellValueFactory(new PropertyValueFactory<>("capacidad"));
        colAulTipo.setCellValueFactory(new PropertyValueFactory<>("tipoAula"));
        colAulEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        // Ordenamiento lógico de pisos (PB < M < P1 < P2)
        colAulPiso.setComparator((piso1, piso2) ->
                Integer.compare(PisoUtils.obtenerValorPiso(piso1), PisoUtils.obtenerValorPiso(piso2)));

        // Buscador progresivo (Ej. E17/PB/E004)
        filteredAulas = new FilteredList<>(masterAulas, p -> true);
        txtBuscarAula.textProperty().addListener((o, old, n) -> {
            String filtro = n.trim().toUpperCase();
            filteredAulas.setPredicate(a -> {
                if (filtro.isEmpty()) return true;
                String rutaAula = (a.getEdificio() + "/" + a.getPiso() + "/" + a.getNumero()).toUpperCase();
                return rutaAula.contains(filtro);
            });
        });

        // Vincular ordenamiento con encabezados de columnas
        SortedList<Aula> sortedData = new SortedList<>(filteredAulas);
        sortedData.comparatorProperty().bind(tablaAulas.comparatorProperty());
        tablaAulas.setItems(sortedData);

        tablaAulas.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && tablaAulas.getSelectionModel().getSelectedItem() != null) {
                abrirEditor(tablaAulas.getSelectionModel().getSelectedItem());
            }
        });

        cargarDatos();

        // Orden visual por defecto: Edificio -> Piso -> Número
        tablaAulas.getSortOrder().addAll(colAulEdificio, colAulPiso, colAulNumero);
    }

    public void cargarDatos() {
        List<Aula> lista = aulaDAO.listarTabla();

        // Ordenamiento por defecto: Edificio -> Piso -> Número
        lista.sort((a, b) -> {
            int cmpEdif = a.getEdificio().compareToIgnoreCase(b.getEdificio());
            if (cmpEdif != 0) return cmpEdif;
            int cmpPiso = Integer.compare(
                    PisoUtils.obtenerValorPiso(a.getPiso()),
                    PisoUtils.obtenerValorPiso(b.getPiso()));
            if (cmpPiso != 0) return cmpPiso;
            return a.getNumero().compareToIgnoreCase(b.getNumero());
        });

        masterAulas.setAll(lista);
    }

    public void abrirModalNueva() {
        abrirEditor(new Aula());
    }

    private void abrirEditor(Aula a) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle(a.getId() == 0 ? "Nueva Aula" : "Editar Aula");

        TextField txtEdif = new TextField(a.getEdificio());
        TextField txtPiso = new TextField(a.getPiso());
        TextField txtNum = new TextField(a.getNumero());

        // Spinner con escritura manual habilitada
        Spinner<Integer> spinCap = new Spinner<>(1, 200, a.getId() == 0 ? 30 : a.getCapacidad(), 1);
        spinCap.setEditable(true);
        spinCap.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                try {
                    spinCap.getValueFactory().setValue(Integer.parseInt(spinCap.getEditor().getText()));
                } catch (NumberFormatException e) {
                    spinCap.getEditor().setText(spinCap.getValue().toString());
                }
            }
        });

        ComboBox<String> comboEst = new ComboBox<>(FXCollections.observableArrayList("activo", "inactivo"));
        comboEst.setValue(a.getId() == 0 ? "activo" : a.getEstado());

        ComboBox<TipoAula> comboTipo = new ComboBox<>(FXCollections.observableArrayList(tipoAulaDAO.listar()));
        if (a.getId() != 0) {
            comboTipo.getItems().stream().filter(t -> t.getId() == a.getIdTipoAula()).findFirst()
                    .ifPresent(comboTipo.getSelectionModel()::select);
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Edificio:"), 0, 0);   grid.add(txtEdif, 1, 0);
        grid.add(new Label("Piso:"), 0, 1);        grid.add(txtPiso, 1, 1);
        grid.add(new Label("Número:"), 0, 2);      grid.add(txtNum, 1, 2);
        grid.add(new Label("Capacidad:"), 0, 3);   grid.add(spinCap, 1, 3);
        grid.add(new Label("Tipo Aula:"), 0, 4);   grid.add(comboTipo, 1, 4);
        grid.add(new Label("Estado:"), 0, 5);      grid.add(comboEst, 1, 5);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK && !txtEdif.getText().isEmpty() && comboTipo.getValue() != null) {
                a.setEdificio(txtEdif.getText().trim().toUpperCase());
                a.setPiso(txtPiso.getText().trim().toUpperCase());
                a.setNumero(txtNum.getText().trim().toUpperCase());
                a.setCapacidad(spinCap.getValue());
                a.setEstado(comboEst.getValue());
                a.setIdTipoAula(comboTipo.getValue().getId());
                aulaDAO.guardar(a);
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
        Aula s = tablaAulas.getSelectionModel().getSelectedItem();
        if (s == null) {
            app.util.AlertUtil.mostrarAdvertencia("Seleccione un aula de la tabla.");
            return;
        }

        boolean confirmar = app.util.AlertUtil.pedirConfirmacion("Confirmar Eliminacion", 
                "ADVERTENCIA\nEsta a punto de eliminar el aula '" + s.getNumero() + "'.\n" +
                "Esto puede borrar en cascada otros registros relacionados. ¿Seguro?");

        if (confirmar) {
            aulaDAO.eliminar(s.getId());
            cargarDatos();
            onDataChanged.run();
        }
    }
}
