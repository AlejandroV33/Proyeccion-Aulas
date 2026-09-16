package app.model.service.annealing.rule;

import app.model.dao.HorarioDAO.HorarioDTO;
import app.model.entity.Aula;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class UnicoPisoMateriaRule implements OptimizationRule {
    private final double pesoHard;

    public UnicoPisoMateriaRule(double pesoHard) {
        this.pesoHard = pesoHard;
    }

    @Override
    public double calculatePenalty(List<HorarioDTO> horarios, Map<Integer, Aula> aulas, int asignados) {
        double penalizacion = 0;

        Map<String, List<HorarioDTO>> porDocente = horarios.stream()
                .filter(h -> h.docente != null && !h.docente.equalsIgnoreCase("Sin profesor") && h.idAulaAsignada != null)
                .collect(Collectors.groupingBy(h -> h.docente));

        for (List<HorarioDTO> horDocente : porDocente.values()) {
            List<HorarioDTO> comunes = new ArrayList<>();
            for(HorarioDTO h : horDocente) {
                if(esComun(h.nombreTipoAula)) comunes.add(h);
            }

            if (comunes.isEmpty()) continue;

            Map<String, List<HorarioDTO>> comunesPorMateria = comunes.stream()
                    .collect(Collectors.groupingBy(h -> h.materia));

            for (List<HorarioDTO> horariosMateria : comunesPorMateria.values()) {
                Set<String> pisosDeEstaMateria = new HashSet<>();
                for (HorarioDTO h : horariosMateria) {
                    Aula a = aulas.get(h.idAulaAsignada);
                    pisosDeEstaMateria.add(a.getEdificio() + "|" + a.getPiso());
                }

                if (pisosDeEstaMateria.size() > 1) {
                    penalizacion += 50.0 * (pisosDeEstaMateria.size() - 1);
                }
            }
        }
        return pesoHard * penalizacion;
    }
    
    private boolean esComun(String tipo) {
        return tipo != null && tipo.equalsIgnoreCase("comun");
    }
}
