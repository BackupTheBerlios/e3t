// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// jcg wrote this.
// last revised February 1st 2000

// List of all kinds of nodes one can find in the syntax tree,
// together with a method to generate a representation

// numbering starts at 227, previous numbers will identify tokens (with an identifier 
// generated automatically by javacc

package tlasany.st;

import util.UniqueString;

public interface SyntaxTreeConstants {

  int  NULL_ID =  227;
  int  N_ActDecl  =  228 ;
  int  N_ActionExpr  =  229 ;
  int  N_AssumeDecl  =  230 ;
  int  N_AssumeProve  =  231 ;
  int  N_Assumption  =  232 ;
  int  N_BeginModule  =  233 ;
  int  N_Body  =  234 ;
  int  N_BoundQuant  =  235 ;
  int  N_Case  =  236 ;
  int  N_CaseArm  =  237 ;
  int  N_CaseStatement  =  238 ;
  int  N_ChooseStatement  =  239 ;
  int  N_ConjItem  =  240 ;
  int  N_ConjList  =  241 ;
  int  N_ConsDecl  =  242 ;
  int  N_DisjItem  =  243 ;
  int  N_DisjList  =  244 ;
  int  N_EndModule  =  245 ;
  int  N_Except  =  246 ;
  int  N_ExceptComponent  =  247 ;
  int  N_ExceptSpec  =  248 ;
  int  N_Times =  249 ; // N_Expression
  int  N_Extends  =  250 ;
  int  N_FairnessExpr  =  251 ;
  int  N_FcnAppl  =  252 ;
  int  N_FcnConst  =  253 ;
  int  N_FieldSet  =  254 ;
  int  N_FieldVal  =  255 ;
  int  N_FunctionDefinition  =  256 ;
  int  N_FunctionParam  =  257 ;
  int  N_GeneralId  =  258 ;
  int  N_GenInfixOp  =  259 ;
  int  N_GenNonExpPrefixOp  =  260 ;
  int  N_GenPostfixOp  =  261 ;
  int  N_GenPrefixOp  =  262 ;
  int  N_IdentDecl  =  263 ;
  int  N_Real       =  264; //  int  N_Identifier  =  264 ;
  int  N_IdentifierTuple  =  265 ;
  int  N_IdentLHS  =  266 ;
  int  N_IdPrefix  =  267 ;
  int  N_IdPrefixElement  =  268 ;
  int  N_IfThenElse  =  269 ;
  int  N_InfixDecl  =  270 ;
  int  N_InfixExpr  =  271 ;
  int  N_InfixLHS  =  272 ;
  int  N_InfixOp  =  273 ;
  int  N_InnerProof  =  274 ;
  int  N_Instance  =  275 ;
  int  N_NonLocalInstance  =  276 ;
  int  N_Integer  =  277 ;
  int  N_LeafProof  =  278 ;
  int  N_LetDefinitions  =  279 ;
  int  N_LetIn  =  280 ;
  int  N_MaybeBound  =  281 ;
  int  N_Module  =  282 ;
  int  N_ModuleDefinition  =  283 ;
  int  N_NonExpPrefixOp  =  284 ;
  int  N_Number  =  285 ;
  int  N_NumberedAssumeProve  =  286 ;
  int  N_OpApplication  =  287 ;
  int  N_OpArgs  =  288 ;
  int  N_OperatorDefinition  =  289 ;
  int  N_OtherArm  =  290 ;
  int  N_ParamDecl  =  291 ;
  int  N_ParamDeclaration  =  292 ;
  int  N_ParenExpr  =  293 ;
  int  N_PostfixDecl  =  294 ;
  int  N_PostfixExpr  =  295 ;
  int  N_PostfixLHS  =  296 ;
  int  N_PostfixOp  =  297 ;
  int  N_PrefixDecl  =  298 ;
  int  N_PrefixExpr  =  299 ;
  int  N_PrefixLHS  =  300 ;
  int  N_PrefixOp  =  301 ;
  int  N_Proof  =  302 ;
  int  N_ProofLet  =  303 ;
  int  N_ProofName  =  304 ;
  int  N_ProofStatement  =  305 ;
  int  N_ProofStep  =  306 ;
  int  N_QEDStep  =  307 ;
  int  N_QuantBound  =  308 ;
  int  N_RcdConstructor  =  309 ;
  int  N_RecordComponent  =  310 ;
  int  N_SetEnumerate  =  311 ;
  int  N_SetExcept  =  312 ;
  int  N_SetOfAll  =  313 ;
  int  N_SetOfFcns  =  314 ;
  int  N_SetOfRcds  =  315 ;
  int  N_SExceptSpec  =  316 ;
  int  N_SFcnDecl  =  317 ;
  int  N_String  =  318 ;
  int  N_SubsetOf  =  319 ;
  int  N_Substitution  =  320 ;
  int  N_TempDecl  =  321 ;
  int  N_Theorem  =  322 ;
  int  N_Tuple  =  323 ;
  int  N_UnboundOrBoundChoose  =  324 ;
  int  N_UnboundQuant  =  325 ;
  int  N_VariableDeclaration  =  326 ;
  int  T_IN = 327;
  int  T_EQUAL = 328;

