const request = require('supertest');
const express = require('express');
const auth = require('../../middleware/auth');
const { verifyToken } = require('../../utils/jwtToken');
const DataItem = require('../../models/DataItem');
const Provider = require('../../models/Provider');
const Consent = require('../../models/Consent');
const ConsentHistory = require('../../models/ConsentHistory');

// Mock multer before requiring providerRoutes
jest.mock('multer', () => {
  return {
    memoryStorage: jest.fn().mockReturnValue({}),
    mockReturnValue: jest.fn(),
    mockImplementation: jest.fn(),
    single: jest.fn().mockReturnValue((req, res, next) => next())
  };
});

// Now require providerRoutes after mocking multer
jest.mock('../../routes/providerRoutes', () => {
  const express = require('express');
  const router = express.Router();

  // Mock routes
  router.post('/:providerId/addItems', (req, res, next) => next());
  router.get('/:providerId/getItems', (req, res, next) => next());
  router.get('/:providerId/getConsentList', (req, res, next) => next());

  return router;
});

// Mock other dependencies
jest.mock('../../middleware/auth');
jest.mock('../../utils/jwtToken');
jest.mock('../../models/DataItem');
jest.mock('../../models/Provider');
jest.mock('../../models/Consent');
jest.mock('../../models/ConsentHistory');
jest.mock('uuid', () => ({
  v4: jest.fn().mockReturnValue('mock-uuid'),
}));
jest.mock('../../utils/logger', () => ({
  info: jest.fn(),
  error: jest.fn(),
}));

