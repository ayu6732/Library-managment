package com.library.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class ReflectionUtils {

    public static void printObjectFields(Object obj) {
        if (obj == null) { System.out.println("✗ Cannot inspect null object."); return; }
        Class<?> clazz = obj.getClass();
        System.out.println("\n=== REFLECTION — " + clazz.getSimpleName() + " ===");
        System.out.println("Full class name : " + clazz.getName());
        System.out.println("Superclass      : " + (clazz.getSuperclass() != null ? clazz.getSuperclass().getSimpleName() : "none"));
        Class<?>[] interfaces = clazz.getInterfaces();
        if (interfaces.length > 0) {
            System.out.print("Implements      : ");
            for (int i = 0; i < interfaces.length; i++) {
                System.out.print(interfaces[i].getSimpleName());
                if (i < interfaces.length - 1) System.out.print(", ");
            }
            System.out.println();
        }
        System.out.println("\n--- Fields ---");
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            Field[] fields = current.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                try {
                    Object value = field.get(obj);
                    System.out.printf("  [%-10s] %-20s = %s%n",
                            field.getType().getSimpleName(), field.getName(),
                            value != null ? value.toString() : "null");
                } catch (IllegalAccessException e) {
                    System.out.printf("  [%-10s] %-20s = <inaccessible>%n",
                            field.getType().getSimpleName(), field.getName());
                }
            }
            current = current.getSuperclass();
        }
        System.out.println("\n--- Public Methods ---");
        Method[] methods = clazz.getMethods();
        int count = 0;
        for (Method method : methods) {
            if (method.getDeclaringClass() == Object.class) continue;
            System.out.println("  " + Modifier.toString(method.getModifiers())
                    + " " + method.getReturnType().getSimpleName() + " " + method.getName() + "()");
            count++;
        }
        if (count == 0) System.out.println("  (none)");
        System.out.println("==========================================");
    }

    public static void printClassInfo(Class<?> clazz) {
        if (clazz == null) { System.out.println("✗ Cannot inspect null class."); return; }
        System.out.println("\n=== CLASS INFO — " + clazz.getSimpleName() + " ===");
        System.out.println("Package     : " + clazz.getPackageName());
        System.out.println("Superclass  : " + (clazz.getSuperclass() != null ? clazz.getSuperclass().getSimpleName() : "none"));
        System.out.println("Fields      : " + clazz.getDeclaredFields().length);
        System.out.println("Methods     : " + clazz.getDeclaredMethods().length);
        System.out.println("Is Abstract : " + Modifier.isAbstract(clazz.getModifiers()));
        System.out.println("Is Interface: " + clazz.isInterface());
    }

    public static <T> void inspectGeneric(T obj) {
        if (obj == null) return;
        System.out.println("\n[Generic Inspection] Runtime type of <T>: " + obj.getClass().getSimpleName());
        printObjectFields(obj);
    }
}