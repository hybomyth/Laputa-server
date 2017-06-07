package com.laputa.server.workers;

import com.laputa.server.core.BlockingIOProcessor;
import com.laputa.server.core.dao.FileManager;
import com.laputa.server.core.dao.UserDao;
import com.laputa.server.core.dao.UserKey;
import com.laputa.server.core.model.AppName;
import com.laputa.server.core.model.auth.User;
import com.laputa.server.core.stats.GlobalStats;
import com.laputa.server.db.DBManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.mockito.Mockito.*;

/**
 * The Laputa Project.
 * Created by Sommer
 * Created on 3/3/2015.
 */
@RunWith(MockitoJUnitRunner.class)
public class ProfileSaverWorkerTest {

    @Mock
    private UserDao userDao;

    @Mock
    private FileManager fileManager;

    @Mock
    private GlobalStats stats;

    private BlockingIOProcessor blockingIOProcessor = new BlockingIOProcessor(4, 1);

    @Test
    public void testCorrectProfilesAreSaved() throws IOException {
        ProfileSaverWorker profileSaverWorker = new ProfileSaverWorker(userDao, fileManager, new DBManager(blockingIOProcessor, true));

        User user1 = new User("1", "", AppName.LAPUTA, "local", false, false);
        User user2 = new User("2", "", AppName.LAPUTA, "local", false, false);
        User user3 = new User("3", "", AppName.LAPUTA, "local", false, false);
        User user4 = new User("4", "", AppName.LAPUTA, "local", false, false);

        ConcurrentMap<UserKey, User> userMap = new ConcurrentHashMap<>();
        userMap.put(new UserKey(user1), user1);
        userMap.put(new UserKey(user2), user2);
        userMap.put(new UserKey(user3), user3);
        userMap.put(new UserKey(user4), user4);

        when(userDao.getUsers()).thenReturn(userMap);
        profileSaverWorker.run();

        verify(fileManager, times(4)).overrideUserFile(any());
        verify(fileManager).overrideUserFile(user1);
        verify(fileManager).overrideUserFile(user2);
        verify(fileManager).overrideUserFile(user3);
        verify(fileManager).overrideUserFile(user4);
    }

    @Test
    public void testNoProfileChanges() throws Exception {
        User user1 = new User("1", "", AppName.LAPUTA, "local", false, false);
        User user2 = new User("2", "", AppName.LAPUTA, "local", false, false);
        User user3 = new User("3", "", AppName.LAPUTA, "local", false, false);
        User user4 = new User("4", "", AppName.LAPUTA, "local", false, false);

        Map<UserKey, User> userMap = new HashMap<>();
        userMap.put(new UserKey("1", AppName.LAPUTA), user1);
        userMap.put(new UserKey("2", AppName.LAPUTA), user2);
        userMap.put(new UserKey("3", AppName.LAPUTA), user3);
        userMap.put(new UserKey("4", AppName.LAPUTA), user4);

        Thread.sleep(1);

        ProfileSaverWorker profileSaverWorker = new ProfileSaverWorker(userDao, fileManager, new DBManager(blockingIOProcessor, true));

        when(userDao.getUsers()).thenReturn(userMap);
        profileSaverWorker.run();

        verifyNoMoreInteractions(fileManager);
    }

}
