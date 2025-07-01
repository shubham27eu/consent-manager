const jwt = require("jsonwebtoken");
const logger = require("./logger");

const generateToken = (data, options = { expiresIn: "1h" }) => {
  const secret = process.env.JWT_SECRET;
  if (!secret) {
    logger.error("JWT_SECRET is not defined in environment variables");
    throw new Error("JWT_SECRET is not defined");
  }
  const token = jwt.sign(data, secret, options);
  logger.info("Generated JWT token successfully");
  return token;
};

const verifyToken = (token) => {
  const secret = process.env.JWT_SECRET;
  if (!secret) {
    logger.error("JWT_SECRET is not defined in environment variables");
    throw new Error("JWT_SECRET is not defined");
  }
  try {
    const decoded = jwt.verify(token, secret);
    logger.info("Verified JWT token successfully");
    return decoded;
  } catch (error) {
    logger.error(`Failed to verify JWT token: ${error.message}`);
    throw error;
  }
};

module.exports = { generateToken, verifyToken };