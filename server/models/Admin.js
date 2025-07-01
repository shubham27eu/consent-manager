const mongoose = require("mongoose");
const { Schema } = mongoose;

const adminSchema = new mongoose.Schema({
  credential_id: { type: Schema.Types.ObjectId, ref: "Credential", required: true },
  first_name: { type: String, required: true, trim: true },
  middle_name: { type: String, trim: true, default: "" },
  last_name: { type: String, required: true, trim: true },
  email: { type: String, required: true, unique: true, trim: true, lowercase: true },
  mobile_no: { type: String, required: true, trim: true },
  date_of_birth: { type: Date },
});

module.exports = mongoose.model("Admin", adminSchema);