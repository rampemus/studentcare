package org.utu.studentcare.calculator;

import org.parboiled.BaseParser;
import org.parboiled.Rule;
import org.parboiled.annotations.BuildParseTree;
import org.parboiled.support.Var;

/**
 * Course specification parser grammar.
 *
 * Oheisluokka, EI tarvitse ymmärtää eikä muokata työn tekemiseksi!
 */
@BuildParseTree
class SpecParserGrammar extends BaseParser<ASTNodes.ASTNode> {
    public Rule InputLine() {
        return Sequence(ExerciseSpec(), EOI);
    }

    Rule ExerciseSpec() {
        return Sequence("{ ", Declarations(), "} ");
    }

    Rule Declarations() {
        return Sequence(Declaration(), ZeroOrMore(", ", Declaration(), push(((ASTNodes.ExerciseSpecsNode) pop(1)).combine(pop()))));
    }

    Rule Declaration() {
        return Sequence(FirstOf(ExerciseDeclaration(), GradingDeclaration()), push(new ASTNodes.ExerciseSpecsNode().add(pop())));
    }

    Rule ExerciseDeclaration() {
        return Sequence("DEF ", Identifier(), "[ ", Number(), ".. ", Number(), "] ", ": ", StringLiteral(), push(new ASTNodes.ExerciseDecl(pop(), pop(), pop(), pop())));
    }

    Rule GradingDeclaration() {
        return Sequence(Identifier(), "= ", Expression(), push(new ASTNodes.GradingDecl(pop(), pop())));
    }

    Rule StringLiteral() {
        return Sequence(
                '"',
                ZeroOrMore(
                        Sequence(TestNot(AnyOf("\r\n\"\\")), ANY)
                ).suppressSubnodes(),
                push(new ASTNodes.Text(match())),
                '"'
        );
    }

    Rule Comparison() {
        return Sequence(PlusMinusExpression(), FirstOf("<=", ">=", "<", ">", "==", "!="), push(new ASTNodes.Text(match())), WhiteSpace(), PlusMinusExpression());
    }

    Rule Expression() {
        return FirstOf(Sequence("IF ", Comparison(), "THEN ", PlusMinusExpression(), "ELSE ", Expression(), push(new ASTNodes.IfthenExpr(pop(), pop(), pop(), pop(), pop()))), PlusMinusExpression());
    }

    Rule PlusMinusExpression() {
        return OperatorRule(Term(), FirstOf("+ ", "- "));
    }

    Rule Term() {
        return OperatorRule(Factor(), FirstOf("* ", "/ "));
    }

    Rule Factor() {
        return OperatorRule(Atom(), toRule("^ "));
    }

    Rule OperatorRule(Rule subRule, Rule operatorRule) {
        Var<Character> op = new Var<>();
        return Sequence(
                subRule,
                ZeroOrMore(
                        operatorRule, op.set(matchedChar()),
                        subRule,
                        push(new ASTNodes.OpExpr(op.get(), pop(1), pop()))
                )
        );
    }

    Rule Atom() {
        return FirstOf(Number(), Reference(), SquareRoot(), Parens());
    }

    Rule Reference() {
        return Sequence(Identifier(), push(new ASTNodes.RefExpr(pop())));
    }

    Rule SquareRoot() {
        return Sequence("SQRT", Parens(), push(new ASTNodes.OpExpr('R', pop())));
    }

    Rule Parens() {
        return Sequence("( ", Expression(), ") ");
    }

    Rule Identifier() {
        return Sequence(OneOrMore(Letter()), push(new ASTNodes.Text(match())), WhiteSpace());
    }

    Rule Number() {
        return Sequence(
                Sequence(
                        Optional(Ch('-')),
                        OneOrMore(Digit()),
                        Optional(Ch('.'), OneOrMore(Digit()))
                ),
                push(new ASTNodes.ValExpr(Double.parseDouble(matchOrDefault("0")))),
                WhiteSpace()
        );
    }

    Rule Digit() {
        return CharRange('0', '9');
    }

    Rule Letter() {
        return CharRange('a', 'z');
    }

    Rule WhiteSpace() {
        return ZeroOrMore(AnyOf(" \t\f\n"));
    }

    @Override
    protected Rule fromStringLiteral(String string) {
        return string.endsWith(" ") ?
                Sequence(String(string.substring(0, string.length() - 1)), WhiteSpace()) :
                String(string);
    }
}