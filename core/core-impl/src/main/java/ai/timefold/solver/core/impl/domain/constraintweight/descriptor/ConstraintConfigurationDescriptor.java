package ai.timefold.solver.core.impl.domain.constraintweight.descriptor;

import static ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorFactory.MemberAccessorType.FIELD_OR_READ_METHOD;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ai.timefold.solver.core.api.domain.constraintweight.ConstraintConfiguration;
import ai.timefold.solver.core.api.domain.constraintweight.ConstraintConfigurationProvider;
import ai.timefold.solver.core.api.domain.constraintweight.ConstraintWeight;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.domain.common.ReflectionHelper;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.policy.DescriptorPolicy;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class ConstraintConfigurationDescriptor<Solution_> {

    private final SolutionDescriptor<Solution_> solutionDescriptor;

    private final Class<?> constraintConfigurationClass;
    private String constraintPackage;

    private final Map<String, ConstraintWeightDescriptor<Solution_>> constraintWeightDescriptorMap;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    public ConstraintConfigurationDescriptor(SolutionDescriptor<Solution_> solutionDescriptor,
            Class<?> constraintConfigurationClass) {
        this.solutionDescriptor = solutionDescriptor;
        this.constraintConfigurationClass = constraintConfigurationClass;
        constraintWeightDescriptorMap = new LinkedHashMap<>();
    }

    public String getConstraintPackage() {
        return constraintPackage;
    }

    public Collection<ConstraintWeightDescriptor<Solution_>> getConstraintWeightDescriptors() {
        return constraintWeightDescriptorMap.values();
    }

    public ConstraintWeightDescriptor<Solution_> getConstraintWeightDescriptor(String propertyName) {
        return constraintWeightDescriptorMap.get(propertyName);
    }

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    public void processAnnotations(DescriptorPolicy descriptorPolicy,
            ScoreDefinition scoreDefinition) {
        processPackAnnotation(descriptorPolicy);
        ArrayList<Method> potentiallyOverwritingMethodList = new ArrayList<>();
        // Iterate inherited members too (unlike for EntityDescriptor where each one is declared)
        // to make sure each one is registered
        for (Class<?> lineageClass : ConfigUtils.getAllAnnotatedLineageClasses(constraintConfigurationClass,
                ConstraintConfiguration.class)) {
            List<Member> memberList = ConfigUtils.getDeclaredMembers(lineageClass);
            for (Member member : memberList) {
                if (member instanceof Method method && potentiallyOverwritingMethodList.stream().anyMatch(
                        m -> member.getName().equals(m.getName()) // Shortcut to discard negatives faster
                                && ReflectionHelper.isMethodOverwritten(method, m.getDeclaringClass()))) {
                    // Ignore member because it is an overwritten method
                    continue;
                }
                processParameterAnnotation(descriptorPolicy, member, scoreDefinition);
            }
            potentiallyOverwritingMethodList.ensureCapacity(potentiallyOverwritingMethodList.size() + memberList.size());
            memberList.stream().filter(member -> member instanceof Method)
                    .forEach(member -> potentiallyOverwritingMethodList.add((Method) member));
        }
        if (constraintWeightDescriptorMap.isEmpty()) {
            throw new IllegalStateException("The constraintConfigurationClass (" + constraintConfigurationClass
                    + ") must have at least 1 member with a "
                    + ConstraintWeight.class.getSimpleName() + " annotation.");
        }
    }

    private void processPackAnnotation(DescriptorPolicy descriptorPolicy) {
        ConstraintConfiguration packAnnotation = constraintConfigurationClass.getAnnotation(ConstraintConfiguration.class);
        if (packAnnotation == null) {
            throw new IllegalStateException("The constraintConfigurationClass (" + constraintConfigurationClass
                    + ") has been specified as a @" + ConstraintConfigurationProvider.class.getSimpleName()
                    + " in the solution class (" + solutionDescriptor.getSolutionClass() + ")," +
                    " but does not have a @" + ConstraintConfiguration.class.getSimpleName() + " annotation.");
        }
        // If a @ConstraintConfiguration extends a @ConstraintConfiguration, their constraintPackage might differ.
        // So the ConstraintWeightDescriptors parse packAnnotation.constraintPackage() themselves.
        constraintPackage = packAnnotation.constraintPackage();
        if (constraintPackage.isEmpty()) {
            Package pack = constraintConfigurationClass.getPackage();
            constraintPackage = (pack == null) ? "" : pack.getName();
        }
    }

    private void processParameterAnnotation(DescriptorPolicy descriptorPolicy, Member member,
            ScoreDefinition scoreDefinition) {
        if (((AnnotatedElement) member).isAnnotationPresent(ConstraintWeight.class)) {
            MemberAccessor memberAccessor = descriptorPolicy.getMemberAccessorFactory().buildAndCacheMemberAccessor(member,
                    FIELD_OR_READ_METHOD, ConstraintWeight.class, descriptorPolicy.getDomainAccessType());
            if (constraintWeightDescriptorMap.containsKey(memberAccessor.getName())) {
                MemberAccessor duplicate = constraintWeightDescriptorMap.get(memberAccessor.getName()).getMemberAccessor();
                throw new IllegalStateException("The constraintConfigurationClass (" + constraintConfigurationClass
                        + ") has a @" + ConstraintWeight.class.getSimpleName()
                        + " annotated member (" + memberAccessor
                        + ") that is duplicated by a member (" + duplicate + ").\n"
                        + "Maybe the annotation is defined on both the field and its getter.");
            }
            if (!scoreDefinition.getScoreClass().isAssignableFrom(memberAccessor.getType())) {
                throw new IllegalStateException("The constraintConfigurationClass (" + constraintConfigurationClass
                        + ") has a @" + ConstraintWeight.class.getSimpleName()
                        + " annotated member (" + memberAccessor
                        + ") with a return type (" + memberAccessor.getType()
                        + ") that is not assignable to the score class (" + scoreDefinition.getScoreClass() + ").\n"
                        + "Maybe make that member (" + memberAccessor.getName() + ") return the score class ("
                        + scoreDefinition.getScoreClass().getSimpleName() + ") instead.");
            }
            ConstraintWeightDescriptor<Solution_> constraintWeightDescriptor = new ConstraintWeightDescriptor<>(this,
                    memberAccessor);
            constraintWeightDescriptorMap.put(memberAccessor.getName(), constraintWeightDescriptor);
        }
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    public SolutionDescriptor<Solution_> getSolutionDescriptor() {
        return solutionDescriptor;
    }

    public Class<?> getConstraintConfigurationClass() {
        return constraintConfigurationClass;
    }

    public ConstraintWeightDescriptor<Solution_> findConstraintWeightDescriptor(String constraintPackage,
            String constraintName) {
        return constraintWeightDescriptorMap.values().stream().filter(
                constraintWeightDescriptor -> constraintWeightDescriptor.getConstraintPackage().equals(constraintPackage)
                        && constraintWeightDescriptor.getConstraintName().equals(constraintName))
                .findFirst().orElse(null);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + constraintConfigurationClass.getName() + ")";
    }
}
