const DataItem = require("../models/DataItem");
const Consent = require("../models/Consent");
const ConsentHistory = require("../models/ConsentHistory");
const Provider = require("../models/Provider");
const Seeker = require("../models/Seeker");
const Credential = require("../models/Credential");
const { v4: uuidv4 } = require("uuid");
const { bucket } = require("../config/firebaseAdmin");
const fetch = require("node-fetch");

const addFileItem = async (req, res) => {
  console.log("Add file item request received:", req.body, req.file);
  const { item_name, item_type, encryptedAESKey, iv } = req.body;
  if (req.provider.role !== "provider") {
    console.log("Unauthorized attempt to add file item by:", req.provider.id);
    return res.status(403).json({ message: "Unauthorized" });
  }
  if (!req.file) {
    console.log("No file uploaded");
    return res.status(400).json({ message: "File is required" });
  }

  try {
    const provider = await Provider.findOne({ credential_id: req.provider.id });
    if (!provider) {
      console.log("Provider not found for credential_id:", req.provider.id);
      return res.status(404).json({ message: "Provider not found" });
    }

    const fileName = `${provider._id}/${Date.now()}-${req.file.originalname}`;
    const file = bucket.file(fileName);
    await file.save(req.file.buffer, {
      metadata: { contentType: req.file.mimetype },
    });
    const [fileUrl] = await file.getSignedUrl({
      action: "read",
      expires: "03-26-2026",
    });

    const dataItem = new DataItem({
      item_name,
      item_type,
      item_owner_id: provider._id,
      encryptedUrl: fileUrl,
      encryptedAESKey,
      iv,
    });
    await dataItem.save();
    console.log("File item added:", dataItem._id, "for provider:", provider._id);
    res.status(201).json({ 
      message: "File item added", 
      itemId: dataItem._id,
      encryptedUrl: fileUrl
    });
  } catch (error) {
    console.error("Error adding file item:", error.message);
    res.status(500).json({ message: error.message });
  }
};

const addItem = async (req, res) => {
  console.log("Add item request received:", req.body);
  const { item_name, item_type, encryptedData, encryptedAESKey, iv } = req.body;
  if (req.provider.role !== "provider") {
    console.log("Unauthorized attempt to add item by:", req.provider.id);
    return res.status(403).json({ message: "Unauthorized" });
  }

  try {
    const provider = await Provider.findOne({ credential_id: req.provider.id });
    if (!provider) {
      console.log("Provider not found for credential_id:", req.provider.id);
      return res.status(404).json({ message: "Provider not found" });
    }

    const dataItem = new DataItem({
      item_name,
      item_type,
      item_owner_id: provider._id,
      encryptedData: item_type === "text" ? Buffer.from(encryptedData, "base64") : null,
      encryptedUrl: item_type !== "text" ? encryptedAESKey : null,
      encryptedAESKey,
      iv,
    });
    await dataItem.save();
    console.log("Data item added:", dataItem._id, "for provider:", provider._id);
    res.status(201).json({ message: "Item added", itemId: dataItem._id });
  } catch (error) {
    console.error("Error adding item:", error.message);
    res.status(500).json({ message: error.message });
  }
};

const getItems = async (req, res) => {
  console.log("Get items request for provider:", req.provider.id);
  if (req.provider.role !== "provider") {
    console.log("Unauthorized attempt to get items by:", req.provider.id);
    return res.status(403).json({ message: "Unauthorized" });
  }

  try {
    const provider = await Provider.findOne({ credential_id: req.provider.id });
    if (!provider) {
      console.log("Provider not found for credential_id:", req.provider.id);
      return res.status(404).json({ message: "Provider not found" });
    }

    const items = await DataItem.find({ 
      item_owner_id: provider._id, 
      isActive: true,
      item_type: "text"
    });
    console.log("Text items retrieved:", items.length);
    res.json(
      items.map((item) => ({
        itemId: item._id,
        item_name: item.item_name,
        item_type: item.item_type,
        encryptedData: item.encryptedData?.toString("base64"),
        encryptedAESKey: item.encryptedAESKey,
        iv: item.iv,
      }))
    );
  } catch (error) {
    console.error("Error getting items:", error.message);
    res.status(500).json({ message: error.message });
  }
};

