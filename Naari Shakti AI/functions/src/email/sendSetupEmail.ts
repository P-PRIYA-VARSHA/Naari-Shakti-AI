import * as admin from 'firebase-admin';

export async function sendSetupEmail(contactEmail: string, setupToken: string, userEmail: string): Promise<void> {
  // Use EmailJS to send email
  const emailData = {
    service_id: 'service_wmfy1ym', // Your EmailJS service ID
    template_id: 'template_dygrjqs', // Your EmailJS template ID
    user_id: '4_khaQaZdPlN0GJA1', // Your EmailJS public key
    template_params: {
      user_email: userEmail,
      setup_link: `https://she-56fea.web.app/setup-drive?token=${setupToken}`,
      to_email: contactEmail
    }
  };

  // Store email data in Firestore for the client to send via EmailJS
  await admin.firestore().collection('pendingEmails').add({
    ...emailData,
    contactEmail: contactEmail,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    status: 'pending'
  });
}
