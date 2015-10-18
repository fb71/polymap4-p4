/*
 * polymap.org 
 * Copyright (C) 2015 individual contributors as indicated by the @authors tag. 
 * All rights reserved.
 * 
 * This is free software; you can redistribute it and/or modify it under the terms of
 * the GNU Lesser General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 */
package org.polymap.p4.style.daos;

import org.geotools.styling.builder.NamedLayerBuilder;
import org.geotools.styling.builder.StyleBuilder;

/**
 * @author Joerg Reichert <joerg@mapzone.io>
 *
 */
public class StyleIdentToSLDVisitor
        extends AbstractStyleToSLDVisitor {

    private final StyleIdentDao styleIdentDao;


    public StyleIdentToSLDVisitor( StyleIdentDao styleIdentDao ) {
        this.styleIdentDao = styleIdentDao;
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.polymap.p4.style.daos.AbstractStyleToSLDVisitor#fillSLD(org.geotools.styling
     * .builder.StyledLayerDescriptorBuilder)
     */
    @Override
    public void fillSLD( SLDBuilder builder ) {
        if(styleIdentDao.getName() != null | styleIdentDao.getTitle() != null ) {
            NamedLayerBuilder namedLayer = builder.namedLayer();
            if(styleIdentDao.getName() != null) {
                namedLayer.name( styleIdentDao.getName() );
            }
            if(styleIdentDao.getTitle() != null) {
                StyleBuilder styleBuilder = builder.style(namedLayer);
                styleBuilder.title( styleIdentDao.getTitle() );
                // have to call this here as otherwise the title wouldn't be set
                builder.featureTypeStyle(styleBuilder);
            }
        }
    }
}
