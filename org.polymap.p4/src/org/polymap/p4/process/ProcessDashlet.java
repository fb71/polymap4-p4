/* 
 * polymap.org
 * Copyright (C) 2016, the @authors. All rights reserved.
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

import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.hortonmachine.modules.demmanipulation.pitfiller.OmsPitfiller;
import org.jgrasstools.hortonmachine.modules.demmanipulation.wateroutlet.OmsWateroutlet;
import org.jgrasstools.hortonmachine.modules.geomorphology.aspect.OmsAspect;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ViewerCell;

import org.polymap.core.data.process.ModuleInfo;
import org.polymap.core.project.ILayer;
import org.polymap.core.runtime.UIThreadExecutor;
import org.polymap.core.ui.FormDataFactory;
import org.polymap.core.ui.FormLayoutFactory;
import org.polymap.core.ui.UIUtils;

import org.polymap.rhei.batik.BatikApplication;
import org.polymap.rhei.batik.Context;
import org.polymap.rhei.batik.PanelSite;
import org.polymap.rhei.batik.Scope;
import org.polymap.rhei.batik.app.SvgImageRegistryHelper;
import org.polymap.rhei.batik.dashboard.DashletSite;
import org.polymap.rhei.batik.dashboard.DefaultDashlet;
import org.polymap.rhei.batik.toolkit.md.ActionProvider;
import org.polymap.rhei.batik.toolkit.md.FunctionalLabelProvider;
import org.polymap.rhei.batik.toolkit.md.ListTreeContentProvider;
import org.polymap.rhei.batik.toolkit.md.MdListViewer;
import org.polymap.rhei.batik.toolkit.md.MdToolkit;

import org.polymap.p4.P4Plugin;
import org.polymap.p4.layer.RasterLayer;

/**
 * Shows a list of available processing modules ({@link JGTModel}). Lets the user
 * choose one to process in {@link ModuleProcessPanel}.
 *
 * @author Falko Bräutigam
 */
public class ProcessDashlet
        extends DefaultDashlet {

    private static final Log log = LogFactory.getLog( ProcessDashlet.class );
    
    /** Inbound: */
    @Scope( P4Plugin.Scope )
    private Context<ILayer>         layer;
    
    /** Outbound: */
    @Scope( P4Plugin.Scope )
    private Context<Class<JGTModel>> selected;
    
    private PanelSite               panelSite;

    private MdToolkit               tk;

    private MdListViewer            list;
    
    
    public ProcessDashlet( PanelSite panelSite ) {
        this.panelSite = panelSite;
    }

    @Override
    public void init( DashletSite site ) {
        super.init( site );
        site.title.set( "Processing" );
        this.tk = (MdToolkit)getSite().toolkit();                    
        //site.constraints.get().add( new MinHeightConstraint( 600, 1 ) );
    }



    @Override
    public void createContents( Composite parent ) {
        // default message
        parent.setLayout( FormLayoutFactory.defaults().margins( 8, 0 ).create() );
        FormDataFactory.on( tk.createLabel( parent, 
                "Connecting data source of this layer...<br/>(Feature/Vector processing is not supported yet)", SWT.WRAP ) )
                .fill().noBottom().height( 45 );
        parent.layout( true, true );

        // RasterLayer?
        RasterLayer.of( layer.get() ).thenAccept( rl -> {
            UIThreadExecutor.async( () -> {
                if (rl.isPresent()) {
                    UIUtils.disposeChildren( parent );
                    createModuleList( parent );
                    FormDataFactory.on( list.getControl() ).fill().height( 400 );
                }
                parent.getParent().getParent().layout( true, true );
            });
        })
        .exceptionally( e -> {
            log.warn( "", e );
            tk.createLabel( parent, "Unable to data from layer." );
            return null;
        });
    }
    
    
    protected void createModuleList( Composite parent ) {
        Class[] MODULES = {OmsAspect.class, OmsWateroutlet.class, OmsPitfiller.class};
        
        list = tk.createListViewer( parent, SWT.SINGLE, SWT.FULL_SELECTION );
        // first line
        list.firstLineLabelProvider.set( FunctionalLabelProvider.of( cell -> {
            ModuleInfo info = ModuleInfo.of( (Class)cell.getElement() );
            cell.setText( info.title() );            
        }));
        // second line
        list.secondLineLabelProvider.set( FunctionalLabelProvider.of( cell -> {
            ModuleInfo info = ModuleInfo.of( (Class)cell.getElement() );
            cell.setText( info.description.get().orElse( "..." ) );            
        }));
        // icon
        list.iconProvider.set( FunctionalLabelProvider.of( cell -> 
            cell.setImage( P4Plugin.images().svgImage( "play-circle-outline.svg", P4Plugin.TOOLBAR_ICON_CONFIG ) ) ) );
        // arrow right
        list.firstSecondaryActionProvider.set( new ActionProvider() {
            @Override
            public void update( ViewerCell cell ) {
                cell.setImage( P4Plugin.images().svgImage( "chevron-right.svg", SvgImageRegistryHelper.NORMAL24 ) );
            }
            @Override
            public void perform( MdListViewer viewer, Object elm ) {
            }
        });
        list.addOpenListener( ev -> {
            selected.set( (Class<JGTModel>)((IStructuredSelection)list.getSelection()).getFirstElement() );
            BatikApplication.instance().getContext().openPanel( panelSite.path(), ModuleProcessPanel.ID );
        });
        list.setContentProvider( new ListTreeContentProvider() );
        list.setInput( MODULES );
    }
    
}
