const { verifyToken } = require('../../utils/jwtToken');
const auth = require('../../middleware/auth');

// Mock dependencies
jest.mock('../../utils/jwtToken');

describe('Authentication Middleware', () => {
  let req, res, next;

  beforeEach(() => {
    // Reset mocks before each test
    jest.clearAllMocks();
    
    // Setup request, response, and next function mocks
    req = {
      header: jest.fn(),
    };
    res = {
      status: jest.fn().mockReturnThis(),
      json: jest.fn(),
    };
    next = jest.fn();
  });

  it('should call next() when a valid token is provided', () => {
    // Setup
    const token = 'valid-token';
    const decodedToken = { id: '123', username: 'testuser', role: 'provider' };
    
    req.header.mockReturnValue(`Bearer ${token}`);
    verifyToken.mockReturnValue(decodedToken);

    // Execute
    auth(req, res, next);

    // Verify
    expect(req.header).toHaveBeenCalledWith('Authorization');
    expect(verifyToken).toHaveBeenCalledWith(token);
    expect(req.provider).toEqual(decodedToken);
    expect(next).toHaveBeenCalled();
    expect(res.status).not.toHaveBeenCalled();
    expect(res.json).not.toHaveBeenCalled();
  });

  it('should return 401 when no token is provided', () => {
    // Setup
    req.header.mockReturnValue(null);

    // Execute
    auth(req, res, next);

    // Verify
    expect(req.header).toHaveBeenCalledWith('Authorization');
    expect(verifyToken).not.toHaveBeenCalled();
    expect(next).not.toHaveBeenCalled();
    expect(res.status).toHaveBeenCalledWith(401);
    expect(res.json).toHaveBeenCalledWith({ message: 'Access denied. No token provided.' });
  });

  it('should return 401 when an invalid token is provided', () => {
    // Setup
    const token = 'invalid-token';
    const error = new Error('Invalid token');
    
    req.header.mockReturnValue(`Bearer ${token}`);
    verifyToken.mockImplementation(() => {
      throw error;
    });

    // Execute
    auth(req, res, next);

    // Verify
    expect(req.header).toHaveBeenCalledWith('Authorization');
    expect(verifyToken).toHaveBeenCalledWith(token);
    expect(next).not.toHaveBeenCalled();
    expect(res.status).toHaveBeenCalledWith(401);
    expect(res.json).toHaveBeenCalledWith({ message: 'Invalid token' });
  });

  it('should handle token without Bearer prefix', () => {
    // Setup
    const token = 'valid-token';
    const decodedToken = { id: '123', username: 'testuser', role: 'provider' };
    
    req.header.mockReturnValue(token); // No Bearer prefix
    verifyToken.mockReturnValue(decodedToken);

    // Execute
    auth(req, res, next);

    // Verify
    expect(req.header).toHaveBeenCalledWith('Authorization');
    expect(verifyToken).toHaveBeenCalledWith(token);
    expect(req.provider).toEqual(decodedToken);
    expect(next).toHaveBeenCalled();
  });
});
