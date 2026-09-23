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
                int idx = rand.nextInt(comunes.size());
                HorarioDTO h = comunes.get(idx);
                Integer aulaOriginal = h.idAulaAsignada;

                Aula nuevaAula = aulasComunes.get(rand.nextInt(aulasComunes.size()));

                if (nuevaAula.getCapacidad() < h.matriculados) continue;

                // 1. Obtener costo local ANTES del cambio
                double costoLocalViejo = evaluator.calcularEnergiaLocal(h, todosHorarios, aulasMap);

                // 2. Realizar el cambio de aula en 'h'
                h.idAulaAsignada = nuevaAula.getId();

                // 3. Validar colisiones (si hay colisión, revertir y usar continue)
                if (hayColision(h, comunes)) {
                    h.idAulaAsignada = aulaOriginal;
                    continue;
                }

                // 4. Obtener costo local DESPUÉS del cambio
                double costoLocalNuevo = evaluator.calcularEnergiaLocal(h, todosHorarios, aulasMap);

                // 5. Calcular la Nueva Energía Total sumando el Delta
                double nuevaEnergia = energiaActual - costoLocalViejo + costoLocalNuevo;
                double delta = nuevaEnergia - energiaActual;

                if (delta < 0) {
                    energiaActual = nuevaEnergia;
                } else {
                    if (Math.exp(-delta / temperatura) > rand.nextDouble()) {
                        energiaActual = nuevaEnergia;
                    } else {
                        h.idAulaAsignada = aulaOriginal;
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