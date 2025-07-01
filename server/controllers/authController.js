const Credential = require("../models/Credential");
const Provider = require("../models/Provider");
const Seeker = require("../models/Seeker");
const Admin = require("../models/Admin");
const ProviderBacklog = require("../models/ProviderBacklog");
const SeekerBacklog = require("../models/SeekerBacklog");
const { generateToken } = require("../utils/jwtToken");
const { generatePasswordHash, verifyPassword } = require("../utils/passwordHash");
const { validationResult } = require("express-validator");
const logger = require("../utils/logger");
const sanitizeHtml = require("sanitize-html");

// Sanitization options
const sanitizeOptions = {
  allowedTags: [],
  allowedAttributes: {},
};

// Calculate age from date_of_birth
const calculateAge = (dob) => {
  const birthDate = new Date(dob);
  const today = new Date();
  if (birthDate > today) {
    throw new Error("Date of birth cannot be in the future");
  }
  let age = today.getFullYear() - birthDate.getFullYear();
  const monthDiff = today.getMonth() - birthDate.getMonth();
  if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())) {
    age--;
  }
  return age;
};

// Handle provider signup
const signup = async (req, res) => {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    logger.info(`Validation errors for signup: ${JSON.stringify(errors.array())}`);
    return res.status(400).json({ errors: errors.array().map(err => err.msg) });
  }

  const { username, password, role, publicKey, ...data } = req.body;

  // Sanitize inputs
  const sanitizedUsername = sanitizeHtml(username, sanitizeOptions);
  const sanitizedRole = sanitizeHtml(role, sanitizeOptions);
  const sanitizedData = {};
  for (const key in data) {
    sanitizedData[key] = sanitizeHtml(data[key], sanitizeOptions);
  }

  try {
    logger.info(`Checking for existing username: ${sanitizedUsername}`);
    const existingCredential = await Credential.findOne({ username: sanitizedUsername });
    if (existingCredential) {
      logger.info(`Username already exists: ${sanitizedUsername}`);
      return res.status(400).json({ message: "Username already exists" });
    }

    if (sanitizedRole === "provider") {
      logger.info(`Checking for existing email in ProviderBacklog: ${sanitizedData.email}`);
      const existingProvider = await ProviderBacklog.findOne({ email: sanitizedData.email });
      if (existingProvider) {
        logger.info(`Email already exists in backlog: ${sanitizedData.email}`);
        return res.status(400).json({ message: "Email already exists" });
      }
      // Calculate age for provider
      const age = calculateAge(sanitizedData.date_of_birth);
      await ProviderBacklog.create({ 
        username: sanitizedUsername, 
        password, 
        role: sanitizedRole, 
        publicKey, 
        age,
        ...sanitizedData 
      }); // Hook hashes password
      logger.info(`Provider added to backlog: ${sanitizedUsername}`);
    } else if (sanitizedRole === "seeker") {
      logger.info(`Checking for existing email in SeekerBacklog: ${sanitizedData.email}`);
      const existingSeeker = await SeekerBacklog.findOne({ email: sanitizedData.email });
      if (existingSeeker) {
        logger.info(`Email already exists in backlog: ${sanitizedData.email}`);
        return res.status(400).json({ message: "Email already exists" });
      }
      await SeekerBacklog.create({ 
        username: sanitizedUsername, 
        password, 
        role: sanitizedRole, 
        publicKey, 
        ...sanitizedData 
      }); // Hook hashes password
      logger.info(`Seeker added to backlog: ${sanitizedUsername}`);
    } else if (sanitizedRole === "admin") {
      const credential = await Credential.create({ 
        username: sanitizedUsername, 
        password, 
        role: sanitizedRole 
      }); // Hook hashes password
      await Admin.create({ credential_id: credential._id, ...sanitizedData });
      logger.info(`Admin created: ${sanitizedUsername}`);
    }
    const response = { message: "Signup successful" };
    if (sanitizedRole !== "admin") {
      response.status = "pending";
    }
    res.status(201).json(response);
    logger.info(`Signup successful for: ${sanitizedUsername}`);
  } catch (error) {
    if (error.code === 11000) {
      logger.info(`Duplicate key error: ${JSON.stringify(error.keyValue)}`);
      const field = Object.keys(error.keyValue)[0];
      return res.status(400).json({ message: `${field.charAt(0).toUpperCase() + field.slice(1)} already exists` });
    }
    logger.error(`Signup error: ${error.message}`);
    if (error.message === "Date of birth cannot be in the future") {
      return res.status(400).json({ message: "Date of birth cannot be in the future" });
    }
    res.status(500).json({ message: "Internal Server Error" });
  }
};

