const mongoose = require("mongoose");
const { Schema } = mongoose;

// Schema for seekers
const seekerSchema = new Schema({
  credential_id: { type: Schema.Types.ObjectId, ref: "Credential", required: true },
  name: { type: String, required: true, unique: true },
  type: { type: String, enum: ["Bank", "Government", "Private Company", "Other"], required: true },
  registration_no: { type: String, required: true, unique: true },
  email: { type: String, required: true, unique: true },
  contact_no: { type: String, required: true },
  address: { type: String, required: true },
  publicKey: { type: String, required: true },
  isActive: { type: Boolean, default: true }, // Added for activation status
});

module.exports = mongoose.model("Seeker", seekerSchema);