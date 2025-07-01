const errorHandler = require('../../middleware/error');
const logger = require('../../utils/logger');

// Mock dependencies
jest.mock('../../utils/logger');

describe('Error Handling Middleware', () => {
  let req, res, next;

  beforeEach(() => {
    // Reset mocks before each test
    jest.clearAllMocks();
    
    // Setup request, response, and next function mocks
    req = {
      ip: '127.0.0.1',
      method: 'GET',
      url: '/test',
    };
    res = {
      status: jest.fn().mockReturnThis(),
      json: jest.fn(),
    };
    next = jest.fn();
  });

  it('should handle standard errors with default status code 500', () => {
    // Setup
    const error = new Error('Test error');

    // Execute
    errorHandler(error, req, res, next);

    // Verify
    expect(logger.error).toHaveBeenCalledWith(
      expect.stringContaining('Test error - GET /test - IP: 127.0.0.1')
    );
    expect(res.status).toHaveBeenCalledWith(500);
    expect(res.json).toHaveBeenCalledWith({
      errors: [{ msg: 'Test error' }],
    });
  });

  it('should handle errors with custom status codes', () => {
    // Setup
    const error = new Error('Not found');
    error.status = 404;

    // Execute
    errorHandler(error, req, res, next);

    // Verify
    expect(logger.error).toHaveBeenCalledWith(
      expect.stringContaining('Not found - GET /test - IP: 127.0.0.1')
    );
    expect(res.status).toHaveBeenCalledWith(404);
    expect(res.json).toHaveBeenCalledWith({
      errors: [{ msg: 'Not found' }],
    });
  });

  it('should handle validation errors from express-validator', () => {
    // Setup
    const error = new Error('Validation failed');
    error.errors = [
      { msg: 'Username is required' },
      { msg: 'Password must be at least 8 characters' },
    ];

    // Execute
    errorHandler(error, req, res, next);

    // Verify
    expect(logger.error).toHaveBeenCalledWith(
      expect.stringContaining('Validation failed - GET /test - IP: 127.0.0.1')
    );
    expect(res.status).toHaveBeenCalledWith(400);
    expect(res.json).toHaveBeenCalledWith({
      errors: [
        { msg: 'Username is required' },
        { msg: 'Password must be at least 8 characters' },
      ],
    });
  });

  it('should use connection.remoteAddress if req.ip is not available', () => {
    // Setup
    const error = new Error('Test error');
    req.ip = undefined;
    req.connection = { remoteAddress: '192.168.1.1' };

    // Execute
    errorHandler(error, req, res, next);

    // Verify
    expect(logger.error).toHaveBeenCalledWith(
      expect.stringContaining('Test error - GET /test - IP: 192.168.1.1')
    );
    expect(res.status).toHaveBeenCalledWith(500);
    expect(res.json).toHaveBeenCalledWith({
      errors: [{ msg: 'Test error' }],
    });
  });

  it('should handle errors with no message', () => {
    // Setup
    const error = new Error();
    error.message = undefined;

    // Execute
    errorHandler(error, req, res, next);

    // Verify
    expect(res.status).toHaveBeenCalledWith(500);
    expect(res.json).toHaveBeenCalledWith({
      errors: [{ msg: 'Internal Server Error' }],
    });
  });
});