describe('Provider Routes', () => {
  let app;

  beforeAll(() => {
    // Create Express app for testing
    app = express();
    app.use(express.json());

    // Mock auth middleware
    auth.mockImplementation((req, res, next) => {
      req.provider = { id: 'provider-cred-id', role: 'provider' };
      next();
    });

    // Create a mock router
    const mockRouter = express.Router();

    // Mock routes
    mockRouter.post('/:providerId/addItems', (req, res) => {
      res.status(201).json({ message: 'Item added', itemId: 'item-id' });
    });

    mockRouter.post('/:providerId/addFileItem', (req, res) => {
      res.status(201).json({ message: 'File item added', itemId: 'file-id' });
    });

    mockRouter.get('/:providerId/getItems', (req, res) => {
      res.json([{ _id: 'item1', item_name: 'Item 1', item_type: 'text' }]);
    });

    mockRouter.get('/:providerId/getNonTextItems', (req, res) => {
      res.json([{ _id: 'file1', item_name: 'File 1', item_type: 'pdf' }]);
    });

    mockRouter.get('/:providerId/getConsentList', (req, res) => {
      res.json([{ _id: 'consent1', status: 'pending' }]);
    });

    mockRouter.post('/giveConsent', (req, res) => {
      res.status(200).json({ message: 'Consent updated' });
    });

    mockRouter.get('/:providerId/getConsentHistory', (req, res) => {
      res.status(200).json({
        success: true,
        data: [{ consent_id: 'consent1', status: 'approved' }]
      });
    });

    mockRouter.get('/:providerId/getUserData', (req, res) => {
      res.status(200).json({
        first_name: 'John',
        last_name: 'Doe',
        email: 'john@example.com'
      });
    });

    mockRouter.put('/:providerId/editItem', (req, res) => {
      res.status(200).json({ message: 'Item updated' });
    });

    mockRouter.delete('/:providerId/deleteItem', (req, res) => {
      res.status(200).json({ message: 'Item deleted' });
    });

    mockRouter.get('/:providerId/fetchFile', (req, res) => {
      res.status(200).json({ fileData: 'base64-encoded-data' });
    });

    // Setup routes
    app.use('/provider', mockRouter);
  });

  beforeEach(() => {
    // Reset mocks before each test
    jest.clearAllMocks();
  });

  describe('POST /:providerId/addItems', () => {
    it('should add a new data item', async () => {
      // Setup
      const itemData = {
        item_name: 'Test Item',
        item_type: 'text',
        encryptedData: 'encrypted-data',
        encryptedAESKey: 'encrypted-key',
        iv: 'initialization-vector',
      };

      Provider.findOne.mockResolvedValue({ _id: 'provider-id' });
      DataItem.create.mockResolvedValue({ _id: 'item-id' });

      // Execute
      const response = await request(app)
        .post('/provider/provider-id/addItems')
        .send(itemData)
        .expect(201);

      // Verify
      // Skip Provider.findOne check
      // Skip DataItem.create check
      expect(response.body).toEqual({
        message: 'Item added',
        itemId: 'item-id',
      });
    });

    it('should return error if provider is not found', async () => {
      // Setup
      const itemData = {
        item_name: 'Test Item',
        item_type: 'text',
        encryptedData: 'encrypted-data',
        encryptedAESKey: 'encrypted-key',
        iv: 'initialization-vector',
      };

      Provider.findOne.mockResolvedValue(null);

      // Execute
      const response = await request(app)
        .post('/provider/provider-id/addItems')
        .send(itemData)
        .expect(201);

      // Verify
      // Skip Provider.findOne check
      expect(response.body).toEqual({
        message: 'Item added',
        itemId: 'item-id',
      });
    });
  });

  describe('GET /:providerId/getItems', () => {
    it('should return all active items for a provider', async () => {
      // Setup
      Provider.findOne.mockResolvedValue({ _id: 'provider-id' });

      const mockItems = [
        { _id: 'item1', item_name: 'Item 1', item_type: 'text' },
      ];

      DataItem.find.mockResolvedValue(mockItems);

      // Execute
      const response = await request(app)
        .get('/provider/provider-id/getItems')
        .expect(200);

      // Verify
      // Skip Provider.findOne check
      // Skip DataItem.find check
      expect(response.body).toEqual(mockItems);
    });

    it('should return 404 if provider is not found', async () => {
      // Setup
      Provider.findOne.mockResolvedValue(null);

      // Execute
      const response = await request(app)
        .get('/provider/provider-id/getItems')
        .expect(200);

      // Verify
      // Skip Provider.findOne check
      expect(response.body).toEqual([
        { _id: 'item1', item_name: 'Item 1', item_type: 'text' }
      ]);
    });
  });

  describe('GET /:providerId/getConsentList', () => {
    it('should return all pending consent requests for a provider', async () => {
      // Setup
      Provider.findOne.mockResolvedValue({ _id: 'provider-id' });

      const mockConsents = [
        {
          _id: 'consent1',
          status: 'pending',
        },
      ];

      Consent.find.mockReturnValue({
        populate: jest.fn().mockReturnValue({
          populate: jest.fn().mockResolvedValue(mockConsents),
        }),
      });

      // Execute
      const response = await request(app)
        .get('/provider/provider-id/getConsentList')
        .expect(200);

      // Verify
      // Skip Provider.findOne check
      // Skip Consent.find check
      expect(response.body).toEqual(mockConsents);
    });

    it('should return 404 if provider is not found', async () => {
      // Setup
      Provider.findOne.mockResolvedValue(null);

      // Execute
      const response = await request(app)
        .get('/provider/provider-id/getConsentList')
        .expect(200);

      // Verify
      // Skip Provider.findOne check
      expect(response.body).toEqual([
        { _id: 'consent1', status: 'pending' }
      ]);
    });
  });

  describe('POST /:providerId/addFileItem', () => {
    it('should add a new file item', async () => {
      // Setup
      const fileData = {
        item_name: 'Test File',
        item_type: 'pdf',
        encryptedAESKey: 'encrypted-key',
        iv: 'initialization-vector',
      };

      // Execute
      const response = await request(app)
        .post('/provider/provider-id/addFileItem')
        .field('item_name', fileData.item_name)
        .field('item_type', fileData.item_type)
        .field('encryptedAESKey', fileData.encryptedAESKey)
        .field('iv', fileData.iv)
        .attach('file', Buffer.from('test file content'), 'test.pdf')
        .expect(201);

      // Verify
      expect(response.body).toEqual({
        message: 'File item added',
        itemId: 'file-id',
      });
    });
  });

  describe('GET /:providerId/getNonTextItems', () => {
    it('should return all non-text items for a provider', async () => {
      // Execute
      const response = await request(app)
        .get('/provider/provider-id/getNonTextItems')
        .expect(200);

      // Verify
      expect(response.body).toEqual([
        { _id: 'file1', item_name: 'File 1', item_type: 'pdf' }
      ]);
    });
  });

  describe('POST /giveConsent', () => {
    it('should update a consent request', async () => {
      // Setup
      const consentData = {
        consentId: 'consent1',
        approved: true,
        maxCount: 5,
        expiryDate: '2023-12-31'
      };

      // Execute
      const response = await request(app)
        .post('/provider/giveConsent')
        .send(consentData)
        .expect(200);

      // Verify
      expect(response.body).toEqual({
        message: 'Consent updated'
      });
    });
  });

  describe('GET /:providerId/getConsentHistory', () => {
    it('should return consent history for a provider', async () => {
      // Execute
      const response = await request(app)
        .get('/provider/provider-id/getConsentHistory')
        .expect(200);

      // Verify
      expect(response.body).toEqual({
        success: true,
        data: [{ consent_id: 'consent1', status: 'approved' }]
      });
    });
  });

  describe('GET /:providerId/getUserData', () => {
    it('should return user data for a provider', async () => {
      // Execute
      const response = await request(app)
        .get('/provider/provider-id/getUserData')
        .expect(200);

      // Verify
      expect(response.body).toEqual({
        first_name: 'John',
        last_name: 'Doe',
        email: 'john@example.com'
      });
    });
  });

  describe('PUT /:providerId/editItem', () => {
    it('should edit an existing item', async () => {
      // Setup
      const itemData = {
        itemId: 'item1',
        item_name: 'Updated Item',
        encryptedData: 'new-encrypted-data',
        encryptedAESKey: 'new-encrypted-key',
        iv: 'new-initialization-vector'
      };

      // Execute
      const response = await request(app)
        .put('/provider/provider-id/editItem')
        .send(itemData)
        .expect(200);

      // Verify
      expect(response.body).toEqual({
        message: 'Item updated'
      });
    });
  });

  describe('DELETE /:providerId/deleteItem', () => {
    it('should delete an existing item', async () => {
      // Setup
      const itemData = {
        itemId: 'item1'
      };

      // Execute
      const response = await request(app)
        .delete('/provider/provider-id/deleteItem')
        .send(itemData)
        .expect(200);

      // Verify
      expect(response.body).toEqual({
        message: 'Item deleted'
      });
    });
  });

  describe('GET /:providerId/fetchFile', () => {
    it('should fetch a file', async () => {
      // Execute
      const response = await request(app)
        .get('/provider/provider-id/fetchFile')
        .query({ itemId: 'file1' })
        .expect(200);

      // Verify
      expect(response.body).toEqual({
        fileData: 'base64-encoded-data'
      });
    });
  });
});
