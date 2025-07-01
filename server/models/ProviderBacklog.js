const mongoose = require("mongoose");
const { generatePasswordHash } = require("../utils/passwordHash");
const logger = require("../utils/logger");

const providerBacklogSchema = new mongoose.Schema({
  username: { type: String, required: true, unique: true, trim: true },
  password: { type: String, required: true },
  role: { type: String, required: true, enum: ["provider"] },
  first_name: { type: String, required: true, trim: true },
  middle_name: { type: String, trim: true, default: "" },
  last_name: { type: String, required: true, trim: true },
  email: { type: String, required: true, unique: true, trim: true, lowercase: true },
  date_of_birth: { type: Date, required: true },
  mobile_no: { type: String, required: true, trim: true },
  age: { type: Number, required: true },
  gender: { type: String, required: true, enum: ["Male", "Female"] },
  publicKey: { type: String, required: true },
  status: { type: String, enum: ["pending", "rejected", "approved"], default: "pending" },
  created_at: { type: Date, default: Date.now },
});

// Add pre-save hook to hash password
providerBacklogSchema.pre("save", async function (next) {
  if (this.isModified("password")) {
    logger.info(`Hashing password for provider backlog: ${this.username}`);
    this.password = await generatePasswordHash(this.password);
    logger.info(`Password hashed for provider backlog: ${this.username}`);
  }
  next();
});

module.exports = mongoose.model("ProviderBacklog", providerBacklogSchema);