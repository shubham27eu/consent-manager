const express = require("express");
const { accessItem, getProviderItems, requestAccessAgain, getSeekerData, fetchSeekerFile } = require("../controllers/dataController");
const { getConsentHistoryBySeekerId } = require("../controllers/historyController");
const auth = require("../middleware/auth");
const router = express.Router();

// Routes for seeker actions
router.get("/providerItems", auth, getProviderItems); // Fetch provider items by username
router.post("/:seeker/accessItem", auth, accessItem); // Access an item and check consent
router.post("/:seeker/requestAccessAgain", auth, requestAccessAgain); // Re-request access for rejected/revoked/expired/count exhausted
router.get("/:seekerId/getConsentHistory", auth, getConsentHistoryBySeekerId); // Get consent history for seeker
router.get("/:seekerId/getSeekerData", auth, getSeekerData); // Fetch seeker data
router.get("/:seekerId/fetchFile", auth, fetchSeekerFile); // New route to fetch file for seekers

module.exports = router;