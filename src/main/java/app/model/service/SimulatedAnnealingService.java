package app.model.service;

import app.model.dao.AulaDAO;
import app.model.dao.HorarioDAO;
import app.model.dao.HorarioDAO.HorarioDTO;
import app.model.entity.Aula;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import app.model.service.annealing.EnergyEvaluator;
import app.model.service.annealing.rule.*;

public class SimulatedAnnealingService {

    private final HorarioDAO horarioDAO = new HorarioDAO();
    private final AulaDAO aulaDAO = new AulaDAO();

    // parametros annealing ajustados
    private static final double TEMP_INICIAL = 20.0;
    private static final double TEMP_FINAL = 0.001;
    private static final double COOLING_RATE = 0.9995;
    private static final int ITERACIONES_POR_TEMP = 5000;

    private final EnergyEvaluator evaluator;

    public SimulatedAnnealingService() {
        this.evaluator = new EnergyEvaluator(Arrays.asList(
                new OcupacionAjusteRule(),
                new DistanciaEstudianteRule(0.08),
                new DistanciaDocenteRule(0.3),
                new UnicoPisoMateriaRule(2.0),
                new AnclajeLaboratorioRule(2.0)
        ));
    }

    public void ejecutarMejorDeTres(Consumer<String> logger) {
        logger.accept("--- iniciando algoritmo de asignacion (mejor de 3) ---");

        List<HorarioDTO> todosHorarios = horarioDAO.listarParaAlgoritmo();
        List<Aula> todasAulas = aulaDAO.listar().stream()
                .filter(a -> "activo".equalsIgnoreCase(a.getEstado()))
                .toList();

        List<HorarioDTO> especiales = new ArrayList<>();
        List<HorarioDTO> comunes = new ArrayList<>();

        for (HorarioDTO h : todosHorarios) {
            if (esComun(h.nombreTipoAula)) comunes.add(h);
            else especiales.add(h);
        }

        logger.accept("horarios especiales: " + especiales.size());
        logger.accept("horarios comunes: " + comunes.size());

        // fase 1: determinista
        asignarEspeciales(especiales, todasAulas, logger);

        // fase 2: annealing 3 veces
        logger.accept(">> fase 2: iniciando 3 intentos de simulated annealing...");
        List<Aula> aulasComunes = todasAulas.stream()
                .filter(a -> a.getIdTipoAula() == 1)
                .toList();

        double mejorEnergiaGlobal = Double.MAX_VALUE;
        Map<Integer, Integer> mejorConfiguracion = new HashMap<>();

        for (int intento = 1; intento <= 3; intento++) {
            logger.accept("> ejecutando intento " + intento + "...");

            // limpiar asignaciones comunes en memoria para este intento
            for (HorarioDTO h : comunes) h.idAulaAsignada = null;

            double energiaFinal = ejecutarUnIntentoAnnealing(comunes, todosHorarios, aulasComunes, todasAulas);
            logger.accept("  energia obtenida en intento " + intento + ": " + String.format("%.4f", energiaFinal));

            if (energiaFinal < mejorEnergiaGlobal) {
                mejorEnergiaGlobal = energiaFinal;
                // guardar mapa de la mejor configuracion
                for (HorarioDTO h : comunes) {
                    mejorConfiguracion.put(h.id, h.idAulaAsignada);
                }
            }
        }

        logger.accept(">> seleccionando mejor escenario con energia: " + String.format("%.4f", mejorEnergiaGlobal));

        // aplicar la mejor configuracion a los objetos en memoria
        for (HorarioDTO h : comunes) {
            h.idAulaAsignada = mejorConfiguracion.get(h.id);
        }

        // guardar todo en bd
        guardarResultados(todosHorarios, todasAulas, logger);
    }

    private boolean esComun(String tipo) {
        return tipo != null && tipo.equalsIgnoreCase("comun");
    }