const getNonTextItems = async (req, res) => {
  console.log("Get non-text items request for provider:", req.provider.id);
  if (req.provider.role !== "provider") {
    console.log("Unauthorized attempt to get non-text items by:", req.provider.id);
    return res.status(403).json({ message: "Unauthorized" });
  }

  try {
    const provider = await Provider.findOne({ credential_id: req.provider.id });
    if (!provider) {
      console.log("Provider not found for credential_id:", req.provider.id);
      return res.status(404).json({ message: "Provider not found" });
    }

    const items = await DataItem.find({ 
      item_owner_id: provider._id, 
      isActive: true, 
      item_type: { $ne: "text" }
    });
    console.log("Non-text items retrieved:", items.length);
    res.json(
      items.map((item) => ({
        itemId: item._id,
        item_name: item.item_name,
        item_type: item.item_type,
        encryptedUrl: item.encryptedUrl,
        encryptedAESKey: item.encryptedAESKey,
        iv: item.iv,
        createdAt: item.createdAt,
      }))
    );
  } catch (error) {
    console.error("Error getting non-text items:", error.message);
    res.status(500).json({ message: error.message });
  }
};

const getConsentList = async (req, res) => {
  console.log("Get consent list request for provider:", req.provider.id);
  if (req.provider.role !== "provider") {
    console.log("Unauthorized attempt to get consent list by:", req.provider.id);
    return res.status(403).json({ message: "Unauthorized" });
  }

  try {
    const provider = await Provider.findOne({ credential_id: req.provider.id });
    if (!provider) {
      console.log("Provider not found for credential_id:", req.provider.id);
      return res.status(404).json({ message: "Provider not found" });
    }

    const consents = await Consent.find({ provider_id: provider._id, status: "pending", isActive: true })
      .populate("item_id", "item_name")
      .populate("seeker_id", "name email publicKey");
    console.log("Pending consents retrieved:", consents.length);

    const formattedConsents = consents.map((consent) => ({
      consent_id: consent._id,
      item_name: consent.item_id?.item_name,
      seeker_name: consent.seeker_id?.name,
      seeker_email: consent.seeker_id?.email,
      seeker_publicKey: consent.seeker_id?.publicKey,
      status: consent.status,
      date_created: consent.date_created,
    }));

    res.json(formattedConsents);
    console.log("Consent list sent for provider:", provider._id);
  } catch (error) {
    console.error("Error getting consent list:", error.message);
    res.status(500).json({ message: error.message });
  }
};

const giveConsent = async (req, res) => {
  console.log("Give consent request received:", req.body);
  const { consent_id, consent, count, validity, encryptedAESKeyForSeeker } = req.body;
  if (req.provider.role !== "provider") {
    console.log("Unauthorized attempt to give consent by:", req.provider.id);
    return res.status(403).json({ message: "Unauthorized" });
  }

  try {
    const provider = await Provider.findOne({ credential_id: req.provider.id });
    if (!provider) {
      console.log("Provider not found for credential_id:", req.provider.id);
      return res.status(404).json({ message: "Provider not found" });
    }

    const consentRequest = await Consent.findById(consent_id)
      .populate("seeker_id", "publicKey")
      .populate("item_id", "encryptedAESKey iv");
    if (!consentRequest || consentRequest.provider_id.toString() !== provider._id.toString() || !consentRequest.isActive) {
      console.log("Consent not found, not owned, or inactive:", consent_id, provider._id);
      return res.status(404).json({ message: "Consent request not found or inactive" });
    }

    const previousStatus = consentRequest.status;
    consentRequest.status = consent === "yes" ? "approved" : consent === "revoke" ? "revoked" : "rejected";

    if (consent === "yes") {
      consentRequest.access_count = count || 1;
      consentRequest.validity_period = validity ? new Date(validity) : consentRequest.validity_period;
      if (encryptedAESKeyForSeeker) {
        consentRequest.encryptedAESKeyForSeeker = encryptedAESKeyForSeeker;
      }
    }

    await consentRequest.save();
    await ConsentHistory.create({
      history_id: uuidv4(),
      consent_id: consentRequest._id,
      changed_by: provider._id,
      previous_status: previousStatus,
      new_status: consentRequest.status,
      action_type: consent === "yes" ? "approve" : consent === "revoke" ? "revoke" : "reject",
      remarks: `Action: ${consent}`,
      additional_info: `Count: ${count || "N/A"}, Validity: ${validity || "N/A"}`,
    });

    if (consent === "yes" && !encryptedAESKeyForSeeker) {
      res.json({
        message: `Consent ${consentRequest.status}`,
        consentRequest: {
          _id: consentRequest._id,
          seekerPublicKey: consentRequest.seeker_id.publicKey,
          originalEncryptedAESKey: consentRequest.item_id.encryptedAESKey,
          iv: consentRequest.item_id.iv,
        },
      });
    } else {
      res.json({ message: `Consent ${consentRequest.status}`, consentRequest });
    }
  } catch (error) {
    console.error("Error giving consent:", error.message);
    res.status(500).json({ message: error.message });
  }
};

