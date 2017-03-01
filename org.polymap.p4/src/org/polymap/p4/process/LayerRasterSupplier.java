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

import java.util.List;
import java.util.stream.Collectors;

import org.geotools.coverage.grid.GridCoverage2D;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;

import org.polymap.core.data.process.ui.FieldViewerSite;
import org.polymap.core.data.process.ui.InputFieldSupplier;
import org.polymap.core.project.ILayer;
import org.polymap.core.project.IMap;
import org.polymap.core.ui.SelectionAdapter;
import org.polymap.core.ui.StatusDispatcher;

import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.p4.layer.RasterLayer;
import org.polymap.p4.project.ProjectRepository;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class LayerRasterSupplier
        extends InputFieldSupplier {

    private static final Log log = LogFactory.getLog( LayerRasterSupplier.class );
    
    private ComboViewer         combo;

    private List<ILayer>        layers;
    

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
        UnitOfWork uow = ProjectRepository.unitOfWork();
        IMap rootMap = uow.entity( IMap.class, ProjectRepository.ROOT_MAP_ID );
        layers = rootMap.layers.stream().collect( Collectors.toList() );
        
        combo = new ComboViewer( parent, SWT.READ_ONLY );
        combo.getCombo().setFont( combo.getCombo().getParent().getFont() );
        combo.getCombo().setVisibleItemCount( 8 );
        combo.setLabelProvider( new LabelProvider() {
            @Override
            public String getText( Object elm ) {
                return ((ILayer)elm).label.get();
            }
        });
        combo.setContentProvider( ArrayContentProvider.getInstance() );
        combo.setInput( layers );
        combo.addSelectionChangedListener( ev -> {
            try {
                supply();
            }
            catch (Exception e) {
                StatusDispatcher.handleError( "Raster input was not properly set.", e );
            }
        });
        combo.setSelection( new StructuredSelection( site.layer.orElse( layers.get( 0 ) ) ) );
        //combo.getCombo().forceFocus();
    }


    public void supply() throws Exception {
        ILayer layer = SelectionAdapter.on( combo.getSelection() ).first( ILayer.class ).get();
        
        // block UIThread until field is set
        RasterLayer rl = RasterLayer.of( layer ).get().get();

        site.setFieldValue( rl.gridCoverage() );
    }

}