    private void asignarEspeciales(List<HorarioDTO> horarios, List<Aula> aulas, Consumer<String> logger) {
        logger.accept(">> asignando aulas especiales...");

        for (HorarioDTO h : horarios) {
            Aula mejorAula = null;
            double menorCosto = Double.MAX_VALUE;

            List<Aula> candidatas = aulas.stream()
                    .filter(a -> a.getIdTipoAula() == h.idTipoAulaReq)
                    .filter(a -> a.getCapacidadFlexible() >= h.matriculados)
                    .toList();

            for (Aula a : candidatas) {
                if (estaOcupada(a.getId(), h, horarios)) continue;

                double costo = OptimizationMetrics.calcularIndiceAjuste(h.matriculados, a.getCapacidad());
                if (costo < menorCosto) {
                    menorCosto = costo;
                    mejorAula = a;
                }
            }

            if (mejorAula != null) {
                h.idAulaAsignada = mejorAula.getId();
            } else {
                logger.accept(String.format("alerta (sin aula especial) para el horario: | %s | %s | %s | %s | %d-%d | matriculados: %d | tipo de aula requerida: %s",
                        h.docente, h.materia, h.paralelo, h.dia, h.horaInicio, h.horaFin, h.matriculados, h.nombreTipoAula));
            }
        }
    }

    private double ejecutarUnIntentoAnnealing(List<HorarioDTO> comunes, List<HorarioDTO> todosHorarios, List<Aula> aulasComunes, List<Aula> todasAulas) {
        Map<Integer, Aula> aulasMap = todasAulas.stream().collect(Collectors.toMap(Aula::getId, a -> a));
        generarSolucionInicial(comunes, aulasComunes);

        double temperatura = TEMP_INICIAL;
        double energiaActual = evaluator.calcularEnergiaTotal(todosHorarios, aulasMap);

        Random rand = new Random();

        while (temperatura > TEMP_FINAL) {
            for (int i = 0; i < ITERACIONES_POR_TEMP; i++) {
                if (rand.nextBoolean()) {
                    // ==========================================
                    // OPERACION 1: MOVE (Mover a un aula aleatoria)
                    // ==========================================
                    int idx = rand.nextInt(comunes.size());
                    HorarioDTO h = comunes.get(idx);
                    Integer aulaOriginal = h.idAulaAsignada;

                    Aula nuevaAula = aulasComunes.get(rand.nextInt(aulasComunes.size()));

                    if (nuevaAula.getCapacidad() < h.matriculados) continue;

                    double costoLocalViejo = evaluator.calcularEnergiaLocal(h, todosHorarios, aulasMap);
                    h.idAulaAsignada = nuevaAula.getId();

                    if (hayColision(h, comunes)) {
                        h.idAulaAsignada = aulaOriginal;
                        continue;
                    }

                    double costoLocalNuevo = evaluator.calcularEnergiaLocal(h, todosHorarios, aulasMap);
                    double delta = costoLocalNuevo - costoLocalViejo;

                    if (delta < 0 || Math.exp(-delta / temperatura) > rand.nextDouble()) {
                        energiaActual += delta;
                    } else {
                        h.idAulaAsignada = aulaOriginal;
                    }
                } else {
                    // ==========================================
                    // OPERACION 2: SWAP (Intercambiar dos horarios)
                    // ==========================================
                    int idx1 = rand.nextInt(comunes.size());
                    int idx2 = rand.nextInt(comunes.size());
                    if (idx1 == idx2) continue;

                    HorarioDTO h1 = comunes.get(idx1);
                    HorarioDTO h2 = comunes.get(idx2);

                    if (h1.idAulaAsignada == null || h2.idAulaAsignada == null || h1.idAulaAsignada.equals(h2.idAulaAsignada)) continue;

                    Integer aulaOrig1 = h1.idAulaAsignada;
                    Integer aulaOrig2 = h2.idAulaAsignada;

                    Aula a1 = aulasMap.get(aulaOrig1);
                    Aula a2 = aulasMap.get(aulaOrig2);

                    // Validar capacidades cruzadas
                    if (a2.getCapacidad() < h1.matriculados || a1.getCapacidad() < h2.matriculados) continue;

                    // Calculo secuencial para evitar desincronizacion (drift) en caso de interacciones
                    // Paso 1: Mover h1 al aula de h2
                    double costoLocalViejo1 = evaluator.calcularEnergiaLocal(h1, todosHorarios, aulasMap);
                    h1.idAulaAsignada = aulaOrig2;
                    double costoLocalNuevo1 = evaluator.calcularEnergiaLocal(h1, todosHorarios, aulasMap);
                    double delta1 = costoLocalNuevo1 - costoLocalViejo1;

                    // Paso 2: Mover h2 al aula de h1 (h1 ya esta en a2, asi que interactuaran correctamente)
                    double costoLocalViejo2 = evaluator.calcularEnergiaLocal(h2, todosHorarios, aulasMap);
                    h2.idAulaAsignada = aulaOrig1;
                    double costoLocalNuevo2 = evaluator.calcularEnergiaLocal(h2, todosHorarios, aulasMap);
                    double delta2 = costoLocalNuevo2 - costoLocalViejo2;

                    double deltaTotal = delta1 + delta2;

                    // Verificar colisiones en el estado final
                    if (hayColision(h1, comunes) || hayColision(h2, comunes)) {
                        h1.idAulaAsignada = aulaOrig1;
                        h2.idAulaAsignada = aulaOrig2;
                        continue;
                    }

                    if (deltaTotal < 0 || Math.exp(-deltaTotal / temperatura) > rand.nextDouble()) {
                        energiaActual += deltaTotal;
                    } else {
                        h1.idAulaAsignada = aulaOrig1;
                        h2.idAulaAsignada = aulaOrig2;
                    }
                }
            }
            temperatura *= COOLING_RATE;
        }
        return energiaActual;
    }

