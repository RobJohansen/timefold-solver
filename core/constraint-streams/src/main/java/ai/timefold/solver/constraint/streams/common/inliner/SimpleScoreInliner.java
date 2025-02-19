package ai.timefold.solver.constraint.streams.common.inliner;

import java.util.Map;

import ai.timefold.solver.constraint.streams.common.AbstractConstraint;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;

final class SimpleScoreInliner extends AbstractScoreInliner<SimpleScore> {

    int score;

    SimpleScoreInliner(Map<Constraint, SimpleScore> constraintWeightMap, boolean constraintMatchEnabled) {
        super(constraintWeightMap, constraintMatchEnabled);
    }

    @Override
    public WeightedScoreImpacter<SimpleScore, ?> buildWeightedScoreImpacter(
            AbstractConstraint<?, ?, ?> constraint) {
        SimpleScore constraintWeight = constraintWeightMap.get(constraint);
        SimpleScoreContext context = new SimpleScoreContext(this, constraint, constraintWeight);
        return WeightedScoreImpacter.of(context, SimpleScoreContext::changeScoreBy);
    }

    @Override
    public SimpleScore extractScore(int initScore) {
        return SimpleScore.ofUninitialized(initScore, score);
    }

    @Override
    public String toString() {
        return SimpleScore.class.getSimpleName() + " inliner";
    }

}
