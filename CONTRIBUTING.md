# Contributing to World Flood

Thanks for your interest in contributing!

## How to contribute

- **Bug reports:** Open a [GitHub issue](../../issues) and include:
  - Minecraft and NeoForge versions
  - Steps to reproduce the problem
  - Expected vs actual behavior
  - Crash logs or relevant game logs, if any
- **Feature suggestions:** Open an issue to discuss the idea before investing
  significant time in implementation.
- **Pull requests:** Fork the repository, create a branch, make your changes,
  and open a pull request against `main`.

## Development setup

1. Clone the repository.
2. Ensure JDK 21 or newer is installed.
3. Run `./gradlew build` to verify the project compiles.
4. Use `./gradlew runClient` to start a test client.

## Code style

- Follow the existing code formatting and naming conventions.
- Keep changes focused on one thing per pull request.
- Add comments for non-obvious logic, especially around Mixin injection points
  and world generation behavior.

## License

By contributing, you agree that your contributions will be licensed under the
same [MIT License](LICENSE) that covers this project.
