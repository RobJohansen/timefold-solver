package ai.timefold.solver.core.impl.domain.variable.inverserelation;

import java.util.List;

import ai.timefold.solver.core.api.domain.variable.ListVariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

public class SingletonListInverseVariableListener<Solution_>
        implements ListVariableListener<Solution_, Object, Object>, SingletonInverseVariableSupply {

    protected final InverseRelationShadowVariableDescriptor<Solution_> shadowVariableDescriptor;
    protected final ListVariableDescriptor<Solution_> sourceVariableDescriptor;

    public SingletonListInverseVariableListener(
            InverseRelationShadowVariableDescriptor<Solution_> shadowVariableDescriptor,
            ListVariableDescriptor<Solution_> sourceVariableDescriptor) {
        this.shadowVariableDescriptor = shadowVariableDescriptor;
        this.sourceVariableDescriptor = sourceVariableDescriptor;
    }

    @Override
    public void beforeEntityAdded(ScoreDirector<Solution_> scoreDirector, Object entity) {
        // Do nothing
    }

    @Override
    public void afterEntityAdded(ScoreDirector<Solution_> scoreDirector, Object entity) {
        for (Object element : sourceVariableDescriptor.getListVariable(entity)) {
            setInverse((InnerScoreDirector<Solution_, ?>) scoreDirector, element, entity, null);
        }
    }

    @Override
    public void beforeEntityRemoved(ScoreDirector<Solution_> scoreDirector, Object entity) {
        // Do nothing
    }

    @Override
    public void afterEntityRemoved(ScoreDirector<Solution_> scoreDirector, Object entity) {
        InnerScoreDirector<Solution_, ?> innerScoreDirector = (InnerScoreDirector<Solution_, ?>) scoreDirector;
        for (Object element : sourceVariableDescriptor.getListVariable(entity)) {
            setInverse(innerScoreDirector, element, null, entity);
        }
    }

    @Override
    public void afterListVariableElementUnassigned(ScoreDirector<Solution_> scoreDirector, Object element) {
        InnerScoreDirector<Solution_, ?> innerScoreDirector = (InnerScoreDirector<Solution_, ?>) scoreDirector;
        innerScoreDirector.beforeVariableChanged(shadowVariableDescriptor, element);
        shadowVariableDescriptor.setValue(element, null);
        innerScoreDirector.afterVariableChanged(shadowVariableDescriptor, element);
    }

    @Override
    public void beforeListVariableChanged(ScoreDirector<Solution_> scoreDirector, Object entity, int fromIndex, int toIndex) {
        // Do nothing
    }

    @Override
    public void afterListVariableChanged(ScoreDirector<Solution_> scoreDirector, Object entity, int fromIndex, int toIndex) {
        InnerScoreDirector<Solution_, ?> innerScoreDirector = (InnerScoreDirector<Solution_, ?>) scoreDirector;
        List<Object> listVariable = sourceVariableDescriptor.getListVariable(entity);
        for (int i = fromIndex; i < toIndex; i++) {
            Object element = listVariable.get(i);
            if (shadowVariableDescriptor.getValue(element) != entity) {
                innerScoreDirector.beforeVariableChanged(shadowVariableDescriptor, element);
                shadowVariableDescriptor.setValue(element, entity);
                innerScoreDirector.afterVariableChanged(shadowVariableDescriptor, element);
            }
        }
    }

    private void setInverse(InnerScoreDirector<Solution_, ?> scoreDirector,
            Object element, Object inverseEntity, Object expectedOldInverseEntity) {
        Object oldInverseEntity = shadowVariableDescriptor.getValue(element);
        if (oldInverseEntity == inverseEntity) {
            return;
        }
        if (scoreDirector.expectShadowVariablesInCorrectState() && oldInverseEntity != expectedOldInverseEntity) {
            throw new IllegalStateException("The entity (" + inverseEntity
                    + ") has a list variable (" + sourceVariableDescriptor.getVariableName()
                    + ") and one of its elements (" + element
                    + ") which has a shadow variable (" + shadowVariableDescriptor.getVariableName()
                    + ") has an oldInverseEntity (" + oldInverseEntity + ") which is not that entity.\n"
                    + "Verify the consistency of your input problem for that shadow variable.");
        }
        scoreDirector.beforeVariableChanged(shadowVariableDescriptor, element);
        shadowVariableDescriptor.setValue(element, inverseEntity);
        scoreDirector.afterVariableChanged(shadowVariableDescriptor, element);
    }

    @Override
    public Object getInverseSingleton(Object planningValue) {
        return shadowVariableDescriptor.getValue(planningValue);
    }
}
