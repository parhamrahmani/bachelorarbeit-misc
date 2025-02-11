import http from 'k6/http';
import { check, sleep} from 'k6';

const BASE_URL     = __ENV.BASE_URL     || 'http://localhost:8081';
const PROJECT_ID   = __ENV.PROJECT_ID   || 'some-project-id';
const TASK_ID      = __ENV.TASK_ID      || 'some-task-id';
const SESSION_ID   = __ENV.SESSION_ID   || 'some-session-id';
const COOKIE_VALUE = __ENV.TEST_COOKIE  || '';
const CHAT_MESSAGE_ID = __ENV.CHAT_MESSAGE_ID || 'b9854462-abb8-4213-8b15-be9290a19959'; 


export function createChatSession() {
    const url = `${BASE_URL}/api/v1/projects/${PROJECT_ID}/tasks/${TASK_ID}/chats`;
    const params = {
        headers: {
            'Cookie': COOKIE_VALUE,
            'Content-Type': 'application/json',
        },
    };
    let res = http.post(url, null, params);
    check(res, {
        'createChatSession => 201': (r) => r.status === 201,
    });
    sleep(1);
}

export function getChatSession(){
    const url = `${BASE_URL}/api/v1/projects/${PROJECT_ID}/tasks/${TASK_ID}/chats/${SESSION_ID}`;
    const params = {
        headers:
        {
            'Cookie': COOKIE_VALUE,
            'Content-Type': 'application/json',
        }
    };
    let res = http.get(url, params);
    check(res, {
        'getChatSession => 200': (r) => r.status === 200,
    });
    sleep(1);
}

export function sendTextMessage() {
    const url = `${BASE_URL}/api/v1/projects/${PROJECT_ID}/tasks/${TASK_ID}/chats/${SESSION_ID}/messages`;
    const payload = JSON.stringify({
        chat_session_id: SESSION_ID,
        sender_id: 'k6-sender-id',
        contentType: 'TEXT',
        content: 'Hello from k6 test!',
    });
    const params = {
        headers: {
            'Cookie': COOKIE_VALUE,
            'Content-Type': 'application/json',
        }
    };
    let res = http.post(url, payload, params);
    check(res, {
        'sendTextMessage => 201': (r) => r.status === 201,
    });
    sleep(1);
}

export function getChatMessage() {
    const url =  `${BASE_URL}/api/v1/projects/${PROJECT_ID}/tasks/${TASK_ID}/chats/${SESSION_ID}/messages/${CHAT_MESSAGE_ID}`;
    const params = {
        headers: {
            'Cookie': COOKIE_VALUE,
            'Content-Type': 'application/json',
        },
    };
    let res = http.get(url, params);
    check(res, {
        'getTextMessage => 200': (r) => r.status === 200,
    });
    sleep(1);
}

export function uploadFileMessage() {
    const url = `${BASE_URL}/api/v1/projects/${PROJECT_ID}/tasks/${TASK_ID}/chats/${SESSION_ID}/messages/upload`;

    // Generate 1 MB of dummy text (1,024 * 1024 = 1 MB).
    const oneMB = new Array(1024 * 1024).fill('A').join('');
    let fileData = http.file(
        oneMB,        // 1 MB of "A"
        'large-file.txt', // file name
        'text/plain'  // MIME type
    );

    let params = {
        headers: {
            'Cookie': COOKIE_VALUE,
        },
    };

    let formData = { file: fileData };
    let res = http.post(url, formData, params);

    check(res, {
        'uploadFileMessage => 201': (r) => r.status === 201,
    });

    sleep(1);
}
export let options = {
    scenarios: {
        ramping_test: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '10m', target: 1000 }, 
                { duration: '10m',   target: 2000 }, 
                { duration: '10m',   target: 2000 },
                { duration: '10m', target: 0 } 
            ],
            gracefulRampDown : '10s' ,
        },
    },
};

export default function () {
    try {
        createChatSession();
        for (let i = 0; i < 10; i++) {
            sendTextMessage();
        }
        getChatMessage();
        getChatSession();
    } catch (e) {
        console.error('Error in default function iteration:', e);
    }
    sleep(1);
}

