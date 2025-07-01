const request = require("supertest");
const express = require("express");
const mongoose = require("mongoose");
const { MongoMemoryServer } = require("mongodb-memory-server");
const { signup } = require("../../controllers/authController");
const Credential = require("../../models/Credential");
const ProviderBacklog = require("../../models/ProviderBacklog");
const SeekerBacklog = require("../../models/SeekerBacklog");
const Admin = require("../../models/Admin");
const { check, validationResult } = require("express-validator");
const sanitizeHtml = require("sanitize-html");
const { generatePasswordHash } = require("../../utils/passwordHash");
const logger = require("../../utils/logger");

// Mock dependencies
jest.mock("../../utils/passwordHash");
jest.mock("../../utils/logger");
jest.mock("sanitize-html");

// Set up Express app for testing
const app = express();
app.use(express.json());

// Validation middleware
const validateSignup = [
  check("username")
    .matches(/^[A-z][A-z0-9-_]{3,23}$/)
    .withMessage("Username must be 4-24 characters, start with a letter, and contain only letters, numbers, underscores, or hyphens"),
  check("password")
    .matches(/^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%]).{8,24}$/)
    .withMessage("Password must be 8-24 characters, with uppercase, lowercase, number, and special character (!@#$%)"),
  check("role")
    .isIn(["provider", "seeker", "admin"])
    .withMessage("Role must be provider, seeker, or admin"),
  check("publicKey")
    .if((value, { req }) => req.body.role !== "admin")
    .notEmpty()
    .withMessage("Public key is required for provider and seeker"),
  check("email")
    .isEmail()
    .withMessage("Must be a valid email address"),
  check("first_name")
    .if((value, { req }) => req.body.role === "provider")
    .matches(/^[A-Za-z]{2,50}$/)
    .withMessage("First name must be 2-50 letters"),
  check("last_name")
    .if((value, { req }) => req.body.role === "provider")
    .matches(/^[A-Za-z]{2,50}$/)
    .withMessage("Last name must be 2-50 letters"),
  check("mobile_no")
    .if((value, { req }) => req.body.role === "provider")
    .matches(/^\d{10}$/)
    .withMessage("Mobile number must be exactly 10 digits"),
  check("date_of_birth")
    .if((value, { req }) => req.body.role === "provider")
    .isDate()
    .withMessage("Date of birth must be a valid date"),
  check("gender")
    .if((value, { req }) => req.body.role === "provider")
    .isIn(["Male", "Female"])
    .withMessage("Gender must be Male or Female"),
  check("name")
    .if((value, { req }) => req.body.role === "seeker")
    .matches(/^[A-Za-z\s]{2,100}$/)
    .withMessage("Name must be 2-100 characters, letters and spaces only"),
  check("contact_no")
    .if((value, { req }) => req.body.role === "seeker")
    .matches(/^\d{10}$/)
    .withMessage("Contact number must be exactly 10 digits"),
  check("address")
    .if((value, { req }) => req.body.role === "seeker")
    .matches(/^.{5,200}$/)
    .withMessage("Address must be 5-200 characters"),
  check("registration_no")
    .if((value, { req }) => req.body.role === "seeker")
    .matches(/^[A-Za-z0-9]{5,50}$/)
    .withMessage("Registration number must be 5-50 alphanumeric characters"),
  check("type")
    .if((value, { req }) => req.body.role === "seeker")
    .isIn(["Bank", "Government", "Private Company", "Other"])
    .withMessage("Type must be Bank, Government, Private Company, or Other"),
];

// Middleware to handle validation errors
const validate = (req, res, next) => {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return res.status(400).json({ errors: errors.array().map(err => err.msg) });
  }
  next();
};

// Define signup route
app.post("/api/auth/signup", validateSignup, validate, signup);

