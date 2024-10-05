package io.github.chyohn.terse.anotations;

import java.lang.annotation.*;

/**
 * indicate the type is internal API, and not export to external application
 * <p>
 * the external application not allowed to use the internal API directly.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Internal {
}
