// Test script for Firebase Functions emulator
const http = require('http');

// Test the validateSetupToken function
function testValidateSetupToken() {
    const options = {
        hostname: 'localhost',
        port: 5001,
        path: '/she-56fea/us-central1/validateSetupToken?token=test123',
        method: 'GET',
        headers: {
            'Content-Type': 'application/json'
        }
    };

    const req = http.request(options, (res) => {
        console.log(`✅ validateSetupToken status: ${res.statusCode}`);
        let data = '';
        res.on('data', (chunk) => {
            data += chunk;
        });
        res.on('end', () => {
            console.log(`📄 Response: ${data}`);
        });
    });

    req.on('error', (error) => {
        console.error(`❌ Error testing validateSetupToken: ${error.message}`);
    });

    req.end();
}

// Test the uploadEvidenceVideo function
function testUploadEvidenceVideo() {
    const testPayload = JSON.stringify({
        userEmail: 'test@example.com',
        trustedContactEmail: 'contact@example.com',
        videoData: 'test_video_data_base64',
        fileName: 'test_video.mp4'
    });

    const options = {
        hostname: 'localhost',
        port: 5001,
        path: '/she-56fea/us-central1/uploadEvidenceVideo',
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Content-Length': Buffer.byteLength(testPayload)
        }
    };

    const req = http.request(options, (res) => {
        console.log(`✅ uploadEvidenceVideo status: ${res.statusCode}`);
        let data = '';
        res.on('data', (chunk) => {
            data += chunk;
        });
        res.on('end', () => {
            console.log(`📄 Response: ${data}`);
        });
    });

    req.on('error', (error) => {
        console.error(`❌ Error testing uploadEvidenceVideo: ${error.message}`);
    });

    req.write(testPayload);
    req.end();
}

// Run tests
console.log('🧪 Testing Firebase Functions emulator...');
console.log('📍 Testing functions at: http://localhost:5001');

setTimeout(() => {
    console.log('\n🔍 Testing validateSetupToken...');
    testValidateSetupToken();
}, 1000);

setTimeout(() => {
    console.log('\n🔍 Testing uploadEvidenceVideo...');
    testUploadEvidenceVideo();
}, 2000);
