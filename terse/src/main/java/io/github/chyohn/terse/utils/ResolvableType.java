/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.chyohn.terse.utils;


import io.github.chyohn.terse.anotations.Internal;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.StringJoiner;


/**
 * Encapsulates a Java {@link Type}, providing access to
 * {@link #getSuperType() supertypes}, {@link #getInterfaces() interfaces}, and
 * {@link #getGeneric(int...) generic parameters} along with the ability to ultimately
 * {@link #resolve() resolve} to a {@link Class}.
 *
 * <pre class="code">
 * private HashMap&lt;Integer, List&lt;String&gt;&gt; myMap;
 *
 * public void example() {
 *     ResolvableType t = ResolvableType.forType(myMap.getClass());
 *     t.getSuperType(); // AbstractMap&lt;Integer, List&lt;String&gt;&gt;
 *     t.asMap(); // Map&lt;Integer, List&lt;String&gt;&gt;
 *     t.getGeneric(0).resolve(); // Integer
 *     t.getGeneric(1).resolve(); // List
 *     t.getGeneric(1); // List&lt;String&gt;
 *     t.resolveGeneric(1, 0); // String
 * }
 * </pre>
 *
 * @see #forClass(Class)
 * @see #forType(Type)
 * @see #forInstance(Object)
 */
@Internal
@SuppressWarnings("serial")
public class ResolvableType implements Serializable {

    /**
     * {@code ResolvableType} returned when no value is available. {@code NONE} is used
     * in preference to {@code null} so that multiple method calls can be safely chained.
     */
    public static final ResolvableType NONE = new ResolvableType(EmptyType.INSTANCE, null, 0);

    private static final ResolvableType[] EMPTY_TYPES_ARRAY = new ResolvableType[0];


    /**
     * The underlying Java type being managed.
     */
    private final Type type;


    /**
     * The {@code VariableResolver} to use or {@code null} if no resolver is available.
     */
    private final VariableResolver variableResolver;

    /**
     * The component type for an array or {@code null} if the type should be deduced.
     */
    private final ResolvableType componentType;

    private final Integer hash;

    private Class<?> resolved;

    private volatile ResolvableType superType;

    private volatile ResolvableType[] interfaces;

    private volatile ResolvableType[] generics;


    /**
     * Private constructor used to create a new {@link ResolvableType} for cache key purposes,
     * with no upfront resolution.
     */
    private ResolvableType(
            Type type, VariableResolver variableResolver) {

        this.type = type;
        this.variableResolver = variableResolver;
        this.componentType = null;
        this.hash = calculateHashCode();
        this.resolved = null;
    }

    /**
     * Private constructor used to create a new {@link ResolvableType} for cache value purposes,
     * with upfront resolution and a pre-calculated hash.
     *
     * @since 4.2
     */
    private ResolvableType(Type type,
                           VariableResolver variableResolver, Integer hash) {
        this.type = type;
        this.variableResolver = variableResolver;
        this.componentType = null;
        this.hash = hash;
        this.resolved = resolveClass();
    }

    /**
     * Private constructor used to create a new {@link ResolvableType} for uncached purposes,
     * with upfront resolution but lazily calculated hash.
     */
    private ResolvableType(Type type,
                           VariableResolver variableResolver, ResolvableType componentType) {

        this.type = type;
        this.variableResolver = variableResolver;
        this.componentType = componentType;
        this.hash = null;
        this.resolved = resolveClass();
    }

    /**
     * Private constructor used to create a new {@link ResolvableType} on a {@link Class} basis.
     * <p>Avoids all {@code instanceof} checks in order to create a straight {@link Class} wrapper.
     *
     * @since 4.2
     */
    private ResolvableType(Class<?> clazz) {
        this.resolved = (clazz != null ? clazz : Object.class);
        this.type = this.resolved;
        this.variableResolver = null;
        this.componentType = null;
        this.hash = null;
    }


    /**
     * Return the underling Java {@link Type} being managed.
     *
     * @return type
     */
    public Type getType() {
        return this.type;
    }

    /**
     * Return the underlying Java {@link Class} being managed, if available;
     * otherwise {@code null}.
     *
     * @return class object
     */
    public Class<?> getRawClass() {
        if (this.type == this.resolved) {
            return this.resolved;
        }
        Type rawType = this.type;
        if (rawType instanceof ParameterizedType) {
            rawType = ((ParameterizedType) rawType).getRawType();
        }
        return (rawType instanceof Class ? (Class<?>) rawType : null);
    }

    /**
     * Return the underlying source of the resolvable type. Will return a {@link Field},
     * or {@link Type} depending on how the {@link ResolvableType}
     * was constructed. This method is primarily to provide access to additional type
     * information or meta-data that alternative JVM languages may provide.
     *
     * @return type
     */
    public Object getSource() {
        return this.type;
    }

