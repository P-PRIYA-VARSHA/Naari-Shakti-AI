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
var _a;
Object.defineProperty(exports, "__esModule", { value: true });
exports.decryptToken = exports.encryptToken = void 0;
const functions = __importStar(require("firebase-functions"));
const ENCRYPTION_KEY = ((_a = functions.config().encryption) === null || _a === void 0 ? void 0 : _a.key) || 'default-key-for-development';
async function encryptToken(token) {
    // For now, return the token as-is (temporary implementation)
    // TODO: Implement proper encryption when needed
    return token;
}
exports.encryptToken = encryptToken;
async function decryptToken(encryptedToken) {
    // For now, return the token as-is (temporary implementation)
    // TODO: Implement proper decryption when needed
    return encryptedToken;
}
exports.decryptToken = decryptToken;
//# sourceMappingURL=encryption.js.map