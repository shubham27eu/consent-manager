const mongoose = require("mongoose");

const dataItemSchema = new mongoose.Schema({
  item_name: { type: String, required: true },
  item_type: { type: String, required: true },
  item_owner_id: { type: mongoose.Schema.Types.ObjectId, ref: "Provider", required: true },
  encryptedData: { type: Buffer },
  encryptedUrl: { type: String },
  encryptedAESKey: { type: String, required: true },
  iv: { type: String, required: true },
  isActive: { type: Boolean, default: true },
  createdAt: { type: Date, default: Date.now }, // Add this field
});

module.exports = mongoose.model("DataItem", dataItemSchema);