package de.remsfal.service.boundary.project;

//import com.datastax.oss.driver.api.core.CqlSession;
import de.remsfal.core.model.project.ChatMessageModel;
import de.remsfal.core.model.project.TaskModel;
import de.remsfal.service.TestData;
import de.remsfal.service.boundary.authentication.SessionManager;
import de.remsfal.service.entity.dao.ChatMessageRepository;
import de.remsfal.service.entity.dao.ChatSessionRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@QuarkusTest
public class loadtesting extends AbstractProjectResourceTest {

    @Inject
    Logger logger;

    @Inject
    SessionManager sessionManager;

    //@Inject
    //CqlSession cqlSession;

    static final String TASK_ID_1 = "5b111b34-1073-4f48-a79d-f19b17e7d56b";
    static final String TASK_ID_2 = "4b8cd355-ad07-437a-9e71-a4e2e3624957";
    static final String EXAMPLE_CHAT_SESSION_ID_1 = UUID.randomUUID().toString();
    private volatile String cookieString;
    static final UUID PROJECT_ID_1_UUID = UUID.fromString(TestData.PROJECT_ID_1);
    static final UUID TASK_ID_1_UUID = UUID.fromString(TASK_ID_1);
    static final UUID EXAMPLE_CHAT_SESSION_ID_1_UUID = UUID.fromString(EXAMPLE_CHAT_SESSION_ID_1);
    static final UUID USER_ID_3_UUID = UUID.fromString(TestData.USER_ID_3);
    static final UUID USER_ID_4_UUID = UUID.fromString(TestData.USER_ID_4);
    static final String CHAT_MESSAGE_ID_1 = "b9854462-abb8-4213-8b15-be9290a19959";
    static final UUID CHAT_MESSAGE_ID_1_UUID = UUID.fromString(CHAT_MESSAGE_ID_1);