    /**
     * Return this type as a resolved {@code Class}, falling back to
     * {@link Object} if no specific class can be resolved.
     *
     * @return the resolved {@link Class} or the {@code Object} fallback
     * @see #getRawClass()
     * @see #resolve(Class)
     * @since 5.1
     */
    public Class<?> toClass() {
        return resolve(Object.class);
    }


    /**
     * Determine whether this {@code ResolvableType} is assignable from the
     * specified other type.
     * <p>Attempts to follow the same rules as the Java compiler, considering
     * whether both the {@link #resolve() resolved} {@code Class} is
     * {@link Class#isAssignableFrom(Class) assignable from} the given type
     * as well as whether all {@link #getGenerics() generics} are assignable.
     *
     * @param other the type to be checked against (as a {@code ResolvableType})
     * @return {@code true} if the specified other type can be assigned to this
     * {@code ResolvableType}; {@code false} otherwise
     */
    public boolean isAssignableFrom(ResolvableType other) {
        return isAssignableFrom(other, null);
    }

    private boolean isAssignableFrom(ResolvableType other, Map<Type, Type> matchedBefore) {

        // If we cannot resolve types, we are not assignable
        if (this == NONE || other == NONE) {
            return false;
        }

        // Deal with array by delegating to the component type
        if (isArray()) {
            return (other.isArray() && getComponentType().isAssignableFrom(other.getComponentType()));
        }

        if (matchedBefore != null && matchedBefore.get(this.type) == other.type) {
            return true;
        }

        // Deal with wildcard bounds
        WildcardBounds ourBounds = WildcardBounds.get(this);
        WildcardBounds typeBounds = WildcardBounds.get(other);

        // In the form X is assignable to <? extends Number>
        if (typeBounds != null) {
            return (ourBounds != null && ourBounds.isSameKind(typeBounds) &&
                    ourBounds.isAssignableFrom(typeBounds.getBounds()));
        }

        // In the form <? extends Number> is assignable to X...
        if (ourBounds != null) {
            return ourBounds.isAssignableFrom(other);
        }

        // Main assignability check about to follow
        boolean exactMatch = (matchedBefore != null);  // We're checking nested generic variables now...
        boolean checkGenerics = true;
        Class<?> ourResolved = null;
        if (this.type instanceof TypeVariable) {
            TypeVariable<?> variable = (TypeVariable<?>) this.type;
            // Try default variable resolution
            if (this.variableResolver != null) {
                ResolvableType resolved = this.variableResolver.resolveVariable(variable);
                if (resolved != null) {
                    ourResolved = resolved.resolve();
                }
            }
            if (ourResolved == null) {
                // Try variable resolution against target type
                if (other.variableResolver != null) {
                    ResolvableType resolved = other.variableResolver.resolveVariable(variable);
                    if (resolved != null) {
                        ourResolved = resolved.resolve();
                        checkGenerics = false;
                    }
                }
            }
            if (ourResolved == null) {
                // Unresolved type variable, potentially nested -> never insist on exact match
                exactMatch = false;
            }
        }
        if (ourResolved == null) {
            ourResolved = resolve(Object.class);
        }
        Class<?> otherResolved = other.toClass();

        // We need an exact type match for generics
        // List<CharSequence> is not assignable from List<String>
        if (exactMatch ? !ourResolved.equals(otherResolved) : !ourResolved.isAssignableFrom(otherResolved)) {
            return false;
        }

        if (checkGenerics) {
            // Recursively check each generic
            ResolvableType[] ourGenerics = getGenerics();
            ResolvableType[] typeGenerics = other.as(ourResolved).getGenerics();
            if (ourGenerics.length != typeGenerics.length) {
                return false;
            }
            if (matchedBefore == null) {
                matchedBefore = new IdentityHashMap<>(1);
            }
            matchedBefore.put(this.type, other.type);
            for (int i = 0; i < ourGenerics.length; i++) {
                if (!ourGenerics[i].isAssignableFrom(typeGenerics[i], matchedBefore)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * @return {@code true} if this type resolves to a Class that represents an array.
     * @see #getComponentType()
     */
    public boolean isArray() {
        if (this == NONE) {
            return false;
        }
        return ((this.type instanceof Class && ((Class<?>) this.type).isArray()) ||
                this.type instanceof GenericArrayType || resolveType().isArray());
    }

    /**
     * @return the ResolvableType representing the component type of the array or
     * {@link #NONE} if this type does not represent an array.
     * @see #isArray()
     */
    public ResolvableType getComponentType() {
        if (this == NONE) {
            return NONE;
        }
        if (this.componentType != null) {
            return this.componentType;
        }
        if (this.type instanceof Class) {
            Class<?> componentType = ((Class<?>) this.type).getComponentType();
            return forType(componentType, this.variableResolver);
        }
        if (this.type instanceof GenericArrayType) {
            return forType(((GenericArrayType) this.type).getGenericComponentType(), this.variableResolver);
        }
        return resolveType().getComponentType();
    }


    /**
     * Return this type as a {@link ResolvableType} of the specified class. Searches
     * {@link #getSuperType() supertype} and {@link #getInterfaces() interface}
     * hierarchies to find a match, returning {@link #NONE} if this type does not
     * implement or extend the specified class.
     *
     * @param type the required type (typically narrowed)
     * @return a {@link ResolvableType} representing this object as the specified
     * type, or {@link #NONE} if not resolvable as that type
     * @see #getSuperType()
     * @see #getInterfaces()
     */
    public ResolvableType as(Class<?> type) {
        if (this == NONE) {
            return NONE;
        }
        Class<?> resolved = resolve();
        if (resolved == null || resolved == type) {
            return this;
        }
        for (ResolvableType interfaceType : getInterfaces()) {
            ResolvableType interfaceAsType = interfaceType.as(type);
            if (interfaceAsType != NONE) {
                return interfaceAsType;
            }
        }
        return getSuperType().as(type);
    }

    /**
     * Return a {@link ResolvableType} representing the direct supertype of this type.
     * <p>If no supertype is available this method returns {@link #NONE}.
     * <p>Note: The resulting {@link ResolvableType} instance may not be {@link Serializable}.
     *
     * @return a {@link ResolvableType} representing
     * @see #getInterfaces()
     */
    public ResolvableType getSuperType() {
        Class<?> resolved = resolve();
        if (resolved == null) {
            return NONE;
        }
        try {
            Type superclass = resolved.getGenericSuperclass();
            if (superclass == null) {
                return NONE;
            }
            ResolvableType superType = this.superType;
            if (superType == null) {
                superType = forType(superclass, this);
                this.superType = superType;
            }
            return superType;
        } catch (TypeNotPresentException ex) {
            // Ignore non-present types in generic signature
            return NONE;
        }
    }

    /**
     * Return a {@link ResolvableType} array representing the direct interfaces
     * implemented by this type. If this type does not implement any interfaces an
     * empty array is returned.
     * <p>Note: The resulting {@link ResolvableType} instances may not be {@link Serializable}.
     *
     * @return a {@link ResolvableType} array
     * @see #getSuperType()
     */
    public ResolvableType[] getInterfaces() {
        Class<?> resolved = resolve();
        if (resolved == null) {
            return EMPTY_TYPES_ARRAY;
        }
        ResolvableType[] interfaces = this.interfaces;
        if (interfaces == null) {
            Type[] genericIfcs = resolved.getGenericInterfaces();
            interfaces = new ResolvableType[genericIfcs.length];
            for (int i = 0; i < genericIfcs.length; i++) {
                interfaces[i] = forType(genericIfcs[i], this);
            }
            this.interfaces = interfaces;
        }
        return interfaces;
    }

    /**
     * Return {@code true} if this type contains generic parameters.
     *
     * @return {@code true} if this type contains generic parameters.
     * @see #getGeneric(int...)
     * @see #getGenerics()
     */
    public boolean hasGenerics() {
        return (getGenerics().length > 0);
    }


    /**
     * Return a {@link ResolvableType} representing the generic parameter for the
     * given indexes. Indexes are zero based; for example given the type
     * {@code Map<Integer, List<String>>}, {@code getGeneric(0)} will access the
     * {@code Integer}. Nested generics can be accessed by specifying multiple indexes;
     * for example {@code getGeneric(1, 0)} will access the {@code String} from the
     * nested {@code List}. For convenience, if no indexes are specified the first
     * generic is returned.
     * <p>If no generic is available at the specified indexes {@link #NONE} is returned.
     *
     * @param indexes the indexes that refer to the generic parameter
     *                (may be omitted to return the first generic)
     * @return a {@link ResolvableType} for the specified generic, or {@link #NONE}
     * @see #hasGenerics()
     * @see #getGenerics()
     * @see #resolveGeneric(int...)
     * @see #resolveGenerics()
     */
    public ResolvableType getGeneric(int... indexes) {
        ResolvableType[] generics = getGenerics();
        if (indexes == null || indexes.length == 0) {
            return (generics.length == 0 ? NONE : generics[0]);
        }
        ResolvableType generic = this;
        for (int index : indexes) {
            generics = generic.getGenerics();
            if (index < 0 || index >= generics.length) {
                return NONE;
            }
            generic = generics[index];
        }
        return generic;
    }

    /**
     * Return an array of {@link ResolvableType ResolvableTypes} representing the generic parameters of
     * this type. If no generics are available an empty array is returned. If you need to
     * access a specific generic consider using the {@link #getGeneric(int...)} method as
     * it allows access to nested generics and protects against
     * {@code IndexOutOfBoundsExceptions}.
     *
     * @return an array of {@link ResolvableType ResolvableTypes} representing the generic parameters
     * (never {@code null})
     * @see #hasGenerics()
     * @see #getGeneric(int...)
     * @see #resolveGeneric(int...)
     * @see #resolveGenerics()
     */
    public ResolvableType[] getGenerics() {
        if (this == NONE) {
            return EMPTY_TYPES_ARRAY;
        }
        ResolvableType[] generics = this.generics;
        if (generics == null) {
            if (this.type instanceof Class) {
                Type[] typeParams = ((Class<?>) this.type).getTypeParameters();
                generics = new ResolvableType[typeParams.length];
                for (int i = 0; i < generics.length; i++) {
                    generics[i] = ResolvableType.forType(typeParams[i], this);
                }
            } else if (this.type instanceof ParameterizedType) {
                Type[] actualTypeArguments = ((ParameterizedType) this.type).getActualTypeArguments();
                generics = new ResolvableType[actualTypeArguments.length];
                for (int i = 0; i < actualTypeArguments.length; i++) {
                    generics[i] = forType(actualTypeArguments[i], this.variableResolver);
                }
            } else {
                generics = resolveType().getGenerics();
            }
            this.generics = generics;
        }
        return generics;
    }

    /**
     * Convenience method that will {@link #getGenerics() get} and
     * {@link #resolve() resolve} generic parameters.
     *
     * @return an array of resolved generic parameters (the resulting array
     * will never be {@code null}, but it may contain {@code null} elements})
     * @see #getGenerics()
     * @see #resolve()
     */
    public Class<?>[] resolveGenerics() {
        ResolvableType[] generics = getGenerics();
        Class<?>[] resolvedGenerics = new Class<?>[generics.length];
        for (int i = 0; i < generics.length; i++) {
            resolvedGenerics[i] = generics[i].resolve();
        }
        return resolvedGenerics;
    }

    /**
     * Convenience method that will {@link #getGenerics() get} and {@link #resolve()
     * resolve} generic parameters, using the specified {@code fallback} if any type
     * cannot be resolved.
     *
     * @param fallback the fallback class to use if resolution fails
     * @return an array of resolved generic parameters
     * @see #getGenerics()
     * @see #resolve()
     */
    public Class<?>[] resolveGenerics(Class<?> fallback) {
        ResolvableType[] generics = getGenerics();
        Class<?>[] resolvedGenerics = new Class<?>[generics.length];
        for (int i = 0; i < generics.length; i++) {
            resolvedGenerics[i] = generics[i].resolve(fallback);
        }
        return resolvedGenerics;
    }

    /**
     * Convenience method that will {@link #getGeneric(int...) get} and
     * {@link #resolve() resolve} a specific generic parameters.
     *
     * @param indexes the indexes that refer to the generic parameter
     *                (may be omitted to return the first generic)
     * @return a resolved {@link Class} or {@code null}
     * @see #getGeneric(int...)
     * @see #resolve()
     */
    public Class<?> resolveGeneric(int... indexes) {
        return getGeneric(indexes).resolve();
    }

    /**
     * Resolve this type to a {@link Class}, returning {@code null}
     * if the type cannot be resolved. This method will consider bounds of
     * {@link TypeVariable TypeVariables} and {@link WildcardType WildcardTypes} if
     * direct resolution fails; however, bounds of {@code Object.class} will be ignored.
     * <p>If this method returns a non-null {@code Class} and {@link #hasGenerics()}
     * returns {@code false}, the given type effectively wraps a plain {@code Class},
     * allowing for plain {@code Class} processing if desirable.
     *
     * @return the resolved {@link Class}, or {@code null} if not resolvable
     * @see #resolve(Class)
     * @see #resolveGeneric(int...)
     * @see #resolveGenerics()
     */
    public Class<?> resolve() {
        return this.resolved;
    }

    /**
     * Resolve this type to a {@link Class}, returning the specified
     * {@code fallback} if the type cannot be resolved. This method will consider bounds
     * of {@link TypeVariable TypeVariables} and {@link WildcardType WildcardTypes} if
     * direct resolution fails; however, bounds of {@code Object.class} will be ignored.
     *
     * @param fallback the fallback class to use if resolution fails
     * @return the resolved {@link Class} or the {@code fallback}
     * @see #resolve()
     * @see #resolveGeneric(int...)
     * @see #resolveGenerics()
     */
    public Class<?> resolve(Class<?> fallback) {
        return (this.resolved != null ? this.resolved : fallback);
    }


    private Class<?> resolveClass() {
        if (this.type == EmptyType.INSTANCE) {
            return null;
        }
        if (this.type instanceof Class) {
            return (Class<?>) this.type;
        }
        if (this.type instanceof GenericArrayType) {
            Class<?> resolvedComponent = getComponentType().resolve();
            return (resolvedComponent != null ? Array.newInstance(resolvedComponent, 0).getClass() : null);
        }
        return resolveType().resolve();
    }

    /**
     * Resolve this type by a single level, returning the resolved value or {@link #NONE}.
     * <p>Note: The returned {@link ResolvableType} should only be used as an intermediary
     * as it cannot be serialized.
     *
     * @return the resolved value or {@link #NONE}
     */
    ResolvableType resolveType() {
        if (this.type instanceof ParameterizedType) {
            return forType(((ParameterizedType) this.type).getRawType(), this.variableResolver);
        }
        if (this.type instanceof WildcardType) {
            Type resolved = resolveBounds(((WildcardType) this.type).getUpperBounds());
            if (resolved == null) {
                resolved = resolveBounds(((WildcardType) this.type).getLowerBounds());
            }
            return forType(resolved, this.variableResolver);
        }
        if (this.type instanceof TypeVariable) {
            TypeVariable<?> variable = (TypeVariable<?>) this.type;
            // Try default variable resolution
            if (this.variableResolver != null) {
                ResolvableType resolved = this.variableResolver.resolveVariable(variable);
                if (resolved != null) {
                    return resolved;
                }
            }
            // Fallback to bounds
            return forType(resolveBounds(variable.getBounds()), this.variableResolver);
        }
        return NONE;
    }


    private Type resolveBounds(Type[] bounds) {
        if (bounds.length == 0 || bounds[0] == Object.class) {
            return null;
        }
        return bounds[0];
    }


    private ResolvableType resolveVariable(TypeVariable<?> variable) {
        if (this.type instanceof TypeVariable) {
            return resolveType().resolveVariable(variable);
        }
        if (this.type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) this.type;
            Class<?> resolved = resolve();
            if (resolved == null) {
                return null;
            }
            TypeVariable<?>[] variables = resolved.getTypeParameters();
            for (int i = 0; i < variables.length; i++) {
                if (ObjectUtils.nullSafeEquals(variables[i].getName(), variable.getName())) {
                    Type actualType = parameterizedType.getActualTypeArguments()[i];
                    return forType(actualType, this.variableResolver);
                }
            }
            Type ownerType = parameterizedType.getOwnerType();
            if (ownerType != null) {
                return forType(ownerType, this.variableResolver).resolveVariable(variable);
            }
        }
        if (this.type instanceof WildcardType) {
            ResolvableType resolved = resolveType().resolveVariable(variable);
            if (resolved != null) {
                return resolved;
            }
        }
        if (this.variableResolver != null) {
            return this.variableResolver.resolveVariable(variable);
        }
        return null;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ResolvableType)) {
            return false;
        }

        ResolvableType otherType = (ResolvableType) other;
        if (!ObjectUtils.nullSafeEquals(this.type, otherType.type)) {
            return false;
        }
        if (this.variableResolver != otherType.variableResolver &&
                (this.variableResolver == null || otherType.variableResolver == null ||
                        !ObjectUtils.nullSafeEquals(this.variableResolver.getSource(), otherType.variableResolver.getSource()))) {
            return false;
        }
        if (!ObjectUtils.nullSafeEquals(this.componentType, otherType.componentType)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return (this.hash != null ? this.hash : calculateHashCode());
    }

    private int calculateHashCode() {
        int hashCode = this.type.hashCode();
        if (this.variableResolver != null) {
            hashCode = 31 * hashCode + this.variableResolver.getSource().hashCode();
        }
        if (this.componentType != null) {
            hashCode = 31 * hashCode + this.componentType.hashCode();
        }
        return hashCode;
    }

    /**
     * Adapts this {@link ResolvableType} to a {@link VariableResolver}.
     */

    VariableResolver asVariableResolver() {
        if (this == NONE) {
            return null;
        }
        return new DefaultVariableResolver(this);
    }

    /**
     * Custom serialization support for {@link #NONE}.
     * return {@link #NONE} if type is empty
     */
    private Object readResolve() {
        return (this.type == EmptyType.INSTANCE ? NONE : this);
    }

    /**
     * @return a String representation of this type in its fully resolved form
     * (including any generic parameters).
     */
    @Override
    public String toString() {
        if (isArray()) {
            return getComponentType() + "[]";
        }
        if (this.resolved == null) {
            return "?";
        }
        if (this.type instanceof TypeVariable) {
            TypeVariable<?> variable = (TypeVariable<?>) this.type;
            if (this.variableResolver == null || this.variableResolver.resolveVariable(variable) == null) {
                // Don't bother with variable boundaries for toString()...
                // Can cause infinite recursions in case of self-references
                return "?";
            }
        }
        if (hasGenerics()) {
            StringBuilder s = new StringBuilder(this.resolved.getName() + '<');
            for (ResolvableType generic : getGenerics()) {
                s.append(generic.toString()).append(",");
            }
            return s.substring(0, s.length() - 1) + '>';
        }
        return this.resolved.getName();
    }


    // Factory methods

    /**
     * Return a {@link ResolvableType} for the specified {@link Class},
     * using the full generic type information for assignability checks.
     * <p>For example: {@code ResolvableType.forClass(MyArrayList.class)}.
     *
     * @param clazz the class to introspect ({@code null} is semantically
     *              equivalent to {@code Object.class} for typical use cases here)
     * @return a {@link ResolvableType} for the specified class
     * @see #forClass(Class, Class)
     * @see #forClassWithGenerics(Class, Class...)
     */
    public static ResolvableType forClass(Class<?> clazz) {
        return new ResolvableType(clazz);
    }

    /**
     * Return a {@link ResolvableType} for the specified {@link Class},
     * doing assignability checks against the raw class only (analogous to
     * {@link Class#isAssignableFrom}, which this serves as a wrapper for).
     * <p>For example: {@code ResolvableType.forRawClass(List.class)}.
     *
     * @param clazz the class to introspect ({@code null} is semantically
     *              equivalent to {@code Object.class} for typical use cases here)
     * @return a {@link ResolvableType} for the specified class
     * @see #forClass(Class)
     * @see #getRawClass()
     * @since 4.2
     */
    public static ResolvableType forRawClass(Class<?> clazz) {
        return new ResolvableType(clazz) {
            @Override
            public ResolvableType[] getGenerics() {
                return EMPTY_TYPES_ARRAY;
            }

            @Override
            public boolean isAssignableFrom(ResolvableType other) {
                Class<?> otherClass = other.resolve();
                return (otherClass != null && (clazz == null || clazz.isAssignableFrom(otherClass)));
            }
        };
    }

    /**
     * Return a {@link ResolvableType} for the specified base type
     * (interface or base class) with a given implementation class.
     * <p>For example: {@code ResolvableType.forClass(List.class, MyArrayList.class)}.
     *
     * @param baseType            the base type (must not be {@code null})
     * @param implementationClass the implementation class
     * @return a {@link ResolvableType} for the specified base type backed by the
     * given implementation class
     * @see #forClass(Class)
     * @see #forClassWithGenerics(Class, Class...)
     */
    public static ResolvableType forClass(Class<?> baseType, Class<?> implementationClass) {
        ResolvableType asType = forType(implementationClass).as(baseType);
        return (asType == NONE ? forType(baseType) : asType);
    }

    /**
     * Return a {@link ResolvableType} for the specified {@link Class} with pre-declared generics.
     *
     * @param clazz    the class (or interface) to introspect
     * @param generics the generics of the class
     * @return a {@link ResolvableType} for the specific class and generics
     * @see #forClassWithGenerics(Class, ResolvableType...)
     */
    public static ResolvableType forClassWithGenerics(Class<?> clazz, Class<?>... generics) {
        ResolvableType[] resolvableGenerics = new ResolvableType[generics.length];
        for (int i = 0; i < generics.length; i++) {
            resolvableGenerics[i] = forClass(generics[i]);
        }
        return forClassWithGenerics(clazz, resolvableGenerics);
    }

    /**
     * Return a {@link ResolvableType} for the specified {@link Class} with pre-declared generics.
     *
     * @param clazz    the class (or interface) to introspect
     * @param generics the generics of the class
     * @return a {@link ResolvableType} for the specific class and generics
     * @see #forClassWithGenerics(Class, Class...)
     */
    public static ResolvableType forClassWithGenerics(Class<?> clazz, ResolvableType... generics) {
        TypeVariable<?>[] variables = clazz.getTypeParameters();

        Type[] arguments = new Type[generics.length];
        for (int i = 0; i < generics.length; i++) {
            ResolvableType generic = generics[i];
            Type argument = (generic != null ? generic.getType() : null);
            arguments[i] = (argument != null && !(argument instanceof TypeVariable) ? argument : variables[i]);
        }

        ParameterizedType syntheticType = new SyntheticParameterizedType(clazz, arguments);
        return forType(syntheticType, new TypeVariablesVariableResolver(variables, generics));
    }

    /**
     * Return a {@link ResolvableType} for the specified instance. The instance does not
     * convey generic information
     *
     * @param instance the instance (possibly {@code null})
     * @return a {@link ResolvableType} for the specified instance,
     * or {@code NONE} for {@code null}
     * @since 4.2
     */
    public static ResolvableType forInstance(Object instance) {
        return (instance != null ? forClass(instance.getClass()) : NONE);
    }

    /**
     * Return a {@link ResolvableType} as an array of the specified {@code componentType}.
     *
     * @param componentType the component type
     * @return a {@link ResolvableType} as an array of the specified component type
     */
    public static ResolvableType forArrayComponent(ResolvableType componentType) {
        Class<?> arrayClass = Array.newInstance(componentType.resolve(), 0).getClass();
        return new ResolvableType(arrayClass, null, componentType);
    }

    /**
     * Return a {@link ResolvableType} for the specified {@link Type}.
     * <p>Note: The resulting {@link ResolvableType} instance may not be {@link Serializable}.
     *
     * @param type the source type (potentially {@code null})
     * @return a {@link ResolvableType} for the specified {@link Type}
     * @see #forType(Type, ResolvableType)
     */
    public static ResolvableType forType(Type type) {
        return forType(type, (VariableResolver) null);
    }

    /**
     * Return a {@link ResolvableType} for the specified {@link Type} backed by the given
     * owner type.
     * <p>Note: The resulting {@link ResolvableType} instance may not be {@link Serializable}.
     *
     * @param type  the source type or {@code null}
     * @param owner the owner type used to resolve variables
     * @return a {@link ResolvableType} for the specified {@link Type} and owner
     * @see #forType(Type)
     */
    public static ResolvableType forType(Type type, ResolvableType owner) {
        VariableResolver variableResolver = null;
        if (owner != null) {
            variableResolver = owner.asVariableResolver();
        }
        return forType(type, variableResolver);
    }


    /**
     * Return a {@link ResolvableType} for the specified {@link Type} backed by a given
     * {@link VariableResolver}.
     *
     * @param type             the source type or {@code null}
     * @param variableResolver the variable resolver or {@code null}
     * @return a {@link ResolvableType} for the specified {@link Type} and {@link VariableResolver}
     */
    static ResolvableType forType(Type type, VariableResolver variableResolver) {

        if (type == null) {
            return NONE;
        }

        // For simple Class references, build the wrapper right away -
        // no expensive resolution necessary, so not worth caching...
        if (type instanceof Class) {
            return new ResolvableType(type, variableResolver, (ResolvableType) null);
        }


        // Check the cache - we may have a ResolvableType which has been resolved before...
        ResolvableType resultType = new ResolvableType(type, variableResolver);
        ResolvableType cachedType = new ResolvableType(type, variableResolver, resultType.hash);
        resultType.resolved = cachedType.resolved;
        return resultType;
    }


    /**
     * 检查指定类是否有指定泛型类型
     *
     * @param type    待检查类
     * @param generic 泛型具体类
     * @return true类有指定的泛型类型
     */
    public static boolean hasGeneric(Class<?> type, Class<?> generic) {
        return hasGeneric(ResolvableType.forType(type), ResolvableType.forType(generic));
    }

    /**
     * 检查指定类是否有指定泛型类型
     *
     * @param type    待检查类
     * @param generic 泛型具体类
     * @return true类有指定的泛型类型
     */
    public static boolean hasGeneric(ResolvableType type, ResolvableType generic) {
        if (type.equals(ResolvableType.NONE)) {
            return false;
        }
        ResolvableType[] types = type.getGenerics();
        for (ResolvableType resolvableType : types) {
            if (resolvableType.isAssignableFrom(generic)) {
                return true;
            }
        }

        for (ResolvableType anInterface : type.getInterfaces()) {
            if (hasGeneric(anInterface, generic)) {
                return true;
            }
        }

        return hasGeneric(type.getSuperType(), generic);
    }

    /**
     * Strategy interface used to resolve {@link TypeVariable TypeVariables}.
     */
    interface VariableResolver extends Serializable {

        /**
         * Return the source of the resolver (used for hashCode and equals).
         */
        Object getSource();

        /**
         * Resolve the specified variable.
         *
         * @param variable the variable to resolve
         * @return the resolved variable, or {@code null} if not found
         */
        ResolvableType resolveVariable(TypeVariable<?> variable);
    }


    @SuppressWarnings("serial")
    private static class DefaultVariableResolver implements VariableResolver {

        private final ResolvableType source;

        DefaultVariableResolver(ResolvableType resolvableType) {
            this.source = resolvableType;
        }

        @Override
        public ResolvableType resolveVariable(TypeVariable<?> variable) {
            return this.source.resolveVariable(variable);
        }

        @Override
        public Object getSource() {
            return this.source;
        }
    }


    @SuppressWarnings("serial")
    private static class TypeVariablesVariableResolver implements VariableResolver {

        private final TypeVariable<?>[] variables;

        private final ResolvableType[] generics;

        public TypeVariablesVariableResolver(TypeVariable<?>[] variables, ResolvableType[] generics) {
            this.variables = variables;
            this.generics = generics;
        }

        @Override
        public ResolvableType resolveVariable(TypeVariable<?> variable) {
            for (int i = 0; i < this.variables.length; i++) {
                TypeVariable<?> resolvedVariable = this.variables[i];
                if (ObjectUtils.nullSafeEquals(resolvedVariable, variable)) {
                    return this.generics[i];
                }
            }
            return null;
        }

        @Override
        public Object getSource() {
            return this.generics;
        }
    }


    private static final class SyntheticParameterizedType implements ParameterizedType, Serializable {

        private final Type rawType;

        private final Type[] typeArguments;

        public SyntheticParameterizedType(Type rawType, Type[] typeArguments) {
            this.rawType = rawType;
            this.typeArguments = typeArguments;
        }

        @Override
        public String getTypeName() {
            String typeName = this.rawType.getTypeName();
            if (this.typeArguments.length > 0) {
                StringJoiner stringJoiner = new StringJoiner(", ", "<", ">");
                for (Type argument : this.typeArguments) {
                    stringJoiner.add(argument.getTypeName());
                }
                return typeName + stringJoiner;
            }
            return typeName;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }

        @Override
        public Type getRawType() {
            return this.rawType;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return this.typeArguments;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof ParameterizedType)) {
                return false;
            }
            ParameterizedType otherType = (ParameterizedType) other;
            return (otherType.getOwnerType() == null && this.rawType.equals(otherType.getRawType()) &&
                    Arrays.equals(this.typeArguments, otherType.getActualTypeArguments()));
        }

        @Override
        public int hashCode() {
            return (this.rawType.hashCode() * 31 + Arrays.hashCode(this.typeArguments));
        }

        @Override
        public String toString() {
            return getTypeName();
        }
    }


