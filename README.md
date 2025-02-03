# Lottery and User Management API

This project provides an API for managing lotteries and users, including functionality for creating lotteries, adding ballots, and user registration/login.

## API Endpoints

### **Lottery Routes**

- **POST /lotteries**
  - Creates a new lottery (requires valid JWT token and `admin` role).
  
- **POST /lotteries/ballots**
  - Adds ballots to a lottery (requires valid JWT token and `user` role).

- **GET /lotteries/{id}**
  - Retrieves a lottery by its ID (requires valid JWT token and accessible for both `admin` and `user` roles).

- **GET /lotteries**
  - Retrieves a list of lotteries with optional filters by status and draw date (requires valid JWT token and accessible for both `admin` and `user` roles).

- **GET /lotteries/{id}/ballots**
  - Retrieves a user's lottery ballots by lottery ID (requires valid JWT token and `user` role).

### **User Routes**

- **POST /admin/register**
  - Registers a new admin user.
  
- **POST /user/register**
  - Registers a new regular user.

- **POST /user/login**
  - Logs in a user and returns a JWT token.

- **GET /users**
  - Retrieves a list of users with optional pagination (requires valid JWT token and requires `admin` role).
