const {
  getProviderBackLog,
  getSeekerBackLog,
  approveProvider,
  approveSeeker,
  inactivateProvider,
  inactivateSeeker,
  getInactiveUsers,
  reactivate,
  getProviders,
  getSeekers
} = require('../../controllers/adminController');

const ProviderBacklog = require('../../models/ProviderBacklog');
const SeekerBacklog = require('../../models/SeekerBacklog');
const Provider = require('../../models/Provider');
const Seeker = require('../../models/Seeker');
const Credential = require('../../models/Credential');
const DataItem = require('../../models/DataItem');
const Consent = require('../../models/Consent');

// Mock models
jest.mock('../../models/ProviderBacklog');
jest.mock('../../models/SeekerBacklog');
jest.mock('../../models/Provider');
jest.mock('../../models/Seeker');
jest.mock('../../models/Credential');
jest.mock('../../models/DataItem');
jest.mock('../../models/Consent');

// Mock console methods to prevent noise during tests
global.console = {
  ...console,
  log: jest.fn(),
  error: jest.fn(),
};

describe('Admin Controller', () => {
  let req, res;

  beforeEach(() => {
    // Reset mocks before each test
    jest.clearAllMocks();

    // Setup request and response mocks
    req = {
      provider: { id: 'admin-id', role: 'admin' },
      body: {},
    };
    res = {
      status: jest.fn().mockReturnThis(),
      json: jest.fn(),
    };
  });

  describe('getProviderBackLog', () => {
    it('should return pending provider backlog entries', async () => {
      // Setup
      const mockProviders = [
        { _id: 'provider1', first_name: 'John', last_name: 'Doe' },
        { _id: 'provider2', first_name: 'Jane', last_name: 'Smith' },
      ];

      ProviderBacklog.find.mockReturnValue({
        sort: jest.fn().mockReturnThis(),
        select: jest.fn().mockResolvedValue(mockProviders)
      });

      // Execute
      await getProviderBackLog(req, res);

      // Verify
      expect(ProviderBacklog.find).toHaveBeenCalledWith({ status: 'pending' });
      expect(res.status).toHaveBeenCalledWith(200);
      expect(res.json).toHaveBeenCalledWith({ data: mockProviders });
    });

    it('should return 403 if user is not an admin', async () => {
      // Setup
      req.provider.role = 'provider';

      // Execute
      await getProviderBackLog(req, res);

      // Verify
      expect(res.status).toHaveBeenCalledWith(403);
      expect(res.json).toHaveBeenCalledWith({ message: 'Unauthorized' });
    });

    it('should handle errors', async () => {
      // Setup
      const error = new Error('Database error');
      ProviderBacklog.find.mockReturnValue({
        sort: jest.fn().mockReturnThis(),
        select: jest.fn().mockRejectedValue(error)
      });

      // Execute
      await getProviderBackLog(req, res);

      // Verify
      expect(res.status).toHaveBeenCalledWith(500);
      expect(res.json).toHaveBeenCalledWith({ message: 'Server error' });
    });
  });

  describe('getSeekerBackLog', () => {
    it('should return pending seeker backlog entries', async () => {
      // Setup
      const mockSeekers = [
        { _id: 'seeker1', name: 'Bank A' },
        { _id: 'seeker2', name: 'Government B' },
      ];

      SeekerBacklog.find.mockReturnValue({
        sort: jest.fn().mockReturnThis(),
        select: jest.fn().mockResolvedValue(mockSeekers)
      });

      // Execute
      await getSeekerBackLog(req, res);

      // Verify
      expect(SeekerBacklog.find).toHaveBeenCalledWith({ status: 'pending' });
      expect(res.status).toHaveBeenCalledWith(200);
      expect(res.json).toHaveBeenCalledWith({ data: mockSeekers });
    });

    it('should return 403 if user is not an admin', async () => {
      // Setup
      req.provider.role = 'seeker';

      // Execute
      await getSeekerBackLog(req, res);

      // Verify
      expect(res.status).toHaveBeenCalledWith(403);
      expect(res.json).toHaveBeenCalledWith({ message: 'Unauthorized' });
    });

    it('should handle errors', async () => {
      // Setup
      const error = new Error('Database error');
      SeekerBacklog.find.mockReturnValue({
        sort: jest.fn().mockReturnThis(),
        select: jest.fn().mockRejectedValue(error)
      });

      // Execute
      await getSeekerBackLog(req, res);

      // Verify
      expect(res.status).toHaveBeenCalledWith(500);
      expect(res.json).toHaveBeenCalledWith({ message: 'Server error' });
    });
  });

  describe('approveProvider', () => {
    it('should approve a provider and move from backlog to active', async () => {
      // Setup
      req.body = {
        providerId: 'provider1',
        action: 'approve',
      };

      const mockProvider = {
        _id: 'provider1',
        username: 'johndoe',
        password: 'hashedpassword',
        first_name: 'John',
        last_name: 'Doe',
        email: 'john@example.com',
        mobile_no: '1234567890',
        date_of_birth: new Date('1990-01-01'),
        age: 33,
        publicKey: 'publickey123',
        status: 'pending',
        save: jest.fn().mockResolvedValue(true),
      };

      ProviderBacklog.findById.mockResolvedValue(mockProvider);

      const mockCredential = {
        _id: 'cred1',
        save: jest.fn().mockResolvedValue(true),
      };

      Credential.mockImplementation(() => mockCredential);
      Provider.create.mockResolvedValue({ _id: 'provider1' });

      // Execute
      await approveProvider(req, res);

      // Verify
      expect(ProviderBacklog.findById).toHaveBeenCalledWith('provider1');
      expect(mockCredential.save).toHaveBeenCalled();
      expect(Provider.create).toHaveBeenCalled();
      expect(mockProvider.status).toBe('approved');
      expect(mockProvider.save).toHaveBeenCalled();
      expect(res.json).toHaveBeenCalledWith({ message: 'Provider approveed' });
    });

    it('should reject a provider', async () => {
      // Setup
      req.body = {
        providerId: 'provider1',
        action: 'reject',
      };

      const mockProvider = {
        _id: 'provider1',
        status: 'pending',
        save: jest.fn().mockResolvedValue(true),
      };

      ProviderBacklog.findById.mockResolvedValue(mockProvider);

      // Execute
      await approveProvider(req, res);

      // Verify
      expect(ProviderBacklog.findById).toHaveBeenCalledWith('provider1');
      expect(mockProvider.status).toBe('rejected');
      expect(mockProvider.save).toHaveBeenCalled();
      expect(res.json).toHaveBeenCalledWith({ message: 'Provider rejected' });
    });

    it('should return 403 if user is not an admin', async () => {
      // Setup
      req.provider.role = 'provider';
      req.body = {
        providerId: 'provider1',
        action: 'approve',
      };

      // Execute
      await approveProvider(req, res);

      // Verify
      expect(res.status).toHaveBeenCalledWith(403);
      expect(res.json).toHaveBeenCalledWith({ message: 'Unauthorized' });
    });

    it('should return 404 if provider is not found', async () => {
      // Setup
      req.body = {
        providerId: 'nonexistent',
        action: 'approve',
      };

      ProviderBacklog.findById.mockResolvedValue(null);

      // Execute
      await approveProvider(req, res);

      // Verify
      expect(ProviderBacklog.findById).toHaveBeenCalledWith('nonexistent');
      expect(res.status).toHaveBeenCalledWith(404);
      expect(res.json).toHaveBeenCalledWith({ message: 'Provider not found' });
    });

    it('should handle errors', async () => {
      // Setup
      req.body = {
        providerId: 'provider1',
        action: 'approve',
      };

      const error = new Error('Database error');
      ProviderBacklog.findById.mockRejectedValue(error);

      // Execute
      await approveProvider(req, res);

      // Verify
      expect(res.status).toHaveBeenCalledWith(500);
      expect(res.json).toHaveBeenCalledWith({ message: 'Database error' });
    });
  });

  describe('approveSeeker', () => {
    it('should approve a seeker and move from backlog to active', async () => {
      // Setup
      req.body = {
        seekerId: 'seeker1',
        action: 'approve',
      };

      const mockSeeker = {
        _id: 'seeker1',
        username: 'bankA',
        password: 'hashedpassword',
        name: 'Bank A',
        type: 'Bank',
        registration_no: 'REG123',
        email: 'bank@example.com',
        contact_no: '1234567890',
        address: '123 Main St',
        publicKey: 'publickey123',
        status: 'pending',
        save: jest.fn().mockResolvedValue(true),
      };

      SeekerBacklog.findById.mockResolvedValue(mockSeeker);

      const mockCredential = {
        _id: 'cred1',
        save: jest.fn().mockResolvedValue(true),
      };

      Credential.mockImplementation(() => mockCredential);
      Seeker.create.mockResolvedValue({ _id: 'seeker1' });

      // Execute
      await approveSeeker(req, res);

      // Verify
      expect(SeekerBacklog.findById).toHaveBeenCalledWith('seeker1');
      expect(mockCredential.save).toHaveBeenCalled();
      expect(Seeker.create).toHaveBeenCalled();
      expect(mockSeeker.status).toBe('approved');
      expect(mockSeeker.save).toHaveBeenCalled();
      expect(res.json).toHaveBeenCalledWith({ message: 'Seeker approveed' });
    });

    it('should reject a seeker', async () => {
      // Setup
      req.body = {
        seekerId: 'seeker1',
        action: 'reject',
      };

      const mockSeeker = {
        _id: 'seeker1',
        status: 'pending',
        save: jest.fn().mockResolvedValue(true),
      };

      SeekerBacklog.findById.mockResolvedValue(mockSeeker);

      // Execute
      await approveSeeker(req, res);

      // Verify
      expect(SeekerBacklog.findById).toHaveBeenCalledWith('seeker1');
      expect(mockSeeker.status).toBe('rejected');
      expect(mockSeeker.save).toHaveBeenCalled();
      expect(res.json).toHaveBeenCalledWith({ message: 'Seeker rejected' });
    });

    it('should return 403 if user is not an admin', async () => {
      // Setup
      req.provider.role = 'seeker';
      req.body = {
        seekerId: 'seeker1',
        action: 'approve',
      };

      // Execute
      await approveSeeker(req, res);

      // Verify
      expect(res.status).toHaveBeenCalledWith(403);
      expect(res.json).toHaveBeenCalledWith({ message: 'Unauthorized' });
    });

    it('should return 404 if seeker is not found', async () => {
      // Setup
      req.body = {
        seekerId: 'nonexistent',
        action: 'approve',
      };

      SeekerBacklog.findById.mockResolvedValue(null);

      // Execute
      await approveSeeker(req, res);

      // Verify
      expect(SeekerBacklog.findById).toHaveBeenCalledWith('nonexistent');
      expect(res.status).toHaveBeenCalledWith(404);
      expect(res.json).toHaveBeenCalledWith({ message: 'Seeker not found' });
    });

    it('should handle errors', async () => {
      // Setup
      req.body = {
        seekerId: 'seeker1',
        action: 'approve',
      };

      const error = new Error('Database error');
      SeekerBacklog.findById.mockRejectedValue(error);

      // Execute
      await approveSeeker(req, res);

      // Verify
      expect(res.status).toHaveBeenCalledWith(500);
      expect(res.json).toHaveBeenCalledWith({ message: 'Database error' });
    });
  });

  describe('inactivateProvider', () => {
    it('should inactivate a provider and related data', async () => {
      // Setup
      req.body = {
        userId: 'provider1',
      };

      const mockProvider = {
        _id: 'provider1',
        isActive: true,
        save: jest.fn().mockResolvedValue(true),
      };

      Provider.findById.mockResolvedValue(mockProvider);
      DataItem.updateMany.mockResolvedValue({});
      Consent.updateMany.mockResolvedValue({});

      // Execute
      await inactivateProvider(req, res);

      // Verify
      expect(Provider.findById).toHaveBeenCalledWith('provider1');
      expect(mockProvider.isActive).toBe(false);
      expect(mockProvider.save).toHaveBeenCalled();
      expect(DataItem.updateMany).toHaveBeenCalledWith(
        { item_owner_id: mockProvider._id },
        { isActive: false }
      );
      expect(Consent.updateMany).toHaveBeenCalledWith(
        { provider_id: mockProvider._id },
        { isActive: false }
      );
      expect(res.json).toHaveBeenCalledWith({ message: 'Provider and related data inactivated' });
    });

    it('should return 403 if user is not an admin', async () => {
      // Setup
      req.provider.role = 'provider';
      req.body = {
        userId: 'provider1',
      };

      // Execute
      await inactivateProvider(req, res);

      // Verify
      expect(res.status).toHaveBeenCalledWith(403);
      expect(res.json).toHaveBeenCalledWith({ message: 'Unauthorized' });
    });

    it('should return 404 if provider is not found', async () => {
      // Setup
      req.body = {
        userId: 'nonexistent',
      };

      Provider.findById.mockResolvedValue(null);

      // Execute
      await inactivateProvider(req, res);

      // Verify
      expect(Provider.findById).toHaveBeenCalledWith('nonexistent');
      expect(res.status).toHaveBeenCalledWith(404);
      expect(res.json).toHaveBeenCalledWith({ message: 'Provider not found' });
    });

    it('should handle errors', async () => {
      // Setup
      req.body = {
        userId: 'provider1',
      };

      const error = new Error('Database error');
      Provider.findById.mockRejectedValue(error);

      // Execute
      await inactivateProvider(req, res);

      // Verify
      expect(res.status).toHaveBeenCalledWith(500);
      expect(res.json).toHaveBeenCalledWith({ message: 'Database error' });
    });
  });

  describe('inactivateSeeker', () => {
    it('should inactivate a seeker', async () => {
      // Setup
      req.body = {
        userId: 'seeker1',
      };

      const mockSeeker = {
        _id: 'seeker1',
        isActive: true,
        save: jest.fn().mockResolvedValue(true),
      };

      Seeker.findById.mockResolvedValue(mockSeeker);

      // Execute
      await inactivateSeeker(req, res);

      // Verify
      expect(Seeker.findById).toHaveBeenCalledWith('seeker1');
      expect(mockSeeker.isActive).toBe(false);
      expect(mockSeeker.save).toHaveBeenCalled();
      expect(res.json).toHaveBeenCalledWith({ message: 'Seeker inactivated' });
    });

    it('should return 403 if user is not an admin', async () => {
      // Setup
      req.provider.role = 'seeker';
      req.body = {
        userId: 'seeker1',
      };

      // Execute
      await inactivateSeeker(req, res);

      // Verify
      expect(res.status).toHaveBeenCalledWith(403);
      expect(res.json).toHaveBeenCalledWith({ message: 'Unauthorized' });
    });

    it('should return 404 if seeker is not found', async () => {
      // Setup
      req.body = {
        userId: 'nonexistent',
      };

      Seeker.findById.mockResolvedValue(null);

      // Execute
      await inactivateSeeker(req, res);

      // Verify
      expect(Seeker.findById).toHaveBeenCalledWith('nonexistent');
      expect(res.status).toHaveBeenCalledWith(404);
      expect(res.json).toHaveBeenCalledWith({ message: 'Seeker not found' });
    });

    it('should handle errors', async () => {
      // Setup
      req.body = {
        userId: 'seeker1',
      };

      const error = new Error('Database error');
      Seeker.findById.mockRejectedValue(error);

      // Execute
      await inactivateSeeker(req, res);

      // Verify
      expect(res.status).toHaveBeenCalledWith(500);
      expect(res.json).toHaveBeenCalledWith({ message: 'Database error' });
    });
  });

  describe('getInactiveUsers', () => {
    it('should return all inactive users', async () => {
      // Setup
      const mockProviders = [
        {
          _id: 'provider1',
          credential_id: { username: 'johndoe' },
          first_name: 'John',
          last_name: 'Doe',
          email: 'john@example.com',
          isActive: false,
        },
      ];

      const mockSeekers = [
        {
          _id: 'seeker1',
          credential_id: { username: 'bankA' },
          name: 'Bank A',
          type: 'Bank',
          email: 'bank@example.com',
          isActive: false,
        },
      ];

      Provider.find.mockReturnValue({
        populate: jest.fn().mockResolvedValue(mockProviders),
      });

      Seeker.find.mockReturnValue({
        populate: jest.fn().mockResolvedValue(mockSeekers),
      });

      // Execute
      await getInactiveUsers(req, res);

      // Verify
      expect(Provider.find).toHaveBeenCalledWith({ isActive: false });
      expect(Seeker.find).toHaveBeenCalledWith({ isActive: false });
      expect(res.status).toHaveBeenCalledWith(200);
      expect(res.json).toHaveBeenCalledWith({
        data: expect.arrayContaining([
          expect.objectContaining({
            _id: 'provider1',
            username: 'johndoe',
            role: 'provider',
          }),
          expect.objectContaining({
            _id: 'seeker1',
            username: 'bankA',
            role: 'seeker',
          }),
        ]),
      });
    });

    it('should return 403 if user is not an admin', async () => {
      // Setup
      req.provider.role = 'provider';

      // Execute
      await getInactiveUsers(req, res);

      // Verify
      expect(res.status).toHaveBeenCalledWith(403);
      expect(res.json).toHaveBeenCalledWith({ message: 'Unauthorized' });
    });

    it('should handle errors', async () => {
      // Setup
      const error = new Error('Database error');
      Provider.find.mockReturnValue({
        populate: jest.fn().mockRejectedValue(error),
      });

      // Execute
      await getInactiveUsers(req, res);

      // Verify
      expect(res.status).toHaveBeenCalledWith(500);
      expect(res.json).toHaveBeenCalledWith({ message: 'Server error' });
    });
  });

  describe('reactivate', () => {
    it('should reactivate a provider and related data', async () => {
      // Setup
      req.body = {
        userId: 'provider1',
      };

      const mockProvider = {
        _id: 'provider1',
        isActive: false,
        save: jest.fn().mockResolvedValue(true),
      };

      Provider.findById.mockResolvedValue(mockProvider);
      Seeker.findById.mockResolvedValue(null);
      DataItem.updateMany.mockResolvedValue({});
      Consent.updateMany.mockResolvedValue({});

      // Execute
      await reactivate(req, res);

      // Verify
      expect(Provider.findById).toHaveBeenCalledWith('provider1');
      expect(mockProvider.isActive).toBe(true);
      expect(mockProvider.save).toHaveBeenCalled();
      expect(DataItem.updateMany).toHaveBeenCalledWith(
        { item_owner_id: mockProvider._id },
        { isActive: true }
      );
      expect(Consent.updateMany).toHaveBeenCalledWith(
        { provider_id: mockProvider._id },
        { isActive: true }
      );
      expect(res.json).toHaveBeenCalledWith({ message: 'Provider reactivated' });
    });

    it('should reactivate a seeker', async () => {
      // Setup
      req.body = {
        userId: 'seeker1',
      };

      const mockSeeker = {
        _id: 'seeker1',
        isActive: false,
        save: jest.fn().mockResolvedValue(true),
      };

      Provider.findById.mockResolvedValue(null);
      Seeker.findById.mockResolvedValue(mockSeeker);

      // Execute
      await reactivate(req, res);

      // Verify
      expect(Seeker.findById).toHaveBeenCalledWith('seeker1');
      expect(mockSeeker.isActive).toBe(true);
      expect(mockSeeker.save).toHaveBeenCalled();
      expect(res.json).toHaveBeenCalledWith({ message: 'Seeker reactivated' });
    });

    it('should return 403 if user is not an admin', async () => {
      // Setup
      req.provider.role = 'provider';
      req.body = {
        userId: 'provider1',
      };

      // Execute
      await reactivate(req, res);

      // Verify
      expect(res.status).toHaveBeenCalledWith(403);
      expect(res.json).toHaveBeenCalledWith({ message: 'Unauthorized' });
    });

    it('should return 404 if user is not found', async () => {
      // Setup
      req.body = {
        userId: 'nonexistent',
      };

      Provider.findById.mockResolvedValue(null);
      Seeker.findById.mockResolvedValue(null);

      // Execute
      await reactivate(req, res);

      // Verify
      expect(Provider.findById).toHaveBeenCalledWith('nonexistent');
      expect(Seeker.findById).toHaveBeenCalledWith('nonexistent');
      expect(res.status).toHaveBeenCalledWith(404);
      expect(res.json).toHaveBeenCalledWith({ message: 'Provider/Seeker not found' });
    });

    it('should handle errors', async () => {
      // Setup
      req.body = {
        userId: 'provider1',
      };

      const error = new Error('Database error');
      Provider.findById.mockRejectedValue(error);

      // Execute
      await reactivate(req, res);

      // Verify
      expect(res.status).toHaveBeenCalledWith(500);
      expect(res.json).toHaveBeenCalledWith({ message: 'Database error' });
    });
  });

  describe('getProviders', () => {
    it('should return all active providers', async () => {
      // Setup
      const mockProviders = [
        {
          _id: 'provider1',
          credential_id: { username: 'johndoe' },
          first_name: 'John',
          last_name: 'Doe',
          email: 'john@example.com',
          mobile_no: '1234567890',
        },
      ];

      Provider.find.mockReturnValue({
        populate: jest.fn().mockReturnThis(),
        select: jest.fn().mockResolvedValue(mockProviders),
      });

      // Execute
      await getProviders(req, res);

      // Verify
      expect(Provider.find).toHaveBeenCalledWith({ isActive: true });
      expect(res.status).toHaveBeenCalledWith(200);
      expect(res.json).toHaveBeenCalledWith({ data: mockProviders });
    });

    it('should return 403 if user is not an admin', async () => {
      // Setup
      req.provider.role = 'provider';

      // Execute
      await getProviders(req, res);

      // Verify
      expect(res.status).toHaveBeenCalledWith(403);
      expect(res.json).toHaveBeenCalledWith({ message: 'Unauthorized' });
    });

    it('should handle errors', async () => {
      // Setup
      const error = new Error('Database error');
      Provider.find.mockReturnValue({
        populate: jest.fn().mockReturnThis(),
        select: jest.fn().mockRejectedValue(error),
      });

      // Execute
      await getProviders(req, res);

      // Verify
      expect(res.status).toHaveBeenCalledWith(500);
      expect(res.json).toHaveBeenCalledWith({ message: 'Server error' });
    });
  });

  describe('getSeekers', () => {
    it('should return all active seekers', async () => {
      // Setup
      const mockSeekers = [
        {
          _id: 'seeker1',
          credential_id: { username: 'bankA' },
          name: 'Bank A',
          type: 'Bank',
          email: 'bank@example.com',
          contact_no: '1234567890',
        },
      ];

      Seeker.find.mockReturnValue({
        populate: jest.fn().mockReturnThis(),
        select: jest.fn().mockResolvedValue(mockSeekers),
      });

      // Execute
      await getSeekers(req, res);

      // Verify
      expect(Seeker.find).toHaveBeenCalledWith({ isActive: true });
      expect(res.status).toHaveBeenCalledWith(200);
      expect(res.json).toHaveBeenCalledWith({ data: mockSeekers });
    });

    it('should return 403 if user is not an admin', async () => {
      // Setup
      req.provider.role = 'provider';

      // Execute
      await getSeekers(req, res);

      // Verify
      expect(res.status).toHaveBeenCalledWith(403);
      expect(res.json).toHaveBeenCalledWith({ message: 'Unauthorized' });
    });

    it('should handle errors', async () => {
      // Setup
      const error = new Error('Database error');
      Seeker.find.mockReturnValue({
        populate: jest.fn().mockReturnThis(),
        select: jest.fn().mockRejectedValue(error),
      });

      // Execute
      await getSeekers(req, res);

      // Verify
      expect(res.status).toHaveBeenCalledWith(500);
      expect(res.json).toHaveBeenCalledWith({ message: 'Server error' });
    });
  });
});
