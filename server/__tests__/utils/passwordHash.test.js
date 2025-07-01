const bcrypt = require('bcryptjs');
const { generatePasswordHash, verifyPassword } = require('../../utils/passwordHash');

// Mock dependencies
jest.mock('bcryptjs');

describe('Password Hash Utilities', () => {
  beforeEach(() => {
    // Reset mocks before each test
    jest.clearAllMocks();
  });

  describe('generatePasswordHash', () => {
    it('should generate a password hash', async () => {
      // Setup
      const password = 'TestPassword123!';
      const salt = 'mockedsalt';
      const hash = 'mockedhash';
      
      bcrypt.genSalt.mockResolvedValue(salt);
      bcrypt.hash.mockResolvedValue(hash);

      // Execute
      const result = await generatePasswordHash(password);

      // Verify
      expect(bcrypt.genSalt).toHaveBeenCalledWith(10);
      expect(bcrypt.hash).toHaveBeenCalledWith(password, salt);
      expect(result).toBe(hash);
    });

    it('should handle errors during hash generation', async () => {
      // Setup
      const password = 'TestPassword123!';
      const error = new Error('Hash generation failed');
      
      bcrypt.genSalt.mockRejectedValue(error);

      // Execute & Verify
      await expect(generatePasswordHash(password)).rejects.toThrow('Hash generation failed');
    });
  });

  describe('verifyPassword', () => {
    it('should return true for matching password and hash', async () => {
      // Setup
      const password = 'TestPassword123!';
      const hash = 'hashedpassword';
      
      bcrypt.compare.mockResolvedValue(true);

      // Execute
      const result = await verifyPassword(password, hash);

      // Verify
      expect(bcrypt.compare).toHaveBeenCalledWith(password, hash);
      expect(result).toBe(true);
    });

    it('should return false for non-matching password and hash', async () => {
      // Setup
      const password = 'WrongPassword123!';
      const hash = 'hashedpassword';
      
      bcrypt.compare.mockResolvedValue(false);

      // Execute
      const result = await verifyPassword(password, hash);

      // Verify
      expect(bcrypt.compare).toHaveBeenCalledWith(password, hash);
      expect(result).toBe(false);
    });

    it('should handle errors during password verification', async () => {
      // Setup
      const password = 'TestPassword123!';
      const hash = 'hashedpassword';
      const error = new Error('Verification failed');
      
      bcrypt.compare.mockRejectedValue(error);

      // Execute & Verify
      await expect(verifyPassword(password, hash)).rejects.toThrow('Verification failed');
    });
  });
});
