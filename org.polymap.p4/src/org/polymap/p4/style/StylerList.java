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

import static org.polymap.rhei.batik.toolkit.md.dp.dp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.geoserver.catalog.SLDHandler;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.StyledLayer;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.styling.UserLayer;
import org.geotools.util.Version;
import org.polymap.core.runtime.Callback;
import org.polymap.core.ui.FormDataFactory;
import org.polymap.core.ui.FormLayoutFactory;
import org.polymap.rhei.batik.toolkit.md.MdListViewer;
import org.polymap.rhei.batik.toolkit.md.MdToolkit;

/**
 * @author Joerg Reichert <joerg@mapzone.io>
 *
 */
public class StylerList
        extends Composite {

    private Button                                newButton, loadButton, saveButton, deleteButton;

    private List<StyledLayerDescriptor>           styles = new ArrayList<StyledLayerDescriptor>();

    private MdListViewer                          styleList;

    private Text                                  styleListFilter;

    private final Supplier<Boolean>                     newCallback;

    private final Supplier<StyledLayerDescriptor> saveSupplier;

    private final Callback<StyledLayerDescriptor> loadCallback;

    private final Supplier<Boolean>                     deleteCallback;


    public StylerList( Composite parent, MdToolkit tk, int style, Supplier<Boolean> newCallback,
            Supplier<StyledLayerDescriptor> saveSupplier, Callback<StyledLayerDescriptor> loadCallback,
            Supplier<Boolean> deleteCallback ) {
        super( parent, style );
        setLayout( FormLayoutFactory.defaults().spacing( dp( 16 ).pix() ).create() );

        this.newCallback = newCallback;
        this.saveSupplier = saveSupplier;
        this.loadCallback = loadCallback;
        this.deleteCallback = deleteCallback;

        Composite styleListFilterComp = createStyleListFilter( this, tk );
        FormDataFactory.on( styleListFilterComp ).left( 0 ).right( 100 );

        Composite styleListComp = createStyleList( this, tk );
        FormDataFactory.on( styleListComp ).top( styleListFilterComp, dp( 30 ).pix() ).left( 0 ).right( 100 )
                .height( 300 );

        Composite buttonBar = createButtons( this, tk );
        FormDataFactory.on( buttonBar ).top( styleListComp, dp( 30 ).pix() ).left( 0 ).right( 100 ).bottom( 100 );
    }


    private Composite createStyleListFilter( Composite parent, MdToolkit tk ) {
        Composite styleListFilterForm = tk.createComposite( parent, SWT.NONE );
        styleListFilterForm.setLayout( FormLayoutFactory.defaults().spacing( dp( 16 ).pix() ).create() );

        Label label = tk.createLabel( styleListFilterForm, "Filter:", SWT.NONE );

        styleListFilter = tk.createText( styleListFilterForm, "", SWT.BORDER );
        styleListFilter.addModifyListener( new ModifyListener() {

            @Override
            public void modifyText( ModifyEvent event ) {
                List<StyledLayerDescriptor> newInput = styles.stream().filter( style -> {
                    String filterText = styleListFilter.getText();
                    String name = getStyleIdentifyingName( style );
                    if (filterText.startsWith( "*" )) {
                        return name != null && name.contains( filterText.substring( 1 ) );
                    }
                    else {
                        return name != null && name.startsWith( filterText );
                    }
                } ).collect( Collectors.toList() );
                styleList.setInput( newInput );
            }
        } );

        FormDataFactory.on( styleListFilter ).left( label, dp( 30 ).pix() ).right( 100 );

        return styleListFilterForm;
    }


    private Composite createStyleList( Composite parent, MdToolkit tk ) {
        Composite styleListComp = tk.createComposite( parent, SWT.NONE );
        styleListComp.setLayout( FormLayoutFactory.defaults().spacing( dp( 16 ).pix() ).create() );
        styles.addAll( loadPredefinedStyles() );
        styleList = tk.createListViewer( styleListComp, SWT.VIRTUAL, SWT.FULL_SELECTION, SWT.BORDER );
        styleList.firstLineLabelProvider.set( new CellLabelProvider() {

            @Override
            public void update( ViewerCell cell ) {
                StyledLayerDescriptor desc = (StyledLayerDescriptor)cell.getElement();
                cell.setText( getStyleIdentifyingName( desc ) );
            }

        } );
        styleList.setContentProvider( new ITreeContentProvider() {

            List<StyledLayerDescriptor> styles;


            @SuppressWarnings("unchecked")
            @Override
            public void inputChanged( Viewer viewer, Object oldInput, Object newInput ) {
                styles = (List<StyledLayerDescriptor>)newInput;
            }


            @Override
            public void dispose() {
            }


            @Override
            public boolean hasChildren( Object element ) {
                return false;
            }


            @Override
            public Object getParent( Object element ) {
                return null;
            }


            @Override
            public Object[] getElements( Object inputElement ) {
                return styles.toArray();
            }


            @Override
            public Object[] getChildren( Object parentElement ) {
                return null;
            }
        } );
        styleList.setInput( styles );
        FormDataFactory.on( styleList.getControl() ).fill();
        return styleListComp;
    }


    private String getStyleIdentifyingName( StyledLayerDescriptor desc ) {
        String name = desc.getTitle();
        if (name == null) {
            name = desc.getName();
        }
        if (name == null) {
            for (StyledLayer layer : desc.getStyledLayers()) {
                if (layer instanceof NamedLayer) {
                    NamedLayer namedLayer = (NamedLayer)layer;
                    name = namedLayer.getName();
                }
                else if (layer instanceof UserLayer) {
                    UserLayer userLayer = (UserLayer)layer;
                    name = userLayer.getName();
                }
            }
        }
        return name;
    }


    private List<StyledLayerDescriptor> loadPredefinedStyles() {
        List<StyledLayerDescriptor> styles = new ArrayList<StyledLayerDescriptor>();
        try (BufferedReader br = new BufferedReader( new InputStreamReader( getClass().getClassLoader()
                .getResourceAsStream( "resources/slds/sld.list" ) ) )) {
            String line;
            Version styleVersion = new Version( "1.0.0" );
            SLDHandler sldHandler = new SLDHandler();
            while ((line = br.readLine()) != null) {
                try (InputStreamReader reader = new InputStreamReader( getClass().getClassLoader().getResourceAsStream(
                        "resources/" + line ) )) {
                    StyledLayerDescriptor sld = sldHandler.parse( reader, styleVersion, null, null );
                    styles.add( sld );
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return styles;
    }


    private Composite createButtons( Composite parent, MdToolkit tk ) {
        Composite buttonBar = tk.createComposite( parent, SWT.NONE );
        FormDataFactory.on( buttonBar ).top( styleList.getControl(), dp( 30 ).pix() ).left( 0 ).right( 100 );
        buttonBar.setLayout( FormLayoutFactory.defaults().spacing( dp( 16 ).pix() ).create() );

        createNewButton( tk, buttonBar );
        createSaveButton( tk, buttonBar );
        createLoadButton( tk, buttonBar );
        createDeleteButton( tk, buttonBar );

        FormDataFactory.on( newButton );
        FormDataFactory.on( saveButton ).left( newButton, dp( 30 ).pix() );
        FormDataFactory.on( loadButton ).left( saveButton, dp( 30 ).pix() );
        FormDataFactory.on( deleteButton ).left( loadButton, dp( 30 ).pix() );

        return buttonBar;
    }


    private void createDeleteButton( MdToolkit tk, Composite buttonBar ) {
        deleteButton = tk.createButton( buttonBar, "Delete Style", SWT.NONE );
        deleteButton.addSelectionListener( new SelectionAdapter() {

            public void widgetSelected( SelectionEvent e ) {
                IStructuredSelection selection = (IStructuredSelection)styleList.getSelection();
                StyledLayerDescriptor style = (StyledLayerDescriptor)selection.getFirstElement();
                // TODO delete
                styles.remove( style );
                styleList.refresh();
                deleteCallback.get();
            }
        } );
    }


    private void createLoadButton( MdToolkit tk, Composite buttonBar ) {
        loadButton = tk.createButton( buttonBar, "Load Style", SWT.NONE );
        loadButton.addSelectionListener( new SelectionAdapter() {

            public void widgetSelected( SelectionEvent e ) {
                IStructuredSelection selection = (IStructuredSelection)styleList.getSelection();
                StyledLayerDescriptor style = (StyledLayerDescriptor)selection.getFirstElement();
                loadCallback.handle( style );
            }
        } );
    }


    private void createNewButton( MdToolkit tk, Composite buttonBar ) {
        newButton = tk.createButton( buttonBar, "New Style", SWT.NONE );
        newButton.addSelectionListener( new SelectionAdapter() {

            public void widgetSelected( SelectionEvent e ) {
                newCallback.get();
            }
        } );
    }


    private void createSaveButton( MdToolkit tk, Composite buttonBar ) {
        saveButton = tk.createButton( buttonBar, "Save Style", SWT.NONE );
        saveButton.addSelectionListener( new SelectionAdapter() {

            public void widgetSelected( SelectionEvent e ) {
                StyledLayerDescriptor sld = saveSupplier.get();
                styles.add( sld );
                styleList.setInput( styles );
                styleList.setSelection( new StructuredSelection( sld ) );
            }
        } );
    }
}
