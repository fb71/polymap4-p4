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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Throwables;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jface.layout.RowDataFactory;
import org.eclipse.jface.layout.RowLayoutFactory;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

import org.polymap.core.data.process.FieldInfo;
import org.polymap.core.data.process.ModuleInfo;
import org.polymap.core.data.process.ui.FieldIO;
import org.polymap.core.data.process.ui.FieldViewer;
import org.polymap.core.data.process.ui.FieldViewerSite;
import org.polymap.core.project.ILayer;
import org.polymap.core.runtime.UIJob;
import org.polymap.core.runtime.UIThreadExecutor;
import org.polymap.core.ui.ColumnDataFactory;
import org.polymap.core.ui.ColumnLayoutFactory;
import org.polymap.core.ui.FormDataFactory;
import org.polymap.core.ui.FormLayoutFactory;
import org.polymap.core.ui.UIUtils;

import org.polymap.rhei.batik.Context;
import org.polymap.rhei.batik.PanelIdentifier;
import org.polymap.rhei.batik.Scope;
import org.polymap.rhei.batik.app.SvgImageRegistryHelper;
import org.polymap.rhei.batik.toolkit.DefaultToolkit;
import org.polymap.rhei.batik.toolkit.IPanelSection;

import org.polymap.p4.P4Panel;
import org.polymap.p4.P4Plugin;

/**
 * Processes a given module ({@link JGTModel}). 
 *
 * @author Falko Bräutigam
 */
