package ar.mppfiles.utils.validation.simple;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Clase utilitaria para validar parámetros desde un Map. Útil, por ejemplo,
 * para parámetros del request.
 *
 * @author mppfiles
 */
public class ParamValidator {

    protected Map params;
    protected Map errores = new HashMap();
    private String campoActual = null;
    private boolean tieneMensaje = false;
    private boolean fueProcesado = false;
    final Logger logger = LoggerFactory.getLogger(ParamValidator.class);

    /**
     * Inicializa la clase desde una matriz de parámetros. Por ejemplo, para
     * usar con request.getParameterMap(). La idea es reducir un
     * Map&lt;String,String[]&gt; a Map&lt;String,String&gt;.
     *
     * @param <T>
     * @param input
     * @return
     */
    public <T extends ParamValidator> T fromMapArray(Map<String, String[]> input) {
        Map<String, Object> firstParams = new HashMap();
        input.entrySet().forEach((entry) -> {
            String[] values = entry.getValue();
            if (!(values == null || values.length == 0)) {
                firstParams.put(entry.getKey(), entry.getValue()[0]);
            }
        });

        return (T) init(firstParams);
    }

    /**
     * Inicializa la clase desde un Map (clave/valor).
     *
     * @param <T>
     * @param params
     * @return
     */
    public <T extends ParamValidator> T fromMap(Map params) {
        return (T) init(params);
    }

    public ParamValidator() {
    }

    /**
     * Método interno de inicialización.
     *
     * @param <T>
     * @param params
     * @return
     */
    protected final <T extends ParamValidator> T init(Map params) {
        this.params = params;
        clean();
        this.params = new HashMap(params);

        return (T) this;
    }

    /**
     * Elimina los parámetros vacíos, asumiendo que nunca fueron enviados.
     * Facilita los chequeos de null y/o cadenas vacías.
     */
    protected void clean() {
        if (params == null || params.isEmpty()) {
            return;
        }

        params.values().removeAll(Collections.singleton(""));
    }

    /**
     * Parámetros recibidos/convertidos.
     *
     * @return
     */
    public Map getParams() {
        return params;
    }

    /**
     * Errores generados.
     *
     * @return
     */
    public Map getErrores() {
        return errores;
    }

    /**
     * Agrega/actualiza el valor de un parámetro.
     *
     * @param param_name
     * @param val
     * @return 
     */
    public ParamValidator set(String param_name, Object val) {
        params.put(param_name, val);
        return this;
    }

    /**
     * Elimina uno o más parámetros.
     *
     * @param param_names
     * @return
     */
    public ParamValidator remove(String... param_names) {
        Arrays.asList(param_names);

        for (String name : param_names) {
            params.remove(name);
        }

        return this;
    }

    /**
     * Agrega/actualiza un mensaje de error, asociado a un parámetro.
     *
     * @param param_name
     * @param message
     * @return
     */
    public ParamValidator setError(String param_name, String message) {
	fueProcesado = true;
        errores.put(param_name, message);
        return this;
    }

    /**
     * Obtiene el mensaje de error asociado a un parámetro.
     *
     * @param param_name
     * @return
     */
    public String getError(String param_name) {
        return (String) errores.get(param_name);
    }
    
    /**
     * Devuelve si el parámetro especificado contiene algún error.
     * @param param_name
     * @return 
     */
    public boolean tieneError(String param_name) {
        return errores.containsKey(param_name);
    }

    /**
     * Ejecuta las validaciones definidas en una clase de validación. No lanza
     * excepciones de validación, sólo setea los errores.
     *
     * @param <T>
     * @return
     * @throws ar.mppfiles.utils.validation.simple.ParamValidationException
     */
    public <T extends ParamValidator> T validate() throws ParamValidationException {
        try {
            doValidate();
        } catch (ParamValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            if (!(ex instanceof ParamValidationException)) {
                setError("global", "Ha ocurrido un error general. Por favor reintente más tarde.");
                logger.error("Error al validar: ", ex);

                throw new ParamValidationException(this);
            }
        }

        

        return (T) this;
    }

    /**
     * Devuelve el estado de las validaciones.
     *
     * @return
     * @throws ar.mppfiles.utils.validation.simple.ParamValidationException
     */
    public boolean isOK() throws ParamValidationException {
        if (!fueProcesado) {
            throw new ParamValidationException("No se ejecutó ninguna validación.");
        }

        return errores.isEmpty();
    }

    /**
     * Obtiene un parámetro como Object.
     *
     * @param param_name
     * @return
     */
    public Object get(String param_name) {
        return params.get(param_name);
    }

