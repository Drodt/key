/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.proof.io;

import org.jspecify.annotations.Nullable;

public class ProblemLoaderException extends Exception {
    private final @Nullable AbstractProblemLoader origin;

    public ProblemLoaderException(@Nullable AbstractProblemLoader origin, Throwable cause) {
        super(cause.getMessage(), cause);
        this.origin = origin;
    }

    public ProblemLoaderException(@Nullable AbstractProblemLoader origin, String msg,
            Throwable cause) {
        super(msg, cause);
        this.origin = origin;
    }

    public ProblemLoaderException(@Nullable AbstractProblemLoader origin, String msg) {
        super(msg);
        this.origin = origin;
    }

    public @Nullable AbstractProblemLoader getOrigin() {
        return origin;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (getMessage() != null) {
            sb.append(getMessage());
        }
        sb.append(" (");
        if (origin == null) {
            sb.append("unknown origin");
        } else {
            sb.append("file: ").append(origin.getFile());
        }
        if (getCause() != null) {
            sb.append("; caused by: ");
            sb.append(getCause());
        }
        sb.append(')');
        return sb.toString();
    }
}
