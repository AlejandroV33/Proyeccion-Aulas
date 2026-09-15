package app.controller.gestion;

import app.model.dao.AulaDAO;
import app.model.dao.HorarioDAO;
import app.model.dao.ParaleloDAO;
import app.model.entity.Aula;
import app.model.entity.Horario;
import app.model.entity.HorarioFila;
import app.model.entity.ParaleloDetalle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

import java.util.ArrayList;
import java.util.List;

/**
 * Sub-controlador para la pestaña "Horarios".
 * Gestiona la tabla principal con deep-linking a otras pestañas,
 * edición de horarios por día, y modales para crear/editar horarios
 * con validación de choques de aula.
 */
public class HorarioTabController {

    private TextField txtBuscarMateria, txtBuscarDocente;
    private TableView<HorarioFila> tablaHorarios;
    private TableColumn<HorarioFila, String> colMateria, colDocente, colParalelo, colAula, colTipoReq;
    private TableColumn<HorarioFila, String> colLunes, colMartes, colMiercoles, colJueves, colViernes, colSabado;
    private TableColumn<HorarioFila, Integer> colMatriculados, colCapacidad;

    private TabPane tabPaneEdicion;
    private Tab tabMaterias, tabParalelos;

    private final HorarioDAO horarioDAO;
    private final AulaDAO aulaDAO;
    private final ParaleloDAO paraleloDAO;

    private List<Aula> todasLasAulasCache;
    private List<ParaleloDetalle> todosParalelosCache;

    private final ObservableList<HorarioFila> masterData = FXCollections.observableArrayList();
    private FilteredList<HorarioFila> filteredData;

    public HorarioTabController(HorarioDAO horarioDAO, AulaDAO aulaDAO, ParaleloDAO paraleloDAO) {
        this.horarioDAO = horarioDAO;
        this.aulaDAO = aulaDAO;
        this.paraleloDAO = paraleloDAO;
    }

    public void inicializar(TextField txtBuscarMateria, TextField txtBuscarDocente,
                            TableView<HorarioFila> tablaHorarios,
                            TableColumn<HorarioFila, String> colMateria,
                            TableColumn<HorarioFila, String> colDocente,
                            TableColumn<HorarioFila, String> colParalelo,
                            TableColumn<HorarioFila, String> colAula,
                            TableColumn<HorarioFila, String> colTipoReq,
                            TableColumn<HorarioFila, String> colLunes,
                            TableColumn<HorarioFila, String> colMartes,
                            TableColumn<HorarioFila, String> colMiercoles,
                            TableColumn<HorarioFila, String> colJueves,
                            TableColumn<HorarioFila, String> colViernes,
                            TableColumn<HorarioFila, String> colSabado,
                            TableColumn<HorarioFila, Integer> colMatriculados,
                            TableColumn<HorarioFila, Integer> colCapacidad,
                            TabPane tabPaneEdicion,
                            Tab tabMaterias, Tab tabParalelos) {
        this.txtBuscarMateria = txtBuscarMateria;
        this.txtBuscarDocente = txtBuscarDocente;
        this.tablaHorarios = tablaHorarios;
        this.colMateria = colMateria;
        this.colDocente = colDocente;
        this.colParalelo = colParalelo;
        this.colAula = colAula;
        this.colTipoReq = colTipoReq;
        this.colLunes = colLunes;
        this.colMartes = colMartes;
        this.colMiercoles = colMiercoles;
        this.colJueves = colJueves;
        this.colViernes = colViernes;
        this.colSabado = colSabado;
        this.colMatriculados = colMatriculados;
        this.colCapacidad = colCapacidad;
        this.tabPaneEdicion = tabPaneEdicion;
        this.tabMaterias = tabMaterias;
        this.tabParalelos = tabParalelos;

        this.todasLasAulasCache = aulaDAO.listar();
        this.todosParalelosCache = paraleloDAO.listarDetalles();

        configurarTabla();
        cargarDatos();
        configurarFiltros();
    }

    // ==========================================
    // CONFIGURACIÓN DE TABLA
    // ==========================================

