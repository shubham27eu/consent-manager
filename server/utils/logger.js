const winston = require("winston");

// Configure Winston logger for file and console output
const logger = winston.createLogger({
  level: "info",
  format: winston.format.combine(
    winston.format.timestamp(),
    winston.format.json()
  ),
  transports: [
    new winston.transports.File({ filename: "error.log", level: "error" }), // Error logs
    new winston.transports.File({ filename: "combined.log" }), // All logs
    new winston.transports.Console(), // Console output
  ],
});

module.exports = logger;


