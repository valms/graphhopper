/*
 *  Licensed to GraphHopper GmbH under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for 
 *  additional information regarding copyright ownership.
 * 
 *  GraphHopper GmbH licenses this file to you under the Apache License, 
 *  Version 2.0 (the "License"); you may not use this file except in 
 *  compliance with the License. You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.graphhopper.routing.phast;

import java.util.Collections;
import java.util.List;

import com.graphhopper.routing.Path;
import com.graphhopper.routing.RoutingAlgorithm;
import com.graphhopper.routing.util.CHEdgeFilter;
import com.graphhopper.routing.util.CHLevelEdgeFilter;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.storage.SPTEntry;
import com.graphhopper.util.CHEdgeIterator;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PMap;

/**
 * @author Peter Karich
 */
public abstract class AbstractRoutingAlgorithmPHAST implements RoutingAlgorithm {
	protected final Graph graph;
	protected final Weighting weighting;
	protected final TraversalMode traversalMode;
	protected NodeAccess nodeAccess;
	protected EdgeExplorer inEdgeExplorer;
	protected EdgeExplorer outEdgeExplorer;
	protected EdgeExplorer targetEdgeExplorer;
	protected int maxVisitedNodes = Integer.MAX_VALUE;
	protected CHEdgeFilter additionalEdgeFilter;
	protected CHLevelEdgeFilter targetEdgeFilter;

	private boolean alreadyRun;

	/**
	 * @param graph
	 *            specifies the graph where this algorithm will run on
	 * @param weighting
	 *            set the used weight calculation (e.g. fastest, shortest).
	 * @param traversalMode
	 *            how the graph is traversed e.g. if via nodes or edges.
	 */

	public AbstractRoutingAlgorithmPHAST(Graph graph, Weighting weighting, TraversalMode traversalMode,
			boolean downwardSearchAllowed) {

		this.weighting = weighting;
		this.traversalMode = traversalMode;
		this.graph = graph;
		this.nodeAccess = this.graph.getNodeAccess();

		if (!downwardSearchAllowed) {
			outEdgeExplorer = graph.createEdgeExplorer();
			inEdgeExplorer = graph.createEdgeExplorer();
			targetEdgeExplorer = graph.createEdgeExplorer();
		} else {
			PMap props = new PMap();
			props.put("allow_downward_search", true);
			
			outEdgeExplorer = graph.createEdgeExplorer(EdgeFilter.ALL_EDGES, props);
			inEdgeExplorer = graph.createEdgeExplorer(EdgeFilter.ALL_EDGES, props);
			targetEdgeExplorer = graph.createEdgeExplorer(EdgeFilter.ALL_EDGES, props);
		}
	}
	
	@Override
	public void setMaxVisitedNodes(int numberOfNodes) {
		this.maxVisitedNodes = numberOfNodes;
	}

	public RoutingAlgorithm setEdgeFilter(CHEdgeFilter edgeFilter) {
		this.additionalEdgeFilter = edgeFilter;
		return this;
	}

	protected boolean accept(CHEdgeIterator iter, int prevOrNextEdgeId) {
		if (!traversalMode.hasUTurnSupport() && iter.getEdge() == prevOrNextEdgeId)
			return false;

		return additionalEdgeFilter == null || additionalEdgeFilter.accept(iter);
	}

	protected void updateBestPath(EdgeIteratorState edgeState, SPTEntry bestSPTEntry, int traversalId) {
	}

	protected void checkAlreadyRun() {
		if (alreadyRun)
			throw new IllegalStateException("Create a new instance per call");

		alreadyRun = true;
	}

	public SPTEntry createSPTEntry(int node, double weight) {
		return new SPTEntry(EdgeIterator.NO_EDGE, node, weight);
	}

	/**
	 * To be overwritten from extending class. Should we make this available in
	 * RoutingAlgorithm interface?
	 * <p>
	 *
	 * @return true if finished.
	 */
	protected abstract boolean finished();

	/**
	 * To be overwritten from extending class. Should we make this available in
	 * RoutingAlgorithm interface?
	 * <p>
	 *
	 * @return true if finished.
	 */

	// public abstract IntObjectMap<SPTEntry> calcMatrix(int from);

	@Override
	public List<Path> calcPaths(int from, int to) {
		return Collections.singletonList(calcPath(from, to));
	}

	protected Path createEmptyPath() {
		return new Path(graph, weighting, -1);
	}

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	@Override
	public String toString() {
		return getName() + "|" + weighting;
	}

	protected boolean isMaxVisitedNodesExceeded() {
		return maxVisitedNodes < getVisitedNodes();
	}
}
