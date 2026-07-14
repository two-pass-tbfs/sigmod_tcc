package com.tinkerpop.gremlin;

import com.tinkerpop.blueprints.Element;
import org.sigmod.chronograph.common.Event;
import org.sigmod.chronograph.common.RetainEventBinaryOperator;
import org.sigmod.chronograph.common.TemporalRelation;
import org.sigmod.chronograph.common.Time;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public interface GremlinFluentPipeline {

    // -------------------Transform: Graph to Element----------------------

    /**
     * Move traverser from a graph to all the vertices
     * <p>
     * Type: transform
     * <p>
     * Lazy Evaluation: true
     * <p>
     * Terminal Step: false
     *
     * @return the extended Pipeline
     */
    GremlinFluentPipeline V();

    /**
     * Move traverser from Graph to all the vertices matched with key-value
     * <p>
     * Type: transform
     * <p>
     * Lazy Evaluation: true
     * <p>
     * Terminal Step: false
     *
     * @param key   they key that all the emitted vertices should be checked on
     * @param value the value that all the emitted vertices should have for the key
     * @return the extended Pipeline
     */
    GremlinFluentPipeline V(String key, Object value);

    /**
     * Move traverser from Graph to all the edges
     * <p>
     * Type: transform
     * <p>
     * Lazy Evaluation: true
     * <p>
     * Terminal Step: false
     *
     * @return the extended Pipeline
     */
    GremlinFluentPipeline E();

    /**
     * Move traverser from Graph to all the edges matched with key-value
     * <p>
     * Type: transform
     * <p>
     * Lazy Evaluation: true
     * <p>
     * Terminal Step: false
     *
     * @param key   they key that all the emitted edges should be checked on
     * @param value the value that all the emitted edges should have for the key
     * @return the extended Pipeline
     */
    GremlinFluentPipeline E(String key, Object value);

    // -------------------Transform: Element <- -> String ----------------------

    /**
     * Move traversers from graph elements to their IDs (String)
     * <p>
     * Type: transform
     * <p>
     * Lazy Evaluation: true
     * <p>
     * Terminal Step: false
     *
     * @return the extended Pipeline
     */
    GremlinFluentPipeline id();

    /**
     * Move traversers from IDs of graph elements to the elements
     * <p>
     * Type: transform
     * <p>
     * Lazy Evaluation: true
     * <p>
     * Terminal Step: false
     *
     * @return the extended Pipeline
     */
    GremlinFluentPipeline element(Class<? extends Element> elementClass);

    // -------------------Transform: Vertex to Edge ----------------------

    /**
     * Move traverser from each vertex to out-going edges
     * <p>
     * Type: transform
     * <p>
     * Lazy Evaluation: true
     * <p>
     * Terminal Step: false
     *
     * @return the extended Pipeline
     */
    GremlinFluentPipeline outE(List<String> label);

    /**
     * Move traverser from each vertex to in-going edges
     * <p>
     * Type: transform
     * <p>
     * Lazy Evaluation: true
     * <p>
     * Terminal Step: false
     *
     * @return the extended Pipeline
     */
    GremlinFluentPipeline inE(List<String> label);

    // -------------------Transform: Edge to Vertex ----------------------

    /**
     * Move traverser from each edge to out vertex
     * <p>
     * Type: transform
     * <p>
     * Lazy Evaluation: true
     * <p>
     * Terminal Step: false
     *
     * @return the extended Pipeline
     */
    GremlinFluentPipeline outV();

    /**
     * Move traverser from each edge to in vertex
     * <p>
     * Type: transform
     * <p>
     * Lazy Evaluation: true
     * <p>
     * Terminal Step: false
     *
     * @return the extended Pipeline
     */
    GremlinFluentPipeline inV();

    // -------------------Transform: Vertex to Vertex ----------------------

    /**
     * Move traverser from each vertex to out-going vertices
     * <p>
     * Type: transform
     * <p>
     * Lazy Evaluation: true
     * <p>
     * Terminal Step: false
     *
     * @return the extended Pipeline
     */
    GremlinFluentPipeline out(List<String> label);

    /**
     * Move traverser from each vertex to out-going vertices
     * <p>
     * Type: transform
     * <p>
     * Lazy Evaluation: true
     * <p>
     * Terminal Step: false
     *
     * @return the extended Pipeline
     */
    GremlinFluentPipeline in(List<String> label);

    /**
     * Given an input, the provided function is computed on the input and the output
     * of that function is emitted.
     * <p>
     * Type: transform
     * <p>
     * Lazy Evaluation: true
     * <p>
     * Terminal Step: false
     *
     * @param function        the custom transform function without flattening
     * @param elementClass    the resulting class
     * @param collectionClass if the resulting class is a collection
     * @return the extended Pipeline
     */
    @SuppressWarnings("rawtypes")
    GremlinFluentPipeline map(Function function, Class<?> elementClass, Class<?> collectionClass);

    /**
     * Given an input, the provided function is computed on the input and the
     * flattened output of that function is emitted.
     * <p>
     * Type: transform
     * <p>
     * Lazy Evaluation: true
     * <p>
     * Terminal Step: false
     *
     * @param function     the custom transform function with flattening
     * @param elementClass the resulting class
     * @return the extended Pipeline
     */
    @SuppressWarnings("rawtypes")
    GremlinFluentPipeline flatMap(Function function, Class<?> elementClass);

    // -------------------Transform: Gather / Scatter ----------------------

    /**
     * All the objects previous to this step are aggregated in a greedy fashion and
     * emitted as a List (boxing). If the current element class is List.class,
     * ignored
     * <p>
     * Type: transform
     * <p>
     * Lazy Evaluation: false
     * <p>
     * Terminal Step: false
     *
     * @return the extended Pipeline
     */
    GremlinFluentPipeline gather();

    /**
     * Any input extending Collection is unboxed. If the input is not extending
     * Collection, the input is emitted as it is. If the current element class is
     * not List.class, ignored
     * <p>
     * Type: transform
     * <p>
     * Lazy Evaluation: true
     * <p>
     * Terminal Step: false
     *
     * @return the extended Pipeline
     */
    GremlinFluentPipeline scatter();

    // -------------------Filter/Sort/Limit ----------------------

    /**
     * Deduplicate edge labels with identical outV and inV For example, one of
     * e(i|l1|j) and e(i|l2|j) would be retained.
     *
     * @return the extended Pipeline
     */
    GremlinFluentPipeline dedupEdgeLabel();

    /**
     * Deduplicate the traversers
     * <p>
     * Type: filter
     * <p>
     * Lazy Evaluation: true
     * <p>
     * Terminal Step: false
     *
     * @return the extended Pipeline
     */
    GremlinFluentPipeline dedup();

    /**
     * A biased coin toss determines if the object is emitted or not.
     *
     * @param lowerBound if Random.getDouble() is larger than lowerBound, retained,
     *                   else filtered. (lowerBound is between 0 to 1)
     * @return the extended Pipeline
     *
     */
    <I> GremlinFluentPipeline random(Double lowerBound);

    /**
     * Check if the vertex or edge has a property with provided key.
     * <p>
     * Type: filter
     * <p>
     * Lazy Evaluation: true
     * <p>
     * Terminal Step: false
     *
     * @param key the property key to check
     * @return the extended Pipeline
     */
    GremlinFluentPipeline has(String key);

    /**
     * Check if the vertex or edge has a property with provided key-value.
     * <p>
     * Type: filter
     * <p>
     * Lazy Evaluation: true
     * <p>
     * Terminal Step: false
     *
     * @param key   the property key to check
     * @param value the object to filter on (in an OR manner)
     * @return the extended Pipeline
     */
    GremlinFluentPipeline has(String key, Object value);

    /**
     * Given an input, the provided predicate evaluates the input and only inputs
     * where predicate for an input is true are emitted.
     * <p>
     * Type: filter
     * <p>
     * Lazy Evaluation: true
     * <p>
     * Terminal Step: false
     *
     * @param predicate the custom filter function
     * @return the extended Pipeline
     */
    <E> GremlinFluentPipeline filter(Predicate<E> predicate);

    /**
     * Sort the traversers based on comparator
     * <p>
     * Type: filter
     * <p>
     * Lazy Evaluation: true
     * <p>
     * Terminal Step: false
     *
     * @param comparator
     * @return the extended Pipeline
     */
    <E> GremlinFluentPipeline sort(Comparator<E> comparator);

    /**
     * Limit the number of traversers
     * <p>
     * Type: filter
     * <p>
     * Lazy Evaluation: true
     * <p>
     * Terminal Step: false
     *
     * @param maxSize
     * @return the extended Pipeline
     */
    GremlinFluentPipeline limit(Long maxSize);

    // ------------------- Side Effect ----------------------

    /**
     *
     * Store traversers into a collection
     * <p>
     * Type: sideEffect
     * <p>
     * Lazy Evaluation: true
     * <p>
     * Terminal Step: false
     *
     * @param collection to store traversers
     * @return
     */
    <E> GremlinFluentPipeline sideEffect(Collection<E> collection);

    /**
     * The provided function is invoked while the incoming object is just emitted to
     * the outgoing object.
     * <p>
     * Type: sideEffect
     * <p>
     * Lazy Evaluation: true
     * <p>
     * Terminal Step: false
     *
     * @param function the function should return the intact argument
     * @return the extended Pipeline
     */
    <E> GremlinFluentPipeline sideEffect(Consumer<E> function);

    // ------------------- Branch ----------------------

    /**
     * if the ifPredicate for an input is true, thenFunction will be applied Else,
     * elseFunction will be applied The resulting stream would not be flattened.
     * Thus, if the elementClass is Collection, put its element class of the
     * collection in collectionElementClass
     *
     * @param ifPredicate
     * @param thenFunction
     * @param elseFunction
     * @param elementClass           identical regardless of ifPredicate passes or
     *                               not
     * @param collectionElementClass identical regardless of ifPredicate passes or
     *                               not
     * @return the extended Pipeline
     */
    <E> GremlinFluentPipeline ifThenElseMap(Predicate<E> ifPredicate, Function<E, ?> thenFunction,
                                            Function<E, ?> elseFunction, Class<?> elementClass, Class<?> collectionElementClass);

    /**
     *
     * if the ifPredicate for an input is true, thenFunction will be applied Else,
     * elseFunction will be applied The resulting stream would be flattened. Thus,
     * the resulting class of the then and else functions would emit Collection
     *
     * @param <I>
     * @param ifPredicate
     * @param thenFunction
     * @param elseFunction
     * @param elementClass
     * @return
     */
    <I> GremlinFluentPipeline ifThenElseFlatMap(Predicate<I> ifPredicate,
                                                Function<I, Collection<?>> thenFunction, Function<I, Collection<?>> elseFunction, Class<?> elementClass);

    /**
     * Useful for naming steps and is used in conjunction with loop
     *
     * @param pointer for the loop
     * @return the extended Pipeline
     */
    GremlinFluentPipeline as(String pointer);

    /**
     * loop repeats a part of a pipeline between as('name') to loop('name',
     * whilePredicate);
     *
     * @param pointer        within a pipeline
     * @param whilePredicate if true, go to the pointer
     * @return the extended Pipeline
     */
    <E> GremlinFluentPipeline loop(String pointer, Predicate<LoopBundle<E>> whilePredicate);

    // ------------------- Aggregation ----------------------

    /**
     * Group the traversers based on results of classifier
     * <p>
     * Type: transform
     * <p>
     * Lazy Evaluation: false
     * <p>
     * Terminal Step: true
     *
     * @param classifier
     * @return the extended Pipeline
     */
    <I, T> Map<T, List<I>> groupBy(Function<I, T> classifier);

    /**
     * Counting the traversers grouped by results of classifier
     * <p>
     * Type: transform
     * <p>
     * Lazy Evaluation: false
     * <p>
     * Terminal Step: true
     *
     * @param classifier
     * @return the extended Pipeline
     */
    <I, T> Map<T, Long> groupCount(Function<I, T> classifier);

    /**
     * Reduce the traversers based on reducer
     * <p>
     * Type: transform
     * <p>
     * Lazy Evaluation: false
     * <p>
     * Terminal Step: true
     *
     * @param reducer
     * @return the extended Pipeline
     */
    Optional<?> reduce(BinaryOperator<?> reducer);

    /**
     *
     * Collect the current traversers in Gremlin as List
     * <p>
     * Lazy Evaluation: false
     * <p>
     * Terminal Step: true
     *
     * @return the current traversers in Gremlin as List
     */
    <I> List<I> toList();

    ////////////////////////////
    ///// Temporal Support /////
    ////////////////////////////

    /**
     * Move traverser from vertices to their event valid at a time t
     * <p>
     * Type: transform
     * <p>
     * Lazy Evaluation: true
     * <p>
     * Terminal Step: false
     *
     * @param t
     * @return the extended Pipeline
     */
    GremlinFluentPipeline v(Time t);

    /**
     * Move traverser from each vertex event to its out-going vertex events
     * <p>
     * Type: transform
     * <p>
     * Lazy Evaluation: true
     * <p>
     * Terminal Step: false
     *
     * @param tr
     * @param label
     * @return the extended Pipeline
     */
    GremlinFluentPipeline oute(TemporalRelation tr, String label);

    /**
     * Move traverser from each vertex event to its in-going vertex events
     * <p>
     * Type: transform
     * <p>
     * Lazy Evaluation: true
     * <p>
     * Terminal Step: false
     *
     * @param tr
     * @param label
     * @return the extended Pipeline
     */
    GremlinFluentPipeline ine(TemporalRelation tr, String label);

    /**
     * Move traverser from each vertex event to its out-going edge events
     * <p>
     * Type: transform
     * <p>
     * Lazy Evaluation: true
     * <p>
     * Terminal Step: false
     *
     * @param tr
     * @param label
     * @return the extended Pipeline
     */
    GremlinFluentPipeline outEe(TemporalRelation tr, String label);

    /**
     * Move traverser from each vertex event to its in-going edge events
     * <p>
     * Type: transform
     * <p>
     * Lazy Evaluation: true
     * <p>
     * Terminal Step: false
     *
     * @param tr
     * @param label
     * @return the extended Pipeline
     */
    GremlinFluentPipeline inEe(TemporalRelation tr, String label);

    /**
     * Move traverser from each edge event to its out-going vertex events
     * <p>
     * Type: transform
     * <p>
     * Lazy Evaluation: true
     * <p>
     * Terminal Step: false
     *
     * @return the extended Pipeline
     */
    GremlinFluentPipeline outVe();

    /**
     * Move traverser from each edge event to its in-going vertex events
     * <p>
     * Type: transform
     * <p>
     * Lazy Evaluation: true
     * <p>
     * Terminal Step: false
     *
     * @return the extended Pipeline
     */
    GremlinFluentPipeline inVe();

    /**
     * Retain one event per graph element with the criteria
     * <p>
     * Type: filter
     * <p>
     * Lazy Evaluation: false
     * <p>
     * Terminal Step: false
     *
     * @return the extended Pipeline
     */
    <K> GremlinFluentPipeline retainEvent(Function<Event, K> keyMapper, Object gamma,
                                          RetainEventBinaryOperator<Event> retainEventBinaryOperator);
}
