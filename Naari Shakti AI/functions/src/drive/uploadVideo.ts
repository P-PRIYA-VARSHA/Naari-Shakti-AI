import { google } from 'googleapis';

export async function uploadVideo(refreshToken: string, videoData: string, fileName: string, userEmail: string): Promise<{ fileId: string }> {
  const maxRetries = 3;
  const baseDelay = 2000; // 2 seconds
  
  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      // Get access token from refresh token
      const oauth2Client = new google.auth.OAuth2();
      oauth2Client.setCredentials({ refresh_token: refreshToken });
      
      // Create Drive service
      const drive = google.drive({ version: 'v3', auth: oauth2Client });
      
      // Create or get evidence folder
      const folderId = await getOrCreateEvidenceFolder(drive, userEmail);
      
      // Upload video
      const fileMetadata = {
        name: fileName,
        parents: [folderId]
      };
      
      const media = {
        mimeType: 'video/mp4',
        body: Buffer.from(videoData, 'base64')
      };
      
      const file = await drive.files.create({
        requestBody: fileMetadata,
        media: media,
        fields: 'id'
      });
      
      return { fileId: file.data.id! };
      
    } catch (error) {
      console.error(`Upload attempt ${attempt} failed:`, error);
      
      if (attempt === maxRetries) {
        throw error;
      }
      
      // Exponential backoff
      const delay = baseDelay * Math.pow(2, attempt - 1);
      await new Promise(resolve => setTimeout(resolve, delay));
    }
  }
  
  throw new Error('All upload attempts failed');
}

async function getOrCreateEvidenceFolder(drive: any, userEmail: string): Promise<string> {
  // Check if folder already exists
  const folderName = `SOS_Evidence_${userEmail.replace('@', '_at_')}`;
  
  const response = await drive.files.list({
    q: `name='${folderName}' and mimeType='application/vnd.google-apps.folder' and trashed=false`,
    fields: 'files(id, name)'
  });
  
  if (response.data.files && response.data.files.length > 0) {
    return response.data.files[0].id;
  }
  
  // Create new folder
  const folderMetadata = {
    name: folderName,
    mimeType: 'application/vnd.google-apps.folder'
  };
  
  const folder = await drive.files.create({
    requestBody: folderMetadata,
    fields: 'id'
  });
  
  return folder.data.id;
}
