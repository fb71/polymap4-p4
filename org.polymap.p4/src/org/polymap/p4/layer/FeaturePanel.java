/* 
 * polymap.org
 * Copyright (C) 2015, Falko Bräutigam. All rights reserved.
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
package org.polymap.p4.layer;

import static org.apache.commons.lang3.StringUtils.abbreviate;
import static org.polymap.core.runtime.UIThreadExecutor.async;

import org.geotools.data.FeatureStore;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;

import com.vividsolutions.jts.geom.Geometry;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import org.polymap.core.data.unitofwork.CommitOperation;
import org.polymap.core.data.unitofwork.UnitOfWork;
import org.polymap.core.operation.OperationSupport;
import org.polymap.core.ui.ColumnLayoutFactory;
import org.polymap.core.ui.SelectionListenerAdapter;
import org.polymap.core.ui.StatusDispatcher;

import org.polymap.rhei.batik.PanelIdentifier;
import org.polymap.rhei.batik.contribution.ContributionManager;
import org.polymap.rhei.batik.toolkit.Snackbar.Appearance;
import org.polymap.rhei.field.FormFieldEvent;
import org.polymap.rhei.field.IFormFieldListener;
import org.polymap.rhei.form.DefaultFormPage;
import org.polymap.rhei.form.IFormPageSite;
import org.polymap.rhei.form.batik.BatikFormContainer;

import org.polymap.p4.P4Panel;

/**
 * Displays a {@link StandardFeatureForm} for the {@link FeatureLayer#clicked()}
 * feature.
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class FeaturePanel
        extends P4Panel
        implements IFormFieldListener {

    public static final PanelIdentifier ID = PanelIdentifier.parse( "feature" );
    
    private FeatureStore                fs;
    
    private Feature                     feature;
    
    private UnitOfWork                  uow;

    private Button                      fab;

    private BatikFormContainer          form;

    private boolean                     previouslyValid = true;
    

    public FeatureStore fs() {
        return fs;
    }
    
    public Feature feature() {
        return feature;
    }
    
    public UnitOfWork uow() {
        return uow;
    }


    @Override
    public void createContents( Composite parent ) {
        try {
            parent.setLayout( ColumnLayoutFactory.defaults().columns( 1, 1 ).spacing( 8 ).margins( 5, 8 ).create() );
            
            fs = featureLayer.get().featureSource(); 
            feature = featureLayer.get().clicked().get();
            
            uow = new UnitOfWork( fs );
            uow.track( feature );
            form = new BatikFormContainer( new StandardFeatureForm() );
            form.createContents( parent );
            
            form.addFieldListener( this );
            
            fab = tk().createFab();
            fab.setToolTipText( "Save changes" );
            fab.setVisible( false );
            fab.addSelectionListener( new SelectionListenerAdapter( ev -> submit() ) );

            ContributionManager.instance().contributeTo( parent, this, ID.id() );
        }
        catch (Exception e) {
            createErrorContents( parent, "Unable to display feature.", e );
        }
    }

    
    @Override
    public void fieldChange( FormFieldEvent ev ) {
        if (ev.hasEventCode( VALUE_CHANGE )) {
            boolean isDirty = form.isDirty();
            boolean isValid = form.isValid();
            
            fab.setVisible( isDirty  );
            fab.setEnabled( isDirty && isValid );
            
            if (previouslyValid && !isValid) {
                tk().createSnackbar( Appearance.FadeIn, "There are invalid settings" );
            }
            if (!previouslyValid && isValid) {
                tk().createSnackbar( Appearance.FadeIn, "Settings are ok" );
            }
            previouslyValid = isValid;
        }
    }

    
    protected void submit() {
        try {
            // XXX doing this inside operation cause "Invalid thread access"
            form.submit( null );
        }
        catch (Exception e) {
            StatusDispatcher.handleError( "Unable to submit form.", e );
        }
        
        CommitOperation op = new CommitOperation().uow.put( uow );
        OperationSupport.instance().execute2( op, false, false, ev -> {
            async( () -> {
                tk().createSnackbar( Appearance.FadeIn, ev.getResult().isOK()
                        ? "Saved" : abbreviate( "Unable to save: " + ev.getResult().getMessage(), 50 ) );
            });
        });
    }

    
    /**
     * 
     */
    class StandardFeatureForm
            extends DefaultFormPage {

        @Override
        public void createFormContents( IFormPageSite site ) {
            super.createFormContents( site );
            site.getPageBody().setLayout( ColumnLayoutFactory.defaults().columns( 1, 1 ).spacing( 3 ).create() );
            
            for (Property prop : FeaturePanel.this.feature.getProperties()) {
                if (Geometry.class.isAssignableFrom( prop.getType().getBinding() )) {
                    // skip Geometry
                }
                else {
                    site.newFormField( prop ).create();
                }
            }
        }
    }
    
}
