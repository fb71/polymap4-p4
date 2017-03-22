/* 
 * polymap.org
 * Copyright (C) 2017, the @authors. All rights reserved.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3.0 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package org.polymap.p4.process;

import org.json.JSONArray;
import org.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.vividsolutions.jts.geom.Coordinate;

import org.eclipse.swt.widgets.Composite;

import org.polymap.rap.openlayers.base.OlEvent;
import org.polymap.rap.openlayers.base.OlMap.Event;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public abstract class ClickMapViewer
        extends BaseMapViewer {

    private static final Log log = LogFactory.getLog( ClickMapViewer.class );


    public ClickMapViewer( Composite parent ) {
        super( parent );

        getMap().addEventListener( Event.click, this );
    }

    
    @Override
    public void handleEvent( OlEvent ev ) {
        log.info( "event: " + ev.properties() );
        JSONObject feature = ev.properties().optJSONObject( "feature" );
        if (feature != null) {
            JSONArray json = feature.getJSONArray( "coordinate" );
            double x = json.getDouble( 0 );
            double y = json.getDouble( 1 );
            Coordinate coord = new Coordinate( x, y );
            onClick( coord );
        }
    }
    

    protected abstract void onClick( Coordinate coordinate );
    
}
