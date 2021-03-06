/*
 * (C) Copyright 2019-2019, by Semen Chudakov and Contributors.
 *
 * JGraphT : a free Java graph-theory library
 *
 * See the CONTRIBUTORS.md file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the
 * GNU Lesser General Public License v2.1 or later
 * which is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1-standalone.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR LGPL-2.1-or-later
 */
package org.jgrapht.alg.shortestpath;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.util.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * Naive algorithm for many-to-many shortest paths problem using.
 *
 * <p>
 * For every pair of source and target vertices computes a shortest path between them and
 * caches the result.
 *
 * <p>
 * For each source vertex a single source shortest paths search is performed, which is stopped
 * as soon as all target vertices are reached. Shortest paths trees are constructed using
 * {@link DijkstraClosestFirstIterator}.
 *
 * @param <V> the graph vertex type
 * @param <E> the graph edge type
 * @author Semen Chudakov
 */
public class DefaultManyToManyShortestPaths<V, E> extends BaseManyToManyShortestPaths<V, E> {

    /**
     * provides implementation of {@link ShortestPathAlgorithm} for a given graph.
     */
    private final Function<Graph<V, E>, ShortestPathAlgorithm<V, E>> function;

    /**
     * Constructs a new instance of the algorithm for a given {@code graph}.
     * The function is defaulted to {@link BidirectionalDijkstraShortestPath}.
     *
     * @param graph a graph
     */
    public DefaultManyToManyShortestPaths(Graph<V, E> graph) {
        this(graph, g -> new BidirectionalDijkstraShortestPath<>(g));
    }

    /**
     * Constructs a new instance of the algorithm for a given {@code graph} and {@code function}.
     *
     * @param graph    a graph
     * @param function provides implementation of {@link ShortestPathAlgorithm}
     */
    public DefaultManyToManyShortestPaths(Graph<V, E> graph, Function<Graph<V, E>, ShortestPathAlgorithm<V, E>> function) {
        super(graph);
        this.function = function;
    }

    @Override
    public ManyToManyShortestPaths<V, E> getManyToManyPaths(Set<V> sources, Set<V> targets) {
        Objects.requireNonNull(sources, "sources cannot be null!");
        Objects.requireNonNull(targets, "targets cannot be null!");

        ShortestPathAlgorithm<V, E> algorithm = function.apply(graph);
        Map<Pair<V, V>, GraphPath<V, E>> pathMap = new HashMap<>();

        for (V source : sources) {
            for (V target : targets) {
                pathMap.put(Pair.of(source, target), algorithm.getPath(source, target));
            }
        }

        return new DefaultManyToManyShortestPathsImpl(sources, targets, pathMap);
    }

    /**
     * Implementation of the
     * {@link org.jgrapht.alg.interfaces.ManyToManyShortestPathsAlgorithm.ManyToManyShortestPaths}.
     * For each pair of source and target vertices stores a corresponding path between them.
     */
    private class DefaultManyToManyShortestPathsImpl extends BaseManyToManyShortestPathsImpl<V, E> {

        /**
         * Map from source vertices to corresponding single source shortest path trees.
         */
        private final Map<Pair<V, V>, GraphPath<V, E>> pathsMap;

        /**
         * Constructs an instance of the algorithm for the given {@code sources},
         * {@code pathsMap} and {@code searchSpaces}.
         *
         * @param sources  source vertices
         * @param targets  target vertices
         * @param pathsMap single source shortest paths trees map
         */
        DefaultManyToManyShortestPathsImpl(Set<V> sources, Set<V> targets,
                                           Map<Pair<V, V>, GraphPath<V, E>> pathsMap) {
            super(sources, targets);
            this.pathsMap = pathsMap;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public GraphPath<V, E> getPath(V source, V target) {
            assertCorrectSourceAndTarget(source, target);
            Pair<V, V> sourceTargetPair = Pair.of(source, target);
            return pathsMap.get(sourceTargetPair);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public double getWeight(V source, V target) {
            assertCorrectSourceAndTarget(source, target);
            Pair<V, V> sourceTargetPair = Pair.of(source, target);

            GraphPath<V, E> path = pathsMap.get(sourceTargetPair);
            if (path == null) {
                return Double.POSITIVE_INFINITY;
            }
            return path.getWeight();
        }
    }
}
