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

    @Override
    public double calculateLocalPenalty(HorarioDTO target, List<HorarioDTO> horarios, Map<Integer, Aula> aulas, int asignados) {
        if (target.idAulaAsignada == null) return 0.0;
        
        double energiaDistanciaEst = 0;
        Aula aTarget = aulas.get(target.idAulaAsignada);
        
        for (HorarioDTO otro : horarios) {
            if (otro == target || otro.semestre != target.semestre || otro.idAulaAsignada == null || !otro.dia.equals(target.dia)) continue;
            
            // Caso 1: otro ocurre justo ANTES de target (otro.horaFin == target.horaInicio)
            if (otro.horaFin == target.horaInicio) {
                Aula aOtro = aulas.get(otro.idAulaAsignada);
                energiaDistanciaEst += OptimizationMetrics.calcularPenalizacionDistancia(aOtro.getEdificio(), aTarget.getEdificio());
            }
            
            // Caso 2: otro ocurre justo DESPUES de target (target.horaFin == otro.horaInicio)
            if (target.horaFin == otro.horaInicio) {
                Aula aOtro = aulas.get(otro.idAulaAsignada);
                energiaDistanciaEst += OptimizationMetrics.calcularPenalizacionDistancia(aTarget.getEdificio(), aOtro.getEdificio());
            }
        }
        
        return peso * (energiaDistanciaEst / asignados);
    }
}
