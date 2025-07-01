const logger = require("../utils/logger");

// Global error handler middleware
const errorHandler = (err, req, res, next) => {
  const ip = req.ip || req.connection.remoteAddress;
  console.error(`Error occurred from ${ip}:`, err.message);
  logger.error(`${err.message} - ${req.method} ${req.url} - IP: ${ip}\n${err.stack}`);

  // Handle validation errors from express-validator
  if (err.errors && Array.isArray(err.errors)) {
    return res.status(400).json({ errors: err.errors.map((e) => ({ msg: e.msg })) });
  }

  // Standardize all other errors
  res.status(err.status || 500).json({
    errors: [{ msg: err.message || "Internal Server Error" }],
  });
};

module.exports = errorHandler;