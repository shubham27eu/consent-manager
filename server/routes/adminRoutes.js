const express = require("express");
const {
  getProviderBackLog,
  getSeekerBackLog,
  approveProvider,
  approveSeeker,
  inactivateProvider,
  inactivateSeeker,
  getInactiveUsers,
  reactivate,
  getProviders,  // New
  getSeekers,    // New
} = require("../controllers/adminController");
const auth = require("../middleware/auth");
const router = express.Router();

// Fetches a list of pending provider signup requests for admin review
router.get("/getProviderBackLog", auth, getProviderBackLog);

// Fetches a list of pending seeker signup requests for admin review
router.get("/getSeekerBackLog", auth, getSeekerBackLog);

// Approves or rejects a provider signup request, moving approved providers to the Provider table
router.post("/approval/provider", auth, approveProvider);

// Approves or rejects a seeker signup request, moving approved seekers to the Seeker table
router.post("/approval/seeker", auth, approveSeeker);

// Inactivates a provider and their associated data items and consents by userId
router.post("/inactivateProvider", auth, inactivateProvider);

// Inactivates a seeker by userId, preventing login and access
router.post("/inactivateSeeker", auth, inactivateSeeker);

// Fetches a list of all inactive users (providers and seekers) for admin review
router.get("/getInactiveUsers", auth, getInactiveUsers);

// Reactivates a provider or seeker by userId, restoring access
router.post("/reactivate", auth, reactivate);

// Fetches all active providers
router.get("/getProviders", auth, getProviders);

// Fetches all active seekers
router.get("/getSeekers", auth, getSeekers);

module.exports = router;