/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2007-2015 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2015 The OpenNMS Group, Inc.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Constructs the appropriate TCP output strategy based on the
 * configured system properties.
 *
 * Optionally wraps the strategy with a queue.
 *
 */
public class TcpOutputStrategyFactory implements ApplicationContextAware {
    private static final Logger LOG = LoggerFactory.getLogger(TcpOutputStrategyFactory.class);

    private ApplicationContext m_context;

    private static enum StrategyName {
        simpleTcpOutputStrategy,
        queuingTcpOutputStrategy
    }

    @Override
    public void setApplicationContext(ApplicationContext context) {
        m_context = context;
    }

    /**
     * <p>getStrategy</p>
     *
     * @return a {@link org.opennms.netmgt.collection.persistence.tcponly.TcpOutputStrategy} object.
     */
    @SuppressWarnings("unchecked")
    public TcpOutputStrategy getStrategy() {
        TcpOutputStrategy tcpStrategy = null;
        Boolean useQueue = (Boolean) m_context.getBean("useQueue");

        if (useQueue) {
            tcpStrategy = (TcpOutputStrategy) m_context.getBean(StrategyName.queuingTcpOutputStrategy.toString());
        } else {
            tcpStrategy = (TcpOutputStrategy) m_context.getBean(StrategyName.simpleTcpOutputStrategy.toString());
        }

        if (tcpStrategy == null) {
            throw new IllegalStateException(String.format("Invalid TCP output configuration useQueue: %s", useQueue));
        }

        return tcpStrategy;
    }
}
