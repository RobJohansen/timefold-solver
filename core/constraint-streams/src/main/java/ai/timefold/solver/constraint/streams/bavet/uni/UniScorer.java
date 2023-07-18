package ai.timefold.solver.constraint.streams.bavet.uni;

import java.util.function.BiFunction;

import ai.timefold.solver.constraint.streams.bavet.common.AbstractScorer;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.UniTuple;
import ai.timefold.solver.constraint.streams.common.inliner.UndoScoreImpacter;
import ai.timefold.solver.constraint.streams.common.inliner.WeightedScoreImpacter;

final class UniScorer<A> extends AbstractScorer<UniTuple<A>> {

    private final BiFunction<WeightedScoreImpacter<?, ?>, A, UndoScoreImpacter> scoreImpacter;

    public UniScorer(WeightedScoreImpacter<?, ?> weightedScoreImpacter,
            BiFunction<WeightedScoreImpacter<?, ?>, A, UndoScoreImpacter> scoreImpacter, int inputStoreIndex) {
        super(weightedScoreImpacter, inputStoreIndex);
        this.scoreImpacter = scoreImpacter;
    }

    @Override
    protected UndoScoreImpacter impact(UniTuple<A> tuple) {
        try {
            return scoreImpacter.apply(weightedScoreImpacter, tuple.factA);
        } catch (Exception e) {
            throw createExceptionOnImpact(tuple, e);
        }
    }
}
