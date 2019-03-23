package org.utu.studentcare.calculator;

import org.utu.studentcare.applogic.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Course specification parser AST.
 *
 * Oheisluokka, EI tarvitse ymmärtää eikä muokata työn tekemiseksi!
 */
class ASTNodes {
    static class ASTNode {
    }

    static class ValRangePlus extends ValRange {
        ValRangePlus(ValRange v) {
            super(v.min, v.max);
        }
        ValRangePlus(double min, double max) {
            super(min, max);
        }

        ValRangePlus add(ValRangePlus other) {
            return new ValRangePlus(min + other.min, max + other.max);
        }

        ValRangePlus sub(ValRangePlus other) {
            return new ValRangePlus(min - other.min, max - other.max);
        }

        ValRangePlus mul(ValRangePlus other) {
            return new ValRangePlus(min * other.min, max * other.max);
        }

        ValRangePlus div(ValRangePlus other) {
            return new ValRangePlus(min / other.max, max / other.min);
        }
    }

    static class Text extends ASTNode {
        final String name;

        public Text(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    static class ExerciseSpecsNode extends ASTNode implements ExerciseSpecs {
        private final List<ExerciseSpec> exerciseDecls = new ArrayList<>();
        private final List<GradingSpec> gradingDecls = new ArrayList<>();

        @Override
        public List<ExerciseSpec> getExerciseDecls() {
            return exerciseDecls;
        }

        @Override
        public List<GradingSpec> getGradingDecls() {
            return gradingDecls;
        }

        ExerciseSpecsNode add(ExerciseSpec e) {
            exerciseDecls.add(e);
            return this;
        }

        public ExerciseSpecsNode add(ASTNode node) {
            if (node instanceof ExerciseSpec)
                return add((ExerciseSpec) node);
            if (node instanceof GradingSpec)
                return add((GradingSpec) node);
            throw new Error("Wrong node type!");
        }

        ExerciseSpecsNode add(GradingSpec g) {
            gradingDecls.add(g);
            return this;
        }

        public ExerciseSpecsNode combine(ASTNode other) {
            return combine((ExerciseSpecsNode) other);
        }

        ExerciseSpecsNode combine(ExerciseSpecsNode other) {
            exerciseDecls.addAll(other.exerciseDecls);
            gradingDecls.addAll(other.gradingDecls);
            return this;
        }

        @Override
        public String toString() {
            StringBuilder tmp = new StringBuilder("{ ");
            for (ExerciseSpec e : getExerciseDecls()) tmp.append("  DEF ").append(e.toString()).append(",\n");
            for (GradingSpec g : getGradingDecls()) tmp.append("  ").append(g.toString()).append(",\n");
            return tmp.substring(0, tmp.length() - ",\n".length()) + "\n}";
        }
    }

    static class ExerciseDecl extends ASTNode implements ExerciseSpec {
        final String id;
        final String description;
        final ValRangePlus range;

        ExerciseDecl(String id, String description, double min, double max) {
            this.id = id;
            this.description = description;
            range = new ValRangePlus(min, max);
        }

        public ExerciseDecl(ASTNode description, ASTNode max, ASTNode min, ASTNode ref) {
            this(((Text) ref).name, ((Text) description).name, ((ValExpr) min).range.min, ((ValExpr) max).range.min);
        }

        @Override
        public String toString() {
            return id + "[" + range + "]: \"" + description + "\"";
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public ValRangePlus getRange() {
            return range;
        }
    }

    static class GradingDecl extends ASTNode implements GradingSpec {
        final String id;
        final Expression expr;

        GradingDecl(String id, Expression expr) {
            this.id = id;
            this.expr = expr;
        }

        public ValRangePlus value(ValueContext context) {
            return getExpr().getValue(context);
        }

        public GradingDecl(ASTNode expr, ASTNode ref) {
            this(((Text) ref).name, (Expression) expr);
        }

        @Override
        public String toString() {
            return id + " = " + expr.toString();
        }

        public String getId() {
            return id;
        }

        Expression getExpr() {
            return expr;
        }
    }

    static abstract class Expression extends ASTNode {
        protected abstract ValRangePlus getValue(ValueContext context);
    }

    static class IfthenExpr extends Expression {
        final Expression condLeft;
        final Expression condRight;
        final Expression thenBranch;
        final Expression elseBranch;
        final String operator;

        public IfthenExpr(Expression condLeft, String operator, Expression condRight, Expression thenBranch, Expression elseBranch) {
            this.condLeft = condLeft;
            this.condRight = condRight;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
            this.operator = operator;
        }

        public IfthenExpr(ASTNode elseBranch, ASTNode thenBranch, ASTNode condRight, ASTNode operator, ASTNode condLeft) {
            this.condLeft = (Expression) condLeft;
            this.condRight = (Expression) condRight;
            this.thenBranch = (Expression) thenBranch;
            this.elseBranch = (Expression) elseBranch;
            this.operator = ((Text) operator).name;
        }

        @Override
        public String toString() {
            return "IF " + condLeft + " " + operator + " " + condRight + " THEN " + thenBranch + " ELSE " + elseBranch;
        }

        @Override
        public ValRangePlus getValue(ValueContext context) {
            ValRangePlus left = condLeft.getValue(context),
                    right = condRight.getValue(context),
                    thenB = thenBranch.getValue(context),
                    elseB = elseBranch.getValue(context);

            switch (operator) {
                case "<":
                    return new ValRangePlus(
                            (left.min < right.max) ? thenB.min : elseB.min,
                            (left.max < right.min) ? thenB.max : elseB.max
                    );

                case "<=":
                    return new ValRangePlus(
                            (left.min <= right.max) ? thenB.min : elseB.min,
                            (left.max <= right.min) ? thenB.max : elseB.max
                    );
                case ">":
                    return new ValRangePlus(
                            (left.max > right.min) ? thenB.min : elseB.min,
                            (left.min > right.max) ? thenB.max : elseB.max
                    );
                case ">=":
                    return new ValRangePlus(
                            (left.max >= right.min) ? thenB.min : elseB.min,
                            (left.min >= right.max) ? thenB.max : elseB.max
                    );
                case "==":
                    return new ValRangePlus(
                            Math.abs(left.min - right.min) < 0.1 ? thenB.min : elseB.min,
                            Math.abs(left.max - right.max) < 0.1 ? thenB.max : elseB.max
                    );
                case "!=":
                    return new ValRangePlus(
                            Math.abs(left.min - right.min) >= 0.1 ? thenB.min : elseB.min,
                            Math.abs(left.max - right.max) >= 0.1 ? thenB.max : elseB.max
                    );
            /*
            case "<": if (left.min < right.max) { return left.max < right.min ? thenB : new ValRangePlus(); } else return elseB;
            case "<=": if (left.min <= right.max) { return left.max <= right.min ? thenB : new ValRangePlus(); } else return elseB;
            case ">": if (left.max > right.min) { return left.min > right.max ? thenB : new ValRangePlus(); } else return elseB;
            case ">=": if (left.max >= right.min) { return left.min >= right.max ? thenB : new ValRangePlus(); } else return elseB;
            case "==": if (left.max-right.min < 0.1 && right.max-left.min < 0.1) return thenB; else return elseB;
            case "!=": if (left.max-right.min < 0.1 && right.max-left.min < 0.1) return elseB; else return thenB;
            */
                default:
                    System.out.println(operator);
                    throw new IllegalStateException();
            }
        }
    }

    static class RangeExpr extends Expression {
        final ValRangePlus range;

        RangeExpr(double min, double max) {
            range = new ValRangePlus(min, max);
        }

        public RangeExpr(ValRangePlus range) {
            this(range.min, range.max);
        }

        public ValRangePlus getValue(ValueContext context) {
            return range;
        }

        @Override
        public String toString() {
            return range.toString();
        }
    }

    static class RefExpr extends Expression {
        final String ref;

        public RefExpr(String ref) {
            this.ref = ref;
        }

        public RefExpr(ASTNode ref) {
            this.ref = ((Text) ref).name;
        }

        public ValRangePlus getValue(ValueContext context) {
            try {
                return new ValRangePlus(context.get(ref));
            } catch (AppLogicException e) {
                throw new RuntimeException("Parsed reference not found!");
            }
        }

        @Override
        public String toString() {
            return ref;
        }
    }

    static class ValExpr extends Expression {
        final ValRangePlus range;

        public ValExpr(double value) {
            this.range = new ValRangePlus(value, value);
        }

        public ValRangePlus getValue(ValueContext context) {
            return range;
        }

        @Override
        public String toString() {
            return range.toString();
        }
    }

    static class OpExpr extends Expression {
        final char operator;
        private final List<Expression> args;

        OpExpr(char operator, List<ASTNode> args) {
            this.operator = operator;
            ArrayList<Expression> exprs = new ArrayList<>();
            for (ASTNode n : args) exprs.add((Expression) n);
            this.args = exprs;
        }

        public OpExpr(char operator, ASTNode... args) {
            this(operator, List.of(args));
        }

        private Expression left() {
            return args.get(0);
        }

        private Expression right() {
            return args.get(1);
        }

        @Override
        public ValRangePlus getValue(ValueContext context) {
            ValRangePlus left = left().getValue(context),
                    right = right().getValue(context);

            switch (operator) {
                case '+':
                    return left.add(right);
                case '-':
                    return left.sub(right);
                case '*':
                    return left.mul(right);
                case '/':
                    return left.div(right);
                case '^':
                    List<Double> vals = List.of(
                            Math.pow(left.min, right.min),
                            Math.pow(left.max, right.min),
                            Math.pow(left.min, right.max),
                            Math.pow(left.max, right.max)
                    );
                    return new ValRangePlus(
                            vals.stream().min(Double::compareTo).orElseThrow(),
                            vals.stream().max(Double::compareTo).orElseThrow()
                    );
                case 'R':
                    double a = Math.sqrt(left.min),
                            b = Math.sqrt(left.max);

                    return new ValRangePlus(Math.min(a, b), Math.max(a, b));
                default:
                    throw new IllegalStateException();
            }
        }

        public String toString() {
            if (args.size() == 1)
                return operator + " " + left();
            else
                return left() + " " + operator + " " + right();
        }
    }
}