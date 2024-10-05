package io.github.chyohn.terse.anotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * indicate the external application can extend
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface External {
}