    /**
     * Internal helper to handle bounds from {@link WildcardType WildcardTypes}.
     */
    private static class WildcardBounds {

        private final Kind kind;

        private final ResolvableType[] bounds;

        /**
         * Internal constructor to create a new {@link WildcardBounds} instance.
         *
         * @param kind   the kind of bounds
         * @param bounds the bounds
         * @see #get(ResolvableType)
         */
        public WildcardBounds(Kind kind, ResolvableType[] bounds) {
            this.kind = kind;
            this.bounds = bounds;
        }

        /**
         * Return {@code true} if this bounds is the same kind as the specified bounds.
         */
        public boolean isSameKind(WildcardBounds bounds) {
            return this.kind == bounds.kind;
        }

        /**
         * Return {@code true} if this bounds is assignable to all the specified types.
         *
         * @param types the types to test against
         * @return {@code true} if this bounds is assignable to all types
         */
        public boolean isAssignableFrom(ResolvableType... types) {
            for (ResolvableType bound : this.bounds) {
                for (ResolvableType type : types) {
                    if (!isAssignable(bound, type)) {
                        return false;
                    }
                }
            }
            return true;
        }

        private boolean isAssignable(ResolvableType source, ResolvableType from) {
            return (this.kind == Kind.UPPER ? source.isAssignableFrom(from) : from.isAssignableFrom(source));
        }

