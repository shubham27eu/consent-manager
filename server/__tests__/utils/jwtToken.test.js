const jwt = require('jsonwebtoken');
const { generateToken, verifyToken } = require('../../utils/jwtToken');
const logger = require('../../utils/logger');

// Mock dependencies
jest.mock('jsonwebtoken');
jest.mock('../../utils/logger');

describe('JWT Token Utilities', () => {
  // Save original environment and restore after tests
  const originalEnv = process.env;

  beforeEach(() => {
    // Reset mocks before each test
    jest.clearAllMocks();
    
    // Setup environment variables for testing
    process.env.JWT_SECRET = 'test-secret';
  });

  afterEach(() => {
    // Restore environment variables
    process.env = originalEnv;
  });

  describe('generateToken', () => {
    it('should generate a token with default options', () => {
      // Setup
      const userData = { id: '123', username: 'testuser', role: 'provider' };
      jwt.sign.mockReturnValue('mocked-token');

      // Execute
      const token = generateToken(userData);

      // Verify
      expect(jwt.sign).toHaveBeenCalledWith(userData, 'test-secret', { expiresIn: '1h' });
      expect(token).toBe('mocked-token');
      expect(logger.info).toHaveBeenCalledWith('Generated JWT token successfully');
    });

    it('should generate a token with custom options', () => {
      // Setup
      const userData = { id: '123', username: 'testuser', role: 'provider' };
      const options = { expiresIn: '2h' };
      jwt.sign.mockReturnValue('mocked-token-custom');

      // Execute
      const token = generateToken(userData, options);

      // Verify
      expect(jwt.sign).toHaveBeenCalledWith(userData, 'test-secret', options);
      expect(token).toBe('mocked-token-custom');
    });

    it('should throw an error if JWT_SECRET is not defined', () => {
      // Setup
      delete process.env.JWT_SECRET;
      const userData = { id: '123', username: 'testuser', role: 'provider' };

      // Execute & Verify
      expect(() => generateToken(userData)).toThrow('JWT_SECRET is not defined');
      expect(logger.error).toHaveBeenCalledWith('JWT_SECRET is not defined in environment variables');
    });
  });

  describe('verifyToken', () => {
    it('should verify a valid token', () => {
      // Setup
      const decodedToken = { id: '123', username: 'testuser', role: 'provider' };
      jwt.verify.mockReturnValue(decodedToken);

      // Execute
      const result = verifyToken('valid-token');

      // Verify
      expect(jwt.verify).toHaveBeenCalledWith('valid-token', 'test-secret');
      expect(result).toEqual(decodedToken);
      expect(logger.info).toHaveBeenCalledWith('Verified JWT token successfully');
    });

    it('should throw an error for an invalid token', () => {
      // Setup
      const error = new Error('Invalid token');
      jwt.verify.mockImplementation(() => {
        throw error;
      });

      // Execute & Verify
      expect(() => verifyToken('invalid-token')).toThrow('Invalid token');
      expect(logger.error).toHaveBeenCalledWith('Failed to verify JWT token: Invalid token');
    });

    it('should throw an error if JWT_SECRET is not defined', () => {
      // Setup
      delete process.env.JWT_SECRET;

      // Execute & Verify
      expect(() => verifyToken('any-token')).toThrow('JWT_SECRET is not defined');
      expect(logger.error).toHaveBeenCalledWith('JWT_SECRET is not defined in environment variables');
    });
  });
});
