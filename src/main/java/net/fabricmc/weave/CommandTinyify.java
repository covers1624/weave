package net.fabricmc.weave;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import cuchaz.enigma.analysis.JarIndex;
import cuchaz.enigma.mapping.ClassMapping;
import cuchaz.enigma.mapping.FieldMapping;
import cuchaz.enigma.mapping.Mappings;
import cuchaz.enigma.mapping.MappingsEnigmaReader;
import cuchaz.enigma.mapping.MethodMapping;
import net.fabricmc.weave.util.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.util.jar.JarFile;

public class CommandTinyify extends Command {
    private JarIndex index;

    public CommandTinyify() {
        super("tinyify");
    }

    @Override
    public String getHelpString() {
        return "<input-jar> <enigma-mappings> <output-tiny> [name-obf] [name-deobf]";
    }

    @Override
    public boolean isArgumentCountValid(int count) {
        return count >= 3 && count <= 5;
    }

    private void write(Writer writer, String[] data) throws IOException {
        writer.write(Utils.TAB_JOINER.join(data) + "\n");
    }

    private void writeData(Writer writer, ClassMapping mapping) throws IOException {
        for (FieldMapping fieldMapping : mapping.fields()) {
            if (fieldMapping.getDeobfName() != null
                    && !fieldMapping.getObfName().equals(fieldMapping.getDeobfName())) {
                String[] data = Utils.serializeEntry(fieldMapping.getObfEntry(mapping.getObfEntry()), true, fieldMapping.getDeobfName());
                write(writer, data);
            }
        }

        for (MethodMapping methodMapping : mapping.methods()) {
            if (methodMapping.getDeobfName() != null
                    && !methodMapping.getObfName().equals(methodMapping.getDeobfName())
                    && Utils.isBehaviorProvider(index, mapping.getObfEntry(), methodMapping.getObfEntry(mapping.getObfEntry()))) {
                String[] data = Utils.serializeEntry(methodMapping.getObfEntry(mapping.getObfEntry()), true, methodMapping.getDeobfName());
                write(writer, data);
            }
        }
    }

    private void writeClass(Writer writer, String prefix, ClassMapping mapping) throws IOException {
        String obfClassName = Utils.NONE_PREFIX_REMOVER.map(mapping.getObfFullName());
        String deobfClassName = mapping.getDeobfName() == null ? obfClassName : Utils.NONE_PREFIX_REMOVER.map(prefix + (mapping.getDeobfName()));
        if (!deobfClassName.equals(obfClassName)) {
            write(writer, new String[]{"CLASS", obfClassName, deobfClassName});
        }

        writeData(writer, mapping);

        for (ClassMapping innerMapping : mapping.innerClasses()) {
            writeClass(writer, prefix + deobfClassName + "$", innerMapping);
        }
    }

    @Override
    public void run(String[] args) throws Exception {
        File injf = new File(args[0]);
        File inf = new File(args[1]);
        File outf = new File(args[2]);
        String nameObf = args.length > 3 ? args[3] : "mojang";
        String nameDeobf = args.length > 4 ? args[4] : "pomf";

        if (!injf.exists() || !injf.isFile()) {
            throw new FileNotFoundException("Input JAR could not be found!");
        }

        if (!inf.exists()) {
            throw new FileNotFoundException("Enigma mappings could not be found!");
        }

        System.out.println("Reading JAR file...");
        index = new JarIndex();
        index.indexJar(new JarFile(injf), true);

        System.out.println("Reading Enigma mappings...");
        Mappings mappings = (new MappingsEnigmaReader()).read(inf);

        System.out.println("Writing Tiny mappings...");
        Writer writer = Files.newWriter(outf, Charsets.UTF_8);
        write(writer, new String[]{"v1", nameObf, nameDeobf});
        for (String s : mappings.getAllObfClassNames()) {
            ClassMapping mapping = mappings.getClassByObf(s);
            if (mapping != null) {
                writeClass(writer, "", mapping);
            }
        }
        writer.close();
    }
}
