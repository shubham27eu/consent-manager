const mongoose = require("mongoose");
const { Schema } = mongoose;

// Schema for consent records
const consentSchema = new Schema({
  item_id: { type: Schema.Types.ObjectId, ref: "DataItem", required: true },
  seeker_id: { type: Schema.Types.ObjectId, ref: "Seeker", required: true },
  provider_id: { type: Schema.Types.ObjectId, ref: "Provider", required: true },
  validity_period: { type: Date, default: () => new Date(8640000000000000) },
  access_count: { type: Number, default: 1 },
  status: {
    type: String,
    enum: ["pending", "rejected", "approved", "count exhausted", "revoked", "expired"],
    default: "pending",
  },
  encryptedAESKeyForSeeker: { type: String },
  date_created: { type: Date, default: Date.now },
  isActive: { type: Boolean, default: true }, // Added to sync with provider status
});

module.exports = mongoose.model("Consent", consentSchema);