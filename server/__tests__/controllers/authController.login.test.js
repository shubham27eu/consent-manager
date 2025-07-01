const request = require('supertest');
const express = require('express');
const mongoose = require('mongoose');
const { MongoMemoryServer } = require('mongodb-memory-server');
const { login } = require('../../controllers/authController');
const Credential = require('../../models/Credential');
const Provider = require('../../models/Provider');
const Seeker = require('../../models/Seeker');
const Admin = require('../../models/Admin');
const { check, validationResult } = require('express-validator');
const sanitizeHtml = require('sanitize-html');
const { verifyPassword } = require('../../utils/passwordHash');
const { generateToken } = require('../../utils/jwtToken');
const logger = require('../../utils/logger');

// Mock dependencies
jest.mock('../../models/Credential');
jest.mock('../../models/Provider');
jest.mock('../../models/Seeker');
jest.mock('../../models/Admin');
jest.mock('sanitize-html');
jest.mock('../../utils/passwordHash');
jest.mock('../../utils/jwtToken');
jest.mock('../../utils/logger');

describe('Auth Controller - Login', () => {
  let app;

  beforeAll(() => {
    // Create Express app for testing
    app = express();
    app.use(express.json());

    // Validation middleware
    const validateLogin = [
      check('username').notEmpty().withMessage('Username is required'),
      check('password').notEmpty().withMessage('Password is required'),
      check('role').isIn(['provider', 'seeker', 'admin']).withMessage('Role must be provider, seeker, or admin'),
    ];

    // Middleware to handle validation errors
    const validate = (req, res, next) => {
      const errors = validationResult(req);
      if (!errors.isEmpty()) {
        return res.status(400).json({ errors: errors.array().map(err => err.msg) });
      }
      next();
    };

    // Define login route
    app.post('/api/auth/login', validateLogin, validate, login);
  });

  beforeEach(() => {
    // Reset mocks before each test
    jest.clearAllMocks();

    // Mock utility functions
    sanitizeHtml.mockImplementation((input) => input);
    verifyPassword.mockResolvedValue(true);
    generateToken.mockReturnValue('mocked-token');
    logger.info.mockImplementation(() => {});
    logger.error.mockImplementation(() => {});
  });

  // Test Case 1: Successful provider login
  it('should successfully log in a provider', async () => {
    // Setup
    const loginData = {
      username: 'testprovider',
      password: 'Password123!',
      role: 'provider',
    };

    const credential = {
      _id: 'cred-id',
      username: 'testprovider',
      password: 'hashedpassword',
      role: 'provider',
    };

    const provider = {
      _id: 'provider-id',
      first_name: 'John',
      last_name: 'Doe',
      isActive: true,
    };

    Credential.findOne.mockResolvedValue(credential);
    Provider.findOne.mockResolvedValue(provider);

    // Execute
    const response = await request(app)
      .post('/api/auth/login')
      .send(loginData)
      .expect(200);

    // Verify
    expect(Credential.findOne).toHaveBeenCalledWith({
      username: 'testprovider',
      role: 'provider',
    });
    expect(verifyPassword).toHaveBeenCalledWith('Password123!', 'hashedpassword');
    expect(Provider.findOne).toHaveBeenCalledWith({ credential_id: 'cred-id' });
    expect(generateToken).toHaveBeenCalledWith({
      id: 'cred-id',
      role: 'provider',
    });
    expect(response.body).toEqual({
      message: 'Login successful',
      token: 'mocked-token',
      userId: 'cred-id',
      role: 'provider',
    });
  });

  // Test Case 2: Successful seeker login
  it('should successfully log in a seeker', async () => {
    // Setup
    const loginData = {
      username: 'testseeker',
      password: 'Password123!',
      role: 'seeker',
    };

    const credential = {
      _id: 'cred-id',
      username: 'testseeker',
      password: 'hashedpassword',
      role: 'seeker',
    };

    const seeker = {
      _id: 'seeker-id',
      name: 'Test Bank',
      isActive: true,
    };

    Credential.findOne.mockResolvedValue(credential);
    Seeker.findOne.mockResolvedValue(seeker);

    // Execute
    const response = await request(app)
      .post('/api/auth/login')
      .send(loginData)
      .expect(200);

    // Verify
    expect(Credential.findOne).toHaveBeenCalledWith({
      username: 'testseeker',
      role: 'seeker',
    });
    expect(verifyPassword).toHaveBeenCalledWith('Password123!', 'hashedpassword');
    expect(Seeker.findOne).toHaveBeenCalledWith({ credential_id: 'cred-id' });
    expect(generateToken).toHaveBeenCalledWith({
      id: 'cred-id',
      role: 'seeker',
    });
    expect(response.body).toEqual({
      message: 'Login successful',
      token: 'mocked-token',
      userId: 'cred-id',
      role: 'seeker',
    });
  });

  // Test Case 3: Successful admin login
  it('should successfully log in an admin', async () => {
    // Setup
    const loginData = {
      username: 'testadmin',
      password: 'Password123!',
      role: 'admin',
    };

    const credential = {
      _id: 'cred-id',
      username: 'testadmin',
      password: 'hashedpassword',
      role: 'admin',
    };

    const admin = {
      _id: 'admin-id',
      first_name: 'Admin',
      last_name: 'User',
    };

    Credential.findOne.mockResolvedValue(credential);
    Admin.findOne.mockResolvedValue(admin);

    // Execute
    const response = await request(app)
      .post('/api/auth/login')
      .send(loginData)
      .expect(200);

    // Verify
    expect(Credential.findOne).toHaveBeenCalledWith({
      username: 'testadmin',
      role: 'admin',
    });
    expect(verifyPassword).toHaveBeenCalledWith('Password123!', 'hashedpassword');
    expect(Admin.findOne).toHaveBeenCalledWith({ credential_id: 'cred-id' });
    expect(generateToken).toHaveBeenCalledWith({
      id: 'cred-id',
      role: 'admin',
    });
    expect(response.body).toEqual({
      message: 'Login successful',
      token: 'mocked-token',
      userId: 'cred-id',
      role: 'admin',
    });
  });

  // Test Case 4: Invalid credentials
  it('should return 401 for invalid credentials', async () => {
    // Setup
    const loginData = {
      username: 'testuser',
      password: 'WrongPassword',
      role: 'provider',
    };

    const credential = {
      _id: 'cred-id',
      username: 'testuser',
      password: 'hashedpassword',
      role: 'provider',
    };

    Credential.findOne.mockResolvedValue(credential);
    verifyPassword.mockResolvedValue(false); // Password doesn't match

    // Execute
    const response = await request(app)
      .post('/api/auth/login')
      .send(loginData)
      .expect(400);

    // Verify
    expect(Credential.findOne).toHaveBeenCalledWith({
      username: 'testuser',
      role: 'provider',
    });
    expect(verifyPassword).toHaveBeenCalledWith('WrongPassword', 'hashedpassword');
    expect(response.body).toEqual({ message: 'Invalid password' });
  });

  // Test Case 5: User not found
  it('should return 401 when user is not found', async () => {
    // Setup
    const loginData = {
      username: 'nonexistent',
      password: 'Password123!',
      role: 'provider',
    };

    Credential.findOne.mockResolvedValue(null);

    // Execute
    const response = await request(app)
      .post('/api/auth/login')
      .send(loginData)
      .expect(500);

    // Verify
    expect(Credential.findOne).toHaveBeenCalledWith({
      username: 'nonexistent',
      role: 'provider',
    });
    expect(response.body).toEqual({ message: 'Internal Server Error' });
  });

  // Test Case 6: Inactive user
  it('should return 403 for inactive provider', async () => {
    // Setup
    const loginData = {
      username: 'inactiveprovider',
      password: 'Password123!',
      role: 'provider',
    };

    const credential = {
      _id: 'cred-id',
      username: 'inactiveprovider',
      password: 'hashedpassword',
      role: 'provider',
    };

    const provider = {
      _id: 'provider-id',
      first_name: 'Inactive',
      last_name: 'User',
      isActive: false, // Inactive user
    };

    Credential.findOne.mockResolvedValue(credential);
    verifyPassword.mockResolvedValue(true);
    Provider.findOne.mockResolvedValue(provider);

    // Execute
    const response = await request(app)
      .post('/api/auth/login')
      .send(loginData)
      .expect(403);

    // Verify
    expect(Provider.findOne).toHaveBeenCalledWith({ credential_id: 'cred-id' });
    expect(response.body).toEqual({ message: 'Account is inactive' });
  });

  // Test Case 7: Validation error
  it('should return validation error for missing fields', async () => {
    // Setup
    const loginData = {
      username: 'testuser',
      // Missing password
      role: 'provider',
    };

    // Execute
    const response = await request(app)
      .post('/api/auth/login')
      .send(loginData)
      .expect(400);

    // Verify
    expect(response.body.errors).toContain('Password is required');
  });

  // Test Case 8: Invalid role
  it('should return validation error for invalid role', async () => {
    // Setup
    const loginData = {
      username: 'testuser',
      password: 'Password123!',
      role: 'invalid', // Invalid role
    };

    // Execute
    const response = await request(app)
      .post('/api/auth/login')
      .send(loginData)
      .expect(400);

    // Verify
    expect(response.body.errors).toContain('Role must be provider, seeker, or admin');
  });
});
