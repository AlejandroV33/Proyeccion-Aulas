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
}
