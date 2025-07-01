require("dotenv").config(); // Must be the first line
console.log("JWT_SECRET from .env in server.js:", process.env.JWT_SECRET); // Debug log
const express = require("express");
const connectDB = require("./config/db");
const cors = require("./config/cors");
const authRoutes = require("./routes/authRoutes");
const providerRoutes = require("./routes/providerRoutes");
const seekerRoutes = require("./routes/seekerRoutes");
const adminRoutes = require("./routes/adminRoutes");
const errorHandler = require("./middleware/error");

// Initialize Express app
const app = express();

console.log("Configuring middleware...");
app.use(cors);
app.use(express.json());

console.log("Setting up routes...");
app.use("/api/auth", authRoutes);
app.use("/provider", providerRoutes);
app.use("/seeker", seekerRoutes);
app.use("/admin", adminRoutes);

app.use(errorHandler);

const PORT = process.env.PORT || 4000;

async function startServer() {
  await connectDB();
  app.listen(PORT, () => console.log(`Server started on port ${PORT}`));
}
startServer().catch((err) => console.error("Server startup error:", err));