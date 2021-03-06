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
package org.polymap.p4.data.importer.ogr;

import java.util.NoSuchElementException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.collection.BaseSimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geojson.feature.FeatureJSON;
import org.opengis.feature.Feature;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.swt.widgets.Composite;

import org.eclipse.core.runtime.IProgressMonitor;

import org.polymap.core.runtime.SubMonitor;
import org.polymap.core.runtime.i18n.IMessages;

import org.polymap.rhei.batik.app.SvgImageRegistryHelper;
import org.polymap.rhei.batik.toolkit.IPanelToolkit;
import org.polymap.rhei.table.FeatureCollectionContentProvider;

import org.polymap.p4.data.importer.ContextIn;
import org.polymap.p4.data.importer.ContextOut;
import org.polymap.p4.data.importer.Importer;
import org.polymap.p4.data.importer.ImporterPlugin;
import org.polymap.p4.data.importer.ImporterSite;
import org.polymap.p4.data.importer.Messages;
import org.polymap.p4.data.importer.prompts.CrsPrompt;
import org.polymap.p4.data.importer.prompts.SchemaNamePrompt;
import org.polymap.p4.data.importer.shapefile.ShpFeatureTableViewer;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class GeojsonOgrImporter
        implements Importer {

    private static final Log log = LogFactory.getLog( GeojsonOgrImporter.class );
    
    private static final IMessages i18n = Messages.forPrefix( "ImporterOgr" );

    private ImporterSite        site;

    @ContextIn
    protected File              f;

    @ContextOut
    private FeatureCollection   features;

    private Exception           exc;

    private SchemaNamePrompt    schemaNamePrompt;

    private CrsPrompt           crsPrompt;

    
    @Override
    public void init( ImporterSite newSite, IProgressMonitor monitor ) throws Exception {
        this.site = newSite;
        site.icon.set( ImporterPlugin.images().svgImage( "file.svg", SvgImageRegistryHelper.NORMAL24 ) );
        site.summary.set( i18n.get( "summary", f.getName() ) );
        site.description.set( i18n.get( "description") );
        site.terminal.set( true );
    }

    @Override
    public ImporterSite site() {
        return site;
    }


    @Override
    public void createPrompts( IProgressMonitor monitor ) throws Exception {
        //crsPrompt = new CrsPrompt( site, defaultCrs() );        
        schemaNamePrompt = new SchemaNamePrompt( site, FilenameUtils.getBaseName( f.getName() ) );
    }


    @Override
    public void verify( IProgressMonitor monitor ) {
        try {
            // translate to json
            monitor.beginTask( "Verify", 4 );
            File json = GeojsonOgrTransformer.translate( f, new SubMonitor( monitor, 1 ) );
            
            // XXX check SRS style
            monitor.subTask( "checking SRS style" );
            File json2 = new File( json.getParentFile(), json.getName() + ".srs-checked" );
            try (
                BufferedReader reader = new BufferedReader( new FileReader( json ) );
                BufferedWriter writer = new BufferedWriter( new FileWriter( json2 ) );
            ){
                String line = null;
                while ((line = reader.readLine()) != null) {
                    String replaced = StringUtils.replace( line, "urn:ogc:def:crs:OGC:1.3:CRS84", "EPSG:4326" );
                    writer.write( replaced );
                }
            }
            monitor.worked( 1 );
                        
            monitor.subTask( "reading schema" );
            features = new JsonFeatureCollection( json2 );
            log.info( "Features: " + features.size() + " : " + features.getSchema() );
            monitor.worked( 1 );
            
            // checking geometries
            SubMonitor submon = new SubMonitor( monitor, 1 );
            submon.beginTask( "checking all features", IProgressMonitor.UNKNOWN );
            try (
                FeatureIterator it = features.features();
            ){
                while (it.hasNext()) {
                    Feature feature = it.next();
                    // geometry
                    GeometryAttribute geom = feature.getDefaultGeometryProperty();
                    if (geom != null && geom.getValue() == null) {
                        throw new RuntimeException( "Feature has no geometry: " + feature.getIdentifier().getID() );
                    }
                    // other checks...?
                    monitor.worked( 1 );
                }
            }

            site.ok.set( true );
            exc = null;
        }
        catch (IOException e) {
            site.ok.set( false );
            exc = e;
        }
    }


    @Override
    public void createResultViewer( Composite parent, IPanelToolkit tk ) {
        if (exc != null) {
            tk.createFlowText( parent, "\nUnable to read the data.\n\n**Reason**: " + exc.getMessage() );
        }
        else {
            try {
                SimpleFeatureType schema = (SimpleFeatureType)features.getSchema();
                // tk.createFlowText( parent, "Features: *" + features.size() + "*" );
                
                ShpFeatureTableViewer table = new ShpFeatureTableViewer( parent, schema );
                table.setContentProvider( new FeatureCollectionContentProvider() );
                
                // XXX GeoTools shapefile impl does not handle setFirstResult() well
                // so we can just display 100 features :(
                FeatureCollection content = features;  //new MaxFeaturesFeatureCollection( features, 100 );
                table.setInput( content );
            }
            catch (Exception e) {
                log.info( "", e );
                exc = e;
                site.ok.set( false );
                tk.createFlowText( parent, "\nUnable to read the data.\n\n**Reason**: " + exc.getMessage() );
            }
        }
    }


    @Override
    public void execute( IProgressMonitor monitor ) throws Exception {
        features = schemaNamePrompt.retypeFeatures( (SimpleFeatureCollection)features, f.getName() );
    }
    

    /**
     * 
     */
    class JsonFeatureCollection
            extends BaseSimpleFeatureCollection {

        private File                json;
        
        private FeatureJSON         featureJSON;
        
        
        public JsonFeatureCollection( File json ) {
            super( null );
            this.json = json;

            featureJSON = new FeatureJSON();
            featureJSON.setEncodeFeatureCRS( true );
            featureJSON.setEncodeNullValues( true );

            try {
                SimpleFeatureType namespace = featureJSON.readFeatureCollectionSchema( json, false );
                SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
                builder.init( namespace );
                builder.setName( namespace.getName().getLocalPart() );
                builder.setNamespaceURI( (String)null );
                schema = builder.buildFeatureType();
                
                featureJSON.setFeatureType( schema );
            }
            catch (IOException e) {
                throw new RuntimeException( e );
            }
        }

        @Override
        public SimpleFeatureIterator features() {
            try {
                // XXX I don't grok all this simple-whatever type voodoo of geotools, really :(
                FeatureIterator<SimpleFeature> result = featureJSON.streamFeatureCollection( json );
                return new SimpleFeatureIterator() {
                    @Override
                    public SimpleFeature next() throws NoSuchElementException {
                        return result.next();
                    }
                    @Override
                    public boolean hasNext() {
                        return result.hasNext();
                    }
                    @Override
                    public void close() {
                        result.close();
                    }
                };
            }
            catch (IOException e) {
                throw new RuntimeException( e );
            }
        }
    }
    
}
