const mongoose = require("mongoose");
const { Schema } = mongoose;
const { v4: uuidv4 } = require("uuid");

const consentHistorySchema = new Schema({
  history_id: { type: String, default: uuidv4, required: true, unique: true },
  consent_id: { type: Schema.Types.ObjectId, ref: "Consent", required: true },
  changed_by: { type: String, required: true },
  previous_status: {
    type: String,
    enum: ["pending", "rejected", "approved", "count exhausted", "revoked", "expired", null],
  },
  new_status: {
    type: String,
    enum: ["pending", "rejected", "approved", "count exhausted", "revoked", "expired"],
    required: true,
  },
  action_type: {
    type: String,
    enum: ["request", "approve", "access", "revoke", "expire", "reject"], // Added "reject"
    required: true,
  },
  timestamp: { type: Date, default: Date.now, required: true },
  remarks: String,
  additional_info: String,
});

module.exports = mongoose.model("ConsentHistory", consentHistorySchema);