    /**
     * Obtiene un parámetro como Boolean.
     *
     * @param param_name
     * @return
     */
    public Boolean getBoolean(String param_name) {
        return isEmpty(param_name) ? null : Boolean.valueOf(getString(param_name).toLowerCase(Locale.getDefault())) || (!"false".equals(getString(param_name).toLowerCase(Locale.getDefault())) && getInteger(param_name) == 1);
    }

    /**
     * Obtiene un parámetro como String.
     *
     * @param param_name
     * @return
     */
    public String getString(String param_name) {
        return isEmpty(param_name) ? null : get(param_name).toString();
    }

    /**
     * Obtiene un parámetro comon Integer.
     *
     * @param param_name
     * @return
     */
    public Integer getInteger(String param_name) {
        return isEmpty(param_name) ? null : Integer.valueOf(getString(param_name));
    }

    /**
     * Obtiene un parámetro como Date, parseándolo desde formato ISO
     * (yyyy-mm-dd).
     *
     * @param param_name
     * @return
     */
    public Date getDate(String param_name) {
        if (isEmpty(param_name)) {
            return null;
        }

        // Si el valor ya fue convertido a Date, se devuelve tal cual, 
        // para evitar doble parseo.
        if (get(param_name) instanceof Date) {
            return (Date) get(param_name);
        }

        Date theDate = null;
        try {
            theDate = new SimpleDateFormat("yyyy-MM-dd").parse(getString(param_name));
        } catch (ParseException ex) {
            setError(param_name, "El parámetro '" + param_name + "' no es una fecha válida.");
        }

        return theDate;
    }

    /**
     * Obtiene un parámetro como Date, tratando de castearlo a Date (formato
     * hh:mm:ss).
     *
     * @param param_name
     * @return
     */
    public Date getTime(String param_name) {
        if (isEmpty(param_name)) {
            return null;
        }

        // Si el valor ya fue convertido a Date, se devuelve tal cual, 
        // para evitar doble parseo.
        if (get(param_name) instanceof Date) {
            return (Date) get(param_name);
        }

        //si el dato viene en formato HH:mm solamente, agrego los segundos
        String raw = getString(param_name);
        if (raw.length() == 5) {
            raw += ":00";
        }

        Date hora = null;

        try {
            hora = new SimpleDateFormat("HH:mm:ss").parse(raw);
        } catch (ParseException ex) {
            setError(campoActual, "El parámetro '" + campoActual + "' no es una hora válida.");
        }
        return hora;
    }

    /**
     * Obtiene un parámetro como BigDecimal.
     *
     * @param param_name
     * @return
     */
    public BigDecimal getBigDecimal(String param_name) {
        return isEmpty(param_name) ? null : new BigDecimal(getString(param_name));
    }

    /**
     * Comprueba que el parámetro NO exista, o bien que no tenga un valor (no es
     * una validación).
     *
     * @param param_name
     * @return
     */
    public boolean isEmpty(String param_name) {
        return get(param_name) == null || get(param_name).toString().isEmpty();
    }

    /**
     * Comprueba que el parámetro SI exista, y que TENGA un valor (no es una
     * validación).
     *
     * @param param_name
     * @return
     */
    public boolean tieneValor(String param_name) {
        return !isEmpty(param_name);
    }

    /**
     * Selecciona un parámetro para aplicarle validaciones sucesivas.
     *
     * @param param_name
     * @return
     */
    public ParamValidator con(String param_name) {
        campoActual = param_name;
        tieneMensaje = false;
        return this;
    }

    /**
     * Valida que el parámetro esté presente y tenga un valor definido.
     *
     * @return
     */
    public ParamValidator requerido() {
        fueProcesado = true;

        if (errores.get(campoActual) != null) {
            return this;
        }
        if (isEmpty(campoActual)) {
            setError(campoActual, "Debe ingresar un valor para '" + campoActual + "'");
        }

        return this;
    }

    /**
     * Valida que el valor del parámetro no tenga espacios.
     *
     * @return
     */
    public ParamValidator sinEspacios() {
        return cumpleRegex("[^\\s]+");
    }

    /**
     * Valida que el valor del parámetro no tenga números.
     *
     * @return
     */
    public ParamValidator sinNumeros() {
        return cumpleRegex("[^\\d]+");
    }

