/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.util.parsing;

import org.key_project.rusty.util.Position;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.jspecify.annotations.Nullable;


public class BuildingException extends RuntimeException {
    private final @Nullable Token offendingSymbol;

    public BuildingException(ParserRuleContext ctx, String format) {
        this(ctx, format, null);
    }

    public BuildingException(Throwable e) {
        super(e);
        offendingSymbol = null;
    }

    public BuildingException(ParserRuleContext ctx, String message, Throwable e) {
        this(ctx == null ? null : ctx.start, message, e);
    }

    public BuildingException(@Nullable Token t, String message, Throwable e) {
        super(message + " at " + getPosition(t), e);
        offendingSymbol = t;
    }

    private static String getPosition(Token t) {
        if (t != null) {
            var p = Position.make(t);
            return t.getTokenSource().getSourceName() + ":" + p.line() + ":" + p.charInLine();
        } else {
            return "";
        }
    }

    public BuildingException(ParserRuleContext ctx, Throwable ex) {
        this(ctx.start, ex.getMessage(), ex);
    }

    @Override
    public String toString() {
        return getMessage() + " (" + getPosition(offendingSymbol) + ")";
    }

    /*
     * public @Nullable Location getLocation() throws MalformedURLException {
     * if (offendingSymbol != null) {
     * URI uri = MiscTools.getURIFromTokenSource(offendingSymbol.getTokenSource());
     * return new Location(uri, Position.fromToken(offendingSymbol));
     * }
     * return null;
     * }
     */
}