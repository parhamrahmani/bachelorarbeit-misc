import { sleep } from 'k6';
import { createChatSession, sendTextMessage, getChatMessage, getChatSession } from './chat-requests.js';

export let options = {
    scenarios: {
        ramping_test: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '2m', target: 100 },
                { duration: '3m', target: 300 }, 
                { duration: '5m', target: 500 },   
                { duration: '7m', target: 700 },
		{duration: '8m',  target: 900 },
		{duration: '5m',  target:1000 },
		{duration: '8m',  target: 900 },
		{ duration: '7m', target: 700 },
                { duration: '5m', target: 500 },
                { duration: '3m', target: 300 },
                { duration: '2m', target: 100 },   
            ],
        },
    },
};

export default function () {
    createChatSession();
    for (let i = 0; i < 10; i++) { // Corrected syntax
        sendTextMessage();
    }
    getChatMessage();
    getChatSession();
    sleep(2);
}
