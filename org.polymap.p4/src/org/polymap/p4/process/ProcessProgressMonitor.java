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

import static org.apache.commons.lang3.StringUtils.removeEnd;

import java.util.EventObject;
import java.util.Optional;

import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Joiner;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import org.polymap.core.runtime.Timer;
import org.polymap.core.runtime.event.EventManager;
import org.polymap.core.ui.FormDataFactory;
import org.polymap.core.ui.FormLayoutFactory;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class ProcessProgressMonitor
        extends DummyProgressMonitor
        implements IJGTProgressMonitor {

    private static final Log log = LogFactory.getLog( ProcessProgressMonitor.class );
    
    private BackgroundJob   bgjob;
    
    private String          taskName;

    private String          subTaskName;

    private volatile int    total = UNKNOWN;

    private volatile int    worked;
    
    private Timer           updated = new Timer();
    
    private volatile boolean canceled;

    /** The display of the last {@link #createContents(Composite)}. */
    private Display         display;
    
    private Label           msg;

    
    public ProcessProgressMonitor( BackgroundJob bgjob ) {
        this.bgjob = bgjob;
        taskName = "Start processing";
    }

    /**
     * (Re-)Creates the UI under the given parent.
     */
    public void createContents( Composite parent ) {
        // FIXME multiple sessions
        display = parent.getDisplay();
        
        parent.setLayout( FormLayoutFactory.defaults().margins( 0, 30 ).create() );
        Label wheel = new Label( parent, SWT.CENTER );
        wheel.setLayoutData( FormDataFactory.filled().noBottom().create() );
        wheel.setText( "Crunching data..." );
       // wheel.setImage( BatikPlugin.images().image( "resources/icons/loading24.gif" ) );

        msg = new Label( parent, SWT.CENTER );
        msg.setLayoutData( FormDataFactory.filled().top( wheel, 10 ).create() );        
        update( false );
    }
    
    /**
     * 
     *
     * @param throttle
     */
    protected void update( boolean throttle ) {
        if (throttle && updated.elapsedTime() < 1000) {
            return;
        }
        updated.start();

        EventManager.instance().publish( new ProgressEvent( bgjob ) );
        
        if (display != null && !display.isDisposed()) {
            display.asyncExec( () -> {
                if (msg != null && !msg.isDisposed()) {
                    StringBuilder s = new StringBuilder( 256 ).append( 
                            Joiner.on( " " ).skipNulls().join( removeEnd( taskName, "..." ), " ...", subTaskName ) );
                    completed().ifPresent( value -> 
                            s.append( " (" ).append( value ).append( "%)" ) );
                    msg.setText( s.toString() );
                }
            });
        }
    }

    /**
     * Percent of work that has been completed so far, or {@link Optional#empty()}
     * if the total amount of work is {@link IJGTProgressMonitor#UNKNOWN}.
     */
    public Optional<Integer> completed() {
        return total != UNKNOWN 
                ? Optional.of( Integer.valueOf( (int)(100f / total * worked) ) )
                : Optional.empty();
    }
    
    public void reset() {
        canceled = false;
        worked = 0;
        subTaskName = null;
    }

    @Override
    public void done() {
        worked = total;
        update( false );
    }

    @Override
    public boolean isCanceled() {
        return canceled;
    }

    @Override
    public void setCanceled( boolean canceled ) {
        this.canceled = canceled;
    }


    @Override
    public void beginTask( String name, int totalWork ) {
        this.taskName = name;
        this.total = totalWork;
        update( false );
    }

    @Override
    public void setTaskName( String name ) {
        this.taskName = name;
        update( false );
    }

    @Override
    public void subTask( String name ) {
        this.subTaskName = name;
        update( true );
    }

    @Override
    public void worked( int work ) {
        worked += work;
        update( true );
    }

    
    /**
     * Fired when {@link ProcessProgressMonitor} changes its progression state.
     */
    public class ProgressEvent
            extends EventObject {
        
        public ProgressEvent( BackgroundJob source ) {
            super( source );
        }

        @Override
        public Object getSource() {
            return (BackgroundJob)super.getSource();
        }

        public Optional<Integer> completed() {
            return ProcessProgressMonitor.this.completed();
        }
    }

}
