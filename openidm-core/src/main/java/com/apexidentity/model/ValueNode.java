/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2010–2011 ApexIdentity Inc. All rights reserved.
 */

package com.apexidentity.model;

// Java Standard Edition
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a value within an object model structure.
 *
 * @author Paul C. Bryan
 */
public class ValueNode extends Node {

    /**
     * Constructs an object model value with a given path.
     *
     * @param object the underlying value.
     * @param path the path of the value in the object model structure.
     */
    public ValueNode(Object object, String path) {
        super(object, path);
    }

    /**
     * Throws a {@code NodeException} if this value is {@code null}.
     *
     * @throws NodeException if this value is {@code null}.
     * @return this value.
     */
    public ValueNode required() throws NodeException {
        if (object == null) {
            throw new NodeException(this, "required value");
        }
        return this;
    }

    /**
     * Called to enforce that the value is a particular type if it is not {@code null}.
     *
     * @param type the class that the underlying value must have.
     * @return this value.
     * @throws NodeException if the value is not the specified type.
     */
    public ValueNode expect(Class type) throws NodeException {
        if (object != null && !type.isInstance(object)) {
            throw new NodeException(this, "expecting " + type.getName());
        }
        return this;
    }

    /**
     * Returns {@code true} if the model value is {@code null}.
     */
    public boolean isNull() {
        return (object == null);
    }

    /**
     * Sets the value to the specified character sequence if the value is currently
     * {@code null}.
     *
     * @param value the character sequence to default this value to.
     * @return this value.
     */
    public ValueNode defaultTo(CharSequence value) {
        return (object != null ? this : new ValueNode(value, path));
    }

    /**
     * Sets the value to the specified integer if the value is currently {@code null}.
     *
     * @param value the integer to default this value to.
     * @return this value.
     */      
    public ValueNode defaultTo(int value) {
        return (object != null ? this : new ValueNode(Integer.valueOf(value), path));
    }

    /**
     * Sets the value to the specified long integer if the value is currently {@code null}.
     *
     * @param value the long integer to default this value to.
     * @return this value.
     */      
    public ValueNode defaultTo(long value) {
        return (object != null ? this : new ValueNode(Long.valueOf(value), path));
    }

    /**
     * Sets the value to the specified double-precision floating point number if the value is currently {@code null}.
     *
     * @param value the double-precision floating point number to default this value to.
     * @return this value.
     */ 
    public ValueNode defaultTo(double value) {
        return (object != null ? this : new ValueNode(Double.valueOf(value), path));
    }

    /**
     * Sets the value to the specified list if the value is currently {@code null}.
     *
     * @param value the list to default this value to.
     * @return this value.
     */
    public ValueNode defaultTo(List value) {
        return (object != null ? this : new ValueNode(value, path));
    }

    /**
     * Sets the value to the specified map if the value is currently {@code null}.
     *
     * @param value the map to default this value to.
     * @return this value.
     */
    public ValueNode defaultTo(Map value) {
        return (object != null ? this : new ValueNode(value, path));
    }

    /**
     * Sets the value to the specified boolean value if value is currently {@code null}.
     *
     * @param value the boolean value to default this value to.
     * @return this value.
     */
    public ValueNode defaultTo(boolean value) {
        return (object != null ? this : new ValueNode(Boolean.valueOf(value), path));
    }

    /**
     * Returns the value as a string. If the value is {@code null}, this method returns
     * {@code null}.
     * 
     * @return the string value.
     * @throws NodeException if the value is not a character sequence.
     */
    public String asString() throws NodeException {
        return (object == null ? null : ((CharSequence)expect(CharSequence.class).object).toString());
    }

    /**
     * Returns the value as a number. If the value is {@code null}, this method returns
     * {@code null}.
     *
     * @return the numeric value.
     * @throws NodeException if the value is not a number.
     */
    public Number asNumber() throws NodeException {
        return (object == null ? null : (Number)expect(Number.class).object);
    }

    /**
     * Returns the value as an integer. This may involve rounding or truncation. If the
     * value is {@code null}, this method throws a {@code NodeException}. To avoid this,
     * use the {@code asNumber} method.
     *
     * @return the integer value.
     * @throws NodeException if the value is {@code null} or is not a number.
     */
    public int asInteger() throws NodeException {
        return required().asNumber().intValue();
    }