// Handle user login (provider, seeker, or admin)
const login = async (req, res) => {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    logger.info(`Validation errors for login: ${JSON.stringify(errors.array())}`);
    return res.status(400).json({ errors: errors.array().map(err => err.msg) });
  }

  const { username, password, role } = req.body;

  // Sanitize inputs
  const sanitizedUsername = sanitizeHtml(username, sanitizeOptions);
  const sanitizedRole = sanitizeHtml(role, sanitizeOptions);

  try {
    logger.info(`Checking credentials for: ${sanitizedUsername}, Role: ${sanitizedRole}`);
    let credential = await Credential.findOne({ username: sanitizedUsername, role: sanitizedRole });
    let isFromBacklog = false;

    if (!credential) {
      logger.info(`No credential found, checking backlog for: ${sanitizedUsername}`);
      let backlog;
      if (sanitizedRole === "provider") {
        backlog = await ProviderBacklog.findOne({ username: sanitizedUsername });
      } else if (sanitizedRole === "seeker") {
        backlog = await SeekerBacklog.findOne({ username: sanitizedUsername });
      }

      if (backlog) {
        if (backlog.status === "pending") {
          logger.info(`User in backlog, pending approval: ${sanitizedUsername}`);
          return res.status(450).json({ message: "Pending approval" });
        }
        if (backlog.status === "rejected") {
          logger.info(`User in backlog, rejected: ${sanitizedUsername}`);
          return res.status(450).json({ message: "Rejected" });
        }
        // If approved but not in Credential yet (edge case), use backlog credential
        credential = backlog;
        isFromBacklog = true;
      } else {
        logger.info(`User not found: ${sanitizedUsername}`);
        return res.status(404).json({ message: "User not found" });
      }
    }

    const isMatch = await verifyPassword(password, credential.password);
    if (!isMatch) {
      logger.info(`Password mismatch for: ${sanitizedUsername}`);
      return res.status(400).json({ message: "Invalid password" });
    }

    let user;
    if (sanitizedRole === "provider") {
      user = await Provider.findOne({ credential_id: credential._id });
    } else if (sanitizedRole === "seeker") {
      user = await Seeker.findOne({ credential_id: credential._id });
    } else if (sanitizedRole === "admin") {
      user = await Admin.findOne({ credential_id: credential._id });
    }

    // If user is from backlog (not yet approved), skip role-specific check
    if (!user && !isFromBacklog) {
      logger.info(`User not found in role-specific collection: ${sanitizedUsername}`);
      return res.status(404).json({ message: "User not found" });
    }

    if ((sanitizedRole === "provider" || sanitizedRole === "seeker") && user && !user.isActive) {
      logger.info(`User is inactive: ${sanitizedUsername}`);
      return res.status(403).json({ message: "Account is inactive" });
    }

    const token = generateToken({ id: credential._id, role: sanitizedRole });
    res.json({ message: "Login successful", token, userId: credential._id, role: sanitizedRole });
    logger.info(`Login successful for: ${sanitizedUsername}`);
  } catch (error) {
    logger.error(`Login error: ${error.message}`);
    res.status(500).json({ message: "Internal Server Error" });
  }
};

module.exports = { signup, login };