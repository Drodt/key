/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.proof;

import java.util.Map;

import org.key_project.logic.Term;
import org.key_project.logic.op.QuantifiableVariable;
import org.key_project.util.collection.ImmutableList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// The abstract superclass of caches for taclet app indexes that are implemented using a common
/// backend `LRUCache` (the backend is stored in `TermTacletAppIndexCacheSet`).
/// The backend is accessed in a way that guarantees that two distinct instances of this class never
/// interfere, by choosing cache keys that are specific for a particular instance of
/// `PrefixTermTacletAppIndexCacheImpl` and cannot be created by other instances. This
/// ensures that it is safe to use one instance of `LRUCache` for many instances of
/// `PrefixTermTacletAppIndexCacheImpl` (different proofs, different proof branches,
/// different locations).
public abstract class PrefixTermTacletAppIndexCacheImpl extends PrefixTermTacletAppIndexCache {
    private static final Logger LOGGER =
        LoggerFactory.getLogger(PrefixTermTacletAppIndexCacheImpl.class);

    private final Map<CacheKey, TermTacletAppIndex> cache;

    protected PrefixTermTacletAppIndexCacheImpl(ImmutableList<QuantifiableVariable> prefix,
            Map<CacheKey, TermTacletAppIndex> cache) {
        super(prefix);
        this.cache = cache;
    }

    @Override
    public TermTacletAppIndex getIndexForTerm(Term t) {
        return cache.get(new CacheKey(this, t));
    }

    private int hits = 0;
    private int total = 0;

    @SuppressWarnings("unused")
    private void countAccess(boolean hit) {
        ++total;
        if (hit) {
            ++hits;
        }
        if (total % 1000 == 0 && total != 0) {
            LOGGER.info("{} {}, size {}: {}", name(), hashCode(), cache.size(),
                ((double) hits) / (double) total);
        }
    }

    @Override
    public void putIndexForTerm(Term t, TermTacletAppIndex index) {
        cache.put(new CacheKey(this, t), index);
    }

    /// Only used for debugging purposes
    protected abstract String name();

    public static final class CacheKey {
        private final PrefixTermTacletAppIndexCacheImpl parent;
        public Term analysedTerm;

        public CacheKey(PrefixTermTacletAppIndexCacheImpl parent, Term analysedTerm) {
            this.parent = parent;
            this.analysedTerm = analysedTerm;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof CacheKey objKey)) {
                return false;
            }

            return parent == objKey.parent && analysedTerm.equals(objKey.analysedTerm);
        }

        public int hashCode() {
            return parent.hashCode() * 3784831 + analysedTerm.hashCode();
        }
    }
}