    /**
     * Returns the value as a double-precision floating point number. This may involve
     * rounding. If the value is {@code null}, this method throws a {@code NodeException}.
     * To avoid this, use the {@code asNumber} method.
     *
     * @return the double-precision floating point value.
     * @throws NodeException if the value is {@code null} or is not a number.
     */
    public double asDouble() throws NodeException {
        return required().asNumber().doubleValue();
    }

    /**
     * Returns the value as a long integer. This may involve rounding or truncation. If the
     * value is {@code null}, this method throws a {@code NodeException}. To avoid this,
     * use the {@code asNumber} method.
     *
     * @return the long integer value.
     * @throws NodeException if the value is {@code null} or is not a number.
     */
    public long asLong() throws NodeException {
        return required().asNumber().longValue();
    }

    /**
     * Returns the value as a Boolean object. If the value is {@code null}, this method
     * returns {@code null}.
     *
     * @return the boolean value.
     * @throws NodeException if the value is not a boolean type.
     */
    public Boolean asBoolean() throws NodeException {
        return (object == null ? null : (Boolean)expect(Boolean.class).object);
    }

    /**
     * Returns the value as a {@link List}-like type. If the value is {@code null}, this
     * method returns {@code null}.
     *
     * @return the listing object representing the list-like object, or {@code null} if no value.
     * @throws NodeException if the value is {@code null} or is not a list-like object.
     * @see ListNode for more information.
     */
    public ListNode asListNode() throws NodeException {
        return (object == null ? null : new ListNode(object, path));
    }

    /**
     * Returns the value as a {@link Map}-like type. If the value is {@code null}, this method
     * returns {@code null}.
     *
     * @return the mapping object representing the map-like object.
     * @throws NodeException if the value is not a map-like object.
     * @see MapNode for more information.
     */
    public MapNode asMapNode() throws NodeException {
        return (object == null ? null: new MapNode(object, path));
    }

    /**
     * Returns the value as a {@link List} of objects of a specified type. If the value is
     * {@code null}, this method returns {@code null}.
     *
     * @return a {@code List} of objects, or {@code} null if no value.
     * @throws NodeException if the value is not a list-like object or contains unexpected types.
     */
    @SuppressWarnings("unchecked")
    public <E> List<E> asList(Class<E> type) throws NodeException {
        if (object == null) {
            return null;
        }
        ListNode listNode = asListNode();
        ArrayList<E> list = new ArrayList<E>(listNode.size());
        for (ValueNode value : listNode) {
            list.add((E)value.expect(type).object);
        }
        return list;
    }

    /**
     * Returns the value as a {@code Map}, with {@code String} keys, and values of a
     * specified type. If the value is {@code null}, this method returns {@code null}.
     *
     * @param type the type expected for all values in the map.
     * @return a map with {@code String} keys and values of the specified type, or {@code null} if no value.
     * @throws NodeException if the value is not a map-like object or contains unexpected value types.
     */
    @SuppressWarnings("unchecked")
    public <V> Map<String, V> asMap(Class<V> type) throws NodeException {
        if (object == null) {
            return null;
        }
        MapNode mapNode = asMapNode();
        HashMap<String, V> map = new HashMap<String, V>(mapNode.size());
        for (String key : mapNode.keySet()) {
            map.put(key, (V)mapNode.get(key).expect(type).object);
        }
        return map;
    }

    /**
     * Returns the object model string value as the enum constant of the specified enum type.
     * If the value is {@code null}, this method returns {@code null}.
     *
     * @param type the enum type to match constants with the value.
     * @return the enum constant represented by the string value.
     * @throws NodeException if the value does not match any of the enum's constants.
     */
    public <T extends Enum<T>> T asEnum(Class<T> type) throws NodeException {
        if (type == null) {
            throw new NullPointerException();
        }
        try {
            return (object == null ? null : Enum.valueOf(type, asString().toUpperCase()));
        }
        catch (IllegalArgumentException iae) {
            StringBuilder sb = new StringBuilder("Expecting one of:");
            for (Object constant : type.getEnumConstants()) {
                sb.append(constant.toString()).append(' ');
            }
            throw new NodeException(this, sb.toString());
        }
    }
}
