/* 
 * polymap.org
 * Copyright (C) 2015, the @autors. All rights reserved.
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
package org.polymap.p4.data.importer.osm;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.polymap.p4.data.importer.ContextIn;
import org.polymap.p4.data.importer.ImporterFactory;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class OsmApiImporterFactory
        implements ImporterFactory {

    private static Log log = LogFactory.getLog( OsmApiImporterFactory.class );
    
    
    @ContextIn
    protected Object                any;
    

    @Override
    public void createImporters( ImporterBuilder builder ) throws Exception {
        if (any == null) {
            builder.newImporter( new OsmApiImporter() );
        }
    }
}