    private void configurarTabla() {
        colMateria.setCellValueFactory(new PropertyValueFactory<>("materia"));
        colDocente.setCellValueFactory(new PropertyValueFactory<>("docente"));
        colParalelo.setCellValueFactory(new PropertyValueFactory<>("paralelo"));
        colMatriculados.setCellValueFactory(new PropertyValueFactory<>("matriculados"));
        colAula.setCellValueFactory(new PropertyValueFactory<>("aulaDesc"));
        colTipoReq.setCellValueFactory(new PropertyValueFactory<>("tipoAulaReq"));
        colCapacidad.setCellValueFactory(new PropertyValueFactory<>("capacidadAula"));

        colLunes.setCellValueFactory(new PropertyValueFactory<>("lunesText"));
        colMartes.setCellValueFactory(new PropertyValueFactory<>("martesText"));
        colMiercoles.setCellValueFactory(new PropertyValueFactory<>("miercolesText"));
        colJueves.setCellValueFactory(new PropertyValueFactory<>("juevesText"));
        colViernes.setCellValueFactory(new PropertyValueFactory<>("viernesText"));
        colSabado.setCellValueFactory(new PropertyValueFactory<>("sabadoText"));

        // Deep linking sutil (solo cursor "manito", sin azul)
        configurarDeepLink(colMateria, tabMaterias);
        configurarDeepLink(colParalelo, tabParalelos);
        configurarDeepLink(colMatriculados, tabParalelos);
        configurarDeepLink(colDocente, tabParalelos);

        // Edición de horas por día
        configurarEdicionDia(colLunes, "lunes");
        configurarEdicionDia(colMartes, "martes");
        configurarEdicionDia(colMiercoles, "miercoles");
        configurarEdicionDia(colJueves, "jueves");
        configurarEdicionDia(colViernes, "viernes");
        configurarEdicionDia(colSabado, "sabado");
    }

    public void cargarDatos() {
        masterData.setAll(horarioDAO.listarFilasEdicion());
    }

    /** Refresca las caches de aulas y paralelos (llamar después de CRUD en esas entidades). */
    public void refrescarCaches() {
        todasLasAulasCache = aulaDAO.listar();
        todosParalelosCache = paraleloDAO.listarDetalles();
    }

    // ==========================================
    // FILTROS
    // ==========================================