  UniqueString[] SyntaxNodeImage = {
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("Not a node"),
    UniqueString.uniqueStringOf("N_ActDecl"),
    UniqueString.uniqueStringOf("N_ActionExpr"),
    UniqueString.uniqueStringOf("N_AssumeDecl"),
    UniqueString.uniqueStringOf("N_AssumeProve"),
    UniqueString.uniqueStringOf("N_Assumption"),
    UniqueString.uniqueStringOf("N_BeginModule"),
    UniqueString.uniqueStringOf("N_Body"),
    UniqueString.uniqueStringOf("N_BoundedQuant"),
    UniqueString.uniqueStringOf("N_Case"),
    UniqueString.uniqueStringOf("N_CaseArm"),
    UniqueString.uniqueStringOf("N_CaseStatement"),
    UniqueString.uniqueStringOf("N_ChooseStatement"),
    UniqueString.uniqueStringOf("N_ConjItem"),
    UniqueString.uniqueStringOf("N_ConjList"),
    UniqueString.uniqueStringOf("N_ConsDecl"),
    UniqueString.uniqueStringOf("N_DisjItem"),
    UniqueString.uniqueStringOf("N_DisjList"),
    UniqueString.uniqueStringOf("N_EndModule"),
    UniqueString.uniqueStringOf("N_Except"),
    UniqueString.uniqueStringOf("N_ExceptComponent"),
    UniqueString.uniqueStringOf("N_ExceptSpec"),
    UniqueString.uniqueStringOf("N_Times"), // "N_Expression"),
    UniqueString.uniqueStringOf("N_Extends"),
    UniqueString.uniqueStringOf("N_FairnessExpr"),
    UniqueString.uniqueStringOf("N_FcnAppl"),
    UniqueString.uniqueStringOf("N_FcnConst"),
    UniqueString.uniqueStringOf("N_FieldSet"),
    UniqueString.uniqueStringOf("N_FieldVal"),
    UniqueString.uniqueStringOf("N_FunctionDefinition"),
    UniqueString.uniqueStringOf("N_FunctionParam"),
    UniqueString.uniqueStringOf("N_GeneralId"),
    UniqueString.uniqueStringOf("N_GenInfixOp"),
    UniqueString.uniqueStringOf("N_GenNonExpPrefixOp"),
    UniqueString.uniqueStringOf("N_GenPostfixOp"),
    UniqueString.uniqueStringOf("N_GenPrefixOp"),
    UniqueString.uniqueStringOf("N_IdentDecl"),
    UniqueString.uniqueStringOf("N_Real"), // was N_Identifier
    UniqueString.uniqueStringOf("N_IdentifierTuple"),
    UniqueString.uniqueStringOf("N_IdentLHS"),
    UniqueString.uniqueStringOf("N_IdPrefix"),
    UniqueString.uniqueStringOf("N_IdPrefixElement"),
    UniqueString.uniqueStringOf("N_IfThenElse"),
    UniqueString.uniqueStringOf("N_InfixDecl"),
    UniqueString.uniqueStringOf("N_InfixExpr"),
    UniqueString.uniqueStringOf("N_InfixLHS"),
    UniqueString.uniqueStringOf("N_InfixOp"),
    UniqueString.uniqueStringOf("N_InnerProof"),
    UniqueString.uniqueStringOf("N_Instance"),
    UniqueString.uniqueStringOf("N_NonLocalInstance"),
    UniqueString.uniqueStringOf("N_Integer"),
    UniqueString.uniqueStringOf("N_LeafProof"),
    UniqueString.uniqueStringOf("N_LetDefinitions"),
    UniqueString.uniqueStringOf("N_LetIn"),
    UniqueString.uniqueStringOf("N_MaybeBound"),
    UniqueString.uniqueStringOf("N_Module"),
    UniqueString.uniqueStringOf("N_ModuleDefinition"),
    UniqueString.uniqueStringOf("N_NonExpPrefixOp"),
    UniqueString.uniqueStringOf("N_Number"),
    UniqueString.uniqueStringOf("N_NumberedAssumeProve"),
    UniqueString.uniqueStringOf("N_OpApplication"),
    UniqueString.uniqueStringOf("N_OpArgs"),
    UniqueString.uniqueStringOf("N_OperatorDefinition"),
    UniqueString.uniqueStringOf("N_OtherArm"),
    UniqueString.uniqueStringOf("N_ParamDecl"),
    UniqueString.uniqueStringOf("N_ParamDeclaration"),
    UniqueString.uniqueStringOf("N_ParenExpr"),
    UniqueString.uniqueStringOf("N_PostfixDecl"),
    UniqueString.uniqueStringOf("N_PostfixExpr"),
    UniqueString.uniqueStringOf("N_PostfixLHS"),
    UniqueString.uniqueStringOf("N_PostfixOp"),
    UniqueString.uniqueStringOf("N_PrefixDecl"),
    UniqueString.uniqueStringOf("N_PrefixExpr"),
    UniqueString.uniqueStringOf("N_PrefixLHS"),
    UniqueString.uniqueStringOf("N_PrefixOp"),
    UniqueString.uniqueStringOf("N_Proof"),
    UniqueString.uniqueStringOf("N_ProofLet"),
    UniqueString.uniqueStringOf("N_ProofName"),
    UniqueString.uniqueStringOf("N_ProofStatement"),
    UniqueString.uniqueStringOf("N_ProofStep"),
    UniqueString.uniqueStringOf("N_QEDStep"),
    UniqueString.uniqueStringOf("N_QuantBound"),
    UniqueString.uniqueStringOf("N_RcdConstructor"),
    UniqueString.uniqueStringOf("N_RecordComponent"),
    UniqueString.uniqueStringOf("N_SetEnumerate"),
    UniqueString.uniqueStringOf("N_SetExcept"),
    UniqueString.uniqueStringOf("N_SetOfAll"),
    UniqueString.uniqueStringOf("N_SetOfFcns"),
    UniqueString.uniqueStringOf("N_SetOfRcds"),
    UniqueString.uniqueStringOf("N_SExceptSpec"),
    UniqueString.uniqueStringOf("N_SFcnDecl"),
    UniqueString.uniqueStringOf("N_String"),
    UniqueString.uniqueStringOf("N_SubsetOf"),
    UniqueString.uniqueStringOf("N_Substitution"),
    UniqueString.uniqueStringOf("N_TempDecl"),
    UniqueString.uniqueStringOf("N_Theorem"),
    UniqueString.uniqueStringOf("N_Tuple"),
    UniqueString.uniqueStringOf("N_UnBoundedOrBoundedChoose"),
    UniqueString.uniqueStringOf("N_UnboundedQuant"),
    UniqueString.uniqueStringOf("N_VariableDeclaration"),
    UniqueString.uniqueStringOf("Token ="),
    UniqueString.uniqueStringOf("Token \\in"),
    UniqueString.uniqueStringOf("Not a node") };
}

