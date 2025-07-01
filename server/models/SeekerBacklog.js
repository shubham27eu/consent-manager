const mongoose = require("mongoose");
const { generatePasswordHash } = require("../utils/passwordHash");
const logger = require("../utils/logger");

const seekerBacklogSchema = new mongoose.Schema({
  username: { type: String, required: true, unique: true, trim: true },
  password: { type: String, required: true },
  role: { type: String, required: true, enum: ["seeker"] },
  name: { type: String, required: true, unique: true, trim: true },
  type: { type: String, enum: ["Bank", "Government", "Private Company", "Other"], required: true },
  registration_no: { type: String, required: true, unique: true, trim: true },
  email: { type: String, required: true, unique: true, trim: true, lowercase: true },
  contact_no: { type: String, required: true, trim: true },
  address: { type: String, required: true, trim: true },
  publicKey: { type: String, required: true },
  status: { type: String, enum: ["pending", "rejected", "approved"], default: "pending" },
  created_at: { type: Date, default: Date.now },
});

// Add pre-save hook to hash password
seekerBacklogSchema.pre("save", async function (next) {
  if (this.isModified("password")) {
    logger.info(`Hashing password for seeker backlog: ${this.username}`);
    this.password = await generatePasswordHash(this.password);
    logger.info(`Password hashed for seeker backlog: ${this.username}`);
  }
  next();
});

module.exports = mongoose.model("SeekerBacklog", seekerBacklogSchema);