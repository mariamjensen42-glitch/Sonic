# Contributing to Sonic

Thank you for your interest in contributing to Sonic! This document provides guidelines and instructions for contributing.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Project Structure](#project-structure)
- [Coding Standards](#coding-standards)
- [Commit Guidelines](#commit-guidelines)
- [Pull Request Process](#pull-request-process)
- [Reporting Issues](#reporting-issues)

## Code of Conduct

Be respectful and inclusive. We welcome contributions from everyone.

## Getting Started

1. Fork the repository
2. Clone your fork locally
3. Create a new branch for your changes
4. Make your changes
5. Push to your fork
6. Open a Pull Request

## Development Setup

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK with API 36
- Kotlin 2.0.21

### Building the Project

```bash
# Clone the repository
git clone https://github.com/your-username/sonic.git
cd sonic

# Build the project
./gradlew build

# Run unit tests
./gradlew test

# Run lint
./gradlew lint
```

### Project Structure

```
sonic/
├── app/              # Main application module
├── data/             # Data layer (repositories, database, API)
├── domain/           # Domain layer (use cases, models)
├── presentation/     # UI layer (Compose screens, ViewModels)
└── gradle/           # Gradle configuration
```

### Architecture

This project follows Clean Architecture with MVI pattern:

- **Presentation Layer**: Jetpack Compose UI + ViewModels
- **Domain Layer**: Use cases, repository interfaces, domain models
- **Data Layer**: Repository implementations, Room database, MediaStore

## Coding Standards

### Kotlin Style

- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Keep functions small and focused
- Use data classes for models
- Prefer immutability (val over var)

### Compose Guidelines

- Use state hoisting pattern
- Keep composables small and reusable
- Use remember for state preservation
- Follow Material 3 design guidelines

### Code Organization

- One class per file
- Package by feature when possible
- Keep related code together

## Commit Guidelines

We follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

### Types

- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

### Examples

```
feat(player): add shuffle mode support
fix(lyrics): fix sync timing issue
docs(readme): update installation instructions
```

## Pull Request Process

1. **Create a branch**: Create a feature branch from `main`
2. **Make changes**: Implement your changes following coding standards
3. **Test**: Ensure all tests pass and add new tests if needed
4. **Commit**: Use conventional commit messages
5. **Push**: Push your changes to your fork
6. **Open PR**: Create a Pull Request with a clear description

### PR Requirements

- Fill out the PR template completely
- Link related issues
- Ensure CI passes
- Request review from maintainers
- Address review feedback

## Reporting Issues

### Bug Reports

Use the [Bug Report template](.github/ISSUE_TEMPLATE/bug_report.yml) and include:

- Clear description of the bug
- Steps to reproduce
- Expected vs actual behavior
- Device and Android version
- App version
- Screenshots if applicable

### Feature Requests

Use the [Feature Request template](.github/ISSUE_TEMPLATE/feature_request.yml) and include:

- Clear description of the feature
- Problem it solves
- Proposed solution
- Any alternatives considered

## Questions?

If you have questions, feel free to:

- Open a [Discussion](https://github.com/cycling74/sonic/discussions)
- Ask in the issue tracker

Thank you for contributing! 🎵
