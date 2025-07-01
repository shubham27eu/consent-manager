const express = require("express");
const { signup, login } = require("../controllers/authController");
const { check, validationResult } = require("express-validator");
const router = express.Router();

// Middleware to handle validation errors
const validate = (req, res, next) => {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return res.status(400).json({ errors: errors.array().map(err => err.msg) });
  }
  next();
};

// Handles new provider/seeker/admin signup, validates input, and adds to backlog if applicable
router.post("/signup", [
  // Common fields
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
  
  // Provider-specific fields
  check("first_name")
    .if((value, { req }) => req.body.role === "provider")
    .matches(/^[A-Za-z]{2,50}$/)
    .withMessage("First name must be 2-50 letters"),
  check("last_name")
    .if((value, { req }) => req.body.role === "provider")
    .matches(/^[A-Za-z]{2,50}$/)
    .withMessage("Last name must be 2-50 letters"),
  check("middle_name")
    .if((value, { req }) => req.body.role === "provider" && value)
    .matches(/^[A-Za-z]{2,50}$/)
    .withMessage("Middle name, if provided, must be 2-50 letters"),
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
  
  // Seeker-specific fields
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
], validate, signup);

// Authenticates provider/seeker/admin login, checks credentials, and returns JWT token if valid
router.post("/login", [
  check("username").notEmpty().withMessage("Username is required"),
  check("password").notEmpty().withMessage("Password is required"),
  check("role").isIn(["provider", "seeker", "admin"]).withMessage("Invalid role"),
], validate, login);

module.exports = router;