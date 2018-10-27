package net.fabricmc.weave.util;

import cuchaz.enigma.analysis.JarIndex;
import cuchaz.enigma.mapping.MethodDescriptor;
import cuchaz.enigma.mapping.TypeDescriptor;
import cuchaz.enigma.mapping.entry.ClassEntry;
import cuchaz.enigma.mapping.entry.Entry;
import cuchaz.enigma.mapping.entry.FieldEntry;
import cuchaz.enigma.mapping.entry.MethodEntry;

import java.util.HashSet;
import java.util.Set;

public class EnigmaUtils {
    private static void addAllPotentialAncestors(JarIndex jarIndex, Set<ClassEntry> classEntries, ClassEntry classObfEntry) {

        for (ClassEntry interfaceEntry : jarIndex.getTranslationIndex().getInterfaces(classObfEntry)) {
            if (classEntries.add(interfaceEntry)) {
                addAllPotentialAncestors(jarIndex, classEntries, interfaceEntry);
            }
        }

        ClassEntry superClassEntry1 = jarIndex.getTranslationIndex().getSuperclass(classObfEntry);
        if (superClassEntry1 != null && classEntries.add(superClassEntry1)) {
            addAllPotentialAncestors(jarIndex, classEntries, superClassEntry1);
        }

    }

    public static boolean isMethodProvider(JarIndex jarIndex, ClassEntry classObfEntry, MethodEntry methodEntry) {
        Set<ClassEntry> classEntries = new HashSet<>();
        addAllPotentialAncestors(jarIndex, classEntries, classObfEntry);

        for (ClassEntry parentEntry : classEntries) {
            MethodEntry ancestorMethodEntry = new MethodEntry(parentEntry, methodEntry.getName(), methodEntry.getDesc());
            if (jarIndex.containsObfMethod(ancestorMethodEntry)) {
                return false;
            }
        }

        return true;
    }

    public static String[] serializeEntry(Entry entry, boolean removeNone, String... extraFields) {
        String[] data = null;

        if (entry instanceof FieldEntry) {
            data = new String[4 + extraFields.length];
            data[0] = "FIELD";
            data[1] = entry.getClassName();
            data[2] = ((FieldEntry) entry).getDesc().toString();
            data[3] = entry.getName();

            if (removeNone) {
                data[1] = Utils.NONE_PREFIX_REMOVER.map(data[1]);
                data[2] = Utils.NONE_PREFIX_REMOVER.mapDesc(data[2]);
            }
        } else if (entry instanceof MethodEntry) {
            data = new String[4 + extraFields.length];
            data[0] = "METHOD";
            data[1] = entry.getClassName();
            data[2] = ((MethodEntry) entry).getDesc().toString();
            data[3] = entry.getName();

            if (removeNone) {
                data[1] = Utils.NONE_PREFIX_REMOVER.map(data[1]);
                data[2] = Utils.NONE_PREFIX_REMOVER.mapMethodDesc(data[2]);
            }
        } else if (entry instanceof ClassEntry) {
            data = new String[2 + extraFields.length];
            data[0] = "CLASS";
            data[1] = entry.getClassName();

            if (removeNone) {
                data[1] = Utils.NONE_PREFIX_REMOVER.map(data[1]);
            }
        }

        if (data != null) {
            System.arraycopy(extraFields, 0, data, data.length - extraFields.length, extraFields.length);
        }

        return data;
    }

    public static Entry deserializeEntry(String[] data) {
        if (data.length > 0) {
            if (data[0].equals("FIELD") && data.length >= 4) {
                return new FieldEntry(new ClassEntry(data[1]), data[3], new TypeDescriptor(data[2]));
            } else if (data[0].equals("METHOD") && data.length >= 4) {
                return new MethodEntry(new ClassEntry(data[1]), data[3], new MethodDescriptor(data[2]));
            } else if (data[0].equals("CLASS") && data.length >= 2) {
                return new ClassEntry(data[1]);
            }
        }

        return null;
    }
}