    private void configurarFiltros() {
        filteredData = new FilteredList<>(masterData, p -> true);
        txtBuscarMateria.textProperty().addListener((obs, oldV, newV) -> actualizarPredicado());
        txtBuscarDocente.textProperty().addListener((obs, oldV, newV) -> actualizarPredicado());

        SortedList<HorarioFila> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tablaHorarios.comparatorProperty());
        tablaHorarios.setItems(sortedData);
    }

    private void actualizarPredicado() {
        String filtroMat = txtBuscarMateria.getText().toLowerCase();
        String filtroDoc = txtBuscarDocente.getText().toLowerCase();

        filteredData.setPredicate(fila -> {
            boolean mat = fila.getMateria() != null && fila.getMateria().toLowerCase().contains(filtroMat);
            boolean doc = fila.getDocente() != null && fila.getDocente().toLowerCase().contains(filtroDoc);
            return (filtroMat.isEmpty() || mat) && (filtroDoc.isEmpty() || doc);
        });
    }

    // ==========================================
    // DEEP LINKING Y EDICIÓN DE DÍAS
    // ==========================================

    /** Configura el deep-link sutil: doble clic en una celda navega a la pestaña destino. */
    private <T> void configurarDeepLink(TableColumn<HorarioFila, T> columna, Tab destino) {
        columna.setCellFactory(tc -> {
            TableCell<HorarioFila, T> cell = new TableCell<>() {
                @Override
                protected void updateItem(T item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.toString());
                    setStyle(empty || item == null ? "" : "-fx-cursor: hand;");
                }
            };
            cell.setOnMouseClicked(e -> {
                if (!cell.isEmpty() && e.getClickCount() == 2) {
                    tabPaneEdicion.getSelectionModel().select(destino);
                }
            });
            return cell;
        });
    }

    /** Configura celdas de día para abrir el editor de horario al hacer doble clic. */
    private void configurarEdicionDia(TableColumn<HorarioFila, String> columna, String dia) {
        columna.setCellFactory(tc -> {
            TableCell<HorarioFila, String> cell = new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item);
                    setStyle(empty || item == null || item.isEmpty()
                            ? "" : "-fx-cursor: hand; -fx-background-color: #e8f4f8;");
                }
            };
            cell.setOnMouseClicked(e -> {
                if (!cell.isEmpty() && e.getClickCount() == 2
                        && cell.getText() != null && !cell.getText().isEmpty()) {
                    abrirEditorHorario(cell.getTableView().getItems().get(cell.getIndex()), dia);
                }
            });
            return cell;
        });
    }

    // ==========================================
    // MODAL NUEVO HORARIO
    // ==========================================

    public void abrirModalNuevoHorario() {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Registrar Nuevo Horario");
        dialog.setHeaderText("Asigna un nuevo bloque de clases");

        // 1. Buscador Paralelo
        TextField txtFiltroParalelo = new TextField();
        txtFiltroParalelo.setPromptText("Filtrar materia o docente...");
        ComboBox<ParaleloDetalle> comboParalelos = new ComboBox<>();
        comboParalelos.setPrefWidth(300);
        llenarComboParalelos(comboParalelos, "");
        txtFiltroParalelo.textProperty().addListener((obs, oldV, newV) ->
                llenarComboParalelos(comboParalelos, newV.toLowerCase()));

        // 2. Día y Horas
        ComboBox<String> comboDia = new ComboBox<>(FXCollections.observableArrayList(
                "Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado"));
        comboDia.getSelectionModel().selectFirst();
        Spinner<Integer> spinInicio = new Spinner<>(7, 22, 7, 1);
        Spinner<Integer> spinFin = new Spinner<>(7, 22, 9, 1);

        // 3. Buscador Aula (Opcional)
        TextField txtFiltroAula = new TextField();
        txtFiltroAula.setPromptText("Edificio, tipo, numero...");
        ComboBox<Aula> comboAulas = new ComboBox<>();
        comboAulas.setPrefWidth(300);
        llenarComboAulas(comboAulas, "");
        txtFiltroAula.textProperty().addListener((obs, oldV, newV) ->
                llenarComboAulas(comboAulas, newV.toLowerCase()));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("1. Buscar Paralelo:"), 0, 0);       grid.add(txtFiltroParalelo, 1, 0);
        grid.add(new Label("Seleccionar:"), 0, 1);               grid.add(comboParalelos, 1, 1);
        grid.add(new Label("2. Día:"), 0, 2);                    grid.add(comboDia, 1, 2);
        grid.add(new Label("Hora Inicio/Fin:"), 0, 3);
        grid.add(new HBox(5, spinInicio, new Label("a"), spinFin), 1, 3);
        grid.add(new Label("3. Buscar Aula (Opcional):"), 0, 4); grid.add(txtFiltroAula, 1, 4);
        grid.add(new Label("Seleccionar:"), 0, 5);               grid.add(comboAulas, 1, 5);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Validar choque de aula antes de cerrar
        final Button btOk = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        btOk.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (comboAulas.getValue() != null) {
                String choque = horarioDAO.verificarChoqueAula(
                        comboAulas.getValue().getId(),
                        comboDia.getValue().toLowerCase(),
                        spinInicio.getValue(),
                        spinFin.getValue(),
                        null // null porque es un horario nuevo
                );
                if (choque != null) {
                    new Alert(Alert.AlertType.WARNING,
                            "¡Choque de Aula Detectado!\nEsa aula ya está ocupada:\n\n" + choque).showAndWait();
                    event.consume();
                }
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK && comboParalelos.getValue() != null) {
                Horario h = new Horario();
                h.setIdParalelo(comboParalelos.getValue().getId());
                h.setDia(comboDia.getValue().toLowerCase());
                h.setHoraInicio(spinInicio.getValue());
                h.setHoraFin(spinFin.getValue());
                if (comboAulas.getValue() != null) h.setIdAula(comboAulas.getValue().getId());
                horarioDAO.insertar(h);
                return true;
            }
            return false;
        });

        dialog.showAndWait().ifPresent(guardado -> {
            if (guardado) cargarDatos();
        });
    }

    // ==========================================
    // MODAL EDITAR HORARIO
    // ==========================================

    private void abrirEditorHorario(HorarioFila fila, String dia) {
        Horario h = obtenerHorarioDeFila(fila, dia);
        if (h == null) return;

        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Editar Horario - " + dia.toUpperCase());
        dialog.setHeaderText(fila.getMateria() + " | " + fila.getParalelo());

        Spinner<Integer> spinInicio = new Spinner<>(7, 22, h.getHoraInicio(), 1);
        Spinner<Integer> spinFin = new Spinner<>(7, 22, h.getHoraFin(), 1);

        TextField txtFiltroAula = new TextField();
        txtFiltroAula.setPromptText("Filtrar edificio, tipo...");
        ComboBox<Aula> comboAulas = new ComboBox<>();
        comboAulas.setPrefWidth(250);
        llenarComboAulas(comboAulas, "");
        txtFiltroAula.textProperty().addListener((obs, oldV, newV) ->
                llenarComboAulas(comboAulas, newV.toLowerCase()));

        if (h.getIdAula() != null) {
            todasLasAulasCache.stream()
                    .filter(a -> a.getId() == h.getIdAula())
                    .findFirst()
                    .ifPresent(comboAulas.getSelectionModel()::select);
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Inicio:"), 0, 0);  grid.add(spinInicio, 1, 0);
        grid.add(new Label("Fin:"), 0, 1);     grid.add(spinFin, 1, 1);
        grid.add(new Label("Buscar:"), 0, 2);   grid.add(txtFiltroAula, 1, 2);
        grid.add(new Label("Aula:"), 0, 3);     grid.add(comboAulas, 1, 3);

        // Botón eliminar horario
        Button btnEliminar = new Button("🗑 Eliminar este Horario");
        btnEliminar.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white;");
        btnEliminar.setOnAction(e -> {
            Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                    "¿Seguro que desea eliminar el horario de " + dia + "?",
                    ButtonType.YES, ButtonType.NO);
            conf.showAndWait().ifPresent(res -> {
                if (res == ButtonType.YES) {
                    horarioDAO.eliminar(h.getId());
                    dialog.setResult(true);
                    dialog.close();
                }
            });
        });
        grid.add(btnEliminar, 0, 4, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Validar choque de aula antes de cerrar
        final Button btOk = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        btOk.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (comboAulas.getValue() != null) {
                String choque = horarioDAO.verificarChoqueAula(
                        comboAulas.getValue().getId(),
                        dia.toLowerCase(),
                        spinInicio.getValue(),
                        spinFin.getValue(),
                        h.getId() // Excluir su propio ID para no "chocar consigo mismo"
                );
                if (choque != null) {
                    new Alert(Alert.AlertType.WARNING,
                            "¡Choque de Aula Detectado!\nEsa aula ya está ocupada:\n\n" + choque).showAndWait();
                    event.consume();
                }
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                Integer idAula = comboAulas.getValue() != null ? comboAulas.getValue().getId() : null;
                horarioDAO.actualizarHorasYAula(h.getId(), spinInicio.getValue(), spinFin.getValue(), idAula);
                return true;
            }
            return false;
        });

        dialog.showAndWait().ifPresent(guardado -> {
            if (guardado) cargarDatos();
        });
    }

    // ==========================================
    // UTILIDADES DE COMBO BOXES
    // ==========================================

    /** Llena el ComboBox de aulas filtradas, incluyendo opción "SIN AULA" como primer ítem. */
    private void llenarComboAulas(ComboBox<Aula> combo, String filtro) {
        String filtroLimpio = filtro.trim().toUpperCase();

        List<Aula> filtradas = new ArrayList<>();
        filtradas.add(null); // Opción "SIN AULA"

        filtradas.addAll(todasLasAulasCache.stream()
                .filter(a -> a.getEstado().equalsIgnoreCase("activo"))
                .filter(a -> {
                    if (filtroLimpio.isEmpty()) return true;
                    String rutaAula = (a.getEdificio() + "/" + a.getPiso() + "/" + a.getNumero()).toUpperCase();
                    return rutaAula.contains(filtroLimpio);
                })
                .toList());

        combo.setItems(FXCollections.observableArrayList(filtradas));

        // Renderizador visual para mostrar "SIN AULA" y sufijo "(L)" para laboratorios
        Callback<ListView<Aula>, ListCell<Aula>> cellFactory = lv -> new ListCell<>() {
            @Override
            protected void updateItem(Aula item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else if (item == null) {
                    setText("--- SIN AULA ---");
                } else {
                    String sufijoLab = (item.getIdTipoAula() != 1) ? " (L)" : "";
                    setText(item.getEdificio() + "/" + item.getPiso() + "/" + item.getNumero()
                            + sufijoLab + " (" + item.getCapacidad() + ")");
                }
            }
        };

        combo.setCellFactory(cellFactory);
        combo.setButtonCell(cellFactory.call(null));
    }

    private void llenarComboParalelos(ComboBox<ParaleloDetalle> combo, String filtro) {
        List<ParaleloDetalle> filtradas = todosParalelosCache.stream()
                .filter(p -> filtro.isEmpty()
                        || p.getMateria().toLowerCase().contains(filtro)
                        || (p.getDocente() != null && p.getDocente().toLowerCase().contains(filtro)))
                .toList();
        combo.setItems(FXCollections.observableArrayList(filtradas));
    }

    private Horario obtenerHorarioDeFila(HorarioFila fila, String dia) {
        return switch (dia) {
            case "lunes" -> fila.getHorarioLunes();
            case "martes" -> fila.getHorarioMartes();
            case "miercoles" -> fila.getHorarioMiercoles();
            case "jueves" -> fila.getHorarioJueves();
            case "viernes" -> fila.getHorarioViernes();
            case "sabado" -> fila.getHorarioSabado();
            default -> null;
        };
    }
}
