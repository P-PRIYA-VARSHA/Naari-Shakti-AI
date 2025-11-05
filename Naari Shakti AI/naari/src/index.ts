import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();

interface HelloNaariData {
  name?: string;
}

export const helloNaari = functions.https.onCall((request) => {
  // request.data contains the input
  const data = request.data as HelloNaariData;
  const userName = data?.name ?? "Guest";

  return {
    message: `Hello, ${userName}! Welcome to Naari Shakti AI 🚀`,
  };
});
