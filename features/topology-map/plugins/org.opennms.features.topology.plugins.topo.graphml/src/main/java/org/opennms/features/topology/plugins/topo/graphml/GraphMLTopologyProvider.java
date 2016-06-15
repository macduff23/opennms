/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2012-2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.features.topology.plugins.topo.graphml;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;

import org.opennms.features.graphml.model.GraphMLGraph;
import org.opennms.features.graphml.model.GraphMLNode;
import org.opennms.features.topology.api.browsers.ContentType;
import org.opennms.features.topology.api.browsers.SelectionChangedListener;
import org.opennms.features.topology.api.support.FocusStrategy;
import org.opennms.features.topology.api.support.VertexHopGraphProvider;
import org.opennms.features.topology.api.topo.AbstractTopologyProvider;
import org.opennms.features.topology.api.topo.DefaultTopologyProviderInfo;
import org.opennms.features.topology.api.topo.Defaults;
import org.opennms.features.topology.api.topo.GraphProvider;
import org.opennms.features.topology.api.topo.TopologyProviderInfo;
import org.opennms.features.topology.api.topo.VertexRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class GraphMLTopologyProvider extends AbstractTopologyProvider implements GraphProvider {

    protected static final String DEFAULT_DESCRIPTION = "This Topology Provider visualizes a predefined GraphML graph.";
    private static final Logger LOG = LoggerFactory.getLogger(GraphMLTopologyProvider.class);;

    private static TopologyProviderInfo createTopologyProviderInfo(GraphMLGraph graph) {
        String name = graph.getProperty(GraphMLProperties.LABEL, graph.getId());
        String description = graph.getProperty(GraphMLProperties.DESCRIPTION, DEFAULT_DESCRIPTION);
        return new DefaultTopologyProviderInfo(name, description);
    }

    private final int defaultSzl;
    private final String preferredLayout;
    private final FocusStrategy focusStrategy;
    private final List<String> focusIds;

    public GraphMLTopologyProvider(GraphMLGraph graph) {
        super(graph.getProperty(GraphMLProperties.NAMESPACE));
        for (GraphMLNode graphMLNode : graph.getNodes()) {
            GraphMLVertex newVertex = new GraphMLVertex(this.getVertexNamespace(), graphMLNode);
            addVertices(newVertex);
        }
        for (org.opennms.features.graphml.model.GraphMLEdge eachEdge : graph.getEdges()) {
            GraphMLVertex sourceVertex = (GraphMLVertex) getVertex(getVertexNamespace(), eachEdge.getSource().getId());
            GraphMLVertex targetVertex = (GraphMLVertex) getVertex(getVertexNamespace(), eachEdge.getTarget().getId());
            if (sourceVertex == null || targetVertex == null) {
                // Skip edges where either the source of target vertices are outside of this graph
                continue;
            }
            GraphMLEdge newEdge = new GraphMLEdge(getEdgeNamespace(), eachEdge, sourceVertex, targetVertex);
            addEdges(newEdge);
        }
        setTopologyProviderInfo(createTopologyProviderInfo(graph));
        defaultSzl = getDefaultSzl(graph);
        focusStrategy = getFocusStrategy(graph);
        focusIds = getFocusIds(graph);
        preferredLayout = getPreferredLayout(graph);
        if (focusStrategy != FocusStrategy.SPECIFIC && !focusIds.isEmpty()) {
            LOG.warn("Focus ids is defined, but strategy is {}. Did you mean to specify {}={}. Ignoring focusIds.", GraphMLProperties.FOCUS_STRATEGY, FocusStrategy.SPECIFIC.name());
        }
    }

    private static String getPreferredLayout(GraphMLGraph graph) {
        return graph.getProperty(GraphMLProperties.PREFERRED_LAYOUT);
    }

    private static FocusStrategy getFocusStrategy(GraphMLGraph graph) {
        String strategy = graph.getProperty(GraphMLProperties.FOCUS_STRATEGY);
        if (strategy != null) {
            return FocusStrategy.getStrategy(strategy, FocusStrategy.FIRST);
        }
        return FocusStrategy.FIRST;
    }

    private static List<String>  getFocusIds(GraphMLGraph graph) {
        String property = graph.getProperty(GraphMLProperties.FOCUS_IDS);
        if (property != null) {
            String[] split = property.split(",");
            return Lists.newArrayList(split);
        }
        return Lists.newArrayList();
    }

    private static int getDefaultSzl(GraphMLGraph graph) {
        Integer szl = graph.getProperty(GraphMLProperties.SEMANTIC_ZOOM_LEVEL);
        if (szl != null) {
            return szl;
        }
        return Defaults.DEFAULT_SEMANTIC_ZOOM_LEVEL;
    }

    @Override
    public void refresh() {
        // TODO: How to handle refresh()?
    }

    @Override
    public void load(final String filename) throws MalformedURLException, JAXBException {
        refresh();
    }

    @Override
    public void save() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Defaults getDefaults() {
        return new Defaults()
                .withSemanticZoomLevel(defaultSzl)
                .withPreferredLayout(preferredLayout)
                .withCriteria(() -> {
                    List<VertexHopGraphProvider.VertexHopCriteria> focusCriteria = focusStrategy.getFocusCriteria(this, focusIds.toArray(new String[focusIds.size()]));
                    return Lists.newArrayList(focusCriteria);
                });
    }

    @Override
    public SelectionChangedListener.Selection getSelection(List<VertexRef> selectedVertices, ContentType contentType) {
        Set<Integer> nodeIds = selectedVertices.stream()
                .filter(eachVertex -> eachVertex.getNamespace().equals(getVertexNamespace()) && eachVertex instanceof GraphMLVertex)
                .map(eachVertex -> (GraphMLVertex) eachVertex)
                .filter(eachVertex -> eachVertex.getNodeID() != null)
                .map(eachVertex -> eachVertex.getNodeID())
                .collect(Collectors.toSet());
        if (contentType == ContentType.Alarm) {
            return new SelectionChangedListener.AlarmNodeIdSelection(nodeIds);
        }
        if (contentType == ContentType.Node) {
            return new SelectionChangedListener.IdSelection<>(nodeIds);
        }
        return SelectionChangedListener.Selection.NONE;
    }

    @Override
    public boolean contributesTo(ContentType type) {
        return Sets.newHashSet(ContentType.Alarm, ContentType.Node).contains(type);
    }
}