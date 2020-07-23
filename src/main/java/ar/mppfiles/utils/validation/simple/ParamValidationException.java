package ar.mppfiles.utils.validation.simple;

import java.util.Map;

/**
 * Excepción que lanzan los ParamValidators. La excepción contendrá la instancia 
 * que la originó, útil si tenemos varias instancias en un mismo proceso.
 *
 * @author mppfiles
 */
public class ParamValidationException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 1799015119643836681L;
    protected ParamValidator params;
    
    /**
     * Constructor simple, para lanzar excepciones aisladas.
     * @param mensaje 
     */
    public ParamValidationException(String mensaje) {
        this.params = new ParamValidator();
        params.setError("global", mensaje);
    }

    /**
     * Constructor desde una instancia existente.
     * @param params 
     */
    public ParamValidationException(ParamValidator params) {
        this.params = params;
    }

    /**
     * Obtiene la instancia involucrada en el error de validación.
     * @return 
     */
    public ParamValidator getParams() {
        return params;
    }
    
    /**
     * Método abreviado para obtener los errores de la instancia.
     * @return 
     */
    public Map<String, String> getErrores() {
        return params.getErrores();
    }

}
