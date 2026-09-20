package app.controller;

import app.controller.gestion.*;
import app.model.dao.*;
import app.model.entity.*;
import app.model.service.ExcelExtractorService;
import app.model.service.ExcelPrepararService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;

import java.io.IOException;

public class GestionController {

    // --- FXML ELEMENTS ---
    @FXML private TabPane mainTabPane;
    @FXML private TabPane tabPaneEdicion;
    @FXML private Tab tabHorarios, tabParalelos, tabMaterias, tabDocentes, tabAulas, tabTipos;

    // HORARIOS
    @FXML private TextField txtBuscarMateria, txtBuscarDocente;
    @FXML private TableView<HorarioFila> tablaHorarios;
    @FXML private TableColumn<HorarioFila, String> colMateria, colDocente, colParalelo, colAula, colTipoReq, colLunes, colMartes, colMiercoles, colJueves, colViernes, colSabado;
    @FXML private TableColumn<HorarioFila, Integer> colMatriculados, colCapacidad;

    // PARALELOS
    @FXML private TableView<Paralelo> tablaParalelos;
    @FXML private TableColumn<Paralelo, String> colParaleloMat, colParaleloDoc, colParaleloNom;
    @FXML private TableColumn<Paralelo, Integer> colParaleloEst;
    @FXML private TextField txtBuscarParaleloMat, txtBuscarParaleloDoc, txtBuscarParaleloNom;

    // MATERIAS
    @FXML private TableColumn<Materia, String> colMatCodigo;
    @FXML private TextField txtBuscarMateriaNom;
    @FXML private TableView<Materia> tablaMaterias;
    @FXML private TableColumn<Materia, String> colMatNombre, colMatDepto, colMatTipo;
    @FXML private TableColumn<Materia, Integer> colMatSemestre, colMatCreditos, colMatHoras;

    // DOCENTES
    @FXML private TextField txtBuscarDocenteNom;
    @FXML private TableView<Docente> tablaDocentes;
    @FXML private TableColumn<Docente, String> colDocNombre, colDocPizarra;

    // AULAS
    @FXML private TextField txtBuscarAula;
    @FXML private TableView<Aula> tablaAulas;
    @FXML private TableColumn<Aula, String> colAulEdificio, colAulPiso, colAulNumero, colAulTipo, colAulEstado;
    @FXML private TableColumn<Aula, Integer> colAulCapacidad;

    // TIPOS AULA
    @FXML private TableView<TipoAula> tablaTiposAulas;
    @FXML private TableColumn<TipoAula, String> colTipNombre;

    // EXCEL
    @FXML private TextArea txtConsolaExcel;
    @FXML private ProgressIndicator progressExcel;

    // VISOR
    @FXML private ComboBox<String> comboFiltroEdificio, comboOrdenOcupacion;
    @FXML private TableView<AulaOcupacion> tablaVisorAulas;
    @FXML private TableColumn<AulaOcupacion, String> colVisAula, colVisLun, colVisMar, colVisMie, colVisJue, colVisVie;
    @FXML private CheckBox chkSoloOcupadas;

    // --- DAOs & SERVICES ---
    private final HorarioDAO horarioDAO = new HorarioDAO();
    private final AulaDAO aulaDAO = new AulaDAO();
    private final ParaleloDAO paraleloDAO = new ParaleloDAO();
    private final MateriaDAO materiaDAO = new MateriaDAO();
    private final DocenteDAO docenteDAO = new DocenteDAO();
    private final TipoAulaDAO tipoAulaDAO = new TipoAulaDAO();
    private final ExcelPrepararService excelPrepararService = new ExcelPrepararService();
    private final ExcelExtractorService extractorService = new ExcelExtractorService();

    // --- SUB CONTROLLERS ---
    private AulaTabController aulaTabController;
    private DocenteTabController docenteTabController;
    private HorarioTabController horarioTabController;
    private ImportExcelController importExcelController;
    private MateriaTabController materiaTabController;
    private ParaleloTabController paraleloTabController;
    private TipoAulaTabController tipoAulaTabController;
    private VisorAulasController visorAulasController;