const getUserData = async (req, res) => {
  console.log("Get user data request for provider:", req.provider.id);
  if (req.provider.role !== "provider") {
    console.log("Unauthorized attempt to get user data by:", req.provider.id);
    return res.status(403).json({ message: "Unauthorized" });
  }

  try {
    const provider = await Provider.findOne({ credential_id: req.provider.id });
    if (!provider) {
      console.log("Provider not found for credential_id:", req.provider.id);
      return res.status(404).json({ message: "Provider not found" });
    }

    res.json({
      first_name: provider.first_name,
      middle_name: provider.middle_name || "",
      last_name: provider.last_name,
      email: provider.email,
      mobile_no: provider.mobile_no,
      date_of_birth: provider.date_of_birth,
      age: provider.age,
      publicKey: provider.publicKey,
    });
  } catch (error) {
    console.error("Error fetching user data:", error.message);
    res.status(500).json({ message: error.message });
  }
};

const editItem = async (req, res) => {
  console.log("Edit item request received:", req.body);
  const { itemId, item_name, item_type, encryptedData, encryptedAESKey, iv } = req.body;
  if (req.provider.role !== "provider") {
    console.log("Unauthorized attempt to edit item by:", req.provider.id);
    return res.status(403).json({ message: "Unauthorized" });
  }

  try {
    const provider = await Provider.findOne({ credential_id: req.provider.id });
    if (!provider) {
      console.log("Provider not found for credential_id:", req.provider.id);
      return res.status(404).json({ message: "Provider not found" });
    }

    const dataItem = await DataItem.findById(itemId);
    if (!dataItem || dataItem.item_owner_id.toString() !== provider._id.toString() || !dataItem.isActive) {
      console.log("Item not found, not owned, or inactive:", itemId);
      return res.status(404).json({ message: "Item not found or inactive" });
    }

    dataItem.item_name = item_name || dataItem.item_name;
    dataItem.item_type = item_type || dataItem.item_type;
    if (item_type === "text" || dataItem.item_type === "text") {
      dataItem.encryptedData = encryptedData ? Buffer.from(encryptedData, "base64") : dataItem.encryptedData;
      dataItem.encryptedUrl = null;
    } else {
      dataItem.encryptedUrl = encryptedAESKey || dataItem.encryptedUrl;
      dataItem.encryptedData = null;
    }
    dataItem.encryptedAESKey = encryptedAESKey || dataItem.encryptedAESKey;
    dataItem.iv = iv || dataItem.iv;

    await dataItem.save();
    console.log("Data item updated:", dataItem._id);
    res.status(200).json({ message: "Item updated", itemId: dataItem._id });
  } catch (error) {
    console.error("Error editing item:", error.message);
    res.status(500).json({ message: error.message });
  }
};

const deleteItem = async (req, res) => {
  console.log("Delete item request received:", req.body);
  const { itemId } = req.body;
  if (req.provider.role !== "provider") {
    console.log("Unauthorized attempt to delete item by:", req.provider.id);
    return res.status(403).json({ message: "Unauthorized" });
  }

  try {
    const provider = await Provider.findOne({ credential_id: req.provider.id });
    if (!provider) {
      console.log("Provider not found for credential_id:", req.provider.id);
      return res.status(404).json({ message: "Provider not found" });
    }

    const dataItem = await DataItem.findById(itemId);
    if (!dataItem || dataItem.item_owner_id.toString() !== provider._id.toString()) {
      console.log("Item not found or not owned:", itemId);
      return res.status(404).json({ message: "Item not found" });
    }

    dataItem.isActive = false;
    await dataItem.save();
    await Consent.updateMany({ item_id: dataItem._id }, { isActive: false });
    console.log("Data item marked inactive:", dataItem._id);
    res.status(200).json({ message: "Item deleted" });
  } catch (error) {
    console.error("Error deleting item:", error.message);
    res.status(500).json({ message: error.message });
  }
};

