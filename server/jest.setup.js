const dotenv = require("dotenv");

dotenv.config({ path: ".env.test" });

console.log("Loaded environment variables:", {
  JWT_SECRET: process.env.JWT_SECRET,
  PORT: process.env.PORT,
  FRONTEND_URL: process.env.FRONTEND_URL,
});