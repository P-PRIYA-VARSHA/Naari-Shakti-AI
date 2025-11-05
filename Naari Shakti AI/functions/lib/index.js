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
exports.legalChat = exports.markEmailSent = exports.getPendingEmails = exports.validateSetupToken = exports.testUploadVideo = exports.uploadEvidenceVideo = exports.completeContactAuth = exports.initiateContactSetup = void 0;
const functions = __importStar(require("firebase-functions"));
const admin = __importStar(require("firebase-admin"));
const generateSetupToken_1 = require("./auth/generateSetupToken");
const sendSetupEmail_1 = require("./email/sendSetupEmail");
const uploadVideo_1 = require("./drive/uploadVideo");
// Initialize Firebase Admin
if (!admin.apps.length) {
    admin.initializeApp();
}
// Generate setup token and send email
exports.initiateContactSetup = functions.https.onCall(async (data, context) => {
    try {
        const { userEmail, contactGoogleEmail } = data;
        // Generate unique setup token
        const setupToken = await (0, generateSetupToken_1.generateSetupToken)(userEmail, contactGoogleEmail);
        // Send setup email
        await (0, sendSetupEmail_1.sendSetupEmail)(contactGoogleEmail, setupToken, userEmail);
        // Store pending setup in Firestore
        await storePendingSetup(userEmail, contactGoogleEmail, setupToken);
        return { success: true, message: 'Setup email sent successfully' };
    }
    catch (error) {
        console.error('Error initiating contact setup:', error);
        throw new functions.https.HttpsError('internal', 'Failed to initiate setup');
    }
});
// Handle contact authorization completion
exports.completeContactAuth = functions.https.onRequest(async (req, res) => {
    try {
        const { setupToken, contactEmail, refreshToken } = req.body;
        // Verify setup token
        const isValid = await (0, generateSetupToken_1.verifyContactAuth)(setupToken, contactEmail);
        if (!isValid) {
            res.status(400).json({ error: 'Invalid setup token' });
            return;
        }
        // Encrypt and store refresh token
        await storeEncryptedToken(contactEmail, refreshToken);
        // Update setup status
        await updateSetupStatus(setupToken, 'completed');
        res.json({ success: true, message: 'Authorization completed' });
    }
    catch (error) {
        console.error('Error completing contact auth:', error);
        res.status(500).json({ error: 'Failed to complete authorization' });
    }
});
// Upload video to contact's Google Drive
exports.uploadEvidenceVideo = functions.https.onRequest(async (req, res) => {
    try {
        // Enable CORS
        res.set('Access-Control-Allow-Origin', '*');
        res.set('Access-Control-Allow-Methods', 'GET, POST');
        res.set('Access-Control-Allow-Headers', 'Content-Type');
        // Handle preflight requests
        if (req.method === 'OPTIONS') {
            res.status(204).send('');
            return;
        }
        if (req.method !== 'POST') {
            res.status(405).json({ error: 'Method not allowed' });
            return;
        }
        const { userEmail, trustedContactEmail, videoData, fileName } = req.body;
        if (!userEmail || !trustedContactEmail || !videoData || !fileName) {
            res.status(400).json({ error: 'Missing required parameters' });
            return;
        }
        // Get contact's encrypted refresh token using the trusted contact email
        const refreshToken = await getDecryptedToken(trustedContactEmail);
        if (!refreshToken || refreshToken.trim() === '') {
            res.status(404).json({
                error: 'Trusted contact not found or not authorized',
                details: `No token found for email: ${trustedContactEmail}`
            });
            return;
        }
        // Upload to Google Drive with retry logic
        const result = await (0, uploadVideo_1.uploadVideo)(refreshToken, videoData, fileName, userEmail);
        res.json({ success: true, fileId: result.fileId });
    }
    catch (error) {
        console.error('Error uploading video:', error);
        res.status(500).json({
            error: 'Failed to upload video',
            details: error instanceof Error ? error.message : 'Unknown error'
        });
    }
});
// Test upload function (for development/testing)
exports.testUploadVideo = functions.https.onRequest(async (req, res) => {
    try {
        // Enable CORS
        res.set('Access-Control-Allow-Origin', '*');
        res.set('Access-Control-Allow-Methods', 'GET, POST');
        res.set('Access-Control-Allow-Headers', 'Content-Type');
        // Handle preflight requests
        if (req.method === 'OPTIONS') {
            res.status(204).send('');
            return;
        }
        if (req.method !== 'POST') {
            res.status(405).json({ error: 'Method not allowed' });
            return;
        }
        const { userEmail, videoData, fileName } = req.body;
        if (!userEmail || !videoData || !fileName) {
            res.status(400).json({ error: 'Missing required parameters' });
            return;
        }
        // For testing, use a mock token
        const mockToken = 'mock_refresh_token_for_testing';
        // Upload to Google Drive with retry logic
        const result = await (0, uploadVideo_1.uploadVideo)(mockToken, videoData, fileName, userEmail);
        res.json({ success: true, fileId: result.fileId, message: 'Test upload completed' });
    }
    catch (error) {
        console.error('Error in test upload:', error);
        res.status(500).json({
            error: 'Failed to upload video in test mode',
            details: error instanceof Error ? error.message : 'Unknown error'
        });
    }
});
// Validate setup token
exports.validateSetupToken = functions.https.onRequest(async (req, res) => {
    try {
        const { token } = req.query;
        if (!token || typeof token !== 'string') {
            res.status(400).json({ valid: false, error: 'Token required' });
            return;
        }
        const isValid = await verifySetupToken(token);
        res.json({ valid: isValid });
    }
    catch (error) {
        console.error('Error validating token:', error);
        res.status(500).json({ valid: false, error: 'Server error' });
    }
});
// Get pending emails for EmailJS
exports.getPendingEmails = functions.https.onCall(async (data, context) => {
    try {
        const pendingEmails = await admin.firestore()
            .collection('pendingEmails')
            .where('status', '==', 'pending')
            .limit(10)
            .get();
        const emails = [];
        pendingEmails.forEach((doc) => {
            emails.push(Object.assign({ id: doc.id }, doc.data()));
        });
        return { success: true, emails };
    }
    catch (error) {
        console.error('Error getting pending emails:', error);
        throw new functions.https.HttpsError('internal', 'Failed to get pending emails');
    }
});
// Mark email as sent
exports.markEmailSent = functions.https.onCall(async (data, context) => {
    try {
        const { emailId } = data;
        await admin.firestore().collection('pendingEmails').doc(emailId).update({
            status: 'sent',
            sentAt: admin.firestore.FieldValue.serverTimestamp()
        });
        return { success: true };
    }
    catch (error) {
        console.error('Error marking email as sent:', error);
        throw new functions.https.HttpsError('internal', 'Failed to mark email as sent');
    }
});
// Helper functions
async function storePendingSetup(userEmail, contactGoogleEmail, setupToken) {
    await admin.firestore().collection('pendingSetups').doc(setupToken).set({
        userEmail,
        contactGoogleEmail,
        status: 'pending',
        createdAt: admin.firestore.FieldValue.serverTimestamp()
    });
}
async function updateSetupStatus(setupToken, status) {
    await admin.firestore().collection('setupTokens').doc(setupToken).update({
        status,
        completedAt: admin.firestore.FieldValue.serverTimestamp()
    });
}
async function getDecryptedToken(contactEmail) {
    var _a;
    try {
        const tokenDoc = await admin.firestore().collection('contactTokens').doc(contactEmail).get();
        const encryptedToken = (_a = tokenDoc.data()) === null || _a === void 0 ? void 0 : _a.encryptedToken;
        if (!encryptedToken) {
            return null;
        }
        // For now, return the encrypted token as-is (temporary fix)
        // TODO: Implement proper decryption
        return encryptedToken;
    }
    catch (error) {
        console.error('Error getting decrypted token:', error);
        return null;
    }
}
async function storeEncryptedToken(contactEmail, refreshToken) {
    // For now, store the token as-is (temporary fix)
    // TODO: Implement proper encryption
    const encryptedToken = refreshToken;
    await admin.firestore().collection('contactTokens').doc(contactEmail).set({
        encryptedToken,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        lastUsed: admin.firestore.FieldValue.serverTimestamp()
    });
}
async function verifySetupToken(token) {
    const tokenDoc = await admin.firestore().collection('setupTokens').doc(token).get();
    if (!tokenDoc.exists) {
        return false;
    }
    const tokenData = tokenDoc.data();
    if ((tokenData === null || tokenData === void 0 ? void 0 : tokenData.expiresAt) < Date.now()) {
        return false;
    }
    return (tokenData === null || tokenData === void 0 ? void 0 : tokenData.status) === 'pending';
}
var legalChat_1 = require("./ai/legalChat");
Object.defineProperty(exports, "legalChat", { enumerable: true, get: function () { return legalChat_1.legalChat; } });
//# sourceMappingURL=index.js.map