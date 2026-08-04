/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.keyproject.key.api.data;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 *
 * @param problemFile URI to the problem file to be loaded
 * @param includes xxx
 */
public record LoadParams(@Nullable Uri problemFile,
        @Nullable List<Uri> includes)
        implements KeYDataTransferObject {
}