    /**
     * Valida que el valor tenga letras, espacios o números, incluyendo acentos,
     * eñes y apóstrofe. No incluye ningún otro signo de puntuación.
     *
     * @return
     */
    public ParamValidator letrasConEspaciosYNumeros() {
        return cumpleRegex("[0-9A-Za-zñÑáéíóúÁÉÍÓÚàèìòùÀÈÌÒÙäëïöüÄËÏÖÜ' ]+");
    }
    
    /**
     * Valida que el valor tenga letras o números, incluyendo acentos,
     * eñes y apóstrofe. No incluye ningún otro signo de puntuación.
     *
     * @return
     */
    public ParamValidator letrasConNumeros() {
        return cumpleRegex("[0-9A-Za-zñÑáéíóúÁÉÍÓÚàèìòùÀÈÌÒÙäëïöüÄËÏÖÜ']+");
    }

    /**
     * Valida que el valor tenga letras o espacios, teniend en cuenta acentos,
     * eñes y apóstrofe. No incluye números, ni signos de puntuación.
     *
     * @return
     */
    public ParamValidator letrasConEspacios() {
        return cumpleRegex("[A-Za-zñÑáéíóúÁÉÍÓÚàèìòùÀÈÌÒÙäëïöüÄËÏÖÜ' ]+");
    }

    /**
     * Valida que el valor tenga únicamente letras, incluyendo acentos y eñes.
     *
     * @return
     */
    public ParamValidator letras() {
        return cumpleRegex("[A-Za-zñÑáéíóúÁÉÍÓÚàèìòùÀÈÌÒÙäëïöüÄËÏÖÜ]+");
    }

    /**
     * Valida que el valor no tenga caracteres "problemáticos" como los signos
     * mayor/menor que, comillas, apóstrofe, etc.
     *
     * @return
     */
    public ParamValidator texto() {
        return cumpleRegex("[^<>\"']+");
    }

    /**
     * Valida que el valor cumpla con el formato de la expresión regular.
     *
     * @param pattern
     * @return
     */
    public ParamValidator cumpleRegex(String pattern) {
        fueProcesado = true;

        if (errores.get(campoActual) != null) {
            return this;
        }

        if (isEmpty(campoActual)) {
            return this;
        }

        pattern = "^" + pattern + "$";

        if (!Pattern.matches(pattern, getString(campoActual))) {
            setError(campoActual, "El valor de '" + campoActual + "' contiene caracteres no válidos");
        }

        return this;
    }

    /**
     * Valida que el parámetro tenga un valor numérico entero. Si es así, se
     * actualiza el valor casteado.
     *
     * @return
     */
    public ParamValidator entero() {
        fueProcesado = true;

        if (errores.get(campoActual) != null) {
            return this;
        }

        try {
            params.put(campoActual, getInteger(campoActual));
        } catch (NumberFormatException e) {
            setError(campoActual, "El valor de '" + campoActual + "' no es un número válido");
        }

        return this;

    }

    /**
     * Valida que el parámetro tenga un valor numérico decimal. Si es así, se
     * actualiza el valor casteado.
     *
     * @return
     */
    public ParamValidator decimal() {
        fueProcesado = true;

        if (errores.get(campoActual) != null) {
            return this;
        }

        try {
            params.put(campoActual, getBigDecimal(campoActual));
        } catch (NumberFormatException e) {
            setError(campoActual, "El valor de '" + campoActual + "' no es un número válido");
        }

        return this;
    }

    /**
     * Valida que el parámetro tenga un valor de fecha (Date). Si es así, se
     * actualiza el valor casteado.
     *
     * @see getDate
     * @return
     */
    public ParamValidator fecha() {
        fueProcesado = true;

        if (errores.get(campoActual) != null) {
            return this;
        }

        params.put(campoActual, getDate(campoActual));

        return this;
    }

    /**
     * Valida que el parámetro tenga un valor de hora (sigue siendo Date). Si es
     * así, se actualiza el valor casteado.
     *
     * @see getTime
     * @return
     */
    public ParamValidator hora() {
        fueProcesado = true;

        if (errores.get(campoActual) != null) {
            return this;
        }

        params.put(campoActual, getTime(campoActual));

        return this;
    }

    /**
     * Asigna un valor a un parámetro inexistente o vacío.
     *
     * @param val
     * @return
     */
    public ParamValidator valorPorDefecto(Object val) {
        if (errores.get(campoActual) != null) {
            return this;
        }
        if (isEmpty(campoActual)) {
            params.put(campoActual, val);
        }

        return this;
    }

