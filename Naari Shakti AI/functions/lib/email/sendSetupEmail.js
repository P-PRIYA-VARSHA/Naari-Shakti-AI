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
exports.sendSetupEmail = void 0;
const admin = __importStar(require("firebase-admin"));
async function sendSetupEmail(contactEmail, setupToken, userEmail) {
    // Use EmailJS to send email
    const emailData = {
        service_id: 'service_wmfy1ym',
        template_id: 'template_dygrjqs',
        user_id: '4_khaQaZdPlN0GJA1',
        template_params: {
            user_email: userEmail,
            setup_link: `https://she-56fea.web.app/setup-drive?token=${setupToken}`,
            to_email: contactEmail
        }
    };
    // Store email data in Firestore for the client to send via EmailJS
    await admin.firestore().collection('pendingEmails').add(Object.assign(Object.assign({}, emailData), { contactEmail: contactEmail, createdAt: admin.firestore.FieldValue.serverTimestamp(), status: 'pending' }));
}
exports.sendSetupEmail = sendSetupEmail;
//# sourceMappingURL=sendSetupEmail.js.map