const getProviderItems = async (req, res) => {
  console.log("Get provider items request received:", req.query);
  if (req.provider.role !== "seeker") {
    console.log("Unauthorized attempt by:", req.provider.id);
    return res.status(403).json({ message: "Unauthorized" });
  }

  const { username } = req.query;
  if (!username) {
    console.log("No username provided in query");
    return res.status(400).json({ message: "Provider username is required" });
  }

  try {
    const credential = await Credential.findOne({ username, role: "provider" });
    if (!credential) {
      console.log("Credential not found for username:", username);
      return res.status(404).json({ message: "Provider not found" });
    }

    const provider = await Provider.findOne({ credential_id: credential._id });
    if (!provider) {
      console.log("Provider not found for credential_id:", credential._id);
      return res.status(404).json({ message: "Provider not found" });
    }

    const dataItems = await DataItem.find({ item_owner_id: provider._id, isActive: true });
    console.log("Active items retrieved:", dataItems.length);
    const itemsMetadata = dataItems.map((item) => ({
      itemId: item._id,
      item_name: item.item_name,
      item_type: item.item_type,
    }));

    res.json({
      provider: {
        username: credential.username,
        firstname: provider.first_name,
        lastname: provider.last_name,
        email: provider.email,
        mobile: provider.mobile_no,
      },
      items: itemsMetadata,
    });
  } catch (error) {
    console.error("Error fetching provider items:", error.message);
    res.status(500).json({ message: error.message });
  }
};

const accessItem = async (req, res) => {
  console.log("Access item request received:", req.body);
  const { item_id } = req.body;
  if (req.provider.role !== "seeker") {
    console.log("Unauthorized attempt to access item by:", req.provider.id);
    return res.status(403).json({ message: "Unauthorized" });
  }

  try {
    const seeker = await Seeker.findOne({ credential_id: req.provider.id });
    if (!seeker || !seeker.isActive) {
      console.log("Seeker not found or inactive for credential_id:", req.provider.id);
      return res.status(403).json({ message: "Seeker inactive or not found" });
    }

    const dataItem = await DataItem.findById(item_id);
    if (!dataItem || !dataItem.isActive) {
      console.log("Data item not found or inactive:", item_id);
      return res.status(404).json({ message: "Item is deleted or inactive" });
    }

    let consent = await Consent.findOne({
      item_id,
      seeker_id: seeker._id,
      provider_id: dataItem.item_owner_id,
    });

    const response = { item_name: dataItem.item_name, item_type: dataItem.item_type };

    const formatDate = (date) => {
      const d = new Date(date);
      return `${String(d.getDate()).padStart(2, "0")}-${String(d.getMonth() + 1).padStart(2, "0")}-${d.getFullYear()}`;
    };

    if (!consent) {
      consent = await Consent.create({
        item_id,
        seeker_id: seeker._id,
        provider_id: dataItem.item_owner_id,
        access_count: 1,
        status: "pending",
      });
      await ConsentHistory.create({
        history_id: uuidv4(),
        consent_id: consent._id,
        changed_by: seeker._id,
        previous_status: null,
        new_status: "pending",
        action_type: "request",
        remarks: "New consent request",
      });
      return res.status(202).json({
        message: "Access request sent",
        consent_status: "pending",
        ...response,
      });
    }

    const previousStatus = consent.status;

    switch (consent.status) {
      case "pending":
        return res.status(202).json({
          message: "Request already pending",
          consent_status: "pending",
          ...response,
        });
      case "rejected":
        return res.status(403).json({
          message: "Request was rejected previously by user",
          consent_status: "rejected",
          ...response,
        });
      case "revoked":
        return res.status(403).json({
          message: "Request was revoked by user",
          consent_status: "revoked",
          ...response,
        });
      case "approved":
        if (new Date() > consent.validity_period) {
          consent.status = "expired";
          await consent.save();
          await ConsentHistory.create({
            history_id: uuidv4(),
            consent_id: consent._id,
            changed_by: "system",
            previous_status: previousStatus,
            new_status: "expired",
            action_type: "expire",
            remarks: "Validity expired",
          });
          return res.status(403).json({
            message: "Validity expired",
            consent_status: "expired",
            ...response,
          });
        }

        if (consent.access_count <= 0) {
          consent.status = "count exhausted";
          await consent.save();
          await ConsentHistory.create({
            history_id: uuidv4(),
            consent_id: consent._id,
            changed_by: "system",
            previous_status: previousStatus,
            new_status: "count exhausted",
            action_type: "expire",
            remarks: "Access count exhausted",
          });
          return res.status(403).json({
            message: "Access count exhausted",
            consent_status: "count exhausted",
            ...response,
          });
        }

        const currentAccessCount = consent.access_count;
        const encryptedDataResponse = {
          message: "Access granted",
          consent_status: "approved",
          encryptedData: dataItem.encryptedData?.toString("base64"),
          encryptedUrl: dataItem.encryptedUrl,
          encryptedAESKeyForSeeker: consent.encryptedAESKeyForSeeker,
          iv: dataItem.iv,
          access_count: currentAccessCount, // Return pre-decrement count
          validity_period: formatDate(consent.validity_period),
          ...response,
        };

        // For text items, decrement access_count here
        if (dataItem.item_type === "text") {
          consent.access_count -= 1;
          if (consent.access_count === 0) {
            consent.status = "count exhausted";
          }
          await consent.save();

          await ConsentHistory.create({
            history_id: uuidv4(),
            consent_id: consent._id,
            changed_by: seeker._id,
            previous_status: previousStatus,
            new_status: consent.status,
            action_type: "access",
            remarks: "Item accessed",
            additional_info: `Remaining Count: ${consent.access_count}`,
          });
        }

        return res.json(encryptedDataResponse);

      case "expired":
        return res.status(403).json({
          message: "Validity expired",
          consent_status: "expired",
          ...response,
        });

      case "count exhausted":
        return res.status(403).json({
          message: "Access count exhausted",
          consent_status: "count exhausted",
          ...response,
        });

      default:
        return res.status(500).json({ message: "Unknown consent status" });
    }
  } catch (error) {
    console.error("Error accessing item:", error.message);
    res.status(500).json({ message: error.message });
  }
};

