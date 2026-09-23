package app.model.service.annealing.rule;

import app.model.dao.HorarioDAO.HorarioDTO;
import app.model.entity.Aula;
import java.util.List;
import java.util.Map;

public interface OptimizationRule {
    double calculatePenalty(List<HorarioDTO> horarios, Map<Integer, Aula> aulas, int asignados);
    double calculateLocalPenalty(HorarioDTO target, List<HorarioDTO> horarios, Map<Integer, Aula> aulas, int asignados);
}