    /**
     * Valida que el parámetro sea igual al valor especificado.
     *
     * @param val
     * @return
     */
    public ParamValidator igualA(Object val) {
        fueProcesado = true;

        if (errores.get(campoActual) != null || get(campoActual) == null) {
            return this;
        }

        if (!this.getString(campoActual).equals(val.toString())) {
            setError(campoActual, "El valor de '" + campoActual + "' debería ser igual a '" + val.toString() + "'");
        }

        return this;
    }

    /**
     * Valida que el parámetro sea distinto del valor especificado.
     *
     * @param val
     * @return
     */
    public ParamValidator distintoDe(Object val) {
        fueProcesado = true;

        if (errores.get(campoActual) != null || val == null || get(campoActual) == null) {
            return this;
        }

        if (this.getString(campoActual).equals(val.toString())) {
            setError(campoActual, "El valor de '" + campoActual + "' debería ser distinto de '" + val.toString() + "'");
        }

        return this;
    }

    /**
     * Valida que la longitud del parámetro (como cadena) no sea inferior al
     * número especificado.
     *
     * @param v2
     * @return
     */
    public ParamValidator minLargo(Integer v2) {
        fueProcesado = true;

        if (errores.get(campoActual) != null || v2 == null || get(campoActual) == null) {
            return this;
        }
        if (getString(campoActual).length() < v2) {
            setError(campoActual, "El valor de '" + campoActual + "' debe ser de al menos " + v2 + " caracteres");
        }

        return this;
    }

    /**
     * Valida que la longitud del parámetro (como cadena) no sea superior al
     * número especificado.
     *
     * @param v2
     * @return
     */
    public ParamValidator maxLargo(Integer v2) {
        fueProcesado = true;

        if (errores.get(campoActual) != null || v2 == null || get(campoActual) == null) {
            return this;
        }
        if (getString(campoActual).length() > v2) {
            setError(campoActual, "El valor de '" + campoActual + "' debe ser de " + v2 + " caracteres como máximo");
        }

        return this;
    }
    
    /**
     * Valida que el largo del campo sea exactamente el especificado.
     *
     * @param v2
     * @return
     */
    public ParamValidator largo(Integer v2) {
        fueProcesado = true;

        if (errores.get(campoActual) != null || v2 == null || get(campoActual) == null) {
            return this;
        }
        if (getString(campoActual).length() != v2) {
            setError(campoActual, "El valor de '" + campoActual + "' debe ser de " + v2 + " caracteres de largo");
        }

        return this;
    }


    /**
     * Valida que el valor del parámetro no sea menor al número especificado (Integer).
     *
     * @param v2
     * @return
     */
    public ParamValidator min(Integer v2) {
        fueProcesado = true;

        if (errores.get(campoActual) != null || v2 == null || get(campoActual) == null) {
            return this;
        }
        try {
            Integer v1 = getInteger(campoActual);
            if (v1 < v2) {
                setError(campoActual, "El valor de '" + campoActual + "' no puede ser menor que " + v2);
            }
        } catch (NumberFormatException ex) {
            logger.error("Error al validar: ", ex);
            setError(campoActual, "Ocurrió un error al validar el parámetro '" + campoActual + "'");
        }

        return this;
    }

    /**
     * Valida que el valor del parámetro no exceda al número especificado (BigDecimal).
     *
     * @param v2
     * @return
     */
    public ParamValidator max(Integer v2) {
        fueProcesado = true;

        if (errores.get(campoActual) != null || v2 == null || get(campoActual) == null) {
            return this;
        }
        try {
            Integer v1 = getInteger(campoActual);
            if (v1 > v2) {
                setError(campoActual, "El valor de '" + campoActual + "' no puede ser mayor que " + v2);
            }
        } catch (NumberFormatException ex) {
            logger.error("Error al validar: ", ex);
            setError(campoActual, "Ocurrió un error al validar el parámetro '" + campoActual + "'");
        }

        return this;
    }
    
    /**
     * Valida que el valor del parámetro no sea menor al número especificado (BigDecimal).
     *
     * @param v2
     * @return
     */
    public ParamValidator min(BigDecimal v2) {
        fueProcesado = true;

        if (errores.get(campoActual) != null || v2 == null || get(campoActual) == null) {
            return this;
        }
        try {
            BigDecimal v1 = getBigDecimal(campoActual);
            if (v1.compareTo(v2) < 0) {
                setError(campoActual, "El valor de '" + campoActual + "' no puede ser menor que " + v2);
            }
        } catch (NumberFormatException ex) {
            logger.error("Error al validar: ", ex);
            setError(campoActual, "Ocurrió un error al validar el parámetro '" + campoActual + "'");
        }

        return this;
    }