const requestAccessAgain = async (req, res) => {
  console.log("Request access again received:", req.body);
  const { item_id } = req.body;
  if (req.provider.role !== "seeker") {
    console.log("Unauthorized attempt by:", req.provider.id);
    return res.status(403).json({ message: "Unauthorized" });
  }

  try {
    const seeker = await Seeker.findOne({ credential_id: req.provider.id });
    if (!seeker || !seeker.isActive) {
      console.log("Seeker not found or inactive for credential_id:", req.provider.id);
      return res.status(403).json({ message: "Seeker inactive or not found" });
    }

    const dataItem = await DataItem.findById(item_id);
    if (!dataItem || !dataItem.isActive) {
      console.log("Data item not found or inactive:", item_id);
      return res.status(404).json({ message: "Item is deleted or inactive" });
    }

    let consent = await Consent.findOne({
      item_id,
      seeker_id: seeker._id,
      provider_id: dataItem.item_owner_id,
    });

    if (!consent || !["rejected", "revoked", "expired", "count exhausted"].includes(consent.status)) {
      return res.status(400).json({ message: "No valid consent to re-request" });
    }

    const previousStatus = consent.status;
    consent.status = "pending";
    consent.access_count = 1;
    consent.validity_period = new Date(8640000000000000);
    await consent.save();

    await ConsentHistory.create({
      history_id: uuidv4(),
      consent_id: consent._id,
      changed_by: seeker._id,
      previous_status: previousStatus,
      new_status: "pending",
      action_type: "request",
      remarks: "Re-requested access",
    });

    res.status(202).json({
      message: "Permission request sent",
      consent_status: "pending",
      item_name: dataItem.item_name,
      item_type: dataItem.item_type,
    });
  } catch (error) {
    console.error("Error re-requesting access:", error.message);
    res.status(500).json({ message: error.message });
  }
};

const getSeekerData = async (req, res) => {
  console.log("Get seeker data request for seeker:", req.provider.id);
  if (req.provider.role !== "seeker") {
    console.log("Unauthorized attempt to get seeker data by:", req.provider.id);
    return res.status(403).json({ message: "Unauthorized" });
  }

  try {
    const seeker = await Seeker.findOne({ credential_id: req.provider.id });
    if (!seeker) {
      console.log("Seeker not found for credential_id:", req.provider.id);
      return res.status(404).json({ message: "Seeker not found" });
    }

    res.json({
      name: seeker.name,
      type: seeker.type,
      registration_no: seeker.registration_no,
      email: seeker.email,
      contact_no: seeker.contact_no,
      addressV1: seeker.address,
    });
  } catch (error) {
    console.error("Error fetching seeker data:", error.message);
    res.status(500).json({ message: "Error fetching seeker data" });
  }
};

