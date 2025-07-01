const ProviderBacklog = require("../models/ProviderBacklog");
const SeekerBacklog = require("../models/SeekerBacklog");
const Credential = require("../models/Credential");
const Provider = require("../models/Provider");
const Seeker = require("../models/Seeker");
const DataItem = require("../models/DataItem");
const Consent = require("../models/Consent");

// Get pending provider signup requests
const getProviderBackLog = async (req, res) => {
  console.log("Fetching provider backlog for admin:", req.provider.id);
  if (req.provider.role !== "admin") {
    console.log("Unauthorized attempt by:", req.provider.id);
    return res.status(403).json({ message: "Unauthorized" });
  }

  try {
    const pendingBacklogs = await ProviderBacklog.find({ status: "pending" })
      .sort({ created_at: 1 })
      .select("-password");
    console.log("Pending provider backlogs retrieved:", pendingBacklogs.length);
    res.status(200).json({ data: pendingBacklogs });
  } catch (error) {
    console.error("Error fetching provider backlog:", error.message);
    res.status(500).json({ message: "Server error" });
  }
};

// Get pending seeker signup requests
const getSeekerBackLog = async (req, res) => {
  console.log("Fetching seeker backlog for admin:", req.provider.id);
  if (req.provider.role !== "admin") {
    console.log("Unauthorized attempt by:", req.provider.id);
    return res.status(403).json({ message: "Unauthorized" });
  }

  try {
    const pendingBacklogs = await SeekerBacklog.find({ status: "pending" })
      .sort({ created_at: 1 })
      .select("-password");
    console.log("Pending seeker backlogs retrieved:", pendingBacklogs.length);
    res.status(200).json({ data: pendingBacklogs });
  } catch (error) {
    console.error("Error fetching seeker backlog:", error.message);
    res.status(500).json({ message: "Server error" });
  }
};

// Approve or reject a provider
const approveProvider = async (req, res) => {
  console.log("Approve provider request received:", req.body);
  const { providerId, action } = req.body;
  if (req.provider.role !== "admin") {
    console.log("Unauthorized attempt by:", req.provider.id);
    return res.status(403).json({ message: "Unauthorized" });
  }

  try {
    console.log("Fetching provider backlog:", providerId);
    const backlog = await ProviderBacklog.findById(providerId);
    if (!backlog) {
      console.log("Provider not found in backlog:", providerId);
      return res.status(404).json({ message: "Provider not found" });
    }

    if (action === "approve") {
      // Create credential without re-hashing the password
      const credential = new Credential({
        username: backlog.username,
        password: backlog.password, // Already hashed from backlog
        role: "provider",
      });
      await credential.save({ validateBeforeSave: false }); // Skip pre-save hook

      await Provider.create({
        credential_id: credential._id,
        first_name: backlog.first_name,
        middle_name: backlog.middle_name,
        last_name: backlog.last_name,
        email: backlog.email,
        mobile_no: backlog.mobile_no,
        date_of_birth: backlog.date_of_birth,
        age: backlog.age,
        publicKey: backlog.publicKey,
        isActive: true,
      });
      backlog.status = "approved";
      console.log("Provider approved and moved to Provider collection:", providerId);
    } else if (action === "reject") {
      backlog.status = "rejected";
      console.log("Provider rejected:", providerId);
    }
    await backlog.save();
    res.json({ message: `Provider ${action}ed` });
    console.log("Response sent for provider:", providerId, "Action:", action);
  } catch (error) {
    console.error("Error approving provider:", error.message);
    res.status(500).json({ message: error.message });
  }
};

