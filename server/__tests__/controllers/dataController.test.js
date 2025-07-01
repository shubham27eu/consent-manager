const {
  addItem,
  getItems,
  getUserData,
  getConsentList,
  giveConsent,
  accessItem,
  editItem,
  deleteItem,
} = require('../../controllers/dataController');

// Mock models
jest.mock('../../models/Provider');
jest.mock('../../models/Seeker');
jest.mock('../../models/DataItem');
jest.mock('../../models/Consent');
jest.mock('../../models/ConsentHistory');
jest.mock('../../models/Credential');

// Mock console methods to prevent noise during tests
global.console = {
  ...console,
  log: jest.fn(),
  error: jest.fn(),
};

describe('Data Controller', () => {
  let req, res;

  beforeEach(() => {
    // Reset mocks before each test
    jest.clearAllMocks();
    
    // Setup request and response mocks
    req = {
      provider: { id: 'provider-id', role: 'provider' },
      body: {},
      params: {},
      query: {},
    };
    res = {
      status: jest.fn().mockReturnThis(),
      json: jest.fn(),
      send: jest.fn(),
    };
  });

  describe('addItem', () => {
    it('should return 403 if user is not a provider', async () => {
      // Setup
      req.provider.role = 'seeker';

      // Execute
      await addItem(req, res);

      // Verify
      expect(res.status).toHaveBeenCalledWith(403);
      expect(res.json).toHaveBeenCalledWith({ message: 'Unauthorized' });
    });
  });

  describe('getItems', () => {
    it('should return 403 if user is not a provider', async () => {
      // Setup
      req.provider.role = 'seeker';

      // Execute
      await getItems(req, res);

      // Verify
      expect(res.status).toHaveBeenCalledWith(403);
      expect(res.json).toHaveBeenCalledWith({ message: 'Unauthorized' });
    });
  });

  describe('getConsentList', () => {
    it('should return 403 if user is not a provider', async () => {
      // Setup
      req.provider.role = 'seeker';

      // Execute
      await getConsentList(req, res);

      // Verify
      expect(res.status).toHaveBeenCalledWith(403);
      expect(res.json).toHaveBeenCalledWith({ message: 'Unauthorized' });
    });
  });

  describe('giveConsent', () => {
    it('should return 403 if user is not a provider', async () => {
      // Setup
      req.provider.role = 'seeker';

      // Execute
      await giveConsent(req, res);

      // Verify
      expect(res.status).toHaveBeenCalledWith(403);
      expect(res.json).toHaveBeenCalledWith({ message: 'Unauthorized' });
    });
  });

  describe('accessItem', () => {
    it('should return 403 if user is not a seeker', async () => {
      // Setup
      req.provider.role = 'provider';

      // Execute
      await accessItem(req, res);

      // Verify
      expect(res.status).toHaveBeenCalledWith(403);
      expect(res.json).toHaveBeenCalledWith({ message: 'Unauthorized' });
    });
  });

  describe('editItem', () => {
    it('should return 403 if user is not a provider', async () => {
      // Setup
      req.provider.role = 'seeker';

      // Execute
      await editItem(req, res);

      // Verify
      expect(res.status).toHaveBeenCalledWith(403);
      expect(res.json).toHaveBeenCalledWith({ message: 'Unauthorized' });
    });
  });

  describe('deleteItem', () => {
    it('should return 403 if user is not a provider', async () => {
      // Setup
      req.provider.role = 'seeker';

      // Execute
      await deleteItem(req, res);

      // Verify
      expect(res.status).toHaveBeenCalledWith(403);
      expect(res.json).toHaveBeenCalledWith({ message: 'Unauthorized' });
    });
  });
});