public class ModuleProcessPanel
        extends P4Panel {

    private static final Log log = LogFactory.getLog( ModuleProcessPanel.class );

    public static final PanelIdentifier ID = PanelIdentifier.parse( "processModule" );

    static {
        FieldIO.ALL.add( LayerRasterSupplier.class );
        FieldIO.ALL.add( LayerRasterConsumer.class );
    }

    /** Inbound: */
    @Scope( P4Plugin.Scope )
    private Context<ILayer>         layer;

    /** Inbound: */
    @Scope( P4Plugin.Scope )
    private Context<ModuleInfo>     moduleInfo;

    private JGTModel                module;

    private IPanelSection           outputSection;

    private IPanelSection           inputSection;

    private Composite               buttons;
    
    private Button                  startBtn;

    private Composite               parent;
    
    private List<FieldViewer>       inputFields = new ArrayList();

    private UIJob                   job;

    
    @Override
    public void init() {
        super.init();
        module = moduleInfo.get().createInstance();
    }

    
    @Override
    public void dispose() {
        if (job != null) {
            job.cancelAndInterrupt();
            job = null;
        }
    }


    @Override
    public void createContents( @SuppressWarnings( "hiding" ) Composite parent ) {
        this.parent = parent;
        site().title.set( moduleInfo.get().title() );
        
        parent.setLayout( FormLayoutFactory.defaults().spacing( 8 ).margins( 2, 8 ).create() );
        
        inputSection = createInputSection();
        FormDataFactory.on( inputSection.getControl() ).fill().noBottom().width( 500 );

        buttons = createButtonsSection();
        FormDataFactory.on( buttons ).fill().top( inputSection.getControl() ).noBottom();

//        outputSection = createOutputSection();
//        FormDataFactory.on( outputSection.getControl() ).fill().top( buttons ).noBottom();
    }


    protected IPanelSection createInputSection() {
        IPanelSection section = tk().createPanelSection( parent, "Input", SWT.BORDER );
        section.getBody().setLayout( ColumnLayoutFactory.defaults().columns( 1, 1 ).margins( 0, 8 ).spacing( 10 ).create() );

        Label label = tk().createLabel( section.getBody(), moduleInfo.get().description.get().orElse( "No description." ), SWT.WRAP );
        label.setLayoutData( ColumnDataFactory.defaults().widthHint( 300 ).create() );
        label.setEnabled( false );
        
        AtomicBoolean isFirst = new AtomicBoolean( true );
        for (FieldInfo fieldInfo : moduleInfo.get().inputFields()) {
            // skip
            if (IJGTProgressMonitor.class.isAssignableFrom( fieldInfo.type.get() )
                    || !fieldInfo.description.get().isPresent()) {
                continue;
            }
            // separator
            if (!isFirst.getAndSet( false )) {
                Label sep = new Label( section.getBody(), SWT.SEPARATOR|SWT.HORIZONTAL );
                UIUtils.setVariant( sep, DefaultToolkit.CSS_SECTION_SEPARATOR );  // XXX
            }
            // field
            FieldViewer fieldViewer = new FieldViewer( new FieldViewerSite()
                    .moduleInfo.put( moduleInfo.get() )
                    .module.put( module )
                    .fieldInfo.put( fieldInfo )
                    .layer.put( layer.get() ) );
            fieldViewer.createContents( section.getBody() )
                    .setLayoutData( ColumnDataFactory.defaults().widthHint( 300 ).create() );
            inputFields.add( fieldViewer );
        }
        
        return section;
    }


    protected IPanelSection createOutputSection() {
        IPanelSection section = tk().createPanelSection( parent, "Output", SWT.BORDER );
        return section;
    }


    protected Composite createButtonsSection() {
        Composite section = tk().createComposite( parent );
        section.setLayout( RowLayoutFactory.fillDefaults().spacing( 8 ).margins( 2, 2 ).fill( true ).justify( true ).create() );
        
        startBtn = tk().createButton( section, "", SWT.PUSH );
        startBtn.setLayoutData( RowDataFactory.swtDefaults().hint( 150, SWT.DEFAULT ).create() );
        startBtn.addSelectionListener( UIUtils.selectionListener( ev -> {
            if (job == null) {
                startProcess();
            } else {
                stopProcess();
                //startBtn.setEnabled( false );
            }
        }));
        updateStartBtn();
        return section;
    }
    
    
    protected void updateStartBtn() {
        if (job != null && job.getState() != Job.NONE) {
            startBtn.setText( "STOP" );
            startBtn.setImage( P4Plugin.images().svgImage( "stop-circle-outline.svg", SvgImageRegistryHelper.WHITE24 ) );
        }
        else {
            startBtn.setText( "RUN" );
            startBtn.setImage( P4Plugin.images().svgImage( "play-circle-outline.svg", SvgImageRegistryHelper.WHITE24 ) );
        }
    }
    
    
    protected void stopProcess() {
        if (job != null) {
            module.pm.setCanceled( true );
            job.cancelAndInterrupt();
        }
        updateStartBtn();
        UIUtils.disposeChildren( outputSection.getBody() );
        Label msg = new Label( outputSection.getBody(), SWT.NONE );
        msg.setText( "Cancel requested..." );
        parent.layout( true, true );
    }
    
    
    protected void startProcess() {
        if (outputSection == null) {
            outputSection = createOutputSection();
            FormDataFactory.on( outputSection.getControl() ).fill().top( buttons ).noBottom();
        }
        else {
            UIUtils.disposeChildren( outputSection.getBody() );
        }

        module.pm = new ProcessProgressMonitor( outputSection.getBody() );
        parent.layout( true, true );
        
        job = new UIJob( "Processing" ) {
            @Override
            protected void runWithException( IProgressMonitor monitor ) throws Exception {
                moduleInfo.get().execute( module, null );
            }
        };
        job.addJobChangeListenerWithContext( new JobChangeAdapter() {
            @Override
            public void done( IJobChangeEvent ev ) {
                UIThreadExecutor.async( () -> {
                    if (!outputSection.getBody().isDisposed()) {
                        job = null;
                        updateStartBtn();
                        
                        UIUtils.disposeChildren( outputSection.getBody() );
                        if (ev.getResult().isOK()) {
                            fillOutputFields();
                        }
                        else {
                            Throwable e = ev.getResult().getException();
                            new Label( outputSection.getBody(), SWT.WRAP ).setText( e != null 
                                    ? Throwables.getRootCause( e ).getMessage() 
                                    : ev.getResult().getMessage() );
                        }
                        parent.layout( true, true );
                    }
                });
            }
        });
        job.schedule();
        updateStartBtn();
    }


    protected void fillOutputFields() {
        outputSection.getBody().setLayout( ColumnLayoutFactory.defaults().columns( 1, 1 ).margins( 0, 8 ).spacing( 10 ).create() );

        AtomicBoolean isFirst = new AtomicBoolean( true );
        for (FieldInfo fieldInfo : moduleInfo.get().outputFields()) {
            // separator
            if (!isFirst.getAndSet( false )) {
                Label sep = new Label( outputSection.getBody(), SWT.SEPARATOR|SWT.HORIZONTAL );
                UIUtils.setVariant( sep, DefaultToolkit.CSS_SECTION_SEPARATOR );  // XXX
            }
            // field
            FieldViewer fieldViewer = new FieldViewer( new FieldViewerSite()
                    .moduleInfo.put( moduleInfo.get() )
                    .module.put( module )
                    .fieldInfo.put( fieldInfo )
                    .layer.put( layer.get() ) );
            fieldViewer.createContents( outputSection.getBody() )
                    .setLayoutData( ColumnDataFactory.defaults().widthHint( 300 ).create() );
            inputFields.add( fieldViewer );
        }
    }
    
}
