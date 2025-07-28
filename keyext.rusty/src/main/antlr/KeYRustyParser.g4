parser grammar KeYRustyParser;

import KeYParser;

options { tokenVocab = KeYRustyLexer; }

@header {
package org.key_project.rusty.parser;
}

varexpId
 : APPLY_UPDATE_ON_RIGID
 | DROP_EFFECTLESS_ELEMENTARIES
 | SIMPLIFY_IF_THEN_ELSE_UPDATE
 | EQUAL_UNIQUE
 | NEW_TYPE_OF
 | NEW_DEPENDING_ON
 | NEW
 | NEW_LOCAL_VARS
 | STORE_TERM_IN
 | STORE_EXPR_IN
 | HAS_INVARIANT
 | GET_INVARIANT
 | GET_VARIANT
 | IS_LABELED
 | DIFFERENT
 ;

elementary_update_term: a=mutating_update_term (ASSIGN b=mutating_update_term)?;
mutating_update_term: a=equivalence_term (MUTATE b=equivalence_term)?;

primitive_term:
    termParen
  | ifThenElseTerm
  | ifExThenElseTerm
  | abbreviation
  | accessterm
  | mRef_term
  | literals
  ;

mRef_term : REF_M LESS simple_ident GREATER;

typemapping
    : (AND MUT?)? simple_ident;

one_sort_decl
:
  doc=DOC_COMMENT?
  (
     GENERIC  sortIds=simple_ident_dots_comma_list
        (ONEOF sortOneOf = oneof_sorts)?
        (EXTENDS sortExt = extends_sorts)? SEMI
    | PROXY  sortIds=simple_ident_dots_comma_list (EXTENDS sortExt=extends_sorts)? SEMI
    | ABSTRACT? (sortIds=simple_ident_dots_comma_list |
                 parametric_sort_decl) (EXTENDS sortExt=extends_sorts)?  SEMI
  )
;

parametric_sort_decl
:
    simple_ident_dots
    formal_sort_param_decls
;

formal_sort_param_decls
: OPENTYPEPARAMS
      formal_sort_param_decl (COMMA formal_sort_param_decl)*
      CLOSETYPEPARAMS ;

formal_sort_param_decl
:
    simple_ident | const_param_decl
;

const_param_decl: CONST simple_ident COLON sortId ;

datatype_decl:
  doc=DOC_COMMENT?
  // weigl: all datatypes are free!
  // FREE?
  name=simple_ident formal_sort_parameters?
  EQUALS
  datatype_constructor (OR datatype_constructor)*
  SEMI
;

sortId
:
    id=simple_ident_dots formal_sort_parameters?
;

formal_sort_parameters
:
    OPENTYPEPARAMS
    formal_sort_param (COMMA formal_sort_param)*
    CLOSETYPEPARAMS
;

formal_sort_param : sortId | CONST simple_ident ;

func_decl
:
    doc=DOC_COMMENT?
    (UNIQUE)?
    func_name = funcpred_name
    formal_sort_param_decls?
	whereToBind=where_to_bind?
    argSorts = arg_sorts
    IMP
    retSort = sortId
    SEMI
;