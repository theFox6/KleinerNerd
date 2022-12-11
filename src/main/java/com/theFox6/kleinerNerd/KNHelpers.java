package com.theFox6.kleinerNerd;

import com.theFox6.kleinerNerd.commands.OptionNotFoundException;
import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Random;
import java.util.Timer;

public class KNHelpers {
    private static class CopyVisitor implements FileVisitor<Path> {
        private final Path source;
        private final Path target;

        public CopyVisitor(Path sourceFolder, Path targetFolder) {
            this.source = sourceFolder;
            this.target = targetFolder;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            if (dir.endsWith(".."))
                return FileVisitResult.SKIP_SUBTREE;
            if (Files.exists(target.resolve(dir))) {
               QueuedLog.error("trying to enter already existing directory " + dir);
            }
            Files.createDirectories(target.resolve(dir));
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (Files.exists(target.resolve(source.relativize(file)))) {
                QueuedLog.error("trying to copy already existing file " + source.relativize(file));
            }
            Files.copy(file, target.resolve(source.relativize(file)));
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            QueuedLog.error("Failure while trying to copy " + source + " to " + target, exc);
            return FileVisitResult.TERMINATE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
            return FileVisitResult.CONTINUE;
        }
    }

    public static final Timer timer = new Timer("KleinerNerd timer", true);

	public static Random rng = new Random(System.nanoTime()^System.currentTimeMillis());

	public static <T> T randomElement(List<T> l) {
		return l.get(rng.nextInt(l.size()));
	}

	public static <T> T randomElement(T[] a) {
		return a[rng.nextInt(a.length)];
	}

    /**
     * extracts an Option and throws an exception when it is not found
     * ensures Nonnull result
     * @param ev the SlashCommandEvent to get the Option from
     * @param optionName the name/id of the Option to get
     * @return the OptionMapping for that option
     * @throws OptionNotFoundException if the option was null
     */
    @Nonnull
    public static OptionMapping getOptionMapping(SlashCommandInteractionEvent ev, String optionName) throws OptionNotFoundException {
        OptionMapping om = ev.getOption(optionName);
        if (om == null)
            throw new OptionNotFoundException(ev.getFullCommandName(), optionName);
        return om;
    }

    public static void copyFolder(Path source, Path destination) throws IOException {
        if (!Files.exists(destination))
            Files.createDirectories(destination);
        Files.walkFileTree(source, new CopyVisitor(source, destination));
    }
}
