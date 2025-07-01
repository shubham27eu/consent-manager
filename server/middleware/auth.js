const { verifyToken } = require("../utils/jwtToken");

// Middleware to authenticate requests using JWT
const auth = (req, res, next) => {
  const token = req.header("Authorization")?.replace("Bearer ", "");
  console.log("Checking token:", token); // Log the token being checked
  if (!token) {
    console.log("No token provided in request");
    return res.status(401).json({ message: "Access denied. No token provided." });
  }

  try {
    const decoded = verifyToken(token);
    console.log("Token decoded successfully:", decoded); // Log decoded token payload
    req.provider = decoded; // Changed "req.user" to "req.provider"
    next();
  } catch (error) {
    console.error("Token verification failed:", error.message); // Log token error
    res.status(401).json({ message: "Invalid token" });
  }
};

module.exports = auth;