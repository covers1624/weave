package net.fabricmc.weave;

import net.fabricmc.weave.merge.JarMerger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class CommandMergeMinecraft extends Command {
    public CommandMergeMinecraft() {
        super("mergeMinecraft");
    }

    @Override
    public String getHelpString() {
        return "<client-jar> <server-jar> <output>";
    }

    @Override
    public boolean isArgumentCountValid(int count) {
        return count == 3;
    }

    @Override
    public void run(String[] args) throws Exception {
        File in1f = new File(args[0]);
        File in2f = new File(args[1]);
        File outf = new File(args[2]);

        if (!in1f.exists() || !in1f.isFile()) {
            throw new FileNotFoundException("Client JAR could not be found!");
        }

        if (!in2f.exists() || !in2f.isFile()) {
            throw new FileNotFoundException("Server JAR could not be found!");
        }

        try {
            FileInputStream in1fs = new FileInputStream(in1f);
            FileInputStream in2fs = new FileInputStream(in2f);
            FileOutputStream outfs = new FileOutputStream(outf);

            JarMerger merger = new JarMerger(in1fs, in2fs, outfs);

            System.out.println("Merging...");

            merger.merge();
            merger.close();

            in1fs.close();
            in2fs.close();
            outfs.close();

            System.out.println("Merge completed!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
