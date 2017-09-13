/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2017-2017 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2017 The OpenNMS Group, Inc.
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

package org.opennms.netmgt.collection.streaming.config;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@XmlRootElement(name="package")
@XmlAccessorType(XmlAccessType.NONE)
public class Package implements Serializable {

    /**
     * Name or identifier for this package.
     */
    @XmlAttribute(name="name")
    private String name;

    /**
     * A rule which addresses belonging to this package must pass. This
     * package is applied only to addresses that pass this filter.
     */
    @XmlElement(name="filter")
    private Filter filter;

    /**
     * RRD parameters for response time data.
     */
    @XmlElement(name="rrd")
    private Rrd rrd;

    @XmlElement(name="adapter")
    private List<Adapter> adapters = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    public Rrd getRrd() {
        return rrd;
    }

    public void setRrd(Rrd rrd) {
        this.rrd = rrd;
    }

    public List<Adapter> getAdapters() {
        return adapters;
    }

    public void setAdapters(List<Adapter> adapters) {
        this.adapters = adapters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Package aPackage = (Package) o;
        return Objects.equals(name, aPackage.name) &&
                Objects.equals(filter, aPackage.filter) &&
                Objects.equals(rrd, aPackage.rrd) &&
                Objects.equals(adapters, aPackage.adapters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, filter, rrd, adapters);
    }

    @Override
    public String toString() {
        return "Package{" +
                "name='" + name + '\'' +
                ", filter=" + filter +
                ", rrd=" + rrd +
                ", adapters=" + adapters +
                '}';
    }
}
