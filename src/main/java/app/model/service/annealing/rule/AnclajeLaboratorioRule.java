package app.model.service.annealing.rule;

import app.model.dao.HorarioDAO.HorarioDTO;
import app.model.entity.Aula;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AnclajeLaboratorioRule implements OptimizationRule {
    private final double pesoHard;

    public AnclajeLaboratorioRule(double pesoHard) {
        this.pesoHard = pesoHard;
    }

    @Override
    public double calculatePenalty(List<HorarioDTO> horarios, Map<Integer, Aula> aulas, int asignados) {
        double penalizacion = 0;

        Map<String, List<HorarioDTO>> porDocente = horarios.stream()
                .filter(h -> h.docente != null && !h.docente.equalsIgnoreCase("Sin profesor") && h.idAulaAsignada != null)
                .collect(Collectors.groupingBy(h -> h.docente));

        for (List<HorarioDTO> horDocente : porDocente.values()) {
            List<HorarioDTO> especiales = new ArrayList<>();
            List<HorarioDTO> comunes = new ArrayList<>();

            for(HorarioDTO h : horDocente) {
                if(esComun(h.nombreTipoAula)) comunes.add(h);
                else especiales.add(h);
            }

            if (comunes.isEmpty() || especiales.isEmpty()) continue;

            Set<String> pisosDeTodasLasMaterias = new HashSet<>();
            for (HorarioDTO h : comunes) {
                Aula a = aulas.get(h.idAulaAsignada);
                pisosDeTodasLasMaterias.add(a.getEdificio() + "|" + a.getPiso());
            }

            Set<String> pisosLaboratorios = new HashSet<>();
            for (HorarioDTO h : especiales) {
                Aula a = aulas.get(h.idAulaAsignada);
                pisosLaboratorios.add(a.getEdificio() + "|" + a.getPiso());
            }

            boolean anclajeExitoso = pisosLaboratorios.stream()
                    .anyMatch(pisosDeTodasLasMaterias::contains);

            if (!anclajeExitoso) {
                penalizacion += 100.0;
            }
        }
        return pesoHard * penalizacion;
    }
    
    private boolean esComun(String tipo) {
        return tipo != null && tipo.equalsIgnoreCase("comun");
    }
}
