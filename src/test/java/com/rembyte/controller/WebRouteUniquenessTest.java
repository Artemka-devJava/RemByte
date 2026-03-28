package com.rembyte.controller;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Controller;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Проверяет, что в web-контроллерах нет дублирующихся маршрутов
 * (одинаковый HTTP-метод + одинаковый path).
 */
class WebRouteUniquenessTest {

    @Test
    void shouldHaveUniqueWebRoutes() throws Exception {
        Set<Class<?>> controllers = scanControllers("com.rembyte.controller");

        Map<String, List<String>> routeOwners = new LinkedHashMap<>();

        for (Class<?> controllerClass : controllers) {
            List<String> classPaths = extractClassPaths(controllerClass);

            for (Method method : controllerClass.getDeclaredMethods()) {
                RouteData routeData = extractMethodRoutes(method);
                if (routeData == null) continue;

                for (String classPath : classPaths) {
                    for (String methodPath : routeData.paths) {
                        String fullPath = normalizePath(classPath, methodPath);
                        for (RequestMethod httpMethod : routeData.methods) {
                            String key = httpMethod.name() + " " + fullPath;
                            String owner = controllerClass.getSimpleName() + "#" + method.getName();
                            routeOwners.computeIfAbsent(key, k -> new ArrayList<>()).add(owner);
                        }
                    }
                }
            }
        }

        List<String> duplicates = new ArrayList<>();
        routeOwners.forEach((route, owners) -> {
            if (owners.size() > 1) {
                duplicates.add(route + " -> " + owners);
            }
        });

        assertTrue(
                duplicates.isEmpty(),
                () -> "Найдены дублирующиеся web-маршруты:\n" + String.join("\n", duplicates)
        );
    }

    private Set<Class<?>> scanControllers(String basePackage) throws Exception {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Controller.class));
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        Set<Class<?>> result = new LinkedHashSet<>();
        for (var beanDef : scanner.findCandidateComponents(basePackage)) {
            String className = beanDef.getBeanClassName();
            if (className != null) {
                result.add(ClassUtils.forName(className, getClass().getClassLoader()));
            }
        }
        return result;
    }

    private List<String> extractClassPaths(Class<?> clazz) {
        RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);
        if (requestMapping == null) return List.of("");

        String[] values = requestMapping.value().length > 0 ? requestMapping.value() : requestMapping.path();
        if (values.length == 0) return List.of("");

        List<String> paths = new ArrayList<>();
        for (String p : values) paths.add(p == null ? "" : p);
        return paths;
    }

    private RouteData extractMethodRoutes(Method method) {
        if (method.isAnnotationPresent(GetMapping.class)) {
            GetMapping a = method.getAnnotation(GetMapping.class);
            return new RouteData(pathsOf(a.value(), a.path()), EnumSet.of(RequestMethod.GET));
        }
        if (method.isAnnotationPresent(PostMapping.class)) {
            PostMapping a = method.getAnnotation(PostMapping.class);
            return new RouteData(pathsOf(a.value(), a.path()), EnumSet.of(RequestMethod.POST));
        }
        if (method.isAnnotationPresent(PutMapping.class)) {
            PutMapping a = method.getAnnotation(PutMapping.class);
            return new RouteData(pathsOf(a.value(), a.path()), EnumSet.of(RequestMethod.PUT));
        }
        if (method.isAnnotationPresent(DeleteMapping.class)) {
            DeleteMapping a = method.getAnnotation(DeleteMapping.class);
            return new RouteData(pathsOf(a.value(), a.path()), EnumSet.of(RequestMethod.DELETE));
        }
        if (method.isAnnotationPresent(PatchMapping.class)) {
            PatchMapping a = method.getAnnotation(PatchMapping.class);
            return new RouteData(pathsOf(a.value(), a.path()), EnumSet.of(RequestMethod.PATCH));
        }
        if (method.isAnnotationPresent(RequestMapping.class)) {
            RequestMapping a = method.getAnnotation(RequestMapping.class);
            List<String> paths = pathsOf(a.value(), a.path());
            Set<RequestMethod> methods;
            if (a.method().length == 0) {
                // Без указания метода RequestMapping применяется ко всем.
                methods = EnumSet.of(
                        RequestMethod.GET,
                        RequestMethod.POST,
                        RequestMethod.PUT,
                        RequestMethod.DELETE,
                        RequestMethod.PATCH,
                        RequestMethod.OPTIONS,
                        RequestMethod.HEAD
                );
            } else {
                methods = EnumSet.copyOf(Arrays.asList(a.method()));
            }
            return new RouteData(paths, methods);
        }
        return null;
    }

    private List<String> pathsOf(String[] value, String[] path) {
        String[] src = value.length > 0 ? value : path;
        if (src.length == 0) return List.of("");

        List<String> result = new ArrayList<>();
        for (String p : src) result.add(p == null ? "" : p);
        return result;
    }

    private String normalizePath(String classPath, String methodPath) {
        String cp = classPath == null ? "" : classPath.trim();
        String mp = methodPath == null ? "" : methodPath.trim();

        String combined;
        if (cp.isEmpty() && mp.isEmpty()) combined = "/";
        else if (cp.isEmpty()) combined = mp;
        else if (mp.isEmpty()) combined = cp;
        else combined = cp + "/" + mp;

        combined = combined.replaceAll("/+", "/");
        if (!combined.startsWith("/")) combined = "/" + combined;
        if (combined.length() > 1 && combined.endsWith("/")) combined = combined.substring(0, combined.length() - 1);
        return combined;
    }

    private record RouteData(List<String> paths, Set<RequestMethod> methods) {}
}

