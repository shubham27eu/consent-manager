const mongoose = require("mongoose");

const connectDB = async () => {
  console.log("Attempting to connect to MongoDB..."); // Log connection attempt

  try {
    // Set the strictQuery option to true or false as needed
    mongoose.set('strictQuery', true);

    await mongoose.connect(process.env.DB_URL, {
      useNewUrlParser: true,
      useUnifiedTopology: true,
    });
    console.log("MongoDB connected successfully"); // Log successful connection
  } catch (error) {
    console.error("MongoDB connection failed:", error.message); // Log connection error
    process.exit(1);
  }
};

module.exports = connectDB;