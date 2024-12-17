# DabaTaxi App

## Description

DabaTaxi is an Android application that simulates a taxi fare meter, providing real-time tracking of distance, time, and fare calculation for drivers. The app displays the current position of the driver on a Google Map and updates the fare dynamically based on distance and time. The app is designed to help simulate taxi fares for both customers and drivers with an interactive and smooth user interface.

### Features:
1. **Real-time Map Tracking**: Shows the current position of the driver using Google Maps API.
2. **Taxi Fare Calculation**:
   - Base fare.
   - Fare per kilometer.
   - Fare per minute.
3. **Dynamic Fare Updates**: Updates the fare in real-time based on distance and time.
4. **Start/Stop Ride**: Button to start the ride and calculate the fare.
5. **Notifications**: Sends a notification with the total fare, distance, and time at the end of the trip.
6. **Permissions**: Handles location permissions using EasyPermissions.
7. **Animations**: Smooth animations for UI transitions, including map movement and fare updates.

## Technologies Used:
- **Android**: Kotlin, Google Maps API, FusedLocationProviderClient, EasyPermissions.
- **Server**: Node.js, Express, PostgreSQL, JWT Authentication, Multer (for file uploads).
- **UI/UX**: MotionLayout, Lottie animations, Material Design.
- **Database**: PostgreSQL for backend data storage.

## Project Setup

To get started with the project, follow these steps:

### 1. Clone the Repository

```bash
git clone https://github.com/your-username/DabaTaxi.git
cd DabaTaxi
```

### 2. Set up Android Project

1. Open the project in **Android Studio**.
2. Make sure to update the `google_maps_api_key` in your **local.properties** file for Google Maps integration.
3. Install necessary dependencies with Gradle:

```bash
./gradlew build
```

### 3. Set up Backend Server (Optional)

If you want to test the backend functionality, follow these steps:

1. Clone the backend repository:
   
   ```bash
   git clone https://github.com/your-username/DabaTaxi.git
   cd DabaTaxi
   ```

2. Install required dependencies using **npm**:

   ```bash
   npm install
   ```

3. Create a `.env` file for database connection and JWT settings, and start the server:

   ```bash
   npm start
   ```

### 4. Running the App

1. Run the app on an Android Emulator or a physical device using Android Studio.

2. Make sure the backend server is running (if applicable), and ensure your **Google Maps API Key** is configured properly.

---

## Screenshots

Here are some screenshots of the app:

- **Main Screen (Map and Fare)**: ![Main Screen](path/to/screenshot.png)
- **Login Screen**: ![Login Screen](path/to/screenshot.png)

---

## Future Enhancements

- **Payment Integration**: Integrate a payment gateway to process transactions after the ride ends.
- **Ride History**: Allow users to view past ride details.
- **Driver Profile Management**: Enable drivers to update their profile and manage their settings.

---

## Contributing

We welcome contributions! To contribute, please fork the repository and submit a pull request. Make sure to follow the coding conventions and provide detailed commit messages.

### Steps to Contribute:

1. Fork the repository.
2. Clone your fork:

   ```bash
   git clone https://github.com/your-username/DabaTaxi.git
   ```

3. Create a new branch for your changes:

   ```bash
   git checkout -b feature-name
   ```

4. Make your changes and commit them:

   ```bash
   git commit -m "Description of changes"
   ```

5. Push to your fork:

   ```bash
   git push origin feature-name
   ```

6. Submit a pull request.

---

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details.

---

## Contact

For any questions or suggestions, feel free to reach out to me at:  
**Email**: [nezarelayachi@gmail.com](mailto:nezarelayachi@gmail.com)

---

**Enjoy using the DabaTaxi!**
