package org.sigmod.chronograph.common;

@FunctionalInterface
public interface RetainEventBinaryOperator<E> {
	E apply(Object gamma, E e1, E e2);
}
