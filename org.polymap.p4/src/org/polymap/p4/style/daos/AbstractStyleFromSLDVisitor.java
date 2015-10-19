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
package org.polymap.p4.style.daos;

import java.awt.Color;

import org.eclipse.swt.graphics.RGB;
import org.geotools.filter.expression.AbstractExpressionVisitor;
import org.geotools.renderer.lite.gridcoverage2d.StyleVisitorAdapter;
import org.geotools.styling.AnchorPoint;
import org.geotools.styling.Displacement;
import org.geotools.styling.ExternalGraphic;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Fill;
import org.geotools.styling.Graphic;
import org.geotools.styling.LinePlacement;
import org.geotools.styling.Mark;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.PointPlacement;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayer;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.UserLayer;
import org.opengis.filter.expression.ExpressionVisitor;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.style.Font;
import org.opengis.style.GraphicalSymbol;

/**
 * @author Joerg Reichert <joerg@mapzone.io>
 *
 */
public abstract class AbstractStyleFromSLDVisitor
        extends StyleVisitorAdapter {

    private ExpressionVisitor colorExpressionVisitor      = null;

    private ExpressionVisitor numberExpressionVisitor     = null;

    private ExpressionVisitor labelExpressionVisitor      = null;

    private ExpressionVisitor stringExpressionVisitor     = null;

    private ExpressionVisitor fontWeightExpressionVisitor = null;

    private ExpressionVisitor fontStyleExpressionVisitor  = null;


    protected ExpressionVisitor getColorExpressionVisitor() {
        if (colorExpressionVisitor == null) {
            colorExpressionVisitor = new AbstractExpressionVisitor() {

                @Override
                public Object visit( Literal expr, Object extraData ) {
                    if (expr.getValue() != null) {
                        if (expr.getValue().toString().startsWith( "#" )) {
                            String hexValue = expr.getValue().toString();
                            Color color = Color.decode( hexValue );
                            return new RGB( color.getRed(), color.getGreen(), color.getBlue() );
                        }
                    }
                    return expr.getValue();
                }
            };
        }
        return colorExpressionVisitor;
    }


    protected ExpressionVisitor getNumberExpressionVisitor() {
        if (numberExpressionVisitor == null) {
            numberExpressionVisitor = new AbstractExpressionVisitor() {

                @Override
                public Object visit( Literal expr, Object extraData ) {
                    if (expr.getValue() != null) {
                        try {
                            return new Double( Double.parseDouble( expr.getValue().toString() ) );
                        }
                        catch (NumberFormatException nfe) {
                            //
                        }
                    }
                    return null;
                }
            };
        }
        return numberExpressionVisitor;
    }


    protected ExpressionVisitor getLabelExpressionVisitor() {
        if (labelExpressionVisitor == null) {
            labelExpressionVisitor = new AbstractExpressionVisitor() {

                @Override
                public Object visit( PropertyName expr, Object extraData ) {
                    return expr.getPropertyName();
                }
            };
        }
        return labelExpressionVisitor;
    }


    protected ExpressionVisitor getStringExpressionVisitor() {
        if (stringExpressionVisitor == null) {
            stringExpressionVisitor = new AbstractExpressionVisitor() {

                @Override
                public Object visit( Literal expr, Object extraData ) {
                    return expr.getValue();
                }
            };
        }
        return stringExpressionVisitor;
    }


    protected ExpressionVisitor getFontWeightExpressionVisitor() {
        if (fontWeightExpressionVisitor == null) {
            fontWeightExpressionVisitor = new AbstractExpressionVisitor() {

                @Override
                public Object visit( Literal expr, Object extraData ) {
                    return expr.getValue();
                }
            };
        }
        return fontWeightExpressionVisitor;
    }


    protected ExpressionVisitor getFontStyleExpressionVisitor() {
        if (fontStyleExpressionVisitor == null) {
            fontStyleExpressionVisitor = new AbstractExpressionVisitor() {

                @Override
                public Object visit( Literal expr, Object extraData ) {
                    return expr.getValue();
                }
            };
        }
        return fontStyleExpressionVisitor;
    }


    @Override
    public void visit( StyledLayerDescriptor sld ) {
        for (StyledLayer layer : sld.getStyledLayers()) {
            if (layer instanceof NamedLayer) {
                ((NamedLayer)layer).accept( this );
            }
            else if (layer instanceof UserLayer) {
                ((UserLayer)layer).accept( this );
            }
        }
    }


    @Override
    public void visit( NamedLayer layer ) {
        for (Style style : layer.getStyles()) {
            style.accept( this );
        }
    }


    @Override
    public void visit( Style style ) {
        for (FeatureTypeStyle featureTypeStyle : style.featureTypeStyles()) {
            featureTypeStyle.accept( this );
        }
    }


    @Override
    public void visit( FeatureTypeStyle fts ) {
        for (Rule rule : fts.rules()) {
            rule.accept( this );
        }
    }


    @Override
    public void visit( Rule rule ) {
        for (Symbolizer symbolizer : rule.getSymbolizers()) {
            symbolizer.accept( this );
        }
    }


    @Override
    public void visit( org.geotools.styling.TextSymbolizer ts ) {
        if (ts.getFill() != null) {
            ts.getFill().accept( this );
        }
        if (ts.getFont() != null) {
            visit( ts.getFont() );
        }
        if (ts.getLabelPlacement() != null) {
            ts.getLabelPlacement().accept( this );
        }
    }


    public void visit( Font font ) {
    }


    @Override
    public void visit( PointPlacement pp ) {
        if(pp.getAnchorPoint() != null) {
            pp.getAnchorPoint().accept( this );
        }
        if(pp.getDisplacement() != null) {
            pp.getDisplacement().accept( this );
        }
    }
    
    @Override
    public void visit( AnchorPoint ap ) {
    }

    @Override
    public void visit( Displacement dis ) {
    }

    @Override
    public void visit( LinePlacement lp ) {
    }


    @Override
    public void visit( org.geotools.styling.PointSymbolizer ps ) {
    }


    @Override
    public void visit( Graphic gr ) {
        for (GraphicalSymbol symbol : gr.graphicalSymbols()) {
            if (symbol instanceof ExternalGraphic) {
                ((ExternalGraphic)symbol).accept( this );
            } else if (symbol instanceof Mark) {
                ((Mark)symbol).accept( this );
            }
        }
    }


    @Override
    public void visit( Mark mark ) {
        if (mark.getFill() != null) {
            mark.getFill().accept( this );
        }
        if (mark.getStroke() != null) {
            mark.getStroke().accept( this );
        }
    }

    
    @Override
    public void visit( ExternalGraphic exgr ) {
    }

    @Override
    public void visit( Fill fill ) {
        if (fill.getGraphicFill() != null) {
            fill.getGraphicFill().accept( this );
        }
    }


    @Override
    public void visit( Stroke stroke ) {
        if (stroke.getGraphicFill() != null) {
            stroke.getGraphicFill().accept( this );
        }
        if (stroke.getGraphicStroke() != null) {
            stroke.getGraphicStroke().accept( this );
        }
    }


    @Override
    public void visit( org.geotools.styling.LineSymbolizer line ) {
    }


    @Override
    public void visit( org.geotools.styling.PolygonSymbolizer poly ) {
    }


    @Override
    public void visit( RasterSymbolizer raster ) {
    }
}
