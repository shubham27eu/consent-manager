
const admin = require("firebase-admin");
const serviceAccount = require("../consentmanager-7deb8-firebase-adminsdk-fbsvc-b405118171.json"); // Update path to your downloaded key

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  storageBucket: "consentmanager-7deb8.firebasestorage.app" // Replace with your bucket from Firebase Console
});

const bucket = admin.storage().bucket();
module.exports = { bucket };