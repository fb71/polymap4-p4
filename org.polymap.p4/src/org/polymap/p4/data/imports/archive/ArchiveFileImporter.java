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
package org.polymap.p4.data.imports.archive;

import static org.polymap.rhei.batik.app.SvgImageRegistryHelper.NORMAL24;

import java.util.List;

import java.io.File;
import java.nio.charset.Charset;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.eclipse.core.runtime.IProgressMonitor;

import org.polymap.rhei.batik.toolkit.md.MdToolkit;

import org.polymap.p4.P4Plugin;
import org.polymap.p4.data.imports.ContextIn;
import org.polymap.p4.data.imports.ContextOut;
import org.polymap.p4.data.imports.ImportTempDir;
import org.polymap.p4.data.imports.ImporterPrompt.Severity;
import org.polymap.p4.data.imports.Importer;
import org.polymap.p4.data.imports.ImporterSite;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class ArchiveFileImporter
        implements Importer {

    private static Log log = LogFactory.getLog( ArchiveFileImporter.class );
    
    protected ImporterSite          site;
    
    @ContextIn
    protected MdToolkit             tk;
    
    @ContextIn
    protected File                  file;
    
    @ContextOut
    protected List<File>            result;
    
    protected Charset               charset;                
    

    @Override
    public ImporterSite site() {
        return site;
    }


    @Override
    public void init( @SuppressWarnings("hiding") ImporterSite site, IProgressMonitor monitor ) {
        this.site = site;

        site.icon.set( P4Plugin.images().svgImage( "zip.svg", NORMAL24 ) );
        site.summary.set( "ZIP archive: " + file.getName() );
        site.description.set( "A ZIP archive contains other files. Click to import files from within the archive." );
    }

    
    @Override
    public void createPrompts( IProgressMonitor monitor ) throws Exception {
        // charset prompt
        site.newPrompt( "charset" )
                .summary.put( "Filename encoding" )
                .description.put( "The encoding of the filenames. If unsure use UTF8." )
                .value.put( "UTF8" )
                .severity.put( Severity.VERIFY )
                .extendedUI.put( (prompt,parent) -> {
                    Button btn = new Button( parent, SWT.CHECK );
                    btn.setText( "Ja, das ist gut" );
                    btn.setSelection( prompt.ok.get() );
                    btn.addSelectionListener( new SelectionAdapter() {
                        @Override
                        public void widgetSelected( SelectionEvent ev ) {
                            prompt.ok.set( btn.getSelection() );
                            prompt.value.put( "ISO" );
                        }
                    });
                    return parent;
                });
    }
    
    
    @Override
    public void verify( IProgressMonitor monitor ) {
        try {
            Thread.sleep( 5000 );
        }
        catch (InterruptedException e) {
        }
        result = new ArchiveReader()
                .targetDir.put( ImportTempDir.create() )
                .run( file, monitor );
    }    

    
    @Override
    public Composite createResultViewer( Composite parent ) {
        if (result == null) {
            Label l = new Label( parent, SWT.NONE );
            l.setText( "Unable to import files from: " + file.getName() );            
        }
        else {
            org.eclipse.swt.widgets.List list = new org.eclipse.swt.widgets.List( parent, SWT. NONE );
            result.stream().sorted().forEach( f -> list.add( f.getName() ) );
        }
        return parent;
    }


    @Override
    public void execute( IProgressMonitor monitor ) throws Exception {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }
    
}