describe("POST /api/auth/signup", () => {
  let mongoServer;

  beforeAll(async () => {
    // Set Mongoose strictQuery option to suppress deprecation warning
    mongoose.set("strictQuery", true);

    // Debug environment variables
    console.log("JWT_SECRET in tests:", process.env.JWT_SECRET);

    // Start MongoDB Memory Server manually
    try {
      mongoServer = await MongoMemoryServer.create();
      const uri = mongoServer.getUri();
      console.log("Manually started MongoDB Memory Server:", uri);
      await mongoose.connect(uri);
    } catch (error) {
      console.error("Failed to start MongoDB Memory Server:", error);
      throw error;
    }
  }, 30000);

  afterEach(async () => {
    // Clear all mocks and database collections
    jest.clearAllMocks();
    if (mongoose.connection.readyState !== 0) {
      await mongoose.connection.db.dropDatabase();
    }
  });

  afterAll(async () => {
    // Disconnect from MongoDB Memory Server
    if (mongoose.connection.readyState !== 0) {
      await mongoose.disconnect();
    }
    if (mongoServer) {
      await mongoServer.stop();
    }
  });

  // Mock utility functions
  beforeEach(() => {
    generatePasswordHash.mockResolvedValue("hashedPassword");
    sanitizeHtml.mockImplementation((input) => input);
    logger.info.mockImplementation(() => {});
    logger.error.mockImplementation(() => {});
  });

  // Test Case 1: Successful provider signup
  it("should successfully sign up a provider", async () => {
    const providerData = {
      username: "testprovider",
      password: "Password123!",
      role: "provider",
      publicKey: "publicKey123",
      email: "provider@test.com",
      first_name: "John",
      last_name: "Doe",
      mobile_no: "1234567890",
      date_of_birth: "1990-01-01",
      gender: "Male",
    };

    const response = await request(app)
      .post("/api/auth/signup")
      .send(providerData)
      .expect(201);

    expect(response.body).toEqual({
      message: "Signup successful",
      status: "pending",
    });

    const providerBacklog = await ProviderBacklog.findOne({ username: "testprovider" });
    expect(providerBacklog).toBeTruthy();
    expect(providerBacklog.email).toBe("provider@test.com");
    expect(providerBacklog.password).toBe("hashedPassword");
    expect(providerBacklog.age).toBe(35); // Based on 2025 - 1990
  });

  // Test Case 2: Successful seeker signup
  it("should successfully sign up a seeker", async () => {
    const seekerData = {
      username: "testseeker",
      password: "Password123!",
      role: "seeker",
      publicKey: "publicKey123",
      email: "seeker@test.com",
      name: "Test Seeker",
      contact_no: "1234567890",
      address: "123 Test Street",
      registration_no: "REG12345",
      type: "Bank",
    };

    const response = await request(app)
      .post("/api/auth/signup")
      .send(seekerData)
      .expect(201);

    expect(response.body).toEqual({
      message: "Signup successful",
      status: "pending",
    });

    const seekerBacklog = await SeekerBacklog.findOne({ username: "testseeker" });
    expect(seekerBacklog).toBeTruthy();
    expect(seekerBacklog.email).toBe("seeker@test.com");
    expect(seekerBacklog.password).toBe("hashedPassword");
  });

  // Test Case 3: Successful admin signup
  it("should successfully sign up an admin", async () => {
    const adminData = {
      username: "testadmin",
      password: "Password123!",
      role: "admin",
      email: "admin@test.com",
      first_name: "Jane",
      last_name: "Smith",
      mobile_no: "1234567890",
    };

    const response = await request(app)
      .post("/api/auth/signup")
      .send(adminData)
      .expect(201);

    expect(response.body).toEqual({
      message: "Signup successful",
    });

    const admin = await Admin.findOne({ email: "admin@test.com" });
    expect(admin).toBeTruthy();
    const credential = await Credential.findOne({ username: "testadmin" });
    expect(credential).toBeTruthy();
    expect(credential.password).toBe("hashedPassword");
  });

  // Test Case 4: Validation error for invalid username
  it("should return validation error for invalid username", async () => {
    const invalidData = {
      username: "123invalid", // Starts with number
      password: "Password123!",
      role: "provider",
      publicKey: "publicKey123",
      email: "provider@test.com",
      first_name: "John",
      last_name: "Doe",
      mobile_no: "1234567890",
      date_of_birth: "1990-01-01",
      gender: "Male",
    };

    const response = await request(app)
      .post("/api/auth/signup")
      .send(invalidData)
      .expect(400);

    expect(response.body.errors).toContain(
      "Username must be 4-24 characters, start with a letter, and contain only letters, numbers, underscores, or hyphens"
    );
  });

  // Test Case 5: Duplicate username
  it("should return error for duplicate username", async () => {
    // Create an existing credential
    await Credential.create({
      username: "testprovider",
      password: "hashedPassword",
      role: "provider",
    });

    const providerData = {
      username: "testprovider",
      password: "Password123!",
      role: "provider",
      publicKey: "publicKey123",
      email: "provider@test.com",
      first_name: "John",
      last_name: "Doe",
      mobile_no: "1234567890",
      date_of_birth: "1990-01-01",
      gender: "Male",
    };

    const response = await request(app)
      .post("/api/auth/signup")
      .send(providerData)
      .expect(400);

    expect(response.body.message).toBe("Username already exists");
  });

  // Test Case 6: Duplicate email for provider
  it("should return error for duplicate email in provider backlog", async () => {
    // Create an existing provider backlog entry
    await ProviderBacklog.create({
      username: "existingprovider",
      password: "hashedPassword",
      role: "provider",
      email: "provider@test.com",
      first_name: "Existing",
      last_name: "User",
      mobile_no: "1234567890",
      date_of_birth: new Date("1990-01-01"),
      gender: "Male",
      publicKey: "publicKey123",
      age: 35,
    });

    const providerData = {
      username: "testprovider",
      password: "Password123!",
      role: "provider",
      publicKey: "publicKey123",
      email: "provider@test.com",
      first_name: "John",
      last_name: "Doe",
      mobile_no: "1234567890",
      date_of_birth: "1990-01-01",
      gender: "Male",
    };

    const response = await request(app)
      .post("/api/auth/signup")
      .send(providerData)
      .expect(400);

    expect(response.body.message).toBe("Email already exists");
  });

  // Test Case 7: Invalid date of birth (future date)
  it("should return error for future date of birth", async () => {
    const providerData = {
      username: "testprovider",
      password: "Password123!",
      role: "provider",
      publicKey: "publicKey123",
      email: "provider@test.com",
      first_name: "John",
      last_name: "Doe",
      mobile_no: "1234567890",
      date_of_birth: "2026-01-01", // Future date
      gender: "Male",
    };

    const response = await request(app)
      .post("/api/auth/signup")
      .send(providerData)
      .expect(400);

    expect(response.body.message).toBe("Date of birth cannot be in the future");
  });

  // Test Case 8: Invalid role
  it("should return validation error for invalid role", async () => {
    const invalidData = {
      username: "testuser",
      password: "Password123!",
      role: "invalid", // Invalid role
      publicKey: "publicKey123",
      email: "test@test.com",
    };

    const response = await request(app)
      .post("/api/auth/signup")
      .send(invalidData)
      .expect(400);

    expect(response.body.errors).toContain("Role must be provider, seeker, or admin");
  });
});