const mongoose = require("mongoose");
const { Schema } = mongoose;

// Schema for providers (individuals)
const providerSchema = new Schema({
  credential_id: { type: Schema.Types.ObjectId, ref: "Credential", required: true },
  first_name: { type: String, required: true },
  middle_name: String,
  last_name: { type: String, required: true },
  email: { type: String, required: true, unique: true },
  mobile_no: { type: String, required: true },
  date_of_birth: { type: Date, required: true },
  age: { type: Number, required: true },
  publicKey: { type: String, required: true },
  isActive: { type: Boolean, default: true }, // Added for activation status
});

module.exports = mongoose.model("Provider", providerSchema);