const fetchFile = async (req, res) => {
  console.log("Fetch file request received:", req.query);
  const { url } = req.query;
  if (!url) {
    console.log("No URL provided in query");
    return res.status(400).json({ message: "URL is required" });
  }
  if (req.provider.role !== "provider") {
    console.log("Unauthorized attempt to fetch file by:", req.provider.id);
    return res.status(403).json({ message: "Unauthorized" });
  }

  try {
    const response = await fetch(url);
    if (!response.ok) throw new Error(`Fetch failed: ${response.status} ${response.statusText}`);

    const arrayBuffer = await response.arrayBuffer();
    const buffer = Buffer.from(arrayBuffer);

    res.set("Content-Type", response.headers.get("Content-Type") || "application/octet-stream");
    res.send(buffer);
    console.log("File fetched and sent successfully");
  } catch (error) {
    console.error("Error fetching file:", error.message);
    res.status(500).json({ message: "Failed to fetch file: " + error.message });
  }
};

const fetchSeekerFile = async (req, res) => {
  console.log("Fetch seeker file request received:", req.query);
  const { url, itemId } = req.query;
  const { seekerId } = req.params;

  if (!url || !itemId) {
    console.log("Missing URL or itemId in query");
    return res.status(400).json({ message: "URL and itemId are required" });
  }
  if (req.provider.role !== "seeker") {
    console.log("Unauthorized attempt to fetch file by:", req.provider.id);
    return res.status(403).json({ message: "Unauthorized" });
  }
  if (req.provider.id !== seekerId) {
    console.log("Seeker ID mismatch:", req.provider.id, seekerId);
    return res.status(403).json({ message: "Unauthorized seeker ID" });
  }

  try {
    const seeker = await Seeker.findOne({ credential_id: req.provider.id });
    if (!seeker || !seeker.isActive) {
      console.log("Seeker not found or inactive for credential_id:", req.provider.id);
      return res.status(403).json({ message: "Seeker inactive or not found" });
    }

    const dataItem = await DataItem.findById(itemId);
    if (!dataItem || !dataItem.isActive) {
      console.log("Data item not found or inactive:", itemId);
      return res.status(404).json({ message: "Item is deleted or inactive" });
    }

    const consent = await Consent.findOne({
      item_id: itemId,
      seeker_id: seeker._id,
      provider_id: dataItem.item_owner_id,
      status: "approved",
    });

    if (!consent) {
      console.log("No approved consent found for item:", itemId, "seeker:", seeker._id);
      return res.status(403).json({ message: "No approved consent found" });
    }

    if (new Date() > consent.validity_period) {
      consent.status = "expired";
      await consent.save();
      await ConsentHistory.create({
        history_id: uuidv4(),
        consent_id: consent._id,
        changed_by: "system",
        previous_status: "approved",
        new_status: "expired",
        action_type: "expire",
        remarks: "Validity expired",
      });
      return res.status(403).json({ message: "Consent expired" });
    }

    if (consent.access_count <= 0) {
      consent.status = "count exhausted";
      await consent.save();
      await ConsentHistory.create({
        history_id: uuidv4(),
        consent_id: consent._id,
        changed_by: "system",
        previous_status: "approved",
        new_status: "count exhausted",
        action_type: "expire",
        remarks: "Access count exhausted",
      });
      return res.status(403).json({ message: "Access count exhausted" });
    }

    const response = await fetch(url);
    if (!response.ok) throw new Error(`Fetch failed: ${response.status} ${response.statusText}`);

    const arrayBuffer = await response.arrayBuffer();
    const buffer = Buffer.from(arrayBuffer);

    // Decrement access_count and update status after successful fetch
    consent.access_count -= 1;
    const previousStatus = consent.status;
    if (consent.access_count === 0) {
      consent.status = "count exhausted";
    }
    await consent.save();

    await ConsentHistory.create({
      history_id: uuidv4(),
      consent_id: consent._id,
      changed_by: seeker._id,
      previous_status: previousStatus,
      new_status: consent.status,
      action_type: "access",
      remarks: "File accessed",
      additional_info: `Remaining Count: ${consent.access_count}`,
    });

    res.set("Content-Type", response.headers.get("Content-Type") || "application/octet-stream");
    res.send(buffer);
    console.log("File fetched and sent successfully for seeker:", seeker._id);
  } catch (error) {
    console.error("Error fetching seeker file:", error.message);
    res.status(500).json({ message: "Failed to fetch file: " + error.message });
  }
};

module.exports = {
  addItem,
  addFileItem,
  getItems,
  getNonTextItems,
  getConsentList,
  giveConsent,
  getUserData,
  editItem,
  deleteItem,
  accessItem,
  getProviderItems,
  requestAccessAgain,
  getSeekerData,
  fetchFile,
  fetchSeekerFile,
};