import * as crypto from 'crypto';
import * as admin from 'firebase-admin';

export async function generateSetupToken(userEmail: string, contactGoogleEmail: string): Promise<string> {
  const token = crypto.randomBytes(32).toString('hex');
  const expiresAt = Date.now() + (24 * 60 * 60 * 1000); // 24 hours
  
  await admin.firestore().collection('setupTokens').doc(token).set({
    userEmail,
    contactGoogleEmail,
    expiresAt,
    status: 'pending',
    createdAt: admin.firestore.FieldValue.serverTimestamp()
  });
  
  return token;
}

export async function verifyContactAuth(setupToken: string, contactEmail: string): Promise<boolean> {
  const tokenDoc = await admin.firestore().collection('setupTokens').doc(setupToken).get();
  
  if (!tokenDoc.exists) {
    return false;
  }
  
  const tokenData = tokenDoc.data();
  if (tokenData?.expiresAt < Date.now()) {
    return false;
  }
  
  if (tokenData?.contactGoogleEmail !== contactEmail) {
    return false;
  }
  
  return tokenData?.status === 'pending';
}
