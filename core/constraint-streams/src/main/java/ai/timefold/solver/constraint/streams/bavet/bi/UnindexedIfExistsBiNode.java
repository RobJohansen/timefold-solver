package ai.timefold.solver.constraint.streams.bavet.bi;

import ai.timefold.solver.constraint.streams.bavet.common.AbstractUnindexedIfExistsNode;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.BiTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.api.function.TriPredicate;

final class UnindexedIfExistsBiNode<A, B, C> extends AbstractUnindexedIfExistsNode<BiTuple<A, B>, C> {

    private final TriPredicate<A, B, C> filtering;

    public UnindexedIfExistsBiNode(boolean shouldExist,
            int inputStoreIndexLeftCounterEntry, int inputStoreIndexRightEntry,
            TupleLifecycle<BiTuple<A, B>> nextNodesTupleLifecycle) {
        this(shouldExist,
                inputStoreIndexLeftCounterEntry, -1, inputStoreIndexRightEntry, -1,
                nextNodesTupleLifecycle, null);
    }

    public UnindexedIfExistsBiNode(boolean shouldExist,
            int inputStoreIndexLeftCounterEntry, int inputStoreIndexLeftTrackerList, int inputStoreIndexRightEntry,
            int inputStoreIndexRightTrackerList,
            TupleLifecycle<BiTuple<A, B>> nextNodesTupleLifecycle,
            TriPredicate<A, B, C> filtering) {
        super(shouldExist,
                inputStoreIndexLeftCounterEntry, inputStoreIndexLeftTrackerList, inputStoreIndexRightEntry,
                inputStoreIndexRightTrackerList,
                nextNodesTupleLifecycle, filtering != null);
        this.filtering = filtering;
    }

    @Override
    protected boolean testFiltering(BiTuple<A, B> leftTuple, UniTuple<C> rightTuple) {
        return filtering.test(leftTuple.factA, leftTuple.factB, rightTuple.factA);
    }

}
