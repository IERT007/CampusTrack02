# CampusTrack02

CampusTrack02 is a lightweight, command-line-based application designed to help students manage their academic life efficiently directly from their terminal.

## Features

*   **Attendance Tracking:** Keep a precise record of your classes, calculate attendance percentages, and stay updated on your standing.
*   **Timetable Management:** Organize your daily and weekly class schedules for quick reference.
*   **CLI-First Design:** Optimized for speed and minimal resource usage.

## Installation & Maintenance

CampusTrack02 is built to be run and maintained via the **Termux CLI** on Android.

1.  **Open Termux** on your device.
2.  Ensure your package list is updated:
    ```bash
    pkg update && pkg upgrade
    ```
3.  Clone the repository or install via your preferred method:
    ```bash
    git clone [repository-url]
    cd CampusTrack02
    ```
4.  Run the application:
    ```bash
    python main.py
    ```

## Usage

*   **View Timetable:** Use the `view-schedule` command to see your upcoming classes.
*   **Log Attendance:** Use `mark-attendance` to record your presence for a specific subject.
*   **Check Stats:** Use `status` to view your current attendance percentage across all subjects.

## Contributing

As this project is maintained via Termux, contributions are welcomed through standard Git workflows. Please ensure all code changes are tested within the Termux environment before submitting a pull request.

## License

This project is open-source and available under the [MIT License](LICENSE).