// Approve or reject a seeker
const approveSeeker = async (req, res) => {
  console.log("Approve seeker request received:", req.body);
  const { seekerId, action } = req.body;
  if (req.provider.role !== "admin") {
    console.log("Unauthorized attempt by:", req.provider.id);
    return res.status(403).json({ message: "Unauthorized" });
  }

  try {
    console.log("Fetching seeker backlog:", seekerId);
    const backlog = await SeekerBacklog.findById(seekerId);
    if (!backlog) {
      console.log("Seeker not found in backlog:", seekerId);
      return res.status(404).json({ message: "Seeker not found" });
    }

    if (action === "approve") {
      // Create credential without re-hashing the password
      const credential = new Credential({
        username: backlog.username,
        password: backlog.password, // Already hashed from backlog
        role: "seeker",
      });
      await credential.save({ validateBeforeSave: false }); // Skip pre-save hook

      await Seeker.create({
        credential_id: credential._id,
        name: backlog.name,
        type: backlog.type,
        registration_no: backlog.registration_no,
        email: backlog.email,
        contact_no: backlog.contact_no,
        address: backlog.address,
        publicKey: backlog.publicKey,
        isActive: true,
      });
      backlog.status = "approved";
      console.log("Seeker approved and moved to Seeker collection:", seekerId);
    } else if (action === "reject") {
      backlog.status = "rejected";
      console.log("Seeker rejected:", seekerId);
    }
    await backlog.save();
    res.json({ message: `Seeker ${action}ed` });
    console.log("Response sent for seeker:", seekerId, "Action:", action);
  } catch (error) {
    console.error("Error approving seeker:", error.message);
    res.status(500).json({ message: error.message });
  }
};

// Inactivate a provider by userId
const inactivateProvider = async (req, res) => {
  console.log("Inactivate provider request received:", req.body);
  const { userId } = req.body;
  if (req.provider.role !== "admin") {
    console.log("Unauthorized attempt by:", req.provider.id);
    return res.status(403).json({ message: "Unauthorized" });
  }

  try {
    console.log("Fetching provider by userId:", userId);
    const provider = await Provider.findById(userId);
    if (!provider) {
      console.log("Provider not found:", userId);
      return res.status(404).json({ message: "Provider not found" });
    }

    provider.isActive = false;
    await provider.save();
    console.log("Provider inactivated:", userId);

    await DataItem.updateMany({ item_owner_id: provider._id }, { isActive: false });
    console.log("Provider's data items inactivated for:", userId);

    await Consent.updateMany({ provider_id: provider._id }, { isActive: false });
    console.log("Provider's consents inactivated for:", userId);

    res.json({ message: "Provider and related data inactivated" });
    console.log("Response sent for provider inactivation:", userId);
  } catch (error) {
    console.error("Error inactivating provider:", error.message);
    res.status(500).json({ message: error.message });
  }
};

// Inactivate a seeker by userId
const inactivateSeeker = async (req, res) => {
  console.log("Inactivate seeker request received:", req.body);
  const { userId } = req.body;
  if (req.provider.role !== "admin") {
    console.log("Unauthorized attempt by:", req.provider.id);
    return res.status(403).json({ message: "Unauthorized" });
  }

  try {
    console.log("Fetching seeker by userId:", userId);
    const seeker = await Seeker.findById(userId);
    if (!seeker) {
      console.log("Seeker not found:", userId);
      return res.status(404).json({ message: "Seeker not found" });
    }

    seeker.isActive = false;
    await seeker.save();
    console.log("Seeker inactivated:", userId);

    res.json({ message: "Seeker inactivated" });
    console.log("Response sent for seeker inactivation:", userId);
  } catch (error) {
    console.error("Error inactivating seeker:", error.message);
    res.status(500).json({ message: error.message });
  }
};

