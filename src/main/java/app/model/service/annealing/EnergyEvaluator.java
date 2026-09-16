package app.model.service.annealing;

import app.model.dao.HorarioDAO.HorarioDTO;
import app.model.entity.Aula;
import app.model.service.annealing.rule.OptimizationRule;

import java.util.List;
import java.util.Map;

public class EnergyEvaluator {
    private final List<OptimizationRule> rules;

    public EnergyEvaluator(List<OptimizationRule> rules) {
        this.rules = rules;
    }

    public double calcularEnergiaTotal(List<HorarioDTO> todosHorarios, Map<Integer, Aula> aulasMap) {
        int asignados = 0;
        for (HorarioDTO h : todosHorarios) {
            if (h.idAulaAsignada != null) {
                asignados++;
            }
        }

        if (asignados == 0) return 10000.0;

        double totalEnergy = 0.0;
        for (OptimizationRule rule : rules) {
            totalEnergy += rule.calculatePenalty(todosHorarios, aulasMap, asignados);
        }

        return totalEnergy;
    }
}