    /**
     * Valida que el valor del parámetro no exceda al número especificado (Integer).
     *
     * @param v2
     * @return
     */
    public ParamValidator max(BigDecimal v2) {
        fueProcesado = true;

        if (errores.get(campoActual) != null || v2 == null || get(campoActual) == null) {
            return this;
        }
        try {
            BigDecimal v1 = getBigDecimal(campoActual);
            if (v1.compareTo(v2) > 0) {
                setError(campoActual, "El valor de '" + campoActual + "' no puede ser mayor que " + v2);
            }
        } catch (NumberFormatException ex) {
            logger.error("Error al validar: ", ex);
            setError(campoActual, "Ocurrió un error al validar el parámetro '" + campoActual + "'");
        }

        return this;
    }

    /**
     * Valida que la fecha del parámetro no sea anterior a la especificada.
     *
     * @param v2
     * @return
     */
    public ParamValidator min(Date v2) {
        fueProcesado = true;

        if (errores.get(campoActual) != null || v2 == null || get(campoActual) == null) {
            return this;
        }
        try {
            Date v1 = getDate(campoActual);

            if (v1.before(v2)) {
                setError(campoActual, "El valor de '" + campoActual + "' no puede ser anterior a " + new SimpleDateFormat("dd/MM/yyyy - HH:mm").format(v2) + "");
            }
        } catch (Exception ex) {
            logger.error("Error al validar: ", ex);
            setError(campoActual, "Ocurrió un error al validar el parámetro '" + campoActual + "'");
        }

        return this;
    }

    /**
     * Valida que la fecha del parámetro no sea posterior a la especificada.
     *
     * @param v2
     * @return
     */
    public ParamValidator max(Date v2) {
        fueProcesado = true;

        if (errores.get(campoActual) != null || v2 == null || get(campoActual) == null) {
            return this;
        }
        try {
            Date v1 = getDate(campoActual);

            if (v1.after(v2)) {
                setError(campoActual, "El valor de '" + campoActual + "' no puede ser posterior a " + new SimpleDateFormat("dd/MM/yyyy - HH:mm").format(v2) + "");
            }
        } catch (Exception ex) {
            logger.error("Error al validar: ", ex);
            setError(campoActual, "Ocurrió un error al validar el parámetro '" + campoActual + "'");
        }

        return this;
    }

    /**
     * Valida que la expresión 'exista' (ej. no sea nula)
     *
     * @param obj
     * @return
     */
    public ParamValidator existente(Object obj) {
        fueProcesado = true;

        if (errores.get(campoActual) != null || get(campoActual) == null) {
            return this;
        }

        if (obj == null) {
            setError(campoActual, "No se encontró un valor válido para " + campoActual);
        }

        return this;
    }

    /**
     * Valida que la expresión no 'exista' (tiene que ser nula)
     *
     * @param obj
     * @return
     */
    public ParamValidator inexistente(Object obj) {
        fueProcesado = true;

        if (errores.get(campoActual) != null || get(campoActual) == null) {
            return this;
        }

        if (obj != null) {
            setError(campoActual, "Se encontró un valor no esperado para " + campoActual);
        }

        return this;
    }
    
    /**
     * Valida que la expresión booleana sea verdadera.
     * @param condicion
     * @return 
     */
    public ParamValidator cumpleCondicion(boolean condicion) {
        fueProcesado = true;

        if (errores.get(campoActual) != null || get(campoActual) == null) {
            return this;
        }

        if (false == condicion) {
            setError(campoActual, "No se cumple la condición requerida para " + campoActual);
        }

        return this;
    }

    /**
     * Reescribe el mensaje de error (si hubiere) para el parámetro a validar.
     *
     * @param msg
     * @param params
     * @return
     */
    public ParamValidator conMensaje(String msg, Object... params) {
        if (errores.get(campoActual) != null && !tieneMensaje) {
            setError(campoActual, String.format(msg, params));
            tieneMensaje = true;
        }

        return this;
    }

    /**
     * Comprueba el estado de las validaciones, de ser necesario, lanzando una
     * excepción para controlar el flujo del programa. Puede usarse varias veces
     * durante el proceso de validación, para controlar en qué momento se hacen
     * los "cortes".
     *
     * @return
     * @throws ar.mppfiles.utils.validation.simple.ParamValidationException
     */
    public ParamValidator check() throws ParamValidationException {
        if (!isOK()) {
            throw new ParamValidationException(this);
        }
        return this;
    }

    /**
     * Define las validaciones aplicadas en la clase.
     *
     * @throws Exception
     */
    protected void doValidate() throws Exception {
    }
}