        /**
         * Return the underlying bounds.
         */
        public ResolvableType[] getBounds() {
            return this.bounds;
        }

        /**
         * Get a {@link WildcardBounds} instance for the specified type, returning
         * {@code null} if the specified type cannot be resolved to a {@link WildcardType}.
         *
         * @param type the source type
         * @return a {@link WildcardBounds} instance or {@code null}
         */
        public static WildcardBounds get(ResolvableType type) {
            ResolvableType resolveToWildcard = type;
            while (!(resolveToWildcard.getType() instanceof WildcardType)) {
                if (resolveToWildcard == NONE) {
                    return null;
                }
                resolveToWildcard = resolveToWildcard.resolveType();
            }
            WildcardType wildcardType = (WildcardType) resolveToWildcard.type;
            Kind boundsType = (wildcardType.getLowerBounds().length > 0 ? Kind.LOWER : Kind.UPPER);
            Type[] bounds = (boundsType == Kind.UPPER ? wildcardType.getUpperBounds() : wildcardType.getLowerBounds());
            ResolvableType[] resolvableBounds = new ResolvableType[bounds.length];
            for (int i = 0; i < bounds.length; i++) {
                resolvableBounds[i] = ResolvableType.forType(bounds[i], type.variableResolver);
            }
            return new WildcardBounds(boundsType, resolvableBounds);
        }

        /**
         * The various kinds of bounds.
         */
        enum Kind {UPPER, LOWER}
    }


    /**
     * Internal {@link Type} used to represent an empty value.
     */
    @SuppressWarnings("serial")
    static class EmptyType implements Type, Serializable {

        static final Type INSTANCE = new EmptyType();

        Object readResolve() {
            return INSTANCE;
        }
    }

}

