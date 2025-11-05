import * as functions from 'firebase-functions';
import * as crypto from 'crypto';

const ENCRYPTION_KEY = functions.config().encryption?.key || 'default-key-for-development';

export async function encryptToken(token: string): Promise<string> {
  // For now, return the token as-is (temporary implementation)
  // TODO: Implement proper encryption when needed
  return token;
}

export async function decryptToken(encryptedToken: string): Promise<string> {
  // For now, return the token as-is (temporary implementation)
  // TODO: Implement proper decryption when needed
  return encryptedToken;
}
