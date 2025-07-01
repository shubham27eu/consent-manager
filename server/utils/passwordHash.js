const bcrypt = require("bcryptjs"); // Changed from 'bcrypt' to 'bcryptjs'


// Hash a password
const generatePasswordHash = async (password) => {
  console.log("Generating password hash for:", password); // Log password being hashed
  const salt = await bcrypt.genSalt(10);
  const hash = await bcrypt.hash(password, salt);
  console.log("Generated hash:", hash); // Log resulting hash
  return hash;
};

// Verify a password against a hash
const verifyPassword = async (password, hash) => {
  console.log("Verifying password against hash"); // Log verification attempt
  const isMatch = await bcrypt.compare(password, hash);
  console.log("Password match result:", isMatch); // Log verification result
  return isMatch;
};

module.exports = { generatePasswordHash, verifyPassword };