    private void generarSolucionInicial(List<HorarioDTO> horarios, List<Aula> aulas) {
        for (HorarioDTO h : horarios) {
            List<Aula> validas = aulas.stream()
                    .filter(a -> a.getCapacidadFlexible() >= h.matriculados)
                    .collect(Collectors.toList());

            Collections.shuffle(validas);

            for (Aula a : validas) {
                h.idAulaAsignada = a.getId();
                if (!hayColision(h, horarios)) break;
                h.idAulaAsignada = null;
            }
        }
    }





    private boolean estaOcupada(int idAula, HorarioDTO actual, List<HorarioDTO> lista) {
        for (HorarioDTO h : lista) {
            if (h == actual) continue;
            if (h.idAulaAsignada != null && h.idAulaAsignada == idAula && h.dia.equals(actual.dia)) {
                if (actual.horaInicio < h.horaFin && actual.horaFin > h.horaInicio) return true;
            }
        }
        return false;
    }

    private boolean hayColision(HorarioDTO actual, List<HorarioDTO> lista) {
        if (actual.idAulaAsignada == null) return false;
        return estaOcupada(actual.idAulaAsignada, actual, lista);
    }

    private void guardarResultados(List<HorarioDTO> horarios, List<Aula> aulas, Consumer<String> logger) {
        logger.accept(">> guardando resultados definitivos en base de datos...");
        Map<Integer, Aula> mapAulas = aulas.stream().collect(Collectors.toMap(Aula::getId, a -> a));

        app.model.dao.BaseDAO.startTransaction();
        try {
            for (HorarioDTO h : horarios) {
                if (h.idAulaAsignada != null) {
                    Aula a = mapAulas.get(h.idAulaAsignada);
                    String prop = h.matriculados + "/" + a.getCapacidad();
                    double idxOcup = (double) h.matriculados / a.getCapacidad();
                    double idxAjuste = OptimizationMetrics.calcularIndiceAjuste(h.matriculados, a.getCapacidad());

                    horarioDAO.actualizarAsignacion(h.id, h.idAulaAsignada, prop, idxOcup, idxAjuste);
                }
            }
            app.model.dao.BaseDAO.commitTransaction();
            logger.accept(">> guardado completo y exitoso.");
        } catch (Exception e) {
            app.model.dao.BaseDAO.rollbackTransaction();
            logger.accept(">> ERROR al guardar resultados. Transaccion revertida.");
            throw e;
        }
    }
}