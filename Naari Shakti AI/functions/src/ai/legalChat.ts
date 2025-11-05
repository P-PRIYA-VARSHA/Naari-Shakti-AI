import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import { GoogleGenerativeAI } from '@google/generative-ai';

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
export const legalChat = functions.https.onCall(async (data, context) => {
    try {
      // Require auth
      if (!context.auth) {
        throw new functions.https.HttpsError('unauthenticated', 'Authentication required');
      }

      const uid = context.auth.uid;
      const prompt: string | undefined = data?.prompt;
      const modelName: string = data?.modelName || 'gemini-1.5-pro'; // or gemini-1.5-flash for speed
      const temperature: number = typeof data?.temperature === 'number' ? data.temperature : 0.4;
      const maxOutputTokens: number = typeof data?.maxOutputTokens === 'number' ? data.maxOutputTokens : 1024;

      if (!prompt || typeof prompt !== 'string') {
        throw new functions.https.HttpsError('invalid-argument', 'Field "prompt" (string) is required.');
      }

      // Basic size guard
      const truncatedPrompt = prompt.length > 6000 ? prompt.slice(0, 6000) + '…' : prompt;

      // Optional: system framing to ensure safe, helpful, non-legal-advice language
      const systemInstruction =
        'You are a helpful legal information assistant for India. Provide concise, educational information with references to acts/sections when possible. Do not provide legal advice. Encourage consulting a licensed advocate for case-specific guidance.';

      // Prefer Firebase Functions Config on Spark plan; fallback to env var if set in emulator/CI
      const apiKey = (functions.config()?.gemini?.key as string | undefined) || process.env.GEMINI_API_KEY;
      if (!apiKey) {
        throw new functions.https.HttpsError('failed-precondition', 'Gemini API key is not configured. Set with: firebase functions:config:set gemini.key="YOUR_KEY"');
      }

      const genAI = new GoogleGenerativeAI(apiKey);
      const model = genAI.getGenerativeModel({ model: modelName, systemInstruction });

      // Optional: conversation history (array of {role: 'user'|'model', parts: string})
      const history = Array.isArray(data?.history) ? data.history : [];

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

      const response = result.response?.text?.() ?? '';

      // Log minimal analytics (do not log prompt or PII)
      await admin.firestore().collection('aiUsageLogs').add({
        uid,
        modelName,
        ts: admin.firestore.FieldValue.serverTimestamp(),
      });

      return { ok: true, text: response };
    } catch (err: any) {
      console.error('legalChat error', err);
      if (err instanceof functions.https.HttpsError) throw err;
      throw new functions.https.HttpsError('internal', err?.message || 'Unexpected error');
    }
  });
