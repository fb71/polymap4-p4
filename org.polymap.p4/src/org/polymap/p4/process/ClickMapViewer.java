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

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.json.JSONArray;
import org.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.vividsolutions.jts.geom.Coordinate;

import org.eclipse.swt.widgets.Composite;

import org.polymap.core.mapeditor.MapViewer;
import org.polymap.core.project.ILayer;
import org.polymap.core.project.IMap;
import org.polymap.core.ui.UIUtils;

import org.polymap.p4.map.ProjectContentProvider;
import org.polymap.p4.map.ProjectLayerProvider;
import org.polymap.p4.project.ProjectRepository;
import org.polymap.rap.openlayers.base.OlEvent;
import org.polymap.rap.openlayers.base.OlMap.Event;
import org.polymap.rap.openlayers.control.MousePositionControl;
import org.polymap.rap.openlayers.control.ScaleLineControl;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public abstract class ClickMapViewer
        extends MapViewer<ILayer> {

    private static final Log log = LogFactory.getLog( ClickMapViewer.class );


    public ClickMapViewer( Composite parent ) {
        super( parent );

        // XXX ???
        IMap map = ProjectRepository.unitOfWork().entity( IMap.class, ProjectRepository.ROOT_MAP_ID );
        
        // triggers {@link MapViewer#refresh()} on {@link ProjectNodeCommittedEvent} 
        contentProvider.set( new ProjectContentProvider() );
        layerProvider.set( new ProjectLayerProvider() );

        ReferencedEnvelope mapMaxExtent = map.maxExtent();
        log.info( "maxExtent: " + mapMaxExtent );
        maxExtent.set( mapMaxExtent );

        addMapControl( new MousePositionControl() );
        addMapControl( new ScaleLineControl() );
        
        setInput( map );
        getControl().moveBelow( null );
        getControl().setBackground( UIUtils.getColor( 0xff, 0xff, 0xff ) );

        parent.getDisplay().timerExec( 500, () -> {
            mapExtent.set( mapMaxExtent );
        });

        getMap().addEventListener( Event.click, this );
    }

    
    @Override
    public void handleEvent( OlEvent ev ) {
        log.info( "event: " + ev.properties() );
        JSONObject feature = ev.properties().optJSONObject( "feature" );
        if (feature != null) {
            JSONArray coord = feature.getJSONArray( "coordinate" );
            double x = coord.getDouble( 0 );
            double y = coord.getDouble( 1 );

            onClick( new Coordinate( x, y ) );
        }
    }
    

    protected abstract void onClick( Coordinate coordinate );

    
}
