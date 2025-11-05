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
exports.verifyContactAuth = exports.generateSetupToken = void 0;
const crypto = __importStar(require("crypto"));
const admin = __importStar(require("firebase-admin"));
async function generateSetupToken(userEmail, contactGoogleEmail) {
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
exports.generateSetupToken = generateSetupToken;
async function verifyContactAuth(setupToken, contactEmail) {
    const tokenDoc = await admin.firestore().collection('setupTokens').doc(setupToken).get();
    if (!tokenDoc.exists) {
        return false;
    }
    const tokenData = tokenDoc.data();
    if ((tokenData === null || tokenData === void 0 ? void 0 : tokenData.expiresAt) < Date.now()) {
        return false;
    }
    if ((tokenData === null || tokenData === void 0 ? void 0 : tokenData.contactGoogleEmail) !== contactEmail) {
        return false;
    }
    return (tokenData === null || tokenData === void 0 ? void 0 : tokenData.status) === 'pending';
}
exports.verifyContactAuth = verifyContactAuth;
//# sourceMappingURL=generateSetupToken.js.map