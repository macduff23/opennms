/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2002-2016 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2016 The OpenNMS Group, Inc.
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

package org.opennms.netmgt.collection.persistence.tcponly;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.NumberFormat;
import java.util.ArrayList;;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;;
import java.util.Map;
import java.util.TreeMap;

import org.opennms.netmgt.collection.api.ByNameComparator;
import org.opennms.netmgt.collection.api.CollectionAttributeType;
import org.opennms.netmgt.collection.api.PersistException;
import org.opennms.netmgt.collection.api.PersistOperationBuilder;
import org.opennms.netmgt.collection.api.ResourceIdentifier;
import org.opennms.netmgt.collection.api.TimeKeeper;
import org.opennms.netmgt.collection.support.DefaultTimeKeeper;
import org.opennms.netmgt.rrd.RrdException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

/**
 * The Class TcpPersistOperationBuilder.
 * 
 * @author <a href="mailto:agalue@opennms.org">Alejandro Galue</a>
 */
public class TcpPersistOperationBuilder implements PersistOperationBuilder {
    /** The Constant LOG. */
    private static final Logger LOG = LoggerFactory.getLogger(TcpPersistOperationBuilder.class);

    private final TcpOutputStrategy m_tcpStrategy;
    private final String m_rrdName;
    private final ResourceIdentifier m_resource;
    private final Map<CollectionAttributeType, Number> m_declarations;
    private final Map<CollectionAttributeType, String> m_str_declarations;
    private TimeKeeper m_timeKeeper = new DefaultTimeKeeper();

    /**
     * <p>Constructor for TcpPersistOperationBuilder.</p>
     *
     * @param tcpStrategy a {@link org.opennms.netmgt.collection.persistence.tcponly.TcpOutputStrategy} object.
     * @param resource a {@link org.opennms.netmgt.collection.api.ResourceIdentifier} object.
     * @param rrdName a {@link java.lang.String} object.
     */
    public TcpPersistOperationBuilder(TcpOutputStrategy tcpStrategy, ResourceIdentifier resource, String rrdName, boolean dontReorderAttributes) {
        m_tcpStrategy = tcpStrategy;
        m_resource = resource;
        m_rrdName = rrdName;
        if (dontReorderAttributes) {
            m_declarations = new LinkedHashMap<>();
            m_str_declarations = new LinkedHashMap<>();
        } else {
            m_declarations = new TreeMap<>(new ByNameComparator());
            m_str_declarations = new TreeMap<>(new ByNameComparator());
        }
    }

    /**
     * Gets the name.
     *
     * @return the name
     */
    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    private File getResourceDir(ResourceIdentifier resource) throws FileNotFoundException {
        return new File("/opt/opennms/share/rrd").toPath()
                .resolve(resource.getPath())
                .toFile();
    }

    /**
     * Sets the attribute value.
     *
     * @param attributeType the attribute type
     * @param value the value
     */
    @Override
    public void setAttributeValue(CollectionAttributeType attributeType, Number value) {
        m_declarations.put(attributeType, value);
    }

    /**
     * Sets the attribute metadata.
     *
     * @param metricIdentifier the metric identifier
     * @param name the name
     */
    @Override
    public void setAttributeMetadata(String metricIdentifier, String name) {}

    /**
     * Commit.
     *
     * @throws PersistException the persist exception
     */
    @Override
    public void commit() throws PersistException {
        if (m_declarations.size() == 0 && m_str_declarations.size() == 0) {
            return;
        }

        try {
            final String ownerName = m_resource.getOwnerName();
            final String absolutePath = getResourceDir(m_resource).getAbsolutePath();

            String rrdFile = absolutePath + File.separator + m_rrdName + ".jrb";
            long timestamp = m_timeKeeper.getCurrentTime();
            long time = (timestamp + 500L) / 1000L;

            TcpOutputStrategy strategy = toGenericType(m_tcpStrategy);
            strategy.updateData(ownerName, rrdFile, new Long(time), getValues());
        } catch (FileNotFoundException e) {
            LoggerFactory.getLogger(getClass()).warn("Could not get resource directory: " + e.getMessage(), e);
            return;
        } catch (Exception e) {
            throw new PersistException(e);
        }
    }

    private List<Double> getValues() {
	List<Double> values = new ArrayList<Double>();
        for (Iterator<CollectionAttributeType> iter = m_declarations.keySet().iterator(); iter.hasNext();) {
            CollectionAttributeType attrDef = iter.next();
            Number value = m_declarations.get(attrDef);
            Double dblValue = new Double(value.doubleValue());
            values.add(dblValue);
        }
        return values;
    }

    @SuppressWarnings("unchecked")
    private static TcpOutputStrategy toGenericType(TcpOutputStrategy tcpStrategy) {
        Assert.notNull(tcpStrategy);
        return (TcpOutputStrategy) tcpStrategy;
    }

    /**
     * <p>getTimeKeeper</p>
     *
     * @return a {@link org.opennms.netmgt.collection.api.TimeKeeper} object.
     */
    public TimeKeeper getTimeKeeper() {
        return m_timeKeeper;
    }

    /**
     * <p>setTimeKeeper</p>
     *
     * @param timeKeeper a {@link org.opennms.netmgt.collection.api.TimeKeeper} object.
     */
    public void setTimeKeeper(TimeKeeper timeKeeper) {
        m_timeKeeper = timeKeeper;
    }

}
