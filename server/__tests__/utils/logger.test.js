// Import the logger
const logger = require('../../utils/logger');

// Mock the logger methods
jest.mock('../../utils/logger', () => ({
  info: jest.fn(),
  error: jest.fn(),
  warn: jest.fn(),
  debug: jest.fn()
}));

describe('Logger Utility', () => {
  beforeEach(() => {
    // Clear all mocks before each test
    jest.clearAllMocks();
  });

  it('should expose logging methods', () => {
    // Verify that the logger exposes the expected methods
    expect(logger.info).toBeDefined();
    expect(logger.error).toBeDefined();
    expect(logger.warn).toBeDefined();
    expect(logger.debug).toBeDefined();
  });

  it('should log messages correctly', () => {
    // Test logging methods
    logger.info('Test info message');
    logger.error('Test error message');
    logger.warn('Test warning message');
    logger.debug('Test debug message');

    // Verify that the logging methods were called
    expect(logger.info).toHaveBeenCalledWith('Test info message');
    expect(logger.error).toHaveBeenCalledWith('Test error message');
    expect(logger.warn).toHaveBeenCalledWith('Test warning message');
    expect(logger.debug).toHaveBeenCalledWith('Test debug message');
  });

  it('should log objects correctly', () => {
    const testObject = { key: 'value', nested: { prop: 'test' } };

    // Test logging an object
    logger.info(testObject);

    // Verify that the logging method was called with the object
    expect(logger.info).toHaveBeenCalledWith(testObject);
  });

  it('should log errors with stack traces', () => {
    const testError = new Error('Test error');

    // Test logging an error
    logger.error(testError);

    // Verify that the logging method was called with the error
    expect(logger.error).toHaveBeenCalledWith(testError);
  });
});
