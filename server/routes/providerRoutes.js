const express = require("express");
const { 
  addItem, 
  getItems, 
  getConsentList, 
  giveConsent, 
  getUserData, 
  editItem, 
  deleteItem, 
  addFileItem, 
  getNonTextItems, 
  fetchFile // Add fetchFile
} = require("../controllers/dataController");
const { getConsentHistoryByProviderId } = require("../controllers/historyController");
const auth = require("../middleware/auth");
const multer = require("multer");
const router = express.Router();

// Configure multer for file uploads
const upload = multer({ storage: multer.memoryStorage() });



// Adds a new encrypted text data item
router.post("/:providerId/addItems", auth, addItem);

// Adds a new file-based data item (e.g., image, PDF)
router.post("/:providerId/addFileItem", auth, upload.single("file"), addFileItem);

// Retrieves all active data items owned by the provider
router.get("/:providerId/getItems", auth, getItems);

// Retrieves all active data items owned by the provider that are not text-based
router.get("/:providerId/getNonTextItems", auth, getNonTextItems);

// Fetches a list of pending consent requests from seekers for the provider's data
router.get("/:providerId/getConsentList", auth, getConsentList);

// Allows the provider to approve or revoke a seeker's consent request
router.post("/giveConsent", auth, giveConsent);

// Returns the consent history for all actions related to the provider's data
router.get("/:providerId/getConsentHistory", auth, getConsentHistoryByProviderId);

// Fetch provider's user data
router.get("/:providerId/getUserData", auth, getUserData);

// Edit an existing data item
router.put("/:providerId/editItem", auth, editItem);

// Delete an existing data item
router.delete("/:providerId/deleteItem", auth, deleteItem);

// New route to fetch files via backend proxy
router.get("/:providerId/fetchFile", auth, fetchFile);

module.exports = router;