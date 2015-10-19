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

import java.util.List;

import org.opengis.feature.type.FeatureType;

import org.eclipse.swt.widgets.Composite;

import org.polymap.model2.Entity;

/**
 * Styler is the high level API to provide simple and complex feature stylings. A
 * Styler provides the UI to manipulate its parameters and it provides the logic to
 * build SLD for it.
 * <p/>
 * A Styler serves the following purposes:
 * <ul>
 * <li>provides a UI to set parameters</li>
 * <li>loads/stores its state (via Model2)</li>
 * <li>build SLD</li>
 * </ul>
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public abstract class Styler
        extends Entity {

    // Nach unserer heutigen Analyse würde ich sagen, dass SLD als persistenz nicht funktioniert,
    // da wir die ausmultiplizierten rules nie wieder auseinander bekommen. Deshalb...
    //    
    // Persistenz würde ich gleich über Model2 machen. Alles was persistent sein soll,
    // wird also ein Property. Komplexe properties sind auch möglich. Ob der Styler selber eine
    // Entity sein sollte, das weiss ich noch nicht
    
    
    /**
     * Wir haben immer ein konkretes schema. Platzhalter scheinen mir zu komplex.
     *
     * @param schema
     */
    public abstract void init( FeatureType schema );
    
    
    public abstract Composite createContents( Composite parent );
    
    /**
     * Für einen externen submit knopf, der vom framework gebaut wird.
     */
    public abstract void submitUI();
    
    public abstract void resetUI();

    
    /**
     * Für komplexe styler müsste hier das "ausmultiplizieren" passieren. D.h. wir müssen
     * werkzeuge bereitstellen, die es erlauben rules zu mischen. Vielleicht ist auch ein
     * Ruler-Wrapper gut?
     * <p/>
     * Mir ist noch nicht klar ob "nach unten" oder "nach oben" aggregiert wird. Gefühlsmässig
     * würde ich sagen: ich nehme das von unten, erweitere es und gebe nach oben. 
     *
     * @param children 
     * @return
     */
    public abstract Object buildSLD( List<Styler> children );
    
}
