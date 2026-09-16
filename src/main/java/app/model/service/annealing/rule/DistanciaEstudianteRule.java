package app.model.service.annealing.rule;

import app.model.dao.HorarioDAO.HorarioDTO;
import app.model.entity.Aula;
import app.model.service.OptimizationMetrics;
import java.util.List;
import java.util.Map;

public class DistanciaEstudianteRule implements OptimizationRule {
    private final double peso;

    public DistanciaEstudianteRule(double peso) {
        this.peso = peso;
    }

    @Override
    public double calculatePenalty(List<HorarioDTO> horarios, Map<Integer, Aula> aulas, int asignados) {
        double energiaDistanciaEst = 0;
        for (HorarioDTO actual : horarios) {
            if (actual.idAulaAsignada == null) continue;
            for (HorarioDTO otro : horarios) {
                if (otro == actual || otro.semestre != actual.semestre || otro.idAulaAsignada == null || !otro.dia.equals(actual.dia)) continue;
                if (otro.horaFin == actual.horaInicio) {
                    Aula a1 = aulas.get(otro.idAulaAsignada);
                    Aula a2 = aulas.get(actual.idAulaAsignada);
                    energiaDistanciaEst += OptimizationMetrics.calcularPenalizacionDistancia(a1.getEdificio(), a2.getEdificio());
                }
            }
        }
        return peso * (energiaDistanciaEst / asignados);
    }
}
