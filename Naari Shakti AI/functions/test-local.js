// Simple test script for running functions locally
const functions = require('firebase-functions');
const admin = require('firebase-admin');

// Initialize Firebase Admin (for local testing)
if (!admin.apps.length) {
  admin.initializeApp();
}

// Import your functions
const { initiateContactSetup, uploadEvidenceVideo } = require('./lib/index');

// Test function
async function testFunctions() {
  console.log('Testing functions locally...');
  
  // Test initiateContactSetup
  try {
    const result = await initiateContactSetup.run({
      data: {
        userEmail: 'test@example.com',
        contactGoogleEmail: 'contact@example.com'
      },
      context: {}
    });
    console.log('initiateContactSetup result:', result);
  } catch (error) {
    console.error('initiateContactSetup error:', error);
  }
}

// Run the test
testFunctions();