    @BeforeEach
    protected void setup() throws Exception {
        logger.info("Setting up test data");
        logger.info("Setting up test users and projects. User " + TestData.USER_ID + " is the manager of all projects.");
        super.setupTestUsers();
        super.setupTestProjects();
        logger.info("Setting up project memberships");
        runInTransaction(() -> entityManager
                .createNativeQuery("INSERT INTO PROJECT_MEMBERSHIP (PROJECT_ID, USER_ID, MEMBER_ROLE) VALUES (?,?,?)")
                .setParameter(1, TestData.PROJECT_ID_1)
                .setParameter(2, TestData.USER_ID_2)
                .setParameter(3, "STAFF")
                .executeUpdate());
        logger.info("User " + TestData.USER_ID_2 + " is a caretaker in project " + TestData.PROJECT_ID_1);
        runInTransaction(() -> entityManager
                .createNativeQuery("INSERT INTO PROJECT_MEMBERSHIP (PROJECT_ID, USER_ID, MEMBER_ROLE) VALUES (?,?,?)")
                .setParameter(1, TestData.PROJECT_ID_1)
                .setParameter(2, TestData.USER_ID_3)
                .setParameter(3, "LESSOR")
                .executeUpdate());
        logger.info("User " + TestData.USER_ID_3 + " is a lessor in project " + TestData.PROJECT_ID_1);
        runInTransaction(() -> entityManager
                .createNativeQuery("INSERT INTO PROJECT_MEMBERSHIP (PROJECT_ID, USER_ID, MEMBER_ROLE) VALUES (?,?,?)")
                .setParameter(1, TestData.PROJECT_ID_1)
                .setParameter(2, TestData.USER_ID_4)
                .setParameter(3, "PROPRIETOR")
                .executeUpdate());
        logger.info("User " + TestData.USER_ID_4 + " is a proprietor in project " + TestData.PROJECT_ID_1);
        runInTransaction(() -> entityManager
                .createNativeQuery("INSERT INTO TASK (ID, TYPE, PROJECT_ID, TITLE, DESCRIPTION, STATUS, CREATED_BY) VALUES (?,?,?,?,?,?,?)")
                .setParameter(1, TASK_ID_1)
                .setParameter(2, "TASK")
                .setParameter(3, TestData.PROJECT_ID_1)
                .setParameter(4, TestData.TASK_TITLE_1)
                .setParameter(5, TestData.TASK_DESCRIPTION_1)
                .setParameter(6, TaskModel.Status.OPEN.name())
                .setParameter(7, TestData.USER_ID)
                .executeUpdate());
        runInTransaction(() -> entityManager
                .createNativeQuery("INSERT INTO TASK (ID, TYPE, PROJECT_ID, TITLE, DESCRIPTION, STATUS, CREATED_BY) VALUES (?,?,?,?,?,?,?)")
                .setParameter(1, TASK_ID_2)
                .setParameter(2, "DEFECT")
                .setParameter(3, TestData.PROJECT_ID_1)
                .setParameter(4, "DEFECT TITLE")
                .setParameter(5, "DEFECT DESCRIPTION")
                .setParameter(6, TaskModel.Status.OPEN.name())
                .setParameter(7, TestData.USER_ID)
                .executeUpdate());

        logger.info("Setting up chat sessions");
        // set up example chat session for a task in project 1
        runInTransaction(() -> entityManager.createNativeQuery(
                        "INSERT INTO CHAT_SESSION (ID, PROJECT_ID, TASK_ID, TASK_TYPE, STATUS) VALUES (?,?,?,?,?)")
                .setParameter(1, EXAMPLE_CHAT_SESSION_ID_1)
                .setParameter(2, TestData.PROJECT_ID_1)
                .setParameter(3, TASK_ID_1)
                .setParameter(4, "TASK")
                .setParameter(5, "OPEN")
                .executeUpdate());

        runInTransaction(() -> entityManager
                .createNativeQuery("INSERT INTO CHAT_SESSION_PARTICIPANT (CHAT_SESSION_ID, PARTICIPANT_ID , ROLE)" +
                        " VALUES (?,?,?)")
                .setParameter(1, EXAMPLE_CHAT_SESSION_ID_1)
                .setParameter(2, TestData.USER_ID_4)
                .setParameter(3, "INITIATOR")
                .executeUpdate());
        // set user-3 as participant of the chat sessions
        runInTransaction(() -> entityManager
                .createNativeQuery("INSERT INTO CHAT_SESSION_PARTICIPANT (CHAT_SESSION_ID, PARTICIPANT_ID , ROLE) " +
                        "VALUES (?,?,?)")
                .setParameter(1, EXAMPLE_CHAT_SESSION_ID_1)
                .setParameter(2, TestData.USER_ID_3)
                .setParameter(3, "HANDLER")
                .executeUpdate());

        runInTransaction(() -> entityManager
                .createNativeQuery("INSERT INTO CHAT_MESSAGE (ID, CHAT_SESSION_ID, SENDER_ID, CONTENT_TYPE, CONTENT)"
                        +
                        " VALUES (?,?,?,?,?)")
                .setParameter(1, CHAT_MESSAGE_ID_1)
                .setParameter(2, EXAMPLE_CHAT_SESSION_ID_1)
                .setParameter(3, TestData.USER_ID_3)
                .setParameter(4, ChatMessageModel.ContentType.TEXT.name())
                .setParameter(5, "Hello World")
                .executeUpdate());

        /*
        logger.info("Setting up chat sessions and messages");
        String insertChatSessionCql = "INSERT INTO REMSFAL.chat_sessions " +
                "(project_id, task_id, session_id, task_type, status, created_at, participants) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        cqlSession.execute(insertChatSessionCql,
                PROJECT_ID_1_UUID, TASK_ID_1_UUID, EXAMPLE_CHAT_SESSION_ID_1_UUID,
                ChatSessionRepository.TaskType.TASK.name(), ChatSessionRepository.Status.OPEN.name(), Instant.now(),
                Map.of(
                        USER_ID_4_UUID, ChatSessionRepository.ParticipantRole.INITIATOR.name(),
                        USER_ID_3_UUID, ChatSessionRepository.ParticipantRole.HANDLER.name()
                ));
        logger.info("Session 1 " + EXAMPLE_CHAT_SESSION_ID_1 + " created on project " + TestData.PROJECT_ID_1);
        String insertChatMessageCql = "INSERT INTO REMSFAL.chat_messages " +
                "(chat_session_id, message_id, sender_id, content_type, content, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        cqlSession.execute(insertChatMessageCql,
                EXAMPLE_CHAT_SESSION_ID_1_UUID, CHAT_MESSAGE_ID_1_UUID, USER_ID_3_UUID,
                ChatMessageRepository.ContentType.TEXT.name(), "Hello World", Instant.now()); */
    }

    @AfterEach
    protected void cleanup() throws Exception {
        logger.info("Cleaning up test data...");
        runInTransaction(() -> {
            entityManager.createNativeQuery("DELETE FROM PROJECT_MEMBERSHIP WHERE PROJECT_ID = ?")
                    .setParameter(1, TestData.PROJECT_ID_1)
                    .executeUpdate();
            entityManager.createNativeQuery("DELETE FROM TASK WHERE PROJECT_ID = ?")
                    .setParameter(1, TestData.PROJECT_ID_1)
                    .executeUpdate();
            runInTransaction(() -> entityManager
                    .createNativeQuery("DELETE FROM CHAT_MESSAGE WHERE CHAT_SESSION_ID = ?")
                    .setParameter(1, EXAMPLE_CHAT_SESSION_ID_1)
                    .executeUpdate());
        });
        logger.info("Cleanup completed.");
    }

