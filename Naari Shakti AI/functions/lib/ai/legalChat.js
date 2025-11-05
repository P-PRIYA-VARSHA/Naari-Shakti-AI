"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || function (mod) {
    if (mod && mod.__esModule) return mod;
    var result = {};
    if (mod != null) for (var k in mod) if (k !== "default" && Object.prototype.hasOwnProperty.call(mod, k)) __createBinding(result, mod, k);
    __setModuleDefault(result, mod);
    return result;
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.legalChat = void 0;
const functions = __importStar(require("firebase-functions"));
const admin = __importStar(require("firebase-admin"));
const generative_ai_1 = require("@google/generative-ai");
// Ensure Admin is initialized in index.ts, but keep a guard for safety when testing this module alone
if (!admin.apps.length) {
    admin.initializeApp();
}
/**
 * Callable HTTPS function: legalChat
 * - Requires Firebase Auth
 * - Reads prompt (required) and optional conversation history
 * - Calls Gemini securely using Secret Manager (GEMINI_API_KEY)
 */
exports.legalChat = functions.https.onCall(async (data, context) => {
    var _a, _b, _c, _d, _e;
    try {
        // Require auth
        if (!context.auth) {
            throw new functions.https.HttpsError('unauthenticated', 'Authentication required');
        }
        const uid = context.auth.uid;
        const prompt = data === null || data === void 0 ? void 0 : data.prompt;
        const modelName = (data === null || data === void 0 ? void 0 : data.modelName) || 'gemini-1.5-pro'; // or gemini-1.5-flash for speed
        const temperature = typeof (data === null || data === void 0 ? void 0 : data.temperature) === 'number' ? data.temperature : 0.4;
        const maxOutputTokens = typeof (data === null || data === void 0 ? void 0 : data.maxOutputTokens) === 'number' ? data.maxOutputTokens : 1024;
        if (!prompt || typeof prompt !== 'string') {
            throw new functions.https.HttpsError('invalid-argument', 'Field "prompt" (string) is required.');
        }
        // Basic size guard
        const truncatedPrompt = prompt.length > 6000 ? prompt.slice(0, 6000) + '…' : prompt;
        // Optional: system framing to ensure safe, helpful, non-legal-advice language
        const systemInstruction = 'You are a helpful legal information assistant for India. Provide concise, educational information with references to acts/sections when possible. Do not provide legal advice. Encourage consulting a licensed advocate for case-specific guidance.';
        // Prefer Firebase Functions Config on Spark plan; fallback to env var if set in emulator/CI
        const apiKey = ((_b = (_a = functions.config()) === null || _a === void 0 ? void 0 : _a.gemini) === null || _b === void 0 ? void 0 : _b.key) || process.env.GEMINI_API_KEY;
        if (!apiKey) {
            throw new functions.https.HttpsError('failed-precondition', 'Gemini API key is not configured. Set with: firebase functions:config:set gemini.key="YOUR_KEY"');
        }
        const genAI = new generative_ai_1.GoogleGenerativeAI(apiKey);
        const model = genAI.getGenerativeModel({ model: modelName, systemInstruction });
        // Optional: conversation history (array of {role: 'user'|'model', parts: string})
        const history = Array.isArray(data === null || data === void 0 ? void 0 : data.history) ? data.history : [];
        const result = await model.generateContent({
            contents: [
                ...history,
                { role: 'user', parts: [{ text: truncatedPrompt }] },
            ],
            generationConfig: {
                temperature,
                maxOutputTokens,
            },
        });
        const response = (_e = (_d = (_c = result.response) === null || _c === void 0 ? void 0 : _c.text) === null || _d === void 0 ? void 0 : _d.call(_c)) !== null && _e !== void 0 ? _e : '';
        // Log minimal analytics (do not log prompt or PII)
        await admin.firestore().collection('aiUsageLogs').add({
            uid,
            modelName,
            ts: admin.firestore.FieldValue.serverTimestamp(),
        });
        return { ok: true, text: response };
    }
    catch (err) {
        console.error('legalChat error', err);
        if (err instanceof functions.https.HttpsError)
            throw err;
        throw new functions.https.HttpsError('internal', (err === null || err === void 0 ? void 0 : err.message) || 'Unexpected error');
    }
});
//# sourceMappingURL=legalChat.js.map