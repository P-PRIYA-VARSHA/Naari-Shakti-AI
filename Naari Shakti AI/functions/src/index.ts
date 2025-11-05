import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import { generateSetupToken, verifyContactAuth } from './auth/generateSetupToken';
import { sendSetupEmail } from './email/sendSetupEmail';
import { uploadVideo } from './drive/uploadVideo';

// Initialize Firebase Admin
if (!admin.apps.length) {
  admin.initializeApp();
}

// Generate setup token and send email
export const initiateContactSetup = functions.https.onCall(async (data, context) => {
  try {
    const { userEmail, contactGoogleEmail } = data;
    
    // Generate unique setup token
    const setupToken = await generateSetupToken(userEmail, contactGoogleEmail);
    
    // Send setup email
    await sendSetupEmail(contactGoogleEmail, setupToken, userEmail);
    
    // Store pending setup in Firestore
    await storePendingSetup(userEmail, contactGoogleEmail, setupToken);
    
    return { success: true, message: 'Setup email sent successfully' };
  } catch (error) {
    console.error('Error initiating contact setup:', error);
    throw new functions.https.HttpsError('internal', 'Failed to initiate setup');
  }
});

// Handle contact authorization completion
export const completeContactAuth = functions.https.onRequest(async (req, res) => {
  try {
    const { setupToken, contactEmail, refreshToken } = req.body;
    
    // Verify setup token
    const isValid = await verifyContactAuth(setupToken, contactEmail);
    if (!isValid) {
      res.status(400).json({ error: 'Invalid setup token' });
      return;
    }
    
    // Encrypt and store refresh token
    await storeEncryptedToken(contactEmail, refreshToken);
    
    // Update setup status
    await updateSetupStatus(setupToken, 'completed');
    
    res.json({ success: true, message: 'Authorization completed' });
  } catch (error) {
    console.error('Error completing contact auth:', error);
    res.status(500).json({ error: 'Failed to complete authorization' });
  }
});

// Upload video to contact's Google Drive
export const uploadEvidenceVideo = functions.https.onRequest(async (req, res) => {
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
    const result = await uploadVideo(refreshToken, videoData, fileName, userEmail);
    
    res.json({ success: true, fileId: result.fileId });
  } catch (error) {
    console.error('Error uploading video:', error);
    res.status(500).json({ 
      error: 'Failed to upload video',
      details: error instanceof Error ? error.message : 'Unknown error'
    });
  }
});

// Test upload function (for development/testing)
export const testUploadVideo = functions.https.onRequest(async (req, res) => {
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
    const result = await uploadVideo(mockToken, videoData, fileName, userEmail);
    
    res.json({ success: true, fileId: result.fileId, message: 'Test upload completed' });
  } catch (error) {
    console.error('Error in test upload:', error);
    res.status(500).json({ 
      error: 'Failed to upload video in test mode',
      details: error instanceof Error ? error.message : 'Unknown error'
    });
  }
});

// Validate setup token
export const validateSetupToken = functions.https.onRequest(async (req, res) => {
  try {
    const { token } = req.query;
    
    if (!token || typeof token !== 'string') {
      res.status(400).json({ valid: false, error: 'Token required' });
      return;
    }
    
    const isValid = await verifySetupToken(token);
    
    res.json({ valid: isValid });
  } catch (error) {
    console.error('Error validating token:', error);
    res.status(500).json({ valid: false, error: 'Server error' });
  }
});

// Get pending emails for EmailJS
export const getPendingEmails = functions.https.onCall(async (data, context) => {
  try {
    const pendingEmails = await admin.firestore()
      .collection('pendingEmails')
      .where('status', '==', 'pending')
      .limit(10)
      .get();
    
    const emails: any[] = [];
    pendingEmails.forEach((doc: any) => {
      emails.push({
        id: doc.id,
        ...doc.data()
      });
    });
    
    return { success: true, emails };
  } catch (error) {
    console.error('Error getting pending emails:', error);
    throw new functions.https.HttpsError('internal', 'Failed to get pending emails');
  }
});

// Mark email as sent
export const markEmailSent = functions.https.onCall(async (data, context) => {
  try {
    const { emailId } = data;
    
    await admin.firestore().collection('pendingEmails').doc(emailId).update({
      status: 'sent',
      sentAt: admin.firestore.FieldValue.serverTimestamp()
    });
    
    return { success: true };
  } catch (error) {
    console.error('Error marking email as sent:', error);
    throw new functions.https.HttpsError('internal', 'Failed to mark email as sent');
  }
});

// Helper functions
async function storePendingSetup(userEmail: string, contactGoogleEmail: string, setupToken: string) {
  await admin.firestore().collection('pendingSetups').doc(setupToken).set({
    userEmail,
    contactGoogleEmail,
    status: 'pending',
    createdAt: admin.firestore.FieldValue.serverTimestamp()
  });
}

async function updateSetupStatus(setupToken: string, status: string) {
  await admin.firestore().collection('setupTokens').doc(setupToken).update({
    status,
    completedAt: admin.firestore.FieldValue.serverTimestamp()
  });
}

async function getDecryptedToken(contactEmail: string): Promise<string | null> {
  try {
    
    const tokenDoc = await admin.firestore().collection('contactTokens').doc(contactEmail).get();
    const encryptedToken = tokenDoc.data()?.encryptedToken;
    
    if (!encryptedToken) {
      return null;
    }
    
    // For now, return the encrypted token as-is (temporary fix)
    // TODO: Implement proper decryption
    return encryptedToken;
  } catch (error) {
    console.error('Error getting decrypted token:', error);
    return null;
  }
}

async function storeEncryptedToken(contactEmail: string, refreshToken: string) {
  
  // For now, store the token as-is (temporary fix)
  // TODO: Implement proper encryption
  const encryptedToken = refreshToken;
  
  await admin.firestore().collection('contactTokens').doc(contactEmail).set({
    encryptedToken,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    lastUsed: admin.firestore.FieldValue.serverTimestamp()
  });
}

async function verifySetupToken(token: string): Promise<boolean> {
  const tokenDoc = await admin.firestore().collection('setupTokens').doc(token).get();
  
  if (!tokenDoc.exists) {
    return false;
  }
  
  const tokenData = tokenDoc.data();
  if (tokenData?.expiresAt < Date.now()) {
    return false;
  }
  
  return tokenData?.status === 'pending';
}

export { legalChat } from './ai/legalChat';