    private String regenerateCookie() {
        try {
            if (cookieString == null) {
                logger.warn("cookieString is null, generating a new cookie.");
                cookieString = generateInitialCookie();
            }
            String refreshToken = extractRefreshToken(cookieString);
            Map<String, jakarta.ws.rs.core.Cookie> currentCookies = Map.of(
                    SessionManager.REFRESH_COOKIE_NAME, new jakarta.ws.rs.core.Cookie(SessionManager.REFRESH_COOKIE_NAME, refreshToken)
            );
            SessionManager.TokenRenewalResponse renewalResponse = sessionManager.renewTokens(currentCookies);
            cookieString = SessionManager.ACCESS_COOKIE_NAME + "=" + renewalResponse.getAccessToken().getValue()
                    + "; " + SessionManager.REFRESH_COOKIE_NAME + "=" + renewalResponse.getRefreshToken().getValue();
            return cookieString;
        } catch (Exception e) {
            logger.error("Error regenerating cookies: " + e.getMessage(), e);
            throw new RuntimeException("Failed to regenerate cookies.", e);
        }
    }

    private String extractRefreshToken(String cookieString) {
        for (String cookie : cookieString.split(";")) {
            String[] keyValue = cookie.trim().split("=");
            if (keyValue.length == 2 && SessionManager.REFRESH_COOKIE_NAME.equals(keyValue[0])) {
                return keyValue[1];
            }
        }
        throw new RuntimeException("Refresh token not found in cookie string.");
    }

    private String generateInitialCookie() {
        Duration ttl = Duration.ofMinutes(5);
        Map<String, ?> cookies = buildCookies(TestData.USER_ID, TestData.USER_EMAIL, ttl);
        return SessionManager.ACCESS_COOKIE_NAME + "=" + cookies.get(SessionManager.ACCESS_COOKIE_NAME)
                + "; " + SessionManager.REFRESH_COOKIE_NAME + "=" + cookies.get(SessionManager.REFRESH_COOKIE_NAME);
    }

    @Test
    public void testLoadWithK6AndPrometheusRemoteWrite() throws Exception {
        // Verify InfluxDB connectivity
        try {
            URL influxUrl = new URL("http://127.0.0.1:8091/ping");
            HttpURLConnection connection = (HttpURLConnection) influxUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            if (connection.getResponseCode() != 204) {
                throw new RuntimeException("Unable to connect to InfluxDB at http://127.0.0.1:8091");
            }
            logger.info("Verified InfluxDB is reachable.");
        } catch (Exception e) {
            logger.error("Error verifying InfluxDB connection", e);
            throw e;
        }

        // Initialize the cookie
        cookieString = regenerateCookie();

        // Path to your k6 scenario script
        String k6ScriptPath = "/home/parham/load-testing/load-testing/chat-requests.js";
        File k6ScriptFile = new File(k6ScriptPath);
        if (!k6ScriptFile.exists()) {
            throw new RuntimeException("K6 script file not found: " + k6ScriptPath);
        }

        ProcessBuilder k6Pb = new ProcessBuilder(
                "k6",
                "run",
                "--out", "json=./k6_results_" + System.currentTimeMillis() + ".json",
                "--out", "influxdb=http://127.0.0.1:8091/k6",
                "--out", "web-dashboard",
                k6ScriptPath
        );

        // Set the environment variables for k6
        Map<String, String> env = k6Pb.environment();
        env.put("BASE_URL", "http://localhost:8081");
        env.put("PROJECT_ID", TestData.PROJECT_ID_1);
        env.put("TASK_ID", TASK_ID_1);
        env.put("SESSION_ID", EXAMPLE_CHAT_SESSION_ID_1);
        env.put("TEST_COOKIE", cookieString);
        env.put("CHAT_MESSAGE_ID", CHAT_MESSAGE_ID_1);

        // Start a thread to refresh cookies periodically
        new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(3 * 60 * 1000); // Refresh every 3 minutes
                    cookieString = regenerateCookie();
                    env.put("TEST_COOKIE", cookieString);
                    logger.info("Refreshed TEST_COOKIE.");
                }
            } catch (InterruptedException e) {
                logger.error("Cookie refresh thread interrupted.", e);
            } catch (Exception e) {
                logger.error("Error in cookie refresh thread", e);
            }
        }).start();

        try {
            logger.info("Starting K6 process...");
            Process k6Process = k6Pb.start();

            try (BufferedReader k6OutReader = new BufferedReader(new InputStreamReader(k6Process.getInputStream()));
                 BufferedReader k6ErrReader = new BufferedReader(new InputStreamReader(k6Process.getErrorStream()))) {
                String line;
                while ((line = k6OutReader.readLine()) != null) {
                    System.out.println("[k6 OUT] " + line);
                }
                while ((line = k6ErrReader.readLine()) != null) {
                    System.err.println("[k6 ERR] " + line);
                }
            }

            int k6ExitCode = k6Process.waitFor();
            logger.info("k6 finished with exitCode=" + k6ExitCode);
            // Log nonzero exit code but do not throw an exception.
            if (k6ExitCode != 0) {
                logger.error("K6 test finished with non-zero exit code: " + k6ExitCode + ". Continuing test execution.");
            }
        } catch (Exception e) {
            logger.error("Error while running K6 process", e);
        }
    }
}
