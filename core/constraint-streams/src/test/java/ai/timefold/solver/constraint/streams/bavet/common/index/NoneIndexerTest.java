package ai.timefold.solver.constraint.streams.bavet.common.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.util.ElementAwareListEntry;

import org.junit.jupiter.api.Test;

class NoneIndexerTest extends AbstractIndexerTest {

    @Test
    void isEmpty() {
        Indexer<UniTuple<String>> indexer = new NoneIndexer<>();
        assertSoftly(softly -> {
            softly.assertThat(getTuples(indexer)).isEmpty();
            softly.assertThat(indexer.isEmpty()).isTrue();
        });
    }

    @Test
    void put() {
        Indexer<UniTuple<String>> indexer = new NoneIndexer<>();
        UniTuple<String> annTuple = newTuple("Ann-F-40");
        assertThat(indexer.size(NoneIndexProperties.INSTANCE)).isEqualTo(0);
        indexer.put(NoneIndexProperties.INSTANCE, annTuple);
        assertThat(indexer.size(NoneIndexProperties.INSTANCE)).isEqualTo(1);
        assertSoftly(softly -> {
            softly.assertThat(indexer.isEmpty()).isFalse();
            softly.assertThat(getTuples(indexer)).containsExactly(annTuple);
        });
    }

    @Test
    void removeTwice() {
        Indexer<UniTuple<String>> indexer = new NoneIndexer<>();
        UniTuple<String> annTuple = newTuple("Ann-F-40");
        ElementAwareListEntry<UniTuple<String>> annEntry = indexer.put(NoneIndexProperties.INSTANCE, annTuple);
        assertSoftly(softly -> {
            softly.assertThat(indexer.isEmpty()).isFalse();
            softly.assertThat(getTuples(indexer)).containsExactly(annTuple);
        });

        indexer.remove(NoneIndexProperties.INSTANCE, annEntry);
        assertSoftly(softly -> {
            softly.assertThat(indexer.isEmpty()).isTrue();
            softly.assertThat(getTuples(indexer)).isEmpty();
        });
        assertThatThrownBy(() -> indexer.remove(NoneIndexProperties.INSTANCE, annEntry))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void visit() {
        Indexer<UniTuple<String>> indexer = new NoneIndexer<>();

        UniTuple<String> annTuple = newTuple("Ann-F-40");
        indexer.put(NoneIndexProperties.INSTANCE, annTuple);
        UniTuple<String> bethTuple = newTuple("Beth-F-30");
        indexer.put(NoneIndexProperties.INSTANCE, bethTuple);

        assertThat(getTuples(indexer)).containsOnly(annTuple, bethTuple);
    }

    private static UniTuple<String> newTuple(String factA) {
        return new UniTuple<>(factA, 0);
    }

}
