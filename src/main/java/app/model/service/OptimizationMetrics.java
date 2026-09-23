package app.model.service;

public class OptimizationMetrics {

    // parametros calibrados
    private static final double ALPHA_VACIO = 4.0;
    private static final double BETA_DESBORDE = 100.0;

    // calculo D(x)
    public static double calcularIndiceAjuste(int matriculados, int capacidad) {
        if (capacidad == 0) return 1000.0; // evitar division por cero

        double x = (double) matriculados / capacidad;

        double terminoBase = Math.pow(x - 0.9, 2);

        // penalizacion si esta muy vacio (< 50%)
        double terminoVacio = ALPHA_VACIO * Math.pow(Math.max(0, 0.5 - x), 2);

        // penalizacion si desborda
        double terminoDesborde = 0.0;
        int exceso = matriculados - capacidad;
        
        if (exceso > 0) {
            // El limite tolerable equivale a ~7.5% de la capacidad del aula
            // Ejemplo: Capacidad 15 -> limite 1. Capacidad 45 -> limite 3.
            int limiteTolerable = Math.max(1, (int) Math.floor(capacidad * 0.075));
            
            if (exceso > limiteTolerable) {
                // Penaliza exponencialmente si se pasa del limite tolerable
                terminoDesborde = Math.pow(3.0, (exceso - limiteTolerable)) * 500.0;
            } else {
                // Penaliza linealmente (fuerte pero aceptable) dentro del limite tolerable
                terminoDesborde = exceso * 50.0;
            }
        } else if (x > 0.95) {
            terminoDesborde = BETA_DESBORDE * Math.pow(x - 0.95, 2);
        }

        return terminoBase + terminoVacio + terminoDesborde;
    }

    // matriz de distancias
    // grupos: A(e17, e18), B(e19), C(e22)
    public static double calcularPenalizacionDistancia(String edif1, String edif2) {
        if (edif1 == null || edif2 == null) return 0.0;

        int grupo1 = getGrupoEdificio(edif1);
        int grupo2 = getGrupoEdificio(edif2);

        if (grupo1 == grupo2) return 0.0;

        // A(1) <-> B(2): 0.5
        if ((grupo1 == 1 && grupo2 == 2) || (grupo1 == 2 && grupo2 == 1)) return 0.5;

        // B(2) <-> C(3): 1.5
        if ((grupo1 == 2 && grupo2 == 3) || (grupo1 == 3 && grupo2 == 2)) return 1.5;

        // A(1) <-> C(3): 2.0
        if ((grupo1 == 1 && grupo2 == 3) || (grupo1 == 3 && grupo2 == 1)) return 2.0;

        return 0.0;
    }

    private static int getGrupoEdificio(String edif) {
        String e = edif.toLowerCase().trim();
        if (e.equals("e17") || e.equals("e18")) return 1;
        if (e.equals("e19")) return 2;
        if (e.equals("e22")) return 3;
        return 0; // desconocido, sin penalizacion
    }
}