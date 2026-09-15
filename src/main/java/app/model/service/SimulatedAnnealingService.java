package app.model.service;

import app.model.dao.AulaDAO;
import app.model.dao.HorarioDAO;
import app.model.dao.HorarioDAO.HorarioDTO;
import app.model.entity.Aula;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SimulatedAnnealingService {

    private final HorarioDAO horarioDAO = new HorarioDAO();
    private final AulaDAO aulaDAO = new AulaDAO();

    // parametros annealing ajustados
    private static final double TEMP_INICIAL = 1.0;
    private static final double TEMP_FINAL = 0.001;
    private static final double COOLING_RATE = 0.995;
    private static final int ITERACIONES_POR_TEMP = 1000;
    // Pesos de Energía
    private static final double W_DIST_ESTUDIANTES = 0.08;
    private static final double W_DIST_DOCENTES = 0.3;
    private static final double W_HARD_DOCENTES = 2.0; // Multiplicador para las reglas inquebrantables

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
        generarSolucionInicial(comunes, aulasComunes);

        double temperatura = TEMP_INICIAL;
        double energiaActual = calcularEnergiaTotal(todosHorarios, todasAulas); // Usamos todos

        Random rand = new Random();

        while (temperatura > TEMP_FINAL) {
            for (int i = 0; i < ITERACIONES_POR_TEMP; i++) {
                int idx = rand.nextInt(comunes.size());
                HorarioDTO h = comunes.get(idx);
                Integer aulaOriginal = h.idAulaAsignada;

                Aula nuevaAula = aulasComunes.get(rand.nextInt(aulasComunes.size()));

                if (nuevaAula.getCapacidad() < h.matriculados) continue;

                h.idAulaAsignada = nuevaAula.getId();

                if (hayColision(h, comunes)) { // Colisiones se buscan solo en comunes
                    h.idAulaAsignada = aulaOriginal;
                    continue;
                }

                double nuevaEnergia = calcularEnergiaTotal(todosHorarios, todasAulas); // Usamos todos
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

    private double calcularEnergiaTotal(List<HorarioDTO> todosHorarios, List<Aula> aulasMap) {
        double energiaOcupacion = 0;
        double energiaDistanciaEst = 0;
        double energiaDistanciaDoc = 0;
        int asignados = 0;

        Map<Integer, Aula> mapAulas = aulasMap.stream().collect(Collectors.toMap(Aula::getId, a -> a));

        for (HorarioDTO h : todosHorarios) {
            if (h.idAulaAsignada == null) {
                if(esComun(h.nombreTipoAula)) energiaOcupacion += 10.0;
                continue;
            }

            Aula a = mapAulas.get(h.idAulaAsignada);
            energiaOcupacion += OptimizationMetrics.calcularIndiceAjuste(h.matriculados, a.getCapacidad());
            energiaDistanciaEst += calcularDistanciaEstudiantes(h, todosHorarios, mapAulas);
            energiaDistanciaDoc += calcularDistanciaDocentes(h, todosHorarios, mapAulas);
            asignados++;
        }

        // Evaluamos las Reglas Fuertes (Mismo Piso / Anclaje Lab)
        double energiaReglasDuras = calcularPenalizacionesDurasDocente(todosHorarios, mapAulas);

        if (asignados == 0) return 10000;

        return (energiaOcupacion / asignados)
                + (W_DIST_ESTUDIANTES * (energiaDistanciaEst / asignados))
                + (W_DIST_DOCENTES * (energiaDistanciaDoc / asignados))
                + (W_HARD_DOCENTES * energiaReglasDuras); // Castigo severo si rompe las reglas
    }

    // ==========================================
    // NUEVO: EVALUACIÓN DE REGLAS FUERTES
    // ==========================================
    private double calcularPenalizacionesDurasDocente(List<HorarioDTO> todosHorarios, Map<Integer, Aula> mapAulas) {
        double penalizacion = 0;

        Map<String, List<HorarioDTO>> porDocente = todosHorarios.stream()
                .filter(h -> h.docente != null && !h.docente.equalsIgnoreCase("Sin profesor") && h.idAulaAsignada != null)
                .collect(Collectors.groupingBy(h -> h.docente));

        for (List<HorarioDTO> horDocente : porDocente.values()) {
            List<HorarioDTO> especiales = new ArrayList<>();
            List<HorarioDTO> comunes = new ArrayList<>();

            for(HorarioDTO h : horDocente) {
                if(esComun(h.nombreTipoAula)) comunes.add(h);
                else especiales.add(h);
            }

            if (comunes.isEmpty()) continue;

            Set<String> pisosDeTodasLasMaterias = new HashSet<>();
            penalizacion += evaluarReglaUnicoPisoMateria(comunes, mapAulas, pisosDeTodasLasMaterias);
            penalizacion += evaluarReglaAnclajeLaboratorio(especiales, mapAulas, pisosDeTodasLasMaterias);
        }
        return penalizacion;
    }

    private double evaluarReglaUnicoPisoMateria(List<HorarioDTO> comunes, Map<Integer, Aula> mapAulas, Set<String> pisosDeTodasLasMaterias) {
        double penalizacion = 0;
        Map<String, List<HorarioDTO>> comunesPorMateria = comunes.stream()
                .collect(Collectors.groupingBy(h -> h.materia));

        for (List<HorarioDTO> horariosMateria : comunesPorMateria.values()) {
            Set<String> pisosDeEstaMateria = new HashSet<>();
            for (HorarioDTO h : horariosMateria) {
                Aula a = mapAulas.get(h.idAulaAsignada);
                pisosDeEstaMateria.add(a.getEdificio() + "|" + a.getPiso());
            }

            if (pisosDeEstaMateria.size() > 1) {
                penalizacion += 50.0 * (pisosDeEstaMateria.size() - 1);
            }
            pisosDeTodasLasMaterias.addAll(pisosDeEstaMateria);
        }
        return penalizacion;
    }

    private double evaluarReglaAnclajeLaboratorio(List<HorarioDTO> especiales, Map<Integer, Aula> mapAulas, Set<String> pisosDeTodasLasMaterias) {
        if (especiales.isEmpty()) return 0;
        
        Set<String> pisosLaboratorios = new HashSet<>();
        for (HorarioDTO h : especiales) {
            Aula a = mapAulas.get(h.idAulaAsignada);
            pisosLaboratorios.add(a.getEdificio() + "|" + a.getPiso());
        }

        boolean anclajeExitoso = pisosLaboratorios.stream()
                .anyMatch(pisosDeTodasLasMaterias::contains);

        return anclajeExitoso ? 0 : 100.0;
    }

    // ==========================================
    // REGLA SUAVE: DISTANCIA ESTUDIANTES Y DOCENTES
    // ==========================================
    private double calcularDistanciaEstudiantes(HorarioDTO actual, List<HorarioDTO> todos, Map<Integer, Aula> aulas) {
        double penalizacion = 0;
        for (HorarioDTO otro : todos) {
            if (otro == actual || otro.semestre != actual.semestre || otro.idAulaAsignada == null || !otro.dia.equals(actual.dia)) continue;
            if (otro.horaFin == actual.horaInicio) {
                Aula a1 = aulas.get(otro.idAulaAsignada);
                Aula a2 = aulas.get(actual.idAulaAsignada);
                penalizacion += OptimizationMetrics.calcularPenalizacionDistancia(a1.getEdificio(), a2.getEdificio());
            }
        }
        return penalizacion;
    }

    private double calcularDistanciaDocentes(HorarioDTO actual, List<HorarioDTO> todos, Map<Integer, Aula> aulas) {
        if (actual.docente == null || actual.docente.equalsIgnoreCase("Sin profesor") || actual.docente.isEmpty()) return 0;

        double penalizacion = 0;
        for (HorarioDTO otro : todos) {
            if (otro == actual || otro.idAulaAsignada == null || !otro.dia.equals(actual.dia)) continue;

            // Si tiene clases seguidas
            if (otro.horaFin == actual.horaInicio && actual.docente.equals(otro.docente)) {
                Aula a1 = aulas.get(otro.idAulaAsignada);
                Aula a2 = aulas.get(actual.idAulaAsignada);

                if (a1.getId() == a2.getId()) {
                    penalizacion += 0; // Ideal: No se mueve
                } else if (a1.getEdificio().equals(a2.getEdificio())) {
                    if (a1.getPiso().equals(a2.getPiso())) {
                        penalizacion += 2; // Mismo piso (leve)
                    } else {
                        penalizacion += 10; // Mismo edificio, distinto piso (medio)
                    }
                } else {
                    penalizacion += 30; // Distinto edificio (grave)
                }
            }
        }
        return penalizacion;
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

        for (HorarioDTO h : horarios) {
            if (h.idAulaAsignada != null) {
                Aula a = mapAulas.get(h.idAulaAsignada);
                String prop = h.matriculados + "/" + a.getCapacidad();
                double idxOcup = (double) h.matriculados / a.getCapacidad();
                double idxAjuste = OptimizationMetrics.calcularIndiceAjuste(h.matriculados, a.getCapacidad());

                horarioDAO.actualizarAsignacion(h.id, h.idAulaAsignada, prop, idxOcup, idxAjuste);
            }
        }
        logger.accept(">> guardado completo y exitoso.");
    }
}