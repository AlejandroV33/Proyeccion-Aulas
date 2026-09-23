package app.model.service.annealing.rule;

import app.model.dao.HorarioDAO.HorarioDTO;
import app.model.entity.Aula;
import java.util.List;
import java.util.Map;

public class DistanciaDocenteRule implements OptimizationRule {
    private final double peso;

    public DistanciaDocenteRule(double peso) {
        this.peso = peso;
    }

    @Override
    public double calculatePenalty(List<HorarioDTO> horarios, Map<Integer, Aula> aulas, int asignados) {
        double energiaDistanciaDoc = 0;
        for (HorarioDTO actual : horarios) {
            if (actual.idAulaAsignada == null || actual.docente == null || actual.docente.equalsIgnoreCase("Sin profesor") || actual.docente.isEmpty()) continue;
            
            for (HorarioDTO otro : horarios) {
                if (otro == actual || otro.idAulaAsignada == null || !otro.dia.equals(actual.dia)) continue;
                if (otro.horaFin == actual.horaInicio && actual.docente.equals(otro.docente)) {
                    Aula a1 = aulas.get(otro.idAulaAsignada);
                    Aula a2 = aulas.get(actual.idAulaAsignada);
                    
                    if (a1.getId() != a2.getId()) {
                        if (a1.getEdificio().equals(a2.getEdificio())) {
                            if (a1.getPiso().equals(a2.getPiso())) {
                                energiaDistanciaDoc += 2;
                            } else {
                                energiaDistanciaDoc += 10;
                            }
                        } else {
                            energiaDistanciaDoc += 30;
                        }
                    }
                }
            }
        }
        return peso * (energiaDistanciaDoc / asignados);
    }

    @Override
    public double calculateLocalPenalty(HorarioDTO target, List<HorarioDTO> horarios, Map<Integer, Aula> aulas, int asignados) {
        if (target.idAulaAsignada == null || target.docente == null || target.docente.equalsIgnoreCase("Sin profesor") || target.docente.isEmpty()) return 0.0;
        
        double energiaDistanciaDoc = 0;
        Aula aTarget = aulas.get(target.idAulaAsignada);
        
        for (HorarioDTO otro : horarios) {
            if (otro == target || otro.idAulaAsignada == null || !otro.dia.equals(target.dia) || !target.docente.equals(otro.docente)) continue;
            
            // Caso 1: otro ocurre justo ANTES de target
            if (otro.horaFin == target.horaInicio) {
                Aula aOtro = aulas.get(otro.idAulaAsignada);
                energiaDistanciaDoc += getPenalty(aOtro, aTarget);
            }
            
            // Caso 2: otro ocurre justo DESPUES de target
            if (target.horaFin == otro.horaInicio) {
                Aula aOtro = aulas.get(otro.idAulaAsignada);
                energiaDistanciaDoc += getPenalty(aTarget, aOtro);
            }
        }
        return peso * (energiaDistanciaDoc / asignados);
    }
    
    private double getPenalty(Aula a1, Aula a2) {
        if (a1.getId() == a2.getId()) return 0.0;
        if (a1.getEdificio().equals(a2.getEdificio())) {
            if (a1.getPiso().equals(a2.getPiso())) {
                return 2;
            } else {
                return 10;
            }
        }
        return 30;
    }
}
