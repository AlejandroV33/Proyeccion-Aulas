package app.model.service.annealing.rule;

import app.model.dao.HorarioDAO.HorarioDTO;
import app.model.entity.Aula;
import app.model.service.OptimizationMetrics;
import java.util.List;
import java.util.Map;

public class OcupacionAjusteRule implements OptimizationRule {
    @Override
    public double calculatePenalty(List<HorarioDTO> horarios, Map<Integer, Aula> aulas, int asignados) {
        double energiaOcupacion = 0;
        for (HorarioDTO h : horarios) {
            if (h.idAulaAsignada == null) {
                if (esComun(h.nombreTipoAula)) {
                    energiaOcupacion += 10.0;
                }
            } else {
                Aula a = aulas.get(h.idAulaAsignada);
                energiaOcupacion += OptimizationMetrics.calcularIndiceAjuste(h.matriculados, a.getCapacidad());
            }
        }
        return energiaOcupacion / asignados;
    }

    @Override
    public double calculateLocalPenalty(HorarioDTO target, List<HorarioDTO> horarios, Map<Integer, Aula> aulas, int asignados) {
        if (target.idAulaAsignada == null) {
            return esComun(target.nombreTipoAula) ? (10.0 / asignados) : 0;
        }
        Aula a = aulas.get(target.idAulaAsignada);
        return OptimizationMetrics.calcularIndiceAjuste(target.matriculados, a.getCapacidad()) / asignados;
    }

    private boolean esComun(String tipo) {
        return tipo != null && tipo.equalsIgnoreCase("comun");
    }
}
