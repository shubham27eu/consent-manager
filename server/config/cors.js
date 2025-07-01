const cors = require("cors");

// Configure CORS with allowed origins and methods
module.exports = cors({
  origin: (origin, callback) => {
    const allowedOrigins = process.env.FRONTEND_URL 
      ? [process.env.FRONTEND_URL] 
      : ["http://localhost:3000", "http://192.168.0.107:3000"];
    
    // Allow requests with no origin (e.g., server-to-server) or if origin is in allowed list
    if (!origin || allowedOrigins.includes(origin)) {
      callback(null, true);
    } else {
      callback(new Error("Not allowed by CORS"));
    }
  },
  methods: ["GET", "POST", "PUT", "DELETE", "OPTIONS"], // Added OPTIONS for preflight
  allowedHeaders: ["Content-Type", "Authorization"],
  credentials: true, // Enable credentials
  optionsSuccessStatus: 204, // Handle preflight requests properly
});