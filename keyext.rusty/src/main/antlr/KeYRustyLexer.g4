lexer grammar KeYRustyLexer;

import KeYLexer;
@ header
{
package org.key_project.rusty.parser;
}

MUT : 'mut';

MUTATE
:   '*->'
    ;

NEW_LOCAL_VARS: '\\newLocalVars';
STORE_TERM_IN : '\\storeTermIn';
STORE_EXPR_IN : '\\storeExprIn';
HAS_INVARIANT : '\\hasInvariant';
GET_INVARIANT : '\\getInvariant';
GET_VARIANT   : '\\getVariant';
IS_LABELED    : '\\isLabeled';
DIFFERENT     : '\\different';
NO_FREE_VAR_IN : '\\noFreeVarIn';
DROP_EFFECTLESS_MUTATING: '\\dropEffectlessMutating';
APPLY_ELEMENTARIES_ON_DEREF_M: '\\applyElementariesOnDerefM';

OPENTYPEPARAMS:'<' '[';
CLOSETYPEPARAMS:']' '>';

CONST : 'const';

SORT: '\\sort';

NON_RIGID: '\\nonRigid';