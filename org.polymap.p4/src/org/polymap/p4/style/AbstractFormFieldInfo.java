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

import org.polymap.rhei.field.IFormField;


/**
 * @author Joerg Reichert <joerg@mapzone.io>
 *
 */
public abstract class AbstractFormFieldInfo
        implements IFormFieldInfo {
    private IFormField formField = null;

    /* (non-Javadoc)
     * @see org.polymap.p4.style.IFormFieldInfo#getFormField()
     */
    @Override
    public IFormField getFormField() {
        return formField;
    }


    /* (non-Javadoc)
     * @see org.polymap.p4.style.IFormFieldInfo#setFormField(org.polymap.rhei.field.IFormField)
     */
    @Override
    public void setFormField( IFormField formField ) {
        this.formField = formField;
    }
}
