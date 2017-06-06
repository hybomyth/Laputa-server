package com.laputa.server.core.dao;

import com.laputa.server.core.model.DashBoard;
import com.laputa.server.core.model.auth.User;
import com.laputa.server.core.model.device.Device;
import com.laputa.server.core.model.widgets.Widget;
import com.laputa.utils.FileUtils;
import com.laputa.utils.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.Files.createDirectories;
import static java.util.function.Function.identity;


/**
 * Class responsible for saving/reading user data to/from disk.
 *
 * User: ddumanskiy
 * Date: 8/11/13
 * Time: 6:53 PM
 */
public class FileManager {

    private static final Logger log = LogManager.getLogger(FileManager.class);
    private static final String USER_FILE_EXTENSION = ".user";

    /**
     * Folder where all user profiles are stored locally.
     */
    private Path dataDir;

    private static final String DELETED_DATA_DIR_NAME = "deleted";
    private static final String BACKUP_DATA_DIR_NAME = "backup";
    private Path deletedDataDir;
    private Path backupDataDir;

    public FileManager(String dataFolder) {
        if (dataFolder == null || dataFolder.isEmpty() || dataFolder.equals("/path")) {
            System.out.println("WARNING : '" + dataFolder + "' does not exists. Please specify correct -dataFolder parameter.");
            dataFolder = Paths.get(System.getProperty("java.io.tmpdir"), "laputa").toString();
            System.out.println("Your data may be lost during server restart. Using temp folder : " + dataFolder);
        }
        try {
            Path dataFolderPath = Paths.get(dataFolder);
            this.dataDir = createDirectories(dataFolderPath);
            this.deletedDataDir = createDirectories(Paths.get(dataFolder, DELETED_DATA_DIR_NAME));
            this.backupDataDir = createDirectories(Paths.get(dataFolder, BACKUP_DATA_DIR_NAME));
        } catch (Exception e) {
            Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "laputa");

            System.out.println("WARNING : could not find folder '" + dataFolder + "'. Please specify correct -dataFolder parameter.");
            System.out.println("Your data may be lost during server restart. Using temp folder : " + tempDir.toString());

            try {
                this.dataDir = createDirectories(tempDir);
                this.deletedDataDir = createDirectories(Paths.get(this.dataDir.toString(), DELETED_DATA_DIR_NAME));
                this.backupDataDir = createDirectories(Paths.get(this.dataDir.toString(), BACKUP_DATA_DIR_NAME));
            } catch (Exception ioe) {
                throw new RuntimeException(ioe);
            }
        }

        log.info("Using data dir '{}'", dataDir);
    }

    public Path getDataDir() {
        return dataDir;
    }

    public Path generateFileName(String email, String appName) {
        return Paths.get(dataDir.toString(), email + "." + appName + USER_FILE_EXTENSION);
    }

    public Path generateBackupFileName(String email, String appName) {
        return Paths.get(backupDataDir.toString(), email + "." + appName + ".user." +
                new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
    }

    public Path generateOldFileName(String userName) {
        return Paths.get(dataDir.toString(), "u_" + userName + USER_FILE_EXTENSION);
    }

    public boolean delete(String email, String appName) {
        Path file = generateFileName(email, appName);
        return FileUtils.move(file, this.deletedDataDir);
    }

    public void overrideUserFile(User user) throws IOException {
        Path path = generateFileName(user.email, user.appName);

        JsonParser.writeUser(path.toFile(), user);

        removeOldFile(user.email);
    }

    private void removeOldFile(String email) {
        //this oldFileName is migration code. should be removed in future versions
        Path oldFileName = generateOldFileName(email);
        try {
            Files.deleteIfExists(oldFileName);
        } catch (Exception e) {
            log.error("Error removing old file. {}", oldFileName, e);
        }
    }

    /**
     * Loads all user profiles one by one from disk using dataDir as starting point.
     *
     * @return mapping between username and it's profile.
     */
    public ConcurrentMap<UserKey, User> deserializeUsers() {
        log.debug("Starting reading user DB.");

        final File[] files = dataDir.toFile().listFiles();

        ConcurrentMap<UserKey, User> temp;
        if (files != null) {
            temp = Arrays.stream(files).parallel()
                    .filter(file -> file.isFile() && file.getName().endsWith(USER_FILE_EXTENSION))
                    .flatMap(file -> {
                        try {
                            User user = JsonParser.parseUserFromFile(file);
                            makeProfileChanges(user);

                            return Stream.of(user);
                        } catch (IOException ioe) {
                            log.error("Error parsing file '{}'. Error : {}", file, ioe.getMessage());
                        }
                        return Stream.empty();
                    })
                    .collect(Collectors.toConcurrentMap(UserKey::new, identity()));
        } else {
            temp = new ConcurrentHashMap<>();
        }

        log.debug("Reading user DB finished.");
        return temp;
    }

    public static void makeProfileChanges(User user) {
        if (user.email == null) {
            user.email = user.name;
        }
        for (DashBoard dashBoard : user.profile.dashBoards) {
            if (dashBoard.devices != null) {
                for (Device device : dashBoard.devices) {
                    device.status = null;
                }
            }

            for (Widget widget : dashBoard.widgets) {
                dashBoard.cleanPinStorage(widget);
            }
        }
    }

    public Map<String, Integer> getUserProfilesSize() {
        Map<String, Integer> userProfileSize = new HashMap<>();
        File[] files = dataDir.toFile().listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(USER_FILE_EXTENSION)) {
                    userProfileSize.put(file.getName(), (int) file.length());
                }
            }
        }
        return userProfileSize;
    }

}
