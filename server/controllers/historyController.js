const Consent = require("../models/Consent");
const ConsentHistory = require("../models/ConsentHistory");
const Provider = require("../models/Provider");
const Seeker = require("../models/Seeker");
const DataItem = require("../models/DataItem");

// Get consent history for a provider
const getConsentHistoryByProviderId = async (req, res) => {
  console.log("Step 1: Fetching consent history for providerId:", req.provider.id);
  if (req.provider.role !== "provider") { // Updated from "individual" to "provider"
    console.log("Unauthorized attempt by:", req.provider.id);
    return res.status(403).json({ message: "Unauthorized" });
  }

  try {
    // Fetch Provider using credential_id
    const provider = await Provider.findOne({ credential_id: req.provider.id });
    if (!provider) {
      console.log("Provider not found for credential_id:", req.provider.id);
      return res.status(404).json({ message: "Provider not found" });
    }

    const consents = await Consent.find({ provider_id: provider._id })
      .populate("item_id", "item_name item_type")
      .populate("seeker_id", "name type email contact_no");
    console.log("Step 2: Found consents for provider:", consents.length);

    const consentIds = consents.map((consent) => consent._id);
    console.log("Step 3: Extracted consent IDs:", consentIds);

    const consentHistories = await ConsentHistory.find({ consent_id: { $in: consentIds } }).sort({ timestamp: -1 });
    console.log("Step 4: Found consent histories:", consentHistories.length);

    const result = await Promise.all(
      consentHistories.map(async (history) => {
        const consent = consents.find((c) => c._id.equals(history.consent_id));
        if (!consent) return null;

        const seeker = consent.seeker_id;
        let seekerName = "Unknown Seeker";
        let additionalInfo = history.additional_info || "N/A";

        if (seeker) {
          seekerName = seeker.name || "Unknown Seeker";
          additionalInfo = `Name: ${seekerName}, Type: ${seeker.type || "N/A"}, Email: ${seeker.email || "N/A"}, Contact: ${seeker.contact_no || "N/A"}`;
        }

        return {
          consent_id: history.consent_id,
          item_name: consent.item_id.item_name,
          item_type: consent.item_id.item_type,
          seeker_name: seekerName,
          status: history.new_status,
          requested_at: history.timestamp,
          additional_info: additionalInfo,
        };
      })
    );
    console.log("Step 5: Formatted history result:", result.length);

    const filteredResult = result.filter((item) => item !== null);
    console.log("Step 6: Filtered history result:", filteredResult.length);

    if (filteredResult.length > 0) {
      console.log("Step 7: Sending success response with history:", filteredResult.length);
      return res.status(200).send({
        success: true,
        data: filteredResult,
      });
    } else {
      console.log("Step 7: No consent history found.");
      return res.status(404).send({
        success: false,
        message: "No consent history found for the given provider.",
      });
    }
  } catch (error) {
    console.error("Step 8: Error fetching consent history:", error.message);
    return res.status(500).send({
      success: false,
      message: "Could not fetch consent history.",
    });
  }
};

// Get consent history for a seeker
const getConsentHistoryBySeekerId = async (req, res) => {
  console.log("Step 1: Fetching consent history for seekerId:", req.provider.id);
  if (req.provider.role !== "seeker") {
    console.log("Unauthorized attempt by:", req.provider.id);
    return res.status(403).json({ message: "Unauthorized" });
  }

  try {
    // Fetch Seeker using credential_id
    const seeker = await Seeker.findOne({ credential_id: req.provider.id });
    if (!seeker) {
      console.log("Seeker not found for credential_id:", req.provider.id);
      return res.status(404).json({ message: "Seeker not found" });
    }

    const consents = await Consent.find({ seeker_id: seeker._id })
      .populate("item_id", "item_name item_type")
      .populate("provider_id", "first_name last_name email mobile_no");
    console.log("Step 2: Found consents for seeker:", consents.length);

    const consentIds = consents.map((consent) => consent._id);
    console.log("Step 3: Extracted consent IDs:", consentIds);

    const consentHistories = await ConsentHistory.find({ consent_id: { $in: consentIds } }).sort({ timestamp: -1 });
    console.log("Step 4: Found consent histories:", consentHistories.length);

    const result = await Promise.all(
      consentHistories.map(async (history) => {
        const consent = consents.find((c) => c._id.equals(history.consent_id));
        if (!consent) return null;

        const provider = consent.provider_id;
        let providerName = "Unknown Provider";
        let additionalInfo = "N/A";

        if (provider) {
          providerName = provider.first_name
            ? `${provider.first_name} ${provider.last_name || ""}`.trim()
            : "Unknown Provider";
          additionalInfo = `Name: ${providerName}, Email: ${provider.email || "N/A"}, Contact: ${provider.mobile_no || "N/A"}`;
        }

        return {
          consent_id: history.consent_id,
          item_name: consent.item_id.item_name,
          item_type: consent.item_id.item_type,
          provider_name: providerName,
          status: history.new_status,
          requested_at: history.timestamp,
          additional_info: additionalInfo,
        };
      })
    );
    console.log("Step 5: Formatted history result:", result.length);

    const filteredResult = result.filter((item) => item !== null);
    console.log("Step 6: Filtered history result:", filteredResult.length);

    if (filteredResult.length > 0) {
      console.log("Step 7: Sending success response with history:", filteredResult.length);
      return res.status(200).send({
        success: true,
        data: filteredResult,
      });
    } else {
      console.log("Step 7: No consent history found.");
      return res.status(404).send({
        success: false,
        message: "No consent history found for the given seeker.",
      });
    }
  } catch (error) {
    console.error("Step 8: Error fetching consent history:", error.message);
    return res.status(500).send({
      success: false,
      message: "Could not fetch consent history.",
    });
  }
};

module.exports = { getConsentHistoryByProviderId, getConsentHistoryBySeekerId };