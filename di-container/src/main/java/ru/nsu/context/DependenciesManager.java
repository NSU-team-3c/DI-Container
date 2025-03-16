package ru.nsu.context;

import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;
import ru.nsu.bean.Bean;

import javax.inject.Inject;
import javax.inject.Named;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DependenciesManager {

    private final Map<String, Bean> beans;

    public DependenciesManager(Map<String, Bean> beans) {
        this.beans = beans;
    }


    public List<String> resolveDependencies() {
        DefaultDirectedGraph<String, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        beans.keySet().forEach(graph::addVertex);
        beans.forEach((beanName, beanDefinition) -> addDependenciesToGraph(beanName, beanDefinition, graph));

        CycleDetector<String, DefaultEdge> cycleDetector = new CycleDetector<>(graph);
        if (cycleDetector.detectCycles()){
            throw new RuntimeException("Detected cyclic dependencies among beans");
        }

        TopologicalOrderIterator<String, DefaultEdge> orderIterator = new TopologicalOrderIterator<>(graph);
        List<String> sortedBeans = new ArrayList<>();
        orderIterator.forEachRemaining(sortedBeans::add);

        return sortedBeans;
    }

    private void addDependenciesToGraph(String beanName, Bean bean, DefaultDirectedGraph<String, DefaultEdge> graph) {
        if (bean != null) {
            var injectedFields =  bean.getInjectedFields();
            if (injectedFields != null) {
                injectedFields.forEach((field) -> {
                    Named namedAnnotation = field.getAnnotation(Named.class);
                    if (namedAnnotation != null && graph.containsVertex(namedAnnotation.value())) {
                        graph.addEdge(beanName, namedAnnotation.value());
                    }
                });
            }
        }

        if (bean != null) {
            var injectedProviderFieldsFields =  bean.getInjectedProviderFields();
            if (injectedProviderFieldsFields != null) {
                injectedProviderFieldsFields.forEach((field) -> {
                    Named namedAnnotation = field.getAnnotation(Named.class);
                    if (namedAnnotation != null && graph.containsVertex(namedAnnotation.value())) {
                        graph.addEdge(beanName, namedAnnotation.value());
                    }
                });
            }
        }

        Constructor<?> constructor = bean.getConstructor();
        if (constructor != null && constructor.isAnnotationPresent(Inject.class)) {
            for (Parameter parameter : constructor.getParameters()) {
                Named named = parameter.getAnnotation(Named.class);
                String depName = (named != null) ? named.value() : parameter.getType().getSimpleName();
                if (graph.containsVertex(depName)) {
                    graph.addEdge(beanName, depName);
                }
            }
        }
    }
}