// Get all inactive users (providers and seekers)
const getInactiveUsers = async (req, res) => {
  console.log("Fetching inactive users for admin:", req.provider.id);
  if (req.provider.role !== "admin") {
    console.log("Unauthorized attempt by:", req.provider.id);
    return res.status(403).json({ message: "Unauthorized" });
  }

  try {
    const inactiveProviders = await Provider.find({ isActive: false }).populate("credential_id", "username");
    const inactiveSeekers = await Seeker.find({ isActive: false }).populate("credential_id", "username");

    const inactiveUsers = [
      ...inactiveProviders.map(provider => ({
        _id: provider._id, // Include _id for frontend use
        username: provider.credential_id.username,
        role: "provider",
        email: provider.email,
        first_name: provider.first_name,
        last_name: provider.last_name,
        isActive: provider.isActive,
      })),
      ...inactiveSeekers.map(seeker => ({
        _id: seeker._id, // Include _id for frontend use
        username: seeker.credential_id.username,
        role: "seeker",
        email: seeker.email,
        name: seeker.name,
        type: seeker.type,
        isActive: seeker.isActive,
      })),
    ];

    console.log("Inactive users retrieved:", inactiveUsers.length);
    res.status(200).json({ data: inactiveUsers });
  } catch (error) {
    console.error("Error fetching inactive users:", error.message);
    res.status(500).json({ message: "Server error" });
  }
};

// Reactivate a provider or seeker by userId
const reactivate = async (req, res) => {
  console.log("Reactivate request received:", req.body);
  const { userId } = req.body;
  if (req.provider.role !== "admin") {
    console.log("Unauthorized attempt by:", req.provider.id);
    return res.status(403).json({ message: "Unauthorized" });
  }

  try {
    console.log("Fetching provider/seeker by userId:", userId);
    const provider = await Provider.findById(userId);
    const seeker = await Seeker.findById(userId);

    if (provider) {
      provider.isActive = true;
      await provider.save();
      await DataItem.updateMany({ item_owner_id: provider._id }, { isActive: true });
      await Consent.updateMany({ provider_id: provider._id }, { isActive: true });
      console.log("Provider reactivated:", userId);
      res.json({ message: "Provider reactivated" });
    } else if (seeker) {
      seeker.isActive = true;
      await seeker.save();
      console.log("Seeker reactivated:", userId);
      res.json({ message: "Seeker reactivated" });
    } else {
      console.log("User not found for reactivation:", userId);
      return res.status(404).json({ message: "Provider/Seeker not found" });
    }
    console.log("Response sent for reactivation of:", userId);
  } catch (error) {
    console.error("Error reactivating:", error.message);
    res.status(500).json({ message: error.message });
  }
};

// Get all active providers
const getProviders = async (req, res) => {
  console.log("Fetching active providers for admin:", req.provider.id);
  if (req.provider.role !== "admin") {
    console.log("Unauthorized attempt by:", req.provider.id);
    return res.status(403).json({ message: "Unauthorized" });
  }

  try {
    const activeProviders = await Provider.find({ isActive: true })
      .populate("credential_id", "username")
      .select("first_name last_name email mobile_no");
    console.log("Active providers retrieved:", activeProviders.length);
    res.status(200).json({ data: activeProviders });
  } catch (error) {
    console.error("Error fetching active providers:", error.message);
    res.status(500).json({ message: "Server error" });
  }
};

// Get all active seekers
const getSeekers = async (req, res) => {
  console.log("Fetching active seekers for admin:", req.provider.id);
  if (req.provider.role !== "admin") {
    console.log("Unauthorized attempt by:", req.provider.id);
    return res.status(403).json({ message: "Unauthorized" });
  }

  try {
    const activeSeekers = await Seeker.find({ isActive: true })
      .populate("credential_id", "username")
      .select("name email contact_no type");
    console.log("Active seekers retrieved:", activeSeekers.length);
    res.status(200).json({ data: activeSeekers });
  } catch (error) {
    console.error("Error fetching active seekers:", error.message);
    res.status(500).json({ message: "Server error" });
  }
};

module.exports = {
  getProviderBackLog,
  getSeekerBackLog,
  approveProvider,
  approveSeeker,
  inactivateProvider,
  inactivateSeeker,
  getInactiveUsers,
  reactivate,
  getProviders,
  getSeekers,
};