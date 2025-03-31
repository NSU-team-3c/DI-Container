package ru.nsu.context;

import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;
import ru.nsu.bean.BeanObject;

import javax.inject.Inject;
import javax.inject.Named;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DependenciesManager {

    private final Map<String, BeanObject> beans;
    private final DefaultDirectedGraph<String, DefaultEdge> graph;
    private final List<String> sortedBeans;

    public DependenciesManager(Map<String, BeanObject> beans) {
        this.beans = beans;
        this.graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        this.sortedBeans = new ArrayList<>();
    }


    public List<String> resolveDependencies() {
        beans.keySet().forEach(graph::addVertex);
        beans.forEach((beanName, beanDefinition) -> addDependenciesToGraph(beanName, beanDefinition, graph));
        checkForCycleDependency(graph);
        TopologicalOrderIterator<String, DefaultEdge> orderIterator = new TopologicalOrderIterator<>(graph);
        orderIterator.forEachRemaining(sortedBeans::add);
        return sortedBeans;
    }

    private void checkForCycleDependency(DefaultDirectedGraph<String, DefaultEdge> graph) {
        CycleDetector<String, DefaultEdge> cycleDetector = new CycleDetector<>(graph);
        if (cycleDetector.detectCycles()) {
            throw new RuntimeException("Detected cyclic dependencies");
        }
    }

    private void addDependenciesToGraph(String beanName, BeanObject bean, DefaultDirectedGraph<String, DefaultEdge> graph) {
        addInjectedFields(beanName, bean, graph);
        addInjectProviderFields(beanName, bean, graph);
        addConstructorParameters(beanName, bean, graph);
    }

    private void addInjectProviderFields(String beanName, BeanObject bean, DefaultDirectedGraph<String, DefaultEdge> graph) {
        if (bean != null) {
            var injectedProviderFieldsFields = bean.getInjectedProviderFields();
            if (injectedProviderFieldsFields != null) {
                injectedProviderFieldsFields.forEach((field) -> {
                    Named namedAnnotation = field.getAnnotation(Named.class);
                    if (namedAnnotation != null && graph.containsVertex(namedAnnotation.value())) {
                        graph.addEdge(beanName, namedAnnotation.value());
                    }
                });
            }
        }
    }

    private void addInjectedFields(String beanName, BeanObject bean, DefaultDirectedGraph<String, DefaultEdge> graph) {
        if (bean != null) {
            var injectedFields = bean.getInjectedFields();
            if (injectedFields != null) {
                injectedFields.forEach((field) -> {
                    Named namedAnnotation = field.getAnnotation(Named.class);
                    if (namedAnnotation != null && graph.containsVertex(namedAnnotation.value())) {
                        graph.addEdge(beanName, namedAnnotation.value());
                    }
                });
            }
        }
    }

    private void addConstructorParameters(String beanName, BeanObject bean, DefaultDirectedGraph<String, DefaultEdge> graph) {
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