package app.controller.gestion;

/**
 * Utilidad para traducir pisos alfanuméricos (PB, M, P1, P2...)
 * a valores numéricos que permiten un ordenamiento lógico.
 */
public final class PisoUtils {

    private PisoUtils() {
        // Clase utilitaria, no instanciable
    }

    /**
     * Traduce un piso a un valor numérico para ordenamiento.
     * PB=0, M=1, P1=2, P2=3, etc. Desconocido=99.
     */
    public static int obtenerValorPiso(String piso) {
        if (piso == null) return 99;
        String p = piso.toUpperCase().trim();
        if (p.equals("PB")) return 0;
        if (p.equals("M")) return 1;
        if (p.startsWith("P")) {
            try {
                return Integer.parseInt(p.substring(1)) + 1;
            } catch (NumberFormatException e) {
                // Piso no parseable, se trata como desconocido
            }
        }
        return 99;
    }
}
