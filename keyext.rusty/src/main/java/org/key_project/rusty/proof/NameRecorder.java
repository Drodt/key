/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.proof;

import org.jspecify.annotations.Nullable;
import org.key_project.logic.Name;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;

import java.util.Objects;

public class NameRecorder {
    private ImmutableList<Name> pre = ImmutableSLList.nil();

    private ImmutableList<Name> post = ImmutableSLList.nil();

    public void setProposals(ImmutableList<Name> proposals) {
        pre = Objects.requireNonNullElseGet(proposals, ImmutableSLList::nil);
    }

    /// Get the name proposals added using [#addProposal(Name)].
    ///
    /// @return the name proposals
    public ImmutableList<Name> getProposals() {
        return post;
    }

    /// Get the name proposals previously set using [#setProposals(ImmutableList)].
    ///
    /// @return the name proposals
    public ImmutableList<Name> getSetProposals() {
        return pre;
    }

    public void addProposal(Name proposal) {
        post = post.append(proposal);
    }

    /// Get a proposal and remove it from this recorder.
    ///
    /// @return the first proposal
    public @Nullable Name getProposal() {
        Name proposal = null;

        if (pre != null && !pre.isEmpty()) {
            proposal = pre.head();
            pre = pre.tail();
        }

        return proposal;
    }

    public NameRecorder copy() {
        final NameRecorder result = new NameRecorder();
        result.pre = pre;
        result.post = post;
        return result;
    }
}
