package no.spk.tidsserie.batch.it;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Denne klassen inneholder logikk for å opprette og slette en midlertidig mappe.
 * Logikken er fisket ut av Junit4, og grunnen til at vi trenger den er at vi tidligere
 * brukte TemporaryFolder regelen til junit4, men så styrte vi livssyklussen manuelt
 * ved å kalle direkte på metodene. Dette må vi gjøre siden denne blir brukt i en cucumber-context
 * Siden oppførselen rundt opprettelse av midlertidige mapper er endret i junit5 ble det enklest
 * å trekke ut den logikken vi trengte
 */
public class TemporaryFolder {
    private final File parentFolder;
    private File folder;

    private static final String TMP_PREFIX = "junit";

    /**
     * Create a temporary folder which uses system default temporary-file
     * directory to create temporary resources.
     */
    public TemporaryFolder() {
        this(null);
    }

    /**
     * Create a temporary folder which uses the specified directory to create
     * temporary resources.
     *
     * @param parentFolder folder where temporary resources will be created.
     *                     If {@code null} then system default temporary-file directory is used.
     */
    public TemporaryFolder(final File parentFolder) {
        this.parentFolder = parentFolder;
    }

    /**
     * for testing purposes only. Do not use.
     */
    public void create() throws IOException {
        folder = createTemporaryFolderIn(parentFolder);
    }

    /**
     * Returns a new fresh folder with a random name under the temporary folder.
     */
    public File newFolder() throws IOException {
        return createTemporaryFolderIn(getRoot());
    }

    private static File createTemporaryFolderIn(File parentFolder) throws IOException {
        try {
            return createTemporaryFolderWithNioApi(parentFolder);
        } catch (final InvocationTargetException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new IOException("Failed to create temporary folder in " + parentFolder);
        } catch (final Exception e) {
            throw new RuntimeException("Failed to create temporary folder in " + parentFolder, e);
        }
    }

    private static File createTemporaryFolderWithNioApi(File parentFolder) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        final Class<?> filesClass = Class.forName("java.nio.file.Files");
        final Object fileAttributeArray = Array.newInstance(Class.forName("java.nio.file.attribute.FileAttribute"), 0);
        final Class<?> pathClass = Class.forName("java.nio.file.Path");
        final Object tempDir;
        if (parentFolder != null) {
            final Method createTempDirectoryMethod = filesClass.getDeclaredMethod("createTempDirectory", pathClass, String.class, fileAttributeArray.getClass());
            final Object parentPath = File.class.getDeclaredMethod("toPath").invoke(parentFolder);
            tempDir = createTempDirectoryMethod.invoke(null, parentPath, TMP_PREFIX, fileAttributeArray);
        } else {
            final Method createTempDirectoryMethod = filesClass.getDeclaredMethod("createTempDirectory", String.class, fileAttributeArray.getClass());
            tempDir = createTempDirectoryMethod.invoke(null, TMP_PREFIX, fileAttributeArray);
        }
        return (File) pathClass.getDeclaredMethod("toFile").invoke(tempDir);
    }

    /**
     * @return the location of this temporary folder.
     */
    public File getRoot() {
        if (folder == null) {
            throw new IllegalStateException(
                    "the temporary folder has not yet been created");
        }
        return folder;
    }

    public void delete() {
        if (!tryDelete()) {
            throw new RuntimeException("Unable to clean up temporary folder " + folder);
        }
    }

    /**
     * Tries to delete all files and folders under the temporary folder and
     * returns whether deletion was successful or not.
     *
     * @return {@code true} if all resources are deleted successfully,
     * {@code false} otherwise.
     */
    private boolean tryDelete() {
        if (folder == null) {
            return true;
        }

        return recursiveDelete(folder);
    }

    private boolean recursiveDelete(File file) {
        // Try deleting file before assuming file is a directory
        // to prevent following symbolic links.
        if (file.delete()) {
            return true;
        }
        File[] files = file.listFiles();
        if (files != null) {
            for (File each : files) {
                if (!recursiveDelete(each)) {
                    return false;
                }
            }
        }
        return file.delete();
    }
}
