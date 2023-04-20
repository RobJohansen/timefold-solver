package ai.timefold.solver.jackson.impl.domain.solution;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.persistence.common.api.domain.solution.SolutionFileIO;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class JacksonSolutionFileIO<Solution_> implements SolutionFileIO<Solution_> {

    private final Class<Solution_> clazz;
    private final ObjectMapper mapper;

    public JacksonSolutionFileIO(Class<Solution_> clazz) {
        this(clazz, new ObjectMapper());
    }

    public JacksonSolutionFileIO(Class<Solution_> clazz, ObjectMapper mapper) {
        this.clazz = clazz;
        this.mapper = mapper;
        // Loads TimefoldJacksonModule via ServiceLoader, as well as any other Jackson modules on the classpath.
        mapper.findAndRegisterModules();
    }

    @Override
    public String getInputFileExtension() {
        return "json";
    }

    @Override
    public String getOutputFileExtension() {
        return "json";
    }

    @Override
    public Solution_ read(File inputSolutionFile) {
        try {
            return mapper.readValue(inputSolutionFile, clazz);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed reading inputSolutionFile (" + inputSolutionFile + ").", e);
        }
    }

    public Solution_ read(InputStream inputSolutionStream) {
        try {
            return mapper.readValue(inputSolutionStream, clazz);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed reading inputSolutionStream.", e);
        }
    }

    @Override
    public void write(Solution_ solution, File file) {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, solution);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed write", e);
        }
    }

}
