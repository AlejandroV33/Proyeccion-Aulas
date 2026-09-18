package app.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import java.util.Optional;

/**
 * Utilidad estandarizada para mostrar alertas y notificaciones en la interfaz grafica.
 * Asegura que todas las alertas se ejecuten siempre en el hilo de JavaFX (JavaFX Application Thread).
 */
public class AlertUtil {

    private AlertUtil() {
        // Constructor privado para ocultar la instanciacion implicita
    }

    /**
     * Muestra un mensaje de informacion al usuario.
     */
    public static void mostrarInfo(String mensaje) {
        ejecutarEnFXThread(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Informacion");
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.showAndWait();
        });
    }

    /**
     * Muestra un mensaje de error critico o del sistema.
     */
    public static void mostrarError(String mensaje) {
        ejecutarEnFXThread(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error de Sistema");
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.showAndWait();
        });
    }

    /**
     * Muestra un mensaje de advertencia.
     */
    public static void mostrarAdvertencia(String mensaje) {
        ejecutarEnFXThread(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Advertencia");
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.showAndWait();
        });
    }

    /**
     * Pide confirmacion al usuario mediante un dialogo SI/NO.
     * Importante: Al depender de showAndWait(), este metodo bloquea el hilo de UI.
     * No debe ser llamado desde un hilo en segundo plano que requiera respuesta sincrona
     * sin el manejo adecuado de concurrencia.
     *
     * @return true si el usuario acepto (YES/OK).
     */
    public static boolean pedirConfirmacion(String titulo, String mensaje) {
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException("pedirConfirmacion() debe ser llamado desde el JavaFX Application Thread.");
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, mensaje, ButtonType.YES, ButtonType.NO);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        Optional<ButtonType> resultado = alert.showAndWait();
        return resultado.isPresent() && resultado.get() == ButtonType.YES;
    }
    
    /**
     * Pide confirmacion al usuario (OK/CANCEL) estándar.
     */
    public static boolean pedirConfirmacionOkCancel(String titulo, String mensaje) {
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException("pedirConfirmacionOkCancel() debe ser llamado desde el JavaFX Application Thread.");
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, mensaje, ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        Optional<ButtonType> resultado = alert.showAndWait();
        return resultado.isPresent() && resultado.get() == ButtonType.OK;
    }

    /**
     * Wrapper de seguridad para asegurar la ejecucion de la alerta en el hilo de UI.
     */
    private static void ejecutarEnFXThread(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }
}