    @FXML
    public void initialize() {
        // Instanciar
        aulaTabController = new AulaTabController(aulaDAO, tipoAulaDAO);
        docenteTabController = new DocenteTabController(docenteDAO);
        horarioTabController = new HorarioTabController(horarioDAO, aulaDAO, paraleloDAO);
        importExcelController = new ImportExcelController(excelPrepararService, extractorService);
        materiaTabController = new MateriaTabController(materiaDAO, tipoAulaDAO);
        paraleloTabController = new ParaleloTabController(paraleloDAO, materiaDAO, docenteDAO);
        tipoAulaTabController = new TipoAulaTabController(tipoAulaDAO);
        visorAulasController = new VisorAulasController(horarioDAO);

        // Inicializar delegando UI elements
        Runnable onDataChangedGlobal = this::refrescarDatos;

        aulaTabController.inicializar(txtBuscarAula, tablaAulas, colAulEdificio, colAulPiso, colAulNumero, colAulCapacidad, colAulTipo, colAulEstado, onDataChangedGlobal);
        docenteTabController.inicializar(txtBuscarDocenteNom, tablaDocentes, colDocNombre, colDocPizarra, onDataChangedGlobal);
        materiaTabController.inicializar(txtBuscarMateriaNom, tablaMaterias, colMatCodigo, colMatNombre, colMatDepto, colMatTipo, colMatSemestre, colMatCreditos, colMatHoras, onDataChangedGlobal);
        tipoAulaTabController.inicializar(tablaTiposAulas, colTipNombre, onDataChangedGlobal);
        
        paraleloTabController.inicializar(txtBuscarParaleloMat, txtBuscarParaleloDoc, txtBuscarParaleloNom, tablaParalelos, colParaleloMat, colParaleloDoc, colParaleloNom, colParaleloEst, tabPaneEdicion, tabMaterias, onDataChangedGlobal);
        
        horarioTabController.inicializar(txtBuscarMateria, txtBuscarDocente, tablaHorarios, colMateria, colDocente, colParalelo, colAula, colTipoReq, colLunes, colMartes, colMiercoles, colJueves, colViernes, colSabado, colMatriculados, colCapacidad, tabPaneEdicion, tabMaterias, tabParalelos);
        
        importExcelController.inicializar(txtConsolaExcel, progressExcel, mainTabPane, onDataChangedGlobal);
        
        visorAulasController.inicializar(comboFiltroEdificio, comboOrdenOcupacion, tablaVisorAulas, colVisAula, colVisLun, colVisMar, colVisMie, colVisJue, colVisVie, chkSoloOcupadas, mainTabPane);

        // Actualizar datos automáticamente al cambiar de pestaña
        mainTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                if (newTab.getText().contains("Visor")) {
                    visorAulasController.cargarDatos();
                } else if (newTab.getText().contains("Edición") || newTab.getText().contains("Edicion")) {
                    Tab activa = tabPaneEdicion.getSelectionModel().getSelectedItem();
                    actualizarPestaniaSecundaria(activa);
                }
            }
        });

        tabPaneEdicion.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            actualizarPestaniaSecundaria(newTab);
        });
    }

    private void actualizarPestaniaSecundaria(Tab activa) {
        if (activa == tabHorarios) horarioTabController.cargarDatos();
        else if (activa == tabParalelos) paraleloTabController.cargarDatos();
        else if (activa == tabMaterias) materiaTabController.cargarDatos();
        else if (activa == tabDocentes) docenteTabController.cargarDatos();
        else if (activa == tabAulas) aulaTabController.cargarDatos();
        else if (activa == tabTipos) tipoAulaTabController.cargarDatos();
    }

    private void refrescarDatos() {
        horarioTabController.refrescarCaches();
        horarioTabController.cargarDatos();
        visorAulasController.cargarDatos();
    }

    // --- EVENTOS FXML DELEGADOS ---
    
    @FXML public void abrirModalNuevoHorario() { horarioTabController.abrirModalNuevoHorario(); }
    @FXML public void abrirModalNuevoParalelo() { paraleloTabController.abrirModalNuevo(); }
    @FXML public void eliminarParalelo() { paraleloTabController.eliminar(); }
    @FXML public void abrirModalNuevaMateria() { materiaTabController.abrirModalNueva(); }
    @FXML public void eliminarMateria() { materiaTabController.eliminar(); }
    @FXML public void abrirModalNuevoDocente() { docenteTabController.abrirModalNuevo(); }
    @FXML public void eliminarDocente() { docenteTabController.eliminar(); }
    @FXML public void abrirModalNuevaAula() { aulaTabController.abrirModalNueva(); }
    @FXML public void eliminarAula() { aulaTabController.eliminar(); }
    @FXML public void abrirModalNuevoTipo() { tipoAulaTabController.abrirModalNuevo(); }
    @FXML public void eliminarTipoAula() { tipoAulaTabController.eliminar(); }
    @FXML public void descargarEjemploExcel() { importExcelController.descargarEjemploExcel(); }
    @FXML public void prepararExcelCrudo() { importExcelController.prepararExcelCrudo(); }
    @FXML public void extraerEInyectarDatos() { importExcelController.extraerEInyectarDatos(); }
    @FXML public void cargarDatosVisorAulas() { visorAulasController.cargarDatos(); }
    @FXML public void exportarExcelVisor() { visorAulasController.exportarExcelVisor(); }
    
    @FXML 
    public void volverInicio() {
        try {
            Parent r = FXMLLoader.load(getClass().getResource("/view/MainView.fxml"));
            mainTabPane.getScene().setRoot(r);
        } catch (IOException e) {
            e.printStackTrace();
            app.util.AlertUtil.mostrarError("Error al volver al inicio: " + e.getMessage());
        }
    }
}