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

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.File;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.io.rasterwriter.OmsRasterWriter;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.action.Action;

import org.eclipse.core.runtime.IProgressMonitor;

import org.polymap.core.catalog.IUpdateableMetadataCatalog.Updater;
import org.polymap.core.catalog.resolve.IResolvableInfo;
import org.polymap.core.data.process.ui.FieldViewerSite;
import org.polymap.core.data.process.ui.OutputFieldConsumer;
import org.polymap.core.data.raster.catalog.GridServiceResolver;
import org.polymap.core.project.ILayer;
import org.polymap.core.runtime.UIJob;
import org.polymap.core.ui.FormDataFactory;
import org.polymap.core.ui.FormLayoutFactory;
import org.polymap.core.ui.UIUtils;

import org.polymap.rhei.batik.toolkit.SimpleDialog;

import org.polymap.model2.query.Expressions;
import org.polymap.p4.P4Plugin;
import org.polymap.p4.catalog.AllResolver;
import org.polymap.p4.project.ProjectRepository;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class LayerRasterConsumer
        extends OutputFieldConsumer {

    private static final Log log = LogFactory.getLog( LayerRasterConsumer.class );
    
    private Button          btn;

    private Text            text;


    @Override
    public String label() {
        return "Layer";
    }

    
    @Override
    public boolean init( @SuppressWarnings( "hiding" ) FieldViewerSite site ) {
        return super.init( site ) && site.fieldInfo.get().type.get().isAssignableFrom( GridCoverage2D.class );
    }

    
    @Override
    public void createContents( Composite parent ) {
        parent.setLayout( FormLayoutFactory.defaults().spacing( 5 ).create() );
        
        btn = new Button( parent, SWT.PUSH );
        btn.setText( "CREATE..." );
        btn.setToolTipText( "Create a new layer with the given name" );
        btn.addSelectionListener( UIUtils.selectionListener( ev -> {
            consume();
        }));
        
        text = new Text( parent, SWT.BORDER );
        text.setToolTipText( "The name of the layer to create" );
        text.addModifyListener( ev -> {
            try {
                boolean layerExists = ProjectRepository.unitOfWork()
                        .query( ILayer.class )
                        .where( Expressions.eq( ILayer.TYPE.label, text.getText() ) )
                        .execute().size() > 0;
                if (!isBlank( text.getText() ) && !layerExists ) {
                    btn.setEnabled( true );
                    btn.setToolTipText( "Create a new layer with the given name" );
                }
                else {
                    btn.setEnabled( false );
                    btn.setToolTipText( "Layer name already exists" );
                }
            }
            catch (Exception e) {
                log.warn( "", e );
            } 
        });
        text.setText( site.layer.get().label.get() + "-" + site.moduleInfo.get().title() ); // simpleClassname.get() );
        text.forceFocus();
        
        FormDataFactory.on( text ).fill().right( 100, -80 );
        FormDataFactory.on( btn ).fill().left( text );
    }
    
    
    public void consume() {
        String layerName = text.getText();
        UIJob job = new UIJob( LayerRasterConsumer.class.getSimpleName() ) {
            @Override
            protected void runWithException( IProgressMonitor monitor ) throws Exception {
                execute( layerName,  monitor );
            }
        };
        job.schedule();
        
        SimpleDialog dialog = new SimpleDialog();
        dialog.title.put( "Creating new layer" );
        dialog.setContents( dialogParent -> {
            new Label( dialogParent, SWT.NONE ).setText( "Running..." );
        });
        dialog.addAction( new Action( "STOP" ) {
            @Override
            public void run() {
            }
        });
        dialog.open();        
    }
    
    
    protected void execute( String layerName, IProgressMonitor monitor ) throws Exception {
        GridCoverage2D resultRaster = site.getFieldValue();
        log.info( "Result: " + resultRaster.toString() );
        
        File targetDir = new File( P4Plugin.gridStoreDir(), layerName );
        File rasterFile = new File( targetDir, layerName + ".tiff" );
        targetDir.mkdirs();
        log.info( "Raster file: " + rasterFile.getAbsolutePath() );
        
        OmsRasterWriter writer = new OmsRasterWriter();
        //writer.pm = pm;
        writer.inRaster = resultRaster;
        writer.file = rasterFile.getAbsolutePath();  
        writer.process();
        
        createCatalogEntry( rasterFile, monitor );
    }
    
    
    protected void createCatalogEntry( File rasterFile, IProgressMonitor monitor ) throws Exception {
        try (
            Updater update = P4Plugin.localCatalog().prepareUpdate()
        ){
            update.newEntry( metadata -> {
                metadata.setTitle( FilenameUtils.getBaseName( rasterFile.getName() ) );
                
//                metadata.setDescription( gridReader.get().getFormat().getName() );
//                if (service.getKeywords() != null) {
//                    metadata.setKeywords( service.getKeywords() );
//                }
                
                metadata.setType( "Grid" );
//                metadata.setFormats( Sets.newHashSet( gridReader.get().getFormat().getName() ) );

                String url = rasterFile.toURI().toString();
                metadata.setConnectionParams( GridServiceResolver.createParams( url ) );
                
                try {
                    IResolvableInfo resolvable = AllResolver.instance().resolve( metadata, monitor );
                    log.info( "Resolved: " + resolvable) ;
                }
                catch (Exception e) {
                    throw new RuntimeException( e );
                }
            });
            update.commit();
        }
    }

}
