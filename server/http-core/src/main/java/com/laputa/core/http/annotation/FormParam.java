package com.laputa.core.http.annotation;

import java.lang.annotation.*;

/**
 * Binds the value(s) of a form parameter contained within a request entity body
 * to a resource classMethod parameter. Values are URL decoded unless this is
 * disabled using the  annotation. A default value can be
 * specified using the  annotation.
 * If the request entity body is absent or is an unsupported media type, the
 * default value is used.
 *
 * The type {@code T} of the annotated parameter must either:
 * <ol>
 * <li>Be a primitive type</li>
 * <li>Have a constructor that accepts a single {@code String} argument</li>
 * <li>Have a static classMethod named {@code valueOf} or {@code fromString}
 * that accepts a single</li>
 * <li>Have a registered implementation of {@link javax.ws.rs.ext.ParamConverterProvider}
 * JAX-RS extension SPI that returns a {@link javax.ws.rs.ext.ParamConverter}
 * instance capable of a "from string" conversion for the type.</li>
 * {@code String} argument (see, for example, {@link Integer#valueOf(String)})</li>
 * <li>Be {@code List<T>}, {@code Set<T>} or
 * {@code SortedSet<T>}, where {@code T} satisfies 2, 3 or 4 above.
 * The resulting collection is read-only.</li>
 * </ol>
 *
 * <p>If the type is not one of the collection types listed in 5 above and the
 * form parameter is represented by multiple values then the first value (lexically)
 * of the parameter is used.</p>
 *
 * <p>Note that, whilst the annotation target permits use on fields and methods,
 * this annotation is only required to be supported on resource classMethod
 * parameters.</p>
 *
 * @author Paul Sandoz
 * @author Marc Hadley
 * @since 1.0
 */
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FormParam {

    /**
     * Defines the name of the form parameter whose value will be used
     * to initialize the value of the annotated classMethod argument. The name is
     * specified in decoded form, any percent encoded literals within the value
     * will not be decoded and will instead be treated as literal text. E.g. if
     * the parameter name is "a b" then the value of the annotation is "a b",
     * <i>not</i> "a+b" or "a%20b".
     */
    String value();
}
