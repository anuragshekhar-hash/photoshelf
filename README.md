# Image Organizer

A simple utility to organize your image library.

## Features

*   **Organize by Date:** Automatically reads EXIF data to sort images into a `YYYY/MM/DD` folder structure.
*   **Customizable Naming:** Rename files based on a configurable pattern (e.g., `YYYY-MM-DD_HH-MM-SS.jpg`).
*   **Recursive Scan:** Scans subdirectories for images to organize.
*   **Duplicate Handling:** (Optional) Detect and handle duplicate images.

## Prerequisites

To build and run this project, you will need:

*   Java Development Kit (JDK) 17 or later
*   Apache Maven 3.6+

## Getting Started

Follow these instructions to get a copy of the project up and running on your local machine.

### 1. Clone the Repository

```bash
git clone https://github.com/your-username/ImageOrganizer.git
cd ImageOrganizer
```
*(Replace `your-username` with the actual GitHub username or repository URL)*

### 2. Build the Project

Use the Maven wrapper to compile the source code and package it into an executable JAR file.

```bash
# On macOS/Linux
./mvnw clean package

# On Windows
./mvnw.cmd clean package
```

After a successful build, you will find the JAR file in the `target/` directory (e.g., `ImageOrganizer-1.0.0.jar`).

## Usage

Run the application from your terminal using the `java -jar` command.

### Syntax

```bash
java -jar target/ImageOrganizer-1.0.0.jar
```


## Contributing

Contributions are welcome! Please feel free to submit a pull request.

1.  Fork the repository.
2.  Create your feature branch (`git checkout -b feature/AmazingFeature`).
3.  Commit your changes (`git commit -m 'Add some AmazingFeature'`).
4.  Push to the branch (`git push origin feature/AmazingFeature`).
5.  Open a Pull Request.

## License

This project is licensed under the GLPv3 License - see the `LICENSE` file for details.