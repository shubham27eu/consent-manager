const mongoose = require("mongoose");
const { Schema } = mongoose;
const { generatePasswordHash } = require("../utils/passwordHash");
const logger = require("../utils/logger");

// Schema for provider credentials
const credentialSchema = new Schema({
  username: { type: String, unique: true, required: true },
  password: { type: String, required: true },
  role: { type: String, enum: ["provider", "seeker", "admin"], required: true },
});

// Pre-save hook to hash password only if it's not already hashed
credentialSchema.pre("save", async function (next) {
  if (this.isModified("password")) {
    // Check if password is already a bcrypt hash (starts with $2a$ or $2b$)
    if (!this.password.startsWith("$2a$") && !this.password.startsWith("$2b$")) {
      logger.info(`Hashing password for credential: ${this.username}`);
      try {
        this.password = await generatePasswordHash(this.password);
        logger.info(`Password hashed for credential: ${this.username}`);
      } catch (error) {
        logger.error(`Failed to hash password for ${this.username}: ${error.message}`);
        return next(error);
      }
    } else {
      logger.info(`Password already hashed, skipping hashing for: ${this.username}`);
    }
  }
  next();
});

module.exports = mongoose.model("Credential", credentialSchema);