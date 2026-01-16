package com.github.lizhiwei88.feign.mock.plugin.generator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mock data generator
 *
 * @author lizhiwei
 * @version 1.0
 * @since 2026/01/16
 */
public class MockDataGenerator {

    private static final int MAX_DEPTH = 3;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

    private MockDataGenerator() {
    }

    public static String generateJson(@NotNull PsiType type) {
        Object data = generateData(type, 0);
        return GSON.toJson(data);
    }

    private static Object generateData(@Nullable PsiType type, int depth) {
        if (type == null) return null;
        if (depth > MAX_DEPTH) return null;

        // 1. Array
        if (type instanceof PsiArrayType psiArrayType) {
            PsiType componentType = psiArrayType.getComponentType();
            return Collections.singletonList(generateData(componentType, depth + 1));
        }

        // 2. Primitives and Wrappers
        if (isBoolean(type)) return true;
        if (isNumeric(type)) return 0;
        if (type.equalsToText("java.lang.String")) return "string";
        if (type.equalsToText("java.lang.Character")) return "a";
        if (type.equalsToText("java.util.Date") || type.equalsToText("java.time.LocalDateTime")
                || type.equalsToText("java.time.LocalDate")) {
            return "2026-01-01 12:00:00";
        }
        if (type.equalsToText("java.math.BigDecimal")) return new BigDecimal("0.00");

        // 3. Resolve Class
        PsiClass psiClass = PsiUtil.resolveClassInType(type);
        if (psiClass == null) return new Object();

        String qName = psiClass.getQualifiedName();
        if (qName != null) {
            // Collection
            if (isCollection(qName)) {
                PsiType elementType = getCollectionElementType(type);
                return Collections.singletonList(generateData(elementType, depth + 1));
            }
            // Map
            if (isMap(qName)) {
                PsiType valueType = getMapValueType(type);
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("key", generateData(valueType, depth + 1));
                return map;
            }
            // Enum
            if (psiClass.isEnum()) {
                for (PsiField field : psiClass.getFields()) {
                    if (field instanceof PsiEnumConstant) {
                        return field.getName();
                    }
                }
                return "";
            }
            // System classes
            if (isJavaSystemClass(qName)) {
                return new Object();
            }
        }

        // 4. Custom Object / POJO
        Map<String, Object> objectMap = new LinkedHashMap<>();
        PsiField[] fields = psiClass.getAllFields();
        for (PsiField field : fields) {
            // Check for static or transient modifiers
            if (!field.hasModifierProperty(PsiModifier.STATIC) && !field.hasModifierProperty(PsiModifier.TRANSIENT)) {
                PsiType fieldType = field.getType();
                // Resolve generics
                if (type instanceof PsiClassType psiClassType) {
                    PsiSubstitutor substitutor = psiClassType.resolveGenerics().getSubstitutor();
                    fieldType = substitutor.substitute(fieldType);
                }

                // Simple recursion check
                if (fieldType == null || !fieldType.equals(type) || depth <= 0) {
                    objectMap.put(field.getName(), generateData(fieldType, depth + 1));
                }
            }
        }

        return objectMap;
    }

    private static boolean isCollection(String qName) {
        return qName.startsWith("java.util.List") || qName.startsWith("java.util.Set") || qName.startsWith("java.util.Collection") ||
                qName.startsWith("java.util.ArrayList") || qName.startsWith("java.util.LinkedList");
    }

    private static boolean isMap(String qName) {
         return qName.startsWith("java.util.Map") || qName.startsWith("java.util.HashMap") || qName.startsWith("java.util.LinkedHashMap");
    }

    private static PsiType getCollectionElementType(PsiType type) {
        if (type instanceof PsiClassType psiClassType) {
            PsiType[] parameters = psiClassType.getParameters();
            if (parameters.length > 0) return parameters[0];
        }
        return null;
    }

    private static PsiType getMapValueType(PsiType type) {
        if (type instanceof PsiClassType psiClassType) {
            PsiType[] parameters = psiClassType.getParameters();
            if (parameters.length > 1) return parameters[1];
        }
        return null;
    }

    private static boolean isBoolean(PsiType type) {
        return PsiTypes.booleanType().equals(type) || type.equalsToText("java.lang.Boolean");
    }

    private static boolean isNumeric(PsiType type) {
        return PsiTypes.intType().equals(type) || type.equalsToText("java.lang.Integer") ||
                PsiTypes.longType().equals(type) || type.equalsToText("java.lang.Long") ||
                PsiTypes.doubleType().equals(type) || type.equalsToText("java.lang.Double") ||
                PsiTypes.floatType().equals(type) || type.equalsToText("java.lang.Float") ||
                PsiTypes.shortType().equals(type) || type.equalsToText("java.lang.Short") ||
                PsiTypes.byteType().equals(type) || type.equalsToText("java.lang.Byte");
    }

    private static boolean isJavaSystemClass(String qName) {
        return qName.startsWith("java.") || qName.startsWith("javax.");
    }
}
