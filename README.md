# Task List Backend Application (Java)

A Java-based task management application demonstrating clean architecture,
incremental refactoring, and REST API design. The project started as a
console-based application and was evolved into a modular backend with
RESTful endpoints.

## Features
- Create and manage tasks and projects
- Optional task deadlines
- View tasks grouped by deadline
- REST API for projects and tasks
- In-memory data storage (database-ready design)
- Modular architecture with separation of concerns

## Tech Stack
- Java
- Maven
- REST APIs
- Basic validation & error handling
- Git (incremental commits)

## Architecture Overview
- Core business logic separated from input/output layers
- Console interface and REST API can run independently
- Designed for easy extension (e.g. database, authentication)

## API Endpoints
- `POST /projects` – Create a new project
- `GET /projects` – Retrieve all projects and their tasks
- `POST /projects/{projectId}/tasks` – Create a task
- `PUT /projects/{projectId}/tasks/{taskId}/deadline` – Add or update deadline
- `GET /projects/view_by_deadline` – View tasks grouped by deadline

## What I Focused On
- Clean, readable Java code
- Small, meaningful commits
- Refactoring without breaking functionality
- Designing APIs before persistence
- Testability and future extensibility
