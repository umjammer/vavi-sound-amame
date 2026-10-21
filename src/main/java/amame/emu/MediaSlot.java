/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package amame.emu;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;


/**
 * A place for a medium, a floppy drive or a cartridge slot, mame's image devices.
 * <p>
 * The calls are made on the emulation thread, {@link Machine#post} them from elsewhere.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-21 nsano initial version <br>
 */
public interface MediaSlot {

    /** "Floppy", "Cartridge" */
    String getName();

    /** the file extensions it takes, without dots, the first one for a new medium */
    List<String> getExtensions();

    /** the file of the medium in it, null when empty */
    Path getPath();

    default boolean isLoaded() {
        return getPath() != null;
    }

    /** puts the medium of a file in, what was in comes out first */
    void load(Path path) throws IOException;

    /** puts a new blank medium in, it is written to that file when it comes out */
    void create(Path path) throws IOException;

    /** takes the medium out, a writable one is written back to its file */
    void unload() throws IOException;
}
