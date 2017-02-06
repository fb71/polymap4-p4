/*
 * polymap.org Copyright (C) 2015 individual contributors as indicated by the
 * 
 * @authors tag. All rights reserved.
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
package org.polymap.p4.data.importer.osm;

import static org.polymap.rhei.batik.app.SvgImageRegistryHelper.NORMAL24;

import java.util.List;

import java.io.File;
import java.io.IOException;

import org.opengis.feature.simple.SimpleFeatureType;

import org.apache.commons.lang3.tuple.Pair;

import org.eclipse.swt.widgets.Composite;

import org.eclipse.core.runtime.IProgressMonitor;

import org.polymap.rhei.batik.toolkit.IPanelToolkit;

import org.polymap.p4.P4Plugin;
import org.polymap.p4.data.importer.ContextIn;
import org.polymap.p4.data.importer.ContextOut;
import org.polymap.p4.data.importer.Importer;
import org.polymap.p4.data.importer.ImporterSite;
import org.polymap.p4.data.importer.prompts.CharsetPrompt;
import org.polymap.p4.data.importer.shapefile.ShpFeatureTableViewer;

/**
 * @author Joerg Reichert <joerg@mapzone.io>
 *
 */
public class OsmXmlFileImporter
        implements Importer {

    private static int                        ELEMENT_PREVIEW_LIMIT = 100;

    private static int                        ELEMENT_IMPORT_LIMIT  = 50000;

    @ContextIn
    protected File                            file;

    @ContextOut
    protected OsmXmlIterableFeatureCollection features;

    protected ImporterSite                    site;

    private Exception                         exception;

    private CharsetPrompt                     charsetPrompt;

    private TagFilterPrompt                   tagPrompt;

    private int                               totalCount            = -1;


    /*
     * (non-Javadoc)
     * 
     * @see org.polymap.p4.data.importer.Importer#site()
     */
    @Override
    public ImporterSite site() {
        return site;
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.polymap.p4.data.importer.Importer#init(org.polymap.p4.data.importer.ImporterSite
     * , org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void init( ImporterSite aSite, IProgressMonitor monitor ) throws Exception {
        this.site = aSite;

        site.icon.set( P4Plugin.images().svgImage( "file-multiple.svg", NORMAL24 ) );
        site.summary.set( "OSM-Import" );
        site.description.set( "Importing an OSM XML file." );
        site.terminal.set( true );
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.polymap.p4.data.importer.Importer#createPrompts(org.eclipse.core.runtime
     * .IProgressMonitor)
     */
    @Override
    public void createPrompts( IProgressMonitor monitor ) throws Exception {
        tagPrompt = new TagFilterPrompt( site );
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.polymap.p4.data.importer.Importer#verify(org.eclipse.core.runtime.
     * IProgressMonitor)
     */
    @Override
    public void verify( IProgressMonitor monitor ) {
        if (tagPrompt.isOk()) {
            try {
                List<Pair<String,String>> tagFilters = tagPrompt.selection();
                features = new OsmXmlIterableFeatureCollection( "osm", file, tagFilters );
                totalCount = features.size();
                if (totalCount > ELEMENT_IMPORT_LIMIT) {
                    throw new IndexOutOfBoundsException( "Your query results in more than " + ELEMENT_IMPORT_LIMIT
                            + " elements. Please provide a smaller OSM extract or refine your tag filters." );
                }
                if (features.iterator().hasNext() && features.getException() == null) {
                    site.ok.set( true );
                }
                else {
                    exception = features.getException();
                    site.ok.set( false );
                }
            }
            catch (IOException | IndexOutOfBoundsException e) {
                site.ok.set( false );
                exception = e;
            }
        }
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.polymap.p4.data.importer.Importer#createResultViewer(org.eclipse.swt.widgets
     * .Composite, org.polymap.rhei.batik.toolkit.IPanelToolkit)
     */
    @Override
    public void createResultViewer( Composite parent, IPanelToolkit toolkit ) {
        if (tagPrompt.isOk()) {
            if (exception != null) {
                toolkit.createFlowText( parent,
                        "\nUnable to read the data.\n\n" + "**Reason**: " + exception.getMessage() );
            }
            else {
                SimpleFeatureType schema = (SimpleFeatureType)features.getSchema();
                ShpFeatureTableViewer table = new ShpFeatureTableViewer( parent, schema );
                if (totalCount > ELEMENT_PREVIEW_LIMIT) {
                    toolkit.createFlowText( parent, "\nShowing " + ELEMENT_PREVIEW_LIMIT + " items of totally found "
                            + totalCount + " elements." );
                    features.setLimit( ELEMENT_PREVIEW_LIMIT );
                }
                table.setContentProvider( new FeatureLazyContentProvider( features ) );
                table.setInput( features );
            }
        }
        else {
            toolkit.createFlowText( parent,
                    "\nOSM Importer is currently deactivated" );
        }
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.polymap.p4.data.importer.Importer#execute(org.eclipse.core.runtime.
     * IProgressMonitor)
     */
    @Override
    public void execute( IProgressMonitor monitor ) throws Exception {
        // create all params for contextOut
        // all is done in verify
        if (totalCount > ELEMENT_IMPORT_LIMIT) {
            features.setLimit( ELEMENT_IMPORT_LIMIT );
        }
    }
}
