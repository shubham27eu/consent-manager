# Unit Tests for Server

This directory contains unit tests for the server-side code of the application.

## Test Structure

The tests are organized by component type:

- `controllers/`: Tests for controller functions
- `middleware/`: Tests for middleware functions
- `routes/`: Tests for API routes
- `utils/`: Tests for utility functions

## Running Tests

To run all tests:

```bash
npm test
```

To run tests with coverage report:

```bash
npm test -- --coverage
```

To run a specific test file:

```bash
npm test -- __tests__/controllers/authController.test.js
```

## Test Coverage

The test coverage report will be generated in the `coverage` directory. You can view the HTML report by opening `coverage/lcov-report/index.html` in your browser.

## Mocking

The tests use Jest's mocking capabilities to mock:

- Database models
- External dependencies
- Utility functions

This allows us to test components in isolation without requiring a real database connection.

## Test Environment

Tests run in a Node.js environment with:

- In-memory MongoDB server for database tests
- Express app for route testing
- Mocked authentication

## Adding New Tests

When adding new tests:

1. Follow the existing structure
2. Mock external dependencies
3. Test both success and error cases
4. Verify all assertions are meaningful
