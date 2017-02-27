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

import static org.apache.commons.lang3.StringUtils.capitalize;

import java.io.File;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.io.rasterwriter.OmsRasterWriter;
import org.jgrasstools.gears.libs.modules.JGTModel;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.layout.RowLayoutFactory;

import org.eclipse.core.runtime.IProgressMonitor;

import org.polymap.core.catalog.IUpdateableMetadataCatalog.Updater;
import org.polymap.core.catalog.resolve.IResolvableInfo;
import org.polymap.core.data.process.FieldInfo;
import org.polymap.core.data.process.ModuleInfo;
import org.polymap.core.data.raster.catalog.GridServiceResolver;
import org.polymap.core.project.ILayer;
import org.polymap.core.runtime.UIJob;
import org.polymap.core.ui.FormDataFactory;
import org.polymap.core.ui.FormLayoutFactory;
import org.polymap.core.ui.UIUtils;

import org.polymap.rhei.batik.Context;
import org.polymap.rhei.batik.PanelIdentifier;
import org.polymap.rhei.batik.Scope;
import org.polymap.rhei.batik.app.SvgImageRegistryHelper;
import org.polymap.rhei.batik.toolkit.IPanelSection;

import org.polymap.p4.P4Panel;
import org.polymap.p4.P4Plugin;
import org.polymap.p4.catalog.AllResolver;
import org.polymap.p4.layer.RasterLayer;

/**
 * Processes a given module ({@link JGTModel}). 
 *
 * @author Falko Bräutigam
 */
public class ProcessModulePanel
        extends P4Panel {

    private static final Log log = LogFactory.getLog( ProcessModulePanel.class );

    public static final PanelIdentifier ID = PanelIdentifier.parse( "processModule" );

    /** Inbound: */
    @Scope( P4Plugin.Scope )
    private Context<ILayer>         layer;

    /** Inbound: */
    @Scope( P4Plugin.Scope )
    private Context<Class<JGTModel>> moduleType;

    private ModuleInfo              moduleInfo;
    
    private JGTModel                module;

    private IPanelSection           outputSection;

    private IPanelSection           inputSection;

    private Composite               parent;
    
    
    @Override
    public void init() {
        super.init();
        moduleInfo = ModuleInfo.of( moduleType.get() );
        module = moduleInfo.createInstance();

        for (FieldInfo info : moduleInfo.inputFields()) {
            log.info( "Input field: " + info );
        }
    }

    
    @Override
    public void createContents( @SuppressWarnings( "hiding" ) Composite parent ) {
        this.parent = parent;
        site().title.set( capitalize( moduleInfo.name.get().orElse( moduleInfo.simpleClassname.get() ) ) );
        
        parent.setLayout( FormLayoutFactory.defaults().spacing( 8 ).margins( 2, 8 ).create() );
        
        inputSection = createInputSection();
        FormDataFactory.on( inputSection.getControl() ).fill().noBottom().width( 500 );

        Composite buttons = createButtonsSection();
        FormDataFactory.on( buttons ).fill().top( inputSection.getControl() ).noBottom();

        outputSection = createOutputSection();
        FormDataFactory.on( outputSection.getControl() ).fill().top( buttons ).noBottom();
    }


    protected IPanelSection createInputSection() {
        IPanelSection section = tk().createPanelSection( parent, "Input", SWT.BORDER );
        tk().createFlowText( section.getBody(), moduleInfo.description.get().orElse( "No description." ) );
        return section;
    }


    protected IPanelSection createOutputSection() {
        IPanelSection section = tk().createPanelSection( parent, "Output", SWT.BORDER );
        return section;
    }


    protected Composite createButtonsSection() {
        Composite section = tk().createComposite( parent );
        section.setLayout( RowLayoutFactory.fillDefaults().spacing( 8 ).margins( 2, 2 ).fill( true ).justify( true ).create() );
        Button startBtn = tk().createButton( section, "Run", SWT.PUSH );
        startBtn.setImage( P4Plugin.images().svgImage( "play-circle-outline.svg", SvgImageRegistryHelper.WHITE24 ) );
        startBtn.addSelectionListener( UIUtils.selectionListener( ev -> execute() ) );
        return section;
    }
    
    
    protected void execute() {
        RasterLayer.of( layer.get() ).thenAccept( rl -> {
            try {
                IProgressMonitor monitor = UIJob.monitorOfThread();
                execute( rl.get(), monitor );
            }
            catch (Exception e) {
                log.info( "", e );
                tk().createLabel( outputSection.getBody(), e.getLocalizedMessage(), SWT.WRAP );            
            }            
        })
        .exceptionally( e -> {
            log.warn( "", e );
            tk().createLabel( parent, "Unable to data from layer." );
            return null;
        });
    }


    protected void execute( RasterLayer rasterLayer, IProgressMonitor monitor ) throws Exception {
        // setup input fields
        FieldInfo gridIn = moduleInfo.inputFields().firstMatch( f -> f.isAssignableFrom( GridCoverage2D.class ) ).get();
        gridIn.setValue( module, rasterLayer.gridCoverage() );
        
        // do it
        moduleInfo.execute( module, null );
        
        // handle output
        FieldInfo gridOut = moduleInfo.outputFields().firstMatch( f -> f.isAssignableFrom( GridCoverage2D.class ) ).get();
        GridCoverage2D resultRaster = gridOut.getValue( module );
        log.info( "Result: " + resultRaster.toString() );
        
        String name = layer.get().label.get() + "-" + moduleInfo.simpleClassname.get();
        File targetDir = new File( P4Plugin.gridStoreDir(), name );
        File rasterFile = new File( targetDir, name + ".tiff" );
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
