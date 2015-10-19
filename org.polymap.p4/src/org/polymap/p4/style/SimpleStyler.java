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
package org.polymap.p4.style;

import java.util.List;

import org.eclipse.swt.widgets.Composite;

import org.polymap.model2.CollectionProperty;

/**
 * Der SimpleStyler müsste alles enthalten was du bisher gemacht hast, also die
 * normalen Symbolizer.
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class SimpleStyler
        extends Styler {

    protected CollectionProperty<SymbolizerStyler>  symbolizers;
    
    
    @Override
    public Composite createContents( Composite parent ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }


    @Override
    public Object buildSLD( List<Styler> children ) {
        Object result = null;
        for (SymbolizerStyler symbolizer : symbolizers) {
            Object symbolizerSLD = symbolizer.buildSLD();
            
            // XXX mit result vereinen
            
        }
        return result;
    }
    
}
